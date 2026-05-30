package com.nexaris.planningservice.services;

import com.nexaris.planningservice.entities.PlanningRolePermission;
import com.nexaris.planningservice.entities.PlanningRole;
import com.nexaris.planningservice.entities.PlanningUserRole;
import com.nexaris.planningservice.repositories.PlanningRolePermissionRepository;
import com.nexaris.planningservice.repositories.PlanningRoleRepository;
import com.nexaris.planningservice.repositories.PlanningUserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PlanningAccessService {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    public static final String PERMISSION_CREATE_ANY_ENTRY = "CREATE_ANY_ENTRY";
    public static final String PERMISSION_CREATE_MEETING = "CREATE_MEETING";
        public static final String PERMISSION_VIEW_ANY_CALENDAR = "VIEW_ANY_CALENDAR";

    private static final List<String> SUPPORTED_PERMISSIONS = List.of(
            PERMISSION_CREATE_ANY_ENTRY,
            PERMISSION_CREATE_MEETING,
            PERMISSION_VIEW_ANY_CALENDAR
    );

    private final PlanningUserRoleRepository planningUserRoleRepository;
    private final PlanningRoleRepository planningRoleRepository;
    private final PlanningRolePermissionRepository planningRolePermissionRepository;

    public PlanningAccessService(
            PlanningUserRoleRepository planningUserRoleRepository,
            PlanningRoleRepository planningRoleRepository,
            PlanningRolePermissionRepository planningRolePermissionRepository
    ) {
        this.planningUserRoleRepository = planningUserRoleRepository;
        this.planningRoleRepository = planningRoleRepository;
        this.planningRolePermissionRepository = planningRolePermissionRepository;
    }

    public boolean canCreateEntryForTargetUser(Integer requesterUserId, Integer targetUserId, String globalRolesHeader) {
        if (requesterUserId == null || targetUserId == null) {
            return false;
        }

        if (requesterUserId.equals(targetUserId)) {
            return true;
        }

        return hasPlanningPermission(requesterUserId, globalRolesHeader, PERMISSION_CREATE_ANY_ENTRY);
    }

    public boolean canViewCalendarForTargetUser(Integer requesterUserId, Integer targetUserId, String globalRolesHeader) {
        if (requesterUserId == null || targetUserId == null) {
            return false;
        }

        if (requesterUserId.equals(targetUserId)) {
            return true;
        }

        return hasPlanningPermission(requesterUserId, globalRolesHeader, PERMISSION_VIEW_ANY_CALENDAR);
    }

    public boolean canSuggestMeeting(Integer requesterUserId, String globalRolesHeader) {
        // Suggestion is part of meeting creation workflow, no dedicated permission.
        return canCreateMeeting(requesterUserId, globalRolesHeader);
    }

    public boolean canCreateMeeting(Integer requesterUserId, String globalRolesHeader) {
        if (requesterUserId == null) {
            return false;
        }

        return hasPlanningPermission(requesterUserId, globalRolesHeader, PERMISSION_CREATE_MEETING);
    }

    public List<String> getEffectivePermissions(Integer userId, String globalRolesHeader) {
        if (userId == null) {
            return List.of();
        }

        return SUPPORTED_PERMISSIONS.stream()
                .filter(permission -> hasPlanningPermission(userId, globalRolesHeader, permission))
                .toList();
    }

    public boolean hasPlanningPermission(Integer userId, String globalRolesHeader, String permission) {
        if (userId == null || permission == null || permission.isBlank()) {
            return false;
        }

        if (hasRole(globalRolesHeader, ROLE_ADMIN)) {
            return true;
        }

        List<PlanningUserRole> localRoles = planningUserRoleRepository.findByUserId(userId);
        if (localRoles.isEmpty()) {
            return false;
        }

        Set<String> normalizedRoleNames = localRoles.stream()
                .map(PlanningUserRole::getPlanningRole)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        if (normalizedRoleNames.isEmpty()) {
            return false;
        }

        Set<String> activeRoleNames = planningRoleRepository.findByRoleNameInAndActiveTrue(normalizedRoleNames)
                .stream()
                .map(PlanningRole::getRoleName)
                .collect(Collectors.toSet());

        if (activeRoleNames.isEmpty()) {
            return false;
        }

        String normalizedPermission = permission.trim().toUpperCase();
        return planningRolePermissionRepository.findByPlanningRoleIn(activeRoleNames)
                .stream()
                .map(PlanningRolePermission::getPermission)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toUpperCase)
                .anyMatch(normalizedPermission::equals);
    }

    public boolean hasRole(String globalRolesHeader, String expectedRole) {
        if (globalRolesHeader == null || globalRolesHeader.isBlank() || expectedRole == null || expectedRole.isBlank()) {
            return false;
        }

        return Arrays.stream(globalRolesHeader.split(","))
                .map(String::trim)
                .anyMatch(role -> expectedRole.equalsIgnoreCase(role));
    }
}
