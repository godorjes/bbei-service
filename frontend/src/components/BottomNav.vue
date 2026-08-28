<script setup>
import { RouterLink } from 'vue-router'
import IconSymbol from './IconSymbol.vue'

defineProps({
  active: { type: String, required: true },
})
</script>

<template>
  <nav class="bottom-nav" aria-label="主导航">
    <RouterLink to="/" class="bottom-nav__item" :class="{ active: active === 'checklists' }">
      <IconSymbol name="checklist" :size="21" />
      <span>清单</span>
    </RouterLink>
    <RouterLink to="/organize" class="bottom-nav__item" :class="{ active: active === 'organize' }">
      <IconSymbol name="settings" :size="21" />
      <span>整理</span>
    </RouterLink>
  </nav>
</template>
