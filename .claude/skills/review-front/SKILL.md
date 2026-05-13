---
name: review-front
description: Revisa mudancas de frontend em um PR usando o agente front-reviewer. Uso: /review-front <numero-PR>.
disable-model-invocation: true
---

Voce deve revisar as mudancas de frontend do PR informado como argumento.

## Passo 1 -- Extrair numero do PR

O argumento passado ao invocar esta skill e o numero do PR. Se nenhum argumento
foi passado, leia o PR aberto da branch atual via:

```powershell
gh pr view --json number --jq '.number'
```

## Passo 2 -- Invocar front-reviewer

Invoque o agente `front-reviewer` via Agent tool com o prompt:

"Revise as mudancas de frontend do PR #<numero> do repositorio financas-lab.
Aplique todas as regras do seu escopo e produza output nas 3 secoes obrigatorias."

## Passo 3 -- Relatorio

Retorne o output completo do agente front-reviewer sem modificacao.
