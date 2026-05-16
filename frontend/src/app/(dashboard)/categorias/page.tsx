'use client'
import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
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
import { formatTipoCategoria } from '@/shared/lib/formatters'

export default function CategoriasPage() {
  const router = useRouter()
  const { data, isLoading, isError } = useQuery({
    queryKey: ['categorias'],
    queryFn: categoriasService.listar,
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Categorias</h1>
        <Button onClick={() => router.push('/categorias/novo')}>Nova Categoria</Button>
      </div>

      {isLoading && (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nome</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead>Categoria Pai</TableHead>
                  <TableHead className="text-right">Acoes</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {[1, 2, 3].map((i) => (
                  <TableRow key={i}>
                    <TableCell><Skeleton className="h-4 w-32" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-20" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-24" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-8" /></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar categorias.</p>
      )}

      {data && data.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhuma categoria cadastrada.</p>
          <Button onClick={() => router.push('/categorias/novo')}>Criar primeira categoria</Button>
        </div>
      )}

      {data && data.length > 0 && (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nome</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead>Categoria Pai</TableHead>
                  <TableHead className="text-right">Acoes</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.map((categoria) => {
                  const nomePai = categoria.categoriaPaiId
                    ? (data.find((c) => c.id === categoria.categoriaPaiId)?.nome ?? '—')
                    : '—'
                  return (
                    <TableRow
                      key={categoria.id}
                      className="cursor-pointer hover:bg-muted/50"
                      onClick={() => router.push(`/categorias/${categoria.id}`)}
                    >
                      <TableCell className="font-medium">
                        <span className="flex items-center gap-2">
                          {categoria.nome}
                          {categoria.system && (
                            <Badge variant="secondary">Sistema</Badge>
                          )}
                        </span>
                      </TableCell>
                      <TableCell>
                        <Badge variant={categoria.tipo === 'RECEITA' ? 'default' : 'destructive'}>
                          {formatTipoCategoria(categoria.tipo)}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground text-sm">
                        {nomePai}
                      </TableCell>
                      <TableCell className="text-right">
                        <span className="text-muted-foreground text-sm">--</span>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
