package com.nexaris.planningservice.controller;

import com.nexaris.planningservice.dto.AgendaEntryResponse;
import com.nexaris.planningservice.dto.CreateAgendaEntryRequest;
import com.nexaris.planningservice.dto.CreateMeetingRequest;
import com.nexaris.planningservice.dto.CreatePlanningTagRequest;
import com.nexaris.planningservice.dto.CreatePlanningRoleRequest;
import com.nexaris.planningservice.dto.MeetingSuggestionRequest;
import com.nexaris.planningservice.dto.MeetingSuggestionResult;
import com.nexaris.planningservice.dto.PlanningTagResponse;
import com.nexaris.planningservice.dto.PlanningRoleResponse;
import com.nexaris.planningservice.dto.ReplacePlanningRolePermissionsRequest;
import com.nexaris.planningservice.dto.UpdateEntryTagsRequest;
import com.nexaris.planningservice.dto.UpdateUserPreferenceRequest;
import com.nexaris.planningservice.dto.UserPreferenceResponse;
import com.nexaris.planningservice.services.PlanningAccessService;
import com.nexaris.planningservice.services.PlanningRoleAdminService;
import com.nexaris.planningservice.services.PlanningService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/planning")
public class PlanningController {

    private final PlanningService planningService;
    private final PlanningAccessService planningAccessService;
    private final PlanningRoleAdminService planningRoleAdminService;

    public PlanningController(
            PlanningService planningService,
            PlanningAccessService planningAccessService,
            PlanningRoleAdminService planningRoleAdminService
    ) {
        this.planningService = planningService;
        this.planningAccessService = planningAccessService;
        this.planningRoleAdminService = planningRoleAdminService;
    }

    @GetMapping("/admin/roles")
    public List<PlanningRoleResponse> listPlanningRoles(
            @RequestHeader(name = "X-User-Id", required = false) Integer requesterUserId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        ensureAdminHeaderPresent(requesterUserId, rolesHeader);
        return planningRoleAdminService.listRoles();
    }

    @GetMapping("/admin/permissions")
    public List<String> listAvailablePlanningPermissions(
            @RequestHeader(name = "X-User-Id", required = false) Integer requesterUserId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        return planningRoleAdminService.listAllowedPermissions(requesterUserId, rolesHeader);
    }

    @PostMapping("/admin/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanningRoleResponse createPlanningRole(
            @RequestHeader(name = "X-User-Id", required = false) Integer requesterUserId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader,
            @RequestBody CreatePlanningRoleRequest request
    ) {
        return planningRoleAdminService.createRole(requesterUserId, rolesHeader, request);
    }

    @PutMapping("/admin/roles/{roleName}/permissions")
    public PlanningRoleResponse replacePlanningRolePermissions(
            @PathVariable String roleName,
            @RequestHeader(name = "X-User-Id", required = false) Integer requesterUserId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader,
            @RequestBody ReplacePlanningRolePermissionsRequest request
    ) {
        return planningRoleAdminService.replaceRolePermissions(
                requesterUserId,
                rolesHeader,
                roleName,
                request == null ? null : request.permissions()
        );
    }

    @PostMapping("/admin/users/{targetUserId}/roles/{roleName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignPlanningRoleToUser(
            @PathVariable Integer targetUserId,
            @PathVariable String roleName,
            @RequestHeader(name = "X-User-Id", required = false) Integer requesterUserId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        planningRoleAdminService.assignRoleToUser(requesterUserId, rolesHeader, targetUserId, roleName);
    }

    @GetMapping("/admin/users/{targetUserId}/roles")
    public List<String> getUserPlanningRoles(
            @PathVariable Integer targetUserId,
            @RequestHeader(name = "X-User-Id", required = false) Integer requesterUserId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        return planningRoleAdminService.getUserRoles(requesterUserId, rolesHeader, targetUserId);
    }

    @DeleteMapping("/admin/users/{targetUserId}/roles/{roleName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokePlanningRoleFromUser(
            @PathVariable Integer targetUserId,
            @PathVariable String roleName,
            @RequestHeader(name = "X-User-Id", required = false) Integer requesterUserId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        planningRoleAdminService.revokeRoleFromUser(requesterUserId, rolesHeader, targetUserId, roleName);
    }

    @PostMapping("/entries/manual")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendaEntryResponse createManualEntry(
            @RequestHeader(name = "X-User-Id", required = false) Integer requesterUserId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader,
            @RequestBody CreateAgendaEntryRequest request
    ) {
        if (requesterUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id est obligatoire");
        }

        Integer requestedUserId = request.userId();
        Integer targetUserId = requestedUserId == null ? requesterUserId : requestedUserId;

        if (!planningAccessService.canCreateEntryForTargetUser(requesterUserId, targetUserId, rolesHeader)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Droits insuffisants pour creer une entree sur l'agenda cible");
        }

        CreateAgendaEntryRequest securedRequest = new CreateAgendaEntryRequest(
                targetUserId,
                request.title(),
                request.startAt(),
                request.endAt(),
                request.tagIds(),
                request.recurrenceFrequency(),
                request.recurrenceUntil(),
                request.recurrenceWeekdays(),
                request.recurrenceSkipHolidays(),
                request.recurrenceExcludedDates()
        );
        return planningService.createManualEntry(securedRequest, requesterUserId);
    }

    @PutMapping("/entries/{entryId}/tags")
    public AgendaEntryResponse updateEntryTags(
            @PathVariable Long entryId,
            @RequestHeader(name = "X-User-Id", required = false) Integer requesterUserId,
            @RequestBody UpdateEntryTagsRequest request
    ) {
        return planningService.updateEntryTags(entryId, request, requesterUserId);
    }

    @DeleteMapping("/entries/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntry(
            @PathVariable Long entryId,
            @RequestHeader(name = "X-User-Id", required = false) Integer requesterUserId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        planningService.deleteEntry(entryId, requesterUserId, rolesHeader);
    }

    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanningTagResponse createTag(
            @RequestHeader(name = "X-User-Id", required = false) Integer adminUserId,
            @RequestBody CreatePlanningTagRequest request
    ) {
        return planningService.createTag(adminUserId, request);
    }

    @GetMapping("/tags")
    public List<PlanningTagResponse> listActiveTags() {
        return planningService.listActiveTags();
    }

    @PutMapping("/tags/{tagId}/color")
    public PlanningTagResponse updateTagColor(@PathVariable Long tagId,
                                              @RequestBody(required = false) String color) {
        return planningService.updateTagColor(tagId, color);
    }

    @GetMapping("/users/{userId}/entries")
    public List<AgendaEntryResponse> getUserEntries(
            @PathVariable Integer userId,
            @RequestHeader(name = "X-User-Id", required = false) Integer requesterUserId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader,
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to
    ) {
        if (requesterUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id est obligatoire");
        }

        if (!planningAccessService.canViewCalendarForTargetUser(requesterUserId, userId, rolesHeader)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Droits insuffisants pour consulter cet agenda");
        }

        return planningService.getUserEntries(userId, from, to);
    }

    @PutMapping("/users/{userId}/preferences")
    public UserPreferenceResponse upsertUserPreference(
            @PathVariable Integer userId,
            @RequestBody UpdateUserPreferenceRequest request
    ) {
        return planningService.upsertUserPreference(userId, request);
    }

    @GetMapping("/users/{userId}/preferences")
    public UserPreferenceResponse getUserPreference(@PathVariable Integer userId) {
        return planningService.getUserPreference(userId);
    }

    @GetMapping("/me/permissions")
    public List<String> getMyPlanningPermissions(
            @RequestHeader(name = "X-User-Id", required = false) Integer requesterUserId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        if (requesterUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id est obligatoire");
        }
        return planningAccessService.getEffectivePermissions(requesterUserId, rolesHeader);
    }

    @PostMapping("/meeting-slots/suggestions")
    public MeetingSuggestionResult suggestMeetingSlots(
            @RequestHeader(name = "X-User-Id", required = false) Integer requesterUserId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader,
            @RequestBody MeetingSuggestionRequest request
    ) {
        enforceSuggestionPermission(requesterUserId, rolesHeader);
        return planningService.suggestMeetingSlots(request);
    }

    @PostMapping("/meetings")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AgendaEntryResponse> createMeeting(
            @RequestHeader(name = "X-User-Id", required = false) Integer requesterUserId,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader,
            @RequestBody CreateMeetingRequest request
    ) {
        enforceMeetingCreationPermission(requesterUserId, rolesHeader);
        return planningService.createMeetingEntries(request);
    }

    private void enforceSuggestionPermission(Integer requesterUserId, String rolesHeader) {
        if (requesterUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id est obligatoire");
        }
        if (!planningAccessService.canSuggestMeeting(requesterUserId, rolesHeader)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Droits insuffisants pour planifier une reunion");
        }
    }

    private void enforceMeetingCreationPermission(Integer requesterUserId, String rolesHeader) {
        if (requesterUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id est obligatoire");
        }
        if (!planningAccessService.canCreateMeeting(requesterUserId, rolesHeader)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Droits insuffisants pour creer une reunion");
        }
    }

    private void ensureAdminHeaderPresent(Integer requesterUserId, String rolesHeader) {
        if (requesterUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id est obligatoire");
        }
        if (!planningAccessService.hasRole(rolesHeader, PlanningAccessService.ROLE_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role admin requis");
        }
    }
}
