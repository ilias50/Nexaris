package com.nexaris.planningservice.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AgendaEntryResponse(
        Long id,
        Integer userId,
        String title,
        String source,
        boolean manualLocked,
        LocalDateTime startAt,
        LocalDateTime endAt,
        List<PlanningTagResponse> tags
) {
}
