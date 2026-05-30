package com.nexaris.holidayproxyservice.api.dto;

import com.nexaris.holidayproxyservice.core.model.HolidayDay;

import java.util.List;

public record HolidayCalendarResponse(
        String countryCode,
        int year,
        String source,
        List<HolidayDay> holidays,
        List<HolidayDay> schoolVacations
) {
}
