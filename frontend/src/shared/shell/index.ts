/**
 * Barrel do pacote `shell` -- shell declarativo da aplicacao (ADR-014, fase UI-1).
 */
export type { ScreenDefinition } from './screens.registry'
export {
  screens,
  getAllScreens,
  findScreenByCode,
  findScreenByPath,
  MAX_MENU_DEPTH,
  SCREEN_CODE_REGEX,
} from './screens.registry'
export type { MenuNode } from './menu-tree'
export { buildMenuTree, findActiveTrail } from './menu-tree'
export { iconMap, resolveIcon, ScreenIcon } from './icon-map'
export { useSidebarStore } from './sidebar-store'
export { SidebarNav } from './SidebarNav'
export { CommandPalette } from './CommandPalette'
