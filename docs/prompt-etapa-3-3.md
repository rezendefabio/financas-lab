# Prompt — Etapa 3.3: Bounded Context `conta` — Infraestrutura

## Contexto

A Etapa 3.2 foi concluída e fechada via PR #30. Domain puro de `conta` está pronto: `Conta` (class imutável, igualdade por id), `TipoConta` (enum), 28 testes.

Esta etapa adiciona a **camada de infraestrutura** do bounded context `conta`. É a etapa mais densa de novidades da fábrica até aqui — introduz simultaneamente sete padrões pela primeira vez no projeto:

1. **Primeira `@Entity` JPA real** (`ContaEntity`)
2. **Primeiro `@Embeddable` JPA** (`MoneyEmbeddable` em `shared/infrastructure/`)
3. **Primeiro `@Mapper` MapStruct** ativando o annotation processor configurado desde a Etapa 1.4 (vai eliminar o warning recorrente "options were not recognized")
4. **Primeira migration Flyway de domínio** (`V2__cria_tabela_conta.sql`)
5. **Primeiro repository pattern completo** (interface no domain + impl na infra delegando a JpaRepository)
6. **Primeira persistência de UUID nativo** em Postgres
7. **Primeiro teste de integração de infra real** com Testcontainers validando save/find/findAll/soft delete query

`application/` (use cases) e `interfaces/` (controllers) ficam fora — Etapa 3.4.

## Escopo decidido (calibrado com operador antes da redação)

### Decisão 1 — Mapeamento de `Money` em JPA

**Opção B: `MoneyEmbeddable` em `shared/infrastructure/`** (espelho de `Money` com anotações JPA, na camada de infra). MapStruct converte `Money` (domain) ↔ `MoneyEmbeddable` (infra).

Razão: regra dura #1 do projeto ("Domínio não conhece Spring nem JPA. Zero anotação de framework em classes de `domain/`."). Anotar `Money` com `@Embeddable` violaria. Custo: classe extra + mapeamento. Ganho: domínio framework-free preservado.

**Estrutura física no banco:** duas colunas para o `Money` da `Conta`:
- `saldo_inicial_valor numeric(19,2) not null`
- `saldo_inicial_moeda varchar(3) not null` (código ISO da moeda — "BRL", "USD", etc)

### Decisão 2 — UUID como PK

- Coluna `id uuid primary key` (tipo nativo do Postgres)
- Geração pela aplicação (`Conta` já faz `UUID.randomUUID()` no construtor "novo")
- `@Id` sem `@GeneratedValue` — geração é responsabilidade do domain, não do banco
- Mapear com `columnDefinition = "uuid"` para garantir tipo nativo (não bytea, não varchar)

### Decisão 3 — Escopo: infra completa + testes de integração

Inclui:
- `ContaEntity` (`@Entity` JPA)
- `MoneyEmbeddable` (`@Embeddable` JPA em `shared/infrastructure/`)
- `ContaMapper` (`@Mapper` MapStruct convertendo Conta ↔ ContaEntity, usando MoneyEmbeddable)
- `ContaRepository` (interface no domain, em `conta/domain/`)
- `ContaJpaRepository` (`extends JpaRepository<ContaEntity, UUID>`, em `conta/infrastructure/`)
- `ContaRepositoryImpl` (impl em `conta/infrastructure/` delegando a `ContaJpaRepository`, fazendo conversão via mapper)
- `V2__cria_tabela_conta.sql` (migration Flyway)
- `ContaRepositoryImplIT` (teste de integração com Testcontainers validando o ciclo completo)

Não inclui (próximas etapas):
- Use cases (`CriarContaUseCase`, etc) — Etapa 3.4
- Controllers, DTOs `*Request`/`*Response` — Etapa 3.4
- Validação de unicidade de nome — não há requisito; mesmo nome em duas contas é permitido (são entidades distintas com ids distintos)

### Decisão 4 — Padrão MapStruct

- `@Mapper(componentModel = "spring")` no `ContaMapper`. Spring vai gerar bean `@Component`. O argumento global `-Amapstruct.defaultComponentModel=spring` no `pom.xml` desde a Etapa 1.4 deveria fazer isso ser implícito, mas explicitar no `@Mapper` é mais seguro: garante o comportamento mesmo se o argumento estiver sendo ignorado (warning recorrente de "options were not recognized" que aparece desde 1.4 sugere que algo está desalinhado).
- Métodos do mapper:
  - `ContaEntity toEntity(Conta conta)`
  - `Conta toDomain(ContaEntity entity)`
  - `MoneyEmbeddable toMoneyEmbeddable(Money money)`
  - `Money toMoney(MoneyEmbeddable embeddable)`
- Sem expressões customizadas no mapper — MapStruct deve resolver automaticamente. Se aparecer atrito, parar e reportar.

### Decisão 5 — Padrão Repository

- **Interface `ContaRepository` em `conta/domain/`** — domain define o que precisa, sem saber de JPA.
- **Métodos da interface** (mínimos para esta etapa):
  - `Conta salvar(Conta conta)` — save (insert ou update — JPA decide via id existente)
  - `Optional<Conta> buscarPorId(UUID id)`
  - `List<Conta> listarTodas()` — retorna ativas e inativas; filtragem é responsabilidade do caller
  - `List<Conta> listarAtivas()` — query custom retornando apenas `ativa = true`
- **`ContaJpaRepository extends JpaRepository<ContaEntity, UUID>` em `conta/infrastructure/`** — interface Spring Data, gera implementação automática.
- **`ContaRepositoryImpl` em `conta/infrastructure/`** com `@Component` (ou `@Repository`) implementando `ContaRepository`, delegando a `ContaJpaRepository` e usando `ContaMapper` para converter.
- **Para `listarAtivas`**: usar `@Query` em `ContaJpaRepository` com `WHERE ativa = true` ou método derivado `findByAtivaTrue()`. Recomendado o método derivado — mais legível, menos string crua.
- **Sem `excluir(UUID id)` nesta etapa.** Soft delete é via `desativar()` no domain + `salvar()`. Hard delete não há caso de uso.

### Decisão 6 — Naming de colunas

- **snake_case** no banco (convenção Postgres): `saldo_inicial_valor`, `saldo_inicial_moeda`, `criado_em`, `atualizado_em`.
- **camelCase** nos atributos Java da entidade: `saldoInicialValor`, `saldoInicialMoeda`, `criadoEm`, `atualizadoEm`.
- **Mapeamento explícito** via `@Column(name = "...")` na entidade. Não confiar em conversor automático.
- **Hibernate naming strategy**: deixar default. Se aparecer atrito (ex: nomes vindo errados), parar e reportar — não mudar a strategy global.

### Decisão 7 — Validação JPA na entidade

- `@NotNull` (Bean Validation) em campos não-nullable da entidade. Aplicado pra Spring Boot validar antes de persistir.
- `@Column(length = 100)` no nome (alinhado com validação do domain — limite 100).
- `@Enumerated(EnumType.STRING)` no `tipo` (valor textual, não ordinal — sobrevive a reordenação do enum).

### JaCoCo

- Threshold de `infrastructure` 60% já está ativo desde Etapa 2.4. Esta etapa adiciona código a esse pacote, deve continuar atendido.
- Threshold de `domain` 90% já está ativo. `ContaRepository` (interface no domain) tem 0 instruções a cobrir (só assinaturas), não impacta.
- Não ativar threshold de `application/` nem `interfaces/`. Continuam comentados aguardando 3.4.
- **Cobertura esperada de infra**: realista ~70-80% (mappers gerados pelo MapStruct contam, conversões básicas, métodos delegando). Não buscar 100% — repository impls com poucos branches têm cobertura natural alta mas mappers gerados pelo MapStruct podem ter linhas não-cobertas (ex: null-checks defensivos).

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra commit do squash da Etapa 3.2 com referência a PR #30
- `docs/prompt-etapa-3-3.md` presente como untracked (este próprio arquivo). Se não aparecer em `git status`, parar e reportar.
- Working tree limpo
- `conta/domain/` existe com `Conta.java` e `TipoConta.java`
- `conta/infrastructure/` **não existe ainda**
- `shared/infrastructure/` existe parcialmente (com `web/SecurityConfig.java`)
- `shared/infrastructure/persistence/` **não existe ainda**

Validar com:

```bash
git status
git log --oneline -1
ls docs/prompt-etapa-3-3.md
ls -la src/main/java/com/laboratorio/financas/conta/
ls -la src/main/java/com/laboratorio/financas/conta/infrastructure/ 2>/dev/null && echo "ATENCAO" || echo "OK: conta/infrastructure ainda nao existe"
ls -la src/main/java/com/laboratorio/financas/shared/infrastructure/persistence/ 2>/dev/null && echo "ATENCAO" || echo "OK: shared persistence ainda nao existe"
ls src/main/resources/db/migration/
```

Esperado:
- `conta/domain/` com 2 arquivos
- `conta/infrastructure/` **não existe**
- `shared/infrastructure/persistence/` **não existe**
- `db/migration/` com `V1__schema_inicial.sql`

Se algum item divergir, parar e reportar.

## Tarefas

### Tarefa 1 — Validar pré-requisitos

```bash
git status
git log --oneline -3
ls docs/prompt-etapa-3-3.md
ls -la src/main/java/com/laboratorio/financas/conta/
ls -la src/main/java/com/laboratorio/financas/conta/infrastructure/ 2>/dev/null && echo "ATENCAO" || echo "OK"
ls -la src/main/java/com/laboratorio/financas/shared/infrastructure/persistence/ 2>/dev/null && echo "ATENCAO" || echo "OK"
ls src/main/resources/db/migration/
```

### Tarefa 2 — Criar branch de trabalho

```bash
git checkout -b feat/conta-infra
```

### Tarefa 3 — Criar `MoneyEmbeddable` em `shared/infrastructure/persistence/`

Arquivo: `src/main/java/com/laboratorio/financas/shared/infrastructure/persistence/MoneyEmbeddable.java`

```java
package com.laboratorio.financas.shared.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Embeddable
public class MoneyEmbeddable {

    @NotNull
    @Column(name = "valor", nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @NotNull
    @Column(name = "moeda", nullable = false, length = 3)
    private String moeda;

    protected MoneyEmbeddable() {
        // Construtor protected exigido pelo JPA. Nao usar em codigo de aplicacao.
    }

    public MoneyEmbeddable(BigDecimal valor, String moeda) {
        this.valor = valor;
        this.moeda = moeda;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getMoeda() {
        return moeda;
    }
}
```

**Notas:**
- Construtor protected (não public) para JPA hidratar via reflexão. Construtor com args para uso pelo MapStruct.
- Sem setters — mesmo sendo `@Embeddable`, mantém imutável após hidratação.
- **Sem `@Column(name = ...)` global no embeddable** — o nome de coluna concreto vem da `ContaEntity` via `@AttributeOverride` na Tarefa 4. O `@Column(name = "valor")` aqui é placeholder para JPA não reclamar; o `@AttributeOverride` sobrescreve.
- Encoding UTF-8 sem BOM.

### Tarefa 4 — Criar `ContaEntity` em `conta/infrastructure/persistence/`

Arquivo: `src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaEntity.java`

```java
package com.laboratorio.financas.conta.infrastructure.persistence;

import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conta")
public class ContaEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoConta tipo;

    @NotNull
    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "saldo_inicial_valor", nullable = false, precision = 19, scale = 2))
    @AttributeOverride(name = "moeda", column = @Column(name = "saldo_inicial_moeda", nullable = false, length = 3))
    private MoneyEmbeddable saldoInicial;

    @Column(name = "ativa", nullable = false)
    private boolean ativa;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected ContaEntity() {
        // Construtor protected exigido pelo JPA.
    }

    public ContaEntity(
            UUID id,
            String nome,
            TipoConta tipo,
            MoneyEmbeddable saldoInicial,
            boolean ativa,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this.id = id;
        this.nome = nome;
        this.tipo = tipo;
        this.saldoInicial = saldoInicial;
        this.ativa = ativa;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public TipoConta getTipo() {
        return tipo;
    }

    public MoneyEmbeddable getSaldoInicial() {
        return saldoInicial;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
```

**Notas:**
- `@AttributeOverride` mapeia colunas concretas do `MoneyEmbeddable` no contexto de `ContaEntity`. Permite que o mesmo embeddable possa ser reusado em outras entidades futuras com nomes de coluna diferentes (ex: `valor_meta_valor` em uma futura `MetaEntity`).
- Sem `equals`/`hashCode` por id na entity. JPA tem casos delicados com igualdade durante hidratação parcial. Padrão Spring/JPA: deixar default. A entidade JPA não vaza pra fora da `infrastructure/` — domain trabalha com `Conta`, que tem igualdade correta.
- Sem `@Version` (optimistic locking) nesta etapa — adicionar quando concorrência justificar.
- Encoding UTF-8 sem BOM.

### Tarefa 5 — Criar `ContaMapper` em `conta/infrastructure/persistence/`

Arquivo: `src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaMapper.java`

```java
package com.laboratorio.financas.conta.infrastructure.persistence;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import java.math.BigDecimal;
import java.util.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContaMapper {

    default ContaEntity toEntity(Conta conta) {
        if (conta == null) {
            return null;
        }
        return new ContaEntity(
                conta.getId(),
                conta.getNome(),
                conta.getTipo(),
                toMoneyEmbeddable(conta.getSaldoInicial()),
                conta.isAtiva(),
                conta.getCriadoEm(),
                conta.getAtualizadoEm()
        );
    }

    default Conta toDomain(ContaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Conta(
                entity.getId(),
                entity.getNome(),
                entity.getTipo(),
                toMoney(entity.getSaldoInicial()),
                entity.isAtiva(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm()
        );
    }

    default MoneyEmbeddable toMoneyEmbeddable(Money money) {
        if (money == null) {
            return null;
        }
        return new MoneyEmbeddable(money.valor(), money.moeda().getCurrencyCode());
    }

    default Money toMoney(MoneyEmbeddable embeddable) {
        if (embeddable == null) {
            return null;
        }
        return new Money(embeddable.getValor(), Currency.getInstance(embeddable.getMoeda()));
    }
}
```

**Notas importantes:**
- **Mapper inteiramente em métodos `default`.** Razão: `Conta` tem dois construtores (novo e reconstrução) e MapStruct não tem como adivinhar qual usar. Métodos default explicitam o construtor de reconstrução. Money idem (record, MapStruct não infere construção).
- **`@Mapper(componentModel = "spring")`** garante que MapStruct gera implementação `@Component` independentemente do argumento global (que vem dando warning).
- Mesmo com tudo em `default`, o `@Mapper` ainda gera classe de implementação concreta `ContaMapperImpl` (vazia, herda os defaults). Spring instancia essa.
- Encoding UTF-8 sem BOM.

### Tarefa 6 — Criar `ContaRepository` (interface no domain)

Arquivo: `src/main/java/com/laboratorio/financas/conta/domain/ContaRepository.java`

```java
package com.laboratorio.financas.conta.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContaRepository {

    Conta salvar(Conta conta);

    Optional<Conta> buscarPorId(UUID id);

    List<Conta> listarTodas();

    List<Conta> listarAtivas();
}
```

**Notas:**
- Interface pura. Zero anotação Spring ou JPA. Pertence ao domain.
- Encoding UTF-8 sem BOM.

### Tarefa 7 — Criar `ContaJpaRepository` em `conta/infrastructure/persistence/`

Arquivo: `src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaJpaRepository.java`

```java
package com.laboratorio.financas.conta.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaJpaRepository extends JpaRepository<ContaEntity, UUID> {

    List<ContaEntity> findByAtivaTrue();
}
```

**Notas:**
- Interface Spring Data. Implementação gerada automaticamente.
- `findByAtivaTrue` é método derivado — Spring Data parsea o nome.
- Encoding UTF-8 sem BOM.

### Tarefa 8 — Criar `ContaRepositoryImpl` em `conta/infrastructure/persistence/`

Arquivo: `src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaRepositoryImpl.java`

```java
package com.laboratorio.financas.conta.infrastructure.persistence;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ContaRepositoryImpl implements ContaRepository {

    private final ContaJpaRepository jpaRepository;
    private final ContaMapper mapper;

    public ContaRepositoryImpl(ContaJpaRepository jpaRepository, ContaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Conta salvar(Conta conta) {
        ContaEntity entity = mapper.toEntity(conta);
        ContaEntity salva = jpaRepository.save(entity);
        return mapper.toDomain(salva);
    }

    @Override
    public Optional<Conta> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Conta> listarTodas() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Conta> listarAtivas() {
        return jpaRepository.findByAtivaTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
```

**Notas:**
- Construtor explícito (sem `@Autowired`, sem Lombok). Spring resolve.
- Sem `@Transactional` em métodos read — Spring Data já trata. Em `salvar`, transação é responsabilidade do caller (use case na Etapa 3.4). Por enquanto, sem anotação — JPA opera com transação implícita do Spring Data quando há.
- Encoding UTF-8 sem BOM.

### Tarefa 9 — Criar migration `V2__cria_tabela_conta.sql`

Arquivo: `src/main/resources/db/migration/V2__cria_tabela_conta.sql`

```sql
-- V2: cria tabela conta
-- Bounded context: conta
-- Etapa 3.3 da Camada 2

CREATE TABLE conta (
    id                    UUID            PRIMARY KEY,
    nome                  VARCHAR(100)    NOT NULL,
    tipo                  VARCHAR(30)     NOT NULL,
    saldo_inicial_valor   NUMERIC(19, 2)  NOT NULL,
    saldo_inicial_moeda   VARCHAR(3)      NOT NULL,
    ativa                 BOOLEAN         NOT NULL,
    criado_em             TIMESTAMPTZ     NOT NULL,
    atualizado_em         TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_conta_ativa ON conta (ativa);
```

**Notas:**
- `TIMESTAMPTZ` no Postgres mapeia bem com `Instant` em Java (preserva fuso, default UTC).
- Index em `ativa` antecipa que `listarAtivas` será chamada com frequência.
- Sem constraint `CHECK` em `tipo` (validação fica em JPA `@Enumerated`); adicionar constraint duplicaria validação.
- Sem foreign keys — não há relacionamento ainda.
- Encoding UTF-8 sem BOM.

### Tarefa 10 — Criar teste de integração `ContaRepositoryImplIT`

Arquivo: `src/test/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaRepositoryImplIT.java`

**Naming:** sufixo `IT` — convenção do projeto registrada em `decisoes.md` para testes de integração que não são executados no Surefire default? **Verificar antes:** os testes de integração existentes (`HealthcheckControllerTest`, `FlywayMigrationTest`) usam sufixo `Test` (singular), não `IT`. **Conformar com o existente: usar sufixo `Test`.**

**Renomear para:** `ContaRepositoryImplTest.java` — alinhado com precedente. Localização: `src/test/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaRepositoryImplTest.java`.

**Estrutura esperada:**

```java
package com.laboratorio.financas.conta.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.TipoConta;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ContaRepositoryImplTest extends AbstractIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Autowired
    private ContaRepositoryImpl repository;

    @Autowired
    private ContaJpaRepository jpaRepository;

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    @Test
    void salvarPersisteContaERetornaInstanciaEquivalente() {
        // Given
        Conta nova = new Conta(
                "Conta Corrente",
                TipoConta.CORRENTE,
                new Money(new BigDecimal("100.00"), BRL)
        );

        // When
        Conta salva = repository.salvar(nova);

        // Then
        assertThat(salva.getId()).isEqualTo(nova.getId());
        assertThat(salva.getNome()).isEqualTo("Conta Corrente");
        assertThat(salva.getTipo()).isEqualTo(TipoConta.CORRENTE);
        assertThat(salva.getSaldoInicial()).isEqualTo(new Money(new BigDecimal("100.00"), BRL));
        assertThat(salva.isAtiva()).isTrue();
    }

    // ... mais testes
}
```

**Cenários obrigatórios:**

1. **`salvarPersisteContaERetornaInstanciaEquivalente`** — cria nova `Conta`, salva, verifica que valores persistem
2. **`salvarPersistePersisteCorretamenteSaldoComDuasCasasDecimais`** — saldo `100.50` é persistido e relido sem perda de precisão
3. **`salvarPersisteCorretamenteSaldoZero`** — saldo zero é persistido corretamente
4. **`salvarPersisteCorretamenteSaldoNegativo`** — saldo `-250.00` (cartão de crédito) persiste corretamente
5. **`salvarPersisteCorretamenteContaCartaoCredito`** — `tipo = CARTAO_CREDITO` persiste como string `"CARTAO_CREDITO"` (verificável via `findById`)
6. **`buscarPorIdRetornaContaQuandoExiste`** — após salvar, `buscarPorId` retorna `Optional` com a conta
7. **`buscarPorIdRetornaVazioQuandoNaoExiste`** — UUID aleatório retorna `Optional.empty()`
8. **`listarTodasRetornaContasAtivasEInativas`** — salva 2 ativas + 1 inativa (criada e desativada), `listarTodas` retorna 3
9. **`listarAtivasRetornaApenasAtivas`** — mesmo cenário, `listarAtivas` retorna 2
10. **`salvarApliceaDesativacao`** — salva conta ativa, desativa via `desativar()`, salva novamente, `buscarPorId` retorna conta com `ativa = false` e `atualizadoEm` mais recente
11. **`salvarComMoedaDiferentePersisteCodigoCorretamente`** — conta com `Money(USD)` persiste código "USD" corretamente

**Total esperado: ~11 testes.** Foco em validar o ciclo Conta ↔ ContaEntity ↔ Postgres real.

**Notas críticas:**

- **`AbstractIntegrationTest`** — classe base de Testcontainers já existente desde Etapa 2.1. Estende e usa `@Autowired` para repos.
- **`@AfterEach` com `deleteAll()`** garante isolamento entre testes. O container é compartilhado (regra Etapa 2.1) mas os dados não.
- **Igualdade de `Money`** após round-trip: `BigDecimal` com escala 2 + Currency BRL deve dar `equals` true. Se Hibernate retornar `numeric(19,2)` como `BigDecimal` com escala diferente, vai falhar — é exatamente o tipo de bug que esse teste pega.
- **Naming dos testes em camelCase puro, sem underscore.** Regra confirmada na 3.1.

### Tarefa 11 — Validar localmente

```bash
.\mvnw.cmd compile

# Verificar que MapStruct gerou ContaMapperImpl:
ls target/generated-sources/annotations/com/laboratorio/financas/conta/infrastructure/persistence/ 2>/dev/null

# Verificar que warning de MapStruct sumiu (ou nao):
.\mvnw.cmd compile 2>&1 | grep -i "options were not recognized" || echo "OK: warning sumiu"

# Build completo:
.\mvnw.cmd verify
```

**Esperado:**
- `ContaMapperImpl.class` em `target/generated-sources/annotations/...` (MapStruct gerou)
- Warning "options were not recognized by any processor" pode continuar aparecendo se for sobre outro argumento — registrar mas não bloquear
- BUILD SUCCESS
- 11+ novos testes de `ContaRepositoryImplTest` passando, mais os existentes (~84 total)
- Checkstyle: 0 violações
- SpotBugs: 0 issues
- JaCoCo: cobertura mantida

**Possíveis pontos de atrito (parar e reportar se acontecer):**

1. **Migration falha ao aplicar** — geralmente erro de SQL. Ler mensagem do Flyway.
2. **MapStruct não gera implementação** — verificar que annotation processor está rodando (target/generated-sources).
3. **`Money.valor()` ou `Money.moeda()` não encontrados** — Money é record, methods são `valor()` e `moeda()` (não `getValor()` etc).
4. **Hibernate reclama de `@Embeddable` sem `@Column` em algum campo** — `@AttributeOverride` na entidade resolve.
5. **Teste de `buscarPorIdRetornaContaQuandoExiste` falha em `assertThat(salva).isEqualTo(nova)`** — não comparar `Conta` com `Conta` via equals (igualdade por id, ok), mas se comparar campo a campo, atenção a `criadoEm`/`atualizadoEm` que podem ter precisão diferente após round-trip Postgres (Postgres `TIMESTAMPTZ` tem precisão de microssegundos, `Instant.now()` em Java pode ter nanos). Usar `isCloseTo(...)` ou truncar pra microssegundos.
6. **Indentação Checkstyle** — continuações em 16 espaços, getters em bloco multi-linha, `if/else` com chaves explícitas. Padrão consolidado nas etapas anteriores. Esboços neste prompt já estão alinhados.

### Tarefa 12 — Atualizar `docs/decisoes.md`

**12a.** Localizar a seção "Padrões aplicados" da arquitetura. Adicionar nota:

```markdown
**Persistência de value objects compartilhados** (a partir da Etapa 3.3): VOs do domain (`Money`, etc) são mapeados para `*Embeddable` em `shared/infrastructure/persistence/`. MapStruct converte na borda. Domain permanece framework-free. `@AttributeOverride` na entidade hospedeira define os nomes de coluna concretos.
```

**12b.** Adicionar nota sobre `@Mapper(componentModel = "spring")`:

```markdown
**MapStruct mappers**: anotação `@Mapper(componentModel = "spring")` sempre explícita. Argumento global `-Amapstruct.defaultComponentModel=spring` no pom.xml ainda gera warning recorrente "options were not recognized" — explicitar no `@Mapper` é o mecanismo confiável.
```

**12c.** Adicionar entrada no histórico:

```markdown
- **2026-05-09** — Etapa 3.3 concluída: infraestrutura do bounded context `conta`. `ContaEntity` (primeira `@Entity` real), `MoneyEmbeddable` em `shared/infrastructure/persistence/` (primeiro `@Embeddable`), `ContaMapper` (primeiro MapStruct ativo), `ContaRepository` (interface no domain), `ContaRepositoryImpl` + `ContaJpaRepository`, `V2__cria_tabela_conta.sql`, `ContaRepositoryImplTest` (11 testes integração com Testcontainers). Mergeado via PR #XX.
```

### Tarefa 13 — Atualizar `docs/progresso.md`

**13a.** Atualizar campo "Última atualização": `2026-05-09 (Etapa 3.3 — Conta infra)`.

**13b.** Na seção da Camada 2, manter o critério `Bounded context conta com domínio puro + use cases + repositório` como `[~]` parcial. Adicionar nota inline atualizada:

`[~] Bounded context conta com domínio puro + use cases + repositório` (domain 3.2 ✅, infra+repository 3.3 ✅, use cases+controllers ⏸️ Etapa 3.4)

**13c.** Marcar critério `MapStruct funcionando entre Entity JPA ↔ Domain` como `[x]`.

**13d.** Adicionar nova seção **"Lições da Etapa 3.3"** logo antes de **"Lições da Etapa 3.2"** (ordem decrescente):

```markdown
## Lições da Etapa 3.3

### Candidatos a hook (automatizar em etapas futuras)

(Preencher com lições reais — ou `(Nenhum novo nesta etapa.)` se nada surgir.)

### Lições de ambiente

(Preencher com lições reais — ou `(Nenhum novo nesta etapa.)` se nada surgir.)
```

**Regra dura:** só registrar lições reais.

**13e.** Adicionar entrada no histórico:

```markdown
- **2026-05-09** — Etapa 3.3 concluída: infra de `conta` (entity, embeddable, mapper, repository pattern, migration V2, 11 testes integração). MapStruct ativo pela primeira vez. Mergeado via PR #XX.
```

### Tarefa 14 — Versionar este próprio prompt

Confirmar `docs/prompt-etapa-3-3.md` em disco e incluir no commit de docs.

### Tarefa 15 — Validação final antes de commitar

```bash
xxd src/main/java/com/laboratorio/financas/shared/infrastructure/persistence/MoneyEmbeddable.java | head -1
xxd src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaEntity.java | head -1
xxd src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaMapper.java | head -1
xxd src/main/java/com/laboratorio/financas/conta/domain/ContaRepository.java | head -1
xxd src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaJpaRepository.java | head -1
xxd src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaRepositoryImpl.java | head -1
xxd src/main/resources/db/migration/V2__cria_tabela_conta.sql | head -1
xxd src/test/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaRepositoryImplTest.java | head -1
# Esperado: nenhum começa com EF BB BF

.\mvnw.cmd verify

git status
```

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Arquivos permitidos:
   - `src/main/java/com/laboratorio/financas/conta/domain/ContaRepository.java` (criação)
   - `src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaEntity.java` (criação)
   - `src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaMapper.java` (criação)
   - `src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaJpaRepository.java` (criação)
   - `src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaRepositoryImpl.java` (criação)
   - `src/main/java/com/laboratorio/financas/shared/infrastructure/persistence/MoneyEmbeddable.java` (criação)
   - `src/main/resources/db/migration/V2__cria_tabela_conta.sql` (criação)
   - `src/test/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaRepositoryImplTest.java` (criação)
   - `docs/decisoes.md`
   - `docs/progresso.md`
   - `docs/prompt-etapa-3-3.md` (este arquivo, versionar)

2. **Não tocar em `pom.xml`.** Threshold JaCoCo de `infrastructure/` 60% já está ativo. Não há mudança de build.

3. **Não tocar em `Conta.java`, `TipoConta.java`, `Money.java`, `MoneyTest.java`, `ContaTest.java`.** Domain pronto, não modificar.

4. **Não tocar em `application.yml`** (configurações JPA já estão prontas — `ddl-auto: validate`, Flyway enabled).

5. **Não criar use case, controller, DTO `*Request`/`*Response`, `*Service`, `*Command`, `*Query`.** Tudo entra na Etapa 3.4.

6. **Não criar `categoria/`, `transacao/`, `auth/`, `usuario/`, ou outro bounded context.**

7. **Não usar Lombok no domain (`ContaRepository`).** Permitido em entity (`@Getter` se quiser), mas decisão: **manter explícito também na entity** desta etapa pra reduzir complexidade de Lombok + MapStruct interagindo. Lombok pode entrar em etapa futura quando justificar.

8. **Não criar exceção customizada `ContaNaoEncontradaException`.** O repository retorna `Optional.empty()` — caller decide. Exceção entra na 3.4 quando use case precisar.

9. **Não adicionar métodos extras no `ContaRepository` além dos quatro especificados.** Sem `contar()`, sem `existePorId()`, sem `buscarPorNome()`. Sem caso de uso ainda.

10. **Não adicionar `@Transactional` em métodos do `ContaRepositoryImpl`.** Tratamento transacional é responsabilidade do use case (Etapa 3.4).

11. **Não criar relacionamentos `@OneToMany`/`@ManyToOne`** com outras entidades (categoria, transacao). Não existem ainda.

12. **Não criar `@NamedQuery`, `@Query` complexa, query nativa.** `findByAtivaTrue` é método derivado simples — basta.

13. **Não relaxar threshold JaCoCo nem desabilitar Checkstyle.** Padrão consolidado.

14. **Sem acentos no código Java** (mensagens, comentários, nomes — manter como nas etapas anteriores).

15. **Encoding UTF-8 sem BOM** em todos os arquivos criados.

16. **Naming de método de teste em camelCase puro, sem underscore.** Regra confirmada na 3.1.

17. **Indentação Checkstyle**: continuações em 16 espaços, getters em bloco multi-linha, `if/else` com chaves explícitas. Padrão consolidado nas 3.1 e 3.2 (cada uma com commit extra de fix).

18. **Lições da Etapa 3.3 só registram observações reais.** Não inventar.

19. **Não antecipar Etapa 3.4.** Sem rascunhar próximas etapas. Sem criar `application/` ou `interfaces/` em `conta/`.

20. **Não tomar decisão silenciosa em zona limítrofe.** Padrão consolidado. Se aparecer dúvida, parar e reportar.

## Estrutura de commits

Branch: `feat/conta-infra`

Commits atômicos, em ordem:

**Commit 1** — `feat(shared): adiciona MoneyEmbeddable em shared/infrastructure/persistence`
- `src/main/java/com/laboratorio/financas/shared/infrastructure/persistence/MoneyEmbeddable.java`

**Commit 2** — `feat(conta): adiciona ContaEntity e migration V2`
- `src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaEntity.java`
- `src/main/resources/db/migration/V2__cria_tabela_conta.sql`

**Commit 3** — `feat(conta): adiciona ContaMapper (MapStruct)`
- `src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaMapper.java`

**Commit 4** — `feat(conta): adiciona repository pattern (interface no domain + impl na infra)`
- `src/main/java/com/laboratorio/financas/conta/domain/ContaRepository.java`
- `src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaJpaRepository.java`
- `src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaRepositoryImpl.java`

**Commit 5** — `test(conta): cobertura de integracao do ContaRepositoryImpl com Testcontainers`
- `src/test/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaRepositoryImplTest.java`

**Commit 6** — `docs: registra etapa 3.3 (conta infra) em decisoes e progresso`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-3-3.md`

## Validação antes de abrir PR

```bash
.\mvnw.cmd verify
git status
git log --oneline -8
```

Esperado:
- BUILD SUCCESS
- ~84 testes passando
- JaCoCo `domain` 90%, `infrastructure` 60%, BUNDLE 75% atendidos
- Working tree limpo
- 6 commits na branch

## PR

Título: `feat: etapa 3.3 — bounded context conta (infraestrutura)`

Body sugerido:

```markdown
## Summary

Implementa a Etapa 3.3 do roadmap (terceira etapa da Camada 2): camada de infraestrutura completa do bounded context `conta`. Sete padrões estreiam pela primeira vez no projeto. Use cases e controllers ficam para 3.4.

### Padrões que estreiam aqui

1. Primeira `@Entity` JPA (`ContaEntity`)
2. Primeiro `@Embeddable` JPA (`MoneyEmbeddable` em `shared/infrastructure/persistence/`)
3. Primeiro `@Mapper` MapStruct (ativando o annotation processor configurado desde 1.4)
4. Primeira migration Flyway de domínio (`V2__cria_tabela_conta.sql`)
5. Primeiro repository pattern completo (interface no domain + impl na infra)
6. Primeira persistência de UUID nativo em Postgres
7. Primeiro teste de integração de infra com Testcontainers (11 testes)

### Mudanças

- `shared/infrastructure/persistence/MoneyEmbeddable.java`: VO mapeável para JPA com `@Embeddable`. Espelho de `Money` na camada de infra. Mantém domain framework-free.
- `conta/domain/ContaRepository.java`: interface pura no domain (4 métodos: `salvar`, `buscarPorId`, `listarTodas`, `listarAtivas`).
- `conta/infrastructure/persistence/ContaEntity.java`: entidade JPA com `@AttributeOverride` mapeando colunas do `MoneyEmbeddable`.
- `conta/infrastructure/persistence/ContaMapper.java`: MapStruct mapper com `@Mapper(componentModel = "spring")` (explícito para evitar warning recorrente).
- `conta/infrastructure/persistence/ContaJpaRepository.java`: Spring Data com método derivado `findByAtivaTrue`.
- `conta/infrastructure/persistence/ContaRepositoryImpl.java`: implementação delegando a JpaRepository, convertendo via mapper.
- `db/migration/V2__cria_tabela_conta.sql`: schema da tabela com `uuid` PK, `numeric(19,2)` para valor, `timestamptz` para timestamps, índice em `ativa`.
- `test/.../ContaRepositoryImplTest.java`: 11 testes de integração com Testcontainers cobrindo save, buscar, listar (ativas/todas), saldo zero/negativo/decimal, soft delete via `desativar`.

### Decisões de escopo

- **`MoneyEmbeddable` na infra, não anotação direta em `Money`**. Preserva regra dura #1 (domain framework-free). Custo: classe extra + mapeamento. Ganho: domínio puro.
- **UUID nativo do Postgres** (`columnDefinition = "uuid"`). Geração pela aplicação via `UUID.randomUUID()` no construtor de `Conta`. Sem `@GeneratedValue`.
- **`@Mapper(componentModel = "spring")` explícito** para evitar dependência do argumento global `-Amapstruct.defaultComponentModel=spring` (que gera warning recorrente).
- **Mapper inteiramente em métodos `default`** porque `Conta` tem dois construtores (novo/reconstrução) e MapStruct não consegue inferir qual usar.
- **`@Transactional` ainda não aplicado** no repository — fica para o use case na 3.4.
- **Teste com sufixo `Test`** (não `IT`), alinhado com precedente do projeto (`HealthcheckControllerTest`, `FlywayMigrationTest`).

### Validação

- `mvnw verify` local: PASSOU
- 11 testes de integração `ContaRepositoryImplTest` passando contra Postgres real (Testcontainers)
- Checkstyle: 0 violações
- SpotBugs: 0 issues
- JaCoCo: domain 90%, infrastructure 60%, BUNDLE 75% — todos atendidos

### Próximo passo

Etapa 3.4 (application + interfaces de `conta` — use cases, DTOs, `@RestController`) — fora do escopo deste PR. Abre em discussão separada.
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

- Branch `feat/conta-infra` empurrada com 7 commits (6 + 1 update do PR)
- PR aberto, CI verde, **não mergeado**
- `main` ainda aponta pro squash da 3.2
- Working tree limpo
- 6 arquivos Java novos + 1 SQL + 1 teste integração + 3 docs atualizados
- Reportar com `git log --oneline -7`, `git status`, `gh pr view --json number,state,statusCheckRollup` e parar.

## O que NÃO fazer ao terminar

- Não mergear o PR. Aguardar autorização explícita.
- Não criar prompt da próxima etapa.
- Não rascunhar Etapa 3.4.
- Não criar `application/` ou `interfaces/` em `conta/`.
- Não criar outros bounded contexts.
- Não tocar em `frontend/`, `scripts/`, `docker-compose.yml`, `pom.xml`.
- Não sugerir "próximo passo" espontaneamente.
