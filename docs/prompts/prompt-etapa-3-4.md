# Prompt — Etapa 3.4: Bounded Context `conta` — Application + Interfaces

## Contexto

A Etapa 3.3.1 foi concluída e fechada via PR #32 (fix do `dev.ps1` para ativar profile `dev`). Validação destrutiva pós-merge confirmou que aplicação sobe corretamente, profile `dev` ativo, V1 e V2 aplicadas, tabela `conta` no schema correto.

Esta etapa **fecha o bounded context `conta` ponta a ponta**: implementa os use cases (`application/`) e os endpoints REST (`interfaces/`). Quando concluída, será possível criar, listar, buscar e desativar contas via HTTP contra o Postgres real.

É a etapa que **estabelece o template** que `categoria`, `transacao` e demais bounded contexts vão seguir nas próximas etapas. Tudo que aparece aqui pela primeira vez vira precedente reutilizável.

## Padrões que estreiam nesta etapa

1. Primeiro use case (`CriarContaUseCase` e demais) — padrão "1 classe = 1 caso de uso"
2. Primeiro DTO de API (`*Request`/`*Response`) e Bean Validation neles
3. Primeiro `@RestController` de domínio (Healthcheck era técnico)
4. Primeiro `@RestControllerAdvice` global com `ProblemDetail` (RFC 7807)
5. Primeiro `@Transactional` aplicado (no use case, não no repository)
6. Primeiro teste e2e via `MockMvc` exercitando ciclo completo
7. Primeira whitelist temporária de Security com TODO explícito de remoção
8. Ativação dos thresholds JaCoCo `application/` 80% e `interfaces/` 70%

## Escopo decidido (calibrado com operador antes da redação)

### REST endpoints

```
POST   /api/contas              → 201 Created, retorna ContaResponse
GET    /api/contas              → 200 OK, lista. Query param ?ativa=true filtra; sem param retorna todas.
GET    /api/contas/{id}         → 200 OK ou 404 Not Found
DELETE /api/contas/{id}         → 204 No Content (soft delete via Conta.desativar())
```

- **DELETE faz soft delete por trás.** Cliente não precisa saber. Chama `DesativarContaUseCase` que usa `Conta.desativar()` e salva.
- **GET com query param `?ativa=true`** filtra ativas; sem param retorna todas. Padrão simples, sem outros filtros nesta etapa.
- **Sem paginação.** MVP single-user com poucas contas. Paginação real entra em `transacao`.

### Use cases

Quatro use cases, cada um em arquivo próprio em `conta/application/`:

1. **`CriarContaUseCase`** — recebe `CriarContaCommand` (record interno: `nome`, `tipo`, `saldoInicialValor`, `saldoInicialMoeda`), constrói `Money`, constrói `Conta` (gera id, ativa, timestamps), chama `repository.salvar`. Retorna `Conta`.
2. **`ListarContasUseCase`** — método `executar(boolean apenasAtivas)`. Se `apenasAtivas` é true → `repository.listarAtivas()`. Senão → `repository.listarTodas()`. Retorna `List<Conta>`.
3. **`BuscarContaPorIdUseCase`** — recebe `UUID`, retorna `Conta`. **Lança `ContaNaoEncontradaException`** quando `Optional.empty()`. Esta é a primeira exceção customizada de domínio do projeto.
4. **`DesativarContaUseCase`** — recebe `UUID`, busca via `BuscarContaPorIdUseCase` (ou direto no repository — decisão abaixo), chama `desativar()`, salva. Retorna `void`.

**Decisão sobre composição de use cases:**

`DesativarContaUseCase` precisa buscar antes de desativar. Duas opções:
- **A)** Recebe `BuscarContaPorIdUseCase` no construtor e delega busca a ele
- **B)** Recebe `ContaRepository` direto e chama `buscarPorId`, lançando `ContaNaoEncontradaException` ele mesmo

**Decisão: B**. Razão: composição de use cases gera dependência transitiva (`Desativar` depende de `Buscar`, que pode evoluir e quebrar `Desativar`). Use cases independentes acoplam só ao repository, que é interface estável. A duplicação de "busca + lança se vazio" é mínima (3 linhas) e evita acoplamento.

**Onde mora `ContaNaoEncontradaException`:**

- Localização: `src/main/java/com/laboratorio/financas/conta/domain/ContaNaoEncontradaException.java`
- Estende `RuntimeException` (não checked).
- Construtor com `UUID id`: `super("Conta nao encontrada: " + id);`
- Pertence ao domínio porque expressa regra de domínio ("conta inexistente"), não detalhe de infra. Use case lança, controller traduz pra 404.

### DTOs

Em `conta/interfaces/dto/`:

1. **`CriarContaRequest`** (record com Bean Validation):
   - `String nome` — `@NotBlank`, `@Size(max = 100)`
   - `TipoConta tipo` — `@NotNull`
   - `BigDecimal saldoInicialValor` — `@NotNull`
   - `String saldoInicialMoeda` — `@NotNull`, `@Size(min = 3, max = 3)` (código ISO)

2. **`ContaResponse`** (record sem validações, é só saída):
   - `UUID id`, `String nome`, `TipoConta tipo`
   - `BigDecimal saldoInicialValor`, `String saldoInicialMoeda`
   - `boolean ativa`, `Instant criadoEm`, `Instant atualizadoEm`

**Conversão Conta ↔ DTOs:**

Não usar MapStruct para os DTOs. Razão: Bean Validation precisa de constructor explícito ou setters; tradução é trivial (campo a campo); MapStruct entre `Conta` (com `Money`) e `ContaResponse` (com `valor` e `moeda` separados) exigiria método `default` com construtor, mesmo padrão de `ContaMapper`. Mais simples ter método estático `ContaResponse.fromDomain(Conta c)` (factory na própria classe response) ou método no controller. **Decisão: método estático `fromDomain` no próprio `ContaResponse`.**

### Exception handler global

`src/main/java/com/laboratorio/financas/shared/infrastructure/web/GlobalExceptionHandler.java`

Anotação `@RestControllerAdvice`. Captura:

- **`MethodArgumentNotValidException`** (Bean Validation falhou em `@Valid`) → 400 Bad Request com `ProblemDetail` listando campos inválidos.
- **`HttpMessageNotReadableException`** (JSON malformado, enum inválido) → 400 Bad Request com mensagem genérica.
- **`ContaNaoEncontradaException`** → 404 Not Found com `id` no detail.
- **`IllegalArgumentException`** (validações de domínio: nome blank no `Conta`, moedas diferentes em `Money`, etc) → 400 Bad Request.
- **`Exception`** (catch-all) → 500 Internal Server Error com mensagem genérica. **Logar stack trace via SLF4J**, retornar `ProblemDetail` sem expor detalhes internos ao cliente.

**`ProblemDetail`** é classe nativa do Spring 6 (`org.springframework.http.ProblemDetail`). Estrutura RFC 7807:
- `type`: URI (pode ser `about:blank` no MVP)
- `title`: descrição curta ("Bad Request", "Not Found")
- `status`: int do código HTTP
- `detail`: descrição específica do erro
- `instance`: URI da requisição (Spring preenche automaticamente em alguns casos; nesta etapa deixar default)

**Não criar reservar para 409 ainda** (sem regra de negócio que justifique nesta etapa). Estrutura do handler permite adicionar depois.

### Transactional

- **`@Transactional` em métodos de use case que escrevem** (`CriarContaUseCase.executar`, `DesativarContaUseCase.executar`).
- **`@Transactional(readOnly = true)` em use cases que só leem** (`ListarContasUseCase.executar`, `BuscarContaPorIdUseCase.executar`).
- **Não em método do controller.** Transação é responsabilidade do use case.
- **Não em método do repository.** Já era responsabilidade do use case por decisão da 3.3 — mantém.

### Security: whitelist temporária

`SecurityConfig` em `shared/infrastructure/web/SecurityConfig.java` (já existe desde 2.3) precisa ganhar entrada:

```java
.requestMatchers("/api/contas/**").permitAll()  // TODO Etapa Auth (futura): remover whitelist quando JWT estiver pronto
```

Posicionar na lista junto com as outras (`/api/healthcheck`, `/actuator/health`, etc).

**Comentário TODO obrigatório** no código indicando que é débito explícito de Auth. Sem comentário, vira "esquecimento" no futuro.

### Localização dos arquivos

```
src/main/java/com/laboratorio/financas/conta/
├── application/
│   ├── CriarContaUseCase.java
│   ├── ListarContasUseCase.java
│   ├── BuscarContaPorIdUseCase.java
│   └── DesativarContaUseCase.java
├── domain/
│   └── ContaNaoEncontradaException.java   ← novo
└── interfaces/
    ├── ContaController.java
    └── dto/
        ├── CriarContaRequest.java
        └── ContaResponse.java

src/main/java/com/laboratorio/financas/shared/infrastructure/web/
└── GlobalExceptionHandler.java   ← novo

src/test/java/com/laboratorio/financas/conta/
├── application/
│   ├── CriarContaUseCaseTest.java
│   ├── ListarContasUseCaseTest.java
│   ├── BuscarContaPorIdUseCaseTest.java
│   └── DesativarContaUseCaseTest.java
└── interfaces/
    └── ContaControllerTest.java   ← teste e2e via MockMvc + Testcontainers
```

### Testes — três níveis

**Use case (testes unitários, sem Spring):**
- Cada use case com mock de `ContaRepository` (Mockito).
- Mockito é justificável aqui (regra `decisoes.md`: "Mockito uso comedido — só quando dependência é externa real ou interface estável" — repository é interface estável).
- Cobertura por use case: ~5-8 testes cobrindo caminho feliz + variações + falhas.

**Controller (teste e2e completo):**
- `ContaControllerTest extends AbstractIntegrationTest` — sobe Spring + Postgres real via Testcontainers.
- `@AutoConfigureMockMvc` para simular requisições HTTP sem subir Tomcat real.
- **Não usar `@MockBean` para repository.** Stack inteira real, dados persistem no Postgres do container, `@AfterEach` limpa.
- Testes obrigatórios:
  1. `POST /api/contas` cria conta válida → 201, Location header (opcional), ContaResponse no body
  2. `POST /api/contas` com nome blank → 400 com ProblemDetail listando campo `nome`
  3. `POST /api/contas` com nome > 100 chars → 400
  4. `POST /api/contas` com tipo null → 400
  5. `POST /api/contas` com saldoInicialMoeda inválida (4 chars) → 400
  6. `POST /api/contas` com tipo enum desconhecido (ex: `"XYZ"`) → 400
  7. `GET /api/contas` retorna todas as contas (ativas + inativas)
  8. `GET /api/contas?ativa=true` retorna só ativas
  9. `GET /api/contas/{id}` retorna conta existente → 200
  10. `GET /api/contas/{id}` para id inexistente → 404 com ProblemDetail
  11. `GET /api/contas/{id}` com UUID malformado → 400
  12. `DELETE /api/contas/{id}` desativa conta existente → 204, GET subsequente mostra `ativa=false`
  13. `DELETE /api/contas/{id}` para id inexistente → 404
  14. Ciclo completo: POST → GET por id → DELETE → GET por id (ativa=false) → GET ?ativa=true (não retorna)

**Total esperado para a etapa: ~50 testes** (4 use cases × ~6 cada + ~14 controller + ContaResponse fromDomain + outros casos).

### JaCoCo

- Ativar threshold de `application/` 80% (descomentar regra que está aguardando primeira classe desde Etapa 2.4).
- Ativar threshold de `interfaces/` 70% (idem).
- Threshold de `domain/` 90% continua atendido (ContaNaoEncontradaException é trivial).
- Threshold de `infrastructure/` 60% continua atendido.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra commit do squash da Etapa 3.3.1 com referência a PR #32
- `docs/prompt-etapa-3-4.md` presente como untracked (este próprio arquivo)
- Working tree limpo
- Pacotes `conta/application/` e `conta/interfaces/` **não existem ainda**
- `shared/infrastructure/web/SecurityConfig.java` existe desde Etapa 2.3
- `shared/infrastructure/web/GlobalExceptionHandler.java` **não existe ainda**

Validar com:

```bash
git status
git log --oneline -1
ls docs/prompt-etapa-3-4.md
ls -la src/main/java/com/laboratorio/financas/conta/
ls src/main/java/com/laboratorio/financas/conta/application/ 2>/dev/null && echo "ATENCAO" || echo "OK: nao existe"
ls src/main/java/com/laboratorio/financas/conta/interfaces/ 2>/dev/null && echo "ATENCAO" || echo "OK: nao existe"
ls src/main/java/com/laboratorio/financas/shared/infrastructure/web/SecurityConfig.java
```

Se algum item divergir, parar e reportar.

## Tarefas

### Tarefa 1 — Validar pré-requisitos

```bash
git status
git log --oneline -3
ls docs/prompt-etapa-3-4.md
ls -la src/main/java/com/laboratorio/financas/conta/
ls src/main/java/com/laboratorio/financas/conta/application/ 2>/dev/null && echo "ATENCAO" || echo "OK"
ls src/main/java/com/laboratorio/financas/conta/interfaces/ 2>/dev/null && echo "ATENCAO" || echo "OK"
ls src/main/java/com/laboratorio/financas/shared/infrastructure/web/SecurityConfig.java
```

### Tarefa 2 — Criar branch de trabalho

```bash
git checkout -b feat/conta-application-interfaces
```

### Tarefa 3 — Criar `ContaNaoEncontradaException`

Arquivo: `src/main/java/com/laboratorio/financas/conta/domain/ContaNaoEncontradaException.java`

```java
package com.laboratorio.financas.conta.domain;

import java.util.UUID;

public class ContaNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public ContaNaoEncontradaException(UUID id) {
        super("Conta nao encontrada: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
```

**Notas:**
- Estende `RuntimeException` (não checked).
- Mantém o `id` acessível para o handler global colocar no `ProblemDetail`.
- Pertence ao domínio.
- Sem acentos.

### Tarefa 4 — Criar use cases em `conta/application/`

**4a.** `CriarContaUseCase.java`

```java
package com.laboratorio.financas.conta.application;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Currency;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarContaUseCase {

    private final ContaRepository repository;

    public CriarContaUseCase(ContaRepository repository) {
        this.repository = repository;
    }

    public record Comando(
            String nome,
            TipoConta tipo,
            BigDecimal saldoInicialValor,
            String saldoInicialMoeda
    ) { }

    @Transactional
    public Conta executar(Comando comando) {
        Currency moeda = Currency.getInstance(comando.saldoInicialMoeda());
        Money saldoInicial = new Money(comando.saldoInicialValor(), moeda);
        Conta nova = new Conta(comando.nome(), comando.tipo(), saldoInicial);
        return repository.salvar(nova);
    }
}
```

**Notas:**
- `Comando` como record interno do use case. Padrão consolidado: comando é específico do caso de uso, não DTO público.
- `Currency.getInstance` lança `IllegalArgumentException` se código for inválido. Bean Validation já filtra na borda da API (`@Size(min=3, max=3)`), mas validação adicional aqui é defesa em profundidade. Handler global captura.
- `@Transactional` no método de execução. Spring abre tx, commit/rollback automático.
- `@Component` (não `@Service` — projeto não usa estereótipo `@Service` por convenção; pode ajustar se preferência for outra).

**4b.** `ListarContasUseCase.java`

```java
package com.laboratorio.financas.conta.application;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarContasUseCase {

    private final ContaRepository repository;

    public ListarContasUseCase(ContaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Conta> executar(boolean apenasAtivas) {
        if (apenasAtivas) {
            return repository.listarAtivas();
        }
        return repository.listarTodas();
    }
}
```

**4c.** `BuscarContaPorIdUseCase.java`

```java
package com.laboratorio.financas.conta.application;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaNaoEncontradaException;
import com.laboratorio.financas.conta.domain.ContaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuscarContaPorIdUseCase {

    private final ContaRepository repository;

    public BuscarContaPorIdUseCase(ContaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Conta executar(UUID id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new ContaNaoEncontradaException(id));
    }
}
```

**4d.** `DesativarContaUseCase.java`

```java
package com.laboratorio.financas.conta.application;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaNaoEncontradaException;
import com.laboratorio.financas.conta.domain.ContaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DesativarContaUseCase {

    private final ContaRepository repository;

    public DesativarContaUseCase(ContaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id) {
        Conta conta = repository.buscarPorId(id)
                .orElseThrow(() -> new ContaNaoEncontradaException(id));
        Conta desativada = conta.desativar();
        repository.salvar(desativada);
    }
}
```

**Nota sobre composição:** decisão registrada — use cases não dependem entre si. `DesativarContaUseCase` busca direto no repository (3 linhas duplicadas em relação a `BuscarContaPorIdUseCase`) e lança `ContaNaoEncontradaException` ele mesmo.

### Tarefa 5 — Criar DTOs em `conta/interfaces/dto/`

**5a.** `CriarContaRequest.java`

```java
package com.laboratorio.financas.conta.interfaces.dto;

import com.laboratorio.financas.conta.domain.TipoConta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CriarContaRequest(
        @NotBlank
        @Size(max = 100)
        String nome,

        @NotNull
        TipoConta tipo,

        @NotNull
        BigDecimal saldoInicialValor,

        @NotNull
        @Size(min = 3, max = 3)
        String saldoInicialMoeda
) { }
```

**5b.** `ContaResponse.java`

```java
package com.laboratorio.financas.conta.interfaces.dto;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.TipoConta;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ContaResponse(
        UUID id,
        String nome,
        TipoConta tipo,
        BigDecimal saldoInicialValor,
        String saldoInicialMoeda,
        boolean ativa,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static ContaResponse fromDomain(Conta conta) {
        return new ContaResponse(
                conta.getId(),
                conta.getNome(),
                conta.getTipo(),
                conta.getSaldoInicial().valor(),
                conta.getSaldoInicial().moeda().getCurrencyCode(),
                conta.isAtiva(),
                conta.getCriadoEm(),
                conta.getAtualizadoEm()
        );
    }
}
```

### Tarefa 6 — Criar `ContaController.java`

Arquivo: `src/main/java/com/laboratorio/financas/conta/interfaces/ContaController.java`

```java
package com.laboratorio.financas.conta.interfaces;

import com.laboratorio.financas.conta.application.BuscarContaPorIdUseCase;
import com.laboratorio.financas.conta.application.CriarContaUseCase;
import com.laboratorio.financas.conta.application.DesativarContaUseCase;
import com.laboratorio.financas.conta.application.ListarContasUseCase;
import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.interfaces.dto.ContaResponse;
import com.laboratorio.financas.conta.interfaces.dto.CriarContaRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contas")
public class ContaController {

    private final CriarContaUseCase criarContaUseCase;
    private final ListarContasUseCase listarContasUseCase;
    private final BuscarContaPorIdUseCase buscarContaPorIdUseCase;
    private final DesativarContaUseCase desativarContaUseCase;

    public ContaController(
            CriarContaUseCase criarContaUseCase,
            ListarContasUseCase listarContasUseCase,
            BuscarContaPorIdUseCase buscarContaPorIdUseCase,
            DesativarContaUseCase desativarContaUseCase
    ) {
        this.criarContaUseCase = criarContaUseCase;
        this.listarContasUseCase = listarContasUseCase;
        this.buscarContaPorIdUseCase = buscarContaPorIdUseCase;
        this.desativarContaUseCase = desativarContaUseCase;
    }

    @PostMapping
    public ResponseEntity<ContaResponse> criar(@Valid @RequestBody CriarContaRequest request) {
        CriarContaUseCase.Comando comando = new CriarContaUseCase.Comando(
                request.nome(),
                request.tipo(),
                request.saldoInicialValor(),
                request.saldoInicialMoeda()
        );
        Conta criada = criarContaUseCase.executar(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(ContaResponse.fromDomain(criada));
    }

    @GetMapping
    public List<ContaResponse> listar(@RequestParam(name = "ativa", required = false) Boolean ativa) {
        boolean apenasAtivas = Boolean.TRUE.equals(ativa);
        List<Conta> contas = listarContasUseCase.executar(apenasAtivas);
        return contas.stream().map(ContaResponse::fromDomain).toList();
    }

    @GetMapping("/{id}")
    public ContaResponse buscar(@PathVariable UUID id) {
        Conta conta = buscarContaPorIdUseCase.executar(id);
        return ContaResponse.fromDomain(conta);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable UUID id) {
        desativarContaUseCase.executar(id);
    }
}
```

**Atenção:** o esboço acima usa `@ResponseStatus` no método `desativar`. Adicionar import: `import org.springframework.web.bind.annotation.ResponseStatus;`

**Notas:**
- Construtor com 4 dependências. Spring resolve.
- `@Valid` no `@RequestBody` ativa Bean Validation no `CriarContaRequest`.
- `@PathVariable UUID id` — Spring converte automaticamente. UUID malformado dispara `MethodArgumentTypeMismatchException`, capturada pelo handler global como 400.
- `?ativa=true` é tratado como `Boolean` (não `boolean`) pra permitir ausência. `Boolean.TRUE.equals(ativa)` evita NPE.

### Tarefa 7 — Criar `GlobalExceptionHandler.java`

Arquivo: `src/main/java/com/laboratorio/financas/shared/infrastructure/web/GlobalExceptionHandler.java`

```java
package com.laboratorio.financas.shared.infrastructure.web;

import com.laboratorio.financas.conta.domain.ContaNaoEncontradaException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail("Validacao falhou em um ou mais campos.");
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                erros.put(fieldError.getField(), fieldError.getDefaultMessage())
        );
        problem.setProperty("erros", erros);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleJsonMalformado(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail("Corpo da requisicao invalido ou malformado.");
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail("Parametro '" + ex.getName() + "' tem formato invalido.");
        return problem;
    }

    @ExceptionHandler(ContaNaoEncontradaException.class)
    public ProblemDetail handleContaNaoEncontrada(ContaNaoEncontradaException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("id", ex.getId().toString());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleArgumentoInvalido(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenerico(Exception ex) {
        log.error("Erro nao tratado", ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal Server Error");
        problem.setDetail("Ocorreu um erro interno. Tente novamente mais tarde.");
        return problem;
    }
}
```

**Notas:**
- `@ExceptionHandler(Exception.class)` é o catch-all. Loga stack trace internamente, retorna mensagem genérica ao cliente (não vaza detalhes).
- Spring 6 reconhece `ProblemDetail` como retorno e serializa como `application/problem+json`.
- `setProperty` adiciona campos extras no body além dos padrão RFC 7807.
- Sem acentos.

### Tarefa 8 — Atualizar `SecurityConfig.java`

Localizar a configuração da `SecurityFilterChain` em `src/main/java/com/laboratorio/financas/shared/infrastructure/web/SecurityConfig.java`.

Na lista de `requestMatchers(...).permitAll()`, **adicionar** `/api/contas/**`:

```java
.requestMatchers(
        "/api/healthcheck",
        "/api/contas/**",  // TODO Etapa Auth (futura): remover whitelist quando JWT estiver pronto
        "/actuator/health",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
).permitAll()
```

**Importante:**
- A lista exata pode variar — manter ordem alfabética interna ou mantê-la como está. Apenas adicionar o item.
- O comentário `// TODO Etapa Auth (futura)` é obrigatório. Sem ele, vira esquecimento.
- Não relaxar mais nada além desta entrada.

### Tarefa 9 — Atualizar `pom.xml` para ativar thresholds JaCoCo

Localizar o bloco do `jacoco-maven-plugin` na execução `check`. As regras de `application/` 80% e `interfaces/` 70% estão comentadas desde a Etapa 2.4.

**Descomentar ambas.** Não tocar nas outras (`domain`, `infrastructure`, BUNDLE).

Antes de descomentar, **ler o bloco comentado** e descomentar exatamente o que está lá. Se a sintaxe divergir do esperado (ex: `<include>**.application.*</include>`), parar e reportar.

### Tarefa 10 — Criar testes de use case

Quatro arquivos em `src/test/java/com/laboratorio/financas/conta/application/`.

**Estrutura padrão:**

```java
package com.laboratorio.financas.conta.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.shared.domain.Money;
// ... demais imports
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
```

Não usar `@ExtendWith(MockitoExtension.class)` — preferir Mockito programático com `Mockito.mock(ContaRepository.class)` no `@BeforeEach`. Mais explícito, menos magia.

**Cobertura mínima por use case:**

- **`CriarContaUseCaseTest`** (~6 testes): caminho feliz, valor zero, valor negativo, moeda inválida (lança `IllegalArgumentException`), `repository.salvar` é chamado uma vez, retorna o que repository retornou.
- **`ListarContasUseCaseTest`** (~4 testes): apenasAtivas=true delega a `listarAtivas`, apenasAtivas=false delega a `listarTodas`, retorna lista vazia, retorna lista com múltiplas contas.
- **`BuscarContaPorIdUseCaseTest`** (~3 testes): retorna conta quando existe, lança `ContaNaoEncontradaException` com id correto quando não existe, repository é chamado uma vez.
- **`DesativarContaUseCaseTest`** (~5 testes): caminho feliz (busca + desativa + salva), lança `ContaNaoEncontradaException` quando id não existe, conta já inativa retorna mesma instância (não chama salvar duas vezes? — verificar comportamento), repository.salvar é chamado uma vez no caminho feliz.

**Sobre `DesativarContaUseCaseTest`:** o use case sempre chama `repository.salvar(desativada)`, mesmo se a conta já estava inativa (porque `Conta.desativar()` retorna a mesma instância nesse caso, e não há optimização). Aceitar esse comportamento — `salvar` é idempotente em JPA via `merge`. Se quiser otimizar, é decisão futura.

**Naming dos testes em camelCase puro, sem underscore.** Padrão consolidado.

**Indentação Checkstyle:** continuações em 16 espaços, getters em bloco, chaves obrigatórias. Esboços já alinhados.

### Tarefa 11 — Criar `ContaControllerTest.java`

Arquivo: `src/test/java/com/laboratorio/financas/conta/interfaces/ContaControllerTest.java`

```java
package com.laboratorio.financas.conta.interfaces;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaJpaRepository;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ContaControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContaJpaRepository jpaRepository;

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    // ... testes
}
```

**Notas críticas:**
- `@AutoConfigureMockMvc` em conjunto com `@SpringBootTest` (herdado de `AbstractIntegrationTest`) sobe contexto completo.
- `ObjectMapper` injetado para serializar request bodies.
- `@AfterEach` com `deleteAll()` garante isolamento entre testes.
- Body do POST deve ser construído como `Map<String, Object>` ou record dedicado serializado, não string crua (evita escape).

**14 testes obrigatórios** conforme listados na seção "Testes — três níveis" acima. Implementar exatamente esses cenários.

### Tarefa 12 — Validar localmente

```bash
.\mvnw.cmd compile

# Verificar que não há regressão de Checkstyle / SpotBugs:
.\mvnw.cmd verify
```

**Esperado:**
- BUILD SUCCESS
- ~50 novos testes passando + os existentes (~134 total)
- Checkstyle: 0 violações
- SpotBugs: 0 issues
- JaCoCo: todos os thresholds atendidos (BUNDLE 75%, domain 90%, infrastructure 60%, application 80%, interfaces 70%)

**Possíveis pontos de atrito (parar e reportar se acontecer):**

1. **`@ResponseStatus` import esquecido no `ContaController`** — adicionar `import org.springframework.web.bind.annotation.ResponseStatus;`
2. **`Currency.getInstance(...)` lança `IllegalArgumentException` para moeda inválida** — confirmar que handler global captura corretamente como 400.
3. **Bean Validation precisa que Spring Boot Validation esteja no classpath** — geralmente vem com `spring-boot-starter-web`. Se não estiver, parar e reportar.
4. **`UUID.fromString` em `@PathVariable` lança `MethodArgumentTypeMismatchException`** — handler global tem o tratamento correto.
5. **JaCoCo `application` 80% pode ficar abaixo se algum branch de validação não for coberto** — adicionar teste, não relaxar threshold.
6. **`MockMvc` precisa de `@AutoConfigureMockMvc`** — anotação obrigatória junto com `@SpringBootTest`.
7. **`AbstractIntegrationTest` já é `@SpringBootTest`** — não anotar de novo no `ContaControllerTest`.

### Tarefa 13 — Atualizar `docs/decisoes.md`

**13a.** Localizar seção "Padrões aplicados". Adicionar notas sobre padrões que estreiam:

```markdown
**Use case = classe** (a partir da Etapa 3.4): 1 classe = 1 caso de uso. Construtor explícito recebe dependências (repositório, outros use cases quando necessário). Método público `executar(...)` é o entry point único. `@Transactional` aplicado no método; `@Transactional(readOnly = true)` em casos puramente de leitura. Comando como record interno do use case quando há mais de 2 parâmetros.

**Composição de use cases**: use cases não dependem entre si. Cada use case acopla apenas ao `Repository`. Duplicação local de lógica de busca + lança exceção é aceita em troca de evitar dependência transitiva.

**Tratamento de exceções via `@RestControllerAdvice`** com `ProblemDetail` (RFC 7807): handler global em `shared/infrastructure/web/GlobalExceptionHandler.java`. Mapeia `MethodArgumentNotValidException` → 400 com lista de campos, `ContaNaoEncontradaException` → 404, `IllegalArgumentException` → 400, `Exception` (catch-all) → 500 com log de stack trace e mensagem genérica.

**Conversão DTO ↔ Domain via método estático `fromDomain` no DTO de resposta**, não MapStruct. Tradução é trivial e MapStruct exigiria método `default` de qualquer forma. Mapper continua usado para Entity ↔ Domain (3.3).
```

**13b.** Atualizar seção "Cobertura mínima por camada (JaCoCo)" → "Status atual de aplicação dos thresholds":
- Mover `application` 80% para ✅ Ativos
- Mover `interfaces` 70% para ✅ Ativos
- Atualizar etapa de referência: "(Etapas 2.4, 3.1, 3.4)"

**13c.** Adicionar entrada no histórico:

```markdown
- **2026-05-09** — Etapa 3.4 concluída: bounded context `conta` finalizado ponta a ponta. 4 use cases, 2 DTOs (`CriarContaRequest`/`ContaResponse`), `ContaController` com 4 endpoints (`POST/GET/GET/DELETE /api/contas`), `GlobalExceptionHandler` com `ProblemDetail` (RFC 7807), whitelist temporária de `/api/contas/**` em `SecurityConfig` (TODO Auth). Thresholds JaCoCo `application` 80% e `interfaces` 70% ativados. Mergeado via PR #XX.
```

### Tarefa 14 — Atualizar `docs/progresso.md`

**14a.** Atualizar "Última atualização": `2026-05-09 (Etapa 3.4 — Conta application+interfaces)`.

**14b.** Na Camada 2:
- Marcar `[x] Bounded context conta com domínio puro + use cases + repositório` (totalmente atendido agora — 3.2+3.3+3.4)
- Marcar `[x] Bean Validation aplicada em DTOs de Request`

**14c.** Adicionar nova seção **"Lições da Etapa 3.4"** logo antes de **"Lições da Etapa 3.3.1"** (ordem decrescente):

```markdown
## Lições da Etapa 3.4

### Candidatos a hook (automatizar em etapas futuras)

(Preencher com lições reais — ou `(Nenhum novo nesta etapa.)` se nada surgir.)

### Lições de ambiente

(Preencher com lições reais — ou `(Nenhum novo nesta etapa.)` se nada surgir.)
```

**14d.** Adicionar entrada no histórico:

```markdown
- **2026-05-09** — Etapa 3.4 concluída: `conta` ponta a ponta. 4 use cases, controller, handler global, ~50 testes (use cases + e2e via MockMvc). Thresholds JaCoCo de `application` e `interfaces` ativados. Mergeado via PR #XX.
```

### Tarefa 15 — Versionar este próprio prompt

Confirmar `docs/prompt-etapa-3-4.md` em disco e incluir no commit de docs.

### Tarefa 16 — Validação final antes de commitar

```bash
# Encoding sem BOM em todos os arquivos novos:
for f in $(find src/main/java/com/laboratorio/financas/conta/application -name "*.java"; \
           find src/main/java/com/laboratorio/financas/conta/interfaces -name "*.java"; \
           find src/main/java/com/laboratorio/financas/conta/domain -name "ContaNaoEncontradaException.java"; \
           find src/main/java/com/laboratorio/financas/shared/infrastructure/web -name "GlobalExceptionHandler.java"; \
           find src/test/java/com/laboratorio/financas/conta/application -name "*.java"; \
           find src/test/java/com/laboratorio/financas/conta/interfaces -name "*.java"); do
    echo "$f:"; xxd "$f" | head -1
done

.\mvnw.cmd verify

git status
```

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Arquivos permitidos:
   - `conta/domain/ContaNaoEncontradaException.java` (criação)
   - `conta/application/CriarContaUseCase.java` (criação)
   - `conta/application/ListarContasUseCase.java` (criação)
   - `conta/application/BuscarContaPorIdUseCase.java` (criação)
   - `conta/application/DesativarContaUseCase.java` (criação)
   - `conta/interfaces/dto/CriarContaRequest.java` (criação)
   - `conta/interfaces/dto/ContaResponse.java` (criação)
   - `conta/interfaces/ContaController.java` (criação)
   - `shared/infrastructure/web/GlobalExceptionHandler.java` (criação)
   - `shared/infrastructure/web/SecurityConfig.java` (apenas adicionar entrada de whitelist)
   - `pom.xml` (apenas descomentar regras JaCoCo de `application` e `interfaces`)
   - 4 arquivos de teste de use case (criação)
   - `ContaControllerTest.java` (criação)
   - `docs/decisoes.md`
   - `docs/progresso.md`
   - `docs/prompt-etapa-3-4.md` (este arquivo, versionar)

2. **Não tocar em `Conta.java`, `TipoConta.java`, `ContaRepository.java`, `ContaEntity.java`, `ContaMapper.java`, `ContaRepositoryImpl.java`, `ContaJpaRepository.java`, `Money.java`, `MoneyEmbeddable.java`.** Etapas anteriores entregaram esses prontos.

3. **Não tocar em `application*.yml`, `docker-compose.yml`, `scripts/*.ps1`, migrations Flyway.**

4. **Não criar `application-prod.yml`.** Débito registrado em `hooks-pendentes.md` desde 3.3.1.

5. **Não criar use cases além dos 4 prescritos.** Sem `RenomearContaUseCase`, `AlterarTipoContaUseCase`, `ReativarContaUseCase`. Sem caso de uso ainda.

6. **Não criar endpoints adicionais.** Sem `PATCH /api/contas/{id}`, sem `POST /api/contas/{id}/desativar`. DELETE faz soft delete, ponto.

7. **Não criar paginação.** Sem `Pageable`, sem `Page<ContaResponse>`. Lista pura.

8. **Não criar ResponseEntity manualmente em todos os endpoints.** Apenas onde agrega valor (POST com 201). GET retorna o tipo direto, DELETE usa `@ResponseStatus`.

9. **Não criar exceções customizadas além de `ContaNaoEncontradaException`.** `IllegalArgumentException` resolve validação. Nada de `ContaInvalidaException`, `MoedaNaoSuportadaException`, etc.

10. **Não usar Lombok nesta etapa.** Class explícita. Lombok pode entrar quando justificar (provavelmente em entity da próxima etapa de domínio).

11. **Não criar campos extras nos DTOs.** Sem `_links`, sem HATEOAS, sem hypermedia.

12. **Não relaxar thresholds JaCoCo.** Cobrir o que precisar.

13. **Não relaxar Checkstyle nem desabilitar regras.** Padrão consolidado.

14. **Sem acentos no código Java.** Padrão.

15. **Encoding UTF-8 sem BOM.** Validar com `xxd`.

16. **Naming de método de teste em camelCase puro.** Regra confirmada na 3.1.

17. **Indentação Checkstyle**: continuações em 16 espaços, getters em bloco multi-linha, chaves obrigatórias em `if/else`. Esboços já alinhados.

18. **Não criar `OpenAPI`/`@Operation`/`@Schema` annotations.** Springdoc gera doc automática a partir do controller. Anotações extras entram quando justificarem.

19. **Lições da Etapa 3.4 só registram observações reais.**

20. **Não antecipar próxima etapa.** Sem rascunhar `categoria`, `transacao`, `Auth`. Sem criar pacote vazio.

21. **Não tomar decisão silenciosa em zona limítrofe.** Se algo divergir entre prompt e código vivo (Checkstyle, decisoes.md, testes existentes), parar e reportar — padrão consolidado desde a 3.1.

## Estrutura de commits

Branch: `feat/conta-application-interfaces`

Commits atômicos, em ordem:

**Commit 1** — `feat(conta): adiciona ContaNaoEncontradaException`
- `src/main/java/com/laboratorio/financas/conta/domain/ContaNaoEncontradaException.java`

**Commit 2** — `feat(conta): adiciona use cases (criar, listar, buscar, desativar)`
- 4 arquivos em `conta/application/`

**Commit 3** — `feat(conta): adiciona DTOs de request e response`
- 2 arquivos em `conta/interfaces/dto/`

**Commit 4** — `feat(conta): adiciona ContaController com endpoints REST`
- `conta/interfaces/ContaController.java`

**Commit 5** — `feat(shared): adiciona GlobalExceptionHandler com ProblemDetail`
- `shared/infrastructure/web/GlobalExceptionHandler.java`

**Commit 6** — `feat(security): whitelist temporaria de /api/contas/** ate JWT entrar`
- `shared/infrastructure/web/SecurityConfig.java`

**Commit 7** — `build: ativa thresholds JaCoCo de application 80% e interfaces 70%`
- `pom.xml`

**Commit 8** — `test(conta): cobertura unitaria dos 4 use cases`
- 4 arquivos em `test/.../conta/application/`

**Commit 9** — `test(conta): cobertura e2e do ContaController via MockMvc + Testcontainers`
- `test/.../conta/interfaces/ContaControllerTest.java`

**Commit 10** — `docs: registra etapa 3.4 (conta application+interfaces) em decisoes e progresso`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-3-4.md`

## Validação antes de abrir PR

```bash
.\mvnw.cmd verify
git status
git log --oneline -12
```

Esperado:
- BUILD SUCCESS
- ~134 testes total
- Working tree limpo
- 10 commits na branch

## PR

Título: `feat: etapa 3.4 — bounded context conta finalizado (application + interfaces)`

Body sugerido:

```markdown
## Summary

Implementa a Etapa 3.4 do roadmap (quarta e última etapa de `conta` na Camada 2): camada de application (use cases) + interfaces (REST controller, DTOs, exception handler global). Bounded context `conta` agora está fechado ponta a ponta.

### Padrões que estreiam aqui

1. Primeiro use case (`CriarContaUseCase` e demais) — padrão "1 classe = 1 caso de uso"
2. Primeiro DTO de API com Bean Validation (`CriarContaRequest`)
3. Primeiro `@RestController` de domínio (`ContaController`)
4. Primeiro `@RestControllerAdvice` com `ProblemDetail` (RFC 7807) — `GlobalExceptionHandler`
5. Primeiro `@Transactional` aplicado (no use case, não no repository)
6. Primeiro teste e2e via `MockMvc` exercitando ciclo completo
7. Primeira whitelist temporária de Security com TODO explícito
8. Thresholds JaCoCo `application/` 80% e `interfaces/` 70% ativados

### Endpoints REST

- `POST /api/contas` → 201 Created
- `GET /api/contas[?ativa=true]` → 200 OK, lista
- `GET /api/contas/{id}` → 200 OK ou 404 Not Found
- `DELETE /api/contas/{id}` → 204 No Content (soft delete via `Conta.desativar()`)

### Mudanças

- `conta/domain/ContaNaoEncontradaException.java`: primeira exceção customizada de domínio.
- `conta/application/`: 4 use cases (`@Component`, `@Transactional` apropriado, comando como record interno).
- `conta/interfaces/dto/CriarContaRequest.java`: record com Bean Validation (`@NotBlank`, `@Size`, `@NotNull`).
- `conta/interfaces/dto/ContaResponse.java`: record com método estático `fromDomain`.
- `conta/interfaces/ContaController.java`: 4 endpoints REST.
- `shared/infrastructure/web/GlobalExceptionHandler.java`: `@RestControllerAdvice` mapeando 400/404/500 com `ProblemDetail`.
- `shared/infrastructure/web/SecurityConfig.java`: whitelist temporária de `/api/contas/**` com TODO explícito.
- `pom.xml`: thresholds JaCoCo `application` 80% e `interfaces` 70% descomentados.
- ~50 testes novos: ~22 unitários (use cases com Mockito), ~14 e2e (`ContaControllerTest` via MockMvc + Testcontainers).

### Decisões de escopo

- **DELETE faz soft delete por trás** — cliente não precisa saber. `Conta.desativar()` retorna nova instância com `ativa=false`.
- **Sem paginação** — MVP single-user com poucas contas. Paginação real entra em `transacao`.
- **`ProblemDetail` (RFC 7807) nativo do Spring 6** — sem dependência extra.
- **Whitelist temporária com TODO obrigatório** — pragmatismo > pureza enquanto JWT não está pronto. Comentário explícito evita esquecimento.
- **Use cases não dependem entre si** — `DesativarContaUseCase` busca direto no repository, lançando exceção localmente. Evita acoplamento transitivo.
- **Conversão Conta ↔ DTOs via método estático `fromDomain`** — sem MapStruct nessa borda. Mapper continua usado em Entity ↔ Domain (3.3).
- **Mockito apenas em testes de use case** — repository é interface estável (justifica). Testes e2e usam stack real via Testcontainers.

### Validação

- `mvnw verify` local: PASSOU
- ~50 testes novos passando
- Checkstyle: 0 violações
- SpotBugs: 0 issues
- JaCoCo: todos os thresholds atendidos (BUNDLE 75%, domain 90%, infrastructure 60%, application 80%, interfaces 70%)
- Validação destrutiva manual (operador, pós-merge): subir `dev.ps1`, `curl POST /api/contas`, `curl GET /api/contas`, etc.

### Próximo passo

Bounded context `conta` fechado. Próximas etapas (em discussão separada): `categoria`, `transacao`, ou `Auth` real (substituir whitelist).
```

## Pós-criação do PR

1. Abrir o PR via `gh pr create`.
2. Capturar o número.
3. Editar `docs/decisoes.md` e `docs/progresso.md` substituindo `Mergeado via PR #XX` por `Mergeado via PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico`
5. Push.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `feat/conta-application-interfaces` empurrada com 11 commits (10 + 1 update do PR)
- PR aberto, CI verde, **não mergeado**
- `main` ainda em `5fa9f8e` (squash da 3.3) ou no commit do squash da 3.3.1 (PR #32 mergeado)
- Working tree limpo
- Bounded context `conta` fechado ponta a ponta
- Reportar com `git log --oneline -11`, `git status`, `gh pr view --json number,state,statusCheckRollup` e parar.

## O que NÃO fazer ao terminar

- Não mergear o PR. Aguardar autorização explícita.
- Não criar prompt da próxima etapa.
- Não rascunhar próximas etapas (`categoria`, `transacao`, `Auth`).
- Não criar pacotes vazios "preparando o terreno".
- Não tocar em `frontend/`, `scripts/`, `docker-compose.yml`, `application*.yml`, migrations.
- Não criar `application-prod.yml` (débito registrado).
- Não sugerir "próximo passo" espontaneamente.
