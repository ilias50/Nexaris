package com.nexaris.planningservice.dto;

import java.util.List;

public record MeetingSuggestionResult(
        Integer requestedDurationMinutes,
        Integer scannedSlots,
        List<MeetingSlotSuggestionResponse> suggestions
) {
}
