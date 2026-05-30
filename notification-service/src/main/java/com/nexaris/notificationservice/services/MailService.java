package com.nexaris.notificationservice.services;

import com.nexaris.notificationservice.dtos.EmailRequest;
import com.nexaris.notificationservice.entities.NotificationEntity;
import com.nexaris.notificationservice.repositories.NotificationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class MailService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final EmailSettingsService emailSettingsService;
    private final NotificationRepository notificationRepository;

    public MailService(EmailSettingsService emailSettingsService,
                       NotificationRepository notificationRepository) {
        this.emailSettingsService = emailSettingsService;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public String getChannelType() {
        return "EMAIL";
    }

    @Override
    @Transactional
    public NotificationEntity send(String recipient, String subject, String message, String htmlContent) {
        return send(recipient, subject, message, htmlContent, null);
    }

    @Transactional
    public NotificationEntity send(String recipient, String subject, String message, String htmlContent, String requestedFrom) {
        NotificationEntity notification = new NotificationEntity();
        notification.setRecipient(recipient);
        notification.setType(getChannelType());
        notification.setTitle(subject);
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setStatus("PENDING");
        notificationRepository.save(notification);

        try {
            JavaMailSenderImpl mailSender = emailSettingsService.buildMailSender();
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            String sender = resolveFrom(requestedFrom);

            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlContent != null ? htmlContent : message, htmlContent != null);
            helper.setFrom(sender);

            mailSender.send(mimeMessage);
            notification.setStatus("SENT");
            notificationRepository.save(notification);
            return notification;
        } catch (MessagingException | RuntimeException e) {
            log.error("Erreur envoi email vers {} : {}", recipient, e.getMessage(), e);
            notification.setStatus("FAILED");
            notificationRepository.save(notification);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Impossible d'envoyer l'email");
        }
    }

    @Transactional
    public NotificationEntity sendEmail(EmailRequest request) {
        return send(request.getTo(), request.getSubject(), request.getBody(),
                request.isHtml() ? request.getBody() : null,
                request.getFrom());
    }

    private String resolveFrom(String requestedFrom) {
        if (requestedFrom == null || requestedFrom.isBlank()) {
            var settings = emailSettingsService.getSettings();
            return settings != null ? settings.getFromAddress() : "no-reply@noreply.local";
        }
        return requestedFrom;
    }
}
