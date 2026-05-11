# Prompt — Etapa 2.1 da Camada 1

Você está trabalhando no projeto `financas-lab`. Estamos na **Etapa 2.1 da Camada 1** — configurar Testcontainers para que testes de integração rodem contra Postgres real, tanto localmente quanto no CI. Esta etapa também resolve um débito técnico consciente da Etapa 1.5: a exclusão do `FinancasApplicationTests` no workflow CI.

## Antes de qualquer ação, leia em ordem:

1. `docs/visao.md` — propósito do projeto
2. `docs/decisoes.md` — stack, padrões e regras duras (especialmente seção "Política de débito técnico consciente" e "Ambiente de desenvolvimento")
3. `docs/adrs.md` — ADR-003 (PostgreSQL em dev e prod), ADR-007 (Testes em três níveis com Testcontainers)
4. `docs/progresso.md` — estado atual e lições anteriores
5. `docs/roadmap-camada-1.md` — Etapa 2.1 detalhada

Após ler, apresente um resumo de 5-7 linhas do que entendeu da tarefa antes de propor qualquer arquivo.

## Contexto e objetivo

Hoje, `FinancasApplicationTests.contextLoads()` falha localmente porque tenta subir contexto Spring sem datasource configurado, e está **excluído do CI** via `-Dtest='!FinancasApplicationTests'` no workflow. Esta etapa:

1. Cria classe base `AbstractIntegrationTest` que sobe Postgres real via Testcontainers
2. Garante que `FinancasApplicationTests.contextLoads()` passe usando esse container
3. Remove a exclusão no `ci.yml` (resolve o débito técnico)
4. Garante que o teste passe no CI (Ubuntu) também, não só no Windows local

**Princípio operacional desta etapa:** mudança em CI exige validação local primeiro, e validação remota (push pra branch + observar GitHub Actions) **antes** de mergear.

## Tarefa

### Branch

Criar a branch `feat/testcontainers-base` a partir de `main` atualizada.

### Arquivos a criar

#### 1. `src/test/java/com/laboratorio/financas/shared/AbstractIntegrationTest.java`

Classe base abstrata para testes de integração. Requisitos não-negociáveis:

- Pacote: `com.laboratorio.financas.shared` (subpacote `shared` do projeto, conforme estrutura do ADR-004)
- Anotações:
  - `@SpringBootTest` (sobe contexto Spring completo)
  - `@ActiveProfiles("test")` (usa profile de teste)
  - `@Testcontainers` (registra ciclo de vida dos containers)
- Container Postgres como **field static final**:
  - Imagem: **`postgres:16-alpine`** (mesma do `docker-compose.yml`, paridade com prod)
  - Configurar `withDatabaseName("financas_test")`, `withUsername("test")`, `withPassword("test")`
  - **Container reutilizado entre testes da mesma JVM** (static, não @Container em campo de instância)
- `@DynamicPropertySource` injetando JDBC URL/username/password no Spring:
  - `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`
  - Também ajustar `spring.flyway.url`, `spring.flyway.user`, `spring.flyway.password` (Flyway lê separado em alguns casos)
- Classe **abstract** (não pode ser instanciada diretamente, só estendida)

Estrutura aproximada (você pode ajustar imports conforme necessário):

```java
package com.laboratorio.financas.shared;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRES = 
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("financas_test")
            .withUsername("test")
            .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }
}
```

**Notas técnicas importantes:**

- `static { POSTGRES.start(); }` em bloco static garante que o container sobe uma vez por JVM e é reutilizado entre testes. Sem isso, container pode subir/descer entre testes da mesma classe.
- `@SuppressWarnings("resource")` é apropriado porque o container vive pela vida da JVM (não precisa de try-with-resources).
- Não usar `@Container` em campo de instância — isso liga o container ao ciclo de vida do método, contrário ao que queremos.

### Arquivos a modificar

#### 2. `src/test/java/com/laboratorio/financas/FinancasApplicationTests.java`

Trocar o teste para estender a classe base:

```java
package com.laboratorio.financas;

import com.laboratorio.financas.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class FinancasApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
```

**Remover** as anotações `@SpringBootTest` e `@ActiveProfiles("test")` daqui — agora elas vêm da classe base. Manter elas nos dois lugares causa avisos e pode dar comportamento estranho de configuração.

#### 3. `src/main/resources/application-test.yml`

Atualizar o arquivo (atualmente é placeholder de 3 linhas) para conter configuração mínima de teste:

```yaml
# Configuracao de teste.
# Datasource e Flyway sao injetados via @DynamicPropertySource
# pela classe AbstractIntegrationTest (Testcontainers).
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
        show_sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

**Observação técnica:** `baseline-on-migrate: true` aqui (diferente do `false` do `application.yml`) porque banco de teste é criado do zero pelo Testcontainers — Flyway precisa baselinar para não reclamar de schema vazio.

**Não declarar `spring.datasource.url`** aqui — vem do `@DynamicPropertySource` em runtime.

#### 4. `.github/workflows/ci.yml`

Remover a exclusão do `FinancasApplicationTests`. Substituir:

```yaml
      - name: Build com testes
        # FinancasApplicationTests.contextLoads falha esperadamente nesta etapa:
        # contexto Spring nao sobe sem Postgres real (Testcontainers configurado na Etapa 2.1).
        # Debito tecnico consciente — exclusao sera removida na Etapa 2.1.
        run: ./mvnw verify -Dtest='!FinancasApplicationTests' -Dsurefire.failIfNoSpecifiedTests=false
```

Por:

```yaml
      - name: Build com testes
        # Testcontainers sobe Postgres real para testes de integracao.
        # Docker disponivel por padrao no runner ubuntu-latest.
        run: ./mvnw verify
```

### Validações obrigatórias antes de commitar

Execute localmente, na ordem:

1. **Garantir que NENHUM container `financas-lab-*` está rodando do docker-compose:**
   ```
   docker compose down
   docker ps
   ```
   Se aparecer container `financas-lab-postgres` ou similar, derrubar antes — Testcontainers vai criar containers próprios em portas dinâmicas, mas concorrer com docker-compose ativo pode confundir output.

2. `./mvnw clean verify 2>&1 | Select-Object -Last 40` (ou `tail -40` se em bash) — primeira execução completa, espera-se BUILD SUCCESS com `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`.
   - Pode demorar 30-60s na primeira vez (Testcontainers baixa imagem se não tem em cache, sobe container, roda Flyway, sobe Spring, executa teste)
   - Se já tem `postgres:16-alpine` localmente (deve ter, do docker-compose), o pull é instantâneo

3. `docker ps` durante execução do teste (em outro terminal, opcional) — deve mostrar container Testcontainers ativo, separado do docker-compose. Não obrigatório, é só diagnóstico.

4. Após o teste:
   ```
   docker ps -a --filter "label=org.testcontainers"
   ```
   Esperado: containers parados ou removidos (Testcontainers gerencia lifecycle).

5. `git status` — deve mostrar:
   - Novo: `src/test/java/com/laboratorio/financas/shared/AbstractIntegrationTest.java`
   - Modificado: `src/test/java/com/laboratorio/financas/FinancasApplicationTests.java`
   - Modificado: `src/main/resources/application-test.yml`
   - Modificado: `.github/workflows/ci.yml`

6. `./mvnw verify` (sem `clean`) — segunda execução. Deve ser mais rápida (cache aquecido, possível reuso de container se `~/.testcontainers.properties` permitir).

Se qualquer validação falhar, **pare e reporte. Não tente consertar criativamente.**

### Commit e PR

Após validações:

1. Mostre `git status` e peça confirmação.
2. Após confirmação, **commits atômicos**:
   - Commit 1: `test: adiciona AbstractIntegrationTest com testcontainers postgres` (apenas `AbstractIntegrationTest.java` + `FinancasApplicationTests.java` + `application-test.yml`)
   - Commit 2: `ci: remove exclusao do FinancasApplicationTests apos testcontainers` (apenas `ci.yml`)
3. Push.
4. Abrir PR via `gh` CLI:
   - Title: `feat: testcontainers para testes de integracao com postgres real`
   - Body com 5 seções:
     - **Summary** — bullets do que foi adicionado/modificado
     - **Resolução de débito técnico** — referenciar a exclusão da Etapa 1.5 sendo removida
     - **Comportamento esperado** — agora `FinancasApplicationTests` roda em PR (local + CI)
     - **Validações executadas** — outputs locais
     - **Riscos e atenção** — primeira execução do CI vai puxar imagem `postgres:16-alpine` no runner; pode demorar mais que o usual (estimativa: 60-90s na primeira vez, depois fica em cache do cache de actions ou simplesmente baixa rápido)

5. **Após abrir o PR, PARE.** Aguardar o CI rodar — primeira execução pode demorar mais que os 27s típicos por causa do pull da imagem Docker do Postgres no runner. Não inicie configurações de hooks, próximas etapas, ou qualquer outra coisa.

## Restrições importantes

- **Não criar pasta `domain/`, `application/`, `infrastructure/`, `interfaces/`** — Clean Architecture entra na Camada 2.
- **Não criar `Money`, entidades, use cases, repositórios.** Camada 2.
- **Não criar Flyway migrations** ainda — Etapa 2.2.
- **Não criar Hello-world endpoint.** Etapa 2.3.
- **Não modificar `pom.xml`.** Testcontainers já está nas dependências desde a Etapa 1.4.
- **Não modificar `docker-compose.yml`** ou `application-dev.yml` — Testcontainers é independente do docker-compose.
- **Não criar configuração de reuse de container** (`~/.testcontainers.properties`) automaticamente — isso é decisão de máquina dev individual, não vai pro repo.
- **Pergunte antes de cada commit.**
- **Não force push.**
- **Após PR aberto e CI verde, PARE.** Não mergear automático, não atualizar `progresso.md`, não iniciar 2.2.

## Pegadinhas conhecidas

1. **`@Container` vs `static`:** se você usar `@Container static`, JUnit Jupiter Testcontainers extension já cuida do start/stop. Se não usar `@Container`, o `static { POSTGRES.start(); }` faz o trabalho. Use uma das duas, **não as duas** (causa double start). O prompt acima usa a segunda — mais explícita e robusta.

2. **JDBC URL gerada pelo Testcontainers** é dinâmica: `jdbc:postgresql://localhost:RANDOM_PORT/financas_test`. Por isso precisa do `@DynamicPropertySource`, não dá pra hardcodar em `application-test.yml`.

3. **Flyway no profile test:** se houver migrations em `db/migration`, Flyway aplica antes do teste rodar. Como ainda não temos migration nenhuma (V1 só vem na Etapa 2.2), Flyway vai validar e não fazer nada — esperado.

4. **`baseline-on-migrate: true`** no profile test permite que Flyway aceite banco vazio sem erro. No prod (`application.yml`) é `false` porque queremos que falhe se schema não bater.

5. **Docker no GitHub Actions:** `ubuntu-latest` tem Docker disponível por default — não precisa de `services:` ou setup adicional. Testcontainers detecta automaticamente.

6. **Tempo de primeira execução do CI:** pode subir de 27s para 80-120s na primeira vez por causa do pull do `postgres:16-alpine` no runner. Não é falha — é cache vazio.

7. **`.testcontainers.properties` em `~/`:** alguns devs configuram `testcontainers.reuse.enable=true` localmente para reutilizar container entre execuções de `mvnw test`. **Não fazer parte deste repo** — é decisão de máquina, e no CI não pode ser usado.

## Observações de ambiente

- Sistema: Windows nativo, PowerShell, Docker Desktop (rodando).
- Disponível: Java 21.0.11, Maven Wrapper 3.9.9, Docker 29.0.1, gh CLI autenticado.
- Branch protection ativa em `main` com required check `CI/build`.
- `decisoes.md` atualizado documentando versões, ambiente e política de débito técnico.
- Working tree no início: clean.
- Imagem `postgres:16-alpine` provavelmente já em cache local do Docker (do `docker-compose up -d` anterior).
