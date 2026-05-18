/**
 * SidebarNav -- menu lateral hierarquico declarativo.
 *
 * Consome o Screen Registry (`getAllScreens`), monta a arvore por `menuPath`
 * (`buildMenuTree`) e renderiza grupos colapsaveis usando os componentes
 * `Sidebar*` do shadcn base-nova. Suporta ate 3 niveis (spec secao 4.1).
 *
 * - Item ativo destacado comparando `findScreenByPath(usePathname())`.
 * - Grupos no caminho do item ativo tambem destacados (breadcrumb visual).
 * - Estado colapsado dos grupos persiste em localStorage via useSidebarStore.
 * - Campo de busca fixo filtra itens por titulo ou codigo em tempo real.
 */
'use client'

import { useState } from 'react'
import { usePathname } from 'next/navigation'
import { ChevronRight, Search } from 'lucide-react'
import {
  SidebarMenu,
  SidebarMenuItem,
  SidebarMenuButton,
  SidebarMenuSub,
  SidebarMenuSubItem,
  SidebarMenuSubButton,
} from '@/shared/components/ui/sidebar'
import { Input } from '@/shared/components/ui/input'
import { cn } from '@/shared/lib/utils'
import { getAllScreens, findScreenByPath } from './screens.registry'
import { buildMenuTree, findActiveTrail, type MenuNode } from './menu-tree'
import { ScreenIcon } from './icon-map'
import { useSidebarStore } from './sidebar-store'
import { useTabsStore } from './tabs-store'

/**
 * Filtra a arvore de menu por query (case-insensitive).
 * Grupos sem filhos apos filtro sao omitidos.
 * Folhas: incluidas se titulo ou code da tela contiver a query.
 */
function filterTree(nodes: MenuNode[], q: string): MenuNode[] {
  const lower = q.toLowerCase()
  return nodes.flatMap((node) => {
    if (!node.children || node.children.length === 0) {
      const titleMatch = node.label.toLowerCase().includes(lower)
      const codeMatch = node.screen?.code.toLowerCase().includes(lower) ?? false
      return titleMatch || codeMatch ? [node] : []
    }
    const filteredChildren = filterTree(node.children, q)
    return filteredChildren.length > 0
      ? [{ ...node, children: filteredChildren }]
      : []
  })
}

interface NodeProps {
  node: MenuNode
  depth: number
  activePath: string | undefined
  activeTrail: Set<string>
  /** Quando true, o grupo e forçado aberto (busca ativa). */
  forceOpen?: boolean
}

function MenuTreeNode({ node, depth, activePath, activeTrail, forceOpen }: NodeProps) {
  const collapsed = useSidebarStore((state) => state.collapsed)
  const toggleGroup = useSidebarStore((state) => state.toggleGroup)
  const openTab = useTabsStore((state) => state.openTab)

  // Folha: tela navegavel. Clicar abre uma aba (Tab Manager, UI-2);
  // a navegacao acontece via efeito reativo no layout.
  if (node.screen) {
    const screen = node.screen
    const isActive = screen.path === activePath

    if (depth === 0) {
      return (
        <SidebarMenuItem>
          <SidebarMenuButton
            onClick={() => openTab(screen.code)}
            isActive={isActive}
            className="cursor-pointer"
          >
            <ScreenIcon name={screen.icon} className="h-4 w-4" />
            <span>{screen.title}</span>
          </SidebarMenuButton>
        </SidebarMenuItem>
      )
    }

    return (
      <SidebarMenuSubItem>
        <SidebarMenuSubButton
          onClick={() => openTab(screen.code)}
          isActive={isActive}
          className="cursor-pointer"
        >
          <ScreenIcon name={screen.icon} className="h-4 w-4" />
          <span>{screen.title}</span>
        </SidebarMenuSubButton>
      </SidebarMenuSubItem>
    )
  }

  // Grupo: colapsavel. Quando forceOpen, ignora collapsed store.
  const isOpen = forceOpen ? true : !collapsed.includes(node.key)
  const isOnActiveTrail = activeTrail.has(node.key)

  return (
    <SidebarMenuItem>
      <SidebarMenuButton
        onClick={() => toggleGroup(node.key)}
        isActive={isOnActiveTrail}
        aria-expanded={isOpen}
        aria-controls={`menu-group-${node.key}`}
        className="cursor-pointer"
      >
        <ChevronRight
          className={cn(
            'h-4 w-4 transition-transform',
            isOpen && 'rotate-90',
          )}
        />
        <span>{node.label}</span>
      </SidebarMenuButton>
      {isOpen && (
        <SidebarMenuSub id={`menu-group-${node.key}`}>
          {node.children.map((child) => (
            <MenuTreeNode
              key={child.key}
              node={child}
              depth={depth + 1}
              activePath={activePath}
              activeTrail={activeTrail}
              forceOpen={forceOpen}
            />
          ))}
        </SidebarMenuSub>
      )}
    </SidebarMenuItem>
  )
}

export function SidebarNav() {
  const pathname = usePathname()
  const activeScreen = findScreenByPath(pathname)
  const activePath = activeScreen?.path

  const [query, setQuery] = useState('')

  const tree = buildMenuTree(getAllScreens())
  const activeTrail = findActiveTrail(tree, activePath)

  const hasQuery = query.trim().length > 0
  const displayTree = hasQuery ? filterTree(tree, query.trim()) : tree

  return (
    <div>
      <div className="px-3 pb-2">
        <div className="relative">
          <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Buscar tela..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="pl-8 h-9 text-sm"
          />
        </div>
      </div>
      {hasQuery && displayTree.length === 0 ? (
        <p className="px-3 py-4 text-sm text-muted-foreground text-center">
          Nenhuma tela encontrada.
        </p>
      ) : (
        <SidebarMenu>
          {displayTree.map((node) => (
            <MenuTreeNode
              key={node.key}
              node={node}
              depth={0}
              activePath={activePath}
              activeTrail={activeTrail}
              forceOpen={hasQuery}
            />
          ))}
        </SidebarMenu>
      )}
    </div>
  )
}
