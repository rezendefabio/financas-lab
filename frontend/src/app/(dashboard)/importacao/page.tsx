'use client'
import { useState } from 'react'
import { importacaoService } from '@/features/importacao'
import type {
  AnaliseImportacaoResponse,
  ImportacaoJobResponse,
} from '@/features/importacao'
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Label } from '@/shared/components/ui/label'
import { Badge } from '@/shared/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/shared/components/ui/table'

const COLUNAS_FORMATO = [
  {
    coluna: 'tipo',
    obrigatorio: 'Sim',
    formato: 'RECEITA, DESPESA ou TRANSFERENCIA',
    exemplo: 'DESPESA',
  },
  {
    coluna: 'valor',
    obrigatorio: 'Sim',
    formato: 'Decimal com ponto',
    exemplo: '150.00',
  },
  {
    coluna: 'data',
    obrigatorio: 'Sim',
    formato: 'AAAA-MM-DD',
    exemplo: '2026-05-18',
  },
  {
    coluna: 'descricao',
    obrigatorio: 'Nao',
    formato: 'Texto livre',
    exemplo: 'Mercado',
  },
  {
    coluna: 'contaId',
    obrigatorio: 'Sim',
    formato: 'UUID da conta',
    exemplo: '(copiar da tela Contas)',
  },
  {
    coluna: 'categoriaId',
    obrigatorio: 'Nao',
    formato: 'UUID da categoria',
    exemplo: '(copiar da tela Categorias)',
  },
]

const HEADER_CSV =
  'tipo,valor,moeda,data,descricao,contaId,contaDestinoId,categoriaId'

type Etapa = 'upload' | 'revisao'

export default function ImportacaoPage() {
  const [etapa, setEtapa] = useState<Etapa>('upload')
  const [arquivo, setArquivo] = useState<File | null>(null)
  const [analise, setAnalise] = useState<AnaliseImportacaoResponse | null>(null)
  const [selecionados, setSelecionados] = useState<Set<number>>(new Set())
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [resultado, setResultado] = useState<ImportacaoJobResponse | null>(null)

  async function handleAnalisar(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!arquivo) return

    setCarregando(true)
    setResultado(null)
    setErro(null)

    try {
      const resposta = await importacaoService.analisarCsv(arquivo)
      setAnalise(resposta)
      setSelecionados(new Set(resposta.itens.map((i) => i.linha)))
      setEtapa('revisao')
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Erro ao analisar o arquivo.')
    } finally {
      setCarregando(false)
    }
  }

  async function handleBaixarModelo() {
    setErro(null)
    try {
      await importacaoService.downloadModelo()
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Erro ao baixar o modelo CSV.')
    }
  }

  function handleVoltar() {
    setEtapa('upload')
    setAnalise(null)
    setSelecionados(new Set())
    setErro(null)
    setResultado(null)
  }

  function toggleLinha(linha: number) {
    setSelecionados((prev) => {
      const novo = new Set(prev)
      if (novo.has(linha)) novo.delete(linha)
      else novo.add(linha)
      return novo
    })
  }

  function toggleTodos() {
    if (!analise) return
    if (selecionados.size === analise.itens.length) {
      setSelecionados(new Set())
    } else {
      setSelecionados(new Set(analise.itens.map((i) => i.linha)))
    }
  }

  async function handleImportarSelecionadas() {
    if (!analise || !arquivo || selecionados.size === 0) return

    setCarregando(true)
    setErro(null)
    setResultado(null)

    try {
      const itensSelecionados = analise.itens.filter((i) =>
        selecionados.has(i.linha),
      )
      const linhas = itensSelecionados.map((i) => i.linhaCsvOriginal).join('\n')
      const csvStr = HEADER_CSV + '\n' + linhas
      const blob = new Blob([csvStr], { type: 'text/csv' })
      const csvFile = new File([blob], arquivo.name)
      const resposta = await importacaoService.importarCsv(csvFile)
      setResultado(resposta)
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Erro ao importar selecionadas.')
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

      {etapa === 'upload' && (
        <>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-base font-semibold">
                Formato esperado
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm text-muted-foreground">
                O arquivo deve ser CSV com separador ; (ponto e virgula) e
                header na primeira linha.
              </p>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Coluna</TableHead>
                    <TableHead>Obrigatorio</TableHead>
                    <TableHead>Formato</TableHead>
                    <TableHead>Exemplo</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {COLUNAS_FORMATO.map((linha) => (
                    <TableRow key={linha.coluna}>
                      <TableCell className="font-medium">
                        {linha.coluna}
                      </TableCell>
                      <TableCell>{linha.obrigatorio}</TableCell>
                      <TableCell>{linha.formato}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {linha.exemplo}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <Button
                type="button"
                variant="outline"
                onClick={handleBaixarModelo}
              >
                Baixar modelo CSV
              </Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-base font-semibold">
                Arquivo CSV
              </CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleAnalisar} className="space-y-4">
                <div className="space-y-1.5">
                  <Label htmlFor="arquivo-csv">Arquivo</Label>
                  <Input
                    id="arquivo-csv"
                    type="file"
                    accept=".csv"
                    onChange={(e) =>
                      setArquivo(e.target.files?.[0] ?? null)
                    }
                  />
                </div>
                <Button type="submit" disabled={!arquivo || carregando}>
                  {carregando ? 'Analisando...' : 'Analisar'}
                </Button>
              </form>
            </CardContent>
          </Card>
        </>
      )}

      {etapa === 'revisao' && analise && (
        <>
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">
              Revise as linhas antes de importar
            </h2>
            <Button type="button" variant="outline" onClick={handleVoltar}>
              Voltar
            </Button>
          </div>

          <div className="flex flex-wrap gap-2">
            <Badge variant="secondary">
              {analise.linhasValidas} linhas validas
            </Badge>
            <Badge
              variant="secondary"
              className={
                analise.possivelDuplicatas > 0
                  ? 'bg-warning text-warning-foreground'
                  : ''
              }
            >
              {analise.possivelDuplicatas} possiveis duplicatas
            </Badge>
            <Badge
              variant="secondary"
              className={
                analise.errosParsing > 0 ? 'bg-destructive/10 text-destructive' : ''
              }
            >
              {analise.errosParsing} erros de parse
            </Badge>
          </div>

          {analise.erros.length > 0 && (
            <Card className="border-destructive">
              <CardHeader className="pb-2">
                <CardTitle className="text-base font-semibold text-destructive">
                  Erros de parse
                </CardTitle>
              </CardHeader>
              <CardContent>
                <ul className="space-y-1 text-sm">
                  {analise.erros.map((e) => (
                    <li key={e.linha}>
                      <span className="font-medium">Linha {e.linha}:</span>{' '}
                      {e.motivo}
                    </li>
                  ))}
                </ul>
              </CardContent>
            </Card>
          )}

          <Card>
            <CardContent className="pt-6">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-10">
                      <input
                        type="checkbox"
                        aria-label="Selecionar tudo"
                        checked={
                          analise.itens.length > 0 &&
                          selecionados.size === analise.itens.length
                        }
                        onChange={toggleTodos}
                      />
                    </TableHead>
                    <TableHead>Linha</TableHead>
                    <TableHead>Tipo</TableHead>
                    <TableHead>Valor</TableHead>
                    <TableHead>Data</TableHead>
                    <TableHead>Descricao</TableHead>
                    <TableHead>Conta</TableHead>
                    <TableHead>Status</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {analise.itens.map((item) => (
                    <TableRow
                      key={item.linha}
                      className={item.possivelDuplicata ? 'bg-orange-50' : ''}
                    >
                      <TableCell>
                        <input
                          type="checkbox"
                          aria-label={`Selecionar linha ${item.linha}`}
                          checked={selecionados.has(item.linha)}
                          onChange={() => toggleLinha(item.linha)}
                        />
                      </TableCell>
                      <TableCell>{item.linha}</TableCell>
                      <TableCell>{item.tipo}</TableCell>
                      <TableCell>{item.valor}</TableCell>
                      <TableCell>{item.data}</TableCell>
                      <TableCell>{item.descricao}</TableCell>
                      <TableCell className="font-mono text-xs">
                        {item.contaId}
                      </TableCell>
                      <TableCell>
                        {item.possivelDuplicata ? (
                          <Badge className="bg-warning text-warning-foreground">
                            Possivel duplicata
                          </Badge>
                        ) : (
                          <Badge className="bg-green-100 text-green-800">
                            OK
                          </Badge>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <div>
            <Button
              type="button"
              onClick={handleImportarSelecionadas}
              disabled={selecionados.size === 0 || carregando}
            >
              {carregando
                ? 'Importando...'
                : `Importar selecionadas (${selecionados.size})`}
            </Button>
          </div>
        </>
      )}

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
