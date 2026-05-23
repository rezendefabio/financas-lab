---
name: batch
description: Executa multiplas tasks em paralelo via Agent tool com worktrees isolados. Cada task e um arquivo em docs/prompts/. Uso: /batch etapa-5-18 etapa-5-19
disable-model-invocation: true
---

Execute as tasks em paralelo seguindo os passos abaixo.
Pare e reporte ao operador se qualquer verificacao inicial falhar.

## Passo 0 -- Resolver, verificar e ler conteudo

Para cada argumento recebido:
- Se comeca com `docs/prompts/`: usar o path literal
- Caso contrario: expandir para `docs/prompts/prompt-{arg}.md`

Para cada path resolvido:
1. Usar o tool Read para ler o arquivo na integra.
   Se o Read retornar erro (arquivo nao encontrado): reportar
   "ERRO: arquivo nao encontrado: {path}" e terminar sem spawnar nada.
2. Guardar o conteudo lido para usar no Passo 2.

## Passo 1 -- Confirmar execucao

Exibir ao operador a lista de tasks que serao executadas em paralelo:

```
/batch: executando N tasks em paralelo:
  [1] docs/prompts/prompt-etapa-5-18.md
  [2] docs/prompts/prompt-etapa-5-19.md
  ...
```

## Passo 2 -- Spawnar todos os agentes (acao atomica)

Esta etapa e UMA UNICA acao atomica: emitir N Agent tool calls simultaneamente,
onde N e o numero de tasks da lista.

Nao ha loop. Nao ha "primeiro um, depois o outro". Todos os Agent calls sao emitidos
na mesma resposta, ao mesmo tempo, antes de qualquer resultado ser recebido.

Para cada task na lista, os parametros do Agent call sao:
- subagent_type: "general-purpose"
- isolation: "worktree"
- run_in_background: false
- prompt: template abaixo com {CONTEUDO_DO_ARQUIVO} e {LABEL} substituidos

### Template do prompt do sub-agente

```
Voce e um executor autonomo no projeto financas-lab.

## Verificacao obrigatoria de ambiente (executar ANTES de qualquer outra acao)

```bash
branch=$(git branch --show-current)
echo "Branch atual: $branch"
if [ "$branch" = "main" ]; then
  echo "ERRO CRITICO: executor esta na branch main. Abortar imediatamente."
  exit 1
fi
```

Se o comando acima retornar "main": parar tudo e reportar
`BLOQUEADOR: executor iniciou em main -- nao e permitido modificar main diretamente`.

## Verificacao de diretorio de trabalho

Antes de criar qualquer arquivo, confirmar que o diretorio de trabalho
e o worktree isolado, nao o repositorio principal:

```bash
worktree_root=$(git rev-parse --show-toplevel)
pwd_atual=$(pwd)
echo "Worktree root: $worktree_root"
echo "PWD atual: $pwd_atual"
if [ "$worktree_root" = "/c/projetos/financas-lab" ] || [ "$worktree_root" = "C:/projetos/financas-lab" ]; then
  echo "ERRO CRITICO: diretorio e o repo principal. Verificar worktree."
fi
```

## Limpeza obrigatoria antes de encerrar

Antes de reportar conclusao, verificar se ha arquivos nao-trackeados fora
do contexto da tarefa no worktree:

```bash
git status --short | grep "^??" | grep -v ".claude/tasks.json"
```

Se houver arquivos `??` inesperados: remover com `rm -f <arquivo>` antes
de encerrar. Nunca deixar arquivos residuais no worktree.

## Convencao de ambiente: bash vs PowerShell

O Bash tool usa `/usr/bin/bash` (Git Bash), NAO PowerShell.
- Para operacoes de arquivo: usar `rm -f`, `cat`, `ls`, `mkdir` (nao `Remove-Item`,
  `Get-Content`, `Get-ChildItem`, `New-Item`)
- Para logica PowerShell complexa: envolver em `powershell -NoProfile -Command "..."`
- Variaveis bash: `VAR=$(comando)`, nao `$var = comando`
- Condicional bash: `if [ -f arquivo ]; then ...; fi`, nao `if (Test-Path ...)`

## Restricao de ambiente e dependencias

- Voce esta num worktree git ISOLADO. A branch `main` e BLOQUEADA.
- NUNCA criar, modificar ou deletar arquivos fora do diretorio do seu worktree.
- NUNCA fazer `git checkout main` ou `git switch main`.
- NUNCA rodar npm install, npm ci ou mvn install no repositorio principal.
- NUNCA rodar npm install no worktree -- usar symlink para node_modules (ver abaixo).

## Setup de dependencias frontend em worktree

Se a task inclui arquivos em `frontend/`, criar symlink antes de rodar qualquer
comando frontend (build, test, lint):

```bash
MAIN_REPO="/c/projetos/financas-lab"
WORKTREE=$(git rev-parse --show-toplevel)
if [ ! -d "$WORKTREE/frontend/node_modules" ] && [ ! -L "$WORKTREE/frontend/node_modules" ]; then
  ln -s "$MAIN_REPO/frontend/node_modules" "$WORKTREE/frontend/node_modules"
  echo "Symlink criado: frontend/node_modules -> $MAIN_REPO/frontend/node_modules"
fi
```

NAO rodar npm install. Se esta task adicionar novas dependencias ao package.json:
reportar "AVISO: operador deve rodar npm install em frontend/ apos o merge."

Sua unica responsabilidade: executar TODOS os passos descritos abaixo de forma
completamente autonoma, sem pedir aprovacao ao operador.

## Instrucoes da tarefa

{CONTEUDO_DO_ARQUIVO}

## Contexto do ambiente

- Voce esta num worktree git isolado do repositorio financas-lab.
- As convencoes do projeto estao em `CLAUDE.md` -- leia antes de comecar.
- Docker esta rodando (daemon ativo). check.ps1 e check-front.ps1 funcionam.
- Git credentials e gh CLI estao configurados -- push e gh pr create funcionam.

## Sobre skills invocados na tarefa

O Skill tool NAO funciona para skills com disable-model-invocation:true.
Para qualquer skill mencionado nas instrucoes (/ship, /write-test, /feature, etc.):
  1. Leia o arquivo `.claude/skills/<nome>/SKILL.md`
  2. Execute a logica descrita nele manualmente, passo a passo

## Execucao

1. Leia `CLAUDE.md`
2. Execute cada passo do fluxo de execucao descrito em "Instrucoes da tarefa" acima
3. Nao pule passos. Nao invente passos que nao estao nas instrucoes.
4. Se um passo falhar: registre o erro e tente corrigir antes de abortar.
   Apenas aborte se o erro for irrecuperavel (ex: check.ps1 falha apos 2 tentativas).

## Relatorio final

Ao terminar (sucesso ou falha), reporte:

Task:     {LABEL}
Branch:   <branch criada>
Commits:  <lista de commits>
PR:       <URL do PR ou "nao aberto">
Reviews:  <resumo de cada review>
Status:   OK | BLOQUEADOR: <motivo>
```

---

ACAO OBRIGATORIA: Emita AGORA todos os {N} Agent tool calls acima em uma unica resposta.
N = numero de tasks confirmadas no Passo 1.
Nao escreva nenhum texto adicional antes ou entre os tool calls.
Nao aguarde resultado de nenhum deles antes de emitir os demais.

## Passo 3 -- Aguardar e consolidar

Aguardar todos os sub-agentes completarem. Consolidar num relatorio final:

```
/batch concluido.

Tasks:    N executadas em paralelo
Duracao:  ~X minutos

[para cada task:]
---
Task:    <nome do arquivo>
Branch:  <branch>
PR:      <URL>
Reviews: <status resumido>
Status:  OK | BLOQUEADOR: <motivo>

[sumario final:]
PRs abertos: N/N
Bloqueadores: <lista ou "nenhum">
```

Se qualquer task retornar BLOQUEADOR: sinalize ao operador mas nao cancele
as outras tasks -- cada worktree e isolado e independente.

### Passo 3.1 -- Reviews automaticos de PR

O /batch roda no repo principal (nao em worktree), portanto o Agent tool esta
disponivel para spawnar sub-agentes de review.

Para cada task com `Status: OK` e PR nao-nulo, executar os reviews em SEQUENCIA
(completar um antes de iniciar o proximo):

Extrair o numero do PR de cada URL (ultimo segmento).

**Review 1 -- pr-reviewer (sempre):**
- subagent_type: "pr-reviewer"
- prompt: "Revise o PR #<numero> do repositorio financas-lab antes do merge."

Aguardar resultado. Se reportar bloqueador: registrar no relatorio final.

**Review 2 -- architect-reviewer (sempre):**
- subagent_type: "architect-reviewer"
- prompt: "Revise as decisoes arquiteturais do PR #<numero> do repositorio financas-lab contra os ADRs do projeto."

Aguardar resultado.

**Review 3 -- front-reviewer (condicional):**

Verificar se a task tem arquivos frontend:

```bash
gh pr diff <numero> --name-only 2>/dev/null | grep "^frontend/" | head -1
```

Se retornar linha nao-vazia:
- subagent_type: "front-reviewer"
- prompt: "Revise as mudancas de frontend do PR #<numero> do repositorio financas-lab."

Aguardar resultado. Se reportar bloqueador: registrar no relatorio final.

Adicionar secao `Reviews:` ao relatorio consolidado (um bloco por task com PR):

```
Reviews:
  [task-001] pr-reviewer:        OK | BLOQUEADOR: <motivo>
  [task-001] architect-reviewer: OK | BLOQUEADOR: <motivo>
  [task-001] front-reviewer:     OK | BLOQUEADOR: <motivo> | N/A (sem frontend)
```

Se nenhuma task tiver PR aberto: pular este passo silenciosamente.

### Cleanup de worktrees e branches orfaos

> ATENCAO: nunca executar comandos PowerShell diretamente no Bash tool sem
> envolver em `powershell -Command "..."` ou `-File`. Redirects em contexto bash
> SEMPRE usam `2>/dev/null`, nunca `2>$null`.

Apos consolidar o relatorio, remover worktrees e branches orfaos nessa ordem
(worktree remove antes de branch -D para evitar erro de branch em uso por worktree registrado).

Escrever o script abaixo em `.claude/tmp-batch-cleanup.ps1` com o Write tool e
executar via `-File`. Remover o script apos a execucao:

```bash
powershell -NoProfile -File .claude/tmp-batch-cleanup.ps1
rm -f .claude/tmp-batch-cleanup.ps1
```

Conteudo do script `.claude/tmp-batch-cleanup.ps1`:

```powershell
# 1. Remover worktrees registrados cujo path contem 'agent-'
$wtOutput = git worktree list --porcelain
foreach ($line in $wtOutput) {
    if ($line -match '^worktree (.+agent-.+)$') {
        git worktree remove -f -f $matches[1] 2>$null
    }
}
git worktree prune
# 2. Remover branches orfaos
$orphanBranches = git branch | Where-Object { $_ -match 'worktree-agent-' }
foreach ($b in @($orphanBranches)) {
    git branch -D $b.Trim() 2>$null
}
```

Se nenhum worktree ou branch orfao encontrado: pular silenciosamente.
