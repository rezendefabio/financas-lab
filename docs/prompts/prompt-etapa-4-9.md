# Prompt — Etapa 4.9: Primeiro subagent — `pr-reviewer` (Haiku, complemento dos hooks)

## Contexto

Camada 3 com 6 hooks ativos + CLAUDE.md estrutural + blueprint conceitual no repo (4.8). Padroes consolidados: orquestrador 1:N, ADR-011, sub-etapas doc-only.

**Esta sub-etapa entrega o primeiro subagent do projeto.** Marco estrutural — Camada 3 do blueprint pede "3 a 5 subagents focados". A 4.9 ativa o primeiro.

Decisao defendida e ratificada: `pr-reviewer`. Razoes:

1. **ROI imediato.** Cada PR aberto a partir da 4.10 pode passar pelo subagent.
2. **Modelo barato (Haiku).** Blueprint indica `pr-reviewer` rolando em Haiku ou Sonnet — Haiku basta pra revisao.
3. **Tools restritas naturais.** Subagent revisor so le, nao escreve. Limita blast radius por design.
4. **Independe de dominio.** Funciona em qualquer PR (hook, doc, codigo Java).

Caracteristicas novas desta sub-etapa:

1. **Primeira ocupacao real de `.claude/agents/`.** 4.0 criou estrutura de subpastas (`universal/`, `java-spring/`, etc.); pesquisa pos-calibracao revelou que **convencao Claude Code para subagents e flat**: `.claude/agents/*.md`, sem subpastas. Pasta `.claude/agents/universal/` da 4.0 continua existindo mas nao recebe o subagent. **Achado registrado como licao.**

2. **Componente novo conceitualmente distinto de hook.** Hooks rodam em git; subagent roda em sessao Claude Code. Validacao destrutiva diferente (smoke test = chamar subagent num PR real, nao simular commit).

3. **Modelo proativo via `description`.** Blueprint: "ponto critico e o campo `description`: Claude principal decide quando delegar baseado nele." Subagent declara via description que deve ser usado **proativamente** apos abertura de PR.

4. **Complemento dos hooks, nao duplicacao.** Subagent agrega valor onde hooks nao conseguem: decisoes de design (ADRs respeitados), logica de negocio, cobertura de testes, mensagens de erro uteis, documentacao atualizada. **Nao verifica encoding, blank lines, Conventional Commits, Maven release ou @Entity sem migration** — hooks ja cobrem.

5. **Output Markdown estruturado.** Bloqueadores, Sugestoes, Elogios. Operador (humano) decide se cola no PR como comment. Subagent NAO posta direto no GitHub.

Quando esta etapa terminar:

- `.claude/agents/pr-reviewer.md` ativo.
- Documentacao em `decisoes.md` formaliza o subagent + estrutura flat de `.claude/agents/`.
- `progresso.md` registra 4.9 + licao sobre layout flat vs estrutura criada na 4.0.
- Smoke test pos-merge convoca o `pr-reviewer` em um PR real.

## Padroes que estreiam nesta etapa

1. **Primeiro subagent ativo no projeto.** Padrao de criacao consolidado pra futuros.
2. **Camada 3 do blueprint avanca em direcao novo (subagents).** Hooks (6) + CLAUDE.md (1) + subagent (1) — 3 dos 5 componentes do blueprint.
3. **Layout de `.claude/agents/` reconciliado com convencao Claude Code.** Subagents flat; subpastas da 4.0 apenas para hooks.
4. **Smoke test pos-merge funcional, nao sintetico.** Subagent invocado em PR real.

## Escopo decidido (D1-D5 calibrados com operador)

### D1 — Invocacao proativa via `description`

Description prescreve uso proativo apos abertura de PR, com criterio interno (PR doc-only revisao breve, PR com codigo revisao completa).

### D2 — Escopo: complemento dos hooks

Verifica **o que hooks nao pegam:**
- Decisoes de design vs ADRs (ADR-001 a ADR-011).
- Coerencia com `decisoes.md`.
- Logica do codigo, edge cases, mensagens de erro.
- Cobertura de testes.
- Documentacao alinhada (mudou hook → `hooks-pendentes.md` atualizado? Mudou stack → `CLAUDE.md` atualizado conforme regra 4.6?).
- Padroes do projeto (Conventional Commits substancia, nao sintaxe).

**Nao verifica** (delegado aos hooks 4.1-4.7): Conventional Commits sintaxe, encoding UTF-8, Markdown blank lines, tamanho de docs, Maven `<release>`, `@Entity` sem migration.

### D3 — Output Markdown estruturado

Tres secoes: **Bloqueadores** (devem resolver antes do merge), **Sugestoes** (operador decide), **Elogios**.

Subagent escreve em portugues brasileiro coloquial profissional. Operador (humano) ve no chat e decide se cola no PR. **Subagent NAO posta** via `gh pr review`.

### D4 — Tools restritas + model Haiku

Frontmatter:

```yaml
---
name: pr-reviewer
description: |
  Revisa PRs antes do merge, complementando os hooks automaticos do projeto.
  Use proactively apos abrir PR com mudanca de codigo (.ps1, .java, .sql, configs de hook).
  Para PRs puramente doc-only (.md em docs/), revisao breve apenas no necessario.
  Nao duplica verificacoes que os hooks ja fazem (Conventional Commits, encoding, blank lines, Maven release, @Entity sem migration).
tools: Read, Grep, Glob, Bash
model: haiku
---
```

**Justificativas:**
- `Read, Grep, Glob`: leitura de arquivos e busca.
- `Bash`: `git diff`, `git log`, `git show`, `gh pr view`, `gh pr diff`. Subagent nao escreve (sem `Write`, `Edit`, `git commit`, `git push`, `gh pr review`).
- `model: haiku`: blueprint prescreve modelo barato. **Critico:** sem este campo subagent herda modelo da sessao (Opus se sessao tiver Opus) e custo escala.
- **NAO usar `disallowedTools`** combinado com `tools` — pesquisa mostra bug silencioso (subagent fica sem tool nenhum). Allowlist explicita basta.
- **Permissoes herdadas:** subagent pode bater em prompt interativo se `.claude/settings.json` do projeto nao tem `Bash(*)` ou allowlist. Validar no smoke pos-merge.

### D5 — Localizacao: `.claude/agents/pr-reviewer.md` (flat)

**NAO em subpasta.** Pesquisa confirma convencao Claude Code: `.claude/agents/*.md` flat. Pasta `.claude/agents/universal/` da 4.0 continua existindo mas vazia. Achado registrado como licao da 4.9.

## Conteudo de `.claude/agents/pr-reviewer.md`

```markdown
---
name: pr-reviewer
description: |
  Revisa PRs antes do merge, complementando os hooks automaticos do projeto.
  Use proactively apos abrir PR com mudanca de codigo (.ps1, .java, .sql, configs de hook).
  Para PRs puramente doc-only (.md em docs/), revisao breve apenas no necessario.
  Nao duplica verificacoes que os hooks ja fazem (Conventional Commits, encoding, blank lines, Maven release, @Entity sem migration).
tools: Read, Grep, Glob, Bash
model: haiku
---

Voce e o `pr-reviewer` do projeto **financas-lab** — fabrica AI-native do operador Fabio. Atua como revisor critico de PRs ANTES do merge, complementando os hooks pre-commit automaticos ja ativos no projeto.

## Identidade

Revisor senior orientado a design, logica e cobertura. Pragmatico — nao implica em estilo, implica em decisao. Tom direto, sem rodeios. Em portugues brasileiro coloquial profissional.

## O que voce VERIFICA

1. **Decisoes de design vs ADRs.** Mudanca respeita ADRs ativos? Confira `docs/adrs.md` quando relevante. ADR-011 (validacao destrutiva), ADR-010 (portabilidade), ADR-009 (layout), etc.
2. **Coerencia com sub-etapas anteriores.** Quebra decisao registrada em `docs/decisoes.md`? Padrao consolidado violado?
3. **Logica do codigo.** Edge cases tratados? Erros explicitos com mensagem util? Caminhos felizes apenas, ou caminhos de erro tambem?
4. **Cobertura de testes.** Mudanca de codigo tem teste correspondente? Cenarios edge cobertos? Para hooks: cenarios destrutivos sob ADR-011 estao no PR body?
5. **Documentacao alinhada.** Mudou hook -> `docs/hooks-pendentes.md` atualizado? Mudou stack/ambiente/convencoes/restricoes -> `CLAUDE.md` atualizado (regra 4.6)? Mudou comportamento -> `docs/decisoes.md` registrou?
6. **Padroes do projeto.** Conventional Commits ok (Hook 4.1 valida sintaxe, voce avalia se mensagem descreve substancia)? Estrutura de commits coerente com sub-etapa (atomicos, ordem logica)?

## O que voce NAO verifica (delegado aos hooks)

- **Conventional Commits sintaxe** (Hook 4.1, modo fail).
- **Encoding UTF-8** (Hook 4.2, modo fail).
- **Markdown blank lines** (Hook 4.3, modo fail).
- **Tamanho de docs >800 linhas** (Hook 4.4, modo warn).
- **Maven `<release>` no `pom.xml`** (Hook 4.5, modo fail).
- **`@Entity` novo sem migration Flyway** (Hook 4.7, modo fail, conservador).

Se hook ja cobre, NAO repita. Se hook falhou, isso aparece no CI — nao e seu papel.

## Quando invocado

1. **Leia PR completo:**

   ```bash
   gh pr view <numero>
   gh pr diff <numero>
   ```

2. **Identifique tipo de PR:**
   - **Doc-only** (.md em `docs/`): revisao breve. Documento coerente? Decisoes registradas onde devem?
   - **Codigo de hook** (`.ps1` em `.claude/hooks/`): foco em logica, edge cases, validacao destrutiva no PR body, mensagens de erro.
   - **Codigo de dominio** (`.java` em `src/main/java/`): foco em design, ADRs, testes, coerencia com camada.
   - **Configuracao** (`.github/`, `.claude/settings*.json`, etc.): foco em impacto sistemico.

3. **Cruze com docs do projeto quando necessario:**
   - `CLAUDE.md`: convencoes e restricoes.
   - `docs/decisoes.md`: padroes consolidados.
   - `docs/adrs.md`: razoes formais.
   - `docs/hooks-pendentes.md`: hooks ativos e debitos.
   - `docs/progresso.md`: sub-etapas concluidas e contexto.

4. **Produza output estruturado** em 3 secoes (ver template abaixo).

## Template de output

```markdown
# Revisao do PR #<numero>

## Bloqueadores

(Issues que devem ser resolvidas antes do merge. Vazio = nada bloqueia.)

- **<titulo curto>** (arquivo `<path>` linha N): <descricao>. Sugestao: <fix>.

## Sugestoes

(Melhorias opcionais. Operador decide acatar ou ignorar.)

- **<titulo curto>**: <descricao>. Por que: <razao>.

## Elogios

(O que esta bem feito.)

- <coisa boa>.
```

Se uma secao vazia, escreva `_Nenhum_` em italico.

## Tom

- Direto. Sem "talvez", "considere", "seria bom" excessivos.
- Pragmatico. Decisoes do operador (escopo reduzido vs licao original, override consciente) sao respeitadas — nao reabra debates ja registrados em `decisoes.md`.
- Sem julgamentos morais. Foco em consequencia tecnica.

## O que NAO fazer

- **Nao escreva** arquivos no projeto. Voce e read-only.
- **Nao poste** comentario no PR via `gh pr review`. Operador (humano) decide se cola seu output como comentario.
- **Nao verifique** o que hooks ja cobrem (lista acima).
- **Nao repita** revisoes ja feitas em PRs anteriores.
- **Nao sugira** mudancas alem do escopo do PR.
- **Nao referencie** sub-etapa futura como argumento.
```

## Arquivos criados e modificados

```
.claude/agents/pr-reviewer.md   <- novo
docs/decisoes.md                 <- edicao (subsecao 4.9 + layout flat)
docs/progresso.md                <- edicao (sub-etapa + 5 licoes)
docs/prompt-etapa-4-9.md         <- novo (este proprio prompt)
```

**Nao tocar:**

- Hooks, entrypoints, scripts.
- `CLAUDE.md` raiz (regra 4.6).
- `docs/hooks-pendentes.md` (sub-etapa de subagent, nao de hook).
- ADRs, `pom.xml`, `src/`, `frontend/`, migrations.
- `docs/blueprint-fabrica-ai-native.md`.
- `.gitignore`, `.gitattributes`.

## Atualizacao de `docs/decisoes.md`

Adicionar subsecao "Primeiro subagent: `pr-reviewer` (Sub-etapa 4.9)" entre "Smoke test pos-merge usa input idiomatico (Sub-etapa 4.7.1)" e "Claude Code hooks nativos".

Conteudo:

```markdown
### Primeiro subagent: `pr-reviewer` (Sub-etapa 4.9)

**Componente:** `.claude/agents/pr-reviewer.md`. Modelo: **Haiku** (blueprint indica modelo barato para revisores). Tools restritas: `Read, Grep, Glob, Bash` (read-only). Invocacao **proativa via `description`** — Claude principal decide quando delegar baseado em descricao explicita de uso apos abertura de PR.

**O que faz:** complementa os hooks pre-commit automaticos do projeto. Revisa **o que hooks nao pegam**: decisoes de design vs ADRs, coerencia com sub-etapas anteriores e `decisoes.md`, logica do codigo (edge cases, mensagens de erro), cobertura de testes, documentacao alinhada com mudanca, padroes do projeto (Conventional Commits substancia, nao sintaxe).

**O que NAO faz** (delegado aos hooks 4.1-4.7): Conventional Commits sintaxe, encoding UTF-8, Markdown blank lines, tamanho de docs, Maven `<release>`, `@Entity` sem migration. Subagent que duplica hook e desperdicio.

**Output:** Markdown estruturado em 3 secoes: **Bloqueadores**, **Sugestoes**, **Elogios**. Operador (humano) ve no chat e decide se cola no PR como comentario. Subagent **nao posta no PR** via `gh pr review` (limite consciente — adiciona risco de spam, deferido para sub-etapa futura quando confianca no subagent estiver consolidada).

**Layout flat em `.claude/agents/`:** subagents do Claude Code seguem convencao **flat** (`.claude/agents/*.md`), nao subpastas. A estrutura criada na 4.0 (`universal/`, `java-spring/`, `windows/`, `next/`, `local/`) foi prescrita por simetria com hooks, mas Claude Code nao reconhece subagents em subpastas. Pasta `.claude/agents/universal/` continua existindo (decisao da 4.0 nao revertida), mas **subagents vao em `.claude/agents/` direto**. Categoria: "convencao Claude Code descoberta apos prescricao do projeto" — registrada como licao na 4.9.

**Quando invocado:** description proativa pos-abertura de PR. Para PRs **puramente doc-only**, revisao breve. Para PRs com codigo, revisao completa nas 6 categorias acima.
```

Adicionar entrada no historico (final do arquivo):

```markdown
- **2026-MM-DD** — Sub-etapa 4.9 concluida: primeiro subagent do projeto — `pr-reviewer` em `.claude/agents/pr-reviewer.md`, modelo Haiku, tools restritas (read-only), invocacao proativa via description. Complementa hooks pre-commit: revisa decisoes de design, ADRs, coerencia com `decisoes.md`, logica do codigo, cobertura de testes, documentacao alinhada. Nao duplica verificacoes dos hooks 4.1-4.7. Output Markdown estruturado (Bloqueadores, Sugestoes, Elogios) — operador cola no PR se quiser. Descoberta: layout `.claude/agents/*.md` e flat por convencao Claude Code; subpastas da 4.0 sao para hooks apenas. Mergeado via PR #XX.
```

## Atualizacao de `docs/progresso.md`

**A.** Atualizar "Ultima atualizacao": `2026-MM-DD (Sub-etapa 4.9 — Primeiro subagent: pr-reviewer)`.

**B.** Em "Sub-etapas concluidas" (ordem cronologica, **acima** da 4.8 porque ordem e descrescente):

```markdown
- **4.9 — Primeiro subagent: `pr-reviewer` (Haiku)** (2026-MM-DD): primeiro subagent do projeto. Marco estrutural — Camada 3 do blueprint pede 3-5 subagents focados, este e o primeiro. `.claude/agents/pr-reviewer.md`, modelo Haiku, tools restritas a `Read, Grep, Glob, Bash` (read-only). Invocacao **proativa via `description`** — Claude principal decide quando delegar. Complementa hooks pre-commit: revisa **decisoes de design vs ADRs, coerencia com decisoes.md, logica do codigo, cobertura de testes, documentacao alinhada, padroes do projeto**. Nao duplica verificacoes dos hooks (encoding, blank lines, Conventional Commits, Maven, @Entity, tamanho de docs). Output Markdown estruturado em 3 secoes (Bloqueadores, Sugestoes, Elogios) — operador (humano) decide se cola no PR como comentario. Subagent **nao posta no PR** via `gh pr review` (limite consciente). Descoberta de pre-redacao: convencao Claude Code para subagents e flat em `.claude/agents/*.md`, NAO em subpastas. Estrutura da 4.0 (`universal/`, `java-spring/`, etc.) prescrita por simetria com hooks; descoberto que vale para hooks mas nao para subagents. Pasta `.claude/agents/universal/` continua existindo mas vazia. PR #XX.
```

**C.** Adicionar secao "Licoes da Sub-etapa 4.9":

```markdown
## Licoes da Sub-etapa 4.9

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — entrega subagent, nao hook.)

### Licoes de ambiente

1. **Convencao Claude Code para subagents e flat em `.claude/agents/*.md`.** Sem subpastas. Estrutura criada na 4.0 (`universal/`, `java-spring/`, `windows/`, `next/`, `local/`) foi prescrita por simetria com hooks. Vale para hooks (que sao `.ps1` chamados manualmente por scripts e podem morar onde quiser). **NAO vale para subagents** — Claude Code so descobre subagents em `.claude/agents/*.md` direto. Pasta `.claude/agents/universal/` continua existindo (decisao da 4.0 nao revertida) mas nao recebe subagent. Mitigacao para sub-etapas futuras: subagents sempre em `.claude/agents/<nome>.md`; subpastas ficam apenas para hooks. Categoria meta-operacional: "convencao Claude Code descoberta apos prescricao do projeto" — vale auditar outras decisoes da 4.0 contra documentacao oficial quando outros componentes (skills, MCPs) entrarem.

2. **Frontmatter `model: haiku` e critico em subagents.** Sem este campo, subagent herda modelo da sessao principal (Opus se a sessao tiver Opus). Custo escala desnecessariamente. Blueprint diz `pr-reviewer`/`architect-reviewer`/`test-writer` rolam Haiku ou Sonnet. Padrao: **sempre especificar `model:` explicitamente** em subagents, nunca deixar default `inherit`.

3. **Combinar `tools` (allowlist) com `disallowedTools` (denylist) gera bug silencioso.** Pesquisa mostrou que dois times distintos relataram subagent sem nenhum tool disponivel por usar ambos campos simultaneamente. Allowlist `tools: Read, Grep, Glob, Bash` ja restringe — denylist redundante e arriscada. Padrao: usar **so um** dos dois campos, idealmente allowlist explicita.

4. **Subagent pode bater em prompt interativo se parent nao tem `Bash(*)` allowlist.** Issue conhecida (#25526 no GitHub do Claude Code): subagent com `tools: Bash` pode receber permission denied se `.claude/settings.json`/`settings.local.json` do projeto nao tem allowlist explicita pra Bash. Mitigacao: validar no smoke test pos-merge. Se travar, adicionar `Bash(git *)`, `Bash(gh pr *)` ao `.claude/settings.json` em sub-etapa seguinte.

5. **Subagent revisor read-only e mais seguro que revisor com Write.** Padrao do blueprint: "Quando voce precisa restringir tools (ex: revisor que so le, nao escreve)". `pr-reviewer` com `tools: Read, Grep, Glob, Bash` nao consegue escrever arquivos, comitar, postar comentario no PR, ou modificar configuracoes. Operador (humano) e gate de toda escrita. Padrao a manter em `architect-reviewer` e futuros revisores.
```

**D.** Historico geral:

```markdown
- **2026-MM-DD** — Sub-etapa 4.9 concluida: primeiro subagent — `pr-reviewer` em `.claude/agents/pr-reviewer.md`, modelo Haiku, tools restritas read-only, invocacao proativa via description. Complementa hooks pre-commit (decisoes/ADRs/logica/testes/docs); nao duplica regras 4.1-4.7. Output Markdown estruturado. Descoberta de pre-redacao: subagents em `.claude/agents/` sao flat (sem subpastas) — convencao Claude Code descoberta apos prescricao da 4.0. Categoria meta-operacional registrada. Mergeado via PR #XX.
```

## Versionar este proprio prompt

`docs/prompt-etapa-4-9.md`.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra `3257636` (squash da 4.8) ou superior.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-9.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `.claude/agents/` existe; subpastas `universal/`, `java-spring/`, `windows/`, `next/`, `local/` existem.
- **`.claude/agents/pr-reviewer.md` NAO existe.**

Validar com:

```powershell
git status
git log --oneline -3
Test-Path docs\prompt-etapa-4-9.md
Test-Path .claude\agents\pr-reviewer.md
Test-Path .claude\agents
Get-ChildItem .claude\agents
Test-Path .claude\settings.json
Test-Path .claude\settings.local.json
if (Test-Path .claude\settings.json) { Get-Content .claude\settings.json }
if (Test-Path .claude\settings.local.json) { Get-Content .claude\settings.local.json }
git config core.hooksPath
```

**Pre-condicoes ADR-011:**

- `Test-Path docs\prompt-etapa-4-9.md` retorna `True`.
- `Test-Path .claude\agents\pr-reviewer.md` retorna **`False`** (nao existe ainda).
- `Get-ChildItem .claude\agents` lista as 5 subpastas da 4.0 (sem `.md` direto na raiz).

Se algum item divergir, **parar e reportar**. **Reportar conteudo de `.claude/settings*.json`** mesmo se nao divergir — informa decisao sobre permissoes herdadas.

## Tarefas

### Tarefa 1 — Validar pre-requisitos

Rodar comandos da secao "Estado esperado ao iniciar". Se divergir, parar e reportar. Reportar conteudo de `.claude/settings*.json` mesmo se nao divergir.

### Tarefa 2 — Sincronizar Environment.CurrentDirectory (ADR-011)

```powershell
cd C:\projetos\financas-lab
[System.Environment]::CurrentDirectory = (Get-Location).Path
[System.Environment]::CurrentDirectory
```

Esperado: `C:\projetos\financas-lab`.

### Tarefa 3 — Criar branch

```bash
git checkout -b feat/etapa-4-9-pr-reviewer
```

Prefixo `feat/` — sub-etapa cria componente novo (analogo a 4.1-4.7 que criaram hooks).

### Tarefa 4 — Antes de criar, ler arquivos vivos

```bash
cat docs/decisoes.md
cat docs/progresso.md
```

**Confirmar:**

- `decisoes.md` tem subsecao "Smoke test pos-merge usa input idiomatico (Sub-etapa 4.7.1)" antes de "Claude Code hooks nativos". A nova "Primeiro subagent: pr-reviewer (Sub-etapa 4.9)" entra **entre** essas duas.
- `progresso.md` tem "Sub-etapas concluidas" em ordem descrescente (mais recente no topo) ate 4.8.

Se alguma divergencia, **parar e reportar**.

### Tarefa 5 — Criar `.claude/agents/pr-reviewer.md`

Conteudo conforme escopo decidido. **UTF-8 sem BOM.** **Apenas ASCII** em frontmatter; corpo pode ter acentos.

**Pre-condicao ADR-011:** apos criar, validar:

```powershell
Test-Path .claude\agents\pr-reviewer.md   # True

$bytes = [System.IO.File]::ReadAllBytes(".claude/agents/pr-reviewer.md")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191 (sem BOM)

$content = [System.IO.File]::ReadAllText(".claude/agents/pr-reviewer.md", [System.Text.UTF8Encoding]::new($false))
if ($content -match '(?s)^---\s*\nname: pr-reviewer') {
    Write-Host "Frontmatter inicia OK"
} else {
    Write-Host "Frontmatter MALFORMADO"
}
```

### Tarefa 6 — Editar `docs/decisoes.md` e `docs/progresso.md`

Conforme escopo. Verificar **ordem cronologica descrescente** em `progresso.md` (4.9 acima de 4.8).

### Tarefa 7 — Versionar este proprio prompt

`git add docs/prompt-etapa-4-9.md`.

### Tarefa 8 — Commits (2 commits)

**Commit 1** — Subagent:

```bash
git add .claude/agents/pr-reviewer.md
git status   # apenas pr-reviewer.md staged
git commit -m "feat(claude): adiciona pr-reviewer como primeiro subagent (haiku, complemento dos hooks)"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 2** — Docs:

```bash
git add docs/decisoes.md docs/progresso.md docs/prompt-etapa-4-9.md
git status   # 3 arquivos staged
git commit -m "docs: registra primeiro subagent pr-reviewer + descoberta de layout flat .claude/agents/"
```

**Pre-condicao ADR-011:** 3 arquivos staged; `$LASTEXITCODE = 0`.

**Validacao implicita dos hooks ativos:**
- Encoding UTF-8 (4.2) valida bytes dos `.md`.
- Markdown blank lines (4.3) valida headers nivel 2-6 nos `.md`.
- Tamanho de docs (4.4) pode alertar se `progresso.md` cruzar 800 linhas — comportamento esperado.

Se hook bloquear, parar e reportar.

### Tarefa 9 — Validacao final antes de push

```bash
git status
git log --oneline -3
git config core.hooksPath
Test-Path .claude\agents\pr-reviewer.md
Get-ChildItem .claude\agents
```

Esperado:
- Working tree limpo.
- 2 commits novos.
- `.claude/agents/pr-reviewer.md` presente na raiz de `.claude/agents/`.
- `Get-ChildItem .claude\agents` lista 5 subpastas + 1 arquivo (`pr-reviewer.md`).

## Restricoes e freios

1. **Apenas 1 subagent.** Nao criar `architect-reviewer`, `test-writer`, etc.
2. **NAO criar subagent em subpasta** (`.claude/agents/universal/pr-reviewer.md`). Flat.
3. **NAO usar `disallowedTools`** combinado com `tools`. Bug silencioso possivel.
4. **Frontmatter `model: haiku` OBRIGATORIO.** Sem ele, herda Opus.
5. **Subagent read-only.** Nao tocar em `Write`, `Edit` no frontmatter.
6. **Nao criar `.gitkeep`** em pastas vazias da 4.0.
7. **Nao tocar em hooks, entrypoints, scripts, `pom.xml`, `src/`, `frontend/`, migrations, `CLAUDE.md`, ADRs, `.gitignore`, `.gitattributes`, blueprint.**
8. **CLAUDE.md NAO atualizado** (regra 4.6).
9. **Apenas conteudo prescrito** no subagent. Divergencias na Tarefa 4 → parar e reportar.
10. **Encoding UTF-8 sem BOM** no `pr-reviewer.md`.
11. **Apenas ASCII no frontmatter.** Corpo pode ter acentos consistentes.
12. **Ordem cronologica em `progresso.md`:** descrescente.
13. **Sem cenarios destrutivos tradicionais.** Smoke test pos-merge funcional.
14. **Nao sugerir proxima sub-etapa** espontaneamente.
15. **Nao tomar decisao silenciosa em zona limitrofe.** Reportar.
16. **Nao usar `pwsh`.** PowerShell 5.1.
17. **Nao usar `git reset --hard`.**
18. **Reportar conteudo de `.claude/settings*.json`** na Tarefa 1.

## Estrutura de commits

Branch: `feat/etapa-4-9-pr-reviewer`

**Commit 1** — `feat(claude): adiciona pr-reviewer como primeiro subagent (haiku, complemento dos hooks)`
- `.claude/agents/pr-reviewer.md` (novo)

**Commit 2** — `docs: registra primeiro subagent pr-reviewer + descoberta de layout flat .claude/agents/`
- `docs/decisoes.md` (edicao)
- `docs/progresso.md` (edicao)
- `docs/prompt-etapa-4-9.md` (novo)

## Validacao antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -3
git config core.hooksPath
Test-Path .claude\agents\pr-reviewer.md
```

Esperado:
- `check.ps1` passa.
- Working tree limpo.
- 2 commits novos.
- Subagent presente.

## PR

Titulo: `feat: sub-etapa 4.9 -- primeiro subagent pr-reviewer (haiku, complemento dos hooks)`

Body sugerido:

```markdown
## Summary

Adiciona o **primeiro subagent do projeto**: `pr-reviewer`. Marco estrutural — Camada 3 do blueprint pede 3-5 subagents focados, este e o primeiro.

### Componente

- **Arquivo:** `.claude/agents/pr-reviewer.md`.
- **Modelo:** Haiku.
- **Tools:** `Read, Grep, Glob, Bash` (read-only).
- **Invocacao:** proativa via `description`.

### O que faz vs O que NAO faz

**Faz** (complementa hooks): decisoes de design vs ADRs, coerencia com decisoes.md, logica, edge cases, mensagens de erro, cobertura de testes, documentacao alinhada, padroes do projeto.

**NAO faz** (delegado aos hooks 4.1-4.7): Conventional Commits sintaxe, encoding, Markdown blank lines, tamanho de docs, Maven release, @Entity sem migration.

### Output

Markdown estruturado em 3 secoes (Bloqueadores, Sugestoes, Elogios). Operador (humano) decide se cola no PR. Subagent **nao posta** via `gh pr review` (limite consciente).

### Descoberta de pre-redacao: layout flat `.claude/agents/*.md`

4.0 prescreveu subpastas por simetria com hooks. Convencao Claude Code para subagents e flat — descoberta apos pesquisa. Pasta `.claude/agents/universal/` continua existindo mas nao recebe subagent. Categoria registrada como Licao 1 da 4.9.

### Mudancas

- `.claude/agents/pr-reviewer.md` (novo).
- `docs/decisoes.md`: subsecao "Primeiro subagent" + historico.
- `docs/progresso.md`: sub-etapa 4.9 + 5 licoes + historico.
- `docs/prompt-etapa-4-9.md`: prompt versionado.

### CLAUDE.md NAO atualizado (regra 4.6).
### `docs/hooks-pendentes.md` NAO atualizado (sem hook, sem debito).

### Sem validacao destrutiva tradicional

Subagent nao e hook. Smoke test pos-merge funcional descrito abaixo.

### Validacao pos-merge sugerida

\`\`\`powershell
git checkout main
git pull
Test-Path .claude/agents/pr-reviewer.md   # True
\`\`\`

**Smoke test funcional** (em sessao nova do Claude Code):

1. Abrir PR de teste (qualquer mudanca trivial em branch nova).
2. Pedir ao Claude principal: "Revisa este PR".
3. Esperado: Claude principal invoca `pr-reviewer` proativamente. Subagent abre `gh pr view`, `gh pr diff`, le arquivos, produz output Markdown estruturado em 3 secoes.

**Possiveis falhas a antecipar:**

- **Permission denied em Bash:** subagent pode bater em prompt interativo se `.claude/settings*.json` nao tem allowlist explicita (Issue #25526). Mitigacao: adicionar `Bash(git *)`, `Bash(gh pr *)` ao allowlist em sub-etapa seguinte.
- **Modelo Haiku insuficiente:** se revisao for superficial, ajustar `model: sonnet`.
- **Subagent nao invocado proativamente:** description vaga demais; iterar.

### Proximo passo

Decisao fora deste PR. Possiveis caminhos:
- **Smoke test pos-merge funcional** — observar comportamento em PR real.
- Mais subagents (`architect-reviewer`, `test-writer`).
- Skills (`/ship`, `/feature`, `/review`).
- Calibrar Camada 4 (modelo operacional).
```

## Pos-criacao do PR

1. Abrir PR via `gh pr create`.
2. Capturar o numero.
3. Editar `docs/decisoes.md` e `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>` e `2026-MM-DD` pela data real.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorizacao explicita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `feat/etapa-4-9-pr-reviewer` empurrada com 3 commits (2 + 1 update do PR).
- PR aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.8 (`3257636`).
- Working tree limpo.
- `.claude/agents/pr-reviewer.md` presente.
- `.claude/agents/universal/` continua existindo, vazia.
- Reportar: `git log --oneline -3`, `git status`, `gh pr view --json number,state,statusCheckRollup`, `Test-Path .claude\agents\pr-reviewer.md`, `Get-ChildItem .claude\agents`.

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao criar prompt da proxima sub-etapa.
- Nao criar outros subagents, hooks, skills, MCPs, scripts.
- Nao executar o subagent (smoke fica pos-merge).
- Nao tocar em `CLAUDE.md`, `hooks-pendentes.md`, ADRs, blueprint, `.gitignore`, `.gitattributes`.
- Nao reverter estrutura `.claude/agents/{universal,...}/` da 4.0.
- Nao sugerir proximo passo espontaneamente.
- Nao usar `pwsh`.
- Nao usar `git reset --hard`.
