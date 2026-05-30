package com.nexaris.notificationservice.services;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class NotificationTemplateService {

    private static final String LANGUAGE_FR = "FR";

    private final Map<String, Map<String, TemplatePair>> templatesByLanguage;

    public NotificationTemplateService() {
        this.templatesByLanguage = buildTemplates();
    }

    public RenderedNotification render(String notificationType,
                                       String languageCode,
                                       String fallbackSubject,
                                       String fallbackMessage,
                                       Map<String, Object> params) {
        String normalizedType = normalizeType(notificationType);
        String normalizedLanguage = normalizeLanguage(languageCode);
        Map<String, Object> safeParams = params != null ? params : Map.of();

        TemplatePair template = resolveTemplate(normalizedLanguage, normalizedType);
        if (template == null) {
            return new RenderedNotification(
                    fallbackSubject != null ? fallbackSubject : "Notification",
                    fallbackMessage != null ? fallbackMessage : ""
            );
        }

        return new RenderedNotification(
                interpolate(template.subject(), safeParams),
                interpolate(template.message(), safeParams)
        );
    }

    private TemplatePair resolveTemplate(String language, String notificationType) {
        Map<String, TemplatePair> languageTemplates = templatesByLanguage.get(language);
        if (languageTemplates != null && languageTemplates.containsKey(notificationType)) {
            return languageTemplates.get(notificationType);
        }
        Map<String, TemplatePair> frTemplates = templatesByLanguage.get(LANGUAGE_FR);
        return frTemplates != null ? frTemplates.get(notificationType) : null;
    }

    private String interpolate(String template, Map<String, Object> params) {
        String rendered = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            rendered = rendered.replace(key, value);
        }
        return rendered;
    }

    private String normalizeType(String notificationType) {
        if (notificationType == null || notificationType.isBlank()) {
            return "GENERAL";
        }
        return notificationType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeLanguage(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return LANGUAGE_FR;
        }
        String upper = languageCode.trim().toUpperCase(Locale.ROOT);
        if (upper.length() > 2) {
            upper = upper.substring(0, 2);
        }
        return switch (upper) {
            case "FR", "EN", "NL", "DE" -> upper;
            default -> LANGUAGE_FR;
        };
    }

    private Map<String, Map<String, TemplatePair>> buildTemplates() {
        Map<String, Map<String, TemplatePair>> byLanguage = new HashMap<>();

        byLanguage.put("FR", Map.ofEntries(
                Map.entry("GENERAL", new TemplatePair("Notification", "{message}")),
            Map.entry("PLANNING_MEETING_CREATED", new TemplatePair("Réunion planifiée", "Vous avez été ajouté à la réunion \"{title}\" du {startAt} au {endAt}.")),
            Map.entry("PLANNING_MEETING_UPDATED", new TemplatePair("Réunion mise à jour", "La réunion \"{title}\" a été mise à jour ({startAt} -> {endAt}).")),
            Map.entry("PLANNING_MEETING_CANCELLED", new TemplatePair("Réunion annulée", "La réunion \"{title}\" a été annulée.")),
            Map.entry("PLANNING_ENTRY_CREATED", new TemplatePair("Créneau planifié", "Un créneau \"{title}\" a été créé du {startAt} au {endAt}.")),
            Map.entry("PLANNING_ENTRY_UPDATED", new TemplatePair("Créneau mis à jour", "Votre créneau \"{title}\" a été mis à jour ({startAt} -> {endAt}).")),
            Map.entry("PLANNING_ENTRY_DELETED", new TemplatePair("Créneau supprimé", "Votre créneau \"{title}\" du {startAt} au {endAt} a été supprimé.")),
            Map.entry("PLANNING_ENTRY_ASSIGNED", new TemplatePair("Créneau assigné", "Un créneau \"{title}\" vous a été assigné du {startAt} au {endAt}.")),
            Map.entry("ORG_ANNOUNCEMENT", new TemplatePair("Annonce organisationnelle", "{announcementTitle} (gravité: {severity}, portée: {scope})")),
            Map.entry("ORG_ANNOUNCEMENT_CREATED", new TemplatePair("Nouvelle annonce organisationnelle", "Une nouvelle annonce a été publiée: \"{announcementTitle}\" (gravité: {severity}, portée: {scope}).")),
            Map.entry("ORG_ANNOUNCEMENT_UPDATED", new TemplatePair("Annonce organisationnelle mise à jour", "Une annonce a été mise à jour: \"{announcementTitle}\" (gravité: {severity}, portée: {scope}).")),
            Map.entry("ORG_ANNOUNCEMENT_DELETED", new TemplatePair("Annonce organisationnelle supprimée", "Une annonce a été supprimée: \"{announcementTitle}\" (gravité: {severity}, portée: {scope})."))
        ));

        byLanguage.put("EN", Map.ofEntries(
                Map.entry("GENERAL", new TemplatePair("Notification", "{message}")),
                Map.entry("PLANNING_MEETING_CREATED", new TemplatePair("Scheduled meeting", "You were added to meeting \"{title}\" from {startAt} to {endAt}.")),
                Map.entry("PLANNING_MEETING_UPDATED", new TemplatePair("Meeting updated", "Meeting \"{title}\" was updated ({startAt} -> {endAt}).")),
                Map.entry("PLANNING_MEETING_CANCELLED", new TemplatePair("Meeting cancelled", "Meeting \"{title}\" was cancelled.")),
                Map.entry("PLANNING_ENTRY_CREATED", new TemplatePair("Planned slot", "A slot \"{title}\" was created from {startAt} to {endAt}.")),
                Map.entry("PLANNING_ENTRY_UPDATED", new TemplatePair("Slot updated", "Your slot \"{title}\" was updated ({startAt} -> {endAt}).")),
                Map.entry("PLANNING_ENTRY_DELETED", new TemplatePair("Slot deleted", "Your slot \"{title}\" from {startAt} to {endAt} was deleted.")),
                Map.entry("PLANNING_ENTRY_ASSIGNED", new TemplatePair("Slot assigned", "A slot \"{title}\" was assigned to you from {startAt} to {endAt}.")),
                Map.entry("ORG_ANNOUNCEMENT", new TemplatePair("Organization announcement", "{announcementTitle} (severity: {severity}, scope: {scope})")),
                Map.entry("ORG_ANNOUNCEMENT_CREATED", new TemplatePair("New organization announcement", "A new announcement was published: \"{announcementTitle}\" (severity: {severity}, scope: {scope}).")),
                Map.entry("ORG_ANNOUNCEMENT_UPDATED", new TemplatePair("Organization announcement updated", "An announcement was updated: \"{announcementTitle}\" (severity: {severity}, scope: {scope}).")),
                Map.entry("ORG_ANNOUNCEMENT_DELETED", new TemplatePair("Organization announcement deleted", "An announcement was deleted: \"{announcementTitle}\" (severity: {severity}, scope: {scope})."))
        ));

        byLanguage.put("NL", Map.ofEntries(
                Map.entry("GENERAL", new TemplatePair("Melding", "{message}")),
                Map.entry("PLANNING_MEETING_CREATED", new TemplatePair("Geplande vergadering", "Je bent toegevoegd aan vergadering \"{title}\" van {startAt} tot {endAt}.")),
                Map.entry("PLANNING_MEETING_UPDATED", new TemplatePair("Vergadering bijgewerkt", "Vergadering \"{title}\" werd bijgewerkt ({startAt} -> {endAt}).")),
                Map.entry("PLANNING_MEETING_CANCELLED", new TemplatePair("Vergadering geannuleerd", "Vergadering \"{title}\" werd geannuleerd.")),
                Map.entry("PLANNING_ENTRY_CREATED", new TemplatePair("Gepland tijdslot", "Een tijdslot \"{title}\" werd aangemaakt van {startAt} tot {endAt}.")),
                Map.entry("PLANNING_ENTRY_UPDATED", new TemplatePair("Tijdslot bijgewerkt", "Je tijdslot \"{title}\" werd bijgewerkt ({startAt} -> {endAt}).")),
                Map.entry("PLANNING_ENTRY_DELETED", new TemplatePair("Tijdslot verwijderd", "Je tijdslot \"{title}\" van {startAt} tot {endAt} werd verwijderd.")),
                Map.entry("PLANNING_ENTRY_ASSIGNED", new TemplatePair("Tijdslot toegewezen", "Een tijdslot \"{title}\" werd aan jou toegewezen van {startAt} tot {endAt}.")),
                Map.entry("ORG_ANNOUNCEMENT", new TemplatePair("Organisatieaankondiging", "{announcementTitle} (prioriteit: {severity}, scope: {scope})")),
                Map.entry("ORG_ANNOUNCEMENT_CREATED", new TemplatePair("Nieuwe organisatieaankondiging", "Een nieuwe aankondiging werd gepubliceerd: \"{announcementTitle}\" (prioriteit: {severity}, scope: {scope}).")),
                Map.entry("ORG_ANNOUNCEMENT_UPDATED", new TemplatePair("Organisatieaankondiging bijgewerkt", "Een aankondiging werd bijgewerkt: \"{announcementTitle}\" (prioriteit: {severity}, scope: {scope}).")),
                Map.entry("ORG_ANNOUNCEMENT_DELETED", new TemplatePair("Organisatieaankondiging verwijderd", "Een aankondiging werd verwijderd: \"{announcementTitle}\" (prioriteit: {severity}, scope: {scope})."))
        ));

        byLanguage.put("DE", Map.ofEntries(
                Map.entry("GENERAL", new TemplatePair("Benachrichtigung", "{message}")),
                Map.entry("PLANNING_MEETING_CREATED", new TemplatePair("Geplantes Meeting", "Du wurdest zum Meeting \"{title}\" von {startAt} bis {endAt} hinzugefügt.")),
                Map.entry("PLANNING_MEETING_UPDATED", new TemplatePair("Meeting aktualisiert", "Meeting \"{title}\" wurde aktualisiert ({startAt} -> {endAt}).")),
                Map.entry("PLANNING_MEETING_CANCELLED", new TemplatePair("Meeting abgesagt", "Meeting \"{title}\" wurde abgesagt.")),
                Map.entry("PLANNING_ENTRY_CREATED", new TemplatePair("Geplanter Zeitslot", "Ein Zeitslot \"{title}\" wurde von {startAt} bis {endAt} erstellt.")),
                Map.entry("PLANNING_ENTRY_UPDATED", new TemplatePair("Zeitslot aktualisiert", "Dein Zeitslot \"{title}\" wurde aktualisiert ({startAt} -> {endAt}).")),
                Map.entry("PLANNING_ENTRY_DELETED", new TemplatePair("Zeitslot gelöscht", "Dein Zeitslot \"{title}\" von {startAt} bis {endAt} wurde gelöscht.")),
                Map.entry("PLANNING_ENTRY_ASSIGNED", new TemplatePair("Zeitslot zugewiesen", "Ein Zeitslot \"{title}\" wurde dir von {startAt} bis {endAt} zugewiesen.")),
                Map.entry("ORG_ANNOUNCEMENT", new TemplatePair("Organisationsankündigung", "{announcementTitle} (Priorität: {severity}, Geltungsbereich: {scope})")),
                Map.entry("ORG_ANNOUNCEMENT_CREATED", new TemplatePair("Neue Organisationsankündigung", "Eine neue Ankündigung wurde veröffentlicht: \"{announcementTitle}\" (Priorität: {severity}, Geltungsbereich: {scope}).")),
                Map.entry("ORG_ANNOUNCEMENT_UPDATED", new TemplatePair("Organisationsankündigung aktualisiert", "Eine Ankündigung wurde aktualisiert: \"{announcementTitle}\" (Priorität: {severity}, Geltungsbereich: {scope}).")),
                Map.entry("ORG_ANNOUNCEMENT_DELETED", new TemplatePair("Organisationsankündigung entfernt", "Eine Ankündigung wurde entfernt: \"{announcementTitle}\" (Priorität: {severity}, Geltungsbereich: {scope})."))
        ));

        return byLanguage;
    }

    public record RenderedNotification(String subject, String message) {}

    private record TemplatePair(String subject, String message) {}
}