<script setup lang="ts">
import { Location } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/api/http'

const route = useRoute()
const router = useRouter(),
  auth = useAuthStore()
async function logout() {
  try {
    if (await auth.logout()) await router.replace('/login')
  } catch (cause) {
    ElMessage.error(getErrorMessage(cause))
  }
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <RouterLink to="/stations" class="brand" aria-label="WaveHub 首页">
        <span class="brand-mark">W</span>
        <span>WaveHub</span>
      </RouterLink>
      <el-menu :default-active="route.path" router class="side-menu">
        <el-menu-item index="/stations" aria-label="台站管理">
          <el-icon><Location /></el-icon><span>台站管理</span>
        </el-menu-item>
      </el-menu>
    </aside>
    <div class="main-shell">
      <header class="topbar">
        <span>{{ route.meta.title }}</span>
        <div class="account-menu">
          <span>{{ auth.username }}</span><el-button text :loading="auth.loggingOut" @click="logout"
            >退出</el-button
          >
        </div>
      </header>
      <main class="page-content"><RouterView /></main>
    </div>
  </div>
</template>
