'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { contasService } from '@/features/contas/services/contas.service'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { formatBRL, formatTipoConta } from '@/shared/lib/formatters'

export default function ContaDetalhePage() {
  const { id } = useParams<{ id: string }>()
  const router = useRouter()
  const queryClient = useQueryClient()
  const [confirmando, setConfirmando] = useState(false)
  const [confirmandoExclusao, setConfirmandoExclusao] = useState(false)

  const { data: conta, isLoading: loadingConta, isError: erroConta } = useQuery({
    queryKey: ['conta', id],
    queryFn: () => contasService.buscarPorId(id),
  })

  const { data: saldo, isLoading: loadingSaldo } = useQuery({
    queryKey: ['conta-saldo', id],
    queryFn: () => contasService.calcularSaldo(id),
    enabled: !!conta,
  })

  const desativarMutation = useMutation({
    mutationFn: () => contasService.desativar(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['contas'] })
      router.push('/contas')
    },
  })

  const excluirMutation = useMutation({
    mutationFn: () => contasService.excluir(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['contas'] })
      router.push('/contas')
    },
  })

  if (loadingConta) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <Card>
          <CardContent className="space-y-4 pt-6">
            <Skeleton className="h-5 w-32" />
            <Skeleton className="h-5 w-24" />
            <Skeleton className="h-8 w-36" />
          </CardContent>
        </Card>
      </div>
    )
  }

  if (erroConta || !conta) {
    return (
      <div className="space-y-4">
        <p className="text-sm text-destructive">Conta nao encontrada.</p>
        <Button variant="outline" onClick={() => router.push('/contas')}>Voltar</Button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">{conta.nome}</h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle>Detalhes</CardTitle>
              <Badge variant={conta.ativa ? 'default' : 'secondary'}>
                {conta.ativa ? 'Ativa' : 'Inativa'}
              </Badge>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <dt className="text-sm text-muted-foreground">Nome</dt>
                <dd className="font-medium">{conta.nome}</dd>
              </div>
              <div>
                <dt className="text-sm text-muted-foreground">Tipo</dt>
                <dd className="font-medium">{formatTipoConta(conta.tipo)}</dd>
              </div>
              <div>
                <dt className="text-sm text-muted-foreground">Saldo Atual</dt>
                <dd className="tabular-nums font-semibold text-lg">
                  {loadingSaldo ? (
                    <Skeleton className="h-6 w-28" />
                  ) : saldo ? (
                    formatBRL(saldo.saldoAtual.valor)
                  ) : '—'}
                </dd>
              </div>
              <div>
                <dt className="text-sm text-muted-foreground">Saldo Inicial</dt>
                <dd className="tabular-nums">{formatBRL(conta.saldoInicialValor)}</dd>
              </div>
              {conta.limiteCreditoValor != null && (
                <div>
                  <dt className="text-sm text-muted-foreground">Limite de credito</dt>
                  <dd className="tabular-nums">{formatBRL(conta.limiteCreditoValor)}</dd>
                </div>
              )}
              {conta.diaFechamento != null && (
                <div>
                  <dt className="text-sm text-muted-foreground">Dia de fechamento</dt>
                  <dd>{conta.diaFechamento}</dd>
                </div>
              )}
              {conta.diaVencimento != null && (
                <div>
                  <dt className="text-sm text-muted-foreground">Dia de vencimento</dt>
                  <dd>{conta.diaVencimento}</dd>
                </div>
              )}
            </dl>

            {conta.ativa && (
              <div className="pt-2">
                {!confirmando ? (
                  <Button
                    variant="outline"
                    className="border-destructive text-destructive hover:bg-destructive hover:text-destructive-foreground"
                    onClick={() => setConfirmando(true)}
                  >
                    Desativar conta
                  </Button>
                ) : (
                  <div className="flex gap-2 items-center">
                    <p className="text-sm text-muted-foreground">Confirmar desativacao?</p>
                    <Button
                      variant="destructive"
                      size="sm"
                      disabled={desativarMutation.isPending}
                      onClick={() => desativarMutation.mutate()}
                    >
                      {desativarMutation.isPending ? 'Desativando...' : 'Confirmar'}
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setConfirmando(false)}
                    >
                      Cancelar
                    </Button>
                  </div>
                )}
              </div>
            )}

            <div className="pt-2 border-t">
              {!confirmandoExclusao ? (
                <Button
                  variant="outline"
                  className="border-destructive text-destructive hover:bg-destructive hover:text-destructive-foreground"
                  onClick={() => setConfirmandoExclusao(true)}
                >
                  Excluir conta
                </Button>
              ) : (
                <div className="flex gap-2 items-center">
                  <p className="text-sm text-muted-foreground">Esta acao nao pode ser desfeita. Confirmar exclusao?</p>
                  <Button
                    variant="destructive"
                    size="sm"
                    disabled={excluirMutation.isPending}
                    onClick={() => excluirMutation.mutate()}
                  >
                    {excluirMutation.isPending ? 'Excluindo...' : 'Excluir conta permanentemente'}
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setConfirmandoExclusao(false)}
                  >
                    Cancelar
                  </Button>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
