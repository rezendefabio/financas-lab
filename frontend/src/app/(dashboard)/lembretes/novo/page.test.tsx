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
  usePathname: () => '/lembretes/novo',
}))

import NovoLembretePage from './page'
import { criarLembrete } from '@/features/lembrete/services/lembrete-service'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

describe('NovoLembretePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe titulo da tela', () => {
    render(<NovoLembretePage />, { wrapper: makeWrapper() })
    expect(screen.getByRole('heading', { name: /novo lembrete/i })).toBeTruthy()
  })

  it('envia payload e navega ao salvar', async () => {
    vi.mocked(criarLembrete).mockResolvedValue({
      id: 'abc',
      userId: 'u',
      titulo: 'X',
      descricao: null,
      dataLembrete: '2026-06-15',
      prioridade: 'MEDIA',
      concluido: false,
      criadoEm: '2026-01-01T00:00:00Z',
      atualizadoEm: null,
    })

    render(<NovoLembretePage />, { wrapper: makeWrapper() })

    await userEvent.type(screen.getByLabelText(/titulo/i), 'Pagar boleto')
    const dataInput = document.querySelector('input[type="date"]') as HTMLInputElement
    await userEvent.type(dataInput, '2026-06-15')

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(criarLembrete).toHaveBeenCalled()
    })
    const arg = vi.mocked(criarLembrete).mock.calls[0][0]
    expect(arg.titulo).toBe('Pagar boleto')
    expect(arg.dataLembrete).toBe('2026-06-15')
    expect(arg.prioridade).toBe('MEDIA')
    expect(arg.concluido).toBe(false)

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/lembretes')
    })
  })

  it('navega para listagem ao clicar em Cancelar', async () => {
    render(<NovoLembretePage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/lembretes')
  })
})
