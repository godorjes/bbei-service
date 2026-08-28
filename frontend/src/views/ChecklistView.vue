<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import IconSymbol from '../components/IconSymbol.vue'
import LoadingState from '../components/LoadingState.vue'
import SceneEditorSheet from '../components/SceneEditorSheet.vue'
import SceneItemSheet from '../components/SceneItemSheet.vue'
import { itemsApi, scenesApi, sectionsApi } from '../api'
import { mergeIds, progressText } from '../catalog'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const error = ref('')
const scene = ref(null)
const allSections = ref([])
const allItems = ref([])
const sceneEditorOpen = ref(false)
const itemEditorOpen = ref(false)
const savingEditor = ref(false)
const savingItems = ref(new Set())
const toast = ref('')
let toastTimer

const statusText = computed(() => progressText(scene.value?.checkedCount || 0, scene.value?.totalCount || 0))

onMounted(load)
onBeforeUnmount(() => clearTimeout(toastTimer))

async function load() {
  loading.value = true
  error.value = ''
  try {
    ;[scene.value, allSections.value, allItems.value] = await Promise.all([
      scenesApi.get(route.params.id), sectionsApi.list(), itemsApi.list(),
    ])
  } catch (exception) {
    error.value = exception.message
  } finally {
    loading.value = false
  }
}

function showToast(message, duration = 3000) {
  toast.value = message
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, duration)
}

async function toggleItem(item) {
  if (savingItems.value.has(item.id)) return
  const next = !item.checked
  item.checked = next
  scene.value.checkedCount += next ? 1 : -1
  savingItems.value = new Set([...savingItems.value, item.id])
  try {
    await scenesApi.setChecked(scene.value.id, item.id, next)
    if (scene.value.totalCount > 0 && scene.value.checkedCount === scene.value.totalCount) showToast('已全部准备好')
  } catch (exception) {
    item.checked = !next
    scene.value.checkedCount += next ? -1 : 1
    error.value = exception.message
  } finally {
    const nextSaving = new Set(savingItems.value)
    nextSaving.delete(item.id)
    savingItems.value = nextSaving
  }
}

async function resetChecklist() {
  if (!scene.value.totalCount || !window.confirm('开始新一次准备会清空当前勾选，继续吗？')) return
  try {
    await scenesApi.reset(scene.value.id)
    scene.value.sections.forEach((section) => section.items.forEach((item) => { item.checked = false }))
    scene.value.checkedCount = 0
    showToast('已清空，可以重新准备')
  } catch (exception) {
    error.value = exception.message
  }
}

function openAddItem() {
  if (!scene.value.sectionIds.length) {
    error.value = '先为场景绑定一个分区，再添加物品'
    sceneEditorOpen.value = true
    return
  }
  itemEditorOpen.value = true
}

async function saveItem(payload) {
  savingEditor.value = true
  error.value = ''
  try {
    if (payload.existingItem) {
      await itemsApi.update(payload.existingItem.id, {
        name: payload.existingItem.name,
        sectionIds: mergeIds(payload.existingItem.sectionIds, payload.sectionIds),
      })
    } else {
      await itemsApi.create({ name: payload.name, sectionIds: payload.sectionIds })
    }
    itemEditorOpen.value = false
    await load()
    showToast(payload.existingItem ? '已使用物品库中的物品' : '物品已创建')
  } catch (exception) {
    error.value = exception.message
  } finally {
    savingEditor.value = false
  }
}

async function saveScene(payload) {
  savingEditor.value = true
  try {
    await scenesApi.update(scene.value.id, payload)
    sceneEditorOpen.value = false
    await load()
  } catch (exception) {
    error.value = exception.message
  } finally {
    savingEditor.value = false
  }
}

async function deleteScene() {
  if (!window.confirm(`删除“${scene.value.name}”？分区和物品都会保留。`)) return
  savingEditor.value = true
  try {
    await scenesApi.remove(scene.value.id)
    await router.replace('/')
  } catch (exception) {
    error.value = exception.message
    savingEditor.value = false
  }
}
</script>

<template>
  <main class="app-page checklist-page-v2">
    <header class="checklist-topbar">
      <button class="icon-button" type="button" aria-label="返回" @click="router.push('/')"><IconSymbol name="back" /></button>
      <div><strong>{{ scene?.name || '场景清单' }}</strong><span v-if="scene">{{ scene.totalCount }} 件物品 · {{ scene.sections.length }} 个分区</span></div>
      <button class="icon-button" type="button" aria-label="管理场景" @click="sceneEditorOpen = true">⋯</button>
    </header>

    <LoadingState v-if="loading" />
    <template v-else-if="scene">
      <section class="check-summary">
        <div><strong>{{ scene.checkedCount }} / {{ scene.totalCount }}</strong><span>{{ statusText }}</span></div>
        <div class="progress"><span :style="{ width: `${scene.totalCount ? scene.checkedCount / scene.totalCount * 100 : 0}%` }" /></div>
      </section>
      <div v-if="error" class="notice notice--error notice--sticky"><span>{{ error }}</span><button type="button" @click="error = ''">关闭</button></div>

      <section class="check-section-stack">
        <article v-for="section in scene.sections" :key="section.id" class="option-section-card">
          <header><strong>{{ section.name }}</strong><span>{{ section.items.filter((item) => item.checked).length }}/{{ section.items.length }} 已勾选</span></header>
          <button
            v-for="item in section.items"
            :key="item.id"
            class="option-item-row"
            :class="{ checked: item.checked }"
            type="button"
            :disabled="savingItems.has(item.id)"
            @click="toggleItem(item)"
          >
            <span class="check-circle">✓</span><span>{{ item.name }}</span>
          </button>
          <p v-if="!section.items.length" class="empty-section">这个分区暂时没有物品</p>
        </article>
        <div v-if="!scene.sections.length" class="empty-checklist">
          <strong>这个场景还没有分区</strong><span>绑定分区后，相关物品会自动出现在这里。</span>
          <button class="button button--outline" type="button" @click="sceneEditorOpen = true">管理场景</button>
        </div>
      </section>

      <div class="check-actions-v2">
        <button class="button button--primary button--full button--large" type="button" @click="openAddItem"><IconSymbol name="plus" :size="19" /> 添加物品</button>
        <button class="reset-button" type="button" @click="resetChecklist">开始新一次准备</button>
      </div>
    </template>

    <Transition name="toast"><div v-if="toast" class="toast-message">{{ toast }}</div></Transition>
    <SceneEditorSheet :open="sceneEditorOpen" :scene="scene" :sections="allSections" :saving="savingEditor" @close="sceneEditorOpen = false" @save="saveScene" @delete="deleteScene" />
    <SceneItemSheet :open="itemEditorOpen" :sections="scene?.sections || []" :items="allItems" :saving="savingEditor" @close="itemEditorOpen = false" @save="saveItem" />
  </main>
</template>
