'use client'
import { useQueries, useQuery } from '@tanstack/react-query'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { orcamentoService } from '@/features/orcamentos/services/orcamento-service'
import type { OrcamentoStatus, Progresso } from '@/features/orcamentos/types/orcamento'
import { formatBRL } from '@/shared/lib/formatters'
import { cn } from '@/shared/lib/utils'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/shared/components/ui/card'

const CORES_STATUS: Record<OrcamentoStatus, string> = {
  ABAIXO: 'bg-green-500',
  ATENCAO: 'bg-yellow-500',
  ATINGIDO: 'bg-orange-500',
  EXCEDIDO: 'bg-red-500',
}

function pad2(valor: number): string {
  return valor.toString().padStart(2, '0')
}

function mesAtualYYYYMM(): string {
  const hoje = new Date()
  return `${hoje.getFullYear()}-${pad2(hoje.getMonth() + 1)}`
}

function OrcamentosProgressoCard() {
  const mesAtual = mesAtualYYYYMM()

  const { data: orcamentos, isLoading: isLoadingOrcamentos } = useQuery({
    queryKey: ['orcamentos-mes-atual'],
    queryFn: () => orcamentoService.listar(),
  })

  const { data: categorias } = useQuery({
    queryKey: ['categorias', 'lookup'],
    queryFn: () => categoriasService.listar(),
  })

  const orcamentosAtivos = (orcamentos ?? []).filter(
    (o) => o.ativo && o.mesAno.substring(0, 7) === mesAtual,
  )

  const progressoQueries = useQueries({
    queries: orcamentosAtivos.map((orcamento) => ({
      queryKey: ['orcamento-progresso', orcamento.id],
      queryFn: () => orcamentoService.progresso(orcamento.id),
      enabled: !!orcamento.id,
    })),
  })

  const nomePorCategoriaId = new Map<string, string>()
  for (const categoria of categorias ?? []) {
    nomePorCategoriaId.set(categoria.id, categoria.nome)
  }

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-base font-semibold">Orcamentos do Mes</CardTitle>
      </CardHeader>
      <CardContent>
        {isLoadingOrcamentos ? (
          <div className="space-y-3">
            {[0, 1, 2].map((idx) => (
              <div key={idx} className="h-12 animate-pulse rounded bg-muted" />
            ))}
          </div>
        ) : orcamentosAtivos.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            Nenhum orcamento cadastrado para este mes.
          </p>
        ) : (
          <ul className="space-y-4">
            {orcamentosAtivos.map((orcamento, index) => {
              const progressoQuery = progressoQueries[index]
              const progresso: Progresso | undefined = progressoQuery?.data
              const nomeCategoria =
                nomePorCategoriaId.get(orcamento.categoriaId) ?? 'Categoria'

              if (!progresso) {
                return (
                  <li key={orcamento.id} className="space-y-2">
                    <p className="text-sm font-medium">{nomeCategoria}</p>
                    <div className="h-3 animate-pulse rounded bg-muted" />
                  </li>
                )
              }

              const larguraVisual = Math.min(progresso.percentualUtilizado, 100)
              const corBarra = CORES_STATUS[progresso.status]

              return (
                <li key={orcamento.id} className="space-y-1">
                  <div className="flex items-center justify-between text-sm">
                    <span className="font-medium">{nomeCategoria}</span>
                    <span className="tabular-nums text-muted-foreground">
                      {progresso.percentualUtilizado.toFixed(0)}%
                    </span>
                  </div>
                  <div
                    className="h-3 w-full overflow-hidden rounded bg-muted"
                    role="progressbar"
                    aria-valuemin={0}
                    aria-valuemax={100}
                    aria-valuenow={Math.round(larguraVisual)}
                  >
                    <div
                      className={cn('h-full transition-all', corBarra)}
                      style={{ width: `${larguraVisual}%` }}
                    />
                  </div>
                  <p className="text-xs tabular-nums text-muted-foreground">
                    {formatBRL(progresso.totalGasto.valor)} /{' '}
                    {formatBRL(progresso.valorLimite.valor)}
                  </p>
                </li>
              )
            })}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

export { OrcamentosProgressoCard }
export default OrcamentosProgressoCard
