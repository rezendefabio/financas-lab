import { z } from 'zod'

export type TipoEmprestimo = 'CONCEDIDO' | 'RECEBIDO'

export interface EmprestimoResponse {
  id: string
  descricao: string
  nomeTerceiro: string | null
  tipo: TipoEmprestimo
  valor: {
    valor: number
    moeda: string
  }
  dataEmprestimo: string
  quitado: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface CriarEmprestimoPayload {
  descricao: string
  nomeTerceiro?: string | null
  tipo: TipoEmprestimo
  valor: number
  moeda: string
  dataEmprestimo: string
  quitado: boolean
}

export type AtualizarEmprestimoPayload = CriarEmprestimoPayload

export const TIPO_EMPRESTIMO_OPTIONS: ReadonlyArray<{
  value: TipoEmprestimo
  label: string
}> = [
  { value: 'CONCEDIDO', label: 'Concedido' },
  { value: 'RECEBIDO', label: 'Recebido' },
]

export const emprestimoFormSchema = z.object({
  descricao: z.string().min(1, 'Descricao e obrigatoria').max(100),
  nomeTerceiro: z
    .string()
    .max(100)
    .optional()
    .or(z.literal('')),
  tipo: z.enum(['CONCEDIDO', 'RECEBIDO']),
  valor: z.coerce.number().positive('Valor deve ser positivo'),
  moeda: z.string().length(3),
  dataEmprestimo: z.string().min(1, 'Data e obrigatoria'),
  quitado: z.boolean(),
})

export type EmprestimoFormValues = z.infer<typeof emprestimoFormSchema>

export function defaultEmprestimoFormValues(): EmprestimoFormValues {
  return {
    descricao: '',
    nomeTerceiro: '',
    tipo: 'CONCEDIDO',
    valor: 0,
    moeda: 'BRL',
    dataEmprestimo: '',
    quitado: false,
  }
}
