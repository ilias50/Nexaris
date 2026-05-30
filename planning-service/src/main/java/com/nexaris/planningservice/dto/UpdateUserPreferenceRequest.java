package com.nexaris.planningservice.dto;

import java.time.LocalTime;

public record UpdateUserPreferenceRequest(
        LocalTime workDayStart,
        LocalTime workDayEnd,
        Integer preferredMeetingBlockMinutes
) {
}
