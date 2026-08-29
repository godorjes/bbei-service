import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';

const require = createRequire(import.meta.url);
const postcss = require('postcss');
const vueTemplateCompiler = require('vue-template-compiler');
const cssPath = new URL('../src/assets/styles.css', import.meta.url);
const appPath = new URL('../src/App.vue', import.meta.url);

async function testLightSurfaceContract() {
  const css = await readFile(cssPath, 'utf8');
  const root = postcss.parse(css);
  const customProperties = new Map();
  const declarations = new Map();

  root.walkRules((rule) => {
    const values = declarations.get(rule.selector) || new Map();
    rule.walkDecls((decl) => {
      values.set(decl.prop, decl.value.trim().toLowerCase());
      if (rule.selector === ':root' && decl.prop.startsWith('--')) {
        customProperties.set(decl.prop, decl.value.trim().toLowerCase());
      }
    });
    declarations.set(rule.selector, values);
  });

  const value = (selector, property) => {
    const raw = declarations.get(selector)?.get(property);
    const variable = raw?.match(/^var\((--[^)]+)\)$/)?.[1];
    return variable ? customProperties.get(variable) : raw;
  };

  assert.equal(value('.phone', 'background'), '#f7f7f5', '手机页面应使用浅暖灰背景');
  assert.equal(value('.phone', 'color'), '#18181b', '手机页面应使用深色正文');
  assert.equal(value('.modal', 'background'), '#ffffff', '底部弹层应为白色');
  assert.equal(value('.modal', 'border-radius'), '24px 24px 0 0', '底部弹层应使用 24px 顶部圆角');
  assert.equal(value('.primary:disabled', 'background'), '#e2e7ed', '禁用主按钮应为浅灰色');
  assert.equal(customProperties.get('--bottom-nav-height'), 'calc(70px + env(safe-area-inset-bottom, 0px))', '底部导航高度应统一包含安全区');
  assert.equal(value('.tag-view-backdrop', 'inset'), '48px 0 var(--bottom-nav-height)', '标签页应避开安全区导航');
  assert.equal(value('.view', 'padding-bottom'), 'calc(var(--bottom-nav-height) + 22px)', '首页内容应避开安全区导航');
  assert.equal(value('.pin-btn', 'width'), '32px', '置顶按钮应保留足够触控面积');
  assert.equal(value('.delete-confirm-dialog', 'background'), '#ffffff', '删除确认层应使用白色卡片');
  assert.equal(value('.delete-confirm-backdrop', 'z-index'), '80', '删除确认层应覆盖其他操作弹层');
  assert.equal(value('.delete-confirm-btn', 'background'), '#d9535f', '最终删除按钮应使用明确的危险色');
}

async function testSceneIconsRenderAsLineSvg() {
let icons;
  try {
    icons = require('../src/ui-icons.cjs');
  } catch {
    icons = {};
  }

  assert.equal(typeof icons.sceneIconSvg, 'function', '应提供场景图标渲染函数');
  const airplane = icons.sceneIconSvg('✈️');
  assert.match(airplane, /^<svg\b/, '场景图标应输出 SVG');
  assert.match(airplane, /viewBox="0 0 24 24"/, '场景图标应使用统一坐标系');
  assert.doesNotMatch(airplane, /✈/, '界面不应继续显示彩色 Emoji');
  assert.notEqual(airplane, icons.sceneIconSvg('💼'), '不同场景图标应保留可辨识性');
  assert.notEqual(icons.cardIconSvg('手机'), icons.cardIconSvg('钱包'), '卡片图标应根据名称保持可辨识性');
  assert.match(icons.uiIconSvg('search'), /<circle\b/, '搜索图标应使用线性 SVG 图形');
  assert.notEqual(icons.uiIconSvg('trash'), icons.uiIconSvg('generic'), '删除确认层应使用专用删除图标');
}

async function loadAppComponent(fakeApi) {
  const appSource = await readFile(appPath, 'utf8');
  const descriptor = vueTemplateCompiler.parseComponent(appSource);
  const scriptBody = descriptor.script.content
    .replace(/^import \{ api \} from '.\/api';\s*$/m, '')
    .replace(/^const \{ sceneIconSvg, uiIconSvg, cardIconSvg \} = require\('.\/ui-icons\.cjs'\);\s*$/m, '')
    .replace('export default', 'return');
  const component = new Function('api', 'sceneIconSvg', 'uiIconSvg', 'cardIconSvg', scriptBody)(
    fakeApi,
    () => '',
    () => '',
    () => ''
  );
  const vm = component.data();
  for (const [name, method] of Object.entries(component.methods)) vm[name] = method.bind(vm);
  return { appSource, component, descriptor, vm };
}

async function testRenderedTemplateCompatibilityContract() {
  const appSource = await readFile(appPath, 'utf8');
  const descriptor = vueTemplateCompiler.parseComponent(appSource);
  const compiled = vueTemplateCompiler.compile(descriptor.template.content, { outputSourceRange: true });
  assert.equal(compiled.errors.length, 0, 'App 模板应能正常编译');

  const buttons = [];
  const visibleText = [];
  const visit = (node) => {
    if (!node) return;
    if (node.type === 1 && node.tag === 'button') buttons.push(node.attrsMap || {});
    if (node.type === 3 && node.text?.trim()) visibleText.push(node.text.trim());
    for (const child of node.children || []) visit(child);
    for (const condition of node.ifConditions || []) {
      if (condition.block !== node) visit(condition.block);
    }
  };
  visit(compiled.ast);

  assert.ok(buttons.some((attrs) => attrs['aria-label'] === '返回'), '返回按钮应提供稳定的无障碍名称');
  assert.ok(buttons.some((attrs) => attrs['aria-label'] === '重置'), '重置按钮应提供稳定的无障碍名称');
  assert.doesNotMatch(visibleText.join(''), /🎉/, '可见模板文字不应残留彩色 Emoji');
}

async function testFixedManagementNavigationContract() {
  const [appSource, css] = await Promise.all([
    readFile(appPath, 'utf8'),
    readFile(cssPath, 'utf8')
  ]);
  const descriptor = vueTemplateCompiler.parseComponent(appSource);
  const compiled = vueTemplateCompiler.compile(descriptor.template.content, { outputSourceRange: true });
  assert.equal(compiled.errors.length, 0, 'App 模板应能正常编译');

  const elements = [];
  const visit = (node) => {
    if (!node) return;
    if (node.type === 1) elements.push(node);
    for (const child of node.children || []) visit(child);
    for (const condition of node.ifConditions || []) {
      if (condition.block !== node) visit(condition.block);
    }
  };
  const textOf = (node) => (node?.children || [])
    .map((child) => child.type === 3 ? child.text.trim() : textOf(child))
    .join('');
  visit(compiled.ast);

  const tabButtons = elements.filter((node) => node.tag === 'button' && node.attrsMap?.role === 'tab');
  assert.deepEqual(tabButtons.map(textOf), ['卡片', '标签'], '管理页应提供卡片和标签两个切换 Tab');
  assert.equal(tabButtons[0]?.attrsMap?.['@click'], "managementTab = 'cards'", '卡片 Tab 应切换到卡片面板');
  assert.equal(tabButtons[1]?.attrsMap?.['@click'], "managementTab = 'tags'", '标签 Tab 应切换到标签面板');

  const bottomNav = elements.find((node) => node.attrsMap?.class === 'bottom-nav');
  assert.equal(bottomNav?.attrsMap?.role, 'navigation', '底部区域应声明为主导航');
  assert.match(textOf(bottomNav), /管理/, '底部导航应使用“管理”入口承载卡片和标签');

  const root = postcss.parse(css);
  let hasFixedMobileNav = false;
  root.walkAtRules('media', (media) => {
    if (!/max-width:\s*430px/.test(media.params)) return;
    media.walkRules('.bottom-nav', (rule) => {
      rule.walkDecls('position', (decl) => {
        if (decl.value.trim().toLowerCase() === 'fixed') hasFixedMobileNav = true;
      });
    });
  });
  assert.ok(hasFixedMobileNav, '移动端底部导航应固定在视口底部');
}

async function testDeleteRequiresConfirmation() {
  const apiCalls = [];
  const fakeApi = {
    deleteCard: async (id) => apiCalls.push(`delete-card:${id}`),
    tags: async () => ({ data: [] }),
    scenes: async () => ({ data: { pinned: [], records: [], total: 0, page: 1 } }),
    cards: async () => ({ data: { records: [], total: 0, totalPages: 0, page: 1, size: 100 } })
  };
  const { descriptor, vm } = await loadAppComponent(fakeApi);

  const card = { id: 7, title: '钱包' };
  vm.requestDelete('card', card);
  assert.deepEqual(apiCalls, [], '请求删除时不应立即调用删除接口');
  assert.equal(vm.showDeleteConfirm, true, '请求删除后应显示确认层');
  assert.equal(vm.deleteTarget.item, card, '确认层应保存当前待删除对象');

  await vm.confirmDelete();
  assert.deepEqual(apiCalls, ['delete-card:7'], '仅在确认后调用对应删除接口');
  assert.equal(vm.showDeleteConfirm, false, '删除完成后应关闭确认层');

  const compiled = vueTemplateCompiler.compile(descriptor.template.content, { outputSourceRange: true });
  assert.equal(compiled.errors.length, 0, 'App 模板应能正常编译');
  const elements = [];
  const nestedButtons = [];
  const visit = (node, insideButton = false) => {
    if (!node) return;
    if (node.type === 1) {
      elements.push(node);
      if (insideButton && node.tag === 'button') nestedButtons.push(node);
    }
    const nextInsideButton = insideButton || node.tag === 'button';
    for (const child of node.children || []) visit(child, nextInsideButton);
    for (const condition of node.ifConditions || []) {
      if (condition.block !== node) visit(condition.block, insideButton);
    }
  };
  visit(compiled.ast);
  assert.ok(elements.some((node) => node.attrsMap?.role === 'alertdialog'), '页面应渲染删除确认对话框');
  const swipeDeleteButtons = elements.filter((node) => node.attrsMap?.class === 'swipe-btn-delete');
  assert.ok(swipeDeleteButtons.length >= 3, '场景、卡片和标签列表都应提供左滑删除按钮');
  assert.ok(swipeDeleteButtons.every((node) => node.attrsMap?.['@click.stop']?.startsWith('requestDelete(')), '左滑删除按钮必须先请求确认');
  assert.ok(elements.some((node) => node.attrsMap?.class === 'delete-confirm-btn' && node.attrsMap?.['@click'] === 'confirmDelete'), '确认按钮应执行最终删除');
  const pinnedSceneRow = elements.find((node) => node.attrsMap?.['v-for']?.includes('scenePinned'));
  assert.match(pinnedSceneRow?.attrsMap?.class || '', /\bswipe-row\b/, '置顶场景也应提供可发现的左滑操作');
  assert.equal(pinnedSceneRow?.attrsMap?.['@touchend'], 'gestureEnd($event, scene.id)', '置顶场景左滑应使用统一的显露按钮交互');
  const textOf = (node) => (node?.children || [])
    .map((child) => child.type === 3 ? child.text.trim() : textOf(child))
    .join('');
  const pinnedSection = elements.find((node) => node.tag === 'section' && textOf(node).includes('置顶场景'));
  assert.match(textOf(pinnedSection), /左滑管理.*长按更多/, '置顶场景应显示与其他列表一致的手势提示');
  assert.equal(nestedButtons.length, 0, '交互控件不能嵌套 button，以免触控和键盘事件冲突');
}

async function testCardDeleteRefreshesRelatedCounts() {
  const apiCalls = [];
  const fakeApi = {
    deleteCard: async (id) => apiCalls.push(`delete-card:${id}`),
    tags: async () => {
      apiCalls.push('tags');
      return { data: [] };
    },
    scenes: async () => {
      apiCalls.push('scenes');
      return { data: { pinned: [], records: [], total: 0, page: 1 } };
    },
    cards: async () => {
      apiCalls.push('cards');
      return { data: { records: [], total: 0, totalPages: 0, page: 1, size: 100 } };
    }
  };
  const { vm } = await loadAppComponent(fakeApi);

  await vm.deleteCardItem({ id: 12, title: '充电宝' });

  assert.equal(apiCalls[0], 'delete-card:12', '应先删除目标卡片');
  assert.deepEqual(new Set(apiCalls.slice(1)), new Set(['tags', 'scenes', 'cards']), '删除卡片后应同步刷新标签计数、场景统计和卡片列表');
}

async function testUiRunnersRequireIsolationOptIn() {
  let safety;
  try {
    safety = require('../ui-test-safety.cjs');
  } catch {
    safety = {};
  }
  assert.equal(typeof safety.requireIsolatedUiTestBaseUrl, 'function', 'UI 自动化脚本应提供隔离环境保护');
  assert.throws(
    () => safety.requireIsolatedUiTestBaseUrl({ UI_TEST_BASE_URL: 'http://127.0.0.1:8080' }),
    /UI_TEST_ISOLATED=1/,
    '未明确声明隔离环境时必须拒绝运行会写数据的 UI 脚本'
  );
  assert.throws(
    () => safety.requireIsolatedUiTestBaseUrl({ UI_TEST_ISOLATED: '1' }),
    /UI_TEST_BASE_URL/,
    '隔离测试必须显式提供目标地址'
  );
  assert.throws(
    () => safety.requireIsolatedUiTestBaseUrl({ UI_TEST_ISOLATED: '1', UI_TEST_BASE_URL: 'https://app.example.com' }),
    /loopback/i,
    '写数据的 UI 测试不能连接任意远程地址'
  );
  assert.equal(
    safety.requireIsolatedUiTestBaseUrl({ UI_TEST_ISOLATED: '1', UI_TEST_BASE_URL: 'http://127.0.0.1:8080/' }),
    'http://127.0.0.1:8080',
    '有效的隔离测试地址应规范化后返回'
  );
}

async function testModalAnimationStartsImmediately() {
  const [appSource, css] = await Promise.all([
    readFile(appPath, 'utf8'),
    readFile(cssPath, 'utf8')
  ]);
  const root = postcss.parse(css);
  const customProperties = new Map();
  root.walkRules(':root', (rule) => {
    rule.walkDecls((decl) => {
      if (decl.prop.startsWith('--')) customProperties.set(decl.prop, decl.value.trim());
    });
  });
  const resolvedAnimation = (selector) => {
    let animation = '';
    root.walkRules(selector, (rule) => {
      rule.walkDecls('animation', (decl) => { animation = decl.value.trim(); });
    });
    return animation.replace(/var\((--[^)]+)\)/g, (_, property) => customProperties.get(property) || '');
  };
  const timeValues = (animation) => animation.match(/(?:^|\s)(\d*\.?\d+(?:ms|s))(?=\s|$)/g)?.map((value) => value.trim()) || [];

  assert.deepEqual(timeValues(resolvedAnimation('.modal-backdrop')), ['0.2s'], '遮罩动画只能包含持续时长，不能产生额外延迟');
  assert.deepEqual(timeValues(resolvedAnimation('.modal')), ['0.35s'], '弹层动画只能包含持续时长，不能产生额外延迟');

  const descriptor = vueTemplateCompiler.parseComponent(appSource);
  const compiled = vueTemplateCompiler.compile(descriptor.template.content, { outputSourceRange: true });
  assert.equal(compiled.errors.length, 0, 'App 模板应能正常编译');
  let bottomNav = null;
  const visit = (node) => {
    if (!node || bottomNav) return;
    if (node.type === 1 && node.attrsMap?.class === 'bottom-nav') bottomNav = node;
    for (const child of node.children || []) visit(child);
    for (const condition of node.ifConditions || []) {
      if (condition.block !== node) visit(condition.block);
    }
  };
  visit(compiled.ast);
  assert.equal(bottomNav?.attrsMap?.['v-if'], "currentView === 'home'", '打开弹框时底部导航应保持挂载，由遮罩覆盖而不是先消失');
}

export async function run() {
  const tests = [
    ['light surface contract', testLightSurfaceContract],
    ['scene icons render as line SVG', testSceneIconsRenderAsLineSvg],
    ['rendered template compatibility contract', testRenderedTemplateCompatibilityContract],
    ['fixed management navigation contract', testFixedManagementNavigationContract],
    ['delete requires confirmation', testDeleteRequiresConfirmation],
    ['card delete refreshes related counts', testCardDeleteRefreshesRelatedCounts],
    ['UI runners require isolation opt-in', testUiRunnersRequireIsolationOptIn],
    ['modal animation starts immediately', testModalAnimationStartsImmediately]
  ];
  const failures = [];

  for (const [name, test] of tests) {
    try {
      await test();
      console.log(`PASS ${name}`);
    } catch (error) {
      failures.push(error);
      console.error(`FAIL ${name}: ${error.message}`);
    }
  }

  if (failures.length) {
    throw new AggregateError(failures, `${failures.length} UI redesign test(s) failed`);
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  run().catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
}
