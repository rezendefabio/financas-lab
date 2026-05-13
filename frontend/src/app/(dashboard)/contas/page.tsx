'use client'
import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { contasService } from '@/services/contas.service'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import type { Conta } from '@/types/conta'

function formatBRL(valor: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor)
}

const TIPO_LABEL: Record<string, string> = {
  CORRENTE: 'Corrente',
  POUPANCA: 'Poupanca',
  DINHEIRO: 'Dinheiro',
  CARTAO_CREDITO: 'Cartao de Credito',
}

function ContaCard({ conta, onClick }: { conta: Conta; onClick: () => void }) {
  return (
    <Card
      className="cursor-pointer hover:bg-muted/50 transition-colors"
      onClick={onClick}
    >
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base">{conta.nome}</CardTitle>
          <Badge variant={conta.ativa ? 'default' : 'secondary'}>
            {conta.ativa ? 'Ativa' : 'Inativa'}
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">{TIPO_LABEL[conta.tipo] ?? conta.tipo}</p>
        <p className="text-lg font-semibold mt-1">
          {formatBRL(conta.saldoInicialValor)}
        </p>
        <p className="text-xs text-muted-foreground">saldo inicial</p>
      </CardContent>
    </Card>
  )
}

export default function ContasPage() {
  const router = useRouter()
  const { data, isLoading, isError } = useQuery({
    queryKey: ['contas'],
    queryFn: contasService.listar,
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Contas</h1>
        <Button onClick={() => router.push('/contas/novo')}>Nova Conta</Button>
      </div>

      {isLoading && (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardHeader>
                <Skeleton className="h-5 w-32" />
              </CardHeader>
              <CardContent className="space-y-2">
                <Skeleton className="h-4 w-20" />
                <Skeleton className="h-6 w-28" />
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar contas.</p>
      )}

      {data && data.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhuma conta cadastrada.</p>
          <Button onClick={() => router.push('/contas/novo')}>Criar primeira conta</Button>
        </div>
      )}

      {data && data.length > 0 && (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {data.map((conta) => (
            <ContaCard
              key={conta.id}
              conta={conta}
              onClick={() => router.push(`/contas/${conta.id}`)}
            />
          ))}
        </div>
      )}
    </div>
  )
}
