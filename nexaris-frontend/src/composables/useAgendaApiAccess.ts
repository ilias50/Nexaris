import { authApi, type AdminUser } from '@/api/auth'
import { holidaysApi, type HolidayDay } from '@/api/holidays'

export function useAgendaApiAccess() {
  async function listAssignableUsers() {
    return authApi.listEnabledUsersSafe()
  }

  async function getHolidayCalendar(
    year: number,
    countryCode: string,
    includeSchoolVacations = true,
    languageCode?: string,
  ) {
    const { data } = await holidaysApi.getCalendar(year, countryCode, includeSchoolVacations, languageCode)
    return data
  }

  return {
    listAssignableUsers,
    getHolidayCalendar,
  }
}

export type { AdminUser, HolidayDay }