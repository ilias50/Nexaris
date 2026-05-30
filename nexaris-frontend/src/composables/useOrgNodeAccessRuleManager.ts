import { computed, ref, type Ref } from 'vue'
import { orgApi, type NodeAccessRule } from '@/api/org'
import { DEFAULT_ACCESS_RULE_ROLES, formatRoleLabel, normalizeRoleCode } from '@/utils/roles'
import type { UserOption } from '@/composables/useOrgNodeMembershipManager'
import { isBlank, trimOrEmpty } from '@/utils/validation'

const FALLBACK_RULE_PERMISSIONS = [
  'READ',
  'EDIT_CONTENT',
  'EDIT_LINKS',
  'MANAGE_MEMBERS',
  'MANAGE_ACCESS',
  'MANAGE_ANNOUNCEMENTS',
  'CREATE_CHILD',
  'DELETE_NODE',
]

export function useOrgNodeAccessRuleManager(
  nodeId: Ref<number>,
  userOptions: Ref<UserOption[]>,
  memberRoles: Ref<string[]>,
) {
  const accessRules = ref<NodeAccessRule[]>([])
  const accessRuleForm = ref({
    effect: 'ALLOW',
    subjectType: 'ROLE',
    subjectValue: '',
    permission: 'READ',
    appliesToChildren: false,
  })
  const accessRuleSaving = ref(false)
  const accessRuleRoleOptions = ref<string[]>([])
  const accessRulePermissionOptions = ref<string[]>([])
  const newAccessRoleInput = ref('')

  const RULE_EFFECTS = ['ALLOW', 'DENY']
  const RULE_SUBJECT_TYPES = ['USER', 'ROLE', 'MEMBERSHIP']

  const subjectValueOptions = computed(() => {
    if (accessRuleForm.value.subjectType === 'USER') {
      return userOptions.value.map((user) => ({ value: String(user.id), label: user.label }))
    }

    if (accessRuleForm.value.subjectType === 'MEMBERSHIP') {
      return memberRoles.value.map((role) => ({ value: role, label: formatRoleLabel(role) }))
    }

    return accessRuleRoleOptions.value.map((role) => ({ value: role, label: formatRoleLabel(role) }))
  })

  function onRuleSubjectTypeChange() {
    accessRuleForm.value.subjectValue = subjectValueOptions.value[0]?.value ?? ''
  }

  async function addAccessRuleRole() {
    const value = normalizeRoleCode(newAccessRoleInput.value)
    if (!value) return

    newAccessRoleInput.value = ''
    const response = await orgApi.addCatalogValue('ACCESS_RULE_ROLE', value)
    accessRuleRoleOptions.value = response.data

    if (accessRuleForm.value.subjectType === 'ROLE') {
      accessRuleForm.value.subjectValue = value
    }
  }

  async function loadAccessRuleRoleCatalog() {
    try {
      const response = await orgApi.getCatalogValues('ACCESS_RULE_ROLE')
      accessRuleRoleOptions.value = response.data
    } catch {
      accessRuleRoleOptions.value = [...DEFAULT_ACCESS_RULE_ROLES]
    }

    if (!accessRuleForm.value.subjectValue) {
      onRuleSubjectTypeChange()
    }
  }

  async function loadAccessRulePermissionCatalog() {
    try {
      const response = await orgApi.getCatalogValues('ACCESS_RULE_PERMISSION')
      accessRulePermissionOptions.value = response.data.length ? response.data : FALLBACK_RULE_PERMISSIONS
    } catch {
      accessRulePermissionOptions.value = FALLBACK_RULE_PERMISSIONS
    }

    if (!accessRulePermissionOptions.value.includes(accessRuleForm.value.permission)) {
      accessRuleForm.value.permission = accessRulePermissionOptions.value[0] ?? 'READ'
    }
  }

  async function refreshAccessRules() {
    if (!nodeId.value) return
    try {
      accessRules.value = (await orgApi.getAccessRules(nodeId.value)).data
    } catch {
      accessRules.value = []
    }
  }

  async function addAccessRule() {
    if (!nodeId.value || isBlank(accessRuleForm.value.subjectValue)) return

    accessRuleSaving.value = true
    try {
      await orgApi.createAccessRule(nodeId.value, {
        effect: accessRuleForm.value.effect,
        subjectType: accessRuleForm.value.subjectType,
        subjectValue: trimOrEmpty(accessRuleForm.value.subjectValue),
        permission: accessRuleForm.value.permission,
        appliesToChildren: accessRuleForm.value.appliesToChildren,
      })
      await refreshAccessRules()
    } finally {
      accessRuleSaving.value = false
    }
  }

  async function removeAccessRule(ruleId: number) {
    await orgApi.deleteAccessRule(ruleId)
    await refreshAccessRules()
  }

  return {
    accessRules,
    accessRuleForm,
    accessRuleSaving,
    accessRuleRoleOptions,
    accessRulePermissionOptions,
    newAccessRoleInput,
    RULE_EFFECTS,
    RULE_SUBJECT_TYPES,
    subjectValueOptions,
    onRuleSubjectTypeChange,
    addAccessRuleRole,
    loadAccessRuleRoleCatalog,
    loadAccessRulePermissionCatalog,
    refreshAccessRules,
    addAccessRule,
    removeAccessRule,
  }
}
