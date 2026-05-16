import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/contas/services/contas.service', () => ({
  contasService: {
    listar: vi.fn(),
    saldoTotal: vi.fn(),
  },
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import ContasPage from './page'
import { contasService } from '@/features/contas/services/contas.service'
import type { Conta, SaldoTotalResponse } from '@/features/contas/types/conta'

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
  userId: null,
  nome: 'Nubank',
  tipo: 'CORRENTE',
  saldoInicialValor: 1500.5,
  saldoInicialMoeda: 'BRL',
  saldoAtualValor: 1500.5,
  saldoAtualMoeda: 'BRL',
  limiteCreditoValor: null,
  limiteCreditoMoeda: null,
  diaFechamento: null,
  diaVencimento: null,
  ativa: true,
  criadoEm: '2024-01-01T00:00:00Z',
  atualizadoEm: '2024-01-01T00:00:00Z',
  ...overrides,
})

const saldoTotalFixture = (overrides?: Partial<SaldoTotalResponse>): SaldoTotalResponse => ({
  valor: 3000,
  moeda: 'BRL',
  totalContas: 2,
  ...overrides,
})

describe('ContasPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(contasService.saldoTotal).mockReturnValue(new Promise(() => {}))
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

  it('exibe label saldo atual nos cards', async () => {
    vi.mocked(contasService.listar).mockResolvedValue([
      contaFixture({ saldoAtualValor: 2000, saldoInicialValor: 1500.5 }),
    ])

    render(<ContasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Nubank')).toBeTruthy()
    })

    expect(screen.getByText('saldo atual')).toBeTruthy()
  })

  it('usa saldoAtualValor quando disponivel', async () => {
    vi.mocked(contasService.listar).mockResolvedValue([
      contaFixture({ saldoAtualValor: 2000, saldoInicialValor: 1500.5 }),
    ])

    render(<ContasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Nubank')).toBeTruthy()
    })

    // R$ 2.000,00 -- saldo atual
    expect(screen.getByText(/2\.000,00/)).toBeTruthy()
  })

  it('usa saldoInicialValor como fallback quando saldoAtualValor e null', async () => {
    vi.mocked(contasService.listar).mockResolvedValue([
      contaFixture({ saldoAtualValor: null, saldoInicialValor: 1500.5 }),
    ])

    render(<ContasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Nubank')).toBeTruthy()
    })

    // R$ 1.500,50 -- fallback para saldo inicial
    expect(screen.getByText(/1\.500,50/)).toBeTruthy()
  })

  it('exibe card de saldo total quando carregado', async () => {
    vi.mocked(contasService.listar).mockResolvedValue([])
    vi.mocked(contasService.saldoTotal).mockResolvedValue(saldoTotalFixture())

    render(<ContasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Saldo total')).toBeTruthy()
    })

    expect(screen.getByText(/3\.000/)).toBeTruthy()
    expect(screen.getByText(/2 conta\(s\)/)).toBeTruthy()
  })

  it('nao exibe card de saldo total quando ainda carregando', () => {
    vi.mocked(contasService.listar).mockResolvedValue([])
    vi.mocked(contasService.saldoTotal).mockReturnValue(new Promise(() => {}))

    render(<ContasPage />, { wrapper: makeWrapper() })

    expect(screen.queryByText('Saldo total')).toBeNull()
  })

  it('formata tipo INVESTIMENTO corretamente', async () => {
    vi.mocked(contasService.listar).mockResolvedValue([
      contaFixture({ tipo: 'INVESTIMENTO' }),
    ])

    render(<ContasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Investimento')).toBeTruthy()
    })
  })
})
