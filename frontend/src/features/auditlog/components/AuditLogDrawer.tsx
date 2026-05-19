'use client'
import { useState } from 'react'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from '@/shared/components/ui/sheet'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { cn } from '@/shared/lib/utils'
import { formatDateTime } from '@/shared/lib/formatters'
import { useAuditLog } from '../hooks/use-audit-log'
import type { AuditAction, AuditLogEntry } from '../types/auditlog'

interface AuditLogDrawerProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  entityType: string
  entityId: string | null
  entityLabel?: string
}

const ACTION_LABEL: Record<AuditAction, string> = {
  CREATE: 'criou',
  UPDATE: 'atualizou',
  DELETE: 'removeu',
}

const ACTION_ICON: Record<AuditAction, typeof Plus> = {
  CREATE: Plus,
  UPDATE: Pencil,
  DELETE: Trash2,
}

const ACTION_COLOR: Record<AuditAction, string> = {
  CREATE: 'bg-green-100 text-green-700',
  UPDATE: 'bg-blue-100 text-blue-700',
  DELETE: 'bg-red-100 text-red-700',
}

/** Tenta prettificar um JSON; devolve o texto cru se nao for JSON valido. */
function prettyJson(raw: string): string {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

function DiffBlock({ label, value }: { label: string; value: string }) {
  return (
    <div className="mt-2">
      <p className="text-xs font-medium text-muted-foreground">{label}</p>
      <pre className="text-xs font-mono bg-muted p-2 rounded overflow-auto max-h-48">
        {prettyJson(value)}
      </pre>
    </div>
  )
}

function AuditEntryItem({ entry }: { entry: AuditLogEntry }) {
  const [expanded, setExpanded] = useState(false)
  const Icon = ACTION_ICON[entry.action]
  const temDetalhes = entry.before != null || entry.after != null

  return (
    <li className="flex gap-3 py-3 border-b border-border last:border-b-0">
      <span
        className={cn(
          'flex h-8 w-8 shrink-0 items-center justify-center rounded-full',
          ACTION_COLOR[entry.action],
        )}
      >
        <Icon className="h-4 w-4" />
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-sm">
          {entry.userEmail ?? 'Sistema'} - {ACTION_LABEL[entry.action]}
        </p>
        <p className="text-xs text-muted-foreground">{formatDateTime(entry.criadoEm)}</p>
        {entry.screenCode && (
          <Badge variant="secondary" className="mt-1 text-[10px]">
            {entry.screenCode}
          </Badge>
        )}
        {temDetalhes && (
          <div>
            <Button
              variant="ghost"
              size="sm"
              className="mt-1 h-7 px-2 text-xs"
              onClick={() => setExpanded((v) => !v)}
            >
              {expanded ? 'Ocultar detalhes' : 'Ver detalhes'}
            </Button>
            {expanded && (
              <div>
                {entry.before != null && <DiffBlock label="Antes" value={entry.before} />}
                {entry.after != null && <DiffBlock label="Depois" value={entry.after} />}
              </div>
            )}
          </div>
        )}
      </div>
    </li>
  )
}

export function AuditLogDrawer({
  open,
  onOpenChange,
  entityType,
  entityId,
  entityLabel,
}: AuditLogDrawerProps) {
  const { entries, isLoading, isError, hasNextPage, isFetchingNextPage, fetchNextPage } =
    useAuditLog(entityType, open ? entityId : null)

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-full sm:max-w-[480px]">
        <SheetHeader>
          <SheetTitle>Historico de alteracoes</SheetTitle>
          <SheetDescription>
            {entityLabel ?? 'Trilha de auditoria do registro'}
          </SheetDescription>
        </SheetHeader>

        <div className="flex-1 overflow-y-auto px-4 pb-4">
          {isLoading && (
            <div className="space-y-3 py-2">
              {[1, 2, 3].map((i) => (
                <div key={i} className="flex gap-3">
                  <Skeleton className="h-8 w-8 rounded-full" />
                  <div className="flex-1 space-y-2">
                    <Skeleton className="h-4 w-40" />
                    <Skeleton className="h-3 w-24" />
                  </div>
                </div>
              ))}
            </div>
          )}

          {isError && (
            <p className="py-4 text-sm text-destructive">
              Erro ao carregar o historico.
            </p>
          )}

          {!isLoading && !isError && entries.length === 0 && (
            <p className="py-8 text-center text-sm text-muted-foreground">
              Nenhum historico encontrado.
            </p>
          )}

          {!isLoading && !isError && entries.length > 0 && (
            <>
              <ul>
                {entries.map((entry) => (
                  <AuditEntryItem key={entry.id} entry={entry} />
                ))}
              </ul>
              {hasNextPage && (
                <div className="pt-3">
                  <Button
                    variant="outline"
                    size="sm"
                    className="w-full"
                    disabled={isFetchingNextPage}
                    onClick={() => fetchNextPage()}
                  >
                    {isFetchingNextPage ? 'Carregando...' : 'Carregar mais'}
                  </Button>
                </div>
              )}
            </>
          )}
        </div>
      </SheetContent>
    </Sheet>
  )
}
