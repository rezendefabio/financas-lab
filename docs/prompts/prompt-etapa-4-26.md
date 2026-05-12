# Prompt -- Sub-etapa 4.26: Split de `decisoes-claude-code.md`

## Contexto

Sub-etapa de manutencao de docs por crescimento (categoria inaugurada pela 4.13).
`decisoes-claude-code.md` esta com ~843+ linhas apos a 4.24, hook 4.4 alertando em
cada commit. A 4.26 resolve o debito antes de abrir a Camada 4.

Criterio de split: identico ao usado na 4.16 para o `decisoes.md` -- **por tema/periodo**.
A secao `## Camada 3 -- Configuracao do Claude Code` (sub-etapas 4.0 a 4.18, hooks e
primeiros subagents) e historia estavel. As secoes `## Sub-etapa 4.19` em diante sao
o conteudo ativo que sera lido com mais frequencia.

Nota: a Camada 3 ja foi fechada na 4.25 (status "✅ Concluída" no progresso.md).
NAO tentar fechar novamente.

Lembrete de workflow: passar este prompt como TEXTO ao executor. Commitar o `.md` manualmente
apos o PR estar aberto.

---

## Escopo decidido

### Arquivo 1: `docs/decisoes-claude-code-historico.md` (NOVO)

Crie o arquivo com o conteudo abaixo. Ele contem a secao historica do `decisoes-claude-code.md`
atual (desde o header ate o final da secao `### Claude Code hooks nativos`).

**Conteudo completo do arquivo:**

```markdown
# Decisoes — Camada 3 (Historico 4.0 a 4.18)

> Arquivo de historico arquivado na Sub-etapa 4.26.
> Contem decisoes operacionais das sub-etapas 4.0 a 4.18 (hooks, primeiros subagents,
> manutencao de docs). Conteudo estavel -- nao editado apos arquivamento.
> Para decisoes das sub-etapas 4.19 em diante, ver `docs/decisoes-claude-code.md`.

---
```

Seguido do conteudo extraido do `decisoes-claude-code.md` atual:
- Da linha 11 (`## Camada 3 — Configuração do Claude Code`) ate o final da subsecao
  `### Claude Code hooks nativos` (linha que contem `"Diferente de git hooks: atua sobre
  comportamento do agente, não validação de código."`)

Ou seja: toda a secao `## Camada 3 - Configuração do Claude Code` com todas as suas
subsecoes `###` (4.0 layout, 4.1 Conventional Commits, 4.2 Encoding, ... 4.18 integration
tests, Claude Code hooks nativos).

**Instrucao para o executor:** Leia `docs/decisoes-claude-code.md` completo. Identifique
a secao a mover (da linha com `## Camada 3 — Configuração do Claude Code` ate a linha que
termina com `"Diferente de git hooks..."` da subsecao `### Claude Code hooks nativos`).
Crie `docs/decisoes-claude-code-historico.md` com o header acima + conteudo extraido.
Use Write. Encoding UTF-8 sem BOM.

Pos-condicao:
```powershell
Test-Path "docs/decisoes-claude-code-historico.md"   # deve retornar: True
Select-String "Camada 3" "docs/decisoes-claude-code-historico.md"   # deve ter match
Select-String "4.18" "docs/decisoes-claude-code-historico.md"       # deve ter match
```

---

### Arquivo 2: `docs/decisoes-claude-code.md` (EDITAR)

Substitua o conteudo do arquivo pelo que segue. O arquivo novo mantem apenas:
1. O cabecalho original (linhas 1-9 do arquivo atual)
2. Uma linha de referencia para o historico
3. As secoes das sub-etapas 4.19 em diante (cada `## Sub-etapa X.Y` e o `## Historico`)

**Instrucao para o executor:** Leia o arquivo atual completo. O novo conteudo do arquivo
deve ser:

```
# Decisoes — Camada 3 (Configuracao do Claude Code)

> Documento dedicado a decisoes operacionais da Camada 3 do projeto: hooks, subagents, skills, padroes de validacao destrutiva, convencoes operacionais de Claude Code.
> Origem: separado de `docs/decisoes.md` na Sub-etapa 4.16 quando o arquivo original cruzou 800 linhas (trigger do hook 4.4 modo warn).
> Para decisoes fundacionais do projeto (Stack, Arquitetura, Convencoes de codigo, Politica de debito, Comandos atomicos, Frontend, Modelo financeiro, Principios do blueprint), ver `decisoes.md`.

**Data de criacao:** 2026-05-11 (Sub-etapa 4.16)

> **Historico arquivado:** decisoes das sub-etapas 4.0 a 4.18 (hooks, primeiros subagents, manutencao de docs) foram movidas para `docs/decisoes-claude-code-historico.md` na Sub-etapa 4.26.

---
```

Seguido de todas as secoes `## Sub-etapa X.Y` (4.19, 4.20, 4.21, 4.22, 4.23, 4.24, e
4.25 se ja mergeada) e a secao `## Historico de mudancas`.

Use Write (nao Edit) para substituir o arquivo inteiro.

Pos-condicao:
```powershell
(Get-Content "docs/decisoes-claude-code.md" | Measure-Object -Line).Lines
# deve ser menor que 400 linhas apos o split

Select-String "4.26" "docs/decisoes-claude-code.md"   # deve ter match (historico de mudancas)
Select-String "4.19" "docs/decisoes-claude-code.md"   # deve ter match (sub-etapa ainda presente)
Select-String "4.0" "docs/decisoes-claude-code.md"    # NAO deve ter match como secao ativa
```

---

### Arquivo 3: `CLAUDE.md` (EDITAR)

Leia o arquivo antes de editar. Localize a linha que menciona `decisoes-claude-code.md`
em "Onde buscar mais" e adicione uma linha abaixo dela para o historico:

Substitua:
```
- `decisoes-claude-code.md` -- decisoes operacionais da Camada 3 (hooks, subagents, skills, padroes de validacao destrutiva).
```
Por:
```
- `decisoes-claude-code.md` -- decisoes operacionais da Camada 3 (sub-etapas 4.19+). Historico arquivado em `decisoes-claude-code-historico.md` (4.0-4.18).
```

Pos-condicao:
```powershell
Select-String "decisoes-claude-code-historico" "CLAUDE.md"   # deve ter match
```

---

### Arquivo 4: `docs/progresso.md` (EDITAR)

Leia o arquivo antes de editar. Aplique as 3 mudancas abaixo.

Nota: a Camada 3 ja foi fechada na 4.25 (status "✅ Concluída" na tabela e na secao
dedicada). NAO tente fechar novamente -- ja esta feito.

**Mudanca 1 -- linha "Ultima atualizacao":**

Substitua o valor atual por:
```
**Última atualização:** 2026-05-12 (Sub-etapa 4.26 -- Split de decisoes-claude-code.md)
```

**Mudanca 2 -- adicionar 4.26 em "Sub-etapas concluidas"** (logo antes de 4.25 ou 4.24,
dependendo do estado do arquivo quando esta branch for criada):

```
- **4.26 -- Split de `decisoes-claude-code.md`** (2026-05-12): terceira aplicacao da
  categoria "manutencao de docs por crescimento" (apos 4.13 e 4.16). Criterio de corte:
  tematico/historico (identico a 4.16 para decisoes.md). Secao 4.0-4.18 arquivada em
  `decisoes-claude-code-historico.md`; sub-etapas 4.19+ permanecem no arquivo ativo.
  `decisoes-claude-code.md` reduz de ~880+ para ~350 linhas. CLAUDE.md atualizado com
  link para historico. Debito da 4.24 (hook warn 808 linhas) resolvido. PR #72.
```

**Mudanca 3 -- adicionar em "Historico de mudancas":**

```
- **2026-05-12** -- Sub-etapa 4.26 concluida: split de `decisoes-claude-code.md`.
  Historico 4.0-4.18 arquivado. Arquivo ativo reduzido. CLAUDE.md atualizado. PR #72.
```

---

## Estado esperado ao iniciar

```powershell
git branch --show-current   # deve retornar: main
git status                  # deve retornar: nothing to commit, working tree clean
git log --oneline -3        # deve mostrar squash do PR mais recente no topo
```

Se a 4.25 ainda nao foi mergeada: aguarde o merge antes de iniciar a 4.26. O conteudo
da 4.25 em `decisoes-claude-code.md` precisa estar em main para ser incluido no arquivo
ativo apos o split.

---

## Tarefas

### Tarefa 1 -- Verificar estado inicial

```powershell
git branch --show-current
git status
git log --oneline -3
(Get-Content "docs/decisoes-claude-code.md" | Measure-Object -Line).Lines
# deve mostrar > 800 linhas (confirmando necessidade do split)
```

### Tarefa 2 -- Criar branch

```powershell
git checkout -b docs/etapa-4-26-split-decisoes-claude-code
git branch --show-current
```

### Tarefa 3 -- Criar `docs/decisoes-claude-code-historico.md`

Leia `docs/decisoes-claude-code.md` completo antes de criar o historico.

Identifique exatamente onde termina a secao a arquivar: fim da subsecao
`### Claude Code hooks nativos` (linha que diz algo como "Diferente de git hooks:
atua sobre comportamento do agente, nao validacao de codigo."), antes da linha
`## Sub-etapa 4.19`.

Crie `docs/decisoes-claude-code-historico.md` com:
1. Header de historico (conforme prescrito)
2. Conteudo da secao arquivada (do `## Camada 3` ate o fim de `### Claude Code hooks nativos`)

Use Write. Encoding UTF-8 sem BOM.

### Tarefa 4 -- Reescrever `docs/decisoes-claude-code.md`

Construa o novo conteudo do arquivo:
1. Cabecalho original (5 primeiras linhas do arquivo atual)
2. Linha em branco
3. Nota de referencia ao historico
4. Separador `---`
5. Todas as secoes `## Sub-etapa X.Y` (4.19 em diante, incluindo 4.25 se presente)
6. Secao `## Historico de mudancas` com entrada da 4.26 adicionada

Use Write para substituir o arquivo inteiro.

### Tarefa 5 -- Verificar contagem de linhas

```powershell
(Get-Content "docs/decisoes-claude-code.md" | Measure-Object -Line).Lines
# deve ser < 400

(Get-Content "docs/decisoes-claude-code-historico.md" | Measure-Object -Line).Lines
# deve ser entre 400 e 800
```

### Tarefa 6 -- Editar `CLAUDE.md`

Aplique a mudanca prescrita (atualizar linha do decisoes-claude-code com link para historico).

### Tarefa 7 -- Editar `docs/progresso.md`

Aplique as 3 mudancas prescritas.

### Tarefa 8 -- Commit

```powershell
git add "docs/decisoes-claude-code-historico.md" "docs/decisoes-claude-code.md" "CLAUDE.md" "docs/progresso.md"
git status
# deve mostrar exatamente esses 4 arquivos (decisoes-historico como novo, demais modificados)
```

Commit:
```
docs(decisoes): arquiva historico 4.0-4.18 em decisoes-claude-code-historico.md
```

### Tarefa 9 -- Validacao pre-ship

```powershell
git log --oneline docs/etapa-4-26-split-decisoes-claude-code ^main
# deve mostrar exatamente 1 commit

git diff main --name-only
# deve mostrar exatamente:
#   CLAUDE.md
#   docs/decisoes-claude-code-historico.md (novo)
#   docs/decisoes-claude-code.md
#   docs/progresso.md

(Get-Content "docs/decisoes-claude-code.md" | Measure-Object -Line).Lines
# deve ser < 400

Select-String "decisoes-claude-code-historico" "CLAUDE.md"   # deve ter match
Select-String "4.19" "docs/decisoes-claude-code.md"          # deve ter match
Select-String "4.0" "docs/decisoes-claude-code-historico.md" # deve ter match
Select-String "4.26" "docs/progresso.md"                     # deve ter match
```

### Tarefa 10 -- Entregar via `/ship`

```
/ship
```

---

## Restricoes e freios

- NAO alterar conteudo das secoes movidas -- apenas mover, nao editar.
- NAO remover secoes 4.19+ do arquivo ativo.
- NAO modificar `decisoes.md` (arquivo diferente de `decisoes-claude-code.md`).
- NAO tocar em arquivos Java ou SQL.
- NAO modificar hooks ou agents.
- NAO tentar fechar Camada 3 -- ja esta fechada na 4.25.
- Se hook bloquear commit: leia a mensagem, corrija sem `--no-verify`.

---

## Estrutura de commits

```
docs(decisoes): arquiva historico 4.0-4.18 em decisoes-claude-code-historico.md
```

(1 commit -- todos os arquivos no mesmo commit porque e uma operacao atomica de split)

---

## Estado esperado ao terminar

- PR #72 aberto.
- `docs/decisoes-claude-code-historico.md` criado.
- `docs/decisoes-claude-code.md` reduzido (<400 linhas).
- `CLAUDE.md` com link para historico.
- `docs/progresso.md` com 4.26 registrada.
- Hook 4.4 para de alertar em `decisoes-claude-code.md`.
- Camada 3 ja estava fechada pela 4.25. Camada 4 pronta para iniciar.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO commitar o arquivo de prompt na branch (operador faz manualmente).
- NAO rodar `/ship` mais de uma vez.
