import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { apiFetch, apiFetchBlob } from './api-client'
import { ApiError } from '@/shared/types/api'
import * as authModule from '@/shared/lib/auth'

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
      status: 500,
      statusText: 'Internal Server Error',
      json: async () => ({ message: 'Erro interno' }),
    }))

    await expect(apiFetch('/protected')).rejects.toBeInstanceOf(ApiError)

    vi.unstubAllGlobals()
  })

  it('on 401: clears token and redirects to /login', async () => {
    vi.spyOn(authModule, 'clearToken')
    vi.mocked(authModule.getToken).mockReturnValue('expired-token')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      json: async () => ({ message: 'Nao autorizado' }),
    }))

    const location = { href: '' }
    Object.defineProperty(window, 'location', { value: location, writable: true })

    await expect(apiFetch('/protected')).rejects.toBeInstanceOf(ApiError)
    expect(authModule.clearToken).toHaveBeenCalledOnce()
    expect(location.href).toBe('/login')

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

describe('apiFetchBlob', () => {
  beforeEach(() => {
    vi.spyOn(authModule, 'getToken')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('returns the response body as Blob on success', async () => {
    const blob = new Blob(['col-a,col-b'], { type: 'text/csv' })
    vi.mocked(authModule.getToken).mockReturnValue('test-token')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      blob: async () => blob,
    }))

    const result = await apiFetchBlob('/api/modelo')

    expect(result).toBe(blob)
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
      blob: async () => new Blob([]),
    }))

    await apiFetchBlob('/api/modelo')

    const fetchCall = vi.mocked(fetch).mock.calls[0]
    const headers = (fetchCall[1] as RequestInit)?.headers as Record<string, string>
    expect(headers.Authorization).toBeUndefined()

    vi.unstubAllGlobals()
  })

  it('throws ApiError on non-ok response', async () => {
    vi.mocked(authModule.getToken).mockReturnValue(null)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
    }))

    await expect(apiFetchBlob('/api/modelo')).rejects.toBeInstanceOf(ApiError)

    vi.unstubAllGlobals()
  })

  it('on 401: clears token and redirects to /login', async () => {
    vi.spyOn(authModule, 'clearToken')
    vi.mocked(authModule.getToken).mockReturnValue('expired-token')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
    }))

    const location = { href: '' }
    Object.defineProperty(window, 'location', { value: location, writable: true })

    await expect(apiFetchBlob('/api/modelo')).rejects.toBeInstanceOf(ApiError)
    expect(authModule.clearToken).toHaveBeenCalledOnce()
    expect(location.href).toBe('/login')

    vi.unstubAllGlobals()
  })
})
