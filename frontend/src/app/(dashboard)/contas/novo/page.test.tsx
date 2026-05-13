import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/services/contas.service', () => ({
  contasService: {
    criar: vi.fn(),
  },
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
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

import { contasService } from '@/services/contas.service'
import NovaConta from './page'

describe('NovaConta', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders form fields', () => {
    render(<NovaConta />)
    expect(screen.getByText('Nova Conta')).toBeTruthy()
    expect(screen.getByLabelText(/nome/i)).toBeTruthy()
    expect(screen.getByLabelText(/saldo inicial/i)).toBeTruthy()
    expect(screen.getByRole('button', { name: /salvar/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('submits form with correct values and redirects to /contas', async () => {
    const fakeConta = { id: '1', nome: 'Nubank', tipo: 'CORRENTE' }
    vi.mocked(contasService.criar).mockResolvedValue(fakeConta as never)

    render(<NovaConta />)

    await userEvent.clear(screen.getByLabelText(/nome/i))
    await userEvent.type(screen.getByLabelText(/nome/i), 'Nubank')

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(contasService.criar).toHaveBeenCalledWith(
        expect.objectContaining({
          nome: 'Nubank',
          tipo: 'CORRENTE',
          saldoInicialValor: 0,
          saldoInicialMoeda: 'BRL',
        })
      )
    })

    await waitFor(() => {
      expect(mockInvalidateQueries).toHaveBeenCalledWith({ queryKey: ['contas'] })
      expect(mockPush).toHaveBeenCalledWith('/contas')
    })
  })

  it('shows api error message when criar fails', async () => {
    vi.mocked(contasService.criar).mockRejectedValue(new Error('Network error'))

    render(<NovaConta />)

    await userEvent.type(screen.getByLabelText(/nome/i), 'Falha')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/erro ao criar conta/i)).toBeTruthy()
    })
  })

  it('shows validation error when nome is empty', async () => {
    render(<NovaConta />)

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/nome obrigatorio/i)).toBeTruthy()
    })
  })

  it('navigates to /contas when cancelar is clicked', async () => {
    render(<NovaConta />)

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/contas')
  })
})
