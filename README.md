# financas-lab

Projeto-laboratório para validar uma fábrica de desenvolvimento AI-native, usando um SaaS de finanças pessoais como caso de uso. O sucesso é medido pelo amadurecimento da fábrica, não pelo produto em si.

## Documentação

- `docs/visao.md` — propósito do projeto, escopo do MVP e critérios de sucesso
- `docs/decisoes.md` — stack, arquitetura, convenções e padrões (foto atual)
- `docs/adrs.md` — decisões arquiteturais com contexto e alternativas consideradas
- `docs/progresso.md` — estado atual da fábrica e próximas camadas
- `docs/roadmap-camada-1.md` — plano detalhado da Camada 1 (infraestrutura de confiança)

## Comandos do projeto

Os scripts em `scripts/` encapsulam os comandos atômicos do projeto.

| Comando | Função |
|---|---|
| `scripts\setup.ps1` | Sobe Docker Compose + baixa deps + compila (sem testes). Use ao clonar o repo ou após reset. |
| `scripts\dev.ps1` | Sobe Docker Compose + roda Spring Boot em modo dev. Bloqueia o terminal. |
| `scripts\test.ps1` | Ciclo rápido: `mvnw test`. Sem JaCoCo check, sem análise estática. |
| `scripts\test-integration.ps1` | Testes + JaCoCo, sem Checkstyle/SpotBugs. Útil pra debugar testes. |
| `scripts\check.ps1` | Gate completo. Equivalente ao CI. |
| `scripts\ship.ps1` | Roda `check.ps1` + `git push`. Não cria PR automaticamente. |

### Pré-requisito Windows

PowerShell por default não permite executar scripts não-assinados. Configurar uma vez:

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

### Pré-requisitos do ambiente

- Java 21 (Temurin recomendado)
- Maven (já incluso via Maven Wrapper — `.\mvnw`)
- Docker Desktop rodando
- (opcional) GitHub CLI (`gh`) para fluxo de PRs

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

## Status

**Camada 1 (Infraestrutura de confiança) — ✅ Concluída** (2026-05-08).

Próxima: Camada 2 (Arquitetura otimizada para agentes).

Documentos relevantes:
- `docs/progresso.md` — estado atual da fábrica
- `docs/retrospectiva-camada-1.md` — reflexão sobre o que aprendemos
- `docs/hooks-pendentes.md` — candidatos a hook para Camada 3
