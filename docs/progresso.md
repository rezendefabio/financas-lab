# Progresso — Construção da Fábrica AI-Native

> Documento de tracking. Mostra **onde estamos** na construção da fábrica e do produto.
> Atualizado conforme camadas avançam. Diferente do `decisoes.md` (que registra escolhas) e dos `adrs.md` (que registram porquês), este documento responde a pergunta: "em que ponto eu estou?".

**Última atualização:** 2026-05-06

---

## Status geral

| Camada | Descrição | Status |
|---|---|---|
| **0** | Discovery (visão, ADRs, decisões, ambiente) | ✅ Concluída |
| **1** | Infraestrutura de confiança | 🔵 Próxima |
| **2** | Arquitetura otimizada para agentes | ⏸️ Aguardando |
| **3** | Configuração do Claude Code (subagents, skills, hooks) | ⏸️ Aguardando |
| **4** | Modelo operacional (tiers de autonomia ativados) | ⏸️ Aguardando |
| **5** | Runtime de agentes (VPS) — opcional | ⏸️ Aguardando |
| **6** | Gestão híbrida Max + API | 🟡 Parcial (configuração API pronta, sem uso) |

**Legenda:** ✅ Concluída | 🟢 Em andamento | 🔵 Próxima | ⏸️ Aguardando | 🟡 Parcial

---

## Camada 0 — Discovery

**Status:** ✅ Concluída em 2026-05-06

### Critérios de "pronto" (todos atendidos)

- [x] Documento de visão escrito e estável (`docs/visao.md`)
- [x] ADRs fundadores escritos (`docs/adrs.md` — 8 ADRs)
- [x] `decisoes.md` consolidando stack, padrões e convenções
- [x] Repositório criado no GitHub privado (`financas-lab`)
- [x] Clone local em `C:\projetos\financas-lab`
- [x] Pré-requisitos do ambiente validados (Java 21, Maven 3.9, Docker 29, Node 22, Git)
- [x] Ambiente de desenvolvimento decidido (Windows nativo)
- [x] `settings.json` do Claude Code ajustado (Sonnet default, effort medium, modelos atuais)
- [x] Conta API configurada com hard limit $30/mês
- [x] Auditoria de plugins/subagents existentes (limpo, só `frontend-design` e `code-review` oficiais)

### O que foi decidido nesta camada

Resumo executivo (detalhes em `adrs.md`):

- **Backend:** Java 21 + Spring Boot 3 + Maven (ADR-001)
- **Frontend:** Next.js 15 + TypeScript + Tailwind + PWA (ADR-002)
- **Banco:** PostgreSQL 16 em dev e prod, sem H2/SQLite (ADR-003)
- **Arquitetura:** Clean Architecture enxuta com porta para DDD tático on-demand (ADR-004)
- **Auth:** JWT stateless com refresh rotativo (ADR-005)
- **Migrations:** Flyway com SQL puro versionado (ADR-006)
- **Testes:** Três níveis com Testcontainers (ADR-007)
- **Modelo financeiro:** Max 5x até evidência de insuficiência (ADR-008)
- **Ambiente:** Windows nativo + PowerShell + Docker Desktop

### Lições da Camada 0 (anotações para refinar a fábrica)

- Auditoria de configuração existente do Claude Code é etapa não-óbvia mas crítica. Subagents/CLAUDE.md global herdados podem conflitar com decisões do projeto. Vale incluir essa auditoria no playbook quando ele for extraído.
- Decidir ambiente (Windows nativo vs WSL2) **antes** de escrever scripts evita refactor de dezenas de arquivos. Elevar isso a ADR formal num projeto futuro.
- Modelo default Opus + effort high é armadilha financeira fácil de cair. Vale entrar em qualquer playbook de partida como "verificar antes de começar".

---

## Camada 1 — Infraestrutura de confiança

**Status:** 🔵 Próxima
**Estimativa:** 2 semanas (semanas 1-2 do projeto)

### Objetivo

Construir a fundação não-negociável da fábrica: testes em três níveis, CI confiável, hooks locais, banco rodando, projeto Spring Boot inicializado, projeto Next.js inicializado. **Zero código de feature nesta camada** — só infraestrutura de validação.

### Critérios de "pronto"

- [x] Repo configurado com `.gitattributes`, `.gitignore`, README inicial
- [x] CLAUDE.md mínimo do projeto criado (apontando para docs)
- [ ] Estrutura de pastas inicial criada
- [x] `docker-compose.yml` rodando Postgres 16 + Redis 7
- [ ] Scripts PowerShell criados: `setup.ps1`, `dev.ps1`, `test.ps1`, `check.ps1`, `ship.ps1`
- [x] Projeto Spring Boot inicializado via Spring Initializr (manualmente)
- [x] `pom.xml` com todas as dependências da stack
- [ ] Flyway configurado, primeira migration criada (schema vazio + tabela de versão)
- [ ] Testcontainers configurado e funcional
- [ ] Hello-world endpoint passando teste e2e via Testcontainers
- [x] JaCoCo configurado (sem thresholds — apenas prepare-agent + report; thresholds por camada entram na Etapa 2.4)
- [ ] Checkstyle + SpotBugs configurados
- [ ] Projeto Next.js inicializado
- [ ] GitHub Actions configurado: lint + test + build em PR
- [ ] CI verde no primeiro commit em `main`
- [ ] Pre-commit hook local rodando lint + format
- [x] Branch protection em `main` (sem push direto, exige PR e CI verde)

### Roadmap detalhado

Ver `docs/roadmap-camada-1.md` para o passo a passo das 2 semanas.

---

## Camada 2 — Arquitetura otimizada para agentes

**Status:** ⏸️ Aguardando
**Pré-requisito:** Camada 1 concluída

### Objetivo

Implementar a estrutura de bounded contexts, primeiros agregados/use cases, value objects compartilhados (`Money`), padrões de mapping (MapStruct) e validação. Ainda sem features completas — só o "esqueleto rico" sobre o qual features serão delegadas no Tier 2.

### Critérios de "pronto" (preliminar)

- [ ] Estrutura de pacotes implementada conforme ADR-004
- [ ] Value object `Money` implementado e testado
- [ ] Bounded context `conta` com domínio puro + use cases + repositório
- [ ] Bounded context `categoria` no mesmo padrão
- [ ] MapStruct funcionando entre Entity JPA ↔ Domain
- [ ] Bean Validation aplicada em DTOs de Request
- [ ] Spring Security configurado com JWT + refresh rotativo
- [ ] Endpoints de auth funcionando (signup, login, refresh, logout)
- [ ] Cobertura JaCoCo nos thresholds definidos
- [ ] OpenAPI gerada automaticamente

(Detalhes serão expandidos quando a Camada 1 estiver concluída.)

---

## Camada 3 — Configuração do Claude Code

**Status:** ⏸️ Aguardando
**Pré-requisito:** Camada 2 com pelo menos um bounded context completo

### Objetivo

Configurar `CLAUDE.md` rico, criar 3-5 subagents focados, criar 5-10 skills (slash commands) para workflows repetidos, configurar hooks que substituem revisão manual.

### Critérios de "pronto" (preliminar)

- [ ] `CLAUDE.md` do projeto escrito (target ≤15KB)
- [ ] Subagent `architect-reviewer` (valida decisões contra ADRs)
- [ ] Subagent `pr-reviewer` (revisão crítica antes do PR)
- [ ] Subagent `test-writer` (gera testes seguindo padrões do projeto)
- [ ] Subagent (opcional) `migration-writer` (gera Flyway migration baseada em diff JPA)
- [ ] Skill `/feature <nome>` (cria estrutura de bounded context)
- [ ] Skill `/ship` (lint + test + build + push + PR)
- [ ] Skill `/migrate` (gera migration + atualiza schema + escreve teste)
- [ ] Skill `/audit` (varre módulos buscando padrão específico)
- [ ] Hook pre-commit funcionando
- [ ] Hook post-edit rodando testes do arquivo mexido
- [ ] Decisão sobre plugin `code-review` oficial: manter, desativar ou reaproveitar?

---

## Camada 4 — Modelo operacional

**Status:** ⏸️ Aguardando
**Pré-requisito:** Camada 3 funcional

### Objetivo

Ativar a fábrica de fato: rodar features no Tier 2, configurar 3 routines Tier 1, validar paralelismo se necessário.

### Critérios de "pronto" (preliminar)

- [ ] Pelo menos 3 features completas implementadas em fluxo Tier 2
- [ ] 3 routines Tier 1 rodando (CI watcher, dependency updater, daily summary ou equivalentes)
- [ ] Pelo menos 1 sessão `/batch` ou paralela executada com sucesso
- [ ] Documentação interna de fluxo do Tier 2 (como abrir, revisar, rejeitar PR de agente)
- [ ] Métrica capturada: tempo entre "spec pronta" e "PR aberto"

---

## Camada 5 — Runtime de agentes (VPS)

**Status:** ⏸️ Aguardando
**Pré-requisito:** Camada 4 funcional + necessidade comprovada

### Objetivo

Mover routines persistentes e batches paralelos pesados para VPS dedicada. Só ativada quando rodar local começar a doer.

(Detalhes em `blueprint-fabrica-ai-native.md` Camada 5.)

---

## Camada 6 — Gestão híbrida Max + API

**Status:** 🟡 Parcial

### Pronto

- [x] Conta Anthropic API configurada com hard limit $30/mês
- [x] API key gerada e guardada em gerenciador de senhas
- [x] Decisão de modelo financeiro registrada em ADR-008

### Pendente

- [ ] Primeira routine usando API direta (decisão fim do mês 2)
- [ ] Avaliar se overflow API justifica budget acima de $50/mês (fim do mês 4)
- [ ] Avaliar Max 20x (fim do mês 6)

---

## Métricas a capturar (a partir da Camada 4)

Para validar a fábrica objetivamente, rastrear:

- **Tempo médio entre "spec de feature pronta" e "PR aberto"**
- **% de PRs aprovados sem segunda revisão manual** (CI verde = mergeable)
- **Tokens consumidos por feature** (Max + API agregados)
- **Quantidade de routines Tier 1 ativas e seu retorno** (qualitativo)
- **Frequência de bater limite do Max 5x** (proxy pra decisão Max 20x)

Definir como capturar quando chegarmos na Camada 4 — não criar burocracia agora.

---

## Lições da Etapa 1.1

### Candidatos a hook (automatizar em etapas futuras)

1. **Linhas em branco em Markdown** — validar que arquivos `.md` modificados têm linhas em branco antes e depois de headers (`##`, `###`). Sem isso, alguns renderers não reconhecem o header.
2. **Encoding UTF-8 em arquivos de texto** — validar que arquivos criados pela fábrica estão em UTF-8.
3. **Conventional Commits** — validar que mensagens de commit seguem o padrão (`feat:`, `fix:`, `chore:`, etc.).
4. **Tamanho de documentos em `docs/`** — alertar se algum `.md` em `docs/` ultrapassa um limite definido (anti-enciclopédia, segundo o princípio "CLAUDE.md curto > CLAUDE.md enciclopédia").

### Lições de ambiente

1. **Tools `Read`/`Write` do Claude Code truncam output do CLI** com marcador "+N lines (ctrl+o to expand)". Sempre validar conteúdo em disco antes de aceitar arquivo criado, não confiar no preview.
2. **PowerShell padrão sem `-Encoding UTF8` lê UTF-8 errado** — mostra `Ã³` no lugar de `ó`, `Ã§` no lugar de `ç`. Para validação confiável de arquivos com acentos, usar `Get-Content -Encoding UTF8` explícito.
3. **`Measure-Object -Line` não conta linhas em branco** — o cmdlet conta apenas linhas com conteúdo. Para contagem real (incluindo vazias), usar `[System.IO.File]::ReadAllLines('<path>').Count`.
4. **Premissas do orquestrador externo podem estar erradas** — validação independente com cálculo concreto resolve. O Claude Code acertou em pushback técnico contradizendo análise visual feita no chat externo. Reforça o princípio: dado concreto vence interpretação.

---

## Lições da Etapa 1.4

### Candidatos a hook (automatizar em etapas futuras)

1. Validar que pesquisa de versões em pom.xml consultou Maven Central / BOM Spring antes de fixar (não memória do agente). Versões podem estar desatualizadas no conhecimento do agente.
2. Validar que `<release>` está sendo usado em vez de `<source>` + `<target>` no maven-compiler-plugin (idiomático desde Java 9).
3. Validar ordem "Lombok antes de MapStruct" em `<annotationProcessorPaths>` do maven-compiler-plugin.
4. Validar que agente NÃO sugere "próxima etapa" espontaneamente após abrir PR. Cada etapa tem fim explícito (PR mergeado + progresso.md atualizado + sync local) antes da próxima.

### Lições de ambiente

1. `python3` não existe no PATH do Windows nativo — o binário se chama `python`. Lição reapareceu durante a sessão (primeira vez no início, segunda vez no curl /v3/api-docs). Persistência de lições entre comandos da mesma sessão é necessária.
2. Spring Security em classpath protege todos os endpoints sem `SecurityFilterChain` customizado. `/v3/api-docs` retorna 401 (não 404) — confirma que springdoc registrou o endpoint mas Spring Security está bloqueando. Será resolvido quando `SecurityFilterChain` for configurado na Camada 2.
3. `<source>` + `<target>` no maven-compiler-plugin não é equivalente a `<release>` desde Java 9. `<release>` garante que apenas APIs públicas da versão alvo são usadas, evitando uso acidental de APIs internas do JDK atual.
4. BOM do Spring Boot 3.5.14 fixa Testcontainers em 1.21.4 mesmo com 2.0.5 disponível no Maven Central. Usar versão do BOM evita incompatibilidades.
5. `mvn -N wrapper:wrapper -Dmaven=X` gera wrapper na versão pedida, mas a versão default do plugin pode estar desalinhada da Maven local. Wrapper foi gerado em 3.9.9 (Maven local: 3.9.15) — recomendado alinhar manualmente em `maven-wrapper.properties` em etapa futura.
6. Spring Boot 3.x mostra warning de "spring.jpa.open-in-view is enabled by default" mesmo configurando `false` explicitamente. É bug conhecido do Spring Boot, não regressão da config. Ignorar.
7. `-Amapstruct.defaultComponentModel=spring` no `<compilerArgs>` gera warning "options were not recognized by any processor" quando não há classe `@Mapper` no projeto. Investigar na Camada 2 quando primeiro mapper for criado: pode ser necessário usar `@Mapper(componentModel = "spring")` direto em cada classe se o argumento global não estiver sendo passado corretamente.

---

## Lições da Etapa 1.3

### Candidatos a hook (automatizar em etapas futuras)

1. Validar que docker-compose.yml não declara campo "version:" (obsoleto em Compose v2).
2. Validar que imagens em docker-compose.yml estão fixadas em major.minor (não :latest).
3. Validar que portas em docker-compose.yml estão expostas em 127.0.0.1, não 0.0.0.0.

### Lições de ambiente

1. Sandbox de bash do Claude Code bloqueia "sleep N && comando" para evitar uso impróprio. Solução idiomática é usar polling com `until <check>; do sleep N; done`.
2. PowerShell padrão sem -Encoding UTF8 lê arquivos UTF-8 incorretamente, mas o conteúdo em disco continua íntegro. Confirmação obrigatória: `Get-Content arquivo -Encoding UTF8`.
3. Healthcheck do Redis com senha precisa receber -a ${REDIS_PASSWORD} explicitamente. Sem isso, healthcheck falha silenciosamente e container fica unhealthy indefinidamente.

---

## Lições da Etapa 1.2

### Candidatos a hook (automatizar em etapas futuras)

1. Validar antes de iniciar Tier 2 que branch protection da default branch está ativa e enforcing — não apenas configurada na interface.
2. Validar que o usuário não está na lista de bypass do ruleset, mesmo sendo admin.

### Lições de ambiente

1. **GitHub Free + repo privado não suporta branch protection.** Tanto "Branch protection rules" (clássica) quanto "Repository rulesets" (nova) ficam configuradas mas marcadas como "Not enforced" — interface permite criar e dá impressão de que está protegido, mas não bloqueia. Solução: tornar repo público (Free) ou migrar pra GitHub Team (pago). Decisão tomada: público.
2. **Validação obrigatória de branch protection é não-negociável.** O passo de testar push direto e ver erro deve sempre ser executado. Sem isso, branch protection pode estar inativa por meses sem ninguém notar.
3. **Repo tornado público não tem custo se nenhum dado sensível está versionado.** ADR-005 (auth) e práticas de .env já garantem isso. Ganho colateral: GitHub Actions ilimitado em repo público, alinhado com o pulo de posicionamento profissional do blueprint.

---

## Histórico de mudanças deste documento

- **2026-05-07** — Etapa 1.4 concluída: Spring Boot 3.5.14 + Java 21 inicializado manualmente, pom.xml com toda a stack, Maven Wrapper 3.9.9, JaCoCo configurado (prepare-agent + report). Mergeado via PR #8.
- **2026-05-07** — Etapa 1.3 concluída: docker-compose.yml com Postgres 16 e Redis 7 rodando e validado (8 checks).
- **2026-05-07** — Etapa 1.2 concluída: branch protection ativa via Repository Ruleset após repo se tornar público.
- **2026-05-07** — Etapa 1.1 concluída: critérios marcados. Seção de lições reescrita após revisão para conter apenas o observado na sessão.
- **2026-05-06** — Criação inicial. Camada 0 marcada como concluída. Critérios da Camada 1 detalhados.
