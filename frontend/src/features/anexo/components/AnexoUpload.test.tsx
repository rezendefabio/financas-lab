import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AnexoUpload } from './AnexoUpload'
import type { Anexo } from '../types/anexo'

vi.mock('../services/anexo.service', () => ({
  anexoService: {
    upload: vi.fn(),
  },
}))

import { anexoService } from '../services/anexo.service'

function makeWrapper(client?: QueryClient) {
  const queryClient =
    client ??
    new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    })
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }
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

describe('AnexoUpload', () => {
  beforeEach(() => vi.clearAllMocks())

  it('selecionar arquivo dispara upload e exibe estado de loading', async () => {
    let resolveUpload: (value: Anexo) => void = () => {}
    vi.mocked(anexoService.upload).mockReturnValue(
      new Promise<Anexo>((res) => {
        resolveUpload = res
      }),
    )
    const user = userEvent.setup()

    render(
      <AnexoUpload entidadeTipo="anotacao" entidadeId="entidade-1" />,
      { wrapper: makeWrapper() },
    )

    const file = new File(['conteudo'], 'comprovante.pdf', { type: 'application/pdf' })
    const input = screen.getByLabelText('Selecionar arquivo para upload') as HTMLInputElement
    await user.upload(input, file)

    expect(anexoService.upload).toHaveBeenCalledWith(
      'anotacao',
      'entidade-1',
      file,
    )
    await waitFor(() => expect(screen.getByText(/Enviando/i)).toBeInTheDocument())

    resolveUpload(mockAnexo)
  })

  it('apos sucesso invalida a query e chama onUploadSuccess', async () => {
    vi.mocked(anexoService.upload).mockResolvedValue(mockAnexo)
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    })
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
    const onUploadSuccess = vi.fn()
    const user = userEvent.setup()

    render(
      <AnexoUpload
        entidadeTipo="anotacao"
        entidadeId="entidade-1"
        onUploadSuccess={onUploadSuccess}
      />,
      { wrapper: makeWrapper(queryClient) },
    )

    const file = new File(['conteudo'], 'comprovante.pdf', { type: 'application/pdf' })
    const input = screen.getByLabelText('Selecionar arquivo para upload') as HTMLInputElement
    await user.upload(input, file)

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: ['anexos', 'anotacao', 'entidade-1'],
      })
      expect(onUploadSuccess).toHaveBeenCalled()
    })
  })
})
