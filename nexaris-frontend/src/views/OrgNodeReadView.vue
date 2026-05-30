<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '@/components/layout/AppLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from '@/i18n'
import type { TranslationKey } from '@/i18n/messages'
import {
  useOrgNodeReadApiAccess,
  type Announcement,
  type NodeContent,
  type NodeLink,
  type OrgNode,
} from '@/composables/useOrgNodeReadApiAccess'
import { toCatalogColorMap } from '@/utils/catalogTransform'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { t, locale } = useI18n()
const orgNodeReadApi = useOrgNodeReadApiAccess()

const nodeId = computed(() => Number(route.params.nodeId))

const loading = ref(true)
const error = ref('')

const node = ref<OrgNode | null>(null)
const content = ref<NodeContent | null>(null)
const links = ref<NodeLink[]>([])
const announcements = ref<Announcement[]>([])
const canEdit = ref(false)

const DEFAULT_TYPE_COLORS: Record<string, string> = {
  ORGANIZATION: '#1e40af',
  DIVISION: '#1e40af',
  DEPARTMENT: '#0369a1',
  TEAM: '#047857',
  UNIT: '#b45309',
}

const DEFAULT_SEVERITY_COLORS: Record<string, string> = {
  INFO: '#1d4ed8',
  WARNING: '#b45309',
  CRITICAL: '#dc2626',
  MAINTENANCE: '#6d28d9',
  INCIDENT: '#be123c',
}

const nodeTypeColors = ref<Record<string, string>>({ ...DEFAULT_TYPE_COLORS })
const severityColors = ref<Record<string, string>>({ ...DEFAULT_SEVERITY_COLORS })
const linkCategoryColors = ref<Record<string, string>>({})

function badgeStyle(color?: string) {
  if (!color) return {}
  return {
    background: `${color}22`,
    borderColor: `${color}88`,
    color,
  }
}

function severityStyle(sev: string) {
  const color = severityColors.value[sev?.toUpperCase()] ?? '#475569'
  return {
    background: `${color}22`,
    color,
    borderColor: `${color}66`,
  }
}

function typeLabel(nodeType: string) {
  const key = `adminOrg.nodeTypes.${nodeType}` as TranslationKey
  const translated = t(key)
  return translated.startsWith('adminOrg.nodeTypes.') ? nodeType : translated
}

function nodeColor(nodeType: string) {
  return nodeTypeColors.value[nodeType?.toUpperCase()] ?? '#64748b'
}

function linkCategoryStyle(cat: string) {
  return badgeStyle(linkCategoryColors.value[cat?.toUpperCase()])
}

function severityBadgeStyle(sev: string) {
  return badgeStyle(severityColors.value[sev?.toUpperCase()])
}

function formatDate(d: string | null | undefined) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString(locale.value, { day: '2-digit', month: 'long', year: 'numeric' })
}

const LINK_ICONS: Record<string, string> = {
  WEBSITE:    '🌐',
  DOCUMENT:   '📄',
  FORM:       '📋',
  INTRANET:   '🏠',
  TOOL:       '🔧',
  GENERAL:    '🔗',
}

function linkIcon(cat: string) {
  return LINK_ICONS[cat?.toUpperCase()] ?? '🔗'
}

const metaEntries = computed<{ key: string; value: string }[]>(() => {
  if (!content.value?.metadataJson) return []
  try {
    const obj = JSON.parse(content.value.metadataJson)
    if (typeof obj === 'object' && obj !== null && !Array.isArray(obj)) {
      return Object.entries(obj).map(([k, v]) => ({ key: k, value: String(v) }))
    }
  } catch {}
  return []
})

const activeLinks = computed(() => links.value.filter(l => l.isActive))
const activeAnnouncements = computed(() => announcements.value.filter(a => a.isActive))
const linksByCategory = computed(() => {
  const map: Record<string, NodeLink[]> = {}
  for (const l of activeLinks.value) {
    const cat = l.category ?? 'GENERAL'
    ;(map[cat] ??= []).push(l)
  }
  return Object.entries(map)
})
const showAdminNodeInfo = computed(() => authStore.isAdmin)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const workspace = await orgNodeReadApi.getNodeReadWorkspace(nodeId.value)
    node.value = workspace.node
    content.value = workspace.content
    links.value = workspace.links
    announcements.value = workspace.announcements

    nodeTypeColors.value = {
      ...DEFAULT_TYPE_COLORS,
      ...toCatalogColorMap(workspace.catalogs.nodeTypes),
    }

    linkCategoryColors.value = toCatalogColorMap(workspace.catalogs.linkCategories)
    severityColors.value = {
      ...DEFAULT_SEVERITY_COLORS,
      ...toCatalogColorMap(workspace.catalogs.severities),
    }

    canEdit.value = workspace.permissions.some(p =>
      ['EDIT_CONTENT', 'EDIT_LINKS', 'MANAGE_MEMBERS', 'MANAGE_ACCESS',
       'MANAGE_ANNOUNCEMENTS', 'CREATE_CHILD', 'DELETE_NODE'].includes(p),
    )
  } catch {
    error.value = t('orgNodeRead.errors.load')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <AppLayout :title="node?.name ?? t('orgNodeRead.titleFallback')">
    <div class="nrv">

      <!-- Back + Edit bar -->
      <div class="nrv__bar">
        <button type="button" class="nrv__back" :aria-label="t('orgNodeRead.back')" @click="router.back()">← {{ t('orgNodeRead.back') }}</button>
        <button
          v-if="canEdit"
          type="button"
          class="nrv__edit-btn"
          :aria-label="t('orgNodeRead.edit')"
          @click="router.push(`/org/nodes/${nodeId}`)"
        >✎ {{ t('orgNodeRead.edit') }}</button>
      </div>

      <p v-if="loading" class="nrv__hint">{{ t('orgNodeRead.loading') }}</p>
      <p v-else-if="error" class="nrv__error">{{ error }}</p>

      <template v-else-if="node">

        <!-- ── Hero header ─────────────────────────────────────────── -->
        <div class="nrv__hero" :style="{ borderColor: nodeColor(node.nodeType) }">
          <div class="nrv__hero-band" :style="{ background: nodeColor(node.nodeType) }">
            <span class="nrv__hero-type">{{ typeLabel(node.nodeType) }}</span>
          </div>
          <div class="nrv__hero-body">
            <h1 class="nrv__hero-name">{{ node.name }}</h1>
            <p v-if="content?.summary" class="nrv__hero-summary">{{ content.summary }}</p>
            <div class="nrv__hero-meta">
              <span v-if="content?.location" class="nrv__chip">📍 {{ content.location }}</span>
              <span v-if="content?.contactEmail" class="nrv__chip">
                ✉️ <a :href="`mailto:${content.contactEmail}`">{{ content.contactEmail }}</a>
              </span>
              <span class="nrv__chip nrv__chip--muted">{{ node.path }}</span>
            </div>
          </div>
        </div>

        <!-- ── Two-column body ─────────────────────────────────────── -->
        <div class="nrv__cols">

          <!-- Left column -->
          <div class="nrv__col">

            <!-- Description -->
            <section v-if="content?.description" class="nrv__section">
              <h2 class="nrv__section-title">{{ t('orgNodeRead.sections.description') }}</h2>
              <p class="nrv__description">{{ content.description }}</p>
            </section>

            <!-- Links -->
            <section v-if="activeLinks.length" class="nrv__section">
              <h2 class="nrv__section-title">🔗 {{ t('orgNodeRead.sections.usefulLinks') }}</h2>
              <div v-for="[cat, catLinks] in linksByCategory" :key="cat" class="nrv__link-group">
                <p class="nrv__link-cat" :style="linkCategoryStyle(cat)">{{ cat }}</p>
                <a
                  v-for="link in catLinks"
                  :key="link.id"
                  :href="link.url"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="nrv__link-item"
                >
                  <span class="nrv__link-icon">{{ linkIcon(link.category) }}</span>
                  <span class="nrv__link-label">{{ link.label }}</span>
                  <span class="nrv__link-arrow">↗</span>
                </a>
              </div>
            </section>

            <!-- Metadata extras -->
            <section v-if="metaEntries.length" class="nrv__section">
              <h2 class="nrv__section-title">{{ t('orgNodeRead.sections.additionalInfo') }}</h2>
              <dl class="nrv__dl">
                <div v-for="entry in metaEntries" :key="entry.key" class="nrv__dl-row">
                  <dt class="nrv__dt">{{ entry.key }}</dt>
                  <dd class="nrv__dd">{{ entry.value }}</dd>
                </div>
              </dl>
            </section>

          </div>

          <!-- Right column -->
          <div class="nrv__col">

            <!-- Announcements -->
            <section v-if="activeAnnouncements.length" class="nrv__section">
              <h2 class="nrv__section-title">📢 {{ t('orgNodeRead.sections.announcements') }}</h2>
              <div
                v-for="ann in activeAnnouncements"
                :key="ann.id"
                class="nrv__ann"
                :style="severityStyle(ann.severity)"
              >
                <div class="nrv__ann-head">
                  <strong class="nrv__ann-title">{{ ann.title }}</strong>
                  <span class="nrv__ann-badge" :style="severityBadgeStyle(ann.severity)">{{ ann.severity }}</span>
                </div>
                <p class="nrv__ann-body">{{ ann.body }}</p>
                <p v-if="ann.startAt || ann.endAt" class="nrv__ann-dates">
                  <span v-if="ann.startAt">{{ t('orgNodeRead.dates.from') }} {{ formatDate(ann.startAt) }}</span>
                  <span v-if="ann.endAt"> {{ t('orgNodeRead.dates.to') }} {{ formatDate(ann.endAt) }}</span>
                </p>
              </div>
            </section>

            <!-- Node info card -->
            <section v-if="showAdminNodeInfo" class="nrv__section">
              <h2 class="nrv__section-title">{{ t('orgNodeRead.sections.nodeInfo') }}</h2>
              <dl class="nrv__dl">
                <div class="nrv__dl-row">
                  <dt class="nrv__dt">{{ t('orgNodeRead.fields.id') }}</dt>
                  <dd class="nrv__dd nrv__dd--mono">#{{ node.id }}</dd>
                </div>
                <div class="nrv__dl-row">
                  <dt class="nrv__dt">{{ t('orgNodeRead.fields.type') }}</dt>
                  <dd class="nrv__dd">{{ typeLabel(node.nodeType) }}</dd>
                </div>
                <div class="nrv__dl-row">
                  <dt class="nrv__dt">{{ t('orgNodeRead.fields.slug') }}</dt>
                  <dd class="nrv__dd nrv__dd--mono">{{ node.slug }}</dd>
                </div>
                <div class="nrv__dl-row">
                  <dt class="nrv__dt">{{ t('orgNodeRead.fields.path') }}</dt>
                  <dd class="nrv__dd nrv__dd--mono">{{ node.path }}</dd>
                </div>
                <div class="nrv__dl-row">
                  <dt class="nrv__dt">{{ t('orgNodeRead.fields.depth') }}</dt>
                  <dd class="nrv__dd">{{ t('orgNodeRead.fields.level') }} {{ node.depth }}</dd>
                </div>
              </dl>
            </section>

          </div>
        </div>

        <!-- Empty state when nothing to show -->
        <p
          v-if="!content?.description && !activeLinks.length && !activeAnnouncements.length && !metaEntries.length"
          class="nrv__empty"
        >
          {{ t('orgNodeRead.emptyDetailedContent') }}
        </p>

      </template>
    </div>
  </AppLayout>
</template>

<style scoped>
.nrv {
  max-width: 1100px;
  padding: 0 0 3rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* ── Bar ─────────────────────────────────── */
.nrv__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.nrv__back {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 0.375rem;
  padding: 0.4rem 0.85rem;
  font-size: 0.85rem;
  cursor: pointer;
  color: var(--color-text-muted);
  transition: all 0.15s;
}
.nrv__back:hover { background: var(--color-surface); color: var(--color-text); }

.nrv__edit-btn {
  background: var(--color-primary, #3b82f6);
  color: white;
  border: none;
  border-radius: 0.375rem;
  padding: 0.4rem 1rem;
  font-size: 0.85rem;
  cursor: pointer;
  font-weight: 600;
  transition: opacity 0.15s;
}
.nrv__edit-btn:hover { opacity: 0.87; }

/* ── Hero ────────────────────────────────── */
.nrv__hero {
  border-radius: 0.75rem;
  overflow: hidden;
  border: 2px solid;
  background: var(--color-surface);
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.nrv__hero-band {
  padding: 0.7rem 1.25rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: white;
}

.nrv__hero-type {
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.nrv__hero-body {
  padding: 1.25rem 1.5rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.nrv__hero-name {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 800;
  color: var(--color-text);
  line-height: 1.2;
}

.nrv__hero-summary {
  margin: 0;
  font-size: 1rem;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.nrv__hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.25rem;
}

/* ── Chips ───────────────────────────────── */
.nrv__chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.78rem;
  padding: 0.25rem 0.65rem;
  border-radius: 9999px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  color: var(--color-text);
}
.nrv__chip a { color: inherit; text-decoration: none; }
.nrv__chip--muted { color: var(--color-text-muted); font-family: monospace; }

/* ── Two-column layout ───────────────────── */
.nrv__cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
  align-items: start;
}

@media (max-width: 760px) {
  .nrv__cols { grid-template-columns: 1fr; }
}

.nrv__col {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* ── Sections ────────────────────────────── */
.nrv__section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 0.65rem;
  padding: 1.1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.nrv__section-title {
  margin: 0;
  font-size: 0.85rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-muted);
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 0.6rem;
}

/* ── Description ─────────────────────────── */
.nrv__description {
  margin: 0;
  font-size: 0.93rem;
  line-height: 1.65;
  color: var(--color-text);
  white-space: pre-wrap;
}

/* ── Links ───────────────────────────────── */
.nrv__link-group { display: flex; flex-direction: column; gap: 0.35rem; }
.nrv__link-cat {
  margin: 0;
  display: inline-flex;
  align-items: center;
  width: fit-content;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
  border: 1px solid transparent;
  border-radius: 9999px;
  padding: 0.12rem 0.5rem;
}

.nrv__link-item {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.55rem 0.8rem;
  border-radius: 0.45rem;
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  text-decoration: none;
  color: var(--color-text);
  font-size: 0.88rem;
  transition: all 0.15s;
}
.nrv__link-item:hover {
  border-color: var(--color-primary, #3b82f6);
  background: var(--color-surface);
}
.nrv__link-icon { font-size: 1rem; }
.nrv__link-label { flex: 1; font-weight: 500; }
.nrv__link-arrow { color: var(--color-text-muted); font-size: 0.85rem; }

/* ── DL (definition list) ────────────────── */
.nrv__dl { display: flex; flex-direction: column; gap: 0; margin: 0; }
.nrv__dl-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 1rem;
  padding: 0.45rem 0;
  border-bottom: 1px solid var(--color-border);
}
.nrv__dl-row:last-child { border-bottom: none; }
.nrv__dt {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-muted);
  flex-shrink: 0;
}
.nrv__dd {
  margin: 0;
  font-size: 0.88rem;
  color: var(--color-text);
  text-align: right;
}
.nrv__dd--mono { font-family: monospace; font-size: 0.82rem; }

/* ── Announcements ───────────────────────── */
.nrv__ann {
  border-radius: 0.5rem;
  padding: 0.85rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  border: 1px solid transparent;
}
.nrv__ann-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}
.nrv__ann-title { font-size: 0.9rem; font-weight: 700; }
.nrv__ann-badge {
  font-size: 0.68rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
  background: rgba(0,0,0,0.08);
}
.nrv__ann-body  { margin: 0; font-size: 0.88rem; line-height: 1.5; }
.nrv__ann-dates { margin: 0; font-size: 0.78rem; opacity: 0.75; }

/* ── Misc ────────────────────────────────── */
.nrv__hint  { color: var(--color-text-muted); font-size: 0.9rem; }
.nrv__error { color: #dc2626; font-size: 0.9rem; }
.nrv__empty { color: var(--color-text-muted); font-size: 0.9rem; text-align: center; padding: 2rem 0; }
</style>
