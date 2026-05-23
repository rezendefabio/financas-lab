import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import { useGlobalErrorHandler } from './useGlobalErrorHandler'
import { useErrorBannerStore } from '@/shared/shell/error-banner-store'

describe('useGlobalErrorHandler', () => {
  beforeEach(() => {
    useErrorBannerStore.setState({ banners: [] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('registra error e unhandledrejection no mount e remove no unmount', () => {
    const addSpy = vi.spyOn(window, 'addEventListener')
    const removeSpy = vi.spyOn(window, 'removeEventListener')
    vi.stubGlobal('fetch', vi.fn())

    const { unmount } = renderHook(() => useGlobalErrorHandler())

    expect(addSpy).toHaveBeenCalledWith('error', expect.any(Function))
    expect(addSpy).toHaveBeenCalledWith(
      'unhandledrejection',
      expect.any(Function),
    )

    unmount()

    expect(removeSpy).toHaveBeenCalledWith('error', expect.any(Function))
    expect(removeSpy).toHaveBeenCalledWith(
      'unhandledrejection',
      expect.any(Function),
    )
  })

  it('cria banner ao receber um ErrorEvent e atualiza codigo via POST /api/incidentes', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ codigo: 'ERR-DEADBEEF' }),
    })
    vi.stubGlobal('fetch', fetchMock)

    renderHook(() => useGlobalErrorHandler())

    act(() => {
      const event = new ErrorEvent('error', {
        message: 'falha sincrona',
        error: Object.assign(new Error('falha sincrona'), { name: 'TypeError' }),
      })
      window.dispatchEvent(event)
    })

    expect(useErrorBannerStore.getState().banners).toHaveLength(1)
    const banner = useErrorBannerStore.getState().banners[0]
    expect(banner.tipo).toBe('TypeError')
    expect(banner.mensagem).toBe('falha sincrona')
    expect(banner.codigo).toBeNull()

    expect(fetchMock).toHaveBeenCalled()
    const call = fetchMock.mock.calls[0]
    expect(call[0]).toContain('/api/incidentes')
    const body = JSON.parse((call[1] as RequestInit).body as string)
    expect(body.classeErro).toBe('TypeError')
    expect(body.operacao).toContain('CLIENT ')

    await waitFor(() => {
      expect(useErrorBannerStore.getState().banners[0].codigo).toBe(
        'ERR-DEADBEEF',
      )
    })
  })

  it('cria banner ao receber unhandledrejection com Error como reason', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ codigo: 'ERR-12345678' }),
    })
    vi.stubGlobal('fetch', fetchMock)

    renderHook(() => useGlobalErrorHandler())

    act(() => {
      const reason = Object.assign(new Error('promise quebrada'), {
        name: 'RangeError',
      })
      const event = new Event('unhandledrejection') as PromiseRejectionEvent
      Object.defineProperty(event, 'reason', { value: reason })
      Object.defineProperty(event, 'promise', { value: Promise.resolve() })
      window.dispatchEvent(event)
    })

    const banner = useErrorBannerStore.getState().banners[0]
    expect(banner.tipo).toBe('RangeError')
    expect(banner.mensagem).toBe('promise quebrada')

    await waitFor(() => {
      expect(useErrorBannerStore.getState().banners[0].codigo).toBe(
        'ERR-12345678',
      )
    })
  })

  it('usa "UnhandledRejection" e mensagem fallback quando reason e string', () => {
    vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise(() => {})))

    renderHook(() => useGlobalErrorHandler())

    act(() => {
      const event = new Event('unhandledrejection') as PromiseRejectionEvent
      Object.defineProperty(event, 'reason', { value: 'algo falhou' })
      Object.defineProperty(event, 'promise', { value: Promise.resolve() })
      window.dispatchEvent(event)
    })

    const banner = useErrorBannerStore.getState().banners[0]
    expect(banner.tipo).toBe('UnhandledRejection')
    expect(banner.mensagem).toBe('algo falhou')
  })

  it('nao quebra quando a chamada a /api/incidentes falha', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockRejectedValue(new Error('rede indisponivel')),
    )

    renderHook(() => useGlobalErrorHandler())

    act(() => {
      window.dispatchEvent(
        new ErrorEvent('error', {
          message: 'qualquer',
          error: new Error('qualquer'),
        }),
      )
    })

    expect(useErrorBannerStore.getState().banners).toHaveLength(1)
    // Banner permanece com codigo null porque o registro falhou.
    await new Promise((r) => setTimeout(r, 0))
    expect(useErrorBannerStore.getState().banners[0].codigo).toBeNull()
  })
})
