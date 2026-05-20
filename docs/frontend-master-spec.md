# Frontend Master Spec — Financas Lab

> Documento mestre de padrões de UX/UI e arquitetura do frontend. Toda skill, agente ou contribuição manual ao frontend deve respeitar e referenciar este documento. Atualizações requerem ADR (Architecture Decision Record).

## 1. Contexto e estado atual

**Aplicacao:** Financas Lab -- gestao financeira pessoal.
**Stack (fixada):** Next.js 16 (App Router) + TypeScript + Tailwind CSS +
shadcn/ui (design system base-nova, usa `@base-ui/react`). Estado global:
Zustand. Server state: TanStack Query. Formularios: react-hook-form + Zod.

**Estado atual (2026-05-19, apos UI-1..UI-8):**

- **Shell declarativo ativo:** Screen Registry (`screens.registry.ts`) com 13
  telas registradas (codigos `MOD-ENT-NNN`). SidebarNav hierarquica gerada do
  registry, com grupos colapsiveis (estado em Zustand + localStorage). Tab Manager
  com abertura de telas como abas, persistencia em URL + localStorage, tab
  Dashboard permanente. Command Palette (Ctrl+K) com busca por codigo/titulo/path
  via cmdk.

- **Listagens padronizadas (UI-5/UI-6):** todas as 13 telas de listagem usam
  `DataTable` + `FilterBar` + `ActionsPanel`. Filtragem server-side (via
  `useListPage` com URL params) para Transacoes; client-side (via `useMemo`) para
  as demais (backends retornam `List<>` nao `Page<>`). `ActionsPanel` oferece
  Log (audit drawer) e Export CSV por entidade selecionada.

- **Formularios padronizados (UI-7/UI-8):** form-kit com `FormGrid` (grid de 12
  colunas) e `FormCol` (span 1-12). Campos de FK usam `LookupField` (combobox
  filtravel com `useQuery` interno). Campos monetarios usam `MoneyInput`.

- **Audit Log (UI-4):** bounded context `auditlog` registra create/update/delete
  para 9 entidades. `AuditLogDrawer` (Sheet lateral, timeline com diff) integrado
  via `ActionsPanel`.

- **Telas implementadas:** Dashboard, Contas, Transacoes, Categorias, Tags,
  Orcamentos, Metas, LancamentosRecorrentes, Payees, Anotacoes, Relatorios,
  Importacao, Incidentes.

- **Responsividade:** sidebar em modo icon/collapsed em viewports menores, icon
  mode com tooltip. Tab bar com scroll. Implementacao basica -- sem bottom sheet
  nem snapshot tests por breakpoint ainda.

## 2. Princípios

1. **Infra de UI separada de telas de domínio.** Shell, templates e componentes base são construídos uma vez por agentes especialistas; agentes de tela apenas consomem contratos declarativos.
2. **Declarativo > imperativo.** Cada tela registra seus metadados (código, ações, filtros, lookups) em um manifesto; o framework renderiza o resto.
3. **Consistência por construção.** Linters e testes no pipeline rejeitam PRs que violem padrões (ex.: campo sem `colSpan`, tela sem entrada no registry).
4. **Responsivo contínuo**, não apenas mobile vs desktop.
5. **Acessibilidade obrigatória** (teclado, ARIA, foco, reduced-motion).
6. **Log de tudo** desde o dia 1 da próxima fase.

## 3. Arquitetura em pacotes/skills

| Pacote | Responsabilidade | Pontos cobertos |
|---|---|---|
| `app-shell` | Layout, menu hierárquico, tab manager, command palette, ações laterais, responsividade | 1, 2, 3, 4, 6 |
| `list-page-template` | Template de tela de listagem com filtros avançados, ações default/custom, grid | 5, 6, 7 |
| `form-kit` | Wrapper de formulário com grid 12 obrigatório + catálogo de componentes de domínio (incluindo `LookupField`) | 7, 8 |
| `audit-log` | Subsistema de log/auditoria (back + front) | Pré-requisito da ação "Log" |

## 4. Padrões detalhados

### 4.1 Ponto 1 — Menu hierárquico com níveis

**O que é:** menu lateral organizado em grupos e subgrupos, com indicação clara de "o que está dentro de quê".

**Contrato:** todas as telas se registram em `screens.registry.ts`:
```ts
{
  code: "FIN-CTA-001",
  title: "Contas",
  path: "/contas",
  menuPath: ["Cadastros", "Financeiro", "Contas"],  // define a hierarquia
  icon: "wallet",
  permissions: ["contas.read"],
  component: lazy(() => import("./screens/contas/list"))
}
```
O componente `<SidebarMenu>` agrupa automaticamente por `menuPath`.

**Regras:**
- Máximo 3 níveis de profundidade.
- Grupos colapsáveis com estado lembrado por usuário.
- Item ativo destacado em todos os níveis (breadcrumb visual no menu).

### 4.2 Ponto 2 — Tabs internas da aplicação

**O que é:** abrir uma tela cria uma aba dentro do app (não do navegador). Múltiplas telas convivem; trocar de aba preserva estado; reload restaura.

**Contrato:** `useTabsStore` (Zustand) com persistência híbrida:
- **URL:** `?tabs=FIN-CTA-001,FIN-TRX-001&active=FIN-TRX-001` (compartilhável, recarregável).
- **localStorage:** estado interno de cada tab (filtros aplicados, scroll, seleção, rascunho de formulário **não-sensível**).

**Regras:**
- Limite máximo: **10 abas** (configurável); ao atingir, fecha a mais antiga não-fixada e avisa.
- Tab pode ser "fixada" (não fecha automaticamente).
- Drag-and-drop para reordenar (desktop) / long-press (touch).
- Fechar a última aba volta para o Dashboard (não fecha o app).
- Tab pode ser duplicada (Ctrl+clique do meio ou menu de contexto).
- **Nunca persistir em localStorage:** senhas, tokens, payloads completos de formulário com dados sensíveis. Apenas IDs e filtros.

### 4.3 Ponto 3 — Código de tela + busca

**O que é:** toda tela tem um código curto. Usuário pesquisa tela por código **ou** descrição, sem precisar decorar localização no menu.

**Formato do código:** `MOD-ENT-NNN`
- `MOD` = módulo (3 letras: FIN, CAD, REL, ADM…)
- `ENT` = entidade (3 letras: CTA=Conta, TRX=Transação, CAT=Categoria…)
- `NNN` = sequencial (001=listagem, 002=cadastro, 003=visualização…)

Ex.: `FIN-CTA-001` = listagem de Contas; `FIN-CTA-002` = cadastro de Contas.

**Componente:** `<CommandPalette>` acionado por **Ctrl+K / Cmd+K** em qualquer breakpoint. Busca em `code`, `title` e `menuPath`. Mostra atalhos recentes e favoritos.

**Skill obrigatória:** `register-screen` — todo agente que cria uma tela executa essa skill, que valida unicidade do `code` e injeta no registry.

### 4.4 Ponto 4 — Responsividade contínua

**Princípio:** o layout se adapta continuamente ao tamanho do viewport (não só mobile/desktop). Suporta uso em ultrawide split, tablets em pé/deitado, janelas lado a lado.

**Breakpoints:**

| Faixa | Menu | Tab bar | Ações | Conteúdo |
|---|---|---|---|---|
| ≥ 1280px | Expandido (texto + ícones) | Visível | Painel lateral visível | Grid 12 pleno |
| 1024–1279px | Rail (só ícones, tooltip no hover) ou expandido conforme preferência | Visível com scroll se necessário | Painel colapsável | Grid 12 |
| 768–1023px | **Colapsado em ícone** | Visível | Drawer/FAB pelo lado direito | Grid 12 reflow |
| < 768px | **Colapsado obrigatório** | Indicador "N abas" + dropdown | Bottom sheet | 1 coluna ou conforme `colSpan` |

**Menu colapsado — comportamento-chave:**
Clicar no ícone abre um overlay/drawer **único** que oferece **simultaneamente**:
1. Árvore de navegação hierárquica (grupos colapsáveis, igual ao desktop).
2. Campo de busca no topo (busca por código e descrição — ponto 3).
3. Resultados filtrados em tempo real abaixo do campo, conforme o usuário digita.

**Importante:** o command palette do ponto 3 e o overlay do menu colapsado são **o mesmo componente** com renderizações ligeiramente diferentes — única porta de entrada para navegação.

**Outras regras:**
- Toggle manual de colapso disponível em qualquer breakpoint (pin/unpin), persistido por usuário.
- Hover ~300ms no modo rail expande temporariamente em overlay.
- Tab bar overflow: scroll horizontal com setas ou agrupamento "+ N".
- Touch targets ≥ 44×44px em viewports < 768px.
- Gestos: swipe da borda esquerda abre menu.
- Header mobile: logo + ícone menu + indicador de tabs + avatar; título da tela na faixa abaixo.
- Modais em mobile ocupam tela cheia; drawers entram como bottom sheet.
- `@media print` esconde shell.

**Acessibilidade:**
- Navegação por teclado completa (Tab, Enter, Esc, setas).
- `aria-expanded`, `aria-controls`, `role="navigation"`.
- Focus trap em overlays.
- Anúncios de troca de tab/menu para screen readers.
- Respeitar `prefers-reduced-motion`.

**Testes obrigatórios no pipeline:**
- Snapshot visual em 375px, 768px, 1440px.
- E2E: redimensionar 1440→375 e verificar colapso do menu.
- E2E: Ctrl+K abre palette em qualquer breakpoint.

### 4.5 Ponto 5 — Tela de listagem padrão com filtros avançados

**O que é:** toda tela CRUD começa por uma listagem dos registros que o usuário tem permissão, com filtros avançados empilháveis.

**Contrato do `<ListPage<T>>`:**
```ts
{
  screenCode: "FIN-CTA-001",
  endpoint: "/api/contas",
  columns: [
    { key, label, sortable, formatter, colSpan? }
  ],
  filterableFields: [
    {
      name: "tipo",
      label: "Tipo",
      type: "enum",       // string | number | date | boolean | enum | ref
      operators: ["eq", "ne", "in"],
      options?: [...],    // para enum
      lookupSource?: "..." // para ref
    }
  ],
  defaultActions: ["log", "export-excel", "print"],
  customActions: [{ id, label, icon, handler, permission }],
  rowActions: [...]
}
```

**Construtor de filtros:**
- UI: chips empilhados com `campo + operador + valor`, agrupáveis com AND/OR.
- Operadores por tipo:
  - `string`: eq, ne, like, in, notnull, null
  - `number`/`date`: eq, ne, gt, lt, ge, le, between, in, null, notnull
  - `boolean`: eq
  - `enum`: eq, ne, in
  - `ref`: eq, ne, in (usa `LookupField` para o valor)

**Contrato de requisição (alinhado com backend):**
```json
{
  "filters": [
    { "field": "tipo", "op": "in", "value": ["CORRENTE","POUPANCA"] },
    { "field": "saldo", "op": "gt", "value": 0 }
  ],
  "logic": "AND",
  "sort": [{ "field": "nome", "dir": "asc" }],
  "page": 1,
  "pageSize": 50
}
```

**Funcionalidades garantidas pelo template:** paginação, ordenação, seleção múltipla, persistência de filtros aplicados em URL+localStorage (sobrevive ao reload e ao trocar de tab), exportação respeitando filtros.

### 4.6 Ponto 6 — Menu lateral de Ações

**Ações padrão (sempre presentes):**
- **Log do registro** — abre drawer com timeline de auditoria do item. Requer subsistema de Audit Log (§5).
- **Exportar lista para Excel** — serializa dados filtrados (client-side com SheetJS p/ listas pequenas; `?export=xlsx` server-side p/ grandes).
- **Imprimir** — rota `/print/{screenCode}?...filtros` com CSS `@media print`.

**Ações customizadas:** declaradas no contrato da `ListPage`, com `permission` opcional.

**Renderização responsiva:** painel lateral em desktop, drawer em tablet, bottom sheet em mobile.

### 4.7 Ponto 7 -- Grid de 12 colunas (implementado em UI-7/UI-8)

**Componentes:** `FormGrid` e `FormCol` em `src/shared/components/`.

`FormGrid` e um wrapper `div` com `grid grid-cols-12 gap-4`. `FormCol` aceita
`span` (1-12, default 12) e aplica `col-span-N`.

**Uso:**
```tsx
<FormGrid>
  <FormCol span={7}>
    <FormField name="valor" ... />
  </FormCol>
  <FormCol span={5}>
    <FormField name="data" ... />
  </FormCol>
  <FormCol span={12}>
    <FormField name="descricao" ... />
  </FormCol>
</FormGrid>
```

**Quando usar:** campos semanticamente relacionados ou de tamanho semelhante que
ganham com layout lado a lado. Nao forcar grid em formularios simples de 1-2 campos.

**Nota:** `colSpan` por breakpoint (`xs/md/lg`) nao foi implementado -- todas as
colunas sao fixas independente do viewport. Melhoria futura.

**Convencao Tailwind:** as strings de classe `col-span-N` no `SPAN_CLASS` record
do `FormCol.tsx` sao literais completas para garantir que o JIT as inclua no build.

### 4.8 Ponto 8 -- LookupField (implementado em UI-7/UI-8)

**Componente:** `LookupField` em `src/shared/components/LookupField.tsx`.

Combobox filtravel para campos de FK. Carrega opcoes via `useQuery` e filtra
client-side pelo texto digitado. Substitui o padrao de `Select` estatico com
busca inline, sem dependencia de cmdk.

**Props:**
```tsx
interface LookupFieldProps {
  value: string | null
  onChange: (value: string | null) => void
  queryKey: string[]       // DEVE ter sufixo distinto da queryKey da listagem
  queryFn: () => Promise<LookupOption[]>
  placeholder?: string
  emptyMessage?: string
  disabled?: boolean
}
```

**Convencao critica de queryKey (B13):**
O TanStack Query usa `queryKey` como chave de cache. Se a pagina de listagem
ja populou `['categorias']` com `Categoria[]` raw, o LookupField receberia esses
objetos sem o campo `label` e crasharia em `o.label.toLowerCase()`.

**Regra:** todo uso de LookupField deve usar `queryKey` com sufixo discriminador:
- `['categorias', 'lookup']` -- nao `['categorias']`
- `['payees', 'lookup']` -- nao `['payees']`
- `['contas', 'ativas']` ja e distinto (tem segundo elemento) -- ok

**LookupOption:**
```ts
interface LookupOption { value: string; label: string }
```

O `queryFn` deve mapear os objetos da API para `LookupOption`:
```tsx
queryFn={() => categoriasService.listar()
  .then(cs => cs.map(c => ({ value: c.id, label: c.nome })))}
```

**Nota:** a visao original (§4.8) descrevia dois campos codigo+descricao com modal
F3. Nao foi implementado assim -- a implementacao atual e um combobox simples.
A visao completa com modal avancado permanece como melhoria futura.

## 5. Subsistema de Audit Log (pré-requisito)

> A aplicação **não possui log hoje**. Sem isso, a ação "Log do registro" do ponto 6 não funciona. **Implementar antes ou junto** da `list-page-template`.

**Eventos a capturar:** create, update (com diff campo a campo), delete, read sensível (opcional), login, logout, falhas de autorização, ações customizadas das telas.

**Schema do evento:**
```ts
{
  id, timestamp,           // UTC + timezone
  userId, userEmail,        // snapshot
  entityType, entityId,
  action,                   // create|update|delete|read|login|logout|custom
  screenCode,               // alinha com screens.registry
  ipAddress, userAgent,
  requestId,                // correlation
  before, after, diff,      // JSON; redact de campos sensíveis
  metadata                  // livre
}
```

**Captura:** middleware no backend como padrão; eventos explícitos para ações customizadas.

**Persistência:**
- Tabela `audit_log` com índices em `(entityType, entityId)`, `userId`, `timestamp`, `screenCode`.
- Imutável via API (sem UPDATE/DELETE expostos).
- Política de retenção: **definir** (sugestão inicial: 1 ano hot + arquivamento).
- Escrita assíncrona (fila/worker) para não impactar latência.

**API:**
- `GET /api/audit-log?entityType=&entityId=&...filtros` — para a action "Log".
- `GET /api/audit-log` — tela administrativa global, com permissão específica.
- Paginação obrigatória.

**UI:** drawer/modal com timeline cronológica, expansão para diff, reaproveitando filtros do `list-page-template`.

**Segurança e LGPD:**
- Mascarar campos sensíveis no diff (lista configurável por entidade).
- Pedido de exclusão de dados pessoais → anonimizar `userId`, manter trilha técnica.
- Se escrita do log falhar, **não** reverter a operação de negócio — fallback em DLQ + alerta.

**Skill nova:** `add-entity-to-audit` — usada pelo agente de backend ao criar nova entidade/CRUD; registra a entidade no middleware automaticamente.

## 6. Ordem de implementação recomendada

1. **Screen Registry + Command Palette + Menu hierárquico** (pontos 1, 3).
2. **Tab Manager** (ponto 2).
3. **Responsividade do shell** (ponto 4) — incluindo unificação palette ↔ menu colapsado.
4. **Audit Log** (§5) — backend + frontend (drawer da action "Log").
5. **`list-page-template`** com filtros avançados + Actions panel (pontos 5, 6).
6. **Refatorar telas existentes** (Contas, Transações, Categorias, Tags, Orçamentos, Metas, Recorrentes, Beneficiários, Anotações) para o novo template — uma issue por tela para o agente de tela.
7. **`form-kit`** com grid 12 obrigatório + `<LookupField>` (pontos 7, 8).
8. **Refatorar formulários existentes** para o `form-kit`.

## 7. Skills/agentes impactados

| Skill/agente | Mudança |
|---|---|
| `register-screen` (nova) | Registra tela no manifesto com code, menuPath, permissions. |
| `add-entity-to-audit` (nova) | Inclui entidade no middleware de audit log. |
| Agente de tela (existente) | Passa a consumir `<ListPage>` e `<FormGrid>` em vez de markup livre; sempre declara `colSpan`. |
| Agente de backend (existente) | Padroniza contrato de filtros JSON (§4.5); registra entidades no audit. |
| Agente de planejamento (existente) | Considera este documento como referência mestre. |
| CI/CD pipeline | Adiciona lints (colSpan, registry), snapshots visuais, E2E de responsividade. |

## 8. Decisões ~~pendentes~~ tomadas (2026-05-18)

> Todas as decisões respondidas. Ver ADR-014 em `docs/adrs.md` para registro formal.

1. **Stack:** Next.js 16 (App Router) ✅ — já em uso; Tailwind + shadcn (base-nova) ✅
2. **State manager:** Zustand para estado de UI (tabs, preferências); TanStack Query permanece para server state ✅
3. **Command Palette:** `cmdk` — já incluso no shadcn, zero dependência nova ✅
4. **Persistência de tabs:** URL + localStorage híbrido ✅
5. **Limite de tabs:** 10 ✅
6. **Preferência de menu colapsado:** localStorage por dispositivo ✅
7. **Tab bar em mobile:** indicador "N abas" + dropdown ✅
8. **Filtros avançados — UI:** chips empilháveis estilo Notion/Linear ✅
9. **RBAC:** fora do escopo desta fase; `permissions: []` no registry sem enforcement ✅
10. **Audit Log — retenção:** 90 dias hot, sem arquivamento por enquanto ✅
11. **Audit Log — escopo:** apenas mutações (create/update/delete); reads não logados ✅
12. **Código de tela:** formato `MOD-ENT-NNN` aprovado ✅

## 9. Glossário

- **Shell:** o "frame" da aplicação (header, menu, tabs, ações) que envolve qualquer tela.
- **Screen Registry:** manifesto declarativo com todas as telas da aplicação.
- **Tab Manager:** estado global que controla as abas internas do app.
- **Command Palette:** overlay de busca rápida (Ctrl+K) que também serve de menu mobile.
- **Lookup:** componente código+descrição+lupa para selecionar registros relacionados.
- **Audit Log:** trilha imutável de eventos para auditoria e ação "Log do registro".

---

**Versão:** 0.2 (decisões da §8 respondidas; ADR-014 criado)
**Última atualização:** 2026-05-18
**Próxima ação:** gerar sub-etapas via `/plan` para implementar as fases UI-1 a UI-6.