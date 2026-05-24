import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('sonner', () => ({
  toast: {
    error: vi.fn(),
    warning: vi.fn(),
  },
}))

vi.mock('@/features/orcamentos/services/orcamento-service', () => ({
  orcamentoService: {
    listar: vi.fn(),
    progresso: vi.fn(),
  },
}))

vi.mock('@/features/categorias/services/categorias.service', () => ({
  categoriasService: {
    listar: vi.fn(),
  },
}))

vi.mock('@/features/metas/services/meta-service', () => ({
  metaService: {
    listar: vi.fn(),
  },
}))

import { useNotificacoesToast } from './useNotificacoesToast'
import { toast } from 'sonner'
import { orcamentoService } from '@/features/orcamentos/services/orcamento-service'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { metaService } from '@/features/metas/services/meta-service'
import type { Orcamento, Progresso } from '@/features/orcamentos/types/orcamento'
import type { Categoria } from '@/features/categorias/types/categoria'
import type { Meta } from '@/features/metas/types/meta'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

function pad2(v: number): string {
  return v.toString().padStart(2, '0')
}

function mesAtualYYYYMM(): string {
  const h = new Date()
  return `${h.getFullYear()}-${pad2(h.getMonth() + 1)}`
}

function dataRelativaISO(diasOffset: number): string {
  const h = new Date()
  const d = new Date(h.getFullYear(), h.getMonth(), h.getDate() + diasOffset)
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

const categoriaFixture: Categoria = {
  id: 'cat-001',
  nome: 'Alimentacao',
  tipo: 'DESPESA',
  categoriaPaiId: null,
  system: false,
  criadoEm: '2024-01-01T00:00:00Z',
  atualizadoEm: '2024-01-01T00:00:00Z',
}

const orcamentoFixture = (overrides?: Partial<Orcamento>): Orcamento => ({
  id: 'orc-001',
  categoriaId: 'cat-001',
  valorLimite: { valor: 500, moeda: 'BRL' },
  mesAno: `${mesAtualYYYYMM()}-01`,
  ativo: true,
  criadoEm: '2024-05-01T00:00:00Z',
  atualizadoEm: '2024-05-01T00:00:00Z',
  ...overrides,
})

const progressoFixture = (overrides?: Partial<Progresso>): Progresso => ({
  orcamentoId: 'orc-001',
  categoriaId: 'cat-001',
  mesAno: `${mesAtualYYYYMM()}-01`,
  valorLimite: { valor: 500, moeda: 'BRL' },
  totalGasto: { valor: 600, moeda: 'BRL' },
  percentualUtilizado: 120,
  status: 'EXCEDIDO',
  ...overrides,
})

const metaFixture = (overrides?: Partial<Meta>): Meta => ({
  id: 'meta-001',
  nome: 'Viagem',
  valorAlvo: { valor: 10000, moeda: 'BRL' },
  valorAtual: { valor: 2000, moeda: 'BRL' },
  prazo: dataRelativaISO(3),
  status: 'EM_ANDAMENTO',
  atrasada: false,
  percentualConcluido: 20,
  criadoEm: '2024-01-01T00:00:00Z',
  atualizadoEm: '2024-01-01T00:00:00Z',
  ...overrides,
})

describe('useNotificacoesToast', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(categoriasService.listar).mockResolvedValue([categoriaFixture])
    vi.mocked(orcamentoService.listar).mockResolvedValue([])
    vi.mocked(orcamentoService.progresso).mockResolvedValue(progressoFixture())
    vi.mocked(metaService.listar).mockResolvedValue([])
  })

  it('chama toast.error uma vez para notificacao orcamento_excedido', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([orcamentoFixture()])
    vi.mocked(orcamentoService.progresso).mockResolvedValue(
      progressoFixture({ status: 'EXCEDIDO', percentualUtilizado: 120 }),
    )

    renderHook(() => useNotificacoesToast(), { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(vi.mocked(toast.error)).toHaveBeenCalledTimes(1)
    })

    const call = vi.mocked(toast.error).mock.calls[0]
    expect(call[0]).toBe('Orcamento excedido')
  })

  it('chama toast.warning para notificacao meta_vencendo', async () => {
    vi.mocked(metaService.listar).mockResolvedValue([metaFixture()])

    renderHook(() => useNotificacoesToast(), { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(vi.mocked(toast.warning)).toHaveBeenCalledTimes(1)
    })

    const call = vi.mocked(toast.warning).mock.calls[0]
    expect(call[0]).toBe('Meta vencendo em breve')
  })

  it('chama toast.error para notificacao meta_vencida', async () => {
    vi.mocked(metaService.listar).mockResolvedValue([
      metaFixture({ prazo: dataRelativaISO(-1) }),
    ])

    renderHook(() => useNotificacoesToast(), { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(vi.mocked(toast.error)).toHaveBeenCalledTimes(1)
    })

    expect(vi.mocked(toast.error).mock.calls[0][0]).toBe('Meta vencida')
  })

  it('nao chama toast novamente para a mesma notificacao em rerender (deduplicacao)', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([orcamentoFixture()])
    vi.mocked(orcamentoService.progresso).mockResolvedValue(
      progressoFixture({ status: 'EXCEDIDO', percentualUtilizado: 120 }),
    )

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

    // Aguarda queries resolverem.
    await new Promise((r) => setTimeout(r, 50))

    expect(vi.mocked(toast.error)).not.toHaveBeenCalled()
    expect(vi.mocked(toast.warning)).not.toHaveBeenCalled()
  })
})
