import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/contas/services/contas.service', () => ({
  contasService: {
    listar: vi.fn(),
  },
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import ContasPage from './page'
import { contasService } from '@/features/contas/services/contas.service'
import type { Conta } from '@/features/contas/types/conta'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const contaFixture = (overrides?: Partial<Conta>): Conta => ({
  id: 'abc-123',
  nome: 'Nubank',
  tipo: 'CORRENTE',
  saldoInicialValor: 1500.5,
  saldoInicialMoeda: 'BRL',
  ativa: true,
  criadoEm: '2024-01-01T00:00:00Z',
  atualizadoEm: '2024-01-01T00:00:00Z',
  ...overrides,
})

describe('ContasPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe skeleton de loading enquanto dados carregam', () => {
    vi.mocked(contasService.listar).mockReturnValue(new Promise(() => {}))

    render(<ContasPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /contas/i })).toBeTruthy()
    // Skeletons renderizados durante loading (3 cards)
    const skeletons = document.querySelectorAll('[class*="skeleton"], [data-slot="skeleton"]')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('exibe lista de contas apos carregamento', async () => {
    vi.mocked(contasService.listar).mockResolvedValue([
      contaFixture({ nome: 'Nubank', tipo: 'CORRENTE', saldoInicialValor: 1500.5, ativa: true }),
      contaFixture({ id: 'def-456', nome: 'Poupanca BB', tipo: 'POUPANCA', saldoInicialValor: 300.0, ativa: false }),
    ])

    render(<ContasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Nubank')).toBeTruthy()
    })

    expect(screen.getByText('Poupanca BB')).toBeTruthy()
    expect(screen.getByText('Conta Corrente')).toBeTruthy()
    expect(screen.getByText('Poupanca')).toBeTruthy()
    expect(screen.getByText('Ativa')).toBeTruthy()
    expect(screen.getByText('Inativa')).toBeTruthy()
  })

  it('exibe mensagem vazia quando lista retorna zero contas', async () => {
    vi.mocked(contasService.listar).mockResolvedValue([])

    render(<ContasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/nenhuma conta cadastrada/i)).toBeTruthy()
    })

    expect(screen.getByRole('button', { name: /criar primeira conta/i })).toBeTruthy()
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(contasService.listar).mockRejectedValue(new Error('Network error'))

    render(<ContasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar contas/i)).toBeTruthy()
    })
  })

  it('navega para /contas/novo ao clicar em Nova Conta', async () => {
    vi.mocked(contasService.listar).mockResolvedValue([])

    render(<ContasPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /nova conta/i }))

    expect(mockPush).toHaveBeenCalledWith('/contas/novo')
  })

  it('navega para /contas/novo ao clicar em Criar primeira conta (estado vazio)', async () => {
    vi.mocked(contasService.listar).mockResolvedValue([])

    render(<ContasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /criar primeira conta/i })).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /criar primeira conta/i }))

    expect(mockPush).toHaveBeenCalledWith('/contas/novo')
  })

  it('navega para /contas/:id ao clicar em um card de conta', async () => {
    vi.mocked(contasService.listar).mockResolvedValue([
      contaFixture({ id: 'abc-123', nome: 'Nubank' }),
    ])

    render(<ContasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Nubank')).toBeTruthy()
    })

    await userEvent.click(screen.getByText('Nubank'))

    expect(mockPush).toHaveBeenCalledWith('/contas/abc-123')
  })

  it('formata saldo inicial em BRL', async () => {
    vi.mocked(contasService.listar).mockResolvedValue([
      contaFixture({ saldoInicialValor: 1500.5 }),
    ])

    render(<ContasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Nubank')).toBeTruthy()
    })

    // R$ 1.500,50 — formato pt-BR
    expect(screen.getByText(/1\.500,50/)).toBeTruthy()
  })

  it('exibe tipo desconhecido literalmente quando nao esta no mapa TIPO_LABEL', async () => {
    vi.mocked(contasService.listar).mockResolvedValue([
      contaFixture({ tipo: 'INVESTIMENTO' as never }),
    ])

    render(<ContasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('INVESTIMENTO')).toBeTruthy()
    })
  })
})
