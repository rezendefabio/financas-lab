// Interfaces geradas a partir dos DTOs Java de Assinatura

export type TipoAssinatura = 'STREAMING' | 'SOFTWARE' | 'ACADEMIA' | 'OUTROS'

export interface ValorMonetario {
  valor: number
  moeda: string
}

// Interface principal -- campos inferidos de AssinaturaResponse.java
export interface Assinatura {
  id: string
  userId: string
  nome: string
  tipo: TipoAssinatura
  valorMensal: ValorMonetario
  dataRenovacao: string
  ativa: boolean
  criadoEm: string
  atualizadoEm: string
}

// Payload de criacao -- campos inferidos de CriarAssinaturaRequest.java
export interface CriarAssinaturaPayload {
  nome: string
  tipo: TipoAssinatura
  valorMensal: number
  moeda: string
  dataRenovacao: string
}

// Payload de atualizacao -- campos inferidos de AtualizarAssinaturaRequest.java
export interface AtualizarAssinaturaPayload {
  nome: string
  tipo: TipoAssinatura
  valorMensal: number
  moeda: string
  dataRenovacao: string
  ativa: boolean
}
