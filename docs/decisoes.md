# Decisões — Stack, Padrões e Convenções

> Documento operacional. Resume **o que foi decidido** para consulta rápida em qualquer chat.
> Diferente dos ADRs, este documento não preserva contexto histórico — é a foto atual.
> Atualizado quando uma decisão muda. Para entender o **porquê** de uma decisão, ver `adrs.md`.

**Última atualização:** 2026-05-10

---

## Stack

### Backend

| Componente | Escolha | Versão |
|---|---|---|
| Linguagem | Java | 21 (LTS) |
| Framework | Spring Boot | 3.5.14 |
| Build | Maven Wrapper | 3.9.9 (alinhar para 3.9.15+ em etapa futura) |
| Compilação | Java release flag | `<release>21</release>` (não `<source>` + `<target>`) |
| ORM | Spring Data JPA + Hibernate | gerenciado pelo BOM Spring Boot |
| Validação | Hibernate Validator (Bean Validation) | gerenciado pelo BOM Spring Boot |
| Migrations | Flyway + flyway-database-postgresql | gerenciado pelo BOM Spring Boot |
| Mapeamento DTO | MapStruct | 1.6.3 |
| Annotation Processing | Lombok antes de MapStruct (ordem obrigatória) | Lombok 1.18.46 (BOM), MapStruct processor 1.6.3 |
| Auth | Spring Security 6 + JJWT (jjwt-api/impl/jackson) | Spring Security via BOM, JJWT 0.12.7 |
| Banco | PostgreSQL | 16-alpine |
| Cache/Blacklist | Redis | 7-alpine |
| Driver Postgres | postgresql | gerenciado pelo BOM Spring Boot |
| Testes — framework | JUnit 5 + AssertJ + Mockito (uso comedido) | gerenciado pelo BOM Spring Boot |
| Testes — integração | Testcontainers (junit-jupiter + postgresql) | 1.21.4 (gerenciado pelo BOM, não 2.x) |
| Cobertura | JaCoCo Maven Plugin | 0.8.14 |
| Análise estática (estilo) | maven-checkstyle-plugin + Checkstyle engine | maven-checkstyle-plugin 3.6.0, Checkstyle 13.4.1 |
| Análise estática (bugs) | spotbugs-maven-plugin | 4.9.8.3 |
| API doc | springdoc-openapi-starter-webmvc-ui | 2.8.17 |

> **Política de versões:** versões fixadas explicitamente em `pom.xml` para dependências não gerenciadas pelo BOM do Spring Boot. Para gerenciadas pelo BOM, deixar sem `<version>` no `pom.xml` (BOM do parent resolve). Atualizações de versão exigem novo PR justificando — não atualizar em massa sem necessidade.

### Ambiente de desenvolvimento

| Componente | Escolha |
|---|---|
| Sistema operacional dev | Windows nativo (não WSL2) |
| Shell | PowerShell |
| Container runtime | Docker Desktop |
| Versionamento | Git for Windows (`git version 2.45+`) |
| GitHub CLI | `gh` instalado e autenticado |
| Editor recomendado | qualquer editor que respeite `.editorconfig` (a ser criado se necessário) |

**Pegadinhas conhecidas de ambiente Windows** (lista viva, atualizada por etapa):

- PowerShell padrão lê UTF-8 errado sem `-Encoding UTF8` explícito (acentos viram `Ã³`, `Ã§`)
- `Out-File -Encoding UTF8` adiciona BOM por default — `javac` rejeita arquivos `.java` com BOM
- Comandos Unix (`tail`, `head`, `grep`, `sed`, `awk`) **não existem** no PowerShell. Equivalentes: `Select-Object -Last/-First`, `Select-String`
- `python` é o binário padrão (não `python3`)
- Maven Wrapper gerado no Windows não vem com bit de execução no git index. Linux Ubuntu (CI) precisa do bit. Solução: `git update-index --chmod=+x mvnw`

### Frontend

| Componente | Escolha | Versão |
|---|---|---|
| Framework | Next.js | 16.2.6 |
| Bundler | Turbopack (default Next 16) | gerenciado pelo Next |
| Linguagem | TypeScript (strict) | ^5 |
| Estilização | Tailwind CSS | ^4 |
| Lint | ESLint (config Next) | eslint-config-next 16.2.6 |
| Componentes | shadcn/ui (copy, não dependência) | shadcn 4.7.0, style base-nova |
| HTTP / cache | @tanstack/react-query | ^5.100.9 |
| Validação | Zod | ^4.4.3 |
| Forms | React Hook Form + @hookform/resolvers | ^7.75.0 / ^5.2.2 |
| React | React | 19.2.4 |
| Node.js | mínimo 20.9, recomendado 22 LTS | — |

### Infraestrutura

| Componente | Escolha |
|---|---|
| Deploy | Railway no MVP (Fly.io como alternativa) |
| CI | GitHub Actions |
| Container local | Docker + Docker Compose |
| Observabilidade | Logfire ou Sentry (a definir no MVP) |
| Feature flags | Postergar — flags simples em DB se necessárias no MVP |
| Runner CI | `ubuntu-latest` |
| Java distribution no CI | Temurin 21 (via `actions/setup-java@v4`) |
| Cache CI | Maven via `cache: maven` do `setup-java@v4` (não combinar com `actions/cache@v4` separado) |

---

## Arquitetura

### Estrutura de pacotes do backend

```
com.laboratorio.financas/
├── {contexto}/                       # bounded context = módulo lógico
│   ├── domain/                       # POJOs, value objects, ports (interfaces)
│   ├── application/                  # use cases (1 classe = 1 caso de uso)
│   ├── infrastructure/               # JPA entities, adapters, mappers
│   └── interfaces/                   # @RestController, request/response DTOs
└── shared/
    ├── domain/                       # Money e demais VOs compartilhados
    └── infrastructure/               # configs globais (security, OpenAPI, etc)
```

**Endpoints técnicos** (healthcheck, métricas, debug) ficam em `shared/infrastructure/web/`, não em bounded context próprio. Bounded contexts são reservados pra domínio de negócio. Precedente: `HealthcheckController` (Etapa 2.3).

### Regras duras (não-negociáveis)

1. **Domínio não conhece Spring nem JPA.** Zero anotação de framework em classes de `domain/`.
2. **Entidade JPA ≠ entidade de domínio.** `TransacaoEntity` (JPA) ≠ `Transacao` (domínio). MapStruct converte na borda da infra.
3. **Dependências apontam para dentro.** `interfaces` → `application` → `domain`. `infrastructure` → `domain` (implementa portas).
4. **Use case = classe.** Padrão de nome: `<Verbo><Substantivo>UseCase` (ex: `CriarTransacaoUseCase`, `ListarTransacoesPorPeriodoUseCase`).
5. **Repository pattern.** Interface no domínio (`TransacaoRepository`), implementação na infra (`TransacaoRepositoryImpl`) que delega a `TransacaoJpaRepository extends JpaRepository`.
6. **Money desde o dia 1.** Toda quantia monetária é `Money`, nunca `BigDecimal` cru, nunca `double`, nunca `float`.

   **Implementação de `Money`** (a partir da Etapa 3.1): record imutável em `shared/domain` com `BigDecimal valor` (escala 2 fixa, `RoundingMode.HALF_EVEN`) e `Currency moeda`. Operações: `somar`, `subtrair`, `multiplicar(BigDecimal)`, `negar`, `ehZero`, `ehNegativo`, `ehPositivo`. Operações entre `Money` exigem mesma moeda — moedas diferentes lançam `IllegalArgumentException`. Métodos adiados (porta aberta): `dividir`, `percentual`, formatação localizada, `Comparable`, conversão entre moedas.

7. **DTOs em três níveis.** `*Request`/`*Response` na API; `*Command`/`*Query` na entrada do use case quando necessário; `*Entity` na persistência. Não vazam entre camadas.
8. **Schema é declarado em SQL via Flyway.** `spring.jpa.hibernate.ddl-auto` é `validate` em todos os ambientes, nunca `update` ou `create`.
9. **Senhas com BCrypt cost 12.** Sem exceção.

### Padrões aplicados

- **Use case com construtor explícito** (sem `@Autowired` em campo). Spring resolve via construtor.
- **Imutabilidade preferencial** em entidades de domínio. Usar `record` para value objects sempre que possível.
- **Exceções tipadas** por contexto: `TransacaoNaoEncontradaException`, `SaldoInsuficienteException`. Sem `RuntimeException` genérica.
- **Validação em duas fases:**
  - Bean Validation nos `*Request` DTOs (formato, tamanho, presença)
  - Validação de invariantes de negócio dentro da entidade ou use case (regras de domínio)
- **Igualdade em entidades de domínio**: por `id`, não por valor. `equals` e `hashCode` implementados manualmente baseados apenas em `id`. Diferente de value objects (Money, etc) que usam record com igualdade estrutural. Estabelecido na Etapa 3.2 com `Conta`.
- **Persistência de value objects compartilhados** (a partir da Etapa 3.3): VOs do domain (`Money`, etc) são mapeados para `*Embeddable` em `shared/infrastructure/persistence/`. MapStruct converte na borda. Domain permanece framework-free. `@AttributeOverride` na entidade hospedeira define os nomes de coluna concretos.
- **MapStruct mappers**: anotação `@Mapper(componentModel = "spring")` sempre explícita. Argumento global `-Amapstruct.defaultComponentModel=spring` no pom.xml ainda gera warning recorrente "options were not recognized" — explicitar no `@Mapper` é o mecanismo confiável.
- **Use case = classe** (a partir da Etapa 3.4): 1 classe = 1 caso de uso. Construtor explícito recebe dependências (repositório, outros use cases quando necessário). Método público `executar(...)` é o entry point único. `@Transactional` aplicado no método; `@Transactional(readOnly = true)` em casos puramente de leitura. Comando como record interno do use case quando há mais de 2 parâmetros.
- **Composição de use cases**: use cases não dependem entre si. Cada use case acopla apenas ao `Repository`. Duplicação local de lógica de busca + lança exceção é aceita em troca de evitar dependência transitiva.
- **Tratamento de exceções via `@RestControllerAdvice`** com `ProblemDetail` (RFC 7807): handler global em `shared/infrastructure/web/GlobalExceptionHandler.java`. Mapeia `MethodArgumentNotValidException` → 400 com lista de campos, `ContaNaoEncontradaException` → 404, `IllegalArgumentException` → 400, `Exception` (catch-all) → 500 com log de stack trace e mensagem genérica.
- **Conversão DTO ↔ Domain via método estático `fromDomain` no DTO de resposta**, não MapStruct. Tradução é trivial e MapStruct exigiria método `default` de qualquer forma. Mapper continua usado para Entity ↔ Domain (3.3).

- **Relacionamentos entre bounded contexts via UUID, não @ManyToOne** (a partir da Etapa 3.6): entidades JPA referenciam outros agregados por `UUID` direto (`@Column(name = "conta_id")`), não por `@ManyToOne ContaEntity`. FK constraint existe no banco, validação de existência fica no use case. Razões: baixo acoplamento entre bounded contexts, evita `LazyInitializationException`, queries mais previsíveis. Padrão idiomático em DDD com bounded contexts.
- **Defesa em profundidade no banco** (a partir da Etapa 3.6): regras de domínio importantes (valor positivo, regras de transferência) são duplicadas em `CHECK` constraints do banco. Domain valida na escrita, banco valida no commit. Custo zero, ganho de robustez contra bypass do domain.
- **Paginação via `Page<T>` e `Pageable` do Spring Data** (a partir da Etapa 3.7): aceito como exceção pragmática à regra "domain não conhece framework". Alternativa seria criar abstrações próprias (`PaginaResultado<T>`, `Paginacao`) que adicionam camada sem ganho real. `Page` e `Pageable` são interfaces, não anotações.
- **Validação de FK no use case via `Optional.isEmpty()` dos repositórios** (a partir da Etapa 3.7): use cases que criam/editam entidades com FK validam existência das referências antes de construir a entidade. Lança `*ComReferenciaInvalidaException` (extends `RuntimeException`) com nome do recurso e id, mapeada como 400 no handler global. Validação no banco (FK constraint) continua existindo como defesa em profundidade.
- **Filtros opcionais via JPQL com sentinelas de data** (a partir da Etapa 3.7): cada filtro não-date usa `:param IS NULL OR campo = :param` no JPQL. Filtros de data usam comparação direta (`t.data >= :dataInicio AND t.data <= :dataFim`) com sentinelas `LocalDate.of(1900, 1, 1)` e `LocalDate.of(9999, 12, 31)` no `RepositoryImpl` quando nulos — evita erro PostgreSQL "could not determine data type of parameter" para nulls de LocalDate. Sem `Specification`. Boring tech.
- **Cruzamento entre bounded contexts via porta no domain** (a partir da Etapa 3.8): quando um bounded context precisa ler estado de outro (ex: `Conta` calcular saldo a partir de `Transacao`), o consumidor define o método na interface de repositório do produtor (ex: `TransacaoRepository.calcularTotaisPorConta`). Implementação fica em `transacao/infrastructure/`. Sem use case do bounded context A chamando use case do B — apenas via portas (interfaces de domínio). Mantém baixo acoplamento entre contextos.
- **Agregação SQL com `COALESCE(SUM(CASE WHEN...), 0)`** (a partir da Etapa 3.8): para totais condicionais por tipo, usar `SUM(CASE WHEN tipo = X THEN valor ELSE 0 END)` envelopado em `COALESCE(..., 0)` para garantir zero em conjunto vazio. JPQL `SELECT new com.path.Record(...)` com tipo path completo + enums fully-qualified.
- **`mvnw clean verify` antes de declarar etapa pronta** (consolidado na Etapa 3.8 a partir de incidente da 3.7): build local sem `clean` pode dar falso positivo por cache de compilação. CI sempre roda do zero — local deve replicar isso na validação final.

### Padrões adiados (porta aberta, não aplicar preventivamente)

- Agregado raiz com invariantes — quando saldo derivado exigir consistência forte
- Domain events — quando houver múltiplos reatores para um mesmo evento
- CQRS — quando queries de relatório justificarem leitura otimizada separada
- Bounded contexts em módulos Maven separados — quando contexto crescer o suficiente

---

## Convenções de código

### Nomenclatura

- **Idioma:** **Português** para nomes de domínio (`Transacao`, `Conta`, `Categoria`, `criarTransacao`). **Inglês** para termos técnicos universais (`Repository`, `UseCase`, `Mapper`, `Request`, `Response`, `findById`).
- **Acentos em código:** **não usar** — `Transacao` em vez de `Transação`. Documentação e UI podem usar acento.
- **Classes:** PascalCase. Métodos e variáveis: camelCase. Constantes: SCREAMING_SNAKE_CASE.
- **Pacotes:** lowercase, sem underscore.

### Estilo

- **Lombok:** uso comedido. Permitido: `@RequiredArgsConstructor`, `@Slf4j`, `@Getter` em entidades JPA. **Evitar:** `@Data`, `@Builder` em domain (preferir record).
- **`var` em locais:** permitido quando o tipo é óbvio do lado direito. Evitar quando ambíguo.
- **Optional:** retorno de busca por ID. Nunca como parâmetro. Nunca em campo.
- **Stream:** preferir sobre for-each quando há transformação. Evitar para side-effects.
- **Imports:** organizados, sem wildcard salvo `java.util.*` em testes.

### Spring específico

- **`@Transactional`** sempre **explícito** no método (não na classe). Sempre `readOnly = true` em queries.
- **Profiles** sempre explícitos: `dev`, `test`, `prod`. Nada de "sem profile".
- **Scripts que invocam `mvnw spring-boot:run` sempre passam `-Dspring-boot.run.profiles=<profile>` explicitamente.** Sem isso, Spring cai no profile `default` (que tem apenas `application.yml`, sem datasource), levando a `Failed to configure a DataSource`. Bug consolidado na Etapa 3.3.1, descoberto em validação destrutiva manual da 3.3. Hook futuro vai validar que `dev.ps1` (e equivalentes) passam a flag.
- **`@Component` / `@Service` / `@Repository`** usados no papel correto, não intercambiavelmente.
- **Configuração via `@ConfigurationProperties`** com record imutável, não `@Value` espalhado.

- **`SecurityFilterChain` com whitelist explícita.** Endpoints públicos são listados em `requestMatchers(...).permitAll()`; o resto é `authenticated()`. Enquanto JWT não está implementado (Camada 2), `authenticated()` funciona como bloqueio efetivo (qualquer request não-whitelisted retorna 401). Whitelist atual: `/api/healthcheck`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`. Quando JWT entrar, `authenticated()` é substituído por filtro JWT — não relaxar pra `permitAll()` global em hipótese alguma. **Atenção:** Spring Security 6 sem `AuthenticationEntryPoint` explícito retorna 403 (não 401) para requisições não autenticadas com `httpBasic` desabilitado — configurar entry point explícito para garantir 401 semanticamente correto.

### Configuração crítica do `pom.xml`

Decisões obrigatórias do `pom.xml` que **não devem ser alteradas sem novo ADR**:

- `<release>21</release>` no `maven-compiler-plugin` (não usar `<source>` + `<target>`)
- `<annotationProcessorPaths>` com **Lombok antes de MapStruct** (inverter quebra build)
- `<compilerArgs>` com `-Amapstruct.defaultComponentModel=spring` (mappers gerados como `@Component`; warning "options were not recognized" é esperado quando ainda não há `@Mapper` no projeto)
- `<scope>` correto:
  - Lombok: `provided`
  - JJWT impl/jackson: `runtime`
  - Test deps: `test`
  - MapStruct: runtime padrão (não declarar scope explícito)
- Spring Boot Maven Plugin com excludes do Lombok no `repackage`
- JaCoCo plugin com 3 execuções: `prepare-agent` + `report` + `check`. Regras ativas hoje: BUNDLE 75% (global), PACKAGE `**.infrastructure.*` 60%. Regras de `domain`/`application`/`interfaces` (90%/80%/70%) ficam **comentadas** no `pom.xml` aguardando primeira classe nesses pacotes (Camada 2). Exclusão única: `FinancasApplication.class`.
- Checkstyle plugin: phase `validate`, severidade `error`, configuração externa em `config/checkstyle/checkstyle.xml`. Base Google Style com overrides: indentação 4 espaços (default Google é 2), linha 140 chars (default Google é 100), regras de Javadoc obrigatório suprimidas.
- SpotBugs plugin: phase `verify`, effort `max`, threshold `medium`, exclude filter em `config/spotbugs/spotbugs-excludes.xml`. Excludes iniciais: `FinancasApplication`, records de `*Response`/`*Request` (EI_EXPOSE_REP), classes `*Config` (UPM_UNCALLED_PRIVATE_METHOD).

### Testes

- **Naming de classe de teste:** sufixo `Test` (singular) é o padrão. Sufixo `Tests` (plural) é tolerado em classes geradas pelo Spring Initializr (`FinancasApplicationTests`) e não deve ser usado em classes novas. Sufixo `IT` (convenção Maven Failsafe) **não é usado** neste projeto — Failsafe não está configurado e Surefire não pega esse sufixo por default.
- **Naming de método de teste:** camelCase puro, sem underscore (restrição do Checkstyle `MethodName` do projeto, que aplica `^[a-z][a-zA-Z0-9]*$`). Estrutura tripartite recomendada quando aplicável: `<metodoTestado><CenarioDoTeste><ResultadoEsperado>` em camelCase. Exemplos: `somarDoisValoresPositivosRetornaSoma`, `construtorComValorNuloLancaNullPointerException`. Tentar manter sob 60 chars; se ficar maior, encurtar preservando intenção. (Nota: versões anteriores deste doc prescreviam underscore — corrigido na Etapa 3.1 para alinhar ao código vivo e ao Checkstyle.)
- **Padrão AAA explícito:** comentários `// Given`, `// When`, `// Then` em testes não-triviais.
- **Não compartilhar estado entre testes.** `@DirtiesContext` quando inevitável, mas evitar.
- **Testcontainers via `@Container` static** para reuso entre testes da mesma classe.

### Análise estática

- **Checkstyle severidade `error` para tudo.** Sem categoria warning silenciosa. Cada supressão é decisão consciente registrada em `config/checkstyle/checkstyle.xml`.
- **SpotBugs excludes estreitos.** Filtros sempre com `<Class>` específico ou `<Bug pattern>` específico. Filtros amplos (ex: pacote inteiro) exigem novo ADR.
- **Javadoc não é obrigatório nesta fase.** Regras `JavadocMethod`, `JavadocType`, `JavadocVariable`, `MissingJavadocMethod`, `MissingJavadocType` ficam desabilitadas. Reativação fica como decisão futura.

### Cobertura mínima por camada (JaCoCo)

| Camada | Meta |
|---|---|
| `domain/` | 90% |
| `application/` | 80% |
| `infrastructure/` | 60% |
| `interfaces/` | 70% |

CI falha se cobertura agregada cair abaixo de 75%.

**Status atual de aplicação dos thresholds (Etapas 2.4, 3.1 e 3.4):**

- ✅ **Ativos:** BUNDLE 75% (global), `infrastructure` 60%, `domain` 90%, `application` 80%, `interfaces` 70%

---

## Convenções operacionais

### Git

- **Branches:** `main` é deployable. Trabalho em `feature/<nome-curto>`, `fix/<nome-curto>`, `refactor/<nome-curto>`.
- **Conventional Commits obrigatórios:** `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`, `build:`, `ci:`.
- **Squash merge** em PRs. Mensagem do squash segue Conventional Commits.
- **Sem commit direto em `main`.** Sempre via PR, mesmo trabalhando solo.

### Migrations Flyway

- **Naming:** `V{N}__{descricao_em_snake_case}.sql` (ex: `V3__cria_tabela_transacao.sql`).
- **Append-only após merge.** Schema fix é nova migration, nunca edição de existente.
- **Toda alteração de `@Entity` JPA exige migration correspondente no mesmo PR.** Hook futuro vai validar isso.
- **`baseline-on-migrate` por profile:** `false` em `application.yml` (defaults) e `application-prod.yml`; `true` apenas em `application-test.yml` (e `application-dev.yml` se existir). Baseline silencioso em prod e fonte classica de inconsistencia.

### Configuração de ambiente

- `application.yml` — defaults
- `application-dev.yml` — dev local
- `application-test.yml` — testes
- `application-prod.yml` — produção
- **Secrets nunca em YAML.** `${VAR_NAME}` referenciando env, com `.env.example` versionado e `.env` no `.gitignore`.

### Scripts PowerShell

- **`Write-Host -ForegroundColor Red` em vez de `Write-Error` antes de `exit N`.** Sob `$ErrorActionPreference = "Stop"`, `Write-Error` lança exceção terminating, encerra o script antes do `exit N`, e em sessão dot-source o `$LASTEXITCODE` não é atualizado (fica 0 falsamente). Padrão correto:
  ```powershell
  if ($alguma_condicao_de_erro) {
      Write-Host "mensagem clara do erro" -ForegroundColor Red
      exit 1
  }
  ```
- **`Write-Error` é apropriado** apenas em scripts/módulos onde quem invoca vai capturar o erro como exceção (try/catch, pipeline com `-ErrorAction`). Em scripts user-facing chamados diretamente no terminal, prefira `Write-Host` colorido.
- **Suspender `Stop` localmente em checagens com comando nativo + redirecionamento.** Sob `$ErrorActionPreference = "Stop"`, comandos nativos (`docker`, `git`, `mvn`, etc) que escrevem em stderr fazem o PowerShell vazar o erro pra tela com stack trace, **antes** que operadores de redirecionamento (`2>&1`, `2>$null`, `2>&1 > $null`) possam suprimir. Não há sintaxe de redirecionamento que evite. Padrão correto:
  ```powershell
  $prev = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  docker info 2>&1 | Out-Null
  $ErrorActionPreference = $prev

  if ($LASTEXITCODE -ne 0) {
      Write-Host "mensagem clara do erro" -ForegroundColor Red
      exit 1
  }
  ```
  Aplicar este padrão sempre que o script chamar comando nativo com intenção de checar `$LASTEXITCODE` em vez de tratar como erro fatal.
- **Manter `$ErrorActionPreference = "Stop"`** no topo dos scripts. A regra acima é apenas para o fluxo "validação detectou problema, sair com código de erro". Para erros inesperados de comandos nativos (ex: Maven crashar), `Stop` continua sendo o comportamento desejado.

---

## Política de débito técnico consciente

Débitos técnicos planejados (decisões temporárias com data de resolução conhecida) **devem ser explícitos**, não silenciosos:

1. **No commit**: mensagem do commit menciona o débito (ex: "ci: exclui FinancasApplicationTests temporariamente até Etapa 2.1").
2. **No PR body**: seção explícita "Débito técnico consciente" descreve o quê, por quê é temporário, e quando será resolvido.
3. **No próprio código** (se possível): comentário acima da linha relevante referenciando a etapa de resolução.
4. **No `progresso.md`**: seção de lições da etapa registra o débito como pendência.

Exemplo real: exclusão de `FinancasApplicationTests` no `ci.yml` da Etapa 1.5, a ser removida na Etapa 2.1 quando Testcontainers configurar o datasource de teste.

**Não confundir débito consciente com bug ou esquecimento.** Débito consciente é decisão; bug é falha; esquecimento é processo ruim.

---

## Comandos atômicos do projeto

Scripts PowerShell em `scripts/`. Padrão da fábrica:

| Comando | Função |
|---|---|
| `.\scripts\setup.ps1` | Sobe Postgres + Redis no Docker, instala deps, compila |
| `.\scripts\dev.ps1` | Sobe backend em modo dev (`mvnw spring-boot:run`) |
| `.\scripts\test.ps1` | Ciclo rápido: apenas `mvnw test` (sem análise estática) |
| `.\scripts\test-integration.ps1` | Testes + JaCoCo, pula Checkstyle/SpotBugs |
| `.\scripts\check.ps1` | Gate completo (`mvnw verify`). Equivalente ao CI. |
| `.\scripts\ship.ps1` | `check.ps1` + push. Sugere comando pra abrir PR. |

**Encoding dos scripts:** UTF-8 sem BOM, obrigatoriamente. PowerShell `Out-File -Encoding UTF8` adiciona BOM e é proibido em arquivos `.ps1`. Usar `[System.IO.File]::WriteAllText(<path>, <conteudo>, (New-Object System.Text.UTF8Encoding $false))` ou método equivalente sem BOM.

**Criação automática de `.env`:** `setup.ps1` e `dev.ps1` criam `.env` automaticamente a partir de `.env.example` quando `.env` está ausente, com aviso amarelo. Se `.env.example` também estiver ausente, o script falha com mensagem clara (repositório corrompido). Padrão alinhado ao princípio "scripts de setup à prova de ambiente zero" (consolidado na retrospectiva da Camada 1).

**Pré-requisito Windows:** `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` (uma vez por usuário). Documentado no README.

---

## Frontend

Aplicação Next.js em `frontend/`.

**Decisões registradas:**

- **Localização:** `frontend/` na raiz do repo. Sem ferramenta de monorepo (workspaces, turborepo, nx). Razão: simplicidade. Migrar para monorepo formal só quando justificar.
- **PWA adiada para Camada 2.** Package `next-pwa` não é mantido desde 2024; sucessor `serwist` ou abordagem nativa via `manifest.ts` ficam como decisão da Camada 2 quando houver telas reais.
- **shadcn/ui via copy.** Componentes vão para `src/components/ui/` quando instalados via `npx shadcn add <componente>`. Não é dependência de runtime — o código fica versionado no repo. Style `base-nova`, baseColor `neutral`, CSS variables ativas.
- **`AGENTS.md` e `CLAUDE.md` do scaffold mantidos.** `AGENTS.md` contém aviso do Next.js 16 sobre breaking changes em relação a training data de agentes; `CLAUDE.md` usa sintaxe `@AGENTS.md` para carregar o aviso como contexto quando o Claude Code está trabalhando em `frontend/`. Não conflita com `CLAUDE.md` da raiz (sintaxe aditiva).
- **CI:** job único com Java + Node executados sequencialmente. Refatorar para dois jobs paralelos só quando justificar.

---

## Modelo financeiro do projeto

Atual:
- **Mês 1-2:** apenas Max 5x ($100/mês). Sem conta API ainda.
- **Configurar até fim do mês 1:** conta em console.anthropic.com com hard limit $30/mês — não usar ainda, apenas pré-configurar.

Próximas reavaliações:
- **Fim do mês 2:** decidir se primeiras routines Tier 1 movem para API ou seguem no Max.
- **Fim do mês 4:** decidir se overflow para API justifica budget acima de $50/mês.
- **Fim do mês 6:** decidir se Max 20x faz sentido (só se Max 5x travar uso interativo).

---

## Camada 3 — Configuração do Claude Code

**Início:** 2026-05-10 (Sub-etapa 4.0).

### Layout de `.claude/`

`.claude/` é a casa da fábrica no projeto. Organizada por escopo de aplicabilidade conforme ADR-009: `universal/`, `java-spring/`, `windows/`, `next/`, `local/` dentro de `hooks/`, `agents/` e `skills/`. Promoção entre escopos exige evidência explícita (segundo contexto + decisão consciente).

### Mecanismo de git hooks no Windows

`git config core.hooksPath .githooks` configurado automaticamente por `scripts/setup.ps1`. Entrypoints em `.githooks/` são wrappers bash sem extensão chamando companheiros `.ps1`. Lógica real fica em `.claude/hooks/<escopo>/`.

### Débito de portabilidade

ADR-010 registra aceitação consciente: hooks e scripts PowerShell-specific. Migração avaliada ao entrar Camada 5 ou nascer 2ª fábrica em outro SO. Custo estimado: 1-3 dias.

### Conventional Commits (Sub-etapa 4.1)

**Tipos permitidos:** feat, fix, chore, docs, test, refactor, style, perf, build, ci.

**Formato:** `<tipo>[(scope)][!]: <descricao>` com pelo menos 10 caracteres na descricao.

**Scope:** opcional. Lowercase + digitos + hifen entre parenteses. Convencao do projeto usa nome do modulo (`feat(transacao):`, `chore(scripts):`).

**Breaking change:** indicado por `!` apos scope (`feat!:` ou `feat(api)!:`).

**Excecoes automaticas:** mensagens iniciadas por `Merge ` ou `Revert ` (geradas pelo git) passam sem validacao.

**Override consciente:** `git commit --no-verify` e escape valido em emergencias (bug critico em producao, hotfix que justifica pular validacao). Cada invocacao deve ser registrada no PR body com motivo. Sem policia automatica — disciplina por norma.

**Hook implementado em:** `.claude/hooks/universal/conventional-commits.ps1`, invocado por `.githooks/commit-msg` (entrypoint bash) -> `.githooks/commit-msg.ps1` (companheiro PowerShell).

### Encoding UTF-8 (Sub-etapa 4.2)

**Regra:** arquivos de texto staged devem ser UTF-8 valido. Arquivos `.ps1` adicionalmente NAO podem ter BOM (licao da Etapa 2.6).

**Whitelist por extensao:** `.md`, `.java`, `.yml`, `.yaml`, `.xml`, `.properties`, `.ps1`, `.sql`, `.ts`, `.tsx`, `.js`, `.jsx`, `.json`, `.css`, `.html`.

**Whitelist por nome exato:** `.gitignore`, `.gitattributes`, `.editorconfig`, `.env.example`. Arquivos sem extensao dentro de `.githooks/` (entrypoints bash) tambem incluidos.

**Fora da whitelist:** binarios (`.png`, `.jpg`, `.pdf`) e tipos nao listados (`.toml`, etc) passam silenciosamente. Adicionar item a whitelist quando primeiro caso real surgir — decisao consciente, nao automatica.

**Filtro do diff:** `git diff --cached --name-only --diff-filter=ACM` (Added, Copied, Modified). Deleted e Renamed-rename-only sao ignorados (sem conteudo a validar).

**Sem deteccao por conteudo** (`file --mime` ou similar) — coerente com ADR-009 ("sem dependencias externas, PowerShell puro").

**Hook implementado em:** `.claude/hooks/universal/encoding-utf8.ps1`, invocado por `.githooks/pre-commit` -> `.githooks/pre-commit.ps1` (orquestrador 1:N).

### Padrao orquestrador 1:N para `pre-commit` (Sub-etapa 4.2)

O entrypoint companheiro `.githooks/pre-commit.ps1` e desenhado como **orquestrador**, diferente do `commit-msg.ps1` da 4.1 que e delegador 1:1. Razao: varias validacoes distintas (encoding, blank lines em Markdown, tamanho de docs, etc) precisam rodar antes de cada commit. Array `$hooks` no orquestrador e o ponto de extensao — sub-etapas seguintes da Camada 3 acrescentam linhas a esse array.

**Execucao em sequencia, nao paralela.** Cada hook le seu proprio `git diff --cached`. Se multiplos hooks falharem, todos reportam suas mensagens (sem early-exit no primeiro fail) — operador ve todas as violacoes de uma vez.

**Sem contrato compartilhado entre hooks** alem de: "exit 0 = ok, exit != 0 = bloqueia".

### Padroes de validacao destrutiva (Sub-etapa 4.2.1)

Ratificado em ADR-011. Toda validacao destrutiva — na branch da etapa ou em smoke test pos-merge — segue tres regras:

1. **Pre-condicao explicita apos cada criacao de arquivo:** `Test-Path` (ou equivalente). Se `False`, parar.
2. **`git status` antes de `git commit`** para confirmar arquivo staged. Se `nothing to commit`, parar.
3. **Verificacao de exit code apos comando que deveria falhar.** Esperar codigo `!= 0`; se vier `0`, cenario nao reproduziu erro esperado.

**Para PowerShell + `[System.IO.File]::WriteAllText` com path relativo:** sincronizar previamente:

```powershell
[System.Environment]::CurrentDirectory = (Get-Location).Path
```

Ou usar path absoluto (`"$PWD\arquivo"`). Sem isso, arquivo e gravado em diretorio que pode divergir do `$PWD` (gotcha confirmado em smoke test pos-merge da 4.2).

**Reportar pre-condicoes verificadas no PR body** — nao basta listar "cenarios validados". Listar tambem o valor observado em cada pre-condicao. Falsos positivos silenciosos sao detectados apenas por verificacao explicita.

**Aplica retroativamente:** sub-etapas 4.3+ devem incluir esses gates no roteiro de validacao destrutiva. Sub-etapas 4.0 a 4.2 ja mergeadas nao sao revistas — confirmacao empirica posterior (smoke test pos-merge da 4.2 corrigido) validou que o codigo dessas sub-etapas esta correto.

### Blank lines em Markdown (Sub-etapa 4.3)

**Regra:** arquivos `.md` staged devem ter linha em branco antes E depois de cada header de nivel 2-6 (`##` ate `######`). Headers de nivel 1 (`#`) sao ignorados (tipicamente titulo do documento).

**Escopo:** apenas `.md` (nao `.markdown`, nao `.mdx`). Qualquer pasta (nao restrito a `docs/`).

**Fronteira do arquivo e linha em branco implicita:** header na primeira linha nao precisa de linha em branco antes. Header na ultima linha nao precisa de linha em branco depois.

**Headers dentro de blocos de codigo (`` ``` ``)** sao ignorados — sao exemplos, nao headers reais. Blocos indentados com 4 espacos nao sao cobertos (limitacao consciente; raro em pratica moderna).

**Hook implementado em:** `.claude/hooks/universal/markdown-blank-lines.ps1`, segundo hook a viver dentro do orquestrador `pre-commit` (4.2). Apenas uma linha adicionada ao array `$hooks` em `.githooks/pre-commit.ps1` — sem refatorar arquitetura.

### Tamanho de docs em modo warn (Sub-etapa 4.4)

**Regra:** arquivos `.md` em `docs/` (qualquer nivel de profundidade) com mais de 800 linhas totais geram alerta visual no terminal durante o commit. **Commit prossegue normalmente — alerta nao bloqueia.**

**Escopo:** apenas `docs/*.md`. Outros `.md` (README raiz, `.github/`, `frontend/`, etc.) sao ignorados.

**Metrica:** linhas totais via `[System.IO.File]::ReadAllLines($path).Count`. Inclui linhas em branco — simples, alinhado com como o operador ve o arquivo.

**Limite:** 800 linhas. Folga sobre o `progresso.md` atual (~680) e `decisoes.md` (~470), com espaco para crescimento natural ao longo das Camadas 3 a 6.

**Padrao novo estabelecido — modo `warn` para regras subjetivas:**

Tamanho de doc nao tem "valor errado". 600 linhas pode ser certo para um doc denso; 1500 pode ser certo para um indice completo. Bloquear forcaria split apressado em momentos inoportunos. Por isso, hooks de **regras subjetivas** seguem padrao `warn`: alertam no terminal mas saem com exit code 0, deixando ao operador a decisao de agir.

Hooks de **regras objetivas** continuam em modo `fail` (Conventional Commits, encoding UTF-8, blank lines em Markdown). Modo do hook e parte do design, registrada em `decisoes.md` quando o hook nasce.

**Override:** nao aplicavel — hook nao bloqueia. `--no-verify` continua valido se necessario para outros hooks da pipeline.

**Hook implementado em:** `.claude/hooks/universal/docs-size.ps1`, terceiro hook no orquestrador `pre-commit` (1:N da 4.2). Apenas uma linha adicionada ao array `$hooks` em `.githooks/pre-commit.ps1`.

**Fecha o lote universal de Markdown** — apos 4.4, proximas universais (se houver) entram por demanda, nao pelo plano. Proximas sub-etapas focam em hooks de stack (`java-spring/`), CLAUDE.md, subagents ou skills.

### Maven release explicito (Sub-etapa 4.5)

**Regra:** se `pom.xml` esta no diff staged, deve conter pelo menos uma ocorrencia da tag `<release>` com qualquer conteudo interno. Caso contrario, commit bloqueado.

**Por que:** licao 1.4 — sem `<release>` explicito, Maven usa default que pode divergir entre dev local e CI, resultando em build inconsistente. Lab atual ja tem `<release>${java.version}</release>` configurado; hook arma regra para prevenir regressao.

**Valor da tag e livre:** `<release>21</release>`, `<release>17</release>`, `<release>${java.version}</release>` — todos passam. Hook valida presenca, nao valor. Versao Java e decisao de projeto, nao decisao de hook.

**Padrao novo estabelecido — hooks especificos de stack:**

Esta e a primeira sub-etapa a ocupar `.claude/hooks/java-spring/`. Universais (`universal/`) e especificos de stack (`java-spring/`, `next/`, `windows/`, `local/`) coexistem no orquestrador `pre-commit` sem distincao sintatica. O array `$hooks` em `.githooks/pre-commit.ps1` lista todos os hooks na ordem de registro, agnostico a escopo.

A diferenca e apenas o **criterio de aplicabilidade dentro do hook:** cada hook le `git diff --cached --name-only` e decide se vale agir. Hooks universais agem sempre (ou filtram por extensao generica como `.md`). Hooks de stack filtram por arquivos especificos da stack (`pom.xml`, `*.java`, `package.json`, etc.).

**Decisao consciente (D2 calibrada com operador):** filtro de aplicabilidade fica dentro do hook, nao no orquestrador. Razao: consistencia com 4.2-4.4. Custo de invocar hook que sai imediato com `exit 0` (quando nao se aplica) e negligivel. Centralizar filtro no orquestrador seria otimizacao prematura — so faria sentido com 20+ hooks ou com hooks pesados (parser de arquivo grande, etc).

**Hook implementado em:** `.claude/hooks/java-spring/maven-release.ps1`, quarto hook no orquestrador `pre-commit`.

### CLAUDE.md do projeto (Sub-etapa 4.6)

`CLAUDE.md` na raiz do repo carrega contexto inicial em toda sessao do Claude Code automaticamente.

**Conteudo:** identidade do projeto, stack, ambiente operacional, mecanismo de hooks (modos `fail`/`warn`), convencoes e padroes, onde buscar mais em `docs/`, lista do que nao fazer.

**Conteudo volatil NAO entra:**

- Estado atual (Camada/Sub-etapa) -- link para `docs/progresso.md`.
- Lista de hooks ativos com regras -- link para `docs/hooks-pendentes.md` (secao "Hooks implementados").
- Lista de arquivos `docs/prompt-etapa-*.md` -- proliferam, agente busca quando precisa.

CLAUDE.md menciona o **mecanismo de hooks** e os **modos `warn`/`fail`** porque sao estruturais. Lista especifica do que esta ativo fica fora.

**Alvo de tamanho:** ate 200 linhas. ~6-8KB. Limite duro: 250 linhas. Razao: CLAUDE.md entra em toda mensagem da sessao, nao so na primeira. Documento curto e denso > documento longo e completo.

**Regra de atualizacao:**

CLAUDE.md e editado **dentro da sub-etapa** que muda algo estrutural -- nao em sub-etapa propria de "atualizacao". Estrutural = stack, ambiente, convencoes, restricoes.

Sub-etapas que apenas adicionam hook **nao editam CLAUDE.md**. Hook entra na lista de `docs/hooks-pendentes.md` (que ja e linkado). Sub-etapas que avancam Camada **nao editam CLAUDE.md**. Estado vive em `docs/progresso.md` (que ja e linkado).

Esta regra entra nas Restricoes/freios dos prompts futuros: "verificar se a sub-etapa muda stack/ambiente/convencoes/restricoes. Se sim, atualizar CLAUDE.md no escopo da sub-etapa. Se nao, nao tocar em CLAUDE.md".

### Claude Code hooks nativos

Mecanismo `PreToolUse`/`Stop`/`UserPromptSubmit` em `.claude/settings.json` é tratado em sub-etapa própria após 4.2. Diferente de git hooks: atua sobre comportamento do agente, não validação de código.

---

## Princípios herdados do blueprint

Lembretes operacionais que regem decisões em chats futuros:

1. **A fábrica é o produto, o software é o subproduto.**
2. **Autonomia é limitada pela qualidade da validação.** Sem CI confiável, agentes voltam a precisar de babá.
3. **Stack on-distribution > stack vistosa.** Boring tech onde der.
4. **CLAUDE.md curto.** Alvo: 10-15KB max. Sem enciclopédia.
5. **3-5 subagents.** Não 30. Mais é gatekeeping de contexto e queda de produtividade.
6. **Tier de autonomia por contexto.** Tier 3 sem culpa quando o contexto pede.
7. **Hooks substituem olhos.** Construir hooks antes de soltar a mão.
8. **Infraestrutura segue necessidade.** VPS quando local doer, não antes.

---

## Como atualizar este documento

- **Decisão pequena (convenção, ferramenta menor):** edição direta + linha no rodapé com data e mudança.
- **Decisão estrutural (stack, arquitetura, padrão fundamental):** novo ADR primeiro, depois reflete aqui.
- **Conflito entre este documento e ADR:** ADR vence (é a fonte canônica do porquê).

### Histórico de mudanças

- **2026-05-11** — Sub-etapa 4.6 concluida: CLAUDE.md do projeto substituido (placeholder 21 linhas da Camada 1 -> versao estrutural 95 linhas, 7 secoes). Conteudo volatil delegado para `docs/` via links. Regra de atualizacao formalizada: editado apenas em sub-etapas que mudam stack/ambiente/convencoes/restricoes. Primeira sub-etapa de curadoria (nao de codigo). PR #46.
- **2026-05-11** — Sub-etapa 4.5 concluida: quinto hook funcional, primeiro de stack (java-spring). Maven `<release>` em modo fail. `.claude/hooks/java-spring/` ativada (primeira ocupacao apos 4.0). Padrao consolidado: orquestrador `pre-commit` agnostico a escopo; cada hook filtra aplicabilidade internamente. Hook preventivo — `pom.xml` atual ja tem `<release>${java.version}</release>` (licao 1.4 aplicada na Camada 1). Mergeado via PR #45.
- **2026-05-11** — Sub-etapa 4.4 concluida: quarto hook funcional. Tamanho de docs em modo warn (`.md` em `docs/` com mais de 800 linhas gera alerta sem bloquear). Estabelece padrao `warn` para regras subjetivas vs `fail` para regras objetivas. Lote universal de Markdown fechado (encoding, blank lines, tamanho). Mergeado via PR #44.
- **2026-05-10** — Sub-etapa 4.3 concluida: terceiro hook funcional. Markdown blank lines validado em `pre-commit`. Regra: headers nivel 2-6 exigem linha em branco antes e depois; nivel 1 ignorado; fronteira do arquivo e linha em branco implicita; blocos de codigo sao ignorados. Segundo hook no orquestrador 1:N — extensao trivial por linha no array `$hooks` (sem refatoracao). Primeira aplicacao de ADR-011 desde a redacao do prompt — 7 cenarios destrutivos validados com pre-condicoes explicitas (`Test-Path`, `git status`, sincronizacao de `Environment.CurrentDirectory`). Mergeado via PR #43.
- **2026-05-10** — Sub-etapa 4.2.1 concluida: registra padroes de validacao destrutiva. ADR-011 formalizado. Licao descoberta em smoke test pos-merge da 4.2 onde `[System.IO.File]::WriteAllText` com path relativo em PowerShell criou arquivos em `[System.Environment]::CurrentDirectory` (`C:\Users\rezen\`) em vez de `$PWD` (`C:\projetos\financas-lab\`). Hook nao foi invocado. Comando rodou sem erro visivel. Conferencia empirica (`Get-ChildItem C:\Users\rezen\test-*.*` vazio) confirmou que agente do Claude Code nao caiu nesse gotcha — risco existe apenas em sessoes onde `$PWD` e `Environment.CurrentDirectory` divergem. Sub-etapa doc-only — sem mudanca de codigo. Mergeado via PR #42.
- **2026-05-10** — Sub-etapa 4.2 concluida: segundo hook funcional. Encoding UTF-8 implementado em 3 camadas (entrypoint bash `.githooks/pre-commit` -> orquestrador `.githooks/pre-commit.ps1` -> hook `.claude/hooks/universal/encoding-utf8.ps1`). Whitelist por extensao + nomes exatos. Regra adicional: `.ps1` rejeita BOM. Padrao orquestrador 1:N estabelecido para `pre-commit` (preparado para 4.3+). Validacao destrutiva confirmou 5 cenarios (md valido passa, ps1 com BOM bloqueia, java Latin-1 bloqueia, png ignorado, override --no-verify bypassa). Mergeado via PR #41.
- **2026-05-10** — Sub-etapa 4.1 concluida: primeiro hook funcional do projeto. Conventional Commits implementado em 3 camadas (entrypoint bash `.githooks/commit-msg` -> companheiro `.githooks/commit-msg.ps1` -> hook `.claude/hooks/universal/conventional-commits.ps1`). Tipos permitidos: feat, fix, chore, docs, test, refactor, style, perf, build, ci. Scope opcional, breaking change via `!`, descricao minima 10 chars. Override `--no-verify` documentado como escape valido. Entrypoint usa `powershell` (Windows PowerShell 5.1, unico PS disponivel neste ambiente). Validacao destrutiva confirmou bloqueio de mensagem invalida + bypass por --no-verify. Mergeado via PR #40.
- **2026-05-10** — Sub-etapa 4.0.1 concluída: fix de posição do bloco `core.hooksPath` em `setup.ps1`. Bloco movido de depois de `docker compose up -d` + `mvnw clean install` para entre validação de `.env` e `docker compose up -d`. Bug descoberto em smoke test destrutivo pós-merge da 4.0 (clone novo com Docker em conflito de nomes). Lição registrada com categoria "prescrição de prompt insuficientemente específica". Débito do `container_name:` fixo no `docker-compose.yml` registrado em `hooks-pendentes.md`. Mergeado via PR #39.
- **2026-05-10** — Sub-etapa 4.0 concluída: abertura da Camada 3 com infraestrutura organizacional. Criada estrutura `.claude/{hooks,agents,skills}/{universal,java-spring,windows,next,local}` com pastas vazias (`.gitkeep`). `.githooks/` criado com README explicativo (sem entrypoints funcionais ainda — esperam 4.1+). `setup.ps1` configura `core.hooksPath=.githooks` automaticamente. ADR-009 (layout `.claude/` e mecanismo de hooks no Windows) e ADR-010 (débito de portabilidade aceito conscientemente) registrados. Triagem completa do `hooks-pendentes.md` mapeando cada item ao escopo de aplicabilidade. Sem hooks funcionais, sem subagents, sem skills. Mergeado via PR #38.
- **2026-05-10** — Etapa 3.8 concluída: saldo derivado da Conta. `CalcularSaldoDaContaUseCase` em `conta/application/` cruza bounded context lendo de `TransacaoRepository.calcularTotaisPorConta` (porta no domain de Transacao). Query JPQL agregada com `SUM(CASE WHEN ...)` retornando record `TotaisTransacaoPorConta` via `SELECT new`. Endpoint `GET /api/contas/{id}/saldo` retorna `SaldoResponse` com breakdown completo (saldoInicial, 4 totais, saldoAtual, calculadoEm). Camada 2 fechada. Mergeado via PR #37.
- **2026-05-09** — Etapa 3.7 concluída: bounded context `transacao` finalizado ponta a ponta. 5 use cases (Criar/Listar/Buscar/Editar/Deletar), DTO único `TransacaoRequest` para POST e PUT, `TransacaoController` com paginação (`Pageable`) e 5 filtros opcionais combináveis, 2 novas exceções (`TransacaoNaoEncontradaException`, `TransacaoComReferenciaInvalidaException`), 3 handlers globais novos (incluindo `ConstraintViolationException`), whitelist atualizada. ~55 testes. Mergeado via PR #36.
- **2026-05-09** — Etapa 3.6 concluída: bounded context `transacao` — domain + infra. Entidade `Transacao` (10 campos, validações cruzadas RECEITA/DESPESA/TRANSFERENCIA), enum `TipoTransacao`, repository (3 métodos básicos — filtros vêm na 3.7), `TransacaoEntity` (FKs por UUID, sem `@ManyToOne`), `TransacaoMapper` (MapStruct, `default` methods), `V4__cria_tabela_transacao.sql` (3 FKs + 2 CHECK constraints como defesa em profundidade), 40 testes. Application e interfaces ficam para 3.7. Mergeado via PR #35.
- **2026-05-09** — Etapa 3.5 concluída: bounded context `categoria` em **etapa única**. Domain (`Categoria`, `TipoCategoria`, `CategoriaNaoEncontradaException`, `CategoriaRepository`), infra (`CategoriaEntity`, `CategoriaMapper`, `CategoriaJpaRepository`, `CategoriaRepositoryImpl`, `V3__cria_tabela_categoria.sql`), application (4 use cases), interfaces (`CategoriaController`, 2 DTOs), handler reusado (+1 entry), whitelist atualizada. Sem hierarquia, sem seed, sem soft delete (decisoes adiadas ate justificarem). Mergeado via PR #34.
- **2026-05-09** — Etapa 3.4 concluída: bounded context `conta` finalizado ponta a ponta. 4 use cases, 2 DTOs (`CriarContaRequest`/`ContaResponse`), `ContaController` com 4 endpoints (`POST/GET/GET/DELETE /api/contas`), `GlobalExceptionHandler` com `ProblemDetail` (RFC 7807), whitelist temporária de `/api/contas/**` em `SecurityConfig` (TODO Auth). Thresholds JaCoCo `application` 80% e `interfaces` 70% ativados. Mergeado via PR #33.
- **2026-05-09** — Etapa 3.3.1 concluída: fix do `dev.ps1` para ativar profile `dev` via `-Dspring-boot.run.profiles=dev`. Bug descoberto em validação destrutiva manual pós-merge da 3.3. Mergeado via PR #32.
- **2026-05-09** — Etapa 3.3 concluída: infraestrutura do bounded context `conta`. `ContaEntity` (primeira `@Entity` real), `MoneyEmbeddable` em `shared/infrastructure/persistence/` (primeiro `@Embeddable`), `ContaMapper` (primeiro MapStruct ativo), `ContaRepository` (interface no domain), `ContaRepositoryImpl` + `ContaJpaRepository`, `V2__cria_tabela_conta.sql`, `ContaRepositoryImplTest` (11 testes integração com Testcontainers). Mergeado via PR #31.
- **2026-05-09** — Etapa 3.2 concluída: bounded context `conta` — domain puro. Entidade `Conta` (class imutável com igualdade por id), enum `TipoConta`, validações de invariante via `IllegalArgumentException`. Saldo atual deliberadamente fora desta etapa (entrará quando `transacao` aparecer). Sem JPA, sem MapStruct, sem persistência — esses ficam para 3.3. Mergeado via PR #30.
- **2026-05-09** — Etapa 3.1 concluída: value object `Money` implementado em `shared/domain` (record imutável, escala 2, HALF_EVEN). Threshold JaCoCo `domain` 90% ativado. Naming de método de teste corrigido no doc (camelCase puro, alinhado ao Checkstyle). Mergeado via PR #29.
- **2026-05-08** — Etapa 2.9 concluída: `setup.ps1` e `dev.ps1` criam `.env` automaticamente a partir de `.env.example` quando ausente. Resolve débito técnico descoberto na Etapa 2.8. Mergeado via PR #28.
- **2026-05-08** — Etapa 2.8 concluída: wrap-up Camada 1. Sem novas decisões técnicas. Documentos `retrospectiva-camada-1.md` e `hooks-pendentes.md` criados. Camada 1 marcada como ✅ concluída.
- **2026-05-08** — Etapa 2.7 concluída: frontend Next.js 16 inicializado em `frontend/`. Stack: TypeScript + Tailwind + ESLint + App Router + shadcn/ui + TanStack Query + Zod + React Hook Form. CI atualizado com steps de Node 22 + lint + build do frontend. PWA adiada para Camada 2. Seção Stack > Frontend atualizada com versões reais; seção `## Frontend` com decisões adicionada.
- **2026-05-08** — Etapa 2.6.2 concluída: fix de UX em `dev.ps1`/`test-integration.ps1`/`check.ps1` — `docker info 2>&1 | Out-Null` sob `Stop` vazava stderr nativo + stack trace do PowerShell, engolindo a mensagem amigável. Aplicado padrão "suspender Stop localmente" em torno do `docker info`. Regra adicionada na seção "Scripts PowerShell".
- **2026-05-08** — Etapa 2.6.1 concluída: fix de bug encontrado em validação manual da 2.6 — `Write-Error` + `exit 1` sob `Stop` não propagava `$LASTEXITCODE = 1` em sessão direta. Substituído por `Write-Host -ForegroundColor Red` + `exit 1` nos 5 scripts. Regra formalizada na seção "Scripts PowerShell".
- **2026-05-08** — Etapa 2.6 concluída: 6 scripts PowerShell em `scripts/` implementados (`setup`, `dev`, `test`, `test-integration`, `check`, `ship`). Diferenciação real entre `test.ps1` (rápido), `test-integration.ps1` (testes + JaCoCo) e `check.ps1` (gate completo, espelho do CI). Encoding UTF-8 sem BOM formalizado.
- **2026-05-08** — Etapa 2.5 concluída: Checkstyle e SpotBugs ativados como gates do `mvnw verify`. Configuração externa em `config/`, severidade `error`, validação destrutiva confirmada para ambos.
- **2026-05-08** — Etapa 2.4 concluída: JaCoCo `check` ativado com thresholds BUNDLE 75% e `infrastructure` 60%. Thresholds de `domain`/`application`/`interfaces` ficam comentados aguardando primeira classe (Camada 2). Validação destrutiva confirmou que `mvnw verify` falha quando cobertura cai abaixo do threshold.
- **2026-05-08** — Etapa 2.3 concluída: primeiro endpoint HTTP (`GET /api/healthcheck`), `SecurityFilterChain` mínimo com whitelist explícita, precedente sobre endpoints técnicos em `shared/infrastructure/web/`, convenção de naming de teste formalizada.
- **2026-05-08** — Etapa 2.2 concluida: primeira migration Flyway (`V1__schema_inicial.sql`) aplicada, configuracao Flyway nos profiles formalizada, regra dura sobre `baseline-on-migrate` por profile registrada.
- **2026-05-08** — Atualização pós-Camada 1 etapas 1.3 a 1.5: versões fixadas no `pom.xml` (Spring Boot 3.5.14, MapStruct 1.6.3, JJWT 0.12.7, springdoc 2.8.17, JaCoCo 0.8.14), seção "Ambiente de desenvolvimento" criada com pegadinhas Windows, configuração crítica do `pom.xml` documentada, scripts PowerShell substituem Makefile, política de débito técnico consciente formalizada.
- **2026-05-06** — Criação inicial. Stack, arquitetura, convenções e modelo financeiro consolidados a partir dos ADRs 001-008.
