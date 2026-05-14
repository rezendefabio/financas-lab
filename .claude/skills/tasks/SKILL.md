---
name: tasks
description: Exibe o estado atual das tasks em .claude/tasks.json. Util para checar progresso entre sessoes.
disable-model-invocation: true
---

Ler `.claude/tasks.json`. Se o arquivo nao existir: reportar "Nenhuma task registrada."

Exibir:

```
Tasks registradas ({total}):

[para cada task, agrupado por planId:]
Plan {planId}:
  [{id}] {titulo}
    Status:  {status}
    Branch:  {branch ou "--"}
    PR:      {pr_url ou "--"}
    Updated: {updated_at}
```
