import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { AnexoList } from './AnexoList'
import { anexosService } from '@/shared/services/anexos.service'
import type { Anexo } from '@/shared/types/anexo'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

const anexoExemplo: Anexo = {
  id: 'anexo-1',
  nome: 'comprovante.pdf',
  tipoConteudo: 'application/pdf',
  tamanho: 2048,
  entidadeTipo: 'TRANSACAO',
  entidadeId: 'ent-1',
  criadoEm: '2026-05-17T10:00:00Z',
}

describe('AnexoList', () => {
  beforeEach(() => {
    vi.spyOn(anexosService, 'listar')
    vi.spyOn(anexosService, 'remover')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('exibe mensagem quando nao ha anexos', async () => {
    vi.mocked(anexosService.listar).mockResolvedValue([])

    render(<AnexoList entidadeTipo="TRANSACAO" entidadeId="ent-1" />, {
      wrapper: makeWrapper(),
    })

    expect(await screen.findByText('Nenhum anexo.')).toBeInTheDocument()
  })

  it('lista anexos com nome, tipo e tamanho formatado', async () => {
    vi.mocked(anexosService.listar).mockResolvedValue([anexoExemplo])

    render(<AnexoList entidadeTipo="TRANSACAO" entidadeId="ent-1" />, {
      wrapper: makeWrapper(),
    })

    expect(await screen.findByText('comprovante.pdf')).toBeInTheDocument()
    expect(screen.getByText('application/pdf')).toBeInTheDocument()
    expect(screen.getByText('2.0 KB')).toBeInTheDocument()
  })

  it('renderiza link de download apontando para o endpoint do anexo', async () => {
    vi.mocked(anexosService.listar).mockResolvedValue([anexoExemplo])

    render(<AnexoList entidadeTipo="TRANSACAO" entidadeId="ent-1" />, {
      wrapper: makeWrapper(),
    })

    const link = await screen.findByRole('link', { name: 'Baixar' })
    expect(link).toHaveAttribute(
      'href',
      expect.stringContaining('/api/anexos/anexo-1/download'),
    )
  })

  it('remove anexo e recarrega a lista', async () => {
    vi.mocked(anexosService.listar)
      .mockResolvedValueOnce([anexoExemplo])
      .mockResolvedValueOnce([])
    vi.mocked(anexosService.remover).mockResolvedValue(undefined)

    render(<AnexoList entidadeTipo="TRANSACAO" entidadeId="ent-1" />, {
      wrapper: makeWrapper(),
    })

    const botaoRemover = await screen.findByRole('button', { name: 'Remover' })
    await userEvent.click(botaoRemover)

    await waitFor(() => {
      expect(anexosService.remover).toHaveBeenCalledWith('anexo-1')
    })
    expect(await screen.findByText('Nenhum anexo.')).toBeInTheDocument()
  })

  it('exibe erro quando a listagem falha', async () => {
    vi.mocked(anexosService.listar).mockRejectedValue(new Error('falha'))

    render(<AnexoList entidadeTipo="TRANSACAO" entidadeId="ent-1" />, {
      wrapper: makeWrapper(),
    })

    expect(await screen.findByRole('alert')).toBeInTheDocument()
  })
})
