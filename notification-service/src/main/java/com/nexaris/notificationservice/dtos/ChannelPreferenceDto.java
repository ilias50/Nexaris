package com.nexaris.notificationservice.dtos;

import java.util.HashMap;
import java.util.Map;

public class ChannelPreferenceDto {
    private boolean notificationsEnabled = true;
    private boolean externalEnabled = true;
    private boolean emailEnabled = true;
    private boolean inAppEnabled = true;
    private Map<String, Boolean> channels = new HashMap<>();
    private Map<String, Boolean> eventTypes = new HashMap<>();

    public ChannelPreferenceDto() {}

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public boolean isExternalEnabled() {
        return externalEnabled;
    }

    public void setExternalEnabled(boolean externalEnabled) {
        this.externalEnabled = externalEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    public void setInAppEnabled(boolean inAppEnabled) {
        this.inAppEnabled = inAppEnabled;
    }

    public Map<String, Boolean> getChannels() {
        return channels != null ? channels : new HashMap<>();
    }

    public void setChannels(Map<String, Boolean> channels) {
        this.channels = channels;
    }

    public Map<String, Boolean> getEventTypes() {
        return eventTypes != null ? eventTypes : new HashMap<>();
    }

    public void setEventTypes(Map<String, Boolean> eventTypes) {
        this.eventTypes = eventTypes;
    }

    /**
     * Ajoute une préférence de canal.
     */
    public void addChannelPreference(String channelName, boolean enabled) {
        if (channels == null) {
            channels = new HashMap<>();
        }
        channels.put(channelName, enabled);
    }

    /**
     * Ajoute une préférence de type d'événement.
     */
    public void addEventTypePreference(String eventType, boolean enabled) {
        if (eventTypes == null) {
            eventTypes = new HashMap<>();
        }
        eventTypes.put(eventType, enabled);
    }
}
