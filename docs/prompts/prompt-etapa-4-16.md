# Prompt — Etapa 4.16: Split do `decisoes.md` por tema (Camada 3 vai para arquivo dedicado)

## Contexto

Camada 3 com 6 hooks (4.1-4.7, com 4.4 refinado pela 4.14) + CLAUDE.md + blueprint + 2 subagents (`pr-reviewer` Haiku + `architect-reviewer` Sonnet) + 2 skills orquestradoras (`/review-pr` + `/review-arch`) + ADR-012 (revisao 4.11) + `progresso-historico.md` (4.13) apos a 4.15.

`docs/decisoes.md` cresceu para ~829 linhas apos a 4.15 — hook 4.4 (modo warn) alertou no Commit 1 da 4.15 quando o arquivo cruzou 800. Trigger do debito registrado em `hooks-pendentes.md` desde a 4.13 ("Aplicar split analogo em `decisoes.md` quando cruzar 800 linhas").

**Diferenca qualitativa em relacao ao split do `progresso.md` (4.13):** o `decisoes.md` tem **dois tipos de conteudo qualitativamente distintos**:

- **Tipo 1 — Decisoes fundacionais** (Stack, Arquitetura, Convencoes de codigo, Convencoes operacionais, Politica de debito, Comandos atomicos, Frontend, Modelo financeiro, Principios do blueprint): convencoes do projeto **independentes de Camada**. Consultadas em qualquer sub-etapa para reabrir contexto.
- **Tipo 2 — Decisoes operacionais da Camada 3** (todas as subsecoes `(Sub-etapa 4.X)` + "Layout de `.claude/`" + "Mecanismo de git hooks no Windows" + "Debito de portabilidade" + "Claude Code hooks nativos"): convencoes especificas da Camada 3 do projeto.

Ambos os tipos sao **vivos** — nenhum e historico arquivado. Decisao da Sub-etapa 4.1 (Conventional Commits) continua valendo em todo commit; padrao de validacao destrutiva (4.2.1) e regra dura aplicada em toda sub-etapa.

**Criterio de corte: tema, nao idade.** O `progresso.md` foi cortado por idade (Camadas concluidas vao para historico arquivado). O `decisoes.md` e cortado por tema (decisoes operacionais de Claude Code separadas das decisoes fundacionais).

**Felizmente, a estrutura ja tem demarcacao H2 que facilita a operacao:** o cabecalho `## Camada 3 — Configuracao do Claude Code` (linha 345 no snapshot pre-4.10; mais adiante no vivo) delimita exatamente onde comecam as decisoes operacionais. Tudo abaixo desse H2, ate o proximo H2 (`## Principios herdados do blueprint`), e o que migra.

Caracteristicas:

1. **Replicacao do padrao 4.13 com criterio diferente.** Mesma categoria operacional ("manutencao de docs por crescimento"), mesmo padrao de execucao (criar arquivo novo, mover conteudo, atualizar referencias, registrar no CLAUDE.md), mas **criterio de corte temático em vez de cronologico**. Licao operacional registrada: criterio de split varia conforme natureza do documento.

2. **Resolve debito explicito da 4.13.** Item "Aplicar split analogo em `decisoes.md` quando cruzar 800 linhas" em `hooks-pendentes.md` (secao "Debitos meta-operacionais") sera removido. Padrao "debito originario X -> resolvido em X+N" (formalizado pela 4.14) replicado: debito da 4.13 resolvido pela 4.16.

3. **Mesma estrutura de execucao da 4.13.** Criar `docs/decisoes-claude-code.md` com a secao H2 movida; editar `docs/decisoes.md` removendo a secao e adicionando referencia cruzada; atualizar CLAUDE.md em "Onde buscar mais"; registrar sub-etapa em `progresso.md`; versionar prompt em `docs/prompts/`.

4. **Auto-referencia consistente.** A subsecao "Split do `decisoes.md` por tema (Sub-etapa 4.16)" entra no arquivo novo (`decisoes-claude-code.md`), nao no `decisoes.md` vivo — porque e decisao operacional de Camada 3, nao decisao fundacional. Padrao "sub-etapa que descreve o split entra no arquivo splittado".

Quando esta etapa terminar:

- `docs/decisoes-claude-code.md`: novo arquivo (~460-490 linhas) com todas as decisoes operacionais de Camada 3.
- `docs/decisoes.md`: enxuto (~370-400 linhas) com decisoes fundacionais. Hook 4.4 para de alertar.
- `CLAUDE.md`: secao "Onde buscar mais" ganha referencia ao `decisoes-claude-code.md`.
- `docs/hooks-pendentes.md`: debito da 4.13 removido.
- `docs/progresso.md`: sub-etapa 4.16 + licoes + historico.
- `docs/prompts/prompt-etapa-4-16.md`: versionado.

## Padroes que estreiam nesta etapa

1. **Split por tema (vs split por idade).** A 4.13 estabeleceu padrao "split por crescimento" usando criterio temporal (Camadas concluidas vao para historico). A 4.16 demonstra que o **criterio de corte varia conforme natureza do documento**:
   - `progresso.md`: cronologico por natureza (estado evolui no tempo) -> split por idade.
   - `decisoes.md`: tematico por natureza (decisoes categorizadas por dominio) -> split por tema.
   - `adrs.md` (se crescer): provavelmente tematico (decisoes por dominio de arquitetura).

   Padrao operacional consolidado: **antes de splittar documento, identificar criterio natural de corte conforme o que o documento e**.

2. **Auto-referencia em sub-etapa que altera estrutura de doc.** A subsecao que descreve a 4.16 entra no arquivo splittado (`decisoes-claude-code.md`), nao no `decisoes.md` vivo. Padrao "sub-etapa que altera estrutura registra a alteracao dentro da nova estrutura". Aplicavel a futuros splits, reorganizacoes ou movimentacoes estruturais de doc.

3. **Cadeia "debito originario X -> resolvido em X+N" replicada em segunda aplicacao.** A 4.14 resolveu debito originario da 4.13 (hook 4.4 excluir `docs/prompts/`). A 4.16 resolve debito originario da 4.13 (split de `decisoes.md`). Padrao consolidado por dupla aplicacao: debitos registrados em PR body ou em `hooks-pendentes.md` em sub-etapa X **sao naturalmente resolvidos** em sub-etapa X+N quando dor concreta dispara, sem necessidade de planejamento centralizado.

## Escopo decidido (calibrado com operador antes da redacao via D1-D8)

### Arquivos modificados

| Arquivo | Tipo de mudanca |
|---|---|
| `docs/decisoes-claude-code.md` | Novo (recebe secao H2 `## Camada 3 — Configuracao do Claude Code` inteira + subsecao 4.16) |
| `docs/decisoes.md` | Editado (remove secao H2 movida; adiciona nota de referencia cruzada; entrada no historico de mudancas) |
| `CLAUDE.md` | Acrescimo em "Onde buscar mais" referenciando `decisoes-claude-code.md` |
| `docs/hooks-pendentes.md` | Item "Aplicar split analogo em `decisoes.md`" removido |
| `docs/progresso.md` | Sub-etapa 4.16 + licoes + historico |
| `docs/prompts/prompt-etapa-4-16.md` | Versionado (novo) |

**Nao tocados:** `.claude/agents/`, `.claude/skills/`, `.claude/hooks/`, `.githooks/`, `pom.xml`, `src/`, `frontend/`, `docs/adrs.md`, `docs/visao.md`, `docs/blueprint-fabrica-ai-native.md`, `docs/progresso-historico.md`, `.gitignore`, `.gitattributes`.

### Estrutura de `docs/decisoes-claude-code.md` (novo)

Cabecalho:

```markdown
# Decisoes — Camada 3 (Configuracao do Claude Code)

> Documento dedicado a decisoes operacionais da Camada 3 do projeto: hooks, subagents, skills, padroes de validacao destrutiva, convencoes operacionais de Claude Code.
> Origem: separado de `docs/decisoes.md` na Sub-etapa 4.16 quando o arquivo original cruzou 800 linhas (trigger do hook 4.4 modo warn).
> Para decisoes fundacionais do projeto (Stack, Arquitetura, Convencoes de codigo, Politica de debito, Comandos atomicos, Frontend, Modelo financeiro, Principios do blueprint), ver `decisoes.md`.

**Data de criacao:** 2026-05-11 (Sub-etapa 4.16)

---
```

Em seguida, **conteudo integral** da secao H2 `## Camada 3 — Configuracao do Claude Code` do `decisoes.md` vivo. Inclui:

- O proprio cabecalho `## Camada 3 — Configuracao do Claude Code`.
- Todas as subsecoes ###  filhas dessa H2: Layout de `.claude/`, Mecanismo de git hooks no Windows, Debito de portabilidade, Conventional Commits (Sub-etapa 4.1), Encoding UTF-8 (Sub-etapa 4.2), Padrao orquestrador 1:N (Sub-etapa 4.2), Padroes de validacao destrutiva (Sub-etapa 4.2.1), Blank lines em Markdown (Sub-etapa 4.3), Tamanho de docs em modo warn (Sub-etapa 4.4), Maven release explicito (Sub-etapa 4.5), CLAUDE.md do projeto (Sub-etapa 4.6), @Entity sem migration Flyway (Sub-etapa 4.7), Smoke test pos-merge usa input idiomatico (Sub-etapa 4.7.1), Primeiro subagent: `pr-reviewer` (Sub-etapa 4.9), Refinamento do `pr-reviewer` pos-smoke (Sub-etapa 4.9.1), Auditoria meta-operacional + ADR-012 (Sub-etapa 4.10) com sua nota de errata adicionada pela 4.15, Primeira skill orquestradora: /review-pr (Sub-etapa 4.11), Segundo subagent: architect-reviewer + skill /review-arch (Sub-etapa 4.12), Manutencao de docs por crescimento (Sub-etapa 4.13), Hook 4.4 exclui `docs/prompts/` (Sub-etapa 4.14), Errata de auditoria meta-operacional (Sub-etapa 4.15), Claude Code hooks nativos.

**Preservacao byte-a-byte:** copiar conteudo integral, sem reformatar, sem reordenar, sem corrigir typos. Encoding UTF-8 sem BOM.

**Subsecao nova 4.16 entra ao final da H2 `## Camada 3 — Configuracao do Claude Code`, antes da subsecao `### Claude Code hooks nativos`** (que e a ultima da H2 original, mas a 4.16 entra antes dela para manter coerencia: hooks nativos discutem mecanismo que vale para qualquer sub-etapa; 4.16 e sub-etapa especifica). 

**Conteudo da subsecao 4.16 a inserir:**

```markdown
### Split do `decisoes.md` por tema (Sub-etapa 4.16)

Segunda aplicacao da categoria "manutencao de docs por crescimento" (consolidada pela 4.13). **Criterio de corte diferente:** enquanto a 4.13 cortou `progresso.md` por idade (Camadas concluidas viraram historico em `progresso-historico.md`), a 4.16 corta `decisoes.md` por **tema**.

**Por que tematico e nao cronologico:** o `decisoes.md` tem dois tipos qualitativamente distintos de conteudo:

- **Decisoes fundacionais** (Stack, Arquitetura, Convencoes de codigo, Convencoes operacionais, Politica de debito tecnico, Comandos atomicos, Frontend, Modelo financeiro, Principios do blueprint): convencoes do projeto consultadas em qualquer sub-etapa para reabrir contexto. Permanecem em `decisoes.md`.
- **Decisoes operacionais de Camada 3** (Layout de `.claude/`, mecanismo de git hooks, hooks 4.1-4.7, subagents 4.9-4.12, skills 4.11-4.12, padroes 4.13-4.15): convencoes especificas de Claude Code. Movidas para `decisoes-claude-code.md`.

**Ambos os tipos sao vivos.** Decisao de Conventional Commits (4.1) continua valendo em todo commit; padrao de validacao destrutiva (4.2.1) e regra dura aplicada em toda sub-etapa. Nao havia "historico" a arquivar — apenas dois conjuntos de decisoes vivas com escopos distintos. Padrao de split temporal da 4.13 nao se aplicava.

**Operacao facilitada por demarcacao H2 ja existente:** `decisoes.md` ja tinha cabecalho `## Camada 3 — Configuracao do Claude Code` delimitando exatamente a regiao a mover. Split foi extracao da H2 inteira (com todas suas subsecoes ### filhas), incluindo "Claude Code hooks nativos" que faz parte do mesmo escopo conceitual.

**`decisoes-claude-code.md` criado** (~460-490 linhas pos-split). `decisoes.md` enxuto (~370-400 linhas). Hook 4.4 para de alertar em ambos.

**CLAUDE.md atualizado** (regra 4.6 — 4.16 e sub-etapa causadora da convencao "decisoes separadas por tema" entrar em uso). Quarta atualizacao de CLAUDE.md no projeto (apos 4.6, 4.11, 4.13).

**Debito da 4.13 resolvido:** item "Aplicar split analogo em `decisoes.md` quando cruzar 800 linhas" removido de `hooks-pendentes.md` -> "Debitos meta-operacionais". Cadeia "debito originario X -> resolvido em X+N" replicada (4.14 ja havia resolvido outro debito originario da 4.13, sobre hook 4.4 excluir `docs/prompts/`).

**Auto-referencia consistente:** esta subsecao 4.16 esta no proprio `decisoes-claude-code.md` (arquivo splittado), nao no `decisoes.md` vivo. Padrao "sub-etapa que altera estrutura registra a alteracao dentro da nova estrutura".

**Padrao operacional consolidado: criterio de split varia conforme natureza do documento.**

- `progresso.md` (cronologico) -> split por idade (Camadas concluidas viram historico).
- `decisoes.md` (tematico) -> split por tema (decisoes operacionais separadas das fundacionais).
- `adrs.md` (se crescer no futuro) -> provavelmente split tematico por dominio de decisao.
- Outros docs futuros -> avaliar natureza antes de splittar.

**Recomendacao operacional registrada nas licoes 4.16:** antes de splittar documento que cruza limite de tamanho, identificar **criterio natural de corte conforme o que o documento e**. Replicar padrao cego (sempre por idade, ou sempre por tema) pode gerar split artificial.
```

### Mudancas em `docs/decisoes.md` (vivo)

**Remocao:** secao H2 `## Camada 3 — Configuracao do Claude Code` inteira (do cabecalho ate antes do proximo H2, que e `## Principios herdados do blueprint`). Inclui todas as subsecoes ### filhas listadas acima.

**Acrescimo 1 — Nota de referencia cruzada** apos a ultima H2 que permanece (`## Modelo financeiro do projeto`) e antes do proximo H2 (`## Principios herdados do blueprint`). Texto:

```markdown
## Decisoes operacionais da Camada 3

Decisoes especificas da Camada 3 (configuracao do Claude Code: hooks, subagents, skills, padroes operacionais) ficam em `decisoes-claude-code.md`. Separadas deste arquivo pela Sub-etapa 4.16 quando `decisoes.md` cruzou 800 linhas (trigger do hook 4.4 modo warn).
```

(Esse e um cabecalho H2 que substitui visualmente onde estava a H2 movida — entrega navegacao curta e ponteiro para o arquivo novo.)

**Acrescimo 2 — Linha no historico de mudancas** (`### Historico de mudancas` no final do `decisoes.md`). Adicionar **no topo do bloco** (ordem cronologica descrescente):

```markdown
- **2026-05-11** — Sub-etapa 4.16: split por tema. Secao `## Camada 3 — Configuracao do Claude Code` (com todas as subsecoes 4.X + Layout `.claude/` + Mecanismo de git hooks + Debito de portabilidade + Claude Code hooks nativos) movida para novo `decisoes-claude-code.md`. `decisoes.md` enxuto (~370-400 linhas) com decisoes fundacionais. Hook 4.4 deixa de alertar.
```

### Mudancas em `CLAUDE.md`

Na secao **"## Onde buscar mais"**, fazer **uma adicao**:

**Adicionar linha sobre `decisoes-claude-code.md`** logo apos a linha de `decisoes.md`:

Texto atual (aproximado — confirmar via leitura na Tarefa 4):

```markdown
- `decisoes.md` -- escolhas tomadas. Por que cada regra existe.
```

Texto novo (insere nova linha depois de `decisoes.md`):

```markdown
- `decisoes.md` -- escolhas tomadas. Por que cada regra existe (decisoes fundacionais: stack, arquitetura, convencoes).
- `decisoes-claude-code.md` -- decisoes operacionais da Camada 3 (hooks, subagents, skills, padroes de validacao destrutiva).
```

### Mudancas em `docs/hooks-pendentes.md`

Remover item "Aplicar split analogo em `decisoes.md` quando cruzar 800 linhas" da secao "Debitos meta-operacionais". Identificar por palavras-chave "split analogo" + "decisoes.md" (ou similares). Remover bullet inteiro de uma so vez.

### Conteudo das edicoes em `docs/progresso.md`

**Edicao 1 — Sub-etapa 4.16 no topo de "Sub-etapas concluidas" da Camada 3** (acima da 4.15):

```markdown
- **4.16 — Split do `decisoes.md` por tema (Camada 3 vai para arquivo dedicado)** (2026-05-11): segunda aplicacao da categoria "manutencao de docs por crescimento" (consolidada pela 4.13). Critério de corte **tematico** (nao cronologico como na 4.13): decisoes fundacionais (stack/arquitetura/convencoes) ficam em `decisoes.md`; decisoes operacionais de Camada 3 (hooks, subagents, skills, padroes operacionais) movem para novo `decisoes-claude-code.md`. Operacao facilitada por demarcacao H2 `## Camada 3 — Configuracao do Claude Code` ja existente no `decisoes.md` — split foi extracao da H2 inteira. Resultado: `decisoes.md` enxuto (~370-400 linhas); `decisoes-claude-code.md` criado (~460-490 linhas); hook 4.4 deixa de alertar em ambos. CLAUDE.md atualizado em "Onde buscar mais" (quarta atualizacao causadora — regra 4.6). Debito da 4.13 removido de `hooks-pendentes.md`. Padrao operacional consolidado: **criterio de split varia conforme natureza do documento** (`progresso.md` cronologico -> por idade; `decisoes.md` tematico -> por tema). PR #XX.
```

**Edicao 2 — Bloco "Licoes da Sub-etapa 4.16"** acima de "Licoes da Sub-etapa 4.15":

```markdown
## Licoes da Sub-etapa 4.16

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — manutencao de docs.)

### Licoes de ambiente

1. **Criterio de split varia conforme natureza do documento.** A 4.13 cortou `progresso.md` por idade porque o documento e cronologico (estado evolui no tempo, Camadas concluidas viram historico). A 4.16 cortou `decisoes.md` por tema porque o documento e tematico (decisoes categorizadas por dominio, todas vivas). Padrao operacional: **antes de splittar documento que cruza limite, identificar criterio natural de corte conforme o que o documento e**. Replicar criterio cego (sempre por idade, ou sempre por tema) pode gerar split artificial. Categoria operacional "manutencao de docs por crescimento" cobre ambos os criterios — instrumento e o mesmo, criterio de aplicacao varia.

2. **Demarcacao H2 ja existente facilitou split tematico imensamente.** `decisoes.md` ja tinha `## Camada 3 — Configuracao do Claude Code` delimitando exatamente a regiao a mover. Split foi extracao da H2 inteira (com todas as subsecoes ### filhas) — sem necessidade de identificar bordas conceituais ad-hoc. **Recomendacao operacional:** docs que crescem candidatos a split futuro devem ser estruturados com H2 marcando dominios distintos desde cedo. Em particular, `adrs.md` pode se beneficiar de H2 por dominio (`## Stack`, `## Operacional`, `## Claude Code`) caso cresca a ponto de exigir split.

3. **Auto-referencia em sub-etapa que altera estrutura.** A subsecao "Split do `decisoes.md` por tema (Sub-etapa 4.16)" entrou no `decisoes-claude-code.md` (arquivo splittado), nao no `decisoes.md` vivo. Padrao: **sub-etapa que altera estrutura de doc registra a alteracao dentro da nova estrutura**. Coerente com o conceito ("split de doc X" e decisao operacional de Camada 3 — pertence ao arquivo de decisoes operacionais). Aplicavel a futuros splits e movimentacoes estruturais.

4. **Cadeia "debito originario X -> resolvido em X+N" replicada por terceira vez no projeto.** Padrao consolidado por tripla aplicacao: 4.13 gerou debito do hook 4.4 -> 4.14 resolveu; 4.13 gerou debito do split de `decisoes.md` -> 4.16 resolveu; 4.10 gerou debitos meta-operacionais -> 4.15 (errata) reclassificou. Debitos registrados explicitamente em sub-etapa X **sao naturalmente resolvidos** em sub-etapa X+N quando dor concreta dispara, sem necessidade de planejamento centralizado. Disciplina de registro + atencao a triggers de dor (warn de hook, observacao empirica) basta.
```

**Edicao 3 — Linha no historico** acima da entrada da 4.15:

```markdown
- **2026-05-11** — Sub-etapa 4.16 concluida (manutencao de docs por crescimento, criterio tematico): split do `decisoes.md`. Secao `## Camada 3 — Configuracao do Claude Code` (incluindo todas as subsecoes 4.X) movida para novo `docs/decisoes-claude-code.md` (~460-490 linhas). `decisoes.md` enxuto (~370-400 linhas). CLAUDE.md atualizado em "Onde buscar mais" (4a vez causadora — regra 4.6). Debito da 4.13 removido de `hooks-pendentes.md`. Padrao operacional novo: **criterio de split varia conforme natureza do documento**. PR #XX.
```

### Versionar este proprio prompt

`docs/prompts/prompt-etapa-4-16.md` entra como novo arquivo no Commit 5.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra squash da 4.15.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompts/prompt-etapa-4-16.md` presente como **untracked** (anexado pelo operador).
- Working tree limpo (exceto o prompt).
- `docs/decisoes.md` existe (~829 linhas pos-4.15).
- `docs/decisoes-claude-code.md` NAO existe — sera criado na Tarefa 5.
- `docs/decisoes.md` tem H2 `## Camada 3 — Configuracao do Claude Code` (linha aproximada — confirmar via Tarefa 4).
- `docs/decisoes.md` tem H2 `## Principios herdados do blueprint` apos a H2 da Camada 3.
- `docs/hooks-pendentes.md` tem item "split analogo em `decisoes.md`" na secao "Debitos meta-operacionais".
- `CLAUDE.md` tem secao "## Onde buscar mais" com linha de `decisoes.md`.

Validar com:

```powershell
git status
git log --oneline -3
git config core.hooksPath
Test-Path docs\prompts\prompt-etapa-4-16.md
Test-Path docs\decisoes-claude-code.md
(Get-Content docs\decisoes.md).Count
```

**Pre-condicoes ADR-011:**

- `Test-Path docs\prompts\prompt-etapa-4-16.md` retorna `True`.
- `Test-Path docs\decisoes-claude-code.md` retorna `False`.
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
git checkout -b docs/etapa-4-16-split-decisoes-camada-3
```

Prefixo `docs/` — sub-etapa documental pura (sem mudanca em codigo).

### Tarefa 4 — Antes de editar, ler arquivos vivos

```bash
cat docs/decisoes.md
cat docs/progresso.md
cat docs/hooks-pendentes.md
cat CLAUDE.md
```

**Confirmar e anotar:**

- `decisoes.md`: localizar **linha exata onde comeca a H2 `## Camada 3 — Configuracao do Claude Code`**. Anotar numero da linha.
- `decisoes.md`: localizar **linha exata onde comeca a proxima H2 (`## Principios herdados do blueprint`)**. Anotar numero da linha. O intervalo entre essas duas linhas (sem incluir a segunda) e exatamente o que sera movido.
- `decisoes.md`: confirmar que dentro da H2 `## Camada 3 — Configuracao do Claude Code` aparecem as subsecoes esperadas (Layout de `.claude/`, Mecanismo de git hooks no Windows, Debito de portabilidade, todas as Sub-etapas 4.X, Claude Code hooks nativos).
- `decisoes.md`: confirmar que a subsecao "Auditoria meta-operacional + ADR-012 (Sub-etapa 4.10)" tem a **nota de errata** adicionada pela 4.15 (blockquote logo apos o titulo).
- `decisoes.md`: localizar `### Historico de mudancas` (sob `## Como atualizar este documento`) — linha onde entra a nova entrada da 4.16.
- `hooks-pendentes.md`: confirmar que existe item "Aplicar split analogo em `decisoes.md`" na secao "Debitos meta-operacionais". **Anotar texto exato** para remocao precisa.
- `CLAUDE.md`: confirmar secao "## Onde buscar mais" com lista de docs em `docs/`. Linha de `decisoes.md` sera atualizada e nova linha adicionada para `decisoes-claude-code.md`.

Se alguma divergencia, **parar e reportar**.

### Tarefa 5 — Criar `docs/decisoes-claude-code.md`

Criar arquivo com:

1. Cabecalho prescrito no escopo (titulo + paragrafo + data de criacao + separador `---`).
2. **Conteudo integral** da H2 `## Camada 3 — Configuracao do Claude Code` extraido do `decisoes.md` vivo. Inclui o **proprio cabecalho da H2** + todas as subsecoes ### filhas (Layout de `.claude/` ate Claude Code hooks nativos).
3. **Subsecao nova 4.16** inserida **antes** da subsecao `### Claude Code hooks nativos` (que e a ultima da H2 original).

**Preservacao byte-a-byte do conteudo extraido:** copiar conteudo integral, sem reformatar, sem reordenar, sem corrigir typos. Encoding UTF-8 sem BOM. Manter inclusive a nota de errata da 4.15 na subsecao da 4.10.

**Pre-condicao ADR-011 apos criar:**

```powershell
Test-Path docs\decisoes-claude-code.md   # True

$bytes = [System.IO.File]::ReadAllBytes("docs/decisoes-claude-code.md")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191 (sem BOM)

$content = [System.IO.File]::ReadAllText("docs/decisoes-claude-code.md", [System.Text.UTF8Encoding]::new($false))

# Cabecalho presente
if ($content -match '# Decisoes.{1,3}Camada 3') {
    Write-Host "Cabecalho OK"
} else {
    Write-Host "Cabecalho AUSENTE"
}

# H2 da Camada 3 presente
if ($content -match '## Camada 3.{1,5}Configuracao do Claude Code') {
    Write-Host "H2 Camada 3 OK"
} else {
    Write-Host "H2 Camada 3 AUSENTE"
}

# Subsecoes 4.X presentes (amostra)
$subsecoes = @('Sub-etapa 4.1', 'Sub-etapa 4.7', 'Sub-etapa 4.10', 'Sub-etapa 4.13', 'Sub-etapa 4.15')
foreach ($s in $subsecoes) {
    if ($content -match [regex]::Escape($s)) {
        Write-Host "Subsecao OK: $s"
    } else {
        Write-Host "Subsecao AUSENTE: $s"
    }
}

# Subsecao 4.16 presente
if ($content -match '### Split do.{1,5}decisoes\.md.{1,30}Sub-etapa 4\.16') {
    Write-Host "Subsecao 4.16 OK"
} else {
    Write-Host "Subsecao 4.16 AUSENTE"
}

# Nota de errata da 4.15 preservada
if ($content -match 'Errata \(Sub-etapa 4\.15\)') {
    Write-Host "Errata 4.15 preservada OK"
} else {
    Write-Host "Errata 4.15 PERDIDA"
}

# H2 fundacional NAO presente neste arquivo (validacao negativa)
$h2_fundacionais = @('## Stack', '## Arquitetura', '## Convencoes de codigo', '## Modelo financeiro')
foreach ($h in $h2_fundacionais) {
    if ($content -match [regex]::Escape($h)) {
        Write-Host "ERRO: $h vazou para decisoes-claude-code"
    } else {
        Write-Host "Fundacional corretamente ausente: $h"
    }
}

# Linhas totais (esperado: ~460-490)
$linhas = (Get-Content docs\decisoes-claude-code.md).Count
Write-Host "Linhas totais: $linhas (esperado: 460-490)"
```

Se algum valor divergir, parar e reportar.

### Tarefa 6 — Editar `docs/decisoes.md` (remover H2 movida + adicionar referencia + entrada no historico)

Aplicar:

1. **Remover** a H2 `## Camada 3 — Configuracao do Claude Code` inteira (do cabecalho ate antes do proximo H2 `## Principios herdados do blueprint`). Inclui todas as subsecoes ### filhas.
2. **Adicionar** a H2 nova "## Decisoes operacionais da Camada 3" com texto prescrito no escopo (referencia cruzada para `decisoes-claude-code.md`), na mesma posicao onde estava a H2 movida (entre `## Modelo financeiro do projeto` e `## Principios herdados do blueprint`).
3. **Adicionar** linha no topo de `### Historico de mudancas` com texto prescrito no escopo.

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/decisoes.md", [System.Text.UTF8Encoding]::new($false))

# H2 Camada 3 (com subsecoes) NAO mais presente
if ($content -match '## Camada 3.{1,5}Configuracao do Claude Code') {
    Write-Host "ERRO: H2 Camada 3 ainda presente"
} else {
    Write-Host "H2 Camada 3 removida OK"
}

# Subsecoes 4.X NAO mais presentes (amostra)
$subsecoes = @('Sub-etapa 4.1', 'Sub-etapa 4.7', 'Sub-etapa 4.13', 'Sub-etapa 4.16')
foreach ($s in $subsecoes) {
    if ($content -match [regex]::Escape($s)) {
        Write-Host "ERRO: $s ainda presente"
    } else {
        Write-Host "Subsecao removida OK: $s"
    }
}

# Nova H2 de referencia presente
if ($content -match '## Decisoes operacionais da Camada 3') {
    Write-Host "H2 referencia OK"
} else {
    Write-Host "H2 referencia AUSENTE"
}

# Link para decisoes-claude-code.md presente
if ($content -match 'decisoes-claude-code\.md') {
    Write-Host "Link OK"
} else {
    Write-Host "Link AUSENTE"
}

# H2 fundacionais preservadas
$h2_fundacionais = @('## Stack', '## Arquitetura', '## Convencoes de codigo', '## Modelo financeiro', '## Principios herdados do blueprint')
foreach ($h in $h2_fundacionais) {
    if ($content -match [regex]::Escape($h)) {
        Write-Host "Fundacional OK: $h"
    } else {
        Write-Host "ERRO: fundacional removida: $h"
    }
}

# Entrada nova no historico
if ($content -match 'Sub-etapa 4\.16.{1,30}split por tema') {
    Write-Host "Historico 4.16 OK"
} else {
    Write-Host "Historico 4.16 AUSENTE"
}

# Linhas totais (esperado: ~370-400, queda de ~430-460)
$linhas = (Get-Content docs\decisoes.md).Count
Write-Host "Linhas totais: $linhas (esperado: 370-400)"
```

**Atencao:** hook 4.4 NAO deve alertar mais para `decisoes.md` nesta sub-etapa (saiu de ~829 para ~370-400, abaixo de 800). Se ainda alertar, investigar — remocao incompleta.

Se algum valor divergir, parar e reportar.

### Tarefa 7 — Atualizar `CLAUDE.md` (acrescimo em "Onde buscar mais")

Aplicar mudanca prescrita no escopo: adicionar linha sobre `decisoes-claude-code.md` apos a linha de `decisoes.md`; ajustar texto da linha de `decisoes.md` para mencionar "decisoes fundacionais".

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("CLAUDE.md", [System.Text.UTF8Encoding]::new($false))

# Linha de decisoes-claude-code presente
if ($content -match '`decisoes-claude-code\.md`.{1,200}operacionais') {
    Write-Host "Linha decisoes-claude-code OK"
} else {
    Write-Host "Linha decisoes-claude-code AUSENTE"
}

# Linha de decisoes.md ajustada
if ($content -match '`decisoes\.md`.{1,200}fundacionais') {
    Write-Host "Linha decisoes.md ajustada OK"
} else {
    Write-Host "Linha decisoes.md NAO ajustada"
}
```

### Tarefa 8 — Atualizar `docs/hooks-pendentes.md` (remover debito 4.13)

Identificar item por palavras-chave "split analogo" + "decisoes.md". Remover bullet inteiro de uma so vez.

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/hooks-pendentes.md", [System.Text.UTF8Encoding]::new($false))

if ($content -match 'split analogo.{1,30}decisoes\.md') {
    Write-Host "ATENCAO: debito ainda presente -- verificar remocao"
} else {
    Write-Host "Debito removido OK"
}
```

### Tarefa 9 — Atualizar `docs/progresso.md` (3 edicoes)

Aplicar **edicoes 1-3** descritas no escopo:

1. Sub-etapa 4.16 ao topo de "Sub-etapas concluidas" (acima da 4.15).
2. "Licoes da Sub-etapa 4.16" acima de "Licoes da Sub-etapa 4.15".
3. Linha de historico acima da entrada da 4.15.

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/progresso.md", [System.Text.UTF8Encoding]::new($false))

# Sub-etapa 4.16 presente
if ($content -match '4\.16.{1,10}Split do.{1,5}decisoes') {
    Write-Host "Sub-etapa 4.16 OK"
} else {
    Write-Host "Sub-etapa 4.16 AUSENTE"
}

# Licoes da 4.16
if ($content -match '## Li.{1,3}es da Sub-etapa 4\.16') {
    Write-Host "Licoes 4.16 OK"
} else {
    Write-Host "Licoes 4.16 AUSENTE"
}

# Ordem cronologica
$pos416 = $content.IndexOf('**4.16')
$pos415 = $content.IndexOf('**4.15')
if ($pos416 -gt 0 -and $pos416 -lt $pos415) {
    Write-Host "Ordem cronologica OK"
} else {
    Write-Host "Ordem cronologica TROCADA"
}

# Linhas totais (esperado: ~480-500, crescimento leve)
$linhas = (Get-Content docs\progresso.md).Count
Write-Host "Linhas totais: $linhas"
```

### Tarefa 10 — Commits (5 commits)

**Commit 1** — Criar `decisoes-claude-code.md`:

```bash
git add docs/decisoes-claude-code.md
git status   # apenas decisoes-claude-code.md staged
git commit -m "docs: cria decisoes-claude-code.md com secao Camada 3 (sub-etapa 4.16)"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Atencao:** hook 4.4 NAO deve alertar (arquivo novo ~460-490 linhas, abaixo de 800).

**Commit 2** — Editar `decisoes.md`:

```bash
git add docs/decisoes.md
git status   # apenas decisoes.md staged
git commit -m "docs: remove secao Camada 3 do decisoes.md (movida para decisoes-claude-code.md)"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Atencao:** hook 4.4 NAO deve alertar mais (decisoes.md em ~370-400 linhas, abaixo de 800).

**Commit 3** — CLAUDE.md + hooks-pendentes.md:

```bash
git add CLAUDE.md docs/hooks-pendentes.md
git status   # 2 arquivos staged
git commit -m "docs: registra decisoes-claude-code.md em CLAUDE.md + remove debito da 4.13"
```

**Pre-condicao ADR-011:** 2 arquivos staged; `$LASTEXITCODE = 0`.

**Commit 4** — `progresso.md`:

```bash
git add docs/progresso.md
git status   # apenas progresso.md staged
git commit -m "docs: sub-etapa 4.16 -- registra split tematico de decisoes.md"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 5** — Versionar prompt:

```bash
git add docs/prompts/prompt-etapa-4-16.md
git status   # apenas prompt-etapa-4-16.md staged
git commit -m "docs: versiona prompt-etapa-4-16.md em docs/prompts/"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Atencao:** hook 4.4 NAO deve alertar (prompt em `docs/prompts/`, isento desde 4.14).

**Validacao implicita dos hooks ativos:**

- Conventional Commits (4.1): mensagens validas.
- Encoding UTF-8 (4.2): valida bytes em todos os arquivos editados/criados.
- Markdown blank lines (4.3): valida headers em `.md` editados.
- Tamanho de docs (4.4): NAO deve alertar em nenhum commit (decisoes.md saiu de 829 para ~370-400; decisoes-claude-code.md nasce com ~460-490).

Se algum hook bloquear, parar e reportar.

### Tarefa 11 — Validacao final antes de push

```bash
git status
git log --oneline -10
git config core.hooksPath
```

```powershell
Test-Path docs\decisoes-claude-code.md             # True
Test-Path docs\decisoes.md                          # True
(Get-Content docs\decisoes.md).Count                # ~370-400
(Get-Content docs\decisoes-claude-code.md).Count    # ~460-490
```

Esperado:

- Working tree limpo.
- 5 commits novos.
- `core.hooksPath` = `.githooks`.

## Restricoes e freios

1. **NAO modificar conteudo extraido ao mover.** Copia byte-a-byte do `decisoes.md` para `decisoes-claude-code.md`. Sem reformatacao, sem reordenacao, sem correcao de typos. Preservacao fiel. Inclui a nota de errata da 4.15 na subsecao da 4.10 — deve ir junto.

2. **NAO mover H2 fundacionais.** As H2 `## Stack`, `## Arquitetura`, `## Convencoes de codigo`, `## Convencoes operacionais`, `## Politica de debito tecnico consciente`, `## Comandos atomicos do projeto`, `## Frontend`, `## Modelo financeiro do projeto`, `## Principios herdados do blueprint`, `## Como atualizar este documento` permanecem em `decisoes.md`.

3. **NAO mover `### Historico de mudancas`.** Esta sob `## Como atualizar este documento`, que e fundacional. Historico cobre cronologia de mudancas em `decisoes.md`. Permanece. `decisoes-claude-code.md` nao tem historico proprio (nao vale duplicar).

4. **NAO mexer em `.claude/agents/`, `.claude/skills/`, `.claude/hooks/`, `.githooks/`.**

5. **NAO modificar `docs/adrs.md`, `docs/visao.md`, `docs/blueprint-fabrica-ai-native.md`, `docs/progresso-historico.md`.**

6. **NAO criar subagents, skills, hooks, MCPs.**

7. **NAO mexer em `~/.claude/` global.**

8. **Encoding UTF-8 sem BOM** em todos os arquivos criados/editados.

9. **Apenas ASCII em mensagens de commit.** Sem acentos, sem em-dash U+2014.

10. **Ordem cronologica descrescente** em "Sub-etapas concluidas", "Licoes", "Historico" em `progresso.md` e em `### Historico de mudancas` no `decisoes.md`.

11. **Hook 4.4 NAO deve alertar em nenhum commit.** Se alertar, investigar — remocao em `decisoes.md` pode estar incompleta ou `decisoes-claude-code.md` pode ter recebido conteudo a mais que o previsto.

12. **Auto-referencia: subsecao 4.16 entra em `decisoes-claude-code.md`, NAO em `decisoes.md`.** Padrao "sub-etapa que altera estrutura registra dentro da nova estrutura".

13. **Sem cenarios destrutivos tradicionais.** Sub-etapa de manutencao de docs — validacao via pre-condicoes ADR-011 em cada Tarefa.

14. **Pre-condicao da Tarefa 5** confere preservacao de subsecoes esperadas + ausencia de H2 fundacionais (validacao positiva e negativa).

15. **Pre-condicao da Tarefa 6** confere remocao das subsecoes movidas + preservacao das H2 fundacionais.

16. **Nao sugerir proxima sub-etapa** espontaneamente alem dos candidatos ja citados (`test-writer`, `/feature`).

17. **Nao tomar decisao silenciosa em zona limitrofe.** Reportar divergencias.

18. **Nao usar `pwsh`.** PowerShell 5.1.

19. **Nao usar `git reset --hard`.**

20. **Nao usar `git commit --no-verify`.**

## Estrutura de commits

Branch: `docs/etapa-4-16-split-decisoes-camada-3`

**Commit 1** — `docs: cria decisoes-claude-code.md com secao Camada 3 (sub-etapa 4.16)`

- `docs/decisoes-claude-code.md` (novo, com H2 da Camada 3 extraida + subsecao 4.16)

**Commit 2** — `docs: remove secao Camada 3 do decisoes.md (movida para decisoes-claude-code.md)`

- `docs/decisoes.md` (H2 da Camada 3 removida + H2 de referencia adicionada + entrada no historico de mudancas)

**Commit 3** — `docs: registra decisoes-claude-code.md em CLAUDE.md + remove debito da 4.13`

- `CLAUDE.md` (acrescimo em "Onde buscar mais")
- `docs/hooks-pendentes.md` (debito da 4.13 removido)

**Commit 4** — `docs: sub-etapa 4.16 -- registra split tematico de decisoes.md`

- `docs/progresso.md` (sub-etapa + licoes + historico)

**Commit 5** — `docs: versiona prompt-etapa-4-16.md em docs/prompts/`

- `docs/prompts/prompt-etapa-4-16.md` (novo)

## Validacao antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -10
git config core.hooksPath
```

```powershell
Test-Path docs\decisoes-claude-code.md
Test-Path docs\decisoes.md
(Get-Content docs\decisoes.md).Count
(Get-Content docs\decisoes-claude-code.md).Count
```

Esperado:

- `check.ps1` passa.
- Working tree limpo.
- 5 commits novos.
- `decisoes.md` ~370-400 linhas; `decisoes-claude-code.md` ~460-490 linhas.

## PR

Titulo: `docs: sub-etapa 4.16 -- split de decisoes.md por tema (Camada 3 vai para decisoes-claude-code.md)`

Body sugerido:

````markdown
## Summary

Segunda aplicacao da categoria "manutencao de docs por crescimento" (consolidada pela 4.13). **Criterio de corte tematico** (diferente da 4.13 que cortou `progresso.md` por idade). Resolve debito explicito registrado em `hooks-pendentes.md` desde a 4.13 ("Aplicar split analogo em `decisoes.md` quando cruzar 800 linhas").

### Por que esta sub-etapa existe

`decisoes.md` cruzou 800 linhas durante a 4.15 (subsecao da errata + nota de errata levaram o arquivo de ~780 para 829 linhas). Hook 4.4 (modo warn) alertou no Commit 1 da 4.15 — trigger do debito registrado desde a 4.13.

### Por que tematico e nao cronologico

`decisoes.md` tem dois tipos qualitativamente distintos de conteudo, **ambos vivos**:

- **Decisoes fundacionais**: Stack, Arquitetura, Convencoes de codigo, Convencoes operacionais, Politica de debito, Comandos atomicos, Frontend, Modelo financeiro, Principios do blueprint. Consultadas em qualquer sub-etapa para reabrir contexto.
- **Decisoes operacionais de Camada 3**: Layout de `.claude/`, mecanismo de git hooks, hooks 4.1-4.7, subagents 4.9-4.12, skills 4.11-4.12, padroes 4.13-4.15.

Nenhum tipo e historico arquivado — todos sao regras ativas. Decisao da 4.1 (Conventional Commits) continua valendo em todo commit; padrao de validacao destrutiva (4.2.1) e regra dura aplicada em toda sub-etapa.

Padrao temporal da 4.13 (Camadas concluidas viram historico) nao se aplica. Padrao tematico (decisoes por dominio) e o natural aqui.

### Padrao operacional novo

**Criterio de split varia conforme natureza do documento:**

- `progresso.md` (cronologico) -> split por idade.
- `decisoes.md` (tematico) -> split por tema.
- `adrs.md` (se crescer) -> provavelmente tematico por dominio.

Categoria operacional "manutencao de docs por crescimento" cobre ambos os criterios — instrumento e o mesmo, criterio de aplicacao varia.

### Operacao facilitada por demarcacao H2 ja existente

`decisoes.md` ja tinha cabecalho `## Camada 3 — Configuracao do Claude Code` delimitando exatamente a regiao a mover. Split foi extracao da H2 inteira (com todas as subsecoes ### filhas) — sem necessidade de identificar bordas conceituais ad-hoc.

**Recomendacao operacional registrada nas licoes:** docs candidatos a split futuro devem ser estruturados com H2 marcando dominios distintos desde cedo.

### Mudancas estruturais

**`docs/decisoes-claude-code.md`** (~460-490 linhas, novo):

- Cabecalho + paragrafo explicando origem.
- H2 `## Camada 3 — Configuracao do Claude Code` extraida do `decisoes.md` integralmente.
- Todas as subsecoes ### filhas (Layout de `.claude/`, Mecanismo de git hooks, Debito de portabilidade, todas as Sub-etapas 4.X, Claude Code hooks nativos) preservadas byte-a-byte.
- Nota de errata da 4.15 na subsecao da 4.10 preservada.
- **Subsecao nova 4.16** inserida antes de "Claude Code hooks nativos" (auto-referencia: a sub-etapa que descreve o split registra a alteracao dentro da nova estrutura).

**`docs/decisoes.md`** (~370-400 linhas, vivo):

- H2 fundacionais preservadas: Stack, Arquitetura, Convencoes de codigo, Convencoes operacionais, Politica de debito tecnico, Comandos atomicos, Frontend, Modelo financeiro, Principios do blueprint, Como atualizar este documento.
- H2 `## Decisoes operacionais da Camada 3` nova com referencia cruzada para `decisoes-claude-code.md`.
- Entrada no `### Historico de mudancas` registrando a 4.16.

**`CLAUDE.md`**:

- Secao "Onde buscar mais" ganha referencia ao `decisoes-claude-code.md`.
- Linha de `decisoes.md` ajustada para mencionar "decisoes fundacionais".

**`docs/hooks-pendentes.md`**:

- Item "Aplicar split analogo em `decisoes.md` quando cruzar 800 linhas" removido da secao "Debitos meta-operacionais".

**`docs/progresso.md`**:

- Sub-etapa 4.16 ao topo + 4 licoes + entrada no historico.

### Hook 4.4 deixa de alertar

`decisoes.md` enxuto (~370-400 linhas), abaixo do limite 800. `decisoes-claude-code.md` nasce em ~460-490 linhas, tambem abaixo. Ambos saudaveis pos-split.

### Convencao registrada em CLAUDE.md (regra 4.6)

Quarta atualizacao causadora de CLAUDE.md no projeto (apos 4.6, 4.11, 4.13). Convencao nova: **decisoes operacionais de Camada 3 ficam em arquivo dedicado**, separadas das decisoes fundacionais. Operadores e agentes em sessoes futuras consultam o arquivo certo conforme tipo de decisao.

### Cadeia "debito originario X -> resolvido em X+N" replicada por terceira vez

- 4.13 gerou debito do hook 4.4 -> **4.14 resolveu**.
- 4.13 gerou debito do split de `decisoes.md` -> **4.16 resolveu**.
- 4.10 gerou debitos meta-operacionais -> 4.15 (errata) reclassificou.

Padrao consolidado por tripla aplicacao.

### Smoke test pos-merge sugerido

1. Sessao nova do Claude Code.
2. Verificar que agente lendo `decisoes.md` em sessao nova reconhece referencia cruzada e sabe onde buscar decisoes operacionais.
3. Conferir que `/review-pr` e `/review-arch` continuam funcionando (componentes nao foram tocados).
4. Conferir que `git log --follow docs/decisoes.md` mostra historico completo (operacao foi remocao + adicao no mesmo arquivo, nao move — historico preservado naturalmente).

### Proximo passo

Decisao fora deste PR. Candidatos naturais:

- **4.17** — `test-writer` + skill `/write-test` (territorio novo: subagent que **gera codigo**, nao revisa).
- **4.17 alternativo** — Skill sem subagent `/feature <nome>` (eixo novo: skill geradora pura).
````

## Pos-criacao do PR

1. Abrir PR via `gh pr create`.
2. Capturar o numero.
3. Editar `docs/decisoes-claude-code.md` (linha 4.16) e `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorizacao explicita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `docs/etapa-4-16-split-decisoes-camada-3` empurrada com 6 commits (5 + 1 update do PR).
- PR aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.15.
- Working tree limpo.
- `docs/decisoes-claude-code.md` existe (~460-490 linhas).
- `docs/decisoes.md` existe (~370-400 linhas).
- Hook 4.4 sem alertas em qualquer arquivo do projeto.
- Reportar: `git log --oneline -10`, `git status`, `gh pr view --json number,state,statusCheckRollup`, contagem de linhas de `decisoes.md` e `decisoes-claude-code.md`.

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao splittar outros docs (`adrs.md`, `progresso.md` de novo) nesta sub-etapa.
- Nao modificar componentes do projeto (`.claude/`, hooks, codigo, frontend).
- Nao atualizar `docs/adrs.md`, `docs/visao.md`, `docs/blueprint-fabrica-ai-native.md`, `docs/progresso-historico.md`.
- Nao mexer em `~/.claude/` global.
- Nao criar prompt da 4.17.
- Nao sugerir proximo passo espontaneamente alem dos candidatos ja citados no PR body.
- Nao usar `pwsh`.
- Nao usar `git reset --hard`.
- Nao usar `git commit --no-verify`.
