package com.nexaris.holidayproxyservice.core.provider;

import java.time.LocalDate;

public record ExternalHolidayApiItem(
        LocalDate date,
        String localName,
        String name
) {
}
