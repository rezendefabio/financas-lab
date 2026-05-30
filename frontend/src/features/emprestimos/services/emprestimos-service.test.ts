import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({ apiFetch: vi.fn() }))

import { apiFetch } from '@/services/api-client'
import { emprestimosService } from './emprestimos-service'
import type { Emprestimo } from '../types/emprestimo'

const mockEmprestimo: Emprestimo = {
  id: '00000000-0000-0000-0000-000000000001',
  descricao: 'Emprestimo ao Joao',
  nomeTerceiro: 'Joao',
  tipo: 'CONCEDIDO',
  valor: { valor: 100, moeda: 'BRL' },
  dataEmprestimo: '2026-05-30',
  quitado: false,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('emprestimosService.listar', () => {
  it('chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockEmprestimo])
    const result = await emprestimosService.listar()
    expect(apiFetch).toHaveBeenCalledWith('/api/emprestimos')
    expect(result).toEqual([mockEmprestimo])
  })
})

describe('emprestimosService.criar', () => {
  it('chama apiFetch com POST e payload serializado', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockEmprestimo)
    const payload = {
      descricao: 'Emprestimo ao Joao',
      nomeTerceiro: 'Joao',
      tipo: 'CONCEDIDO' as const,
      valor: 100,
      moeda: 'BRL',
      dataEmprestimo: '2026-05-30',
    }
    await emprestimosService.criar(payload)
    expect(apiFetch).toHaveBeenCalledWith('/api/emprestimos', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  })
})

describe('emprestimosService.atualizar', () => {
  it('chama apiFetch com PUT id e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockEmprestimo)
    const payload = {
      descricao: 'Nova',
      nomeTerceiro: 'Maria',
      tipo: 'RECEBIDO' as const,
      valor: 250,
      moeda: 'BRL',
      dataEmprestimo: '2026-06-01',
      quitado: true,
    }
    await emprestimosService.atualizar(mockEmprestimo.id, payload)
    expect(apiFetch).toHaveBeenCalledWith(`/api/emprestimos/${mockEmprestimo.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  })
})

describe('emprestimosService.remover', () => {
  it('chama apiFetch com DELETE no path com id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)
    await emprestimosService.remover(mockEmprestimo.id)
    expect(apiFetch).toHaveBeenCalledWith(`/api/emprestimos/${mockEmprestimo.id}`, {
      method: 'DELETE',
    })
  })
})
