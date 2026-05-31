'use client'

import * as React from 'react'
import { useRouter } from 'next/navigation'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  EmprestimoForm,
  defaultEmprestimoFormValues,
  emprestimoService,
  type EmprestimoFormValues,
} from '@/features/emprestimo'

export default function NovoEmprestimoPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = React.useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: (values: EmprestimoFormValues) =>
      emprestimoService.criar({
        descricao: values.descricao,
        nomeTerceiro: values.nomeTerceiro || undefined,
        tipo: values.tipo,
        valor: values.valor,
        moeda: values.moeda,
        dataEmprestimo: values.dataEmprestimo,
        quitado: values.quitado,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['emprestimos'] })
      router.push('/emprestimos')
    },
    onError: () => setApiError('Erro ao salvar o emprestimo.'),
  })

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Novo Emprestimo</h1>
      <EmprestimoForm
        defaultValues={defaultEmprestimoFormValues()}
        onSubmit={mutation.mutate}
        isSubmitting={mutation.isPending}
        apiError={apiError}
        onClearApiError={() => setApiError(null)}
        submitLabel="Salvar"
        onCancel={() => router.push('/emprestimos')}
      />
    </div>
  )
}
