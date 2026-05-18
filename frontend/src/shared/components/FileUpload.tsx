'use client'

import { useId, useState, type ChangeEvent } from 'react'
import { Input } from '@/shared/components/ui/input'
import { Label } from '@/shared/components/ui/label'
import { anexosService } from '@/shared/services/anexos.service'
import { ApiError } from '@/shared/types/api'

/** Limite de tamanho de arquivo aceito pelo backend (FazerUploadAnexoUseCase). */
const MAX_TAMANHO_BYTES = 10 * 1024 * 1024

interface FileUploadProps {
  entidadeTipo: string
  entidadeId: string
  /**
   * Filtro de tipos aceitos para o seletor de arquivo (atributo HTML `accept`).
   * Quando omitido, qualquer tipo de arquivo e aceito.
   */
  accept?: string
  onUploadConcluido?: () => void
}

function FileUpload({
  entidadeTipo,
  entidadeId,
  accept,
  onUploadConcluido,
}: FileUploadProps) {
  const inputId = useId()
  const [uploading, setUploading] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  // Remontar o input apos cada upload limpa o arquivo selecionado.
  const [inputKey, setInputKey] = useState(0)

  async function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const arquivo = event.target.files?.[0]
    if (!arquivo) return

    if (arquivo.size > MAX_TAMANHO_BYTES) {
      setErro('Arquivo excede o limite de 10MB.')
      // Remontar o input descarta o arquivo selecionado; upload nao prossegue.
      setInputKey((k) => k + 1)
      return
    }

    setUploading(true)
    setErro(null)
    try {
      await anexosService.upload(arquivo, entidadeTipo, entidadeId)
      setInputKey((k) => k + 1)
      onUploadConcluido?.()
    } catch (e) {
      const mensagem =
        e instanceof ApiError ? e.message : 'Falha ao enviar o arquivo.'
      setErro(mensagem)
    } finally {
      setUploading(false)
    }
  }

  return (
    <div className="space-y-2">
      <Label htmlFor={inputId}>Arquivo</Label>
      <Input
        key={inputKey}
        id={inputId}
        type="file"
        accept={accept}
        disabled={uploading}
        onChange={handleChange}
      />
      {uploading && (
        <p className="text-sm text-muted-foreground" role="status">
          Enviando...
        </p>
      )}
      {erro && (
        <p className="text-sm text-destructive" role="alert">
          {erro}
        </p>
      )}
    </div>
  )
}

export { FileUpload }
export default FileUpload
