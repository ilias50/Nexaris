package com.nexaris.holidayproxyservice.core.model;

import java.time.LocalDate;

public record HolidayDay(LocalDate date, String name, String type) {
}
