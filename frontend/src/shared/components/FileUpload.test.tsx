import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { FileUpload } from './FileUpload'
import { anexosService } from '@/shared/services/anexos.service'
import { ApiError } from '@/shared/types/api'

describe('FileUpload', () => {
  beforeEach(() => {
    vi.spyOn(anexosService, 'upload')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function arquivo() {
    return new File(['conteudo'], 'doc.pdf', { type: 'application/pdf' })
  }

  it('renderiza um input do tipo file', () => {
    render(<FileUpload entidadeTipo="TRANSACAO" entidadeId="id-1" />)
    expect(screen.getByLabelText('Arquivo')).toHaveAttribute('type', 'file')
  })

  it('aplica o accept informado via prop', () => {
    render(
      <FileUpload
        entidadeTipo="TRANSACAO"
        entidadeId="id-1"
        accept="application/pdf"
      />,
    )
    expect(screen.getByLabelText('Arquivo')).toHaveAttribute(
      'accept',
      'application/pdf',
    )
  })

  it('faz upload do arquivo selecionado e chama onUploadConcluido', async () => {
    const user = userEvent.setup()
    vi.mocked(anexosService.upload).mockResolvedValue({} as never)
    const onUploadConcluido = vi.fn()
    render(
      <FileUpload
        entidadeTipo="TRANSACAO"
        entidadeId="id-1"
        onUploadConcluido={onUploadConcluido}
      />,
    )

    await user.upload(screen.getByLabelText('Arquivo'), arquivo())

    await waitFor(() => {
      expect(anexosService.upload).toHaveBeenCalledWith(
        expect.any(File),
        'TRANSACAO',
        'id-1',
      )
    })
    await waitFor(() => expect(onUploadConcluido).toHaveBeenCalled())
  })

  it('rejeita arquivo acima de 10MB sem chamar o servico de upload', async () => {
    const user = userEvent.setup()
    const arquivoGrande = new File(
      [new Uint8Array(10 * 1024 * 1024 + 1)],
      'grande.pdf',
      { type: 'application/pdf' },
    )
    render(<FileUpload entidadeTipo="TRANSACAO" entidadeId="id-1" />)

    await user.upload(screen.getByLabelText('Arquivo'), arquivoGrande)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Arquivo excede o limite de 10MB.',
    )
    expect(anexosService.upload).not.toHaveBeenCalled()
  })

  it('exibe mensagem de erro quando o upload falha', async () => {
    const user = userEvent.setup()
    vi.mocked(anexosService.upload).mockRejectedValue(
      new ApiError(400, 'Arquivo invalido'),
    )
    render(<FileUpload entidadeTipo="TRANSACAO" entidadeId="id-1" />)

    await user.upload(screen.getByLabelText('Arquivo'), arquivo())

    expect(await screen.findByRole('alert')).toHaveTextContent('Arquivo invalido')
  })
})
