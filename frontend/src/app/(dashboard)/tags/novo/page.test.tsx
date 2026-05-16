import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/tag', () => ({
  criarTag: vi.fn(),
  listarTags: vi.fn(),
  atualizarTag: vi.fn(),
  deletarTag: vi.fn(),
}))

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, back: mockBack }),
}))

import NovaTagPage from './page'
import { criarTag } from '@/features/tag'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

describe('NovaTagPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renderiza formulario com campos Nome e Cor', () => {
    render(<NovaTagPage />, { wrapper: makeWrapper() })

    expect(screen.getByRole('heading', { name: /nova tag/i })).toBeTruthy()
    expect(screen.getByLabelText(/nome/i)).toBeTruthy()
    expect(screen.getByLabelText(/cor/i)).toBeTruthy()
  })

  it('exibe erro de validacao quando nome esta vazio', async () => {
    render(<NovaTagPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/nome obrigatorio/i)).toBeTruthy()
    })
  })

  it('nao permite nome com mais de 50 caracteres via maxLength', () => {
    render(<NovaTagPage />, { wrapper: makeWrapper() })

    const nomeInput = screen.getByLabelText(/nome/i)
    expect(nomeInput.getAttribute('maxlength')).toBe('50')
  })

  it('submete formulario com dados validos e navega para /tags', async () => {
    vi.mocked(criarTag).mockResolvedValue({
      id: 'tag-001',
      userId: 'user-001',
      nome: 'Urgente',
      cor: '#FF5733',
      criadoEm: '2026-01-01T00:00:00Z',
    })

    render(<NovaTagPage />, { wrapper: makeWrapper() })

    await userEvent.type(screen.getByLabelText(/nome/i), 'Urgente')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(criarTag).toHaveBeenCalledWith(expect.objectContaining({ nome: 'Urgente' }))
    })

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/tags')
    })
  })

  it('exibe erro de api quando criacao falha', async () => {
    vi.mocked(criarTag).mockRejectedValue(new Error('Server error'))

    render(<NovaTagPage />, { wrapper: makeWrapper() })

    await userEvent.type(screen.getByLabelText(/nome/i), 'Urgente')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/erro ao criar tag/i)).toBeTruthy()
    })
  })

  it('navega de volta ao clicar Cancelar', async () => {
    render(<NovaTagPage />, { wrapper: makeWrapper() })

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/tags')
  })

  it('possui color picker nativo para o campo cor', () => {
    render(<NovaTagPage />, { wrapper: makeWrapper() })

    const colorPicker = screen.getByLabelText(/selecionar cor/i)
    expect(colorPicker.getAttribute('type')).toBe('color')
  })
})
