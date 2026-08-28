<script setup>
import { computed } from 'vue'
import { progressPercent } from '../utils'

const props = defineProps({
  checked: { type: Number, default: 0 },
  total: { type: Number, default: 0 },
})

const percent = computed(() => progressPercent(props.checked, props.total))
</script>

<template>
  <div class="progress" role="progressbar" :aria-valuenow="checked" aria-valuemin="0" :aria-valuemax="total">
    <span :style="{ width: `${percent}%` }" />
  </div>
</template>
