# Prompt — Etapa 1.4 da Camada 1

Você está trabalhando no projeto `financas-lab`. Estamos na **Etapa 1.4 da Camada 1** — inicializar projeto Spring Boot com todas as dependências da stack e configuração mínima.

## Antes de qualquer ação, leia em ordem:

1. `docs/visao.md` — propósito do projeto
2. `docs/decisoes.md` — stack, padrões e regras duras (especialmente seção "Stack > Backend")
3. `docs/adrs.md` — ADR-001 (Java 21 + Spring Boot 3 + Maven), ADR-003 (Postgres), ADR-004 (Clean Architecture), ADR-006 (Flyway), ADR-007 (Testes)
4. `docs/progresso.md` — estado atual
5. `docs/roadmap-camada-1.md` — Etapa 1.4 detalhada

Após ler, apresente um resumo de 5-7 linhas do que entendeu da tarefa antes de propor qualquer arquivo.

## Importante sobre esta etapa

Esta etapa **não pode ser feita por chamada direta a Spring Initializr via API ou download programático**. O motivo: Spring Initializr gera um zip que precisa ser baixado, extraído e mesclado no repo existente — o ambiente do Claude Code não é a melhor ferramenta pra isso.

Em vez disso, **você (Claude Code) vai criar todos os arquivos manualmente**, espelhando o que Spring Initializr produziria. Isso tem três vantagens:

1. Determinístico (não depende de versão do Initializr no momento da geração)
2. Auditável (cada arquivo proposto é revisado individualmente)
3. Educativo (o usuário e você veem cada peça do projeto Spring Boot ser construída)

## Tarefa

### Branch

Criar a branch `feat/spring-boot-init` a partir de `main` atualizada.

### Configuração do projeto Spring Boot

**Coordenadas Maven:**
- groupId: `com.laboratorio`
- artifactId: `financas`
- version: `0.0.1-SNAPSHOT`
- name: `financas`
- description: `SaaS de financas pessoais - laboratorio para fabrica AI-native`
- java.version: `21`
- spring-boot.version: usar **última versão estável 3.x atual** (verificar via `mvn help:evaluate` ou consulta web; **não chutar versão**)

### Estrutura de diretórios a criar

```
financas-lab/
├── .mvn/
│   └── wrapper/
│       ├── maven-wrapper.properties
│       └── (arquivos do wrapper)
├── mvnw
├── mvnw.cmd
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/laboratorio/financas/
│   │   │       └── FinancasApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-test.yml
│   │       └── db/
│   │           └── migration/
│   │               └── (vazio nesta etapa, será populado na 2.2)
│   └── test/
│       └── java/
│           └── com/laboratorio/financas/
│               └── FinancasApplicationTests.java
└── (arquivos existentes mantidos: docs/, README.md, CLAUDE.md, .gitignore, .gitattributes, docker-compose.yml, .env.example, LICENSE)
```

### Conteúdo dos arquivos

#### `pom.xml`

**Parent:** `spring-boot-starter-parent` na versão estável atual de Spring Boot 3.x.

**Dependências obrigatórias:**

| Dependência | Escopo |
|---|---|
| `spring-boot-starter-web` | runtime |
| `spring-boot-starter-data-jpa` | runtime |
| `spring-boot-starter-security` | runtime |
| `spring-boot-starter-validation` | runtime |
| `spring-boot-starter-actuator` | runtime |
| `org.postgresql:postgresql` | runtime |
| `org.flywaydb:flyway-core` | runtime |
| `org.flywaydb:flyway-database-postgresql` | runtime |
| `org.projectlombok:lombok` | provided / annotationProcessor |
| `org.mapstruct:mapstruct` (versão 1.6.x) | runtime |
| `org.mapstruct:mapstruct-processor` (versão 1.6.x) | annotationProcessor |
| `io.jsonwebtoken:jjwt-api` (versão 0.12.x) | runtime |
| `io.jsonwebtoken:jjwt-impl` (versão 0.12.x) | runtime |
| `io.jsonwebtoken:jjwt-jackson` (versão 0.12.x) | runtime |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` (versão 2.6.x) | runtime |
| `spring-boot-starter-test` | test |
| `spring-security-test` | test |
| `org.testcontainers:junit-jupiter` (versão atual) | test |
| `org.testcontainers:postgresql` (versão atual) | test |

**Plugins obrigatórios:**

1. `spring-boot-maven-plugin` — config padrão (com excludes do Lombok no `repackage`).
2. `maven-compiler-plugin` — Java 21, com `<annotationProcessorPaths>` declarando Lombok e MapStruct (nessa ordem; ordem importa).
3. `jacoco-maven-plugin` versão 0.8.x — duas execuções:
   - `prepare-agent`
   - `report` (gera HTML/XML em `target/site/jacoco/`)
   - **Ainda sem regras de cobertura por camada** — entram quando estrutura de pacotes existir (Camada 2). Por enquanto, só geração de relatório.

**Não incluir nesta etapa:**

- Checkstyle plugin (entra na 2.5)
- SpotBugs plugin (entra na 2.5)
- Plugins de profile-based execution

#### `FinancasApplication.java`

```java
package com.laboratorio.financas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FinancasApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancasApplication.class, args);
    }
}
```

#### `FinancasApplicationTests.java`

Apenas o teste de boot do contexto, **com profile de teste explícito**:

```java
package com.laboratorio.financas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FinancasApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

#### `application.yml` (defaults compartilhados)

```yaml
spring:
  application:
    name: financas
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when_authorized

springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
```

**Pontos não-negociáveis:**

- `spring.jpa.hibernate.ddl-auto: validate` (nunca `update`/`create`/`create-drop`)
- `spring.jpa.open-in-view: false` (regra dura — Open Session In View é antipattern)
- `time_zone: UTC` (datas em UTC, conversão na borda)

#### `application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/${POSTGRES_DB:financas_dev}
    username: ${POSTGRES_USER:financas}
    password: ${POSTGRES_PASSWORD:changeme_local_only}
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:changeme_local_only}

logging:
  level:
    com.laboratorio.financas: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
```

#### `application-test.yml`

Vazio por enquanto, com placeholder:

```yaml
# Configurações específicas de teste serão adicionadas na Etapa 2.1
# (configuração do Testcontainers).
# Por enquanto, mantemos arquivo vazio para o profile existir.
```

### Maven Wrapper

Use `mvn -N wrapper:wrapper` para gerar `mvnw`, `mvnw.cmd` e o diretório `.mvn/wrapper/`. Use a versão atual do Maven 3.9.x.

### Validações obrigatórias antes de commitar

Execute, na ordem:

1. `.\mvnw clean install -DskipTests` — deve completar com BUILD SUCCESS sem download lentíssimo (cache local provavelmente já tem boa parte). Se primeira execução, pode demorar 2-5 min.
2. `docker compose up -d` — sobe Postgres + Redis.
3. Aguardar healthcheck (polling com `until` ou comando equivalente, **não usar `sleep N && cmd` em chain**).
4. `.\mvnw spring-boot:run -Dspring-boot.run.profiles=dev` em background ou em terminal separado — sobe a aplicação. Se rodar em background, capturar PID pra matar depois.
5. Aguardar app subir. Validar com `curl -s http://localhost:8080/actuator/health` — deve retornar `{"status":"UP"}` ou similar.
6. Validar `curl -s http://localhost:8080/v3/api-docs` — deve retornar JSON OpenAPI (mesmo que vazio de endpoints).
7. Matar app Spring Boot.
8. `docker compose down` — limpa containers.
9. `.\mvnw test` — roda `FinancasApplicationTests.contextLoads()`. **Nota:** vai falhar provavelmente, porque o teste tenta subir contexto Spring que precisa de banco e o Postgres não está rodando. **Isso é esperado nesta etapa** — o teste só vai funcionar quando Testcontainers for configurado na Etapa 2.1. Reportar a falha como esperada e seguir.

   *Alternativa, se preferir teste passando agora:* configurar `application-test.yml` com `spring.autoconfigure.exclude` desabilitando datasource auto-config. Mas isso vai precisar ser desfeito na 2.1. Recomendo aceitar a falha esperada e documentar.

10. `git status` — deve mostrar apenas arquivos novos esperados.

Se qualquer validação **diferente da esperada** falhar, pare, reporte e não tente consertar sem instrução.

### Commit e PR

Após validações:

1. Mostre `git status` e peça confirmação.
2. Faça commits atômicos:
   - Commit 1: `chore: adiciona maven wrapper` (apenas `mvnw`, `mvnw.cmd`, `.mvn/`)
   - Commit 2: `feat: inicializa projeto spring boot 3 com java 21 e dependencias da stack` (`pom.xml`, código Java, application*.yml)
3. Push.
4. Abrir PR via `gh` CLI:
   - Title: `feat: inicializa projeto spring boot 3 com dependencias da stack`
   - Body com 4 seções:
     - **Summary** — bullets do que foi adicionado
     - **Stack contemplada** — lista das dependências principais com versão
     - **Validações executadas** — lista dos 10 checks com status
     - **Notas para revisão** — alertar sobre o teste `contextLoads` falhar até Etapa 2.1 ser concluída

## Restrições importantes

- **Não criar pacote `domain/`, `application/`, `infrastructure/`, `interfaces/`** — estrutura Clean Architecture entra na Camada 2. Apenas o pacote raiz `com.laboratorio.financas` com a classe `FinancasApplication` nesta etapa.
- **Não criar nenhum endpoint REST.** O Hello World vem na Etapa 2.3.
- **Não criar Spring Security config customizada.** A dependência está no `pom.xml`, mas configuração detalhada vem na Camada 2.
- **Não criar nenhuma `@Entity`, `@Repository`, `@Service`.** Camada 2.
- **Não criar Flyway migrations.** Etapa 2.2.
- **Não criar Testcontainers config.** Etapa 2.1.
- **Não adicionar Checkstyle/SpotBugs.** Etapa 2.5.
- **Não usar `ddl-auto: update` ou `create`.** Em nenhum profile, em nenhum momento.
- **Não adicionar dependências fora da lista** acima sem perguntar.
- **Não modificar `docker-compose.yml`, `.env.example`, `docs/`, `README.md`, `CLAUDE.md`** nesta etapa.
- **Pergunte antes de cada commit.**
- **Não force push.**

## Pegadinhas conhecidas

1. **Ordem de annotationProcessor importa**: Lombok antes de MapStruct. Inverter quebra build com erro confuso.
2. **MapStruct precisa de `<componentModel>spring</componentModel>` configurado via `<compilerArgs>`** no `maven-compiler-plugin` para gerar mappers como `@Component`. Sem isso, mappers gerados não são injetáveis.
3. **JJWT 0.12.x** mudou API significativamente da 0.11.x. Não usar exemplos antigos.
4. **springdoc-openapi 2.x** é para Spring Boot 3.x. Versão 1.x era para Spring Boot 2.x. Não confundir.
5. **Flyway 10+** exige dependência separada `flyway-database-postgresql` além do `flyway-core`. Sem ela, erro de "no module found" em runtime.
6. **`open-in-view: false`** vai gerar warning no startup avisando que Open Session In View está desabilitado. **Isso é esperado e correto.** Ignorar o warning.

## Observações de ambiente

- Sistema: Windows nativo, PowerShell, Docker Desktop.
- Disponível: Java 21.0.11, Maven 3.9.15, Docker 29.0.1, gh CLI autenticado.
- Branch protection ativa em `main` — push direto bloqueado.
- Working tree no início: clean (com `.claude/` untracked).
- `.env` local existe com placeholders adequados para dev.
