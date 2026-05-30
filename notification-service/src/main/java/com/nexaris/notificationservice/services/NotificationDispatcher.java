package com.nexaris.notificationservice.services;

import com.nexaris.notificationservice.dtos.SendNotificationRequest;
import com.nexaris.notificationservice.entities.NotificationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Service central pour dispatcher les notifications vers le bon canal.
 * Permet d'ajouter de nouvelles implémentations sans modifier ce service.
 */
@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final Map<String, NotificationChannel> channels;

    public NotificationDispatcher(List<NotificationChannel> channelList) {
        this.channels = channelList.stream()
            .collect(Collectors.toMap(
                ch -> ch.getChannelType().toUpperCase(Locale.ROOT),
                ch -> ch,
                (existing, duplicate) -> {
                    throw new IllegalStateException("Duplicate notification channel type: "
                        + duplicate.getChannelType());
                }
            ));
        log.info("📬 Canaux de notification disponibles : {}", channels.keySet());
    }

    /**
     * Envoie une notification sur le canal spécifié
     */
    public NotificationEntity send(SendNotificationRequest request) {
        String channel = request.getChannel().toUpperCase(Locale.ROOT);
        NotificationChannel notificationChannel = channels.get(channel);

        if (notificationChannel == null) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Canal de notification '" + channel + "' n'existe pas. " +
                    "Canaux disponibles : " + channels.keySet());
        }

        log.info("📤 Envoi notification via {} à {}", channel, request.getRecipient());
        return notificationChannel.send(
                request.getRecipient(),
                request.getSubject(),
                request.getMessage(),
                request.getHtmlContent()
        );
    }

    /**
     * Liste tous les canaux disponibles
     */
    public List<String> getAvailableChannels() {
        return channels.keySet().stream().sorted().toList();
    }
}
