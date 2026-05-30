package com.nexaris.notificationservice.services;

import com.nexaris.notificationservice.dtos.EmailSettingsDto;
import com.nexaris.notificationservice.entities.EmailSettings;
import com.nexaris.notificationservice.repositories.EmailSettingsRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Properties;

/**
 * Gère la configuration SMTP stockée en base de données.
 * Au démarrage, si aucune configuration n'existe, les variables d'environnement
 * (spring.mail.*) servent de valeurs initiales.
 */
@Service
public class EmailSettingsService {

    private static final Logger log = LoggerFactory.getLogger(EmailSettingsService.class);

    private final EmailSettingsRepository repository;

    @Value("${spring.mail.host:}")
    private String envHost;

    @Value("${spring.mail.port:587}")
    private int envPort;

    @Value("${spring.mail.username:}")
    private String envUsername;

    @Value("${spring.mail.password:}")
    private String envPassword;

    @Value("${notification.mail.default-from:no-reply@noreply.local}")
    private String envFromAddress;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean envSmtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean envStarttls;

    @Value("${spring.mail.properties.mail.smtp.ssl.trust:}")
    private String envSslTrust;

    public EmailSettingsService(EmailSettingsRepository repository) {
        this.repository = repository;
    }

    /**
     * Si la table est vide au démarrage, on insère une ligne initialisée
     * depuis les variables d'environnement pour que l'admin ait une base.
     */
    @PostConstruct
    @Transactional
    public void seedFromEnvIfEmpty() {
        if (repository.count() == 0 && !envHost.isBlank()) {
            EmailSettings settings = new EmailSettings();
            settings.setHost(envHost);
            settings.setPort(envPort);
            settings.setUsername(envUsername);
            settings.setPassword(envPassword);
            settings.setFromAddress(envFromAddress.isBlank() ? "no-reply@noreply.local" : envFromAddress);
            settings.setSmtpAuth(envSmtpAuth);
            settings.setStarttls(envStarttls);
            settings.setSslTrust(envSslTrust.isBlank() ? null : envSslTrust);
            repository.save(settings);
            log.info("Configuration e-mail initialisée depuis les variables d'environnement.");
        }
    }

    /** Retourne la configuration actuelle, ou null si aucune n'a été définie. */
    public EmailSettings getSettings() {
        return repository.findTopByOrderByIdAsc().orElse(null);
    }

    /** Retourne la configuration sous forme de DTO (mot de passe masqué). */
    public EmailSettingsDto getSettingsDto() {
        EmailSettings s = getSettings();
        if (s == null) return null;
        EmailSettingsDto dto = new EmailSettingsDto();
        dto.setHost(s.getHost());
        dto.setPort(s.getPort());
        dto.setUsername(s.getUsername());
        dto.setPassword(null); // never expose password
        dto.setFromAddress(s.getFromAddress());
        dto.setSmtpAuth(s.isSmtpAuth());
        dto.setStarttls(s.isStarttls());
        dto.setSslTrust(s.getSslTrust());
        return dto;
    }

    /** Crée ou met à jour la configuration SMTP en base. */
    @Transactional
    public EmailSettingsDto saveSettings(EmailSettingsDto dto) {
        EmailSettings settings = repository.findTopByOrderByIdAsc().orElseGet(EmailSettings::new);
        settings.setHost(dto.getHost());
        settings.setPort(dto.getPort());
        settings.setUsername(dto.getUsername());
        // Blank password = keep existing
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            settings.setPassword(dto.getPassword());
        }
        settings.setFromAddress(dto.getFromAddress());
        settings.setSmtpAuth(dto.isSmtpAuth());
        settings.setStarttls(dto.isStarttls());
        settings.setSslTrust(dto.getSslTrust());
        repository.save(settings);
        log.info("Configuration e-mail mise à jour par l'administrateur (host={}:{}).",
                settings.getHost(), settings.getPort());
        return getSettingsDto();
    }

    /**
     * Construit un JavaMailSenderImpl prêt à l'emploi depuis la config en base.
     * Appelé par MailService avant chaque envoi.
     */
    public JavaMailSenderImpl buildMailSender() {
        EmailSettings s = getSettings();
        if (s == null || s.getHost().isBlank()) {
            throw new IllegalStateException(
                "Aucune configuration e-mail définie. Configurez le serveur SMTP dans Administration → E-mail.");
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(s.getHost());
        sender.setPort(s.getPort());
        sender.setUsername(s.getUsername());
        sender.setPassword(s.getPassword());
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(s.isSmtpAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(s.isStarttls()));
        if (s.getSslTrust() != null && !s.getSslTrust().isBlank()) {
            props.put("mail.smtp.ssl.trust", s.getSslTrust());
        }
        return sender;
    }
}
