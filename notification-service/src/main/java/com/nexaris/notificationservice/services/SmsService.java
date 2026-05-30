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
 * Exemple d'implémentation SMS.
 * Pour l'activer en production, configurer via variable d'environment : SMS_ENABLED=true
 * et ajouter les dépendances Twilio ou autre SDK SMS.
 */
@Service
@ConditionalOnProperty(name = "notification.sms.enabled", havingValue = "true", matchIfMissing = false)
public class SmsService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final NotificationRepository notificationRepository;

    public SmsService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public String getChannelType() {
        return "SMS";
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
            // Ici on ajouterait l'appel à Twilio, AWS SNS, ou autre provider SMS
            log.info("📱 Envoi SMS à {} : {}", recipient, message);
            // TODO: effectuer l'appel API au service SMS
            // Example: twilioClient.messages.create(...)

            notification.setStatus("SENT");
            notificationRepository.save(notification);
            return notification;
        } catch (Exception e) {
            log.error("Erreur envoi SMS vers {} : {}", recipient, e.getMessage(), e);
            notification.setStatus("FAILED");
            notificationRepository.save(notification);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Impossible d'envoyer le SMS");
        }
    }
}
