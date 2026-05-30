import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({ apiFetch: vi.fn() }))

import { apiFetch } from '@/services/api-client'
import { emprestimoService } from './emprestimo-service'
import type { EmprestimoResponse } from '../types'

const mockEmprestimo: EmprestimoResponse = {
  id: '00000000-0000-0000-0000-000000000001',
  descricao: 'Emprestimo a Joao',
  nomeTerceiro: 'Joao Silva',
  tipo: 'CONCEDIDO',
  valor: { valor: 500, moeda: 'BRL' },
  dataEmprestimo: '2026-05-30',
  quitado: false,
  criadoEm: '2026-05-30T00:00:00Z',
  atualizadoEm: '2026-05-30T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('emprestimoService', () => {
  it('listar chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockEmprestimo])
    const result = await emprestimoService.listar()
    expect(apiFetch).toHaveBeenCalledWith('/api/emprestimos')
    expect(result).toEqual([mockEmprestimo])
  })

  it('buscarPorId chama apiFetch com path /api/emprestimos/{id}', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockEmprestimo)
    await emprestimoService.buscarPorId(mockEmprestimo.id)
    expect(apiFetch).toHaveBeenCalledWith(
      `/api/emprestimos/${mockEmprestimo.id}`,
    )
  })

  it('criar chama apiFetch com POST e payload JSON', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockEmprestimo)
    const payload = {
      descricao: 'X',
      nomeTerceiro: 'Y',
      tipo: 'CONCEDIDO' as const,
      valor: 100,
      moeda: 'BRL',
      dataEmprestimo: '2026-05-30',
    }
    await emprestimoService.criar(payload)
    expect(apiFetch).toHaveBeenCalledWith('/api/emprestimos', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  })

  it('atualizar chama apiFetch com PUT id e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockEmprestimo)
    const payload = {
      descricao: 'X',
      nomeTerceiro: 'Y',
      tipo: 'RECEBIDO' as const,
      valor: 200,
      moeda: 'BRL',
      dataEmprestimo: '2026-06-01',
      quitado: true,
    }
    await emprestimoService.atualizar(mockEmprestimo.id, payload)
    expect(apiFetch).toHaveBeenCalledWith(
      `/api/emprestimos/${mockEmprestimo.id}`,
      {
        method: 'PUT',
        body: JSON.stringify(payload),
      },
    )
  })

  it('deletar chama apiFetch com DELETE no path com id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)
    await emprestimoService.deletar(mockEmprestimo.id)
    expect(apiFetch).toHaveBeenCalledWith(
      `/api/emprestimos/${mockEmprestimo.id}`,
      { method: 'DELETE' },
    )
  })
})
