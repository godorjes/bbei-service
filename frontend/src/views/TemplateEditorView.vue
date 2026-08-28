<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import IconSymbol from '../components/IconSymbol.vue'
import LoadingState from '../components/LoadingState.vue'
import { templatesApi } from '../api'
import { makeId, normalizeItemName } from '../utils'

const route = useRoute()
const router = useRouter()
const templateId = computed(() => route.params.id ? Number(route.params.id) : null)
const loading = ref(Boolean(templateId.value))
const saving = ref(false)
const error = ref('')
const newGroupName = ref('')
const newItemNames = reactive({})
const model = reactive({
  name: '',
  icon: 'suitcase',
  pinned: true,
  sections: [{ id: makeId(), title: '未分组', items: [] }],
})
const iconOptions = [
  { value: 'suitcase', label: '旅行箱' },
  { value: 'briefcase', label: '公文包' },
  { value: 'backpack', label: '背包' },
]

onMounted(async () => {
  if (!templateId.value) return
  try {
    const data = await templatesApi.get(templateId.value)
    model.name = data.name
    model.icon = data.icon
    model.pinned = data.pinned
    model.sections = data.sections
  } catch (exception) {
    error.value = exception.message
  } finally {
    loading.value = false
  }
})

function addGroup() {
  const title = newGroupName.value.trim()
  if (!title) return
  if (model.sections.some((section) => section.title === title)) {
    error.value = `“${title}”分组已存在`
    return
  }
  model.sections.push({ id: makeId(), title, items: [] })
  newGroupName.value = ''
  error.value = ''
}

function removeGroup(index) {
  const section = model.sections[index]
  if (section.items.length && !window.confirm(`“${section.title}”中还有物品，确定移除整个分组吗？`)) return
  model.sections.splice(index, 1)
  if (!model.sections.length) model.sections.push({ id: makeId(), title: '未分组', items: [] })
}

function addItem(section) {
  const name = (newItemNames[section.id] || '').trim()
  if (!name) return
  const duplicate = model.sections.flatMap((group) => group.items)
    .some((item) => normalizeItemName(item.name) === normalizeItemName(name))
  if (duplicate) {
    error.value = `“${name}”已经在模板中`
    return
  }
  section.items.push({
    id: makeId(),
    name,
    quantity: 1,
    note: null,
    checked: false,
    temporary: false,
  })
  newItemNames[section.id] = ''
  error.value = ''
}

function removeItem(section, index) {
  section.items.splice(index, 1)
}

async function save() {
  if (!model.name.trim()) {
    error.value = '请输入模板名称'
    return
  }
  const names = model.sections.flatMap((section) => section.items.map((item) => normalizeItemName(item.name)).filter(Boolean))
  if (new Set(names).size !== names.length) {
    error.value = '模板中存在同名物品，请合并后再保存'
    return
  }
  saving.value = true
  error.value = ''
  try {
    const payload = {
      name: model.name.trim(),
      icon: model.icon,
      pinned: model.pinned,
      sections: model.sections.map((section) => ({
        ...section,
        title: section.title.trim() || '未分组',
        items: section.items.filter((item) => item.name.trim()).map((item) => ({
          ...item,
          name: item.name.trim(),
          quantity: Math.max(1, Number(item.quantity) || 1),
          note: item.note?.trim() || null,
        })),
      })),
    }
    if (templateId.value) await templatesApi.update(templateId.value, payload)
    else await templatesApi.create(payload)
    await router.replace('/templates')
  } catch (exception) {
    error.value = exception.message
    saving.value = false
  }
}
</script>

<template>
  <main class="app-page template-editor-page">
    <header class="page-header">
      <button class="icon-button" type="button" aria-label="返回" @click="router.back()">
        <IconSymbol name="back" />
      </button>
      <h1>{{ templateId ? '编辑模板' : '新建模板' }}</h1>
      <button class="header-text-button" type="button" :disabled="saving" @click="save">{{ saving ? '保存中' : '保存' }}</button>
    </header>

    <LoadingState v-if="loading" />
    <form v-else class="template-form" @submit.prevent="save">
      <div v-if="error" class="notice notice--error notice--sticky">
        <span>{{ error }}</span>
        <button type="button" @click="error = ''">关闭</button>
      </div>

      <section class="form-card">
        <label class="field">
          <span>模板名称</span>
          <input v-model="model.name" maxlength="50" placeholder="例如：周末旅行" />
        </label>
        <div class="field">
          <span>图标</span>
          <div class="icon-options">
            <button
              v-for="option in iconOptions"
              :key="option.value"
              type="button"
              :class="{ active: model.icon === option.value }"
              :aria-label="option.label"
              @click="model.icon = option.value"
            >
              <IconSymbol :name="option.value" />
            </button>
          </div>
        </div>
        <label class="switch-row switch-row--plain">
          <span>
            <strong>显示在常用模板</strong>
            <small>首页最多突出展示最近的常用模板</small>
          </span>
          <input v-model="model.pinned" type="checkbox" />
          <i />
        </label>
      </section>

      <div class="section-heading section-heading--editor">
        <div>
          <h2>物品与分组</h2>
          <p>分组只属于当前模板，不会变成全局分类。</p>
        </div>
      </div>

      <section v-for="(section, sectionIndex) in model.sections" :key="section.id" class="editor-section-card">
        <div class="editor-section-card__header">
          <input v-model="section.title" maxlength="30" aria-label="分组名称" />
          <button class="icon-button icon-button--danger" type="button" aria-label="删除分组" @click="removeGroup(sectionIndex)">
            <IconSymbol name="trash" :size="17" />
          </button>
        </div>

        <div class="editor-items">
          <article v-for="(item, itemIndex) in section.items" :key="item.id" class="editor-item">
            <input v-model="item.name" maxlength="50" aria-label="物品名称" />
            <div class="editor-item__details">
              <label>
                <span>数量</span>
                <input v-model.number="item.quantity" type="number" min="1" max="99" inputmode="numeric" />
              </label>
              <input v-model="item.note" maxlength="100" aria-label="备注" placeholder="备注（可选）" />
              <button type="button" aria-label="删除物品" @click="removeItem(section, itemIndex)">×</button>
            </div>
          </article>
        </div>

        <div class="inline-add-row">
          <input
            v-model="newItemNames[section.id]"
            maxlength="50"
            placeholder="添加物品"
            @keyup.enter.prevent="addItem(section)"
          />
          <button type="button" :aria-label="`添加到${section.title}`" @click="addItem(section)">
            <IconSymbol name="plus" :size="18" />
          </button>
        </div>
      </section>

      <div class="new-group-row">
        <input v-model="newGroupName" maxlength="30" placeholder="新分组名称" @keyup.enter.prevent="addGroup" />
        <button class="button button--outline" type="button" @click="addGroup">
          <IconSymbol name="plus" :size="18" /> 添加分组
        </button>
      </div>

      <button class="button button--primary button--full button--large" type="submit" :disabled="saving">
        {{ saving ? '正在保存…' : '保存模板' }}
      </button>
    </form>
  </main>
</template>
