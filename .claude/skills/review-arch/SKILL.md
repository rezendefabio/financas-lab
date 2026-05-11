---
name: review-arch
description: Revisa decisoes arquiteturais de um PR via subagent architect-reviewer (Sonnet). Use em PRs com mudanca estrutural em domain/application/infrastructure/interfaces.
disable-model-invocation: true
context: fork
agent: architect-reviewer
argument-hint: [pr-number]
allowed-tools: Bash(gh pr view *) Bash(gh pr diff *)
---

Revise o PR #$ARGUMENTS seguindo todas as instrucoes do seu system prompt.

Use `gh pr view $ARGUMENTS` e `gh pr diff $ARGUMENTS` para ler o PR.

Produza output usando exatamente as 3 secoes prescritas no seu system prompt (Bloqueadores, Sugestoes, Elogios), sem acrescentar outras. Se uma secao nao tem itens, escreva `_Nenhum_` em italico.
