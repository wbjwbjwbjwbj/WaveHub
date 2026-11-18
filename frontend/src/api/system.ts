import { get } from './http'
import type { SystemInfo } from '@/types/api'

export const getSystemInfo = () => get<SystemInfo>('/system/info')
