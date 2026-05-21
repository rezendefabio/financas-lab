import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/features/metas/services/meta-service', () => ({
  metaService: {
    criar: vi.fn(),
  },
}))

const mockPush = vi.fn()
const mockBack = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, back: mockBack }),
  usePathname: () => '/metas/novo',
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

import { metaService } from '@/features/metas/services/meta-service'
import NovaMetaPage from './page'

describe('NovaMetaPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renderiza campos do formulario', () => {
    render(<NovaMetaPage />)
    expect(screen.getByText('Nova Meta')).toBeTruthy()
    expect(screen.getByLabelText(/nome/i)).toBeTruthy()
    expect(screen.getByLabelText(/valor alvo/i)).toBeTruthy()
    expect(screen.getByLabelText(/prazo/i)).toBeTruthy()
    expect(screen.getByRole('button', { name: /salvar/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeTruthy()
  })

  it('envia formulario com valores corretos e redireciona para /metas', async () => {
    const fakeMeta = { id: '1', nome: 'Viagem Europa' }
    vi.mocked(metaService.criar).mockResolvedValue(fakeMeta as never)

    render(<NovaMetaPage />)

    const { fireEvent } = await import('@testing-library/react')

    await userEvent.clear(screen.getByLabelText(/nome/i))
    await userEvent.type(screen.getByLabelText(/nome/i), 'Viagem Europa')

    // Setar valor alvo via fireEvent (valor 0 nao passa validacao positive())
    const valorInput = screen.getByLabelText(/valor alvo/i)
    fireEvent.change(valorInput, { target: { value: '5000' } })

    // Setar prazo via fireEvent para contornar limitacoes do userEvent com input type="date"
    const prazoInput = screen.getByLabelText(/prazo/i)
    fireEvent.change(prazoInput, { target: { value: '2027-12-31' } })

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(metaService.criar).toHaveBeenCalledWith(
        expect.objectContaining({
          nome: 'Viagem Europa',
          valorAlvoMoeda: 'BRL',
          prazo: '2027-12-31',
        })
      )
    })

    await waitFor(() => {
      expect(mockInvalidateQueries).toHaveBeenCalledWith({ queryKey: ['metas'] })
      expect(mockPush).toHaveBeenCalledWith('/metas')
    })
  })

  it('exibe mensagem de erro de API quando criar falha', async () => {
    vi.mocked(metaService.criar).mockRejectedValue(new Error('Network error'))

    render(<NovaMetaPage />)

    const { fireEvent } = await import('@testing-library/react')

    await userEvent.type(screen.getByLabelText(/nome/i), 'Falha')
    fireEvent.change(screen.getByLabelText(/valor alvo/i), { target: { value: '1000' } })
    // Setar prazo valido para passar na validacao de data
    fireEvent.change(screen.getByLabelText(/prazo/i), { target: { value: '2027-12-31' } })
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/erro ao criar meta/i)).toBeTruthy()
    })
  })

  it('exibe erro de validacao quando nome esta vazio', async () => {
    render(<NovaMetaPage />)

    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => {
      expect(screen.getByText(/nome e obrigatorio/i)).toBeTruthy()
    })
  })

  it('navega para /metas ao clicar em Cancelar', async () => {
    render(<NovaMetaPage />)

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(mockPush).toHaveBeenCalledWith('/metas')
  })

  it('chama router.back() ao clicar no botao Voltar', async () => {
    render(<NovaMetaPage />)

    await userEvent.click(screen.getByRole('button', { name: /voltar/i }))

    expect(mockBack).toHaveBeenCalled()
  })
})
