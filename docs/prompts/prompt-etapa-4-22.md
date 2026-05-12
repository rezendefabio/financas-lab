# Prompt -- Sub-etapa 4.22: Hook post-edit (unit tests apos edicao de domain)

## Contexto

Sub-etapa 4.22 da Camada 3. Entrega o hook post-edit: quando Claude Code edita um
arquivo em `*/domain/*.java`, o hook dispara automaticamente e roda o unit test
correspondente. Silencioso quando nao ha teste correspondente.

Mecanismo: Claude Code native hook (`PostToolUse`) configurado em
`.claude/settings.json`. Diferente dos git hooks (4.1-4.7): aquele atua no
`git commit`; este atua em tempo real durante a sessao do Claude Code.

`settings.json` e gitignored (decisao da 4.0). A configuracao e gerada pelo
`setup.ps1`, mantendo o padrao existente (setup.ps1 ja configura `core.hooksPath`).

Lembrete de workflow: passar este prompt como TEXTO ao executor. Commitar o `.md`
manualmente apos o PR estar aberto.

---

## Padroes que estreiam

**Primeiro hook nativo do Claude Code no projeto.** Todos os hooks anteriores (4.1-4.7)
sao git hooks (pre-commit, commit-msg). Este usa o evento `PostToolUse` do harness do
Claude Code — executa apos cada uso de `Edit` ou `Write`, com acesso ao path do arquivo
editado via stdin JSON.

**PostToolUse nunca bloqueia** — a tool ja rodou. O hook fornece feedback (testes
passando ou falhando), nao impede a acao.

**setup.ps1 como gestor de configuracao local.** Padrao ja existente (`core.hooksPath`).
A 4.22 adiciona um segundo bloco ao setup: cria `.claude/settings.json` se nao existir.

---

## Escopo decidido

### Arquivo 1: `.claude/hooks/post-edit/run-tests.ps1` (NOVO)

```powershell
# .claude/hooks/post-edit/run-tests.ps1
# Hook PostToolUse: roda unit test quando arquivo em */domain/*.java e editado.
# Silencioso se nao ha teste correspondente.

param()

$ErrorActionPreference = "Continue"

$stdin = [Console]::In.ReadToEnd()
if (-not $stdin) { exit 0 }

try {
    $data = $stdin | ConvertFrom-Json
} catch {
    exit 0
}

$filePath = $data.tool_input.file_path
if (-not $filePath) { exit 0 }

# Apenas arquivos em */domain/*.java dentro de src/main/java/
if ($filePath -notmatch 'src[/\\]main[/\\]java[/\\].*[/\\]domain[/\\][^/\\]+\.java$') {
    exit 0
}

$className = [System.IO.Path]::GetFileNameWithoutExtension($filePath)
$testFilePath = $filePath -replace '(src[/\\])main([/\\]java[/\\])', '$1test$2'
$testFilePath = $testFilePath -replace '\.java$', 'Test.java'

if (-not (Test-Path $testFilePath)) {
    exit 0
}

$testClassName = "${className}Test"
Write-Host "[post-edit] Rodando $testClassName..."

[System.Environment]::CurrentDirectory = (Get-Location).Path
$output = & .\mvnw test "-Dtest=$testClassName" 2>&1
$exit = $LASTEXITCODE

if ($exit -eq 0) {
    Write-Host "[post-edit] $testClassName PASSOU"
} else {
    Write-Host "[post-edit] $testClassName FALHOU:"
    $output | ForEach-Object { Write-Host "  $_" }
}

exit 0
```

### Arquivo 2: `scripts/setup.ps1` (EDITAR)

Adicione o bloco abaixo ENTRE o bloco `core.hooksPath` e o bloco `Docker Compose`.
Ou seja, logo apos a linha `Write-Host "[OK] core.hooksPath configurado para .githooks/" -ForegroundColor Green`.

Garanta uma linha em branco antes do novo `Write-Host "==>..."`.

```powershell
Write-Host "==> Configurando hook post-edit do Claude Code..." -ForegroundColor Cyan
$settingsPath = ".claude/settings.json"
if (-not (Test-Path $settingsPath)) {
    [System.Environment]::CurrentDirectory = (Get-Location).Path
    $settingsContent = @'
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "powershell.exe",
            "args": ["-NoProfile", "-File", ".claude/hooks/post-edit/run-tests.ps1"],
            "timeout": 60
          }
        ]
      }
    ]
  }
}
'@
    [System.IO.File]::WriteAllText(
        $settingsPath,
        $settingsContent,
        (New-Object System.Text.UTF8Encoding $false)
    )
    if (-not (Test-Path $settingsPath)) {
        Write-Host "[ERRO] Falha ao criar .claude/settings.json." -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] .claude/settings.json criado com hook post-edit." -ForegroundColor Green
} else {
    Write-Host "[OK] .claude/settings.json ja existe -- nao sobrescrito." -ForegroundColor Green
}
Write-Host ""
```

### Arquivo 3: `docs/progresso.md` (EDITAR)

**Mudanca 1 -- linha "Ultima atualizacao":**
```
**Última atualização:** 2026-05-12 (Sub-etapa 4.22 -- Hook post-edit para unit tests)
```

**Mudanca 2 -- marcar hook post-edit como concluido** (secao "Criterios de pronto"):

Substitua:
```
- [ ] Hook post-edit rodando testes do arquivo mexido
```
Por:
```
- [x] Hook post-edit rodando testes do arquivo mexido -- concluido 4.22
```

**Mudanca 3 -- adicionar 4.22 em "Sub-etapas concluidas"** (logo antes da entrada 4.21):

```
- **4.22 -- Hook post-edit para unit tests em domain** (2026-05-12): primeiro hook
  nativo do Claude Code no projeto (`PostToolUse`, evento pos-edicao). Dispara apos
  uso de `Edit` ou `Write` em `*/domain/*.java`; roda unit test correspondente via
  `mvnw test -Dtest=<Classe>Test`; silencioso quando sem teste ou arquivo fora do
  escopo. Nunca bloqueia (PostToolUse e non-blocking por design). Configuracao em
  `.claude/settings.json` (gitignored, gerado pelo `setup.ps1`). Script versionado
  em `.claude/hooks/post-edit/run-tests.ps1`. Timeout 60s. Padrao novo: hook nativo
  Claude Code vs git hook (4.1-4.7). PR #68.
```

**Mudanca 4 -- adicionar em "Historico de mudancas":**

```
- **2026-05-12** -- Sub-etapa 4.22 concluida: hook post-edit (`PostToolUse`) para unit
  tests em `*/domain/*.java`. Primeiro hook nativo Claude Code do projeto. `setup.ps1`
  ampliado para gerar `.claude/settings.json`. Script em `.claude/hooks/post-edit/`.
  CLAUDE.md NAO atualizado. PR #68.
```

### Arquivo 4: `docs/decisoes-claude-code.md` (EDITAR)

Adicione subsecao antes do "Historico de mudancas" (linha em branco antes e depois
de cada `##`):

```
## Sub-etapa 4.22 -- Hook post-edit (hook nativo Claude Code)

### Git hook vs hook nativo Claude Code

Hooks anteriores (4.1-4.7) sao **git hooks**: disparam em eventos do git
(`pre-commit`, `commit-msg`), configurados via `core.hooksPath=.githooks`.
Vivem em `.githooks/` (entrypoints) e `.claude/hooks/` (logica).

O hook post-edit (4.22) e um **hook nativo do Claude Code**: dispara em eventos
do harness (`PostToolUse`), configurado em `.claude/settings.json`. Vive em
`.claude/hooks/post-edit/run-tests.ps1`.

### Decisao: PostToolUse e non-blocking

PostToolUse nunca bloqueia a execucao do Claude Code -- a tool (Edit/Write) ja
rodou quando o hook e chamado. O hook fornece feedback (testes passando/falhando)
mas nao impede nenhuma acao. Modo equivalente a `warn` dos git hooks.

### Decisao: settings.json via setup.ps1

`.claude/settings.json` e gitignored (decisao da 4.0 -- configs locais pessoais
nao versionadas). A configuracao do hook e gerada pelo `setup.ps1` (idempotente:
cria se nao existe, pula se ja existe). Script do hook (`.claude/hooks/post-edit/
run-tests.ps1`) e versionado -- apenas o `settings.json` que o referencia e local.

### Escopo: apenas domain, apenas unit

- Domain files (`*/domain/*.java`): unit tests rapidos, sem Docker, sem Testcontainers.
- Infrastructure files (`*RepositoryImpl.java`): EXCLUIDOS -- integration tests lentos
  demais para hook post-edit.
- Arquivos sem teste: hook silencioso (sem ruido).
```

Adicione ao "Historico de mudancas":

```
- **2026-05-12** -- Sub-etapa 4.22 concluida: primeiro hook nativo Claude Code
  (`PostToolUse`). Git hook vs hook nativo documentado. `setup.ps1` como gestor de
  `settings.json`. Escopo domain-only. PR #68.
```

### Arquivo 5: `docs/hooks-pendentes.md` (EDITAR)

Leia o arquivo antes de editar. Localize a secao `## Hooks implementados` e adicione
ao final da lista (apos o item do hook de tamanho de docs):

```
- **Hook post-edit unit tests** (Sub-etapa 4.22, PR #68). Hook nativo Claude Code
  (`PostToolUse`) em `.claude/hooks/post-edit/run-tests.ps1`, referenciado por
  `.claude/settings.json` (gitignored, gerado pelo `setup.ps1`). Dispara apos `Edit`
  ou `Write` em `*/domain/*.java` dentro de `src/main/java/`. Roda `mvnw test
  -Dtest=<Classe>Test` se arquivo de teste existir; silencioso caso contrario.
  Timeout 60s. Non-blocking (PostToolUse nao bloqueia por design). Escopo futuro:
  integration tests para `*RepositoryImpl.java` se performance permitir.
```

---

## Estado esperado ao iniciar

```powershell
git branch --show-current   # deve retornar: main
git status                  # deve retornar: nothing to commit, working tree clean
git log --oneline -1        # deve mostrar squash do PR #67 (4.21) no topo
```

---

## Tarefas

### Tarefa 1 -- Verificar estado inicial (ADR-011)

```powershell
git branch --show-current
git status
git log --oneline -1
```

### Tarefa 2 -- Criar branch

```powershell
git checkout -b feat/etapa-4-22-hook-post-edit
git branch --show-current   # deve retornar: feat/etapa-4-22-hook-post-edit
```

### Tarefa 3 -- Criar `.claude/hooks/post-edit/run-tests.ps1`

Pre-condicao:
```powershell
Test-Path ".claude/hooks/post-edit/"  # deve retornar: False
```

```powershell
New-Item -ItemType Directory -Path ".claude/hooks/post-edit/"
```

Use Write para criar `.claude/hooks/post-edit/run-tests.ps1` com o conteudo prescrito.
Codificacao UTF-8 sem BOM.

Pos-condicao:
```powershell
Test-Path ".claude/hooks/post-edit/run-tests.ps1"  # deve retornar: True
```

### Tarefa 4 -- Editar `scripts/setup.ps1`

Leia o arquivo antes de editar. Localize a linha:
```
Write-Host "[OK] core.hooksPath configurado para .githooks/" -ForegroundColor Green
```

Use Edit para inserir o bloco prescrito logo apos essa linha (com linha em branco
separando do proximo bloco `Write-Host "==> Subindo servicos..."`).

Pos-condicao -- confirme que o bloco foi inserido:
```powershell
Select-String "settings.json" "scripts/setup.ps1" | Measure-Object | Select-Object -Expand Count
# deve retornar > 0
```

### Tarefa 5 -- Primeiro commit

```powershell
git add ".claude/hooks/post-edit/run-tests.ps1" "scripts/setup.ps1"
git status
# deve mostrar exatamente esses 2 arquivos staged
```

Commit (scope `claude` sem ponto para o hook; scope `scripts` para o setup):
```
feat(claude): adiciona hook post-edit para unit tests apos edicao de domain
```

### Tarefa 6 -- Validacao destrutiva do hook (smoke)

**Cenario 1 -- arquivo nao-domain (deve ser silencioso):**

Edite um arquivo em `application/`, por exemplo
`src/main/java/com/laboratorio/financas/conta/application/CriarContaUseCase.java`.
Adicione um comentario vazio no final (ex: `// smoke 4.22`).
O hook deve ser silencioso (sem output `[post-edit]`).
Reverta a mudanca usando Edit (remova o comentario).

**Cenario 2 -- arquivo domain com teste existente:**

Edite `src/main/java/com/laboratorio/financas/conta/domain/Conta.java`.
Adicione um comentario vazio no final (ex: `// smoke 4.22`).
O hook deve disparar e exibir:
```
[post-edit] Rodando ContaTest...
[post-edit] ContaTest PASSOU
```
Reverta a mudanca usando Edit (remova o comentario).

**Criterios de sucesso do smoke:**
- [ ] Cenario 1: hook silencioso em arquivo nao-domain
- [ ] Cenario 2: hook dispara, ContaTest roda e passa
- [ ] Working tree limpa apos reversoes

Se qualquer criterio falhar: reporte erro literal. Nao tente auto-corrigir.

### Tarefa 7 -- Validar setup.ps1 com simulacao local

Execute o setup.ps1 (ou apenas o bloco de settings.json) para confirmar que
`.claude/settings.json` e criado corretamente:

```powershell
# Remove temporariamente se ja existir de sessoes anteriores
if (Test-Path ".claude/settings.json") {
    Write-Host "settings.json ja existe -- testando idempotencia (nao removendo)"
} else {
    Write-Host "settings.json nao existe -- setup.ps1 devera criar"
}
```

Execute o bloco do setup.ps1 isoladamente (copie e rode as linhas do bloco
`Configurando hook post-edit`). Confirme:
```powershell
Test-Path ".claude/settings.json"  # deve retornar: True
Get-Content ".claude/settings.json" -Encoding UTF8 | Select-Object -First 5
# deve mostrar o JSON com "hooks" e "PostToolUse"
```

ATENCAO: `.claude/settings.json` e gitignored -- `git status` nao vai mostrar
esse arquivo. Isso e esperado.

### Tarefa 8 -- Atualizar docs

Aplique as mudancas prescritas nos Arquivos 3, 4 e 5 (`progresso.md`,
`decisoes-claude-code.md`, `hooks-pendentes.md`). Leia cada arquivo antes de editar.

Pos-condicao:
```powershell
Select-String "4.22" "docs/progresso.md" | Measure-Object | Select-Object -Expand Count
Select-String "4.22" "docs/decisoes-claude-code.md" | Measure-Object | Select-Object -Expand Count
Select-String "post-edit" "docs/hooks-pendentes.md" | Measure-Object | Select-Object -Expand Count
# todos devem retornar > 0
```

### Tarefa 9 -- Segundo e terceiro commits

```powershell
git add "docs/progresso.md"
git status
```
Commit:
```
docs(progresso): registra sub-etapa 4.22 e criterio hook post-edit como concluido
```

```powershell
git add "docs/decisoes-claude-code.md" "docs/hooks-pendentes.md"
git status
```
Commit:
```
docs(decisoes): registra hook nativo Claude Code inaugurado na 4.22
```

### Tarefa 10 -- Validacao pre-ship

```powershell
git log --oneline feat/etapa-4-22-hook-post-edit ^main
# deve mostrar exatamente 3 commits

git diff main --name-only
# deve mostrar exatamente:
#   .claude/hooks/post-edit/run-tests.ps1
#   docs/decisoes-claude-code.md
#   docs/hooks-pendentes.md
#   docs/progresso.md
#   scripts/setup.ps1

git status
# deve retornar: nothing to commit, working tree clean

Select-String "PostToolUse" ".claude/hooks/post-edit/run-tests.ps1"
# deve ter match

Select-String "domain" ".claude/hooks/post-edit/run-tests.ps1"
# deve ter match

Select-String "settings.json" "scripts/setup.ps1"
# deve ter match
```

### Tarefa 11 -- Entregar via `/ship`

```
/ship
```

---

## Restricoes e freios

- NAO usar scope `.claude` em commits -- usar `claude` sem ponto (licao 4.19).
- NAO commitar `.claude/settings.json` -- e gitignored propositalmente.
- NAO modificar o CLAUDE.md.
- NAO editar bounded contexts existentes alem das mudancas de smoke (revertidas).
- Se hook bloquear commit: leia a mensagem, corrija sem `--no-verify`.
- Smoke falhou? Reporte erro literal. Nao tente auto-corrigir em loop.

---

## Estrutura de commits

```
feat(claude): adiciona hook post-edit para unit tests apos edicao de domain
docs(progresso): registra sub-etapa 4.22 e criterio hook post-edit como concluido
docs(decisoes): registra hook nativo Claude Code inaugurado na 4.22
```

---

## Estado esperado ao terminar

- PR #68 aberto (via `/ship`).
- Working tree limpa.
- `.claude/hooks/post-edit/run-tests.ps1` existente e versionado.
- `scripts/setup.ps1` com bloco de settings.json.
- `docs/progresso.md`, `docs/decisoes-claude-code.md`, `docs/hooks-pendentes.md`
  atualizados.
- `.claude/settings.json` existente localmente (NAO versionado -- gitignored).

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO commitar o arquivo de prompt na branch (operador faz manualmente).
- NAO rodar `/ship` mais de uma vez.
