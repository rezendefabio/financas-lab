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

Spawnar sub-agente para analisar e corrigir:

Usar o Agent tool com subagent_type `general-purpose` e o seguinte prompt,
substituindo os placeholders pelos valores reais (`{BRANCH}` por `$branch`,
`{PR_NUMBER}` por `$number`, `{WORKTREE_PATH}` por `$worktreePath`,
`{REPO_ROOT}` por `$repoRoot`, `{LOGS_FAILED}` por `$logsFailed`):

---
Voce e um desenvolvedor senior debugando um CI vermelho.
Branch: {BRANCH}. PR: #{PR_NUMBER}. Worktree a criar: {WORKTREE_PATH}.

## Log de falha do CI

```
{LOGS_FAILED}
```

## Tarefa

Corrija o problema que causou a falha acima. Voce tem no maximo 2 tentativas.

### Passo 1 -- Entender a falha

Leia o log acima e identifique:
- Qual etapa falhou (compilacao, testes, lint, build)?
- Qual arquivo e qual linha causaram a falha?
- A correcao e mecanica (import faltando, assertion errada, erro de sintaxe,
  campo renomeado, tipo errado) ou exige decisao de negocio / redesign?

Se exigir decisao de negocio ou redesign arquitetural: reportar
`NAO CORRIGIDO: <motivo> -- requer intervencao humana` e encerrar sem abrir worktree.

### Passo 2 -- Abrir worktree e corrigir

```powershell
Set-Location {REPO_ROOT}
git fetch origin
git worktree add {WORKTREE_PATH} {BRANCH}
Set-Location {WORKTREE_PATH}
```

Leia os arquivos relevantes indicados pelo log. Aplique a correcao minima
necessaria para resolver a falha sem alterar logica nao relacionada.

### Passo 3 -- Validar localmente

Se a falha foi em testes ou build Java:
```powershell
.\scripts\check.ps1
```

Se a falha foi em testes ou build frontend:
```powershell
.\scripts\check-front.ps1
```

Se o gate local passar (`$LASTEXITCODE -eq 0`): ir para Passo 4.

Se falhar: analisar o novo erro.
- Se for um erro diferente que voce consegue corrigir: corrigir e rodar o gate novamente (esta e a segunda tentativa).
- Se falhar novamente ou o erro for irrecuperavel: `git worktree remove {WORKTREE_PATH} --force`, reportar
  `NAO CORRIGIDO: <motivo da falha persistente>` e encerrar.

### Passo 4 -- Commit e push

```powershell
git add -A
git commit -m "fix(<scope>): corrige CI -- <descricao curta do problema>"
git push origin {BRANCH}
```

Se o push falhar por branch divergida (`rejected ... non-fast-forward`):
- Fazer rebase: `git rebase origin/{BRANCH}`
- Se o rebase tiver conflitos: resolver usando o mesmo raciocinio do Passo 2
  (entender a intencao de cada lado, produzir sintese correta, sem marcadores
  de conflito no resultado final). Continuar com `git rebase --continue --no-edit`.
- Se o rebase suceder: `git push origin {BRANCH} --force-with-lease`
- Se o rebase falhar com contradicao genuina: `git rebase --abort`,
  `git worktree remove {WORKTREE_PATH} --force`,
  reportar `NAO CORRIGIDO: conflito pos-fix requer intervencao manual`.

```powershell
git worktree remove {WORKTREE_PATH}
```

Reportar `CORRIGIDO: <descricao do que foi corrigido>`.
---

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
