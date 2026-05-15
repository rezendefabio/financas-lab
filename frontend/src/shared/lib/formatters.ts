export function formatBRL(valor: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(valor)
}

export function formatTipoConta(tipo: string): string {
  const labels: Record<string, string> = {
    CORRENTE: 'Conta Corrente',
    POUPANCA: 'Poupanca',
    DINHEIRO: 'Dinheiro',
    CARTAO_CREDITO: 'Cartao de Credito',
  }
  return labels[tipo] ?? tipo
}

export function formatTipoCategoria(tipo: string): string {
  const labels: Record<string, string> = {
    RECEITA: 'Receita',
    DESPESA: 'Despesa',
  }
  return labels[tipo] ?? tipo
}

export function formatTipoTransacao(tipo: string): string {
  const labels: Record<string, string> = {
    RECEITA: 'Receita',
    DESPESA: 'Despesa',
    TRANSFERENCIA: 'Transferencia',
  }
  return labels[tipo] ?? tipo
}

export function formatDate(dataIso: string | null | undefined): string {
  if (!dataIso) return '--'
  // Date-only strings (LocalDate) get noon UTC to avoid timezone day-shift.
  // Strings that already carry a time component (Instant/LocalDateTime) are parsed directly.
  const date = dataIso.includes('T') ? new Date(dataIso) : new Date(dataIso + 'T12:00:00')
  return date.toLocaleDateString('pt-BR')
}

export function formatDateTime(isoTimestamp: string | null | undefined): string {
  if (!isoTimestamp) return '--'
  return new Date(isoTimestamp).toLocaleString('pt-BR')
}
