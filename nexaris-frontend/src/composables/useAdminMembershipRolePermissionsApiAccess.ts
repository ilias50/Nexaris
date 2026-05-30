import { orgApi } from '@/api/org'

export function useAdminMembershipRolePermissionsApiAccess() {
  async function getMembershipRoles() {
    return orgApi.getCatalogValues('MEMBERSHIP_ROLE')
  }

  async function getAvailablePermissions() {
    return orgApi.getAvailablePermissions()
  }

  async function getRolePermissions(role: string) {
    return orgApi.getMembershipRolePermissions(role)
  }

  async function addMembershipRole(value: string) {
    return orgApi.addCatalogValue('MEMBERSHIP_ROLE', value)
  }

  async function replaceRolePermissions(role: string, permissions: string[]) {
    return orgApi.replaceMembershipRolePermissions(role, permissions)
  }

  return {
    getMembershipRoles,
    getAvailablePermissions,
    getRolePermissions,
    addMembershipRole,
    replaceRolePermissions,
  }
}
