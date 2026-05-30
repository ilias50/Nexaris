package com.nexaris.orgservice.dto;

import java.time.LocalDateTime;

public record NodeMembershipResponse(
        Integer id,
        Integer nodeId,
        Integer userId,
        String membershipRole,
        boolean isPrimary,
        LocalDateTime activeFrom,
        LocalDateTime activeTo
) {
}
