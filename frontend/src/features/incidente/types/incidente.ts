export interface IncidenteResponse {
  id: string
  codigo: string
  operacao: string
  classeErro: string
  mensagem: string
  stackTrace: string
  criadoEm: string // Instant -> string ISO
}
