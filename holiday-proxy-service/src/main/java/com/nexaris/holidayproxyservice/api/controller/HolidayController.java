package com.nexaris.holidayproxyservice.api.controller;

import com.nexaris.holidayproxyservice.api.dto.CountryCodesResponse;
import com.nexaris.holidayproxyservice.api.dto.HolidayCalendarResponse;
import com.nexaris.holidayproxyservice.core.service.HolidayCalendarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/holidays")
public class HolidayController {

    private final HolidayCalendarService holidayCalendarService;

    public HolidayController(HolidayCalendarService holidayCalendarService) {
        this.holidayCalendarService = holidayCalendarService;
    }

    @GetMapping
    public HolidayCalendarResponse getHolidays(
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "true") boolean includeSchoolVacations,
            @RequestParam(required = false) String languageCode
    ) {
        return holidayCalendarService.getCalendar(countryCode, year, includeSchoolVacations, languageCode);
    }

    @GetMapping("/countries")
    public CountryCodesResponse getSupportedCountries() {
        List<String> countryCodes = holidayCalendarService.getAvailableCountryCodes()
                .stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        return new CountryCodesResponse(countryCodes);
    }
}
