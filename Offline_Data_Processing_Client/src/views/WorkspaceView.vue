<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, mode } from '../services/api'
import { validateParameters } from '../services/domain'
const props = defineProps({ section: { type: String, required: true } })
const metadata = {
  devices: {
    title: '设备管理',
    en: 'DEVICE MANAGEMENT',
    desc: '维护传感器与采集设备，为数据处理任务建立设备连接。',
    noun: '设备',
    add: '新增设备',
  },
  models: {
    title: '模型管理',
    en: 'MODEL LIBRARY',
    desc: '集中管理算法模型，导入模型资源包并维护模型信息。',
    noun: '模型',
    add: '导入模型',
  },
  tasks: {
    title: '任务创建',
    en: 'TASK WORKSPACE',
    desc: '选择处理模型，绑定采集设备，构建你的离线数据处理任务。',
    noun: '任务',
    add: '创建任务',
  },
  runs: {
    title: '任务启停',
    en: 'EXECUTION CONTROL',
    desc: '掌握任务运行状态，调整模型参数，控制每一次数据处理。',
    noun: '任务',
    add: '创建任务',
  },
}
const info = computed(() => metadata[props.section])
const kind = computed(() => (props.section === 'runs' ? 'tasks' : props.section))
const isTask = computed(() => kind.value === 'tasks')
const statuses = {
  created: '已创建',
  running: '运行中',
  stopped: '已停止',
  succeeded: '执行成功',
  failed: '执行失败',
}
const statusTypes = {
  created: 'info',
  running: 'primary',
  stopped: 'warning',
  succeeded: 'success',
  failed: 'danger',
}
const query = reactive({ keyword: '', type: '', status: '', page: 1, pageSize: 10 })
const rows = ref([]),
  total = ref(0),
  loading = ref(false),
  error = ref(''),
  models = ref([]),
  devices = ref([])
const stats = computed(() =>
  isTask.value
    ? [
        { label: '任务总数', value: total.value, caption: '当前查询结果', icon: '⊞' },
        {
          label: '运行中',
          value: rows.value.filter((x) => x.status === 'running').length,
          caption: '当前页运行任务',
          icon: '▷',
        },
        {
          label: '待启动',
          value: rows.value.filter((x) => ['created', 'stopped'].includes(x.status)).length,
          caption: '当前页可启动任务',
          icon: '◷',
        },
      ]
    : [
        {
          label: `${info.value.noun}总数`,
          value: total.value,
          caption: '当前查询结果',
          icon: kind.value === 'devices' ? '▦' : '◈',
        },
        {
          label: '本页类型',
          value: new Set(rows.value.map((x) => x.type)).size,
          caption: '按用途分类管理',
          icon: '⊞',
        },
        {
          label: '工作模式',
          value: mode === 'demo' ? '演示' : '接口',
          caption: mode === 'demo' ? '浏览器本地保存' : '连接后端服务',
          icon: '⌁',
        },
      ],
)
let sequence = 0
async function load(silent = false) {
  const token = ++sequence,
    section = kind.value
  if (!silent) loading.value = true
  error.value = ''
  try {
    const result = await api.list(section, { ...query, keyword: query.keyword.trim() })
    if (token !== sequence) return
    rows.value = result.items
    total.value = result.total
    if (!result.items.length && query.page > 1) {
      query.page--
      return load()
    }
  } catch (e) {
    if (token === sequence) error.value = e.message
  } finally {
    if (token === sequence) loading.value = false
  }
}
async function references() {
  ;[models.value, devices.value] = await Promise.all([api.all('models'), api.all('devices')])
}
watch(
  () => props.section,
  () => {
    query.keyword = ''
    query.type = ''
    query.status = ''
    query.page = 1
    rows.value = []
    load()
    references().catch(() => {})
  },
  { immediate: true },
)
const timer = setInterval(() => {
  if (
    props.section === 'runs' &&
    !loading.value &&
    !busy.value &&
    !dialog.value &&
    !parameterDialog.value
  )
    load(true)
}, 10000)
onBeforeUnmount(() => {
  clearInterval(timer)
  sequence++
})
function search() {
  query.page = 1
  load()
}
function reset() {
  query.keyword = ''
  query.type = ''
  query.status = ''
  search()
}
function date(value) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
}
function modelName(row) {
  return (
    row.modelName ||
    row.model?.name ||
    models.value.find((m) => m.id === row.modelId)?.name ||
    row.modelId
  )
}
function deviceNames(row) {
  return (
    row.deviceNames ||
    row.devices?.map((x) => x.name) ||
    row.deviceIds?.map((id) => devices.value.find((d) => d.id === id)?.name || id) ||
    []
  )
}
const dialog = ref(false),
  busy = ref(false),
  editing = ref(''),
  file = ref(null),
  formRef = ref(),
  formError = ref('')
const form = reactive({
  name: '',
  model: '',
  type: '',
  ip: '',
  description: '',
  modelId: '',
  deviceIds: [],
})
const rules = computed(() => ({
  name: [{ required: true, whitespace: true, message: '请输入名称', trigger: 'blur' }],
  model: [{ required: true, whitespace: true, message: '请输入设备型号', trigger: 'blur' }],
  type: [{ required: true, whitespace: true, message: '请输入类型', trigger: 'blur' }],
  modelId: [{ required: true, message: '请选择处理模型', trigger: 'change' }],
  deviceIds: [
    { type: 'array', required: true, min: 1, message: '请至少选择一台设备', trigger: 'change' },
  ],
  ip: [
    {
      validator: (_rule, value, callback) =>
        !value ||
        /^(?:(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)$/.test(
          value,
        )
          ? callback()
          : callback(new Error('请输入有效的 IPv4 地址')),
      trigger: 'blur',
    },
  ],
}))
async function openEditor(row) {
  busy.value = true
  try {
    if (isTask.value) await references()
    const data = row ? await api.get(kind.value, row.id) : {}
    editing.value = row?.id || ''
    file.value = null
    formError.value = ''
    Object.keys(form).forEach((key) => delete form[key])
    Object.assign(form, {
      name: '',
      model: '',
      type: '',
      ip: '',
      description: '',
      modelId: '',
      deviceIds: [],
      ...data,
      ip: data.ip || '',
    })
    dialog.value = true
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    busy.value = false
  }
}
function chooseFile(event) {
  file.value = event.target.files[0] || null
}
async function save() {
  if (!(await formRef.value.validate().catch(() => false))) return
  if (
    kind.value === 'models' &&
    !editing.value &&
    (!file.value || !/\.zip$/i.test(file.value.name))
  ) {
    formError.value = '请选择 ZIP 模型包'
    return
  }
  busy.value = true
  formError.value = ''
  try {
    let data
    if (kind.value === 'devices')
      data = {
        name: form.name.trim(),
        model: form.model.trim(),
        type: form.type.trim(),
        ip: form.ip || null,
        description: form.description,
      }
    else if (kind.value === 'models') {
      data = { name: form.name.trim(), type: form.type.trim(), description: form.description }
      if (!editing.value) {
        const payload = new FormData()
        Object.entries(data).forEach(([key, value]) => payload.append(key, value))
        payload.append('file', file.value)
        data = payload
      }
    } else
      data = {
        name: form.name.trim(),
        modelId: form.modelId,
        deviceIds: [...form.deviceIds],
        description: form.description,
        ...(form.parameterValues && editing.value ? { parameterValues: form.parameterValues } : {}),
      }
    await api.save(kind.value, editing.value, data)
    dialog.value = false
    ElMessage.success('保存成功')
    await load()
    await references()
  } catch (e) {
    formError.value = e.message
  } finally {
    busy.value = false
  }
}
async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除“${row.name}”吗？此操作无法撤销。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  busy.value = true
  try {
    await api.remove(kind.value, row.id)
    ElMessage.success('已删除')
    await load()
    await references()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    busy.value = false
  }
}
async function action(row, command) {
  busy.value = true
  try {
    await api.action(row.id, command)
    ElMessage.success(command === 'start' ? '任务已启动' : '任务已停止')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    busy.value = false
  }
}
async function download(row) {
  busy.value = true
  try {
    const data = await api.exportModel(row.id)
    const url = URL.createObjectURL(data.blob)
    const a = document.createElement('a')
    a.href = url
    a.download = data.filename
    a.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    busy.value = false
  }
}
const parameterDialog = ref(false),
  parameterDefs = ref([]),
  parameterValues = reactive({}),
  parameterTask = ref(null),
  parameterError = ref('')
async function openParameters(row) {
  busy.value = true
  try {
    const data = await api.parameters(row.id)
    parameterDefs.value = data.parameters
    parameterTask.value = row
    parameterError.value = ''
    Object.keys(parameterValues).forEach((k) => delete parameterValues[k])
    data.parameters.forEach((p) => {
      parameterValues[p.key] =
        p.value ?? p.defaultValue ?? (p.type === 'boolean' ? false : undefined)
    })
    parameterDialog.value = true
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    busy.value = false
  }
}
async function saveParameters() {
  busy.value = true
  parameterError.value = ''
  try {
    const values = Object.fromEntries(
      Object.entries(parameterValues).filter(([, v]) => v !== undefined && v !== null),
    )
    validateParameters(parameterDefs.value, values)
    await api.saveParameters(parameterTask.value.id, values)
    parameterDialog.value = false
    ElMessage.success('参数已保存')
  } catch (e) {
    parameterError.value = e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section>
    <div class="page-heading">
      <div>
        <div class="eyebrow">{{ info.en }}</div>
        <h1>{{ info.title }}</h1>
        <p>{{ info.desc }}</p>
      </div>
      <el-button
        v-if="section !== 'runs'"
        type="primary"
        size="large"
        :disabled="busy"
        @click="openEditor()"
        >＋ {{ info.add }}</el-button
      ><RouterLink v-else to="/tasks"
        ><el-button type="primary" size="large">＋ 创建任务</el-button></RouterLink
      >
    </div>
    <div class="stats">
      <article v-for="(stat, i) in stats" :key="stat.label">
        <div>
          <p>{{ stat.label }}</p>
          <strong>{{ stat.value }}</strong
          ><small>{{ stat.caption }}</small>
        </div>
        <span :class="['stat-icon', `stat-${i}`]">{{ stat.icon }}</span>
      </article>
    </div>
    <div v-if="section === 'tasks'" class="workflow">
      <span class="step-number">01</span>
      <div><b>选择处理模型</b><small>确定本次任务的处理算法</small></div>
      <span class="step-arrow">→</span><span class="step-number">02</span>
      <div><b>绑定传感器设备</b><small>支持多台设备联合处理</small></div>
      <span class="step-arrow">→</span><span class="step-number">03</span>
      <div><b>配置并创建任务</b><small>前往任务启停页面开始运行</small></div>
    </div>
    <div class="panel">
      <div class="panel-title">
        <div>
          <h2>{{ section === 'runs' ? '运行任务' : info.noun + '列表' }}</h2>
          <span>{{ total }} 个{{ info.noun }}</span>
        </div>
        <el-button text :disabled="loading || busy" @click="load()">↻ 刷新列表</el-button>
      </div>
      <form class="filters" @submit.prevent="search">
        <el-input
          v-model="query.keyword"
          :placeholder="'搜索' + info.noun + '名称'"
          clearable
          aria-label="搜索名称"
        /><el-select
          v-if="isTask"
          v-model="query.status"
          placeholder="全部状态"
          clearable
          aria-label="任务状态"
          ><el-option
            v-for="(label, value) in statuses"
            :key="value"
            :label="label"
            :value="value" /></el-select
        ><el-input
          v-else
          v-model="query.type"
          placeholder="输入类型筛选"
          clearable
          aria-label="类型筛选"
        /><el-button type="primary" native-type="submit">查询</el-button
        ><el-button @click="reset">重置</el-button
        ><span v-if="section === 'runs'" class="refresh-hint">每 10 秒自动刷新</span>
      </form>
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        :closable="false"
        show-icon
        class="table-error"
      />
      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        style="width: 100%"
        :empty-text="error ? '加载失败，请检查连接后刷新' : '暂无数据，点击右上角按钮开始创建'"
      >
        <el-table-column label="名称" min-width="220"
          ><template #default="{ row }"
            ><div class="name-cell">
              <span class="row-icon">{{ isTask ? '⊞' : kind === 'models' ? '◈' : '▦' }}</span>
              <div>
                <b>{{ row.name }}</b
                ><small>{{ row.description || '暂无描述' }}</small>
              </div>
            </div></template
          ></el-table-column
        >
        <template v-if="kind === 'devices'"
          ><el-table-column prop="model" label="设备型号" min-width="130" /><el-table-column
            label="类型"
            min-width="120"
            ><template #default="{ row }"
              ><span class="type-pill">{{ row.type }}</span></template
            ></el-table-column
          ><el-table-column label="IP 地址" min-width="145"
            ><template #default="{ row }"
              ><span class="mono">{{ row.ip || '未配置' }}</span></template
            ></el-table-column
          ></template
        >
        <template v-else-if="kind === 'models'"
          ><el-table-column label="模型类型" min-width="120"
            ><template #default="{ row }"
              ><span class="type-pill">{{ row.type }}</span></template
            ></el-table-column
          ><el-table-column prop="fileName" label="模型文件" min-width="180" show-overflow-tooltip
        /></template>
        <template v-else
          ><el-table-column label="绑定模型" min-width="175"
            ><template #default="{ row }">{{ modelName(row) }}</template></el-table-column
          ><el-table-column label="绑定设备" min-width="170"
            ><template #default="{ row }"
              ><el-tooltip :content="deviceNames(row).join('、')"
                ><span class="device-count">▦ {{ row.deviceIds.length }} 台设备</span></el-tooltip
              ></template
            ></el-table-column
          ><el-table-column label="任务状态" width="115"
            ><template #default="{ row }"
              ><el-tag :type="statusTypes[row.status]" effect="light" round
                ><span class="tag-dot">●</span>{{ statuses[row.status] || row.status }}</el-tag
              ></template
            ></el-table-column
          ></template
        >
        <el-table-column label="创建时间" min-width="170"
          ><template #default="{ row }"
            ><span class="date-text">{{ date(row.createdAt) }}</span></template
          ></el-table-column
        >
        <el-table-column
          label="操作"
          :width="section === 'runs' ? 245 : kind === 'models' ? 175 : 130"
          fixed="right"
          ><template #default="{ row }"
            ><div class="row-actions">
              <template v-if="section === 'runs'"
                ><el-button
                  link
                  type="primary"
                  :disabled="busy || row.status === 'running'"
                  @click="openParameters(row)"
                  >配置参数</el-button
                ><el-button
                  link
                  type="success"
                  :disabled="busy || !['created', 'stopped'].includes(row.status)"
                  @click="action(row, 'start')"
                  >启动</el-button
                ><el-button
                  link
                  type="warning"
                  :disabled="busy || row.status !== 'running'"
                  @click="action(row, 'stop')"
                  >停止</el-button
                ></template
              ><template v-else
                ><el-button
                  link
                  type="primary"
                  :disabled="busy || row.status === 'running'"
                  @click="openEditor(row)"
                  >编辑</el-button
                ><el-button
                  v-if="kind === 'models'"
                  link
                  type="primary"
                  :disabled="busy || mode === 'demo'"
                  @click="download(row)"
                  >导出</el-button
                ><el-button
                  link
                  type="danger"
                  :disabled="busy || row.status === 'running'"
                  @click="remove(row)"
                  >删除</el-button
                ></template
              >
            </div></template
          ></el-table-column
        >
      </el-table>
      <div class="pagination">
        <span>共 {{ total }} 条记录</span
        ><el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="sizes, prev, pager, next"
          @current-change="load()"
          @size-change="search"
        />
      </div>
    </div>
    <div class="page-note">
      <span>ⓘ</span>
      {{
        section === 'runs'
          ? '仅已创建或已停止的任务支持启动；运行中的任务需停止后才能编辑配置。'
          : section === 'tasks'
            ? '创建任务后，可前往「任务启停」配置模型参数并启动数据处理。'
            : kind === 'models'
              ? '模型使用 ZIP 包导入；解压、存储和重新打包导出由后端服务完成。'
              : '设备 IP 为可选字段；新增设备后即可在创建任务时进行绑定。'
      }}
    </div>
  </section>
  <el-dialog
    v-model="dialog"
    :title="editing ? '编辑' + info.noun : info.add"
    width="620px"
    destroy-on-close
    :close-on-click-modal="false"
    :before-close="
      (done) => {
        if (!busy) done()
      }
    "
  >
    <el-alert
      v-if="formError"
      :title="formError"
      type="error"
      :closable="false"
      class="dialog-alert"
    />
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      :disabled="busy"
      @submit.prevent="save"
    >
      <el-form-item :label="info.noun + '名称'" prop="name"
        ><el-input
          v-model="form.name"
          maxlength="200"
          show-word-limit
          placeholder="输入便于识别的名称"
      /></el-form-item>
      <template v-if="kind === 'devices'"
        ><div class="form-grid">
          <el-form-item label="设备型号" prop="model"
            ><el-input v-model="form.model" maxlength="200" /></el-form-item
          ><el-form-item label="设备类型" prop="type"
            ><el-input v-model="form.type" maxlength="100" placeholder="例如：地震传感器"
          /></el-form-item>
        </div>
        <el-form-item label="IP 地址（可选）" prop="ip"
          ><el-input v-model="form.ip" placeholder="例如：192.168.1.101" /></el-form-item
      ></template>
      <template v-if="kind === 'models'"
        ><el-form-item label="模型类型" prop="type"
          ><el-input v-model="form.type" placeholder="例如：事件检测" /></el-form-item
        ><el-form-item v-if="!editing" label="模型 ZIP 包" required
          ><div class="file-drop">
            <span>◈</span><b>选择模型资源包</b>
            <p>仅支持 .zip 格式</p>
            <input
              type="file"
              accept=".zip,application/zip"
              aria-label="选择模型 ZIP 包"
              @change="chooseFile"
            /><small v-if="file">已选择：{{ file.name }}</small>
          </div></el-form-item
        ></template
      >
      <template v-if="isTask"
        ><el-form-item label="处理模型" prop="modelId"
          ><el-select v-model="form.modelId" placeholder="请选择模型" filterable style="width: 100%"
            ><el-option v-for="m in models" :key="m.id" :label="m.name" :value="m.id" /></el-select
          ><small v-if="!models.length">暂无模型，请先前往模型管理导入。</small></el-form-item
        ><el-form-item label="绑定设备" prop="deviceIds"
          ><el-select
            v-model="form.deviceIds"
            multiple
            filterable
            placeholder="选择一台或多台传感器设备"
            style="width: 100%"
            ><el-option v-for="d in devices" :key="d.id" :label="d.name" :value="d.id" /></el-select
          ><small v-if="!devices.length">暂无设备，请先前往设备管理新增。</small></el-form-item
        ></template
      >
      <el-form-item label="功能描述 / 备注" prop="description"
        ><el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          maxlength="1000"
          show-word-limit
          placeholder="描述用途与处理目标（可选）"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button :disabled="busy" @click="dialog = false">取消</el-button
      ><el-button type="primary" :loading="busy" @click="save">{{
        editing ? '保存修改' : kind === 'models' ? '导入模型' : '确认创建'
      }}</el-button></template
    >
  </el-dialog>
  <el-dialog
    v-model="parameterDialog"
    title="模型参数配置"
    width="580px"
    destroy-on-close
    :close-on-click-modal="false"
    :before-close="
      (done) => {
        if (!busy) done()
      }
    "
  >
    <p class="parameter-subtitle">
      {{ parameterTask?.name }} · {{ parameterTask ? modelName(parameterTask) : '' }}
    </p>
    <el-alert
      v-if="parameterError"
      :title="parameterError"
      type="error"
      :closable="false"
      class="dialog-alert"
    />
    <el-empty v-if="!parameterDefs.length" description="此模型未提供可配置参数" />
    <el-form label-position="top" :disabled="busy"
      ><el-form-item v-for="p in parameterDefs" :key="p.key" :label="p.label" :required="p.required"
        ><el-switch v-if="p.type === 'boolean'" v-model="parameterValues[p.key]" /><el-select
          v-else-if="p.type === 'select'"
          v-model="parameterValues[p.key]"
          clearable
          style="width: 100%"
          ><el-option
            v-for="option in p.options"
            :key="option"
            :value="option"
            :label="option" /></el-select
        ><el-input-number
          v-else-if="['number', 'integer'].includes(p.type)"
          v-model="parameterValues[p.key]"
          :min="p.min"
          :max="p.max"
          :precision="p.type === 'integer' ? 0 : undefined"
          :step="p.type === 'integer' ? 1 : 0.1"
        /><el-input v-else v-model="parameterValues[p.key]" /><small
          v-if="p.description"
          class="parameter-description"
          >{{ p.description }}</small
        ></el-form-item
      ></el-form
    >
    <template #footer
      ><el-button :disabled="busy" @click="parameterDialog = false">取消</el-button
      ><el-button
        type="primary"
        :disabled="!parameterDefs.length"
        :loading="busy"
        @click="saveParameters"
        >保存参数</el-button
      ></template
    >
  </el-dialog>
</template>
