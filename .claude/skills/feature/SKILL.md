---
name: feature
description: Cria estrutura de bounded context com esqueleto basico (domain, application, infrastructure/persistence, interfaces/dto) + 11 arquivos Java stub. Recebe nome do contexto em snake_case minusculo como argumento.
disable-model-invocation: true
argument-hint: [nome-do-bounded-context]
---

Voce deve criar o bounded context `$ARGUMENTS` no projeto financas-lab. Execute todos
os passos em ordem. Pare e reporte ao operador se qualquer pre-condicao falhar.

## Definicoes

Defina internamente antes de qualquer acao:

- `ARG` = `$ARGUMENTS` (ex: `cartao`, `meta_financeira`, `orcamento`)
- `NOME` = PascalCase de `ARG`: capitalize a primeira letra de cada segmento separado
  por underscore e concatene. Exemplos: `cartao` -> `Cartao`,
  `meta_financeira` -> `MetaFinanceira`, `orcamento` -> `Orcamento`.
- Pacote base: `com.laboratorio.financas.ARG`
- Diretorio base: `src/main/java/com/laboratorio/financas/ARG/`

## Passo 0 -- Validacoes (ADR-011)

**Validacao 1 -- formato:**
Verifique se ARG casa com `^[a-z][a-z0-9_]*$`. Se nao casar: escreva
"ERRO: argumento invalido -- use apenas letras minusculas, digitos e underscore,
comecando com letra (ex: /feature cartao, /feature meta_financeira)" e termine.

**Validacao 2 -- existencia:**
Use Bash (PowerShell) para verificar:
```powershell
Test-Path "src/main/java/com/laboratorio/financas/ARG/"
```
Se retornar `True`: escreva "ERRO: bounded context 'ARG' ja existe em
src/main/java/com/laboratorio/financas/ARG/ -- skill abortada para nao sobrescrever
trabalho existente" e termine.

## Passo 1 -- Criar diretorios

```powershell
[System.Environment]::CurrentDirectory = (Get-Location).Path
New-Item -ItemType Directory -Force `
  -Path "src/main/java/com/laboratorio/financas/ARG/domain", `
         "src/main/java/com/laboratorio/financas/ARG/application", `
         "src/main/java/com/laboratorio/financas/ARG/infrastructure/persistence", `
         "src/main/java/com/laboratorio/financas/ARG/interfaces/dto"
```

Verifique com `Test-Path` que os 4 diretorios foram criados. Se algum ausente:
reporte qual falhou e termine.

## Passo 2 -- Criar os 11 arquivos

Use a ferramenta Write para cada arquivo. Substitua `NOME` e `ARG` pelos valores
definidos no inicio. Codificacao: UTF-8 sem BOM.

### Arquivo 1: src/main/java/com/laboratorio/financas/ARG/domain/NOME.java

```java
package com.laboratorio.financas.ARG.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class NOME {

    private final UUID id;
    private final String nome;
    private final Instant criadoEm;

    public NOME(String nome) {
        this(UUID.randomUUID(), nome, Instant.now());
    }

    public NOME(UUID id, String nome, Instant criadoEm) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(nome, "nome nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        if (nome.isBlank()) {
            throw new IllegalArgumentException("nome nao pode ser vazio");
        }
        this.id = id;
        this.nome = nome;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NOME other)) {
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
        return "NOME{id=" + id + ", nome='" + nome + "'}";
    }
}
```

### Arquivo 2: src/main/java/com/laboratorio/financas/ARG/domain/NOMERepository.java

```java
package com.laboratorio.financas.ARG.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NOMERepository {

    NOME salvar(NOME domain);

    Optional<NOME> buscarPorId(UUID id);

    List<NOME> listarTodos();
}
```

### Arquivo 3: src/main/java/com/laboratorio/financas/ARG/domain/NOMENaoEncontradaException.java

```java
package com.laboratorio.financas.ARG.domain;

import java.util.UUID;

public class NOMENaoEncontradaException extends RuntimeException {

    private final UUID id;

    public NOMENaoEncontradaException(UUID id) {
        super("ARG nao encontrado: " + id);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
```

### Arquivo 4: src/main/java/com/laboratorio/financas/ARG/application/CriarNOMEUseCase.java

```java
package com.laboratorio.financas.ARG.application;

import com.laboratorio.financas.ARG.domain.NOME;
import com.laboratorio.financas.ARG.domain.NOMERepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarNOMEUseCase {

    private final NOMERepository repository;

    public CriarNOMEUseCase(NOMERepository repository) {
        this.repository = repository;
    }

    public record Comando(String nome) { }

    @Transactional
    public NOME executar(Comando comando) {
        NOME novo = new NOME(comando.nome());
        return repository.salvar(novo);
    }
}
```

### Arquivo 5: src/main/java/com/laboratorio/financas/ARG/infrastructure/persistence/NOMEEntity.java

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
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected NOMEEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public NOMEEntity(UUID id, String nome, Instant criadoEm) {
        this.id = id;
        this.nome = nome;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
```

### Arquivo 6: src/main/java/com/laboratorio/financas/ARG/infrastructure/persistence/NOMEJpaRepository.java

```java
package com.laboratorio.financas.ARG.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NOMEJpaRepository extends JpaRepository<NOMEEntity, UUID> {
}
```

### Arquivo 7: src/main/java/com/laboratorio/financas/ARG/infrastructure/persistence/NOMERepositoryImpl.java

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
    public NOME salvar(NOME domain) {
        NOMEEntity entity = mapper.toEntity(domain);
        NOMEEntity salva = jpaRepository.save(entity);
        return mapper.toDomain(salva);
    }

    @Override
    public Optional<NOME> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<NOME> listarTodos() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
```

### Arquivo 8: src/main/java/com/laboratorio/financas/ARG/infrastructure/persistence/NOMEMapper.java

```java
package com.laboratorio.financas.ARG.infrastructure.persistence;

import com.laboratorio.financas.ARG.domain.NOME;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NOMEMapper {

    default NOMEEntity toEntity(NOME domain) {
        if (domain == null) {
            return null;
        }
        return new NOMEEntity(domain.getId(), domain.getNome(), domain.getCriadoEm());
    }

    default NOME toDomain(NOMEEntity entity) {
        if (entity == null) {
            return null;
        }
        return new NOME(entity.getId(), entity.getNome(), entity.getCriadoEm());
    }
}
```

### Arquivo 9: src/main/java/com/laboratorio/financas/ARG/interfaces/NOMEController.java

```java
package com.laboratorio.financas.ARG.interfaces;

import com.laboratorio.financas.ARG.application.CriarNOMEUseCase;
import com.laboratorio.financas.ARG.interfaces.dto.CriarNOMERequest;
import com.laboratorio.financas.ARG.interfaces.dto.NOMEResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// TODO: ajustar /api/ARGs para plural correto em pt-BR (ex: /api/cartoes)
@RestController
@RequestMapping("/api/ARGs")
public class NOMEController {

    private final CriarNOMEUseCase criarNOMEUseCase;

    public NOMEController(CriarNOMEUseCase criarNOMEUseCase) {
        this.criarNOMEUseCase = criarNOMEUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NOMEResponse criar(@RequestBody @Valid CriarNOMERequest request) {
        // TODO: implementar
        throw new UnsupportedOperationException("Nao implementado");
    }
}
```

### Arquivo 10: src/main/java/com/laboratorio/financas/ARG/interfaces/dto/CriarNOMERequest.java

```java
package com.laboratorio.financas.ARG.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarNOMERequest(
        @NotBlank
        @Size(max = 100)
        String nome
) { }
```

### Arquivo 11: src/main/java/com/laboratorio/financas/ARG/interfaces/dto/NOMEResponse.java

```java
package com.laboratorio.financas.ARG.interfaces.dto;

import java.time.Instant;
import java.util.UUID;

public record NOMEResponse(
        UUID id,
        String nome,
        Instant criadoEm
) { }
```

## Passo 3 -- Verificar criacao

Execute:
```powershell
Get-ChildItem -Recurse "src/main/java/com/laboratorio/financas/ARG/" | Select-Object FullName
```

Confirme que os 11 arquivos existem. Se algum ausente: reporte qual falta e nao emita
o relatorio de sucesso.

## Passo 4 -- Relatorio final

Produza o seguinte relatorio (substituindo ARG e NOME pelos valores reais):

```
Bounded context 'ARG' criado.

Estrutura (11 arquivos):
  domain/NOME.java
  domain/NOMERepository.java
  domain/NOMENaoEncontradaException.java
  application/CriarNOMEUseCase.java
  infrastructure/persistence/NOMEEntity.java
  infrastructure/persistence/NOMEJpaRepository.java
  infrastructure/persistence/NOMERepositoryImpl.java
  infrastructure/persistence/NOMEMapper.java
  interfaces/NOMEController.java
  interfaces/dto/CriarNOMERequest.java
  interfaces/dto/NOMEResponse.java

ATENCAO -- antes do primeiro commit:
  1. Crie a migration Flyway V<n>__create_ARG_table.sql
     (hook 4.7 bloqueia commit com @Entity novo sem migration)
  2. Ajuste /api/ARGs para o plural correto no NOMEController.java
  3. Preencha os TODO com logica de negocio especifica do dominio
```
