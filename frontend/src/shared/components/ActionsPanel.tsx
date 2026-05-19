'use client'

/**
 * ActionsPanel -- painel de acoes de tela (ADR-014, fase UI-5).
 *
 * Agrupa as acoes transversais de uma tela de listagem: abrir o historico de
 * auditoria (`AuditLogDrawer`), exportar CSV e imprimir. Em telas largas exibe
 * botoes lado a lado; em mobile (< 768px) colapsa num `<DropdownMenu>`.
 *
 * O estado do `AuditLogDrawer` e gerenciado internamente.
 */

import * as React from 'react'
import { Download, History, MoreHorizontal, Printer } from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/shared/components/ui/dropdown-menu'
import { useIsMobile } from '@/shared/hooks/use-mobile'
import { AuditLogDrawer } from '@/features/auditlog'

export interface ActionsPanelProps {
  /** Tipo de entidade para a trilha de auditoria. Se ausente, a acao Log some. */
  entityType?: string
  /** Id do registro selecionado. Se `null`, a acao Log fica desabilitada. */
  entityId?: string | null
  /** Rotulo do registro exibido no cabecalho do drawer. */
  entityLabel?: string
  /** Code da tela atual (informativo / extensoes futuras). */
  screenCode?: string
  /** Handler de exportacao CSV. Se ausente, o botao fica desabilitado. */
  onExportCsv?: () => void
  /** Handler de impressao. Se ausente, o botao fica oculto. */
  onPrint?: () => void
  /** Slot para acoes customizadas da tela. */
  extraActions?: React.ReactNode
}

export function ActionsPanel({
  entityType,
  entityId,
  entityLabel,
  screenCode: _screenCode,
  onExportCsv,
  onPrint,
  extraActions,
}: ActionsPanelProps) {
  const isMobile = useIsMobile()
  const [auditOpen, setAuditOpen] = React.useState(false)

  const showLog = Boolean(entityType)
  const logDisabled = !entityId

  const auditDrawer = showLog ? (
    <AuditLogDrawer
      open={auditOpen}
      onOpenChange={setAuditOpen}
      entityType={entityType as string}
      entityId={entityId ?? null}
      entityLabel={entityLabel}
    />
  ) : null

  if (isMobile) {
    return (
      <>
        <DropdownMenu>
          <DropdownMenuTrigger
            render={
              <Button
                type="button"
                variant="outline"
                size="sm"
                aria-label="Acoes"
              >
                <MoreHorizontal className="h-3.5 w-3.5" />
              </Button>
            }
          />
          <DropdownMenuContent align="end">
            {showLog && (
              <DropdownMenuItem
                disabled={logDisabled}
                onClick={() => setAuditOpen(true)}
              >
                <History className="h-4 w-4" />
                Log
              </DropdownMenuItem>
            )}
            <DropdownMenuItem disabled={!onExportCsv} onClick={onExportCsv}>
              <Download className="h-4 w-4" />
              Exportar CSV
            </DropdownMenuItem>
            {onPrint && (
              <DropdownMenuItem onClick={onPrint}>
                <Printer className="h-4 w-4" />
                Imprimir
              </DropdownMenuItem>
            )}
          </DropdownMenuContent>
        </DropdownMenu>
        {extraActions}
        {auditDrawer}
      </>
    )
  }

  return (
    <>
      <div className="flex items-center gap-2">
        {showLog && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={logDisabled}
            onClick={() => setAuditOpen(true)}
          >
            <History className="h-3.5 w-3.5" />
            Log
          </Button>
        )}
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={!onExportCsv}
          onClick={onExportCsv}
        >
          <Download className="h-3.5 w-3.5" />
          Exportar CSV
        </Button>
        {onPrint && (
          <Button type="button" variant="outline" size="sm" onClick={onPrint}>
            <Printer className="h-3.5 w-3.5" />
            Imprimir
          </Button>
        )}
        {extraActions}
      </div>
      {auditDrawer}
    </>
  )
}
