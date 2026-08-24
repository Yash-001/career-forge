import apiClient from './client'

export interface DashboardProfile {
  exists: boolean
  hasTitle: boolean
  hasSummary: boolean
  hasContactInfo: boolean
  completionPercent: number
}

export interface RecentResumeEntry {
  id: string
  name: string
  latestVersionNumber: number
  updatedAt: string
}

export interface DashboardResumes {
  resumeCount: number
  versionCount: number
  recentResumes: RecentResumeEntry[]
}

export interface RecentApplicationEntry {
  id: string
  companyName: string
  jobTitle: string
  applicationDate: string
  status: string
}

export interface DashboardApplications {
  total: number
  applied: number
  interview: number
  offer: number
  rejected: number
  recentApplications: RecentApplicationEntry[]
}

export interface DashboardSubscription {
  tier: 'FREE' | 'PRO'
  status: string | null
  provider: string | null
  currentPeriodStart: string | null
  currentPeriodEnd: string | null
}

export interface DashboardUsage {
  pdfExportsUsed: number
  pdfExportsLimit: number
  atLimit: boolean
}

export interface DashboardQuickActions {
  canCreateResume: boolean
  canLogApplication: boolean
  canUpgrade: boolean
}

export interface DashboardSummary {
  profile: DashboardProfile
  resumes: DashboardResumes
  applications: DashboardApplications
  subscription: DashboardSubscription
  usage: DashboardUsage
  quickActions: DashboardQuickActions
}

function extractError(err: unknown): { message: string } {
  const e = err as { response?: { data?: { message?: string; error?: { message?: string } } } }
  const msg =
    e?.response?.data?.error?.message ??
    e?.response?.data?.message ??
    'Something went wrong. Please try again.'
  return { message: msg }
}

export const dashboardApi = {
  get: async (): Promise<DashboardSummary> => {
    const res = await apiClient.get('/dashboard')
    return res.data
  },
  extractError,
}
