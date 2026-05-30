import { planningApi } from '@/api/planning'

export function useAdminPlanningRolePermissionsApiAccess() {
  async function getPlanningRoles() {
    return planningApi.listPlanningRoles()
  }

  async function getAvailablePermissions() {
    return planningApi.listAvailablePlanningPermissions()
  }

  async function addPlanningRole(roleName: string) {
    return planningApi.createPlanningRole({ roleName })
  }

  async function replaceRolePermissions(roleName: string, permissions: string[]) {
    return planningApi.replacePlanningRolePermissions(roleName, { permissions })
  }

  return {
    getPlanningRoles,
    getAvailablePermissions,
    addPlanningRole,
    replaceRolePermissions,
  }
}
