'use client'
import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { lancamentoRecorrenteService } from '@/features/lancamentorecorrente'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { formatBRL, formatDate } from '@/shared/lib/formatters'

const PERIODICIDADE_LABELS: Record<string, string> = {
  SEMANAL: 'Semanal',
  QUINZENAL: 'Quinzenal',
  MENSAL: 'Mensal',
  BIMESTRAL: 'Bimestral',
  TRIMESTRAL: 'Trimestral',
  SEMESTRAL: 'Semestral',
  ANUAL: 'Anual',
}

const TIPO_LABELS: Record<string, string> = {
  RECEITA: 'Receita',
  DESPESA: 'Despesa',
}

export default function LancamentosRecorrentesPage() {
  const router = useRouter()

  const { data, isLoading, isError } = useQuery({
    queryKey: ['lancamentos-recorrentes'],
    queryFn: lancamentoRecorrenteService.listar,
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Lancamentos Recorrentes</h1>
        <Button onClick={() => router.push('/lancamentos-recorrentes/novo')}>
          + Novo Lancamento Recorrente
        </Button>
      </div>

      {isLoading && (
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardContent className="pt-4 space-y-2">
                <Skeleton className="h-4 w-48" />
                <Skeleton className="h-4 w-32" />
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar lancamentos recorrentes.</p>
      )}

      {data && data.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhum lancamento recorrente cadastrado.</p>
          <Button onClick={() => router.push('/lancamentos-recorrentes/novo')}>
            Criar primeiro lancamento recorrente
          </Button>
        </div>
      )}

      {data && data.length > 0 && (
        <div className="rounded-md border">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b bg-muted/50">
                <th className="px-4 py-3 text-left font-medium">Descricao</th>
                <th className="px-4 py-3 text-left font-medium">Tipo</th>
                <th className="px-4 py-3 text-left font-medium">Periodicidade</th>
                <th className="px-4 py-3 text-left font-medium">Proxima Ocorrencia</th>
                <th className="px-4 py-3 text-right font-medium">Valor</th>
                <th className="px-4 py-3 text-left font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {data.map((item) => (
                <tr
                  key={item.id}
                  className="border-b cursor-pointer hover:bg-muted/50 transition-colors"
                  onClick={() => router.push(`/lancamentos-recorrentes/${item.id}`)}
                >
                  <td className="px-4 py-3">{item.descricao}</td>
                  <td className="px-4 py-3">
                    <Badge variant={item.tipo === 'RECEITA' ? 'default' : 'secondary'}>
                      {TIPO_LABELS[item.tipo] ?? item.tipo}
                    </Badge>
                  </td>
                  <td className="px-4 py-3">{PERIODICIDADE_LABELS[item.periodicidade] ?? item.periodicidade}</td>
                  <td className="px-4 py-3">{formatDate(item.proximaOcorrencia)}</td>
                  <td className="px-4 py-3 text-right tabular-nums">{formatBRL(item.valor.valor)}</td>
                  <td className="px-4 py-3">
                    <Badge variant={item.ativo ? 'default' : 'outline'}>
                      {item.ativo ? 'Ativo' : 'Inativo'}
                    </Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
