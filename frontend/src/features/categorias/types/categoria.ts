export interface Categoria {
  id: string
  nome: string
  tipo: 'RECEITA' | 'DESPESA'
  categoriaPaiId: string | null
  criadoEm: string
}
