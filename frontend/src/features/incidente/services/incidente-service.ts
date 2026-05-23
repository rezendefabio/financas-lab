import { apiFetch } from '@/services/api-client'
import type { IncidenteResponse, FiltrosIncidente } from '../types/incidente'

export interface RegistrarIncidenteInput {
  operacao: string
  classeErro: string
  mensagem: string
  stackTrace?: string
}

export const incidenteService = {
  listar: (filtros: FiltrosIncidente = {}): Promise<IncidenteResponse[]> => {
    const params = new URLSearchParams()
    if (filtros.criadoApartirDe) params.set('criadoApartirDe', filtros.criadoApartirDe)
    if (filtros.criadoAte) params.set('criadoAte', filtros.criadoAte)
    if (filtros.classeErro) params.set('classeErro', filtros.classeErro)
    if (filtros.operacao) params.set('operacao', filtros.operacao)
    const qs = params.toString()
    return apiFetch(`/api/incidentes${qs ? `?${qs}` : ''}`)
  },

  buscarPorCodigo: (codigo: string): Promise<IncidenteResponse> =>
    apiFetch(`/api/incidentes/${encodeURIComponent(codigo)}`),

  registrar: async (input: RegistrarIncidenteInput): Promise<string | null> => {
    try {
      const data = await apiFetch<{ codigo?: string }>('/api/incidentes', {
        method: 'POST',
        body: JSON.stringify({
          operacao: input.operacao,
          classeErro: input.classeErro,
          mensagem: input.mensagem.slice(0, 500),
          stackTrace: input.stackTrace ?? '',
        }),
      })
      return data?.codigo ?? null
    } catch {
      return null
    }
  },
}
