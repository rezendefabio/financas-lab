'use client'
import { FluxoCaixaCard } from '@/features/dashboard'
import type { FluxoCaixa } from '@/features/dashboard'

interface FluxoCaixaResumoProps {
  data: FluxoCaixa
  isLoading?: boolean
}

/**
 * Resumo de fluxo de caixa do mes para a tela de relatorios.
 * Reutiliza o FluxoCaixaCard do bounded context dashboard -- mesmo
 * contrato de endpoint (/api/relatorios/dashboard/fluxo-caixa).
 */
function FluxoCaixaResumo({ data, isLoading = false }: FluxoCaixaResumoProps) {
  return <FluxoCaixaCard data={data} isLoading={isLoading} />
}

export { FluxoCaixaResumo }
export default FluxoCaixaResumo
