package com.nexaris.holidayproxyservice.core.model;

import java.util.List;

public record HolidayCalendarData(List<HolidayDay> holidays, List<HolidayDay> schoolVacations) {
}
