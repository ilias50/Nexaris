package com.nexaris.orgservice.dto;

import java.time.LocalDateTime;

public record AnnouncementResponse(
        Integer id,
        Integer nodeId,
        String scopeType,
        String title,
        String body,
        String severity,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean isActive,
        Integer createdByUserId
) {
}
