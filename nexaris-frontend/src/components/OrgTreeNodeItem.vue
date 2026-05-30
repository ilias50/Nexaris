<script setup lang="ts">
import { type PropType } from 'vue'
import BaseButton from '@/components/BaseButton.vue'
import BaseInput from '@/components/BaseInput.vue'
import { useI18n } from '@/i18n'
import type { OrgTreeNode } from '@/types/domain'

const props = defineProps({
  node: { type: Object as PropType<OrgTreeNode>, required: true },
  selectedId: { type: Number as PropType<number | null>, default: null },
  showAddFormFor: { type: [String, Number] as PropType<'root' | number | null>, default: null },
  newNodeName: { type: String, default: '' },
  newNodeType: { type: String, default: 'DEPARTMENT' },
  nodeTypes: { type: Array as PropType<string[]>, default: () => [] },
  addNodeLoading: { type: Boolean, default: false },
  addNodeError: { type: String, default: '' },
})

const emit = defineEmits<{
  (e: 'select', node: OrgTreeNode): void
  (e: 'open-add', id: number): void
  (e: 'submit-add'): void
  (e: 'cancel-add'): void
  (e: 'delete', id: number): void
  (e: 'update:new-node-name', val: string): void
  (e: 'update:new-node-type', val: string): void
}>()

const { t } = useI18n()
</script>

<template>
  <li class="org-tree__item">
    <div :class="['org-tree__node', selectedId === node.id && 'org-tree__node--active']">
      <button class="org-tree__node-name" @click="emit('select', node)">
        {{ node.name }}
        <span class="org-tree__node-type">{{ node.nodeType }}</span>
      </button>
      <div class="org-tree__node-actions">
        <button class="org-tree__btn" :title="t('adminOrg.addChildNode')" @click="emit('open-add', node.id)">＋</button>
        <button class="org-tree__btn org-tree__btn--del" :title="t('adminOrg.deleteNode')" @click="emit('delete', node.id)">✕</button>
      </div>
    </div>

    <!-- Add child form -->
    <div v-if="showAddFormFor === node.id" class="org-add-form org-add-form--child">
      <BaseInput
        :model-value="newNodeName"
        :label="t('adminOrg.nodeNameLabel')"
        type="text"
        :placeholder="t('adminOrg.nodeNamePlaceholder')"
        @update:model-value="emit('update:new-node-name', $event as string)"
      />
      <div class="org-select-field">
        <label class="org-select-label">{{ t('adminOrg.nodeTypeLabel') }}</label>
        <select
          :value="newNodeType"
          class="org-select"
          @change="emit('update:new-node-type', ($event.target as HTMLSelectElement).value)"
        >
          <option v-for="type in nodeTypes" :key="type" :value="type">{{ type }}</option>
        </select>
      </div>
      <p v-if="addNodeError" class="org-error">{{ addNodeError }}</p>
      <div class="org-add-form__actions">
        <BaseButton type="button" size="sm" variant="ghost" @click="emit('cancel-add')">✕</BaseButton>
        <BaseButton type="button" size="sm" :loading="addNodeLoading" @click="emit('submit-add')">✓</BaseButton>
      </div>
    </div>

    <!-- Recursive children -->
    <ul v-if="node.children && node.children.length" class="org-tree org-tree--nested">
      <OrgTreeNodeItem
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :selected-id="selectedId"
        :show-add-form-for="showAddFormFor"
        :new-node-name="newNodeName"
        :new-node-type="newNodeType"
        :node-types="nodeTypes"
        :add-node-loading="addNodeLoading"
        :add-node-error="addNodeError"
        @select="emit('select', $event)"
        @open-add="emit('open-add', $event)"
        @submit-add="emit('submit-add')"
        @cancel-add="emit('cancel-add')"
        @delete="emit('delete', $event)"
        @update:new-node-name="emit('update:new-node-name', $event)"
        @update:new-node-type="emit('update:new-node-type', $event)"
      />
    </ul>
  </li>
</template>

<style scoped>
.org-tree {
  list-style: none;
  padding: 0;
  margin: 0;
}

.org-tree--nested {
  padding-left: 1.1rem;
  border-left: 2px solid rgba(148, 163, 184, 0.2);
  margin-left: 0.5rem;
  margin-top: 0.2rem;
}

.org-tree__item {
  margin: 0.15rem 0;
}

.org-tree__node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: var(--radius-sm);
  padding: 0.3rem 0.5rem;
  transition: background 0.15s;
}

.org-tree__node:hover {
  background: rgba(148, 163, 184, 0.1);
}

.org-tree__node--active {
  background: rgba(59, 130, 246, 0.1);
}

.org-tree__node-name {
  background: none;
  border: none;
  cursor: pointer;
  font: inherit;
  color: var(--color-text);
  text-align: left;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.org-tree__node-type {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  background: rgba(148, 163, 184, 0.15);
  border-radius: 999px;
  padding: 0.1rem 0.45rem;
}

.org-tree__node-actions {
  display: flex;
  gap: 0.25rem;
  opacity: 0;
  transition: opacity 0.15s;
}

.org-tree__node:hover .org-tree__node-actions {
  opacity: 1;
}

.org-tree__btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.85rem;
  color: var(--color-text-muted);
  padding: 0.15rem 0.3rem;
  border-radius: var(--radius-sm);
}

.org-tree__btn:hover {
  background: rgba(148, 163, 184, 0.2);
}

.org-tree__btn--del:hover {
  color: var(--color-error, #dc2626);
}

.org-add-form {
  background: rgba(248, 250, 252, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: var(--radius-sm);
  padding: 0.75rem;
  margin-bottom: 0.75rem;
  display: grid;
  gap: 0.55rem;
}

.org-add-form--child {
  margin-left: 0.5rem;
  margin-top: 0.35rem;
}

.org-add-form__actions {
  display: flex;
  gap: 0.4rem;
  justify-content: flex-end;
}

.org-select-field {
  display: grid;
  gap: 0.25rem;
}

.org-select-label {
  font-size: 0.85rem;
  font-weight: 600;
}

.org-select {
  border: 1px solid rgba(148, 163, 184, 0.45);
  border-radius: var(--radius-sm);
  padding: 0.5rem 0.65rem;
  font: inherit;
  color: var(--color-text);
  background: var(--color-surface);
  width: 100%;
}

.org-select:focus {
  outline: none;
  border-color: rgba(59, 130, 246, 0.75);
}

.org-error {
  color: var(--color-error, #dc2626);
  font-size: 0.88rem;
}
</style>
