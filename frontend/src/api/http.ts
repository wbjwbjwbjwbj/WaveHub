import axios from 'axios'
import type { ApiResponse } from '@/types/api'

const http = axios.create({ baseURL: '/api', timeout: 10000, withCredentials: true })

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
