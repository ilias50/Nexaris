package com.nexaris.holidayproxyservice.core.service;

import com.nexaris.holidayproxyservice.api.dto.HolidayCalendarResponse;
import com.nexaris.holidayproxyservice.core.model.HolidayCalendarData;
import com.nexaris.holidayproxyservice.core.model.HolidayDay;
import com.nexaris.holidayproxyservice.core.provider.ExternalHolidayDataProvider;
import com.nexaris.holidayproxyservice.core.provider.MockHolidayDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class HolidayCalendarService {

    private final MockHolidayDataProvider mockHolidayDataProvider;
    private final ExternalHolidayDataProvider externalHolidayDataProvider;
    private final String defaultCountryCode;
    private final String providerMode;
    private final boolean fallbackToMock;

    public HolidayCalendarService(
            MockHolidayDataProvider mockHolidayDataProvider,
            ExternalHolidayDataProvider externalHolidayDataProvider,
            @Value("${holiday.provider.mode:external}") String providerMode,
            @Value("${holiday.provider.fallback-to-mock:true}") boolean fallbackToMock,
            @Value("${holiday.default-country:FR}") String defaultCountryCode
    ) {
        this.mockHolidayDataProvider = mockHolidayDataProvider;
        this.externalHolidayDataProvider = externalHolidayDataProvider;
        this.providerMode = providerMode == null ? "external" : providerMode.trim().toLowerCase();
        this.fallbackToMock = fallbackToMock;
        this.defaultCountryCode = normalizeCountryCode(defaultCountryCode);
    }

    public HolidayCalendarResponse getCalendar(String countryCode, Integer year, boolean includeSchoolVacations) {
        return getCalendar(countryCode, year, includeSchoolVacations, null);
        }

        public HolidayCalendarResponse getCalendar(
            String countryCode,
            Integer year,
            boolean includeSchoolVacations,
            String languageCode
        ) {
        String normalizedCountryCode = countryCode == null || countryCode.isBlank()
                ? defaultCountryCode
                : normalizeCountryCode(countryCode);

        int normalizedYear = year == null ? Year.now().getValue() : year;
        validateYear(normalizedYear);

        String normalizedLanguageCode = normalizeLanguageCode(languageCode);

        ProviderResult providerResult = resolveCalendarData(
            normalizedCountryCode,
            normalizedYear,
            normalizedLanguageCode
        );
        HolidayCalendarData data = providerResult.data();

        List<HolidayDay> schoolVacations = includeSchoolVacations
                ? data.schoolVacations()
                : List.of();

        return new HolidayCalendarResponse(
                normalizedCountryCode,
                normalizedYear,
                providerResult.sourceName(),
                data.holidays(),
                schoolVacations
        );
    }

    public Set<String> getAvailableCountryCodes() {
        Set<String> mockCountries = mockHolidayDataProvider.getAvailableCountryCodes();
        if ("mock".equals(providerMode)) {
            return mockCountries;
        }

        Set<String> externalCountries = externalHolidayDataProvider.getAvailableCountryCodes();
        if (!externalCountries.isEmpty()) {
            return externalCountries;
        }

        return fallbackToMock ? mockCountries : Set.of();
    }

    private ProviderResult resolveCalendarData(String countryCode, int year, String languageCode) {
        if ("mock".equals(providerMode)) {
            return fromProvider(mockHolidayDataProvider, countryCode, year);
        }

        Optional<HolidayCalendarData> externalData = externalHolidayDataProvider.findByCountryAndYear(
                countryCode,
                year,
                languageCode
        );
        if (externalData.isPresent()) {
            return new ProviderResult(externalHolidayDataProvider.sourceName(), externalData.get());
        }

        if (fallbackToMock) {
            return fromProvider(mockHolidayDataProvider, countryCode, year);
        }

        throw new IllegalArgumentException(
                "No holiday data found for country " + countryCode + " and year " + year
        );
    }

    private static ProviderResult fromProvider(MockHolidayDataProvider provider, String countryCode, int year) {
        HolidayCalendarData data = provider.findByCountryAndYear(countryCode, year)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No holiday data found for country " + countryCode + " and year " + year
                ));
        return new ProviderResult(provider.sourceName(), data);
    }

    private static String normalizeCountryCode(String countryCode) {
        String normalized = countryCode.trim().toUpperCase();
        if (!normalized.matches("^[A-Z]{2}$")) {
            throw new IllegalArgumentException("countryCode must follow ISO 3166-1 alpha-2 format");
        }
        return normalized;
    }

    private static void validateYear(int year) {
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("year must be between 2000 and 2100");
        }
    }

    private static String normalizeLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return "EN";
        }

        String normalized = languageCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() < 2) {
            return "EN";
        }

        return normalized.substring(0, 2);
    }

    private record ProviderResult(String sourceName, HolidayCalendarData data) {
    }
}
