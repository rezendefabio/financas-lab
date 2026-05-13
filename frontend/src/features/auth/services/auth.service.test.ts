import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { authService } from './auth.service'
import * as authModule from '@/shared/lib/auth'

vi.mock('@/services/api-client', () => ({
  apiFetch: vi.fn(),
}))

import { apiFetch } from '@/services/api-client'

describe('authService', () => {
  beforeEach(() => {
    vi.spyOn(authModule, 'setToken')
    vi.spyOn(authModule, 'clearToken')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('login chama apiFetch e armazena o token', async () => {
    const tokenResponse = { token: 'jwt-token', tipo: 'Bearer', expiresIn: 900 }
    vi.mocked(apiFetch).mockResolvedValue(tokenResponse)

    const result = await authService.login({ email: 'a@b.com', senha: '123' })

    expect(apiFetch).toHaveBeenCalledWith('/api/auth/login', expect.objectContaining({ method: 'POST' }))
    expect(authModule.setToken).toHaveBeenCalledWith('jwt-token')
    expect(result).toEqual(tokenResponse)
  })

  it('logout limpa o token', () => {
    authService.logout()
    expect(authModule.clearToken).toHaveBeenCalled()
  })
})
