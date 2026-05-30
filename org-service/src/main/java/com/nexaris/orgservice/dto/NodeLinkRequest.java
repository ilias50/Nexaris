package com.nexaris.orgservice.dto;

public record NodeLinkRequest(
        String label,
        String url,
        String category,
        String icon,
        String visibility,
        Integer sortOrder,
        Boolean isActive
) {
}
