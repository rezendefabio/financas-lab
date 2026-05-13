# Prompt -- Sub-etapa 5.15: Reorganizacao feature-first do frontend

## Contexto

Refactor de estrutura de pastas do frontend para o padrao feature-first,
espelhando os bounded contexts do backend. Motivacao: com muitos dominios,
a estrutura layer-first atual (todos os services juntos, todos os types juntos)
torna dificil localizar e entender o escopo de um dominio. Feature-first co-localiza
tudo de um dominio em `src/features/<dominio>/`.

Nenhuma logica de negocio muda. Apenas movimentacao de arquivos + atualizacao
de imports + atualizacao dos agentes para conhecer os novos paths.

Camada 4. Refactor puro + atualizacao de agentes + ADR-013.

---

## Nova estrutura de pastas

```
frontend/src/
├── app/                          <- Next.js routing (paginas ficam aqui)
├── features/                     <- dominios (espelha bounded contexts)
│   ├── auth/
│   │   ├── hooks/
│   │   │   └── use-auth.ts
│   │   ├── services/
│   │   │   ├── auth.service.ts
│   │   │   └── auth.service.test.ts
│   │   └── index.ts              <- barrel exports
│   ├── contas/
│   │   ├── services/
│   │   │   └── contas.service.ts
│   │   ├── types/
│   │   │   └── conta.ts
│   │   └── index.ts
│   ├── categorias/
│   │   ├── services/
│   │   │   └── categorias.service.ts
│   │   ├── types/
│   │   │   └── categoria.ts
│   │   └── index.ts
│   └── transacoes/
│       ├── services/
│       │   └── transacoes.service.ts
│       ├── types/
│       │   └── transacao.ts
│       └── index.ts
├── shared/                       <- genuinamente compartilhado entre dominios
│   ├── components/
│   │   └── ui/                   <- shadcn components (de src/components/ui/)
│   ├── hooks/
│   │   └── use-mobile.ts
│   ├── lib/
│   │   ├── auth.ts               <- JWT utils (de src/lib/auth.ts)
│   │   ├── auth.test.ts
│   │   └── utils.ts              <- shadcn cn utility
│   └── types/
│       └── api.ts                <- ApiError e tipos compartilhados
├── services/                     <- infraestrutura HTTP (so api-client aqui)
│   ├── api-client.ts
│   └── api-client.test.ts
├── providers/                    <- React providers (sem mudanca)
├── stories/                      <- Storybook (sem mudanca de pasta)
└── test/                         <- setup.ts (sem mudanca)
```

---

## Arquivos a mover (mapeamento completo)

| De | Para |
|----|------|
| `src/hooks/use-auth.ts` | `src/features/auth/hooks/use-auth.ts` |
| `src/services/auth.service.ts` | `src/features/auth/services/auth.service.ts` |
| `src/services/auth.service.test.ts` | `src/features/auth/services/auth.service.test.ts` |
| `src/services/contas.service.ts` | `src/features/contas/services/contas.service.ts` |
| `src/types/conta.ts` | `src/features/contas/types/conta.ts` |
| `src/services/categorias.service.ts` | `src/features/categorias/services/categorias.service.ts` |
| `src/types/categoria.ts` | `src/features/categorias/types/categoria.ts` |
| `src/services/transacoes.service.ts` | `src/features/transacoes/services/transacoes.service.ts` |
| `src/types/transacao.ts` | `src/features/transacoes/types/transacao.ts` |
| `src/hooks/use-mobile.ts` | `src/shared/hooks/use-mobile.ts` |
| `src/lib/auth.ts` | `src/shared/lib/auth.ts` |
| `src/lib/auth.test.ts` | `src/shared/lib/auth.test.ts` |
| `src/lib/utils.ts` | `src/shared/lib/utils.ts` |
| `src/types/api.ts` | `src/shared/types/api.ts` |
| `src/components/ui/` (todos) | `src/shared/components/ui/` |

**Ficam no lugar:**
- `src/services/api-client.ts` e `api-client.test.ts` (infraestrutura HTTP)
- `src/providers/` (sem mudanca)
- `src/app/` (Next.js routing -- paginas ficam aqui)
- `src/test/setup.ts`
- `src/stories/` (Storybook)

---

## Barrel exports a criar

Para cada feature, criar `index.ts` que reexporta os simbolos publicos:

**`src/features/auth/index.ts`:**
```typescript
export { authService } from './services/auth.service'
export { useAuth } from './hooks/use-auth'
```

**`src/features/contas/index.ts`:**
```typescript
export type { Conta, TipoConta, SaldoResponse, SaldoTotalResponse } from './types/conta'
export { contasService } from './services/contas.service'
```

**`src/features/categorias/index.ts`:**
```typescript
export type { Categoria, TipoCategoria } from './types/categoria'
export { categoriasService } from './services/categorias.service'
```

**`src/features/transacoes/index.ts`:**
```typescript
export type { Transacao } from './types/transacao'
export { transacoesService } from './services/transacoes.service'
```

Leia cada arquivo de types e service antes de criar o barrel para usar os
nomes de export corretos.

---

## Imports a atualizar (todos os arquivos afetados)

Apos mover os arquivos, atualizar os imports em:

**`src/services/api-client.ts`:**
- `@/lib/auth` → `@/shared/lib/auth`

**`src/services/api-client.test.ts`:**
- `@/lib/auth` → `@/shared/lib/auth`
- `@/types/api` → `@/shared/types/api`

**`src/providers/auth-provider.tsx`:**
- `@/hooks/use-auth` → `@/features/auth/hooks/use-auth` (ou `@/features/auth`)
- `@/lib/auth` → `@/shared/lib/auth`

**`src/app/(auth)/login/page.tsx` e `page.test.tsx`:**
- `@/services/auth.service` → `@/features/auth/services/auth.service`
- `@/hooks/use-auth` → `@/features/auth/hooks/use-auth`
- `@/types/api` → `@/shared/types/api`

**`src/app/(dashboard)/layout.tsx`:**
- `@/hooks/use-mobile` → `@/shared/hooks/use-mobile`
- `@/hooks/use-auth` → `@/features/auth/hooks/use-auth`
- `@/components/ui/*` → `@/shared/components/ui/*`

**`src/app/(dashboard)/page.tsx`:**
- `@/services/contas.service` → `@/features/contas/services/contas.service`
- `@/types/conta` → `@/features/contas/types/conta`

**`src/app/(dashboard)/contas/page.tsx` e `page.test.tsx`:**
- `@/services/contas.service` → `@/features/contas/services/contas.service`
- `@/types/conta` → `@/features/contas/types/conta`

**`src/app/(dashboard)/contas/novo/page.tsx` e `page.test.tsx`:**
- mesmos updates de contas acima

**`src/app/(dashboard)/contas/[id]/page.tsx` e `page.test.tsx`:**
- mesmos updates de contas acima

**`src/shared/lib/auth.ts`:**
- sem imports externos para atualizar (usa apenas localStorage)

**`src/features/auth/services/auth.service.ts`:**
- `../api-client` → `@/services/api-client`
- `@/lib/auth` → `@/shared/lib/auth`

**`src/features/contas/services/contas.service.ts`:**
- `../api-client` → `@/services/api-client`
- `@/types/conta` → `./types/conta` ou `../types/conta`

**`src/stories/*.tsx` e `*.stories.ts`:**
- `@/components/ui/*` → `@/shared/components/ui/*`

**Todos os arquivos `components/ui/` movidos para `shared/components/ui/`:**
- `@/lib/utils` → `@/shared/lib/utils`

Leia cada arquivo antes de editar. Nao adivinhar imports -- verificar o conteudo
real do arquivo para saber o que precisa mudar.

---

## Atualizar `.claude/agents/test-writer.md` -- modo frontend

Leia o arquivo completo antes de editar.

Substituir a tabela de categorias por path na secao "## Modo frontend":

```markdown
### Identificar categoria pelo path

| Categoria | Path pattern | Arquivo de teste gerado |
|-----------|-------------|-------------------------|
| Componente | `src/app/**/*.tsx`, `src/shared/components/**/*.tsx`, `src/features/*/components/**/*.tsx` | mesmo diretorio, `<Nome>.test.tsx` |
| Hook | `src/features/*/hooks/**/*.ts`, `src/shared/hooks/**/*.ts` | mesmo diretorio, `<nome>.test.ts` |
| Service | `src/features/*/services/**/*.ts`, `src/services/**/*.ts` | mesmo diretorio, `<nome>.test.ts` |
| Utility | `src/shared/lib/**/*.ts` | mesmo diretorio, `<nome>.test.ts` |
```

Atualizar tambem a instrucao de leitura de referencia:
- `src/app/(auth)/login/page.tsx` continua valida como referencia de componente
- `src/features/auth/services/auth.service.test.ts` como referencia de service

---

## Atualizar `.claude/agents/front-reviewer.md` -- regras B1 e B4

Leia o arquivo completo antes de editar.

**B1 (atualizar):**

| ID | Regra | Como detectar |
|----|-------|---------------|
| B1 | `fetch(` fora de `src/services/api-client.ts` | grep por `fetch(` em arquivos frontend exceto `src/services/api-client.ts` e `*.test.*` |

Motivacao: na nova estrutura, domain services ficam em `features/*/services/`
e NAO devem chamar `fetch` diretamente -- apenas `apiFetch` de `@/services/api-client`.

**B4 (atualizar paths):**

| ID | Regra | Como detectar |
|----|-------|---------------|
| B4 | `any` como tipo de resposta de API | grep por `: any` ou `as any` em `src/features/*/services/`, `src/features/*/types/`, `src/shared/types/` |

---

## Atualizar `CLAUDE.md` -- secao `## Frontend`

Leia o arquivo completo antes de editar. Substituir a secao `## Frontend` por:

```markdown
## Frontend

- Framework: Next.js 16 (App Router) em `frontend/`.
- Testes: Vitest + Testing Library (`npm test` em `frontend/`).
- Design system: shadcn/ui com estilo `base-nova`. Adicionar componentes via `npx shadcn@latest add <nome>`.
- Camada de API: `src/services/api-client.ts` (unico ponto de `fetch`). Domain services ficam em
  `src/features/<dominio>/services/`. NAO usar `fetch` diretamente fora de `api-client.ts`.
- Auth: JWT em localStorage via `src/shared/lib/auth.ts`. Provider em `src/providers/auth-provider.tsx`.
- Dev: `.\scripts\dev-front.ps1` (inicia Next.js dev server).
- `base-nova` usa `@base-ui/react` -- nao tem `@radix-ui`. Usar `render` prop em vez de `asChild`.
- Organizacao: feature-first (ADR-013). Cada dominio em `src/features/<dominio>/` com
  `services/`, `types/`, `hooks/`, `components/` e `index.ts`. Codigo compartilhado em `src/shared/`.
- Testes: ao criar componente, hook ou service, invocar `/write-test <path>` para gerar
  teste Vitest + Testing Library colocado.
```

---

## Criar ADR-013 em `docs/adrs.md`

Leia o arquivo antes de editar. Adicionar ao final:

```markdown
## ADR-013 -- Organizacao frontend feature-first (2026-05-13)

**Contexto:** Com multiplos dominios no frontend, a estrutura layer-first
(todos services juntos, todos types juntos) dificulta localizar e entender
o escopo de um dominio. A mesma limitacao que levou o backend a bounded contexts.

**Decisao:** Adotar estrutura feature-first espelhando os bounded contexts do
backend. Cada dominio vive em `src/features/<dominio>/` com suas proprias pastas
`services/`, `types/`, `hooks/`, `components/` e um `index.ts` de barrel exports.
Codigo genuinamente compartilhado (shadcn components, auth utils, api types) fica
em `src/shared/`. O unico ponto de `fetch` e `src/services/api-client.ts`.

**Consequencias:**
- Facil localizar tudo de um dominio sem buscar em multiplas pastas raiz.
- Espelha mentalmente o backend: `features/contas/` = bounded context `conta`.
- Adicionar novo dominio = criar pasta `features/<dominio>/` com estrutura padrao.
- Remover dominio = deletar `features/<dominio>/` sem impacto em outros.
- Imports mais longos (`@/features/contas/services/contas.service`) mitigados por
  barrel exports (`@/features/contas`).
```

---

## Validacao

**Antes de commitar:**
```powershell
.\scripts\check-front.ps1
```

Deve passar lint + todos os testes (46 existentes) + build.
Se falhar apos mover arquivos: verificar imports quebrados no output do lint/build.

**Pre-condicao (ADR-011):**
- `Test-Path` para confirmar que cada arquivo foi movido antes de deletar o original
- `git status` antes do commit para confirmar que nenhum arquivo foi perdido

---

## Fluxo de execucao

```
1. git checkout -b refactor/etapa-5-15-feature-first

2. Ler todos os arquivos afetados antes de comecar (ver lista acima)

3. Criar estrutura de pastas features/ e shared/

4. Mover arquivos (seguir tabela de mapeamento)

5. Criar barrel exports (index.ts para cada feature)

6. Atualizar imports em todos os arquivos afetados

7. .\scripts\check-front.ps1 -- corrigir qualquer import quebrado antes de continuar

8. commit: refactor(frontend): reorganiza para estrutura feature-first

9. Editar .claude/agents/test-writer.md (path patterns)

10. Editar .claude/agents/front-reviewer.md (regras B1, B4)

11. Editar CLAUDE.md (secao ## Frontend)

12. Adicionar ADR-013 em docs/adrs.md

13. commit: feat(claude): atualiza agentes e CLAUDE.md para estrutura feature-first

14. Atualizar docs/progresso.md (registra 5.15)

15. commit: docs(progresso): registra sub-etapa 5.15
    (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-15.md)

16. /ship -> PR com reviews; corrigir apontamentos autonomamente
```

---

## Estrutura de commits (5.15)

```
refactor(frontend): reorganiza para estrutura feature-first
feat(claude): atualiza agentes e CLAUDE.md para estrutura feature-first
docs(progresso): registra sub-etapa 5.15
```

---

## Restricoes

- NAO alterar logica de negocio -- apenas mover arquivos e atualizar imports.
- NAO mover `src/services/api-client.ts` -- e infraestrutura HTTP, fica na raiz de services/.
- NAO mover `src/app/` -- Next.js exige pages em app/.
- NAO mover `src/providers/` -- providers sao transversais, nao especificos de dominio.
- NAO mover `src/stories/` -- Storybook tem configuracao propria.
- Se `check-front.ps1` falhar: investigar o output, corrigir imports, rodar novamente
  antes de commitar. Nao commitar com testes ou build quebrados.
- Leia cada arquivo antes de mover para garantir que os imports internos estao corretos.
- `git status` antes do commit: verificar que nenhum arquivo foi esquecido ou duplicado.

---

## Estado esperado ao terminar

- PR aberto com 3 commits acima de main.
- Estrutura `src/features/` com 4 dominios (auth, contas, categorias, transacoes).
- Estrutura `src/shared/` com components/ui, hooks, lib, types.
- `check-front.ps1` verde (46 testes passando + build).
- Nenhum import apontando para paths antigos (src/services/<dominio>, src/types/, src/hooks/, src/lib/, src/components/ui/).
- test-writer e front-reviewer com paths atualizados.
- CLAUDE.md e ADR-013 documentando a nova estrutura.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO rodar /ship mais de uma vez.
- NAO deletar arquivos antes de confirmar que o novo path esta correto com Test-Path.
