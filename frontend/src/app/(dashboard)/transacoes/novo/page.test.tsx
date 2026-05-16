import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/features/transacoes/services/transacoes.service', () => ({
  transacoesService: {
    criar: vi.fn(),
  },
}))

vi.mock('@/features/contas/services/contas.service', () => ({
  contasService: {
    listar: vi.fn(),
  },
}))

vi.mock('@/features/categorias/services/categorias.service', () => ({
  categoriasService: {
    listar: vi.fn(),
  },
}))

vi.mock('@/features/payee/services/payee-service', () => ({
  listarPayees: vi.fn().mockResolvedValue([]),
}))

vi.mock('@/features/tag/services/tag-service', () => ({
  listarTags: vi.fn().mockResolvedValue([]),
}))

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, back: mockBack }),
}))

const mockInvalidateQueries = vi.fn()
vi.mock('@tanstack/react-query', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-query')>()
  return {
    ...actual,
    useQuery: vi.fn(() => ({ data: undefined, isLoading: false })),
    useMutation: vi.fn(({ mutationFn, onSuccess, onError }: {
      mutationFn: (data: unknown) => Promise<unknown>
      onSuccess: (result: unknown) => Promise<void>
      onError: (err: unknown) => void
    }) => ({
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

import NovaTransacaoPage from './page'

describe('NovaTransacaoPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renderiza o titulo da pagina', () => {
    render(<NovaTransacaoPage />)
    expect(screen.getByText('Nova Transacao')).toBeTruthy()
  })

  it('renderiza campo Valor (R$)', () => {
    render(<NovaTransacaoPage />)
    expect(screen.getByLabelText(/valor/i)).toBeTruthy()
  })

  it('renderiza campo Data', () => {
    render(<NovaTransacaoPage />)
    expect(screen.getByLabelText(/data/i)).toBeTruthy()
  })

  it('renderiza campo Descricao', () => {
    render(<NovaTransacaoPage />)
    expect(screen.getByLabelText(/descricao/i)).toBeTruthy()
  })

  it('renderiza campo Status', () => {
    render(<NovaTransacaoPage />)
    expect(screen.getByText(/^status$/i)).toBeTruthy()
  })

  it('renderiza botao Salvar', () => {
    render(<NovaTransacaoPage />)
    expect(screen.getByRole('button', { name: /salvar/i })).toBeTruthy()
  })

  it('renderiza botao Cancelar', () => {
    render(<NovaTransacaoPage />)
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('navega para /transacoes ao clicar em Cancelar', async () => {
    render(<NovaTransacaoPage />)

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/transacoes')
  })

  it('renderiza botao Voltar com aria-label', () => {
    render(<NovaTransacaoPage />)
    expect(screen.getByRole('button', { name: /voltar/i })).toBeTruthy()
  })
})
