package com.nexaris.notificationservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client interne pour appeler auth-service via la gateway et récupérer les infos utilisateur (email).
 */
@Service
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final String gatewayUrl;
    private final String serviceToken;

    public AuthServiceClient(
            @Value("${app.gateway-url}") String gatewayUrl,
            @Value("${service.token}") String serviceToken) {
        this.gatewayUrl = requireConfigured("app.gateway-url", gatewayUrl);
        this.serviceToken = serviceToken;
    }

    /**
     * Récupère l'email d'un utilisateur via endpoint interne. Retourne null en cas d'erreur (non bloquant).
     */
    public String getUserEmail(Long userId) {
        InternalUserProfile profile = getUserProfile(userId);
        return profile != null ? profile.email() : null;
    }

    public InternalUserProfile getUserProfile(Long userId) {
        String url = normalizeBase(gatewayUrl) + "/api/v1/auth/internal/user/" + userId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Service-Token", serviceToken);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            if (response.getBody() != null) {
                Object email = response.getBody().get("email");
                Object languageCode = response.getBody().get("languageCode");
                return new InternalUserProfile(
                        email != null ? email.toString() : null,
                        languageCode != null ? languageCode.toString() : null
                );
            }
        } catch (RestClientException ex) {
            log.warn("Impossible de récupérer le profil de l'utilisateur {} : {}", userId, ex.getMessage());
        }
        return null;
    }

    public record InternalUserProfile(String email, String languageCode) {}

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
