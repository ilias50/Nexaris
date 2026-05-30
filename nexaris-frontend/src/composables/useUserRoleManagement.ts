import { computed, ref, type Ref } from 'vue'
import { authApi, type AdminUser } from '@/api/auth'
import { orgApi, type OrgUserRole } from '@/api/org'
import { planningApi } from '@/api/planning'
import { useAuthStore } from '@/stores/auth'
import { DEFAULT_ACCESS_RULE_ROLES, DEFAULT_AUTH_ROLES, formatRoleLabel, isDefaultUserRole, normalizeRoleCode, normalizeRoleList } from '@/utils/roles'
import { extractRoleNames } from '@/utils/users'
import type { TranslationKey } from '@/i18n/messages'

const PLANNING_ROLE_ADDED_KEY: TranslationKey = 'adminUserRoles.messages.orgRoleAdded'
const PLANNING_ROLE_REMOVED_KEY: TranslationKey = 'adminUserRoles.messages.orgRoleRemoved'
const PLANNING_ROLE_ADD_ERROR_KEY: TranslationKey = 'adminUserRoles.errors.addOrgRole'
const PLANNING_ROLE_REMOVE_ERROR_KEY: TranslationKey = 'adminUserRoles.errors.removeOrgRole'

export function useUserRoleManagement(
  selectedUser: Ref<AdminUser | null>,
  refreshSelectedUser: (userId: number) => Promise<void>,
  t: (key: TranslationKey) => string,
  userOrgRolesRef?: Ref<OrgUserRole[]>,
) {
  const auth = useAuthStore()

  const savingRole = ref(false)
  const roleActionMessage = ref('')
  const roleActionError = ref('')
  const authRoleToAdd = ref<string>(DEFAULT_AUTH_ROLES[0] ?? 'ROLE_USER')
  const orgCatalogRoleToAdd = ref('ROLE_MANAGER')
  const planningRoleToAdd = ref('')
  const authRoleOptions = [...DEFAULT_AUTH_ROLES]
  const orgCatalogRoleOptions = ref<string[]>([])
  const planningRoleOptions = ref<string[]>([])
  const userOrgRoles = userOrgRolesRef ?? ref<OrgUserRole[]>([])
  const userPlanningRoles = ref<string[]>([])

  const selectedUserRoles = computed(() => extractRoleNames(selectedUser.value?.roles))

  async function loadOrgRoleCatalog() {
    try {
      const { data } = await orgApi.getCatalogValues('ACCESS_RULE_ROLE')
      const normalized = normalizeRoleList(data)
      orgCatalogRoleOptions.value = normalized.length ? normalized : [...DEFAULT_ACCESS_RULE_ROLES]
      if (!orgCatalogRoleOptions.value.includes(orgCatalogRoleToAdd.value)) {
        orgCatalogRoleToAdd.value = orgCatalogRoleOptions.value[0] ?? 'ROLE_MANAGER'
      }
    } catch {
      orgCatalogRoleOptions.value = [...DEFAULT_ACCESS_RULE_ROLES]
      orgCatalogRoleToAdd.value = 'ROLE_MANAGER'
    }
  }

  async function loadPlanningRoleCatalog() {
    try {
      const { data } = await planningApi.listPlanningRoles()
      const roleNames = normalizeRoleList(data.map((role) => role.roleName))
      planningRoleOptions.value = roleNames
      if (!planningRoleOptions.value.includes(planningRoleToAdd.value)) {
        planningRoleToAdd.value = planningRoleOptions.value[0] ?? ''
      }
    } catch {
      planningRoleOptions.value = []
      planningRoleToAdd.value = ''
    }
  }

  async function loadUserPlanningRoles() {
    if (!selectedUser.value) {
      userPlanningRoles.value = []
      return
    }

    try {
      const { data } = await planningApi.getUserPlanningRoles(selectedUser.value.id)
      userPlanningRoles.value = normalizeRoleList(data)
    } catch {
      userPlanningRoles.value = []
    }
  }

  async function addAuthRole() {
    if (!selectedUser.value) return
    const role = normalizeRoleCode(authRoleToAdd.value)
    if (!role) return
    if (selectedUserRoles.value.includes(role)) {
      roleActionError.value = t('adminUserRoles.errors.roleAlreadyAssigned')
      roleActionMessage.value = ''
      return
    }

    savingRole.value = true
    roleActionError.value = ''
    roleActionMessage.value = ''
    try {
      await authApi.assignRoleToUser(selectedUser.value.id, role)
      await refreshSelectedUser(selectedUser.value.id)
      roleActionMessage.value = t('adminUserRoles.messages.roleAdded')
    } finally {
      savingRole.value = false
    }
  }

  async function removeAuthRole(roleName: string) {
    if (!selectedUser.value) return
    if (selectedUser.value.id === auth.user?.id && roleName === 'ROLE_ADMIN') {
      roleActionError.value = t('adminUserRoles.errors.cannotRemoveOwnAdmin')
      roleActionMessage.value = ''
      return
    }
    if (isDefaultUserRole(roleName)) {
      roleActionError.value = t('adminUserRoles.errors.cannotRemoveDefaultAuthRole')
      roleActionMessage.value = ''
      return
    }

    savingRole.value = true
    roleActionError.value = ''
    roleActionMessage.value = ''
    try {
      await authApi.revokeRoleFromUser(selectedUser.value.id, roleName)
      await refreshSelectedUser(selectedUser.value.id)
      roleActionMessage.value = t('adminUserRoles.messages.roleRemoved')
    } finally {
      savingRole.value = false
    }
  }

  async function addOrgCatalogRole() {
    if (!selectedUser.value) return
    const role = normalizeRoleCode(orgCatalogRoleToAdd.value)
    if (!role) return
    if (userOrgRoles.value.some((currentRole) => normalizeRoleCode(currentRole.roleName) === role)) {
      roleActionError.value = t('adminUserRoles.errors.roleAlreadyAssigned')
      roleActionMessage.value = ''
      return
    }

    savingRole.value = true
    roleActionError.value = ''
    roleActionMessage.value = ''
    try {
      const { data } = await orgApi.assignUserGlobalRole(selectedUser.value.id, role)
      userOrgRoles.value = data
      roleActionMessage.value = t('adminUserRoles.messages.orgRoleAdded')
    } finally {
      savingRole.value = false
    }
  }

  async function removeOrgCatalogRole(roleName: string) {
    if (!selectedUser.value) return
    if (isDefaultUserRole(roleName)) {
      roleActionError.value = t('adminUserRoles.errors.cannotRemoveDefaultOrgRole')
      roleActionMessage.value = ''
      return
    }

    savingRole.value = true
    roleActionError.value = ''
    roleActionMessage.value = ''
    try {
      const { data } = await orgApi.removeUserGlobalRole(selectedUser.value.id, roleName)
      userOrgRoles.value = data
      roleActionMessage.value = t('adminUserRoles.messages.orgRoleRemoved')
    } finally {
      savingRole.value = false
    }
  }

  async function addPlanningRole() {
    if (!selectedUser.value) return
    const role = normalizeRoleCode(planningRoleToAdd.value)
    if (!role) return
    if (userPlanningRoles.value.includes(role)) {
      roleActionError.value = t('adminUserRoles.errors.roleAlreadyAssigned')
      roleActionMessage.value = ''
      return
    }

    savingRole.value = true
    roleActionError.value = ''
    roleActionMessage.value = ''
    try {
      await planningApi.assignPlanningRoleToUser(selectedUser.value.id, role)
      await loadUserPlanningRoles()
      roleActionMessage.value = t(PLANNING_ROLE_ADDED_KEY)
    } catch {
      roleActionError.value = t(PLANNING_ROLE_ADD_ERROR_KEY)
    } finally {
      savingRole.value = false
    }
  }

  async function removePlanningRole(roleName: string) {
    if (!selectedUser.value) return

    savingRole.value = true
    roleActionError.value = ''
    roleActionMessage.value = ''
    try {
      await planningApi.revokePlanningRoleFromUser(selectedUser.value.id, roleName)
      await loadUserPlanningRoles()
      roleActionMessage.value = t(PLANNING_ROLE_REMOVED_KEY)
    } catch {
      roleActionError.value = t(PLANNING_ROLE_REMOVE_ERROR_KEY)
    } finally {
      savingRole.value = false
    }
  }

  return {
    authRoleToAdd,
    authRoleOptions,
    orgCatalogRoleToAdd,
    orgCatalogRoleOptions,
    planningRoleToAdd,
    planningRoleOptions,
    userOrgRoles,
    userPlanningRoles,
    selectedUserRoles,
    savingRole,
    roleActionMessage,
    roleActionError,
    loadOrgRoleCatalog,
    loadPlanningRoleCatalog,
    loadUserPlanningRoles,
    addAuthRole,
    removeAuthRole,
    addOrgCatalogRole,
    removeOrgCatalogRole,
    addPlanningRole,
    removePlanningRole,
  }
}