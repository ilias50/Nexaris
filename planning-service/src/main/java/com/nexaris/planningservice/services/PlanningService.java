package com.nexaris.planningservice.services;

import com.nexaris.planningservice.dto.AgendaEntryResponse;
import com.nexaris.planningservice.dto.CreateAgendaEntryRequest;
import com.nexaris.planningservice.dto.CreateMeetingRequest;
import com.nexaris.planningservice.dto.CreatePlanningTagRequest;
import com.nexaris.planningservice.dto.MeetingSlotSuggestionResponse;
import com.nexaris.planningservice.dto.MeetingSuggestionRequest;
import com.nexaris.planningservice.dto.MeetingSuggestionResult;
import com.nexaris.planningservice.dto.PlanningTagResponse;
import com.nexaris.planningservice.dto.UpdateEntryTagsRequest;
import com.nexaris.planningservice.dto.UpdateUserPreferenceRequest;
import com.nexaris.planningservice.dto.UserPreferenceResponse;
import com.nexaris.planningservice.entities.AgendaEntry;
import com.nexaris.planningservice.entities.PlanningTag;
import com.nexaris.planningservice.entities.UserPlanningPreference;
import com.nexaris.planningservice.repositories.AgendaEntryRepository;
import com.nexaris.planningservice.repositories.PlanningTagRepository;
import com.nexaris.planningservice.repositories.UserPlanningPreferenceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PlanningService {

    private final AgendaEntryRepository agendaEntryRepository;
    private final UserPlanningPreferenceRepository preferenceRepository;
    private final PlanningTagRepository planningTagRepository;
    private final NotificationClient notificationClient;

    public PlanningService(
            AgendaEntryRepository agendaEntryRepository,
            UserPlanningPreferenceRepository preferenceRepository,
            PlanningTagRepository planningTagRepository,
            NotificationClient notificationClient
    ) {
        this.agendaEntryRepository = agendaEntryRepository;
        this.preferenceRepository = preferenceRepository;
        this.planningTagRepository = planningTagRepository;
        this.notificationClient = notificationClient;
    }

    @Transactional
    public AgendaEntryResponse createManualEntry(CreateAgendaEntryRequest request, Integer requesterUserId) {
        if (request.userId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId est obligatoire");
        }
        if (request.startAt() == null || request.endAt() == null || !request.startAt().isBefore(request.endAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plage horaire invalide");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title est obligatoire");
        }

        Set<PlanningTag> tags = resolveTags(request.tagIds());
        List<LocalDateTime> starts = buildRecurrenceStarts(request);
        Duration duration = Duration.between(request.startAt(), request.endAt());

        AgendaEntry firstSaved = null;
        for (LocalDateTime start : starts) {
            AgendaEntry entry = new AgendaEntry();
            entry.setUserId(request.userId());
            entry.setTitle(request.title().trim());
            entry.setSource("MANUAL");
            entry.setManualLocked(true);
            entry.setStartAt(start);
            entry.setEndAt(start.plus(duration));
            entry.setCreatedAt(LocalDateTime.now());
            entry.setTags(new HashSet<>(tags));

            AgendaEntry saved = agendaEntryRepository.save(entry);
            if (firstSaved == null) {
                firstSaved = saved;
            }
        }

        if (firstSaved == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune occurrence à créer");
        }

        String notificationType = requesterUserId != null && !requesterUserId.equals(request.userId())
            ? "PLANNING_ENTRY_ASSIGNED"
            : "PLANNING_ENTRY_CREATED";
        Map<String, Object> templateParams = Map.of(
            "title", request.title().trim(),
            "startAt", toNotificationDateTime(firstSaved.getStartAt()),
            "endAt", toNotificationDateTime(firstSaved.getEndAt()),
            "occurrenceCount", starts.size()
        );
        notificationClient.sendNotification(
                List.of(request.userId()),
                "/agenda",
            notificationType,
            templateParams
        );

        return toEntryResponse(firstSaved);
    }

    @Transactional
    public AgendaEntryResponse updateEntryTags(Long entryId, UpdateEntryTagsRequest request, Integer requesterUserId) {
        AgendaEntry entry = agendaEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agenda entry introuvable"));
        entry.setTags(resolveTags(request == null ? null : request.tagIds()));
        AgendaEntry saved = agendaEntryRepository.save(entry);

        if (requesterUserId == null || !requesterUserId.equals(saved.getUserId())) {
            Map<String, Object> templateParams = Map.of(
                "title", safeValue(saved.getTitle()),
                "startAt", toNotificationDateTime(saved.getStartAt()),
                "endAt", toNotificationDateTime(saved.getEndAt())
            );
            notificationClient.sendNotification(
                    List.of(saved.getUserId()),
                    "/agenda",
                "PLANNING_ENTRY_UPDATED",
                templateParams
            );
        }

        return toEntryResponse(saved);
    }

    @Transactional
    public void deleteEntry(Long entryId, Integer requesterUserId, String rolesHeader) {
        AgendaEntry entry = agendaEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agenda entry introuvable"));

        Integer targetUserId = entry.getUserId();
        String title = entry.getTitle();
        LocalDateTime startAt = entry.getStartAt();
        LocalDateTime endAt = entry.getEndAt();

        boolean isAdmin = hasRole(rolesHeader, "ROLE_ADMIN");
        boolean isManualEntry = "MANUAL".equalsIgnoreCase(entry.getSource());
        boolean isOwnerOfManualEntry = isManualEntry
                && requesterUserId != null
                && requesterUserId.equals(entry.getUserId());

        if (!isAdmin && !isOwnerOfManualEntry) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Suppression non autorisée");
        }

        agendaEntryRepository.delete(entry);

        if (requesterUserId == null || !requesterUserId.equals(targetUserId)) {
        Map<String, Object> templateParams = Map.of(
            "title", safeValue(title),
            "startAt", toNotificationDateTime(startAt),
            "endAt", toNotificationDateTime(endAt)
        );
            notificationClient.sendNotification(
                    List.of(targetUserId),
                    "/agenda",
            "PLANNING_ENTRY_DELETED",
            templateParams
            );
        }
    }

    @Transactional
    public PlanningTagResponse createTag(Integer adminUserId, CreatePlanningTagRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name est obligatoire");
        }

        String normalizedName = request.name().trim();
        if (planningTagRepository.findByNameIgnoreCase(normalizedName).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un tag avec ce nom existe déjà");
        }

        PlanningTag tag = new PlanningTag();
        tag.setName(normalizedName);
        tag.setDescription(request.description());
        tag.setColor(normalizeColor(request.color()));
        tag.setBlocking(request.blocking() != null && request.blocking());
        tag.setActive(request.active() == null || request.active());
        tag.setCreatedByUserId(adminUserId);
        tag.setCreatedAt(LocalDateTime.now());

        return toTagResponse(planningTagRepository.save(tag));
    }

    @Transactional
    public PlanningTagResponse updateTagColor(Long tagId, String color) {
        PlanningTag tag = planningTagRepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag introuvable"));

        tag.setColor(normalizeColor(color));
        return toTagResponse(planningTagRepository.save(tag));
    }

    public List<PlanningTagResponse> listActiveTags() {
        return planningTagRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toTagResponse)
                .toList();
    }

    public List<AgendaEntryResponse> getUserEntries(Integer userId, LocalDateTime from, LocalDateTime to) {
        if (userId == null || from == null || to == null || !from.isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paramètres de recherche invalides");
        }

        return agendaEntryRepository.findByUserIdAndEndAtAfterAndStartAtBeforeOrderByStartAtAsc(userId, from, to)
                .stream()
                .map(this::toEntryResponse)
                .toList();
    }

    @Transactional
    public UserPreferenceResponse upsertUserPreference(Integer userId, UpdateUserPreferenceRequest request) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId est obligatoire");
        }

        LocalTime workDayStart = request.workDayStart() == null ? LocalTime.of(8, 30) : request.workDayStart();
        LocalTime workDayEnd = request.workDayEnd() == null ? LocalTime.of(17, 30) : request.workDayEnd();
        Integer preferredMeetingBlockMinutes = request.preferredMeetingBlockMinutes() == null
                ? 60
                : request.preferredMeetingBlockMinutes();

        if (!workDayStart.isBefore(workDayEnd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "workDayStart doit être avant workDayEnd");
        }
        if (preferredMeetingBlockMinutes < 15 || preferredMeetingBlockMinutes > 240) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "preferredMeetingBlockMinutes doit être entre 15 et 240");
        }

        UserPlanningPreference preference = preferenceRepository.findByUserId(userId).orElseGet(UserPlanningPreference::new);
        preference.setUserId(userId);
        preference.setWorkDayStart(workDayStart);
        preference.setWorkDayEnd(workDayEnd);
        preference.setPreferredMeetingBlockMinutes(preferredMeetingBlockMinutes);

        LocalDateTime now = LocalDateTime.now();
        if (preference.getCreatedAt() == null) {
            preference.setCreatedAt(now);
        }
        preference.setUpdatedAt(now);

        return toPreferenceResponse(preferenceRepository.save(preference));
    }

    public UserPreferenceResponse getUserPreference(Integer userId) {
        return preferenceRepository.findByUserId(userId)
                .map(this::toPreferenceResponse)
                .orElse(new UserPreferenceResponse(userId, LocalTime.of(8, 30), LocalTime.of(17, 30), 60));
    }

    public MeetingSuggestionResult suggestMeetingSlots(MeetingSuggestionRequest request) {
        validateSuggestionRequest(request);

        List<Integer> participantUserIds = deduplicatedUserIds(request.participantUserIds());
        int durationMinutes = request.durationMinutes();
        int maxSuggestions = request.maxSuggestions() == null ? 10 : request.maxSuggestions();

        List<AgendaEntry> overlappingEntries = agendaEntryRepository.findOverlappingForUsers(
                participantUserIds,
                request.windowStart(),
                request.windowEnd()
        );

        Map<Integer, List<AgendaEntry>> entriesByUser = new HashMap<>();
        for (Integer userId : participantUserIds) {
            entriesByUser.put(userId, new ArrayList<>());
        }
        for (AgendaEntry entry : overlappingEntries) {
            entriesByUser.computeIfAbsent(entry.getUserId(), ignored -> new ArrayList<>()).add(entry);
        }

        Map<Integer, UserPlanningPreference> preferenceByUser = new HashMap<>();
        for (UserPlanningPreference preference : preferenceRepository.findByUserIdIn(participantUserIds)) {
            preferenceByUser.put(preference.getUserId(), preference);
        }

        List<MeetingSlotSuggestionResponse> suggestions = new ArrayList<>();
        int scannedSlots = 0;

        LocalDateTime candidateStart = request.windowStart();
        while (!candidateStart.plusMinutes(durationMinutes).isAfter(request.windowEnd())) {
            scannedSlots++;
            LocalDateTime candidateEnd = candidateStart.plusMinutes(durationMinutes);

            if (isSlotAvailableForAllUsers(participantUserIds, entriesByUser, candidateStart, candidateEnd)) {
                int score = scoreSlot(participantUserIds, preferenceByUser, candidateStart, candidateEnd);
                String rationale = buildRationale(score, candidateStart);
                suggestions.add(new MeetingSlotSuggestionResponse(candidateStart, candidateEnd, score, rationale));
            }

            candidateStart = candidateStart.plusMinutes(15);
        }

        suggestions.sort(Comparator
                .comparing(MeetingSlotSuggestionResponse::score)
                .reversed()
                .thenComparing(MeetingSlotSuggestionResponse::startAt));

        if (suggestions.size() > maxSuggestions) {
            suggestions = suggestions.subList(0, maxSuggestions);
        }

        return new MeetingSuggestionResult(durationMinutes, scannedSlots, suggestions);
    }

    @Transactional
    public List<AgendaEntryResponse> createMeetingEntries(CreateMeetingRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title est obligatoire");
        }
        if (request.startAt() == null || request.endAt() == null || !request.startAt().isBefore(request.endAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plage horaire invalide");
        }

        List<Integer> participantUserIds = deduplicatedUserIds(request.participantUserIds());
        String title = request.title().trim();
        Set<PlanningTag> tags = resolveTags(request.tagIds());
        LocalDateTime now = LocalDateTime.now();

        List<AgendaEntryResponse> createdEntries = new ArrayList<>();
        for (Integer participantUserId : participantUserIds) {
            AgendaEntry entry = new AgendaEntry();
            entry.setUserId(participantUserId);
            entry.setTitle(title);
            entry.setSource("MEETING");
            entry.setManualLocked(true);
            entry.setStartAt(request.startAt());
            entry.setEndAt(request.endAt());
            entry.setCreatedAt(now);
            entry.setTags(new HashSet<>(tags));
            createdEntries.add(toEntryResponse(agendaEntryRepository.save(entry)));
        }

        Map<String, Object> templateParams = Map.of(
                "title", title,
                "startAt", toNotificationDateTime(request.startAt()),
                "endAt", toNotificationDateTime(request.endAt())
        );
        notificationClient.sendNotification(
            participantUserIds,
            "/agenda",
            "PLANNING_MEETING_CREATED",
            templateParams
        );

        return createdEntries;
    }

    private boolean isSlotAvailableForAllUsers(
            List<Integer> userIds,
            Map<Integer, List<AgendaEntry>> entriesByUser,
            LocalDateTime slotStart,
            LocalDateTime slotEnd
    ) {
        for (Integer userId : userIds) {
            List<AgendaEntry> userEntries = entriesByUser.getOrDefault(userId, List.of());
            if (hasBlockingEntry(userEntries, slotStart, slotEnd)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasBlockingEntry(List<AgendaEntry> entries, LocalDateTime slotStart, LocalDateTime slotEnd) {
        for (AgendaEntry entry : entries) {
            if (isOverlap(entry.getStartAt(), entry.getEndAt(), slotStart, slotEnd)
                    && isBlocking(entry)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlocking(AgendaEntry entry) {
        return entry.getTags() != null && entry.getTags().stream().anyMatch(PlanningTag::isBlocking);
    }

    private int scoreSlot(
            List<Integer> userIds,
            Map<Integer, UserPlanningPreference> preferenceByUser,
            LocalDateTime slotStart,
            LocalDateTime slotEnd
    ) {
        int score = 0;

        for (Integer userId : userIds) {
            UserPlanningPreference preference = preferenceByUser.get(userId);
            LocalTime preferredStart = preference == null ? LocalTime.of(8, 30) : preference.getWorkDayStart();
            LocalTime preferredEnd = preference == null ? LocalTime.of(17, 30) : preference.getWorkDayEnd();

            boolean inPreferredWindow = !slotStart.toLocalTime().isBefore(preferredStart)
                    && !slotEnd.toLocalTime().isAfter(preferredEnd);
            if (inPreferredWindow) {
                score += 40;
            }
        }

        DayOfWeek day = slotStart.getDayOfWeek();
        if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
            score += 20;
        }

        int hour = slotStart.getHour();
        if (hour >= 9 && hour <= 16) {
            score += 15;
        }

        return score;
    }

    private String buildRationale(int score, LocalDateTime slotStart) {
        if (score >= 100) {
            return "Créneau optimal selon disponibilités et préférences";
        }
        if (slotStart.getDayOfWeek() == DayOfWeek.SATURDAY || slotStart.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return "Créneau disponible mais en week-end";
        }
        return "Créneau disponible avec compromis acceptable";
    }

    private void validateSuggestionRequest(MeetingSuggestionRequest request) {
        if (request == null
                || request.participantUserIds() == null
                || request.participantUserIds().isEmpty()
                || request.durationMinutes() == null
                || request.windowStart() == null
                || request.windowEnd() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paramètres de suggestion invalides");
        }

        if (!request.windowStart().isBefore(request.windowEnd())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "windowStart doit être avant windowEnd");
        }
        if (request.durationMinutes() < 15 || request.durationMinutes() > 480) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "durationMinutes doit être entre 15 et 480");
        }

        int maxSuggestions = request.maxSuggestions() == null ? 10 : request.maxSuggestions();
        if (maxSuggestions < 1 || maxSuggestions > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxSuggestions doit être entre 1 et 50");
        }
    }

    private List<Integer> deduplicatedUserIds(List<Integer> userIds) {
        Set<Integer> unique = new HashSet<>();
        List<Integer> result = new ArrayList<>();
        for (Integer userId : userIds) {
            if (userId != null && unique.add(userId)) {
                result.add(userId);
            }
        }
        if (result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "participantUserIds ne peut pas être vide");
        }
        return result;
    }

    private boolean isOverlap(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        return aEnd.isAfter(bStart) && aStart.isBefore(bEnd);
    }

    private String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }

        String normalized = color.trim();
        if (!normalized.startsWith("#")) {
            normalized = "#" + normalized;
        }
        return normalized;
    }

    private boolean hasRole(String rolesHeader, String expectedRole) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return false;
        }

        return java.util.Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch(role -> expectedRole.equalsIgnoreCase(role));
    }

    private List<LocalDateTime> buildRecurrenceStarts(CreateAgendaEntryRequest request) {
        String recurrenceFrequency = request.recurrenceFrequency() == null
                ? "NONE"
                : request.recurrenceFrequency().trim().toUpperCase();

        if (recurrenceFrequency.isBlank() || "NONE".equals(recurrenceFrequency)) {
            return List.of(request.startAt());
        }

        LocalDate until = request.recurrenceUntil();
        if (until == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceUntil est obligatoire pour une récurrence");
        }

        LocalDate startDate = request.startAt().toLocalDate();
        if (until.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceUntil doit être >= à la date de début");
        }

        List<LocalDateTime> starts = new ArrayList<>();
        if ("DAILY".equals(recurrenceFrequency)) {
            LocalDate cursor = startDate;
            while (!cursor.isAfter(until)) {
                starts.add(cursor.atTime(request.startAt().toLocalTime()));
                cursor = cursor.plusDays(1);
                if (starts.size() > 366) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Récurrence trop longue (max 366 occurrences)");
                }
            }
            return applyHolidayExclusions(starts, request);
        }

        if ("WEEKLY".equals(recurrenceFrequency)) {
            Set<DayOfWeek> allowedDays = resolveWeeklyDays(request.recurrenceWeekdays(), request.startAt().getDayOfWeek());
            LocalDate cursor = startDate;
            while (!cursor.isAfter(until)) {
                if (allowedDays.contains(cursor.getDayOfWeek())) {
                    starts.add(cursor.atTime(request.startAt().toLocalTime()));
                    if (starts.size() > 366) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Récurrence trop longue (max 366 occurrences)");
                    }
                }
                cursor = cursor.plusDays(1);
            }

            if (starts.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune occurrence ne correspond aux jours hebdomadaires choisis");
            }
            return applyHolidayExclusions(starts, request);
        }

        if ("WEEKDAYS".equals(recurrenceFrequency)) {
            LocalDate cursor = startDate;
            while (!cursor.isAfter(until)) {
                DayOfWeek day = cursor.getDayOfWeek();
                if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                    starts.add(cursor.atTime(request.startAt().toLocalTime()));
                    if (starts.size() > 366) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Récurrence trop longue (max 366 occurrences)");
                    }
                }
                cursor = cursor.plusDays(1);
            }

            if (starts.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune occurrence ouvrable n'a été calculée");
            }
            return applyHolidayExclusions(starts, request);
        }

        if ("MONTHLY".equals(recurrenceFrequency)) {
            YearMonth cursorMonth = YearMonth.from(startDate);
            int dayOfMonth = startDate.getDayOfMonth();

            while (!cursorMonth.atDay(1).isAfter(until)) {
                int validDay = Math.min(dayOfMonth, cursorMonth.lengthOfMonth());
                LocalDate occurrenceDate = cursorMonth.atDay(validDay);

                if (!occurrenceDate.isBefore(startDate) && !occurrenceDate.isAfter(until)) {
                    starts.add(occurrenceDate.atTime(request.startAt().toLocalTime()));
                    if (starts.size() > 366) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Récurrence trop longue (max 366 occurrences)");
                    }
                }
                cursorMonth = cursorMonth.plusMonths(1);
            }

            if (starts.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune occurrence mensuelle valide n'a été calculée");
            }
            return applyHolidayExclusions(starts, request);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceFrequency invalide (NONE, DAILY, WEEKLY, WEEKDAYS, MONTHLY)");
    }

    private List<LocalDateTime> applyHolidayExclusions(List<LocalDateTime> starts, CreateAgendaEntryRequest request) {
        if (!Boolean.TRUE.equals(request.recurrenceSkipHolidays())) {
            return starts;
        }

        Set<LocalDate> excludedDates = resolveExcludedDates(request.recurrenceExcludedDates());
        if (excludedDates.isEmpty()) {
            return starts;
        }

        List<LocalDateTime> filtered = starts.stream()
                .filter(start -> !excludedDates.contains(start.toLocalDate()))
                .toList();

        if (filtered.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Toutes les occurrences tombent sur des jours fériés exclus");
        }
        return filtered;
    }

    private Set<LocalDate> resolveExcludedDates(List<LocalDate> excludedDates) {
        if (excludedDates == null || excludedDates.isEmpty()) {
            return Set.of();
        }

        Set<LocalDate> result = new HashSet<>();
        for (LocalDate excludedDate : excludedDates) {
            if (excludedDate != null) {
                result.add(excludedDate);
            }
        }
        return result;
    }

    private Set<DayOfWeek> resolveWeeklyDays(List<String> recurrenceWeekdays, DayOfWeek defaultDay) {
        if (recurrenceWeekdays == null || recurrenceWeekdays.isEmpty()) {
            return Set.of(defaultDay);
        }

        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String raw : recurrenceWeekdays) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                days.add(DayOfWeek.valueOf(raw.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jour hebdomadaire invalide: " + raw);
            }
        }

        if (days.isEmpty()) {
            days.add(defaultDay);
        }
        return days;
    }

    private Set<PlanningTag> resolveTags(List<Long> tagIds) {
        Set<PlanningTag> result = new HashSet<>();
        if (tagIds == null || tagIds.isEmpty()) {
            return result;
        }

        for (Long tagId : tagIds) {
            if (tagId == null) {
                continue;
            }
            PlanningTag tag = planningTagRepository.findById(tagId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag introuvable: " + tagId));
            if (!tag.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag inactif: " + tag.getName());
            }
            result.add(tag);
        }
        return result;
    }

    private AgendaEntryResponse toEntryResponse(AgendaEntry entry) {
        return new AgendaEntryResponse(
                entry.getId(),
                entry.getUserId(),
                entry.getTitle(),
                entry.getSource(),
                entry.isManualLocked(),
                entry.getStartAt(),
                entry.getEndAt(),
                entry.getTags().stream().map(this::toTagResponse).toList()
        );
    }

    private PlanningTagResponse toTagResponse(PlanningTag tag) {
        return new PlanningTagResponse(
                tag.getId(),
                tag.getName(),
                tag.getDescription(),
                tag.getColor(),
                tag.isActive(),
                tag.isBlocking(),
                tag.getCreatedByUserId()
        );
    }

    private UserPreferenceResponse toPreferenceResponse(UserPlanningPreference preference) {
        return new UserPreferenceResponse(
                preference.getUserId(),
                preference.getWorkDayStart(),
                preference.getWorkDayEnd(),
                preference.getPreferredMeetingBlockMinutes()
        );
    }

    private String toNotificationDateTime(LocalDateTime value) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return value != null ? value.format(fmt) : "?";
    }

    private String safeValue(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }
}
