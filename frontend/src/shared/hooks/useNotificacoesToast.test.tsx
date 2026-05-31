import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { Notificacao } from '@/features/notificacoes/types/notificacao'

vi.mock('sonner', () => ({
  toast: {
    error: vi.fn(),
    warning: vi.fn(),
  },
}))

vi.mock('@/features/notificacoes/services/notificacao-service', () => ({
  notificacoesService: {
    listar: vi.fn(),
    descartar: vi.fn(),
  },
}))

import { useNotificacoesToast } from './useNotificacoesToast'
import { toast } from 'sonner'
import { notificacoesService } from '@/features/notificacoes/services/notificacao-service'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const notificacao = (overrides?: Partial<Notificacao>): Notificacao => ({
  id: 'notif-001',
  tipo: 'ORCAMENTO_EXCEDIDO',
  referenciaId: 'orc-001',
  titulo: 'Orcamento excedido',
  descricao: 'Alimentacao: 120% utilizado',
  criadoEm: '2024-05-01T00:00:00Z',
  ...overrides,
})

describe('useNotificacoesToast', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(notificacoesService.listar).mockResolvedValue([])
  })

  it('chama toast.error uma vez para notificacao ORCAMENTO_EXCEDIDO', async () => {
    vi.mocked(notificacoesService.listar).mockResolvedValue([notificacao()])

    renderHook(() => useNotificacoesToast(), { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(vi.mocked(toast.error)).toHaveBeenCalledTimes(1)
    })
    expect(vi.mocked(toast.error).mock.calls[0][0]).toBe('Orcamento excedido')
  })

  it('chama toast.warning para notificacao META_VENCENDO', async () => {
    vi.mocked(notificacoesService.listar).mockResolvedValue([
      notificacao({
        id: 'notif-002',
        tipo: 'META_VENCENDO',
        titulo: 'Meta vencendo em breve',
        descricao: 'Viagem: vence em 3 dias',
      }),
    ])

    renderHook(() => useNotificacoesToast(), { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(vi.mocked(toast.warning)).toHaveBeenCalledTimes(1)
    })
    expect(vi.mocked(toast.warning).mock.calls[0][0]).toBe('Meta vencendo em breve')
  })

  it('chama toast.error para notificacao META_VENCIDA', async () => {
    vi.mocked(notificacoesService.listar).mockResolvedValue([
      notificacao({
        id: 'notif-003',
        tipo: 'META_VENCIDA',
        titulo: 'Meta vencida',
        descricao: 'Viagem: prazo encerrado',
      }),
    ])

    renderHook(() => useNotificacoesToast(), { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(vi.mocked(toast.error)).toHaveBeenCalledTimes(1)
    })
    expect(vi.mocked(toast.error).mock.calls[0][0]).toBe('Meta vencida')
  })

  it('nao chama toast novamente para a mesma notificacao em rerender (deduplicacao)', async () => {
    vi.mocked(notificacoesService.listar).mockResolvedValue([notificacao()])

    const { rerender } = renderHook(() => useNotificacoesToast(), {
      wrapper: makeWrapper(),
    })

    await waitFor(() => {
      expect(vi.mocked(toast.error)).toHaveBeenCalledTimes(1)
    })

    rerender()
    rerender()

    expect(vi.mocked(toast.error)).toHaveBeenCalledTimes(1)
  })

  it('nao chama toast quando nao ha notificacoes', async () => {
    renderHook(() => useNotificacoesToast(), { wrapper: makeWrapper() })

    await new Promise((r) => setTimeout(r, 50))

    expect(vi.mocked(toast.error)).not.toHaveBeenCalled()
    expect(vi.mocked(toast.warning)).not.toHaveBeenCalled()
  })
})
