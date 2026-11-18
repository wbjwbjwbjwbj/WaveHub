export interface ApiResponse<T> {
  code: string
  message: string
  data: T
}

export interface SystemInfo {
  name: string
  version: string
  databaseMode: 'standalone' | 'kingbase'
  serverTime: string
}
