export interface ValorMonetario {
  valor: number
  moeda: string
}

export interface Conta {
  id: string
  nome: string
  tipo: 'CORRENTE' | 'POUPANCA' | 'INVESTIMENTO' | 'CARTEIRA'
  saldoInicial: ValorMonetario
  ativo: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface SaldoResponse {
  saldoAtual: ValorMonetario
  saldoInicial: ValorMonetario
  totalReceitas: ValorMonetario
  totalDespesas: ValorMonetario
}

export interface SaldoTotalResponse {
  valor: number
  moeda: string
  totalContas: number
}
