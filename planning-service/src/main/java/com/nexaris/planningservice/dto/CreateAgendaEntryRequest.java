package com.nexaris.planningservice.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CreateAgendaEntryRequest(
        Integer userId,
        String title,
        LocalDateTime startAt,
        LocalDateTime endAt,
        List<Long> tagIds,
        String recurrenceFrequency,
        LocalDate recurrenceUntil,
        List<String> recurrenceWeekdays,
        Boolean recurrenceSkipHolidays,
        List<LocalDate> recurrenceExcludedDates
) {
}
