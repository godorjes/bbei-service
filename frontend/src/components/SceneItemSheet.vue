<script setup>
import { computed, reactive, ref, watch } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  sections: { type: Array, default: () => [] },
  items: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'save'])
const query = ref('')
const selectedItemId = ref(null)
const form = reactive({ sectionIds: [] })

const suggestions = computed(() => {
  const value = query.value.trim().toLocaleLowerCase()
  if (!value) return []
  return props.items.filter((item) => item.name.toLocaleLowerCase().includes(value)).slice(0, 6)
})

watch(() => props.open, (open) => {
  if (!open) return
  query.value = ''
  selectedItemId.value = null
  form.sectionIds = props.sections[0] ? [props.sections[0].id] : []
}, { immediate: true })

function toggleSection(id) {
  form.sectionIds = form.sectionIds.includes(id)
    ? form.sectionIds.filter((value) => value !== id)
    : [...form.sectionIds, id]
}

function choose(item) {
  selectedItemId.value = item.id
  query.value = item.name
}

function submit() {
  const name = query.value.trim()
  if (!name || !form.sectionIds.length || props.saving) return
  const exact = props.items.find((item) => item.name.trim().toLocaleLowerCase() === name.toLocaleLowerCase())
  emit('save', { existingItem: exact || null, name, sectionIds: [...form.sectionIds] })
}
</script>

<template>
  <Teleport to="body">
    <Transition name="sheet">
      <div v-if="open" class="sheet-backdrop" @click.self="emit('close')">
        <form class="bottom-sheet editor-sheet" @submit.prevent="submit">
          <div class="sheet-handle" />
          <div class="sheet-header">
            <div><small>从物品库添加</small><h2>添加物品</h2></div>
            <button class="icon-button" type="button" aria-label="关闭" @click="emit('close')">×</button>
          </div>
          <label class="field">
            <span>搜索或输入物品名称</span>
            <input v-model="query" autofocus maxlength="50" placeholder="例如：充电器" @input="selectedItemId = null" />
          </label>
          <div v-if="suggestions.length" class="suggestion-list">
            <button v-for="item in suggestions" :key="item.id" type="button" :class="{ selected: selectedItemId === item.id }" @click="choose(item)">
              <span><strong>{{ item.name }}</strong><small>{{ item.sectionNames.length ? item.sectionNames.join('、') : '未绑定分区' }}</small></span><em>使用</em>
            </button>
          </div>
          <p v-else-if="query.trim()" class="new-item-note">没有同名物品，保存后会新建。</p>
          <div class="binding-heading"><div><strong>加入哪些分区</strong><span>可多选，场景内仍只显示一次</span></div></div>
          <div class="choice-list choice-list--compact">
            <label v-for="section in sections" :key="section.id" class="choice-row">
              <input type="checkbox" :checked="form.sectionIds.includes(section.id)" @change="toggleSection(section.id)" />
              <span class="choice-check">✓</span><span class="choice-copy"><strong>{{ section.name }}</strong></span>
            </label>
          </div>
          <button class="button button--primary button--full button--large" type="submit" :disabled="saving || !query.trim() || !form.sectionIds.length">
            {{ saving ? '正在添加…' : '添加到场景' }}
          </button>
        </form>
      </div>
    </Transition>
  </Teleport>
</template>
