<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import StationEditor from '@/components/StationEditor.vue'
import { stationApi } from '@/api/stations'
import { getErrorMessage } from '@/api/http'
import type { Station, Sensor } from '@/types/station'

const route = useRoute(),
  router = useRouter()
const auth = useAuthStore()
const stations = ref<Station[]>([]),
  sensors = ref<Sensor[]>([]),
  selected = ref<Station | null>(null)
const stationTotal = ref(0),
  sensorTotal = ref(0),
  loading = ref(false),
  sensorsLoading = ref(false)
const error = ref(''),
  sensorError = ref(''),
  deleting = ref(false)
const readPositive = (value: unknown, fallback: number) =>
  /^\d+$/.test(String(value)) && Number(value) > 0 && Number.isSafeInteger(Number(value))
    ? Number(value)
    : fallback
const query = reactive({
  keyword: String(route.query.keyword || ''),
  enabled: undefined as boolean | undefined,
  page: readPositive(route.query.page, 1),
  size: 20,
})
const sensorQuery = reactive({
  keyword: '',
  enabled: undefined as boolean | undefined,
  page: 1,
  size: 20,
})
const editor = ref<{ kind: 'station' | 'sensor'; id: string | null } | null>(null)
const editorRef = ref<InstanceType<typeof StationEditor> | null>(null)
let stationSequence = 0,
  sensorSequence = 0
async function leaveEditor() {
  if (editorRef.value && !(await editorRef.value.confirmClose())) return false
  editor.value = null
  return true
}
async function remember() {
  await router.replace({
    query: {
      ...(selected.value ? { stationId: selected.value.id } : {}),
      ...(query.keyword ? { keyword: query.keyword } : {}),
      page: String(query.page),
    },
  })
}
async function loadStations() {
  const sequence = ++stationSequence
  loading.value = true
  error.value = ''
  try {
    const data = await stationApi.list(query)
    if (sequence !== stationSequence) return
    stations.value = data.records
    stationTotal.value = data.total
    if (!data.records.length && query.page > 1 && data.total > 0) {
      query.page = Math.ceil(data.total / query.size)
      await loadStations()
    }
  } catch (cause) {
    if (sequence === stationSequence) {
      stations.value = []
      error.value = getErrorMessage(cause)
    }
  } finally {
    if (sequence === stationSequence) loading.value = false
  }
}
async function loadSensors() {
  const sequence = ++sensorSequence,
    station = selected.value
  sensorError.value = ''
  sensors.value = []
  sensorTotal.value = 0
  if (!station) {
    sensorsLoading.value = false
    return
  }
  sensorsLoading.value = true
  try {
    const data = await stationApi.sensors(station.id, sensorQuery)
    if (sequence !== sensorSequence) return
    sensors.value = data.records
    sensorTotal.value = data.total
    if (!data.records.length && sensorQuery.page > 1 && data.total > 0) {
      sensorQuery.page = Math.ceil(data.total / sensorQuery.size)
      await loadSensors()
    }
  } catch (cause) {
    if (sequence === sensorSequence) sensorError.value = getErrorMessage(cause)
  } finally {
    if (sequence === sensorSequence) sensorsLoading.value = false
  }
}
async function selectStation(station: Station) {
  if (!(await leaveEditor())) return
  selected.value = station
  sensorQuery.page = 1
  sensorQuery.keyword = ''
  sensorQuery.enabled = undefined
  await remember()
  await loadSensors()
}
async function searchStations() {
  query.page = 1
  await loadStations()
  await remember()
}
async function searchSensors() {
  sensorQuery.page = 1
  await loadSensors()
}
async function openEditor(kind: 'station' | 'sensor', id: string | null) {
  if (await leaveEditor()) editor.value = { kind, id }
}
async function saved(id: string) {
  const kind = editor.value!.kind
  editor.value = null
  try {
    if (kind === 'station') selected.value = await stationApi.detail(id)
    else if (selected.value) selected.value = await stationApi.detail(selected.value.id)
    await Promise.all([loadStations(), loadSensors()])
    await remember()
    ElMessage.success('已保存')
  } catch (cause) {
    error.value = getErrorMessage(cause)
  }
}
async function remove(kind: 'station' | 'sensor', record: Station | Sensor) {
  if (deleting.value || !(await leaveEditor())) return
  try {
    await ElMessageBox.confirm(`删除“${record.name}”？`, '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  deleting.value = true
  try {
    if (kind === 'station') {
      await stationApi.remove(record.id)
      if (selected.value?.id === record.id) {
        selected.value = null
        await loadSensors()
      }
    } else {
      await stationApi.removeSensor(selected.value!.id, record.id)
      selected.value = await stationApi.detail(selected.value!.id)
    }
    await Promise.all([loadStations(), loadSensors()])
    await remember()
    ElMessage.success('已删除')
  } catch (cause) {
    ElMessage.error(getErrorMessage(cause))
  } finally {
    deleting.value = false
  }
}
onBeforeRouteLeave(async () => leaveEditor())
onBeforeUnmount(() => {
  auth.beforeLogout = null
})
onMounted(async () => {
  auth.beforeLogout = leaveEditor
  await loadStations()
  if (error.value) return
  const id = route.query.stationId
  if (typeof id === 'string') {
    try {
      selected.value = await stationApi.detail(id)
    } catch (cause) {
      ElMessage.error(getErrorMessage(cause))
      selected.value = null
    }
  } else selected.value = stations.value[0] ?? null
  await remember()
  await loadSensors()
})
</script>

<template>
  <div class="management-grid" :class="{ 'with-editor': editor }">
    <div class="management-lists">
      <section class="data-panel station-panel">
        <div class="section-toolbar">
          <h1>台站</h1>
          <el-button type="primary" @click="openEditor('station', null)">新增台站</el-button>
        </div>
        <form class="search-toolbar" @submit.prevent="searchStations">
          <el-input
            v-model="query.keyword"
            aria-label="台站编码或名称"
            placeholder="编码 / 名称"
            clearable
            maxlength="128"
          /><el-select
            v-model="query.enabled"
            aria-label="台站状态"
            placeholder="全部状态"
            clearable
            ><el-option label="启用" :value="true" /><el-option
              label="停用"
              :value="false" /></el-select
          ><el-button native-type="submit" :loading="loading">查询</el-button>
        </form>
        <el-alert v-if="error" :title="error" type="error" :closable="false"
          ><el-button text @click="loadStations">重试</el-button></el-alert
        >
        <el-table
          v-else
          v-loading="loading"
          :data="stations"
          row-key="id"
          :row-class-name="
            ({ row }: { row: Station }) => (row.id === selected?.id ? 'selected-row' : '')
          "
          empty-text="暂无台站"
        >
          <el-table-column
            prop="code"
            label="编码"
            min-width="150"
            show-overflow-tooltip
          /><el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="longitude" label="经度（°）" min-width="120" /><el-table-column
            prop="latitude"
            label="纬度（°）"
            min-width="120"
          /><el-table-column prop="elevationM" label="高程（m）" min-width="100"
            ><template #default="{ row }">{{ row.elevationM ?? '—' }}</template></el-table-column
          >
          <el-table-column prop="sensorCount" label="传感器" width="70" /><el-table-column
            label="状态"
            width="80"
            ><template #default="{ row }"
              ><el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{
                row.enabled ? '启用' : '停用'
              }}</el-tag></template
            ></el-table-column
          >
          <el-table-column label="操作" width="210" fixed="right"
            ><template #default="{ row }"
              ><el-button link type="primary" @click="selectStation(row)">查看传感器</el-button
              ><el-button link @click="openEditor('station', row.id)">编辑</el-button
              ><el-button link type="danger" :disabled="deleting" @click="remove('station', row)"
                >删除</el-button
              ></template
            ></el-table-column
          >
        </el-table>
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="stationTotal"
          layout="total, prev, pager, next"
          @current-change="loadStations().then(remember)"
        />
      </section>
      <section class="data-panel sensor-panel">
        <div class="section-toolbar">
          <h2>{{ selected ? `${selected.name} · 传感器` : '传感器' }}</h2>
          <el-button type="primary" :disabled="!selected" @click="openEditor('sensor', null)"
            >新增传感器</el-button
          >
        </div>
        <template v-if="selected">
          <form class="search-toolbar" @submit.prevent="searchSensors">
            <el-input
              v-model="sensorQuery.keyword"
              aria-label="传感器编码名称或序列号"
              placeholder="编码 / 名称 / 序列号"
              clearable
              maxlength="128"
            /><el-select
              v-model="sensorQuery.enabled"
              aria-label="传感器状态"
              placeholder="全部状态"
              clearable
              ><el-option label="启用" :value="true" /><el-option
                label="停用"
                :value="false" /></el-select
            ><el-button native-type="submit" :loading="sensorsLoading">查询</el-button>
          </form>
          <el-alert v-if="sensorError" :title="sensorError" type="error" :closable="false"
            ><el-button text @click="loadSensors">重试</el-button></el-alert
          >
          <el-table
            v-else
            v-loading="sensorsLoading"
            :data="sensors"
            row-key="id"
            empty-text="暂无传感器"
            ><el-table-column prop="code" label="编码" min-width="100" /><el-table-column
              prop="name"
              label="名称"
              min-width="140"
              show-overflow-tooltip
            /><el-table-column prop="longitude" label="经度（°）" min-width="120" /><el-table-column
              prop="latitude"
              label="纬度（°）"
              min-width="120"
            /><el-table-column prop="model" label="型号" min-width="100"
              ><template #default="{ row }">{{ row.model || '—' }}</template></el-table-column
            ><el-table-column label="采样率（Hz）" min-width="120"
              ><template #default="{ row }">{{
                row.nominalSampleRateHz ?? '—'
              }}</template></el-table-column
            ><el-table-column label="状态" width="80"
              ><template #default="{ row }"
                ><el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{
                  row.enabled ? '启用' : '停用'
                }}</el-tag></template
              ></el-table-column
            ><el-table-column label="操作" width="120" fixed="right"
              ><template #default="{ row }"
                ><el-button link @click="openEditor('sensor', row.id)">编辑</el-button
                ><el-button link type="danger" :disabled="deleting" @click="remove('sensor', row)"
                  >删除</el-button
                ></template
              ></el-table-column
            ></el-table
          >
          <el-pagination
            v-model:current-page="sensorQuery.page"
            :page-size="sensorQuery.size"
            :total="sensorTotal"
            layout="total, prev, pager, next"
            @current-change="loadSensors"
          />
        </template>
        <el-empty v-else description="请选择台站" :image-size="64" />
      </section>
    </div>
    <StationEditor
      v-if="editor"
      :id="editor.id"
      ref="editorRef"
      :kind="editor.kind"
      :station="selected"
      @close="editor = null"
      @saved="saved"
    />
  </div>
</template>
