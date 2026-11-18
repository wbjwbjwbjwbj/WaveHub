const now = '2026-11-18T02:30:00Z'
const parameters = [
  {
    key: 'sampleRate',
    label: '采样率（Hz）',
    type: 'integer',
    required: true,
    min: 1,
    max: 10000,
    defaultValue: 100,
  },
  {
    key: 'threshold',
    label: '检测阈值',
    type: 'number',
    required: true,
    min: 0,
    max: 1,
    defaultValue: 0.8,
  },
  { key: 'denoise', label: '启用降噪', type: 'boolean', defaultValue: true },
  {
    key: 'window',
    label: '窗口类型',
    type: 'select',
    options: ['hann', 'hamming', 'rectangular'],
    defaultValue: 'hann',
  },
]
export function initialData() {
  return {
    devices: [
      {
        id: 'dev-001',
        name: '一号地震传感器',
        model: 'CMG-3T',
        type: '地震传感器',
        ip: '192.168.1.101',
        description: '实验室东侧采集点',
        createdAt: now,
        updatedAt: now,
      },
      {
        id: 'dev-002',
        name: '二号振动采集设备',
        model: 'INV3062',
        type: '振动传感器',
        ip: '192.168.1.102',
        description: '振动信号采集',
        createdAt: now,
        updatedAt: now,
      },
      {
        id: 'dev-003',
        name: '三号地震传感器',
        model: 'CMG-3T',
        type: '地震传感器',
        ip: null,
        description: '离线数据采集设备',
        createdAt: now,
        updatedAt: now,
      },
    ],
    models: [
      {
        id: 'model-001',
        name: '地震事件检测模型',
        type: '事件检测',
        description: '识别波形中的地震事件，提取有效信号片段',
        fileName: 'seismic-detector-v1.zip',
        createdAt: now,
        parameters,
      },
      {
        id: 'model-002',
        name: '多通道信号降噪模型',
        type: '信号降噪',
        description: '多通道波形预处理与背景噪声抑制',
        fileName: 'denoise-v2.zip',
        createdAt: now,
        parameters,
      },
    ],
    tasks: [
      {
        id: 'task-001',
        name: '地震波形事件检测',
        modelId: 'model-001',
        deviceIds: ['dev-001', 'dev-003'],
        description: '对实验室采集的地震波形进行事件检测与分析',
        status: 'created',
        createdAt: now,
        updatedAt: now,
        parameterValues: {},
      },
      {
        id: 'task-002',
        name: '振动数据预处理',
        modelId: 'model-002',
        deviceIds: ['dev-002'],
        description: '完成振动数据的降噪预处理',
        status: 'running',
        createdAt: now,
        updatedAt: now,
        parameterValues: {},
      },
    ],
  }
}
export function validateTransition(status, action) {
  if (action === 'start' && !['created', 'stopped'].includes(status))
    throw new Error('仅已创建或已停止的任务可以启动')
  if (action === 'stop' && status !== 'running') throw new Error('仅运行中的任务可以停止')
  return action === 'start' ? 'running' : 'stopped'
}
export function validateParameters(definitions, values) {
  if (Object.keys(values).some((key) => !definitions.some((p) => p.key === key)))
    throw new Error('包含未知参数')
  for (const p of definitions) {
    const v = values[p.key]
    if (v === undefined || v === null || v === '') {
      if (p.required) throw new Error(`${p.label}为必填项`)
      continue
    }
    if (
      ['number', 'integer'].includes(p.type) &&
      (typeof v !== 'number' ||
        !Number.isFinite(v) ||
        (p.type === 'integer' && !Number.isInteger(v)) ||
        (p.min != null && v < p.min) ||
        (p.max != null && v > p.max))
    )
      throw new Error(`${p.label}数值无效或超出范围`)
    if (p.type === 'boolean' && typeof v !== 'boolean') throw new Error(`${p.label}必须为布尔值`)
    if (p.type === 'string' && typeof v !== 'string') throw new Error(`${p.label}必须为文本`)
    if (p.type === 'select' && !p.options?.includes(v)) throw new Error(`${p.label}选项无效`)
  }
}
