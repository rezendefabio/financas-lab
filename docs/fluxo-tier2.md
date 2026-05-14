# Fluxo Tier 2 -- Guia de intervencao do operador

Este documento descreve quando intervir vs deixar a fabrica resolver.

## Os tres checkpoints do operador

1. **Requisito**: operador passa objetivo para `/plan`
2. **Planejamento**: operador revisa tasks e aprova (ou cancela)
3. **PRs**: operador revisa e faz merge (ou rejeita)

Fora desses tres momentos, a fabrica e autonoma.

---

## Situacoes comuns e como agir

### PR com CI vermelho

**Aguardar primeiro.** O babysitter verifica a cada 10 minutos e tenta
auto-fix automatico (1 tentativa com gate local). So intervir se:
- O babysitter reportou `NAO CORRIGIDO: requer intervencao humana`
- O CI continua vermelho apos 2 iteracoes do babysitter (20 min)

**Como intervir**: ler o log de falha via `gh run view <runId> --log-failed`,
corrigir manualmente, commitar com `fix(<scope>): ...` e fazer push da branch.

### PR com conflito com main

**Aguardar primeiro.** O babysitter detecta `CONFLICTING` e spawna o
sub-agente `conflict-resolver`. So intervir se:
- O babysitter reportou `REBASE ABORTADO: <motivo de contradicao genuina>`
- O conflito envolve decisao de negocio que o agente nao pode tomar

**Como intervir**: fazer checkout da branch, resolver manualmente, push.

### PR atras de main (BEHIND)

**Nao intervir.** O babysitter detecta `mergeStateStatus == BEHIND` e
executa `gh pr update-branch` automaticamente na proxima iteracao (10 min).

Se precisar resolver agora: `gh pr update-branch <numero>`.

### PR com apontamento bloqueador do review

**Depende do tipo:**

- **Apontamento objetivo** (anotacao faltando, import ausente, convencao ADR):
  o executor corrige autonomamente no Passo 5.1 do /ship. Se nao corrigiu,
  editar o arquivo, rodar `check.ps1`, commitar com `fix(<scope>): ...`, push.

- **Apontamento subjetivo ou arquitetural** (redesign de interface, mudanca
  de estrategia, impacto em outros bounded contexts): avaliar se concorda.
  Se sim: comentar no PR com a decisao e fazer merge. Se nao: rejeitar o PR
  (ver secao "Como rejeitar um PR de agente").

### Quando rejeitar um PR de agente

Rejeitar (fechar sem merge) quando:
- A abordagem escolhida pelo agente viola um ADR estabelecido de forma nao-reparavel
- O escopo do PR extrapolou o objetivo original
- A implementacao tem risco arquitetural que exige redesign (nao e correcao pontual)

**Como rejeitar**:
```powershell
gh pr close <numero> --comment "Rejeitado: <motivo>. Proximo executor deve <instrucao corretiva>."
```

Nao deletar a branch imediatamente -- manter por 24h para referencia.

### Como cancelar um /plan em andamento

Se o `/plan` ja exibiu o planejamento e aguarda aprovacao: responder "Nao,
cancelar" na pergunta do AskUserQuestion. As tasks ficam registradas em
`.claude/tasks.json` com status `pending` mas nenhum executor e spawned.

Se os executores ja foram spawnados e estao rodando: nao ha interrupcao
automatica. Aguardar completarem e rejeitar os PRs abertos que nao deve
ser mergeados.

---

## Sinais de que a fabrica precisa de intervencao

- Babysitter reporta o mesmo PR como `NAO CORRIGIDO` por 3 iteracoes seguidas
- CI do main vermelho por mais de 1 hora sem PR de fix aberto pelo watch-ci
- `/plan` nao spawnou executores apos aprovacao (verificar `.claude/tasks.json`)
- tasks.json com tasks em status `pending` por mais de 30 minutos sem atividade

---

## Comandos uteis de diagnostico

```powershell
# Estado das tasks do ultimo /plan
/tasks

# PRs abertos
gh pr list --state open --json number,title,mergeable,mergeStateStatus

# CI do main
gh run list --branch main --limit 5 --json conclusion,status,displayTitle

# Ultimo log de CI com falha
gh run list --branch main --limit 1 --json databaseId,conclusion | ConvertFrom-Json
gh run view <runId> --log-failed

# Worktrees ativos (se houver execucao em andamento)
git worktree list
```
