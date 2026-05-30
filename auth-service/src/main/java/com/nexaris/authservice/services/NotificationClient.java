package com.nexaris.authservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class NotificationClient {
    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestTemplate restTemplate;
    private final String gatewayUrl;
    private final String notificationEmailEndpoint;

    public NotificationClient(
            @Value("${app.gateway-url}") String gatewayUrl,
            @Value("${app.notification-email-endpoint}") String notificationEmailEndpoint
    ) {
        this.restTemplate = new RestTemplate();
        this.gatewayUrl = requireConfigured("app.gateway-url", gatewayUrl);
        this.notificationEmailEndpoint = requireConfigured("app.notification-email-endpoint", notificationEmailEndpoint);
    }

    public void sendWelcomeEmail(String recipientEmail, String firstName) {
        String target = normalizeBaseUrl(gatewayUrl) + normalizeEndpointPath(notificationEmailEndpoint);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = Map.of(
                "to", recipientEmail,
                "subject", "Bienvenue sur Nexaris",
                "body", "Bonjour " + firstName + ",\n\nBienvenue sur Nexaris. Votre compte est maintenant actif.",
                "html", false
        );

        try {
            restTemplate.postForEntity(target, new HttpEntity<>(payload, headers), Object.class);
        } catch (RestClientException ex) {
            // Non bloquant: l'absence de mail ne doit pas annuler la création du compte.
            log.warn("Impossible d'envoyer l'email de bienvenue a {}", recipientEmail, ex);
        }
    }

    private String normalizeBaseUrl(String rawBaseUrl) {
        return rawBaseUrl.endsWith("/") ? rawBaseUrl.substring(0, rawBaseUrl.length() - 1) : rawBaseUrl;
    }

    private String normalizeEndpointPath(String rawPath) {
        return rawPath.startsWith("/") ? rawPath : "/" + rawPath;
    }

    private String requireConfigured(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration: " + key);
        }
        return value;
    }
}
