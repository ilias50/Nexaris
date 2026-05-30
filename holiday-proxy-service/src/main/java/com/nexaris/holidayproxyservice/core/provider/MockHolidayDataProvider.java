package com.nexaris.holidayproxyservice.core.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexaris.holidayproxyservice.core.model.HolidayCalendarData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class MockHolidayDataProvider implements HolidayDataProvider {

    private final Map<String, Map<Integer, HolidayCalendarData>> data;

    public MockHolidayDataProvider(
            ObjectMapper objectMapper,
            @Value("classpath:reference/holidays.mock.json") Resource source
    ) {
        this.data = loadData(objectMapper, source);
    }

    @Override
    public Optional<HolidayCalendarData> findByCountryAndYear(String countryCode, int year) {
        return Optional.ofNullable(data.get(countryCode))
                .map(yearMap -> yearMap.get(year));
    }

    @Override
    public Set<String> getAvailableCountryCodes() {
        return data.keySet();
    }

    @Override
    public String sourceName() {
        return "mock-file";
    }

    private Map<String, Map<Integer, HolidayCalendarData>> loadData(ObjectMapper objectMapper, Resource source) {
        try (InputStream in = source.getInputStream()) {
            Map<String, Map<Integer, HolidayCalendarData>> parsed =
                    objectMapper.readValue(in, new TypeReference<>() {
                    });
            return parsed == null ? Collections.emptyMap() : parsed;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load mock holiday data", e);
        }
    }
}
