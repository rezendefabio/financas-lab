---
name: babysit-prs
description: Loop que monitora PRs abertos a cada 10 minutos: auto-rebase em conflito com main, relatorio de CI falhando. Invocar uma vez -- o loop se auto-agenda via ScheduleWakeup.
disable-model-invocation: true
---

Voce e o babysitter de PRs do projeto financas-lab. Execute uma iteracao
completa e agende a proxima ao final.

No inicio de cada iteracao, capturar o diretorio raiz do repositorio:

```powershell
$repoRoot = (Get-Location).Path
```

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

  Usar o Agent tool com subagent_type `general-purpose` e o seguinte prompt,
  substituindo {WORKTREE_PATH} e {PR_NUMBER} pelos valores reais:

  ---
  Voce e um desenvolvedor senior fazendo merge manual de um rebase com conflito.
  Worktree: {WORKTREE_PATH}. PR: #{PR_NUMBER}.

  Sua meta: para cada arquivo em conflito, produzir um resultado correto que
  satisfaca a intencao de ambos os lados. So aborte diante de contradicao
  genuina que nao tem resolucao sem decisao humana.

  ## Passo 1 -- Entender o contexto

  ```powershell
  Set-Location {WORKTREE_PATH}
  git log --oneline -5
  git diff --name-only --diff-filter=U
  ```

  Para cada arquivo em conflito, leia o arquivo completo (com marcadores
  `<<<<<<<`, `=======`, `>>>>>>>`). Se precisar de contexto adicional para
  entender a intencao de alguma mudanca (arquivos relacionados, mensagens de
  commit, dependencias), leia o que for necessario.

  ## Passo 2 -- Resolver cada conflito

  Para cada arquivo em conflito:

  1. Entenda o que "ours" (origin/main) pretendia fazer naquele trecho.
  2. Entenda o que "theirs" (branch do PR) pretendia fazer naquele trecho.
  3. Produza o resultado que satisfaz ambas as intencoes, de forma correta e
     idiomatica para o tipo de arquivo e linguagem.

  O resultado nao precisa ser "um lado + o outro lado colados". Pode ser uma
  sintese -- o que importa e que a intencao de cada lado seja preservada no
  resultado final, sem duplicatas desnecessarias, sem codigo invalido.

  Se em algum trecho as intencoes sao contraditorias e nao ha sintese possivel
  sem uma decisao que voce nao consegue tomar com seguranca: marque esse arquivo
  como NAO RESOLVIDO e registre o motivo preciso.

  ## Passo 3 -- Aplicar e continuar

  Para cada arquivo resolvido:
  - Escrever o arquivo com o conteudo final (sem nenhum marcador de conflito)
  - `git add <arquivo>`

  Se todos os arquivos foram resolvidos:
  - `git rebase --continue --no-edit`
  - Verificar `$LASTEXITCODE` imediatamente apos o comando
  - Se `$LASTEXITCODE -eq 0`: reportar `RESOLVIDO: <descricao de como cada arquivo foi tratado>`
  - Se `$LASTEXITCODE -ne 0` (novo conflito no proximo commit do rebase): repetir Passos 1, 2 e 3

  Se algum arquivo NAO foi resolvido:
  - `git rebase --abort`
  - Reportar `ABORTADO: <arquivo> -- <motivo da contradicao>`
  ---

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

### Passo 3b -- Registrar CI falhando

Para cada PR com check falhando, registrar:
"PR #N (<titulo>): CI falhou -- <nome do check>"

### Passo 4 -- Relatorio da iteracao

Exibir resumo:

```
[babysit-prs HH:MM] N PRs verificados

<para cada PR:>
  PR #N <titulo>: <REBASE OK | REBASE RESOLVIDO (inteligente) | REBASE ABORTADO: <motivo> | CI FALHOU: <check> | OK>

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
