import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AnexoList } from './AnexoList'
import type { Anexo } from '../types/anexo'

vi.mock('../services/anexo.service', () => ({
  anexoService: {
    listarPorEntidade: vi.fn(),
    remover: vi.fn(),
    downloadUrl: (id: string) => `/api/anexos/${id}/download`,
  },
}))

import { anexoService } from '../services/anexo.service'

function makeWrapper() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
  }
}

function renderList() {
  return render(
    <AnexoList entidadeTipo="anotacao" entidadeId="entidade-1" />,
    { wrapper: makeWrapper() },
  )
}

const mockAnexo: Anexo = {
  id: 'anexo-1',
  nome: 'comprovante.pdf',
  tipoConteudo: 'application/pdf',
  tamanho: 2048,
  entidadeTipo: 'anotacao',
  entidadeId: 'entidade-1',
  criadoEm: '2026-01-01T00:00:00Z',
}

describe('AnexoList', () => {
  beforeEach(() => vi.clearAllMocks())

  it('exibe "Nenhum anexo." quando a lista esta vazia', async () => {
    vi.mocked(anexoService.listarPorEntidade).mockResolvedValue([])

    renderList()

    await waitFor(() =>
      expect(screen.getByText('Nenhum anexo.')).toBeInTheDocument(),
    )
  })

  it('renderiza nome do arquivo e link de download para cada item', async () => {
    vi.mocked(anexoService.listarPorEntidade).mockResolvedValue([mockAnexo])

    renderList()

    await waitFor(() =>
      expect(screen.getByText('comprovante.pdf')).toBeInTheDocument(),
    )
    const downloadLink = screen.getByLabelText('Baixar comprovante.pdf')
    expect(downloadLink).toHaveAttribute('href', '/api/anexos/anexo-1/download')
    expect(downloadLink).toHaveAttribute('target', '_blank')
    expect(screen.getByText('2.0 KB')).toBeInTheDocument()
  })

  it('clique em excluir chama anexoService.remover com id correto', async () => {
    vi.mocked(anexoService.listarPorEntidade).mockResolvedValue([mockAnexo])
    vi.mocked(anexoService.remover).mockResolvedValue(undefined)
    const user = userEvent.setup()

    renderList()

    await waitFor(() =>
      expect(screen.getByText('comprovante.pdf')).toBeInTheDocument(),
    )
    await user.click(screen.getByLabelText('Excluir comprovante.pdf'))

    await waitFor(() =>
      expect(anexoService.remover).toHaveBeenCalledWith('anexo-1'),
    )
  })
})
