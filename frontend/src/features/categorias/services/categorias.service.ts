import { apiFetch } from '@/services/api-client'
import type { Categoria } from '../types/categoria'

interface CriarCategoriaRequest {
  nome: string
  tipo: 'RECEITA' | 'DESPESA'
  categoriaPaiId?: string
}

export const categoriasService = {
  listar: () => apiFetch<Categoria[]>('/api/categorias'),
  buscar: (id: string) => apiFetch<Categoria>(`/api/categorias/${id}`),
  criar: (data: CriarCategoriaRequest) =>
    apiFetch<Categoria>('/api/categorias', { method: 'POST', body: JSON.stringify(data) }),
}
