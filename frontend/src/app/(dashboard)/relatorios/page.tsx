'use client'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { contasService } from '@/features/contas/services/contas.service'
import { getFluxoCaixa } from '@/features/dashboard'
import {
  relatorioService,
  GastosPorCategoriaChart,
  EvolucaoSaldoChart,
  FluxoCaixaResumo,
} from '@/features/relatorios'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/shared/components/ui/card'
import { Input } from '@/shared/components/ui/input'
import { Label } from '@/shared/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'
import { Skeleton } from '@/shared/components/ui/skeleton'

const TODAS_CONTAS = '__todas__'

function primeiroDiaDoMes(): string {
  const agora = new Date()
  const ano = agora.getFullYear()
  const mes = String(agora.getMonth() + 1).padStart(2, '0')
  return `${ano}-${mes}-01`
}

function hoje(): string {
  const agora = new Date()
  const ano = agora.getFullYear()
  const mes = String(agora.getMonth() + 1).padStart(2, '0')
  const dia = String(agora.getDate()).padStart(2, '0')
  return `${ano}-${mes}-${dia}`
}

export default function RelatoriosPage() {
  const agora = new Date()
  const [dataInicio, setDataInicio] = useState(primeiroDiaDoMes())
  const [dataFim, setDataFim] = useState(hoje())
  const [contaId, setContaId] = useState<string>(TODAS_CONTAS)
  const [ano, setAno] = useState<number>(agora.getFullYear())
  const [mes, setMes] = useState<number>(agora.getMonth() + 1)

  const contaFiltro = contaId === TODAS_CONTAS ? undefined : contaId

  const { data: contas } = useQuery({
    queryKey: ['contas', 'relatorios'],
    queryFn: () => contasService.listar(),
  })

  const gastos = useQuery({
    queryKey: ['relatorio-gastos', dataInicio, dataFim, contaFiltro],
    queryFn: () => relatorioService.getGastosPorCategoria(dataInicio, dataFim, contaFiltro),
  })

  const evolucao = useQuery({
    queryKey: ['relatorio-evolucao', dataInicio, dataFim, contaFiltro],
    queryFn: () => relatorioService.getEvolucaoSaldo(dataInicio, dataFim, contaFiltro),
  })

  const fluxo = useQuery({
    queryKey: ['relatorio-fluxo-caixa', ano, mes],
    queryFn: () => getFluxoCaixa(ano, mes),
  })

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold tracking-tight">Relatorios</h1>

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-base font-semibold">Filtros</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <div className="space-y-1.5">
              <Label htmlFor="data-inicio">Data inicio</Label>
              <Input
                id="data-inicio"
                type="date"
                value={dataInicio}
                onChange={(e) => setDataInicio(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="data-fim">Data fim</Label>
              <Input
                id="data-fim"
                type="date"
                value={dataFim}
                onChange={(e) => setDataFim(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="conta-filtro">Conta</Label>
              <Select value={contaId} onValueChange={(v) => setContaId(v as string)}>
                <SelectTrigger id="conta-filtro" className="w-full">
                  <SelectValue placeholder="Todas as contas">
                    {(v: string | null) => {
                      if (!v || v === TODAS_CONTAS) return 'Todas as contas'
                      return (contas ?? []).find((c) => c.id === v)?.nome ?? 'Todas as contas'
                    }}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={TODAS_CONTAS}>Todas as contas</SelectItem>
                  {(contas ?? []).map((conta) => (
                    <SelectItem key={conta.id} value={conta.id}>
                      {conta.nome}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      <section className="space-y-2">
        {gastos.isLoading && <Skeleton className="h-80 w-full" />}
        {gastos.isError && (
          <p className="text-sm text-destructive">Erro ao carregar gastos por categoria.</p>
        )}
        {gastos.data && <GastosPorCategoriaChart data={gastos.data} />}
      </section>

      <section className="space-y-2">
        {evolucao.isLoading && <Skeleton className="h-96 w-full" />}
        {evolucao.isError && (
          <p className="text-sm text-destructive">Erro ao carregar evolucao do saldo.</p>
        )}
        {evolucao.data && <EvolucaoSaldoChart data={evolucao.data} />}
      </section>

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-base font-semibold">Fluxo de caixa do mes</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="fluxo-ano">Ano</Label>
              <Input
                id="fluxo-ano"
                type="number"
                value={ano}
                min={2000}
                max={2100}
                onChange={(e) => setAno(Number(e.target.value))}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="fluxo-mes">Mes</Label>
              <Input
                id="fluxo-mes"
                type="number"
                value={mes}
                min={1}
                max={12}
                onChange={(e) => setMes(Number(e.target.value))}
              />
            </div>
          </div>
          {fluxo.isLoading && <Skeleton className="h-40 w-full" />}
          {fluxo.isError && (
            <p className="text-sm text-destructive">Erro ao carregar fluxo de caixa.</p>
          )}
          {fluxo.data && <FluxoCaixaResumo data={fluxo.data} isLoading={fluxo.isLoading} />}
        </CardContent>
      </Card>
    </div>
  )
}
