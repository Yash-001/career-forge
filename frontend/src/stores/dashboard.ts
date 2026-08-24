import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dashboardApi, type ActivityEntry, type AnalyticsSummary, type DashboardSummary } from '@/api/dashboard'

export const useDashboardStore = defineStore('dashboard', () => {
  const summary = ref<DashboardSummary | null>(null)
  const analytics = ref<AnalyticsSummary | null>(null)
  const activity = ref<ActivityEntry[]>([])
  const loading = ref(false)
  const analyticsLoading = ref(false)
  const activityLoading = ref(false)
  const error = ref<string | null>(null)
  const analyticsError = ref<string | null>(null)
  const activityError = ref<string | null>(null)

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

  async function loadAnalytics() {
    analyticsLoading.value = true
    analyticsError.value = null
    try {
      analytics.value = await dashboardApi.getAnalytics()
    } catch (err) {
      analyticsError.value = dashboardApi.extractError(err).message
    } finally {
      analyticsLoading.value = false
    }
  }

  async function loadActivity() {
    activityLoading.value = true
    activityError.value = null
    try {
      activity.value = await dashboardApi.getActivity()
    } catch (err) {
      activityError.value = dashboardApi.extractError(err).message
    } finally {
      activityLoading.value = false
    }
  }

  return {
    summary, analytics, activity,
    loading, analyticsLoading, activityLoading,
    error, analyticsError, activityError,
    loadDashboard, loadAnalytics, loadActivity,
  }
})
