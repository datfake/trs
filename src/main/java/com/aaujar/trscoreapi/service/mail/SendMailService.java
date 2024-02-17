package com.aaujar.trscoreapi.service.mail;

import com.aaujar.trscoreapi.model.mail.EmailBean;

public interface SendMailService {
    void sendMail(EmailBean emailBean);

    void sendMailWelcomePatient(String patientId, String fullName, String email);

    void sendMailWelcomeUser(String url, String fullName, String email, String token);
}
