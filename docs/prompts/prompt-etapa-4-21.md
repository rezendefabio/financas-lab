# Prompt -- Sub-etapa 4.21: Skill `/audit` para varredura de padroes

## Contexto

Sub-etapa 4.21 da Camada 3. Entrega a skill `/audit` -- varredura de padroes em
`src/main/java/`. Recebe um padrao de busca como argumento (string ou regex) e
reporta todos os matches com contexto (arquivo, linha, trecho).

Terceira skill direta sem subagent do projeto (padroes 4.19 e 4.20 consolidados).
SKILL.md mais curto que os anteriores -- a skill delega o trabalho pesado para a
ferramenta Grep do Claude Code.

Smoke: `/audit "@Entity"` deve retornar exatamente 3 matches (ContaEntity, CategoriaEntity,
TransacaoEntity).

Lembrete de workflow: passar este prompt como TEXTO ao executor (nao como arquivo
na branch). Commitar o `.md` manualmente apos o PR estar aberto.

---

## Padroes que estreiam

Nenhum. Replicacao pura do padrao skill direta (4.19/4.20). SKILL.md mais enxuto
que os anteriores por delegar o Grep ao Claude Code. Categoria: "replicacao de
padrao consolidado" (terceira aplicacao).

---

## Escopo decidido

### Arquivo 1: `.claude/skills/audit/SKILL.md` (NOVO)

```
---
name: audit
description: Varre src/main/java/ buscando um padrao e reporta todos os matches com contexto (arquivo, linha, trecho). Recebe padrao de busca como argumento (string ou regex). Exemplos: /audit TODO, /audit @Deprecated, /audit UnsupportedOperationException.
disable-model-invocation: true
argument-hint: [padrao-de-busca]
---

Voce deve varrer `src/main/java/` buscando o padrao `$ARGUMENTS` e reportar todos
os matches com contexto estruturado.

## Passo 0 -- Validacao

Se `$ARGUMENTS` estiver vazio ou nao informado: escreva a mensagem abaixo e termine.

```
ERRO: /audit requer um padrao de busca.

Exemplos:
  /audit TODO
  /audit @Deprecated
  /audit UnsupportedOperationException
  /audit "@Entity"
  /audit "throw new RuntimeException"
```

## Passo 1 -- Buscar

Use a ferramenta Grep com os parametros:
- pattern: `$ARGUMENTS`
- path: `src/main/java/`
- output_mode: `content`
- `-n`: true (mostrar numeros de linha)

## Passo 2 -- Formatar e reportar

**Se nenhum match encontrado:**
```
/audit "$ARGUMENTS"

Nenhum match encontrado em src/main/java/.
```

**Se matches encontrados:** agrupe por arquivo. Para cada arquivo liste as linhas
com match no formato `  L<numero>: <conteudo da linha>`. Ao final, totalize.

Formato de saida:

```
/audit "<padrao>"

<caminho/do/Arquivo1.java> (<n> match(es))
  L<numero>: <conteudo>
  L<numero>: <conteudo>

<caminho/do/Arquivo2.java> (<n> match(es))
  L<numero>: <conteudo>

Total: <total> match(es) em <arquivos> arquivo(s)
```
```

---

### Arquivo 2: `docs/progresso.md` (EDITAR)

**Mudanca 1 -- linha "Ultima atualizacao":**

Substitua o valor atual por:
```
**Última atualização:** 2026-05-12 (Sub-etapa 4.21 -- Skill /audit para varredura de padroes)
```

**Mudanca 2 -- marcar `/audit` como concluido** (secao "Criterios de pronto"):

Substitua:
```
- [ ] Skill `/audit` (varre modulos buscando padrao especifico)
```
Por:
```
- [x] Skill `/audit` (varre modulos buscando padrao especifico) -- concluido 4.21
```

**Mudanca 3 -- adicionar 4.21 em "Sub-etapas concluidas"** (logo antes da entrada 4.20):

```
- **4.21 -- Skill `/audit` (skill direta sem subagent, terceira aplicacao)** (2026-05-12):
  varredura de padroes em `src/main/java/`. Recebe string ou regex como argumento,
  usa ferramenta Grep do Claude Code, reporta matches agrupados por arquivo com numero
  de linha e trecho. SKILL.md mais enxuto que os anteriores (delega busca ao Grep nativo).
  Smoke: `/audit "@Entity"` retornou 3 matches (ContaEntity, CategoriaEntity,
  TransacaoEntity) -- resultado verificavel e correto. Categoria: "replicacao de padrao
  consolidado" (terceira skill direta). PR #67.
```

**Mudanca 4 -- adicionar em "Historico de mudancas":**

```
- **2026-05-12** -- Sub-etapa 4.21 concluida: skill `/audit` em
  `.claude/skills/audit/SKILL.md`. Terceira skill direta sem subagent. Grep em
  `src/main/java/` com padrao livre. Smoke via `/audit "@Entity"` (3 matches). PR #67.
```

---

### Arquivo 3: `docs/decisoes-claude-code.md` (EDITAR)

Adicione uma subsecao curta logo antes do "Historico de mudancas":

```
## Sub-etapa 4.21 -- Skill `/audit` sem subagent

Terceira replicacao do padrao skill direta (4.19, 4.20, 4.21). Nenhuma decisao
estrutural nova.

SKILL.md mais curto que os anteriores: a skill instrui Claude Code a usar o tool
`Grep` diretamente, sem scripting PowerShell. O trabalho pesado (regex, indexacao
de arquivos, formatacao de output) e feito pelo Grep nativo do Claude Code.

Padrao consolidado: skills procedurais de baixa complexidade delegam para tools
nativos do Claude Code (Grep, Glob, Write, Bash) conforme necessidade -- sem
overhead de subagent.
```

Adicione ao "Historico de mudancas":

```
- **2026-05-12** -- Sub-etapa 4.21 concluida: skill `/audit` direta. Terceira
  replicacao do padrao 4.19. Grep nativo, output estruturado. PR #67.
```

---

## Estado esperado ao iniciar

```powershell
git branch --show-current   # deve retornar: main
git status                  # deve retornar: nothing to commit, working tree clean
git log --oneline -1        # deve mostrar squash do PR #66 (4.20) no topo
```

---

## Tarefas

### Tarefa 1 -- Verificar estado inicial (ADR-011)

```powershell
git branch --show-current
git status
git log --oneline -1
```

### Tarefa 2 -- Criar branch

```powershell
git checkout -b feat/etapa-4-21-audit-skill
git branch --show-current   # deve retornar: feat/etapa-4-21-audit-skill
```

### Tarefa 3 -- Criar `.claude/skills/audit/SKILL.md`

Pre-condicao:
```powershell
Test-Path ".claude/skills/audit/"  # deve retornar: False
```

```powershell
New-Item -ItemType Directory -Path ".claude/skills/audit/"
```

Use Write para criar `.claude/skills/audit/SKILL.md` com o conteudo prescrito.
Codificacao UTF-8 sem BOM.

Pos-condicao:
```powershell
Test-Path ".claude/skills/audit/SKILL.md"          # deve retornar: True
Select-String "disable-model-invocation" ".claude/skills/audit/SKILL.md"  # deve ter match
Select-String "context: fork" ".claude/skills/audit/SKILL.md"              # NAO deve ter match
Select-String "Grep" ".claude/skills/audit/SKILL.md"                       # deve ter match
```

### Tarefa 4 -- Primeiro commit

```powershell
git add ".claude/skills/audit/SKILL.md"
git status
```

Commit (scope `claude` sem ponto):
```
feat(claude): adiciona skill /audit para varredura de padroes no codigo
```

### Tarefa 5 -- Atualizar `docs/progresso.md`

Leia o arquivo antes de editar. Aplique as 4 mudancas prescritas com Edit.

Pos-condicao:
```powershell
Select-String "4.21" "docs/progresso.md" | Measure-Object | Select-Object -Expand Count
# deve retornar > 0
```

### Tarefa 6 -- Segundo commit

```powershell
git add "docs/progresso.md"
git status
```

Commit:
```
docs(progresso): registra sub-etapa 4.21 e criterio /audit como concluido
```

### Tarefa 7 -- Atualizar `docs/decisoes-claude-code.md`

Leia o final do arquivo antes de editar. Adicione a subsecao 4.21 antes do
"Historico de mudancas". Garanta linha em branco antes e depois de cada `##`.

Pos-condicao:
```powershell
Select-String "4.21" "docs/decisoes-claude-code.md" | Measure-Object | Select-Object -Expand Count
# deve retornar > 0
```

### Tarefa 8 -- Terceiro commit

```powershell
git add "docs/decisoes-claude-code.md"
git status
```

Commit:
```
docs(decisoes): registra skill /audit como terceira replicacao do padrao direta
```

### Tarefa 9 -- Smoke

Execute:
```
/audit "@Entity"
```

Criterios de sucesso:
- [ ] Skill executa sem erro
- [ ] Retorna exatamente 3 arquivos: ContaEntity.java, CategoriaEntity.java, TransacaoEntity.java
- [ ] Output agrupado por arquivo com numero de linha
- [ ] Total: 3 matches em 3 arquivos

Se qualquer criterio falhar: reporte o erro literal. Nao tente auto-corrigir.

### Tarefa 10 -- Entregar via `/ship`

Com os 3 commits feitos e smoke OK, execute:
```
/ship
```

A skill deve passar as 4 verificacoes, rodar check.ps1, push e criar o PR #67.

---

## Restricoes e freios

- NAO usar scope `.claude` em commits -- usar `claude` sem ponto (licao 4.19).
- NAO modificar o CLAUDE.md.
- NAO editar bounded contexts existentes.
- Se hook bloquear commit: leia a mensagem, corrija sem `--no-verify`.

---

## Estrutura de commits

```
feat(claude): adiciona skill /audit para varredura de padroes no codigo
docs(progresso): registra sub-etapa 4.21 e criterio /audit como concluido
docs(decisoes): registra skill /audit como terceira replicacao do padrao direta
```

---

## Estado esperado ao terminar

- PR #67 aberto (criado via `/ship`).
- Working tree limpa.
- `.claude/skills/audit/SKILL.md` existente.
- `progresso.md` com 4.21 registrada e `/audit` marcado como `[x]`.
- `decisoes-claude-code.md` com subsecao 4.21 adicionada.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO commitar o arquivo de prompt na branch (workflow novo -- operador faz isso
  manualmente apos PR aberto).
