export interface Categoria {
  id: string
  nome: string
  tipo: 'RECEITA' | 'DESPESA'
  categoriaPaiId: string | null
  system: boolean
  criadoEm: string
  atualizadoEm: string
}
