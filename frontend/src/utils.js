export function progressPercent(checked, total) {
  if (!total) return 0
  return Math.round((checked / total) * 100)
}

export function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date).replaceAll('/', '.')
}

export function makeId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export function normalizeItemName(value) {
  return value.trim().replace(/\s+/g, ' ').toLocaleLowerCase('zh-CN')
}
