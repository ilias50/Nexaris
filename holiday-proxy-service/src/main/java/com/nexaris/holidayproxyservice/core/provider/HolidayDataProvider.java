package com.nexaris.holidayproxyservice.core.provider;

import com.nexaris.holidayproxyservice.core.model.HolidayCalendarData;

import java.util.Optional;
import java.util.Set;

public interface HolidayDataProvider {
    Optional<HolidayCalendarData> findByCountryAndYear(String countryCode, int year);

    Set<String> getAvailableCountryCodes();

    String sourceName();
}
