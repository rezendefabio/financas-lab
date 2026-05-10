# Prompt — Etapa 4.2: Hook universal de encoding UTF-8 via pre-commit (estreia padrão orquestrador 1:N)

## Contexto

A Sub-etapa 4.1 (PR #40) entregou o primeiro hook funcional do projeto: Conventional Commits via `commit-msg`. Padrão de 3 camadas (entrypoint bash sem extensão → companheiro `.ps1` → hook real em `.claude/hooks/universal/`) provado em produção. Smoke test pós-merge confirmou comportamento em `main`.

**Esta sub-etapa entrega o segundo hook funcional** e introduz três novidades estruturais:

1. **Segundo entrypoint do padrão de 3 camadas:** `pre-commit` (não `commit-msg`).
2. **Primeira validação multi-arquivo:** o hook lê `git diff --cached --name-only --diff-filter=ACM` para iterar sobre arquivos staged em vez de receber um único arquivo de input.
3. **Companheiro `.ps1` evolui para orquestrador 1:N:** diferente do `commit-msg.ps1` da 4.1 que invocava um único hook, o `pre-commit.ps1` é desenhado para invocar **múltiplos hooks em sequência**. Justificativa: sub-etapas 4.3 (Markdown blank lines) e 4.4 (tamanho de docs) também usarão `pre-commit`. Se o companheiro fosse fino 1:1, precisaria refatorar na 4.3.

A regra implementada: **arquivos staged devem ser UTF-8 válido; arquivos `.ps1` adicionalmente NÃO podem ter BOM** (lição da Etapa 2.6). Lista de tipos validados via whitelist por extensão.

Quando esta etapa terminar:

- Qualquer commit local com arquivo `.ps1` contendo BOM **bloqueia**.
- Qualquer commit local com arquivo de texto em encoding inválido (Latin-1, etc.) **bloqueia**.
- Arquivos binários (`.png`, `.jpg`, etc.) e tipos fora da whitelist **passam silenciosamente**.
- Override `--no-verify` continua válido como escape.
- Padrão orquestrador 1:N estabelecido, pronto para 4.3+.

## Padrões que estreiam nesta etapa

1. **Segundo entrypoint do padrão de 3 camadas** — `pre-commit` (não `commit-msg`).
2. **Primeira validação multi-arquivo** via `git diff --cached --name-only --diff-filter=ACM`.
3. **Padrão orquestrador 1:N no companheiro `pre-commit.ps1`** — preparado para receber hooks adicionais em 4.3+.
4. **Whitelist por extensão como critério de aplicabilidade** — primeira aplicação. Sem detecção por conteúdo (`file --mime`); mantém zero dependências externas conforme ADR-009.
5. **Primeira regra com comportamento diferente por tipo** — `.ps1` rejeita BOM, outros aceitam.
6. **Pré-requisito de ambiente declarado explicitamente** na seção "Estado esperado ao iniciar" — primeira aplicação da lição da 4.1 ("prescrição minha assumiu ambiente sem confirmar").

## Escopo decidido (calibrado com operador antes da redação)

### Regra implementada

**Whitelist (arquivos validados):**

```
Extensões: .md, .java, .yml, .yaml, .xml, .properties, .ps1, .sql,
           .ts, .tsx, .js, .jsx, .json, .css, .html

Nomes exatos: .gitignore, .gitattributes, .editorconfig, .env.example

Arquivos sem extensão dentro de .githooks/ (entrypoints bash)
```

**Validação aplicada:**

- **Todos os tipos da whitelist:** devem ser UTF-8 válido (decodificação estrita sem caracteres inválidos).
- **`.ps1`:** adicionalmente NÃO podem ter BOM (bytes `EF BB BF` no início). Razão histórica: lição da Etapa 2.6 — BOM em `.ps1` quebra alguns parsers e pode afetar `javac`/Maven em arquivos vizinhos.
- **Outros tipos da whitelist:** BOM permitido (não rejeita).

**Filtro do diff:** `--diff-filter=ACM` (Added, Copied, Modified). Não valida arquivos Deleted (sem conteúdo) ou Renamed-rename-only (sem mudança de conteúdo).

**Arquivos fora da whitelist:** ignorados silenciosamente. Binários (`.png`, `.jpg`, `.pdf`), tipos não listados (`.toml`, `.lock`, etc.) passam sem validação.

**Override consciente:** `git commit --no-verify` (já documentado em `decisoes.md` na 4.1).

### Arquivos criados e modificados

```
.claude/hooks/universal/encoding-utf8.ps1      ← novo (lógica real)
.githooks/pre-commit                            ← novo (entrypoint bash, sem extensão)
.githooks/pre-commit.ps1                        ← novo (orquestrador 1:N)
.githooks/README.md                             ← edição (documenta padrão orquestrador)
docs/decisoes.md                                ← edição (regra UTF-8 + padrão orquestrador)
docs/hooks-pendentes.md                         ← edição (move item UTF-8 para "Implementados")
docs/progresso.md                               ← edição (lições + sub-etapa + histórico)
docs/prompt-etapa-4-2.md                        ← novo (este próprio prompt)
```

**Não remover `.gitkeep` de `.claude/hooks/universal/`** — já foi removido na 4.1 (primeiro hook). Pasta já tem `conventional-commits.ps1` real.

### Conteúdo de `.githooks/pre-commit` (entrypoint bash, sem extensão)

```bash
#!/usr/bin/env bash
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
powershell -NoProfile -ExecutionPolicy Bypass -File "$SCRIPT_DIR/pre-commit.ps1" "$@"
```

**Notas críticas:**

1. **Sem extensão.** Git invoca exatamente `pre-commit`.
2. **`powershell` (PS5.1), não `pwsh`.** Lição consolidada da 4.1.
3. **Line endings LF** (cuidado do `.gitattributes`).
4. **Bit de execução obrigatório:** `git update-index --chmod=+x .githooks/pre-commit` após `git add`. Validar com `git ls-files --stage` retornando `100755`.
5. **Passa `"$@"`** — embora `pre-commit` não receba argumentos do git por default, manter por consistência com o padrão da 4.1.

### Conteúdo de `.githooks/pre-commit.ps1` (orquestrador 1:N)

```powershell
$ErrorActionPreference = "Stop"

$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$repoRoot = git rev-parse --show-toplevel 2>&1
$ErrorActionPreference = $prev

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERRO] Nao foi possivel identificar o repo root." -ForegroundColor Red
    exit 1
}

# Hooks pre-commit executados em sequencia. Adicionar novos hooks aqui em sub-etapas seguintes
# (4.3 Markdown blank lines, 4.4 tamanho de docs, etc).
$hooks = @(
    (Join-Path $repoRoot ".claude\hooks\universal\encoding-utf8.ps1")
)

$failed = $false
foreach ($hookPath in $hooks) {
    if (-not (Test-Path $hookPath)) {
        Write-Host "[ERRO] Hook nao encontrado: $hookPath" -ForegroundColor Red
        $failed = $true
        continue
    }
    & $hookPath
    if ($LASTEXITCODE -ne 0) {
        $failed = $true
    }
}

if ($failed) { exit 1 }
exit 0
```

**Notas críticas:**

1. **Suspensão local de `$ErrorActionPreference`** ao chamar `git rev-parse` (lição da Etapa 2.6.2). Mesmo padrão aplicado pelo agente em `commit-msg.ps1` da 4.1.
2. **Array `$hooks` é o ponto de extensão.** Sub-etapas 4.3+ acrescentam linhas aqui.
3. **Execução em sequência, não paralela.** Cada hook lê seu próprio `git diff --cached`. Se múltiplos hooks falharem, todos reportam suas mensagens (não early-exit no primeiro fail).
4. **Sem duplicação de lógica de validação no orquestrador.** Lógica fica nos hooks reais.

### Conteúdo de `.claude/hooks/universal/encoding-utf8.ps1` (lógica real)

```powershell
$ErrorActionPreference = "Stop"

function Test-IsWhitelisted {
    param([string]$Path)

    $extensions = @(
        '.md', '.java', '.yml', '.yaml', '.xml', '.properties', '.ps1', '.sql',
        '.ts', '.tsx', '.js', '.jsx', '.json', '.css', '.html'
    )

    $nomesExatos = @(
        '.gitignore', '.gitattributes', '.editorconfig', '.env.example'
    )

    $ext = [System.IO.Path]::GetExtension($Path).ToLower()
    $nome = [System.IO.Path]::GetFileName($Path)

    if ($extensions -contains $ext) { return $true }
    if ($nomesExatos -contains $nome) { return $true }

    # Arquivos sem extensao dentro de .githooks/ (entrypoints bash)
    $pathNorm = $Path -replace '\\', '/'
    if ($pathNorm -like '.githooks/*' -and -not $ext) { return $true }

    return $false
}

function Test-FileEncoding {
    param([string]$Path)

    $bytes = [System.IO.File]::ReadAllBytes($Path)

    if ($bytes.Length -eq 0) {
        return @{ ValidUtf8 = $true; HasBom = $false }
    }

    $hasBom = ($bytes.Length -ge 3 -and `
               $bytes[0] -eq 0xEF -and `
               $bytes[1] -eq 0xBB -and `
               $bytes[2] -eq 0xBF)

    if ($hasBom) {
        $contentBytes = $bytes[3..($bytes.Length - 1)]
    } else {
        $contentBytes = $bytes
    }

    # Decodificacao UTF-8 estrita: lanca excecao se houver sequencia invalida
    try {
        $utf8Strict = New-Object System.Text.UTF8Encoding($false, $true)
        $null = $utf8Strict.GetString($contentBytes)
        return @{ ValidUtf8 = $true; HasBom = $hasBom }
    } catch {
        return @{ ValidUtf8 = $false; HasBom = $hasBom }
    }
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
    # Sem arquivos staged elegiveis - nada a validar
    exit 0
}

$problems = @()

foreach ($file in $stagedFiles) {
    if (-not (Test-Path $file)) { continue }
    if (-not (Test-IsWhitelisted $file)) { continue }

    $check = Test-FileEncoding -Path $file

    if (-not $check.ValidUtf8) {
        $problems += [PSCustomObject]@{
            File = $file
            Problem = "Nao e UTF-8 valido (encoding incorreto, possivelmente Latin-1, Windows-1252, etc)"
        }
        continue
    }

    $ext = [System.IO.Path]::GetExtension($file).ToLower()
    if ($ext -eq '.ps1' -and $check.HasBom) {
        $problems += [PSCustomObject]@{
            File = $file
            Problem = "Arquivo .ps1 com BOM (regra: PowerShell exige UTF-8 sem BOM, licao da Etapa 2.6)"
        }
    }
}

if ($problems.Count -gt 0) {
    Write-Host ""
    Write-Host "[ERRO] Validacao de encoding falhou em $($problems.Count) arquivo(s):" -ForegroundColor Red
    Write-Host ""
    foreach ($p in $problems) {
        Write-Host "  - $($p.File)" -ForegroundColor Yellow
        Write-Host "      $($p.Problem)" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "Regra:" -ForegroundColor Cyan
    Write-Host "  - Arquivos de texto devem ser UTF-8 valido." -ForegroundColor Cyan
    Write-Host "  - Arquivos .ps1 devem ser UTF-8 SEM BOM (licao da Etapa 2.6)." -ForegroundColor Cyan
    Write-Host "  - Outros tipos podem ter ou nao ter BOM (passam quando UTF-8 valido)." -ForegroundColor Cyan
    Write-Host "  - Binarios e tipos fora da whitelist sao ignorados." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Como corrigir:" -ForegroundColor Cyan
    Write-Host "  - Reabrir o arquivo em editor (VSCode, Notepad++, ISE) e salvar como 'UTF-8 sem BOM'." -ForegroundColor Cyan
    Write-Host "  - Em VSCode: clicar no encoding na barra inferior, escolher 'Save with Encoding' > 'UTF-8'." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Override consciente em emergencia: git commit --no-verify" -ForegroundColor Yellow
    Write-Host "(documentar uso em PR body conforme decisoes.md)" -ForegroundColor Yellow
    exit 1
}

exit 0
```

**Notas críticas:**

1. **Sem acentos** nas mensagens.
2. **Encoding UTF-8 sem BOM** no próprio arquivo (auto-validação: o hook precisa passar pela sua própria regra).
3. **`Write-Host` em vez de `Write-Error`** (padrão consolidado).
4. **Suspensão local de `$ErrorActionPreference`** ao chamar `git diff --cached` (comando nativo, lição da 2.6.2).
5. **Filtro `Test-Path $file`** — arquivo pode estar staged mas removido do working tree em casos raros (rename, etc). Skip silencioso.
6. **`Where-Object { $_ -and $_.Trim() }`** elimina strings vazias do output de `git diff --cached`.
7. **Sem dependências externas** — só .NET nativo (`System.IO.File`, `System.Text.UTF8Encoding`).
8. **Decodificação UTF-8 estrita** — `UTF8Encoding($false, $true)` significa "sem BOM, lança exceção em bytes inválidos". É o critério de validade.

### Atualização de `.githooks/README.md`

Adicionar nova seção **após** a seção "Padrão de wrapper" existente:

```markdown

## Padrão de orquestrador (1:N) — para hooks `pre-commit`

Diferente do `commit-msg` (que valida uma única regra por commit), o entrypoint `pre-commit` é projetado para invocar **múltiplos hooks em sequência**. Razão: várias validações distintas (encoding, blank lines em Markdown, tamanho de docs, etc.) precisam rodar antes de cada commit.

O companheiro `.githooks/pre-commit.ps1` itera sobre um array `$hooks` invocando cada um e agrega os exit codes — se qualquer hook falhar, o commit é bloqueado, mas todos os hooks rodam (não há early-exit no primeiro fail). Isso garante que o operador veja todas as violações de uma vez, não uma por commit-tentativa.

Para adicionar um novo hook ao `pre-commit`:

1. Criar `.claude/hooks/universal/<nome>.ps1` (ou `<escopo>/<nome>.ps1`).
2. Acrescentar uma linha ao array `$hooks` em `.githooks/pre-commit.ps1`:

\```powershell
$hooks = @(
    (Join-Path $repoRoot ".claude\hooks\universal\encoding-utf8.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\<novo-hook>.ps1")
)
\```

Cada hook é responsável por ler seu próprio `git diff --cached` e reportar violações com mensagens claras. Não há contrato compartilhado além de "exit 0 = ok, exit != 0 = bloqueia".

```

### Atualização de `docs/decisoes.md`

Adicionar nova subseção sob "Camada 3 — Configuração do Claude Code", **após** "Conventional Commits (Sub-etapa 4.1)" e **antes** de "Claude Code hooks nativos":

```markdown
### Encoding UTF-8 (Sub-etapa 4.2)

**Regra:** arquivos de texto staged devem ser UTF-8 valido. Arquivos `.ps1` adicionalmente NAO podem ter BOM (licao da Etapa 2.6).

**Whitelist por extensao:** `.md`, `.java`, `.yml`, `.yaml`, `.xml`, `.properties`, `.ps1`, `.sql`, `.ts`, `.tsx`, `.js`, `.jsx`, `.json`, `.css`, `.html`.

**Whitelist por nome exato:** `.gitignore`, `.gitattributes`, `.editorconfig`, `.env.example`. Arquivos sem extensao dentro de `.githooks/` (entrypoints bash) tambem incluidos.

**Fora da whitelist:** binarios (`.png`, `.jpg`, `.pdf`) e tipos nao listados (`.toml`, etc) passam silenciosamente. Adicionar item a whitelist quando primeiro caso real surgir — decisao consciente, nao automatica.

**Filtro do diff:** `git diff --cached --name-only --diff-filter=ACM` (Added, Copied, Modified). Deleted e Renamed-rename-only sao ignorados (sem conteudo a validar).

**Sem deteccao por conteudo** (`file --mime` ou similar) — coerente com ADR-009 ("sem dependencias externas, PowerShell puro").

**Hook implementado em:** `.claude/hooks/universal/encoding-utf8.ps1`, invocado por `.githooks/pre-commit` -> `.githooks/pre-commit.ps1` (orquestrador 1:N).

### Padrao orquestrador 1:N para `pre-commit` (Sub-etapa 4.2)

O entrypoint companheiro `.githooks/pre-commit.ps1` e desenhado como **orquestrador**, diferente do `commit-msg.ps1` da 4.1 que e delegador 1:1. Razao: varias validacoes distintas (encoding, blank lines em Markdown, tamanho de docs, etc) precisam rodar antes de cada commit. Array `$hooks` no orquestrador e o ponto de extensao — sub-etapas seguintes da Camada 3 acrescentam linhas a esse array.

**Execucao em sequencia, nao paralela.** Cada hook le seu proprio `git diff --cached`. Se multiplos hooks falharem, todos reportam suas mensagens (sem early-exit no primeiro fail) — operador ve todas as violacoes de uma vez.

**Sem contrato compartilhado entre hooks** alem de: "exit 0 = ok, exit != 0 = bloqueia".
```

Adicionar entrada no histórico (final do arquivo):

```markdown
- **2026-MM-DD** — Sub-etapa 4.2 concluida: segundo hook funcional. Encoding UTF-8 implementado em 3 camadas (entrypoint bash `.githooks/pre-commit` -> orquestrador `.githooks/pre-commit.ps1` -> hook `.claude/hooks/universal/encoding-utf8.ps1`). Whitelist por extensao + nomes exatos. Regra adicional: `.ps1` rejeita BOM. Padrao orquestrador 1:N estabelecido para `pre-commit` (preparado para 4.3+). Validacao destrutiva confirmou 5 cenarios (md valido passa, ps1 com BOM bloqueia, java Latin-1 bloqueia, png ignorado, override --no-verify bypassa). Mergeado via PR #XX.
```

Substituir `2026-MM-DD` pela data real.

### Atualização de `docs/hooks-pendentes.md`

**Operação A** — Remover linha do item Encoding UTF-8 da seção "Hooks Markdown / docs":

```markdown
- **Encoding UTF-8 em arquivos de texto.** (Etapa 1.1) Validar que arquivos criados estão em UTF-8 (sem BOM em scripts `.ps1`; com ou sem BOM em outros).
```

**Operação B** — Adicionar entrada na seção "Hooks implementados" (criada na 4.1):

```markdown
- **Encoding UTF-8** (Sub-etapa 4.2, PR #XX). Implementado em `.claude/hooks/universal/encoding-utf8.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Whitelist por extensao e nome exato. Regra adicional: `.ps1` rejeita BOM (licao 2.6); outros tipos aceitam BOM. Binarios e tipos fora da whitelist sao ignorados.
```

**Operação C** — Atualizar data no topo:

```markdown
**Última atualização:** 2026-MM-DD (Sub-etapa 4.2 — Encoding UTF-8 implementado)
```

### Atualização de `docs/progresso.md`

**A.** Atualizar "Última atualização": `2026-MM-DD (Sub-etapa 4.2 — Encoding UTF-8 implementado)`.

**B.** Na seção "Camada 3 — Configuração do Claude Code", subseção "Sub-etapas concluídas", adicionar (**em ordem cronológica**, após a 4.1):

```markdown
- **4.2 — Hook universal de encoding UTF-8** (2026-MM-DD): segundo hook funcional. Estreia o entrypoint `pre-commit` no padrao de 3 camadas, primeira validacao multi-arquivo via `git diff --cached`, e padrao orquestrador 1:N no companheiro `pre-commit.ps1` (preparado para 4.3+). Whitelist por extensao + nomes exatos. Regra: `.ps1` rejeita BOM (licao 2.6); outros tipos aceitam BOM. 5 cenarios destrutivos validados (md ok, ps1+BOM bloqueia, java Latin-1 bloqueia, png ignorado, --no-verify bypassa). PR #XX.
```

**C.** Adicionar seção "Lições da Sub-etapa 4.2":

```markdown
## Licoes da Sub-etapa 4.2

### Candidatos a hook (automatizar em etapas futuras)

(A preencher se houver durante execucao.)

### Licoes de ambiente

(A preencher se houver durante execucao. Esperado pelo menos: padrao orquestrador 1:N para pre-commit consolidado — sub-etapas 4.3+ apenas acrescentam linhas ao array `$hooks`, sem refatorar arquitetura.)
```

**D.** Adicionar entrada no histórico geral:

```markdown
- **2026-MM-DD** — Sub-etapa 4.2 concluida: segundo hook funcional. Encoding UTF-8 ativo via `pre-commit` hook. Padrao orquestrador 1:N estabelecido (companheiro `pre-commit.ps1` itera sobre array `$hooks` agregando exit codes). Primeira validacao multi-arquivo via `git diff --cached`. Mergeado via PR #XX.
```

### Versionar este próprio prompt

`docs/prompt-etapa-4-2.md`.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra commit `d2b7110` (squash da 4.1) ou superior.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-2.md` presente como **untracked** (operador colocou antes de iniciar).
- Working tree limpo (exceto o prompt).
- `.claude/hooks/universal/` contém **apenas** `conventional-commits.ps1` (sem `.gitkeep`, sem `encoding-utf8.ps1`).
- `.githooks/` contém **apenas** `README.md`, `commit-msg`, `commit-msg.ps1` (sem `pre-commit`, sem `pre-commit.ps1`).

**Pré-requisito de ambiente confirmado** (lição da 4.1):

- `powershell` (Windows PowerShell 5.1) disponível. **NÃO usar `pwsh`** (PowerShell Core 7) — confirmado indisponível neste ambiente.
- Git Bash disponível (vem com Git for Windows).

Validar com:

```bash
git status
git log --oneline -3
ls docs/prompt-etapa-4-2.md
git config core.hooksPath
ls .claude/hooks/universal/
ls .githooks/
powershell -Command "Write-Host 'powershell available'"
```

Se algum item divergir, **parar e reportar**.

## Tarefas

### Tarefa 1 — Validar pré-requisitos

Rodar comandos da seção "Estado esperado ao iniciar". Se algum item divergir, parar e reportar.

### Tarefa 2 — Criar branch de trabalho

```bash
git checkout -b feat/etapa-4-2-encoding-utf8-hook
```

### Tarefa 3 — Antes de escrever, ler arquivos vivos

```bash
cat .githooks/README.md
cat .githooks/commit-msg
cat .githooks/commit-msg.ps1
cat .claude/hooks/universal/conventional-commits.ps1
cat docs/decisoes.md
cat docs/hooks-pendentes.md
cat docs/progresso.md
cat .gitattributes
```

**Confirmar especialmente:**

- `.githooks/commit-msg.ps1` (companheiro da 4.1) mostra padrão consolidado de descobrir `repo root` + delegar. Replicar consistência no orquestrador da 4.2.
- `.claude/hooks/universal/conventional-commits.ps1` mostra estilo de mensagens, formatação de erros, padrão de `exit 0`/`exit 1`.
- `decisoes.md` tem subseção "Conventional Commits (Sub-etapa 4.1)" e subseção "Claude Code hooks nativos" — a nova "Encoding UTF-8" entra **entre** essas duas.

Se alguma divergência, **parar e reportar**.

### Tarefa 4 — Criar hook universal `.claude/hooks/universal/encoding-utf8.ps1`

Conteúdo conforme seção "Conteúdo de `.claude/hooks/universal/encoding-utf8.ps1`". Encoding UTF-8 sem BOM. Sem acentos nas mensagens.

**Validação manual do próprio arquivo:** após criar, rodar:

```powershell
$bytes = [System.IO.File]::ReadAllBytes(".claude/hooks/universal/encoding-utf8.ps1")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: bytes que NAO sejam 239, 187, 191 (que seria BOM)
```

Se aparecer `239, 187, 191`, o arquivo tem BOM — corrigir reabrindo em editor e salvando como UTF-8 sem BOM.

### Tarefa 5 — Criar orquestrador `.githooks/pre-commit.ps1`

Conteúdo conforme seção "Conteúdo de `.githooks/pre-commit.ps1`". Encoding UTF-8 sem BOM. Sem acentos.

**Validação manual:** mesmo procedimento da Tarefa 4 (verificar primeiros 3 bytes ≠ BOM).

### Tarefa 6 — Criar entrypoint `.githooks/pre-commit` (sem extensão)

Conteúdo conforme seção "Conteúdo de `.githooks/pre-commit`".

**Critérios técnicos críticos (mesmos da 4.1):**

1. **Nome sem extensão.** Exatamente `pre-commit`.
2. **Line endings LF.** Validar com `file .githooks/pre-commit` após criação.
3. **Bit de execução no git index:**

```bash
git add .githooks/pre-commit
git update-index --chmod=+x .githooks/pre-commit
git ls-files --stage .githooks/pre-commit
```

Esperado: saída começa com `100755`.

### Tarefa 7 — Atualizar `.githooks/README.md`

Adicionar seção "Padrão de orquestrador (1:N)" conforme escopo decidido. Inserir **após** a seção "Padrão de wrapper" existente, **antes** de qualquer seção subsequente.

### Tarefa 8 — Stage de tudo

```bash
git add .claude/hooks/universal/encoding-utf8.ps1
git add .githooks/pre-commit.ps1
git add .githooks/README.md
# .githooks/pre-commit ja foi adicionado na Tarefa 6 com chmod +x
git status
```

Esperado: 4 arquivos staged (3 novos + 1 modificado).

### Tarefa 9 — Commit 1 (hook lógica)

```bash
git commit -m "feat(claude): adiciona hook universal de encoding utf-8"
```

**Observação:** neste momento, o hook `pre-commit` ainda **não está commitado** — git não invoca o orquestrador. O Conventional Commits (`commit-msg`) está ativo e valida esta mensagem. Esperado: commit aceito.

Espera, vai precisar fazer commits em ordem específica. Reorganizar:

**Plano corrigido:**

```bash
# Stage apenas o hook real primeiro
git add .claude/hooks/universal/encoding-utf8.ps1
git status
# Esperado: 1 arquivo staged
git commit -m "feat(claude): adiciona hook universal de encoding utf-8"
# Hook real commitado. pre-commit ainda nao existe -> git nao invoca.

# Agora stage do entrypoint + orquestrador + README
git add .githooks/pre-commit
git update-index --chmod=+x .githooks/pre-commit
git add .githooks/pre-commit.ps1
git add .githooks/README.md
git status
# Esperado: 3 arquivos staged
git commit -m "feat(githooks): adiciona entrypoint pre-commit + orquestrador powershell"
# pre-commit agora existe -> git invoca o hook na hora do commit.
# Hook valida os 3 arquivos staged. Todos devem ser UTF-8 sem BOM.
# .githooks/pre-commit (sem extensao): no .githooks/, passa whitelist.
# .githooks/pre-commit.ps1: validado, deve estar sem BOM.
# .githooks/README.md: validado, BOM ok.
# Esperado: commit aceito.
```

### Tarefa 10 — Validação destrutiva (5 cenários)

**A partir deste ponto, os commits "reais" estão feitos. O hook está ativo. Próximos commits são destrutivos para validar comportamento. Serão revertidos na Tarefa 11.**

#### Cenário 1+4 (combinados): arquivos válidos — `.md` UTF-8 + `.png` binário

```powershell
# Criar .md UTF-8 sem BOM
"# Teste markdown UTF-8 sem BOM" | Out-File -Encoding utf8NoBOM test-utf8.md
# Se utf8NoBOM nao for suportado (PS5.1), usar:
[System.IO.File]::WriteAllText("test-utf8.md", "# Teste markdown UTF-8 sem BOM", (New-Object System.Text.UTF8Encoding $false))

# Criar .png binario (header PNG valido, 8 bytes)
$pngBytes = [byte[]](0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
[System.IO.File]::WriteAllBytes("test-binary.png", $pngBytes)

git add test-utf8.md test-binary.png
git commit -m "test: validacao manual cenarios 1 e 4 do hook encoding utf8"
```

**Esperado:** commit aceito. `test-utf8.md` passa na whitelist e é UTF-8 válido. `test-binary.png` fora da whitelist, ignorado.

Se commit for rejeitado, reportar saída completa e parar.

#### Cenário 2: `.ps1` com BOM — bloqueia

```powershell
$utf8WithBom = New-Object System.Text.UTF8Encoding($true)
[System.IO.File]::WriteAllText("test-bom.ps1", "Write-Host 'teste com BOM'", $utf8WithBom)

git add test-bom.ps1
git commit -m "test: validacao manual cenario 2 do hook encoding utf8"
```

**Esperado:** commit **rejeitado**. Hook reporta `test-bom.ps1` com problema "Arquivo .ps1 com BOM". Exit code do `git commit` ≠ 0.

Reportar saída completa do hook.

#### Cenário 3: `.java` em Latin-1 — bloqueia

```powershell
$latin1 = [System.Text.Encoding]::GetEncoding("ISO-8859-1")
# String com caractere fora do ASCII (ç em Latin-1 = byte 0xE7, invalido em UTF-8 isolado)
[System.IO.File]::WriteAllText("test-latin1.java", 'public class Test { String s = "operaç"; }', $latin1)

git add test-latin1.java
git commit -m "test: validacao manual cenario 3 do hook encoding utf8"
```

**Esperado:** commit **rejeitado**. Hook reporta `test-latin1.java` com problema "Nao e UTF-8 valido". Hook **também reporta** `test-bom.ps1` ainda staged (do cenário 2). Reportar saída completa.

#### Cenário 5: override `--no-verify` bypassa

```bash
git commit --no-verify -m "test: validacao manual cenario 5 override no-verify"
```

**Esperado:** commit aceito (hook bypassado). Commit inclui `test-bom.ps1` + `test-latin1.java`.

### Tarefa 11 — Limpeza dos commits e arquivos de teste

```bash
git log --oneline -5
# Esperado:
# - commit cenario 5 (HEAD)
# - commit cenario 1+4
# - commit feat(githooks): adiciona entrypoint...
# - commit feat(claude): adiciona hook universal...
# - squash 4.1 (origem)
```

Reset os 2 commits temporários (cenários 1+4 e 5):

```bash
git branch --show-current  # confirmar que esta em feat/etapa-4-2-encoding-utf8-hook
git reset --hard HEAD~2
```

**Atenção crítica:**

1. **`git reset --hard` SOMENTE na branch da etapa.** Validar com `git branch --show-current` antes.
2. Se a contagem de commits divergir do esperado, **parar e reportar** — não ajustar número de `HEAD~` silenciosamente.

Limpar arquivos de teste do working tree (caso ainda existam):

```powershell
Remove-Item test-utf8.md, test-binary.png, test-bom.ps1, test-latin1.java -ErrorAction SilentlyContinue
git status
```

Esperado:
- Working tree limpo.
- `git log --oneline -1` mostra commit `feat(githooks): adiciona entrypoint pre-commit + orquestrador powershell`.

### Tarefa 12 — Editar `docs/decisoes.md`

Adicionar subseção "Encoding UTF-8 (Sub-etapa 4.2)" e subseção "Padrão orquestrador 1:N para `pre-commit` (Sub-etapa 4.2)" conforme escopo decidido. Inserir entre "Conventional Commits (Sub-etapa 4.1)" e "Claude Code hooks nativos".

Adicionar entrada no histórico (final do arquivo). Substituir `2026-MM-DD` pela data real.

### Tarefa 13 — Editar `docs/hooks-pendentes.md`

Operações A, B, C conforme escopo decidido.

### Tarefa 14 — Editar `docs/progresso.md`

Operações A, B, C, D conforme escopo decidido. **Atenção à ordem cronológica em "Sub-etapas concluídas":** 4.0 → 4.0.1 → 4.1 → 4.2.

### Tarefa 15 — Versionar este próprio prompt

`git add docs/prompt-etapa-4-2.md`.

### Tarefa 16 — Commit 3 (docs decisoes + hooks-pendentes)

```bash
git add docs/decisoes.md docs/hooks-pendentes.md
git commit -m "docs: registra regra de encoding utf-8 e padrao orquestrador pre-commit"
```

Hook próprio valida os docs. Ambos `.md`, devem ser UTF-8 válido. Deve passar.

### Tarefa 17 — Commit 4 (docs progresso + prompt)

```bash
git add docs/progresso.md docs/prompt-etapa-4-2.md
git commit -m "docs: registra sub-etapa 4.2 em progresso e versiona prompt"
```

### Tarefa 18 — Validação final antes de push

```bash
git status
git log --oneline -5
git config core.hooksPath
git ls-files --stage .githooks/pre-commit
file .githooks/pre-commit
```

Esperado:
- Working tree limpo.
- 4 commits novos.
- `core.hooksPath` retorna `.githooks`.
- `.githooks/pre-commit` registrado como `100755`.
- `file .githooks/pre-commit` confirma ASCII/UTF-8 com LF.

**`check.ps1` opcional** — esta etapa não toca em código Java, mas confirma suite intocada se rodado.

## Restrições e freios

1. **Não criar outros hooks.** Esta etapa entrega **apenas** o encoding UTF-8. Markdown blank lines (4.3) e tamanho de docs (4.4) ficam para sub-etapas seguintes.

2. **Não criar entrypoints `pre-push` ou outros.** Apenas `pre-commit` (junto com `commit-msg` já existente da 4.1).

3. **Não criar subagents, skills, ou CLAUDE.md.**

4. **Não tocar em scripts existentes** (`setup.ps1`, `dev.ps1`, etc).

5. **Não tocar em `src/`, `pom.xml`, `application*.yml`, `frontend/`, `docker-compose.yml`, migrations.**

6. **Não tocar em `.gitignore`, `.gitattributes`, ADRs.**

7. **Não tocar em `.claude/hooks/universal/conventional-commits.ps1`** (hook da 4.1, está correto).

8. **Não tocar em `.githooks/commit-msg`, `.githooks/commit-msg.ps1`** (entrypoints da 4.1).

9. **Não introduzir dependências externas** (`file` Unix tool, husky, pre-commit framework Python). Continua PowerShell puro + .NET nativo.

10. **Encoding UTF-8 sem BOM** em todos os arquivos novos. `.ps1` especialmente sensível — o próprio hook vai rejeitar se houver BOM em qualquer `.ps1` staged.

11. **Sem acentos** nas mensagens de output dos hooks. Docs `.md` podem ter acentos normalmente.

12. **Line endings LF** em `.githooks/pre-commit` (entrypoint sem extensão).

13. **Não usar `Write-Error` + `exit`.** Padrão: `Write-Host -ForegroundColor Red` + `exit 1`.

14. **Bit de execução obrigatório** via `git update-index --chmod=+x` para `.githooks/pre-commit`. Lição da Etapa 1.5.

15. **Não usar `pwsh`.** Apenas `powershell` (PowerShell 5.1). Confirmado na 4.1.

16. **Lógica de validação fica em `.claude/hooks/universal/encoding-utf8.ps1`.** Orquestrador `pre-commit.ps1` é fino — só descobre repo root, itera array `$hooks`, agrega exit codes.

17. **Não fazer hook falhar em arquivo deletado.** `Test-Path $file` antes de tentar ler.

18. **Não duplicar lógica** entre orquestrador e hook real.

19. **`git reset --hard` apenas na branch da etapa.** Nunca em `main`. Validar com `git branch --show-current`.

20. **Validação destrutiva COMPLETA (5 cenários) é gate de "pronto".** Reportar saídas no PR body.

21. **Não tomar decisão silenciosa em zona limítrofe.** Se algum cenário comportar diferente do esperado, parar e reportar.

22. **Não sugerir próxima etapa espontaneamente.**

23. **Antes de escrever cada arquivo, ler arquivos vivos** (Tarefa 3).

24. **Hook deve passar pela sua própria validação.** O próprio `encoding-utf8.ps1` precisa ser UTF-8 sem BOM (auto-validação no Commit 1 — embora hook não esteja ativo ainda nesse commit, deve estar correto). O `pre-commit.ps1` é validado no Commit 2 quando hook fica ativo.

25. **Ordem cronológica em `progresso.md`:** 4.0 → 4.0.1 → 4.1 → 4.2. Não inverter.

## Estrutura de commits

Branch: `feat/etapa-4-2-encoding-utf8-hook`

**Commit 1** — `feat(claude): adiciona hook universal de encoding utf-8`
- `.claude/hooks/universal/encoding-utf8.ps1` (novo)

**Commit 2** — `feat(githooks): adiciona entrypoint pre-commit + orquestrador powershell`
- `.githooks/pre-commit` (novo, bit de execução setado)
- `.githooks/pre-commit.ps1` (novo)
- `.githooks/README.md` (modificado — adiciona seção "Padrão de orquestrador")

**Commit 3** — `docs: registra regra de encoding utf-8 e padrao orquestrador pre-commit`
- `docs/decisoes.md`
- `docs/hooks-pendentes.md`

**Commit 4** — `docs: registra sub-etapa 4.2 em progresso e versiona prompt`
- `docs/progresso.md`
- `docs/prompt-etapa-4-2.md`

## Validação antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -5
git config core.hooksPath
git ls-files --stage .githooks/pre-commit
git ls-files --stage .githooks/commit-msg
```

Esperado:
- `check.ps1` passa.
- Working tree limpo.
- 4 commits novos na branch.
- `core.hooksPath` retorna `.githooks`.
- `.githooks/pre-commit` e `.githooks/commit-msg` ambos registrados como `100755`.

## PR

Título: `feat: sub-etapa 4.2 — hook universal de encoding utf-8 (estreia padrao orquestrador pre-commit)`

Body sugerido:

```markdown
## Summary

Implementa o **segundo hook funcional** do projeto: encoding UTF-8 via `pre-commit`. Estreia o segundo entrypoint do padrao de 3 camadas, a primeira validacao multi-arquivo, e o **padrao orquestrador 1:N** para `pre-commit`.

### Por que orquestrador

Diferente do `commit-msg.ps1` da 4.1 (delegador 1:1, uma regra), o `pre-commit.ps1` desta sub-etapa e desenhado para invocar **multiplos hooks em sequencia**. Razao: sub-etapas 4.3 (Markdown blank lines) e 4.4 (tamanho de docs) tambem usarao `pre-commit`. Se o companheiro fosse fino 1:1, precisaria refatorar na 4.3. Decisao tomada antes da redacao do prompt com o operador.

### Regra implementada

- **Whitelist por extensao:** `.md`, `.java`, `.yml`, `.yaml`, `.xml`, `.properties`, `.ps1`, `.sql`, `.ts`, `.tsx`, `.js`, `.jsx`, `.json`, `.css`, `.html`.
- **Whitelist por nome exato:** `.gitignore`, `.gitattributes`, `.editorconfig`, `.env.example`. Tambem arquivos sem extensao dentro de `.githooks/`.
- **Validacao:** todos os tipos da whitelist devem ser UTF-8 valido. `.ps1` adicionalmente NAO pode ter BOM (licao da Etapa 2.6).
- **Filtro do diff:** `--diff-filter=ACM` (Added, Copied, Modified).
- **Fora da whitelist:** binarios e tipos nao listados passam silenciosamente.
- **Override:** `git commit --no-verify` em emergencias.

### Validacao destrutiva manual

Cinco cenarios executados antes do commit final:

1. **Arquivos validos passam** (`test-utf8.md` UTF-8 + `test-binary.png` binario) — commit aceito.
2. **`.ps1` com BOM bloqueia** (`test-bom.ps1`) — commit rejeitado com mensagem clara.
3. **`.java` em Latin-1 bloqueia** (`test-latin1.java`) — commit rejeitado.
4. **`.png` binario e ignorado** (parte do cenario 1).
5. **`--no-verify` bypassa** — commit aceito.

Todos os 5 cenarios passaram conforme esperado.

### Mudancas

- `.claude/hooks/universal/encoding-utf8.ps1`: logica real do hook. Le `git diff --cached --name-only --diff-filter=ACM`, filtra pela whitelist, valida cada arquivo com UTF-8 estrito (`System.Text.UTF8Encoding($false, $true)`). Para `.ps1`, verifica adicionalmente ausencia de BOM (bytes EF BB BF).
- `.githooks/pre-commit`: entrypoint bash sem extensao. Bit de execucao registrado via `git update-index --chmod=+x`. Chama `powershell` (nao `pwsh`).
- `.githooks/pre-commit.ps1`: orquestrador 1:N. Itera array `$hooks` invocando cada um e agregando exit codes. Sem early-exit no primeiro fail — todos os hooks rodam para que o operador veja todas as violacoes de uma vez.
- `.githooks/README.md`: adiciona secao "Padrao de orquestrador (1:N)" documentando como sub-etapas seguintes acrescentam hooks ao `pre-commit`.
- `docs/decisoes.md`: subsecoes "Encoding UTF-8 (Sub-etapa 4.2)" e "Padrao orquestrador 1:N para pre-commit (Sub-etapa 4.2)". Entrada no historico.
- `docs/hooks-pendentes.md`: item Encoding UTF-8 movido para "Hooks implementados". Data atualizada.
- `docs/progresso.md`: sub-etapa 4.2 em "Sub-etapas concluidas". Licoes da 4.2. Entrada no historico.

### Validacao destrutiva pos-merge sugerida

Em qualquer branch (nao main):

\```powershell
# Cenario A: arquivo valido passa
"# teste" | Out-File -Encoding utf8NoBOM teste-pos-merge.md
git add teste-pos-merge.md
git commit -m "test: smoke pos merge da sub etapa 4.2"  # aceito

# Cenario B: .ps1 com BOM bloqueia
[System.IO.File]::WriteAllText("teste-bom.ps1", "Write-Host 'x'", (New-Object System.Text.UTF8Encoding $true))
git add teste-bom.ps1
git commit -m "test: smoke com BOM em ps1"  # rejeitado

# Cenario C: override bypassa
git commit --no-verify -m "test: smoke com no verify"  # aceito

# Limpeza
git reset --hard HEAD~2
Remove-Item teste-pos-merge.md, teste-bom.ps1 -ErrorAction SilentlyContinue
\```

### Proximo passo

Sub-etapa 4.3 (hook universal de blank lines em Markdown). Apenas acrescenta linha ao array `$hooks` em `.githooks/pre-commit.ps1` + cria `.claude/hooks/universal/markdown-blank-lines.ps1`. Padrao orquestrador 1:N estabelecido nesta sub-etapa reduz o trabalho da 4.3 a "implementar a regra". Decisao fora deste PR.
```

## Pós-criação do PR

1. Abrir PR via `gh pr create`.
2. Capturar o número.
3. Editar `docs/decisoes.md`, `docs/hooks-pendentes.md`, `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `feat/etapa-4-2-encoding-utf8-hook` empurrada com 5 commits (4 + 1 update do PR).
- PR aberto, CI verde, **não mergeado**.
- `main` ainda no squash da 4.1.
- Working tree limpo.
- Arquivos `test-utf8.md`, `test-binary.png`, `test-bom.ps1`, `test-latin1.java` **removidos**.
- `git ls-files --stage .githooks/pre-commit` retorna `100755`.
- Reportar com `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup`, e saídas dos 5 cenários destrutivos.

## O que NÃO fazer ao terminar

- Não mergear o PR.
- Não criar prompt da Sub-etapa 4.3.
- Não criar hooks de Markdown blank lines ou tamanho de docs.
- Não criar subagents, skills, CLAUDE.md.
- Não tocar em scripts existentes além dos arquivos prescritos.
- Não tocar em `.gitignore`, `.gitattributes`, ADRs.
- Não tocar em arquivos da 4.1 (`conventional-commits.ps1`, `commit-msg`, `commit-msg.ps1`).
- Não deixar arquivos `test-*.tmp/.md/.ps1/.java/.png` na branch.
- Não deixar commits de validação destrutiva no histórico — limpar via `git reset --hard`.
- Não sugerir "próximo passo" espontaneamente.
- Não relaxar whitelist se aparecer falso positivo em algum tipo legítimo de arquivo — reportar e calibrar com operador.
- Não relaxar regra "sem BOM em `.ps1`" — é lição registrada da Etapa 2.6, não está em discussão.
- Não duplicar lógica de validação entre orquestrador e hook real.
- Não usar `pwsh` — apenas `powershell`.
