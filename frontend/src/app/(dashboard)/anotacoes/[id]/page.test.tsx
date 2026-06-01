import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/anotacoes/services/anotacao-service', () => ({
  anotacaoService: {
    buscarPorId: vi.fn(),
    deletar: vi.fn(),
  },
}))

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, back: mockBack }),
  useParams: () => ({ id: 'abc-123' }),
}))

import AnotacaoDetalhePage from './page'
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
  conteudo: 'Fatura do cartao',
  tipo: 'LEMBRETE',
  prioridade: 'MEDIA',
  valorMontante: null,
  valorMoeda: null,
  dataReferencia: null,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
  ...overrides,
})

describe('AnotacaoDetalhePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe skeleton durante loading', () => {
    vi.mocked(anotacaoService.buscarPorId).mockReturnValue(new Promise(() => {}))

    render(<AnotacaoDetalhePage />, { wrapper: makeWrapper() })

    const skeletons = document.querySelectorAll('[data-slot="skeleton"]')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('exibe detalhes da anotacao apos carregamento', async () => {
    vi.mocked(anotacaoService.buscarPorId).mockResolvedValue(anotacaoFixture())

    render(<AnotacaoDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getAllByText('Pagar fatura').length).toBeGreaterThan(0)
    })

    expect(screen.getByText('Fatura do cartao')).toBeTruthy()
    expect(screen.getByText('Lembrete')).toBeTruthy()
    expect(screen.getByText('Media')).toBeTruthy()
  })

  it('exibe mensagem de erro quando nao encontrado', async () => {
    vi.mocked(anotacaoService.buscarPorId).mockRejectedValue(new Error('Not found'))

    render(<AnotacaoDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/anotacao nao encontrada/i)).toBeTruthy()
    })
  })

  it('exibe botoes Editar e Deletar', async () => {
    vi.mocked(anotacaoService.buscarPorId).mockResolvedValue(anotacaoFixture())

    render(<AnotacaoDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getAllByText('Pagar fatura').length).toBeGreaterThan(0)
    })

    expect(screen.getByRole('button', { name: /editar/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /deletar/i })).toBeTruthy()
  })

  it('navega para pagina de edicao ao clicar em Editar', async () => {
    vi.mocked(anotacaoService.buscarPorId).mockResolvedValue(anotacaoFixture())

    render(<AnotacaoDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /editar/i })).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /editar/i }))

    expect(mockPush).toHaveBeenCalledWith('/anotacoes/abc-123/editar')
  })

  it('exibe botao de confirmacao ao clicar em Deletar', async () => {
    vi.mocked(anotacaoService.buscarPorId).mockResolvedValue(anotacaoFixture())

    render(<AnotacaoDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /deletar/i })).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /deletar/i }))

    expect(screen.getByRole('button', { name: /confirmar/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('exibe valor monetario quando anotacao tem valor', async () => {
    vi.mocked(anotacaoService.buscarPorId).mockResolvedValue(
      anotacaoFixture({ valorMontante: 1500, valorMoeda: 'BRL' })
    )

    render(<AnotacaoDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/1\.500/)).toBeTruthy()
    })
  })

  it('exibe timestamps com formatDateTime (criadoEm e atualizadoEm)', async () => {
    vi.mocked(anotacaoService.buscarPorId).mockResolvedValue(anotacaoFixture())

    render(<AnotacaoDetalhePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/criado em/i)).toBeTruthy()
      expect(screen.getByText(/atualizado em/i)).toBeTruthy()
    })
  })
})
