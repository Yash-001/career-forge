import apiClient from './client'
export type { ApiError } from './errors'

export interface AuthResponse {
  userId: string
  email: string
  firstName: string | null
  lastName: string | null
  subscriptionTier: string
  accessToken: string
  refreshToken: string
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const res = await apiClient.post('/auth/login', { email, password })
  return res.data
}

export async function register(
  firstName: string,
  lastName: string,
  email: string,
  password: string,
): Promise<AuthResponse> {
  const res = await apiClient.post('/auth/register', { firstName, lastName, email, password })
  return res.data
}
