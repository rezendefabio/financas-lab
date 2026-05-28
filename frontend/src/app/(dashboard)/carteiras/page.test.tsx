import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/carteira', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/features/carteira')>()
  return {
    ...actual,
    listarCarteiras: vi.fn(),
    deletarCarteira: vi.fn(),
  }
})

vi.mock('@/features/contas/services/contas.service', () => ({
  contasService: {
    listar: vi.fn(),
  },
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import CarteirasPage from './page'
import { listarCarteiras } from '@/features/carteira'
import type { CarteiraResponse } from '@/features/carteira'
import { contasService } from '@/features/contas/services/contas.service'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const carteiraFixture = (overrides?: Partial<CarteiraResponse>): CarteiraResponse => ({
  id: 'abc-123',
  contaId: 'conta-1',
  nome: 'Tesouro',
  tipo: 'RENDA_FIXA',
  ativo: true,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
  ...overrides,
})

const contaFixture = {
  id: 'conta-1',
  nome: 'Nubank',
  ativa: true,
}

describe('CarteirasPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.mocked(contasService.listar).mockResolvedValue([contaFixture] as any)
  })

  it('exibe titulo da tela', () => {
    vi.mocked(listarCarteiras).mockReturnValue(new Promise(() => {}))

    render(<CarteirasPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /carteiras/i })).toBeTruthy()
  })

  it('exibe lista de carteiras com nome da conta e badge de tipo', async () => {
    vi.mocked(listarCarteiras).mockResolvedValue([
      carteiraFixture({ nome: 'Tesouro', tipo: 'RENDA_FIXA' }),
    ])

    render(<CarteirasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Tesouro')).toBeTruthy()
    })

    expect(screen.getByText('Nubank')).toBeTruthy()
    expect(screen.getByText('Renda Fixa')).toBeTruthy()
  })

  it('exibe badge de tipo correto para CRIPTOMOEDA', async () => {
    vi.mocked(listarCarteiras).mockResolvedValue([
      carteiraFixture({ tipo: 'CRIPTOMOEDA' }),
    ])

    render(<CarteirasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Criptomoeda')).toBeTruthy()
    })
  })

  it('exibe mensagem vazia quando lista retorna zero carteiras', async () => {
    vi.mocked(listarCarteiras).mockResolvedValue([])

    render(<CarteirasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/nenhuma carteira cadastrada/i)).toBeTruthy()
    })

    expect(screen.getByRole('button', { name: /criar primeira carteira/i })).toBeTruthy()
  })

  it('exibe mensagem de erro quando query falha', async () => {
    vi.mocked(listarCarteiras).mockRejectedValue(new Error('Network error'))

    render(<CarteirasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/erro ao carregar carteiras/i)).toBeTruthy()
    })
  })

  it('navega para /carteiras/nova ao clicar em Nova carteira', async () => {
    vi.mocked(listarCarteiras).mockResolvedValue([])

    render(<CarteirasPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /nova carteira/i }))

    expect(mockPush).toHaveBeenCalledWith('/carteiras/nova')
  })

  it('aciona delete com confirmacao', async () => {
    vi.mocked(listarCarteiras).mockResolvedValue([carteiraFixture({ id: 'abc-123' })])
    const { deletarCarteira } = await import('@/features/carteira')
    vi.mocked(deletarCarteira).mockResolvedValue(undefined)

    render(<CarteirasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Tesouro')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))
    await userEvent.click(screen.getByRole('button', { name: /confirmar/i }))

    await waitFor(() => {
      expect(deletarCarteira).toHaveBeenCalledWith('abc-123')
    })
  })

  it('navega para /carteiras/:id ao clicar em Editar', async () => {
    vi.mocked(listarCarteiras).mockResolvedValue([carteiraFixture({ id: 'abc-123' })])

    render(<CarteirasPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText('Tesouro')).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /editar/i }))

    expect(mockPush).toHaveBeenCalledWith('/carteiras/abc-123')
  })
})
