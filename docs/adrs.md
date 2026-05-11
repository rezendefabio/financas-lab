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

---

## ADR-009 — Layout `.claude/` e mecanismo de hooks no Windows

**Status:** Aceito
**Data:** 2026-05-10

### Contexto

Abertura da Camada 3 (Configuração do Claude Code). O `hooks-pendentes.md` lista ~20 itens com escopos heterogeneos: alguns universais (Conventional Commits, encoding UTF-8), alguns especificos de stack (Maven `<release>`, sufixo `Test`), alguns especificos de SO (PowerShell `Write-Error` + `exit`), alguns especificos de framework (`shadcn init` deixando `button.tsx`). Tratar todos como iguais cria tres problemas: dificulta separar o que serve a outros projetos (laboratorio e tambem investimento em fabrica replicavel), inflaciona o lookup do agente em cada validacao, e mistura preocupacoes conceitualmente distintas. Ao mesmo tempo, criar repositorio separado para a "fabrica" antes da primeira replicacao real e abstracao prematura (N=1 nao ensina abstracao).

### Decisao

**Casa unica no `financas-lab/`, com separacao interna por escopo de aplicabilidade.**

Estrutura:

```
.claude/
├── hooks/
│   ├── universal/        (qualquer projeto)
│   ├── java-spring/      (preset stack Java/Maven/Spring)
│   ├── windows/          (Windows + PowerShell)
│   ├── next/             (Next.js)
│   └── local/            (so financas-lab)
├── agents/
│   ├── universal/
│   ├── java-spring/
│   └── local/
└── skills/
    ├── universal/
    └── local/
```

**Mecanismo de git hooks no Windows:**

- `git config core.hooksPath .githooks` (configurado automaticamente por `scripts/setup.ps1`).
- Entrypoints em `.githooks/` sao arquivos sem extensao (`pre-commit`, `commit-msg`, `pre-push`) — wrappers bash minimos que invocam `pwsh -File .githooks/<nome>.ps1`. Git no Windows (Git Bash) interpreta o shebang `#!/usr/bin/env bash`.
- Logica real fica em `.claude/hooks/<escopo>/*.ps1`, invocada pelos `.ps1` companherios em `.githooks/`.

**Regra de promocao entre escopos:**

- Item nasce na pasta mais especifica que comporta seu uso real (default: `local/`).
- Promove para escopo mais amplo (`java-spring/` → `universal/`) apenas apos **evidencia explicita** de aplicabilidade no escopo maior, registrada em commit ou ADR. Evidencia minima: segundo projeto/contexto provando reuso, ou justificativa tecnica documentada.
- Promocao e decisao consciente, nao automatica. Nao ha ferramenta que "detecta" universalidade.

### Alternativas consideradas

- **Tudo plano em `.claude/hooks/` sem separacao por escopo** — rejeitada porque nao captura a heterogeneidade ja presente em `hooks-pendentes.md` e dificulta reuso futuro. Adicionar disciplina retroativamente custa mais que nascer com ela.
- **Repositorio separado `fabrica-ai-native/` consumido por copia via `new-fabrica.ps1`** — considerada e rejeitada nesta fase. N=1 (este projeto unico) nao fornece evidencia suficiente para validar a fronteira universal vs. stack vs. local. Decisao reversivel: quando 2a fabrica nascer ou itens em `universal/`/`java-spring/` estabilizarem, extrair para repo separado vira refactor barato.
- **Husky (Node.js) ou pre-commit framework (Python) em vez de PowerShell + `core.hooksPath`** — rejeitada para preservar coerencia com a Camada 1 (scripts ja sao PowerShell, decisao da Camada 0 foi Windows nativo). Adicionar dependencia nova ao ciclo de hooks e friccao desnecessaria no atual estagio.
- **Claude Code hooks nativos (`PreToolUse`, `Stop`) como mecanismo unico** — rejeitada como solucao completa porque cobre apenas comportamento do agente, nao validacao de codigo pre-commit. Os dois mecanismos sao complementares; Claude Code hooks entram em sub-etapa propria apos 4.2.

### Consequencias

**Aceitas:**

- Cinco pastas vazias com `.gitkeep` no nascimento — ruido visual inicial. Justificado pela disciplina que estabelece.
- Decisao sobre escopo de cada hook fica em humanos no momento da criacao — nao ha tooling que valida automaticamente. Em troca, forca reflexao explicita.
- Entrypoints em `.githooks/` exigirem wrapper bash + companheiro `.ps1` e dois arquivos por hook do git — verboso, mas idiomatico no Windows + Git Bash.

**Ganhos:**

- Quando 2a fabrica nascer, copia `hooks/universal/` + `agents/universal/` + `skills/universal/` e ignora o resto. Custo de replicacao proporcional ao que de fato e replicavel.
- Lookup do agente reduz: ao trabalhar em codigo Java, hooks em `next/` ou `windows/` sao irrelevantes e nem precisam ser carregados se a invocacao do agente filtrar por escopo.
- Mistura de preocupacoes fica visualmente clara — separacao fisica e o gate.
- Reversibilidade preservada: estrutura interna pode virar repo separado depois sem refactor doloroso, porque a fronteira ja esta desenhada.

---

## ADR-010 — Debito de portabilidade: PowerShell e Windows-specific aceitos conscientemente

**Status:** Aceito
**Data:** 2026-05-10

### Contexto

Camada 0 decidiu Windows nativo + PowerShell + Docker Desktop como ambiente. A Camada 1 produziu 6 scripts `.ps1` em `scripts/` e dedicou 2 sub-etapas inteiras (2.6.1, 2.6.2) a bugs especificos do PowerShell. A Camada 3 vai produzir hooks e wrappers que tambem serao PowerShell-specific. Ja existe risco previsto (Camada 5) de migracao para VPS Linux para rodar routines de agente, momento em que parte dessa infraestrutura tera de ser reescrita em bash. A pergunta e: pagar custo de cross-platform agora (postura defensiva) ou aceitar custo futuro maior em troca de velocidade presente?

### Decisao

**Aceitar conscientemente o debito de portabilidade.** Manter scripts e hooks PowerShell-specific. Nao introduzir abstracao cross-platform (Node, Python como wrapper, Docker para tudo) preventivamente.

### Criterio explicito de revisao

Esta decisao e revisitada quando **qualquer um** dos eventos abaixo ocorrer:

1. **Camada 5 entra em escopo** — abertura formal da decisao de subir VPS Linux para routines persistentes ou batch paralelo pesado.
2. **2a fabrica nasce em outro SO** — quando segundo projeto adotar este modelo de fabrica em ambiente nao-Windows.
3. **Dor concreta acumulada** — soma de tempo perdido em workarounds de PowerShell (medida em horas registradas em `progresso.md`) cruzar limiar subjetivo de "vale reescrever". Sem numero fixo aqui; e gatilho de bom senso revisitado a cada retrospectiva de camada.

Quando qualquer um disparar, abrir ADR novo (superseder este) com nova decisao.

### Custo estimado da migracao futura

- Reescrita de 6 scripts em `scripts/*.ps1` para `scripts/*.sh` — 4-8h.
- Reescrita de hooks em `.claude/hooks/windows/` e wrappers em `.githooks/` (quantidade depende do que a Camada 3 produzir) — 4-12h.
- Revalidacao destrutiva manual de todos os fluxos no SO destino — 4-8h.
- **Total estimado: 1-3 dias de trabalho** num momento futuro escolhido conscientemente.

Estimativa intencionalmente otimista — assume que a logica e estavel e so o veiculo muda. Se descobrir que padroes PowerShell-especificos vazaram para o desenho (nao so sintaxe), o custo dobra.

### Mitigacao enquanto debito vigora

- Cada hook em `.claude/hooks/windows/` traz comentario no topo identificando explicitamente que e Windows-only. Reduz surpresa futura.
- Logica de hook fica enxuta nos `.ps1`; complexidade real fica em codigo que poderia ser portado (validacao via `grep` padrao, leitura de bytes, etc.). PowerShell vira involucro fino, nao corpo da logica.
- Estrutura `.claude/hooks/<escopo>/` ja separa Windows-specific do resto. Migrar significa reescrever uma pasta, nao auditar todo o projeto.

### Alternativas consideradas

- **Cross-platform desde ja (Node/Python como veiculo de hooks)** — rejeitada por custo presente certo em troca de ganho futuro hipotetico. Postura inconsistente com decisao da Camada 0 (Windows nativo) e com principio "stack on-distribution > stack que voce acha bonita".
- **Tudo em Docker, hooks rodam em container** — rejeitada por inverter o eixo do problema (carrega complexidade massiva no presente para resolver portabilidade que pode nunca ser exercida).
- **Nada decidido formalmente, lidar quando bater** — rejeitada porque debito nao registrado e debito que assombra como ansiedade difusa. Formalizar em ADR transforma "preocupacao" em "tarefa futura com gatilho claro".

### Consequencias

**Aceitas:**

- Quando a Camada 5 ou 2a fabrica chegarem, 1-3 dias de trabalho de migracao — custo conhecido em momento previsto.
- Contribuidores externos (improvavel neste estagio, mas possivel) precisariam de Windows + PowerShell para rodar localmente. Cobertura limitada.

**Ganhos:**

- Zero custo presente. Velocidade da Camada 3 nao e comprometida por abstracao defensiva.
- Coerencia com decisoes anteriores do projeto (Camada 0 e 1).
- Debito conhecido > ansiedade difusa. Decisao arquivada com criterio de revisao; sai do plano mental ativo.

---

## ADR-011 — Padroes de validacao destrutiva

**Status:** Aceito
**Data:** 2026-05-10

### Contexto

Validacao destrutiva (executada na branch da etapa ou em smoke test pos-merge) e instrumento de qualidade de primeira linha do projeto. Camada 1 estabeleceu o principio: "validacao destrutiva manual e nao-negociavel; encontrou 3 bugs que toda automacao validou como verde" (retrospectiva da Camada 1).

Sub-etapa 4.2 expos uma armadilha do metodo: validacao que parece passar pode nao ter exercitado o hook. Em smoke test pos-merge da 4.2, operador executou tres cenarios destrutivos. Todos reportaram "comando rodou sem erro visivel" — mas `git status` mostrava working tree limpo em todos. Hook nao foi invocado em nenhum cenario. Causa raiz: `[System.IO.File]::WriteAllText` com path relativo em PowerShell grava em `[System.Environment]::CurrentDirectory`, nao em `$PWD`. Quando a sessao faz `cd` para entrar no repo, esses dois caminhos divergem. Arquivo e criado em `C:\Users\<user>\`, invisivel ao git rodando em `C:\projetos\...`.

Investigacao posterior confirmou que o agente do Claude Code **nao caiu** nesse gotcha durante a validacao destrutiva da branch da 4.2 (provavel: agente foi spawnado ja dentro do diretorio do repo, sincronizando ambos automaticamente). Mas o operador caiu, evidenciando que o risco e real em sessoes onde `$PWD` e `Environment.CurrentDirectory` divergem.

O ponto critico nao e o gotcha especifico do PowerShell. E o **principio mais geral**: "comando rodou sem erro" e premissa fraca para concluir "cenario foi exercitado". Sem verificacao explicita de pre-condicao, validacao destrutiva produz falsos positivos sem alerta.

### Decisao

Toda validacao destrutiva (na branch da etapa ou em smoke test pos-merge) **deve incluir verificacao explicita de pre-condicao** antes de cada cenario.

**Padroes obrigatorios:**

1. **Apos criar arquivo de teste:** `Test-Path .\arquivo` (ou equivalente). Esperado: `True`. Se `False`, parar e investigar — arquivo nao foi criado onde esperado.
2. **Antes de `git commit`:** rodar `git status` e confirmar que ha arquivo staged. Se sair `nothing to commit, working tree clean`, parar — cenario nao tem entrada.
3. **Apos comando que deveria falhar:** verificar `$LASTEXITCODE` (PowerShell) ou similar. Esperar codigo `!= 0`. Se vier `0`, cenario nao reproduziu o erro esperado.
4. **Para `[System.IO.File]::WriteAllText` com path relativo em PowerShell:** sincronizar previamente `[System.Environment]::CurrentDirectory = (Get-Location).Path`, OU usar caminho absoluto (`"$PWD\arquivo"`). Sem sincronizacao, arquivo pode ser gravado em diretorio diferente do `git`.

**Reportar resultado de cada pre-condicao no PR body** da sub-etapa. Nao basta listar "cenarios validados" — listar tambem as pre-condicoes verificadas e seus valores observados.

### Alternativas consideradas

- **Confiar em "o comando nao deu erro visivel"** — rejeitada. Foi exatamente a hipotese que falhou no smoke test pos-merge da 4.2. Falsos positivos sao silenciosos por natureza; nao ha mecanismo de detecao se nao houver verificacao explicita.
- **Forcar uso de caminhos absolutos em todos os scripts de validacao** — considerada e parcialmente adotada. Mais robusto, mas verboso. Sincronizacao previa de `Environment.CurrentDirectory` resolve o caso PowerShell sem afetar legibilidade.
- **Criar tooling automatico** (linter de scripts de validacao destrutiva) — rejeitada como prematura. Padrao primeiro, automacao depois quando justificar.
- **Limitar validacao destrutiva a agente apenas** (que nao caiu no gotcha) — rejeitada. Operador precisa validar pos-merge em ambiente real; nao pode delegar essa responsabilidade.

### Consequencias

**Aceitas:**

- Prompts de validacao destrutiva ficam mais verbose (cenarios com `Test-Path`, `git status` explicitos).
- Operador e agente seguem o mesmo padrao — sem atalhos por familiaridade do operador com o ambiente.

**Ganhos:**

- Zero falsos positivos silenciosos em validacao destrutiva.
- Padrao replicavel para sub-etapas seguintes da Camada 3 (4.3, 4.4) e qualquer hook futuro.
- Aprendizado registrado em ADR formal — nao se perde em prosa de retrospectiva.
- Reforco do principio consolidado da Camada 1: "validacao destrutiva e instrumento de qualidade de primeira linha". O gotcha mostrou que o principio precisa de gates explicitos para ser eficaz.

---

## ADR-012 — Subagents do projeto invocados via skill orquestradora

**Status:** Aceito
**Data:** 2026-05-11

### Contexto

Camada 3 do blueprint do projeto prescreve 3-5 subagents focados, invocados proativamente pelo Claude principal via campo `description` no frontmatter. Sub-etapas 4.9 e 4.9.1 entregaram o primeiro subagent (`pr-reviewer`, Haiku, tools read-only). Smoke test pos-merge da 4.9.1 revelou que **invocacao proativa via `description` nao e deterministica**.

Pedido "revisa este PR" foi executado pelo Claude principal direto (label visual "Skill(review)"), sem invocar o subagent via Task tool. Comportamento persistiu mesmo apos desabilitar o plugin global `code-review`. O subagent existe, esta bem-formado (template prescritivo + 2 exemplos few-shot apos a 4.9.1), e funcionou quando invocado explicitamente — mas o Claude principal nao o chamou.

Investigacao identificou quatro fatores que contribuem para o nao-determinismo:

1. **Memoria global do Claude Code em `~/.claude/projects/<hash>/memory/`** — 21 arquivos `.md` de feedback/sub-etapas, auto-memory ON, escreve sem confirmacao. Influencia heuristica de delegacao de formas opacas.
2. **Plugins globais nao-versionados** (`code-review`, `frontend-design`) alteram comportamento do Claude principal mesmo desabilitados localmente.
3. **Built-in agents do Claude Code** (`Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup`) competem com subagents do projeto no espaco de delegacao.
4. **Heuristica de delegacao** prefere execucao direta em tarefas que o Claude principal julga simples. Description "Use proactively" e instrucao fraca diante de pressao por simplicidade.

O blueprint do projeto (linha 76) ja avisava: *"O ponto critico e o campo `description`: o Claude principal decide quando delegar baseado nele. Description vaga = subagent nunca chamado."* A descoberta empirica e mais forte: **description bem-formada tambem pode nao disparar delegacao** quando o Claude principal opta por execucao direta. A premissa "subagents invocados proativamente via description" e insuficiente.

Esta nao e falha do `pr-reviewer` em particular. E limite arquitetural que afeta toda a Camada 3, e portanto a estrategia inteira de uso de subagents no projeto.

### Decisao

**Subagents do projeto sao invocados via skill orquestradora dedicada.** Cada subagent tem uma skill (`.claude/skills/<escopo>/<nome>.md`) que, quando invocada pelo operador via slash command, instrui o Claude principal a delegar ao subagent via Task tool.

**Mecanismo:**

1. Operador invoca a skill explicitamente (ex: `/review-pr <numero>`).
2. A skill contem prompt direto: "Use a ferramenta Task para invocar o subagent `<nome>`. Repasse o input completo conforme o template."
3. Claude principal executa a Task tool, que dispara o subagent.
4. Subagent roda em contexto isolado (gatekeeping de contexto), com modelo barato e tools restritas conforme frontmatter.
5. Output do subagent retorna para o Claude principal, que apresenta ao operador.

**Nao mecanismo:**

- Invocacao por heuristica de delegacao proativa via campo `description` e considerada **nao-determinismo arquitetural** e nao e mecanismo primario.
- O campo `description` continua existindo nos subagents e e usado como documentacao + fallback, mas nao e a porta de entrada esperada.

**Padroes obrigatorios:**

1. **Todo subagent do projeto tem skill orquestradora correspondente.** Subagent sem skill e nao-acessivel deterministicamente — entra como debito ou e considerado nao-pronto.
2. **A skill prescreve invocacao via Task tool em texto direto.** Tom imperativo ("Use a Task tool...", "Invoque o subagent..."), nao sugestivo.
3. **A skill carrega contexto/input do operador.** Slash command pode receber argumentos (ex: numero do PR) que a skill repassa ao subagent via prompt.
4. **Smoke test do par skill+subagent** valida o caminho ponta-a-ponta: invocacao da skill -> Task tool disparada -> subagent executado -> output retornado.

### Alternativas consideradas

- **Caminho A — Description imperativa.** Editar `description` do subagent para tom imperativo ("ALWAYS delegate via Task tool"). Rejeitada: continua dependendo de heuristica do Claude principal "ler" a description e respeitar. Caixa-preta. Mesmo se "passar" em smoke, atribuicao causal e fraca — nao se sabe se foi o "ALWAYS" ou outro fator. Nao escala para `architect-reviewer`, `test-writer` futuros.
- **Caminho C — Re-pensar Camada 3 sem subagents.** Aceitar que invocacao proativa nao funciona e abandonar subagents em favor de CLAUDE.md + hooks + skills (linha 87 do blueprint cita esse padrao como "80% do ganho"). Rejeitada: descarta valor real do `pr-reviewer` (tools restritas read-only, gatekeeping de contexto, modelo barato) baseado em N=1. Decisao grande demais para a evidencia atual.
- **Status quo** (manter description proativa e esperar mais amostras). Rejeitada: descobertas meta-operacionais (memoria global, plugins, built-ins) tornam smoke tests futuros nao-confiaveis sem mitigacao previa. Adiar e acumular custo opaco.

### Consequencias

**Aceitas:**

- Operador invoca subagent explicitamente via slash command, nao "Claude principal" mediando.
- Cada subagent custa 2 componentes (subagent + skill), nao 1.
- Camada 3 ganha 1 criterio de "pronto" novo: padrao skill orquestradora validado com smoke.
- `pr-reviewer` (4.9 + 4.9.1) **mantem-se valido** — o componente esta correto, so faltava o mecanismo de invocacao deterministico.

**Ganhos:**

- **Determinismo da invocacao.** Slash command e ato explicito do operador.
- **Determinismo da delegacao.** Skill prescreve Task tool em texto direto.
- **Preservacao de subagent como ferramenta.** Tools restritas, modelo barato, contexto isolado continuam valendo.
- **Padrao escalavel.** `architect-reviewer`, `test-writer`, `migration-writer` futuros nascem com skill correspondente.
- **Ensinavel.** Regra simples: "subagent sempre vem com skill". Sem zona limitrofe.

**Custos reconhecidos:**

- Plugin `code-review` (decidir manter, desativar ou reaproveitar — criterio de pronto da Camada 3) continua aberto. Independente do ADR-012.
- Investigacao dos built-in agents (o que `Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup` realmente fazem) fica como debito em `hooks-pendentes.md`.
- Risco residual: skill pode tambem nao invocar Task tool deterministicamente. Smoke da 4.11 (primeira skill orquestradora) valida.
