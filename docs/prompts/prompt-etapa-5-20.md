# Prompt -- Sub-etapa 5.20: Skill /batch (execucao paralela de tasks)

## Contexto

Primeira sub-etapa de infraestrutura da fabrica orientada a Camada A da visao Boris Cherny:
o operador invoca `/batch etapa-5-18 etapa-5-19` e o skill spawna sub-agentes em paralelo
via Agent tool com `isolation: worktree`. Cada sub-agente implementa uma task autonomamente,
roda o gate, abre o PR e dispara os reviews. O operador nao abre terminal adicional algum.

Este skill elimina o modelo "dois terminais manuais". O smoke test imediato serao as
sub-etapas 5.18 (Categorias) e 5.19 (Transacoes) que ja tem prompts prontos em
`docs/prompts/`.

Camada 3 (infraestrutura de fabrica). Sem mudancas no produto.

---

## O que criar

Um unico arquivo: `.claude/skills/batch/SKILL.md`

Seguir exatamente o padrao flat dos skills existentes em `.claude/skills/*/SKILL.md`.
Leia `.claude/skills/ship/SKILL.md` e `.claude/skills/review-pr/SKILL.md` como referencia
de estilo antes de escrever.

---

## Conteudo do SKILL.md

```markdown
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
```

---

## Fluxo de execucao do executor

```
1. git checkout -b feat/etapa-5-20-skill-batch

2. Ler .claude/skills/ship/SKILL.md e .claude/skills/review-pr/SKILL.md como referencia

3. Criar .claude/skills/batch/SKILL.md com o conteudo acima

4. Validacao destrutiva (ADR-011):
   - Test-Path .claude/skills/batch/SKILL.md  -- deve retornar True
   - Verificar que o frontmatter tem name, description e disable-model-invocation: true

5. git status  -- confirmar que so batch/SKILL.md esta staged

6. commit: feat(claude): adiciona skill /batch para execucao paralela de tasks

7. Atualizar docs/progresso.md (registra sub-etapa 5.20)

8. commit: docs(progresso): registra sub-etapa 5.20
   (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-20.md)

9. /ship -> PR; corrigir apontamentos autonomamente
```

---

## Estrutura de commits (5.20)

```
feat(claude): adiciona skill /batch para execucao paralela de tasks
docs(progresso): registra sub-etapa 5.20
```

---

## Restricoes

- NAO modificar skills existentes.
- NAO adicionar logica de retry automatico dentro do skill -- isso e responsabilidade
  do sub-agente, nao do orquestrador.
- O skill NAO deve fazer push ou commit por conta propria -- apenas spawna agentes.
- `disable-model-invocation: true` e obrigatorio no frontmatter.
- O template do prompt do sub-agente deve ser copiado literalmente para o SKILL.md --
  nao resumir, nao parafrasear. O sub-agente precisa das instrucoes completas.

---

## Como usar apos merge

```
# Iniciar sessao do executor (uma unica vez):
claude --dangerously-skip-permissions

# Invocar o batch:
/batch etapa-5-18 etapa-5-19
```

O executor abre dois worktrees em paralelo, implementa Categorias e Transacoes
simultaneamente, abre dois PRs e reporta os resultados. Zero terminais adicionais.

---

## Estado esperado ao terminar

- PR aberto com 2 commits acima de main.
- `.claude/skills/batch/SKILL.md` criado com frontmatter correto e logica completa.
- `.\scripts\check.ps1` verde (nao ha codigo de produto -- apenas o skill).
- Smoke test descrito na secao "Como usar" pronto para ser executado apos merge.
