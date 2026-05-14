import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/transacoes/services/transacoes.service', () => ({
  transacoesService: {
    listar: vi.fn(),
  },
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import TransacoesPage from './page'
import { transacoesService } from '@/features/transacoes/services/transacoes.service'
import type { Transacao } from '@/features/transacoes/types/transacao'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const transacaoFixture = (overrides?: Partial<Transacao>): Transacao => ({
  id: 'tx-001',
  tipo: 'DESPESA',
  valor: 150.0,
  moeda: 'BRL',
  data: '2026-05-13',
  descricao: 'Supermercado',
  contaId: 'conta-001',
  contaDestinoId: null,
  categoriaId: null,
  criadoEm: '2026-05-13T00:00:00Z',
  atualizadoEm: '2026-05-13T00:00:00Z',
  ...overrides,
})

describe('TransacoesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe skeleton de loading enquanto dados carregam', () => {
    vi.mocked(transacoesService.listar).mockReturnValue(new Promise(() => {}))

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /transacoes/i })).toBeTruthy()
    const skeletons = document.querySelectorAll('[class*="skeleton"], [data-slot="skeleton"]')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('exibe lista de transacoes apos carregamento', async () => {
    vi.mocked(transacoesService.listar).mockResolvedValue({
      content: [
        transacaoFixture({ tipo: 'DESPESA', descricao: 'Supermercado', valor: 150.0, moeda: 'BRL' }),
        transacaoFixture({ id: 'tx-002', tipo: 'RECEITA', descricao: 'Salario', valor: 5000.0, moeda: 'BRL' }),
      ],
      totalElements: 2,
    })

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Supermercado')).toBeTruthy()
    })

    expect(screen.getByText('Salario')).toBeTruthy()
    expect(screen.getByText('Despesa')).toBeTruthy()
    expect(screen.getByText('Receita')).toBeTruthy()
  })

  it('exibe mensagem vazia quando lista retorna zero transacoes', async () => {
    vi.mocked(transacoesService.listar).mockResolvedValue({
      content: [],
      totalElements: 0,
    })

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/nenhuma transacao cadastrada/i)).toBeTruthy()
    })

    expect(screen.getByRole('button', { name: /registrar primeira transacao/i })).toBeTruthy()
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(transacoesService.listar).mockRejectedValue(new Error('Network error'))

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar transacoes/i)).toBeTruthy()
    })
  })

  it('navega para /transacoes/novo ao clicar em Nova Transacao', async () => {
    vi.mocked(transacoesService.listar).mockResolvedValue({ content: [], totalElements: 0 })

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /nova transacao/i }))

    expect(mockPush).toHaveBeenCalledWith('/transacoes/novo')
  })

  it('exibe aviso quando ha mais de 20 registros', async () => {
    vi.mocked(transacoesService.listar).mockResolvedValue({
      content: [transacaoFixture()],
      totalElements: 25,
    })

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/exibindo 20 de 25/i)).toBeTruthy()
    })
  })

  it('formata valor da transacao em BRL', async () => {
    vi.mocked(transacoesService.listar).mockResolvedValue({
      content: [transacaoFixture({ valor: 1500.5, moeda: 'BRL' })],
      totalElements: 1,
    })

    render(<TransacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Supermercado')).toBeTruthy()
    })

    expect(screen.getByText(/1\.500,50/)).toBeTruthy()
  })
})
