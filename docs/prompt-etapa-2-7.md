# Prompt — Etapa 2.7: Inicializar projeto Next.js

## Contexto

A Etapa 2.6.2 foi concluída e fechada via PR #25. `main` está em `23a65cc`, working tree limpo.

Esta etapa inicializa o frontend do projeto: pasta `frontend/` na raiz do mesmo repo, projeto Next.js 16 (mais recente estável) com TypeScript + Tailwind + ESLint + App Router + `src/` dir + alias `@/*`. Nada de feature ainda — apenas pipeline (build + lint) passando localmente e no CI.

Objetivo do roadmap: frontend inicializado, build passando, sem feature ainda.

Escopo decidido nesta etapa (calibrado com operador antes da redação):

- **Localização:** `frontend/` na raiz do mesmo repo (não monorepo formal — só um diretório).
- **Versão:** Next.js 16.x (16.2 atual estável). Backend mantém em monorepo lógico, sem ferramenta de gerenciamento monorepo (sem `pnpm workspaces`, sem `turborepo`, sem `nx`).
- **Stack:** TypeScript + Tailwind CSS + ESLint + App Router + `src/` dir + alias `@/*`. Tudo via `create-next-app` flags.
- **Bundler:** Turbopack (default no Next.js 16, sem ação extra).
- **PWA adiada para Camada 2.** O roadmap original cita `next-pwa`, mas o package está sem manutenção desde 2024. Sucessores (`serwist` + abordagem nativa via `manifest.ts`) ficam como decisão da Camada 2 quando o frontend tiver telas reais. Esta etapa **não instala** nenhum package PWA.
- **Dependências adicionais instaladas:** `@tanstack/react-query`, `zod`, `react-hook-form`, `@hookform/resolvers`. Roadmap pede. Instaladas mas não usadas — ficam disponíveis pra Camada 2.
- **shadcn/ui inicializada agora.** Decisão arquitetural — vale tomar de uma vez. Custo baixo. Inicializar via `npx shadcn@latest init` com defaults sensatos. Sem componentes ainda.
- **`AGENTS.md` e `CLAUDE.md` gerados pelo create-next-app:** **inspecionar conteúdo, decidir caso a caso**. Se forem genéricos ("siga padrões Next.js"), remover (temos nosso `CLAUDE.md` na raiz). Se trouxerem instruções específicas úteis (ex: "use App Router, não Pages Router"), preservar movendo conteúdo relevante para nosso próprio CLAUDE.md ou AGENTS.md do frontend. Padrão: sem ceremony.
- **CI ajustado:** `ci.yml` ganha steps de Node 20 setup + `npm ci` em `frontend/` + `npm run build` em `frontend/` + `npm run lint` em `frontend/`. **Job único** com Java + Node (não dois jobs separados). Mais simples pro projeto pequeno; refatora pra dois jobs quando justificar.
- **Validação destrutiva:** ESLint do Next.js tem regras strict. Erro proposital de ESLint → CI vermelho. Mesmo padrão das etapas anteriores.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra `23a65cc fix: etapa 2.6.2 — UX limpa em checagem de Docker nos scripts PowerShell (#25)`
- `docs/prompt-etapa-2-7.md` presente como untracked (este próprio arquivo, anexado pelo operador antes de iniciar). Se não aparecer em `git status`, parar e reportar.
- Working tree sem outras mudanças
- Node.js 22 instalado no ambiente (operador confirmou no Camada 0). Validar com `node --version` antes de começar.
- Docker Desktop pode estar parado — esta etapa não usa.

Validar com `git status`, `git log --oneline -1`, `node --version`, `npm --version` antes de começar.

## Pesquisar antes de chutar versões

Antes de inicializar o projeto Next.js, **pesquisar npm registry** (https://www.npmjs.com/package/create-next-app) para confirmar versão estável atual. Esperado: 16.2.x ou superior. **Se retornar versão menor que 16, parar e reportar** — pode indicar problema com cache ou registry.

Para as 4 dependências adicionais (`@tanstack/react-query`, `zod`, `react-hook-form`, `@hookform/resolvers`), instalar com `npm install` sem fixar versão (deixa o npm resolver para a latest estável). As versões resolvidas são registradas em `decisoes.md` na Tarefa 6 após instalação real.

## Tarefas

### Tarefa 1 — Validar pré-requisitos do ambiente

```bash
git status
git log --oneline -1
node --version    # esperado: v22.x.x ou superior, mínimo v20.9
npm --version     # esperado: 10.x ou superior
```

Se `node --version` < 20.9, parar e reportar. Next.js 16 exige Node 20+.

### Tarefa 2 — Inicializar projeto Next.js em `frontend/`

A partir da raiz do projeto (`C:\projetos\financas-lab`), rodar:

```bash
npx create-next-app@latest frontend \
  --typescript \
  --tailwind \
  --eslint \
  --app \
  --src-dir \
  --import-alias "@/*" \
  --use-npm \
  --skip-install
```

Notas:
- `--skip-install` evita rodar `npm install` automaticamente — vamos rodar depois explicitamente. Reduz risco de logs ruidosos durante a inicialização.
- Não usar `--yes` (que aceita defaults da última execução cached, comportamento imprevisível).
- Próximo passo é entrar na pasta e rodar `npm install` explicitamente.

Após criação:

```bash
cd frontend
npm install
```

Esperado: `package.json`, `package-lock.json`, `tsconfig.json`, `eslint.config.mjs` (ou `.eslintrc.json`), `next.config.ts`, `postcss.config.mjs`, `tailwind.config.ts` (se Tailwind v3) ou apenas `app/globals.css` com `@import "tailwindcss"` (se Tailwind v4 — provável no Next 16). Página default em `frontend/src/app/page.tsx`.

### Tarefa 3 — Inspecionar arquivos `AGENTS.md` e `CLAUDE.md` gerados

Next.js 16 inclui esses arquivos no scaffold. Antes de decidir manter ou remover:

```bash
ls frontend/AGENTS.md frontend/CLAUDE.md 2>/dev/null && cat frontend/AGENTS.md frontend/CLAUDE.md
```

**Se o conteúdo for genérico** (ex: "use App Router", "siga conventions do Next.js"):
- Remover ambos arquivos.
- Confiar que nosso `CLAUDE.md` na raiz cobre instruções gerais.

**Se trouxer instrução específica e útil** (ex: padrão de uso do Turbopack, conventions específicas):
- Preservar conteúdo. Mover instrução relevante para nosso `CLAUDE.md` da raiz, ou manter `frontend/AGENTS.md` apontando pra `../CLAUDE.md`.
- Decisão fica registrada em `decisoes.md`.

**Padrão default:** se não tiver certeza, **remover ambos**. Lições da fábrica: `CLAUDE.md` curto > `CLAUDE.md` enciclopédia. Multiplicar arquivos de instrução por subdiretório multiplica risco de conflito.

Reportar o conteúdo encontrado e a decisão tomada.

### Tarefa 4 — Instalar dependências adicionais

A partir de `frontend/`:

```bash
npm install @tanstack/react-query zod react-hook-form @hookform/resolvers
```

Não fixa versão — deixa npm resolver para latest estáveis. Versões reais ficam em `frontend/package.json`.

Após instalação, validar versões resolvidas:

```bash
node -e "const p = require('./package.json'); console.log(JSON.stringify(p.dependencies, null, 2))"
```

Reportar versões — vão pro `decisoes.md` na Tarefa 6.

### Tarefa 5 — Inicializar shadcn/ui

```bash
npx shadcn@latest init
```

Prompts esperados (responder com defaults sensatos):
- "Which color would you like to use as base color?" → **Slate** (neutro)
- "Where is your global CSS file?" → manter default (provavelmente `src/app/globals.css`)
- "Do you want to use CSS variables for theming?" → **Yes**
- "Where is your `tailwind.config.ts`?" → manter default (pode não aparecer em Next 16 com Tailwind v4 — se não aparecer, OK)
- "Configure the import alias for components?" → manter default (`@/components`)
- "Configure the import alias for utils?" → manter default (`@/lib/utils`)

**Se algum prompt for muito ambíguo ou abrir caminho que parece errado, parar e reportar** ao operador antes de continuar.

Após init, esperado:
- `components.json` na raiz de `frontend/`
- `src/lib/utils.ts` (helper `cn()` utilizando `clsx` + `tailwind-merge`)
- Possíveis ajustes em `tailwind.config.ts` ou `globals.css` (CSS variables para tema)

**Não instalar nenhum componente ainda.** Apenas inicialização.

### Tarefa 6 — Validar build e lint

```bash
npm run build
```

Esperado: BUILD SUCCESS, output em `.next/`. Possíveis warnings de "no pages found" são normais — temos só a página default.

```bash
npm run lint
```

Esperado: 0 erros. Pode ter warnings — registrar quais (mas não corrigir nesta etapa, é Camada 2).

**Se `build` ou `lint` falharem**, parar e reportar antes de prosseguir. Build verde local é pré-requisito para integração no CI.

### Tarefa 7 — Atualizar `.gitignore` da raiz

`frontend/` traz seu próprio `.gitignore` (gerado pelo create-next-app). Conferir que `.gitignore` da raiz **não** sobrescreve regras importantes do frontend (`node_modules/`, `.next/`, `.env*.local`).

Se houver conflito ou ausência de regra crítica, **adicionar** ao `.gitignore` da raiz:

```
# Frontend (Next.js)
frontend/.next/
frontend/node_modules/
frontend/.env*.local
frontend/out/
```

Se já cobertos pelo `.gitignore` do `frontend/`, não duplicar — confirmar via `git status` que esses paths não aparecem como untracked.

### Tarefa 8 — Atualizar `ci.yml`

Localizar `.github/workflows/ci.yml`. Adicionar step de validação do frontend **após** os steps existentes do Java/Maven (não antes — Java leva mais tempo, falha cedo).

Mudanças esperadas no `ci.yml`:

1. Step novo `Setup Node.js`:
   ```yaml
   - name: Setup Node.js
     uses: actions/setup-node@v4
     with:
       node-version: '22'
       cache: 'npm'
       cache-dependency-path: 'frontend/package-lock.json'
   ```

2. Step novo `Install frontend dependencies`:
   ```yaml
   - name: Install frontend dependencies
     working-directory: frontend
     run: npm ci
   ```

3. Step novo `Lint frontend`:
   ```yaml
   - name: Lint frontend
     working-directory: frontend
     run: npm run lint
   ```

4. Step novo `Build frontend`:
   ```yaml
   - name: Build frontend
     working-directory: frontend
     run: npm run build
   ```

**Manter o restante do `ci.yml` inalterado.** O job continua sendo único — Java + Node no mesmo job, executados sequencialmente.

**Não adicionar matriz de versões Node**, **não criar segundo job**, **não adicionar caching customizado** além do que `actions/setup-node@v4` faz nativo.

### Tarefa 9 — Atualizar README.md

Adicionar nova seção "Frontend" após a seção "Comandos do projeto" existente (criada na 2.6). Estrutura:

```markdown
## Frontend

Aplicação web em Next.js 16 (App Router) + TypeScript + Tailwind. Localização: `frontend/`.

| Comando (a partir de `frontend/`) | Função |
|---|---|
| `npm install` | Instala dependências |
| `npm run dev` | Sobe dev server em http://localhost:3000 |
| `npm run build` | Build de produção |
| `npm run lint` | Roda ESLint |
| `npm run start` | Sobe build de produção |

### Stack

- Next.js 16 (App Router, Turbopack)
- TypeScript
- Tailwind CSS
- ESLint
- shadcn/ui (componentes via copy, não dependência)
- TanStack Query, Zod, React Hook Form (instaladas, sem uso ainda — Camada 2)

PWA fica para Camada 2.
```

### Tarefa 10 — Atualizar `decisoes.md`

**10a.** Adicionar nova subseção "Frontend" após a seção "Comandos atômicos do projeto":

```markdown
## Frontend

Aplicação Next.js em `frontend/`.

| Componente | Escolha | Versão |
|---|---|---|
| Framework | Next.js | <versão real do package.json> |
| Bundler | Turbopack (default Next 16) | gerenciado pelo Next |
| Linguagem | TypeScript (strict) | <versão> |
| Estilização | Tailwind CSS | <versão> |
| Lint | ESLint (config Next) | <versão eslint-config-next> |
| Componentes | shadcn/ui (copy, não dependência) | inicializado, sem componentes |
| HTTP / cache | @tanstack/react-query | <versão> |
| Validação | Zod | <versão> |
| Forms | React Hook Form + @hookform/resolvers | <versões> |
| Node.js | mínimo 20.9, recomendado 22 LTS | — |

**Decisões registradas:**

- **Localização:** `frontend/` na raiz do repo. Sem ferramenta de monorepo (workspaces, turborepo, nx). Razão: simplicidade. Migrar para monorepo formal só quando justificar.
- **PWA adiada para Camada 2.** Package `next-pwa` não é mantido desde 2024; sucessor `serwist` ou abordagem nativa via `manifest.ts` ficam como decisão da Camada 2 quando houver telas reais.
- **shadcn/ui via copy.** Componentes vão para `src/components/ui/` quando instalados via `npx shadcn add <componente>`. Não é dependência de runtime — o código fica versionado no repo.
- **`AGENTS.md` e `CLAUDE.md` do scaffold:** <decisão tomada na Tarefa 3 — "removidos" ou "preservados parcialmente em ...">
- **CI:** job único com Java + Node executados sequencialmente. Refatorar para dois jobs paralelos só quando justificar.
```

**10b.** Adicionar entrada no **"Histórico de mudanças"** no fim do arquivo, no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.7 concluída: frontend Next.js 16 inicializado em `frontend/`. Stack: TypeScript + Tailwind + ESLint + App Router + shadcn/ui + TanStack Query + Zod + React Hook Form. CI atualizado com steps de Node 20 + lint + build do frontend. PWA adiada para Camada 2.
```

### Tarefa 11 — Atualizar `progresso.md`

**11a.** Atualizar campo "Última atualização" no topo: `2026-05-08 (Etapa 2.7)`.

**11b.** Marcar como `[x]` na seção da Camada 1 o critério `Projeto Next.js inicializado`.

**11c.** Adicionar nova seção **"Lições da Etapa 2.7"** logo antes de **"Lições da Etapa 2.6.2"** (mantendo ordem decrescente):

```markdown
## Lições da Etapa 2.7

### Candidatos a hook (automatizar em etapas futuras)

(Preencher com lições reais.)

### Lições de ambiente

(Preencher com lições reais.)
```

**Regra dura:** só registrar lições **realmente observadas** durante a execução. Se algo notável surgir (versão Next que muda comportamento, conflito Tailwind v3 vs v4, comportamento estranho de ESLint, fricção de shadcn/ui em Next 16, etc.), registrar honestamente. Se nada digno surgir, deixar `(Nenhum novo nesta etapa.)` — não inventar.

**11d.** Adicionar entrada no **"Histórico de mudanças deste documento"** no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.7 concluída: Next.js 16 inicializado em `frontend/`, dependências adicionais instaladas, shadcn/ui configurado, CI atualizado, decisões e stack registradas. Mergeado via PR #XX.
```

### Tarefa 12 — Versionar este próprio prompt

Confirmar que `docs/prompt-etapa-2-7.md` está em disco como untracked e incluir no commit de docs.

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Arquivos permitidos:
   - Tudo dentro de `frontend/` (criação)
   - `.gitignore` (raiz, apenas se necessário)
   - `.github/workflows/ci.yml`
   - `README.md`
   - `docs/decisoes.md`
   - `docs/progresso.md`
   - `docs/prompt-etapa-2-7.md` (este arquivo)

2. **Não tocar em `pom.xml`, `application.yml`, `docker-compose.yml`, `scripts/*.ps1`, `src/main/...`, `src/test/...`.** Etapa 2.7 é frontend-only.

3. **Não criar feature.** Esta etapa é pipeline + scaffold. Não criar pages, não criar components customizados, não mexer na página default do Next.js. Se a página default tiver conteúdo "demo" excessivo (links Vercel etc), tudo bem — fica pra Camada 2 limpar.

4. **Não inicializar PWA.** `next-pwa` não é mantido. Sucessor (`serwist` ou nativo) fica para Camada 2. Esta etapa **não toca** em manifest, service worker, ou qualquer artefato PWA.

5. **Não fazer escolhas exóticas no shadcn/ui init.** Defaults sensatos. Se algum prompt for ambíguo ou parecer estranho, parar e reportar.

6. **Não criar segundo job no CI.** Job único com Java + Node, executados sequencialmente. Steps do Node ficam **após** os do Java/Maven.

7. **Não adicionar matriz de versões** (Node 18, 20, 22), **não cachear node_modules manualmente** (`actions/setup-node@v4` com `cache: 'npm'` resolve), **não usar `pnpm` ou `yarn`** (nesta etapa é npm — alinhado com decisão da Camada 0).

8. **Não inicializar TypeScript "non-strict".** O default do create-next-app já é strict. Manter.

9. **Não mover `tsconfig.json` ou `eslint.config.mjs` para o root do projeto.** Eles ficam dentro de `frontend/`. Não criar ferramenta de monorepo formal.

10. **Não fixar versões nas dependências adicionais.** `npm install <pkg>` sem `@x.y.z` — deixa npm resolver. Versões reais ficam no `package-lock.json` (commit) e em `decisoes.md` (Tarefa 10a).

11. **Não rodar `npm run dev` no agente.** Bloqueia o terminal. Validação `dev` é do operador, manualmente, depois do merge.

12. **Não criar `src/components/ui/` com componentes shadcn**. Apenas inicialização. Adicionar componentes só na Camada 2.

13. **`bash_tool` é bash, não PowerShell.** Comandos POSIX. Pra invocar PowerShell, `powershell.exe -Command ...`. Lição registrada desde 2.5.

14. **Encoding UTF-8 sem BOM em arquivos criados manualmente** (decisões em `decisoes.md`, lições em `progresso.md`, edits em `README.md`). Validar com `xxd` se aplicável.

15. **Não antecipar Etapa 2.8 (wrap-up).** Sem conclusões fortes sobre Camada 1 fechada. A 2.8 fará isso.

16. **Lições da Etapa 2.7 só registram observações reais.** Se Tarefa 11c ficar com `(Nenhum novo nesta etapa.)`, tudo bem. Não inventar lições.

## Estrutura de commits

Branch: `feat/frontend-init`

Commits atômicos, em ordem:

**Commit 1** — `feat: inicializa projeto Next.js 16 em frontend/`
- Tudo gerado pelo `create-next-app` (todo o conteúdo de `frontend/`)
- Inclui ajustes de Tarefa 3 se houver (remoção de `AGENTS.md`/`CLAUDE.md` se decidido)

**Commit 2** — `feat: adiciona dependencias do frontend (TanStack Query, Zod, RHF) e shadcn/ui`
- `frontend/package.json` (com novas deps)
- `frontend/package-lock.json` (atualizado)
- `frontend/components.json`
- `frontend/src/lib/utils.ts`
- Quaisquer ajustes em `frontend/tailwind.config.*` ou `frontend/src/app/globals.css` feitos pelo shadcn init

**Commit 3** — `ci: adiciona steps de lint e build do frontend`
- `.github/workflows/ci.yml`
- `.gitignore` (se necessário)

**Commit 4** — `docs: registra etapa 2.7 (frontend Next.js) em README, decisoes e progresso`
- `README.md`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-2-7.md`

## Validação antes de abrir PR

```bash
# Build local do frontend funciona:
cd frontend
npm run build
npm run lint

# Voltar para a raiz, working tree esperado:
cd ..
git status   # nothing to commit
git log --oneline -6
```

## PR

Título: `feat: etapa 2.7 — inicializa frontend Next.js 16 em frontend/`

Body sugerido (ajustar com observações reais):

```markdown
## Summary

Implementa a Etapa 2.7 do roadmap: frontend Next.js 16 inicializado em `frontend/`. Pipeline (build + lint) passando localmente e no CI. Sem features ainda.

### Mudanças

- `frontend/`: projeto Next.js 16 com TypeScript + Tailwind + ESLint + App Router + `src/` dir + alias `@/*`. Turbopack default.
- Dependências adicionais: `@tanstack/react-query`, `zod`, `react-hook-form`, `@hookform/resolvers`. Instaladas mas sem uso ainda.
- shadcn/ui inicializado (`components.json`, `src/lib/utils.ts`). Sem componentes ainda.
- `.github/workflows/ci.yml`: novos steps Node 22 + npm ci + lint + build (job único com Java).
- `README.md`: nova seção "Frontend" com tabela de comandos e stack.
- `decisoes.md`: subseção "Frontend" com versões reais.
- `progresso.md`: critério marcado, lições registradas.

### Decisões de escopo

- **PWA adiada para Camada 2.** `next-pwa` não é mantido; sucessor (`serwist` ou nativo) decide quando houver telas reais.
- **shadcn/ui inicializada agora**, mas sem componentes. Decisão arquitetural tomada de uma vez.
- **CI com job único** Java + Node sequencial. Refatora para dois jobs paralelos só quando justificar.
- **`AGENTS.md` e `CLAUDE.md` do scaffold:** <descrever decisão real tomada na Tarefa 3>

### Validação

- `npm run build` local: PASSOU
- `npm run lint` local: PASSOU
- CI: <verde após push>

### Próximo passo

Etapa 2.8 (wrap-up Camada 1) — fora do escopo deste PR.
```

## Pós-criação do PR

1. Abrir o PR via `gh pr create`.
2. Capturar o número.
3. Editar `docs/progresso.md` substituindo `Mergeado via PR #XX` por `Mergeado via PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico do progresso.md`
5. Push.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- `main` local sincronizada com `origin/main`, incluindo squash commit da 2.7
- `git status` limpo
- `frontend/` com Next.js 16 funcional
- `npm run build` em `frontend/` passa
- `npm run lint` em `frontend/` passa
- CI verde no PR final
- Branch `feat/frontend-init` deletada local e remotamente

Reportar ao operador o estado final com `git log --oneline -3`, `git status`, e `cd frontend && cat package.json | head -30`. Parar.

## O que NÃO fazer ao terminar

- Não criar prompt da 2.8
- Não mexer em backend
- Não criar features no frontend
- Não tentar inicializar PWA
- Não rodar `npm run dev` no bash_tool
- Não sugerir "próximo passo" espontaneamente
