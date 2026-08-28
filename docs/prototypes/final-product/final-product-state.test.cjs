const test = require('node:test');
const assert = require('node:assert/strict');

let model = {};
try {
  model = require('./final-product-state.js');
} catch (error) {
  if (error.code !== 'MODULE_NOT_FOUND') throw error;
}

let ui = {};
try {
  ui = require('./final-product-ui.js');
} catch (error) {
  if (error.code !== 'MODULE_NOT_FOUND') throw error;
}

test('initial state contains three ready-to-use scenes without group data', () => {
  assert.equal(typeof model.createInitialState, 'function');
  const state = model.createInitialState();
  assert.deepEqual(state.scenes.map((scene) => scene.name), ['周末出游', '商务出差', '一周旅行']);
  assert.equal(state.scenes.flatMap((scene) => scene.items).some((item) => 'group' in item), false);
});

test('opening the same scene resumes one preparation instead of creating duplicates', () => {
  let state = model.createInitialState();
  state = model.openScene(state, 'weekend');
  const preparationId = state.preparations.weekend.id;
  state = model.openScene(state, 'weekend');
  assert.equal(state.screen, 'checklist');
  assert.equal(state.activeSceneId, 'weekend');
  assert.equal(state.preparations.weekend.id, preparationId);
  assert.equal(Object.keys(state.preparations).length, 1);
});

test('checking an item changes only the active preparation', () => {
  const opened = model.openScene(model.createInitialState(), 'weekend');
  const state = model.toggleItem(opened, 'id-card');
  assert.equal(state.preparations.weekend.items.find((item) => item.itemId === 'id-card').checked, true);
  assert.equal(state.scenes.find((scene) => scene.id === 'weekend').items.find((item) => item.itemId === 'id-card').checked, undefined);
  assert.equal(opened.preparations.weekend.items.find((item) => item.itemId === 'id-card').checked, false);
});

test('adding an item persists it to the scene and current preparation without a group', () => {
  let state = model.openScene(model.createInitialState(), 'weekend');
  state = model.addItem(state, { name: '折叠伞', quantity: 1, note: '放在侧袋' });
  const sceneItem = state.scenes.find((scene) => scene.id === 'weekend').items.find((item) => item.name === '折叠伞');
  const preparationItem = state.preparations.weekend.items.find((item) => item.name === '折叠伞');
  assert.deepEqual({ quantity: sceneItem.quantity, note: sceneItem.note }, { quantity: 1, note: '放在侧袋' });
  assert.equal(preparationItem.checked, false);
  assert.equal('group' in sceneItem, false);
  assert.equal(state.catalog.filter((item) => item.name === '折叠伞').length, 1);
});

test('adding a catalog item reuses its global identity', () => {
  let state = model.openScene(model.createInitialState(), 'weekend');
  const catalogCount = state.catalog.length;
  state = model.addItem(state, { name: '笔记本电脑' });
  const added = state.scenes.find((scene) => scene.id === 'weekend').items.find((item) => item.name === '笔记本电脑');
  assert.equal(added.itemId, 'laptop');
  assert.equal(state.catalog.length, catalogCount);
});

test('adding a normalized duplicate to one scene is rejected', () => {
  const state = model.openScene(model.createInitialState(), 'weekend');
  assert.throws(
    () => model.addItem(state, { name: ' 手机 充电器 ' }),
    /已经在清单中/
  );
});

test('pending filter hides checked items without changing their order', () => {
  let state = model.openScene(model.createInitialState(), 'weekend');
  state = model.toggleItem(state, 'id-card');
  state = model.setFilter(state, 'pending');
  const visible = model.getVisibleItems(state);
  assert.equal(visible.some((item) => item.itemId === 'id-card'), false);
  assert.deepEqual(visible.slice(0, 2).map((item) => item.itemId), ['phone-charger', 'power-bank']);
});

test('catalog search suggests reusable items that are not already in the active scene', () => {
  const state = model.openScene(model.createInitialState(), 'weekend');
  const suggestions = model.searchCatalog(state, '电脑');
  assert.deepEqual(suggestions.map((item) => item.name), ['笔记本电脑', '电脑充电器']);
  assert.equal(model.searchCatalog(state, '手机充电器').length, 0);
});

test('completion with pending items asks once then archives and returns directly home', () => {
  const state = model.openScene(model.createInitialState(), 'weekend');
  const blocked = model.completePreparation(state, false);
  assert.equal(blocked.requiresConfirmation, true);
  assert.equal(blocked.remaining, 8);
  assert.equal(blocked.state.history.length, 0);

  const completed = model.completePreparation(state, true);
  assert.equal(completed.requiresConfirmation, false);
  assert.equal(completed.state.screen, 'home');
  assert.equal(completed.state.preparations.weekend, undefined);
  assert.equal(completed.state.history.length, 1);
  assert.equal(completed.state.toast.message, '准备完成，可以出发了');
});

test('creating a scene opens an empty preparation immediately', () => {
  const state = model.createScene(model.createInitialState(), ' 临时拜访 ');
  const scene = state.scenes.find((entry) => entry.name === '临时拜访');
  assert.ok(scene);
  assert.equal(state.activeSceneId, scene.id);
  assert.equal(state.screen, 'checklist');
  assert.deepEqual(state.preparations[scene.id].items, []);
});

test('restarting keeps scene items and clears every check', () => {
  let state = model.openScene(model.createInitialState(), 'weekend');
  state = model.toggleItem(state, 'id-card');
  state = model.restartPreparation(state);
  assert.equal(state.preparations.weekend.items.every((item) => item.checked === false), true);
  assert.equal(state.scenes.find((scene) => scene.id === 'weekend').items.length, 8);
  assert.equal(state.screen, 'checklist');
});

test('abandoning removes only the active progress and returns home', () => {
  const opened = model.openScene(model.createInitialState(), 'weekend');
  const state = model.abandonPreparation(opened);
  assert.equal(state.preparations.weekend, undefined);
  assert.equal(state.scenes.find((scene) => scene.id === 'weekend').items.length, 8);
  assert.equal(state.screen, 'home');
  assert.equal(state.history.length, 0);
});

test('completion toast can be dismissed without changing history', () => {
  const opened = model.openScene(model.createInitialState(), 'weekend');
  const completed = model.completePreparation(opened, true).state;
  const state = model.dismissToast(completed);
  assert.equal(state.toast, null);
  assert.equal(state.history.length, 1);
});

test('home rendering gives first-time users direct scene choices', () => {
  assert.equal(typeof ui.renderApp, 'function');
  const html = ui.renderApp(model.createInitialState());
  assert.match(html, /这次要去哪？/);
  assert.match(html, /周末出游/);
  assert.match(html, /商务出差/);
  assert.match(html, /一周旅行/);
  assert.match(html, /创建自己的场景/);
});

test('checklist rendering is flat and exposes only the approved primary actions', () => {
  const state = model.openScene(model.createInitialState(), 'weekend');
  const html = ui.renderApp(state);
  assert.match(html, /周末出游/);
  assert.match(html, /添加物品/);
  assert.match(html, /完成准备/);
  assert.match(html, /全部/);
  assert.match(html, /未完成/);
  assert.doesNotMatch(html, /创建分组|选择分组|未分组|以后也带|仅本次|写回模板/);
});
