import apiClient from './client'
import { extractApiError } from './errors'

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
  jobUrl: string | null
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
  analytics: AnalyticsSummary
  activity: ActivityEntry[]
}

export interface ApplicationTrendEntry {
  year: number
  month: number
  count: number
}

export interface AnalyticsSummary {
  pipelineApplied: number
  pipelineInterview: number
  pipelineOffer: number
  pipelineRejected: number
  trend: ApplicationTrendEntry[]
}

export interface ActivityEntry {
  type: string
  label: string
  subLabel: string
  linkPath: string
  occurredAt: string
}

function extractError(err: unknown): { message: string } {
  return { message: extractApiError(err).message }
}

export const dashboardApi = {
  get: async (): Promise<DashboardSummary> => {
    const res = await apiClient.get('/dashboard')
    return res.data
  },
  getAnalytics: async (): Promise<AnalyticsSummary> => {
    const res = await apiClient.get('/dashboard/analytics')
    return res.data
  },
  getActivity: async (): Promise<ActivityEntry[]> => {
    const res = await apiClient.get('/dashboard/activity')
    return res.data
  },
  extractError,
}
