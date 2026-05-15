---
name: babysit-prs
description: Loop que monitora PRs abertos a cada 5 minutos: auto-rebase em conflito com main, relatorio de CI falhando. Invocar uma vez -- o loop se auto-agenda via ScheduleWakeup.
disable-model-invocation: true
---

Voce e o babysitter de PRs do projeto financas-lab. Execute uma iteracao
completa e agende a proxima ao final.

**CONVENCAO DE EXECUCAO:** O Bash tool usa `/usr/bin/bash` (Git Bash), NAO
PowerShell. Para operacoes simples de arquivo: usar bash (`cat`, `rm -f`,
`if [ -f ... ]`). Para logica PowerShell complexa (ConvertFrom-Json, loops,
Where-Object, Get-Process): envolver em `powershell -NoProfile -Command "..."`
(curtos) ou escrever script `.ps1` com Write tool e executar com
`powershell -NoProfile -File <script>` (longos). Variaveis bash: `VAR=$(cmd)`,
nao `$var = cmd`. Condicional bash: `if [ -f arquivo ]; then ...; fi`.

No inicio de cada iteracao, capturar o diretorio raiz do repositorio usando o
Bash tool com o comando `pwd`, e guardar o resultado como variavel bash
`repo_root` para uso nos passos seguintes.

## Iteracao

### Passo 0 -- Carregar state file

Verificar se `.claude/babysit-prs.state` existe e ler o conteudo:

```bash
if [ -f ".claude/babysit-prs.state" ]; then cat ".claude/babysit-prs.state"; else echo '{"prs":{}}'; fi
```

- Se existir: parsear o JSON via `powershell -NoProfile -Command`:
  ```bash
  powershell -NoProfile -Command "Get-Content '.claude/babysit-prs.state' | ConvertFrom-Json"
  ```
  Se o parse falhar (JSON invalido): reinicializar com `@{ prs = @{} }`.
- Se nao existir: inicializar com `@{ prs = @{} }`.

Guardar o objeto como `$state` para uso nos passos seguintes.

### Passo 0.5 -- Limpar worktrees orphan

Escrever o script abaixo em `.claude/tmp-babysit-cleanup.ps1` com o Write
tool e executar via `-File`. Remover o script apos a execucao:

```bash
powershell -NoProfile -File .claude/tmp-babysit-cleanup.ps1
rm -f .claude/tmp-babysit-cleanup.ps1
```

Conteudo do script `.claude/tmp-babysit-cleanup.ps1`:

```powershell
# Listar todos os worktrees com informacao de lock
$worktreeRaw = git worktree list --porcelain

# Parsear blocos separados por linha vazia
$blocks = ($worktreeRaw -join "`n") -split "`n`n"

$orphansRemoved = @()

foreach ($block in $blocks) {
    $lines = $block -split "`n"
    $pathLine = $lines | Where-Object { $_ -match "^worktree " } | Select-Object -First 1
    $lockLine = $lines | Where-Object { $_ -match "^locked " }   | Select-Object -First 1

    if (-not $lockLine) { continue }

    $wtPath = $pathLine -replace "^worktree ", ""

    if ($lockLine -match "\(pid (\d+)\)") {
        $orphanPid = [int]$matches[1]
        $proc = Get-Process -Id $orphanPid -ErrorAction SilentlyContinue
        if ($null -eq $proc) {
            git worktree remove -f -f $wtPath 2>&1 | Out-Null
            $orphansRemoved += $wtPath
        }
    }
}

# Retornar lista de orphans removidos (uma por linha)
$orphansRemoved
```

Se a saida do script estiver vazia: silencioso, nenhuma linha extra no relatorio.
Se houver entradas: adicionar ao relatorio do Passo 4 a linha:
  Worktrees orphan removidos: <caminho1>, <caminho2> ...

### Passo 1 -- Listar PRs abertos

```bash
gh pr list --state open --json number,title,headRefName,mergeable,mergeStateStatus
```

Se retornar lista vazia: reportar "Nenhum PR aberto." e agendar proxima iteracao
(Passo 4).

### Passo 2 -- Para cada PR, verificar estado

Para cada PR na lista:

**2.0 -- Verificar se PR foi tratado recentemente (anti-reprocessamento):**

Obter SHA atual do HEAD do branch do PR via `powershell -NoProfile -Command`:

```bash
powershell -NoProfile -Command "(gh pr view <NUMBER> --json headRefOid | ConvertFrom-Json).headRefOid"
```

Se `$state.prs` contem entrada para este PR (chave `"$number"`):
- Calcular diferenca em minutos entre agora (UTC) e `last_checked` do state.
- Se `head_sha == $headSha` E diferenca < 30 minutos:
  - Obter mergeStateStatus atual do PR via `powershell -NoProfile -Command`:
    ```bash
    powershell -NoProfile -Command "(gh pr view <NUMBER> --json mergeStateStatus | ConvertFrom-Json).mergeStateStatus"
    ```
  - Se `$currentStatus == $state.prs["$number"].merge_state_status`:
    - Registrar no relatorio: "PR #N: IGNORADO (sem mudanca desde ultimo tratamento)"
    - Pular para o proximo PR (ir direto ao proximo item do loop).
  - Caso contrario (status mudou -- ex: CLEAN -> CONFLICTING): processar normalmente.
    - Salvar `$statusAnterior = $state.prs["$number"].merge_state_status` e `$statusAtual = $currentStatus`
      para uso no relatorio do Passo 4 (flag: `$reprocessadoPorStatus = $true`).
- Caso contrario (SHA mudou OU >= 30 minutos): processar normalmente.

Se nao contem entrada para este PR: processar normalmente.

**2a -- Verificar conflito com main:**

```bash
powershell -NoProfile -Command "gh pr view <NUMBER> --json mergeable,headRefName,mergeStateStatus | ConvertFrom-Json"
```

Se `mergeable == "CONFLICTING"`: executar auto-rebase (Passo 3a).
Se `mergeable == "MERGEABLE"` ou `"UNKNOWN"`: pular rebase.

**2a.1 -- Verificar se PR esta BEHIND:**

Se `mergeStateStatus == "BEHIND"` E `mergeable != "CONFLICTING"`:
executar update automatico (Passo 3c).

Logica:
- Se `mergeable == "CONFLICTING"`, o Passo 3a ja trata -- nao duplicar.
- Se `mergeStateStatus == "BEHIND"` e `mergeable` for `"UNKNOWN"` ou `"MERGEABLE"`,
  o update via API e seguro e deve ser feito.
- O campo `mergeStateStatus` pode valer: `"BEHIND"`, `"BLOCKED"`, `"CLEAN"`,
  `"DIRTY"`, `"DRAFT"`, `"HAS_HOOKS"`, `"UNKNOWN"`, `"UNSTABLE"`. Apenas
  `"BEHIND"` dispara a acao.

**2b -- Verificar CI:**

```bash
powershell -NoProfile -Command "
  \$checks = gh pr checks <NUMBER> --json name,state,conclusion | ConvertFrom-Json
  \$failing = \$checks | Where-Object { \$_.conclusion -eq 'FAILURE' -or \$_.state -eq 'FAILURE' }
  \$failing
"
```

Se `$failing` nao for vazio: executar auto-fix (Passo 3b).
Se todos passando ou pendentes: sem acao.

### Passo 3a -- Auto-rebase (apenas se CONFLICTING)

Para o PR com conflito, usar comandos bash:

```bash
# Obter nome do branch via powershell (ConvertFrom-Json)
branch=$(powershell -NoProfile -Command "(gh pr view <NUMBER> --json headRefName | ConvertFrom-Json).headRefName")
worktree_path="$repo_root/.claude/worktrees/babysit-pr-<NUMBER>"

# Criar worktree para o branch do PR
git fetch origin
git worktree add "$worktree_path" "$branch"

# Rebase sobre main atualizado
cd "$worktree_path" && git rebase origin/main
```

Se o rebase falhar (`$?` false):

  **Spawnar sub-agente para resolver os conflitos:**

  Usar o Agent tool com subagent_type `general-purpose`. Ler o arquivo
  `.claude/agents/conflict-resolver.md` e passar seu conteudo (body apos o
  frontmatter YAML) como prompt, substituindo as variaveis de ambiente:
  - $WORKTREE_PATH pelo valor real de `$worktree_path`
  - $PR_NUMBER pelo numero do PR

  Com base no retorno do sub-agente:
  - Se `RESOLVIDO`: `git push origin "$branch" --force-with-lease`, remover worktree,
    registrar "PR #N: rebase com resolucao inteligente OK"
  - Se `ABORTADO`: remover worktree, registrar a mensagem como motivo
  - Em ambos os casos: `cd "$repo_root"`

Se o rebase suceder:
```bash
git push origin "$branch" --force-with-lease
git worktree remove "$worktree_path"
cd "$repo_root"
```
- Registrar no relatorio: "PR #N: rebase executado com sucesso"

Apos acao (sucesso ou falha), atualizar state via `powershell -NoProfile -Command`:

```bash
powershell -NoProfile -Command "
  \$headSha = (gh pr view <NUMBER> --json headRefOid | ConvertFrom-Json).headRefOid
  \$freshStatus = (gh pr view <NUMBER> --json mergeStateStatus | ConvertFrom-Json).mergeStateStatus
  \$state.prs['<NUMBER>'] = @{
    last_action        = 'rebase'
    last_checked       = (Get-Date).ToUniversalTime().ToString('o')
    head_sha           = \$headSha
    merge_state_status = \$freshStatus
  }
"
```

### Passo 3b -- Auto-fix de CI

Para cada PR com CI falhando, obter branch, worktree path, runId e logs via
`powershell -NoProfile -Command`:

```bash
powershell -NoProfile -Command "
  \$branch = (gh pr view <NUMBER> --json headRefName | ConvertFrom-Json).headRefName
  \$runId = (gh run list --branch \$branch --json databaseId,conclusion |
    ConvertFrom-Json |
    Where-Object { \$_.conclusion -eq 'failure' } |
    Select-Object -First 1).databaseId
  Write-Output \$branch
  Write-Output \$runId
"
```

Depois capturar os logs do run com falha:

```bash
gh run view <RUN_ID> --log-failed
```

Usar o Agent tool com subagent_type `general-purpose`. Ler o arquivo
`.claude/agents/ci-fixer.md` e passar seu conteudo (body apos o frontmatter
YAML) como prompt, precedido pelo log de falha:

"## Log de falha do CI\n\n{LOGS_FAILED}\n\n" + conteudo do ci-fixer.md,
substituindo as variaveis:
- $BRANCH pelo valor real de $branch
- $PR_NUMBER pelo numero do PR
- $WORKTREE_PATH pelo valor real de $worktreePath
- $REPO_ROOT pelo valor real de $repoRoot

Com base no retorno do sub-agente:
- Se `CORRIGIDO`: registrar "PR #N: CI auto-fix OK -- <descricao>"
- Se `NAO CORRIGIDO`: registrar "PR #N: CI falhou -- auto-fix nao aplicavel: <motivo>"
- Em ambos os casos: `cd "$repo_root"`

Apos acao (corrigido ou nao), atualizar state via `powershell -NoProfile -Command`:

```bash
powershell -NoProfile -Command "
  \$headSha = (gh pr view <NUMBER> --json headRefOid | ConvertFrom-Json).headRefOid
  \$freshStatus = (gh pr view <NUMBER> --json mergeStateStatus | ConvertFrom-Json).mergeStateStatus
  \$state.prs['<NUMBER>'] = @{
    last_action        = 'ci-fix'
    last_checked       = (Get-Date).ToUniversalTime().ToString('o')
    head_sha           = \$headSha
    merge_state_status = \$freshStatus
  }
"
```

### Passo 3c -- Auto-update (apenas se BEHIND e nao CONFLICTING)

Para o PR com `mergeStateStatus == "BEHIND"` e `mergeable != "CONFLICTING"`:

```bash
gh pr update-branch <NUMBER>
```

Se retornar sucesso (`$?` true):
- Registrar no relatorio: "PR #N: atualizado via update-branch (estava BEHIND)"

Se retornar erro:
- Registrar no relatorio: "PR #N: update-branch falhou -- <mensagem de erro>"
- Nao spawnar sub-agente, nao criar worktree. O erro e passivo.

Apos acao (sucesso ou falha), atualizar state via `powershell -NoProfile -Command`:

```bash
powershell -NoProfile -Command "
  \$headSha = (gh pr view <NUMBER> --json headRefOid | ConvertFrom-Json).headRefOid
  \$freshStatus = (gh pr view <NUMBER> --json mergeStateStatus | ConvertFrom-Json).mergeStateStatus
  \$state.prs['<NUMBER>'] = @{
    last_action        = 'update-branch'
    last_checked       = (Get-Date).ToUniversalTime().ToString('o')
    head_sha           = \$headSha
    merge_state_status = \$freshStatus
  }
"
```

### Passo 3d -- Atualizar state para PRs sem acao

Se nenhuma acao foi executada para um PR (nao CONFLICTING, nao BEHIND, CI ok),
atualizar state com `last_action = "ok"` via `powershell -NoProfile -Command`:

```bash
powershell -NoProfile -Command "
  \$headSha = (gh pr view <NUMBER> --json headRefOid | ConvertFrom-Json).headRefOid
  \$freshStatus = (gh pr view <NUMBER> --json mergeStateStatus | ConvertFrom-Json).mergeStateStatus
  \$state.prs['<NUMBER>'] = @{
    last_action        = 'ok'
    last_checked       = (Get-Date).ToUniversalTime().ToString('o')
    head_sha           = \$headSha
    merge_state_status = \$freshStatus
  }
"
```

Depois de processar todos os PRs, salvar o state atualizado em disco usando o
Write tool para gravar o JSON em `.claude/babysit-prs.state`.
O JSON deve ser gerado via `$state | ConvertTo-Json -Depth 5`.

### Passo 4 -- Relatorio da iteracao

Exibir resumo:

```
[babysit-prs HH:MM] N PRs verificados

<para cada PR:>
  PR #N <titulo>: <REBASE OK | REBASE RESOLVIDO (inteligente) | REBASE ABORTADO: <motivo> | UPDATE-BRANCH OK | UPDATE-BRANCH FALHOU: <motivo> | CI AUTO-FIX OK | CI FALHOU (manual): <motivo> | IGNORADO (sem mudanca desde ultimo tratamento) | OK>
  <se $reprocessadoPorStatus == $true para este PR:>
  PR #N: reprocessado (mergeStateStatus mudou: $statusAnterior -> $statusAtual)

Proxima verificacao em 5 minutos.
```

### Passo 5 -- Agendar proxima iteracao

Confirmar que o relatorio do Passo 4 foi gerado com sucesso antes de agendar.
Se qualquer erro irrecuperavel ocorreu durante a iteracao, reportar ao operador
e encerrar sem agendar.

Usar ScheduleWakeup:
- delaySeconds: 270
- reason: "proxima iteracao do babysit-prs"
- prompt: `<<autonomous-loop-dynamic>>`
