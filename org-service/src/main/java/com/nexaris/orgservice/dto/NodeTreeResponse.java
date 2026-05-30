package com.nexaris.orgservice.dto;

import java.util.List;

public record NodeTreeResponse(
        Integer id,
        Integer parentId,
        String nodeType,
        String name,
        String slug,
        String path,
        Integer depth,
        Integer sortOrder,
        boolean isActive,
        List<NodeTreeResponse> children
) {
}
