# Prompt -- Sub-etapa 5.21: Skill /babysit-prs (Camada B da fabrica)

## Contexto

Camada B da fabrica: um loop que monitora PRs abertos e age autonomamente.
Duas acoes no escopo desta sub-etapa:

1. **Auto-rebase** -- PR com conflito com main e detectado, rebase executado
   automaticamente via worktree isolado, branch atualizada com force-with-lease.
2. **Monitoramento de CI** -- PRs com checks falhando sao reportados ao operador
   com o nome do check falho (sem auto-fix nesta versao).

O loop roda a cada 10 minutos via ScheduleWakeup enquanto a sessao esta ativa.
O operador invoca `/babysit-prs` uma vez e o loop se auto-agenda.

Referencia direta: Boris Cherny -- "I have one that's babysitting my PRs,
like fixing CI, auto rebasing."

Camada 3 (infraestrutura de fabrica). Sem mudancas no produto.

---

## O que criar

Um unico arquivo: `.claude/skills/babysit-prs/SKILL.md`

Leia `.claude/skills/batch/SKILL.md` e `.claude/skills/ship/SKILL.md`
como referencia de estilo antes de escrever.

---

## Conteudo do SKILL.md

```markdown
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
```

---

## Fluxo de execucao do executor

```
1. git checkout -b feat/etapa-5-21-babysit-prs

2. Ler .claude/skills/batch/SKILL.md e .claude/skills/ship/SKILL.md como referencia

3. Criar .claude/skills/babysit-prs/SKILL.md com o conteudo acima

4. Validacao destrutiva (ADR-011):
   - Test-Path .claude/skills/babysit-prs/SKILL.md -- deve retornar True
   - Verificar que frontmatter tem name, description e disable-model-invocation: true

5. git status -- confirmar que so babysit-prs/SKILL.md esta staged

6. commit: feat(claude): adiciona skill /babysit-prs (loop babysitter de PRs)

7. Atualizar docs/progresso.md (registra sub-etapa 5.21)

8. commit: docs(progresso): registra sub-etapa 5.21
   (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-21.md)

9. /ship -> PR; corrigir apontamentos autonomamente
```

---

## Estrutura de commits (5.21)

```
feat(claude): adiciona skill /babysit-prs (loop babysitter de PRs)
docs(progresso): registra sub-etapa 5.21
```

---

## Restricoes

- NAO implementar auto-fix de CI nesta versao -- apenas reportar.
- NAO tentar rebase se mergeable == "UNKNOWN" -- estado transiente, aguardar
  proxima iteracao.
- Se rebase falhar por conflito nao-trivial: abortar e reportar, nunca forcar.
- O loop usa `<<autonomous-loop-dynamic>>` no ScheduleWakeup -- nao inventar
  outro prompt, este sentinel e o correto para /loop dinamico.
- `disable-model-invocation: true` e obrigatorio no frontmatter.
- Worktrees de babysit sempre em `.claude/worktrees/babysit-pr-<number>` para
  nao conflitar com worktrees do /batch.

---

## Como usar apos merge

```
# Em qualquer sessao Claude Code (sem necessidade de --dangerously-skip-permissions):
/babysit-prs
```

O loop roda imediatamente e se auto-agenda a cada 10 minutos.
Para parar: encerrar a sessao ou deixar sem PRs abertos.

---

## Estado esperado ao terminar

- PR aberto com 2 commits acima de main.
- `.claude/skills/babysit-prs/SKILL.md` criado com frontmatter correto.
- Loop funcional: detecta CONFLICTING, executa rebase, reporta CI falhando.
- `.\scripts\check.ps1` verde.
