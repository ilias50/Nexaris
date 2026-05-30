package com.nexaris.orgservice.dto;

public record UserRightsPurgeResponse(
        Integer userId,
        long removedGlobalRoles,
        long removedMemberships,
        long removedUserAccessRules
) {
}
