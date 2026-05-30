package com.nexaris.planningservice.dto;

import java.time.LocalTime;

public record UserPreferenceResponse(
        Integer userId,
        LocalTime workDayStart,
        LocalTime workDayEnd,
        Integer preferredMeetingBlockMinutes
) {
}
