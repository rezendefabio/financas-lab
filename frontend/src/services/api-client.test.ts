import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { apiFetch, apiFetchBlob } from './api-client'
import { ApiError } from '@/shared/types/api'
import * as authModule from '@/shared/lib/auth'
import { useErrorBannerStore } from '@/shared/shell/error-banner-store'

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

  it('on 401: clears session and redirects to /login', async () => {
    vi.spyOn(authModule, 'clearSession').mockResolvedValue()
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
    expect(authModule.clearSession).toHaveBeenCalledOnce()
    expect(location.href).toBe('/login')

    vi.unstubAllGlobals()
  })

  it('on 500 with codigoErro: publishes banner and throws ApiError com codigo', async () => {
    useErrorBannerStore.setState({ banners: [] })
    vi.mocked(authModule.getToken).mockReturnValue(null)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
      json: async () => ({ codigoErro: 'ERR-ABCDEF12', detail: 'Falha geral' }),
    }))

    await expect(apiFetch('/x')).rejects.toMatchObject({
      status: 500,
      message: expect.stringContaining('ERR-ABCDEF12'),
    })
    const banners = useErrorBannerStore.getState().banners
    expect(banners).toHaveLength(1)
    expect(banners[0].codigo).toBe('ERR-ABCDEF12')
    expect(banners[0].tipo).toBe('ServerError')
    expect(banners[0].mensagem).toBe('Falha geral')

    vi.unstubAllGlobals()
  })

  it('on 502 sem codigoErro: publishes banner com codigo null e lanca ApiError', async () => {
    useErrorBannerStore.setState({ banners: [] })
    vi.mocked(authModule.getToken).mockReturnValue(null)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 502,
      statusText: 'Bad Gateway',
      json: async () => ({}),
    }))

    await expect(apiFetch('/x')).rejects.toBeInstanceOf(ApiError)
    const banners = useErrorBannerStore.getState().banners
    expect(banners).toHaveLength(1)
    expect(banners[0].codigo).toBeNull()
    expect(banners[0].mensagem).toBe('Erro interno do servidor')

    vi.unstubAllGlobals()
  })

  it('on 503: publishes banner (range >=500 cobre todo 5xx)', async () => {
    useErrorBannerStore.setState({ banners: [] })
    vi.mocked(authModule.getToken).mockReturnValue(null)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 503,
      statusText: 'Service Unavailable',
      json: async () => ({ detail: 'Em manutencao' }),
    }))

    await expect(apiFetch('/x')).rejects.toBeInstanceOf(ApiError)
    const banners = useErrorBannerStore.getState().banners
    expect(banners).toHaveLength(1)
    expect(banners[0].mensagem).toBe('Em manutencao')

    vi.unstubAllGlobals()
  })

  it('on 400/422: nao publica banner (range >=500 nao se aplica)', async () => {
    useErrorBannerStore.setState({ banners: [] })
    vi.mocked(authModule.getToken).mockReturnValue(null)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 422,
      statusText: 'Unprocessable Entity',
      json: async () => ({ message: 'Validacao falhou' }),
    }))

    await expect(apiFetch('/x')).rejects.toBeInstanceOf(ApiError)
    expect(useErrorBannerStore.getState().banners).toHaveLength(0)

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

  it('on 401: clears session and redirects to /login', async () => {
    vi.spyOn(authModule, 'clearSession').mockResolvedValue()
    vi.mocked(authModule.getToken).mockReturnValue('expired-token')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
    }))

    const location = { href: '' }
    Object.defineProperty(window, 'location', { value: location, writable: true })

    await expect(apiFetchBlob('/api/modelo')).rejects.toBeInstanceOf(ApiError)
    expect(authModule.clearSession).toHaveBeenCalledOnce()
    expect(location.href).toBe('/login')

    vi.unstubAllGlobals()
  })
})
