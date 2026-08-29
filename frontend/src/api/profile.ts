import apiClient from './client'
import { extractApiError } from './errors'
export type { ApiError } from './errors'

export type EmploymentType =
  | 'FULL_TIME'
  | 'PART_TIME'
  | 'CONTRACT'
  | 'INTERNSHIP'
  | 'FREELANCE'

export type ProficiencyLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT'

export interface ProfileData {
  id: string
  phone: string | null
  location: string | null
  professionalTitle: string | null
  professionalSummary: string | null
  linkedinUrl: string | null
  githubUrl: string | null
  portfolioUrl: string | null
  createdAt: string
  updatedAt: string
}

export interface WorkExperience {
  id: string
  companyName: string
  jobTitle: string
  location: string | null
  employmentType: EmploymentType | null
  startDate: string
  endDate: string | null
  currentlyWorking: boolean
  description: string | null
  displayOrder: number
  createdAt: string
  updatedAt: string
}

export interface Education {
  id: string
  institutionName: string
  degree: string | null
  fieldOfStudy: string | null
  location: string | null
  startDate: string | null
  endDate: string | null
  grade: string | null
  description: string | null
  displayOrder: number
  createdAt: string
  updatedAt: string
}

export interface Skill {
  id: string
  name: string
  category: string | null
  proficiency: ProficiencyLevel | null
  displayOrder: number
  createdAt: string
  updatedAt: string
}

export interface UpdateProfilePayload {
  phone?: string
  location?: string
  professionalTitle?: string
  professionalSummary?: string
  linkedinUrl?: string
  githubUrl?: string
  portfolioUrl?: string
}

export interface WorkExperiencePayload {
  companyName: string
  jobTitle: string
  location?: string
  employmentType?: EmploymentType | null
  startDate: string
  endDate?: string | null
  currentlyWorking: boolean
  description?: string
  displayOrder: number
}

export interface EducationPayload {
  institutionName: string
  degree?: string
  fieldOfStudy?: string
  location?: string
  startDate?: string | null
  endDate?: string | null
  grade?: string
  description?: string
  displayOrder: number
}

export interface SkillPayload {
  name: string
  category?: string
  proficiency?: ProficiencyLevel | null
  displayOrder: number
}

export const profileApi = {
  async getProfile(): Promise<ProfileData> {
    const r = await apiClient.get<ProfileData>('/profile')
    return r.data
  },

  async upsertProfile(payload: UpdateProfilePayload): Promise<ProfileData> {
    const r = await apiClient.put<ProfileData>('/profile', payload)
    return r.data
  },

  async getExperiences(): Promise<WorkExperience[]> {
    const r = await apiClient.get<WorkExperience[]>('/profile/experience')
    return r.data
  },

  async createExperience(payload: WorkExperiencePayload): Promise<WorkExperience> {
    const r = await apiClient.post<WorkExperience>('/profile/experience', payload)
    return r.data
  },

  async updateExperience(id: string, payload: WorkExperiencePayload): Promise<WorkExperience> {
    const r = await apiClient.put<WorkExperience>(`/profile/experience/${id}`, payload)
    return r.data
  },

  async deleteExperience(id: string): Promise<void> {
    await apiClient.delete(`/profile/experience/${id}`)
  },

  async getEducations(): Promise<Education[]> {
    const r = await apiClient.get<Education[]>('/profile/education')
    return r.data
  },

  async createEducation(payload: EducationPayload): Promise<Education> {
    const r = await apiClient.post<Education>('/profile/education', payload)
    return r.data
  },

  async updateEducation(id: string, payload: EducationPayload): Promise<Education> {
    const r = await apiClient.put<Education>(`/profile/education/${id}`, payload)
    return r.data
  },

  async deleteEducation(id: string): Promise<void> {
    await apiClient.delete(`/profile/education/${id}`)
  },

  async getSkills(): Promise<Skill[]> {
    const r = await apiClient.get<Skill[]>('/profile/skills')
    return r.data
  },

  async createSkill(payload: SkillPayload): Promise<Skill> {
    const r = await apiClient.post<Skill>('/profile/skills', payload)
    return r.data
  },

  async updateSkill(id: string, payload: SkillPayload): Promise<Skill> {
    const r = await apiClient.put<Skill>(`/profile/skills/${id}`, payload)
    return r.data
  },

  async deleteSkill(id: string): Promise<void> {
    await apiClient.delete(`/profile/skills/${id}`)
  },

  extractError: extractApiError,
}
