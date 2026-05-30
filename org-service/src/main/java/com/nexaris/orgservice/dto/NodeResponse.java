package com.nexaris.orgservice.dto;

public record NodeResponse(
        Integer id,
        Integer parentId,
        String nodeType,
        String name,
        String slug,
        String path,
        Integer depth,
        Integer sortOrder,
        boolean isActive
) {
}
