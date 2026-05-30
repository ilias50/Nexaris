package com.nexaris.notificationservice.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Convertisseur JPA pour stocker les préférences de canaux en JSON.
 * Permet d'ajouter facilement de nouveaux canaux sans migration de schéma.
 */
@Converter(autoApply = true)
public class ChannelPreferencesConverter implements AttributeConverter<Map<String, Boolean>, String> {

    private static final Logger log = LoggerFactory.getLogger(ChannelPreferencesConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Boolean> attribute) {
        if (attribute == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException ex) {
            log.warn("Erreur de sérialisation des préférences de canaux : {}", ex.getMessage());
            return "{}";
        }
    }

    @Override
    public Map<String, Boolean> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<Map<String, Boolean>>() {});
        } catch (JsonProcessingException ex) {
            log.warn("Erreur de désérialisation des préférences de canaux : {}", ex.getMessage());
            return new HashMap<>();
        }
    }
}
