package com.nexaris.planningservice.services;

import com.nexaris.planningservice.dto.CreatePlanningRoleRequest;
import com.nexaris.planningservice.dto.PlanningRoleResponse;
import com.nexaris.planningservice.entities.PlanningRole;
import com.nexaris.planningservice.entities.PlanningRolePermission;
import com.nexaris.planningservice.entities.PlanningUserRole;
import com.nexaris.planningservice.repositories.PlanningRolePermissionRepository;
import com.nexaris.planningservice.repositories.PlanningRoleRepository;
import com.nexaris.planningservice.repositories.PlanningUserRoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PlanningRoleAdminService {

    private static final Set<String> ALLOWED_PERMISSIONS = Set.of(
            PlanningAccessService.PERMISSION_CREATE_ANY_ENTRY,
            PlanningAccessService.PERMISSION_CREATE_MEETING,
            PlanningAccessService.PERMISSION_VIEW_ANY_CALENDAR
    );

    private final PlanningRoleRepository planningRoleRepository;
    private final PlanningRolePermissionRepository planningRolePermissionRepository;
    private final PlanningUserRoleRepository planningUserRoleRepository;
    private final PlanningAccessService planningAccessService;

    public PlanningRoleAdminService(
            PlanningRoleRepository planningRoleRepository,
            PlanningRolePermissionRepository planningRolePermissionRepository,
            PlanningUserRoleRepository planningUserRoleRepository,
            PlanningAccessService planningAccessService
    ) {
        this.planningRoleRepository = planningRoleRepository;
        this.planningRolePermissionRepository = planningRolePermissionRepository;
        this.planningUserRoleRepository = planningUserRoleRepository;
        this.planningAccessService = planningAccessService;
    }

    public List<PlanningRoleResponse> listRoles() {
        return planningRoleRepository.findAllByOrderByRoleNameAsc()
                .stream()
                .map(this::toRoleResponse)
                .toList();
    }

    public List<String> listAllowedPermissions(Integer requesterUserId, String rolesHeader) {
        requireAdmin(requesterUserId, rolesHeader);
        return ALLOWED_PERMISSIONS.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    public List<String> getUserRoles(Integer requesterUserId, String rolesHeader, Integer targetUserId) {
        requireAdmin(requesterUserId, rolesHeader);

        if (targetUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetUserId est obligatoire");
        }

        return planningUserRoleRepository.findByUserId(targetUserId)
                .stream()
                .map(PlanningUserRole::getPlanningRole)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional
    public PlanningRoleResponse createRole(Integer requesterUserId, String rolesHeader, CreatePlanningRoleRequest request) {
        requireAdmin(requesterUserId, rolesHeader);

        if (request == null || request.roleName() == null || request.roleName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roleName est obligatoire");
        }

        String normalizedRoleName = normalizeRoleName(request.roleName());
        if (planningRoleRepository.existsByRoleName(normalizedRoleName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce role planning existe deja");
        }

        PlanningRole role = new PlanningRole();
        role.setRoleName(normalizedRoleName);
        role.setDescription(request.description());
        role.setActive(request.active() == null || request.active());
        role.setCreatedByUserId(requesterUserId);
        role.setCreatedAt(LocalDateTime.now());

        return toRoleResponse(planningRoleRepository.save(role));
    }

    @Transactional
    public PlanningRoleResponse replaceRolePermissions(
            Integer requesterUserId,
            String rolesHeader,
            String roleName,
            List<String> permissions
    ) {
        requireAdmin(requesterUserId, rolesHeader);

        String normalizedRoleName = normalizeRoleName(roleName);
        PlanningRole role = planningRoleRepository.findByRoleName(normalizedRoleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role planning introuvable"));

        Set<String> normalizedPermissions = normalizePermissions(permissions);

        planningRolePermissionRepository.deleteByPlanningRole(normalizedRoleName);
        if (!normalizedPermissions.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (String permission : normalizedPermissions) {
                PlanningRolePermission row = new PlanningRolePermission();
                row.setPlanningRole(normalizedRoleName);
                row.setPermission(permission);
                row.setCreatedAt(now);
                planningRolePermissionRepository.save(row);
            }
        }

        return toRoleResponse(role);
    }

    @Transactional
    public void assignRoleToUser(Integer requesterUserId, String rolesHeader, Integer targetUserId, String roleName) {
        requireAdmin(requesterUserId, rolesHeader);

        if (targetUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetUserId est obligatoire");
        }

        String normalizedRoleName = normalizeRoleName(roleName);
        PlanningRole role = planningRoleRepository.findByRoleName(normalizedRoleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role planning introuvable"));

        if (!role.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role planning inactif");
        }

        if (planningUserRoleRepository.existsByUserIdAndPlanningRole(targetUserId, normalizedRoleName)) {
            return;
        }

        PlanningUserRole userRole = new PlanningUserRole();
        userRole.setUserId(targetUserId);
        userRole.setPlanningRole(normalizedRoleName);
        userRole.setCreatedAt(LocalDateTime.now());
        planningUserRoleRepository.save(userRole);
    }

    @Transactional
    public void revokeRoleFromUser(Integer requesterUserId, String rolesHeader, Integer targetUserId, String roleName) {
        requireAdmin(requesterUserId, rolesHeader);

        if (targetUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetUserId est obligatoire");
        }

        String normalizedRoleName = normalizeRoleName(roleName);
        planningUserRoleRepository.deleteByUserIdAndPlanningRole(targetUserId, normalizedRoleName);
    }

    private void requireAdmin(Integer requesterUserId, String rolesHeader) {
        if (requesterUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id est obligatoire");
        }

        if (!planningAccessService.hasRole(rolesHeader, PlanningAccessService.ROLE_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role admin requis");
        }
    }

    private PlanningRoleResponse toRoleResponse(PlanningRole role) {
        List<String> permissions = planningRolePermissionRepository.findByPlanningRoleOrderByPermissionAsc(role.getRoleName())
                .stream()
                .map(PlanningRolePermission::getPermission)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(permission -> !permission.isBlank())
                .map(String::toUpperCase)
            .filter(ALLOWED_PERMISSIONS::contains)
                .distinct()
                .toList();

        return new PlanningRoleResponse(
                role.getRoleName(),
                role.getDescription(),
                role.isActive(),
                permissions
        );
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roleName est obligatoire");
        }

        String normalized = roleName.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        if (!normalized.startsWith("PLANNING_")) {
            normalized = "PLANNING_" + normalized;
        }
        return normalized;
    }

    private Set<String> normalizePermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Set.of();
        }

        Set<String> normalized = new HashSet<>();
        for (String permission : permissions) {
            if (permission == null || permission.isBlank()) {
                continue;
            }
            String normalizedPermission = permission.trim().toUpperCase();
            if (!ALLOWED_PERMISSIONS.contains(normalizedPermission)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permission planning invalide: " + normalizedPermission);
            }
            normalized.add(normalizedPermission);
        }
        return normalized;
    }
}
