import { apiFetch } from './api-client'
import { setToken, clearToken } from '@/lib/auth'

interface LoginRequest { email: string; senha: string }
interface RegistrarRequest { email: string; senha: string }
interface TokenResponse { token: string; tipo: string; expiresIn: number }
interface UsuarioResponse { id: string; email: string; criadoEm: string }

export const authService = {
  async login(data: LoginRequest): Promise<TokenResponse> {
    const res = await apiFetch<TokenResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
    })
    setToken(res.token)
    return res
  },

  async registrar(data: RegistrarRequest): Promise<UsuarioResponse> {
    return apiFetch('/api/auth/registrar', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  },

  logout(): void {
    clearToken()
  },
}
