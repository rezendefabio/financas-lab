import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/lancamentorecorrente', () => ({
  lancamentoRecorrenteService: {
    listar: vi.fn(),
  },
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import LancamentosRecorrentesPage from './page'
import { lancamentoRecorrenteService } from '@/features/lancamentorecorrente'
import type { LancamentoRecorrente } from '@/features/lancamentorecorrente'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const lancamentoFixture = (overrides?: Partial<LancamentoRecorrente>): LancamentoRecorrente => ({
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
  ...overrides,
})

describe('LancamentosRecorrentesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe skeleton de loading enquanto dados carregam', () => {
    vi.mocked(lancamentoRecorrenteService.listar).mockReturnValue(new Promise(() => {}))

    render(<LancamentosRecorrentesPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /lancamentos recorrentes/i })).toBeTruthy()
    const skeletons = document.querySelectorAll('[class*="skeleton"], [data-slot="skeleton"]')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('exibe tabela de lancamentos apos carregamento', async () => {
    vi.mocked(lancamentoRecorrenteService.listar).mockResolvedValue([
      lancamentoFixture({ descricao: 'Aluguel', tipo: 'DESPESA', periodicidade: 'MENSAL', ativo: true }),
      lancamentoFixture({ id: 'lr-456', descricao: 'Salario', tipo: 'RECEITA', periodicidade: 'MENSAL', ativo: false }),
    ])

    render(<LancamentosRecorrentesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Aluguel')).toBeTruthy()
    })

    expect(screen.getByText('Salario')).toBeTruthy()
    expect(screen.getByText('Despesa')).toBeTruthy()
    expect(screen.getByText('Receita')).toBeTruthy()
    expect(screen.getAllByText('Mensal').length).toBeGreaterThan(0)
    expect(screen.getByText('Sim')).toBeTruthy()
    expect(screen.getByText('Nao')).toBeTruthy()
  })

  it('exibe mensagem vazia quando lista retorna zero lancamentos', async () => {
    vi.mocked(lancamentoRecorrenteService.listar).mockResolvedValue([])

    render(<LancamentosRecorrentesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/nenhum lancamento recorrente cadastrado/i)).toBeTruthy()
    })

    expect(screen.getByRole('button', { name: /criar primeiro lancamento recorrente/i })).toBeTruthy()
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(lancamentoRecorrenteService.listar).mockRejectedValue(new Error('Network error'))

    render(<LancamentosRecorrentesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar lancamentos recorrentes/i)).toBeTruthy()
    })
  })

  it('navega para /lancamentos-recorrentes/novo ao clicar em botao novo', async () => {
    vi.mocked(lancamentoRecorrenteService.listar).mockResolvedValue([])

    render(<LancamentosRecorrentesPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /novo lancamento recorrente/i }))

    expect(mockPush).toHaveBeenCalledWith('/lancamentos-recorrentes/novo')
  })

  it('navega para /lancamentos-recorrentes/:id ao clicar em linha da tabela', async () => {
    vi.mocked(lancamentoRecorrenteService.listar).mockResolvedValue([
      lancamentoFixture({ id: 'lr-123', descricao: 'Aluguel' }),
    ])

    render(<LancamentosRecorrentesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Aluguel')).toBeTruthy()
    })

    await userEvent.click(screen.getByText('Aluguel'))

    expect(mockPush).toHaveBeenCalledWith('/lancamentos-recorrentes/lr-123')
  })

  it('exibe valor formatado em BRL', async () => {
    vi.mocked(lancamentoRecorrenteService.listar).mockResolvedValue([
      lancamentoFixture({ valor: { valor: 1200, moeda: 'BRL' } }),
    ])

    render(<LancamentosRecorrentesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Aluguel')).toBeTruthy()
    })

    expect(screen.getByText(/1\.200/)).toBeTruthy()
  })

  it('exibe data formatada da proxima ocorrencia', async () => {
    vi.mocked(lancamentoRecorrenteService.listar).mockResolvedValue([
      lancamentoFixture({ proximaOcorrencia: '2026-06-01' }),
    ])

    render(<LancamentosRecorrentesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Aluguel')).toBeTruthy()
    })

    expect(screen.getByText('01/06/2026')).toBeTruthy()
  })
})
