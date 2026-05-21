import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/features/payee/services/payee-service', () => ({
  criarPayee: vi.fn(),
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
  usePathname: () => '/payees/novo',
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
    useQuery: vi.fn(() => ({ data: [] })),
  }
})

import { criarPayee } from '@/features/payee/services/payee-service'
import NovoPayeePage from './page'

describe('NovoPayeePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('exibe campos do formulario', () => {
    render(<NovoPayeePage />)
    expect(screen.getByText('Novo Beneficiario')).toBeTruthy()
    expect(screen.getByLabelText(/nome/i)).toBeTruthy()
    expect(screen.getByRole('button', { name: /salvar/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('exibe label de categoria padrao opcional', () => {
    render(<NovoPayeePage />)
    expect(screen.getByText('Categoria Padrao (opcional)')).toBeTruthy()
  })

  it('submete formulario e navega para /payees', async () => {
    const fakePayee = { id: 'pay-001', nome: 'Supermercado', categoriaPadraoId: undefined }
    vi.mocked(criarPayee).mockResolvedValue(fakePayee as never)

    render(<NovoPayeePage />)

    await userEvent.clear(screen.getByLabelText(/nome/i))
    await userEvent.type(screen.getByLabelText(/nome/i), 'Supermercado')

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(criarPayee).toHaveBeenCalledWith(
        expect.objectContaining({ nome: 'Supermercado' })
      )
    })

    await waitFor(() => {
      expect(mockInvalidateQueries).toHaveBeenCalledWith({ queryKey: ['payees'] })
      expect(mockPush).toHaveBeenCalledWith('/payees')
    })
  })

  it('exibe erro de API quando criar falha', async () => {
    vi.mocked(criarPayee).mockRejectedValue(new Error('Network error'))

    render(<NovoPayeePage />)

    await userEvent.type(screen.getByLabelText(/nome/i), 'Falha')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/erro ao criar beneficiario/i)).toBeTruthy()
    })
  })

  it('exibe erro de validacao quando nome e vazio', async () => {
    render(<NovoPayeePage />)

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/nome obrigatorio/i)).toBeTruthy()
    })
  })

  it('navega para /payees ao clicar em Cancelar', async () => {
    render(<NovoPayeePage />)

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/payees')
  })

  it('campo nome tem maxLength=100', () => {
    render(<NovoPayeePage />)

    const input = screen.getByLabelText(/nome/i) as HTMLInputElement
    expect(input.maxLength).toBe(100)
  })
})
