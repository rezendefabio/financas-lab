# CRUD Patterns — financas-lab

Referencia canonica de padroes de implementacao do projeto. Usada pelo planejador
para inlinar trechos no prompt do executor, e pelo executor como guia de consulta.

Codigo real extraido dos bounded contexts `tag` (base), `carteira` (com enum),
`conta` (com Money/@Embedded), `transacao` (FK + soft-delete + paginacao + filtros),
`orcamento` (calculo cross-context), `meta` (maquina de estados),
`categoria` (hierarquia + visibilidade) e `lancamentorecorrente` (value object com comportamento).
Adaptar nomes, campos e tipos para o novo dominio.

## Mapa de documentos (fronteira — nao duplicar)

Este arquivo ensina o **esqueleto** de um CRUD e linka para os docs especializados.
Ao gerar codigo, consultar tambem:

- **`docs/field-type-catalog.md`** — mapeamento tipo-backend -> componente-frontend
  (MoneyInput, LookupField, Select com render function, datas, FormGrid spans,
  formatters). Fonte unica para *qual* componente usar em cada campo. **Nao
  reproduzir essas decisoes aqui** — referenciar.
- **`docs/frontend-master-spec.md`** — arquitetura e contratos de UX do shell
  (Screen Registry, Tabs, Command Palette, `useListPage`, `FilterBar`,
  `ActionsPanel`, Audit Log, responsividade). Fonte do *porque* e dos contratos.
- **`docs/adrs.md`** — ADR-013 (feature-first), ADR-014 (shell declarativo + `useListPage`).

## Como escolher o padrao (simples -> complexo)

1. **Backbone (sempre)** — secoes 1 a 6: domain imutavel, Entity JPA, mapper,
   repository, 4 use cases CRUD, controller, DTOs, 4 niveis de teste. Cobre o CRUD
   estilo `tag`/`carteira`/`conta`.
2. **Relacionamento entre contextos** — secao 1.6: FK por UUID, validacao de
   existencia no use case. Nunca `@ManyToOne`.
3. **Complexidade alem do CRUD** — secao 10: soft-delete, paginacao+filtros
   (Specification), calculo cross-context, maquina de estados, agregacao JPQL,
   hierarquia/visibilidade, value object com comportamento, operacao composta
   (par vinculado). **Antes de assumir "CRUD simples", checar a secao 10** — se a
   entidade tem volume alto, estado mutavel com regras, ou depende de outro
   contexto para um calculo, ela cai em um desses padroes.
4. **Frontend** — secao 7: decide entre listagem client-side (backend `List<>`) e
   server-side paginada (backend `Page<>`), e usa o `*Form` compartilhado.

## Infra compartilhada assumida (NAO recriar)

Estes componentes ja existem no repositorio e sao consumidos pelos padroes
abaixo. Importar/estender — nunca recriar nem duplicar:

**Backend:**

- `shared/domain/Money` — value object `record(BigDecimal valor, Currency moeda)`
  com `ehPositivo()`, `valor()`, `moeda()`. Construir com
  `new Money(bd, Currency.getInstance("BRL"))`.
- `shared/infrastructure/persistence/MoneyEmbeddable` — usado nas Entities (secao 2.3).
- `shared/infrastructure/web/GlobalExceptionHandler` — **ja trata globalmente**:
  `MethodArgumentNotValidException` -> 400 com `{ erros: { campo: msg } }` (o teste
  E2E depende disso); `ConstraintViolationException` -> 400; `IllegalStateException`
  e `IllegalArgumentException` -> 400; `Exception` -> 500 (registra incidente).
  **So adicionar o handler `<Entidade>NaoEncontradoException` da entidade nova**
  (secao 3) — os demais ja cobrem o resto.
- `usuario/domain/UsuarioRepository` — injetado no controller para resolver userId.
- `auditlog/infrastructure/AuditPublisher` + `auditlog/domain/{AuditEvent,AuditAction}` —
  wiring de auditoria do controller (secao 5.1).
- `shared/AbstractIntegrationTest` e `AbstractAuthenticatedIntegrationTest` — bases
  de teste (secao 6.0). Estender, nao reescrever.

**Frontend:** `@/services/api-client` (`apiFetch`), `useListPage`, `FilterBar`,
`ActionsPanel`, `DataTable`, `StatusBadge`, `FormGrid`/`FormCol`, `LookupField`,
`MoneyInput`, `useDraftForm`, `formatters` — ver `field-type-catalog.md` e
`frontend-master-spec.md`.

## Convencoes canonicas (alinhadas ao contexto base `tag`)

- **Use cases sao `@Component`** (nao `@Service`). Stereotype usado por todos os
  contextos do projeto, incluindo a referencia base `tag`.
- **Use cases que escrevem sao `@Transactional`** (Criar/Atualizar/Deletar e
  transicoes de estado). Listar/Buscar podem ser `@Transactional(readOnly = true)`
  ou sem anotacao.
- **Controller resolve userId** injetando `UsuarioRepository` + parametro
  `Authentication`, via `authentication.getName()` (secao 5.1, copiar verbatim).
  NAO usar `SecurityContextHolder` direto (variante legada em `transacao`).
- **Auditoria e padrao, nao opcional** — todo controller publica `AuditEvent`
  (secao 5.1).
- **Tabelas usam nomes em portugues singular, NAO em ingles plural.** A tabela
  do usuario chama `usuario` (NAO `users`); idem `conta` (NAO `accounts`),
  `categoria`, `transacao`, `carteira`, `lembrete`, etc. FK para o usuario e
  `REFERENCES usuario(id)` -- NUNCA `users(id)`.

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

### 1.7 Relacionamento de colecao (M:N por UUID)

Quando a entidade referencia **varios** registros de outro contexto (ex: uma
transacao tem N tags), o projeto NAO usa `@ManyToMany` com entidades. Guarda uma
colecao de UUIDs, persistida via tabela de juncao com `@ElementCollection`.
Referencia: `transacao` (`tagIds`).

**Domain — colecao imutavel de UUID:**
```java
private final List<UUID> tagIds;   // nunca List<Tag>
// no construtor, copia defensiva:
this.tagIds = (tagIds != null) ? Collections.unmodifiableList(new ArrayList<>(tagIds))
                               : Collections.emptyList();
```

**Entity JPA — `@ElementCollection` + `@CollectionTable` (Set, nao @ManyToMany):**
```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "<tabela>_tag", joinColumns = @JoinColumn(name = "<tabela>_id"))
@Column(name = "tag_id")
private Set<UUID> tagIds = new HashSet<>();
```

**Mapper** — converter `List<UUID>` (domain) <-> `Set<UUID>` (entity):
`new HashSet<>(domain.getTagIds())` na ida; `new ArrayList<>(entity.getTagIds())` na volta.

**Migration — tabela de juncao dedicada:**
```sql
CREATE TABLE <tabela>_tag (
    <tabela>_id  UUID NOT NULL REFERENCES <tabela>(id) ON DELETE CASCADE,
    tag_id       UUID NOT NULL,
    PRIMARY KEY (<tabela>_id, tag_id)
);
CREATE INDEX idx_<tabela>_tag_tag ON <tabela>_tag (tag_id);
```

**DTO** — Request recebe `List<UUID> tagIds` (opcional); Response devolve `List<UUID>`
(so os UUIDs, nunca objetos Tag aninhados). No frontend e multi-select (checkboxes),
nao `LookupField` (que e single-select) — ver `TransacaoForm`.

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

**So a `<Entidade>NaoEncontradoException` precisa de handler novo.** Validacao (400
com `erros`), `IllegalStateException`/`IllegalArgumentException` (400) e erro
generico (500) ja sao tratados globalmente (ver "Infra compartilhada assumida") —
nao recriar.

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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Criar<Entidade>UseCase {

    private final <Entidade>Repository repository;

    public Criar<Entidade>UseCase(<Entidade>Repository repository) {
        this.repository = repository;
    }

    @Transactional
    public <Entidade> executar(Comando comando) {
        <Entidade> entidade = new <Entidade>(comando.userId(), comando.nome());
        return repository.salvar(entidade);
    }

    public record Comando(UUID userId, String nome) {}
}
```

### 4.2 Padrao Listar

```java
@Component
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
@Component
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
@Component
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

**ATENCAO (dois padroes obrigatorios do projeto, ambos presentes no `tag` base):**
1. Injeta `UsuarioRepository` para resolver userId pelo email do token JWT — nao
   usar UUID direto do token, nao usar `SecurityContextHolder`.
2. Publica `AuditEvent` via `AuditPublisher` em CREATE/UPDATE/DELETE e le o header
   opcional `X-Screen-Code`. **Auditoria e parte do CRUD, nao opcional.**

```java
package com.laboratorio.financas.<contexto>.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
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
    private static final String ENTITY_TYPE = "<contexto>";   // ex: "tag"

    private final Criar<Entidade>UseCase criarUseCase;
    private final Listar<Entidade>sUseCase listarUseCase;
    private final Atualizar<Entidade>UseCase atualizarUseCase;
    private final Deletar<Entidade>UseCase deletarUseCase;
    private final UsuarioRepository usuarioRepository;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    // Construtor com todos os campos (incluindo auditPublisher e objectMapper)

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
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = resolverUserId(authentication);
        Criar<Entidade>UseCase.Comando comando =
                new Criar<Entidade>UseCase.Comando(userId, request.nome());
        <Entidade>Response response = <Entidade>Response.fromDomain(criarUseCase.executar(comando));
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, response.id(), AuditAction.CREATE,
                userEmail(authentication), screenCode, null, toJson(response)));
        return response;
    }

    @PutMapping("/{id}")
    public <Entidade>Response atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody Atualizar<Entidade>Request request,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = resolverUserId(authentication);
        Atualizar<Entidade>UseCase.Comando comando =
                new Atualizar<Entidade>UseCase.Comando(id, request.nome());
        <Entidade>Response response = <Entidade>Response.fromDomain(atualizarUseCase.executar(comando));
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.UPDATE,
                userEmail(authentication), screenCode, null, toJson(response)));
        return response;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        resolverUserId(authentication);
        deletarUseCase.executar(id);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.DELETE,
                userEmail(authentication), screenCode, null, null));
    }

    // Extrator de userId — COPIAR VERBATIM, nao reinventar
    private UUID resolverUserId(Authentication authentication) {
        String email = authentication.getName();
        return usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuario autenticado nao encontrado: " + email))
                .getId();
    }

    private String userEmail(Authentication authentication) {
        return (authentication != null) ? authentication.getName() : null;
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            LOG.warn("Falha ao serializar payload de audit log para {}", ENTITY_TYPE, ex);
            return null;
        }
    }
}
```

> **Diff before/after na auditoria:** o `tag` base nao captura o estado anterior no
> UPDATE (passa `null` no `before`). Onde o diff importa, buscar a entidade antes
> de atualizar e passar `toJson(estadoAntes)` no campo `before` (padrao `transacao`).
> O `entityType` deve constar no middleware de auditoria — ver skill
> `add-entity-to-audit` e `frontend-master-spec.md` §5.

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

### 5.2.1 Campo monetario (Money) em DTOs

Referencia: `orcamento/interfaces/dto/OrcamentoResponse.java`.

`Money` (domain) usa `Currency`, que serializa mal em JSON. **Convencao do projeto:**
o Request recebe `valor` + `moeda` planos; o Response expoe um **record aninhado**
`ValorMonetario(BigDecimal valor, String moeda)` (o frontend le `campo.valor` —
field-type-catalog "Objetos aninhados").

```java
// Request — valor e moeda planos:
public record Criar<Entidade>Request(
        @NotNull BigDecimal valorLimite,                 // dominio exige positivo
        @NotNull @Size(min = 3, max = 3) String moeda,   // ex: "BRL"
        // ... outros campos
) {}

// Use case monta o Money: new Money(req.valorLimite(), Currency.getInstance(req.moeda()))

// Response — record aninhado (nao achatar, nao expor Currency):
public record <Entidade>Response(
        UUID id,
        ValorMonetario valorLimite,
        // ... outros campos
) {
    public record ValorMonetario(BigDecimal valor, String moeda) { }

    public static <Entidade>Response fromDomain(<Entidade> d) {
        return new <Entidade>Response(
                d.getId(),
                new ValorMonetario(d.getValorLimite().valor(),
                                   d.getValorLimite().moeda().getCurrencyCode()),
                // ...
        );
    }
}
```

> No frontend, o tipo TS espelha o aninhamento: `valorLimite: { valor: number; moeda: string }`.
> Envio (Payload) e plano (`valor`/`moeda`); leitura e aninhada. Ver secao 7.1.

---

## 6. Testes Java

### 6.0 Classes base de teste (nao modificar — copiar imports e heranca)

**AbstractIntegrationTest** — base para RepositoryImplTest:

```java
// package com.laboratorio.financas.shared;
// Extender em tests de repositorio: class <X>RepositoryImplTest extends AbstractIntegrationTest
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("financas_test").withUsername("test").withPassword("test");
    protected static final MinIOContainer MINIO =
            new MinIOContainer("minio/minio:latest")
            .withUserName("testminio").withPassword("testminio123");
    static { POSTGRES.start(); MINIO.start(); }
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("minio.endpoint", MINIO::getS3URL);
        registry.add("minio.access-key", MINIO::getUserName);
        registry.add("minio.secret-key", MINIO::getPassword);
        registry.add("minio.bucket", () -> "financas-lab-test");
    }
}
```

**AbstractAuthenticatedIntegrationTest** — base para ControllerTest:

```java
// Extender em tests de controller: class <X>ControllerTest extends AbstractAuthenticatedIntegrationTest
@AutoConfigureMockMvc
public abstract class AbstractAuthenticatedIntegrationTest extends AbstractIntegrationTest {
    @Autowired protected MockMvc mockMvc;
    @Autowired private UsuarioJpaRepository usuarioJpaRepository;
    protected String token;
    protected UUID authenticatedUserId;

    @BeforeEach
    void autenticar() throws Exception {
        String body = "{\"email\":\"executor@test.com\",\"senha\":\"senha12345678\"}";
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        token = new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("token").asText();
        authenticatedUserId = usuarioJpaRepository.findByEmail("executor@test.com")
                .map(u -> u.getId()).orElse(null);
    }

    protected MockHttpServletRequestBuilder comAuth(MockHttpServletRequestBuilder req) {
        return req.header("Authorization", "Bearer " + token);
    }
}
```

**Padrao de BeforeEach no RepositoryImplTest** (criar usuario persistido):

```java
@Autowired private UsuarioJpaRepository usuarioJpaRepository;
private UUID userId;

@BeforeEach
void setup() {
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
    UsuarioEntity entity = new UsuarioEntity(
            id,
            "teste+" + id + "@test.com",
            "hash_bcrypt",
            true,
            Instant.now(),
            null,
            Instant.now()
    );
    usuarioJpaRepository.save(entity);
    return id;
}
```

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

### 7.0 Utilitarios e imports frontend

**Formatters disponíveis** — importar de `@/shared/lib/formatters`:

```typescript
import { formatBRL, formatDate, formatDateTime } from '@/shared/lib/formatters'

// Uso:
formatBRL(123.45)          // → "R$ 123,45"
formatDate('2026-05-28')   // → "28/05/2026"
formatDateTime('2026-05-28T14:30:00Z') // → "28/05/2026, 11:30:00"
```

Campos monetarios na DataTable: sempre `cell: (v) => formatBRL(v.valor.valor)` (valor e objeto `{ valor, moeda }`).
Campos de data (Instant/string ISO): `cell: (v) => formatDateTime(v.criadoEm)`.
Campos LocalDate: `cell: (v) => formatDate(v.data)`.

**Teste de service** — padrao com `vi.mock` (copiar e adaptar):

```typescript
// frontend/src/features/<dominio>/services/<dominio>-service.test.ts
import { describe, it, expect, vi, afterEach } from 'vitest'

vi.mock('@/services/api-client', () => ({ apiFetch: vi.fn() }))

import { apiFetch } from '@/services/api-client'
import { listar<Entidades>, criar<Entidade>, atualizar<Entidade>, deletar<Entidade> }
  from './<dominio>-service'

const mock<Entidade> = {
  id: '00000000-0000-0000-0000-000000000001',
  nome: 'Teste',
  ativo: true,
  criadoEm: '2026-01-01T00:00:00Z',
  atualizadoEm: '2026-01-01T00:00:00Z',
}

afterEach(() => { vi.restoreAllMocks() })

describe('listar<Entidades>', () => {
  it('chama apiFetch com path correto', async () => {
    vi.mocked(apiFetch).mockResolvedValue([mock<Entidade>])
    const result = await listar<Entidades>()
    expect(apiFetch).toHaveBeenCalledWith('/api/<dominio>s')
    expect(result).toEqual([mock<Entidade>])
  })
})

describe('criar<Entidade>', () => {
  it('chama apiFetch com POST e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mock<Entidade>)
    const payload = { nome: 'Teste' }
    await criar<Entidade>(payload)
    expect(apiFetch).toHaveBeenCalledWith('/api/<dominio>s', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  })
})

describe('atualizar<Entidade>', () => {
  it('chama apiFetch com PUT id e payload', async () => {
    vi.mocked(apiFetch).mockResolvedValue(mock<Entidade>)
    const payload = { nome: 'Novo' }
    await atualizar<Entidade>(mock<Entidade>.id, payload)
    expect(apiFetch).toHaveBeenCalledWith(`/api/<dominio>s/${mock<Entidade>.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  })
})

describe('deletar<Entidade>', () => {
  it('chama apiFetch com DELETE no path com id', async () => {
    vi.mocked(apiFetch).mockResolvedValue(undefined)
    await deletar<Entidade>(mock<Entidade>.id)
    expect(apiFetch).toHaveBeenCalledWith(`/api/<dominio>s/${mock<Entidade>.id}`, {
      method: 'DELETE',
    })
  })
})
```

### 7.0.1 Decisao inicial: listagem client-side ou server-side?

Antes de gerar qualquer arquivo frontend, decidir o regime de listagem — isso
define o contrato do service e da pagina:

| Regime | Quando | Backend | Service retorna | Pagina usa |
|---|---|---|---|---|
| **Client-side** (default p/ cadastros) | Volume baixo, sem paginacao no backend | controller retorna `List<Response>` | `<Entidade>Response[]` | `useQuery` + filtro `useMemo` + `DataTable` |
| **Server-side paginada** | Volume alto (transacoes, audit), filtros/sort no banco | controller retorna `Page<Response>` (secao 10.2) | `PageResponse<<Entidade>Response>` | `useListPage` + `FilterBar` + paginacao |

Hoje so `transacao` e server-side; os demais cadastros sao client-side
(`frontend-master-spec.md` §1). Se o backend for `Page<>`, o service **deve**
retornar `PageResponse<T>` — caso contrario nao pluga no `useListPage`.

### 7.1 Types

```typescript
// frontend/src/features/<dominio>/types/<dominio>.ts

// Para dominio com enum:
export type Tipo<Entidade> = 'VALOR_A' | 'VALOR_B' | 'VALOR_C' | 'OUTROS'

export interface <Entidade>Response {
  id: string
  nome: string
  descricao: string | null
  // Money vem aninhado — NAO achatar (field-type-catalog "Objetos aninhados"):
  // valorLimite: { valor: number; moeda: string }
  ativo: boolean
  criadoEm: string
  atualizadoEm: string
}

export interface Criar<Entidade>Payload {
  nome: string
  descricao?: string
  // enum: tipo: Tipo<Entidade>
  // Money: valor: number; moeda: string  (envia plano, recebe aninhado)
}

export interface Atualizar<Entidade>Payload {
  nome: string
  descricao?: string
}
```

`PageResponse<T>` (regime server-side) ja existe — importar de
`@/shared/hooks/useListPage`, nao redefinir.

### 7.2 Service

**Variante client-side (cadastros):**

```typescript
// frontend/src/features/<dominio>/services/<dominio>-service.ts
import { apiFetch } from '@/services/api-client'
import type { <Entidade>Response, Criar<Entidade>Payload, Atualizar<Entidade>Payload } from '../types/<dominio>'

export const <entidade>Service = {
  listar: (): Promise<<Entidade>Response[]> =>
    apiFetch<<Entidade>Response[]>('/api/<plural>'),
  criar: (payload: Criar<Entidade>Payload) =>
    apiFetch<<Entidade>Response>('/api/<plural>', { method: 'POST', body: JSON.stringify(payload) }),
  atualizar: (id: string, payload: Atualizar<Entidade>Payload) =>
    apiFetch<<Entidade>Response>(`/api/<plural>/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deletar: (id: string) =>
    apiFetch<void>(`/api/<plural>/${id}`, { method: 'DELETE' }),
}
```

**Variante server-side paginada** (referencia: `transacoes.service.ts`) — `listar`
recebe params de filtro/paginacao e devolve `PageResponse<T>`:

```typescript
import type { PageResponse } from '@/shared/hooks/useListPage'

export interface Listar<Entidade>sParams {
  // filtros enum diretos + paginacao + sort
  tipo?: string
  /** Filtros adicionais: `campo:operador:valor,...` (ver secao 10.2). */
  filtros?: string
  page?: number
  size?: number
  /** `campo:dir` — ex: `data:desc`. */
  sort?: string
}

listar: (params?: Listar<Entidade>sParams) => {
  const qs = new URLSearchParams()
  if (params) Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== '') qs.set(k, String(v))
  })
  const query = qs.toString() ? `?${qs}` : ''
  return apiFetch<PageResponse<<Entidade>Response>>(`/api/<plural>${query}`)
},
```

### 7.3 Pagina de listagem (server-side paginada)

Referencia canonica: `frontend/src/app/(dashboard)/transacoes/page.tsx`.
Contrato em `frontend-master-spec.md` §4.5 (filtros) e §4.6 (ActionsPanel).

Elementos obrigatorios:
- `const SCREEN_CODE = 'MOD-ENT-001'` (secao 8).
- `useListPage<<Entidade>, ...>({ queryKey, fetcher, defaultSort })` — concentra
  filtros/paginacao/sort na URL e dispara o fetch. O `fetcher` traduz os
  `activeFilters` em params do backend (helper `buildBackendParams`, ver
  `transacoes/page.tsx`).
- `FilterBar` com `FilterFieldDef[]` (tipos `string|number|date|boolean|enum`) e
  `OPERATORS_BY_TYPE`. Filtros de FK (enum dinamico) populam `options` via
  `useQuery` da entidade referenciada.
- `DataTable` com `ColumnDef[]`: `render` formata via `formatBRL`/`formatDate`
  (field-type-catalog "Formatadores"); `StatusBadge` para enums com cor.
- `rowActions`: Editar (navega `/<plural>/${id}/editar`) + Excluir com
  **confirmacao inline** (state `confirmDeleteId`, troca botao por Confirmar/Cancelar).
- `ActionsPanel` com `entityType`, `entityId={selecionada?.id ?? null}`,
  `screenCode`, `onExportCsv` (via `exportToCsv`).
- Controles de paginacao (Anterior/Proxima) a partir de `page`/`totalPages`.

```tsx
const { data, totalElements, totalPages, page, sort, isLoading, activeFilters,
        addFilter, removeFilter, clearFilters, setPage, setSort } =
  useListPage<<Entidade>, Record<string, string>>({
    queryKey: '<plural>',
    fetcher: ({ activeFilters, page, size, sort }) =>
      <entidade>Service.listar({ ...buildBackendParams(activeFilters), page, size, sort }),
    defaultSort: { field: 'criadoEm', dir: 'desc' },
  })
```

### 7.3b Pagina de listagem (client-side, cadastros)

Para backend `List<>`: `useQuery({ queryKey: ['<plural>'], queryFn:
<entidade>Service.listar })`, filtro client-side com `useMemo`, mesmo `DataTable`
+ `rowActions` + `ActionsPanel`. Sem `useListPage`. Botao "Nova <Entidade>"
navega para `/<plural>/nova`.

### 7.4 Componente de formulario compartilhado (`<Entidade>Form`)

**Padrao do projeto: criacao e edicao compartilham um unico `<Entidade>Form`**
(referencia: `features/transacoes/components/TransacaoForm.tsx`). As paginas
`nova` e `[id]` sao wrappers finos que so diferem na mutation. NAO duplicar
schema/campos entre as duas paginas.

O componente concentra:
- `useForm` + `zodResolver(schema)`, com `schema` Zod **espelhando o `*Request.java`**
  (B6 — divergencia e bloqueador). Money -> `z.coerce.number().positive()`;
  `@Size(max=N)` -> `.max(N)`; FK obrigatoria -> `.uuid()`; opcional -> `.optional()`.
- `useDraftForm(form)` **internamente** (a pagina pai nao chama de novo —
  CLAUDE.md). `clearDraft()` no submit e no Cancelar.
- Layout `FormGrid` + `FormCol span={1-12}` (spans sugeridos em field-type-catalog).
- **Campo por tipo: seguir `field-type-catalog.md`** — `MoneyInput` para valor,
  `LookupField` para FK (com `queryKey` sufixada — B13), `Controller`+`Select`
  com `SelectValue` render-function para enum, `<Input type="date">` para LocalDate,
  `<input type="hidden">` para `moeda` (valor fixo `'BRL'`).

```tsx
interface <Entidade>FormProps {
  defaultValues: <Entidade>FormValues
  onSubmit: (values: <Entidade>FormValues) => void
  isSubmitting: boolean
  apiError: string | null
  onClearApiError: () => void
  submitLabel: string
  onCancel: () => void
}

export function <Entidade>Form({ defaultValues, onSubmit, isSubmitting, apiError,
                                 onClearApiError, submitLabel, onCancel }: <Entidade>FormProps) {
  const form = useForm<<Entidade>FormValues>({ resolver: zodResolver(schema), defaultValues })
  const { clearDraft } = useDraftForm(form)

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit((v) => { clearDraft(); onClearApiError(); onSubmit(v) })}
            className="space-y-4">
        <FormGrid>
          {/* enum: Controller + Select + SelectValue render fn (field-type-catalog) */}
          <FormCol span={6}>
            <Controller control={form.control} name="tipo" render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger className="w-full">
                  <SelectValue>{(v) => TIPOS.find(t => t.value === v)?.label ?? 'Selecione'}</SelectValue>
                </SelectTrigger>
                <SelectContent>{TIPOS.map(t => <SelectItem key={t.value} value={t.value}>{t.label}</SelectItem>)}</SelectContent>
              </Select>
            )} />
          </FormCol>
          {/* Money */}
          <FormCol span={6}>
            <FormField control={form.control} name="valor" render={({ field }) => (
              <FormItem><FormLabel>Valor (R$)</FormLabel>
                <FormControl><MoneyInput value={field.value} onChange={field.onChange} id={field.name} /></FormControl>
                <FormMessage /></FormItem>
            )} />
          </FormCol>
          {/* FK -> LookupField (queryKey com sufixo distinto — B13) */}
          <FormCol span={12}>
            <Controller control={form.control} name="categoriaId" render={({ field }) => (
              <LookupField value={field.value ?? null} onChange={(v) => field.onChange(v ?? undefined)}
                queryKey={['categorias', 'lookup']}
                queryFn={() => categoriasService.listar().then(cs => cs.map(c => ({ value: c.id, label: c.nome })))}
                placeholder="Selecione" />
            )} />
          </FormCol>
        </FormGrid>
        <input type="hidden" {...form.register('moeda')} />
        {apiError && <p className="text-sm text-destructive">{apiError}</p>}
        <div className="flex gap-3 pt-2">
          <Button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Salvando...' : submitLabel}</Button>
          <Button type="button" variant="outline" onClick={onCancel}>Cancelar</Button>
        </div>
      </form>
    </Form>
  )
}
```

### 7.5 Paginas `nova` e `[id]/editar` (wrappers finos)

```tsx
// nova/page.tsx — so a mutation de criacao
const mutation = useMutation({
  mutationFn: (values: <Entidade>FormValues) => <entidade>Service.criar(values),
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['<plural>'] })
    router.push('/<plural>')
  },
  onError: () => setApiError('Erro ao salvar.'),
})
return <<Entidade>Form defaultValues={default<Entidade>FormValues()} onSubmit={mutation.mutate}
         isSubmitting={mutation.isPending} apiError={apiError}
         onClearApiError={() => setApiError(null)} submitLabel="Salvar"
         onCancel={() => router.push('/<plural>')} />

// [id]/editar/page.tsx — carrega dados e passa como defaultValues
const { data } = useQuery({ queryKey: ['<plural>', id], queryFn: () => <entidade>Service.buscarPorId(id) })
// renderizar o <Entidade>Form so quando `data` chegar (defaultValues a partir de `data`),
// ou montar com defaults e usar resetWithDraft no useEffect (ver CLAUDE.md).
const mutation = useMutation({
  mutationFn: (values: <Entidade>FormValues) => <entidade>Service.atualizar(id, values),
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['<plural>'] })
    await queryClient.invalidateQueries({ queryKey: ['<plural>', id] })
    router.push('/<plural>')
  },
})
```

> **Nota de rota:** edicao navega para `/<plural>/${id}/editar` (padrao atual).
> O `/<plural>/${id}` sem `/editar` so existe em telas legadas.

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
- [ ] `<Entidade>Controller.java` (com wiring de auditoria — secao 5.1)
- [ ] DTOs: Criar/AtualizarRequest + Response (Money aninhado se houver — secao 5.2.1)
- [ ] `GlobalExceptionHandler.java`: adicionar SO o handler `<Entidade>NaoEncontradoException`
- [ ] Auditoria: `entityType` registrado no middleware (skill `add-entity-to-audit`)
- [ ] Se M:N por UUID: migration da tabela de juncao `<tabela>_tag` (secao 1.7)

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

> Se a entidade usar qualquer padrao da secao 10 (soft-delete, paginacao,
> calculo cross-context, maquina de estados, hierarquia, value object,
> operacao composta), o checklist acima ganha itens extras — ver a subsecao
> correspondente.

---

## 10. Padroes avancados (alem do CRUD simples)

As secoes 1-9 cobrem o CRUD backbone. Quando a entidade exige algo a mais,
combinar o backbone com um ou mais dos padroes abaixo. Cada um e codigo real
adaptado; manter a arquitetura (regra de negocio no domain, persistencia na
infra, orquestracao no use case).

### 10.1 Soft-delete (referencia: `transacao`)

Em vez de remover a linha, marca-se `deleted_at`. Buscas padrao filtram
`deleted_at IS NULL`. Use quando o registro precisa sobreviver para auditoria/
historico.

- **Domain** — campo `Instant deletedAt` (null = ativo) + helper:
  ```java
  private final Instant deletedAt;
  public boolean isDeleted() { return deletedAt != null; }
  ```
- **Repository (interface)** — `void softDelete(UUID id)` separado de `deletar`
  (delete fisico, so para testes). `buscarPorId` ja exclui soft-deleted.
- **JpaRepository** — queries explicitas filtrando `deletedAt`:
  ```java
  @Query("SELECT t FROM <Entidade>Entity t WHERE t.id = :id AND t.deletedAt IS NULL")
  Optional<<Entidade>Entity> findByIdAndNotDeleted(@Param("id") UUID id);

  @Transactional @Modifying
  @Query("UPDATE <Entidade>Entity t SET t.deletedAt = CURRENT_TIMESTAMP, "
       + "t.atualizadoEm = CURRENT_TIMESTAMP WHERE t.id = :id")
  void softDeleteById(@Param("id") UUID id);
  ```
- **Migration** — coluna `deleted_at TIMESTAMPTZ` (nullable, sem default).
- **Controller** — `@DeleteMapping` chama o use case de soft delete; continua
  retornando `204`.

### 10.2 Paginacao + filtros dinamicos (Specification) (referencia: `transacao`)

Para listagens de alto volume com filtros combinaveis e ordenacao no banco.
Plugа direto no `useListPage` do frontend (secao 7.3).

- **Domain — objeto de filtros** (`record FiltrosTransacao`): campos opcionais
  (null = sem filtro) + `List<FiltroGenerico>` para filtros campo:operador:valor.
  Os campos validos e operadores aceitos sao **regra de dominio** (enum
  `FiltroTransacaoCampo`), nao de infra.
- **Domain — repository** retorna `Page<>` e recebe campo de ordenacao **de
  dominio** (enum `OrdenacaoTransacao` + `DirecaoOrdenacao`), nunca um
  `org.springframework...Sort` cru:
  ```java
  Page<<Entidade>> listarComFiltrosOrdenado(
      Filtros<Entidade> filtros, int page, int size,
      Ordenacao<Entidade> ordenacao, DirecaoOrdenacao direcao);
  ```
- **Infra — JpaRepository** estende tambem `JpaSpecificationExecutor<<Entidade>Entity>`.
- **Infra — Specifications** (Criteria API tipada, sem concatenar string -> sem
  SQL injection). Sempre incluir `cb.isNull(root.get("deletedAt"))` quando houver
  soft-delete. Money e `@Embedded`: o numero vive em `root.get("valor").get("valor")`.
  Escapar curingas LIKE (`%`/`_`) em filtros string.
- **Controller** — `@Validated`; `@RequestParam ... @Min(0) int page`,
  `@Min(1) @Max(100) int size`; parseia `sort` (`campo:dir`) e `filtros`
  (`campo:operador:valor,...`, valor URI-encoded) e retorna `Page<Response>` via
  `resultado.map(Response::fromDomain)`.
- **Frontend** — service retorna `PageResponse<T>` (secao 7.2 variante paginada).

### 10.3 Agregacao via JPQL (constructor expression) (referencia: `transacao`)

Para totais/somatorios calculados no banco (ex: saldo, totais por conta). Definir
um `record` de dominio e projeta-lo com `SELECT new ...`:

```java
// domain: record TotaisTransacaoPorConta(BigDecimal receitas, BigDecimal despesas, ...)
@Query("""
    SELECT new com.laboratorio.financas.<contexto>.domain.Totais...(
        COALESCE(SUM(CASE WHEN t.tipo = ...RECEITA AND t.transferGroupId IS NULL
                          THEN t.valor.valor ELSE 0 END), 0),
        ...)
    FROM <Entidade>Entity t
    WHERE t.contaId = :contaId AND t.deletedAt IS NULL
    """)
Totais... calcularTotaisPorConta(@Param("contaId") UUID contaId);
```

### 10.4 Calculo cross-context (referencia: `orcamento`)

Um contexto pode **ler** outro injetando o repository **de dominio** do outro
contexto (nunca a Entity/JpaRepository). A regra divide-se em duas camadas — e a
separacao e o ponto arquitetural mais importante deste padrao:

- **Use case = orquestracao.** Carrega o agregado, agrega dados do outro contexto
  (a soma de transacoes e cross-context, logo e orquestracao) e delega a regra ao
  dominio. NAO contem limiares nem calculo de percentual.
- **Dominio = regra de negocio.** O calculo do percentual e a classificacao
  (limiares) vivem num metodo do agregado, que retorna um value object de
  resultado. Assim a regra e testavel em unit test puro (sem Spring/Mockito).

**Dominio — metodo no agregado + VO de resultado:**

```java
// <contexto>/domain/Progresso<Entidade>.java
public record Progresso<Entidade>(Money valorLimite, Money totalGasto,
                                  BigDecimal percentualUtilizado, StatusProgresso status) { }

// no agregado <Entidade> (regra de negocio aqui, nao no use case):
public Progresso<Entidade> avaliarProgresso(Money totalGasto) {
    if (valorLimite.valor().signum() == 0) {
        return new Progresso<Entidade>(valorLimite, totalGasto, BigDecimal.ZERO, StatusProgresso.EXCEDIDO);
    }
    BigDecimal percentual = totalGasto.valor()
            .multiply(BigDecimal.valueOf(100))
            .divide(valorLimite.valor(), 2, RoundingMode.HALF_UP);
    return new Progresso<Entidade>(valorLimite, totalGasto, percentual,
            StatusProgresso.classificar(percentual));
}

// limiares no enum (mesmo padrao da secao 10.7 — comportamento junto do dado):
public enum StatusProgresso {
    ABAIXO, ATENCAO, ATINGIDO, EXCEDIDO;
    public static StatusProgresso classificar(BigDecimal percentual) {
        if (percentual.compareTo(BigDecimal.valueOf(100)) > 0)  return EXCEDIDO;
        if (percentual.compareTo(BigDecimal.valueOf(100)) == 0) return ATINGIDO;
        if (percentual.compareTo(BigDecimal.valueOf(80)) >= 0)  return ATENCAO;
        return ABAIXO;
    }
}
```

**Use case — so orquestra:**

```java
@Component
public class CalcularProgressoDo<Entidade>UseCase {
  private final <Entidade>Repository <entidade>Repository;
  private final TransacaoRepository transacaoRepository; // repo de DOMINIO do outro contexto

  @Transactional(readOnly = true)
  public Progresso<Entidade> executar(UUID id) {
    var <entidade> = <entidade>Repository.buscarPorId(id).orElseThrow(...);
    var filtros = new FiltrosTransacao(/* mes, categoria, tipo DESPESA */);
    Money totalGasto = new Money(
        transacaoRepository.listarComFiltros(filtros, Pageable.unpaged())
            .getContent().stream().map(t -> t.getValor().valor())
            .reduce(BigDecimal.ZERO, BigDecimal::add),
        <entidade>.getValorLimite().moeda());
    return <entidade>.avaliarProgresso(totalGasto);   // regra delegada ao dominio
  }
}
```

> **Divida tecnica conhecida:** o `orcamento` atual mantem percentual+limiares
> dentro do `CalcularProgressoDoOrcamentoUseCase`. Isso e um atalho a refatorar —
> codigo novo deve seguir a separacao acima (regra no dominio, orquestracao no
> use case).

### 10.5 Maquina de estados no dominio (referencia: `meta`)

Entidade com `status` mutavel e transicoes validadas. O dominio rejeita transicao
ilegal com `IllegalStateException` — que o `GlobalExceptionHandler` **ja mapeia
globalmente para 400** (nao criar handler novo; ver "Infra compartilhada assumida").
O use case so orquestra (carrega, chama o metodo, salva).

```java
public void registrarDeposito(Money deposito) {
    if (status != StatusMeta.EM_ANDAMENTO)
        throw new IllegalStateException("Meta nao esta em andamento");
    // valida deposito (positivo, mesma moeda) ...
    this.valorAtual = new Money(valorAtual.valor().add(deposito.valor()), valorAtual.moeda());
    if (valorAtual.valor().compareTo(valorAlvo.valor()) >= 0)
        this.status = StatusMeta.CONCLUIDA;   // transicao automatica
    this.atualizadoEm = Instant.now();
}
public void cancelar() {
    if (status == StatusMeta.CONCLUIDA)
        throw new IllegalStateException("Meta ja concluida nao pode ser cancelada");
    this.status = StatusMeta.CANCELADA;
    this.atualizadoEm = Instant.now();
}
```

- **Use cases extras** alem do CRUD: um por transicao (`RegistrarDepositoEm<Entidade>UseCase`,
  `Cancelar<Entidade>UseCase`), cada um com seu `record Comando`.
- **Controller** — endpoints de acao: `POST /api/<plural>/{id}/depositos`,
  `POST /api/<plural>/{id}/cancelamento` (ou similar), nao apenas PUT generico.
- **Teste de dominio** — cobrir cada transicao valida E cada transicao ilegal.

### 10.6 Queries derivadas, hierarquia e visibilidade (referencia: `categoria`)

Quando ha mais buscas que `findByUserId`:

- **Derived queries** do Spring Data: `findByTipo(...)`, `existsByNomeAndUserId(...)`.
- **Hierarquia (self-FK)** — campo `UUID categoriaPaiId` (mesmo padrao da secao 1.6,
  apontando para a propria tabela). Migration: FK auto-referente
  `REFERENCES categoria(id)`. Queries `findByCategoriaPaiIdIsNull()` (raizes) e
  `findByCategoriaPaiId(id)` (filhos).
- **Visibilidade multi-tenant** — registros `system` (globais) + do usuario:
  ```java
  @Query("SELECT c FROM <Entidade>Entity c WHERE c.system = true OR c.userId = :userId")
  List<<Entidade>Entity> findVisiveisPara(@Param("userId") UUID userId);
  ```
- **Unicidade condicional** — `existsByNomeAndUserId` + `existsByNomeAndSystemTrue`,
  validada no use case com excecao de dominio (`<Entidade>JaExisteException` -> 409).

### 10.7 Value object / enum com comportamento (referencia: `lancamentorecorrente.Periodicidade`)

Quando uma escolha (enum) carrega regra. Em vez de `switch` espalhado, o
comportamento vive no proprio enum (constant-specific methods):

```java
public enum Periodicidade {
    MENSAL   { public LocalDate calcularProxima(LocalDate atual) { return atual.plusMonths(1); } },
    SEMANAL  { public LocalDate calcularProxima(LocalDate atual) { return atual.plusWeeks(1); } },
    // ...
    ;
    public abstract LocalDate calcularProxima(LocalDate atual);
}
```

A entidade delega: `this.proximaOcorrencia = periodicidade.calcularProxima(proximaOcorrencia)`.
Persistir o enum com `@Enumerated(EnumType.STRING)` (secao 2.2).

### 10.8 Operacao composta / registros vinculados (referencia: `transacao` transferencia)

Quando uma acao do usuario cria **mais de um registro** ligados entre si (ex:
transferencia = 1 DESPESA na origem + 1 RECEITA no destino, com IDs cruzados).
Usar **static factory** no dominio retornando um `record` com as partes:

```java
public record TransferenciaPar(Transacao despesa, Transacao receita) { }

public static TransferenciaPar criarParTransferencia(
        UUID userId, Money valor, UUID contaOrigemId, UUID contaDestinoId,
        LocalDate data, String descricao, UUID categoriaId) {
    if (contaOrigemId.equals(contaDestinoId))
        throw new IllegalArgumentException("origem nao pode ser igual ao destino");
    UUID groupId = UUID.randomUUID();
    UUID idDespesa = UUID.randomUUID(), idReceita = UUID.randomUUID();
    // despesa.transferPairId = idReceita; receita.transferPairId = idDespesa;
    // ambos compartilham transferGroupId = groupId
    return new TransferenciaPar(despesa, receita);
}
```

- O use case valida as referencias (contas) e **salva as duas partes na mesma
  transacao** (`@Transactional`).
- Agregacoes devem tratar os pares (ex: excluir transferencias do total de
  receitas/despesas "reais" — ver `calcularTotaisPorConta`, secao 10.3).
- Soft-delete de uma parte normalmente implica a outra (decisao de dominio).
