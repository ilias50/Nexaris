package com.nexaris.planningservice.dto;

import java.util.List;

public record ReplacePlanningRolePermissionsRequest(
        List<String> permissions
) {
}
