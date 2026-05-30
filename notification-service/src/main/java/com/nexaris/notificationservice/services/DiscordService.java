package com.nexaris.notificationservice.services;

import com.nexaris.notificationservice.entities.NotificationEntity;
import com.nexaris.notificationservice.repositories.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Exemple d'implémentation Discord.
 * Pour l'activer en production, configurer : DISCORD_ENABLED=true et DISCORD_WEBHOOK_URL=...
 * Puis ajouter dépendances Discord (JDA ou autre lib).
 */
@Service
@ConditionalOnProperty(name = "notification.discord.enabled", havingValue = "true", matchIfMissing = false)
public class DiscordService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(DiscordService.class);

    private final NotificationRepository notificationRepository;

    public DiscordService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public String getChannelType() {
        return "DISCORD";
    }

    @Override
    @Transactional
    public NotificationEntity send(String recipient, String subject, String message, String htmlContent) {
        NotificationEntity notification = new NotificationEntity();
        notification.setRecipient(recipient);
        notification.setType(getChannelType());
        notification.setTitle(subject);
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setStatus("PENDING");
        notificationRepository.save(notification);

        try {
            // Ici on ajouterait l'appel à Discord Webhook ou JDA API
            log.info("🎮 Envoi notification Discord à {} : {}", recipient, message);
            // TODO: effectuer l'appel API Discord
            // Example: webhookClient.send(message)

            notification.setStatus("SENT");
            notificationRepository.save(notification);
            return notification;
        } catch (Exception e) {
            log.error("Erreur envoi Discord à {} : {}", recipient, e.getMessage(), e);
            notification.setStatus("FAILED");
            notificationRepository.save(notification);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Impossible d'envoyer la notification Discord");
        }
    }
}
