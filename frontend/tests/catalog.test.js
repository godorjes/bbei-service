import test from 'node:test'
import assert from 'node:assert/strict'
import * as catalog from '../src/catalog.js'

const { mergeIds, moveId, progressText } = catalog

test('moveId reorders a section without mutating the source array', () => {
  const source = [1, 2, 3]

  assert.deepEqual(moveId(source, 2, -1), [2, 1, 3])
  assert.deepEqual(moveId(source, 1, -1), [1, 2, 3])
  assert.deepEqual(source, [1, 2, 3])
})

test('mergeIds keeps existing order and appends only new bindings', () => {
  assert.deepEqual(mergeIds([3, 1], [1, 2, 2]), [3, 1, 2])
})

test('progressText keeps checklist feedback short and direct', () => {
  assert.equal(progressText(0, 4), '还有 4 件未确认')
  assert.equal(progressText(3, 4), '还有 1 件未确认')
  assert.equal(progressText(4, 4), '已全部准备好')
  assert.equal(progressText(0, 0), '还没有物品')
})

test('isSceneComplete only marks a non-empty fully checked scene as complete', () => {
  assert.equal(catalog.isSceneComplete?.({ checkedCount: 3, totalCount: 3 }), true)
  assert.equal(catalog.isSceneComplete?.({ checkedCount: 2, totalCount: 3 }), false)
  assert.equal(catalog.isSceneComplete?.({ checkedCount: 0, totalCount: 0 }), false)
})
