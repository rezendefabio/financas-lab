'use client'

import { use, useState } from 'react'
import { useRouter } from 'next/navigation'
import {
  EmprestimoForm,
  useAtualizarEmprestimo,
  useEmprestimo,
  type EmprestimoFormValues,
} from '@/features/emprestimo'
import { Skeleton } from '@/shared/components/ui/skeleton'

interface PageProps {
  params: Promise<{ id: string }>
}

export default function EditarEmprestimoPage({ params }: PageProps) {
  const router = useRouter()
  const { id } = use(params)
  const [apiError, setApiError] = useState<string | null>(null)
  const { data, isLoading, isError } = useEmprestimo(id)
  const mutation = useAtualizarEmprestimo(id)

  if (isLoading) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full" />
      </div>
    )
  }

  if (isError || !data) {
    return <p>Erro ao carregar emprestimo.</p>
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

  const handleSubmit = (values: EmprestimoFormValues) => {
    mutation.mutate(
      {
        descricao: values.descricao,
        nomeTerceiro: values.nomeTerceiro
          ? values.nomeTerceiro
          : undefined,
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
        onError: () => setApiError('Erro ao salvar emprestimo.'),
      },
    )
  }

  return (
    <div className="space-y-4 max-w-3xl">
      <h1 className="text-2xl font-semibold">Editar emprestimo</h1>
      <EmprestimoForm
        defaultValues={initialValues}
        onSubmit={handleSubmit}
        isSubmitting={mutation.isPending}
        apiError={apiError}
        onClearApiError={() => setApiError(null)}
        submitLabel="Salvar alteracoes"
        onCancel={() => router.push('/emprestimos')}
        showQuitado
      />
    </div>
  )
}
