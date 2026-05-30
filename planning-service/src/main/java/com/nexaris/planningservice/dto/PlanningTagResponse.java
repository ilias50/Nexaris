package com.nexaris.planningservice.dto;

public record PlanningTagResponse(
        Long id,
        String name,
        String description,
        String color,
        boolean active,
        boolean blocking,
        Integer createdByUserId
) {
}
