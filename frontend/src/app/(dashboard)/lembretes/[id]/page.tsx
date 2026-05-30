'use client'
import { useState, useMemo } from 'react'
import { useRouter, useParams } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import {
  useLembrete,
  useAtualizarLembrete,
  LembreteForm,
} from '@/features/lembrete'
import type { LembreteFormValues } from '@/features/lembrete'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'

export default function EditarLembretePage() {
  const router = useRouter()
  const params = useParams<{ id: string }>()
  const id = params.id
  const [apiError, setApiError] = useState<string | null>(null)

  const { data: lembrete, isLoading, isError } = useLembrete(id)

  const mutation = useAtualizarLembrete(id)

  const loadedValues = useMemo<LembreteFormValues | null>(() => {
    if (!lembrete) return null
    return {
      titulo: lembrete.titulo,
      descricao: lembrete.descricao ?? '',
      dataLembrete: lembrete.dataLembrete,
      prioridade: lembrete.prioridade,
      concluido: lembrete.concluido,
    }
  }, [lembrete])

  const handleSubmit = (values: LembreteFormValues) => {
    mutation.mutate(
      {
        titulo: values.titulo,
        descricao: values.descricao && values.descricao.length > 0 ? values.descricao : null,
        dataLembrete: values.dataLembrete,
        prioridade: values.prioridade,
        concluido: values.concluido,
      },
      {
        onSuccess: () => router.push('/lembretes'),
        onError: () => setApiError('Erro ao atualizar lembrete.'),
      },
    )
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <h1 className="text-2xl font-semibold tracking-tight">Editar Lembrete</h1>
        </div>
        <p className="text-muted-foreground">Carregando...</p>
      </div>
    )
  }

  if (isError || !lembrete) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <h1 className="text-2xl font-semibold tracking-tight">Editar Lembrete</h1>
        </div>
        <p className="text-sm text-destructive">Lembrete nao encontrado.</p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Editar Lembrete</h1>
      </div>

      <div className="max-w-2xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <LembreteForm
              loadedValues={loadedValues}
              onSubmit={handleSubmit}
              isSubmitting={mutation.isPending}
              apiError={apiError}
              onClearApiError={() => setApiError(null)}
              submitLabel="Salvar"
              onCancel={() => router.push('/lembretes')}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
