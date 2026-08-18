import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { applicationApi, type ApplicationResponse, type ApplicationStatus } from '@/api/application'

export const useApplicationStore = defineStore('application', () => {
  const applications = ref<ApplicationResponse[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const byStatus = computed(() => (status: ApplicationStatus) =>
    applications.value.filter((a) => a.status === status),
  )

  async function loadApplications() {
    loading.value = true
    error.value = null
    try {
      applications.value = await applicationApi.list()
    } catch (err) {
      error.value = applicationApi.extractError(err).message
    } finally {
      loading.value = false
    }
  }

  async function addApplication(payload: Parameters<typeof applicationApi.create>[0]): Promise<ApplicationResponse> {
    const created = await applicationApi.create(payload)
    applications.value.unshift(created)
    return created
  }

  async function editApplication(id: string, payload: Parameters<typeof applicationApi.update>[1]): Promise<ApplicationResponse> {
    const updated = await applicationApi.update(id, payload)
    const idx = applications.value.findIndex((a) => a.id === id)
    if (idx !== -1) applications.value[idx] = updated
    return updated
  }

  async function removeApplication(id: string): Promise<void> {
    await applicationApi.remove(id)
    applications.value = applications.value.filter((a) => a.id !== id)
  }

  return { applications, loading, error, byStatus, loadApplications, addApplication, editApplication, removeApplication }
})
