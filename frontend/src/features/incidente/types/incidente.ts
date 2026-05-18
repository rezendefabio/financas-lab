export interface IncidenteResponse {
  id: string
  codigo: string
  operacao: string
  classeErro: string
  mensagem: string
  stackTrace: string
  criadoEm: string // Instant -> string ISO
}

export interface FiltrosIncidente {
  criadoApartirDe?: string // ISO datetime string
  criadoAte?: string
  classeErro?: string
  operacao?: string
}
