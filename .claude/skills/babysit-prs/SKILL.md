---
name: babysit-prs
description: Loop que monitora PRs abertos a cada 10 minutos: auto-rebase em conflito com main, relatorio de CI falhando. Invocar uma vez -- o loop se auto-agenda via ScheduleWakeup.
disable-model-invocation: true
---

Voce e o babysitter de PRs do projeto financas-lab. Execute uma iteracao
completa e agende a proxima ao final.

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
gh pr checks <number>
```

Se houver checks com status `fail`: registrar no relatorio (Passo 3b).
Se todos `pass` ou `pending`: sem acao.

### Passo 3a -- Auto-rebase (apenas se CONFLICTING)

Para o PR com conflito:

```powershell
$branch = $pr.headRefName
$worktreePath = ".claude/worktrees/babysit-pr-$number"

# Criar worktree para o branch do PR
git fetch origin
git worktree add $worktreePath $branch

# Rebase sobre main atualizado
Set-Location $worktreePath
git rebase origin/main
```

Se o rebase falhar (conflito nao-trivial):
- Abortar: `git rebase --abort`
- Registrar no relatorio: "PR #N: rebase falhou -- conflito requer resolucao manual"
- Remover worktree: `git worktree remove $worktreePath --force`
- Continuar para o proximo PR

Se o rebase suceder:
```powershell
git push origin $branch --force-with-lease
git worktree remove $worktreePath
Set-Location <raiz do repositorio>
```
- Registrar no relatorio: "PR #N: rebase executado com sucesso"

### Passo 3b -- Registrar CI falhando

Para cada PR com check falhando, registrar:
"PR #N (<titulo>): CI falhou -- <nome do check>"

### Passo 4 -- Relatorio da iteracao

Exibir resumo:

```
[babysit-prs HH:MM] N PRs verificados

<para cada PR:>
  PR #N <titulo>: <REBASE OK | REBASE FALHOU (manual) | CI FALHOU: <check> | OK>

Proxima verificacao em 10 minutos.
```

### Passo 5 -- Agendar proxima iteracao

Usar ScheduleWakeup:
- delaySeconds: 600
- reason: "proxima iteracao do babysit-prs"
- prompt: `<<autonomous-loop-dynamic>>`
