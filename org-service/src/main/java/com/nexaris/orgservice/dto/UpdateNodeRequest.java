package com.nexaris.orgservice.dto;

public record UpdateNodeRequest(
        String nodeType,
        String name,
        String slug,
        Integer sortOrder,
        Boolean isActive
) {
}
