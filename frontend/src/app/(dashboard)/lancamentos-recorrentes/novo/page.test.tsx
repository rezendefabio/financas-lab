import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/features/lancamentorecorrente', () => ({
  lancamentoRecorrenteService: {
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

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, back: mockBack }),
  usePathname: () => '/lancamentos-recorrentes/novo',
}))

const mockInvalidateQueries = vi.fn()
vi.mock('@tanstack/react-query', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-query')>()
  return {
    ...actual,
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
    useQuery: vi.fn(() => ({ data: undefined, isLoading: false, isError: false })),
    useQueryClient: () => ({ invalidateQueries: mockInvalidateQueries }),
  }
})

import NovoLancamentoRecorrentePage from './page'

describe('NovoLancamentoRecorrentePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe titulo do formulario', () => {
    render(<NovoLancamentoRecorrentePage />)
    expect(screen.getByText('Novo Lancamento Recorrente')).toBeTruthy()
  })

  it('exibe campo descricao', () => {
    render(<NovoLancamentoRecorrentePage />)
    expect(screen.getByLabelText(/descricao/i)).toBeTruthy()
  })

  it('exibe campo valor', () => {
    render(<NovoLancamentoRecorrentePage />)
    expect(screen.getByLabelText(/valor/i)).toBeTruthy()
  })

  it('exibe botao Salvar', () => {
    render(<NovoLancamentoRecorrentePage />)
    expect(screen.getByRole('button', { name: /salvar/i })).toBeTruthy()
  })

  it('exibe botao Cancelar', () => {
    render(<NovoLancamentoRecorrentePage />)
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('exibe erro de validacao quando descricao esta vazia', async () => {
    render(<NovoLancamentoRecorrentePage />)

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/descricao obrigatoria/i)).toBeTruthy()
    })
  })

  it('navega para /lancamentos-recorrentes ao clicar em cancelar', async () => {
    render(<NovoLancamentoRecorrentePage />)

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/lancamentos-recorrentes')
  })

  it('navega para pagina anterior ao clicar no botao voltar', async () => {
    render(<NovoLancamentoRecorrentePage />)

    await userEvent.click(screen.getByRole('button', { name: /voltar/i }))

    expect(mockBack).toHaveBeenCalled()
  })

  it('exibe label Tipo', () => {
    render(<NovoLancamentoRecorrentePage />)
    expect(screen.getByText('Tipo')).toBeTruthy()
  })

  it('exibe label Periodicidade', () => {
    render(<NovoLancamentoRecorrentePage />)
    expect(screen.getByText('Periodicidade')).toBeTruthy()
  })

  it('exibe label Conta', () => {
    render(<NovoLancamentoRecorrentePage />)
    expect(screen.getByText('Conta')).toBeTruthy()
  })

  it('exibe label Proxima Ocorrencia', () => {
    render(<NovoLancamentoRecorrentePage />)
    expect(screen.getByText('Proxima Ocorrencia')).toBeTruthy()
  })
})
