import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { apiFetch } from './api-client'
import { ApiError } from '@/types/api'
import * as authModule from '@/lib/auth'

describe('apiFetch', () => {
  beforeEach(() => {
    vi.spyOn(authModule, 'getToken')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('adds Authorization header when token exists', async () => {
    vi.mocked(authModule.getToken).mockReturnValue('test-token')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ data: 'result' }),
    }))

    await apiFetch('/test')

    const fetchCall = vi.mocked(fetch).mock.calls[0]
    expect((fetchCall[1] as RequestInit)?.headers).toMatchObject({
      Authorization: 'Bearer test-token',
    })

    vi.unstubAllGlobals()
  })

  it('omits Authorization header when no token', async () => {
    vi.mocked(authModule.getToken).mockReturnValue(null)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({}),
    }))

    await apiFetch('/test')

    const fetchCall = vi.mocked(fetch).mock.calls[0]
    const headers = (fetchCall[1] as RequestInit)?.headers as Record<string, string>
    expect(headers.Authorization).toBeUndefined()

    vi.unstubAllGlobals()
  })

  it('throws ApiError on non-ok response', async () => {
    vi.mocked(authModule.getToken).mockReturnValue(null)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      json: async () => ({ message: 'Nao autorizado' }),
    }))

    await expect(apiFetch('/protected')).rejects.toBeInstanceOf(ApiError)

    vi.unstubAllGlobals()
  })

  it('returns undefined for 204 status', async () => {
    vi.mocked(authModule.getToken).mockReturnValue(null)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 204,
    }))

    const result = await apiFetch('/delete')
    expect(result).toBeUndefined()

    vi.unstubAllGlobals()
  })
})
