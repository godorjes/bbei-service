const fs = require('fs');
const path = require('path');
const { requireIsolatedUiTestBaseUrl } = require('./ui-test-safety.cjs');
const BASE_URL = requireIsolatedUiTestBaseUrl();
const { chromium } = require('playwright');

const REPORT_PATH = path.resolve(__dirname, '..', 'ui-test-report-tags-2026-04-08.md');

function nowDate() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`;
}

async function ensureHome(page) {
  const close = async (selector) => {
    const loc = page.locator(selector);
    if (await loc.count()) await loc.first().click();
  };
  await close('.modal .icon-btn[aria-label="关闭"]');
  await close('.modal.full .icon-btn[aria-label="关闭"]');
  await close('.modal-backdrop');
  if (await page.locator('.scene-header .icon-btn[aria-label="返回"]').count()) {
    await page.click('.scene-header .icon-btn[aria-label="返回"]');
  }
  await page.waitForSelector('.bottom-nav');
}

(async () => {
  const results = new Map();
  const setResult = (id, status, note) => results.set(id, { status, note });
  const tagName = `UI测试-购物清单-${Date.now()}`;
  let createdTagId = null;

  let browser;
  try {
    browser = await chromium.launch({ channel: 'chrome' });
  } catch (e) {
    browser = await chromium.launch();
  }
  const page = await browser.newPage({ viewport: { width: 420, height: 860 } });

  try {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });
    await page.waitForSelector('text=场景');

    // Open tag manager
    await page.click('.bottom-nav .nav-btn:has-text("管理")');
    await page.waitForSelector('.modal.full:has-text("管理")');
    await page.click('.management-tab:has-text("标签")');
    await page.waitForSelector('.tag-management-panel');

    // Open new tag modal
    await page.click('.modal.full [aria-label="新建标签"]');
    await page.waitForSelector('.modal:has-text("新建标签")');

    // TAG-004: switch color to orange
    try {
      await page.click('.color-list .color-btn:has-text("橙色")');
      await page.waitForTimeout(100);
      const isActive = await page.locator('.color-list .color-btn:has-text("橙色")').evaluate(el => el.classList.contains('active'));
      if (isActive) setResult('TAG-004', 'PASS', '橙色选中');
      else setResult('TAG-004', 'FAIL', '橙色未选中');
    } catch (e) {
      setResult('TAG-004', 'FAIL', '切换颜色失败');
    }

    // TAG-006: create tag
    try {
      await page.fill('.modal:has-text("新建标签") input', tagName);
      await page.click('.modal:has-text("新建标签") .primary');
      await page.waitForSelector('.modal.full .tag-management-panel');
      const exists = await page.locator('.tag-manage-row', { hasText: tagName }).count();
      const listRes = await fetch(BASE_URL + '/api/tags/list', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: '{}'
      });
      const created = (await listRes.json() || []).find(tag => tag.name === tagName);
      createdTagId = created ? created.id : null;
      if (exists) setResult('TAG-006', 'PASS', '创建成功');
      else setResult('TAG-006', 'FAIL', '未出现在列表');
    } catch (e) {
      setResult('TAG-006', 'FAIL', '创建失败');
    }

    // TAG-007: empty name should disable
    try {
      await page.click('.modal.full [aria-label="新建标签"]');
      const disabled = await page.locator('.modal:has-text("新建标签") .primary').isDisabled();
      if (disabled) setResult('TAG-007', 'PASS', '空名称禁用');
      else setResult('TAG-007', 'FAIL', '空名称未禁用');
      await page.click('.modal:has-text("新建标签") .icon-btn[aria-label="关闭"]');
      await ensureHome(page);
    } catch (e) {
      setResult('TAG-007', 'FAIL', '检测失败');
    }
  } finally {
    if (createdTagId) {
      try {
        await fetch(BASE_URL + '/api/tags/delete', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ id: createdTagId })
        });
      } catch (error) {
        console.warn('Failed to clean up isolated UI test tag:', error.message);
      }
    }
    await browser.close();
  }

  const lines = [];
  lines.push('# UI 自动化测试报告（仅 TAG-004/006/007）');
  lines.push('');
  lines.push(`日期：${nowDate()}`);
  lines.push(`地址：${BASE_URL}`);
  lines.push('');
  lines.push('| 用例编号 | 状态 | 备注 |');
  lines.push('| --- | --- | --- |');
  for (const id of ['TAG-004','TAG-006','TAG-007']) {
    const r = results.get(id) || { status: 'SKIP', note: '未执行' };
    lines.push(`| ${id} | ${r.status} | ${r.note} |`);
  }
  const content = '\ufeff' + lines.join('\n');
  fs.writeFileSync(REPORT_PATH, content, 'utf8');
  console.log('Report:', REPORT_PATH);
})();
