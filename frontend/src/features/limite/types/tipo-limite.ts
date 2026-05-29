import type { TipoLimite } from './limite'

export const TIPO_LIMITE_OPTIONS: { value: TipoLimite; label: string }[] = [
  { value: 'DIARIO', label: 'Diario' },
  { value: 'SEMANAL', label: 'Semanal' },
  { value: 'MENSAL', label: 'Mensal' },
  { value: 'ANUAL', label: 'Anual' },
]

export const TIPO_LIMITE_LABEL: Record<TipoLimite, string> = {
  DIARIO: 'Diario',
  SEMANAL: 'Semanal',
  MENSAL: 'Mensal',
  ANUAL: 'Anual',
}
