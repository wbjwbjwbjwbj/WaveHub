<script setup lang="ts">
import { Grid, Connection } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <RouterLink to="/overview" class="brand" aria-label="WaveHub 首页">
        <span class="brand-mark">W</span>
        <span>WaveHub<small>业务管理平台</small></span>
      </RouterLink>
      <div class="nav-label">工作空间</div>
      <el-menu :default-active="route.path" router class="side-menu">
        <el-menu-item index="/overview">
          <el-icon><Grid /></el-icon><span>工作台</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <el-icon><Connection /></el-icon> 基础框架 · v0.1.0
      </div>
    </aside>
    <div class="main-shell">
      <header class="topbar">
        <span>工作空间 <span class="breadcrumb-separator">/</span> {{ route.meta.title }}</span>
        <el-tag type="info" effect="plain" round>开发工作台</el-tag>
      </header>
      <main class="page-content"><RouterView /></main>
      <footer class="page-footer">WaveHub · 让业务协作更有条理</footer>
    </div>
  </div>
</template>
