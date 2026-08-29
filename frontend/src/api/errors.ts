/**
 * Shared API error extraction and friendly-message mapping.
 * All API modules delegate to this so messages are consistent.
 */

export interface ApiError {
  status: number
  code: string
  message: string
  fieldErrors?: Record<string, string>
}

type RawError = {
  response?: {
    status?: number
    data?: { code?: string; message?: string; fieldErrors?: Record<string, string> }
  }
  request?: unknown // set by axios when request was made but no response received
  message?: string
}

/** Friendly messages for well-known error codes. */
const FRIENDLY: Record<string, string> = {
  AI_PROVIDER_UNAVAILABLE:
    'AI tailoring is temporarily unavailable. You can continue editing your resume manually.',
  PDF_EXPORT_LIMIT_EXCEEDED:
    "You've reached your 3 monthly PDF exports. Upgrade to Pro for unlimited exports.",
  RESUME_LIMIT_EXCEEDED:
    'You have reached the resume limit for your plan. Upgrade to Pro to create more resumes.',
  BILLING_PROVIDER_ERROR:
    'The billing provider is temporarily unavailable. Please try again in a moment.',
  UNAUTHORIZED: 'Your session has expired. Please sign in again.',
  FORBIDDEN: 'You do not have permission to perform this action.',
  NOT_FOUND: 'The requested resource was not found.',
}

export function extractApiError(err: unknown): ApiError {
  const e = err as RawError

  // Network error — request was sent but no response received
  if (e.request && !e.response) {
    return {
      status: 0,
      code: 'NETWORK_ERROR',
      message: 'Unable to reach the server. Please check your connection and try again.',
    }
  }

  const status = e.response?.status ?? 0
  const code = e.response?.data?.code ?? 'UNKNOWN_ERROR'
  const serverMessage = e.response?.data?.message ?? ''
  const fieldErrors = e.response?.data?.fieldErrors

  // Use friendly override when available, otherwise fall back to server message
  const message = FRIENDLY[code] ?? (serverMessage || 'Something went wrong. Please try again.')

  return { status, code, message, fieldErrors }
}
