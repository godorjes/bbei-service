<script setup>
import { computed, onMounted, ref } from 'vue'
import BottomNav from '../components/BottomNav.vue'
import CatalogEditorSheet from '../components/CatalogEditorSheet.vue'
import EmptyState from '../components/EmptyState.vue'
import IconSymbol from '../components/IconSymbol.vue'
import LoadingState from '../components/LoadingState.vue'
import { itemsApi, scenesApi, sectionsApi } from '../api'

const loading = ref(true)
const saving = ref(false)
const error = ref('')
const tab = ref('sections')
const scenes = ref([])
const sections = ref([])
const items = ref([])
const query = ref('')
const unboundOnly = ref(false)
const editorOpen = ref(false)
const editingEntity = ref(null)

const unboundCount = computed(() => items.value.filter((item) => !item.sectionIds.length).length)
const visibleItems = computed(() => {
  const value = query.value.trim().toLocaleLowerCase()
  return items.value.filter((item) => (!unboundOnly.value || !item.sectionIds.length) && (!value || item.name.toLocaleLowerCase().includes(value)))
})

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    ;[scenes.value, sections.value, items.value] = await Promise.all([
      scenesApi.list(), sectionsApi.list(), itemsApi.list(),
    ])
  } catch (exception) {
    error.value = exception.message
  } finally {
    loading.value = false
  }
}

function openEditor(entity = null) {
  editingEntity.value = entity
  editorOpen.value = true
}

async function saveEntity(payload) {
  saving.value = true
  error.value = ''
  try {
    const api = tab.value === 'sections' ? sectionsApi : itemsApi
    if (editingEntity.value) await api.update(editingEntity.value.id, payload)
    else await api.create(payload)
    editorOpen.value = false
    editingEntity.value = null
    await load()
  } catch (exception) {
    error.value = exception.message
  } finally {
    saving.value = false
  }
}

async function deleteEntity(entity) {
  const isSection = tab.value === 'sections'
  const consequence = isSection
    ? '其中物品会保留，没有其他分区的物品将变成未绑定。'
    : '它会从所有分区和场景中移除。'
  if (!window.confirm(`删除“${entity.name}”？${consequence}`)) return
  saving.value = true
  try {
    await (isSection ? sectionsApi : itemsApi).remove(entity.id)
    editorOpen.value = false
    editingEntity.value = null
    await load()
  } catch (exception) {
    error.value = exception.message
  } finally {
    saving.value = false
  }
}

function switchTab(value) {
  tab.value = value
  query.value = ''
  unboundOnly.value = false
  editingEntity.value = null
}
</script>

<template>
  <main class="app-page app-page--with-nav organize-page">
    <header class="option-header organize-header">
      <div><p class="eyebrow">平时无需进入</p><h1>整理</h1><span>需要维护内容时再来</span></div>
      <button class="icon-button icon-button--accent" type="button" :aria-label="tab === 'sections' ? '新建分区' : '新建物品'" @click="openEditor()"><IconSymbol name="plus" /></button>
    </header>

    <div class="organize-tabs">
      <button type="button" :class="{ active: tab === 'sections' }" @click="switchTab('sections')">分区</button>
      <button type="button" :class="{ active: tab === 'items' }" @click="switchTab('items')">物品</button>
    </div>

    <LoadingState v-if="loading" />
    <template v-else>
      <div v-if="error" class="notice notice--error"><span>{{ error }}</span><button type="button" @click="load">重试</button></div>

      <template v-if="tab === 'sections'">
        <section v-if="sections.length" class="manage-stack-v2">
          <button v-for="section in sections" :key="section.id" class="manage-row-v2" type="button" @click="openEditor(section)">
            <span class="manage-mark">区</span>
            <span class="manage-copy"><strong>{{ section.name }}</strong><small>{{ section.itemCount }} 件物品</small><em :class="{ unbound: !section.sceneNames.length }">{{ section.sceneNames.length ? section.sceneNames.join('、') : '暂未绑定场景' }}</em></span>
            <IconSymbol name="chevron" :size="18" />
          </button>
        </section>
        <EmptyState v-else title="还没有分区" description="分区用于把较长的清单切成几段，可以被多个场景复用。">
          <button class="button button--primary" type="button" @click="openEditor()">新建分区</button>
        </EmptyState>
      </template>

      <template v-else>
        <div class="item-tools-v2">
          <label class="search-field"><span>⌕</span><input v-model="query" placeholder="搜索物品" /></label>
          <button v-if="unboundCount" class="quiet-hint" :class="{ active: unboundOnly }" type="button" @click="unboundOnly = !unboundOnly">
            <i />{{ unboundCount }} 个未绑定
          </button>
        </div>
        <section v-if="visibleItems.length" class="manage-stack-v2">
          <button v-for="item in visibleItems" :key="item.id" class="manage-row-v2" type="button" @click="openEditor(item)">
            <span class="manage-mark" :class="{ 'manage-mark--unbound': !item.sectionIds.length }">{{ item.sectionIds.length ? '物' : '' }}</span>
            <span class="manage-copy"><strong>{{ item.name }}</strong><em :class="{ unbound: !item.sectionNames.length }">{{ item.sectionNames.length ? item.sectionNames.join('、') : '未绑定分区' }}</em></span>
            <IconSymbol name="chevron" :size="18" />
          </button>
        </section>
        <EmptyState v-else :title="query || unboundOnly ? '没有符合条件的物品' : '还没有物品'" description="物品只创建一次，可以绑定到多个分区。">
          <button v-if="!query && !unboundOnly" class="button button--primary" type="button" @click="openEditor()">新建物品</button>
        </EmptyState>
      </template>
    </template>

    <CatalogEditorSheet
      :open="editorOpen"
      :type="tab === 'sections' ? 'section' : 'item'"
      :entity="editingEntity"
      :scenes="scenes"
      :sections="sections"
      :saving="saving"
      @close="editorOpen = false"
      @save="saveEntity"
      @delete="deleteEntity"
    />
    <BottomNav active="organize" />
  </main>
</template>
