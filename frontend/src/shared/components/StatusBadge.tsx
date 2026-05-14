import { Badge } from '@/shared/components/ui/badge'

export type StatusConfig = {
  label: string
  variant: 'default' | 'secondary' | 'outline' | 'destructive'
}

export interface StatusBadgeProps {
  status: string
  config: Record<string, StatusConfig>
  fallbackLabel?: string
}

export const ORCAMENTO_STATUS_CONFIG: Record<string, StatusConfig> = {
  ABAIXO: { label: 'Abaixo', variant: 'default' },
  ATENCAO: { label: 'Atencao', variant: 'secondary' },
  ATINGIDO: { label: 'Atingido', variant: 'outline' },
  EXCEDIDO: { label: 'Excedido', variant: 'destructive' },
}

export const META_STATUS_CONFIG: Record<string, StatusConfig> = {
  EM_ANDAMENTO: { label: 'Em andamento', variant: 'default' },
  CONCLUIDA: { label: 'Concluida', variant: 'secondary' },
  CANCELADA: { label: 'Cancelada', variant: 'outline' },
}

export const CONTA_ATIVA_CONFIG: Record<string, StatusConfig> = {
  'true': { label: 'Ativa', variant: 'default' },
  'false': { label: 'Inativa', variant: 'secondary' },
}

function StatusBadge({ status, config, fallbackLabel }: StatusBadgeProps) {
  const entry = config[status]
  const label = entry ? entry.label : (fallbackLabel ?? status)
  const variant = entry ? entry.variant : 'secondary'

  return <Badge variant={variant}>{label}</Badge>
}

export { StatusBadge }
export default StatusBadge
