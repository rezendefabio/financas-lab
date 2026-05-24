'use client'
import { Fragment, useEffect, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { incidenteService } from '@/features/incidente'
import type { FiltrosIncidente } from '@/features/incidente'
import { ApiError } from '@/shared/types/api'
import { formatDateTime } from '@/shared/lib/formatters'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Label } from '@/shared/components/ui/label'
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from '@/shared/components/ui/table'

export default function IncidentesPage() {
  const searchParams = useSearchParams()
  const router = useRouter()

  // Ler filtros da URL (estado inicial vem da URL ao montar)
  const dataInicio = searchParams.get('dataInicio') ?? ''
  const dataFim = searchParams.get('dataFim') ?? ''
  const classeErro = searchParams.get('classeErro') ?? ''
  const operacao = searchParams.get('operacao') ?? ''
  const codigo = searchParams.get('codigo') ?? ''
  const submitted = searchParams.get('submitted') === '1'

  // Estado local APENAS para os inputs controlados (sincroniza com URL ao submeter)
  const [inputDataInicio, setInputDataInicio] = useState(dataInicio)
  const [inputDataFim, setInputDataFim] = useState(dataFim)
  const [inputClasseErro, setInputClasseErro] = useState(classeErro)
  const [inputOperacao, setInputOperacao] = useState(operacao)
  const [inputCodigo, setInputCodigo] = useState(codigo)
  const [expandidoId, setExpandidoId] = useState<string | null>(null)

  // Sincronizar inputs quando URL muda (ex: ao voltar para a aba). A URL e a
  // fonte da verdade -- ao restaurar a aba, os inputs locais precisam refletir
  // os params atuais. setState aqui e proposital (sync URL -> inputs).
  useEffect(() => {
    /* eslint-disable react-hooks/set-state-in-effect */
    setInputDataInicio(dataInicio)
    setInputDataFim(dataFim)
    setInputClasseErro(classeErro)
    setInputOperacao(operacao)
    setInputCodigo(codigo)
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [dataInicio, dataFim, classeErro, operacao, codigo])

  const {
    data: resultadosFiltros,
    isLoading: loadingFiltros,
    error: erroFiltros,
  } = useQuery({
    queryKey: ['incidentes-filtros', dataInicio, dataFim, classeErro, operacao],
    queryFn: () => {
      const filtros: FiltrosIncidente = {}
      if (dataInicio) filtros.criadoApartirDe = new Date(dataInicio).toISOString()
      if (dataFim) filtros.criadoAte = new Date(dataFim).toISOString()
      if (classeErro) filtros.classeErro = classeErro
      if (operacao) filtros.operacao = operacao
      return incidenteService.listar(filtros)
    },
    enabled: submitted && !codigo,
  })

  const {
    data: resultadoCodigo,
    isLoading: loadingCodigo,
    error: erroCodigo,
  } = useQuery({
    queryKey: ['incidente-codigo', codigo],
    queryFn: () => incidenteService.buscarPorCodigo(codigo),
    enabled: submitted && !!codigo,
    retry: false,
  })

  function handleBuscarFiltros(e: React.FormEvent) {
    e.preventDefault()
    const params = new URLSearchParams()
    if (inputDataInicio) params.set('dataInicio', inputDataInicio)
    if (inputDataFim) params.set('dataFim', inputDataFim)
    if (inputClasseErro.trim()) params.set('classeErro', inputClasseErro.trim())
    if (inputOperacao.trim()) params.set('operacao', inputOperacao.trim())
    params.set('submitted', '1')
    setExpandidoId(null)
    router.push(`/incidentes?${params.toString()}`)
  }

  function handleBuscarCodigo(e: React.FormEvent) {
    e.preventDefault()
    if (!inputCodigo.trim()) return
    const params = new URLSearchParams()
    params.set('codigo', inputCodigo.trim())
    params.set('submitted', '1')
    setExpandidoId(null)
    router.push(`/incidentes?${params.toString()}`)
  }

  function handleLimpar() {
    setInputDataInicio('')
    setInputDataFim('')
    setInputClasseErro('')
    setInputOperacao('')
    setInputCodigo('')
    setExpandidoId(null)
    router.push('/incidentes')
  }

  function alternarDetalhe(id: string) {
    setExpandidoId((atual) => (atual === id ? null : id))
  }

  const carregando = loadingFiltros || loadingCodigo
  const resultados = codigo
    ? resultadoCodigo
      ? [resultadoCodigo]
      : null
    : (resultadosFiltros ?? null)
  const erroQuery = erroFiltros || erroCodigo
  const erroMsg = erroQuery
    ? erroQuery instanceof ApiError && erroQuery.status === 404
      ? 'Codigo nao encontrado.'
      : erroQuery instanceof Error
        ? erroQuery.message
        : 'Erro ao buscar.'
    : null

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">Incidentes</h1>
        <p className="text-sm text-muted-foreground">
          Consulte os erros registrados por periodo, classe ou operacao -- ou
          va direto a um incidente pelo codigo ERR-XXXXXXXX.
        </p>
      </div>

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-base font-semibold">Filtros</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleBuscarFiltros} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="filtro-inicio">Data/hora inicio</Label>
                <Input
                  id="filtro-inicio"
                  type="datetime-local"
                  value={inputDataInicio}
                  onChange={(e) => setInputDataInicio(e.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="filtro-fim">Data/hora fim</Label>
                <Input
                  id="filtro-fim"
                  type="datetime-local"
                  value={inputDataFim}
                  onChange={(e) => setInputDataFim(e.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="filtro-classe">Classe do erro</Label>
                <Input
                  id="filtro-classe"
                  placeholder="ex: NullPointerException"
                  value={inputClasseErro}
                  onChange={(e) => setInputClasseErro(e.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="filtro-operacao">Operacao</Label>
                <Input
                  id="filtro-operacao"
                  placeholder="ex: POST /api/transacoes"
                  value={inputOperacao}
                  onChange={(e) => setInputOperacao(e.target.value)}
                />
              </div>
            </div>
            <div className="flex gap-2">
              <Button type="submit" disabled={carregando}>
                {carregando ? 'Buscando...' : 'Buscar'}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={handleLimpar}
                disabled={carregando}
              >
                Limpar filtros
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-base font-semibold">
            Busca rapida por codigo
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form
            onSubmit={handleBuscarCodigo}
            className="flex flex-col gap-3 sm:flex-row sm:items-end"
          >
            <div className="flex-1 space-y-1.5">
              <Label htmlFor="busca-codigo">Codigo</Label>
              <Input
                id="busca-codigo"
                placeholder="ERR-XXXXXXXX"
                value={inputCodigo}
                onChange={(e) => setInputCodigo(e.target.value)}
              />
            </div>
            <Button type="submit" disabled={!inputCodigo.trim() || carregando}>
              Ir para incidente
            </Button>
          </form>
        </CardContent>
      </Card>

      {erroMsg && <p className="text-sm text-destructive">{erroMsg}</p>}

      {!erroMsg && resultados !== null && resultados.length === 0 && (
        <p className="text-sm text-muted-foreground">
          Nenhum incidente encontrado para os filtros informados.
        </p>
      )}

      {!erroMsg && resultados !== null && resultados.length > 0 && (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Codigo</TableHead>
                  <TableHead>Data/hora</TableHead>
                  <TableHead>Operacao</TableHead>
                  <TableHead>Classe do erro</TableHead>
                  <TableHead>Mensagem</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {resultados.map((incidente) => (
                  <Fragment key={incidente.id}>
                    <TableRow
                      className="cursor-pointer"
                      aria-expanded={expandidoId === incidente.id}
                      onClick={() => alternarDetalhe(incidente.id)}
                    >
                      <TableCell className="font-mono">
                        {incidente.codigo}
                      </TableCell>
                      <TableCell>{formatDateTime(incidente.criadoEm)}</TableCell>
                      <TableCell>{incidente.operacao}</TableCell>
                      <TableCell>{incidente.classeErro}</TableCell>
                      <TableCell className="max-w-xs truncate">
                        {incidente.mensagem}
                      </TableCell>
                    </TableRow>
                    {expandidoId === incidente.id && (
                      <TableRow>
                        <TableCell colSpan={5} className="bg-muted/40">
                          <div className="space-y-3 p-2 text-sm">
                            <div className="space-y-0.5">
                              <span className="text-xs font-medium text-muted-foreground">
                                ID
                              </span>
                              <p className="font-mono text-xs text-foreground">
                                {incidente.id}
                              </p>
                            </div>
                            <div className="space-y-0.5">
                              <span className="text-xs font-medium text-muted-foreground">
                                Mensagem
                              </span>
                              <p className="text-foreground">
                                {incidente.mensagem}
                              </p>
                            </div>
                            <div className="space-y-1.5">
                              <span className="text-xs font-medium text-muted-foreground">
                                Stack trace
                              </span>
                              <pre className="max-h-80 overflow-x-auto overflow-y-auto rounded-md bg-muted p-3 font-mono text-xs text-foreground">
                                {incidente.stackTrace}
                              </pre>
                            </div>
                          </div>
                        </TableCell>
                      </TableRow>
                    )}
                  </Fragment>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
