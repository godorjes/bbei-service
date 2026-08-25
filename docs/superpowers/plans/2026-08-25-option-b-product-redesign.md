# 带上方案 B Product Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the template/history workflow with reusable scenes backed by independent sections and items, matching the approved navigation option B prototype.

**Architecture:** Add normalized catalog tables and resource-oriented Spring Boot services while keeping legacy JSON tables as read-only backup. Replace the Vue routes with a two-tab mobile flow: scene checklists for daily use and an organize area for maintaining reusable sections and items.

**Tech Stack:** Java 11, Spring Boot 2.7.18, MyBatis 2.3.2, H2, Vue 3.4, Vue Router 4.4, Vite 5.4, plain JavaScript and CSS.

**Spec:** `docs/superpowers/specs/2026-08-25-option-b-product-redesign-design.md`

## Global Constraints

- Keep Java at version 11 and Spring Boot at version 2.7.18.
- Do not add login, recommendation, departure time, tags, completion pages, or history to the new flow.
- Keep the main navigation to exactly “清单” and “整理”.
- An item has one global identity and can bind to multiple sections.
- A scene can bind to multiple ordered sections.
- Deduplicate an item within a scene by item ID and place it in the earliest matching scene section.
- Preserve the legacy H2 tables and migrate active templates idempotently.
- Keep the first-version item editor name-only.
- Do not add a frontend UI framework.

---

### Task 1: Normalized catalog schema and domain contracts

**Files:**
- Modify: `backend/src/main/resources/schema.sql`
- Create: `backend/src/main/java/com/bibei/entity/SceneEntity.java`
- Create: `backend/src/main/java/com/bibei/entity/PackingSectionEntity.java`
- Create: `backend/src/main/java/com/bibei/entity/PackingItemEntity.java`
- Create: `backend/src/main/java/com/bibei/entity/SceneItemRow.java`
- Create: `backend/src/main/java/com/bibei/dto/CatalogModels.java`
- Create: `backend/src/test/java/com/bibei/CatalogSchemaIntegrationTest.java`

**Interfaces:**
- Produces: `CatalogModels.SaveSceneRequest`, `SaveSectionRequest`, `SaveItemRequest`, `CheckedRequest`, `SceneSummary`, `SceneDetail`, `SectionView`, and `ItemView`.
- Produces: six normalized tables plus `app_migration(version, applied_at)`.

- [ ] **Step 1: Write a failing schema smoke test**

```java
@SpringBootTest
class CatalogSchemaIntegrationTest {
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void normalizedCatalogTablesAreCreated() {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM scene", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM packing_section", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM packing_item", Integer.class)).isZero();
    }
}
```

- [ ] **Step 2: Run the test and verify the missing-table failure**

Run: `mvn -Dtest=CatalogSchemaIntegrationTest test`

Expected: FAIL because `scene`, `packing_section`, or `packing_item` does not exist.

- [ ] **Step 3: Add normalized tables and DTO/entity contracts**

The SQL must create `scene`, `packing_section`, `packing_item`, `scene_section`, `section_item`, `scene_item_state`, and `app_migration`. Relationship tables use composite primary keys and foreign keys with `ON DELETE CASCADE`; `scene_section` stores `sort_order INT NOT NULL`.

The request contracts are:

```java
public static class SaveSceneRequest {
    @NotBlank(message = "请输入场景名称")
    @Size(max = 50, message = "场景名称不能超过50个字")
    public String name;
    public List<Long> sectionIds = new ArrayList<>();
}

public static class SaveSectionRequest {
    @NotBlank(message = "请输入分区名称")
    @Size(max = 30, message = "分区名称不能超过30个字")
    public String name;
    public List<Long> sceneIds = new ArrayList<>();
}

public static class SaveItemRequest {
    @NotBlank(message = "请输入物品名称")
    @Size(max = 50, message = "物品名称不能超过50个字")
    public String name;
    public List<Long> sectionIds = new ArrayList<>();
}
```

- [ ] **Step 4: Run the schema smoke test**

Run: `mvn -Dtest=CatalogSchemaIntegrationTest test`

Expected: PASS.

### Task 2: Scene persistence and deduplicated checklist service

**Files:**
- Create: `backend/src/main/java/com/bibei/mapper/SceneMapper.java`
- Create: `backend/src/main/java/com/bibei/service/SceneService.java`
- Create: `backend/src/test/java/com/bibei/SceneServiceIntegrationTest.java`

**Interfaces:**
- Consumes: normalized schema and DTOs from Task 1.
- Produces: `list()`, `get(long)`, `create(SaveSceneRequest)`, `update(long, SaveSceneRequest)`, `delete(long)`, `setChecked(long,long,boolean)`, and `reset(long)`.

- [ ] **Step 1: Write failing scene behavior tests**

```java
@Test
void sharedItemAppearsOnceInEarliestSceneSection() {
    SceneDetail detail = sceneService.get(fixtures.sceneWithOneItemInTwoSections());
    assertThat(detail.sections.get(0).items).extracting(item -> item.name).containsExactly("充电器");
    assertThat(detail.sections.get(1).items).isEmpty();
    assertThat(detail.totalCount).isEqualTo(1);
}

@Test
void checkedStatePersistsAndResetClearsIt() {
    long sceneId = fixtures.simpleScene();
    long itemId = sceneService.get(sceneId).sections.get(0).items.get(0).id;
    sceneService.setChecked(sceneId, itemId, true);
    assertThat(sceneService.get(sceneId).checkedCount).isEqualTo(1);
    sceneService.reset(sceneId);
    assertThat(sceneService.get(sceneId).checkedCount).isZero();
}
```

- [ ] **Step 2: Run the tests and verify missing service failures**

Run: `mvn -Dtest=SceneServiceIntegrationTest test`

Expected: FAIL because `SceneService` and `SceneMapper` do not exist.

- [ ] **Step 3: Implement scene CRUD, ordered bindings, checklist assembly, and checked state**

`SceneMapper.findChecklistRows(sceneId)` returns rows ordered by `scene_section.sort_order`, then item name. `SceneService.get` builds sections in insertion order and tracks a `Set<Long> seenItemIds`; a repeated item is skipped after its first occurrence. `setChecked` first verifies that the item is currently visible in the scene, then inserts or deletes `scene_item_state` in one transaction.

- [ ] **Step 4: Run scene service tests**

Run: `mvn -Dtest=SceneServiceIntegrationTest test`

Expected: PASS, including deduplication, order, persistence, reset, duplicate-name rejection, and scene deletion without deleting sections/items.

### Task 3: Independent section and item services

**Files:**
- Create: `backend/src/main/java/com/bibei/mapper/SectionMapper.java`
- Create: `backend/src/main/java/com/bibei/mapper/ItemMapper.java`
- Create: `backend/src/main/java/com/bibei/service/SectionService.java`
- Create: `backend/src/main/java/com/bibei/service/ItemService.java`
- Create: `backend/src/test/java/com/bibei/CatalogBindingIntegrationTest.java`

**Interfaces:**
- Produces: section and item list/get/create/update/delete methods.
- Produces: `ItemService.list(String query)` with case-insensitive trimmed-name matching.

- [ ] **Step 1: Write failing many-to-many and deletion tests**

```java
@Test
void itemCanBindToSeveralSections() {
    ItemView item = itemService.create(fixtures.itemRequest("充电器", sectionA, sectionB));
    assertThat(item.sectionIds).containsExactlyInAnyOrder(sectionA, sectionB);
}

@Test
void deletingSectionKeepsItsItemsAsUnbound() {
    long itemId = itemService.create(fixtures.itemRequest("雨伞", sectionA)).id;
    sectionService.delete(sectionA);
    assertThat(itemService.get(itemId).sectionIds).isEmpty();
}
```

- [ ] **Step 2: Run tests and verify failure**

Run: `mvn -Dtest=CatalogBindingIntegrationTest test`

Expected: FAIL because the services do not exist.

- [ ] **Step 3: Implement binding updates without disturbing scene section order**

When editing a section, remove scene bindings no longer selected and append only newly selected bindings using `MAX(sort_order) + 1`; do not delete and recreate existing `scene_section` rows. Item bindings may be replaced transactionally because they have no display-order column.

Names are trimmed and compared with `LOWER(TRIM(name))`. Creating or renaming to an existing normalized name throws `IllegalArgumentException` with `“名称”已经存在`.

- [ ] **Step 4: Run binding tests**

Run: `mvn -Dtest=CatalogBindingIntegrationTest test`

Expected: PASS for multiple bindings, unbound items, search, duplicate prevention, and safe deletes.

### Task 4: Idempotent legacy-template migration and starter data

**Files:**
- Create: `backend/src/main/java/com/bibei/config/CatalogDataInitializer.java`
- Modify: `backend/src/main/java/com/bibei/config/SeedDataRunner.java`
- Create: `backend/src/test/java/com/bibei/CatalogMigrationIntegrationTest.java`

**Interfaces:**
- Consumes: `TemplateService.list(false)` and normalized services.
- Produces: migration version `option-b-catalog-v1`.

- [ ] **Step 1: Write a failing idempotent migration test**

```java
@Test
void activeTemplatesMigrateOnceAndMergeNames() {
    initializer.initialize();
    int firstSceneCount = sceneService.list().size();
    long chargerCount = itemService.list("手机充电器").stream()
        .filter(item -> item.name.equals("手机充电器")).count();
    initializer.initialize();
    assertThat(sceneService.list()).hasSize(firstSceneCount);
    assertThat(chargerCount).isEqualTo(1);
}
```

- [ ] **Step 2: Run the migration test and verify failure**

Run: `mvn -Dtest=CatalogMigrationIntegrationTest test`

Expected: FAIL because `CatalogDataInitializer` does not exist.

- [ ] **Step 3: Implement initialization order**

If `app_migration` already contains `option-b-catalog-v1`, return. Otherwise, migrate active legacy templates when present; when none exist, create the three starter scenes and shared normalized sections/items directly. Insert the migration marker only after the transaction succeeds. Disable the old runner from seeding additional legacy templates.

- [ ] **Step 4: Run migration and existing backend tests**

Run: `mvn test`

Expected: PASS. Existing legacy flow tests remain valid because old tables and services are retained.

### Task 5: Resource-oriented REST endpoints

**Files:**
- Create: `backend/src/main/java/com/bibei/controller/SceneController.java`
- Create: `backend/src/main/java/com/bibei/controller/SectionController.java`
- Create: `backend/src/main/java/com/bibei/controller/ItemController.java`
- Create: `backend/src/test/java/com/bibei/CatalogApiIntegrationTest.java`

**Interfaces:**
- Produces: `/api/scenes`, `/api/sections`, `/api/items`, scene checked-state, and reset endpoints exactly as specified in the design.

- [ ] **Step 1: Write failing MockMvc contract tests**

```java
mockMvc.perform(post("/api/scenes")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"骑行\",\"sectionIds\":[]}"))
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.name").value("骑行"));

mockMvc.perform(put("/api/scenes/{sceneId}/items/{itemId}/checked", sceneId, itemId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"checked\":true}"))
    .andExpect(status().isNoContent());
```

- [ ] **Step 2: Run API tests and verify 404 failures**

Run: `mvn -Dtest=CatalogApiIntegrationTest test`

Expected: FAIL with 404 for the new endpoints.

- [ ] **Step 3: Add thin validated controllers**

Controllers delegate to services, return 201 for create, 204 for delete/reset/check-state, and reuse `ApiExceptionHandler` for 400/404 responses. `GET /api/items?query=充电` performs search; numeric item lookup uses `/api/items/{id:\\d+}` so it cannot conflict with the legacy `/api/items/search` endpoint.

- [ ] **Step 4: Run all backend tests**

Run: `mvn test`

Expected: PASS.

### Task 6: Frontend API boundary, routes, and shared editor components

**Files:**
- Modify: `frontend/src/api.js`
- Modify: `frontend/src/router.js`
- Modify: `frontend/src/components/BottomNav.vue`
- Create: `frontend/src/components/SceneEditorSheet.vue`
- Create: `frontend/src/components/CatalogEditorSheet.vue`
- Create: `frontend/src/components/SceneItemSheet.vue`
- Create: `frontend/src/catalog.js`
- Create: `frontend/tests/catalog.test.js`
- Modify: `frontend/package.json`

**Interfaces:**
- Produces: `scenesApi`, `sectionsApi`, and `itemsApi` resource clients.
- Produces: routes `/`, `/scenes/:id`, and `/organize`; all legacy routes redirect to `/`.

- [ ] **Step 1: Write failing pure-state tests**

```javascript
import test from 'node:test'
import assert from 'node:assert/strict'
import { moveId, progressText } from '../src/catalog.js'

test('moveId reorders a selected section without mutating input', () => {
  const ids = [1, 2, 3]
  assert.deepEqual(moveId(ids, 2, -1), [2, 1, 3])
  assert.deepEqual(ids, [1, 2, 3])
})

test('progressText keeps first-use copy direct', () => {
  assert.equal(progressText(0, 4), '还有 4 件未确认')
  assert.equal(progressText(4, 4), '已全部准备好')
})
```

- [ ] **Step 2: Add the test script and verify failure**

Run: `npm test`

Expected: FAIL because `catalog.js` does not exist.

- [ ] **Step 3: Implement helpers, API clients, route redirects, and name-only sheets**

`moveId(ids, id, direction)` returns a copied reordered array with boundary checks. Editor sheets emit validated payloads and never mutate prop arrays. `SceneItemSheet` lists matching existing items first and emits either an existing item ID with selected section IDs or a new name with selected section IDs.

- [ ] **Step 4: Run frontend state tests and build**

Run: `npm test`

Expected: PASS.

Run: `npm run build`

Expected: PASS.

### Task 7: Scene list home and scene management

**Files:**
- Modify: `frontend/src/views/HomeView.vue`
- Use: `frontend/src/components/SceneEditorSheet.vue`
- Use: `frontend/src/components/BottomNav.vue`

**Interfaces:**
- Consumes: `scenesApi.list/create/update/remove`.
- Produces: direct scene opening and scene create/edit/delete interaction.

- [ ] **Step 1: Replace the home view with option B scene cards**

The heading is “带上” with the helper line “选择一个场景，直接开始准备”. Each card renders `checkedCount / totalCount`, opens `/scenes/:id`, and exposes a small edit action that does not trigger card navigation.

- [ ] **Step 2: Wire scene editor save/delete and reload behavior**

Create and update send `{ name, sectionIds }`; deletion requires confirmation and leaves sections/items untouched. Show empty state “还没有场景” with one “创建场景” action.

- [ ] **Step 3: Build frontend**

Run: `npm run build`

Expected: PASS without Vue template warnings.

### Task 8: Direct reusable scene checklist

**Files:**
- Modify: `frontend/src/views/ChecklistView.vue`
- Use: `frontend/src/components/SceneItemSheet.vue`
- Use: `frontend/src/components/SceneEditorSheet.vue`

**Interfaces:**
- Consumes: `scenesApi.get/setChecked/reset` and `itemsApi.create/update`.
- Produces: immediate checklist interaction with 3-second completion toast.

- [ ] **Step 1: Replace list-instance logic with scene detail loading**

Render `scene.sections` as returned by the backend; do not regroup or duplicate items in the browser. Display `checkedCount / totalCount`, section counts, and an empty-section message without a completion button.

- [ ] **Step 2: Add optimistic checked-state updates with rollback**

Toggle the item locally, adjust `checkedCount`, call `setChecked`, and restore the previous state if the request fails. When the new count equals a non-zero total, display “已全部准备好” for 3000 ms.

- [ ] **Step 3: Add global item reuse and reset**

“添加物品” defaults to the first scene section. Selecting an existing item updates its section bindings by union; a new name creates one global item. “重新准备” asks for confirmation, calls reset, and clears all local checked flags.

- [ ] **Step 4: Run tests and build**

Run: `npm test`

Expected: PASS.

Run: `npm run build`

Expected: PASS.

### Task 9: Organize sections and items

**Files:**
- Create: `frontend/src/views/OrganizeView.vue`
- Use: `frontend/src/components/CatalogEditorSheet.vue`
- Use: `frontend/src/components/BottomNav.vue`

**Interfaces:**
- Consumes: section and item CRUD APIs plus scene summaries.
- Produces: the “整理” page with “分区/物品” internal tabs.

- [ ] **Step 1: Implement section mode**

List each section with item count and bound scene names. The editor saves `{ name, sceneIds }`. New sections may be unbound. Deletion shows the exact consequence: items remain and may become unbound.

- [ ] **Step 2: Implement item mode and quiet unbound hint**

Search filters by name through the backend query. Each item shows bound section names. The editor saves `{ name, sectionIds }`. When unbound items exist, show only a small dot and “N 个未绑定”; clicking it filters to those items and shows a short toast explanation.

- [ ] **Step 3: Run tests and build**

Run: `npm test`

Expected: PASS.

Run: `npm run build`

Expected: PASS.

### Task 10: Visual replacement, integration verification, and cleanup

**Files:**
- Modify: `frontend/src/styles.css`
- Modify: `frontend/src/components/IconSymbol.vue`
- Verify: `README.md`
- Verify: `docs/prototypes/final-product/navigation-option-b.html`

**Interfaces:**
- Consumes: all completed backend and frontend features.
- Produces: responsive option B UI and verified runnable application.

- [ ] **Step 1: Replace obsolete workflow styles with option B styles**

Use the prototype’s warm neutral background, green accent, 480px mobile frame, fixed two-item navigation, card/list hierarchy, bottom sheets, safe-area padding, focus states, reduced-motion rule, and desktop preview frame. Remove visual selectors used only by history/completion/template pages.

- [ ] **Step 2: Run the full automated checks**

Run in `backend`: `mvn test`

Expected: all backend tests PASS.

Run in `frontend`: `npm test`

Expected: all Node tests PASS.

Run in `frontend`: `npm run build`

Expected: Vite build completes without errors.

- [ ] **Step 3: Start both applications and verify the critical flow**

Verify in the browser at mobile width:

1. Home opens directly with scene cards.
2. Creating a scene and binding two sections persists after refresh.
3. One item bound to two selected sections appears once in the scene.
4. Checking an item persists after refresh.
5. “重新准备” clears the scene.
6. Editing a shared section changes every bound scene.
7. Deleting a section keeps the item and shows it as unbound.
8. Legacy routes redirect to the new home.

- [ ] **Step 4: Inspect changes and record final evidence**

Run: `git -c safe.directory=D:/code/bibei diff --check`

Expected: no whitespace errors.

Run: `git -c safe.directory=D:/code/bibei status --short`

Expected: only intentional source, test, spec, and plan changes are listed; generated `target`, `dist`, database, and log files remain ignored.
