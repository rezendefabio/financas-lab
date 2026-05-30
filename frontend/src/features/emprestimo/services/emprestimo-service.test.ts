import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({ apiFetch: vi.fn() }))

import { apiFetch } from '@/services/api-client'
import { emprestimoService } from './emprestimo-service'
import type {
  Emprestimo,
  CriarEmprestimoPayload,
  AtualizarEmprestimoPayload,
} from '../types/emprestimo'

const mockEntity: Emprestimo = {
  id: '00000000-0000-0000-0000-000000000001',
  userId: '00000000-0000-0000-0000-000000000099',
  descricao: 'Teste',
  nomeTerceiro: 'Joao',
  tipo: 'CONCEDIDO',
  valor: { valor: 150, moeda: 'BRL' },
  dataEmprestimo: '2026-01-15',
  quitado: false,
  criadoEm: '2026-01-15T00:00:00Z',
  atualizadoEm: '2026-01-15T00:00:00Z',
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('emprestimoService.listar', () => {
  it('chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mockEntity])
    const result = await emprestimoService.listar()
    expect(apiFetch).toHaveBeenCalledWith('/api/emprestimos')
    expect(result).toEqual([mockEntity])
  })
})

describe('emprestimoService.buscar', () => {
  it('chama apiFetch com path do id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockEntity)
    const result = await emprestimoService.buscar(mockEntity.id)
    expect(apiFetch).toHaveBeenCalledWith(`/api/emprestimos/${mockEntity.id}`)
    expect(result).toEqual(mockEntity)
  })
})

describe('emprestimoService.criar', () => {
  it('chama apiFetch com POST e payload serializado', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockEntity)
    const payload: CriarEmprestimoPayload = {
      descricao: 'Teste',
      nomeTerceiro: 'Joao',
      tipo: 'CONCEDIDO',
      valor: 150,
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
  it('chama apiFetch com PUT no id e payload serializado', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mockEntity)
    const payload: AtualizarEmprestimoPayload = {
      descricao: 'Atualizado',
      nomeTerceiro: null,
      tipo: 'RECEBIDO',
      valor: 200,
      dataEmprestimo: '2026-02-01',
      quitado: true,
    }
    await emprestimoService.atualizar(mockEntity.id, payload)
    expect(apiFetch).toHaveBeenCalledWith(`/api/emprestimos/${mockEntity.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  })
})

describe('emprestimoService.remover', () => {
  it('chama apiFetch com DELETE no id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)
    await emprestimoService.remover(mockEntity.id)
    expect(apiFetch).toHaveBeenCalledWith(`/api/emprestimos/${mockEntity.id}`, {
      method: 'DELETE',
    })
  })
})
