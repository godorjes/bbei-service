<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BottomNav from '../components/BottomNav.vue'
import EmptyState from '../components/EmptyState.vue'
import IconSymbol from '../components/IconSymbol.vue'
import LoadingState from '../components/LoadingState.vue'
import { listsApi } from '../api'
import { formatDate } from '../utils'

const router = useRouter()
const loading = ref(true)
const history = ref([])
const error = ref('')
const reusingId = ref(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    history.value = await listsApi.history()
  } catch (exception) {
    error.value = exception.message
  } finally {
    loading.value = false
  }
}

async function reuse(list) {
  if (reusingId.value) return
  reusingId.value = list.id
  error.value = ''
  try {
    const created = await listsApi.reuse(list.id)
    await router.push(`/lists/${created.id}`)
  } catch (exception) {
    error.value = exception.message
    reusingId.value = null
  }
}
</script>

<template>
  <main class="app-page app-page--with-nav">
    <header class="simple-title-header">
      <div>
        <h1>历史记录</h1>
        <p>完成的清单会安静地收在这里</p>
      </div>
    </header>

    <LoadingState v-if="loading" />

    <template v-else>
      <div v-if="error" class="notice notice--error">{{ error }}</div>
      <div v-if="history.length" class="history-list">
        <article v-for="list in history" :key="list.id" class="history-card">
          <button class="history-card__main" type="button" @click="router.push(`/lists/${list.id}?readonly=1`)">
            <span class="history-card__icon">
              <IconSymbol :name="list.sourceTemplateId ? 'briefcase' : 'checklist'" />
            </span>
            <span class="history-card__content">
              <strong>{{ list.title }}</strong>
              <small>{{ formatDate(list.completedAt) }}</small>
            </span>
            <span class="history-card__count">{{ list.checkedCount }} / {{ list.totalCount }}</span>
          </button>
          <button class="button button--small-outline" type="button" :disabled="reusingId !== null" @click="reuse(list)">
            {{ reusingId === list.id ? '创建中…' : '再次使用' }}
          </button>
        </article>
      </div>

      <EmptyState
        v-else
        title="还没有历史记录"
        description="完成一次准备后，它会自动出现在这里。"
      >
        <button class="button button--primary" type="button" @click="router.push('/choose')">开始第一次准备</button>
      </EmptyState>
    </template>

    <BottomNav active="history" />
  </main>
</template>
