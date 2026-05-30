export type PrioridadeLembrete = 'BAIXA' | 'MEDIA' | 'ALTA'

export const PRIORIDADE_LEMBRETE_OPTIONS: { value: PrioridadeLembrete; label: string }[] = [
  { value: 'BAIXA', label: 'Baixa' },
  { value: 'MEDIA', label: 'Media' },
  { value: 'ALTA', label: 'Alta' },
]

export const PRIORIDADE_LEMBRETE_LABEL: Record<PrioridadeLembrete, string> = {
  BAIXA: 'Baixa',
  MEDIA: 'Media',
  ALTA: 'Alta',
}

export interface Lembrete {
  id: string
  userId: string
  titulo: string
  descricao: string | null
  dataLembrete: string
  prioridade: PrioridadeLembrete
  concluido: boolean
  criadoEm: string
  atualizadoEm: string | null
}

export interface CriarLembretePayload {
  titulo: string
  descricao?: string | null
  dataLembrete: string
  prioridade: PrioridadeLembrete
  concluido: boolean
}

export type AtualizarLembretePayload = CriarLembretePayload
