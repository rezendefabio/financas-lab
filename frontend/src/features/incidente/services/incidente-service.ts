import { apiFetch } from '@/services/api-client'
import type { IncidenteResponse } from '../types/incidente'

export const incidenteService = {
  buscarPorCodigo: (codigo: string): Promise<IncidenteResponse> =>
    apiFetch(`/api/incidentes/${encodeURIComponent(codigo)}`),
}
