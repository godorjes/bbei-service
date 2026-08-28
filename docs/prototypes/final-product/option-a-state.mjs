const seedSections = [
  {
    id: 'essentials', name: '重要物品', collapsed: false, items: [
      ['id-card', '身份证'], ['phone', '手机'], ['wallet', '钱包'],
      ['keys', '钥匙'], ['tickets', '车票与行程单'], ['cash', '少量现金']
    ]
  },
  {
    id: 'electronics', name: '电子设备', collapsed: false, items: [
      ['phone-charger', '手机充电器'], ['power-bank', '充电宝'], ['earphones', '耳机'],
      ['laptop', '笔记本电脑'], ['laptop-charger', '电脑充电器'], ['cable', '数据线'],
      ['adapter', '转换插头'], ['camera', '相机']
    ]
  },
  {
    id: 'clothing', name: '衣物', collapsed: false, items: [
      ['tops', '上衣 × 5'], ['trousers', '裤子 × 3'], ['underwear', '内衣 × 7'],
      ['socks', '袜子 × 7'], ['pajamas', '睡衣'], ['jacket', '外套'],
      ['walking-shoes', '步行鞋'], ['slippers', '拖鞋'], ['hat', '帽子'],
      ['raincoat', '轻便雨衣'], ['laundry-bag', '脏衣袋']
    ]
  },
  {
    id: 'toiletries', name: '洗护用品', collapsed: false, items: [
      ['toothbrush', '牙刷'], ['toothpaste', '牙膏'], ['cleanser', '洗面奶'],
      ['shampoo', '洗发水'], ['towel', '毛巾'], ['skincare', '护肤品'],
      ['razor', '剃须刀']
    ]
  },
  {
    id: 'health', name: '药品与健康', collapsed: false, items: [
      ['daily-medicine', '常用药'], ['cold-medicine', '感冒药'], ['bandage', '创可贴'],
      ['mask', '口罩'], ['tissues', '纸巾']
    ]
  },
  {
    id: 'other', name: '其他', collapsed: false, items: [
      ['umbrella', '雨伞'], ['bottle', '水杯'], ['snacks', '零食'],
      ['book', '书'], ['shopping-bag', '折叠购物袋']
    ]
  }
];

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function normalizeName(value) {
  return String(value || '').trim().toLocaleLowerCase('zh-CN').replace(/\s+/g, '');
}

export function createInitialState() {
  return {
    title: '一周旅行',
    filter: 'all',
    nextItemId: 1,
    nextSectionId: 1,
    sections: seedSections.map((section) => ({
      ...section,
      items: section.items.map(([id, name]) => ({ id, name, checked: false }))
    }))
  };
}

export function getOverallProgress(state) {
  const items = state.sections.flatMap((section) => section.items);
  return {
    checked: items.filter((item) => item.checked).length,
    total: items.length
  };
}

export function getSectionProgress(state, sectionId) {
  const section = state.sections.find((entry) => entry.id === sectionId);
  if (!section) throw new Error('分区不存在');
  return {
    checked: section.items.filter((item) => item.checked).length,
    total: section.items.length
  };
}

export function toggleItem(currentState, itemId) {
  const state = clone(currentState);
  const item = state.sections.flatMap((section) => section.items).find((entry) => entry.id === itemId);
  if (!item) throw new Error('物品不存在');
  item.checked = !item.checked;
  return state;
}

export function toggleSection(currentState, sectionId) {
  const state = clone(currentState);
  const section = state.sections.find((entry) => entry.id === sectionId);
  if (!section) throw new Error('分区不存在');
  section.collapsed = !section.collapsed;
  return state;
}

export function setFilter(currentState, filter) {
  if (!['all', 'pending'].includes(filter)) throw new Error('筛选条件无效');
  const state = clone(currentState);
  state.filter = filter;
  return state;
}

export function getVisibleSections(state) {
  const sections = clone(state.sections);
  if (state.filter === 'all') return sections;
  return sections
    .map((section) => ({ ...section, items: section.items.filter((item) => !item.checked) }))
    .filter((section) => section.items.length > 0);
}

export function addItem(currentState, input) {
  const state = clone(currentState);
  const name = String(input?.name || '').trim();
  if (!name) throw new Error('请输入物品名称');
  const normalized = normalizeName(name);
  const duplicate = state.sections.flatMap((section) => section.items)
    .some((item) => normalizeName(item.name) === normalized);
  if (duplicate) throw new Error('这个物品已经在清单中');

  const sectionId = input.sectionId || 'other';
  const section = state.sections.find((entry) => entry.id === sectionId);
  if (!section) throw new Error('分区不存在');
  section.items.push({ id: `custom-${state.nextItemId}`, name, checked: false });
  state.nextItemId += 1;
  return state;
}

export function clearChecks(currentState) {
  const state = clone(currentState);
  state.sections.forEach((section) => {
    section.items.forEach((item) => { item.checked = false; });
    section.collapsed = false;
  });
  state.filter = 'all';
  return state;
}

export function renameSection(currentState, sectionId, nextName) {
  const state = clone(currentState);
  const section = state.sections.find((entry) => entry.id === sectionId);
  if (!section) throw new Error('分区不存在');
  const name = String(nextName || '').trim();
  if (!name) throw new Error('请输入分区名称');
  const duplicate = state.sections.some((entry) => entry.id !== sectionId && normalizeName(entry.name) === normalizeName(name));
  if (duplicate) throw new Error('分区名称已存在');
  section.name = name;
  return state;
}

export function addSection(currentState, sectionName) {
  const state = clone(currentState);
  const name = String(sectionName || '').trim();
  if (!name) throw new Error('请输入分区名称');
  if (state.sections.some((section) => normalizeName(section.name) === normalizeName(name))) {
    throw new Error('分区名称已存在');
  }
  state.sections.push({
    id: `custom-section-${state.nextSectionId}`,
    name,
    collapsed: false,
    items: []
  });
  state.nextSectionId += 1;
  return state;
}
