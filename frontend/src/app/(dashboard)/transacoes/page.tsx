'use client'
import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { transacoesService } from '@/features/transacoes/services/transacoes.service'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from '@/shared/components/ui/table'
import { formatBRL, formatTipoTransacao, formatDate } from '@/shared/lib/formatters'

function badgeVariant(tipo: string) {
  if (tipo === 'RECEITA') return 'default' as const
  if (tipo === 'DESPESA') return 'destructive' as const
  return 'secondary' as const
}

export default function TransacoesPage() {
  const router = useRouter()
  const { data, isLoading, isError } = useQuery({
    queryKey: ['transacoes'],
    queryFn: () => transacoesService.listar({ size: 20 }),
  })

  const transacoes = data?.content ?? []

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Transacoes</h1>
        <Button onClick={() => router.push('/transacoes/novo')}>Nova Transacao</Button>
      </div>

      {isLoading && (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Descricao</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead>Data</TableHead>
                  <TableHead className="text-right">Valor</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {[1, 2, 3].map((i) => (
                  <TableRow key={i}>
                    <TableCell><Skeleton className="h-4 w-40" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-20" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-28" /></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar transacoes.</p>
      )}

      {!isLoading && !isError && transacoes.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhuma transacao cadastrada.</p>
          <Button onClick={() => router.push('/transacoes/novo')}>Registrar primeira transacao</Button>
        </div>
      )}

      {transacoes.length > 0 && (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Descricao</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead>Data</TableHead>
                  <TableHead className="text-right">Valor</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {transacoes.map((t) => (
                  <TableRow key={t.id}>
                    <TableCell className="font-medium">{t.descricao}</TableCell>
                    <TableCell>
                      <Badge variant={badgeVariant(t.tipo)}>
                        {formatTipoTransacao(t.tipo)}
                      </Badge>
                    </TableCell>
                    <TableCell>{formatDate(t.data)}</TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatBRL(t.valor)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {data && data.totalElements > 20 && (
        <p className="text-xs text-muted-foreground text-center">
          Exibindo 20 de {data.totalElements} transacoes.
        </p>
      )}
    </div>
  )
}
