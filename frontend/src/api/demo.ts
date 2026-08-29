import apiClient from './client'

export interface DemoLoginResponse {
  userId: string
  email: string
  firstName: string | null
  lastName: string | null
  subscriptionTier: string
  accessToken: string
  refreshToken: string
}

/**
 * Calls POST /api/v1/demo/login.
 * Returns null if demo mode is not enabled on the server (403).
 * Throws for unexpected errors.
 */
export async function demoLogin(): Promise<DemoLoginResponse | null> {
  try {
    const res = await apiClient.post<DemoLoginResponse>('/demo/login')
    return res.data
  } catch (err: unknown) {
    const e = err as { response?: { status?: number } }
    if (e.response?.status === 403) return null
    throw err
  }
}
