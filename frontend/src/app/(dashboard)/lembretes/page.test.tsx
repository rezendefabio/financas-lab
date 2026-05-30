import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/lembrete/services/lembrete-service', () => ({
  listarLembretes: vi.fn(),
  buscarLembrete: vi.fn(),
  criarLembrete: vi.fn(),
  atualizarLembrete: vi.fn(),
  excluirLembrete: vi.fn(),
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  usePathname: () => '/lembretes',
}))

import LembretesPage from './page'
import {
  listarLembretes,
  excluirLembrete,
} from '@/features/lembrete/services/lembrete-service'
import type { Lembrete } from '@/features/lembrete'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const lembreteFixture = (overrides?: Partial<Lembrete>): Lembrete => ({
  id: 'abc-123',
  userId: 'user-1',
  titulo: 'Pagar boleto',
  descricao: 'Conta de luz',
  dataLembrete: '2026-06-15',
  prioridade: 'MEDIA',
  concluido: false,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: null,
  ...overrides,
})

describe('LembretesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe titulo da tela', () => {
    vi.mocked(listarLembretes).mockReturnValue(new Promise(() => {}))

    render(<LembretesPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /lembretes/i })).toBeTruthy()
  })

  it('exibe lista com data formatada e prioridade traduzida', async () => {
    vi.mocked(listarLembretes).mockResolvedValue([lembreteFixture()])

    render(<LembretesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Pagar boleto')).toBeTruthy()
    })

    expect(screen.getByText('15/06/2026')).toBeTruthy()
    expect(screen.getByText('Media')).toBeTruthy()
    expect(screen.getByText('Nao')).toBeTruthy()
  })

  it('exibe Sim quando concluido', async () => {
    vi.mocked(listarLembretes).mockResolvedValue([
      lembreteFixture({ concluido: true }),
    ])

    render(<LembretesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Sim')).toBeTruthy()
    })
  })

  it('exibe mensagem vazia quando lista retorna zero lembretes', async () => {
    vi.mocked(listarLembretes).mockResolvedValue([])

    render(<LembretesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/nenhum lembrete cadastrado/i)).toBeTruthy()
    })

    expect(screen.getByRole('button', { name: /criar primeiro lembrete/i })).toBeTruthy()
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(listarLembretes).mockRejectedValue(new Error('Network error'))

    render(<LembretesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar lembretes/i)).toBeTruthy()
    })
  })

  it('navega para /lembretes/novo ao clicar em Novo Lembrete', async () => {
    vi.mocked(listarLembretes).mockResolvedValue([])

    render(<LembretesPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /novo lembrete/i }))

    expect(mockPush).toHaveBeenCalledWith('/lembretes/novo')
  })

  it('aciona exclusao com confirmacao', async () => {
    vi.mocked(listarLembretes).mockResolvedValue([lembreteFixture({ id: 'abc-123' })])
    vi.mocked(excluirLembrete).mockResolvedValue(undefined)

    render(<LembretesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Pagar boleto')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))
    await userEvent.click(screen.getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(excluirLembrete).toHaveBeenCalledWith('abc-123')
    })
  })

  it('navega para /lembretes/:id ao clicar em Editar', async () => {
    vi.mocked(listarLembretes).mockResolvedValue([lembreteFixture({ id: 'abc-123' })])

    render(<LembretesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Pagar boleto')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /editar/i }))

    expect(mockPush).toHaveBeenCalledWith('/lembretes/abc-123')
  })
})
