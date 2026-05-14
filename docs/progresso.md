# Progresso — Construção da Fábrica AI-Native

> Documento de tracking. Mostra **onde estamos** na construção da fábrica e do produto.
> Atualizado conforme camadas avançam. Diferente do `decisoes.md` (que registra escolhas) e dos `adrs.md` (que registram porquês), este documento responde a pergunta: "em que ponto eu estou?".

**Última atualização:** 2026-05-14 (Sub-etapa 5.37 -- /plan exibe planejamento e aguarda aprovacao antes de spawnar)

---

## Status geral

| Camada | Descrição | Status |
|---|---|---|
| **0** | Discovery (visão, ADRs, decisões, ambiente) | ✅ Concluída ([historico](progresso-historico.md#camada-0--discovery)) |
| **1** | Infraestrutura de confiança | ✅ Concluída ([historico](progresso-historico.md#camada-1--infraestrutura-de-confianca)) |
| **2** | Arquitetura otimizada para agentes | ✅ Concluída ([historico](progresso-historico.md#camada-2--arquitetura-otimizada-para-agentes)) |
| **3** | Configuração do Claude Code (subagents, skills, hooks) | ✅ Concluída |
| **4** | Modelo operacional (tiers de autonomia ativados) | 🟢 Em andamento |
| **5** | Runtime de agentes (VPS) — opcional | ⏸️ Aguardando |
| **6** | Gestão híbrida Max + API | 🟡 Parcial (configuração API pronta, sem uso) |

**Legenda:** ✅ Concluída | 🟢 Em andamento | 🔵 Próxima | ⏸️ Aguardando | 🟡 Parcial

> Historico detalhado das Camadas 0, 1, 2 (Criterios, decisoes, Licoes) em `progresso-historico.md`.

---

## Camada 3 — Configuração do Claude Code

**Status:** ✅ Concluída
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
- **4.26 -- Split de `decisoes-claude-code.md`** (2026-05-12): terceira aplicacao da
  categoria "manutencao de docs por crescimento" (apos 4.13 e 4.16). Criterio de corte:
  tematico/historico (identico a 4.16 para decisoes.md). Secao 4.0-4.18 arquivada em
  `decisoes-claude-code-historico.md`; sub-etapas 4.19+ permanecem no arquivo ativo.
  `decisoes-claude-code.md` reduz de ~880 para ~171 linhas. CLAUDE.md atualizado com
  link para historico. Debito da 4.25 (hook warn ~880 linhas) resolvido. PR #72.
- **4.25 -- Ampliacao do test-writer para E2E tests** (2026-05-12): terceiro nivel
  de teste no subagent test-writer. Cobre `*/interfaces/*Controller.java` com
  `@AutoConfigureMockMvc` + `MockMvc`, stack completa com Testcontainers (AbstractIntegrationTest).
  Smoke: `/write-test ContaController.java` gerou `ContaControllerTest.java` com testes
  de todos os 5 endpoints; BUILD SUCCESS. Ultima sub-etapa da Camada 3. PR #71.
- **4.24 -- Skill `/migrate` (orquestradora de skills)** (2026-05-12): terceira categoria
  de skill do projeto. Encadeia `/write-migration` (subagent migration-writer) e `/write-test`
  (subagent test-writer) em sequencia para um bounded context. Para se migration falhar, nao
  propaga para testes. Relatorio combinado com status de cada passo. Sem logica propria --
  toda geracao delegada para os subagents ja existentes. Smoke parcial honesto: `/migrate conta`
  validou orquestramento e propagacao de erro (V2 ja existe; /write-test nao foi invocado).
  Happy path aguarda primeiro bounded context novo (Camada 4). PR #70.

- **4.23 -- Subagent migration-writer + skill `/write-migration`** (2026-05-12):
  segundo subagent gerador do projeto (primeiro: test-writer, 4.17). Le `*Entity.java`,
  interpreta anotacoes JPA (`@Column`, `@Id`, `@Embedded`, `@AttributeOverride`,
  `@Enumerated`), descobre proximo numero Flyway via Glob, gera `V<N>__cria_tabela_<name>.sql`.
  Nao gera FK constraints nem indexes (domain-specific). Padrao: subagent Sonnet com
  `context: fork` (mesma categoria do test-writer). Pre-requisito para a 4.24 (`/migrate`
  encadeia migration-writer + test-writer). Smoke parcial honesto: logica de parsing e
  conflito validadas; write path nao exercida por ausencia de bounded context sem migration.
  PR #69.
- **4.22 -- Hook post-edit para unit tests em domain** (2026-05-12): primeiro hook
  nativo do Claude Code no projeto (`PostToolUse`, evento pos-edicao). Dispara apos
  uso de `Edit` ou `Write` em `*/domain/*.java`; roda unit test correspondente via
  `mvnw test -Dtest=<Classe>Test`; silencioso quando sem teste ou arquivo fora do
  escopo. Nunca bloqueia (PostToolUse e non-blocking por design). Configuracao em
  `.claude/settings.json` (gitignored, gerado pelo `setup.ps1`). Script versionado
  em `.claude/hooks/post-edit/run-tests.ps1`. Timeout 60s. Padrao novo: hook nativo
  Claude Code vs git hook (4.1-4.7). PR #68.
- **4.21 -- Skill `/audit` (skill direta sem subagent, terceira aplicacao)** (2026-05-12):
  varredura de padroes em `src/main/java/`. Recebe string ou regex como argumento,
  usa ferramenta Grep do Claude Code, reporta matches agrupados por arquivo com numero
  de linha e trecho. SKILL.md mais enxuto que os anteriores (delega busca ao Grep nativo).
  Smoke: `/audit "@Entity"` retornou 3 matches (ContaEntity, CategoriaEntity,
  TransacaoEntity) -- resultado verificavel e correto. Categoria: "replicacao de padrao
  consolidado" (terceira skill direta). PR #67.
- **4.20 -- Skill `/ship` (skill direta sem subagent, segunda aplicacao)** (2026-05-12):
  segunda skill sem `context: fork` e sem `agent:` do projeto. Orquestra entrega de PR:
  4 verificacoes de seguranca (branch != main, working tree limpa, commits acima de main,
  gh autenticado), gate `check.ps1` (mvn clean verify + Docker), push com `-u origin`,
  criacao de PR via `gh pr create` com titulo extraido do ultimo commit e lista de commits
  da branch no body. Para em qualquer falha, sem push ou PR criado. Smoke natural e
  completo: a propria skill criou o PR #66 da sub-etapa 4.20. Categoria: "replicacao de
  padrao consolidado" (segunda skill direta, mesma estrutura da 4.19). PR #66.
- **4.19 -- Skill `/feature <nome>` (skill geradora direta sem subagent)** (2026-05-12):
  primeira skill sem `context: fork` e sem `agent:` do projeto. Claude Code principal
  segue as instrucoes do `.claude/skills/feature/SKILL.md` diretamente, usando Write +
  Bash. Recebe nome do bounded context em snake_case minusculo; valida formato
  (`^[a-z][a-z0-9_]*$`) e existencia previa; computa PascalCase; cria 4 diretorios +
  11 arquivos Java stub (domain: POJO + Repository + Exception; application: UseCase;
  infrastructure/persistence: Entity + JpaRepository + RepositoryImpl + Mapper;
  interfaces/dto: Controller + 2 DTOs). Esqueleto compila, mas exige migration Flyway
  antes do primeiro commit (hook 4.7). Smoke parcial honesto: verificacao de criacao dos
  11 arquivos + compilacao (`mvn compile`); `mvn verify` fica como responsabilidade do
  desenvolvedor apos criar migration. Categoria: "primeira aplicacao de padrao em eixo
  novo" (mesma da 4.17 -- skill sem subagent vs skill-com-fork). PR #65.
- **4.18 — Ampliacao do `test-writer` para integration tests + revisao da 4.17.1** (2026-05-12): sub-etapa de **ampliacao de subagent por escopo prescrito** (refactor categoria 4.14). A ADR-007 prescreve tres niveis (unit, integration, E2E); a 4.17 entregou apenas unit; a 4.18 completa integration. E2E fica para sub-etapa futura. **Tambem revisa o passo "0" da 4.17.1**: arquivo existente com metodo alvo NAO coberto ganha excecao — subagent ACRESCENTA `@Test` via `Edit` em vez de parar. Auditoria empirica pre-calibracao (aplicando licao da 4.17.1) revelou **gap arquitetural concreto**: 4 queries customizadas em `*JpaRepository.java` sem teste integration (incluindo `calcularTotaisPorConta` com JPQL `CASE WHEN`/`COALESCE` complexa). Cobaia obvia para smoke real, diferente da 4.17 (smoke parcial honesto). Modificacoes no `test-writer.md`: tools ganham `Edit`; description ampliada; identidade reconhece dois niveis; secao "O que voce GERA" reescrita com detecao de nivel por path + regras integration + redirecionamento `JpaRepository -> *Impl`; passo "0" reformulado; exemplo few-shot 4 adicionado; restricoes ajustadas. Nota de revisao adicionada a subsecao 4.17.1 em `decisoes-claude-code.md` (padrao identico a errata 4.10 -> 4.15). **Padrao operacional adotado:** "implementa e roda, ajusta se precisar" (formalizado pelo operador). CLAUDE.md NAO atualizado. PR #63.
- **4.17.1 — Refinamento pos-smoke do `test-writer`: comportamento "arquivo ja existe"** (2026-05-12): segunda aplicacao da categoria "refinamento pos-smoke empirico" (primeira foi a 4.9.1). Smoke da 4.17 (conduzido em `Conta.java`) revelou que o arquivo de teste alvo (`ContaTest.java`) ja existia com cobertura manual cuidadosa; subagent improvisou auditoria minuciosa (output de alta qualidade tecnica, mas comportamento nao-prescrito). Inventario das 11 classes de domain sem teste no projeto confirmou que **nenhuma e cobaia legitima** (interfaces de repositorio, exceptions, enums, records sem logica). Sub-etapa adiciona prescricao explicita: passo "0" no fluxo verificando se arquivo existe; se existe, subagent para, valida via mvnw, reporta com cobertura resumida em max 3 linhas sem bullets, lista 2 opcoes ao operador. Exemplo few-shot 3 ilustra. 2 restricoes novas em "O que NAO fazer". Smoke da 4.17 mantido como **validacao parcial** honestamente (criterio nao trapaceado): componentes funcionam parcialmente, geracao real aguarda primeiro uso na Camada 4. CLAUDE.md NAO atualizado. PR #62.
- **4.17 — Primeiro subagent gerador (`test-writer` Sonnet) + skill `/write-test`** (2026-05-12): terceiro par skill+subagent do projeto, **primeiro gerador** (vs revisores `pr-reviewer` Haiku e `architect-reviewer` Sonnet). Aplica padrao ADR-012 em eixo qualitativamente novo. Subagent em `.claude/agents/test-writer.md` (flat) com modelo Sonnet e tools `Read, Grep, Glob, Bash, Write` (primeiro subagent com `Write`). Escopo focado: **apenas unit tests para classes em `*/domain/`** (subset ADR-007). Integration e E2E ficam para 4.18+ se uso justificar — sub-etapa de refactor (categoria 4.14), nao novos subagents especialistas. Arquitetura C escolhida deliberadamente (1 subagent que cresce por refactor) vs Arquitetura A (3 especialistas) ou B (1 generalista ja com 3 niveis). Regras duras enumeradas no system prompt (JUnit 5, AssertJ, sem Spring, sufixo Test, pacote espelho, mock manual inline). `ContaTest.java` como referencia de estilo. Validacao via `./mvnw test -Dtest=<NomeDoTest>` antes de reportar — **sem loop autonomo de auto-correcao**. Template de output: arquivo + relatorio em 5 secoes (Arquivo gerado, Cobertura, Validacao, Decisoes de design, Limitacoes). Skill `.claude/skills/write-test/SKILL.md` espelho da `review-arch/SKILL.md` com adaptacao. 2 exemplos few-shot (caso happy Conta + caso validacao falhando). Categoria nova: **"primeira aplicacao de padrao em eixo novo"**. Sub-padrao operacional novo (revisor vs gerador) registrado em `decisoes-claude-code.md`. CLAUDE.md NAO atualizado (regra 4.6 — convencao "subagents e skills" ja registrada na 4.11). PR #61.
- **4.16 — Split do `decisoes.md` por tema (Camada 3 vai para arquivo dedicado)** (2026-05-11): segunda aplicacao da categoria "manutencao de docs por crescimento" (consolidada pela 4.13). Critério de corte **tematico** (nao cronologico como na 4.13): decisoes fundacionais (stack/arquitetura/convencoes) ficam em `decisoes.md`; decisoes operacionais de Camada 3 (hooks, subagents, skills, padroes operacionais) movem para novo `decisoes-claude-code.md`. Operacao facilitada por demarcacao H2 `## Camada 3 — Configuracao do Claude Code` ja existente no `decisoes.md` — split foi extracao da H2 inteira. Resultado: `decisoes.md` enxuto (~370-400 linhas); `decisoes-claude-code.md` criado (~460-490 linhas); hook 4.4 deixa de alertar em ambos. CLAUDE.md atualizado em "Onde buscar mais" (quarta atualizacao causadora — regra 4.6). Debito da 4.13 removido de `hooks-pendentes.md`. Padrao operacional consolidado: **criterio de split varia conforme natureza do documento** (`progresso.md` cronologico -> por idade; `decisoes.md` tematico -> por tema). PR #60.
- **4.15 — Errata de auditoria meta-operacional (re-classificacao dos debitos 4.10)** (2026-05-11): sub-etapa documental pura. Auditoria empirica de `~/.claude/` revelou que 2 dos 3 debitos da 4.10 estavam baseados em premissas equivocadas. Re-classificacao: plugins (`code-review`, `frontend-design`) sao oficiais cacheados em `~/.claude/plugins/cache/claude-plugins-official/` -- fora do backlog do projeto; memoria global tem ~85 KB reais (vs ~427 MB de transcripts que a 4.10 confundiu como memoria); built-ins teoricos sem dor pratica observada. Achado novo: 427 MB de transcripts em `~/.claude/projects/` -- fora do escopo do projeto, registrado para visibilidade. Categoria nova: **"errata de auditoria meta-operacional"** -- distinta de auditoria (4.10) e errata de ADR (4.11). Nota de errata adicionada a subsecao 4.10 em `decisoes.md` (padrao identico a ADR-012 / 4.11). CLAUDE.md NAO atualizado (errata nao muda convencao). PR #59.
- **4.14 — Hook 4.4 exclui `docs/prompts/` da verificacao de tamanho** (2026-05-11): sub-etapa de refactor de hook existente — primeira do projeto. Categoria nova: **"ajuste de hook por contexto novo"**. `.claude/hooks/universal/docs-size.ps1` modificado para ignorar `.md` em `docs/prompts/`. Comportamento original (alerta para `.md` em `docs/` >800 linhas) preservado para `docs/` raiz e subpastas futuras que nao sejam `docs/prompts/`. Modo `warn` mantido, limite 800 mantido. Outros hooks que tocam `.md` (encoding 4.2, blank lines 4.3) permanecem ativos em `docs/prompts/`. Resolve debito explicito registrado no PR body da 4.13 ("excluir `docs/prompts/` da verificacao do hook 4.4"). Validacao destrutiva sob ADR-011: 6 cenarios cobrindo comportamento original preservado + comportamento novo. CLAUDE.md NAO atualizado (regra 4.6: refactor de hook nao muda convencao). PR #58.
- **4.13 — Split do `progresso.md` por crescimento + reorganizacao de `docs/prompts/`** (2026-05-11): sub-etapa de manutencao de docs. Categoria nova: **"manutencao de docs por crescimento"**. `progresso.md` cresceu para ~891 linhas — hook 4.4 (modo warn) alertando consistentemente. Camadas 0, 1, 2 (concluidas) + Licoes de etapas 1.X/2.X/3.X movidas para novo arquivo `progresso-historico.md` (~500 linhas). `progresso.md` vivo enxuto (~400 linhas) — Camada 3 em andamento + Camadas 4-6 planejadas + Licoes 4.X + historico de mudancas. `docs/prompt-etapa-*.md` movidos para `docs/prompts/` via `git mv` (preserva historico via rename detection). CLAUDE.md atualizado em "Onde buscar mais" com referencias atualizadas (regra 4.6 — esta sub-etapa e a causadora da convencao "split de docs por crescimento"). Debito explicito registrado em `hooks-pendentes.md`: aplicar split analogo em `decisoes.md` quando cruzar 800 linhas. Smoke 4.12 marcado como concluido (validado pelo operador em PR #35). PR #57.
- **4.12 — Segundo subagent `architect-reviewer` + skill `/review-arch`** (2026-05-11): segunda aplicacao do padrao ADR-012 validado pela 4.11. **Replicacao pura** — sem refinamento de padrao. Subagent em `.claude/agents/architect-reviewer.md` (flat) com modelo **Sonnet** (primeiro Sonnet do projeto — `pr-reviewer` e Haiku). Tools restritas read-only (`Read, Grep, Glob, Bash`). Escopo focado em subset arquitetural duro: ADR-004 (Clean Arch), ADR-005 (JWT), ADR-006 (Flyway), ADR-007 (testes). Demais ADRs cobertos pelo `pr-reviewer` — evita duplicacao. Skill `.claude/skills/review-arch/SKILL.md` espelho da `review-pr` com adaptacao de nome/agent. Template de output identico ao `pr-reviewer` (3 secoes). 2 exemplos few-shot: caso happy (PR que respeita os 4 ADRs) + caso problema (PR violando ADR-004 em multiplos pontos). Categoria nova: **"replicacao de padrao consolidado"**. Bifurcacao explicita entre revisores: `pr-reviewer` cobre o micro, `architect-reviewer` cobre o estrutural — operador escolhe via slash command. CLAUDE.md NAO atualizado (regra 4.6 — convencao ja registrada na 4.11). PR #56.
- **4.11 — Primeira skill orquestradora `/review-pr` + errata ADR-012** (2026-05-11): primeira skill do projeto, primeira implementacao do padrao decidido em ADR-012. `.claude/skills/review-pr/SKILL.md` (flat) com `context: fork` + `agent: pr-reviewer` + `disable-model-invocation: true`. Mecanismo nativo do Claude Code dispara contexto forkado no subagent — sem instrucao textual via Task tool. Errata do ADR-012 anexada: investigacao da doc oficial revelou que o mecanismo literal prescrito originalmente ("skill contem prompt: 'Use a Task tool...'") reproduzia o nao-determinismo que se buscava eliminar. Decisao estrutural preservada, mecanismo refinado. Categoria nova: "errata de ADR baseada em descoberta de documentacao oficial". Pastas orfas `.claude/skills/local/` e `.claude/skills/universal/` removidas — skills sao flat (analogo aos subagents). CLAUDE.md atualizado com subsecao "Subagents e skills" (regra 4.6). 3 licoes novas em progresso.md. PR #55.
- **4.10 — Auditoria de aprendizado meta-operacional + ADR-012** (2026-05-11): sub-etapa doc-only inaugurando categoria **"auditoria meta-operacional"**. Registra 4 descobertas do smoke da 4.9.1: (1) memoria global em `~/.claude/projects/<hash>/memory/` com auto-memory ON; (2) plugins globais afetam comportamento sem aparecer no repo; (3) built-in agents do Claude Code competem com subagents do projeto; (4) heuristica de delegacao proativa via `description` nao e deterministica. Decisao estrutural tomada: **Caminho B — subagents invocados via skill orquestradora dedicada**, formalizada em **ADR-012**. Caminhos A (description imperativa) e C (abandonar subagents) avaliados e rejeitados com justificativa. Criterios de "pronto" da Camada 3 ajustados — cada subagent vem com skill correspondente. `pr-reviewer` (4.9 + 4.9.1) permanece valido — componente funciona, faltava mecanismo de invocacao deterministico. PR #53 (smoke da 4.9.1) fechado sem merge, URL preservada como evidencia. Tres debitos meta-operacionais novos em `hooks-pendentes.md`. CLAUDE.md NAO atualizado — sincronizacao acontece na 4.11 quando primeira skill orquestradora entra. PR #54.
- **4.9.1 — Refinamento do `pr-reviewer` pos-smoke** (2026-05-11): sub-etapa de refactor (branch `refactor/`). Smoke test pos-merge da 4.9 confirmou que subagent funciona — invocado proativamente, le PR, produz output util. Mas output divergiu do template prescrito: 4 secoes (Visao Geral, Analise, Itens Especificos, Conclusao) em vez das 3 (Bloqueadores, Sugestoes, Elogios). Sub-etapa **refina o `.claude/agents/pr-reviewer.md`** em 2 mudancas: (1) template prescritivo ("Voce DEVE usar exatamente as 3 secoes", com lista explicita de secoes proibidas); (2) 2 exemplos few-shot (PR doc-only sem problemas + PR de hook com sugestao real). Haiku mantido — smoke confirmou que problema era do prompt do subagent, nao da capacidade do modelo. Categoria nova **"refinamento de componente baseado em smoke empirico"** — distinta de "registro pos-smoke falho" (smoke da 4.9 passou; refinamento melhora aderencia ao prescrito). PR #52.
- **4.9 — Primeiro subagent: `pr-reviewer` (Haiku)** (2026-05-11): primeiro subagent do projeto. Marco estrutural — Camada 3 do blueprint pede 3-5 subagents focados, este e o primeiro. `.claude/agents/pr-reviewer.md`, modelo Haiku, tools restritas a `Read, Grep, Glob, Bash` (read-only). Invocacao **proativa via `description`** — Claude principal decide quando delegar. Complementa hooks pre-commit: revisa **decisoes de design vs ADRs, coerencia com decisoes.md, logica do codigo, cobertura de testes, documentacao alinhada, padroes do projeto**. Nao duplica verificacoes dos hooks (encoding, blank lines, Conventional Commits, Maven, @Entity, tamanho de docs). Output Markdown estruturado em 3 secoes (Bloqueadores, Sugestoes, Elogios) — operador (humano) decide se cola no PR como comentario. Subagent **nao posta no PR** via `gh pr review` (limite consciente). Descoberta de pre-redacao: convencao Claude Code para subagents e flat em `.claude/agents/*.md`, NAO em subpastas. `.claude/agents/` tem 3 subpastas (`universal/`, `java-spring/`, `local/`) e `.claude/hooks/` tem 5 — estruturas assimetricas; prescricao do prompt assumiu simetria total sem verificar estado real; ADR-011 detectou divergencia na Tarefa 1 antes de virar registro errado. Pasta `.claude/agents/universal/` continua existindo mas nao recebe subagent. PR #50.
- **4.8 — Sub `blueprint-fabrica-ai-native.md` ao repo (doc-only)** (2026-05-11): sub-etapa minimalista. `CLAUDE.md` ja referenciava `docs/blueprint-fabrica-ai-native.md` em "Onde buscar mais" desde a 4.6, mas o arquivo nunca havia sido commitado -- link apontava para arquivo inexistente. Operador tinha copia em `C:\Users\rezen\Downloads\` (644 linhas, UTF-8 sem BOM). Sub-etapa copia o arquivo para `docs/`. Documento de referencia conceitual fundadora -- define vocabulario do projeto (Tier 1/2/3, routines, `/batch`, "modelo Boris Cherny"). 6 headers `###` editados para conformar com hook 4.3 (linhas em branco apos header). Sem mudanca de codigo, sem hook. PR #49.
- **4.7.1 — Registro de licoes pos-smoke da 4.7 (doc-only)** (2026-05-11): sub-etapa doc-only analoga a 4.2.1. Smoke test pos-merge da 4.7 (cenario B) usou Java single-line sintetico que nao casou com a regex `(?m)^\s*@Entity\b` do hook entity-migration. Falso negativo apareceu como falha do hook em producao; diagnostico identificou que o problema era no smoke test, nao no hook. **Duas licoes registradas:** (1) tecnica -- regex do entity-migration e fragil para Java single-line, ajuste para `@Entity\b` fica como debito; (2) operacional -- smoke test pos-merge usa input idiomatico, nao sintetico. Sem mudanca de codigo. PR #48.
- **4.6 — CLAUDE.md do projeto** (2026-05-11): primeira sub-etapa de curadoria (nao codigo). Substitui CLAUDE.md placeholder (criado na Camada 1, 21 linhas) por versao estrutural com 7 secoes (identidade, stack, ambiente, mecanismo de hooks, convencoes, onde buscar mais, o que nao fazer). 95 linhas, ~5KB. Conteudo volatil delegado para `docs/` via links — CLAUDE.md so atualizado em sub-etapas que mudam stack/ambiente/convencoes/restricoes. Validacao via smoke test pos-merge em sessao nova do Claude Code. PR #46.
- **4.5 — Hook Java/Spring de Maven release** (2026-05-11): quinto hook funcional, primeiro de stack. Ativa `.claude/hooks/java-spring/` (vazia desde 4.0). Valida que `pom.xml` no diff staged contem `<release>` (qualquer valor). Modo fail. Padrao consolidado: orquestrador `pre-commit` agnostico a escopo; hook decide aplicabilidade lendo o proprio `git diff --cached`. Hook preventivo — `pom.xml` atual ja cumpre (licao 1.4 aplicada na Camada 1). 6 cenarios destrutivos sob ADR-011. PR #45.
- **4.4 — Hook universal de tamanho de docs (modo warn)** (2026-05-11): quarto hook funcional. Terceiro no orquestrador `pre-commit`. Alerta sobre `.md` em `docs/` com mais de 800 linhas — **nao bloqueia commit**, apenas visibiliza. Estabelece padrao `warn` para regras subjetivas (distinto de `fail` para regras objetivas). Fecha lote universal de Markdown (encoding 4.2 + blank lines 4.3 + tamanho 4.4). 5 cenarios destrutivos sob ADR-011. PR #44.
- **4.3 — Hook universal de Markdown blank lines** (2026-05-10): terceiro hook funcional. Segundo hook no orquestrador `pre-commit` (1:N da 4.2). Valida headers `##`-`######` em arquivos `.md` (qualquer pasta). Fronteira de arquivo e blocos de codigo isentos. Primeira aplicacao de ADR-011 desde a redacao do prompt — 7 cenarios destrutivos com `Test-Path` + `git status` + sincronizacao de `Environment.CurrentDirectory` em cada um. PR #43.

### Criterios de "pronto" (preliminar, ajustados pela 4.10, 4.11, 4.12, 4.13, 4.16, 4.17, 4.17.1 e 4.18)

- [x] `CLAUDE.md` do projeto escrito (target <=15KB) -- concluido 4.6, atualizado 4.11, 4.13 e 4.16
- [x] Padrao skill orquestradora -> subagent decidido -- ADR-012 (4.10, revisado 4.11)
- [x] Subagent `pr-reviewer` (revisao critica antes do PR) -- concluido 4.9 + refinado 4.9.1
- [x] Skill `/review-pr` orquestrando `pr-reviewer` (par com ADR-012) -- concluido 4.11
- [x] Smoke pos-merge da 4.11 validando padrao skill+subagent ponta-a-ponta -- validado em PR #55 + PR #45
- [x] Subagent `architect-reviewer` (Sonnet, valida decisoes contra ADR-004/005/006/007) -- concluido 4.12
- [x] Skill `/review-arch` orquestrando `architect-reviewer` (par com ADR-012) -- concluido 4.12
- [x] Smoke pos-merge da 4.12 validando segundo par skill+subagent -- validado em PR #35
- [x] Subagent `test-writer` + skill `/write-test` (par ADR-012, primeiro gerador) -- concluido 4.17 (escopo: unit tests). **Refinado pela 4.17.1** (comportamento "arquivo ja existe"). **Ampliado pela 4.18 para integration tests** (cobertura ADR-007 parcial -> completa unit + integration; E2E fica para sub-etapa futura).
- [ ] Smoke pos-merge da 4.17 (unit tests) -- **validacao parcial em 2026-05-12** (componentes OK; cobaia tinha teste pre-existente; geracao propriamente dita aguarda primeiro uso real na Camada 4).
- [x] Smoke pos-merge da 4.18 validando integration tests com cobaia real (`calcularTotaisPorConta`) -- concluido 2026-05-12 via PR #64. 11 novos @Test acrescentados (4 para calcularTotaisPorConta + 7 para listarComFiltros), 21/21 passando. Uma assertion corrigida (COALESCE retorna zero, nao null). Fix entregue em PR separado (fix cirurgico de smoke).
- [x] Subagent migration-writer + skill `/write-migration` -- concluido 4.23
- [x] Skill `/feature <nome>` (cria estrutura de bounded context) -- concluido 4.19
- [x] Skill `/ship` (lint + test + build + push + PR) -- concluido 4.20
- [x] Skill `/migrate` (encadeia migration-writer + test-writer) -- concluido 4.24
- [x] Skill `/audit` (varre modulos buscando padrao especifico) -- concluido 4.21
- [x] Ampliacao do `test-writer` para E2E tests (controllers) -- concluido 4.25
- [x] Hook pre-commit funcionando -- concluido 4.1-4.7, refinado 4.14
- [x] Hook post-edit rodando testes do arquivo mexido -- concluido 4.22
- [ ] Decisao sobre plugin `code-review` oficial: nao e debito do projeto (re-classificado 4.15)

---

## Camada 4 — Modelo operacional

**Status:** 🟢 Em andamento
**Pré-requisito:** Camada 3 funcional

### Objetivo

Ativar a fábrica de fato: rodar features no Tier 2, configurar 3 routines Tier 1, validar paralelismo se necessário.

### Sub-etapas concluídas

- **5.37 -- /plan exibe planejamento e aguarda aprovacao antes de spawnar** (2026-05-14):
  Modificacao da skill `/plan` para exibir planejamento detalhado (id, titulo, resumo) antes
  de spawnar executores. Novo campo `resumo` adicionado ao formato JSON do planejador e ao
  registro em `.claude/tasks.json`. Passo 3 substituido por exibicao detalhada + AskUserQuestion
  ("Sim, spawnar" / "Nao, cancelar"); recusa encerra sem spawnar. Antigos Passos 3 e 4
  renumerados para Passos 4 e 5. Default da confirmacao e NAO. PR aberto.

- **5.36 -- babysit-prs detecta BEHIND e atualiza via update-branch** (2026-05-14):
  Adiciona deteccao de `mergeStateStatus == "BEHIND"` ao babysitter de PRs. Tres
  mudancas em `.claude/skills/babysit-prs/SKILL.md`: **(1) Passo 1:** campo
  `mergeStateStatus` adicionado ao payload do `gh pr list` e `gh pr view`.
  **(2) Passo 2a.1:** nova verificacao apos conflito -- se `mergeStateStatus == "BEHIND"`
  e `mergeable != "CONFLICTING"`, dispara Passo 3c (update via API, seguro sem rebase).
  **(3) Passo 3c:** novo bloco que executa `gh pr update-branch <number>`; sucesso
  registra "UPDATE-BRANCH OK", erro e passivo (sem worktree, sem sub-agente).
  **(4) Passo 4:** labels do relatorio atualizadas com `UPDATE-BRANCH OK` e
  `UPDATE-BRANCH FALHOU`. Fluxos 3a (CONFLICTING) e 3b (CI) intactos. PR aberto.

- **5.35 -- ADR-012: registra conflict-resolver e ci-fixer como agents** (2026-05-14):
  Debito tecnico da 5.29 resolvido. Os sub-agentes de resolucao de conflito e de
  auto-fix de CI que estavam definidos inline no `.claude/skills/babysit-prs/SKILL.md`
  foram extraidos para arquivos registrados em `.claude/agents/` conforme ADR-012.
  **(1) `.claude/agents/conflict-resolver.md`:** agente Sonnet com tools Read/Edit/Write/Bash.
  System prompt extraido do babysitter (fluxo 3 passos: entender contexto, resolver cada
  conflito com raciocinio sobre intencao de cada lado, aplicar e continuar o rebase).
  **(2) `.claude/agents/ci-fixer.md`:** agente Sonnet com tools Read/Edit/Write/Bash/Grep/Glob.
  System prompt extraido do babysitter (fluxo 4 passos: entender a falha, abrir worktree,
  validar localmente, commit e push). **(3) `.claude/skills/babysit-prs/SKILL.md`:** blocos
  inline substituidos por instrucoes que leem o arquivo do agente registrado e passam seu
  conteudo como prompt. Logica de resolucao preservada integralmente -- apenas o local
  de definicao do prompt mudou. Conformidade com ADR-012: todo sub-agente do projeto tem
  arquivo de definicao em `.claude/agents/<nome>.md`. PR aberto.

- **5.34 -- housekeeping: permissoes setup.ps1, sintaxe bash nas skills de loop, cleanup worktrees /batch** (2026-05-14):
  Tres debitos acumulados resolvidos juntos. **(1) `scripts/setup.ps1`:** duas permissoes
  novas adicionadas ao template de `.claude/settings.json` -- `"Bash(cd * && git rebase *)"` e
  `"Bash(cd * && GIT_EDITOR=* git rebase *)"` -- garantindo que novos ambientes recebam as
  mesmas permissoes que o ambiente atual ja tem. **(2) Skills de loop (babysit-prs, watch-ci,
  daily-summary):** substituida a linha `$repoRoot = (Get-Location).Path` (sintaxe PowerShell
  que falha no contexto bash do Claude Code) por instrucao de capturar `$repoRoot` via Bash
  tool com `pwd`. **(3) Skill `/batch`:** apos consolidar o relatorio no Passo 3, nova secao
  "Cleanup de branches orfaos" remove branches locais `worktree-agent-*` remanescentes apos
  execucao do Agent tool com `isolation: worktree`. PR aberto.

- **5.33 -- skill /plan -- agente planejador + rastreamento persistente** (2026-05-14):
  Skill `/plan` em `.claude/skills/plan/SKILL.md` com `disable-model-invocation: true`.
  Remove o humano da coordenacao: operador fornece objetivo de alto nivel; skill spawna
  sub-agente planejador que le o projeto, quebra em tasks independentes e paralelizaveis
  com prompts completos para cada executor; registra tasks em `.claude/tasks.json` com
  campos `id`, `planId`, `titulo`, `status`, `branch`, `pr_url`, `created_at`,
  `updated_at`; spawna executores em paralelo (acao atomica); atualiza state apos
  conclusao. Skill `/tasks` em `.claude/skills/tasks/SKILL.md` permite consultar
  estado das tasks entre sessoes, agrupado por `planId`. Estado persistente em
  `.claude/tasks.json` gitignored (adicionado ao `.gitignore`). PR aberto.

- **5.32 -- routine Tier 1 daily-summary** (2026-05-14):
  Terceira routine Tier 1 da fabrica. Skill `/daily-summary` em
  `.claude/skills/daily-summary/SKILL.md` com `disable-model-invocation: true`.
  Logica: verifica estado persistente em `.claude/daily-summary.state`; se menos
  de 20h desde a ultima execucao, reporta tempo restante e agenda proxima
  verificacao sem gerar resumo. Se >= 20h: coleta PRs mergeados hoje (filtro por
  `mergedAt > $agora.Date`), PRs abertos (flag `CONFLICTING`), ultimo run do CI
  em main, commits em main no dia; exibe bloco formatado com separadores `===`;
  salva timestamp em `daily-summary.state`; agenda proxima iteracao via
  `ScheduleWakeup` com `delaySeconds: 3600`. Estado persistente gitignored
  (`.claude/daily-summary.state` adicionado ao `.gitignore`). PR aberto.

- **5.31 -- routine Tier 1 watch-ci -- CI watcher/healer do branch main** (2026-05-14):
  Cria `.claude/skills/watch-ci/SKILL.md`, routine Tier 1 que monitora o CI do
  branch `main` a cada 30 minutos. Complementa o babysitter (5.21+5.30), que cobre
  PRs abertos; o watch-ci cobre o branch principal apos um merge quebrado.
  Fluxo: (1) consulta `gh run list --branch main --limit 1` para obter o run mais
  recente; (2) se `in_progress` ou `queued`: aguarda sem agir; se `success`: limpa
  state file e reporta verde; se `failure`: verifica `.claude/watch-ci.state` para
  evitar acao repetida sobre a mesma falha; se nao processado, salva state, captura
  logs via `gh run view --log-failed` e spawna sub-agente `general-purpose` que cria
  um branch de fix, aplica correcao minima em worktree isolado, valida com `check.ps1`
  ou `check-front.ps1`, commita, faz push e abre PR; (3) exibe relatorio
  `[watch-ci HH:MM]` e agenda proxima iteracao com `ScheduleWakeup` (`delaySeconds: 1800`).
  State file `.claude/watch-ci.state` adicionado ao `.gitignore`. PR aberto.

- **5.30 -- babysitter CI auto-fix via sub-agente** (2026-05-14):
  Passo 3b da skill `/babysit-prs` substituido por acao real de auto-fix.
  Antes: apenas registrava "CI falhou" no relatorio sem agir.
  Agora: (1) Passo 2b captura checks via JSON estruturado (`gh pr checks --json
  name,state,conclusion`) e identifica os falhando; (2) Passo 3b obtém os logs
  do run com falha via `gh run view --log-failed`, spawna sub-agente
  `general-purpose` que age como desenvolvedor senior -- le o log, identifica
  causa raiz (compilacao, testes, lint), aplica correcao minima em worktree
  isolado, valida com `check.ps1` ou `check-front.ps1`, commita e faz push.
  Tenta no maximo 2 vezes. Se a correcao exigir decisao de negocio ou redesign
  arquitetural: reporta "NAO CORRIGIDO: requer intervencao humana" sem abrir
  worktree. Relatorio atualizado com labels `CI AUTO-FIX OK` e
  `CI FALHOU (manual): <motivo>`. PR aberto.

- **5.29 -- babysitter delega resolucao de conflitos para sub-agente inteligente** (2026-05-14):
  Passo 3a da skill `/babysit-prs` reformulado. Em vez de abortar conservadoramente
  qualquer falha de rebase, spawna um sub-agente `general-purpose` que age como
  desenvolvedor senior fazendo merge manual. O sub-agente le cada arquivo em conflito
  na integra, entende a intencao de "ours" (origin/main) e "theirs" (branch do PR),
  e produz uma sintese correta e idiomatica para o tipo de arquivo. Aborta apenas
  diante de contradicao genuina sem resolucao possivel. Se RESOLVIDO: push
  `--force-with-lease` e registra "rebase com resolucao inteligente OK". Se ABORTADO:
  registra o motivo preciso da contradicao. PR aberto.
  **Debito tecnico:** `subagent_type: general-purpose` built-in pode conflitar com
  ADR-012, que prefere subagents registrados em `.claude/agents/` via `context: fork +
  agent: <nome>`. Alternativa: criar `.claude/agents/conflict-resolver.md` com
  frontmatter proprio (modelo, tools restritas) e invocar via Agent tool apontando para
  ele. Decisao do operador: anotar como debito, nao bloquear merge.

- **5.28 -- /batch embute conteudo inline; gitignore docs/prompts/** (2026-05-14):
  resolve problema recorrente de arquivos untracked em `docs/prompts/` bloqueando
  operacoes git do babysitter. **(1) `.gitignore`:** entrada `docs/prompts/` adicionada
  -- arquivos de prompt sao efemeros, conteudo vai inline para o executor via /batch.
  **(2) `/batch` (SKILL.md):** Passo 0 substituido -- em vez de verificar existencia com
  Glob, usa tool Read para ler o arquivo na integra e guarda o conteudo. Template do
  sub-agente reescrito: executor recebe `{CONTEUDO_DO_ARQUIVO}` inline em vez de um
  path `{PATH}` para ler do disco. Label do relatorio final usa o path original como
  `{LABEL}`. **(3) `scripts/setup.ps1`:** bloco de permissoes `git rebase`, `git worktree`,
  `git push --force-with-lease` confirmado presente. **(4) `CLAUDE.md`:** secao "Modelo
  executor" atualizada -- item 4 do Fluxo reformulado para descrever o novo mecanismo
  inline; mencao a "commitar o .md da sub-etapa" removida das convencoes implicitas.
  PR aberto.

- **5.27 -- Orcamentos frontend (listagem, criacao e detalhe com progresso)** (2026-05-14):
  tres paginas Next.js para o bounded context `orcamento`. **(1) Feature layer:**
  `features/orcamentos/types/orcamento.ts` (interfaces Orcamento, Progresso,
  CriarOrcamentoPayload, OrcamentoStatus, ValorMonetario), `services/orcamento-service.ts`
  (listar, buscar, criar, desativar, progresso via apiFetch), `index.ts` com barrel exports.
  **(2) Listagem `/orcamentos`:** tabela (nao cards) com colunas Categoria (nome via
  join client-side), Mes/Ano (MM/YYYY), Limite (formatBRL), Status (badge ativo/inativo),
  Acoes (link Ver). Estado vazio e estado de erro cobertos. **(3) Criacao `/orcamentos/novo`:**
  React Hook Form + Zod, campos categoria (Select carregado de GET /api/categorias, agrupado
  DESPESA primeiro depois RECEITA), valorLimiteValor (input number step=0.01), mesAno
  (input type=month). Payload monta valorLimiteMoeda fixo como "BRL" e concatena "-01" ao
  mesAno. **(4) Detalhe `/orcamentos/[id]`:** duas secoes -- dados do orcamento (categoria,
  mes/ano, limite, badge ativo/inativo, criado em) e progresso do mes (totalGasto,
  percentualUtilizado via componente Progress, badge de status com cores semanticas:
  ABAIXO=verde/default, ATENCAO=secondary, ATINGIDO=outline, EXCEDIDO=destructive).
  Botao Desativar aparece apenas se ativo=true; DELETE /api/orcamentos/{id} redireciona
  para listagem. **(5) Componente Progress:** criado em `shared/components/ui/progress.tsx`
  (div com role=progressbar, calculo de percentagem com clamp 0-100). **(6) Sidebar:**
  link "Orcamentos" adicionado ao layout do dashboard (icone Wallet). **(7) Testes:**
  `orcamento-service.test.ts` gerado (5 casos cobrindo todos os metodos do service).
  `check-front.ps1` verde (76 testes, lint, build). PR aberto.

- **5.26 -- Catalogo de mapeamento tipo-backend para componente-frontend** (2026-05-14):
  criado `docs/field-type-catalog.md` com regras de mapeamento para valores monetarios
  (`BigDecimal` → `<Input type="number" step="0.01">`), datas/timestamps, booleanos,
  FKs (`UUID` → `<Select>` carregado da API), enums, strings com restricoes e valores
  calculados/read-only. CLAUDE.md atualizado com referencia ao catalogo na secao
  `## Frontend`. Regra B7 adicionada ao `front-reviewer` para bloquear violacoes de
  tipo semantico em campos frontend. PR #103.

- **5.25 -- Fix `/batch`: paralelismo e verificacao de arquivos** (2026-05-14):
  dois bugs corrigidos na skill `/batch` (`.claude/skills/batch/SKILL.md`).
  **(1) Bug Test-Path:** Passo 0 usava bloco PowerShell com `Test-Path`, que nao
  existe no contexto bash do Claude Code -- substituido por instrucao de verificacao
  via tool Glob nativo (sem dependencia de shell). **(2) Bug paralelismo serial:**
  instrucao de intencao ("enviar TODOS em UMA UNICA mensagem") era insuficiente --
  o modelo emitia um Agent call por vez. Passo 2 reestruturado com cabecalho
  "acao atomica", remocao explicita da nocao de loop, e secao "ACAO OBRIGATORIA"
  imperativa ao final. Template do sub-agente preservado identico. Passo 1 e Passo 3
  intocados. PR aberto.

- **5.24 -- Gitignore fix + validacao Camada B** (2026-05-14): `.claude/scheduled_tasks.lock`
  adicionado ao `.gitignore` (arquivo criado pelo runtime Claude Code quando `ScheduleWakeup`
  esta ativo; nao faz parte do repositorio). Validacao real da Camada B concluida: `/babysit-prs`
  detectou `mergeable == "CONFLICTING"` em PR #100 apos merge do PR #99 (ambos tocaram
  `docs/progresso.md`); rebase sobre `origin/main` executado via worktree isolado;
  `git push --force-with-lease` OK; operador nao precisou intervir. PR #101.
- **5.23 -- Fix categorias duplicadas no formulario de transacao** (2026-05-13): correcao
  de bug visual em `/transacoes/novo`. O select de categoria exibia entradas duplicadas
  quando o banco continha categorias com nomes identicos criadas em sessoes distintas
  (sem constraint UNIQUE). Correcao no frontend: calculo de `categoriasDoTipo` com
  deduplicacao por nome via `findIndex` (mantendo primeiro ocorrente por tipo); calculo
  de `temDuplicatas` comparando contagem antes/apos deduplicacao; aviso `text-amber-600`
  exibido quando duplicatas detectadas; redundancia `!isTransferencia` no filtro JSX
  interno eliminada (condicao externa ja garante). Apenas
  `frontend/src/app/(dashboard)/transacoes/novo/page.tsx` modificado. 71 testes passando,
  `check-front.ps1` verde. PR aberto.

- **5.22 -- Fix tipo Transacao e NaN na listagem** (2026-05-13): correcao de bug critico
  na pagina `/transacoes`. `TransacaoResponse.java` retorna `valor` como `BigDecimal` plano
  e `moeda` como `String` plana, mas o tipo TypeScript `Transacao` declarava
  `valor: ValorMonetario` (objeto aninhado `{ valor: number, moeda: string }`), causando
  `transacao.valor.valor == undefined == NaN` no `formatBRL`. **(1) Tipo corrigido:**
  `frontend/src/features/transacoes/types/transacao.ts` substituido -- `valor: number` e
  `moeda: string` como campos planos; import de `ValorMonetario` removido; campo
  `atualizadoEm` adicionado (presente no Java mas ausente no tipo anterior).
  **(2) Componente corrigido:** `page.tsx` alterado de `formatBRL(transacao.valor.valor)`
  para `formatBRL(transacao.valor)`. **(3) Testes corrigidos:** mocks em `page.test.tsx`
  atualizados de `valor: { valor: X, moeda: 'BRL' }` para `valor: X, moeda: 'BRL'`.
  71 testes passando, `check-front.ps1` verde (lint + testes + build). PR aberto.

- **5.19 -- Transacoes frontend (listagem e criacao)** (2026-05-13): paginas
  `/transacoes` e `/transacoes/novo` no frontend. **(1) Fix B6:** `CriarTransacaoRequest`
  corrigido no servico -- `valorValor`/`valorMoeda` renomeados para `valor`/`moeda`
  espelhando `TransacaoRequest.java`. **(2) Formatadores:** `formatTipoTransacao` e
  `formatDate` adicionados em `shared/lib/formatters.ts` com testes colocados.
  **(3) Listagem:** cards com badge por tipo (`default`=Receita, `destructive`=Despesa,
  `secondary`=Transferencia), skeleton, estado vazio, aviso se >20 registros.
  **(4) Formulario:** schema Zod espelhando `TransacaoRequest.java` (tipo enum,
  valor coerce.number, moeda length(3), data min(1), descricao min/max, contaId uuid);
  campos condicionais: `contaDestinoId` visivel apenas em TRANSFERENCIA, `categoriaId`
  oculto em TRANSFERENCIA e filtrado pelo tipo da transacao. `check-front.ps1` verde
  (55 testes, lint, build). PR aberto.

- **5.21 -- Skill `/babysit-prs` (loop babysitter de PRs)** (2026-05-13): primeira
  sub-etapa da Camada B da fabrica (modelo Boris Cherny -- "babysitting my PRs").
  Skill direta sem subagent, sem produto. Loop que monitora PRs abertos a cada 10
  minutos via `ScheduleWakeup`. Duas acoes no escopo: **(1) Auto-rebase:** PR com
  `mergeable == "CONFLICTING"` e detectado; worktree isolado criado em
  `.claude/worktrees/babysit-pr-<number>`; rebase sobre `origin/main` executado;
  se sucesso: `git push --force-with-lease` e relatorio "REBASE OK"; se falha:
  `git rebase --abort`, worktree removido, relatorio "REBASE FALHOU (manual)".
  **(2) Monitoramento de CI:** `gh pr checks <number>` detecta checks com `fail`;
  registrado no relatorio sem auto-fix. Estado `UNKNOWN` ignorado (transiente).
  Loop se auto-agenda com `delaySeconds: 600` e `prompt: <<autonomous-loop-dynamic>>`.
  Operador invoca `/babysit-prs` uma vez -- sem necessidade de `--dangerously-skip-permissions`.
  `disable-model-invocation: true` no frontmatter. PR aberto.

- **5.20 -- Skill `/batch` (execucao paralela de tasks)** (2026-05-13): primeira
  sub-etapa de infraestrutura de fabrica da Camada A (modelo Boris Cherny). Skill
  direta sem subagent, sem produto. `/batch etapa-5-18 etapa-5-19` resolve os paths
  dos prompts, valida existencia de cada arquivo, exibe lista de tasks ao operador e
  spawna todos os Agent tool calls em uma unica mensagem (garantindo paralelismo real).
  Cada sub-agente roda em worktree isolado (`isolation: worktree`), le o prompt da task,
  executa todos os passos autonomamente (incluindo /ship manual), e retorna relatorio
  padronizado (Task/Branch/Commits/PR/Reviews/Status). Orquestrador consolida num
  relatorio final com PRs abertos e bloqueadores. `disable-model-invocation: true` no
  frontmatter. Template do prompt do sub-agente copiado literalmente para o SKILL.md
  (sem parafrasear). Sem logica de retry no orquestrador -- responsabilidade do sub-agente.
  Smoke: 5.18 (Categorias) e 5.19 (Transacoes) prontos para execucao pos-merge. PR #94.

- **5.18 -- Categorias frontend (listagem e criacao)** (2026-05-13): duas paginas Next.js
  para o bounded context `categoria`. Pagina `/categorias`: grid de cards com badge
  RECEITA (primary) / DESPESA (destructive), skeleton de carregamento, estado vazio,
  border-l-4 com cor por tipo. Pagina `/categorias/novo`: formulario React Hook Form +
  Zod, campos Nome (min 1 / max 100), Tipo (select RECEITA/DESPESA), Categoria pai
  (select opcional carregado da API -- so exibido quando ha categorias existentes).
  Schema Zod espelha `CriarCategoriaRequest.java` (B6 cumprida). Formatador
  `formatTipoCategoria` adicionado em `shared/lib/formatters.ts`. 54 testes passando,
  `check-front.ps1` verde. PR aberto.

- **5.17 -- Redesign visual Fintech-Clean (polimento)** (2026-05-13): polimento visual
  das paginas existentes sem mudanca de logica. **(1) Fix de fonte:** auto-referencia
  circular `--font-sans: var(--font-sans)` corrigida para `var(--font-geist-sans)` --
  Geist Sans agora aplicado em toda a tipografia. **(2) Login split-layout:** painel
  esquerdo emerald com identidade visual (oculto em mobile) + formulario no painel direito.
  **(3) Sidebar ativo:** `usePathname` + prop `isActive` no `SidebarMenuButton` -- item
  selecionado recebe destaque visual. **(4) Dashboard:** 3 cards (saldo total com
  `border-l-primary` + icone `TrendingUp` + 2 placeholders `border-dashed`). **(5) Contas:**
  `ContaCard` com `border-l-4 border-l-primary` (ativa) ou `border-l-border` (inativa),
  saldo em `text-xl font-bold`. **(6) Botao Desativar:** `variant="outline"` com
  `border-destructive text-destructive` em vez de `variant="destructive"`. **(7) Radius:**
  `--radius` reduzido de 0.625rem para 0.5rem -- visual mais profissional. 53 testes,
  `check-front.ps1` verde. PR aberto.

- **5.16 -- Tema Fintech-Clean + validacao espelhada + B6** (2026-05-13): tres entregas
  visuais e de convencao. **(1) Tema:** paleta Fintech-Clean aplicada em `globals.css`
  (sidebar zinc-900, fundo zinc-50, acento emerald-600, charts em escala emerald).
  Bloco `.dark` preservado sem alteracoes. **(2) Formatadores compartilhados:**
  `shared/lib/formatters.ts` com `formatBRL` (Intl.NumberFormat pt-BR) e `formatTipoConta`
  (labels por enum). Substitui duplicacoes inline nas 3 paginas de contas e no dashboard.
  Teste colocado gerado (5 casos). **(3) Grid e layout consistente:** paginas de listagem
  com `grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`; formularios com `max-w-xl` + botao
  ArrowLeft + `aria-label="Voltar"`. Pagina de detalhe com `dl/dt/dd grid sm:grid-cols-2`.
  Valores monetarios com `tabular-nums` em todos os pontos. **(4) Validacao espelhada:**
  schema Zod de `contas/novo` corrigido: `z.coerce.number()` (com cast `as Resolver<FormValues>`
  para compatibilidade TS), `z.string().length(3).default('BRL')`, espelhando
  `CriarContaRequest.java`. **(5) Regra B6 + convencao CLAUDE.md:** front-reviewer
  recebe B6 (schema Zod divergente do DTO Java e bloqueador); CLAUDE.md ganha
  convencao de validacao espelhada na secao Frontend. 53 testes passando,
  `check-front.ps1` verde. PR a abrir.

- **5.15 -- Reorganizacao feature-first do frontend** (2026-05-13): refactor de estrutura de
  pastas do frontend para o padrao feature-first, espelhando os bounded contexts do backend.
  Arquivos movidos de `src/services/`, `src/types/`, `src/hooks/`, `src/lib/` e `src/components/ui/`
  para `src/features/<dominio>/` e `src/shared/`. Features: auth, contas, categorias, transacoes.
  Barrel exports `index.ts` criados para cada feature. Nenhuma logica de negocio alterada.
  46 testes passando, build verde. Agentes `test-writer` e `front-reviewer` atualizados com
  novos path patterns. CLAUDE.md e ADR-013 documentam a nova estrutura. PR a abrir.

- **5.14 -- CRUD de Contas no frontend** (2026-05-13): primeira feature de dominio no
  frontend. Tres paginas criadas na rota `/contas`: listagem (cards com badge ativa/inativa,
  skeleton, estado vazio), criacao (React Hook Form + Zod 4, select de tipo via base-ui,
  saldo inicial com `z.number()` + `parseFloat` no onChange), detalhe (saldo atual via
  `calcularSaldo`, desativacao com confirmacao inline de dois botoes). Tipos `conta.ts`
  corrigidos para bater com ContaResponse do backend: `TipoConta` atualizado para
  `CORRENTE | POUPANCA | DINHEIRO | CARTAO_CREDITO` (enum Java real), campos `ativa`,
  `saldoInicialValor`, `saldoInicialMoeda` (flat, nao ValorMonetario); `SaldoResponse`
  expandido com todos os campos do backend. Testes Vitest para cada pagina gerados via
  `test-writer`: 46 testes passando (7 arquivos). `check-front.ps1` verde (lint + testes +
  build). Licao: `z.coerce.number()` e `z.string().default()` causam incompatibilidade de
  tipos com `zodResolver` no Zod 4 -- usar `z.number()` com conversao no onChange e sem
  `.default()` no schema (usar apenas em `defaultValues` do `useForm`). PR a abrir.

- **5.13 -- test-writer estendido para frontend (Vitest + Testing Library)** (2026-05-13):
  extensao aditiva do subagent `test-writer` para detectar paths `frontend/` e gerar testes
  Vitest + Testing Library no lugar de JUnit. Tres categorias cobertas: componente
  (`src/app/**/*.tsx`, `src/components/**/*.tsx`), hook (`src/hooks/**/*.ts`) e
  service/utility (`src/services/**/*.ts`, `src/lib/**/*.ts`). Arquivo de teste colocado
  no mesmo diretorio do arquivo alvo (colocated). Deteccao inserida no inicio do fluxo do
  agente (`## Deteccao de frontend` antes da logica Java). Validacao via Push-Location/Pop-Location
  + `npm run test:run` em `frontend/`. Recusa explicitamente se arquivo de teste ja existir.
  Logica Java intacta (extensao e aditiva, nao substitutiva). CLAUDE.md atualizado com
  convencao de `/write-test` para frontend. Smoke C1: recusa de `auth.service.test.ts`
  ja existente confirmada. Smoke C2: `NomeDisplay.tsx` temporario gerou 2 testes + 17/17
  passando. Arquivos temporarios removidos apos validacao. PR a abrir.

- **5.12 -- Agente front-reviewer e skill /review-front** (2026-05-13): revisor de codigo
  frontend especializado nas convencoes do projeto. Agente `.claude/agents/front-reviewer.md`
  (modelo haiku, read-only). 5 bloqueadores (B1 fetch fora de services/, B2 asChild em
  base-nova, B3 URL hardcoded, B4 `any` em tipos de API, B5 credencial literal), 4 sugestoes
  (S1-S4), 3 elogios (E1-E3). Skill `/review-front` invocavel. Review 3 condicional integrado
  ao /ship (Passo 5): so invocado se ha arquivos `frontend/` na branch. Smoke contra PR #84:
  detectou B3 (http://localhost:8080 hardcoded em api-client.ts), S2 (hooks sem testes),
  E2 (apiFetch) e E3 (ApiError) -- validou motivacao e escopo correto do agente. PR aberto.

- **5.11 -- check-front.ps1 e integracao ao /ship** (2026-05-13): gate de qualidade do
  frontend: lint + test:run + build em `frontend/`, fail-fast (mais rapido primeiro). Script
  `scripts/check-front.ps1` standalone com exit 0. Integrado ao /ship como Passo 1.1 condicional:
  so roda se ha arquivos `frontend/` na branch (PRs backend-only nao sao penalizados). 3 cenarios
  de validacao destrutiva: C1 script standalone passou (lint OK, 15 testes OK, build OK), C2 branch
  sem frontend detectou skip corretamente, C3 npm run test:run confirmado com 15 testes passando. PR aberto.

- **5.10 -- Hook secret-scanning pre-commit** (2026-05-13): hook universal que bloqueia
  commits com credenciais literais em codigo-fonte. Modo fail. Extensoes monitoradas:
  `.java`, `.ts`, `.tsx`, `.js`, `.jsx`, `.properties`, `.yml`, `.yaml`, `.json`. Seis
  padroes: P1 chave PEM privada, P2 AWS Access Key ID, P3 GitHub token, P4 OpenAI/Anthropic
  API key, P5 password literal (>= 8 chars), P6 secret/apiKey literal. Exclusoes: `src/test/`
  (senhas de teste esperadas), arquivos `*.example` e `*-example.*`. Placeholders Spring
  (`${...}`) e prefixos de variavel de ambiente (`$`) nao sao bloqueados. 5 cenarios de
  validacao destrutiva: C1 chave PEM bloqueada, C2 password literal bloqueada, C3 placeholder
  Spring passou, C4 arquivo em src/test/ passou, C5 API key em TypeScript bloqueada. PR a abrir.

- **5.9 -- Frontend foundation: Vitest, Storybook, api-client, dashboard** (2026-05-13): fundacao
  de desenvolvimento do frontend Next.js 16. Vitest 4 + Testing Library (jsdom) com globals;
  Storybook 10 com framework `@storybook/nextjs-vite` (compativel com Next.js 16). Estrutura de
  pastas: types/, lib/, services/, providers/, hooks/, components/ui/. api-client.ts com JWT
  automatico em Authorization header; servicos base para auth, contas, categorias, transacoes.
  Providers: QueryProvider (TanStack Query) + AuthProvider (JWT context). Paginas: login (React
  Hook Form + Zod 4 + shadcn), (dashboard)/layout.tsx (sidebar shadcn com protecao de rota),
  (dashboard)/page.tsx (saldo total via TanStack Query). Componentes shadcn/ui 14x instalados
  via `npx shadcn@latest add`; form.tsx implementado manualmente (nao disponivel no base-nova).
  Decisao base-nova: usa @base-ui/react (nao @radix-ui); SidebarMenuButton usa prop `render`
  em vez de `asChild`. Storybook detectou framework nextjs-vite automaticamente. 12 testes,
  BUILD SUCCESS (npm run build), lint limpo. scripts/dev-front.ps1 criado. PR a abrir.
- **5.8 -- Gaps pre-frontend: hierarquia categoria + seed + saldo total** (2026-05-13): tres gaps
  identificados por auditoria antes do frontend. **Gap 1:** hierarquia pai/filho em Categoria --
  campo `categoriaPaiId` (UUID nullable) + FK com ON DELETE SET NULL + validacao na use case
  (so 1 nivel: subcategoria nao pode ter filhos). Novos metodos no repositorio: `listarRaiz()`
  e `listarFilhosDe()`. **Gap 2:** seed de 11 categorias iniciais (8 DESPESA + 3 RECEITA) via
  migration V10 com UUIDs fixos determinísticos. `@BeforeEach` adicionado aos testes de
  categoria para neutralizar o seed. **Gap 3:** `CalcularSaldoTotalUseCase` soma saldos de
  todas as contas ativas; endpoint `GET /api/contas/saldo-total` retorna `{valor, moeda,
  totalContas}`. Nenhum novo bounded context -- todas as mudancas sao extensoes de codigo
  existente. 527 testes, BUILD SUCCESS. 4 commits, PR a abrir.
- **5.7 -- Bounded context `usuario` + autenticacao JWT** (2026-05-13): sexto feature Tier 2.
  Padrao novo: **JWT stateless + BCrypt** -- primeira ocorrencia no projeto.
  `JwtAuthenticationFilter` (`OncePerRequestFilter`) valida Bearer token a cada request.
  `SecurityConfig` configurado para stateless (sem sessao HTTP). `PasswordEncoder` via
  `BCryptPasswordEncoder` bean no `SecurityConfig`. Dois endpoints publicos:
  `POST /api/auth/registrar` e `POST /api/auth/login`; todos os demais `/api/**`
  passam a exigir token valido (retornam 401 sem auth). `AbstractAuthenticatedIntegrationTest`
  criado como base padrao para todos os testes E2E de endpoints protegidos: registra
  usuario de teste, faz login, guarda token, expoe `comAuth()` para envolver requests.
  8 *ControllerTest existentes atualizados para `extends AbstractAuthenticatedIntegrationTest`
  com `comAuth(...)` em cada `mockMvc.perform`. `AuthControllerTest` mantem
  `extends AbstractIntegrationTest` (testa os proprios endpoints de auth, que sao publicos).
  Migration V8 (tabela usuario com constraint unique em email). 4 commits. PR a abrir.
- **5.6 -- Bounded context `importacao`** (2026-05-13): quinto feature Tier 2.
  Padrao novo: **file upload + batch cross-BC** -- primeira ocorrencia no projeto.
  `ImportacaoController` recebe multipart/form-data (`POST /api/importacoes/csv`).
  `ImportarTransacoesCsvUseCase` parseia CSV com `BufferedReader` (sem biblioteca externa)
  e persiste Transacoes via `TransacaoRepository` (cross-BC write, analogo a 5.4).
  Estrategia duas fases: parsing sem DB (detecta erros de formato) + persistencia
  `@Transactional` (detecta erros de constraint). Erros individuais coletados sem abortar
  batch. Sem domain/ proprio, sem infrastructure/, sem migration. 2 commits, 5 arquivos.
  PR #81.
- **5.5 -- Bounded context `relatorio`** (2026-05-13): quarto feature Tier 2.
  Padrao novo: **BC pure-query** -- bounded context sem entidade de dominio, sem migration,
  sem repositorio proprio. So application/ e interfaces/. Injeta repositorios de outros BCs
  (TransacaoRepository, CategoriaRepository) diretamente nos use cases. Dois use cases de
  agregacao: GastosPorCategoriaUseCase (agrupa despesas por categoria, ordena por total desc)
  e EvolucaoSaldoUseCase (receitas/despesas/saldo por mes em intervalo). Dois endpoints GET
  em /api/relatorios/. Registra primeiro padrao de feature de leitura sem persistencia propria.
  PR #79.
- **5.4 -- Bounded context `lancamento-recorrente`** (2026-05-13): terceiro feature Tier 2.
  Recorrencias (despesas/receitas periodicas) com 7 periodicidades (SEMANAL a ANUAL).
  Padrao novo: **cross-BC write** -- ExecutarLancamentoRecorrenteUseCase injeta
  TransacaoRepository e cria uma Transacao de outro bounded context via transacaoRepository.salvar().
  Ate 5.3, cruzamento de BCs era apenas leitura (CalcularProgressoDoOrcamentoUseCase).
  Enum Periodicidade com metodo abstrato calcularProxima() usando anonymous classes.
  5 use cases, 5 endpoints (CRUD + /execucoes sub-recurso). V7 migration. PR #78.
- **5.3 -- Bounded context `meta`** (2026-05-12): segundo feature Tier 2.
  Objetivos financeiros com depositos e acompanhamento de progresso.
  Padrao novo: dois @Embedded MoneyEmbeddable na mesma Entity (valorAlvo + valorAtual).
  StatusMeta enum (EM_ANDAMENTO/CONCLUIDA/CANCELADA). Endpoint POST /depositos (sub-recurso).
  fromDomain() estatico no DTO para conversao inline. 5 endpoints, 4 commits. PR #76.
- **5.2 -- test-writer ampliado para `*/application/*UseCase.java`** (2026-05-12): segunda
  ampliacao do test-writer (apos 4.18 para integration). Deteccao de nivel via path
  `*/application/*UseCase.java`. Regras: JUnit 5 + AssertJ + Mockito.mock(), zero Spring,
  @BeforeEach setUp(), helpers privados. Referencia de estilo: CalcularSaldoDaContaUseCaseTest.
  Smoke: CalcularProgressoDoOrcamentoUseCaseTest gerado (cobre 7+ cenarios incluindo fronteiras
  de StatusProgresso). Lacuna do PR #73 resolvida. PR #75.
- **5.1 -- Bounded context `orcamento`** (2026-05-12): primeiro feature Tier 2 completo.
  Fluxo exercitado: /feature -> domain -> /migrate -> application + infra + interfaces -> /ship.
  CRUD (criar, listar, buscar, desativar) + endpoint de progresso por categoria/mes.
  Cross-bounded-context: CalcularProgressoDoOrcamentoUseCase injeta TransacaoRepository.
  StatusProgresso enum (ABAIXO/ATENCAO/ATINGIDO/EXCEDIDO) inaugurado no domain. 5 endpoints,
  4 commits, ~22 arquivos. Camada 4 inaugurada. PR #73.

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

## Licoes da Sub-etapa 4.18

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — ampliacao de subagent.)

### Licoes de ambiente

1. **Categoria operacional combinada: ampliacao + revisao.** A 4.18 e simultaneamente "ajuste de subagent por contexto novo" (analogo a 4.14 — escopo prescrito pela ADR-007 cumprido parcial pela 4.17, completa agora) **e** revisao da 4.17.1 (excecao "metodo nao coberto"). Padrao operacional: sub-etapas podem combinar categorias quando refactor de componente exige refinamento de prescricao previa. Importante manter os dois registros separados (subsecao 4.18 explica ambos; nota de revisao em 4.17.1 aponta para 4.18; registros originais preservados — padrao identico a errata 4.10 -> 4.15).

2. **Auditoria empirica pre-calibracao revelou gap concreto.** Aplicando licao da 4.17.1 ("auditar antes de calibrar"), foi conduzido inventario PowerShell que revelou 4 queries customizadas em `*JpaRepository.java` sem teste integration. Cobaia obvia (`calcularTotaisPorConta`), gap real, smoke determinavel. Diferente da 4.17 que descobriu **ausencia de cobaia legitima** so no smoke, a 4.18 descobriu **presenca de cobaia legitima** antes da calibracao. **Padrao operacional firmado:** antes de ampliar subagent gerador, auditar projeto empiricamente para confirmar se ha cobaia natural (classe com comportamento real sem teste). Aplicavel a futuras ampliacoes do `test-writer` (E2E) e a outros subagents geradores (`migration-writer`).

3. **Detecao de nivel por path como padrao operacional para subagent que cobre multiplos niveis.** Regras explicitas no system prompt: path-to-level + fallback "fora do escopo conhecido" para paths nao mapeados. Subagent nao improvisa quando path nao casa nenhuma regra — reporta e termina. Padrao replicavel para subagents futuros que cobrem multiplos contextos (ex: `migration-writer` que cobre migrations de criacao vs migrations de alteracao).

4. **Redirecionamento `JpaRepository -> Impl` como convencao do projeto explicita no subagent.** Testes integration de queries customizadas vivem no `*RepositoryImplTest.java` (junto com testes do Impl), nao em arquivo separado. Subagent precisa fazer a traducao implicitamente e reportar no relatorio. Padrao operacional: **convencoes do projeto que afetam decisao de geracao devem estar prescritas no system prompt do subagent, nao deixadas para inferencia.**

5. **Tools dos subagents seguem o que o subagent faz, refinado.** A 4.17 prescreveu "geradores tem `Write`". A 4.18 refina: "geradores que tambem precisam acrescentar a arquivo existente tem `Write` + `Edit`". Padrao "tools por funcao" evolui com necessidade. Aplicavel a `migration-writer` futuro (provavelmente tools = `Read, Grep, Glob, Bash, Write` — migrations criam arquivos novos, raramente editam existentes).

6. **Excecao prescrita ao "arquivo ja existe" formaliza a improvisacao bem-sucedida observada na 4.17.** A 4.17.1 prescreveu "arquivo existe -> pare, reporte". A 4.18 reconhece que **quando metodo alvo NAO esta coberto**, parar e desperdicio — operador quer cobertura, nao ausencia. Excecao formalizada via `Edit` (acrescenta, nao sobrescreve). Padrao operacional: prescricoes podem ser refinadas conforme casos de uso revelam exception. Refinamento subsequente registra a revisao via nota de errata (padrao identico a 4.10 -> 4.15) preservando registro original.

7. **Padrao "implementa e roda, ajusta se precisar" formalizado pelo operador.** Sub-etapas pendentes da Camada 3 (8 itens — `test-writer` integration na 4.18, skills `/feature`, `/ship`, `/migrate`, `/audit`, hook post-edit, `migration-writer` opcional, eventual E2E) serao implementadas em sequencia. Cada uma: entrega + smoke + ajuste minimo se aparecer + proxima sem buscar perfeicao preventiva. Camada 4 (fabrica rodando com features reais) so abre quando todos os 8 itens estiverem entregues — independente de smoke ser "completo" ou "parcial honesto". **Disciplina contra perfeccionismo travador.**

## Licoes da Sub-etapa 4.17.1

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — refinamento de subagent.)

### Licoes de ambiente

1. **Categoria "refinamento pos-smoke empirico" consolidada por dupla aplicacao.** Primeira foi 4.9.1 (refinamento do `pr-reviewer` pos-smoke). Agora 4.17.1 (refinamento do `test-writer` pos-smoke). Padrao operacional: smoke revela borda nao-prescrita; sub-etapa cirurgica X.Y.1 adiciona prescricao explicita sem mudar o resto do componente. Categoria distinta de patch tecnico (4.0.1), ajuste de hook por contexto novo (4.14), errata de auditoria meta-operacional (4.15). **Recomendacao operacional firmada por dupla aplicacao:** quando smoke revela comportamento improvisado bem-sucedido, formalizar a improvisacao em refinamento subsequente — confiar em improvisacao recorrente e risco.

2. **Smoke parcial registrado honestamente: padrao novo.** Em vez de marcar `[x]` (mentira parcial) ou abandonar (perda de info), padrao formalizado: **manter como `[ ]` com nota explicativa** indicando o que foi validado e o que aguarda. Aplicavel a futuros smokes que tropecem em contexto que invalida validacao completa. **Disciplina de registro fiel** e valor por si — agentes em sessoes futuras leem o estado real, nao uma narrativa aspiracional.

3. **Inventario empirico revelou que projeto nao tinha cobaia legitima.** As 11 classes de domain sem teste no `financas-lab` eram boilerplate (interfaces de repositorio, exceptions, enums sem metodos, records sem logica) — nenhuma com comportamento real testavel. Forcar smoke em uma delas teria gerado teste tautologico (validando JVM ou constantes). **Licao operacional consolidada:** antes de calibrar smoke de subagent gerador, **auditar se projeto tem cobaia natural** (classe com comportamento real sem teste manual). Se nao tiver, smoke completo aguarda primeiro uso real — nao forcar cobaia artificial.

4. **Calibracao da 4.17 tinha lacuna que so apareceu no smoke.** Eu (assistente) deveria ter perguntado, antes de calibrar, "existe classe de domain com comportamento real sem teste manual?". Nao perguntei. Lacuna estava em D3 (smoke pos-merge). Padrao operacional registrado: **na calibracao de subagent gerador, incluir explicitamente "existe cobaia natural?" como pergunta antes de prescrever smoke**. Replicavel a futuros subagents geradores (ex: `migration-writer` exigira "existe migration que falta gerar?").

5. **Output da auditoria foi de alta qualidade — Sonnet entrega bem em analise estruturada, independente da funcao formal do subagent.** O `test-writer` improvisou comportamento de revisor e produziu analise minuciosa comparavel ao que `architect-reviewer` produziria. Sinal de que Sonnet tem competencia transversal entre funcoes. Mas **isso reforca a necessidade de prescrever escopo claramente** — sem restricao explicita, subagent pode "ajudar demais" e invadir territorio de outro componente (revisor). Restricao "max 3 linhas, sem bullets" no resumo da 4.17.1 e exatamente para evitar essa invasao.

6. **Anomalia conhecida do `context: fork` apareceu de novo: duplicacao de bullets.** Output do smoke 4.17 teve "equals e hashCode:" repetido com bullet. Mesma anomalia observada em smokes 4.11 e 4.12. Confirmado em **3 smokes diferentes com 3 subagents diferentes** — caracteristica sistematica do mecanismo de invocacao via fork, nao especifica de subagent ou modelo. Aceito como caracteristica conhecida; nao bloqueante. Tentativa de mitigar via instrucao prescritiva mais forte tem baixa probabilidade de sucesso — Claude principal (que emite cabecalho antes do fork) nao e controlado pelo system prompt do subagent.

## Licoes da Sub-etapa 4.17

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — entrega subagent + skill, nao hook.)

### Licoes de ambiente

1. **Subagent gerador como categoria distinta de subagent revisor.** Sub-padrao operacional novo formalizado. Dimensoes que diferenciam: tools (geradores tem `Write`), output (artefato + relatorio vs prosa), validacao (geradores validam via `Bash`), smoke (compila/passa vs tem 3 secoes), risco (medio vs baixo). Registrado como refinamento taxonomico da ADR-012 — **sem ADR novo**, porque a decisao estrutural (skill orquestradora -> subagent dedicado) permanece; apenas a aplicacao varia.

2. **Categoria operacional nova: "primeira aplicacao de padrao em eixo novo".** Distinta de "primeira aplicacao" (4.11, primeira implementacao do padrao recem-decidido) e "replicacao de padrao consolidado" (4.12, segunda aplicacao em caso equivalente). Esta categoria cobre quando o padrao base e preservado mas o eixo de aplicacao muda qualitativamente. Padrao replicavel para futuros casos: aplicar padrao validado em eixo novo (ex: hooks em linguagem diferente, skills em fluxo diferente) sem reabrir decisao estrutural do padrao base.

3. **Arquitetura C (subagent focado que cresce por refactor) escolhida sobre A (3 especialistas) e B (1 generalista).** Principio "infraestrutura segue necessidade" aplicado a desenho de subagent. Vantagem: 4.18 sabera se vale ampliar e como, em vez de decidir no escuro. Risco: system prompt pode crescer demais e justificar split em sub-etapa futura — aceitavel, e refactor consciente quando dor aparecer. Padrao operacional: **subagents iniciam focados e crescem por refactor, nao por proliferacao**.

4. **"Subagent reporta, operador decide" extendido para geradores.** Subagent gera, valida via `./mvnw test`, **reporta resultado sem tentar auto-corrigir em loop**. Se nao compilou ou falhou: reporta erro literal, devolve decisao. Padrao "operador soberano" preservado mesmo em subagent que mexe em arquivos do projeto. Razao: loop autonomo de auto-correcao tem risco de recursao (tenta arrumar, piora, tenta arrumar, piora). Primeiro contato com subagent gerador nao deveria abrir essa porta.

5. **Smoke qualitativamente diferente dos anteriores.** Smokes 4.11 e 4.12 (revisores) validavam "output e texto bem-formatado em 3 secoes ancoradas em ADRs". Smoke 4.17 (gerador) valida "output compila e passa nos testes". Falha aqui e visivelmente quantificavel via `./mvnw test`. Implicacao: smokes de subagents geradores tem **criterio binario forte** (compila/nao compila; passa/nao passa) — sem zona cinza interpretativa que os revisores tem. **Recomendacao operacional registrada:** smokes de subagents geradores futuros devem ser desenhados com criterio binario verificavel via comando do projeto.

6. **Primeiro subagent do projeto com `Write` — decisao consciente.** Geradores precisam escrever; revisores nao. Padrao operacional: **tools dos subagents seguem o que o subagent faz, nao um conjunto fixo**. Subagent revisor com `Write` seria desperdicio e risco. Subagent gerador sem `Write` seria inutil. Aplicavel a futuros subagents (ex: `migration-writer` se vier, tambem precisa `Write`; subagent `security-reviewer` se vier, segue read-only do `pr-reviewer`).

## Licoes da Sub-etapa 4.16

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — manutencao de docs.)

### Licoes de ambiente

1. **Criterio de split varia conforme natureza do documento.** A 4.13 cortou `progresso.md` por idade porque o documento e cronologico (estado evolui no tempo, Camadas concluidas viram historico). A 4.16 cortou `decisoes.md` por tema porque o documento e tematico (decisoes categorizadas por dominio, todas vivas). Padrao operacional: **antes de splittar documento que cruza limite, identificar criterio natural de corte conforme o que o documento e**. Replicar criterio cego (sempre por idade, ou sempre por tema) pode gerar split artificial. Categoria operacional "manutencao de docs por crescimento" cobre ambos os criterios — instrumento e o mesmo, criterio de aplicacao varia.

2. **Demarcacao H2 ja existente facilitou split tematico imensamente.** `decisoes.md` ja tinha `## Camada 3 — Configuracao do Claude Code` delimitando exatamente a regiao a mover. Split foi extracao da H2 inteira (com todas as subsecoes ### filhas) — sem necessidade de identificar bordas conceituais ad-hoc. **Recomendacao operacional:** docs que crescem candidatos a split futuro devem ser estruturados com H2 marcando dominios distintos desde cedo. Em particular, `adrs.md` pode se beneficiar de H2 por dominio (`## Stack`, `## Operacional`, `## Claude Code`) caso cresca a ponto de exigir split.

3. **Auto-referencia em sub-etapa que altera estrutura.** A subsecao "Split do `decisoes.md` por tema (Sub-etapa 4.16)" entrou no `decisoes-claude-code.md` (arquivo splittado), nao no `decisoes.md` vivo. Padrao: **sub-etapa que altera estrutura de doc registra a alteracao dentro da nova estrutura**. Coerente com o conceito ("split de doc X" e decisao operacional de Camada 3 — pertence ao arquivo de decisoes operacionais). Aplicavel a futuros splits e movimentacoes estruturais.

4. **Cadeia "debito originario X -> resolvido em X+N" replicada por terceira vez no projeto.** Padrao consolidado por tripla aplicacao: 4.13 gerou debito do hook 4.4 -> 4.14 resolveu; 4.13 gerou debito do split de `decisoes.md` -> 4.16 resolveu; 4.10 gerou debitos meta-operacionais -> 4.15 (errata) reclassificou. Debitos registrados explicitamente em sub-etapa X **sao naturalmente resolvidos** em sub-etapa X+N quando dor concreta dispara, sem necessidade de planejamento centralizado. Disciplina de registro + atencao a triggers de dor (warn de hook, observacao empirica) basta.

## Licoes da Sub-etapa 4.15

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — errata documental.)

### Licoes de ambiente

1. **Categoria nova: "errata de auditoria meta-operacional".** Distinta de "auditoria meta-operacional" (4.10 -- identifica debitos baseado em observacao inicial) e "errata de ADR baseada em doc oficial" (4.11 -- corrige decisao estrutural via doc oficial). Esta categoria corrige auditoria anterior baseada em **verificacao empirica posterior**. Aplicavel a qualquer caso onde auditoria inicial tinha premissa errada: categoriza fenomeno mal, dimensiona errado, inclui itens fora do escopo. Padrao replicavel.

2. **Auditoria empirica de territorio opaco revelou disparidade de magnitude entre o suposto e o real.** Debito "memoria global" foi registrado pela 4.10 sem dimensionamento -- verificacao empirica mostrou ~85 KB reais. Sem auditoria, mitigacao teria sido baseada em premissa errada. Padrao operacional formalizado: **debitos meta-operacionais registrados sem dimensionamento empirico tem risco alto de descricao inflada**. Recomendacao para auditorias futuras: incluir comando de inspecao de magnitude (`Get-ChildItem -Recurse | Measure-Object -Property Length -Sum` ou equivalente) antes de classificar como debito.

3. **Plugins oficiais cacheados nao sao debito do projeto.** A 4.10 registrou "plugins globais nao-versionados" assumindo que `code-review` e `frontend-design` eram instalacoes manuais. Auditoria mostrou que ficam em `~/.claude/plugins/cache/claude-plugins-official/` -- plugins oficiais distribuidos pelo Claude Code, equivalentes a built-ins. Padrao operacional: **antes de registrar debito sobre "ferramenta X instalada", verificar se foi instalacao deliberada do operador ou parte default do ambiente.** Categoria pratica: itens default do ambiente nao entram no backlog do projeto.

4. **Padrao "auditar antes de mitigar" demonstrado em segunda aplicacao.** Aplicado em 4.13 (dimensionar `progresso.md` antes de cortar -- 891 linhas reais, corte por Camada concluida). Aplicado de novo em 4.15 (auditar `~/.claude/` antes de mitigar -- descobertas que ajustaram escopo da sub-etapa). Em ambos os casos, auditoria empirica revelou informacao que mudou a sub-etapa. **Recomendacao operacional consolidada por dupla aplicacao:** antes de mitigar territorio opaco, auditar primeiro. Custo baixo (3-5 comandos); risco de pular alto (mitigacao errada).

5. **Decisoes sobre `~/.claude/` global ficam fora do escopo do projeto.** A 4.15 mantem-se estritamente documental -- nao modifica auto-memory, nao limpa transcripts, nao desinstala plugins. Decisoes sobre setup pessoal do Claude Code sao do operador, nao do `financas-lab`. Padrao operacional: **sub-etapas do projeto nao prescrevem modificacoes em config global do Claude Code**, mesmo quando observam fenomeno relevante. Se mitigacao em config global virar necessidade, decisao pessoal do operador, nao sub-etapa do projeto.

## Licoes da Sub-etapa 4.14

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — refactor de hook existente.)

### Licoes de ambiente

1. **Categoria nova: "ajuste de hook por contexto novo".** Distinta de "patch tecnico" (4.0.1), "refinamento pos-smoke empirico" (4.9.1), "errata de ADR baseada em doc oficial" (4.11). Hook cumpre regra original; sub-etapa posterior cria contexto onde regra gera falso positivo. Refactor ajusta hook sem reverter intencao. Padrao replicavel para futuros casos analogos: subpasta criada em sub-etapa N gera falso positivo de hook X; sub-etapa N+M ajusta hook X com exclusao especifica.

2. **Primeira modificacao de hook existente no projeto.** Hooks 4.1-4.7 entregaram componentes novos; 4.14 e o primeiro **refactor de hook**. Branch `refactor/` aplicada conforme padrao consolidado (4.9.1). Padrao operacional: hooks podem evoluir conforme contexto do projeto evolui, sem perder cobertura original. Outras dimensoes que podem motivar refactor de hook no futuro: limite numerico (limite 800 pode ser ajustado), modo (warn -> fail ou vice-versa), regex (cobrir mais ou menos casos).

3. **Debito originario de sub-etapa anterior resolvido em sub-etapa subsequente.** PR body da 4.13 registrou debito explicito; 4.14 resolveu. Cadeia "sub-etapa X cria contexto -> X+N resolve debito de contexto" formalizada como padrao operacional. Aplicavel a outros debitos meta-operacionais (4.10): memoria global, plugins, built-in agents. Cada um pode virar sub-etapa dedicada de mitigacao quando aparecer dor real.

4. **Validacao destrutiva em refactor de hook exige cobertura dupla.** Cenarios devem cobrir (a) comportamento original preservado (hook ainda alerta no caso que deveria alertar) e (b) comportamento novo introduzido (hook nao alerta mais no caso isento). Sem (a), refactor pode quebrar cobertura sem detectar. Sem (b), mudanca nao foi exercitada. Padrao a aplicar em refactors futuros de hook.

## Licoes da Sub-etapa 4.13

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — manutencao de docs.)

### Licoes de ambiente

1. **Categoria nova: "manutencao de docs por crescimento".** Distinta de "auditoria meta-operacional" (4.10 — descobertas que afetam estrategia), "errata de ADR" (4.11 — corrige mecanismo literal), "replicacao de padrao consolidado" (4.12 — segunda aplicacao). Manutencao de docs trata **dor de tamanho** — documento cresce a ponto de degradar legibilidade (hook 4.4 alerta consistentemente). Sub-etapa quebra preservando informacao. Padrao replicavel: `decisoes.md`, `adrs.md`, outros docs futuros quando dor real aparecer.

2. **Split por Camada concluida e criterio simples e claro.** Alternativas (cortar por sub-etapa atomica, por tempo) sao mais granulares mas menos coerentes. Padrao operacional: **historico = Camadas concluidas; vivo = Camada em andamento + planejamento + ultimas licoes**. Critericamente claro, facil de comunicar.

3. **`git mv` em batch preserva historico de cada arquivo.** `git mv docs/prompt-etapa-*.md docs/prompts/` (ou equivalente PowerShell) renomeia todos preservando rename detection do git. `blame` continua funcionando, historico nao se perde. Aplicavel em outras reorganizacoes futuras.

4. **CLAUDE.md atualizado pela 3a vez (4.6, 4.11, 4.13) — cada vez por convencao causadora distinta.** Padrao operacional consolidado: CLAUDE.md so muda quando a propria sub-etapa estabelece nova convencao. Aqui: convencao "split de docs por crescimento" + convencao "prompts em `docs/prompts/`".

## Licoes da Sub-etapa 4.12

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — entrega subagent + skill, nao hook.)

### Licoes de ambiente

1. **Categoria nova: "replicacao de padrao consolidado".** Distinta de "descoberta" (4.10 — identifica problema, decide via ADR) e "primeira aplicacao" (4.11 — primeira implementacao, frequentemente refina prescricao). Replicacao reusa forma validada sem refinamento. Padrao operacional: a **terceira aplicacao do mesmo padrao** (ex: 4.13 com `test-writer`) provavelmente revela uma categoria de licao diferente — "consolidacao em escala" — quando padrao passa de 1 caso para N casos sem dor. Replicacao confirma; consolidacao em escala fixa.

2. **Modelo Sonnet para subagent que raciocina sobre estrutura.** `pr-reviewer` (Haiku) verifica logica/padroes superficiais; `architect-reviewer` (Sonnet) verifica camadas/dependencias/abstracoes. Diferenca de capacidade real — Haiku tende a ser superficial em raciocinio arquitetural (Clean Arch exige seguir dependencias entre arquivos, raciocinar sobre direcao de import). Padrao registrado: **Haiku para revisao de superficie, Sonnet para revisao estrutural**. Aplicavel a `security-reviewer`, `performance-reviewer` futuros conforme natureza da revisao.

3. **Bifurcacao explicita entre subagents revisores evita duplicacao.** `pr-reviewer` cobre "coerencia com ADRs ativos" no escopo do PR (verificacao de superficie); `architect-reviewer` cobre "subset arquitetural duro" com analise estrutural. Sem sobreposicao — cada subagent tem escopo mutuamente exclusivo. Operador escolhe via slash command. Padrao "delegacao por especialidade" replicavel em revisores futuros sem retrabalho de forma.

4. **Variante A (revisor de PR) vs Variante B (auditor de codebase) — escolha consciente.** Para architect-reviewer, Variante A escolhida — argumento explicito (numero do PR), escopo controlado, smoke determinavel. Variante B (audita codebase inteiro sem argumento) descartada — output longo, escopo aberto, smoke nao-controlado. Padrao operacional: subagents-revisores comecam como Variante A; auditores de codebase entram como subagents separados (ou skills sem subagent) se aparecer dor real.

## Licoes da Sub-etapa 4.11

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — entrega skill + errata ADR.)

### Licoes de ambiente

1. **Skills sao flat em `.claude/skills/<nome>/SKILL.md`.** Doc oficial do Claude Code (https://code.claude.com/docs/en/skills) confirma: o nome do diretorio direto sob `.claude/skills/` vira o slash command. Subpastas intermediarias nao geram skills validas. A 4.0 criou `.claude/skills/local/` e `.claude/skills/universal/` por intencao de organizacao por escopo (analoga a `.claude/agents/local/` etc.) — pelo padrao oficial, isso nao funciona. Pastas removidas na 4.11. **Estruturas `.claude/{hooks,agents,skills}` sao assimetricas:** hooks=5 subpastas, agents=flat, skills=flat. Cada mecanismo do Claude Code tem convencao propria — simetria entre eles e ilusao da 4.0.

2. **Mecanismo nativo `context: fork` + `agent: <nome>` substitui invocacao textual via Task tool.** A 4.10 prescreveu Modo textual no ADR-012 ("skill contem prompt: 'Use a Task tool...'") sem investigar a doc oficial. Doc oficial mostra Modo nativo via frontmatter: `context: fork` cria contexto isolado, `agent: <nome>` aponta para subagent custom em `.claude/agents/`. Skill content vira prompt do subagent forkado **sem intermediacao** do Claude principal. Determinismo arquitetural — elimina o nao-determinismo que o ADR buscava resolver. Errata anexada ao ADR-012 nesta sub-etapa. Categoria operacional nova: **"errata de ADR baseada em descoberta de documentacao oficial"**.

3. **Leitura previa da documentacao oficial e parte do escopo de prompt-criacao para primeiro componente de um tipo novo.** A 4.0 prescreveu `.claude/skills/` com subpastas sem consultar doc; a 4.9 prescreveu `.claude/agents/` com subpastas sem consultar; ambas viraram debito de cleanup. A 4.11 nasceu refinada porque investigou a doc oficial antes da redacao. Padrao a aplicar: **antes de prescrever primeiro componente de um tipo novo** (primeira skill, primeiro MCP, primeiro hook em settings.json, etc.), ler a doc oficial do mecanismo. Aplicacao retroativa: as 4.0/4.9 nao serao revisadas (pastas removidas; lesao consolidada como historico), mas o padrao vale a partir da 4.11.

## Licoes da Sub-etapa 4.10

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — auditoria doc-only.)

### Licoes de ambiente

1. **Categoria nova: "auditoria meta-operacional".** Diferente de "registro pos-smoke falho" (4.2.1, 4.7.1) e de "refinamento pos-smoke empirico" (4.9.1) — essas afetam 1 componente, esta afeta estrategia de camada. Auditoria meta-operacional registra descobertas + sai com decisao estrutural tomada (nao apenas com licao isolada). Sub-etapa doc-only mas com ADR nascendo junto (precedente: ADR-011 + sub-etapa 4.2.1).

2. **Premissa "subagent invocado proativamente via description" e insuficiente.** Blueprint do projeto (linha 76) avisava: "description vaga = subagent nunca chamado". Smoke da 4.9.1 mostra forma mais forte: **description bem-formada tambem pode nao disparar delegacao** quando Claude principal opta por execucao direta. Heuristica de delegacao e caixa-preta — depende de contexto, humor do principal, plugins globais, built-ins competindo. Padrao a substituir: skill orquestradora dedicada com invocacao via Task tool em texto direto (ADR-012).

3. **Memoria global do Claude Code escreve sem confirmacao.** `~/.claude/projects/<hash>/memory/` recebe `.md` de feedback automaticamente em sessoes de auto-memory ON. Vetor de contaminacao cross-projeto opaco. Mitigacao (desligar auto-memory, auditar 21 .md existentes, politica de retencao) fica como debito em `hooks-pendentes.md`. Padrao operacional a registrar: **auditar `~/.claude/` antes de smoke tests sensiveis** quando determinismo for criterio.

4. **Plugins globais e built-in agents alteram smoke tests sem aparecer no repo.** Smoke da 4.9.1 confirmou comportamento alterado mesmo com plugin `code-review` desabilitado. Built-ins (`Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup`) competem com subagents do projeto. Implicacao: **smoke teste cuja interpretacao depende de "subagent foi invocado?" precisa documentar estado de plugins + built-ins** ou nao tem credibilidade. Investigacao detalhada como debito.

## Licoes da Sub-etapa 4.9.1

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — refactor de subagent.)

### Licoes de ambiente

1. **Categoria nova: "refinamento de componente baseado em smoke empirico".** Diferente de "registro pos-smoke falho" (4.2.1, 4.7.1): aqui o smoke **passou** — componente funciona — mas revelou comportamento subotimo (output divergindo do prescrito). Sub-etapa de refactor refina o componente, nao apenas documenta licao. Padrao operacional para componentes complexos (subagents, skills futuras, MCPs): smoke pos-merge pode ser positivo E gerar sub-etapa de refinamento subsequente. Diferente de hooks, onde smoke ou bloqueia (falha) ou aceita (funciona); subagents tem espectro mais amplo de "funciona mas pode melhorar".

2. **Few-shot prompting > descricao abstrata para modelos menores (Haiku).** Subagent original da 4.9 descrevia template via "ver template abaixo" com codigo Markdown. Subagent improvisou estrutura propria. Refinamento da 4.9.1 adiciona 2 exemplos completos de output — Haiku tende a aderir melhor a estrutura via exemplos concretos vs descricao abstrata. Padrao a aplicar em `architect-reviewer`, `test-writer` e outros subagents Haiku futuros: incluir 1-3 exemplos completos, nao apenas template abstrato.

3. **Tom prescritivo > tom sugestivo em instrucoes para subagents Haiku.** "Voce DEVE usar exatamente as 3 secoes, nesta ordem, sem acrescentar outras" tem aderencia maior que "ver template abaixo". Pode parecer rude na leitura humana, mas modelos menores precisam de instrucoes diretas. Lista explicita do que **nao** fazer ("Nao use 'Visao Geral', 'Analise'...") complementa a prescricao positiva. Padrao a aplicar em outros subagents Haiku.

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

## Histórico de mudanças deste documento

- **2026-05-13** -- Sub-etapa 5.12 concluida: agente front-reviewer + skill /review-front.
  Review 3 condicional no /ship. Smoke validou B3 (URL hardcoded) contra PR #84. PR aberto.
- **2026-05-13** -- Sub-etapa 5.11 concluida: check-front.ps1 + Passo 1.1 condicional no /ship.
  3 cenarios destrutivos validados. PR aberto.
- **2026-05-13** -- Sub-etapa 5.10 concluida: hook secret-scanning pre-commit. 6 padroes
  (PEM, AWS, GitHub, OpenAI, password, secret). 5 cenarios destrutivos validados. PR a abrir.
- **2026-05-12** -- Sub-etapa 5.3 concluida: bounded context `meta`. Segundo Tier 2. PR #76.
- **2026-05-12** -- Sub-etapa 5.2 concluida: test-writer ampliado para application use cases.
  CalcularProgressoDoOrcamentoUseCaseTest gerado. PR #75.
- **2026-05-12** -- Sub-etapa 5.1 concluida: bounded context `orcamento`. Camada 4
  inaugurada. Primeiro feature Tier 2 completo. PR #73.
- **2026-05-12** -- Sub-etapa 4.26 concluida: split de `decisoes-claude-code.md`.
  Historico 4.0-4.18 arquivado. Arquivo ativo reduzido. CLAUDE.md atualizado. PR #72.
- **2026-05-12** -- Sub-etapa 4.25 concluida: test-writer ampliado para E2E tests.
  Terceiro nivel de teste (unit + integration + E2E). Camada 3 fechada. PR #71.
- **2026-05-12** -- Sub-etapa 4.24 concluida: skill `/migrate` orquestradora em
  `.claude/skills/migrate/SKILL.md`. Terceira categoria de skill (orquestradora de skills).
  Encadeia /write-migration + /write-test. CLAUDE.md NAO atualizado. PR #70.
- **2026-05-12** -- Sub-etapa 4.23 concluida: subagent `migration-writer` em
  `.claude/agents/migration-writer.md` + skill `/write-migration` em
  `.claude/skills/write-migration/SKILL.md`. Derivacao SQL de anotacoes JPA.
  Pre-requisito para 4.24. CLAUDE.md NAO atualizado. PR #69.
- **2026-05-12** -- Sub-etapa 4.22 concluida: hook post-edit (`PostToolUse`) para unit
  tests em `*/domain/*.java`. Primeiro hook nativo Claude Code do projeto. `setup.ps1`
  ampliado para gerar `.claude/settings.json`. Script em `.claude/hooks/post-edit/`.
  CLAUDE.md NAO atualizado. PR #68.
- **2026-05-12** -- Sub-etapa 4.21 concluida: skill `/audit` em
  `.claude/skills/audit/SKILL.md`. Terceira skill direta sem subagent. Grep em
  `src/main/java/` com padrao livre. Smoke via `/audit "@Entity"` (3 matches). PR #67.
- **2026-05-12** -- Sub-etapa 4.20 concluida: skill `/ship` em
  `.claude/skills/ship/SKILL.md`. Segunda skill direta sem subagent. Gate integrado
  (check.ps1), 4 verificacoes de seguranca, push + PR automaticos. Smoke natural
  completo (skill criou o proprio PR). Categoria "replicacao de padrao consolidado".
  CLAUDE.md NAO atualizado. PR #66.
- **2026-05-12** -- Sub-etapa 4.19 concluida: skill `/feature <nome>` em
  `.claude/skills/feature/SKILL.md`. Primeira skill sem subagent do projeto. Gera
  estrutura de bounded context (4 dirs + 11 arquivos Java stub). Validacao de
  argumento + existencia previa. Esqueleto compilavel, developer adiciona migration +
  logica. Smoke parcial honesto. Categoria "primeira aplicacao de padrao em eixo novo".
  Smoke 4.18 marcado como concluido (PR #64). CLAUDE.md NAO atualizado (convencao de
  skills ja registrada na 4.11). PR #65.
- **2026-05-12** — Sub-etapa 4.18 concluida (ampliacao de subagent por escopo prescrito + revisao 4.17.1): `test-writer` ampliado para integration tests, cobrindo `*RepositoryImpl` em `*/infrastructure/persistence/` via `AbstractIntegrationTest` (Testcontainers). Detecao de nivel por path; redirecionamento `JpaRepository -> *Impl`. Passo "0" reformulado: excecao "metodo nao coberto" usa `Edit` para acrescentar. Tools ganham `Edit`. Cobaia real: `calcularTotaisPorConta` (gap concreto descoberto na auditoria empirica). Categoria combinada. Padrao "implementa e roda, ajusta se precisar" formalizado. 7 licoes novas. CLAUDE.md NAO atualizado. PR #63.
- **2026-05-12** — Sub-etapa 4.17.1 concluida (refinamento pos-smoke empirico): refinamento do `test-writer.md` com prescricao explicita para "arquivo ja existe". Smoke da 4.17 revelou que cobaia (`Conta.java`) tinha teste pre-existente — subagent improvisou auditoria minuciosa, bem mas nao-prescrita. Sub-etapa adiciona passo "0" no fluxo (verifica existencia antes de gerar; se existe, para, reporta com resumo em max 3 linhas, lista 2 opcoes ao operador). Exemplo few-shot 3 ilustra. 2 restricoes novas. Inventario empirico confirmou que projeto nao tem cobaia legitima (11 classes sem teste sao boilerplate). Smoke 4.17 mantido como **validacao parcial** honestamente — smoke completo aguarda primeiro uso real na Camada 4. Categoria "refinamento pos-smoke empirico" consolidada por dupla aplicacao (apos 4.9.1). 6 licoes novas. PR #62.
- **2026-05-12** — Sub-etapa 4.17 concluida: primeiro subagent gerador (`test-writer` Sonnet, tools com `Write`) + skill `/write-test`. Escopo focado em unit tests para `*/domain/` (subset ADR-007). Arquitetura C escolhida (1 subagent que cresce por refactor vs 3 especialistas ou 1 generalista). Sub-padrao operacional novo: gerador vs revisor (tools, output, validacao, smoke). Validacao via `./mvnw test` antes de reportar — sem loop autonomo de auto-correcao. Categoria nova: "primeira aplicacao de padrao em eixo novo". 6 licoes novas. CLAUDE.md NAO atualizado. PR #61.
- **2026-05-11** — Sub-etapa 4.16 concluida (manutencao de docs por crescimento, criterio tematico): split do `decisoes.md`. Secao `## Camada 3 — Configuracao do Claude Code` (incluindo todas as subsecoes 4.X) movida para novo `docs/decisoes-claude-code.md` (~460-490 linhas). `decisoes.md` enxuto (~370-400 linhas). CLAUDE.md atualizado em "Onde buscar mais" (4a vez causadora — regra 4.6). Debito da 4.13 removido de `hooks-pendentes.md`. Padrao operacional novo: **criterio de split varia conforme natureza do documento**. PR #60.
- **2026-05-11** — Sub-etapa 4.15 concluida (errata documental): re-classificacao dos 3 debitos meta-operacionais da 4.10 apos auditoria empirica de `~/.claude/`. Plugins removidos do backlog (oficiais cacheados, nao instalacoes manuais). Memoria global re-dimensionada (~85 KB real vs ~427 MB de transcripts que a 4.10 confundiu). Built-ins re-classificados como "sob observacao sem dor pratica". Achado novo: 427 MB de transcripts em `~/.claude/projects/` -- fora do escopo do projeto. Categoria nova: "errata de auditoria meta-operacional". Nota de errata na subsecao 4.10 (decisoes.md). CLAUDE.md NAO atualizado. PR #59.
- **2026-05-11** — Sub-etapa 4.14 concluida (refactor): hook 4.4 (`docs-size.ps1`) modificado para excluir `docs/prompts/` da verificacao de tamanho. Resolve debito registrado no PR body da 4.13. Categoria nova: "ajuste de hook por contexto novo". Primeira modificacao de hook existente no projeto. 6 cenarios destrutivos sob ADR-011. CLAUDE.md NAO atualizado. PR #58.
- **2026-05-11** — Sub-etapa 4.13 concluida: split do `progresso.md` por crescimento (~891 -> ~400 linhas). Camadas 0/1/2 + Licoes 1.X/2.X/3.X movidas para `progresso-historico.md`. Reorganizacao: `docs/prompt-etapa-*.md` para `docs/prompts/` via `git mv`. CLAUDE.md atualizado em "Onde buscar mais" (regra 4.6). Debito: split analogo de `decisoes.md` quando cruzar 800 linhas. Categoria nova: "manutencao de docs por crescimento". Smoke 4.12 marcado como concluido. PR #57.
- **2026-05-11** — Sub-etapa 4.12 concluida: segundo subagent `architect-reviewer` (Sonnet, valida ADR-004/005/006/007) + skill `/review-arch`. Replicacao pura do padrao 4.11 — sem refinamento. Categoria nova: "replicacao de padrao consolidado". Padrao Haiku/Sonnet registrado (Haiku para superficie, Sonnet para estrutura). Bifurcacao explicita entre `pr-reviewer` (micro) e `architect-reviewer` (estrutural). CLAUDE.md NAO atualizado (regra 4.6 — convencao ja na 4.11). 4 licoes novas. PR #56.
- **2026-05-11** — Sub-etapa 4.11 concluida: primeira skill orquestradora `/review-pr` em `.claude/skills/review-pr/SKILL.md` (flat) + errata do ADR-012 (mecanismo nativo `context: fork` substitui invocacao textual via Task tool). Pastas orfas `.claude/skills/local/` e `.claude/skills/universal/` removidas. CLAUDE.md atualizado com subsecao "Subagents e skills" (regra 4.6). Categoria nova: "errata de ADR baseada em descoberta de documentacao oficial". 3 licoes novas. PR #55.
- **2026-05-11** — Sub-etapa 4.10 concluida (doc-only): auditoria meta-operacional. Categoria nova inaugurada. Registra 4 descobertas do smoke 4.9.1 + decide Caminho B (skill orquestradora -> subagent) via ADR-012. Criterios de "pronto" da Camada 3 ajustados. PR #53 (smoke 4.9.1) fechado sem merge — URL preservada como evidencia. PR #54.
- **2026-05-11** — Sub-etapa 4.9.1 concluida (refactor): refinamento do `pr-reviewer` pos-smoke. Template prescritivo + 2 exemplos few-shot. Haiku mantido. Categoria nova "refinamento de componente baseado em smoke empirico" — smoke da 4.9 passou (componente funciona) mas revelou divergencia do template prescrito; refactor refina aderencia. Mergeado via PR #52.
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
