import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dashboardApi, type DashboardSummary } from '@/api/dashboard'

export const useDashboardStore = defineStore('dashboard', () => {
  const summary = ref<DashboardSummary | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function loadDashboard() {
    loading.value = true
    error.value = null
    try {
      summary.value = await dashboardApi.get()
    } catch (err) {
      error.value = dashboardApi.extractError(err).message
    } finally {
      loading.value = false
    }
  }

  return { summary, loading, error, loadDashboard }
})
