<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import EmptyState from '../components/EmptyState.vue'
import IconSymbol from '../components/IconSymbol.vue'
import LoadingState from '../components/LoadingState.vue'
import { templatesApi } from '../api'

const router = useRouter()
const loading = ref(true)
const tab = ref('active')
const activeTemplates = ref([])
const archivedTemplates = ref([])
const error = ref('')

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [active, archived] = await Promise.all([templatesApi.list(false), templatesApi.list(true)])
    activeTemplates.value = active
    archivedTemplates.value = archived
  } catch (exception) {
    error.value = exception.message
  } finally {
    loading.value = false
  }
}

async function toggleArchive(template, archived) {
  try {
    await templatesApi.archive(template.id, archived)
    await load()
  } catch (exception) {
    error.value = exception.message
  }
}
</script>

<template>
  <main class="app-page">
    <header class="page-header">
      <button class="icon-button" type="button" aria-label="返回" @click="router.push('/')">
        <IconSymbol name="back" />
      </button>
      <h1>场景模板</h1>
      <button class="icon-button icon-button--accent" type="button" aria-label="新建模板" @click="router.push('/templates/new')">
        <IconSymbol name="plus" />
      </button>
    </header>

    <div class="segmented segmented--wide">
      <button type="button" :class="{ active: tab === 'active' }" @click="tab = 'active'">使用中</button>
      <button type="button" :class="{ active: tab === 'archived' }" @click="tab = 'archived'">已归档</button>
    </div>

    <LoadingState v-if="loading" />
    <template v-else>
      <div v-if="error" class="notice notice--error">{{ error }}</div>

      <div v-if="(tab === 'active' ? activeTemplates : archivedTemplates).length" class="manage-list">
        <article
          v-for="template in (tab === 'active' ? activeTemplates : archivedTemplates)"
          :key="template.id"
          class="manage-card"
        >
          <button class="manage-card__main" type="button" @click="router.push(`/templates/${template.id}/edit`)">
            <span class="scene-card__icon"><IconSymbol :name="template.icon" /></span>
            <span>
              <strong>{{ template.name }}</strong>
              <small>{{ template.sections.length }} 个分组 · {{ template.itemCount }} 件物品</small>
            </span>
          </button>
          <div class="manage-card__actions">
            <span v-if="template.pinned && tab === 'active'" class="quiet-badge">
              <IconSymbol name="pin" :size="13" /> 常用
            </span>
            <button type="button" @click="toggleArchive(template, tab === 'active')">
              {{ tab === 'active' ? '归档' : '恢复' }}
            </button>
          </div>
        </article>
      </div>

      <EmptyState
        v-else
        :title="tab === 'active' ? '还没有场景模板' : '没有已归档模板'"
        :description="tab === 'active' ? '模板保持稳定，本次变化默认不会写回。' : '暂时不用的模板可以收在这里。'"
      >
        <button v-if="tab === 'active'" class="button button--primary" type="button" @click="router.push('/templates/new')">
          新建模板
        </button>
      </EmptyState>
    </template>
  </main>
</template>
