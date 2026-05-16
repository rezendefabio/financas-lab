export interface ValorMonetario {
  valor: number
  moeda: string
}

export type TipoConta =
  | 'CORRENTE'
  | 'POUPANCA'
  | 'DINHEIRO'
  | 'CARTAO_CREDITO'
  | 'INVESTIMENTO'
  | 'OUTRO'

export interface Conta {
  id: string
  userId: string | null
  nome: string
  tipo: TipoConta
  saldoInicialValor: number
  saldoInicialMoeda: string
  saldoAtualValor: number | null
  saldoAtualMoeda: string | null
  limiteCreditoValor: number | null
  limiteCreditoMoeda: string | null
  diaFechamento: number | null
  diaVencimento: number | null
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
