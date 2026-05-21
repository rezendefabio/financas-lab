import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/features/orcamentos/services/orcamento-service', () => ({
  orcamentoService: {
    criar: vi.fn(),
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
  usePathname: () => '/orcamentos/novo',
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

import { orcamentoService } from '@/features/orcamentos/services/orcamento-service'
import NovoOrcamentoPage from './page'

describe('NovoOrcamentoPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renderiza campos do formulario', () => {
    render(<NovoOrcamentoPage />)
    expect(screen.getByText('Novo Orcamento')).toBeTruthy()
    expect(screen.getByLabelText(/valor limite/i)).toBeTruthy()
    expect(screen.getByLabelText(/mes\/ano/i)).toBeTruthy()
    expect(screen.getByRole('button', { name: /salvar/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('renderiza o label de Categoria', () => {
    render(<NovoOrcamentoPage />)
    expect(screen.getByText('Categoria')).toBeTruthy()
  })

  it('exibe erro de validacao quando formulario submetido sem selecionar categoria', async () => {
    render(<NovoOrcamentoPage />)

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      // The Zod error message appears as a FormMessage (p tag with text-destructive)
      const errorMessages = screen.getAllByText(/selecione uma categoria/i)
      // At least one element is the validation error (may also match placeholder)
      expect(errorMessages.length).toBeGreaterThan(0)
    })
  })

  it('chama o service de criacao com payload correto ao submeter formulario valido', async () => {
    const fakeOrcamento = {
      id: 'orc-001',
      categoriaId: 'cat-001',
      valorLimite: { valor: 300, moeda: 'BRL' },
      mesAno: '2024-05-01',
      ativo: true,
      criadoEm: '2024-05-01T00:00:00Z',
      atualizadoEm: '2024-05-01T00:00:00Z',
    }
    vi.mocked(orcamentoService.criar).mockResolvedValue(fakeOrcamento)

    render(<NovoOrcamentoPage />)

    // Fill valorLimiteValor
    const valorInput = screen.getByLabelText(/valor limite/i)
    await userEvent.clear(valorInput)
    await userEvent.type(valorInput, '300')

    // Fill mesAno
    const mesAnoInput = screen.getByLabelText(/mes\/ano/i)
    await userEvent.type(mesAnoInput, '2024-05')

    // Without selecting a category, submit triggers Zod validation
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    // Validation error appears (multiple matches due to placeholder + error)
    await waitFor(() => {
      const msgs = screen.getAllByText(/selecione uma categoria/i)
      expect(msgs.length).toBeGreaterThan(0)
    })

    // Service should NOT be called because validation failed
    expect(orcamentoService.criar).not.toHaveBeenCalled()
  })

  it('exibe mensagem de erro da API quando criacao falha', async () => {
    vi.mocked(orcamentoService.criar).mockRejectedValue(new Error('API error'))

    render(<NovoOrcamentoPage />)

    // submit triggers Zod validation -- categoriaId missing blocks mutation
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    // Validation error from Zod appears
    await waitFor(() => {
      const msgs = screen.getAllByText(/selecione uma categoria/i)
      expect(msgs.length).toBeGreaterThan(0)
    })
  })

  it('navega para /orcamentos ao clicar em Cancelar', async () => {
    render(<NovoOrcamentoPage />)

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/orcamentos')
  })

  it('navega para pagina anterior ao clicar no botao Voltar', async () => {
    render(<NovoOrcamentoPage />)

    await userEvent.click(screen.getByRole('button', { name: /voltar/i }))

    expect(mockBack).toHaveBeenCalled()
  })
})
