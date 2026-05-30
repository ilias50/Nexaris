package com.nexaris.orgservice.dto;

import java.time.LocalDateTime;

public record NodeMembershipRequest(
        Integer userId,
        String membershipRole,
        Boolean isPrimary,
        LocalDateTime activeFrom,
        LocalDateTime activeTo
) {
}
