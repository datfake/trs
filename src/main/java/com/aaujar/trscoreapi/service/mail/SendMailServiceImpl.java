package com.aaujar.trscoreapi.service.mail;

import com.aaujar.trscoreapi.model.mail.Attachment;
import com.aaujar.trscoreapi.model.mail.EmailBean;
import com.google.common.io.Files;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

//import javax.mail.Message;
//import javax.mail.Multipart;
//import javax.mail.internet.InternetAddress;
//import javax.mail.internet.MimeBodyPart;
//import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Service
public class SendMailServiceImpl implements SendMailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Override
    public void sendMail(EmailBean emailBean) {
        File tempDir = null;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            Multipart contentPart = new MimeMultipart();

            MimeBodyPart bodyPart = new MimeBodyPart();
            bodyPart.setText(emailBean.getBody(), "UTF-8", getSubType(emailBean.getMimeType()));
            contentPart.addBodyPart(bodyPart);

            if (emailBean.getAttachments() != null) {
                tempDir = Files.createTempDir();
                for (Attachment attachment : emailBean.getAttachments()) {
                    // Create attachment file in temporary directory
                    byte[] attachmentContent = Base64.getDecoder().decode(attachment.getContent());
                    File attachmentFile = new File(tempDir, attachment.getFileName());
                    Files.write(attachmentContent, attachmentFile);

                    // Attach the attachment file to email
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    attachmentPart.attachFile(attachmentFile);
                    attachmentPart.setContentID("<" + attachment.getContentId() + ">");
                    contentPart.addBodyPart(attachmentPart);
                }
            }

            message.setContent(contentPart);
            message.setSubject(emailBean.getSubject(), "UTF-8");
            message.setSentDate(new Date());
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(emailBean.getEmailAddressList()));

            if (emailBean.getReplyTo() != null) {
                message.setReplyTo(InternetAddress.parse(emailBean.getReplyTo()));
            }
            mailSender.send(message);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tempDir != null) {
                try {
                    FileUtils.deleteDirectory(tempDir);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    @Override
    public void sendMailWelcomePatient(String patientId, String fullName, String email) {
        try {
            // (1) Loading template
            Context myContext = new Context();
            myContext.setVariable("link", "https://hktest.budibase.app/embed/trs#/getting-started/patientId=" + patientId + "&name=" + URLEncoder.encode("Hello World", "UTF-8").replace("+", "%20"));
            myContext.setVariable("fullName", fullName);

            String htmlTemplate = templateEngine.process("welcome-patient", myContext);

            // (2) Send email
            EmailBean bean = new EmailBean();
            bean.setMimeType(MediaType.TEXT_HTML.toString());
            bean.setBody(htmlTemplate);
            bean.setTo(new String[]{email});
            bean.setSubject("Greeting Patient");
            this.sendMail(bean);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void sendMailWelcomeUser(String url, String fullName, String email, String token) {
        try {
            // (1) Loading template
            Context myContext = new Context();
            myContext.setVariable("link", url + "/#/reset-password?token=" + token + "&email=" + email);
            myContext.setVariable("fullName", fullName);

            String htmlTemplate = templateEngine.process("welcome-user", myContext);

            // (2) Send email
            EmailBean bean = new EmailBean();
            bean.setMimeType(MediaType.TEXT_HTML.toString());
            bean.setBody(htmlTemplate);
            bean.setTo(new String[]{email});
            bean.setSubject("Greeting User");
            this.sendMail(bean);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String getSubType(String mimeType) {
        return mimeType.substring(mimeType.lastIndexOf("/") + 1);
    }
}
