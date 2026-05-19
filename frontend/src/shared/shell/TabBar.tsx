/**
 * TabBar -- faixa de abas internas do app (ADR-014 fase 2, spec secao 4.2).
 *
 * Renderiza as abas abertas (`useTabsStore`), permite ativar, fechar, fixar,
 * duplicar, reordenar (drag-and-drop HTML5 nativo) e abrir novas (botao "+",
 * que dispara o CommandPalette). Sincroniza o estado das abas com a URL
 * (`?tabs=CODE1,CODE2&active=CODE`); a URL tem prioridade sobre o localStorage
 * na montagem.
 *
 * Responsividade: >= 768px a faixa horizontal completa com scroll; < 768px
 * apenas a aba ativa + dropdown com a lista.
 */
'use client'

import { useEffect, useRef, useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { ChevronDown, Pin, Plus, X } from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
} from '@/shared/components/ui/dropdown-menu'
import { cn } from '@/shared/lib/utils'
import { findScreenByCode } from './screens.registry'
import { useTabsStore, type Tab } from './tabs-store'
import { useCommandPaletteStore } from './command-palette-store'

/** Le `?tabs=...&active=...` da URL. Retorna null quando nao ha o parametro. */
function readTabsFromParams(
  params: URLSearchParams,
): { codes: string[]; active: string | null } | null {
  const raw = params.get('tabs')
  if (!raw) return null
  const codes = raw
    .split(',')
    .map((code) => code.trim())
    .filter((code) => code.length > 0 && findScreenByCode(code) !== undefined)
  if (codes.length === 0) return null
  return { codes, active: params.get('active') }
}

/** Item de aba individual. */
function TabItem({
  tab,
  index,
  isActive,
  onDragStart,
  onDragOver,
  onDrop,
}: {
  tab: Tab
  index: number
  isActive: boolean
  onDragStart: (index: number) => void
  onDragOver: (event: React.DragEvent) => void
  onDrop: (index: number) => void
}) {
  const screen = findScreenByCode(tab.screenCode)
  const setActive = useTabsStore((state) => state.setActive)
  const closeTab = useTabsStore((state) => state.closeTab)
  const togglePin = useTabsStore((state) => state.togglePin)
  const duplicateTab = useTabsStore((state) => state.duplicateTab)
  const tabs = useTabsStore((state) => state.tabs)

  const [menuOpen, setMenuOpen] = useState(false)

  if (!screen) return null

  const closeOthers = () => {
    tabs
      .filter((other) => other.id !== tab.id)
      .forEach((other) => closeTab(other.id))
  }

  return (
    <DropdownMenu open={menuOpen} onOpenChange={setMenuOpen}>
      <div
        role="tab"
        aria-selected={isActive}
        data-active={isActive ? '' : undefined}
        draggable
        onDragStart={() => onDragStart(index)}
        onDragOver={onDragOver}
        onDrop={() => onDrop(index)}
        onContextMenu={(event) => {
          event.preventDefault()
          setMenuOpen(true)
        }}
        onClick={() => setActive(tab.id)}
        className={cn(
          'group/tab flex shrink-0 cursor-pointer items-center gap-1.5 border-b-2 border-transparent px-3 py-2 text-sm whitespace-nowrap transition-colors',
          isActive
            ? 'border-b-primary bg-background font-medium text-foreground'
            : 'text-muted-foreground hover:bg-muted hover:text-foreground',
        )}
      >
        <span className="truncate">{screen.title}</span>
        {tab.pinned ? (
          <button
            type="button"
            aria-label={`Desfixar aba ${screen.title}`}
            onClick={(event) => {
              event.stopPropagation()
              togglePin(tab.id)
            }}
            className="rounded p-0.5 text-muted-foreground hover:text-foreground"
          >
            <Pin className="h-3 w-3 fill-current" />
          </button>
        ) : (
          <button
            type="button"
            aria-label={`Fechar aba ${screen.title}`}
            onClick={(event) => {
              event.stopPropagation()
              closeTab(tab.id)
            }}
            className="rounded p-0.5 opacity-60 hover:opacity-100"
          >
            <X className="h-3 w-3" />
          </button>
        )}
        {/* Trigger invisivel: ancora o menu de contexto a esta aba.
            base-nova exige <button> nativo quando nativeButton=true. */}
        <DropdownMenuTrigger
          render={<button type="button" className="sr-only" aria-hidden tabIndex={-1} />}
        />
      </div>
      <DropdownMenuContent>
        <DropdownMenuItem onClick={() => closeTab(tab.id)}>
          Fechar
        </DropdownMenuItem>
        <DropdownMenuItem onClick={closeOthers}>
          Fechar outras
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => duplicateTab(tab.id)}>
          Duplicar
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={() => togglePin(tab.id)}>
          {tab.pinned ? 'Desfixar' : 'Fixar'}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

export function TabBar() {
  const tabs = useTabsStore((state) => state.tabs)
  const activeId = useTabsStore((state) => state.activeId)
  const reorder = useTabsStore((state) => state.reorder)
  const setActive = useTabsStore((state) => state.setActive)
  const closeTab = useTabsStore((state) => state.closeTab)
  const openPalette = useCommandPaletteStore((state) => state.setOpen)

  const router = useRouter()
  const searchParams = useSearchParams()

  const [dragIndex, setDragIndex] = useState<number | null>(null)
  /** Garante que a leitura URL -> store rode apenas uma vez (na montagem). */
  const hydratedFromUrl = useRef(false)

  // Montagem: URL tem prioridade sobre o localStorage.
  useEffect(() => {
    if (hydratedFromUrl.current) return
    hydratedFromUrl.current = true
    const fromUrl = readTabsFromParams(searchParams)
    if (!fromUrl) return
    const urlTabs: Tab[] = fromUrl.codes.map((screenCode) => ({
      id:
        typeof crypto !== 'undefined' && crypto.randomUUID
          ? crypto.randomUUID()
          : Math.random().toString(36).slice(2),
      screenCode,
      pinned: false,
    }))
    const active =
      urlTabs.find((tab) => tab.screenCode === fromUrl.active)?.id ??
      urlTabs[0]?.id ??
      null
    useTabsStore.setState({ tabs: urlTabs, activeId: active })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Mudancas no store -> URL + navegacao (replace unico, sem poluir historico).
  // Usar o path da screen ATIVA como destino evita a race condition com o
  // efeito de navegacao reativa no layout (que foi removido — UI-2 fix).
  useEffect(() => {
    if (!hydratedFromUrl.current) return
    const activeTab = tabs.find((tab) => tab.id === activeId)
    const targetScreen = activeTab
      ? findScreenByCode(activeTab.screenCode)
      : undefined
    const targetPath = targetScreen?.path ?? window.location.pathname

    const params = new URLSearchParams()
    if (tabs.length > 0) {
      params.set('tabs', tabs.map((tab) => tab.screenCode).join(','))
      if (activeTab) params.set('active', activeTab.screenCode)
    }
    const query = params.toString()
    const url = query ? `${targetPath}?${query}` : targetPath
    router.replace(url)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tabs, activeId])

  if (tabs.length === 0) {
    return null
  }

  const activeTab = tabs.find((tab) => tab.id === activeId)
  const activeScreen = activeTab
    ? findScreenByCode(activeTab.screenCode)
    : undefined

  const handleDrop = (targetIndex: number) => {
    if (dragIndex !== null && dragIndex !== targetIndex) {
      reorder(dragIndex, targetIndex)
    }
    setDragIndex(null)
  }

  return (
    <div className="tab-bar-root flex items-center border-b border-border bg-muted/30">
      {/* >= 768px: faixa horizontal scrollavel. */}
      <div
        role="tablist"
        aria-label="Abas abertas"
        className="hidden flex-1 items-center overflow-x-auto md:flex [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        {tabs.map((tab, index) => (
          <TabItem
            key={tab.id}
            tab={tab}
            index={index}
            isActive={tab.id === activeId}
            onDragStart={setDragIndex}
            onDragOver={(event) => event.preventDefault()}
            onDrop={handleDrop}
          />
        ))}
      </div>

      {/* < 768px: aba ativa + dropdown com a lista. */}
      <div className="flex flex-1 items-center md:hidden">
        <DropdownMenu>
          <DropdownMenuTrigger
            render={
              <button
                type="button"
                className="flex flex-1 items-center justify-between gap-2 px-3 py-2 text-sm"
              >
                <span className="truncate font-medium">
                  {activeScreen?.title ?? 'Abas'}
                </span>
                <span className="flex items-center gap-1 text-muted-foreground">
                  {tabs.length} abas
                  <ChevronDown className="h-4 w-4" />
                </span>
              </button>
            }
          />
          <DropdownMenuContent>
            {tabs.map((tab) => {
              const screen = findScreenByCode(tab.screenCode)
              if (!screen) return null
              return (
                <DropdownMenuItem
                  key={tab.id}
                  onClick={() => setActive(tab.id)}
                  className={cn(tab.id === activeId && 'bg-accent')}
                >
                  {tab.pinned && <Pin className="h-3 w-3 fill-current" />}
                  <span className="flex-1 truncate">{screen.title}</span>
                  <button
                    type="button"
                    aria-label={`Fechar aba ${screen.title}`}
                    onClick={(event) => {
                      event.stopPropagation()
                      closeTab(tab.id)
                    }}
                  >
                    <X className="h-3 w-3" />
                  </button>
                </DropdownMenuItem>
              )
            })}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {/* Botao "+": abre o CommandPalette para escolher nova tela. */}
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        aria-label="Abrir nova aba"
        className="mx-1 shrink-0"
        onClick={() => openPalette(true)}
      >
        <Plus className="h-4 w-4" />
      </Button>
    </div>
  )
}
