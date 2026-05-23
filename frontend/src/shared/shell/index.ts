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
export { TabBar } from './TabBar'
export { ShellHeader } from './ShellHeader'
export { useBreakpointSidebarCollapse } from './use-breakpoint-sidebar'
export { useSwipeToOpen } from './use-swipe-to-open'
export { useTabsStore, MAX_TABS } from './tabs-store'
export type { Tab } from './tabs-store'
export { useCommandPaletteStore } from './command-palette-store'
export { ErrorBanner } from './ErrorBanner'
export { useErrorBannerStore } from './error-banner-store'
export type { ErrorBannerItem } from './error-banner-store'
