'use client'
import { Fragment, useState } from 'react'
import { incidenteService } from '@/features/incidente'
import type { IncidenteResponse, FiltrosIncidente } from '@/features/incidente'
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
  const [dataInicio, setDataInicio] = useState('')
  const [dataFim, setDataFim] = useState('')
  const [classeErro, setClasseErro] = useState('')
  const [operacao, setOperacao] = useState('')
  const [codigo, setCodigo] = useState('')

  const [resultados, setResultados] = useState<IncidenteResponse[] | null>(null)
  const [expandidoId, setExpandidoId] = useState<string | null>(null)
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  async function buscarComFiltros() {
    const filtros: FiltrosIncidente = {}
    if (dataInicio) filtros.criadoApartirDe = new Date(dataInicio).toISOString()
    if (dataFim) filtros.criadoAte = new Date(dataFim).toISOString()
    if (classeErro.trim()) filtros.classeErro = classeErro.trim()
    if (operacao.trim()) filtros.operacao = operacao.trim()

    setCarregando(true)
    setErro(null)
    setExpandidoId(null)
    try {
      const lista = await incidenteService.listar(filtros)
      setResultados(lista)
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Erro ao listar incidentes.')
      setResultados(null)
    } finally {
      setCarregando(false)
    }
  }

  function limparFiltros() {
    setDataInicio('')
    setDataFim('')
    setClasseErro('')
    setOperacao('')
    setResultados(null)
    setExpandidoId(null)
    setErro(null)
  }

  async function buscarPorCodigo() {
    const termo = codigo.trim()
    if (!termo) return

    setCarregando(true)
    setErro(null)
    setExpandidoId(null)
    try {
      const incidente = await incidenteService.buscarPorCodigo(termo)
      setResultados([incidente])
      setExpandidoId(incidente.id)
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        setErro('Codigo nao encontrado.')
      } else {
        setErro(e instanceof Error ? e.message : 'Erro ao buscar o incidente.')
      }
      setResultados(null)
    } finally {
      setCarregando(false)
    }
  }

  function alternarDetalhe(id: string) {
    setExpandidoId((atual) => (atual === id ? null : id))
  }

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
          <form
            onSubmit={(event) => {
              event.preventDefault()
              buscarComFiltros()
            }}
            className="space-y-4"
          >
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="filtro-inicio">Data/hora inicio</Label>
                <Input
                  id="filtro-inicio"
                  type="datetime-local"
                  value={dataInicio}
                  onChange={(e) => setDataInicio(e.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="filtro-fim">Data/hora fim</Label>
                <Input
                  id="filtro-fim"
                  type="datetime-local"
                  value={dataFim}
                  onChange={(e) => setDataFim(e.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="filtro-classe">Classe do erro</Label>
                <Input
                  id="filtro-classe"
                  placeholder="ex: NullPointerException"
                  value={classeErro}
                  onChange={(e) => setClasseErro(e.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="filtro-operacao">Operacao</Label>
                <Input
                  id="filtro-operacao"
                  placeholder="ex: POST /api/transacoes"
                  value={operacao}
                  onChange={(e) => setOperacao(e.target.value)}
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
                onClick={limparFiltros}
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
            onSubmit={(event) => {
              event.preventDefault()
              buscarPorCodigo()
            }}
            className="flex flex-col gap-3 sm:flex-row sm:items-end"
          >
            <div className="flex-1 space-y-1.5">
              <Label htmlFor="busca-codigo">Codigo</Label>
              <Input
                id="busca-codigo"
                placeholder="ERR-XXXXXXXX"
                value={codigo}
                onChange={(e) => setCodigo(e.target.value)}
              />
            </div>
            <Button type="submit" disabled={!codigo.trim() || carregando}>
              Ir para incidente
            </Button>
          </form>
        </CardContent>
      </Card>

      {erro && <p className="text-sm text-destructive">{erro}</p>}

      {resultados !== null && resultados.length === 0 && (
        <p className="text-sm text-muted-foreground">
          Nenhum incidente encontrado para os filtros informados.
        </p>
      )}

      {resultados !== null && resultados.length > 0 && (
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
