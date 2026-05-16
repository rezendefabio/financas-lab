'use client'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter, useParams } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { lancamentoRecorrenteService } from '@/features/lancamentorecorrente'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { formatBRL, formatDate, formatDateTime } from '@/shared/lib/formatters'

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

export default function LancamentoRecorrenteDetalhePage() {
  const router = useRouter()
  const params = useParams()
  const id = params.id as string
  const queryClient = useQueryClient()
  const [confirmandoDesativar, setConfirmandoDesativar] = useState(false)
  const [desativarError, setDesativarError] = useState<string | null>(null)
  const [executarSuccess, setExecutarSuccess] = useState(false)
  const [executarError, setExecutarError] = useState<string | null>(null)

  const { data: lancamento, isLoading, isError } = useQuery({
    queryKey: ['lancamento-recorrente', id],
    queryFn: () => lancamentoRecorrenteService.buscar(id),
  })

  const desativarMutation = useMutation({
    mutationFn: () => lancamentoRecorrenteService.desativar(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['lancamentos-recorrentes'] })
      await queryClient.invalidateQueries({ queryKey: ['lancamento-recorrente', id] })
      setConfirmandoDesativar(false)
    },
    onError: () => {
      setDesativarError('Erro ao desativar lancamento recorrente.')
    },
  })

  const executarMutation = useMutation({
    mutationFn: () => lancamentoRecorrenteService.executar(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['lancamento-recorrente', id] })
      setExecutarSuccess(true)
      setExecutarError(null)
    },
    onError: () => {
      setExecutarError('Erro ao executar lancamento recorrente.')
      setExecutarSuccess(false)
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

  if (isError || !lancamento) {
    return (
      <div className="space-y-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.push('/lancamentos-recorrentes')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <p className="text-sm text-destructive">Lancamento recorrente nao encontrado.</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          aria-label="Voltar"
          onClick={() => router.push('/lancamentos-recorrentes')}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Detalhe do Lancamento Recorrente</h1>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Dados do Lancamento</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-muted-foreground">Descricao</p>
              <p className="font-medium">{lancamento.descricao}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Tipo</p>
              <Badge variant={lancamento.tipo === 'RECEITA' ? 'default' : 'secondary'}>
                {TIPO_LABELS[lancamento.tipo] ?? lancamento.tipo}
              </Badge>
            </div>
            <div>
              <p className="text-muted-foreground">Valor</p>
              <p className="font-medium tabular-nums">{formatBRL(lancamento.valor.valor)}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Periodicidade</p>
              <p className="font-medium">
                {PERIODICIDADE_LABELS[lancamento.periodicidade] ?? lancamento.periodicidade}
              </p>
            </div>
            <div>
              <p className="text-muted-foreground">Proxima Ocorrencia</p>
              <p className="font-medium">{formatDate(lancamento.proximaOcorrencia)}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Status</p>
              <Badge variant={lancamento.ativo ? 'default' : 'outline'}>
                {lancamento.ativo ? 'Ativo' : 'Inativo'}
              </Badge>
            </div>
            <div>
              <p className="text-muted-foreground">Criado em</p>
              <p className="font-medium">{formatDateTime(lancamento.criadoEm)}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Atualizado em</p>
              <p className="font-medium">{formatDateTime(lancamento.atualizadoEm)}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {executarSuccess && (
        <p className="text-sm text-green-600">Lancamento executado com sucesso.</p>
      )}
      {executarError && <p className="text-sm text-destructive">{executarError}</p>}

      <div className="flex gap-3 flex-wrap">
        <Button
          variant="outline"
          disabled={executarMutation.isPending}
          onClick={() => {
            setExecutarSuccess(false)
            setExecutarError(null)
            executarMutation.mutate()
          }}
        >
          {executarMutation.isPending ? 'Executando...' : 'Executar agora'}
        </Button>

        {lancamento.ativo && !confirmandoDesativar && (
          <Button
            variant="destructive"
            onClick={() => {
              setDesativarError(null)
              setConfirmandoDesativar(true)
            }}
          >
            Desativar
          </Button>
        )}

        {lancamento.ativo && confirmandoDesativar && (
          <div className="flex items-center gap-3">
            <p className="text-sm text-muted-foreground">Confirmar desativacao?</p>
            <Button
              variant="destructive"
              disabled={desativarMutation.isPending}
              onClick={() => {
                setDesativarError(null)
                desativarMutation.mutate()
              }}
            >
              {desativarMutation.isPending ? 'Desativando...' : 'Confirmar'}
            </Button>
            <Button
              variant="outline"
              onClick={() => setConfirmandoDesativar(false)}
            >
              Cancelar
            </Button>
          </div>
        )}
      </div>

      {desativarError && <p className="text-sm text-destructive">{desativarError}</p>}
    </div>
  )
}
