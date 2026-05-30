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
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, back: mockBack }),
  useParams: () => ({ id: 'abc-123' }),
  usePathname: () => '/lembretes/abc-123',
}))

import EditarLembretePage from './page'
import {
  buscarLembrete,
  atualizarLembrete,
} from '@/features/lembrete/services/lembrete-service'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const lembreteCarregado = {
  id: 'abc-123',
  userId: 'u',
  titulo: 'Pagar boleto',
  descricao: 'Conta de luz',
  dataLembrete: '2026-06-15',
  prioridade: 'MEDIA' as const,
  concluido: false,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: null,
}

describe('EditarLembretePage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('exibe carregando enquanto busca', () => {
    vi.mocked(buscarLembrete).mockReturnValue(new Promise(() => {}))

    render(<EditarLembretePage />, { wrapper: makeWrapper() })

    expect(screen.getByText(/carregando/i)).toBeTruthy()
  })

  it('exibe erro quando busca falha', async () => {
    vi.mocked(buscarLembrete).mockRejectedValue(new Error('not found'))

    render(<EditarLembretePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/lembrete nao encontrado/i)).toBeTruthy()
    })
  })

  it('popula formulario com dados carregados e atualiza', async () => {
    vi.mocked(buscarLembrete).mockResolvedValue(lembreteCarregado)
    vi.mocked(atualizarLembrete).mockResolvedValue(lembreteCarregado)

    render(<EditarLembretePage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect((screen.getByLabelText(/titulo/i) as HTMLInputElement).value).toBe('Pagar boleto')
    })

    const tituloInput = screen.getByLabelText(/titulo/i) as HTMLInputElement
    await userEvent.clear(tituloInput)
    await userEvent.type(tituloInput, 'Renomeado')

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(atualizarLembrete).toHaveBeenCalled()
    })
    const args = vi.mocked(atualizarLembrete).mock.calls[0]
    expect(args[0]).toBe('abc-123')
    expect(args[1].titulo).toBe('Renomeado')

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/lembretes')
    })
  })
})
