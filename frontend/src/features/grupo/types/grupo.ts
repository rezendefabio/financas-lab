export interface Grupo {
  id: string
  userId: string
  nome: string
  descricao: string | null
  ativo: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface CriarGrupoPayload {
  nome: string
  descricao?: string
}

export interface AtualizarGrupoPayload {
  nome: string
  descricao?: string
}
