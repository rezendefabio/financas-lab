'use client'
import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { orcamentoService } from '@/features/orcamentos/services/orcamento-service'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { formatBRL } from '@/shared/lib/formatters'
import type { Orcamento } from '@/features/orcamentos/types/orcamento'
import type { Categoria } from '@/features/categorias/types/categoria'

function formatMesAno(mesAno: string): string {
  // mesAno = "YYYY-MM-01"
  const [year, month] = mesAno.split('-')
  return `${month}/${year}`
}

function OrcamentoRow({
  orcamento,
  categorias,
  onVer,
}: {
  orcamento: Orcamento
  categorias: Categoria[]
  onVer: () => void
}) {
  const categoria = categorias.find((c) => c.id === orcamento.categoriaId)
  return (
    <tr className="border-b last:border-0 hover:bg-muted/30 transition-colors">
      <td className="py-3 px-4 text-sm">{categoria?.nome ?? orcamento.categoriaId}</td>
      <td className="py-3 px-4 text-sm">{formatMesAno(orcamento.mesAno)}</td>
      <td className="py-3 px-4 text-sm text-right tabular-nums">{formatBRL(orcamento.valorLimite.valor)}</td>
      <td className="py-3 px-4 text-sm">
        <Badge variant={orcamento.ativo ? 'default' : 'secondary'}>
          {orcamento.ativo ? 'Ativo' : 'Inativo'}
        </Badge>
      </td>
      <td className="py-3 px-4 text-sm">
        <Button variant="ghost" size="sm" onClick={onVer}>
          Ver
        </Button>
      </td>
    </tr>
  )
}

export default function OrcamentosPage() {
  const router = useRouter()

  const { data: orcamentos, isLoading, isError } = useQuery({
    queryKey: ['orcamentos'],
    queryFn: orcamentoService.listar,
  })

  const { data: categorias = [] } = useQuery({
    queryKey: ['categorias'],
    queryFn: categoriasService.listar,
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Orcamentos</h1>
        <Button onClick={() => router.push('/orcamentos/novo')}>+ Novo Orcamento</Button>
      </div>

      {isLoading && (
        <div className="space-y-2">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      )}

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar orcamentos.</p>
      )}

      {orcamentos && orcamentos.length === 0 && (
        <p className="text-muted-foreground">Nenhum orcamento cadastrado.</p>
      )}

      {orcamentos && orcamentos.length > 0 && (
        <div className="rounded-md border">
          <table className="w-full">
            <thead>
              <tr className="border-b bg-muted/50">
                <th className="py-3 px-4 text-left text-sm font-medium text-muted-foreground">Categoria</th>
                <th className="py-3 px-4 text-left text-sm font-medium text-muted-foreground">Mes/Ano</th>
                <th className="py-3 px-4 text-right text-sm font-medium text-muted-foreground">Limite</th>
                <th className="py-3 px-4 text-left text-sm font-medium text-muted-foreground">Status</th>
                <th className="py-3 px-4 text-left text-sm font-medium text-muted-foreground">Acoes</th>
              </tr>
            </thead>
            <tbody>
              {orcamentos.map((orcamento) => (
                <OrcamentoRow
                  key={orcamento.id}
                  orcamento={orcamento}
                  categorias={categorias}
                  onVer={() => router.push(`/orcamentos/${orcamento.id}`)}
                />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
