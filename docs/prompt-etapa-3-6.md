# Prompt — Etapa 3.6: Bounded Context `transacao` — Domain + Infraestrutura

## Contexto

A Etapa 3.5 foi concluída e fechada via PR #34. Bounded context `categoria` está vivo ponta a ponta. Camada 2 tem dois bounded contexts validados (`conta`, `categoria`) e o template foi comprovadamente replicável.

Esta etapa abre o **terceiro e mais complexo bounded context**: `transacao`. É a entidade central do produto — sem ela, `conta` e `categoria` são cadastros vazios. Com ela, o produto começa a fazer sentido pro usuário (registrar receitas/despesas/transferências, derivar saldo, gerar relatórios).

**Quebra em duas etapas** (calibrada antes da redação):
- **3.6 (esta)**: domain + infra (entity JPA, mapper, repository, migration). FKs reais com `Conta` e `Categoria`. Sem use cases, sem controller.
- **3.7 (próxima)**: application + interfaces. Use cases (incluindo validação de FK), controller com paginação, PUT pra editar.

**Saldo derivado fica em 3.8 dedicada** depois que `transacao` estiver completo.

## Padrões que estreiam nesta etapa

1. **Primeira FK em JPA** (`@ManyToOne` ou `@JoinColumn`) — `transacao` referencia `Conta` e `Categoria`
2. **Primeiras validações cruzadas de domínio** — `tipo == TRANSFERENCIA → contaDestinoId obrigatório && != contaId`, etc
3. **Migration com FOREIGN KEY constraints** — V4 com FKs para `conta` e `categoria`
4. **Mapeamento `LocalDate` ↔ `DATE` em Postgres** — primeira data sem hora no projeto
5. **Repository com filtros compostos** — listar transações por conta + período + tipo (mas só esqueleto na 3.6; uso pleno em 3.7)

## Escopo decidido (calibrado com operador antes da redação)

### Modelagem de `Transacao`

```
Transacao {
  id: UUID                          // gerado pela aplicação
  tipo: TipoTransacao               // RECEITA, DESPESA, TRANSFERENCIA
  valor: Money                      // sempre positivo, escala 2, BRL no MVP
  data: LocalDate                   // dia da transação, sem hora
  descricao: String                 // max 200 chars, pode ser blank? — decisão abaixo
  contaId: UUID                     // FK para Conta (sempre presente)
  contaDestinoId: UUID              // FK para Conta (só TRANSFERENCIA, null nos demais)
  categoriaId: UUID                 // FK para Categoria (null em TRANSFERENCIA, opcional nos demais)
  criadoEm: Instant
  atualizadoEm: Instant
}
```

**Decisões pontuais:**

- **`descricao` é obrigatória.** `@NotBlank`, max 200 chars. Razão: transação sem descrição em UI é inútil ("R$ 50 em 15/05" — pra quê?). MVP single-user prefere forçar descrição mínima a permitir vazia.
- **`valor` sempre positivo.** Sinal vem do `tipo` (RECEITA = entrada, DESPESA = saída). `Money.ehPositivo()` tem que retornar true. Aceita `valor.ehZero()`? **Não**. Transação de R$ 0 não tem sentido.
- **`data` é `LocalDate`** (sem hora). MVP é registro manual. Importação de extrato com timestamp está fora do MVP.
- **Validações cruzadas no construtor:**
  - `tipo == TRANSFERENCIA` → `contaDestinoId != null` && `contaId != contaDestinoId` && `categoriaId == null`
  - `tipo != TRANSFERENCIA` → `contaDestinoId == null` (categoriaId pode ser null ou não)
- **Sem `valor.ehNegativo()` permitido em construtor.** Validação rígida.
- **Tudo via `IllegalArgumentException`.** Sem exceção customizada nesta etapa.

### Decisões de design (espelho de Conta/Categoria, com adaptações)

- **`Transacao` é class final imutável.** Mesmo padrão estabelecido em `Conta` e `Categoria`.
- **Construtores: dois.**
  1. **Construtor "novo"**: `Transacao(TipoTransacao tipo, Money valor, LocalDate data, String descricao, UUID contaId, UUID contaDestinoId, UUID categoriaId)`. Gera `id`, define `criadoEm = atualizadoEm = Instant.now()`.
  2. **Construtor "reconstrução"**: recebe todos os campos (10 ao todo).
- **Igualdade por `id`**.
- **Sem método `desativar()`** — `transacao` não tem soft delete.
- **Sem método de mutação nesta etapa** (`alterar`, `editar`, etc) — o ciclo PUT na 3.7 vai criar nova instância via construtor de reconstrução, não mutar.
- **Repository pattern padrão** — interface no domain, impl na infra delegando a JpaRepository com mapper.
- **Localização**:
  - `transacao/domain/Transacao.java`
  - `transacao/domain/TipoTransacao.java`
  - `transacao/domain/TransacaoRepository.java`
  - `transacao/infrastructure/persistence/TransacaoEntity.java`
  - `transacao/infrastructure/persistence/TransacaoJpaRepository.java`
  - `transacao/infrastructure/persistence/TransacaoRepositoryImpl.java`
  - `transacao/infrastructure/persistence/TransacaoMapper.java`
  - `db/migration/V4__cria_tabela_transacao.sql`
- **Sem `TransacaoNaoEncontradaException` nesta etapa.** Use cases (3.7) precisam dela; criar agora seria especulação fora de escopo. Quando `BuscarTransacaoPorIdUseCase` aparecer na 3.7, a exceção entra junto.

### Repository

Interface `TransacaoRepository` em `transacao/domain/`. Métodos para esta etapa:

- `Transacao salvar(Transacao transacao)`
- `Optional<Transacao> buscarPorId(UUID id)`
- `void deletar(UUID id)`

**Por que apenas três métodos:** filtros compostos (por conta, período, tipo, com paginação) precisam de spec do controller na 3.7. Implementar antes seria adivinhar. **Hard line: NÃO criar `listarPorConta`, `listarPorPeriodo`, `listarPorCategoria`, `listarPaginado` nesta etapa.** Agente vai querer; recusar.

### Persistência — modelagem da `TransacaoEntity`

**Decisão sobre FKs:** usar `UUID` direto como coluna no JPA, **não** `@ManyToOne ContaEntity`. Razão:

- Bounded contexts mantêm baixo acoplamento. `TransacaoEntity` referenciando `ContaEntity` cruzaria fronteira de bounded context na camada JPA.
- Evita `LazyInitializationException` clássico do JPA quando o session fecha.
- Performance: sem joins implícitos, queries mais previsíveis.
- Validação de existência da FK fica no use case (decisão 9 do calibração: "validação de FK via repositórios diretos no use case"), não no JPA.
- FK constraint **existe no banco** (V4 cria), mas relação JPA é por UUID solto.

Este é o padrão típico em arquitetura DDD com bounded contexts: relacionamentos entre agregados são por **id**, não por referência de entidade.

```java
@Entity
@Table(name = "transacao")
public class TransacaoEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoTransacao tipo;

    @NotNull
    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "valor_valor", nullable = false, precision = 19, scale = 2))
    @AttributeOverride(name = "moeda", column = @Column(name = "valor_moeda", nullable = false, length = 3))
    private MoneyEmbeddable valor;

    @NotNull
    @Column(name = "data_transacao", nullable = false)
    private LocalDate data;

    @NotNull
    @Column(name = "descricao", nullable = false, length = 200)
    private String descricao;

    @NotNull
    @Column(name = "conta_id", columnDefinition = "uuid", nullable = false)
    private UUID contaId;

    @Column(name = "conta_destino_id", columnDefinition = "uuid")
    private UUID contaDestinoId;

    @Column(name = "categoria_id", columnDefinition = "uuid")
    private UUID categoriaId;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    // construtores + getters
}
```

**Atenção a nomes de coluna:** `data` é palavra reservada em alguns contextos SQL — usar `data_transacao` por segurança. Nomes das colunas do `Money` seguem padrão `<campo>_valor`/`<campo>_moeda` (consistente com `ContaEntity` que tem `saldo_inicial_valor`/`saldo_inicial_moeda`).

### Migration V4

```sql
-- V4: cria tabela transacao
-- Bounded context: transacao
-- Etapa 3.6 da Camada 2

CREATE TABLE transacao (
    id                UUID            PRIMARY KEY,
    tipo              VARCHAR(20)     NOT NULL,
    valor_valor       NUMERIC(19, 2)  NOT NULL,
    valor_moeda       VARCHAR(3)      NOT NULL,
    data_transacao    DATE            NOT NULL,
    descricao         VARCHAR(200)    NOT NULL,
    conta_id          UUID            NOT NULL,
    conta_destino_id  UUID,
    categoria_id      UUID,
    criado_em         TIMESTAMPTZ     NOT NULL,
    atualizado_em     TIMESTAMPTZ     NOT NULL,
    CONSTRAINT fk_transacao_conta             FOREIGN KEY (conta_id)         REFERENCES conta (id),
    CONSTRAINT fk_transacao_conta_destino     FOREIGN KEY (conta_destino_id) REFERENCES conta (id),
    CONSTRAINT fk_transacao_categoria         FOREIGN KEY (categoria_id)     REFERENCES categoria (id),
    CONSTRAINT chk_transacao_valor_positivo   CHECK (valor_valor > 0),
    CONSTRAINT chk_transacao_transferencia    CHECK (
        (tipo = 'TRANSFERENCIA' AND conta_destino_id IS NOT NULL AND conta_id <> conta_destino_id AND categoria_id IS NULL)
        OR
        (tipo <> 'TRANSFERENCIA' AND conta_destino_id IS NULL)
    )
);

CREATE INDEX idx_transacao_conta_data ON transacao (conta_id, data_transacao DESC);
CREATE INDEX idx_transacao_categoria ON transacao (categoria_id);
```

**Notas críticas:**

- **3 FOREIGN KEYs**, sem `ON DELETE` explícito (default RESTRICT — não permite deletar conta com transações). Decisão alinhada com `decisoes.md` regra dura: deletar conta com transações é destrutivo.
- **CHECK constraint duplica validação de domínio** (valor positivo + regras de transferência). Defesa em profundidade: domain valida na escrita, banco valida no commit. Se alguma das duas falhar, a outra pega. Custo zero, ganho de robustez.
- **Index composto `(conta_id, data_transacao DESC)`** antecipa query principal: "transações da conta X ordenadas por data descendente". Vai ser usada em listagem paginada na 3.7 e em saldo derivado na 3.8.
- **Index simples em `categoria_id`** para queries de relatório por categoria (futuras).
- **Sem index em `conta_destino_id`** — uso esperado é raro (só pra listar "transferências recebidas em conta X" como caso especial). Adicionar quando query justificar.

### TransacaoMapper

Mesmo padrão de `ContaMapper` e `CategoriaMapper`: `@Mapper(componentModel = "spring")`, métodos `default` (porque `Transacao` tem dois construtores não-inferíveis), conversão `Money` ↔ `MoneyEmbeddable`.

### TransacaoJpaRepository

Spring Data simples nesta etapa:

```java
public interface TransacaoJpaRepository extends JpaRepository<TransacaoEntity, UUID> {
    // Métodos derivados serão adicionados na 3.7 conforme use cases pedirem.
    // Esta etapa não adiciona nenhum custom — JpaRepository default basta.
}
```

**Hard line: NÃO adicionar métodos derivados nesta etapa.** Mesmo motivo do repository: filtros vêm com a 3.7.

### TransacaoRepositoryImpl

Padrão idêntico aos anteriores:

```java
@Component
public class TransacaoRepositoryImpl implements TransacaoRepository {

    private final TransacaoJpaRepository jpaRepository;
    private final TransacaoMapper mapper;

    public TransacaoRepositoryImpl(TransacaoJpaRepository jpaRepository, TransacaoMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Transacao salvar(Transacao transacao) {
        TransacaoEntity entity = mapper.toEntity(transacao);
        TransacaoEntity salva = jpaRepository.save(entity);
        return mapper.toDomain(salva);
    }

    @Override
    public Optional<Transacao> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void deletar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
```

### Testes

**Domain (`TransacaoTest`)** — ~30 testes cobrindo:

Construtor "novo":
1. Constrói RECEITA válida (sem contaDestinoId, com categoriaId)
2. Constrói RECEITA válida (sem categoriaId — opcional)
3. Constrói DESPESA válida (sem contaDestinoId, com categoriaId)
4. Constrói TRANSFERENCIA válida (com contaDestinoId, sem categoriaId)
5. id é gerado, criadoEm em janela, atualizadoEm == criadoEm
6. Lança `NullPointerException` quando tipo é null
7. Lança `NullPointerException` quando valor é null
8. Lança `NullPointerException` quando data é null
9. Lança `NullPointerException` quando descricao é null
10. Lança `IllegalArgumentException` quando descricao é blank
11. Lança `IllegalArgumentException` quando descricao > 200 chars
12. Lança `NullPointerException` quando contaId é null
13. Lança `IllegalArgumentException` quando valor é zero
14. Lança `IllegalArgumentException` quando valor é negativo
15. Aceita descricao com 200 chars exatos
16. Aceita descricao com 1 char não-blank

Validações cruzadas:
17. RECEITA com contaDestinoId não-null → `IllegalArgumentException`
18. DESPESA com contaDestinoId não-null → `IllegalArgumentException`
19. TRANSFERENCIA com contaDestinoId null → `IllegalArgumentException`
20. TRANSFERENCIA com contaDestinoId == contaId → `IllegalArgumentException`
21. TRANSFERENCIA com categoriaId não-null → `IllegalArgumentException`
22. RECEITA com categoriaId null aceita
23. DESPESA com categoriaId null aceita

Construtor "reconstrução":
24. Reconstrói preservando todos os campos
25. Lança `NullPointerException` quando id null
26. Lança `NullPointerException` quando criadoEm null
27. Aceita atualizadoEm null (defaulta para criadoEm)

Igualdade e toString:
28. Duas transações com mesmo id são iguais
29. Ids diferentes não são iguais
30. toString contém id, tipo, valor (não polui com timestamps)

**Repository (`TransacaoRepositoryImplTest`)** — ~10 testes de integração com Testcontainers:

1. Salvar e recuperar RECEITA preserva todos os campos
2. Salvar e recuperar DESPESA com `Money` decimal exato
3. Salvar e recuperar TRANSFERENCIA com contaDestinoId preservado
4. Salvar e recuperar com `LocalDate` preservada (sem timezone shift)
5. Salvar e recuperar com `descricao` em 200 chars
6. BuscarPorId retorna vazio quando inexistente
7. Deletar remove do banco
8. Deletar id inexistente não lança (Spring Data ignora silenciosamente)
9. **Salvar viola FK quando contaId não existe** — espera `DataIntegrityViolationException` (ou subclasse)
10. **Salvar viola CHECK constraint quando enviado direto entity inválido** — bypassar domain validation criando `TransacaoEntity` direto com `valor_valor < 0`. Espera erro do banco. Documenta que CHECK é defesa em profundidade real.

**Importante:** ambos os testes 9 e 10 são **destrutivos por design** — testam que o banco rejeita lixo, não que o domain protege. Documentam que a defesa em profundidade funciona.

**Atenção a setup de teste 9 e 10:**

Para teste 9 (FK violada): construir `TransacaoEntity` direto (não via `Transacao` domain) com `contaId = UUID.randomUUID()` (qualquer UUID que não existe na tabela conta). Salvar via `jpaRepository`, esperar exceção do tipo `DataIntegrityViolationException`. **Importante:** não esperar pelo tipo exato (`ConstraintViolationException` do Hibernate vs `PSQLException` cru) — Spring envolve em `DataIntegrityViolationException`. Usar `assertThatThrownBy(...).isInstanceOf(DataIntegrityViolationException.class)`.

Para teste 10 (CHECK violada): mesmo padrão, mas com `valor_valor = BigDecimal("-50.00")` direto na entity. Esperar mesma exceção genérica.

**Setup geral dos testes de integração:**

`@AfterEach` precisa limpar `transacao` ANTES de `categoria` e `conta` (ordem importa por causa das FKs). Padrão:

```java
@AfterEach
void limpar() {
    transacaoJpaRepository.deleteAll();
    contaJpaRepository.deleteAll();
    categoriaJpaRepository.deleteAll();
}
```

Os testes 1-8 precisam de `Conta` e `Categoria` reais em banco antes de criar transação. **Setup helper:**

```java
private UUID criarContaPersistida() {
    Conta conta = new Conta("Conta de Teste", TipoConta.CORRENTE, new Money(BigDecimal.ZERO, BRL));
    contaRepositoryImpl.salvar(conta);
    return conta.getId();
}

private UUID criarCategoriaPersistida(TipoCategoria tipo) {
    Categoria cat = new Categoria("Categoria " + tipo, tipo);
    categoriaRepositoryImpl.salvar(cat);
    return cat.getId();
}
```

Injetar `ContaRepositoryImpl` e `CategoriaRepositoryImpl` via `@Autowired` no teste para usar nos helpers.

### JaCoCo

Todos os thresholds já ativos. Sem alteração no `pom.xml`. Cobertura esperada de domain ≥95%, infra ≥80%.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra commit do squash da Etapa 3.5 com referência a PR #34
- `docs/prompt-etapa-3-6.md` presente como untracked
- Working tree limpo
- Pacote `com.laboratorio.financas.transacao` **não existe ainda**
- Migrações: V1, V2, V3 (V4 será criada nesta etapa)

Validar com:

```bash
git status
git log --oneline -1
ls docs/prompt-etapa-3-6.md
ls -la src/main/java/com/laboratorio/financas/transacao/ 2>/dev/null && echo "ATENCAO" || echo "OK"
ls src/main/resources/db/migration/
```

Se algum item divergir, parar e reportar.

## Tarefas

### Tarefa 1 — Validar pré-requisitos

```bash
git status
git log --oneline -3
ls docs/prompt-etapa-3-6.md
ls -la src/main/java/com/laboratorio/financas/transacao/ 2>/dev/null && echo "ATENCAO" || echo "OK"
ls -la src/main/java/com/laboratorio/financas/conta/
ls -la src/main/java/com/laboratorio/financas/categoria/
ls src/main/resources/db/migration/
```

Esperado:
- `transacao/` não existe
- `conta/` e `categoria/` completos
- Migrations: V1, V2, V3

### Tarefa 2 — Criar branch de trabalho

```bash
git checkout -b feat/transacao-domain-infra
```

### Tarefa 3 — Antes de escrever, ler código vivo

Ler obrigatoriamente em disco antes de implementar (lição consolidada da 3.4 e reforçada na 3.5):

```bash
cat src/main/java/com/laboratorio/financas/conta/domain/Conta.java
cat src/main/java/com/laboratorio/financas/categoria/domain/Categoria.java
cat src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaEntity.java
cat src/main/java/com/laboratorio/financas/categoria/infrastructure/persistence/CategoriaEntity.java
cat src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaMapper.java
cat src/main/java/com/laboratorio/financas/shared/infrastructure/persistence/MoneyEmbeddable.java
cat src/main/resources/db/migration/V2__cria_tabela_conta.sql
```

Replicar fielmente os padrões. **Código vivo > esboço do prompt** quando divergirem.

### Tarefa 4 — Criar domain

**4a.** `transacao/domain/TipoTransacao.java`

```java
package com.laboratorio.financas.transacao.domain;

public enum TipoTransacao {
    RECEITA,
    DESPESA,
    TRANSFERENCIA
}
```

**4b.** `transacao/domain/Transacao.java`

Class final imutável com 10 campos. Estrutura completa:

```java
package com.laboratorio.financas.transacao.domain;

import com.laboratorio.financas.shared.domain.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Transacao {

    private static final int DESCRICAO_MAX_LENGTH = 200;

    private final UUID id;
    private final TipoTransacao tipo;
    private final Money valor;
    private final LocalDate data;
    private final String descricao;
    private final UUID contaId;
    private final UUID contaDestinoId;
    private final UUID categoriaId;
    private final Instant criadoEm;
    private final Instant atualizadoEm;

    /**
     * Construtor para criar nova Transacao. Gera id, define criadoEm=atualizadoEm=now.
     */
    public Transacao(
            TipoTransacao tipo,
            Money valor,
            LocalDate data,
            String descricao,
            UUID contaId,
            UUID contaDestinoId,
            UUID categoriaId
    ) {
        this(
                UUID.randomUUID(),
                tipo,
                valor,
                data,
                descricao,
                contaId,
                contaDestinoId,
                categoriaId,
                Instant.now(),
                null
        );
    }

    /**
     * Construtor de reconstrucao. Usado pelo repository para hidratar instancia persistida.
     */
    public Transacao(
            UUID id,
            TipoTransacao tipo,
            Money valor,
            LocalDate data,
            String descricao,
            UUID contaId,
            UUID contaDestinoId,
            UUID categoriaId,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
        Objects.requireNonNull(valor, "valor nao pode ser nulo");
        Objects.requireNonNull(data, "data nao pode ser nula");
        Objects.requireNonNull(contaId, "contaId nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarDescricao(descricao);
        validarValor(valor);
        validarRegrasDeTransferencia(tipo, contaId, contaDestinoId, categoriaId);

        this.id = id;
        this.tipo = tipo;
        this.valor = valor;
        this.data = data;
        this.descricao = descricao.trim();
        this.contaId = contaId;
        this.contaDestinoId = contaDestinoId;
        this.categoriaId = categoriaId;
        this.criadoEm = criadoEm;
        this.atualizadoEm = (atualizadoEm != null) ? atualizadoEm : criadoEm;
    }

    private static void validarDescricao(String descricao) {
        Objects.requireNonNull(descricao, "descricao nao pode ser nula");
        String trimmed = descricao.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("descricao nao pode ser vazia");
        }
        if (trimmed.length() > DESCRICAO_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "descricao nao pode ter mais de " + DESCRICAO_MAX_LENGTH + " caracteres"
            );
        }
    }

    private static void validarValor(Money valor) {
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException("valor deve ser positivo");
        }
    }

    private static void validarRegrasDeTransferencia(
            TipoTransacao tipo,
            UUID contaId,
            UUID contaDestinoId,
            UUID categoriaId
    ) {
        if (tipo == TipoTransacao.TRANSFERENCIA) {
            if (contaDestinoId == null) {
                throw new IllegalArgumentException(
                        "TRANSFERENCIA exige contaDestinoId"
                );
            }
            if (contaId.equals(contaDestinoId)) {
                throw new IllegalArgumentException(
                        "TRANSFERENCIA nao pode ter contaId igual a contaDestinoId"
                );
            }
            if (categoriaId != null) {
                throw new IllegalArgumentException(
                        "TRANSFERENCIA nao deve ter categoriaId"
                );
            }
        } else {
            if (contaDestinoId != null) {
                throw new IllegalArgumentException(
                        tipo + " nao deve ter contaDestinoId"
                );
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public Money getValor() {
        return valor;
    }

    public LocalDate getData() {
        return data;
    }

    public String getDescricao() {
        return descricao;
    }

    public UUID getContaId() {
        return contaId;
    }

    public UUID getContaDestinoId() {
        return contaDestinoId;
    }

    public UUID getCategoriaId() {
        return categoriaId;
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
        if (!(o instanceof Transacao other)) {
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
        return "Transacao{id=" + id + ", tipo=" + tipo + ", valor=" + valor + ", data=" + data + "}";
    }
}
```

**Notas:**
- Indentação Checkstyle: 16 espaços nas continuações.
- Sem acentos, sem Lombok.
- `validarValor` precisa ser chamada **depois** de `Objects.requireNonNull(valor, ...)` — agente atento à ordem.
- Encoding UTF-8 sem BOM.

**4c.** `transacao/domain/TransacaoRepository.java`

```java
package com.laboratorio.financas.transacao.domain;

import java.util.Optional;
import java.util.UUID;

public interface TransacaoRepository {

    Transacao salvar(Transacao transacao);

    Optional<Transacao> buscarPorId(UUID id);

    void deletar(UUID id);
}
```

### Tarefa 5 — Criar testes do domain

`test/.../transacao/domain/TransacaoTest.java`

~30 testes conforme listado em "Testes" acima. Naming camelCase puro. Usar constantes no topo:

```java
private static final Currency BRL = Currency.getInstance("BRL");
private static final Money VALOR_100 = new Money(new BigDecimal("100.00"), BRL);
private static final LocalDate HOJE = LocalDate.now();
private static final UUID CONTA_A = UUID.randomUUID();
private static final UUID CONTA_B = UUID.randomUUID();
private static final UUID CATEGORIA = UUID.randomUUID();
```

### Tarefa 6 — Criar infra

**6a.** `transacao/infrastructure/persistence/TransacaoEntity.java` conforme spec na seção "Persistência" acima.

**6b.** `transacao/infrastructure/persistence/TransacaoMapper.java`

```java
package com.laboratorio.financas.transacao.infrastructure.persistence;

import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.shared.infrastructure.persistence.MoneyEmbeddable;
import com.laboratorio.financas.transacao.domain.Transacao;
import java.math.BigDecimal;
import java.util.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransacaoMapper {

    default TransacaoEntity toEntity(Transacao transacao) {
        if (transacao == null) {
            return null;
        }
        return new TransacaoEntity(
                transacao.getId(),
                transacao.getTipo(),
                toMoneyEmbeddable(transacao.getValor()),
                transacao.getData(),
                transacao.getDescricao(),
                transacao.getContaId(),
                transacao.getContaDestinoId(),
                transacao.getCategoriaId(),
                transacao.getCriadoEm(),
                transacao.getAtualizadoEm()
        );
    }

    default Transacao toDomain(TransacaoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Transacao(
                entity.getId(),
                entity.getTipo(),
                toMoney(entity.getValor()),
                entity.getData(),
                entity.getDescricao(),
                entity.getContaId(),
                entity.getContaDestinoId(),
                entity.getCategoriaId(),
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

**6c.** `transacao/infrastructure/persistence/TransacaoJpaRepository.java`

```java
package com.laboratorio.financas.transacao.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransacaoJpaRepository extends JpaRepository<TransacaoEntity, UUID> {
    // Metodos derivados serao adicionados na 3.7 conforme use cases pedirem.
}
```

**6d.** `transacao/infrastructure/persistence/TransacaoRepositoryImpl.java` conforme spec.

### Tarefa 7 — Criar migration V4

`src/main/resources/db/migration/V4__cria_tabela_transacao.sql` conforme spec na seção "Migration V4".

### Tarefa 8 — Criar teste de integração

`test/.../transacao/infrastructure/persistence/TransacaoRepositoryImplTest.java`

~10 testes conforme spec. **Atenção especial aos testes 9 e 10** (FK + CHECK violadas) que comprovam defesa em profundidade.

### Tarefa 9 — Validar localmente

```bash
.\mvnw.cmd compile

# Testes da etapa primeiro:
.\mvnw.cmd test -Dtest='Transacao*Test'

# Build completo:
.\mvnw.cmd verify
```

**Esperado:**
- BUILD SUCCESS
- ~40 novos testes passando (~210 total)
- Checkstyle 0 violações, SpotBugs 0 issues
- JaCoCo todos os thresholds atendidos
- Migration V4 aplicada nos testes via Testcontainers

**Possíveis pontos de atrito:**

1. **`LocalDate` precisa de `@Column` sem `columnDefinition`** — Hibernate mapeia pra `DATE` automaticamente. Forçar `columnDefinition = "date"` redundante.
2. **CHECK constraint pode rejeitar string com aspas simples no enum** — testar que `tipo = 'TRANSFERENCIA'` (string entre aspas simples) está correto no SQL, não dupla.
3. **`Money` no `Transacao` exige import de `BigDecimal` e `Currency`** — atenção aos imports no mapper.
4. **Teste 9 (FK violada)**: Spring Data envolve `ConstraintViolationException` em `DataIntegrityViolationException`. Importar `org.springframework.dao.DataIntegrityViolationException`.
5. **Ordem de cleanup no `@AfterEach`**: transacao primeiro (filha), depois categoria e conta (pais). Inverter quebra com FK.

### Tarefa 10 — Atualizar `docs/decisoes.md`

Adicionar entrada no histórico:

```markdown
- **2026-05-09** — Etapa 3.6 concluída: bounded context `transacao` — domain + infra. Entidade `Transacao` (10 campos, validações cruzadas RECEITA/DESPESA/TRANSFERENCIA), enum `TipoTransacao`, repository (3 métodos básicos — filtros vêm na 3.7), `TransacaoEntity` (FKs por UUID, sem `@ManyToOne`), `TransacaoMapper` (MapStruct, `default` methods), `V4__cria_tabela_transacao.sql` (3 FKs + 2 CHECK constraints como defesa em profundidade), 40 testes. Application e interfaces ficam para 3.7. Mergeado via PR #XX.
```

**Adicionar nota em "Padrões aplicados"** (decisão arquitetural relevante):

```markdown
- **Relacionamentos entre bounded contexts via UUID, não @ManyToOne** (a partir da Etapa 3.6): entidades JPA referenciam outros agregados por `UUID` direto (`@Column(name = "conta_id")`), não por `@ManyToOne ContaEntity`. FK constraint existe no banco, validação de existência fica no use case. Razões: baixo acoplamento entre bounded contexts, evita `LazyInitializationException`, queries mais previsíveis. Padrão idiomático em DDD com bounded contexts.
- **Defesa em profundidade no banco** (a partir da Etapa 3.6): regras de domínio importantes (valor positivo, regras de transferência) são duplicadas em `CHECK` constraints do banco. Domain valida na escrita, banco valida no commit. Custo zero, ganho de robustez contra bypass do domain.
```

### Tarefa 11 — Atualizar `docs/progresso.md`

**11a.** Atualizar "Última atualização": `2026-05-09 (Etapa 3.6 — transacao domain+infra)`.

**11b.** Adicionar seção "Lições da Etapa 3.6" com candidatos a hook + lições reais. Não inventar.

**11c.** Adicionar entrada no histórico:

```markdown
- **2026-05-09** — Etapa 3.6 concluída: domain + infra de `transacao`. Entidade com validações cruzadas, V4 com FKs e CHECK constraints, ~40 testes. Mergeado via PR #XX.
```

### Tarefa 12 — Versionar este próprio prompt

`docs/prompt-etapa-3-6.md` no commit de docs.

### Tarefa 13 — Validação final antes de commitar

```bash
find src/main/java/com/laboratorio/financas/transacao -name "*.java" -exec sh -c 'echo "$1:"; xxd "$1" | head -1' _ {} \;
find src/test/java/com/laboratorio/financas/transacao -name "*.java" -exec sh -c 'echo "$1:"; xxd "$1" | head -1' _ {} \;
xxd src/main/resources/db/migration/V4__cria_tabela_transacao.sql | head -1

.\mvnw.cmd verify

git status
```

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Permitidos:
   - Pasta nova `transacao/` (domain + infrastructure/persistence + tests)
   - `db/migration/V4__cria_tabela_transacao.sql` (criação)
   - `docs/decisoes.md` (histórico + 2 padrões novos)
   - `docs/progresso.md` (lições + histórico)
   - `docs/prompt-etapa-3-6.md` (versionar)

2. **Não tocar em `conta/`, `categoria/`, `shared/`.** Sem "harmonização" cross-context.

3. **Não tocar em `pom.xml`, `application*.yml`, `docker-compose.yml`, `scripts/*.ps1`.**

4. **Não criar `application/` em `transacao/`.** Use cases ficam pra 3.7.

5. **Não criar `interfaces/` em `transacao/`.** Controller, DTOs ficam pra 3.7.

6. **Não criar `TransacaoNaoEncontradaException` nesta etapa.** Sem use cases, sem necessidade. Entra na 3.7.

7. **Não criar métodos derivados em `TransacaoJpaRepository`.** Repository nesta etapa tem 3 métodos: salvar, buscarPorId, deletar. Filtros vêm com 3.7.

8. **Não criar `listarPorConta`, `listarPorPeriodo`, `listarPaginado`** em `TransacaoRepository`. Adivinhar shape antes do use case usar é especulação.

9. **Não criar relacionamento `@ManyToOne` com `ContaEntity` ou `CategoriaEntity`.** Decisão explícita: FKs por UUID solto.

10. **Não tocar em `GlobalExceptionHandler` ou `SecurityConfig`.** Esses ganham handler/whitelist na 3.7 quando o controller existir.

11. **Não usar Lombok.**

12. **Não relaxar Checkstyle, SpotBugs, JaCoCo.**

13. **Sem acentos no código Java.**

14. **Encoding UTF-8 sem BOM.**

15. **Naming de teste em camelCase puro.**

16. **Indentação Checkstyle**: 16 espaços nas continuações, getters em bloco, chaves obrigatórias.

17. **Antes de escrever cada classe, ler a contraparte de `conta/` ou `categoria/`** (Tarefa 3). Código vivo > prosa do prompt.

18. **Lições da Etapa 3.6 só registram observações reais.** Não inventar.

19. **Não antecipar Etapa 3.7 ou 3.8.** Sem rascunhar use cases, controller, saldo derivado.

20. **Não tomar decisão silenciosa em zona limítrofe.** Padrão consolidado.

## Estrutura de commits

Branch: `feat/transacao-domain-infra`

**Commit 1** — `feat(transacao): adiciona domain (Transacao, TipoTransacao, repository interface)`
- 3 arquivos em `transacao/domain/`

**Commit 2** — `test(transacao): cobertura do domain (Transacao com validacoes cruzadas)`
- 1 arquivo em `test/.../transacao/domain/`

**Commit 3** — `feat(transacao): adiciona infra (entity, mapper, jpa repo, repo impl) + migration V4`
- 4 arquivos em `transacao/infrastructure/persistence/`
- `db/migration/V4__cria_tabela_transacao.sql`

**Commit 4** — `test(transacao): cobertura de integracao com FK e CHECK constraints`
- 1 arquivo em `test/.../transacao/infrastructure/persistence/`

**Commit 5** — `docs: registra etapa 3.6 (transacao domain+infra) em decisoes e progresso`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-3-6.md`

## Validação antes de abrir PR

```bash
.\mvnw.cmd verify
git status
git log --oneline -6
```

Esperado: BUILD SUCCESS, working tree limpo, 5 commits.

## PR

Título: `feat: etapa 3.6 — bounded context transacao (domain + infra)`

Body sugerido:

```markdown
## Summary

Implementa a Etapa 3.6 do roadmap (primeira metade do bounded context `transacao`): domain + infra. Application e interfaces ficam para 3.7. Saldo derivado fica para 3.8 dedicada.

### Padrões que estreiam aqui

1. **Primeira FK em JPA** — `transacao` referencia `Conta` e `Categoria` por UUID solto (não `@ManyToOne`). FK constraint existe no banco; validação de existência fica no use case (3.7).
2. **Primeiras validações cruzadas no domain** — `tipo == TRANSFERENCIA` exige `contaDestinoId != null && != contaId && categoriaId == null`; demais tipos rejeitam `contaDestinoId`.
3. **Primeira migration com FOREIGN KEYs e CHECK constraints** — V4 com 3 FKs (conta, conta_destino, categoria) + 2 CHECKs (valor positivo, regras de transferência) como defesa em profundidade.
4. **Primeiro mapeamento `LocalDate` ↔ `DATE`** em Postgres.
5. **Padrão "relacionamento entre bounded contexts via UUID"** consolidado e documentado em `decisoes.md`.

### Mudanças

- `transacao/domain/Transacao.java`: class final imutável com 10 campos, dois construtores, validações cruzadas no construtor de reconstrução.
- `transacao/domain/TipoTransacao.java`: enum (RECEITA, DESPESA, TRANSFERENCIA).
- `transacao/domain/TransacaoRepository.java`: interface com 3 métodos básicos (salvar, buscarPorId, deletar). Filtros virão na 3.7.
- `transacao/infrastructure/persistence/TransacaoEntity.java`: `@Entity` com FKs por UUID solto, `@AttributeOverride` mapeando `MoneyEmbeddable`.
- `transacao/infrastructure/persistence/TransacaoMapper.java`: MapStruct com `default` methods.
- `transacao/infrastructure/persistence/TransacaoJpaRepository.java`: Spring Data básico, sem custom methods nesta etapa.
- `transacao/infrastructure/persistence/TransacaoRepositoryImpl.java`: delega a JpaRepository via mapper.
- `db/migration/V4__cria_tabela_transacao.sql`: tabela com 11 colunas, 3 FKs, 2 CHECKs, 2 índices.
- ~30 testes de domain + ~10 de integração (~40 total novos).

### Decisões de escopo

- **Application + interfaces ficam para 3.7.** Sem use cases, controller, DTOs nesta etapa.
- **Saldo derivado fica para 3.8 dedicada.**
- **FK por UUID, não `@ManyToOne`.** Baixo acoplamento entre bounded contexts.
- **CHECK constraints duplicam validação de domínio.** Defesa em profundidade.
- **Repository com 3 métodos só.** Filtros vêm com use cases na 3.7. Adivinhar shape agora é especulação.

### Validação

- `mvnw verify` local: PASSOU
- ~40 testes novos passando, incluindo 2 testes destrutivos validando que FK e CHECK rejeitam input inválido no nível do banco
- Checkstyle: 0 violações, SpotBugs: 0 issues
- JaCoCo: todos os thresholds atendidos

### Próximo passo

Etapa 3.7 (application + interfaces de `transacao` — use cases com validação cruzada de FKs, controller com paginação, PUT pra editar) — fora do escopo deste PR.
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

- Branch `feat/transacao-domain-infra` empurrada com 6 commits (5 + 1 update do PR)
- PR aberto, CI verde, **não mergeado**
- `main` ainda no squash da 3.5
- Working tree limpo
- Reportar com `git log --oneline -6`, `git status`, `gh pr view --json number,state,statusCheckRollup` e parar.

## O que NÃO fazer ao terminar

- Não mergear o PR.
- Não criar prompt da próxima etapa.
- Não rascunhar 3.7 ou 3.8.
- Não tocar em `frontend/`, `scripts/`, `docker-compose.yml`, `conta/`, `categoria/`, `shared/`, `pom.xml`.
- Não sugerir "próximo passo" espontaneamente.
