/**
 * Construcao da arvore de menu a partir do Screen Registry.
 *
 * Transforma a lista plana de `ScreenDefinition` (cada uma com seu `menuPath`)
 * numa arvore de no maximo 3 niveis, agrupando por `menuPath`. Usado pelo
 * SidebarNav e pelo CommandPalette.
 */
import type { ScreenDefinition } from './screens.registry'
import { getAllScreens, MAX_MENU_DEPTH } from './screens.registry'

/** No da arvore de menu: ou um grupo com filhos, ou uma folha (tela). */
export interface MenuNode {
  /** Rotulo exibido no menu (ultimo segmento do menuPath para grupos). */
  label: string
  /** Chave estavel e unica do no (menuPath completo ate aqui, com `/`). */
  key: string
  /** Tela associada -- presente apenas em folhas. */
  screen?: ScreenDefinition
  /** Sub-nos -- presente apenas em grupos. */
  children: MenuNode[]
}

/**
 * Constroi a arvore de menu a partir das telas informadas (default: todas
 * do registry). O ultimo segmento de `menuPath` e a folha (a tela); os
 * segmentos anteriores sao grupos.
 *
 * menuPath com mais de MAX_MENU_DEPTH niveis e truncado ao limite -- a
 * validacao de profundidade e responsabilidade da skill /register-screen,
 * aqui apenas garantimos que a UI nunca renderiza mais que 3 niveis.
 */
export function buildMenuTree(
  screensInput: ScreenDefinition[] = getAllScreens(),
): MenuNode[] {
  const roots: MenuNode[] = []

  for (const screen of screensInput) {
    const segments = screen.menuPath.slice(0, MAX_MENU_DEPTH)
    if (segments.length === 0) {
      continue
    }

    let level = roots
    let keyPrefix = ''

    segments.forEach((segment, index) => {
      const isLeaf = index === segments.length - 1
      keyPrefix = keyPrefix ? `${keyPrefix}/${segment}` : segment

      let node = level.find((candidate) => candidate.key === keyPrefix)
      if (!node) {
        node = { label: segment, key: keyPrefix, children: [] }
        level.push(node)
      }
      if (isLeaf) {
        node.screen = screen
      }
      level = node.children
    })
  }

  return roots
}

/**
 * Retorna o conjunto de chaves de grupo (key) que contem -- direta ou
 * indiretamente -- a tela cujo `path` casa com o caminho ativo. Usado para
 * destacar o "breadcrumb" do item ativo no menu.
 */
export function findActiveTrail(
  tree: MenuNode[],
  activePath: string | undefined,
): Set<string> {
  const trail = new Set<string>()
  if (!activePath) {
    return trail
  }

  const visit = (node: MenuNode): boolean => {
    if (node.screen && node.screen.path === activePath) {
      return true
    }
    const hasActiveChild = node.children.some(visit)
    if (hasActiveChild) {
      trail.add(node.key)
    }
    return hasActiveChild
  }

  tree.forEach(visit)
  return trail
}
