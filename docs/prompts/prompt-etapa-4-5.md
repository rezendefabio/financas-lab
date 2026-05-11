# Prompt — Etapa 4.5: Hook Java/Spring de Maven `<release>` (primeiro hook de stack, ativa escopo `java-spring/`)

## Contexto

Camada 3 com 4 hooks funcionais em produção, todos universais:

- 4.1 (PR #40): Conventional Commits via `commit-msg`.
- 4.2 (PR #41): Encoding UTF-8 via `pre-commit`, orquestrador 1:N.
- 4.3 (PR #43): Markdown blank lines via `pre-commit`.
- 4.4 (PR #44): Tamanho de docs (modo warn) via `pre-commit`.

ADR-011 (PR #42) formaliza padrões de validação destrutiva.

**Esta sub-etapa entrega o primeiro hook específico de stack** — não universal. Estreia ocupação da pasta `.claude/hooks/java-spring/` (criada na 4.0, vazia desde então). Quarto hook a viver no orquestrador `pre-commit`.

Características novas desta sub-etapa:

1. **Primeiro hook de escopo de stack (`java-spring/`)**, não universal. Decidiu-se na D2 que **cada hook decide internamente** se vale rodar lendo o próprio `git diff --cached` — mesmo padrão dos universais. Orquestrador continua igual: lista todos os hooks no array `$hooks` sem distinção de escopo. Filtro é responsabilidade do hook.

2. **Primeiro hook a remover `.gitkeep`** de uma pasta de escopo (análogo ao que aconteceu em `.claude/hooks/universal/` na 4.1).

3. **Hook puramente preventivo no momento da criação.** `pom.xml` atual do projeto já tem `<release>${java.version}</release>` (lição 2.5 aplicada na Camada 2). Hook não corrige débito existente — arma regra para prevenir regressão futura.

A regra implementada: se `pom.xml` está no diff staged, ele deve conter pelo menos uma ocorrência da tag `<release>...</release>` dentro de `<configuration>` do `maven-compiler-plugin`. Caso contrário, bloqueia commit.

Quando esta etapa terminar:

- Qualquer commit local que modifique `pom.xml` removendo `<release>` será bloqueado.
- Hooks específicos de stack provam consistência do padrão "cada hook decide se roda".
- `.claude/hooks/java-spring/` ocupada com primeiro hook real.

## Padrões que estreiam nesta etapa

1. **Primeiro hook não-universal.** Estabelece que pasta de escopo (`java-spring/`) segue mesmo padrão de invocação que `universal/` — diferença é apenas o **critério de aplicabilidade** dentro do hook (lê `git diff --cached`, filtra por tipo de arquivo).
2. **Orquestrador agnóstico a escopo.** Array `$hooks` em `.githooks/pre-commit.ps1` não distingue universal de stack. Esta sub-etapa **prova** essa decisão arquitetural funcionando.
3. **Hook puramente preventivo.** Primeiro hook que arma regra sem corrigir débito existente — `pom.xml` já cumpre. Modelo replicável para sub-etapas futuras que queiram travar regressão.

## Escopo decidido (calibrado com operador antes da redação)

### Regra implementada

**Gatilho:** `pom.xml` no diff staged (`git diff --cached --name-only --diff-filter=ACM` retorna `pom.xml`).

**Validação:**

- Procurar **pelo menos uma ocorrência** da tag `<release>` (com qualquer conteúdo interno) no conteúdo do `pom.xml` staged.
- Aceita qualquer valor interno: `<release>21</release>`, `<release>${java.version}</release>`, `<release>17</release>`. Hook valida **presença**, não valor — versão é decisão de projeto registrada no próprio `pom.xml`.
- Aceita whitespace e quebras de linha entre `<release>` e `</release>`.

**Não validar:**

- Localização exata dentro do XML (se está em `maven-compiler-plugin` ou outro lugar). Razão: parsear XML é overkill; regex simples basta e raramente produz falso positivo (`<release>` é tag específica do compilador).
- Valor da versão Java. Razão: ratificado em D5.3.
- `pom.xml` em sub-módulos (se houver). Razão: lab é módulo único hoje; quando virar multi-módulo, sub-etapa futura calibra escopo.

**Comportamento ao detectar ausência:** `Write-Host -ForegroundColor Red` com tag `[ERRO]`, mensagem explicativa apontando a lição 2.5, instrução de fix, override `--no-verify` mencionado. Exit code 1.

**Filtro do diff:** apenas `pom.xml` no diff staged dispara o hook. Outros arquivos `.xml` (settings, etc) são ignorados.

**Override:** `git commit --no-verify` continua válido (padrão consolidado).

### Arquivos criados e modificados

```
.claude/hooks/java-spring/maven-release.ps1   ← novo (lógica real)
.claude/hooks/java-spring/.gitkeep             ← REMOVIDO (pasta passa a ter arquivo real)
.githooks/pre-commit.ps1                       ← edição (adiciona 1 linha ao array $hooks)
docs/decisoes.md                               ← edição (subseção 4.5 + nota sobre escopo de stack)
docs/hooks-pendentes.md                        ← edição (move item Maven <release> para Implementados + data)
docs/progresso.md                              ← edição (lições + sub-etapa + histórico)
docs/prompt-etapa-4-5.md                       ← novo (este próprio prompt)
```

**Não tocar:**

- Hooks universais (`conventional-commits.ps1`, `encoding-utf8.ps1`, `markdown-blank-lines.ps1`, `docs-size.ps1`).
- Entrypoints (`commit-msg`, `pre-commit`).
- `.githooks/commit-msg.ps1`.
- `.githooks/README.md` (padrão orquestrador já documentado).
- `pom.xml` do projeto (já tem `<release>${java.version}</release>` — não precisa de fix).
- ADRs.
- Código (`src/`, `frontend/`, scripts, migrations, etc).

### Conteúdo de `.claude/hooks/java-spring/maven-release.ps1`

```powershell
$ErrorActionPreference = "Stop"

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

# Filtro de aplicabilidade: hook so age se pom.xml estiver no diff
$pomStaged = $stagedFiles | Where-Object { ($_ -replace '\\', '/') -eq 'pom.xml' }

# Forcar contexto array (licao 4.3: PS5.1 desempacota array de 1 elemento)
$pomStaged = @($pomStaged)

if ($pomStaged.Count -eq 0) {
    # pom.xml nao esta no diff - hook nao se aplica
    exit 0
}

# pom.xml esta staged - validar presenca da tag <release>
$pomPath = $pomStaged[0]

if (-not (Test-Path $pomPath)) {
    Write-Host "[ERRO] pom.xml listado no diff mas nao encontrado no working tree." -ForegroundColor Red
    exit 1
}

$content = [System.IO.File]::ReadAllText($pomPath, [System.Text.UTF8Encoding]::new($false))

# Regex: <release> seguido de qualquer conteudo (incluindo quebras), seguido de </release>
# (?s) ativa modo single-line: . casa qualquer caractere, inclusive newline
$hasReleaseTag = $content -match '(?s)<release\s*>.*?</release\s*>'

if (-not $hasReleaseTag) {
    Write-Host ""
    Write-Host "[ERRO] pom.xml staged nao contem tag <release> dentro de <configuration> do maven-compiler-plugin." -ForegroundColor Red
    Write-Host ""
    Write-Host "Por que esta regra existe (licao 2.5):" -ForegroundColor Cyan
    Write-Host "  - Sem <release> explicito, Maven usa default que pode divergir entre dev local e CI." -ForegroundColor Cyan
    Write-Host "  - Resultado: build passa local, quebra em CI (ou vice-versa) por versao Java diferente." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Como corrigir:" -ForegroundColor Cyan
    Write-Host "  - Adicionar <release>21</release> (ou versao configurada via variavel, ex: \$\{java.version\}) dentro de" -ForegroundColor Cyan
    Write-Host "    <configuration> do plugin maven-compiler-plugin no pom.xml." -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Exemplo minimo:" -ForegroundColor Cyan
    Write-Host "  <plugin>" -ForegroundColor Cyan
    Write-Host "    <artifactId>maven-compiler-plugin</artifactId>" -ForegroundColor Cyan
    Write-Host "    <configuration>" -ForegroundColor Cyan
    Write-Host "      <release>21</release>" -ForegroundColor Cyan
    Write-Host "    </configuration>" -ForegroundColor Cyan
    Write-Host "  </plugin>" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Override consciente em emergencia: git commit --no-verify" -ForegroundColor Yellow
    Write-Host "(documentar uso em PR body conforme decisoes.md)" -ForegroundColor Yellow
    exit 1
}

exit 0
```

**Notas críticas:**

1. **Apenas ASCII em mensagens** (lição da 4.4 — em-dash U+2014 quebra parse PS5.1 em strings). Hífen simples `-` em todas as listas.
2. **`$pomStaged = @($pomStaged)` antes do `.Count`** — lição da 4.3 (array unwrapping PS5.1) aplicada na fonte.
3. **`[System.Text.UTF8Encoding]::new($false)`** ao ler `pom.xml` — robusto independente de BOM.
4. **Regex `(?s)<release\s*>.*?</release\s*>`:**
   - `(?s)` ativa modo single-line: `.` casa newline também (necessário se o conteúdo da tag estiver em múltiplas linhas — improvável mas tecnicamente possível).
   - `\s*` permite whitespace antes do `>` da tag de abertura/fechamento.
   - `.*?` é greedy mínimo (casa o menor trecho possível).
5. **`exit 0` quando `pom.xml` não está no diff** — hook se autodesativa silenciosamente. Consistente com filtro "cada hook decide" (D2).
6. **`Write-Host -ForegroundColor Red`** para `[ERRO]`, `Cyan` para instrução, `Yellow` para override. Mesmo esquema dos hooks 4.1-4.3 (regras objetivas em modo fail).
7. **Sem dependências externas.** PowerShell puro + .NET nativo.
8. **Encoding UTF-8 sem BOM** no próprio arquivo (hook da 4.2 valida).

### Edição em `.githooks/pre-commit.ps1`

**Estado atual** (após 4.4):

```powershell
$hooks = @(
    (Join-Path $repoRoot ".claude\hooks\universal\encoding-utf8.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\markdown-blank-lines.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\docs-size.ps1")
)
```

**Estado após esta etapa:**

```powershell
$hooks = @(
    (Join-Path $repoRoot ".claude\hooks\universal\encoding-utf8.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\markdown-blank-lines.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\docs-size.ps1")
    (Join-Path $repoRoot ".claude\hooks\java-spring\maven-release.ps1")
)
```

Apenas uma linha adicionada. Array continua agnóstico a escopo — universal e stack convivem sem distinção sintática.

### Remoção do `.gitkeep`

`.claude/hooks/java-spring/.gitkeep` deve ser removido quando o primeiro hook real entrar na pasta. Mesma operação feita em `universal/` na 4.1.

Operação: `git rm .claude/hooks/java-spring/.gitkeep`. Vai no mesmo commit do hook lógico.

### Atualização de `docs/decisoes.md`

Adicionar nova subseção sob "Camada 3 — Configuração do Claude Code", **após** "Tamanho de docs em modo warn (Sub-etapa 4.4)" e **antes** de "Claude Code hooks nativos":

```markdown
### Maven release explicito (Sub-etapa 4.5)

**Regra:** se `pom.xml` esta no diff staged, deve conter pelo menos uma ocorrencia da tag `<release>` com qualquer conteudo interno. Caso contrario, commit bloqueado.

**Por que:** licao 2.5 — sem `<release>` explicito, Maven usa default que pode divergir entre dev local e CI, resultando em build inconsistente. Lab atual ja tem `<release>${java.version}</release>` configurado; hook arma regra para prevenir regressao.

**Valor da tag e livre:** `<release>21</release>`, `<release>17</release>`, `<release>${java.version}</release>` — todos passam. Hook valida presenca, nao valor. Versao Java e decisao de projeto, nao decisao de hook.

**Padrao novo estabelecido — hooks especificos de stack:**

Esta e a primeira sub-etapa a ocupar `.claude/hooks/java-spring/`. Universais (`universal/`) e especificos de stack (`java-spring/`, `next/`, `windows/`, `local/`) coexistem no orquestrador `pre-commit` sem distincao sintatica. O array `$hooks` em `.githooks/pre-commit.ps1` lista todos os hooks na ordem de registro, agnostico a escopo.

A diferenca e apenas o **criterio de aplicabilidade dentro do hook:** cada hook le `git diff --cached --name-only` e decide se vale agir. Hooks universais agem sempre (ou filtram por extensao generica como `.md`). Hooks de stack filtram por arquivos especificos da stack (`pom.xml`, `*.java`, `package.json`, etc.).

**Decisao consciente (D2 calibrada com operador):** filtro de aplicabilidade fica dentro do hook, nao no orquestrador. Razao: consistencia com 4.2-4.4. Custo de invocar hook que sai imediato com `exit 0` (quando nao se aplica) e negligivel. Centralizar filtro no orquestrador seria otimizacao prematura — so faria sentido com 20+ hooks ou com hooks pesados (parser de arquivo grande, etc).

**Hook implementado em:** `.claude/hooks/java-spring/maven-release.ps1`, quarto hook no orquestrador `pre-commit`.
```

Adicionar entrada no histórico (final do arquivo):

```markdown
- **2026-MM-DD** — Sub-etapa 4.5 concluida: quinto hook funcional, primeiro de stack (java-spring). Maven `<release>` em modo fail. `.claude/hooks/java-spring/` ativada (primeira ocupacao apos 4.0). Padrao consolidado: orquestrador `pre-commit` agnostico a escopo; cada hook filtra aplicabilidade internamente. Hook preventivo — `pom.xml` atual ja tem `<release>${java.version}</release>` (licao 2.5 aplicada na Camada 2). Mergeado via PR #XX.
```

### Atualização de `docs/hooks-pendentes.md`

**Operação A** — Remover linha do item Maven `<release>` da seção "Hooks Maven / Java":

Procurar pela linha (exata):

```markdown
- **Versao Java em `<release>` no `pom.xml`.** (Etapa 2.5) Validar que `pom.xml` tem `<release>` explicito no `maven-compiler-plugin`.
```

Se não existir exatamente assim, **parar e reportar** — o item pode ter texto ligeiramente diferente; identificar antes de remover.

**Operação B** — Adicionar entrada em "Hooks implementados":

```markdown
- **Maven release explicito** (Sub-etapa 4.5, PR #XX). Implementado em `.claude/hooks/java-spring/maven-release.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Hook age apenas se `pom.xml` esta no diff staged. Valida presenca de pelo menos uma tag `<release>` no conteudo do `pom.xml` (qualquer valor interno aceito). Modo fail. Primeira ocupacao de `.claude/hooks/java-spring/`.
```

**Operação C** — Atualizar data:

```markdown
**Última atualização:** 2026-MM-DD (Sub-etapa 4.5 — Maven release explicito em fail)
```

### Atualização de `docs/progresso.md`

**A.** Atualizar "Última atualização": `2026-MM-DD (Sub-etapa 4.5 — Maven release explicito)`.

**B.** Em "Sub-etapas concluídas" (ordem cronológica, após 4.4):

```markdown
- **4.5 — Hook Java/Spring de Maven release** (2026-MM-DD): quinto hook funcional, primeiro de stack. Ativa `.claude/hooks/java-spring/` (vazia desde 4.0). Valida que `pom.xml` no diff staged contem `<release>` (qualquer valor). Modo fail. Padrao consolidado: orquestrador `pre-commit` agnostico a escopo; hook decide aplicabilidade lendo o proprio `git diff --cached`. Hook preventivo — `pom.xml` atual ja cumpre (licao 2.5 aplicada na Camada 2). 6 cenarios destrutivos sob ADR-011. PR #XX.
```

**C.** Adicionar seção "Lições da Sub-etapa 4.5":

```markdown
## Licoes da Sub-etapa 4.5

### Candidatos a hook (automatizar em etapas futuras)

(A preencher se houver durante execucao.)

### Licoes de ambiente

(A preencher se houver durante execucao. Esperado pelo menos: padrao "hook decide aplicabilidade internamente" consolidado em segunda dimensao — universais (filtram por extensao generica) e stack (filtram por arquivo especifico) convivem no mesmo orquestrador sem distincao sintatica.)
```

**D.** Histórico geral:

```markdown
- **2026-MM-DD** — Sub-etapa 4.5 concluida: quinto hook funcional, primeiro de stack (java-spring). Maven `<release>` ativo via `pre-commit`. `.claude/hooks/java-spring/` ocupada pela primeira vez. Padrao orquestrador agnostico a escopo consolidado. Mergeado via PR #XX.
```

### Versionar este próprio prompt

`docs/prompt-etapa-4-5.md`.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra `66aced2` (squash da 4.4) ou superior.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-5.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `.claude/hooks/universal/` contém **4 arquivos** `.ps1`: conventional-commits, encoding-utf8, markdown-blank-lines, docs-size.
- `.claude/hooks/java-spring/` contém **apenas** `.gitkeep` (vazia desde 4.0).
- `pom.xml` na raiz contém `<release>${java.version}</release>` na linha ~161 (confirmado pelo operador antes da redação).

**Pré-requisitos de ambiente (lições acumuladas):**

- `powershell` (Windows PowerShell 5.1) disponível. **NÃO usar `pwsh`** (lição 4.1).
- Git Bash disponível.
- **`[System.Environment]::CurrentDirectory` sincronizado com `$PWD`** antes de qualquer `[System.IO.File]::WriteAllText` ou `ReadAllText` com path relativo (ADR-011 / lição 4.2.1).
- **Padrão `@(...)` ao consumir retorno de função em PS5.1** (lição 4.3).
- **Apenas ASCII em strings de mensagens `.ps1`** (lição 4.4 — em-dash U+2014 quebra parse).

Validar com:

```powershell
git status
git log --oneline -3
Test-Path docs\prompt-etapa-4-5.md
git config core.hooksPath
Get-ChildItem .claude\hooks\universal\
Get-ChildItem .claude\hooks\java-spring\
Select-String -Path pom.xml -Pattern "<release>" -Context 0,0
```

**Pré-condições ADR-011:**

- `Test-Path docs\prompt-etapa-4-5.md` retorna `True`.
- `git status` mostra apenas o prompt como untracked.
- `.claude/hooks/universal/` lista exatamente 4 `.ps1`.
- `.claude/hooks/java-spring/` lista apenas `.gitkeep`.
- `Select-String pom.xml` retorna pelo menos uma linha com `<release>`.

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

**Esperado:** segunda linha retorna `C:\projetos\financas-lab`.

### Tarefa 3 — Criar branch

```bash
git checkout -b feat/etapa-4-5-maven-release-hook
```

### Tarefa 4 — Antes de editar, ler arquivos vivos

```bash
cat .claude/hooks/universal/docs-size.ps1
cat .githooks/pre-commit.ps1
cat docs/decisoes.md
cat docs/hooks-pendentes.md
cat docs/progresso.md
cat pom.xml
```

**Confirmar especialmente:**

- `docs-size.ps1` (referência de estilo — replicar consistência, especialmente `@(...)` antes de `.Count` e mensagens em ASCII).
- `pre-commit.ps1` tem array `$hooks` com 3 entradas (encoding-utf8, markdown-blank-lines, docs-size).
- `decisoes.md` tem subseção "Tamanho de docs em modo warn (Sub-etapa 4.4)" → "Claude Code hooks nativos". A nova "Maven release explicito (Sub-etapa 4.5)" entra **entre** essas duas.
- `hooks-pendentes.md` tem item Maven `<release>` na seção "Hooks Maven / Java" — confirmar texto exato antes de remover.
- `progresso.md` tem "Sub-etapas concluídas" em ordem 4.0 → 4.0.1 → 4.1 → 4.2 → 4.2.1 → 4.3 → 4.4.
- `pom.xml` tem `<release>${java.version}</release>` ou similar — hook precisa aceitar exatamente esse formato (D5.3).

Se alguma divergência, **parar e reportar**.

### Tarefa 5 — Criar `.claude/hooks/java-spring/maven-release.ps1`

Conteúdo conforme escopo decidido. **UTF-8 sem BOM.** **Apenas ASCII** em mensagens.

**Pré-condição ADR-011:** após criar, validar bytes:

```powershell
$bytes = [System.IO.File]::ReadAllBytes(".claude/hooks/java-spring/maven-release.ps1")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
```

Esperado: NÃO 239, 187, 191 (BOM). Se aparecer BOM, corrigir.

**Validação adicional — sem caracteres não-ASCII em strings:**

```powershell
$content = [System.IO.File]::ReadAllText(".claude/hooks/java-spring/maven-release.ps1", [System.Text.UTF8Encoding]::new($false))
$nonAscii = [regex]::Matches($content, '[^\x00-\x7F]')
Write-Host "Caracteres nao-ASCII encontrados: $($nonAscii.Count)"
```

Esperado: `0`. Se aparecer >0, identificar e corrigir (lição 4.4).

### Tarefa 6 — Remover `.gitkeep` de `java-spring/`

```bash
git rm .claude/hooks/java-spring/.gitkeep
```

Vai no mesmo Commit 1 que o hook lógico.

### Tarefa 7 — Editar `.githooks/pre-commit.ps1`

Adicionar uma linha ao array `$hooks` conforme escopo. Validar:

```bash
git diff .githooks/pre-commit.ps1
```

Esperado: exatamente 1 linha adicionada (a do `maven-release.ps1`).

### Tarefa 8 — Commit 1 (hook lógica + remoção do .gitkeep)

```bash
git status
```

**Pré-condição ADR-011:** deve mostrar:
- Novo: `.claude/hooks/java-spring/maven-release.ps1`
- Deletado: `.claude/hooks/java-spring/.gitkeep`

```bash
git add .claude/hooks/java-spring/maven-release.ps1
git status   # confirma adição + remoção via git rm
git commit -m "feat(claude): adiciona hook java-spring de maven release explicito"
```

**Pré-condição ADR-011:** `$LASTEXITCODE` após commit deve ser `0`. Mensagem passa pelo hook Conventional Commits (ativo). Hook de encoding valida o `.ps1` novo.

### Tarefa 9 — Commit 2 (orquestrador estendido)

```bash
git add .githooks/pre-commit.ps1
git status
```

**Pré-condição ADR-011:** 1 arquivo staged.

```bash
git commit -m "feat(githooks): adiciona maven-release ao orquestrador pre-commit"
```

A partir deste commit, hook `maven-release` está **ativo no pre-commit** local.

**Importante:** próximo commit pode acionar o hook se `pom.xml` entrar no diff. Como esta sub-etapa não toca `pom.xml`, hook deve sair com `exit 0` silenciosamente em todos os commits subsequentes desta sub-etapa.

### Tarefa 10 — Pré-validação dos docs (preview)

Antes de commitar os 3 docs, validar via execução direta do hook (sem commit):

```bash
git add docs/decisoes.md docs/hooks-pendentes.md docs/progresso.md
git status
```

**Pré-condição ADR-011:** 3 arquivos staged. Nenhum deles é `pom.xml`.

```powershell
& .\.claude\hooks\java-spring\maven-release.ps1
$hookResult = $LASTEXITCODE
Write-Host "Hook exit code: $hookResult"
```

Esperado: `0` (hook não se aplica — `pom.xml` não está staged).

### Tarefa 11 — Commit 3 (docs decisoes + hooks-pendentes)

```bash
git reset HEAD docs/progresso.md
git status   # apenas decisoes.md e hooks-pendentes.md staged
git commit -m "docs: registra maven release explicito e padrao hooks de stack"
```

**Pré-condição ADR-011:** 2 staged; `$LASTEXITCODE = 0`.

### Tarefa 12 — Commit 4 (progresso + prompt versionado)

```bash
git add docs/progresso.md docs/prompt-etapa-4-5.md
git status   # 2 staged
git commit -m "docs: registra sub-etapa 4.5 em progresso e versiona prompt"
```

**Pré-condição ADR-011:** 2 staged; `$LASTEXITCODE = 0`.

### Tarefa 13 — Validação destrutiva sob ADR-011 (6 cenários)

**Pré-condição global:**

```powershell
[System.Environment]::CurrentDirectory
(Get-Location).Path
git status
git log --oneline -1
```

Sincronizado, working tree limpo, HEAD no Commit 4.

**Importante — backup do `pom.xml` real do projeto:**

Os cenários 1, 2 e 3 vão modificar `pom.xml` temporariamente. Fazer backup antes:

```powershell
Copy-Item pom.xml pom.xml.backup
Write-Host "Backup criado: pom.xml.backup"
Test-Path pom.xml.backup   # True
```

**Restaurar `pom.xml` original entre cenários** que o modificam.

#### Cenário 1: `pom.xml` com `<release>21</release>` correto -> passa

```powershell
$repoRoot = (Get-Location).Path

# Restaurar pom.xml do backup (estado original)
Copy-Item pom.xml.backup pom.xml -Force

# pom.xml atual ja tem <release>${java.version}</release> - cenario 1 testa "passa quando existe"
# Modificar o pom.xml minimamente (adicionar comentario) para forcar entrada no diff
$content = [System.IO.File]::ReadAllText("$repoRoot\pom.xml", [System.Text.UTF8Encoding]::new($false))
$content = $content -replace '(<project)', "<!-- Smoke test cenario 1 -->`n`$1"
[System.IO.File]::WriteAllText("$repoRoot\pom.xml", $content, (New-Object System.Text.UTF8Encoding $false))

# Pre-condicao ADR-011
$tagPresente = (Select-String -Path pom.xml -Pattern "<release>" -Quiet)
Write-Host "Tag <release> presente: $tagPresente (esperado: True)"

git reset HEAD
git add pom.xml
git status
git commit -m "test: validacao destrutiva cenario 1 pom com release ok"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 1 exit code: $exitCode (esperado: 0)"
```

**Esperado:** commit aceito.

#### Cenário 2: `pom.xml` sem `<release>` -> bloqueia

```powershell
# Restaurar pom.xml do backup
Copy-Item pom.xml.backup pom.xml -Force

# Remover linha contendo <release>
$content = [System.IO.File]::ReadAllText("$repoRoot\pom.xml", [System.Text.UTF8Encoding]::new($false))
$contentSemRelease = $content -replace '(?m)^\s*<release>.*</release>\s*\r?\n', ''
[System.IO.File]::WriteAllText("$repoRoot\pom.xml", $contentSemRelease, (New-Object System.Text.UTF8Encoding $false))

# Pre-condicao ADR-011 - tag deve estar ausente agora
$tagPresente = (Select-String -Path pom.xml -Pattern "<release>" -Quiet)
Write-Host "Tag <release> presente: $tagPresente (esperado: False)"

git reset HEAD
git add pom.xml
git status

git commit -m "test: validacao destrutiva cenario 2 pom sem release deveria bloquear"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 2 exit code: $exitCode (esperado: != 0)"
```

**Esperado:** commit **rejeitado**. Hook reporta `[ERRO]`, instrução de fix, lição 2.5. `$LASTEXITCODE != 0`.

Se aceito, **parar e investigar** — regex ou lógica errada.

#### Cenário 3: `pom.xml` com `<release>17</release>` (valor diferente) -> passa

```powershell
# Restaurar pom.xml do backup
Copy-Item pom.xml.backup pom.xml -Force

# Substituir <release>${java.version}</release> por <release>17</release>
$content = [System.IO.File]::ReadAllText("$repoRoot\pom.xml", [System.Text.UTF8Encoding]::new($false))
$contentNovaVersao = $content -replace '<release>[^<]*</release>', '<release>17</release>'
[System.IO.File]::WriteAllText("$repoRoot\pom.xml", $contentNovaVersao, (New-Object System.Text.UTF8Encoding $false))

# Pre-condicao
$matchLine = Select-String -Path pom.xml -Pattern "<release>17</release>" -Quiet
Write-Host "Tag <release>17</release> presente: $matchLine (esperado: True)"

git reset HEAD
git add pom.xml
git status

git commit -m "test: validacao destrutiva cenario 3 pom release valor diferente"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 3 exit code: $exitCode (esperado: 0)"
```

**Esperado:** commit aceito. Hook valida presença, não valor (D5.3).

#### Cenário 4: `.java` no diff sem `pom.xml` -> ignorado

```powershell
# Restaurar pom.xml do backup (limpa o stage de pom)
Copy-Item pom.xml.backup pom.xml -Force
git reset HEAD pom.xml

# Criar um .java de teste qualquer
[System.IO.File]::WriteAllText("$repoRoot\test-foo.java", "public class TestFoo {}", (New-Object System.Text.UTF8Encoding $false))

Test-Path .\test-foo.java

git reset HEAD
git add test-foo.java
git status

git commit -m "test: validacao destrutiva cenario 4 java sem pom no diff"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 4 exit code: $exitCode (esperado: 0)"
```

**Esperado:** commit aceito. Hook detecta que `pom.xml` não está no diff, sai imediato com `exit 0`.

#### Cenário 5: commit sem `.java` nem `pom.xml` -> ignorado silenciosamente

```powershell
# Criar arquivo neutro qualquer
[System.IO.File]::WriteAllText("$repoRoot\test-neutro.txt", "conteudo neutro", (New-Object System.Text.UTF8Encoding $false))

Test-Path .\test-neutro.txt

git reset HEAD
git add test-neutro.txt
git status

git commit -m "test: validacao destrutiva cenario 5 commit sem java nem pom"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 5 exit code: $exitCode (esperado: 0)"
```

**Esperado:** commit aceito. Hook sai imediato com `exit 0`.

#### Cenário 6: override `--no-verify` em `pom.xml` sem `<release>` -> bypassa

```powershell
# Remover release do pom.xml de novo
Copy-Item pom.xml.backup pom.xml -Force
$content = [System.IO.File]::ReadAllText("$repoRoot\pom.xml", [System.Text.UTF8Encoding]::new($false))
$contentSemRelease = $content -replace '(?m)^\s*<release>.*</release>\s*\r?\n', ''
[System.IO.File]::WriteAllText("$repoRoot\pom.xml", $contentSemRelease, (New-Object System.Text.UTF8Encoding $false))

git reset HEAD
git add pom.xml
git status

git commit --no-verify -m "test: validacao destrutiva cenario 6 override no-verify"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 6 exit code: $exitCode (esperado: 0)"
```

**Esperado:** commit aceito (bypass).

### Tarefa 14 — Limpeza dos commits e arquivos de teste

```bash
git log --oneline -10
```

Esperado (do mais recente):

```
<commit cenario 6>
<commit cenario 5>
<commit cenario 4>
<commit cenario 3>
<commit cenario 1>
<Commit 4>
<Commit 3>
<Commit 2>
<Commit 1>
<squash 4.4>
```

**5 commits de teste** (cenários 1, 3, 4, 5, 6 — cenário 2 foi rejeitado, não gerou commit).

```bash
git branch --show-current  # confirmar feat/etapa-4-5-maven-release-hook
git reset --hard HEAD~5
```

Sandbox-block esperado. Pedir execução manual ao operador. Antes de pedir, **contar manualmente os commits `test:`** no `git log`. Se diferente de 5, parar e reportar (algum cenário comportou diferente do esperado).

**Restaurar `pom.xml` original** e limpar backup:

```powershell
Copy-Item pom.xml.backup pom.xml -Force
Remove-Item pom.xml.backup -ErrorAction SilentlyContinue
Remove-Item test-foo.java, test-neutro.txt -ErrorAction SilentlyContinue
git status
```

**Pré-condição ADR-011:** working tree limpo. Se `pom.xml` aparecer modificado, comparar com `git show HEAD:pom.xml` — deve ser idêntico.

### Tarefa 15 — Validação final antes de push

```bash
git status
git log --oneline -5
git config core.hooksPath
git ls-files .claude/hooks/java-spring/
```

Esperado:
- Working tree limpo.
- 4 commits novos da 4.5.
- `core.hooksPath` retorna `.githooks`.
- `git ls-files .claude/hooks/java-spring/` retorna **apenas** `maven-release.ps1` (sem `.gitkeep` — foi removido no Commit 1).

## Restrições e freios

1. **Não criar outros hooks.** Apenas Maven `<release>`. Outros candidatos (`@Entity`, sufixo `Test`, Lombok) ficam para sub-etapas futuras se entrarem no plano.

2. **Não tocar em hooks universais** (`conventional-commits.ps1`, `encoding-utf8.ps1`, `markdown-blank-lines.ps1`, `docs-size.ps1`).

3. **Não tocar em entrypoints** (`commit-msg`, `pre-commit`, companheiros). Apenas `pre-commit.ps1` ganha 1 linha no array.

4. **Não criar subagents, skills, CLAUDE.md.**

5. **Não tocar em scripts existentes** (`setup.ps1`, etc.).

6. **Não tocar em `src/`, `application*.yml`, `frontend/`, `docker-compose.yml`, migrations.**

7. **Não tocar em `pom.xml` real (commitado).** Apenas durante validação destrutiva, com backup + restauração obrigatória.

8. **Não tocar em `.gitignore`, `.gitattributes`, ADRs.**

9. **Não tocar em `.githooks/README.md`.**

10. **Não introduzir dependências externas.** PowerShell puro + .NET nativo.

11. **Encoding UTF-8 sem BOM** no `maven-release.ps1`.

12. **APENAS ASCII em strings de mensagens do hook** (lição 4.4). Hifen simples `-`, sem em-dash, sem aspas tipográficas.

13. **`$pomStaged = @(...)` antes de `.Count`** (lição 4.3).

14. **Sincronização de `Environment.CurrentDirectory`** antes de qualquer `WriteAllText/ReadAllText` com path relativo (ADR-011).

15. **Não usar `Write-Error` + `exit`.** Padrão: `Write-Host -ForegroundColor` + `exit N`.

16. **Não usar `pwsh`.** Apenas `powershell` (PS5.1).

17. **Hook DEVE retornar exit 0** quando `pom.xml` não está no diff. Modo fail aplica **apenas** quando o gatilho dispara.

18. **`git reset --hard` apenas na branch da etapa.** Sandbox-block esperado; aprovação manual.

19. **Backup do `pom.xml` obrigatório** antes de validação destrutiva (cenários 1, 2, 3, 6 modificam o arquivo real). Restauração final obrigatória.

20. **Validação destrutiva COMPLETA (6 cenários)** é gate de pronto. Reportar saídas no PR body incluindo pré-condições.

21. **`git reset HEAD` entre cenários** — staging isolado (lição 4.3).

22. **Não tomar decisão silenciosa em zona limítrofe.** Se algum cenário comportar diferente do esperado, parar e reportar.

23. **Ordem cronológica em `progresso.md`:** 4.0 → 4.0.1 → 4.1 → 4.2 → 4.2.1 → 4.3 → 4.4 → 4.5.

24. **Lógica de validação fica em `maven-release.ps1`**, orquestrador apenas referencia.

25. **Não sugerir próxima sub-etapa** espontaneamente.

26. **Antes de escrever cada arquivo, ler arquivo vivo** (Tarefa 4).

27. **Validar conteúdo ASCII do hook** explicitamente após criação (Tarefa 5) — defesa contra lição 4.4 que afetou a sub-etapa anterior.

## Estrutura de commits

Branch: `feat/etapa-4-5-maven-release-hook`

**Commit 1** — `feat(claude): adiciona hook java-spring de maven release explicito`
- `.claude/hooks/java-spring/maven-release.ps1` (novo)
- `.claude/hooks/java-spring/.gitkeep` (REMOVIDO)

**Commit 2** — `feat(githooks): adiciona maven-release ao orquestrador pre-commit`
- `.githooks/pre-commit.ps1` (1 linha)

**Commit 3** — `docs: registra maven release explicito e padrao hooks de stack`
- `docs/decisoes.md`
- `docs/hooks-pendentes.md`

**Commit 4** — `docs: registra sub-etapa 4.5 em progresso e versiona prompt`
- `docs/progresso.md`
- `docs/prompt-etapa-4-5.md`

## Validação antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -5
git config core.hooksPath
```

Esperado:
- `check.ps1` passa (importante — primeira sub-etapa que toca pasta `java-spring/`, vale rodar suite Maven).
- Working tree limpo.
- 4 commits novos.
- `pom.xml` real do projeto **idêntico** ao estado original em `main` (backup restaurado corretamente).

## PR

Título: `feat: sub-etapa 4.5 — hook java-spring de maven release (primeiro hook de stack, ativa java-spring/)`

Body sugerido:

```markdown
## Summary

Implementa o **quinto hook funcional** do projeto: Maven `<release>` explicito via `pre-commit`. **Primeiro hook de stack** (nao-universal). Ativa pasta `.claude/hooks/java-spring/` que estava vazia desde 4.0.

**Padrao consolidado:** orquestrador `pre-commit` agnostico a escopo. Universais e stack convivem no array `$hooks` sem distincao sintatica. Filtro de aplicabilidade fica dentro de cada hook (lendo `git diff --cached`).

### Regra implementada

- **Gatilho:** `pom.xml` esta no diff staged (Added/Copied/Modified).
- **Validacao:** conteudo do `pom.xml` contem pelo menos uma ocorrencia da tag `<release>...</release>`. Aceita qualquer valor interno: `<release>21</release>`, `<release>17</release>`, `<release>${java.version}</release>`.
- **Modo:** fail. Bloqueia commit em violacao.
- **Razao:** licao 2.5 — sem `<release>` explicito, Maven usa default que diverge entre dev local e CI.
- **Override:** `git commit --no-verify`.

### Hook puramente preventivo

`pom.xml` atual do projeto ja tem `<release>${java.version}</release>` (licao 2.5 aplicada na Camada 2). Hook nao corrige debito existente — arma regra para prevenir regressao futura. Modelo replicavel para sub-etapas futuras que queiram travar boas decisoes do passado.

### Validacao destrutiva sob ADR-011

Seis cenarios com pre-condicoes explicitas (`Test-Path`, `git status`, `git reset HEAD`, sincronizacao `Environment.CurrentDirectory`, backup obrigatorio do `pom.xml` real):

1. **`pom.xml` com `<release>` correto** — aceito.
2. **`pom.xml` sem `<release>`** — rejeitado. Hook reporta licao 2.5 + exemplo de fix.
3. **`pom.xml` com `<release>17</release>` (valor diferente)** — aceito. Hook valida presenca, nao valor (decisao D5.3).
4. **`.java` no diff sem `pom.xml`** — ignorado silenciosamente (hook nao se aplica).
5. **Commit sem `.java` nem `pom.xml`** — ignorado silenciosamente.
6. **`--no-verify` em `pom.xml` sem `<release>`** — bypassa.

Todos passaram conforme esperado.

### Aplicacao das licoes acumuladas

- **Licao 4.1:** entrypoint usa `powershell` (PS5.1), nao `pwsh`.
- **Licao 4.2:** encoding UTF-8 sem BOM no `.ps1`. Hook proprio (4.2) ativo validaria o `maven-release.ps1` no Commit 1.
- **Licao 4.3:** `$pomStaged = @($pomStaged)` antes de `.Count`. Aplicado na fonte.
- **Licao 4.4:** apenas ASCII em mensagens. Validado explicitamente apos criar o hook.
- **ADR-011:** todas as pre-condicoes presentes no roteiro destrutivo. `Environment.CurrentDirectory` sincronizado antes de `WriteAllText`. `Test-Path` apos cada modificacao. Backup do `pom.xml` real obrigatorio.

### Mudancas

- `.claude/hooks/java-spring/maven-release.ps1`: logica real. Le `git diff --cached`, filtra `pom.xml`, valida regex `(?s)<release\s*>.*?</release\s*>`. Mensagem de erro inclui licao 2.5 + exemplo concreto de fix.
- `.claude/hooks/java-spring/.gitkeep`: REMOVIDO. Pasta passa a ter arquivo real.
- `.githooks/pre-commit.ps1`: adiciona 1 linha ao array `$hooks`. Quarto hook no orquestrador.
- `docs/decisoes.md`: subsecao "Maven release explicito (Sub-etapa 4.5)". Inclui nota sobre padrao de hooks de stack (orquestrador agnostico a escopo, hook decide aplicabilidade). Entrada no historico.
- `docs/hooks-pendentes.md`: item Maven `<release>` movido para "Hooks implementados". Data atualizada.
- `docs/progresso.md`: sub-etapa 4.5. Licoes da 4.5. Entrada no historico.

### Validacao destrutiva pos-merge sugerida

Em qualquer branch (nao main), com `[System.Environment]::CurrentDirectory = (Get-Location).Path` sincronizado:

\```powershell
# Backup obrigatorio
Copy-Item pom.xml pom.xml.backup

# Cenario A: pom valido passa
$conteudo = [System.IO.File]::ReadAllText("pom.xml", [System.Text.UTF8Encoding]::new($false))
$conteudo = $conteudo -replace '(<project)', "<!-- smoke -->`n`$1"
[System.IO.File]::WriteAllText("pom.xml", $conteudo, (New-Object System.Text.UTF8Encoding $false))
git reset HEAD
git add pom.xml
git commit -m "test: smoke 4.5 pom valido"  # aceito

# Cenario B: pom sem release bloqueia
Copy-Item pom.xml.backup pom.xml -Force
$conteudo = [System.IO.File]::ReadAllText("pom.xml", [System.Text.UTF8Encoding]::new($false))
$conteudo = $conteudo -replace '(?m)^\s*<release>.*</release>\s*\r?\n', ''
[System.IO.File]::WriteAllText("pom.xml", $conteudo, (New-Object System.Text.UTF8Encoding $false))
git reset HEAD
git add pom.xml
git commit -m "test: smoke 4.5 pom sem release"  # rejeitado

# Cenario C: override
git commit --no-verify -m "test: smoke 4.5 override"  # aceito

# Limpeza
Copy-Item pom.xml.backup pom.xml -Force
Remove-Item pom.xml.backup
git reset --hard HEAD~2  # remove cenarios A e C; B nao gerou commit
\```

### Proximo passo

Decisao fora deste PR. Possiveis caminhos:
- Mais hooks de stack (`@Entity` sem migration, sufixo `Test`).
- CLAUDE.md do projeto.
- Subagents (`pr-reviewer`, `architect-reviewer`).
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

- Branch `feat/etapa-4-5-maven-release-hook` empurrada com 5 commits (4 + 1 update).
- PR aberto, CI verde, **não mergeado**.
- `main` ainda no squash da 4.4.
- Working tree limpo.
- `pom.xml` real **idêntico** ao de `main` (não tocado pelos commits, restaurado após destrutivo).
- `.claude/hooks/java-spring/` contém **apenas** `maven-release.ps1` (sem `.gitkeep`).
- Reportar com `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup`, e saídas dos 6 cenários destrutivos incluindo pré-condições verificadas.

## O que NÃO fazer ao terminar

- Não mergear o PR.
- Não criar prompt da próxima sub-etapa.
- Não criar outros hooks de stack ou universais.
- Não criar subagents, skills, CLAUDE.md.
- Não tocar em scripts existentes.
- Não tocar em arquivos das sub-etapas anteriores.
- Não tocar em `.gitignore`, `.gitattributes`, ADRs.
- **Não deixar `pom.xml` modificado.** Restauração final é obrigatória.
- **Não deixar `pom.xml.backup` no working tree.**
- Não deixar `test-*.*` na branch.
- Não deixar commits `test:` no histórico — limpar via `git reset --hard` com aprovação manual.
- Não sugerir próximo passo espontaneamente.
- Não pular pré-condições ADR-011.
- Não esquecer `@(...)` antes de `.Count`.
- Não usar caracteres não-ASCII em mensagens do hook.
- Não usar `pwsh`.
- Não validar valor da versão Java (apenas presença da tag).
