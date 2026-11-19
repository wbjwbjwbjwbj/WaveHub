import axios from 'axios'
import type { ApiResponse } from '@/types/api'

const http = axios.create({ baseURL: '/api', timeout: 10000, withCredentials: true })

let csrf: { headerName: string; token: string } | null = null
export function resetCsrf() {
  csrf = null
}

http.interceptors.request.use(async (config) => {
  if (!['get', 'head', 'options'].includes(config.method?.toLowerCase() || 'get')) {
    if (!csrf)
      csrf = (await http.get<ApiResponse<{ headerName: string; token: string }>>('/auth/csrf')).data
        .data
    config.headers.set(csrf.headerName, csrf.token)
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error)) {
      if (error.response?.status === 401 || error.response?.status === 403) resetCsrf()
      if (error.response?.status === 401 && !error.config?.url?.startsWith('/auth/')) {
        window.dispatchEvent(new Event('wavehub:unauthorized'))
      }
    }
    return Promise.reject(error)
  },
)

export async function request<T>(
  method: 'get' | 'post' | 'put' | 'delete',
  url: string,
  data?: unknown,
): Promise<T> {
  const response = await http.request<ApiResponse<T>>({ method, url, data })
  if (response.data.code !== 'OK') throw new Error(response.data.message || '请求失败')
  return response.data.data
}

export async function get<T>(url: string): Promise<T> {
  const { data } = await http.get<ApiResponse<T>>(url)
  if (data.code !== 'OK') throw new Error(data.message || '请求失败')
  return data.data
}

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
    return error.response?.data?.message || '无法连接后端，请检查后端服务是否已启动'
  }
  return error instanceof Error ? error.message : '请求失败，请稍后重试'
}

// 后续写操作先通过 /auth/csrf 获取令牌，并将其放入返回的 headerName 对应的请求头。
export default http
