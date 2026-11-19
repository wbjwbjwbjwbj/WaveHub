export interface Station {
  id: string
  code: string
  name: string
  longitude: number
  latitude: number
  elevationM: number | null
  enabled: boolean
  description: string | null
  sensorCount: number
  createdAt: string
  updatedAt: string
}
export interface Sensor extends Omit<Station, 'sensorCount'> {
  stationId: string
  sensorType: string | null
  model: string | null
  serialNumber: string | null
  nominalSampleRateHz: number | null
  parameters: Record<string, unknown>
}
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}
export type StationInput = Pick<
  Station,
  'code' | 'name' | 'longitude' | 'latitude' | 'elevationM' | 'enabled' | 'description'
>
export type SensorInput = StationInput &
  Pick<Sensor, 'sensorType' | 'model' | 'serialNumber' | 'nominalSampleRateHz' | 'parameters'>
