package com.aaujar.trscoreapi.scheduler;

import com.aaujar.trscoreapi.service.mail.SendMailService;
import com.aaujar.trscoreapi.utils.budibase.BudibaseAPI;
import com.google.gson.JsonObject;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;

@Component
public class ScheduledCliniko {

    @Autowired
    private SendMailService mailService;

    @Value("${cliniko.url}")
    private String clinikoUrl;

    @Value("${cliniko.api-key}")
    private String clinikoApiKey;

    @Value("${budibase.url}")
    private String budiBaseUrl;

    @Value("${budibase.api-key}")
    private String budiBaseApiKey;

    @Value("${budibase.x-budibase-app-id}")
    private String budiBaseAppId;

    @Value("${budibase.booking-table-id}")
    private String budiBaseBookingTableId;

    @Value("${budibase.patient-table-id}")
    private String budiBasePatientTableId;

    @Value("${budibase.booking-column}")
    private String budiBaseBookingColumn;

    @Value("${budibase.patient-column}")
    private String budiBasePatientColumn;

//    @Scheduled(cron = "* * * * * ?") //Run every
//    @Scheduled(cron = "0 */1 * * * ?") //Run every 1 minute
    @Scheduled(cron = "0 */10 * * * ?") //Run every 10 minutes
    public void doScheduleTask() {
        syncPatients();
        syncBookAppointment();
    }

    @Async
    void syncBookAppointment() {
        try {
            OkHttpClient client = new OkHttpClient();
            HttpUrl url = HttpUrl.parse(clinikoUrl).newBuilder()
                    .addPathSegment("v1")
                    .addPathSegment("individual_appointments")
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", getBasicAuthenticationHeader(clinikoApiKey, ""))
                    .addHeader("Accept", "application/json")
                    .build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                JSONObject responseJson = new JSONObject(response.body().string());
                JSONArray patientJsonArr = responseJson.getJSONArray("individual_appointments");
                patientJsonArr.forEach(element -> {
                    JSONObject jsonObject = (JSONObject) element;
                    String id = jsonObject.getString("id");
                    boolean isEmpty = BudibaseAPI.checkRowEmpty(budiBaseUrl, budiBaseApiKey, budiBaseAppId, budiBaseBookingTableId, budiBaseBookingColumn, id);
                    if (isEmpty) {
                        String firstName = "";
                        String lastName = "";
                        String email = "";
                        JsonObject objectAppointment = new JsonObject();
                        objectAppointment.addProperty("booking_id_cliniko", id);
                        objectAppointment.addProperty("startAt", jsonObject.getString("starts_at"));
                        objectAppointment.addProperty("endAt", jsonObject.getString("ends_at"));

                        this.setPractitioner(jsonObject, objectAppointment);
                        this.setType(jsonObject, objectAppointment);

                        String patientLink =  jsonObject.getJSONObject("patient").getJSONObject("links").getString("self");
                        String patientId = patientLink.split("/")[5];
                        objectAppointment.addProperty("patient_id_cliniko", patientId);

                        HttpUrl urlPatient = HttpUrl.parse(clinikoUrl).newBuilder()
                                .addPathSegment("v1")
                                .addPathSegment("patients")
                                .addPathSegment(patientId)
                                .build();
                        Request requestPatient = new Request.Builder()
                                .url(urlPatient)
                                .addHeader("Authorization", getBasicAuthenticationHeader(clinikoApiKey, ""))
                                .addHeader("Accept", "application/json")
                                .build();

                        try {
                            Response responsePatient = client.newCall(requestPatient).execute();
                            if (responsePatient.isSuccessful()) {
                                JSONObject responsePatientJson = new JSONObject(responsePatient.body().string());
                                firstName = responsePatientJson.getString("first_name");
                                lastName = responsePatientJson.getString("last_name");
                                email = !responsePatientJson.isNull("email")? responsePatientJson.getString("email") : "";
                                String gender = responsePatientJson.getString("sex");
                                String city = responsePatientJson.getString("city");
                                String mobilePhone = "";
                                String homePhone = "";

                                JSONArray patientPhoneNumbers = responsePatientJson.getJSONArray("patient_phone_numbers");
                                for (int i = 0; i < patientPhoneNumbers.length(); i++) {
                                    JSONObject phoneObj = patientPhoneNumbers.getJSONObject(i);
                                    if (phoneObj.getString("phone_type").equals("Mobile")) mobilePhone = phoneObj.getString("number");
                                    if (phoneObj.getString("phone_type").equals("Home")) homePhone = phoneObj.getString("number");
                                }

                                objectAppointment.addProperty("firstName", firstName);
                                objectAppointment.addProperty("lastName", lastName);
                                objectAppointment.addProperty("gender", gender);
                                objectAppointment.addProperty("city", city);
                                objectAppointment.addProperty("mobilePhone", mobilePhone);
                                objectAppointment.addProperty("homePhone", homePhone);

                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        BudibaseAPI.addRecord(budiBaseUrl, budiBaseApiKey, budiBaseAppId, budiBaseBookingTableId, objectAppointment.toString());
                        // send email greeting patient
                        if (!email.isEmpty()) {
                            mailService.sendMailWelcomePatient(patientId, firstName + " " + lastName, email);
                        }
                    }
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void setType(JSONObject jsonObjectBooking, JsonObject objectAppointment) {
        OkHttpClient client = new OkHttpClient();
        String typeLink =  jsonObjectBooking.getJSONObject("appointment_type").getJSONObject("links").getString("self");

        HttpUrl urlType = HttpUrl.parse(typeLink).newBuilder()
                .build();
        Request requestType = new Request.Builder()
                .url(urlType)
                .addHeader("Authorization", getBasicAuthenticationHeader(clinikoApiKey, ""))
                .addHeader("Accept", "application/json")
                .build();

        try {
            Response responseType = client.newCall(requestType).execute();
            if (responseType.isSuccessful()) {
                JSONObject responseTypeJson = new JSONObject(responseType.body().string());
                objectAppointment.addProperty("type", responseTypeJson.getString("name"));
                objectAppointment.addProperty("color", responseTypeJson.getString("color"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void setPractitioner(JSONObject jsonObjectBooking, JsonObject objectAppointment) {
        OkHttpClient client = new OkHttpClient();
        String practitionerLink =  jsonObjectBooking.getJSONObject("practitioner").getJSONObject("links").getString("self");

        HttpUrl urlPractitioner = HttpUrl.parse(practitionerLink).newBuilder()
                .build();
        Request requestPractitioner = new Request.Builder()
                .url(urlPractitioner)
                .addHeader("Authorization", getBasicAuthenticationHeader(clinikoApiKey, ""))
                .addHeader("Accept", "application/json")
                .build();

        try {
            Response responsePractitioner = client.newCall(requestPractitioner).execute();
            if (responsePractitioner.isSuccessful()) {
                JSONObject responsePractitionerJson = new JSONObject(responsePractitioner.body().string());
                objectAppointment.addProperty("practitioner", responsePractitionerJson.getString("label"));
                objectAppointment.addProperty("practitionerId", responsePractitionerJson.getString("id"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Async
    void syncPatients() {
        try {
            OkHttpClient client = new OkHttpClient();
            HttpUrl url = HttpUrl.parse(clinikoUrl).newBuilder()
                    .addPathSegment("v1")
                    .addPathSegment("patients")
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", getBasicAuthenticationHeader(clinikoApiKey, ""))
                    .addHeader("Accept", "application/json")
                    .build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                JSONObject responseJson = new JSONObject(response.body().string());
                JSONArray patientJsonArr = responseJson.getJSONArray("patients");
                patientJsonArr.forEach(element -> {
                    JSONObject jsonObject = (JSONObject) element;
                    String patientId = jsonObject.getString("id");
                    Boolean isEmpty = BudibaseAPI.checkRowEmpty(budiBaseUrl, budiBaseApiKey, budiBaseAppId, budiBasePatientTableId, budiBasePatientColumn, patientId);
                    if (isEmpty) {
                        JsonObject objectPatient = new JsonObject();
                        String firstName = jsonObject.getString("first_name");
                        String lastName = jsonObject.getString("last_name");
                        String email = !jsonObject.isNull("email")? jsonObject.getString("email") : "";
                        objectPatient.addProperty("title", jsonObject.getString("title"));
                        objectPatient.addProperty("firstName", firstName);
                        objectPatient.addProperty("lastName", lastName);
                        objectPatient.addProperty("email", email);

                        JSONArray patientPhoneNumbers = jsonObject.getJSONArray("patient_phone_numbers");
                        objectPatient.addProperty("patient_id_cliniko", patientId);
                        objectPatient.addProperty("gender", !jsonObject.isNull("sex") ?  jsonObject.getString("sex") : null);
                        objectPatient.addProperty("city", !jsonObject.isNull("city") ? jsonObject.getString("city") : null);
                        objectPatient.addProperty("dob", !jsonObject.isNull("date_of_birth") ? jsonObject.getString("date_of_birth") : null);
                        objectPatient.addProperty("address", jsonObject.getString("address_1") + jsonObject.getString("address_2") + jsonObject.getString("address_3"));
                        objectPatient.addProperty("state", jsonObject.getString("state"));
                        objectPatient.addProperty("postalCode", jsonObject.getString("post_code"));
                        objectPatient.addProperty("region", jsonObject.getString("country"));
                        objectPatient.addProperty("company", "");
                        objectPatient.addProperty("jobTitle", "");
                        objectPatient.addProperty("category", "");

                        String businessPhone = "";
                        String homePhone = "";
                        String mobilePhone = "";
                        String faxNumber = "";

                        for (int i = 0; i < patientPhoneNumbers.length(); i++) {
                            JSONObject phoneObj = patientPhoneNumbers.getJSONObject(i);
                            if (phoneObj.getString("phone_type").equals("Mobile")) mobilePhone = phoneObj.getString("number");
                            if (phoneObj.getString("phone_type").equals("Work")) businessPhone = phoneObj.getString("number");
                            if (phoneObj.getString("phone_type").equals("Home")) homePhone = phoneObj.getString("number");
                            if (phoneObj.getString("phone_type").equals("Fax")) faxNumber = phoneObj.getString("number");
                        }
                        objectPatient.addProperty("businessPhone", businessPhone);
                        objectPatient.addProperty("homePhone", homePhone);
                        objectPatient.addProperty("mobilePhone", mobilePhone);
                        objectPatient.addProperty("faxNumber", faxNumber);

                        objectPatient.addProperty("referringDr", "");
                        objectPatient.addProperty("referringAddress", "");
                        objectPatient.addProperty("medicareNo", jsonObject.getString("medicare"));
                        objectPatient.addProperty("privateHealthFund", jsonObject.getString("medicare"));
                        objectPatient.addProperty("healthFundNo", "");
                        objectPatient.addProperty("anotherField", "");
                        objectPatient.addProperty("hasSendEmail", "false");

                        BudibaseAPI.addRecord(budiBaseUrl, budiBaseApiKey, budiBaseAppId, budiBasePatientTableId, objectPatient.toString());
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

//    private String formatDate(String dateTime) {
//        Instant instant = Instant.parse(dateTime);
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/dd/yy HH:mm");
//        LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneOffset.ofHours(10));
//        return ldt.format(formatter);
//    }
}
