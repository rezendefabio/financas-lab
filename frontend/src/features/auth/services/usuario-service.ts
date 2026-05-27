import { apiFetch } from '@/services/api-client'

export interface PerfilUsuario {
  id: string
  email: string
  name: string | null
  criadoEm: string
  updatedAt: string | null
}

export interface AtualizarPerfilPayload {
  name: string | null
}

export interface AlterarSenhaPayload {
  senhaAtual: string
  novaSenha: string
}

export const usuarioService = {
  getPerfil: (): Promise<PerfilUsuario> =>
    apiFetch<PerfilUsuario>('/api/perfil'),
  atualizarPerfil: (data: AtualizarPerfilPayload): Promise<PerfilUsuario> =>
    apiFetch<PerfilUsuario>('/api/perfil', {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  alterarSenha: (data: AlterarSenhaPayload): Promise<void> =>
    apiFetch<void>('/api/perfil/senha', {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
}
