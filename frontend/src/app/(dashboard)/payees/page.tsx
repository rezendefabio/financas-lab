'use client'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { listarPayees, deletarPayee } from '@/features/payee/services/payee-service'
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

export default function PayeesPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)

  const { data: payees, isLoading, isError } = useQuery({
    queryKey: ['payees'],
    queryFn: listarPayees,
  })

  const { data: categorias } = useQuery({
    queryKey: ['categorias'],
    queryFn: categoriasService.listar,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deletarPayee(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['payees'] })
      setConfirmDeleteId(null)
    },
  })

  const getCategoriaName = (id?: string) => {
    if (!id || !categorias) return '—'
    const cat = categorias.find((c) => c.id === id)
    return cat ? cat.nome : '—'
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Beneficiarios</h1>
        <Button onClick={() => router.push('/payees/novo')}>+ Novo Beneficiario</Button>
      </div>

      {isLoading && (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nome</TableHead>
                  <TableHead>Categoria Padrao</TableHead>
                  <TableHead className="text-right">Acoes</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {[1, 2, 3].map((i) => (
                  <TableRow key={i}>
                    <TableCell><Skeleton className="h-4 w-32" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-16" /></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar beneficiarios.</p>
      )}

      {payees && payees.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhum beneficiario cadastrado.</p>
          <Button onClick={() => router.push('/payees/novo')}>Criar primeiro beneficiario</Button>
        </div>
      )}

      {payees && payees.length > 0 && (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nome</TableHead>
                  <TableHead>Categoria Padrao</TableHead>
                  <TableHead className="text-right">Acoes</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {payees.map((payee) => (
                  <TableRow key={payee.id}>
                    <TableCell className="font-medium">{payee.nome}</TableCell>
                    <TableCell>{getCategoriaName(payee.categoriaPadraoId)}</TableCell>
                    <TableCell className="text-right">
                      {confirmDeleteId === payee.id ? (
                        <span className="inline-flex gap-2">
                          <Button
                            size="sm"
                            variant="destructive"
                            onClick={() => deleteMutation.mutate(payee.id)}
                            disabled={deleteMutation.isPending}
                          >
                            Confirmar
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => setConfirmDeleteId(null)}
                          >
                            Cancelar
                          </Button>
                        </span>
                      ) : (
                        <span className="inline-flex gap-2">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => router.push(`/payees/${payee.id}/editar`)}
                          >
                            Editar
                          </Button>
                          <Button
                            size="sm"
                            variant="destructive"
                            onClick={() => setConfirmDeleteId(payee.id)}
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
