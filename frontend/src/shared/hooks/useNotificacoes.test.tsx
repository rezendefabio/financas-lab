import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

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

import { useNotificacoes } from './useNotificacoes'
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

const categoriaFixture = (overrides?: Partial<Categoria>): Categoria => ({
  id: 'cat-001',
  nome: 'Alimentacao',
  tipo: 'DESPESA',
  categoriaPaiId: null,
  system: false,
  criadoEm: '2024-01-01T00:00:00Z',
  atualizadoEm: '2024-01-01T00:00:00Z',
  ...overrides,
})

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

const metaFixture = (overrides?: Partial<Meta>): Meta => ({
  id: 'meta-001',
  nome: 'Viagem',
  valorAlvo: { valor: 10000, moeda: 'BRL' },
  valorAtual: { valor: 2000, moeda: 'BRL' },
  prazo: dataRelativaISO(30),
  status: 'EM_ANDAMENTO',
  atrasada: false,
  percentualConcluido: 20,
  criadoEm: '2024-01-01T00:00:00Z',
  atualizadoEm: '2024-01-01T00:00:00Z',
  ...overrides,
})

describe('useNotificacoes', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(categoriasService.listar).mockResolvedValue([categoriaFixture()])
    vi.mocked(orcamentoService.listar).mockResolvedValue([])
    vi.mocked(orcamentoService.progresso).mockResolvedValue(progressoFixture())
    vi.mocked(metaService.listar).mockResolvedValue([])
  })

  it('retorna lista vazia quando nao ha orcamentos ativos no mes nem metas', async () => {
    const { result } = renderHook(() => useNotificacoes(), {
      wrapper: makeWrapper(),
    })

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.notificacoes).toEqual([])
  })

  it('ignora orcamento com mesAno diferente do mes atual', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([
      orcamentoFixture({ mesAno: '2020-01-01' }),
    ])
    vi.mocked(orcamentoService.progresso).mockResolvedValue(
      progressoFixture({ status: 'EXCEDIDO', percentualUtilizado: 150 }),
    )

    const { result } = renderHook(() => useNotificacoes(), {
      wrapper: makeWrapper(),
    })

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.notificacoes).toEqual([])
  })

  it('emite notificacao orcamento_excedido para status EXCEDIDO', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([orcamentoFixture()])
    vi.mocked(orcamentoService.progresso).mockResolvedValue(
      progressoFixture({ status: 'EXCEDIDO', percentualUtilizado: 110 }),
    )

    const { result } = renderHook(() => useNotificacoes(), {
      wrapper: makeWrapper(),
    })

    await waitFor(() => {
      expect(result.current.notificacoes).toHaveLength(1)
    })

    const n = result.current.notificacoes[0]
    expect(n.tipo).toBe('orcamento_excedido')
    expect(n.titulo).toBe('Orcamento excedido')
    expect(n.descricao).toContain('Alimentacao')
    expect(n.descricao).toContain('110%')
  })

  it('emite notificacao orcamento_atencao para status ATENCAO', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([orcamentoFixture()])
    vi.mocked(orcamentoService.progresso).mockResolvedValue(
      progressoFixture({ status: 'ATENCAO', percentualUtilizado: 85 }),
    )

    const { result } = renderHook(() => useNotificacoes(), {
      wrapper: makeWrapper(),
    })

    await waitFor(() => {
      expect(result.current.notificacoes).toHaveLength(1)
    })

    expect(result.current.notificacoes[0].tipo).toBe('orcamento_atencao')
  })

  it('nao emite notificacao para orcamento status ABAIXO', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([orcamentoFixture()])
    vi.mocked(orcamentoService.progresso).mockResolvedValue(
      progressoFixture({ status: 'ABAIXO' }),
    )

    const { result } = renderHook(() => useNotificacoes(), {
      wrapper: makeWrapper(),
    })

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.notificacoes).toEqual([])
  })

  it('usa fallback Orcamento quando categoria nao e encontrada', async () => {
    vi.mocked(orcamentoService.listar).mockResolvedValue([
      orcamentoFixture({ categoriaId: 'cat-inexistente' }),
    ])
    vi.mocked(orcamentoService.progresso).mockResolvedValue(
      progressoFixture({ status: 'EXCEDIDO' }),
    )

    const { result } = renderHook(() => useNotificacoes(), {
      wrapper: makeWrapper(),
    })

    await waitFor(() => {
      expect(result.current.notificacoes).toHaveLength(1)
    })

    expect(result.current.notificacoes[0].descricao).toContain('Orcamento')
  })

  it('emite notificacao meta_vencendo quando prazo em 3 dias', async () => {
    vi.mocked(metaService.listar).mockResolvedValue([
      metaFixture({ prazo: dataRelativaISO(3) }),
    ])

    const { result } = renderHook(() => useNotificacoes(), {
      wrapper: makeWrapper(),
    })

    await waitFor(() => {
      expect(result.current.notificacoes).toHaveLength(1)
    })

    const n = result.current.notificacoes[0]
    expect(n.tipo).toBe('meta_vencendo')
    expect(n.titulo).toBe('Meta vencendo em breve')
    expect(n.descricao).toContain('Viagem')
    expect(n.descricao).toContain('3 dias')
  })

  it('emite notificacao meta_vencida quando prazo ja passou', async () => {
    vi.mocked(metaService.listar).mockResolvedValue([
      metaFixture({ prazo: dataRelativaISO(-1) }),
    ])

    const { result } = renderHook(() => useNotificacoes(), {
      wrapper: makeWrapper(),
    })

    await waitFor(() => {
      expect(result.current.notificacoes).toHaveLength(1)
    })

    const n = result.current.notificacoes[0]
    expect(n.tipo).toBe('meta_vencida')
    expect(n.titulo).toBe('Meta vencida')
    expect(n.descricao).toContain('Viagem')
  })

  it('nao emite notificacao para meta CONCLUIDA mesmo com prazo proximo', async () => {
    vi.mocked(metaService.listar).mockResolvedValue([
      metaFixture({ prazo: dataRelativaISO(2), status: 'CONCLUIDA' }),
    ])

    const { result } = renderHook(() => useNotificacoes(), {
      wrapper: makeWrapper(),
    })

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.notificacoes).toEqual([])
  })

  it('nao emite notificacao para meta com prazo distante (> 7 dias)', async () => {
    vi.mocked(metaService.listar).mockResolvedValue([
      metaFixture({ prazo: dataRelativaISO(30) }),
    ])

    const { result } = renderHook(() => useNotificacoes(), {
      wrapper: makeWrapper(),
    })

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false)
    })

    expect(result.current.notificacoes).toEqual([])
  })

  it('usa singular "1 dia" quando prazo e amanha', async () => {
    vi.mocked(metaService.listar).mockResolvedValue([
      metaFixture({ prazo: dataRelativaISO(1) }),
    ])

    const { result } = renderHook(() => useNotificacoes(), {
      wrapper: makeWrapper(),
    })

    await waitFor(() => {
      expect(result.current.notificacoes).toHaveLength(1)
    })

    expect(result.current.notificacoes[0].descricao).toContain('1 dia')
    expect(result.current.notificacoes[0].descricao).not.toContain('1 dias')
  })
})
