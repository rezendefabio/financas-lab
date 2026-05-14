'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter, useParams } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { orcamentoService } from '@/features/orcamentos/services/orcamento-service'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { Progress } from '@/shared/components/ui/progress'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { formatBRL, formatDateTime } from '@/shared/lib/formatters'
import type { OrcamentoStatus } from '@/features/orcamentos/types/orcamento'

function formatMesAno(mesAno: string): string {
  const [year, month] = mesAno.split('-')
  return `${month}/${year}`
}

function statusVariant(status: OrcamentoStatus): 'default' | 'secondary' | 'outline' | 'destructive' {
  switch (status) {
    case 'ABAIXO':
      return 'default'
    case 'ATENCAO':
      return 'secondary'
    case 'ATINGIDO':
      return 'outline'
    case 'EXCEDIDO':
      return 'destructive'
    default:
      return 'default'
  }
}

function statusLabel(status: OrcamentoStatus): string {
  switch (status) {
    case 'ABAIXO':
      return 'Abaixo'
    case 'ATENCAO':
      return 'Atencao'
    case 'ATINGIDO':
      return 'Atingido'
    case 'EXCEDIDO':
      return 'Excedido'
    default:
      return status
  }
}

export default function OrcamentoDetalhePage() {
  const router = useRouter()
  const params = useParams()
  const id = params.id as string
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const { data: orcamento, isLoading, isError } = useQuery({
    queryKey: ['orcamento', id],
    queryFn: () => orcamentoService.buscar(id),
  })

  const { data: progresso, isLoading: isLoadingProgresso } = useQuery({
    queryKey: ['orcamento-progresso', id],
    queryFn: () => orcamentoService.progresso(id),
    enabled: !!orcamento,
  })

  const { data: categorias = [] } = useQuery({
    queryKey: ['categorias'],
    queryFn: categoriasService.listar,
  })

  const categoria = categorias.find((c) => c.id === orcamento?.categoriaId)

  const desativarMutation = useMutation({
    mutationFn: () => orcamentoService.desativar(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['orcamentos'] })
      router.push('/orcamentos')
    },
    onError: () => {
      setApiError('Erro ao desativar orcamento.')
    },
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-48 w-full" />
      </div>
    )
  }

  if (isError || !orcamento) {
    return <p className="text-sm text-destructive">Erro ao carregar orcamento.</p>
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.push('/orcamentos')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Detalhe do Orcamento</h1>
      </div>

      {/* Secao 1 - Dados do orcamento */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Dados do Orcamento</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-muted-foreground">Categoria</p>
              <p className="font-medium">{categoria?.nome ?? orcamento.categoriaId}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Mes/Ano</p>
              <p className="font-medium">{formatMesAno(orcamento.mesAno)}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Limite</p>
              <p className="font-medium tabular-nums">{formatBRL(orcamento.valorLimite.valor)}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Status</p>
              <Badge variant={orcamento.ativo ? 'default' : 'secondary'}>
                {orcamento.ativo ? 'Ativo' : 'Inativo'}
              </Badge>
            </div>
            <div>
              <p className="text-muted-foreground">Criado em</p>
              <p className="font-medium">{formatDateTime(orcamento.criadoEm)}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Secao 2 - Progresso do mes */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Progresso do Mes</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {isLoadingProgresso && <Skeleton className="h-20 w-full" />}
          {progresso && (
            <>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-muted-foreground">Gasto</p>
                  <p className="font-medium tabular-nums">{formatBRL(progresso.totalGasto.valor)}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Limite</p>
                  <p className="font-medium tabular-nums">{formatBRL(progresso.valorLimite.valor)}</p>
                </div>
              </div>
              <div className="space-y-1">
                <Progress value={Math.min(progresso.percentualUtilizado, 100)} />
                <div className="flex items-center justify-between text-sm">
                  <span className="text-muted-foreground">
                    {progresso.percentualUtilizado.toFixed(1)}%
                  </span>
                  <Badge variant={statusVariant(progresso.status)}>
                    {statusLabel(progresso.status)}
                  </Badge>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>

      {apiError && <p className="text-sm text-destructive">{apiError}</p>}

      <div className="flex gap-3">
        <Button variant="outline" onClick={() => router.push('/orcamentos')}>
          Voltar
        </Button>
        {orcamento.ativo && (
          <Button
            variant="destructive"
            disabled={desativarMutation.isPending}
            onClick={() => {
              setApiError(null)
              desativarMutation.mutate()
            }}
          >
            {desativarMutation.isPending ? 'Desativando...' : 'Desativar'}
          </Button>
        )}
      </div>
    </div>
  )
}
