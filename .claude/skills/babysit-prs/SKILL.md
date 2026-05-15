---
name: babysit-prs
description: Loop que monitora PRs abertos a cada 5 minutos: auto-rebase em conflito com main, relatorio de CI falhando. Invocar uma vez -- o loop se auto-agenda via ScheduleWakeup.
disable-model-invocation: true
---

Voce e o babysitter de PRs do projeto financas-lab. Execute uma iteracao
completa e agende a proxima ao final.

No inicio de cada iteracao, capturar o diretorio raiz do repositorio usando o
Bash tool com o comando `pwd`, e guardar o resultado como $repoRoot para uso
nos passos seguintes.

## Iteracao

### Passo 0 -- Carregar state file

Verificar se `.claude/babysit-prs.state` existe:

```powershell
$stateFile = "$repoRoot/.claude/babysit-prs.state"
```

- Se existir: ler e parsear o JSON usando `Get-Content $stateFile | ConvertFrom-Json`.
  Se o parse falhar (JSON invalido): reinicializar com `@{ prs = @{} }`.
- Se nao existir: inicializar com `@{ prs = @{} }`.

Guardar o objeto como `$state` para uso nos passos seguintes.

### Passo 0.5 -- Limpar worktrees orphan

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
```

Se $orphansRemoved estiver vazio: silencioso, nenhuma linha extra no relatorio.
Se houver entradas: adicionar ao relatorio do Passo 4 a linha:
  Worktrees orphan removidos: <caminho1>, <caminho2> ...
Nao usar Write-Host inline no foreach.

### Passo 1 -- Listar PRs abertos

```powershell
gh pr list --state open --json number,title,headRefName,mergeable,mergeStateStatus
```

Se retornar lista vazia: reportar "Nenhum PR aberto." e agendar proxima iteracao
(Passo 4).

### Passo 2 -- Para cada PR, verificar estado

Para cada PR na lista:

**2.0 -- Verificar se PR foi tratado recentemente (anti-reprocessamento):**

Obter SHA atual do HEAD do branch do PR:

```powershell
$headSha = (gh pr view $number --json headRefOid | ConvertFrom-Json).headRefOid
```

Se `$state.prs` contem entrada para este PR (chave `"$number"`):
- Calcular diferenca em minutos entre agora (UTC) e `last_checked` do state.
- Se `head_sha == $headSha` E diferenca < 30 minutos:
  - Obter mergeStateStatus atual do PR:
    ```powershell
    $currentStatus = (gh pr view $number --json mergeStateStatus | ConvertFrom-Json).mergeStateStatus
    ```
  - Se `$currentStatus == $state.prs["$number"].merge_state_status`:
    - Registrar no relatorio: "PR #N: IGNORADO (sem mudanca desde ultimo tratamento)"
    - Pular para o proximo PR (ir direto ao proximo item do loop).
  - Caso contrario (status mudou -- ex: CLEAN -> CONFLICTING): processar normalmente.
    - Guardar em variavel que o processamento foi por mudanca de status (para o relatorio do Passo 4).
- Caso contrario (SHA mudou OU >= 30 minutos): processar normalmente.

Se nao contem entrada para este PR: processar normalmente.

**2a -- Verificar conflito com main:**

```powershell
$pr = gh pr view <number> --json mergeable,headRefName,mergeStateStatus | ConvertFrom-Json
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

```powershell
$checks = gh pr checks $number --json name,state,conclusion | ConvertFrom-Json
$failing = $checks | Where-Object { $_.conclusion -eq "FAILURE" -or $_.state -eq "FAILURE" }
```

Se `$failing` nao for vazio: executar auto-fix (Passo 3b).
Se todos passando ou pendentes: sem acao.

### Passo 3a -- Auto-rebase (apenas se CONFLICTING)

Para o PR com conflito:

```powershell
$branch = $pr.headRefName
$worktreePath = "$repoRoot/.claude/worktrees/babysit-pr-$number"

# Criar worktree para o branch do PR
git fetch origin
git worktree add $worktreePath $branch

# Rebase sobre main atualizado
Set-Location $worktreePath
git rebase origin/main
```

Se o rebase falhar (`$LASTEXITCODE -ne 0`):

  **Spawnar sub-agente para resolver os conflitos:**

  Usar o Agent tool com subagent_type `general-purpose`. Ler o arquivo
  `.claude/agents/conflict-resolver.md` e passar seu conteudo (body apos o
  frontmatter YAML) como prompt, substituindo as variaveis de ambiente:
  - $WORKTREE_PATH pelo valor real de $worktreePath
  - $PR_NUMBER pelo numero do PR

  Com base no retorno do sub-agente:
  - Se `RESOLVIDO`: `git push origin $branch --force-with-lease`, remover worktree,
    registrar "PR #N: rebase com resolucao inteligente OK"
  - Se `ABORTADO`: remover worktree, registrar a mensagem como motivo
  - Em ambos os casos: `Set-Location $repoRoot`

Se o rebase suceder:
```powershell
git push origin $branch --force-with-lease
git worktree remove $worktreePath
Set-Location $repoRoot
```
- Registrar no relatorio: "PR #N: rebase executado com sucesso"

Apos acao (sucesso ou falha), atualizar state:
```powershell
$headSha = (gh pr view $number --json headRefOid | ConvertFrom-Json).headRefOid
$state.prs["$number"] = @{
    last_action        = "rebase"
    last_checked       = (Get-Date).ToUniversalTime().ToString("o")
    head_sha           = $headSha
    merge_state_status = $pr.mergeStateStatus
}
```

### Passo 3b -- Auto-fix de CI

Para cada PR com CI falhando:

```powershell
$branch = (gh pr view $number --json headRefName | ConvertFrom-Json).headRefName
$worktreePath = "$repoRoot/.claude/worktrees/babysit-ci-$number"

# Obter logs do run com falha
$runId = (gh run list --branch $branch --json databaseId,conclusion `
    | ConvertFrom-Json `
    | Where-Object { $_.conclusion -eq "failure" } `
    | Select-Object -First 1).databaseId

$logsFailed = gh run view $runId --log-failed
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
- Em ambos os casos: `Set-Location $repoRoot`

Apos acao (corrigido ou nao), atualizar state:
```powershell
$headSha = (gh pr view $number --json headRefOid | ConvertFrom-Json).headRefOid
$state.prs["$number"] = @{
    last_action        = "ci-fix"
    last_checked       = (Get-Date).ToUniversalTime().ToString("o")
    head_sha           = $headSha
    merge_state_status = $pr.mergeStateStatus
}
```

### Passo 3c -- Auto-update (apenas se BEHIND e nao CONFLICTING)

Para o PR com `mergeStateStatus == "BEHIND"` e `mergeable != "CONFLICTING"`:

```powershell
gh pr update-branch <number>
```

Se retornar sucesso (`$LASTEXITCODE -eq 0`):
- Registrar no relatorio: "PR #N: atualizado via update-branch (estava BEHIND)"

Se retornar erro:
- Registrar no relatorio: "PR #N: update-branch falhou -- <mensagem de erro>"
- Nao spawnar sub-agente, nao criar worktree. O erro e passivo.

Apos acao (sucesso ou falha), atualizar state:
```powershell
$headSha = (gh pr view $number --json headRefOid | ConvertFrom-Json).headRefOid
$state.prs["$number"] = @{
    last_action        = "update-branch"
    last_checked       = (Get-Date).ToUniversalTime().ToString("o")
    head_sha           = $headSha
    merge_state_status = $pr.mergeStateStatus
}
```

### Passo 3d -- Atualizar state para PRs sem acao

Se nenhuma acao foi executada para um PR (nao CONFLICTING, nao BEHIND, CI ok),
atualizar state com `last_action = "ok"`:

```powershell
$state.prs["$number"] = @{
    last_action        = "ok"
    last_checked       = (Get-Date).ToUniversalTime().ToString("o")
    head_sha           = $headSha
    merge_state_status = $pr.mergeStateStatus
}
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
  <se processado por mudanca de mergeStateStatus (nao por mudanca de SHA):>
  PR #N: reprocessado (mergeStateStatus mudou: <status_anterior> -> <status_atual>)

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
