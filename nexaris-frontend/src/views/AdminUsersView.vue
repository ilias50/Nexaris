<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseButton from '@/components/BaseButton.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import AdminRegistrationSettingsCard from '@/components/admin/AdminRegistrationSettingsCard.vue'
import AdminCreateUserCard from '@/components/admin/AdminCreateUserCard.vue'
import AdminUserSelector from '@/components/admin/AdminUserSelector.vue'
import UserRolesEditor from '@/components/admin/UserRolesEditor.vue'
import AdminUserProfileSummary from '@/components/admin/AdminUserProfileSummary.vue'
import AdminUserProfileActions from '@/components/admin/AdminUserProfileActions.vue'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from '@/i18n'
import { isDefaultUserRole } from '@/utils/roles'
import { resolveProfileImageUrl } from '@/utils/profileImage'
import { useUserRoleManagement } from '@/composables/useUserRoleManagement'
import { useAdminRegistrationSettings } from '@/composables/useAdminRegistrationSettings'
import { useAdminUserCreation } from '@/composables/useAdminUserCreation'
import { useAdminUserLifecycleActions } from '@/composables/useAdminUserLifecycleActions'
import { useAdminUserDirectory, type AdminUser, type OrgUserRole } from '@/composables/useAdminUserDirectory'
import { getApiBaseUrl } from '@/config/runtime'

const { t } = useI18n()
const auth = useAuthStore()
const apiBaseUrl = getApiBaseUrl()

const {
  registrationEnabled,
  loadingRegistration,
  savingRegistration,
  registrationMessage,
  registrationError,
  loadRegistrationStatus,
  saveRegistrationStatus,
} = useAdminRegistrationSettings(t)

const {
  firstName,
  lastName,
  email,
  password,
  confirmPassword,
  creatingUser,
  createUserMessage,
  createUserError,
  handleCreateUser,
} = useAdminUserCreation(t)

const selectedUser = ref<AdminUser | null>(null)
const userOrgRoles = ref<OrgUserRole[]>([])

const {
  usersVisible,
  users,
  loadingUsers,
  usersMessage,
  usersError,
  loadingUserProfile,
  userProfileError,
  loadAllUsers,
  showUsersList,
  selectUser: selectDirectoryUser,
} = useAdminUserDirectory(selectedUser, userOrgRoles, t)

const roleManagement = useUserRoleManagement(selectedUser, selectUser, t, userOrgRoles)

const {
  authRoleToAdd,
  authRoleOptions,
  orgCatalogRoleToAdd,
  orgCatalogRoleOptions,
  planningRoleToAdd,
  planningRoleOptions,
  selectedUserRoles,
  userPlanningRoles,
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
} = roleManagement

function isProtectedAuthRole(roleName: string) {
  return isDefaultUserRole(roleName)
}

function isProtectedOrgRole(roleName: string) {
  return isDefaultUserRole(roleName)
}

function updateAuthRoleToAdd(roleName: string) {
  authRoleToAdd.value = roleName
}

function updateOrgRoleToAdd(roleName: string) {
  orgCatalogRoleToAdd.value = roleName
}

function updatePlanningRoleToAdd(roleName: string) {
  planningRoleToAdd.value = roleName
}

const selectedUserProfileImageSrc = computed(() => {
  return resolveProfileImageUrl(selectedUser.value?.profileImageUrl, apiBaseUrl)
})

function clearRoleFeedback() {
  roleActionError.value = ''
  roleActionMessage.value = ''
}

const {
  resettingPassword,
  resetPasswordMessage,
  resetPasswordError,
  deletingUser,
  deleteUserMessage,
  deleteUserError,
  showDeleteConfirm,
  isDeletingSelf,
  deleteConfirmDetails,
  clearUserActionMessages,
  handleResetPassword,
  askDeleteUserConfirmation,
  cancelDeleteConfirmation,
  confirmDeleteUser,
} = useAdminUserLifecycleActions(
  selectedUser,
  loadAllUsers,
  () => auth.user?.id,
  t,
)

async function selectUser(userId: number) {
  clearRoleFeedback()
  clearUserActionMessages()
  await selectDirectoryUser(userId)
  await loadUserPlanningRoles()
}

onMounted(async () => {
  await Promise.all([loadRegistrationStatus(), loadOrgRoleCatalog(), loadPlanningRoleCatalog()])
})
</script>

<template>
  <AppLayout :title="t('adminUsers.title')">
    <div class="admin-users">
      <AdminRegistrationSettingsCard
        :enabled="registrationEnabled"
        :loading="loadingRegistration"
        :saving="savingRegistration"
        :message="registrationMessage"
        :error="registrationError"
        @update:enabled="registrationEnabled = $event"
        @save="saveRegistrationStatus"
      />

      <AdminCreateUserCard
        :first-name="firstName"
        :last-name="lastName"
        :email="email"
        :password="password"
        :confirm-password="confirmPassword"
        :loading="creatingUser"
        :message="createUserMessage"
        :error="createUserError"
        @update:first-name="firstName = $event"
        @update:last-name="lastName = $event"
        @update:email="email = $event"
        @update:password="password = $event"
        @update:confirm-password="confirmPassword = $event"
        @submit="handleCreateUser"
      />

      <section class="nx-admin-card">
        <h2 class="nx-admin-title">{{ t('adminUsers.management.title') }}</h2>
        <p class="nx-admin-subtitle">{{ t('adminUsers.management.description') }}</p>

        <div class="user-admin-actions">
          <BaseButton variant="secondary" :loading="loadingUsers" @click="showUsersList">
            {{ t('adminUsers.management.showListButton') }}
          </BaseButton>
          <BaseButton variant="ghost" :loading="loadingUsers" @click="loadAllUsers">
            {{ t('adminUsers.management.refreshButton') }}
          </BaseButton>
        </div>

        <p v-if="usersMessage" class="nx-admin-success">{{ usersMessage }}</p>
        <p v-if="usersError" class="nx-admin-error">{{ usersError }}</p>

        <div v-if="loadingUsers" class="nx-admin-info">{{ t('adminUsers.management.loadingUsers') }}</div>

        <div v-else-if="usersVisible" class="user-admin-grid">
          <div>
            <AdminUserSelector
              :title="t('adminUsers.management.listTitle')"
              :users="users"
              :selected-user-id="selectedUser?.id ?? null"
              :loading-users="false"
              :loading-text="t('adminUsers.management.loadingUsers')"
              :empty-text="t('adminUsers.management.emptyList')"
              :show-refresh="false"
              @select-user="selectUser"
            />
          </div>

          <div class="user-profile">
            <h3 class="nx-admin-subtitle nx-admin-subtitle-sm">{{ t('adminUsers.management.profileTitle') }}</h3>

            <div v-if="loadingUserProfile" class="nx-admin-info">{{ t('adminUsers.management.loadingProfile') }}</div>
            <p v-else-if="userProfileError" class="nx-admin-error">{{ userProfileError }}</p>

            <div v-else-if="selectedUser" class="profile-card">
              <AdminUserProfileSummary
                :user="selectedUser"
                :auth-roles="selectedUserRoles"
                :profile-image-src="selectedUserProfileImageSrc"
              />

              <div class="roles-section">
                <h4 class="nx-admin-subtitle nx-admin-subtitle-sm">{{ t('adminUserRoles.title') }}</h4>

                <UserRolesEditor
                  :auth-roles="selectedUserRoles"
                  :org-roles="userOrgRoles"
                  :planning-roles="userPlanningRoles"
                  :auth-role-to-add="authRoleToAdd"
                  :auth-role-options="authRoleOptions"
                  :org-role-to-add="orgCatalogRoleToAdd"
                  :org-role-options="orgCatalogRoleOptions"
                  :planning-role-to-add="planningRoleToAdd"
                  :planning-role-options="planningRoleOptions"
                  :saving-role="savingRole"
                  :action-message="roleActionMessage"
                  :action-error="roleActionError"
                  :disable-auth-role-removal="isProtectedAuthRole"
                  :disable-org-role-removal="isProtectedOrgRole"
                  @update:auth-role-to-add="updateAuthRoleToAdd"
                  @update:org-role-to-add="updateOrgRoleToAdd"
                  @update:planning-role-to-add="updatePlanningRoleToAdd"
                  @add-auth-role="addAuthRole"
                  @remove-auth-role="removeAuthRole"
                  @add-org-role="addOrgCatalogRole"
                  @remove-org-role="removeOrgCatalogRole"
                  @add-planning-role="addPlanningRole"
                  @remove-planning-role="removePlanningRole"
                />
              </div>

              <div class="profile-actions">
                <AdminUserProfileActions
                  :key="selectedUser.id"
                  :resetting-password="resettingPassword"
                  :deleting-user="deletingUser"
                  :is-deleting-self="isDeletingSelf"
                  :reset-password-message="resetPasswordMessage"
                  :reset-password-error="resetPasswordError"
                  :delete-user-message="deleteUserMessage"
                  :delete-user-error="deleteUserError"
                  @reset-password="handleResetPassword"
                  @delete-user="askDeleteUserConfirmation"
                />
              </div>
            </div>

            <p v-else class="nx-admin-info">{{ t('adminUsers.management.selectUserHint') }}</p>
          </div>
        </div>
      </section>

      <ConfirmDialog
        v-model="showDeleteConfirm"
        :title="t('adminUsers.management.deleteUserButton')"
        :message="t('adminUsers.management.confirmDelete')"
        :details="deleteConfirmDetails"
        :confirm-text="t('adminUsers.management.deleteUserButton')"
        :cancel-text="t('adminUsers.management.cancel')"
        confirm-variant="danger"
        :loading="deletingUser"
        @cancel="cancelDeleteConfirmation"
        @confirm="confirmDeleteUser"
      />
    </div>
  </AppLayout>
</template>

<style scoped>
.admin-users {
  display: grid;
  gap: 1.25rem;
}

.user-admin-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  margin-bottom: 0.9rem;
}

.user-admin-grid {
  display: grid;
  grid-template-columns: minmax(230px, 320px) minmax(0, 1fr);
  gap: 1rem;
}

.user-profile {
  min-height: 220px;
}

.profile-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.8rem;
}

.profile-actions {
  width: 100%;
}

.roles-section,
.profile-actions {
  width: 100%;
}

.roles-section {
  border-top: 1px solid var(--color-border);
  margin-top: 0.5rem;
  padding-top: 0.75rem;
}

@media (max-width: 768px) {
  .user-admin-grid {
    grid-template-columns: 1fr;
  }

  .profile-card { padding: 0.75rem; }
}
</style>
