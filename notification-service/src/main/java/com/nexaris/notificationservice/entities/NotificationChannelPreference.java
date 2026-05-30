package com.nexaris.notificationservice.entities;

import com.nexaris.notificationservice.config.ChannelPreferencesConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Entity
@Table(name = "notification_channel_preferences")
public class NotificationChannelPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "notifications_enabled")
    private boolean notificationsEnabled = true;

    @Column(name = "external_enabled")
    private boolean externalEnabled = true;

    @Column(name = "email_enabled")
    private boolean emailEnabled = true;

    @Column(name = "in_app_enabled")
    private boolean inAppEnabled = true;

    @Column(name = "channel_preferences", columnDefinition = "TEXT")
    @Convert(converter = ChannelPreferencesConverter.class)
    private Map<String, Boolean> channelPreferences = new HashMap<>();

    @Column(name = "event_preferences", columnDefinition = "TEXT")
    @Convert(converter = ChannelPreferencesConverter.class)
    private Map<String, Boolean> eventPreferences = new HashMap<>();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public NotificationChannelPreference() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

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

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    public void setInAppEnabled(boolean inAppEnabled) {
        this.inAppEnabled = inAppEnabled;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Boolean> getChannelPreferences() {
        return channelPreferences != null ? channelPreferences : new HashMap<>();
    }

    public void setChannelPreferences(Map<String, Boolean> channelPreferences) {
        this.channelPreferences = channelPreferences;
    }

    public Map<String, Boolean> getEventPreferences() {
        return eventPreferences != null ? eventPreferences : new HashMap<>();
    }

    public void setEventPreferences(Map<String, Boolean> eventPreferences) {
        this.eventPreferences = eventPreferences;
    }

    /**
     * Récupère la préférence pour un canal spécifique.
     * Retourne true par défaut si le canal n'existe pas.
     */
    public boolean isChannelEnabled(String channelName) {
        return getChannelPreferences().getOrDefault(normalizePreferenceKey(channelName), true);
    }

    /**
     * Définit la préférence pour un canal spécifique.
     */
    public void setChannelEnabled(String channelName, boolean enabled) {
        if (channelPreferences == null) {
            channelPreferences = new HashMap<>();
        }
        channelPreferences.put(normalizePreferenceKey(channelName), enabled);
    }

    /**
     * Récupère la préférence pour un type d'événement.
     * Retourne true par défaut si le type n'existe pas.
     */
    public boolean isEventTypeEnabled(String eventType) {
        return getEventPreferences().getOrDefault(normalizePreferenceKey(eventType), true);
    }

    /**
     * Définit la préférence pour un type d'événement.
     */
    public void setEventTypeEnabled(String eventType, boolean enabled) {
        if (eventPreferences == null) {
            eventPreferences = new HashMap<>();
        }
        eventPreferences.put(normalizePreferenceKey(eventType), enabled);
    }

    private String normalizePreferenceKey(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
