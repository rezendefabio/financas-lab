'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  LembreteForm,
  defaultLembreteFormValues,
  lembreteService,
  type LembreteFormValues,
} from '@/features/lembrete'

const SCREEN_CODE = 'MOD-LEM-001'

export default function NovoLembretePage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: (values: LembreteFormValues) =>
      lembreteService.criar({
        titulo: values.titulo,
        descricao: values.descricao ? values.descricao : undefined,
        dataLembrete: values.dataLembrete,
        prioridade: values.prioridade,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['lembretes'] })
      router.push('/lembretes')
    },
    onError: () => setApiError('Erro ao salvar lembrete.'),
  })

  return (
    <div className="space-y-4 p-6" data-screen-code={SCREEN_CODE}>
      <h1 className="text-2xl font-semibold">Novo lembrete</h1>
      <LembreteForm
        defaultValues={defaultLembreteFormValues()}
        onSubmit={(v) => mutation.mutate(v)}
        isSubmitting={mutation.isPending}
        apiError={apiError}
        onClearApiError={() => setApiError(null)}
        submitLabel="Salvar"
        onCancel={() => router.push('/lembretes')}
        showConcluido={false}
      />
    </div>
  )
}
