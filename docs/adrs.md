# ADRs — Decisões Arquiteturais Fundadoras

> Architecture Decision Records do projeto-laboratório.
> Cada ADR registra uma decisão importante, **o contexto** em que foi tomada, **as alternativas consideradas** e **as consequências aceitas**.
> ADR não muda — se uma decisão é revista, cria-se um novo ADR que supersede o anterior.

**Formato:** numeração sequencial, status (Aceito | Superseded por XXX | Deprecated), data.

---

## ADR-001 — Stack backend: Java 21 + Spring Boot 3 + Maven

**Status:** Aceito
**Data:** 2026-05-06

### Contexto

O projeto precisa de um stack backend para o MVP. O desenvolvedor tem base Java fullstack consolidada e está em transição para perfil Python/IA. A fábrica AI-native requer máxima fluência do agente no stack escolhido (princípio "boring tech, on-distribution" da Camada 2).

### Decisão

Backend em **Java 21 (LTS) + Spring Boot 3.x + Maven**.

### Alternativas consideradas

- **Python + FastAPI** — recomendação inicial baseada em alinhamento com transição declarada. Rejeitada porque o desenvolvedor optou por validar a fábrica em terreno onde o domínio técnico não compete com a curva da fábrica. Validar dois aprendizados simultâneos (fábrica + linguagem nova) introduz ruído na avaliação.
- **Gradle Kotlin DSL** em vez de Maven — rejeitada por menor previsibilidade do agente em casos não-triviais (plugins, builds customizados). Maven XML é verboso mas absolutamente determinístico.
- **Java 17** — rejeitada por estar 2 LTS atrás de Java 21 no momento da decisão. Java 21 é o padrão atual para projetos novos.

### Consequências

**Aceitas:**
- Verbosidade maior — features terão 30-50% mais código que equivalente em Python. Custo de revisão humana aumenta proporcionalmente.
- Build/CI mais lentos — JVM startup e compilação Maven exigem estratégia de testes incrementais; rodar suite completa pre-commit é inviável.
- Spring tem configuração implícita via anotações — exige disciplina explícita (profiles, qualifiers, @Transactional escopo) documentada em `decisoes.md`.

**Ganhos:**
- Tipos fortes em compile-time pegam categoria inteira de erros que Python pegaria só em runtime.
- Fluência do desenvolvedor reduz fricção em decisões de modelagem.
- Spring Boot é altamente on-distribution para Claude — fluência do agente é equivalente a Python/FastAPI.

---

## ADR-002 — Frontend: Next.js 15 + TypeScript + PWA

**Status:** Aceito
**Data:** 2026-05-06

### Contexto

MVP precisa de presença web e mobile. Três caminhos viáveis foram considerados: React Native + Expo + Next.js separados, Flutter mobile + web qualquer, ou PWA pura.

### Decisão

**Next.js 15 + TypeScript + Tailwind + PWA via next-pwa**, com offline básico (cache de leitura, sync ao reconectar).

### Alternativas consideradas

- **React Native + Expo + Next.js compartilhando lógica** — rejeitada para MVP por triplicar complexidade de build, deploy e teste. Recomendada para fase 2 se o laboratório validar.
- **Flutter mobile** — rejeitada por menor fluência do agente comparado a React/Next.js, contrariando princípio de stack on-distribution.
- **Apenas web responsivo sem PWA** — rejeitada por perder oportunidade de exercitar offline-first, que é decisão arquitetural relevante para a fábrica.

### Consequências

**Aceitas:**
- Mobile não-nativo tem limitações reais (notificações push fracas em iOS, sem acesso a hardware avançado).
- PWA em iOS tem suporte limitado (instalação via Safari, restrições de storage).

**Ganhos:**
- Codebase única, decisões de UI únicas, deploy único.
- Velocidade de iteração maximizada para fase de laboratório.
- Migração futura para React Native compartilha React + TypeScript + Zod.

---

## ADR-003 — Persistência: PostgreSQL em dev e prod

**Status:** Aceito
**Data:** 2026-05-06

### Contexto

Projeto precisa de banco relacional. Padrão comum em projetos Java é usar H2 em dev (start rápido, sem dependência externa) e Postgres em prod.

### Decisão

**PostgreSQL 16 em dev e prod.** Sem H2, sem SQLite, sem variação de banco entre ambientes.

Versão é fixada em `decisoes.md` para Postgres 16 LTS no momento da decisão.

### Alternativas consideradas

- **H2 em dev, Postgres em prod** — rejeitada pelo histórico de bugs de paridade: queries com tipos PG-específicos (jsonb, arrays, intervalos), comportamento de `LIKE` case-sensitive, funções de data divergentes. Custo de descobrir inconsistência em prod supera ganho de start rápido em dev.
- **SQLite em dev** — mesma razão. Adicionalmente, agente erra mais quando testa contra dialeto diferente do que vai rodar.

### Consequências

**Aceitas:**
- Desenvolvedor precisa ter Docker rodando localmente. Não é fricção real em 2026.
- Setup inicial inclui docker-compose para Postgres — entra no `make setup`.

**Ganhos:**
- Zero bugs de paridade dev/prod.
- Testes de integração com Testcontainers usam mesma versão do Postgres que prod.
- Agente trabalha com um dialeto SQL apenas.

---

## ADR-004 — Arquitetura: Clean Architecture enxuta com porta para DDD tático on-demand

**Status:** Aceito
**Data:** 2026-05-06

### Contexto

Projeto-laboratório precisa de arquitetura que: (a) seja exercício honesto de boa arquitetura, (b) permita evolução para padrões mais sofisticados sem refactor doloroso, (c) seja fluente para o agente executar com pouca revisão. DDD tático completo (agregados, domain events, anti-corruption layers, bounded contexts elaborados) foi considerado mas rejeitado para o estado inicial — finanças pessoais no MVP é majoritariamente CRUD com algumas invariantes, não justifica o ceremony desde o dia 1.

### Decisão

**Clean Architecture em 4 camadas explícitas por bounded context (módulo):**

```
src/main/java/com/laboratorio/financas/
├── {contexto}/
│   ├── domain/         # POJOs puros, ZERO anotação Spring/JPA
│   ├── application/    # use cases (1 classe = 1 caso de uso)
│   ├── infrastructure/ # @Entity JPA, repositórios concretos, adapters
│   └── interfaces/     # @RestController, DTOs de API
└── shared/
    └── domain/         # value objects compartilhados (Money, etc)
```

**Regra dura, não-negociável:** entidade JPA (`TransacaoEntity`) **nunca** é entidade de domínio (`Transacao`). MapStruct converte na borda da infra.

**Regra dura:** dependências apontam sempre para dentro — `interfaces` → `application` → `domain`; `infrastructure` → `domain`. Domínio não conhece nada além de si mesmo e do `shared/domain`.

**Porta aberta para DDD tático:** padrões como agregado, domain event, value object adicional, bounded context separado entram **on demand** quando uma regra de negócio justificar, não preventivamente.

### Alternativas consideradas

- **DDD tático completo desde o início** — rejeitada pelo custo de modelagem em domínio simples e maior taxa de erro do agente em modelagem (não em mecânica). Pode ser introduzido por contexto quando justificar (provavelmente em "Investimentos" ou "Metas" se evoluir).
- **Service + Repository + DTO procedural** (estilo Spring tradicional) — rejeitada por dificultar evolução e por concentrar lógica de negócio em services anêmicos sobre entidades JPA, padrão que não suporta crescimento de complexidade.
- **Hexagonal/Ports & Adapters explícito** — efetivamente o que esta decisão implementa, mas com nomenclatura de Clean Architecture que é mais on-distribution e menos sujeita a interpretações divergentes do agente.

### Consequências

**Aceitas:**
- Mais código que abordagem procedural — mappers, interfaces, separação de DTOs em três níveis (domínio, persistência, API).
- Curva inicial de calibração para o agente respeitar as fronteiras — vai entrar em hooks de validação na Camada 3.

**Ganhos:**
- Domínio testável sem mock de banco (testes unitários puros).
- Troca de framework de persistência ou API custa apenas a infraestrutura, não o domínio.
- Quando uma regra justificar agregado completo, introdução é local — não exige reescrita.

### Padrões aplicados desde o dia 1

- **Use case como classe** (não função em controller). 1 classe = 1 caso de uso. Nome no padrão `<Verbo><Substantivo>UseCase` (ex: `CriarTransacaoUseCase`).
- **Repository pattern** com interface no domínio, implementação na infraestrutura.
- **Value Object `Money`** — `BigDecimal` + currency code, operações que retornam novo `Money`, comparação por valor não por referência.
- **Entidades de domínio puras** — sem `@Entity`, sem `@Component`, sem dependência de Spring/JPA.
- **DTOs separados em três níveis** — `*Request`/`*Response` na borda da API, `*Command`/`*Query` na entrada do use case (quando complexo), `*Entity` na persistência.

### Padrões adiados (porta aberta)

- **Agregado raiz com invariantes** — entrará em `Conta` quando saldo passar a ser derivado e exigir consistência forte (estimado: semana 3-4 do MVP).
- **Domain events** — entrarão quando houver mais de um reator para um evento (ex: "transação criada" disparando atualização de saldo + relatório + notificação).
- **Bounded contexts separados em módulos Maven** — hoje é separação por package. Vira módulo Maven se um contexto crescer o suficiente para justificar isolamento de build.
- **CQRS** — não previsto no MVP. Possível em "Relatórios" se queries de leitura crescerem.

---

## ADR-005 — Autenticação: JWT stateless com refresh token rotativo

**Status:** Aceito
**Data:** 2026-05-06

### Contexto

MVP é single-user, mas auth precisa existir para: (a) proteger API, (b) preparar fundação para multi-user futuro sem refactor doloroso, (c) exercitar Spring Security na fábrica.

### Decisão

**JWT stateless com refresh token rotativo armazenado em cookie httpOnly.**

- Access token: 15 minutos, transmitido em header `Authorization: Bearer`
- Refresh token: 7 dias, em cookie httpOnly+secure+sameSite=strict
- Refresh rotativo: cada refresh emite novo refresh token e invalida o anterior (revogação por blacklist em Redis ou tabela)
- Senhas hashadas com BCrypt (cost factor 12)
- Sem OAuth no MVP, sem 2FA no MVP

### Alternativas consideradas

- **Sessão server-side com cookie** — rejeitada por dificultar separação clara de back/front e por não ser idiomática em SPA + PWA.
- **JWT sem refresh** — rejeitada por forçar trade-off ruim entre segurança (token curto = re-login frequente) e UX (token longo = janela de exposição grande).
- **OAuth com Google/GitHub no MVP** — rejeitada por adicionar dependência externa e fluxo de cadastro mais complexo. Entra em fase 2 se justificar.

### Consequências

**Aceitas:**
- Necessidade de Redis (ou tabela em Postgres) para blacklist de refresh tokens — entra na infra desde o início.
- Spring Security 6 com configuração programática (não XML) — exige cuidado redobrado em decisões de filter chain.

**Ganhos:**
- Foundation pronta para multi-user, multi-device, OAuth posterior sem reescrita.
- Stateless permite escala horizontal trivial (não relevante no MVP, mas zero custo).

---

## ADR-006 — Migrations: Flyway com versionamento sequencial

**Status:** Aceito
**Data:** 2026-05-06

### Contexto

Projeto precisa de gestão de schema versionada. Spring Boot suporta Flyway e Liquibase nativamente.

### Decisão

**Flyway com migrations versionadas em SQL puro**, padrão `V{N}__{descricao}.sql`.

Localização: `src/main/resources/db/migration/`.

Política: migrations são **append-only** após merge em main. Schema fixes via nova migration, nunca por edição de migration anterior.

### Alternativas consideradas

- **Liquibase** — rejeitada por XML/YAML mais verboso e menos legível que SQL puro. Boring vence vistoso.
- **JPA `ddl-auto=update`** — rejeitada absolutamente. Em nenhum momento, em nenhum ambiente, JPA gera schema. Schema é declarado em SQL versionado.

### Consequências

**Aceitas:**
- Disciplina exigida do desenvolvedor e do agente — alterar `@Entity` exige escrever migration correspondente.
- Vai entrar em hooks de validação na Camada 3: detectar `@Entity` modificada sem migration nova é warning crítico.

**Ganhos:**
- Schema reprodutível em qualquer ambiente.
- Histórico de evolução de schema versionado junto com código.
- Rollback possível com migration reversa explícita.

---

## ADR-007 — Testes em três níveis com Testcontainers

**Status:** Aceito
**Data:** 2026-05-06

### Contexto

Princípio fundador da fábrica: "autonomia é proporcional à infraestrutura de validação". CI confiável é gate único da verdade. Stack precisa de estratégia de testes que cubra os três níveis exigidos pelo blueprint (unit, integration, e2e) e seja viável em Java/Spring sem virar ceremony.

### Decisão

**Três níveis de teste com fronteiras explícitas:**

1. **Unit** — testa domain puro, sem Spring, sem banco, sem mock pesado. JUnit 5 + AssertJ. Foco: regras de negócio em entidades, use cases com repositórios mockados manualmente (não Mockito quando dispensável).
2. **Integration** — testa use case + repositório real contra **Postgres em Testcontainers**. JUnit 5 + Spring Boot Test + Testcontainers. Foco: contratos entre application e infrastructure.
3. **E2E** — testa fluxo completo via API. SpringBootTest + MockMvc ou TestRestTemplate. Foco: 1-2 fluxos principais por bounded context, não exaustivo.

**Cobertura via JaCoCo:**
- Domain: meta 90%+ (é puro, deve ser fácil)
- Application: meta 80%+
- Infrastructure: meta 60%+ (mappers e configs têm valor menor de teste)
- Interfaces: meta 70%+ (validação de contratos da API)

**Mockito uso comedido** — só quando dependência é externa real (HTTP client, mensageria). Para repositório, preferir Testcontainers ou test double manual.

### Alternativas consideradas

- **In-memory H2 para integration tests** — rejeitada pelo mesmo motivo de ADR-003. Bug de paridade não é aceitável.
- **Mockito como padrão** — rejeitada por gerar testes frágeis acoplados a implementação. Mockito é ferramenta cirúrgica, não padrão.
- **Cobertura única > 80%** — rejeitada por incentivar testes vazios. Cobertura por camada respeita o valor real de cada camada.

### Consequências

**Aceitas:**
- Testcontainers inicia container Docker em testes de integração — startup mais lento que H2. Mitigação: container é reusado entre testes na mesma JVM.
- CI precisa de Docker disponível — todas as plataformas-alvo (GitHub Actions, Railway) suportam.

**Ganhos:**
- Testes refletem comportamento real de produção.
- Refactor de implementação não quebra testes (testes não conhecem detalhes internos).
- Cobertura por camada incentiva testar o que importa.

---

## ADR-008 — Modelo financeiro do projeto: Max 5x até evidência de insuficiência

**Status:** Aceito
**Data:** 2026-05-06

### Contexto

Camada 6 do blueprint estabelece estratégia híbrida Max + API. Projeto-laboratório precisa de baseline financeira concreta.

### Decisão

**Mês 1-2:** Apenas Max 5x. Sem conta API ainda.
**Mês 3-4:** Max 5x + conta API configurada com hard limit $30/mês. Routines Tier 1 que justificarem migram para API.
**Mês 5+:** Reavaliar com dados de uso reais.

Migração para Max 20x **só** se houver evidência objetiva de que Max 5x trava o desenvolvimento interativo (≥2x por semana), não para resolver problema de automação.

### Alternativas consideradas

Documentadas no blueprint Camada 6. Decisão aqui é apenas formalizar o ponto de partida.

### Consequências

Configuração da conta API e budget alerts deve ser feita ainda no mês 1, mesmo que não usada — evita estar travado em momento crítico configurando billing.
