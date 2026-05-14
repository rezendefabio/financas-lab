import { type ReactNode } from 'react'
import { cn } from '@/shared/lib/utils'
import {
  Card,
  CardContent,
  CardHeader,
} from '@/shared/components/ui/card'

interface VariacaoProps {
  valor: string
  positiva: boolean
}

interface StatCardProps {
  titulo: string
  valor: string
  variacao?: VariacaoProps
  descricao?: string
  icone?: ReactNode
  className?: string
}

function StatCard({ titulo, valor, variacao, descricao, icone, className }: StatCardProps) {
  return (
    <Card className={className}>
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between">
          <p className="text-sm text-muted-foreground">{titulo}</p>
          {icone && <div className="text-muted-foreground">{icone}</div>}
        </div>
      </CardHeader>
      <CardContent className="space-y-1">
        <p className="text-2xl font-bold tabular-nums">{valor}</p>
        {variacao && (
          <p
            className={cn(
              'text-sm',
              variacao.positiva ? 'text-emerald-600' : 'text-destructive',
            )}
          >
            {variacao.valor}
          </p>
        )}
        {descricao && (
          <p className="text-xs text-muted-foreground">{descricao}</p>
        )}
      </CardContent>
    </Card>
  )
}

export { StatCard }
export default StatCard
