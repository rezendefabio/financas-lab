import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@tanstack/react-query', () => ({
  useQuery: vi.fn(),
  useMutation: vi.fn(),
  useQueryClient: vi.fn(),
}))

vi.mock('@/features/orcamentos/services/orcamento-service', () => ({
  orcamentoService: {
    buscar: vi.fn(),
    progresso: vi.fn(),
    desativar: vi.fn(),
  },
}))

vi.mock('@/features/categorias/services/categorias.service', () => ({
  categoriasService: {
    listar: vi.fn(),
  },
}))

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useParams: () => ({ id: 'orc-001' }),
  useRouter: () => ({ push: mockPush, back: mockBack }),
}))

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import OrcamentoDetalhePage from './page'
import type { Orcamento, Progresso } from '@/features/orcamentos/types/orcamento'

const orcamentoAtivo: Orcamento = {
  id: 'orc-001',
  categoriaId: 'cat-001',
  valorLimite: { valor: 500, moeda: 'BRL' },
  mesAno: '2024-05-01',
  ativo: true,
  criadoEm: '2024-05-01T00:00:00Z',
  atualizadoEm: '2024-05-01T00:00:00Z',
}

const orcamentoInativo: Orcamento = {
  ...orcamentoAtivo,
  ativo: false,
}

const progressoFixture: Progresso = {
  orcamentoId: 'orc-001',
  categoriaId: 'cat-001',
  mesAno: '2024-05-01',
  valorLimite: { valor: 500, moeda: 'BRL' },
  totalGasto: { valor: 200, moeda: 'BRL' },
  percentualUtilizado: 40,
  status: 'ABAIXO',
}

function mockQueryOrcamento(orcamento: Orcamento, progresso?: Progresso) {
  vi.mocked(useQuery).mockImplementation(({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === 'orcamento') {
      return { data: orcamento, isLoading: false, isError: false } as ReturnType<typeof useQuery>
    }
    if (queryKey[0] === 'orcamento-progresso') {
      return { data: progresso, isLoading: false, isError: false } as ReturnType<typeof useQuery>
    }
    // categorias
    return { data: [], isLoading: false, isError: false } as ReturnType<typeof useQuery>
  })
}

function mockQueryLoading() {
  vi.mocked(useQuery).mockImplementation(({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === 'orcamento') {
      return { data: undefined, isLoading: true, isError: false } as ReturnType<typeof useQuery>
    }
    return { data: undefined, isLoading: false, isError: false } as ReturnType<typeof useQuery>
  })
}

function mockQueryError() {
  vi.mocked(useQuery).mockImplementation(({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === 'orcamento') {
      return { data: undefined, isLoading: false, isError: true } as ReturnType<typeof useQuery>
    }
    return { data: undefined, isLoading: false, isError: false } as ReturnType<typeof useQuery>
  })
}

function mockMutationIdle() {
  vi.mocked(useMutation).mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
  } as unknown as ReturnType<typeof useMutation>)
}

describe('OrcamentoDetalhePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(useQueryClient).mockReturnValue({
      invalidateQueries: vi.fn(),
    } as unknown as ReturnType<typeof useQueryClient>)
    mockMutationIdle()
  })

  describe('estado de loading', () => {
    it('exibe skeleton durante carregamento', () => {
      mockQueryLoading()
      render(<OrcamentoDetalhePage />)

      const skeletons = document.querySelectorAll('[data-slot="skeleton"]')
      expect(skeletons.length).toBeGreaterThan(0)
    })

    it('nao exibe conteudo do orcamento durante loading', () => {
      mockQueryLoading()
      render(<OrcamentoDetalhePage />)

      expect(screen.queryByText('Detalhe do Orcamento')).toBeNull()
    })
  })

  describe('estado de erro', () => {
    it('exibe mensagem de erro quando orcamento nao encontrado', () => {
      mockQueryError()
      render(<OrcamentoDetalhePage />)

      expect(screen.getByText(/erro ao carregar orcamento/i)).toBeTruthy()
    })
  })

  describe('happy path — orcamento ativo', () => {
    it('exibe titulo da pagina', () => {
      mockQueryOrcamento(orcamentoAtivo)
      render(<OrcamentoDetalhePage />)

      expect(screen.getByText('Detalhe do Orcamento')).toBeTruthy()
    })

    it('exibe limite formatado em BRL', () => {
      mockQueryOrcamento(orcamentoAtivo)
      render(<OrcamentoDetalhePage />)

      expect(screen.getByText(/500/)).toBeTruthy()
    })

    it('exibe mes/ano formatado', () => {
      mockQueryOrcamento(orcamentoAtivo)
      render(<OrcamentoDetalhePage />)

      expect(screen.getByText('05/2024')).toBeTruthy()
    })

    it('exibe badge Ativo para orcamento ativo', () => {
      mockQueryOrcamento(orcamentoAtivo)
      render(<OrcamentoDetalhePage />)

      expect(screen.getByText('Ativo')).toBeTruthy()
    })

    it('exibe botao Desativar para orcamento ativo', () => {
      mockQueryOrcamento(orcamentoAtivo)
      render(<OrcamentoDetalhePage />)

      expect(screen.getByRole('button', { name: /desativar/i })).toBeTruthy()
    })
  })

  describe('happy path — orcamento inativo', () => {
    it('exibe badge Inativo para orcamento inativo', () => {
      mockQueryOrcamento(orcamentoInativo)
      render(<OrcamentoDetalhePage />)

      expect(screen.getByText('Inativo')).toBeTruthy()
    })

    it('nao exibe botao Desativar para orcamento inativo', () => {
      mockQueryOrcamento(orcamentoInativo)
      render(<OrcamentoDetalhePage />)

      expect(screen.queryByRole('button', { name: /desativar/i })).toBeNull()
    })
  })

  describe('secao de progresso', () => {
    it('exibe secao Progresso do Mes quando ha dados de progresso', () => {
      mockQueryOrcamento(orcamentoAtivo, progressoFixture)
      render(<OrcamentoDetalhePage />)

      expect(screen.getByText('Progresso do Mes')).toBeTruthy()
    })

    it('exibe total gasto formatado em BRL', () => {
      mockQueryOrcamento(orcamentoAtivo, progressoFixture)
      render(<OrcamentoDetalhePage />)

      expect(screen.getByText(/200/)).toBeTruthy()
    })

    it('exibe percentual utilizado', () => {
      mockQueryOrcamento(orcamentoAtivo, progressoFixture)
      render(<OrcamentoDetalhePage />)

      expect(screen.getByText('40.0%')).toBeTruthy()
    })

    it('exibe badge de status do progresso', () => {
      mockQueryOrcamento(orcamentoAtivo, progressoFixture)
      render(<OrcamentoDetalhePage />)

      expect(screen.getByText('Abaixo')).toBeTruthy()
    })

    it('exibe barra de progresso com role progressbar', () => {
      mockQueryOrcamento(orcamentoAtivo, progressoFixture)
      render(<OrcamentoDetalhePage />)

      expect(screen.getByRole('progressbar')).toBeTruthy()
    })
  })

  describe('navegacao', () => {
    it('navega para /orcamentos ao clicar em Voltar', async () => {
      mockQueryOrcamento(orcamentoAtivo)
      render(<OrcamentoDetalhePage />)

      const botoesVoltar = screen.getAllByRole('button', { name: /voltar/i })
      await userEvent.click(botoesVoltar[0])

      expect(mockPush).toHaveBeenCalledWith('/orcamentos')
    })

    it('chama mutate ao clicar em Desativar', async () => {
      const mockMutate = vi.fn()
      vi.mocked(useMutation).mockReturnValue({
        mutate: mockMutate,
        isPending: false,
      } as unknown as ReturnType<typeof useMutation>)

      mockQueryOrcamento(orcamentoAtivo)
      render(<OrcamentoDetalhePage />)

      await userEvent.click(screen.getByRole('button', { name: /desativar/i }))

      expect(mockMutate).toHaveBeenCalledOnce()
    })
  })
})
