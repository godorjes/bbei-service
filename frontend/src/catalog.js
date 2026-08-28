export function moveId(ids, id, direction) {
  const result = [...ids]
  const index = result.indexOf(id)
  const target = index + direction
  if (index < 0 || target < 0 || target >= result.length) return result
  ;[result[index], result[target]] = [result[target], result[index]]
  return result
}

export function mergeIds(current = [], additions = []) {
  return [...new Set([...current, ...additions])]
}

export function progressText(checked, total) {
  if (!total) return '还没有物品'
  if (checked >= total) return '已全部准备好'
  return `还有 ${total - checked} 件未确认`
}

export function isSceneComplete(scene) {
  return scene.totalCount > 0 && scene.checkedCount >= scene.totalCount
}
