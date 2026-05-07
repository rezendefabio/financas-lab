# Decisões — Stack, Padrões e Convenções

> Documento operacional. Resume **o que foi decidido** para consulta rápida em qualquer chat.
> Diferente dos ADRs, este documento não preserva contexto histórico — é a foto atual.
> Atualizado quando uma decisão muda. Para entender o **porquê** de uma decisão, ver `adrs.md`.

**Última atualização:** 2026-05-06

---

## Stack

### Backend

| Componente | Escolha | Versão |
|---|---|---|
| Linguagem | Java | 21 (LTS) |
| Framework | Spring Boot | 3.x (última estável) |
| Build | Maven | wrapper versionado no repo |
| ORM | Spring Data JPA + Hibernate | versão Spring Boot |
| Validação | Hibernate Validator (Bean Validation) | versão Spring Boot |
| Migrations | Flyway | versão Spring Boot |
| Mapeamento DTO | MapStruct | última estável |
| Auth | Spring Security 6 + JWT (jjwt) | versão Spring Boot |
| Banco | PostgreSQL | 16 |
| Cache/Blacklist | Redis | 7 |
| Testes | JUnit 5 + AssertJ + Testcontainers + Mockito | versões consistentes |
| Cobertura | JaCoCo | última estável |
| Análise estática | SpotBugs + Checkstyle | configuração mínima |
| API doc | springdoc-openapi | última estável |

### Frontend

| Componente | Escolha |
|---|---|
| Framework | Next.js 15 (App Router) |
| Linguagem | TypeScript (strict) |
| Estilização | Tailwind CSS |
| PWA | next-pwa |
| Validação | Zod |
| HTTP | TanStack Query (React Query) + fetch nativo |
| Forms | React Hook Form + Zod resolver |
| Componentes | shadcn/ui (copy, não dependência) |

### Infraestrutura

| Componente | Escolha |
|---|---|
| Deploy | Railway no MVP (Fly.io como alternativa) |
| CI | GitHub Actions |
| Container local | Docker + Docker Compose |
| Observabilidade | Logfire ou Sentry (a definir no MVP) |
| Feature flags | Postergar — flags simples em DB se necessárias no MVP |

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

### Regras duras (não-negociáveis)

1. **Domínio não conhece Spring nem JPA.** Zero anotação de framework em classes de `domain/`.
2. **Entidade JPA ≠ entidade de domínio.** `TransacaoEntity` (JPA) ≠ `Transacao` (domínio). MapStruct converte na borda da infra.
3. **Dependências apontam para dentro.** `interfaces` → `application` → `domain`. `infrastructure` → `domain` (implementa portas).
4. **Use case = classe.** Padrão de nome: `<Verbo><Substantivo>UseCase` (ex: `CriarTransacaoUseCase`, `ListarTransacoesPorPeriodoUseCase`).
5. **Repository pattern.** Interface no domínio (`TransacaoRepository`), implementação na infra (`TransacaoRepositoryImpl`) que delega a `TransacaoJpaRepository extends JpaRepository`.
6. **Money desde o dia 1.** Toda quantia monetária é `Money`, nunca `BigDecimal` cru, nunca `double`, nunca `float`.
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
- **`@Component` / `@Service` / `@Repository`** usados no papel correto, não intercambiavelmente.
- **Configuração via `@ConfigurationProperties`** com record imutável, não `@Value` espalhado.

### Testes

- **Nomes de teste:** `metodoTestado_cenarioDoTeste_resultadoEsperado` (ex: `criarTransacao_comValorNegativo_lancaExcecao`).
- **Padrão AAA explícito:** comentários `// Given`, `// When`, `// Then` em testes não-triviais.
- **Não compartilhar estado entre testes.** `@DirtiesContext` quando inevitável, mas evitar.
- **Testcontainers via `@Container` static** para reuso entre testes da mesma classe.

### Cobertura mínima por camada (JaCoCo)

| Camada | Meta |
|---|---|
| `domain/` | 90% |
| `application/` | 80% |
| `infrastructure/` | 60% |
| `interfaces/` | 70% |

CI falha se cobertura agregada cair abaixo de 75%.

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

### Configuração de ambiente

- `application.yml` — defaults
- `application-dev.yml` — dev local
- `application-test.yml` — testes
- `application-prod.yml` — produção
- **Secrets nunca em YAML.** `${VAR_NAME}` referenciando env, com `.env.example` versionado e `.env` no `.gitignore`.

---

## Comandos atômicos do projeto (alvo)

A serem implementados via Makefile na Camada 1. Padrão da fábrica:

| Comando | Função |
|---|---|
| `make setup` | Sobe Postgres + Redis no Docker, instala deps, roda migrations |
| `make dev` | Sobe backend + frontend em modo desenvolvimento |
| `make test` | Roda testes unitários (rápidos, sem container) |
| `make test-integration` | Roda integration + e2e (com Testcontainers) |
| `make lint` | Checkstyle + SpotBugs + ESLint |
| `make check` | `lint` + `test` (espelho do que o CI roda em PR) |
| `make ship` | `check` + push + abre PR |

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

- **2026-05-06** — Criação inicial. Stack, arquitetura, convenções e modelo financeiro consolidados a partir dos ADRs 001-008.
