package com.nexaris.planningservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client interne pour appeler notification-service et envoyer des notifications.
 */
@Service
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final String gatewayUrl;

    public NotificationClient(
            @Value("${app.gateway-url}") String gatewayUrl) {
        this.gatewayUrl = requireConfigured("app.gateway-url", gatewayUrl);
    }

    /**
     * Envoie une notification à une liste d'utilisateurs (non bloquant).
     * Le message doit être déjà formaté par l'appelant.
     */
    public void sendNotification(List<Integer> userIds, String title, String message, String link) {
        sendNotification(userIds, title, message, link, "GENERAL");
    }

    public void sendNotification(List<Integer> userIds,
                                 String title,
                                 String message,
                                 String link,
                                 String notificationType) {
        sendNotification(userIds, title, message, link, notificationType, Map.of());
    }

    public void sendNotification(List<Integer> userIds,
                                 String title,
                                 String message,
                                 String link,
                                 String notificationType,
                                 Map<String, Object> templateParams) {
        if (userIds == null || userIds.isEmpty()) return;

        String url = normalizeBase(gatewayUrl) + "/api/v1/notifications/send-bulk";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Convertir Integer -> Long
        List<Long> longIds = userIds.stream().map(Integer::longValue).toList();

        Map<String, Object> payload = new HashMap<>();
        payload.put("userIds", longIds);
        payload.put("title", title != null ? title : "Notification");
        payload.put("message", message != null ? message : "");
        payload.put("link", link != null ? link : "");
        payload.put("notificationType", notificationType != null && !notificationType.isBlank()
                ? notificationType
                : "GENERAL");
        payload.put("templateParams", templateParams != null ? templateParams : Map.of());

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), Void.class);
            log.info("Notification envoyee a {} utilisateur(s)", longIds.size());
        } catch (RestClientException ex) {
            log.warn("Impossible d'envoyer la notification : {}", ex.getMessage());
        }
    }

    public void sendNotification(List<Integer> userIds,
                                 String link,
                                 String notificationType,
                                 Map<String, Object> templateParams) {
        sendNotification(userIds, "Notification", "", link, notificationType, templateParams);
    }

    private String normalizeBase(String raw) {
        return raw.endsWith("/") ? raw.substring(0, raw.length() - 1) : raw;
    }

    private String requireConfigured(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration: " + key);
        }
        return value;
    }
}
