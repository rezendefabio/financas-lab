import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({ apiFetch: vi.fn() }))

import { apiFetch } from '@/services/api-client'
import { emprestimoService } from './emprestimo-service'
import type { EmprestimoResponse } from '../types/emprestimo'

const mockEmprestimo: EmprestimoResponse = {
  id: '00000000-0000-0000-0000-000000000001',
  descricao: 'Emprestimo ao Joao',
  nomeTerceiro: 'Joao',
  tipo: 'CONCEDIDO',
  valor: { valor: 100, moeda: 'BRL' },
  dataEmprestimo: '2026-01-15',
  quitado: false,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('emprestimoService.listar', () => {
  it('chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockEmprestimo])
    const result = await emprestimoService.listar()
    expect(apiFetch).toHaveBeenCalledWith('/api/emprestimos')
    expect(result).toEqual([mockEmprestimo])
  })
})

describe('emprestimoService.buscarPorId', () => {
  it('deriva o registro da listagem', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockEmprestimo])
    const result = await emprestimoService.buscarPorId(mockEmprestimo.id)
    expect(apiFetch).toHaveBeenCalledWith('/api/emprestimos')
    expect(result).toEqual(mockEmprestimo)
  })

  it('retorna undefined quando o id nao existe', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockEmprestimo])
    const result = await emprestimoService.buscarPorId('inexistente')
    expect(result).toBeUndefined()
  })
})

describe('emprestimoService.criar', () => {
  it('chama apiFetch com POST e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockEmprestimo)
    const payload = {
      descricao: 'Emprestimo ao Joao',
      tipo: 'CONCEDIDO' as const,
      valor: 100,
      moeda: 'BRL',
      dataEmprestimo: '2026-01-15',
      quitado: false,
    }
    await emprestimoService.criar(payload)
    expect(apiFetch).toHaveBeenCalledWith('/api/emprestimos', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  })
})

describe('emprestimoService.atualizar', () => {
  it('chama apiFetch com PUT id e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockEmprestimo)
    const payload = {
      descricao: 'Nova',
      tipo: 'RECEBIDO' as const,
      valor: 200,
      moeda: 'BRL',
      dataEmprestimo: '2026-02-01',
      quitado: true,
    }
    await emprestimoService.atualizar(mockEmprestimo.id, payload)
    expect(apiFetch).toHaveBeenCalledWith(`/api/emprestimos/${mockEmprestimo.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  })
})

describe('emprestimoService.deletar', () => {
  it('chama apiFetch com DELETE no path com id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)
    await emprestimoService.deletar(mockEmprestimo.id)
    expect(apiFetch).toHaveBeenCalledWith(`/api/emprestimos/${mockEmprestimo.id}`, {
      method: 'DELETE',
    })
  })
})
