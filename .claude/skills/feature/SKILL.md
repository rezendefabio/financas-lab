---
name: feature
description: Cria bounded context completo (22 arquivos Java = 15 producao + 7 testes) alinhado a docs/crud-patterns.md baseline. Inclui 4 use cases CRUD, controller com auditoria, userId, repository completo, DTOs e os 4 niveis de teste (domain, useCase com Mockito, RepositoryImpl com Testcontainers, Controller E2E). Elimina necessidade de invocar /write-test para o baseline. Recebe nome do contexto em snake_case minusculo.
disable-model-invocation: true
argument-hint: [nome-do-bounded-context]
---

Voce deve criar o bounded context `$ARGUMENTS` no projeto financas-lab gerando o
**baseline completo** ja alinhado com `docs/crud-patterns.md` (secoes 1.1, 1.4,
1.5, 2.1, 2.4-2.7, 3, 4.1-4.4, 5.1, 5.2).

**Fonte unica de padrao:** `docs/crud-patterns.md`. Esta skill gera o gabarito
canonico inline. O executor depois so adapta nomes/tipos de campos especificos
do dominio (Money, enum, FK, M:N, soft-delete, state machine etc -- secoes 1.2,
1.3, 1.6, 1.7, 5.2.1, 10.x de crud-patterns). **NAO ler outros bounded contexts
como template** -- os 15 arquivos abaixo ja sao o canonico.

## Definicoes

- `ARG` = `$ARGUMENTS` (ex: `lembrete`, `meta_financeira`)
- `NOME` = PascalCase de `ARG`: capitalize a primeira letra de cada segmento
  separado por underscore e concatene (`lembrete` -> `Lembrete`,
  `meta_financeira` -> `MetaFinanceira`)
- `nome` = camelCase de `ARG` (primeira letra minuscula): `lembrete`, `metaFinanceira`
- `nomes` = plural simples (`lembretes`, `metaFinanceiras`) -- executor ajusta
  plural pt-BR no @RequestMapping se preciso
- Pacote base: `com.laboratorio.financas.ARG`

## Passo 0 -- Validacoes (ADR-011)

**V1 -- formato:**
ARG deve casar com `^[a-z][a-z0-9_]*$`. Se nao casar:
"ERRO: argumento invalido -- use letras minusculas, digitos e underscore (ex: /feature lembrete)" e termine.

**V2 -- existencia:**
```powershell
Test-Path "src/main/java/com/laboratorio/financas/ARG/"
```
Se `True`: "ERRO: bounded context 'ARG' ja existe" e termine.

## Passo 1 -- Criar diretorios

```powershell
[System.Environment]::CurrentDirectory = (Get-Location).Path
New-Item -ItemType Directory -Force `
  -Path "src/main/java/com/laboratorio/financas/ARG/domain", `
         "src/main/java/com/laboratorio/financas/ARG/application", `
         "src/main/java/com/laboratorio/financas/ARG/infrastructure/persistence", `
         "src/main/java/com/laboratorio/financas/ARG/interfaces", `
         "src/main/java/com/laboratorio/financas/ARG/interfaces/dto", `
         "src/test/java/com/laboratorio/financas/ARG/domain", `
         "src/test/java/com/laboratorio/financas/ARG/application", `
         "src/test/java/com/laboratorio/financas/ARG/infrastructure/persistence", `
         "src/test/java/com/laboratorio/financas/ARG/interfaces"
```

Verifique com `Test-Path` que os 9 diretorios foram criados. Se algum ausente:
reporte qual falhou e termine.

**Atencao:** `interfaces/` (NAO `interfaces/rest/`). Controller fica em
`interfaces/`, DTOs em `interfaces/dto/`. Convencao do projeto.

## Passo 2 -- Criar os 22 arquivos (15 producao + 7 testes)

Use Write para cada arquivo. Substitua `NOME`, `ARG`, `nome`, `nomes` pelos
valores definidos. Codificacao: UTF-8 sem BOM.

### Arquivo 1: src/main/java/com/laboratorio/financas/ARG/domain/NOME.java

```java
package com.laboratorio.financas.ARG.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class NOME {

    private static final int NOME_MAX_LENGTH = 100;

    private final UUID id;
    private final UUID userId;
    private String nome;
    private boolean ativo;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    public NOME(UUID userId, String nome) {
        this(UUID.randomUUID(), userId, nome, true, Instant.now(), Instant.now());
    }

    public NOME(UUID id, UUID userId, String nome,
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

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NOME other)) return false;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}
```

### Arquivo 2: src/main/java/com/laboratorio/financas/ARG/domain/NOMERepository.java

```java
package com.laboratorio.financas.ARG.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NOMERepository {

    NOME salvar(NOME entidade);

    Optional<NOME> buscarPorId(UUID id);

    List<NOME> listarPorUserId(UUID userId);

    NOME atualizar(NOME entidade);

    void deletar(UUID id);
}
```

### Arquivo 3: src/main/java/com/laboratorio/financas/ARG/domain/NOMENaoEncontradoException.java

```java
package com.laboratorio.financas.ARG.domain;

import java.util.UUID;

public class NOMENaoEncontradoException extends RuntimeException {

    private final UUID id;

    public NOMENaoEncontradoException(UUID id) {
        super("NOME nao encontrado: " + id);
        this.id = id;
    }

    public UUID getId() { return id; }
}
```

### Arquivo 4: src/main/java/com/laboratorio/financas/ARG/application/CriarNOMEUseCase.java

```java
package com.laboratorio.financas.ARG.application;

import com.laboratorio.financas.ARG.domain.NOME;
import com.laboratorio.financas.ARG.domain.NOMERepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarNOMEUseCase {

    private final NOMERepository repository;

    public CriarNOMEUseCase(NOMERepository repository) {
        this.repository = repository;
    }

    public record Comando(UUID userId, String nome) {}

    @Transactional
    public NOME executar(Comando comando) {
        NOME entidade = new NOME(comando.userId(), comando.nome());
        return repository.salvar(entidade);
    }
}
```

### Arquivo 5: src/main/java/com/laboratorio/financas/ARG/application/ListarNOMEsUseCase.java

```java
package com.laboratorio.financas.ARG.application;

import com.laboratorio.financas.ARG.domain.NOME;
import com.laboratorio.financas.ARG.domain.NOMERepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarNOMEsUseCase {

    private final NOMERepository repository;

    public ListarNOMEsUseCase(NOMERepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<NOME> executar(UUID userId) {
        return repository.listarPorUserId(userId);
    }
}
```

### Arquivo 6: src/main/java/com/laboratorio/financas/ARG/application/AtualizarNOMEUseCase.java

```java
package com.laboratorio.financas.ARG.application;

import com.laboratorio.financas.ARG.domain.NOME;
import com.laboratorio.financas.ARG.domain.NOMENaoEncontradoException;
import com.laboratorio.financas.ARG.domain.NOMERepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AtualizarNOMEUseCase {

    private final NOMERepository repository;

    public AtualizarNOMEUseCase(NOMERepository repository) {
        this.repository = repository;
    }

    public record Comando(UUID id, String nome) {}

    @Transactional
    public NOME executar(Comando comando) {
        NOME entidade = repository.buscarPorId(comando.id())
                .orElseThrow(() -> new NOMENaoEncontradoException(comando.id()));
        entidade.atualizar(comando.nome());
        return repository.atualizar(entidade);
    }
}
```

### Arquivo 7: src/main/java/com/laboratorio/financas/ARG/application/DeletarNOMEUseCase.java

```java
package com.laboratorio.financas.ARG.application;

import com.laboratorio.financas.ARG.domain.NOMENaoEncontradoException;
import com.laboratorio.financas.ARG.domain.NOMERepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeletarNOMEUseCase {

    private final NOMERepository repository;

    public DeletarNOMEUseCase(NOMERepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id) {
        repository.buscarPorId(id)
                .orElseThrow(() -> new NOMENaoEncontradoException(id));
        repository.deletar(id);
    }
}
```

### Arquivo 8: src/main/java/com/laboratorio/financas/ARG/infrastructure/persistence/NOMEEntity.java

```java
package com.laboratorio.financas.ARG.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ARG")
public class NOMEEntity {

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

    protected NOMEEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public NOMEEntity(UUID id, UUID userId, String nome,
                      boolean ativo, Instant criadoEm, Instant atualizadoEm) {
        this.id = id;
        this.userId = userId;
        this.nome = nome;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
```

### Arquivo 9: src/main/java/com/laboratorio/financas/ARG/infrastructure/persistence/NOMEJpaRepository.java

```java
package com.laboratorio.financas.ARG.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NOMEJpaRepository extends JpaRepository<NOMEEntity, UUID> {

    List<NOMEEntity> findByUserId(UUID userId);
}
```

### Arquivo 10: src/main/java/com/laboratorio/financas/ARG/infrastructure/persistence/NOMEMapper.java

```java
package com.laboratorio.financas.ARG.infrastructure.persistence;

import com.laboratorio.financas.ARG.domain.NOME;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NOMEMapper {

    default NOMEEntity toEntity(NOME domain) {
        if (domain == null) return null;
        return new NOMEEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getNome(),
                domain.isAtivo(),
                domain.getCriadoEm(),
                domain.getAtualizadoEm()
        );
    }

    default NOME toDomain(NOMEEntity entity) {
        if (entity == null) return null;
        return new NOME(
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

### Arquivo 11: src/main/java/com/laboratorio/financas/ARG/infrastructure/persistence/NOMERepositoryImpl.java

```java
package com.laboratorio.financas.ARG.infrastructure.persistence;

import com.laboratorio.financas.ARG.domain.NOME;
import com.laboratorio.financas.ARG.domain.NOMERepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NOMERepositoryImpl implements NOMERepository {

    private final NOMEJpaRepository jpaRepository;
    private final NOMEMapper mapper;

    public NOMERepositoryImpl(NOMEJpaRepository jpaRepository, NOMEMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public NOME salvar(NOME entidade) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(entidade)));
    }

    @Override
    public Optional<NOME> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<NOME> listarPorUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public NOME atualizar(NOME entidade) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(entidade)));
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
```

### Arquivo 12: src/main/java/com/laboratorio/financas/ARG/interfaces/NOMEController.java

```java
package com.laboratorio.financas.ARG.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.ARG.application.AtualizarNOMEUseCase;
import com.laboratorio.financas.ARG.application.CriarNOMEUseCase;
import com.laboratorio.financas.ARG.application.DeletarNOMEUseCase;
import com.laboratorio.financas.ARG.application.ListarNOMEsUseCase;
import com.laboratorio.financas.ARG.domain.NOME;
import com.laboratorio.financas.ARG.interfaces.dto.AtualizarNOMERequest;
import com.laboratorio.financas.ARG.interfaces.dto.CriarNOMERequest;
import com.laboratorio.financas.ARG.interfaces.dto.NOMEResponse;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// TODO: ajustar /api/nomes para plural correto pt-BR se nao for o padrao default.
@RestController
@RequestMapping("/api/nomes")
public class NOMEController {

    private static final Logger LOG = LoggerFactory.getLogger(NOMEController.class);
    private static final String ENTITY_TYPE = "ARG";

    private final CriarNOMEUseCase criarUseCase;
    private final ListarNOMEsUseCase listarUseCase;
    private final AtualizarNOMEUseCase atualizarUseCase;
    private final DeletarNOMEUseCase deletarUseCase;
    private final UsuarioRepository usuarioRepository;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public NOMEController(
            CriarNOMEUseCase criarUseCase,
            ListarNOMEsUseCase listarUseCase,
            AtualizarNOMEUseCase atualizarUseCase,
            DeletarNOMEUseCase deletarUseCase,
            UsuarioRepository usuarioRepository,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper) {
        this.criarUseCase = criarUseCase;
        this.listarUseCase = listarUseCase;
        this.atualizarUseCase = atualizarUseCase;
        this.deletarUseCase = deletarUseCase;
        this.usuarioRepository = usuarioRepository;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<NOMEResponse> listar(Authentication authentication) {
        UUID userId = resolverUserId(authentication);
        return listarUseCase.executar(userId).stream()
                .map(NOMEResponse::fromDomain)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NOMEResponse criar(
            @Valid @RequestBody CriarNOMERequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = resolverUserId(authentication);
        CriarNOMEUseCase.Comando comando = new CriarNOMEUseCase.Comando(userId, request.nome());
        NOMEResponse response = NOMEResponse.fromDomain(criarUseCase.executar(comando));
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, response.id(), AuditAction.CREATE,
                userEmail(authentication), screenCode, null, toJson(response)));
        return response;
    }

    @PutMapping("/{id}")
    public NOMEResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarNOMERequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        resolverUserId(authentication);
        AtualizarNOMEUseCase.Comando comando = new AtualizarNOMEUseCase.Comando(id, request.nome());
        NOMEResponse response = NOMEResponse.fromDomain(atualizarUseCase.executar(comando));
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
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            LOG.warn("Falha ao serializar payload de audit log para {}", ENTITY_TYPE, ex);
            return null;
        }
    }
}
```

### Arquivo 13: src/main/java/com/laboratorio/financas/ARG/interfaces/dto/CriarNOMERequest.java

```java
package com.laboratorio.financas.ARG.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarNOMERequest(
        @NotBlank @Size(max = 100) String nome
) {}
```

### Arquivo 14: src/main/java/com/laboratorio/financas/ARG/interfaces/dto/AtualizarNOMERequest.java

```java
package com.laboratorio.financas.ARG.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarNOMERequest(
        @NotBlank @Size(max = 100) String nome
) {}
```

### Arquivo 15: src/main/java/com/laboratorio/financas/ARG/interfaces/dto/NOMEResponse.java

```java
package com.laboratorio.financas.ARG.interfaces.dto;

import com.laboratorio.financas.ARG.domain.NOME;
import java.util.UUID;

public record NOMEResponse(
        UUID id,
        UUID userId,
        String nome,
        boolean ativo,
        String criadoEm,
        String atualizadoEm
) {
    public static NOMEResponse fromDomain(NOME domain) {
        return new NOMEResponse(
                domain.getId(),
                domain.getUserId(),
                domain.getNome(),
                domain.isAtivo(),
                domain.getCriadoEm().toString(),
                domain.getAtualizadoEm().toString()
        );
    }
}
```

### Arquivo 16: src/test/java/com/laboratorio/financas/ARG/domain/NOMETest.java

```java
package com.laboratorio.financas.ARG.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NOMETest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void construtorCriacaoComArgumentosValidosCriaEntidade() {
        Instant antes = Instant.now();
        NOME entidade = new NOME(USER_ID, "Nome Valido");
        Instant depois = Instant.now();

        assertThat(entidade.getId()).isNotNull();
        assertThat(entidade.getUserId()).isEqualTo(USER_ID);
        assertThat(entidade.getNome()).isEqualTo("Nome Valido");
        assertThat(entidade.isAtivo()).isTrue();
        assertThat(entidade.getCriadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorCriacaoComUserIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new NOME(null, "Nome"))
                .withMessageContaining("userId");
    }

    @Test
    void construtorCriacaoComNomeNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new NOME(USER_ID, null))
                .withMessageContaining("nome");
    }

    @Test
    void construtorCriacaoComNomeBlankLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new NOME(USER_ID, "  "))
                .withMessageContaining("nome");
    }

    @Test
    void desativarMudaAtivoParaFalseEAtualizaTimestamp() {
        NOME entidade = new NOME(USER_ID, "Nome");
        Instant antes = entidade.getAtualizadoEm();

        entidade.desativar();

        assertThat(entidade.isAtivo()).isFalse();
        assertThat(entidade.getAtualizadoEm()).isAfterOrEqualTo(antes);
    }

    @Test
    void atualizarMudaNomeEAtualizaTimestamp() {
        NOME entidade = new NOME(USER_ID, "Nome Antigo");

        entidade.atualizar("Nome Novo");

        assertThat(entidade.getNome()).isEqualTo("Nome Novo");
    }
}
```

### Arquivo 17: src/test/java/com/laboratorio/financas/ARG/application/CriarNOMEUseCaseTest.java

```java
package com.laboratorio.financas.ARG.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.laboratorio.financas.ARG.domain.NOME;
import com.laboratorio.financas.ARG.domain.NOMERepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CriarNOMEUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private NOMERepository repository;
    private CriarNOMEUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(NOMERepository.class);
        useCase = new CriarNOMEUseCase(repository);
    }

    @Test
    void executarCaminhoFelizSalvaERetornaEntidade() {
        NOME salvo = new NOME(USER_ID, "Teste");
        when(repository.salvar(any(NOME.class))).thenReturn(salvo);

        CriarNOMEUseCase.Comando cmd = new CriarNOMEUseCase.Comando(USER_ID, "Teste");
        NOME resultado = useCase.executar(cmd);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Teste");
        verify(repository, times(1)).salvar(any(NOME.class));
    }
}
```

### Arquivo 18: src/test/java/com/laboratorio/financas/ARG/application/ListarNOMEsUseCaseTest.java

```java
package com.laboratorio.financas.ARG.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.ARG.domain.NOME;
import com.laboratorio.financas.ARG.domain.NOMERepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ListarNOMEsUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private NOMERepository repository;
    private ListarNOMEsUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(NOMERepository.class);
        useCase = new ListarNOMEsUseCase(repository);
    }

    @Test
    void executarRetornaListaDoRepository() {
        when(repository.listarPorUserId(USER_ID))
                .thenReturn(List.of(new NOME(USER_ID, "A"), new NOME(USER_ID, "B")));

        List<NOME> resultado = useCase.executar(USER_ID);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(n -> n.getUserId().equals(USER_ID));
    }
}
```

### Arquivo 19: src/test/java/com/laboratorio/financas/ARG/application/AtualizarNOMEUseCaseTest.java

```java
package com.laboratorio.financas.ARG.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.laboratorio.financas.ARG.domain.NOME;
import com.laboratorio.financas.ARG.domain.NOMENaoEncontradoException;
import com.laboratorio.financas.ARG.domain.NOMERepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AtualizarNOMEUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private NOMERepository repository;
    private AtualizarNOMEUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(NOMERepository.class);
        useCase = new AtualizarNOMEUseCase(repository);
    }

    @Test
    void executarComIdExistenteAtualizaNome() {
        NOME existente = new NOME(USER_ID, "Antigo");
        when(repository.buscarPorId(existente.getId())).thenReturn(Optional.of(existente));
        when(repository.atualizar(any(NOME.class))).thenAnswer(inv -> inv.getArgument(0));

        NOME resultado = useCase.executar(new AtualizarNOMEUseCase.Comando(existente.getId(), "Novo"));

        assertThat(resultado.getNome()).isEqualTo("Novo");
        verify(repository).atualizar(any(NOME.class));
    }

    @Test
    void executarComIdInexistenteLancaNOMENaoEncontradoException() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(new AtualizarNOMEUseCase.Comando(id, "X")))
                .isInstanceOf(NOMENaoEncontradoException.class);

        verify(repository, never()).atualizar(any());
    }
}
```

### Arquivo 20: src/test/java/com/laboratorio/financas/ARG/application/DeletarNOMEUseCaseTest.java

```java
package com.laboratorio.financas.ARG.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.laboratorio.financas.ARG.domain.NOME;
import com.laboratorio.financas.ARG.domain.NOMENaoEncontradoException;
import com.laboratorio.financas.ARG.domain.NOMERepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeletarNOMEUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private NOMERepository repository;
    private DeletarNOMEUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(NOMERepository.class);
        useCase = new DeletarNOMEUseCase(repository);
    }

    @Test
    void executarComIdExistenteDeleta() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.of(new NOME(USER_ID, "A")));

        useCase.executar(id);

        verify(repository).deletar(id);
    }

    @Test
    void executarComIdInexistenteLancaExcecaoENaoDeleta() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(id))
                .isInstanceOf(NOMENaoEncontradoException.class);

        verify(repository, never()).deletar(any());
    }
}
```

### Arquivo 21: src/test/java/com/laboratorio/financas/ARG/infrastructure/persistence/NOMERepositoryImplTest.java

```java
package com.laboratorio.financas.ARG.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import com.laboratorio.financas.ARG.domain.NOME;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioEntity;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NOMERepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private NOMERepositoryImpl repository;
    @Autowired
    private NOMEJpaRepository jpaRepository;
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
    void salvarEBuscarPorIdRetornaEntidadePersistida() {
        NOME entidade = new NOME(userId, "Teste");
        repository.salvar(entidade);

        Optional<NOME> resultado = repository.buscarPorId(entidade.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("Teste");
    }

    @Test
    void listarPorUserIdRetornaSomenteDoUsuario() {
        repository.salvar(new NOME(userId, "A"));
        repository.salvar(new NOME(userId, "B"));
        UUID outroUserId = criarUsuarioPersistido();
        repository.salvar(new NOME(outroUserId, "Outro"));

        List<NOME> resultado = repository.listarPorUserId(userId);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(n -> n.getUserId().equals(userId));
    }

    @Test
    void deletarRemoveEntidade() {
        NOME entidade = new NOME(userId, "Para deletar");
        repository.salvar(entidade);

        repository.deletar(entidade.getId());

        assertThat(repository.buscarPorId(entidade.getId())).isEmpty();
    }
}
```

### Arquivo 22: src/test/java/com/laboratorio/financas/ARG/interfaces/NOMEControllerTest.java

```java
package com.laboratorio.financas.ARG.interfaces;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.ARG.infrastructure.persistence.NOMEJpaRepository;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class NOMEControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private NOMEJpaRepository jpaRepository;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    @Test
    void postValidoRetorna201() throws Exception {
        Map<String, Object> body = Map.of("nome", "Teste");
        mockMvc.perform(comAuth(post("/api/nomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Teste"));
    }

    @Test
    void postNomeBlankRetorna400() throws Exception {
        Map<String, Object> body = Map.of("nome", "  ");
        mockMvc.perform(comAuth(post("/api/nomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getListaRetornaArray() throws Exception {
        mockMvc.perform(comAuth(post("/api/nomes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nome", "A")))));

        mockMvc.perform(comAuth(get("/api/nomes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void putExistenteRetorna200ComNomeAtualizado() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/nomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Antigo")))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(put("/api/nomes/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Novo")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Novo"));
    }

    @Test
    void putInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(put("/api/nomes/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "X")))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteExistenteRetorna204() throws Exception {
        MvcResult r = mockMvc.perform(comAuth(post("/api/nomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Apagar")))))
                .andReturn();
        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(delete("/api/nomes/" + id)))
                .andExpect(status().isNoContent());
    }

    @Test
    void semAuthRetorna401() throws Exception {
        mockMvc.perform(get("/api/nomes")).andExpect(status().isUnauthorized());
    }
}
```

## Passo 3 -- Verificar criacao

```powershell
Get-ChildItem -Recurse "src/main/java/com/laboratorio/financas/ARG/" -File | Select-Object FullName
Get-ChildItem -Recurse "src/test/java/com/laboratorio/financas/ARG/" -File | Select-Object FullName
```

Confirme que os 22 arquivos existem (15 producao + 7 testes). Se algum ausente:
reporte qual falta e nao emita o relatorio de sucesso.

## Passo 4 -- Relatorio final

```
Bounded context 'ARG' criado (baseline completo, 22 arquivos = 15 producao + 7 testes).

Producao (15):
  domain/NOME.java
  domain/NOMERepository.java
  domain/NOMENaoEncontradoException.java
  application/CriarNOMEUseCase.java
  application/ListarNOMEsUseCase.java
  application/AtualizarNOMEUseCase.java
  application/DeletarNOMEUseCase.java
  infrastructure/persistence/NOMEEntity.java
  infrastructure/persistence/NOMEJpaRepository.java
  infrastructure/persistence/NOMEMapper.java
  infrastructure/persistence/NOMERepositoryImpl.java
  interfaces/NOMEController.java       (com wiring de auditoria)
  interfaces/dto/CriarNOMERequest.java
  interfaces/dto/AtualizarNOMERequest.java
  interfaces/dto/NOMEResponse.java

Testes (7, cobrindo os 4 niveis -- secao 6 de crud-patterns):
  domain/NOMETest.java                                 (unit, JUnit + AssertJ)
  application/CriarNOMEUseCaseTest.java                (Mockito)
  application/ListarNOMEsUseCaseTest.java              (Mockito)
  application/AtualizarNOMEUseCaseTest.java            (Mockito)
  application/DeletarNOMEUseCaseTest.java              (Mockito)
  infrastructure/persistence/NOMERepositoryImplTest.java (Testcontainers)
  interfaces/NOMEControllerTest.java                   (MockMvc E2E)

PROXIMOS PASSOS (responsabilidade do executor):
  1. Adaptar campos especificos do dominio (Money, enum, FK, M:N, soft-delete,
     state machine etc) seguindo as secoes correspondentes de docs/crud-patterns.md
     (1.2, 1.3, 1.6, 1.7, 5.2.1, 10.x). NAO ler outros bounded contexts.
     IMPORTANTE: ao renomear/adicionar campos no NOME.java/NOMEEntity.java, ATUALIZAR
     os testes correspondentes (NOMETest, *UseCaseTest, *RepositoryImplTest, *ControllerTest)
     para refletir os novos campos. Os testes gerados ja cobrem o baseline -- expanda;
     nao recrie.
  2. Criar migration Flyway V<N>__cria_tabela_ARG.sql (numero reservado no prompt da task).
     FK para usuario usa `REFERENCES usuario(id)` -- NAO `users(id)`.
  3. Ajustar /api/nomes para plural pt-BR se necessario (ex: /api/lembretes)
  4. Adicionar handler de NOMENaoEncontradoException no GlobalExceptionHandler
     (secao 3 de crud-patterns.md -- ProblemDetail, NAO void)
  5. Registrar ENTITY_TYPE 'ARG' no middleware de auditoria via /add-entity-to-audit
  6. NAO invocar /write-test para os 4 niveis de teste backend -- ja foram criados.
     /write-test so e necessario se a expansao do dominio (passo 1) adicionar um arquivo
     novo de producao que merece teste proprio (ex: um value object com regra propria).
```
