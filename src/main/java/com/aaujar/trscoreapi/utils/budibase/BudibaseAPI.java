package com.aaujar.trscoreapi.utils.budibase;

import com.squareup.okhttp.*;
import lombok.extern.log4j.Log4j2;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

@Log4j2
public class BudibaseAPI {
    public static Boolean checkRowEmpty(String budibaseUrl, String apiKey, String appId, String tableId, String key, String value) {
        OkHttpClient client = new OkHttpClient();

        String jsonBody = "{\"query\":{\"string\":{\"" + key + "\":\"" + value + "\"}}}";
        String url = budibaseUrl +"/tables/" + tableId + "/rows/search";
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/json"), jsonBody))
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json")
                .addHeader("x-budibase-api-key", apiKey)
                .addHeader("x-budibase-app-id", appId)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                JSONObject responseJson = new JSONObject(response.body().string());
                JSONArray datas = responseJson.getJSONArray("data");
                if (datas.isEmpty()) return true;
            } else {
                log.error("500 Internal Server Error From BudiBase");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void addRecord(String budibaseUrl, String apiKey, String appId, String tableId, String jsonBody) {
        OkHttpClient client = new OkHttpClient();

        String url = budibaseUrl + "/tables/" + tableId + "/rows";
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/json"), jsonBody))
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json")
                .addHeader("x-budibase-api-key", apiKey)
                .addHeader("x-budibase-app-id", appId)
                .build();

        try {
            Response response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
