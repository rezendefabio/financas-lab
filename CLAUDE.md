# financas-lab

Laboratorio AI-native para construcao de fabrica de software replicavel.
Domain: gestao financeira pessoal (proxy de estudo, nao produto final).
Operador unico: Fabio Rezende. Sessoes via Claude Code em Windows nativo.

Estado atual e camadas: `docs/progresso.md` (secao "Status geral por Camada").

## Stack

- Java 21 + Spring Boot 3.x.
- PostgreSQL em Docker (container `financas-lab-postgres`).
- Migrations: Flyway (`src/main/resources/db/migration/V*.sql`).
- Persistencia: JPA + Hibernate.
- Mapeamento DTO: MapStruct + Lombok (ordem importa em `pom.xml`).
- Build: Maven (`pom.xml` na raiz, modulo unico). `<release>${java.version}</release>` no maven-compiler-plugin.
- Frontend: ainda nao implementado (planejado para Camada 5).
- Sem Gradle. Sem Kotlin. Sem React ainda.

## Ambiente

- SO: Windows nativo (nao WSL, nao Linux). Paths usam backslash em alguns contextos.
- Shell: PowerShell 5.1 (nativo). **NAO usar `pwsh`** -- PowerShell Core 7 nao esta disponivel.
- Git Bash disponivel (vem com Git for Windows).
- Docker Desktop ativo. Compose: `docker compose up -d`.

### Comandos do dia

- Setup inicial: `.\scripts\setup.ps1` (idempotente).
- Dev (start backend + deps): `.\scripts\dev.ps1`.
- Validacao completa: `.\scripts\check.ps1` (mvn verify + verifica encoding).
- Subir banco isolado: `docker compose up -d postgres`.

## Hooks ativos

Mecanismo: `core.hooksPath=.githooks`. Cada entrypoint em `.githooks/` (`commit-msg`, `pre-commit`) invoca companheiro `.ps1` que delega para hook real em `.claude/hooks/{escopo}/`. Detalhes em `.githooks/README.md`.

Modos:

- **`fail`** para regras objetivas -- bloqueia commit em violacao.
- **`warn`** para regras subjetivas -- alerta no terminal sem bloquear.

Decisao de modo registrada em `docs/decisoes.md` quando o hook nasce.

Override: `git commit --no-verify` bypassa hooks. Uso documentado no PR body.

Lista completa de hooks ativos e suas regras: `docs/hooks-pendentes.md` (secao "Hooks implementados").

## Convencoes e padroes

### Branches

- `feat/etapa-X-Y-descricao` para sub-etapas que adicionam funcionalidade.
- `fix/...` para correcoes.
- `docs/...` para sub-etapas doc-only.

### Commits

Conventional Commits obrigatorio (hook ativo). Mensagens em portugues, sem acentos no codigo, ASCII apenas em strings de scripts `.ps1`.

### Sub-etapas

Trabalho organizado em sub-etapas pequenas dentro de Camadas. Cada sub-etapa: 1 branch, 3-4 commits, 1 PR, validacao destrutiva, smoke test pos-merge. Calibracao via D1-D5 antes do prompt. Padrao detalhado em `docs/progresso.md`.

### Modelo executor (Camada 4+)

A partir da Camada 4 o trabalho segue o modelo orquestrador/executor:

- **Orquestrador** (esta sessao): gera o prompt da sub-etapa em `docs/prompts/prompt-etapa-X-Y.md` e abre PR do `.md` antes de o executor iniciar.
- **Executor** (sessao nova): le o arquivo de prompt do disco e entrega o PR de forma autonoma.

Fluxo:
1. Orquestrador gera `docs/prompts/prompt-etapa-X-Y.md` (arquivo fica untracked no disco)
2. Operador abre terminal novo e roda: `claude --dangerously-skip-permissions`
3. Operador instrui o executor: `"leia docs/prompts/prompt-etapa-X-Y.md e execute"`
4. Executor le o arquivo, executa todas as tarefas, commita o `.md` junto com os docs
   no ultimo commit da branch e entrega o PR autonomamente

Para execucao sem interrupcao de aprovacao de ferramentas, o executor deve ser iniciado com:

```
claude --dangerously-skip-permissions
```

Sem esse flag o executor para em cada tool call (Bash, Write, Edit, git) aguardando aprovacao manual -- o que quebra o ciclo autonomo da fabrica.

### Subagents e skills

Subagents do projeto vivem em `.claude/agents/<nome>.md` (flat); skills orquestradoras correspondentes em `.claude/skills/<nome>/SKILL.md` (flat). Skill usa `context: fork` + `agent: <nome>` no frontmatter para forkar contexto no subagent. Operador invoca via slash command (`/<nome> <args>`); skill com `disable-model-invocation: true` nao e invocada automaticamente. Invocacao proativa via campo `description` no subagent e considerada nao-deterministica e nao e mecanismo primario (ADR-012).

### Validacao destrutiva (ADR-011)

Toda nova regra/hook exige validacao destrutiva com cenarios explicitos. Pre-condicoes obrigatorias: `Test-Path` apos criar arquivo, `git status` antes de `git commit`, verificacao de `$LASTEXITCODE`, sincronizacao de `[System.Environment]::CurrentDirectory = (Get-Location).Path` antes de `[System.IO.File]::WriteAllText` com path relativo.

### Decisao silenciosa em zona limitrofe

Padrao central vigiado. Em divergencia entre prescricao e ambiente real, parar e reportar, nunca adivinhar.

## Onde buscar mais

Documentos de referencia em `docs/`:

- `progresso.md` -- onde estamos. Tracking de Camadas e sub-etapas. Licoes meta-operacionais.
- `progresso-historico.md` -- historico arquivado de Camadas concluidas (Camadas 0, 1, 2 ate sub-etapa 3.8).
- `decisoes.md` -- escolhas tomadas. Por que cada regra existe (decisoes fundacionais: stack, arquitetura, convencoes).
- `decisoes-claude-code.md` -- decisoes operacionais da Camada 3 (sub-etapas 4.19+). Historico arquivado em `decisoes-claude-code-historico.md` (4.0-4.18).
- `adrs.md` -- decisoes arquiteturais formais.
- `hooks-pendentes.md` -- backlog de hooks + hooks implementados (lista completa).
- `visao.md` -- direcao do projeto e Camadas planejadas.

Prompts versionados de cada sub-etapa ficam em `docs/prompts/prompt-etapa-X-Y.md`. Nao listados individualmente; agente busca quando precisa.

## O que NAO fazer

- Usar `pwsh` em scripts. PowerShell 5.1 (`powershell`) e o unico disponivel.
- Caracteres nao-ASCII em strings de hooks `.ps1`. Em-dash U+2014 quebra parse (licao 4.4).
- `git commit --no-verify` sem documentar no PR body.
- `git reset --hard` em main, ou sem confirmar `git branch --show-current` primeiro.
- Tocar em `pom.xml` removendo `<release>` (hook bloqueia, e por bom motivo -- licao 1.4).
- Criar arquivos `.md` em `docs/` sem linhas em branco antes/depois de headers (hook bloqueia).
- Validacao destrutiva sem pre-condicoes ADR-011 -- produz falsos positivos silenciosos.
- Assumir contexto sem ler arquivos vivos antes de editar (lista de "ler arquivos vivos" nos prompts).
- Atualizar CLAUDE.md fora de uma sub-etapa que muda hook/padrao/stack. Sincronizacao e parte do escopo da sub-etapa causadora.
