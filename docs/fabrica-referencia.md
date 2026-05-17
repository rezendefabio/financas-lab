# Referência da Fábrica — financas-lab

> Guia completo de skills, agentes, hooks e fluxos de trabalho da fábrica AI-native.
> Leia este documento para entender como cada peça se encaixa e como o `/plan` leva
> um objetivo até um PR revisado e pronto para merge.

---

## Visão Geral

A fábrica é composta por três camadas que colaboram:

```
OPERADOR
   │
   ▼
SKILLS (slash commands)       ← orquestradoras; o operador as invoca
   │
   ▼
AGENTES (subagents)           ← especialistas; as skills os spawnam
   │
   ▼
HOOKS (git + Claude Code)     ← guardiões; disparam em eventos
```

O operador nunca precisa saber como cada peça funciona internamente —
ele invoca `/plan <objetivo>` e a fábrica entrega PRs prontos para review.

---

## Skills (Slash Commands)

### Skills de Execução de Tarefas

| Skill | Argumento | O que faz |
|-------|-----------|-----------|
| `/plan` | objetivo de alto nível | Spawna planejador (Opus) → quebra em tasks → registra em `tasks.json` → spawna executores paralelos em worktrees isolados → reviews automáticos → cleanup |
| `/batch` | `etapa-X-Y etapa-X-Z ...` | Executa múltiplos prompts de `docs/prompts/` em paralelo via worktrees isolados; cada arquivo = um agente autônomo |
| `/ship` | _(sem argumento)_ | Entrega a branch atual: verifica segurança → `check.ps1` → `check-front.ps1` (se frontend) → push → cria PR → 3 reviews automáticos → corrige apontamentos objetivos autonomamente |

### Skills de Geração de Código

| Skill | Argumento | O que faz |
|-------|-----------|-----------|
| `/feature` | `nome-bounded-context` | Cria 11 arquivos Java stub (domain, application, infrastructure, interfaces) para um bounded context novo |
| `/feature-front` | `nome-bounded-context` | Cria 6 arquivos frontend stub (types, service, index, list page, create page, detail page) lendo os DTOs Java como fonte de verdade. Infere interfaces TypeScript, schema Zod e métodos do service automaticamente |
| `/migrate` | `nome-bounded-context` | Fluxo pós-`/feature`: encadeia `/write-migration` (SQL Flyway) e `/write-test` (unit test da entidade domain) |
| `/write-migration` | `nome-bounded-context` | Spawna `migration-writer` para gerar `V<N>__cria_tabela_<nome>.sql` a partir das anotações JPA da Entity |
| `/write-test` | `path-da-classe` | Spawna `test-writer` para gerar testes no nível correto (unit/Mockito/Testcontainers/MockMvc/Vitest) |
| `/write-report` | descrição multiline do relatório | Spawna `report-writer` para gerar componente React de relatório impresso (`@react-pdf/renderer`). Layout padrão: cabeçalho + tabela + rodapé. Nunca usa shadcn dentro do Document |
| `/write-job` | descrição multiline do job | Spawna `job-writer` para gerar scaffold completo de job Spring Batch: ItemReader, ItemProcessor, ItemWriter, JobConfig, JobListener, JobLauncher ou Scheduler, migration Flyway para tabelas `BATCH_*` |

### Skills de Review Manual

| Skill | Argumento | O que faz |
|-------|-----------|-----------|
| `/review-pr` | `<numero-PR>` | Spawna `pr-reviewer` (Haiku) para revisar o PR antes do merge |
| `/review-arch` | `<numero-PR>` | Spawna `architect-reviewer` (Sonnet) para revisar decisões estruturais contra ADRs |
| `/review-front` | `<numero-PR>` | Spawna `front-reviewer` (Haiku) para revisar mudanças em `frontend/` |

### Skills de Monitoramento (Routines)

| Skill | Quando invocar | O que faz |
|-------|----------------|-----------|
| `/babysit-prs` | Uma vez; auto-agenda via ScheduleWakeup | Loop a cada 5 min: detecta PRs com conflito → auto-rebase; reporta CI falhando |
| `/watch-ci` | Uma vez; auto-agenda | Loop a cada 30 min: CI vermelho no main → spawna agente de fix → abre PR de correção |
| `/daily-summary` | Uma vez; auto-agenda | Resume o dia: PRs mergeados, abertos, CI do main, commits recentes |
| `/factory-metrics` | Uma vez; auto-agenda | Coleta métricas semanais: tempo spec→PR, PRs/dia, taxa de correção autônoma, bloqueadores humanos |

### Skills de Auditoria e Utilitários

| Skill | Argumento | O que faz |
|-------|-----------|-----------|
| `/audit` | `padrão-de-busca` | Varre `src/main/java/` com grep e reporta todos os matches com contexto (arquivo, linha, trecho) |
| `/tasks` | _(sem argumento)_ | Exibe o estado atual das tasks em `.claude/tasks.json` agrupadas por planId |

### Skills de Setup de Projeto (uso único)

| Skill | Argumento | O que faz |
|-------|-----------|-----------|
| `/init-project` | `"descrição do projeto" [--figma <url>]` | Macro-skill: encadeia `/setup-architecture` + `/setup-design` + `/setup-infra` em sequência |
| `/setup-design` | `"dominio" [--figma <url>]` | Spawna `design-planner` → propõe design system completo → aguarda aprovação → gera `docs/design-system.md` |
| `/setup-architecture` | _(varia)_ | Inicializa estrutura arquitetural do projeto |
| `/setup-infra` | _(varia)_ | Inicializa infraestrutura (Docker, banco, CI) |

---

## Agentes (Subagents)

Os agentes são especialistas que vivem em `.claude/agents/<nome>.md`. Nunca são
invocados diretamente pelo operador — são spawanados por skills.

### Agentes Revisores

| Agente | Modelo | Quem spawna | O que faz |
|--------|--------|-------------|-----------|
| `pr-reviewer` | Haiku | `/ship`, `/review-pr`, `/plan` | Revisa PRs contra convenções do projeto: ADRs, validação destrutiva, cobertura de testes, convenções de commit. Produz: Bloqueadores / Sugestões / Elogios |
| `architect-reviewer` | Sonnet | `/ship`, `/review-arch`, `/plan` | Revisa decisões estruturais contra ADRs arquiteturais duros: Clean Architecture (ADR-004), JWT, Flyway, testes. Foco em camadas e dependências |
| `front-reviewer` | Haiku | `/ship`, `/review-front`, `/plan` | Revisa mudanças em `frontend/`: fetch fora de `services/`, `asChild` em base-nova, valores hardcoded de ambiente, `any` em tipos de API, ausência de testes. 5 bloqueadores, 4 sugestões |

### Agentes Geradores

| Agente | Modelo | Quem spawna | O que faz |
|--------|--------|-------------|-----------|
| `test-writer` | Sonnet | `/write-test` | Detecta o tipo de classe pelo path e gera o teste correto: unit (domain), Mockito (UseCase), Testcontainers (RepositoryImpl), MockMvc (Controller), Vitest (frontend). Valida rodando os testes antes de reportar |
| `migration-writer` | Sonnet | `/write-migration` | Lê `*Entity.java`, deriva colunas das anotações JPA (`@Column`, `@Id`, `@Embedded`, `@AttributeOverride`, `@Enumerated`), descobre próximo número Flyway, escreve `V<N>__cria_tabela_<nome>.sql` |
| `design-planner` | Sonnet | `/setup-design` | Propõe design system completo: paleta, tipografia, componentes, mapeamentos de tipo-de-dado por domínio |
| `report-writer` | Sonnet | `/write-report` | Gera componente `@react-pdf/renderer` com cabeçalho, tabela mapeada dos campos informados e rodapé. Lê tipos TypeScript do domínio, usa `formatBRL`/`formatDate`, exporta named export com `PDFDownloadLink` |
| `job-writer` | Sonnet | `/write-job` | Gera scaffold Spring Batch: ItemReader (CSV/stub), ItemProcessor (validação + null para inválidos), ItemWriter (saveAll), JobConfig (chunk configurável), JobListener (logs), JobLauncher REST ou Scheduler. Gera migration `BATCH_*` e adiciona dependência Maven se ausente |

### Agentes de Manutenção

| Agente | Modelo | Quem spawna | O que faz |
|--------|--------|-------------|-----------|
| `ci-fixer` | Sonnet | `/watch-ci` | Analisa log de CI vermelho, identifica causa raiz, aplica correção mínima. Máximo 2 tentativas. Suporta: compilação, unit, integration, build frontend |
| `conflict-resolver` | Sonnet | manual / futuro | Resolve conflitos de merge/rebase com raciocínio sobre intenção. Produz síntese correta sem marcadores de conflito |

---

## Hooks

### Arquitetura do Sistema de Hooks

```
git commit
     │
     ├─► commit-msg  (.githooks/commit-msg)
     │        └─► commit-msg.ps1
     │                 └─► .claude/hooks/universal/conventional-commits.ps1
     │
     ├─► pre-commit  (.githooks/pre-commit)
     │        └─► pre-commit.ps1  [orquestrador 1:N]
     │                 ├─► universal/encoding-utf8.ps1
     │                 ├─► universal/markdown-blank-lines.ps1
     │                 ├─► universal/docs-size.ps1
     │                 ├─► universal/secret-scanning.ps1
     │                 ├─► java-spring/maven-release.ps1
     │                 ├─► java-spring/entity-migration.ps1
     │                 ├─► java-spring/entity-migration-modified.ps1
     │                 ├─► java-spring/baseline-on-migrate.ps1
     │                 ├─► java-spring/lombok-mapstruct-order.ps1
     │                 ├─► java-spring/mvnw-profile.ps1
     │                 ├─► java-spring/mvnw-executable.ps1
     │                 ├─► java-spring/maven-central-versions.ps1
     │                 ├─► java-spring/test-conventions.ps1
     │                 ├─► windows/write-error-exit.ps1
     │                 ├─► windows/unix-commands.ps1
     │                 ├─► windows/lastexitcode-stop.ps1
     │                 └─► next/shadcn-artifacts.ps1
     │
     └─► post-merge  (.githooks/post-merge)
              └─► post-merge.ps1
                       └─► universal/npm-install-on-package-change.ps1
```

Adicionalmente, o hook nativo do Claude Code dispara fora do git:

```
Edit/Write em */domain/*.java
     └─► PostToolUse (.claude/hooks/post-edit/run-tests.ps1)
              └─► roda mvnw test -Dtest=<ClasseTest> se arquivo de teste existir
```

### Tabela de Hooks por Escopo

#### commit-msg (1 hook)

| Hook | Modo | O que valida |
|------|------|-------------|
| `conventional-commits.ps1` | **fail** | Formato Conventional Commits: tipo, scope, `!`, descrição mín. 10 chars. Tipos aceitos: feat, fix, chore, docs, test, refactor, style, perf, build, ci. Merge/revert commits isentos |

#### pre-commit — escopo universal (4 hooks)

| Hook | Modo | O que valida |
|------|------|-------------|
| `encoding-utf8.ps1` | **fail** | Encoding UTF-8 em `.java`, `.ts`, `.tsx`, `.js`, `.ps1`, `.md`, `.yml`, `.yaml`, `.json`. `.ps1` rejeita BOM; outros aceitam |
| `markdown-blank-lines.ps1` | **fail** | Linha em branco antes/depois de headers `##` a `######` em arquivos `.md`. Blocos de código ignorados. Nível 1 ignorado |
| `docs-size.ps1` | **warn** | Arquivos `.md` em `docs/` (exceto `docs/prompts/`) acima de 800 linhas. Alerta visual, não bloqueia |
| `secret-scanning.ps1` | **fail** | 6 padrões: chave PEM, AWS Key ID, GitHub token, OpenAI/Anthropic API key, `password` literal, `secret`/`apiKey` literal. Exclui `src/test/`, arquivos `*.example`, placeholders `${...}` e `$...` |

#### pre-commit — escopo java-spring (9 hooks)

| Hook | Modo | O que valida |
|------|------|-------------|
| `maven-release.ps1` | **fail** | `pom.xml` staged deve conter ao menos uma tag `<release>`. Só age se `pom.xml` está no diff |
| `entity-migration.ps1` | **fail** | `@Entity` nova (status A) em `src/main/java/` exige ao menos um `V<N>__*.sql` novo no mesmo commit |
| `entity-migration-modified.ps1` | **warn** | `@Entity` modificada (status M) com campo novo suspeito (linha `private`, `@Column`, `@Id`, `@Embedded` adicionada) → avisa sobre necessidade de `ALTER TABLE` |
| `baseline-on-migrate.ps1` | **fail** | `baseline-on-migrate: true` só permitido em `application-test.yml` e `application-dev.yml`; nunca em prod |
| `lombok-mapstruct-order.ps1` | **fail** | Lombok deve aparecer antes de `mapstruct-processor` em `<annotationProcessorPaths>` no `pom.xml` |
| `mvnw-profile.ps1` | **fail** | Scripts `.ps1` em `scripts/` não podem conter `mvnw spring-boot:run` sem `-Dspring-boot.run.profiles=` |
| `mvnw-executable.ps1` | **fail** | `mvnw` deve ter bit de execução `100755` no índice git |
| `maven-central-versions.ps1` | **warn** | Versões de plugins/dependências no `pom.xml` comparadas com Maven Central via API; alerta se desatualizadas. Falha de rede silenciosa |
| `test-conventions.ps1` | **fail** | Classes em `src/test/java/` devem terminar em `Test` ou começar com `Abstract`. Classes `Abstract*` em `*/shared/` devem ter modificador `abstract` |

#### pre-commit — escopo windows (3 hooks)

| Hook | Modo | O que valida |
|------|------|-------------|
| `unix-commands.ps1` | **warn** | Arquivos `.ps1` staged não devem usar `tail`, `head`, `grep`, `sed`, `awk` (não existem no PowerShell nativo) |
| `lastexitcode-stop.ps1` | **warn** | `.ps1` com `$ErrorActionPreference = "Stop"` + `$LASTEXITCODE` sem suspensão local `"Continue"` → padrão frágil |
| `write-error-exit.ps1` | **warn** | `Write-Error` seguido de `exit` em `.ps1` → usar `Write-Host -ForegroundColor Red + exit N` |

#### pre-commit — escopo next (1 hook)

| Hook | Modo | O que valida |
|------|------|-------------|
| `shadcn-artifacts.ps1` | **warn** | Detecta `frontend/src/components/ui/button.tsx` (artefato de scaffold shadcn) ou `AGENTS.md`/`CLAUDE.md` dentro de `frontend/` (artefatos de scaffold de framework) |

#### post-merge (1 hook)

| Hook | Modo | O que faz |
|------|------|-----------|
| `npm-install-on-package-change.ps1` | **warn** | Detecta `frontend/package.json` alterado no merge via `ORIG_HEAD` → roda `npm install` automaticamente. Evita `Module not found` após pull |

#### Claude Code PostToolUse (1 hook nativo)

| Hook | Modo | O que faz |
|------|------|-----------|
| `post-edit/run-tests.ps1` | non-blocking | Após `Edit` ou `Write` em `*/domain/*.java` → roda `mvnw test -Dtest=<Classe>Test` se arquivo de teste existir. Timeout 60s |

---

## Dependências Entre Peças

```
/plan
  └─► task-planner (Opus agent)      ← quebra objetivo em tasks
  └─► task-executor (general-purpose) ← por task, em worktree isolado
        ├─► lê CLAUDE.md
        │
        ├─► [se backend]
        │     ├─► /feature   → cria estrutura do bounded context (11 stubs Java)
        │     ├─► /migrate   → /write-migration (migration-writer) + /write-test (test-writer)
        │     ├─► implementa UseCases, Repository, Controller, DTOs
        │     └─► /write-test (test-writer) para cada camada
        │
        ├─► [se frontend]
        │     ├─► /feature-front → lê DTOs Java → gera 6 stubs tipados
        │     │     ├─► types/<dominio>.ts  (interfaces inferidas de *Response.java)
        │     │     ├─► services/<dominio>-service.ts  (métodos inferidos do Controller)
        │     │     ├─► index.ts  (barrel export)
        │     │     ├─► app/(dashboard)/<dominio>/page.tsx  (list stub)
        │     │     ├─► app/(dashboard)/<dominio>/novo/page.tsx  (form stub + Zod)
        │     │     └─► app/(dashboard)/<dominio>/[id]/page.tsx  (detail stub)
        │     ├─► executor preenche UI: tabela, FormFields, ações (guiado pelo prompt da task)
        │     │     └─► consulta docs/field-type-catalog.md para cada campo
        │     └─► /write-test → Vitest + Testing Library para cada arquivo gerado
        │
        ├─► ./mvnw verify (backend) / npm run build (frontend)
        └─► /ship
              ├─► check.ps1  → pre-commit hooks correm aqui via git commit
              ├─► check-front.ps1 (condicional)
              ├─► git push + gh pr create
              ├─► pr-reviewer (Haiku)
              ├─► architect-reviewer (Sonnet)
              └─► front-reviewer (Haiku, condicional se há frontend/)

/batch
  └─► lê docs/prompts/prompt-etapa-X-Y.md
  └─► agent (worktree isolado) por arquivo
        └─► executa o fluxo descrito no prompt de forma autônoma
              └─► geralmente termina com /ship (que inclui reviews)

/write-test
  └─► test-writer (Sonnet)
        ├─► detecta tipo pelo path:
        │     ├─► */domain/         → JUnit 5 + AssertJ (sem Spring)
        │     ├─► */application/*UseCase.java → Mockito
        │     ├─► */persistence/*RepositoryImpl → Testcontainers
        │     ├─► */interfaces/*Controller → MockMvc
        │     └─► frontend/**       → Vitest + Testing Library
        └─► valida rodando os testes antes de reportar

/migrate
  ├─► /write-migration → migration-writer (Sonnet)
  └─► /write-test      → test-writer (Sonnet)

/ship
  ├─► check.ps1
  ├─► check-front.ps1 (se frontend/ na branch)
  ├─► git push + gh pr create
  ├─► pr-reviewer        (sempre)
  ├─► architect-reviewer (sempre)
  ├─► front-reviewer     (se frontend/ na branch)
  └─► corrige apontamentos objetivos autonomamente → push + edita PR body
```

---

## Fluxo Completo do `/plan` — Do Objetivo ao PR

```
Operador digita: /plan "implemente tela de Orçamentos com CRUD completo"
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│  PASSO 0 — Preparar state                                   │
│  • Cria/verifica .claude/tasks.json                         │
│  • Gera planId = "plan-20260517-143000"                     │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│  PASSO 1 — Planejador (Opus)                                │
│  • Lê task-planner.md                                       │
│  • Spawna agente Opus com o objetivo                        │
│  • Opus analisa codebase, ADRs, progresso.md               │
│  • Retorna JSON com tasks + premissas globais               │
│                                                             │
│  Exemplo de retorno:                                        │
│  { tasks: [                                                 │
│      { id: "task-001", titulo: "Backend: bounded context    │
│        orcamento", tipo: "backend_only", ... },             │
│      { id: "task-002", titulo: "Frontend: tela Orçamentos", │
│        tipo: "frontend_only", ... }                         │
│  ], premissas_globais: ["...", "..."] }                     │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│  PASSO 2 — Registrar tasks em .claude/tasks.json            │
│  • Cada task: id, planId, titulo, resumo,                   │
│    status: "pending", branch: null, pr_url: null            │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│  PASSO 3 — Apresentar plano ao operador                     │
│                                                             │
│  /plan: 2 tasks planejadas para "..."                       │
│                                                             │
│  Premissas assumidas:                                       │
│    [P1] bounded context orcamento ainda não existe          │
│    [P2] frontend usa feature-first (ADR-013)                │
│                                                             │
│  [1] Backend: bounded context orcamento                     │
│      Complexidade: M | Risco: baixo | Migration: V22        │
│      Implementa domain, 5 use cases, infra, API REST        │
│                                                             │
│  [2] Frontend: tela Orçamentos                              │
│      Complexidade: M | Risco: baixo | Migration: nenhuma   │
│      CRUD completo com validação Zod espelhando Java        │
│                                                             │
│  AskUserQuestion: "O plano está correto?"                   │
│    → "Sim, spawnar os executores agora"   ──────────────┐   │
│    → "Quero discutir ou ajustar o plano"  (loop)        │   │
│    → "Cancelar"                           (encerra)     │   │
└─────────────────────────────────────────────────────────┼───┘
                                                          │
                           Operador aprova                │
                                                          ▼
┌─────────────────────────────────────────────────────────────┐
│  PASSO 4 — Spawnar executores em paralelo (ação atômica)    │
│                                                             │
│  Agent(task-001, isolation="worktree") ──┐                  │
│  Agent(task-002, isolation="worktree") ──┤ emitidos         │
│                                          │ ao mesmo tempo   │
│  Cada executor:                          │                  │
│    1. Verifica que não está em main      │                  │
│    2. Lê CLAUDE.md                       │                  │
│    3. Executa o prompt da task:          │                  │
│                                          │                  │
│    [task backend]                        │                  │
│       • git checkout -b feat/etapa-...   │                  │
│       • /feature <bounded-context>       │                  │
│       • Implementa domain + Entity       │                  │
│       • /migrate → SQL + unit tests      │                  │
│       • Implementa UseCases              │                  │
│       • Implementa infra + interfaces    │                  │
│       • /write-test para cada camada     │                  │
│       • ./mvnw verify (BUILD SUCCESS)    │                  │
│                                          │                  │
│    [task frontend]                       │                  │
│       • git checkout -b feat/etapa-...   │                  │
│       • /feature-front <dominio>         │                  │
│         → lê DTOs Java → gera 6 stubs   │                  │
│       • Preenche UI: tabela, FormFields, │                  │
│         ações (guiado pelo prompt)       │                  │
│       • /write-test para cada arquivo   │                  │
│       • npm run build (sem erros)        │                  │
│                                          │                  │
│       • /ship ───────────────────────┐  │                  │
│                                      │  │                  │
│  /ship internamente:                 │  │                  │
│    V1..V4 verificações de segurança  │  │                  │
│    → check.ps1 (gate completo)       │  │                  │
│    → check-front.ps1 (condicional)   │  │                  │
│    → git push -u origin <branch>     │  │                  │
│    → gh pr create                    │  │                  │
│    → pr-reviewer (Haiku)             │  │                  │
│    → architect-reviewer (Sonnet)     │  │                  │
│    → front-reviewer (condicional)    │  │                  │
│    → corrige apontamentos objetivos  │  │                  │
│    → push + edita PR body            │  │                  │
│    Reporta: Branch/PR/Reviews/Status ┘  │                  │
│                                         │                  │
│  Os dois executores rodam em paralelo ──┘                  │
│  (cada um em seu próprio worktree git isolado)             │
└─────────────────────────────────────────────────────────────┘
                                │
                    Ambos completam (minutos depois)
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│  PASSO 5 — Consolidar resultados                            │
│                                                             │
│  5.1 Parse de cada relatório:                               │
│      extrai Branch, PR URL, Status (OK/BLOQUEADOR)          │
│                                                             │
│  5.2 Atualiza .claude/tasks.json com status final           │
│                                                             │
│  5.3 Relatório preliminar:                                  │
│      [task-001] Backend orcamento: OK (PR: #182)           │
│      [task-002] Frontend Orçamentos: OK (PR: #183)         │
│      PRs abertos: 2/2  |  Bloqueadores: nenhum             │
│                                                             │
│  5.4 Reviews automáticos por PR (sequencial):               │
│      task-001 (backend_only):                               │
│        → pr-reviewer:        OK                             │
│        → (front-reviewer:    N/A — backend-only)           │
│      task-002 (frontend_only):                              │
│        → pr-reviewer:        OK                             │
│        → front-reviewer:     OK                             │
│                                                             │
│  Relatório final consolidado com seção Reviews              │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│  PASSO 6 — Cleanup                                          │
│                                                             │
│  6.1 Remove worktrees com path contendo "agent-"            │
│      (usa git worktree remove -f -f para worktrees locked)  │
│                                                             │
│  6.2 Remove branches com upstream "gone" (já mergeadas)     │
│      (git fetch --prune + git branch -d)                    │
│                                                             │
│  6.3 Remove branches locais-only sem tracking               │
│      (prefixo review-* ou worktree-*, usa git branch -D)    │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
         Operador recebe 2 PRs prontos para merge no GitHub
         Cada PR já foi revisado por pr-reviewer + architect-reviewer
         (+ front-reviewer se havia mudanças de frontend)
```

---

## O que Acontece em Cada `git commit` (Hooks Ativos)

Quando o executor (ou o operador) roda `git commit`, os hooks disparam automaticamente:

```
git commit -m "feat(orcamento): implementa domain e migration"
     │
     ├─► commit-msg:
     │     ✓ "feat(orcamento): implementa domain e migration"
     │       tipo=feat, scope=orcamento, descrição=30 chars → OK
     │
     └─► pre-commit (todos rodam, mesmo se um falhar):
           ✓ encoding-utf8: arquivos Java e SQL em UTF-8
           ✓ markdown-blank-lines: nenhum .md staged
           ✓ docs-size: progresso.md tem 450 linhas (< 800) → warn silencioso
           ✓ secret-scanning: nenhum padrão suspeito
           ✓ maven-release: pom.xml tem <release>21</release>
           ✓ entity-migration: OrcamentoEntity.java (status A) + V22__*.sql staged → OK
           ✓ entity-migration-modified: nenhuma Entity modificada
           ✓ baseline-on-migrate: application-test.yml tem baseline=true → OK (test profile)
           ✓ lombok-mapstruct-order: lombok antes de mapstruct no pom.xml
           ✓ mvnw-profile: scripts/dev.ps1 tem -Dspring-boot.run.profiles=dev
           ✓ mvnw-executable: mvnw com mode 100755
           ✓ maven-central-versions: Spring Boot 3.x é a versão atual → OK
           ✓ test-conventions: OrcamentoTest.java termina em "Test"
           ✓ write-error-exit: nenhum Write-Error problemático
           ✓ unix-commands: nenhum grep/sed em .ps1
           ✓ lastexitcode-stop: $ErrorActionPreference tratado corretamente
           ✓ shadcn-artifacts: nenhum artefato de scaffold
     │
     COMMIT CRIADO ✓
```

---

## Convenções de Implementação Frontend

O executor usa `/feature-front <dominio>` para gerar o scaffold inicial — análogo ao
`/feature` Java. A skill lê os DTOs Java e gera 6 arquivos tipados; o executor preenche
a UI guiado pelo prompt da task (gerado pelo planejador). As convenções abaixo governam
tanto a skill quanto o executor ao preencher os detalhes:

### 1. Estrutura de arquivos (ADR-013 feature-first)

```
frontend/src/features/<dominio>/
  services/    ← único ponto de fetch (via api-client.ts)
  types/       ← tipos TypeScript espelhando o backend
  hooks/       ← React hooks do domínio
  components/  ← componentes React do domínio
  index.ts     ← barrel export
```

Código compartilhado entre domínios vai em `frontend/src/shared/`.

### 2. Catálogo de campos: docs/field-type-catalog.md

Referência obrigatória antes de implementar qualquer campo. Violação = bloqueador **B7**
no `front-reviewer`. Mapeamento principal:

| Tipo Java | Input no formulário | Exibição |
|-----------|--------------------|----|
| `BigDecimal` monetário | `MoneyInput` (shared) | `formatBRL(value)` |
| `LocalDate` | `<Input type="date">` | `formatDate(value)` |
| `LocalDate` mês/ano | `<Input type="month">` + concatena `-01` ao enviar | `MM/YYYY` |
| `Instant` / `LocalDateTime` | não editável | `formatDate(value)` |
| `boolean` | `<Switch>` | Badge colorido |
| UUID FK | `<Select>` carregado do endpoint | Nome da entidade |
| Enum Java | `<Select>` com mapa de labels + render function | Label do mapa |
| Status calculado | `StatusBadge` com cor semântica | — |
| `@Size(max=N)` | `<Input maxLength={N}>` | — |

**Atenção especial ao `Select.Value`:** `@base-ui/react` não espelha automaticamente
o texto do item selecionado. Obrigatório usar render function como `children`:

```tsx
<SelectValue placeholder="Selecione">
  {(v: string | null) => OPTIONS.find(o => o.value === v)?.label ?? 'Selecione'}
</SelectValue>
```

### 3. Validação espelhada Zod/Java (bloqueador B6)

Cada anotação Java no `*Request.java` deve ter equivalente no schema Zod:
- `@NotBlank` → `.min(1)`
- `@Size(max=N)` → `.max(N)`
- `@Min(N)` → `.min(N)`
- `@NotNull` → campo obrigatório no Zod (sem `.optional()`)

### 4. Convenções de fetch

`fetch` direto é **proibido** fora de `src/services/api-client.ts` (bloqueador B1).
Cada domínio cria seu próprio service em `features/<dominio>/services/` que chama `apiFetch`.

---

## Modos dos Hooks: fail vs warn

| Modo | Comportamento | Quando usar |
|------|---------------|-------------|
| **fail** | Bloqueia o commit (exit 1). O commit não é criado | Regras objetivas: violação = bug garantido em produção |
| **warn** | Imprime aviso colorido, commit prossegue (exit 0) | Regras subjetivas: falso positivo é possível; decisão humana |

Override disponível: `git commit --no-verify` (documentar motivo no PR body).

---

## Gates de Qualidade (Pré-Push)

O `/ship` roda dois gates antes do push. Ambos devem passar com exit 0:

| Gate | Script | O que valida |
|------|--------|-------------|
| Backend | `scripts/check.ps1` | `./mvnw verify` completo: compilação + todos os testes (unit + integration) + checkstyle. Exige Docker ativo (Testcontainers) |
| Frontend | `scripts/check-front.ps1` | `npm run lint` + `npm run test:run` + `npm run build`. Só roda se há arquivos `frontend/` na branch |

---

## Localizações dos Arquivos

```
.githooks/
  commit-msg          ← entrypoint bash → commit-msg.ps1
  commit-msg.ps1      ← delega para conventional-commits.ps1
  pre-commit          ← entrypoint bash → pre-commit.ps1
  pre-commit.ps1      ← orquestrador 1:N (array $hooks)
  post-merge          ← entrypoint bash → post-merge.ps1
  post-merge.ps1      ← delega para npm-install-on-package-change.ps1

.claude/
  hooks/
    universal/        ← encoding-utf8, markdown-blank-lines, docs-size,
    │                    secret-scanning, conventional-commits,
    │                    npm-install-on-package-change
    java-spring/      ← maven-release, entity-migration, entity-migration-modified,
    │                    baseline-on-migrate, lombok-mapstruct-order, mvnw-profile,
    │                    mvnw-executable, maven-central-versions, test-conventions
    windows/          ← unix-commands, lastexitcode-stop, write-error-exit
    next/             ← shadcn-artifacts
    post-edit/        ← run-tests.ps1 (hook nativo Claude Code PostToolUse)

  agents/
    pr-reviewer.md          ← Haiku, revisor de PR
    architect-reviewer.md   ← Sonnet, revisor arquitetural
    front-reviewer.md       ← Haiku, revisor de frontend
    test-writer.md          ← Sonnet, gerador de testes
    migration-writer.md     ← Sonnet, gerador de migration SQL
    ci-fixer.md             ← Sonnet, corretor de CI vermelho
    conflict-resolver.md    ← Sonnet, resolvedor de conflitos
    design-planner.md       ← Sonnet, planejador de design system
    report-writer.md        ← Sonnet, gerador de PDFs com @react-pdf/renderer
    job-writer.md           ← Sonnet, gerador de scaffold Spring Batch

  skills/
    plan/SKILL.md           ← /plan
    batch/SKILL.md          ← /batch
    ship/SKILL.md           ← /ship
    feature/SKILL.md        ← /feature (Java: 11 stubs)
    feature-front/SKILL.md  ← /feature-front (frontend: 6 stubs a partir de DTOs Java)
    migrate/SKILL.md        ← /migrate
    write-test/SKILL.md     ← /write-test
    write-migration/SKILL.md ← /write-migration
    write-report/SKILL.md   ← /write-report (relatório PDF)
    write-job/SKILL.md      ← /write-job (scaffold Spring Batch)
    review-pr/SKILL.md      ← /review-pr
    review-arch/SKILL.md    ← /review-arch
    review-front/SKILL.md   ← /review-front
    audit/SKILL.md          ← /audit
    tasks/SKILL.md          ← /tasks
    babysit-prs/SKILL.md    ← /babysit-prs
    watch-ci/SKILL.md       ← /watch-ci
    daily-summary/SKILL.md  ← /daily-summary
    factory-metrics/SKILL.md ← /factory-metrics
    setup-design/SKILL.md   ← /setup-design
    init-project/SKILL.md   ← /init-project

  tasks.json          ← estado das tasks do /plan (planId, status, branch, pr_url)
  settings.json       ← hooks nativos Claude Code (gitignored, gerado pelo setup.ps1)

scripts/
  check.ps1           ← gate backend (mvnw verify)
  check-front.ps1     ← gate frontend (lint + test + build)
  setup.ps1           ← configura core.hooksPath + gera settings.json

docs/
  adrs.md             ← ADRs formais do projeto
  decisoes.md         ← decisões fundacionais (stack, arquitetura)
  decisoes-claude-code.md ← decisões operacionais da Camada 3+
  progresso.md        ← estado atual por Camada e sub-etapa
  hooks-pendentes.md  ← backlog e histórico de hooks
  field-type-catalog.md ← mapeamento de tipo-backend para componente frontend
  prompts/            ← prompts de sub-etapas (gitignored, efêmeros)
```
