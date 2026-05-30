'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import {
  EmprestimoForm,
  defaultEmprestimoFormValues,
  useCriarEmprestimo,
  type EmprestimoFormValues,
} from '@/features/emprestimo'

export default function NovoEmprestimoPage() {
  const router = useRouter()
  const [apiError, setApiError] = useState<string | null>(null)
  const mutation = useCriarEmprestimo()

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
        onError: () => setApiError('Erro ao salvar emprestimo.'),
      },
    )
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Novo emprestimo</h1>
      <EmprestimoForm
        defaultValues={defaultEmprestimoFormValues()}
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
