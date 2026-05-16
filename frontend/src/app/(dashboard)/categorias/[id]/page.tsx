'use client'
import { useQuery } from '@tanstack/react-query'
import { useRouter, useParams } from 'next/navigation'
import { Lock } from 'lucide-react'
import { ArrowLeft } from 'lucide-react'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { formatTipoCategoria, formatDateTime } from '@/shared/lib/formatters'

export default function CategoriaDetalhePage() {
  const router = useRouter()
  const params = useParams()
  const id = params.id as string

  const { data: categoria, isLoading, isError } = useQuery({
    queryKey: ['categoria', id],
    queryFn: () => categoriasService.buscar(id),
  })

  const { data: todasCategorias } = useQuery({
    queryKey: ['categorias'],
    queryFn: categoriasService.listar,
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-48 w-full" />
      </div>
    )
  }

  if (isError || !categoria) {
    return <p className="text-sm text-destructive">Erro ao carregar categoria.</p>
  }

  const nomePai = categoria.categoriaPaiId
    ? (todasCategorias?.find((c) => c.id === categoria.categoriaPaiId)?.nome ?? categoria.categoriaPaiId)
    : '—'

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.push('/categorias')}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Detalhe da Categoria</h1>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Dados da Categoria</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-muted-foreground">Nome</p>
              <p className="font-medium flex items-center gap-2">
                {categoria.nome}
                {categoria.system && <Lock className="h-3 w-3 text-muted-foreground" />}
              </p>
            </div>
            <div>
              <p className="text-muted-foreground">Tipo</p>
              <Badge variant={categoria.tipo === 'RECEITA' ? 'default' : 'destructive'}>
                {formatTipoCategoria(categoria.tipo)}
              </Badge>
            </div>
            <div>
              <p className="text-muted-foreground">Categoria Pai</p>
              <p className="font-medium">{nomePai}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Sistema</p>
              {categoria.system ? (
                <Badge variant="secondary">Sim</Badge>
              ) : (
                <Badge variant="outline">Nao</Badge>
              )}
            </div>
            <div>
              <p className="text-muted-foreground">Criado em</p>
              <p className="font-medium">{formatDateTime(categoria.criadoEm)}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Atualizado em</p>
              <p className="font-medium">{formatDateTime(categoria.atualizadoEm)}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {!categoria.system && (
        <Button onClick={() => router.push(`/categorias/${id}/editar`)}>
          Editar
        </Button>
      )}
    </div>
  )
}
