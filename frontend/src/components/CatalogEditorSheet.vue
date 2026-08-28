<script setup>
import { computed, reactive, watch } from 'vue'
import IconSymbol from './IconSymbol.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  type: { type: String, required: true },
  entity: { type: Object, default: null },
  scenes: { type: Array, default: () => [] },
  sections: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'save', 'delete'])
const form = reactive({ name: '', selectedIds: [] })
const isSection = computed(() => props.type === 'section')
const options = computed(() => (isSection.value ? props.scenes : props.sections))

watch(() => [props.open, props.entity, props.type], () => {
  if (!props.open) return
  form.name = props.entity?.name || ''
  form.selectedIds = [...(isSection.value ? props.entity?.sceneIds || [] : props.entity?.sectionIds || [])]
}, { immediate: true })

function toggle(id) {
  form.selectedIds = form.selectedIds.includes(id)
    ? form.selectedIds.filter((value) => value !== id)
    : [...form.selectedIds, id]
}

function submit() {
  const name = form.name.trim()
  if (!name || props.saving) return
  emit('save', isSection.value
    ? { name, sceneIds: [...form.selectedIds] }
    : { name, sectionIds: [...form.selectedIds] })
}
</script>

<template>
  <Teleport to="body">
    <Transition name="sheet">
      <div v-if="open" class="sheet-backdrop" @click.self="emit('close')">
        <form class="bottom-sheet editor-sheet" @submit.prevent="submit">
          <div class="sheet-handle" />
          <div class="sheet-header">
            <div><small>{{ isSection ? '独立分区' : '独立物品' }}</small><h2>{{ entity ? '编辑' : '新建' }}{{ isSection ? '分区' : '物品' }}</h2></div>
            <button class="icon-button" type="button" aria-label="关闭" @click="emit('close')">×</button>
          </div>
          <label class="field">
            <span>{{ isSection ? '分区名称' : '物品名称' }}</span>
            <input v-model="form.name" autofocus :maxlength="isSection ? 30 : 50" :placeholder="isSection ? '例如：洗漱' : '例如：充电器'" />
          </label>
          <div class="binding-heading">
            <div><strong>{{ isSection ? '出现在哪些场景' : '绑定到哪些分区' }}</strong><span>可以暂时不绑定</span></div>
            <em>{{ form.selectedIds.length }} 个</em>
          </div>
          <div v-if="options.length" class="choice-list choice-list--compact">
            <label v-for="option in options" :key="option.id" class="choice-row">
              <input type="checkbox" :checked="form.selectedIds.includes(option.id)" @change="toggle(option.id)" />
              <span class="choice-check">✓</span>
              <span class="choice-copy"><strong>{{ option.name }}</strong></span>
            </label>
          </div>
          <p v-else class="sheet-empty">暂时没有可绑定内容，也可以先保存。</p>
          <button class="button button--primary button--full button--large" type="submit" :disabled="saving || !form.name.trim()">
            {{ saving ? '正在保存…' : '保存' }}
          </button>
          <button v-if="entity" class="button button--danger-ghost button--full" type="button" :disabled="saving" @click="emit('delete', entity)">
            <IconSymbol name="trash" :size="18" /> 删除{{ isSection ? '分区' : '物品' }}
          </button>
        </form>
      </div>
    </Transition>
  </Teleport>
</template>
