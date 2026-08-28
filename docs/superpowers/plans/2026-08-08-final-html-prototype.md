# Final Ungrouped HTML Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone, mobile-first interactive HTML prototype and screenshots for the approved ungrouped “带上” product flow.

**Architecture:** Keep prototype business state in a dependency-free UMD JavaScript module that can run both under Node tests and from `file://` in a browser. Render the UI from a standalone HTML file with inline CSS and event delegation. The prototype models scenes, one active preparation per scene, a reusable item catalog, flat checklists, persistent additions, completion history, and a three-second completion toast.

**Tech Stack:** HTML5, CSS3, browser JavaScript, Node.js built-in `node:test`, Codex in-app browser.

## Global Constraints

- Do not expose groups, tags, templates, write-back, or an independent completion page.
- A scene card opens its checklist in one click and resumes its current preparation when one exists.
- Adding an item requires only its name and permanently adds it to the scene by default.
- Checklist items render as one stable flat list with “全部/未完成” filtering.
- Completing returns directly home and shows “准备完成，可以出发了” for three seconds.
- The prototype must work when opened directly through `file://` without a development server.
- Create new files under `docs/prototypes/final-product/`; do not overwrite the existing interactive prototype.
- `D:\code\bibei` is not a Git repository, so commit steps are documented but cannot be executed unless the user later initializes Git.

---

### Task 1: Prototype State Model

**Files:**
- Create: `docs/prototypes/final-product/final-product-state.js`
- Create: `docs/prototypes/final-product/final-product-state.test.cjs`

**Interfaces:**
- Produces: global/CommonJS object `BibeiState`.
- Produces: `createInitialState()`, `openScene(state, sceneId)`, `toggleItem(state, itemId)`, `addItem(state, input)`, `createScene(state, name)`, `restartPreparation(state)`, `abandonPreparation(state)`, `completePreparation(state, force)`, `setFilter(state, filter)`, `getVisibleItems(state)`, `searchCatalog(state, query)`, and `dismissToast(state)`.
- State shape: `{ screen, scenes, catalog, preparations, activeSceneId, filter, history, toast }`.

- [x] **Step 1: Write failing state tests**

```js
const test = require('node:test');
const assert = require('node:assert/strict');
const model = require('./final-product-state.js');

test('opening a scene creates one resumable preparation with a flat item list', () => {
  let state = model.createInitialState();
  state = model.openScene(state, 'weekend');
  const first = state.preparations.weekend;
  state = model.openScene(state, 'weekend');
  assert.equal(state.preparations.weekend.id, first.id);
  assert.ok(first.items.length > 0);
  assert.equal(first.items.some((item) => 'group' in item), false);
});

test('a newly typed item is persisted to the scene and current preparation', () => {
  let state = model.openScene(model.createInitialState(), 'weekend');
  state = model.addItem(state, { name: '折叠伞', quantity: 1, note: '' });
  assert.ok(state.scenes.find((scene) => scene.id === 'weekend').items.some((item) => item.name === '折叠伞'));
  assert.ok(state.preparations.weekend.items.some((item) => item.name === '折叠伞'));
});

test('completion requires confirmation when pending items remain then archives and returns home', () => {
  let state = model.openScene(model.createInitialState(), 'weekend');
  const blocked = model.completePreparation(state, false);
  assert.equal(blocked.requiresConfirmation, true);
  const completed = model.completePreparation(state, true);
  assert.equal(completed.requiresConfirmation, false);
  assert.equal(completed.state.screen, 'home');
  assert.equal(completed.state.toast.message, '准备完成，可以出发了');
  assert.equal(completed.state.history.length, 1);
  assert.equal(completed.state.preparations.weekend, undefined);
});
```

- [x] **Step 2: Run tests to verify failure**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\final-product-state.test.cjs`

Expected: FAIL because `final-product-state.js` does not exist or does not export the required functions.

- [x] **Step 3: Implement the minimal state module**

Implement a UMD wrapper so the same API is assigned to `module.exports` in Node and `globalThis.BibeiState` in the browser. Seed three scenes and a normalized reusable catalog. All state functions return cloned state objects. `addItem` rejects normalized duplicates in the active scene, reuses a catalog item when possible, and updates both the scene and preparation. `completePreparation` returns `{ state, requiresConfirmation, remaining }` without mutation.

Core normalization and wrapper:

```js
(function (root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.BibeiState = api;
}(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  const normalizeName = (value) => String(value || '').trim().toLocaleLowerCase('zh-CN').replace(/\s+/g, '');
  // State constructors and immutable transitions are returned below.
  return { createInitialState, openScene, toggleItem, addItem, createScene, restartPreparation,
    abandonPreparation, completePreparation, setFilter, getVisibleItems, searchCatalog, dismissToast };
}));
```

- [x] **Step 4: Run state tests**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\final-product-state.test.cjs`

Expected: all tests PASS.

- [x] **Step 5: Record completion**

No Git commit is possible in the current folder. Mark Task 1 complete in this plan after recording the passing command output.

---

### Task 2: Mobile-First Interactive UI

**Files:**
- Create: `docs/prototypes/final-product/final-product-ui.js`
- Create: `docs/prototypes/final-product/prototype.html`
- Modify: `docs/prototypes/final-product/final-product-state.test.cjs`

**Interfaces:**
- Consumes: `window.BibeiState` from `final-product-state.js`.
- Consumes: `window.BibeiUI.renderApp(state)` from `final-product-ui.js`.
- Produces: DOM root `#app`, dialogs `#item-sheet`, `#scene-dialog`, `#confirm-dialog`, and `#history-dialog`.
- Produces: event actions `open-scene`, `toggle-item`, `set-filter`, `open-add`, `choose-suggestion`, `save-item`, `complete`, `force-complete`, `restart`, `abandon`, `create-scene`, `history`, and `back`.

- [x] **Step 1: Add failing tests for real rendered output**

```js
const ui = require('./final-product-ui.js');

test('home rendering gives a first-time user direct scene choices', () => {
  const html = ui.renderApp(model.createInitialState());
  assert.match(html, /这次要去哪？/);
  assert.match(html, /周末出游/);
});

test('checklist rendering is flat and exposes the approved primary actions', () => {
  const state = model.openScene(model.createInitialState(), 'weekend');
  const html = ui.renderApp(state);
  assert.match(html, /添加物品/);
  assert.match(html, /完成准备/);
  assert.doesNotMatch(html, /创建分组|选择分组|以后也带|仅本次|写回模板/);
});
```

- [x] **Step 2: Run tests to verify the HTML contract fails**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\final-product-state.test.cjs`

Expected: FAIL because `final-product-ui.js` does not exist or does not export `renderApp`.

- [x] **Step 3: Build the standalone HTML shell and visual system**

Create a UMD renderer that exports `renderApp(state)` to Node and assigns it to `window.BibeiUI` in the browser. Create a responsive 390px mobile canvas using warm ivory surfaces, forest green primary actions, dark ink typography, rounded scene cards, clear 44px touch targets, bottom safe-area padding, and reduced-motion support. Load the modules with:

```html
<script src="./final-product-state.js"></script>
<script src="./final-product-ui.js"></script>
```

Render the home page with three scenario cards, a secondary history action, and a dashed create-scene card. Render the checklist with stable flat rows, progress, filters, fixed add/complete actions, and no group headings.

- [x] **Step 4: Implement dialogs and event delegation**

The add-item sheet must search reusable catalog items while typing, add an existing suggestion in one click, or create the typed name with optional quantity/note. Completing with pending items opens a confirmation dialog; forcing completion returns home and schedules `dismissToast` after 3000ms. History is read-only. Restart and abandon require confirmation.

- [x] **Step 5: Run all prototype tests**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\final-product-state.test.cjs`

Expected: all tests PASS and forbidden product terms are absent from user-visible HTML.

- [x] **Step 6: Record completion**

No Git commit is possible in the current folder. Mark Task 2 complete after recording the passing command output.

---

### Task 3: Browser Verification and UI Screenshots

**Files:**
- Create: `docs/prototypes/final-product/home.png`
- Create: `docs/prototypes/final-product/checklist.png`
- Create: `docs/prototypes/final-product/add-item.png`
- Modify: `docs/superpowers/plans/2026-08-08-final-html-prototype.md`

**Interfaces:**
- Consumes: `file:///D:/code/bibei/docs/prototypes/final-product/prototype.html`.
- Produces: verified interactive prototype and three 390×844-oriented screenshots.

- [x] **Step 1: Open the prototype in the in-app browser**

Navigate to `file:///D:/code/bibei/docs/prototypes/final-product/prototype.html` and inspect the visible page tree.

- [x] **Step 2: Verify the main flow**

Perform: home → click “周末出游” → toggle one item → switch to “未完成” → open “添加物品” → type “折叠伞” → save → confirm the new item appears without any group selection.

- [x] **Step 3: Verify completion behavior**

Click “完成准备”, confirm the pending-items dialog, and verify the browser returns directly home with the completion toast. Verify there is no completion screen and the toast disappears after three seconds.

- [x] **Step 4: Capture screenshots**

Capture home, checklist, and open add-item sheet states to the three PNG targets. Screenshots must show no group headings or tags and must preserve the mobile canvas without horizontal clipping.

- [x] **Step 5: Run final verification**

Run: `node --test D:\code\bibei\docs\prototypes\final-product\final-product-state.test.cjs`

Expected: all tests PASS. Re-open the HTML once after capture to confirm it still loads from `file://` without console-visible failure state.

- [x] **Step 6: Record completion**

Mark all plan checkboxes complete and report the HTML and screenshot paths to the user. No Git commit is possible in the current folder.

## Execution Record

- Final test run: 15 passed, 0 failed (`node final-product-state.test.cjs`).
- Syntax checks: state module, UI renderer, and test file all exited with code 0.
- Browser verification: main preparation flow, pending confirmation, direct return home, three-second toast dismissal, history, and custom-scene creation all passed with no console warnings or errors.
- Browser automation used `http://127.0.0.1:4173/prototype.html` because automation cannot navigate to `file://`; the deliverable itself remains standalone and uses only relative local scripts.
- Screenshots generated: `home.png`, `checklist.png`, and `add-item.png`.
