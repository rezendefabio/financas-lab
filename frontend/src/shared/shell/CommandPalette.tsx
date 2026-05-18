/**
 * CommandPalette -- busca rapida de telas (Ctrl+K / Cmd+K).
 *
 * Acionado por Ctrl+K / Cmd+K em qualquer lugar do app. Busca em `code`,
 * `title` e `menuPath` das telas do Screen Registry. Selecionar um resultado
 * navega para `screen.path` e fecha o palette.
 *
 * Arquitetura (ADR-014 / spec secao 4.4): o motor de busca/lista e o `Command`
 * do `cmdk`; o overlay e o `<Dialog>` do shadcn base-nova (@base-ui/react),
 * evitando depender do dialog do radix. cmdk fornece focus trap, navegacao por
 * setas e Esc.
 *
 * // UI-3: este componente sera reusado como overlay do menu colapsado mobile
 * // -- a spec (secao 4.4) define palette e menu colapsado como o mesmo
 * // componente, com a arvore de navegacao hierarquica renderizada acima da
 * // lista de resultados. Nesta fase implementamos apenas o modo palette.
 */
'use client'

import { useCallback, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Command } from 'cmdk'
import { Dialog, DialogContent } from '@/shared/components/ui/dialog'
import { resolveIcon } from './icon-map'
import { getAllScreens } from './screens.registry'

export function CommandPalette() {
  const router = useRouter()
  const [open, setOpen] = useState(false)
  const screens = getAllScreens()

  // Listener global de teclado: Ctrl+K / Cmd+K alterna o palette.
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key.toLowerCase() === 'k' && (event.metaKey || event.ctrlKey)) {
        event.preventDefault()
        setOpen((prev) => !prev)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])

  const handleSelect = useCallback(
    (path: string) => {
      setOpen(false)
      router.push(path)
    },
    [router],
  )

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent
        showCloseButton={false}
        className="overflow-hidden p-0 sm:max-w-lg"
        aria-label="Busca de telas"
      >
        <Command
          // cmdk casa o filtro contra o `value` de cada item; incluimos code,
          // title e menuPath no value para busca abrangente (spec secao 4.3).
          label="Busca de telas"
          className="flex flex-col"
        >
          <Command.Input
            autoFocus
            placeholder="Buscar tela por codigo ou nome..."
            aria-label="Buscar tela por codigo ou nome"
            className="h-12 w-full border-b border-border bg-transparent px-4 text-sm outline-none placeholder:text-muted-foreground"
          />
          <Command.List className="max-h-80 overflow-y-auto p-2">
            <Command.Empty className="py-6 text-center text-sm text-muted-foreground">
              Nenhuma tela encontrada.
            </Command.Empty>
            {screens.map((screen) => {
              const Icon = resolveIcon(screen.icon)
              return (
                <Command.Item
                  key={screen.code}
                  value={`${screen.code} ${screen.title} ${screen.menuPath.join(' ')}`}
                  onSelect={() => handleSelect(screen.path)}
                  className="flex cursor-pointer items-center gap-3 rounded-md px-3 py-2 text-sm data-[selected=true]:bg-accent data-[selected=true]:text-accent-foreground"
                >
                  <Icon className="h-4 w-4 shrink-0" />
                  <div className="flex min-w-0 flex-col">
                    <span className="truncate">{screen.title}</span>
                    <span className="truncate text-xs text-muted-foreground">
                      {screen.menuPath.join(' / ')}
                    </span>
                  </div>
                  <span className="ml-auto shrink-0 font-mono text-xs text-muted-foreground">
                    {screen.code}
                  </span>
                </Command.Item>
              )
            })}
          </Command.List>
        </Command>
      </DialogContent>
    </Dialog>
  )
}
