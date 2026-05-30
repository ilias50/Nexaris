package com.nexaris.notificationservice.services;

import com.nexaris.notificationservice.dtos.BulkNotificationRequest;
import com.nexaris.notificationservice.dtos.SendNotificationRequest;
import com.nexaris.notificationservice.entities.NotificationChannelPreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserNotificationService {

    private static final Logger log = LoggerFactory.getLogger(UserNotificationService.class);
    private static final String DEFAULT_NOTIFICATION_TYPE = "GENERAL";
    private static final List<String> AVAILABLE_NOTIFICATION_TYPES = List.of(
        "GENERAL",
        "PLANNING_MEETING_CREATED",
        "PLANNING_MEETING_UPDATED",
        "PLANNING_MEETING_CANCELLED",
        "PLANNING_ENTRY_CREATED",
        "PLANNING_ENTRY_UPDATED",
        "PLANNING_ENTRY_DELETED",
        "PLANNING_ENTRY_ASSIGNED",
        "ORG_ANNOUNCEMENT_CREATED",
        "ORG_ANNOUNCEMENT_UPDATED",
        "ORG_ANNOUNCEMENT_DELETED",
        "ORG_ANNOUNCEMENT"
    );

    private final ChannelPreferenceService channelPreferenceService;
    private final InAppNotificationService inAppNotificationService;
    private final AuthServiceClient authServiceClient;
    private final NotificationTemplateService notificationTemplateService;
    private final NotificationDispatcher notificationDispatcher;

    public UserNotificationService(ChannelPreferenceService channelPreferenceService,
                                   InAppNotificationService inAppNotificationService,
                                   AuthServiceClient authServiceClient,
                                   NotificationTemplateService notificationTemplateService,
                                   NotificationDispatcher notificationDispatcher) {
        this.channelPreferenceService = channelPreferenceService;
        this.inAppNotificationService = inAppNotificationService;
        this.authServiceClient = authServiceClient;
        this.notificationTemplateService = notificationTemplateService;
        this.notificationDispatcher = notificationDispatcher;
    }

    public void sendBulk(BulkNotificationRequest request) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            return;
        }

        String fallbackSubject = request.getTitle() != null ? request.getTitle() : "Notification";
        String fallbackMessage = request.getMessage() != null ? request.getMessage() : "";
        String link = request.getLink() != null ? request.getLink() : "";
        String notificationType = normalizeNotificationType(request.getNotificationType());
        Map<String, Object> templateParams = request.getTemplateParams() != null ? request.getTemplateParams() : Map.of();
        
        // Récupérer les channels demandés (par défaut EMAIL si non spécifiés)
        List<String> requestedChannels = request.getChannels() != null && !request.getChannels().isEmpty()
            ? request.getChannels()
            : java.util.List.of("EMAIL");

        for (Long userId : request.getUserIds()) {
            try {
                NotificationChannelPreference pref = channelPreferenceService.getOrDefault(userId);
            AuthServiceClient.InternalUserProfile userProfile = authServiceClient.getUserProfile(userId);
            String userLanguage = userProfile != null ? userProfile.languageCode() : null;
            NotificationTemplateService.RenderedNotification rendered = notificationTemplateService.render(
                notificationType,
                userLanguage,
                fallbackSubject,
                fallbackMessage,
                templateParams
            );
            String subject = rendered.subject();
            String message = rendered.message();

                if (!pref.isNotificationsEnabled()) {
                    log.debug("Notifications totalement désactivées pour userId {}", userId);
                    continue;
                }

                if (!pref.isEventTypeEnabled(notificationType)) {
                    log.debug("Type de notification {} désactivé pour userId {}", notificationType, userId);
                    continue;
                }
                
                // Les notifications in-app respectent explicitement la préférence utilisateur.
                if (pref.isInAppEnabled()) {
                    sendInAppNotification(userId, subject, message, link);
                } else {
                    log.debug("In-app notifications désactivées pour userId {}", userId);
                }
                
                if (!pref.isExternalEnabled()) {
                    log.debug("Canaux externes désactivés pour userId {}", userId);
                    continue;
                }

                // Envoyer par chaque channel demandé (EMAIL par défaut)
                for (String channel : requestedChannels) {
                    // Ne pas envoyer IN_APP deux fois
                    if (channel.equalsIgnoreCase("IN_APP")) {
                        continue;
                    }
                    
                    if ("EMAIL".equalsIgnoreCase(channel) && pref.isEmailEnabled()) {
                        String email = userProfile != null ? userProfile.email() : null;
                        if ((email == null || email.isBlank())) {
                            email = authServiceClient.getUserEmail(userId);
                        }
                        sendEmailNotification(email, subject, message, channel);
                    } else if (!channel.equalsIgnoreCase("EMAIL") && pref.isChannelEnabled(channel)) {
                        // Approche générique pour tous les canaux dynamiques
                        sendChannelNotification(userId, subject, message, channel);
                    }
                }
            } catch (RuntimeException ex) {
                log.warn("Echec d'envoi de notification pour userId {}: {}", userId, ex.getMessage());
            }
        }
    }

    public List<String> getAvailableNotificationTypes() {
        return AVAILABLE_NOTIFICATION_TYPES;
    }

    private String normalizeNotificationType(String notificationType) {
        if (notificationType == null || notificationType.isBlank()) {
            return DEFAULT_NOTIFICATION_TYPE;
        }
        return notificationType.trim().toUpperCase(Locale.ROOT);
    }
    
    @Transactional
    private void sendInAppNotification(Long userId, String subject, String message, String link) {
        inAppNotificationService.create(userId, subject, message, link);
    }
    
    @Transactional
    private void sendEmailNotification(String email, String subject, String message, String channel) {
        if (email == null || email.isBlank()) {
            return;
        }
        SendNotificationRequest sendRequest = new SendNotificationRequest();
        sendRequest.setChannel(channel);
        sendRequest.setRecipient(email);
        sendRequest.setSubject(subject);
        sendRequest.setMessage(message);
        notificationDispatcher.send(sendRequest);
    }
    
    @Transactional
    private void sendChannelNotification(Long userId, String subject, String message, String channel) {
        SendNotificationRequest sendRequest = new SendNotificationRequest();
        sendRequest.setChannel(channel);
        sendRequest.setRecipient(String.valueOf(userId));
        sendRequest.setSubject(subject);
        sendRequest.setMessage(message);
        notificationDispatcher.send(sendRequest);
    }
}