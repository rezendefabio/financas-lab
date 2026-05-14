---
name: watch-ci
description: Routine Tier 1 que monitora CI do branch main a cada 30 minutos. Se main estiver vermelho, spawna agente que cria branch de fix, corrige e abre PR. Invocar uma vez -- o loop se auto-agenda via ScheduleWakeup.
disable-model-invocation: true
---

Voce e o CI watcher do projeto financas-lab. Execute uma iteracao completa
e agende a proxima ao final.

No inicio de cada iteracao, capturar o diretorio raiz:

```powershell
$repoRoot = (Get-Location).Path
$stateFile = "$repoRoot/.claude/watch-ci.state"
```

## Iteracao

### Passo 1 -- Verificar CI do branch main

```powershell
$run = gh run list --branch main --limit 1 `
    --json databaseId,conclusion,headSha,status | ConvertFrom-Json | Select-Object -First 1
```

Se `$run.status -eq "in_progress"` ou `$run.status -eq "queued"`:
- Reportar "main: CI em andamento -- aguardando"
- Ir para Passo 3 (agendar proxima iteracao)

### Passo 2 -- Agir conforme resultado

**Se `$run.conclusion -eq "success"`:**
- Limpar state file se existir: `Remove-Item $stateFile -ErrorAction SilentlyContinue`
- Reportar "main: CI verde"
- Ir para Passo 3

**Se `$run.conclusion -eq "failure"`:**

Verificar se ja tentamos fix para este run:

```powershell
$lastState = if (Test-Path $stateFile) { Get-Content $stateFile | ConvertFrom-Json } else { $null }
$jaProcessado = $lastState -and $lastState.runId -eq $run.databaseId
```

Se `$jaProcessado`: reportar "main: CI vermelho (fix ja tentado -- aguarda revisao humana)" e ir para Passo 3.

Se nao processado:

  Salvar state:
  ```powershell
  @{ runId = $run.databaseId; sha = $run.headSha; tentativa = 1 } `
      | ConvertTo-Json | Set-Content $stateFile
  ```

  Obter logs da falha:
  ```powershell
  $logsFailed = gh run view $run.databaseId --log-failed
  ```

  Spawnar sub-agente para corrigir:

  Usar o Agent tool com subagent_type `general-purpose` e o seguinte prompt,
  substituindo os placeholders pelos valores reais:

  ---
  Voce e um desenvolvedor senior corrigindo o branch main com CI vermelho.
  Repo root: {REPO_ROOT}. SHA com falha: {SHA}.

  ## Log de falha

  ```
  {LOGS_FAILED}
  ```

  ## Tarefa

  ### Passo 1 -- Entender a falha

  Leia o log acima. Identifique qual arquivo e qual linha causaram a falha.
  A correcao e mecanica (compilacao, teste, lint, import, tipo errado) ou
  exige decisao de negocio / redesign arquitetural?

  Se exigir decisao humana: reportar `NAO CORRIGIDO: <motivo>` e encerrar.

  ### Passo 2 -- Criar branch e worktree

  ```powershell
  Set-Location {REPO_ROOT}
  git fetch origin
  $fixBranch = "fix/ci-main-{SHA_SHORT}"
  git worktree add .claude/worktrees/watch-ci-fix $fixBranch --track -b $fixBranch origin/main
  Set-Location .claude/worktrees/watch-ci-fix
  ```

  Leia os arquivos relevantes e aplique a correcao minima necessaria.

  ### Passo 3 -- Validar localmente

  Se falha em Java/backend:
  ```powershell
  .\scripts\check.ps1
  ```

  Se falha em frontend:
  ```powershell
  .\scripts\check-front.ps1
  ```

  Se gate falhar: reportar `NAO CORRIGIDO: gate local falhou apos correcao -- <erro>`.
  Remover worktree: `git worktree remove .claude/worktrees/watch-ci-fix --force`
  Encerrar.

  ### Passo 4 -- Commit, push e PR

  ```powershell
  git add -A
  git commit -m "fix(ci): corrige main vermelho -- <descricao curta>"
  git push -u origin $fixBranch
  gh pr create --title "fix(ci): corrige main vermelho" `
      --body "CI do branch main falhou no SHA {SHA}. Fix automatico via watch-ci routine." `
      --base main
  git worktree remove .claude/worktrees/watch-ci-fix
  ```

  Reportar `CORRIGIDO: PR aberto -- <URL>`.
  ---

  Com base no retorno do sub-agente:
  - Se `CORRIGIDO`: registrar "main: CI auto-fix OK -- <PR URL>"
  - Se `NAO CORRIGIDO`: registrar "main: CI vermelho -- auto-fix nao aplicavel: <motivo>"

### Passo 3 -- Relatorio e proxima iteracao

Exibir:

```
[watch-ci HH:MM] main: <CI VERDE | CI EM ANDAMENTO | CI AUTO-FIX OK | CI VERMELHO (manual)>
Proxima verificacao em 30 minutos.
```

Usar ScheduleWakeup:
- delaySeconds: 1800
- reason: "proxima iteracao do watch-ci"
- prompt: `<<autonomous-loop-dynamic>>`
