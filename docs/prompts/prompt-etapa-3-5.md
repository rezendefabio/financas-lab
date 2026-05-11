# Prompt — Etapa 3.5: Bounded Context `categoria` (Etapa Única)

## Contexto

A Etapa 3.4 foi concluída e fechada via PR #33. Bounded context `conta` está vivo ponta a ponta: domain + infra + application + interfaces, com endpoints REST funcionando, `ProblemDetail` retornando, persistência validada destrutivamente em ambiente real.

Esta etapa implementa o **segundo bounded context** do projeto: `categoria`. Faz isso em **etapa única** porque o template de bounded context já está consolidado pelas 3.1-3.4. O objetivo da 3.5 é validar exatamente isso — que o segundo bounded context custa significativamente menos que o primeiro, evidência crítica de que o template é replicável.

Comparação esperada:

| Aspecto | `conta` (3.1-3.4) | `categoria` (3.5) |
|---|---|---|
| Etapas | 4 + 1 fix (3.1, 3.2, 3.3, 3.3.1, 3.4) | 1 |
| Valor agregado novo (`Money`, `MapStruct`, `ProblemDetail`, etc) | Muito | Quase zero |
| Linhas de código | ~2140 adicionadas | Estimado: ~1000 |
| Decisões pendentes | Várias | Mínimas |
| Tempo previsível | Variável | Replicável |

Se a 3.5 sair em ritmo proporcionalmente menor que `conta`, **a fábrica está validada para `transacao` e bounded contexts subsequentes**. Esse é o gate real, não o número de testes.

## Escopo decidido (calibrado com operador antes da redação)

### Modelagem de `Categoria`

- **`id`**: `UUID` gerado pela aplicação no construtor (mesmo padrão de `Conta`).
- **`nome`**: `String`, obrigatório. Validação: não-nulo, não-blank após trim, mínimo 1 char não-blank, máximo 100 chars.
- **`tipo`**: enum `TipoCategoria` com valores: `RECEITA`, `DESPESA`. Faz parte da regra de negócio — categoria de receita não classifica despesa e vice-versa.
- **`criadoEm`** / **`atualizadoEm`**: `Instant`. Auditoria mínima, mesmo padrão de `Conta`.

### O que está fora desta etapa (porta aberta)

- **Hierarquia (pai/filho).** `visao.md` MVP prescreve hierarquia em 1 nível, mas implementar agora é especulação sem caso de uso. Quando a UI mostrar árvore, entra. Por enquanto categoria é flat.
- **Seed inicial.** Decisão de UX (quais categorias padrão?) deve aparecer junto com a UI, não preventivamente no backend.
- **Soft delete via `desativar()`.** `Conta` tem soft delete porque desativá-la não pode quebrar transações históricas. Para `categoria` o mesmo argumento se aplica, **mas só quando `transacao` existir referenciando categoria**. Por enquanto, sem FK existente, hard delete via `DELETE /api/categorias/{id}` é seguro. Quando `transacao` aparecer com FK pra `categoria`, esta decisão é revisitada — possivelmente migration adicionando coluna `ativa` + endpoint de desativação. **Esta etapa não cria coluna `ativa` em `categoria`.**
- **Validação de unicidade de nome dentro do tipo.** Não há requisito; duas categorias `RECEITA` chamadas "Salário" são permitidas (são entidades distintas com ids distintos). Mesma decisão tomada em `conta`.

### Decisões de design

- **`Categoria` é classe imutável final.** Mesmo padrão de `Conta` — class explícita, getters, `equals`/`hashCode` por id, `toString` enxuto. Não record, porque tem vários campos com validação coordenada.
- **Construtores: dois.**
  1. **Construtor "novo"**: `Categoria(String nome, TipoCategoria tipo)`. Gera `id`, define `criadoEm = atualizadoEm = Instant.now()`.
  2. **Construtor "reconstrução"** (para repository hidratar): `Categoria(UUID id, String nome, TipoCategoria tipo, Instant criadoEm, Instant atualizadoEm)`. Recebe todos os campos.
- **Igualdade por `id`** (entidade, não VO).
- **Sem método `desativar()`** — não há campo `ativa`.
- **Sem método `renomear()` / `alterarTipo()`** — sem caso de uso na 3.5. Mutações futuras entram quando UC justificar.
- **`IllegalArgumentException`/`NullPointerException`** do JDK para validações. Mesmo padrão de `Conta`.

### Endpoints REST

```
POST   /api/categorias                          → 201, retorna CategoriaResponse
GET    /api/categorias                          → 200, lista. Query param ?tipo=RECEITA|DESPESA filtra; sem param retorna todas.
GET    /api/categorias/{id}                     → 200 ou 404
DELETE /api/categorias/{id}                     → 204 (hard delete, não soft — ver decisão acima)
```

**Diferenças em relação a `/api/contas`:**

- `DELETE` é **hard delete** (chama `repository.deletar(id)`). Endpoint exposto, mas comportamento sob a capa diferente. **`CategoriaResponse` retornada não tem campo `ativa`** (não existe em `Categoria`).
- Filtro do `GET` é por `?tipo=RECEITA|DESPESA` (enum), não por `?ativa=true` (boolean).

### Use cases (4 + diferenças)

Em `categoria/application/`:

1. **`CriarCategoriaUseCase`** — recebe `Comando(nome, tipo)` (record interno). Constrói `Categoria` e salva.
2. **`ListarCategoriasUseCase`** — método `executar(TipoCategoria tipo)`. Se `tipo` é null → `repository.listarTodas()`. Senão → `repository.listarPorTipo(tipo)`.
3. **`BuscarCategoriaPorIdUseCase`** — recebe `UUID`, retorna `Categoria`. Lança `CategoriaNaoEncontradaException` quando vazio.
4. **`DeletarCategoriaUseCase`** — recebe `UUID`, valida existência, deleta. **Importante:** valida que existe antes de deletar (busca + lança 404 se não acha) para retornar 404 em vez de "204 silencioso" quando id não existe. Concorda com REST estrito.

### Repository

Interface `CategoriaRepository` em `categoria/domain/`. Métodos:

- `Categoria salvar(Categoria c)`
- `Optional<Categoria> buscarPorId(UUID id)`
- `List<Categoria> listarTodas()`
- `List<Categoria> listarPorTipo(TipoCategoria tipo)`
- `void deletar(UUID id)`

`CategoriaJpaRepository` (Spring Data) em `categoria/infrastructure/persistence/`. Método derivado: `findByTipo(TipoCategoria tipo)`.

`CategoriaRepositoryImpl` (`@Component`) delega a JpaRepository, usa `CategoriaMapper`.

### Persistência

- **Tabela `categoria`** com migration `V3__cria_tabela_categoria.sql`:
  ```sql
  CREATE TABLE categoria (
      id            UUID            PRIMARY KEY,
      nome          VARCHAR(100)    NOT NULL,
      tipo          VARCHAR(20)     NOT NULL,
      criado_em     TIMESTAMPTZ     NOT NULL,
      atualizado_em TIMESTAMPTZ     NOT NULL
  );

  CREATE INDEX idx_categoria_tipo ON categoria (tipo);
  ```
- **`CategoriaEntity`** com `@Entity`, mesmo padrão de `ContaEntity`.
- **Sem `@Embeddable`** — não há `Money` em `Categoria`.
- **`CategoriaMapper`** com `@Mapper(componentModel = "spring")`. Inteiramente em métodos `default` (mesmo motivo de `ContaMapper` — construtor de reconstrução não-inferível).

### DTOs

- **`CriarCategoriaRequest`** (record com Bean Validation):
  - `String nome` — `@NotBlank`, `@Size(max = 100)`
  - `TipoCategoria tipo` — `@NotNull`

- **`CategoriaResponse`** (record):
  - `UUID id`, `String nome`, `TipoCategoria tipo`, `Instant criadoEm`, `Instant atualizadoEm`
  - Método estático `fromDomain(Categoria c)`.

### Controller

`CategoriaController` em `categoria/interfaces/`. 4 endpoints. Mesmo padrão estrutural de `ContaController`.

Filtro `?tipo` é `TipoCategoria` opcional (enum, não Boolean):
```java
@GetMapping
public List<CategoriaResponse> listar(@RequestParam(name = "tipo", required = false) TipoCategoria tipo) {
    List<Categoria> categorias = listarUseCase.executar(tipo);
    return categorias.stream().map(CategoriaResponse::fromDomain).toList();
}
```

Spring converte string → enum automaticamente. `?tipo=XYZ` (valor inválido) lança `MethodArgumentTypeMismatchException`, capturada pelo handler global como 400.

### Exception handler

**Reusar `GlobalExceptionHandler` existente.** Adicionar **um único handler** novo:

```java
@ExceptionHandler(CategoriaNaoEncontradaException.class)
public ProblemDetail handleCategoriaNaoEncontrada(CategoriaNaoEncontradaException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setTitle("Not Found");
    problem.setDetail(ex.getMessage());
    problem.setProperty("id", ex.getId().toString());
    return problem;
}
```

Demais handlers (validação, JSON malformado, type mismatch, IllegalArgumentException, catch-all) já cobrem `categoria` sem alteração.

### Security

Adicionar `/api/categorias/**` à whitelist temporária do `SecurityConfig`, **com mesmo TODO de Auth**:

```java
.requestMatchers(
        "/api/healthcheck",
        "/api/contas/**",       // TODO Etapa Auth (futura): remover quando JWT estiver pronto
        "/api/categorias/**",   // TODO Etapa Auth (futura): remover quando JWT estiver pronto
        "/actuator/health",
        ...
).permitAll()
```

### CategoriaNaoEncontradaException

`src/main/java/com/laboratorio/financas/categoria/domain/CategoriaNaoEncontradaException.java`

Mesmo padrão de `ContaNaoEncontradaException`: `RuntimeException`, expõe `getId()`.

### Localização dos arquivos

```
src/main/java/com/laboratorio/financas/categoria/
├── application/
│   ├── BuscarCategoriaPorIdUseCase.java
│   ├── CriarCategoriaUseCase.java
│   ├── DeletarCategoriaUseCase.java
│   └── ListarCategoriasUseCase.java
├── domain/
│   ├── Categoria.java
│   ├── CategoriaNaoEncontradaException.java
│   ├── CategoriaRepository.java
│   └── TipoCategoria.java
├── infrastructure/persistence/
│   ├── CategoriaEntity.java
│   ├── CategoriaJpaRepository.java
│   ├── CategoriaMapper.java
│   └── CategoriaRepositoryImpl.java
└── interfaces/
    ├── CategoriaController.java
    └── dto/
        ├── CategoriaResponse.java
        └── CriarCategoriaRequest.java

src/main/resources/db/migration/
└── V3__cria_tabela_categoria.sql

src/test/java/com/laboratorio/financas/categoria/
├── application/
│   ├── BuscarCategoriaPorIdUseCaseTest.java
│   ├── CriarCategoriaUseCaseTest.java
│   ├── DeletarCategoriaUseCaseTest.java
│   └── ListarCategoriasUseCaseTest.java
├── domain/
│   └── CategoriaTest.java
├── infrastructure/persistence/
│   └── CategoriaRepositoryImplTest.java
└── interfaces/
    └── CategoriaControllerTest.java
```

Alterações em arquivos existentes:
- `shared/infrastructure/web/GlobalExceptionHandler.java`: novo handler
- `shared/infrastructure/security/SecurityConfig.java`: nova entrada na whitelist

### JaCoCo

Todos os thresholds já estão ativos desde 3.4. Sem alteração no `pom.xml`. Cobertura esperada de cada classe nova: alta (mappers podem ficar em ~80%, restante em ~95-100%).

### Testes

**Domain (CategoriaTest)** — ~15 testes cobrindo construtor "novo", construtor "reconstrução", validações, igualdade por id, toString. Mesmo padrão de `ContaTest`.

**Use cases** — ~5 testes cada com Mockito programático.

**Repository (CategoriaRepositoryImplTest)** — ~6 testes de integração com Testcontainers cobrindo salvar, buscar, listar todas, listar por tipo, deletar, deletar inexistente.

**Controller (CategoriaControllerTest)** — ~10 testes e2e via MockMvc + Testcontainers cobrindo:
1. POST válido → 201
2. POST nome blank → 400
3. POST tipo null → 400
4. POST tipo inválido (string desconhecida) → 400
5. GET listar todas (sem param)
6. GET listar com `?tipo=RECEITA`
7. GET listar com `?tipo=XYZ` (inválido) → 400
8. GET por id existente → 200
9. GET por id inexistente → 404
10. DELETE existente → 204
11. DELETE inexistente → 404
12. Ciclo completo: POST → GET → DELETE → GET (404)

**Total esperado: ~50 testes.** Volume similar à 3.4.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra commit do squash da Etapa 3.4 com referência a PR #33
- `docs/prompt-etapa-3-5.md` presente como untracked
- Working tree limpo
- Pacote `com.laboratorio.financas.categoria` **não existe ainda**
- Migration mais alta no `db/migration/` é `V2__cria_tabela_conta.sql`

Validar com:

```bash
git status
git log --oneline -1
ls docs/prompt-etapa-3-5.md
ls -la src/main/java/com/laboratorio/financas/categoria/ 2>/dev/null && echo "ATENCAO" || echo "OK"
ls src/main/resources/db/migration/
```

Se algum item divergir, parar e reportar.

## Tarefas

### Tarefa 1 — Validar pré-requisitos

```bash
git status
git log --oneline -3
ls docs/prompt-etapa-3-5.md
ls -la src/main/java/com/laboratorio/financas/categoria/ 2>/dev/null && echo "ATENCAO" || echo "OK"
ls -la src/main/java/com/laboratorio/financas/conta/
ls src/main/resources/db/migration/
ls src/main/java/com/laboratorio/financas/shared/infrastructure/security/SecurityConfig.java
ls src/main/java/com/laboratorio/financas/shared/infrastructure/web/GlobalExceptionHandler.java
```

Esperado:
- Working tree limpo, exceto `docs/prompt-etapa-3-5.md` untracked
- `categoria/` **não existe**
- `conta/` existe completo (4 subpastas)
- Migrações: `V1__schema_inicial.sql`, `V2__cria_tabela_conta.sql`
- `SecurityConfig.java` em `shared/infrastructure/security/` (caminho correto, confirmado na 3.4)
- `GlobalExceptionHandler.java` em `shared/infrastructure/web/`

### Tarefa 2 — Criar branch de trabalho

```bash
git checkout -b feat/categoria-completo
```

### Tarefa 3 — Criar domain

**Antes de escrever, ler `Conta.java` e `TipoConta.java` em disco** para replicar fielmente o padrão estabelecido (não inferir de memória — código vivo é a fonte). Adaptar para `Categoria`/`TipoCategoria`.

**3a.** `src/main/java/com/laboratorio/financas/categoria/domain/TipoCategoria.java`

```java
package com.laboratorio.financas.categoria.domain;

public enum TipoCategoria {
    RECEITA,
    DESPESA
}
```

**3b.** `src/main/java/com/laboratorio/financas/categoria/domain/Categoria.java`

Class final imutável seguindo padrão de `Conta`. Estrutura:

```java
package com.laboratorio.financas.categoria.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Categoria {

    private static final int NOME_MAX_LENGTH = 100;

    private final UUID id;
    private final String nome;
    private final TipoCategoria tipo;
    private final Instant criadoEm;
    private final Instant atualizadoEm;

    /**
     * Construtor para criar nova Categoria. Gera id, define criadoEm=atualizadoEm=now.
     */
    public Categoria(String nome, TipoCategoria tipo) {
        this(
                UUID.randomUUID(),
                nome,
                tipo,
                Instant.now(),
                null
        );
    }

    /**
     * Construtor de reconstrucao. Usado pelo repository para hidratar instancia persistida.
     */
    public Categoria(
            UUID id,
            String nome,
            TipoCategoria tipo,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarNome(nome);

        this.id = id;
        this.nome = nome.trim();
        this.tipo = tipo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = (atualizadoEm != null) ? atualizadoEm : criadoEm;
    }

    private static void validarNome(String nome) {
        Objects.requireNonNull(nome, "nome nao pode ser nulo");
        String trimmed = nome.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("nome nao pode ser vazio");
        }
        if (trimmed.length() > NOME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "nome nao pode ter mais de " + NOME_MAX_LENGTH + " caracteres"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public TipoCategoria getTipo() {
        return tipo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Categoria other)) {
            return false;
        }
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Categoria{id=" + id + ", nome='" + nome + "', tipo=" + tipo + "}";
    }
}
```

**Notas:**
- Sem método `desativar()` (Categoria não tem `ativa`).
- Indentação Checkstyle: 16 espaços nas continuações, getters em bloco multi-linha, chaves obrigatórias.
- Sem acentos.
- Encoding UTF-8 sem BOM.

**3c.** `src/main/java/com/laboratorio/financas/categoria/domain/CategoriaNaoEncontradaException.java`

Espelho de `ContaNaoEncontradaException`:

```java
package com.laboratorio.financas.categoria.domain;

import java.util.UUID;

public class CategoriaNaoEncontradaException extends RuntimeException {

    private final UUID id;

    public CategoriaNaoEncontradaException(UUID id) {
        super("Categoria nao encontrada: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
```

**3d.** `src/main/java/com/laboratorio/financas/categoria/domain/CategoriaRepository.java`

```java
package com.laboratorio.financas.categoria.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoriaRepository {

    Categoria salvar(Categoria categoria);

    Optional<Categoria> buscarPorId(UUID id);

    List<Categoria> listarTodas();

    List<Categoria> listarPorTipo(TipoCategoria tipo);

    void deletar(UUID id);
}
```

### Tarefa 4 — Criar testes do domain

`src/test/java/com/laboratorio/financas/categoria/domain/CategoriaTest.java`

**Cenários obrigatórios** (~15 testes):

Construtor "novo":
1. Constrói com argumentos válidos: id gerado não-nulo, criadoEm em janela, atualizadoEm == criadoEm
2. Trima nome com espaços nas pontas
3. Lança `NullPointerException` quando nome é null
4. Lança `IllegalArgumentException` quando nome é blank
5. Lança `IllegalArgumentException` quando nome > 100 chars
6. Aceita nome com 100 chars
7. Lança `NullPointerException` quando tipo é null
8. Aceita ambos os tipos (RECEITA, DESPESA)
9. Dois Categorias têm ids diferentes (sanity de UUID)

Construtor "reconstrução":
10. Reconstrói com todos os campos preservando valores
11. Lança `NullPointerException` quando id null
12. Lança `NullPointerException` quando criadoEm null
13. Aceita atualizadoEm null (defaulta para criadoEm)

Igualdade e toString:
14. Duas categorias com mesmo id são iguais (mesmo com demais campos diferentes)
15. `equals` retorna false para null e tipos diferentes
16. `toString` contém id, nome, tipo (não contém timestamps)

**Naming camelCase puro**, sem underscore. Indentação Checkstyle alinhada.

### Tarefa 5 — Criar use cases

Quatro arquivos em `categoria/application/`. **Antes de escrever, ler os 4 use cases de `conta/application/`** para replicar padrão fielmente.

**5a.** `CriarCategoriaUseCase.java`

```java
package com.laboratorio.financas.categoria.application;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarCategoriaUseCase {

    private final CategoriaRepository repository;

    public CriarCategoriaUseCase(CategoriaRepository repository) {
        this.repository = repository;
    }

    public record Comando(String nome, TipoCategoria tipo) { }

    @Transactional
    public Categoria executar(Comando comando) {
        Categoria nova = new Categoria(comando.nome(), comando.tipo());
        return repository.salvar(nova);
    }
}
```

**5b.** `ListarCategoriasUseCase.java`

```java
package com.laboratorio.financas.categoria.application;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarCategoriasUseCase {

    private final CategoriaRepository repository;

    public ListarCategoriasUseCase(CategoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Categoria> executar(TipoCategoria tipo) {
        if (tipo == null) {
            return repository.listarTodas();
        }
        return repository.listarPorTipo(tipo);
    }
}
```

**5c.** `BuscarCategoriaPorIdUseCase.java`

```java
package com.laboratorio.financas.categoria.application;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.CategoriaNaoEncontradaException;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BuscarCategoriaPorIdUseCase {

    private final CategoriaRepository repository;

    public BuscarCategoriaPorIdUseCase(CategoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Categoria executar(UUID id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new CategoriaNaoEncontradaException(id));
    }
}
```

**5d.** `DeletarCategoriaUseCase.java`

Importante: valida existência antes de deletar (lança 404 se não acha).

```java
package com.laboratorio.financas.categoria.application;

import com.laboratorio.financas.categoria.domain.CategoriaNaoEncontradaException;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeletarCategoriaUseCase {

    private final CategoriaRepository repository;

    public DeletarCategoriaUseCase(CategoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id) {
        if (repository.buscarPorId(id).isEmpty()) {
            throw new CategoriaNaoEncontradaException(id);
        }
        repository.deletar(id);
    }
}
```

### Tarefa 6 — Criar testes dos use cases

Quatro arquivos em `test/.../categoria/application/`, ~5 testes cada (~20 total). Mockito programático (sem `@ExtendWith`). Padrão de `conta/application/`.

### Tarefa 7 — Criar infra de persistência

**Antes de escrever, ler `ContaEntity.java`, `ContaJpaRepository.java`, `ContaRepositoryImpl.java`, `ContaMapper.java` em disco.** Replicar padrão.

**7a.** `CategoriaEntity.java` em `categoria/infrastructure/persistence/`

```java
package com.laboratorio.financas.categoria.infrastructure.persistence;

import com.laboratorio.financas.categoria.domain.TipoCategoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categoria")
public class CategoriaEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoCategoria tipo;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected CategoriaEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public CategoriaEntity(
            UUID id,
            String nome,
            TipoCategoria tipo,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this.id = id;
        this.nome = nome;
        this.tipo = tipo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public TipoCategoria getTipo() {
        return tipo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
```

**7b.** `CategoriaMapper.java`

```java
package com.laboratorio.financas.categoria.infrastructure.persistence;

import com.laboratorio.financas.categoria.domain.Categoria;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    default CategoriaEntity toEntity(Categoria categoria) {
        if (categoria == null) {
            return null;
        }
        return new CategoriaEntity(
                categoria.getId(),
                categoria.getNome(),
                categoria.getTipo(),
                categoria.getCriadoEm(),
                categoria.getAtualizadoEm()
        );
    }

    default Categoria toDomain(CategoriaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Categoria(
                entity.getId(),
                entity.getNome(),
                entity.getTipo(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm()
        );
    }
}
```

**7c.** `CategoriaJpaRepository.java`

```java
package com.laboratorio.financas.categoria.infrastructure.persistence;

import com.laboratorio.financas.categoria.domain.TipoCategoria;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, UUID> {

    List<CategoriaEntity> findByTipo(TipoCategoria tipo);
}
```

**7d.** `CategoriaRepositoryImpl.java`

```java
package com.laboratorio.financas.categoria.infrastructure.persistence;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CategoriaRepositoryImpl implements CategoriaRepository {

    private final CategoriaJpaRepository jpaRepository;
    private final CategoriaMapper mapper;

    public CategoriaRepositoryImpl(CategoriaJpaRepository jpaRepository, CategoriaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Categoria salvar(Categoria categoria) {
        CategoriaEntity entity = mapper.toEntity(categoria);
        CategoriaEntity salva = jpaRepository.save(entity);
        return mapper.toDomain(salva);
    }

    @Override
    public Optional<Categoria> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Categoria> listarTodas() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Categoria> listarPorTipo(TipoCategoria tipo) {
        return jpaRepository.findByTipo(tipo).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
```

### Tarefa 8 — Criar migration V3

`src/main/resources/db/migration/V3__cria_tabela_categoria.sql`

```sql
-- V3: cria tabela categoria
-- Bounded context: categoria
-- Etapa 3.5 da Camada 2

CREATE TABLE categoria (
    id            UUID            PRIMARY KEY,
    nome          VARCHAR(100)    NOT NULL,
    tipo          VARCHAR(20)     NOT NULL,
    criado_em     TIMESTAMPTZ     NOT NULL,
    atualizado_em TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_categoria_tipo ON categoria (tipo);
```

### Tarefa 9 — Criar teste de integração de repository

`test/.../categoria/infrastructure/persistence/CategoriaRepositoryImplTest.java`

Padrão de `ContaRepositoryImplTest`. ~6 testes cobrindo:
1. Salvar persiste e retorna instância equivalente
2. BuscarPorId retorna quando existe
3. BuscarPorId retorna vazio quando não existe
4. ListarTodas retorna todas as categorias salvas
5. ListarPorTipo filtra corretamente (cria 2 RECEITA + 1 DESPESA, filtra por RECEITA, espera 2)
6. Deletar remove do banco
7. Deletar id inexistente não lança (Spring Data ignora silenciosamente — confirmar comportamento; se lançar, ajustar teste pra esperar exceção)

### Tarefa 10 — Criar DTOs

`categoria/interfaces/dto/CriarCategoriaRequest.java`

```java
package com.laboratorio.financas.categoria.interfaces.dto;

import com.laboratorio.financas.categoria.domain.TipoCategoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarCategoriaRequest(
        @NotBlank
        @Size(max = 100)
        String nome,

        @NotNull
        TipoCategoria tipo
) { }
```

`categoria/interfaces/dto/CategoriaResponse.java`

```java
package com.laboratorio.financas.categoria.interfaces.dto;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import java.time.Instant;
import java.util.UUID;

public record CategoriaResponse(
        UUID id,
        String nome,
        TipoCategoria tipo,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static CategoriaResponse fromDomain(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getTipo(),
                categoria.getCriadoEm(),
                categoria.getAtualizadoEm()
        );
    }
}
```

### Tarefa 11 — Criar `CategoriaController`

`categoria/interfaces/CategoriaController.java`

```java
package com.laboratorio.financas.categoria.interfaces;

import com.laboratorio.financas.categoria.application.BuscarCategoriaPorIdUseCase;
import com.laboratorio.financas.categoria.application.CriarCategoriaUseCase;
import com.laboratorio.financas.categoria.application.DeletarCategoriaUseCase;
import com.laboratorio.financas.categoria.application.ListarCategoriasUseCase;
import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import com.laboratorio.financas.categoria.interfaces.dto.CategoriaResponse;
import com.laboratorio.financas.categoria.interfaces.dto.CriarCategoriaRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CriarCategoriaUseCase criarCategoriaUseCase;
    private final ListarCategoriasUseCase listarCategoriasUseCase;
    private final BuscarCategoriaPorIdUseCase buscarCategoriaPorIdUseCase;
    private final DeletarCategoriaUseCase deletarCategoriaUseCase;

    public CategoriaController(
            CriarCategoriaUseCase criarCategoriaUseCase,
            ListarCategoriasUseCase listarCategoriasUseCase,
            BuscarCategoriaPorIdUseCase buscarCategoriaPorIdUseCase,
            DeletarCategoriaUseCase deletarCategoriaUseCase
    ) {
        this.criarCategoriaUseCase = criarCategoriaUseCase;
        this.listarCategoriasUseCase = listarCategoriasUseCase;
        this.buscarCategoriaPorIdUseCase = buscarCategoriaPorIdUseCase;
        this.deletarCategoriaUseCase = deletarCategoriaUseCase;
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CriarCategoriaRequest request) {
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando(
                request.nome(),
                request.tipo()
        );
        Categoria criada = criarCategoriaUseCase.executar(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoriaResponse.fromDomain(criada));
    }

    @GetMapping
    public List<CategoriaResponse> listar(@RequestParam(name = "tipo", required = false) TipoCategoria tipo) {
        List<Categoria> categorias = listarCategoriasUseCase.executar(tipo);
        return categorias.stream().map(CategoriaResponse::fromDomain).toList();
    }

    @GetMapping("/{id}")
    public CategoriaResponse buscar(@PathVariable UUID id) {
        Categoria categoria = buscarCategoriaPorIdUseCase.executar(id);
        return CategoriaResponse.fromDomain(categoria);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id) {
        deletarCategoriaUseCase.executar(id);
    }
}
```

### Tarefa 12 — Adicionar handler ao `GlobalExceptionHandler`

Editar `src/main/java/com/laboratorio/financas/shared/infrastructure/web/GlobalExceptionHandler.java` adicionando **um único método novo** (e o import correspondente):

```java
import com.laboratorio.financas.categoria.domain.CategoriaNaoEncontradaException;
```

```java
@ExceptionHandler(CategoriaNaoEncontradaException.class)
public ProblemDetail handleCategoriaNaoEncontrada(CategoriaNaoEncontradaException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setTitle("Not Found");
    problem.setDetail(ex.getMessage());
    problem.setProperty("id", ex.getId().toString());
    return problem;
}
```

Inserir abaixo do handler de `ContaNaoEncontradaException` (mantém estrutura e proximidade visual). Não tocar em nenhum outro método.

### Tarefa 13 — Atualizar `SecurityConfig`

Editar `src/main/java/com/laboratorio/financas/shared/infrastructure/security/SecurityConfig.java`. Localizar a lista de `requestMatchers(...).permitAll()` e **adicionar `/api/categorias/**`** com TODO obrigatório.

Posicionar logo após `/api/contas/**`, mantendo padrão visual.

### Tarefa 14 — Criar `CategoriaControllerTest` (e2e)

`test/.../categoria/interfaces/CategoriaControllerTest.java`

Padrão de `ContaControllerTest`. ~12 testes conforme listado em "Testes" acima.

### Tarefa 15 — Validar localmente

```bash
.\mvnw.cmd compile

# Rodar testes da etapa primeiro (rápido):
.\mvnw.cmd test -Dtest='Categoria*Test'

# Build completo:
.\mvnw.cmd verify
```

**Esperado:**
- BUILD SUCCESS
- ~50 novos testes + os existentes (~166 total)
- Checkstyle: 0 violações
- SpotBugs: 0 issues
- JaCoCo: todos os thresholds atendidos
- Migration V3 aplicada nos testes de integração via Testcontainers (logs do Flyway visíveis)

### Tarefa 16 — Atualizar `docs/decisoes.md`

Adicionar entrada no **histórico**:

```markdown
- **2026-05-09** — Etapa 3.5 concluída: bounded context `categoria` em **etapa única**. Domain (`Categoria`, `TipoCategoria`, `CategoriaNaoEncontradaException`, `CategoriaRepository`), infra (`CategoriaEntity`, `CategoriaMapper`, `CategoriaJpaRepository`, `CategoriaRepositoryImpl`, `V3__cria_tabela_categoria.sql`), application (4 use cases), interfaces (`CategoriaController`, 2 DTOs), handler reusado (+1 entry), whitelist atualizada. Sem hierarquia, sem seed, sem soft delete (decisões adiadas até justificarem). Mergeado via PR #XX.
```

**Não adicionar nova seção em "Padrões aplicados".** `categoria` segue 100% os padrões já estabelecidos por `conta`. O fato de não adicionar nada novo aqui **é a evidência de que o template é replicável** — esse é o ponto da etapa.

### Tarefa 17 — Atualizar `docs/progresso.md`

**17a.** Atualizar "Última atualização": `2026-05-09 (Etapa 3.5 — categoria)`.

**17b.** Marcar critério da Camada 2: `[x] Bounded context categoria no mesmo padrão`.

**17c.** Adicionar seção **"Lições da Etapa 3.5"** logo antes de "Lições da Etapa 3.4". A lição central da etapa é metalinguística:

```markdown
## Lições da Etapa 3.5

### Candidatos a hook (automatizar em etapas futuras)

(Preencher com lições reais — ou `(Nenhum novo nesta etapa.)` se nada surgir.)

### Lições de ambiente

(Preencher com lições reais — ou `(Nenhum novo nesta etapa.)` se nada surgir.)

### Sobre a etapa em si (template replicável validado)

(Preencher com observações sobre tempo total, número de zonas limítrofes encontradas, qualidade do template estabelecido por `conta`. Esta seção existe porque a 3.5 é o teste real de "o template do bounded context é replicável" — e merece reflexão própria. Não inventar dados; reportar o real.)
```

**17d.** Adicionar entrada no histórico:

```markdown
- **2026-05-09** — Etapa 3.5 concluída: bounded context `categoria` em etapa única. ~50 testes novos. Template estabelecido por `conta` validado como replicável. Mergeado via PR #XX.
```

### Tarefa 18 — Versionar este próprio prompt

Confirmar `docs/prompt-etapa-3-5.md` em disco e incluir no commit de docs.

### Tarefa 19 — Validação final antes de commitar

```bash
# Encoding sem BOM em todos os arquivos novos:
find src/main/java/com/laboratorio/financas/categoria -name "*.java" -exec sh -c 'echo "$1:"; xxd "$1" | head -1' _ {} \;
find src/test/java/com/laboratorio/financas/categoria -name "*.java" -exec sh -c 'echo "$1:"; xxd "$1" | head -1' _ {} \;
xxd src/main/resources/db/migration/V3__cria_tabela_categoria.sql | head -1

.\mvnw.cmd verify

git status
git log --oneline -10
```

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Permitidos:
   - Pasta nova `categoria/` completa (4 subpastas em `main/`, 4 subpastas em `test/`)
   - `db/migration/V3__cria_tabela_categoria.sql` (criação)
   - `shared/infrastructure/web/GlobalExceptionHandler.java` (adicionar 1 handler + 1 import — sem outras alterações)
   - `shared/infrastructure/security/SecurityConfig.java` (adicionar 1 entrada de whitelist com TODO)
   - `docs/decisoes.md` (apenas histórico)
   - `docs/progresso.md` (lições + critério + histórico)
   - `docs/prompt-etapa-3-5.md` (este arquivo, versionar)

2. **Não tocar em `conta/`.** Bounded context `conta` está pronto e estável. Não modificar nem por "harmonização".

3. **Não tocar em `pom.xml`.** JaCoCo já tem todos os thresholds ativos.

4. **Não tocar em `application*.yml`, `docker-compose.yml`, `scripts/*.ps1`.**

5. **Não criar hierarquia (`parent_id`).** Decisão explícita.

6. **Não criar seed (`V3__seed_categorias_iniciais.sql` ou similar).** Decisão explícita.

7. **Não criar soft delete em `Categoria` (campo `ativa`).** Decisão explícita. Hard delete via repository.

8. **Não criar use cases além dos 4 prescritos.** Sem `Renomear`, `AlterarTipo`, etc.

9. **Não criar exceção customizada além de `CategoriaNaoEncontradaException`.**

10. **Não criar relacionamento JPA com `ContaEntity`.** Não há FK pra `categoria` ainda.

11. **Não usar Lombok.**

12. **Não relaxar threshold JaCoCo nem desabilitar Checkstyle.** Padrão consolidado.

13. **Sem acentos no código Java.** Padrão.

14. **Encoding UTF-8 sem BOM.**

15. **Naming de método de teste em camelCase puro.**

16. **Indentação Checkstyle**: 16 espaços nas continuações, getters em bloco, chaves obrigatórias.

17. **Antes de escrever cada classe, ler a contraparte de `conta/`** (se existir) para replicar padrão fielmente. Listei explicitamente nas tarefas. **Código vivo > esboço do prompt** quando divergirem (lição consolidada da 3.4 com `SecurityConfig`).

18. **Lições da Etapa 3.5 só registram observações reais.**

19. **Não antecipar próxima etapa.** Sem rascunhar `transacao` ou `Auth`. Sem criar pacote vazio.

20. **Não tomar decisão silenciosa em zona limítrofe.** Padrão consolidado.

## Estrutura de commits

Branch: `feat/categoria-completo`

Commits atômicos. Volume maior que outras etapas, então estruturação fina:

**Commit 1** — `feat(categoria): adiciona domain (Categoria, TipoCategoria, exception, repository interface)`
- 4 arquivos em `categoria/domain/`

**Commit 2** — `test(categoria): cobertura do domain (Categoria)`
- 1 arquivo em `test/.../categoria/domain/`

**Commit 3** — `feat(categoria): adiciona use cases (criar, listar, buscar, deletar)`
- 4 arquivos em `categoria/application/`

**Commit 4** — `test(categoria): cobertura unitaria dos 4 use cases`
- 4 arquivos em `test/.../categoria/application/`

**Commit 5** — `feat(categoria): adiciona infra (entity, mapper, jpa repo, repo impl) + migration V3`
- 4 arquivos em `categoria/infrastructure/persistence/`
- `db/migration/V3__cria_tabela_categoria.sql`

**Commit 6** — `test(categoria): cobertura de integracao do CategoriaRepositoryImpl`
- 1 arquivo em `test/.../categoria/infrastructure/persistence/`

**Commit 7** — `feat(categoria): adiciona DTOs e CategoriaController`
- 3 arquivos em `categoria/interfaces/` e `categoria/interfaces/dto/`

**Commit 8** — `feat(shared): adiciona handler de CategoriaNaoEncontradaException + whitelist /api/categorias`
- `shared/infrastructure/web/GlobalExceptionHandler.java`
- `shared/infrastructure/security/SecurityConfig.java`

**Commit 9** — `test(categoria): cobertura e2e do CategoriaController via MockMvc`
- 1 arquivo em `test/.../categoria/interfaces/`

**Commit 10** — `docs: registra etapa 3.5 (categoria completo) em decisoes e progresso`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-3-5.md`

## Validação antes de abrir PR

```bash
.\mvnw.cmd verify
git status
git log --oneline -11
```

Esperado: BUILD SUCCESS, working tree limpo, 10 commits.

## PR

Título: `feat: etapa 3.5 — bounded context categoria (etapa unica)`

Body sugerido:

```markdown
## Summary

Implementa a Etapa 3.5 do roadmap: segundo bounded context do projeto (`categoria`) em **etapa única**, validando que o template estabelecido por `conta` (3.1-3.4) é replicável a baixo custo marginal.

### O que tem em `categoria` (espelho de `conta` adaptado)

- **Domain**: `Categoria` (class final imutável, igualdade por id), `TipoCategoria` (RECEITA/DESPESA), `CategoriaNaoEncontradaException`, `CategoriaRepository` (interface).
- **Infra**: `CategoriaEntity` (`@Entity`), `CategoriaMapper` (MapStruct, `default` methods), `CategoriaJpaRepository` (Spring Data, `findByTipo`), `CategoriaRepositoryImpl`, migration `V3__cria_tabela_categoria.sql`.
- **Application**: 4 use cases (`Criar`, `Listar`, `BuscarPorId`, `Deletar`) com `@Transactional` apropriado.
- **Interfaces**: `CategoriaController` com 4 endpoints REST, `CriarCategoriaRequest` (Bean Validation), `CategoriaResponse` (com `fromDomain`).

### O que NÃO tem (decisões explícitas)

- **Sem hierarquia (parent_id)** — `visao.md` MVP prescreve 1 nível, mas implementar agora é especulação. Entra quando UI precisar de árvore.
- **Sem seed inicial** — Decisão de UX (quais categorias?). Adiada para etapa de UI.
- **Sem soft delete** — `Categoria` não tem `ativa`. DELETE faz hard delete. Reavaliar quando `transacao` aparecer com FK para `categoria`.

### Endpoints REST

```
POST   /api/categorias              → 201
GET    /api/categorias[?tipo=...]   → 200
GET    /api/categorias/{id}         → 200 ou 404
DELETE /api/categorias/{id}         → 204 ou 404
```

### Reuso de infra existente

- `GlobalExceptionHandler`: +1 handler para `CategoriaNaoEncontradaException`. Demais handlers (validação, type mismatch, IllegalArgumentException, catch-all) cobrem `categoria` sem alteração.
- `SecurityConfig`: +1 entrada na whitelist temporária com TODO de Auth.
- `pom.xml`: nenhuma alteração (JaCoCo já tem todos os thresholds ativos desde 3.4).

### Validação

- `mvnw verify` local: PASSOU
- ~50 testes novos passando
- Checkstyle: 0 violações, SpotBugs: 0 issues
- JaCoCo: todos os thresholds atendidos

### Sobre a hipótese da etapa

Esta etapa testou se o segundo bounded context custa significativamente menos que o primeiro. Resultado:

<descrever observações reais — número de zonas limítrofes encontradas, tempo relativo, quantidade de re-leituras de código vivo de `conta` necessárias. Esta seção existe pra documentar a evidência empírica de que o template é replicável (ou não).>

### Próximo passo

Próximas opções (em discussão separada): `transacao` (que vai usar `categoria` via FK) ou `Auth` real (que substitui as whitelists temporárias).
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

- Branch `feat/categoria-completo` empurrada com 11 commits (10 + 1 update do PR)
- PR aberto, CI verde, **não mergeado**
- `main` ainda no squash da 3.4
- Working tree limpo
- Bounded context `categoria` completo
- Reportar com `git log --oneline -11`, `git status`, `gh pr view --json number,state,statusCheckRollup` e parar.

## O que NÃO fazer ao terminar

- Não mergear o PR.
- Não criar prompt da próxima etapa.
- Não rascunhar `transacao` ou `Auth`.
- Não tocar em `frontend/`, `scripts/`, `docker-compose.yml`, `conta/`, `pom.xml`.
- Não sugerir "próximo passo" espontaneamente.
