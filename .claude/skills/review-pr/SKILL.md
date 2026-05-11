---
name: review-pr
description: Revisa o PR informado via subagent pr-reviewer (Haiku). Use antes do merge.
disable-model-invocation: true
context: fork
agent: pr-reviewer
argument-hint: [pr-number]
allowed-tools: Bash(gh pr view *) Bash(gh pr diff *)
---

Revise o PR #$ARGUMENTS seguindo todas as instrucoes do seu system prompt.

Use `gh pr view $ARGUMENTS` e `gh pr diff $ARGUMENTS` para ler o PR.

Produza output usando exatamente as 3 secoes prescritas no seu system prompt (Bloqueadores, Sugestoes, Elogios), sem acrescentar outras. Se uma secao nao tem itens, escreva `_Nenhum_` em italico.
