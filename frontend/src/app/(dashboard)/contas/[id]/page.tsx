'use client'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useRouter } from 'next/navigation'
import { useState } from 'react'
import { contasService } from '@/features/contas/services/contas.service'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'

function formatBRL(valor: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor)
}

const TIPO_LABEL: Record<string, string> = {
  CORRENTE: 'Corrente',
  POUPANCA: 'Poupanca',
  DINHEIRO: 'Dinheiro',
  CARTAO_CREDITO: 'Cartao de Credito',
}

export default function ContaDetalhePage() {
  const { id } = useParams<{ id: string }>()
  const router = useRouter()
  const queryClient = useQueryClient()
  const [confirmando, setConfirmando] = useState(false)

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
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{conta.nome}</h1>
        <Button variant="outline" onClick={() => router.push('/contas')}>Voltar</Button>
      </div>

      <Card className="max-w-lg">
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Detalhes</CardTitle>
            <Badge variant={conta.ativa ? 'default' : 'secondary'}>
              {conta.ativa ? 'Ativa' : 'Inativa'}
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <p className="text-sm text-muted-foreground">Tipo</p>
            <p className="font-medium">{TIPO_LABEL[conta.tipo] ?? conta.tipo}</p>
          </div>

          <div>
            <p className="text-sm text-muted-foreground">Saldo inicial</p>
            <p className="font-medium">{formatBRL(conta.saldoInicialValor)}</p>
          </div>

          <div>
            <p className="text-sm text-muted-foreground">Saldo atual</p>
            {loadingSaldo ? (
              <Skeleton className="h-6 w-28" />
            ) : saldo ? (
              <p className="text-xl font-bold">{formatBRL(saldo.saldoAtual.valor)}</p>
            ) : null}
          </div>

          {conta.ativa && (
            <div className="pt-2">
              {!confirmando ? (
                <Button
                  variant="destructive"
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
        </CardContent>
      </Card>
    </div>
  )
}
