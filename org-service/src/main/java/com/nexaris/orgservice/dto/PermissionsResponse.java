package com.nexaris.orgservice.dto;

import java.util.Set;

public record PermissionsResponse(
        Integer nodeId,
        Integer userId,
        Set<String> permissions
) {
}
