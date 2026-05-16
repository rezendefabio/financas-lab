import { apiFetch } from '@/services/api-client'
import type { GastosPorCategoria, EvolucaoSaldo } from '../types/relatorio'

function montarQuery(dataInicio: string, dataFim: string, contaId?: string): string {
  const params = new URLSearchParams({ dataInicio, dataFim })
  if (contaId) {
    params.set('contaId', contaId)
  }
  return params.toString()
}

export const relatorioService = {
  getGastosPorCategoria: (dataInicio: string, dataFim: string, contaId?: string) =>
    apiFetch<GastosPorCategoria>(
      `/api/relatorios/gastos-por-categoria?${montarQuery(dataInicio, dataFim, contaId)}`,
    ),
  getEvolucaoSaldo: (dataInicio: string, dataFim: string, contaId?: string) =>
    apiFetch<EvolucaoSaldo>(
      `/api/relatorios/evolucao-saldo?${montarQuery(dataInicio, dataFim, contaId)}`,
    ),
}
