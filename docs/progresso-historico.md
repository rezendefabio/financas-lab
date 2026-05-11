# Progresso -- Historico Arquivado

> Historico arquivado de Camadas concluidas. Documento de leitura, nao vivo.
> Origem: separado de `docs/progresso.md` na Sub-etapa 4.13 quando o vivo cruzou ~890 linhas e o hook 4.4 passou a alertar consistentemente.
> Camadas cobertas: 0 (Discovery), 1 (Infraestrutura), 2 (Arquitetura).
> Para estado atual do projeto e Camada em andamento, ver `progresso.md`.

**Data de criacao:** 2026-05-11 (Sub-etapa 4.13)

## Mapeamento etapa -> Camada

A numeracao das etapas neste projeto nao corresponde 1:1 a numeracao das Camadas. Para evitar confusao:

| Etapas | Camada |
|---|---|
| 1.1, 1.2, 1.3, 1.4, 1.5 | Camada 1 (Infraestrutura) |
| 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.6.1, 2.6.2, 2.7, 2.8, 2.9 | Camada 1 (extensao e wrap-up) |
| 3.1, 3.2, 3.3, 3.3.1, 3.4, 3.5, 3.6, 3.7, 3.8 | Camada 2 (Arquitetura) |
| 4.0+ | Camada 3 (Claude Code) -- ver `progresso.md` |

---

## Camada 0 — Discovery

**Status:** ✅ Concluída em 2026-05-06

### Critérios de "pronto" (todos atendidos)

- [x] Documento de visão escrito e estável (`docs/visao.md`)
- [x] ADRs fundadores escritos (`docs/adrs.md` — 8 ADRs)
- [x] `decisoes.md` consolidando stack, padrões e convenções
- [x] Repositório criado no GitHub privado (`financas-lab`)
- [x] Clone local em `C:\projetos\financas-lab`
- [x] Pré-requisitos do ambiente validados (Java 21, Maven 3.9, Docker 29, Node 22, Git)
- [x] Ambiente de desenvolvimento decidido (Windows nativo)
- [x] `settings.json` do Claude Code ajustado (Sonnet default, effort medium, modelos atuais)
- [x] Conta API configurada com hard limit $30/mês
- [x] Auditoria de plugins/subagents existentes (limpo, só `frontend-design` e `code-review` oficiais)

### O que foi decidido nesta camada

Resumo executivo (detalhes em `adrs.md`):

- **Backend:** Java 21 + Spring Boot 3 + Maven (ADR-001)
- **Frontend:** Next.js 15 + TypeScript + Tailwind + PWA (ADR-002)
- **Banco:** PostgreSQL 16 em dev e prod, sem H2/SQLite (ADR-003)
- **Arquitetura:** Clean Architecture enxuta com porta para DDD tático on-demand (ADR-004)
- **Auth:** JWT stateless com refresh rotativo (ADR-005)
- **Migrations:** Flyway com SQL puro versionado (ADR-006)
- **Testes:** Três níveis com Testcontainers (ADR-007)
- **Modelo financeiro:** Max 5x até evidência de insuficiência (ADR-008)
- **Ambiente:** Windows nativo + PowerShell + Docker Desktop

### Lições da Camada 0 (anotações para refinar a fábrica)

- Auditoria de configuração existente do Claude Code é etapa não-óbvia mas crítica. Subagents/CLAUDE.md global herdados podem conflitar com decisões do projeto. Vale incluir essa auditoria no playbook quando ele for extraído.
- Decidir ambiente (Windows nativo vs WSL2) **antes** de escrever scripts evita refactor de dezenas de arquivos. Elevar isso a ADR formal num projeto futuro.
- Modelo default Opus + effort high é armadilha financeira fácil de cair. Vale entrar em qualquer playbook de partida como "verificar antes de começar".

---

## Camada 1 — Infraestrutura de confiança

**Status:** ✅ Concluída em 2026-05-08

### Objetivo

Construir a fundação não-negociável da fábrica: testes em três níveis, CI confiável, hooks locais, banco rodando, projeto Spring Boot inicializado, projeto Next.js inicializado. **Zero código de feature nesta camada** — só infraestrutura de validação.

### Critérios de "pronto"

- [x] Repo configurado com `.gitattributes`, `.gitignore`, README inicial
- [x] CLAUDE.md mínimo do projeto criado (apontando para docs)
- [x] Estrutura de pastas inicial criada
- [x] `docker-compose.yml` rodando Postgres 16 + Redis 7
- [x] Scripts PowerShell criados: `setup.ps1`, `dev.ps1`, `test.ps1`, `check.ps1`, `ship.ps1`
- [x] Projeto Spring Boot inicializado via Spring Initializr (manualmente)
- [x] `pom.xml` com todas as dependências da stack
- [x] Flyway configurado, primeira migration criada (schema vazio + tabela de versão)
- [x] Testcontainers configurado e funcional
- [x] Hello-world endpoint passando teste e2e via Testcontainers
- [x] JaCoCo configurado com thresholds (BUNDLE 75%, infrastructure 60%; domain/application/interfaces aguardam Camada 2)
- [x] Checkstyle + SpotBugs configurados
- [x] Projeto Next.js inicializado
- [x] GitHub Actions configurado: lint + test + build em PR
- [x] CI verde no primeiro commit em `main`
- [ ] Pre-commit hook local rodando lint + format
- [x] Branch protection em `main` (sem push direto, exige PR e CI verde)

### Auditoria final (Etapa 2.8)

Critérios de "Camada 1 concluída" definidos em `roadmap-camada-1.md`:

| Critério | Status | Evidência |
|---|---|---|
| Clone novo + `setup.ps1` em <10 min | ✅ | 29 segundos (clone 1.5s + setup ~27s). Margem 20x. Nota: setup passa silenciosamente com `.env` ausente — credenciais vazias. Débito técnico registrado. |
| CI verde no `main` em ≥3 commits consecutivos | ✅ | PRs #26, #25, #24 mergeados com `build: COMPLETED / SUCCESS` |
| Cobertura JaCoCo nos thresholds | ✅ | `mvnw verify` local confirmou. BUNDLE 75%, infrastructure 60% |
| ≥1 PR rejeitado pelo CI por motivo legítimo | ✅ | Etapa 1.2: PR #12 bloqueado por branch protection (teste destrutivo). Etapas 2.4 e 2.5: validações destrutivas confirmaram CI falhando em violações reais |
| Operador confia que CI verde = código mergeable sem segunda revisão | 🟡 Parcial | Sim para código de produção. Scripts/configs ainda precisam de validação manual destrutiva — CI não cobre `.ps1` e `.env`. Confiança expandirá com hooks da Camada 3. |
| `progresso.md` atualizado | ✅ | Esta auditoria + outras seções refletem estado atual |

**Resultado:** Camada 1 CONCLUÍDA.

Ver `docs/retrospectiva-camada-1.md` para reflexão histórica e `docs/hooks-pendentes.md` para lista consolidada de candidatos a hook (input para Camada 3).

### Roadmap detalhado

Ver `docs/roadmap-camada-1.md` para o passo a passo das 2 semanas.

---

## Camada 2 — Arquitetura otimizada para agentes

**Status:** ✅ Concluída em 2026-05-10
**Pré-requisito:** Camada 1 concluída

### Objetivo

Implementar a estrutura de bounded contexts, primeiros agregados/use cases, value objects compartilhados (`Money`), padrões de mapping (MapStruct) e validação. Ainda sem features completas — só o "esqueleto rico" sobre o qual features serão delegadas no Tier 2.

### Critérios de "pronto" (preliminar)

- [ ] Estrutura de pacotes implementada conforme ADR-004
- [x] Value object `Money` implementado e testado
- [x] Bounded context `conta` com domínio puro + use cases + repositório (domain 3.2 ✅, infra+repository 3.3 ✅, use cases+controllers 3.4 ✅)
- [x] Bounded context `categoria` no mesmo padrão
- [x] Bounded context `transacao` ponta a ponta (domain + infra 3.6, application + interfaces 3.7)
- [x] Saldo derivado da Conta cruzando bounded contexts via porta (3.8)
- [x] MapStruct funcionando entre Entity JPA ↔ Domain
- [x] Bean Validation aplicada em DTOs de Request
- [x] Cobertura JaCoCo nos thresholds definidos
- [ ] Spring Security configurado com JWT + refresh rotativo
- [ ] Endpoints de auth funcionando (signup, login, refresh, logout)
- [ ] OpenAPI gerada automaticamente

(Detalhes serão expandidos quando a Camada 1 estiver concluída.)

---

## Lições da Etapa 1.1

### Candidatos a hook (automatizar em etapas futuras)

1. **Linhas em branco em Markdown** — validar que arquivos `.md` modificados têm linhas em branco antes e depois de headers (`##`, `###`). Sem isso, alguns renderers não reconhecem o header.
2. **Encoding UTF-8 em arquivos de texto** — validar que arquivos criados pela fábrica estão em UTF-8.
3. **Conventional Commits** — validar que mensagens de commit seguem o padrão (`feat:`, `fix:`, `chore:`, etc.).
4. **Tamanho de documentos em `docs/`** — alertar se algum `.md` em `docs/` ultrapassa um limite definido (anti-enciclopédia, segundo o princípio "CLAUDE.md curto > CLAUDE.md enciclopédia").

### Lições de ambiente

1. **Tools `Read`/`Write` do Claude Code truncam output do CLI** com marcador "+N lines (ctrl+o to expand)". Sempre validar conteúdo em disco antes de aceitar arquivo criado, não confiar no preview.
2. **PowerShell padrão sem `-Encoding UTF8` lê UTF-8 errado** — mostra `Ã³` no lugar de `ó`, `Ã§` no lugar de `ç`. Para validação confiável de arquivos com acentos, usar `Get-Content -Encoding UTF8` explícito.
3. **`Measure-Object -Line` não conta linhas em branco** — o cmdlet conta apenas linhas com conteúdo. Para contagem real (incluindo vazias), usar `[System.IO.File]::ReadAllLines('<path>').Count`.
4. **Premissas do orquestrador externo podem estar erradas** — validação independente com cálculo concreto resolve. O Claude Code acertou em pushback técnico contradizendo análise visual feita no chat externo. Reforça o princípio: dado concreto vence interpretação.

---

---

## Lições da Etapa 3.8

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

1. **JPQL `SELECT new` com record exige path fully-qualified da classe e dos enums.** `SELECT new TotaisTransacaoPorConta(...)` sem package completo falha em tempo de parsing. Enums também precisam de path completo: `com.laboratorio.financas.transacao.domain.TipoTransacao.RECEITA`. Sem isso, Hibernate não resolve os símbolos no contexto JPQL.
2. **`WHERE t.contaId = :contaId OR t.contaDestinoId = :contaId` + `CASE WHEN ... AND t.contaId = :contaId`** é a combinação necessária para capturar transferências corretamente. O `WHERE` abre para todas as transações em que a conta participa (origem OU destino), e o `AND t.contaId/contaDestinoId` dentro do `CASE` distingue o papel da conta em cada tipo. Sem o `OR` no `WHERE`, transferências recebidas teriam totais sempre zero.
3. **`COALESCE(SUM(...), 0)` é necessário quando o `SUM` pode ser NULL.** `SUM` sobre conjunto vazio retorna NULL no SQL. Sem `COALESCE`, `TotaisTransacaoPorConta` receberia `null` no construtor do record, quebrando qualquer uso downstream.
4. **`mvnw clean verify` obrigatório antes de declarar etapa pronta** (reiterado após incidente da 3.7). Cache de compilação pode mascarar erros. Esta etapa passou em `clean verify` sem atrito, confirmando que o template JPQL prescrito funcionou na primeira tentativa.

---

## Lições da Etapa 3.7

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

1. **PostgreSQL não infere tipo de parâmetro `null` em JPQL quando o campo é `LocalDate`.** A query `(:dataInicio IS NULL OR t.data >= :dataInicio)` gera SQL `(? is null or t.data >= ?)`. Quando `:dataInicio` é null, o PostgreSQL não consegue inferir o tipo SQL do `?` e lança `could not determine data type of parameter $N`. UUID e enum null funcionam por ter representação unívoca. Solução: substituir null por valores sentinela (`LocalDate.of(1900,1,1)` e `LocalDate.of(9999,12,31)`) no repositório e remover o `IS NULL` da query.
2. **`Instant` retornado pelo JPA `save()` tem precisão de nanosegundos (Java) vs. microssegundos (PostgreSQL/banco).** `Instant.now()` retorna `...684277400Z` (9 casas decimais), mas ao recarregar do banco via `buscarPorId`, o mesmo campo retorna `...684277Z` (6 casas decimais). Comparar strings exatas de `criadoEm`/`atualizadoEm` entre `save()` e reload falha. Solução: usar `notNullValue()` em vez de comparação exata quando o teste não precisa verificar o valor preciso.
3. **`@Validated` na classe controller é necessário para ativar `@Min`/`@Max` em parâmetros primitivos (`int`) de query string.** Sem `@Validated`, as anotações são ignoradas silenciosamente — `page=-1` e `size=200` são aceitos. Com `@Validated`, violações lançam `ConstraintViolationException` (não `MethodArgumentNotValidException`), requerendo handler separado no `GlobalExceptionHandler`.
4. **Decisão silenciosa em zona limítrofe (recorrência sexta).** Prompt prescrevia JPQL com `IS NULL OR` uniformemente nos 5 filtros. Agente alterou para sentinelas hardcoded (`LocalDate.of(1900,1,1)`/`9999,12,31`) nos 2 filtros de data sem reportar. Operador detectou em revisão de diff antes do merge. Lição estatística: em todas as etapas da Camada 2, agente tomou pelo menos uma decisão silenciosa em zona limítrofe apesar das instruções explícitas em todos os prompts. A frequência torna inevitável que hooks mecânicos da Camada 3 incluam validação de diff contra padrões prescritos no prompt — não basta repetir "não tomar decisão silenciosa" em prosa. **Caso técnico real escondido pela sentinela:** após restaurar `IS NULL OR` conforme prescrito, CI revelou que Postgres não consegue inferir tipo de parâmetro `LocalDate` em `? IS NULL` — erro `could not determine data type of parameter $N`. Solução final: `COALESCE(:param, coluna)` nos 2 filtros de data, mantendo `IS NULL OR` nos outros 3 (UUID e enum funcionam normalmente). A sentinela original era resposta errada para problema real, não pura gambiarra. Lição complementar: "BUILD SUCCESS local" pode dar falso positivo quando o test runner não pega todos os testes — usar `mvnw clean verify` antes de declarar etapa pronta. Validação efetiva é CI verde, não build local.

---

## Lições da Etapa 3.6

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

1. **Checkstyle `lineWrappingIndentation=8` com `throw new Exception(` aninhado dentro de dois `if` exige 24 espaços de indentação no argumento** (16 do throw + 8 de wrapping), não 20. A diferença é não-óbvia quando se usa 4 espaços por nível e se perde a conta dos aninhamentos. Padrão a observar em código com múltiplos `if` aninhados antes do commit.
2. **`@AttributeOverride` deve ficar em linha única** quando o total de caracteres couber em 140 (limite do Checkstyle do projeto). A alternativa de quebrar `@Column(` em múltiplas linhas requer que os atributos do `@Column` comecem com 12 espaços (4 de campo + 8 de wrapping), não 8.
3. **Nomes de método de teste com underscore são violação de Checkstyle** (`MethodName` aceita apenas `^[a-z][a-zA-Z0-9]*$`). Padrão consolidado desde a Etapa 3.1, mas reapareceu nesta etapa — confirmar antes do primeiro compile.

## Lições da Etapa 3.5

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

(Nenhum novo nesta etapa.)

### Sobre a etapa em si (template replicável validado)

O segundo bounded context (`categoria`) foi implementado em etapa única, contra 4 etapas + 1 fix do `conta`. O template estabelecido por `conta` foi replicado sem zonas limítrofes significativas: nenhuma nova dependência no `pom.xml`, nenhum novo padrão de código, nenhuma decisão silenciosa necessária. As únicas adaptações foram semânticas (`hard delete` vs `soft delete`, filtro por `tipo` vs por `ativa`) — não estruturais. O compile passou com 0 violações Checkstyle na primeira tentativa e `mvnw verify` resultou em BUILD SUCCESS com 170 testes (36 novos), 0 SpotBugs, JaCoCo "All coverage checks have been met". Evidência de que o template é replicável.

## Lições da Etapa 3.4

### Candidatos a hook (automatizar em etapas futuras)

1. **Detectar identificadores inexistentes no codebase introduzidos por commit.** Agente escreveu testes com valores de enum que não existiam (`CREDITO`, `INVESTIMENTO`); o build falhou; o agente corrigiu sozinho sem reportar. Um hook que rode `git diff --staged` e valide que novos identificadores Java referenciam símbolos existentes (via compilação incremental ou grep no source) detectaria isso antes do commit.

### Lições de ambiente

1. **Decisão silenciosa em build error (recorrente).** Testes foram escritos com valores de enum inexistentes (`CREDITO`, `INVESTIMENTO`); build falhou; o agente leu o enum e corrigiu sozinho para os valores reais (`CARTAO_CREDITO`, `DINHEIRO`) sem reportar a discrepância. Mesmo padrão de "decisão silenciosa em zona limítrofe" das etapas anteriores, agora em forma de fix-de-compilação. Recorrência reforça que o padrão dificilmente vai ser eliminado só via prompt — vai precisar de hook mecânico na Camada 3 que detecte `git diff` introduzindo identificadores que não existem no codebase atual antes do commit.
2. **`log` como nome de constante viola Checkstyle `ConstantName`** (padrão `^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$`). Em Java, `private static final Logger` é constante e deve ser nomeada `LOG`. Regra a considerar em geração de código: constantes SLF4J sempre em `SCREAMING_SNAKE_CASE` mesmo sendo convenção do ecossistema usá-las em minúsculas.

---

## Lições da Etapa 3.3.1

### Candidatos a hook (automatizar em etapas futuras)

1. **Validar que scripts que invocam `mvnw spring-boot:run` passam `-Dspring-boot.run.profiles=<profile>` explicitamente.** Hook leve: `grep -nE "mvnw\s+spring-boot:run" scripts/*.ps1 | grep -v "spring-boot.run.profiles"` deve retornar zero linhas. Sem a flag, Spring cai em profile `default` sem datasource.

### Lições de ambiente

1. **Validação destrutiva manual em ambiente real é instrumento de qualidade de primeira linha.** Bug do `dev.ps1` não ativando profile `dev` passou pelo CI verde de toda a Camada 2 porque CI usa profile `test` via `@DynamicPropertySource`. Só apareceu quando o operador tentou subir a aplicação local de fato pós-merge da 3.3 — exatamente o tipo de cenário que CI não cobre. Quarta ocorrência consecutiva do mesmo padrão (2.6.1, 2.6.2, 2.8, 3.3.1).

---

## Lições da Etapa 3.3

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

(Nenhum novo nesta etapa.)

---

## Lições da Etapa 3.2

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

1. **Checkstyle `Indentation` com `lineWrappingIndentation=8` se aplica a parâmetros de construtor multi-linha e a argumentos em chamadas `this(...)`**. Parâmetros do construtor precisam estar em `base + 8` (ex: método em 4 espaços → parâmetros em 12). Argumentos de `this(...)` no corpo de método (em 8 espaços) precisam estar em 16. Getters de uma linha com `{ return x; }` violam `LeftCurly` — devem ser expandidos em três linhas. `if` sem `{}` viola `NeedBraces`. Corrido na tentativa inicial — corrigido antes do primeiro `mvnw verify` verde.

---

## Lições da Etapa 3.1

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

1. **Conflito naming de testes vs Checkstyle detectado em zona limítrofe.** `decisoes.md` prescrevia underscore (`metodoTestado_cenarioDoTeste_resultadoEsperado`), mas Checkstyle do projeto e testes existentes (`HealthcheckControllerTest`, `FlywayMigrationTest`) usam camelCase puro. Agente detectou em zona limítrofe e escalou em vez de tomar decisão silenciosa. Resolução: doc alinhado ao código vivo (camelCase puro). Reforça princípio: quando `decisoes.md` diverge do que está rodando (testes, configs, CI), a verdade canônica é o código vivo, e o doc é o débito a resolver.

---

## Lições da Etapa 2.9

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

(Nenhum novo nesta etapa.)

---

## Lições da Etapa 2.8

### Candidatos a hook (automatizar em etapas futuras)

1. **`setup.ps1` deve detectar `.env` ausente antes de subir containers.** Clone novo sem `.env` resulta em containers com credenciais vazias — Docker Compose interpreta variáveis ausentes como strings vazias e sobe silenciosamente. Hook: validar presença de `.env` antes de chamar Docker Compose, criando a partir de `.env.example` com aviso ou falhando com mensagem clara.

### Lições de ambiente

1. **Validação manual destrutiva de clone novo revelou falha silenciosa em `setup.ps1`.** Sem `.env`, Docker Compose sobe containers com credenciais vazias — setup "conclui com sucesso" porque `mvn -DskipTests` não testa conexão, mas ambiente é inutilizável para dev real. CI nunca teria detectado porque CI tem secrets injetados. Reforça princípio consolidado: validação manual destrutiva de fluxo completo (clone + setup + uso real) detecta classes de bug que automação jamais pega.

---

## Lições da Etapa 2.7

### Candidatos a hook (automatizar em etapas futuras)

1. **`npx shadcn@latest init --defaults` instala componente `button.tsx` automaticamente** além dos artefatos de init (`components.json`, `src/lib/utils.ts`, `globals.css`). Em etapas que proíbem componentes, o hook deve detectar e remover `src/components/ui/*.tsx` gerado pelo init antes do commit.

### Lições de ambiente

1. **Next.js 16 usa Tailwind v4** (não v3). Não há `tailwind.config.ts` separado — configuração via `@import "tailwindcss"` em `globals.css`. shadcn 4.x detecta isso automaticamente e usa `style: base-nova` com CSS variables. Agentes treinados em Next.js 14/15 com Tailwind v3 devem consultar `node_modules/next/dist/docs/` antes de editar estilos.
2. **`shadcn@latest init --defaults` não prompta — escolhe defaults internos (baseColor `neutral`, style `base-nova`).** O prompt original pedia `Slate`; o resultado foi `neutral`. Ambos são neutros aceitáveis. Em etapas futuras que exijam cor específica, passar `--base-color <cor>` explicitamente se o shadcn suportar a flag, ou rodar sem `--defaults` e responder prompts.
3. **`AGENTS.md` e `CLAUDE.md` gerados pelo create-next-app são mecanismo de contexto do Claude Code**, não arquivos decorativos. `CLAUDE.md` com `@AGENTS.md` é sintaxe aditiva que carrega o aviso quando o agente está trabalhando em `frontend/`. Não conflita com `CLAUDE.md` da raiz. Decisão inicial de remover foi corrigida pelo operador — registrar padrão: scaffolds Next.js 16+ incluem esses arquivos intencionalmente.

---

## Lições da Etapa 2.6.2

### Candidatos a hook (automatizar em etapas futuras)

1. Detectar comando nativo (`docker`, `git`, `mvn`, etc) seguido de `if ($LASTEXITCODE -ne 0)` em arquivos `.ps1` **sem** ser precedido por suspensão local de `$ErrorActionPreference`. Indica risco do mesmo bug que esta etapa corrigiu. Hook leve futuro: `grep -B1 -A2 "if (\$LASTEXITCODE" scripts/*.ps1` revisado caso a caso.

### Lições de ambiente

1. **`$ErrorActionPreference = "Stop"` + comando nativo + redirecionamento de stderr é incompatível em PowerShell.** Os operadores de redirecionamento (`2>&1`, `2>$null`, `2>&1 > $null`, `2>&1 | Out-Null`) **não são aplicados antes** do `Stop` interceptar stderr de comando nativo. Resultado: erro vaza pra tela com stack trace do PowerShell. Testado: 3 variantes de redirecionamento falharam. Única solução prática: suspender `Stop` localmente durante a checagem.
2. **Validação manual continua descobrindo bugs que automação não pega.** A 2.6.1 corrigiu um bug parecido (`Write-Error` + `exit` sob `Stop`), e foi descoberta na validação manual destrutiva. A 2.6.2 corrigiu outro bug do mesmo padrão raiz (`Stop` + comando que escreve stderr), descoberto também em validação manual destrutiva (rodar `dev.ps1` com Docker parado). Conclusão reforçada: validação manual destrutiva é instrumento de qualidade de primeira linha, não opcional.
3. **Diagnóstico via teste em terminal direto > inferência.** A solução final foi descoberta por reprodução isolada no terminal (3 tentativas, comparação de comportamento com/sem `Stop`). Sem reprodução isolada, teria ficado tentando variações com `2>&1` infinitamente. Padrão pra debugar comportamento confuso de PowerShell: reproduzir linha-a-linha no terminal direto antes de mexer em script.

---

## Lições da Etapa 2.6.1

### Candidatos a hook (automatizar em etapas futuras)

1. Detectar `Write-Error` seguido de `exit N` em arquivos `.ps1` — combinação que indica bug do mesmo padrão que esta etapa corrigiu. Hook leve: `grep -B0 -A1 "Write-Error" scripts/*.ps1 | grep -A1 "exit"`.

### Lições de ambiente

1. **Validação manual destrutiva pega bugs que validação automática mascara.** A 2.6 passou sintaxe (parser), encoding (sem BOM) e CI (porque CI invoca via subprocess, que traduz exceção terminating em exit 1 corretamente). Mas em uso interativo no PowerShell o `$LASTEXITCODE` ficava 0 falsamente. Conclusão: validação manual no fluxo real do operador descobriu bug que toda automação validou como verde. Validar destrutivamente é não-negociável.
2. **`$ErrorActionPreference = "Stop"` + `Write-Error` + `exit N` é armadilha clássica em PowerShell.** O `Stop` faz `Write-Error` virar exceção terminating, abortando o script antes do `exit N`. Em sessão direta, `$LASTEXITCODE` permanece com o valor do último comando externo que rodou (geralmente 0). Em subprocess (`powershell.exe -File`), o exit traduz pra 1 corretamente. Comportamento inconsistente. Padrão correto registrado em `decisoes.md`.
3. **Subprocess test mascara bug de exit code em PowerShell.** Para validar exit code real de scripts `.ps1`, usar `powershell.exe -Command` rodando o script + captura de `$LASTEXITCODE` na **mesma sessão**. Subprocess via `-File` reporta exit code do processo (que sempre é 1 quando há exceção), não do comportamento da sessão.
4. **Validação destrutiva da branch `== main` não foi exercitada** porque o agente está em feature branch durante o fix. O caminho de erro segue o mesmo padrão `Write-Host` vermelho + `exit 1` dos demais `Write-Error`s do `ship.ps1`, então é coberto pelo precedente da validação 1 (working tree sujo).

---

## Lições da Etapa 2.6

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

1. A tool `Write` do Claude Code cria arquivos com encoding UTF-8 sem BOM por default — confirmado via `xxd` (primeiros bytes `23 20`, não `EF BB BF`). Arquivos com apenas ASCII são reportados como "ASCII text" pelo `file`; arquivos com caractere backtick (`` ` ``) são reportados como "Unicode text, UTF-8 text" mesmo sem BOM. A verificação confiável é via `xxd`, não via `file`.
2. Usar a tool `Write` nativa do Claude Code para criar scripts `.ps1` é mais seguro que passar heredoc via `-Command` do `powershell.exe` no Bash tool — o escape de shell dentro do heredoc pode corromper o conteúdo (blocos duplicados, erros de texto).

---

## Lições da Etapa 2.5

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Licoes de ambiente

1. O `maven-checkstyle-plugin` ao ser configurado com `fileExtensions = java, properties, xml` também tentaria validar arquivos `.xml` e `.properties` do source tree com regras como `NewlineAtEndOfFile lineSeparator=lf`. Para evitar falsos positivos em arquivos que não são código Java, a configuração foi restrita a `fileExtensions = java`. Isso é suficiente para o objetivo da etapa.
2. O `tail` do log do `mvnw verify` é dominado por linhas de progress bar de download do Maven (first run baixa todas as deps do SpotBugs/Checkstyle). Em execuções subsequentes o log é compacto. Lição para futuras etapas: na primeira execução com novos plugins, o log útil fica no final — usar `grep -E` no arquivo completo é mais confiável que `tail`.
3. Indentação de `lineWrappingIndentation = 8` do Checkstyle `Indentation` module aplica-se à primeira linha de continuação após um `=` ou abertura de método multi-linha. Linhas seguintes da mesma expressão (ex: chain `withDatabaseName`, `withUsername`) podem ficar no mesmo nível da primeira continuação sem nova violação — Checkstyle não exige escalada incremental por linha de chain.

---

## Lições da Etapa 2.4

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Licoes de ambiente

1. JaCoCo `check` com `<minimum>` fora do intervalo [0.0, 1.0] gera BUILD FAILURE com mensagem `Rule violated... given minimum ratio is X.XX, but must be between 0.0 and 1.0` — a falha é de configuração inválida, não de cobertura insuficiente. Threshold > 1.0 prova que o gate roda e bloqueia o build, mas não valida a avaliação de cobertura em si.
2. Código de configuração Spring (`@Configuration` com `@Bean`) atinge 100% de cobertura de instrução JaCoCo com qualquer teste que carregue o contexto (`@SpringBootTest`). Todas as instruções do método `securityFilterChain` rodam na criação do bean — não em chamadas subsequentes. Isso significa que testes de comportamento do Security (ex: endpoint bloqueado retornando 401) não contribuem para cobertura JaCoCo do `SecurityConfig`, apenas para cobertura comportamental. Consequência prática: validação destrutiva por remoção de testes de Security não reduz coverage.
3. Uso de `tail` (Unix) em vez de equivalente PowerShell (`Select-Object -Last N`) em ambiente Windows — erro já documentado em `decisoes.md` repetido durante a execução. Candidato a hook que valide comandos Unix em scripts antes de rodar.

---

## Lições da Etapa 2.3

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Licoes de ambiente

1. Spring Security 6 sem `AuthenticationEntryPoint` explícito retorna 403 (não 401) para requisições não autenticadas quando `httpBasic` está desabilitado. Sem entry point configurado, o framework não sabe como desafiar o cliente e cai para 403 (Forbidden). Solução: adicionar `.exceptionHandling(ex -> ex.authenticationEntryPoint(...))` com `response.sendError(401)` explícito. Registrado em `decisoes.md` na seção Spring específico.

---

## Licoes da Etapa 2.2

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa. A regra "baseline-on-migrate apenas em profiles de teste/dev" foi formalizada em decisoes.md mas ja constava como candidato na Etapa 2.1.)

### Licoes de ambiente

1. Sufixo `IT` (convencao Maven Failsafe) nao e reconhecido pelo Surefire padrao do Spring Boot parent — testes `*IT.java` nao rodam sem configurar o Failsafe plugin. O projeto ja usa `*Tests` para testes de integracao via Testcontainers (ex: `FinancasApplicationTests`); seguir esse padrao e mais coerente que introduzir Failsafe nesta etapa. Decisao sobre adotar Failsafe formalmente fica para etapa propria com ADR se necessario.
2. O prompt original prescrevia sufixo `IT` (convencao Maven Failsafe) sem validar que o Failsafe plugin estava configurado no pom.xml — nao estava. Isso causou tentativa inicial de adicionar Failsafe ao pom.xml fora do escopo declarado, bloqueada pela Restricao 1 do prompt. Licao para geracao de prompts futuros: validar convencoes de teste do projeto (pom.xml, Surefire/Failsafe configurados, padrao de naming dos testes existentes) antes de prescrever sufixo de classe em prompt.

---

## Lições da Etapa 2.1

### Candidatos a hook (automatizar em etapas futuras)

1. Validar que classes base de teste em `src/test/java/.../shared/` (ou pacote equivalente) que servem de superclasse para outras classes de teste tenham modificador `abstract`. Sem `abstract`, JUnit tenta instanciar a classe base e roda o lifecycle dela além das filhas, gerando execução duplicada e potencial inicialização de container fora de hora.
2. Validar que `application-test.yml` (e qualquer profile de teste) não declare URL JDBC hardcoded quando o projeto usa Testcontainers via `@DynamicPropertySource`. Hardcoding anula o ponto da injeção dinâmica e faz o teste rodar contra banco que não é o do container.
3. Validar que `baseline-on-migrate: true` apareça **apenas** em profiles de teste (e potencialmente `dev`), nunca em `application.yml` (defaults) ou `application-prod.yml`. Em produção, baseline silencioso de schema desconhecido é fonte clássica de inconsistência.

### Lições de ambiente

1. Cold-start de CI com Testcontainers em runner do GitHub Actions (`ubuntu-latest`) foi de 33s no PR #17, bem abaixo dos 80-120s previstos no plano da etapa. O cache de camada do `postgres:16-alpine` no runner é mais agressivo que o estimado. Calibrar previsões futuras para 30-90s, com 120s como teto pessimista — não como expectativa.
2. `spring.jpa.hibernate.ddl-auto: validate` com schema vazio (sem migrations ainda) só funciona porque também não existem classes `@Entity` no projeto. Quando as primeiras entidades JPA aparecerem na Camada 2, validar contra schema vazio vai quebrar a inicialização do Spring. Exige migration `V1` não-vazia ou migrations escritas em paralelo às entidades — não depois. Já cobre a Etapa 2.2 (primeira migration Flyway).

---

## Lições da Etapa 1.5

### Candidatos a hook (automatizar em etapas futuras)

1. Validar que mvnw tem bit de execução no git index antes de empurrar workflow Linux que o use. Comando: git update-index --chmod=+x mvnw. Sem isso, primeiro CI no Linux falha com Permission denied.
2. Validar que comandos em scripts/instruções para Windows não usam ferramentas Unix (tail, head, grep, sed, awk) que não existem no PowerShell. Equivalentes: Select-Object -Last/-First, Select-String.
3. Validar que arquivos .java criados via Out-File no PowerShell usam encoding sem BOM. Out-File -Encoding UTF8 adiciona BOM por default, e javac rejeita arquivos com BOM no início. Alternativas: [System.IO.File]::WriteAllText com UTF8Encoding(false), ou Set-Content -Encoding utf8NoBOM no PowerShell 7+.
4. Validar que toda configuração de branch protection ou required check passou por teste destrutivo (PR proposital com CI falhando, confirmar bloqueio do merge) antes de ser declarada concluída.

### Lições de ambiente

1. Maven Wrapper gerado no Windows não tem bit de execução no git por default. Linux Ubuntu (GitHub Actions) precisa do bit para executar ./mvnw, e o erro é Permission denied (exit code 126). Solução estrutural: git update-index --chmod=+x mvnw. O bit fica registrado no git index, não afeta o arquivo no Windows mas Linux passa a respeitar.
2. Distinção entre flags do Maven Surefire: -DfailIfNoTests=false ignora projeto sem nenhum teste; -Dsurefire.failIfNoSpecifiedTests=false ignora pattern especificado que não casa com nenhum teste. Não são intercambiáveis.
3. actions/upload-artifact@v4 com if-no-files-found tem default warn (não fail). Diretório vazio não quebra o CI por default, mas explicitar o parâmetro torna o comportamento auto-documentado.
4. Cache Maven via cache: maven do actions/setup-java@v4 é suficiente. Não combinar com actions/cache@v4 separado para Maven.
5. Squash merge preserva mudanças de file mode (chmod +x). Confirmação observada: PR #11 mergeou com "mode change 100644 => 100755" preservado em main.
6. Out-File -Encoding UTF8 no PowerShell adiciona BOM no arquivo. javac rejeita arquivos .java com BOM (illegal character: '﻿'). Mesma família de problema do Get-Content -Encoding UTF8 (lição da Etapa 1.4): PowerShell trata UTF-8 com BOM por default em ambas direções.
7. gh pr merge tem flag --admin para bypass de proteções. Comando deve ser tratado como destrutivo e nunca usado em fluxo normal. Branch protection + required check confirmados funcionais por teste destrutivo (PR #12 bloqueado com erro "the base branch policy prohibits the merge").

---

## Lições da Etapa 1.4

### Candidatos a hook (automatizar em etapas futuras)

1. Validar que pesquisa de versões em pom.xml consultou Maven Central / BOM Spring antes de fixar (não memória do agente). Versões podem estar desatualizadas no conhecimento do agente.
2. Validar que `<release>` está sendo usado em vez de `<source>` + `<target>` no maven-compiler-plugin (idiomático desde Java 9).
3. Validar ordem "Lombok antes de MapStruct" em `<annotationProcessorPaths>` do maven-compiler-plugin.
4. Validar que agente NÃO sugere "próxima etapa" espontaneamente após abrir PR. Cada etapa tem fim explícito (PR mergeado + progresso.md atualizado + sync local) antes da próxima.

### Lições de ambiente

1. `python3` não existe no PATH do Windows nativo — o binário se chama `python`. Lição reapareceu durante a sessão (primeira vez no início, segunda vez no curl /v3/api-docs). Persistência de lições entre comandos da mesma sessão é necessária.
2. Spring Security em classpath protege todos os endpoints sem `SecurityFilterChain` customizado. `/v3/api-docs` retorna 401 (não 404) — confirma que springdoc registrou o endpoint mas Spring Security está bloqueando. Será resolvido quando `SecurityFilterChain` for configurado na Camada 2.
3. `<source>` + `<target>` no maven-compiler-plugin não é equivalente a `<release>` desde Java 9. `<release>` garante que apenas APIs públicas da versão alvo são usadas, evitando uso acidental de APIs internas do JDK atual.
4. BOM do Spring Boot 3.5.14 fixa Testcontainers em 1.21.4 mesmo com 2.0.5 disponível no Maven Central. Usar versão do BOM evita incompatibilidades.
5. `mvn -N wrapper:wrapper -Dmaven=X` gera wrapper na versão pedida, mas a versão default do plugin pode estar desalinhada da Maven local. Wrapper foi gerado em 3.9.9 (Maven local: 3.9.15) — recomendado alinhar manualmente em `maven-wrapper.properties` em etapa futura.
6. Spring Boot 3.x mostra warning de "spring.jpa.open-in-view is enabled by default" mesmo configurando `false` explicitamente. É bug conhecido do Spring Boot, não regressão da config. Ignorar.
7. `-Amapstruct.defaultComponentModel=spring` no `<compilerArgs>` gera warning "options were not recognized by any processor" quando não há classe `@Mapper` no projeto. Investigar na Camada 2 quando primeiro mapper for criado: pode ser necessário usar `@Mapper(componentModel = "spring")` direto em cada classe se o argumento global não estiver sendo passado corretamente.

---

## Lições da Etapa 1.3

### Candidatos a hook (automatizar em etapas futuras)

1. Validar que docker-compose.yml não declara campo "version:" (obsoleto em Compose v2).
2. Validar que imagens em docker-compose.yml estão fixadas em major.minor (não :latest).
3. Validar que portas em docker-compose.yml estão expostas em 127.0.0.1, não 0.0.0.0.

### Lições de ambiente

1. Sandbox de bash do Claude Code bloqueia "sleep N && comando" para evitar uso impróprio. Solução idiomática é usar polling com `until <check>; do sleep N; done`.
2. PowerShell padrão sem -Encoding UTF8 lê arquivos UTF-8 incorretamente, mas o conteúdo em disco continua íntegro. Confirmação obrigatória: `Get-Content arquivo -Encoding UTF8`.
3. Healthcheck do Redis com senha precisa receber -a ${REDIS_PASSWORD} explicitamente. Sem isso, healthcheck falha silenciosamente e container fica unhealthy indefinidamente.

---

## Lições da Etapa 1.2

### Candidatos a hook (automatizar em etapas futuras)

1. Validar antes de iniciar Tier 2 que branch protection da default branch está ativa e enforcing — não apenas configurada na interface.
2. Validar que o usuário não está na lista de bypass do ruleset, mesmo sendo admin.

### Lições de ambiente

1. **GitHub Free + repo privado não suporta branch protection.** Tanto "Branch protection rules" (clássica) quanto "Repository rulesets" (nova) ficam configuradas mas marcadas como "Not enforced" — interface permite criar e dá impressão de que está protegido, mas não bloqueia. Solução: tornar repo público (Free) ou migrar pra GitHub Team (pago). Decisão tomada: público.
2. **Validação obrigatória de branch protection é não-negociável.** O passo de testar push direto e ver erro deve sempre ser executado. Sem isso, branch protection pode estar inativa por meses sem ninguém notar.
3. **Repo tornado público não tem custo se nenhum dado sensível está versionado.** ADR-005 (auth) e práticas de .env já garantem isso. Ganho colateral: GitHub Actions ilimitado em repo público, alinhado com o pulo de posicionamento profissional do blueprint.

---

