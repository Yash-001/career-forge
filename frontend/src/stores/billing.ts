import { defineStore } from 'pinia'
import { ref } from 'vue'
import { billingApi, type SubscriptionResponse } from '@/api/billing'

export const useBillingStore = defineStore('billing', () => {
  const subscription = ref<SubscriptionResponse | null>(null)
  const loading = ref(false)
  const upgrading = ref(false)
  const canceling = ref(false)
  const error = ref<string | null>(null)

  async function loadSubscription() {
    loading.value = true
    error.value = null
    try {
      subscription.value = await billingApi.getSubscription()
    } catch (err) {
      error.value = billingApi.extractError(err).message
    } finally {
      loading.value = false
    }
  }

  async function upgrade(): Promise<boolean> {
    upgrading.value = true
    error.value = null
    try {
      await billingApi.checkout()
      await loadSubscription()
      return true
    } catch (err) {
      error.value = billingApi.extractError(err).message
      return false
    } finally {
      upgrading.value = false
    }
  }

  async function cancelSubscription(): Promise<boolean> {
    canceling.value = true
    error.value = null
    try {
      await billingApi.cancel()
      await loadSubscription()
      return true
    } catch (err) {
      error.value = billingApi.extractError(err).message
      return false
    } finally {
      canceling.value = false
    }
  }

  return { subscription, loading, upgrading, canceling, error, loadSubscription, upgrade, cancelSubscription }
})
