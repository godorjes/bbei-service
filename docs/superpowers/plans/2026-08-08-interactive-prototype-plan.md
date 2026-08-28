# Interactive First-Use Prototype Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone clickable HTML prototype of the approved “选择场景 → 对照清单 → 完成” flow.

**Architecture:** Keep UI state in a small dependency-free ES module and render it from one responsive HTML page with inline CSS. Test the state module with Node's built-in test runner, test the HTML contract statically, then validate the complete flow in a real local browser.

**Tech Stack:** HTML5, CSS3, JavaScript ES modules, Node.js 21 built-in test runner, local HTTP server, in-app browser.

## Global Constraints

- Do not modify the existing Vue application or backend.
- Keep the prototype under `docs/prototypes/interactive/`.
- Main flow must not contain “模板、写回、变更汇总”.
- Home-to-checklist takes one click.
- Mobile-first layout with one dominant next action per screen.
- The project is not a Git repository, so commit checkpoints are recorded as local verification checkpoints only.

---

### Task 1: Prototype State Model

**Files:**
- Create: `docs/prototypes/interactive/prototype-state.mjs`
- Create: `docs/prototypes/interactive/prototype-state.test.mjs`

**Interfaces:**
- Produces: `SCENES`, `createInitialState()`, `startScene(state, sceneId)`, `toggleItem(state, itemId)`, `addItem(state, name)`, `openCompletion(state)`, `resolveAddedItem(state, keep)`.
- Consumers: `prototype.html` and the Node tests.

- [ ] **Step 1: Write failing tests for initial state and one-click scene start**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import { createInitialState, startScene } from './prototype-state.mjs';

test('starts on the scene chooser', () => {
  assert.equal(createInitialState().screen, 'home');
});

test('starts a scene in one action', () => {
  const state = startScene(createInitialState(), 'weekend');
  assert.equal(state.screen, 'checklist');
  assert.equal(state.activeScene.id, 'weekend');
});
```

- [ ] **Step 2: Run tests and verify RED**

Run: `node --test D:\code\bibei\docs\prototypes\interactive\prototype-state.test.mjs`
Expected: FAIL because `prototype-state.mjs` does not exist.

- [ ] **Step 3: Implement the minimal initial state and scene transition**

```js
export function createInitialState() {
  return { screen: 'home', activeScene: null, addedItem: null };
}

export function startScene(state, sceneId) {
  return { ...state, screen: 'checklist', activeScene: structuredClone(SCENES[sceneId]) };
}
```

- [ ] **Step 4: Run tests and verify GREEN**

Run: `node --test D:\code\bibei\docs\prototypes\interactive\prototype-state.test.mjs`
Expected: 2 tests pass.

- [ ] **Step 5: Add failing tests for check, add, complete, and keep-only-this-time behavior**

```js
test('toggles an item', () => assert.equal(toggleItem(startScene(createInitialState(), 'weekend'), 'power-bank').activeScene.items.find(item => item.id === 'power-bank').checked, true));
test('adds a temporary item', () => assert.equal(addItem(startScene(createInitialState(), 'weekend'), '电脑充电器').addedItem.name, '电脑充电器'));
test('opens completion', () => assert.equal(openCompletion(startScene(createInitialState(), 'weekend')).screen, 'complete'));
test('resolves an added item without changing the original scene', () => assert.equal(resolveAddedItem(openCompletion(addItem(startScene(createInitialState(), 'weekend'), '电脑充电器')), false).screen, 'home'));
```

- [ ] **Step 6: Run tests and verify RED for missing functions**

- [ ] **Step 7: Implement immutable item and completion transitions**

- [ ] **Step 8: Run the full state test file and verify GREEN**

---

### Task 2: Responsive Single-Page Prototype

**Files:**
- Create: `docs/prototypes/interactive/prototype.html`

**Interfaces:**
- Consumes: all exports from `prototype-state.mjs`.
- Produces: buttons with stable selectors `[data-scene]`, `[data-item]`, `[data-action="add"]`, `[data-action="finish"]`, `[data-action="keep"]`, `[data-action="once"]`.

- [ ] **Step 1: Start the local server and run the browser flow assertion against the missing page**

Expected: RED because `prototype.html` returns 404 and the “这次要去哪？” heading is absent.

- [ ] **Step 2: Implement `prototype.html` with accessible buttons, modal add form, responsive phone shell, and DOM rendering**

The page imports the state API, renders each screen from `state.screen`, and attaches event delegation to the stable selectors listed above. The add dialog validates a non-empty name and returns focus to the trigger after closing.

- [ ] **Step 3: Run the state tests again and verify GREEN**

Run: `node --test D:\code\bibei\docs\prototypes\interactive\prototype-state.test.mjs`
Expected: all state behaviors pass with no warnings.

---

### Task 3: Real Browser Flow Verification

**Files:**
- Verify: `docs/prototypes/interactive/prototype.html`

**Interfaces:**
- Consumes: local URL `http://localhost:4174/prototype.html`.
- Produces: browser evidence for the complete interaction and responsive visual state.

- [ ] **Step 1: Start a local static server on port 4174**

Run: `python -m http.server 4174 --directory D:\code\bibei\docs\prototypes\interactive`

- [ ] **Step 2: Verify home screen copy and scene buttons**

- [ ] **Step 3: Click “周末出游”, toggle “充电宝”, add “电脑充电器”, and complete**

- [ ] **Step 4: Verify the completion question and choose “仅本次”**

- [ ] **Step 5: Verify the prototype returns to the home screen and capture a screenshot**

- [ ] **Step 6: Keep the local prototype server running for user review and report its URL**
