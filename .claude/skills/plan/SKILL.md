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

Ler o arquivo `.claude/skills/plan/prompts/task-planner.md` com a ferramenta
Read e usar seu conteudo como prompt, substituindo `{OBJETIVO}` pelo argumento
recebido.

Usar o Agent tool com:
- subagent_type: "general-purpose"
- model: "opus"
- prompt: conteudo de task-planner.md com {OBJETIVO} substituido

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

ANTES de chamar qualquer ferramenta, escrever como texto de resposta ao operador
(nao como resultado de tool call) o planejamento completo no formato abaixo.
Este texto DEVE aparecer no chat antes do AskUserQuestion:

```
/plan: {N} tasks planejadas para "{OBJETIVO}"

Premissas assumidas:
  [P1] {premissa_1}
  [P2] {premissa_2}
  ...

  [1] {task-001 titulo}
      Complexidade: {M} | Risco: {baixo} | Migration: {V22 ou "nenhuma"}
      {task-001 resumo}

  [2] {task-002 titulo}
      Complexidade: {M} | Risco: {baixo} | Migration: {nenhuma}
      {task-002 resumo}

  ...
```

As premissas vem de `premissas_globais` no JSON retornado pelo planejador.
Se o operador escolher "Quero discutir / ajustar", ele pode rejeitar uma
premissa -- o planejador deve ser re-invocado com a correcao e o plano
reapresentado.

Apos escrever o texto acima, chamar AskUserQuestion com:
- Pergunta: "O plano esta correto?"
- Opcao 1: "Sim, spawnar os executores agora"
- Opcao 2: "Quero discutir ou ajustar o plano"
- Opcao 3: "Cancelar"

**Se "Sim, spawnar":** continuar para o Passo 4.

**Se "Quero discutir / ajustar":** entrar em loop de discussao:
  1. Perguntar ao operador o que deseja ajustar
  2. Incorporar o feedback (adicionar, remover ou modificar tasks na lista em memoria)
  3. Atualizar `.claude/tasks.json` com as tasks revisadas
  4. Reapresentar o plano atualizado no mesmo formato acima
  5. Repetir o AskUserQuestion ate o operador escolher "Sim" ou "Cancelar"

**Se "Cancelar":** exibir "Execucao cancelada. Tasks registradas em
.claude/tasks.json com status pending." e encerrar sem spawnar nenhum executor.

## Passo 4 -- Spawnar executores

Spawnar todos os executores em paralelo (uma unica acao atomica -- todos os
Agent tool calls emitidos na mesma resposta):

Para cada task, usar o Agent tool com:
- subagent_type: "general-purpose"
- isolation: "worktree"
- run_in_background: false
- prompt: ler `.claude/skills/plan/prompts/task-executor.md` com a ferramenta
  Read, substituir `{CONTEUDO}` pelo campo `prompt` da task e `{LABEL}` pelo
  campo `id` da task.

ACAO OBRIGATORIA: emitir AGORA todos os {N} Agent tool calls em uma unica resposta.
Nao escrever texto entre os tool calls. Nao aguardar resultado de um antes de emitir os demais.

## Passo 5 -- Atualizar state e consolidar

Apos todos os executores completarem, executar os quatro sub-passos abaixo em ordem.
Nao exibir o relatorio final antes de concluir 5.1 e 5.2 para TODAS as tasks.
O relatorio do Sub-passo 5.3 e preliminar; a versao consolidada (com a secao
`Reviews:`) so e exibida no Sub-passo 5.4, apos os reviews completarem.

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

### Sub-passo 5.3 -- Exibir relatorio final preliminar

So exibir apos 5.1 e 5.2 concluidos para todas as tasks. Este relatorio e
preliminar -- a secao `Reviews:` ainda nao existe nesta etapa; ela e adicionada
e o relatorio re-exibido no Sub-passo 5.4.

```
/plan concluido: "{OBJETIVO}"

[para cada task:]
  [{id}] {titulo}: <OK (PR: <url>) | BLOQUEADO: <motivo>>

PRs abertos: N/{total}
Bloqueadores: <lista ou "nenhum">
```

### Sub-passo 5.4 -- Reviews automaticos de PR (obrigatorio)

Executar APOS o relatorio preliminar do Sub-passo 5.3 ter sido exibido.

Para cada task com `status: "completed"` e `pr_url` nao-nulo, executar os
reviews em sequencia (nao em paralelo -- cada review deve completar antes do
proximo iniciar):

Para cada task com `status: "completed"` mas `pr_url` nulo: nao roda review --
registrar na secao `Reviews:` como `INCOMPLETO (PR nao foi aberto)`.

**Review 1 -- pr-reviewer (sempre):**
- subagent_type: "pr-reviewer"
- prompt: "Revise o PR #<numero> do repositorio financas-lab antes do merge."
  (extrair <numero> do ultimo segmento da pr_url)

Aguardar o resultado. Se reportar bloqueador: registrar no relatorio final.

**Review 2 -- front-reviewer (condicional):**

Verificar se a task tem arquivos frontend. Usar como sinal o campo `tipo` da
task (se disponivel no tasks.json) ou o titulo/resumo da task:
- Se `tipo` for "frontend_only" ou "feature_completa": spawnar front-reviewer
- Se `tipo` for "backend_only": pular
- Se `tipo` ausente ou "refactor": spawnar front-reviewer por precaucao

- subagent_type: "front-reviewer"
- prompt: "Revise as mudancas de frontend do PR #<numero> do repositorio financas-lab."

Aguardar o resultado. Se reportar bloqueador: registrar no relatorio final.

Apos todos os reviews completarem, re-exibir o relatorio final consolidado --
o mesmo formato do Sub-passo 5.3, agora acrescido da secao `Reviews:`:

```
Reviews:
  [task-001] pr-reviewer:    OK | BLOQUEADOR: <motivo>
  [task-001] front-reviewer: OK | BLOQUEADOR: <motivo> | N/A (backend-only)
```

Se multiplas tasks tiverem PR: repetir o ciclo de reviews para cada uma
antes de re-exibir o relatorio final consolidado.

## Passo 6 -- Cleanup de worktrees e branches orfaos

Apos exibir o relatorio final, executar os dois sub-passos de limpeza em ordem.

### Sub-passo 6.1 -- Remover worktrees e branches com prefixo agent

```bash
# Remover worktrees registrados cujo path contem 'agent-'
git worktree list --porcelain | grep "^worktree " | awk '{print $2}' | grep "agent-" | while read wt; do
  git worktree remove -f -f "$wt" 2>/dev/null || true
done

# Remover branches locais com prefixo worktree-agent-
git branch | grep "worktree-agent-" | while read b; do
  git branch -D "$(echo $b | tr -d ' *')" 2>/dev/null || true
done
```

### Sub-passo 6.2 -- Remover branches de feature cujo remote foi deletado (merged)

```bash
# Sincronizar refs remotas (remove tracking de branches deletadas no GitHub)
git fetch --prune 2>/dev/null || true

# Deletar branches locais cujo upstream foi removido (gone)
git branch -vv | grep ': gone]' | awk '{print $1}' | while read b; do
  git branch -d "$b" 2>/dev/null || true
done
```

O `-d` (minusculo) so deleta branches totalmente mergeadas -- nao ha risco de
perda de trabalho nao publicado. Se o branch ainda nao foi mergeado, o comando
falha silenciosamente (o `|| true` garante que nao aborta o cleanup).

### Sub-passo 6.3 -- Remover branches locais-only sem tracking (review-*, worktree-*)

Branches criados por sub-agentes reviewers (prefixo `review-`) e por worktrees
(prefixo `worktree-`) nunca sao publicados no remote -- nao tem upstream configurado
e nao aparecem no `grep ': gone]'` do Sub-passo 6.2.

```bash
# Deletar branches locais-only com prefixo review- ou worktree-
# -D e seguro aqui: esses branches nunca foram publicados
git branch | grep -E '^\s*(review-|worktree-)' | tr -d ' *' | while read b; do
  git branch -D "$b" 2>/dev/null || true
done
```

Se nenhum branch desse tipo existir: pular silenciosamente.

Se nenhum worktree ou branch orfao encontrado: pular silenciosamente.
