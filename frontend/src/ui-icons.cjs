const SVG_OPEN = '<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">';

function svg(body) {
  return `${SVG_OPEN}${body}</svg>`;
}

const sceneIcons = {
  '🚪': '<path d="M5 21h14M7 21V4.8A1.8 1.8 0 0 1 8.8 3H17v18M12 12h.01"/>',
  '✈️': '<path d="M21 16v-2l-8-5V3.5a1.5 1.5 0 0 0-3 0V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5z"/>',
  '💼': '<rect x="3" y="7" width="18" height="12" rx="2"/><path d="M8 7V5.5A1.5 1.5 0 0 1 9.5 4h5A1.5 1.5 0 0 1 16 5.5V7M3 12h18M10 12v2h4v-2"/>',
  '🏃': '<circle cx="13" cy="4" r="1.6"/><path d="m9 21 2-5-3-3 2.5-4 3 2 3-1M11 16l4 2 2 3M6 10l2.5-3 2 2"/>',
  '🌴': '<path d="M12 21c1-5 .5-9-1-13M4 8c3-3 6-2 7 0M11 8c0-4 3-6 6-5M11 8c3-3 7-2 9 1M11 8c-3-4-7-3-9-1"/>',
  '🎒': '<path d="M7 8V6a5 5 0 0 1 10 0v2M6 8h12a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-9a2 2 0 0 1 2-2zM8 13h8v5H8zM4 12H2v5h2M20 12h2v5h-2"/>',
  '🏕️': '<path d="m3 20 9-16 9 16zM12 4v16M8.5 20 12 14l3.5 6"/>',
  '🎉': '<path d="m4 20 4-12 8 8zM13 4l1-2M17 7l3-2M17 12l4 1M8 8l8 8M10 4l1 2"/>',
  '📌': '<path d="m14 4 6 6-3 1-3.5 3.5.5 4-1 1-3.5-3.5L5 21l-2-2 5-4.5L4.5 11l1-1 4 .5L13 7z"/>',
  '📦': '<path d="m3 7 9-4 9 4-9 4zM3 7v10l9 4 9-4V7M12 11v10M7.5 5l9 4"/>',
  '🎧': '<path d="M4 14v-2a8 8 0 0 1 16 0v2M4 14h3v6H5a1 1 0 0 1-1-1zM20 14h-3v6h2a1 1 0 0 0 1-1z"/>',
  '🧳': '<rect x="5" y="6" width="14" height="15" rx="2"/><path d="M9 6V4h6v2M9 10v7M15 10v7M8 21v1M16 21v1"/>',
  '🚲': '<circle cx="6" cy="17" r="4"/><circle cx="18" cy="17" r="4"/><path d="m6 17 4-7 4 7H6l3-5h6l3 5M9 7h3"/>',
  '🧼': '<rect x="3" y="9" width="18" height="11" rx="4"/><path d="M8 9c0-3 2-5 5-5h3M7 14h10"/>',
  '📷': '<path d="M4 7h4l2-3h4l2 3h4a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z"/><circle cx="12" cy="13" r="4"/>',
  default: '<path d="M20 13 13 20l-9-9V4h7z"/><circle cx="8.5" cy="8.5" r="1"/>'
};

const uiIcons = {
  tag: sceneIcons.default,
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/>',
  grid: '<rect x="4" y="4" width="6" height="6" rx="1"/><rect x="14" y="4" width="6" height="6" rx="1"/><rect x="4" y="14" width="6" height="6" rx="1"/><rect x="14" y="14" width="6" height="6" rx="1"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  close: '<path d="m6 6 12 12M18 6 6 18"/>',
  back: '<path d="m15 18-6-6 6-6"/>',
  reset: '<path d="M4 4v6h6M20 20v-6h-6"/><path d="M20 9a8 8 0 0 0-13.7-3L4 10M4 15a8 8 0 0 0 13.7 3L20 14"/>',
  celebrate: '<path d="m5 21 3-10 5 5zM13 4l1-2M17 7l3-2M17 12l4 1M9 8l7 7M8 4l1 2"/>',
  chevron: '<path d="m9 18 6-6-6-6"/>',
  pin: '<path d="M12 17v5M7 3h10l-2 6 3 3H6l3-3z"/>',
  layers: '<path d="m12 3 9 5-9 5-9-5z"/><path d="m3 12 9 5 9-5M3 16l9 5 9-5"/>',
  card: '<rect x="3" y="5" width="18" height="14" rx="3"/><path d="M3 10h18M7 15h4"/>',
  trash: '<path d="M4 7h16M9 7V4h6v3M7 7l1 14h8l1-14M10 11v6M14 11v6"/>',
  phone: '<rect x="7" y="2" width="10" height="20" rx="2"/><path d="M11 18h2"/>',
  key: '<circle cx="8" cy="15" r="4"/><path d="m11 12 8-8 2 2-2 2 1 1-2 2-1-1-3 3"/>',
  wallet: '<path d="M4 6h14a2 2 0 0 1 2 2v11H4a2 2 0 0 1-2-2V7a3 3 0 0 1 3-3h12"/><path d="M15 11h6v4h-6a2 2 0 0 1 0-4z"/>',
  id: '<rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="8" cy="11" r="2"/><path d="M5.5 16c.8-2 4.2-2 5 0M14 9h4M14 13h4"/>',
  battery: '<rect x="7" y="3" width="10" height="18" rx="2"/><path d="M10 1h4M10 12h4M12 10v4"/>',
  umbrella: '<path d="M4 12a8 8 0 0 1 16 0c-2-1-3-1-4 0-2-1-3-1-4 0-2-1-3-1-4 0-2-1-3-1-4 0zM12 12v6a2 2 0 0 0 4 0"/>',
  generic: '<circle cx="12" cy="12" r="8"/><path d="M9 12h6M12 9v6"/>'
};

function sceneIconSvg(icon) {
  return svg(sceneIcons[icon] || sceneIcons.default);
}

function uiIconSvg(name) {
  return svg(uiIcons[name] || uiIcons.generic);
}

function cardIconSvg(title = '') {
  const value = String(title);
  let name = 'generic';
  if (/手机|电话/.test(value)) name = 'phone';
  else if (/钥匙/.test(value)) name = 'key';
  else if (/钱包/.test(value)) name = 'wallet';
  else if (/身份证|证件/.test(value)) name = 'id';
  else if (/充电|电池|电源/.test(value)) name = 'battery';
  else if (/雨伞|伞/.test(value)) name = 'umbrella';
  else if (/耳机/.test(value)) name = 'headphones';
  else if (/相机|拍照/.test(value)) name = 'camera';
  else if (/行李|箱/.test(value)) name = 'luggage';
  return svg(uiIcons[name] || sceneIcons[{ headphones: '🎧', camera: '📷', luggage: '🧳' }[name]] || uiIcons.generic);
}

module.exports = { sceneIconSvg, uiIconSvg, cardIconSvg };
