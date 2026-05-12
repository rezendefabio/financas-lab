# Prompt — Etapa 4.14: Hook 4.4 exclui `docs/prompts/` da verificacao de tamanho

## Contexto

Camada 3 com 6 hooks + CLAUDE.md + blueprint + 2 subagents (`pr-reviewer` Haiku + `architect-reviewer` Sonnet) + 2 skills orquestradoras (`/review-pr` + `/review-arch`) + ADR-012 (revisao 4.11) + `progresso-historico.md` (4.13) apos a 4.13. Padrao skill+subagent validado em 2 casos (PRs #55, #45, #35).

A 4.13 (manutencao de docs por crescimento) reorganizou `docs/prompt-etapa-*.md` para `docs/prompts/` via `git mv` (43 arquivos). Efeito colateral observado durante o commit da propria 4.13: hook 4.4 (`docs-size.ps1`, modo warn) alertou no `prompt-etapa-4-13.md` (1018 linhas). Investigacao confirmou que o hook varre subpastas de `docs/` — alerta tambem dispara para prompts antigos longos (4.11 ~870 linhas, 4.12 953 linhas).

**Comportamento atual do hook 4.4:** atua sobre qualquer `.md` em `docs/` (e subpastas) que aparece no diff staged. Alerta (modo `warn`) se arquivo cruza 800 linhas. Nao bloqueia commit.

**Problema:** prompts versionados em `docs/prompts/` sao **registros historicos por natureza**. Tamanho nao e criterio de qualidade — prompts longos sao consequencia natural de sub-etapas complexas. Alerta repetido em prompts antigos polui o output do commit e degrada sinal/ruido do hook.

**Debito registrado no PR body da 4.13:** "Excluir `docs/prompts/` da verificacao do hook 4.4 em sub-etapa futura." Esta sub-etapa resolve.

Caracteristicas:

1. **Sub-etapa de refactor de hook existente.** Branch prefixo `refactor/`. Primeira sub-etapa no projeto que **modifica hook existente** (4.1-4.7 entregaram hooks novos; 4.0.1 corrigiu bug em script de setup).

2. **Categoria nova: "ajuste de hook por contexto novo".** Distinta de:
   - "Patch tecnico" (4.0.1): corrige bug do entregue.
   - "Refinamento pos-smoke empirico" (4.9.1): componente funcional mas output divergente.
   - "Errata de ADR baseada em descoberta de documentacao oficial" (4.11): decisao estrutural preservada, mecanismo literal refinado.
   - **Esta categoria:** hook cumpre regra original corretamente, mas contexto novo (subpasta criada em sub-etapa anterior — aqui `docs/prompts/` na 4.13) muda o que a regra deveria fazer. Refactor ajusta hook ao novo contexto sem mudar a intencao original.

3. **Mudanca cirurgica.** Apenas adiciona exclusao de `docs/prompts/` no filtro de path do `docs-size.ps1`. Comportamento original (alerta para `.md` em `docs/` >800 linhas) preservado para `docs/` raiz e qualquer subpasta futura que nao seja `docs/prompts/`.

4. **Outros hooks que tocam `.md` permanecem inalterados.** Hook 4.2 (encoding UTF-8) e 4.3 (Markdown blank lines) continuam atuando em `docs/prompts/`. Justificativa: encoding e blank lines sao convencoes de qualidade que se aplicam a qualquer `.md` no repo, incluindo prompts.

5. **Validacao destrutiva sob ADR-011 obrigatoria.** Mudanca de hook exige cenarios destrutivos cobrindo comportamento antigo preservado + comportamento novo introduzido. 6 cenarios prescritos.

Quando esta etapa terminar:

- `.claude/hooks/universal/docs-size.ps1`: filtro de path atualizado para excluir `docs/prompts/`.
- `docs/decisoes.md`: subsecao 4.14 antes de "Claude Code hooks nativos".
- `docs/progresso.md`: sub-etapa 4.14 + licoes + historico.
- `docs/hooks-pendentes.md`: debito da 4.13 removido + linha de "Hooks implementados" do 4.4 atualizada com referencia a 4.14.
- `docs/prompts/prompt-etapa-4-14.md`: versionado.

## Padroes que estreiam nesta etapa

1. **Categoria nova: "ajuste de hook por contexto novo".** Hook entregue cumpre regra original; contexto subsequente (subpasta criada por sub-etapa posterior) introduz caso onde a regra original gera falso positivo. Refactor ajusta hook sem reverter intencao. Distinta de patch tecnico, refinamento pos-smoke, e errata documental.

2. **Primeira modificacao de hook existente no projeto.** Hooks 4.1-4.7 entregaram componentes novos; esta sub-etapa **refina um existente**. Estabelece padrao operacional: hooks podem evoluir conforme contexto do projeto evolui, sem perder cobertura original. Branch `refactor/` formaliza a categoria.

3. **Debito originario de sub-etapa anterior resolvido em sub-etapa subsequente.** Padrao operacional novo: PR body da 4.13 registrou debito explicito ("excluir `docs/prompts/`"); 4.14 resolve. Cadeia "sub-etapa X cria contexto -> X+N resolve debito de contexto". Padrao a aplicar em outros debitos meta-operacionais (4.10).

## Escopo decidido (calibrado com operador antes da redacao via D1-D5)

### Arquivos modificados

| Arquivo | Tipo de mudanca |
|---|---|
| `.claude/hooks/universal/docs-size.ps1` | Filtro de path atualizado (exclui `docs/prompts/`) |
| `docs/decisoes.md` | Subsecao 4.14 antes de "Claude Code hooks nativos" |
| `docs/progresso.md` | Sub-etapa 4.14 + licoes + historico |
| `docs/hooks-pendentes.md` | Debito 4.13 removido + linha do hook 4.4 em "Hooks implementados" atualizada |
| `docs/prompts/prompt-etapa-4-14.md` | Versionado (novo) |

**Nao tocados:** `.claude/agents/`, `.claude/skills/`, outros hooks (`.claude/hooks/universal/encoding-utf8.ps1`, `markdown-blank-lines.ps1`, `conventional-commits.ps1`, `.claude/hooks/java-spring/`), `.githooks/`, `pom.xml`, `src/`, `frontend/`, `CLAUDE.md` (D5: nao atualizar), `docs/adrs.md`, `docs/visao.md`, `docs/blueprint-fabrica-ai-native.md`, `docs/progresso-historico.md`, `.gitignore`, `.gitattributes`.

### Mudanca em `.claude/hooks/universal/docs-size.ps1`

**Intencao da mudanca:** adicionar exclusao de `docs/prompts/` ao filtro de path que seleciona quais arquivos do diff staged sao verificados.

**Operacionalizacao:** o hook atual filtra `.md` em `docs/` usando alguma forma de regex/match (provavel `Where-Object { $_ -match '^docs/.+\.md$' }` ou equivalente). A mudanca adiciona condicao **AND NOT** para excluir `docs/prompts/`.

**Padrao esperado da modificacao** (sintaxe pode variar conforme codigo atual):

```powershell
# Antes (exemplo -- sintaxe exata depende do codigo atual):
$stagedDocs = git diff --cached --name-only | Where-Object { $_ -match '^docs/.+\.md$' }

# Depois:
$stagedDocs = git diff --cached --name-only | Where-Object {
    $_ -match '^docs/.+\.md$' -and $_ -notmatch '^docs/prompts/'
}
```

**Tarefa do executor:** ler o `docs-size.ps1` atual, identificar o ponto onde o filtro de path acontece, e aplicar exclusao no padrao acima. **A intencao e mais importante que a sintaxe exata** — se o hook usa outra estrutura (ex: regex em variavel separada, multiplos `Where-Object`), adaptar mantendo a intencao: **arquivos em `docs/prompts/` nao devem ser verificados pelo hook 4.4**.

**Mensagens de erro/warn do hook:** nao precisam mudar. O hook continua emitindo o mesmo warn para arquivos que estouram limite — apenas o **conjunto** de arquivos verificados diminui.

**Restricoes da mudanca:**

- **Encoding UTF-8 sem BOM** preservado.
- **Apenas ASCII** em strings de mensagens (licao 4.4 sobre em-dash U+2014 quebrando parse PS5.1).
- **Sem mudanca de modo** (continua `warn`, nao `fail`).
- **Sem mudanca de limite** (continua 800 linhas).
- **Sem mudanca de extensao alvo** (continua `.md`).

### Conteudo da subsecao em `docs/decisoes.md`

Inserir **antes** da subsecao `### Claude Code hooks nativos`, **apos** a subsecao "Manutencao de docs por crescimento (Sub-etapa 4.13)":

```markdown
### Hook 4.4 exclui `docs/prompts/` (Sub-etapa 4.14)

Sub-etapa de refactor de hook existente — primeira do projeto. Categoria nova: **"ajuste de hook por contexto novo"**. Hook `docs-size.ps1` (4.4, modo warn) modificado para ignorar `.md` em `docs/prompts/`.

**Por que:** a 4.13 (manutencao de docs por crescimento) criou `docs/prompts/` movendo 43 prompts versionados para la via `git mv`. Hook 4.4 passou a alertar em prompts antigos longos (4.11 ~870 linhas, 4.12 953, 4.13 1018) — falso positivo. Prompts versionados sao **registros historicos por natureza**; tamanho nao e criterio de qualidade.

**Mudanca cirurgica:** filtro de path do hook ganha exclusao `docs/prompts/`. Comportamento original (alerta para `.md` em `docs/` >800 linhas) preservado para `docs/` raiz e qualquer subpasta futura que nao seja `docs/prompts/`. Modo `warn` mantido, limite 800 mantido, extensao `.md` mantida.

**Outros hooks que tocam `.md` permanecem inalterados:**

- **Hook 4.2** (encoding UTF-8): continua atuando em `docs/prompts/`. Encoding e convencao de qualidade que vale para qualquer `.md` no repo.
- **Hook 4.3** (Markdown blank lines): continua atuando em `docs/prompts/`. Mesma justificativa.

Hook 4.4 isenta `docs/prompts/` **porque tamanho e fenomeno emergente** de sub-etapas complexas, nao desvio de qualidade.

**Categoria operacional nova: "ajuste de hook por contexto novo".** Hook cumpre regra original; sub-etapa posterior (aqui 4.13) cria contexto onde regra original gera falso positivo. Refactor ajusta hook ao contexto sem reverter intencao. Distinta de:

- **"Patch tecnico"** (4.0.1): corrige bug do entregue.
- **"Refinamento pos-smoke empirico"** (4.9.1): componente funcional mas output divergente.
- **"Errata de ADR baseada em descoberta de documentacao oficial"** (4.11): decisao estrutural preservada, mecanismo literal refinado.

Aplicavel a futuros casos onde subpasta criada por sub-etapa posterior introduz contexto que invalida regra de hook existente.

**Validacao destrutiva sob ADR-011:** 6 cenarios cobrindo comportamento original preservado + comportamento novo introduzido. Detalhes no PR body da 4.14.

**Debito da 4.13 resolvido:** item "modificar hook 4.4 para excluir docs/prompts/" removido de `hooks-pendentes.md`. Padrao operacional: debito originario de sub-etapa anterior resolvido em sub-etapa subsequente — cadeia "X cria contexto -> X+N resolve debito de contexto".
```

### Atualizacoes em `docs/hooks-pendentes.md`

**Mudanca 1 — Remover o item do debito** da secao "Debitos meta-operacionais" criada na 4.10.

Texto a remover (linha aproximada — confirmar via Tarefa 4):

```markdown
- **Excluir `docs/prompts/` da verificacao do hook 4.4.** (Descoberto na 4.13.) Hook 4.4 (`docs-size.ps1`, modo warn) alerta em `.md` de `docs/prompts/` que cruzam 800 linhas (4.11 ~870, 4.12 953, 4.13 1018). Prompts versionados sao registros historicos — tamanho nao e criterio de qualidade. Resolver em sub-etapa pequena modificando o filtro de path do hook.
```

(Se o texto exato diferir, identificar o item por palavras-chave "docs/prompts" + "hook 4.4" e remover o item inteiro — bullet completo de uma so vez.)

**Mudanca 2 — Atualizar a linha do hook 4.4 em "Hooks implementados"** para registrar a modificacao da 4.14.

Texto atual (linha aproximada):

```markdown
- **Tamanho de docs em `docs/` (modo warn)** (Sub-etapa 4.4, PR #44). Implementado em `.claude/hooks/universal/docs-size.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Limite: 800 linhas totais. Alerta visual em amarelo, **nao bloqueia commit**. Apenas `.md` em `docs/` — outros `.md` ignorados. Modo `warn` registrado como padrao para regras subjetivas em `decisoes.md`.
```

Texto novo (acrescenta nota sobre 4.14):

```markdown
- **Tamanho de docs em `docs/` (modo warn)** (Sub-etapa 4.4, PR #44; refinado pela 4.14 em PR #XX). Implementado em `.claude/hooks/universal/docs-size.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Limite: 800 linhas totais. Alerta visual em amarelo, **nao bloqueia commit**. Apenas `.md` em `docs/` — outros `.md` ignorados. **A partir da 4.14:** `docs/prompts/` excluido da verificacao (prompts versionados sao registros historicos por natureza; tamanho nao e criterio de qualidade). Modo `warn` registrado como padrao para regras subjetivas em `decisoes.md`.
```

### Conteudo das edicoes em `docs/progresso.md`

**Edicao 1 — Sub-etapa 4.14 no topo de "Sub-etapas concluidas" da Camada 3** (acima da 4.13):

```markdown
- **4.14 — Hook 4.4 exclui `docs/prompts/` da verificacao de tamanho** (2026-05-11): sub-etapa de refactor de hook existente — primeira do projeto. Categoria nova: **"ajuste de hook por contexto novo"**. `.claude/hooks/universal/docs-size.ps1` modificado para ignorar `.md` em `docs/prompts/`. Comportamento original (alerta para `.md` em `docs/` >800 linhas) preservado para `docs/` raiz e subpastas futuras que nao sejam `docs/prompts/`. Modo `warn` mantido, limite 800 mantido. Outros hooks que tocam `.md` (encoding 4.2, blank lines 4.3) permanecem ativos em `docs/prompts/`. Resolve debito explicito registrado no PR body da 4.13 ("excluir `docs/prompts/` da verificacao do hook 4.4"). Validacao destrutiva sob ADR-011: 6 cenarios cobrindo comportamento original preservado + comportamento novo. CLAUDE.md NAO atualizado (regra 4.6: refactor de hook nao muda convencao). PR #XX.
```

**Edicao 2 — Bloco "Licoes da Sub-etapa 4.14"** acima de "Licoes da Sub-etapa 4.13":

```markdown
## Licoes da Sub-etapa 4.14

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — refactor de hook existente.)

### Licoes de ambiente

1. **Categoria nova: "ajuste de hook por contexto novo".** Distinta de "patch tecnico" (4.0.1), "refinamento pos-smoke empirico" (4.9.1), "errata de ADR baseada em doc oficial" (4.11). Hook cumpre regra original; sub-etapa posterior cria contexto onde regra gera falso positivo. Refactor ajusta hook sem reverter intencao. Padrao replicavel para futuros casos analogos: subpasta criada em sub-etapa N gera falso positivo de hook X; sub-etapa N+M ajusta hook X com exclusao especifica.

2. **Primeira modificacao de hook existente no projeto.** Hooks 4.1-4.7 entregaram componentes novos; 4.14 e o primeiro **refactor de hook**. Branch `refactor/` aplicada conforme padrao consolidado (4.9.1). Padrao operacional: hooks podem evoluir conforme contexto do projeto evolui, sem perder cobertura original. Outras dimensoes que podem motivar refactor de hook no futuro: limite numerico (limite 800 pode ser ajustado), modo (warn -> fail ou vice-versa), regex (cobrir mais ou menos casos).

3. **Debito originario de sub-etapa anterior resolvido em sub-etapa subsequente.** PR body da 4.13 registrou debito explicito; 4.14 resolveu. Cadeia "sub-etapa X cria contexto -> X+N resolve debito de contexto" formalizada como padrao operacional. Aplicavel a outros debitos meta-operacionais (4.10): memoria global, plugins, built-in agents. Cada um pode virar sub-etapa dedicada de mitigacao quando aparecer dor real.

4. **Validacao destrutiva em refactor de hook exige cobertura dupla.** Cenarios devem cobrir (a) comportamento original preservado (hook ainda alerta no caso que deveria alertar) e (b) comportamento novo introduzido (hook nao alerta mais no caso isento). Sem (a), refactor pode quebrar cobertura sem detectar. Sem (b), mudanca nao foi exercitada. Padrao a aplicar em refactors futuros de hook.
```

**Edicao 3 — Linha no historico** acima da entrada da 4.13:

```markdown
- **2026-05-11** — Sub-etapa 4.14 concluida (refactor): hook 4.4 (`docs-size.ps1`) modificado para excluir `docs/prompts/` da verificacao de tamanho. Resolve debito registrado no PR body da 4.13. Categoria nova: "ajuste de hook por contexto novo". Primeira modificacao de hook existente no projeto. 6 cenarios destrutivos sob ADR-011. CLAUDE.md NAO atualizado. PR #XX.
```

### Versionar este proprio prompt

`docs/prompts/prompt-etapa-4-14.md` entra como novo arquivo no Commit 3. Padrao consolidado (desde 4.13, prompts em `docs/prompts/`).

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra squash da 4.13.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompts/prompt-etapa-4-14.md` presente como **untracked** (anexado pelo operador).
- Working tree limpo (exceto o prompt).
- `.claude/hooks/universal/docs-size.ps1` existe (entregue na 4.4, ~30-50 linhas estimadas).
- `docs/hooks-pendentes.md` tem secao "Debitos meta-operacionais" (criada na 4.10, expandida pela 4.13) com item "Excluir `docs/prompts/` da verificacao do hook 4.4".

Validar com:

```powershell
git status
git log --oneline -3
git config core.hooksPath
Test-Path .claude\hooks\universal\docs-size.ps1
Test-Path docs\prompts\prompt-etapa-4-14.md
```

**Pre-condicoes ADR-011:**

- Working tree limpo exceto o prompt.
- `docs-size.ps1` existe.
- Prompt anexado em `docs/prompts/` (padrao novo desde 4.13).

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
git checkout -b refactor/etapa-4-14-hook-docs-size-exclui-prompts
```

Prefixo `refactor/` — refactor de hook existente (analogo a 4.9.1 que refinou subagent).

### Tarefa 4 — Antes de editar, ler arquivos vivos

```bash
cat .claude/hooks/universal/docs-size.ps1
cat docs/decisoes.md
cat docs/progresso.md
cat docs/hooks-pendentes.md
```

**Confirmar:**

- `docs-size.ps1`: identificar o ponto onde o filtro de path acontece. Provavel padrao: `Where-Object { $_ -match '^docs/.+\.md$' }` ou `Where-Object { $_ -like 'docs/*.md' }` ou equivalente. **Anotar a sintaxe exata** para aplicar modificacao na Tarefa 5.
- `decisoes.md`: tem subsecao "Manutencao de docs por crescimento (Sub-etapa 4.13)" antes de "Claude Code hooks nativos". Nova subsecao 4.14 entra **entre** essas duas.
- `progresso.md`: tem "Sub-etapas concluidas" da Camada 3 em ordem descrescente comecando em 4.13. Sub-etapa 4.14 entra **acima** da 4.13.
- `progresso.md`: tem "Licoes da Sub-etapa 4.13" — "Licoes da Sub-etapa 4.14" entra **acima**.
- `progresso.md`: tem entrada de historico da 4.13 — linha da 4.14 entra **acima**.
- `hooks-pendentes.md`: tem secao "Debitos meta-operacionais" com item sobre "Excluir `docs/prompts/` da verificacao do hook 4.4". **Anotar texto exato** para remocao precisa na Tarefa 7.
- `hooks-pendentes.md`: tem secao "Hooks implementados" com linha sobre hook 4.4 (tamanho de docs). **Anotar texto exato** para atualizacao precisa na Tarefa 7.

Se alguma divergencia, **parar e reportar**.

### Tarefa 5 — Modificar `.claude/hooks/universal/docs-size.ps1`

**Objetivo:** adicionar exclusao de `docs/prompts/` ao filtro de path.

**Acao:** identificar o `Where-Object` (ou equivalente) que seleciona `.md` em `docs/`, e adicionar condicao **AND NOT** para excluir `docs/prompts/`.

**Padrao esperado** (sintaxe exata depende do codigo atual lido na Tarefa 4):

```powershell
# Padrao tipico:
| Where-Object { $_ -match '^docs/.+\.md$' -and $_ -notmatch '^docs/prompts/' }
```

Adaptar a sintaxe ao codigo existente — **manter a intencao**: arquivos em `docs/prompts/` nao sao verificados.

**Restricoes:**

- Encoding UTF-8 sem BOM preservado.
- Apenas ASCII em mensagens (licao 4.4 sobre em-dash U+2014).
- Sem mudanca de modo (continua `warn`).
- Sem mudanca de limite (continua 800).
- Sem mudanca de extensao alvo (continua `.md`).

**Pre-condicao ADR-011 apos modificar:**

```powershell
Test-Path .claude\hooks\universal\docs-size.ps1   # True

$bytes = [System.IO.File]::ReadAllBytes(".claude/hooks/universal/docs-size.ps1")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191 (sem BOM)

$content = [System.IO.File]::ReadAllText(".claude/hooks/universal/docs-size.ps1", [System.Text.UTF8Encoding]::new($false))

# Confirmar exclusao de docs/prompts/ presente
if ($content -match 'docs/prompts/') {
    Write-Host "Exclusao docs/prompts OK"
} else {
    Write-Host "Exclusao docs/prompts AUSENTE"
}

# Confirmar filtro original ainda presente (algum match com docs/.+\.md)
if ($content -match 'docs/.+\\\.md|docs/.\*\\\.md|docs/\*\.md') {
    Write-Host "Filtro original preservado OK"
} else {
    Write-Host "ATENCAO: filtro original pode ter sido alterado -- verificar manualmente"
}

# Confirmar modo warn preservado
if ($content -match 'warn|WARN|Warn') {
    Write-Host "Modo warn OK"
} else {
    Write-Host "ATENCAO: modo warn ausente -- verificar"
}

# Limite 800 preservado
if ($content -match '800') {
    Write-Host "Limite 800 OK"
} else {
    Write-Host "ATENCAO: limite 800 ausente -- verificar"
}

# Linhas totais (provavel aumento de 1-3 linhas)
$linhas = (Get-Content .claude\hooks\universal\docs-size.ps1).Count
Write-Host "Linhas totais: $linhas"
```

Se algum valor divergir, parar e reportar.

### Tarefa 6 — Validacao destrutiva sob ADR-011 (6 cenarios)

**Estrutura de cada cenario:**

1. Criar arquivo de teste.
2. `Test-Path` confirma criacao no diretorio correto.
3. `git add` do arquivo de teste.
4. `git status` confirma arquivo staged.
5. Tentar `git commit` (mensagem de teste). Hook dispara.
6. Verificar comportamento (alerta vs silencio).
7. `git reset HEAD <arquivo>` + `Remove-Item <arquivo>` para limpar.

**Cenario 1 — `.md` em `docs/` raiz com >800 linhas (deve alertar).**

Cria `docs/teste-tamanho-grande.md` com 900 linhas de conteudo dummy.

```powershell
# Criar arquivo de teste
$conteudo = ((1..900) | ForEach-Object { "Linha $_" }) -join "`n"
[System.IO.File]::WriteAllText("$PWD\docs\teste-tamanho-grande.md", $conteudo, [System.Text.UTF8Encoding]::new($false))

Test-Path docs\teste-tamanho-grande.md
(Get-Content docs\teste-tamanho-grande.md).Count   # Esperado: 900

git add docs/teste-tamanho-grande.md
git status   # Confirmar staged

# Commit (mensagem de teste)
git commit -m "test: cenario 1 hook 4.14 -- alerta esperado"
# **Esperado:** commit completa, MAS hook emite warn no terminal sobre arquivo cruzar 800 linhas.

# Cleanup
git reset HEAD~1 --soft   # Desfaz commit mantendo staged
git reset HEAD docs/teste-tamanho-grande.md   # Unstage
Remove-Item docs\teste-tamanho-grande.md
```

**Cenario 2 — `.md` em `docs/` raiz com <800 linhas (nao deve alertar).**

Repete estrutura com 500 linhas. Esperado: commit completa, **sem warn no terminal**.

```powershell
$conteudo = ((1..500) | ForEach-Object { "Linha $_" }) -join "`n"
[System.IO.File]::WriteAllText("$PWD\docs\teste-tamanho-pequeno.md", $conteudo, [System.Text.UTF8Encoding]::new($false))

Test-Path docs\teste-tamanho-pequeno.md
(Get-Content docs\teste-tamanho-pequeno.md).Count

git add docs/teste-tamanho-pequeno.md
git status

git commit -m "test: cenario 2 hook 4.14 -- sem alerta esperado"

# Cleanup
git reset HEAD~1 --soft
git reset HEAD docs/teste-tamanho-pequeno.md
Remove-Item docs\teste-tamanho-pequeno.md
```

**Cenario 3 — `.md` em `docs/prompts/` com >800 linhas (NAO deve alertar — comportamento novo).**

Esperado: commit completa, **sem warn no terminal** (hook ignora `docs/prompts/`).

```powershell
$conteudo = ((1..900) | ForEach-Object { "Linha $_" }) -join "`n"
[System.IO.File]::WriteAllText("$PWD\docs\prompts\teste-prompt-grande.md", $conteudo, [System.Text.UTF8Encoding]::new($false))

Test-Path docs\prompts\teste-prompt-grande.md
(Get-Content docs\prompts\teste-prompt-grande.md).Count

git add docs/prompts/teste-prompt-grande.md
git status

git commit -m "test: cenario 3 hook 4.14 -- prompts grandes nao alertam"

# Cleanup
git reset HEAD~1 --soft
git reset HEAD docs/prompts/teste-prompt-grande.md
Remove-Item docs\prompts\teste-prompt-grande.md
```

**Cenario 4 — `.md` em `docs/prompts/` com <800 linhas (caso happy, nao deve alertar).**

```powershell
$conteudo = ((1..500) | ForEach-Object { "Linha $_" }) -join "`n"
[System.IO.File]::WriteAllText("$PWD\docs\prompts\teste-prompt-pequeno.md", $conteudo, [System.Text.UTF8Encoding]::new($false))

Test-Path docs\prompts\teste-prompt-pequeno.md
(Get-Content docs\prompts\teste-prompt-pequeno.md).Count

git add docs/prompts/teste-prompt-pequeno.md
git status

git commit -m "test: cenario 4 hook 4.14 -- prompts pequenos nao alertam"

# Cleanup
git reset HEAD~1 --soft
git reset HEAD docs/prompts/teste-prompt-pequeno.md
Remove-Item docs\prompts\teste-prompt-pequeno.md
```

**Cenario 5 — `.md` em outra subpasta de `docs/` (nao `prompts/`) com >800 linhas (deve alertar — regra "prompts sao excecao, nao todas subpastas").**

```powershell
# Criar subpasta de teste
New-Item -ItemType Directory -Path docs\teste-subpasta -Force

$conteudo = ((1..900) | ForEach-Object { "Linha $_" }) -join "`n"
[System.IO.File]::WriteAllText("$PWD\docs\teste-subpasta\teste.md", $conteudo, [System.Text.UTF8Encoding]::new($false))

Test-Path docs\teste-subpasta\teste.md
(Get-Content docs\teste-subpasta\teste.md).Count

git add docs/teste-subpasta/teste.md
git status

git commit -m "test: cenario 5 hook 4.14 -- subpasta nao-prompts alerta"
# **Esperado:** commit completa COM warn (hook age em subpastas que nao sao docs/prompts/).

# Cleanup
git reset HEAD~1 --soft
git reset HEAD docs/teste-subpasta/teste.md
Remove-Item docs\teste-subpasta\teste.md
Remove-Item docs\teste-subpasta
```

**Cenario 6 — `.md` fora de `docs/` (ex: na raiz do repo) com >800 linhas (nao deve alertar — comportamento original do hook 4.4).**

```powershell
$conteudo = ((1..900) | ForEach-Object { "Linha $_" }) -join "`n"
[System.IO.File]::WriteAllText("$PWD\teste-raiz.md", $conteudo, [System.Text.UTF8Encoding]::new($false))

Test-Path teste-raiz.md
(Get-Content teste-raiz.md).Count

git add teste-raiz.md
git status

git commit -m "test: cenario 6 hook 4.14 -- fora de docs nao alerta"
# **Esperado:** commit completa SEM warn (hook so age em docs/).

# Cleanup
git reset HEAD~1 --soft
git reset HEAD teste-raiz.md
Remove-Item teste-raiz.md
```

**Apos os 6 cenarios:** confirmar working tree limpo.

```powershell
git status   # Esperado: working tree clean (apenas o docs-size.ps1 modificado se ainda nao foi commitado)
```

**Anotar resultados de cada cenario** (alerta/silencio) para incluir no PR body.

**Atencao:** se algum cenario divergir do esperado:

- **Cenario 1 ou 5 sem alerta:** hook nao esta verificando arquivos que deveria. Mudanca da Tarefa 5 muito agressiva — investigar e corrigir.
- **Cenario 3 com alerta:** exclusao de `docs/prompts/` nao funcionou. Mudanca da Tarefa 5 insuficiente — investigar regex.
- **Cenario 2, 4 ou 6 com alerta:** hook esta alertando indevidamente. Investigar.

### Tarefa 7 — Atualizar `docs/hooks-pendentes.md`

**Mudanca 1:** remover item "Excluir `docs/prompts/` da verificacao do hook 4.4" da secao "Debitos meta-operacionais".

**Mudanca 2:** atualizar linha do hook 4.4 em "Hooks implementados" adicionando referencia a 4.14 (texto conforme escopo decidido).

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/hooks-pendentes.md", [System.Text.UTF8Encoding]::new($false))

# Debito removido
if ($content -match 'Excluir.{1,30}docs/prompts.{1,30}hook 4\.4') {
    Write-Host "ATENCAO: debito ainda presente -- verificar remocao"
} else {
    Write-Host "Debito removido OK"
}

# Linha 4.14 em Hooks implementados
if ($content -match 'refinado pela 4\.14') {
    Write-Host "Linha 4.4 atualizada OK"
} else {
    Write-Host "Linha 4.4 nao atualizada -- verificar"
}
```

### Tarefa 8 — Atualizar `docs/decisoes.md` (subsecao 4.14)

Copiar bloco "Conteudo da subsecao em decisoes.md" do escopo. Inserir **antes** de `### Claude Code hooks nativos`, **apos** "Manutencao de docs por crescimento (Sub-etapa 4.13)".

**Pre-condicao ADR-011:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/decisoes.md", [System.Text.UTF8Encoding]::new($false))

if ($content -match '### Hook 4\.4 exclui') {
    Write-Host "Subsecao 4.14 OK"
} else {
    Write-Host "Subsecao 4.14 AUSENTE"
}

# Ordem: 4.13 antes da 4.14 antes de hooks nativos
$pos413 = $content.IndexOf('Manutencao de docs por crescimento')
$pos414 = $content.IndexOf('Hook 4.4 exclui')
$posNativos = $content.IndexOf('Claude Code hooks nativos')
if ($pos413 -lt $pos414 -and $pos414 -lt $posNativos) {
    Write-Host "Ordem OK"
} else {
    Write-Host "Ordem TROCADA"
}
```

### Tarefa 9 — Atualizar `docs/progresso.md` (3 edicoes)

Aplicar **edicoes 1-3** descritas no escopo:

1. Sub-etapa 4.14 ao topo de "Sub-etapas concluidas" (acima da 4.13).
2. "Licoes da Sub-etapa 4.14" acima de "Licoes da Sub-etapa 4.13".
3. Linha de historico acima da entrada da 4.13.

**Pre-condicao ADR-011:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/progresso.md", [System.Text.UTF8Encoding]::new($false))

if ($content -match '4\.14.{1,10}Hook 4\.4 exclui') {
    Write-Host "Sub-etapa 4.14 OK"
} else {
    Write-Host "Sub-etapa 4.14 AUSENTE"
}

if ($content -match '## Li.{1,3}es da Sub-etapa 4\.14') {
    Write-Host "Licoes 4.14 OK"
} else {
    Write-Host "Licoes 4.14 AUSENTE"
}

# Ordem cronologica
$pos414 = $content.IndexOf('**4.14')
$pos413 = $content.IndexOf('**4.13')
if ($pos414 -gt 0 -and $pos414 -lt $pos413) {
    Write-Host "Ordem cronologica OK"
} else {
    Write-Host "Ordem cronologica TROCADA"
}

# Linhas totais (esperado: 440-450, ligeiro crescimento do 437 atual)
$linhas = (Get-Content docs\progresso.md).Count
Write-Host "Linhas totais: $linhas"
```

### Tarefa 10 — Commits (3 commits)

**Commit 1** — Hook modificado:

```bash
git add .claude/hooks/universal/docs-size.ps1
git status   # apenas docs-size.ps1 staged
git commit -m "refactor(hooks): docs-size exclui docs/prompts/ da verificacao (sub-etapa 4.14)"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 2** — Docs (decisoes + progresso + hooks-pendentes):

```bash
git add docs/decisoes.md docs/progresso.md docs/hooks-pendentes.md
git status   # 3 arquivos staged
git commit -m "docs: sub-etapa 4.14 -- registra ajuste de hook por contexto novo + resolve debito 4.13"
```

**Pre-condicao ADR-011:** 3 arquivos staged; `$LASTEXITCODE = 0`.

**Atencao:** hook 4.4 (apos modificacao) NAO deve alertar mais — `progresso.md` em ~440 linhas, abaixo do limite. Se alertar, investigar.

**Commit 3** — Versionar prompt:

```bash
git add docs/prompts/prompt-etapa-4-14.md
git status   # 1 arquivo staged
git commit -m "docs: versiona prompt-etapa-4-14.md em docs/prompts/"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Atencao:** hook 4.4 NAO deve alertar nesse commit — o proprio prompt-etapa-4-14.md esta em `docs/prompts/`, que agora e isento. **Este e o cenario 4 da validacao destrutiva acontecendo organicamente no proprio fluxo da sub-etapa.**

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

## Restricoes e freios

1. **NAO modificar outros hooks.** Apenas `.claude/hooks/universal/docs-size.ps1`. Hooks 4.1, 4.2, 4.3, 4.5, 4.7 permanecem intactos.

2. **NAO criar hooks novos.** Sub-etapa de refactor.

3. **NAO mudar modo do hook 4.4.** Continua `warn` (nao `fail`).

4. **NAO mudar limite do hook 4.4.** Continua 800 linhas.

5. **NAO mudar extensao alvo.** Continua `.md`.

6. **NAO mexer em `.claude/agents/`, `.claude/skills/`.**

7. **NAO mexer em `~/.claude/` global** (memoria global, plugins globais — debitos 4.10 nao mitigados nesta sub-etapa).

8. **NAO atualizar `CLAUDE.md`.** Regra 4.6: refactor de hook nao muda convencao do projeto (convencao "hooks decidem aplicabilidade internamente; modo warn vs fail" permanece).

9. **NAO atualizar `docs/adrs.md`.** Sem ADR novo.

10. **NAO atualizar `docs/progresso-historico.md`, `docs/visao.md`, `docs/blueprint-fabrica-ai-native.md`.**

11. **Cenarios destrutivos da Tarefa 6 sao obrigatorios.** 6 cenarios completos. Pular cenario quebra ADR-011.

12. **Cleanup completo apos cada cenario.** Working tree limpo entre cenarios. Sem arquivos de teste vazando para commit final.

13. **Cenarios usam `[System.IO.File]::WriteAllText` com path absoluto (`$PWD\...`) ou Environment.CurrentDirectory sincronizado** (licao 4.2.1 / ADR-011). Sem path relativo solto.

14. **Validacao destrutiva usa `git reset HEAD~1 --soft`** apos cada commit de teste (e nao `git reset --hard` — restricao 16).

15. **Encoding UTF-8 sem BOM** em todos os arquivos editados/criados.

16. **NAO usar `git reset --hard`.** `git reset HEAD~1 --soft` ou `git reset HEAD <arquivo>` para staging.

17. **Apenas ASCII** em strings de hook (licao 4.4 — em-dash U+2014 quebra parse PS5.1).

18. **Ordem cronologica descrescente** em "Sub-etapas concluidas", "Licoes", "Historico" em `progresso.md`.

19. **Hook 4.4 modificado NAO deve alertar** no Commit 3 (prompt em `docs/prompts/`). Se alertar, mudanca da Tarefa 5 nao foi aplicada corretamente — investigar.

20. **Nao sugerir proxima sub-etapa** espontaneamente alem dos candidatos ja citados (mitigar debitos meta-operacionais 4.10, `test-writer`, `/feature`).

21. **Nao tomar decisao silenciosa em zona limitrofe.** Reportar divergencias.

22. **Nao usar `pwsh`.** PowerShell 5.1.

23. **Nao usar `git commit --no-verify`.**

## Estrutura de commits

Branch: `refactor/etapa-4-14-hook-docs-size-exclui-prompts`

**Commit 1** — `refactor(hooks): docs-size exclui docs/prompts/ da verificacao (sub-etapa 4.14)`

- `.claude/hooks/universal/docs-size.ps1` (filtro de path modificado)

**Commit 2** — `docs: sub-etapa 4.14 -- registra ajuste de hook por contexto novo + resolve debito 4.13`

- `docs/decisoes.md` (subsecao 4.14)
- `docs/progresso.md` (sub-etapa + licoes + historico)
- `docs/hooks-pendentes.md` (debito removido + linha 4.4 atualizada)

**Commit 3** — `docs: versiona prompt-etapa-4-14.md em docs/prompts/`

- `docs/prompts/prompt-etapa-4-14.md` (novo)

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
- 3 commits novos.
- `core.hooksPath` = `.githooks`.

## PR

Titulo: `refactor: sub-etapa 4.14 -- hook 4.4 exclui docs/prompts/ da verificacao de tamanho`

Body sugerido:

````markdown
## Summary

Sub-etapa de refactor de hook existente — primeira do projeto. Hook `docs-size.ps1` (4.4, modo warn) modificado para ignorar `.md` em `docs/prompts/`. Categoria nova: **"ajuste de hook por contexto novo"**. Resolve debito explicito registrado no PR body da 4.13.

### Por que esta sub-etapa existe

A 4.13 (manutencao de docs por crescimento) criou `docs/prompts/` movendo 43 prompts versionados para la via `git mv`. Hook 4.4 (modo warn, limite 800 linhas) passou a alertar em prompts antigos longos:

- `prompt-etapa-4-11.md` ~870 linhas
- `prompt-etapa-4-12.md` 953 linhas
- `prompt-etapa-4-13.md` 1018 linhas

Falso positivo: prompts versionados sao **registros historicos por natureza**. Tamanho nao e criterio de qualidade — prompts longos sao consequencia natural de sub-etapas complexas. Alerta repetido em prompts antigos polui output do commit e degrada sinal/ruido do hook.

### Mudanca cirurgica

Filtro de path do hook `docs-size.ps1` ganha exclusao `docs/prompts/`. Tudo o mais preservado:

- Modo `warn` (nao `fail`).
- Limite 800 linhas.
- Extensao `.md`.
- Alerta para `.md` em `docs/` raiz e qualquer subpasta futura que nao seja `docs/prompts/`.

Outros hooks que tocam `.md` permanecem ativos em `docs/prompts/`:

- **Hook 4.2** (encoding UTF-8): convencao de qualidade que vale para qualquer `.md`.
- **Hook 4.3** (Markdown blank lines): mesma justificativa.

Hook 4.4 isenta `docs/prompts/` porque tamanho e fenomeno emergente de sub-etapas complexas, nao desvio de qualidade.

### Categoria nova: "ajuste de hook por contexto novo"

Distinta de:

- **"Patch tecnico"** (4.0.1): corrige bug do entregue.
- **"Refinamento pos-smoke empirico"** (4.9.1): componente funcional mas output divergente.
- **"Errata de ADR baseada em descoberta de documentacao oficial"** (4.11): decisao estrutural preservada, mecanismo literal refinado.
- **Esta categoria:** hook cumpre regra original corretamente, mas contexto novo (subpasta criada em sub-etapa anterior — aqui `docs/prompts/` na 4.13) muda o que a regra deveria fazer. Refactor ajusta hook ao contexto sem reverter intencao.

### Padrao operacional novo: "debito originario X -> resolvido em X+N"

PR body da 4.13 registrou debito explicito ("excluir `docs/prompts/`"); 4.14 resolveu. Cadeia "sub-etapa X cria contexto -> X+N resolve debito de contexto" formalizada. Aplicavel a outros debitos meta-operacionais (4.10): memoria global, plugins, built-in agents — cada um pode virar sub-etapa dedicada de mitigacao quando aparecer dor real.

### Primeira modificacao de hook existente no projeto

Hooks 4.1-4.7 entregaram componentes novos; 4.14 e o **primeiro refactor de hook**. Branch `refactor/` aplicada conforme padrao consolidado (4.9.1 refinou subagent; 4.14 refina hook). Hooks podem evoluir conforme contexto do projeto evolui, sem perder cobertura original.

### Validacao destrutiva sob ADR-011 (6 cenarios)

| Cenario | Caso | Esperado | Observado |
|---|---|---|---|
| 1 | `.md` em `docs/` raiz, 900 linhas | Alerta (warn) | **A confirmar pelo agente** |
| 2 | `.md` em `docs/` raiz, 500 linhas | Sem alerta | **A confirmar pelo agente** |
| 3 | `.md` em `docs/prompts/`, 900 linhas | Sem alerta (NOVO) | **A confirmar pelo agente** |
| 4 | `.md` em `docs/prompts/`, 500 linhas | Sem alerta | **A confirmar pelo agente** |
| 5 | `.md` em `docs/teste-subpasta/`, 900 linhas | Alerta (warn) | **A confirmar pelo agente** |
| 6 | `.md` na raiz do repo, 900 linhas | Sem alerta | **A confirmar pelo agente** |

Cenarios cobrem (a) comportamento original preservado: 1, 2, 5, 6; (b) comportamento novo introduzido: 3, 4. Cleanup completo entre cenarios via `git reset HEAD~1 --soft` + `Remove-Item`.

### Debito 4.13 resolvido

Item "Excluir `docs/prompts/` da verificacao do hook 4.4" removido de `hooks-pendentes.md` -> "Debitos meta-operacionais". Linha do hook 4.4 em "Hooks implementados" atualizada com referencia a 4.14 (sub-etapa que refinou).

### Mudancas

- `.claude/hooks/universal/docs-size.ps1`: filtro de path adiciona exclusao `docs/prompts/`. ~1-3 linhas alteradas.
- `docs/decisoes.md`: subsecao "Hook 4.4 exclui `docs/prompts/` (Sub-etapa 4.14)" antes de "Claude Code hooks nativos".
- `docs/progresso.md`: sub-etapa 4.14 + 4 licoes + historico.
- `docs/hooks-pendentes.md`: debito 4.13 removido + linha do hook 4.4 atualizada.
- `docs/prompts/prompt-etapa-4-14.md`: prompt versionado.

### CLAUDE.md NAO atualizado

Regra 4.6: refactor de hook nao muda convencao do projeto. Convencao "hooks decidem aplicabilidade internamente; modo warn vs fail; padrao orquestrador agnostico a escopo" permanece.

### Hook 4.4 NAO alerta neste PR

Apos a modificacao da Tarefa 5, hook 4.4 nao alerta em `prompt-etapa-4-14.md` (em `docs/prompts/`, agora isento). **Validacao organica do cenario 4** durante o Commit 3 da propria sub-etapa.

### Proximo passo

Decisao fora deste PR. Candidatos naturais:

- **4.15** — Mitigar debitos meta-operacionais da 4.10 (memoria global, plugins globais, built-in agents).
- **4.15 alternativo** — `test-writer` + skill `/write-test` (terceiro par skill+subagent — territorio novo: subagent gerador).
- **4.15 alternativo** — Skill sem subagent `/feature <nome>` (eixo novo: skill geradora pura).
````

## Pos-criacao do PR

1. Abrir PR via `gh pr create`.
2. Capturar o numero.
3. Editar `docs/decisoes.md`, `docs/progresso.md`, `docs/hooks-pendentes.md` substituindo `PR #XX` por `PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorizacao explicita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `refactor/etapa-4-14-hook-docs-size-exclui-prompts` empurrada com 4 commits (3 + 1 update do PR).
- PR aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.13.
- Working tree limpo.
- `.claude/hooks/universal/docs-size.ps1` modificado (~1-3 linhas alteradas).
- `docs/hooks-pendentes.md` sem o debito da 4.13 + linha do hook 4.4 com referencia a 4.14.
- Reportar: `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup`, **resultado de cada um dos 6 cenarios destrutivos**.

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao modificar outros hooks (4.1, 4.2, 4.3, 4.5, 4.7).
- Nao criar hooks novos.
- Nao criar subagents, skills, MCPs.
- Nao mexer em `~/.claude/` global.
- Nao atualizar `CLAUDE.md`, `adrs.md`, `visao.md`, `blueprint-fabrica-ai-native.md`, `progresso-historico.md`.
- Nao criar prompt da 4.15.
- Nao deixar arquivos de teste residuais no working tree.
- Nao sugerir proximo passo espontaneamente alem dos candidatos ja citados no PR body.
- Nao usar `pwsh`.
- Nao usar `git reset --hard`.
- Nao usar `git commit --no-verify`.
