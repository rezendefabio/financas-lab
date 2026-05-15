import { apiFetch } from '@/services/api-client'
import type { Conta, SaldoResponse, SaldoTotalResponse } from '../types/conta'

interface CriarContaRequest {
  nome: string
  tipo: string
  saldoInicialValor: number
  saldoInicialMoeda: string
  userId?: string
  limiteCreditoValor?: number
  limiteCreditoMoeda?: string
  diaFechamento?: number
  diaVencimento?: number
}

export const contasService = {
  listar: () => apiFetch<Conta[]>('/api/contas'),
  criar: (data: CriarContaRequest) =>
    apiFetch<Conta>('/api/contas', { method: 'POST', body: JSON.stringify(data) }),
  buscarPorId: (id: string) => apiFetch<Conta>(`/api/contas/${id}`),
  calcularSaldo: (id: string) => apiFetch<SaldoResponse>(`/api/contas/${id}/saldo`),
  saldoTotal: () => apiFetch<SaldoTotalResponse>('/api/contas/saldo-total'),
  desativar: (id: string) =>
    apiFetch<void>(`/api/contas/${id}`, { method: 'DELETE' }),
}
