package com.nexaris.notificationservice.dtos;

import jakarta.validation.constraints.NotBlank;

public class SendNotificationRequest {

    @NotBlank(message = "Le canal est obligatoire (EMAIL, SMS, DISCORD, etc.)")
    private String channel;

    @NotBlank(message = "Le destinataire est obligatoire")
    private String recipient;

    @NotBlank(message = "Le sujet est obligatoire")
    private String subject;

    @NotBlank(message = "Le message est obligatoire")
    private String message;

    private String htmlContent;

    public SendNotificationRequest() {}

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }
}
