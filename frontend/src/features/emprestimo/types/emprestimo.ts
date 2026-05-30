// Interfaces geradas a partir dos DTOs Java de Emprestimo

export interface ValorMonetario {
  valor: number
  moeda: string
}

export type TipoEmprestimo = 'CONCEDIDO' | 'RECEBIDO'

export interface Emprestimo {
  id: string
  descricao: string
  nomeTerceiro: string | null
  tipo: TipoEmprestimo
  valor: ValorMonetario
  dataEmprestimo: string
  quitado: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface CriarEmprestimoPayload {
  descricao: string
  nomeTerceiro?: string | null
  tipo: TipoEmprestimo
  valor: number
  moeda: string
  dataEmprestimo: string
  quitado: boolean
}

export interface AtualizarEmprestimoPayload {
  descricao: string
  nomeTerceiro?: string | null
  tipo: TipoEmprestimo
  valor: number
  moeda: string
  dataEmprestimo: string
  quitado: boolean
}
