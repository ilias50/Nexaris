<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppLayout from '@/components/layout/AppLayout.vue'
import CatalogChipManagerCard from '@/components/admin/CatalogChipManagerCard.vue'
import { useI18n } from '@/i18n'
import { useAdminOrgCatalogsApiAccess, type CatalogEntry } from '@/composables/useAdminOrgCatalogsApiAccess'
import { normalizeRoleCode } from '@/utils/roles'
import { toUpperTrimmed } from '@/utils/validation'

const { t } = useI18n()
const adminOrgCatalogsApi = useAdminOrgCatalogsApiAccess()

const roles = ref<CatalogEntry[]>([])
const membershipRoles = ref<CatalogEntry[]>([])
const nodeTypes = ref<CatalogEntry[]>([])
const severities = ref<CatalogEntry[]>([])
const linkCategories = ref<CatalogEntry[]>([])
const roleInput = ref('')
const membershipRoleInput = ref('')
const typeInput = ref('')
const severityInput = ref('')
const linkCategoryInput = ref('')
const loading = ref(false)

function normalizeUpper(raw: string) {
  return toUpperTrimmed(raw)
}

async function loadAll() {
  loading.value = true
  try {
    const workspace = await adminOrgCatalogsApi.getWorkspace(true)
    roles.value = workspace.accessRuleRoles
    membershipRoles.value = workspace.membershipRoles
    nodeTypes.value = workspace.nodeTypes
    severities.value = workspace.announcementSeverities
    linkCategories.value = workspace.linkCategories
  } finally {
    loading.value = false
  }
}

async function setColor(list: CatalogEntry[], catalogType: string, value: string, color: string) {
  try {
    const res = await adminOrgCatalogsApi.updateCatalogColor(catalogType, value, color || null)
    const idx = list.findIndex(e => e.value === value)
    if (idx !== -1) list[idx] = res.data
  } catch { /* ignore */ }
}

function updateCatalogColor(catalogType: string, list: CatalogEntry[], payload: { value: string; color: string }) {
  return setColor(list, catalogType, payload.value, payload.color)
}

async function addCatalogValue(
  catalogType: string,
  listRef: { value: CatalogEntry[] },
  inputRef: { value: string },
  normalize: (raw: string) => string,
) {
  const next = normalize(inputRef.value)
  if (!next) return
  inputRef.value = ''
  await adminOrgCatalogsApi.addCatalogValue(catalogType, next)
  adminOrgCatalogsApi.clearCatalogMetaCache(catalogType)
  listRef.value = await adminOrgCatalogsApi.getCatalogMetaCached(catalogType, true)
}

async function removeCatalogValue(catalogType: string, listRef: { value: CatalogEntry[] }, value: string) {
  await adminOrgCatalogsApi.removeCatalogValue(catalogType, value)
  listRef.value = listRef.value.filter((entry) => entry.value !== value)
}

async function addRole() {
  await addCatalogValue('ACCESS_RULE_ROLE', roles, roleInput, normalizeRoleCode)
}

async function removeRole(role: string) {
  await removeCatalogValue('ACCESS_RULE_ROLE', roles, role)
}

async function addMembershipRole() {
  await addCatalogValue('MEMBERSHIP_ROLE', membershipRoles, membershipRoleInput, normalizeRoleCode)
}

async function removeMembershipRole(role: string) {
  await removeCatalogValue('MEMBERSHIP_ROLE', membershipRoles, role)
}

async function addType() {
  await addCatalogValue('NODE_TYPE', nodeTypes, typeInput, normalizeUpper)
}

async function removeType(type: string) {
  await removeCatalogValue('NODE_TYPE', nodeTypes, type)
}

async function addSeverity() {
  await addCatalogValue('ANNOUNCEMENT_SEVERITY', severities, severityInput, normalizeUpper)
}

async function removeSeverity(val: string) {
  await removeCatalogValue('ANNOUNCEMENT_SEVERITY', severities, val)
}

async function addLinkCategory() {
  await addCatalogValue('LINK_CATEGORY', linkCategories, linkCategoryInput, normalizeUpper)
}

async function removeLinkCategory(val: string) {
  await removeCatalogValue('LINK_CATEGORY', linkCategories, val)
}

onMounted(loadAll)
</script>

<template>
  <AppLayout :title="t('adminOrgCatalogs.title')">
    <div class="aoc">
      <div class="aoc__header">
        <h3>{{ t('adminOrgCatalogs.title') }}</h3>
        <p>{{ t('adminOrgCatalogs.subtitle') }}</p>
      </div>

      <p v-if="loading" class="aoc__hint">{{ t('adminOrgCatalogs.loading') }}</p>

      <div v-else class="aoc__grid">
        <CatalogChipManagerCard
          :title="t('adminOrgCatalogs.rolesTitle')"
          :hint="t('adminOrgCatalogs.rolesHint')"
          :input-label="t('adminOrgCatalogs.newRoleLabel')"
          :input-placeholder="t('adminOrgCatalogs.rolePlaceholder')"
          :input-value="roleInput"
          :entries="roles"
          :display-as-role-label="true"
          @update:input-value="roleInput = $event"
          @add="addRole"
          @remove="removeRole"
          @color-change="updateCatalogColor('ACCESS_RULE_ROLE', roles, $event)"
        />

        <CatalogChipManagerCard
          :title="t('adminOrgCatalogs.membershipRolesTitle')"
          :hint="t('adminOrgCatalogs.membershipRolesHint')"
          :input-label="t('adminOrgCatalogs.newMembershipRoleLabel')"
          :input-placeholder="t('adminOrgCatalogs.membershipRolePlaceholder')"
          :input-value="membershipRoleInput"
          :entries="membershipRoles"
          :display-as-role-label="true"
          manage-link-to="/admin/org-membership-role-permissions"
          :manage-link-label="t('adminOrgCatalogs.membershipRolesManagePermissions')"
          @update:input-value="membershipRoleInput = $event"
          @add="addMembershipRole"
          @remove="removeMembershipRole"
          @color-change="updateCatalogColor('MEMBERSHIP_ROLE', membershipRoles, $event)"
        />

        <CatalogChipManagerCard
          :title="t('adminOrgCatalogs.typesTitle')"
          :hint="t('adminOrgCatalogs.typesHint')"
          :input-label="t('adminOrgCatalogs.newTypeLabel')"
          :input-placeholder="t('adminOrgCatalogs.typePlaceholder')"
          :input-value="typeInput"
          :entries="nodeTypes"
          @update:input-value="typeInput = $event"
          @add="addType"
          @remove="removeType"
          @color-change="updateCatalogColor('NODE_TYPE', nodeTypes, $event)"
        />

        <CatalogChipManagerCard
          :title="t('adminOrgCatalogs.severitiesTitle')"
          :hint="t('adminOrgCatalogs.severitiesHint')"
          :input-label="t('adminOrgCatalogs.newSeverityLabel')"
          :input-placeholder="t('adminOrgCatalogs.severityPlaceholder')"
          :input-value="severityInput"
          :entries="severities"
          @update:input-value="severityInput = $event"
          @add="addSeverity"
          @remove="removeSeverity"
          @color-change="updateCatalogColor('ANNOUNCEMENT_SEVERITY', severities, $event)"
        />

        <CatalogChipManagerCard
          :title="t('adminOrgCatalogs.linkCategoriesTitle')"
          :hint="t('adminOrgCatalogs.linkCategoriesHint')"
          :input-label="t('adminOrgCatalogs.newLinkCategoryLabel')"
          :input-placeholder="t('adminOrgCatalogs.linkCategoryPlaceholder')"
          :input-value="linkCategoryInput"
          :entries="linkCategories"
          @update:input-value="linkCategoryInput = $event"
          @add="addLinkCategory"
          @remove="removeLinkCategory"
          @color-change="updateCatalogColor('LINK_CATEGORY', linkCategories, $event)"
        />
      </div>
    </div>
  </AppLayout>
</template>

<style scoped>
.aoc {
  max-width: 1000px;
}
.aoc__header {
  margin-bottom: 1rem;
}
.aoc__header h3 {
  margin: 0 0 0.2rem;
}
.aoc__header p {
  margin: 0;
  color: var(--color-text-muted);
}
.aoc__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}
.aoc__hint {
  margin: 0 0 0.75rem;
  color: var(--color-text-muted);
  font-size: 0.88rem;
}
@media (max-width: 920px) {
  .aoc__grid {
    grid-template-columns: 1fr;
  }
}
</style>
