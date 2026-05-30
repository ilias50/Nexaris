package com.nexaris.notificationservice.dtos;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public class BulkNotificationRequest {

    @NotEmpty(message = "La liste des utilisateurs est obligatoire")
    private List<Long> userIds;

    private String title;

    private String message;

    private String notificationType;

    private Map<String, Object> templateParams;

    private String link;
    
    private List<String> channels;

    public BulkNotificationRequest() {}

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
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

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public Map<String, Object> getTemplateParams() {
        return templateParams;
    }

    public void setTemplateParams(Map<String, Object> templateParams) {
        this.templateParams = templateParams;
    }

    public String getLink() {
        return link;
    }
    
    public List<String> getChannels() {
        return channels;
    }
    
    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
