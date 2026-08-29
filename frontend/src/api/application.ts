import apiClient from './client'
import { extractApiError } from './errors'
export type { ApiError } from './errors'

export type ApplicationStatus = 'APPLIED' | 'INTERVIEW' | 'OFFER' | 'REJECTED'

export interface ApplicationResponse {
  id: string
  companyName: string
  jobTitle: string
  applicationDate: string
  jobUrl: string | null
  status: ApplicationStatus
  resumeVersionId: string | null
  resumeVersionTitle: string | null
  resumeVersionNumber: number | null
  createdAt: string
  updatedAt: string
}

export interface CreateApplicationPayload {
  companyName: string
  jobTitle: string
  applicationDate: string
  jobUrl?: string | null
  status?: ApplicationStatus
  resumeVersionId?: string | null
}

export interface UpdateApplicationPayload {
  companyName: string
  jobTitle: string
  applicationDate: string
  jobUrl?: string | null
  status: ApplicationStatus
  resumeVersionId?: string | null
}

export const applicationApi = {
  async list(): Promise<ApplicationResponse[]> {
    const r = await apiClient.get<ApplicationResponse[]>('/applications')
    return r.data
  },

  async get(id: string): Promise<ApplicationResponse> {
    const r = await apiClient.get<ApplicationResponse>(`/applications/${id}`)
    return r.data
  },

  async create(payload: CreateApplicationPayload): Promise<ApplicationResponse> {
    const r = await apiClient.post<ApplicationResponse>('/applications', payload)
    return r.data
  },

  async update(id: string, payload: UpdateApplicationPayload): Promise<ApplicationResponse> {
    const r = await apiClient.put<ApplicationResponse>(`/applications/${id}`, payload)
    return r.data
  },

  async remove(id: string): Promise<void> {
    await apiClient.delete(`/applications/${id}`)
  },

  extractError: extractApiError,
}
