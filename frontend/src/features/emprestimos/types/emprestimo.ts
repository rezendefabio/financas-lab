// Interfaces geradas a partir dos DTOs Java de Emprestimo.

export type TipoEmprestimo = 'CONCEDIDO' | 'RECEBIDO'

export interface ValorMonetario {
  valor: number
  moeda: string
}

// Interface principal -- campos inferidos de EmprestimoResponse.java
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

// Payload de criacao -- campos inferidos de CriarEmprestimoRequest.java
export interface CriarEmprestimoPayload {
  descricao: string
  nomeTerceiro?: string
  tipo: TipoEmprestimo
  valor: number
  moeda: string
  dataEmprestimo: string
}

// Payload de atualizacao -- campos inferidos de AtualizarEmprestimoRequest.java
export interface AtualizarEmprestimoPayload {
  descricao: string
  nomeTerceiro?: string
  tipo: TipoEmprestimo
  valor: number
  moeda: string
  dataEmprestimo: string
  quitado: boolean
}
