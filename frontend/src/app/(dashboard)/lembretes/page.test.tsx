import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/lembrete', async () => {
  const actual =
    await vi.importActual<typeof import('@/features/lembrete')>(
      '@/features/lembrete',
    )
  return {
    ...actual,
    lembreteService: {
      listar: vi.fn(),
      buscarPorId: vi.fn(),
      criar: vi.fn(),
      atualizar: vi.fn(),
      deletar: vi.fn(),
    },
    useLembretes: vi.fn(),
  }
})

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import LembretesPage from './page'
import { lembreteService, useLembretes, type LembreteResponse } from '@/features/lembrete'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const fixture = (overrides?: Partial<LembreteResponse>): LembreteResponse => ({
  id: 'lem-001',
  titulo: 'Pagar conta',
  descricao: 'Conta de luz',
  dataLembrete: '2026-06-15',
  prioridade: 'MEDIA',
  concluido: false,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
  ...overrides,
})

describe('LembretesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(useLembretes).mockReturnValue({
      data: [fixture()],
      isLoading: false,
      error: null,
    } as unknown as ReturnType<typeof useLembretes>)
  })

  it('exibe cabecalhos da tabela', async () => {
    render(<LembretesPage />, { wrapper: makeWrapper() })
    await waitFor(() => expect(screen.getByText('Titulo')).toBeTruthy())
    expect(screen.getByText('Data')).toBeTruthy()
    expect(screen.getByText('Prioridade')).toBeTruthy()
    expect(screen.getByText('Status')).toBeTruthy()
    expect(screen.getByText('Acoes')).toBeTruthy()
  })

  it('exibe titulo do lembrete', async () => {
    render(<LembretesPage />, { wrapper: makeWrapper() })
    await waitFor(() => expect(screen.getByText('Pagar conta')).toBeTruthy())
  })

  it('navega para /lembretes/novo ao clicar em Novo lembrete', async () => {
    render(<LembretesPage />, { wrapper: makeWrapper() })
    await userEvent.click(screen.getByRole('button', { name: /novo lembrete/i }))
    expect(mockPush).toHaveBeenCalledWith('/lembretes/novo')
  })

  it('exibe confirmacao ao clicar em Excluir', async () => {
    render(<LembretesPage />, { wrapper: makeWrapper() })
    await waitFor(() => expect(screen.getByText('Pagar conta')).toBeTruthy())
    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))
    expect(screen.getByRole('button', { name: /confirmar/i })).toBeTruthy()
  })

  it('chama deletar ao confirmar exclusao', async () => {
    vi.mocked(lembreteService.deletar).mockResolvedValue(undefined)
    render(<LembretesPage />, { wrapper: makeWrapper() })
    await waitFor(() => expect(screen.getByText('Pagar conta')).toBeTruthy())
    await userEvent.click(screen.getByRole('button', { name: /excluir/i }))
    await userEvent.click(screen.getByRole('button', { name: /confirmar/i }))
    await waitFor(() =>
      expect(lembreteService.deletar).toHaveBeenCalledWith('lem-001'),
    )
  })

  it('navega para edicao ao clicar Editar', async () => {
    render(<LembretesPage />, { wrapper: makeWrapper() })
    await waitFor(() => expect(screen.getByText('Pagar conta')).toBeTruthy())
    await userEvent.click(screen.getByRole('button', { name: /editar/i }))
    expect(mockPush).toHaveBeenCalledWith('/lembretes/lem-001/editar')
  })
})
