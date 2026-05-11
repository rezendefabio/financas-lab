# Prompt — Etapa 4.7: Hook Java/Spring de @Entity sem migration Flyway (modo conservador, status A only)

## Contexto

Camada 3 com 5 hooks funcionais em producao + CLAUDE.md estrutural apos a Sub-etapa 4.6:

- 4.1 (PR #40): Conventional Commits via `commit-msg`.
- 4.2 (PR #41): Encoding UTF-8 via `pre-commit`, orquestrador 1:N.
- 4.3 (PR #43): Markdown blank lines via `pre-commit`.
- 4.4 (PR #44): Tamanho de docs (modo warn) via `pre-commit`.
- 4.5 (PR #45): Maven release explicito via `pre-commit`. Primeiro hook de stack.
- 4.6 (PR #46): CLAUDE.md do projeto.

ADR-011 (PR #42) formaliza padroes de validacao destrutiva.

**Esta sub-etapa entrega o sexto hook funcional** e o segundo de stack (`java-spring/`). Regra: arquivo `.java` novo com `@Entity` no diff staged exige migration Flyway nova no mesmo commit.

Caracteristicas novas desta sub-etapa:

1. **Primeira sub-etapa com decisao consciente de escopo reduzido vs licao original.** Texto da licao 2.1 em `docs/hooks-pendentes.md` linha 86 diz: "Modificacao de `@Entity` JPA exige migration Flyway no mesmo PR". Implementacao 4.7 cobre apenas Entity **nova** (status `A` no diff), nao modificacao (status `M`). Razao: status `M` produz falso positivo alto (refatoracao cosmetica de campo Java sem mudanca de schema). Caso "modificacao de Entity existente" fica como **debito explicito**, registrado em `hooks-pendentes.md`.

2. **Validacao destrutiva toca codigo real do projeto.** Cenario 4 modifica `Categoria.java` (Entity real, status `M`) para confirmar empiricamente que hook nao dispara. Backup via `git restore`. Mais arriscado que mocks; defesa em camadas obrigatoria.

3. **Hook puramente preventivo.** Auditoria pre-redacao confirmou ratio coerente: 3 Entities (Categoria, Conta, Transacao) + 4 migrations (V1__schema_inicial + 1 por Entity). Sem debito de Camada 2. Hook arma regra para Camada 4+ quando novas Entities entrarem.

Quando esta etapa terminar:

- Hook ativo bloqueia commits com `.java` novo contendo `@Entity` se nenhuma migration nova em `src/main/resources/db/migration/`.
- Caso "modificacao de Entity existente" formalmente registrado como debito conscientemente aceito.
- Segundo hook em `.claude/hooks/java-spring/`. Padrao "orquestrador agnostico a escopo" reforcado.

## Padroes que estreiam nesta etapa

1. **Decisao consciente de escopo reduzido vs licao original.** Hook implementa fracao da regra; restante registrado como debito.
2. **Validacao destrutiva toca codigo real (cenario 4).** Primeira sub-etapa apos 4.5 a fazer isso. Backup via `git restore` em vez de `Copy-Item`.
3. **Segundo hook java-spring no orquestrador.** Padrao agnostico a escopo provado em segunda dimensao.

## Escopo decidido (calibrado com operador antes da redacao)

### Regra implementada

**Gatilho:** `.java` no diff staged com status `A` (Added) que contem `@Entity` no conteudo.

**Validacao:**

- Para cada `.java` novo (status A) com `@Entity`, exigir pelo menos um arquivo `.sql` novo (status A) em `src/main/resources/db/migration/` com prefixo `V<numero>__*.sql` no mesmo commit.
- Hook valida **presenca** de migration nova, nao cobertura. Multiplas Entities + 1 migration = aceito (cobertura e responsabilidade do dev).
- Migration pode estar em qualquer subpasta de `src/main/resources/db/migration/`? **Nao.** Apenas direto na pasta (padrao Flyway).

**Nao validar:**

- Status `M` (Modified) — modificacao de Entity existente. Debito explicito registrado em `hooks-pendentes.md`.
- Status `R` (Renamed). Renomear arquivo Java tipicamente nao muda schema (a menos que mude `@Table(name=...)`, mas isso e mudanca intencional manual).
- Conteudo da migration. Hook nao verifica se ha `CREATE TABLE` apropriado — so verifica existencia do arquivo. Mesmo principio do Maven `<release>` (4.5).
- Pasta da Entity. Hook detecta `@Entity` em qualquer `.java` sob `src/main/java/`. Convencao de pasta (`infrastructure/persistence/`) e decisao arquitetural, nao responsabilidade do hook.

**Comportamento ao detectar Entity nova sem migration:** `Write-Host -ForegroundColor Red` com tag `[ERRO]`, mensagem explicativa apontando licao 2.1, instrucao de fix, exemplo minimo de migration, override `--no-verify` mencionado. Exit code 1.

**Filtro do diff:** `git diff --cached --name-only --diff-filter=A` para arquivos adicionados.

**Override:** `git commit --no-verify` continua valido.

### Arquivos criados e modificados

```
.claude/hooks/java-spring/entity-migration.ps1   <- novo (logica real)
.githooks/pre-commit.ps1                          <- edicao (adiciona 1 linha ao array $hooks)
docs/decisoes.md                                  <- edicao (subsecao 4.7 + nota sobre escopo reduzido)
docs/hooks-pendentes.md                           <- edicao (Maven Central + sufixo Test ficam; @Entity para implementados COM qualificador; novo item para debito de modificacao)
docs/progresso.md                                 <- edicao (licoes + sub-etapa + historico)
docs/prompt-etapa-4-7.md                          <- novo (este proprio prompt)
```

**Nao tocar:**

- Hooks universais (`conventional-commits`, `encoding-utf8`, `markdown-blank-lines`, `docs-size`).
- Hook `maven-release.ps1` (4.5).
- Entrypoints (`commit-msg`, `pre-commit`), companheiros (`commit-msg.ps1`).
- `.githooks/README.md`.
- `CLAUDE.md` raiz (esta sub-etapa NAO muda stack/ambiente/convencoes/restricoes — regra de manutencao da 4.6 se aplica: nao tocar).
- ADRs.
- `pom.xml`, scripts, `docker-compose.yml`.
- `src/` (exceto cenario 4 de validacao destrutiva, com backup obrigatorio).
- `.gitignore`, `.gitattributes`.

### Conteudo de `.claude/hooks/java-spring/entity-migration.ps1`

```powershell
$ErrorActionPreference = "Stop"

# Listar arquivos staged com status A (Added)
$prev = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$stagedRaw = git diff --cached --name-only --diff-filter=A 2>&1
$diffExitCode = $LASTEXITCODE
$ErrorActionPreference = $prev

if ($diffExitCode -ne 0) {
    Write-Host "[ERRO] Falha ao listar arquivos staged via git diff --cached --diff-filter=A." -ForegroundColor Red
    exit 1
}

$stagedFiles = $stagedRaw | Where-Object { $_ -and $_.Trim() }

if (-not $stagedFiles -or $stagedFiles.Count -eq 0) {
    exit 0
}

# Filtrar .java novos sob src/main/java/
$javaFiles = $stagedFiles | Where-Object {
    $norm = $_ -replace '\\', '/'
    $norm -like 'src/main/java/*' -and $norm.EndsWith('.java')
}
$javaFiles = @($javaFiles)

if ($javaFiles.Count -eq 0) {
    # Nenhum .java novo - hook nao se aplica
    exit 0
}

# Para cada .java novo, verificar se contem @Entity
$entitiesNovas = @()

foreach ($file in $javaFiles) {
    if (-not (Test-Path $file)) { continue }

    $content = [System.IO.File]::ReadAllText($file, [System.Text.UTF8Encoding]::new($false))

    # @Entity em qualquer contexto (anotacao em linha propria ou inline)
    # Regex tolera whitespace antes, ignora @EntityListeners ou outras anotacoes com prefixo Entity
    if ($content -match '(?m)^\s*@Entity\b') {
        $entitiesNovas += $file
    }
}

$entitiesNovas = @($entitiesNovas)

if ($entitiesNovas.Count -eq 0) {
    # Java novos existem, mas nenhum tem @Entity - hook nao se aplica
    exit 0
}

# Existe @Entity novo. Verificar se ha migration nova.
$migrationsNovas = $stagedFiles | Where-Object {
    $norm = $_ -replace '\\', '/'
    $norm -match '^src/main/resources/db/migration/V\d+__.*\.sql$'
}
$migrationsNovas = @($migrationsNovas)

if ($migrationsNovas.Count -gt 0) {
    # Regra cumprida: ha @Entity novo E ha migration nova
    exit 0
}

# Bloqueio: @Entity novo sem migration nova
Write-Host ""
Write-Host "[ERRO] Detectado(s) $($entitiesNovas.Count) arquivo(s) com @Entity novo(s) no commit, mas nenhuma migration nova em src/main/resources/db/migration/." -ForegroundColor Red
Write-Host ""
Write-Host "Arquivo(s) com @Entity novo(s):" -ForegroundColor Red
foreach ($e in $entitiesNovas) {
    Write-Host "  - $e" -ForegroundColor Red
}
Write-Host ""
Write-Host "Por que esta regra existe (licao 2.1):" -ForegroundColor Cyan
Write-Host "  - Nova @Entity sem migration cria divergencia entre codigo e schema." -ForegroundColor Cyan
Write-Host "  - Hibernate pode tentar criar tabela em runtime, mas isso e proibido em prod (ddl-auto=validate)." -ForegroundColor Cyan
Write-Host "  - Resultado: build local passa, prod quebra ao subir." -ForegroundColor Cyan
Write-Host ""
Write-Host "Como corrigir:" -ForegroundColor Cyan
Write-Host "  - Criar arquivo src/main/resources/db/migration/V<n+1>__<descricao>.sql com CREATE TABLE." -ForegroundColor Cyan
Write-Host "  - Incluir no mesmo commit que a Entity." -ForegroundColor Cyan
Write-Host ""
Write-Host "Exemplo minimo:" -ForegroundColor Cyan
Write-Host "  -- V5__cria_tabela_pagamento.sql" -ForegroundColor Cyan
Write-Host "  CREATE TABLE pagamento (" -ForegroundColor Cyan
Write-Host "    id UUID PRIMARY KEY," -ForegroundColor Cyan
Write-Host "    valor NUMERIC(15,2) NOT NULL" -ForegroundColor Cyan
Write-Host "  );" -ForegroundColor Cyan
Write-Host ""
Write-Host "Override consciente em emergencia: git commit --no-verify" -ForegroundColor Yellow
Write-Host "(documentar uso em PR body conforme decisoes.md)" -ForegroundColor Yellow
exit 1
```

**Notas criticas:**

1. **Apenas ASCII em mensagens** (licao 4.4 — em-dash U+2014 quebra parse PS5.1 em strings).
2. **`@(...)` antes de `.Count`** (licao 4.3 — array unwrapping PS5.1) aplicado em 3 pontos: `$javaFiles`, `$entitiesNovas`, `$migrationsNovas`.
3. **`[System.Text.UTF8Encoding]::new($false)`** ao ler `.java` — robusto independente de BOM.
4. **Regex `(?m)^\s*@Entity\b`:**
   - `(?m)` ativa modo multilinha: `^` casa inicio de linha (nao so do arquivo).
   - `\s*` permite indentacao.
   - `@Entity\b` — word boundary impede match em `@EntityListeners`, `@EntityGraph`, etc.
5. **Filtro de migration apenas direto em `db/migration/`** (sem subpastas) — `^src/main/resources/db/migration/V\d+__.*\.sql$` exige path exato. Padrao Flyway.
6. **Encoding UTF-8 sem BOM** no proprio arquivo (hook 4.2 valida).
7. **Sem dependencias externas.** PowerShell puro + .NET nativo.

### Edicao em `.githooks/pre-commit.ps1`

**Estado atual** (apos 4.5):

```powershell
$hooks = @(
    (Join-Path $repoRoot ".claude\hooks\universal\encoding-utf8.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\markdown-blank-lines.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\docs-size.ps1")
    (Join-Path $repoRoot ".claude\hooks\java-spring\maven-release.ps1")
)
```

**Estado apos esta etapa:**

```powershell
$hooks = @(
    (Join-Path $repoRoot ".claude\hooks\universal\encoding-utf8.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\markdown-blank-lines.ps1")
    (Join-Path $repoRoot ".claude\hooks\universal\docs-size.ps1")
    (Join-Path $repoRoot ".claude\hooks\java-spring\maven-release.ps1")
    (Join-Path $repoRoot ".claude\hooks\java-spring\entity-migration.ps1")
)
```

Apenas uma linha adicionada. Quinto hook no orquestrador, segundo java-spring.

### Atualizacao de `docs/decisoes.md`

Adicionar nova subsecao sob "Camada 3 — Configuracao do Claude Code", **apos** "CLAUDE.md do projeto (Sub-etapa 4.6)" e **antes** de "Claude Code hooks nativos":

```markdown
### @Entity sem migration Flyway (Sub-etapa 4.7)

**Regra:** se `.java` novo (status `A` no diff) sob `src/main/java/` contem `@Entity`, deve haver pelo menos um arquivo `.sql` novo em `src/main/resources/db/migration/V<n>__*.sql` no mesmo commit. Caso contrario, commit bloqueado.

**Por que:** licao 2.1 — nova `@Entity` sem migration cria divergencia entre codigo e schema. Hibernate pode tentar criar tabela em runtime, mas isso e proibido em producao (`ddl-auto=validate`). Build local passa, prod quebra ao subir.

**Escopo reduzido conscientemente — caso edge fora desta sub-etapa:**

Texto original da licao 2.1 em `docs/hooks-pendentes.md` (linha 86 antes desta sub-etapa) menciona: "Modificacao de `@Entity` JPA exige migration Flyway no mesmo PR". Esta sub-etapa implementa apenas o caso de Entity **nova** (status `A`), nao modificacao (status `M`).

Razao: status `M` produziria falso positivo alto. Refatoracao cosmetica de Entity existente (rename de variavel Java sem `@Column(name=...)`, adicao de comentario, mudanca de formatacao) nao requer migration — mas hook nao distingue isso sem parser de Java. Forcar criacao de migration vazia destruiria confianca no hook rapidamente.

Caso "modificacao de Entity existente requer migration" fica como **debito conscientemente aceito**, registrado em `docs/hooks-pendentes.md` na lista "Pendentes". Hoje, modificacao depende de disciplina do dev + revisao de PR. Se aparecer dor real (CI quebrar por esquecimento), sub-etapa futura calibra implementacao mais sofisticada (talvez parsing de diff `git diff --cached -U0 <arquivo>` para detectar adicao de `@Column`).

**Valor da migration e livre:** hook valida **presenca**, nao conteudo. Multiplas Entities + 1 migration consolidada = aceito (cobertura e responsabilidade do dev). Analogo ao Maven `<release>` (4.5) que valida presenca da tag, nao valor.

**Hook implementado em:** `.claude/hooks/java-spring/entity-migration.ps1`, quinto hook no orquestrador `pre-commit`, segundo em java-spring.
```

Adicionar entrada no historico (final do arquivo):

```markdown
- **2026-MM-DD** — Sub-etapa 4.7 concluida: sexto hook funcional, segundo de stack java-spring. `@Entity` novo (status A) exige migration Flyway nova no mesmo commit. Modo conservador conscientemente reduzido vs licao original 2.1 (modificacao de Entity existente fica como debito explicito, registrado em hooks-pendentes.md). Hook preventivo — projeto ja tem ratio coerente (3 Entities + 4 migrations). 6 cenarios destrutivos sob ADR-011, incluindo modificacao de Entity real (Categoria.java) para confirmar empiricamente que hook nao dispara em status M. Mergeado via PR #XX.
```

### Atualizacao de `docs/hooks-pendentes.md`

**Operacao A** — Mover item de "Pendentes" para "Implementados" COM qualificador:

Procurar linha 86 (texto exato):

```markdown
- **Modificação de `@Entity` JPA exige migration Flyway no mesmo PR.** (Etapa 2.1) Hook detecta diff em `@Entity` sem novo arquivo `Vn__*.sql`.
```

**Substituir por linha em "Pendentes" qualificada (caso edge):**

```markdown
- **Modificação de `@Entity` JPA existente exige migration Flyway no mesmo PR.** (Etapa 2.1, caso edge — modo conservador na 4.7 cobre apenas Entity nova/status A; modificacao/status M produz falso positivo alto e ficou como debito explicito). Avaliar implementacao sofisticada (parser de diff `git diff --cached -U0`) se aparecer dor real.
```

**Operacao B** — Adicionar entrada em "Hooks implementados":

```markdown
- **@Entity nova sem migration Flyway (modo conservador)** (Sub-etapa 4.7, PR #XX). Implementado em `.claude/hooks/java-spring/entity-migration.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Hook age apenas se ha `.java` novo (status A) sob `src/main/java/` contendo `@Entity`. Exige pelo menos um arquivo `src/main/resources/db/migration/V<n>__*.sql` novo no mesmo commit. Valida presenca, nao conteudo. Modo fail. Modificacao de Entity existente (status M) **nao dispara** o hook — caso edge registrado como debito em "Pendentes".
```

**Operacao C** — Atualizar data:

```markdown
**Última atualização:** 2026-MM-DD (Sub-etapa 4.7 — @Entity nova exige migration, modo conservador)
```

### Atualizacao de `docs/progresso.md`

**A.** Atualizar "Ultima atualizacao": `2026-MM-DD (Sub-etapa 4.7 — @Entity sem migration Flyway)`.

**B.** Em "Sub-etapas concluidas" (ordem cronologica, apos 4.6):

```markdown
- **4.7 — Hook Java/Spring de @Entity sem migration Flyway (modo conservador)** (2026-MM-DD): sexto hook funcional, segundo de stack. Bloqueia commit com `.java` novo (status A) contendo `@Entity` em `src/main/java/` se nao houver migration nova em `src/main/resources/db/migration/V<n>__*.sql`. Modo fail. Escopo conscientemente reduzido vs licao 2.1 — modificacao de Entity existente (status M) **nao dispara** o hook, ficou como debito explicito em `hooks-pendentes.md`. Hook preventivo: projeto ja tem ratio coerente (3 Entities + 4 migrations). Padrao agnostico a escopo reforcado: orquestrador `pre-commit` continua sem distincao sintatica entre universal e stack. 6 cenarios destrutivos sob ADR-011 incluindo modificacao de Entity real (Categoria.java) para confirmar empiricamente que hook nao dispara em status M. PR #XX.
```

**C.** Adicionar secao "Licoes da Sub-etapa 4.7":

```markdown
## Licoes da Sub-etapa 4.7

### Candidatos a hook (automatizar em etapas futuras)

(A preencher se houver durante execucao.)

### Licoes de ambiente

(A preencher se houver durante execucao. Esperado pelo menos: padrao "decisao consciente de escopo reduzido vs licao original" formalizado — hooks podem implementar fracao da regra quando caso completo produz falso positivo alto. Resto vira debito explicito em `hooks-pendentes.md`, nao decisao silenciosa.)
```

**D.** Historico geral:

```markdown
- **2026-MM-DD** — Sub-etapa 4.7 concluida: sexto hook funcional, segundo de stack. `@Entity` novo (status A) exige migration. Escopo conscientemente reduzido vs licao 2.1 — modificacao de Entity existente fica como debito explicito. Validacao destrutiva tocou codigo real (Categoria.java) com backup via git restore. Mergeado via PR #XX.
```

### Versionar este proprio prompt

`docs/prompt-etapa-4-7.md`.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra `bd4d8f4` (squash da 4.6) ou superior.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-7.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `.claude/hooks/universal/` contem 4 arquivos `.ps1`.
- `.claude/hooks/java-spring/` contem **apenas** `maven-release.ps1` (sem `entity-migration.ps1`).
- `src/main/java/com/laboratorio/financas/categoria/infrastructure/persistence/CategoriaEntity.java` existe (auditoria pre-prompt confirmou).
- `src/main/resources/db/migration/` contem **4** migrations (V1, V2, V3, V4).

**Pre-requisitos de ambiente (licoes acumuladas):**

- `powershell` (Windows PowerShell 5.1) disponivel. **NAO usar `pwsh`** (licao 4.1).
- Git Bash disponivel.
- **`[System.Environment]::CurrentDirectory` sincronizado com `$PWD`** (ADR-011 / licao 4.2.1).
- **Padrao `@(...)` ao consumir retorno de funcao** (licao 4.3).
- **Apenas ASCII em strings de mensagens `.ps1`** (licao 4.4).

Validar com:

```powershell
git status
git log --oneline -3
Test-Path docs\prompt-etapa-4-7.md
git config core.hooksPath
Get-ChildItem .claude\hooks\universal\
Get-ChildItem .claude\hooks\java-spring\
Get-ChildItem src\main\resources\db\migration\V*.sql
Test-Path src\main\java\com\laboratorio\financas\categoria\infrastructure\persistence\CategoriaEntity.java
```

**Pre-condicoes ADR-011:**

- `Test-Path docs\prompt-etapa-4-7.md` retorna `True`.
- `git status` mostra apenas o prompt como untracked.
- `.claude/hooks/universal/` lista exatamente 4 `.ps1`.
- `.claude/hooks/java-spring/` lista **apenas** `maven-release.ps1`.
- `src\main\java\com\laboratorio\financas\categoria\infrastructure\persistence\CategoriaEntity.java` retorna `True`.
- Pasta `db/migration/` contem V1, V2, V3, V4.

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
git checkout -b feat/etapa-4-7-entity-migration-hook
```

### Tarefa 4 — Antes de editar, ler arquivos vivos

```bash
cat .claude/hooks/java-spring/maven-release.ps1
cat .githooks/pre-commit.ps1
cat docs/decisoes.md
cat docs/hooks-pendentes.md
cat docs/progresso.md
cat src/main/java/com/laboratorio/financas/categoria/infrastructure/persistence/CategoriaEntity.java
```

**Confirmar:**

- `maven-release.ps1` (referencia de estilo java-spring — replicar consistencia, `@(...)` antes de `.Count`, mensagens ASCII).
- `pre-commit.ps1` tem array `$hooks` com 4 entradas (encoding-utf8, markdown-blank-lines, docs-size, maven-release).
- `decisoes.md` tem subsecao "CLAUDE.md do projeto (Sub-etapa 4.6)" -> "Claude Code hooks nativos". A nova "@Entity sem migration Flyway (Sub-etapa 4.7)" entra **entre** essas duas.
- `hooks-pendentes.md` tem item linha ~86: "**Modificação de `@Entity` JPA exige migration Flyway no mesmo PR.** (Etapa 2.1)..." — confirmar texto exato antes de substituir.
- `progresso.md` tem "Sub-etapas concluidas" em ordem ate 4.6.
- `CategoriaEntity.java` existe e contem `@Entity`. Confirmar que tem permissao para modificar durante cenario 4 (sera restaurado via `git restore`).

Se alguma divergencia, **parar e reportar**.

### Tarefa 5 — Criar `.claude/hooks/java-spring/entity-migration.ps1`

Conteudo conforme escopo decidido. **UTF-8 sem BOM.** **Apenas ASCII** em mensagens.

**Pre-condicao ADR-011:** apos criar, validar:

```powershell
Test-Path .claude\hooks\java-spring\entity-migration.ps1   # True

$bytes = [System.IO.File]::ReadAllBytes(".claude/hooks/java-spring/entity-migration.ps1")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191

$content = [System.IO.File]::ReadAllText(".claude/hooks/java-spring/entity-migration.ps1", [System.Text.UTF8Encoding]::new($false))
$nonAscii = [regex]::Matches($content, '[^\x00-\x7F]')
Write-Host "Caracteres nao-ASCII: $($nonAscii.Count)"
# Esperado: 0
```

Se BOM presente ou non-ASCII >0, corrigir antes de seguir.

### Tarefa 6 — Editar `.githooks/pre-commit.ps1`

Adicionar uma linha ao array `$hooks` conforme escopo. Validar:

```bash
git diff .githooks/pre-commit.ps1
```

Esperado: exatamente 1 linha adicionada (a do `entity-migration.ps1`).

### Tarefa 7 — Commit 1 (hook logica)

```bash
git add .claude/hooks/java-spring/entity-migration.ps1
git status   # apenas entity-migration.ps1 staged
git commit -m "feat(claude): adiciona hook java-spring de @entity sem migration flyway"
```

**Pre-condicao ADR-011:** `$LASTEXITCODE = 0`. Hook de encoding (4.2) valida o `.ps1` novo. Hook de blank lines (4.3) nao se aplica (nao e `.md`). Hook de docs-size (4.4) nao se aplica. Hook de maven-release (4.5) nao se aplica (`pom.xml` nao staged). Hook entity-migration (este) ainda nao esta no orquestrador (sera adicionado em Commit 2).

### Tarefa 8 — Commit 2 (orquestrador estendido)

```bash
git add .githooks/pre-commit.ps1
git status   # apenas pre-commit.ps1 staged
git commit -m "feat(githooks): adiciona entity-migration ao orquestrador pre-commit"
```

A partir deste commit, hook `entity-migration` esta **ativo no pre-commit** local.

**Importante:** proximos commits desta sub-etapa nao devem disparar o hook entity-migration. Como nenhum `.java` novo entra nos commits 3-4 (apenas docs `.md` e prompt), hook sai com `exit 0` silenciosamente.

### Tarefa 9 — Pre-validacao dos docs (preview)

Antes de commitar, validar via execucao direta:

```bash
git add docs/decisoes.md docs/hooks-pendentes.md docs/progresso.md
git status
```

**Pre-condicao ADR-011:** 3 arquivos staged. Nenhum e `.java`.

```powershell
& .\.claude\hooks\java-spring\entity-migration.ps1
$hookResult = $LASTEXITCODE
Write-Host "Hook exit code: $hookResult (esperado: 0 — sem .java novo)"
```

Esperado: 0.

### Tarefa 10 — Commit 3 (docs decisoes + hooks-pendentes)

```bash
git reset HEAD docs/progresso.md
git status   # apenas decisoes.md e hooks-pendentes.md staged
git commit -m "docs: registra entity-migration e padrao de escopo reduzido vs licao original"
```

**Pre-condicoes ADR-011:** 2 staged; `$LASTEXITCODE = 0`.

### Tarefa 11 — Commit 4 (progresso + prompt versionado)

```bash
git add docs/progresso.md docs/prompt-etapa-4-7.md
git status   # 2 staged
git commit -m "docs: registra sub-etapa 4.7 em progresso e versiona prompt"
```

**Pre-condicoes ADR-011:** 2 staged; `$LASTEXITCODE = 0`.

### Tarefa 12 — Validacao destrutiva sob ADR-011 (6 cenarios)

**Pre-condicao global:**

```powershell
[System.Environment]::CurrentDirectory
(Get-Location).Path
git status
git log --oneline -1
```

Sincronizado, working tree limpo, HEAD no Commit 4.

**Backup obrigatorio de `CategoriaEntity.java`** (cenario 4 modifica arquivo real):

```powershell
$pathEntity = "src\main\java\com\laboratorio\financas\categoria\infrastructure\persistence\CategoriaEntity.java"
Copy-Item $pathEntity "$pathEntity.backup"
Write-Host "Backup criado: $pathEntity.backup"
Test-Path "$pathEntity.backup"   # True
```

#### Cenario 1: `.java` novo SEM `@Entity` -> aceito

```powershell
$repoRoot = (Get-Location).Path
$testDir = "$repoRoot\src\main\java\com\laboratorio\financas\test"
New-Item -ItemType Directory -Path $testDir -Force | Out-Null

$conteudoSemEntity = @"
package com.laboratorio.financas.test;

public class TestUtility {
    public static String greet() {
        return "hello";
    }
}
"@

[System.IO.File]::WriteAllText("$testDir\TestUtility.java", $conteudoSemEntity, (New-Object System.Text.UTF8Encoding $false))

Test-Path "$testDir\TestUtility.java"

git reset HEAD
git add src/main/java/com/laboratorio/financas/test/TestUtility.java
git status   # apenas TestUtility.java staged

git commit -m "test: validacao destrutiva cenario 1 java sem entity"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 1 exit code: $exitCode (esperado: 0)"
```

**Esperado:** commit aceito, sem mensagem do hook entity-migration.

#### Cenario 2: `.java` novo COM `@Entity` + migration nova -> aceito

```powershell
$conteudoEntity = @"
package com.laboratorio.financas.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class TestSmokeEntity {
    @Id
    private Long id;
}
"@

[System.IO.File]::WriteAllText("$testDir\TestSmokeEntity.java", $conteudoEntity, (New-Object System.Text.UTF8Encoding $false))

$conteudoMigration = "-- Smoke test migration`nCREATE TABLE test_smoke_entity (id BIGINT PRIMARY KEY);`n"
[System.IO.File]::WriteAllText("$repoRoot\src\main\resources\db\migration\V999__test_smoke.sql", $conteudoMigration, (New-Object System.Text.UTF8Encoding $false))

Test-Path "$testDir\TestSmokeEntity.java"
Test-Path "$repoRoot\src\main\resources\db\migration\V999__test_smoke.sql"

git reset HEAD
git add src/main/java/com/laboratorio/financas/test/TestSmokeEntity.java
git add src/main/resources/db/migration/V999__test_smoke.sql
git status

git commit -m "test: validacao destrutiva cenario 2 entity com migration"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 2 exit code: $exitCode (esperado: 0)"
```

**Esperado:** commit aceito (regra cumprida).

#### Cenario 3: `.java` novo COM `@Entity` SEM migration -> bloqueado

```powershell
$conteudoEntity2 = @"
package com.laboratorio.financas.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class TestEntitySemMigration {
    @Id
    private Long id;
}
"@

[System.IO.File]::WriteAllText("$testDir\TestEntitySemMigration.java", $conteudoEntity2, (New-Object System.Text.UTF8Encoding $false))

Test-Path "$testDir\TestEntitySemMigration.java"

git reset HEAD
git add src/main/java/com/laboratorio/financas/test/TestEntitySemMigration.java
git status   # apenas TestEntitySemMigration.java staged

git commit -m "test: validacao destrutiva cenario 3 entity sem migration deveria bloquear"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 3 exit code: $exitCode (esperado: != 0)"
```

**Esperado:** commit **rejeitado**. Hook reporta `[ERRO]`, lista `TestEntitySemMigration.java`, cita licao 2.1, instrucao de fix, exemplo de migration.

Se aceito, **parar e investigar** — regex de `@Entity` ou logica errada.

#### Cenario 4: `.java` modificado (existente) COM `@Entity` SEM migration -> aceito (status M, hook nao dispara)

```powershell
# Modificar CategoriaEntity.java adicionando um comentario inocuo
$pathEntity = "$repoRoot\src\main\java\com\laboratorio\financas\categoria\infrastructure\persistence\CategoriaEntity.java"
$contentOriginal = [System.IO.File]::ReadAllText($pathEntity, [System.Text.UTF8Encoding]::new($false))
$contentModificado = "// Cenario 4 ADR-011 - sera revertido`n" + $contentOriginal
[System.IO.File]::WriteAllText($pathEntity, $contentModificado, (New-Object System.Text.UTF8Encoding $false))

# Confirmar que e modificacao (status M)
git status   # deve mostrar CategoriaEntity.java como modified

git reset HEAD
git add src/main/java/com/laboratorio/financas/categoria/infrastructure/persistence/CategoriaEntity.java
git status   # confirma "modified" (nao "new file")

git commit -m "test: validacao destrutiva cenario 4 entity modificada hook nao dispara"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 4 exit code: $exitCode (esperado: 0 — status M, hook nao dispara)"
```

**Esperado:** commit aceito. Hook detecta que `CategoriaEntity.java` tem status `M`, nao `A`, e nao dispara. Confirma empiricamente o escopo conservador da 4.7.

#### Cenario 5: 2 `.java` novos COM `@Entity` + 1 migration nova -> aceito (cobertura responsabilidade do dev)

```powershell
$conteudoEntity3 = @"
package com.laboratorio.financas.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class TestMultiOne {
    @Id
    private Long id;
}
"@

$conteudoEntity4 = @"
package com.laboratorio.financas.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class TestMultiTwo {
    @Id
    private Long id;
}
"@

[System.IO.File]::WriteAllText("$testDir\TestMultiOne.java", $conteudoEntity3, (New-Object System.Text.UTF8Encoding $false))
[System.IO.File]::WriteAllText("$testDir\TestMultiTwo.java", $conteudoEntity4, (New-Object System.Text.UTF8Encoding $false))

$conteudoMigration5 = "-- Smoke test migration cenario 5`nCREATE TABLE test_multi (id BIGINT PRIMARY KEY);`n"
[System.IO.File]::WriteAllText("$repoRoot\src\main\resources\db\migration\V998__test_multi.sql", $conteudoMigration5, (New-Object System.Text.UTF8Encoding $false))

git reset HEAD
git add src/main/java/com/laboratorio/financas/test/TestMultiOne.java
git add src/main/java/com/laboratorio/financas/test/TestMultiTwo.java
git add src/main/resources/db/migration/V998__test_multi.sql
git status

git commit -m "test: validacao destrutiva cenario 5 multiplas entities uma migration"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 5 exit code: $exitCode (esperado: 0)"
```

**Esperado:** commit aceito.

#### Cenario 6: override `--no-verify` no cenario 3 -> bypassa

```powershell
$conteudoEntity6 = @"
package com.laboratorio.financas.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class TestOverride {
    @Id
    private Long id;
}
"@

[System.IO.File]::WriteAllText("$testDir\TestOverride.java", $conteudoEntity6, (New-Object System.Text.UTF8Encoding $false))

git reset HEAD
git add src/main/java/com/laboratorio/financas/test/TestOverride.java
git status

git commit --no-verify -m "test: validacao destrutiva cenario 6 override no-verify"
$exitCode = $LASTEXITCODE
Write-Host "Cenario 6 exit code: $exitCode (esperado: 0)"
```

**Esperado:** commit aceito (bypass).

### Tarefa 13 — Limpeza dos commits e arquivos de teste

```bash
git log --oneline -10
```

Esperado (do mais recente):

```
<commit cenario 6>
<commit cenario 5>
<commit cenario 4>
<commit cenario 2>
<commit cenario 1>
<Commit 4>
<Commit 3>
<Commit 2>
<Commit 1>
<squash 4.6>
```

**5 commits de teste** (cenarios 1, 2, 4, 5, 6 — cenario 3 foi rejeitado, sem commit).

Restaurar `CategoriaEntity.java` ANTES do reset (essencial — reset volta history, mas working tree precisa estar limpo):

```powershell
Copy-Item "$pathEntity.backup" $pathEntity -Force
git diff src\main\java\com\laboratorio\financas\categoria\infrastructure\persistence\CategoriaEntity.java
# Esperado: vazio
```

Reset (sandbox-block, pedir execucao manual):

```bash
git branch --show-current  # confirmar feat/etapa-4-7-entity-migration-hook
git reset --hard HEAD~5
```

Antes de pedir, **contar manualmente** commits `test:` em `git log`. Se diferente de 5, parar e reportar.

Limpar arquivos de teste:

```powershell
Remove-Item "$repoRoot\src\main\java\com\laboratorio\financas\test" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item "$repoRoot\src\main\resources\db\migration\V999__test_smoke.sql" -ErrorAction SilentlyContinue
Remove-Item "$repoRoot\src\main\resources\db\migration\V998__test_multi.sql" -ErrorAction SilentlyContinue
Remove-Item "$pathEntity.backup" -ErrorAction SilentlyContinue
git status
```

**Pre-condicao ADR-011:** working tree limpo. `CategoriaEntity.java` deve estar **identico** ao estado original.

### Tarefa 14 — Validacao final antes de push

```bash
git status
git log --oneline -5
git config core.hooksPath
git diff src/main/java/com/laboratorio/financas/categoria/infrastructure/persistence/CategoriaEntity.java
git ls-files .claude/hooks/java-spring/
```

Esperado:
- Working tree limpo.
- 4 commits novos da 4.7.
- `core.hooksPath` retorna `.githooks`.
- `git diff CategoriaEntity.java` **vazio**.
- `git ls-files .claude/hooks/java-spring/` retorna 2 arquivos: `maven-release.ps1`, `entity-migration.ps1`.

## Restricoes e freios

1. **Nao criar outros hooks** alem do `entity-migration`.

2. **Nao tocar em hooks existentes** (`conventional-commits`, `encoding-utf8`, `markdown-blank-lines`, `docs-size`, `maven-release`).

3. **Nao tocar em entrypoints**, scripts, `pom.xml`, `frontend/`, `docker-compose.yml`, ADRs, `.gitignore`, `.gitattributes`, `.githooks/README.md`.

4. **Nao tocar em `CLAUDE.md`** (esta sub-etapa nao muda stack/ambiente/convencoes/restricoes — regra de manutencao da 4.6 se aplica).

5. **Nao tocar em `src/`** EXCETO durante cenario 4 da validacao destrutiva, com backup obrigatorio + restauracao.

6. **Nao introduzir dependencias externas.** PowerShell puro + .NET nativo.

7. **Encoding UTF-8 sem BOM** no `entity-migration.ps1`.

8. **APENAS ASCII** em strings de mensagens do hook (licao 4.4).

9. **`@(...)` antes de `.Count`** (licao 4.3) em todos os pontos: `$javaFiles`, `$entitiesNovas`, `$migrationsNovas`.

10. **`[System.Environment]::CurrentDirectory` sincronizado** antes de qualquer `WriteAllText/ReadAllText` com path relativo (ADR-011).

11. **Nao usar `Write-Error` + `exit`.** Padrao: `Write-Host -ForegroundColor` + `exit N`.

12. **Nao usar `pwsh`.** Apenas `powershell` (PS5.1).

13. **Hook DEVE retornar exit 0** quando nenhum `.java` novo (status A) esta no diff, OU quando nenhum `.java` novo tem `@Entity`. Modo fail aplica **apenas** quando gatilho dispara.

14. **`git reset --hard` apenas na branch da etapa.** Sandbox-block esperado; aprovacao manual.

15. **Backup do `CategoriaEntity.java` obrigatorio** antes do cenario 4. Restauracao via `Copy-Item` do backup + `git diff` confirmando vazio. Se `git restore` for usado em alternativa, validar `git diff` igualmente.

16. **Validacao destrutiva COMPLETA (6 cenarios)** e gate de pronto. Reportar saidas no PR body incluindo pre-condicoes.

17. **`git reset HEAD` entre cenarios** — staging isolado (licao 4.3).

18. **Nao tomar decisao silenciosa em zona limitrofe.** Se algum cenario comportar diferente do esperado, parar e reportar.

19. **Ordem cronologica em `progresso.md`:** 4.0 -> 4.0.1 -> 4.1 -> 4.2 -> 4.2.1 -> 4.3 -> 4.4 -> 4.5 -> 4.6 -> 4.7.

20. **Logica de validacao fica em `entity-migration.ps1`**, orquestrador apenas referencia.

21. **Nao sugerir proxima sub-etapa** espontaneamente.

22. **Antes de escrever cada arquivo, ler arquivo vivo** (Tarefa 4).

23. **`hooks-pendentes.md`:** item da Etapa 2.1 e **substituido** por versao qualificada em "Pendentes" (caso edge de modificacao). NAO simplesmente removido. Texto qualificado conforme escopo. Se texto exato da linha 86 divergir do esperado, parar e reportar (analogo ao achado da 4.5 sobre numeracao 2.5 vs 1.4).

24. **Cenario 4 toca codigo real (`CategoriaEntity.java`).** Backup obrigatorio. Restauracao validada via `git diff` vazio antes de seguir.

25. **Pacote `test/` deve ficar fora do build se o build for executado.** Como `mvn verify` nao roda nos cenarios destrutivos (so `git commit`), nao ha risco imediato. Mas se algum cenario provocar build acidentalmente, classes mock podem quebrar compilacao por falta de dependencias. **Nao rodar `.\scripts\check.ps1` enquanto pacote `test/` mock existir.**

26. **Validacao final (Tarefa 14)** confirma `git diff CategoriaEntity.java` vazio E `git ls-files .claude/hooks/java-spring/` lista os 2 hooks esperados.

## Estrutura de commits

Branch: `feat/etapa-4-7-entity-migration-hook`

**Commit 1** — `feat(claude): adiciona hook java-spring de @entity sem migration flyway`
- `.claude/hooks/java-spring/entity-migration.ps1` (novo)

**Commit 2** — `feat(githooks): adiciona entity-migration ao orquestrador pre-commit`
- `.githooks/pre-commit.ps1` (1 linha)

**Commit 3** — `docs: registra entity-migration e padrao de escopo reduzido vs licao original`
- `docs/decisoes.md`
- `docs/hooks-pendentes.md`

**Commit 4** — `docs: registra sub-etapa 4.7 em progresso e versiona prompt`
- `docs/progresso.md`
- `docs/prompt-etapa-4-7.md`

## Validacao antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -5
git config core.hooksPath
git diff src/main/java/com/laboratorio/financas/categoria/infrastructure/persistence/CategoriaEntity.java
```

Esperado:
- `check.ps1` passa.
- Working tree limpo.
- 4 commits novos.
- `git diff CategoriaEntity.java` vazio.
- `pom.xml` real do projeto **identico** ao estado original.

## PR

Titulo: `feat: sub-etapa 4.7 — hook java-spring de @entity sem migration flyway (modo conservador)`

Body sugerido:

```markdown
## Summary

Implementa o **sexto hook funcional** do projeto e **segundo de stack** (java-spring): `@Entity` sem migration Flyway. **Primeiro hook com decisao consciente de escopo reduzido vs licao original.**

### Regra implementada

- **Gatilho:** `.java` novo (status A no diff staged) sob `src/main/java/` contendo `@Entity`.
- **Validacao:** exige pelo menos um arquivo `src/main/resources/db/migration/V<n>__*.sql` novo (status A) no mesmo commit.
- **Modo:** fail. Bloqueia commit em violacao.
- **Razao:** licao 2.1 — nova `@Entity` sem migration cria divergencia entre codigo e schema. Prod com `ddl-auto=validate` quebra ao subir.
- **Override:** `git commit --no-verify`.

### Escopo conscientemente reduzido vs licao original

Texto da licao 2.1 em `docs/hooks-pendentes.md` linha 86 (antes desta sub-etapa) menciona: "Modificacao de `@Entity` JPA exige migration Flyway no mesmo PR".

Esta sub-etapa implementa apenas o caso de Entity **nova** (status A), nao modificacao (status M).

**Razao:** status M produz falso positivo alto. Refatoracao cosmetica de Entity existente (rename de variavel, comentario, formatacao) nao requer migration — mas hook nao distingue isso sem parser de Java. Forcar criacao de migration vazia destruiria confianca rapidamente.

Caso "modificacao de Entity existente requer migration" fica como **debito conscientemente aceito**, registrado em `hooks-pendentes.md` na lista "Pendentes". Se aparecer dor real, sub-etapa futura calibra implementacao sofisticada.

### Hook puramente preventivo

Auditoria pre-redacao confirmou ratio coerente no projeto: 3 Entities (Categoria, Conta, Transacao) + 4 migrations (V1__schema_inicial + 1 por Entity). Sem debito de Camada 2. Hook arma regra para Camada 4+ quando novas Entities entrarem.

### Validacao destrutiva sob ADR-011

Seis cenarios com pre-condicoes explicitas, incluindo **modificacao de Entity real** (Categoria.java) para confirmar empiricamente o escopo conservador:

1. **`.java` novo SEM `@Entity`** — aceito.
2. **`.java` novo COM `@Entity` + migration nova** — aceito.
3. **`.java` novo COM `@Entity` SEM migration** — rejeitado. Hook reporta licao 2.1 + exemplo de fix.
4. **`CategoriaEntity.java` modificado (status M)** — aceito. Hook nao dispara em status M. Confirma escopo conservador.
5. **2 `.java` novos COM `@Entity` + 1 migration nova** — aceito. Cobertura e responsabilidade do dev.
6. **`--no-verify` em cenario 3** — bypassa.

Todos passaram. Backup do `CategoriaEntity.java` obrigatorio antes; restauracao validada via `git diff` vazio.

### Aplicacao das licoes acumuladas

- Licao 4.1: `powershell`, nao `pwsh`.
- Licao 4.2: UTF-8 sem BOM.
- Licao 4.3: `@(...)` antes de `.Count` em 3 pontos.
- Licao 4.4: apenas ASCII em mensagens — validado explicitamente apos criar o hook.
- ADR-011: pre-condicoes em cada cenario, `Environment.CurrentDirectory` sincronizado, backup obrigatorio para cenario que toca codigo real.

### Mudancas

- `.claude/hooks/java-spring/entity-migration.ps1`: logica real. Le `git diff --cached --diff-filter=A`, filtra `.java` em `src/main/java/`, detecta `@Entity` via regex `(?m)^\s*@Entity\b`, exige migration nova em `src/main/resources/db/migration/V<n>__*.sql`.
- `.githooks/pre-commit.ps1`: adiciona 1 linha ao array `$hooks`. Quinto hook no orquestrador.
- `docs/decisoes.md`: subsecao "@Entity sem migration Flyway (Sub-etapa 4.7)" + nota explicita sobre escopo reduzido + debito de modificacao de Entity existente.
- `docs/hooks-pendentes.md`: item da Etapa 2.1 substituido por versao qualificada em "Pendentes" (caso edge — modificacao). Novo item em "Implementados" com qualificador "modo conservador".
- `docs/progresso.md`: sub-etapa 4.7 em "Sub-etapas concluidas". Licoes da 4.7.

### CLAUDE.md NAO atualizado

Esta sub-etapa nao muda stack, ambiente, convencoes ou restricoes — apenas adiciona hook (caso volatil delegado para `hooks-pendentes.md`). Conforme regra de manutencao formalizada na 4.6, CLAUDE.md nao e tocado.

### Validacao destrutiva pos-merge sugerida

Em qualquer branch (nao main), com `[System.Environment]::CurrentDirectory = (Get-Location).Path` sincronizado:

\```powershell
# Cenario A: java novo SEM @Entity passa
[System.IO.File]::WriteAllText("$PWD\src\main\java\com\laboratorio\financas\Smoke47A.java", "package com.laboratorio.financas; public class Smoke47A {}", (New-Object System.Text.UTF8Encoding $false))
git reset HEAD
git add "src/main/java/com/laboratorio/financas/Smoke47A.java"
git commit -m "test: smoke 4.7 java sem entity"  # aceito

# Cenario B: java novo COM @Entity SEM migration bloqueia
$entity = "package com.laboratorio.financas; import jakarta.persistence.Entity; @Entity public class Smoke47B {}"
[System.IO.File]::WriteAllText("$PWD\src\main\java\com\laboratorio\financas\Smoke47B.java", $entity, (New-Object System.Text.UTF8Encoding $false))
git reset HEAD
git add "src/main/java/com/laboratorio/financas/Smoke47B.java"
git commit -m "test: smoke 4.7 entity sem migration"  # rejeitado citando licao 2.1

# Limpeza
git reset --hard HEAD~1
Remove-Item "src/main/java/com/laboratorio/financas/Smoke47A.java", "src/main/java/com/laboratorio/financas/Smoke47B.java" -ErrorAction SilentlyContinue
\```

### Proximo passo

Decisao fora deste PR. Possiveis caminhos:
- Mais hooks de stack: sufixo `Test`/`IT`, Lombok/MapStruct, Maven Central.
- Subagents (`pr-reviewer`, `architect-reviewer`).
- Skills (`/ship`, `/feature`).
- Claude Code hooks nativos.

Calibracao em sessao separada.
```

## Pos-criacao do PR

1. Abrir PR via `gh pr create`.
2. Capturar o numero.
3. Editar `docs/decisoes.md`, `docs/hooks-pendentes.md`, `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>` e `2026-MM-DD` pela data real.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorizacao explicita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `feat/etapa-4-7-entity-migration-hook` empurrada com 5 commits (4 + 1 update do PR).
- PR aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.6.
- Working tree limpo.
- `CategoriaEntity.java` **identico** ao estado em main (backup restaurado).
- `.claude/hooks/java-spring/` contem 2 arquivos: `maven-release.ps1`, `entity-migration.ps1`.
- `src/main/resources/db/migration/` ainda contem **4** arquivos (V1-V4) — V998 e V999 deletados.
- Pasta `src/main/java/.../test/` nao existe (deletada recursivamente).
- Reportar com `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup`, saidas dos 6 cenarios com pre-condicoes, e `git diff CategoriaEntity.java` vazio.

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao criar prompt da proxima sub-etapa.
- Nao criar outros hooks (de stack ou universal).
- Nao criar subagents, skills.
- Nao tocar em `CLAUDE.md` (regra 4.6).
- Nao tocar em scripts existentes.
- Nao tocar em arquivos das sub-etapas anteriores (alem de Categoria.java durante cenario 4, com restauracao).
- Nao tocar em `.gitignore`, `.gitattributes`, ADRs.
- Nao deixar `CategoriaEntity.java` modificado.
- Nao deixar `CategoriaEntity.java.backup` no working tree.
- Nao deixar pasta `src/main/java/.../test/` no working tree.
- Nao deixar `V998__test_multi.sql` ou `V999__test_smoke.sql` no working tree.
- Nao deixar commits `test:` no historico — limpar via `git reset --hard` com aprovacao manual.
- Nao sugerir proximo passo espontaneamente.
- Nao pular pre-condicoes ADR-011.
- Nao esquecer `@(...)` antes de `.Count`.
- Nao usar caracteres nao-ASCII em mensagens do hook.
- Nao usar `pwsh`.
- Nao executar `.\scripts\check.ps1` enquanto pacote `test/` mock existir.
