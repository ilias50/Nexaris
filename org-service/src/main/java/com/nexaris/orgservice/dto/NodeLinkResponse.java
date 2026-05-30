package com.nexaris.orgservice.dto;

public record NodeLinkResponse(
        Integer id,
        Integer nodeId,
        String label,
        String url,
        String category,
        String icon,
        String visibility,
        Integer sortOrder,
        boolean isActive
) {
}
