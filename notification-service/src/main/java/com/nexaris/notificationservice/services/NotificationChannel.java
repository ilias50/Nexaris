package com.nexaris.notificationservice.services;

import com.nexaris.notificationservice.entities.NotificationEntity;

/**
 * Interface pour tous les canaux de notification.
 * Permet d'ajouter facilement de nouveaux canaux (SMS, Discord, Slack, etc.)
 */
public interface NotificationChannel {

    /**
     * @return type du channel (EMAIL, SMS, DISCORD, SLACK, etc.)
     */
    String getChannelType();

    /**
     * Envoie une notification via ce canal
     * @param recipient destinataire (email, téléphone, username, etc.)
     * @param subject sujet/titre du message
     * @param message message en texte brut
     * @param htmlContent contenu HTML optionnel (null si pas applicable)
     * @return entité de notification persistée
     */
    NotificationEntity send(String recipient, String subject, String message, String htmlContent);
}
