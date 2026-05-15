import { apiFetch } from '@/services/api-client'
import type { FluxoCaixa } from '../types/dashboard'

export async function getFluxoCaixa(ano: number, mes: number): Promise<FluxoCaixa> {
  return apiFetch<FluxoCaixa>(`/api/relatorios/dashboard/fluxo-caixa?ano=${ano}&mes=${mes}`)
}
