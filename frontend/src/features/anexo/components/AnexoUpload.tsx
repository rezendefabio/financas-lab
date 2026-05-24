'use client'

import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Upload, Loader2 } from 'lucide-react'
import { useRef, useState, type ChangeEvent, type DragEvent } from 'react'
import { Button } from '@/shared/components/ui/button'
import { cn } from '@/shared/lib/utils'
import { anexoService } from '../services/anexo.service'

const LIMITE_MB = 10
const LIMITE_BYTES = LIMITE_MB * 1024 * 1024

export interface AnexoUploadProps {
  entidadeTipo: string
  entidadeId: string
  onUploadSuccess?: () => void
  className?: string
}

export function AnexoUpload({
  entidadeTipo,
  entidadeId,
  onUploadSuccess,
  className,
}: AnexoUploadProps) {
  const queryClient = useQueryClient()
  const inputRef = useRef<HTMLInputElement>(null)
  const [isDragging, setIsDragging] = useState(false)
  const [aviso, setAviso] = useState<string | null>(null)

  const uploadMutation = useMutation({
    mutationFn: (arquivo: File) =>
      anexoService.upload(entidadeTipo, entidadeId, arquivo),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['anexos', entidadeTipo, entidadeId],
      })
      onUploadSuccess?.()
      if (inputRef.current) inputRef.current.value = ''
    },
  })

  function processarArquivo(arquivo: File | undefined) {
    if (!arquivo) return
    if (arquivo.size > LIMITE_BYTES) {
      setAviso(`Arquivo maior que ${LIMITE_MB} MB. Upload pode demorar.`)
    } else {
      setAviso(null)
    }
    uploadMutation.mutate(arquivo)
  }

  function handleInputChange(event: ChangeEvent<HTMLInputElement>) {
    processarArquivo(event.target.files?.[0])
  }

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault()
    setIsDragging(false)
    processarArquivo(event.dataTransfer.files?.[0])
  }

  function handleDragOver(event: DragEvent<HTMLDivElement>) {
    event.preventDefault()
    setIsDragging(true)
  }

  function handleDragLeave() {
    setIsDragging(false)
  }

  return (
    <div className={cn('space-y-2', className)}>
      <div
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        className={cn(
          'flex flex-col items-center justify-center gap-2 rounded-md border border-dashed px-4 py-6 text-center transition-colors',
          isDragging ? 'border-primary bg-primary/5' : 'border-border',
        )}
      >
        <Upload className="h-6 w-6 text-muted-foreground" />
        <p className="text-sm text-muted-foreground">
          Arraste um arquivo aqui ou
        </p>
        <input
          ref={inputRef}
          type="file"
          className="sr-only"
          aria-label="Selecionar arquivo para upload"
          onChange={handleInputChange}
          disabled={uploadMutation.isPending}
        />
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={uploadMutation.isPending}
          onClick={() => inputRef.current?.click()}
        >
          {uploadMutation.isPending ? (
            <>
              <Loader2 className="mr-1 h-3.5 w-3.5 animate-spin" />
              Enviando...
            </>
          ) : (
            'Selecionar arquivo'
          )}
        </Button>
      </div>
      {aviso && <p className="text-xs text-amber-600">{aviso}</p>}
      {uploadMutation.isError && (
        <p className="text-xs text-destructive">
          Falha no upload: {(uploadMutation.error as Error)?.message ?? 'erro desconhecido'}
        </p>
      )}
    </div>
  )
}
