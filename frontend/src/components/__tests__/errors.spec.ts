import { describe, it, expect } from 'vitest'
import { extractApiError } from '@/api/errors'

describe('extractApiError', () => {
  it('returns friendly message for AI_PROVIDER_UNAVAILABLE', () => {
    const err = { response: { status: 503, data: { code: 'AI_PROVIDER_UNAVAILABLE', message: 'raw' } } }
    const result = extractApiError(err)
    expect(result.code).toBe('AI_PROVIDER_UNAVAILABLE')
    expect(result.message).toContain('AI tailoring is temporarily unavailable')
  })

  it('returns friendly message for PDF_EXPORT_LIMIT_EXCEEDED', () => {
    const err = { response: { status: 402, data: { code: 'PDF_EXPORT_LIMIT_EXCEEDED', message: 'raw' } } }
    const result = extractApiError(err)
    expect(result.message).toContain("You've reached your 3 monthly PDF exports")
    expect(result.message).toContain('Upgrade to Pro')
  })

  it('returns friendly message for BILLING_PROVIDER_ERROR', () => {
    const err = { response: { status: 502, data: { code: 'BILLING_PROVIDER_ERROR', message: 'raw' } } }
    const result = extractApiError(err)
    expect(result.message).toContain('billing provider is temporarily unavailable')
  })

  it('returns friendly message for RESUME_LIMIT_EXCEEDED', () => {
    const err = { response: { status: 402, data: { code: 'RESUME_LIMIT_EXCEEDED', message: 'raw' } } }
    const result = extractApiError(err)
    expect(result.message).toContain('resume limit')
  })

  it('returns network error message when no response', () => {
    const err = { request: {}, message: 'Network Error' }
    const result = extractApiError(err)
    expect(result.status).toBe(0)
    expect(result.code).toBe('NETWORK_ERROR')
    expect(result.message).toContain('Unable to reach the server')
  })

  it('falls back to server message for unknown codes', () => {
    const err = { response: { status: 400, data: { code: 'SOME_CODE', message: 'Custom server message.' } } }
    const result = extractApiError(err)
    expect(result.message).toBe('Custom server message.')
  })

  it('returns generic message when no code and no message', () => {
    const err = { response: { status: 500, data: {} } }
    const result = extractApiError(err)
    expect(result.message).toBe('Something went wrong. Please try again.')
  })

  it('returns status and code from response', () => {
    const err = { response: { status: 404, data: { code: 'NOT_FOUND', message: 'Not found.' } } }
    const result = extractApiError(err)
    expect(result.status).toBe(404)
    expect(result.code).toBe('NOT_FOUND')
  })

  it('includes fieldErrors when present', () => {
    const err = {
      response: {
        status: 400,
        data: { code: 'VALIDATION_ERROR', message: 'Invalid.', fieldErrors: { email: 'Invalid email.' } },
      },
    }
    const result = extractApiError(err)
    expect(result.fieldErrors).toEqual({ email: 'Invalid email.' })
  })

  it('handles completely empty error object', () => {
    const result = extractApiError({})
    expect(result.status).toBe(0)
    expect(result.code).toBe('UNKNOWN_ERROR')
    expect(result.message).toBe('Something went wrong. Please try again.')
  })
})
