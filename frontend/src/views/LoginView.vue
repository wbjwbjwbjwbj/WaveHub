<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getErrorMessage } from '@/api/http'
const auth = useAuthStore(),
  router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false),
  error = ref('')
async function login() {
  if (loading.value || !form.username || !form.password) return
  loading.value = true
  error.value = ''
  try {
    await auth.login(form.username.trim(), form.password)
    await router.replace('/stations')
  } catch (cause) {
    error.value = getErrorMessage(cause)
  } finally {
    loading.value = false
  }
}
</script>
<template>
  <main class="login-page">
    <form class="login-form" @submit.prevent="login">
      <h1>WaveHub</h1>
      <el-input
        v-model="form.username"
        aria-label="用户名"
        placeholder="用户名"
        autocomplete="username"
      /><el-input
        v-model="form.password"
        aria-label="密码"
        placeholder="密码"
        type="password"
        show-password
        autocomplete="current-password"
      /><el-alert v-if="error" :title="error" type="error" :closable="false" /><el-button
        type="primary"
        native-type="submit"
        :loading="loading"
        :disabled="!form.username || !form.password"
        >登录</el-button
      >
    </form>
  </main>
</template>
