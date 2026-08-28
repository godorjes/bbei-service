# 带上完整可交互原型 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个双击即可运行、刷新不丢数据、所有可见操作都真实可用的手机端清单 HTML 原型。

**Architecture:** 生产交付只有一个 HTML，内部按 `Seed → Model → Storage → View → Controller` 分层，使用事件委托驱动两个主页面和底部弹层。Node 测试从 HTML 的具名内联脚本中提取纯模型与存储代码执行，因此既保持生产文件独立，又能对业务规则做自动化验证。

**Tech Stack:** HTML5、CSS3、原生 JavaScript、Web Storage、Node.js 内置 `node:test` / `assert` / `vm`。

**Spec:** `docs/superpowers/specs/2026-08-21-full-interactive-prototype-design.md`

## Global Constraints

- 生产原型为 `docs/prototypes/final-product/full-interactive-prototype.html`，不得引用外部 JavaScript、CSS、字体、图片或网络资源。
- 必须同时支持 `file://` 和本地 HTTP 打开。
- 数据保存键固定为 `bibei.fullPrototype.v1`，数据结构版本固定为 `1`。
- 不实现登录、同步、时间、提醒、推荐、标签、全局物品库、历史行程或完成庆祝页。
- 每张清单至少保留一个分区；删除分区不得删除其中的物品。
- 勾选状态持久化；筛选、当前页面和弹层状态不持久化。
- 所有危险操作必须确认；所有可见按钮必须真实可用。
- `D:\code\bibei` 当前不是 Git 仓库，因此各任务以测试通过和文件快照作为检查点，不执行 Git 提交。

## File Structure

- Create: `docs/prototypes/final-product/full-interactive-prototype.html` — 唯一生产交付，包含界面、样式、种子数据、模型、存储和控制器。
- Create: `docs/prototypes/final-product/full-interactive-prototype.test.cjs` — 从 HTML 提取具名脚本并验证数据模型、存储、静态依赖和关键控件。
- Preserve: `docs/prototypes/final-product/option-a-lightweight-sections.html` — 保留旧方案作问题对照，不再继续修改。

---

### Task 1: 建立可测试的数据模型与三张独立清单

**Files:**
- Create: `docs/prototypes/final-product/full-interactive-prototype.test.cjs`
- Create: `docs/prototypes/final-product/full-interactive-prototype.html`

**Interfaces:**
- Produces: `globalThis.BibeiModel`，包含 `createSeedData()`、`validateAppData(value)`、`createScene(data, name)`、`renameScene(data, sceneId, name)`、`deleteScene(data, sceneId)`、`getSceneStats(data, sceneId)`。
- Produces: HTML 中的 `<script id="bibei-model">`，测试通过 `vm.runInNewContext` 提取执行。

- [ ] **Step 1: 写模型提取器和失败测试**

```js
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const htmlPath = path.join(__dirname, 'full-interactive-prototype.html');

function readHtml() {
  return fs.readFileSync(htmlPath, 'utf8');
}

function runInlineScript(id, additions = {}) {
  const html = readHtml();
  const match = html.match(new RegExp(`<script id=["']${id}["'][^>]*>([\\s\\S]*?)<\\/script>`));
  assert.ok(match, `missing inline script: ${id}`);
  const sandbox = { console, Date, Math, JSON, ...additions };
  sandbox.globalThis = sandbox;
  vm.runInNewContext(match[1], sandbox, { filename: `${id}.js` });
  return sandbox;
}

function loadModel() {
  return runInlineScript('bibei-model').BibeiModel;
}

test('seed data contains three independent scenes with exact totals', () => {
  const Model = loadModel();
  const data = Model.createSeedData();
  assert.equal(data.version, 1);
  assert.deepEqual(
    Array.from(data.scenes, scene => [scene.name, Model.getSceneStats(data, scene.id).total]),
    [['周末出游', 18], ['商务出差', 24], ['一周旅行', 42]]
  );
  assert.notEqual(data.scenes[0].sections, data.scenes[1].sections);
});

test('a new scene starts with one default section and no items', () => {
  const Model = loadModel();
  const result = Model.createScene(Model.createSeedData(), '露营');
  const scene = result.data.scenes.find(entry => entry.id === result.sceneId);
  assert.equal(scene.name, '露营');
  assert.equal(scene.sections.length, 1);
  assert.equal(scene.sections[0].name, '物品');
  assert.equal(scene.sections[0].items.length, 0);
});

test('scene names reject empty and normalized duplicates', () => {
  const Model = loadModel();
  const data = Model.createSeedData();
  assert.throws(() => Model.createScene(data, '  '), /请输入清单名称/);
  assert.throws(() => Model.createScene(data, '周 末 出 游'), /清单名称已存在/);
});
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\full-interactive-prototype.test.cjs`

Expected: FAIL，错误包含 `ENOENT` 或 `missing inline script: bibei-model`。

- [ ] **Step 3: 创建 HTML 骨架和纯模型**

HTML 先包含完整文档骨架，以及不访问 DOM 的具名脚本：

```html
<script id="bibei-model">
(() => {
  const VERSION = 1;
  const clone = value => JSON.parse(JSON.stringify(value));
  const normalize = value => String(value ?? '').trim().toLocaleLowerCase('zh-CN').replace(/\s+/g, '');
  const uid = prefix => `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;

  function requireName(value, emptyMessage) {
    const name = String(value ?? '').trim();
    if (!name) throw new Error(emptyMessage);
    return name;
  }

  function findScene(data, sceneId) {
    const scene = data.scenes.find(entry => entry.id === sceneId);
    if (!scene) throw new Error('清单不存在');
    return scene;
  }

  function createScene(current, inputName) {
    const data = clone(current);
    const name = requireName(inputName, '请输入清单名称');
    if (data.scenes.some(scene => normalize(scene.name) === normalize(name))) throw new Error('清单名称已存在');
    const sceneId = uid('scene');
    const now = new Date().toISOString();
    data.scenes.push({ id: sceneId, name, createdAt: now, updatedAt: now, sections: [
      { id: uid('section'), name: '物品', items: [] }
    ] });
    return { data, sceneId };
  }

  globalThis.BibeiModel = Object.freeze({
    VERSION, createSeedData, validateAppData, createScene, renameScene, deleteScene, getSceneStats
  });
})();
</script>
```

`createSeedData()` 使用固定 ID，分别生成 18、24、42 件物品；每次调用返回深拷贝。`validateAppData()` 只接受 `version === 1`、`scenes` 为数组、每张清单至少有一个分区且所有必需 ID/名称/物品字段类型正确的数据。`renameScene()` 复用名称校验并更新时间；`deleteScene()` 允许删除到零张；`getSceneStats()` 只从物品实时计算 `{ checked, total, sectionCount }`。

- [ ] **Step 4: 运行模型测试并确认通过**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\full-interactive-prototype.test.cjs`

Expected: 3 tests PASS。

- [ ] **Step 5: 增加清单生命周期测试并实现缺失行为**

```js
test('rename and delete scene return new data without mutating input', () => {
  const Model = loadModel();
  const original = Model.createSeedData();
  const id = original.scenes[0].id;
  const renamed = Model.renameScene(original, id, '两天短途');
  assert.equal(original.scenes[0].name, '周末出游');
  assert.equal(renamed.scenes[0].name, '两天短途');
  const deleted = Model.deleteScene(renamed, id);
  assert.equal(deleted.scenes.some(scene => scene.id === id), false);
});
```

Run: `node --test D:\code\bibei\docs\prototypes\final-product\full-interactive-prototype.test.cjs`

Expected: 4 tests PASS。

---

### Task 2: 完成物品、分区和统计业务规则

**Files:**
- Modify: `docs/prototypes/final-product/full-interactive-prototype.html`
- Modify: `docs/prototypes/final-product/full-interactive-prototype.test.cjs`

**Interfaces:**
- Consumes: `BibeiModel.createSeedData()` 和上一任务的数据结构。
- Produces: `addItem`、`renameItem`、`moveItem`、`deleteItem`、`toggleItem`、`clearChecks`、`addSection`、`renameSection`、`moveSection`、`deleteSection`。
- All mutation signatures: `(data, sceneId, ...arguments) => newData`，不修改输入对象。

- [ ] **Step 1: 写物品操作失败测试**

```js
test('item lifecycle updates derived stats and preserves the source value', () => {
  const Model = loadModel();
  const original = Model.createSeedData();
  const scene = original.scenes[0];
  const section = scene.sections[0];
  const added = Model.addItem(original, scene.id, { name: '运动相机', sectionId: section.id });
  assert.equal(Model.getSceneStats(original, scene.id).total, 18);
  assert.equal(Model.getSceneStats(added.data, scene.id).total, 19);
  const checked = Model.toggleItem(added.data, scene.id, added.itemId);
  assert.equal(Model.getSceneStats(checked, scene.id).checked, 1);
  const renamed = Model.renameItem(checked, scene.id, added.itemId, '防水相机');
  assert.equal(renamed.scenes[0].sections[0].items.at(-1).name, '防水相机');
  const removed = Model.deleteItem(renamed, scene.id, added.itemId);
  assert.equal(Model.getSceneStats(removed, scene.id).total, 18);
});

test('items cannot be empty or duplicated across sections of one scene', () => {
  const Model = loadModel();
  const data = Model.createSeedData();
  const scene = data.scenes[0];
  assert.throws(() => Model.addItem(data, scene.id, { name: '', sectionId: scene.sections[0].id }), /请输入物品名称/);
  assert.throws(() => Model.addItem(data, scene.id, { name: '手 机', sectionId: scene.sections[0].id }), /这个物品已经在清单中/);
});
```

- [ ] **Step 2: 运行测试并确认因函数不存在而失败**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\full-interactive-prototype.test.cjs`

Expected: FAIL，错误包含 `Model.addItem is not a function`。

- [ ] **Step 3: 实现物品操作并导出接口**

实现统一定位辅助函数：

```js
function findItemContext(scene, itemId) {
  for (const section of scene.sections) {
    const index = section.items.findIndex(item => item.id === itemId);
    if (index >= 0) return { section, index, item: section.items[index] };
  }
  throw new Error('物品不存在');
}
```

`addItem` 返回 `{ data, itemId }`；`renameItem` 在当前清单全局做规范化重名校验但排除自身；`moveItem` 从原分区删除后追加到目标分区；`toggleItem` 只翻转 `checked`；`clearChecks` 将当前清单全部物品设为 `false`。

- [ ] **Step 4: 写分区规则失败测试**

```js
test('section reorder and delete never lose items', () => {
  const Model = loadModel();
  let data = Model.createSeedData();
  const sceneId = data.scenes[0].id;
  const added = Model.addSection(data, sceneId, '临时');
  data = added.data;
  const sourceId = data.scenes[0].sections[0].id;
  const before = Model.getSceneStats(data, sceneId).total;
  data = Model.moveSection(data, sceneId, added.sectionId, -1);
  data = Model.deleteSection(data, sceneId, sourceId);
  assert.equal(Model.getSceneStats(data, sceneId).total, before);
  assert.equal(data.scenes[0].sections.some(section => section.id === sourceId), false);
});

test('the final section cannot be deleted', () => {
  const Model = loadModel();
  const created = Model.createScene(Model.createSeedData(), '空清单');
  const scene = created.data.scenes.find(entry => entry.id === created.sceneId);
  assert.throws(() => Model.deleteSection(created.data, scene.id, scene.sections[0].id), /至少需要一个分区/);
});
```

- [ ] **Step 5: 实现分区操作和确定性删除规则**

`addSection` 返回 `{ data, sectionId }`；`renameSection` 做当前清单内规范化重名校验；`moveSection(data, sceneId, sectionId, direction)` 只接受 `-1` 或 `1`，到达边界时返回内容相同的新数据；`deleteSection` 把物品移动到被删分区的下一个分区，无下一个时移动到上一个分区，然后删除源分区。

- [ ] **Step 6: 运行全部模型测试**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\full-interactive-prototype.test.cjs`

Expected: 8 tests PASS，0 FAIL。

---

### Task 3: 建立可靠的本地持久化

**Files:**
- Modify: `docs/prototypes/final-product/full-interactive-prototype.html`
- Modify: `docs/prototypes/final-product/full-interactive-prototype.test.cjs`

**Interfaces:**
- Consumes: `BibeiModel.createSeedData()`、`BibeiModel.validateAppData(value)`。
- Produces: `globalThis.BibeiStorage.create(adapter, now)`，返回 `{ load(), save(data), key }`。
- `load()` returns: `{ data, warning: null | string }`；`save(data)` returns: `{ ok: boolean, warning: null | string }`。

- [ ] **Step 1: 写存储失败测试和存储脚本加载器**

```js
function loadStorage(adapter, now = () => 1724198400000) {
  const sandbox = runInlineScript('bibei-model');
  const html = readHtml();
  const match = html.match(/<script id=["']bibei-storage["'][^>]*>([\s\S]*?)<\/script>/);
  assert.ok(match, 'missing inline script: bibei-storage');
  sandbox.localStorage = adapter;
  vm.runInNewContext(match[1], sandbox, { filename: 'bibei-storage.js' });
  return sandbox.BibeiStorage.create(adapter, now);
}

function memoryStorage(seed = {}) {
  const values = new Map(Object.entries(seed));
  return {
    getItem: key => values.has(key) ? values.get(key) : null,
    setItem: (key, value) => values.set(key, String(value)),
    removeItem: key => values.delete(key),
    keys: () => Array.from(values.keys())
  };
}

test('storage saves and reloads checked state', () => {
  const Model = loadModel();
  const adapter = memoryStorage();
  const storage = loadStorage(adapter);
  let data = Model.createSeedData();
  const scene = data.scenes[0];
  const itemId = scene.sections[0].items[0].id;
  data = Model.toggleItem(data, scene.id, itemId);
  assert.equal(storage.save(data).ok, true);
  assert.equal(storage.load().data.scenes[0].sections[0].items[0].checked, true);
});

test('corrupt storage is backed up and replaced with valid seed data', () => {
  const adapter = memoryStorage({ 'bibei.fullPrototype.v1': '{broken' });
  const storage = loadStorage(adapter);
  const result = storage.load();
  assert.match(result.warning, /本地数据已损坏/);
  assert.equal(result.data.scenes.length, 3);
  assert.ok(adapter.keys().some(key => key === 'bibei.fullPrototype.v1.corrupt.1724198400000'));
});
```

- [ ] **Step 2: 运行测试并确认缺少 `bibei-storage`**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\full-interactive-prototype.test.cjs`

Expected: FAIL，错误包含 `missing inline script: bibei-storage`。

- [ ] **Step 3: 实现存储适配器**

```html
<script id="bibei-storage">
(() => {
  const KEY = 'bibei.fullPrototype.v1';
  function create(adapter, now = Date.now) {
    function load() {
      const raw = adapter.getItem(KEY);
      if (raw === null) return { data: BibeiModel.createSeedData(), warning: null };
      try {
        const parsed = JSON.parse(raw);
        if (!BibeiModel.validateAppData(parsed)) throw new Error('invalid schema');
        return { data: parsed, warning: null };
      } catch (error) {
        try { adapter.setItem(`${KEY}.corrupt.${now()}`, raw); } catch (_) {}
        return { data: BibeiModel.createSeedData(), warning: '本地数据已损坏，已恢复示例清单' };
      }
    }
    function save(data) {
      try {
        adapter.setItem(KEY, JSON.stringify(data));
        return { ok: true, warning: null };
      } catch (error) {
        return { ok: false, warning: '当前浏览器无法保存，刷新后可能丢失修改' };
      }
    }
    return Object.freeze({ key: KEY, load, save });
  }
  globalThis.BibeiStorage = Object.freeze({ create });
})();
</script>
```

- [ ] **Step 4: 增加写入失败测试并运行全部测试**

```js
test('storage write failure keeps the app usable and returns one warning', () => {
  const adapter = memoryStorage();
  adapter.setItem = () => { throw new Error('denied'); };
  const storage = loadStorage(adapter);
  const result = storage.save(loadModel().createSeedData());
  assert.equal(result.ok, false);
  assert.match(result.warning, /刷新后可能丢失修改/);
});
```

Run: `node --test D:\code\bibei\docs\prototypes\final-product\full-interactive-prototype.test.cjs`

Expected: 11 tests PASS，0 FAIL。

---

### Task 4: 完成手机视觉壳、首页和清单生命周期

**Files:**
- Modify: `docs/prototypes/final-product/full-interactive-prototype.html`
- Modify: `docs/prototypes/final-product/full-interactive-prototype.test.cjs`

**Interfaces:**
- Consumes: `BibeiModel`、`BibeiStorage`。
- Produces: `globalThis.BibeiApp`，包含 `init()`、`render()`、`navigate(screen, sceneId)` 和 `commit(nextData, successMessage)`。
- DOM actions: `data-action="open-scene|open-scene-menu|new-scene|rename-scene|delete-scene|back-home"`。

- [ ] **Step 1: 写独立文件和关键控件的静态失败测试**

```js
test('production html is standalone and declares every home action', () => {
  const html = readHtml();
  assert.doesNotMatch(html, /<script[^>]+src=/i);
  assert.doesNotMatch(html, /<link[^>]+rel=["']stylesheet/i);
  assert.doesNotMatch(html, /https?:\/\//i);
  for (const action of ['open-scene', 'open-scene-menu', 'new-scene', 'back-home']) {
    assert.match(html, new RegExp(`data-action=["']${action}["']`));
  }
});
```

- [ ] **Step 2: 运行测试并确认缺少首页控件**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\full-interactive-prototype.test.cjs`

Expected: FAIL，指出至少一个 `data-action` 不存在。

- [ ] **Step 3: 实现双主页面 HTML 与移动端 CSS**

建立：

```html
<main id="home-screen" class="screen"></main>
<main id="checklist-screen" class="screen" hidden></main>
<dialog id="form-sheet" class="sheet"></dialog>
<dialog id="action-sheet" class="sheet"></dialog>
<dialog id="confirm-dialog" class="confirm-dialog"></dialog>
<div id="toast" role="status" aria-live="polite" hidden></div>
```

CSS 使用既定米白/绿色变量，`.app-shell` 最大宽度 `430px`，主要按钮和图标按钮最小触控尺寸 `44px`，底部操作栏兼容 `env(safe-area-inset-bottom)`，为 `:focus-visible` 提供 3px 绿色轮廓，并在 `prefers-reduced-motion: reduce` 下关闭非必要动画。

- [ ] **Step 4: 实现首页渲染与清单生命周期控制器**

`BibeiApp.init()` 从 Storage 加载数据、绑定一次根级事件委托并渲染首页。首页卡片的统计全部来自 `Model.getSceneStats()`。清单卡片主体和更多按钮必须是两个独立按钮；更多按钮调用 `stopPropagation()`。新建成功后 `navigate('checklist', sceneId)`。删除当前或首页清单前调用统一 `openConfirm({ title, message, confirmLabel, danger, onConfirm })`。

- [ ] **Step 5: 运行测试并进行首页浏览器检查**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\full-interactive-prototype.test.cjs`

Expected: 12 tests PASS，0 FAIL。

Browser checks:

1. 打开 `http://127.0.0.1:4174/full-interactive-prototype.html`。
2. 依次打开三张清单，标题和总数分别为 18、24、42。
3. 新建“露营”，确认直接进入且只有“物品”分区。
4. 返回首页改名为“周末露营”，刷新后名称保持。
5. 发起删除后点击取消，确认清单仍存在；再次确认删除，清单消失。

---

### Task 5: 完成清单勾选、筛选和物品管理

**Files:**
- Modify: `docs/prototypes/final-product/full-interactive-prototype.html`
- Modify: `docs/prototypes/final-product/full-interactive-prototype.test.cjs`

**Interfaces:**
- Consumes: Task 2 的所有物品操作和 `BibeiApp.commit()`。
- DOM actions: `toggle-item`、`open-item-menu`、`add-item`、`rename-item`、`move-item`、`delete-item`、`set-filter`、`toggle-section`、`jump-section`。
- Temporary UI state: `{ screen, activeSceneId, filter, collapsedSectionIds, lastSectionId }`。

- [ ] **Step 1: 写清单控件和无占位行为的静态失败测试**

```js
test('checklist exposes real item, filter and section actions', () => {
  const html = readHtml();
  for (const action of [
    'toggle-item', 'open-item-menu', 'add-item', 'set-filter',
    'toggle-section', 'jump-section'
  ]) assert.match(html, new RegExp(`data-action=["']${action}["']`));
  assert.doesNotMatch(html, /功能未完成|仅演示|重点演示/);
});
```

- [ ] **Step 2: 运行测试并确认缺少清单动作**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\full-interactive-prototype.test.cjs`

Expected: FAIL，指出至少一个清单动作不存在。

- [ ] **Step 3: 实现清单渲染**

每次 `renderChecklist()`：

- 从活动清单重新计算总进度、未勾选数量和每个分区进度。
- `filter === 'pending'` 时只隐藏已勾选物品；分区没有待办时不渲染该分区。
- 分区折叠集合只保存在 `ui.collapsedSectionIds`。
- 物品行主体按钮执行 `toggle-item`，尾部独立按钮执行 `open-item-menu`。
- 无物品时显示“添加第一个物品”，全部完成时显示“已全部勾选，切回全部可查看完整清单”。

- [ ] **Step 4: 实现物品表单与动作菜单**

使用统一表单弹层的四种模式：`new-scene`、`rename-scene`、`add-item`、`rename-item`。添加物品的分区下拉框必有默认值：上下文分区 → `ui.lastSectionId` → 第一分区。移动物品使用动作弹层中的目标分区列表；选择当前分区时关闭菜单但不写数据。删除物品必须通过确认弹窗。

- [ ] **Step 5: 验证统计同步和刷新持久化**

Browser checks:

1. 在“周末出游”勾选两件物品，确认清单总进度、分区进度和“未勾选”数量同时变化。
2. 切换“未勾选”，确认已勾选项消失；切回“全部”恢复显示。
3. 添加“运动相机”，确认总数从 18 变 19，首页卡片也显示 19。
4. 把“运动相机”改名为“防水相机”，移动到另一个分区，然后删除；每一步都检查所在分区与统计。
5. 刷新页面，确认此前的勾选状态仍保留，而筛选恢复为“全部”。

- [ ] **Step 6: 运行全部自动化测试**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\full-interactive-prototype.test.cjs`

Expected: 13 tests PASS，0 FAIL。

---

### Task 6: 完成分区管理、危险操作和最终验收

**Files:**
- Modify: `docs/prototypes/final-product/full-interactive-prototype.html`
- Modify: `docs/prototypes/final-product/full-interactive-prototype.test.cjs`

**Interfaces:**
- Consumes: Task 2 的分区操作、`clearChecks` 和 `BibeiApp.commit()`。
- DOM actions: `manage-sections`、`add-section`、`rename-section`、`move-section-up`、`move-section-down`、`delete-section`、`clear-checks`、`rename-current-scene`、`delete-current-scene`。

- [ ] **Step 1: 写设置与分区动作的静态失败测试**

```js
test('every settings and section management action exists', () => {
  const html = readHtml();
  for (const action of [
    'manage-sections', 'add-section', 'rename-section',
    'move-section-up', 'move-section-down', 'delete-section',
    'clear-checks', 'rename-current-scene', 'delete-current-scene'
  ]) assert.match(html, new RegExp(`data-action=["']${action}["']`));
});

test('all inline scripts parse as classic JavaScript', () => {
  const html = readHtml();
  const scripts = Array.from(html.matchAll(/<script(?:\s+[^>]*)?>([\s\S]*?)<\/script>/g), match => match[1]);
  assert.ok(scripts.length >= 3);
  for (const source of scripts) new vm.Script(source);
});
```

- [ ] **Step 2: 运行测试并确认设置动作缺失**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\full-interactive-prototype.test.cjs`

Expected: FAIL，指出至少一个设置动作不存在。

- [ ] **Step 3: 实现清单设置和分区管理弹层**

分区管理每行显示名称、物品数、改名、上移、下移和删除按钮；首行禁用上移，末行禁用下移，只剩一行时禁用删除。删除非空分区的确认文案明确写出接收物品的目标分区名称，确认后调用 `Model.deleteSection()`。所有操作后重新渲染清单和管理弹层。

清单设置包含修改名称、管理分区、清空勾选和删除清单。清空勾选确认后调用 `Model.clearChecks()`；若当前没有已勾选物品则按钮禁用。删除当前清单后返回首页。

- [ ] **Step 4: 实现统一反馈、焦点和对话框关闭规则**

- 成功 toast 显示 2000ms，并替换而非叠加前一条消息。
- 输入错误写入表单内 `role="alert"` 区域，弹层保持打开并聚焦错误字段。
- 打开表单时保存触发元素，关闭后把焦点还给仍存在的触发元素。
- Escape 关闭非危险弹层；确认弹窗的取消不执行回调。
- 存储失败警告在一次页面会话中只展示一次。

- [ ] **Step 5: 运行全部自动化测试**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\full-interactive-prototype.test.cjs`

Expected: 15 tests PASS，0 FAIL。

- [ ] **Step 6: 走完整浏览器验收矩阵**

HTTP 验收：

1. 三张清单、新建清单、改名、删除及取消删除。
2. 添加、改名、移动、勾选、取消勾选和删除物品。
3. 新增、改名、上移、下移和删除空/非空分区。
4. 删除非空分区后确认总物品数不变。
5. 清空勾选后确认物品与分区均未改变。
6. 刷新后确认持久化，且首页/清单/分区的统计一致。
7. 检查浏览器控制台为 0 个运行时错误、0 个失败资源请求。

`file://` 验收：

1. 直接打开 `D:\code\bibei\docs\prototypes\final-product\full-interactive-prototype.html`。
2. 重复“进入清单 → 勾选 → 新增物品 → 刷新”流程。
3. 确认没有跨域、模块加载或网络错误。

- [ ] **Step 7: 做最终响应式和可访问性检查**

在 360×800、390×844、430×932 和桌面宽度下检查：无横向滚动；固定底栏不遮挡最后一行；所有关键触控区至少 44px；键盘 Tab 顺序合理；焦点轮廓可见；页面缩放 200% 时仍能完成所有操作。
