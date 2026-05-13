# Prompt -- Sub-etapa 5.3: Bounded context `meta` (objetivos financeiros)

## Contexto

Segunda sub-etapa da Camada 4. Entrega o bounded context `meta` — objetivos financeiros
com acompanhamento de depositos e progresso. Fecha o loop com `orcamento`: enquanto
`orcamento` controla gastos por categoria, `meta` controla poupanca por objetivo.

Padrao novo: dois campos `Money` na mesma Entity (`valorAlvo` e `valorAtual`), exigindo
dois `@Embedded @AttributeOverride` distintos para o mesmo tipo `MoneyEmbeddable`.

Lembrete de workflow: executor le este arquivo do disco e commita o `.md` junto com
os docs no ultimo commit da branch.

---

## Escopo decidido

### Bounded context: `meta`

**Package base:** `com.laboratorio.financas.meta`

---

### Arquivo 1: `meta/domain/Meta.java` (IMPLEMENTAR sobre stub do /feature)

Campos:
- `id`: UUID — `UUID.randomUUID()` no construtor
- `nome`: String
- `valorAlvo`: Money — alvo a atingir
- `valorAtual`: Money — acumulado de depositos (inicia em `new Money(BigDecimal.ZERO, valorAlvo.moeda())`)
- `prazo`: LocalDate
- `status`: StatusMeta — `EM_ANDAMENTO` no construtor
- `criadoEm`: Instant — `Instant.now()` no construtor
- `atualizadoEm`: Instant — `Instant.now()` no construtor

Validacoes no construtor (lancam `IllegalArgumentException`):
- `nome != null && !nome.isBlank()`
- `valorAlvo != null`
- `valorAlvo.valor().compareTo(BigDecimal.ZERO) > 0`
- `prazo != null`

Metodos:
- `registrarDeposito(Money deposito)`:
  - Se `status != EM_ANDAMENTO` → lanca `IllegalStateException("Meta nao esta em andamento")`
  - `deposito != null` e `deposito.valor().compareTo(BigDecimal.ZERO) > 0` → `IllegalArgumentException` se nao
  - Se `!deposito.moeda().equals(valorAlvo.moeda())` → `IllegalArgumentException("Moeda do deposito deve ser igual a moeda do valorAlvo")`
  - `valorAtual = new Money(valorAtual.valor().add(deposito.valor()), valorAtual.moeda())`
  - Se `valorAtual.valor().compareTo(valorAlvo.valor()) >= 0` → `status = StatusMeta.CONCLUIDA`
  - `atualizadoEm = Instant.now()`
- `cancelar()`: se `status == CONCLUIDA` → lanca `IllegalStateException("Meta ja concluida nao pode ser cancelada")`; senao `status = CANCELADA; atualizadoEm = Instant.now()`
- Getters para todos os campos (sem setters publicos)

---

### Arquivo 2: `meta/domain/MetaRepository.java` (IMPLEMENTAR sobre stub do /feature)

```java
Meta salvar(Meta meta);
Optional<Meta> buscarPorId(UUID id);
List<Meta> listar();
Meta atualizar(Meta meta);
```

---

### Arquivo 3: `meta/domain/MetaNaoEncontradaException.java` (IMPLEMENTAR sobre stub do /feature)

`RuntimeException` com mensagem `"Meta nao encontrada: " + id`.

---

### Arquivo 4: `meta/domain/StatusMeta.java` (NOVO — NAO gerado pelo /feature)

```java
public enum StatusMeta {
    EM_ANDAMENTO,
    CONCLUIDA,
    CANCELADA
}
```

---

### Arquivo 5: `meta/application/CriarMetaUseCase.java` (IMPLEMENTAR sobre stub do /feature)

- Inner record `Comando(String nome, BigDecimal valorAlvoValor, String valorAlvoMoeda, LocalDate prazo)`
- Cria `Money valorAlvo = new Money(valorAlvoValor, Currency.getInstance(valorAlvoMoeda))`
- Cria `new Meta(nome, valorAlvo, prazo)` e salva via `metaRepository.salvar(meta)`
- Retorna `Meta`

---

### Arquivo 6: `meta/application/ListarMetasUseCase.java` (NOVO)

Retorna `List<Meta>` via `metaRepository.listar()`.

---

### Arquivo 7: `meta/application/BuscarMetaPorIdUseCase.java` (NOVO)

Aceita `UUID id`. Retorna `Meta` ou lanca `MetaNaoEncontradaException(id)`.

---

### Arquivo 8: `meta/application/RegistrarDepositoEmMetaUseCase.java` (NOVO)

- Inner record `Comando(UUID metaId, BigDecimal depositoValor, String depositoMoeda)`
- Busca meta (lanca `MetaNaoEncontradaException` se nao encontrada)
- Cria `Money deposito = new Money(depositoValor, Currency.getInstance(depositoMoeda))`
- Chama `meta.registrarDeposito(deposito)`
- Chama `metaRepository.atualizar(meta)`
- Retorna `Meta` atualizada

---

### Arquivo 9: `meta/application/CancelarMetaUseCase.java` (NOVO)

- Aceita `UUID id`
- Busca meta (lanca `MetaNaoEncontradaException` se nao encontrada)
- Chama `meta.cancelar()`
- Chama `metaRepository.atualizar(meta)`
- Retorna void

---

### Arquivo 10: `meta/infrastructure/persistence/MetaEntity.java` (IMPLEMENTAR sobre stub do /feature)

**ATENCAO -- padrao novo:** dois campos `MoneyEmbeddable` na mesma Entity exigem
`@AttributeOverride` distinto em cada um. Leia `ContaEntity.java` para ver o padrao
de um campo, e aplique para dois:

```java
@Entity
@Table(name = "meta")
public class MetaEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @NotNull
    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "valor_alvo_valor", nullable = false, precision = 19, scale = 2))
    @AttributeOverride(name = "moeda", column = @Column(name = "valor_alvo_moeda", nullable = false, length = 3))
    private MoneyEmbeddable valorAlvo;

    @NotNull
    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "valor_atual_valor", nullable = false, precision = 19, scale = 2))
    @AttributeOverride(name = "moeda", column = @Column(name = "valor_atual_moeda", nullable = false, length = 3))
    private MoneyEmbeddable valorAtual;

    @NotNull
    @Column(name = "prazo", nullable = false)
    private LocalDate prazo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusMeta status;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    // construtor protected JPA, construtor com todos os campos, getters
}
```

---

### Arquivo 11: `meta/infrastructure/persistence/MetaJpaRepository.java` (IMPLEMENTAR sobre stub)

`extends JpaRepository<MetaEntity, UUID>` — sem `@Query` customizado para 5.3.

---

### Arquivo 12: `meta/infrastructure/persistence/MetaMapper.java` (IMPLEMENTAR sobre stub)

MapStruct. Leia `OrcamentoMapper.java` antes — mesmo padrao de conversao `MoneyEmbeddable <-> Money`.
Agora com dois campos Money (`valorAlvo` e `valorAtual`). Seguir o mesmo padrao de mapeamento.
Tambem mapear o enum `StatusMeta`.

---

### Arquivo 13: `meta/infrastructure/persistence/MetaRepositoryImpl.java` (IMPLEMENTAR sobre stub)

Mesmo padrao de `OrcamentoRepositoryImpl`:
- `salvar`: toEntity → save → toMeta
- `atualizar`: identico ao salvar
- `buscarPorId`: findById → map(mapper::toMeta)
- `listar`: findAll → stream → map → toList

---

### Arquivo 14: Migration SQL (gerado pelo `/migrate`)

Versao esperada: **V6** (atual maximo e V5 — verificar antes com glob).

Conteudo esperado:
```sql
CREATE TABLE meta (
    id UUID PRIMARY KEY,
    nome VARCHAR(200) NOT NULL,
    valor_alvo_valor NUMERIC(19,2) NOT NULL,
    valor_alvo_moeda VARCHAR(3) NOT NULL,
    valor_atual_valor NUMERIC(19,2) NOT NULL DEFAULT 0,
    valor_atual_moeda VARCHAR(3) NOT NULL,
    prazo DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'EM_ANDAMENTO',
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL
);
```

---

### Arquivo 15: `meta/interfaces/dto/CriarMetaRequest.java` (IMPLEMENTAR sobre stub)

```java
record CriarMetaRequest(
    @NotBlank String nome,
    @NotNull BigDecimal valorAlvoValor,
    @NotNull @Size(min = 3, max = 3) String valorAlvoMoeda,
    @NotNull @FutureOrPresent LocalDate prazo
) {}
```

---

### Arquivo 16: `meta/interfaces/dto/RegistrarDepositoRequest.java` (NOVO — NAO gerado pelo /feature)

```java
record RegistrarDepositoRequest(
    @NotNull @Positive BigDecimal valor,
    @NotNull @Size(min = 3, max = 3) String moeda
) {}
```

---

### Arquivo 17: `meta/interfaces/dto/MetaResponse.java` (IMPLEMENTAR sobre stub)

**Antes de implementar:** leia `OrcamentoResponse.java` para ver o padrao de `ValorMonetario`
e o padrao de conversao inline. Leia `ContaController.java` para ver o padrao `fromDomain()`.

```java
record MetaResponse(
    UUID id,
    String nome,
    ValorMonetario valorAlvo,
    ValorMonetario valorAtual,
    LocalDate prazo,
    String status,
    boolean atrasada,
    BigDecimal percentualConcluido,
    Instant criadoEm,
    Instant atualizadoEm
) {
    record ValorMonetario(BigDecimal valor, String moeda) {}

    static MetaResponse fromDomain(Meta meta) {
        BigDecimal percentual = meta.getValorAlvo().valor().compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : meta.getValorAtual().valor()
                .multiply(BigDecimal.valueOf(100))
                .divide(meta.getValorAlvo().valor(), 2, RoundingMode.HALF_UP);

        boolean atrasada = meta.getStatus() == StatusMeta.EM_ANDAMENTO
            && LocalDate.now().isAfter(meta.getPrazo());

        return new MetaResponse(
            meta.getId(),
            meta.getNome(),
            new ValorMonetario(meta.getValorAlvo().valor(), meta.getValorAlvo().moeda().getCurrencyCode()),
            new ValorMonetario(meta.getValorAtual().valor(), meta.getValorAtual().moeda().getCurrencyCode()),
            meta.getPrazo(),
            meta.getStatus().name(),
            atrasada,
            percentual,
            meta.getCriadoEm(),
            meta.getAtualizadoEm()
        );
    }
}
```

---

### Arquivo 18: `meta/interfaces/MetaController.java` (IMPLEMENTAR sobre stub)

5 endpoints. Verifique `GlobalExceptionHandler` para mapeamento de `MetaNaoEncontradaException` → 404
e `IllegalStateException` → 400 (se ja houver handler; se nao, adicionar).

| Metodo | Path | Status | Corpo |
|--------|------|--------|-------|
| POST | /api/metas | 201 | MetaResponse |
| GET | /api/metas | 200 | List<MetaResponse> |
| GET | /api/metas/{id} | 200 | MetaResponse |
| DELETE | /api/metas/{id} | 204 | — |
| POST | /api/metas/{id}/depositos | 200 | MetaResponse |

DELETE chama `CancelarMetaUseCase` (soft cancel — muda status para CANCELADA).
POST depositos chama `RegistrarDepositoEmMetaUseCase` com o id da meta + corpo da request.
Conversao `Meta → MetaResponse` via `MetaResponse.fromDomain(meta)` (padrao ContaController).

Adicionar `/api/metas/**` ao `SecurityConfig` (mesma linha dos outros endpoints).

---

### Arquivo 19: `docs/progresso.md` (EDITAR)

Leia o arquivo antes de editar. Aplique as 3 mudancas.

**Mudanca 1 -- linha "Ultima atualizacao":**
```
**Última atualização:** 2026-05-12 (Sub-etapa 5.3 -- Bounded context meta)
```

**Mudanca 2 -- adicionar 5.3 em "Sub-etapas concluidas" da Camada 4 (logo antes de 5.2):**
```
- **5.3 -- Bounded context `meta`** (2026-05-12): segundo feature Tier 2.
  Objetivos financeiros com depositos e acompanhamento de progresso.
  Padrao novo: dois @Embedded MoneyEmbeddable na mesma Entity (valorAlvo + valorAtual).
  StatusMeta enum (EM_ANDAMENTO/CONCLUIDA/CANCELADA). Endpoint POST /depositos (sub-recurso).
  fromDomain() estatico no DTO para conversao inline. 5 endpoints, 4 commits. PR #76.
```

**Mudanca 3 -- adicionar em "Historico de mudancas":**
```
- **2026-05-12** -- Sub-etapa 5.3 concluida: bounded context `meta`. Segundo Tier 2. PR #76.
```

---

### Arquivo 20: `docs/decisoes-claude-code.md` (EDITAR)

Leia o arquivo antes de editar. Adicione nova secao antes de `## Historico de mudancas`:

```
## Sub-etapa 5.3 -- Bounded context `meta`

### Padrao: dois @Embedded do mesmo tipo na mesma Entity

Quando uma Entity tem dois campos Money (ex: `valorAlvo` e `valorAtual`), cada um
precisa de `@AttributeOverride` proprio com nomes de colunas distintos. Sem isso,
JPA levanta `HibernateException: column "valor" is mapped more than once`.

Exemplo:
```java
@Embedded
@AttributeOverride(name = "valor", column = @Column(name = "valor_alvo_valor", ...))
@AttributeOverride(name = "moeda", column = @Column(name = "valor_alvo_moeda", ...))
private MoneyEmbeddable valorAlvo;

@Embedded
@AttributeOverride(name = "valor", column = @Column(name = "valor_atual_valor", ...))
@AttributeOverride(name = "moeda", column = @Column(name = "valor_atual_moeda", ...))
private MoneyEmbeddable valorAtual;
```

### Decisao: fromDomain() estatico no DTO

`MetaResponse.fromDomain(Meta meta)` encapsula a conversao inline (percentualConcluido,
atrasada computados). Padrao herdado de ContaResponse. Preferido a mapper separado
para DTOs de interface -- logica de apresentacao fica no proprio DTO.

### Decisao: deposito como sub-recurso POST

`POST /api/metas/{id}/depositos` em vez de `PUT /api/metas/{id}` para registrar deposito.
Semantica correta: deposito e uma acao, nao uma atualizacao parcial do recurso.
```

Adicionar ao `## Historico de mudancas`:
```
- **2026-05-12** -- Sub-etapa 5.3 concluida: bounded context `meta`. Padrao dois @Embedded
  na mesma Entity. fromDomain() no DTO. POST sub-recurso para depositos. PR #76.
```

---

## Estado esperado ao iniciar

```powershell
git branch --show-current   # deve retornar: main
git status                  # deve retornar: nothing to commit, working tree clean
git log --oneline -3        # deve mostrar PR #75 (5.2) ou posterior no topo
```

---

## Tarefas

### Tarefa 1 -- Verificar estado inicial

```powershell
git branch --show-current
git status
git log --oneline -3
```

### Tarefa 2 -- Criar branch

```powershell
git checkout -b feat/etapa-5-3-meta
git branch --show-current
```

### Tarefa 3 -- Gerar skeleton via /feature

```
/feature meta
```

Pos-condicao:
```powershell
Test-Path "src/main/java/com/laboratorio/financas/meta/domain/Meta.java"
Test-Path "src/main/java/com/laboratorio/financas/meta/domain/MetaRepository.java"
Test-Path "src/main/java/com/laboratorio/financas/meta/domain/MetaNaoEncontradaException.java"
Test-Path "src/main/java/com/laboratorio/financas/meta/application/CriarMetaUseCase.java"
Test-Path "src/main/java/com/laboratorio/financas/meta/infrastructure/persistence/MetaEntity.java"
Test-Path "src/main/java/com/laboratorio/financas/meta/infrastructure/persistence/MetaJpaRepository.java"
Test-Path "src/main/java/com/laboratorio/financas/meta/infrastructure/persistence/MetaRepositoryImpl.java"
Test-Path "src/main/java/com/laboratorio/financas/meta/infrastructure/persistence/MetaMapper.java"
Test-Path "src/main/java/com/laboratorio/financas/meta/interfaces/MetaController.java"
Test-Path "src/main/java/com/laboratorio/financas/meta/interfaces/dto/CriarMetaRequest.java"
Test-Path "src/main/java/com/laboratorio/financas/meta/interfaces/dto/MetaResponse.java"
# todos devem retornar True
```

### Tarefa 4 -- Commit 1: skeleton

```powershell
git add "src/main/java/com/laboratorio/financas/meta/"
git status
```

Commit:
```
feat(meta): cria skeleton via /feature
```

### Tarefa 5 -- Implementar domain

1. `Meta.java` — conforme prescricao
2. `MetaRepository.java` — interface com 4 metodos
3. `MetaNaoEncontradaException.java` — RuntimeException com mensagem
4. `StatusMeta.java` — NOVO, enum com EM_ANDAMENTO, CONCLUIDA, CANCELADA

Pos-condicao:
```powershell
Test-Path "src/main/java/com/laboratorio/financas/meta/domain/StatusMeta.java"
# deve retornar: True

Select-String "registrarDeposito" "src/main/java/com/laboratorio/financas/meta/domain/Meta.java"
# deve ter match
```

### Tarefa 6 -- Implementar MetaEntity.java

**ATENCAO:** dois campos `@Embedded MoneyEmbeddable` na mesma classe exigem
`@AttributeOverride` com nomes de colunas distintos para cada um. Sem isso o JPA
levanta erro de coluna mapeada mais de uma vez. Use exatamente o padrao prescrito
no Arquivo 10.

Leia `ContaEntity.java` antes para ver o padrao de um campo, depois aplique para dois.

Pos-condicao:
```powershell
Select-String "valor_alvo_valor" "src/main/java/com/laboratorio/financas/meta/infrastructure/persistence/MetaEntity.java"
# deve ter match

Select-String "valor_atual_valor" "src/main/java/com/laboratorio/financas/meta/infrastructure/persistence/MetaEntity.java"
# deve ter match
```

### Tarefa 7 -- Gerar migration e unit tests via /migrate

```powershell
Get-ChildItem "src/main/resources/db/migration/" | Sort-Object Name
# deve listar V1-V5 -- proximo sera V6
```

```
/migrate meta
```

Pos-condicao:
```powershell
Test-Path "src/main/resources/db/migration/V6__cria_tabela_meta.sql"
# deve retornar: True

Test-Path "src/test/java/com/laboratorio/financas/meta/domain/MetaTest.java"
# deve retornar: True
```

Se migration-writer gerar versao diferente de V6: pare e reporte.

### Tarefa 8 -- Commit 2: domain + entity + migration + unit tests

```powershell
git add "src/main/java/com/laboratorio/financas/meta/domain/" `
        "src/main/java/com/laboratorio/financas/meta/infrastructure/persistence/MetaEntity.java" `
        "src/main/resources/db/migration/V6__cria_tabela_meta.sql" `
        "src/test/java/com/laboratorio/financas/meta/domain/MetaTest.java"
git status
```

Commit:
```
feat(meta): implementa domain e Entity; adiciona migration e unit tests via /migrate
```

### Tarefa 9 -- Implementar application layer (5 use cases)

1. `CriarMetaUseCase.java` — substitui stub
2. `ListarMetasUseCase.java` — NOVO
3. `BuscarMetaPorIdUseCase.java` — NOVO
4. `RegistrarDepositoEmMetaUseCase.java` — NOVO
5. `CancelarMetaUseCase.java` — NOVO

Pos-condicao:
```powershell
(Get-ChildItem "src/main/java/com/laboratorio/financas/meta/application/").Count
# deve retornar 5
```

### Tarefa 10 -- Commit 3: application layer

```powershell
git add "src/main/java/com/laboratorio/financas/meta/application/"
git status
```

Commit:
```
feat(meta): implementa application layer (5 use cases)
```

### Tarefa 11 -- Implementar infrastructure

Leia `OrcamentoMapper.java` antes de implementar `MetaMapper` (mesmo padrao, agora com dois campos Money).

1. `MetaMapper.java` — MapStruct, dois campos MoneyEmbeddable, enum StatusMeta
2. `MetaRepositoryImpl.java` — implementa MetaRepository

### Tarefa 12 -- Implementar interfaces

Leia `OrcamentoResponse.java` para ver o padrao ValorMonetario.
Leia `ContaController.java` para ver o padrao `fromDomain()`.
Verifique `GlobalExceptionHandler` — adicionar handler para `IllegalStateException` → 400 se nao existir.

1. `CriarMetaRequest.java` — substitui stub
2. `RegistrarDepositoRequest.java` — NOVO em `interfaces/dto/`
3. `MetaResponse.java` — substitui stub; inclui `fromDomain()` estatico
4. `MetaController.java` — 5 endpoints conforme tabela
5. `SecurityConfig` — adicionar `/api/metas/**`

### Tarefa 13 -- Gerar integration tests

```
/write-test src/main/java/com/laboratorio/financas/meta/infrastructure/persistence/MetaRepositoryImpl.java
```

Pos-condicao:
```powershell
Test-Path "src/test/java/com/laboratorio/financas/meta/infrastructure/persistence/MetaRepositoryImplTest.java"
# deve retornar: True
```

### Tarefa 14 -- Gerar E2E tests

```
/write-test src/main/java/com/laboratorio/financas/meta/interfaces/MetaController.java
```

Pos-condicao:
```powershell
Test-Path "src/test/java/com/laboratorio/financas/meta/interfaces/MetaControllerTest.java"
# deve retornar: True
```

### Tarefa 15 -- Gerar unit tests do use case de deposito

```
/write-test src/main/java/com/laboratorio/financas/meta/application/RegistrarDepositoEmMetaUseCase.java
```

Pos-condicao:
```powershell
Test-Path "src/test/java/com/laboratorio/financas/meta/application/RegistrarDepositoEmMetaUseCaseTest.java"
# deve retornar: True
```

### Tarefa 16 -- Validacao completa

```powershell
./mvnw verify
# BUILD SUCCESS obrigatorio antes de prosseguir
```

Se testes falharem: corrija antes de continuar.

### Tarefa 17 -- Atualizar docs

Aplique as mudancas prescritas nos Arquivos 19 e 20.
Leia cada arquivo antes de editar.

### Tarefa 18 -- Commit 4: infrastructure + interfaces + tests + docs + prompt

```powershell
git add `
  "src/main/java/com/laboratorio/financas/meta/infrastructure/" `
  "src/main/java/com/laboratorio/financas/meta/interfaces/" `
  "src/test/java/com/laboratorio/financas/meta/" `
  "src/main/java/com/laboratorio/financas/shared/interfaces/GlobalExceptionHandler.java" `
  "src/main/java/com/laboratorio/financas/shared/infrastructure/security/SecurityConfig.java" `
  "docs/progresso.md" `
  "docs/decisoes-claude-code.md" `
  "docs/prompts/prompt-etapa-5-3.md"
git status
```

Commit:
```
feat(meta): implementa infrastructure, interfaces e tests; registra sub-etapa 5.3
```

### Tarefa 19 -- Validacao pre-ship

```powershell
git log --oneline feat/etapa-5-3-meta ^main
# deve mostrar exatamente 4 commits

git diff main --name-only | Measure-Object -Line
# deve mostrar ~25-30 arquivos

git status
# deve retornar: nothing to commit, working tree clean
```

### Tarefa 20 -- Entregar via /ship

```
/ship
```

---

## Restricoes e freios

- NAO modificar bounded contexts existentes (conta, categoria, transacao, orcamento).
- NAO criar migration para bounded contexts existentes.
- NAO modificar `AbstractIntegrationTest.java`.
- NAO modificar `Money.java` ou shared domain.
- Se migration-writer gerar versao diferente de V6: pare e reporte.
- Se `./mvnw verify` falhar: corrija antes de commitar.
- Decisao silenciosa proibida: em divergencia entre prescricao e ambiente real, pare e reporte.

---

## Estrutura de commits

```
feat(meta): cria skeleton via /feature
feat(meta): implementa domain e Entity; adiciona migration e unit tests via /migrate
feat(meta): implementa application layer (5 use cases)
feat(meta): implementa infrastructure, interfaces e tests; registra sub-etapa 5.3
```

4 commits. Escopo `meta` em todos.

---

## Estado esperado ao terminar

- PR #76 aberto (com reviews automaticas via /ship).
- 4 commits na branch `feat/etapa-5-3-meta`.
- `./mvnw verify` verde.
- `V6__cria_tabela_meta.sql` criada.
- `MetaTest.java` (unit via /migrate), `MetaRepositoryImplTest.java` (integration),
  `MetaControllerTest.java` (E2E), `RegistrarDepositoEmMetaUseCaseTest.java` (unit application).
- `docs/progresso.md` e `docs/decisoes-claude-code.md` atualizados.
- `docs/prompts/prompt-etapa-5-3.md` commitado na branch.
