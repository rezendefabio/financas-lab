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

export function formatDate(dataIso: string): string {
  // Append noon UTC to avoid day-off-by-one from timezone conversion
  return new Date(dataIso + 'T12:00:00').toLocaleDateString('pt-BR')
}
