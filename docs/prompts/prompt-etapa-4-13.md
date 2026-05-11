# Prompt — Etapa 4.13: Split do `progresso.md` por crescimento + reorganizacao de `docs/prompts/`

## Contexto

Camada 3 com 6 hooks + CLAUDE.md + blueprint + 2 subagents (`pr-reviewer` Haiku + `architect-reviewer` Sonnet) + 2 skills orquestradoras (`/review-pr` + `/review-arch`) + ADR-012 (revisao 4.11) apos a 4.12. Padrao skill+subagent validado empiricamente em 2 casos (PRs #55, #45, #35).

`docs/progresso.md` cresceu para ~891 linhas — hook 4.4 (tamanho de docs, modo warn) alertando consistentemente em cada sub-etapa que toca o arquivo. Padrao "infraestrutura segue necessidade" diz para tratar quando dor real aparecer; dor real apareceu.

`docs/` tem hoje 6 docs principais (`progresso.md`, `decisoes.md`, `adrs.md`, `hooks-pendentes.md`, `visao.md`, `blueprint-fabrica-ai-native.md`) + uma quantidade variavel de prompts versionados (`prompt-etapa-*.md`) que ja dominam visualmente o diretorio. Reorganizacao para `docs/prompts/` libera `docs/` para hierarquia clara: 6 docs principais + 1 subpasta de prompts.

Esta sub-etapa entrega:

1. **Split do `progresso.md` por Camada concluida.** Camadas 0, 1, 2 (todas ✅ concluidas) e suas Licoes movem para novo arquivo `docs/progresso-historico.md`. Camada 3 (em andamento) + Camadas 4-6 (planejamento) + Metricas + Licoes 4.X + Historico de mudancas permanecem no vivo. Estimativa: `progresso.md` de ~891 para ~400 linhas; `progresso-historico.md` nasce com ~500 linhas.

2. **Reorganizacao `docs/prompt-etapa-*.md` para `docs/prompts/`.** Move via `git mv` (preserva historico de cada arquivo). Padrao consolidado para prompts futuros (`docs/prompts/prompt-etapa-X-Y.md`).

3. **CLAUDE.md atualizado** — 2 acrescimos em "Onde buscar mais" (referenciar `progresso-historico.md` + ajustar referencia de prompts para `docs/prompts/`). Regra 4.6 disparada: 4.13 e a sub-etapa causadora da convencao "split de docs por crescimento" entrar em uso.

4. **Debito explicito em `hooks-pendentes.md`** — "Aplicar split analogo em `decisoes.md` quando cruzar 800 linhas" (decisoes.md em ~600 linhas hoje, padrao registrado para aplicacao futura por dor).

5. **Smoke 4.12 marcado como concluido** em criterios da Camada 3 (validado em PR #35 conforme relato do operador).

6. **Categoria nova: "manutencao de docs por crescimento"** — registra padrao operacional para splits futuros (decisoes.md quando crescer; progresso.md de novo quando crescer demais; outros docs por dor real).

Quando esta etapa terminar:

- `docs/progresso-historico.md`: novo arquivo arquivando Camadas 0/1/2 + Licoes de etapas 1.X, 2.X, 3.X.
- `docs/progresso.md`: enxuto (~400 linhas) com Camada 3 viva + Camadas 4-6 planejadas + Licoes 4.X + historico.
- `docs/prompts/`: nova subpasta com todos os `prompt-etapa-*.md` movidos via `git mv`.
- `CLAUDE.md`: secao "Onde buscar mais" atualizada.
- `docs/decisoes.md`: subsecao 4.13 antes de "Claude Code hooks nativos".
- `docs/progresso.md` (vivo): sub-etapa 4.13 + smoke 4.12 ✅ + licoes + historico.
- `docs/hooks-pendentes.md`: debito de split `decisoes.md` registrado.
- `docs/prompts/prompt-etapa-4-13.md`: versionado (no novo destino).

## Padroes que estreiam nesta etapa

1. **Categoria nova: "manutencao de docs por crescimento".** Distinta de:
   - "Refinamento pos-smoke empirico" (4.9.1): corrige comportamento de componente.
   - "Auditoria meta-operacional" (4.10): registra descobertas que afetam estrategia de camada.
   - "Errata de ADR baseada em descoberta de documentacao oficial" (4.11): corrige mecanismo literal de ADR.
   - "Replicacao de padrao consolidado" (4.12): segunda aplicacao de padrao validado.
   - **Esta categoria:** quando documento operacional cresce a ponto de degradar legibilidade (limite warn do hook 4.4 cruzado consistentemente), sub-etapa de manutencao quebra o doc preservando informacao. Padrao replicavel para `decisoes.md`, `adrs.md`, outros docs futuros.

2. **Split por Camada concluida vs sub-etapa atomica.** Criterio escolhido: cortar por **Camada inteira concluida** (0, 1, 2 vao). Alternativas consideradas e rejeitadas:
   - Cortar por sub-etapa (mover so blocos `## Licoes da Sub-etapa X.Y`): preserva mais contexto no vivo mas split fica artificial.
   - Cortar por tempo (ex: ate 30 dias atras): arbitrario, nao alinhado com cronologia conceitual do projeto.
   - **Por Camada concluida e:** cronologicamente claro, conceitualmente coerente (Camada concluida = historico estavel), facil de comunicar.

3. **Reorganizacao `docs/prompts/` no mesmo PR que o split.** Cleanup atomico — `docs/` ganha hierarquia clara: 6 docs principais + 1 subpasta `prompts/`. Custo extra na sub-etapa: `git mv` em batch dos prompts existentes. Preserva historico via `git mv` (Git detecta rename, mantem blame).

## Escopo decidido (calibrado com operador antes da redacao via D1-D5)

### Arquivos modificados

| Arquivo | Tipo de mudanca |
|---|---|
| `docs/progresso-historico.md` | Novo (recebe Camadas 0/1/2 + Licoes etapas 1.X/2.X/3.X) |
| `docs/progresso.md` | Editado (removidas seçoes movidas, atualizada tabela Status geral, adicionada 4.13, smoke 4.12 ✅, licoes 4.13, historico) |
| `docs/prompts/` | Nova subpasta |
| `docs/prompts/prompt-etapa-*.md` | Movidos via `git mv` (preserva historico) |
| `CLAUDE.md` | 2 linhas em "Onde buscar mais" |
| `docs/decisoes.md` | Subsecao 4.13 antes de "Claude Code hooks nativos" |
| `docs/hooks-pendentes.md` | Debito de split `decisoes.md` registrado |
| `docs/prompts/prompt-etapa-4-13.md` | Versionado (no novo destino) |

**Nao tocados:** `.claude/agents/`, `.claude/skills/`, `.claude/hooks/`, `.githooks/`, `pom.xml`, `src/`, `frontend/`, `docs/adrs.md`, `docs/visao.md`, `docs/blueprint-fabrica-ai-native.md`, `.gitignore`, `.gitattributes`.

### Estrutura do `docs/progresso-historico.md` (novo arquivo)

Cabecalho:

```markdown
# Progresso — Historico Arquivado

> Historico arquivado de Camadas concluidas. Documento de leitura, nao vivo.
> Origem: separado de `docs/progresso.md` na Sub-etapa 4.13 quando o vivo cruzou ~890 linhas e o hook 4.4 passou a alertar consistentemente.
> Camadas cobertas: 0 (Discovery), 1 (Infraestrutura), 2 (Arquitetura).
> Para estado atual do projeto e Camada em andamento, ver `progresso.md`.

**Data de criacao:** 2026-05-11 (Sub-etapa 4.13)

## Mapeamento etapa -> Camada

A numeracao das etapas neste projeto nao corresponde 1:1 a numeracao das Camadas. Para evitar confusao:

| Etapas | Camada |
|---|---|
| 1.1, 1.2, 1.3, 1.4, 1.5 | Camada 1 (Infraestrutura) |
| 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.6.1, 2.6.2, 2.7, 2.8, 2.9 | Camada 1 (extensao e wrap-up) |
| 3.1, 3.2, 3.3, 3.3.1, 3.4, 3.5, 3.6, 3.7, 3.8 | Camada 2 (Arquitetura) |
| 4.0+ | Camada 3 (Claude Code) — ver `progresso.md` |

---
```

Em seguida, mover **conteudo integral** dos seguintes blocos do `progresso.md` atual para este arquivo, **na ordem**:

1. `## Camada 0 — Discovery` (do cabecalho ate antes de `## Camada 1`).
2. `## Camada 1 — Infraestrutura de confianca` (do cabecalho ate antes de `## Camada 2`).
3. `## Camada 2 — Arquitetura otimizada para agentes` (do cabecalho ate antes de `## Camada 3`).
4. `## Licoes da Etapa 1.1` (este bloco esta isolado no meio do arquivo, antes das Licoes 4.X — mover).
5. **Todos os blocos `## Licoes da Etapa X.Y` para X em {1, 2, 3}** que aparecem depois das Licoes 4.X (sequencia continua a partir de `## Licoes da Etapa 3.8` ate o ultimo antes de `## Historico de mudancas deste documento`).

### Mudancas em `docs/progresso.md` (vivo)

**Remocoes:**

- Bloco `## Camada 0 — Discovery` ate antes de `## Camada 1`.
- Bloco `## Camada 1 — Infraestrutura de confianca` ate antes de `## Camada 2`.
- Bloco `## Camada 2 — Arquitetura otimizada para agentes` ate antes de `## Camada 3`.
- Bloco `## Licoes da Etapa 1.1`.
- Todos os blocos `## Licoes da Etapa X.Y` para X em {1, 2, 3} apos as Licoes 4.X.

**Mantidos:**

- Cabecalho + paragrafo de abertura.
- `## Status geral` (tabela — sera atualizada conforme abaixo).
- `## Camada 3 — Configuracao do Claude Code` (em andamento — atualizada conforme abaixo).
- `## Camada 4 — Modelo operacional` (planejamento).
- `## Camada 5 — Runtime de agentes (VPS)` (planejamento).
- `## Camada 6 — Gestao hibrida Max + API` (planejamento).
- `## Metricas a capturar (a partir da Camada 4)`.
- Todos os blocos `## Licoes da Sub-etapa 4.X` (Camada 3 inteira).
- `## Historico de mudancas deste documento` (inteiro — historico cronologico preservado).

**Atualizacao da tabela `## Status geral`:**

Adicionar coluna ou nota apos "Concluida" para Camadas 0/1/2, linkando para o historico. Exemplo:

```markdown
| Camada | Descricao | Status |
|---|---|---|
| **0** | Discovery (visao, ADRs, decisoes, ambiente) | ✅ Concluida ([historico](progresso-historico.md#camada-0--discovery)) |
| **1** | Infraestrutura de confianca | ✅ Concluida ([historico](progresso-historico.md#camada-1--infraestrutura-de-confianca)) |
| **2** | Arquitetura otimizada para agentes | ✅ Concluida ([historico](progresso-historico.md#camada-2--arquitetura-otimizada-para-agentes)) |
| **3** | Configuracao do Claude Code (subagents, skills, hooks) | 🟢 Em andamento |
| **4** | Modelo operacional (tiers de autonomia ativados) | ⏸️ Aguardando |
| **5** | Runtime de agentes (VPS) — opcional | ⏸️ Aguardando |
| **6** | Gestao hibrida Max + API | 🟡 Parcial (configuracao API pronta, sem uso) |
```

**Adicionar nota apos a tabela:**

```markdown
> Historico detalhado das Camadas 0, 1, 2 (Critérios, decisoes, Licoes) em `progresso-historico.md`.
```

**Adicoes em "Camada 3":**

- **Sub-etapa 4.13** ao topo de "Sub-etapas concluidas" (acima da 4.12).
- **Criterios de "pronto"** ajustados — marcar smoke 4.12 como concluido (validado em PR #35 conforme relato do operador).

**Bloco "Licoes da Sub-etapa 4.13"** acima de "Licoes da Sub-etapa 4.12".

**Linha no historico** acima da entrada da 4.12.

### Conteudo das edicoes em `docs/progresso.md`

**Edicao 1 — Sub-etapa 4.13 no topo de "Sub-etapas concluidas":**

```markdown
- **4.13 — Split do `progresso.md` por crescimento + reorganizacao de `docs/prompts/`** (2026-05-11): sub-etapa de manutencao de docs. Categoria nova: **"manutencao de docs por crescimento"**. `progresso.md` cresceu para ~891 linhas — hook 4.4 (modo warn) alertando consistentemente. Camadas 0, 1, 2 (concluidas) + Licoes de etapas 1.X/2.X/3.X movidas para novo arquivo `progresso-historico.md` (~500 linhas). `progresso.md` vivo enxuto (~400 linhas) — Camada 3 em andamento + Camadas 4-6 planejadas + Licoes 4.X + historico de mudancas. `docs/prompt-etapa-*.md` movidos para `docs/prompts/` via `git mv` (preserva historico via rename detection). CLAUDE.md atualizado em "Onde buscar mais" com referencias atualizadas (regra 4.6 — esta sub-etapa e a causadora da convencao "split de docs por crescimento"). Debito explicito registrado em `hooks-pendentes.md`: aplicar split analogo em `decisoes.md` quando cruzar 800 linhas. Smoke 4.12 marcado como concluido (validado pelo operador em PR #35). PR #XX.
```

**Edicao 2 — Criterios da Camada 3 ajustados** (substituir bloco atual; principal mudanca e marcar smoke 4.12 como `[x]`):

```markdown
### Criterios de "pronto" (preliminar, ajustados pela 4.10, 4.11, 4.12 e 4.13)

- [x] `CLAUDE.md` do projeto escrito (target <=15KB) — concluido 4.6, atualizado 4.11 e 4.13
- [x] Padrao skill orquestradora -> subagent decidido — ADR-012 (4.10, revisado 4.11)
- [x] Subagent `pr-reviewer` (revisao critica antes do PR) — concluido 4.9 + refinado 4.9.1
- [x] Skill `/review-pr` orquestrando `pr-reviewer` (par com ADR-012) — concluido 4.11
- [x] Smoke pos-merge da 4.11 validando padrao skill+subagent ponta-a-ponta — validado em PR #55 + PR #45
- [x] Subagent `architect-reviewer` (Sonnet, valida decisoes contra ADR-004/005/006/007) — concluido 4.12
- [x] Skill `/review-arch` orquestrando `architect-reviewer` (par com ADR-012) — concluido 4.12
- [x] Smoke pos-merge da 4.12 validando segundo par skill+subagent — validado em PR #35
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

**Edicao 3 — Bloco "Licoes da Sub-etapa 4.13"** acima de "Licoes da Sub-etapa 4.12":

```markdown
## Licoes da Sub-etapa 4.13

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — manutencao de docs.)

### Licoes de ambiente

1. **Categoria nova: "manutencao de docs por crescimento".** Distinta de "auditoria meta-operacional" (4.10 — descobertas que afetam estrategia), "errata de ADR" (4.11 — corrige mecanismo literal), "replicacao de padrao consolidado" (4.12 — segunda aplicacao). Manutencao de docs trata **dor de tamanho** — documento cresce a ponto de degradar legibilidade (hook 4.4 alerta consistentemente). Sub-etapa quebra preservando informacao. Padrao replicavel: `decisoes.md`, `adrs.md`, outros docs futuros quando dor real aparecer.

2. **Split por Camada concluida e criterio simples e claro.** Alternativas (cortar por sub-etapa atomica, por tempo) sao mais granulares mas menos coerentes. Padrao operacional: **historico = Camadas concluidas; vivo = Camada em andamento + planejamento + ultimas licoes**. Critericamente claro, facil de comunicar.

3. **`git mv` em batch preserva historico de cada arquivo.** `git mv docs/prompt-etapa-*.md docs/prompts/` (ou equivalente PowerShell) renomeia todos preservando rename detection do git. `blame` continua funcionando, historico nao se perde. Aplicavel em outras reorganizacoes futuras.

4. **CLAUDE.md atualizado pela 3a vez (4.6, 4.11, 4.13) — cada vez por convencao causadora distinta.** Padrao operacional consolidado: CLAUDE.md so muda quando a propria sub-etapa estabelece nova convencao. Aqui: convencao "split de docs por crescimento" + convencao "prompts em `docs/prompts/`".
```

**Edicao 4 — Linha no historico** acima da entrada da 4.12:

```markdown
- **2026-05-11** — Sub-etapa 4.13 concluida: split do `progresso.md` por crescimento (~891 -> ~400 linhas). Camadas 0/1/2 + Licoes 1.X/2.X/3.X movidas para `progresso-historico.md`. Reorganizacao: `docs/prompt-etapa-*.md` para `docs/prompts/` via `git mv`. CLAUDE.md atualizado em "Onde buscar mais" (regra 4.6). Debito: split analogo de `decisoes.md` quando cruzar 800 linhas. Categoria nova: "manutencao de docs por crescimento". Smoke 4.12 marcado como concluido. PR #XX.
```

### Conteudo da subsecao em `docs/decisoes.md`

Inserir **antes** da subsecao `### Claude Code hooks nativos`:

```markdown
### Manutencao de docs por crescimento (Sub-etapa 4.13)

Sub-etapa de manutencao que inaugura categoria "manutencao de docs por crescimento". Tratamento de dor real — `progresso.md` cresceu para ~891 linhas, hook 4.4 (modo warn) alertando em cada sub-etapa que toca o arquivo.

**Split do `progresso.md`:** criterio "cortar por Camada concluida". Camadas 0 (Discovery), 1 (Infraestrutura), 2 (Arquitetura) — todas ✅ — movem para novo `docs/progresso-historico.md`. Camada 3 em andamento + Camadas 4-6 planejadas + Licoes 4.X + historico de mudancas permanecem no `progresso.md` vivo.

**Por que cortar por Camada e nao por sub-etapa atomica:** mais coerente conceitualmente (Camada concluida = historico estavel), criterio simples de comunicar e replicar, evita splits artificiais. Alternativas avaliadas: cortar por tempo (ex: 30 dias) e cortar por bloco de licao individual — ambas rejeitadas por arbitrariedade ou fragmentacao.

**Tabela "Status geral" do `progresso.md` vivo** ganha link para cada Camada concluida no historico arquivado. Operadores e agentes em sessoes futuras chegam ao detalhe historico via 1 clique.

**Reorganizacao `docs/prompts/`:** `docs/` tinha 6 docs principais + ~13+ prompts versionados (`prompt-etapa-*.md`). Prompts dominavam visualmente o diretorio. Movidos para nova subpasta `docs/prompts/` via `git mv` (preserva historico via rename detection). `docs/` fica com hierarquia clara: 6 docs principais + 1 subpasta. Padrao para prompts futuros: `docs/prompts/prompt-etapa-X-Y.md`.

**CLAUDE.md atualizado** (regra 4.6 — 4.13 e a sub-etapa causadora das convencoes "split por crescimento" + "prompts em `docs/prompts/`"). Duas linhas em "Onde buscar mais": uma para `progresso-historico.md`, outra ajustando a referencia de prompts para apontar para `docs/prompts/`.

**Debito registrado em `hooks-pendentes.md`:** "Aplicar split analogo em `decisoes.md` quando cruzar 800 linhas. Padrao consolidado pela 4.13: split por secao funcional (subsecoes de sub-etapas antigas viram `decisoes-historico.md`). Tratar quando dor real aparecer — `decisoes.md` em ~600 linhas hoje, ainda navegavel."

**Smoke 4.12 marcado como concluido nesta sub-etapa.** Relato do operador (PR #35 revisado via `/review-arch`): output usou as 3 secoes prescritas, ancoragem nominal em ADR-004/006/007, Sonnet articulou raciocinio arquitetural com profundidade qualitativamente diferente do Haiku. Padrao skill+subagent validado em 2 casos.

**Categoria operacional "manutencao de docs por crescimento":** padrao replicavel para `decisoes.md`, `adrs.md`, outros docs futuros. Distinta de outras categorias operacionais ja consolidadas (auditoria meta-operacional, errata de ADR, replicacao de padrao, refinamento pos-smoke). Trata dor especifica de tamanho/legibilidade, nao de comportamento ou decisao estrutural.
```

### Conteudo do debito em `docs/hooks-pendentes.md`

Adicionar item na secao "Debitos meta-operacionais" (criada na 4.10), apos os 3 debitos existentes (memoria global, built-in agents, plugins globais):

```markdown
- **Aplicar split analogo em `decisoes.md` quando cruzar 800 linhas.** (Descoberto na 4.13.) `decisoes.md` em ~600 linhas hoje, ainda navegavel. Padrao consolidado pela 4.13: split por secao funcional — subsecoes de sub-etapas antigas (`### <decisao> (Sub-etapa X.Y)`) movem para `decisoes-historico.md` quando dor real aparecer. Criterio paralelo ao do `progresso.md`: agrupar por Camada cobertas pelas subsecoes (ex: subsecoes 4.0-4.7 se decisao agrupar bem por sub-etapas iniciais; ou por bloco conceitual: stack, convencoes operacionais, decisoes de Claude Code). Resolver quando hook 4.4 alertar consistentemente em `decisoes.md`.
```

### Conteudo das atualizacoes em `CLAUDE.md`

Na secao **"## Onde buscar mais"**, fazer **duas mudancas**:

**Mudanca 1 — Adicionar linha sobre `progresso-historico.md`** logo apos a linha de `progresso.md`:

Texto atual (linhas ~77-81):

```markdown
Documentos de referencia em `docs/`:

- `progresso.md` -- onde estamos. Tracking de Camadas e sub-etapas. Licoes meta-operacionais.
- `decisoes.md` -- escolhas tomadas. Por que cada regra existe.
- `adrs.md` -- decisoes arquiteturais formais.
- `hooks-pendentes.md` -- backlog de hooks + hooks implementados (lista completa).
- `visao.md` -- direcao do projeto e Camadas planejadas.
```

Texto novo (insere nova linha depois de `progresso.md`):

```markdown
Documentos de referencia em `docs/`:

- `progresso.md` -- onde estamos. Tracking de Camadas e sub-etapas. Licoes meta-operacionais.
- `progresso-historico.md` -- historico arquivado de Camadas concluidas (Camadas 0, 1, 2 ate sub-etapa 3.8).
- `decisoes.md` -- escolhas tomadas. Por que cada regra existe.
- `adrs.md` -- decisoes arquiteturais formais.
- `hooks-pendentes.md` -- backlog de hooks + hooks implementados (lista completa).
- `visao.md` -- direcao do projeto e Camadas planejadas.
```

**Mudanca 2 — Ajustar referencia a prompts** (logo apos a lista acima):

Texto atual:

```markdown
Prompts versionados de cada sub-etapa ficam em `docs/prompt-etapa-X-Y.md`. Nao listados individualmente; agente busca quando precisa.
```

Texto novo:

```markdown
Prompts versionados de cada sub-etapa ficam em `docs/prompts/prompt-etapa-X-Y.md`. Nao listados individualmente; agente busca quando precisa.
```

### Versionar este proprio prompt

`docs/prompts/prompt-etapa-4-13.md` entra como novo arquivo no Commit 6. **Atencao especial:** este arquivo de prompt deve ser anexado diretamente em `docs/prompts/` (nao em `docs/`). Se o operador anexou em `docs/prompt-etapa-4-13.md` (padrao antigo), mover para `docs/prompts/prompt-etapa-4-13.md` via `git mv` ou `Move-Item` antes do commit.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra squash da 4.12.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-13.md` **OU** `docs/prompts/prompt-etapa-4-13.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `.claude/agents/pr-reviewer.md` e `.claude/agents/architect-reviewer.md` existem — nao serao modificados.
- `.claude/skills/review-pr/SKILL.md` e `.claude/skills/review-arch/SKILL.md` existem — nao serao modificadas.
- `docs/prompt-etapa-*.md` presentes em `docs/` (~13-17 arquivos, contagem exata a confirmar via Tarefa 4).

Validar com:

```powershell
git status
git log --oneline -3
git config core.hooksPath
Get-ChildItem docs\prompt-etapa-*.md | Measure-Object | Select-Object -ExpandProperty Count
Test-Path docs\prompts
```

**Pre-condicoes ADR-011:**

- Working tree limpo exceto o prompt.
- Sem conflitos pendentes.

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
git checkout -b docs/etapa-4-13-split-progresso-prompts
```

Prefixo `docs/` — sub-etapa de manutencao de docs (sem mudanca em codigo).

### Tarefa 4 — Antes de editar, ler arquivos vivos e listar prompts existentes

```bash
cat docs/progresso.md
cat docs/decisoes.md
cat docs/hooks-pendentes.md
cat CLAUDE.md
```

```powershell
Get-ChildItem docs\prompt-etapa-*.md | Select-Object Name, Length
Get-ChildItem docs\prompt-etapa-*.md | Measure-Object | Select-Object -ExpandProperty Count
Test-Path docs\prompts
```

**Confirmar:**

- `progresso.md` tem secoes:
  - `## Camada 0 — Discovery` (a remover).
  - `## Camada 1 — Infraestrutura de confianca` (a remover).
  - `## Camada 2 — Arquitetura otimizada para agentes` (a remover).
  - `## Camada 3 — Configuracao do Claude Code` (mantida — sub-etapa 4.13 entra no topo de "Sub-etapas concluidas").
  - `## Camada 4 — Modelo operacional` (mantida).
  - `## Camada 5 — Runtime de agentes (VPS)` (mantida).
  - `## Camada 6 — Gestao hibrida Max + API` (mantida).
  - `## Metricas a capturar` (mantida).
  - `## Licoes da Etapa 1.1` (a remover — bloco isolado).
  - `## Licoes da Sub-etapa 4.X` (mantidas — Camada 3 inteira: 4.13, 4.12, 4.11, 4.10, 4.9.1, 4.9, 4.8, 4.7.1, 4.7, 4.6, 4.5, 4.4, 4.3, 4.2.1, 4.2, 4.1, 4.0).
  - **Blocos `## Licoes da Etapa X.Y` para X em {1, 2, 3}** apos as Licoes 4.X (a remover — todos, sequencia comeca em `## Licoes da Etapa 3.8` e termina antes de `## Historico de mudancas deste documento`).
  - `## Historico de mudancas deste documento` (mantido inteiro).
- `decisoes.md` tem subsecao "Segundo subagent: architect-reviewer + skill /review-arch (Sub-etapa 4.12)" antes de "Claude Code hooks nativos". Nova subsecao 4.13 entra **entre** essas duas.
- `hooks-pendentes.md` tem secao "Debitos meta-operacionais" (criada na 4.10) com 3 debitos. Novo debito de split entra como 4o item da secao.
- `CLAUDE.md` tem secao "Onde buscar mais" com lista de docs em `docs/` e linha sobre prompts. Sera atualizado conforme escopo.
- `docs/prompt-etapa-*.md`: contar quantos. Esperado ~13-17. **Anotar o numero exato para validacao posterior.**
- `docs/prompts/` NAO existe ainda.

Se alguma divergencia, **parar e reportar**.

### Tarefa 5 — Criar `docs/progresso-historico.md`

Criar arquivo com:

1. Cabecalho prescrito no escopo (titulo + paragrafo + tabela de mapeamento).
2. **Conteudo extraido integralmente do `progresso.md`**, na ordem:
   - Bloco `## Camada 0 — Discovery` (do cabecalho ate antes de `## Camada 1`).
   - Bloco `## Camada 1 — Infraestrutura de confianca` (do cabecalho ate antes de `## Camada 2`).
   - Bloco `## Camada 2 — Arquitetura otimizada para agentes` (do cabecalho ate antes de `## Camada 3`).
   - Bloco `## Licoes da Etapa 1.1` (inteiro).
   - Bloco a partir de `## Licoes da Etapa 3.8` ate antes de `## Historico de mudancas deste documento` (inteiro — inclui Licoes 3.X, 2.X, 1.X em ordem cronologica reversa).

**Preservacao fiel:** copiar bytes exatos (encoding UTF-8 sem BOM). Nao parafrasear, nao reorganizar internamente, nao editar conteudo das licoes.

**Pre-condicao ADR-011 apos criar:**

```powershell
Test-Path docs\progresso-historico.md   # True

$bytes = [System.IO.File]::ReadAllBytes("docs/progresso-historico.md")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191 (sem BOM)

$content = [System.IO.File]::ReadAllText("docs/progresso-historico.md", [System.Text.UTF8Encoding]::new($false))

# Cabecalho presente
if ($content -match '# Progresso.{1,5}Historico Arquivado') {
    Write-Host "Cabecalho OK"
} else {
    Write-Host "Cabecalho AUSENTE"
}

# Tabela de mapeamento presente
if ($content -match 'Mapeamento etapa.{1,5}Camada') {
    Write-Host "Tabela mapeamento OK"
} else {
    Write-Host "Tabela mapeamento AUSENTE"
}

# Camadas 0, 1, 2 presentes
$camadas = @('## Camada 0', '## Camada 1', '## Camada 2')
foreach ($c in $camadas) {
    if ($content -match [regex]::Escape($c)) {
        Write-Host "Camada OK: $c"
    } else {
        Write-Host "Camada AUSENTE: $c"
    }
}

# Camada 3 NAO presente neste arquivo
if ($content -match '## Camada 3') {
    Write-Host "ERRO: Camada 3 vazou para historico"
} else {
    Write-Host "Camada 3 corretamente ausente"
}

# Linhas totais (esperado: ~500)
$linhas = (Get-Content docs\progresso-historico.md).Count
Write-Host "Linhas totais: $linhas (esperado: ~500)"
```

Se algum valor divergir, parar e reportar.

### Tarefa 6 — Editar `docs/progresso.md` (remover blocos movidos + adicionar 4.13)

Aplicar:

1. **Remover bloco** `## Camada 0 — Discovery` ate antes de `## Camada 1`.
2. **Remover bloco** `## Camada 1 — Infraestrutura de confianca` ate antes de `## Camada 2`.
3. **Remover bloco** `## Camada 2 — Arquitetura otimizada para agentes` ate antes de `## Camada 3`.
4. **Atualizar tabela "Status geral"** com links para `progresso-historico.md` nas Camadas 0/1/2 + adicionar nota apos tabela (conforme escopo).
5. **Remover bloco** `## Licoes da Etapa 1.1`.
6. **Remover blocos** `## Licoes da Etapa X.Y` (para X em {1, 2, 3}) apos `## Licoes da Sub-etapa 4.0`. Sequencia comeca em `## Licoes da Etapa 3.8` e termina antes de `## Historico de mudancas deste documento`. **Remover tudo nesse intervalo.**
7. **Adicionar sub-etapa 4.13** ao topo de "Sub-etapas concluidas" da Camada 3 (acima da 4.12).
8. **Substituir bloco "Criterios de 'pronto' (preliminar, ajustados pela 4.10, 4.11 e 4.12)"** pela versao ajustada 4.10+4.11+4.12+4.13 (smoke 4.12 marcado como `[x]`).
9. **Adicionar bloco "Licoes da Sub-etapa 4.13"** acima de "Licoes da Sub-etapa 4.12".
10. **Adicionar linha de historico** acima da entrada da 4.12.

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/progresso.md", [System.Text.UTF8Encoding]::new($false))

# Camadas 0, 1, 2 NAO mais presentes como secao
$camadas_removidas = @('## Camada 0', '## Camada 1.{1,5}Infraestrutura', '## Camada 2.{1,5}Arquitetura')
foreach ($c in $camadas_removidas) {
    if ($content -match $c) {
        Write-Host "ERRO: $c ainda presente no vivo"
    } else {
        Write-Host "Removido OK: $c"
    }
}

# Camada 3 ainda presente
if ($content -match '## Camada 3') {
    Write-Host "Camada 3 OK"
} else {
    Write-Host "ERRO: Camada 3 removida indevidamente"
}

# Tabela Status geral com link para historico
if ($content -match 'progresso-historico.md') {
    Write-Host "Link historico OK"
} else {
    Write-Host "Link historico AUSENTE"
}

# Licoes 1.X, 2.X, 3.X NAO mais presentes
$licoes_removidas = @('## Licoes da Etapa 1.1', '## Licoes da Etapa 3.8', '## Licoes da Etapa 2.1', '## Licoes da Etapa 1.5')
foreach ($l in $licoes_removidas) {
    if ($content -match [regex]::Escape($l)) {
        Write-Host "ERRO: $l ainda presente no vivo"
    } else {
        Write-Host "Removido OK: $l"
    }
}

# Licoes 4.X ainda presentes
if ($content -match '## Licoes da Sub-etapa 4\.0') {
    Write-Host "Licoes 4.0 OK"
} else {
    Write-Host "ERRO: Licoes 4.0 removida indevidamente"
}

# Sub-etapa 4.13 presente
if ($content -match '4\.13.{1,5}Split do') {
    Write-Host "Sub-etapa 4.13 OK"
} else {
    Write-Host "Sub-etapa 4.13 AUSENTE"
}

# Criterio smoke 4.12 marcado como concluido
if ($content -match '\[x\] Smoke pos-merge da 4\.12') {
    Write-Host "Criterio smoke 4.12 OK"
} else {
    Write-Host "Criterio smoke 4.12 AUSENTE ou nao marcado"
}

# Linhas totais (esperado: ~400)
$linhas = (Get-Content docs\progresso.md).Count
Write-Host "Linhas totais: $linhas (esperado: ~400)"
```

Se algum valor divergir, parar e reportar.

**Atencao:** hook 4.4 (tamanho de docs, modo warn) **NAO** deve alertar mais nesta sub-etapa para `progresso.md` (saiu de ~900 para ~400, abaixo de 800). Se ainda alertar, investigar — pode ter sobrado bloco grande nao previsto.

### Tarefa 7 — Criar `docs/prompts/` e mover prompts existentes via `git mv`

```powershell
# Criar diretorio
New-Item -ItemType Directory -Path docs\prompts -Force

# Listar prompts a mover
Get-ChildItem docs\prompt-etapa-*.md | Select-Object Name
```

**Mover via `git mv`** (preserva historico por arquivo):

```bash
# Listar arquivos primeiro
git ls-files docs/prompt-etapa-*.md

# Mover cada um
git mv docs/prompt-etapa-4-0.md docs/prompts/prompt-etapa-4-0.md
git mv docs/prompt-etapa-4-0-1.md docs/prompts/prompt-etapa-4-0-1.md
# ... repetir para cada arquivo encontrado em git ls-files
```

**Alternativa em batch via bash** (se preferir):

```bash
for f in $(git ls-files docs/prompt-etapa-*.md); do
    target="docs/prompts/$(basename "$f")"
    git mv "$f" "$target"
done
```

**Atencao:** wildcard `git mv docs/prompt-etapa-*.md docs/prompts/` pode funcionar em alguns shells mas nem todos. O loop e mais seguro. Use shell bash do Git for Windows para o loop.

**Importante:** o **proprio arquivo deste prompt** (`prompt-etapa-4-13.md`) ainda nao foi staged nem commitado. Se o operador anexou em `docs/prompt-etapa-4-13.md` (padrao antigo), tambem mover via `Move-Item` ou `git mv` apos add. Se ja esta em `docs/prompts/prompt-etapa-4-13.md`, nao precisa mover. **Verificar localizacao na Tarefa 4 e ajustar antes do Commit 6.**

**Pre-condicao ADR-011 apos mover:**

```powershell
# Diretorio criado
Test-Path docs\prompts   # True

# Prompts movidos
Get-ChildItem docs\prompts\prompt-etapa-*.md | Measure-Object | Select-Object -ExpandProperty Count
# Esperado: mesma quantidade que estava em docs\ antes da Tarefa 4

# docs\prompt-etapa-*.md NAO existe mais no diretorio raiz docs\
Get-ChildItem docs\prompt-etapa-*.md -ErrorAction SilentlyContinue
# Esperado: vazio (ou apenas o prompt-etapa-4-13.md se ainda nao foi movido)

# Git status mostra renames
git status
# Esperado: arquivos como "renamed: docs/prompt-etapa-X.md -> docs/prompts/prompt-etapa-X.md"
```

Se git status mostrar arquivos como "deleted" + "new file" em vez de "renamed", **rename detection falhou** — git nao reconheceu como rename. Provavel causa: conteudo do arquivo foi modificado durante a operacao. Investigar e reverter.

### Tarefa 8 — Atualizar `CLAUDE.md` (2 linhas em "Onde buscar mais")

Aplicar **Mudanca 1** (adicionar linha sobre `progresso-historico.md`) e **Mudanca 2** (ajustar referencia de prompts) conforme escopo.

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("CLAUDE.md", [System.Text.UTF8Encoding]::new($false))

if ($content -match 'progresso-historico\.md.{1,200}historico arquivado') {
    Write-Host "Linha progresso-historico OK"
} else {
    Write-Host "Linha progresso-historico AUSENTE"
}

if ($content -match 'docs/prompts/prompt-etapa') {
    Write-Host "Referencia atualizada OK"
} else {
    Write-Host "Referencia ainda em docs/prompt-etapa (nao docs/prompts/)"
}
```

### Tarefa 9 — Atualizar `docs/decisoes.md` (subsecao 4.13)

Copiar bloco "Conteudo da subsecao em decisoes.md" do escopo. Inserir **antes** da subsecao `### Claude Code hooks nativos`, **apos** a subsecao "Segundo subagent: architect-reviewer + skill /review-arch (Sub-etapa 4.12)".

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/decisoes.md", [System.Text.UTF8Encoding]::new($false))

if ($content -match '### Manutencao de docs por crescimento') {
    Write-Host "Subsecao 4.13 OK"
} else {
    Write-Host "Subsecao 4.13 AUSENTE"
}

# Ordem: 4.12 antes da 4.13 antes de hooks nativos
$pos412 = $content.IndexOf('Segundo subagent')
$pos413 = $content.IndexOf('Manutencao de docs')
$posNativos = $content.IndexOf('Claude Code hooks nativos')
if ($pos412 -lt $pos413 -and $pos413 -lt $posNativos) {
    Write-Host "Ordem OK"
} else {
    Write-Host "Ordem TROCADA"
}
```

### Tarefa 10 — Atualizar `docs/hooks-pendentes.md` (debito de split decisoes.md)

Adicionar item ao final da secao "Debitos meta-operacionais" conforme escopo.

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/hooks-pendentes.md", [System.Text.UTF8Encoding]::new($false))

if ($content -match 'split analogo em.{1,30}decisoes\.md') {
    Write-Host "Debito split decisoes OK"
} else {
    Write-Host "Debito split decisoes AUSENTE"
}
```

### Tarefa 11 — Mover (ou validar localizacao) deste proprio prompt

Verificar onde o operador anexou o `prompt-etapa-4-13.md`:

```powershell
Test-Path docs\prompt-etapa-4-13.md          # se True, padrao antigo -- mover
Test-Path docs\prompts\prompt-etapa-4-13.md  # se True, padrao novo -- manter
```

**Se em `docs/`:** mover para `docs/prompts/`:

```bash
git mv docs/prompt-etapa-4-13.md docs/prompts/prompt-etapa-4-13.md
```

(Ou `Move-Item` se ainda nao foi staged.)

**Se em `docs/prompts/`:** ja esta no destino, nada a fazer.

### Tarefa 12 — Commits (6 commits)

**Commit 1** — Criar historico:

```bash
git add docs/progresso-historico.md
git status   # apenas progresso-historico.md staged
git commit -m "docs: cria progresso-historico.md com Camadas 0/1/2 (sub-etapa 4.13)"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 2** — Editar progresso.md (remocoes + atualizacao Status geral, ainda sem 4.13):

Hmm — tarefa 6 ja inclui adicionar 4.13. Como separar?

**Alternativa:** fazer Tarefa 6 em 2 passos:

- **Passo 6a:** apenas remocoes (blocos Camadas 0/1/2 + Licoes antigas) + atualizacao da tabela Status geral.
- **Passo 6b:** adicoes da 4.13 (sub-etapa, criterios, licoes, historico).

Reorganizando os commits:

**Commit 2** — Remover do progresso.md o conteudo movido (apos passo 6a):

```bash
git add docs/progresso.md
git status   # apenas progresso.md staged
git commit -m "docs: remove Camadas 0/1/2 e licoes antigas do progresso.md (movidas para historico)"
```

**Commit 3** — Mover prompts via git mv:

```bash
# Apos Tarefa 7 (git mv em loop)
git status   # arquivos renomeados
git commit -m "docs: reorganiza docs/prompt-etapa-*.md para docs/prompts/ (preserva historico via git mv)"
```

**Pre-condicao ADR-011:** ~13-17 arquivos como `renamed`; `$LASTEXITCODE = 0`.

**Commit 4** — Atualizar CLAUDE.md + decisoes.md + hooks-pendentes.md (convencoes registradas):

```bash
git add CLAUDE.md docs/decisoes.md docs/hooks-pendentes.md
git status   # 3 arquivos staged
git commit -m "docs: registra convencoes 4.13 (split de docs + docs/prompts/) em CLAUDE.md, decisoes, hooks-pendentes"
```

**Pre-condicao ADR-011:** 3 arquivos staged; `$LASTEXITCODE = 0`.

**Commit 5** — Adicionar sub-etapa 4.13 ao progresso.md (passo 6b):

```bash
git add docs/progresso.md
git status   # apenas progresso.md staged
git commit -m "docs: sub-etapa 4.13 -- registra split + manutencao de docs por crescimento"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Atencao:** hook 4.4 NAO deve alertar — progresso.md agora em ~400 linhas.

**Commit 6** — Versionar prompt (no destino novo):

```bash
git add docs/prompts/prompt-etapa-4-13.md
git status   # 1 arquivo staged
git commit -m "docs: versiona prompt-etapa-4-13.md em docs/prompts/"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Validacao implicita dos hooks ativos:**

- Conventional Commits (4.1): mensagens validas.
- Encoding UTF-8 (4.2): valida bytes em todos os arquivos editados/criados.
- Markdown blank lines (4.3): valida headers em `.md` editados.
- Tamanho de docs (4.4): NAO deve alertar (vivo agora em ~400 linhas; historico tem ~500 mas nao esta em `docs/` raiz com path padrao do hook).

**Atencao especial:** o hook 4.4 verifica `.md` em `docs/`. `docs/progresso-historico.md` (~500 linhas) e `docs/prompts/prompt-etapa-*.md` (alguns >800 linhas, como o 4.12 com 953) sao verificados pelo hook se o path padrao do hook nao excluir subpastas. **Investigar comportamento do hook 4.4 em arquivos de `docs/prompts/`** durante o commit:

- Se hook 4.4 alertar em arquivos de `docs/prompts/`: comportamento esperado para arquivos antigos (4.12 = 953 linhas). Sao alertas warn, **nao bloqueiam**. Mas registrar como observacao no PR body.
- Se for desejavel que prompts sejam excluidos da verificacao 4.4 (sao registros historicos de prompts, nao docs vivos): vira **debito** em `hooks-pendentes.md` para sub-etapa futura — modificar o hook 4.4 para excluir `docs/prompts/`. NAO modificar hook nesta sub-etapa (estoura escopo).

### Tarefa 13 — Validacao final antes de push

```bash
git status
git log --oneline -10
git config core.hooksPath
```

```powershell
Test-Path docs\progresso-historico.md             # True
Test-Path docs\prompts                            # True
Get-ChildItem docs\prompts\prompt-etapa-*.md | Measure-Object | Select-Object -ExpandProperty Count
# Esperado: contagem da Tarefa 4 + 1 (prompt da 4.13)

Get-ChildItem docs\prompt-etapa-*.md -ErrorAction SilentlyContinue
# Esperado: vazio (todos movidos)

(Get-Content docs\progresso.md).Count               # ~400
(Get-Content docs\progresso-historico.md).Count     # ~500
```

Esperado:

- Working tree limpo.
- 6 commits novos.
- `progresso.md` enxuto (~400); `progresso-historico.md` criado (~500); `docs/prompts/` com todos os prompts; CLAUDE.md/decisoes.md/hooks-pendentes.md atualizados.

## Restricoes e freios

1. **NAO modificar conteudo das licoes ao mover.** Copia byte-a-byte do `progresso.md` para `progresso-historico.md`. Sem reformatacao, sem reordenacao, sem correcao de typos. Preservacao fiel.

2. **NAO modificar conteudo das Camadas 0/1/2 ao mover.** Mesma regra acima.

3. **NAO mexer em `.claude/agents/`, `.claude/skills/`, `.claude/hooks/`, `.githooks/`.** Sub-etapa de docs apenas.

4. **NAO modificar `docs/adrs.md`, `docs/visao.md`, `docs/blueprint-fabrica-ai-native.md`.** Fora do escopo de manutencao.

5. **NAO modificar `docs/decisoes.md` alem da adicao da subsecao 4.13.** Split de `decisoes.md` e **debito** registrado em `hooks-pendentes.md`, NAO executado nesta sub-etapa.

6. **NAO modificar nenhum hook** (incluindo o 4.4 que pode alertar em arquivos de `docs/prompts/`). Se o hook 4.4 alertar para prompts antigos longos, registrar como debito em `hooks-pendentes.md` — NAO modificar o hook agora.

7. **Mover arquivos via `git mv`, nao `mv` ou `Move-Item`.** Preserva rename detection. Se git status mostrar "deleted + new file" em vez de "renamed", parar e investigar.

8. **Validar contagem de prompts antes e depois do git mv.** Quantidade no `docs/prompts/` apos move == quantidade em `docs/` antes da Tarefa 4 (sem contar o `prompt-etapa-4-13.md` que pode ou nao estar no destino certo desde o inicio).

9. **NAO sair com prompts em ambos os lugares.** Apos Tarefa 7, `docs/prompt-etapa-*.md` deve estar vazio (zero arquivos) e `docs/prompts/prompt-etapa-*.md` deve ter tudo.

10. **Encoding UTF-8 sem BOM** em todos os arquivos criados/editados.

11. **Apenas ASCII em mensagens de commit.** Sem acentos, sem em-dash U+2014.

12. **Ordem cronologica descrescente** em "Sub-etapas concluidas" da Camada 3, "Licoes da Sub-etapa", e "Historico de mudancas".

13. **Sem cenarios destrutivos tradicionais.** Sub-etapa de manutencao — validacao via pre-condicoes ADR-011 em cada Tarefa.

14. **Hook 4.4 NAO deve alertar mais para `progresso.md`** apos Commit 2 (vivo em ~400 linhas). Se alertar, indica que remocao foi incompleta — investigar.

15. **Hook 4.4 pode alertar para arquivos em `docs/prompts/`** (prompts antigos longos). Comportamento esperado se hook verifica subpastas — **registrar como debito** se aparecer, NAO modificar hook.

16. **Nao sugerir proxima sub-etapa** espontaneamente alem dos candidatos ja citados (test-writer, /feature, mitigacao meta-operacional, eventual split de decisoes.md quando crescer).

17. **Nao tomar decisao silenciosa em zona limitrofe.** Reportar divergencias.

18. **Nao usar `pwsh`.** PowerShell 5.1.

19. **Nao usar `git reset --hard`.**

20. **Nao usar `git commit --no-verify`.**

## Estrutura de commits

Branch: `docs/etapa-4-13-split-progresso-prompts`

**Commit 1** — `docs: cria progresso-historico.md com Camadas 0/1/2 (sub-etapa 4.13)`

- `docs/progresso-historico.md` (novo)

**Commit 2** — `docs: remove Camadas 0/1/2 e licoes antigas do progresso.md (movidas para historico)`

- `docs/progresso.md` (remocoes + atualizacao da tabela Status geral; sem 4.13 ainda)

**Commit 3** — `docs: reorganiza docs/prompt-etapa-*.md para docs/prompts/ (preserva historico via git mv)`

- `docs/prompt-etapa-*.md` -> `docs/prompts/prompt-etapa-*.md` (~13-17 arquivos como renamed)

**Commit 4** — `docs: registra convencoes 4.13 (split de docs + docs/prompts/) em CLAUDE.md, decisoes, hooks-pendentes`

- `CLAUDE.md` (2 linhas em "Onde buscar mais")
- `docs/decisoes.md` (subsecao 4.13)
- `docs/hooks-pendentes.md` (debito de split `decisoes.md`)

**Commit 5** — `docs: sub-etapa 4.13 -- registra split + manutencao de docs por crescimento`

- `docs/progresso.md` (sub-etapa 4.13 + criterios + licoes + historico)

**Commit 6** — `docs: versiona prompt-etapa-4-13.md em docs/prompts/`

- `docs/prompts/prompt-etapa-4-13.md` (novo, no destino)

## Validacao antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -10
git config core.hooksPath
```

```powershell
Test-Path docs\progresso-historico.md
Test-Path docs\prompts
Get-ChildItem docs\prompts | Measure-Object | Select-Object -ExpandProperty Count
Get-ChildItem docs\prompt-etapa-*.md -ErrorAction SilentlyContinue
(Get-Content docs\progresso.md).Count
(Get-Content docs\progresso-historico.md).Count
```

Esperado:

- `check.ps1` passa.
- Working tree limpo.
- 6 commits novos.
- `progresso.md` ~400 linhas; `progresso-historico.md` ~500 linhas; `docs/prompts/` com todos os prompts + 4.13.

## PR

Titulo: `docs: sub-etapa 4.13 -- split do progresso.md por crescimento + reorganizacao de docs/prompts/`

Body sugerido:

````markdown
## Summary

Sub-etapa de **manutencao de docs por crescimento** (categoria nova). `progresso.md` cresceu para ~891 linhas; hook 4.4 (modo warn) alertando consistentemente. Split por Camada concluida: Camadas 0 (Discovery), 1 (Infraestrutura), 2 (Arquitetura) + suas Licoes movem para novo `docs/progresso-historico.md`. Camada 3 em andamento + Camadas 4-6 planejadas + Licoes 4.X + historico de mudancas permanecem no vivo. Reorganizacao adicional: `docs/prompt-etapa-*.md` movidos para `docs/prompts/` via `git mv` (preserva historico via rename detection).

### Por que esta sub-etapa existe

Dor real: `progresso.md` em ~891 linhas, hook 4.4 alertando em cada sub-etapa que toca o arquivo desde a 4.10 (854 linhas). Principio do projeto e "infraestrutura segue necessidade" — dor apareceu, manutencao agora.

### Categoria nova: "manutencao de docs por crescimento"

Distinta de:

- **"Auditoria meta-operacional"** (4.10): descobertas que afetam estrategia.
- **"Errata de ADR baseada em descoberta de documentacao oficial"** (4.11): corrige mecanismo literal de ADR.
- **"Replicacao de padrao consolidado"** (4.12): segunda aplicacao de padrao validado.
- **Esta categoria:** trata **dor de tamanho** — documento operacional cruzou limite de legibilidade. Sub-etapa quebra preservando informacao. Padrao replicavel para outros docs (decisoes.md, adrs.md) quando dor real aparecer.

### Mudancas estruturais

**`docs/progresso-historico.md`** (~500 linhas, novo):

- Cabecalho + tabela de mapeamento etapa -> Camada (numeracao do projeto nao e 1:1 com camadas).
- `## Camada 0 — Discovery` (movida inteira).
- `## Camada 1 — Infraestrutura de confianca` (movida inteira).
- `## Camada 2 — Arquitetura otimizada para agentes` (movida inteira).
- Todas as `## Licoes da Etapa 1.X, 2.X, 3.X` (movidas inteiras, preservacao byte-a-byte).

**`docs/progresso.md`** (~400 linhas, vivo):

- Cabecalho preservado.
- Tabela "Status geral" com links para `progresso-historico.md` nas Camadas 0/1/2.
- `## Camada 3 — Configuracao do Claude Code` (em andamento) com sub-etapa 4.13 no topo.
- `## Camada 4`, `## Camada 5`, `## Camada 6` (planejamento).
- `## Metricas a capturar`.
- Todas as `## Licoes da Sub-etapa 4.X` (Camada 3 inteira preservada).
- `## Historico de mudancas deste documento` (preservado inteiro, com linha 4.13 no topo).

**`docs/prompts/`** (novo diretorio):

- `~13-17 arquivos prompt-etapa-*.md` movidos via `git mv` (rename detection do git preserva historico individual de cada arquivo).
- `prompt-etapa-4-13.md` (este prompt) versionado no novo destino.

**`CLAUDE.md`:**

- Secao "Onde buscar mais" ganha referencia ao `progresso-historico.md` e atualiza referencia de prompts para `docs/prompts/`.

**`docs/decisoes.md`:**

- Subsecao "Manutencao de docs por crescimento (Sub-etapa 4.13)" antes de "Claude Code hooks nativos".

**`docs/hooks-pendentes.md`:**

- Debito explicito: "Aplicar split analogo em `decisoes.md` quando cruzar 800 linhas" (decisoes.md em ~600 hoje, padrao registrado para aplicacao futura).

### Smoke da 4.12 marcado como concluido

Operador validou smoke pos-merge da 4.12 em PR #35 (etapa 3.6 — transacao domain+infra). Output do `architect-reviewer` (Sonnet) usou as 3 secoes prescritas, ancorou nominalmente em ADR-004/006/007, Sonnet articulou raciocinio arquitetural com profundidade qualitativamente diferente do Haiku do `pr-reviewer`. Padrao skill+subagent validado em 2 casos (PR #55 + PR #35).

Criterio `[ ] Smoke pos-merge da 4.12 validando segundo par skill+subagent` agora marcado como `[x]`.

### Hook 4.4 — comportamento esperado

**Em `progresso.md` (vivo):** NAO deve alertar mais (saiu de ~900 para ~400, abaixo do limite 800).

**Em arquivos de `docs/prompts/`:** PODE alertar para prompts antigos longos (4.12 = 953 linhas; 4.11 = ~900). Comportamento esperado se hook verifica subpastas de `docs/`. Sao alertas warn, **nao bloqueiam**. **Registrar como debito em `hooks-pendentes.md`** apos PR criado (se alerta aparecer) — modificar hook 4.4 para excluir `docs/prompts/` fica como sub-etapa futura.

### Convencoes registradas (regra 4.6)

CLAUDE.md atualizado pela **terceira vez** (apos 4.6 e 4.11). Cada atualizacao corresponde a uma convencao causadora distinta:

- 4.6: criacao do CLAUDE.md estrutural.
- 4.11: convencao "subagents + skills" (primeira skill orquestradora entrou em uso).
- 4.13: convencoes "split de docs por crescimento" + "prompts em `docs/prompts/`".

### Smoke test pos-merge sugerido

1. Sessao nova do Claude Code.
2. Verificar que `progresso.md` lido pelo agente em sessao nova nao referencia mais Camadas 0/1/2 inline — apenas via link para historico.
3. Verificar que `/review-pr` e `/review-arch` continuam funcionando (componentes nao foram tocados, mas vale checar que `git mv` dos prompts nao quebrou nada).
4. Conferir que `git log --follow docs/prompts/prompt-etapa-4-7.md` mostra historico completo (preservacao via rename detection).

### Proximo passo

Decisao fora deste PR. Candidatos naturais:

- **4.14** — `test-writer` + skill `/write-test` (terceiro par skill+subagent; territorio novo: subagent que gera codigo).
- **4.14 alternativo** — Skill sem subagent `/feature <nome>` (cria estrutura de bounded context).
- **4.14 alternativo** — Mitigar debitos meta-operacionais da 4.10 (memoria global, plugins, built-ins).
- **Eventual 4.X** — Split analogo de `decisoes.md` quando cruzar 800 linhas (debito registrado).
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

- Branch `docs/etapa-4-13-split-progresso-prompts` empurrada com 7 commits (6 + 1 update do PR).
- PR aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.12.
- Working tree limpo.
- `docs/progresso-historico.md` existe (~500 linhas).
- `docs/progresso.md` existe (~400 linhas).
- `docs/prompts/` existe com todos os prompts versionados (`~14-18` arquivos contando o 4.13).
- `docs/prompt-etapa-*.md` no diretorio raiz `docs/` NAO existe mais.
- Componentes da Camada 3 (`pr-reviewer.md`, `architect-reviewer.md`, `review-pr/SKILL.md`, `review-arch/SKILL.md`) inalterados.
- Reportar: `git log --oneline -10`, `git status`, `gh pr view --json number,state,statusCheckRollup`, contagem de arquivos em `docs/prompts/`, linhas de `progresso.md` e `progresso-historico.md`.

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao executar split analogo em `decisoes.md` (fica como debito).
- Nao modificar hooks (mesmo se 4.4 alertar em `docs/prompts/`).
- Nao criar prompt da 4.14.
- Nao criar outros subagents, skills, hooks, MCPs.
- Nao modificar `.claude/agents/`, `.claude/skills/`, `.claude/hooks/`.
- Nao modificar `adrs.md`, `visao.md`, `blueprint-fabrica-ai-native.md`.
- Nao mexer em `~/.claude/` global.
- Nao sugerir proximo passo espontaneamente alem dos candidatos ja citados no PR body.
- Nao usar `pwsh`.
- Nao usar `git reset --hard`.
- Nao usar `git commit --no-verify`.
