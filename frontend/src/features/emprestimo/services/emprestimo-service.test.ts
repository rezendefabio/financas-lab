import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({ apiFetch: vi.fn() }))

import { apiFetch } from '@/services/api-client'
import { emprestimoService } from './emprestimo-service'

const mockEmprestimo = {
  id: '00000000-0000-0000-0000-000000000001',
  descricao: 'Emprestimo X',
  nomeTerceiro: 'Joao',
  tipo: 'CONCEDIDO' as const,
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
  it('chama apiFetch com path do id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockEmprestimo)
    await emprestimoService.buscarPorId(mockEmprestimo.id)
    expect(apiFetch).toHaveBeenCalledWith(`/api/emprestimos/${mockEmprestimo.id}`)
  })
})

describe('emprestimoService.criar', () => {
  it('chama apiFetch com POST e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockEmprestimo)
    const payload = {
      descricao: 'X',
      nomeTerceiro: 'Joao',
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
      descricao: 'Y',
      nomeTerceiro: '',
      tipo: 'RECEBIDO' as const,
      valor: 200,
      moeda: 'BRL',
      dataEmprestimo: '2026-02-01',
      quitado: true,
    }
    await emprestimoService.atualizar(mockEmprestimo.id, payload)
    expect(apiFetch).toHaveBeenCalledWith(
      `/api/emprestimos/${mockEmprestimo.id}`,
      { method: 'PUT', body: JSON.stringify(payload) },
    )
  })
})

describe('emprestimoService.excluir', () => {
  it('chama apiFetch com DELETE no path com id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)
    await emprestimoService.excluir(mockEmprestimo.id)
    expect(apiFetch).toHaveBeenCalledWith(`/api/emprestimos/${mockEmprestimo.id}`, {
      method: 'DELETE',
    })
  })
})
