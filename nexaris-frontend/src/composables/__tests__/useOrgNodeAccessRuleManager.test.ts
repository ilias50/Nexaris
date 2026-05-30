import { describe, expect, it, beforeEach, vi } from 'vitest'
import { ref } from 'vue'
import { useOrgNodeAccessRuleManager } from '@/composables/useOrgNodeAccessRuleManager'

const orgMocks = vi.hoisted(() => ({
  addCatalogValue: vi.fn(),
  getCatalogValues: vi.fn(),
  getAccessRules: vi.fn(),
  createAccessRule: vi.fn(),
  deleteAccessRule: vi.fn(),
}))

vi.mock('@/api/org', () => ({
  orgApi: {
    addCatalogValue: orgMocks.addCatalogValue,
    getCatalogValues: orgMocks.getCatalogValues,
    getAccessRules: orgMocks.getAccessRules,
    createAccessRule: orgMocks.createAccessRule,
    deleteAccessRule: orgMocks.deleteAccessRule,
  },
}))

describe('useOrgNodeAccessRuleManager', () => {
  beforeEach(() => {
    orgMocks.addCatalogValue.mockReset()
    orgMocks.getCatalogValues.mockReset()
    orgMocks.getAccessRules.mockReset()
    orgMocks.createAccessRule.mockReset()
    orgMocks.deleteAccessRule.mockReset()
  })

  it('updates subject options when subject type changes to USER', () => {
    const userOptions = ref([{ id: 7, label: 'Alice (#7)' }])
    const memberRoles = ref<string[]>(['ROLE_MEMBER'])
    const composable = useOrgNodeAccessRuleManager(ref(31), userOptions, memberRoles)

    composable.accessRuleForm.value.subjectType = 'USER'
    composable.onRuleSubjectTypeChange()

    expect(composable.accessRuleForm.value.subjectValue).toBe('7')
  })

  it('adds and normalizes access rule role', async () => {
    orgMocks.addCatalogValue.mockResolvedValue({ data: ['ROLE_ADMIN', 'ROLE_MANAGER'] })

    const composable = useOrgNodeAccessRuleManager(ref(31), ref([]), ref([]))
    composable.newAccessRoleInput.value = 'manager'
    composable.accessRuleForm.value.subjectType = 'ROLE'

    await composable.addAccessRuleRole()

    expect(orgMocks.addCatalogValue).toHaveBeenCalledWith('ACCESS_RULE_ROLE', 'ROLE_MANAGER')
    expect(composable.accessRuleRoleOptions.value).toEqual(['ROLE_ADMIN', 'ROLE_MANAGER'])
    expect(composable.accessRuleForm.value.subjectValue).toBe('ROLE_MANAGER')
  })
})
