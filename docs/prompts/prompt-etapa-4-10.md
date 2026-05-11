# Prompt — Etapa 4.10: Auditoria de aprendizado meta-operacional + ADR-012 (skill orquestradora invoca subagent)

## Contexto

Camada 3 com 6 hooks + CLAUDE.md + blueprint + 1 subagent (`pr-reviewer` Haiku, refinado pela 4.9.1) apos os PRs 38-50 e 52. Main em `675e286`. PR #53 (smoke da 4.9.1) aberto, nao mergeado.

Smoke test pos-merge da 4.9.1 revelou **descoberta estrutural** que afeta toda a Camada 3, nao so o `pr-reviewer`: invocacao proativa de subagent via campo `description` **nao e deterministica** no Claude Code. Pedido "revisa o PR" foi executado pelo Claude principal diretamente (label visual "Skill(review)"), sem delegar ao subagent via Task tool. Comportamento persistiu mesmo com plugin `code-review` desabilitado.

Quatro descobertas meta-operacionais correlatas, ainda nao registradas no repo:

1. **Memoria global do Claude Code em `~/.claude/projects/<hash>/memory/`.** 21 arquivos `.md` de feedback/sub-etapas, auto-memory ON, escreve sem confirmacao. Vetor potencial de contaminacao cross-projeto.
2. **Plugins instalados globalmente afetam comportamento sem aparecer no repo.** Plugins como `code-review` e `frontend-design` (identificados na Camada 0) podem alterar heuristicas do Claude principal mesmo desabilitados localmente.
3. **Built-in agents do Claude Code competem com subagents do projeto.** Cinco built-ins identificados: `Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup`. Quando usuario faz pedido vago, Claude principal pode escolher built-in em vez do subagent do projeto.
4. **Heuristica de delegacao prefere execucao direta em PRs triviais.** Description "Use proactively" e vaga demais para forcar delegacao via Task tool quando o Claude principal julga a tarefa simples.

Esta sub-etapa entrega **auditoria doc-only com decisao estrutural tomada**. Categoria nova: **"auditoria meta-operacional"** — diferente de "registro pos-smoke falho" (1 componente afetado) e "refinamento pos-smoke empirico" (1 componente afetado). Auditoria meta-operacional registra descobertas que afetam **varios componentes ou estrategia de camada**, e sai com decisao tomada (nao apenas com lista de opcoes).

Caracteristicas:

1. **Doc-only puro.** Branch prefixo `docs/`. Sem mudanca em codigo, hooks, subagents, skills.
2. **ADR-012 nasce nesta sub-etapa.** "Subagents do projeto sao invocados via skill orquestradora dedicada." Precedente: ADR-011 nasceu junto com a 4.2.1, no mesmo PR.
3. **Decisao A/B/C tomada e justificada.** A 4.10 sai com **Caminho B** escolhido — skill orquestradora invoca subagent via Task tool. Implementacao da primeira skill orquestradora (`/review-pr`) fica para a 4.11.
4. **Criterios de "pronto" da Camada 3 ajustados.** Cada subagent futuro nasce com skill orquestradora correspondente.
5. **PR #53 fechado sem merge.** Smoke foi descoberta, nao entrega de codigo. URL referenciada na 4.10 como evidencia.
6. **Debitos meta-operacionais registrados.** Auto-memory, plugins globais, built-in agents — investigacoes detalhadas viram debitos em `hooks-pendentes.md`.

Quando esta etapa terminar:

- `docs/adrs.md`: ADR-012 formalizado.
- `docs/decisoes.md`: subsecao "Auditoria meta-operacional (Sub-etapa 4.10)" registra as 4 descobertas + decisao Caminho B.
- `docs/progresso.md`: sub-etapa 4.10 + 3-4 licoes + criterios de Camada 3 ajustados + entrada de historico.
- `docs/hooks-pendentes.md`: 3 debitos meta-operacionais novos.
- `docs/prompt-etapa-4-10.md`: prompt versionado.
- PR #53 fechado com comentario explicativo.

## Padroes que estreiam nesta etapa

1. **Categoria nova: "auditoria meta-operacional".** Diferente de:
   - "Registro pos-smoke falho" (4.2.1, 4.7.1): smoke de 1 componente falhou; sub-etapa doc-only.
   - "Refinamento pos-smoke empirico" (4.9.1): smoke de 1 componente passou mas revelou comportamento suboptimo.
   - **Auditoria meta-operacional:** descobertas afetam varios componentes ou a estrategia de uma camada. Sai com decisao estrutural tomada, nao so com licao registrada.

2. **ADR nascendo junto com sub-etapa doc-only.** Precedente: ADR-011 + sub-etapa 4.2.1 no mesmo PR. Aplicado aqui para ADR-012 + sub-etapa 4.10.

3. **Padrao skill orquestradora -> subagent.** Subagents do projeto sao invocados via slash command de skill dedicada, nao por heuristica de delegacao proativa do Claude principal. Detalhes em ADR-012.

4. **Sub-etapa fecha PR de smoke aberto.** PR #53 fica encerrado sem merge com comentario referenciando a 4.10. Smoke e instrumento de descoberta, nao codigo de producao.

## Escopo decidido (calibrado com operador antes da redacao via D1-D6)

### Arquivos modificados

| Arquivo | Tipo de mudanca |
|---|---|
| `docs/adrs.md` | ADR-012 adicionado ao final |
| `docs/decisoes.md` | Subsecao "Auditoria meta-operacional (Sub-etapa 4.10)" antes de "Claude Code hooks nativos" |
| `docs/progresso.md` | Sub-etapa 4.10 + licoes + criterios Camada 3 ajustados + historico |
| `docs/hooks-pendentes.md` | 3 debitos meta-operacionais novos |
| `docs/prompt-etapa-4-10.md` | Versionado (novo arquivo) |

**Nao tocados:** `.claude/agents/pr-reviewer.md`, `.claude/hooks/`, `.githooks/`, `pom.xml`, `src/`, `frontend/`, `CLAUDE.md`, `.gitignore`, `.gitattributes`, blueprint.

### Conteudo do ADR-012 (adicionar ao final de `docs/adrs.md`)

```markdown
## ADR-012 — Subagents do projeto invocados via skill orquestradora

**Status:** Aceito
**Data:** 2026-05-11

### Contexto

Camada 3 do blueprint do projeto prescreve 3-5 subagents focados, invocados proativamente pelo Claude principal via campo `description` no frontmatter. Sub-etapas 4.9 e 4.9.1 entregaram o primeiro subagent (`pr-reviewer`, Haiku, tools read-only). Smoke test pos-merge da 4.9.1 revelou que **invocacao proativa via `description` nao e deterministica**.

Pedido "revisa este PR" foi executado pelo Claude principal direto (label visual "Skill(review)"), sem invocar o subagent via Task tool. Comportamento persistiu mesmo apos desabilitar o plugin global `code-review`. O subagent existe, esta bem-formado (template prescritivo + 2 exemplos few-shot apos a 4.9.1), e funcionou quando invocado explicitamente — mas o Claude principal nao o chamou.

Investigacao identificou quatro fatores que contribuem para o nao-determinismo:

1. **Memoria global do Claude Code em `~/.claude/projects/<hash>/memory/`** — 21 arquivos `.md` de feedback/sub-etapas, auto-memory ON, escreve sem confirmacao. Influencia heuristica de delegacao de formas opacas.
2. **Plugins globais nao-versionados** (`code-review`, `frontend-design`) alteram comportamento do Claude principal mesmo desabilitados localmente.
3. **Built-in agents do Claude Code** (`Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup`) competem com subagents do projeto no espaco de delegacao.
4. **Heuristica de delegacao** prefere execucao direta em tarefas que o Claude principal julga simples. Description "Use proactively" e instrucao fraca diante de pressao por simplicidade.

O blueprint do projeto (linha 76) ja avisava: *"O ponto critico e o campo `description`: o Claude principal decide quando delegar baseado nele. Description vaga = subagent nunca chamado."* A descoberta empirica e mais forte: **description bem-formada tambem pode nao disparar delegacao** quando o Claude principal opta por execucao direta. A premissa "subagents invocados proativamente via description" e insuficiente.

Esta nao e falha do `pr-reviewer` em particular. E limite arquitetural que afeta toda a Camada 3, e portanto a estrategia inteira de uso de subagents no projeto.

### Decisao

**Subagents do projeto sao invocados via skill orquestradora dedicada.** Cada subagent tem uma skill (`.claude/skills/<escopo>/<nome>.md`) que, quando invocada pelo operador via slash command, instrui o Claude principal a delegar ao subagent via Task tool.

**Mecanismo:**

1. Operador invoca a skill explicitamente (ex: `/review-pr <numero>`).
2. A skill contem prompt direto: "Use a ferramenta Task para invocar o subagent `<nome>`. Repasse o input completo conforme o template."
3. Claude principal executa a Task tool, que dispara o subagent.
4. Subagent roda em contexto isolado (gatekeeping de contexto), com modelo barato e tools restritas conforme frontmatter.
5. Output do subagent retorna para o Claude principal, que apresenta ao operador.

**Nao mecanismo:**

- Invocacao por heuristica de delegacao proativa via campo `description` e considerada **nao-determinismo arquitetural** e nao e mecanismo primario.
- O campo `description` continua existindo nos subagents e e usado como documentacao + fallback, mas nao e a porta de entrada esperada.

**Padroes obrigatorios:**

1. **Todo subagent do projeto tem skill orquestradora correspondente.** Subagent sem skill e nao-acessivel deterministicamente — entra como debito ou e considerado nao-pronto.
2. **A skill prescreve invocacao via Task tool em texto direto.** Tom imperativo ("Use a Task tool...", "Invoque o subagent..."), nao sugestivo.
3. **A skill carrega contexto/input do operador.** Slash command pode receber argumentos (ex: numero do PR) que a skill repassa ao subagent via prompt.
4. **Smoke test do par skill+subagent** valida o caminho ponta-a-ponta: invocacao da skill -> Task tool disparada -> subagent executado -> output retornado.

### Alternativas consideradas

- **Caminho A — Description imperativa.** Editar `description` do subagent para tom imperativo ("ALWAYS delegate via Task tool"). Rejeitada: continua dependendo de heuristica do Claude principal "ler" a description e respeitar. Caixa-preta. Mesmo se "passar" em smoke, atribuicao causal e fraca — nao se sabe se foi o "ALWAYS" ou outro fator. Nao escala para `architect-reviewer`, `test-writer` futuros.
- **Caminho C — Re-pensar Camada 3 sem subagents.** Aceitar que invocacao proativa nao funciona e abandonar subagents em favor de CLAUDE.md + hooks + skills (linha 87 do blueprint cita esse padrao como "80% do ganho"). Rejeitada: descarta valor real do `pr-reviewer` (tools restritas read-only, gatekeeping de contexto, modelo barato) baseado em N=1. Decisao grande demais para a evidencia atual.
- **Status quo** (manter description proativa e esperar mais amostras). Rejeitada: descobertas meta-operacionais (memoria global, plugins, built-ins) tornam smoke tests futuros nao-confiaveis sem mitigacao previa. Adiar e acumular custo opaco.

### Consequencias

**Aceitas:**

- Operador invoca subagent explicitamente via slash command, nao "Claude principal" mediando.
- Cada subagent custa 2 componentes (subagent + skill), nao 1.
- Camada 3 ganha 1 criterio de "pronto" novo: padrao skill orquestradora validado com smoke.
- `pr-reviewer` (4.9 + 4.9.1) **mantem-se valido** — o componente esta correto, so faltava o mecanismo de invocacao deterministico.

**Ganhos:**

- **Determinismo da invocacao.** Slash command e ato explicito do operador.
- **Determinismo da delegacao.** Skill prescreve Task tool em texto direto.
- **Preservacao de subagent como ferramenta.** Tools restritas, modelo barato, contexto isolado continuam valendo.
- **Padrao escalavel.** `architect-reviewer`, `test-writer`, `migration-writer` futuros nascem com skill correspondente.
- **Ensinavel.** Regra simples: "subagent sempre vem com skill". Sem zona limitrofe.

**Custos reconhecidos:**

- Plugin `code-review` (decidir manter, desativar ou reaproveitar — criterio de pronto da Camada 3) continua aberto. Independente do ADR-012.
- Investigacao dos built-in agents (o que `Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup` realmente fazem) fica como debito em `hooks-pendentes.md`.
- Risco residual: skill pode tambem nao invocar Task tool deterministicamente. Smoke da 4.11 (primeira skill orquestradora) valida.
```

### Conteudo da subsecao em `docs/decisoes.md`

Inserir **antes** da subsecao `### Claude Code hooks nativos` (linha 570 do estado atual):

```markdown
### Auditoria meta-operacional (Sub-etapa 4.10)

Sub-etapa doc-only que inaugura categoria "auditoria meta-operacional" — diferente de "registro pos-smoke falho" (4.2.1, 4.7.1) e de "refinamento pos-smoke empirico" (4.9.1), pois afeta varios componentes / estrategia de camada, nao 1 componente.

**Quatro descobertas registradas:**

1. **Memoria global do Claude Code em `~/.claude/projects/<hash>/memory/`.** 21 arquivos `.md` de feedback/sub-etapas, auto-memory ON, escreve sem confirmacao. Vetor de contaminacao cross-projeto opaco. Mitigacao detalhada (desligar auto-memory? auditar conteudo? versionar politica de retencao?) registrada como debito em `hooks-pendentes.md`.

2. **Plugins instalados globalmente afetam comportamento sem aparecer no repo.** `code-review`, `frontend-design` (identificados na Camada 0) alteram heuristicas do Claude principal mesmo desabilitados localmente. Smoke da 4.9.1 confirmou comportamento alterado com plugin desabilitado.

3. **Built-in agents do Claude Code competem com subagents do projeto.** Cinco built-ins identificados nominalmente: `Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup`. Investigacao do que cada um faz e quando dispara fica como debito.

4. **Heuristica de delegacao proativa via `description` nao e deterministica.** Smoke da 4.9.1 mostrou que description bem-formada e tom prescritivo (4.9.1 endureceu o subagent) nao garantem invocacao via Task tool. Claude principal optou por execucao direta em PR trivial. Premissa do blueprint (linha 76) precisa de reinterpretacao: description ajuda mas nao garante.

**Decisao estrutural tomada:** Caminho B — **subagents do projeto sao invocados via skill orquestradora dedicada.** Formalizado em ADR-012.

**Alternativas avaliadas e rejeitadas:**

- **Caminho A** (description imperativa "ALWAYS delegate"): palpite sem evidencia, continua dependendo de heuristica caixa-preta.
- **Caminho C** (re-pensar Camada 3 sem subagents): descarta `pr-reviewer` baseado em N=1.

**Implicacoes para a Camada 3:**

- Criterios de "pronto" ajustados em `progresso.md`: cada subagent vem com skill orquestradora correspondente.
- `pr-reviewer` (4.9 + 4.9.1) **permanece valido**. Smoke confirmou que o componente funciona quando invocado.
- Sub-etapa 4.11 implementa primeira skill orquestradora (`/review-pr` invocando `pr-reviewer`). Smoke da 4.11 valida o padrao ADR-012 ponta-a-ponta.
- Subagents futuros (`architect-reviewer`, `test-writer`, `migration-writer`) nascem com skill correspondente — nunca isolados.

**Evidencia empirica:** PR #53 (smoke da 4.9.1) e a evidencia que gerou esta auditoria. PR fechado sem merge — smoke e descoberta, nao codigo de producao. URL preservada no historico para referencia.

**CLAUDE.md NAO atualizado nesta sub-etapa.** Regra 4.6: CLAUDE.md sincronizado com sub-etapa causadora. A 4.10 decide a convencao; a 4.11 implementa a primeira skill e atualiza CLAUDE.md com a convencao em uso.
```

### Conteudo das edicoes em `docs/progresso.md`

**Tres edicoes** neste arquivo:

**Edicao 1 — Adicionar sub-etapa 4.10 ao topo da lista "Sub-etapas concluidas" da Camada 3** (entre o titulo da secao e o item da 4.0):

```markdown
- **4.10 — Auditoria de aprendizado meta-operacional + ADR-012** (2026-05-11): sub-etapa doc-only inaugurando categoria **"auditoria meta-operacional"**. Registra 4 descobertas do smoke da 4.9.1: (1) memoria global em `~/.claude/projects/<hash>/memory/` com auto-memory ON; (2) plugins globais afetam comportamento sem aparecer no repo; (3) built-in agents do Claude Code competem com subagents do projeto; (4) heuristica de delegacao proativa via `description` nao e deterministica. Decisao estrutural tomada: **Caminho B — subagents invocados via skill orquestradora dedicada**, formalizada em **ADR-012**. Caminhos A (description imperativa) e C (abandonar subagents) avaliados e rejeitados com justificativa. Criterios de "pronto" da Camada 3 ajustados — cada subagent vem com skill correspondente. `pr-reviewer` (4.9 + 4.9.1) permanece valido — componente funciona, faltava mecanismo de invocacao deterministico. PR #53 (smoke da 4.9.1) fechado sem merge, URL preservada como evidencia. Tres debitos meta-operacionais novos em `hooks-pendentes.md`. CLAUDE.md NAO atualizado — sincronizacao acontece na 4.11 quando primeira skill orquestradora entra. PR #XX.
```

**Edicao 2 — Reescrever lista de criterios de "pronto" da Camada 3** (substituir bloco atual em linhas ~170-183):

```markdown
### Criterios de "pronto" (preliminar, ajustados pela 4.10)

- [x] `CLAUDE.md` do projeto escrito (target <=15KB) — concluido 4.6
- [x] Padrao skill orquestradora -> subagent decidido — ADR-012 (4.10)
- [x] Subagent `pr-reviewer` (revisao critica antes do PR) — concluido 4.9 + refinado 4.9.1
- [ ] Skill `/review-pr` orquestrando `pr-reviewer` (par com ADR-012, smoke valida padrao ponta-a-ponta)
- [ ] Subagent `architect-reviewer` + skill `/review-arch` (par ADR-012)
- [ ] Subagent `test-writer` + skill `/write-test` (par ADR-012)
- [ ] Subagent (opcional) `migration-writer` + skill `/write-migration` (par ADR-012)
- [ ] Skill `/feature <nome>` (cria estrutura de bounded context)
- [ ] Skill `/ship` (lint + test + build + push + PR)
- [ ] Skill `/migrate` (gera migration + atualiza schema + escreve teste)
- [ ] Skill `/audit` (varre modulos buscando padrao especifico)
- [x] Hook pre-commit funcionando — concluido 4.1-4.7
- [ ] Hook post-edit rodando testes do arquivo mexido
- [ ] Decisao sobre plugin `code-review` oficial: manter, desativar ou reaproveitar?
```

**Edicao 3 — Adicionar bloco de "Licoes da Sub-etapa 4.10"** (entre as licoes da 4.9.1 e as da 4.8 — ordem cronologica descrescente):

```markdown
## Licoes da Sub-etapa 4.10

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — auditoria doc-only.)

### Licoes de ambiente

1. **Categoria nova: "auditoria meta-operacional".** Diferente de "registro pos-smoke falho" (4.2.1, 4.7.1) e de "refinamento pos-smoke empirico" (4.9.1) — essas afetam 1 componente, esta afeta estrategia de camada. Auditoria meta-operacional registra descobertas + sai com decisao estrutural tomada (nao apenas com licao isolada). Sub-etapa doc-only mas com ADR nascendo junto (precedente: ADR-011 + sub-etapa 4.2.1).

2. **Premissa "subagent invocado proativamente via description" e insuficiente.** Blueprint do projeto (linha 76) avisava: "description vaga = subagent nunca chamado". Smoke da 4.9.1 mostra forma mais forte: **description bem-formada tambem pode nao disparar delegacao** quando Claude principal opta por execucao direta. Heuristica de delegacao e caixa-preta — depende de contexto, humor do principal, plugins globais, built-ins competindo. Padrao a substituir: skill orquestradora dedicada com invocacao via Task tool em texto direto (ADR-012).

3. **Memoria global do Claude Code escreve sem confirmacao.** `~/.claude/projects/<hash>/memory/` recebe `.md` de feedback automaticamente em sessoes de auto-memory ON. Vetor de contaminacao cross-projeto opaco. Mitigacao (desligar auto-memory, auditar 21 .md existentes, politica de retencao) fica como debito em `hooks-pendentes.md`. Padrao operacional a registrar: **auditar `~/.claude/` antes de smoke tests sensiveis** quando determinismo for criterio.

4. **Plugins globais e built-in agents alteram smoke tests sem aparecer no repo.** Smoke da 4.9.1 confirmou comportamento alterado mesmo com plugin `code-review` desabilitado. Built-ins (`Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup`) competem com subagents do projeto. Implicacao: **smoke teste cuja interpretacao depende de "subagent foi invocado?" precisa documentar estado de plugins + built-ins** ou nao tem credibilidade. Investigacao detalhada como debito.
```

**Edicao 4 — Adicionar linha no historico** (topo do "Historico de mudancas deste documento"):

```markdown
- **2026-05-11** — Sub-etapa 4.10 concluida (doc-only): auditoria meta-operacional. Categoria nova inaugurada. Registra 4 descobertas do smoke 4.9.1 + decide Caminho B (skill orquestradora -> subagent) via ADR-012. Criterios de "pronto" da Camada 3 ajustados. PR #53 (smoke 4.9.1) fechado sem merge — URL preservada como evidencia. PR #XX.
```

### Conteudo dos debitos em `docs/hooks-pendentes.md`

Adicionar **nova secao** "Debitos meta-operacionais" **antes** da secao "Hooks implementados" (linha 114 do estado atual):

```markdown
## Debitos meta-operacionais

Itens descobertos na Sub-etapa 4.10 (auditoria meta-operacional). Nao sao hooks tradicionais — sao investigacoes/mitigacoes do ambiente Claude Code que afetam credibilidade de smoke tests e determinismo de invocacao. Categoria "descoberta a aprofundar".

- **Memoria global do Claude Code em `~/.claude/projects/<hash>/memory/`.** (Descoberto na 4.10.) 21 arquivos `.md` de feedback/sub-etapas, auto-memory ON, escreve sem confirmacao. Vetor de contaminacao cross-projeto opaco. Tres mitigacoes possiveis a avaliar: (1) desligar auto-memory globalmente em configuracao do Claude Code; (2) auditar conteudo dos 21 .md existentes para identificar o que veio do `financas-lab` vs outros projetos; (3) versionar politica de retencao no repo (ex: `docs/politica-memoria-claude.md`). Sem impacto em fluxo normal — afeta credibilidade de smoke tests onde determinismo de invocacao importa. Resolver antes do smoke da primeira skill orquestradora (4.11) se a credibilidade do smoke for criterio.

- **Investigar built-in agents do Claude Code.** (Descoberto na 4.10.) Cinco built-ins identificados nominalmente: `Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup`. Competem com subagents do projeto no espaco de delegacao do Claude principal. Investigar: (1) o que cada um faz; (2) quando dispara; (3) se pode ser desabilitado ou se compete sempre; (4) interacao com skills orquestradoras (ADR-012). Documentacao oficial: https://docs.claude.com/en/docs/claude-code/sub-agents (verificar antes de assumir comportamento). Resolver antes ou junto da 4.11 — afeta interpretacao do smoke do par skill+subagent.

- **Auditar plugins globais instalados.** (Descoberto na 4.10.) `code-review` e `frontend-design` identificados na Camada 0; smoke da 4.9.1 confirmou que `code-review` desabilitado localmente ainda afeta comportamento. Investigar: (1) listar plugins instalados globalmente (`claude plugin list` ou equivalente); (2) decidir manter/desabilitar/desinstalar caso a caso; (3) versionar decisao no repo. Decisao sobre plugin `code-review` oficial (criterio aberto da Camada 3) e parcialmente bloqueada por esta investigacao.
```

### Versionar este proprio prompt

`docs/prompt-etapa-4-10.md` entra como novo arquivo no Commit 3. Padrao consolidado desde a 4.7.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra `675e286` (squash da 4.9.1) ou superior.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-10.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `.claude/agents/pr-reviewer.md` existe (~140 linhas, frontmatter `model: haiku`) — nao sera modificado.
- PR #53 aberto, nao mergeado.

Validar com:

```powershell
git status
git log --oneline -3
Test-Path docs\prompt-etapa-4-10.md
Test-Path .claude\agents\pr-reviewer.md
git config core.hooksPath
gh pr view 53 --json number,state
```

**Pre-condicoes ADR-011:**

- `Test-Path docs\prompt-etapa-4-10.md` retorna `True`.
- `Test-Path .claude\agents\pr-reviewer.md` retorna `True`.
- `gh pr view 53` retorna state `OPEN`.
- Working tree limpo exceto o prompt.

Se algum item divergir, **parar e reportar**.

## Tarefas

### Tarefa 1 — Validar pre-requisitos

Rodar comandos da secao "Estado esperado ao iniciar". Se divergir, parar e reportar.

### Tarefa 2 — Sincronizar Environment.CurrentDirectory (ADR-011)

```powershell
cd C:\projetos\financas-lab
[System.Environment]::CurrentDirectory = (Get-Location).Path
[System.Environment]::CurrentDirectory
```

Esperado: `C:\projetos\financas-lab`.

### Tarefa 3 — Criar branch

```bash
git checkout -b docs/etapa-4-10-auditoria-aprendizado
```

Prefixo `docs/` — sub-etapa doc-only.

### Tarefa 4 — Antes de editar, ler arquivos vivos

```bash
cat docs/adrs.md
cat docs/decisoes.md
cat docs/progresso.md
cat docs/hooks-pendentes.md
```

**Confirmar:**

- `adrs.md` termina com ADR-011 (linha ~491). ADR-012 entra como nova secao **apos** ADR-011, no fim do arquivo.
- `decisoes.md` tem subsecao "Refinamento do `pr-reviewer` pos-smoke (Sub-etapa 4.9.1)" (linha ~547) antes de "Claude Code hooks nativos" (linha ~570). Nova subsecao "Auditoria meta-operacional (Sub-etapa 4.10)" entra **entre** essas duas.
- `progresso.md` tem "Sub-etapas concluidas" da Camada 3 em ordem descrescente comecando em 4.9.1 (item mais recente, primeiro da lista). Sub-etapa 4.10 entra **acima** da 4.9.1.
- `progresso.md` tem bloco "Criterios de 'pronto' (preliminar)" da Camada 3 (linhas ~170-183). Bloco e **substituido inteiro** pela versao ajustada pela 4.10.
- `progresso.md` tem "Licoes da Sub-etapa 4.9.1" (linha ~289). "Licoes da Sub-etapa 4.10" entra **acima** da 4.9.1 (ordem cronologica descrescente).
- `progresso.md` tem "Historico de mudancas deste documento" no fim. Linha da 4.10 entra **acima** da entrada mais recente (4.9.1, linha ~795).
- `hooks-pendentes.md` tem "Hooks implementados" (linha ~114). Nova secao "Debitos meta-operacionais" entra **acima** desta.

Se alguma divergencia, **parar e reportar**.

### Tarefa 5 — Editar `docs/adrs.md` (adicionar ADR-012)

Copiar bloco "Conteudo do ADR-012" do escopo decidido acima. Adicionar ao final de `adrs.md`. Encoding UTF-8 sem BOM. Linhas em branco antes/depois de headers `## `, `### `.

**Pre-condicao ADR-011 apos editar:**

```powershell
Test-Path docs\adrs.md   # True

$bytes = [System.IO.File]::ReadAllBytes("docs/adrs.md")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191 (sem BOM)

$content = [System.IO.File]::ReadAllText("docs/adrs.md", [System.Text.UTF8Encoding]::new($false))

# Validar ADR-012 presente
if ($content -match 'ADR-012.{1,5}Subagents do projeto invocados via skill orquestradora') {
    Write-Host "ADR-012 OK"
} else {
    Write-Host "ADR-012 AUSENTE"
}

# Validar Status / Data presentes
if ($content -match 'ADR-012[\s\S]*?\*\*Status:\*\* Aceito[\s\S]*?\*\*Data:\*\* 2026-05-11') {
    Write-Host "Status/Data OK"
} else {
    Write-Host "Status/Data ALTERADO"
}

# Linhas totais (esperado: ~565)
$linhas = (Get-Content docs\adrs.md).Count
Write-Host "Linhas totais: $linhas"
```

Se algum valor divergir, parar e reportar.

### Tarefa 6 — Editar `docs/decisoes.md` (subsecao 4.10)

Copiar bloco "Conteudo da subsecao em decisoes.md" do escopo. Inserir **antes** da linha `### Claude Code hooks nativos`.

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/decisoes.md", [System.Text.UTF8Encoding]::new($false))

if ($content -match '### Auditoria meta-operacional \(Sub-etapa 4\.10\)') {
    Write-Host "Subsecao 4.10 OK"
} else {
    Write-Host "Subsecao 4.10 AUSENTE"
}

# Ordem: 4.9.1 antes da 4.10 antes de hooks nativos
$pos491 = $content.IndexOf('Refinamento do `pr-reviewer` pos-smoke (Sub-etapa 4.9.1)')
$pos410 = $content.IndexOf('Auditoria meta-operacional (Sub-etapa 4.10)')
$posNativos = $content.IndexOf('Claude Code hooks nativos')
if ($pos491 -lt $pos410 -and $pos410 -lt $posNativos) {
    Write-Host "Ordem OK"
} else {
    Write-Host "Ordem TROCADA -- investigar"
}
```

### Tarefa 7 — Editar `docs/progresso.md` (4 edicoes)

Aplicar **edicoes 1 a 4** descritas no escopo, na ordem:

1. Adicionar sub-etapa 4.10 ao topo de "Sub-etapas concluidas" (acima da 4.9.1).
2. Substituir bloco "Criterios de 'pronto' (preliminar)" da Camada 3 pela versao ajustada.
3. Adicionar "Licoes da Sub-etapa 4.10" acima de "Licoes da Sub-etapa 4.9.1".
4. Adicionar linha de historico acima da entrada da 4.9.1.

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/progresso.md", [System.Text.UTF8Encoding]::new($false))

# Sub-etapa 4.10 presente
if ($content -match '4\.10.{1,5}Auditoria de aprendizado meta-operacional') {
    Write-Host "Sub-etapa 4.10 OK"
} else {
    Write-Host "Sub-etapa 4.10 AUSENTE"
}

# Licoes da 4.10 presentes
if ($content -match '## Licoes da Sub-etapa 4\.10') {
    Write-Host "Licoes 4.10 OK"
} else {
    Write-Host "Licoes 4.10 AUSENTE"
}

# Criterios ajustados (ADR-012 mencionado)
if ($content -match 'ADR-012') {
    Write-Host "Criterios com ADR-012 OK"
} else {
    Write-Host "Criterios sem ADR-012"
}

# Ordem cronologica descrescente em sub-etapas concluidas (4.10 acima de 4.9.1)
$pos410 = $content.IndexOf('**4.10')
$pos491 = $content.IndexOf('**4.9.1')
if ($pos410 -gt 0 -and $pos410 -lt $pos491) {
    Write-Host "Ordem cronologica OK"
} else {
    Write-Host "Ordem cronologica TROCADA"
}

# Linhas totais (esperado: ~870 dada a expansao)
$linhas = (Get-Content docs\progresso.md).Count
Write-Host "Linhas totais: $linhas"
```

**Atencao especial:** Hook 4.4 (tamanho de docs, modo warn) provavelmente vai alertar se `progresso.md` cruzar 800 linhas. Comportamento esperado, **nao bloqueia commit**. Reportar alerta se aparecer.

### Tarefa 8 — Editar `docs/hooks-pendentes.md` (3 debitos meta-operacionais)

Copiar bloco "Conteudo dos debitos em hooks-pendentes.md" do escopo. Adicionar nova secao "## Debitos meta-operacionais" antes de "## Hooks implementados".

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/hooks-pendentes.md", [System.Text.UTF8Encoding]::new($false))

if ($content -match '## Debitos meta-operacionais') {
    Write-Host "Secao OK"
} else {
    Write-Host "Secao AUSENTE"
}

# Tres debitos esperados
$mem = $content -match 'Memoria global do Claude Code'
$builtin = $content -match 'Investigar built-in agents'
$plugins = $content -match 'Auditar plugins globais'
Write-Host "Debito memoria global: $mem"
Write-Host "Debito built-in agents: $builtin"
Write-Host "Debito plugins globais: $plugins"

# Ordem: debitos meta antes de hooks implementados
$posMeta = $content.IndexOf('## Debitos meta-operacionais')
$posImpl = $content.IndexOf('## Hooks implementados')
if ($posMeta -gt 0 -and $posMeta -lt $posImpl) {
    Write-Host "Ordem OK"
} else {
    Write-Host "Ordem TROCADA"
}
```

### Tarefa 9 — Versionar este proprio prompt

```bash
git status
```

Confirmar que `docs/prompt-etapa-4-10.md` aparece como **untracked**. Sera incluido no Commit 3.

### Tarefa 10 — Commits (3 commits)

**Commit 1** — ADR-012:

```bash
git add docs/adrs.md
git status   # apenas adrs.md staged
git commit -m "docs(adr): ADR-012 -- subagents invocados via skill orquestradora"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 2** — Auditoria meta-operacional (decisoes + progresso + hooks-pendentes):

```bash
git add docs/decisoes.md docs/progresso.md docs/hooks-pendentes.md
git status   # 3 arquivos staged
git commit -m "docs: sub-etapa 4.10 -- auditoria meta-operacional + 3 debitos novos"
```

**Pre-condicao ADR-011:** 3 arquivos staged; `$LASTEXITCODE = 0`.

**Atencao:** hook 4.4 (tamanho docs warn) pode alertar para `progresso.md` cruzar 800 linhas. **Nao bloqueia.** Reportar se aparecer.

**Commit 3** — Versionar prompt:

```bash
git add docs/prompt-etapa-4-10.md
git status   # 1 arquivo staged
git commit -m "docs: versiona prompt-etapa-4-10.md"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Validacao implicita dos hooks ativos:**

- Conventional Commits (4.1): mensagens seguem padrao `docs(adr):`, `docs:` — validas.
- Encoding UTF-8 (4.2): valida bytes em todos os `.md`.
- Markdown blank lines (4.3): valida headers nivel 2-6 em todos os `.md` editados.
- Tamanho de docs (4.4): pode alertar em `progresso.md`.
- Maven release (4.5), @Entity (4.7): nao se aplicam (sem `.java`/`pom.xml`).

Se algum hook bloquear, parar e reportar.

### Tarefa 11 — Validacao final antes de push

```bash
git status
git log --oneline -5
git config core.hooksPath
```

Esperado:

- Working tree limpo.
- 3 commits novos.
- `core.hooksPath` = `.githooks`.

### Tarefa 12 — Fechar PR #53 (smoke da 4.9.1) sem merge

**Apos abrir PR da 4.10 (proxima tarefa) e ter o numero dele**, executar:

```bash
gh pr close 53 --comment "Smoke da 4.9.1 cumpriu seu papel -- gerou descobertas estruturais sobre nao-determinismo de invocacao proativa de subagent, registradas formalmente em PR #<numero-da-4.10> (sub-etapa 4.10). Smoke e instrumento de descoberta, nao codigo de producao. Fechado sem merge; URL preservada como evidencia no historico do projeto."
```

**Importante:** **nao fechar PR #53 antes** de abrir o PR da 4.10. O comentario referencia o numero da 4.10. Sequencia: abrir 4.10 -> capturar numero -> fechar #53 com numero da 4.10 no comentario.

## Restricoes e freios

1. **Apenas doc-only.** Sem mudanca em `.claude/agents/`, `.claude/hooks/`, `.githooks/`, `pom.xml`, `src/`, `frontend/`, `scripts/`, migrations, `.gitignore`, `.gitattributes`, blueprint.

2. **CLAUDE.md NAO atualizado.** Regra 4.6: sincronizacao acontece na sub-etapa causadora. A 4.10 decide a convencao; a 4.11 implementa a primeira skill orquestradora e atualiza CLAUDE.md.

3. **NAO criar skill, subagent, hook, MCP, script** nesta sub-etapa. Sub-etapa decide; nao implementa.

4. **NAO modificar `.claude/agents/pr-reviewer.md`.** Subagent existente permanece valido conforme decisao do ADR-012.

5. **NAO investigar built-in agents nesta sub-etapa.** Esta investigacao e debito explicito em `hooks-pendentes.md`. Tentar resolver agora estoura o escopo.

6. **NAO mexer em `~/.claude/`** (memoria global, plugins globais). Mitigacoes ficam como debito. Tentar mitigar agora estoura o escopo e arrisca contaminar smoke tests futuros sem registro de antes/depois.

7. **NAO mergear PR #53** nem agora nem depois. Fechar sem merge conforme Tarefa 12.

8. **NAO mergear o PR da 4.10** apos abrir. Esperar autorizacao explicita do operador.

9. **Decisao Caminho B ja tomada** (D6 calibrado). Se aparecer ambiguidade no texto do prompt sobre A vs B vs C, **parar e reportar** — nao tomar decisao silenciosa.

10. **Encoding UTF-8 sem BOM** em todos os `.md` editados/criados.

11. **Apenas ASCII em mensagens de commit.** Sem acentos, sem em-dash U+2014.

12. **Ordem cronologica descrescente** em todos os blocos de historico (`progresso.md`: sub-etapas concluidas + licoes + historico).

13. **Sem cenarios destrutivos tradicionais.** Sub-etapa doc-only — validacao via pre-condicoes ADR-011 em cada Tarefa.

14. **Hook 4.4 (tamanho docs, modo warn) pode alertar em `progresso.md`.** Comportamento esperado se o arquivo cruzar 800 linhas com as adicoes. **Nao bloqueia commit.** Reportar alerta se aparecer.

15. **Nao sugerir proxima sub-etapa espontaneamente.** Apos PR aberto + #53 fechado, parar.

16. **Nao tomar decisao silenciosa em zona limitrofe.** Reportar divergencias.

17. **Nao usar `pwsh`.** PowerShell 5.1 (`powershell`).

18. **Nao usar `git reset --hard`.**

19. **Nao usar `git commit --no-verify`** nesta sub-etapa. Hooks devem passar; alertar warn e ok.

## Estrutura de commits

Branch: `docs/etapa-4-10-auditoria-aprendizado`

**Commit 1** — `docs(adr): ADR-012 -- subagents invocados via skill orquestradora`

- `docs/adrs.md` (adicao do ADR-012)

**Commit 2** — `docs: sub-etapa 4.10 -- auditoria meta-operacional + 3 debitos novos`

- `docs/decisoes.md` (subsecao 4.10)
- `docs/progresso.md` (sub-etapa 4.10 + criterios Camada 3 ajustados + licoes + historico)
- `docs/hooks-pendentes.md` (3 debitos meta-operacionais)

**Commit 3** — `docs: versiona prompt-etapa-4-10.md`

- `docs/prompt-etapa-4-10.md` (novo)

## Validacao antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -5
git config core.hooksPath
```

Esperado:

- `check.ps1` passa (build + testes do projeto inalterados).
- Working tree limpo.
- 3 commits novos.

## PR

Titulo: `docs: sub-etapa 4.10 -- auditoria meta-operacional + ADR-012 (skill orquestradora invoca subagent)`

Body sugerido:

````markdown
## Summary

Sub-etapa doc-only inaugurando categoria **"auditoria meta-operacional"**. Registra 4 descobertas do smoke pos-merge da 4.9.1 + toma decisao estrutural sobre uso de subagents no projeto (Caminho B), formalizada em **ADR-012**.

### Por que esta sub-etapa existe

Smoke test pos-merge da 4.9.1 revelou que **invocacao proativa de subagent via campo `description` nao e deterministica** no Claude Code. Pedido "revisa este PR" foi executado pelo Claude principal direto, sem delegar ao `pr-reviewer` via Task tool. Subagent existe, esta bem-formado (template prescritivo + 2 exemplos few-shot apos a 4.9.1) — mas o Claude principal nao o chamou.

A descoberta nao e falha do `pr-reviewer`. E limite arquitetural que afeta toda a Camada 3.

PR #53 (smoke da 4.9.1) e a evidencia que gerou esta auditoria.

### Quatro descobertas registradas

1. **Memoria global em `~/.claude/projects/<hash>/memory/`** — 21 .md, auto-memory ON, escreve sem confirmacao. Vetor de contaminacao cross-projeto opaco.
2. **Plugins globais nao-versionados** (`code-review`, `frontend-design`) alteram comportamento mesmo desabilitados localmente.
3. **Built-in agents do Claude Code** (`Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup`) competem com subagents do projeto.
4. **Heuristica de delegacao via `description`** prefere execucao direta em tarefas que o Claude principal julga simples. Description "Use proactively" e instrucao fraca.

### Decisao tomada: Caminho B (formalizado em ADR-012)

**Subagents do projeto sao invocados via skill orquestradora dedicada.** Cada subagent tem uma skill (`.claude/skills/<escopo>/<nome>.md`) que, quando invocada via slash command pelo operador, prescreve em texto direto a invocacao via Task tool.

**Alternativas rejeitadas:**

- **Caminho A** (description imperativa "ALWAYS delegate"): palpite sem evidencia; continua dependendo de heuristica caixa-preta; nao escala.
- **Caminho C** (abandonar subagents): descarta `pr-reviewer` (4.9 + 4.9.1) baseado em N=1.

### Categoria nova: "auditoria meta-operacional"

Diferente de:

- **"Registro pos-smoke falho"** (4.2.1, 4.7.1): afeta 1 componente.
- **"Refinamento pos-smoke empirico"** (4.9.1): afeta 1 componente.
- **Auditoria meta-operacional:** afeta varios componentes / estrategia de camada. Sai com decisao estrutural tomada.

### Mudancas

- `docs/adrs.md`: **ADR-012** -- subagents invocados via skill orquestradora. ~85 linhas novas.
- `docs/decisoes.md`: subsecao "Auditoria meta-operacional (Sub-etapa 4.10)" -- 4 descobertas + decisao Caminho B + alternativas rejeitadas.
- `docs/progresso.md`: sub-etapa 4.10 + criterios de "pronto" da Camada 3 ajustados (cada subagent vem com skill correspondente) + 4 licoes + entrada de historico.
- `docs/hooks-pendentes.md`: nova secao "Debitos meta-operacionais" com 3 debitos (memoria global, built-in agents, plugins globais).
- `docs/prompt-etapa-4-10.md`: prompt versionado.

### CLAUDE.md NAO atualizado

Regra 4.6: CLAUDE.md sincronizado com sub-etapa causadora. A 4.10 decide a convencao; a 4.11 implementa a primeira skill orquestradora e atualiza CLAUDE.md.

### `pr-reviewer` permanece valido

Smoke da 4.9.1 confirmou que o componente funciona quando invocado. O que falta e o mecanismo deterministico de invocacao — o que ADR-012 estabelece. Sub-etapa 4.11 implementa `/review-pr` orquestrando `pr-reviewer` e valida o padrao ponta-a-ponta.

### PR #53 fechado sem merge

PR #53 (smoke da 4.9.1) e descoberta, nao codigo. Foi fechado com comentario referenciando este PR. URL preservada no historico para evidencia.

### Sem validacao destrutiva tradicional

Sub-etapa doc-only. Pre-condicoes ADR-011 (Test-Path, encoding sem BOM, ordem cronologica, presenca de markers no conteudo) verificadas em cada Tarefa.

### Hook 4.4 pode alertar

`progresso.md` provavelmente cruza 800 linhas com as adicoes. Hook em modo warn — alerta sem bloquear. Comportamento esperado.

### Proximo passo

Decisao fora deste PR. Candidato natural: **4.11 — primeira skill orquestradora `/review-pr`** invocando `pr-reviewer` via Task tool, com smoke validando padrao ADR-012 ponta-a-ponta.
````

## Pos-criacao do PR

1. Abrir PR via `gh pr create`.
2. Capturar o numero.
3. Editar `docs/decisoes.md` e `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. **Fechar PR #53** com `gh pr close 53 --comment "..."` referenciando o numero do PR da 4.10 (ver Tarefa 12).
7. Esperar CI verde no PR da 4.10.
8. **Aguardar autorizacao explicita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `docs/etapa-4-10-auditoria-aprendizado` empurrada com 4 commits (3 + 1 update do PR).
- PR da 4.10 aberto, CI verde, **nao mergeado**.
- PR #53 fechado sem merge, com comentario referenciando o PR da 4.10.
- `main` ainda no squash da 4.9.1 (`675e286`).
- Working tree limpo.
- `.claude/agents/pr-reviewer.md` inalterado (~140 linhas).
- Reportar: `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup`, `gh pr view 53 --json state` (esperado `CLOSED`).

## O que NAO fazer ao terminar

- Nao mergear o PR da 4.10.
- Nao reabrir PR #53.
- Nao criar prompt da proxima sub-etapa (4.11).
- Nao criar skills, subagents, hooks, MCPs, scripts.
- Nao modificar `pr-reviewer.md`.
- Nao mexer em `~/.claude/` global (memoria, plugins).
- Nao investigar built-in agents — fica como debito.
- Nao atualizar `CLAUDE.md`, blueprint, `.gitignore`, `.gitattributes`.
- Nao sugerir proximo passo espontaneamente alem do candidato natural ja citado no PR body.
- Nao usar `pwsh`.
- Nao usar `git reset --hard`.
- Nao usar `git commit --no-verify`.
