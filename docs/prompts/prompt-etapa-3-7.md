# Prompt — Etapa 3.7: Bounded Context `transacao` — Application + Interfaces

## Contexto

A Etapa 3.6 foi concluída e fechada via PR #35. Domain + infra de `transacao` estão prontos: entidade com validações cruzadas, `TransacaoEntity` com FKs por UUID, `V4__cria_tabela_transacao.sql` com 3 FOREIGN KEYs e 2 CHECK constraints, mapper, repository básico (3 métodos), 40 testes incluindo 2 destrutivos comprovando defesa em profundidade no banco.

Esta etapa **fecha o bounded context `transacao` ponta a ponta**: implementa application (use cases com validação de FK) e interfaces (controller paginado, DTOs, handlers globais). Quando concluída, será possível registrar receitas/despesas/transferências via API REST com filtros compostos.

Esta é a **etapa mais densa de regras de negócio até agora**. Não pelas linhas (volume similar à 3.4), mas pelas decisões de design REST consolidadas: validação de FK antes de persistir, paginação real, filtros compostos, edição via PUT, exceção dedicada para FK inválida.

## Padrões que estreiam nesta etapa

1. **Primeiro use case com validação de FK** — `CriarTransacaoUseCase` valida que `contaId`, `contaDestinoId` (se houver) e `categoriaId` (se houver) **existem** antes de salvar
2. **Primeiro use case que recebe `Pageable`** — `ListarTransacoesUseCase`
3. **Primeira resposta paginada** (`Page<TransacaoResponse>`)
4. **Primeiro endpoint PUT** — edição via body completo
5. **Primeiros filtros compostos** — `?contaId`, `?dataInicio`, `?dataFim`, `?tipo`, `?categoriaId` combináveis
6. **Primeiro `@Transactional` que envolve 3 repositórios** (Conta, Categoria, Transacao)
7. **Primeira exceção dedicada para FK inválida** — `TransacaoComReferenciaInvalidaException` → 400 com mensagem específica
8. **Primeira query JPQL custom com filtros opcionais** (`WHERE :filtro IS NULL OR campo = :filtro`)
9. **Primeiro limite de `size` na paginação** — `@Max(100)` no controller

## Escopo decidido (calibrado com operador antes da redação)

### Endpoints REST

```
POST   /api/transacoes                                                   → 201 Created
GET    /api/transacoes                                                   → 200 OK, paginado
                                                                            params: page, size, sort, contaId, dataInicio, dataFim, tipo, categoriaId
GET    /api/transacoes/{id}                                              → 200 ou 404
PUT    /api/transacoes/{id}                                              → 200 ou 404 (body completo igual ao POST)
DELETE /api/transacoes/{id}                                              → 204 ou 404
```

### Decisões pontuais

- **DTO único `TransacaoRequest`** usado em POST e PUT. Mesmas validações, mesmos campos. Sem `Atualizar*Request` separado.
- **Paginação default**: `page=0&size=20&sort=data,desc`. Limite **máximo `size=100`** no controller via `@Max(100)`. Sem limite, cliente pode pedir milhões.
- **Filtros opcionais combináveis**: `?contaId`, `?dataInicio`, `?dataFim`, `?tipo`, `?categoriaId`. Todos opcionais. Sem multi-valor (`?tipo=RECEITA,DESPESA` adiado).
- **`?contaId` filtra origem OR destino**. Usuário pensa "transações da conta X" sem distinguir. Transferências aparecem nas duas contas.
- **`?dataInicio`/`?dataFim`** como `LocalDate`, inclusivos nas duas pontas. Spring converte query param automaticamente.
- **Filtros via JPQL com `:filtro IS NULL OR campo = :filtro`**. Sem `Specification`. Boring tech.
- **Resposta não inclui nomes resolvidos** (`contaNome`, `categoriaNome`). Só ids. Frontend faz join no cliente.
- **PUT permite alterar tipo** (RECEITA → TRANSFERENCIA, etc). Sem bloqueio. Mantém PUT simétrico ao POST.
- **`TransacaoComReferenciaInvalidaException` → 400** (não 422). Boring vence vistoso.
- **`TransacaoNaoEncontradaException` → 404**. Mesmo padrão de `ContaNaoEncontradaException`.
- **5 use cases**: `Criar`, `Listar`, `BuscarPorId`, `Editar`, `Deletar`. Use cases independentes (acoplam só ao repositório, não entre si — exceto `Editar` e `Deletar` que validam existência via repository).
- **Sem composição entre use cases.** `EditarTransacaoUseCase` busca direto no repository, não chama `BuscarTransacaoPorIdUseCase`. Padrão consolidado da 3.4.

### Localização dos arquivos

```
src/main/java/com/laboratorio/financas/transacao/
├── application/
│   ├── BuscarTransacaoPorIdUseCase.java
│   ├── CriarTransacaoUseCase.java
│   ├── DeletarTransacaoUseCase.java
│   ├── EditarTransacaoUseCase.java
│   └── ListarTransacoesUseCase.java
├── domain/
│   ├── TransacaoComReferenciaInvalidaException.java   ← novo
│   └── TransacaoNaoEncontradaException.java            ← novo
├── infrastructure/persistence/
│   └── (alterações em TransacaoJpaRepository — query custom)
└── interfaces/
    ├── TransacaoController.java
    └── dto/
        ├── TransacaoRequest.java
        └── TransacaoResponse.java

src/main/java/com/laboratorio/financas/shared/infrastructure/web/
└── (alterações em GlobalExceptionHandler — +2 handlers)

src/main/java/com/laboratorio/financas/shared/infrastructure/security/
└── (alterações em SecurityConfig — +1 whitelist)
```

### Use cases

**1. `CriarTransacaoUseCase`** (com validação de FK):

```java
@Component
public class CriarTransacaoUseCase {

    private final TransacaoRepository transacaoRepository;
    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;

    public CriarTransacaoUseCase(
            TransacaoRepository transacaoRepository,
            ContaRepository contaRepository,
            CategoriaRepository categoriaRepository
    ) {
        this.transacaoRepository = transacaoRepository;
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public record Comando(
            TipoTransacao tipo,
            BigDecimal valor,
            String moeda,
            LocalDate data,
            String descricao,
            UUID contaId,
            UUID contaDestinoId,
            UUID categoriaId
    ) { }

    @Transactional
    public Transacao executar(Comando comando) {
        validarReferencias(comando);

        Money valor = new Money(comando.valor(), Currency.getInstance(comando.moeda()));
        Transacao nova = new Transacao(
                comando.tipo(),
                valor,
                comando.data(),
                comando.descricao(),
                comando.contaId(),
                comando.contaDestinoId(),
                comando.categoriaId()
        );

        return transacaoRepository.salvar(nova);
    }

    private void validarReferencias(Comando comando) {
        if (contaRepository.buscarPorId(comando.contaId()).isEmpty()) {
            throw new TransacaoComReferenciaInvalidaException("conta", comando.contaId());
        }
        if (comando.contaDestinoId() != null
                && contaRepository.buscarPorId(comando.contaDestinoId()).isEmpty()) {
            throw new TransacaoComReferenciaInvalidaException("contaDestino", comando.contaDestinoId());
        }
        if (comando.categoriaId() != null
                && categoriaRepository.buscarPorId(comando.categoriaId()).isEmpty()) {
            throw new TransacaoComReferenciaInvalidaException("categoria", comando.categoriaId());
        }
    }
}
```

**Notas:**
- Validação de FK ANTES de construir `Money` e `Transacao` — falha rápida.
- Validações cruzadas do domain (regras de TRANSFERENCIA) disparam no construtor de `Transacao`. Se `IllegalArgumentException` cair aqui, o handler global captura como 400.
- `@Transactional` cobre validação + salvar atomicamente (se um repository validar e outro falhar, rollback).

**2. `ListarTransacoesUseCase`** (com paginação e filtros):

```java
@Component
public class ListarTransacoesUseCase {

    private final TransacaoRepository repository;

    public ListarTransacoesUseCase(TransacaoRepository repository) {
        this.repository = repository;
    }

    public record Filtros(
            UUID contaId,
            LocalDate dataInicio,
            LocalDate dataFim,
            TipoTransacao tipo,
            UUID categoriaId
    ) { }

    @Transactional(readOnly = true)
    public Page<Transacao> executar(Filtros filtros, Pageable pageable) {
        return repository.listarComFiltros(filtros, pageable);
    }
}
```

**Adicionar método em `TransacaoRepository`:**

```java
Page<Transacao> listarComFiltros(ListarTransacoesUseCase.Filtros filtros, Pageable pageable);
```

**Decisão sobre dependência circular:** o tipo `ListarTransacoesUseCase.Filtros` é referenciado em `TransacaoRepository` (domain). Mas `Filtros` é record interno de `ListarTransacoesUseCase` (application). Application depende de domain, mas domain dependendo de application via tipo interno é dependência circular conceitual.

**Mover `Filtros` para `transacao/domain/`** como record top-level:

```java
// transacao/domain/FiltrosTransacao.java
package com.laboratorio.financas.transacao.domain;

import java.time.LocalDate;
import java.util.UUID;

public record FiltrosTransacao(
        UUID contaId,
        LocalDate dataInicio,
        LocalDate dataFim,
        TipoTransacao tipo,
        UUID categoriaId
) { }
```

Use case e repository usam `FiltrosTransacao` (domain). Camadas respeitadas. Padrão consolidado.

**3. `BuscarTransacaoPorIdUseCase`** — espelho de `BuscarContaPorIdUseCase`:

```java
@Component
public class BuscarTransacaoPorIdUseCase {

    private final TransacaoRepository repository;

    public BuscarTransacaoPorIdUseCase(TransacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Transacao executar(UUID id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new TransacaoNaoEncontradaException(id));
    }
}
```

**4. `EditarTransacaoUseCase`** (busca + valida FKs + reconstrói):

```java
@Component
public class EditarTransacaoUseCase {

    private final TransacaoRepository transacaoRepository;
    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;

    public EditarTransacaoUseCase(
            TransacaoRepository transacaoRepository,
            ContaRepository contaRepository,
            CategoriaRepository categoriaRepository
    ) {
        this.transacaoRepository = transacaoRepository;
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public Transacao executar(UUID id, CriarTransacaoUseCase.Comando comando) {
        Transacao existente = transacaoRepository.buscarPorId(id)
                .orElseThrow(() -> new TransacaoNaoEncontradaException(id));

        validarReferencias(comando);

        Money valor = new Money(comando.valor(), Currency.getInstance(comando.moeda()));
        Transacao atualizada = new Transacao(
                existente.getId(),
                comando.tipo(),
                valor,
                comando.data(),
                comando.descricao(),
                comando.contaId(),
                comando.contaDestinoId(),
                comando.categoriaId(),
                existente.getCriadoEm(),
                Instant.now()
        );

        return transacaoRepository.salvar(atualizada);
    }

    private void validarReferencias(CriarTransacaoUseCase.Comando comando) {
        if (contaRepository.buscarPorId(comando.contaId()).isEmpty()) {
            throw new TransacaoComReferenciaInvalidaException("conta", comando.contaId());
        }
        if (comando.contaDestinoId() != null
                && contaRepository.buscarPorId(comando.contaDestinoId()).isEmpty()) {
            throw new TransacaoComReferenciaInvalidaException("contaDestino", comando.contaDestinoId());
        }
        if (comando.categoriaId() != null
                && categoriaRepository.buscarPorId(comando.categoriaId()).isEmpty()) {
            throw new TransacaoComReferenciaInvalidaException("categoria", comando.categoriaId());
        }
    }
}
```

**Decisão sobre reuso do `Comando`:** `EditarTransacaoUseCase` reusa `CriarTransacaoUseCase.Comando` porque o shape é idêntico. Sem criar `EditarComando` separado. Se diverging futuro, refatora.

**Decisão sobre duplicação de `validarReferencias`:** método privado idêntico em `Criar` e `Editar`. Aceitável — extrair pra service compartilhada quebraria o padrão "use case independente". Duplicação de 12 linhas é o preço.

**5. `DeletarTransacaoUseCase`** — valida existência antes de deletar:

```java
@Component
public class DeletarTransacaoUseCase {

    private final TransacaoRepository repository;

    public DeletarTransacaoUseCase(TransacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id) {
        if (repository.buscarPorId(id).isEmpty()) {
            throw new TransacaoNaoEncontradaException(id);
        }
        repository.deletar(id);
    }
}
```

### Exceções (em `transacao/domain/`)

**`TransacaoNaoEncontradaException`:**

```java
package com.laboratorio.financas.transacao.domain;

import java.util.UUID;

public class TransacaoNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public TransacaoNaoEncontradaException(UUID id) {
        super("Transacao nao encontrada: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
```

**`TransacaoComReferenciaInvalidaException`:**

```java
package com.laboratorio.financas.transacao.domain;

import java.util.UUID;

public class TransacaoComReferenciaInvalidaException extends RuntimeException {

    private final String recurso;
    private final UUID id;

    public TransacaoComReferenciaInvalidaException(String recurso, UUID id) {
        super("Referencia invalida: " + recurso + " com id " + id + " nao existe");
        this.recurso = recurso;
        this.id = id;
    }

    public String getRecurso() {
        return recurso;
    }

    public UUID getId() {
        return id;
    }
}
```

### DTOs

**`TransacaoRequest`** (record com Bean Validation, usado em POST e PUT):

```java
package com.laboratorio.financas.transacao.interfaces.dto;

import com.laboratorio.financas.transacao.domain.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransacaoRequest(
        @NotNull
        TipoTransacao tipo,

        @NotNull
        BigDecimal valor,

        @NotNull
        @Size(min = 3, max = 3)
        String moeda,

        @NotNull
        LocalDate data,

        @NotBlank
        @Size(max = 200)
        String descricao,

        @NotNull
        UUID contaId,

        UUID contaDestinoId,

        UUID categoriaId
) { }
```

**Notas:**
- `@NotNull` em `valor` mas **não** `@Positive`. Validação de positividade fica no domain (`Money.ehPositivo` em `Transacao` construtor) para gerar `IllegalArgumentException` consistente, não erro Bean Validation.
- `contaDestinoId` e `categoriaId` SEM `@NotNull` (são opcionais, validação cruzada fica no domain).
- Não usar `@Pattern` no `moeda` para forçar uppercase — Bean Validation só verifica tamanho. Se cliente enviar `"brl"` minúsculo, `Currency.getInstance("brl")` lança `IllegalArgumentException` no use case, capturada pelo handler global. Aceitável.

**`TransacaoResponse`**:

```java
package com.laboratorio.financas.transacao.interfaces.dto;

import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransacaoResponse(
        UUID id,
        TipoTransacao tipo,
        BigDecimal valor,
        String moeda,
        LocalDate data,
        String descricao,
        UUID contaId,
        UUID contaDestinoId,
        UUID categoriaId,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static TransacaoResponse fromDomain(Transacao t) {
        return new TransacaoResponse(
                t.getId(),
                t.getTipo(),
                t.getValor().valor(),
                t.getValor().moeda().getCurrencyCode(),
                t.getData(),
                t.getDescricao(),
                t.getContaId(),
                t.getContaDestinoId(),
                t.getCategoriaId(),
                t.getCriadoEm(),
                t.getAtualizadoEm()
        );
    }
}
```

### Controller

**`TransacaoController`**:

```java
package com.laboratorio.financas.transacao.interfaces;

import com.laboratorio.financas.transacao.application.BuscarTransacaoPorIdUseCase;
import com.laboratorio.financas.transacao.application.CriarTransacaoUseCase;
import com.laboratorio.financas.transacao.application.DeletarTransacaoUseCase;
import com.laboratorio.financas.transacao.application.EditarTransacaoUseCase;
import com.laboratorio.financas.transacao.application.ListarTransacoesUseCase;
import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.interfaces.dto.TransacaoRequest;
import com.laboratorio.financas.transacao.interfaces.dto.TransacaoResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transacoes")
@Validated
public class TransacaoController {

    private static final int SIZE_MAX = 100;

    private final CriarTransacaoUseCase criarTransacaoUseCase;
    private final ListarTransacoesUseCase listarTransacoesUseCase;
    private final BuscarTransacaoPorIdUseCase buscarTransacaoPorIdUseCase;
    private final EditarTransacaoUseCase editarTransacaoUseCase;
    private final DeletarTransacaoUseCase deletarTransacaoUseCase;

    public TransacaoController(
            CriarTransacaoUseCase criarTransacaoUseCase,
            ListarTransacoesUseCase listarTransacoesUseCase,
            BuscarTransacaoPorIdUseCase buscarTransacaoPorIdUseCase,
            EditarTransacaoUseCase editarTransacaoUseCase,
            DeletarTransacaoUseCase deletarTransacaoUseCase
    ) {
        this.criarTransacaoUseCase = criarTransacaoUseCase;
        this.listarTransacoesUseCase = listarTransacoesUseCase;
        this.buscarTransacaoPorIdUseCase = buscarTransacaoPorIdUseCase;
        this.editarTransacaoUseCase = editarTransacaoUseCase;
        this.deletarTransacaoUseCase = deletarTransacaoUseCase;
    }

    @PostMapping
    public ResponseEntity<TransacaoResponse> criar(@Valid @RequestBody TransacaoRequest request) {
        CriarTransacaoUseCase.Comando comando = toComando(request);
        Transacao criada = criarTransacaoUseCase.executar(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransacaoResponse.fromDomain(criada));
    }

    @GetMapping
    public Page<TransacaoResponse> listar(
            @RequestParam(name = "contaId", required = false) UUID contaId,
            @RequestParam(name = "dataInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(name = "dataFim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(name = "tipo", required = false) TipoTransacao tipo,
            @RequestParam(name = "categoriaId", required = false) UUID categoriaId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(SIZE_MAX) int size
    ) {
        FiltrosTransacao filtros = new FiltrosTransacao(contaId, dataInicio, dataFim, tipo, categoriaId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "data"));
        Page<Transacao> resultado = listarTransacoesUseCase.executar(filtros, pageable);
        return resultado.map(TransacaoResponse::fromDomain);
    }

    @GetMapping("/{id}")
    public TransacaoResponse buscar(@PathVariable UUID id) {
        Transacao transacao = buscarTransacaoPorIdUseCase.executar(id);
        return TransacaoResponse.fromDomain(transacao);
    }

    @PutMapping("/{id}")
    public TransacaoResponse editar(@PathVariable UUID id, @Valid @RequestBody TransacaoRequest request) {
        CriarTransacaoUseCase.Comando comando = toComando(request);
        Transacao atualizada = editarTransacaoUseCase.executar(id, comando);
        return TransacaoResponse.fromDomain(atualizada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id) {
        deletarTransacaoUseCase.executar(id);
    }

    private CriarTransacaoUseCase.Comando toComando(TransacaoRequest request) {
        return new CriarTransacaoUseCase.Comando(
                request.tipo(),
                request.valor(),
                request.moeda(),
                request.data(),
                request.descricao(),
                request.contaId(),
                request.contaDestinoId(),
                request.categoriaId()
        );
    }
}
```

**Notas críticas:**

- **`@Validated`** na classe (não só `@Valid` em params) habilita validação de `@Min`/`@Max` em parâmetros simples (não-DTOs). Sem isso, `@Min(0)` em `int page` é ignorado.
- **`Sort.by(Sort.Direction.DESC, "data")`** — campo é `data` no domain, mas vai virar `data_transacao` no SQL via mapeamento JPA. Spring Data resolve sozinho? **Não tem certeza.** Verificar comportamento. Se não resolver, usar `"dataTransacao"` ou `"data_transacao"` (tentar variações). **Se quebrar:** parar e reportar — pode precisar de adaptação no JPQL.
- **`@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)`** força parsing como `yyyy-MM-dd`. Default do Spring pode aceitar `dd/MM/yyyy` em locale pt-BR — explicitar evita.
- **Limite `size <= 100`** via constante `SIZE_MAX`.

### TransacaoJpaRepository — query custom

Precisa adicionar query JPQL com filtros opcionais:

```java
package com.laboratorio.financas.transacao.infrastructure.persistence;

import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransacaoJpaRepository extends JpaRepository<TransacaoEntity, UUID> {

    @Query("""
            SELECT t FROM TransacaoEntity t
            WHERE (:contaId IS NULL OR t.contaId = :contaId OR t.contaDestinoId = :contaId)
              AND (:dataInicio IS NULL OR t.data >= :dataInicio)
              AND (:dataFim IS NULL OR t.data <= :dataFim)
              AND (:tipo IS NULL OR t.tipo = :tipo)
              AND (:categoriaId IS NULL OR t.categoriaId = :categoriaId)
            """)
    Page<TransacaoEntity> findComFiltros(
            @Param("contaId") UUID contaId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("tipo") TipoTransacao tipo,
            @Param("categoriaId") UUID categoriaId,
            Pageable pageable
    );
}
```

**Notas:**
- **`(:contaId IS NULL OR t.contaId = :contaId OR t.contaDestinoId = :contaId)`** — a regra "filtra origem OR destino" aparece aqui. Se `contaId` é null no filtro, condição passa (1=1).
- **JPQL, não SQL nativo.** Funciona com qualquer banco compatível com JPA.
- **`@Param("...")`** explícito porque o nome do parâmetro Java pode não bater com o nome no JPQL após compilação (embora com `-parameters` flag bata, o explícito é mais seguro).
- **Sem `@Modifying`** — query é só leitura.

### TransacaoRepository (interface domain) — adicionar método

```java
package com.laboratorio.financas.transacao.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransacaoRepository {

    Transacao salvar(Transacao transacao);

    Optional<Transacao> buscarPorId(UUID id);

    void deletar(UUID id);

    Page<Transacao> listarComFiltros(FiltrosTransacao filtros, Pageable pageable);
}
```

**Atenção:** importar `Page` e `Pageable` do Spring Data **no domain** **viola** parcialmente a regra "domain não conhece framework". `Page` é interface, `Pageable` é interface — não anotações Spring. Decisão pragmática: aceitar essa única exceção, alternativa seria criar abstrações próprias (`PaginaResultado<T>`, `Paginacao`) que adicionam camada sem ganho real.

**Registrar essa decisão em `decisoes.md`** explicitamente como exceção consciente.

### TransacaoRepositoryImpl — implementar listarComFiltros

```java
@Override
public Page<Transacao> listarComFiltros(FiltrosTransacao filtros, Pageable pageable) {
    return jpaRepository.findComFiltros(
            filtros.contaId(),
            filtros.dataInicio(),
            filtros.dataFim(),
            filtros.tipo(),
            filtros.categoriaId(),
            pageable
    ).map(mapper::toDomain);
}
```

### GlobalExceptionHandler — adicionar 2 handlers

Em `shared/infrastructure/web/GlobalExceptionHandler.java`, adicionar (junto com imports):

```java
import com.laboratorio.financas.transacao.domain.TransacaoComReferenciaInvalidaException;
import com.laboratorio.financas.transacao.domain.TransacaoNaoEncontradaException;
```

```java
@ExceptionHandler(TransacaoNaoEncontradaException.class)
public ProblemDetail handleTransacaoNaoEncontrada(TransacaoNaoEncontradaException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setTitle("Not Found");
    problem.setDetail(ex.getMessage());
    problem.setProperty("id", ex.getId().toString());
    return problem;
}

@ExceptionHandler(TransacaoComReferenciaInvalidaException.class)
public ProblemDetail handleReferenciaInvalida(TransacaoComReferenciaInvalidaException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle("Bad Request");
    problem.setDetail(ex.getMessage());
    problem.setProperty("recurso", ex.getRecurso());
    problem.setProperty("id", ex.getId().toString());
    return problem;
}
```

Posicionar logo após o handler de `CategoriaNaoEncontradaException`. Não tocar nos outros.

**Adicional:** adicionar handler para `ConstraintViolationException` se ainda não existir. Esse é o que dispara quando `@Min`/`@Max` em parâmetros do controller falham (com `@Validated`):

```java
import jakarta.validation.ConstraintViolationException;

@ExceptionHandler(ConstraintViolationException.class)
public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle("Bad Request");
    problem.setDetail("Parametro fora dos limites permitidos.");
    problem.setProperty("violacoes", ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .toList());
    return problem;
}
```

### SecurityConfig — whitelist

Em `shared/infrastructure/security/SecurityConfig.java`, adicionar `/api/transacoes/**` à lista `permitAll`, com mesmo TODO de Auth.

### Testes

**Use cases (5 arquivos)** — Mockito programático:

- `CriarTransacaoUseCaseTest`: ~10 testes
  - Caminho feliz com FK válida (RECEITA, DESPESA, TRANSFERENCIA)
  - FK conta inválida → `TransacaoComReferenciaInvalidaException`
  - FK contaDestino inválida (TRANSFERENCIA) → mesma exceção
  - FK categoria inválida → mesma exceção
  - Validação cruzada do domain falha (TRANSFERENCIA sem contaDestinoId) → `IllegalArgumentException`
  - Repositório.salvar é chamado uma vez no caminho feliz

- `EditarTransacaoUseCaseTest`: ~8 testes (similar a `Criar` + busca primeiro + preserva criadoEm + atualiza atualizadoEm)
- `BuscarTransacaoPorIdUseCaseTest`: ~3 testes (caminho feliz + 404)
- `DeletarTransacaoUseCaseTest`: ~4 testes (caminho feliz, 404, deletar é chamado uma vez)
- `ListarTransacoesUseCaseTest`: ~5 testes (delega ao repository com filtros e pageable)

**Controller (`TransacaoControllerTest`)** — e2e via MockMvc + Testcontainers:

~25 testes cobrindo:

POST:
1. POST válido (RECEITA) com FKs reais → 201
2. POST válido (TRANSFERENCIA) com FKs reais → 201
3. POST com nome blank → 400
4. POST com valor zero → 400 (vai pelo domain validation)
5. POST com contaId inexistente → 400 com `recurso: "conta"` no body
6. POST com contaDestinoId inexistente (TRANSFERENCIA) → 400 com `recurso: "contaDestino"`
7. POST com categoriaId inexistente → 400 com `recurso: "categoria"`
8. POST TRANSFERENCIA sem contaDestinoId → 400 (validação cruzada)

GET:
9. GET listar sem filtros → 200, paginado, default `size=20`
10. GET com `?size=200` → 400 (excede SIZE_MAX)
11. GET com `?size=0` → 400
12. GET com `?contaId=X` filtra origem
13. GET com `?contaId=X` filtra destino (TRANSFERENCIA aparece)
14. GET com `?dataInicio` e `?dataFim` filtra período
15. GET com `?tipo=RECEITA` filtra tipo
16. GET com `?categoriaId=X` filtra categoria
17. GET com filtros combinados
18. GET com `?tipo=XYZ` (inválido) → 400
19. GET por id existente → 200
20. GET por id inexistente → 404

PUT:
21. PUT atualiza transação existente → 200, criadoEm preservado, atualizadoEm atualizado
22. PUT em id inexistente → 404
23. PUT com FK inválida → 400

DELETE:
24. DELETE existente → 204
25. DELETE inexistente → 404

**Ciclo completo:**
26. POST → GET por id → PUT → GET (mostra atualização) → DELETE → GET (404)

**Total esperado: ~55 testes novos. ~265 total.**

### Cleanup `@AfterEach` — ordem importa

```java
@AfterEach
void limpar() {
    transacaoJpaRepository.deleteAll();  // filha primeiro
    contaJpaRepository.deleteAll();
    categoriaJpaRepository.deleteAll();
}
```

Inverter quebra com FK.

### JaCoCo

Todos os thresholds já ativos. Sem alteração no `pom.xml`.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra commit do squash da Etapa 3.6 com referência a PR #35
- `docs/prompt-etapa-3-7.md` presente como untracked
- Working tree limpo
- Pacote `transacao/application/` e `transacao/interfaces/` **não existem**
- Migration mais alta: V4

Validar com:

```bash
git status
git log --oneline -1
ls docs/prompt-etapa-3-7.md
ls -la src/main/java/com/laboratorio/financas/transacao/
ls src/main/java/com/laboratorio/financas/transacao/application/ 2>/dev/null && echo "ATENCAO" || echo "OK"
ls src/main/java/com/laboratorio/financas/transacao/interfaces/ 2>/dev/null && echo "ATENCAO" || echo "OK"
```

## Tarefas

### Tarefa 1 — Validar pré-requisitos

```bash
git status
git log --oneline -3
ls docs/prompt-etapa-3-7.md
ls -la src/main/java/com/laboratorio/financas/transacao/domain/
ls -la src/main/java/com/laboratorio/financas/transacao/infrastructure/persistence/
ls src/main/java/com/laboratorio/financas/transacao/application/ 2>/dev/null && echo "ATENCAO" || echo "OK"
ls src/main/java/com/laboratorio/financas/transacao/interfaces/ 2>/dev/null && echo "ATENCAO" || echo "OK"
ls src/main/java/com/laboratorio/financas/shared/infrastructure/web/GlobalExceptionHandler.java
ls src/main/java/com/laboratorio/financas/shared/infrastructure/security/SecurityConfig.java
```

### Tarefa 2 — Criar branch de trabalho

```bash
git checkout -b feat/transacao-application-interfaces
```

### Tarefa 3 — Antes de escrever, ler código vivo

```bash
cat src/main/java/com/laboratorio/financas/conta/application/CriarContaUseCase.java
cat src/main/java/com/laboratorio/financas/conta/interfaces/ContaController.java
cat src/main/java/com/laboratorio/financas/conta/interfaces/dto/CriarContaRequest.java
cat src/main/java/com/laboratorio/financas/conta/interfaces/dto/ContaResponse.java
cat src/main/java/com/laboratorio/financas/shared/infrastructure/web/GlobalExceptionHandler.java
cat src/main/java/com/laboratorio/financas/shared/infrastructure/security/SecurityConfig.java
cat src/main/java/com/laboratorio/financas/transacao/domain/Transacao.java
cat src/main/java/com/laboratorio/financas/transacao/domain/TransacaoRepository.java
```

Replicar fielmente os padrões. **Código vivo > esboço do prompt** quando divergirem.

### Tarefa 4 — Criar exceções de domain

`transacao/domain/TransacaoNaoEncontradaException.java` e `transacao/domain/TransacaoComReferenciaInvalidaException.java` conforme especificado.

### Tarefa 5 — Criar `FiltrosTransacao` em domain

`transacao/domain/FiltrosTransacao.java` conforme especificado.

### Tarefa 6 — Atualizar `TransacaoRepository` (interface domain)

Adicionar método `listarComFiltros(FiltrosTransacao, Pageable): Page<Transacao>`. Imports de `Page` e `Pageable` do Spring Data.

### Tarefa 7 — Atualizar `TransacaoJpaRepository`

Adicionar método `findComFiltros` com `@Query` JPQL conforme especificado.

### Tarefa 8 — Atualizar `TransacaoRepositoryImpl`

Implementar `listarComFiltros` delegando ao JpaRepository.

### Tarefa 9 — Criar 5 use cases em `transacao/application/`

Conforme especificado. **Hard line:** validação de FK em `Criar` e `Editar` é duplicação aceita (12 linhas idênticas). NÃO extrair para service compartilhado.

### Tarefa 10 — Criar testes dos use cases

5 arquivos com Mockito programático, ~30 testes total. Naming camelCase puro. Indentação 16 espaços nas continuações.

### Tarefa 11 — Criar DTOs

`transacao/interfaces/dto/TransacaoRequest.java` e `transacao/interfaces/dto/TransacaoResponse.java` conforme especificado.

### Tarefa 12 — Criar `TransacaoController`

Conforme especificado. Atenção a:
- `@Validated` na classe
- `@DateTimeFormat(iso = ISO.DATE)` em `LocalDate` query params
- `@Min`/`@Max` em `page`/`size`
- Sort default `data,desc` — se quebrar com nome de campo, parar e reportar

### Tarefa 13 — Atualizar `GlobalExceptionHandler`

Adicionar 3 handlers: `TransacaoNaoEncontradaException`, `TransacaoComReferenciaInvalidaException`, `ConstraintViolationException`. Imports correspondentes.

### Tarefa 14 — Atualizar `SecurityConfig`

Adicionar `/api/transacoes/**` à whitelist com TODO de Auth.

### Tarefa 15 — Criar `TransacaoControllerTest` (e2e)

~25 testes conforme especificado. **Cleanup ordenado** no `@AfterEach`. **Setup helpers** para criar `Conta` e `Categoria` persistidas:

```java
private UUID criarContaPersistida() {
    Conta conta = new Conta("Conta " + UUID.randomUUID().toString().substring(0, 8),
                            TipoConta.CORRENTE,
                            new Money(BigDecimal.ZERO, BRL));
    contaRepositoryImpl.salvar(conta);
    return conta.getId();
}

private UUID criarCategoriaPersistida(TipoCategoria tipo) {
    Categoria cat = new Categoria("Categoria " + tipo, tipo);
    categoriaRepositoryImpl.salvar(cat);
    return cat.getId();
}
```

**Atenção a JSON dos testes:** body com `LocalDate` precisa ser ISO format (`"2025-01-15"`). UUIDs como string.

### Tarefa 16 — Validar localmente

```bash
.\mvnw.cmd compile
.\mvnw.cmd test -Dtest='Transacao*Test'
.\mvnw.cmd verify
```

**Esperado:**
- BUILD SUCCESS
- ~55 novos testes (~265 total)
- Checkstyle 0, SpotBugs 0
- JaCoCo todos os thresholds atendidos

**Possíveis pontos de atrito (parar e reportar):**

1. **Sort `"data"` no Pageable**: Spring Data pode confundir nome de campo entre domain (`data`) e SQL (`data_transacao`). Se erro de "property not found", tentar `"data_transacao"` ou `"dataTransacao"`. Documentar no PR body qual variação funcionou.
2. **`@Validated` na classe controller** sem habilitar globalmente pode não capturar `@Min`/`@Max` — verificar se `ConstraintViolationException` é lançada como esperado.
3. **`@DateTimeFormat`** pode ser ignorado em algumas versões do Spring Boot 3 — testar com `?dataInicio=2025-01-01` real.
4. **JPQL named parameters** podem precisar de flag `-parameters` no compilador. Já está configurado no `pom.xml` (`<parameters>true</parameters>`)? Verificar antes de declarar bug.
5. **Cleanup ordenado** com transação aberta pode falhar — usar `@AfterEach` sem `@Transactional`.
6. **Indentação Checkstyle**: 16 espaços nas continuações, lambdas em testes precisam atenção. Esboços já alinhados.
7. **Naming de teste em camelCase puro**, sem underscore. Padrão consolidado.

### Tarefa 17 — Atualizar `docs/decisoes.md`

**17a.** Adicionar nota em "Padrões aplicados":

```markdown
- **Paginação via `Page<T>` e `Pageable` do Spring Data** (a partir da Etapa 3.7): aceito como exceção pragmática à regra "domain não conhece framework". Alternativa seria criar abstrações próprias (`PaginaResultado<T>`, `Paginacao`) que adicionam camada sem ganho real. `Page` e `Pageable` são interfaces, não anotações.
- **Validação de FK no use case via `Optional.isEmpty()` dos repositórios** (a partir da Etapa 3.7): use cases que criam/editam entidades com FK validam existência das referências antes de construir a entidade. Lança `*ComReferenciaInvalidaException` (extends `RuntimeException`) com nome do recurso e id, mapeada como 400 no handler global. Validação no banco (FK constraint) continua existindo como defesa em profundidade.
- **Filtros opcionais via JPQL `:filtro IS NULL OR campo = :filtro`** (a partir da Etapa 3.7): cada filtro é parâmetro opcional na query. Sem `Specification`. Boring tech. Migrar quando volume justificar.
```

**17b.** Adicionar entrada no histórico:

```markdown
- **2026-05-09** — Etapa 3.7 concluída: bounded context `transacao` finalizado ponta a ponta. 5 use cases (Criar/Listar/Buscar/Editar/Deletar), DTO único `TransacaoRequest` para POST e PUT, `TransacaoController` com paginação (`Pageable`) e 5 filtros opcionais combináveis, 2 novas exceções (`TransacaoNaoEncontradaException`, `TransacaoComReferenciaInvalidaException`), 3 handlers globais novos (incluindo `ConstraintViolationException`), whitelist atualizada. ~55 testes. Mergeado via PR #XX.
```

### Tarefa 18 — Atualizar `docs/progresso.md`

**18a.** Atualizar "Última atualização": `2026-05-09 (Etapa 3.7 — transacao application+interfaces)`.

**18b.** Marcar critério da Camada 2 (se houver — verificar lista de critérios).

**18c.** Adicionar seção "Lições da Etapa 3.7" com candidatos a hook + lições reais. Não inventar.

**18d.** Adicionar entrada no histórico:

```markdown
- **2026-05-09** — Etapa 3.7 concluída: `transacao` ponta a ponta. 5 use cases, controller com paginação e 5 filtros, ~55 testes. Mergeado via PR #XX.
```

### Tarefa 19 — Versionar este próprio prompt

`docs/prompt-etapa-3-7.md` no commit de docs.

### Tarefa 20 — Validação final antes de commitar

```bash
find src/main/java/com/laboratorio/financas/transacao -name "*.java" -newer src/main/resources/db/migration/V4__cria_tabela_transacao.sql -exec sh -c 'echo "$1:"; xxd "$1" | head -1' _ {} \;

.\mvnw.cmd verify

git status
```

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Permitidos:
   - Arquivos novos em `transacao/application/` (5 use cases)
   - Arquivos novos em `transacao/domain/` (2 exceções + `FiltrosTransacao`)
   - Arquivos novos em `transacao/interfaces/` (controller + 2 DTOs)
   - Edição em `transacao/domain/TransacaoRepository.java` (adicionar método)
   - Edição em `transacao/infrastructure/persistence/TransacaoJpaRepository.java` (query custom)
   - Edição em `transacao/infrastructure/persistence/TransacaoRepositoryImpl.java` (impl método)
   - Edição em `shared/infrastructure/web/GlobalExceptionHandler.java` (3 handlers + imports)
   - Edição em `shared/infrastructure/security/SecurityConfig.java` (1 entrada whitelist)
   - Arquivos novos de teste em `test/.../transacao/application/` e `test/.../transacao/interfaces/`
   - `docs/decisoes.md`, `docs/progresso.md`, `docs/prompt-etapa-3-7.md`

2. **Não tocar em `Transacao.java`, `TipoTransacao.java`, `TransacaoEntity.java`, `TransacaoMapper.java`.** Domain e infra prontos da 3.6.

3. **Não tocar em `conta/`, `categoria/`, exceto leitura para ler padrão.**

4. **Não tocar em `pom.xml`, `application*.yml`, `docker-compose.yml`, `scripts/*.ps1`, migrations existentes.**

5. **Não criar V5 ou outra migration.** Esta etapa não muda schema.

6. **Não criar mais use cases além dos 5 prescritos.** Sem `TransferirEntreContasUseCase`, sem `BuscarPorContaUseCase`, etc.

7. **Não criar mais exceções além das 2 prescritas.**

8. **Não criar mais DTOs além dos 2 prescritos.** Não criar `AtualizarTransacaoRequest` separado — reusar `TransacaoRequest`.

9. **Não criar `TransacaoSpecification` ou similar** (Spring Data Specification). Decisão explícita: filtros via JPQL.

10. **Não extrair `validarReferencias` para classe compartilhada entre `Criar` e `Editar`.** Duplicação aceita.

11. **Não usar Lombok.**

12. **Não relaxar Checkstyle, SpotBugs, JaCoCo.**

13. **Sem acentos no código Java.**

14. **Encoding UTF-8 sem BOM.**

15. **Naming de teste em camelCase puro, sem underscore.** Padrão consolidado.

16. **Indentação Checkstyle**: 16 espaços nas continuações, getters em bloco multi-linha, chaves obrigatórias em `if/else`, lambdas alinhadas.

17. **Antes de escrever cada classe, ler a contraparte de `conta/` ou ler arquivos existentes** (Tarefa 3). Código vivo > prompt.

18. **Validação destrutiva manual** é responsabilidade do operador, pós-merge. Documentar no PR body.

19. **Lições da Etapa 3.7 só registram observações reais.**

20. **Não antecipar 3.8.** Sem rascunhar saldo derivado.

21. **Não tomar decisão silenciosa em zona limítrofe.** Padrão consolidado.

## Estrutura de commits

Branch: `feat/transacao-application-interfaces`

**Commit 1** — `feat(transacao): adiciona excecoes de dominio (NaoEncontrada, ReferenciaInvalida)`
- 2 arquivos em `transacao/domain/`

**Commit 2** — `feat(transacao): adiciona FiltrosTransacao no dominio + atualiza Repository com listarComFiltros`
- 1 arquivo novo (`FiltrosTransacao.java`)
- Edição em `transacao/domain/TransacaoRepository.java`

**Commit 3** — `feat(transacao): adiciona query JPQL com filtros opcionais em TransacaoJpaRepository`
- Edição em `transacao/infrastructure/persistence/TransacaoJpaRepository.java`
- Edição em `transacao/infrastructure/persistence/TransacaoRepositoryImpl.java`

**Commit 4** — `feat(transacao): adiciona 5 use cases (Criar, Listar, Buscar, Editar, Deletar)`
- 5 arquivos em `transacao/application/`

**Commit 5** — `test(transacao): cobertura unitaria dos 5 use cases`
- 5 arquivos em `test/.../transacao/application/`

**Commit 6** — `feat(transacao): adiciona DTOs (TransacaoRequest, TransacaoResponse) e Controller`
- 3 arquivos em `transacao/interfaces/`

**Commit 7** — `feat(shared): adiciona handlers de Transacao* e ConstraintViolation no GlobalExceptionHandler`
- Edição em `shared/infrastructure/web/GlobalExceptionHandler.java`

**Commit 8** — `feat(security): whitelist temporaria de /api/transacoes/** ate JWT entrar`
- Edição em `shared/infrastructure/security/SecurityConfig.java`

**Commit 9** — `test(transacao): cobertura e2e do TransacaoController via MockMvc + Testcontainers`
- 1 arquivo em `test/.../transacao/interfaces/`

**Commit 10** — `docs: registra etapa 3.7 (transacao application+interfaces) em decisoes e progresso`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-3-7.md`

## Validação antes de abrir PR

```bash
.\mvnw.cmd verify
git status
git log --oneline -11
```

Esperado: BUILD SUCCESS, working tree limpo, 10 commits.

## PR

Título: `feat: etapa 3.7 — bounded context transacao finalizado (application + interfaces)`

Body sugerido:

```markdown
## Summary

Implementa a Etapa 3.7 do roadmap (etapa final de `transacao` na Camada 2): camada de application (5 use cases com validação de FK) + interfaces (REST controller paginado com 5 filtros opcionais combináveis, DTO único para POST e PUT). Bounded context `transacao` agora está fechado ponta a ponta.

### Padrões que estreiam aqui

1. Primeiro use case com validação de FK (`Criar` e `Editar` validam existência de Conta/Categoria antes de salvar)
2. Primeiro use case com `Pageable` (`Listar`)
3. Primeira resposta paginada (`Page<TransacaoResponse>`)
4. Primeiro endpoint PUT
5. Primeiros filtros compostos (5 opcionais: contaId, dataInicio, dataFim, tipo, categoriaId)
6. Primeira exceção dedicada para FK inválida (`TransacaoComReferenciaInvalidaException` → 400)
7. Primeira query JPQL custom com filtros opcionais (`:filtro IS NULL OR campo = :filtro`)
8. Primeiro limite de `size` na paginação (`@Max(100)`)

### Endpoints REST

```
POST   /api/transacoes              → 201
GET    /api/transacoes              → 200 paginado, 5 filtros opcionais
GET    /api/transacoes/{id}         → 200 ou 404
PUT    /api/transacoes/{id}         → 200 ou 404 (body completo)
DELETE /api/transacoes/{id}         → 204 ou 404
```

### Decisões de escopo

- **DTO único** `TransacaoRequest` para POST e PUT.
- **`?contaId` filtra origem OU destino** (transferências aparecem nas duas contas).
- **Validação de FK via repositórios diretos** no use case (decisão consolidada da 3.4).
- **Duplicação de `validarReferencias`** entre `Criar` e `Editar` aceita (12 linhas).
- **`Page<T>` e `Pageable` do Spring Data no domain** aceito como exceção pragmática.
- **Filtros via JPQL com `IS NULL OR`**, sem `Specification`. Boring tech.
- **PUT permite alterar tipo** (RECEITA → TRANSFERENCIA, etc).
- **Limite máximo `size=100`** no controller via `@Validated` + `@Max`.

### Mudanças

- 2 exceções de domínio em `transacao/domain/`.
- `FiltrosTransacao` (record) em `transacao/domain/`.
- `TransacaoRepository.listarComFiltros` (interface domain).
- `TransacaoJpaRepository.findComFiltros` (JPQL com 5 filtros opcionais).
- `TransacaoRepositoryImpl.listarComFiltros` (impl).
- 5 use cases em `transacao/application/`.
- 2 DTOs em `transacao/interfaces/dto/`.
- `TransacaoController` com 5 endpoints.
- 3 handlers novos em `GlobalExceptionHandler` (NaoEncontrada, ReferenciaInvalida, ConstraintViolation).
- 1 entrada na whitelist do `SecurityConfig`.
- ~55 testes novos: ~30 unitários (use cases) + ~25 e2e (`TransacaoControllerTest`).

### Validação

- `mvnw verify` local: PASSOU
- ~55 testes novos passando
- Checkstyle: 0 violações, SpotBugs: 0 issues
- JaCoCo: todos os thresholds atendidos

### Próximo passo

Etapa 3.8 (saldo derivado da Conta — endpoint `GET /api/contas/{id}/saldo` com query agregada sobre `transacao`) — fora do escopo deste PR.
```

## Pós-criação do PR

1. Abrir PR via `gh pr create`.
2. Capturar o número.
3. Editar `docs/decisoes.md` e `docs/progresso.md` substituindo `Mergeado via PR #XX` por `Mergeado via PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico`
5. Push.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `feat/transacao-application-interfaces` empurrada com 11 commits (10 + 1 update do PR)
- PR aberto, CI verde, **não mergeado**
- `main` ainda no squash da 3.6
- Working tree limpo
- Bounded context `transacao` fechado ponta a ponta
- Reportar com `git log --oneline -11`, `git status`, `gh pr view --json number,state,statusCheckRollup` e parar.

## O que NÃO fazer ao terminar

- Não mergear o PR.
- Não criar prompt da próxima etapa.
- Não rascunhar 3.8 (saldo derivado).
- Não criar saldo derivado, endpoint de saldo, query agregada SUM em `Conta` ou `Transacao`.
- Não tocar em `frontend/`, `scripts/`, `docker-compose.yml`, `pom.xml`, `application*.yml`, migrations.
- Não criar mais migrations.
- Não sugerir "próximo passo" espontaneamente.
