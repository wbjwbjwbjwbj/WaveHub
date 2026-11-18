<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { mode, setMode } from './services/api'
const route = useRoute()
const router = useRouter()
const links = [
  { path: '/devices', label: '设备管理', icon: '▦', caption: '传感器与采集设备' },
  { path: '/models', label: '模型管理', icon: '◈', caption: '算法模型与资源包' },
  { path: '/tasks', label: '任务创建', icon: '⊞', caption: '配置模型与设备绑定' },
  { path: '/runs', label: '任务启停', icon: '▷', caption: '运行控制与参数配置' },
]
const current = computed(() => links.find((x) => x.path === route.path) || links[0])
const selectedMode = ref(mode)
function changeMode(value) {
  setMode(value)
  router.go(0)
}
</script>
<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-icon">∿</span>
        <div>离线数据处理<span>OFFLINE WORKSPACE</span></div>
      </div>
      <div class="workspace-label">工作空间 <span>LOCAL</span></div>
      <nav>
        <RouterLink v-for="link in links" :key="link.path" :to="link.path"
          ><i>{{ link.icon }}</i>
          <div>
            {{ link.label }}<small>{{ link.caption }}</small>
          </div></RouterLink
        >
      </nav>
      <div class="sidebar-foot">
        <span class="local-dot"></span> 本地工作空间<small>数据处理 · 模型编排 · 任务执行</small>
      </div>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <div>
          工作空间 <span>/</span> <b>{{ current.label }}</b>
        </div>
        <div class="topbar-right">
          <span :class="['mode-dot', { live: selectedMode === 'api' }]"></span
          ><el-select
            v-model="selectedMode"
            aria-label="数据模式"
            style="width: 144px"
            @change="changeMode"
            ><el-option label="本地演示模式" value="demo" /><el-option
              label="真实接口模式"
              value="api" /></el-select
          ><span class="avatar">研</span>
        </div>
      </header>
      <main>
        <div class="demo-notice" v-if="selectedMode === 'demo'">
          演示工作空间 ·
          数据仅保存在此浏览器，模型导入与任务运行均为模拟。切换“真实接口模式”连接后端。
        </div>
        <RouterView />
      </main>
      <footer>离线数据处理客户端 <span>研发工作台 / v1.0</span></footer>
    </div>
  </div>
</template>
