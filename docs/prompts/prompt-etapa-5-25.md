# Prompt -- Sub-etapa 5.25: Fix /batch -- paralelismo e verificacao de arquivos

## Contexto

Dois bugs conhecidos na skill `/batch` (`.claude/skills/batch/SKILL.md`):

**Bug 1 -- `Test-Path` em contexto bash:**
O Passo 0 usa um bloco PowerShell com `Test-Path`, mas a skill e executada no contexto
bash do Claude Code. `Test-Path` nao e um comando bash -- falha silenciosamente ou
com erro. Fix: substituir por verificacao usando o tool `Glob` (nativo, sem dependencia
de shell).

**Bug 2 -- Paralelismo sequencial:**
O Passo 2 diz "enviar TODOS os Agent tool calls em UMA UNICA mensagem", mas o modelo
tende a emitir um Agent call, aguardar o resultado, e so entao emitir o proximo --
execucao serial. A instrucao de intencao nao e suficiente. Fix: reestruturar o Passo 2
para que a instrucao seja uma ACAO IMEDIATA e ATOMICA, sem espaco para interpretacao
sequencial.

Leia antes de comecar:
- `.claude/skills/batch/SKILL.md` (arquivo a corrigir)

---

## Mudanca 1 -- Fix Bug 1: verificacao de arquivos via Glob

Substituir o Passo 0 atual por:

```markdown
## Passo 0 -- Resolver e verificar paths

Para cada argumento recebido:
- Se comeca com `docs/prompts/`: usar o path literal
- Caso contrario: expandir para `docs/prompts/prompt-{arg}.md`

Verificar que cada arquivo existe usando o tool Glob com o pattern exato do path.
Se o Glob retornar lista vazia para qualquer path: reportar "ERRO: arquivo nao encontrado: {path}" e terminar sem spawnar nada.
```

---

## Mudanca 2 -- Fix Bug 2: paralelismo via acao imperativa

Substituir o Passo 2 atual por:

```markdown
## Passo 2 -- Spawnar todos os agentes (acao atomica)

Esta etapa e UMA UNICA acao atomica: emitir N Agent tool calls simultaneamente,
onde N e o numero de tasks da lista.

Nao ha loop. Nao ha "primeiro um, depois o outro". Todos os Agent calls sao emitidos
na mesma resposta, ao mesmo tempo, antes de qualquer resultado ser recebido.

Para cada task na lista, os parametros do Agent call sao:
- subagent_type: "general-purpose"
- isolation: "worktree"
- run_in_background: false
- prompt: template abaixo com {PATH} substituido pelo path real

### Template do prompt do sub-agente

[mantem o mesmo template atual -- nao alterar]

---

ACAO OBRIGATORIA: Emita AGORA todos os {N} Agent tool calls acima em uma unica resposta.
N = numero de tasks confirmadas no Passo 1.
Nao escreva nenhum texto adicional antes ou entre os tool calls.
Nao aguarde resultado de nenhum deles antes de emitir os demais.
```

---

## Fluxo de execucao

```
1. git checkout -b fix/etapa-5-25-batch-paralelismo

2. Ler .claude/skills/batch/SKILL.md

3. Aplicar Mudancas 1 e 2 preservando todo o resto do arquivo intacto
   (Passo 1, template do sub-agente, Passo 3 -- nenhuma outra alteracao)

4. Confirmar que o arquivo resultante:
   - Nao contem mais o bloco powershell com Test-Path
   - Passo 2 contem a secao "ACAO OBRIGATORIA" ao final
   - Template do sub-agente esta preservado identico

5. commit: fix(claude): corrige batch skill -- paralelismo e verificacao de arquivos

6. Atualizar docs/progresso.md (registra sub-etapa 5.25)

7. commit: docs(progresso): registra sub-etapa 5.25
   (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-25.md)

8. /ship -> PR; corrigir apontamentos autonomamente
```

## Estrutura de commits

```
fix(claude): corrige batch skill -- paralelismo e verificacao de arquivos
docs(progresso): registra sub-etapa 5.25
```

## Restricoes

- Alterar APENAS `.claude/skills/batch/SKILL.md` (alem de docs/).
- NAO alterar o template do sub-agente (linhas 52-91 do arquivo atual).
- NAO alterar Passo 1 nem Passo 3.
- check.ps1 NAO e necessario (sem alteracao de codigo Java ou frontend).
- O scope do commit e `claude` (sem ponto -- convencao do projeto para `.claude/`).
