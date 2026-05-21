import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

vi.mock('@/features/tag', () => ({
  listarTags: vi.fn(),
  atualizarTag: vi.fn(),
  criarTag: vi.fn(),
  deletarTag: vi.fn(),
}))

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, back: mockBack }),
  useParams: () => ({ id: 'tag-001' }),
  usePathname: () => '/tags/tag-001/editar',
}))

import EditarTagPage from './page'
import { listarTags, atualizarTag } from '@/features/tag'
import type { Tag } from '@/features/tag'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const tagFixture = (overrides?: Partial<Tag>): Tag => ({
  id: 'tag-001',
  userId: 'user-001',
  nome: 'Urgente',
  cor: '#FF5733',
  criadoEm: '2026-01-01T00:00:00Z',
  ...overrides,
})

describe('EditarTagPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe skeleton enquanto dados carregam', () => {
    vi.mocked(listarTags).mockReturnValue(new Promise(() => {}))

    render(<EditarTagPage />, { wrapper: makeWrapper() })

    const skeletons = document.querySelectorAll('[data-slot="skeleton"]')
    expect(skeletons.length).toBeGreaterThan(0)
  })

  it('pre-popula formulario com dados da tag', async () => {
    vi.mocked(listarTags).mockResolvedValue([tagFixture()])

    render(<EditarTagPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /editar tag/i })).toBeTruthy()
    })

    const nomeInput = screen.getByLabelText(/nome/i) as HTMLInputElement
    await waitFor(() => {
      expect(nomeInput.value).toBe('Urgente')
    })
  })

  it('exibe erro quando tag nao e encontrada', async () => {
    vi.mocked(listarTags).mockResolvedValue([])

    render(<EditarTagPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/tag nao encontrada/i)).toBeTruthy()
    })
  })

  it('submete atualizacao e navega para /tags', async () => {
    vi.mocked(listarTags).mockResolvedValue([tagFixture()])
    vi.mocked(atualizarTag).mockResolvedValue({
      ...tagFixture(),
      nome: 'Urgente Editado',
    })

    render(<EditarTagPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /editar tag/i })).toBeTruthy()
    })

    const nomeInput = screen.getByLabelText(/nome/i)
    await userEvent.clear(nomeInput)
    await userEvent.type(nomeInput, 'Urgente Editado')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(atualizarTag).toHaveBeenCalledWith(
        'tag-001',
        expect.objectContaining({ nome: 'Urgente Editado' })
      )
    })

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/tags')
    })
  })

  it('exibe erro de api quando atualizacao falha', async () => {
    vi.mocked(listarTags).mockResolvedValue([tagFixture()])
    vi.mocked(atualizarTag).mockRejectedValue(new Error('Server error'))

    render(<EditarTagPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      const nomeInput = screen.getByLabelText(/nome/i)
      expect(nomeInput).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/erro ao atualizar tag/i)).toBeTruthy()
    })
  })

  it('navega para /tags ao clicar Cancelar', async () => {
    vi.mocked(listarTags).mockResolvedValue([tagFixture()])

    render(<EditarTagPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/tags')
  })

  it('nao permite nome com mais de 50 caracteres via maxLength', async () => {
    vi.mocked(listarTags).mockResolvedValue([tagFixture()])

    render(<EditarTagPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      const nomeInput = screen.getByLabelText(/nome/i)
      expect(nomeInput.getAttribute('maxlength')).toBe('50')
    })
  })
})
