<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseInput from '@/components/BaseInput.vue'
import { useOrgNodeDetailApiAccess, type OrgNode } from '@/composables/useOrgNodeDetailApiAccess'
import { useAuthStore } from '@/stores/auth'
import { formatRoleLabel } from '@/utils/roles'
import { useI18n } from '@/i18n'
import type { TranslationKey } from '@/i18n/messages'
import { useOrgNodeContentManager } from '@/composables/useOrgNodeContentManager'
import { useOrgNodeMembershipManager } from '@/composables/useOrgNodeMembershipManager'
import { useOrgNodeAccessRuleManager } from '@/composables/useOrgNodeAccessRuleManager'
import { isBlank, trimOrEmpty } from '@/utils/validation'
import { formatEffectLabel, formatPermissionLabel } from '@/utils/permissions'
import type { NodeAccessRule } from '@/api/org'

const route = useRoute()
const auth = useAuthStore()
const { t } = useI18n()
const orgNodeDetailApi = useOrgNodeDetailApiAccess()

const nodeId = computed(() => Number(route.params.nodeId))

const loading = ref(false)
const loadError = ref('')

const node = ref<OrgNode | null>(null)
const permissions = ref<string[]>([])

function hasPermission(...codes: string[]) {
  return auth.isAdmin || codes.some((code) => permissions.value.includes(code))
}

const canEditContent = computed(() => hasPermission('EDIT_CONTENT'))
const canEditLinks = computed(() => hasPermission('EDIT_LINKS'))
const canManageMembers = computed(() => hasPermission('MANAGE_MEMBERS'))
const canManageAccess = computed(() => hasPermission('MANAGE_ACCESS'))
const canManageAnnouncements = computed(() => hasPermission('MANAGE_ANNOUNCEMENTS'))

function permissionLabel(permission: string) {
  return formatPermissionLabel(permission, t)
}

function effectLabel(effect: string) {
  return formatEffectLabel(effect, t)
}

function accessRuleSubjectValueLabel(rule: NodeAccessRule) {
  if (rule.subjectType !== 'USER') {
    return formatRoleLabel(rule.subjectValue)
  }

  const userId = Number(rule.subjectValue)
  if (!Number.isFinite(userId)) {
    return rule.subjectValue
  }

  return userOptions.value.find((user) => user.id === userId)?.label ?? t('orgNodeDetail.unknownUser')
}

const renaming = ref(false)
const renameValue = ref('')
const renameLoading = ref(false)
const renameError = ref('')

function openRename() {
  renameValue.value = node.value?.name ?? ''
  renameError.value = ''
  renaming.value = true
}

function cancelRename() {
  renaming.value = false
  renameError.value = ''
}

async function submitRename() {
  if (isBlank(renameValue.value) || !nodeId.value) return
  renameLoading.value = true
  renameError.value = ''
  try {
    const { data } = await orgNodeDetailApi.updateNode(nodeId.value, { name: trimOrEmpty(renameValue.value) })
    node.value = data
    renaming.value = false
  } catch {
    renameError.value = t('adminOrg.errors.renameNode')
  } finally {
    renameLoading.value = false
  }
}

const {
  contentForm,
  metadataEntries,
  contentSaving,
  links,
  linkForm,
  linkSaving,
  announcements,
  announcementForm,
  announcementSaving,
  categoryList,
  categoryColorMap,
  newCategoryInput,
  severityList,
  severityColorMap,
  newSeverityInput,
  SEVERITY_DESCRIPTIONS,
  SCOPE_DESCRIPTIONS,
  addMetadataEntry,
  removeMetadataEntry,
  categoryBadgeStyle,
  severityBadgeStyle,
  setWorkspaceData,
  saveContent,
  addLink,
  removeLink,
  addCategory,
  addSeverity,
  addAnnouncement,
  removeAnnouncement,
} = useOrgNodeContentManager(nodeId, () => auth.user?.id ?? null)

const {
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
} = useOrgNodeMembershipManager(nodeId)

const {
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
} = useOrgNodeAccessRuleManager(nodeId, userOptions, memberRoles)

async function loadNodeWorkspace() {
  if (!nodeId.value) return
  loading.value = true
  loadError.value = ''
  try {
    const workspace = await orgNodeDetailApi.getNodeDetailWorkspace(nodeId.value)

    node.value = workspace.node
    setWorkspaceData({
      content: workspace.content,
      links: workspace.links,
      announcements: workspace.announcements,
      categoriesMeta: workspace.catalogs.linkCategories,
      severitiesMeta: workspace.catalogs.severities,
    })

    setMemberRoles(workspace.catalogs.membershipRoles)
    if (memberRoles.value.length === 0) {
      throw new Error('Membership role catalog is missing.')
    }

    permissions.value = workspace.permissions

    await Promise.all([refreshMemberships(), refreshAccessRules()])
  } catch {
    loadError.value = t('adminOrg.errors.loadNodeDetails')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadAccessRuleRoleCatalog(), loadAccessRulePermissionCatalog()])
  if (accessRuleForm.value.subjectType === 'USER' && !accessRuleForm.value.subjectValue) {
    onRuleSubjectTypeChange()
  }
  await loadNodeWorkspace()
})

function resolveTextKey(key: string | undefined) {
  if (!key) return ''
  return t(key as TranslationKey)
}
</script>

<template>
  <AppLayout :title="node ? `${t('orgNodeDetail.titlePrefix')}: ${node.name}` : t('orgNodeRead.titleFallback')">
    <div class="ond">
      <header class="ond__header">
        <template v-if="renaming">
          <div class="ond__rename-row">
            <input
              v-model="renameValue"
              class="ond__rename-input"
              type="text"
              autofocus
              @keyup.enter="submitRename"
              @keyup.escape="cancelRename"
            />
            <button class="ond__rename-save" :disabled="renameLoading" :aria-label="t('orgNodeDetail.renameSaveAria')" @click="submitRename">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            </button>
            <button class="ond__rename-cancel" :aria-label="t('orgNodeDetail.renameCancelAria')" @click="cancelRename">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <p v-if="renameError" class="ond__error">{{ renameError }}</p>
        </template>
        <template v-else>
          <div class="ond__rename-row">
            <h3 class="ond__header-name">{{ node ? node.name : t('orgNodeRead.titleFallback') }}</h3>
            <button v-if="node && auth.isAdmin" class="ond__rename-btn" :title="t('orgNodeDetail.renameTitle')" :aria-label="t('orgNodeDetail.renameTitle')" @click="openRename">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            </button>
          </div>
        </template>
        <p v-if="node" class="ond__meta">{{ node.nodeType }} • {{ node.path }}</p>
      </header>

      <p v-if="loading" class="ond__hint">{{ t('orgNodeDetail.loading') }}</p>
      <p v-else-if="loadError" class="ond__error">{{ loadError }}</p>

      <template v-else>
        <section class="ond__card">
          <h4>{{ t('orgNodeDetail.permissionsDetected') }}</h4>
          <div class="ond__chips">
            <span v-if="permissions.length === 0" class="ond__hint">{{ t('orgNodeDetail.noExplicitPermissions') }}</span>
            <span v-for="perm in permissions" :key="perm" class="ond__chip">{{ permissionLabel(perm) }}</span>
          </div>
        </section>

        <section class="ond__card">
          <h4>{{ t('orgNodeDetail.membersTitle') }}</h4>
          <div class="ond__row-2">
            <div>
              <label class="ond__label">{{ t('orgNodeDetail.userLabel') }}</label>
              <select v-model="membershipForm.userId" class="ond__select">
                <option v-for="u in userOptions" :key="u.id" :value="String(u.id)">{{ u.label }}</option>
              </select>
            </div>
            <div>
              <label class="ond__label">{{ t('orgNodeDetail.roleLabel') }}</label>
              <select v-model="membershipForm.membershipRole" class="ond__select" :disabled="memberRoles.length === 0">
                <option value="" disabled>{{ t('orgNodeDetail.selectRole') }}</option>
                <option v-for="role in memberRoles" :key="role" :value="role">{{ role }}</option>
              </select>
              <div class="ond__add-custom">
                <input v-model="newMemberRoleInput" class="ond__meta-input" :placeholder="t('orgNodeDetail.newRolePlaceholder')" @keyup.enter="addMemberRole" />
                <button type="button" class="ond__add-btn" @click="addMemberRole">+ {{ t('orgNodeDetail.add') }}</button>
              </div>
            </div>
          </div>
          <div class="ond__actions">
            <BaseButton type="button" :loading="membershipSaving" :disabled="!canManageMembers || !membershipForm.userId || !membershipForm.membershipRole" @click="addMembership">{{ t('orgNodeDetail.addMember') }}</BaseButton>
          </div>
          <div class="ond__list">
            <div v-for="m in memberships" :key="m.id" class="ond__list-item">
              <div>
                <strong>{{ memberLabel(m.userId) }}</strong>
                <p class="ond__muted">{{ m.membershipRole }}</p>
              </div>
              <button class="ond__danger" :disabled="!canManageMembers" @click="removeMembership(m.id)">{{ t('orgNodeDetail.delete') }}</button>
            </div>
          </div>
        </section>

        <section class="ond__card">
          <h4>{{ t('orgNodeDetail.accessRulesTitle') }}</h4>
          <div class="ond__row-4">
            <div>
              <label class="ond__label">{{ t('orgNodeDetail.effectLabel') }}</label>
              <select v-model="accessRuleForm.effect" class="ond__select">
                <option v-for="effect in RULE_EFFECTS" :key="effect" :value="effect">{{ effectLabel(effect) }}</option>
              </select>
            </div>
            <div>
              <label class="ond__label">{{ t('orgNodeDetail.subjectLabel') }}</label>
              <select v-model="accessRuleForm.subjectType" class="ond__select" @change="onRuleSubjectTypeChange">
                <option v-for="subject in RULE_SUBJECT_TYPES" :key="subject" :value="subject">{{ formatRoleLabel(subject) }}</option>
              </select>
            </div>
            <div>
              <label class="ond__label">{{ t('orgNodeDetail.permissionLabel') }}</label>
              <select v-model="accessRuleForm.permission" class="ond__select">
                <option v-for="perm in accessRulePermissionOptions" :key="perm" :value="perm">{{ permissionLabel(perm) }}</option>
              </select>
            </div>
            <div>
              <label class="ond__label">{{ t('orgNodeDetail.valueLabel') }}</label>
              <select v-model="accessRuleForm.subjectValue" class="ond__select">
                <option v-for="opt in subjectValueOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
              <div v-if="accessRuleForm.subjectType === 'ROLE'" class="ond__add-custom">
                <input v-model="newAccessRoleInput" class="ond__meta-input" :placeholder="t('orgNodeDetail.newRolePlaceholder')" @keyup.enter="addAccessRuleRole" />
                <button type="button" class="ond__add-btn" @click="addAccessRuleRole">+ {{ t('orgNodeDetail.add') }}</button>
              </div>
              <div v-if="accessRuleForm.subjectType === 'MEMBERSHIP'" class="ond__add-custom">
                <input v-model="newMemberRoleInput" class="ond__meta-input" :placeholder="t('orgNodeDetail.newMembershipRolePlaceholder')" @keyup.enter="addMemberRole" />
                <button type="button" class="ond__add-btn" @click="addMemberRole">+ {{ t('orgNodeDetail.add') }}</button>
              </div>
            </div>
          </div>
          <label class="ond__checkbox">
            <input v-model="accessRuleForm.appliesToChildren" type="checkbox" />
            {{ t('orgNodeDetail.applyToChildren') }}
          </label>
          <div class="ond__actions">
            <BaseButton type="button" :loading="accessRuleSaving" :disabled="!canManageAccess" @click="addAccessRule">{{ t('orgNodeDetail.addRule') }}</BaseButton>
          </div>
          <div class="ond__list">
            <div v-for="rule in accessRules" :key="rule.id" class="ond__list-item">
              <div>
                <strong>{{ effectLabel(rule.effect) }} {{ permissionLabel(rule.permission) }}</strong>
                <p class="ond__muted">{{ formatRoleLabel(rule.subjectType) }}: {{ accessRuleSubjectValueLabel(rule) }}<span v-if="rule.appliesToChildren"> • {{ t('orgNodeDetail.applyToChildren') }}</span></p>
              </div>
              <button class="ond__danger" :disabled="!canManageAccess" @click="removeAccessRule(rule.id)">{{ t('orgNodeDetail.delete') }}</button>
            </div>
          </div>
        </section>

        <section class="ond__card">
          <div class="ond__card-head">
            <h4>{{ t('orgNodeDetail.nodeInfoTitle') }}</h4>
          </div>
          <div class="ond__grid">
            <BaseInput v-model="contentForm.summary" :label="t('orgNodeDetail.summaryLabel')" type="text" :placeholder="t('orgNodeDetail.summaryPlaceholder')" />
            <BaseInput v-model="contentForm.contactEmail" :label="t('orgNodeDetail.contactEmailLabel')" type="email" :placeholder="t('orgNodeDetail.contactEmailPlaceholder')" />
            <BaseInput v-model="contentForm.location" :label="t('orgNodeDetail.locationLabel')" type="text" :placeholder="t('orgNodeDetail.locationPlaceholder')" />
          </div>
          <div class="ond__meta-section">
            <div class="ond__meta-header">
              <label class="ond__label">{{ t('orgNodeDetail.extraInfoLabel') }}</label>
              <button class="ond__add-btn" type="button" :disabled="!canEditContent" @click="addMetadataEntry">+ {{ t('orgNodeDetail.addField') }}</button>
            </div>
            <p v-if="metadataEntries.length === 0" class="ond__muted ond__meta-empty">{{ t('orgNodeDetail.noExtraField') }}</p>
            <div v-for="(entry, i) in metadataEntries" :key="i" class="ond__meta-row">
              <input v-model="entry.key" class="ond__meta-input" :placeholder="t('orgNodeDetail.keyPlaceholder')" />
              <input v-model="entry.value" class="ond__meta-input" :placeholder="t('orgNodeDetail.valuePlaceholder')" />
              <button class="ond__danger ond__meta-remove" type="button" :disabled="!canEditContent" @click="removeMetadataEntry(i)" :title="t('orgNodeDetail.delete')" :aria-label="t('orgNodeDetail.removeFieldAria')">×</button>
            </div>
          </div>
          <label class="ond__label">{{ t('orgNodeDetail.descriptionLabel') }}</label>
          <textarea v-model="contentForm.description" class="ond__textarea" rows="5" :placeholder="t('orgNodeDetail.descriptionPlaceholder')" />
          <div class="ond__actions">
            <BaseButton type="button" :loading="contentSaving" :disabled="!canEditContent" @click="saveContent">{{ t('orgNodeDetail.saveInfo') }}</BaseButton>
          </div>
        </section>

        <section class="ond__card">
          <h4>{{ t('orgNodeDetail.usefulLinksTitle') }}</h4>
          <div class="ond__row-3">
            <BaseInput v-model="linkForm.label" :label="t('orgNodeDetail.labelLabel')" type="text" :placeholder="t('orgNodeDetail.labelPlaceholder')" />
            <BaseInput v-model="linkForm.url" :label="t('orgNodeDetail.urlLabel')" type="text" :placeholder="t('orgNodeDetail.urlPlaceholder')" />
            <div>
              <label class="ond__label">{{ t('orgNodeDetail.categoryLabel') }}</label>
              <select v-model="linkForm.category" class="ond__select">
                <option v-for="cat in categoryList" :key="cat" :value="cat">{{ cat }}</option>
              </select>
              <div class="ond__add-custom">
                <input v-model="newCategoryInput" class="ond__meta-input" :placeholder="t('orgNodeDetail.newCategoryPlaceholder')" @keyup.enter="addCategory" />
                <button type="button" class="ond__add-btn" @click="addCategory">+ {{ t('orgNodeDetail.add') }}</button>
              </div>
            </div>
          </div>
          <div class="ond__actions">
            <BaseButton type="button" :loading="linkSaving" :disabled="!canEditLinks" @click="addLink">{{ t('orgNodeDetail.addLink') }}</BaseButton>
          </div>
          <div class="ond__list">
            <div v-for="l in links" :key="l.id" class="ond__list-item">
              <a :href="l.url" target="_blank" rel="noopener" class="ond__link">{{ l.label }}</a>
              <span class="ond__chip" :style="categoryBadgeStyle(l.category)">{{ l.category }}</span>
              <button class="ond__danger" :disabled="!canEditLinks" @click="removeLink(l.id)">{{ t('orgNodeDetail.delete') }}</button>
            </div>
          </div>
        </section>

        <section class="ond__card">
          <h4>{{ t('orgNodeDetail.announcementsTitle') }}</h4>
          <BaseInput v-model="announcementForm.title" :label="t('orgNodeDetail.titleLabel')" type="text" :placeholder="t('orgNodeDetail.titlePlaceholder')" />
          <label class="ond__label">{{ t('orgNodeDetail.messageLabel') }}</label>
          <textarea v-model="announcementForm.body" class="ond__textarea" rows="4" :placeholder="t('orgNodeDetail.messagePlaceholder')" />
          <div class="ond__row-4">
            <div>
              <label class="ond__label">{{ t('orgNodeDetail.severityLabel') }}</label>
              <select v-model="announcementForm.severity" class="ond__select">
                <option v-for="sev in severityList" :key="sev" :value="sev">{{ sev }}</option>
              </select>
              <p v-if="SEVERITY_DESCRIPTIONS[announcementForm.severity]" class="ond__field-hint">
                {{ resolveTextKey(SEVERITY_DESCRIPTIONS[announcementForm.severity]) }}
              </p>
              <div class="ond__add-custom">
                <input v-model="newSeverityInput" class="ond__meta-input" :placeholder="t('orgNodeDetail.newSeverityPlaceholder')" @keyup.enter="addSeverity" />
                <button type="button" class="ond__add-btn" @click="addSeverity">+ {{ t('orgNodeDetail.add') }}</button>
              </div>
            </div>
            <div>
              <label class="ond__label">{{ t('orgNodeDetail.scopeLabel') }}</label>
              <select v-model="announcementForm.scopeType" class="ond__select">
                <option value="NODE">{{ t('orgNodeDetail.scopeNode') }}</option>
                <option value="SUBTREE">{{ t('orgNodeDetail.scopeSubtree') }}</option>
                <option value="GLOBAL">{{ t('orgNodeDetail.scopeGlobal') }}</option>
              </select>
              <p class="ond__field-hint">{{ resolveTextKey(SCOPE_DESCRIPTIONS[announcementForm.scopeType]) }}</p>
            </div>
            <div>
              <label class="ond__label">{{ t('orgNodeDetail.startLabel') }}</label>
              <input v-model="announcementForm.startAt" type="datetime-local" class="ond__input" />
            </div>
            <div>
              <label class="ond__label">{{ t('orgNodeDetail.endLabel') }}</label>
              <input v-model="announcementForm.endAt" type="datetime-local" class="ond__input" />
            </div>
          </div>
          <div class="ond__actions">
            <BaseButton type="button" :loading="announcementSaving" :disabled="!canManageAnnouncements" @click="addAnnouncement">{{ t('orgNodeDetail.publish') }}</BaseButton>
          </div>
          <div class="ond__list">
            <div v-for="a in announcements" :key="a.id" class="ond__list-item">
              <div>
                <strong>{{ a.title }}</strong>
                <p class="ond__muted">{{ a.body }}</p>
              </div>
              <span class="ond__chip" :style="severityBadgeStyle(a.severity)">{{ a.severity }}</span>
              <button class="ond__danger" :disabled="!canManageAnnouncements" @click="removeAnnouncement(a.id)">{{ t('orgNodeDetail.delete') }}</button>
            </div>
          </div>
        </section>
      </template>
    </div>
  </AppLayout>
</template>

<style scoped>
.ond { max-width: 960px; }
.ond__header h3 { margin: 0 0 0.25rem; }
.ond__meta { margin: 0 0 1rem; color: var(--color-text-muted); }
.ond__card { border: 1px solid rgba(148,163,184,.25); border-radius: var(--radius-md); padding: 1rem; margin-bottom: 1rem; background: var(--color-surface); }
.ond__grid { display: grid; grid-template-columns: 1fr 1fr; gap: .75rem; }
.ond__row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: .75rem; }
.ond__row-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: .75rem; }
.ond__row-4 { display: grid; grid-template-columns: 1fr 1fr 1fr 1fr; gap: .75rem; }
.ond__label { display:block; font-size:.85rem; font-weight:600; margin:.4rem 0 .25rem; }
.ond__textarea { width:100%; border:1px solid rgba(148,163,184,.45); border-radius: var(--radius-sm); padding:.55rem .65rem; font:inherit; }
.ond__select { width:100%; border:1px solid rgba(148,163,184,.45); border-radius: var(--radius-sm); padding:.55rem .65rem; font:inherit; height:2.4rem; background: var(--color-surface); }
.ond__actions { margin-top:.75rem; display:flex; justify-content:flex-end; }
.ond__list { margin-top:.75rem; display:grid; gap:.45rem; }
.ond__list-item { display:flex; align-items:center; justify-content:space-between; gap:.75rem; border:1px solid rgba(148,163,184,.2); border-radius: var(--radius-sm); padding:.5rem .6rem; }
.ond__link { color: var(--color-primary, #3b82f6); text-decoration: none; }
.ond__link:hover { text-decoration: underline; }
.ond__danger { border:1px solid rgba(220,38,38,.4); color:#dc2626; background:none; border-radius: var(--radius-sm); padding:.2rem .5rem; cursor:pointer; }
.ond__danger:disabled { opacity:.45; cursor:not-allowed; }
.ond__chips { display:flex; flex-wrap:wrap; gap:.35rem; }
.ond__chip { border:1px solid rgba(148,163,184,.25); background: rgba(148,163,184,.1); border-radius:999px; padding:.12rem .5rem; font-size:.78rem; }
.ond__muted { color: var(--color-text-muted); font-size:.85rem; margin:0; }
.ond__hint { color: var(--color-text-muted); }
.ond__error { color: var(--color-error, #dc2626); }
.ond__input { width: 100%; border: 1px solid rgba(148,163,184,.45); border-radius: var(--radius-sm); padding: .55rem .65rem; font: inherit; background: var(--color-surface); }
.ond__meta-section { margin-top: .75rem; }
.ond__meta-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: .4rem; }
.ond__meta-empty { margin: .25rem 0; font-style: italic; }
.ond__meta-row { display: grid; grid-template-columns: 1fr 1fr auto; gap: .5rem; margin-bottom: .35rem; align-items: center; }
.ond__meta-input { border: 1px solid rgba(148,163,184,.45); border-radius: var(--radius-sm); padding: .45rem .6rem; font: inherit; width: 100%; background: var(--color-surface); }
.ond__meta-remove { padding: .15rem .45rem; font-size: 1.1rem; line-height: 1; }
.ond__add-btn { border: 1px dashed rgba(148,163,184,.5); color: var(--color-primary, #3b82f6); background: none; border-radius: var(--radius-sm); padding: .25rem .65rem; cursor: pointer; font-size: .82rem; }
.ond__add-btn:hover:not(:disabled) { background: rgba(59,130,246,.06); }
.ond__add-btn:disabled { opacity: .45; cursor: not-allowed; }
.ond__add-custom { display: flex; gap: .4rem; margin-top: .35rem; align-items: center; }
.ond__field-hint { font-size: .78rem; color: var(--color-text-muted); margin: .25rem 0 0; font-style: italic; line-height: 1.35; }
.ond__align-end { display: flex; align-items: end; }
.ond__checkbox { display: flex; align-items: center; gap: .4rem; margin-top: .65rem; font-size: .86rem; color: var(--color-text-muted); }
.ond__rename-row { display: flex; align-items: center; gap: .5rem; margin-bottom: .15rem; }
.ond__header-name { margin: 0; }
.ond__rename-btn { background: none; border: none; cursor: pointer; color: var(--color-text-muted); padding: .25rem; border-radius: var(--radius-sm); display: flex; align-items: center; }
.ond__rename-btn:hover { color: var(--color-primary, #3b82f6); background: rgba(59,130,246,.07); }
.ond__rename-input { font-size: 1.17rem; font-weight: 700; border: 1px solid rgba(59,130,246,.6); border-radius: var(--radius-sm); padding: .25rem .5rem; font-family: inherit; color: var(--color-text); background: var(--color-surface); min-width: 16rem; }
.ond__rename-input:focus { outline: none; border-color: var(--color-primary, #3b82f6); }
.ond__rename-save { background: rgba(16,185,129,.1); border: 1px solid rgba(16,185,129,.5); color: #10b981; cursor: pointer; padding: .3rem; border-radius: var(--radius-sm); display: flex; align-items: center; }
.ond__rename-save:hover:not(:disabled) { background: rgba(16,185,129,.2); }
.ond__rename-save:disabled { opacity: .5; cursor: not-allowed; }
.ond__rename-cancel { background: none; border: none; cursor: pointer; color: var(--color-text-muted); padding: .3rem; border-radius: var(--radius-sm); display: flex; align-items: center; }
.ond__rename-cancel:hover { color: #dc2626; background: rgba(220,38,38,.07); }
@media (max-width: 900px) {
  .ond__grid, .ond__row-2, .ond__row-3, .ond__row-4 { grid-template-columns: 1fr; }
  .ond__rename-row { align-items: stretch; flex-wrap: wrap; }
  .ond__rename-input { min-width: 0; width: 100%; }
}
</style>
