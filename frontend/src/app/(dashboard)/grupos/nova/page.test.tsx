import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/grupo', () => ({
  criarGrupo: vi.fn(),
  listarGrupos: vi.fn(),
  atualizarGrupo: vi.fn(),
  deletarGrupo: vi.fn(),
}))

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, back: mockBack }),
  usePathname: () => '/grupos/nova',
}))

import NovoGrupoPage from './page'
import { criarGrupo } from '@/features/grupo'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

describe('NovoGrupoPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renderiza formulario com campos Nome e Descricao', () => {
    render(<NovoGrupoPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /novo grupo/i })).toBeTruthy()
    expect(screen.getByLabelText(/nome/i)).toBeTruthy()
    expect(screen.getByLabelText(/descricao/i)).toBeTruthy()
  })

  it('exibe erro de validacao quando nome esta vazio', async () => {
    render(<NovoGrupoPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/nome obrigatorio/i)).toBeTruthy()
    })
  })

  it('nao permite nome com mais de 100 caracteres via maxLength', () => {
    render(<NovoGrupoPage />, { wrapper: makeWrapper() })

    const nomeInput = screen.getByLabelText(/nome/i)
    expect(nomeInput.getAttribute('maxlength')).toBe('100')
  })

  it('submete formulario com dados validos e navega para /grupos', async () => {
    vi.mocked(criarGrupo).mockResolvedValue({
      id: 'grupo-001',
      userId: 'user-001',
      nome: 'Viagem Europa',
      descricao: null,
      ativo: true,
      criadoEm: '2026-01-01T00:00:00Z',
      atualizadoEm: '2026-01-01T00:00:00Z',
    })

    render(<NovoGrupoPage />, { wrapper: makeWrapper() })

    await userEvent.type(screen.getByLabelText(/nome/i), 'Viagem Europa')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(criarGrupo).toHaveBeenCalledWith(
        expect.objectContaining({ nome: 'Viagem Europa' }),
      )
    })

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/grupos')
    })
  })

  it('exibe erro de api quando criacao falha', async () => {
    vi.mocked(criarGrupo).mockRejectedValue(new Error('Server error'))

    render(<NovoGrupoPage />, { wrapper: makeWrapper() })

    await userEvent.type(screen.getByLabelText(/nome/i), 'Viagem Europa')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/erro ao criar grupo/i)).toBeTruthy()
    })
  })

  it('navega para /grupos ao clicar Cancelar', async () => {
    render(<NovoGrupoPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/grupos')
  })
})
