package com.nexaris.orgservice.dto;

public record CreateNodeRequest(
        Integer parentId,
        String nodeType,
        String name,
        String slug,
        Integer sortOrder,
        Boolean isActive
) {
}
