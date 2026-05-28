import type { TipoCarteira } from './carteira'

export const TIPO_CARTEIRA_OPTIONS: { value: TipoCarteira; label: string }[] = [
  { value: 'RENDA_FIXA', label: 'Renda Fixa' },
  { value: 'RENDA_VARIAVEL', label: 'Renda Variavel' },
  { value: 'CRIPTOMOEDA', label: 'Criptomoeda' },
  { value: 'OUTROS', label: 'Outros' },
]

export const TIPO_CARTEIRA_LABEL: Record<TipoCarteira, string> = {
  RENDA_FIXA: 'Renda Fixa',
  RENDA_VARIAVEL: 'Renda Variavel',
  CRIPTOMOEDA: 'Criptomoeda',
  OUTROS: 'Outros',
}

export const TIPO_CARTEIRA_BADGE_CLASS: Record<TipoCarteira, string> = {
  RENDA_FIXA: 'bg-blue-600 hover:bg-blue-600',
  RENDA_VARIAVEL: 'bg-green-600 hover:bg-green-600',
  CRIPTOMOEDA: 'bg-purple-600 hover:bg-purple-600',
  OUTROS: 'bg-gray-500 hover:bg-gray-500',
}
