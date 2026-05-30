package com.nexaris.notificationservice.services;

import com.nexaris.notificationservice.dtos.ChannelPreferenceDto;
import com.nexaris.notificationservice.entities.NotificationChannelPreference;
import com.nexaris.notificationservice.repositories.NotificationChannelPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class ChannelPreferenceService {

    private final NotificationChannelPreferenceRepository repository;

    public ChannelPreferenceService(NotificationChannelPreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ChannelPreferenceDto getPreferences(Long userId) {
        return repository.findByUserId(userId)
                .map(this::toDto)
                .orElseGet(this::defaultPreferences);
    }

    @Transactional
    public ChannelPreferenceDto savePreferences(Long userId, ChannelPreferenceDto dto) {
        NotificationChannelPreference pref = repository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationChannelPreference p = new NotificationChannelPreference();
                    p.setUserId(userId);
                    return p;
                });
        pref.setEmailEnabled(dto.isEmailEnabled());
        pref.setInAppEnabled(dto.isInAppEnabled());
        pref.setNotificationsEnabled(dto.isNotificationsEnabled());
        pref.setExternalEnabled(dto.isExternalEnabled());
        
        // Sauvegarder les canaux dynamiques
        if (dto.getChannels() != null && !dto.getChannels().isEmpty()) {
            pref.setChannelPreferences(normalizeChannelPreferences(dto.getChannels()));
        }

        // Sauvegarder les préférences de types d'événements
        if (dto.getEventTypes() != null && !dto.getEventTypes().isEmpty()) {
            pref.setEventPreferences(normalizeEventPreferences(dto.getEventTypes()));
        }
        
        pref.setUpdatedAt(LocalDateTime.now());
        repository.save(pref);
        return toDto(pref);
    }

    @Transactional(readOnly = true)
    public NotificationChannelPreference getOrDefault(Long userId) {
        return repository.findByUserId(userId).orElseGet(() -> {
            NotificationChannelPreference p = new NotificationChannelPreference();
            p.setUserId(userId);
            p.setEmailEnabled(true);
            p.setInAppEnabled(true);
            p.setNotificationsEnabled(true);
            p.setExternalEnabled(true);
            p.setChannelPreferences(new HashMap<>());
            p.setEventPreferences(new HashMap<>());
            return p;
        });
    }

    /**
     * Récupère la préférence pour un canal spécifique.
     * Retourne true par défaut si le canal n'existe pas.
     */
    @Transactional(readOnly = true)
    public boolean isChannelEnabled(Long userId, String channelName) {
        NotificationChannelPreference pref = getOrDefault(userId);
        return pref.isChannelEnabled(channelName);
    }

    /**
     * Définit la préférence pour un canal spécifique.
     */
    @Transactional
    public void setChannelEnabled(Long userId, String channelName, boolean enabled) {
        NotificationChannelPreference pref = repository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationChannelPreference p = new NotificationChannelPreference();
                    p.setUserId(userId);
                    return p;
                });
        pref.setChannelEnabled(channelName, enabled);
        pref.setUpdatedAt(LocalDateTime.now());
        repository.save(pref);
    }

    private ChannelPreferenceDto toDto(NotificationChannelPreference p) {
        ChannelPreferenceDto dto = new ChannelPreferenceDto();
        dto.setEmailEnabled(p.isEmailEnabled());
        dto.setInAppEnabled(p.isInAppEnabled());
        dto.setNotificationsEnabled(p.isNotificationsEnabled());
        dto.setExternalEnabled(p.isExternalEnabled());
        dto.setChannels(new HashMap<>(p.getChannelPreferences()));
        dto.setEventTypes(new HashMap<>(p.getEventPreferences()));
        return dto;
    }

    private ChannelPreferenceDto defaultPreferences() {
        ChannelPreferenceDto dto = new ChannelPreferenceDto();
        dto.setEmailEnabled(true);
        dto.setInAppEnabled(true);
        dto.setNotificationsEnabled(true);
        dto.setExternalEnabled(true);
        dto.setChannels(new HashMap<>());
        dto.setEventTypes(new HashMap<>());
        return dto;
    }

    private Map<String, Boolean> normalizeEventPreferences(Map<String, Boolean> eventPreferences) {
        Map<String, Boolean> normalized = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : eventPreferences.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            String key = entry.getKey().trim().toUpperCase(Locale.ROOT);
            normalized.put(key, Boolean.TRUE.equals(entry.getValue()));
        }
        return normalized;
    }

    private Map<String, Boolean> normalizeChannelPreferences(Map<String, Boolean> channelPreferences) {
        Map<String, Boolean> normalized = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : channelPreferences.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            String key = entry.getKey().trim().toUpperCase(Locale.ROOT);
            normalized.put(key, Boolean.TRUE.equals(entry.getValue()));
        }
        return normalized;
    }
}
