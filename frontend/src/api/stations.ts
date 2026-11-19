import http, { request } from './http'
import type { ApiResponse } from '@/types/api'
import type { PageResult, Station, Sensor, StationInput, SensorInput } from '@/types/station'

export interface ListQuery {
  keyword?: string
  enabled?: boolean
  page: number
  size: number
}
export const stationApi = {
  async list(params: ListQuery) {
    return (await http.get<ApiResponse<PageResult<Station>>>('/stations', { params })).data.data
  },
  detail: (id: string) => request<Station>('get', `/stations/${id}`),
  save: (id: string | null, data: StationInput) =>
    request<Station>(id ? 'put' : 'post', id ? `/stations/${id}` : '/stations', data),
  remove: (id: string) => request('delete', `/stations/${id}`),
  async sensors(stationId: string, params: ListQuery) {
    return (
      await http.get<ApiResponse<PageResult<Sensor>>>(`/stations/${stationId}/sensors`, { params })
    ).data.data
  },
  sensor: (stationId: string, id: string) =>
    request<Sensor>('get', `/stations/${stationId}/sensors/${id}`),
  saveSensor: (stationId: string, id: string | null, data: SensorInput) =>
    request<Sensor>(
      id ? 'put' : 'post',
      `/stations/${stationId}/sensors${id ? '/' + id : ''}`,
      data,
    ),
  removeSensor: (stationId: string, id: string) =>
    request('delete', `/stations/${stationId}/sensors/${id}`),
}
