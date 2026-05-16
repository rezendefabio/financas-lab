'use client'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { listarTags, deletarTag } from '@/features/tag'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from '@/shared/components/ui/table'

export default function TagsPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [confirmandoId, setConfirmandoId] = useState<string | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['tags'],
    queryFn: listarTags,
  })

  const deletarMutation = useMutation({
    mutationFn: (id: string) => deletarTag(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['tags'] })
      setConfirmandoId(null)
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Tags</h1>
        <Button onClick={() => router.push('/tags/novo')}>+ Nova Tag</Button>
      </div>

      {isLoading && (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nome</TableHead>
                  <TableHead>Cor</TableHead>
                  <TableHead className="text-right">Acoes</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {[1, 2, 3].map((i) => (
                  <TableRow key={i}>
                    <TableCell><Skeleton className="h-4 w-32" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-16" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-20" /></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar tags.</p>
      )}

      {data && data.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhuma tag cadastrada.</p>
          <Button onClick={() => router.push('/tags/novo')}>Criar primeira tag</Button>
        </div>
      )}

      {data && data.length > 0 && (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nome</TableHead>
                  <TableHead>Cor</TableHead>
                  <TableHead className="text-right">Acoes</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.map((tag) => (
                  <TableRow key={tag.id}>
                    <TableCell className="font-medium">{tag.nome}</TableCell>
                    <TableCell>
                      {tag.cor ? (
                        <div className="flex items-center gap-2">
                          <span
                            className="inline-block h-4 w-4 rounded-full border border-border"
                            style={{ backgroundColor: tag.cor }}
                            aria-label={`Cor: ${tag.cor}`}
                          />
                          <span className="text-sm text-muted-foreground">{tag.cor}</span>
                        </div>
                      ) : (
                        <span className="text-sm text-muted-foreground">—</span>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      {confirmandoId === tag.id ? (
                        <span className="flex items-center justify-end gap-2">
                          <Button
                            size="sm"
                            variant="destructive"
                            disabled={deletarMutation.isPending}
                            onClick={() => deletarMutation.mutate(tag.id)}
                          >
                            Confirmar
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => setConfirmandoId(null)}
                          >
                            Cancelar
                          </Button>
                        </span>
                      ) : (
                        <span className="flex items-center justify-end gap-2">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => router.push(`/tags/${tag.id}/editar`)}
                          >
                            Editar
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            className="text-destructive hover:text-destructive"
                            onClick={() => setConfirmandoId(tag.id)}
                          >
                            Excluir
                          </Button>
                        </span>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
