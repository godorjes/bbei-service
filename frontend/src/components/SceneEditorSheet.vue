<script setup>
import { reactive, watch } from 'vue'
import { moveId } from '../catalog'
import IconSymbol from './IconSymbol.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  scene: { type: Object, default: null },
  sections: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'save', 'delete'])
const form = reactive({ name: '', sectionIds: [] })

watch(() => [props.open, props.scene], () => {
  if (!props.open) return
  form.name = props.scene?.name || ''
  form.sectionIds = [...(props.scene?.sectionIds || [])]
}, { immediate: true })

function toggleSection(id) {
  form.sectionIds = form.sectionIds.includes(id)
    ? form.sectionIds.filter((value) => value !== id)
    : [...form.sectionIds, id]
}

function submit() {
  const name = form.name.trim()
  if (name && !props.saving) emit('save', { name, sectionIds: [...form.sectionIds] })
}
</script>

<template>
  <Teleport to="body">
    <Transition name="sheet">
      <div v-if="open" class="sheet-backdrop" @click.self="emit('close')">
        <form class="bottom-sheet editor-sheet" @submit.prevent="submit">
          <div class="sheet-handle" />
          <div class="sheet-header">
            <div><small>场景与分区</small><h2>{{ scene ? '编辑场景' : '新建场景' }}</h2></div>
            <button class="icon-button" type="button" aria-label="关闭" @click="emit('close')">×</button>
          </div>
          <label class="field">
            <span>场景名称</span>
            <input v-model="form.name" autofocus maxlength="50" placeholder="例如：周末旅行" />
          </label>
          <div class="binding-heading">
            <div><strong>使用哪些分区</strong><span>选中后可以调整显示顺序</span></div>
            <em>{{ form.sectionIds.length }} 个</em>
          </div>
          <div v-if="sections.length" class="choice-list">
            <label v-for="section in sections" :key="section.id" class="choice-row">
              <input type="checkbox" :checked="form.sectionIds.includes(section.id)" @change="toggleSection(section.id)" />
              <span class="choice-check">✓</span>
              <span class="choice-copy"><strong>{{ section.name }}</strong><small>{{ section.itemCount }} 件物品</small></span>
              <span v-if="form.sectionIds.includes(section.id)" class="reorder-buttons">
                <button type="button" aria-label="上移" @click.prevent="form.sectionIds = moveId(form.sectionIds, section.id, -1)">↑</button>
                <button type="button" aria-label="下移" @click.prevent="form.sectionIds = moveId(form.sectionIds, section.id, 1)">↓</button>
              </span>
            </label>
          </div>
          <p v-else class="sheet-empty">还没有分区，可以先保存场景，再到“整理”中创建。</p>
          <button class="button button--primary button--full button--large" type="submit" :disabled="saving || !form.name.trim()">
            {{ saving ? '正在保存…' : '保存场景' }}
          </button>
          <button v-if="scene" class="button button--danger-ghost button--full" type="button" :disabled="saving" @click="emit('delete', scene)">
            <IconSymbol name="trash" :size="18" /> 删除场景
          </button>
        </form>
      </div>
    </Transition>
  </Teleport>
</template>
