import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { Notificacao } from '@/features/notificacoes/types/notificacao'

vi.mock('@/features/notificacoes/services/notificacao-service', () => ({
  notificacoesService: {
    listar: vi.fn(),
    descartar: vi.fn(),
  },
}))

import { useNotificacoes, useDescartarNotificacao } from './useNotificacoes'
import { notificacoesService } from '@/features/notificacoes/services/notificacao-service'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const notificacao: Notificacao = {
  id: 'notif-001',
  tipo: 'ORCAMENTO_EXCEDIDO',
  referenciaId: 'orc-001',
  titulo: 'Orcamento excedido',
  descricao: 'Alimentacao: 120% utilizado',
  criadoEm: '2024-05-01T00:00:00Z',
}

describe('useNotificacoes', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('busca as notificacoes do backend via service', async () => {
    vi.mocked(notificacoesService.listar).mockResolvedValue([notificacao])

    const { result } = renderHook(() => useNotificacoes(), { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(result.current.notificacoes).toHaveLength(1)
    })
    expect(notificacoesService.listar).toHaveBeenCalled()
    expect(result.current.notificacoes[0].titulo).toBe('Orcamento excedido')
  })

  it('retorna lista vazia enquanto carrega', () => {
    vi.mocked(notificacoesService.listar).mockImplementation(() => new Promise(() => {}))

    const { result } = renderHook(() => useNotificacoes(), { wrapper: makeWrapper() })

    expect(result.current.notificacoes).toEqual([])
    expect(result.current.isLoading).toBe(true)
  })
})

describe('useDescartarNotificacao', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('chama o service de descarte com o id', async () => {
    vi.mocked(notificacoesService.descartar).mockResolvedValue(undefined)

    const { result } = renderHook(() => useDescartarNotificacao(), { wrapper: makeWrapper() })
    result.current.mutate('notif-001')

    await waitFor(() => {
      expect(notificacoesService.descartar).toHaveBeenCalledWith('notif-001')
    })
  })
})
