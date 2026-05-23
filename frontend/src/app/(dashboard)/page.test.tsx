import { render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/contas/services/contas.service', () => ({
  contasService: {
    saldoTotal: vi.fn(),
  },
}))

vi.mock('@/features/dashboard', () => ({
  getFluxoCaixa: vi.fn(),
  getGastosMesAtual: vi.fn(),
  getEvolucaoUltimosSeisMeses: vi.fn(),
}))

vi.mock('@/features/dashboard/components/OrcamentosProgressoCard', () => ({
  OrcamentosProgressoCard: () => <div data-testid="orcamentos-progresso-card" />,
}))

vi.mock('@/features/relatorios', () => ({
  GastosPorCategoriaChart: ({ data }: { data: unknown }) => (
    <div data-testid="gastos-categoria-chart">{JSON.stringify(data)}</div>
  ),
  EvolucaoSaldoChart: ({ data }: { data: unknown }) => (
    <div data-testid="evolucao-saldo-chart">{JSON.stringify(data)}</div>
  ),
}))

import DashboardPage from './page'
import { contasService } from '@/features/contas/services/contas.service'
import {
  getFluxoCaixa,
  getGastosMesAtual,
  getEvolucaoUltimosSeisMeses,
} from '@/features/dashboard'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const pendingPromise = <T,>(): Promise<T> => new Promise<T>(() => {})

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(getGastosMesAtual).mockResolvedValue({
      dataInicio: '2026-05-01',
      dataFim: '2026-05-23',
      totalGeral: { valor: 0, moeda: 'BRL' },
      itensPorCategoria: [],
    })
    vi.mocked(getEvolucaoUltimosSeisMeses).mockResolvedValue({
      dataInicio: '2025-12-01',
      dataFim: '2026-05-23',
      totalReceitas: { valor: 0, moeda: 'BRL' },
      totalDespesas: { valor: 0, moeda: 'BRL' },
      saldoLiquido: { valor: 0, moeda: 'BRL' },
      evolucaoPorMes: [],
    })
  })

  it('renderiza titulo Dashboard e o card de Orcamentos', () => {
    vi.mocked(contasService.saldoTotal).mockReturnValue(pendingPromise())
    vi.mocked(getFluxoCaixa).mockReturnValue(pendingPromise())

    render(<DashboardPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /dashboard/i })).toBeInTheDocument()
    expect(screen.getByTestId('orcamentos-progresso-card')).toBeInTheDocument()
  })

  it('exibe skeleton de loading enquanto saldo total carrega', () => {
    vi.mocked(contasService.saldoTotal).mockReturnValue(pendingPromise())
    vi.mocked(getFluxoCaixa).mockReturnValue(pendingPromise())

    const { container } = render(<DashboardPage />, { wrapper: makeWrapper() })

    expect(container.querySelectorAll('.animate-pulse').length).toBeGreaterThan(0)
  })

  it('exibe valor do saldo total e contagem de contas apos carregamento', async () => {
    vi.mocked(contasService.saldoTotal).mockResolvedValue({
      valor: 1234.56,
      moeda: 'BRL',
      totalContas: 3,
    })
    vi.mocked(getFluxoCaixa).mockReturnValue(pendingPromise())

    render(<DashboardPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/1\.234,56/)).toBeInTheDocument()
    })
    expect(screen.getByText(/3 contas ativas/)).toBeInTheDocument()
  })

  it('exibe singular "conta ativa" quando totalContas eh 1', async () => {
    vi.mocked(contasService.saldoTotal).mockResolvedValue({
      valor: 100,
      moeda: 'BRL',
      totalContas: 1,
    })
    vi.mocked(getFluxoCaixa).mockReturnValue(pendingPromise())

    render(<DashboardPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/1 conta ativa/)).toBeInTheDocument()
    })
  })

  it('exibe mensagem de erro quando saldo total falha', async () => {
    vi.mocked(contasService.saldoTotal).mockRejectedValue(new Error('boom'))
    vi.mocked(getFluxoCaixa).mockReturnValue(pendingPromise())

    render(<DashboardPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar saldo/i)).toBeInTheDocument()
    })
  })

  it('exibe receitas e despesas do mes formatadas em BRL', async () => {
    vi.mocked(contasService.saldoTotal).mockReturnValue(pendingPromise())
    vi.mocked(getFluxoCaixa).mockResolvedValue({
      ano: 2026,
      mes: 5,
      totalReceitas: 5000,
      totalDespesas: 1500,
      saldo: 3500,
      moeda: 'BRL',
    })

    render(<DashboardPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/5\.000,00/)).toBeInTheDocument()
    })
    expect(screen.getByText(/1\.500,00/)).toBeInTheDocument()
  })

  it('renderiza GastosPorCategoriaChart quando dados de gastos chegam', async () => {
    vi.mocked(contasService.saldoTotal).mockReturnValue(pendingPromise())
    vi.mocked(getFluxoCaixa).mockReturnValue(pendingPromise())
    vi.mocked(getGastosMesAtual).mockResolvedValue({
      dataInicio: '2026-05-01',
      dataFim: '2026-05-23',
      totalGeral: { valor: 200, moeda: 'BRL' },
      itensPorCategoria: [
        { categoriaId: 'c1', nomeCategoria: 'Mercado', totalGasto: { valor: 200, moeda: 'BRL' } },
      ],
    })

    render(<DashboardPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByTestId('gastos-categoria-chart')).toBeInTheDocument()
    })
  })

  it('renderiza EvolucaoSaldoChart quando dados de evolucao chegam', async () => {
    vi.mocked(contasService.saldoTotal).mockReturnValue(pendingPromise())
    vi.mocked(getFluxoCaixa).mockReturnValue(pendingPromise())
    vi.mocked(getEvolucaoUltimosSeisMeses).mockResolvedValue({
      dataInicio: '2025-12-01',
      dataFim: '2026-05-23',
      totalReceitas: { valor: 100, moeda: 'BRL' },
      totalDespesas: { valor: 50, moeda: 'BRL' },
      saldoLiquido: { valor: 50, moeda: 'BRL' },
      evolucaoPorMes: [
        {
          mes: '2026-05',
          totalReceitas: { valor: 100, moeda: 'BRL' },
          totalDespesas: { valor: 50, moeda: 'BRL' },
          saldoLiquido: { valor: 50, moeda: 'BRL' },
        },
      ],
    })

    render(<DashboardPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByTestId('evolucao-saldo-chart')).toBeInTheDocument()
    })
  })
})
