(function (root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) {
    module.exports = api;
  } else {
    root.BibeiState = api;
  }
}(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  const seedScenes = [
    {
      id: 'weekend',
      name: '周末出游',
      icon: 'bag',
      pinned: true,
      items: [
        { itemId: 'id-card', name: '身份证', quantity: 1, note: '' },
        { itemId: 'phone-charger', name: '手机充电器', quantity: 1, note: '' },
        { itemId: 'power-bank', name: '充电宝', quantity: 1, note: '' },
        { itemId: 'headphones', name: '耳机', quantity: 1, note: '' },
        { itemId: 'clothes', name: '换洗衣物', quantity: 2, note: '' },
        { itemId: 'toothbrush', name: '牙刷', quantity: 1, note: '' },
        { itemId: 'cleanser', name: '洗面奶', quantity: 1, note: '' },
        { itemId: 'umbrella', name: '雨伞', quantity: 1, note: '' }
      ]
    },
    {
      id: 'business',
      name: '商务出差',
      icon: 'briefcase',
      pinned: true,
      items: [
        { itemId: 'id-card', name: '身份证', quantity: 1, note: '' },
        { itemId: 'laptop', name: '笔记本电脑', quantity: 1, note: '' },
        { itemId: 'laptop-charger', name: '电脑充电器', quantity: 1, note: '放入电脑包' },
        { itemId: 'phone-charger', name: '手机充电器', quantity: 1, note: '' },
        { itemId: 'shirt', name: '衬衫', quantity: 2, note: '' },
        { itemId: 'clothes', name: '换洗衣物', quantity: 2, note: '' },
        { itemId: 'documents', name: '会议资料', quantity: 1, note: '' },
        { itemId: 'business-cards', name: '名片', quantity: 1, note: '' }
      ]
    },
    {
      id: 'week',
      name: '一周旅行',
      icon: 'suitcase',
      pinned: true,
      items: [
        { itemId: 'id-card', name: '身份证', quantity: 1, note: '' },
        { itemId: 'bank-card', name: '银行卡', quantity: 1, note: '' },
        { itemId: 'phone-charger', name: '手机充电器', quantity: 1, note: '' },
        { itemId: 'power-bank', name: '充电宝', quantity: 1, note: '' },
        { itemId: 'headphones', name: '耳机', quantity: 1, note: '' },
        { itemId: 'tops', name: '上衣', quantity: 5, note: '' },
        { itemId: 'pants', name: '裤子', quantity: 3, note: '' },
        { itemId: 'clothes', name: '换洗衣物', quantity: 7, note: '' },
        { itemId: 'toothbrush', name: '牙刷', quantity: 1, note: '' },
        { itemId: 'cleanser', name: '洗面奶', quantity: 1, note: '' },
        { itemId: 'towel', name: '毛巾', quantity: 1, note: '' },
        { itemId: 'medicine', name: '常用药', quantity: 1, note: '' },
        { itemId: 'bandage', name: '创可贴', quantity: 1, note: '' }
      ]
    }
  ];

  function clone(value) {
    return JSON.parse(JSON.stringify(value));
  }

  function normalizeName(value) {
    return String(value || '').trim().toLocaleLowerCase('zh-CN').replace(/\s+/g, '');
  }

  function createInitialState() {
    const scenes = clone(seedScenes);
    const catalogById = {};
    scenes.flatMap((scene) => scene.items).forEach((item) => {
      catalogById[item.itemId] = { id: item.itemId, name: item.name };
    });
    return {
      screen: 'home',
      scenes,
      catalog: Object.values(catalogById),
      preparations: {},
      activeSceneId: null,
      filter: 'all',
      history: [],
      toast: null
    };
  }

  function openScene(currentState, sceneId) {
    const state = clone(currentState);
    const scene = state.scenes.find((entry) => entry.id === sceneId);
    if (!scene) throw new Error('没有找到这个场景');
    if (!state.preparations[sceneId]) {
      state.preparations[sceneId] = {
        id: `preparation-${sceneId}-${Date.now()}`,
        sceneId,
        sceneName: scene.name,
        createdAt: new Date().toISOString(),
        items: scene.items.map((item) => ({ ...item, checked: false }))
      };
    }
    state.activeSceneId = sceneId;
    state.screen = 'checklist';
    state.filter = 'all';
    state.toast = null;
    return state;
  }

  function toggleItem(currentState, itemId) {
    const state = clone(currentState);
    const preparation = state.preparations[state.activeSceneId];
    if (!preparation) throw new Error('当前没有正在准备的场景');
    const item = preparation.items.find((entry) => entry.itemId === itemId);
    if (!item) throw new Error('没有找到这件物品');
    item.checked = !item.checked;
    return state;
  }

  function addItem(currentState, input) {
    const state = clone(currentState);
    const scene = state.scenes.find((entry) => entry.id === state.activeSceneId);
    const preparation = state.preparations[state.activeSceneId];
    if (!scene || !preparation) throw new Error('当前没有正在准备的场景');
    const name = String(input && input.name || '').trim();
    const normalized = normalizeName(name);
    if (!normalized) throw new Error('请输入物品名称');
    if (scene.items.some((item) => normalizeName(item.name) === normalized)) {
      throw new Error(`“${name}”已经在清单中`);
    }
    let catalogItem = state.catalog.find((entry) => normalizeName(entry.name) === normalized);
    if (!catalogItem) {
      catalogItem = { id: `item-${Date.now()}-${state.catalog.length + 1}`, name };
      state.catalog.push(catalogItem);
    }
    const item = {
      itemId: catalogItem.id,
      name: catalogItem.name,
      quantity: Math.max(1, Number.parseInt(input.quantity, 10) || 1),
      note: String(input.note || '').trim()
    };
    scene.items.push(item);
    preparation.items.push({ ...item, checked: false });
    return state;
  }

  function setFilter(currentState, filter) {
    const state = clone(currentState);
    state.filter = filter === 'pending' ? 'pending' : 'all';
    return state;
  }

  function getVisibleItems(state) {
    const preparation = state.preparations[state.activeSceneId];
    if (!preparation) return [];
    return state.filter === 'pending'
      ? preparation.items.filter((item) => !item.checked)
      : preparation.items.slice();
  }

  function searchCatalog(state, query) {
    const normalized = normalizeName(query);
    if (!normalized) return [];
    const scene = state.scenes.find((entry) => entry.id === state.activeSceneId);
    const existingIds = new Set((scene ? scene.items : []).map((item) => item.itemId));
    return state.catalog.filter((item) => (
      !existingIds.has(item.id) && normalizeName(item.name).includes(normalized)
    ));
  }

  function completePreparation(currentState, force) {
    const state = clone(currentState);
    const preparation = state.preparations[state.activeSceneId];
    if (!preparation) throw new Error('当前没有正在准备的场景');
    const remaining = preparation.items.filter((item) => !item.checked).length;
    if (remaining > 0 && !force) {
      return { state, requiresConfirmation: true, remaining };
    }
    const completedAt = new Date().toISOString();
    state.history.unshift({
      id: `history-${preparation.sceneId}-${Date.now()}`,
      sceneId: preparation.sceneId,
      sceneName: preparation.sceneName,
      items: clone(preparation.items),
      checkedCount: preparation.items.filter((item) => item.checked).length,
      totalCount: preparation.items.length,
      completedAt
    });
    delete state.preparations[preparation.sceneId];
    state.activeSceneId = null;
    state.screen = 'home';
    state.filter = 'all';
    state.toast = { message: '准备完成，可以出发了' };
    return { state, requiresConfirmation: false, remaining };
  }

  function createScene(currentState, rawName) {
    const name = String(rawName || '').trim();
    if (!name) throw new Error('请输入场景名称');
    const state = clone(currentState);
    const scene = {
      id: `scene-${Date.now()}-${state.scenes.length + 1}`,
      name,
      icon: 'compass',
      pinned: false,
      items: []
    };
    state.scenes.push(scene);
    return openScene(state, scene.id);
  }

  function restartPreparation(currentState) {
    const state = clone(currentState);
    const preparation = state.preparations[state.activeSceneId];
    if (!preparation) throw new Error('当前没有正在准备的场景');
    preparation.items.forEach((item) => {
      item.checked = false;
    });
    state.filter = 'all';
    return state;
  }

  function abandonPreparation(currentState) {
    const state = clone(currentState);
    if (!state.preparations[state.activeSceneId]) throw new Error('当前没有正在准备的场景');
    delete state.preparations[state.activeSceneId];
    state.activeSceneId = null;
    state.screen = 'home';
    state.filter = 'all';
    return state;
  }

  function dismissToast(currentState) {
    const state = clone(currentState);
    state.toast = null;
    return state;
  }

  return {
    createInitialState,
    openScene,
    toggleItem,
    addItem,
    setFilter,
    getVisibleItems,
    searchCatalog,
    completePreparation,
    createScene,
    restartPreparation,
    abandonPreparation,
    dismissToast
  };
}));
