# Prompt -- Sub-etapa 4.20: Skill `/ship` para entrega de PR

## Contexto

Sub-etapa 4.20 da Camada 3. Entrega a skill `/ship` -- orquestradora de entrega.
Executa em sequencia: verificacoes de seguranca, gate `check.ps1`, push, criacao de PR.

Segunda skill direta sem subagent do projeto (padrao inaugurado na 4.19). Nenhum
argumento necessario -- a skill extrai titulo do ultimo commit e lista de commits da
branch automaticamente.

O smoke desta sub-etapa e a propria skill em acao: use `/ship` para criar o PR da 4.20.

---

## Padroes que estreiam

**Segunda aplicacao do padrao "skill direta sem subagent" (4.19).** Sem novidades
estruturais -- apenas o segundo caso de skill que instrui Claude Code a agir
diretamente via Write + Bash. Categoria: "replicacao de padrao consolidado" (mesma
da 4.12).

**Smoke natural e completo.** Diferente das skills anteriores (smokes parciais ou com
cobaia artificial), o smoke do `/ship` e a propria entrega da 4.20: a skill cria o PR
da branch `feat/etapa-4-20-ship-skill`. Se funcionar, smoke completo e automatico.

---

## Escopo decidido

### Arquivo 1: `.claude/skills/ship/SKILL.md` (NOVO)

```
---
name: ship
description: Orquestra entrega de PR: verifica estado, roda check.ps1 (gate completo), push, cria PR com titulo do ultimo commit e lista de commits da branch. Sem argumento necessario.
disable-model-invocation: true
---

Voce deve entregar a branch atual como PR no GitHub. Execute todos os passos em ordem.
Pare e reporte ao operador se qualquer verificacao ou comando falhar.

## Passo 0 -- Verificacoes de seguranca (ADR-011)

Execute cada verificacao via Bash (PowerShell). Se qualquer uma falhar: reporte
exatamente qual falhou e termine sem fazer push ou PR.

**V1 -- branch nao e main:**
```powershell
$branch = git branch --show-current
if ($branch -eq "main") {
    Write-Host "ERRO: voce esta em main. /ship opera apenas em branches de feature."
    exit 1
}
Write-Host "Branch: $branch"
```

**V2 -- working tree limpa:**
```powershell
$status = git status --porcelain
if ($status) {
    Write-Host "ERRO: working tree nao esta limpa. Commite ou descarte as mudancas antes de /ship."
    Write-Host $status
    exit 1
}
Write-Host "Working tree: limpa"
```

**V3 -- ha commits acima de main:**
```powershell
$ahead = git rev-list --count main..HEAD
if ($ahead -eq 0) {
    Write-Host "ERRO: nenhum commit acima de main. Nao ha nada para shipar."
    exit 1
}
Write-Host "Commits acima de main: $ahead"
```

**V4 -- gh CLI disponivel e autenticado:**
```powershell
gh auth status 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: gh CLI nao autenticado. Rode 'gh auth login' antes de /ship."
    exit 1
}
Write-Host "gh CLI: autenticado"
```

## Passo 1 -- Gate completo

```powershell
.\scripts\check.ps1
```

Se exit code != 0: escreva "ERRO: gate falhou (check.ps1 exit $LASTEXITCODE). Corrija
antes de shipar." e termine. Nao faca push.

ATENCAO: check.ps1 exige Docker Desktop rodando (Testcontainers). Se Docker nao
estiver ativo, o gate falhara na verificacao de Docker antes de qualquer teste.

## Passo 2 -- Push

```powershell
$branch = git branch --show-current
git push -u origin $branch
```

Se exit code != 0: escreva "ERRO: push falhou." com o output do git e termine.
Nao crie PR.

## Passo 3 -- Extrair informacoes para o PR

```powershell
$titulo = git log -1 --format="%s"
$commits = git log main..HEAD --oneline
$branch = git branch --show-current
```

## Passo 4 -- Criar PR

Monte o body do PR com a lista de commits e o aviso padrao.
Execute:

```powershell
gh pr create `
  --title $titulo `
  --base main `
  --body "## Commits nesta branch

$commits

---
_Gerado via /ship. Adicione detalhes de decisao e smoke se necessario._"
```

Se exit code != 0: escreva "ERRO: gh pr create falhou." com o output e termine.

## Passo 5 -- Relatorio final

Produza:

```
/ship concluido.

Branch:   <branch>
Gate:     check.ps1 passou
Push:     OK
PR:       <URL retornada pelo gh pr create>
Titulo:   <titulo>
Commits:  <numero> acima de main

Proximos passos sugeridos:
  /review-pr <numero-do-pr>   -- revisao critica antes do merge
  /review-arch <numero-do-pr> -- validacao de ADRs arquiteturais
```
```

---

### Arquivo 2: `docs/progresso.md` (EDITAR)

**Mudanca 1 -- linha "Ultima atualizacao":**

Substitua o valor atual por:
```
**Última atualização:** 2026-05-12 (Sub-etapa 4.20 -- Skill /ship para entrega de PR)
```

**Mudanca 2 -- marcar `/ship` como concluido** (secao "Criterios de pronto"):

Substitua:
```
- [ ] Skill `/ship` (lint + test + build + push + PR)
```
Por:
```
- [x] Skill `/ship` (lint + test + build + push + PR) -- concluido 4.20
```

**Mudanca 3 -- adicionar 4.20 em "Sub-etapas concluidas"** (logo antes da entrada 4.19):

```
- **4.20 -- Skill `/ship` (skill direta sem subagent, segunda aplicacao)** (2026-05-12):
  segunda skill sem `context: fork` e sem `agent:` do projeto. Orquestra entrega de PR:
  4 verificacoes de seguranca (branch != main, working tree limpa, commits acima de main,
  gh autenticado), gate `check.ps1` (mvn clean verify + Docker), push com `-u origin`,
  criacao de PR via `gh pr create` com titulo extraido do ultimo commit e lista de commits
  da branch no body. Para em qualquer falha, sem push ou PR criado. Smoke natural e
  completo: a propria skill criou o PR #66 da sub-etapa 4.20. Categoria: "replicacao de
  padrao consolidado" (segunda skill direta, mesma estrutura da 4.19). PR #66.
```

**Mudanca 4 -- adicionar em "Historico de mudancas":**

```
- **2026-05-12** -- Sub-etapa 4.20 concluida: skill `/ship` em
  `.claude/skills/ship/SKILL.md`. Segunda skill direta sem subagent. Gate integrado
  (check.ps1), 4 verificacoes de seguranca, push + PR automaticos. Smoke natural
  completo (skill criou o proprio PR). Categoria "replicacao de padrao consolidado".
  CLAUDE.md NAO atualizado. PR #66.
```

---

### Arquivo 3: `docs/decisoes-claude-code.md` (EDITAR)

Adicione uma subsecao curta logo antes do "Historico de mudancas" (garanta linha em
branco antes e depois do `##`):

```
## Sub-etapa 4.20 -- Skill `/ship` sem subagent

Segunda aplicacao do padrao inaugurado na 4.19 (skill direta sem `context: fork`).
Nenhuma decisao estrutural nova -- replicacao pura. Detalhes em [[project-scope-claude-dot]].

Criterio de escolha confirmado: skill direta e adequada quando a tarefa e
puramente procedural (sequencia de comandos shell com logica de fluxo simples).
`/ship` confirma o padrao: 5 passos bem definidos, sem raciocinio de dominio.

Nota de smoke: `/ship` testou a si propria ao criar o PR #66. Primeiro smoke
verdadeiramente automatico e completo do projeto (sem cobaia artificial e sem
parcialidade -- gate real, push real, PR real).
```

Adicione ao "Historico de mudancas":

```
- **2026-05-12** -- Sub-etapa 4.20 concluida: skill `/ship` direta (sem subagent).
  Replicacao pura do padrao 4.19. Smoke natural completo (skill criou o proprio PR).
  PR #66.
```

---

## Estado esperado ao iniciar

```powershell
git branch --show-current   # deve retornar: main
git status                  # deve retornar: nothing to commit, working tree clean
git log --oneline -1        # deve mostrar squash do PR #65 (4.19) no topo
```

Se qualquer condicao falhar: pare e reporte.

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
git checkout -b feat/etapa-4-20-ship-skill
git branch --show-current   # deve retornar: feat/etapa-4-20-ship-skill
```

### Tarefa 3 -- Criar `.claude/skills/ship/SKILL.md`

Pre-condicao:
```powershell
Test-Path ".claude/skills/ship/"  # deve retornar: False
```

```powershell
New-Item -ItemType Directory -Path ".claude/skills/ship/"
```

Use Write para criar `.claude/skills/ship/SKILL.md` com o conteudo prescrito.
Codificacao UTF-8 sem BOM.

Pos-condicao:
```powershell
Test-Path ".claude/skills/ship/SKILL.md"   # deve retornar: True
Select-String "disable-model-invocation" ".claude/skills/ship/SKILL.md"  # deve ter match
Select-String "context: fork" ".claude/skills/ship/SKILL.md"              # NAO deve ter match
```

### Tarefa 4 -- Primeiro commit

```powershell
git add ".claude/skills/ship/SKILL.md"
git status
```

Commit (scope `claude` sem ponto -- licao da 4.19):
```
feat(claude): adiciona skill /ship para entrega de PR
```

### Tarefa 5 -- Atualizar `docs/progresso.md`

Leia o arquivo antes de editar. Aplique as 4 mudancas prescritas. Use Edit para
cada mudanca. Nao altere nenhum trecho alem dos prescritos.

Pos-condicao:
```powershell
Select-String "4.20" "docs/progresso.md" | Measure-Object | Select-Object -Expand Count
# deve retornar > 0
```

### Tarefa 6 -- Segundo commit

```powershell
git add "docs/progresso.md"
git status
```

Commit:
```
docs(progresso): registra sub-etapa 4.20 e criterio /ship como concluido
```

### Tarefa 7 -- Atualizar `docs/decisoes-claude-code.md`

Leia o final do arquivo antes de editar. Adicione a subsecao 4.20 antes do
"Historico de mudancas". Garanta linha em branco antes e depois de cada `##`.

Pos-condicao:
```powershell
Select-String "4.20" "docs/decisoes-claude-code.md" | Measure-Object | Select-Object -Expand Count
# deve retornar > 0
```

### Tarefa 8 -- Terceiro commit

```powershell
git add "docs/decisoes-claude-code.md"
git status
```

Commit:
```
docs(decisoes): registra replicacao do padrao skill direta na 4.20
```

### Tarefa 9 -- Smoke (a propria skill em acao)

Com os 3 commits feitos na branch `feat/etapa-4-20-ship-skill`, execute:

```
/ship
```

A skill deve:
1. Passar as 4 verificacoes (branch != main ✓, status limpa ✓, commits > 0 ✓, gh ✓)
2. Rodar `check.ps1` e passar (exige Docker ativo)
3. Fazer push da branch
4. Criar o PR com titulo extraido do ultimo commit
5. Exibir relatorio com URL do PR

**Criterios de sucesso:**
- [ ] 4 verificacoes passaram
- [ ] check.ps1 exit 0
- [ ] Push realizado (branch aparece no remote)
- [ ] PR criado com URL valida
- [ ] Relatorio exibido com "proximos passos"

Se qualquer criterio falhar: reporte o erro literal. Nao tente auto-corrigir.
Abrir 4.20.1 e decisao do operador.

---

## Restricoes e freios

- NAO usar scope `.claude` em commits -- usar `claude` sem ponto (licao 4.19).
- NAO modificar o CLAUDE.md. Convencao de skills ja registrada na 4.11.
- NAO editar bounded contexts existentes ou qualquer arquivo Java.
- NAO criar a migration Flyway de nenhum bounded context.
- Se hook bloquear commit: leia a mensagem, corrija sem `--no-verify`.

---

## Estrutura de commits

3 commits, nesta ordem:

```
feat(claude): adiciona skill /ship para entrega de PR
docs(progresso): registra sub-etapa 4.20 e criterio /ship como concluido
docs(decisoes): registra replicacao do padrao skill direta na 4.20
```

---

## Validacao antes do smoke

```powershell
git log --oneline feat/etapa-4-20-ship-skill ^main
# deve mostrar exatamente 3 commits

git diff main --name-only
# deve mostrar exatamente:
#   .claude/skills/ship/SKILL.md
#   docs/decisoes-claude-code.md
#   docs/progresso.md

Select-String "disable-model-invocation: true" ".claude/skills/ship/SKILL.md"
# deve ter match

Select-String "context: fork" ".claude/skills/ship/SKILL.md"
# NAO deve ter match

Select-String "check.ps1" ".claude/skills/ship/SKILL.md"
# deve ter match

Select-String "gh pr create" ".claude/skills/ship/SKILL.md"
# deve ter match
```

---

## O smoke e o PR

O smoke desta sub-etapa e a propria invocacao de `/ship`. A skill cria o PR #66.
Nao ha separacao entre "validar antes do PR" e "criar PR" -- sao a mesma acao.

Reporte ao operador:
1. Output completo da execucao do `/ship` (5 passos).
2. URL do PR criado.
3. Criterios de smoke (lista acima) -- marcados com ✓ ou ✗.
4. Se qualquer criterio falhou: erro literal.

---

## Estado esperado ao terminar

- Branch `feat/etapa-4-20-ship-skill` com 3 commits acima de main.
- PR #66 aberto (criado pela propria skill).
- Working tree limpa.
- `.claude/skills/ship/SKILL.md` existente com conteudo correto.
- `progresso.md` com 4.20 registrada e `/ship` marcado como `[x]`.
- `decisoes-claude-code.md` com subsecao 4.20 adicionada.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao explicita do operador.
- NAO abrir 4.20.1 sem relatar primeiro ao operador o que falhou.
- NAO rodar `/ship` mais de uma vez (cria PRs duplicados).
