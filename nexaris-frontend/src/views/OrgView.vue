<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '@/components/layout/AppLayout.vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseInput from '@/components/BaseInput.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useI18n } from '@/i18n'
import { useAuthStore } from '@/stores/auth'
import { useAdminOrgApiAccess } from '@/composables/useAdminOrgApiAccess'
import { useOrgTreeApiAccess, type MyOrgTreeNode } from '@/composables/useOrgTreeApiAccess'
import { toCatalogColorMap } from '@/utils/catalogTransform'
import { isBlank, trimOrEmpty } from '@/utils/validation'

const { t } = useI18n()
const router = useRouter()
const auth = useAuthStore()
const orgTreeApi = useOrgTreeApiAccess()
const adminOrgApi = useAdminOrgApiAccess()

const tree = ref<MyOrgTreeNode[]>([])
const loading = ref(false)
const error = ref('')
const search = ref('')
const editableNodeIds = ref<number[]>([])
const nodeCounters = ref<Record<number, { links: number; announcements: number }>>({})
const nodeTypeColors = ref<Record<string, string>>({})
const expandedById = ref<Record<number, boolean>>({})
const selectedNodeId = ref<number | null>(null)
const nodeById = ref<Record<number, MyOrgTreeNode>>({})
const parentById = ref<Record<number, number | null>>({})
const nodeTypeOptions = ref<string[]>([])

const showCreateForm = ref(false)
const createFormMode = ref<'top' | 'inline'>('inline')
const createName = ref('')
const createType = ref('DEPARTMENT')
const createParentId = ref<number | null>(null)
const createLoading = ref(false)
const createError = ref('')

const showRenameForm = ref(false)
const renameName = ref('')
const renameLoading = ref(false)
const renameError = ref('')

const showDeleteConfirm = ref(false)
const deletingNode = ref(false)
const deleteError = ref('')

interface TreeRow {
  node: MyOrgTreeNode
  depth: number
  hasChildren: boolean
  expanded: boolean
}

const DEFAULT_TYPE_COLORS: Record<string, string> = {
  ORGANIZATION: '#3b82f6',
  DIVISION: '#3b82f6',
  DEPARTMENT: '#0ea5e9',
  TEAM: '#10b981',
  UNIT: '#f59e0b',
}

function typeColor(type: string) {
  return nodeTypeColors.value[type?.toUpperCase()] ?? DEFAULT_TYPE_COLORS[type] ?? '#94a3b8'
}

function normalizeText(value: string) {
  return value.trim().toLowerCase()
}

function openNodeForEdit(nodeId: number) {
  router.push(`/org/nodes/${nodeId}`)
}

function openNodeDetail(nodeId: number) {
  router.push(`/org/nodes/${nodeId}/view`)
}

function canEditNode(nodeId: number) {
  return editableNodeIds.value.includes(nodeId)
}

function canManageNode(nodeId: number) {
  return auth.isAdmin || canEditNode(nodeId)
}

function canDeleteNode(nodeId: number) {
  return canManageNode(nodeId) && (parentById.value[nodeId] ?? null) !== null
}

function collectEditableIds(nodes: MyOrgTreeNode[]): number[] {
  const ids: number[] = []
  const visit = (node: MyOrgTreeNode) => {
    if (node.canEdit) ids.push(node.id)
    node.children?.forEach(visit)
  }
  nodes.forEach(visit)
  return ids
}

function flattenTree(nodes: MyOrgTreeNode[]): MyOrgTreeNode[] {
  const result: MyOrgTreeNode[] = []
  const visit = (node: MyOrgTreeNode) => {
    result.push(node)
    if (node.children) node.children.forEach(visit)
  }
  nodes.forEach(visit)
  return result
}

function compareNodes(a: MyOrgTreeNode, b: MyOrgTreeNode) {
  const aChildren = a.children?.length ?? 0
  const bChildren = b.children?.length ?? 0
  const byChildrenCount = aChildren - bChildren
  if (byChildrenCount !== 0) return byChildrenCount

  const bySortOrder = (a.sortOrder ?? 0) - (b.sortOrder ?? 0)
  if (bySortOrder !== 0) return bySortOrder

  const byName = a.name.localeCompare(b.name, 'fr', { sensitivity: 'base' })
  if (byName !== 0) return byName

  return a.id - b.id
}

function normalizeHierarchyOrder(nodes: MyOrgTreeNode[]): MyOrgTreeNode[] {
  return [...nodes]
    .sort(compareNodes)
    .map((node) => ({
      ...node,
      children: normalizeHierarchyOrder(node.children ?? []),
    }))
}

function buildIndexes(nodes: MyOrgTreeNode[]) {
  const nextNodeById: Record<number, MyOrgTreeNode> = {}
  const nextParentById: Record<number, number | null> = {}

  const visit = (items: MyOrgTreeNode[], parentId: number | null) => {
    items.forEach((node) => {
      nextNodeById[node.id] = node
      nextParentById[node.id] = parentId
      if (node.children?.length && expandedById.value[node.id] === undefined) {
        expandedById.value[node.id] = true
      }
      if (node.children?.length) visit(node.children, node.id)
    })
  }

  visit(nodes, null)
  nodeById.value = nextNodeById
  parentById.value = nextParentById
}

function buildRows(nodes: MyOrgTreeNode[], depth: number, query: string): TreeRow[] {
  const rows: TreeRow[] = []

  nodes.forEach((node) => {
    const hasChildren = Boolean(node.children?.length)
    const childRows = hasChildren ? buildRows(node.children, depth + 1, query) : []

    const selfMatches = !query
      || normalizeText(node.name).includes(query)
      || normalizeText(node.nodeType).includes(query)
      || normalizeText(node.path).includes(query)

    const includeNode = !query || selfMatches || childRows.length > 0
    if (!includeNode) return

    const expanded = query ? true : (expandedById.value[node.id] ?? true)
    rows.push({ node, depth, hasChildren, expanded })

    if (hasChildren && expanded) {
      rows.push(...childRows)
    }
  })

  return rows
}

const treeRows = computed(() => {
  const query = normalizeText(search.value)
  return buildRows(tree.value, 0, query)
})

const selectedNode = computed(() => {
  if (selectedNodeId.value && nodeById.value[selectedNodeId.value]) {
    return nodeById.value[selectedNodeId.value]
  }
  return treeRows.value[0]?.node ?? null
})

const selectedNodePath = computed(() => {
  if (!selectedNode.value) return []
  const path: MyOrgTreeNode[] = []
  let cursor: number | null = selectedNode.value.id
  while (cursor) {
    const node = nodeById.value[cursor]
    if (!node) break
    path.unshift(node)
    cursor = parentById.value[cursor] ?? null
  }
  return path
})

const totalVisibleNodes = computed(() => flattenTree(tree.value).length)
const manageableParentNodes = computed(() => {
  return flattenTree(tree.value).filter((node) => canManageNode(node.id))
})
const canShowTopCreateButton = computed(() => manageableParentNodes.value.length > 0)
const selectedCreateParentLabel = computed(() => {
  if (createParentId.value === null) return '-'
  const node = nodeById.value[createParentId.value]
  if (!node) return '-'
  return `${node.name} (${node.nodeType})`
})
const canManageSelectedNode = computed(() => {
  if (!selectedNode.value) return false
  return canManageNode(selectedNode.value.id)
})

const canDeleteSelectedNode = computed(() => {
  if (!selectedNode.value) return false
  return canDeleteNode(selectedNode.value.id)
})

const deleteNodeDetails = computed(() => {
  if (!selectedNode.value) return ''
  return `${selectedNode.value.name} (${selectedNode.value.nodeType})`
})

function selectNode(nodeId: number) {
  selectedNodeId.value = nodeId
}

function toggleNode(nodeId: number) {
  expandedById.value[nodeId] = !(expandedById.value[nodeId] ?? true)
}

function resetManageState() {
  showCreateForm.value = false
  createFormMode.value = 'inline'
  showRenameForm.value = false
  createName.value = ''
  renameName.value = ''
  createError.value = ''
  renameError.value = ''
  deleteError.value = ''
}

function openCreateWithParentPicker() {
  if (!manageableParentNodes.value.length) return
  showRenameForm.value = false
  createFormMode.value = 'top'
  createName.value = ''
  createType.value = nodeTypeOptions.value[0] ?? 'DEPARTMENT'
  createParentId.value = selectedNode.value && canManageNode(selectedNode.value.id)
    ? selectedNode.value.id
    : manageableParentNodes.value[0]?.id ?? null
  createError.value = ''
  showCreateForm.value = true
}

function openCreateForNode(nodeId: number) {
  if (!canManageNode(nodeId)) return
  showRenameForm.value = false
  createFormMode.value = 'inline'
  createParentId.value = nodeId
  createName.value = ''
  createType.value = nodeTypeOptions.value[0] ?? 'DEPARTMENT'
  createError.value = ''
  showCreateForm.value = true
}

function openRenameForNode(nodeId: number) {
  if (!canManageNode(nodeId)) return
  const node = nodeById.value[nodeId]
  if (!node) return
  showCreateForm.value = false
  renameName.value = node.name
  renameError.value = ''
  showRenameForm.value = true
}

async function submitCreateNode() {
  if (isBlank(createName.value) || createParentId.value === null) return
  if (!canManageNode(createParentId.value)) {
    createError.value = t('adminOrg.errors.createNode')
    return
  }

  createLoading.value = true
  createError.value = ''
  try {
    const createdNode = await adminOrgApi.createNode({
      parentId: createParentId.value,
      nodeType: createType.value,
      name: trimOrEmpty(createName.value),
    })

    let creatorMembershipAttachFailed = false
    if (!auth.isAdmin && auth.user?.id) {
      try {
        await adminOrgApi.createMembership(createdNode.id, {
          userId: auth.user.id,
          membershipRole: 'MEMBRE',
          isPrimary: true,
        })
      } catch {
        creatorMembershipAttachFailed = true
      }
    }

    await loadTree()
    if (creatorMembershipAttachFailed) {
      error.value = t('adminOrg.errors.addMember')
    }
    showCreateForm.value = false
    createFormMode.value = 'inline'
    createName.value = ''
  } catch {
    createError.value = t('adminOrg.errors.createNode')
  } finally {
    createLoading.value = false
  }
}

async function submitRenameNode() {
  if (!selectedNode.value || isBlank(renameName.value)) return
  if (!canManageNode(selectedNode.value.id)) {
    renameError.value = t('adminOrg.errors.renameNode')
    return
  }

  renameLoading.value = true
  renameError.value = ''
  try {
    const nodeId = selectedNode.value.id
    await adminOrgApi.updateNode(nodeId, { name: trimOrEmpty(renameName.value) })
    await loadTree()
    selectedNodeId.value = nodeId
    showRenameForm.value = false
  } catch {
    renameError.value = t('adminOrg.errors.renameNode')
  } finally {
    renameLoading.value = false
  }
}

function askDeleteNodeConfirmation() {
  if (!selectedNode.value || !canDeleteNode(selectedNode.value.id)) return
  showDeleteConfirm.value = true
  deleteError.value = ''
}

function cancelDeleteNodeConfirmation() {
  showDeleteConfirm.value = false
}

async function confirmDeleteNode() {
  if (!selectedNode.value || !canDeleteNode(selectedNode.value.id)) {
    showDeleteConfirm.value = false
    return
  }

  deletingNode.value = true
  deleteError.value = ''
  const nodeId = selectedNode.value.id
  try {
    await adminOrgApi.deleteNode(nodeId)
    showDeleteConfirm.value = false
    await loadTree()
  } catch {
    deleteError.value = t('adminOrg.errors.deleteNode')
  } finally {
    deletingNode.value = false
  }
}

async function loadNodeTypes() {
  try {
    const nodeTypeMeta = await adminOrgApi.getNodeTypeCatalogMeta(true)
    nodeTypeOptions.value = nodeTypeMeta.map((entry) => entry.value)
  } catch {
    nodeTypeOptions.value = ['ORGANIZATION', 'DIVISION', 'DEPARTMENT', 'TEAM', 'UNIT']
  }

  if (!nodeTypeOptions.value.includes(createType.value)) {
    createType.value = nodeTypeOptions.value[0] ?? 'DEPARTMENT'
  }
}

async function loadNodeCounters() {
  const allNodes = flattenTree(tree.value)
  const counters: Record<number, { links: number; announcements: number }> = {}

  try {
    await Promise.all(allNodes.map(async (node) => {
      try {
        const [linksRes, annRes] = await Promise.all([
          orgTreeApi.getNodeLinks(node.id, true),
          orgTreeApi.getNodeAnnouncements(node.id),
        ])
        counters[node.id] = {
          links: (linksRes.data ?? []).length,
          announcements: (annRes.data ?? []).length,
        }
      } catch {
        counters[node.id] = { links: 0, announcements: 0 }
      }
    }))
    nodeCounters.value = counters
  } catch {
    // silently handle
  }
}

async function loadTree() {
  loading.value = true
  error.value = ''
  deleteError.value = ''
  editableNodeIds.value = []
  try {
    const workspace = await orgTreeApi.getMyTreeWorkspace()
    const data = normalizeHierarchyOrder(workspace.tree)
    tree.value = data
    nodeTypeColors.value = {
      ...DEFAULT_TYPE_COLORS,
      ...toCatalogColorMap(workspace.nodeTypeMeta),
    }
    buildIndexes(data)
    editableNodeIds.value = collectEditableIds(data)
    selectedNodeId.value = data[0]?.id ?? null
    resetManageState()
    await loadNodeCounters()
  } catch {
    error.value = t('org.loadError')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadTree(), loadNodeTypes()])
})
</script>

<template>
  <AppLayout :title="t('org.title')">
    <div class="ov">
      <div class="ov__topbar">
        <div>
          <h3 class="ov__title">{{ t('org.title') }}</h3>
          <p class="ov__sub">{{ t('org.overviewSubtitle') }}</p>
        </div>
        <div class="ov__topbar-meta">
          <span class="ov__pill">{{ totalVisibleNodes }} {{ t('org.visibleNodesLabel') }}</span>
          <BaseButton
            v-if="canShowTopCreateButton"
            type="button"
            size="sm"
            @click="openCreateWithParentPicker"
          >
            + {{ t('adminOrg.addNode') }}
          </BaseButton>
          <input
            v-model="search"
            class="ov__search"
            type="text"
            :placeholder="t('org.searchPlaceholder')"
          />
        </div>
      </div>

      <div v-if="showCreateForm && createFormMode === 'top'" class="ov__create-top-card">
        <h5>{{ t('adminOrg.addNode') }}</h5>
        <p class="ov__hint">
          {{ t('adminOrg.createUnder') }}: {{ selectedCreateParentLabel }}
        </p>
        <div class="ov__create-top-grid">
          <BaseInput
            v-model="createName"
            :label="t('adminOrg.nodeNameLabel')"
            type="text"
            :placeholder="t('adminOrg.nodeNamePlaceholder')"
          />
          <div class="ov__manage-field">
            <label class="ov__manage-label">{{ t('adminOrg.parentLabel') }}</label>
            <select
              :value="createParentId ?? ''"
              class="ov__manage-select"
              @change="createParentId = Number(($event.target as HTMLSelectElement).value)"
            >
              <option v-for="parentNode in manageableParentNodes" :key="parentNode.id" :value="parentNode.id">
                {{ parentNode.name }} ({{ parentNode.nodeType }})
              </option>
            </select>
          </div>
          <div class="ov__manage-field">
            <label class="ov__manage-label">{{ t('adminOrg.nodeTypeLabel') }}</label>
            <select v-model="createType" class="ov__manage-select">
              <option v-for="tp in nodeTypeOptions" :key="tp" :value="tp">{{ tp }}</option>
            </select>
          </div>
        </div>
        <p v-if="createError" class="ov__error">{{ createError }}</p>
        <div class="ov__manage-actions">
          <BaseButton type="button" variant="ghost" @click="showCreateForm = false">
            {{ t('adminOrg.cancel') }}
          </BaseButton>
          <BaseButton type="button" :loading="createLoading" @click="submitCreateNode">
            {{ t('adminOrg.create') }}
          </BaseButton>
        </div>
      </div>

      <p v-if="loading" class="ov__hint">…</p>
      <p v-else-if="error" class="ov__error">{{ error }}</p>
      <p v-else-if="tree.length === 0" class="ov__hint">{{ t('org.empty') }}</p>

      <div v-else class="ov__workspace">
        <section class="ov__tree-panel">
          <div class="ov__panel-head">{{ t('org.treeTitle') }}</div>
          <div v-if="treeRows.length === 0" class="ov__hint">{{ t('org.noSearchResult') }}</div>
          <div v-else class="ov__rows">
            <div
              v-for="row in treeRows"
              :key="row.node.id"
              class="ov__row"
              :class="{ 'ov__row--selected': selectedNode?.id === row.node.id }"
              :style="{ '--depth': row.depth, '--node-color': typeColor(row.node.nodeType) }"
              @click="selectNode(row.node.id)"
              @keydown.enter="selectNode(row.node.id)"
              @keydown.space.prevent="selectNode(row.node.id)"
              tabindex="0"
              role="button"
            >
              <span class="ov__row-indent" aria-hidden="true" />
              <button
                v-if="row.hasChildren"
                class="ov__toggle"
                type="button"
                :aria-label="row.expanded ? t('org.collapseNode') : t('org.expandNode')"
                @click.stop="toggleNode(row.node.id)"
              >
                {{ row.expanded ? '▾' : '▸' }}
              </button>
              <span v-else class="ov__toggle ov__toggle--ghost">·</span>

              <span class="ov__type-dot" />

              <span class="ov__row-main">
                <span class="ov__row-name">{{ row.node.name }}</span>
                <span class="ov__row-meta">{{ row.node.nodeType }} · {{ row.node.children?.length ?? 0 }} {{ t('org.childrenLabel') }}</span>
              </span>

              <span class="ov__row-counters">
                <span class="ov__badge">🔗 {{ nodeCounters[row.node.id]?.links ?? 0 }}</span>
                <span class="ov__badge">📢 {{ nodeCounters[row.node.id]?.announcements ?? 0 }}</span>
              </span>
            </div>
          </div>
        </section>

        <section v-if="selectedNode" class="ov__detail-panel">
          <div class="ov__detail-hero" :style="{ borderColor: typeColor(selectedNode.nodeType) }">
            <div class="ov__detail-band" :style="{ background: typeColor(selectedNode.nodeType) }" />
            <div class="ov__detail-body">
              <span class="ov__detail-type">{{ selectedNode.nodeType }}</span>
              <h4 class="ov__detail-name">{{ selectedNode.name }}</h4>
              <p class="ov__detail-path">{{ selectedNode.path }}</p>

              <div class="ov__crumbs">
                <span v-for="crumb in selectedNodePath" :key="crumb.id" class="ov__crumb">{{ crumb.name }}</span>
              </div>

              <div class="ov__stats">
                <div class="ov__stat">
                  <span class="ov__stat-label">{{ t('org.directChildrenLabel') }}</span>
                  <span class="ov__stat-value">{{ selectedNode.children?.length ?? 0 }}</span>
                </div>
                <div class="ov__stat">
                  <span class="ov__stat-label">{{ t('org.linksLabel') }}</span>
                  <span class="ov__stat-value">{{ nodeCounters[selectedNode.id]?.links ?? 0 }}</span>
                </div>
                <div class="ov__stat">
                  <span class="ov__stat-label">{{ t('org.announcementsLabel') }}</span>
                  <span class="ov__stat-value">{{ nodeCounters[selectedNode.id]?.announcements ?? 0 }}</span>
                </div>
              </div>

              <div class="ov__actions">
                <button class="ov__btn ov__btn--detail" @click="openNodeDetail(selectedNode.id)">👁 {{ t('org.detailAction') }}</button>
                <button
                  v-if="canEditNode(selectedNode.id)"
                  class="ov__btn ov__btn--edit"
                  @click="openNodeForEdit(selectedNode.id)"
                >✎ {{ t('org.editNodeAction') }}</button>
                <button
                  v-if="canManageSelectedNode"
                  class="ov__btn ov__btn--manage"
                  @click="openCreateForNode(selectedNode.id)"
                >＋ {{ t('adminOrg.addChildNode') }}</button>
                <button
                  v-if="canManageSelectedNode"
                  class="ov__btn ov__btn--manage"
                  @click="openRenameForNode(selectedNode.id)"
                >✎ {{ t('adminOrg.renameNodeAction') }}</button>
                <button
                  v-if="canDeleteSelectedNode"
                  class="ov__btn ov__btn--danger"
                  :disabled="deletingNode"
                  @click="askDeleteNodeConfirmation"
                >✕ {{ t('adminOrg.deleteNode') }}</button>
              </div>

              <div
                v-if="showCreateForm && createFormMode === 'inline' && createParentId === selectedNode.id"
                class="ov__manage-card"
              >
                <h5>{{ t('adminOrg.addChildNode') }}</h5>
                <BaseInput
                  v-model="createName"
                  :label="t('adminOrg.nodeNameLabel')"
                  type="text"
                  :placeholder="t('adminOrg.nodeNamePlaceholder')"
                />
                <div class="ov__manage-field">
                  <label class="ov__manage-label">{{ t('adminOrg.nodeTypeLabel') }}</label>
                  <select v-model="createType" class="ov__manage-select">
                    <option v-for="tp in nodeTypeOptions" :key="tp" :value="tp">{{ tp }}</option>
                  </select>
                </div>
                <p v-if="createError" class="ov__error">{{ createError }}</p>
                <div class="ov__manage-actions">
                  <BaseButton type="button" variant="ghost" @click="showCreateForm = false">
                    {{ t('adminOrg.cancel') }}
                  </BaseButton>
                  <BaseButton type="button" :loading="createLoading" @click="submitCreateNode">
                    {{ t('adminOrg.create') }}
                  </BaseButton>
                </div>
              </div>

              <div v-if="showRenameForm" class="ov__manage-card">
                <h5>{{ t('adminOrg.renameNodeAction') }}</h5>
                <BaseInput
                  v-model="renameName"
                  :label="t('adminOrg.nodeNameLabel')"
                  type="text"
                  :placeholder="t('adminOrg.nodeNamePlaceholder')"
                />
                <p v-if="renameError" class="ov__error">{{ renameError }}</p>
                <div class="ov__manage-actions">
                  <BaseButton type="button" variant="ghost" @click="showRenameForm = false">
                    {{ t('adminOrg.cancel') }}
                  </BaseButton>
                  <BaseButton type="button" :loading="renameLoading" @click="submitRenameNode">
                    {{ t('adminOrg.renameNodeAction') }}
                  </BaseButton>
                </div>
              </div>

              <p v-if="deleteError" class="ov__error">{{ deleteError }}</p>
            </div>
          </div>
        </section>
      </div>

      <ConfirmDialog
        v-model="showDeleteConfirm"
        :title="t('adminOrg.deleteNode')"
        :message="t('adminOrg.confirmDeleteNode')"
        :details="deleteNodeDetails"
        :confirm-text="t('adminOrg.deleteNode')"
        :cancel-text="t('adminOrg.cancel')"
        confirm-variant="danger"
        :loading="deletingNode"
        @cancel="cancelDeleteNodeConfirmation"
        @confirm="confirmDeleteNode"
      />
    </div>
  </AppLayout>
</template>

<style scoped>
.ov {
  max-width: 1400px;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.ov__title {
  font-size: 1.2rem;
  font-weight: 700;
  margin: 0;
}

.ov__sub {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  margin: 0;
}

.ov__topbar {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
}

.ov__topbar-meta {
  display: flex;
  gap: 0.6rem;
  align-items: center;
  flex-wrap: wrap;
}

.ov__pill {
  font-size: 0.78rem;
  font-weight: 600;
  padding: 0.3rem 0.55rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-primary, #3b82f6) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-primary, #3b82f6) 30%, transparent);
}

.ov__search {
  width: 320px;
  border: 1px solid color-mix(in srgb, var(--color-text) 16%, transparent);
  background: color-mix(in srgb, var(--color-surface) 82%, transparent);
  border-radius: 0.55rem;
  padding: 0.5rem 0.7rem;
  color: var(--color-text);
}

.ov__workspace {
  display: grid;
  grid-template-columns: minmax(380px, 0.95fr) minmax(320px, 1.05fr);
  gap: 1rem;
  min-height: 68vh;
}

.ov__tree-panel,
.ov__detail-panel {
  border: 1px solid color-mix(in srgb, var(--color-text) 10%, transparent);
  border-radius: 0.9rem;
  background:
    radial-gradient(1200px 450px at 100% 0%, color-mix(in srgb, var(--color-primary, #3b82f6) 12%, transparent), transparent 50%),
    color-mix(in srgb, var(--color-surface) 96%, transparent);
  backdrop-filter: blur(6px);
}

.ov__panel-head {
  padding: 0.85rem 0.95rem;
  font-size: 0.84rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
  border-bottom: 1px solid color-mix(in srgb, var(--color-text) 10%, transparent);
}

.ov__rows {
  max-height: calc(68vh - 50px);
  overflow: auto;
  padding: 0.35rem;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.ov__row {
  position: relative;
  border: 1px solid transparent;
  border-radius: 0.6rem;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 0.45rem;
  width: 100%;
  padding: 0.45rem 0.55rem;
  color: var(--color-text);
  text-align: left;
  cursor: pointer;
  transition: all 0.12s ease;
}

.ov__row:hover {
  border-color: color-mix(in srgb, var(--node-color) 35%, transparent);
  background: color-mix(in srgb, var(--node-color) 10%, transparent);
}

.ov__row--selected {
  border-color: color-mix(in srgb, var(--node-color) 50%, transparent);
  background: color-mix(in srgb, var(--node-color) 14%, transparent);
}

.ov__row-indent {
  width: calc(var(--depth) * 0.95rem);
  flex: 0 0 auto;
}

.ov__toggle {
  width: 1.1rem;
  height: 1.1rem;
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  padding: 0;
  cursor: pointer;
}

.ov__toggle--ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.ov__type-dot {
  width: 0.62rem;
  height: 0.62rem;
  border-radius: 999px;
  background: var(--node-color);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--node-color) 18%, transparent);
  flex: 0 0 auto;
}

.ov__row-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 0.05rem;
}

.ov__row-name {
  font-size: 0.88rem;
  font-weight: 650;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ov__row-meta {
  font-size: 0.73rem;
  color: var(--color-text-muted);
}

.ov__row-counters {
  margin-left: auto;
  display: flex;
  gap: 0.3rem;
}

.ov__badge {
  font-size: 0.7rem;
  padding: 0.16rem 0.42rem;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--color-text) 14%, transparent);
  background: color-mix(in srgb, var(--color-bg) 70%, transparent);
}

.ov__detail-hero {
  margin: 0.9rem;
  border-radius: 0.8rem;
  border: 1px solid;
  overflow: hidden;
  background: color-mix(in srgb, var(--color-surface) 95%, transparent);
}

.ov__detail-band {
  height: 0.5rem;
}

.ov__detail-body {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.ov__detail-type {
  width: fit-content;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.18rem 0.45rem;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--color-text) 14%, transparent);
  color: var(--color-text-muted);
}

.ov__detail-name {
  margin: 0;
  font-size: 1.22rem;
}

.ov__detail-path {
  margin: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 0.8rem;
  color: var(--color-text-muted);
  overflow-wrap: anywhere;
}

.ov__crumbs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.ov__crumb {
  font-size: 0.72rem;
  padding: 0.15rem 0.44rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-bg) 66%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-text) 12%, transparent);
}

.ov__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.5rem;
}

.ov__stat {
  border: 1px solid color-mix(in srgb, var(--color-text) 10%, transparent);
  border-radius: 0.6rem;
  padding: 0.55rem;
  background: color-mix(in srgb, var(--color-bg) 58%, transparent);
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.ov__stat-label {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.ov__stat-value {
  font-size: 1.05rem;
  font-weight: 700;
}

.ov__actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.ov__btn {
  border: 1px solid transparent;
  border-radius: 0.5rem;
  padding: 0.5rem 0.8rem;
  font-size: 0.86rem;
  font-weight: 600;
  cursor: pointer;
}

.ov__btn--detail {
  background: color-mix(in srgb, var(--color-primary, #3b82f6) 11%, transparent);
  border-color: color-mix(in srgb, var(--color-primary, #3b82f6) 33%, transparent);
  color: var(--color-text);
}

.ov__btn--edit {
  background: var(--color-primary, #3b82f6);
  color: #fff;
}

.ov__btn--manage {
  background: color-mix(in srgb, var(--color-primary, #3b82f6) 11%, transparent);
  border-color: color-mix(in srgb, var(--color-primary, #3b82f6) 33%, transparent);
  color: var(--color-text);
}

.ov__btn--danger {
  background: color-mix(in srgb, #dc2626 14%, transparent);
  border-color: color-mix(in srgb, #dc2626 35%, transparent);
  color: #dc2626;
}

.ov__manage-card {
  border: 1px solid color-mix(in srgb, var(--color-text) 12%, transparent);
  border-radius: 0.7rem;
  background: color-mix(in srgb, var(--color-bg) 65%, transparent);
  padding: 0.8rem;
  display: grid;
  gap: 0.6rem;
}

.ov__manage-card h5 {
  margin: 0;
  font-size: 0.9rem;
}

.ov__manage-field {
  display: grid;
  gap: 0.2rem;
}

.ov__manage-label {
  font-size: 0.8rem;
  font-weight: 600;
}

.ov__manage-select {
  border: 1px solid color-mix(in srgb, var(--color-text) 16%, transparent);
  background: color-mix(in srgb, var(--color-surface) 90%, transparent);
  border-radius: 0.45rem;
  padding: 0.45rem 0.55rem;
  color: var(--color-text);
  font: inherit;
}

.ov__manage-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.ov__hint {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  padding: 0.7rem;
}

.ov__create-top-card {
  border: 1px solid color-mix(in srgb, var(--color-primary, #3b82f6) 35%, transparent);
  border-radius: 0.8rem;
  background: color-mix(in srgb, var(--color-surface) 96%, transparent);
  padding: 0.9rem;
  display: grid;
  gap: 0.65rem;
}

.ov__create-top-card h5 {
  margin: 0;
  font-size: 0.95rem;
}

.ov__create-top-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr 0.8fr;
  gap: 0.65rem;
}

.ov__error {
  color: var(--color-error, #dc2626);
  font-size: 0.87rem;
}

@media (max-width: 1024px) {
  .ov__workspace {
    grid-template-columns: minmax(320px, 1fr) minmax(320px, 1fr);
  }

  .ov__search {
    width: 100%;
    min-width: 220px;
  }
}

@media (max-width: 768px) {
  .ov__topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .ov__topbar-meta {
    flex-direction: column;
    align-items: stretch;
  }

  .ov__search {
    width: 100%;
  }

  .ov__workspace {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .ov__create-top-grid {
    grid-template-columns: 1fr;
  }

  .ov__rows {
    max-height: 46vh;
  }

  .ov__stats {
    grid-template-columns: 1fr;
  }

  .ov__actions,
  .ov__manage-actions {
    justify-content: stretch;
  }

  .ov__btn,
  .ov__actions :deep(button),
  .ov__manage-actions :deep(button) {
    width: 100%;
    text-align: center;
  }

  .ov__row {
    flex-wrap: wrap;
    row-gap: 0.35rem;
  }

  .ov__row-main {
    flex: 1;
    min-width: 170px;
  }

  .ov__row-counters {
    width: 100%;
    margin-left: 0;
    justify-content: flex-start;
  }
}

@media (max-width: 520px) {
  .ov {
    gap: 0.8rem;
  }

  .ov__title {
    font-size: 1.05rem;
  }

  .ov__detail-name {
    font-size: 1.05rem;
  }

  .ov__detail-body {
    padding: 0.85rem;
  }

  .ov__badge {
    font-size: 0.66rem;
    padding: 0.12rem 0.35rem;
  }
}
</style>
