import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import LoginPage from './page'
import { ApiError } from '@/shared/types/api'

vi.mock('@/features/auth/services/auth.service', () => ({
  authService: {
    login: vi.fn(),
  },
}))

vi.mock('@/features/auth/hooks/use-auth', () => ({
  useAuth: () => ({ refresh: vi.fn(), loggedIn: false, logout: vi.fn() }),
}))

const mockPush = vi.fn()
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

import { authService } from '@/features/auth/services/auth.service'

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders email and senha fields', () => {
    render(<LoginPage />)
    expect(screen.getByLabelText(/email/i)).toBeTruthy()
    expect(screen.getByLabelText(/senha/i)).toBeTruthy()
  })

  it('calls authService.login on submit', async () => {
    vi.mocked(authService.login).mockResolvedValue({
      token: 'tok',
      tipo: 'Bearer',
      expiresIn: 3600,
    })

    render(<LoginPage />)
    await userEvent.type(screen.getByLabelText(/email/i), 'test@test.com')
    await userEvent.type(screen.getByLabelText(/senha/i), 'senha123')
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }))

    await waitFor(() => {
      expect(authService.login).toHaveBeenCalledWith({
        email: 'test@test.com',
        senha: 'senha123',
      })
    })
  })

  it('shows error message on 401', async () => {
    vi.mocked(authService.login).mockRejectedValue(new ApiError(401, 'Unauthorized'))

    render(<LoginPage />)
    await userEvent.type(screen.getByLabelText(/email/i), 'test@test.com')
    await userEvent.type(screen.getByLabelText(/senha/i), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }))

    await waitFor(() => {
      expect(screen.getByText(/email ou senha invalidos/i)).toBeTruthy()
    })
  })
})
