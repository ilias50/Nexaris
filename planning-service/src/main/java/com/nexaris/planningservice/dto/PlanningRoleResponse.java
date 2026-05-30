package com.nexaris.planningservice.dto;

import java.util.List;

public record PlanningRoleResponse(
        String roleName,
        String description,
        boolean active,
        List<String> permissions
) {
}
