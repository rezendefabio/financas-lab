import { apiFetch } from '@/services/api-client'
import { relatorioService } from '@/features/relatorios/services/relatorio-service'
import type { GastosPorCategoria, EvolucaoSaldo } from '@/features/relatorios/types/relatorio'
import type { FluxoCaixa } from '../types/dashboard'

export async function getFluxoCaixa(ano: number, mes: number): Promise<FluxoCaixa> {
  return apiFetch<FluxoCaixa>(`/api/relatorios/dashboard/fluxo-caixa?ano=${ano}&mes=${mes}`)
}

function pad2(valor: number): string {
  return valor.toString().padStart(2, '0')
}

function formatarData(data: Date): string {
  return `${data.getFullYear()}-${pad2(data.getMonth() + 1)}-${pad2(data.getDate())}`
}

export async function getGastosMesAtual(): Promise<GastosPorCategoria> {
  const hoje = new Date()
  const primeiroDia = new Date(hoje.getFullYear(), hoje.getMonth(), 1)
  return relatorioService.getGastosPorCategoria(formatarData(primeiroDia), formatarData(hoje))
}

export async function getEvolucaoUltimosSeisMeses(): Promise<EvolucaoSaldo> {
  const hoje = new Date()
  const inicio = new Date(hoje.getFullYear(), hoje.getMonth() - 6, 1)
  return relatorioService.getEvolucaoSaldo(formatarData(inicio), formatarData(hoje))
}

export { relatorioService }
