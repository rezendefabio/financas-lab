/**
 * Screen Registry -- manifesto declarativo de todas as telas da aplicacao.
 *
 * Fonte de verdade: docs/frontend-master-spec.md (secao 4.1 e 4.3) e ADR-014.
 *
 * Toda tela da aplicacao deve ter uma entrada aqui. Novas telas sao registradas
 * via skill /register-screen, que valida unicidade do `code` e injeta a entrada.
 *
 * --- Criterio de codificacao (MOD-ENT-NNN) ---
 *
 * Formato do `code`: MOD-ENT-NNN, regex `^[A-Z]{3}-[A-Z]{3}-\d{3}$`.
 *
 *  - MOD = modulo (3 letras):
 *      FIN = financeiro (contas, transacoes, orcamentos, metas, recorrentes, importacao)
 *      CAD = cadastros  (categorias, tags, beneficiarios)
 *      REL = relatorios e analise (dashboard, relatorios, anotacoes)
 *      ADM = administracao (incidentes)
 *  - ENT = entidade (3 letras): CTA=Conta, TRX=Transacao, ORC=Orcamento,
 *      MET=Meta, REC=Recorrente, IMP=Importacao, CAT=Categoria, TAG=Tag,
 *      BEN=Beneficiario, CCU=CentroCusto, DSH=Dashboard, ANL=Analise/Relatorios,
 *      ANO=Anotacao, INC=Incidente.
 *  - NNN = sequencial dentro da entidade: 001 = listagem (tela principal).
 *      Cadastro/visualizacao/edicao herdam a mesma rota base nesta fase e nao
 *      tem code proprio -- apenas a tela de listagem entra no registry/menu.
 */

export interface ScreenDefinition {
  /** Codigo curto unico no formato MOD-ENT-NNN. */
  code: string
  /** Titulo legivel da tela. */
  title: string
  /** Rota da tela no App Router. */
  path: string
  /** Hierarquia de menu, do grupo raiz ate o item. Max 3 niveis. */
  menuPath: string[]
  /** Nome do icone lucide-react (mapeado em icon-map.ts). */
  icon: string
  /** Permissoes RBAC -- vazio nesta fase (ADR-014 decisao 9). */
  permissions: string[]
}

/** Profundidade maxima de menuPath permitida (spec secao 4.1). */
export const MAX_MENU_DEPTH = 3

/** Regex de validacao do codigo de tela (spec secao 4.3). */
export const SCREEN_CODE_REGEX = /^[A-Z]{3}-[A-Z]{3}-\d{3}$/

/**
 * Manifesto das telas existentes.
 *
 * Novas entradas sao injetadas pela skill /register-screen, que preserva
 * a ordem e a formatacao deste array.
 */
export const screens: ScreenDefinition[] = [
  {
    code: 'REL-DSH-001',
    title: 'Dashboard',
    path: '/',
    menuPath: ['Visao Geral', 'Dashboard'],
    icon: 'home',
    permissions: [],
  },
  {
    code: 'FIN-CTA-001',
    title: 'Contas',
    path: '/contas',
    menuPath: ['Cadastros', 'Financeiro', 'Contas'],
    icon: 'credit-card',
    permissions: [],
  },
  {
    code: 'FIN-TRX-001',
    title: 'Transacoes',
    path: '/transacoes',
    menuPath: ['Movimento', 'Transacoes'],
    icon: 'arrow-left-right',
    permissions: [],
  },
  {
    code: 'FIN-IMP-001',
    title: 'Importar CSV',
    path: '/importacao',
    menuPath: ['Movimento', 'Importar CSV'],
    icon: 'upload',
    permissions: [],
  },
  {
    code: 'CAD-CAT-001',
    title: 'Categorias',
    path: '/categorias',
    menuPath: ['Cadastros', 'Classificacao', 'Categorias'],
    icon: 'tag',
    permissions: [],
  },
  {
    code: 'CAD-TAG-001',
    title: 'Tags',
    path: '/tags',
    menuPath: ['Cadastros', 'Classificacao', 'Tags'],
    icon: 'tags',
    permissions: [],
  },
  {
    code: 'FIN-ORC-001',
    title: 'Orcamentos',
    path: '/orcamentos',
    menuPath: ['Planejamento', 'Orcamentos'],
    icon: 'wallet',
    permissions: [],
  },
  {
    code: 'FIN-MET-001',
    title: 'Metas',
    path: '/metas',
    menuPath: ['Planejamento', 'Metas'],
    icon: 'target',
    permissions: [],
  },
  {
    code: 'FIN-REC-001',
    title: 'Recorrentes',
    path: '/lancamentos-recorrentes',
    menuPath: ['Planejamento', 'Recorrentes'],
    icon: 'repeat',
    permissions: [],
  },
  {
    code: 'CAD-BEN-001',
    title: 'Beneficiarios',
    path: '/payees',
    menuPath: ['Cadastros', 'Financeiro', 'Beneficiarios'],
    icon: 'users',
    permissions: [],
  },
  {
    code: 'CAD-CCU-001',
    title: 'Centros de Custo',
    path: '/centros-custo',
    menuPath: ['Cadastros', 'Classificacao', 'Centros de Custo'],
    icon: 'layers',
    permissions: [],
  },
  {
    code: 'CAD-GRP-001',
    title: 'Grupos',
    path: '/grupos',
    menuPath: ['Cadastros', 'Classificacao', 'Grupos'],
    icon: 'users',
    permissions: [],
  },
  {
    code: 'CAD-LMT-001',
    title: 'Limites',
    path: '/limites',
    menuPath: ['Cadastros', 'Planejamento', 'Limites'],
    icon: 'gauge',
    permissions: [],
  },
  {
    code: 'FIN-FAT-001',
    title: 'Faturas',
    path: '/faturas',
    menuPath: ['Cadastros', 'Financeiro', 'Faturas'],
    icon: 'receipt',
    permissions: [],
  },
  {
    code: 'FIN-CTR-001',
    title: 'Carteiras',
    path: '/carteiras',
    menuPath: ['Cadastros', 'Financeiro', 'Carteiras'],
    icon: 'wallet',
    permissions: [],
  },
  {
    code: 'FIN-EMP-001',
    title: 'Emprestimos',
    path: '/emprestimos',
    menuPath: ['Cadastros', 'Financeiro', 'Emprestimos'],
    icon: 'hand-coins',
    permissions: [],
  },
  {
    code: 'REL-ANO-001',
    title: 'Anotacoes',
    path: '/anotacoes',
    menuPath: ['Analise', 'Anotacoes'],
    icon: 'sticky-note',
    permissions: [],
  },
  {
    code: 'REL-ANL-001',
    title: 'Relatorios',
    path: '/relatorios',
    menuPath: ['Analise', 'Relatorios'],
    icon: 'bar-chart-3',
    permissions: [],
  },
  {
    code: 'ADM-INC-001',
    title: 'Incidentes',
    path: '/incidentes',
    menuPath: ['Administracao', 'Incidentes'],
    icon: 'alert-triangle',
    permissions: [],
  },
  {
    code: 'MOD-LEM-001',
    title: 'Lembretes',
    path: '/lembretes',
    menuPath: ['Planejamento', 'Lembretes'],
    icon: 'bell',
    permissions: [],
  },
  {
    code: 'USR-PRF-001',
    title: 'Meu Perfil',
    path: '/perfil',
    menuPath: ['Conta', 'Meu Perfil'],
    icon: 'user-circle',
    permissions: [],
  },
]

/** Retorna todas as telas registradas. */
export function getAllScreens(): ScreenDefinition[] {
  return screens
}

/** Localiza uma tela pelo `code`. Retorna `undefined` se nao existir. */
export function findScreenByCode(code: string): ScreenDefinition | undefined {
  return screens.find((screen) => screen.code === code)
}

/**
 * Localiza a tela cuja `path` melhor casa com o caminho informado.
 *
 * A raiz (`/`) so casa de forma exata. As demais telas casam quando o caminho
 * informado e igual ou comeca com `path` seguido de `/` (rotas filhas como
 * `/contas/novo` resolvem para a tela `/contas`). Quando ha mais de um match,
 * vence o de `path` mais longo (mais especifico).
 */
export function findScreenByPath(path: string): ScreenDefinition | undefined {
  let best: ScreenDefinition | undefined
  for (const screen of screens) {
    if (screen.path === '/') {
      if (path === '/') {
        return screen
      }
      continue
    }
    const isMatch = path === screen.path || path.startsWith(`${screen.path}/`)
    if (isMatch && (!best || screen.path.length > best.path.length)) {
      best = screen
    }
  }
  return best
}
