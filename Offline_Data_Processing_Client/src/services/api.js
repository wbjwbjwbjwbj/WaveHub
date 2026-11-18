import { initialData, validateTransition, validateParameters } from './domain'
const STORE = 'offline-workspace-demo-v1'
export const mode =
  localStorage.getItem('offline-workspace-mode') || import.meta.env.VITE_DATA_MODE || 'demo'
export function setMode(value) {
  localStorage.setItem('offline-workspace-mode', value)
}
let db
try {
  db = JSON.parse(localStorage.getItem(STORE)) || initialData()
} catch {
  db = initialData()
}
const persist = () => localStorage.setItem(STORE, JSON.stringify(db))
const base = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '')
export async function request(path, { method = 'GET', body, query, download = false } = {}) {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query || {}))
    if (value !== '' && value != null) params.set(key, value)
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), 60000)
  try {
    const response = await fetch(`${base}/client${path}${params.size ? `?${params}` : ''}`, {
      method,
      body: body instanceof FormData ? body : body === undefined ? undefined : JSON.stringify(body),
      headers:
        body !== undefined && !(body instanceof FormData)
          ? { 'Content-Type': 'application/json' }
          : {},
      signal: controller.signal,
    })
    if (!response.ok) {
      const error = await response.json().catch(() => ({}))
      throw new Error(error.message || `请求失败（${response.status}）`)
    }
    if (download) {
      const disposition = response.headers.get('Content-Disposition') || ''
      const utf = disposition.match(/filename\*=UTF-8''([^;]+)/i)
      const ascii = disposition.match(/filename="?([^";]+)"?/i)
      let filename = ascii?.[1] || 'model.zip'
      if (utf) {
        try {
          filename = decodeURIComponent(utf[1])
        } catch {
          /* retain ASCII fallback */
        }
      }
      return { blob: await response.blob(), filename: filename.split(/[\\/]/).pop() }
    }
    return response.status === 204 ? undefined : response.json()
  } catch (error) {
    if (error.name === 'AbortError') throw new Error('请求超时，请检查后端连接', { cause: error })
    throw error
  } finally {
    clearTimeout(timer)
  }
}
function resource(kind, id) {
  const row = db[kind].find((x) => x.id === id)
  if (!row) throw new Error('资源不存在，请刷新列表')
  return row
}
export const api = {
  async list(kind, query = {}) {
    if (mode === 'api') return request(`/${kind}`, { query })
    const matches = db[kind].filter(
      (x) =>
        (!query.keyword || x.name.toLowerCase().includes(query.keyword.trim().toLowerCase())) &&
        (!query.type || x.type === query.type) &&
        (!query.status || x.status === query.status),
    )
    const page = query.page || 1,
      pageSize = query.pageSize || 20
    return {
      items: structuredClone(matches.slice((page - 1) * pageSize, page * pageSize)),
      total: matches.length,
      page,
      pageSize,
    }
  },
  async all(kind) {
    const result = []
    let page = 1
    while (true) {
      const data = await this.list(kind, { page, pageSize: 200 })
      result.push(...data.items)
      if (result.length >= data.total || !data.items.length) return result
      page++
    }
  },
  async get(kind, id) {
    return mode === 'api'
      ? request(`/${kind}/${encodeURIComponent(id)}`)
      : structuredClone(resource(kind, id))
  },
  async save(kind, id, values) {
    if (mode === 'api')
      return request(`/${kind}${id ? `/${encodeURIComponent(id)}` : ''}`, {
        method: id ? 'PUT' : 'POST',
        body: values,
      })
    const old = id ? resource(kind, id) : null
    if (kind === 'tasks' && old?.status === 'running') throw new Error('运行中的任务不可编辑')
    let input = values
    if (values instanceof FormData) {
      input = Object.fromEntries(values)
      input.fileName = input.file?.name
      input.sizeBytes = input.file?.size
      delete input.file
      input.parameters = []
    }
    const row = {
      ...old,
      ...input,
      id: id || crypto.randomUUID(),
      createdAt: old?.createdAt || new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }
    if (kind === 'tasks') {
      resource('models', row.modelId)
      row.deviceIds.forEach((d) => resource('devices', d))
      row.status ||= 'created'
    }
    if (old) Object.assign(old, row)
    else db[kind].unshift(row)
    persist()
    return structuredClone(row)
  },
  async remove(kind, id) {
    if (mode === 'api') return request(`/${kind}/${encodeURIComponent(id)}`, { method: 'DELETE' })
    const row = resource(kind, id)
    if (kind === 'tasks' && row.status === 'running') throw new Error('请先停止任务再删除')
    if (
      kind !== 'tasks' &&
      db.tasks.some((t) => (kind === 'models' ? t.modelId === id : t.deviceIds.includes(id)))
    )
      throw new Error('该资源已被任务引用，请先解除绑定')
    db[kind] = db[kind].filter((x) => x.id !== id)
    persist()
  },
  async action(id, action) {
    if (mode === 'api')
      return request(`/tasks/${encodeURIComponent(id)}/${action}`, { method: 'POST' })
    const row = resource('tasks', id)
    row.status = validateTransition(row.status, action)
    row.updatedAt = new Date().toISOString()
    persist()
    return structuredClone(row)
  },
  async parameters(id) {
    if (mode === 'api') return request(`/tasks/${encodeURIComponent(id)}/parameters`)
    const row = resource('tasks', id)
    return {
      parameters: structuredClone(resource('models', row.modelId).parameters || []).map((p) => ({
        ...p,
        value: row.parameterValues?.[p.key] ?? p.defaultValue,
      })),
    }
  },
  async saveParameters(id, values) {
    if (mode === 'api')
      return request(`/tasks/${encodeURIComponent(id)}/parameters`, {
        method: 'PUT',
        body: { values },
      })
    const row = resource('tasks', id)
    if (row.status === 'running') throw new Error('运行中不可修改参数')
    validateParameters((await this.parameters(id)).parameters, values)
    row.parameterValues = values
    persist()
  },
  async exportModel(id) {
    if (mode !== 'api') throw new Error('演示模式不生成模型 ZIP，请连接真实接口后导出')
    return request(`/models/${encodeURIComponent(id)}/export`, { download: true })
  },
}
