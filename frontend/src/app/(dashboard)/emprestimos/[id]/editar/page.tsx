'use client'

import { useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { Skeleton } from '@/shared/components/ui/skeleton'
import {
  EmprestimoForm,
  useEmprestimo,
  useAtualizarEmprestimo,
  type EmprestimoFormValues,
} from '@/features/emprestimo'

export default function EditarEmprestimoPage() {
  const router = useRouter()
  const params = useParams<{ id: string }>()
  const id = params.id
  const { data, isLoading, isError } = useEmprestimo(id)
  const mutation = useAtualizarEmprestimo(id)
  const [apiError, setApiError] = useState<string | null>(null)

  if (isLoading) {
    return <Skeleton className="h-64 w-full" />
  }
  if (isError || !data) {
    return <p className="text-sm text-destructive">Erro ao carregar emprestimo.</p>
  }

  const initialValues: EmprestimoFormValues = {
    descricao: data.descricao,
    nomeTerceiro: data.nomeTerceiro ?? '',
    tipo: data.tipo,
    valor: data.valor.valor,
    moeda: data.valor.moeda,
    dataEmprestimo: data.dataEmprestimo,
    quitado: data.quitado,
  }

  const onSubmit = (values: EmprestimoFormValues) => {
    mutation.mutate(
      {
        descricao: values.descricao,
        nomeTerceiro:
          values.nomeTerceiro && values.nomeTerceiro.trim().length > 0
            ? values.nomeTerceiro
            : null,
        tipo: values.tipo,
        valor: values.valor,
        moeda: values.moeda,
        dataEmprestimo: values.dataEmprestimo,
        quitado: values.quitado,
      },
      {
        onSuccess: () => {
          router.push('/emprestimos')
        },
        onError: () => setApiError('Erro ao atualizar emprestimo.'),
      },
    )
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Editar emprestimo</h1>
      <EmprestimoForm
        defaultValues={initialValues}
        onSubmit={onSubmit}
        isSubmitting={mutation.isPending}
        apiError={apiError}
        onClearApiError={() => setApiError(null)}
        submitLabel="Salvar"
        onCancel={() => router.push('/emprestimos')}
      />
    </div>
  )
}
