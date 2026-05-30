package com.nexaris.notificationservice.controller;

import com.nexaris.notificationservice.dtos.EmailSettingsDto;
import com.nexaris.notificationservice.services.EmailSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications/settings")
public class EmailSettingsController {

    private final EmailSettingsService emailSettingsService;

    public EmailSettingsController(EmailSettingsService emailSettingsService) {
        this.emailSettingsService = emailSettingsService;
    }

    /**
     * GET /api/v1/notifications/settings/email
     * Retourne la configuration SMTP courante (mot de passe masqué).
     * Réservé aux administrateurs (rôle vérifié via le header X-User-Roles injecté par la Gateway).
     */
    @GetMapping("/email")
    public ResponseEntity<EmailSettingsDto> getEmailSettings(
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {
        requireAdmin(rolesHeader);
        EmailSettingsDto dto = emailSettingsService.getSettingsDto();
        if (dto == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(dto);
    }

    /**
     * PUT /api/v1/notifications/settings/email
     * Crée ou remplace la configuration SMTP.
     * Réservé aux administrateurs.
     */
    @PutMapping("/email")
    public ResponseEntity<EmailSettingsDto> updateEmailSettings(
            @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
            @Valid @RequestBody EmailSettingsDto dto) {
        requireAdmin(rolesHeader);
        EmailSettingsDto saved = emailSettingsService.saveSettings(dto);
        return ResponseEntity.status(HttpStatus.OK).body(saved);
    }

    // -----------------------------------------------------------------------
    private void requireAdmin(String rolesHeader) {
        if (rolesHeader == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès réservé aux administrateurs.");
        }
        List<String> roles = List.of(rolesHeader.split(","));
        boolean isAdmin = roles.stream().map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès réservé aux administrateurs.");
        }
    }
}
