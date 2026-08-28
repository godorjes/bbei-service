const item = (id, name, group, checked = false) => ({ id, name, group, checked, temporary: false });

export const SCENES = {
  weekend: {
    id: 'weekend',
    name: '周末出游',
    icon: 'bag',
    items: [
      item('id-card', '身份证', '证件'),
      item('phone-charger', '手机充电器', '数码'),
      item('power-bank', '充电宝', '数码'),
      item('headphones', '耳机', '数码'),
      item('clothes', '换洗衣物 × 2', '衣物'),
      item('pajamas', '睡衣', '衣物'),
      item('toothbrush', '牙刷', '洗漱'),
      item('cleanser', '洗面奶', '洗漱'),
    ],
  },
  business: {
    id: 'business',
    name: '商务出差',
    icon: 'case',
    items: [
      item('id-card', '身份证', '证件'),
      item('laptop', '笔记本电脑', '数码'),
      item('laptop-charger', '电脑充电器', '数码'),
      item('phone-charger', '手机充电器', '数码'),
      item('shirt', '衬衫', '衣物'),
      item('jacket', '外套', '衣物'),
      item('toothbrush', '牙刷', '洗漱'),
      item('documents', '会议资料', '工作'),
    ],
  },
  week: {
    id: 'week',
    name: '一周旅行',
    icon: 'suitcase',
    items: [
      item('id-card', '身份证', '证件'),
      item('tickets', '车票 / 机票', '证件'),
      item('phone-charger', '手机充电器', '数码'),
      item('power-bank', '充电宝', '数码'),
      item('headphones', '耳机', '数码'),
      item('clothes', '换洗衣物 × 5', '衣物'),
      item('pajamas', '睡衣', '衣物'),
      item('shoes', '备用鞋', '衣物'),
      item('toothbrush', '牙刷', '洗漱'),
      item('cleanser', '洗面奶', '洗漱'),
      item('towel', '毛巾', '洗漱'),
      item('medicine', '常用药', '其他'),
      item('umbrella', '雨伞', '其他'),
    ],
  },
};

export function createInitialState() {
  return {
    screen: 'home',
    activeScene: null,
    addedItem: null,
    notice: '',
  };
}

export function startScene(state, sceneId) {
  const scene = SCENES[sceneId];
  if (!scene) return state;

  return {
    ...state,
    screen: 'checklist',
    activeScene: structuredClone(scene),
    addedItem: null,
    notice: '',
  };
}

export function startCustomScene(state, rawName) {
  const name = rawName.trim();
  if (!name) return state;

  return {
    ...state,
    screen: 'checklist',
    activeScene: {
      id: 'custom',
      name,
      icon: 'bag',
      items: [],
    },
    addedItem: null,
    notice: '',
  };
}

export function toggleItem(state, itemId) {
  if (!state.activeScene) return state;
  const index = state.activeScene.items.findIndex((entry) => entry.id === itemId);
  if (index < 0) return state;

  const items = state.activeScene.items.map((entry) => (
    entry.id === itemId ? { ...entry, checked: !entry.checked } : entry
  ));

  return {
    ...state,
    activeScene: { ...state.activeScene, items },
  };
}

export function addItem(state, rawName, group = '其他') {
  if (!state.activeScene) return state;
  const name = rawName.trim();
  if (!name) return state;

  const temporaryCount = state.activeScene.items.filter((entry) => entry.temporary).length;
  const addedItem = {
    id: `temporary-${temporaryCount + 1}`,
    name,
    group,
    checked: false,
    temporary: true,
  };

  return {
    ...state,
    activeScene: {
      ...state.activeScene,
      items: [...state.activeScene.items, addedItem],
    },
    addedItem,
  };
}

export function openCompletion(state) {
  if (!state.activeScene) return state;
  return { ...state, screen: 'complete' };
}

export function resolveAddedItem(state, keep) {
  if (!state.activeScene) return state;

  let notice = '准备完成，可以出发了';
  if (state.addedItem) {
    notice = keep
      ? `已将“${state.addedItem.name}”保留到${state.activeScene.name}`
      : '已完成，本次新增不会保留到场景';
  }

  return {
    ...createInitialState(),
    notice,
  };
}
