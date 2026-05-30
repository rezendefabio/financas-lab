export type TipoEmprestimo = 'CONCEDIDO' | 'RECEBIDO'

export interface EmprestimoResponse {
  id: string
  descricao: string
  nomeTerceiro: string | null
  tipo: TipoEmprestimo
  valor: { valor: number; moeda: string }
  dataEmprestimo: string
  quitado: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface CriarEmprestimoPayload {
  descricao: string
  nomeTerceiro?: string
  tipo: TipoEmprestimo
  valor: number
  moeda: string
  dataEmprestimo: string
}

export interface AtualizarEmprestimoPayload {
  descricao: string
  nomeTerceiro?: string
  tipo: TipoEmprestimo
  valor: number
  moeda: string
  dataEmprestimo: string
  quitado: boolean
}
