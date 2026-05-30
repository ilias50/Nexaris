package com.nexaris.holidayproxyservice.api.controller;

import com.nexaris.holidayproxyservice.api.dto.CountryCodesResponse;
import com.nexaris.holidayproxyservice.api.dto.HolidayCalendarResponse;
import com.nexaris.holidayproxyservice.core.model.HolidayDay;
import com.nexaris.holidayproxyservice.core.service.HolidayCalendarService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayControllerTest {

    @Mock
    private HolidayCalendarService holidayCalendarService;

    @InjectMocks
    private HolidayController holidayController;

    @Test
    void getSupportedCountries_ShouldReturnSortedCountryCodes() {
        when(holidayCalendarService.getAvailableCountryCodes())
                .thenReturn(Set.of("DE", "BE", "FR"));

        CountryCodesResponse response = holidayController.getSupportedCountries();

        assertThat(response.countryCodes()).containsExactly("BE", "DE", "FR");
        verify(holidayCalendarService).getAvailableCountryCodes();
    }

    @Test
    void getHolidays_ShouldDelegateToService() {
        HolidayCalendarResponse expected = new HolidayCalendarResponse(
                "BE",
                2026,
                "external",
                List.of(new HolidayDay(LocalDate.of(2026, 1, 1), "New Year", "PUBLIC")),
                List.of()
        );
        when(holidayCalendarService.getCalendar("BE", 2026, false, "FR")).thenReturn(expected);

        HolidayCalendarResponse response = holidayController.getHolidays("BE", 2026, false, "FR");

        assertThat(response).isEqualTo(expected);
        verify(holidayCalendarService).getCalendar("BE", 2026, false, "FR");
    }
}
