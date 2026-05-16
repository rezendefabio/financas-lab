'use client'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { contasService } from '@/features/contas/services/contas.service'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { cn } from '@/shared/lib/utils'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { formatBRL, formatTipoConta } from '@/shared/lib/formatters'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'
import type { Conta } from '@/features/contas/types/conta'

function ContaCard({ conta, onClick }: { conta: Conta; onClick: () => void }) {
  return (
    <Card
      className={cn(
        "cursor-pointer transition-colors hover:bg-muted/50 border-l-4",
        conta.ativa ? "border-l-primary" : "border-l-border"
      )}
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
        <p className="text-sm text-muted-foreground">{formatTipoConta(conta.tipo)}</p>
        <p className="text-xl font-bold tabular-nums mt-2">
          {formatBRL(conta.saldoAtualValor ?? conta.saldoInicialValor)}
        </p>
        <p className="text-xs text-muted-foreground">saldo atual</p>
      </CardContent>
    </Card>
  )
}

export default function ContasPage() {
  const router = useRouter()
  const [filtroAtiva, setFiltroAtiva] = useState<boolean | undefined>(undefined)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['contas', filtroAtiva],
    queryFn: () => contasService.listar(filtroAtiva),
  })

  const { data: saldoTotal } = useQuery({
    queryKey: ['contas-saldo-total'],
    queryFn: contasService.saldoTotal,
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Contas</h1>
        <div className="flex items-center gap-3">
          <Select
            value={filtroAtiva === undefined ? 'todas' : String(filtroAtiva)}
            onValueChange={(v) => setFiltroAtiva(v === 'todas' ? undefined : v === 'true')}
          >
            <SelectTrigger className="w-36">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="todas">Todas</SelectItem>
              <SelectItem value="true">Ativas</SelectItem>
              <SelectItem value="false">Inativas</SelectItem>
            </SelectContent>
          </Select>
          <Button onClick={() => router.push('/contas/novo')}>Nova Conta</Button>
        </div>
      </div>

      {saldoTotal && (
        <Card className="bg-primary/5 border-primary/20">
          <CardContent className="pt-4 pb-4">
            <p className="text-sm text-muted-foreground">Saldo total</p>
            <p className="text-2xl font-bold tabular-nums">
              {formatBRL(saldoTotal.valor)}
            </p>
            <p className="text-xs text-muted-foreground">{saldoTotal.totalContas} conta(s)</p>
          </CardContent>
        </Card>
      )}

      {isLoading && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
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
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
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
