import apiClient from './index'

/** One holiday or school-vacation day as returned by the holiday-proxy-service */
export interface HolidayDay {
  date: string  // "YYYY-MM-DD"
  name: string
  type: string  // e.g. "PUBLIC_HOLIDAY"
}

export interface HolidayCalendarResponse {
  countryCode: string
  year: number
  source: string
  holidays: HolidayDay[]
  schoolVacations: HolidayDay[]
}

export interface CountryCodesResponse {
  countryCodes: string[]
}

export const holidaysApi = {
  getCalendar(
    year: number,
    countryCode: string,
    includeSchoolVacations = true,
    languageCode?: string,
  ) {
    return apiClient.get<HolidayCalendarResponse>('/api/v1/holidays', {
      params: { countryCode, year, includeSchoolVacations, languageCode },
    })
  },

  getSupportedCountries() {
    return apiClient.get<CountryCodesResponse>('/api/v1/holidays/countries')
  },
}
