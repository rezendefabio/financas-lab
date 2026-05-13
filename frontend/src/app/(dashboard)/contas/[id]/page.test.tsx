import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@tanstack/react-query', () => ({
  useQuery: vi.fn(),
  useMutation: vi.fn(),
  useQueryClient: vi.fn(),
}))

vi.mock('@/services/contas.service', () => ({
  contasService: {
    buscarPorId: vi.fn(),
    calcularSaldo: vi.fn(),
    desativar: vi.fn(),
  },
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useParams: () => ({ id: 'conta-123' }),
  useRouter: () => ({ push: mockPush }),
}))

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import ContaDetalhePage from './page'
import type { Conta, SaldoResponse } from '@/types/conta'

const contaAtiva: Conta = {
  id: 'conta-123',
  nome: 'Nubank',
  tipo: 'CORRENTE',
  saldoInicialValor: 1000,
  saldoInicialMoeda: 'BRL',
  ativa: true,
  criadoEm: '2024-01-01T00:00:00Z',
  atualizadoEm: '2024-01-01T00:00:00Z',
}

const contaInativa: Conta = {
  ...contaAtiva,
  ativa: false,
}

const saldoResponse: SaldoResponse = {
  contaId: 'conta-123',
  saldoInicial: { valor: 1000, moeda: 'BRL' },
  totalReceitas: { valor: 500, moeda: 'BRL' },
  totalDespesas: { valor: 200, moeda: 'BRL' },
  totalTransferenciasEnviadas: { valor: 0, moeda: 'BRL' },
  totalTransferenciasRecebidas: { valor: 0, moeda: 'BRL' },
  saldoAtual: { valor: 1300, moeda: 'BRL' },
  calculadoEm: '2024-01-01T00:00:00Z',
}

/**
 * Discrimina pelo queryKey[0] para retornar o resultado correto
 * independente de quantas vezes useQuery for chamado por re-renders.
 */
function mockQueryConta(conta: Conta, saldo?: SaldoResponse) {
  vi.mocked(useQuery).mockImplementation(({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === 'conta') {
      return { data: conta, isLoading: false, isError: false } as ReturnType<typeof useQuery>
    }
    return { data: saldo, isLoading: false, isError: false } as ReturnType<typeof useQuery>
  })
}

function mockQueryLoading() {
  vi.mocked(useQuery).mockImplementation(({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === 'conta') {
      return { data: undefined, isLoading: true, isError: false } as ReturnType<typeof useQuery>
    }
    return { data: undefined, isLoading: false, isError: false } as ReturnType<typeof useQuery>
  })
}

function mockQueryError() {
  vi.mocked(useQuery).mockImplementation(({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === 'conta') {
      return { data: undefined, isLoading: false, isError: true } as ReturnType<typeof useQuery>
    }
    return { data: undefined, isLoading: false, isError: false } as ReturnType<typeof useQuery>
  })
}

function mockQuerySaldoLoading(conta: Conta) {
  vi.mocked(useQuery).mockImplementation(({ queryKey }: { queryKey: string[] }) => {
    if (queryKey[0] === 'conta') {
      return { data: conta, isLoading: false, isError: false } as ReturnType<typeof useQuery>
    }
    return { data: undefined, isLoading: true, isError: false } as ReturnType<typeof useQuery>
  })
}

function mockMutationIdle() {
  vi.mocked(useMutation).mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
  } as unknown as ReturnType<typeof useMutation>)
}

describe('ContaDetalhePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(useQueryClient).mockReturnValue({
      invalidateQueries: vi.fn(),
    } as unknown as ReturnType<typeof useQueryClient>)
    mockMutationIdle()
  })

  describe('estado de loading', () => {
    it('nao exibe conteudo da conta enquanto carrega', () => {
      mockQueryLoading()
      render(<ContaDetalhePage />)
      expect(screen.queryByText('Nubank')).toBeNull()
      expect(screen.queryByText('Conta nao encontrada.')).toBeNull()
    })
  })

  describe('estado de erro', () => {
    it('exibe mensagem de erro quando conta nao encontrada', () => {
      mockQueryError()
      render(<ContaDetalhePage />)
      expect(screen.getByText('Conta nao encontrada.')).toBeTruthy()
    })

    it('exibe botao Voltar no estado de erro que navega para /contas', async () => {
      mockQueryError()
      render(<ContaDetalhePage />)
      await userEvent.click(screen.getByRole('button', { name: /voltar/i }))
      expect(mockPush).toHaveBeenCalledWith('/contas')
    })
  })

  describe('happy path — conta ativa', () => {
    it('exibe nome da conta', () => {
      mockQueryConta(contaAtiva, saldoResponse)
      render(<ContaDetalhePage />)
      expect(screen.getByText('Nubank')).toBeTruthy()
    })

    it('exibe tipo formatado', () => {
      mockQueryConta(contaAtiva, saldoResponse)
      render(<ContaDetalhePage />)
      expect(screen.getByText('Corrente')).toBeTruthy()
    })

    it('exibe badge Ativa para conta ativa', () => {
      mockQueryConta(contaAtiva, saldoResponse)
      render(<ContaDetalhePage />)
      expect(screen.getByText('Ativa')).toBeTruthy()
    })

    it('exibe saldo inicial formatado em BRL', () => {
      mockQueryConta(contaAtiva, saldoResponse)
      render(<ContaDetalhePage />)
      expect(screen.getByText(/1\.000/)).toBeTruthy()
    })

    it('exibe saldo atual quando saldo carregado', () => {
      mockQueryConta(contaAtiva, saldoResponse)
      render(<ContaDetalhePage />)
      expect(screen.getByText(/1\.300/)).toBeTruthy()
    })

    it('exibe botao Voltar que navega para /contas', async () => {
      mockQueryConta(contaAtiva, saldoResponse)
      render(<ContaDetalhePage />)
      const botoes = screen.getAllByRole('button', { name: /voltar/i })
      await userEvent.click(botoes[0])
      expect(mockPush).toHaveBeenCalledWith('/contas')
    })

    it('exibe botao Desativar conta para conta ativa', () => {
      mockQueryConta(contaAtiva, saldoResponse)
      render(<ContaDetalhePage />)
      expect(screen.getByRole('button', { name: /desativar conta/i })).toBeTruthy()
    })
  })

  describe('happy path — conta inativa', () => {
    it('exibe badge Inativa para conta inativa', () => {
      mockQueryConta(contaInativa, saldoResponse)
      render(<ContaDetalhePage />)
      expect(screen.getByText('Inativa')).toBeTruthy()
    })

    it('nao exibe botao Desativar para conta inativa', () => {
      mockQueryConta(contaInativa, saldoResponse)
      render(<ContaDetalhePage />)
      expect(screen.queryByRole('button', { name: /desativar conta/i })).toBeNull()
    })
  })

  describe('fluxo de desativacao', () => {
    it('exibe confirmacao ao clicar em Desativar conta', async () => {
      mockQueryConta(contaAtiva, saldoResponse)
      render(<ContaDetalhePage />)
      await userEvent.click(screen.getByRole('button', { name: /desativar conta/i }))
      expect(screen.getByText(/confirmar desativacao/i)).toBeTruthy()
      expect(screen.getByRole('button', { name: /confirmar/i })).toBeTruthy()
      expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
    })

    it('volta ao estado inicial ao clicar em Cancelar', async () => {
      mockQueryConta(contaAtiva, saldoResponse)
      render(<ContaDetalhePage />)
      await userEvent.click(screen.getByRole('button', { name: /desativar conta/i }))
      await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))
      expect(screen.getByRole('button', { name: /desativar conta/i })).toBeTruthy()
      expect(screen.queryByText(/confirmar desativacao/i)).toBeNull()
    })

    it('chama mutate ao confirmar desativacao', async () => {
      const mockMutate = vi.fn()
      vi.mocked(useMutation).mockReturnValue({
        mutate: mockMutate,
        isPending: false,
      } as unknown as ReturnType<typeof useMutation>)

      mockQueryConta(contaAtiva, saldoResponse)
      render(<ContaDetalhePage />)
      await userEvent.click(screen.getByRole('button', { name: /desativar conta/i }))
      await userEvent.click(screen.getByRole('button', { name: /confirmar/i }))
      expect(mockMutate).toHaveBeenCalledOnce()
    })

    it('exibe texto Desativando... quando mutation esta pendente', async () => {
      const mockMutate = vi.fn()
      vi.mocked(useMutation).mockReturnValue({
        mutate: mockMutate,
        isPending: true,
      } as unknown as ReturnType<typeof useMutation>)

      mockQueryConta(contaAtiva, saldoResponse)
      render(<ContaDetalhePage />)
      // Com confirmando=false e isPending=true, o botao "Desativar conta" aparece normalmente.
      // Para testar o texto "Desativando...", precisamos entrar no estado confirmando=true.
      // Clicamos em "Desativar conta" para setar confirmando=true; como isPending ja e true,
      // o botao confirmar deve exibir "Desativando..." imediatamente.
      await userEvent.click(screen.getByRole('button', { name: /desativar conta/i }))
      await waitFor(() => {
        expect(screen.getByText(/desativando\.\.\./i)).toBeTruthy()
      })
    })
  })

  describe('saldo em loading', () => {
    it('nao exibe saldo atual quando saldo ainda carregando', () => {
      mockQuerySaldoLoading(contaAtiva)
      render(<ContaDetalhePage />)
      expect(screen.queryByText(/1\.300/)).toBeNull()
    })
  })
})
