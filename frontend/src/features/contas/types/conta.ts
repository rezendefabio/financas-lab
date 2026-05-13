export interface ValorMonetario {
  valor: number
  moeda: string
}

export type TipoConta = 'CORRENTE' | 'POUPANCA' | 'DINHEIRO' | 'CARTAO_CREDITO'

export interface Conta {
  id: string
  nome: string
  tipo: TipoConta
  saldoInicialValor: number
  saldoInicialMoeda: string
  ativa: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface SaldoResponse {
  contaId: string
  saldoInicial: ValorMonetario
  totalReceitas: ValorMonetario
  totalDespesas: ValorMonetario
  totalTransferenciasEnviadas: ValorMonetario
  totalTransferenciasRecebidas: ValorMonetario
  saldoAtual: ValorMonetario
  calculadoEm: string
}

export interface SaldoTotalResponse {
  valor: number
  moeda: string
  totalContas: number
}
