// Interfaces geradas a partir dos DTOs Java de Emprestimo.

export type TipoEmprestimo = 'CONCEDIDO' | 'RECEBIDO'

export interface ValorMonetario {
  valor: number
  moeda: string
}

// Interface principal -- espelha EmprestimoResponse.java (valor aninhado, nao achatado).
export interface Emprestimo {
  id: string
  userId: string
  descricao: string
  nomeTerceiro: string | null
  tipo: TipoEmprestimo
  valor: ValorMonetario
  dataEmprestimo: string
  quitado: boolean
  criadoEm: string
  atualizadoEm: string
}

// Payload de criacao -- espelha CriarEmprestimoRequest.java (valor/moeda planos).
export interface CriarEmprestimoPayload {
  descricao: string
  nomeTerceiro?: string
  tipo: TipoEmprestimo
  valor: number
  moeda: string
  dataEmprestimo: string
}

// Payload de atualizacao -- espelha AtualizarEmprestimoRequest.java (inclui quitado).
export interface AtualizarEmprestimoPayload {
  descricao: string
  nomeTerceiro?: string
  tipo: TipoEmprestimo
  valor: number
  moeda: string
  dataEmprestimo: string
  quitado: boolean
}
