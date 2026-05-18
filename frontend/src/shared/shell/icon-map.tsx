/**
 * Mapa explicito de `icon: string` (do screens.registry) para o componente
 * lucide-react correspondente.
 *
 * Mapa estatico e intencional: NAO usar import dinamico arbitrario (ADR-014 /
 * instrucoes UI-1). Ao registrar uma tela com um icone novo, adicionar a
 * entrada aqui.
 */
import { createElement } from 'react'
import {
  Home,
  CreditCard,
  ArrowLeftRight,
  Tag,
  Tags,
  Wallet,
  Target,
  Repeat,
  Users,
  StickyNote,
  BarChart3,
  AlertTriangle,
  Upload,
  Folder,
  type LucideIcon,
} from 'lucide-react'

/** Mapa nome -> componente de icone. */
export const iconMap: Record<string, LucideIcon> = {
  home: Home,
  'credit-card': CreditCard,
  'arrow-left-right': ArrowLeftRight,
  tag: Tag,
  tags: Tags,
  wallet: Wallet,
  target: Target,
  repeat: Repeat,
  users: Users,
  'sticky-note': StickyNote,
  'bar-chart-3': BarChart3,
  'alert-triangle': AlertTriangle,
  upload: Upload,
  folder: Folder,
}

/**
 * Resolve o componente de icone para um nome. Faz fallback para `Folder`
 * quando o nome nao esta mapeado (icone de grupo / desconhecido).
 */
export function resolveIcon(name: string): LucideIcon {
  return iconMap[name] ?? Folder
}

/**
 * Renderiza o icone de uma tela a partir do nome (campo `icon` do registry).
 *
 * Componente declarado em escopo de modulo (nao dentro de render). Usa
 * `createElement` em vez de instanciar um componente capitalizado local
 * (`const Icon = ...`) para satisfazer a regra de lint
 * `react-hooks/static-components`, que proibe criar componentes durante o render.
 */
export function ScreenIcon({
  name,
  className,
}: {
  name: string
  className?: string
}) {
  return createElement(resolveIcon(name), { className })
}
