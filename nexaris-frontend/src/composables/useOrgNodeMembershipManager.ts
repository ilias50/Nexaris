import { ref, type Ref } from 'vue'
import { authApi } from '@/api/auth'
import { orgApi, type NodeMembership } from '@/api/org'
import { formatUserDisplayName } from '@/utils/users'

export type UserOption = { id: number; label: string }

export function useOrgNodeMembershipManager(nodeId: Ref<number>) {
  const memberships = ref<NodeMembership[]>([])
  const membershipForm = ref({ userId: '', membershipRole: '' })
  const membershipSaving = ref(false)
  const userOptions = ref<UserOption[]>([])
  const memberRoles = ref<string[]>([])
  const newMemberRoleInput = ref('')

  function setMemberRoles(nextRoles: string[]) {
    memberRoles.value = [...nextRoles]
    if (!memberRoles.value.includes(membershipForm.value.membershipRole)) {
      membershipForm.value.membershipRole = memberRoles.value[0] ?? ''
    }
  }

  function memberLabel(userId: number) {
    return userOptions.value.find((user) => user.id === userId)?.label ?? 'User'
  }

  async function loadUsers() {
      const users = await authApi.listEnabledUsersSafe()
      userOptions.value = users.map((user) => ({
        id: user.id,
        label: formatUserDisplayName(user),
      }))

      const firstUser = userOptions.value[0]
      if (!membershipForm.value.userId && firstUser) {
        membershipForm.value.userId = String(firstUser.id)
      }
  }

  async function addMemberRole() {
    const value = newMemberRoleInput.value.trim().toUpperCase()
    if (!value) return

    newMemberRoleInput.value = ''
    const response = await orgApi.addCatalogValue('MEMBERSHIP_ROLE', value)
    setMemberRoles(response.data)
    membershipForm.value.membershipRole = value
  }

  async function refreshMemberships() {
    if (!nodeId.value) return
    try {
      memberships.value = (await orgApi.getNodeMemberships(nodeId.value)).data

      const knownUserIds = new Set(userOptions.value.map((user) => user.id))
      const missingUserIds = Array.from(new Set(memberships.value.map((membership) => membership.userId))).filter(
        (id) => !knownUserIds.has(id),
      )

      if (missingUserIds.length > 0) {
        const missingUsers = await Promise.all(
          missingUserIds.map(async (id) => {
            try {
              const { data } = await authApi.getUser(id)
              return { id, label: formatUserDisplayName(data) }
            } catch {
              return null
            }
          }),
        )

        userOptions.value = [...userOptions.value, ...missingUsers.filter((entry): entry is UserOption => entry !== null)]
      }
    } catch {
      memberships.value = []
    }
  }

  async function addMembership() {
    if (!nodeId.value || !membershipForm.value.userId || !membershipForm.value.membershipRole) return

    const userId = Number(membershipForm.value.userId)
    if (!userId) return

    membershipSaving.value = true
    try {
      await orgApi.createMembership(nodeId.value, {
        userId,
        membershipRole: membershipForm.value.membershipRole,
      })
      await refreshMemberships()
    } finally {
      membershipSaving.value = false
    }
  }

  async function removeMembership(membershipId: number) {
    await orgApi.deleteMembership(membershipId)
    await refreshMemberships()
  }

  return {
    memberships,
    membershipForm,
    membershipSaving,
    userOptions,
    memberRoles,
    newMemberRoleInput,
    setMemberRoles,
    memberLabel,
    loadUsers,
    addMemberRole,
    refreshMemberships,
    addMembership,
    removeMembership,
  }
}
