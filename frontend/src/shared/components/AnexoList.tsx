'use client'

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button } from '@/shared/components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/shared/components/ui/table'
import { formatDateTime, formatTamanho } from '@/shared/lib/formatters'
import { anexosService } from '@/shared/services/anexos.service'

interface AnexoListProps {
  entidadeTipo: string
  entidadeId: string
}

function AnexoList({ entidadeTipo, entidadeId }: AnexoListProps) {
  const queryClient = useQueryClient()
  const queryKey = ['anexos', entidadeTipo, entidadeId]

  const { data, isLoading, isError } = useQuery({
    queryKey,
    queryFn: () => anexosService.listar(entidadeTipo, entidadeId),
  })

  const removerMutation = useMutation({
    mutationFn: (id: string) => anexosService.remover(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey })
    },
  })

  if (isLoading) {
    return (
      <p className="text-sm text-muted-foreground" role="status">
        Carregando anexos...
      </p>
    )
  }

  if (isError) {
    return (
      <p className="text-sm text-destructive" role="alert">
        Erro ao carregar os anexos.
      </p>
    )
  }

  const anexos = data ?? []

  return (
    <div className="space-y-2">
      {removerMutation.isError && (
        <p className="text-sm text-destructive" role="alert">
          Falha ao remover o anexo.
        </p>
      )}
      {anexos.length === 0 ? (
        <p className="text-sm text-muted-foreground">Nenhum anexo.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Nome</TableHead>
              <TableHead>Tipo</TableHead>
              <TableHead>Tamanho</TableHead>
              <TableHead>Data</TableHead>
              <TableHead className="text-right">Acoes</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {anexos.map((anexo) => (
              <TableRow key={anexo.id}>
                <TableCell className="font-medium">{anexo.nome}</TableCell>
                <TableCell className="text-muted-foreground">
                  {anexo.tipoConteudo}
                </TableCell>
                <TableCell className="tabular-nums">
                  {formatTamanho(anexo.tamanho)}
                </TableCell>
                <TableCell>{formatDateTime(anexo.criadoEm)}</TableCell>
                <TableCell className="space-x-2 text-right">
                  <Button
                    variant="outline"
                    size="sm"
                    render={
                      <a
                        href={anexosService.urlDownload(anexo.id)}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        Baixar
                      </a>
                    }
                  />
                  <Button
                    variant="destructive"
                    size="sm"
                    disabled={
                      removerMutation.isPending &&
                      removerMutation.variables === anexo.id
                    }
                    onClick={() => removerMutation.mutate(anexo.id)}
                  >
                    {removerMutation.isPending &&
                    removerMutation.variables === anexo.id
                      ? 'Removendo...'
                      : 'Remover'}
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  )
}

export { AnexoList }
export default AnexoList
