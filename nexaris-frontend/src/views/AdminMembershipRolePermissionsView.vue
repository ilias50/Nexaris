<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseButton from '@/components/BaseButton.vue'
import { useI18n } from '@/i18n'
import { useAdminMembershipRolePermissionsApiAccess } from '@/composables/useAdminMembershipRolePermissionsApiAccess'
import { formatRoleLabel, normalizeRoleCode } from '@/utils/roles'
import { formatPermissionLabel } from '@/utils/permissions'

const { t } = useI18n()
const membershipRoleApi = useAdminMembershipRolePermissionsApiAccess()

const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const saveError = ref('')
const saveSuccess = ref('')

const roleOptions = ref<string[]>([])
const selectedRole = ref('')
const newRoleInput = ref('')
const availablePermissions = ref<string[]>([])
const selectedPermissions = ref<string[]>([])

const hasSelection = computed(() => !!selectedRole.value)
const canSave = computed(() => hasSelection.value && !saving.value && availablePermissions.value.length > 0)

function permissionLabel(permission: string) {
  return formatPermissionLabel(permission, t)
}

async function loadBaseData() {
  loading.value = true
  loadError.value = ''
  try {
    const [rolesRes, permissionsRes] = await Promise.all([
      membershipRoleApi.getMembershipRoles(),
      membershipRoleApi.getAvailablePermissions(),
    ])
    roleOptions.value = rolesRes.data
    availablePermissions.value = permissionsRes.data

    if (!selectedRole.value || !roleOptions.value.includes(selectedRole.value)) {
      selectedRole.value = roleOptions.value[0] ?? ''
    }
  } catch {
    loadError.value = t('adminMembershipRolePermissions.errors.load')
  } finally {
    loading.value = false
  }
}

async function loadRolePermissions() {
  saveSuccess.value = ''
  saveError.value = ''
  selectedPermissions.value = []

  if (!selectedRole.value) {
    return
  }

  try {
    const res = await membershipRoleApi.getRolePermissions(selectedRole.value)
    selectedPermissions.value = res.data
  } catch {
    saveError.value = t('adminMembershipRolePermissions.errors.loadRole')
  }
}

watch(selectedRole, () => {
  loadRolePermissions()
})

async function addRole() {
  const next = normalizeRoleCode(newRoleInput.value)
  if (!next) return

  saveError.value = ''
  saveSuccess.value = ''

  try {
    const res = await membershipRoleApi.addMembershipRole(next)
    roleOptions.value = res.data
    selectedRole.value = next
    selectedPermissions.value = []
    newRoleInput.value = ''
    saveSuccess.value = t('adminMembershipRolePermissions.messages.roleCreated')
  } catch {
    saveError.value = t('adminMembershipRolePermissions.errors.createRole')
  }
}

async function savePermissions() {
  if (!selectedRole.value) return
  saving.value = true
  saveError.value = ''
  saveSuccess.value = ''
  try {
    const res = await membershipRoleApi.replaceRolePermissions(selectedRole.value, selectedPermissions.value)
    selectedPermissions.value = res.data
    saveSuccess.value = t('adminMembershipRolePermissions.messages.saved')
  } catch {
    saveError.value = t('adminMembershipRolePermissions.errors.save')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadBaseData()
  await loadRolePermissions()
})
</script>

<template>
  <AppLayout :title="t('adminMembershipRolePermissions.title')">
    <div class="amrp">
      <div class="amrp__header">
        <h3>{{ t('adminMembershipRolePermissions.title') }}</h3>
        <p>{{ t('adminMembershipRolePermissions.subtitle') }}</p>
      </div>

      <p v-if="loading" class="amrp__hint">{{ t('adminMembershipRolePermissions.loading') }}</p>
      <p v-else-if="loadError" class="amrp__error">{{ loadError }}</p>

      <template v-else>
        <section class="amrp__card">
          <label class="amrp__label" for="membership-role-select">{{ t('adminMembershipRolePermissions.roleLabel') }}</label>
          <select id="membership-role-select" v-model="selectedRole" class="amrp__select">
            <option v-for="role in roleOptions" :key="role" :value="role">{{ formatRoleLabel(role) }}</option>
          </select>
          <p class="amrp__hint amrp__hint--top">{{ t('adminMembershipRolePermissions.createRoleHint') }}</p>
          <div class="amrp__create-row">
            <input
              v-model="newRoleInput"
              class="amrp__input"
              type="text"
              :placeholder="t('adminMembershipRolePermissions.newRoleLabel')"
              @keyup.enter="addRole"
            />
            <BaseButton type="button" @click="addRole">
              {{ t('adminMembershipRolePermissions.addRole') }}
            </BaseButton>
          </div>
        </section>

        <section class="amrp__card">
          <h4>{{ t('adminMembershipRolePermissions.permissionsTitle') }}</h4>
          <p class="amrp__hint">{{ t('adminMembershipRolePermissions.permissionsHint') }}</p>

          <div class="amrp__permissions">
            <label v-for="permission in availablePermissions" :key="permission" class="amrp__permission-item">
              <input v-model="selectedPermissions" type="checkbox" :value="permission" :disabled="!hasSelection" />
              <span>{{ permissionLabel(permission) }}</span>
            </label>
          </div>

          <p v-if="saveError" class="amrp__error">{{ saveError }}</p>
          <p v-if="saveSuccess" class="amrp__success">{{ saveSuccess }}</p>

          <div class="amrp__actions">
            <BaseButton type="button" :loading="saving" :disabled="!canSave" @click="savePermissions">
              {{ t('adminMembershipRolePermissions.save') }}
            </BaseButton>
          </div>
        </section>
      </template>
    </div>
  </AppLayout>
</template>

<style scoped>
.amrp {
  max-width: 860px;
}
.amrp__header {
  margin-bottom: 1rem;
}
.amrp__header h3 {
  margin: 0 0 0.2rem;
}
.amrp__header p {
  margin: 0;
  color: var(--color-text-muted);
}
.amrp__card {
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  padding: 1rem;
  margin-bottom: 1rem;
}
.amrp__label {
  display: block;
  margin-bottom: 0.4rem;
  font-weight: 600;
}
.amrp__select {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.6rem;
  background: var(--color-surface);
  color: var(--color-text);
}
.amrp__hint {
  margin: 0 0 0.75rem;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}
.amrp__hint--top {
  margin-top: 0.65rem;
}
.amrp__create-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 0.6rem;
  align-items: end;
}
.amrp__input {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.6rem;
  background: var(--color-surface);
  color: var(--color-text);
}
.amrp__permissions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.5rem;
}
.amrp__permission-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.45rem 0.55rem;
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: var(--radius-sm);
  background: rgba(148, 163, 184, 0.08);
}
.amrp__actions {
  margin-top: 1rem;
  display: flex;
  justify-content: flex-end;
}
.amrp__error {
  color: #dc2626;
}
.amrp__success {
  color: #0f766e;
}

@media (max-width: 900px) {
  .amrp {
    max-width: 100%;
  }

  .amrp__card {
    padding: 0.85rem;
  }

  .amrp__create-row {
    grid-template-columns: 1fr;
  }

  .amrp__actions {
    justify-content: stretch;
  }

  .amrp__actions :deep(button) {
    width: 100%;
  }
}

@media (max-width: 520px) {
  .amrp__permissions {
    grid-template-columns: 1fr;
  }

  .amrp__permission-item {
    align-items: flex-start;
  }
}
</style>
