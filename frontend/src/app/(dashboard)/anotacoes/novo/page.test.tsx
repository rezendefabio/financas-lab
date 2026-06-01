import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/features/anotacoes/services/anotacao-service', () => ({
  anotacaoService: {
    criar: vi.fn(),
  },
}))

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, back: mockBack }),
  usePathname: () => '/anotacoes/novo',
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

import { anotacaoService } from '@/features/anotacoes/services/anotacao-service'
import NovaAnotacao from './page'

describe('NovaAnotacao', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders form fields', () => {
    render(<NovaAnotacao />)
    expect(screen.getByText('Nova Anotacao')).toBeTruthy()
    expect(screen.getByLabelText(/titulo/i)).toBeTruthy()
    expect(screen.getByRole('button', { name: /salvar/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('submits form and redirects to /anotacoes on success', async () => {
    const fakeAnotacao = {
      id: '1',
      titulo: 'Pagar fatura',
      tipo: 'LEMBRETE',
      prioridade: 'MEDIA',
      userId: 'user-1',
      conteudo: null,
      valorMontante: null,
      valorMoeda: null,
      dataReferencia: null,
      criadoEm: '2026-01-01T00:00:00Z',
      atualizadoEm: '2026-01-01T00:00:00Z',
    }
    vi.mocked(anotacaoService.criar).mockResolvedValue(fakeAnotacao as never)

    render(<NovaAnotacao />)

    await userEvent.clear(screen.getByLabelText(/titulo/i))
    await userEvent.type(screen.getByLabelText(/titulo/i), 'Pagar fatura')

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(anotacaoService.criar).toHaveBeenCalledWith(
        expect.objectContaining({
          titulo: 'Pagar fatura',
          tipo: 'LEMBRETE',
          prioridade: 'MEDIA',
        })
      )
    })

    await waitFor(() => {
      expect(mockInvalidateQueries).toHaveBeenCalledWith({ queryKey: ['anotacoes'] })
      expect(mockPush).toHaveBeenCalledWith('/anotacoes')
    })
  })

  it('shows api error message when criar fails', async () => {
    vi.mocked(anotacaoService.criar).mockRejectedValue(new Error('Network error'))

    render(<NovaAnotacao />)

    await userEvent.type(screen.getByLabelText(/titulo/i), 'Falha')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/erro ao criar anotacao/i)).toBeTruthy()
    })
  })

  it('shows validation error when titulo is empty', async () => {
    render(<NovaAnotacao />)

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/titulo obrigatorio/i)).toBeTruthy()
    })
  })

  it('navigates to /anotacoes when cancelar is clicked', async () => {
    render(<NovaAnotacao />)

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/anotacoes')
  })
})
