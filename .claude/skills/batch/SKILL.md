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
