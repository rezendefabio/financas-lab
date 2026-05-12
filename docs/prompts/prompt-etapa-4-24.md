# Prompt -- Sub-etapa 4.24: Skill `/migrate` (orquestradora de skills)

## Contexto

Sub-etapa 4.24 da Camada 3. Entrega a skill `/migrate` -- skill orquestradora que encadeia
`/write-migration` e `/write-test` em sequencia para um bounded context recebido como argumento.

Patron novo: **skill orquestradora de skills**. Diferente das skills diretas (4.19-4.22, executam
shell/ferramentas diretamente) e das skills-com-fork (4.17-4.18, 4.23, delegam a subagent),
`/migrate` instrui o Claude Code principal a invocar outras skills em sequencia via Skill tool.

Lembrete de workflow: passar este prompt como TEXTO ao executor. Commitar o `.md` manualmente
apos o PR estar aberto.

---

## Padroes que estreiam

**Skill orquestradora de skills.** Primeira skill do projeto que nao executa ferramentas
diretamente nem delega a subagent unico, mas instrui Claude Code a invocar outras skills em
sequencia. Categoria nova vs as anteriores:

| Categoria | Exemplos | Mecanismo |
|-----------|----------|-----------|
| Skill direta | /ship, /audit, /feature | Instrucoes para Claude Code usar Bash/Write/Grep |
| Skill-com-fork | /write-test, /write-migration | context: fork + agent: <nome> |
| Skill orquestradora | /migrate (4.24) | Instrucoes para Claude Code invocar outras skills |

`/migrate` e a mais simples das tres em conteudo (sem logica propria) mas a mais poderosa
em composicao: combina dois geradores existentes em workflow unico.

---

## Escopo decidido

### Arquivo 1: `.claude/skills/migrate/SKILL.md` (NOVO)

Conteudo prescrito:

```markdown
---
name: migrate
description: Fluxo completo pos-/feature: gera migration SQL e unit tests para domain POJO do bounded context. Encadeia /write-migration (subagent migration-writer) e /write-test (subagent test-writer). Para se migration falhar. Argumento: nome do bounded context (snake_case).
disable-model-invocation: true
argument-hint: [nome-do-bounded-context]
---

Execute em sequencia para o bounded context `$ARGUMENTS`. Pare e reporte ao operador
se qualquer passo falhar. Nao prossiga para o proximo passo em caso de falha.

## Passo 0 -- Validar argumento

Se `$ARGUMENTS` vazio ou nao casa `^[a-z][a-z0-9_]*$`:
- Reporte: "ERRO: argumento invalido. Informe nome do bounded context em snake_case (ex: conta)."
- Termine.

## Passo 1 -- Verificar que Entity existe

Derive o PascalCase: split por `_`, capitalize primeira letra de cada parte, concatene.
Exemplo: `meu_contexto` -> `MeuContexto`.

Verifique via Glob ou Read que o arquivo existe:
```
src/main/java/com/laboratorio/financas/$ARGUMENTS/infrastructure/persistence/<PascalCase>Entity.java
```

Se nao existir:
- Reporte: "ERRO: Entity nao encontrada. Execute /feature $ARGUMENTS antes de /migrate."
- Termine.

## Passo 2 -- Gerar migration SQL

Invoque a skill `/write-migration $ARGUMENTS`.

Aguarde o relatorio do subagent migration-writer. Se o relatorio indicar erro (Entity nao
encontrada, conflito de versao, ou qualquer falha no Write):
- Reporte: "ERRO no passo 1 (/write-migration): <mensagem de erro do subagent>"
- Termine. Nao invoque /write-test.

## Passo 3 -- Gerar unit tests do domain POJO

Derive o path do domain POJO:
```
src/main/java/com/laboratorio/financas/$ARGUMENTS/domain/<PascalCase>.java
```

Verifique se o arquivo existe. Se nao existir:
- Reporte: "AVISO: domain POJO nao encontrado em <path>. Pulando geracao de tests."
- Prossiga para o Passo 4 (relatorio final) sem invocar /write-test.

Se existir, invoque a skill `/write-test <path-derivado>`.

Aguarde o relatorio do subagent test-writer. Se o relatorio indicar falha de compilacao
ou testes falhando:
- Registre o status como FALHOU no relatorio final, mas NAO reverta a migration.

## Passo 4 -- Relatorio final

Produza:

```
/migrate concluido para: $ARGUMENTS

Passo 1 -- Migration SQL:   <OK | FALHOU: <motivo>>
Passo 2 -- Unit tests:      <OK | FALHOU: <motivo> | PULADO: <motivo>>

Arquivos gerados:
  <path da migration, se gerada>
  <path do arquivo de test, se gerado>

Pendencias manuais:
  - Adicionar FK constraints e indexes na migration conforme necessidade
  - Revisar e complementar os testes gerados

Proximos passos sugeridos:
  git add <arquivos gerados>
  git commit -m "feat(<contexto>): adiciona migration e tests para <nome>"
  /ship   -- apos commit
```
```

---

### Arquivo 2: `docs/progresso.md` (EDITAR)

Leia o arquivo antes de editar. Aplique as 4 mudancas abaixo via Edit.

**Mudanca 1 -- linha "Ultima atualizacao":**

Substitua o valor atual por:
```
**Última atualização:** 2026-05-12 (Sub-etapa 4.24 -- Skill /migrate orquestradora)
```

**Mudanca 2 -- marcar `/migrate` como concluido** (secao "Criterios de pronto"):

Substitua:
```
- [ ] Skill `/migrate` (encadeia migration-writer + test-writer)
```
Por:
```
- [x] Skill `/migrate` (encadeia migration-writer + test-writer) -- concluido 4.24
```

**Mudanca 3 -- adicionar 4.24 em "Sub-etapas concluidas"** (logo antes da entrada 4.23):

```
- **4.24 -- Skill `/migrate` (orquestradora de skills)** (2026-05-12): terceira categoria
  de skill do projeto. Encadeia `/write-migration` (subagent migration-writer) e `/write-test`
  (subagent test-writer) em sequencia para um bounded context. Para se migration falhar, nao
  propaga para testes. Relatorio combinado com status de cada passo. Sem logica propria --
  toda geracao delegada para os subagents ja existentes. Smoke parcial honesto: `/migrate conta`
  validou orquestramento e propagacao de erro (V2 ja existe; /write-test nao foi invocado).
  Happy path aguarda primeiro bounded context novo (Camada 4). PR #70.
```

**Mudanca 4 -- adicionar em "Historico de mudancas":**

```
- **2026-05-12** -- Sub-etapa 4.24 concluida: skill `/migrate` orquestradora em
  `.claude/skills/migrate/SKILL.md`. Terceira categoria de skill (orquestradora de skills).
  Encadeia /write-migration + /write-test. CLAUDE.md NAO atualizado. PR #70.
```

---

### Arquivo 3: `docs/decisoes-claude-code.md` (EDITAR)

Leia o arquivo antes de editar. Adicione subsecao antes do "Historico de mudancas"
(linha em branco antes e depois de cada `##`):

```
## Sub-etapa 4.24 -- Skill `/migrate` orquestradora

### Terceira categoria de skill

O projeto agora tem tres categorias de skill:

1. **Skill direta** (4.19-4.22): `disable-model-invocation: true`, sem `context: fork`.
   Claude Code executa instrucoes que usam ferramentas (Bash, Write, Grep) diretamente.
   Adequada para: sequencias procedurais de comandos shell.

2. **Skill-com-fork** (4.17-4.18, 4.23): `context: fork` + `agent: <nome>`.
   Claude Code forca novo contexto com subagent especializado.
   Adequada para: raciocinio de dominio (geracao de codigo, SQL).

3. **Skill orquestradora** (4.24): `disable-model-invocation: true`, sem fork proprio.
   Claude Code invoca outras skills em sequencia via Skill tool.
   Adequada para: composicao de workflows existentes sem logica propria.

### Decisao: stop-on-first-failure

`/migrate` para se a migration falhar -- nao invoca `/write-test` se SQL nao foi gerado.
Racional: unit test de domain POJO pode existir independentemente da migration (a migracao
e que precisa existir antes do commit com @Entity). Reversao da migration nao e prescrita
(operador decide).

### Decisao: sem logica propria

`/migrate` nao duplica logica dos subagents. Toda geracao e responsabilidade de
`migration-writer` e `test-writer`. `/migrate` e apenas um script de orquestracao.
```

Adicione ao "Historico de mudancas":

```
- **2026-05-12** -- Sub-etapa 4.24 concluida: skill `/migrate` orquestradora (terceira
  categoria). Taxonomia de skills do projeto consolidada em 3 categorias. PR #70.
```

---

## Estado esperado ao iniciar

```powershell
git branch --show-current   # deve retornar: main
git status                  # deve retornar: nothing to commit, working tree clean
git log --oneline -1        # deve mostrar squash do PR #69 (4.23) no topo
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
git checkout -b feat/etapa-4-24-migrate-skill
git branch --show-current   # deve retornar: feat/etapa-4-24-migrate-skill
```

### Tarefa 3 -- Criar `.claude/skills/migrate/SKILL.md`

Pre-condicao:
```powershell
Test-Path ".claude/skills/migrate/"   # deve retornar: False
```

```powershell
New-Item -ItemType Directory -Path ".claude/skills/migrate/"
```

Use Write para criar `.claude/skills/migrate/SKILL.md` com o conteudo prescrito.
Encoding UTF-8 sem BOM.

Pos-condicao:
```powershell
Test-Path ".claude/skills/migrate/SKILL.md"   # deve retornar: True
Select-String "disable-model-invocation: true" ".claude/skills/migrate/SKILL.md"   # deve ter match
Select-String "write-migration" ".claude/skills/migrate/SKILL.md"                  # deve ter match
Select-String "write-test" ".claude/skills/migrate/SKILL.md"                       # deve ter match
Select-String "context: fork" ".claude/skills/migrate/SKILL.md"                    # NAO deve ter match
```

### Tarefa 4 -- Primeiro commit

```powershell
git add ".claude/skills/migrate/SKILL.md"
git status
# deve mostrar exatamente 1 arquivo staged
```

Commit (scope `claude` sem ponto -- licao 4.19):
```
feat(claude): adiciona skill /migrate orquestradora de /write-migration e /write-test
```

### Tarefa 5 -- Smoke: `/migrate conta`

Execute a skill:
```
/migrate conta
```

O fluxo esperado:
1. Argumento `conta` validado como snake_case (OK)
2. PascalCase derivado: `Conta`
3. ContaEntity.java verificado (existe)
4. `/write-migration conta` invocado -> migration-writer detecta V2 existente -> reporta conflito
5. `/migrate` recebe erro do passo 1 e PARA. `/write-test` NAO e invocado.
6. Relatorio final exibe: Passo 1 FALHOU, Passo 2 PULADO/nao-executado

**Criterios de sucesso do smoke:**
- [ ] Argumento validado corretamente
- [ ] Entity verificada (ContaEntity existe)
- [ ] `/write-migration` invocado (passo delegado ao subagent)
- [ ] Erro de conflito propagado para o relatorio
- [ ] `/write-test` NAO invocado (stop-on-first-failure)
- [ ] Relatorio final exibido com status de cada passo

Se qualquer criterio falhar: reporte erro literal. Nao tente auto-corrigir.

NOTA: este e um smoke parcial honesto. O happy path completo (migration gerada + tests gerados)
so sera exercido na Camada 4 com o primeiro bounded context novo. Isso e esperado e aceito.

### Tarefa 6 -- Atualizar docs

Aplique as mudancas prescritas nos Arquivos 2 e 3 (`progresso.md`, `decisoes-claude-code.md`).
Leia cada arquivo antes de editar. Nao altere nenhum trecho alem dos prescritos.

Pos-condicao:
```powershell
Select-String "4.24" "docs/progresso.md" | Measure-Object | Select-Object -Expand Count
# deve retornar > 0

Select-String "4.24" "docs/decisoes-claude-code.md" | Measure-Object | Select-Object -Expand Count
# deve retornar > 0
```

### Tarefa 7 -- Segundo e terceiro commits

```powershell
git add "docs/progresso.md"
git status
```
Commit:
```
docs(progresso): registra sub-etapa 4.24 e skill /migrate como concluida
```

```powershell
git add "docs/decisoes-claude-code.md"
git status
```
Commit:
```
docs(decisoes): registra skill-orquestradora inaugurada na 4.24 e taxonomia de skills
```

### Tarefa 8 -- Validacao pre-ship

```powershell
git log --oneline feat/etapa-4-24-migrate-skill ^main
# deve mostrar exatamente 3 commits

git diff main --name-only
# deve mostrar exatamente:
#   .claude/skills/migrate/SKILL.md
#   docs/decisoes-claude-code.md
#   docs/progresso.md

git status
# deve retornar: nothing to commit, working tree clean

Select-String "disable-model-invocation: true" ".claude/skills/migrate/SKILL.md"
# deve ter match

Select-String "context: fork" ".claude/skills/migrate/SKILL.md"
# NAO deve ter match (orquestradora nao tem fork proprio)

Select-String "write-migration" ".claude/skills/migrate/SKILL.md"
# deve ter match

Select-String "write-test" ".claude/skills/migrate/SKILL.md"
# deve ter match
```

### Tarefa 9 -- Entregar via `/ship`

```
/ship
```

---

## Restricoes e freios

- NAO usar scope `.claude` em commits -- usar `claude` sem ponto (licao 4.19).
- NAO adicionar `context: fork` na skill -- orquestradora e skill direta.
- NAO duplicar logica de migration-writer ou test-writer na skill -- toda geracao
  e responsabilidade dos subagents existentes.
- NAO modificar o CLAUDE.md.
- NAO editar arquivos Java ou SQL existentes.
- Se hook bloquear commit: leia a mensagem, corrija sem `--no-verify`.
- Smoke falhou de forma inesperada? Reporte erro literal. Nao tente auto-corrigir.

---

## Estrutura de commits

```
feat(claude): adiciona skill /migrate orquestradora de /write-migration e /write-test
docs(progresso): registra sub-etapa 4.24 e skill /migrate como concluida
docs(decisoes): registra skill-orquestradora inaugurada na 4.24 e taxonomia de skills
```

---

## Estado esperado ao terminar

- PR #70 aberto (via `/ship`).
- Working tree limpa.
- `.claude/skills/migrate/SKILL.md` existente e versionado.
- `docs/progresso.md` e `docs/decisoes-claude-code.md` atualizados.
- Migrations existentes (V1-V4) intactas.
- Nenhum arquivo temporario de smoke restante.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO commitar o arquivo de prompt na branch (operador faz manualmente).
- NAO rodar `/ship` mais de uma vez.
- NAO reaproveitar o smoke de 4.23 aqui -- o smoke da 4.24 e especifico para
  orquestracao (verificar que /write-test NAO e chamado quando migration falha).
