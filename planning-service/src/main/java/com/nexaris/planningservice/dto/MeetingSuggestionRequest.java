package com.nexaris.planningservice.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MeetingSuggestionRequest(
        List<Integer> participantUserIds,
        Integer durationMinutes,
        LocalDateTime windowStart,
        LocalDateTime windowEnd,
        Integer maxSuggestions
) {
}
