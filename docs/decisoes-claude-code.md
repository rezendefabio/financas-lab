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

## Sub-etapa 5.1 -- Bounded context `orcamento` (Camada 4 inaugurada)

### Camada 4: primeiro feature Tier 2

Fluxo Tier 2 exercitado pela primeira vez: /feature -> domain -> /migrate -> application
+ infra + interfaces -> /ship. Bounded context `orcamento` entrega CRUD + progresso
por categoria/mes.

### Decisao: cross-bounded-context no use case de progresso

`CalcularProgressoDoOrcamentoUseCase` injeta `TransacaoRepository` diretamente,
seguindo o padrao estabelecido por `CalcularSaldoDaContaUseCase` (Camada 2). Sem
wrapper ou porta intermediaria entre bounded contexts.

### Decisao: StatusProgresso enum no domain

`StatusProgresso` (ABAIXO, ATENCAO, ATINGIDO, EXCEDIDO) criado manualmente em
`orcamento/domain/` -- nao gerado pelo /feature (geradores criam apenas o minimo
estrutural, nao enums de dominio especificos).

---

## Sub-etapa 5.2 -- test-writer ampliado para application use cases

### Padrao de unit test para application use cases

`*/application/*UseCase.java` usa Mockito (nao mock manual inline) porque use cases
tem dependencias de interface (repositories) que exigem stub de comportamento especifico.
Diferente de `*/domain/` onde mocks manuais bastam, use cases precisam de `when().thenReturn()`
para simular cenarios de repositorio (buscar/nao encontrar, retornar lista vazia, etc.).

Referencia de estilo: `CalcularSaldoDaContaUseCaseTest.java` (Camada 2, ja existia).

### Decisao: smoke = entrega real

O smoke da 5.2 e tambem o artefato de valor: `CalcularProgressoDoOrcamentoUseCaseTest.java`
resolve a lacuna identificada pelo pr-reviewer do PR #73. Commitado na branch, nao descartado.

---

## Sub-etapa 5.3 -- Bounded context `meta`

### Padrao: dois @Embedded do mesmo tipo na mesma Entity

Quando uma Entity tem dois campos Money (ex: `valorAlvo` e `valorAtual`), cada um
precisa de `@AttributeOverride` proprio com nomes de colunas distintos. Sem isso,
JPA levanta `HibernateException: column "valor" is mapped more than once`.

Exemplo:

```java
@Embedded
@AttributeOverride(name = "valor", column = @Column(name = "valor_alvo_valor", ...))
@AttributeOverride(name = "moeda", column = @Column(name = "valor_alvo_moeda", ...))
private MoneyEmbeddable valorAlvo;

@Embedded
@AttributeOverride(name = "valor", column = @Column(name = "valor_atual_valor", ...))
@AttributeOverride(name = "moeda", column = @Column(name = "valor_atual_moeda", ...))
private MoneyEmbeddable valorAtual;
```

### Decisao: fromDomain() estatico no DTO

`MetaResponse.fromDomain(Meta meta)` encapsula a conversao inline (percentualConcluido,
atrasada computados). Padrao herdado de ContaResponse. Preferido a mapper separado
para DTOs de interface -- logica de apresentacao fica no proprio DTO.

### Decisao: deposito como sub-recurso POST

`POST /api/metas/{id}/depositos` em vez de `PUT /api/metas/{id}` para registrar deposito.
Semantica correta: deposito e uma acao, nao uma atualizacao parcial do recurso.

---

## Sub-etapa 5.9 -- Frontend foundation: base-nova e render prop

### Decisao: `render` prop em vez de `asChild`

O design system `base-nova` usa `@base-ui/react` (nao `@radix-ui`). A prop `asChild`
do Radix nao existe -- o padrao equivalente e `render prop` (ex:
`<SidebarMenuButton render={<Link href="...">} />`). Qualquer componente que tente
usar `asChild` com base-nova quebra silenciosamente. Registrado como bloqueador B2
no `front-reviewer`.

### Decisao: Vitest + Testing Library para frontend

Jest nao era opcao (incompatibilidade com App Router + RSC). Vitest com
`@testing-library/react` cobre componentes, hooks e services. Testes ficam colocados
junto ao arquivo-alvo (`componente.test.tsx` ao lado de `componente.tsx`).

---

## Sub-etapas 5.12/5.15/5.16 -- front-reviewer, ADR-013 e B6

### Decisao: front-reviewer com categorias B/S/E

O `front-reviewer` classifica apontamentos em tres categorias:
- **Bloqueadores (B):** violacoes objetivas que impedem merge (fetch fora de services/,
  asChild em base-nova, URL hardcoded de ambiente, `any` em tipos de API, credencial literal,
  valores Instant com formatDate em vez de formatDateTime, tipo de campo divergente do catalog).
- **Sugestoes (S):** nao bloqueadoras (console.log em producao, componente sem teste).
- **Elogios (E):** boas praticas observadas.

### Decisao: ADR-013 -- feature-first no frontend

Organizacao `src/features/<dominio>/` com `services/`, `types/`, `hooks/`, `components/`,
`index.ts`. Codigo compartilhado em `src/shared/`. Codigo de dominio nao vaza para `src/app/`.
Registrado como ADR-013 no `adrs.md`.

### Decisao: B6 -- Zod espelha anotacoes Java

Schema Zod do formulario frontend deve espelhar cada anotacao Java no `*Request.java`
correspondente: `@NotBlank` -> `.min(1)`, `@Size(max=N)` -> `.max(N)`, `@Min(N)` -> `.min(N)`.
Divergencia entre Zod e Java e bloqueador B6. Validacao espelhada elimina categoria
inteira de bugs de validacao assimetrica.

---

## Sub-etapas 5.20/5.21 -- /batch e babysit-prs: Camadas A e B da fabrica

### Decisao: "Boris Cherny model" -- loops + parallelism

A fabrica opera em dois eixos paralelos:
- **Camada A (/batch):** execucao paralela de tasks via Agent tool com `isolation: worktree`.
  Acao atomica: todos os N Agent calls emitidos em uma unica resposta, antes de qualquer
  resultado. Loop serial e bug.
- **Camada B (babysit-prs):** routine Tier 1 de monitoramento continuo via `ScheduleWakeup`.

### Decisao: 270s (nao 300s) para ScheduleWakeup

O cache do Claude Code tem TTL de 5 minutos (300s). Usar exatamente 300s paga o cache miss
sem amortiza-lo. `delaySeconds: 270` mantem o contexto quente. `delaySeconds: 600` (10min)
foi o valor inicial do babysit-prs; reduzido para 270s na 5.50.

---

## Sub-etapa 5.26 -- field-type-catalog: B7

### Decisao: catalogo prescritivo antes de implementar qualquer campo frontend

`docs/field-type-catalog.md` mapeia tipo Java -> componente React (BigDecimal -> MoneyInput,
UUID FK -> Select carregado da API, Instant -> formatDateTime, etc.). O executor deve
consultar o catalogo antes de implementar qualquer campo. Violacao e bloqueador B7 no
`front-reviewer`. Objetivo: eliminar a categoria de bugs onde o componente errado e
escolhido por inferencia incorreta do tipo.

---

## Sub-etapa 5.28 -- /batch inline: prompts efemeros

### Decisao: executor recebe conteudo inline, nao path de arquivo

A partir da 5.28, `/batch` le o arquivo de prompt com Read tool e embute o conteudo
inline no template do sub-agente. O executor nao precisa que o arquivo exista no
worktree. `docs/prompts/` adicionado ao `.gitignore` -- prompts sao artefatos de
orquestracao, nao codigo versionado. O registro permanente fica em `docs/progresso.md`.

---

## Sub-etapas 5.29/5.30 -- sub-agentes inteligentes de CI e conflito

### Decisao: sub-agente resolve conflito com raciocinio sobre intencao

Em vez de abortar conservadoramente qualquer falha de rebase, o babysitter spawna
`conflict-resolver` que le cada arquivo em conflito na integra, entende a intencao de
"ours" (main) vs "theirs" (branch do PR), e produz sintese correta. Aborta apenas
diante de contradicao genuina de negocio. Extraido para `.claude/agents/conflict-resolver.md`
na 5.35 (ADR-012 compliance).

### Decisao: CI auto-fix via sub-agente com maximo 2 tentativas

`ci-fixer` le o log de falha, identifica causa raiz (compilacao, testes, lint), aplica
correcao minima em worktree isolado, valida com `check.ps1`, commita e faz push.
Se a correcao exigir decisao de negocio: reporta "NAO CORRIGIDO: requer intervencao
humana". Extraido para `.claude/agents/ci-fixer.md` na 5.35.

---

## Sub-etapa 5.33 -- /plan: aprovacao humana e rastreamento persistente

### Decisao: gate humano entre planejamento e spawning

`/plan` segue fluxo Passo 0 (state) -> Passo 1 (planejador) -> Passo 2 (exibe plan) ->
Passo 3 (AskUserQuestion: "Sim, spawnar" / "Nao, cancelar") -> Passo 4 (spawn) ->
Passo 5 (aguarda e atualiza). O humano aprova o plano antes de qualquer executor ser
spawado. Default da confirmacao e NAO.

### Decisao: tasks.json como estado persistente

`.claude/tasks.json` (gitignored) persiste `id`, `planId`, `titulo`, `status`,
`branch`, `pr_url`, `created_at`, `updated_at`. Permite retomar o acompanhamento
entre sessoes via `/tasks`. Garante rastreabilidade mesmo se a sessao for encerrada
durante a execucao.

---

## Sub-etapas 5.47/5.48 -- /setup-design e /init-project: macro-skills

### Decisao: macro-skill orquestra outras skills sem logica propria

`/init-project` introduz a quarta categoria de skill: macro-skill orquestradora que
invoca `/setup-architecture`, `/setup-design` e `/setup-infra` em sequencia, lendo
cada SKILL.md e executando manualmente (nunca via Skill tool). Toda logica de geracao
e delegada para as skills especializadas.

### Decisao: design-planner como primeiro sub-agente de design

`design-planner` (Sonnet, context: fork) recebe dominio do projeto e produz proposta
de paleta OKLCH, tipografia, mapeamento tipo-dado -> componente, page templates e
bloqueadores B8/B9/B10. Skill exibe proposta ao operador via AskUserQuestion antes
de escrever qualquer arquivo.

---

## Sub-etapas 5.49/5.59 -- isolamento de worktree: guard obrigatorio

### Decisao: executor verifica worktree antes de qualquer acao de arquivo

Causa raiz de arquivos residuais no repo principal: executor operando no worktree
isolado mas criando arquivos com path relativo que resolvia para o diretorio do
processo pai. Guard obrigatorio no inicio de cada executor:

```bash
worktree_root=$(git rev-parse --show-toplevel)
if [ "$worktree_root" = "/c/projetos/financas-lab" ]; then
  echo "ERRO CRITICO: diretorio e o repo principal."
fi
```

E limpeza obrigatoria antes de encerrar: verificar `git status --short | grep "^??"` e
remover qualquer arquivo `??` inesperado.

---

## Historico de mudancas deste documento

- **2026-05-12** -- Sub-etapa 5.3 concluida: bounded context `meta`. Padrao dois @Embedded
  na mesma Entity. fromDomain() no DTO. POST sub-recurso para depositos. PR #76.
- **2026-05-12** -- Sub-etapa 5.2 concluida: test-writer ampliado (4o nivel: application
  use cases com Mockito). Lacuna do PR #73 resolvida. PR #75.
- **2026-05-12** -- Sub-etapa 5.1 concluida: bounded context `orcamento`. Camada 4
  inaugurada. Primeiro feature Tier 2 completo. PR #73.
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
- **2026-05-15** -- Sub-etapa 5.61 concluida: skill `/factory-metrics` (routine Tier 1).
  Decisoes de design: (1) throttle 20h reutiliza padrao do daily-summary; (2) filtro de
  branch `^(feat|fix|docs)/` exclui `refactor/` deliberadamente (raros, nao afetam metrica
  de producao da fabrica); (3) `tempo_spec_pr_min` mede diferenca entre abertura do PR e
  data do primeiro commit -- proxy de "work to PR", nao "spec to PR" (nome historico, mantido
  por compatibilidade com o state file); (4) `teve_correcao_autonoma` e heuristica baseada
  em commits `fix(` + mais de 1 commit total -- detecta iteracao com correcao, mas nao
  distingue feedback humano de correcao autonoma exata. PR #145.

---

## Decisoes Arquiteturais Pendentes de Implementacao (2026-05-17)

Decisoes tomadas em sessao de discussao. Nenhuma sub-etapa aberta ainda --
registradas aqui para orientar o planejador quando as features entrarem no backlog.

### Relatorios Impressos (PDF)

**Decisao:** `@react-pdf/renderer` no frontend (Next.js), nao JasperReports no backend.

**Racional:** PDF e preocupacao de apresentacao -- pertence a camada de view. O executor
ja conhece React; `@react-pdf/renderer` usa os mesmos padroes (componentes declarativos,
props tipadas) que o resto do frontend. JasperReports exigiria que o executor aprendesse
templates `.jrxml` e a API Java da biblioteca -- conhecimento que nao transfere entre
bounded contexts e que o operador (unico humano no loop) nao poderia revisar com agilidade.

**Operador tem experiencia com JasperReports** mas isso nao transfere para o executor.
Consistencia da fabrica prevalece sobre familiaridade do operador com a ferramenta.

**Impacto na fabrica:**
- Nova dependencia: `@react-pdf/renderer` em `frontend/package.json`
- `docs/field-type-catalog.md`: adicionar secao "Campos em relatorio impresso" com
  mapeamento de tipos Java para elementos `@react-pdf/renderer` (`<Text>`, `<View>`,
  formatters)
- Pagina de relatorio impresso: componente React isolado, sem `'use client'` de
  interacao -- exporta `<PDFDownloadLink>` ou `<PDFViewer>` conforme contexto
- Se necessario arquivar o PDF gerado: frontend gera blob → `POST /api/anexos` via
  bounded context `anexo` (padrao de gerenciamento de arquivos abaixo)

**Nao se aplica a** dashboards e resumos em tela (Recharts, ja implementado).

---

### Gerenciamento de Arquivos (Upload/Download)

**Decisao:** MinIO no Docker para dev + S3 na nuvem para prod; bounded context `anexo`
como camada de abstracao transversal.

**Racional:** MinIO e S3-compatible -- mesmo SDK Java (`software.amazon.awssdk:s3`),
mesma API, configuracao isolada em `application-dev.yml` vs `application-prod.yml`.
Zero infra adicional para dev (ja temos Docker). Cloudflare R2 e alternativa mais
barata que S3 para prod -- decisao de infra adiada para quando houver deploy real.

**Bounded context `anexo`:**

```
Anexo {
  id:            UUID (PK)
  entidadeTipo:  ENUM (TRANSACAO, NOTA, META, ...) -- referencia logica, sem FK
  entidadeId:    UUID
  nomeOriginal:  String (nome do arquivo que o usuario enviou)
  nomeStorage:   String (UUID gerado -- evita colisao e nao expoe nome original)
  contentType:   String (MIME type)
  tamanhoBytes:  Long
  criadoEm:      Instant
}
```

`entidadeId` e referencia logica (sem FK de banco) para evitar acoplamento estrutural
entre bounded contexts. Cada bounded context que precisa de anexos injeta `AnexoRepository`
na camada de application -- nao conhece o storage diretamente.

**Endpoints:**
- `POST /api/anexos` (multipart/form-data): recebe arquivo, salva no storage, persiste metadados
- `GET /api/anexos/{id}/download`: retorna redirect para URL pre-assinada (S3/MinIO) ou stream direto

**Impacto na fabrica:**
- `docs/field-type-catalog.md`: adicionar entrada para campos do tipo "arquivo anexo"
  (input = `<Input type="file">` com validacao de tipo/tamanho, exibicao = link de download)
- CLAUDE.md: documentar que bounded contexts com upload nao usam `/feature` puro --
  dependem do bounded context `anexo` e requerem configuracao de storage no `application*.yml`

---

### Mobile

**Decisao (curto prazo):** PWA -- `manifest.json` + service worker (Workbox) + icones.
Reusa integralmente o Next.js existente. Previsto no MVP (`visao.md`).

**Decisao (longo prazo, pos-MVP):** React Native com Expo se app nativo for necessario.

**Racional do RN/Expo:** reutiliza tipos TypeScript dos services existentes; executor ja
conhece o ecossistema React; a fabrica ganha uma skill `/feature-rn` analogica ao
`/feature-front` sem precisar ensinar Dart/Flutter ao executor. Flutter invalida todo o
padrao de skills frontend atual -- custo desproporcional.

**App nativo esta explicitamente fora do MVP** (`visao.md`, secao "O que fica fora").
Reabertura exige justificativa explicita conforme politica do documento de visao.

---

### Complexidade Alem de CRUD

Tres padroes distintos, com estrategias diferentes:

**A. Regras de negocio ricas (dominio com comportamento)**

Padrao atual ja e correto: entidades com metodos (`desativar()`, `registrarDeposito()`).
O que falta documentar e quando criar **domain events** em vez de logica direta no UseCase.

Criterio: se um UseCase precisar notificar outro bounded context apos uma operacao
(ex: `TransacaoCriada` → atualizar saldo da `Conta`), usar Spring Application Events
(`ApplicationEventPublisher` + `@EventListener`). Evita acoplamento direto entre
repositorios de bounded contexts distintos na camada de application.

Impacto na fabrica: adicionar secao em CLAUDE.md sobre quando usar eventos vs chamada
direta. O planejador deve detectar dependencias cruzadas entre bounded contexts e
declarar `tipo: "feature_com_eventos"` na task.

**B. Processamento assincrono leve (operacoes em background)**

Ferramenta: `@Async` do Spring (configurado via `@EnableAsync` + `ThreadPoolTaskExecutor`).
Casos de uso: recalculo de saldo pos-importacao, envio de notificacao, geracao de PDF
server-side se necessario.

Sem mensageria, sem infra extra. O UseCase retorna imediatamente; o trabalho pesado
roda em thread separada do pool. Falhas sao logadas -- sem retry automatico neste nivel.

**C. Processamento assincrono pesado (jobs longos)**

Ferramenta: Spring Batch. Casos de uso: importacao de CSV (prevista no MVP),
categorizacao automatica por IA (pos-MVP), conciliacao bancaria (pos-MVP).

Spring Batch adiciona suas proprias tabelas de metadados (gerenciadas pelo Flyway).
Bounded context `job` contem `JobLauncher`, `Job`, `Step` e listeners.
A fabrica precisaria de uma skill `/feature-job` quando este padrao for inaugurado.

**Ordem de implementacao sugerida:**
1. `@Async` leve (entra junto com a primeira feature que precisar, sem sub-etapa propria)
2. Spring Batch (entra na sub-etapa de importacao CSV -- prevista no MVP)
3. Domain events (entra quando surgir primeira dependencia cruzada real entre bounded contexts)
