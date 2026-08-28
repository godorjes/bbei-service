<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '../components/BottomNav.vue'
import EmptyState from '../components/EmptyState.vue'
import IconSymbol from '../components/IconSymbol.vue'
import LoadingState from '../components/LoadingState.vue'
import SceneEditorSheet from '../components/SceneEditorSheet.vue'
import { scenesApi, sectionsApi } from '../api'
import { isSceneComplete } from '../catalog'

const router = useRouter()
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const scenes = ref([])
const sections = ref([])
const editorOpen = ref(false)
const editingScene = ref(null)
const resettingSceneId = ref(null)

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    ;[scenes.value, sections.value] = await Promise.all([scenesApi.list(), sectionsApi.list()])
  } catch (exception) {
    error.value = exception.message
  } finally {
    loading.value = false
  }
}

function openEditor(scene = null) {
  editingScene.value = scene
  editorOpen.value = true
}

async function saveScene(payload) {
  if (saving.value) return
  saving.value = true
  error.value = ''
  try {
    if (editingScene.value) await scenesApi.update(editingScene.value.id, payload)
    else await scenesApi.create(payload)
    editorOpen.value = false
    editingScene.value = null
    await load()
  } catch (exception) {
    error.value = exception.message
  } finally {
    saving.value = false
  }
}

async function startNewPreparation(scene) {
  if (resettingSceneId.value !== null) return
  resettingSceneId.value = scene.id
  error.value = ''
  try {
    await scenesApi.reset(scene.id)
    await router.push(`/scenes/${scene.id}`)
  } catch (exception) {
    error.value = exception.message
  } finally {
    resettingSceneId.value = null
  }
}

async function deleteScene(scene) {
  if (!window.confirm(`删除“${scene.name}”？分区和物品都会保留。`)) return
  saving.value = true
  try {
    await scenesApi.remove(scene.id)
    editorOpen.value = false
    editingScene.value = null
    await load()
  } catch (exception) {
    error.value = exception.message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <main class="app-page app-page--with-nav">
    <header class="home-header option-header">
      <div><p class="eyebrow">带上</p><h1>这次要去哪？</h1><span>选择一个场景，直接开始准备</span></div>
      <button class="icon-button icon-button--accent" type="button" aria-label="新建场景" @click="openEditor()">
        <IconSymbol name="plus" />
      </button>
    </header>

    <LoadingState v-if="loading" />
    <template v-else>
      <div v-if="error" class="notice notice--error"><span>{{ error }}</span><button type="button" @click="load">重试</button></div>

      <section v-if="scenes.length" class="scene-grid">
        <article v-for="scene in scenes" :key="scene.id" class="option-scene-card" :class="{ 'option-scene-card--complete': isSceneComplete(scene) }">
          <button class="scene-card-main" :class="{ 'scene-card-main--complete': isSceneComplete(scene) }" type="button" @click="router.push(`/scenes/${scene.id}`)">
            <span class="scene-letter">{{ scene.name.slice(0, 1) }}</span>
            <span class="scene-card-copy">
              <strong>{{ scene.name }}</strong>
              <small v-if="isSceneComplete(scene)" class="scene-complete-copy">上次已带齐 · {{ scene.sectionIds.length }} 个分区</small>
              <small v-else>{{ scene.totalCount }} 件物品 · {{ scene.sectionIds.length }} 个分区</small>
              <span class="mini-progress"><i :style="{ width: `${scene.totalCount ? scene.checkedCount / scene.totalCount * 100 : 0}%` }" /></span>
            </span>
            <span class="scene-count">{{ scene.checkedCount }}/{{ scene.totalCount }}</span>
          </button>
          <button class="scene-edit" type="button" :aria-label="`编辑${scene.name}`" @click="openEditor(scene)">⋯</button>
          <button
            v-if="isSceneComplete(scene)"
            class="scene-restart"
            type="button"
            :disabled="resettingSceneId !== null"
            :aria-label="`开始${scene.name}的新一次准备`"
            @click="startNewPreparation(scene)"
          >
            {{ resettingSceneId === scene.id ? '正在重置…' : '开始新一次' }}
          </button>
        </article>
      </section>

      <EmptyState v-else title="还没有场景" description="先创建一个经常使用的出门场景，下次打开就能直接勾选。">
        <button class="button button--primary" type="button" @click="openEditor()">创建场景</button>
      </EmptyState>

      <button v-if="scenes.length" class="create-outline" type="button" @click="openEditor()">
        <IconSymbol name="plus" :size="18" /> 创建新的场景
      </button>
    </template>

    <SceneEditorSheet
      :open="editorOpen"
      :scene="editingScene"
      :sections="sections"
      :saving="saving"
      @close="editorOpen = false"
      @save="saveScene"
      @delete="deleteScene"
    />
    <BottomNav active="checklists" />
  </main>
</template>
