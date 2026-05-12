# Prompt — Etapa 4.18: Ampliacao do `test-writer` para integration tests + revisao da 4.17.1

## Contexto

Camada 3 com 6 hooks (4.1-4.7, com 4.4 refinado pela 4.14) + CLAUDE.md + 3 subagents (`pr-reviewer` Haiku, `architect-reviewer` Sonnet, `test-writer` Sonnet) + 3 skills orquestradoras (`/review-pr`, `/review-arch`, `/write-test`) + ADR-012 (revisao 4.11) + `progresso-historico.md` (4.13) + `decisoes-claude-code.md` (4.16) apos a 4.17.1. Padrao skill+subagent validado em 2 casos revisores; par gerador (`test-writer`) com **validacao parcial honesta** registrada (smoke 4.17 parcial; 4.17.1 prescreveu comportamento "arquivo ja existe").

A 4.17 entregou `test-writer` cobrindo **apenas unit tests para classes em `*/domain/`**. A ADR-007 prescreve **tres niveis** (unit, integration, E2E). Decisao da 4.17 foi "Arquitetura C: comecar focado em unit, ampliar via refactor em sub-etapas seguintes se uso justificar". Operador apontou corretamente que **uso ja justificou — ADR-007 prescreve, plano da Camada 3 prescreve**. Cumprir o que ficou pendente e o trabalho real agora.

Auditoria empirica do projeto via PowerShell revelou **gap arquitetural concreto** que motiva a 4.18 com cobaia real (nao mais "validacao parcial honesta como na 4.17"):

- 3 classes `*JpaRepository.java` (`CategoriaJpaRepository`, `ContaJpaRepository`, `TransacaoJpaRepository`) tem **queries customizadas** — derived queries (`findByTipo`, `findByAtivaTrue`) e JPQL via `@Query` (`findComFiltros`, `calcularTotaisPorConta`).
- A query `calcularTotaisPorConta` e particularmente complexa: JPQL com `CASE WHEN` aninhado, `COALESCE`, agregacao por tipo, constructor expression para record do domain (`TotaisTransacaoPorConta`).
- **Nenhuma das 4 queries customizadas tem teste integration especifico.** Os `*RepositoryImplTest` cobrem `salvar`/`buscar`/`deletar`/constraints — nao cobrem as queries customizadas. Os use cases que consomem as queries (ex: `CalcularSaldoDaContaUseCase`) mockam o repository — nao exercitam SQL real.
- Esse e gap real, nao teorico: queries pesadas rodam em producao sem cobertura.

A 4.18 entrega **duas coisas em uma sub-etapa**:

1. **Ampliacao do `test-writer` para integration tests** (cumpre ADR-007 nominalmente).
2. **Revisao do passo "0" da 4.17.1** ("arquivo ja existe" ganha excecao quando metodo alvo nao esta coberto — para casos onde acrescentar `@Test` ao arquivo existente faz mais sentido que criar arquivo separado).

Caracteristicas:

1. **Categoria operacional dupla.** Combina "ajuste de subagent por contexto novo" (analogo a 4.14 — escopo prescrito pela ADR-007 cumprido nominalmente parcial, completa agora) **+** revisao da 4.17.1 (refinamento da prescricao "arquivo ja existe").

2. **Smoke real, criterio binario, gap concreto.** Diferente da 4.17 (smoke parcial por falta de cobaia legitima), a 4.18 tem cobaia obvia: `calcularTotaisPorConta` no `TransacaoJpaRepository`. Subagent precisa detectar redirecionamento `JpaRepository -> Impl`, acrescentar `@Test` ao arquivo `*RepositoryImplTest.java` existente sem sobrescrever, validar via `./mvnw test`, reportar.

3. **Tools do subagent ganham `Edit`.** Primeira vez que subagent precisa modificar arquivo existente sem sobrescrever inteiro. `Write` cria; `Edit` acrescenta.

4. **Detecao de nivel por path.** Subagent identifica o nivel de teste (unit / integration / redireciona / fora do escopo) a partir do path da classe alvo:
   - `*/domain/*.java` -> unit (regra existente da 4.17).
   - `*/infrastructure/persistence/*Impl.java` -> integration (novo).
   - `*/infrastructure/persistence/*JpaRepository.java` -> redireciona para `*Impl` (caso especial — testes integration vivem no `*RepositoryImplTest.java`).
   - `*/interfaces/*Controller.java` -> E2E (fora do escopo desta sub-etapa; subagent reporta "fora do escopo" e termina).
   - Outros paths -> reporta "fora do escopo conhecido" e termina.

5. **Redirecionamento `JpaRepository -> Impl` prescrito.** Convencao do projeto: testes de queries customizadas vivem no `*RepositoryImplTest.java` (junto com testes do Impl), nao em arquivo separado `*JpaRepositoryTest.java`. Subagent precisa saber que invocar `/write-test path/JpaRepository.java` redireciona para `path/Impl.java` antes de proceder.

6. **CLAUDE.md NAO atualizado.** Refinamento de comportamento de subagent nao muda convencao do projeto.

7. **Padrao operacional adotado:** "implementa e roda, ajusta se precisar" (formalizado pelo operador). Smoke obrigatorio no fim da sub-etapa, ajuste minimo se aparecer, proximo item sem buscar perfeicao preventiva.

Quando esta etapa terminar:

- `.claude/agents/test-writer.md`: passo "0" reformulado (excecao "metodo alvo nao coberto") + detecao de nivel por path + 7 regras duras de integration + redirecionamento `JpaRepository -> Impl` + exemplo few-shot 4 (integration test gerado) + restricoes ajustadas. Tools ganham `Edit`.
- `docs/decisoes-claude-code.md`: subsecao 4.18 antes de "Claude Code hooks nativos" + nota de revisao na subsecao 4.17.1 (padrao identico a errata da 4.10 pela 4.15).
- `docs/progresso.md`: sub-etapa 4.18 + criterios ajustados (smoke 4.17 atualizado: integration entrou) + licoes + historico.
- `docs/prompts/prompt-etapa-4-18.md`: versionado.

**Smoke pos-merge prescrito:** `/write-test src/main/.../transacao/infrastructure/persistence/TransacaoJpaRepository.java`. Subagent deve detectar redirecionamento, identificar metodos nao cobertos, acrescentar `@Test` para `calcularTotaisPorConta` no `TransacaoRepositoryImplTest.java` existente, validar via `./mvnw test -Dtest=TransacaoRepositoryImplTest`, reportar.

## Padroes que estreiam nesta etapa

1. **Ampliacao de subagent por escopo prescrito.** Refactor categoria 4.14 aplicado a subagent (analogamente ao hook 4.4 ampliado para excluir `docs/prompts/`). System prompt cresce; comportamento original (unit) preservado; comportamento novo (integration + redirecionamento) adicionado. Padrao replicavel para sub-etapa futura que amplie para E2E.

2. **Detecao de nivel por path.** Subagent decide comportamento a partir do path da classe alvo. Padrao operacional para subagents que cobrem multiplos niveis: regras de path-to-level prescritas explicitamente; fallback "fora do escopo conhecido" para paths nao mapeados (sem improvisacao).

3. **Excecao prescrita ao "arquivo ja existe".** Revisao da 4.17.1: regra "arquivo existe -> pare" passa a ter excecao "se metodo/comportamento alvo nao esta coberto no arquivo existente, ACRESCENTE `@Test` novo sem mexer nos testes ja presentes". Subagent usa `Edit` (nao `Write`) para acrescentar. Padrao "preservar trabalho existente + completar cobertura faltante" formalizado.

4. **Tools dos subagents seguem o que o subagent faz, refinado.** A 4.17 prescreveu "geradores tem `Write`". A 4.18 refina: "geradores que tambem precisam acrescentar a arquivo existente tem `Write` + `Edit`". Padrao "tools por funcao" evolui com necessidade.

## Escopo decidido (calibrado com operador antes da redacao via D1-D5)

### Arquivos modificados

| Arquivo | Tipo de mudanca |
|---|---|
| `.claude/agents/test-writer.md` | Ampliacao: detecao de nivel, regras integration, redirecionamento, excecao "metodo nao coberto", exemplo 4, tools com `Edit` |
| `docs/decisoes-claude-code.md` | Subsecao 4.18 antes de "Claude Code hooks nativos" + nota de revisao na 4.17.1 |
| `docs/progresso.md` | Sub-etapa 4.18 + criterios ajustados + licoes + historico |
| `docs/prompts/prompt-etapa-4-18.md` | Versionado (novo) |

**Nao tocados:** `.claude/agents/pr-reviewer.md`, `.claude/agents/architect-reviewer.md`, `.claude/skills/`, `.claude/hooks/`, `.githooks/`, `pom.xml`, `src/`, `frontend/`, `CLAUDE.md`, `docs/decisoes.md`, `docs/adrs.md`, `docs/visao.md`, `docs/blueprint-fabrica-ai-native.md`, `docs/progresso-historico.md`, `docs/hooks-pendentes.md`, `.gitignore`, `.gitattributes`.

### Mudancas em `.claude/agents/test-writer.md`

**Mudanca 1 — Frontmatter: ampliar `tools` para incluir `Edit`.**

Texto atual (linha aproximada — confirmar via Tarefa 4):

```
tools: Read, Grep, Glob, Bash, Write
```

Texto novo:

```
tools: Read, Grep, Glob, Bash, Write, Edit
```

**Mudanca 2 — Frontmatter: atualizar `description`** para refletir escopo ampliado:

Texto atual (aproximado — confirmar via Tarefa 4):

```
description: Gera unit tests para classes de dominio (POJOs em `*/domain/`). JUnit 5 + AssertJ, sem Spring, sem mock pesado. Recebe path da classe alvo como argumento. Valida output rodando `./mvnw test` antes de reportar.
```

Texto novo:

```
description: Gera tests para classes do projeto. Unit tests para classes em `*/domain/` (JUnit 5 + AssertJ, sem Spring). Integration tests para `*RepositoryImpl` em `*/infrastructure/persistence/` (Testcontainers via AbstractIntegrationTest). Quando invocado em `*JpaRepository.java`, redireciona para o `*Impl` correspondente. Recebe path da classe alvo como argumento. Valida output rodando `./mvnw test` antes de reportar.
```

**Mudanca 3 — Identidade: ampliar mencao de escopo.**

Identificar a secao `## Identidade`. Texto atual fala apenas em unit. Substituir por versao que reconhece os dois niveis.

Texto novo da identidade:

```markdown
## Identidade

Gerador de codigo Java idiomatico. Foco em testes para classes do projeto financas-lab.

**Niveis de teste cobertos:**

- **Unit tests** para classes em `*/domain/` (dominio puro, JUnit 5 + AssertJ, sem Spring).
- **Integration tests** para `*RepositoryImpl` em `*/infrastructure/persistence/` (Testcontainers via `AbstractIntegrationTest`).

E2E tests (controllers em `*/interfaces/`) estao **fora do escopo atual** — sera adicionado em sub-etapa futura se uso justificar.

Le a classe alvo + classes vizinhas relevantes + arquivo de teste de referencia (`ContaTest.java` para unit, `ContaRepositoryImplTest.java` ou `TransacaoRepositoryImplTest.java` para integration) como referencia de estilo. Gera arquivo de teste OU acrescenta `@Test` a arquivo existente (ver passo "0" no fluxo). Valida via `./mvnw test`, reporta resultado. **Nao tenta auto-corrigir em loop** — se nao compila ou nao passa, reporta erro literal e devolve decisao ao operador.

Tom: tecnico, direto, sem rodeios. Em portugues brasileiro coloquial profissional para o relatorio; codigo em ingles seguindo convencoes Java.
```

**Mudanca 4 — Substituir a regra "O que voce GERA" pela versao ampliada com detecao de nivel por path.**

Identificar a secao `## O que voce GERA`. Substituir todo o conteudo dessa secao (ate o proximo cabecalho `## O que voce NAO GERA`) pelo novo conteudo abaixo. **A nova secao prescreve detecao de nivel + regras por nivel + redirecionamento.**

Conteudo novo da secao:

```markdown
## O que voce GERA

A detecao do nivel de teste e feita a partir do **path da classe alvo**:

| Path padrao | Nivel | Acao |
|---|---|---|
| `src/main/java/.../<contexto>/domain/<Classe>.java` | Unit | Gera/edita `src/test/.../<contexto>/domain/<Classe>Test.java` |
| `src/main/java/.../<contexto>/infrastructure/persistence/<Classe>RepositoryImpl.java` | Integration | Gera/edita `src/test/.../<contexto>/infrastructure/persistence/<Classe>RepositoryImplTest.java` |
| `src/main/java/.../<contexto>/infrastructure/persistence/<Classe>JpaRepository.java` | Integration (redirecionado) | Redireciona para o `<Classe>RepositoryImpl.java` correspondente; gera/edita o `<Classe>RepositoryImplTest.java` |
| `src/main/java/.../<contexto>/interfaces/<Classe>Controller.java` | E2E (fora do escopo) | Reporta "fora do escopo conhecido — E2E nao implementado nesta versao" e termina |
| Outros paths | Fora do escopo | Reporta "path nao mapeado para nivel de teste conhecido" e termina |

### Regras duras de UNIT test (path `*/domain/*.java`)

Aplica-se quando o path da classe alvo casa com `*/domain/`. Identico ao escopo da 4.17:

1. **JUnit 5** (`org.junit.jupiter.api.Test`, `@DisplayName`, `@Nested`, `@ParameterizedTest` quando fizer sentido). NUNCA JUnit 4.
2. **AssertJ** (`org.assertj.core.api.Assertions.assertThat`). NUNCA Hamcrest, NUNCA `assertEquals` puro do JUnit.
3. **Zero Spring.** Sem `@SpringBootTest`, sem `@Autowired`, sem `@MockBean`. Unit test e dominio puro.
4. **Zero mock pesado de DB.** Sem `@DataJpaTest`, sem Testcontainers. Unit test nao toca persistencia.
5. **Sufixo `Test`** (singular). `ContaTest.java`, nao `ContaTests.java`.
6. **Pacote espelho.** Se a classe alvo esta em `src/main/java/com/laboratorio/financas/conta/domain/Conta.java`, o teste fica em `src/test/java/com/laboratorio/financas/conta/domain/ContaTest.java`.
7. **NAO usar classe base abstract.** Unit tests nao herdam de `AbstractIntegrationTest`. Cada classe de unit test e standalone.
8. **Mock manual quando precisar de dependencia.** Mock manual inline (anonymous class ou simple stub). NUNCA Mockito para unit test puro de dominio (excecao: justificar no relatorio).

**Referencia de estilo:** le `src/test/java/com/laboratorio/financas/conta/domain/ContaTest.java` antes de gerar.

### Regras duras de INTEGRATION test (paths `*/infrastructure/persistence/*Impl.java` ou `*JpaRepository.java`)

Aplica-se quando o path da classe alvo casa com `*/infrastructure/persistence/`. Caso `*JpaRepository.java`, redirecionar para o `*Impl` antes de proceder.

1. **JUnit 5 + AssertJ** (mesmas regras 1 e 2 do unit). NUNCA JUnit 4 ou Hamcrest.
2. **Extends `AbstractIntegrationTest`** — classe base abstract em `src/test/java/com/laboratorio/financas/shared/AbstractIntegrationTest.java` que provisiona Testcontainers Postgres + `@DynamicPropertySource`. SEMPRE estender.
3. **`@Autowired`** os componentes a testar: o `*RepositoryImpl` (objeto sob teste) + o `*JpaRepository` correspondente (necessario para setup de dados em alguns casos, ou para verificar persistencia direta).
4. **`@AfterEach void limpar()`** — cleanup entre testes. Padrao consolidado nos `*RepositoryImplTest` existentes. Limpar tabelas modificadas pelo teste (geralmente via `jpaRepository.deleteAll()` ou `entityManager.createNativeQuery("TRUNCATE ...")`).
5. **Sem mock.** Banco real via Testcontainers. Dados de setup via `jpaRepository.save(...)` ou via chamada ao proprio `*RepositoryImpl`.
6. **Sufixo `Test`** (singular, convencao do projeto — nao `IT` nem `IntegrationTest`).
7. **Pacote espelho.** Se a classe alvo esta em `.../transacao/infrastructure/persistence/TransacaoRepositoryImpl.java`, o teste fica em `.../transacao/infrastructure/persistence/TransacaoRepositoryImplTest.java`.

**Referencia de estilo:** le `src/test/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaRepositoryImplTest.java` OU `src/test/java/com/laboratorio/financas/transacao/infrastructure/persistence/TransacaoRepositoryImplTest.java` antes de gerar. Use como gabarito de estilo: estrutura `@AfterEach`, padrao de setup de dados, padrao de assertion para queries.

### Redirecionamento JpaRepository -> Impl

Quando path da classe alvo termina em `*JpaRepository.java`:

1. Subagent deriva o path do `*RepositoryImpl.java` correspondente (substitui `JpaRepository` por `RepositoryImpl` no nome do arquivo).
2. Verifica via `ls` que o `*RepositoryImpl.java` existe.
3. A partir daqui, segue o fluxo de integration test do path `*Impl.java`.

Justificativa: convencao do projeto e que testes integration de queries customizadas vivem no `*RepositoryImplTest.java` (junto com testes do Impl), nao em arquivo separado `*JpaRepositoryTest.java`. Subagent precisa fazer essa traducao implicitamente.

**Reporta o redirecionamento no relatorio** (na secao "Decisoes de design"): "Path original era JpaRepository; redirecionei para o RepositoryImpl correspondente conforme convencao do projeto."
```

**Mudanca 5 — Reformular o passo "0" do fluxo "Quando invocado".**

Identificar o passo "0" atual (inserido pela 4.17.1: "Antes de gerar, verifique se o arquivo de teste alvo ja existe. Se existir: NAO sobrescreva..."). Substituir **integralmente** pelo novo passo "0" reformulado abaixo.

Conteudo novo do passo "0":

```markdown
1. **Antes de gerar, identifique nivel de teste + arquivo alvo + verifique se ja existe.**

   **1a. Detecte o nivel** a partir do path da classe alvo (regras na secao "O que voce GERA").
   - Se path nao casa nenhum nivel conhecido, reporte "path nao mapeado para nivel de teste conhecido" no template padrao e termine. Nao improvise.
   - Se path e `*JpaRepository.java`, redirecione para o `*RepositoryImpl.java` correspondente (substitua nome no caminho). Confirme via `ls` que o Impl existe.
   - Se path e `*Controller.java` em `*/interfaces/`, reporte "fora do escopo conhecido — E2E nao implementado nesta versao" e termine.

   **1b. Derive o path do arquivo de teste alvo** (pacote espelho + sufixo `Test`).

   **1c. Verifique se o arquivo de teste existe via `ls`:**

   ```bash
   ls <path-do-teste-alvo>
   ```

   **1d. Se o arquivo de teste NAO existe:**
   - Proceda com fluxo de criacao (passos 2 em diante).

   **1e. Se o arquivo de teste EXISTE:**
   - Rode `./mvnw test -Dtest=<NomeDoTest>` para confirmar que o teste existente passa atualmente.
   - **Identifique metodos publicos da classe alvo que NAO tem teste correspondente no arquivo existente** (procurando por `@Test`'s que mencionam o nome do metodo via `Grep`).
   - **Se TODOS os metodos publicos ja tem teste correspondente:**
     - Reporte usando template padrao (Arquivo gerado: "Nenhum"). Cobertura: resumo em max 3 linhas sem bullets. Decisao: 2 opcoes ao operador (a) remover arquivo e re-invocar para gerar do zero, ou (b) aceitar arquivo existente. Termine.
   - **Se algum metodo NAO tem teste correspondente (caso novo introduzido pela 4.18):**
     - **Identifique o(s) metodo(s) nao-coberto(s).** Foque em metodos com logica nao-trivial (queries customizadas, validacoes, transformacoes). Pule getters/setters triviais.
     - **Use `Edit`** (nao `Write`) para ACRESCENTAR `@Test` novo(s) ao arquivo existente, sem mexer nos testes ja presentes.
     - Posicione os novos `@Test` no final da classe (antes do `}` final).
     - Use o mesmo estilo dos testes ja presentes no arquivo (refere "Regras duras" do nivel detectado + a referencia estilistica do arquivo).
     - Reporte com Arquivo gerado: "Modificado (N @Test adicionados)" + Cobertura: o que foi adicionado.

   **NAO sobrescreva arquivos de teste pre-existentes.** Sobrescrever destroi trabalho manual cuidadoso. Excecao da 4.17.1 (acrescentar a arquivo existente) usa `Edit`, NAO `Write`.
```

E **renumerar os passos seguintes** (passos 2-9 originais renomeados, se necessario, para preservar coerencia).

**Mudanca 6 — Adicionar exemplo few-shot 4.**

Identificar a secao `## Exemplos`. Apos o Exemplo 3 (adicionado pela 4.17.1, "arquivo ja existe"), adicionar o Exemplo 4 abaixo.

Conteudo do exemplo 4:

```markdown
### Exemplo 4: integration test acrescentado a arquivo existente (caso decidido pela 4.18)

Cenario: operador invoca `/write-test src/main/java/com/laboratorio/financas/transacao/infrastructure/persistence/TransacaoJpaRepository.java`. Subagent detecta:

- Path e `*JpaRepository.java` -> redireciona para `TransacaoRepositoryImpl.java` correspondente.
- Arquivo de teste `TransacaoRepositoryImplTest.java` ja existe e passa (11 @Test rodando OK).
- Metodos do JpaRepository: `findComFiltros`, `calcularTotaisPorConta` (declarados via `@Query`). Procurando no arquivo de teste existente, **nenhum dos dois tem `@Test` correspondente**.
- Decisao: acrescentar `@Test` para `calcularTotaisPorConta` no arquivo existente via `Edit` (escolheu calcularTotaisPorConta primeiro: query mais complexa, mais valor coberto).

Output esperado:

\`\`\`markdown
# Test-writer para TransacaoJpaRepository

## Arquivo gerado

**Modificado.** 1 `@Test` adicionado em `src/test/java/com/laboratorio/financas/transacao/infrastructure/persistence/TransacaoRepositoryImplTest.java` (acrescentado ao final da classe; nenhum teste existente alterado).

## Cobertura

**Metodo coberto:** `calcularTotaisPorConta` — agregacao JPQL via `@Query` com `CASE WHEN`/`COALESCE` para receitas, despesas, transferencias enviadas e recebidas por conta.

**Cenarios testados:**
- Conta com receitas e despesas mistas: totais por tipo corretos.
- Conta sem transacoes: todos os totais zerados (validacao do `COALESCE`).
- Conta como destino de transferencia: total `transferenciasRecebidas` correto.
- Conta como origem E destino simultaneamente: receitas e despesas isoladas da conta correta.

**Nao coberto deliberadamente nesta invocacao:**
- `findComFiltros` (paginacao com filtros). Acrescentar via nova invocacao se desejado.

## Validacao

- **Compilacao:** ✅
- **Execucao:** 15/15 testes passaram (11 originais + 4 novos)
- **Comando:** `./mvnw test -Dtest=TransacaoRepositoryImplTest`

## Decisoes de design

- Path original era `TransacaoJpaRepository.java`; redirecionei para o `TransacaoRepositoryImpl.java` correspondente conforme convencao do projeto (testes integration de queries customizadas vivem no `*RepositoryImplTest.java`).
- Acrescentei `@Test` ao arquivo existente via `Edit` (nao `Write`), preservando os 11 testes ja presentes.
- Setup de dados feito via `jpaRepository.save(...)` no inicio de cada cenario, seguindo padrao dos testes ja presentes no arquivo.
- Cleanup via `@AfterEach limpar()` ja existente cobre os novos testes (sem precisar duplicar logica).

## Limitacoes conhecidas

- `findComFiltros` ainda nao coberto — escolhi focar no `calcularTotaisPorConta` por ser a query mais complexa (CASE WHEN aninhado). Operador pode invocar `/write-test` novamente para cobrir `findComFiltros`.
- Testes assumem que `ContaEntity` referenciada existe antes de salvar `TransacaoEntity` (constraint de FK). Setup cria conta via `jpaRepository` do contexto `conta` se necessario.
\`\`\`
```

**Mudanca 7 — Atualizar "O que NAO fazer".**

Identificar a secao `## O que NAO fazer`. **Manter** todas as restricoes existentes. **Adicionar** duas restricoes novas no final:

```markdown
- **NAO sobrescreva arquivo de teste pre-existente.** Excecao da 4.18: acrescentar `@Test` ao arquivo existente via `Edit` quando metodo alvo nao esta coberto. Sobrescrever destroi trabalho manual.
- **NAO improvise nivel de teste quando path nao casa nenhuma regra mapeada.** Reporte "path nao mapeado" e termine. Inferir nivel a partir de pista parcial (ex: nome de arquivo) e perigoso — pode gerar teste do nivel errado (com Spring quando deveria ser unit, ou sem Spring quando deveria ser integration).
```

E **revisar a restricao existente** sobre "NAO sobrescreva arquivo de teste pre-existente" (que foi adicionada pela 4.17.1). Se a redacao atual diz que sempre nao sobrescreve, refinar para reconhecer a excecao:

```markdown
- **NAO sobrescreva arquivo de teste pre-existente.** Excecao prescrita pela 4.18: acrescentar `@Test` ao arquivo existente via `Edit` quando metodo alvo nao esta coberto. Sobrescrita destrutiva (substituir todo o conteudo) e proibida.
```

(Se houver redacao duplicada apos as duas mudancas acima, fundir em uma so restricao com o conteudo mais completo.)

### Conteudo da subsecao em `docs/decisoes-claude-code.md`

Inserir **antes** da subsecao `### Claude Code hooks nativos`, **apos** "Refinamento pos-smoke do test-writer: comportamento 'arquivo ja existe' (Sub-etapa 4.17.1)":

```markdown
### Ampliacao do test-writer para integration tests (Sub-etapa 4.18)

Sub-etapa de **ampliacao de subagent por escopo prescrito** (refactor categoria 4.14). A ADR-007 prescreve tres niveis de teste (unit, integration, E2E); a 4.17 entregou apenas unit; a 4.18 completa integration. E2E fica para sub-etapa futura.

**Tambem revisa o passo "0" da 4.17.1** ("arquivo ja existe" ganha excecao "metodo alvo nao coberto" -> acrescenta via `Edit`). Padrao identico ao da errata da 4.10 pela 4.15: nota de revisao adicionada a subsecao 4.17.1 apontando para esta subsecao 4.18; registro original da 4.17.1 preservado integralmente.

**Gap arquitetural concreto descoberto na auditoria empirica:**

Antes de calibrar a 4.18, foi conduzida auditoria via PowerShell aplicando a licao da 4.17.1 ("auditar antes de calibrar"). Resultado revelou que:

- 3 `*JpaRepository.java` do projeto tem queries customizadas (derived queries + `@Query` JPQL).
- A query `calcularTotaisPorConta` no `TransacaoJpaRepository` e particularmente complexa: JPQL com `CASE WHEN` aninhado, `COALESCE`, constructor expression para record do domain.
- **Nenhuma das 4 queries customizadas tem teste integration especifico.** Os `*RepositoryImplTest` cobrem `salvar`/`buscar`/`deletar`/constraints, nao as queries. Use cases que consomem as queries mockam o repository.

Diferente da 4.17 (sem cobaia legitima -> smoke parcial honesto), a 4.18 tem **cobaia obvia** (`calcularTotaisPorConta`) e **gap real** a fechar. Smoke binario, criterio determinavel.

**Modificacoes no `test-writer.md`:**

1. **Tools:** ganham `Edit` (alem do `Write` ja existente). Necessario para acrescentar `@Test` a arquivo existente sem sobrescrever.

2. **`description`:** atualizada para refletir os dois niveis cobertos + redirecionamento.

3. **`## Identidade`:** ampliada para reconhecer unit e integration. E2E declarado explicitamente fora do escopo.

4. **`## O que voce GERA`:** substituida integralmente. Agora prescreve:
   - **Detecao de nivel por path** (tabela: domain -> unit, infrastructure/persistence/Impl -> integration, JpaRepository -> redireciona, Controller -> E2E fora do escopo, outros -> fora do escopo conhecido).
   - **Regras duras de unit** (preservadas da 4.17).
   - **Regras duras de integration:** JUnit 5 + AssertJ; extends `AbstractIntegrationTest`; @Autowired do `*RepositoryImpl` + `*JpaRepository`; @AfterEach `limpar()`; sem mock; sufixo `Test`; pacote espelho. Referencia de estilo: `ContaRepositoryImplTest.java` ou `TransacaoRepositoryImplTest.java`.
   - **Redirecionamento JpaRepository -> Impl:** convencao do projeto e que testes de queries customizadas vivem no `*RepositoryImplTest.java`. Subagent precisa fazer a traducao implicitamente e reportar nas "Decisoes de design".

5. **Passo "0" do fluxo `## Quando invocado`:** reformulado. Detecta nivel + verifica existencia + decide acao:
   - Path nao mapeado -> reporta "fora do escopo" e termina (sem improvisar).
   - Arquivo de teste nao existe -> proceda com fluxo de criacao (passos 2+).
   - Arquivo existe + todos os metodos cobertos -> reporta "ja coberto" (comportamento 4.17.1).
   - Arquivo existe + metodo nao coberto -> ACRESCENTE via `Edit`, sem mexer nos testes ja presentes (excecao nova 4.18).
   - NAO sobrescreva (destruir trabalho manual e proibido).

6. **Exemplo few-shot 4** adicionado: integration test acrescentado a `TransacaoRepositoryImplTest.java` existente para cobrir `calcularTotaisPorConta`. Output mostra estrutura do relatorio para o caso "metodo nao coberto -> acrescenta via Edit".

7. **Restricoes em "O que NAO fazer":**
   - Restricao "NAO sobrescreva arquivo pre-existente" (4.17.1) refinada com a excecao da 4.18.
   - Restricao nova: NAO improvise nivel quando path nao casa regra mapeada.

**Smoke pos-merge prescrito** (responsabilidade do operador apos autorizar merge):

```
/write-test src/main/java/com/laboratorio/financas/transacao/infrastructure/persistence/TransacaoJpaRepository.java
```

**Criterios de sucesso:**

1. Subagent detecta path = `*JpaRepository.java` e **redireciona para o `TransacaoRepositoryImpl.java`** correspondente. Reporta o redirecionamento em "Decisoes de design".
2. Subagent verifica que `TransacaoRepositoryImplTest.java` existe e passa (11/11 testes atuais).
3. Subagent identifica que `calcularTotaisPorConta` e `findComFiltros` nao tem `@Test` correspondente no arquivo existente.
4. Subagent **acrescenta `@Test` para `calcularTotaisPorConta`** ao arquivo existente via `Edit`. Nao sobrescreve; nao mexe nos 11 testes ja presentes.
5. `./mvnw test -Dtest=TransacaoRepositoryImplTest` compila e passa (todos os testes — antigos + novos).
6. Teste gerado e integration real (extends `AbstractIntegrationTest`, Testcontainers, sem mock).
7. Cobertura razoavel da query: cenarios com dados reais (conta com receitas/despesas, conta sem transacoes, conta destino de transferencia, etc.).

**Se algum criterio falhar:** abrir 4.18.1 (refinamento pos-smoke empirico, categoria 4.9.1/4.17.1).

**Padrao operacional adotado:** "implementa e roda, ajusta se precisar" (formalizado pelo operador na sessao de calibracao). Smoke obrigatorio no fim da sub-etapa; ajuste minimo se aparecer borda; proximo item sem buscar perfeicao preventiva. Camada 3 completa = todos os 8 itens entregues conforme planejado (skills `/feature`, `/ship`, `/migrate`, `/audit`, hook post-edit, `migration-writer` opcional, mais a ampliacao do test-writer concluida nesta 4.18 para integration; E2E fica para sub-etapa futura).

**Revisao da 4.17.1** registrada via nota na subsecao 4.17.1 (logo apos o titulo, antes do corpo):

> **Revisao (Sub-etapa 4.18):** O passo "0" prescrito aqui foi reformulado pela 4.18 para incluir excecao "metodo alvo nao coberto" — quando arquivo de teste existe MAS algum metodo publico da classe alvo nao tem `@Test` correspondente, subagent ACRESCENTA via `Edit` (nao sobrescreve, nao Write). Comportamento "ja coberto" da 4.17.1 preservado para caso onde todos os metodos ja tem teste. Ver subsecao "Ampliacao do test-writer para integration tests (Sub-etapa 4.18)" para detalhes. Registro original da 4.17.1 preservado integralmente.

**Categoria operacional: combina duas.** "Ajuste de subagent por contexto novo" (analogo a 4.14 — escopo prescrito pela ADR-007 cumprido nominalmente parcial, completa agora) **+** revisao da prescricao 4.17.1.

**CLAUDE.md NAO atualizado.** Ampliacao de comportamento de subagent nao muda convencao do projeto. Convencao "subagents e skills" (4.11) preservada.
```

### Conteudo da nota de revisao na subsecao 4.17.1 (`decisoes-claude-code.md`)

Identificar a subsecao "Refinamento pos-smoke do test-writer: comportamento 'arquivo ja existe' (Sub-etapa 4.17.1)" em `decisoes-claude-code.md`. Adicionar a seguinte nota **logo apos o titulo da subsecao 4.17.1, antes do corpo**:

```markdown
> **Revisao (Sub-etapa 4.18):** O passo "0" prescrito aqui foi reformulado pela 4.18 para incluir excecao "metodo alvo nao coberto" — quando arquivo de teste existe MAS algum metodo publico da classe alvo nao tem `@Test` correspondente, subagent ACRESCENTA via `Edit` (nao sobrescreve, nao Write). Comportamento "ja coberto" da 4.17.1 preservado para caso onde todos os metodos ja tem teste. Ver subsecao "Ampliacao do test-writer para integration tests (Sub-etapa 4.18)" abaixo para detalhes. Registro original desta subsecao preservado integralmente.
```

### Conteudo das edicoes em `docs/progresso.md`

**Edicao 1 — Sub-etapa 4.18 no topo de "Sub-etapas concluidas" da Camada 3** (acima da 4.17.1):

```markdown
- **4.18 — Ampliacao do `test-writer` para integration tests + revisao da 4.17.1** (2026-05-12): sub-etapa de **ampliacao de subagent por escopo prescrito** (refactor categoria 4.14). A ADR-007 prescreve tres niveis (unit, integration, E2E); a 4.17 entregou apenas unit; a 4.18 completa integration. E2E fica para sub-etapa futura. **Tambem revisa o passo "0" da 4.17.1**: arquivo existente com metodo alvo NAO coberto ganha excecao — subagent ACRESCENTA `@Test` via `Edit` em vez de parar. Auditoria empirica pre-calibracao (aplicando licao da 4.17.1) revelou **gap arquitetural concreto**: 4 queries customizadas em `*JpaRepository.java` sem teste integration (incluindo `calcularTotaisPorConta` com JPQL `CASE WHEN`/`COALESCE` complexa). Cobaia obvia para smoke real, diferente da 4.17 (smoke parcial honesto). Modificacoes no `test-writer.md`: tools ganham `Edit`; description ampliada; identidade reconhece dois niveis; secao "O que voce GERA" reescrita com detecao de nivel por path + regras integration + redirecionamento `JpaRepository -> *Impl`; passo "0" reformulado; exemplo few-shot 4 adicionado; restricoes ajustadas. Nota de revisao adicionada a subsecao 4.17.1 em `decisoes-claude-code.md` (padrao identico a errata 4.10 -> 4.15). **Padrao operacional adotado:** "implementa e roda, ajusta se precisar" (formalizado pelo operador). CLAUDE.md NAO atualizado. PR #XX.
```

**Edicao 2 — Atualizar criterios da Camada 3.**

Substituir bloco atual de "Criterios de 'pronto'". Mudancas principais:

- Linha do `test-writer` ganha "ampliado pela 4.18 para integration".
- Smoke 4.17 mantem nota "validacao parcial em unit; integration validado na 4.18 com cobaia real".
- Adicionar item explicito `[ ] Smoke pos-merge da 4.18 validando integration tests` (criterio binario novo).

```markdown
### Criterios de "pronto" (preliminar, ajustados pela 4.10, 4.11, 4.12, 4.13, 4.16, 4.17, 4.17.1 e 4.18)

- [x] `CLAUDE.md` do projeto escrito (target <=15KB) -- concluido 4.6, atualizado 4.11, 4.13 e 4.16
- [x] Padrao skill orquestradora -> subagent decidido -- ADR-012 (4.10, revisado 4.11)
- [x] Subagent `pr-reviewer` (revisao critica antes do PR) -- concluido 4.9 + refinado 4.9.1
- [x] Skill `/review-pr` orquestrando `pr-reviewer` (par com ADR-012) -- concluido 4.11
- [x] Smoke pos-merge da 4.11 validando padrao skill+subagent ponta-a-ponta -- validado em PR #55 + PR #45
- [x] Subagent `architect-reviewer` (Sonnet, valida decisoes contra ADR-004/005/006/007) -- concluido 4.12
- [x] Skill `/review-arch` orquestrando `architect-reviewer` (par com ADR-012) -- concluido 4.12
- [x] Smoke pos-merge da 4.12 validando segundo par skill+subagent -- validado em PR #35
- [x] Subagent `test-writer` + skill `/write-test` (par ADR-012, primeiro gerador) -- concluido 4.17 (escopo: unit tests). **Refinado pela 4.17.1** (comportamento "arquivo ja existe"). **Ampliado pela 4.18 para integration tests** (cobertura ADR-007 parcial -> completa unit + integration; E2E fica para sub-etapa futura).
- [ ] Smoke pos-merge da 4.17 (unit tests) -- **validacao parcial em 2026-05-12** (componentes OK; cobaia tinha teste pre-existente; geracao propriamente dita aguarda primeiro uso real na Camada 4).
- [ ] Smoke pos-merge da 4.18 validando integration tests com cobaia real (`calcularTotaisPorConta`).
- [ ] Subagent (opcional) `migration-writer` + skill `/write-migration` (par ADR-012)
- [ ] Skill `/feature <nome>` (cria estrutura de bounded context)
- [ ] Skill `/ship` (lint + test + build + push + PR)
- [ ] Skill `/migrate` (gera migration + atualiza schema + escreve teste)
- [ ] Skill `/audit` (varre modulos buscando padrao especifico)
- [ ] Ampliacao do `test-writer` para E2E tests (sub-etapa futura se uso justificar)
- [x] Hook pre-commit funcionando -- concluido 4.1-4.7, refinado 4.14
- [ ] Hook post-edit rodando testes do arquivo mexido
- [ ] Decisao sobre plugin `code-review` oficial: nao e debito do projeto (re-classificado 4.15)
```

**Edicao 3 — Bloco "Licoes da Sub-etapa 4.18"** acima de "Licoes da Sub-etapa 4.17.1":

```markdown
## Licoes da Sub-etapa 4.18

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — ampliacao de subagent.)

### Licoes de ambiente

1. **Categoria operacional combinada: ampliacao + revisao.** A 4.18 e simultaneamente "ajuste de subagent por contexto novo" (analogo a 4.14 — escopo prescrito pela ADR-007 cumprido parcial pela 4.17, completa agora) **e** revisao da 4.17.1 (excecao "metodo nao coberto"). Padrao operacional: sub-etapas podem combinar categorias quando refactor de componente exige refinamento de prescricao previa. Importante manter os dois registros separados (subsecao 4.18 explica ambos; nota de revisao em 4.17.1 aponta para 4.18; registros originais preservados — padrao identico a errata 4.10 -> 4.15).

2. **Auditoria empirica pre-calibracao revelou gap concreto.** Aplicando licao da 4.17.1 ("auditar antes de calibrar"), foi conduzido inventario PowerShell que revelou 4 queries customizadas em `*JpaRepository.java` sem teste integration. Cobaia obvia (`calcularTotaisPorConta`), gap real, smoke determinavel. Diferente da 4.17 que descobriu **ausencia de cobaia legitima** so no smoke, a 4.18 descobriu **presenca de cobaia legitima** antes da calibracao. **Padrao operacional firmado:** antes de ampliar subagent gerador, auditar projeto empiricamente para confirmar se ha cobaia natural (classe com comportamento real sem teste). Aplicavel a futuras ampliacoes do `test-writer` (E2E) e a outros subagents geradores (`migration-writer`).

3. **Detecao de nivel por path como padrao operacional para subagent que cobre multiplos niveis.** Regras explicitas no system prompt: path-to-level + fallback "fora do escopo conhecido" para paths nao mapeados. Subagent nao improvisa quando path nao casa nenhuma regra — reporta e termina. Padrao replicavel para subagents futuros que cobrem multiplos contextos (ex: `migration-writer` que cobre migrations de criacao vs migrations de alteracao).

4. **Redirecionamento `JpaRepository -> Impl` como convencao do projeto explicita no subagent.** Testes integration de queries customizadas vivem no `*RepositoryImplTest.java` (junto com testes do Impl), nao em arquivo separado. Subagent precisa fazer a traducao implicitamente e reportar no relatorio. Padrao operacional: **convencoes do projeto que afetam decisao de geracao devem estar prescritas no system prompt do subagent, nao deixadas para inferencia.**

5. **Tools dos subagents seguem o que o subagent faz, refinado.** A 4.17 prescreveu "geradores tem `Write`". A 4.18 refina: "geradores que tambem precisam acrescentar a arquivo existente tem `Write` + `Edit`". Padrao "tools por funcao" evolui com necessidade. Aplicavel a `migration-writer` futuro (provavelmente tools = `Read, Grep, Glob, Bash, Write` — migrations criam arquivos novos, raramente editam existentes).

6. **Excecao prescrita ao "arquivo ja existe" formaliza a improvisacao bem-sucedida observada na 4.17.** A 4.17.1 prescreveu "arquivo existe -> pare, reporte". A 4.18 reconhece que **quando metodo alvo NAO esta coberto**, parar e desperdicio — operador quer cobertura, nao ausencia. Excecao formalizada via `Edit` (acrescenta, nao sobrescreve). Padrao operacional: prescricoes podem ser refinadas conforme casos de uso revelam exception. Refinamento subsequente registra a revisao via nota de errata (padrao identico a 4.10 -> 4.15) preservando registro original.

7. **Padrao "implementa e roda, ajusta se precisar" formalizado pelo operador.** Sub-etapas pendentes da Camada 3 (8 itens — `test-writer` integration na 4.18, skills `/feature`, `/ship`, `/migrate`, `/audit`, hook post-edit, `migration-writer` opcional, eventual E2E) serao implementadas em sequencia. Cada uma: entrega + smoke + ajuste minimo se aparecer + proxima sem buscar perfeicao preventiva. Camada 4 (fabrica rodando com features reais) so abre quando todos os 8 itens estiverem entregues — independente de smoke ser "completo" ou "parcial honesto". **Disciplina contra perfeccionismo travador.**
```

**Edicao 4 — Linha no historico** acima da entrada da 4.17.1:

```markdown
- **2026-05-12** — Sub-etapa 4.18 concluida (ampliacao de subagent por escopo prescrito + revisao 4.17.1): `test-writer` ampliado para integration tests, cobrindo `*RepositoryImpl` em `*/infrastructure/persistence/` via `AbstractIntegrationTest` (Testcontainers). Detecao de nivel por path; redirecionamento `JpaRepository -> *Impl`. Passo "0" reformulado: excecao "metodo nao coberto" usa `Edit` para acrescentar. Tools ganham `Edit`. Cobaia real: `calcularTotaisPorConta` (gap concreto descoberto na auditoria empirica). Categoria combinada. Padrao "implementa e roda, ajusta se precisar" formalizado. 7 licoes novas. CLAUDE.md NAO atualizado. PR #XX.
```

### Versionar este proprio prompt

`docs/prompts/prompt-etapa-4-18.md` entra como novo arquivo no Commit 4.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra squash da 4.17.1.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompts/prompt-etapa-4-18.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `.claude/agents/test-writer.md` existe (~213 linhas pos-4.17.1). Sera modificado nesta sub-etapa.
- `docs/decisoes-claude-code.md` tem subsecao "Refinamento pos-smoke do test-writer: comportamento 'arquivo ja existe' (Sub-etapa 4.17.1)" antes de "Claude Code hooks nativos".
- `src/test/java/com/laboratorio/financas/transacao/infrastructure/persistence/TransacaoRepositoryImplTest.java` existe (11 @Test passando, **a confirmar via Tarefa 4**).
- `src/test/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaRepositoryImplTest.java` existe (`extends AbstractIntegrationTest`, **a confirmar**).
- `src/test/java/com/laboratorio/financas/shared/AbstractIntegrationTest.java` existe.

Validar com:

```powershell
git status
git log --oneline -3
git config core.hooksPath
Test-Path .claude\agents\test-writer.md
Test-Path docs\prompts\prompt-etapa-4-18.md
(Get-Content .claude\agents\test-writer.md).Count
Test-Path src\test\java\com\laboratorio\financas\transacao\infrastructure\persistence\TransacaoRepositoryImplTest.java
Test-Path src\test\java\com\laboratorio\financas\shared\AbstractIntegrationTest.java
```

**Pre-condicoes ADR-011:**

- `Test-Path .claude\agents\test-writer.md` retorna `True`.
- `Test-Path docs\prompts\prompt-etapa-4-18.md` retorna `True`.
- Arquivos de referencia (`TransacaoRepositoryImplTest.java`, `AbstractIntegrationTest.java`) existem.
- Working tree limpo exceto o prompt.

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
git checkout -b refactor/etapa-4-18-test-writer-integration
```

Prefixo `refactor/` — refactor de subagent existente.

### Tarefa 4 — Antes de editar, ler arquivos vivos

```bash
cat .claude/agents/test-writer.md
cat docs/decisoes-claude-code.md
cat docs/progresso.md
cat src/test/java/com/laboratorio/financas/transacao/infrastructure/persistence/TransacaoRepositoryImplTest.java
cat src/test/java/com/laboratorio/financas/shared/AbstractIntegrationTest.java
```

**Confirmar e anotar:**

- `test-writer.md`: anotar linha exata onde aparece:
  - Frontmatter `tools: Read, Grep, Glob, Bash, Write` (sera modificado para incluir `Edit`).
  - Frontmatter `description:` (sera substituido).
  - Secao `## Identidade` (corpo sera substituido).
  - Secao `## O que voce GERA` (corpo sera substituido integralmente).
  - Passo "1" no fluxo `## Quando invocado` que e o passo "0" da 4.17.1 (sera reformulado).
  - Secao `## Exemplos` e onde termina o Exemplo 3 (Exemplo 4 entra apos).
  - Secao `## O que NAO fazer` (2 restricoes ajustadas + 1 nova).
- `decisoes-claude-code.md`: confirma que subsecao 4.17.1 existe antes de "Claude Code hooks nativos". Nova subsecao 4.18 entra entre essas duas. Nota de revisao adicionada na 4.17.1.
- `progresso.md`: confirma "Sub-etapas concluidas" da Camada 3 em ordem descrescente comecando em 4.17.1. Sub-etapa 4.18 entra acima.
- `progresso.md`: confirma "Criterios de 'pronto'" — bloco sera substituido pela versao ajustada.
- `progresso.md`: confirma "Licoes da Sub-etapa 4.17.1" — "Licoes da Sub-etapa 4.18" entra acima.
- `TransacaoRepositoryImplTest.java`: confirma que extends `AbstractIntegrationTest`, tem `@AfterEach void limpar()`, tem `@Autowired` para `TransacaoRepositoryImpl` e `TransacaoJpaRepository`. Anota numero de `@Test` atual (esperado: 11).
- `AbstractIntegrationTest.java`: confirma estrutura (Testcontainers Postgres, `@DynamicPropertySource`, etc.).

Se alguma divergencia, **parar e reportar**.

### Tarefa 5 — Modificar `.claude/agents/test-writer.md` (7 mudancas)

Aplicar **Mudancas 1-7** descritas no escopo, na ordem:

1. Frontmatter `tools` ganha `Edit`.
2. Frontmatter `description` substituido.
3. Secao `## Identidade` substituida.
4. Secao `## O que voce GERA` substituida integralmente (com detecao de nivel + regras unit + regras integration + redirecionamento).
5. Passo "1" no fluxo `## Quando invocado` (passo "0" original da 4.17.1) reformulado.
6. Exemplo few-shot 4 adicionado apos o Exemplo 3.
7. Secao `## O que NAO fazer`: revisar restricao existente sobre "NAO sobrescreva" + adicionar restricao nova sobre "NAO improvise nivel".

**Restricoes da edicao:**

- Encoding UTF-8 sem BOM preservado.
- Sem acentos no body. Em-dash U+2014 permitido em prosa Markdown (padrao consolidado).
- Comportamento original (unit) preservado integralmente nas regras duras de unit.
- Restricoes 1, 2, 3, 4, 5, 6, 7, 8 do escopo original de unit (4.17) preservadas.
- Restricao "NAO tente auto-corrigir em loop" (4.17) preservada.
- Restricao "NAO use Mockito em unit test puro" (4.17) preservada.

**Pre-condicao ADR-011 apos editar:**

```powershell
Test-Path .claude\agents\test-writer.md   # True

$bytes = [System.IO.File]::ReadAllBytes(".claude/agents/test-writer.md")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191 (sem BOM)

$content = [System.IO.File]::ReadAllText(".claude/agents/test-writer.md", [System.Text.UTF8Encoding]::new($false))

# Mudanca 1: Edit nos tools
if ($content -match 'tools:.*Edit') {
    Write-Host "Tools com Edit OK"
} else {
    Write-Host "Tools com Edit AUSENTE"
}

# Mudanca 2: description amplia
if ($content -match 'Integration tests para.{1,50}RepositoryImpl') {
    Write-Host "Description ampliada OK"
} else {
    Write-Host "Description ampliada AUSENTE"
}

# Mudanca 3: Identidade reconhece dois niveis
if ($content -match 'Niveis de teste cobertos') {
    Write-Host "Identidade dois niveis OK"
} else {
    Write-Host "Identidade dois niveis AUSENTE"
}

# Mudanca 4: Detecao de nivel por path
if ($content -match 'detecao.{1,5}nivel.{1,50}path') {
    Write-Host "Detecao por path OK"
} else {
    Write-Host "Detecao por path AUSENTE"
}

# Mudanca 4: Regras integration presentes
if ($content -match 'AbstractIntegrationTest') {
    Write-Host "Regra AbstractIntegrationTest OK"
} else {
    Write-Host "Regra AbstractIntegrationTest AUSENTE"
}

# Mudanca 4: Redirecionamento
if ($content -match 'Redirecionamento.{1,30}Impl') {
    Write-Host "Redirecionamento OK"
} else {
    Write-Host "Redirecionamento AUSENTE"
}

# Mudanca 5: Passo "0" reformulado com Edit
if ($content -match 'ACRESCENT.{1,30}Edit') {
    Write-Host "Passo 0 reformulado OK"
} else {
    Write-Host "Passo 0 reformulado AUSENTE"
}

# Mudanca 6: Exemplo 4
if ($content -match '### Exemplo 4') {
    Write-Host "Exemplo 4 OK"
} else {
    Write-Host "Exemplo 4 AUSENTE"
}

# 4 exemplos few-shot totais
$exemplos = ([regex]::Matches($content, '### Exemplo \d')).Count
Write-Host "Exemplos encontrados: $exemplos (esperado: 4)"

# Mudanca 7: restricao "NAO improvise nivel"
if ($content -match 'NAO improvise nivel') {
    Write-Host "Restricao improvise nivel OK"
} else {
    Write-Host "Restricao improvise nivel AUSENTE"
}

# Regras duras de unit (4.17) ainda presentes (amostra)
$regras_unit = @('JUnit 5', 'AssertJ', 'Zero Spring', 'NAO tente auto-corrigir em loop')
foreach ($r in $regras_unit) {
    if ($content -match $r) {
        Write-Host "Regra unit preservada OK: $r"
    } else {
        Write-Host "ERRO: regra unit removida: $r"
    }
}

# Linhas totais (esperado: ~330-370, crescimento significativo sobre 213)
$linhas = (Get-Content .claude\agents\test-writer.md).Count
Write-Host "Linhas totais: $linhas (esperado: 330-370)"
```

Se algum valor divergir, parar e reportar.

### Tarefa 6 — Atualizar `docs/decisoes-claude-code.md` (subsecao 4.18 + nota na 4.17.1)

Aplicar duas mudancas:

1. Inserir subsecao 4.18 **antes** de `### Claude Code hooks nativos`, **apos** "Refinamento pos-smoke do test-writer: comportamento 'arquivo ja existe' (Sub-etapa 4.17.1)".
2. Adicionar nota de revisao na subsecao 4.17.1 (blockquote logo apos o titulo, antes do corpo).

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/decisoes-claude-code.md", [System.Text.UTF8Encoding]::new($false))

# Subsecao 4.18
if ($content -match '### Ampliacao do test-writer para integration') {
    Write-Host "Subsecao 4.18 OK"
} else {
    Write-Host "Subsecao 4.18 AUSENTE"
}

# Nota de revisao na 4.17.1
if ($content -match '> \*\*Revisao \(Sub-etapa 4\.18\)') {
    Write-Host "Nota revisao 4.17.1 OK"
} else {
    Write-Host "Nota revisao 4.17.1 AUSENTE"
}

# Ordem: 4.17.1 antes da 4.18 antes de hooks nativos
$pos417_1 = $content.IndexOf('Refinamento pos-smoke do test-writer')
$pos418 = $content.IndexOf('Ampliacao do test-writer para integration')
$posNativos = $content.IndexOf('Claude Code hooks nativos')
if ($pos417_1 -lt $pos418 -and $pos418 -lt $posNativos) {
    Write-Host "Ordem OK"
} else {
    Write-Host "Ordem TROCADA"
}

# Subsecao 4.17.1 ainda presente (nao removida indevidamente)
if ($content -match '### Refinamento pos-smoke do test-writer') {
    Write-Host "4.17.1 preservada OK"
} else {
    Write-Host "ERRO: 4.17.1 removida"
}

# Hook 4.4 NAO deve alertar
$linhas = (Get-Content docs\decisoes-claude-code.md).Count
Write-Host "Linhas totais: $linhas"
```

### Tarefa 7 — Atualizar `docs/progresso.md` (4 edicoes)

Aplicar **edicoes 1-4** descritas no escopo:

1. Sub-etapa 4.18 ao topo de "Sub-etapas concluidas".
2. Substituir bloco "Criterios de 'pronto'" pela versao ajustada.
3. "Licoes da Sub-etapa 4.18" acima de "Licoes da Sub-etapa 4.17.1".
4. Linha de historico acima da entrada da 4.17.1.

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/progresso.md", [System.Text.UTF8Encoding]::new($false))

# Sub-etapa 4.18
if ($content -match '4\.18.{1,15}Ampliacao do.{1,15}test-writer') {
    Write-Host "Sub-etapa 4.18 OK"
} else {
    Write-Host "Sub-etapa 4.18 AUSENTE"
}

# Licoes 4.18
if ($content -match '## Li.{1,3}es da Sub-etapa 4\.18') {
    Write-Host "Licoes 4.18 OK"
} else {
    Write-Host "Licoes 4.18 AUSENTE"
}

# Criterio test-writer ampliado
if ($content -match '\[x\] Subagent `test-writer`.{1,200}Ampliado pela 4\.18') {
    Write-Host "Criterio test-writer ampliado OK"
} else {
    Write-Host "Criterio test-writer ampliado AUSENTE"
}

# Criterio smoke 4.18 pendente
if ($content -match '\[ \] Smoke pos-merge da 4\.18') {
    Write-Host "Criterio smoke 4.18 pendente OK"
} else {
    Write-Host "Criterio smoke 4.18 pendente AUSENTE"
}

# Ordem cronologica
$pos418 = $content.IndexOf('**4.18')
$pos417_1 = $content.IndexOf('**4.17.1')
if ($pos418 -gt 0 -and $pos418 -lt $pos417_1) {
    Write-Host "Ordem cronologica OK"
} else {
    Write-Host "Ordem cronologica TROCADA"
}

# Hook 4.4 NAO deve alertar
$linhas = (Get-Content docs\progresso.md).Count
Write-Host "Linhas totais: $linhas"
```

### Tarefa 8 — Commits (4 commits)

**Commit 1** — Subagent ampliado:

```bash
git add .claude/agents/test-writer.md
git status   # apenas test-writer.md staged
git commit -m "refactor(claude): test-writer ampliado para integration tests + revisao passo 0 da 4.17.1"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 2** — Decisoes:

```bash
git add docs/decisoes-claude-code.md
git status   # apenas decisoes-claude-code.md staged
git commit -m "docs: subsecao 4.18 (ampliacao do test-writer) + nota revisao 4.17.1"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 3** — Progresso:

```bash
git add docs/progresso.md
git status   # apenas progresso.md staged
git commit -m "docs: sub-etapa 4.18 -- ampliacao test-writer para integration + criterios ajustados"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Atencao:** hook 4.4 NAO deve alertar.

**Commit 4** — Versionar prompt:

```bash
git add docs/prompts/prompt-etapa-4-18.md
git status   # apenas prompt-etapa-4-18.md staged
git commit -m "docs: versiona prompt-etapa-4-18.md em docs/prompts/"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Validacao implicita dos hooks ativos:**

- Conventional Commits (4.1): mensagens validas.
- Encoding UTF-8 (4.2): valida bytes.
- Markdown blank lines (4.3): valida headers.
- Tamanho de docs (4.4): NAO deve alertar (decisoes-claude-code.md cresce, mas deve ficar abaixo de 800; progresso.md cresce levemente).

Se algum hook bloquear, parar e reportar.

### Tarefa 9 — Validacao final antes de push

```bash
git status
git log --oneline -5
git config core.hooksPath
```

```powershell
Test-Path .claude\agents\test-writer.md          # True
(Get-Content .claude\agents\test-writer.md).Count
Test-Path .claude\agents\pr-reviewer.md          # True (inalterado)
Test-Path .claude\agents\architect-reviewer.md   # True (inalterado)
Test-Path .claude\skills\write-test\SKILL.md     # True (inalterado)
Test-Path src\test\java\com\laboratorio\financas\transacao\infrastructure\persistence\TransacaoRepositoryImplTest.java  # True (inalterado)
```

Esperado:

- Working tree limpo.
- 4 commits novos.
- `test-writer.md` em ~330-370 linhas.
- Componentes da 4.9/4.12/4.17 (skill `/write-test`) e arquivos de teste do projeto inalterados.

## Restricoes e freios

1. **NAO modificar `.claude/agents/pr-reviewer.md` ou `.claude/agents/architect-reviewer.md`.** Subagents revisores permanecem intactos.

2. **NAO modificar `.claude/skills/write-test/SKILL.md` ou outras skills.** Skills permanecem intactas.

3. **NAO modificar `.claude/hooks/`, `.githooks/`.**

4. **NAO criar subagents, skills, hooks novos.** Sub-etapa de ampliacao.

5. **NAO atualizar `CLAUDE.md`.** Ampliacao de comportamento de subagent nao muda convencao do projeto.

6. **NAO atualizar `docs/adrs.md`.** Sem ADR novo.

7. **NAO atualizar `docs/decisoes.md` (fundacional).** Ampliacao e de componente operacional da Camada 3 — vai em `decisoes-claude-code.md`.

8. **NAO atualizar `docs/visao.md`, `docs/blueprint-fabrica-ai-native.md`, `docs/progresso-historico.md`, `docs/hooks-pendentes.md`.**

9. **NAO marcar `[x] Smoke pos-merge da 4.17`.** Mantem como `[ ]` com nota de "validacao parcial". Smoke da 4.18 e item separado (`[ ] Smoke pos-merge da 4.18`).

10. **NAO modificar regras duras de unit do `test-writer.md`** (JUnit 5, AssertJ, Zero Spring, sufixo Test, mock manual, etc.). Preservadas integralmente. Aplicam-se quando path detectado e `*/domain/`.

11. **NAO modificar restricao "NAO tente auto-corrigir em loop"** (4.17). Padrao operador-soberano preservado.

12. **NAO modificar Exemplos 1, 2, 3** (4.17 e 4.17.1). Apenas adicionar Exemplo 4.

13. **NAO criar arquivos de teste reais** (ex: `TransacaoRepositoryImplTest.java` modificado) nesta sub-etapa. Refinamento entrega prescricao; smoke real (que efetivamente modifica `TransacaoRepositoryImplTest.java`) e responsabilidade do operador apos merge.

14. **NAO modificar `src/`, `frontend/`, `pom.xml`.**

15. **NAO modificar `src/test/`** nesta sub-etapa. Smoke pos-merge mexera em `TransacaoRepositoryImplTest.java` quando o operador rodar `/write-test`. Aqui apenas prescrevemos.

16. **Preservar nota de errata da 4.17.1 (que aponta para 4.18) intacta** apos adicionada — registro original da 4.17.1 nao deve ser modificado, apenas a nota anexada.

17. **Encoding UTF-8 sem BOM** em todos os arquivos editados.

18. **Apenas ASCII em mensagens de commit.** Sem acentos, sem em-dash U+2014.

19. **Sem acentos no body do `test-writer.md`** (alinhado com convencao de subagents). Em-dash U+2014 permitido em prosa Markdown.

20. **Ordem cronologica descrescente** em "Sub-etapas concluidas", "Licoes", "Historico" em `progresso.md`.

21. **Sem cenarios destrutivos tradicionais.** Sub-etapa modifica system prompt — validacao via pre-condicoes ADR-011 em cada Tarefa. Validacao empirica real vem no smoke pos-merge.

22. **Hook 4.4 NAO deve alertar em nenhum commit.** Se alertar, investigar.

23. **Nao sugerir proxima sub-etapa** espontaneamente alem dos candidatos ja citados (smoke pos-merge da 4.18, 4.19 com `/feature` ou outra skill pendente, eventual 4.18.1 se smoke revelar borda).

24. **Nao tomar decisao silenciosa em zona limitrofe.** Reportar divergencias.

25. **Nao usar `pwsh`.** PowerShell 5.1.

26. **Nao usar `git reset --hard`.**

27. **Nao usar `git commit --no-verify`.**

## Estrutura de commits

Branch: `refactor/etapa-4-18-test-writer-integration`

**Commit 1** — `refactor(claude): test-writer ampliado para integration tests + revisao passo 0 da 4.17.1`

- `.claude/agents/test-writer.md` (7 mudancas: tools, description, identidade, "O que voce GERA", passo "0" reformulado, Exemplo 4, restricoes)

**Commit 2** — `docs: subsecao 4.18 (ampliacao do test-writer) + nota revisao 4.17.1`

- `docs/decisoes-claude-code.md` (subsecao 4.18 + nota de revisao na 4.17.1)

**Commit 3** — `docs: sub-etapa 4.18 -- ampliacao test-writer para integration + criterios ajustados`

- `docs/progresso.md` (sub-etapa 4.18 + criterios ajustados + 7 licoes + historico)

**Commit 4** — `docs: versiona prompt-etapa-4-18.md em docs/prompts/`

- `docs/prompts/prompt-etapa-4-18.md` (novo)

## Validacao antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -5
git config core.hooksPath
```

```powershell
Test-Path .claude\agents\test-writer.md
(Get-Content .claude\agents\test-writer.md).Count
Test-Path .claude\agents\pr-reviewer.md
Test-Path .claude\agents\architect-reviewer.md
Test-Path .claude\skills\write-test\SKILL.md
```

Esperado:

- `check.ps1` passa.
- Working tree limpo.
- 4 commits novos.
- `test-writer.md` em ~330-370 linhas.
- Componentes nao tocados permanecem inalterados.

## PR

Titulo: `refactor: sub-etapa 4.18 -- test-writer ampliado para integration tests + revisao 4.17.1`

Body sugerido:

````markdown
## Summary

Sub-etapa de **ampliacao de subagent por escopo prescrito** (refactor categoria 4.14). A ADR-007 prescreve tres niveis de teste; a 4.17 entregou apenas unit; a 4.18 completa integration. E2E fica para sub-etapa futura. **Tambem revisa o passo "0" da 4.17.1** (excecao "metodo nao coberto" -> acrescenta via `Edit`).

### Por que esta sub-etapa existe

Camada 3 do plano prescreve `test-writer` cobrindo os tres niveis da ADR-007. A 4.17 entregou apenas unit (Arquitetura C: comecar focado, ampliar se uso justificar). Uso justificou — ADR-007 prescreve, plano prescreve, **completar a Camada 3 conforme planejado** e o trabalho real agora.

### Auditoria empirica pre-calibracao revelou gap concreto

Aplicando licao da 4.17.1 ("auditar antes de calibrar"), foi conduzido inventario PowerShell:

- 3 `*JpaRepository.java` tem queries customizadas.
- 4 queries customizadas (`findByTipo`, `findByAtivaTrue`, `findComFiltros`, `calcularTotaisPorConta`) **sem teste integration**.
- `calcularTotaisPorConta`: JPQL com `CASE WHEN`/`COALESCE`/constructor expression. Alta complexidade, alto risco semantico, zero cobertura.

**Gap arquitetural concreto, nao teorico.** Diferente da 4.17 (sem cobaia legitima -> smoke parcial honesto), a 4.18 tem cobaia obvia.

### Categoria operacional combinada

A 4.18 e simultaneamente:

- **"Ajuste de subagent por contexto novo"** (analogo a 4.14): escopo prescrito pela ADR-007 cumprido parcial pela 4.17, completa agora.
- **Revisao da 4.17.1**: passo "0" reformulado para incluir excecao "metodo nao coberto" -> acrescenta via `Edit`.

Padrao operacional: sub-etapas podem combinar categorias quando refactor de componente exige refinamento de prescricao previa. Registros separados (subsecao 4.18 explica ambos; nota de revisao em 4.17.1 aponta para 4.18; registro original 4.17.1 preservado — padrao identico a errata 4.10 -> 4.15).

### Modificacoes no `test-writer.md` (7 mudancas)

1. **Tools:** ganham `Edit`. Necessario para acrescentar `@Test` a arquivo existente.
2. **`description`** ampliada para refletir dois niveis + redirecionamento.
3. **`## Identidade`** ampliada (unit + integration; E2E fora do escopo declarado).
4. **`## O que voce GERA`** substituida integralmente: detecao de nivel por path + regras unit + regras integration + redirecionamento `JpaRepository -> *Impl`.
5. **Passo "0" do fluxo** reformulado: detecta nivel, verifica existencia, decide acao (criar via `Write` se nao existe, acrescentar via `Edit` se metodo nao coberto, reportar "ja coberto" se todos cobertos).
6. **Exemplo few-shot 4** adicionado: integration test acrescentado a `TransacaoRepositoryImplTest.java` para cobrir `calcularTotaisPorConta`.
7. **Restricoes em "O que NAO fazer":** "NAO sobrescreva" refinada com excecao da 4.18; "NAO improvise nivel" adicionada.

### Regras duras de integration test

- JUnit 5 + AssertJ.
- Extends `AbstractIntegrationTest` (Testcontainers Postgres via `@DynamicPropertySource` — lição 2.1).
- `@Autowired` do `*RepositoryImpl` + `*JpaRepository`.
- `@AfterEach void limpar()`.
- Sem mock.
- Sufixo `Test` (singular, convencao do projeto).
- Pacote espelho.
- Referencia de estilo: `ContaRepositoryImplTest.java` ou `TransacaoRepositoryImplTest.java`.

### Redirecionamento JpaRepository -> Impl

Convencao do projeto: testes de queries customizadas vivem em `*RepositoryImplTest.java` (junto com testes do Impl), nao em arquivo separado. Subagent faz a traducao implicitamente e reporta nas "Decisoes de design".

### Smoke pos-merge prescrito (responsabilidade do operador)

```
/write-test src/main/java/com/laboratorio/financas/transacao/infrastructure/persistence/TransacaoJpaRepository.java
```

**Criterios de sucesso:**

1. Subagent detecta `*JpaRepository.java` -> redireciona para `*RepositoryImpl.java` correspondente.
2. Verifica que `TransacaoRepositoryImplTest.java` existe (11 @Test atuais passando).
3. Identifica que `calcularTotaisPorConta` nao tem @Test correspondente.
4. **Acrescenta @Test para `calcularTotaisPorConta` ao arquivo existente via `Edit`** (nao sobrescreve, nao mexe nos 11 testes ja presentes).
5. `./mvnw test -Dtest=TransacaoRepositoryImplTest` compila e passa.
6. Teste e integration real (Testcontainers, sem mock).
7. Cobertura razoavel da query: cenarios com dados reais.

**Se falhar:** abrir 4.18.1 (refinamento pos-smoke empirico).

### Padrao operacional adotado: "implementa e roda, ajusta se precisar"

Formalizado pelo operador durante calibracao. Sub-etapas pendentes da Camada 3 (8 itens — esta 4.18 + 7 restantes) serao implementadas em sequencia. Cada uma: entrega + smoke + ajuste minimo se aparecer + proxima sem buscar perfeicao preventiva. Disciplina contra perfeccionismo travador.

### CLAUDE.md NAO atualizado

Ampliacao de comportamento de subagent nao muda convencao do projeto. Regra 4.6 preservada.

### Proximo passo

Decisao fora deste PR. Candidatos naturais (em ordem proposta para Camada 3):

- **Smoke pos-merge da 4.18** (responsabilidade do operador apos merge).
- **4.19** — `/feature <nome>` (skill geradora pura sem subagent; eixo novo nao-validado ainda).
- **4.20** — `/ship` (skill orquestradora de fluxo: lint + test + build + push + PR).
- **4.21** — Hook post-edit (evento novo no Claude Code).
- **4.22** — `migration-writer` + `/write-migration` (par gerador, replicacao do padrao test-writer).
- **4.23** — `/migrate` (orquestra migration-writer + atualiza schema + invoca /write-test).
- **4.24** — `/audit` (varre modulos buscando padrao especifico).
- **Eventual 4.25** — ampliacao do test-writer para E2E tests se uso justificar.
- **Camada 4** — abre quando todos os itens da Camada 3 estiverem entregues.
````

## Pos-criacao do PR

1. Abrir PR via `gh pr create`.
2. Capturar o numero.
3. Editar `docs/decisoes-claude-code.md` e `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorizacao explicita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `refactor/etapa-4-18-test-writer-integration` empurrada com 5 commits (4 + 1 update do PR).
- PR aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.17.1.
- Working tree limpo.
- `.claude/agents/test-writer.md` em ~330-370 linhas.
- `.claude/agents/pr-reviewer.md`, `.claude/agents/architect-reviewer.md`, `.claude/skills/`, `src/test/` inalterados.
- Reportar: `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup`, contagem de linhas do `test-writer.md`, contagem de exemplos few-shot (esperado: 4).

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao executar a skill `/write-test` (smoke real e pos-merge).
- Nao modificar `TransacaoRepositoryImplTest.java` ou outros arquivos de teste.
- Nao modificar `src/main/java/`, `frontend/`, `pom.xml`.
- Nao criar prompt da 4.19 ou outros.
- Nao criar outros subagents, skills, hooks, MCPs.
- Nao modificar `.claude/skills/`, outros subagents.
- Nao mexer em `~/.claude/` global.
- Nao atualizar `CLAUDE.md`, blueprint, `.gitignore`, `.gitattributes`, `hooks-pendentes.md`, `adrs.md`, `decisoes.md`, `visao.md`, `progresso-historico.md`.
- Nao marcar `[x] Smoke pos-merge da 4.17` (smoke 4.17 fica como validacao parcial; smoke 4.18 e item separado).
- Nao sugerir proximo passo espontaneamente alem dos candidatos ja citados no PR body.
- Nao usar `pwsh`.
- Nao usar `git reset --hard`.
- Nao usar `git commit --no-verify`.
