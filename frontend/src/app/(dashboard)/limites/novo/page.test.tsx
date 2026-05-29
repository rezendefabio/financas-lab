import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/features/limite', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/features/limite')>()
  return {
    ...actual,
    criarLimite: vi.fn(),
  }
})

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, back: mockBack }),
  usePathname: () => '/limites/novo',
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
  }
})

import NovoLimitePage from './page'
import { criarLimite } from '@/features/limite'

describe('NovoLimitePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('renderiza campos do formulario', () => {
    render(<NovoLimitePage />)

    expect(screen.getByText('Novo Limite')).toBeTruthy()
    expect(screen.getByLabelText(/nome/i)).toBeTruthy()
    expect(screen.getByLabelText('Tipo')).toBeTruthy()
    expect(screen.getByLabelText('Valor')).toBeTruthy()
    expect(screen.getByRole('button', { name: /salvar/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('envia formulario valido chamando criarLimite e redireciona para /limites', async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.mocked(criarLimite).mockResolvedValue({} as any)

    render(<NovoLimitePage />)

    await userEvent.type(screen.getByLabelText(/nome/i), 'Limite de lazer')
    await userEvent.type(screen.getByLabelText('Valor'), '500')

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(criarLimite).toHaveBeenCalledWith(
        expect.objectContaining({
          nome: 'Limite de lazer',
          tipo: 'MENSAL',
          valor: 500,
        }),
      )
    })

    await waitFor(() => {
      expect(mockInvalidateQueries).toHaveBeenCalledWith({ queryKey: ['limites'] })
      expect(mockPush).toHaveBeenCalledWith('/limites')
    })
  })

  it('exibe erro de validacao quando nome esta vazio', async () => {
    render(<NovoLimitePage />)

    await userEvent.type(screen.getByLabelText('Valor'), '500')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/nome obrigatorio/i)).toBeTruthy()
    })
    expect(criarLimite).not.toHaveBeenCalled()
  })

  it('exibe erro de validacao quando valor nao e positivo', async () => {
    render(<NovoLimitePage />)

    await userEvent.type(screen.getByLabelText(/nome/i), 'Limite de lazer')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/valor deve ser positivo/i)).toBeTruthy()
    })
    expect(criarLimite).not.toHaveBeenCalled()
  })

  it('exibe mensagem de erro de API quando criarLimite falha', async () => {
    vi.mocked(criarLimite).mockRejectedValue(new Error('Network error'))

    render(<NovoLimitePage />)

    await userEvent.type(screen.getByLabelText(/nome/i), 'Limite de lazer')
    await userEvent.type(screen.getByLabelText('Valor'), '500')

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/erro ao criar limite/i)).toBeTruthy()
    })
  })

  it('navega para /limites ao clicar em Cancelar', async () => {
    render(<NovoLimitePage />)

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/limites')
  })
})
