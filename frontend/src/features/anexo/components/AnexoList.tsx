'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Download, Trash2 } from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { cn } from '@/shared/lib/utils'
import { anexoService } from '../services/anexo.service'

export interface AnexoListProps {
  entidadeTipo: string
  entidadeId: string
  className?: string
}

function formatarTamanho(bytes: number): string {
  return (bytes / 1024).toFixed(1) + ' KB'
}

export function AnexoList({ entidadeTipo, entidadeId, className }: AnexoListProps) {
  const queryClient = useQueryClient()

  const { data: anexos, isLoading } = useQuery({
    queryKey: ['anexos', entidadeTipo, entidadeId],
    queryFn: () => anexoService.listarPorEntidade(entidadeTipo, entidadeId),
  })

  const removerMutation = useMutation({
    mutationFn: (id: string) => anexoService.remover(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['anexos', entidadeTipo, entidadeId],
      })
    },
  })

  if (isLoading) {
    return (
      <div className={cn('space-y-2', className)} data-testid="anexo-list-loading">
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </div>
    )
  }

  if (!anexos || anexos.length === 0) {
    return (
      <p className={cn('text-sm text-muted-foreground', className)}>
        Nenhum anexo.
      </p>
    )
  }

  return (
    <ul className={cn('space-y-2', className)}>
      {anexos.map((anexo) => (
        <li
          key={anexo.id}
          className="flex items-center justify-between gap-3 rounded-md border bg-card px-3 py-2"
        >
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium">{anexo.nome}</p>
            <p className="text-xs text-muted-foreground">
              {formatarTamanho(anexo.tamanho)}
            </p>
          </div>
          <div className="flex items-center gap-1">
            <a
              href={anexoService.downloadUrl(anexo.id)}
              target="_blank"
              rel="noopener noreferrer"
              aria-label={`Baixar ${anexo.nome}`}
              className="inline-flex h-7 w-7 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
            >
              <Download className="h-4 w-4" />
            </a>
            <Button
              variant="ghost"
              size="icon-sm"
              aria-label={`Excluir ${anexo.nome}`}
              disabled={removerMutation.isPending}
              onClick={() => removerMutation.mutate(anexo.id)}
            >
              <Trash2 className="h-4 w-4 text-destructive" />
            </Button>
          </div>
        </li>
      ))}
    </ul>
  )
}
