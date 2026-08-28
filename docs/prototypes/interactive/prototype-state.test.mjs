import test from 'node:test';
import assert from 'node:assert/strict';

import {
  addItem,
  createInitialState,
  openCompletion,
  resolveAddedItem,
  startCustomScene,
  startScene,
  toggleItem,
} from './prototype-state.mjs';

test('opens on the scene chooser', () => {
  assert.equal(createInitialState().screen, 'home');
});

test('starts the weekend scene in one action', () => {
  const state = startScene(createInitialState(), 'weekend');

  assert.equal(state.screen, 'checklist');
  assert.equal(state.activeScene.id, 'weekend');
  assert.equal(state.activeScene.items.length, 8);
});

test('starts a named custom scene with an empty checklist', () => {
  const state = startCustomScene(createInitialState(), ' 周日散步 ');

  assert.equal(state.screen, 'checklist');
  assert.equal(state.activeScene.name, '周日散步');
  assert.deepEqual(state.activeScene.items, []);
});

test('toggles a checklist item without mutating the previous state', () => {
  const started = startScene(createInitialState(), 'weekend');
  const toggled = toggleItem(started, 'power-bank');

  assert.equal(started.activeScene.items.find((entry) => entry.id === 'power-bank').checked, false);
  assert.equal(toggled.activeScene.items.find((entry) => entry.id === 'power-bank').checked, true);
});

test('adds a temporary item to the active scene', () => {
  const started = startScene(createInitialState(), 'weekend');
  const updated = addItem(started, ' 电脑充电器 ', '数码');

  assert.equal(updated.activeScene.items.length, 9);
  assert.deepEqual(updated.addedItem, {
    id: 'temporary-1',
    name: '电脑充电器',
    group: '数码',
    checked: false,
    temporary: true,
  });
});

test('opens the completion screen for the active scene', () => {
  const completed = openCompletion(startScene(createInitialState(), 'weekend'));

  assert.equal(completed.screen, 'complete');
  assert.equal(completed.activeScene.name, '周末出游');
});

test('resolves a temporary item as only this time and returns home', () => {
  const completed = openCompletion(addItem(startScene(createInitialState(), 'weekend'), '电脑充电器', '数码'));
  const resolved = resolveAddedItem(completed, false);

  assert.equal(resolved.screen, 'home');
  assert.equal(resolved.activeScene, null);
  assert.equal(resolved.notice, '已完成，本次新增不会保留到场景');
});
