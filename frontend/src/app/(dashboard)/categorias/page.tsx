'use client'
import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { cn } from '@/shared/lib/utils'
import { formatTipoCategoria } from '@/shared/lib/formatters'
import type { Categoria } from '@/features/categorias/types/categoria'

function CategoriaCard({ categoria, onClick }: { categoria: Categoria; onClick: () => void }) {
  const isReceita = categoria.tipo === 'RECEITA'
  return (
    <Card
      className={cn(
        'cursor-pointer transition-colors hover:bg-muted/50 border-l-4',
        isReceita ? 'border-l-primary' : 'border-l-destructive'
      )}
      onClick={onClick}
    >
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base">{categoria.nome}</CardTitle>
          <Badge variant={isReceita ? 'default' : 'destructive'}>
            {formatTipoCategoria(categoria.tipo)}
          </Badge>
        </div>
      </CardHeader>
      {categoria.categoriaPaiId && (
        <CardContent>
          <p className="text-xs text-muted-foreground">Subcategoria</p>
        </CardContent>
      )}
    </Card>
  )
}

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
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardHeader>
                <Skeleton className="h-5 w-32" />
              </CardHeader>
            </Card>
          ))}
        </div>
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
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.map((categoria) => (
            <CategoriaCard
              key={categoria.id}
              categoria={categoria}
              onClick={() => {}}
            />
          ))}
        </div>
      )}
    </div>
  )
}
