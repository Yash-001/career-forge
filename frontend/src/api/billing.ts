import apiClient from './client'
import { extractApiError } from './errors'
export type { ApiError as BillingApiError } from './errors'

export type SubscriptionTier = 'FREE' | 'PRO'
export type SubscriptionStatus = 'ACTIVE' | 'INACTIVE' | 'CANCELED' | 'PAST_DUE'
export type BillingProvider = 'DEMO' | 'STRIPE'

export interface SubscriptionResponse {
  tier: SubscriptionTier
  status: SubscriptionStatus
  provider: BillingProvider
  currentPeriodStart: string | null
  currentPeriodEnd: string | null
  pdfExportsUsed: number | null
  pdfExportsLimit: number | null
}

export interface CheckoutResponse {
  action: string
  tier: SubscriptionTier
  status: SubscriptionStatus
  message: string
}

export const billingApi = {
  async getSubscription(): Promise<SubscriptionResponse> {
    const r = await apiClient.get<SubscriptionResponse>('/billing/subscription')
    return r.data
  },

  async checkout(): Promise<CheckoutResponse> {
    const r = await apiClient.post<CheckoutResponse>('/billing/checkout')
    return r.data
  },

  async cancel(): Promise<SubscriptionResponse> {
    const r = await apiClient.post<SubscriptionResponse>('/billing/cancel')
    return r.data
  },

  extractError: extractApiError,
}
