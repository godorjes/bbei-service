<script setup>
import { onBeforeUnmount, reactive, ref, watch } from 'vue'
import { itemsApi } from '../api'
import IconSymbol from './IconSymbol.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  item: { type: Object, default: null },
  sections: { type: Array, default: () => [] },
  initialSectionId: { type: String, default: '' },
  listId: { type: [Number, String], default: null },
})

const emit = defineEmits(['close', 'save', 'delete'])
const suggestions = ref([])
const searching = ref(false)
const searchError = ref('')
const selectedSuggestionName = ref('')
let searchTimer
let searchRequest = 0


const form = reactive({
  name: '',
  quantity: 1,
  note: '',
  sectionId: '',
})

watch(
  () => [props.open, props.item, props.initialSectionId],
  () => {
    if (!props.open) return
    form.name = props.item?.name || ''
    form.quantity = props.item?.quantity || 1
    form.note = props.item?.note || ''
    form.sectionId = props.item?.sectionId || props.initialSectionId || props.sections[0]?.id || ''
    suggestions.value = []
    selectedSuggestionName.value = ''
    searchError.value = ''
  },
  { immediate: true },
)

watch(
  () => form.name,
  (value) => {
    clearTimeout(searchTimer)
    const query = value.trim()
    if (!props.open || props.item || !query || selectedSuggestionName.value === query) {
      suggestions.value = []
      searching.value = false
      return
    }
    selectedSuggestionName.value = ''
    const requestId = ++searchRequest
    searching.value = true
    searchTimer = setTimeout(async () => {
      searching.value = true
      searchError.value = ''
      try {
        const result = await itemsApi.search(query, props.listId)
        if (requestId === searchRequest) suggestions.value = result
      } catch (exception) {
        if (requestId === searchRequest) searchError.value = exception.message
      } finally {
        if (requestId === searchRequest) searching.value = false
      }
    }, 180)
  },
)

onBeforeUnmount(() => clearTimeout(searchTimer))

function useSuggestion(suggestion) {
  form.name = suggestion.name
  form.quantity = suggestion.quantity || 1
  form.note = suggestion.note || ''
  const matchingSection = props.sections.find((section) => section.title === suggestion.sectionTitle)
  if (matchingSection) form.sectionId = matchingSection.id
  selectedSuggestionName.value = suggestion.name
  suggestions.value = []
}

function submit() {
  if (!form.name.trim()) return
  emit('save', {
    name: form.name.trim(),
    quantity: Math.max(1, Number(form.quantity) || 1),
    note: form.note.trim() || null,
    sectionId: form.sectionId,
  })
}
</script>

<template>
  <Teleport to="body">
    <Transition name="sheet">
      <div v-if="open" class="sheet-backdrop" @click.self="emit('close')">
        <section class="bottom-sheet" role="dialog" aria-modal="true" :aria-label="item ? '编辑物品' : '添加物品'">
          <div class="sheet-handle" />
          <div class="sheet-header">
            <h2>{{ item ? '编辑物品' : '添加物品' }}</h2>
            <button class="icon-button" type="button" aria-label="关闭" @click="emit('close')">×</button>
          </div>

          <label class="field item-name-field">
            <span>{{ item ? '物品名称' : '搜索或新增物品' }}</span>
            <input v-model="form.name" autofocus maxlength="50" placeholder="例如：充电器" @keyup.enter="submit" />
          </label>

          <div v-if="!item && (searching || suggestions.length || (form.name.trim() && !searchError))" class="reuse-panel">
            <div class="reuse-panel__heading">
              <strong>以前用过</strong>
              <span v-if="searching" class="spinner spinner--small" />
            </div>
            <button
              v-for="suggestion in suggestions"
              :key="`${suggestion.name}-${suggestion.source}`"
              class="reuse-row"
              type="button"
              @click="useSuggestion(suggestion)"
            >
              <span>
                <strong>{{ suggestion.name }}</strong>
                <small>{{ suggestion.source }}<template v-if="suggestion.note"> · {{ suggestion.note }}</template></small>
              </span>
              <em>× {{ suggestion.quantity || 1 }}</em>
            </button>
            <p v-if="!searching && !suggestions.length" class="reuse-empty">没有找到，保存后将作为本次物品。</p>
          </div>
          <p v-if="searchError" class="inline-error">{{ searchError }}</p>
          <p v-if="!item" class="temporary-note"><span>本次</span> 默认只加入这次清单，完成时再决定是否写回模板。</p>

          <div class="field-row">
            <label class="field field--quantity">
              <span>数量</span>
              <input v-model.number="form.quantity" type="number" min="1" max="99" inputmode="numeric" />
            </label>
            <label class="field">
              <span>分组</span>
              <select v-model="form.sectionId">
                <option v-for="section in sections" :key="section.id" :value="section.id">
                  {{ section.title }}
                </option>
              </select>
            </label>
          </div>

          <label class="field">
            <span>备注（可选）</span>
            <input v-model="form.note" maxlength="100" placeholder="例如：放入电脑包" />
          </label>

          <button class="button button--primary button--full" type="button" @click="submit">
            {{ item ? '保存修改' : '添加到本次清单' }}
          </button>
          <button v-if="item" class="button button--danger-ghost button--full" type="button" @click="emit('delete')">
            <IconSymbol name="trash" :size="18" />
            仅从本次清单移除
          </button>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>
