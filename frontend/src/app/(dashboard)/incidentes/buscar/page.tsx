'use client'
import { useState } from 'react'
import { incidenteService } from '@/features/incidente'
import type { IncidenteResponse } from '@/features/incidente'
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

export default function BuscarIncidentePage() {
  const [codigo, setCodigo] = useState('')
  const [carregando, setCarregando] = useState(false)
  const [incidente, setIncidente] = useState<IncidenteResponse | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const termo = codigo.trim()
    if (!termo) return

    setCarregando(true)
    setIncidente(null)
    setErro(null)

    try {
      const resposta = await incidenteService.buscarPorCodigo(termo)
      setIncidente(resposta)
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        setErro('Codigo nao encontrado.')
      } else {
        setErro(e instanceof Error ? e.message : 'Erro ao buscar o incidente.')
      }
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">
          Consultar Incidente
        </h1>
        <p className="text-sm text-muted-foreground">
          Informe o codigo de erro (ERR-XXXXXXXX) exibido na tela para ver os
          detalhes do incidente.
        </p>
      </div>

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-base font-semibold">
            Codigo de erro
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="codigo-incidente">Codigo</Label>
              <Input
                id="codigo-incidente"
                placeholder="ERR-XXXXXXXX"
                value={codigo}
                onChange={(e) => setCodigo(e.target.value)}
              />
            </div>
            <Button type="submit" disabled={!codigo.trim() || carregando}>
              {carregando ? 'Buscando...' : 'Buscar'}
            </Button>
          </form>
        </CardContent>
      </Card>

      {incidente && (
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base font-semibold">
              {incidente.codigo}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <dl className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-0.5">
                <dt className="text-xs font-medium text-muted-foreground">
                  Operacao
                </dt>
                <dd className="text-sm text-foreground">
                  {incidente.operacao}
                </dd>
              </div>
              <div className="space-y-0.5">
                <dt className="text-xs font-medium text-muted-foreground">
                  Classe do erro
                </dt>
                <dd className="text-sm text-foreground">
                  {incidente.classeErro}
                </dd>
              </div>
              <div className="space-y-0.5">
                <dt className="text-xs font-medium text-muted-foreground">
                  Ocorrido em
                </dt>
                <dd className="text-sm text-foreground">
                  {formatDateTime(incidente.criadoEm)}
                </dd>
              </div>
            </dl>

            <div className="space-y-0.5">
              <dt className="text-xs font-medium text-muted-foreground">
                Mensagem
              </dt>
              <dd className="text-sm text-foreground">{incidente.mensagem}</dd>
            </div>

            <div className="space-y-1.5">
              <dt className="text-xs font-medium text-muted-foreground">
                Stack trace
              </dt>
              <pre className="max-h-80 overflow-auto rounded-md bg-muted p-3 font-mono text-xs text-foreground">
                {incidente.stackTrace}
              </pre>
            </div>
          </CardContent>
        </Card>
      )}

      {erro && (
        <Card>
          <CardContent className="pt-6">
            <p className="text-sm text-destructive">{erro}</p>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
