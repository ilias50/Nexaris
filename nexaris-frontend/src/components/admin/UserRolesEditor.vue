<script setup lang="ts">
import BaseButton from '@/components/BaseButton.vue'
import type { OrgUserRole } from '@/types/domain'
import { useI18n } from '@/i18n'
import { formatRoleLabel } from '@/utils/roles'

const { t } = useI18n()

const props = defineProps<{
  authRoles: string[]
  orgRoles: OrgUserRole[]
  planningRoles: string[]
  authRoleToAdd: string
  authRoleOptions: string[]
  orgRoleToAdd: string
  orgRoleOptions: string[]
  planningRoleToAdd: string
  planningRoleOptions: string[]
  savingRole: boolean
  actionMessage?: string
  actionError?: string
  disableAuthRoleRemoval?: (roleName: string) => boolean
  disableOrgRoleRemoval?: (roleName: string) => boolean
}>()

const emit = defineEmits<{
  (event: 'update:authRoleToAdd', value: string): void
  (event: 'update:orgRoleToAdd', value: string): void
  (event: 'update:planningRoleToAdd', value: string): void
  (event: 'add-auth-role'): void
  (event: 'remove-auth-role', roleName: string): void
  (event: 'add-org-role'): void
  (event: 'remove-org-role', roleName: string): void
  (event: 'add-planning-role'): void
  (event: 'remove-planning-role', roleName: string): void
}>()

function updateAuthRoleToAdd(event: Event) {
  emit('update:authRoleToAdd', (event.target as HTMLSelectElement).value)
}

function updateOrgRoleToAdd(event: Event) {
  emit('update:orgRoleToAdd', (event.target as HTMLSelectElement).value)
}

function updatePlanningRoleToAdd(event: Event) {
  emit('update:planningRoleToAdd', (event.target as HTMLSelectElement).value)
}

function canRemoveAuthRole(roleName: string) {
  return props.disableAuthRoleRemoval ? !props.disableAuthRoleRemoval(roleName) : true
}

function canRemoveOrgRole(roleName: string) {
  return props.disableOrgRoleRemoval ? !props.disableOrgRoleRemoval(roleName) : true
}
</script>

<template>
  <div class="ure">
    <label class="ure__label">{{ t('adminUserRoles.currentRoles') }} (auth)</label>
    <div class="ure__chips">
      <span v-if="authRoles.length === 0" class="ure__hint">{{ t('adminUserRoles.noRole') }}</span>
      <span v-for="role in authRoles" :key="role" class="ure__chip">
        {{ formatRoleLabel(role) }}
        <button
          class="ure__chip-remove"
          :disabled="savingRole || !canRemoveAuthRole(role)"
          @click="emit('remove-auth-role', role)"
        >×</button>
      </span>
    </div>

    <div class="ure__add-grid">
      <select class="ure__select" :value="authRoleToAdd" @change="updateAuthRoleToAdd">
        <option v-for="role in authRoleOptions" :key="role" :value="role">{{ formatRoleLabel(role) }}</option>
      </select>
      <BaseButton type="button" :loading="savingRole" @click="emit('add-auth-role')">{{ t('adminUserRoles.addRoleAction') }}</BaseButton>
    </div>

    <label class="ure__label">{{ t('adminUserRoles.orgRolesTitle') }}</label>
    <p class="ure__hint">{{ t('adminUserRoles.orgRolesHint') }}</p>
    <div class="ure__chips">
      <span v-if="orgRoles.length === 0" class="ure__hint">{{ t('adminUserRoles.noOrgRole') }}</span>
      <span v-for="role in orgRoles" :key="`${role.id}-${role.roleName}`" class="ure__chip">
        {{ formatRoleLabel(role.roleName) }}
        <button
          class="ure__chip-remove"
          :disabled="savingRole || !canRemoveOrgRole(role.roleName)"
          @click="emit('remove-org-role', role.roleName)"
        >×</button>
      </span>
    </div>

    <div class="ure__add-grid">
      <select class="ure__select" :value="orgRoleToAdd" @change="updateOrgRoleToAdd">
        <option v-for="role in orgRoleOptions" :key="role" :value="role">{{ formatRoleLabel(role) }}</option>
      </select>
      <BaseButton type="button" :loading="savingRole" @click="emit('add-org-role')">{{ t('adminUserRoles.addOrgRoleAction') }}</BaseButton>
    </div>

    <label class="ure__label">{{ t('adminUserRoles.planningRolesTitle') }}</label>
    <p class="ure__hint">{{ t('adminUserRoles.planningRolesHint') }}</p>
    <div class="ure__chips">
      <span v-if="planningRoles.length === 0" class="ure__hint">{{ t('adminUserRoles.noPlanningRole') }}</span>
      <span v-for="role in planningRoles" :key="role" class="ure__chip">
        {{ formatRoleLabel(role) }}
        <button
          class="ure__chip-remove"
          :disabled="savingRole"
          @click="emit('remove-planning-role', role)"
        >×</button>
      </span>
    </div>

    <div class="ure__add-grid">
      <select class="ure__select" :value="planningRoleToAdd" @change="updatePlanningRoleToAdd">
        <option v-for="role in planningRoleOptions" :key="role" :value="role">{{ formatRoleLabel(role) }}</option>
      </select>
      <BaseButton type="button" :loading="savingRole" @click="emit('add-planning-role')">{{ t('adminUserRoles.addPlanningRoleAction') }}</BaseButton>
    </div>

    <p v-if="actionMessage" class="ure__success">{{ actionMessage }}</p>
    <p v-if="actionError" class="ure__error">{{ actionError }}</p>
  </div>
</template>

<style scoped>
.ure {
  display: grid;
  gap: 0.25rem;
}

.ure__label {
  display: block;
  font-size: 0.84rem;
  font-weight: 600;
  margin-bottom: 0.25rem;
}

.ure__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-bottom: 0.65rem;
}

.ure__chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  border: 1px solid rgba(59, 130, 246, 0.35);
  background: rgba(59, 130, 246, 0.08);
  color: #1d4ed8;
  border-radius: 999px;
  padding: 0.2rem 0.5rem;
  font-size: 0.82rem;
}

.ure__chip-remove {
  border: none;
  background: none;
  color: inherit;
  cursor: pointer;
  font-size: 0.95rem;
  line-height: 1;
  padding: 0;
}

.ure__chip-remove:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.ure__add-grid {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 0.65rem;
  align-items: end;
  margin-bottom: 0.75rem;
}

.ure__select {
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.45);
  border-radius: var(--radius-sm);
  padding: 0.5rem 0.6rem;
  height: 2.35rem;
  font: inherit;
  background: var(--color-surface);
}

.ure__hint {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.ure__success {
  margin-top: 0.7rem;
  font-size: 0.85rem;
  color: #065f46;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: var(--radius-sm);
  padding: 0.45rem 0.6rem;
}

.ure__error {
  margin-top: 0.7rem;
  font-size: 0.85rem;
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: var(--radius-sm);
  padding: 0.45rem 0.6rem;
}
</style>