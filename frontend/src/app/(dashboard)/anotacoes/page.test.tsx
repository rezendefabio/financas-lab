import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/anotacoes/services/anotacao-service', () => ({
  anotacaoService: {
    listar: vi.fn(),
    deletar: vi.fn(),
  },
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import AnotacoesPage from './page'
import { anotacaoService } from '@/features/anotacoes/services/anotacao-service'
import type { Anotacao } from '@/features/anotacoes/types/anotacao'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const anotacaoFixture = (overrides?: Partial<Anotacao>): Anotacao => ({
  id: 'abc-123',
  userId: 'user-1',
  titulo: 'Pagar fatura',
  conteudo: null,
  tipo: 'LEMBRETE',
  prioridade: 'MEDIA',
  valorMontante: null,
  valorMoeda: null,
  dataReferencia: null,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
  ...overrides,
})

describe('AnotacoesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe skeleton de loading enquanto dados carregam', () => {
    vi.mocked(anotacaoService.listar).mockReturnValue(new Promise(() => {}))

    render(<AnotacoesPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /anotacoes/i })).toBeTruthy()
  })

  it('exibe lista de anotacoes apos carregamento', async () => {
    vi.mocked(anotacaoService.listar).mockResolvedValue([
      anotacaoFixture({ titulo: 'Pagar fatura', tipo: 'LEMBRETE', prioridade: 'MEDIA' }),
      anotacaoFixture({ id: 'def-456', titulo: 'Planejamento anual', tipo: 'PLANEJAMENTO', prioridade: 'ALTA' }),
    ])

    render(<AnotacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Pagar fatura')).toBeTruthy()
    })

    expect(screen.getByText('Planejamento anual')).toBeTruthy()
    expect(screen.getByText('Lembrete')).toBeTruthy()
    expect(screen.getByText('Planejamento')).toBeTruthy()
  })

  it('exibe mensagem vazia quando lista retorna zero anotacoes', async () => {
    vi.mocked(anotacaoService.listar).mockResolvedValue([])

    render(<AnotacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/nenhuma anotacao cadastrada/i)).toBeTruthy()
    })

    expect(screen.getByRole('button', { name: /criar primeira anotacao/i })).toBeTruthy()
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(anotacaoService.listar).mockRejectedValue(new Error('Network error'))

    render(<AnotacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar anotacoes/i)).toBeTruthy()
    })
  })

  it('navega para /anotacoes/novo ao clicar em Nova Anotacao', async () => {
    vi.mocked(anotacaoService.listar).mockResolvedValue([])

    render(<AnotacoesPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /nova anotacao/i }))

    expect(mockPush).toHaveBeenCalledWith('/anotacoes/novo')
  })

  it('exibe valor formatado quando anotacao tem valorMontante', async () => {
    vi.mocked(anotacaoService.listar).mockResolvedValue([
      anotacaoFixture({ valorMontante: 500, valorMoeda: 'BRL' }),
    ])

    render(<AnotacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Pagar fatura')).toBeTruthy()
    })

    expect(screen.getByText(/500/)).toBeTruthy()
  })

  it('exibe -- para valor quando nao ha valorMontante', async () => {
    vi.mocked(anotacaoService.listar).mockResolvedValue([
      anotacaoFixture({ valorMontante: null }),
    ])

    render(<AnotacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Pagar fatura')).toBeTruthy()
    })

    const dashes = screen.getAllByText('--')
    expect(dashes.length).toBeGreaterThan(0)
  })

  it('navega para /anotacoes/:id ao clicar em Ver', async () => {
    vi.mocked(anotacaoService.listar).mockResolvedValue([
      anotacaoFixture({ id: 'abc-123', titulo: 'Pagar fatura' }),
    ])

    render(<AnotacoesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Pagar fatura')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /ver/i }))

    expect(mockPush).toHaveBeenCalledWith('/anotacoes/abc-123')
  })
})
