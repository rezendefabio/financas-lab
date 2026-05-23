import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/features/payee/services/payee-service', () => ({
  listarPayees: vi.fn(),
  atualizarPayee: vi.fn(),
}))

vi.mock('@/features/categorias/services/categorias.service', () => ({
  categoriasService: { listar: vi.fn() },
}))

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, back: mockBack }),
  useParams: () => ({ id: 'pay-001' }),
  usePathname: () => '/payees/pay-001/editar',
}))

const mockInvalidateQueries = vi.fn()
vi.mock('@tanstack/react-query', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-query')>()
  return {
    ...actual,
    useMutation: vi.fn(({ mutationFn, onSuccess, onError }) => ({
      mutate: vi.fn(async (data: unknown) => {
        try {
          const result = await mutationFn(data)
          await onSuccess(result)
        } catch (err) {
          onError(err)
        }
      }),
      isPending: false,
    })),
    useQueryClient: () => ({ invalidateQueries: mockInvalidateQueries }),
    useQuery: vi.fn(() => {
      return { data: undefined, isLoading: false, isError: false }
    }),
  }
})

import type { Payee } from '@/features/payee/types/payee'

import EditarPayeePage from './page'
import { useQuery } from '@tanstack/react-query'

const payeeFixture = (overrides?: Partial<Payee>): Payee => ({
  id: 'pay-001',
  userId: 'user-001',
  nome: 'Supermercado Extra',
  categoriaPadraoId: undefined,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
  ...overrides,
})

describe('EditarPayeePage', () => {
  // Refs estaveis: useQuery e chamado a cada render, retornar novo objeto/array
  // a cada chamada criava loop infinito (payee = data.find(...) era nova ref a
  // cada render, disparando useEffect [payee] -> resetWithDraft -> watch -> save
  // -> store update -> re-render).
  const payeesData = [payeeFixture()]
  const categoriasData: unknown[] = []
  const payeesResult = { data: payeesData, isLoading: false, isError: false } as ReturnType<typeof useQuery>
  const categoriasResult = { data: categoriasData, isLoading: false, isError: false } as ReturnType<typeof useQuery>

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(useQuery).mockImplementation(({ queryKey }: { queryKey: unknown[] }) => {
      if (Array.isArray(queryKey) && queryKey[0] === 'payees') {
        return payeesResult
      }
      return categoriasResult
    })
  })

  it('exibe formulario com dados pre-populados', async () => {
    render(<EditarPayeePage />)

    await waitFor(() => {
      const input = screen.getByLabelText(/nome/i) as HTMLInputElement
      expect(input.value).toBe('Supermercado Extra')
    })
  })

  it('exibe "Beneficiario nao encontrado" quando id nao existe', async () => {
    const emptyResult = { data: [], isLoading: false, isError: false } as ReturnType<typeof useQuery>
    vi.mocked(useQuery).mockImplementation(() => emptyResult)

    render(<EditarPayeePage />)

    await waitFor(() => {
      expect(screen.getByText(/beneficiario nao encontrado/i)).toBeTruthy()
    })
  })

  it('exibe estado de loading', async () => {
    const loadingResult = { data: undefined, isLoading: true, isError: false } as ReturnType<typeof useQuery>
    const emptyResult = { data: [], isLoading: false, isError: false } as ReturnType<typeof useQuery>
    vi.mocked(useQuery).mockImplementation(({ queryKey }: { queryKey: unknown[] }) => {
      if (Array.isArray(queryKey) && queryKey[0] === 'payees') {
        return loadingResult
      }
      return emptyResult
    })

    render(<EditarPayeePage />)

    expect(screen.getByText(/carregando/i)).toBeTruthy()
  })

  it('botao cancelar navega para /payees', async () => {
    render(<EditarPayeePage />)

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
    })

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/payees')
  })

  it('exibe titulo Editar Beneficiario', () => {
    render(<EditarPayeePage />)

    expect(screen.getByText('Editar Beneficiario')).toBeTruthy()
  })

  it('campo nome tem maxLength=100', async () => {
    render(<EditarPayeePage />)

    await waitFor(() => {
      const input = screen.getByLabelText(/nome/i) as HTMLInputElement
      expect(input.maxLength).toBe(100)
    })
  })
})
