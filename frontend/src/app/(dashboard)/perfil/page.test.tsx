import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/features/auth', () => ({
  usuarioService: {
    getPerfil: vi.fn(),
    atualizarPerfil: vi.fn(),
    alterarSenha: vi.fn(),
  },
}))

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), back: vi.fn() }),
  usePathname: () => '/perfil',
}))

const mockInvalidateQueries = vi.fn()
const queryStates = new Map<string, { data: unknown; isLoading: boolean; isError: boolean }>()

vi.mock('@tanstack/react-query', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-query')>()
  return {
    ...actual,
    useQuery: vi.fn(({ queryKey }) => {
      const key = JSON.stringify(queryKey)
      return queryStates.get(key) ?? { data: undefined, isLoading: false, isError: false }
    }),
    useMutation: vi.fn(({ mutationFn, onSuccess, onError }) => ({
      mutate: vi.fn(async (data: unknown) => {
        try {
          const result = await mutationFn(data)
          if (onSuccess) await onSuccess(result)
        } catch (err) {
          if (onError) onError(err)
        }
      }),
      isPending: false,
    })),
    useQueryClient: () => ({ invalidateQueries: mockInvalidateQueries }),
  }
})

const mockToast = { success: vi.fn(), error: vi.fn() }
vi.mock('sonner', () => ({
  toast: {
    success: (...args: unknown[]) => mockToast.success(...args),
    error: (...args: unknown[]) => mockToast.error(...args),
  },
}))

import { usuarioService } from '@/features/auth'
import { ApiError } from '@/shared/types/api'
import PerfilPage from './page'

describe('PerfilPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    queryStates.clear()
    queryStates.set(JSON.stringify(['perfil']), {
      data: {
        id: '1',
        email: 'fabio@example.com',
        name: 'Fabio',
        criadoEm: '2026-01-01T00:00:00Z',
        updatedAt: null,
      },
      isLoading: false,
      isError: false,
    })
  })

  it('renderiza secoes de perfil e senha', () => {
    render(<PerfilPage />)
    expect(screen.getByText('Meu Perfil')).toBeTruthy()
    expect(screen.getByText('Dados do perfil')).toBeTruthy()
    expect(screen.getByRole('heading', { name: /alterar senha/i })).toBeTruthy()
    expect(screen.getByLabelText(/nome/i)).toBeTruthy()
    expect(screen.getByLabelText(/e-mail/i)).toBeTruthy()
    expect(screen.getByLabelText(/senha atual/i)).toBeTruthy()
    expect(screen.getByLabelText(/nova senha/i)).toBeTruthy()
  })

  it('exibe email do perfil em modo readonly', () => {
    render(<PerfilPage />)
    const emailInput = screen.getByLabelText(/e-mail/i) as HTMLInputElement
    expect(emailInput.value).toBe('fabio@example.com')
    expect(emailInput.readOnly || emailInput.disabled).toBe(true)
  })

  it('atualiza perfil com nome trimado e exibe toast', async () => {
    vi.mocked(usuarioService.atualizarPerfil).mockResolvedValue({
      id: '1',
      email: 'fabio@example.com',
      name: 'Novo Nome',
      criadoEm: '2026-01-01T00:00:00Z',
      updatedAt: '2026-05-27T00:00:00Z',
    })

    render(<PerfilPage />)
    const input = screen.getByLabelText(/nome/i)
    await userEvent.clear(input)
    await userEvent.type(input, '  Novo Nome  ')
    await userEvent.click(screen.getByRole('button', { name: /^salvar$/i }))

    await waitFor(() => {
      expect(usuarioService.atualizarPerfil).toHaveBeenCalledWith({ name: 'Novo Nome' })
    })
    await waitFor(() => {
      expect(mockToast.success).toHaveBeenCalled()
    })
  })

  it('envia null quando nome esta vazio', async () => {
    vi.mocked(usuarioService.atualizarPerfil).mockResolvedValue({
      id: '1',
      email: 'fabio@example.com',
      name: null,
      criadoEm: '2026-01-01T00:00:00Z',
      updatedAt: '2026-05-27T00:00:00Z',
    })

    render(<PerfilPage />)
    const input = screen.getByLabelText(/nome/i)
    await userEvent.clear(input)
    await userEvent.click(screen.getByRole('button', { name: /^salvar$/i }))

    await waitFor(() => {
      expect(usuarioService.atualizarPerfil).toHaveBeenCalledWith({ name: null })
    })
  })

  it('valida nova senha minima de 6 caracteres', async () => {
    render(<PerfilPage />)
    await userEvent.type(screen.getByLabelText(/senha atual/i), 'senha123')
    await userEvent.type(screen.getByLabelText(/nova senha/i), 'abc')
    await userEvent.click(screen.getByRole('button', { name: /alterar senha/i }))

    await waitFor(() => {
      expect(screen.getByText(/minimo 6 caracteres/i)).toBeTruthy()
    })
    expect(usuarioService.alterarSenha).not.toHaveBeenCalled()
  })

  it('altera senha com sucesso e limpa campos', async () => {
    vi.mocked(usuarioService.alterarSenha).mockResolvedValue(undefined)

    render(<PerfilPage />)
    await userEvent.type(screen.getByLabelText(/senha atual/i), 'senha123')
    await userEvent.type(screen.getByLabelText(/nova senha/i), 'novaSenha123')
    await userEvent.click(screen.getByRole('button', { name: /alterar senha/i }))

    await waitFor(() => {
      expect(usuarioService.alterarSenha).toHaveBeenCalledWith({
        senhaAtual: 'senha123',
        novaSenha: 'novaSenha123',
      })
    })
    await waitFor(() => {
      expect(mockToast.success).toHaveBeenCalled()
    })
  })

  it('exibe toast de erro especifico para 422 (senha atual incorreta)', async () => {
    vi.mocked(usuarioService.alterarSenha).mockRejectedValue(
      new ApiError(422, 'Senha atual incorreta'),
    )

    render(<PerfilPage />)
    await userEvent.type(screen.getByLabelText(/senha atual/i), 'errada')
    await userEvent.type(screen.getByLabelText(/nova senha/i), 'novaSenha123')
    await userEvent.click(screen.getByRole('button', { name: /alterar senha/i }))

    await waitFor(() => {
      expect(mockToast.error).toHaveBeenCalledWith('Senha atual incorreta.')
    })
  })
})
