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
