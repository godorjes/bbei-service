<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import IconSymbol from '../components/IconSymbol.vue'
import LoadingState from '../components/LoadingState.vue'
import { listsApi, templatesApi } from '../api'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const creatingId = ref(null)
const error = ref('')
const templates = ref([])

onMounted(async () => {
  try {
    templates.value = await templatesApi.list()
    const requestedId = Number(route.query.template)
    if (requestedId && templates.value.some((template) => template.id === requestedId)) {
      await createFromTemplate(requestedId)
    }
  } catch (exception) {
    error.value = exception.message
  } finally {
    loading.value = false
  }
})

async function createFromTemplate(templateId) {
  if (creatingId.value) return
  creatingId.value = templateId
  error.value = ''
  try {
    const list = await listsApi.create({ templateId })
    await router.replace(`/lists/${list.id}`)
  } catch (exception) {
    error.value = exception.message
    creatingId.value = null
  }
}

async function createBlank() {
  if (creatingId.value) return
  creatingId.value = 'blank'
  error.value = ''
  try {
    const list = await listsApi.create({})
    await router.replace(`/lists/${list.id}`)
  } catch (exception) {
    error.value = exception.message
    creatingId.value = null
  }
}
</script>

<template>
  <main class="app-page">
    <header class="page-header">
      <button class="icon-button" type="button" aria-label="返回" @click="router.push('/')">
        <IconSymbol name="back" />
      </button>
      <h1>选择场景</h1>
      <span class="header-spacer" />
    </header>

    <LoadingState v-if="loading" />

    <template v-else>
      <p class="page-intro">从一个稳定模板开始，本次调整不会自动改变模板。</p>
      <div v-if="error" class="notice notice--error">{{ error }}</div>

      <section class="scene-list">
        <button
          v-for="template in templates"
          :key="template.id"
          class="scene-card"
          type="button"
          :disabled="creatingId !== null"
          @click="createFromTemplate(template.id)"
        >
          <span class="scene-card__icon">
            <IconSymbol :name="template.icon" :size="26" />
          </span>
          <span class="scene-card__body">
            <strong>{{ template.name }}</strong>
            <small>{{ template.itemCount }} 件物品</small>
          </span>
          <span v-if="creatingId === template.id" class="spinner spinner--small" />
          <IconSymbol v-else name="chevron" :size="18" />
        </button>
      </section>

      <button class="blank-card" type="button" :disabled="creatingId !== null" @click="createBlank">
        <span v-if="creatingId === 'blank'" class="spinner spinner--small" />
        <IconSymbol v-else name="plus" />
        <span>
          <strong>空白清单</strong>
          <small>从零开始，本次完成后可保存为模板</small>
        </span>
      </button>

      <button class="text-link text-link--center" type="button" @click="router.push('/templates/new')">
        创建新的场景模板
      </button>
    </template>
  </main>
</template>
