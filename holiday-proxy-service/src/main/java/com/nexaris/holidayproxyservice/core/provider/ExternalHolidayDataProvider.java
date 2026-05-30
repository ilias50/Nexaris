package com.nexaris.holidayproxyservice.core.provider;

import com.nexaris.holidayproxyservice.core.model.HolidayCalendarData;
import com.nexaris.holidayproxyservice.core.model.HolidayDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class ExternalHolidayDataProvider implements HolidayDataProvider {

    private static final Logger log = LoggerFactory.getLogger(ExternalHolidayDataProvider.class);

    private final RestClient restClient;
    private final String publicHolidaysPath;
        private static final Map<String, String> FRENCH_HOLIDAY_BY_ENGLISH_NAME = Map.ofEntries(
            Map.entry("New Year's Day", "Jour de l'an"),
            Map.entry("Good Friday", "Vendredi saint"),
            Map.entry("Easter Sunday", "Paques"),
            Map.entry("Easter Monday", "Lundi de Paques"),
            Map.entry("Labour Day", "Fete du Travail"),
            Map.entry("Ascension Day", "Ascension"),
            Map.entry("Day after Ascension Day", "Lendemain de l'Ascension"),
            Map.entry("Whit Monday", "Lundi de Pentecote"),
            Map.entry("Belgian National Day", "Fete nationale belge"),
            Map.entry("Assumption Day", "Assomption"),
            Map.entry("All Saints' Day", "Toussaint"),
            Map.entry("Armistice Day", "Armistice"),
            Map.entry("Christmas Day", "Noel"),
            Map.entry("St. Stephen's Day", "Saint-Etienne")
        );

    public ExternalHolidayDataProvider(
            @Value("${holiday.external.base-url}") String baseUrl,
            @Value("${holiday.external.public-holidays-path}") String publicHolidaysPath
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.publicHolidaysPath = publicHolidaysPath;
    }

    @Override
    public Optional<HolidayCalendarData> findByCountryAndYear(String countryCode, int year) {
        return findByCountryAndYear(countryCode, year, null);
    }

    public Optional<HolidayCalendarData> findByCountryAndYear(String countryCode, int year, String languageCode) {
        try {
            ExternalHolidayApiItem[] response = restClient.get()
                    .uri(publicHolidaysPath, year, countryCode)
                    .retrieve()
                    .body(ExternalHolidayApiItem[].class);

            if (response == null) {
                return Optional.empty();
            }

            List<HolidayDay> holidays = Arrays.stream(response)
                    .filter(item -> item.date() != null)
                    .map(item -> new HolidayDay(
                            item.date(),
                        pickHolidayName(item, countryCode, languageCode),
                            "PUBLIC_HOLIDAY"
                    ))
                    .toList();

            return Optional.of(new HolidayCalendarData(holidays, List.of()));
        } catch (Exception ex) {
            log.warn("External holiday API request failed for country={} year={}: {}", countryCode, year, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Set<String> getAvailableCountryCodes() {
        return Set.of();
    }

    @Override
    public String sourceName() {
        return "external-api";
    }

    private static String pickHolidayName(ExternalHolidayApiItem item, String countryCode, String languageCode) {
        String normalizedLanguage = languageCode == null ? "" : languageCode.trim().toUpperCase();
        String normalizedCountry = countryCode == null ? "" : countryCode.trim().toUpperCase();

        String localName = item.localName();
        String globalName = item.name();

        // English users expect international names; other locales prefer local names when available.
        if ("EN".equals(normalizedLanguage)) {
            if (globalName != null && !globalName.isBlank()) {
                return globalName;
            }
            return localName;
        }

        if ("FR".equals(normalizedLanguage)) {
            // In FR country data, local names are already French.
            if ("FR".equals(normalizedCountry) && localName != null && !localName.isBlank()) {
                return localName;
            }

            // For countries like BE where local names may be Dutch, prefer explicit French mapping.
            if (globalName != null && !globalName.isBlank()) {
                String translated = FRENCH_HOLIDAY_BY_ENGLISH_NAME.get(globalName);
                if (translated != null && !translated.isBlank()) {
                    return translated;
                }
            }

            if (localName != null && !localName.isBlank()) {
                return localName;
            }

            return globalName;
        }

        if (localName != null && !localName.isBlank()) {
            return localName;
        }

        return globalName;
    }
}
