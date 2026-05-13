---
name: batch
description: Executa multiplas tasks em paralelo via Agent tool com worktrees isolados. Cada task e um arquivo em docs/prompts/. Uso: /batch etapa-5-18 etapa-5-19
disable-model-invocation: true
---

Execute as tasks em paralelo seguindo os passos abaixo.
Pare e reporte ao operador se qualquer verificacao inicial falhar.

## Passo 0 -- Resolver paths dos prompts

Para cada argumento recebido:
- Se comeca com `docs/prompts/`: usar o path literal
- Caso contrario: expandir para `docs/prompts/prompt-{arg}.md`

Verificar que cada arquivo existe:
```powershell
foreach ($path in $paths) {
    if (-not (Test-Path $path)) {
        Write-Host "ERRO: arquivo nao encontrado: $path"
        exit 1
    }
}
```

Se algum nao existir: reportar e terminar sem spawnar nada.

## Passo 1 -- Confirmar execucao

Exibir ao operador a lista de tasks que serao executadas em paralelo:

```
/batch: executando N tasks em paralelo:
  [1] docs/prompts/prompt-etapa-5-18.md
  [2] docs/prompts/prompt-etapa-5-19.md
  ...
```

## Passo 2 -- Spawnar todos os agentes em paralelo

IMPORTANTE: enviar TODOS os Agent tool calls em UMA UNICA mensagem ao modelo.
Chamadas em mensagens separadas executam sequencialmente -- nao e o objetivo.

Para cada task, usar:
- subagent_type: "general-purpose"
- isolation: "worktree"
- run_in_background: false
- prompt: ver template abaixo (substituir {PATH} pelo path real do arquivo)

### Template do prompt do sub-agente

```
Voce e um executor autonomo no projeto financas-lab.

Sua unica responsabilidade: ler o arquivo `{PATH}` e executar TODOS os passos
descritos nele de forma completamente autonoma, sem pedir aprovacao ao operador.

## Contexto do ambiente

- Voce esta num worktree git isolado do repositorio financas-lab.
- As convencoes do projeto estao em `CLAUDE.md` -- leia antes de comecar.
- Docker esta rodando (daemon ativo). check.ps1 e check-front.ps1 funcionam.
- Git credentials e gh CLI estao configurados -- push e gh pr create funcionam.

## Sobre skills invocados no prompt

O Skill tool NAO funciona para skills com disable-model-invocation:true.
Para qualquer skill mencionado no prompt (/ship, /write-test, /feature, etc.):
  1. Leia o arquivo `.claude/skills/<nome>/SKILL.md`
  2. Execute a logica descrita nele manualmente, passo a passo

## Execucao

1. Leia `CLAUDE.md`
2. Leia `{PATH}` na integra
3. Execute cada passo do fluxo de execucao descrito no arquivo
4. Nao pule passos. Nao invente passos que nao estao no arquivo.
5. Se um passo falhar: registre o erro e tente corrigir antes de abortar.
   Apenas aborte se o erro for irrecuperavel (ex: check.ps1 falha apos 2 tentativas).

## Relatorio final

Ao terminar (sucesso ou falha), reporte:

Task:     {PATH}
Branch:   <branch criada>
Commits:  <lista de commits>
PR:       <URL do PR ou "nao aberto">
Reviews:  <resumo de cada review>
Status:   OK | BLOQUEADOR: <motivo>
```

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
