import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/fatura', () => ({
  listarFaturas: vi.fn(),
  deletarFatura: vi.fn(),
}))

vi.mock('@/features/contas/services/contas.service', () => ({
  contasService: {
    listar: vi.fn(),
  },
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import FaturasPage from './page'
import { listarFaturas } from '@/features/fatura'
import type { FaturaResponse } from '@/features/fatura'
import { contasService } from '@/features/contas/services/contas.service'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const faturaFixture = (overrides?: Partial<FaturaResponse>): FaturaResponse => ({
  id: 'abc-123',
  contaId: 'conta-1',
  nome: 'Cartao Maio',
  dataVencimento: '2026-06-10',
  dataFechamento: null,
  valorTotal: { valor: 1500, moeda: 'BRL' },
  paga: false,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
  ...overrides,
})

const contaFixture = {
  id: 'conta-1',
  nome: 'Nubank',
  ativa: true,
}

describe('FaturasPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.mocked(contasService.listar).mockResolvedValue([contaFixture] as any)
  })

  it('exibe titulo da tela', () => {
    vi.mocked(listarFaturas).mockReturnValue(new Promise(() => {}))

    render(<FaturasPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /faturas/i })).toBeTruthy()
  })

  it('exibe lista de faturas apos carregamento com nome da conta', async () => {
    vi.mocked(listarFaturas).mockResolvedValue([
      faturaFixture({ nome: 'Cartao Maio', paga: false }),
    ])

    render(<FaturasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Cartao Maio')).toBeTruthy()
    })

    expect(screen.getByText('Nubank')).toBeTruthy()
    expect(screen.getByText('Pendente')).toBeTruthy()
  })

  it('exibe badge Paga quando fatura esta paga', async () => {
    vi.mocked(listarFaturas).mockResolvedValue([
      faturaFixture({ paga: true }),
    ])

    render(<FaturasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Paga')).toBeTruthy()
    })
  })

  it('exibe valor formatado em BRL', async () => {
    vi.mocked(listarFaturas).mockResolvedValue([
      faturaFixture({ valorTotal: { valor: 1500, moeda: 'BRL' } }),
    ])

    render(<FaturasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Cartao Maio')).toBeTruthy()
    })

    expect(screen.getByText(/1\.500/)).toBeTruthy()
  })

  it('exibe traco quando valorTotal e nulo', async () => {
    vi.mocked(listarFaturas).mockResolvedValue([
      faturaFixture({ valorTotal: null }),
    ])

    render(<FaturasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Cartao Maio')).toBeTruthy()
    })

    expect(screen.getAllByText('—').length).toBeGreaterThan(0)
  })

  it('exibe mensagem vazia quando lista retorna zero faturas', async () => {
    vi.mocked(listarFaturas).mockResolvedValue([])

    render(<FaturasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/nenhuma fatura cadastrada/i)).toBeTruthy()
    })

    expect(screen.getByRole('button', { name: /criar primeira fatura/i })).toBeTruthy()
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(listarFaturas).mockRejectedValue(new Error('Network error'))

    render(<FaturasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar faturas/i)).toBeTruthy()
    })
  })

  it('navega para /faturas/nova ao clicar em Nova fatura', async () => {
    vi.mocked(listarFaturas).mockResolvedValue([])

    render(<FaturasPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /nova fatura/i }))

    expect(mockPush).toHaveBeenCalledWith('/faturas/nova')
  })

  it('navega para /faturas/:id ao clicar em Editar', async () => {
    vi.mocked(listarFaturas).mockResolvedValue([
      faturaFixture({ id: 'abc-123' }),
    ])

    render(<FaturasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Cartao Maio')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /editar/i }))

    expect(mockPush).toHaveBeenCalledWith('/faturas/abc-123')
  })
})
