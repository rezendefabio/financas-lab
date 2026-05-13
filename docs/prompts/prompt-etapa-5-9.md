# Prompt -- Sub-etapa 5.9: Frontend foundation

## Contexto

O scaffold do frontend ja existe em `frontend/` (criado por executor anterior):

- Next.js 16.2.6 + React 19.2.4 + TypeScript
- Tailwind CSS 4 + shadcn/ui (estilo `base-nova`, tokens oklch configurados)
- React Hook Form + Zod 4 + TanStack React Query instalados
- ESLint flat config v9
- `components/ui/` VAZIO -- nenhum componente shadcn instalado
- Sem Vitest, sem Storybook, sem camada de API, sem auth, sem paginas reais

Esta sub-etapa constroi a fundacao de desenvolvimento sobre esse scaffold:
testes, design system, estrutura de pastas, camada de servicos com JWT,
layout do dashboard e paginas de login + home. A sub-etapa NAO implementa
features de negocio -- apenas a infra que as suporta.

**LEIA `frontend/AGENTS.md` (e `frontend/CLAUDE.md`) ANTES de qualquer codigo.**
Next.js 16 tem breaking changes em relacao ao que o modelo conhece de treinamento.
Leia os guias relevantes em `frontend/node_modules/next/dist/docs/` conforme indicado.

Sub-etapa 5.9 (Camada 4).

---

## Decisoes de stack (nao alterar)

- **Framework:** Next.js 16 com App Router (manter o scaffold existente)
- **Testes:** Vitest + Testing Library (nao Jest) -- compativel com Tailwind 4 e ESM
- **Design system:** shadcn/ui com `npx shadcn@latest add <componente>` (nao instalar manualmente)
- **Estilos:** Tailwind CSS 4 + tokens oklch ja configurados em `globals.css` (nao alterar)
- **HTTP:** `fetch` nativo com wrapper proprio (nao axios, nao swr)
- **Estado de servidor:** TanStack React Query (ja instalado)
- **Forms:** React Hook Form + Zod 4 (ja instalados)
- **Storybook:** `@storybook/nextjs` -- se incompativel com Next.js 16, usar `@storybook/experimental-nextjs-vite`

---

## Estrutura de pastas a criar

Dentro de `frontend/src/`:

```
app/
  (auth)/
    login/
      page.tsx
  (dashboard)/
    layout.tsx        -- layout protegido com sidebar
    page.tsx          -- home: card de saldo total
components/
  ui/                 -- preenchido por shadcn add
  features/           -- componentes compostos por dominio (vazio por ora)
hooks/                -- custom React hooks
  use-auth.ts
services/             -- camada de acesso a API
  api-client.ts
  auth.service.ts
  contas.service.ts
  categorias.service.ts
  transacoes.service.ts
lib/
  utils.ts            -- ja existe (nao alterar)
  auth.ts             -- JWT token utils (localStorage)
providers/
  auth-provider.tsx
  query-provider.tsx
types/
  api.ts
  conta.ts
  categoria.ts
  transacao.ts
test/
  setup.ts            -- importa @testing-library/jest-dom
```

---

## Parte 1 -- Vitest + Storybook + estrutura

### Pacotes a instalar (devDependencies)

```
npm install -D vitest @vitejs/plugin-react vite-tsconfig-paths jsdom
npm install -D @testing-library/react @testing-library/user-event @testing-library/jest-dom
```

Storybook (rodar com `--yes` para nao travar em input):
```
npx storybook@latest init --yes
```

Se o init falhar com Next.js 16: instalar manualmente `@storybook/experimental-nextjs-vite` e criar `.storybook/main.ts` com framework `@storybook/experimental-nextjs-vite`. Registrar a decisao no corpo do commit.

### `vitest.config.ts` (na raiz de `frontend/`)

```ts
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tsconfigPaths from 'vite-tsconfig-paths'

export default defineConfig({
  plugins: [react(), tsconfigPaths()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    exclude: ['node_modules', '.next', '.storybook'],
  },
})
```

### `src/test/setup.ts`

```ts
import '@testing-library/jest-dom'
```

### Adicionar scripts em `package.json`

```json
"test": "vitest",
"test:run": "vitest run",
"storybook": "storybook dev -p 6006",
"build-storybook": "storybook build"
```

### ESLint -- regra arquitetural

Adicionar ao `eslint.config.mjs` uma regra que impede `fetch(` fora de `services/`:

```js
{
  files: ['src/**/*.{ts,tsx}'],
  ignores: ['src/services/**'],
  rules: {
    'no-restricted-globals': [
      'error',
      { name: 'fetch', message: 'Use api-client in src/services/ instead of raw fetch()' },
    ],
  },
},
```

---

## Parte 2 -- Camada de API com JWT

### `src/lib/auth.ts`

```ts
const TOKEN_KEY = 'financas_token'

export const getToken = (): string | null =>
  typeof window !== 'undefined' ? localStorage.getItem(TOKEN_KEY) : null

export const setToken = (token: string): void =>
  localStorage.setItem(TOKEN_KEY, token)

export const clearToken = (): void =>
  localStorage.removeItem(TOKEN_KEY)

export const isAuthenticated = (): boolean =>
  !!getToken()
```

### `src/types/api.ts`

```ts
export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message)
    this.name = 'ApiError'
  }
}
```

### `src/services/api-client.ts`

```ts
import { getToken } from '@/lib/auth'
import { ApiError } from '@/types/api'

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken()
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
  })
  if (res.status === 204) return undefined as T
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new ApiError(res.status, body.message ?? res.statusText)
  }
  return res.json() as Promise<T>
}
```

### `src/types/conta.ts`

```ts
export interface ValorMonetario {
  valor: number
  moeda: string
}

export interface Conta {
  id: string
  nome: string
  tipo: 'CORRENTE' | 'POUPANCA' | 'INVESTIMENTO' | 'CARTEIRA'
  saldoInicial: ValorMonetario
  ativo: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface SaldoResponse {
  saldoAtual: ValorMonetario
  saldoInicial: ValorMonetario
  totalReceitas: ValorMonetario
  totalDespesas: ValorMonetario
}

export interface SaldoTotalResponse {
  valor: number
  moeda: string
  totalContas: number
}
```

### `src/types/categoria.ts`

```ts
export interface Categoria {
  id: string
  nome: string
  tipo: 'RECEITA' | 'DESPESA'
  categoriaPaiId: string | null
  criadoEm: string
}
```

### `src/types/transacao.ts`

```ts
import { ValorMonetario } from './conta'

export interface Transacao {
  id: string
  tipo: 'RECEITA' | 'DESPESA' | 'TRANSFERENCIA'
  valor: ValorMonetario
  data: string
  descricao: string
  contaId: string
  contaDestinoId: string | null
  categoriaId: string | null
  criadoEm: string
}
```

### `src/services/auth.service.ts`

```ts
import { apiFetch } from './api-client'
import { setToken, clearToken } from '@/lib/auth'

interface LoginRequest { email: string; senha: string }
interface RegistrarRequest { email: string; senha: string }
interface TokenResponse { token: string; tipo: string; expiresIn: number }
interface UsuarioResponse { id: string; email: string; criadoEm: string }

export const authService = {
  async login(data: LoginRequest): Promise<TokenResponse> {
    const res = await apiFetch<TokenResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
    })
    setToken(res.token)
    return res
  },

  async registrar(data: RegistrarRequest): Promise<UsuarioResponse> {
    return apiFetch('/api/auth/registrar', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  },

  logout(): void {
    clearToken()
  },
}
```

### `src/services/contas.service.ts`

```ts
import { apiFetch } from './api-client'
import type { Conta, SaldoResponse, SaldoTotalResponse } from '@/types/conta'

interface CriarContaRequest {
  nome: string
  tipo: string
  saldoInicialValor: number
  saldoInicialMoeda: string
}

export const contasService = {
  listar: () => apiFetch<Conta[]>('/api/contas'),
  criar: (data: CriarContaRequest) =>
    apiFetch<Conta>('/api/contas', { method: 'POST', body: JSON.stringify(data) }),
  buscarPorId: (id: string) => apiFetch<Conta>(`/api/contas/${id}`),
  calcularSaldo: (id: string) => apiFetch<SaldoResponse>(`/api/contas/${id}/saldo`),
  saldoTotal: () => apiFetch<SaldoTotalResponse>('/api/contas/saldo-total'),
  desativar: (id: string) =>
    apiFetch<void>(`/api/contas/${id}`, { method: 'DELETE' }),
}
```

### `src/services/categorias.service.ts`

```ts
import { apiFetch } from './api-client'
import type { Categoria } from '@/types/categoria'

interface CriarCategoriaRequest {
  nome: string
  tipo: 'RECEITA' | 'DESPESA'
  categoriaPaiId?: string
}

export const categoriasService = {
  listar: () => apiFetch<Categoria[]>('/api/categorias'),
  criar: (data: CriarCategoriaRequest) =>
    apiFetch<Categoria>('/api/categorias', { method: 'POST', body: JSON.stringify(data) }),
}
```

### `src/services/transacoes.service.ts`

```ts
import { apiFetch } from './api-client'
import type { Transacao } from '@/types/transacao'

interface CriarTransacaoRequest {
  tipo: string
  valorValor: number
  valorMoeda: string
  data: string
  descricao: string
  contaId: string
  contaDestinoId?: string
  categoriaId?: string
}

interface ListarTransacoesParams {
  contaId?: string
  dataInicio?: string
  dataFim?: string
  tipo?: string
  page?: number
  size?: number
}

export const transacoesService = {
  listar: (params?: ListarTransacoesParams) => {
    const qs = new URLSearchParams()
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined) qs.set(k, String(v))
      })
    }
    const query = qs.toString() ? `?${qs}` : ''
    return apiFetch<{ content: Transacao[]; totalElements: number }>(`/api/transacoes${query}`)
  },
  criar: (data: CriarTransacaoRequest) =>
    apiFetch<Transacao>('/api/transacoes', { method: 'POST', body: JSON.stringify(data) }),
}
```

---

## Parte 3 -- Layout, providers e paginas

### `src/providers/query-provider.tsx`

```tsx
'use client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'

export function QueryProvider({ children }: { children: React.ReactNode }) {
  const [client] = useState(() => new QueryClient())
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}
```

### `src/providers/auth-provider.tsx`

```tsx
'use client'
import { createContext, useContext, useState, useCallback } from 'react'
import { isAuthenticated, clearToken } from '@/lib/auth'

interface AuthContextValue {
  loggedIn: boolean
  logout: () => void
  refresh: () => void
}

const AuthContext = createContext<AuthContextValue>({
  loggedIn: false,
  logout: () => {},
  refresh: () => {},
})

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [loggedIn, setLoggedIn] = useState(() => isAuthenticated())

  const logout = useCallback(() => {
    clearToken()
    setLoggedIn(false)
  }, [])

  const refresh = useCallback(() => {
    setLoggedIn(isAuthenticated())
  }, [])

  return (
    <AuthContext.Provider value={{ loggedIn, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
```

### `src/hooks/use-auth.ts`

```ts
export { useAuth } from '@/providers/auth-provider'
```

### Atualizar `src/app/layout.tsx`

Envolver com QueryProvider + AuthProvider. Atualizar metadata (titulo: "Financas Lab").
Manter fonts Geist existentes.

### Instalar componentes shadcn/ui

Rodar cada um individualmente (nao todos de vez):

```
npx shadcn@latest add button
npx shadcn@latest add input
npx shadcn@latest add card
npx shadcn@latest add form
npx shadcn@latest add label
npx shadcn@latest add badge
npx shadcn@latest add separator
npx shadcn@latest add dropdown-menu
npx shadcn@latest add sheet
npx shadcn@latest add sidebar
npx shadcn@latest add table
npx shadcn@latest add dialog
npx shadcn@latest add select
npx shadcn@latest add sonner
```

Se algum componente nao existir no `base-nova` style: pular e registrar no corpo do commit.

### `src/app/(auth)/login/page.tsx`

Pagina de login client-side:
- Form com email + senha usando React Hook Form + Zod
- Chama `authService.login()` no submit
- Em sucesso: chama `auth.refresh()` e usa `router.push('/dashboard')`
  (ou a rota equivalente no (dashboard) group)
- Em erro 401: exibe "Email ou senha invalidos"
- Usar componentes shadcn: Card, Form, Input, Button, Label
- Layout centralizado na tela

### `src/app/(dashboard)/layout.tsx`

Layout protegido:
- Verifica `isAuthenticated()` no mount (client-side); se falso, redireciona para `/login`
- Sidebar com links de navegacao:
  - Dashboard (icone Home)
  - Contas (icone CreditCard)
  - Transacoes (icone ArrowLeftRight)
  - Categorias (icone Tag)
  - Relatorios (icone BarChart3)
- Header com titulo + nome do usuario + botao Sair
- Usar componente Sidebar do shadcn/ui

### `src/app/(dashboard)/page.tsx`

Home do dashboard:
- Titulo "Dashboard"
- Card de saldo total: chama `contasService.saldoTotal()` com TanStack Query
  (`useQuery({ queryKey: ['saldo-total'], queryFn: () => contasService.saldoTotal() })`)
- Exibe valor formatado em BRL: `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor)`
- Exibe total de contas ativas
- Estado de loading com skeleton (Badge ou div com animate-pulse)
- Estado de erro com mensagem simples

---

## `.env.local`

Criar `frontend/.env.local` (nao commitado -- ja deve estar no `.gitignore`):

```
NEXT_PUBLIC_API_URL=http://localhost:8080
```

Verificar que `frontend/.gitignore` inclui `.env.local`. Se nao incluir, adicionar.

---

## `scripts/dev-front.ps1`

Criar em `scripts/` (raiz do projeto, junto com `dev.ps1`):

```powershell
$frontendPath = Join-Path $PSScriptRoot "..\frontend"
Set-Location $frontendPath
npm run dev
```

---

## Testes

### `src/services/api-client.test.ts`

Unit tests para `apiFetch`:
- Testa que Authorization header e adicionado quando token existe
- Testa que Authorization header NAO e adicionado quando sem token
- Testa que ApiError e lancado em resposta nao-ok
- Testa que retorna undefined para status 204
- Usar `vi.stubGlobal('fetch', ...)` para mock de fetch

### `src/lib/auth.test.ts`

Unit tests para `getToken`, `setToken`, `clearToken`, `isAuthenticated`:
- Usar `vi.stubGlobal` para mockar localStorage
- Ou configurar jsdom que ja provem localStorage

### `src/app/(auth)/login/page.test.tsx`

Component test para a pagina de login:
- Renderiza campos email e senha
- Submete e chama `authService.login`
- Mostra erro em caso de falha 401
- Usar `vi.mock('@/services/auth.service')` para mockar o service

---

## Storybook -- primeira story

Criar `src/components/ui/button.stories.tsx` (apos `npx shadcn add button`):

```tsx
import type { Meta, StoryObj } from '@storybook/nextjs'
import { Button } from './button'

const meta: Meta<typeof Button> = {
  component: Button,
  title: 'UI/Button',
}
export default meta

type Story = StoryObj<typeof Button>

export const Primary: Story = { args: { children: 'Confirmar' } }
export const Destructive: Story = { args: { children: 'Excluir', variant: 'destructive' } }
export const Outline: Story = { args: { children: 'Cancelar', variant: 'outline' } }
```

---

## Atualizar `CLAUDE.md` do projeto

Adicionar secao `## Frontend` apos a secao `## Stack`, com:

```
## Frontend

- Framework: Next.js 16 (App Router) em `frontend/`.
- Testes: Vitest + Testing Library (`npm test` em `frontend/`).
- Design system: shadcn/ui com estilo `base-nova`. Adicionar componentes via `npx shadcn@latest add <nome>`.
- Camada de API: `src/services/` com `api-client.ts` (fetch + JWT). NAO usar fetch direto fora de `services/`.
- Auth: JWT em localStorage via `src/lib/auth.ts`. Provider em `src/providers/auth-provider.tsx`.
- Dev: `.\scripts\dev-front.ps1` (inicia Next.js dev server).
```

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-9-frontend-foundation

2. LEIA frontend/AGENTS.md e frontend/CLAUDE.md primeiro
   LEIA os guias relevantes em frontend/node_modules/next/dist/docs/

3. cd frontend && npm install (garantir dependencias instaladas)

4. Instalar pacotes de teste:
   npm install -D vitest @vitejs/plugin-react vite-tsconfig-paths jsdom
   npm install -D @testing-library/react @testing-library/user-event @testing-library/jest-dom

5. npx storybook@latest init --yes
   Se falhar: instalar @storybook/experimental-nextjs-vite manualmente

6. Criar vitest.config.ts + src/test/setup.ts

7. Criar estrutura de pastas (criar arquivos vazios ou com exports minimos para evitar
   erros de import ao rodar next build)

8. Atualizar eslint.config.mjs (regra no-restricted-globals para fetch)

9. commit: feat(frontend): configura Vitest, Storybook e estrutura de pastas

10. Implementar toda a Parte 2 (types/, lib/auth.ts, services/)

11. commit: feat(frontend): implementa api-client com JWT e servicos base

12. Instalar componentes shadcn/ui (um a um)
    Implementar providers/, hooks/use-auth.ts
    Atualizar app/layout.tsx
    Implementar (auth)/login/page.tsx
    Implementar (dashboard)/layout.tsx + page.tsx
    Criar scripts/dev-front.ps1

13. commit: feat(frontend): implementa layout dashboard, paginas e componentes UI

14. Escrever testes (api-client.test.ts, auth.test.ts, login/page.test.tsx)
    Criar button.stories.tsx
    Verificar que `npm run test:run` passa
    Verificar que `npm run build` passa (Next.js build)
    Verificar que `npm run lint` passa
    Atualizar CLAUDE.md do projeto (secao Frontend)
    Atualizar docs/progresso.md (registra 5.9)

15. commit: feat(frontend): testes, documentacao e script de dev; registra sub-etapa 5.9
    (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-9.md)

16. /ship -> PR com reviews; corrigir apontamentos autonomamente
```

---

## Estrutura de commits (5.9)

```
feat(frontend): configura Vitest, Storybook e estrutura de pastas
feat(frontend): implementa api-client com JWT e servicos base
feat(frontend): implementa layout dashboard, paginas e componentes UI
feat(frontend): testes, documentacao e script de dev; registra sub-etapa 5.9
```

---

## Arquivos de referencia (ler antes de implementar)

- `frontend/AGENTS.md` -- aviso sobre breaking changes do Next.js 16
- `frontend/package.json` -- versoes instaladas (Zod 4, React Query 5, etc.)
- `frontend/components.json` -- configuracao do shadcn (style, aliases, css)
- `frontend/src/app/globals.css` -- tokens oklch (nao alterar)
- `frontend/src/lib/utils.ts` -- ja existe (nao recriar)
- `frontend/eslint.config.mjs` -- config atual a estender
- `frontend/tsconfig.json` -- paths alias `@/*` -> `./src/*`
- `scripts/dev.ps1` -- padrao para criar dev-front.ps1

---

## Restricoes

- NAO alterar `frontend/src/app/globals.css` (tokens oklch ja definidos)
- NAO instalar axios -- usar fetch nativo via api-client
- NAO usar `jest` -- apenas `vitest`
- NAO alterar `frontend/tsconfig.json` (ja configurado)
- NAO alterar `frontend/components.json` (shadcn ja configurado)
- NAO commitar `.env.local` (verificar .gitignore)
- NAO usar `pwsh` em `.ps1` -- apenas `powershell`
- Zod: versao 4 instalada -- API diferente da v3 (ex: `z.object` permanece, mas `z.infer` e identico)
- Tailwind: versao 4 -- sem `tailwind.config.js`, configuracao via CSS em `globals.css`
- Se hook bloquear commit: ler a mensagem, corrigir sem --no-verify
- Se `npm run build` falhar em alguma pagina por Server Component incompativel com hook:
  adicionar `'use client'` no topo da pagina

---

## Estado esperado ao terminar

- PR aberto com 4 commits acima de main.
- `npm run test:run` em `frontend/` -- todos os testes passando.
- `npm run build` em `frontend/` -- BUILD SUCCESS.
- `npm run lint` em `frontend/` -- sem erros.
- Storybook configurado (pode ter warnings no init -- nao e bloqueante).
- GET /api/contas/saldo-total integrado na home do dashboard.
- Login funcional (formulario chama o backend real em localhost:8080).
- docs/progresso.md com 5.9 registrada.
- docs/prompts/prompt-etapa-5-9.md commitado no ultimo commit.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO rodar /ship mais de uma vez.
- NAO implementar features de negocio (CRUD de contas, transacoes) -- ficam para 5.10+.
