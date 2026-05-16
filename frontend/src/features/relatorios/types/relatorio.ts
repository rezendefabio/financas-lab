export interface ValorMonetario {
  valor: number
  moeda: string
}

export interface ItemGastoCategoria {
  categoriaId: string | null
  nomeCategoria: string
  totalGasto: ValorMonetario
}

export interface GastosPorCategoria {
  dataInicio: string
  dataFim: string
  totalGeral: ValorMonetario
  itensPorCategoria: ItemGastoCategoria[]
}

export interface ItemEvolucaoMes {
  mes: string
  totalReceitas: ValorMonetario
  totalDespesas: ValorMonetario
  saldoLiquido: ValorMonetario
}

export interface EvolucaoSaldo {
  dataInicio: string
  dataFim: string
  totalReceitas: ValorMonetario
  totalDespesas: ValorMonetario
  saldoLiquido: ValorMonetario
  evolucaoPorMes: ItemEvolucaoMes[]
}
