import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('../services/orcamento-service', () => ({
  orcamentoService: {
    listar: vi.fn(),
    progresso: vi.fn(),
  },
}))

import { useOrcamentosAtivosComProgresso } from './useOrcamentosAtivosComProgresso'
import { orcamentoService } from '../services/orcamento-service'
import type { Orcamento, Progresso } from '../types/orcamento'

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
  totalGasto: { valor: 100, moeda: 'BRL' },
  percentualUtilizado: 20,
  status: 'ABAIXO',
  ...overrides,
})

describe('useOrcamentosAtivosComProgresso', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(orcamentoService.listar).mockResolvedValue([])
    vi.mocked(orcamentoService.progresso).mockResolvedValue(progressoFixture())
  })

  it('retorna lista vazia quando nao ha orcamentos', async () => {
    const { result } = renderHook(() => useOrcamentosAtivosComProgresso(), {
      wrapper: makeWrapper(),
    })
    await waitFor(() => expect(result.current.isLoading).toBe(false))
    expect(result.current.itens).toEqual([])
  })

  it('filtra orcamentos inativos', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([
      orcamentoFixture({ ativo: false }),
    ])
    const { result } = renderHook(() => useOrcamentosAtivosComProgresso(), {
      wrapper: makeWrapper(),
    })
    await waitFor(() => expect(result.current.isLoading).toBe(false))
    expect(result.current.itens).toEqual([])
  })

  it('filtra orcamentos com mesAno diferente do mes atual', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([
      orcamentoFixture({ mesAno: '2020-01-01' }),
    ])
    const { result } = renderHook(() => useOrcamentosAtivosComProgresso(), {
      wrapper: makeWrapper(),
    })
    await waitFor(() => expect(result.current.isLoading).toBe(false))
    expect(result.current.itens).toEqual([])
  })

  it('retorna orcamento ativo do mes com progresso', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([orcamentoFixture()])
    vi.mocked(orcamentoService.progresso).mockResolvedValue(
      progressoFixture({ status: 'EXCEDIDO' }),
    )
    const { result } = renderHook(() => useOrcamentosAtivosComProgresso(), {
      wrapper: makeWrapper(),
    })
    await waitFor(() => expect(result.current.itens).toHaveLength(1))
    expect(result.current.itens[0].orcamento.id).toBe('orc-001')
    expect(result.current.itens[0].progresso?.status).toBe('EXCEDIDO')
  })
})
