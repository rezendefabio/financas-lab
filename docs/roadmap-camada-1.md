# Roadmap — Camada 1 (Infraestrutura de Confiança)

> Plano detalhado das 2 semanas que constroem a fundação da fábrica.
> Cada etapa tem **objetivo**, **critério de pronto** e **observações** sobre armadilhas conhecidas.

**Princípio fundador a lembrar:** *"A autonomia que você consegue dar é limitada pela qualidade da infraestrutura de validação."* Esta camada é a infraestrutura de validação. Toda a fábrica daqui pra frente apoia nela.

---

## Princípios da Camada 1

1. **Zero código de feature.** Tudo o que for escrito aqui é infraestrutura — esqueletos, configurações, testes de fumaça. Resistir à tentação de "já fazer um endpoint útil enquanto está mexendo".
2. **CI verde do primeiro dia.** Cada commit em `main` deve passar no CI. Não tem "ainda vou arrumar o CI semana que vem".
3. **Hooks locais espelham CI.** O que CI valida, hook local valida primeiro. Evita o ciclo "push → vermelho → fix → push" que queima tokens e tempo.
4. **Tudo versionado.** Schema, configuração, scripts, hooks. Nada "que só funciona na minha máquina".
5. **Documentação executável.** Cada coisa configurada tem comando para rodar (`.\scripts\setup.ps1`, etc.), não prosa explicando como fazer.

---

## Visão geral das 2 semanas

| Semana | Foco | Entregável principal |
|---|---|---|
| **1** | Esqueleto + banco + Spring Boot inicial + CI básico | Hello-world endpoint passando teste e2e via CI |
| **2** | Hardening: testes três níveis, JaCoCo, hooks, frontend, docs | Fábrica fundacional pronta para Camada 2 |

---

## Semana 1 — Esqueleto e CI

### Etapa 1.1 — Estrutura inicial do repositório

**Objetivo:** Repo deixar de estar vazio e ter os artefatos de infraestrutura mínima.

**Tarefas:**

1. Criar `.gitattributes` rigoroso para line endings (CRLF para `.bat`/`.ps1`, LF para resto).
2. Criar `.gitignore` Java + Node (Maven `target/`, IDE files, `.env`, `node_modules/`, `.next/`).
3. Criar `README.md` curto (5-10 linhas) apontando para `docs/`.
4. Criar `CLAUDE.md` mínimo na raiz (com a regra "não escrever código de feature ainda").
5. Mover `visao.md`, `adrs.md`, `decisoes.md`, `progresso.md`, este `roadmap-camada-1.md` para `docs/`.
6. Primeiro commit: `chore: initial repo structure`.

**Critério de pronto:**
- [ ] `git status` limpo
- [ ] Estrutura visível: `docs/`, `CLAUDE.md`, `README.md`, `.gitignore`, `.gitattributes`
- [ ] Push para `main` no GitHub feito

**Armadilhas:**
- `.gitattributes` precisa ser committed **antes** de outros arquivos para line endings serem normalizados corretamente. Se você commitar arquivos antes do `.gitattributes`, eles ficam com line endings "errados" no repo. Faz na ordem.
- Não criar pasta `src/` ainda. Spring inicializa essa estrutura na etapa 1.4.

---

### Etapa 1.2 — Branch protection no GitHub

**Objetivo:** Impedir push direto em `main`, exigir PR e CI verde.

**Tarefas:**

1. No GitHub: Settings → Branches → Add rule
2. Branch name pattern: `main`
3. Marcar:
   - Require a pull request before merging
   - Require status checks to pass before merging (vai aparecer "ci" depois da etapa 1.5)
   - Require branches to be up to date before merging
   - Do not allow bypassing the above settings (importante mesmo trabalhando solo — força disciplina)

**Critério de pronto:**
- [ ] `git push origin main` direto retorna erro
- [ ] PRs exigem CI verde para merge

**Armadilhas:**
- Trabalhando solo, vai parecer burocracia. É exatamente isso que você quer — disciplina forçada por ferramenta, não por força de vontade. Quando agentes começarem a abrir PRs, você vai agradecer.

---

### Etapa 1.3 — Docker Compose: Postgres + Redis

**Objetivo:** Banco e cache rodando localmente via Docker, sem instalação nativa no Windows.

**Tarefas:**

1. Criar `docker-compose.yml` na raiz com:
   - Service `postgres`: imagem `postgres:16-alpine`, porta 5432, volume nomeado, env vars para usuário/senha/db.
   - Service `redis`: imagem `redis:7-alpine`, porta 6379.
   - Healthcheck em ambos.
2. Criar `.env.example` versionado com defaults para dev (NUNCA commitar `.env` com credenciais reais).
3. Adicionar `.env` ao `.gitignore`.
4. Testar: `docker compose up -d`, depois `docker compose ps` deve mostrar ambos `(healthy)`.

**Critério de pronto:**
- [ ] `docker compose up -d` sobe os dois serviços sem erro
- [ ] Conexão psql ao Postgres funcionando (ex: via DBeaver ou `docker compose exec postgres psql`)
- [ ] `docker compose down` derruba sem erro
- [ ] `docker compose down -v` limpa volume (testar pelo menos uma vez)

**Armadilhas:**
- Docker Desktop precisa estar rodando no Windows. Documentar isso no README.
- Volume nomeado, não bind mount. Bind mount no Windows tem permissão chata com Postgres.
- Nunca commitar `.env` com credenciais. Mesmo em projeto solo, é hábito que você quer ter automatizado.

---

### Etapa 1.4 — Inicializar projeto Spring Boot

**Objetivo:** Backend Spring Boot rodando, com dependências da stack instaladas.

**Tarefas:**

1. Acessar https://start.spring.io
2. Configurar:
   - Project: Maven
   - Language: Java
   - Spring Boot: 3.x (última estável)
   - Group: `com.laboratorio`
   - Artifact: `financas`
   - Name: `financas`
   - Package name: `com.laboratorio.financas`
   - Packaging: Jar
   - Java: 21
3. Dependências iniciais:
   - Spring Web
   - Spring Data JPA
   - Spring Security
   - Validation
   - PostgreSQL Driver
   - Flyway Migration
   - Spring Boot Actuator
   - Lombok
   - Testcontainers
4. Generate, baixar zip, extrair conteúdo na raiz do `financas-lab` (mesclando com o que já tem).
5. Adicionar manualmente ao `pom.xml`:
   - MapStruct (1.5+)
   - jjwt (api, impl, jackson) para JWT
   - JaCoCo plugin
   - Checkstyle plugin
   - SpotBugs plugin
   - springdoc-openapi-starter-webmvc-ui
6. `.\mvnw clean install` deve passar.

**Critério de pronto:**
- [ ] `.\mvnw clean install` retorna SUCCESS
- [ ] `.\mvnw spring-boot:run` sobe a aplicação (vai falhar conexão com banco se Docker Compose não estiver up — esperado)
- [ ] Com `docker compose up -d` rodando, `.\mvnw spring-boot:run` sobe e Actuator responde em `http://localhost:8080/actuator/health`

**Armadilhas:**
- `spring.jpa.hibernate.ddl-auto` precisa ser `validate` em todos os ambientes. Spring Initializr não configura isso — fazer manualmente em `application.yml`.
- Profiles: criar `application.yml` (defaults), `application-dev.yml`, `application-test.yml` desde já. Mesmo vazios, a estrutura entra.
- Lombok: certificar que IDE tem plugin Lombok instalado, senão IDE acusa erros que não existem.

---

### Etapa 1.5 — GitHub Actions: CI básico

**Objetivo:** CI rodando lint + test + build em todo PR.

**Tarefas:**

1. Criar `.github/workflows/ci.yml` com job único:
   - Trigger: pull_request e push em main.
   - Steps:
     - Checkout
     - Setup JDK 21 (Temurin)
     - Cache Maven dependencies (crítico para velocidade)
     - `.\mvnw verify` (compila + testa + cobertura)
2. Testar: criar branch `chore/ci-test`, push, abrir PR, ver CI rodando.
3. Quando passar, mergear via PR.

**Critério de pronto:**
- [ ] CI roda em push e PR
- [ ] Cache Maven funcional (segunda execução notavelmente mais rápida)
- [ ] CI verde em PR consegue ser mergeado
- [ ] CI vermelho bloqueia merge (testar com falha proposital, depois corrigir)

**Armadilhas:**
- Cache Maven é vital. Sem ele, cada CI baixa dependências do zero (3-5 min de download). Com cache, segunda execução cai pra 30s de download.
- Não rodar Testcontainers no CI ainda — será adicionado na etapa 2.x quando configurarmos.
- GitHub Actions free tier dá 2.000 min/mês. Cada CI vai usar 2-5 min. Você não vai estourar, mas vale ter em mente.

---

## Semana 2 — Testes, hooks e frontend

### Etapa 2.1 — Testcontainers funcional

**Objetivo:** Testes de integração rodam contra Postgres real via Testcontainers, em ambiente local e no CI.

**Tarefas:**

1. Adicionar Testcontainers PostgreSQL ao `pom.xml`.
2. Criar classe `AbstractIntegrationTest` em `src/test/java/.../shared/` com:
   - `@SpringBootTest`
   - Container Postgres static, reutilizado entre testes
   - `@DynamicPropertySource` injetando URL/user/password
3. Criar primeiro teste de integração placeholder (testa que contexto Spring sobe e Postgres está acessível).
4. `.\mvnw verify` localmente: deve subir container, rodar teste, derrubar container.
5. Atualizar `ci.yml` para garantir Docker disponível (já é por default no `ubuntu-latest`).

**Critério de pronto:**
- [ ] Teste de integração passa local
- [ ] Teste de integração passa no CI
- [ ] Container é reutilizado (verificar logs — só sobe uma vez por execução)

**Armadilhas:**
- Container static `private static final` é o padrão. Container não-static sobe e desce a cada teste — multiplica o tempo de teste por N.
- `testcontainers.reuse.enable=true` em `~/.testcontainers.properties` em desenvolvimento local acelera ainda mais (reutiliza container entre execuções de `mvnw test`). Não usar no CI.

---

### Etapa 2.2 — Primeira migration Flyway

**Objetivo:** Schema versionado, Flyway rodando.

**Tarefas:**

1. Criar `src/main/resources/db/migration/V1__schema_inicial.sql` com:
   - Comentário explicando o que faz
   - Statement vazio ou criação de uma tabela `__healthcheck` placeholder
2. Configurar Flyway em `application.yml`:
   - `spring.flyway.enabled: true`
   - `spring.flyway.locations: classpath:db/migration`
3. Subir aplicação: Flyway deve aplicar V1.
4. Verificar tabela `flyway_schema_history` no Postgres.

**Critério de pronto:**
- [ ] Aplicação sobe e Flyway aplica V1 sem erro
- [ ] Tabela `flyway_schema_history` mostra V1 como applied
- [ ] Subir de novo: Flyway detecta que V1 já foi aplicada, não roda de novo

**Armadilhas:**
- Naming da migration é estrito: `V{N}__{descricao_em_snake_case}.sql`. Dois underscores entre versão e descrição.
- Editar migration **após** ela ser aplicada quebra o checksum. Se errar nessa primeira, dropar `flyway_schema_history` e recomeçar é aceitável. Em produção, jamais.

---

### Etapa 2.3 — Hello-world endpoint com teste e2e

**Objetivo:** Validar que toda a stack funciona end-to-end com teste real.

**Tarefas:**

1. Criar package `com.laboratorio.financas.healthcheck.interfaces.rest`.
2. Criar `HealthcheckController` com endpoint `GET /api/healthcheck` retornando `{"status": "ok", "timestamp": "..."}`.
3. Criar teste e2e `HealthcheckControllerIT` em `src/test/java/.../healthcheck/`:
   - Usa `AbstractIntegrationTest`
   - `@AutoConfigureMockMvc` ou `TestRestTemplate`
   - Testa que GET retorna 200 e payload correto
4. Rodar `.\mvnw verify`: tudo verde.
5. Push, PR, CI verde, merge.

**Critério de pronto:**
- [ ] Endpoint responde 200 ao acessar `http://localhost:8080/api/healthcheck`
- [ ] Teste e2e passa local
- [ ] Teste e2e passa no CI
- [ ] PR mergeado em main

**Armadilhas:**
- Não adicionar nada de auth nesse endpoint ainda. Spring Security vem na Camada 2.
- Resistir à tentação de "já criar endpoint de transação". Esta etapa é validar pipeline end-to-end, não construir feature.

---

### Etapa 2.4 — JaCoCo com thresholds por camada

**Objetivo:** Cobertura medida e CI falha se cobertura cair abaixo dos thresholds do `decisoes.md`.

**Tarefas:**

1. Configurar JaCoCo no `pom.xml`:
   - `prepare-agent` antes de testes
   - `report` após testes
   - `check` com regras por camada:
     - Pacote `domain`: 90%
     - Pacote `application`: 80%
     - Pacote `infrastructure`: 60%
     - Pacote `interfaces`: 70%
     - Total: 75%
2. `.\mvnw verify` deve gerar relatório em `target/site/jacoco/index.html`.
3. Configurar workflow do CI para falhar se thresholds não forem atingidos (já vem do `mvnw verify` se `check` está configurado).

**Critério de pronto:**
- [ ] Relatório JaCoCo gerado em `target/site/jacoco/`
- [ ] CI valida thresholds e falha se não bater
- [ ] Healthcheck endpoint atual cobre suficientemente para passar (deve ser fácil — pouco código)

**Armadilhas:**
- Thresholds altos em projeto novo são fáceis. Eles ficam difíceis quando o código cresce. Isso é proposital — força a escrever teste junto com código.
- Configuração JaCoCo no `pom.xml` é verbosa e cheia de pegadinhas. Use exemplo da documentação oficial. Se demorar mais que 1 hora pra acertar, aceitar threshold global de 75% e refinar por camada na Camada 2 é razoável.

---

### Etapa 2.5 — Análise estática: Checkstyle e SpotBugs

**Objetivo:** Convenções de código aplicadas automaticamente. Bugs comuns detectados antes de testes.

**Tarefas:**

1. Adicionar plugin Checkstyle ao `pom.xml`:
   - Usar perfil Google Style ou Sun Coding Conventions como base, customizar mínimo.
   - Configurar para falhar build em violações.
2. Adicionar plugin SpotBugs ao `pom.xml`:
   - Effort: max
   - Threshold: medium (low gera muito ruído)
   - Excluir filtros conhecidos (false positives típicos do Spring)
3. `.\mvnw verify` deve rodar ambos sem erro no código atual.

**Critério de pronto:**
- [ ] Checkstyle valida e passa
- [ ] SpotBugs analisa e passa
- [ ] CI executa ambos
- [ ] Violação proposital quebra o build (testar com warning suppression em algum lugar, depois corrigir)

**Armadilhas:**
- Configuração de Checkstyle muito rigorosa no início vira atrito constante. Comece permissivo (linhas até 140 chars, etc) e aperte conforme convenções emergem.
- SpotBugs Effort=max é lento (1-3 min em projeto pequeno). Em projetos maiores, pode subir para 5+ min. Se virar gargalo, baixar para "default".

---

### Etapa 2.6 — Scripts PowerShell

**Objetivo:** Comandos atômicos do projeto (princípio "documentação executável").

**Tarefas:**

1. Criar `scripts/setup.ps1`:
   - `docker compose up -d`
   - `.\mvnw clean install -DskipTests`
   - Mensagem de sucesso
2. Criar `scripts/dev.ps1`:
   - Verifica Docker rodando
   - `docker compose up -d`
   - `.\mvnw spring-boot:run`
3. Criar `scripts/test.ps1`:
   - `.\mvnw test` (apenas testes unitários, sem `-Dintegration`)
4. Criar `scripts/test-integration.ps1`:
   - `.\mvnw verify` (com Testcontainers)
5. Criar `scripts/check.ps1`:
   - Espelha o que CI roda: `.\mvnw verify`
6. Criar `scripts/ship.ps1`:
   - Roda `check.ps1`
   - Se passar, faz `git push`
   - Sugere comando `gh pr create` (ou cria via GitHub CLI se disponível)
7. Documentar no README os comandos.

**Critério de pronto:**
- [ ] Todos os scripts executam sem erro
- [ ] README atualizado com tabela de comandos
- [ ] Pré-aprovar `Bash(./scripts/*.ps1)` no `settings.json` do Claude Code (ou `Bash(powershell *)`)

**Armadilhas:**
- PowerShell por default exige permissão para executar scripts: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`. Documentar.
- Caminhos com espaços precisam aspas duplas ou escape. Evitar projetos em `C:\Users\Nome com Espaço\`.

---

### Etapa 2.7 — Inicializar projeto Next.js

**Objetivo:** Frontend inicializado, build passando, sem feature ainda.

**Tarefas:**

1. Decidir estrutura: monorepo ou pasta separada? **Recomendação: pasta `frontend/` na raiz do mesmo repo** (não monorepo formal — só um diretório).
2. `cd frontend; npx create-next-app@latest . --typescript --tailwind --eslint --app --src-dir --import-alias "@/*"`
3. Adicionar dependências:
   - `next-pwa` (configurar em `next.config.js`)
   - `@tanstack/react-query`
   - `zod`
   - `react-hook-form` + `@hookform/resolvers`
4. `npm run build` deve passar.
5. `npm run dev` deve subir Next em `http://localhost:3000`.
6. Adicionar steps no `ci.yml` para validar frontend (lint + build).

**Critério de pronto:**
- [ ] `frontend/` inicializado
- [ ] `npm run build` passa
- [ ] CI valida frontend
- [ ] Página inicial padrão do Next.js carrega

**Armadilhas:**
- Não configurar shadcn/ui ainda — entra na Camada 2 quando começar a ter telas.
- Não criar páginas de feature. Validar pipeline e parar.

---

### Etapa 2.8 — Atualização de docs e wrap-up

**Objetivo:** Documentação reflete o estado real ao final da Camada 1.

**Tarefas:**

1. Atualizar `progresso.md`:
   - Marcar Camada 1 como ✅ Concluída
   - Adicionar lições aprendidas
   - Marcar Camada 2 como 🔵 Próxima
2. Atualizar `decisoes.md` se alguma decisão emergiu durante a implementação (provavelmente vai emergir 2-3).
3. Adicionar ao README a tabela de comandos PowerShell e link para `docs/`.
4. Commit final da Camada 1.

**Critério de pronto:**
- [ ] `progresso.md` reflete estado real
- [ ] README útil para alguém entrando no projeto pela primeira vez (você daqui a 2 meses, por exemplo)
- [ ] Push em main com CI verde

---

## Definição de "Camada 1 concluída"

A Camada 1 está concluída quando **todos** os critérios abaixo são verdade simultaneamente:

- [ ] Você consegue clonar o repo numa máquina nova, rodar `.\scripts\setup.ps1` e ter tudo funcionando em menos de 10 minutos
- [ ] CI verde no `main` há pelo menos 3 commits consecutivos
- [ ] Cobertura JaCoCo nos thresholds
- [ ] Pelo menos 1 PR foi rejeitado pelo CI por motivo legítimo (aprendizado)
- [ ] Você confia que se CI está verde, código está mergeable sem segunda revisão linha a linha
- [ ] `progresso.md` atualizado

**A última condição é a mais importante.** Se você ainda não confia no CI, a Camada 1 não terminou — falta calibração.

---

## O que vem depois (preview Camada 2)

Quando a Camada 1 estiver fechada, próximos passos:

1. Implementar value object `Money`
2. Implementar bounded context `conta` completo (domain + application + infrastructure + interfaces)
3. Implementar bounded context `categoria`
4. Configurar Spring Security com JWT + refresh
5. Endpoints de auth funcionais
6. Primeira feature implementada manualmente do início ao fim para servir de referência aos agentes

A Camada 2 é onde você ainda escreve a maior parte do código manualmente, mas com a fundação sólida para começar a delegar partes.

A delegação real vira norma na Camada 4.

---

## Tempo realista por etapa

Para calibrar expectativa (essas estimativas assumem foco e ambiente sem distração):

| Etapa | Estimativa |
|---|---|
| 1.1 | 30 min |
| 1.2 | 15 min |
| 1.3 | 1-2h |
| 1.4 | 2-3h |
| 1.5 | 1-2h |
| 2.1 | 2-3h |
| 2.2 | 30 min |
| 2.3 | 1-2h |
| 2.4 | 1-2h |
| 2.5 | 2-3h |
| 2.6 | 1-2h |
| 2.7 | 1-2h |
| 2.8 | 30 min |
| **Total** | **15-25h de foco real** |

Em ritmo de 2-3h/dia útil, fecha em 2 semanas confortavelmente. Em ritmo de fim de semana, fecha em 3-4 fins de semana.

**Não correr.** Camada 1 mal feita compromete tudo daqui pra frente.
