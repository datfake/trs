package com.aaujar.trscoreapi.model.mail;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class EmailBean {
    private String[] to;

    private String replyTo;
    private String mimeType;

    private String body;

    private String subject;
    private List<Attachment> attachments;

    public String[] getTo() {
        return to;
    }

    public void setTo(String[] to) {
        this.to = to;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    /**
     * Returns the email address list
     *
     * @return
     */
    public String getEmailAddressList() {
        return StringUtils.join(to, ",");
    }
}
