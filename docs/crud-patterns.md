# CRUD Patterns — financas-lab

Referencia canonica de padroes de implementacao do projeto. Usada pelo planejador
para inlinar trechos no prompt do executor, e pelo executor como guia de consulta.

Codigo real extraido dos bounded contexts `tag` (base), `carteira` (com enum),
`conta` (com Money/@Embedded) e `transacao` (com FK para outros bounded contexts).
Adaptar nomes, campos e tipos para o novo dominio.

---

## 1. Domain Layer

### 1.1 Entidade de dominio (padrao base — sem enum, sem Money)

Referencia: `tag/domain/Tag.java`

```java
package com.laboratorio.financas.<contexto>.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class <Entidade> {

    private static final int NOME_MAX_LENGTH = 100;

    private final UUID id;
    private final UUID userId;
    private final String nome;
    // ... outros campos imutaveis
    private final boolean ativo;
    private final Instant criadoEm;
    private Instant atualizadoEm;  // mutavel (desativar/atualizar)

    // Construtor de criacao: gera id e timestamps
    public <Entidade>(UUID userId, String nome) {
        this(UUID.randomUUID(), userId, nome, true, Instant.now(), Instant.now());
    }

    // Construtor de reconstituicao: todos os campos (para o mapper)
    public <Entidade>(UUID id, UUID userId, String nome,
                      boolean ativo, Instant criadoEm, Instant atualizadoEm) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(userId, "userId nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarNome(nome);

        this.id = id;
        this.userId = userId;
        this.nome = nome.trim();
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    private static void validarNome(String nome) {
        Objects.requireNonNull(nome, "nome nao pode ser nulo");
        String trimmed = nome.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("nome nao pode ser vazio");
        }
        if (trimmed.length() > NOME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                "nome nao pode ter mais de " + NOME_MAX_LENGTH + " caracteres");
        }
    }

    public void desativar() {
        this.ativo = false;
        this.atualizadoEm = Instant.now();
    }

    public void atualizar(String novoNome) {
        validarNome(novoNome);
        this.nome = novoNome.trim();
        this.atualizadoEm = Instant.now();
    }

    // Getters para todos os campos
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof <Entidade> other)) return false;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}
```

### 1.2 Com enum (adicionar ao padrao base)

Referencia: `carteira/domain/TipoCarteira.java` + `CarteiraEntity.java`

```java
// Arquivo separado: <Entidade>/<Dominio>/Tipo<Entidade>.java
public enum Tipo<Entidade> {
    VALOR_A,
    VALOR_B,
    VALOR_C,
    OUTROS
}

// Na entidade de dominio, adicionar campo:
private final Tipo<Entidade> tipo;

// No construtor de criacao:
Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
this.tipo = tipo;

// Getter:
public Tipo<Entidade> getTipo() { return tipo; }
```

### 1.3 Com Money/@Embedded (adicionar ao padrao base)

Referencia: `conta/domain/Conta.java` + `ContaEntity.java`

```java
import com.laboratorio.financas.shared.domain.Money;

// Campo na entidade de dominio:
private final Money valorLimite;  // Money e um record(BigDecimal valor, Currency moeda)

// No construtor:
Objects.requireNonNull(valorLimite, "valorLimite nao pode ser nulo");
if (valorLimite.valor().compareTo(BigDecimal.ZERO) <= 0) {
    throw new IllegalArgumentException("valorLimite deve ser positivo");
}
this.valorLimite = valorLimite;
```

### 1.4 Repository interface

```java
package com.laboratorio.financas.<contexto>.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface <Entidade>Repository {
    <Entidade> salvar(<Entidade> entidade);
    Optional<<Entidade>> buscarPorId(UUID id);
    List<<Entidade>> listarPorUserId(UUID userId);
    <Entidade> atualizar(<Entidade> entidade);
    void deletar(UUID id);
}
```

### 1.5 Exception de dominio

```java
package com.laboratorio.financas.<contexto>.domain;

import java.util.UUID;

public class <Entidade>NaoEncontradoException extends RuntimeException {

    private final UUID id;

    public <Entidade>NaoEncontradoException(UUID id) {
        super("<Entidade> nao encontrado: " + id);
        this.id = id;
    }

    public UUID getId() { return id; }
}
```

### 1.6 Relacionamento FK com outro bounded context

Quando o novo dominio referencia outro bounded context (ex: `transacao` referencia `conta` e `categoria`),
o projeto usa UUID simples — sem `@ManyToOne`/`@JoinColumn`. A integracao e feita por ID, nao por objeto.

**Regra do projeto:** guarda UUID, valida existencia no use case, devolve UUID no response.

**Domain — apenas UUID no campo:**
```java
// Correto: referencia por UUID
private final UUID categoriaId;  // nao carrega o objeto Categoria

// Errado: nunca fazer
// private final Categoria categoria;
```

**Entity JPA — apenas @Column com o UUID:**
```java
// Correto: sem @ManyToOne, sem @JoinColumn
@Column(name = "categoria_id", columnDefinition = "uuid")   // nullable se opcional
private UUID categoriaId;

@Column(name = "conta_id", columnDefinition = "uuid", nullable = false)  // nullable=false se obrigatorio
private UUID contaId;

// Errado: nunca fazer
// @ManyToOne
// @JoinColumn(name = "categoria_id")
// private CategoriaEntity categoria;
```

**Use case — valida existencia antes de salvar (referencia: CriarTransacaoUseCase):**
```java
@Component
public class Criar<Entidade>UseCase {

    private final <Entidade>Repository <entidade>Repository;
    private final ContaRepository contaRepository;       // repositorio referenciado
    private final CategoriaRepository categoriaRepository; // repositorio referenciado

    // Injetar todos os repositorios no construtor
    public Criar<Entidade>UseCase(
            <Entidade>Repository <entidade>Repository,
            ContaRepository contaRepository,
            CategoriaRepository categoriaRepository) {
        this.<entidade>Repository = <entidade>Repository;
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public <Entidade> executar(Comando comando) {
        // Validar existencia das referencias ANTES de criar o objeto de dominio
        contaRepository.buscarPorId(comando.contaId())
                .orElseThrow(() -> new ContaNaoEncontradaException(comando.contaId()));

        // categoriaId pode ser opcional (nullable)
        if (comando.categoriaId() != null) {
            categoriaRepository.buscarPorId(comando.categoriaId())
                    .orElseThrow(() -> new CategoriaNaoEncontradaException(comando.categoriaId()));
        }

        <Entidade> nova = new <Entidade>(
                comando.contaId(),
                comando.categoriaId(),
                // ... outros campos
        );
        return <entidade>Repository.salvar(nova);
    }

    public record Comando(UUID contaId, UUID categoriaId, /* ... */) {}
}
```

**DTO Request — UUID anotado com @NotNull se obrigatorio:**
```java
public record Criar<Entidade>Request(
    @NotNull UUID contaId,         // obrigatorio
    UUID categoriaId,              // opcional (nullable sem @NotNull)
    // ...
) {}
```

**DTO Response — devolve UUID, nao o objeto aninhado:**
```java
public record <Entidade>Response(
    UUID id,
    UUID contaId,      // correto: so o UUID
    UUID categoriaId,  // correto: so o UUID
    // ...
) {}
// Errado: nunca embutir CategoriaResponse dentro de <Entidade>Response
```

**Migration SQL — FK explícita no banco:**
```sql
CREATE TABLE <tabela> (
    id              UUID            PRIMARY KEY,
    user_id         UUID            NOT NULL,
    conta_id        UUID            NOT NULL,     -- FK obrigatoria
    categoria_id    UUID,                         -- FK opcional (nullable)
    -- ... outros campos
    CONSTRAINT fk_<tabela>_conta      FOREIGN KEY (conta_id)     REFERENCES conta(id),
    CONSTRAINT fk_<tabela>_categoria  FOREIGN KEY (categoria_id) REFERENCES categoria(id)
);
CREATE INDEX idx_<tabela>_conta     ON <tabela> (conta_id);
CREATE INDEX idx_<tabela>_categoria ON <tabela> (categoria_id);
```

**Atencao: numero de migration reservado pelo planejador.** O executor usa o numero
ja fixado no campo `migracoes_reservadas` da task — nao recalcula dinamicamente.

**Cuidado com ordem de migrations:** a tabela referenciada (ex: `conta`) deve existir
antes da tabela que tem a FK. O planejador verifica via Passo 1.5 qual V ja existe.

---

## 2. Infrastructure Layer

### 2.1 Entity JPA (padrao base)

Referencia: `tag/infrastructure/persistence/TagEntity.java`

```java
package com.laboratorio.financas.<contexto>.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "<tabela>")
public class <Entidade>Entity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @NotNull
    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected <Entidade>Entity() {
        // Construtor protected exigido pelo JPA.
    }

    public <Entidade>Entity(UUID id, UUID userId, String nome,
                            boolean ativo, Instant criadoEm, Instant atualizadoEm) {
        this.id = id;
        this.userId = userId;
        this.nome = nome;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    // Getters para todos os campos (sem setters)
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
```

### 2.2 Com enum na Entity

Referencia: `carteira/infrastructure/persistence/CarteiraEntity.java`

```java
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

// Adicionar campo na Entity:
@NotNull
@Enumerated(EnumType.STRING)
@Column(name = "tipo", nullable = false, length = 30)
private Tipo<Entidade> tipo;
```

### 2.3 Com Money na Entity

Referencia: `conta/infrastructure/persistence/ContaEntity.java`

```java
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Embedded;

// Adicionar campo na Entity:
@NotNull
@Embedded
@AttributeOverrides({
    @AttributeOverride(name = "valor",
        column = @Column(name = "valor_limite_valor", nullable = false, precision = 19, scale = 2)),
    @AttributeOverride(name = "moeda",
        column = @Column(name = "valor_limite_moeda", nullable = false, length = 3))
})
private MoneyEmbeddable valorLimite;
```

### 2.4 JpaRepository

```java
package com.laboratorio.financas.<contexto>.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface <Entidade>JpaRepository extends JpaRepository<<Entidade>Entity, UUID> {

    List<<Entidade>Entity> findByUserId(UUID userId);
}
```

### 2.5 Mapper MapStruct

Referencia: `tag/infrastructure/persistence/TagMapper.java`

```java
package com.laboratorio.financas.<contexto>.infrastructure.persistence;

import com.laboratorio.financas.<contexto>.domain.<Entidade>;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface <Entidade>Mapper {

    default <Entidade>Entity toEntity(<Entidade> domain) {
        if (domain == null) return null;
        return new <Entidade>Entity(
                domain.getId(),
                domain.getUserId(),
                domain.getNome(),
                domain.isAtivo(),
                domain.getCriadoEm(),
                domain.getAtualizadoEm()
        );
    }

    default <Entidade> toDomain(<Entidade>Entity entity) {
        if (entity == null) return null;
        return new <Entidade>(
                entity.getId(),
                entity.getUserId(),
                entity.getNome(),
                entity.isAtivo(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm()
        );
    }
}
```

### 2.6 RepositoryImpl

Referencia: `tag/infrastructure/persistence/TagRepositoryImpl.java`

```java
package com.laboratorio.financas.<contexto>.infrastructure.persistence;

import com.laboratorio.financas.<contexto>.domain.<Entidade>;
import com.laboratorio.financas.<contexto>.domain.<Entidade>NaoEncontradoException;
import com.laboratorio.financas.<contexto>.domain.<Entidade>Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class <Entidade>RepositoryImpl implements <Entidade>Repository {

    private final <Entidade>JpaRepository jpaRepository;
    private final <Entidade>Mapper mapper;

    public <Entidade>RepositoryImpl(<Entidade>JpaRepository jpaRepository,
                                    <Entidade>Mapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public <Entidade> salvar(<Entidade> entidade) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(entidade)));
    }

    @Override
    public Optional<<Entidade>> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<<Entidade>> listarPorUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public <Entidade> atualizar(<Entidade> entidade) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(entidade)));
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
```

### 2.7 Migration SQL

```sql
-- V{N}__cria_tabela_<tabela>.sql
CREATE TABLE <tabela> (
    id              UUID            PRIMARY KEY,
    user_id         UUID            NOT NULL,
    nome            VARCHAR(100)    NOT NULL,
    -- campo enum:
    tipo            VARCHAR(30)     NOT NULL,
    -- campo Money:
    valor_limite_valor  NUMERIC(19,2)   NOT NULL,
    valor_limite_moeda  VARCHAR(3)      NOT NULL,
    ativo           BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMPTZ     NOT NULL,
    atualizado_em   TIMESTAMPTZ     NOT NULL
);
-- Sem FK para outros bounded contexts (integridade via aplicacao).
```

---

## 3. GlobalExceptionHandler — Wiring obrigatorio

**IMPORTANTE:** Registrar a nova excecao no handler ANTES de implementar o Controller.
Nao fazer isso causa 500 em vez de 404 e exige segundo run de mvn verify.

Referencia: `shared/infrastructure/web/GlobalExceptionHandler.java`

```java
// 1. Adicionar import no topo do GlobalExceptionHandler:
import com.laboratorio.financas.<contexto>.domain.<Entidade>NaoEncontradoException;

// 2. Adicionar handler (antes do handleGenerico no final):
@ExceptionHandler(<Entidade>NaoEncontradoException.class)
public ProblemDetail handle<Entidade>NaoEncontrado(<Entidade>NaoEncontradoException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setTitle("Not Found");
    problem.setDetail(ex.getMessage());
    problem.setProperty("id", ex.getId().toString());
    return problem;
}
```

---

## 4. Application Layer (Use Cases)

### 4.1 Padrao Criar

```java
package com.laboratorio.financas.<contexto>.application;

import com.laboratorio.financas.<contexto>.domain.<Entidade>;
import com.laboratorio.financas.<contexto>.domain.<Entidade>Repository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class Criar<Entidade>UseCase {

    private final <Entidade>Repository repository;

    public Criar<Entidade>UseCase(<Entidade>Repository repository) {
        this.repository = repository;
    }

    public <Entidade> executar(Comando comando) {
        <Entidade> entidade = new <Entidade>(comando.userId(), comando.nome());
        return repository.salvar(entidade);
    }

    public record Comando(UUID userId, String nome) {}
}
```

### 4.2 Padrao Listar

```java
@Service
public class Listar<Entidade>sUseCase {

    private final <Entidade>Repository repository;

    public Listar<Entidade>sUseCase(<Entidade>Repository repository) {
        this.repository = repository;
    }

    public List<<Entidade>> executar(UUID userId) {
        return repository.listarPorUserId(userId);
    }
}
```

### 4.3 Padrao Atualizar

```java
@Service
public class Atualizar<Entidade>UseCase {

    private final <Entidade>Repository repository;

    public Atualizar<Entidade>UseCase(<Entidade>Repository repository) {
        this.repository = repository;
    }

    public <Entidade> executar(Comando comando) {
        <Entidade> entidade = repository.buscarPorId(comando.id())
                .orElseThrow(() -> new <Entidade>NaoEncontradoException(comando.id()));
        entidade.atualizar(comando.nome());
        return repository.atualizar(entidade);
    }

    public record Comando(UUID id, String nome) {}
}
```

### 4.4 Padrao Deletar

```java
@Service
public class Deletar<Entidade>UseCase {

    private final <Entidade>Repository repository;

    public Deletar<Entidade>UseCase(<Entidade>Repository repository) {
        this.repository = repository;
    }

    public void executar(UUID id) {
        repository.buscarPorId(id)
                .orElseThrow(() -> new <Entidade>NaoEncontradoException(id));
        repository.deletar(id);
    }
}
```

---

## 5. Interface Layer (Controller + DTOs)

### 5.1 Controller

Referencia: `tag/interfaces/TagController.java`

**ATENCAO:** O Controller injeta `UsuarioRepository` para resolver userId a partir
do email do token JWT. Este e o padrao do projeto — nao usar UUID direto do token.

```java
package com.laboratorio.financas.<contexto>.interfaces;

import com.laboratorio.financas.<contexto>.application.*;
import com.laboratorio.financas.<contexto>.domain.<Entidade>;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/<plural>")
public class <Entidade>Controller {

    private static final Logger LOG = LoggerFactory.getLogger(<Entidade>Controller.class);

    private final Criar<Entidade>UseCase criarUseCase;
    private final Listar<Entidade>sUseCase listarUseCase;
    private final Atualizar<Entidade>UseCase atualizarUseCase;
    private final Deletar<Entidade>UseCase deletarUseCase;
    private final UsuarioRepository usuarioRepository;

    // Construtor com todos os campos

    @GetMapping
    public List<<Entidade>Response> listar(Authentication authentication) {
        UUID userId = resolverUserId(authentication);
        return listarUseCase.executar(userId).stream()
                .map(<Entidade>Response::fromDomain)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public <Entidade>Response criar(
            @Valid @RequestBody Criar<Entidade>Request request,
            Authentication authentication) {
        UUID userId = resolverUserId(authentication);
        Criar<Entidade>UseCase.Comando comando =
                new Criar<Entidade>UseCase.Comando(userId, request.nome());
        return <Entidade>Response.fromDomain(criarUseCase.executar(comando));
    }

    @PutMapping("/{id}")
    public <Entidade>Response atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody Atualizar<Entidade>Request request,
            Authentication authentication) {
        Atualizar<Entidade>UseCase.Comando comando =
                new Atualizar<Entidade>UseCase.Comando(id, request.nome());
        return <Entidade>Response.fromDomain(atualizarUseCase.executar(comando));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id, Authentication authentication) {
        deletarUseCase.executar(id);
    }

    // Extrator de userId — COPIAR VERBATIM, nao reinventar
    private UUID resolverUserId(Authentication authentication) {
        String email = authentication.getName();
        return usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuario autenticado nao encontrado: " + email))
                .getId();
    }
}
```

### 5.2 DTOs

```java
// Request de criacao
public record Criar<Entidade>Request(
        @NotBlank @Size(max = 100) String nome,
        @Size(max = 300) String descricao  // nullable sem @NotBlank
) {}

// Request de atualizacao
public record Atualizar<Entidade>Request(
        @NotBlank @Size(max = 100) String nome,
        @Size(max = 300) String descricao
) {}

// Response com factory method
public record <Entidade>Response(
        UUID id,
        String nome,
        String descricao,
        boolean ativo,
        String criadoEm,
        String atualizadoEm
) {
    public static <Entidade>Response fromDomain(<Entidade> domain) {
        return new <Entidade>Response(
                domain.getId(),
                domain.getNome(),
                domain.getDescricao(),
                domain.isAtivo(),
                domain.getCriadoEm().toString(),
                domain.getAtualizadoEm().toString()
        );
    }
}
```

---

## 6. Testes Java

### 6.1 Unit test de dominio

Referencia: `tag/domain/TagTest.java`

```java
class <Entidade>Test {

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void construtorCriacaoComArgumentosValidosCria<Entidade>() {
        Instant antes = Instant.now();
        <Entidade> e = new <Entidade>(USER_ID, "Nome Valido");
        Instant depois = Instant.now();

        assertThat(e.getId()).isNotNull();
        assertThat(e.getUserId()).isEqualTo(USER_ID);
        assertThat(e.getNome()).isEqualTo("Nome Valido");
        assertThat(e.isAtivo()).isTrue();
        assertThat(e.getCriadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorCriacaoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new <Entidade>(null, "Nome"))
                .withMessageContaining("userId");
    }

    @Test
    void construtorCriacaoComNomeNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new <Entidade>(USER_ID, null))
                .withMessageContaining("nome");
    }

    @Test
    void construtorCriacaoComNomeBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new <Entidade>(USER_ID, "  "))
                .withMessageContaining("nome");
    }

    @Test
    void desativarMudaAtivoParaFalseEAtualizaTimestamp() {
        <Entidade> e = new <Entidade>(USER_ID, "Nome");
        Instant antes = e.getAtualizadoEm();

        e.desativar();

        assertThat(e.isAtivo()).isFalse();
        assertThat(e.getAtualizadoEm()).isAfterOrEqualTo(antes);
    }
}
```

### 6.2 Unit test de use case (Mockito)

Referencia: `tag/application/CriarTagUseCaseTest.java`

```java
class Criar<Entidade>UseCaseTest {

    private <Entidade>Repository repository;
    private Criar<Entidade>UseCase useCase;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(<Entidade>Repository.class);
        useCase = new Criar<Entidade>UseCase(repository);
    }

    @Test
    void executarCaminhoFelizRetorna<Entidade>Criada() {
        <Entidade> salva = new <Entidade>(USER_ID, "Teste");
        when(repository.salvar(any(<Entidade>.class))).thenReturn(salva);

        Criar<Entidade>UseCase.Comando cmd = new Criar<Entidade>UseCase.Comando(USER_ID, "Teste");
        <Entidade> resultado = useCase.executar(cmd);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Teste");
        verify(repository, times(1)).salvar(any(<Entidade>.class));
    }

    @Test
    void executarComIdInexistenteLanca<Entidade>NaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(new Atualizar<Entidade>UseCase.Comando(id, "X")))
                .isInstanceOf(<Entidade>NaoEncontradoException.class);

        verify(repository, never()).atualizar(any());
    }
}
```

### 6.3 Integration test (Testcontainers)

Referencia: `tag/infrastructure/persistence/TagRepositoryImplTest.java`

```java
class <Entidade>RepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private <Entidade>RepositoryImpl repository;

    @Autowired
    private <Entidade>JpaRepository jpaRepository;

    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
        userId = criarUsuarioPersistido();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
    }

    private UUID criarUsuarioPersistido() {
        UUID id = UUID.randomUUID();
        usuarioJpaRepository.save(new UsuarioEntity(
                id, "teste+" + id + "@test.com", "hash", true,
                Instant.now(), null, Instant.now()));
        return id;
    }

    @Test
    void salvarEBuscarPorId() {
        <Entidade> e = new <Entidade>(userId, "Teste");
        repository.salvar(e);

        Optional<<Entidade>> resultado = repository.buscarPorId(e.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("Teste");
    }

    @Test
    void listarPorUserIdRetornaApenasDoUsuario() {
        repository.salvar(new <Entidade>(userId, "A"));
        repository.salvar(new <Entidade>(userId, "B"));
        repository.salvar(new <Entidade>(UUID.randomUUID(), "Outro"));

        List<<Entidade>> lista = repository.listarPorUserId(userId);

        assertThat(lista).hasSize(2);
        assertThat(lista).allMatch(e -> e.getUserId().equals(userId));
    }

    @Test
    void deletarRemoveEntidade() {
        <Entidade> e = new <Entidade>(userId, "Remover");
        repository.salvar(e);

        repository.deletar(e.getId());

        assertThat(repository.buscarPorId(e.getId())).isEmpty();
    }
}
```

### 6.4 E2E test (MockMvc)

Referencia: `tag/interfaces/TagControllerTest.java`

```java
@AutoConfigureMockMvc
class <Entidade>ControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private <Entidade>JpaRepository jpaRepository;

    @BeforeEach
    void setUp() { jpaRepository.deleteAll(); }

    @AfterEach
    void limpar() { jpaRepository.deleteAll(); }

    @Test
    void postValidoRetorna201() throws Exception {
        Map<String, Object> body = Map.of("nome", "Teste");
        mockMvc.perform(comAuth(post("/api/<plural>")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome", equalTo("Teste")));
    }

    @Test
    void postNomeBlankRetorna400() throws Exception {
        Map<String, Object> body = Map.of("nome", "   ");
        mockMvc.perform(comAuth(post("/api/<plural>")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome", notNullValue()));
    }

    @Test
    void getListaRetorna200() throws Exception {
        mockMvc.perform(comAuth(post("/api/<plural>")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nome", "A")))));

        mockMvc.perform(comAuth(get("/api/<plural>")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void putExistenteRetorna200() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/<plural>")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Antiga")))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(put("/api/<plural>/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Nova")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Nova")));
    }

    @Test
    void putInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(put("/api/<plural>/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "X")))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExistenteRetorna204() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/<plural>")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Remover")))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(delete("/api/<plural>/" + id)))
                .andExpect(status().isNoContent());
    }

    @Test
    void semAuthRetorna401() throws Exception {
        mockMvc.perform(get("/api/<plural>")).andExpect(status().isUnauthorized());
    }
}
```

---

## 7. Frontend Layer

### 7.1 Types

```typescript
// frontend/src/features/<dominio>/types/<dominio>.ts

// Para dominio com enum:
export type Tipo<Entidade> = 'VALOR_A' | 'VALOR_B' | 'VALOR_C' | 'OUTROS'

export interface <Entidade>Response {
  id: string
  nome: string
  descricao: string | null
  ativo: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface Criar<Entidade>Payload {
  nome: string
  descricao?: string
  // Se tiver enum: tipo: Tipo<Entidade>
}

export interface Atualizar<Entidade>Payload {
  nome: string
  descricao?: string
}
```

### 7.2 Service

```typescript
// frontend/src/features/<dominio>/services/<dominio>-service.ts
import { apiFetch } from '@/services/api-client'
import type { <Entidade>Response, Criar<Entidade>Payload, Atualizar<Entidade>Payload } from '../types/<dominio>'

export const <entidade>Service = {
  listar: (): Promise<<Entidade>Response[]> =>
    apiFetch<<Entidade>Response[]>('/api/<plural>'),

  criar: (payload: Criar<Entidade>Payload): Promise<<Entidade>Response> =>
    apiFetch<<Entidade>Response>('/api/<plural>', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  atualizar: (id: string, payload: Atualizar<Entidade>Payload): Promise<<Entidade>Response> =>
    apiFetch<<Entidade>Response>(`/api/<plural>/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  deletar: (id: string): Promise<void> =>
    apiFetch<void>(`/api/<plural>/${id}`, { method: 'DELETE' }),
}
```

### 7.3 Pagina de listagem

Referencia: `frontend/src/app/(dashboard)/tags/page.tsx`

Elementos obrigatorios:
- `SCREEN_CODE` no formato `MOD-ENT-001` (ver secao 8)
- `useQuery({ queryKey: ['<plural>'], queryFn: <entidade>Service.listar })`
- `DataTable` com colunas e `rowActions` (Editar + confirmar Excluir)
- `ActionsPanel` com `entityType`, `entityId`, `screenCode`
- Botao "Nova <Entidade>" navega para `/<plural>/nova`
- Edicao navega para `/<plural>/${id}` (nao `/editar` — so tags usam esse padrao legado)

### 7.4 Pagina de criacao (nova)

```typescript
// Elementos obrigatorios:
// 1. Schema Zod espelhando CriarRequest.java (B6 — bloqueador se divergir)
const schema = z.object({
  nome: z.string().min(1, 'Obrigatorio').max(100),
  descricao: z.string().max(300).optional(),
  // enum: tipo: z.enum(['VALOR_A', 'VALOR_B', 'VALOR_C', 'OUTROS'])
})

// 2. useDraftForm (OBRIGATORIO — clearDraft no onSuccess e no Cancelar)
const { clearDraft } = useDraftForm(form)

// 3. useMutation com invalidateQueries e redirect apos sucesso
const mutation = useMutation({
  mutationFn: (values: FormValues) => <entidade>Service.criar(values),
  onSuccess: async () => {
    clearDraft()
    await queryClient.invalidateQueries({ queryKey: ['<plural>'] })
    router.push('/<plural>')
  },
})

// 4. Select para enum (usar Controller, nao FormField — evita double-wrapping base-ui)
// Ver docs/field-type-catalog.md para o componente certo por tipo de campo
```

### 7.5 Pagina de edicao ([id])

```typescript
// Elementos obrigatorios:
// 1. useQuery para carregar dados existentes
const { data } = useQuery({
  queryKey: ['<plural>', id],
  queryFn: () => <entidade>Service.buscar(id),  // se GET /{id} existir
  // Se nao houver GET /{id}: listar().find(e => e.id === id)
})

// 2. resetWithDraft (substitui form.reset — preserva rascunho)
const { clearDraft, resetWithDraft } = useDraftForm(form)
useEffect(() => {
  if (data) resetWithDraft({ nome: data.nome, descricao: data.descricao ?? '' })
}, [data])

// 3. useMutation de atualizacao
const mutation = useMutation({
  mutationFn: (values: FormValues) => <entidade>Service.atualizar(id, values),
  onSuccess: async () => {
    clearDraft()
    await queryClient.invalidateQueries({ queryKey: ['<plural>'] })
    router.push('/<plural>')
  },
})
```

---

## 8. Screen Registry

### 8.1 Formato do codigo de tela

Formato: `MOD-ENT-NNN` (regex `^[A-Z]{3}-[A-Z]{3}-\d{3}$`)

| Modulo | Codigo | Exemplos de entidade |
|--------|--------|---------------------|
| FIN    | Financeiro | CTA=Conta, TRX=Transacao, ORC=Orcamento, MET=Meta, REC=Recorrente, FAT=Fatura, CAR=Carteira |
| CAD    | Cadastros | CAT=Categoria, TAG=Tag, GRP=Grupo, LIM=Limite |
| REL    | Relatorios | DSH=Dashboard, ANL=Analise |
| ADM    | Administracao | INC=Incidente |

NNN: `001` = tela de listagem (unico code por entidade nesta fase).

### 8.2 Entrada no registry

```typescript
// Adicionar em frontend/src/shared/shell/screens.registry.ts
// Apos a ultima entrada do modulo correspondente:
{
  code: 'CAD-LIM-001',          // substituir pelo codigo correto
  title: '<Entidade>s',
  path: '/<plural>',
  menuPath: ['Cadastros', '<Entidade>s'],
  icon: '<icone-lucide>',        // ver icon-map.ts para icones disponiveis
  permissions: [],
},
```

**Atencao:** Ha um teste `screens.registry.test.ts` com `expect(getAllScreens().length).toBe(N)`.
Ao adicionar uma entrada, incrementar N em 1 nesse teste.

---

## 9. Checklist de entrega (executor)

Antes de chamar /ship, confirmar que todos os itens abaixo existem:

**Backend:**
- [ ] `<Entidade>.java` (domain)
- [ ] `<Entidade>Repository.java` (interface)
- [ ] `<Entidade>NaoEncontradoException.java`
- [ ] `<Entidade>Entity.java`
- [ ] `<Entidade>JpaRepository.java`
- [ ] `<Entidade>Mapper.java`
- [ ] `<Entidade>RepositoryImpl.java`
- [ ] `V{N}__cria_tabela_<tabela>.sql`
- [ ] `Criar<Entidade>UseCase.java`
- [ ] `Listar<Entidade>sUseCase.java`
- [ ] `Atualizar<Entidade>UseCase.java`
- [ ] `Deletar<Entidade>UseCase.java`
- [ ] `<Entidade>Controller.java`
- [ ] DTOs: Criar/AtualizarRequest + Response
- [ ] `GlobalExceptionHandler.java` atualizado (wiring da nova excecao)

**Testes Java:**
- [ ] `<Entidade>Test.java` (unit domain)
- [ ] `Criar/Listar/Atualizar/Deletar<Entidade>UseCaseTest.java` (Mockito x4)
- [ ] `<Entidade>RepositoryImplTest.java` (Testcontainers)
- [ ] `<Entidade>ControllerTest.java` (MockMvc)

**Frontend:**
- [ ] `types/<dominio>.ts`
- [ ] `services/<dominio>-service.ts`
- [ ] `index.ts`
- [ ] `<plural>/page.tsx` (listagem com DataTable + SCREEN_CODE)
- [ ] `<plural>/nova/page.tsx` (criacao com useDraftForm)
- [ ] `<plural>/[id]/page.tsx` (edicao com resetWithDraft)
- [ ] Link no sidebar (`screens.registry.ts` + contador no teste)

**Testes Frontend:**
- [ ] `<dominio>-service.test.ts`
- [ ] `<plural>/page.test.tsx`
- [ ] `<plural>/nova/page.test.tsx`
- [ ] `<plural>/[id]/page.test.tsx`
