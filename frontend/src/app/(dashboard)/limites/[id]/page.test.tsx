import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/features/limite', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/features/limite')>()
  return {
    ...actual,
    buscarLimite: vi.fn(),
    atualizarLimite: vi.fn(),
  }
})

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, back: mockBack }),
  useParams: () => ({ id: 'abc-123' }),
  usePathname: () => '/limites/abc-123',
}))

const mockInvalidateQueries = vi.fn()
const limiteData = {
  id: 'abc-123',
  userId: 'user-1',
  nome: 'Limite de lazer',
  tipo: 'MENSAL' as const,
  valor: 500,
  ativo: true,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

vi.mock('@tanstack/react-query', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-query')>()
  return {
    ...actual,
    useQuery: vi.fn(() => ({ data: limiteData, isLoading: false, isError: false })),
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
  }
})

import EditarLimitePage from './page'
import { atualizarLimite } from '@/features/limite'

describe('EditarLimitePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('preenche o formulario com os dados carregados', async () => {
    render(<EditarLimitePage />)

    await waitFor(() => {
      expect((screen.getByLabelText(/nome/i) as HTMLInputElement).value).toBe(
        'Limite de lazer',
      )
    })
  })

  it('envia atualizacao chamando atualizarLimite e redireciona', async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.mocked(atualizarLimite).mockResolvedValue({} as any)

    render(<EditarLimitePage />)

    await waitFor(() => {
      expect((screen.getByLabelText(/nome/i) as HTMLInputElement).value).toBe(
        'Limite de lazer',
      )
    })

    const nome = screen.getByLabelText(/nome/i)
    await userEvent.clear(nome)
    await userEvent.type(nome, 'Renomeado')

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(atualizarLimite).toHaveBeenCalledWith(
        'abc-123',
        expect.objectContaining({
          nome: 'Renomeado',
          tipo: 'MENSAL',
          valor: 500,
        }),
      )
    })

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/limites')
    })
  })

  it('navega para /limites ao clicar em Cancelar', async () => {
    render(<EditarLimitePage />)

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/limites')
  })
})
