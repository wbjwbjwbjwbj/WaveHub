<script setup lang="ts">
import { onMounted } from 'vue'
import { Refresh, Right } from '@element-plus/icons-vue'
import { useSystemStore } from '@/stores/system'

const system = useSystemStore()
onMounted(() => void system.refresh())

const modules = [
  {
    number: '01',
    title: '页面与交互',
    text: 'Vue 3 · TypeScript · Element Plus',
    status: '已就绪',
  },
  {
    number: '02',
    title: '接口与服务',
    text: 'Spring Boot · Spring Security · MyBatis-Plus',
    status: '已搭建',
  },
  { number: '03', title: '业务与权限', text: '根据业务需求扩展用户、角色和菜单', status: '待开发' },
]
</script>

<template>
  <section class="page-heading">
    <div>
      <div class="eyebrow">WORKSPACE OVERVIEW</div>
      <h1>工作台</h1>
      <p>从这里开始，构建你的业务管理系统。</p>
    </div>
    <el-button :icon="Refresh" :loading="system.loading" @click="system.refresh"
      >刷新状态</el-button
    >
  </section>

  <section class="welcome-panel">
    <div>
      <span class="welcome-label">WELCOME TO WAVEHUB</span>
      <h2>基础已就绪，业务由此展开。</h2>
      <p>连接页面、服务与数据，逐步构建清晰的业务工作流。</p>
      <span class="welcome-link"
        >Vue + Spring Boot <el-icon><Right /></el-icon
      ></span>
    </div>
    <div class="wave-art" aria-hidden="true">
      <span></span><span></span><span></span><span></span><span></span>
    </div>
  </section>

  <div class="module-grid">
    <article v-for="item in modules" :key="item.number" class="module-card">
      <div class="module-top">
        <span class="module-number">{{ item.number }}</span><el-tag
          :type="item.status === '待开发' ? 'info' : 'success'"
          size="small"
          effect="plain"
          >{{ item.status }}</el-tag
        >
      </div>
      <h3>{{ item.title }}</h3>
      <p>{{ item.text }}</p>
    </article>
  </div>

  <section class="status-panel">
    <div class="panel-heading">
      <div>
        <h2>服务连接</h2>
        <p>当前页面通过真实 API 获取后端状态。</p>
      </div>
      <el-tag :type="system.info ? 'success' : system.error ? 'danger' : 'info'">{{
        system.info ? '后端已连接' : system.error ? '后端未连接' : '正在检查'
      }}</el-tag>
    </div>
    <el-alert
      v-if="system.error"
      :title="system.error"
      type="warning"
      :closable="false"
      show-icon
    />
    <el-skeleton v-else-if="system.loading" :rows="3" animated />
    <el-descriptions v-else-if="system.info" :column="1" border>
      <el-descriptions-item label="服务名称">{{ system.info.name }}</el-descriptions-item>
      <el-descriptions-item label="应用版本">{{ system.info.version }}</el-descriptions-item>
      <el-descriptions-item label="数据库模式">{{
        system.info.databaseMode === 'kingbase'
          ? 'KingbaseES 模式（连接状态请查看健康检查）'
          : '独立启动模式（尚未接入数据库）'
      }}</el-descriptions-item>
      <el-descriptions-item label="服务时间">{{
        new Date(system.info.serverTime).toLocaleString('zh-CN')
      }}</el-descriptions-item>
    </el-descriptions>
    <p class="panel-note">当前为项目骨架，登录、权限管理和业务数据将在后续按需实现。</p>
  </section>
</template>
