'use client'

import { use, useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  LembreteForm,
  lembreteService,
  useLembrete,
  type LembreteFormValues,
} from '@/features/lembrete'

const SCREEN_CODE = 'MOD-LEM-001'

interface PageProps {
  params: Promise<{ id: string }>
}

export default function EditarLembretePage({ params }: PageProps) {
  const { id } = use(params)
  const router = useRouter()
  const queryClient = useQueryClient()
  const { data } = useLembrete(id)
  const [apiError, setApiError] = useState<string | null>(null)

  const defaults = useMemo<LembreteFormValues | null>(() => {
    if (!data) return null
    return {
      titulo: data.titulo,
      descricao: data.descricao ?? '',
      dataLembrete: data.dataLembrete,
      prioridade: data.prioridade,
      concluido: data.concluido,
    }
  }, [data])

  const mutation = useMutation({
    mutationFn: (values: LembreteFormValues) =>
      lembreteService.atualizar(id, {
        titulo: values.titulo,
        descricao: values.descricao ? values.descricao : undefined,
        dataLembrete: values.dataLembrete,
        prioridade: values.prioridade,
        concluido: values.concluido,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['lembretes'] })
      await queryClient.invalidateQueries({ queryKey: ['lembretes', id] })
      router.push('/lembretes')
    },
    onError: () => setApiError('Erro ao salvar lembrete.'),
  })

  if (!defaults) {
    return (
      <div className="p-6" data-screen-code={SCREEN_CODE}>
        Carregando...
      </div>
    )
  }

  return (
    <div className="space-y-4 p-6" data-screen-code={SCREEN_CODE}>
      <h1 className="text-2xl font-semibold">Editar lembrete</h1>
      <LembreteForm
        defaultValues={defaults}
        onSubmit={(v) => mutation.mutate(v)}
        isSubmitting={mutation.isPending}
        apiError={apiError}
        onClearApiError={() => setApiError(null)}
        submitLabel="Salvar"
        onCancel={() => router.push('/lembretes')}
        showConcluido={true}
      />
    </div>
  )
}
