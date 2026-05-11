# Prompt — Etapa 4.4: Hook universal de tamanho de docs (modo warn — fecha lote universal de Markdown)

## Contexto

Camada 3 com 3 hooks funcionais em produção:

- 4.1 (PR #40): Conventional Commits via `commit-msg`.
- 4.2 (PR #41): Encoding UTF-8 via `pre-commit`, orquestrador 1:N estabelecido.
- 4.3 (PR #43): Markdown blank lines via `pre-commit`, primeira regra que parseia conteúdo.

ADR-011 (PR #42) formalizou padrões de validação destrutiva.

**Esta sub-etapa entrega o quarto hook funcional** e fecha o lote universal de Markdown. Após 4.4, próximas sub-etapas vão para hooks de stack (`java-spring/`), CLAUDE.md do projeto, subagents, ou skills.

Características novas desta sub-etapa:

1. **Primeiro hook em modo `warn`, não `fail`.** Tamanho de doc é subjetivo — não tem "valor errado". Hook alerta no stdout mas **não bloqueia commit**. Decisão consciente registrada em `decisoes.md`.
2. **Quarto hook no orquestrador `pre-commit`** — mais uma linha no array `$hooks`. Padrão de extensão por linha consolidado desde 4.3.
3. **Padrão de modo no orquestrador.** Como o hook não bloqueia, o orquestrador trata exit code 0 como sucesso e qualquer outro como falha — mas o hook em modo `warn` sempre sai com 0. Decisão: warn é puramente cosmético do ponto de vista do git; alerta no stdout/stderr é o gate humano.

A regra implementada: arquivos `.md` em `docs/` no diff staged que tenham **mais de 800 linhas totais** geram alerta visual no terminal durante o commit. Commit prossegue.

Quando esta etapa terminar:

- Commit local que toque `.md` em `docs/` com >800 linhas exibe alerta visível.
- Lote universal de Markdown fechado (encoding UTF-8 + blank lines + tamanho).
- `progresso.md` (~680 linhas) e `decisoes.md` (~470 linhas) permanecem dentro do limite — alerta não dispara hoje, mas ficará armado para evitar inflação enciclopédica futura.

## Padrões que estreiam nesta etapa

1. **Primeiro hook em modo `warn`** — alerta sem bloquear. Estabelece padrão para regras subjetivas futuras.
2. **Quarto hook funcional, terceiro no orquestrador `pre-commit`.**
3. **Escopo restrito por pasta** (`docs/` apenas) — primeira regra com filtro de path, não apenas extensão.
4. **Fecha o lote universal de Markdown** — após 4.4, próximas universais (se houver) entram por demanda, não pelo plano.

## Escopo decidido (calibrado com operador antes da redação)

### Regra implementada

**Alvo:** arquivos `.md` cujo caminho começa com `docs/`. Outros `.md` (README.md raiz, `.github/`, `frontend/`, etc.) são ignorados.

**Métrica:** linhas totais do arquivo (incluindo linhas em branco). Usa `[System.IO.File]::ReadAllLines($path).Count`.

**Limite:** 800 linhas. Acima disso → alerta.

**Comportamento:** alerta visual no terminal (cor amarela, formato consistente com outros hooks). **Não bloqueia commit.** Hook sempre sai com exit code 0.

**Filtro do diff:** `git diff --cached --name-only --diff-filter=ACM` (padrão consolidado). Apenas arquivos no diff são checados — arquivos pre-existentes grandes não disparam alerta se não forem tocados.

**Override:** não aplicável — hook não bloqueia. `--no-verify` continua válido se necessário para outros hooks da pipeline.

### Arquivos criados e modificados

```
.claude/hooks/universal/docs-size.ps1     ← novo (lógica real, modo warn)
.githooks/pre-commit.ps1                   ← edição (adiciona 1 linha ao array $hooks)
docs/decisoes.md                           ← edição (subseção da 4.4 + modo warn como padrão para regras subjetivas)
docs/hooks-pendentes.md                    ← edição (move item tamanho-docs para Implementados + data)
docs/progresso.md                          ← edição (lições + sub-etapa + histórico)
docs/prompt-etapa-4-4.md                   ← novo (este próprio prompt)
```

**Não tocar:**

- `.claude/hooks/universal/conventional-commits.ps1` (4.1).
- `.claude/hooks/universal/encoding-utf8.ps1` (4.2).
- `.claude/hooks/universal/markdown-blank-lines.ps1` (4.3).
- `.githooks/commit-msg`, `.githooks/commit-msg.ps1` (4.1).
- `.githooks/pre-commit` (entrypoint bash da 4.2 — só o `.ps1` companheiro muda).
- `.githooks/README.md` (padrão orquestrador já documentado desde 4.2).
- ADRs.

### Conteúdo de `.claude/hooks/universal/docs-size.ps1` (lógica real, modo warn)

```powershell
$ErrorActionPreference = "Stop"

# Limite de linhas para alerta de tamanho de docs (modo warn — nao bloqueia commit).
$LIMITE_LINHAS = 800

function Test-IsDocsMarkdown {
    param([string]$Path)

    # Apenas .md em docs/ (qualquer nivel de profundidade).
    $pathNorm = $Path -replace '\\', '/'
    $ext = [System.IO.Path]::GetExtension($pathNorm).ToLower()

    if ($ext -ne '.md') { return $false }
    if (-not ($pathNorm -like 'docs/*')) { return $false }

    return $true
}

function Get-LineCount {
    param([string]$Path)

    $lines = [System.IO.File]::ReadAllLines($Path, [System.Text.UTF8Encoding]::new($false))
    if ($null -eq $lines) { return 0 }
    return $lines.Count
}

# Listar arquivos staged (Added, Copied, Modified)
$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$stagedRaw = git diff --cached --name-only --diff-filter=ACM 2>&1
$diffExitCode = $LASTEXITCODE
$ErrorActionPreference = $prev

if ($diffExitCode -ne 0) {
    Write-Host "[ERRO] Falha ao listar arquivos staged via git diff --cached." -ForegroundColor Red
    exit 1
}

$stagedFiles = $stagedRaw | Where-Object { $_ -and $_.Trim() }

if (-not $stagedFiles -or $stagedFiles.Count -eq 0) {
    exit 0
}

$alerts = @()

foreach ($file in $stagedFiles) {
    if (-not (Test-Path $file)) { continue }
    if (-not (Test-IsDocsMarkdown $file)) { continue }

    $lineCount = Get-LineCount -Path $file
    if ($lineCount -gt $LIMITE_LINHAS) {
        $alerts += [PSCustomObject]@{
            File = $file
            Lines = $lineCount
            Excess = $lineCount - $LIMITE_LINHAS
        }
    }
}

# Forcar contexto array (licao 4.3: PS5.1 desempacota array de 1 elemento)
$alerts = @($alerts)

if ($alerts.Count -gt 0) {
    Write-Host ""
    Write-Host "[ALERTA] Tamanho de docs acima do limite ($LIMITE_LINHAS linhas) em $($alerts.Count) arquivo(s):" -ForegroundColor Yellow
    Write-Host ""
    foreach ($a in $alerts) {
        Write-Host "  $($a.File)" -ForegroundColor Yellow
        Write-Host "    Linhas: $($a.Lines) (excede limite em $($a.Excess) linhas)" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "Recomendacao:" -ForegroundColor Cyan
    Write-Host "  - Docs em 'docs/' com mais de $LIMITE_LINHAS linhas tendem a virar enciclopedia." -ForegroundColor Cyan
    Write-Host "  - Considerar dividir em arquivos menores ou extrair secoes maduras para ADRs." -ForegroundColor Cyan
    Write-Host "  - Este e um alerta — commit prossegue normalmente. Nao precisa de --no-verify." -ForegroundColor Cyan
    Write-Host ""
}

# Modo warn: hook nunca bloqueia.
exit 0
```

**Notas críticas:**

1. **`exit 0` sempre** — modo warn não bloqueia. Diferente dos hooks anteriores (4.1, 4.2, 4.3) que retornam 1 em violação.
2. **Lição da 4.3 aplicada na fonte:** `$alerts = @($alerts)` força contexto array antes do `.Count`. Evita o gotcha PS5.1 que descobrimos.
3. **Sem acentos** nas mensagens.
4. **Encoding UTF-8 sem BOM** no próprio arquivo (hook da 4.2 ativo valida).
5. **`Write-Host -ForegroundColor Yellow`** (não Red) — distingue visualmente "alerta" de "erro" nos outros hooks.
6. **Tag `[ALERTA]`** em vez de `[ERRO]` — consistência visual com a distinção warn vs fail.
7. **Recomendação explícita "não precisa de --no-verify"** na mensagem — evita override desnecessário.
8. **Suspensão local de `$ErrorActionPreference`** ao chamar `git diff --cached` (lição 2.6.2).

### Edição em `.githooks/pre-commit.ps1` (adiciona 1 linha ao array `$hooks`)

**Estado atual** (do PR #43):

```powershell
$hooks = @(
    (Join-Path $repoRoot ".claude\hooks\universal\encoding-utf8.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\markdown-blank-lines.ps1")
)
```

**Estado após esta etapa:**

```powershell
$hooks = @(
    (Join-Path $repoRoot ".claude\hooks\universal\encoding-utf8.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\markdown-blank-lines.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\docs-size.ps1")
)
```

**Apenas essa linha muda.** Resto do arquivo inalterado.

### Atualização de `docs/decisoes.md`

Adicionar nova subseção sob "Camada 3 — Configuração do Claude Code", **após** "Blank lines em Markdown (Sub-etapa 4.3)" e **antes** de "Claude Code hooks nativos":

```markdown
### Tamanho de docs em modo warn (Sub-etapa 4.4)

**Regra:** arquivos `.md` em `docs/` (qualquer nivel de profundidade) com mais de 800 linhas totais geram alerta visual no terminal durante o commit. **Commit prossegue normalmente — alerta nao bloqueia.**

**Escopo:** apenas `docs/*.md`. Outros `.md` (README raiz, `.github/`, `frontend/`, etc.) sao ignorados.

**Metrica:** linhas totais via `[System.IO.File]::ReadAllLines($path).Count`. Inclui linhas em branco — simples, alinhado com como o operador ve o arquivo.

**Limite:** 800 linhas. Folga sobre o `progresso.md` atual (~680) e `decisoes.md` (~470), com espaco para crescimento natural ao longo das Camadas 3 a 6.

**Padrao novo estabelecido — modo `warn` para regras subjetivas:**

Tamanho de doc nao tem "valor errado". 600 linhas pode ser certo para um doc denso; 1500 pode ser certo para um indice completo. Bloquear forcaria split apressado em momentos inoportunos. Por isso, hooks de **regras subjetivas** seguem padrao `warn`: alertam no terminal mas saem com exit code 0, deixando ao operador a decisao de agir.

Hooks de **regras objetivas** continuam em modo `fail` (Conventional Commits, encoding UTF-8, blank lines em Markdown). Modo do hook e parte do design, registrada em `decisoes.md` quando o hook nasce.

**Override:** nao aplicavel — hook nao bloqueia. `--no-verify` continua valido se necessario para outros hooks da pipeline.

**Hook implementado em:** `.claude/hooks/universal/docs-size.ps1`, terceiro hook no orquestrador `pre-commit` (1:N da 4.2). Apenas uma linha adicionada ao array `$hooks` em `.githooks/pre-commit.ps1`.

**Fecha o lote universal de Markdown** — apos 4.4, proximas universais (se houver) entram por demanda, nao pelo plano. Proximas sub-etapas focam em hooks de stack (`java-spring/`), CLAUDE.md, subagents ou skills.
```

Adicionar entrada no histórico (final do arquivo):

```markdown
- **2026-MM-DD** — Sub-etapa 4.4 concluida: quarto hook funcional. Tamanho de docs em modo warn (`.md` em `docs/` com mais de 800 linhas gera alerta sem bloquear). Estabelece padrao `warn` para regras subjetivas vs `fail` para regras objetivas. Lote universal de Markdown fechado (encoding, blank lines, tamanho). Mergeado via PR #XX.
```

### Atualização de `docs/hooks-pendentes.md`

**Operação A** — Remover item "Tamanho de docs em `docs/`" da seção "Hooks Markdown / docs":

```markdown
- **Tamanho de docs em `docs/`.** (Etapa 1.1) Alertar se algum `.md` em `docs/` ultrapassa limite (anti-enciclopédia).
```

Após esta remoção, a seção "Hooks Markdown / docs" fica **vazia** — lote universal fechado.

**Operação B** — Adicionar entrada em "Hooks implementados":

```markdown
- **Tamanho de docs em `docs/` (modo warn)** (Sub-etapa 4.4, PR #XX). Implementado em `.claude/hooks/universal/docs-size.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Limite: 800 linhas totais. Alerta visual em amarelo, **nao bloqueia commit**. Apenas `.md` em `docs/` — outros `.md` ignorados. Modo `warn` registrado como padrao para regras subjetivas em `decisoes.md`.
```

**Operação C** — Considerar remover ou ajustar a seção "Hooks Markdown / docs" se ficar vazia. Defende-se manter a seção com nota "(Todos os itens deste grupo foram implementados ate 4.4 — encoding, blank lines, tamanho.)" para preservar histórico organizacional.

**Operação D** — Atualizar data:

```markdown
**Última atualização:** 2026-MM-DD (Sub-etapa 4.4 — Tamanho de docs em modo warn)
```

### Atualização de `docs/progresso.md`

**A.** Atualizar "Última atualização": `2026-MM-DD (Sub-etapa 4.4 — Tamanho de docs em modo warn)`.

**B.** Em "Sub-etapas concluídas" (ordem cronológica, após 4.3):

```markdown
- **4.4 — Hook universal de tamanho de docs (modo warn)** (2026-MM-DD): quarto hook funcional. Terceiro no orquestrador `pre-commit`. Alerta sobre `.md` em `docs/` com mais de 800 linhas — **nao bloqueia commit**, apenas visibiliza. Estabelece padrao `warn` para regras subjetivas (distinto de `fail` para regras objetivas). Fecha lote universal de Markdown (encoding 4.2 + blank lines 4.3 + tamanho 4.4). 5 cenarios destrutivos sob ADR-011. PR #XX.
```

**C.** Adicionar seção "Lições da Sub-etapa 4.4":

```markdown
## Licoes da Sub-etapa 4.4

### Candidatos a hook (automatizar em etapas futuras)

(A preencher se houver durante execucao.)

### Licoes de ambiente

(A preencher se houver durante execucao. Esperado pelo menos: padrao `warn` vs `fail` formalizado em decisoes.md — regras subjetivas geram alerta sem bloquear; regras objetivas continuam bloqueando.)
```

**D.** Histórico geral:

```markdown
- **2026-MM-DD** — Sub-etapa 4.4 concluida: quarto hook funcional. Tamanho de docs em modo warn — alerta sem bloquear `.md` em `docs/` com mais de 800 linhas. Lote universal de Markdown fechado. Padrao `warn` vs `fail` para regras subjetivas vs objetivas registrado. Mergeado via PR #XX.
```

### Versionar este próprio prompt

`docs/prompt-etapa-4-4.md`.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra `0488daa` (squash da 4.3) ou superior.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-4.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `.claude/hooks/universal/` contém `conventional-commits.ps1`, `encoding-utf8.ps1`, `markdown-blank-lines.ps1` (3 arquivos, sem `docs-size.ps1`).
- `.githooks/` contém `README.md`, `commit-msg`, `commit-msg.ps1`, `pre-commit`, `pre-commit.ps1`.

**Pré-requisitos de ambiente (lições da 4.1, 4.2, 4.2.1, 4.3):**

- `powershell` (Windows PowerShell 5.1) disponível. **NÃO usar `pwsh`.**
- Git Bash disponível.
- **`[System.Environment]::CurrentDirectory` deve ser sincronizado com `$PWD`** antes de qualquer `[System.IO.File]::WriteAllText` com path relativo (ADR-011).
- **Padrão `@(...)` ao consumir retorno de função em PS5.1** (lição da 4.3).

Validar com:

```powershell
git status
git log --oneline -3
Test-Path docs\prompt-etapa-4-4.md
git config core.hooksPath
Get-ChildItem .claude\hooks\universal\
Get-ChildItem .githooks\
```

**Pré-condições ADR-011:**

- `Test-Path docs\prompt-etapa-4-4.md` retorna `True`.
- `git status` mostra apenas o prompt como untracked.
- `.claude/hooks/universal/` lista exatamente 3 `.ps1`.

Se algum item divergir, **parar e reportar**.

## Tarefas

### Tarefa 1 — Validar pré-requisitos

Rodar comandos da seção "Estado esperado ao iniciar". Se divergir, parar e reportar.

### Tarefa 2 — Sincronizar `Environment.CurrentDirectory` (ADR-011)

```powershell
cd C:\projetos\financas-lab
[System.Environment]::CurrentDirectory = (Get-Location).Path
[System.Environment]::CurrentDirectory
```

**Esperado:** segunda linha retorna `C:\projetos\financas-lab`. Se divergir, re-executar.

### Tarefa 3 — Criar branch

```bash
git checkout -b feat/etapa-4-4-docs-size-hook
```

### Tarefa 4 — Antes de editar, ler arquivos vivos

```bash
cat .claude/hooks/universal/markdown-blank-lines.ps1
cat .githooks/pre-commit.ps1
cat docs/decisoes.md
cat docs/hooks-pendentes.md
cat docs/progresso.md
```

**Confirmar especialmente:**

- `markdown-blank-lines.ps1` (referência de estilo para o novo hook — replicar consistência).
- `pre-commit.ps1` tem array `$hooks` com 2 entradas atuais (`encoding-utf8`, `markdown-blank-lines`).
- `decisoes.md` tem subseções em ordem: "Padrao orquestrador 1:N (4.2)" → "Padroes de validacao destrutiva (4.2.1)" → "Blank lines em Markdown (4.3)" → "Claude Code hooks nativos". A nova "Tamanho de docs em modo warn (4.4)" entra **entre 4.3 e Claude Code hooks nativos**.
- `hooks-pendentes.md` tem item "Tamanho de docs em `docs/`" em "Hooks Markdown / docs" — será removido.
- `progresso.md` tem "Sub-etapas concluídas" em ordem 4.0 → 4.0.1 → 4.1 → 4.2 → 4.2.1 → 4.3.

Se alguma divergência, **parar e reportar**.

### Tarefa 5 — Criar hook universal `.claude/hooks/universal/docs-size.ps1`

Conteúdo conforme escopo decidido. **UTF-8 sem BOM.** Sem acentos.

**Pré-condição ADR-011:** após criar, validar bytes:

```powershell
$bytes = [System.IO.File]::ReadAllBytes(".claude/hooks/universal/docs-size.ps1")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191
```

Se aparecer BOM, corrigir antes de seguir.

### Tarefa 6 — Editar `.githooks/pre-commit.ps1`

Adicionar **uma linha** ao array `$hooks` conforme escopo. Validar com `git diff` mostrando **exatamente 1 linha adicionada**.

### Tarefa 7 — Commit 1 (hook lógica)

```bash
git add .claude/hooks/universal/docs-size.ps1
git status
```

**Pré-condição ADR-011:** 1 arquivo staged.

```bash
git commit -m "feat(claude): adiciona hook universal de tamanho de docs em modo warn"
```

**Pré-condição ADR-011:** `$LASTEXITCODE` deve ser `0`.

### Tarefa 8 — Commit 2 (orquestrador estendido)

```bash
git add .githooks/pre-commit.ps1
git status
```

**Pré-condição ADR-011:** 1 arquivo staged.

```bash
git commit -m "feat(githooks): adiciona docs-size ao orquestrador pre-commit"
```

A partir deste commit, hook `docs-size` está **ativo no pre-commit** local.

**Importante:** o próprio `progresso.md` (~680 linhas) e `decisoes.md` (~470 linhas) estão abaixo do limite de 800. Próximos commits desta sub-etapa não devem disparar alerta. Se dispararem, há erro no limite ou no escopo (talvez algum arquivo passou de 800 sem notarmos).

### Tarefa 9 — Pré-validação dos docs antes do Commit 3

Confirmar que docs editados nesta sub-etapa não disparam alerta:

```bash
git add docs/decisoes.md docs/hooks-pendentes.md docs/progresso.md
git status
```

Executar hook direto:

```powershell
& .\.claude\hooks\universal\docs-size.ps1
$hookResult = $LASTEXITCODE
Write-Host "Hook exit code: $hookResult"
```

**Esperado:** exit 0 (modo warn — sempre sai 0). **Não deve aparecer mensagem `[ALERTA]`** porque nenhum dos 3 docs excede 800 linhas após esta sub-etapa.

Se aparecer `[ALERTA]`, investigar:
- `progresso.md` cresceu além de 800 com adições da 4.4? Provável se `Lições da Sub-etapa 4.4` virou seção longa. Considerar concisão na lição.
- `decisoes.md` cresceu além de 800? Improvável (~470 + ~30 linhas da subseção nova = ~500).
- Bug no hook? Validar limite e cálculo de linhas.

Se alerta legítimo (algum doc realmente cruzou 800), **a 4.4 conviveu com sua própria intervenção**: o alerta dispara no próprio commit que documenta a 4.4. Considerar isso como caso destrutivo orgânico — incluir na validação destrutiva.

### Tarefa 10 — Commit 3 (docs decisoes + hooks-pendentes)

```bash
git reset HEAD docs/progresso.md
git status  # deve mostrar decisoes.md e hooks-pendentes.md staged
git commit -m "docs: registra tamanho de docs em modo warn e padrao warn-vs-fail"
```

**Pré-condições ADR-011:** 2 arquivos staged antes do commit; `$LASTEXITCODE = 0` após.

### Tarefa 11 — Commit 4 (progresso + prompt versionado)

```bash
git add docs/progresso.md docs/prompt-etapa-4-4.md
git status
git commit -m "docs: registra sub-etapa 4.4 em progresso e versiona prompt"
```

**Pré-condições ADR-011:** 2 arquivos staged; `$LASTEXITCODE = 0`.

### Tarefa 12 — Validação destrutiva sob ADR-011 (5 cenários)

**Pré-condição global:**

```powershell
[System.Environment]::CurrentDirectory
(Get-Location).Path
git status
git log --oneline -1
```

Sincronizado, working tree limpo, HEAD no Commit 4.

#### Cenário 1: `.md` em `docs/` com ≤800 linhas → passa silenciosamente

```powershell
$repoRoot = (Get-Location).Path

# Criar doc pequeno em docs/
$conteudoPequeno = @"
# Doc Pequeno

Texto curto.

## Secao

Conteudo.
"@

[System.IO.File]::WriteAllText("$repoRoot\docs\test-small.md", $conteudoPequeno, (New-Object System.Text.UTF8Encoding $false))

Test-Path .\docs\test-small.md   # True

git reset HEAD
git add docs/test-small.md
git status   # apenas test-small.md staged

git commit -m "test: validacao destrutiva cenario 1 docs pequeno"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 1 exit code: $exitCode (esperado: 0)"
```

**Esperado:** commit aceito, **sem alerta visível** do hook docs-size (poucas linhas).

#### Cenário 2: `.md` em `docs/` com >800 linhas → alerta + commit aceito

```powershell
# Gerar doc com 1000 linhas (header + 999 linhas de conteudo)
$linhas = @("# Doc Grande", "")
for ($i = 1; $i -le 998; $i++) {
    $linhas += "Linha de conteudo numero $i."
}
$conteudoGrande = $linhas -join "`n"

[System.IO.File]::WriteAllText("$repoRoot\docs\test-large.md", $conteudoGrande, (New-Object System.Text.UTF8Encoding $false))

Test-Path .\docs\test-large.md

# Confirmar tamanho via PowerShell antes do commit
$linhasReais = [System.IO.File]::ReadAllLines("$repoRoot\docs\test-large.md").Count
Write-Host "Linhas reais do test-large.md: $linhasReais (esperado: > 800)"

git reset HEAD
git add docs/test-large.md
git status

git commit -m "test: validacao destrutiva cenario 2 docs grande alerta"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 2 exit code: $exitCode (esperado: 0 — modo warn nao bloqueia)"
```

**Esperado:**
- Output do `git commit` inclui `[ALERTA] Tamanho de docs acima do limite...` em amarelo.
- Reporta `docs/test-large.md` com contagem de linhas e excesso.
- `$LASTEXITCODE = 0` mesmo com alerta — **commit aceito**.

Se commit for rejeitado, hook está em modo errado (`exit 1` em vez de `exit 0`). Parar e investigar.

Se commit aceito **sem alerta visível**, hook não detectou. Parar e investigar (provavelmente limite ou escopo).

#### Cenário 3: `.md` fora de `docs/` com >800 linhas → ignorado

```powershell
# Mesmo conteudo grande, mas na raiz do repo
$conteudoGrande = (Get-Content docs/test-large.md -Raw)
[System.IO.File]::WriteAllText("$repoRoot\test-large-root.md", $conteudoGrande, (New-Object System.Text.UTF8Encoding $false))

Test-Path .\test-large-root.md

git reset HEAD
git add test-large-root.md
git status

git commit -m "test: validacao destrutiva cenario 3 md fora de docs grande"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 3 exit code: $exitCode (esperado: 0)"
```

**Esperado:** commit aceito **sem alerta** do `docs-size` (arquivo está fora de `docs/`).

#### Cenário 4: múltiplos `.md` em `docs/` no mesmo commit (alguns >800, outros não)

```powershell
$conteudoMedio = @("# Doc Medio", "") + (1..500 | ForEach-Object { "Linha $_." })
$conteudoMedioStr = $conteudoMedio -join "`n"
[System.IO.File]::WriteAllText("$repoRoot\docs\test-medium.md", $conteudoMedioStr, (New-Object System.Text.UTF8Encoding $false))

Test-Path .\docs\test-medium.md
$linhasMedio = [System.IO.File]::ReadAllLines("$repoRoot\docs\test-medium.md").Count
Write-Host "test-medium.md linhas: $linhasMedio (esperado: ~502, abaixo de 800)"

# Modificar test-large.md (que ja existe e tem >800)
Add-Content -Path "$repoRoot\docs\test-large.md" -Value "Linha adicional."

git reset HEAD
git add docs/test-medium.md docs/test-large.md
git status

git commit -m "test: validacao destrutiva cenario 4 multiplos docs alguns grandes"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 4 exit code: $exitCode (esperado: 0)"
```

**Esperado:**
- Alerta sobre `test-large.md` (>800 linhas).
- **Sem** alerta sobre `test-medium.md` (≤800 linhas).
- Commit aceito.

#### Cenário 5: `.md` em `docs/` cresceu mas ainda ≤800 → passa silenciosamente

```powershell
# Adicionar conteudo ao test-medium.md (que ja tem ~502 linhas)
Add-Content -Path "$repoRoot\docs\test-medium.md" -Value @"

Mais uma secao.

Conteudo adicional.
"@

$linhasNovas = [System.IO.File]::ReadAllLines("$repoRoot\docs\test-medium.md").Count
Write-Host "test-medium.md linhas apos crescer: $linhasNovas (esperado: ainda abaixo de 800)"

git reset HEAD
git add docs/test-medium.md
git status

git commit -m "test: validacao destrutiva cenario 5 docs cresceu mas dentro do limite"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 5 exit code: $exitCode (esperado: 0)"
```

**Esperado:** commit aceito **sem alerta** — arquivo cresceu mas continua abaixo do limite.

### Tarefa 13 — Limpeza dos commits e arquivos de teste

```bash
git log --oneline -10
```

Esperado (do mais recente):

```
<commit cenario 5>
<commit cenario 4>
<commit cenario 3>
<commit cenario 2>
<commit cenario 1>
<Commit 4: docs progresso + prompt>
<Commit 3: docs decisoes + hooks-pendentes>
<Commit 2: feat(githooks)>
<Commit 1: feat(claude)>
<squash 4.3>
```

**5 commits de teste** (todos os 5 cenários são aceitos em modo warn).

```bash
git branch --show-current  # confirmar feat/etapa-4-4-docs-size-hook
git reset --hard HEAD~5
```

**ATENÇÃO ADR-011:** se contagem divergir do esperado, **parar e reportar**. Se sandbox bloquear `git reset --hard`, pedir execução manual ao operador (lição da 4.2).

Limpar arquivos de teste:

```powershell
Remove-Item docs/test-small.md, docs/test-large.md, docs/test-medium.md, test-large-root.md -ErrorAction SilentlyContinue
git status
```

**Pré-condição ADR-011:** working tree limpo.

### Tarefa 14 — Validação final antes de push

```bash
git status
git log --oneline -5
git config core.hooksPath
```

Esperado:
- Working tree limpo.
- 4 commits novos.
- `core.hooksPath` retorna `.githooks`.

## Restrições e freios

1. **Não criar outros hooks.** Apenas tamanho de docs. Lote universal fecha aqui.

2. **Não tocar em entrypoints** (`.githooks/commit-msg`, `.githooks/pre-commit`). Apenas `.githooks/pre-commit.ps1` ganha 1 linha no array.

3. **Não modificar hooks existentes** (`conventional-commits.ps1`, `encoding-utf8.ps1`, `markdown-blank-lines.ps1`).

4. **Não criar subagents, skills, CLAUDE.md.**

5. **Não tocar em scripts existentes** (`setup.ps1`, etc).

6. **Não tocar em `src/`, `pom.xml`, `application*.yml`, `frontend/`, `docker-compose.yml`, migrations.**

7. **Não tocar em `.gitignore`, `.gitattributes`, ADRs.**

8. **Não tocar em `.githooks/README.md`** (padrão orquestrador já documentado).

9. **Não introduzir dependências externas.** PowerShell puro + .NET nativo.

10. **Encoding UTF-8 sem BOM** no `docs-size.ps1`.

11. **Sem acentos** nas mensagens do hook.

12. **Não usar `Write-Error` + `exit`.** Padrão: `Write-Host -ForegroundColor` + `exit N`.

13. **Não usar `pwsh`.** Apenas `powershell` (PS5.1).

14. **Hook DEVE sempre retornar exit code 0** — modo warn. Diferente dos hooks anteriores. Se retornar 1 em qualquer cenário, está errado.

15. **Aplicar lição da 4.3 (array unwrapping PS5.1):** `$alerts = @($alerts)` antes de checar `.Count`. Sem isso, alerta de 1 arquivo pode ser silenciado.

16. **Pré-condições ADR-011** em cada cenário destrutivo: `Test-Path`, `git status`, `$LASTEXITCODE`.

17. **`git reset --hard` apenas na branch da etapa.** Sandbox-block; aprovação manual.

18. **Validação destrutiva COMPLETA (5 cenários)** é gate de "pronto". Reportar saídas no PR body **incluindo pré-condições**.

19. **`git reset HEAD` entre cenários** — staging isolado (lição da 4.3, item 3 das lições). Cada cenário começa com staging limpo.

20. **Não tomar decisão silenciosa em zona limítrofe.** Se algum cenário comportar diferente do esperado, parar e reportar.

21. **Ordem cronológica em `progresso.md`:** 4.0 → 4.0.1 → 4.1 → 4.2 → 4.2.1 → 4.3 → 4.4.

22. **Lógica de validação fica em `docs-size.ps1`**, orquestrador apenas referencia.

23. **Não sugerir próxima sub-etapa** espontaneamente.

24. **Antes de escrever cada arquivo, ler arquivo vivo** (Tarefa 4).

25. **Mensagem do hook deve usar `Yellow`, não `Red`.** Distinção visual entre alerta e erro é parte do padrão warn-vs-fail.

26. **Tag `[ALERTA]` na mensagem do hook.** Não `[ERRO]` — consistência com a distinção.

## Estrutura de commits

Branch: `feat/etapa-4-4-docs-size-hook`

**Commit 1** — `feat(claude): adiciona hook universal de tamanho de docs em modo warn`
- `.claude/hooks/universal/docs-size.ps1` (novo)

**Commit 2** — `feat(githooks): adiciona docs-size ao orquestrador pre-commit`
- `.githooks/pre-commit.ps1` (1 linha)

**Commit 3** — `docs: registra tamanho de docs em modo warn e padrao warn-vs-fail`
- `docs/decisoes.md`
- `docs/hooks-pendentes.md`

**Commit 4** — `docs: registra sub-etapa 4.4 em progresso e versiona prompt`
- `docs/progresso.md`
- `docs/prompt-etapa-4-4.md`

## Validação antes de abrir PR

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

## PR

Título: `feat: sub-etapa 4.4 — hook universal de tamanho de docs (modo warn, fecha lote universal de markdown)`

Body sugerido:

```markdown
## Summary

Implementa o **quarto hook funcional** do projeto: tamanho de docs em modo `warn`. **Fecha o lote universal de Markdown** (encoding 4.2 + blank lines 4.3 + tamanho 4.4). Apos esta sub-etapa, proximas universais entram por demanda, nao pelo plano.

**Padrao novo estabelecido:** `warn` para regras subjetivas, `fail` para regras objetivas. Registrado em `decisoes.md`.

### Regra implementada

- **Alvo:** `.md` em `docs/` (qualquer nivel de profundidade). Outros `.md` (README raiz, `.github/`, etc.) sao ignorados.
- **Limite:** 800 linhas totais. Folga sobre `progresso.md` (~680) e `decisoes.md` (~470).
- **Comportamento:** alerta visual em amarelo no terminal. **Nao bloqueia commit.**
- **Override:** nao aplicavel — hook nao bloqueia.

### Por que modo `warn`

Tamanho de doc e subjetivo — nao tem "valor errado". 600 linhas pode ser certo para um doc denso; 1500 pode ser certo para um indice completo. Bloquear forcaria split apressado em momentos inoportunos. Por isso, hooks de **regras subjetivas** seguem padrao `warn`: alertam no terminal mas saem com exit code 0.

Hooks de **regras objetivas** continuam em modo `fail` (Conventional Commits, encoding UTF-8, blank lines em Markdown).

### Validacao destrutiva sob ADR-011

Cinco cenarios executados, cada um com pre-condicoes explicitas (`Test-Path`, `git status`, `git reset HEAD` para staging isolado conforme licao 3 da 4.3):

1. **`.md` em `docs/` com ≤800 linhas** — commit aceito, sem alerta.
2. **`.md` em `docs/` com >800 linhas** — alerta em amarelo, commit aceito (modo warn).
3. **`.md` fora de `docs/` com >800 linhas** — commit aceito, sem alerta (fora do escopo).
4. **Multiplos `.md` em `docs/` (alguns >800, outros ≤800)** — alerta seletivo, commit aceito.
5. **`.md` em `docs/` cresceu mas ainda ≤800** — commit aceito, sem alerta.

Todos passaram conforme esperado.

### Aplicacao da licao 4.3 na fonte

`$alerts = @($alerts)` aplicado **antes** do `.Count` no hook docs-size, prevenindo o gotcha PS5.1 que descobrimos in-flight na 4.3 (PowerShell 5.1 desempacota array de 1 elemento). Sem essa linha, alerta sobre exatamente 1 arquivo grande seria silenciosamente ignorado.

### Mudancas

- `.claude/hooks/universal/docs-size.ps1`: logica real. Le `git diff --cached --name-only --diff-filter=ACM`, filtra `.md` em `docs/`, conta linhas via `ReadAllLines.Count`, compara contra limite. Modo warn: sempre `exit 0`. Mensagem em `Write-Host -ForegroundColor Yellow` com tag `[ALERTA]`.
- `.githooks/pre-commit.ps1`: adiciona 1 linha ao array `$hooks`. Terceiro hook no orquestrador (encoding-utf8, markdown-blank-lines, docs-size).
- `docs/decisoes.md`: subsecao "Tamanho de docs em modo warn (Sub-etapa 4.4)". Estabelece padrao `warn` vs `fail` para regras subjetivas vs objetivas. Entrada no historico.
- `docs/hooks-pendentes.md`: item "Tamanho de docs em `docs/`" movido para "Hooks implementados". Secao "Hooks Markdown / docs" fica vazia — lote fechado. Data atualizada.
- `docs/progresso.md`: sub-etapa 4.4 em "Sub-etapas concluidas". Licoes da 4.4. Entrada no historico.

### Validacao destrutiva pos-merge sugerida

Em qualquer branch (nao main), com `[System.Environment]::CurrentDirectory = (Get-Location).Path` sincronizado:

\```powershell
# Cenario A: doc pequeno passa silenciosamente
[System.IO.File]::WriteAllText("$PWD\docs\smoke-small.md", "# Smoke`n`nConteudo curto.`n", (New-Object System.Text.UTF8Encoding $false))
git reset HEAD
git add docs/smoke-small.md
git status
git commit -m "test: smoke 4.4 doc pequeno"  # aceito sem alerta

# Cenario B: doc grande gera alerta mas aceita commit
$linhas = @("# Smoke Grande") + (1..900 | ForEach-Object { "Linha $_." })
[System.IO.File]::WriteAllText("$PWD\docs\smoke-large.md", ($linhas -join "`n"), (New-Object System.Text.UTF8Encoding $false))
git reset HEAD
git add docs/smoke-large.md
git status
git commit -m "test: smoke 4.4 doc grande alerta"  # aceito COM alerta amarelo

# Limpeza
git reset --hard HEAD~2
Remove-Item docs/smoke-*.md -ErrorAction SilentlyContinue
\```

### Lote universal de Markdown — fechado

Apos 4.4, secao "Hooks Markdown / docs" em `hooks-pendentes.md` fica vazia. Tres hooks universais cobrem o essencial:

- Encoding UTF-8 (4.2) — bytes corretos.
- Blank lines em Markdown (4.3) — formato dos headers.
- Tamanho de docs (4.4) — anti-enciclopedia.

Proximas universais entram apenas se nova licao da Camada 1 nao coberta surgir, ou por demanda concreta de fora do plano.

### Proximo passo

Decisao fora deste PR. Caminhos possiveis para a Camada 3:
- Hooks de stack (`java-spring/`) — primeiro candidato: Maven `<release>` ou `@Entity` sem migration.
- CLAUDE.md do projeto (sub-etapa 4.3 do roadmap original).
- Primeiros subagents (`pr-reviewer`, `architect-reviewer`).
- Skills (`/ship`, `/feature`).

Calibracao em sessao separada.
```

## Pós-criação do PR

1. Abrir PR via `gh pr create`.
2. Capturar o número.
3. Editar `docs/decisoes.md`, `docs/hooks-pendentes.md`, `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>` e `2026-MM-DD` pela data real.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `feat/etapa-4-4-docs-size-hook` empurrada com 5 commits (4 + 1 update do PR).
- PR aberto, CI verde, **não mergeado**.
- `main` ainda no squash da 4.3.
- Working tree limpo.
- Arquivos `test-small.md`, `test-large.md`, `test-medium.md`, `test-large-root.md` removidos.
- Reportar com `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup`, e saídas dos 5 cenários destrutivos.

## O que NÃO fazer ao terminar

- Não mergear o PR.
- Não criar prompt da próxima sub-etapa.
- Não criar hooks de stack.
- Não criar subagents, skills, CLAUDE.md.
- Não tocar em scripts existentes.
- Não tocar em arquivos das 4.1, 4.2, 4.2.1, 4.3.
- Não tocar em `.gitignore`, `.gitattributes`, ADRs.
- Não deixar `test-*.*` na branch.
- Não deixar commits `test:` no histórico — limpar via `git reset --hard` com aprovação manual.
- Não sugerir próximo passo espontaneamente.
- Não pular pré-condições ADR-011.
- Não usar `pwsh`.
- Não esquecer `$alerts = @($alerts)` antes de `.Count` — gotcha PS5.1.
- Não tornar o hook bloqueante — modo warn é parte do design, registrado em `decisoes.md`.
