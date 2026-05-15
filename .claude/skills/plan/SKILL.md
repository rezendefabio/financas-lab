---
name: plan
description: Recebe objetivo de alto nivel, spawna sub-agente planejador que quebra em tasks, registra em .claude/tasks.json e spawna executores em paralelo. O humano so revisa os PRs.
disable-model-invocation: true
---

> IMPORTANTE: Nunca execute o objetivo diretamente. Sempre seguir o fluxo
> completo Passo 0 -> Passo 1 -> Passo 2 -> Passo 3 -> Passo 4 -> Passo 5,
> independentemente da complexidade ou tamanho do objetivo recebido. Se o
> operador invocou /plan, e porque quer o fluxo completo: planejador,
> aprovacao e executores em paralelo.

## Input

Argumento recebido: objetivo de alto nivel (ex: "implemente telas de Meta e
Lancamento Recorrente").

## Passo 0 -- Preparar state

```bash
TASKS_FILE=".claude/tasks.json"
[ -f "$TASKS_FILE" ] || printf '{ "tasks": [] }' > "$TASKS_FILE"
PLAN_ID="plan-$(date +%Y%m%d-%H%M%S)"
echo "$PLAN_ID"
```

Guardar o valor impresso como `planId` para uso nos passos seguintes.

## Passo 1 -- Spawnar sub-agente planejador

Usar o Agent tool com subagent_type `general-purpose` e o seguinte prompt,
substituindo {OBJETIVO} pelo argumento recebido:

---
Voce e o planejador da fabrica financas-lab. Seu objetivo: receber um objetivo
de alto nivel e produzir uma lista de tasks executaveis, cada uma com o prompt
completo que o executor vai receber.

## Objetivo recebido

{OBJETIVO}

## Passo 1 -- Entender o contexto do projeto

Leia os seguintes arquivos para entender o estado atual:
- `CLAUDE.md` (convencoes, stack, estrutura)
- `docs/progresso.md` (o que ja foi feito, sub-etapas concluidas)

## Passo 1.5 -- Auditar o estado atual do projeto (OBRIGATORIO antes de propor tasks)

Antes de propor qualquer task, executar as seguintes verificacoes:

**1. Bounded contexts existentes:**
Listar diretorios em `src/main/java/` que correspondem a bounded contexts:
usar Glob com pattern `src/main/java/**/*Application.java` para localizar o
pacote base, depois Glob `src/main/java/**/domain/*.java` para listar entidades.

**2. Migrations existentes:**
Listar `src/main/resources/db/migration/V*.sql` para saber o numero mais alto.
O proximo numero disponivel e max(V) + 1.

**3. Features ja implementadas:**
Ler `docs/progresso.md` secao "Camada 4" para identificar o que ja foi entregue.

**4. Regras de auditoria:**
- Se o objetivo menciona criar X e X ja existe como bounded context: a task deve
  ser REFACTOR de X, nao CRIACAO. Mencionar explicitamente no titulo da task.
- Se o numero de migration proposto ja existe: usar o proximo disponivel.
- Nunca propor duplicar logica que ja existe (auth, JWT, repositorios base).

Registrar o resultado da auditoria (bounded contexts encontrados, ultimo V, features
concluidas relevantes) antes de prosseguir para o Passo 2.

## Passo 2 -- Quebrar em tasks (fatia vertical obrigatoria)

Analise o objetivo e decomponha em tasks independentes e paralelizaveis.

### Principio central: fatia vertical

Cada task deve entregar uma feature COMPLETA, do banco ate a tela:
- Migration SQL (se a feature altera o schema)
- Domain: entidade, repositorio, excecoes de dominio
- Application: use cases com testes Mockito
- Infrastructure: repositorio JPA com testes Testcontainers
- Interface: controller REST com testes MockMvc
- **Frontend: pagina(s) Next.js com testes Vitest** (quando a feature tem UI)

**Regra absoluta:** se o objetivo menciona "tela", "pagina", "frontend", "dashboard",
"formulario" ou qualquer coisa visivel ao usuario, a task DEVE incluir o frontend.
Nao e permitido criar uma task que so entrega backend quando a feature tem UI.

**Quando nao ha frontend:** features puramente internas (jobs, migrations de dados,
refactors de infra, hooks) podem ser tasks sem frontend. Neste caso, o titulo da
task deve deixar claro que e backend-only ("Refactor interno de X", "Migration de dados Y").

### Criterios de task

Cada task deve:
- Ser executavel de forma isolada num worktree git proprio
- Ter escopo de UMA feature completa (nao metade de uma feature)
- Nao depender de outra task desta lista para compilar e testar
- Ter dependencias de dados claramente identificadas (ex: "depende de task-001
  para FK de usuario -- spawnar apos task-001 mergeada")

### Paralelismo correto

- Tasks SEM dependencia de dados: spawnar todas em paralelo
- Tasks COM dependencia: identificar a dependencia no prompt do executor e
  orientar o executor a verificar se a migration da task-dependente ja foi
  mergeada antes de criar FKs

Para cada task, escreva o prompt completo que o executor vai receber.
O prompt deve conter: contexto, auditoria do que ja existe, o que fazer,
arquivos a ler, fluxo de execucao (incluindo frontend quando aplicavel),
estrutura de commits, restricoes. Seguir o padrao dos prompts em docs/prompts/.

## Passo 3 -- Retornar lista de tasks

Retornar um JSON com a lista de tasks no formato:

```json
{
  "tasks": [
    {
      "id": "task-001",
      "titulo": "descricao curta",
      "resumo": "1-3 linhas descrevendo o que esta task vai fazer, quais arquivos vai tocar e qual resultado vai produzir",
      "prompt": "conteudo completo do prompt do executor"
    }
  ]
}
```

O campo `resumo` deve ser uma descricao executiva: o que a task entrega, quais arquivos
principais serao editados, qual e o output esperado (ex: PR aberto, arquivo modificado).
Maximo 3 linhas por task.

Retornar APENAS o JSON, sem texto antes ou depois.
---

## Passo 2 -- Registrar tasks no state

Para cada task retornada pelo planejador, adicionar em `.claude/tasks.json`:

```json
{
  "id": "{task.id}",
  "planId": "{planId}",
  "titulo": "{task.titulo}",
  "resumo": "{task.resumo}",
  "status": "pending",
  "branch": null,
  "pr_url": null,
  "created_at": "{timestamp ISO}",
  "updated_at": "{timestamp ISO}"
}
```

## Passo 3 -- Exibir planejamento e aguardar aprovacao

Exibir ao operador o planejamento completo:

```
/plan: {N} tasks planejadas para "{OBJETIVO}":

  [1] {task-001 titulo}
      {task-001 resumo}

  [2] {task-002 titulo}
      {task-002 resumo}

  ...
```

Usar AskUserQuestion com a pergunta "Deseja spawnar os {N} executores agora?"
e duas opcoes: "Sim, spawnar" e "Nao, cancelar".

Se o operador escolher "Sim, spawnar": continuar para o Passo 4 (spawnar executores).
Se o operador escolher "Nao, cancelar" ou qualquer outra resposta: exibir
"Execucao cancelada. Tasks registradas em .claude/tasks.json com status pending."
e encerrar sem spawnar nenhum executor.

## Passo 4 -- Spawnar executores

Spawnar todos os executores em paralelo (uma unica acao atomica -- todos os
Agent tool calls emitidos na mesma resposta):

Para cada task, usar o Agent tool com:
- subagent_type: "general-purpose"
- isolation: "worktree"
- run_in_background: false
- prompt: template abaixo com {CONTEUDO} e {LABEL} substituidos

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

## Restricao absoluta de ambiente

- Voce esta num worktree git ISOLADO. A branch `main` e BLOQUEADA.
- NUNCA criar, modificar ou deletar arquivos fora do diretorio do seu worktree.
- NUNCA fazer `git checkout main` ou `git switch main`.
- NUNCA instalar dependencias (npm install, mvn install) no diretorio raiz do
  repositorio principal -- apenas no seu worktree.
- Se precisar instalar dependencias: verificar que esta no worktree antes de rodar.
- Verificacao obrigatoria antes de qualquer npm install ou npm ci:
  ```bash
  worktree_dir=$(git rev-parse --show-toplevel)
  echo "Diretorio atual: $(pwd)"
  echo "Raiz do worktree: $worktree_dir"
  # Confirmar que nao e o repositorio principal verificando se o path contem 'agent-'
  if echo "$worktree_dir" | grep -v 'agent-' > /dev/null; then
    echo "AVISO: este worktree pode ser o repositorio principal. Verificar antes de instalar."
  fi
  ```
- O npm install DEVE ser executado com `cd <dir-do-worktree> && npm install`, nunca com `npm install` na raiz.
- Nunca executar `npm install` ou `npm ci` sem confirmar primeiro que o diretorio atual e o worktree isolado.

Sua unica responsabilidade: executar TODOS os passos descritos abaixo de forma
completamente autonoma, sem pedir aprovacao ao operador.

## Instrucoes da tarefa

{CONTEUDO}

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
2. Execute cada passo do fluxo de execucao descrito em "Instrucoes da tarefa"
3. Nao pule passos. Nao invente passos.
4. Se um passo falhar: registre o erro e tente corrigir. Aborte se irrecuperavel.

## Relatorio final

Task:     {LABEL}
Branch:   <branch criada>
Commits:  <lista de commits>
PR:       <URL do PR ou "nao aberto">
Status:   OK | BLOQUEADOR: <motivo>
```

ACAO OBRIGATORIA: emitir AGORA todos os {N} Agent tool calls em uma unica resposta.
Nao escrever texto entre os tool calls. Nao aguardar resultado de um antes de emitir os demais.

## Passo 5 -- Atualizar state e consolidar

Apos todos os executores completarem, executar os tres sub-passos abaixo em ordem.
Nao exibir o relatorio final antes de concluir 5.1 e 5.2 para TODAS as tasks.

### Sub-passo 5.1 -- Parsear cada relatorio

O resultado de cada Agent tool call e uma string de texto. Para cada resultado,
iterar linha a linha e extrair:

- `branch`: primeira linha que comeca com `Branch:` (apos trim)
  - Extrair tudo apos o prefixo, aplicar trim
  - Se vazio ou ausente: null
  - Exemplo: `"Branch:   feat/etapa-X-Y-foo"` -> `"feat/etapa-X-Y-foo"`

- `pr_url`: primeira linha que comeca com `PR:` (apos trim)
  - Se valor comecar com `https://`: usar como pr_url
  - Se valor for `"nao aberto"` (case-insensitive): null
  - Nos demais casos: null

- `status`: primeira linha que comeca com `Status:` (apos trim)
  - Se contem `OK`: `"completed"`
  - Se contem `BLOQUEADOR`: `"blocked"`
  - Se ausente: `"blocked"`

### Sub-passo 5.2 -- Gravar tasks.json apos cada executor

Para cada task (pelo campo `id`):

1. Ler `.claude/tasks.json` com a ferramenta Read
2. Fazer parse do JSON em memoria
3. Localizar objeto cujo `id` corresponde a task
4. Sobrescrever: `status`, `branch`, `pr_url`, `updated_at` (timestamp ISO)
5. Serializar e gravar com a ferramenta Write
   (NAO usar Bash com echo -- causa problemas de encoding)

Executar para TODAS as tasks antes de prosseguir para 5.3.

### Sub-passo 5.3 -- Exibir relatorio final

So exibir apos 5.1 e 5.2 concluidos para todas as tasks:

```
/plan concluido: "{OBJETIVO}"

[para cada task:]
  [{id}] {titulo}: <OK (PR: <url>) | BLOQUEADO: <motivo>>

PRs abertos: N/{total}
Bloqueadores: <lista ou "nenhum">
```

## Passo 6 -- Cleanup de worktrees e branches orfaos

Apos exibir o relatorio final, remover worktrees e branches orfaos nessa ordem
(worktree remove antes de branch -D para evitar erro de branch em uso por worktree registrado):

```powershell
# 1. Remover worktrees registrados cujo path contem 'agent-'
$wtOutput = git worktree list --porcelain
foreach ($line in $wtOutput) {
    if ($line -match '^worktree (.+agent-.+)$') {
        git worktree remove -f -f $matches[1] 2>$null
    }
}
# 2. Remover branches orfaos
$orphanBranches = git branch | Where-Object { $_ -match 'worktree-agent-' }
foreach ($b in @($orphanBranches)) {
    git branch -D $b.Trim() 2>$null
}
```

Se nenhum worktree ou branch orfao encontrado: pular silenciosamente.
