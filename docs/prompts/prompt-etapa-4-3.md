# Prompt — Etapa 4.3: Hook universal de blank lines em Markdown (terceiro hook funcional, primeiro sob ADR-011)

## Contexto

Camada 3 com 2 hooks funcionais em produção:

- 4.1 (PR #40): Conventional Commits via `commit-msg`, padrão 3 camadas estabelecido.
- 4.2 (PR #41): Encoding UTF-8 via `pre-commit`, padrão orquestrador 1:N estabelecido.
- 4.2.1 (PR #42): ADR-011 formaliza padrões de validação destrutiva. Sub-etapa doc-only.

Esta sub-etapa entrega o **terceiro hook funcional** e o **segundo hook a viver dentro do orquestrador `pre-commit`**. Estreia também:

1. **Primeira aplicação do ADR-011 desde a primeira linha** do roteiro de validação destrutiva. Tarefas incluem `Test-Path` após criação de arquivo, `git status` antes de `git commit`, verificação explícita de `$LASTEXITCODE`, e sincronização de `[System.Environment]::CurrentDirectory` antes de qualquer `[System.IO.File]::WriteAllText` com path relativo. Não é nota lateral — é gate de "pronto".
2. **Validação de regra de formatação Markdown** (não de bytes). Primeira regra que precisa **parsear conteúdo** linha a linha, identificar headers, e validar contexto vizinho.
3. **Padrão "extensão por linha no array `$hooks`"** consolidado. Esta sub-etapa apenas **acrescenta uma linha** ao orquestrador `.githooks/pre-commit.ps1` — sem refatorar nada da 4.2.

A regra implementada: arquivos `.md` no diff staged devem ter **linha em branco antes e depois de cada header de nível 2-6** (`##` até `######`). Headers de nível 1 (`#`) são ignorados (tipicamente título do documento). Fronteira do arquivo (header na primeira ou última linha) é tratada como linha em branco implícita — passa sem reclamar.

Quando esta etapa terminar:

- Qualquer commit local com `.md` contendo header `##`+ sem linha em branco adjacente **bloqueia**.
- Arquivos `.md` que não modificam headers passam silenciosamente.
- Arquivos não-`.md` no diff são ignorados.
- Override `--no-verify` continua válido.
- Camada 3 fecha o lote universal de Markdown — restará apenas tamanho de docs (4.4) como sub-etapa universal antes de partir para hooks de stack (Maven, Java) ou para subagents.

## Padrões que estreiam nesta etapa

1. **Terceiro hook funcional do projeto.**
2. **Segundo hook no orquestrador `pre-commit`** — provando que o padrão 1:N da 4.2 se sustenta com extensão mínima.
3. **Primeira regra que parseia conteúdo** (linha a linha), não apenas bytes (encoding).
4. **Primeira aplicação do ADR-011** desde a redação do prompt. Roteiro inclui pré-condições explícitas em cada cenário destrutivo. Sem isso, regrediríamos ao padrão "comando rodou sem erro" da 4.2.
5. **Validação destrutiva com 7 cenários** — mais cenários que a 4.1 (4) e 4.2 (5), refletindo as bordas extras de uma regra de formatação.

## Escopo decidido (calibrado com operador antes da redação)

### Regra implementada

**Alvo:** apenas arquivos `.md` (não `.markdown`, não `.mdx`).

**Aplicação:** qualquer pasta (não restrito a `docs/`).

**Headers validados:** `##` (nível 2) até `######` (nível 6). Header `#` (nível 1) é **ignorado** — tipicamente título do documento, frequentemente na primeira linha.

**Regra:**

1. Header `##` a `######` deve ter **linha em branco antes** (linha anterior vazia ou apenas espaços/tabs).
2. Header `##` a `######` deve ter **linha em branco depois** (linha seguinte vazia ou apenas espaços/tabs).
3. **Fronteira do arquivo é linha em branco implícita:**
   - Header na primeira linha não-vazia → não há "linha antes" a validar. Passa.
   - Header na última linha não-vazia → não há "linha depois" a validar. Passa.
4. **Headers dentro de blocos de código (```` ``` ````)** são ignorados. Não são headers Markdown, são exemplos.

**Filtro do diff:** `git diff --cached --name-only --diff-filter=ACM` (mesmo padrão da 4.2). Apenas arquivos `.md` no diff são validados — passa também por filtro de extensão dentro do hook.

**Comportamento ao detectar:** lista cada violação com arquivo + linha + tipo (antes/depois) + 1 linha de contexto. Bloqueia commit.

**Override consciente:** `git commit --no-verify` (padrão já consolidado).

### Arquivos criados e modificados

```
.claude/hooks/universal/markdown-blank-lines.ps1   ← novo (lógica real)
.githooks/pre-commit.ps1                            ← edição (adiciona 1 linha ao array $hooks)
docs/decisoes.md                                    ← edição (subseção da 4.3)
docs/hooks-pendentes.md                             ← edição (move item Markdown para Implementados + data)
docs/progresso.md                                   ← edição (lições + sub-etapa + histórico)
docs/prompt-etapa-4-3.md                            ← novo (este próprio prompt)
```

**Não tocar:**

- `.claude/hooks/universal/conventional-commits.ps1` (hook da 4.1).
- `.claude/hooks/universal/encoding-utf8.ps1` (hook da 4.2).
- `.githooks/commit-msg`, `.githooks/commit-msg.ps1` (entrypoints da 4.1).
- `.githooks/pre-commit` (entrypoint bash da 4.2 — só o `.ps1` companheiro muda).
- `.githooks/README.md` (já documenta padrão orquestrador desde 4.2 — não precisa de ajuste).
- ADRs.

### Conteúdo de `.claude/hooks/universal/markdown-blank-lines.ps1` (lógica real)

```powershell
$ErrorActionPreference = "Stop"

function Test-IsHeaderLine {
    param([string]$Line)

    # Header Markdown de nivel 2 a 6: ##, ###, ####, #####, ######
    # Seguido de espaco e conteudo. Permite indentacao com espacos antes (alguns editores).
    return ($Line -match '^\s{0,3}#{2,6}\s+\S')
}

function Test-IsBlankLine {
    param([string]$Line)

    # Linha em branco: vazia ou apenas espacos/tabs
    return ($Line -match '^\s*$')
}

function Get-MarkdownViolations {
    param([string]$FilePath)

    $lines = [System.IO.File]::ReadAllLines($FilePath, [System.Text.UTF8Encoding]::new($false))
    if ($null -eq $lines -or $lines.Count -eq 0) { return @() }

    $violations = @()
    $inCodeBlock = $false

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]

        # Toggle de bloco de codigo (linha que comeca com ```)
        if ($line -match '^\s{0,3}```') {
            $inCodeBlock = -not $inCodeBlock
            continue
        }

        if ($inCodeBlock) { continue }

        if (-not (Test-IsHeaderLine $line)) { continue }

        # Validar linha anterior (se existir)
        if ($i -gt 0) {
            $prevLine = $lines[$i - 1]
            if (-not (Test-IsBlankLine $prevLine)) {
                $violations += [PSCustomObject]@{
                    File = $FilePath
                    Line = $i + 1  # 1-indexed para humanos
                    Type = 'sem linha em branco ANTES'
                    Header = $line.TrimEnd()
                }
            }
        }
        # Se $i -eq 0 (header na primeira linha), passa por fronteira implicita.

        # Validar linha seguinte (se existir)
        if ($i -lt $lines.Count - 1) {
            $nextLine = $lines[$i + 1]
            if (-not (Test-IsBlankLine $nextLine)) {
                $violations += [PSCustomObject]@{
                    File = $FilePath
                    Line = $i + 1
                    Type = 'sem linha em branco DEPOIS'
                    Header = $line.TrimEnd()
                }
            }
        }
        # Se $i -eq $lines.Count - 1 (header na ultima linha), passa por fronteira implicita.
    }

    return $violations
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

$allViolations = @()

foreach ($file in $stagedFiles) {
    if (-not (Test-Path $file)) { continue }

    $ext = [System.IO.Path]::GetExtension($file).ToLower()
    if ($ext -ne '.md') { continue }

    $violations = Get-MarkdownViolations -FilePath $file
    if ($violations.Count -gt 0) {
        $allViolations += $violations
    }
}

if ($allViolations.Count -gt 0) {
    Write-Host ""
    Write-Host "[ERRO] Validacao de blank lines em Markdown falhou em $($allViolations.Count) ocorrencia(s):" -ForegroundColor Red
    Write-Host ""
    foreach ($v in $allViolations) {
        Write-Host "  $($v.File):$($v.Line)  ($($v.Type))" -ForegroundColor Yellow
        Write-Host "    > $($v.Header)" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "Regra:" -ForegroundColor Cyan
    Write-Host "  - Headers Markdown de nivel 2-6 (##, ###, ####, #####, ######) devem ter linha em branco antes E depois." -ForegroundColor Cyan
    Write-Host "  - Headers de nivel 1 (#) sao ignorados (tipicamente titulo do documento)." -ForegroundColor Cyan
    Write-Host "  - Header na primeira linha do arquivo nao precisa de linha em branco antes (fronteira implicita)." -ForegroundColor Cyan
    Write-Host "  - Header na ultima linha do arquivo nao precisa de linha em branco depois (fronteira implicita)." -ForegroundColor Cyan
    Write-Host "  - Headers dentro de blocos de codigo sao ignorados." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Como corrigir:" -ForegroundColor Cyan
    Write-Host "  - Editar o arquivo .md e inserir linha em branco onde apontado." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Override consciente em emergencia: git commit --no-verify" -ForegroundColor Yellow
    Write-Host "(documentar uso em PR body conforme decisoes.md)" -ForegroundColor Yellow
    exit 1
}

exit 0
```

**Notas críticas:**

1. **Sem acentos** nas mensagens.
2. **Encoding UTF-8 sem BOM** no próprio arquivo (hook da 4.2 ativo vai rejeitar se houver BOM).
3. **`Write-Host` em vez de `Write-Error`** (padrão consolidado).
4. **Suspensão local de `$ErrorActionPreference`** ao chamar `git diff --cached` (lição 2.6.2).
5. **Indentação de até 3 espaços antes de `##`** (alguns editores formatam assim). Documentado no regex `^\s{0,3}#{2,6}\s+\S`.
6. **Blocos de código com `` ``` ``** ignorados — `inCodeBlock` toggle simples. Não cobre blocos indentados (com 4 espaços) — limitação consciente; raro em práticas modernas.
7. **`[System.Text.UTF8Encoding]::new($false)`** lê sem assumir BOM — robusto independente do BOM no arquivo.
8. **Sem dependências externas** — só .NET nativo.

### Edição em `.githooks/pre-commit.ps1` (adiciona 1 linha ao array `$hooks`)

**Estado atual da pasta** (do PR #41):

```powershell
$hooks = @(
    (Join-Path $repoRoot ".claude\hooks\universal\encoding-utf8.ps1")
)
```

**Estado após esta etapa:**

```powershell
$hooks = @(
    (Join-Path $repoRoot ".claude\hooks\universal\encoding-utf8.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\markdown-blank-lines.ps1")
)
```

**Apenas essa linha muda.** Resto do arquivo inalterado.

### Atualização de `docs/decisoes.md`

Adicionar nova subseção sob "Camada 3 — Configuração do Claude Code", **após** "Padroes de validacao destrutiva (Sub-etapa 4.2.1)" e **antes** de "Claude Code hooks nativos":

```markdown
### Blank lines em Markdown (Sub-etapa 4.3)

**Regra:** arquivos `.md` staged devem ter linha em branco antes E depois de cada header de nivel 2-6 (`##` ate `######`). Headers de nivel 1 (`#`) sao ignorados (tipicamente titulo do documento).

**Escopo:** apenas `.md` (nao `.markdown`, nao `.mdx`). Qualquer pasta (nao restrito a `docs/`).

**Fronteira do arquivo e linha em branco implicita:** header na primeira linha nao precisa de linha em branco antes. Header na ultima linha nao precisa de linha em branco depois.

**Headers dentro de blocos de codigo (`` ``` ``)** sao ignorados — sao exemplos, nao headers reais. Blocos indentados com 4 espacos nao sao cobertos (limitacao consciente; raro em pratica moderna).

**Hook implementado em:** `.claude/hooks/universal/markdown-blank-lines.ps1`, segundo hook a viver dentro do orquestrador `pre-commit` (4.2). Apenas uma linha adicionada ao array `$hooks` em `.githooks/pre-commit.ps1` — sem refatorar arquitetura.
```

Adicionar entrada no histórico (final do arquivo):

```markdown
- **2026-MM-DD** — Sub-etapa 4.3 concluida: terceiro hook funcional. Markdown blank lines validado em `pre-commit`. Regra: headers nivel 2-6 exigem linha em branco antes e depois; nivel 1 ignorado; fronteira do arquivo e linha em branco implicita; blocos de codigo sao ignorados. Segundo hook no orquestrador 1:N — extensao trivial por linha no array `$hooks` (sem refatoracao). Primeira aplicacao de ADR-011 desde a redacao do prompt — 7 cenarios destrutivos validados com pre-condicoes explicitas (`Test-Path`, `git status`, sincronizacao de `Environment.CurrentDirectory`). Mergeado via PR #XX.
```

### Atualização de `docs/hooks-pendentes.md`

**Operação A** — Remover linha do item de Markdown da seção "Hooks Markdown / docs":

```markdown
- **Linhas em branco em Markdown.** (Etapa 1.1) Validar que arquivos `.md` modificados têm linhas em branco antes e depois de headers (`##`, `###`). Sem isso, alguns renderers não reconhecem o header.
```

**Operação B** — Adicionar entrada em "Hooks implementados":

```markdown
- **Blank lines em Markdown** (Sub-etapa 4.3, PR #XX). Implementado em `.claude/hooks/universal/markdown-blank-lines.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Valida headers de nivel 2-6 em arquivos `.md` (qualquer pasta). Nivel 1 ignorado; fronteira do arquivo e linha em branco implicita; blocos de codigo sao ignorados.
```

**Operação C** — Atualizar data:

```markdown
**Última atualização:** 2026-MM-DD (Sub-etapa 4.3 — Markdown blank lines implementado)
```

### Atualização de `docs/progresso.md`

**A.** Atualizar "Última atualização": `2026-MM-DD (Sub-etapa 4.3 — Markdown blank lines implementado)`.

**B.** Em "Sub-etapas concluídas" (em ordem cronológica, após 4.2.1):

```markdown
- **4.3 — Hook universal de Markdown blank lines** (2026-MM-DD): terceiro hook funcional. Segundo hook no orquestrador `pre-commit` (1:N da 4.2). Valida headers `##`-`######` em arquivos `.md` (qualquer pasta). Fronteira de arquivo e blocos de codigo isentos. Primeira aplicacao de ADR-011 desde a redacao do prompt — 7 cenarios destrutivos com `Test-Path` + `git status` + sincronizacao de `Environment.CurrentDirectory` em cada um. PR #XX.
```

**C.** Adicionar seção "Lições da Sub-etapa 4.3":

```markdown
## Licoes da Sub-etapa 4.3

### Candidatos a hook (automatizar em etapas futuras)

(A preencher se houver durante execucao.)

### Licoes de ambiente

(A preencher se houver durante execucao. Esperado pelo menos: padrao "extensao do orquestrador 1:N por linha" validado em pratica — sub-etapas seguintes que adicionarem hooks a `pre-commit` farao a mesma operacao trivial.)
```

**D.** Histórico geral:

```markdown
- **2026-MM-DD** — Sub-etapa 4.3 concluida: terceiro hook funcional. Markdown blank lines ativo via `pre-commit`. Segundo hook no orquestrador 1:N. Primeira sub-etapa a aplicar ADR-011 desde a redacao do prompt. Mergeado via PR #XX.
```

### Versionar este próprio prompt

`docs/prompt-etapa-4-3.md`.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra squash da 4.2.1 (PR #42) ou superior.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-3.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `.claude/hooks/universal/` contém **apenas** `conventional-commits.ps1` e `encoding-utf8.ps1` (sem `markdown-blank-lines.ps1`).
- `.githooks/` contém `README.md`, `commit-msg`, `commit-msg.ps1`, `pre-commit`, `pre-commit.ps1`.

**Pré-requisitos de ambiente (lições da 4.1, 4.2, 4.2.1):**

- `powershell` (Windows PowerShell 5.1) disponível. **NÃO usar `pwsh`.**
- Git Bash disponível.
- **`[System.Environment]::CurrentDirectory` deve ser sincronizado com `$PWD` antes de qualquer `[System.IO.File]::WriteAllText` com path relativo** (ADR-011). Aplicado nas Tarefas 11+ desta sub-etapa.

Validar com:

```powershell
git status
git log --oneline -3
Test-Path docs\prompt-etapa-4-3.md
git config core.hooksPath
Get-ChildItem .claude\hooks\universal\
Get-ChildItem .githooks\
powershell -Command "Write-Host 'powershell available'"
```

**Pré-condições explícitas (ADR-011):**

- `Test-Path docs\prompt-etapa-4-3.md` deve retornar `True`.
- `git status` deve mostrar apenas o prompt como untracked.
- `.claude/hooks/universal/` deve listar exatamente 2 `.ps1` (conventional-commits, encoding-utf8).

Se algum item divergir, **parar e reportar**.

## Tarefas

### Tarefa 1 — Validar pré-requisitos

Rodar comandos da seção "Estado esperado ao iniciar". Confirmar cada saída explicitamente. Se divergir, parar e reportar.

### Tarefa 2 — Sincronizar `Environment.CurrentDirectory` (ADR-011)

Antes de qualquer outra coisa, no console PowerShell que vai executar esta sub-etapa:

```powershell
[System.Environment]::CurrentDirectory = (Get-Location).Path
[System.Environment]::CurrentDirectory
```

**Esperado:** segunda linha retorna o caminho do repo (`C:\projetos\financas-lab` ou equivalente). Se retornar outro caminho (ex: `C:\Users\...`), o `cd` foi feito mas `Environment.CurrentDirectory` não foi atualizado — re-executar a primeira linha.

**Crítico:** sem este passo, validação destrutiva pode produzir falso positivo silencioso (lição da 4.2.1, ADR-011).

### Tarefa 3 — Criar branch

```bash
git checkout -b feat/etapa-4-3-markdown-blank-lines-hook
```

### Tarefa 4 — Antes de editar, ler arquivos vivos

```bash
cat .claude/hooks/universal/encoding-utf8.ps1
cat .githooks/pre-commit.ps1
cat docs/decisoes.md
cat docs/hooks-pendentes.md
cat docs/progresso.md
cat docs/adrs.md
```

**Confirmar especialmente:**

- `encoding-utf8.ps1` (referência de estilo de hook universal — replicar consistência).
- `pre-commit.ps1` (orquestrador) tem array `$hooks` com 1 entrada atual (`encoding-utf8.ps1`).
- `decisoes.md` tem subseções "Padrao orquestrador 1:N para `pre-commit` (Sub-etapa 4.2)" → "Padroes de validacao destrutiva (Sub-etapa 4.2.1)" → "Claude Code hooks nativos". A nova "Blank lines em Markdown (Sub-etapa 4.3)" entra **entre 4.2.1 e Claude Code hooks nativos**.
- `hooks-pendentes.md` tem item Markdown blank lines em "Hooks Markdown / docs" — será removido.
- `progresso.md` tem "Sub-etapas concluídas" em ordem 4.0 → 4.0.1 → 4.1 → 4.2 → 4.2.1.
- ADR-011 em `adrs.md` (referência para o roteiro de validação destrutiva).

Se alguma divergência, **parar e reportar**.

### Tarefa 5 — Criar hook universal `.claude/hooks/universal/markdown-blank-lines.ps1`

Conteúdo conforme escopo decidido. **UTF-8 sem BOM.** Sem acentos.

**Validação imediata do próprio arquivo:**

```powershell
$bytes = [System.IO.File]::ReadAllBytes(".claude/hooks/universal/markdown-blank-lines.ps1")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: bytes que NAO sejam 239, 187, 191
```

Se aparecer `239, 187, 191`, BOM presente — corrigir antes de seguir.

### Tarefa 6 — Editar `.githooks/pre-commit.ps1`

Adicionar **uma linha** ao array `$hooks`. Resultado final:

```powershell
$hooks = @(
    (Join-Path $repoRoot ".claude\hooks\universal\encoding-utf8.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\markdown-blank-lines.ps1")
)
```

Nada mais muda. Validar com `git diff .githooks/pre-commit.ps1` — deve mostrar **exatamente 1 linha adicionada**.

### Tarefa 7 — Commit 1 (hook lógica)

```bash
git add .claude/hooks/universal/markdown-blank-lines.ps1
git status
```

**Pré-condição ADR-011:** `git status` deve mostrar 1 arquivo staged (`.claude/hooks/universal/markdown-blank-lines.ps1`). Se não, parar.

Hook de encoding UTF-8 (ativo) vai validar este `.md`?... Não — é `.ps1`. Hook valida `.ps1` (whitelist da 4.2). Encoding UTF-8 sem BOM esperado.

```bash
git commit -m "feat(claude): adiciona hook universal de markdown blank lines"
```

**Pré-condição ADR-011:** verificar `$LASTEXITCODE` deve ser `0`. Se não, hook de encoding rejeitou — investigar.

### Tarefa 8 — Commit 2 (orquestrador estendido)

```bash
git add .githooks/pre-commit.ps1
git status
```

**Pré-condição ADR-011:** `git status` deve mostrar 1 arquivo staged.

```bash
git commit -m "feat(githooks): adiciona markdown blank lines ao orquestrador pre-commit"
```

**Pré-condição ADR-011:** `$LASTEXITCODE` deve ser `0`.

**Importante:** a partir deste commit, o hook `markdown-blank-lines` está **ativo no `pre-commit`** local (porque o orquestrador agora inclui ele). Próximos commits passam por esse hook também.

### Tarefa 9 — Pré-commit dos docs (preview da auto-validação)

Antes de commitar `decisoes.md`, `hooks-pendentes.md`, `progresso.md`, validar manualmente que os headers nesses arquivos seguem a regra recém-implementada — caso contrário o hook vai rejeitar o próprio commit que documenta o hook.

```powershell
& .\.claude\hooks\universal\markdown-blank-lines.ps1
```

(Sem arquivos staged, hook sai com `exit 0` silenciosamente.)

Para testar contra os 3 docs editados, primeiro fazer staging:

```bash
git add docs/decisoes.md docs/hooks-pendentes.md docs/progresso.md
```

**Pré-condição ADR-011:** `git status` deve mostrar 3 arquivos staged.

Executar hook diretamente (sem commit):

```powershell
& .\.claude\hooks\universal\markdown-blank-lines.ps1
$hookResult = $LASTEXITCODE
Write-Host "Hook exit code: $hookResult"
```

**Esperado:** `0` (sem violações). Se aparecer violação, corrigir o `.md` ofensor antes de commitar.

### Tarefa 10 — Commit 3 (docs decisoes + hooks-pendentes)

```bash
git status
git commit -m "docs: registra blank lines em markdown como hook implementado"
```

**Pré-condições ADR-011:** `git status` deve mostrar 2 staged (`decisoes.md`, `hooks-pendentes.md`); `$LASTEXITCODE` após commit deve ser `0`.

Se algum dos 3 docs foi automaticamente "destaged" (porque o commit anterior já tinha staged docs/progresso.md junto), refazer staging seletivo.

**Atenção:** a Tarefa 9 fez stage dos 3 docs juntos para validar via hook direto. Aqui queremos commitar apenas 2 docs neste commit (`decisoes.md` + `hooks-pendentes.md`) e deixar `progresso.md` para o Commit 4 junto com o prompt versionado. Operacionalmente:

```bash
git reset HEAD docs/progresso.md
git status  # deve mostrar decisoes.md e hooks-pendentes.md staged; progresso.md modified mas unstaged
git commit -m "docs: registra blank lines em markdown como hook implementado"
```

### Tarefa 11 — Commit 4 (progresso + prompt versionado)

```bash
git add docs/progresso.md docs/prompt-etapa-4-3.md
git status
```

**Pré-condição ADR-011:** `git status` deve mostrar 2 staged.

```bash
git commit -m "docs: registra sub-etapa 4.3 em progresso e versiona prompt"
```

**Pré-condição ADR-011:** `$LASTEXITCODE` deve ser `0`.

### Tarefa 12 — Validação destrutiva sob ADR-011 (7 cenários)

**Pré-condição global:** confirmar antes de iniciar:

```powershell
[System.Environment]::CurrentDirectory  # deve ser o caminho do repo
(Get-Location).Path                      # mesmo caminho
git status                               # working tree limpo
git log --oneline -1                     # deve ser o Commit 4
```

Se algum não bater, **parar**.

#### Cenário 1: `.md` válido (header com linhas em branco antes e depois)

```powershell
$repoRoot = (Get-Location).Path

$conteudo = @"
# Titulo principal

Texto inicial.

## Header valido

Conteudo apos header.
"@

[System.IO.File]::WriteAllText("$repoRoot\test-valid.md", $conteudo, (New-Object System.Text.UTF8Encoding $false))

# Pre-condicao ADR-011
Test-Path .\test-valid.md  # esperado: True

git add test-valid.md
git status                 # esperado: test-valid.md staged

git commit -m "test: validacao destrutiva cenario 1 markdown valido"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 1 exit code: $exitCode (esperado: 0)"
```

**Esperado:** commit aceito (`exitCode = 0`).

#### Cenário 2: `.md` com header sem linha em branco ANTES

```powershell
$conteudoSemBlankAntes = @"
# Titulo principal
Texto que toca header.
## Header sem blank antes

Texto seguinte.
"@

[System.IO.File]::WriteAllText("$repoRoot\test-sem-blank-antes.md", $conteudoSemBlankAntes, (New-Object System.Text.UTF8Encoding $false))

Test-Path .\test-sem-blank-antes.md  # True

git add test-sem-blank-antes.md
git status                            # 1 staged

git commit -m "test: validacao destrutiva cenario 2 sem blank antes"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 2 exit code: $exitCode (esperado: != 0)"
```

**Esperado:** commit **rejeitado**. Output do hook reporta `test-sem-blank-antes.md:3 (sem linha em branco ANTES)`. `$LASTEXITCODE != 0`.

Se aceito, parar — hook não está identificando violação.

#### Cenário 3: `.md` com header sem linha em branco DEPOIS

```powershell
$conteudoSemBlankDepois = @"
# Titulo principal

Texto inicial.

## Header sem blank depois
Texto imediato sem espaco.
"@

[System.IO.File]::WriteAllText("$repoRoot\test-sem-blank-depois.md", $conteudoSemBlankDepois, (New-Object System.Text.UTF8Encoding $false))

Test-Path .\test-sem-blank-depois.md

git add test-sem-blank-depois.md
git status

git commit -m "test: validacao destrutiva cenario 3 sem blank depois"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 3 exit code: $exitCode (esperado: != 0)"
```

**Esperado:** rejeitado. Reporta violação. Hook **também reporta** `test-sem-blank-antes.md` ainda staged (do cenário 2). Output legível.

#### Cenário 4: header na primeira linha do arquivo (fronteira implícita)

```powershell
$conteudoHeaderNoTopo = @"
## Header na primeira linha

Conteudo depois.
"@

[System.IO.File]::WriteAllText("$repoRoot\test-header-topo.md", $conteudoHeaderNoTopo, (New-Object System.Text.UTF8Encoding $false))

Test-Path .\test-header-topo.md

# Limpar staging anterior (cenarios 2 e 3 estao staged)
git reset HEAD test-sem-blank-antes.md test-sem-blank-depois.md
git add test-header-topo.md
git status  # apenas test-header-topo.md staged

git commit -m "test: validacao destrutiva cenario 4 header no topo do arquivo"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 4 exit code: $exitCode (esperado: 0)"
```

**Esperado:** aceito. Header `##` na primeira linha não-vazia → fronteira implícita.

#### Cenário 5: `.md` no commit mas SEM modificação de header

```powershell
# Modificar test-valid.md sem mexer em header
Add-Content -Path "$repoRoot\test-valid.md" -Value "`nMais texto adicionado sem novos headers."

git add test-valid.md
git status

git commit -m "test: validacao destrutiva cenario 5 sem mudanca em header"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 5 exit code: $exitCode (esperado: 0)"
```

**Esperado:** aceito. O hook **valida o arquivo inteiro** (não só o diff), mas como os headers existentes seguem regra, passa.

**Observação importante:** o hook desta sub-etapa valida o arquivo todo, não só linhas adicionadas no diff. É decisão consciente — validar diff requer parser de unified diff (complexidade alta). Validar arquivo inteiro tem efeito colateral positivo: arquivos pre-existentes que violem regra serão flagrados na primeira vez que entrarem em diff. Limitação aceita: arquivos `.md` antigos com violações **não** disparam o hook se não forem tocados.

#### Cenário 6: `.java` no diff (não `.md`)

```powershell
[System.IO.File]::WriteAllText("$repoRoot\test-foo.java", "public class Foo {}", (New-Object System.Text.UTF8Encoding $false))

Test-Path .\test-foo.java

git add test-foo.java
git status

git commit -m "test: validacao destrutiva cenario 6 java fora do escopo md"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 6 exit code: $exitCode (esperado: 0)"
```

**Esperado:** aceito. `.java` não é `.md` — ignorado pelo hook desta sub-etapa.

(Hook de encoding UTF-8 da 4.2 também valida — espera-se UTF-8 válido, que o conteúdo trivial respeita.)

#### Cenário 7: override `--no-verify` bypassa

Re-stage de arquivos do cenário 2 (violação clara):

```powershell
git add test-sem-blank-antes.md
git status

git commit --no-verify -m "test: validacao destrutiva cenario 7 override no-verify"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 7 exit code: $exitCode (esperado: 0)"
```

**Esperado:** aceito (bypass).

### Tarefa 13 — Limpeza dos commits e arquivos de teste

```bash
git log --oneline -10
```

Esperado (do mais recente):

```
<commit cenario 7>
<commit cenario 6>
<commit cenario 5>
<commit cenario 4>
<commit cenario 1>
<Commit 4: docs progresso + prompt>
<Commit 3: docs decisoes + hooks-pendentes>
<Commit 2: feat(githooks)>
<Commit 1: feat(claude)>
<squash 4.2.1>
```

**5 commits de teste** (cenários 1, 4, 5, 6, 7 — cenários 2 e 3 foram rejeitados, não geraram commit).

```bash
git branch --show-current  # confirmar feat/etapa-4-3-markdown-blank-lines-hook
git reset --hard HEAD~5
```

**ATENÇÃO ADR-011:** o número `5` pode estar errado se algum cenário comportar diferente do esperado. Antes do reset, **contar manualmente** quantos commits de teste estão no `git log` (procurar prefixo `test:`). Se for diferente de 5, ajustar.

Se a sandbox bloquear `git reset --hard`, parar e pedir execução manual ao operador (lição da 4.2).

Limpar arquivos de teste do working tree:

```powershell
Remove-Item test-valid.md, test-sem-blank-antes.md, test-sem-blank-depois.md, test-header-topo.md, test-foo.java -ErrorAction SilentlyContinue
git status
```

**Pré-condição ADR-011:** `git status` deve mostrar working tree limpo. Se aparecer algum `test-*.*` como untracked, deletar manualmente.

### Tarefa 14 — Validação final antes de push

```bash
git status
git log --oneline -5
git config core.hooksPath
```

Esperado:

- Working tree limpo.
- 4 commits novos na branch (Commits 1-4 prescritos).
- `core.hooksPath` retorna `.githooks`.
- Mensagens dos 4 commits em Conventional Commits válido (passaram pelo hook).

**`check.ps1` opcional** — etapa não toca em código Java, mas confirma suite intocada.

## Restrições e freios

1. **Não criar outros hooks.** Apenas Markdown blank lines. Tamanho de docs (4.4) fica para próxima sub-etapa.

2. **Não tocar em outros entrypoints** (`.githooks/commit-msg`, `.githooks/pre-commit`). Apenas `.githooks/pre-commit.ps1` ganha 1 linha no array.

3. **Não modificar hooks existentes** (`conventional-commits.ps1`, `encoding-utf8.ps1`).

4. **Não criar subagents, skills, CLAUDE.md.**

5. **Não tocar em scripts existentes** (`setup.ps1`, `dev.ps1`, etc.).

6. **Não tocar em `src/`, `pom.xml`, `application*.yml`, `frontend/`, `docker-compose.yml`, migrations.**

7. **Não tocar em `.gitignore`, `.gitattributes`, ADRs (incluindo ADR-011 recém-criado).**

8. **Não tocar em `.githooks/README.md`.** Padrão orquestrador já documentado desde 4.2.

9. **Não introduzir dependências externas.** PowerShell puro + .NET nativo.

10. **Encoding UTF-8 sem BOM** no `markdown-blank-lines.ps1` (hook da 4.2 vai rejeitar se houver BOM).

11. **Sem acentos** nas mensagens do hook. Docs `.md` podem ter acentos.

12. **Não usar `Write-Error` + `exit`.** Padrão consolidado.

13. **Não usar `pwsh`.** Apenas `powershell` (consolidado na 4.1).

14. **Pré-condições ADR-011 são gates de "pronto":** `Test-Path` após criação, `git status` antes de commit, `$LASTEXITCODE` após comando que deveria falhar, sincronização de `Environment.CurrentDirectory`. Não opcional — não há "vou pular esta porque parece supérfluo".

15. **`git reset --hard` apenas na branch da etapa.** Sandbox pode bloquear; nesse caso, parar e pedir execução manual ao operador (lição da 4.2).

16. **Validação destrutiva COMPLETA (7 cenários) é gate de "pronto".** Reportar saídas no PR body. Cada cenário deve incluir o `Test-Path` e `git status` da pré-condição.

17. **Não tomar decisão silenciosa em zona limítrofe.** Se algum cenário comportar diferente do esperado, parar e reportar — em particular, se algum cenário inesperadamente passar quando deveria bloquear (falso negativo), investigar com `git diff --cached` e execução direta do hook antes de seguir.

18. **Não sugerir próxima etapa espontaneamente.**

19. **Antes de escrever cada arquivo, ler arquivo vivo** (Tarefa 4).

20. **Hook deve passar pela sua própria regra:** se o `markdown-blank-lines.ps1` validasse `.ps1`, seria auto-recursivo. Como valida apenas `.md`, o próprio arquivo é exempto. Mas os docs editados (`.md`) **devem** passar — Tarefa 9 verifica isso antes do Commit 3.

21. **Ordem cronológica em `progresso.md`:** 4.0 → 4.0.1 → 4.1 → 4.2 → 4.2.1 → 4.3.

22. **Lógica de validação fica em `.claude/hooks/universal/markdown-blank-lines.ps1`.** Orquestrador `pre-commit.ps1` apenas referencia.

23. **Não duplicar lógica** entre cenários destrutivos (todos usam mesmo padrão de pré-condição).

## Estrutura de commits

Branch: `feat/etapa-4-3-markdown-blank-lines-hook`

**Commit 1** — `feat(claude): adiciona hook universal de markdown blank lines`
- `.claude/hooks/universal/markdown-blank-lines.ps1` (novo)

**Commit 2** — `feat(githooks): adiciona markdown blank lines ao orquestrador pre-commit`
- `.githooks/pre-commit.ps1` (1 linha adicionada ao array `$hooks`)

**Commit 3** — `docs: registra blank lines em markdown como hook implementado`
- `docs/decisoes.md`
- `docs/hooks-pendentes.md`

**Commit 4** — `docs: registra sub-etapa 4.3 em progresso e versiona prompt`
- `docs/progresso.md`
- `docs/prompt-etapa-4-3.md`

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
- `core.hooksPath` retorna `.githooks`.

## PR

Título: `feat: sub-etapa 4.3 — hook universal de markdown blank lines (segundo hook no orquestrador pre-commit)`

Body sugerido:

```markdown
## Summary

Implementa o **terceiro hook funcional** do projeto: Markdown blank lines via `pre-commit`. Segundo hook a viver dentro do orquestrador 1:N estabelecido na 4.2 — **extensao trivial por uma linha** no array `$hooks`, sem refatorar nada.

**Primeira sub-etapa a aplicar ADR-011** desde a redacao do prompt. Roteiro de validacao destrutiva inclui `Test-Path`, `git status` explicito, verificacao de `$LASTEXITCODE`, e sincronizacao previa de `[System.Environment]::CurrentDirectory = (Get-Location).Path`.

### Regra implementada

- **Alvo:** arquivos `.md` (nao `.markdown`, nao `.mdx`).
- **Aplicacao:** qualquer pasta (nao restrito a `docs/`).
- **Headers validados:** nivel 2-6 (`##` ate `######`). Nivel 1 (`#`) ignorado.
- **Linha em branco antes E depois** de cada header validado.
- **Fronteira do arquivo e linha em branco implicita** (header na primeira ou ultima linha passa).
- **Blocos de codigo (`` ``` ``)** sao ignorados — headers dentro de blocos sao exemplos, nao headers reais.
- **Override:** `git commit --no-verify` em emergencias.

### Validacao destrutiva sob ADR-011

Sete cenarios executados, cada um com pre-condicoes explicitas (`Test-Path`, `git status`) antes do `git commit`:

1. **`.md` valido** (header com blank antes e depois) — commit aceito.
2. **`.md` sem blank antes do header** — commit rejeitado. Hook reporta `arquivo:linha (sem linha em branco ANTES)`.
3. **`.md` sem blank depois do header** — commit rejeitado.
4. **Header na primeira linha** (fronteira implicita) — commit aceito.
5. **`.md` no diff mas sem mudanca em header** — commit aceito.
6. **`.java` no diff** (nao `.md`) — commit aceito (fora do escopo do hook).
7. **`--no-verify` bypassa** — commit aceito.

Todos passaram conforme esperado.

### Mudancas

- `.claude/hooks/universal/markdown-blank-lines.ps1`: logica real. Le `git diff --cached --name-only --diff-filter=ACM`, filtra `.md`, parseia cada arquivo procurando headers `##`-`######`, valida vizinhos linha-a-linha. Toggle de `inCodeBlock` para ignorar headers dentro de `` ``` ``. Reporta violacoes com `arquivo:linha (tipo)` e contexto da linha do header.
- `.githooks/pre-commit.ps1`: adiciona 1 linha ao array `$hooks` referenciando o novo hook. Sem outras mudancas.
- `docs/decisoes.md`: subsecao "Blank lines em Markdown (Sub-etapa 4.3)" sob "Camada 3". Entrada no historico.
- `docs/hooks-pendentes.md`: item movido para "Hooks implementados". Data atualizada.
- `docs/progresso.md`: sub-etapa 4.3 em "Sub-etapas concluidas". Licoes da 4.3. Entrada no historico.

### Limitacao consciente

Hook valida o **arquivo inteiro**, nao apenas linhas adicionadas no diff. Razao: validar diff requer parser de unified diff (complexidade alta sem ganho proporcional). Efeito colateral positivo: arquivos pre-existentes com violacao serao flagrados na primeira vez que entrarem em diff. Efeito colateral aceito: arquivos `.md` legados com violacoes nao disparam o hook se nao forem tocados — calibracao retroativa fica para sub-etapa futura quando primeiro caso doloroso aparecer.

### Validacao destrutiva pos-merge sugerida

Em qualquer branch (nao main), com `[System.Environment]::CurrentDirectory = (Get-Location).Path` sincronizado:

\```powershell
# Cenario A: .md valido passa
@"
## Header com blank

Conteudo apos.
"@ | Out-File smoke-valid.md  # cuidado: PS5.1 emite BOM por default; usar WriteAllText se relevante

git add smoke-valid.md
git status  # confirmar staged
git commit -m "test: smoke pos merge 4.3 cenario valido"  # aceito

# Cenario B: .md invalido bloqueia
@"
## Header sem blank
Texto imediato.
"@ | Out-File smoke-invalido.md

git add smoke-invalido.md
git status
git commit -m "test: smoke pos merge 4.3 cenario invalido"  # rejeitado

# Limpeza
git reset --hard HEAD~1
Remove-Item smoke-*.md -ErrorAction SilentlyContinue
\```

### Proximo passo

Sub-etapa 4.4 (hook universal de tamanho de docs). Acrescenta mais uma linha ao array `$hooks` do orquestrador, fechando o lote universal de Markdown. Decisao fora deste PR.
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

- Branch `feat/etapa-4-3-markdown-blank-lines-hook` empurrada com 5 commits (4 + 1 update do PR).
- PR aberto, CI verde, **não mergeado**.
- `main` ainda no squash da 4.2.1.
- Working tree limpo.
- Arquivos `test-*.md` e `test-foo.java` removidos.
- Reportar com `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup`, e saídas dos 7 cenários destrutivos **incluindo pré-condições verificadas**.

## O que NÃO fazer ao terminar

- Não mergear o PR.
- Não criar prompt da Sub-etapa 4.4.
- Não criar hook de tamanho de docs.
- Não criar subagents, skills, CLAUDE.md.
- Não tocar em scripts existentes além dos arquivos prescritos.
- Não tocar em arquivos das 4.1, 4.2, 4.2.1.
- Não tocar em `.gitignore`, `.gitattributes`, ADRs.
- Não deixar `test-*.*` na branch.
- Não deixar commits `test:` no histórico — limpar via `git reset --hard` (com aprovação manual se sandbox bloquear).
- Não sugerir próximo passo espontaneamente.
- Não pular pré-condições ADR-011 mesmo que "pareça obvio que vai passar".
- Não usar `pwsh`.
- Não validar arquivos não-`.md` no hook desta sub-etapa.
- Não tentar parsear diff em vez de arquivo inteiro — limitação consciente, escopo futuro.
