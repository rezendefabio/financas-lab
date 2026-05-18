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
 */
'use client'

import { usePathname } from 'next/navigation'
import Link from 'next/link'
import { ChevronRight } from 'lucide-react'
import {
  SidebarMenu,
  SidebarMenuItem,
  SidebarMenuButton,
  SidebarMenuSub,
  SidebarMenuSubItem,
  SidebarMenuSubButton,
} from '@/shared/components/ui/sidebar'
import { cn } from '@/shared/lib/utils'
import { getAllScreens, findScreenByPath } from './screens.registry'
import { buildMenuTree, findActiveTrail, type MenuNode } from './menu-tree'
import { ScreenIcon } from './icon-map'
import { useSidebarStore } from './sidebar-store'

interface NodeProps {
  node: MenuNode
  depth: number
  activePath: string | undefined
  activeTrail: Set<string>
}

function MenuTreeNode({ node, depth, activePath, activeTrail }: NodeProps) {
  const collapsed = useSidebarStore((state) => state.collapsed)
  const toggleGroup = useSidebarStore((state) => state.toggleGroup)

  // Folha: tela navegavel.
  if (node.screen) {
    const screen = node.screen
    const isActive = screen.path === activePath

    if (depth === 0) {
      return (
        <SidebarMenuItem>
          <SidebarMenuButton
            render={<Link href={screen.path} />}
            isActive={isActive}
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
          render={<Link href={screen.path} />}
          isActive={isActive}
        >
          <ScreenIcon name={screen.icon} className="h-4 w-4" />
          <span>{screen.title}</span>
        </SidebarMenuSubButton>
      </SidebarMenuSubItem>
    )
  }

  // Grupo: colapsavel.
  const isOpen = !collapsed.includes(node.key)
  const isOnActiveTrail = activeTrail.has(node.key)

  return (
    <SidebarMenuItem>
      <SidebarMenuButton
        onClick={() => toggleGroup(node.key)}
        isActive={isOnActiveTrail}
        aria-expanded={isOpen}
        aria-controls={`menu-group-${node.key}`}
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

  const tree = buildMenuTree(getAllScreens())
  const activeTrail = findActiveTrail(tree, activePath)

  return (
    <SidebarMenu>
      {tree.map((node) => (
        <MenuTreeNode
          key={node.key}
          node={node}
          depth={0}
          activePath={activePath}
          activeTrail={activeTrail}
        />
      ))}
    </SidebarMenu>
  )
}
