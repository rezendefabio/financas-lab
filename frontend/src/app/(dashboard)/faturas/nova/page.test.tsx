import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/features/fatura', () => ({
  criarFatura: vi.fn(),
}))

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
  usePathname: () => '/faturas/nova',
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

import NovaFaturaPage from './page'
import { criarFatura } from '@/features/fatura'

const contaId = '3fa85f64-5717-4562-b3fc-2c963f66afa6'

describe('NovaFaturaPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('renderiza campos do formulario', () => {
    render(<NovaFaturaPage />)

    expect(screen.getByText('Nova Fatura')).toBeTruthy()
    expect(screen.getByLabelText(/nome/i)).toBeTruthy()
    expect(screen.getByLabelText('Conta')).toBeTruthy()
    expect(screen.getByLabelText(/data de vencimento/i)).toBeTruthy()
    expect(screen.getByLabelText(/data de fechamento/i)).toBeTruthy()
    expect(screen.getByLabelText(/valor total/i)).toBeTruthy()
    expect(screen.getByRole('button', { name: /salvar/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('envia formulario valido chamando criarFatura e redireciona para /faturas', async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    vi.mocked(criarFatura).mockResolvedValue({} as any)

    render(<NovaFaturaPage />)

    await userEvent.type(screen.getByLabelText(/nome/i), 'Cartao Maio')
    fireEvent.change(screen.getByLabelText('Conta'), { target: { value: contaId } })
    fireEvent.change(screen.getByLabelText(/data de vencimento/i), {
      target: { value: '2026-06-10' },
    })

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(criarFatura).toHaveBeenCalledWith(
        expect.objectContaining({
          nome: 'Cartao Maio',
          contaId,
          dataVencimento: '2026-06-10',
        }),
      )
    })

    await waitFor(() => {
      expect(mockInvalidateQueries).toHaveBeenCalledWith({ queryKey: ['faturas'] })
      expect(mockPush).toHaveBeenCalledWith('/faturas')
    })
  })

  it('exibe erro de validacao quando nome esta vazio', async () => {
    render(<NovaFaturaPage />)

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/nome obrigatorio/i)).toBeTruthy()
    })
    expect(criarFatura).not.toHaveBeenCalled()
  })

  it('exibe mensagem de erro de API quando criarFatura falha', async () => {
    vi.mocked(criarFatura).mockRejectedValue(new Error('Network error'))

    render(<NovaFaturaPage />)

    await userEvent.type(screen.getByLabelText(/nome/i), 'Cartao Maio')
    fireEvent.change(screen.getByLabelText('Conta'), { target: { value: contaId } })
    fireEvent.change(screen.getByLabelText(/data de vencimento/i), {
      target: { value: '2026-06-10' },
    })

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/erro ao criar fatura/i)).toBeTruthy()
    })
  })

  it('navega para /faturas ao clicar em Cancelar', async () => {
    render(<NovaFaturaPage />)

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/faturas')
  })

  it('chama router.back() ao clicar no botao Voltar', async () => {
    render(<NovaFaturaPage />)

    await userEvent.click(screen.getByRole('button', { name: /voltar/i }))

    expect(mockBack).toHaveBeenCalled()
  })
})
