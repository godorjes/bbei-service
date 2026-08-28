const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  })
  if (!response.ok) {
    let message = '操作失败，请稍后重试'
    try {
      const data = await response.json()
      message = data.message || message
    } catch {
      // Keep the readable fallback when the response is not JSON.
    }
    throw new Error(message)
  }
  if (response.status === 204) return null
  return response.json()
}

const body = (payload) => ({ body: JSON.stringify(payload) })

export const scenesApi = {
  list: () => request('/scenes'),
  get: (id) => request(`/scenes/${id}`),
  create: (payload) => request('/scenes', { method: 'POST', ...body(payload) }),
  update: (id, payload) => request(`/scenes/${id}`, { method: 'PUT', ...body(payload) }),
  remove: (id) => request(`/scenes/${id}`, { method: 'DELETE' }),
  setChecked: (sceneId, itemId, checked) => request(`/scenes/${sceneId}/items/${itemId}/checked`, {
    method: 'PUT', ...body({ checked }),
  }),
  reset: (id) => request(`/scenes/${id}/reset`, { method: 'POST' }),
}

export const sectionsApi = {
  list: () => request('/sections'),
  get: (id) => request(`/sections/${id}`),
  create: (payload) => request('/sections', { method: 'POST', ...body(payload) }),
  update: (id, payload) => request(`/sections/${id}`, { method: 'PUT', ...body(payload) }),
  remove: (id) => request(`/sections/${id}`, { method: 'DELETE' }),
}

export const itemsApi = {
  list: (query = '') => request(`/items${query ? `?query=${encodeURIComponent(query)}` : ''}`),
  get: (id) => request(`/items/${id}`),
  create: (payload) => request('/items', { method: 'POST', ...body(payload) }),
  update: (id, payload) => request(`/items/${id}`, { method: 'PUT', ...body(payload) }),
  remove: (id) => request(`/items/${id}`, { method: 'DELETE' }),
}
