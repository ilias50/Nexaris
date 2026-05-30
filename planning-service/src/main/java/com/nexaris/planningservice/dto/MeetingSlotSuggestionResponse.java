package com.nexaris.planningservice.dto;

import java.time.LocalDateTime;

public record MeetingSlotSuggestionResponse(
        LocalDateTime startAt,
        LocalDateTime endAt,
        Integer score,
        String rationale
) {
}
