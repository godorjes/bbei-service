import assert from 'node:assert/strict';

async function loadModel() {
  try {
    return await import('./option-a-state.mjs');
  } catch (error) {
    if (error.code === 'ERR_MODULE_NOT_FOUND' || /Module not found/.test(error.message)) return {};
    throw error;
  }
}

export async function runOptionAStateTests() {
  const model = await loadModel();
  const results = [];

  function check(name, testBody) {
    testBody();
    results.push(name);
  }

  check('一周旅行默认清单按轻量分区容纳至少 40 件物品', () => {
    assert.equal(typeof model.createInitialState, 'function');
    const state = model.createInitialState();
    assert.ok(state.sections.length >= 5);
    assert.ok(state.sections.flatMap((section) => section.items).length >= 40);
    assert.equal(state.sections.some((section) => 'tags' in section), false);
    assert.equal(state.sections.flatMap((section) => section.items).some((item) => 'tags' in item), false);
  });

  check('勾选物品同时更新总进度和所在分区进度', () => {
    let state = model.createInitialState();
    state = model.toggleItem(state, 'id-card');
    assert.deepEqual(model.getOverallProgress(state), { checked: 1, total: 42 });
    assert.deepEqual(model.getSectionProgress(state, 'essentials'), { checked: 1, total: 6 });
  });

  check('折叠一个分区不会改变物品或其他分区', () => {
    const initial = model.createInitialState();
    const state = model.toggleSection(initial, 'electronics');
    assert.equal(state.sections.find((section) => section.id === 'electronics').collapsed, true);
    assert.equal(state.sections.find((section) => section.id === 'clothing').collapsed, false);
    assert.equal(state.sections.flatMap((section) => section.items).length, 42);
  });

  check('未勾选筛选保留分区结构并隐藏已勾选物品', () => {
    let state = model.createInitialState();
    state = model.toggleItem(state, 'id-card');
    state = model.setFilter(state, 'pending');
    const visible = model.getVisibleSections(state);
    assert.equal(visible.find((section) => section.id === 'essentials').items.some((item) => item.id === 'id-card'), false);
    assert.equal(visible.some((section) => section.id === 'electronics'), true);
  });

  check('从分区入口新增时直接加入指定分区', () => {
    let state = model.createInitialState();
    state = model.addItem(state, { name: '运动相机', sectionId: 'electronics' });
    assert.equal(state.sections.find((section) => section.id === 'electronics').items.at(-1).name, '运动相机');
    assert.throws(() => model.addItem(state, { name: ' 运动 相机 ', sectionId: 'other' }), /已经在清单中/);
  });

  check('没有选择分区的新物品进入其他', () => {
    const state = model.addItem(model.createInitialState(), { name: '备用袋' });
    assert.equal(state.sections.find((section) => section.id === 'other').items.at(-1).name, '备用袋');
  });

  check('清空勾选保留清单结构和物品', () => {
    let state = model.toggleItem(model.createInitialState(), 'id-card');
    state = model.clearChecks(state);
    assert.deepEqual(model.getOverallProgress(state), { checked: 0, total: 42 });
    assert.equal(state.sections.flatMap((section) => section.items).length, 42);
  });

  check('分区管理可以改名但不能产生同名分区', () => {
    assert.equal(typeof model.renameSection, 'function');
    let state = model.createInitialState();
    state = model.renameSection(state, 'electronics', '数码设备');
    assert.equal(state.sections.find((section) => section.id === 'electronics').name, '数码设备');
    assert.throws(() => model.renameSection(state, 'electronics', ' 衣物 '), /分区名称已存在/);
  });

  check('新增分区保持为空且不引入标签', () => {
    assert.equal(typeof model.addSection, 'function');
    const state = model.addSection(model.createInitialState(), '随身食品');
    const section = state.sections.at(-1);
    assert.equal(section.name, '随身食品');
    assert.deepEqual(section.items, []);
    assert.equal('tags' in section, false);
  });

  return results;
}
