import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

const clearDraftMock = vi.fn()
vi.mock('@/shared/hooks/useDraftForm', () => ({
  useDraftForm: () => ({
    clearDraft: clearDraftMock,
    resetWithDraft: (values: unknown) => values,
  }),
}))

vi.mock('@/features/grupo', () => ({
  listarGrupos: vi.fn(),
  atualizarGrupo: vi.fn(),
  criarGrupo: vi.fn(),
  deletarGrupo: vi.fn(),
}))

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, back: mockBack }),
  useParams: () => ({ id: 'grupo-001' }),
  usePathname: () => '/grupos/grupo-001',
}))

import EditarGrupoPage from './page'
import { listarGrupos, atualizarGrupo } from '@/features/grupo'
import type { Grupo } from '@/features/grupo'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const grupoFixture = (overrides?: Partial<Grupo>): Grupo => ({
  id: 'grupo-001',
  userId: 'user-001',
  nome: 'Viagem Europa',
  descricao: 'Gastos da viagem',
  ativo: true,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
  ...overrides,
})

describe('EditarGrupoPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('carrega dados via useQuery e pre-popula o formulario', async () => {
    vi.mocked(listarGrupos).mockResolvedValue([grupoFixture()])

    render(<EditarGrupoPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /editar grupo/i })).toBeTruthy()
    })
    expect(listarGrupos).toHaveBeenCalled()
  })

  it('exibe erro quando grupo nao e encontrado', async () => {
    vi.mocked(listarGrupos).mockResolvedValue([])

    render(<EditarGrupoPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByText(/grupo nao encontrado/i)).toBeTruthy()
    })
  })

  it('submete atualizacao e navega para /grupos', async () => {
    vi.mocked(listarGrupos).mockResolvedValue([grupoFixture()])
    vi.mocked(atualizarGrupo).mockResolvedValue(grupoFixture({ nome: 'Editado' }))

    render(<EditarGrupoPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /editar grupo/i })).toBeTruthy()
    })

    const nomeInput = screen.getByLabelText(/nome/i)
    await userEvent.type(nomeInput, 'Editado')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(atualizarGrupo).toHaveBeenCalledWith(
        'grupo-001',
        expect.objectContaining({ nome: expect.stringContaining('Editado') }),
      )
    })

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/grupos')
    })
  })

  it('botao Cancelar chama clearDraft e navega para /grupos', async () => {
    vi.mocked(listarGrupos).mockResolvedValue([grupoFixture()])

    render(<EditarGrupoPage />, { wrapper: makeWrapper() })

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(clearDraftMock).toHaveBeenCalled()
    expect(mockPush).toHaveBeenCalledWith('/grupos')
  })
})
