package com.nexaris.orgservice.dto;

import java.time.LocalDateTime;

public record AnnouncementRequest(
        Integer nodeId,
        String scopeType,
        String title,
        String body,
        String severity,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Boolean isActive,
        Integer createdByUserId
) {
}
