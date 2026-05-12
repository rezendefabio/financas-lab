# Prompt — Etapa 4.15: Errata de auditoria meta-operacional (re-classificacao dos debitos 4.10)

## Contexto

Camada 3 com 6 hooks (4.1-4.7, com 4.4 refinado pela 4.14) + CLAUDE.md + blueprint + 2 subagents (`pr-reviewer` Haiku + `architect-reviewer` Sonnet) + 2 skills orquestradoras (`/review-pr` + `/review-arch`) + ADR-012 (revisao 4.11) + `progresso-historico.md` (4.13) apos a 4.14. Padrao skill+subagent validado em 2 casos (PRs #55, #45, #35).

A 4.10 (auditoria meta-operacional) registrou 3 debitos meta-operacionais em `hooks-pendentes.md` para mitigacao futura:

1. Memoria global auto-ON sem confirmacao.
2. Plugins globais nao-versionados.
3. Built-in agents do Claude Code competindo com subagents custom.

A intencao desta sub-etapa era **mitigar** esses debitos. Antes de calibrar D1-Dn, foi conduzida auditoria empirica de `~/.claude/` via comandos PowerShell. Resultados revelaram **disparidade entre o suposto e o real em dois dos tres debitos**:

**Achado 1 — Memoria vs Transcripts.**

`~/.claude/projects/` tem 17 diretorios de projeto, totalizando ~427 MB em 853 arquivos. **A pasta `memory/` (memoria derivada propriamente dita) aparece apenas em UM projeto e ocupa ~85 KB.** O restante dos 427 MB sao transcripts de conversa — logs brutos das sessoes do Claude Code, distribuidos diretamente sob cada hash de projeto. A 4.10 confundiu "memoria global" (resumos curtos derivados) com "transcripts" (logs brutos). Magnitude real do debito de memoria: minima. Questao de principio ("auto-ON sem confirmacao") permanece, mas impacto pratico e ordens de grandeza menor que o suposto.

**Achado 2 — Plugins oficiais cacheados, nao instalacoes manuais.**

Plugins `code-review` e `frontend-design` ficam em `~/.claude/plugins/cache/claude-plugins-official/`. Sao **plugins oficiais distribuidos pelo Claude Code**, cacheados pelo marketplace. Nao foram instalados manualmente pelo operador. Decisao sobre eles e decisao sobre setup pessoal do Claude Code, **nao sobre `financas-lab`**. Debito a remover do backlog do projeto.

**Achado 3 — Built-ins sem dor pratica observada.**

Operador relatou nao ter observado em sessoes recentes (maio/2026) delegacao para built-in agents (`Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup`) nem interferencia com subagents custom. Debito teorico, nao pratico. **Sob observacao, sem dor pratica.**

**Achado 4 — Transcripts fora do escopo do projeto.**

427 MB de transcripts em 17 projetos. Maior conversa unica: 90 MB. Sem rotina de expiracao automatica. **Fenomeno fora do escopo do projeto** — gestao de storage do Claude Code e decisao pessoal sobre setup, nao do `financas-lab`. Registrar para visibilidade, sem prescricao de acao.

**Reorientacao da sub-etapa:** ao inves de mitigar debitos pesados (escopo original), 4.15 entrega **errata documental** corrigindo o registro da 4.10 baseado em verificacao empirica posterior. Categoria operacional nova: **"errata de auditoria meta-operacional"** — distinta de auditoria (4.10) e errata de ADR (4.11).

Caracteristicas:

1. **Sub-etapa documental pura.** Sem mudanca em `.claude/`, hooks, subagents, skills, codigo. Apenas docs do projeto.

2. **Categoria nova "errata de auditoria meta-operacional".** Distinta de:
   - "Auditoria meta-operacional" (4.10): identifica debitos baseado em observacao inicial.
   - "Errata de ADR baseada em descoberta de documentacao oficial" (4.11): corrige decisao estrutural via doc oficial.
   - **Esta categoria:** corrige auditoria anterior baseada em verificacao empirica posterior. Aplicavel a casos onde auditoria inicial categoriza fenomeno mal, dimensiona errado, ou inclui itens fora do escopo real.

3. **Padrao "nota de errata + subsecao nova" replicado.** Identico ao usado na 4.11 para ADR-012 (registro original preservado integralmente; nota aponta para errata; subsecao nova explica o que mudou).

4. **CLAUDE.md NAO atualizado.** Errata nao muda convencao do projeto.

5. **Sem mudanca em `~/.claude/`.** Decisoes sobre desligar auto-memory, limpar transcripts, ou desinstalar plugins ficam **fora do escopo** — sao decisoes pessoais do operador sobre seu setup do Claude Code, nao do `financas-lab`.

Quando esta etapa terminar:

- `docs/decisoes.md`: subsecao "Errata de auditoria meta-operacional (Sub-etapa 4.15)" antes de "Claude Code hooks nativos" + nota de errata adicionada a subsecao da 4.10.
- `docs/hooks-pendentes.md`: item de plugins removido; itens de memoria e built-ins reescritos com re-classificacao; item novo de transcripts adicionado.
- `docs/progresso.md`: sub-etapa 4.15 + licoes + historico.
- `docs/prompts/prompt-etapa-4-15.md`: versionado.

## Padroes que estreiam nesta etapa

1. **Categoria nova: "errata de auditoria meta-operacional".** Instrumento para corrigir registros do projeto baseados em verificacao empirica posterior. Distinta de patch tecnico, refinamento pos-smoke, errata de ADR, replicacao, manutencao de docs, ajuste de hook por contexto novo. Especifica para casos onde **auditoria inicial tinha premissa errada**.

2. **"Auditar antes de mitigar" reforcado em segunda aplicacao.** A 4.13 dimensionou `progresso.md` antes de cortar (descobriu 891 linhas reais e cortou por Camada concluida). A 4.15 auditou `~/.claude/` antes de mitigar (descobriu que dois de tres debitos estavam baseados em premissa errada). Padrao operacional consolidado: **antes de mitigar territorio opaco, auditar empiricamente**. Custo da auditoria e baixo (3-5 comandos de inspecao); risco de pular auditoria e alto (mitigacao baseada em premissa errada).

3. **Debitos meta-operacionais sem dimensionamento empirico tem risco alto de descricao inflada.** A 4.10 registrou "memoria global" sem medir tamanho. Verificacao empirica mostrou ~85 KB reais. Recomendacao operacional formalizada nas licoes: futuras auditorias incluem comando de inspecao de magnitude antes de classificar como debito.

## Escopo decidido (calibrado com operador antes da redacao via D1-D5)

### Arquivos modificados

| Arquivo | Tipo de mudanca |
|---|---|
| `docs/decisoes.md` | Subsecao 4.15 antes de "Claude Code hooks nativos" + nota de errata na subsecao da 4.10 |
| `docs/hooks-pendentes.md` | Item plugins removido + itens memoria/built-ins reescritos + item transcripts adicionado |
| `docs/progresso.md` | Sub-etapa 4.15 + licoes + historico |
| `docs/prompts/prompt-etapa-4-15.md` | Versionado (novo) |

**Nao tocados:** `.claude/agents/`, `.claude/skills/`, `.claude/hooks/`, `.githooks/`, `pom.xml`, `src/`, `frontend/`, `CLAUDE.md`, `docs/adrs.md`, `docs/visao.md`, `docs/blueprint-fabrica-ai-native.md`, `docs/progresso-historico.md`, `.gitignore`, `.gitattributes`, `~/.claude/` global.

### Conteudo da nota de errata na subsecao da 4.10 (`decisoes.md`)

A 4.10 esta registrada em `decisoes.md` como subsecao "Auditoria meta-operacional + ADR-012 (Sub-etapa 4.10)" (titulo exato pode variar — identificar via leitura na Tarefa 4). Adicionar a seguinte nota **logo apos o titulo da subsecao 4.10, antes do corpo**:

```markdown
> **Errata (Sub-etapa 4.15):** auditoria empirica posterior revelou que dois dos tres debitos registrados aqui estavam baseados em premissas equivocadas. "Plugins globais" sao plugins oficiais cacheados (nao instalacoes manuais — debito removido do backlog). "Memoria global" foi confundida com "transcripts" (memoria real ~85 KB; transcripts ~427 MB). "Built-ins competindo" e teorico (sem dor pratica observada). Ver subsecao "Errata de auditoria meta-operacional (Sub-etapa 4.15)" abaixo para detalhes. Registro original desta subsecao preservado integralmente — auditoria registra o que sabiamos no momento.
```

### Conteudo da subsecao 4.15 (`decisoes.md`)

Inserir **antes** de `### Claude Code hooks nativos`, **apos** "Hook 4.4 exclui `docs/prompts/` (Sub-etapa 4.14)":

```markdown
### Errata de auditoria meta-operacional (Sub-etapa 4.15)

Sub-etapa de errata documental. Auditoria empirica de `~/.claude/` em maio/2026 revelou que **dois dos tres debitos meta-operacionais registrados na 4.10 estavam baseados em premissas equivocadas**. Esta sub-etapa corrige o registro sem alterar o conteudo original da 4.10 (preservacao historica).

**Verificacao empirica conduzida** via PowerShell em `~/.claude/projects/` e `~/.claude/plugins/`:

- `~/.claude/projects/` tem 17 diretorios de projeto totalizando ~427 MB em 853 arquivos.
- A pasta `memory/` (memoria derivada) aparece apenas em UM projeto e ocupa ~85 KB total.
- Os ~427 MB restantes sao transcripts de conversa — logs brutos das sessoes, distribuidos diretamente sob cada hash de projeto.
- Plugins `code-review` e `frontend-design` ficam em `~/.claude/plugins/cache/claude-plugins-official/` — plugins oficiais cacheados pelo marketplace `claude-plugins-official`, nao instalacoes manuais.

**Re-classificacao dos 3 debitos da 4.10:**

1. **"Memoria global auto-ON sem confirmacao"** -> **debito real, mas magnitude muito menor que o suposto.** Memoria derivada total: ~85 KB. A 4.10 confundiu memoria (resumos curtos) com transcripts (logs brutos de conversa). Questao de principio ("auto-ON sem confirmacao") permanece valida; impacto pratico minimo. Acao: reescrito em `hooks-pendentes.md` com dimensionamento real.

2. **"Plugins globais nao-versionados"** -> **nao e debito do projeto.** `code-review` e `frontend-design` sao plugins oficiais distribuidos pelo Claude Code, cacheados pelo marketplace `claude-plugins-official`. Equivalentes a built-ins. Decisao sobre eles e decisao sobre setup pessoal do Claude Code, nao sobre `financas-lab`. Acao: **removido do backlog do projeto** em `hooks-pendentes.md`.

3. **"Built-in agents competindo com subagents custom"** -> **sob observacao, sem dor pratica relatada.** Operador nao observou em sessoes recentes (maio/2026) delegacao para built-ins (`Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup`) nem interferencia com `pr-reviewer` ou `architect-reviewer`. Debito teorico, nao pratico. Acao: reescrito em `hooks-pendentes.md` como "sob observacao".

**Achado novo (nao registrado pela 4.10):**

4. **Transcripts em `~/.claude/projects/<hash>/<conversa-hash>/` ocupam ~427 MB em 17 projetos.** Sem rotina de expiracao automatica. Maior conversa unica: 90 MB. **Fenomeno fora do escopo do projeto** — gestao de storage do Claude Code e decisao pessoal sobre setup, nao do `financas-lab`. Acao: registrado em `hooks-pendentes.md` para visibilidade, sem prescricao de acao.

**Categoria operacional nova: "errata de auditoria meta-operacional".** Distinta de:

- **"Auditoria meta-operacional"** (4.10): identifica debitos baseado em observacao inicial.
- **"Errata de ADR baseada em descoberta de documentacao oficial"** (4.11): corrige decisao estrutural via doc oficial.
- **Esta categoria:** corrige auditoria anterior baseada em **verificacao empirica posterior**. Aplicavel a casos onde auditoria inicial categoriza fenomeno mal, dimensiona errado, ou inclui itens fora do escopo real.

**Padrao "auditar antes de mitigar" demonstrado em segunda aplicacao.** A 4.13 dimensionou `progresso.md` antes de cortar (descobriu 891 linhas reais e cortou por Camada concluida). A 4.15 auditou `~/.claude/` antes de mitigar (descobriu que dois debitos da 4.10 estavam baseados em premissa errada). Em ambos os casos, auditoria empirica ajustou o escopo da sub-etapa. **Recomendacao operacional consolidada:** antes de mitigar territorio opaco, auditar empiricamente. Custo da auditoria e baixo (3-5 comandos de inspecao); risco de pular auditoria e alto (mitigacao baseada em premissa errada).

**Nota de errata adicionada a subsecao da 4.10** em `decisoes.md` apontando para esta subsecao. Padrao identico a ADR-012 que recebeu errata via 4.11. Registro historico da 4.10 preservado integralmente.

**Nada modificado em `~/.claude/`.** Decisoes sobre desligar auto-memory, limpar transcripts, ou desinstalar plugins ficam **fora do escopo** — sao decisoes pessoais do operador sobre seu setup do Claude Code, nao do projeto `financas-lab`.
```

### Mudancas em `docs/hooks-pendentes.md`

A 4.10 criou secao "Debitos meta-operacionais" com 3 itens. Aplicar 4 mudancas:

**Mudanca 1 — Remover item sobre plugins.**

Identificar item por palavras-chave "plugins globais" + "nao-versionados" (ou similares). Remover bullet inteiro de uma so vez.

**Mudanca 2 — Reescrever item sobre memoria global.**

Identificar item por palavras-chave "memoria global" + "auto-memory" / "auto-ON" (ou similares). Substituir pelo texto:

```markdown
- **Memoria global auto-ON sem confirmacao.** (Identificado 4.10, re-classificado 4.15 apos auditoria empirica.) `~/.claude/projects/<hash>/memory/` armazena memoria derivada sem confirmacao explicita do operador. **Magnitude real apos auditoria: ~85 KB total** (somando todos os projetos do operador) — fracao minima dos ~427 MB que `~/.claude/projects/` ocupa. A 4.10 confundiu memoria (~85 KB de resumos) com transcripts (~427 MB de logs brutos). Questao de principio ("auto-ON sem confirmacao") permanece valida; impacto pratico minimo. Acao: se aparecer dor concreta (ex: vazamento percebido de informacao entre projetos, ou comportamento inesperado vinculado a memoria), revisitar e abrir sub-etapa de mitigacao. Sem necessidade de mitigacao imediata.
```

**Mudanca 3 — Reescrever item sobre built-ins.**

Identificar item por palavras-chave "built-in" + "agents" (ou similares). Substituir pelo texto:

```markdown
- **Built-in agents do Claude Code potencialmente competindo com subagents custom.** (Identificado 4.10, re-classificado 4.15.) Built-ins documentados (`Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup`) podem teoricamente competir com subagents custom (`pr-reviewer` Haiku, `architect-reviewer` Sonnet). **Sob observacao, sem dor pratica relatada.** Operador nao observou em sessoes recentes (maio/2026) delegacao para built-ins nem interferencia com subagents custom. Debito teorico, nao pratico. Acao: se aparecer caso onde Claude principal delega para built-in em vez de subagent custom esperado, registrar contexto especifico e abrir sub-etapa de mitigacao.
```

**Mudanca 4 — Adicionar item novo sobre transcripts.**

Acrescentar ao final da secao "Debitos meta-operacionais" (apos o item de built-ins reescrito):

```markdown
- **Transcripts em `~/.claude/projects/<hash>/<conversa-hash>/` sem rotina de expiracao.** (Achado 4.15 via auditoria empirica.) ~427 MB acumulados em 17 projetos do operador. Maior conversa unica: 90 MB. Sem mecanismo automatico de expiracao por idade ou limpeza. **Fora do escopo do projeto `financas-lab`** — gestao de storage do Claude Code e decisao pessoal sobre setup, nao do projeto. Registrado para visibilidade. Acao: nenhuma do lado do projeto. Operador pode considerar limpeza periodica de transcripts antigos no proprio fluxo (fora desta sub-etapa).
```

### Conteudo das edicoes em `docs/progresso.md`

**Edicao 1 — Sub-etapa 4.15 no topo de "Sub-etapas concluidas" da Camada 3** (acima da 4.14):

```markdown
- **4.15 — Errata de auditoria meta-operacional (re-classificacao dos debitos 4.10)** (2026-05-11): sub-etapa documental pura. Auditoria empirica de `~/.claude/` revelou que 2 dos 3 debitos da 4.10 estavam baseados em premissas equivocadas. Re-classificacao: plugins (`code-review`, `frontend-design`) sao oficiais cacheados em `~/.claude/plugins/cache/claude-plugins-official/` — fora do backlog do projeto; memoria global tem ~85 KB reais (vs ~427 MB de transcripts que a 4.10 confundiu como memoria); built-ins teoricos sem dor pratica observada. Achado novo: 427 MB de transcripts em `~/.claude/projects/` — fora do escopo do projeto, registrado para visibilidade. Categoria nova: **"errata de auditoria meta-operacional"** — distinta de auditoria (4.10) e errata de ADR (4.11). Nota de errata adicionada a subsecao 4.10 em `decisoes.md` (padrao identico a ADR-012 / 4.11). CLAUDE.md NAO atualizado (errata nao muda convencao). PR #XX.
```

**Edicao 2 — Bloco "Licoes da Sub-etapa 4.15"** acima de "Licoes da Sub-etapa 4.14":

```markdown
## Licoes da Sub-etapa 4.15

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — errata documental.)

### Licoes de ambiente

1. **Categoria nova: "errata de auditoria meta-operacional".** Distinta de "auditoria meta-operacional" (4.10 — identifica debitos baseado em observacao inicial) e "errata de ADR baseada em doc oficial" (4.11 — corrige decisao estrutural via doc oficial). Esta categoria corrige auditoria anterior baseada em **verificacao empirica posterior**. Aplicavel a qualquer caso onde auditoria inicial tinha premissa errada: categoriza fenomeno mal, dimensiona errado, inclui itens fora do escopo. Padrao replicavel.

2. **Auditoria empirica de territorio opaco revelou disparidade de magnitude entre o suposto e o real.** Debito "memoria global" foi registrado pela 4.10 sem dimensionamento — verificacao empirica mostrou ~85 KB reais. Sem auditoria, mitigacao teria sido baseada em premissa errada. Padrao operacional formalizado: **debitos meta-operacionais registrados sem dimensionamento empirico tem risco alto de descricao inflada**. Recomendacao para auditorias futuras: incluir comando de inspecao de magnitude (`Get-ChildItem -Recurse | Measure-Object -Property Length -Sum` ou equivalente) antes de classificar como debito.

3. **Plugins oficiais cacheados nao sao debito do projeto.** A 4.10 registrou "plugins globais nao-versionados" assumindo que `code-review` e `frontend-design` eram instalacoes manuais. Auditoria mostrou que ficam em `~/.claude/plugins/cache/claude-plugins-official/` — plugins oficiais distribuidos pelo Claude Code, equivalentes a built-ins. Padrao operacional: **antes de registrar debito sobre "ferramenta X instalada", verificar se foi instalacao deliberada do operador ou parte default do ambiente.** Categoria pratica: itens default do ambiente nao entram no backlog do projeto.

4. **Padrao "auditar antes de mitigar" demonstrado em segunda aplicacao.** Aplicado em 4.13 (dimensionar `progresso.md` antes de cortar — 891 linhas reais, corte por Camada concluida). Aplicado de novo em 4.15 (auditar `~/.claude/` antes de mitigar — descobertas que ajustaram escopo da sub-etapa). Em ambos os casos, auditoria empirica revelou informacao que mudou a sub-etapa. **Recomendacao operacional consolidada por dupla aplicacao:** antes de mitigar territorio opaco, auditar primeiro. Custo baixo (3-5 comandos); risco de pular alto (mitigacao errada).

5. **Decisoes sobre `~/.claude/` global ficam fora do escopo do projeto.** A 4.15 mantem-se estritamente documental — nao modifica auto-memory, nao limpa transcripts, nao desinstala plugins. Decisoes sobre setup pessoal do Claude Code sao do operador, nao do `financas-lab`. Padrao operacional: **sub-etapas do projeto nao prescrevem modificacoes em config global do Claude Code**, mesmo quando observam fenomeno relevante. Se mitigacao em config global virar necessidade, decisao pessoal do operador, nao sub-etapa do projeto.
```

**Edicao 3 — Linha no historico** acima da entrada da 4.14:

```markdown
- **2026-05-11** — Sub-etapa 4.15 concluida (errata documental): re-classificacao dos 3 debitos meta-operacionais da 4.10 apos auditoria empirica de `~/.claude/`. Plugins removidos do backlog (oficiais cacheados, nao instalacoes manuais). Memoria global re-dimensionada (~85 KB real vs ~427 MB de transcripts que a 4.10 confundiu). Built-ins re-classificados como "sob observacao sem dor pratica". Achado novo: 427 MB de transcripts em `~/.claude/projects/` — fora do escopo do projeto. Categoria nova: "errata de auditoria meta-operacional". Nota de errata na subsecao 4.10 (decisoes.md). CLAUDE.md NAO atualizado. PR #XX.
```

### Versionar este proprio prompt

`docs/prompts/prompt-etapa-4-15.md` entra como novo arquivo no Commit 4.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra squash da 4.14 (`8f43b18` ou hash atual).
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompts/prompt-etapa-4-15.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `docs/decisoes.md` tem subsecao "Auditoria meta-operacional + ADR-012 (Sub-etapa 4.10)" (titulo exato a confirmar via leitura).
- `docs/decisoes.md` tem subsecao "Hook 4.4 exclui `docs/prompts/` (Sub-etapa 4.14)" — sub-etapa anterior, ja mergeada.
- `docs/hooks-pendentes.md` tem secao "Debitos meta-operacionais" (criada na 4.10) com 3 itens (memoria, plugins, built-ins).

Validar com:

```powershell
git status
git log --oneline -3
git config core.hooksPath
Test-Path docs\prompts\prompt-etapa-4-15.md
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
git checkout -b docs/etapa-4-15-errata-auditoria-4-10
```

Prefixo `docs/` — sub-etapa documental pura.

### Tarefa 4 — Antes de editar, ler arquivos vivos

```bash
cat docs/decisoes.md
cat docs/progresso.md
cat docs/hooks-pendentes.md
```

**Confirmar e anotar:**

- `decisoes.md`: tem subsecao da 4.10 — **anotar titulo exato** (provavel "Auditoria meta-operacional + ADR-012 (Sub-etapa 4.10)" ou variacao). Nota de errata sera inserida logo apos esse titulo, antes do corpo da subsecao.
- `decisoes.md`: tem subsecao "Hook 4.4 exclui `docs/prompts/` (Sub-etapa 4.14)" antes de "Claude Code hooks nativos". Nova subsecao 4.15 entra **entre** essas duas.
- `progresso.md`: tem "Sub-etapas concluidas" da Camada 3 em ordem descrescente comecando em 4.14. Sub-etapa 4.15 entra **acima** da 4.14.
- `progresso.md`: tem "Licoes da Sub-etapa 4.14" — "Licoes da Sub-etapa 4.15" entra **acima**.
- `progresso.md`: tem entrada de historico da 4.14 — linha da 4.15 entra **acima**.
- `hooks-pendentes.md`: tem secao "Debitos meta-operacionais" com 3 itens. **Anotar textos exatos dos itens de plugins, memoria global e built-ins** para identificacao precisa na Tarefa 6.

Se alguma divergencia, **parar e reportar**.

### Tarefa 5 — Atualizar `docs/decisoes.md`

Aplicar duas mudancas:

**Mudanca 1 — Adicionar nota de errata** logo apos o titulo da subsecao da 4.10 (antes do corpo da subsecao), com o texto prescrito no escopo.

**Mudanca 2 — Inserir subsecao 4.15** antes de `### Claude Code hooks nativos`, apos "Hook 4.4 exclui `docs/prompts/` (Sub-etapa 4.14)", com o texto prescrito no escopo.

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/decisoes.md", [System.Text.UTF8Encoding]::new($false))

# Nota de errata adicionada
if ($content -match '> \*\*Errata \(Sub-etapa 4\.15\)') {
    Write-Host "Nota de errata na 4.10 OK"
} else {
    Write-Host "Nota de errata AUSENTE"
}

# Subsecao 4.15 presente
if ($content -match '### Errata de auditoria meta-operacional') {
    Write-Host "Subsecao 4.15 OK"
} else {
    Write-Host "Subsecao 4.15 AUSENTE"
}

# Ordem: 4.14 antes da 4.15 antes de hooks nativos
$pos414 = $content.IndexOf('Hook 4.4 exclui')
$pos415 = $content.IndexOf('Errata de auditoria meta-operacional')
$posNativos = $content.IndexOf('Claude Code hooks nativos')
if ($pos414 -lt $pos415 -and $pos415 -lt $posNativos) {
    Write-Host "Ordem OK"
} else {
    Write-Host "Ordem TROCADA"
}
```

### Tarefa 6 — Atualizar `docs/hooks-pendentes.md`

Aplicar 4 mudancas conforme escopo:

**Mudanca 1:** remover item sobre plugins (palavras-chave "plugins globais" + "nao-versionados").

**Mudanca 2:** reescrever item sobre memoria global (palavras-chave "memoria global" + "auto-memory" / "auto-ON") com texto novo prescrito.

**Mudanca 3:** reescrever item sobre built-ins (palavras-chave "built-in" + "agents") com texto novo prescrito.

**Mudanca 4:** adicionar item novo sobre transcripts ao final da secao "Debitos meta-operacionais".

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/hooks-pendentes.md", [System.Text.UTF8Encoding]::new($false))

# Plugins removidos
if ($content -match 'plugins globais.{1,30}nao-versionados') {
    Write-Host "ATENCAO: item de plugins ainda presente -- verificar remocao"
} else {
    Write-Host "Plugins removidos OK"
}

# Memoria reescrita com dimensionamento
if ($content -match 'Magnitude real apos auditoria.{1,30}85 KB') {
    Write-Host "Memoria reescrita OK"
} else {
    Write-Host "Memoria NAO reescrita"
}

# Built-ins reescritos
if ($content -match 'Sob observacao.{1,50}sem dor pratica relatada') {
    Write-Host "Built-ins reescritos OK"
} else {
    Write-Host "Built-ins NAO reescritos"
}

# Transcripts adicionado
if ($content -match 'Transcripts em.{1,80}sem rotina de expiracao') {
    Write-Host "Transcripts adicionado OK"
} else {
    Write-Host "Transcripts AUSENTE"
}
```

### Tarefa 7 — Atualizar `docs/progresso.md`

Aplicar **edicoes 1-3** descritas no escopo:

1. Sub-etapa 4.15 ao topo de "Sub-etapas concluidas" (acima da 4.14).
2. "Licoes da Sub-etapa 4.15" acima de "Licoes da Sub-etapa 4.14".
3. Linha de historico acima da entrada da 4.14.

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/progresso.md", [System.Text.UTF8Encoding]::new($false))

# Sub-etapa 4.15 presente
if ($content -match '4\.15.{1,10}Errata de auditoria') {
    Write-Host "Sub-etapa 4.15 OK"
} else {
    Write-Host "Sub-etapa 4.15 AUSENTE"
}

# Licoes da 4.15
if ($content -match '## Li.{1,3}es da Sub-etapa 4\.15') {
    Write-Host "Licoes 4.15 OK"
} else {
    Write-Host "Licoes 4.15 AUSENTE"
}

# Ordem cronologica
$pos415 = $content.IndexOf('**4.15')
$pos414 = $content.IndexOf('**4.14')
if ($pos415 -gt 0 -and $pos415 -lt $pos414) {
    Write-Host "Ordem cronologica OK"
} else {
    Write-Host "Ordem cronologica TROCADA"
}

# Linhas totais (esperado: 460-470, crescimento leve)
$linhas = (Get-Content docs\progresso.md).Count
Write-Host "Linhas totais: $linhas"
```

### Tarefa 8 — Commits (4 commits)

**Commit 1** — `decisoes.md` (errata + subsecao):

```bash
git add docs/decisoes.md
git status   # apenas decisoes.md staged
git commit -m "docs: errata da 4.10 + subsecao 4.15 (re-classificacao dos debitos meta-operacionais)"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 2** — `hooks-pendentes.md` (4 mudancas):

```bash
git add docs/hooks-pendentes.md
git status   # apenas hooks-pendentes.md staged
git commit -m "docs(hooks-pendentes): re-classifica debitos 4.10 + adiciona transcripts (4.15)"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 3** — `progresso.md` (3 edicoes):

```bash
git add docs/progresso.md
git status   # apenas progresso.md staged
git commit -m "docs: sub-etapa 4.15 -- registra errata de auditoria meta-operacional"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Atencao:** hook 4.4 NAO deve alertar — `progresso.md` em ~460-470 linhas, abaixo do limite 800.

**Commit 4** — Versionar prompt:

```bash
git add docs/prompts/prompt-etapa-4-15.md
git status   # apenas prompt-etapa-4-15.md staged
git commit -m "docs: versiona prompt-etapa-4-15.md em docs/prompts/"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Atencao:** hook 4.4 NAO deve alertar (prompt em `docs/prompts/`, isento desde 4.14).

**Validacao implicita dos hooks ativos:**

- Conventional Commits (4.1): mensagens validas.
- Encoding UTF-8 (4.2): valida bytes em todos os arquivos editados.
- Markdown blank lines (4.3): valida headers em `.md` editados.
- Tamanho de docs (4.4): NAO deve alertar.
- Maven release (4.5), @Entity (4.7): nao se aplicam.

Se algum hook bloquear, parar e reportar.

### Tarefa 9 — Validacao final antes de push

```bash
git status
git log --oneline -5
git config core.hooksPath
```

Esperado:

- Working tree limpo.
- 4 commits novos.
- `core.hooksPath` = `.githooks`.

## Restricoes e freios

1. **NAO modificar `~/.claude/` global.** Sub-etapa estritamente documental. Sem desligar auto-memory, sem limpar transcripts, sem desinstalar plugins. Decisoes sobre setup pessoal do Claude Code ficam **fora do escopo do projeto**.

2. **NAO modificar `.claude/agents/`, `.claude/skills/`, `.claude/hooks/`, `.githooks/`.** Sub-etapa nao toca em componentes do projeto.

3. **NAO criar subagents, skills, hooks novos.**

4. **NAO atualizar `CLAUDE.md`.** Regra 4.6: errata documental nao muda convencao do projeto.

5. **NAO atualizar `docs/adrs.md`.** Sem ADR novo. Sem errata em ADR (ADR-012 ja teve sua errata na 4.11; ADRs 1-11 nao sao afetados pela 4.15).

6. **NAO atualizar `docs/visao.md`, `docs/blueprint-fabrica-ai-native.md`, `docs/progresso-historico.md`.**

7. **Preservar registro historico da 4.10 em `decisoes.md` integralmente.** Apenas adicionar nota de errata logo apos o titulo. NAO editar, reescrever, ou remover conteudo original da subsecao 4.10. Padrao identico ao da ADR-012 / errata 4.11.

8. **Linha de errata em `decisoes.md` usa formato blockquote** (`>`). Visualmente distinto do conteudo original — sinaliza que e adicao posterior.

9. **Encoding UTF-8 sem BOM** em todos os arquivos editados.

10. **Apenas ASCII em mensagens de commit.** Sem acentos, sem em-dash U+2014.

11. **Ordem cronologica descrescente** em "Sub-etapas concluidas", "Licoes", "Historico" em `progresso.md`.

12. **Sem cenarios destrutivos.** Sub-etapa documental — validacao via pre-condicoes ADR-011 em cada Tarefa basta.

13. **Hook 4.4 NAO deve alertar** em nenhum commit. Se alertar, investigar — `progresso.md` ainda esta em faixa segura.

14. **Nao sugerir proxima sub-etapa** espontaneamente alem dos candidatos ja citados (test-writer, /feature, outros eventuais).

15. **Nao tomar decisao silenciosa em zona limitrofe.** Reportar divergencias.

16. **Nao usar `pwsh`.** PowerShell 5.1.

17. **Nao usar `git reset --hard`.**

18. **Nao usar `git commit --no-verify`.**

19. **Nao executar comandos em `~/.claude/`** alem dos ja conduzidos na auditoria. Auditoria empirica foi encerrada — sub-etapa apenas registra o que foi descoberto. Comandos adicionais de inspecao em `~/.claude/` estouram escopo.

## Estrutura de commits

Branch: `docs/etapa-4-15-errata-auditoria-4-10`

**Commit 1** — `docs: errata da 4.10 + subsecao 4.15 (re-classificacao dos debitos meta-operacionais)`

- `docs/decisoes.md` (nota de errata na subsecao da 4.10 + subsecao 4.15 antes de "Claude Code hooks nativos")

**Commit 2** — `docs(hooks-pendentes): re-classifica debitos 4.10 + adiciona transcripts (4.15)`

- `docs/hooks-pendentes.md` (plugins removido + memoria reescrita + built-ins reescrito + transcripts adicionado)

**Commit 3** — `docs: sub-etapa 4.15 -- registra errata de auditoria meta-operacional`

- `docs/progresso.md` (sub-etapa 4.15 + licoes + historico)

**Commit 4** — `docs: versiona prompt-etapa-4-15.md em docs/prompts/`

- `docs/prompts/prompt-etapa-4-15.md` (novo)

## Validacao antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -5
git config core.hooksPath
```

Esperado:

- `check.ps1` passa.
- Working tree limpo.
- 4 commits novos.
- `core.hooksPath` = `.githooks`.

## PR

Titulo: `docs: sub-etapa 4.15 -- errata de auditoria meta-operacional (re-classificacao dos debitos 4.10)`

Body sugerido:

````markdown
## Summary

Sub-etapa **documental pura**. Auditoria empirica de `~/.claude/` revelou que 2 dos 3 debitos meta-operacionais registrados pela 4.10 estavam baseados em premissas equivocadas. Esta sub-etapa corrige o registro sem alterar o conteudo original da 4.10. Categoria nova: **"errata de auditoria meta-operacional"**.

### Por que esta sub-etapa existe

A 4.15 foi calibrada inicialmente como **mitigacao dos debitos meta-operacionais** registrados pela 4.10. Antes de definir o escopo de mitigacao, foi conduzida auditoria empirica de `~/.claude/` via comandos PowerShell. Resultados revelaram disparidade significativa entre o suposto e o real:

- **Memoria global:** a 4.10 registrou como debito sem dimensionar. Auditoria mostrou ~85 KB de memoria derivada real (pasta `memory/` aparece apenas em UM projeto). Os ~427 MB que `~/.claude/projects/` ocupa sao **transcripts de conversa** (logs brutos das sessoes), nao memoria.

- **Plugins:** a 4.10 registrou `code-review` e `frontend-design` como debito assumindo serem instalacoes manuais. Auditoria mostrou que ficam em `~/.claude/plugins/cache/claude-plugins-official/` — plugins oficiais cacheados pelo marketplace. Equivalentes a built-ins.

- **Built-ins:** debito teorico. Operador nao observou em sessoes recentes (maio/2026) delegacao para built-ins nem interferencia com subagents custom.

A escolha consciente apos a auditoria foi reorientar a 4.15 para **errata documental** ao inves de mitigacao baseada em premissa errada.

### Categoria nova: "errata de auditoria meta-operacional"

Distinta de:

- **"Auditoria meta-operacional"** (4.10): identifica debitos baseado em observacao inicial.
- **"Errata de ADR baseada em descoberta de documentacao oficial"** (4.11): corrige decisao estrutural via doc oficial.
- **Esta categoria:** corrige auditoria anterior baseada em **verificacao empirica posterior**. Aplicavel a casos onde auditoria inicial tinha premissa errada — categoriza fenomeno mal, dimensiona errado, ou inclui itens fora do escopo real.

### Re-classificacao dos 3 debitos da 4.10

1. **Memoria global auto-ON** -> reescrito em `hooks-pendentes.md` com dimensionamento real (~85 KB) e nota sobre confusao com transcripts.
2. **Plugins globais** -> **removido do backlog** (plugins oficiais cacheados, nao instalacoes manuais).
3. **Built-ins competindo** -> reescrito como "sob observacao, sem dor pratica relatada".

### Achado novo (nao registrado pela 4.10)

**Transcripts em `~/.claude/projects/<hash>/<conversa-hash>/` ocupam ~427 MB em 17 projetos.** Sem rotina de expiracao. Maior conversa unica: 90 MB. Registrado em `hooks-pendentes.md` para visibilidade. **Fora do escopo do projeto** — gestao de storage do Claude Code e decisao pessoal do operador.

### Padrao "auditar antes de mitigar" demonstrado em segunda aplicacao

Aplicado em 4.13 (dimensionar `progresso.md` antes de cortar — 891 linhas reais, corte por Camada concluida). Aplicado de novo em 4.15 (auditar `~/.claude/` antes de mitigar — descobertas que reorientaram a sub-etapa). Em ambos os casos, auditoria empirica revelou informacao que ajustou o escopo.

**Recomendacao operacional consolidada por dupla aplicacao:** antes de mitigar territorio opaco, auditar primeiro. Custo baixo (3-5 comandos de inspecao); risco de pular alto (mitigacao baseada em premissa errada).

### Padrao "nota de errata + subsecao nova" replicado

Identico ao usado na 4.11 para ADR-012:

- Registro original da 4.10 em `decisoes.md` preservado integralmente.
- Nota de errata em blockquote logo apos o titulo da subsecao, apontando para 4.15.
- Subsecao 4.15 nova explicando o que mudou.

Auditoria registra o que sabiamos no momento; errata corrige o que descobrimos depois.

### Mudancas

- `docs/decisoes.md`: nota de errata na subsecao da 4.10 + subsecao "Errata de auditoria meta-operacional (Sub-etapa 4.15)" antes de "Claude Code hooks nativos".
- `docs/hooks-pendentes.md`: item de plugins removido; itens de memoria e built-ins reescritos; item novo de transcripts adicionado.
- `docs/progresso.md`: sub-etapa 4.15 + 5 licoes + historico.
- `docs/prompts/prompt-etapa-4-15.md`: prompt versionado.

### Sem mudanca em `~/.claude/` global

Sub-etapa estritamente documental. Decisoes sobre desligar auto-memory, limpar transcripts, ou desinstalar plugins ficam **fora do escopo** — sao decisoes pessoais do operador sobre seu setup do Claude Code, nao do `financas-lab`.

### CLAUDE.md NAO atualizado

Regra 4.6: errata documental nao muda convencao do projeto.

### Proximo passo

Decisao fora deste PR. Candidatos naturais:

- **4.16** — `test-writer` + skill `/write-test` (terceiro par skill+subagent — territorio novo: subagent que **gera codigo**, nao revisa).
- **4.16 alternativo** — Skill sem subagent `/feature <nome>` (eixo novo: skill geradora pura).
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

- Branch `docs/etapa-4-15-errata-auditoria-4-10` empurrada com 5 commits (4 + 1 update do PR).
- PR aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.14.
- Working tree limpo.
- `docs/decisoes.md` com nota de errata na 4.10 + subsecao 4.15.
- `docs/hooks-pendentes.md` com debitos meta-operacionais re-classificados.
- `docs/progresso.md` com sub-etapa 4.15 + licoes + historico.
- Sem mudanca em `~/.claude/` global.
- Reportar: `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup`.

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao modificar `~/.claude/` global em momento algum.
- Nao executar comandos adicionais de inspecao em `~/.claude/` alem dos ja conduzidos.
- Nao modificar componentes do projeto (`.claude/agents/`, `.claude/skills/`, `.claude/hooks/`, hooks, código, frontend).
- Nao atualizar `CLAUDE.md`, `adrs.md`, `visao.md`, `blueprint-fabrica-ai-native.md`, `progresso-historico.md`.
- Nao criar prompt da 4.16.
- Nao sugerir proximo passo espontaneamente alem dos candidatos ja citados no PR body.
- Nao usar `pwsh`.
- Nao usar `git reset --hard`.
- Nao usar `git commit --no-verify`.
