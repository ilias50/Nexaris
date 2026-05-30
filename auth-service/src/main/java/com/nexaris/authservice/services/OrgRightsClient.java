package com.nexaris.authservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class OrgRightsClient {

    private static final Logger log = LoggerFactory.getLogger(OrgRightsClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final String gatewayUrl;
    private final String serviceToken;

    public OrgRightsClient(
            @Value("${app.gateway-url}") String gatewayUrl,
            @Value("${service.token}") String serviceToken) {
        this.gatewayUrl = requireConfigured("app.gateway-url", gatewayUrl);
        this.serviceToken = serviceToken;
    }

    public void purgeUserRights(Integer userId) {
        String url = normalizeBase(gatewayUrl) + "/api/v1/org/internal/users/" + userId + "/purge-rights";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Service-Token", serviceToken);

        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers), Void.class);
        } catch (RestClientException ex) {
            log.error("Echec de purge des droits org pour userId={}", userId, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La purge des droits organisationnels a échoué.");
        }
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
