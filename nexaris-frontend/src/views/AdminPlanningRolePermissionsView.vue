<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseButton from '@/components/BaseButton.vue'
import { useI18n } from '@/i18n'
import { useAdminPlanningRolePermissionsApiAccess } from '@/composables/useAdminPlanningRolePermissionsApiAccess'
import { formatRoleLabel, normalizeRoleCode } from '@/utils/roles'
import { formatPermissionLabel } from '@/utils/permissions'

const { t } = useI18n()
const planningRoleApi = useAdminPlanningRolePermissionsApiAccess()

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
      planningRoleApi.getPlanningRoles(),
      planningRoleApi.getAvailablePermissions(),
    ])

    roleOptions.value = rolesRes.data.map((role) => role.roleName)
    availablePermissions.value = permissionsRes.data

    if (!selectedRole.value || !roleOptions.value.includes(selectedRole.value)) {
      selectedRole.value = roleOptions.value[0] ?? ''
    }
  } catch {
    loadError.value = t('adminPlanningRolePermissions.errors.load')
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
    const res = await planningRoleApi.getPlanningRoles()
    const selected = res.data.find((role) => role.roleName === selectedRole.value)
    selectedPermissions.value = selected?.permissions ?? []
  } catch {
    saveError.value = t('adminPlanningRolePermissions.errors.loadRole')
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
    await planningRoleApi.addPlanningRole(next)
    await loadBaseData()
    selectedRole.value = next
    selectedPermissions.value = []
    newRoleInput.value = ''
    saveSuccess.value = t('adminPlanningRolePermissions.messages.roleCreated')
  } catch {
    saveError.value = t('adminPlanningRolePermissions.errors.createRole')
  }
}

async function savePermissions() {
  if (!selectedRole.value) return

  saving.value = true
  saveError.value = ''
  saveSuccess.value = ''

  try {
    const res = await planningRoleApi.replaceRolePermissions(selectedRole.value, selectedPermissions.value)
    selectedPermissions.value = res.data.permissions
    saveSuccess.value = t('adminPlanningRolePermissions.messages.saved')
  } catch {
    saveError.value = t('adminPlanningRolePermissions.errors.save')
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
  <AppLayout :title="t('adminPlanningRolePermissions.title')">
    <div class="aprp">
      <div class="aprp__header">
        <h3>{{ t('adminPlanningRolePermissions.title') }}</h3>
        <p>{{ t('adminPlanningRolePermissions.subtitle') }}</p>
      </div>

      <p v-if="loading" class="aprp__hint">{{ t('adminPlanningRolePermissions.loading') }}</p>
      <p v-else-if="loadError" class="aprp__error">{{ loadError }}</p>

      <template v-else>
        <section class="aprp__card">
          <label class="aprp__label" for="planning-role-select">{{ t('adminPlanningRolePermissions.roleLabel') }}</label>
          <select id="planning-role-select" v-model="selectedRole" class="aprp__select">
            <option v-for="role in roleOptions" :key="role" :value="role">{{ formatRoleLabel(role) }}</option>
          </select>
          <p class="aprp__hint aprp__hint--top">{{ t('adminPlanningRolePermissions.createRoleHint') }}</p>
          <div class="aprp__create-row">
            <input
              v-model="newRoleInput"
              class="aprp__input"
              type="text"
              :placeholder="t('adminPlanningRolePermissions.newRoleLabel')"
              @keyup.enter="addRole"
            />
            <BaseButton type="button" @click="addRole">
              {{ t('adminPlanningRolePermissions.addRole') }}
            </BaseButton>
          </div>
        </section>

        <section class="aprp__card">
          <h4>{{ t('adminPlanningRolePermissions.permissionsTitle') }}</h4>
          <p class="aprp__hint">{{ t('adminPlanningRolePermissions.permissionsHint') }}</p>

          <div class="aprp__permissions">
            <label v-for="permission in availablePermissions" :key="permission" class="aprp__permission-item">
              <input v-model="selectedPermissions" type="checkbox" :value="permission" :disabled="!hasSelection" />
              <span>{{ permissionLabel(permission) }}</span>
            </label>
          </div>

          <p v-if="saveError" class="aprp__error">{{ saveError }}</p>
          <p v-if="saveSuccess" class="aprp__success">{{ saveSuccess }}</p>

          <div class="aprp__actions">
            <BaseButton type="button" :loading="saving" :disabled="!canSave" @click="savePermissions">
              {{ t('adminPlanningRolePermissions.save') }}
            </BaseButton>
          </div>
        </section>
      </template>
    </div>
  </AppLayout>
</template>

<style scoped>
.aprp {
  max-width: 860px;
}
.aprp__header {
  margin-bottom: 1rem;
}
.aprp__header h3 {
  margin: 0 0 0.2rem;
}
.aprp__header p {
  margin: 0;
  color: var(--color-text-muted);
}
.aprp__card {
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  padding: 1rem;
  margin-bottom: 1rem;
}
.aprp__label {
  display: block;
  margin-bottom: 0.4rem;
  font-weight: 600;
}
.aprp__select {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.6rem;
  background: var(--color-surface);
  color: var(--color-text);
}
.aprp__hint {
  margin: 0 0 0.75rem;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}
.aprp__hint--top {
  margin-top: 0.65rem;
}
.aprp__create-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 0.6rem;
  align-items: end;
}
.aprp__input {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.6rem;
  background: var(--color-surface);
  color: var(--color-text);
}
.aprp__permissions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.5rem;
}
.aprp__permission-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.45rem 0.55rem;
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: var(--radius-sm);
  background: rgba(148, 163, 184, 0.08);
}
.aprp__actions {
  margin-top: 1rem;
  display: flex;
  justify-content: flex-end;
}
.aprp__error {
  color: #dc2626;
}
.aprp__success {
  color: #0f766e;
}

@media (max-width: 900px) {
  .aprp {
    max-width: 100%;
  }

  .aprp__card {
    padding: 0.85rem;
  }

  .aprp__create-row {
    grid-template-columns: 1fr;
  }

  .aprp__actions {
    justify-content: stretch;
  }

  .aprp__actions :deep(button) {
    width: 100%;
  }
}

@media (max-width: 520px) {
  .aprp__permissions {
    grid-template-columns: 1fr;
  }
}
</style>
