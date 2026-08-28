<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import IconSymbol from '../components/IconSymbol.vue'
import LoadingState from '../components/LoadingState.vue'
import { listsApi } from '../api'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const submitting = ref(false)
const error = ref('')
const list = ref(null)
const selected = ref(new Set())
const saveAsTemplate = ref(false)
const templateName = ref('')

const changes = computed(() => list.value?.changes || { added: [], removed: [], modified: [] })
const temporaryItems = computed(() => changes.value.added || [])
const hasChanges = computed(() => (
  changes.value.added.length + changes.value.removed.length + changes.value.modified.length
) > 0)
const remaining = computed(() => Math.max(0, (list.value?.totalCount || 0) - (list.value?.checkedCount || 0)))

onMounted(async () => {
  try {
    list.value = await listsApi.get(route.params.id)
    if (list.value.status === 'COMPLETED') {
      await router.replace('/history')
      return
    }
    templateName.value = list.value.title.replace(/\s*·\s*\d+月\d+日$/, '')
  } catch (exception) {
    error.value = exception.message
  } finally {
    loading.value = false
  }
})

function toggleSelected(id) {
  const next = new Set(selected.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  selected.value = next
}

async function complete() {
  if (submitting.value) return
  if (!list.value.sourceTemplateId && saveAsTemplate.value && !templateName.value.trim()) {
    error.value = '请输入新模板名称'
    return
  }
  submitting.value = true
  error.value = ''
  try {
    await listsApi.complete(list.value.id, {
      promoteItemIds: [...selected.value],
      saveAsTemplateName: !list.value.sourceTemplateId && saveAsTemplate.value ? templateName.value.trim() : null,
    })
    await router.replace('/history')
  } catch (exception) {
    error.value = exception.message
    submitting.value = false
  }
}
</script>

<template>
  <main class="app-page complete-page">
    <header class="page-header">
      <button class="icon-button" type="button" aria-label="返回" @click="router.back()">
        <IconSymbol name="back" />
      </button>
      <h1>完成本次准备</h1>
      <span class="header-spacer" />
    </header>

    <LoadingState v-if="loading" />
    <template v-else-if="list">
      <section class="complete-summary">
        <span class="complete-summary__check">✓</span>
        <div>
          <p>已准备</p>
          <strong>{{ list.checkedCount }} <small>/ {{ list.totalCount }}</small></strong>
        </div>
      </section>

      <div v-if="remaining" class="notice notice--warm">
        还有 {{ remaining }} 项没有勾选，也可以结束本次准备。
      </div>
      <div v-if="error" class="notice notice--error">{{ error }}</div>

      <section v-if="list.sourceTemplateId && hasChanges" class="change-card change-card--summary">
        <div class="change-card__header">
          <div>
            <h2>本次变化</h2>
            <p>默认不修改模板，只能选择新增物品写回。</p>
          </div>
          <span>{{ changes.added.length + changes.removed.length + changes.modified.length }} 项</span>
        </div>

        <div v-if="changes.added.length" class="change-kind">
          <div class="change-kind__title change-kind__title--added">新增</div>
          <button
            v-for="item in changes.added"
            :key="`added-${item.itemId}`"
            class="change-row"
            type="button"
            @click="toggleSelected(item.itemId)"
          >
            <span class="check-button" :class="{ checked: selected.has(item.itemId) }">
              <span v-if="selected.has(item.itemId)">✓</span>
            </span>
            <span>
              <strong>{{ item.name }}</strong>
              <small>{{ item.sectionTitle }}<template v-if="item.quantity > 1"> · × {{ item.quantity }}</template></small>
            </span>
          </button>
        </div>

        <div v-if="changes.removed.length" class="change-kind">
          <div class="change-kind__title change-kind__title--removed">删除</div>
          <div v-for="item in changes.removed" :key="`removed-${item.itemId}`" class="change-summary-row">
            <strong>{{ item.name }}</strong>
            <small>{{ item.sectionTitle }} · {{ item.description }}</small>
          </div>
        </div>

        <div v-if="changes.modified.length" class="change-kind">
          <div class="change-kind__title change-kind__title--modified">修改</div>
          <div v-for="item in changes.modified" :key="`modified-${item.itemId}`" class="change-summary-row">
            <strong>{{ item.name }}<template v-if="item.quantity > 1"> × {{ item.quantity }}</template></strong>
            <small>{{ item.description }}</small>
          </div>
        </div>

        <div class="template-policy">
          <strong>{{ selected.size ? `已选择 ${selected.size} 项加入模板` : '默认不修改模板' }}</strong>
          <span>删除和修改只保留在本次历史中。</span>
        </div>
      </section>
      <section v-else-if="list.sourceTemplateId && !hasChanges" class="quiet-card">
        <span class="quiet-card__icon">✓</span>
        <div>
          <h2>模板保持不变</h2>
          <p>本次没有新增物品，勾选和修改只保留在历史记录中。</p>
        </div>
      </section>

      <section v-else-if="!list.sourceTemplateId" class="change-card">
        <label class="switch-row">
          <span>
            <strong>保存为新模板</strong>
            <small>下次可以直接复用这份物品清单</small>
          </span>
          <input v-model="saveAsTemplate" type="checkbox" />
          <i />
        </label>
        <label v-if="saveAsTemplate" class="field field--inside-card">
          <span>模板名称</span>
          <input v-model="templateName" maxlength="50" placeholder="例如：露营" />
        </label>
      </section>

      <div class="complete-actions">
        <button class="button button--primary button--full button--large" type="button" :disabled="submitting" @click="complete">
          {{ submitting ? '正在保存…' : '完成并保存' }}
        </button>
        <p v-if="list.sourceTemplateId && temporaryItems.length && !selected.size">本次新增物品不会加入模板</p>
      </div>
    </template>
  </main>
</template>
