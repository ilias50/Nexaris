package com.nexaris.planningservice.dto;

public record CreatePlanningTagRequest(
        String name,
        String description,
        String color,
        Boolean blocking,
        Boolean active
) {
}
