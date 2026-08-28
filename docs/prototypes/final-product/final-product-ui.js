(function (root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) {
    module.exports = api;
  } else {
    root.BibeiUI = api;
  }
}(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function renderSceneIcon(icon) {
    const common = 'viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"';
    if (icon === 'briefcase') {
      return `<svg ${common}><rect x="3.5" y="7" width="17" height="12.5" rx="3"/><path d="M8.5 7V5.5A1.5 1.5 0 0 1 10 4h4a1.5 1.5 0 0 1 1.5 1.5V7M3.5 12.5h17M10 12.5v2h4v-2"/></svg>`;
    }
    if (icon === 'suitcase') {
      return `<svg ${common}><rect x="5" y="5.5" width="14" height="15" rx="3"/><path d="M9 5.5V4h6v1.5M9 9v8M15 9v8M8 20.5v1M16 20.5v1"/></svg>`;
    }
    if (icon === 'compass') {
      return `<svg ${common}><circle cx="12" cy="12" r="8.5"/><path d="m14.8 9.2-1.5 4.1-4.1 1.5 1.5-4.1 4.1-1.5Z"/></svg>`;
    }
    return `<svg ${common}><path d="M7 9V7.5A5 5 0 0 1 17 7.5V9"/><rect x="5" y="8" width="14" height="12" rx="4"/><path d="M8.5 12.5h7M8 20v1M16 20v1"/></svg>`;
  }

  function renderHome(state) {
    const sceneCards = state.scenes.map((scene) => {
      const preparation = state.preparations[scene.id];
      const checked = preparation ? preparation.items.filter((item) => item.checked).length : 0;
      const total = preparation ? preparation.items.length : scene.items.length;
      const progress = preparation
        ? `<span class="scene-progress"><strong>${checked}</strong> / ${total} 已准备</span>`
        : `<span class="scene-meta">${total} 件物品</span>`;
      return `<button class="scene-card" type="button" data-action="open-scene" data-scene-id="${escapeHtml(scene.id)}">
        <span class="scene-icon scene-icon--${escapeHtml(scene.icon)}">${renderSceneIcon(scene.icon)}</span>
        <span class="scene-copy"><strong>${escapeHtml(scene.name)}</strong>${progress}</span>
        <span class="scene-arrow" aria-hidden="true">›</span>
      </button>`;
    }).join('');

    return `<main class="screen home-screen">
      <header class="home-topbar">
        <span class="brand">带上</span>
        <button class="text-action" type="button" data-action="history">历史</button>
      </header>
      <section class="home-intro">
        <p class="eyebrow">出门前，确认一下</p>
        <h1>这次要去哪？</h1>
        <p>点一个场景，马上开始准备</p>
      </section>
      ${state.toast ? `<div class="toast" role="status">${escapeHtml(state.toast.message)}</div>` : ''}
      <section class="scene-list" aria-label="常用场景">${sceneCards}</section>
      <button class="create-scene" type="button" data-action="open-create-scene">
        <span aria-hidden="true">＋</span> 创建自己的场景
      </button>
    </main>`;
  }

  function renderChecklist(state) {
    const preparation = state.preparations[state.activeSceneId];
    if (!preparation) return renderHome(state);
    const checkedCount = preparation.items.filter((item) => item.checked).length;
    const totalCount = preparation.items.length;
    const progress = totalCount ? Math.round((checkedCount / totalCount) * 100) : 0;
    const visibleItems = state.filter === 'pending'
      ? preparation.items.filter((item) => !item.checked)
      : preparation.items;
    const rows = visibleItems.map((item) => `<button class="item-row${item.checked ? ' is-checked' : ''}" type="button" data-action="toggle-item" data-item-id="${escapeHtml(item.itemId)}">
      <span class="check-mark" aria-hidden="true">${item.checked ? '✓' : ''}</span>
      <span class="item-copy">
        <span class="item-title">${escapeHtml(item.name)}${item.quantity > 1 ? `<small>× ${item.quantity}</small>` : ''}</span>
        ${item.note ? `<span class="item-note">${escapeHtml(item.note)}</span>` : ''}
      </span>
    </button>`).join('');

    return `<main class="screen checklist-screen">
      <header class="checklist-topbar">
        <button class="icon-action" type="button" data-action="back" aria-label="返回首页">‹</button>
        <div class="checklist-title"><h1>${escapeHtml(preparation.sceneName)}</h1><span>自动保存当前进度</span></div>
        <button class="icon-action more-action" type="button" data-action="open-more" aria-label="更多操作">•••</button>
      </header>
      <section class="progress-card" aria-label="准备进度">
        <div class="progress-copy"><span><strong>${checkedCount}</strong> / ${totalCount}</span><small>${progress === 100 ? '全部确认' : '已准备'}</small></div>
        <div class="progress-track"><span style="width:${progress}%"></span></div>
      </section>
      <div class="filter-tabs" role="group" aria-label="筛选物品">
        <button type="button" data-action="set-filter" data-filter="all" class="${state.filter === 'all' ? 'is-active' : ''}">全部 <span>${totalCount}</span></button>
        <button type="button" data-action="set-filter" data-filter="pending" class="${state.filter === 'pending' ? 'is-active' : ''}">未完成 <span>${totalCount - checkedCount}</span></button>
      </div>
      <section class="flat-list" aria-label="物品清单">
        ${rows || `<div class="empty-state"><span class="empty-check">✓</span><strong>${totalCount ? '都准备好了' : '清单还是空的'}</strong><p>${totalCount ? '可以点击下方完成准备' : '先添加第一件要带的东西吧'}</p></div>`}
      </section>
      <div class="bottom-actions">
        <button class="add-button" type="button" data-action="open-add"><span aria-hidden="true">＋</span> 添加物品</button>
        <button class="complete-button" type="button" data-action="complete">完成准备</button>
      </div>
    </main>`;
  }

  function renderApp(state) {
    return state.screen === 'checklist' ? renderChecklist(state) : renderHome(state);
  }

  return { renderApp };
}));
