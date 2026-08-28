<script setup>
defineProps({
  name: { type: String, default: 'suitcase' },
  size: { type: Number, default: 22 },
  strokeWidth: { type: Number, default: 1.8 },
})
</script>

<template>
  <svg
    class="icon-symbol"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <template v-if="name === 'back'">
      <path d="m15 18-6-6 6-6" />
    </template>
    <template v-else-if="name === 'chevron'">
      <path d="m9 18 6-6-6-6" />
    </template>
    <template v-else-if="name === 'plus'">
      <path d="M12 5v14M5 12h14" />
    </template>
    <template v-else-if="name === 'checklist'">
      <rect x="5" y="3" width="14" height="18" rx="2" />
      <path d="M9 8h6M9 12h6M9 16h4" />
    </template>
    <template v-else-if="name === 'history'">
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </template>
    <template v-else-if="name === 'settings'">
      <path d="M4 7h10M18 7h2M4 17h2M10 17h10M14 4v6M6 14v6" />
    </template>
    <template v-else-if="name === 'edit'">
      <path d="M12 20h9M16.5 3.5a2.12 2.12 0 0 1 3 3L8 18l-4 1 1-4Z" />
    </template>
    <template v-else-if="name === 'trash'">
      <path d="M4 7h16M9 7V4h6v3M7 7l1 13h8l1-13M10 11v5M14 11v5" />
    </template>
    <template v-else-if="name === 'archive'">
      <path d="M4 5h16v4H4zM6 9v10h12V9M10 13h4" />
    </template>
    <template v-else-if="name === 'pin'">
      <path d="m12 17-5 5M15 3l6 6-4 1-4 6-5-5 6-4z" />
    </template>
    <template v-else-if="name === 'briefcase'">
      <rect x="3" y="7" width="18" height="13" rx="2" />
      <path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M3 12h18M10 12v2h4v-2" />
    </template>
    <template v-else-if="name === 'backpack'">
      <path d="M7 8V7a5 5 0 0 1 10 0v1M6 21h12a2 2 0 0 0 2-2v-8a3 3 0 0 0-3-3H7a3 3 0 0 0-3 3v8a2 2 0 0 0 2 2Z" />
      <path d="M8 13h8M8 17h5" />
    </template>
    <template v-else>
      <rect x="4" y="7" width="16" height="14" rx="2" />
      <path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M4 12h16" />
    </template>
  </svg>
</template>
