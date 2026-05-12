# Decisoes — Camada 3 (Configuracao do Claude Code)

> Documento dedicado a decisoes operacionais da Camada 3 do projeto: hooks, subagents, skills, padroes de validacao destrutiva, convencoes operacionais de Claude Code.
> Origem: separado de `docs/decisoes.md` na Sub-etapa 4.16 quando o arquivo original cruzou 800 linhas (trigger do hook 4.4 modo warn).
> Para decisoes fundacionais do projeto (Stack, Arquitetura, Convencoes de codigo, Politica de debito, Comandos atomicos, Frontend, Modelo financeiro, Principios do blueprint), ver `decisoes.md`.

**Data de criacao:** 2026-05-11 (Sub-etapa 4.16)

> **Historico arquivado:** decisoes das sub-etapas 4.0 a 4.18 (hooks, primeiros subagents, manutencao de docs) foram movidas para `docs/decisoes-claude-code-historico.md` na Sub-etapa 4.26.

---

## Sub-etapa 4.19 -- Skill `/feature` sem subagent

### Decisao: skill direta vs skill-com-fork

ADR-012 (4.10/4.11) prescreveu o padrao `skill orquestradora -> subagent dedicado`
para tarefas que exigem fork de contexto. A 4.19 introduz variante nova: **skill
direta sem subagent**. Claude Code principal le o `SKILL.md` e executa as instrucoes
com seus proprios tools (Write + Bash), sem fork.

Criterio de escolha entre os dois padroes:

| Padrao | Quando usar |
|---|---|
| Skill com fork (ADR-012) | Tarefa exige isolamento de contexto, modelo diferente do principal, ou tools mais restritas |
| Skill direta (4.19) | Tarefa e procedural (sequencia de Write + Bash), sem necessidade de isolamento, sem logica de negocio emergente |

`/feature` e claramente procedural: valida argumento, cria diretorios, escreve 11
arquivos de template fixo, reporta resultado. Nao ha raciocinio de dominio que
justifique isolamento em subagent.

### Frontmatter da skill direta

```yaml
name: feature
disable-model-invocation: true
argument-hint: [nome-do-bounded-context]
```

Ausencia de `context: fork` e `agent:` e intencional e distingue a skill direta das
skills-com-fork existentes. Sem `allowed-tools` restrito -- Claude Code usa seus tools
normais (Write, Bash) conforme prescrito pelo corpo do SKILL.md.

### Categoria operacional

"Primeira aplicacao de padrao em eixo novo" -- mesma da 4.17 (primeiro subagent
gerador). A 4.19 estreia o eixo "skill sem subagent", distinto de:

- skills-com-fork (review-pr 4.11, review-arch 4.12, write-test 4.17)
- subagents revisores (pr-reviewer 4.9, architect-reviewer 4.12)
- subagents geradores (test-writer 4.17)

### Smoke parcial honesto

Cobaia natural nao disponivel (operador nao tem bounded context novo para produzir
agora). Smoke verifica: (1) skill executa sem erro; (2) 11 arquivos criados com
conteudo correto; (3) `mvn compile` passa. `mvn verify` e responsabilidade do
desenvolvedor apos criar migration Flyway para a Entity gerada (hook 4.7 bloqueia
commit sem migration). Padrao registrado pela segunda vez (primeira: 4.17).

### Aviso para uso real

O skeleton gerado inclui `@Entity` em `NOMEEntity.java`. Hook 4.7 bloqueia commit de
arquivo Java novo com `@Entity` sem migration Flyway correspondente. Desenvolvedor
deve criar `V<n>__create_ARG_table.sql` antes de commitar o bounded context.

## Sub-etapa 4.20 -- Skill `/ship` sem subagent

Segunda aplicacao do padrao inaugurado na 4.19 (skill direta sem `context: fork`).
Nenhuma decisao estrutural nova -- replicacao pura. Detalhes em [[project-scope-claude-dot]].

Criterio de escolha confirmado: skill direta e adequada quando a tarefa e
puramente procedural (sequencia de comandos shell com logica de fluxo simples).
`/ship` confirma o padrao: 5 passos bem definidos, sem raciocinio de dominio.

Nota de smoke: `/ship` testou a si propria ao criar o PR #66. Primeiro smoke
verdadeiramente automatico e completo do projeto (sem cobaia artificial e sem
parcialidade -- gate real, push real, PR real).

## Sub-etapa 4.21 -- Skill `/audit` sem subagent

Terceira replicacao do padrao skill direta (4.19, 4.20, 4.21). Nenhuma decisao
estrutural nova.

SKILL.md mais curto que os anteriores: a skill instrui Claude Code a usar o tool
`Grep` diretamente, sem scripting PowerShell. O trabalho pesado (regex, indexacao
de arquivos, formatacao de output) e feito pelo Grep nativo do Claude Code.

Padrao consolidado: skills procedurais de baixa complexidade delegam para tools
nativos do Claude Code (Grep, Glob, Write, Bash) conforme necessidade -- sem
overhead de subagent.

## Sub-etapa 4.22 -- Hook post-edit (hook nativo Claude Code)

### Git hook vs hook nativo Claude Code

Hooks anteriores (4.1-4.7) sao **git hooks**: disparam em eventos do git
(`pre-commit`, `commit-msg`), configurados via `core.hooksPath=.githooks`.
Vivem em `.githooks/` (entrypoints) e `.claude/hooks/` (logica).

O hook post-edit (4.22) e um **hook nativo do Claude Code**: dispara em eventos
do harness (`PostToolUse`), configurado em `.claude/settings.json`. Vive em
`.claude/hooks/post-edit/run-tests.ps1`.

### Decisao: PostToolUse e non-blocking

PostToolUse nunca bloqueia a execucao do Claude Code -- a tool (Edit/Write) ja
rodou quando o hook e chamado. O hook fornece feedback (testes passando/falhando)
mas nao impede nenhuma acao. Modo equivalente a `warn` dos git hooks.

### Decisao: settings.json via setup.ps1

`.claude/settings.json` e gitignored (decisao da 4.0 -- configs locais pessoais
nao versionadas). A configuracao do hook e gerada pelo `setup.ps1` (idempotente:
cria se nao existe, pula se ja existe). Script do hook (`.claude/hooks/post-edit/
run-tests.ps1`) e versionado -- apenas o `settings.json` que o referencia e local.

### Escopo: apenas domain, apenas unit

- Domain files (`*/domain/*.java`): unit tests rapidos, sem Docker, sem Testcontainers.
- Infrastructure files (`*RepositoryImpl.java`): EXCLUIDOS -- integration tests lentos
  demais para hook post-edit.
- Arquivos sem teste: hook silencioso (sem ruido).

## Sub-etapa 4.23 -- Subagent migration-writer

### Subagent vs skill direta

Decisao: subagent (mesmo padrao do test-writer, 4.17) -- nao skill direta (padrao da 4.19).
Criterio confirmado na calibracao da 4.23: derivacao Java -> SQL exige raciocinio sobre tipos
e anotacoes, nao e sequencia procedural simples. Skill direta e adequada para sequencias
de comandos shell com logica simples; subagent e adequado para raciocinio de dominio.

### Escopo do SQL gerado

Decisao deliberada: migration-writer gera apenas `CREATE TABLE` basico.

- **FK constraints:** NAO geradas. Dependem de ordem de criacao de tabelas e de conhecimento
  do schema completo -- informacao que o subagent nao tem de forma confiavel.
- **Indexes:** NAO gerados. Domain-specific; o desenvolvedor conhece os padroes de query.
- **CHECK constraints:** NAO geradas. Logica de negocio; risco de erro silencioso alto.

Resultado: SQL gerado e sempre correto (sem FK/CHECK errados), mas incompleto (exige
complemento manual). Trade-off aceito: corretude > completude automatica.

### Versao Flyway

Numeracao automatica (max das migrations existentes + 1). Colisao impossivel durante uso
normal (um desenvolvedor, sem migrations paralelas). Se colisao ocorrer: migration-writer
reporta o conflito e nao sobrescreve.

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

## Sub-etapa 4.25 -- E2E tests e fechamento da Camada 3

### Terceiro nivel de teste no test-writer

Taxonomia de niveis de teste consolidada:

| Nivel | Path | Framework | Infra |
|-------|------|-----------|-------|
| Unit | `*/domain/*.java` | JUnit 5 + AssertJ | Nenhuma |
| Integration | `*/infrastructure/persistence/*RepositoryImpl.java` | JUnit 5 + AssertJ + Testcontainers | AbstractIntegrationTest |
| E2E | `*/interfaces/*Controller.java` | JUnit 5 + AssertJ + MockMvc + Testcontainers | AbstractIntegrationTest + @AutoConfigureMockMvc |

### Decisao: AbstractIntegrationTest como base para E2E

E2E tests estendem `AbstractIntegrationTest` (mesma base dos integration tests) porque
precisam do banco Testcontainers para o stack completo. A diferenca e so `@AutoConfigureMockMvc`
na classe de teste para habilitar MockMvc -- `@SpringBootTest` da base ja usa webEnvironment
MOCK por default, compativel com MockMvc.

### Decisao: escopo de E2E (happy path + erro principal)

E2E tests cobrem status HTTP e body minimo -- nao testam logica de negocio em profundidade.
Racional: logica de negocio ja e coberta por unit tests de dominio; E2E serve para validar
que o wiring (controller -> use case -> repositorio -> banco) esta correto.

### Fechamento da Camada 3

Camada 3 concluida com 4.25. Entregou:
- Git hooks (4.1-4.7, 4.14): lint, encoding, markdown, stack, docs-size, post-edit
- Subagents (4.9, 4.9.1, 4.17, 4.17.1, 4.18, 4.23): pr-reviewer, test-writer (3 niveis), migration-writer
- Skills (4.11, 4.19-4.24): /review-pr, /review-arch, /feature, /ship, /audit, /write-test, /write-migration, /migrate
- Hook nativo Claude Code (4.22): PostToolUse post-edit

---

## Historico de mudancas deste documento

- **2026-05-12** -- Sub-etapa 4.26 concluida: split de `decisoes-claude-code.md`.
  Historico 4.0-4.18 arquivado em `decisoes-claude-code-historico.md`. Arquivo ativo
  reduzido de ~880 para ~250 linhas. CLAUDE.md atualizado com link para historico. PR #72.
- **2026-05-12** -- Sub-etapa 4.25 concluida: test-writer E2E (terceiro nivel).
  Camada 3 fechada. Debito: split decisoes-claude-code.md (808 linhas). PR #71.
- **2026-05-12** -- Sub-etapa 4.24 concluida: skill `/migrate` orquestradora (terceira
  categoria). Taxonomia de skills do projeto consolidada em 3 categorias. PR #70.
- **2026-05-12** -- Sub-etapa 4.23 concluida: subagent `migration-writer` (segundo gerador
  do projeto). Derivacao Java -> SQL de anotacoes JPA. Escopo deliberadamente basico
  (sem FK/CHECK/indexes). Numeracao Flyway automatica. Pre-requisito para 4.24. PR #69.
- **2026-05-12** -- Sub-etapa 4.22 concluida: primeiro hook nativo Claude Code
  (`PostToolUse`). Git hook vs hook nativo documentado. `setup.ps1` como gestor de
  `settings.json`. Escopo domain-only. PR #68.
- **2026-05-12** -- Sub-etapa 4.21 concluida: skill `/audit` direta. Terceira
  replicacao do padrao 4.19. Grep nativo, output estruturado. PR #67.
- **2026-05-12** -- Sub-etapa 4.20 concluida: skill `/ship` direta (sem subagent).
  Replicacao pura do padrao 4.19. Smoke natural completo (skill criou o proprio PR).
  PR #66.
- **2026-05-12** -- Sub-etapa 4.19 concluida: skill `/feature <nome>` direta (sem
  subagent). Padrao novo: skill procedural usa Write + Bash sem fork. Criterio de
  escolha entre skill-com-fork e skill-direta registrado. Categoria "primeira aplicacao
  de padrao em eixo novo". Smoke parcial honesto (segunda aplicacao do padrao). PR #65.
