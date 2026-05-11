# Prompt — Etapa 4.11: Primeira skill orquestradora `/review-pr` + errata ADR-012 (mecanismo nativo `context: fork`)

## Contexto

Camada 3 com 6 hooks + CLAUDE.md + blueprint + 1 subagent (`pr-reviewer` Haiku) + ADR-012 (decidiu padrao "skill orquestradora invoca subagent") apos a 4.10. Main no squash da 4.10. Working tree limpo.

A 4.10 decidiu o padrao estrutural (Caminho B); a 4.11 entrega a **primeira implementacao** dele. Tres descobertas pre-redacao reconfiguraram o escopo previsto:

1. **`.claude/skills/` e flat.** Doc oficial do Claude Code (https://code.claude.com/docs/en/skills) mostra estrutura `.claude/skills/<nome>/SKILL.md` direto — sem subdiretorio intermediario. As pastas `.claude/skills/local/` e `.claude/skills/universal/` criadas pela 4.0 nao geram slash commands validos. Categoria identica a descoberta da 4.9 sobre subagents. **Estruturas `.claude/{hooks,agents,skills}` sao assimetricas**: hooks=5 subpastas, agents=flat, skills=flat.

2. **Mecanismo nativo `context: fork` + `agent: <nome>` em SKILL.md frontmatter.** O ADR-012 prescreveu "skill com instrucao textual: 'Use a Task tool...'" — esse e o **Modo textual**, depende do Claude principal interpretar a instrucao. A doc oficial mostra **Modo nativo** via frontmatter: `context: fork` cria contexto isolado e `agent: <nome>` aponta para subagent custom; o conteudo da skill vira o prompt do subagent forkado **sem intermediacao** do Claude principal. Determinismo arquitetural superior — elimina o nao-determinismo que o ADR-012 buscava resolver.

3. **`disable-model-invocation: true`** restringe invocacao apenas ao operador via slash command, eliminando auto-discovery via description matching. Maximo determinismo.

Esta sub-etapa entrega cinco coisas em um unico PR:

1. **Errata do ADR-012.** Substitui Modo textual (passos 1-5 textuais + Task tool) pelo Modo nativo (`context: fork` + `agent`). **Decisao estrutural preservada** — skill orquestra subagent, sem mudanca de direcao. So o mecanismo literal muda. Categoria nova: **"errata de ADR baseada em descoberta de documentacao oficial"**.

2. **Skill `/review-pr`** em `.claude/skills/review-pr/SKILL.md` (flat). Frontmatter com `context: fork`, `agent: pr-reviewer`, `disable-model-invocation: true`, `argument-hint: [pr-number]`, `allowed-tools` para `gh pr`. Conteudo curto e prescritivo — vira o prompt do subagent forkado.

3. **Cleanup das pastas orfas** `.claude/skills/local/` e `.claude/skills/universal/`. Removidas porque nao geram skills validas e podem confundir sub-etapas futuras.

4. **CLAUDE.md atualizado.** Subsecao nova "Subagents e skills" em "Convencoes e padroes" registra a regra do ADR-012 (revisado) em 3-4 linhas. Regra 4.6 disparada — 4.11 e a sub-etapa causadora da convencao entrar em uso.

5. **Registro em decisoes/progresso** + versionamento do prompt.

Quando esta etapa terminar:

- `docs/adrs.md`: ADR-012 com Revisao 2026-05-11 anexada (mecanismo refinado, decisao preservada).
- `.claude/skills/review-pr/SKILL.md`: primeira skill orquestradora do projeto.
- `.claude/skills/local/`, `.claude/skills/universal/`: removidas.
- `CLAUDE.md`: subsecao "Subagents e skills" adicionada.
- `docs/decisoes.md`: subsecao 4.11 antes de "Claude Code hooks nativos".
- `docs/progresso.md`: sub-etapa 4.11 + criterios Camada 3 ajustados + licoes + historico.
- `docs/prompt-etapa-4-11.md`: versionado.

## Padroes que estreiam nesta etapa

1. **Categoria nova: "errata de ADR baseada em descoberta de documentacao oficial".** Distinta de:
   - "Patch tecnico" (4.0.1): corrige bug.
   - "Refinamento pos-smoke empirico" (4.9.1): smoke revelou suboptimo.
   - "Auditoria meta-operacional" (4.10): descobertas afetam estrategia de camada.
   - **Errata documental:** decisao estrutural permanece, mecanismo literal muda apos investigacao da doc oficial. Categoria operacional, aplicavel a futuros ADRs.

2. **Leitura previa da documentacao oficial como parte do escopo de prompt-criacao para componente novo.** A 4.11 nasceu refinada porque investigou a doc oficial antes da redacao final. Padrao a aplicar: **antes de prescrever primeiro componente de um tipo novo** (primeira skill, primeiro MCP, etc.), ler a doc oficial do mecanismo. Aplicacao retroativa: a 4.0 prescreveu `.claude/skills/` com subpastas sem consultar doc; a 4.9 prescreveu `.claude/agents/` com subpastas sem consultar; ambas viraram debito de cleanup.

3. **Primeira skill do projeto.** Marco estrutural — Camada 3 do blueprint pede 5-10 skills, este e o primeiro. Estabelece o padrao replicavel para todas as skills orquestradoras futuras.

4. **Cleanup de artefatos orfaos no mesmo PR que estabelece o padrao correto.** Pastas `local/` e `universal/` em `.claude/skills/` viram lixo morto se nao removidas. Decisao: remover na 4.11 evita debito acumulado.

## Escopo decidido (calibrado com operador antes da redacao via D1-D5)

### Arquivos modificados

| Arquivo | Tipo de mudanca |
|---|---|
| `docs/adrs.md` | Revisao anexada ao ADR-012 |
| `.claude/skills/review-pr/SKILL.md` | Novo (skill orquestradora) |
| `.claude/skills/local/` | Removida (pasta + conteudo) |
| `.claude/skills/universal/` | Removida (pasta + conteudo) |
| `CLAUDE.md` | Subsecao "Subagents e skills" adicionada |
| `docs/decisoes.md` | Subsecao 4.11 antes de "Claude Code hooks nativos" |
| `docs/progresso.md` | Sub-etapa 4.11 + criterios Camada 3 ajustados + licoes + historico |
| `docs/prompt-etapa-4-11.md` | Versionado (novo) |

**Nao tocados:** `.claude/agents/pr-reviewer.md`, `.claude/hooks/`, `.githooks/`, `pom.xml`, `src/`, `frontend/`, blueprint, `.gitignore`, `.gitattributes`, `docs/hooks-pendentes.md`.

### Errata do ADR-012 (em `docs/adrs.md`)

**Adicionar bloco no FINAL do ADR-012** (apos a secao "Consequencias"), preservando o ADR original. O bloco e errata explicita — texto original fica intacto para historico:

```markdown
### Revisao 2026-05-11 (Sub-etapa 4.11)

Investigacao da documentacao oficial do Claude Code durante a redacao da 4.11 revelou:

1. **Estrutura de skills e flat.** `.claude/skills/<nome>/SKILL.md` direto sob `.claude/skills/`. Path original "`.claude/skills/<escopo>/<nome>.md`" mencionado no Contexto e Mecanismo deste ADR esta incorreto. Path correto: **`.claude/skills/<nome>/SKILL.md`**. Analogo a descoberta da 4.9 sobre subagents (flat em `.claude/agents/<nome>.md`).

2. **Mecanismo nativo via frontmatter substitui invocacao textual via Task tool.** O Mecanismo descrito originalmente (passos 1-5 com "skill contem prompt direto: 'Use a ferramenta Task...'") depende de o Claude principal interpretar instrucao textual e disparar Task tool — reproduz o mesmo nao-determinismo que o ADR-012 buscava eliminar. A doc oficial (https://code.claude.com/docs/en/skills, secao "Run skills in a subagent") mostra mecanismo nativo via frontmatter:

   ```yaml
   ---
   context: fork
   agent: <nome>
   ---
   [conteudo da skill]
   ```

   Com `context: fork`, o Claude Code cria contexto isolado automaticamente; o `agent: <nome>` aponta para subagent custom em `.claude/agents/<nome>.md`; o conteudo da skill vira o prompt do subagent forkado **sem intermediacao** do Claude principal. Determinismo arquitetural superior.

3. **Decisao estrutural preservada.** Skill orquestra subagent permanece valido. Apenas o **mecanismo literal** muda (frontmatter `context: fork` em vez de instrucao textual). Padroes obrigatorios 1, 3 e 4 da Decisao original permanecem. Padrao obrigatorio 2 reescrito abaixo.

**Mecanismo revisado:**

1. Operador invoca a skill explicitamente (ex: `/review-pr 53`).
2. A skill em `.claude/skills/<nome>/SKILL.md` declara `context: fork` + `agent: <nome>` no frontmatter, apontando para o subagent custom em `.claude/agents/<nome>.md`.
3. Claude Code cria contexto forkado e executa a skill content como prompt do subagent — sem intermediacao do Claude principal.
4. Subagent roda em contexto isolado, com modelo, tools e permissoes definidas no frontmatter de `.claude/agents/<nome>.md` (system prompt vem do body do subagent; user prompt vem do SKILL.md).
5. Output do subagent retorna para o Claude principal, que apresenta ao operador.

**Padrao obrigatorio 2 revisado:**

> A skill declara `context: fork` + `agent: <nome>` no frontmatter, apontando para o subagent custom. Conteudo da skill vira prompt do subagent forkado pelo mecanismo nativo do Claude Code.

**Nao mecanismo (expandido):**

- Invocacao por heuristica de delegacao proativa via campo `description` no subagent (descartado no ADR original).
- **Invocacao via instrucao textual na skill** ("Use a Task tool..."): tambem descartado nesta revisao. Reproduz nao-determinismo do Claude principal interpretando texto.

**Recomendacao adicional:** skills orquestradoras de subagent devem usar `disable-model-invocation: true` no frontmatter. Isso restringe invocacao apenas ao operador via slash command, eliminando auto-discovery via description matching da skill. Maximo determinismo: skill so dispara quando operador escreve `/<nome>`.

**Categoria operacional:** "errata de ADR baseada em descoberta de documentacao oficial". Distinta de patch tecnico (corrige bug do entregue) e refinamento pos-smoke empirico (smoke revelou suboptimo). Aplicavel a futuros ADRs cuja prescricao se mostre tecnicamente incorreta apos consulta a fonte oficial.
```

### Conteudo de `.claude/skills/review-pr/SKILL.md` (novo arquivo)

Encoding UTF-8 sem BOM. Sem acentos (alinhado com restante do projeto).

```markdown
---
name: review-pr
description: Revisa o PR informado via subagent pr-reviewer (Haiku). Use antes do merge.
disable-model-invocation: true
context: fork
agent: pr-reviewer
argument-hint: [pr-number]
allowed-tools: Bash(gh pr view *) Bash(gh pr diff *)
---

Revise o PR #$ARGUMENTS seguindo todas as instrucoes do seu system prompt.

Use `gh pr view $ARGUMENTS` e `gh pr diff $ARGUMENTS` para ler o PR.

Produza output usando exatamente as 3 secoes prescritas no seu system prompt (Bloqueadores, Sugestoes, Elogios), sem acrescentar outras. Se uma secao nao tem itens, escreva `_Nenhum_` em italico.
```

**Razao do conteudo curto:** o system prompt do subagent (body de `.claude/agents/pr-reviewer.md`) ja contem toda a logica de revisao — identidade, o que verifica, template de output, exemplos few-shot, tom. A skill so precisa entregar a tarefa concreta + reforcar template. Duplicar conteudo do subagent na skill geraria divergencia de fonte.

### Remocao das pastas orfas `.claude/skills/local/` e `.claude/skills/universal/`

Criadas pela 4.0 sem skill dentro (provavelmente com `.gitkeep` ou `.gitignore` interno). Removidas via `git rm -r`. Sem impacto funcional — nao geram slash commands. Cleanup evita confusao em sub-etapas futuras.

**Verificacao previa obrigatoria** (Tarefa 4): conferir se ha conteudo dentro das pastas. Se houver arquivo nao previsto, **parar e reportar** — pode ser intervencao nao registrada.

### Atualizacao em `CLAUDE.md`

Adicionar nova subsecao "### Subagents e skills" em "Convencoes e padroes", **entre** as subsecoes "Sub-etapas" (linha ~61) e "Validacao destrutiva (ADR-011)" (linha ~65):

```markdown
### Subagents e skills

Subagents do projeto vivem em `.claude/agents/<nome>.md` (flat); skills orquestradoras correspondentes em `.claude/skills/<nome>/SKILL.md` (flat). Skill usa `context: fork` + `agent: <nome>` no frontmatter para forkar contexto no subagent. Operador invoca via slash command (`/<nome> <args>`); skill com `disable-model-invocation: true` nao e invocada automaticamente. Invocacao proativa via campo `description` no subagent e considerada nao-deterministica e nao e mecanismo primario (ADR-012).
```

Acrescimo de 4 linhas. Convencao registrada. Detalhes ficam no ADR-012 (linkado via `docs/adrs.md` em "Onde buscar mais").

### Conteudo da subsecao em `docs/decisoes.md`

Inserir **antes** da subsecao `### Claude Code hooks nativos` (mesma posicao usada pela 4.10):

```markdown
### Primeira skill orquestradora: /review-pr (Sub-etapa 4.11)

Primeira implementacao do padrao decidido em ADR-012 (Sub-etapa 4.10). Marco estrutural — Camada 3 do blueprint pede 5-10 skills, este e o primeiro.

**Componente:** `.claude/skills/review-pr/SKILL.md`. Frontmatter declara:

- `name: review-pr`, `description` clara.
- `disable-model-invocation: true` — apenas operador invoca via `/review-pr <numero>`.
- `context: fork` + `agent: pr-reviewer` — mecanismo nativo do Claude Code dispara contexto forkado no subagent `pr-reviewer`. **Sem instrucao textual** "use Task tool...". Determinismo arquitetural via frontmatter.
- `argument-hint: [pr-number]` — autocomplete sugere argumento.
- `allowed-tools: Bash(gh pr view *) Bash(gh pr diff *)` — pre-aprovacao para evitar prompt de permissao no smoke.

**Conteudo curto.** System prompt do subagent (body de `pr-reviewer.md`) ja contem toda a logica de revisao (identidade, verificacoes, template, exemplos). A skill apenas entrega a tarefa concreta (`Revise o PR #$ARGUMENTS...`) + reforco do template (3 secoes). Evita duplicacao de fonte.

**Errata ADR-012 acompanha esta sub-etapa.** Investigacao da doc oficial do Claude Code (https://code.claude.com/docs/en/skills) revelou que o mecanismo prescrito originalmente no ADR-012 ("skill contem instrucao textual: 'Use a Task tool...'") reproduzia o mesmo nao-determinismo que o ADR buscava eliminar. Mecanismo nativo `context: fork` substitui — decisao estrutural preservada, mecanismo literal refinado. Categoria nova: **"errata de ADR baseada em descoberta de documentacao oficial"**.

**Cleanup de pastas orfas.** `.claude/skills/local/` e `.claude/skills/universal/` foram criadas pela 4.0 com expectativa de organizacao por escopo. Pelo padrao oficial, skills sao flat em `.claude/skills/<nome>/SKILL.md` — pastas intermediarias nao geram slash commands. Removidas via `git rm -r` na 4.11 para evitar confusao em sub-etapas futuras. **Estruturas `.claude/{hooks,agents,skills}` continuam assimetricas** (hooks=5 subpastas, agents=flat, skills=flat) — reflete decisoes especificas de cada mecanismo.

**Smoke test pos-merge** valida o padrao ponta-a-ponta:

1. Sessao nova do Claude Code.
2. Abrir PR de teste trivial em branch nova.
3. Invocar `/review-pr <numero>`.
4. **Criterios de sucesso:**
   - Skill dispara fork no agent `pr-reviewer` (Haiku) — sem execucao direta pelo Claude principal.
   - Output usa exatamente as 3 secoes prescritas (Bloqueadores, Sugestoes, Elogios).
   - Sem secoes extras (Visao Geral, Analise, Conclusao, etc.).
5. **Risco residual reconhecido:** debitos meta-operacionais da 4.10 (memoria global em `~/.claude/projects/.../memory/`, plugins globais, built-in agents) ainda nao mitigados. Smoke positivo nao prova determinismo absoluto, mas evidencia funcional do par skill+subagent suficiente para validar padrao.

**CLAUDE.md atualizado nesta sub-etapa** (regra 4.6 — 4.11 e a sub-etapa causadora da convencao "subagents+skills" entrar em uso). Subsecao "Subagents e skills" adicionada em "Convencoes e padroes" com 4 linhas resumindo o padrao do ADR-012 revisado.

**Critierios de "pronto" da Camada 3 ajustados.** Padrao "skill orquestradora -> subagent validado com smoke" depende do smoke pos-merge da 4.11 — fica em pendente ate o smoke passar.
```

### Conteudo das edicoes em `docs/progresso.md`

**Edicao 1 — Sub-etapa 4.11 no topo de "Sub-etapas concluidas" da Camada 3** (acima da 4.10):

```markdown
- **4.11 — Primeira skill orquestradora `/review-pr` + errata ADR-012** (2026-05-11): primeira skill do projeto, primeira implementacao do padrao decidido em ADR-012. `.claude/skills/review-pr/SKILL.md` (flat) com `context: fork` + `agent: pr-reviewer` + `disable-model-invocation: true`. Mecanismo nativo do Claude Code dispara contexto forkado no subagent — sem instrucao textual via Task tool. Errata do ADR-012 anexada: investigacao da doc oficial revelou que o mecanismo literal prescrito originalmente ("skill contem prompt: 'Use a Task tool...'") reproduzia o nao-determinismo que se buscava eliminar. Decisao estrutural preservada, mecanismo refinado. Categoria nova: "errata de ADR baseada em descoberta de documentacao oficial". Pastas orfas `.claude/skills/local/` e `.claude/skills/universal/` removidas — skills sao flat (analogo aos subagents). CLAUDE.md atualizado com subsecao "Subagents e skills" (regra 4.6). 3 licoes novas em progresso.md. PR #XX.
```

**Edicao 2 — Ajustar criterios de "pronto" da Camada 3** (substituir bloco atual ajustado pela 4.10):

```markdown
### Criterios de "pronto" (preliminar, ajustados pela 4.10 e 4.11)

- [x] `CLAUDE.md` do projeto escrito (target <=15KB) — concluido 4.6, atualizado 4.11
- [x] Padrao skill orquestradora -> subagent decidido — ADR-012 (4.10, revisado 4.11)
- [x] Subagent `pr-reviewer` (revisao critica antes do PR) — concluido 4.9 + refinado 4.9.1
- [x] Skill `/review-pr` orquestrando `pr-reviewer` (par com ADR-012) — concluido 4.11
- [ ] Smoke pos-merge da 4.11 validando padrao skill+subagent ponta-a-ponta
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

**Edicao 3 — Bloco "Licoes da Sub-etapa 4.11"** acima de "Licoes da Sub-etapa 4.10":

```markdown
## Licoes da Sub-etapa 4.11

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — entrega skill + errata ADR.)

### Licoes de ambiente

1. **Skills sao flat em `.claude/skills/<nome>/SKILL.md`.** Doc oficial do Claude Code (https://code.claude.com/docs/en/skills) confirma: o nome do diretorio direto sob `.claude/skills/` vira o slash command. Subpastas intermediarias nao geram skills validas. A 4.0 criou `.claude/skills/local/` e `.claude/skills/universal/` por intencao de organizacao por escopo (analoga a `.claude/agents/local/` etc.) — pelo padrao oficial, isso nao funciona. Pastas removidas na 4.11. **Estruturas `.claude/{hooks,agents,skills}` sao assimetricas:** hooks=5 subpastas, agents=flat, skills=flat. Cada mecanismo do Claude Code tem convencao propria — simetria entre eles e ilusao da 4.0.

2. **Mecanismo nativo `context: fork` + `agent: <nome>` substitui invocacao textual via Task tool.** A 4.10 prescreveu Modo textual no ADR-012 ("skill contem prompt: 'Use a Task tool...'") sem investigar a doc oficial. Doc oficial mostra Modo nativo via frontmatter: `context: fork` cria contexto isolado, `agent: <nome>` aponta para subagent custom em `.claude/agents/`. Skill content vira prompt do subagent forkado **sem intermediacao** do Claude principal. Determinismo arquitetural — elimina o nao-determinismo que o ADR buscava resolver. Errata anexada ao ADR-012 nesta sub-etapa. Categoria operacional nova: **"errata de ADR baseada em descoberta de documentacao oficial"**.

3. **Leitura previa da documentacao oficial e parte do escopo de prompt-criacao para primeiro componente de um tipo novo.** A 4.0 prescreveu `.claude/skills/` com subpastas sem consultar doc; a 4.9 prescreveu `.claude/agents/` com subpastas sem consultar; ambas viraram debito de cleanup. A 4.11 nasceu refinada porque investigou a doc oficial antes da redacao. Padrao a aplicar: **antes de prescrever primeiro componente de um tipo novo** (primeira skill, primeiro MCP, primeiro hook em settings.json, etc.), ler a doc oficial do mecanismo. Aplicacao retroativa: as 4.0/4.9 nao serao revisadas (pastas removidas; lesao consolidada como historico), mas o padrao vale a partir da 4.11.
```

**Edicao 4 — Linha no historico** acima da entrada da 4.10:

```markdown
- **2026-05-11** — Sub-etapa 4.11 concluida: primeira skill orquestradora `/review-pr` em `.claude/skills/review-pr/SKILL.md` (flat) + errata do ADR-012 (mecanismo nativo `context: fork` substitui invocacao textual via Task tool). Pastas orfas `.claude/skills/local/` e `.claude/skills/universal/` removidas. CLAUDE.md atualizado com subsecao "Subagents e skills" (regra 4.6). Categoria nova: "errata de ADR baseada em descoberta de documentacao oficial". 3 licoes novas. PR #XX.
```

### Versionar este proprio prompt

`docs/prompt-etapa-4-11.md` entra como novo arquivo no Commit 4. Padrao consolidado desde a 4.7.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra squash da 4.10 (hash a confirmar via comando — exata sequencia esperada nao prescrita aqui).
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-11.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `.claude/agents/pr-reviewer.md` existe (~140 linhas, `model: haiku`) — nao sera modificado.
- `.claude/skills/local/` e `.claude/skills/universal/` existem (provavelmente vazias ou com `.gitkeep`/`.gitignore` interno).

Validar com:

```powershell
git status
git log --oneline -5
Test-Path docs\prompt-etapa-4-11.md
Test-Path .claude\agents\pr-reviewer.md
Test-Path .claude\skills\local
Test-Path .claude\skills\universal
git config core.hooksPath
```

**Pre-condicoes ADR-011:**

- `Test-Path docs\prompt-etapa-4-11.md` retorna `True`.
- `Test-Path .claude\agents\pr-reviewer.md` retorna `True`.
- `Test-Path .claude\skills\local` retorna `True`.
- `Test-Path .claude\skills\universal` retorna `True`.
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
git checkout -b feat/etapa-4-11-skill-review-pr
```

Prefixo `feat/` — adiciona funcionalidade (primeira skill do projeto).

### Tarefa 4 — Antes de editar, ler arquivos vivos e validar pastas orfas

```bash
cat docs/adrs.md
cat docs/decisoes.md
cat docs/progresso.md
cat CLAUDE.md
cat .claude/agents/pr-reviewer.md
```

```powershell
Get-ChildItem .claude\skills\local -Force
Get-ChildItem .claude\skills\universal -Force
Get-ChildItem .claude\skills -Force
```

**Confirmar:**

- `adrs.md` termina com ADR-012 (com a estrutura Contexto/Decisao/Alternativas/Consequencias). Errata entra **apos** "Consequencias", **antes** do proximo ADR (se houver) ou no fim do arquivo.
- `decisoes.md` tem subsecao "Auditoria meta-operacional (Sub-etapa 4.10)" antes de "Claude Code hooks nativos". Nova subsecao "Primeira skill orquestradora: /review-pr (Sub-etapa 4.11)" entra **entre** essas duas.
- `progresso.md` tem "Sub-etapas concluidas" da Camada 3 em ordem descrescente comecando em 4.10. Sub-etapa 4.11 entra **acima** da 4.10.
- `progresso.md` tem "Criterios de 'pronto' (preliminar, ajustados pela 4.10)" — substituir bloco inteiro pela versao ajustada 4.10+4.11.
- `progresso.md` tem "Licoes da Sub-etapa 4.10" — "Licoes da Sub-etapa 4.11" entra **acima**.
- `progresso.md` tem entrada de historico da 4.10 — linha da 4.11 entra **acima**.
- `CLAUDE.md` tem secao "## Convencoes e padroes" com subsecoes "Branches", "Commits", "Sub-etapas", "Validacao destrutiva (ADR-011)", "Decisao silenciosa em zona limitrofe". Nova subsecao "### Subagents e skills" entra **entre** "### Sub-etapas" e "### Validacao destrutiva (ADR-011)".
- `.claude/agents/pr-reviewer.md` permanece com frontmatter `model: haiku`. **Nao sera modificado nesta sub-etapa.**
- `.claude/skills/local/` e `.claude/skills/universal/`: listar conteudo. Esperado: vazias ou com apenas `.gitkeep`/`.gitignore`. Se houver arquivo nao previsto, **parar e reportar**.
- `.claude/skills/` contem apenas `local/` e `universal/` no nivel raiz. Sera adicionado `review-pr/` na Tarefa 6.

Se alguma divergencia, **parar e reportar**.

### Tarefa 5 — Errata do ADR-012 em `docs/adrs.md`

Copiar bloco "Errata do ADR-012" do escopo decidido acima. Adicionar **apos** a secao "Consequencias" do ADR-012, preservando todo o texto original. Encoding UTF-8 sem BOM. Linhas em branco antes/depois de headers.

**Pre-condicao ADR-011 apos editar:**

```powershell
Test-Path docs\adrs.md   # True

$bytes = [System.IO.File]::ReadAllBytes("docs/adrs.md")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191 (sem BOM)

$content = [System.IO.File]::ReadAllText("docs/adrs.md", [System.Text.UTF8Encoding]::new($false))

# Revisao da 4.11 presente
if ($content -match 'Revisao 2026-05-11 \(Sub-etapa 4\.11\)') {
    Write-Host "Revisao 4.11 OK"
} else {
    Write-Host "Revisao 4.11 AUSENTE"
}

# Mecanismo revisado presente
if ($content -match 'Mecanismo revisado') {
    Write-Host "Mecanismo revisado OK"
} else {
    Write-Host "Mecanismo revisado AUSENTE"
}

# Texto original do ADR-012 preservado (sem destruir Contexto/Decisao/Alternativas/Consequencias)
$adr012_original_markers = @('## ADR-012.{1,5}Subagents do projeto', '### Contexto', '### Decisao', '### Alternativas consideradas', '### Consequencias')
foreach ($marker in $adr012_original_markers) {
    if ($content -match $marker) {
        Write-Host "Original marker OK: $marker"
    } else {
        Write-Host "Original marker AUSENTE: $marker -- ADR original danificado"
    }
}
```

Se algum valor divergir, parar e reportar.

### Tarefa 6 — Criar `.claude/skills/review-pr/SKILL.md`

Criar diretorio e arquivo:

```powershell
New-Item -ItemType Directory -Path .claude\skills\review-pr -Force
```

Copiar bloco "Conteudo de `.claude/skills/review-pr/SKILL.md`" do escopo. Encoding UTF-8 sem BOM. Sem acentos.

**Pre-condicao ADR-011 apos criar:**

```powershell
Test-Path .claude\skills\review-pr\SKILL.md   # True

$bytes = [System.IO.File]::ReadAllBytes(".claude/skills/review-pr/SKILL.md")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191 (sem BOM)

$content = [System.IO.File]::ReadAllText(".claude/skills/review-pr/SKILL.md", [System.Text.UTF8Encoding]::new($false))

# Frontmatter completo
$markers = @('name: review-pr', 'disable-model-invocation: true', 'context: fork', 'agent: pr-reviewer', 'argument-hint: \[pr-number\]', 'allowed-tools:')
foreach ($marker in $markers) {
    if ($content -match $marker) {
        Write-Host "Frontmatter OK: $marker"
    } else {
        Write-Host "Frontmatter AUSENTE: $marker"
    }
}

# $ARGUMENTS presente no corpo
if ($content -match '\$ARGUMENTS') {
    Write-Host "Argumento OK"
} else {
    Write-Host "Argumento AUSENTE"
}

# Linhas totais (esperado: ~13-15)
$linhas = (Get-Content .claude\skills\review-pr\SKILL.md).Count
Write-Host "Linhas totais: $linhas (esperado: 13-15)"
```

Se algum valor divergir, parar e reportar.

### Tarefa 7 — Remover pastas orfas `.claude/skills/local/` e `.claude/skills/universal/`

**Validacao previa obrigatoria** (ja feita na Tarefa 4): conteudo das pastas confirmado como vazio ou apenas `.gitkeep`/`.gitignore`. Se houver duvida, voltar para Tarefa 4.

```bash
git rm -r .claude/skills/local
git rm -r .claude/skills/universal
```

**Pre-condicao ADR-011 apos remover:**

```powershell
Test-Path .claude\skills\local        # False
Test-Path .claude\skills\universal    # False
Test-Path .claude\skills\review-pr    # True (criado na Tarefa 6)

git status
# Esperado: arquivos de local/ e universal/ aparecem como "deleted"
# Esperado: .claude/skills/review-pr/SKILL.md aparece como "untracked"
```

Se algum valor divergir, parar e reportar.

### Tarefa 8 — Atualizar `CLAUDE.md` (subsecao "Subagents e skills")

Copiar bloco "Atualizacao em CLAUDE.md" do escopo. Inserir **entre** "### Sub-etapas" e "### Validacao destrutiva (ADR-011)".

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("CLAUDE.md", [System.Text.UTF8Encoding]::new($false))

if ($content -match '### Subagents e skills') {
    Write-Host "Subsecao OK"
} else {
    Write-Host "Subsecao AUSENTE"
}

# Ordem: Sub-etapas antes da nova antes de Validacao destrutiva
$posSubEtapas = $content.IndexOf('### Sub-etapas')
$posNova = $content.IndexOf('### Subagents e skills')
$posValidacao = $content.IndexOf('### Validacao destrutiva')
if ($posSubEtapas -gt 0 -and $posSubEtapas -lt $posNova -and $posNova -lt $posValidacao) {
    Write-Host "Ordem OK"
} else {
    Write-Host "Ordem TROCADA"
}
```

### Tarefa 9 — Atualizar `docs/decisoes.md` (subsecao 4.11)

Copiar bloco "Conteudo da subsecao em decisoes.md" do escopo. Inserir **antes** da linha `### Claude Code hooks nativos`, **apos** a subsecao "Auditoria meta-operacional (Sub-etapa 4.10)".

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/decisoes.md", [System.Text.UTF8Encoding]::new($false))

if ($content -match '### Primeira skill orquestradora') {
    Write-Host "Subsecao 4.11 OK"
} else {
    Write-Host "Subsecao 4.11 AUSENTE"
}

# Ordem: 4.10 antes da 4.11 antes de hooks nativos
$pos410 = $content.IndexOf('Auditoria meta-operacional')
$pos411 = $content.IndexOf('Primeira skill orquestradora')
$posNativos = $content.IndexOf('Claude Code hooks nativos')
if ($pos410 -lt $pos411 -and $pos411 -lt $posNativos) {
    Write-Host "Ordem OK"
} else {
    Write-Host "Ordem TROCADA -- investigar"
}
```

### Tarefa 10 — Atualizar `docs/progresso.md` (4 edicoes)

Aplicar **edicoes 1-4** descritas no escopo, na ordem:

1. Adicionar sub-etapa 4.11 ao topo de "Sub-etapas concluidas" (acima da 4.10).
2. Substituir bloco "Criterios de 'pronto'" da Camada 3 pela versao ajustada 4.10+4.11.
3. Adicionar "Licoes da Sub-etapa 4.11" acima de "Licoes da Sub-etapa 4.10".
4. Adicionar linha de historico acima da entrada da 4.10.

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/progresso.md", [System.Text.UTF8Encoding]::new($false))

# Sub-etapa 4.11 presente
if ($content -match '4\.11.{1,5}Primeira skill orquestradora') {
    Write-Host "Sub-etapa 4.11 OK"
} else {
    Write-Host "Sub-etapa 4.11 AUSENTE"
}

# Licoes da 4.11 presentes
if ($content -match '## Licoes da Sub-etapa 4\.11') {
    Write-Host "Licoes 4.11 OK"
} else {
    Write-Host "Licoes 4.11 AUSENTE"
}

# Criterios atualizados (review-pr concluido + smoke pendente)
if ($content -match 'Skill `/review-pr` orquestrando `pr-reviewer`.{1,30}concluido 4\.11') {
    Write-Host "Criterio /review-pr concluido OK"
} else {
    Write-Host "Criterio /review-pr concluido AUSENTE"
}

if ($content -match 'Smoke pos-merge da 4\.11') {
    Write-Host "Criterio smoke pendente OK"
} else {
    Write-Host "Criterio smoke pendente AUSENTE"
}

# Ordem cronologica descrescente (4.11 acima de 4.10)
$pos411 = $content.IndexOf('**4.11')
$pos410 = $content.IndexOf('**4.10')
if ($pos411 -gt 0 -and $pos411 -lt $pos410) {
    Write-Host "Ordem cronologica OK"
} else {
    Write-Host "Ordem cronologica TROCADA"
}

# Linhas totais (esperado: ~900 -- sobe do 854 atual com as adicoes)
$linhas = (Get-Content docs\progresso.md).Count
Write-Host "Linhas totais: $linhas"
```

**Atencao:** Hook 4.4 vai alertar — `progresso.md` ja estava em 854 linhas, agora cruza ~900. Modo warn, **nao bloqueia commit**. Comportamento esperado.

### Tarefa 11 — Versionar este proprio prompt

```bash
git status
```

Confirmar que `docs/prompt-etapa-4-11.md` aparece como **untracked**. Sera incluido no Commit 4.

### Tarefa 12 — Commits (4 commits)

**Commit 1** — Errata ADR-012:

```bash
git add docs/adrs.md
git status   # apenas adrs.md staged
git commit -m "docs(adr): errata ADR-012 -- mecanismo nativo context: fork (sub-etapa 4.11)"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 2** — Skill + cleanup:

```bash
git add .claude/skills/review-pr/SKILL.md
git add -u .claude/skills/local .claude/skills/universal
git status   # 1 arquivo novo + arquivos de local/universal como deleted
git commit -m "feat(claude): primeira skill orquestradora review-pr + remove pastas orfas"
```

**Pre-condicao ADR-011:** SKILL.md staged + arquivos removidos staged como deleted; `$LASTEXITCODE = 0`.

**Commit 3** — Docs (CLAUDE.md + decisoes + progresso):

```bash
git add CLAUDE.md docs/decisoes.md docs/progresso.md
git status   # 3 arquivos staged
git commit -m "docs: sub-etapa 4.11 -- registra primeira skill + atualiza CLAUDE.md (regra 4.6)"
```

**Pre-condicao ADR-011:** 3 arquivos staged; `$LASTEXITCODE = 0`.

**Atencao:** hook 4.4 pode alertar para `progresso.md` cruzar ~900 linhas. Nao bloqueia.

**Commit 4** — Versionar prompt:

```bash
git add docs/prompt-etapa-4-11.md
git status   # 1 arquivo staged
git commit -m "docs: versiona prompt-etapa-4-11.md"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Validacao implicita dos hooks ativos:**

- Conventional Commits (4.1): mensagens validas (`docs(adr):`, `feat(claude):`, `docs:`).
- Encoding UTF-8 (4.2): valida bytes em todos os arquivos editados/criados.
- Markdown blank lines (4.3): valida headers em `.md` editados.
- Tamanho de docs (4.4): pode alertar em `progresso.md`.
- Maven release (4.5), @Entity (4.7): nao se aplicam.

Se algum hook bloquear, parar e reportar.

### Tarefa 13 — Validacao final antes de push

```bash
git status
git log --oneline -5
git config core.hooksPath
```

```powershell
Test-Path .claude\skills\review-pr\SKILL.md      # True
Test-Path .claude\skills\local                    # False
Test-Path .claude\skills\universal                # False
(Get-Content .claude\skills\review-pr\SKILL.md).Count   # 13-15
```

Esperado:

- Working tree limpo.
- 4 commits novos.
- `core.hooksPath` = `.githooks`.
- Skill nova existe; pastas orfas removidas.

## Restricoes e freios

1. **NAO modificar `.claude/agents/pr-reviewer.md`.** Subagent existente permanece intacto. Frontmatter `model: haiku` preservado.

2. **Errata do ADR-012 preserva o texto original.** Adicionar revisao **depois** de "Consequencias". Nao apagar, reescrever ou refatorar o ADR-012 original. Decisao estrutural permanece — apenas o mecanismo literal e refinado.

3. **NAO modificar outros ADRs.** Apenas anexar revisao ao ADR-012.

4. **NAO criar outras skills, subagents, hooks** nesta sub-etapa. Apenas `review-pr`.

5. **NAO mexer em `~/.claude/`** (memoria global, plugins globais, built-ins). Debitos da 4.10 permanecem como debitos.

6. **NAO executar a skill** (`/review-pr`) nesta sub-etapa. Smoke pos-merge e responsabilidade do operador apos autorizar merge.

7. **NAO atualizar `docs/hooks-pendentes.md`** nesta sub-etapa. Sem novo debito; debitos meta-operacionais da 4.10 permanecem ate o smoke pos-merge da 4.11 sugerir mitigacao ou aceitacao.

8. **Skill content deve ser CURTO.** ~10 linhas no body apos frontmatter. System prompt do subagent ja contem toda a logica — duplicar e gerar divergencia.

9. **Sem acentos** no SKILL.md (alinhado com restante do projeto, evita gotchas de encoding em ferramentas Windows-nativas).

10. **Frontmatter na ordem exata prescrita:** `name`, `description`, `disable-model-invocation`, `context`, `agent`, `argument-hint`, `allowed-tools`. Frontmatter YAML — chaves em snake-kebab-case conforme doc oficial.

11. **`disable-model-invocation: true` e obrigatorio.** Sem isso, a skill pode ser invocada automaticamente pelo Claude via description matching, reintroduzindo nao-determinismo.

12. **Pre-validacao das pastas orfas (`Get-ChildItem` na Tarefa 4) antes de remover.** Se houver arquivo nao previsto, parar e reportar — pode ser intervencao nao registrada.

13. **CLAUDE.md atualizado de forma minima.** Subsecao "Subagents e skills" com 3-4 linhas. Nao reescrever outras subsecoes.

14. **Encoding UTF-8 sem BOM** em todos os arquivos editados/criados.

15. **Apenas ASCII em mensagens de commit.** Sem acentos, sem em-dash U+2014.

16. **Ordem cronologica descrescente** em todos os blocos de historico de `progresso.md`.

17. **Sem cenarios destrutivos tradicionais.** Sub-etapa cria componente novo (skill); smoke pos-merge valida funcionamento. Pre-condicoes ADR-011 em cada Tarefa.

18. **Hook 4.4 vai alertar** em `progresso.md`. Comportamento esperado. **Nao bloqueia commit.**

19. **Nao sugerir proxima sub-etapa** espontaneamente alem do candidato natural ja citado (smoke pos-merge + eventual 4.11.1 de refinamento se smoke revelar suboptimo).

20. **Nao tomar decisao silenciosa em zona limitrofe.** Reportar divergencias.

21. **Nao usar `pwsh`.** PowerShell 5.1 (`powershell`).

22. **Nao usar `git reset --hard`.**

23. **Nao usar `git commit --no-verify`.**

## Estrutura de commits

Branch: `feat/etapa-4-11-skill-review-pr`

**Commit 1** — `docs(adr): errata ADR-012 -- mecanismo nativo context: fork (sub-etapa 4.11)`

- `docs/adrs.md` (revisao anexada ao ADR-012)

**Commit 2** — `feat(claude): primeira skill orquestradora review-pr + remove pastas orfas`

- `.claude/skills/review-pr/SKILL.md` (novo)
- `.claude/skills/local/` (removido)
- `.claude/skills/universal/` (removido)

**Commit 3** — `docs: sub-etapa 4.11 -- registra primeira skill + atualiza CLAUDE.md (regra 4.6)`

- `CLAUDE.md` (subsecao "Subagents e skills")
- `docs/decisoes.md` (subsecao 4.11)
- `docs/progresso.md` (sub-etapa 4.11 + criterios + licoes + historico)

**Commit 4** — `docs: versiona prompt-etapa-4-11.md`

- `docs/prompt-etapa-4-11.md` (novo)

## Validacao antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -5
git config core.hooksPath
```

```powershell
Test-Path .claude\skills\review-pr\SKILL.md
Test-Path .claude\skills\local
Test-Path .claude\skills\universal
```

Esperado:

- `check.ps1` passa.
- Working tree limpo.
- 4 commits novos.
- Skill criada; pastas orfas removidas.

## PR

Titulo: `feat: sub-etapa 4.11 -- primeira skill orquestradora /review-pr + errata ADR-012`

Body sugerido:

````markdown
## Summary

Primeira skill orquestradora do projeto + primeira implementacao do padrao decidido em ADR-012 (Sub-etapa 4.10). Tres entregas em um unico PR:

1. **Errata do ADR-012** — mecanismo nativo `context: fork` substitui invocacao textual via Task tool. Decisao estrutural preservada, mecanismo refinado apos investigacao da doc oficial.
2. **Skill `/review-pr`** em `.claude/skills/review-pr/SKILL.md` (flat). Frontmatter declara `context: fork` + `agent: pr-reviewer` + `disable-model-invocation: true`.
3. **Cleanup das pastas orfas** `.claude/skills/local/` e `.claude/skills/universal/`. Skills sao flat — analogo aos subagents.

CLAUDE.md atualizado com subsecao "Subagents e skills" (regra 4.6).

### Por que esta sub-etapa nasceu refinada

Pre-redacao do prompt da 4.11 incluiu investigacao da documentacao oficial do Claude Code (https://code.claude.com/docs/en/skills). Tres descobertas reconfiguraram o escopo previsto:

1. **Skills sao flat em `.claude/skills/<nome>/SKILL.md`.** Subpastas `local/` e `universal/` da 4.0 nao geram slash commands validos.
2. **Mecanismo nativo `context: fork` + `agent: <nome>`** via frontmatter substitui invocacao textual ("Use a Task tool...") prescrita no ADR-012. Determinismo arquitetural — elimina o nao-determinismo que o ADR buscava resolver.
3. **`disable-model-invocation: true`** elimina auto-discovery via description matching. Skill so dispara via slash command explicito do operador.

### Errata do ADR-012

Anexada **apos** a secao "Consequencias" do ADR-012, preservando texto original.

**Mudancas:**

- Path `.claude/skills/<escopo>/<nome>.md` corrigido para `.claude/skills/<nome>/SKILL.md` (flat).
- Mecanismo (passos 1-5) revisado: `context: fork` + `agent` no frontmatter substitui instrucao textual + Task tool.
- Padrao obrigatorio 2 reescrito.
- Nao-mecanismo expandido para incluir explicitamente a instrucao textual como descartada.
- Recomendacao adicional: `disable-model-invocation: true` em skills orquestradoras de subagent.

**Decisao estrutural preservada.** Skill orquestra subagent permanece. Padroes obrigatorios 1, 3, 4 mantidos.

### Categoria nova: "errata de ADR baseada em descoberta de documentacao oficial"

Distinta de:

- **"Patch tecnico"** (4.0.1): corrige bug do entregue.
- **"Refinamento pos-smoke empirico"** (4.9.1): smoke revelou suboptimo.
- **"Auditoria meta-operacional"** (4.10): descobertas afetam estrategia de camada.
- **Esta categoria:** decisao estrutural preservada, mecanismo literal refinado apos consulta a doc oficial. Aplicavel a futuros ADRs cuja prescricao se mostre tecnicamente incorreta.

### Conteudo da skill (`.claude/skills/review-pr/SKILL.md`)

Frontmatter (7 campos) + body curto (~5 linhas). System prompt do subagent (`pr-reviewer.md`) ja contem toda a logica de revisao — skill apenas entrega a tarefa concreta + reforca template. Evita duplicacao.

```yaml
---
name: review-pr
description: Revisa o PR informado via subagent pr-reviewer (Haiku). Use antes do merge.
disable-model-invocation: true
context: fork
agent: pr-reviewer
argument-hint: [pr-number]
allowed-tools: Bash(gh pr view *) Bash(gh pr diff *)
---
```

### Mudancas

- `docs/adrs.md`: errata anexada ao ADR-012.
- `.claude/skills/review-pr/SKILL.md`: novo (~13-15 linhas).
- `.claude/skills/local/`: removida via `git rm -r`.
- `.claude/skills/universal/`: removida via `git rm -r`.
- `CLAUDE.md`: subsecao "Subagents e skills" adicionada em "Convencoes e padroes" (~4 linhas).
- `docs/decisoes.md`: subsecao "Primeira skill orquestradora: /review-pr (Sub-etapa 4.11)".
- `docs/progresso.md`: sub-etapa 4.11 + criterios Camada 3 ajustados + 3 licoes + historico.
- `docs/prompt-etapa-4-11.md`: prompt versionado.

### Smoke test pos-merge (responsabilidade do operador)

1. Sessao nova do Claude Code.
2. Abrir PR de teste trivial em branch nova.
3. Invocar `/review-pr <numero>`.
4. **Criterios de sucesso:**
   - Skill dispara fork no agent `pr-reviewer` (Haiku) — sem execucao direta pelo Claude principal.
   - Output usa exatamente as 3 secoes (Bloqueadores, Sugestoes, Elogios).
   - Sem secoes extras (Visao Geral, Analise, Conclusao).
5. **Risco residual:** debitos meta-operacionais da 4.10 (memoria global, plugins, built-ins) nao mitigados. Smoke positivo valida funcionamento do par skill+subagent, nao determinismo absoluto.

### Hook 4.4 alerta esperado

`progresso.md` cruza ~900 linhas com as adicoes (modo warn, nao bloqueia).

### Proximo passo

Decisao fora deste PR. Candidatos naturais:

- **Smoke pos-merge da 4.11** (responsabilidade do operador apos merge).
- **4.11.1** (refactor pos-smoke) se smoke revelar suboptimo no output ou na invocacao.
- **4.12** (segundo subagent `architect-reviewer` + skill `/review-arch`) — replicar padrao validado.
````

## Pos-criacao do PR

1. Abrir PR via `gh pr create`.
2. Capturar o numero.
3. Editar `docs/decisoes.md` e `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorizacao explicita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `feat/etapa-4-11-skill-review-pr` empurrada com 5 commits (4 + 1 update do PR).
- PR aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.10.
- Working tree limpo.
- `.claude/skills/review-pr/SKILL.md` existe (~13-15 linhas).
- `.claude/skills/local/` e `.claude/skills/universal/` **nao existem mais** no working tree.
- `.claude/agents/pr-reviewer.md` inalterado.
- Reportar: `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup`, `Test-Path .claude\skills\review-pr\SKILL.md`.

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao executar a skill `/review-pr` (smoke e pos-merge).
- Nao criar prompt da 4.11.1 ou 4.12.
- Nao criar outras skills, subagents, hooks, MCPs.
- Nao modificar `.claude/agents/pr-reviewer.md`.
- Nao mexer em `~/.claude/` global.
- Nao atualizar blueprint, `.gitignore`, `.gitattributes`, `hooks-pendentes.md`.
- Nao sugerir proximo passo espontaneamente alem dos candidatos ja citados no PR body.
- Nao usar `pwsh`.
- Nao usar `git reset --hard`.
- Nao usar `git commit --no-verify`.
