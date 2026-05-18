'use client'
import { useState } from 'react'
import { importacaoService } from '@/features/importacao'
import type { ImportacaoJobResponse } from '@/features/importacao'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Label } from '@/shared/components/ui/label'

export default function ImportacaoPage() {
  const [arquivo, setArquivo] = useState<File | null>(null)
  const [carregando, setCarregando] = useState(false)
  const [resultado, setResultado] = useState<ImportacaoJobResponse | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!arquivo) return

    setCarregando(true)
    setResultado(null)
    setErro(null)

    try {
      const resposta = await importacaoService.importarCsv(arquivo)
      setResultado(resposta)
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Erro ao importar o arquivo.')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">
          Importar Transacoes
        </h1>
        <p className="text-sm text-muted-foreground">
          Faca o upload de um arquivo CSV no formato padrao do financas-lab.
        </p>
      </div>

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-base font-semibold">
            Arquivo CSV
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="arquivo-csv">Arquivo</Label>
              <Input
                id="arquivo-csv"
                type="file"
                accept=".csv"
                onChange={(e) => setArquivo(e.target.files?.[0] ?? null)}
              />
            </div>
            <Button type="submit" disabled={!arquivo || carregando}>
              {carregando ? 'Importando...' : 'Importar'}
            </Button>
          </form>
        </CardContent>
      </Card>

      {resultado && (
        <Card>
          <CardContent className="space-y-1 pt-6">
            <p className="text-sm font-medium text-foreground">
              Importacao iniciada. Job ID: {resultado.jobExecutionId}
            </p>
            <p className="text-sm text-muted-foreground">
              O processamento ocorre em background. Consulte o status em breve.
            </p>
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
