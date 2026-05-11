# Prompt — Etapa 2.3: Healthcheck endpoint com teste e2e

## Contexto

A Etapa 2.2 (primeira migration Flyway) foi concluída e fechada via PR #19. `main` está em `fdd9918`, working tree limpo.

Esta etapa cria o primeiro endpoint HTTP do projeto (`GET /api/healthcheck`), valida a stack end-to-end com teste de integração real, e configura `SecurityFilterChain` mínimo permitindo o endpoint público (e a doc OpenAPI) enquanto bloqueia o resto do mundo com `authenticated()`.

Objetivo do roadmap: validar que toda a stack funciona end-to-end com teste real.

Escopo decidido nesta etapa (calibrado com operador antes da redação):

- Spring Security ativo (já está no classpath desde a 1.4) com `SecurityFilterChain` configurado, whitelist explícita pra endpoints públicos (healthcheck + actuator/health + OpenAPI/Swagger), `authenticated()` no resto. JWT vem na Camada 2 substituindo o `authenticated()` por filtro JWT.
- Endpoint mora em `shared/infrastructure/web/` — healthcheck é infra técnica, não bounded context de domínio. Precedente registrado em `decisoes.md`.
- Endpoint NÃO consulta a tabela `__healthcheck`. Healthcheck deve ser leve. A tabela existe pra validar que Flyway aplicou, e isso já é coberto pelo `FlywayMigrationTest` da 2.2.
- Naming de teste: sufixo `Test` (singular). `FinancasApplicationTests` (plural, gerado pelo Spring Initializr) fica como exceção tolerada — não renomear nesta etapa.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra `fdd9918 feat: etapa 2.2 — primeira migration Flyway (V1 schema inicial) (#19)`
- `docs/prompt-etapa-2-3.md` presente como untracked (este próprio arquivo, anexado pelo operador antes de iniciar). Se não aparecer em `git status`, parar e reportar — significa que o arquivo não foi anexado ou está com nome diferente.
- Working tree sem outras mudanças além do prompt untracked acima
- Docker Desktop rodando (necessário pra Testcontainers no `mvnw verify`)

Validar com `git status` e `git log --oneline -1` antes de começar. Se estado divergir, parar e reportar.

## Tarefas

### Tarefa 1 — `SecurityConfig` mínimo

**Caminho:** `src/main/java/com/laboratorio/financas/shared/infrastructure/security/SecurityConfig.java`

Criar pacote `shared.infrastructure.security` (não existe ainda) e classe de configuração:

- Anotada com `@Configuration` e `@EnableWebSecurity`
- Bean `SecurityFilterChain` com:
  - `csrf().disable()` (API stateless, sem cookies de sessão pra proteger nesta fase; quando JWT entrar e cookie de refresh existir, revisitar)
  - `sessionManagement` em `STATELESS`
  - `authorizeHttpRequests` com whitelist explícita:
    - `permitAll` em: `/api/healthcheck`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`
    - `authenticated()` em qualquer outro request
  - `httpBasic` e `formLogin` desabilitados (`.disable()` em ambos) — não queremos prompt de login do browser nem form padrão do Spring; quando JWT entrar, ele será o único caminho de auth.

Esqueleto sugerido (ajustar imports e estilo conforme padrão do projeto):

```java
package com.laboratorio.financas.shared.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/healthcheck",
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .build();
    }
}
```

Comentário no topo da classe (Javadoc curto) explicando que `authenticated()` é placeholder até JWT chegar na Camada 2.

### Tarefa 2 — DTO de resposta do healthcheck

**Caminho:** `src/main/java/com/laboratorio/financas/shared/infrastructure/web/HealthcheckResponse.java`

Record imutável com dois campos:

```java
package com.laboratorio.financas.shared.infrastructure.web;

import java.time.Instant;

public record HealthcheckResponse(String status, Instant timestamp) {
}
```

Justificativa (não vai pro código): record alinhado com a regra de `decisoes.md` ("imutabilidade preferencial; usar `record` para value objects sempre que possível"). `Instant` serializa em ISO-8601 UTC por default no Jackson (configuração padrão do Spring Boot).

### Tarefa 3 — Controller do healthcheck

**Caminho:** `src/main/java/com/laboratorio/financas/shared/infrastructure/web/HealthcheckController.java`

```java
package com.laboratorio.financas.shared.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/healthcheck")
public class HealthcheckController {

    @GetMapping
    public HealthcheckResponse healthcheck() {
        return new HealthcheckResponse("ok", Instant.now());
    }
}
```

Sem `@Tag` do springdoc nesta etapa — anotação OpenAPI rica entra quando primeiro endpoint de domínio for documentado.

### Tarefa 4 — Teste e2e

**Caminho:** `src/test/java/com/laboratorio/financas/shared/infrastructure/web/HealthcheckControllerTest.java`

Estende `AbstractIntegrationTest` (sobe Testcontainers Postgres e contexto Spring completo, herdado da 2.1). Usa `MockMvc` configurado via `@AutoConfigureMockMvc`.

Esqueleto sugerido:

```java
package com.laboratorio.financas.shared.infrastructure.web;

import com.laboratorio.financas.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class HealthcheckControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveRetornarStatusOkETimestampValido() throws Exception {
        mockMvc.perform(get("/api/healthcheck"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.timestamp").value(
                        matchesPattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z$")
                ));
    }

    @Test
    void deveBloquearEndpointNaoMapeadoComUnauthorized() throws Exception {
        mockMvc.perform(get("/api/qualquer-coisa-protegida"))
                .andExpect(status().isUnauthorized());
    }
}
```

Justificativa do segundo teste (não vai pro código): valida que o `SecurityFilterChain` está realmente protegendo o que não está na whitelist. Sem este teste, podemos deployar com Security mal configurado e nem perceber.

### Tarefa 5 — Validar localmente

```powershell
cd C:\projetos\financas-lab
.\mvnw clean verify
```

Esperado:

- `Tests run: 5` (1 do `FinancasApplicationTests.contextLoads()` + 2 do `FlywayMigrationTest` + 2 do `HealthcheckControllerTest`)
- `BUILD SUCCESS`
- Tempo total: 30-90s

Validação manual via curl (opcional mas educativa):

```powershell
docker compose up -d postgres
.\mvnw spring-boot:run
# em outro terminal:
curl http://localhost:8080/api/healthcheck
# Esperado: HTTP 200, JSON {"status":"ok","timestamp":"2026-05-08T..."}
curl -i http://localhost:8080/api/qualquer-rota-nao-mapeada
# Esperado: HTTP 401 Unauthorized
# Ctrl+C no spring-boot:run
docker compose down
```

### Tarefa 6 — Atualizar `decisoes.md`

Três adições, todas curtas:

**6a.** Em **"Convenções de código"** → **"Estrutura de pacotes do backend"**, após o bloco existente, adicionar nota explicativa (não modificar a estrutura prescrita; apenas registrar precedente):

```markdown
**Endpoints técnicos** (healthcheck, métricas, debug) ficam em `shared/infrastructure/web/`, não em bounded context próprio. Bounded contexts são reservados pra domínio de negócio. Precedente: `HealthcheckController` (Etapa 2.3).
```

**6b.** Em **"Convenções de código"** → **"Testes"**, ajustar o bullet sobre naming pra registrar a convenção:

```markdown
- **Naming de classe de teste:** sufixo `Test` (singular) é o padrão. Sufixo `Tests` (plural) é tolerado em classes geradas pelo Spring Initializr (`FinancasApplicationTests`) e não deve ser usado em classes novas. Sufixo `IT` (convenção Maven Failsafe) **não é usado** neste projeto — Failsafe não está configurado e Surefire não pega esse sufixo por default.
```

(Se já existe um bullet sobre naming nessa seção, substituir; se não, adicionar entre os existentes em ordem coerente.)

**6c.** Em **"Convenções operacionais"** ou em seção apropriada (procurar onde "Spring Security" aparece; se não aparecer, adicionar nova subseção curta em "Padrões aplicados" ou "Spring específico"):

```markdown
- **`SecurityFilterChain` com whitelist explícita.** Endpoints públicos são listados em `requestMatchers(...).permitAll()`; o resto é `authenticated()`. Enquanto JWT não está implementado (Camada 2), `authenticated()` funciona como bloqueio efetivo (qualquer request não-whitelisted retorna 401). Whitelist atual: `/api/healthcheck`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`. Quando JWT entrar, `authenticated()` é substituído por filtro JWT — não relaxar pra `permitAll()` global em hipótese alguma.
```

**6d.** Adicionar linha no histórico de mudanças no fim do arquivo, no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.3 concluída: primeiro endpoint HTTP (`GET /api/healthcheck`), `SecurityFilterChain` mínimo com whitelist explícita, precedente sobre endpoints técnicos em `shared/infrastructure/web/`, convenção de naming de teste formalizada.
```

### Tarefa 7 — Atualizar `progresso.md`

**7a.** Atualizar campo "Última atualização" no topo: `2026-05-08 (Etapa 2.3)`.

**7b.** Marcar como `[x]` o critério `Hello-world endpoint passando teste e2e via Testcontainers` na seção da Camada 1.

**7c.** Adicionar nova seção **"Lições da Etapa 2.3"** logo antes de **"Licoes da Etapa 2.2"** (mantendo ordem decrescente).

Conteúdo: candidatos a hook e lições de ambiente que **realmente forem observados durante a execução**. Se nada digno emergir, escrever explicitamente:

```markdown
## Lições da Etapa 2.3

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Licoes de ambiente

(Nenhuma nova nesta etapa.)
```

**Regra dura:** só registrar lições que **realmente foram observadas durante a execução**. Se Spring Security der erro de configuração, MockMvc tiver pegadinha de auto-config, JSON serialization de `Instant` der formato diferente do esperado, etc. — registrar honestamente. Não inventar.

**7d.** Adicionar entrada no **"Histórico de mudanças deste documento"** no topo da lista. Texto base com placeholder explícito:

```markdown
- **2026-05-08** — Etapa 2.3 concluída: HealthcheckController em `/api/healthcheck`, SecurityConfig com whitelist explícita, HealthcheckControllerTest com 2 testes (status + bloqueio de não-whitelisted). Mergeado via PR #XX.
```

(O `#XX` é placeholder. Será substituído pelo número real do PR num commit adicional **depois** que o PR for aberto, conforme passo final desta etapa — ver "Estrutura de commits" e "Pós-criação do PR" abaixo.)

### Tarefa 8 — Versionar este próprio prompt

Confirmar que `docs/prompt-etapa-2-3.md` está em disco como untracked, e incluir no commit de docs.

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Arquivos permitidos:
   - `src/main/java/com/laboratorio/financas/shared/infrastructure/security/SecurityConfig.java` (novo)
   - `src/main/java/com/laboratorio/financas/shared/infrastructure/web/HealthcheckController.java` (novo)
   - `src/main/java/com/laboratorio/financas/shared/infrastructure/web/HealthcheckResponse.java` (novo)
   - `src/test/java/com/laboratorio/financas/shared/infrastructure/web/HealthcheckControllerTest.java` (novo)
   - `docs/decisoes.md`
   - `docs/progresso.md`
   - `docs/prompt-etapa-2-3.md` (este arquivo)

2. **"Necessidade técnica direta" não é exceção válida à Restrição 1.** Se ao executar você identificar que algo precisa mudar fora dessa lista (`pom.xml`, `application.yml`, `AbstractIntegrationTest`, qualquer outro arquivo), **pare e reporte** ao operador antes de tocar. Lição registrada na Etapa 2.2: tentar resolver gap silenciosamente é exatamente o padrão que essa restrição existe pra bloquear.

3. **Não criar `@Entity` JPA pra `__healthcheck`.** Endpoint não consulta a tabela. A tabela continua existindo só pra validar pipeline Flyway, e o `FlywayMigrationTest` da 2.2 já cobre isso.

4. **Não adicionar JWT, login, signup, refresh token, BCrypt, ou qualquer pedaço de auth real.** Tudo isso é Camada 2 (ADR-005). `SecurityFilterChain` com whitelist + `authenticated()` é o **máximo** desta etapa. Se aparecer tentação de "já preparar" filtro JWT vazio, recusar.

5. **Não adicionar feature flags, métricas customizadas, healthcheck profundo (DB ping, Redis ping, etc.) nem dependências novas no `pom.xml`.** Healthcheck mínimo é literalmente status hardcoded "ok". Profundidade vem se/quando justificar — não preventivamente.

6. **Não criar bounded context próprio pra healthcheck.** Decidido: vai em `shared/infrastructure/web/`. Não criar pacote `healthcheck/` no nível raiz nem subpacotes `domain/`, `application/`, `interfaces/` pra essa funcionalidade.

7. **Não antecipar Etapa 2.4 (JaCoCo thresholds).** Não tocar em `pom.xml`, não ajustar `<configuration>` do JaCoCo plugin. Cobertura desta etapa só precisa não regredir em relação à 2.2; thresholds por camada vêm na 2.4.

8. **Validar conteúdo bruto antes de commitar.** Para arquivos com acentos: `Get-Content <path> -Encoding UTF8`. Para `.java`: garantir UTF-8 sem BOM (`Out-File -Encoding UTF8` adiciona BOM e `javac` rejeita — lição registrada em `decisoes.md`).

9. **Pesquisar antes de chutar sintaxe Spring Security 6.** A API mudou bastante entre 5 e 6 (removido `WebSecurityConfigurerAdapter`, lambdas DSL passaram a ser obrigatórios em vários pontos). Em dúvida, consultar documentação oficial da versão 6.x — não memória.

## Estrutura de commits

Branch: `feat/healthcheck-endpoint`

Commits atômicos, em ordem:

**Commit 1** — `feat: SecurityConfig com whitelist explicita e authenticated por padrao`
- `src/main/java/com/laboratorio/financas/shared/infrastructure/security/SecurityConfig.java`

**Commit 2** — `feat: endpoint GET /api/healthcheck`
- `src/main/java/com/laboratorio/financas/shared/infrastructure/web/HealthcheckController.java`
- `src/main/java/com/laboratorio/financas/shared/infrastructure/web/HealthcheckResponse.java`

**Commit 3** — `test: HealthcheckControllerTest cobrindo status 200 e bloqueio 401`
- `src/test/java/com/laboratorio/financas/shared/infrastructure/web/HealthcheckControllerTest.java`

**Commit 4** — `docs: registra etapa 2.3 (healthcheck) em decisoes e progresso`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-2-3.md`

## Validação antes de abrir PR

```powershell
git status                                                          # working tree limpo
git log --oneline -6                                                # 4 commits novos visiveis
.\mvnw clean verify                                                 # BUILD SUCCESS, Tests run: 5
Get-Content src/main/java/com/laboratorio/financas/shared/infrastructure/security/SecurityConfig.java -Encoding UTF8 | Select-Object -First 10
Get-Content docs/progresso.md -Encoding UTF8 | Select-String "Etapa 2.3"
```

## PR

Título: `feat: etapa 2.3 — endpoint healthcheck com SecurityConfig minimo`

Body sugerido (ajustar conforme execução real):

```markdown
## Summary

Implementa a Etapa 2.3 do roadmap: primeiro endpoint HTTP do projeto, validação end-to-end da stack, configuração inicial de Spring Security com whitelist explícita.

### Mudanças

- `SecurityConfig`: `SecurityFilterChain` com whitelist (`/api/healthcheck`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`) e `authenticated()` no resto. CSRF desabilitado (API stateless), session policy STATELESS, httpBasic e formLogin desabilitados.
- `HealthcheckController` em `shared/infrastructure/web/`: endpoint `GET /api/healthcheck` retornando `{"status":"ok","timestamp":"<ISO-8601 UTC>"}` via record `HealthcheckResponse`.
- `HealthcheckControllerTest`: dois testes — (a) endpoint retorna 200 com JSON correto, (b) request a path não-whitelisted retorna 401.
- `decisoes.md`: registra precedente sobre endpoints técnicos em `shared/infrastructure/web/`, formaliza convenção de naming de teste (`Test` singular padrão, `Tests` tolerado pra classes do Initializr, `IT` não usado), documenta política do `SecurityFilterChain` com whitelist.
- `progresso.md`: marca critério Hello-world endpoint como concluído, registra lições da etapa.

### Validação

- `mvnw clean verify` local: BUILD SUCCESS, Tests run: 5
- `curl http://localhost:8080/api/healthcheck` retorna 200 com payload JSON esperado
- `curl http://localhost:8080/api/qualquer-rota` retorna 401 (Security funcionando)

### Decisões de escopo

- Endpoint NÃO consulta a tabela `__healthcheck`. Healthcheck deve ser leve; tabela existe pra validar Flyway, coberto pelo `FlywayMigrationTest` da 2.2.
- `SecurityFilterChain` com `authenticated()` no resto é placeholder até JWT (Camada 2). Não relaxar pra `permitAll()` global em hipótese alguma.

### Próximo passo

Etapa 2.4 (JaCoCo com thresholds por camada) — fora do escopo deste PR.
```

## Pós-criação do PR

Antes de mergear, fazer commit adicional **na mesma branch** corrigindo o `#XX` do `progresso.md` pelo número real do PR:

1. Abrir o PR via `gh pr create` (já planejado acima).
2. Capturar o número do PR retornado pelo comando.
3. Editar `docs/progresso.md` substituindo `Mergeado via PR #XX` por `Mergeado via PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico do progresso.md`
5. Push na mesma branch.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- `main` local sincronizada com `origin/main`, incluindo squash commit da 2.3
- `git status` limpo
- `mvnw clean verify` passa local com `Tests run: 5`
- `docs/progresso.md` reflete 2.3 concluída, número real do PR no histórico
- Branch `feat/healthcheck-endpoint` deletada local e remotamente

Reportar ao operador o estado final com `git log --oneline -3` e `git status`. Parar.

## O que NÃO fazer ao terminar

- Não criar prompt da 2.4
- Não tocar em `pom.xml` pra adicionar Failsafe, ajustar JaCoCo, adicionar dependência de auth, nada
- Não criar `application-dev.yml` ou `application-prod.yml`
- Não criar entidades JPA, repositórios, ou bounded contexts de domínio
- Não sugerir "próximo passo" espontaneamente. Fim de etapa = parada explícita.
