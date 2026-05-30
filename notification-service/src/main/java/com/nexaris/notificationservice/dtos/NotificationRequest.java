package com.nexaris.notificationservice.dtos;

import jakarta.validation.constraints.NotBlank;

public class NotificationRequest {

    @NotBlank(message = "Le destinataire est obligatoire")
    private String recipient;

    @NotBlank(message = "Le type de notification est obligatoire")
    private String type;

    @NotBlank(message = "Le titre est obligatoire")
    private String title;

    @NotBlank(message = "Le message est obligatoire")
    private String message;

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
