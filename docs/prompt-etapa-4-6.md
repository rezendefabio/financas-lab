# Prompt — Etapa 4.6: CLAUDE.md do projeto (curadoria de contexto inicial)

## Contexto

Camada 3 com 5 hooks funcionais em producao apos a Sub-etapa 4.5:

- 4.1 (PR #40): Conventional Commits via `commit-msg`.
- 4.2 (PR #41): Encoding UTF-8 via `pre-commit`, orquestrador 1:N.
- 4.3 (PR #43): Markdown blank lines via `pre-commit`.
- 4.4 (PR #44): Tamanho de docs (modo warn) via `pre-commit`.
- 4.5 (PR #45): Maven release explicito via `pre-commit`. Primeiro hook de stack.

ADR-011 (PR #42) formaliza padroes de validacao destrutiva.

**Esta sub-etapa entrega o CLAUDE.md do projeto** — arquivo na raiz que o Claude Code le automaticamente em toda sessao iniciada nele. Vira contexto pre-carregado: agente nasce sabendo o essencial sem precisar abrir docs.

Caracteristicas novas desta sub-etapa:

1. **Primeira sub-etapa de curadoria, nao de codigo.** Diferente das 4.1-4.5 (que adicionavam hooks executaveis), esta produz **documento estrutural**. Validacao destrutiva nao tem cenarios obvios — substituida por smoke test pos-merge especifico (sessao nova do Claude Code respondendo perguntas).

2. **Estabelece padrao "CLAUDE.md editado apenas quando muda algo estrutural".** Conteudo volatil (estado atual, lista de hooks, lista de docs proliferantes) **fica fora**, com link para `docs/`. Decisao consciente para evitar atualizacao a cada sub-etapa.

3. **Primeiro arquivo na raiz do repo** criado pelo projeto (alem de `pom.xml`, `docker-compose.yml`, scripts pre-existentes). Sinal visivel da Camada 3 a quem clona o repo.

Quando esta etapa terminar:

- `CLAUDE.md` na raiz com ~100 linhas, ~5KB.
- Agente em sessao nova carrega contexto sem precisar abrir `docs/`.
- Padrao "atualizar CLAUDE.md so quando muda algo estrutural" registrado em `docs/decisoes.md`.

## Padroes que estreiam nesta etapa

1. **Primeira sub-etapa de curadoria, nao de codigo.** Saida e documento, nao executavel.
2. **Smoke test pos-merge via sessao nova do Claude Code.** Nao usa `git commit` em cenarios. Valida que contexto carrega corretamente.
3. **Regra "conteudo volatil fica fora do CLAUDE.md"** formalizada.

## Escopo decidido (calibrado com operador antes da redacao)

### Alvo de tamanho

- **Recomendado:** ate 200 linhas. ~6-8KB.
- **Limite duro:** 250 linhas. Se chegar la, algo deveria virar link para `docs/`.

Razao: CLAUDE.md entra em **toda mensagem** da sessao (nao so primeira). Tokens acumulam em sessoes longas. Documento curto e denso > documento longo e completo.

### Estrutura em 7 secoes

Sequencia importa (agente le de cima pra baixo):

1. **Identidade do projeto** (5 linhas).
2. **Stack** (12 linhas).
3. **Ambiente operacional** (15 linhas).
4. **Hooks ativos** (mecanismo + filosofia, ~15 linhas).
5. **Convencoes e padroes** (18 linhas).
6. **Onde buscar mais** (10 linhas).
7. **O que NAO fazer** (12 linhas).

Total estimado: ~95-100 linhas. Folga sobre 200.

### Tom

Conversacional direto. Frases curtas + bullets. Sem prosa formal.

### Pronome

Descritivo neutro. Sem "voce". Documento, nao conversa.

### Atualizacao

CLAUDE.md e editado **dentro da sub-etapa** que muda algo estrutural — nao em sub-etapa propria de "atualizacao". Estrutural = stack, ambiente, convencoes, restricoes.

**Conteudo volatil NAO entra:**

- Estado atual do projeto (Camada/Sub-etapa). Vai para `docs/progresso.md` via link.
- Lista de hooks ativos com regras. Vai para `docs/hooks-pendentes.md` via link.
- Lista de arquivos `docs/prompt-etapa-*.md`. Proliferam, nao listados.

CLAUDE.md menciona o **mecanismo de hooks** e **modos `warn`/`fail`** porque sao estruturais. Lista especifica do que esta ativo fica fora.

### Conteudo das 7 secoes

#### Secao 1 — Identidade

```markdown
# financas-lab

Laboratorio AI-native para construcao de fabrica de software replicavel.
Domain: gestao financeira pessoal (proxy de estudo, nao produto final).
Operador unico: Fabio Rezende. Sessoes via Claude Code em Windows nativo.

Estado atual e camadas: `docs/progresso.md` (secao "Status geral por Camada").
```

#### Secao 2 — Stack

```markdown
## Stack

- Java 21 + Spring Boot 3.x.
- PostgreSQL em Docker (container `financas-lab-postgres`).
- Migrations: Flyway (`src/main/resources/db/migration/V*.sql`).
- Persistencia: JPA + Hibernate.
- Mapeamento DTO: MapStruct + Lombok (ordem importa em `pom.xml`).
- Build: Maven (`pom.xml` na raiz, modulo unico). `<release>${java.version}</release>` no maven-compiler-plugin.
- Frontend: ainda nao implementado (planejado para Camada 5).
- Sem Gradle. Sem Kotlin. Sem React ainda.
```

#### Secao 3 — Ambiente operacional

```markdown
## Ambiente

- SO: Windows nativo (nao WSL, nao Linux). Paths usam backslash em alguns contextos.
- Shell: PowerShell 5.1 (nativo). **NAO usar `pwsh`** — PowerShell Core 7 nao esta disponivel.
- Git Bash disponivel (vem com Git for Windows).
- Docker Desktop ativo. Compose: `docker compose up -d`.

### Comandos do dia

- Setup inicial: `.\scripts\setup.ps1` (idempotente).
- Dev (start backend + deps): `.\scripts\dev.ps1`.
- Validacao completa: `.\scripts\check.ps1` (mvn verify + verifica encoding).
- Subir banco isolado: `docker compose up -d postgres`.
```

#### Secao 4 — Hooks ativos

```markdown
## Hooks ativos

Mecanismo: `core.hooksPath=.githooks`. Cada entrypoint em `.githooks/` (`commit-msg`, `pre-commit`) invoca companheiro `.ps1` que delega para hook real em `.claude/hooks/{escopo}/`. Detalhes em `.githooks/README.md`.

Modos:

- **`fail`** para regras objetivas — bloqueia commit em violacao.
- **`warn`** para regras subjetivas — alerta no terminal sem bloquear.

Decisao de modo registrada em `docs/decisoes.md` quando o hook nasce.

Override: `git commit --no-verify` bypassa hooks. Uso documentado no PR body.

Lista completa de hooks ativos e suas regras: `docs/hooks-pendentes.md` (secao "Hooks implementados").
```

#### Secao 5 — Convencoes e padroes

```markdown
## Convencoes e padroes

### Branches

- `feat/etapa-X-Y-descricao` para sub-etapas que adicionam funcionalidade.
- `fix/...` para correcoes.
- `docs/...` para sub-etapas doc-only.

### Commits

Conventional Commits obrigatorio (hook ativo). Mensagens em portugues, sem acentos no codigo, ASCII apenas em strings de scripts `.ps1`.

### Sub-etapas

Trabalho organizado em sub-etapas pequenas dentro de Camadas. Cada sub-etapa: 1 branch, 3-4 commits, 1 PR, validacao destrutiva, smoke test pos-merge. Calibracao via D1-D5 antes do prompt. Padrao detalhado em `docs/progresso.md`.

### Validacao destrutiva (ADR-011)

Toda nova regra/hook exige validacao destrutiva com cenarios explicitos. Pre-condicoes obrigatorias: `Test-Path` apos criar arquivo, `git status` antes de `git commit`, verificacao de `$LASTEXITCODE`, sincronizacao de `[System.Environment]::CurrentDirectory = (Get-Location).Path` antes de `[System.IO.File]::WriteAllText` com path relativo.

### Decisao silenciosa em zona limitrofe

Padrao central vigiado. Em divergencia entre prescricao e ambiente real, parar e reportar, nunca adivinhar.
```

#### Secao 6 — Onde buscar mais

```markdown
## Onde buscar mais

Documentos de referencia em `docs/`:

- `progresso.md` — onde estamos. Tracking de Camadas e sub-etapas. Licoes meta-operacionais.
- `decisoes.md` — escolhas tomadas. Por que cada regra existe.
- `adrs.md` — decisoes arquiteturais formais.
- `hooks-pendentes.md` — backlog de hooks + hooks implementados (lista completa).
- `visao.md` — direcao do projeto e Camadas planejadas.

Prompts versionados de cada sub-etapa ficam em `docs/prompt-etapa-X-Y.md`. Nao listados individualmente; agente busca quando precisa.
```

#### Secao 7 — O que NAO fazer

```markdown
## O que NAO fazer

- Usar `pwsh` em scripts. PowerShell 5.1 (`powershell`) e o unico disponivel.
- Caracteres nao-ASCII em strings de hooks `.ps1`. Em-dash U+2014 quebra parse (licao 4.4).
- `git commit --no-verify` sem documentar no PR body.
- `git reset --hard` em main, ou sem confirmar `git branch --show-current` primeiro.
- Tocar em `pom.xml` removendo `<release>` (hook bloqueia, e por bom motivo — licao 1.4).
- Criar arquivos `.md` em `docs/` sem linhas em branco antes/depois de headers (hook bloqueia).
- Validacao destrutiva sem pre-condicoes ADR-011 — produz falsos positivos silenciosos.
- Assumir contexto sem ler arquivos vivos antes de editar (lista de "ler arquivos vivos" nos prompts).
- Atualizar CLAUDE.md fora de uma sub-etapa que muda hook/padrao/stack. Sincronizacao e parte do escopo da sub-etapa causadora.
```

### Arquivos criados e modificados

```
CLAUDE.md                           ← novo (raiz do repo)
docs/decisoes.md                    ← edicao (subsecao da 4.6 + regra de atualizacao)
docs/progresso.md                   ← edicao (licoes + sub-etapa + historico)
docs/prompt-etapa-4-6.md            ← novo (este proprio prompt)
```

**Nao tocar:**

- Hooks existentes (`.claude/hooks/universal/*`, `.claude/hooks/java-spring/*`).
- Entrypoints (`.githooks/*`).
- `.githooks/README.md`.
- `docs/hooks-pendentes.md` — esta sub-etapa nao adiciona hook nem candidato.
- ADRs.
- `pom.xml`, scripts (`setup.ps1`, `dev.ps1`, `check.ps1`).
- `src/`, `frontend/`, `docker-compose.yml`, migrations.
- `.gitignore`, `.gitattributes`.

### Atualizacao de `docs/decisoes.md`

Adicionar nova subsecao sob "Camada 3 — Configuracao do Claude Code", **apos** "Maven release explicito (Sub-etapa 4.5)" e **antes** de "Claude Code hooks nativos":

```markdown
### CLAUDE.md do projeto (Sub-etapa 4.6)

`CLAUDE.md` na raiz do repo carrega contexto inicial em toda sessao do Claude Code automaticamente.

**Conteudo:** identidade do projeto, stack, ambiente operacional, mecanismo de hooks (modos `fail`/`warn`), convencoes e padroes, onde buscar mais em `docs/`, lista do que nao fazer.

**Conteudo volatil NAO entra:**

- Estado atual (Camada/Sub-etapa) — link para `docs/progresso.md`.
- Lista de hooks ativos com regras — link para `docs/hooks-pendentes.md` (secao "Hooks implementados").
- Lista de arquivos `docs/prompt-etapa-*.md` — proliferam, agente busca quando precisa.

CLAUDE.md menciona o **mecanismo de hooks** e os **modos `warn`/`fail`** porque sao estruturais. Lista especifica do que esta ativo fica fora.

**Alvo de tamanho:** ate 200 linhas. ~6-8KB. Limite duro: 250 linhas. Razao: CLAUDE.md entra em toda mensagem da sessao, nao so na primeira. Documento curto e denso > documento longo e completo.

**Regra de atualizacao:**

CLAUDE.md e editado **dentro da sub-etapa** que muda algo estrutural — nao em sub-etapa propria de "atualizacao". Estrutural = stack, ambiente, convencoes, restricoes.

Sub-etapas que apenas adicionam hook **nao editam CLAUDE.md**. Hook entra na lista de `docs/hooks-pendentes.md` (que ja e linkado). Sub-etapas que avancam Camada **nao editam CLAUDE.md**. Estado vive em `docs/progresso.md` (que ja e linkado).

Esta regra entra nas Restricoes/freios dos prompts futuros: "verificar se a sub-etapa muda stack/ambiente/convencoes/restricoes. Se sim, atualizar CLAUDE.md no escopo da sub-etapa. Se nao, nao tocar em CLAUDE.md".
```

Adicionar entrada no historico (final do arquivo):

```markdown
- **2026-MM-DD** — Sub-etapa 4.6 concluida: CLAUDE.md do projeto criado na raiz. ~100 linhas, 7 secoes, conteudo volatil delegado para `docs/` via links. Regra de atualizacao formalizada: editado apenas em sub-etapas que mudam stack/ambiente/convencoes/restricoes. Primeira sub-etapa de curadoria (nao de codigo). Mergeado via PR #XX.
```

### Atualizacao de `docs/progresso.md`

**A.** Atualizar "Ultima atualizacao": `2026-MM-DD (Sub-etapa 4.6 — CLAUDE.md do projeto)`.

**B.** Em "Sub-etapas concluidas" (ordem cronologica, apos 4.5):

```markdown
- **4.6 — CLAUDE.md do projeto** (2026-MM-DD): primeira sub-etapa de curadoria (nao codigo). Cria `CLAUDE.md` na raiz com 7 secoes (identidade, stack, ambiente, mecanismo de hooks, convencoes, onde buscar mais, o que nao fazer). ~100 linhas, ~5KB. Conteudo volatil delegado para `docs/` via links — CLAUDE.md so atualizado em sub-etapas que mudam stack/ambiente/convencoes/restricoes. Validacao via smoke test pos-merge em sessao nova do Claude Code. PR #XX.
```

**C.** Adicionar secao "Licoes da Sub-etapa 4.6":

```markdown
## Licoes da Sub-etapa 4.6

### Candidatos a hook (automatizar em etapas futuras)

(A preencher se houver durante execucao.)

### Licoes de ambiente

(A preencher se houver durante execucao. Esperado pelo menos: padrao "conteudo volatil fica fora do CLAUDE.md" formalizado — sub-etapas que apenas adicionam hook nao editam CLAUDE.md. Sub-etapas que avancam Camada nao editam CLAUDE.md.)
```

**D.** Historico geral:

```markdown
- **2026-MM-DD** — Sub-etapa 4.6 concluida: CLAUDE.md do projeto criado. Primeira sub-etapa de curadoria. Padrao de atualizacao formalizado. Mergeado via PR #XX.
```

### Versionar este proprio prompt

`docs/prompt-etapa-4-6.md`.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra `bf35736` (squash da 4.5) ou superior.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-6.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- **`CLAUDE.md` NAO existe** na raiz do repo (sera criado nesta sub-etapa).

Validar com:

```powershell
git status
git log --oneline -3
Test-Path docs\prompt-etapa-4-6.md
Test-Path CLAUDE.md
git config core.hooksPath
```

**Pre-condicoes ADR-011:**

- `Test-Path docs\prompt-etapa-4-6.md` retorna `True`.
- `Test-Path CLAUDE.md` retorna `False`.
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
git checkout -b docs/etapa-4-6-claude-md
```

Prefixo `docs/` — sub-etapa de curadoria, nao codigo (analogo a 4.2.1).

### Tarefa 4 — Antes de editar, ler arquivos vivos

```bash
cat docs/decisoes.md
cat docs/progresso.md
cat docs/hooks-pendentes.md
ls scripts/
```

**Confirmar:**

- `decisoes.md` tem subsecao "Maven release explicito (Sub-etapa 4.5)" → "Claude Code hooks nativos". A nova "CLAUDE.md do projeto (Sub-etapa 4.6)" entra **entre** essas duas.
- `progresso.md` tem "Sub-etapas concluidas" em ordem ate 4.5.
- `hooks-pendentes.md` tem secao "Hooks implementados" com 5 itens (Conventional Commits, Encoding UTF-8, Blank lines em Markdown, Maven release explicito, Tamanho de docs em warn).
- `scripts/` contem `setup.ps1`, `dev.ps1`, `check.ps1`. Se algum nome divergir do que CLAUDE.md vai citar, ajustar o conteudo do CLAUDE.md.

Se alguma divergencia, **parar e reportar**.

### Tarefa 5 — Criar `CLAUDE.md` na raiz

Conteudo conforme as 7 secoes do "Escopo decidido". **UTF-8 sem BOM.** Acentos sao permitidos em `.md` (hook de encoding so rejeita BOM, nao acentos). Mas as 7 secoes propostas estao sem acentos por consistencia com o resto do projeto.

**Pre-condicao ADR-011:** apos criar, validar:

```powershell
Test-Path CLAUDE.md          # True
$bytes = [System.IO.File]::ReadAllBytes("CLAUDE.md")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191 (BOM)

$linhas = [System.IO.File]::ReadAllLines("CLAUDE.md").Count
Write-Host "Total de linhas: $linhas (esperado: ~95-110, limite duro 250)"
```

Se BOM presente, corrigir. Se linhas >150, revisar conteudo para garantir que nada volatil entrou.

### Tarefa 6 — Commit 1 (CLAUDE.md)

```bash
git add CLAUDE.md
git status   # apenas CLAUDE.md staged
git commit -m "docs: cria CLAUDE.md do projeto na raiz do repo"
```

**Pre-condicao ADR-011:** `$LASTEXITCODE = 0`. Hook de encoding UTF-8 e Markdown blank lines vao validar o `CLAUDE.md`.

**Importante:** Markdown blank lines hook valida headers nivel 2-6. Se algum header `##`/`###` em CLAUDE.md nao tiver linha em branco antes/depois, hook rejeita o proprio commit. Conferir mentalmente o conteudo prescrito — todos os headers tem `\n` antes e depois. Se hook bloquear, investigar.

### Tarefa 7 — Editar `docs/decisoes.md`

Adicionar subsecao "CLAUDE.md do projeto (Sub-etapa 4.6)" conforme escopo. Inserir entre as subsecoes existentes.

Adicionar entrada no historico (final). Substituir `2026-MM-DD` pela data real.

### Tarefa 8 — Editar `docs/progresso.md`

Operacoes A, B, C, D conforme escopo. **Atencao a ordem cronologica:** 4.0 → 4.0.1 → 4.1 → 4.2 → 4.2.1 → 4.3 → 4.4 → 4.5 → 4.6.

### Tarefa 9 — Versionar este proprio prompt

`git add docs/prompt-etapa-4-6.md`.

### Tarefa 10 — Commit 2 (docs)

```bash
git add docs/decisoes.md docs/progresso.md docs/prompt-etapa-4-6.md
git status   # 3 arquivos staged
git commit -m "docs: registra sub-etapa 4.6 CLAUDE.md em decisoes progresso e versiona prompt"
```

**Pre-condicao ADR-011:** 3 arquivos staged; `$LASTEXITCODE = 0`.

**Validacao implicita:** hooks de encoding, blank lines, tamanho de docs e Maven release todos ativos. Como nenhum dos 3 arquivos e `pom.xml`, hook de Maven nao age. Hooks de encoding e blank lines validam os `.md`. Hook de tamanho pode alertar se `progresso.md` cruzar 800 linhas com as adicoes da 4.6 — se aparecer alerta, e funcionamento esperado (modo warn), commit prossegue.

### Tarefa 11 — Validacao final antes de push

```bash
git status
git log --oneline -3
git config core.hooksPath
Test-Path CLAUDE.md
```

Esperado:
- Working tree limpo.
- 2 commits novos da 4.6.
- `core.hooksPath` retorna `.githooks`.
- `CLAUDE.md` existe.

**`check.ps1` opcional** — sub-etapa nao toca em codigo Java, mas confirma suite intocada.

## Restricoes e freios

1. **Sub-etapa de curadoria, nao codigo.** Nao criar `.ps1`, `.java`, `.bash`. Apenas `.md`.

2. **Nao criar hooks novos.** Nao tocar em `.claude/hooks/`, `.githooks/`.

3. **Nao tocar em entrypoints**, scripts, `pom.xml`, `src/`, `frontend/`, migrations.

4. **Nao tocar em ADRs, `hooks-pendentes.md`, `.gitignore`, `.gitattributes`.**

5. **CLAUDE.md em UTF-8 sem BOM.** Hook de encoding (4.2) valida.

6. **CLAUDE.md com linhas em branco antes/depois de headers nivel 2-6.** Hook de Markdown blank lines (4.3) valida.

7. **Apenas conteudo prescrito.** Nao adicionar secoes/itens que nao estao no escopo. Se durante a Tarefa 4 (ler arquivos vivos) descobrir que algo no escopo esta errado (ex: nome de script diferente), parar e reportar antes de ajustar.

8. **Conteudo volatil NAO entra no CLAUDE.md:** estado atual de Camada/Sub-etapa, lista de hooks especificos com regras, lista de arquivos prompt. Mantem como link/referencia para `docs/`.

9. **Tom conversacional direto.** Frases curtas, bullets. Sem prosa formal.

10. **Pronome neutro.** Sem "voce".

11. **Acentos permitidos em CLAUDE.md** (e em outros `.md` deste commit). Hook de encoding so rejeita BOM, nao acentos. Conteudo prescrito esta sem acentos por consistencia com o restante do projeto — manter.

12. **Tamanho alvo: ate 200 linhas.** Limite duro 250.

13. **Nao tomar decisao silenciosa em zona limitrofe.** Se conteudo da Tarefa 4 divergir do esperado (ex: nome de script, lista de hooks), parar e reportar.

14. **Nao sugerir proxima sub-etapa** espontaneamente.

15. **Antes de escrever cada arquivo, ler arquivo vivo** (Tarefa 4).

16. **Ordem cronologica em `progresso.md`:** 4.0 → 4.0.1 → 4.1 → 4.2 → 4.2.1 → 4.3 → 4.4 → 4.5 → 4.6.

17. **Nao usar `git reset --hard`** nesta sub-etapa. Sem validacao destrutiva com cenarios git — smoke test pos-merge tem formato diferente (sessao nova do Claude Code).

18. **Sem cenarios destrutivos tradicionais.** Sub-etapa de curadoria nao tem "comportamento a testar via git commit". Validacao acontece pos-merge via Smoke test descrito na secao "PR".

## Estrutura de commits

Branch: `docs/etapa-4-6-claude-md`

**Commit 1** — `docs: cria CLAUDE.md do projeto na raiz do repo`
- `CLAUDE.md` (novo)

**Commit 2** — `docs: registra sub-etapa 4.6 CLAUDE.md em decisoes progresso e versiona prompt`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-4-6.md`

## Validacao antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -3
Test-Path CLAUDE.md
```

Esperado:
- `check.ps1` passa.
- Working tree limpo.
- 2 commits novos.
- `CLAUDE.md` existe.

## PR

Titulo: `docs: sub-etapa 4.6 — CLAUDE.md do projeto (primeira sub-etapa de curadoria)`

Body sugerido:

```markdown
## Summary

Cria `CLAUDE.md` na raiz do repo. **Primeira sub-etapa de curadoria** (nao codigo). Estabelece padrao "conteudo volatil fica fora — CLAUDE.md editado apenas em sub-etapas que mudam stack/ambiente/convencoes/restricoes".

### O que entra no CLAUDE.md

Sete secoes:

1. **Identidade do projeto** — o que e `financas-lab`, operador, link para `docs/progresso.md` (estado atual).
2. **Stack** — Java 21, Spring Boot, PostgreSQL, Flyway, JPA, MapStruct, Lombok, Maven. Versoes onde importa.
3. **Ambiente operacional** — Windows nativo, PowerShell 5.1 (nao pwsh), Docker. Comandos do dia (`setup.ps1`, `dev.ps1`, `check.ps1`).
4. **Hooks ativos** — mecanismo + modos `fail`/`warn`. **Lista especifica fica em `docs/hooks-pendentes.md`** (conteudo volatil).
5. **Convencoes e padroes** — branches, commits, sub-etapas, validacao destrutiva (ADR-011), decisao silenciosa em zona limitrofe.
6. **Onde buscar mais** — links para `progresso.md`, `decisoes.md`, `adrs.md`, `hooks-pendentes.md`, `visao.md`. Sem `prompt-etapa-*.md` (proliferam).
7. **O que NAO fazer** — lista curta dos nao-negociaveis (pwsh, em-dash em `.ps1`, override sem documentar, etc).

### Por que conteudo volatil fica fora

Decisao de design consciente:

- **Estado atual de Camada/Sub-etapa** muda toda sub-etapa. Fica em `docs/progresso.md` (linkado).
- **Lista de hooks ativos** muda toda sub-etapa que adiciona hook. Fica em `docs/hooks-pendentes.md` (linkado).
- **Lista de prompts versionados** prolifera. Agente busca em `docs/prompt-etapa-X-Y.md` quando precisa.

Razao: CLAUDE.md entra em **toda mensagem** da sessao (nao so primeira). Token budget importa em sessoes longas. Atualizar CLAUDE.md toda sub-etapa seria custo de manutencao injustificado.

### Regra de atualizacao formalizada

CLAUDE.md e editado **dentro da sub-etapa** que muda algo estrutural — nao em sub-etapa propria de "atualizacao". Estrutural = stack, ambiente, convencoes, restricoes. Sub-etapas que apenas adicionam hook **nao editam CLAUDE.md**.

Esta regra entra nas Restricoes/freios dos prompts futuros.

### Tamanho

Alvo ~100 linhas, ~5KB. Limite duro 250 linhas. Recomendacao Anthropic e da pratica comum (200 linhas) respeitada.

### Sem validacao destrutiva tradicional

Sub-etapa de curadoria nao tem cenarios `git commit` para testar. Smoke test pos-merge tem formato diferente — sessao nova do Claude Code respondendo perguntas para validar que contexto carrega corretamente. Ver secao "Validacao pos-merge" abaixo.

### Mudancas

- `CLAUDE.md` (novo, raiz): ~100 linhas, 7 secoes conforme descricao.
- `docs/decisoes.md`: subsecao "CLAUDE.md do projeto (Sub-etapa 4.6)" formalizando regra de atualizacao. Entrada no historico.
- `docs/progresso.md`: sub-etapa 4.6 em "Sub-etapas concluidas". Licoes da 4.6. Entrada no historico.
- `docs/prompt-etapa-4-6.md`: prompt versionado.

### Validacao pos-merge sugerida

Em sessao **nova** do Claude Code (apos `/clear` ou abrindo nova janela), fazer 3 perguntas que CLAUDE.md responderia. Validar que agente responde sem precisar abrir arquivos:

1. **"Qual a versao do Java do projeto?"** Esperado: agente responde "Java 21" sem consultar `pom.xml`.
2. **"Qual shell devo usar para scripts?"** Esperado: agente responde "PowerShell 5.1, nao usar `pwsh`".
3. **"Onde encontro a lista de hooks ativos?"** Esperado: agente aponta `docs/hooks-pendentes.md` (secao "Hooks implementados").

Se 3 respostas baterem, CLAUDE.md esta carregando contexto corretamente.

### Proximo passo

Decisao fora deste PR. Possiveis caminhos:
- Mais hooks de stack (java-spring): `@Entity` sem migration, sufixo Test, Lombok/MapStruct.
- Primeiros subagents (`pr-reviewer`, `architect-reviewer`).
- Skills (`/ship`, `/feature`).
- Claude Code hooks nativos.

Calibracao em sessao separada.
```

## Pos-criacao do PR

1. Abrir PR via `gh pr create`.
2. Capturar o numero.
3. Editar `docs/decisoes.md`, `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>` e `2026-MM-DD` pela data real.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorizacao explicita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `docs/etapa-4-6-claude-md` empurrada com 3 commits (2 + 1 update do PR).
- PR aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.5.
- Working tree limpo.
- `CLAUDE.md` presente na raiz.
- Reportar com `git log --oneline -3`, `git status`, `gh pr view --json number,state,statusCheckRollup`, e contagem de linhas do `CLAUDE.md`.

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao criar prompt da proxima sub-etapa.
- Nao criar hooks, scripts, subagents, skills.
- Nao tocar em codigo (`src/`, `pom.xml`, `frontend/`, scripts).
- Nao tocar em arquivos das sub-etapas anteriores.
- Nao tocar em `.gitignore`, `.gitattributes`, ADRs, `hooks-pendentes.md`.
- Nao deixar arquivos `test-*.*` (nao houve cenarios destrutivos tradicionais — nada de teste para limpar).
- Nao sugerir proximo passo espontaneamente.
- Nao colocar conteudo volatil no CLAUDE.md (estado atual, lista de hooks, lista de prompts).
- Nao passar de 200 linhas no CLAUDE.md. Limite duro 250.
- Nao usar `pwsh` em qualquer comando.
- Nao colocar caracteres nao-ASCII em strings de qualquer `.ps1` (esta sub-etapa nao cria `.ps1`, mas regra permanece).
