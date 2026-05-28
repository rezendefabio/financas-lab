import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/features/carteira', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/features/carteira')>()
  return {
    ...actual,
    criarCarteira: vi.fn(),
  }
})

vi.mock('@/features/contas/services/contas.service', () => ({
  contasService: {
    listar: vi.fn(),
  },
}))

// Base UI Popover (LookupField) nao abre de forma confiavel no JSDOM; substitui
// por um <select> simples que expoe o mesmo contrato value/onChange.
vi.mock('@/shared/components/LookupField', () => ({
  LookupField: ({
    value,
    onChange,
  }: {
    value: string | null
    onChange: (v: string | null) => void
  }) => (
    <select
      aria-label="Conta"
      value={value ?? ''}
      onChange={(e) => onChange(e.target.value || null)}
    >
      <option value="">Selecione a conta</option>
      <option value="3fa85f64-5717-4562-b3fc-2c963f66afa6">Nubank</option>
    </select>
  ),
}))

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, back: mockBack }),
  usePathname: () => '/carteiras/nova',
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

import NovaCarteiraPage from './page'
import { criarCarteira } from '@/features/carteira'

const contaId = '3fa85f64-5717-4562-b3fc-2c963f66afa6'

describe('NovaCarteiraPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('renderiza campos do formulario', () => {
    render(<NovaCarteiraPage />)

    expect(screen.getByText('Nova Carteira')).toBeTruthy()
    expect(screen.getByLabelText(/nome/i)).toBeTruthy()
    expect(screen.getByLabelText('Conta')).toBeTruthy()
    expect(screen.getByLabelText('Tipo')).toBeTruthy()
    expect(screen.getByRole('button', { name: /salvar/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('envia formulario valido chamando criarCarteira e redireciona para /carteiras', async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.mocked(criarCarteira).mockResolvedValue({} as any)

    render(<NovaCarteiraPage />)

    await userEvent.type(screen.getByLabelText(/nome/i), 'Tesouro')
    fireEvent.change(screen.getByLabelText('Conta'), { target: { value: contaId } })

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(criarCarteira).toHaveBeenCalledWith(
        expect.objectContaining({
          nome: 'Tesouro',
          contaId,
          tipo: 'RENDA_FIXA',
        }),
      )
    })

    await waitFor(() => {
      expect(mockInvalidateQueries).toHaveBeenCalledWith({ queryKey: ['carteiras'] })
      expect(mockPush).toHaveBeenCalledWith('/carteiras')
    })
  })

  it('exibe erro de validacao quando nome esta vazio', async () => {
    render(<NovaCarteiraPage />)

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/nome obrigatorio/i)).toBeTruthy()
    })
    expect(criarCarteira).not.toHaveBeenCalled()
  })

  it('exibe mensagem de erro de API quando criarCarteira falha', async () => {
    vi.mocked(criarCarteira).mockRejectedValue(new Error('Network error'))

    render(<NovaCarteiraPage />)

    await userEvent.type(screen.getByLabelText(/nome/i), 'Tesouro')
    fireEvent.change(screen.getByLabelText('Conta'), { target: { value: contaId } })

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/erro ao criar carteira/i)).toBeTruthy()
    })
  })

  it('navega para /carteiras ao clicar em Cancelar', async () => {
    render(<NovaCarteiraPage />)

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/carteiras')
  })
})
