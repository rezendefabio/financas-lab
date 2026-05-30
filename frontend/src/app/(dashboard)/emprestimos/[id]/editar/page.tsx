'use client'
import { use, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { ArrowLeft } from 'lucide-react'
import { emprestimosService } from '@/features/emprestimos/services/emprestimos-service'
import { useEmprestimo } from '@/features/emprestimos/hooks/use-emprestimos'
import {
  EmprestimoForm,
  type EmprestimoFormValues,
} from '@/features/emprestimos/components/EmprestimoForm'
import type { AtualizarEmprestimoPayload } from '@/features/emprestimos/types/emprestimo'
import { Card, CardContent } from '@/shared/components/ui/card'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'

interface PageProps {
  params: Promise<{ id: string }>
}

function toAtualizarPayload(values: EmprestimoFormValues): AtualizarEmprestimoPayload {
  return {
    descricao: values.descricao,
    nomeTerceiro: values.nomeTerceiro || undefined,
    tipo: values.tipo,
    valor: values.valor,
    moeda: values.moeda,
    dataEmprestimo: values.dataEmprestimo,
    quitado: values.quitado,
  }
}

export default function EditarEmprestimoPage({ params }: PageProps) {
  const router = useRouter()
  const { id } = use(params)
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const { data, isLoading, isError } = useEmprestimo(id)

  const mutation = useMutation({
    mutationFn: (v: EmprestimoFormValues) =>
      emprestimosService.atualizar(id, toAtualizarPayload(v)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['emprestimos'] })
      await queryClient.invalidateQueries({ queryKey: ['emprestimos', id] })
      router.push('/emprestimos')
    },
    onError: () => setApiError('Erro ao atualizar emprestimo.'),
  })

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-48 w-full" />
      </div>
    )
  }

  if (isError || !data) {
    return (
      <p className="text-sm text-destructive">Erro ao carregar emprestimo.</p>
    )
  }

  // Derivar initialValues SINCRONAMENTE a partir de `data` (early-return acima
  // garante que existe). Sem useState/useEffect (react-hooks/set-state-in-effect).
  const initialValues: EmprestimoFormValues = {
    descricao: data.descricao,
    nomeTerceiro: data.nomeTerceiro ?? '',
    tipo: data.tipo,
    valor: data.valor.valor,
    moeda: data.valor.moeda,
    dataEmprestimo: data.dataEmprestimo,
    quitado: data.quitado,
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          aria-label="Voltar"
          onClick={() => router.push('/emprestimos')}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">
          Editar Emprestimo
        </h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <EmprestimoForm
              defaultValues={initialValues}
              onSubmit={(v) => mutation.mutate(v)}
              isSubmitting={mutation.isPending}
              apiError={apiError}
              onClearApiError={() => setApiError(null)}
              submitLabel="Salvar alteracoes"
              onCancel={() => router.push('/emprestimos')}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
