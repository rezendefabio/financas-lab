# financas-lab

Laboratório de desenvolvimento **AI-native**: construção de uma fábrica de software replicável usando um SaaS de finanças pessoais como caso de uso. O sucesso é medido pelo amadurecimento da fábrica, não pelo produto em si.

> **Para a entrevista:** este repositório documenta não apenas o que foi construído, mas *como* foi construído — uma metodologia completa de desenvolvimento com agentes de IA operando em ciclos autônomos. Os ~230 PRs merged representam iterações curtas e verificáveis, cada uma com gate automatizado (lint + testes + build) antes do merge.

---

## O que é a fábrica AI-native

O projeto valida que agentes de IA (Claude Code) podem ser configurados para entregar features end-to-end de forma autônoma e verificável, com intervenção humana apenas em decisões arquiteturais e aprovação de PRs.

### Camadas construídas

| Camada | Descrição | Status |
|--------|-----------|--------|
| **0 — Discovery** | Visão, ADRs, decisões de stack, ambiente | ✅ Concluída |
| **1 — Infraestrutura de confiança** | CI/CD, Maven Wrapper, Docker Compose, Checkstyle, SpotBugs, JaCoCo, cobertura ≥80% | ✅ Concluída |
| **2 — Arquitetura otimizada para agentes** | Clean Architecture, bounded contexts, JWT, Flyway, padrão de repositório, testes em 3 níveis | ✅ Concluída |
| **3 — Configuração do Claude Code** | CLAUDE.md rico, subagents, skills (slash commands), hooks git + hooks nativos | ✅ Concluída |
| **4 — Modelo operacional** | Fábrica ativa: Tier 2 (features completas), `/batch` (paralelismo), `/plan` (planejamento autônomo) | 🟢 Em andamento |

### Tooling da fábrica (construído neste repo)

**Subagents especializados** (`.claude/agents/`):
- `pr-reviewer` — revisa PRs contra ADRs e convenções do projeto
- `architect-reviewer` — valida Clean Architecture, JWT, Flyway, cobertura de testes
- `test-writer` — gera unit tests (JUnit 5), integration tests (Testcontainers) e E2E tests (MockMvc) para Java; Vitest + Testing Library para frontend
- `migration-writer` — lê `*Entity.java`, deriva colunas de anotações JPA, gera migration Flyway
- `report-writer` — gera componentes de relatório PDF com `@react-pdf/renderer`
- `job-writer` — gera scaffold de jobs Spring Batch (chunk-oriented)
- `design-planner` — propõe design system (paleta, tipografia, componentes)
- `front-reviewer` — revisa PRs de frontend contra 12 regras objetivas

**Skills (slash commands)** (`.claude/skills/`):
- `/feature <nome>` — cria estrutura completa de bounded context (11 arquivos stub)
- `/migrate <nome>` — gera migration SQL + testes de integração
- `/write-test <arquivo>` — gera testes para qualquer camada
- `/write-report <spec>` — gera relatório PDF
- `/write-job <spec>` — gera job Spring Batch
- `/ship` — gate completo (lint + testes + build) + push + PR + reviews automáticos
- `/batch <prompt1> <prompt2>` — executa múltiplas tasks em paralelo via worktrees isolados
- `/plan <objetivo>` — planejador autônomo que decompõe objetivos em tasks e as executa em paralelo
- `/audit <padrão>` — varre o código buscando padrão ou violação
- `/register-screen <spec>` — registra tela no Screen Registry com validação

**Hooks git** (`.githooks/` + `.claude/hooks/`):
- Conventional Commits (modo fail)
- Encoding UTF-8 (modo fail)
- Markdown blank lines (modo fail)
- Docs size >800 linhas (modo warn)
- Maven `<release>` no pom.xml (modo fail)
- `@Entity` sem migration Flyway (modo fail)
- Post-edit: roda testes unitários do domínio após edição (hook nativo Claude Code)

---

## O produto: SaaS de finanças pessoais

### Stack

**Backend:**
- Java 21 + Spring Boot 3.x
- PostgreSQL (Docker)
- Flyway (migrations versionadas)
- JPA + Hibernate + MapStruct + Lombok
- Spring Security (JWT)
- Spring Batch (importação CSV)
- Spring Application Events (comunicação cross-context)
- MinIO (armazenamento de arquivos, API S3-compatível)
- Testcontainers (testes de integração isolados)
- JaCoCo (cobertura ≥80%), Checkstyle, SpotBugs

**Frontend:**
- Next.js 16 (App Router) + TypeScript
- Tailwind CSS + shadcn/ui (design system base-nova)
- TanStack Query (server state), Zustand (client state), React Hook Form + Zod
- Vitest + Testing Library
- PWA (instalável, offline fallback)
- `@react-pdf/renderer` (relatórios PDF)
- cmdk (Command Palette)

### Bounded contexts implementados

| Context | Operações | Destaque |
|---------|-----------|----------|
| `conta` | CRUD + saldo derivado | Tipos: CORRENTE, POUPANÇA, DINHEIRO, CARTÃO |
| `categoria` | CRUD + hierarquia | Categorias pai/filho, filtro por RECEITA/DESPESA |
| `transacao` | CRUD + filtros avançados + importação CSV | Paginação backend, filtros por tipo/status/conta/data/valor |
| `orcamento` | CRUD + progresso | Calcula % gasto vs limite; alertas via domain events |
| `meta` | CRUD + depósitos | Tracking de valor acumulado vs alvo |
| `lancamento_recorrente` | CRUD | Agendamento por periodicidade |
| `payee` | CRUD | Beneficiários/fornecedores |
| `tag` | CRUD | Classificação livre de transações |
| `anotacao` | CRUD | Notas livres por entidade |
| `auditlog` | Leitura | Trilha imutável de eventos create/update/delete |
| `anexo` | Upload/download | Arquivos por entidade via MinIO |
| `incidente` | Registro + busca | Erros não tratados com código ERR-XXXXXXXX |

### Arquitetura (Clean Architecture + DDD)

```
src/main/java/.../
├── <context>/
│   ├── domain/          # Entidade, Repository (interface), Exceptions
│   ├── application/     # UseCases (orquestram domain, sem infra)
│   ├── infrastructure/
│   │   └── persistence/ # Entity JPA, JpaRepository, Mapper, RepositoryImpl
│   └── interfaces/
│       ├── *Controller.java
│       └── dto/
└── shared/
    ├── domain/          # Money, tipos compartilhados
    └── infrastructure/  # SecurityConfig, AsyncConfig, etc.
```

Regra estrita: `Controller → UseCase → Repository (interface)`. Controllers nunca acessam repositórios diretamente.

### Frontend: Shell declarativo

O frontend segue uma arquitetura de shell declarativo com:

- **Screen Registry** — todas as telas registradas em `screens.registry.ts` com código único (ex: `FIN-TRX-001`)
- **Tab Manager** — abas abertas por tela, persistidas em Zustand
- **Command Palette** (Ctrl+K) — navegação por todas as telas via busca
- **Sidebar hierárquica** — menu colapsável gerado do Screen Registry
- **DataTable + FilterBar** — listagem com ordenação, filtros e exportação CSV em todas as telas
- **ActionsPanel** — histórico de auditoria + exportação CSV por entidade selecionada
- **Audit Log Drawer** — timeline de eventos por entidade com diff antes/depois

### Números

| Métrica | Valor |
|---------|-------|
| Testes backend | ~930 |
| Testes frontend | ~500 |
| Migrations Flyway | 25 |
| PRs merged | ~230 |
| Bounded contexts | 12 |
| Telas (rotas) | 26 |

---

## Começando

### Pré-requisitos

- Java 21 (Temurin recomendado)
- Docker Desktop
- Node.js 18+
- PowerShell 5.1 (Windows nativo — não `pwsh`)
- GitHub CLI (`gh`) para o fluxo de PRs

### Setup

```powershell
# Configurar permissão de execução (uma vez)
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned

# Setup completo (Docker + deps + compile)
.\scripts\setup.ps1

# Subir backend em modo dev
.\scripts\dev.ps1

# Subir frontend em modo dev (terminal separado)
.\scripts\dev-front.ps1
```

Acesse: `http://localhost:3000`

### Comandos do dia

| Comando | Função |
|---------|--------|
| `.\scripts\check.ps1` | Gate completo: `mvn verify` + encoding (equivalente ao CI) |
| `.\scripts\check-front.ps1` | Gate frontend: lint + testes + build |
| `.\scripts\dev.ps1` | Backend em modo dev (Docker Compose + Spring Boot) |
| `.\scripts\dev-front.ps1` | Frontend em modo dev (Next.js) |
| `.\scripts\test.ps1` | Ciclo rápido: só `mvnw test` |

### Frontend (a partir de `frontend/`)

```bash
npm install
npm run dev      # dev server
npm run build    # build produção
npm run test:run # testes (Vitest)
npm run lint     # ESLint
```

---

## Documentação

| Arquivo | Conteúdo |
|---------|----------|
| `docs/visao.md` | Propósito do projeto, escopo do MVP, critérios de sucesso |
| `docs/progresso.md` | Estado atual da fábrica, sub-etapas concluídas |
| `docs/decisoes.md` | Stack, arquitetura e convenções (decisões fundacionais) |
| `docs/decisoes-claude-code.md` | Decisões operacionais do Claude Code (Camada 3+) |
| `docs/adrs.md` | Architecture Decision Records com contexto e alternativas |
| `docs/hooks-pendentes.md` | Backlog de hooks + lista dos implementados |
| `docs/fabrica-referencia.md` | Referência completa da fábrica: agentes, skills, rotinas |
| `CLAUDE.md` | Instruções para o Claude Code (stack, convenções, restrições) |
