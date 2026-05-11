# Prompt — Etapa 4.7.1: Registro de débito da regex do hook entity-migration + lição "smoke test deve usar input idiomático" (doc-only)

## Contexto

Camada 3 com 6 hooks funcionais + CLAUDE.md estrutural apos a Sub-etapa 4.7:

- 4.1 (PR #40): Conventional Commits.
- 4.2 (PR #41): Encoding UTF-8 + orquestrador 1:N.
- 4.2.1 (PR #42): ADR-011 padroes de validacao destrutiva (doc-only).
- 4.3 (PR #43): Markdown blank lines.
- 4.4 (PR #44): Tamanho de docs (warn).
- 4.5 (PR #45): Maven release (java-spring).
- 4.6 (PR #46): CLAUDE.md do projeto (curadoria).
- 4.7 (PR #47): `@Entity` sem migration Flyway (java-spring, modo conservador).

**Esta sub-etapa entrega registro doc-only** de duas lições descobertas no smoke test pos-merge da 4.7. **Sem mudanca de codigo** — analoga a 4.2.1 (que formalizou ADR-011 apos smoke test da 4.2).

Caracteristicas:

1. **Sub-etapa doc-only.** Sem hook novo, sem ajuste de regex (4.7.1 patch foi conscientemente preterido). Apenas registro.

2. **Duas licoes ao mesmo tempo:**
   - **Tecnica:** regex `(?m)^\s*@Entity\b` do hook entity-migration (4.7) e fragil para Java single-line. Caso edge real mas improvavel; mantido como debito.
   - **Operacional:** smoke test pos-merge falsificou falha do hook porque usou input sintetico (Java single-line). Hook funciona em producao real. Estabelecer regra: smoke test usa input idiomatico, nao sintetico.

3. **Sem validacao destrutiva tradicional.** Smoke test pos-merge tem formato analogo a 4.6 (sem cenarios git) — leitura de documentos atualizados confirma registro.

Quando esta etapa terminar:

- `docs/hooks-pendentes.md` tem entrada explicita de debito da regex do entity-migration.
- `docs/decisoes.md` tem subsecao formalizando "smoke test pos-merge usa input idiomatico".
- `docs/progresso.md` registra 4.7.1 com as 2 licoes.

## Padroes que estreiam nesta etapa

1. **Segunda sub-etapa doc-only do projeto** (apos 4.2.1). Padrao "sub-etapa de registro pos-smoke-test-falho" repetido — consolida o tipo.

2. **Smoke test pos-merge como gate funcional.** Quando smoke falha, **investiga primeiro**, decide depois. 4.7.1 e produto direto desse padrao — investigacao revelou que o problema era no roteiro do smoke, nao no hook; mas tambem revelou edge case real registrado.

3. **Debito tecnico conscientemente preterido** vs patch imediato. Decisao registrada de **nao** entrar em 4.7.1 porque caso edge e improvavel em producao. Padrao "registrar primeiro, corrigir se aparecer dor" formalizado.

## Escopo decidido (calibrado com operador antes da redacao)

### O que registrar

**Licao 1 — Tecnica (debito de regex):**

Hook `.claude/hooks/java-spring/entity-migration.ps1` (4.7) usa regex `(?m)^\s*@Entity\b` para detectar `@Entity` em arquivos `.java`. Regex exige `@Entity` no inicio de linha (com possivel whitespace de indentacao). Funciona perfeitamente em Java idiomatico: IDEs formatam, devs quebram linha apos `package` e `import`, anotacao fica em linha propria.

**Caso edge nao coberto:** Java single-line / minificado. Exemplo:

```java
package com.laboratorio.financas; import jakarta.persistence.Entity; @Entity public class Foo {}
```

Nesse caso, `@Entity` esta no meio da linha. Regex nao casa. Hook nao detecta. Commit passa sem migration.

**Decisao consciente:** nao corrigir nesta sub-etapa. Razao: caso edge artificial (devs nao escrevem assim, IDEs nao formatam assim). Mitigacao quando tocar no hook por outro motivo: trocar regex por `@Entity\b` (sem ancora `(?m)^\s*`). Word boundary `\b` ainda evita match em `@EntityListeners`, `@EntityGraph`, etc.

**Licao 2 — Operacional (smoke test idiomatico):**

Smoke test pos-merge da 4.7 (Cenario B) criou Java single-line com `@Entity` inline:

```powershell
$conteudoB = "package com.laboratorio.financas; import jakarta.persistence.Entity; @Entity public class Smoke47B {}"
```

Resultado: hook nao bloqueou (porque regex nao casou). Falso negativo. Diagnostico inicial chegou a hipotetizar bug no hook; foi descoberto que o **smoke test** estava errado.

**Regra formalizada:** smoke test pos-merge deve replicar **input idiomatico** — Java multi-linha com anotacao em linha propria, JSON formatado, etc. Input sintetico (single-line, minificado) pode produzir falso positivo ou falso negativo da regra. **Mitigacao para hooks de stack:** smoke test cria input que parece com codigo real escrito por humanos (com quebras de linha onde IDE colocaria).

### Arquivos modificados

```
docs/decisoes.md           ← edicao (subsecao 4.7.1 + regra de smoke test idiomatico)
docs/hooks-pendentes.md    ← edicao (entrada de debito da regex do entity-migration)
docs/progresso.md          ← edicao (licoes + sub-etapa + historico)
docs/prompt-etapa-4-7-1.md ← novo (este proprio prompt)
```

**Nao tocar:**

- `.claude/hooks/java-spring/entity-migration.ps1` (4.7). Regex permanece como esta. Ajuste fica como debito.
- Outros hooks, entrypoints, orquestrador.
- `.githooks/`.
- ADRs.
- `pom.xml`, scripts, `src/`, `frontend/`, migrations.
- `CLAUDE.md` raiz (esta sub-etapa nao muda stack/ambiente/convencoes/restricoes — regra 4.6 se aplica).
- `.gitignore`, `.gitattributes`.

### Atualizacao de `docs/decisoes.md`

Adicionar nova subsecao sob "Camada 3 — Configuracao do Claude Code", **apos** "@Entity sem migration Flyway (Sub-etapa 4.7)" e **antes** de "Claude Code hooks nativos":

```markdown
### Smoke test pos-merge usa input idiomatico (Sub-etapa 4.7.1)

**Regra:** smoke test pos-merge de hooks deve replicar **input idiomatico** — codigo formatado como humanos com IDE escreveriam, nao sintetico.

**Por que:** descoberto no smoke test pos-merge da 4.7. Cenario B usou Java single-line (`package x; import y.Entity; @Entity public class Foo {}`) que nao casou com a regex do hook entity-migration (`(?m)^\s*@Entity\b` exige `@Entity` em inicio de linha). Resultado: smoke aparentou falha do hook quando o problema era input sintetico.

**Diagnostico:** hook funciona em producao real (Java idiomatico tem `@Entity` em linha propria). Smoke test sintetico mascarou falso negativo. Investigacao identificou bug no smoke, nao no hook.

**Regra concreta para smoke tests futuros:**

- **Java:** anotacoes em linha propria; `package` em uma linha; `import` cada um em linha propria; classe em linha propria; campos em linhas separadas.
- **JSON:** formatado (indented), nao compactado.
- **YAML / XML / Markdown:** formato padrao com quebras de linha.

**Excecoes:** se o hook explicitamente trabalha com formato compactado (minifier, linter de minificacao), smoke usa o formato esperado. Hoje nao ha hooks assim no projeto.

**Categoria de licao consolidada:** "smoke test deve replicar input idiomatico, nao sintetico". Adicionada como regra geral em smoke tests pos-merge de hooks daqui pra frente.
```

Adicionar entrada no historico (final do arquivo):

```markdown
- **2026-MM-DD** — Sub-etapa 4.7.1 concluida (doc-only): registro de duas licoes do smoke test pos-merge da 4.7. (1) Debito tecnico: regex `(?m)^\s*@Entity\b` do hook entity-migration e fragil para Java single-line — mantida; ajuste para `@Entity\b` quando tocar no hook por outro motivo. (2) Regra operacional: smoke test pos-merge usa input idiomatico, nao sintetico. Categoria "sub-etapa doc-only de registro de licao apos smoke test falho" consolidada (analoga a 4.2.1). Mergeado via PR #XX.
```

### Atualizacao de `docs/hooks-pendentes.md`

**Operacao A** — Adicionar entrada em "Notas de cuidado para validacao destrutiva" (ou criar secao "Debitos conhecidos de hooks ativos" se nao existir):

```markdown
- **Hook entity-migration (4.7) tem regex fragil para Java single-line.** Regex atual `(?m)^\s*@Entity\b` em `.claude/hooks/java-spring/entity-migration.ps1` exige `@Entity` no inicio de linha (apos whitespace de indentacao). Nao detecta Java single-line com `@Entity` no meio da linha (ex: `package x; import y.Entity; @Entity public class Foo {}`). Caso edge improvavel em producao (IDEs formatam, devs quebram linha apos `package`/`import`). **Mitigacao quando tocar no hook por outro motivo:** trocar regex por `@Entity\b` (sem ancora de linha). Word boundary `\b` ainda evita match em `@EntityListeners`, `@EntityGraph`, etc. Descoberto no smoke test pos-merge da 4.7 (cenario B usou Java single-line sintetico).
```

**Operacao B** — Atualizar data:

```markdown
**Última atualização:** 2026-MM-DD (Sub-etapa 4.7.1 — registro de licoes da 4.7)
```

**Atencao:** confirmar nome exato da secao "Notas de cuidado para validacao destrutiva" antes de inserir. Se nao existir, criar "## Debitos conhecidos de hooks ativos" no final do arquivo, antes do "## Historico de mudancas" (se houver) ou no final absoluto.

### Atualizacao de `docs/progresso.md`

**A.** Atualizar "Ultima atualizacao": `2026-MM-DD (Sub-etapa 4.7.1 — Registro de licoes pos-smoke 4.7)`.

**B.** Em "Sub-etapas concluidas" (ordem cronologica, apos 4.7):

```markdown
- **4.7.1 — Registro de licoes pos-smoke da 4.7 (doc-only)** (2026-MM-DD): sub-etapa doc-only analoga a 4.2.1. Smoke test pos-merge da 4.7 (cenario B) usou Java single-line sintetico que nao casou com a regex `(?m)^\s*@Entity\b` do hook entity-migration. Falso negativo apareceu como falha do hook em producao; diagnostico identificou que o problema era no smoke test, nao no hook. **Duas licoes registradas:** (1) tecnica — regex do entity-migration e fragil para Java single-line, ajuste para `@Entity\b` fica como debito; (2) operacional — smoke test pos-merge usa input idiomatico, nao sintetico. Sem mudanca de codigo. PR #XX.
```

**C.** Adicionar secao "Licoes da Sub-etapa 4.7.1":

```markdown
## Licoes da Sub-etapa 4.7.1

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — doc-only.)

### Licoes de ambiente

1. **Regex de deteccao em `.ps1` pode falhar silenciosamente em input edge case.** Regex `(?m)^\s*@Entity\b` do hook entity-migration funciona em Java idiomatico (anotacao em linha propria) mas nao em Java single-line. Hook nao detecta `@Entity` no meio da linha. Mitigacao quando tocar no hook por outro motivo: `@Entity\b` (sem ancora de linha) cobre ambos os casos sem perder precisao (word boundary impede match em `@EntityListeners`).
2. **Smoke test pos-merge deve usar input idiomatico, nao sintetico.** Cenario B do smoke da 4.7 usou Java single-line (`package x; import y.Entity; @Entity public class Foo {}`) que nao exercitou o hook corretamente. Diagnostico inicial hipotetizou bug no hook; investigacao revelou que o problema era o input do smoke. Regra consolidada: smoke test cria codigo como humanos escreveriam (multi-linha, anotacao em linha propria, etc.). Excecao: hooks de minificacao usam input compactado por design.
3. **Padrao "sub-etapa doc-only de registro pos-smoke falho" consolidado.** Esta sub-etapa repete o tipo introduzido pela 4.2.1 (que formalizou ADR-011 apos smoke test da 4.2 falhar com `Environment.CurrentDirectory` vs `$PWD`). Categoria estabelecida: quando smoke test pos-merge falsifica resultado ou revela edge case real, sub-etapa doc-only registra causa raiz + decisao consciente (corrigir agora vs aceitar como debito).
4. **Decisao consciente "registrar primeiro, corrigir se aparecer dor" formalizada.** Patch tecnico (4.7.1) foi conscientemente preterido. Razao: caso edge artificial, custo de sub-etapa > valor de cobrir caso improvavel. Padrao operacional: registrar debito explicito em `hooks-pendentes.md`, atualizar quando tocar no hook por outro motivo, ou abrir sub-etapa dedicada se aparecer dor real.
```

**D.** Historico geral:

```markdown
- **2026-MM-DD** — Sub-etapa 4.7.1 concluida (doc-only): registro de licoes pos-smoke da 4.7. Debito tecnico da regex do entity-migration + regra "smoke test idiomatico, nao sintetico". Categoria "sub-etapa doc-only de registro pos-smoke falho" consolidada (analoga a 4.2.1). Mergeado via PR #XX.
```

### Versionar este proprio prompt

`docs/prompt-etapa-4-7-1.md`.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra `aa18318` (squash da 4.7) ou superior.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-7-1.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `.claude/hooks/java-spring/entity-migration.ps1` presente e **inalterado** desde 4.7.

Validar com:

```powershell
git status
git log --oneline -3
Test-Path docs\prompt-etapa-4-7-1.md
Test-Path .claude\hooks\java-spring\entity-migration.ps1
git config core.hooksPath
```

**Pre-condicoes ADR-011:**

- `Test-Path docs\prompt-etapa-4-7-1.md` retorna `True`.
- `Test-Path .claude\hooks\java-spring\entity-migration.ps1` retorna `True`.
- `git status` mostra apenas o prompt como untracked.

Se algum item divergir, **parar e reportar**.

## Tarefas

### Tarefa 1 — Validar pre-requisitos

Rodar comandos da secao "Estado esperado ao iniciar". Se divergir, parar e reportar.

### Tarefa 2 — Sincronizar Environment.CurrentDirectory (ADR-011)

```powershell
cd C:\projetos\financas-lab
[System.Environment]::CurrentDirectory = (Get-Location).Path
[System.Environment]::CurrentDirectory
```

Esperado: `C:\projetos\financas-lab`.

### Tarefa 3 — Criar branch

```bash
git checkout -b docs/etapa-4-7-1-licoes-pos-smoke
```

Prefixo `docs/` — sub-etapa de registro doc-only (analogo a 4.2.1 e 4.6).

### Tarefa 4 — Antes de editar, ler arquivos vivos

```bash
cat docs/decisoes.md
cat docs/hooks-pendentes.md
cat docs/progresso.md
```

**Confirmar:**

- `decisoes.md` tem subsecao "@Entity sem migration Flyway (Sub-etapa 4.7)" -> "Claude Code hooks nativos". A nova "Smoke test pos-merge usa input idiomatico (Sub-etapa 4.7.1)" entra **entre** essas duas.
- `hooks-pendentes.md` tem secao "Notas de cuidado para validacao destrutiva" OU outra similar. Se existir, novo item entra la. Se nao existir, criar "## Debitos conhecidos de hooks ativos" no final do arquivo (antes do "## Historico de mudancas" se houver, senao no final absoluto).
- `progresso.md` tem "Sub-etapas concluidas" em ordem ate 4.7.
- Entry da 4.7 em `progresso.md` (criada pela propria 4.7) **nao** precisa ser modificada — a 4.7.1 e registro novo, nao revisao retroativa.

Se alguma divergencia, **parar e reportar**.

### Tarefa 5 — Editar `docs/decisoes.md`

Adicionar subsecao "Smoke test pos-merge usa input idiomatico (Sub-etapa 4.7.1)" conforme escopo. Inserir entre subsecoes existentes.

Adicionar entrada no historico (final). Substituir `2026-MM-DD` pela data real e `PR #XX` por placeholder a ser substituido pos-PR.

### Tarefa 6 — Editar `docs/hooks-pendentes.md`

Operacoes A e B conforme escopo. **Atencao:** confirmar nome exato da secao alvo antes de inserir item (Tarefa 4 confirma).

### Tarefa 7 — Editar `docs/progresso.md`

Operacoes A, B, C, D conforme escopo. **Ordem cronologica:** 4.0 -> 4.0.1 -> 4.1 -> 4.2 -> 4.2.1 -> 4.3 -> 4.4 -> 4.5 -> 4.6 -> 4.7 -> 4.7.1.

### Tarefa 8 — Versionar este proprio prompt

`git add docs/prompt-etapa-4-7-1.md`.

### Tarefa 9 — Commit unico (sub-etapa doc-only)

Por ser doc-only e enxuta, **um unico commit** consolida tudo:

```bash
git add docs/decisoes.md docs/hooks-pendentes.md docs/progresso.md docs/prompt-etapa-4-7-1.md
git status   # 4 arquivos staged
git commit -m "docs: registra licoes pos-smoke 4.7 -- debito regex entity-migration + smoke idiomatico"
```

**Pre-condicao ADR-011:** 4 arquivos staged; `$LASTEXITCODE = 0`.

**Validacao implicita:** hooks ativos vao validar.

- Encoding UTF-8 (4.2) valida bytes.
- Markdown blank lines (4.3) valida headers nivel 2-6 nos `.md`.
- Tamanho de docs (4.4, warn) pode alertar se `progresso.md` cruzar 800 linhas — comportamento esperado, commit prossegue.
- Maven release (4.5) nao age (sem `pom.xml`).
- Entity-migration (4.7) nao age (sem `.java` novo).

Se algum hook bloquear (encoding ou blank lines), investigar antes de seguir.

### Tarefa 10 — Validacao final antes de push

```bash
git status
git log --oneline -3
git config core.hooksPath
```

Esperado:
- Working tree limpo.
- 1 commit novo da 4.7.1.
- `core.hooksPath` retorna `.githooks`.

## Restricoes e freios

1. **Sub-etapa doc-only.** Nao criar `.ps1`, `.java`. Apenas edicoes em `.md`.

2. **Nao tocar em `.claude/hooks/java-spring/entity-migration.ps1`.** Regex permanece. Ajuste fica como debito conscientemente aceito.

3. **Nao tocar em outros hooks, entrypoints, orquestrador, scripts, ADRs, `pom.xml`, `src/`, `frontend/`, migrations, `CLAUDE.md`, `.gitignore`, `.gitattributes`.**

4. **CLAUDE.md NAO atualizado** — esta sub-etapa nao muda stack/ambiente/convencoes/restricoes (regra 4.6 se aplica).

5. **Nao introduzir novas regras** alem das prescritas (debito da regex + smoke idiomatico).

6. **Tom conversacional direto** nas adicoes em `.md`, consistente com o resto dos docs.

7. **Sem acentos** nas adicoes (consistencia com o restante do projeto, embora `.md` aceite acentos tecnicamente).

8. **Apenas conteudo prescrito.** Se durante Tarefa 4 (ler arquivos vivos) descobrir algo divergente do esperado (nome de secao, texto exato), parar e reportar — analogo ao achado de "licao 2.5 vs 1.4" na 4.5 e ao "CLAUDE.md placeholder pre-existente" na 4.6.

9. **Ordem cronologica em `progresso.md`:** 4.0 -> 4.0.1 -> 4.1 -> 4.2 -> 4.2.1 -> 4.3 -> 4.4 -> 4.5 -> 4.6 -> 4.7 -> 4.7.1.

10. **Sem cenarios destrutivos tradicionais.** Sub-etapa doc-only, mesmo padrao da 4.2.1 e 4.6.

11. **Nao sugerir proxima sub-etapa** espontaneamente.

12. **Antes de escrever cada arquivo, ler arquivo vivo** (Tarefa 4).

13. **Nao tomar decisao silenciosa em zona limitrofe.** Divergencias na Tarefa 4 -> parar e reportar.

14. **Nao usar `pwsh`.** PowerShell 5.1.

15. **Nao usar `git reset --hard`** nesta sub-etapa. Nao ha cenarios destrutivos a limpar.

## Estrutura de commits

Branch: `docs/etapa-4-7-1-licoes-pos-smoke`

**Commit unico** — `docs: registra licoes pos-smoke 4.7 -- debito regex entity-migration + smoke idiomatico`
- `docs/decisoes.md`
- `docs/hooks-pendentes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-4-7-1.md`

## Validacao antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -3
git config core.hooksPath
```

Esperado:
- `check.ps1` passa.
- Working tree limpo.
- 1 commit novo da 4.7.1.

## PR

Titulo: `docs: sub-etapa 4.7.1 -- registro de licoes pos-smoke 4.7 (debito regex + smoke idiomatico)`

Body sugerido:

```markdown
## Summary

Sub-etapa **doc-only** que registra duas licoes descobertas no smoke test pos-merge da 4.7. **Sem mudanca de codigo.** Analoga a 4.2.1 (que formalizou ADR-011 apos smoke da 4.2).

### Contexto

Smoke test pos-merge da 4.7 (cenario B) criou Java single-line:

\```powershell
$conteudoB = "package com.laboratorio.financas; import jakarta.persistence.Entity; @Entity public class Smoke47B {}"
\```

Hook entity-migration NAO bloqueou o commit, embora o arquivo tivesse `@Entity` sem migration. Diagnostico inicial hipotetizou bug no hook em producao. Investigacao identificou que:

1. Regex `(?m)^\s*@Entity\b` do hook exige `@Entity` no **inicio de linha**.
2. Java single-line tem `@Entity` no meio da linha.
3. Regex nao casa. Hook nao detecta. Hook funciona em Java idiomatico (que e o caso real).

**Bug estava no smoke test, nao no hook.** Mas o edge case da regex e real.

### Duas licoes registradas

**1. Tecnica — debito da regex (conscientemente aceito):**

Regex frágil para Java single-line. **Nao corrigida nesta sub-etapa** porque caso edge e artificial (devs nao escrevem assim, IDEs nao formatam assim). Registrado em `docs/hooks-pendentes.md` com mitigacao: trocar para `@Entity\b` (sem ancora de linha) quando tocar no hook por outro motivo. Word boundary continua impedindo match em `@EntityListeners`, `@EntityGraph`.

**2. Operacional — smoke test idiomatico:**

Smoke test pos-merge deve replicar **input idiomatico** — codigo formatado como humanos com IDE escreveriam. Input sintetico (single-line, minificado) pode produzir falso negativo ou falso positivo. Formalizado em `docs/decisoes.md` como regra para smoke tests futuros.

### Por que NAO 4.7.1 (patch tecnico)

Foi considerado e conscientemente preterido. Razao:

- Caso edge artificial em producao.
- Custo de sub-etapa (1 prompt + agente + PR + CI + merge) > valor de cobrir caso improvavel.
- Padrao "registrar debito explicito, corrigir se aparecer dor" e coerente com decisao da 4.7 (que ja preteriu cobertura de `@Entity` em status M pela mesma razao).

Se algum dia algum dev commitar Java single-line e hook nao pegar, debito esta registrado com mitigacao pronta. Custo de aplicar e baixo no momento.

### Padrao "sub-etapa doc-only de registro pos-smoke falho" consolidado

Esta sub-etapa repete o tipo introduzido pela 4.2.1. Categoria estabelecida:

> Quando smoke test pos-merge falsifica resultado ou revela edge case real, abrir sub-etapa doc-only que registra causa raiz + decisao consciente (corrigir agora vs aceitar como debito).

Aplicacoes anteriores:
- **4.2.1:** smoke test pos-4.2 revelou `[System.IO.File]::WriteAllText` gravando em `Environment.CurrentDirectory` vs `$PWD`. Resultou em ADR-011 (padroes de validacao destrutiva).
- **4.7.1 (esta sub-etapa):** smoke test pos-4.7 revelou regex fragil da entity-migration + bug no proprio roteiro do smoke.

### Mudancas

- `docs/decisoes.md`: subsecao "Smoke test pos-merge usa input idiomatico (Sub-etapa 4.7.1)". Entrada no historico.
- `docs/hooks-pendentes.md`: entrada de debito da regex do entity-migration em "Notas de cuidado" (ou secao dedicada se criada). Data atualizada.
- `docs/progresso.md`: sub-etapa 4.7.1 em "Sub-etapas concluidas". Licoes da 4.7.1 (4 itens). Entrada no historico.
- `docs/prompt-etapa-4-7-1.md`: prompt versionado.

### CLAUDE.md NAO atualizado

Sub-etapa nao muda stack/ambiente/convencoes/restricoes — apenas registra licoes e debito. Regra de manutencao da 4.6 se aplica: CLAUDE.md nao e tocado.

### Sem validacao destrutiva tradicional

Sub-etapa doc-only. Smoke test pos-merge tem formato analogo a 4.6 e 4.2.1: leitura de documentos atualizados confirma registro.

### Proximo passo

Decisao fora deste PR. Possiveis caminhos:
- Mais hooks de stack: sufixo `Test`/`IT`, Lombok/MapStruct, Maven Central.
- Subagents (`pr-reviewer`, `architect-reviewer`).
- Skills (`/ship`, `/feature`).
- Claude Code hooks nativos.
- Iniciar Camada 4 (desenvolvimento de dominio).

Calibracao em sessao separada.
```

## Pos-criacao do PR

1. Abrir PR via `gh pr create`.
2. Capturar o numero.
3. Editar `docs/decisoes.md`, `docs/hooks-pendentes.md`, `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>` e `2026-MM-DD` pela data real.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorizacao explicita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `docs/etapa-4-7-1-licoes-pos-smoke` empurrada com 2 commits (1 + 1 update do PR).
- PR aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.7.
- Working tree limpo.
- `entity-migration.ps1` **inalterado** desde 4.7 (regex `(?m)^\s*@Entity\b` permanece).
- Reportar com `git log --oneline -3`, `git status`, `gh pr view --json number,state,statusCheckRollup`.

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao criar prompt da proxima sub-etapa.
- Nao criar hooks, ajustar regex do entity-migration, criar subagents, skills, scripts.
- Nao tocar em codigo, `CLAUDE.md`, ADRs, `.gitignore`, `.gitattributes`.
- Nao deixar `test-*.*` (nao houve cenarios destrutivos — nada a limpar).
- Nao sugerir proximo passo espontaneamente.
- Nao usar `pwsh`.
- Nao usar `git reset --hard`.
- Nao corrigir a regex agora. Debito explicito registrado e suficiente.
