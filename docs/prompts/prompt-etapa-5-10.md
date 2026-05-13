# Prompt -- Sub-etapa 5.10: Hook de secret scanning (pre-commit)

## Contexto

Hook pre-commit que bloqueia commits com credenciais literais em codigo-fonte.
Motivacao: durante a 5.9, um origin hardcoded (`http://localhost:3000`) chegou ao
PR sem ser barrado. Secret scanning e o primeiro passo da infra de seguranca do
front que impede que chaves de API, senhas e tokens vazem para o repositorio.

Camada 4. Sem novo bounded context. Apenas infra de hook.

---

## O que implementar

### Novo hook: `.claude/hooks/universal/secret-scanning.ps1`

**Comportamento geral:**
- Roda em modo `fail` (bloqueia commit se violacao encontrada).
- Le arquivos staged via `git diff --cached --name-only --diff-filter=ACM`.
- Para cada arquivo, le o conteudo staged via `git show :arquivo`.
- Aplica os padroes abaixo linha a linha.
- Reporta cada violacao no formato: `[ARQUIVO] linha N: <descricao do padrao>`
- Ao final, se houver qualquer violacao: imprime mensagem de instrucao e `exit 1`.

**Extensoes monitoradas (whitelist de extensao):**
`.java`, `.ts`, `.tsx`, `.js`, `.jsx`, `.properties`, `.yml`, `.yaml`, `.json`

**Arquivos excluidos (ignorar sempre, nao aplicar padroes):**
- Qualquer caminho contendo `src/test/` (senhas de teste sao esperadas)
- Qualquer arquivo cujo nome termine em `.example` ou `-example.*`
- `*.env.example`

**Padroes que causam `fail` (aplicar em ordem, regex PowerShell):**

| ID | Regex (case-insensitive) | Descricao |
|----|--------------------------|-----------|
| P1 | `-----BEGIN .*(PRIVATE\|RSA) KEY-----` | Chave PEM privada |
| P2 | `AKIA[0-9A-Z]{16}` | AWS Access Key ID |
| P3 | `(ghp\|ghs\|gho\|ghu\|ghr)_[A-Za-z0-9]{36,}` | GitHub Personal/OAuth token |
| P4 | `sk-[A-Za-z0-9]{32,}` | OpenAI/Anthropic API key pattern |
| P5 | `password\s*[=:]\s*["'][^\$\{][^"']{7,}["']` | password= com valor literal nao-placeholder (>= 8 chars) |
| P6 | `(secret\|api.?key)\s*[=:]\s*["'][^\$\{][^"']{7,}["']` | secret= ou apiKey= com valor literal |

**Notas sobre os padroes:**
- P5 e P6 ignoram valores que comecam com `$` ou `{` (sao placeholders Spring/env).
- Todos os padroes sao `case-insensitive` (`-imatch` ou `-replace` com flag `i`).
- Nao tentar parsear comentarios -- false positives em comentarios sao aceitaveis
  (o autor remove ou adiciona excecao explicita).

**Mensagem de instrucao ao bloquear:**
```
[secret-scanning] Credenciais literais detectadas. Mova para application.properties
com @Value ou para variavel de ambiente. Para falso positivo documentado,
adicione o arquivo a lista de exclusao em .claude/hooks/universal/secret-scanning.ps1.
```

**Estrutura do script** (seguir padrao de `encoding-utf8.ps1`):
```powershell
$ErrorActionPreference = "Stop"

# ... (ler staged files, tratar $LASTEXITCODE igual ao encoding-utf8.ps1)

$extensoesMonitoradas = @(".java", ".ts", ".tsx", ".js", ".jsx",
                          ".properties", ".yml", ".yaml", ".json")

$padroes = @(
    @{ Id = "P1"; Regex = "-----BEGIN .*(PRIVATE|RSA) KEY-----";     Desc = "Chave PEM privada" },
    @{ Id = "P2"; Regex = "AKIA[0-9A-Z]{16}";                        Desc = "AWS Access Key ID" },
    @{ Id = "P3"; Regex = "(ghp|ghs|gho|ghu|ghr)_[A-Za-z0-9]{36,}"; Desc = "GitHub token" },
    @{ Id = "P4"; Regex = "sk-[A-Za-z0-9]{32,}";                     Desc = "OpenAI/Anthropic API key" },
    @{ Id = "P5"; Regex = 'password\s*[=:]\s*["''][^\$\{][^"'']{7,}["'']'; Desc = "Password literal" },
    @{ Id = "P6"; Regex = '(secret|api.?key)\s*[=:]\s*["''][^\$\{][^"'']{7,}["'']'; Desc = "Secret/API key literal" }
)

$failed = $false

foreach ($file in $stagedFiles) {
    # checar extensao e exclusoes
    # ler conteudo com: git show ":$file"
    # iterar linhas, aplicar padroes
    # acumular $problems
}

if ($failed) {
    Write-Host "[secret-scanning] ..." -ForegroundColor Red
    exit 1
}

exit 0
```

Leia `encoding-utf8.ps1` antes de implementar para copiar o padrao exato de
`git diff --cached`, tratamento de `$LASTEXITCODE`, e iteracao sobre arquivos.

---

### Atualizar `.githooks/pre-commit.ps1`

Adicionar o novo hook ao array `$hooks`, logo apos `entity-migration.ps1`:

```powershell
".claude\hooks\universal\secret-scanning.ps1"
```

Leia o arquivo antes de editar.

---

### Atualizar `docs/hooks-pendentes.md`

Na secao "Hooks implementados", adicionar entrada para o novo hook seguindo o
padrao das entradas existentes. Incluir:
- Nome: **Secret Scanning**
- Sub-etapa: 5.10
- Caminho: `.claude/hooks/universal/secret-scanning.ps1`
- Comportamento: extensoes monitoradas, 6 padroes (P1-P6), exclusoes (src/test/, *.example)
- Modo: **fail**

Leia o arquivo antes de editar. Respeitar linha em branco antes e depois de headers
(hook markdown-blank-lines esta ativo).

---

## Validacao destrutiva

Apos implementar, executar os cenarios abaixo. Cada um deve ter resultado anotado
no body do PR.

**Cenario 1 -- chave PEM (deve bloquear):**
```powershell
# criar arquivo temporario staged com chave PEM falsa
$conteudo = "String x = `"-----BEGIN RSA PRIVATE KEY-----`";"
Set-Content -Path "src/main/java/Test.java" -Value $conteudo -Encoding UTF8
git add src/main/java/Test.java
git commit -m "test: secret scan"
# esperado: bloqueado com mensagem [secret-scanning]
git restore --staged src/main/java/Test.java
git restore src/main/java/Test.java
```

**Cenario 2 -- password literal (deve bloquear):**
```powershell
$conteudo = 'String pwd = "minhasenha123";'
Set-Content -Path "src/main/java/Test.java" -Value $conteudo -Encoding UTF8
git add src/main/java/Test.java
git commit -m "test: secret scan"
# esperado: bloqueado
git restore --staged src/main/java/Test.java
git restore src/main/java/Test.java
```

**Cenario 3 -- placeholder Spring (NAO deve bloquear):**
```powershell
$conteudo = 'String pwd = "${app.db.password}";'
Set-Content -Path "src/main/java/Test.java" -Value $conteudo -Encoding UTF8
git add src/main/java/Test.java
git commit -m "test: placeholder ok"
# esperado: commit passa (placeholder nao e literal)
git reset HEAD~1
git restore src/main/java/Test.java
```

**Cenario 4 -- arquivo de teste (NAO deve bloquear):**
```powershell
$conteudo = 'String pwd = "senhateste123";'
Set-Content -Path "src/test/java/Test.java" -Value $conteudo -Encoding UTF8
git add src/test/java/Test.java
git commit -m "test: test file exempted"
# esperado: commit passa (src/test/ e excluido)
git reset HEAD~1
git restore src/test/java/Test.java
```

**Cenario 5 -- arquivo TypeScript com API key (deve bloquear):**
```powershell
$conteudo = 'const key = "sk-AbCdEfGhIjKlMnOpQrStUvWxYz012345678901";'
Set-Content -Path "frontend/src/lib/config.ts" -Value $conteudo -Encoding UTF8
git add frontend/src/lib/config.ts
git commit -m "test: ts secret"
# esperado: bloqueado
git restore --staged frontend/src/lib/config.ts
git restore frontend/src/lib/config.ts
```

Pre-condicao obrigatoria (ADR-011): verificar `Test-Path` e `git status` antes de
cada `git commit`. Registrar `$LASTEXITCODE` apos cada operacao critica.

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-10-secret-scanning

2. Ler antes de implementar:
   - .claude/hooks/universal/encoding-utf8.ps1  (padrao de iteracao)
   - .githooks/pre-commit.ps1                   (array $hooks)
   - docs/hooks-pendentes.md                    (formato de documentacao)

3. Criar .claude/hooks/universal/secret-scanning.ps1

4. Editar .githooks/pre-commit.ps1 (adicionar ao array)

5. Editar docs/hooks-pendentes.md (documentar hook)

6. Executar os 5 cenarios de validacao destrutiva

7. commit: feat(claude): adiciona hook secret-scanning pre-commit

8. Atualizar docs/progresso.md (registra 5.10)

9. commit: docs(progresso): registra sub-etapa 5.10
   (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-10.md)

10. /ship -> PR com reviews; corrigir apontamentos autonomamente
```

---

## Estrutura de commits (5.10)

```
feat(claude): adiciona hook secret-scanning pre-commit
docs(progresso): registra sub-etapa 5.10
```

---

## Restricoes

- NAO modificar hooks existentes (encoding, markdown, docs-size, maven-release, entity-migration).
- NAO criar nova categoria de hook -- este e universal (pasta `.claude/hooks/universal/`).
- Modo obrigatorio: `fail` (secret scanning nunca pode ser warn).
- Se hook bloquear commit durante validacao destrutiva: isso e o comportamento correto,
  nao e um erro -- documentar no PR como validacao bem-sucedida.
- Se hook nao bloquear quando deveria: isso e um bug -- corrigir o script antes de commitar.
- Commits de validacao criados durante cenarios destrutivos: usar `git reset HEAD~1`
  para desfaze-los antes do commit final (nao incluir commits de teste no PR).

---

## Estado esperado ao terminar

- PR aberto com 2 commits acima de main.
- 5 cenarios de validacao destrutiva documentados no body do PR.
- `git commit` com password literal bloqueado com mensagem clara.
- `git commit` com placeholder Spring (`${...}`) passando normalmente.
- `git commit` com arquivo em `src/test/` passando normalmente.
- docs/hooks-pendentes.md com entrada de Secret Scanning (5.10).
- docs/progresso.md com 5.10 registrada.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO rodar /ship mais de uma vez.
- NAO remover commits de teste usando `git reset --hard` -- usar `git reset HEAD~1`
  (soft) para preservar os arquivos e so entao `git restore`.
