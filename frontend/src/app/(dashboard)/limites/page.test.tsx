import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/limite', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/features/limite')>()
  return {
    ...actual,
    listarLimites: vi.fn(),
    desativarLimite: vi.fn(),
  }
})

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import LimitesPage from './page'
import { listarLimites } from '@/features/limite'
import type { Limite } from '@/features/limite'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const limiteFixture = (overrides?: Partial<Limite>): Limite => ({
  id: 'abc-123',
  userId: 'user-1',
  nome: 'Limite de lazer',
  tipo: 'MENSAL',
  valor: 500,
  ativo: true,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
  ...overrides,
})

describe('LimitesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe titulo da tela', () => {
    vi.mocked(listarLimites).mockReturnValue(new Promise(() => {}))

    render(<LimitesPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /limites/i })).toBeTruthy()
  })

  it('exibe lista com tipo formatado e valor em BRL', async () => {
    vi.mocked(listarLimites).mockResolvedValue([
      limiteFixture({ nome: 'Limite de lazer', tipo: 'MENSAL', valor: 500 }),
    ])

    render(<LimitesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Limite de lazer')).toBeTruthy()
    })

    expect(screen.getByText('Mensal')).toBeTruthy()
    expect(screen.getByText(/R\$\s?500,00/)).toBeTruthy()
  })

  it('exibe mensagem vazia quando lista retorna zero limites', async () => {
    vi.mocked(listarLimites).mockResolvedValue([])

    render(<LimitesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/nenhum limite cadastrado/i)).toBeTruthy()
    })

    expect(screen.getByRole('button', { name: /criar primeiro limite/i })).toBeTruthy()
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(listarLimites).mockRejectedValue(new Error('Network error'))

    render(<LimitesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar limites/i)).toBeTruthy()
    })
  })

  it('navega para /limites/novo ao clicar em Novo Limite', async () => {
    vi.mocked(listarLimites).mockResolvedValue([])

    render(<LimitesPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /novo limite/i }))

    expect(mockPush).toHaveBeenCalledWith('/limites/novo')
  })

  it('aciona desativacao com confirmacao', async () => {
    vi.mocked(listarLimites).mockResolvedValue([limiteFixture({ id: 'abc-123' })])
    const { desativarLimite } = await import('@/features/limite')
    vi.mocked(desativarLimite).mockResolvedValue(undefined)

    render(<LimitesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Limite de lazer')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /desativar/i }))
    await userEvent.click(screen.getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(desativarLimite).toHaveBeenCalledWith('abc-123')
    })
  })

  it('navega para /limites/:id ao clicar em Editar', async () => {
    vi.mocked(listarLimites).mockResolvedValue([limiteFixture({ id: 'abc-123' })])

    render(<LimitesPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Limite de lazer')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /editar/i }))

    expect(mockPush).toHaveBeenCalledWith('/limites/abc-123')
  })
})
