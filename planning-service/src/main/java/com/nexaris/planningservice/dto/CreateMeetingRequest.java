package com.nexaris.planningservice.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CreateMeetingRequest(
        String title,
        List<Integer> participantUserIds,
        LocalDateTime startAt,
        LocalDateTime endAt,
        List<Long> tagIds
) {
}
