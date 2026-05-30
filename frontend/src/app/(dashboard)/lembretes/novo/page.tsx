'use client'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { useCriarLembrete, LembreteForm } from '@/features/lembrete'
import type { LembreteFormValues } from '@/features/lembrete'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'

export default function NovoLembretePage() {
  const router = useRouter()
  const [apiError, setApiError] = useState<string | null>(null)

  const mutation = useCriarLembrete()

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
        onSuccess: () => {
          router.push('/lembretes')
        },
        onError: () => setApiError('Erro ao criar lembrete.'),
      },
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Novo Lembrete</h1>
      </div>

      <div className="max-w-2xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <LembreteForm
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
