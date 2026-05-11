# Progresso — Construção da Fábrica AI-Native

> Documento de tracking. Mostra **onde estamos** na construção da fábrica e do produto.
> Atualizado conforme camadas avançam. Diferente do `decisoes.md` (que registra escolhas) e dos `adrs.md` (que registram porquês), este documento responde a pergunta: "em que ponto eu estou?".

**Última atualização:** 2026-05-11 (Sub-etapa 4.9 — Primeiro subagent: pr-reviewer)

---

## Status geral

| Camada | Descrição | Status |
|---|---|---|
| **0** | Discovery (visão, ADRs, decisões, ambiente) | ✅ Concluída |
| **1** | Infraestrutura de confiança | ✅ Concluída |
| **2** | Arquitetura otimizada para agentes | ✅ Concluída |
| **3** | Configuração do Claude Code (subagents, skills, hooks) | 🟢 Em andamento |
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

**Status:** ✅ Concluída em 2026-05-08

### Objetivo

Construir a fundação não-negociável da fábrica: testes em três níveis, CI confiável, hooks locais, banco rodando, projeto Spring Boot inicializado, projeto Next.js inicializado. **Zero código de feature nesta camada** — só infraestrutura de validação.

### Critérios de "pronto"

- [x] Repo configurado com `.gitattributes`, `.gitignore`, README inicial
- [x] CLAUDE.md mínimo do projeto criado (apontando para docs)
- [x] Estrutura de pastas inicial criada
- [x] `docker-compose.yml` rodando Postgres 16 + Redis 7
- [x] Scripts PowerShell criados: `setup.ps1`, `dev.ps1`, `test.ps1`, `check.ps1`, `ship.ps1`
- [x] Projeto Spring Boot inicializado via Spring Initializr (manualmente)
- [x] `pom.xml` com todas as dependências da stack
- [x] Flyway configurado, primeira migration criada (schema vazio + tabela de versão)
- [x] Testcontainers configurado e funcional
- [x] Hello-world endpoint passando teste e2e via Testcontainers
- [x] JaCoCo configurado com thresholds (BUNDLE 75%, infrastructure 60%; domain/application/interfaces aguardam Camada 2)
- [x] Checkstyle + SpotBugs configurados
- [x] Projeto Next.js inicializado
- [x] GitHub Actions configurado: lint + test + build em PR
- [x] CI verde no primeiro commit em `main`
- [ ] Pre-commit hook local rodando lint + format
- [x] Branch protection em `main` (sem push direto, exige PR e CI verde)

### Auditoria final (Etapa 2.8)

Critérios de "Camada 1 concluída" definidos em `roadmap-camada-1.md`:

| Critério | Status | Evidência |
|---|---|---|
| Clone novo + `setup.ps1` em <10 min | ✅ | 29 segundos (clone 1.5s + setup ~27s). Margem 20x. Nota: setup passa silenciosamente com `.env` ausente — credenciais vazias. Débito técnico registrado. |
| CI verde no `main` em ≥3 commits consecutivos | ✅ | PRs #26, #25, #24 mergeados com `build: COMPLETED / SUCCESS` |
| Cobertura JaCoCo nos thresholds | ✅ | `mvnw verify` local confirmou. BUNDLE 75%, infrastructure 60% |
| ≥1 PR rejeitado pelo CI por motivo legítimo | ✅ | Etapa 1.2: PR #12 bloqueado por branch protection (teste destrutivo). Etapas 2.4 e 2.5: validações destrutivas confirmaram CI falhando em violações reais |
| Operador confia que CI verde = código mergeable sem segunda revisão | 🟡 Parcial | Sim para código de produção. Scripts/configs ainda precisam de validação manual destrutiva — CI não cobre `.ps1` e `.env`. Confiança expandirá com hooks da Camada 3. |
| `progresso.md` atualizado | ✅ | Esta auditoria + outras seções refletem estado atual |

**Resultado:** Camada 1 CONCLUÍDA.

Ver `docs/retrospectiva-camada-1.md` para reflexão histórica e `docs/hooks-pendentes.md` para lista consolidada de candidatos a hook (input para Camada 3).

### Roadmap detalhado

Ver `docs/roadmap-camada-1.md` para o passo a passo das 2 semanas.

---

## Camada 2 — Arquitetura otimizada para agentes

**Status:** ✅ Concluída em 2026-05-10
**Pré-requisito:** Camada 1 concluída

### Objetivo

Implementar a estrutura de bounded contexts, primeiros agregados/use cases, value objects compartilhados (`Money`), padrões de mapping (MapStruct) e validação. Ainda sem features completas — só o "esqueleto rico" sobre o qual features serão delegadas no Tier 2.

### Critérios de "pronto" (preliminar)

- [ ] Estrutura de pacotes implementada conforme ADR-004
- [x] Value object `Money` implementado e testado
- [x] Bounded context `conta` com domínio puro + use cases + repositório (domain 3.2 ✅, infra+repository 3.3 ✅, use cases+controllers 3.4 ✅)
- [x] Bounded context `categoria` no mesmo padrão
- [x] Bounded context `transacao` ponta a ponta (domain + infra 3.6, application + interfaces 3.7)
- [x] Saldo derivado da Conta cruzando bounded contexts via porta (3.8)
- [x] MapStruct funcionando entre Entity JPA ↔ Domain
- [x] Bean Validation aplicada em DTOs de Request
- [x] Cobertura JaCoCo nos thresholds definidos
- [ ] Spring Security configurado com JWT + refresh rotativo
- [ ] Endpoints de auth funcionando (signup, login, refresh, logout)
- [ ] OpenAPI gerada automaticamente

(Detalhes serão expandidos quando a Camada 1 estiver concluída.)

---

## Camada 3 — Configuração do Claude Code

**Status:** 🟢 Em andamento
**Pré-requisito:** Camada 2 com pelo menos um bounded context completo

### Objetivo

Configurar `CLAUDE.md` rico, criar 3-5 subagents focados, criar 5-10 skills (slash commands) para workflows repetidos, configurar hooks que substituem revisão manual.

### Sub-etapas concluídas

- **4.0 — Infraestrutura organizacional** (2026-05-10): estrutura `.claude/` separada por escopo, `.githooks/` com `core.hooksPath` configurado por `setup.ps1`, ADR-009 e ADR-010, triagem do `hooks-pendentes.md`. Sem hooks/agents/skills funcionais. PR #38.
- **4.0.1 — Fix de posição do bloco core.hooksPath** (2026-05-10): `setup.ps1` reorganizado para configurar `core.hooksPath` ANTES de operações que podem falhar (Docker, Maven). Bug descoberto em smoke test destrutivo pós-merge da 4.0. Validação destrutiva com Docker propositalmente quebrado confirma fix. Débito Docker `container_name:` registrado em `hooks-pendentes.md`. PR #39.
- **4.1 — Hook universal de Conventional Commits** (2026-05-10): primeiro hook funcional do projeto. Estabelece padrao de invocacao em 3 camadas (entrypoint bash sem extensao -> companheiro `.ps1` -> hook em `.claude/hooks/universal/`). Valida mensagem de commit contra Conventional Commits (10 tipos permitidos, scope opcional, breaking change via `!`, descricao minima 10 chars). Excecoes automaticas: merge e revert. Override `--no-verify` documentado em `decisoes.md`. Entrypoint usa `powershell` (PS5.1, unico disponivel no ambiente). Validacao destrutiva manual confirma bloqueio de mensagem invalida + bypass por `--no-verify`. PR #40.
- **4.2 — Hook universal de encoding UTF-8** (2026-05-10): segundo hook funcional. Estreia o entrypoint `pre-commit` no padrao de 3 camadas, primeira validacao multi-arquivo via `git diff --cached`, e padrao orquestrador 1:N no companheiro `pre-commit.ps1` (preparado para 4.3+). Whitelist por extensao + nomes exatos. Regra: `.ps1` rejeita BOM (licao 2.6); outros tipos aceitam BOM. 5 cenarios destrutivos validados (md ok, ps1+BOM bloqueia, java Latin-1 bloqueia, png ignorado, --no-verify bypassa). PR #41.
- **4.2.1 — Padroes de validacao destrutiva** (2026-05-10): sub-etapa doc-only registrando licao descoberta em smoke test pos-merge da 4.2. `[System.IO.File]::WriteAllText` com path relativo em PowerShell grava em `[System.Environment]::CurrentDirectory` (nao em `$PWD`), produzindo falso positivo silencioso quando sessao fez `cd`. ADR-011 formaliza padroes de validacao destrutiva: `Test-Path` apos criar arquivo, `git status` antes de `git commit`, verificacao de exit code, sincronizacao de `Environment.CurrentDirectory`. Aplica retroativamente a sub-etapas 4.3+; 4.0-4.2 nao sao revistas (smoke test corrigido confirmou codigo correto). PR #42.
- **4.7 — Hook Java/Spring de @Entity sem migration Flyway (modo conservador)** (2026-05-11): sexto hook funcional, segundo de stack. Bloqueia commit com `.java` novo (status A) contendo `@Entity` em `src/main/java/` se nao houver migration nova em `src/main/resources/db/migration/V<n>__*.sql`. Modo fail. Escopo conscientemente reduzido vs licao 2.1 -- modificacao de Entity existente (status M) **nao dispara** o hook, ficou como debito explicito em `hooks-pendentes.md`. Hook preventivo: projeto ja tem ratio coerente (3 Entities + 4 migrations). Padrao agnostico a escopo reforcado: orquestrador `pre-commit` continua sem distincao sintatica entre universal e stack. 6 cenarios destrutivos sob ADR-011 incluindo modificacao de Entity real (Categoria.java) para confirmar empiricamente que hook nao dispara em status M. PR #47.
- **4.9 — Primeiro subagent: `pr-reviewer` (Haiku)** (2026-05-11): primeiro subagent do projeto. Marco estrutural — Camada 3 do blueprint pede 3-5 subagents focados, este e o primeiro. `.claude/agents/pr-reviewer.md`, modelo Haiku, tools restritas a `Read, Grep, Glob, Bash` (read-only). Invocacao **proativa via `description`** — Claude principal decide quando delegar. Complementa hooks pre-commit: revisa **decisoes de design vs ADRs, coerencia com decisoes.md, logica do codigo, cobertura de testes, documentacao alinhada, padroes do projeto**. Nao duplica verificacoes dos hooks (encoding, blank lines, Conventional Commits, Maven, @Entity, tamanho de docs). Output Markdown estruturado em 3 secoes (Bloqueadores, Sugestoes, Elogios) — operador (humano) decide se cola no PR como comentario. Subagent **nao posta no PR** via `gh pr review` (limite consciente). Descoberta de pre-redacao: convencao Claude Code para subagents e flat em `.claude/agents/*.md`, NAO em subpastas. `.claude/agents/` tem 3 subpastas (`universal/`, `java-spring/`, `local/`) e `.claude/hooks/` tem 5 — estruturas assimetricas; prescricao do prompt assumiu simetria total sem verificar estado real; ADR-011 detectou divergencia na Tarefa 1 antes de virar registro errado. Pasta `.claude/agents/universal/` continua existindo mas nao recebe subagent. PR #50.
- **4.8 — Sub `blueprint-fabrica-ai-native.md` ao repo (doc-only)** (2026-05-11): sub-etapa minimalista. `CLAUDE.md` ja referenciava `docs/blueprint-fabrica-ai-native.md` em "Onde buscar mais" desde a 4.6, mas o arquivo nunca havia sido commitado -- link apontava para arquivo inexistente. Operador tinha copia em `C:\Users\rezen\Downloads\` (644 linhas, UTF-8 sem BOM). Sub-etapa copia o arquivo para `docs/`. Documento de referencia conceitual fundadora -- define vocabulario do projeto (Tier 1/2/3, routines, `/batch`, "modelo Boris Cherny"). 6 headers `###` editados para conformar com hook 4.3 (linhas em branco apos header). Sem mudanca de codigo, sem hook. PR #49.
- **4.7.1 — Registro de licoes pos-smoke da 4.7 (doc-only)** (2026-05-11): sub-etapa doc-only analoga a 4.2.1. Smoke test pos-merge da 4.7 (cenario B) usou Java single-line sintetico que nao casou com a regex `(?m)^\s*@Entity\b` do hook entity-migration. Falso negativo apareceu como falha do hook em producao; diagnostico identificou que o problema era no smoke test, nao no hook. **Duas licoes registradas:** (1) tecnica -- regex do entity-migration e fragil para Java single-line, ajuste para `@Entity\b` fica como debito; (2) operacional -- smoke test pos-merge usa input idiomatico, nao sintetico. Sem mudanca de codigo. PR #48.
- **4.6 — CLAUDE.md do projeto** (2026-05-11): primeira sub-etapa de curadoria (nao codigo). Substitui CLAUDE.md placeholder (criado na Camada 1, 21 linhas) por versao estrutural com 7 secoes (identidade, stack, ambiente, mecanismo de hooks, convencoes, onde buscar mais, o que nao fazer). 95 linhas, ~5KB. Conteudo volatil delegado para `docs/` via links — CLAUDE.md so atualizado em sub-etapas que mudam stack/ambiente/convencoes/restricoes. Validacao via smoke test pos-merge em sessao nova do Claude Code. PR #46.
- **4.5 — Hook Java/Spring de Maven release** (2026-05-11): quinto hook funcional, primeiro de stack. Ativa `.claude/hooks/java-spring/` (vazia desde 4.0). Valida que `pom.xml` no diff staged contem `<release>` (qualquer valor). Modo fail. Padrao consolidado: orquestrador `pre-commit` agnostico a escopo; hook decide aplicabilidade lendo o proprio `git diff --cached`. Hook preventivo — `pom.xml` atual ja cumpre (licao 1.4 aplicada na Camada 1). 6 cenarios destrutivos sob ADR-011. PR #45.
- **4.4 — Hook universal de tamanho de docs (modo warn)** (2026-05-11): quarto hook funcional. Terceiro no orquestrador `pre-commit`. Alerta sobre `.md` em `docs/` com mais de 800 linhas — **nao bloqueia commit**, apenas visibiliza. Estabelece padrao `warn` para regras subjetivas (distinto de `fail` para regras objetivas). Fecha lote universal de Markdown (encoding 4.2 + blank lines 4.3 + tamanho 4.4). 5 cenarios destrutivos sob ADR-011. PR #44.
- **4.3 — Hook universal de Markdown blank lines** (2026-05-10): terceiro hook funcional. Segundo hook no orquestrador `pre-commit` (1:N da 4.2). Valida headers `##`-`######` em arquivos `.md` (qualquer pasta). Fronteira de arquivo e blocos de codigo isentos. Primeira aplicacao de ADR-011 desde a redacao do prompt — 7 cenarios destrutivos com `Test-Path` + `git status` + sincronizacao de `Environment.CurrentDirectory` em cada um. PR #43.

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

## Licoes da Sub-etapa 4.9

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — entrega subagent, nao hook.)

### Licoes de ambiente

1. **Convencao Claude Code para subagents e flat em `.claude/agents/*.md`.** Sem subpastas. A 4.0 criou 3 subpastas em `.claude/agents/` (`universal/`, `java-spring/`, `local/`) por intencao de organizacao por escopo. Vale para hooks (`.claude/hooks/` tem 5 subpastas: `java-spring`, `local`, `next`, `universal`, `windows` — `.ps1` sao chamados manualmente por scripts e podem morar onde quiser). NAO vale para subagents — Claude Code so descobre subagents em `.claude/agents/*.md` direto. Pastas em `.claude/agents/` continuam existindo (decisao da 4.0 nao revertida) mas nao recebem subagent. Mitigacao para sub-etapas futuras: subagents sempre em `.claude/agents/<nome>.md`; subpastas ficam apenas para hooks. Categoria meta-operacional: "convencao Claude Code descoberta apos prescricao do projeto" — vale auditar outras decisoes da 4.0 contra documentacao oficial quando outros componentes (skills, MCPs) entrarem. Skills: `.claude/skills/` tem 2 subpastas (`local`, `universal`) — pre-validar convencao Claude Code antes de criar primeira skill.

2. **Frontmatter `model: haiku` e critico em subagents.** Sem este campo, subagent herda modelo da sessao principal (Opus se a sessao tiver Opus). Custo escala desnecessariamente. Blueprint diz `pr-reviewer`/`architect-reviewer`/`test-writer` rolam Haiku ou Sonnet. Padrao: **sempre especificar `model:` explicitamente** em subagents, nunca deixar default `inherit`.

3. **Combinar `tools` (allowlist) com `disallowedTools` (denylist) gera bug silencioso.** Pesquisa mostrou que dois times distintos relataram subagent sem nenhum tool disponivel por usar ambos campos simultaneamente. Allowlist `tools: Read, Grep, Glob, Bash` ja restringe — denylist redundante e arriscada. Padrao: usar **so um** dos dois campos, idealmente allowlist explicita.

4. **Subagent pode bater em prompt interativo se parent nao tem allowlist Bash.** Issue conhecida (#25526 no GitHub do Claude Code): subagent com `tools: Bash` pode receber permission denied se `.claude/settings.json`/`settings.local.json` do projeto nao tem allowlist explicita pra Bash. Mitigacao: validar no smoke test pos-merge. Neste projeto: `.claude/settings.local.json` ja contem `Bash(powershell *)`, `Bash(git *)`, `Bash(gh pr *)` — risco descrito esta mitigado preventivamente.

5. **Subagent revisor read-only e mais seguro que revisor com Write.** Padrao do blueprint: "Quando voce precisa restringir tools (ex: revisor que so le, nao escreve)". `pr-reviewer` com `tools: Read, Grep, Glob, Bash` nao consegue escrever arquivos, comitar, postar comentario no PR, ou modificar configuracoes. Operador (humano) e gate de toda escrita. Padrao a manter em `architect-reviewer` e futuros revisores.

6. **Discrepancia entre prescricao e estado real detectada na Tarefa 1 (ADR-011 funcionando).** Prompt da 4.9 afirmou que `.claude/agents/` tinha 5 subpastas; estado real mostra 3 (`universal`, `java-spring`, `local`). Causa: prescricao confundiu com `.claude/hooks/` (que tem 5) — assumiu simetria total entre `.claude/{hooks,agents,skills}/` sem verificar estado real. Estruturas sao assimetricas (hooks=5, agents=3, skills=2), provavelmente refletindo decisoes especificas da 4.0. Categoria "prescricao assumiu coisa sem confirmar". Mitigacao confirmada: ADR-011 obriga validacao na Tarefa 1; divergencia foi detectada antes de virar registro factualmente errado em `decisoes.md`/`progresso.md`. Padrao a manter: ler arquivo/diretorio vivo (`Get-ChildItem`, `cat`, `Test-Path`) sempre antes de escrever texto sobre estado do projeto.

## Licoes da Sub-etapa 4.8

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa -- doc-only minimalista.)

### Licoes de ambiente

1. **Referenciado em docs mas ausente no repo.** `CLAUDE.md` referenciava `docs/blueprint-fabrica-ai-native.md` em "Onde buscar mais" desde a 4.6. `docs/progresso.md` tambem referenciava em criterios da Camada 1. Arquivo nunca foi commitado -- link quebrado por meses. Padrao a vigiar: quando CLAUDE.md ou docs adicionam link para arquivo em `docs/`, confirmar com `Test-Path` que o arquivo existe no repo, nao apenas no sistema do operador.
2. **Sub-etapa de "cumprir promessa ja feita".** 4.8 nao adiciona nada novo -- entrega ato fundador que ja estava implicito em outros docs. Categoria meta-operacional: revisao periodica de links em `CLAUDE.md` e `docs/` pode revelar arquivos referenciados mas ausentes. Vale executar de novo quando sub-etapas futuras adicionarem novas referencias.
3. **Blueprint editado com 6 insercoes de linha em branco para conformar com hook 4.3.** Formato cosmético; conteudo semantico preservado integralmente. Decisao consciente: arquivos em `docs/` seguem regra do projeto. Excecao criaria categoria "arquivos privilegiados" sem beneficio real. Override `--no-verify` foi considerado e rejeitado -- deve ser reservado para emergencias, nao trabalho mecanico de formatacao.
4. **Placeholder `PR #XX` orfao de sub-etapa anterior pode ser substituido pelo PR errado em sub-etapa seguinte.** Achado pre-merge da 4.8: a entrada da 4.7.1 em "Sub-etapas concluidas" tinha `PR #XX` (placeholder nao substituido pela 4.7.1, que so atualizou o "Historico de mudancas"). Agente da 4.8 substituiu pelo PR atual (#49) em vez do correto (#48). Mitigacao para sub-etapas futuras: ao editar progresso.md, se encontrar `PR #XX` em entrada **diferente** da sub-etapa atual, **parar e perguntar** ao operador qual o PR correto. Nao assumir que `XX` se refere a sub-etapa em curso. Categoria "decisao silenciosa em zona limitrofe" -- 12a ocorrencia rastreada nas sub-etapas.

## Licoes da Sub-etapa 4.7.1

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa -- doc-only.)

### Licoes de ambiente

1. **Regex de deteccao em `.ps1` pode falhar silenciosamente em input edge case.** Regex `(?m)^\s*@Entity\b` do hook entity-migration funciona em Java idiomatico (anotacao em linha propria) mas nao em Java single-line. Hook nao detecta `@Entity` no meio da linha. Mitigacao quando tocar no hook por outro motivo: `@Entity\b` (sem ancora de linha) cobre ambos os casos sem perder precisao (word boundary impede match em `@EntityListeners`).
2. **Smoke test pos-merge deve usar input idiomatico, nao sintetico.** Cenario B do smoke da 4.7 usou Java single-line (`package x; import y.Entity; @Entity public class Foo {}`) que nao exercitou o hook corretamente. Diagnostico inicial hipotetizou bug no hook; investigacao revelou que o problema era o input do smoke. Regra consolidada: smoke test cria codigo como humanos escreveriam (multi-linha, anotacao em linha propria, etc.). Excecao: hooks de minificacao usam input compactado por design.
3. **Padrao "sub-etapa doc-only de registro pos-smoke falho" consolidado.** Esta sub-etapa repete o tipo introduzido pela 4.2.1 (que formalizou ADR-011 apos smoke test da 4.2 falhar com `Environment.CurrentDirectory` vs `$PWD`). Categoria estabelecida: quando smoke test pos-merge falsifica resultado ou revela edge case real, sub-etapa doc-only registra causa raiz + decisao consciente (corrigir agora vs aceitar como debito).
4. **Decisao consciente "registrar primeiro, corrigir se aparecer dor" formalizada.** Patch tecnico (4.7.1) foi conscientemente preterido. Razao: caso edge artificial, custo de sub-etapa > valor de cobrir caso improvavel. Padrao operacional: registrar debito explicito em `hooks-pendentes.md`, atualizar quando tocar no hook por outro motivo, ou abrir sub-etapa dedicada se aparecer dor real.

## Licoes da Sub-etapa 4.7

### Candidatos a hook (automatizar em etapas futuras)

(A preencher se houver durante execucao.)

### Licoes de ambiente

1. **Padrao "decisao consciente de escopo reduzido vs licao original" formalizado.** Hooks podem implementar fracao da regra quando caso completo produz falso positivo alto (status M de Entity existente geraria falso positivo alto por refatoracao cosmetica). Restante vira debito explicito em `hooks-pendentes.md`, nao decisao silenciosa. Cobre o caso onde a licao original e mais ampla que o que e implementavel com confianca.

2. **Heredoc PowerShell `@" ... "@` aninhado em `powershell -Command "..."` via Bash sofre conflito de escape e quebra com `TerminatorExpectedAtEndOfString`.** O shell Bash interpreta as aspas duplas dentro do heredoc antes de passar para o PowerShell, terminando a string prematuramente. Terceira recorrencia da categoria "ambiente PowerShell tem comportamento nao-obvio que quebra silenciosamente" (apos pwsh/powershell 4.1, array unwrapping 4.3, em-dash 4.4). **Fix operacional:** usar tool `Write` para criar arquivos com conteudo multi-linha em sub-etapas destrutivas; reservar `WriteAllText` para escrita simples sem heredoc em sub-etapas futuras.

3. **`git diff <arquivo>` em cenario destrutivo que toca codigo real mostra delta volatil entre working tree e HEAD -- nao confirma estado do arquivo commitado.** No cenario 4, `git diff CategoriaEntity.java` mostrou linha com `-` (comentario do cenario 4). Agente interpretou como "arquivo restaurado ao estado original" e prosseguiu. Na verdade, o `-` indicava "working tree remove esta linha em relacao ao HEAD" -- e HEAD era o commit do cenario 4, que ja tinha o comentario. O `git reset --hard HEAD~5` produziu estado limpo por coincidencia estrutural (Commit 4, alvo do reset, era anterior aos cenarios e tinha o arquivo original). **Mitigacao para sub-etapas futuras que tocam codigo real:** apos restaurar backup, validar com `git show HEAD:<path>` para confirmar o que esta commitado em HEAD, nao confiar apenas em `git diff` que mostra delta volatil. Categoria "decisao silenciosa em zona limitrofe" -- agente interpretou evidencia ambigua e prosseguiu sem pedir confirmacao.

## Licoes da Sub-etapa 4.6

### Candidatos a hook (automatizar em etapas futuras)

(A preencher se houver durante execucao.)

### Licoes de ambiente

1. **Padrao "conteudo volatil fica fora do CLAUDE.md" formalizado.** Sub-etapas que apenas adicionam hook nao editam CLAUDE.md. Sub-etapas que avancam Camada nao editam CLAUDE.md. Estado vive em `docs/progresso.md`; lista de hooks em `docs/hooks-pendentes.md`. Ambos ja sao linkados no CLAUDE.md -- atualizar esses docs e suficiente, sem precisar tocar no CLAUDE.md a cada sub-etapa.
2. **Primeiro CLAUDE.md nao e "criado do zero" -- e substituicao de placeholder.** Critério da Camada 1 registrava "CLAUDE.md minimo do projeto criado (apontando para docs)" -- placeholder de 21 linhas estava presente. Prompt da 4.6 prescrevia `Test-Path CLAUDE.md -> False`, mas retornou `True`. Agente detectou e reportou antes de agir. Sub-etapa foi reencuadrada como "substituicao" em vez de "criacao", sem mudanca operacional (git add cobre ambos os casos, status mostra "modified" em vez de "new file").
3. **Prescricao baseada em pressuposto nao confirmado.** Prompt original prescrevia `Test-Path CLAUDE.md` retornando `False`, sob suposicao de que o arquivo nao existia. Pre-existia placeholder de 21 linhas criado na Camada 1. Agente detectou divergencia na pre-condicao ADR-011 e reportou. Segunda recorrencia da categoria "prescricao assumiu ambiente sem confirmar" (primeira: numeracao licao 2.5 vs 1.4 na 4.5). Mitigacao para prompts futuros: verificar explicitamente todos os `Test-Path X -> False` prescritos antes de redigir.

## Licoes da Sub-etapa 4.5

### Candidatos a hook (automatizar em etapas futuras)

(A preencher se houver durante execucao.)

### Licoes de ambiente

(A preencher se houver durante execucao. Esperado pelo menos: padrao "hook decide aplicabilidade internamente" consolidado em segunda dimensao — universais (filtram por extensao generica) e stack (filtram por arquivo especifico) convivem no mesmo orquestrador sem distincao sintatica.)

## Licoes da Sub-etapa 4.4

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Licoes de ambiente

1. **Caracter em dash (U+2014) em arquivo `.ps1` quebra parse do PowerShell 5.1.** Em UTF-8, o em dash ocupa 3 bytes: `0xE2 0x80 0x94`. O PowerShell 5.1 le arquivos `.ps1` com encoding do sistema (tipicamente Windows-1252), interpretando `0x94` como aspa-curva-direita (`"`), o que termina a string prematuramente e gera `TerminatorExpectedAtEndOfString`. Fix: usar apenas ASCII em mensagens de hook. Em dash substituido por hifen simples `-`. **Regra transversal para hooks futuros:** sem caracteres Unicode nas strings de mensagem — apenas ASCII. Instrucao "Sem acentos" do prompt ja cobria acentos, mas nao colocava em evidencia caracteres tipograficos (em dash, aspas curvas). Agora cobertura explicita.
2. **Padrao `warn` vs `fail` formalizado em `decisoes.md`.** Regras subjetivas (tamanho de doc) saem com exit code 0 sempre; regras objetivas (encoding, blank lines, commits) saem com exit code 1 em violacao. Modo e decisao de design, nao implementacao — registrado quando o hook nasce, nao retroativamente.

## Licoes da Sub-etapa 4.3

### Candidatos a hook (automatizar em etapas futuras)

(A preencher se houver durante execucao.)

### Licoes de ambiente

1. **Padrao "extensao do orquestrador 1:N por linha" validado em pratica.** Sub-etapas seguintes que adicionarem hooks a `pre-commit` farao a mesma operacao trivial — uma linha no array `$hooks` em `.githooks/pre-commit.ps1`.

2. **PowerShell 5.1 desempacota array de 1 elemento ao retornar de funcao.** Bug descoberto durante validacao destrutiva: `Get-MarkdownViolations` retornando uma unica violacao (`PSCustomObject`) era atribuida como objeto solto, nao como array. `$violations.Count` em objeto solto retornava `$null`; `$null -gt 0` e `$false`; violacao silenciosamente ignorada. Comportamento confirmado em diagnostico isolado: `function Test { return @(@{a=1}) }; (Test).GetType().Name` retorna `PSCustomObject`, nao `Object[]`. **Fix aplicado:** envolver retorno em `@(...)` no consumidor — `$violations = @(Get-MarkdownViolations -FilePath $file)`. Alternativa equivalente: `return ,$violations` no produtor. Adotado fix no consumidor por ser mais defensivo se mudar produtor depois. **Categoria da licao:** segunda recorrencia de falso positivo silencioso em validacao destrutiva (primeira foi `Environment.CurrentDirectory` na 4.2.1). Diferentes causas raiz, mesma natureza — validacao aparenta passar mas nao exercitou o que deveria. **Cuidado transversal para hooks PS5.1 futuros:** sempre usar `@(...)` ao consumir retorno de funcao que pode retornar 0, 1 ou N itens. Adicionar ao template de hooks futuros.

3. **Validacao destrutiva precisa de staging isolado entre cenarios.** Cenarios 2 e 3 da primeira tentativa rodaram com staging contaminado de cenarios anteriores. Pre-condicao adicional: `git reset HEAD` (ou stage seletivo) antes de cada cenario garante que apenas o arquivo do cenario corrente esta sendo validado. ADR-011 ja prescreve `git status` antes de `git commit`, mas nao explicita o reset entre cenarios — vale revisar texto do ADR em sub-etapa futura.

---

## Licoes da Sub-etapa 4.2

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Licoes de ambiente

1. **`git reset --hard` e bloqueado pelo sandbox do Claude Code por design.** Nao ha forma de aprovar via permissao persistente no CLI — o gate deve permanecer ativo para exigir intervencao consciente. Procedimento: agente reporta commits a remover com `git log --oneline`, confirma branch com `git branch --show-current`, e solicita que o operador execute `! git reset --hard HEAD~N` no terminal. Alternativa seria criar branch nova a partir do commit desejado, mas isso diverge do roteiro de validacao destrutiva consolidado.
2. **Padrao orquestrador 1:N para `pre-commit` consolidado.** Sub-etapas 4.3+ apenas acrescentam linhas ao array `$hooks` em `.githooks/pre-commit.ps1` sem refatorar arquitetura.
3. **`[System.IO.File]::WriteAllText` com path relativo em PowerShell grava em `[System.Environment]::CurrentDirectory`, nao em `$PWD`.** Descoberto em smoke test pos-merge (operador, nao agente). Quando sessao PowerShell faz `cd` para entrar no repo apos abrir, `$PWD` e atualizado mas `Environment.CurrentDirectory` permanece no diretorio original (tipicamente home do usuario). Arquivo criado por `WriteAllText("arquivo.md", ...)` vai parar em `C:\Users\<user>\arquivo.md`; `git add arquivo.md` rodando em `C:\projetos\financas-lab\` falha com `pathspec did not match`; `git commit` reporta `nothing to commit, working tree clean`. Observador desatento conclui "cenario passou" quando hook nem foi invocado. **Conferencia empirica:** `Get-ChildItem C:\Users\rezen\test-*.*` veio vazio apos validacao destrutiva da branch da 4.2 — confirmando que agente do Claude Code nao caiu nesse gotcha (provavel: agente foi spawnado ja dentro do repo, sincronizando `Environment.CurrentDirectory`). Risco real existe em sessoes onde `$PWD` e `Environment.CurrentDirectory` divergem. **Categoria da licao:** padrao de validacao, nao bug de codigo. Resolvido na Sub-etapa 4.2.1 com ADR-011 formalizando "comando rodou sem erro != cenario exercitado" como principio de validacao destrutiva.

---

## Licoes da Sub-etapa 4.2.1

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum diretamente — padrao de validacao destrutiva e procedimento, nao codigo automatizavel facil. Nota de cuidado registrada em `hooks-pendentes.md`.)

### Licoes de ambiente

1. **Primeiro ADR de processo do projeto.** ADR-001 a ADR-010 sao decisoes de stack, layout, debitos. ADR-011 e o primeiro sobre **metodo de validacao**. Padrao de ADR funciona igualmente bem para decisoes de processo — vale considerar em sub-etapas futuras quando padroes operacionais aparecerem.
2. **Operador caiu, agente nao.** Risco assimetrico entre sessoes que iniciaram no repo (agente) e sessoes que fizeram `cd` para entrar (operador). ADR-011 cobre os dois casos com mesmo padrao — sem privilegio de "agente sabe o ambiente, pode pular o gate".
3. **Falso positivo silencioso e o pior tipo de bug em validacao.** "Nada deu erro visivel" e a mensagem mais perigosa quando o objetivo era reproduzir erro. ADR-011 ataca exatamente isso com verificacao de pre-condicao explicita.

---

## Licoes da Sub-etapa 4.1

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Licoes de ambiente

1. **`pwsh` (PowerShell Core 7) nao esta instalado neste ambiente.** Prompt assumiu `pwsh` disponivel sem verificar pre-requisito. Entrypoint `.githooks/commit-msg` ajustado para `powershell` (Windows PowerShell 5.1, nativo no Windows). Licao: prompts futuros devem declarar pre-requisitos de ambiente explicitamente (versao de PowerShell, versao de git, etc) na secao "Estado esperado ao iniciar". Segunda ocorrencia de prescricao assumindo ambiente sem confirmar (primeira: posicao do bloco `core.hooksPath` na 4.0).
2. **Primeira parada-e-reporta do agente em zona limitrofe tecnica real.** Tarefa 8 (cenario destrutivo 1) falhou com `pwsh: command not found`. Agente parou e reportou em vez de tentar workaround silencioso (ex: substituir `pwsh` por `powershell` sem avisar). Padrao consolidado em prosa funcionou pela primeira vez em zona limitrofe tecnica, nao apenas estilistica. Reforco para prompts futuros: a instrucao "nao tomar decisao silenciosa" esta funcionando.

---

## Lições da Sub-etapa 4.0

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa — etapa de infraestrutura, sem geração de código que produzisse lição.)

### Lições de ambiente

1. **`.gitignore` ignorava `.claude/` inteiramente** (linha adicionada na Camada 0 para excluir settings locais pessoais). Ao iniciar a Camada 3, a estrutura de projeto em `.claude/hooks/`, `.claude/agents/`, `.claude/skills/` precisava ser versionada. Resolução: substituir `.claude/` por `.claude/settings.local.json` e `.claude/settings.json` no `.gitignore`, preservando a intenção original (não versionar configs pessoais) sem bloquear a estrutura de projeto. Lição: ao decidir que `.claude/` vai hospedar artefatos de projeto, revisar o `.gitignore` é etapa obrigatória.

### Lições de ambiente (descobertas em smoke test destrutivo pós-merge)

**Bug operacional: bloco `core.hooksPath` em posição inadequada no `setup.ps1`.** Prompt da 4.0 prescreveu "antes da finalização do script (antes da mensagem de sucesso)". Agente seguiu literalmente — colocou após `docker compose up -d` e `mvnw clean install`. Em uso normal (Docker rodando), funciona. Quando Docker falha (cenário real do smoke test: conflito de container_name entre clones paralelos), script aborta em `exit 1` antes do bloco e `core.hooksPath` nunca é configurado. Resultado: clone novo em ambiente com Docker quebrado fica com estrutura `.claude/` presente mas mecanismo de hooks da 4.1+ inerte — falha silenciosa exata que a retrospectiva da Camada 1 documentou como cara de descobrir tarde.

**Categoria da lição: prescrição de prompt insuficientemente específica, não decisão silenciosa do agente.** Instrução vaga ("antes da finalização") gera execução tecnicamente correta mas operacionalmente frágil. Reforça princípio já conhecido: prompts cirúrgicos exigem especificidade absoluta sobre **invariantes** (o que NÃO pode falhar para o bloco rodar), não só sobre **fronteiras** (onde colocar). Resolvido na Sub-etapa 4.0.1.

**Segundo achado do smoke test: `docker-compose.yml` com `container_name:` fixo.** `financas-lab-postgres` e `financas-lab-redis` têm nome global; dois clones em paralelo disparam conflito do Docker daemon. Sem impacto em fluxo normal (1 clone por vez). Registrado como débito em `hooks-pendentes.md`, seção "Débitos de configuração". Resolver quando paralelismo de clones virar necessidade real (debugging em branch isolada, smoke test sistematizado pós-merge). Custo estimado: 1-2h.

**Reforço do princípio: smoke test destrutivo continua sendo instrumento de qualidade de primeira linha, mesmo em sub-etapas "só de infraestrutura".** A 4.0 parecia o tipo de etapa sem código novo, sem feature, só pasta e config — aparentemente dispensável de validação destrutiva rigorosa. Errado. Bug não estava no código, estava na **ordem de instruções** do `setup.ps1` — invisível em revisão de diff, visível em validação destrutiva real com cenário de falha. Quinta ocorrência consecutiva (2.6.1, 2.6.2, 2.8, 3.3.1, 4.0) em que smoke test destrutivo pegou bug que CI não pegaria.

---

## Lições da Etapa 3.8

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

1. **JPQL `SELECT new` com record exige path fully-qualified da classe e dos enums.** `SELECT new TotaisTransacaoPorConta(...)` sem package completo falha em tempo de parsing. Enums também precisam de path completo: `com.laboratorio.financas.transacao.domain.TipoTransacao.RECEITA`. Sem isso, Hibernate não resolve os símbolos no contexto JPQL.
2. **`WHERE t.contaId = :contaId OR t.contaDestinoId = :contaId` + `CASE WHEN ... AND t.contaId = :contaId`** é a combinação necessária para capturar transferências corretamente. O `WHERE` abre para todas as transações em que a conta participa (origem OU destino), e o `AND t.contaId/contaDestinoId` dentro do `CASE` distingue o papel da conta em cada tipo. Sem o `OR` no `WHERE`, transferências recebidas teriam totais sempre zero.
3. **`COALESCE(SUM(...), 0)` é necessário quando o `SUM` pode ser NULL.** `SUM` sobre conjunto vazio retorna NULL no SQL. Sem `COALESCE`, `TotaisTransacaoPorConta` receberia `null` no construtor do record, quebrando qualquer uso downstream.
4. **`mvnw clean verify` obrigatório antes de declarar etapa pronta** (reiterado após incidente da 3.7). Cache de compilação pode mascarar erros. Esta etapa passou em `clean verify` sem atrito, confirmando que o template JPQL prescrito funcionou na primeira tentativa.

---

## Lições da Etapa 3.7

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

1. **PostgreSQL não infere tipo de parâmetro `null` em JPQL quando o campo é `LocalDate`.** A query `(:dataInicio IS NULL OR t.data >= :dataInicio)` gera SQL `(? is null or t.data >= ?)`. Quando `:dataInicio` é null, o PostgreSQL não consegue inferir o tipo SQL do `?` e lança `could not determine data type of parameter $N`. UUID e enum null funcionam por ter representação unívoca. Solução: substituir null por valores sentinela (`LocalDate.of(1900,1,1)` e `LocalDate.of(9999,12,31)`) no repositório e remover o `IS NULL` da query.
2. **`Instant` retornado pelo JPA `save()` tem precisão de nanosegundos (Java) vs. microssegundos (PostgreSQL/banco).** `Instant.now()` retorna `...684277400Z` (9 casas decimais), mas ao recarregar do banco via `buscarPorId`, o mesmo campo retorna `...684277Z` (6 casas decimais). Comparar strings exatas de `criadoEm`/`atualizadoEm` entre `save()` e reload falha. Solução: usar `notNullValue()` em vez de comparação exata quando o teste não precisa verificar o valor preciso.
3. **`@Validated` na classe controller é necessário para ativar `@Min`/`@Max` em parâmetros primitivos (`int`) de query string.** Sem `@Validated`, as anotações são ignoradas silenciosamente — `page=-1` e `size=200` são aceitos. Com `@Validated`, violações lançam `ConstraintViolationException` (não `MethodArgumentNotValidException`), requerendo handler separado no `GlobalExceptionHandler`.
4. **Decisão silenciosa em zona limítrofe (recorrência sexta).** Prompt prescrevia JPQL com `IS NULL OR` uniformemente nos 5 filtros. Agente alterou para sentinelas hardcoded (`LocalDate.of(1900,1,1)`/`9999,12,31`) nos 2 filtros de data sem reportar. Operador detectou em revisão de diff antes do merge. Lição estatística: em todas as etapas da Camada 2, agente tomou pelo menos uma decisão silenciosa em zona limítrofe apesar das instruções explícitas em todos os prompts. A frequência torna inevitável que hooks mecânicos da Camada 3 incluam validação de diff contra padrões prescritos no prompt — não basta repetir "não tomar decisão silenciosa" em prosa. **Caso técnico real escondido pela sentinela:** após restaurar `IS NULL OR` conforme prescrito, CI revelou que Postgres não consegue inferir tipo de parâmetro `LocalDate` em `? IS NULL` — erro `could not determine data type of parameter $N`. Solução final: `COALESCE(:param, coluna)` nos 2 filtros de data, mantendo `IS NULL OR` nos outros 3 (UUID e enum funcionam normalmente). A sentinela original era resposta errada para problema real, não pura gambiarra. Lição complementar: "BUILD SUCCESS local" pode dar falso positivo quando o test runner não pega todos os testes — usar `mvnw clean verify` antes de declarar etapa pronta. Validação efetiva é CI verde, não build local.

---

## Lições da Etapa 3.6

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

1. **Checkstyle `lineWrappingIndentation=8` com `throw new Exception(` aninhado dentro de dois `if` exige 24 espaços de indentação no argumento** (16 do throw + 8 de wrapping), não 20. A diferença é não-óbvia quando se usa 4 espaços por nível e se perde a conta dos aninhamentos. Padrão a observar em código com múltiplos `if` aninhados antes do commit.
2. **`@AttributeOverride` deve ficar em linha única** quando o total de caracteres couber em 140 (limite do Checkstyle do projeto). A alternativa de quebrar `@Column(` em múltiplas linhas requer que os atributos do `@Column` comecem com 12 espaços (4 de campo + 8 de wrapping), não 8.
3. **Nomes de método de teste com underscore são violação de Checkstyle** (`MethodName` aceita apenas `^[a-z][a-zA-Z0-9]*$`). Padrão consolidado desde a Etapa 3.1, mas reapareceu nesta etapa — confirmar antes do primeiro compile.

## Lições da Etapa 3.5

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

(Nenhum novo nesta etapa.)

### Sobre a etapa em si (template replicável validado)

O segundo bounded context (`categoria`) foi implementado em etapa única, contra 4 etapas + 1 fix do `conta`. O template estabelecido por `conta` foi replicado sem zonas limítrofes significativas: nenhuma nova dependência no `pom.xml`, nenhum novo padrão de código, nenhuma decisão silenciosa necessária. As únicas adaptações foram semânticas (`hard delete` vs `soft delete`, filtro por `tipo` vs por `ativa`) — não estruturais. O compile passou com 0 violações Checkstyle na primeira tentativa e `mvnw verify` resultou em BUILD SUCCESS com 170 testes (36 novos), 0 SpotBugs, JaCoCo "All coverage checks have been met". Evidência de que o template é replicável.

## Lições da Etapa 3.4

### Candidatos a hook (automatizar em etapas futuras)

1. **Detectar identificadores inexistentes no codebase introduzidos por commit.** Agente escreveu testes com valores de enum que não existiam (`CREDITO`, `INVESTIMENTO`); o build falhou; o agente corrigiu sozinho sem reportar. Um hook que rode `git diff --staged` e valide que novos identificadores Java referenciam símbolos existentes (via compilação incremental ou grep no source) detectaria isso antes do commit.

### Lições de ambiente

1. **Decisão silenciosa em build error (recorrente).** Testes foram escritos com valores de enum inexistentes (`CREDITO`, `INVESTIMENTO`); build falhou; o agente leu o enum e corrigiu sozinho para os valores reais (`CARTAO_CREDITO`, `DINHEIRO`) sem reportar a discrepância. Mesmo padrão de "decisão silenciosa em zona limítrofe" das etapas anteriores, agora em forma de fix-de-compilação. Recorrência reforça que o padrão dificilmente vai ser eliminado só via prompt — vai precisar de hook mecânico na Camada 3 que detecte `git diff` introduzindo identificadores que não existem no codebase atual antes do commit.
2. **`log` como nome de constante viola Checkstyle `ConstantName`** (padrão `^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$`). Em Java, `private static final Logger` é constante e deve ser nomeada `LOG`. Regra a considerar em geração de código: constantes SLF4J sempre em `SCREAMING_SNAKE_CASE` mesmo sendo convenção do ecossistema usá-las em minúsculas.

---

## Lições da Etapa 3.3.1

### Candidatos a hook (automatizar em etapas futuras)

1. **Validar que scripts que invocam `mvnw spring-boot:run` passam `-Dspring-boot.run.profiles=<profile>` explicitamente.** Hook leve: `grep -nE "mvnw\s+spring-boot:run" scripts/*.ps1 | grep -v "spring-boot.run.profiles"` deve retornar zero linhas. Sem a flag, Spring cai em profile `default` sem datasource.

### Lições de ambiente

1. **Validação destrutiva manual em ambiente real é instrumento de qualidade de primeira linha.** Bug do `dev.ps1` não ativando profile `dev` passou pelo CI verde de toda a Camada 2 porque CI usa profile `test` via `@DynamicPropertySource`. Só apareceu quando o operador tentou subir a aplicação local de fato pós-merge da 3.3 — exatamente o tipo de cenário que CI não cobre. Quarta ocorrência consecutiva do mesmo padrão (2.6.1, 2.6.2, 2.8, 3.3.1).

---

## Lições da Etapa 3.3

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

(Nenhum novo nesta etapa.)

---

## Lições da Etapa 3.2

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

1. **Checkstyle `Indentation` com `lineWrappingIndentation=8` se aplica a parâmetros de construtor multi-linha e a argumentos em chamadas `this(...)`**. Parâmetros do construtor precisam estar em `base + 8` (ex: método em 4 espaços → parâmetros em 12). Argumentos de `this(...)` no corpo de método (em 8 espaços) precisam estar em 16. Getters de uma linha com `{ return x; }` violam `LeftCurly` — devem ser expandidos em três linhas. `if` sem `{}` viola `NeedBraces`. Corrido na tentativa inicial — corrigido antes do primeiro `mvnw verify` verde.

---

## Lições da Etapa 3.1

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

1. **Conflito naming de testes vs Checkstyle detectado em zona limítrofe.** `decisoes.md` prescrevia underscore (`metodoTestado_cenarioDoTeste_resultadoEsperado`), mas Checkstyle do projeto e testes existentes (`HealthcheckControllerTest`, `FlywayMigrationTest`) usam camelCase puro. Agente detectou em zona limítrofe e escalou em vez de tomar decisão silenciosa. Resolução: doc alinhado ao código vivo (camelCase puro). Reforça princípio: quando `decisoes.md` diverge do que está rodando (testes, configs, CI), a verdade canônica é o código vivo, e o doc é o débito a resolver.

---

## Lições da Etapa 2.9

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

(Nenhum novo nesta etapa.)

---

## Lições da Etapa 2.8

### Candidatos a hook (automatizar em etapas futuras)

1. **`setup.ps1` deve detectar `.env` ausente antes de subir containers.** Clone novo sem `.env` resulta em containers com credenciais vazias — Docker Compose interpreta variáveis ausentes como strings vazias e sobe silenciosamente. Hook: validar presença de `.env` antes de chamar Docker Compose, criando a partir de `.env.example` com aviso ou falhando com mensagem clara.

### Lições de ambiente

1. **Validação manual destrutiva de clone novo revelou falha silenciosa em `setup.ps1`.** Sem `.env`, Docker Compose sobe containers com credenciais vazias — setup "conclui com sucesso" porque `mvn -DskipTests` não testa conexão, mas ambiente é inutilizável para dev real. CI nunca teria detectado porque CI tem secrets injetados. Reforça princípio consolidado: validação manual destrutiva de fluxo completo (clone + setup + uso real) detecta classes de bug que automação jamais pega.

---

## Lições da Etapa 2.7

### Candidatos a hook (automatizar em etapas futuras)

1. **`npx shadcn@latest init --defaults` instala componente `button.tsx` automaticamente** além dos artefatos de init (`components.json`, `src/lib/utils.ts`, `globals.css`). Em etapas que proíbem componentes, o hook deve detectar e remover `src/components/ui/*.tsx` gerado pelo init antes do commit.

### Lições de ambiente

1. **Next.js 16 usa Tailwind v4** (não v3). Não há `tailwind.config.ts` separado — configuração via `@import "tailwindcss"` em `globals.css`. shadcn 4.x detecta isso automaticamente e usa `style: base-nova` com CSS variables. Agentes treinados em Next.js 14/15 com Tailwind v3 devem consultar `node_modules/next/dist/docs/` antes de editar estilos.
2. **`shadcn@latest init --defaults` não prompta — escolhe defaults internos (baseColor `neutral`, style `base-nova`).** O prompt original pedia `Slate`; o resultado foi `neutral`. Ambos são neutros aceitáveis. Em etapas futuras que exijam cor específica, passar `--base-color <cor>` explicitamente se o shadcn suportar a flag, ou rodar sem `--defaults` e responder prompts.
3. **`AGENTS.md` e `CLAUDE.md` gerados pelo create-next-app são mecanismo de contexto do Claude Code**, não arquivos decorativos. `CLAUDE.md` com `@AGENTS.md` é sintaxe aditiva que carrega o aviso quando o agente está trabalhando em `frontend/`. Não conflita com `CLAUDE.md` da raiz. Decisão inicial de remover foi corrigida pelo operador — registrar padrão: scaffolds Next.js 16+ incluem esses arquivos intencionalmente.

---

## Lições da Etapa 2.6.2

### Candidatos a hook (automatizar em etapas futuras)

1. Detectar comando nativo (`docker`, `git`, `mvn`, etc) seguido de `if ($LASTEXITCODE -ne 0)` em arquivos `.ps1` **sem** ser precedido por suspensão local de `$ErrorActionPreference`. Indica risco do mesmo bug que esta etapa corrigiu. Hook leve futuro: `grep -B1 -A2 "if (\$LASTEXITCODE" scripts/*.ps1` revisado caso a caso.

### Lições de ambiente

1. **`$ErrorActionPreference = "Stop"` + comando nativo + redirecionamento de stderr é incompatível em PowerShell.** Os operadores de redirecionamento (`2>&1`, `2>$null`, `2>&1 > $null`, `2>&1 | Out-Null`) **não são aplicados antes** do `Stop` interceptar stderr de comando nativo. Resultado: erro vaza pra tela com stack trace do PowerShell. Testado: 3 variantes de redirecionamento falharam. Única solução prática: suspender `Stop` localmente durante a checagem.
2. **Validação manual continua descobrindo bugs que automação não pega.** A 2.6.1 corrigiu um bug parecido (`Write-Error` + `exit` sob `Stop`), e foi descoberta na validação manual destrutiva. A 2.6.2 corrigiu outro bug do mesmo padrão raiz (`Stop` + comando que escreve stderr), descoberto também em validação manual destrutiva (rodar `dev.ps1` com Docker parado). Conclusão reforçada: validação manual destrutiva é instrumento de qualidade de primeira linha, não opcional.
3. **Diagnóstico via teste em terminal direto > inferência.** A solução final foi descoberta por reprodução isolada no terminal (3 tentativas, comparação de comportamento com/sem `Stop`). Sem reprodução isolada, teria ficado tentando variações com `2>&1` infinitamente. Padrão pra debugar comportamento confuso de PowerShell: reproduzir linha-a-linha no terminal direto antes de mexer em script.

---

## Lições da Etapa 2.6.1

### Candidatos a hook (automatizar em etapas futuras)

1. Detectar `Write-Error` seguido de `exit N` em arquivos `.ps1` — combinação que indica bug do mesmo padrão que esta etapa corrigiu. Hook leve: `grep -B0 -A1 "Write-Error" scripts/*.ps1 | grep -A1 "exit"`.

### Lições de ambiente

1. **Validação manual destrutiva pega bugs que validação automática mascara.** A 2.6 passou sintaxe (parser), encoding (sem BOM) e CI (porque CI invoca via subprocess, que traduz exceção terminating em exit 1 corretamente). Mas em uso interativo no PowerShell o `$LASTEXITCODE` ficava 0 falsamente. Conclusão: validação manual no fluxo real do operador descobriu bug que toda automação validou como verde. Validar destrutivamente é não-negociável.
2. **`$ErrorActionPreference = "Stop"` + `Write-Error` + `exit N` é armadilha clássica em PowerShell.** O `Stop` faz `Write-Error` virar exceção terminating, abortando o script antes do `exit N`. Em sessão direta, `$LASTEXITCODE` permanece com o valor do último comando externo que rodou (geralmente 0). Em subprocess (`powershell.exe -File`), o exit traduz pra 1 corretamente. Comportamento inconsistente. Padrão correto registrado em `decisoes.md`.
3. **Subprocess test mascara bug de exit code em PowerShell.** Para validar exit code real de scripts `.ps1`, usar `powershell.exe -Command` rodando o script + captura de `$LASTEXITCODE` na **mesma sessão**. Subprocess via `-File` reporta exit code do processo (que sempre é 1 quando há exceção), não do comportamento da sessão.
4. **Validação destrutiva da branch `== main` não foi exercitada** porque o agente está em feature branch durante o fix. O caminho de erro segue o mesmo padrão `Write-Host` vermelho + `exit 1` dos demais `Write-Error`s do `ship.ps1`, então é coberto pelo precedente da validação 1 (working tree sujo).

---

## Lições da Etapa 2.6

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

1. A tool `Write` do Claude Code cria arquivos com encoding UTF-8 sem BOM por default — confirmado via `xxd` (primeiros bytes `23 20`, não `EF BB BF`). Arquivos com apenas ASCII são reportados como "ASCII text" pelo `file`; arquivos com caractere backtick (`` ` ``) são reportados como "Unicode text, UTF-8 text" mesmo sem BOM. A verificação confiável é via `xxd`, não via `file`.
2. Usar a tool `Write` nativa do Claude Code para criar scripts `.ps1` é mais seguro que passar heredoc via `-Command` do `powershell.exe` no Bash tool — o escape de shell dentro do heredoc pode corromper o conteúdo (blocos duplicados, erros de texto).

---

## Lições da Etapa 2.5

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Licoes de ambiente

1. O `maven-checkstyle-plugin` ao ser configurado com `fileExtensions = java, properties, xml` também tentaria validar arquivos `.xml` e `.properties` do source tree com regras como `NewlineAtEndOfFile lineSeparator=lf`. Para evitar falsos positivos em arquivos que não são código Java, a configuração foi restrita a `fileExtensions = java`. Isso é suficiente para o objetivo da etapa.
2. O `tail` do log do `mvnw verify` é dominado por linhas de progress bar de download do Maven (first run baixa todas as deps do SpotBugs/Checkstyle). Em execuções subsequentes o log é compacto. Lição para futuras etapas: na primeira execução com novos plugins, o log útil fica no final — usar `grep -E` no arquivo completo é mais confiável que `tail`.
3. Indentação de `lineWrappingIndentation = 8` do Checkstyle `Indentation` module aplica-se à primeira linha de continuação após um `=` ou abertura de método multi-linha. Linhas seguintes da mesma expressão (ex: chain `withDatabaseName`, `withUsername`) podem ficar no mesmo nível da primeira continuação sem nova violação — Checkstyle não exige escalada incremental por linha de chain.

---

## Lições da Etapa 2.4

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Licoes de ambiente

1. JaCoCo `check` com `<minimum>` fora do intervalo [0.0, 1.0] gera BUILD FAILURE com mensagem `Rule violated... given minimum ratio is X.XX, but must be between 0.0 and 1.0` — a falha é de configuração inválida, não de cobertura insuficiente. Threshold > 1.0 prova que o gate roda e bloqueia o build, mas não valida a avaliação de cobertura em si.
2. Código de configuração Spring (`@Configuration` com `@Bean`) atinge 100% de cobertura de instrução JaCoCo com qualquer teste que carregue o contexto (`@SpringBootTest`). Todas as instruções do método `securityFilterChain` rodam na criação do bean — não em chamadas subsequentes. Isso significa que testes de comportamento do Security (ex: endpoint bloqueado retornando 401) não contribuem para cobertura JaCoCo do `SecurityConfig`, apenas para cobertura comportamental. Consequência prática: validação destrutiva por remoção de testes de Security não reduz coverage.
3. Uso de `tail` (Unix) em vez de equivalente PowerShell (`Select-Object -Last N`) em ambiente Windows — erro já documentado em `decisoes.md` repetido durante a execução. Candidato a hook que valide comandos Unix em scripts antes de rodar.

---

## Lições da Etapa 2.3

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Licoes de ambiente

1. Spring Security 6 sem `AuthenticationEntryPoint` explícito retorna 403 (não 401) para requisições não autenticadas quando `httpBasic` está desabilitado. Sem entry point configurado, o framework não sabe como desafiar o cliente e cai para 403 (Forbidden). Solução: adicionar `.exceptionHandling(ex -> ex.authenticationEntryPoint(...))` com `response.sendError(401)` explícito. Registrado em `decisoes.md` na seção Spring específico.

---

## Licoes da Etapa 2.2

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa. A regra "baseline-on-migrate apenas em profiles de teste/dev" foi formalizada em decisoes.md mas ja constava como candidato na Etapa 2.1.)

### Licoes de ambiente

1. Sufixo `IT` (convencao Maven Failsafe) nao e reconhecido pelo Surefire padrao do Spring Boot parent — testes `*IT.java` nao rodam sem configurar o Failsafe plugin. O projeto ja usa `*Tests` para testes de integracao via Testcontainers (ex: `FinancasApplicationTests`); seguir esse padrao e mais coerente que introduzir Failsafe nesta etapa. Decisao sobre adotar Failsafe formalmente fica para etapa propria com ADR se necessario.
2. O prompt original prescrevia sufixo `IT` (convencao Maven Failsafe) sem validar que o Failsafe plugin estava configurado no pom.xml — nao estava. Isso causou tentativa inicial de adicionar Failsafe ao pom.xml fora do escopo declarado, bloqueada pela Restricao 1 do prompt. Licao para geracao de prompts futuros: validar convencoes de teste do projeto (pom.xml, Surefire/Failsafe configurados, padrao de naming dos testes existentes) antes de prescrever sufixo de classe em prompt.

---

## Lições da Etapa 2.1

### Candidatos a hook (automatizar em etapas futuras)

1. Validar que classes base de teste em `src/test/java/.../shared/` (ou pacote equivalente) que servem de superclasse para outras classes de teste tenham modificador `abstract`. Sem `abstract`, JUnit tenta instanciar a classe base e roda o lifecycle dela além das filhas, gerando execução duplicada e potencial inicialização de container fora de hora.
2. Validar que `application-test.yml` (e qualquer profile de teste) não declare URL JDBC hardcoded quando o projeto usa Testcontainers via `@DynamicPropertySource`. Hardcoding anula o ponto da injeção dinâmica e faz o teste rodar contra banco que não é o do container.
3. Validar que `baseline-on-migrate: true` apareça **apenas** em profiles de teste (e potencialmente `dev`), nunca em `application.yml` (defaults) ou `application-prod.yml`. Em produção, baseline silencioso de schema desconhecido é fonte clássica de inconsistência.

### Lições de ambiente

1. Cold-start de CI com Testcontainers em runner do GitHub Actions (`ubuntu-latest`) foi de 33s no PR #17, bem abaixo dos 80-120s previstos no plano da etapa. O cache de camada do `postgres:16-alpine` no runner é mais agressivo que o estimado. Calibrar previsões futuras para 30-90s, com 120s como teto pessimista — não como expectativa.
2. `spring.jpa.hibernate.ddl-auto: validate` com schema vazio (sem migrations ainda) só funciona porque também não existem classes `@Entity` no projeto. Quando as primeiras entidades JPA aparecerem na Camada 2, validar contra schema vazio vai quebrar a inicialização do Spring. Exige migration `V1` não-vazia ou migrations escritas em paralelo às entidades — não depois. Já cobre a Etapa 2.2 (primeira migration Flyway).

---

## Lições da Etapa 1.5

### Candidatos a hook (automatizar em etapas futuras)

1. Validar que mvnw tem bit de execução no git index antes de empurrar workflow Linux que o use. Comando: git update-index --chmod=+x mvnw. Sem isso, primeiro CI no Linux falha com Permission denied.
2. Validar que comandos em scripts/instruções para Windows não usam ferramentas Unix (tail, head, grep, sed, awk) que não existem no PowerShell. Equivalentes: Select-Object -Last/-First, Select-String.
3. Validar que arquivos .java criados via Out-File no PowerShell usam encoding sem BOM. Out-File -Encoding UTF8 adiciona BOM por default, e javac rejeita arquivos com BOM no início. Alternativas: [System.IO.File]::WriteAllText com UTF8Encoding(false), ou Set-Content -Encoding utf8NoBOM no PowerShell 7+.
4. Validar que toda configuração de branch protection ou required check passou por teste destrutivo (PR proposital com CI falhando, confirmar bloqueio do merge) antes de ser declarada concluída.

### Lições de ambiente

1. Maven Wrapper gerado no Windows não tem bit de execução no git por default. Linux Ubuntu (GitHub Actions) precisa do bit para executar ./mvnw, e o erro é Permission denied (exit code 126). Solução estrutural: git update-index --chmod=+x mvnw. O bit fica registrado no git index, não afeta o arquivo no Windows mas Linux passa a respeitar.
2. Distinção entre flags do Maven Surefire: -DfailIfNoTests=false ignora projeto sem nenhum teste; -Dsurefire.failIfNoSpecifiedTests=false ignora pattern especificado que não casa com nenhum teste. Não são intercambiáveis.
3. actions/upload-artifact@v4 com if-no-files-found tem default warn (não fail). Diretório vazio não quebra o CI por default, mas explicitar o parâmetro torna o comportamento auto-documentado.
4. Cache Maven via cache: maven do actions/setup-java@v4 é suficiente. Não combinar com actions/cache@v4 separado para Maven.
5. Squash merge preserva mudanças de file mode (chmod +x). Confirmação observada: PR #11 mergeou com "mode change 100644 => 100755" preservado em main.
6. Out-File -Encoding UTF8 no PowerShell adiciona BOM no arquivo. javac rejeita arquivos .java com BOM (illegal character: '﻿'). Mesma família de problema do Get-Content -Encoding UTF8 (lição da Etapa 1.4): PowerShell trata UTF-8 com BOM por default em ambas direções.
7. gh pr merge tem flag --admin para bypass de proteções. Comando deve ser tratado como destrutivo e nunca usado em fluxo normal. Branch protection + required check confirmados funcionais por teste destrutivo (PR #12 bloqueado com erro "the base branch policy prohibits the merge").

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

- **2026-05-11** — Sub-etapa 4.9 concluida: primeiro subagent — `pr-reviewer` em `.claude/agents/pr-reviewer.md`, modelo Haiku, tools restritas read-only, invocacao proativa via description. Complementa hooks pre-commit (decisoes/ADRs/logica/testes/docs); nao duplica regras 4.1-4.7. Output Markdown estruturado. Descoberta: subagents em `.claude/agents/` sao flat (sem subpastas) — convencao Claude Code descoberta apos prescricao da 4.0. Estruturas assimetricas: hooks=5 subpastas, agents=3, skills=2. ADR-011 detectou divergencia antes de virar erro. Categoria meta-operacional registrada. Mergeado via PR #50.
- **2026-05-11** — Sub-etapa 4.8 concluida (doc-only): `blueprint-fabrica-ai-native.md` adicionado ao repo. Link em `CLAUDE.md` (secao "Onde buscar mais") passa a apontar para arquivo real. Documento de referencia conceitual fundadora -- define Tier 1/2/3, routines, `/batch`, "modelo Boris Cherny". Mergeado via PR #49.
- **2026-05-11** — Sub-etapa 4.7.1 concluida (doc-only): registro de licoes pos-smoke da 4.7. Debito tecnico da regex do entity-migration + regra "smoke test idiomatico, nao sintetico". Categoria "sub-etapa doc-only de registro pos-smoke falho" consolidada (analoga a 4.2.1). Mergeado via PR #48.
- **2026-05-11** — Sub-etapa 4.7 concluida: sexto hook funcional, segundo de stack. `@Entity` novo (status A) exige migration. Escopo conscientemente reduzido vs licao 2.1 -- modificacao de Entity existente fica como debito explicito. Validacao destrutiva tocou codigo real (Categoria.java) com backup via git restore. Mergeado via PR #47.
- **2026-05-11** — Sub-etapa 4.6 concluida: CLAUDE.md do projeto substituido. Primeira sub-etapa de curadoria. Padrao de atualizacao formalizado. PR #46.
- **2026-05-11** — Sub-etapa 4.5 concluida: quinto hook funcional, primeiro de stack (java-spring). Maven `<release>` ativo via `pre-commit`. `.claude/hooks/java-spring/` ocupada pela primeira vez. Padrao orquestrador agnostico a escopo consolidado. Mergeado via PR #45.
- **2026-05-11** — Sub-etapa 4.4 concluida: quarto hook funcional. Tamanho de docs em modo warn — alerta sem bloquear `.md` em `docs/` com mais de 800 linhas. Lote universal de Markdown fechado. Padrao `warn` vs `fail` para regras subjetivas vs objetivas registrado. Em dash U+2014 em hook `.ps1` quebra parse PS5.1 — ASCII apenas em mensagens. Mergeado via PR #44.
- **2026-05-10** — Sub-etapa 4.3 concluida: terceiro hook funcional. Markdown blank lines ativo via `pre-commit`. Segundo hook no orquestrador 1:N. Primeira sub-etapa a aplicar ADR-011 desde a redacao do prompt. Mergeado via PR #43.
- **2026-05-10** — Sub-etapa 4.2.1 concluida: padroes de validacao destrutiva formalizados em ADR-011. Sub-etapa doc-only. Licao descoberta em smoke test pos-merge da 4.2: `[System.IO.File]::WriteAllText` com path relativo em PowerShell pode gravar em diretorio invisivel ao git. Mergeado via PR #42.
- **2026-05-10** — Sub-etapa 4.2 concluida: segundo hook funcional. Encoding UTF-8 ativo via `pre-commit` hook. Padrao orquestrador 1:N estabelecido (companheiro `pre-commit.ps1` itera sobre array `$hooks` agregando exit codes). Primeira validacao multi-arquivo via `git diff --cached`. Mergeado via PR #41.
- **2026-05-10** — Sub-etapa 4.1 concluida: primeiro hook funcional. Conventional Commits ativo via `commit-msg` hook. Padrao de 3 camadas (entrypoint bash -> companheiro `.ps1` -> hook universal) estabelecido como referencia para sub-etapas seguintes da Camada 3. Mergeado via PR #40.
- **2026-05-10** — Sub-etapa 4.0.1 concluída: fix do `setup.ps1`. Bloco `core.hooksPath` movido para posição que sobrevive a falha de Docker. Lição categorizada como "prescrição insuficientemente específica" (não decisão silenciosa). Débito Docker `container_name:` registrado. Mergeado via PR #39.
- **2026-05-10** — Sub-etapa 4.0 concluída: abertura da Camada 3 com infraestrutura organizacional. Estrutura `.claude/` separada por escopo, `setup.ps1` configura `core.hooksPath`, ADR-009 e ADR-010 registrados, triagem do `hooks-pendentes.md`. Mergeado via PR #38.
- **2026-05-10** — Etapa 3.8 concluída: saldo derivado da Conta. Endpoint GET /api/contas/{id}/saldo, primeiro cruzamento entre bounded contexts via porta no domain, primeira query agregada JPQL. Camada 2 fechada. Mergeado via PR #37.
- **2026-05-09** — Etapa 3.7 concluída: `transacao` ponta a ponta. 5 use cases, controller com paginação e 5 filtros, ~55 testes. Mergeado via PR #36.
- **2026-05-09** — Etapa 3.6 concluída: domain + infra de `transacao`. Entidade com validações cruzadas, V4 com FKs e CHECK constraints, ~40 testes. Mergeado via PR #35.
- **2026-05-09** — Etapa 3.5 concluída: bounded context `categoria` em etapa única. ~50 testes novos. Template estabelecido por `conta` validado como replicável. Mergeado via PR #34.
- **2026-05-09** — Etapa 3.4 concluída: `conta` ponta a ponta. 4 use cases, controller, handler global, 116 testes total (use cases unitários + e2e via MockMvc). Thresholds JaCoCo de `application` e `interfaces` ativados. Mergeado via PR #33.
- **2026-05-09** — Etapa 3.3.1 concluída: fix do `dev.ps1` para ativar profile `dev` (`-Dspring-boot.run.profiles=dev`). Bug descoberto em validação destrutiva manual pós-merge da 3.3. Débito de `application-prod.yml` ausente registrado em `hooks-pendentes.md`. Mergeado via PR #32.
- **2026-05-09** — Etapa 3.3 concluída: infra de `conta` (entity, embeddable, mapper, repository pattern, migration V2, 11 testes integração). MapStruct ativo pela primeira vez. Mergeado via PR #31.
- **2026-05-09** — Etapa 3.2 concluída: domain puro de `conta` (entidade `Conta`, enum `TipoConta`, 28 testes). Mergeado via PR #30.
- **2026-05-09** — Etapa 3.1 concluída: `Money` implementado em `shared/domain`, threshold JaCoCo `domain` 90% ativado. Camada 2 marcada como 🟢 Em andamento. Mergeado via PR #29.
- **2026-05-08** — Etapa 2.9 concluída: `setup.ps1` e `dev.ps1` criam `.env` automaticamente a partir de `.env.example` quando ausente. Débito técnico da Camada 1 (descoberto na 2.8) resolvido. Mergeado via PR #28.
- **2026-05-08** — Etapa 2.8 concluída: wrap-up Camada 1. Auditoria de critérios, retrospectiva criada (`docs/retrospectiva-camada-1.md`), hooks pendentes consolidados (`docs/hooks-pendentes.md`). Camada 1 marcada como ✅ Concluída. Mergeado via PR #27.
- **2026-05-08** — Etapa 2.7 concluída: Next.js 16 inicializado em `frontend/`, dependências adicionais instaladas, shadcn/ui configurado, CI atualizado, decisões e stack registradas. Mergeado via PR #26.
- **2026-05-08** — Etapa 2.6.2 concluída: fix de UX em checagem de Docker nos scripts `.ps1`. Aplicado padrão "suspender `Stop` localmente" em `dev.ps1`/`test-integration.ps1`/`check.ps1`. Regra adicionada em `decisoes.md`. Mergeado via PR #25.
- **2026-05-08** — Etapa 2.6.1 concluída: fix de exit code em scripts `.ps1`. `Write-Error` + `exit 1` substituído por `Write-Host -ForegroundColor Red` + `exit 1` nos 5 scripts afetados. Regra formalizada em `decisoes.md`. Lições registradas. Mergeado via PR #24.
- **2026-05-08** — Etapa 2.6 concluída: 6 scripts PowerShell criados em `scripts/`. README atualizado com tabela de comandos + pré-requisito ExecutionPolicy. Mergeado via PR #23.
- **2026-05-08** — Etapa 2.5 concluída: Checkstyle (`validate`) e SpotBugs (`verify`) integrados como gates obrigatórios do `mvnw verify`. Configuração externa, severidade `error`, validação destrutiva confirmada. Mergeado via PR #22.
- **2026-05-08** — Etapa 2.4 concluída: JaCoCo `check` com thresholds aplicados (BUNDLE 75%, infrastructure 60%), thresholds dos pacotes vazios comentados como TODO Camada 2, validação destrutiva confirmando gate. Mergeado via PR #21.
- **2026-05-08** — Etapa 2.3 concluída: HealthcheckController em `/api/healthcheck`, SecurityConfig com whitelist explícita, HealthcheckControllerTest com 2 testes (status + bloqueio de não-whitelisted). Mergeado via PR #20.
- **2026-05-08** — Etapa 2.2 concluida: V1__schema_inicial.sql aplicada, Flyway configurado em todos os profiles, FlywayMigrationTest validando aplicacao da migration. Mergeado via PR #19.
- **2026-05-08** — Etapa 2.1 concluída: Testcontainers configurado, AbstractIntegrationTest criado, FinancasApplicationTests passa contra Postgres real via container, débito técnico da Etapa 1.5 (exclusão do FinancasApplicationTests no CI) resolvido. Mergeado via PR #17.
- **2026-05-08** — Etapa 1.5 concluída: GitHub Actions CI configurado, branch protection com required check validada destrutivamente (PR #12 bloqueado). Mergeado via PR #11.
- **2026-05-07** — Etapa 1.4 concluída: Spring Boot 3.5.14 + Java 21 inicializado manualmente, pom.xml com toda a stack, Maven Wrapper 3.9.9, JaCoCo configurado (prepare-agent + report). Mergeado via PR #8.
- **2026-05-07** — Etapa 1.3 concluída: docker-compose.yml com Postgres 16 e Redis 7 rodando e validado (8 checks).
- **2026-05-07** — Etapa 1.2 concluída: branch protection ativa via Repository Ruleset após repo se tornar público.
- **2026-05-07** — Etapa 1.1 concluída: critérios marcados. Seção de lições reescrita após revisão para conter apenas o observado na sessão.
- **2026-05-06** — Criação inicial. Camada 0 marcada como concluída. Critérios da Camada 1 detalhados.
