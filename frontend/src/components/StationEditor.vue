<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import axios from 'axios'
import { stationApi } from '@/api/stations'
import { getErrorMessage } from '@/api/http'
import type { SensorInput, Station } from '@/types/station'

const props = defineProps<{
  kind: 'station' | 'sensor'
  id: string | null
  station: Station | null
}>()
const emit = defineEmits<{ close: []; saved: [id: string] }>()
const form = reactive({
  code: '',
  name: '',
  longitude: '' as number | string,
  latitude: '' as number | string,
  elevationM: '' as number | string,
  enabled: true,
  description: '',
  sensorType: '',
  model: '',
  serialNumber: '',
  nominalSampleRateHz: '' as number | string,
})
const parameters = ref('{}'),
  loading = ref(true),
  loadFailed = ref(false),
  saving = ref(false),
  error = ref(''),
  baseline = ref('')
const fields = ref<Record<string, string>>({})
const snapshot = () => JSON.stringify({ ...form, parameters: parameters.value })
const dirty = computed(() => !loading.value && !loadFailed.value && baseline.value !== snapshot())
let sequence = 0
watch(
  () => [props.kind, props.id, props.station?.id],
  async () => {
    const current = ++sequence
    loading.value = true
    loadFailed.value = false
    error.value = ''
    fields.value = {}
    Object.assign(form, {
      code: '',
      name: '',
      longitude: '',
      latitude: '',
      elevationM: '',
      enabled: true,
      description: '',
      sensorType: '',
      model: '',
      serialNumber: '',
      nominalSampleRateHz: '',
    })
    parameters.value = '{}'
    try {
      if (props.id) {
        const data =
          props.kind === 'station'
            ? await stationApi.detail(props.id)
            : await stationApi.sensor(props.station!.id, props.id)
        if (current !== sequence) return
        Object.assign(form, {
          code: data.code,
          name: data.name,
          longitude: data.longitude,
          latitude: data.latitude,
          elevationM: data.elevationM ?? '',
          enabled: data.enabled,
          description: data.description ?? '',
        })
        if ('parameters' in data) {
          Object.assign(form, {
            sensorType: data.sensorType ?? '',
            model: data.model ?? '',
            serialNumber: data.serialNumber ?? '',
            nominalSampleRateHz: data.nominalSampleRateHz ?? '',
          })
          parameters.value = JSON.stringify(data.parameters, null, 2)
        }
      }
      baseline.value = snapshot()
    } catch (cause) {
      loadFailed.value = true
      error.value = getErrorMessage(cause)
    } finally {
      if (current === sequence) loading.value = false
    }
  },
  { immediate: true },
)

async function confirmClose() {
  if (saving.value) return false
  if (!dirty.value) return true
  try {
    await ElMessageBox.confirm('放弃未保存的更改？', '确认', {
      confirmButtonText: '放弃更改',
      cancelButtonText: '继续编辑',
      type: 'warning',
    })
    return true
  } catch {
    return false
  }
}
async function close() {
  if (await confirmClose()) emit('close')
}
defineExpose({ confirmClose })

function copyStationCoordinates() {
  if (!props.station) return
  form.longitude = props.station.longitude
  form.latitude = props.station.latitude
  form.elevationM = props.station.elevationM ?? ''
}

async function save() {
  if (saving.value || loading.value || loadFailed.value) return
  fields.value = {}
  error.value = ''
  let parsed: unknown
  try {
    parsed = JSON.parse(parameters.value)
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object')
      throw new Error('扩展参数必须为 JSON 对象')
  } catch {
    error.value = '扩展参数必须为有效的 JSON 对象'
    return
  }
  const numberOrNull = (value: number | string) => (value === '' ? null : Number(value))
  const data: SensorInput = {
    code: form.code.trim(),
    name: form.name.trim(),
    longitude: Number(form.longitude),
    latitude: Number(form.latitude),
    elevationM: numberOrNull(form.elevationM),
    enabled: form.enabled,
    description: form.description.trim() || null,
    sensorType: form.sensorType.trim() || null,
    model: form.model.trim() || null,
    serialNumber: form.serialNumber.trim() || null,
    nominalSampleRateHz: numberOrNull(form.nominalSampleRateHz),
    parameters: parsed as Record<string, unknown>,
  }
  saving.value = true
  try {
    const record =
      props.kind === 'station'
        ? await stationApi.save(props.id, data)
        : await stationApi.saveSensor(props.station!.id, props.id, data)
    baseline.value = snapshot()
    emit('saved', record.id)
  } catch (cause) {
    error.value = getErrorMessage(cause)
    if (axios.isAxiosError(cause)) fields.value = cause.response?.data?.data?.fieldErrors ?? {}
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <aside class="editor-panel">
    <div class="section-toolbar">
      <h2>{{ id ? '编辑' : '新增' }}{{ kind === 'station' ? '台站' : '传感器' }}</h2>
      <el-button text :disabled="saving" @click="close">关闭</el-button>
    </div>
    <el-skeleton v-if="loading" :rows="8" animated />
    <div v-else class="editor-form">
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-form label-position="top" :disabled="saving || loadFailed" @submit.prevent="save">
        <div class="editor-fields">
          <el-form-item v-if="kind === 'sensor'" label="所属台站"
            ><el-input :model-value="station?.name" disabled
          /></el-form-item>
          <el-form-item label="编码" :error="fields.code" required
            ><el-input v-model="form.code" required maxlength="64" pattern="[A-Za-z0-9_-]+"
          /></el-form-item>
          <el-form-item label="名称" :error="fields.name" required
            ><el-input v-model="form.name" required maxlength="128"
          /></el-form-item>
          <div class="field-grid">
            <el-form-item label="经度（°）" :error="fields.longitude" required
              ><el-input
                v-model="form.longitude"
                type="number"
                required
                min="-180"
                max="180"
                step="0.0000001"
            /></el-form-item>
            <el-form-item label="纬度（°）" :error="fields.latitude" required
              ><el-input
                v-model="form.latitude"
                type="number"
                required
                min="-90"
                max="90"
                step="0.0000001"
            /></el-form-item>
          </div>
          <el-form-item label="高程（m）" :error="fields.elevationM"
            ><el-input
              v-model="form.elevationM"
              type="number"
              min="-999999999.999"
              max="999999999.999"
              step="0.001"
          /></el-form-item>
          <template v-if="kind === 'sensor'">
            <el-button text @click="copyStationCoordinates">复制台站坐标</el-button>
            <el-form-item label="类型" :error="fields.sensorType"
              ><el-input v-model="form.sensorType" maxlength="64"
            /></el-form-item>
            <el-form-item label="型号" :error="fields.model"
              ><el-input v-model="form.model" maxlength="128"
            /></el-form-item>
            <el-form-item label="序列号" :error="fields.serialNumber"
              ><el-input v-model="form.serialNumber" maxlength="128"
            /></el-form-item>
            <el-form-item label="标称采样率（Hz）" :error="fields.nominalSampleRateHz"
              ><el-input
                v-model="form.nominalSampleRateHz"
                type="number"
                min="0.00000001"
                max="999999999999.99999999"
                step="0.00000001"
            /></el-form-item>
            <details class="parameter-details">
              <summary>扩展参数</summary>
              <el-input v-model="parameters" aria-label="扩展参数 JSON" type="textarea" :rows="6" />
            </details>
          </template>
          <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
          <el-form-item label="说明" :error="fields.description"
            ><el-input v-model="form.description" type="textarea" :rows="3" maxlength="2000"
          /></el-form-item>
        </div>
        <div class="editor-actions">
          <el-button type="primary" native-type="submit" :loading="saving" :disabled="loadFailed"
            >保存</el-button
          ><el-button :disabled="saving" @click="close">取消</el-button>
        </div>
      </el-form>
    </div>
  </aside>
</template>
