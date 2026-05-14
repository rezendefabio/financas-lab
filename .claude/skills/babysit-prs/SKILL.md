---
name: babysit-prs
description: Loop que monitora PRs abertos a cada 10 minutos: auto-rebase em conflito com main, relatorio de CI falhando. Invocar uma vez -- o loop se auto-agenda via ScheduleWakeup.
disable-model-invocation: true
---

Voce e o babysitter de PRs do projeto financas-lab. Execute uma iteracao
completa e agende a proxima ao final.

No inicio de cada iteracao, capturar o diretorio raiz do repositorio usando o
Bash tool com o comando `pwd`, e guardar o resultado como $repoRoot para uso
nos passos seguintes.

## Iteracao

### Passo 1 -- Listar PRs abertos

```powershell
gh pr list --state open --json number,title,headRefName,mergeable
```

Se retornar lista vazia: reportar "Nenhum PR aberto." e agendar proxima iteracao
(Passo 4).

### Passo 2 -- Para cada PR, verificar estado

Para cada PR na lista:

**2a -- Verificar conflito com main:**

```powershell
$pr = gh pr view <number> --json mergeable,headRefName | ConvertFrom-Json
```

Se `mergeable == "CONFLICTING"`: executar auto-rebase (Passo 3a).
Se `mergeable == "MERGEABLE"` ou `"UNKNOWN"`: pular rebase.

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

### Passo 4 -- Relatorio da iteracao

Exibir resumo:

```
[babysit-prs HH:MM] N PRs verificados

<para cada PR:>
  PR #N <titulo>: <REBASE OK | REBASE RESOLVIDO (inteligente) | REBASE ABORTADO: <motivo> | CI AUTO-FIX OK | CI FALHOU (manual): <motivo> | OK>

Proxima verificacao em 10 minutos.
```

### Passo 5 -- Agendar proxima iteracao

Confirmar que o relatorio do Passo 4 foi gerado com sucesso antes de agendar.
Se qualquer erro irrecuperavel ocorreu durante a iteracao, reportar ao operador
e encerrar sem agendar.

Usar ScheduleWakeup:
- delaySeconds: 600
- reason: "proxima iteracao do babysit-prs"
- prompt: `<<autonomous-loop-dynamic>>`
