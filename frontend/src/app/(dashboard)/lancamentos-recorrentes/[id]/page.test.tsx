import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@tanstack/react-query', () => ({
  useQuery: vi.fn(),
  useMutation: vi.fn(),
  useQueryClient: vi.fn(),
}))

vi.mock('@/features/lancamentorecorrente', () => ({
  lancamentoRecorrenteService: {
    buscar: vi.fn(),
    desativar: vi.fn(),
    executar: vi.fn(),
  },
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useParams: () => ({ id: 'lr-123' }),
  useRouter: () => ({ push: mockPush }),
}))

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import LancamentoRecorrenteDetalhePage from './page'
import type { LancamentoRecorrente } from '@/features/lancamentorecorrente'

const lancamentoAtivo: LancamentoRecorrente = {
  id: 'lr-123',
  descricao: 'Aluguel',
  tipo: 'DESPESA',
  valor: { valor: 1200, moeda: 'BRL' },
  contaId: 'conta-abc',
  categoriaId: null,
  periodicidade: 'MENSAL',
  proximaOcorrencia: '2026-06-01',
  ativo: true,
  criadoEm: '2026-05-01T00:00:00Z',
  atualizadoEm: '2026-05-01T00:00:00Z',
}

const lancamentoInativo: LancamentoRecorrente = {
  ...lancamentoAtivo,
  ativo: false,
}

function mockQueryLancamento(lancamento: LancamentoRecorrente) {
  vi.mocked(useQuery).mockImplementation(({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === 'lancamento-recorrente') {
      return { data: lancamento, isLoading: false, isError: false } as ReturnType<typeof useQuery>
    }
    return { data: undefined, isLoading: false, isError: false } as ReturnType<typeof useQuery>
  })
}

function mockQueryLoading() {
  vi.mocked(useQuery).mockReturnValue({
    data: undefined,
    isLoading: true,
    isError: false,
  } as ReturnType<typeof useQuery>)
}

function mockQueryError() {
  vi.mocked(useQuery).mockReturnValue({
    data: undefined,
    isLoading: false,
    isError: true,
  } as ReturnType<typeof useQuery>)
}

function mockMutationIdle() {
  vi.mocked(useMutation).mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
  } as unknown as ReturnType<typeof useMutation>)
}

describe('LancamentoRecorrenteDetalhePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(useQueryClient).mockReturnValue({
      invalidateQueries: vi.fn(),
    } as unknown as ReturnType<typeof useQueryClient>)
    mockMutationIdle()
  })

  describe('estado de loading', () => {
    it('exibe skeleton enquanto carrega', () => {
      mockQueryLoading()
      render(<LancamentoRecorrenteDetalhePage />)
      expect(screen.queryByText('Aluguel')).toBeNull()
      const skeletons = document.querySelectorAll('[class*="skeleton"], [data-slot="skeleton"]')
      expect(skeletons.length).toBeGreaterThan(0)
    })
  })

  describe('estado de erro', () => {
    it('exibe mensagem quando lancamento nao encontrado', () => {
      mockQueryError()
      render(<LancamentoRecorrenteDetalhePage />)
      expect(screen.getByText(/lancamento recorrente nao encontrado/i)).toBeTruthy()
    })

    it('botao Voltar no estado de erro navega para /lancamentos-recorrentes', async () => {
      mockQueryError()
      render(<LancamentoRecorrenteDetalhePage />)
      await userEvent.click(screen.getByRole('button', { name: /voltar/i }))
      expect(mockPush).toHaveBeenCalledWith('/lancamentos-recorrentes')
    })
  })

  describe('happy path — lancamento ativo', () => {
    it('exibe descricao do lancamento', () => {
      mockQueryLancamento(lancamentoAtivo)
      render(<LancamentoRecorrenteDetalhePage />)
      expect(screen.getByText('Aluguel')).toBeTruthy()
    })

    it('exibe badge Despesa', () => {
      mockQueryLancamento(lancamentoAtivo)
      render(<LancamentoRecorrenteDetalhePage />)
      expect(screen.getByText('Despesa')).toBeTruthy()
    })

    it('exibe valor formatado em BRL', () => {
      mockQueryLancamento(lancamentoAtivo)
      render(<LancamentoRecorrenteDetalhePage />)
      expect(screen.getByText(/1\.200/)).toBeTruthy()
    })

    it('exibe periodicidade formatada', () => {
      mockQueryLancamento(lancamentoAtivo)
      render(<LancamentoRecorrenteDetalhePage />)
      expect(screen.getByText('Mensal')).toBeTruthy()
    })

    it('exibe proxima ocorrencia formatada', () => {
      mockQueryLancamento(lancamentoAtivo)
      render(<LancamentoRecorrenteDetalhePage />)
      expect(screen.getByText('01/06/2026')).toBeTruthy()
    })

    it('exibe badge Ativo', () => {
      mockQueryLancamento(lancamentoAtivo)
      render(<LancamentoRecorrenteDetalhePage />)
      expect(screen.getByText('Ativo')).toBeTruthy()
    })

    it('exibe botao Desativar para lancamento ativo', () => {
      mockQueryLancamento(lancamentoAtivo)
      render(<LancamentoRecorrenteDetalhePage />)
      expect(screen.getByRole('button', { name: /desativar/i })).toBeTruthy()
    })

    it('exibe botao Executar agora', () => {
      mockQueryLancamento(lancamentoAtivo)
      render(<LancamentoRecorrenteDetalhePage />)
      expect(screen.getByRole('button', { name: /executar agora/i })).toBeTruthy()
    })
  })

  describe('happy path — lancamento inativo', () => {
    it('exibe badge Inativo', () => {
      mockQueryLancamento(lancamentoInativo)
      render(<LancamentoRecorrenteDetalhePage />)
      expect(screen.getByText('Inativo')).toBeTruthy()
    })

    it('nao exibe botao Desativar para lancamento inativo', () => {
      mockQueryLancamento(lancamentoInativo)
      render(<LancamentoRecorrenteDetalhePage />)
      expect(screen.queryByRole('button', { name: /desativar/i })).toBeNull()
    })
  })

  describe('fluxo de desativacao', () => {
    it('exibe confirmacao ao clicar em Desativar', async () => {
      mockQueryLancamento(lancamentoAtivo)
      render(<LancamentoRecorrenteDetalhePage />)
      await userEvent.click(screen.getByRole('button', { name: /desativar/i }))
      expect(screen.getByText(/confirmar desativacao/i)).toBeTruthy()
      expect(screen.getByRole('button', { name: /confirmar/i })).toBeTruthy()
      expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
    })

    it('volta ao estado inicial ao clicar em Cancelar', async () => {
      mockQueryLancamento(lancamentoAtivo)
      render(<LancamentoRecorrenteDetalhePage />)
      await userEvent.click(screen.getByRole('button', { name: /desativar/i }))
      await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))
      expect(screen.queryByText(/confirmar desativacao/i)).toBeNull()
      expect(screen.getByRole('button', { name: /desativar/i })).toBeTruthy()
    })

    it('chama mutate ao confirmar desativacao', async () => {
      const mockMutate = vi.fn()
      vi.mocked(useMutation).mockReturnValue({
        mutate: mockMutate,
        isPending: false,
      } as unknown as ReturnType<typeof useMutation>)

      mockQueryLancamento(lancamentoAtivo)
      render(<LancamentoRecorrenteDetalhePage />)
      await userEvent.click(screen.getByRole('button', { name: /desativar/i }))
      await userEvent.click(screen.getByRole('button', { name: /confirmar/i }))
      expect(mockMutate).toHaveBeenCalled()
    })
  })

  describe('fluxo de execucao', () => {
    it('chama mutate ao clicar em Executar agora', async () => {
      const mockMutate = vi.fn()
      vi.mocked(useMutation).mockReturnValue({
        mutate: mockMutate,
        isPending: false,
      } as unknown as ReturnType<typeof useMutation>)

      mockQueryLancamento(lancamentoAtivo)
      render(<LancamentoRecorrenteDetalhePage />)
      await userEvent.click(screen.getByRole('button', { name: /executar agora/i }))
      expect(mockMutate).toHaveBeenCalled()
    })
  })
})
