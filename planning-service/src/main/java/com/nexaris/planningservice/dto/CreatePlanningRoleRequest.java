package com.nexaris.planningservice.dto;

public record CreatePlanningRoleRequest(
        String roleName,
        String description,
        Boolean active
) {
}
