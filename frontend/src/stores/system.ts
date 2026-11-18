import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getSystemInfo } from '@/api/system'
import { getErrorMessage } from '@/api/http'
import type { SystemInfo } from '@/types/api'

export const useSystemStore = defineStore('system', () => {
  const info = ref<SystemInfo | null>(null)
  const loading = ref(false)
  const error = ref('')

  async function refresh() {
    if (loading.value) return
    loading.value = true
    error.value = ''
    info.value = null
    try {
      info.value = await getSystemInfo()
    } catch (cause) {
      error.value = getErrorMessage(cause)
    } finally {
      loading.value = false
    }
  }

  return { info, loading, error, refresh }
})
