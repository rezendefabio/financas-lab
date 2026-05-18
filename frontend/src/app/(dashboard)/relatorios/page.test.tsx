import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('recharts', async () => {
  const actual = await vi.importActual<typeof import('recharts')>('recharts')
  return {
    ...actual,
    ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
      <div style={{ width: 600, height: 300 }}>{children}</div>
    ),
  }
})

vi.mock('@/features/relatorios', async () => {
  const actual = await vi.importActual<typeof import('@/features/relatorios')>(
    '@/features/relatorios',
  )
  return {
    ...actual,
    relatorioService: {
      getGastosPorCategoria: vi.fn(),
      getEvolucaoSaldo: vi.fn(),
    },
  }
})

vi.mock('@/features/dashboard', async () => {
  const actual = await vi.importActual<typeof import('@/features/dashboard')>(
    '@/features/dashboard',
  )
  return {
    ...actual,
    getFluxoCaixa: vi.fn(),
  }
})

vi.mock('@/features/contas/services/contas.service', () => ({
  contasService: {
    listar: vi.fn(),
  },
}))

vi.mock('@/features/relatorios/components/RelatorioGastosPorCategoria', () => ({
  RelatorioGastosPorCategoria: ({ periodo }: { periodo: string }) => (
    <button type="button">Baixar PDF {periodo}</button>
  ),
}))

import RelatoriosPage from './page'
import { relatorioService } from '@/features/relatorios'
import { getFluxoCaixa } from '@/features/dashboard'
import { contasService } from '@/features/contas/services/contas.service'
import type { Conta } from '@/features/contas/types/conta'
import type { GastosPorCategoria, EvolucaoSaldo } from '@/features/relatorios'
import type { FluxoCaixa } from '@/features/dashboard'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const gastosFixture: GastosPorCategoria = {
  dataInicio: '2026-05-01',
  dataFim: '2026-05-16',
  totalGeral: { valor: 500, moeda: 'BRL' },
  itensPorCategoria: [
    { categoriaId: 'cat-1', nomeCategoria: 'Alimentacao', totalGasto: { valor: 500, moeda: 'BRL' } },
  ],
}

const evolucaoFixture: EvolucaoSaldo = {
  dataInicio: '2026-05-01',
  dataFim: '2026-05-16',
  totalReceitas: { valor: 3000, moeda: 'BRL' },
  totalDespesas: { valor: 1000, moeda: 'BRL' },
  saldoLiquido: { valor: 2000, moeda: 'BRL' },
  evolucaoPorMes: [
    {
      mes: '2026-05-01',
      totalReceitas: { valor: 3000, moeda: 'BRL' },
      totalDespesas: { valor: 1000, moeda: 'BRL' },
      saldoLiquido: { valor: 2000, moeda: 'BRL' },
    },
  ],
}

const fluxoFixture: FluxoCaixa = {
  ano: 2026,
  mes: 5,
  totalReceitas: 3000,
  totalDespesas: 1000,
  saldo: 2000,
  moeda: 'BRL',
}

const contaFixture: Conta = {
  id: 'conta-001',
  userId: null,
  nome: 'Conta Corrente',
  tipo: 'CORRENTE',
  saldoInicialValor: 0,
  saldoInicialMoeda: 'BRL',
  saldoAtualValor: null,
  saldoAtualMoeda: null,
  limiteCreditoValor: null,
  limiteCreditoMoeda: null,
  diaFechamento: null,
  diaVencimento: null,
  ativa: true,
  criadoEm: '2026-05-01T00:00:00Z',
  atualizadoEm: '2026-05-01T00:00:00Z',
}

describe('RelatoriosPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(contasService.listar).mockResolvedValue([contaFixture])
    vi.mocked(relatorioService.getGastosPorCategoria).mockResolvedValue(gastosFixture)
    vi.mocked(relatorioService.getEvolucaoSaldo).mockResolvedValue(evolucaoFixture)
    vi.mocked(getFluxoCaixa).mockResolvedValue(fluxoFixture)
  })

  it('renderiza o titulo da pagina e o card de filtros', () => {
    render(<RelatoriosPage />, { wrapper: makeWrapper() })
    expect(screen.getByRole('heading', { name: /relatorios/i })).toBeInTheDocument()
    expect(screen.getByText('Filtros')).toBeInTheDocument()
  })

  it('exibe skeletons de loading enquanto os relatorios carregam', () => {
    vi.mocked(relatorioService.getGastosPorCategoria).mockReturnValue(new Promise(() => {}))
    vi.mocked(relatorioService.getEvolucaoSaldo).mockReturnValue(new Promise(() => {}))
    vi.mocked(getFluxoCaixa).mockReturnValue(new Promise(() => {}))

    render(<RelatoriosPage />, { wrapper: makeWrapper() })

    const skeletons = document.querySelectorAll('[data-slot="skeleton"]')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('renderiza as tres secoes de relatorio apos o carregamento', async () => {
    render(<RelatoriosPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Gastos por categoria')).toBeInTheDocument()
    })
    expect(screen.getByText('Evolucao do saldo')).toBeInTheDocument()
    expect(screen.getByText('Fluxo de caixa do mes')).toBeInTheDocument()
  })

  it('chama os servicos de relatorio com as datas padrao', async () => {
    render(<RelatoriosPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(relatorioService.getGastosPorCategoria).toHaveBeenCalled()
    })
    expect(relatorioService.getEvolucaoSaldo).toHaveBeenCalled()
    expect(getFluxoCaixa).toHaveBeenCalled()
  })

  it('dispara nova query ao trocar a data de inicio', async () => {
    const user = userEvent.setup()
    render(<RelatoriosPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(relatorioService.getGastosPorCategoria).toHaveBeenCalled()
    })
    const chamadasIniciais = vi.mocked(relatorioService.getGastosPorCategoria).mock.calls.length

    const inputInicio = screen.getByLabelText('Data inicio')
    await user.clear(inputInicio)
    await user.type(inputInicio, '2026-01-01')

    await waitFor(() => {
      expect(
        vi.mocked(relatorioService.getGastosPorCategoria).mock.calls.length,
      ).toBeGreaterThan(chamadasIniciais)
    })
    expect(relatorioService.getGastosPorCategoria).toHaveBeenLastCalledWith(
      '2026-01-01',
      expect.any(String),
      undefined,
    )
  })

  it('renderiza o botao de download do PDF quando os gastos carregam', async () => {
    render(<RelatoriosPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /baixar pdf/i }),
      ).toBeInTheDocument()
    })
  })

  it('exibe mensagem de erro quando a query de gastos falha', async () => {
    vi.mocked(relatorioService.getGastosPorCategoria).mockRejectedValue(
      new Error('Network error'),
    )

    render(<RelatoriosPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar gastos por categoria/i)).toBeInTheDocument()
    })
  })

  it('exibe mensagem de erro quando a query de fluxo de caixa falha', async () => {
    vi.mocked(getFluxoCaixa).mockRejectedValue(new Error('Network error'))

    render(<RelatoriosPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar fluxo de caixa/i)).toBeInTheDocument()
    })
  })
})
