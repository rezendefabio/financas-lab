# Prompt -- Sub-etapa 5.1: Bounded context `orcamento` (Camada 4 inaugurada)

## Contexto

Primeira sub-etapa da Camada 4. Exercita o fluxo Tier 2 completo pela primeira vez:
`/feature → domain → /migrate → application + infra + interfaces → /ship`.

Bounded context `orcamento` entrega: CRUD (criar, listar, buscar, desativar) + endpoint
de progresso por categoria/mes. Cross-bounded-context: `CalcularProgressoDoOrcamentoUseCase`
injeta `TransacaoRepository` (padrao de `CalcularSaldoDaContaUseCase`).

Lembrete de workflow: passar este prompt como TEXTO ao executor. Commitar o `.md`
manualmente apos o PR estar aberto.

---

## Escopo decidido

### Bounded context: `orcamento`

**Package base:** `com.laboratorio.financas.orcamento`

---

### Arquivo 1: `orcamento/domain/Orcamento.java` (IMPLEMENTAR sobre stub do /feature)

Campos:
- `id`: UUID — gerado no construtor com `UUID.randomUUID()`
- `categoriaId`: UUID
- `valorLimite`: Money
- `mesAno`: LocalDate — sempre dia 1 do mes
- `ativo`: boolean — `true` no construtor
- `criadoEm`: Instant — `Instant.now()` no construtor
- `atualizadoEm`: Instant — `Instant.now()` no construtor

Validacoes no construtor (lancam `IllegalArgumentException`):
- `categoriaId != null`
- `valorLimite != null`
- `valorLimite.valor().compareTo(BigDecimal.ZERO) > 0`
- `mesAno != null`; se `mesAno.getDayOfMonth() != 1`, truncar: `mesAno = mesAno.withDayOfMonth(1)`

Metodos:
- `desativar()`: `this.ativo = false; this.atualizadoEm = Instant.now();`
- Getters para todos os campos (sem setters publicos)

---

### Arquivo 2: `orcamento/domain/OrcamentoRepository.java` (IMPLEMENTAR sobre stub do /feature)

Interface:

```java
Orcamento salvar(Orcamento orcamento);
Optional<Orcamento> buscarPorId(UUID id);
List<Orcamento> listar();
Orcamento atualizar(Orcamento orcamento);
```

---

### Arquivo 3: `orcamento/domain/OrcamentoNaoEncontradoException.java` (IMPLEMENTAR sobre stub do /feature)

`RuntimeException` com mensagem `"Orcamento nao encontrado: " + id`.

---

### Arquivo 4: `orcamento/domain/StatusProgresso.java` (NOVO — NAO gerado pelo /feature)

Enum com 4 valores:
- `ABAIXO` — percentual < 80%
- `ATENCAO` — percentual >= 80% e < 100%
- `ATINGIDO` — percentual == 100% (compareTo == 0)
- `EXCEDIDO` — percentual > 100%

---

### Arquivo 5: `orcamento/application/CriarOrcamentoUseCase.java` (IMPLEMENTAR sobre stub do /feature)

- Inner record: `Comando(UUID categoriaId, BigDecimal valorLimiteValor, String valorLimiteMoeda, LocalDate mesAno)`
- Cria `Money` com `new Money(valorLimiteValor, Currency.getInstance(valorLimiteMoeda))`
- Cria `new Orcamento(categoriaId, valorLimite, mesAno)` e salva via `orcamentoRepository.salvar(orcamento)`
- Retorna `Orcamento`

---

### Arquivo 6: `orcamento/application/ListarOrcamentosUseCase.java` (NOVO)

- Sem parametros. Retorna `List<Orcamento>` via `orcamentoRepository.listar()`

---

### Arquivo 7: `orcamento/application/BuscarOrcamentoPorIdUseCase.java` (NOVO)

- Aceita `UUID id`. Retorna `Orcamento` ou lanca `OrcamentoNaoEncontradoException(id)`

---

### Arquivo 8: `orcamento/application/DesativarOrcamentoUseCase.java` (NOVO)

- Aceita `UUID id`
- Busca orcamento (lanca `OrcamentoNaoEncontradoException` se nao encontrado)
- Chama `orcamento.desativar()`
- Chama `orcamentoRepository.atualizar(orcamento)`
- Retorna void

---

### Arquivo 9: `orcamento/application/CalcularProgressoDoOrcamentoUseCase.java` (NOVO)

**Antes de implementar:** leia `TransacaoRepository.java` para verificar o tipo de
retorno de `listarComFiltros` (pode ser `Page<Transacao>` ou `List<Transacao>`).

Campos injetados: `OrcamentoRepository` + `TransacaoRepository`

Inner record `Resultado`:
```java
record Resultado(
    UUID orcamentoId,
    UUID categoriaId,
    LocalDate mesAno,
    Money valorLimite,
    Money totalGasto,
    BigDecimal percentualUtilizado,
    StatusProgresso status
) {}
```

Logica do metodo `executar(UUID orcamentoId)`:

1. Busca orcamento: `orcamentoRepository.buscarPorId(orcamentoId)` — lanca
   `OrcamentoNaoEncontradoException` se nao encontrado

2. Constroi filtro (FiltrosTransacao e um record — construtor posicional):
   ```java
   FiltrosTransacao filtros = new FiltrosTransacao(
       null,                                           // contaId (nao usado aqui)
       orcamento.getMesAno(),                          // dataInicio
       orcamento.getMesAno().plusMonths(1).minusDays(1), // dataFim
       TipoTransacao.DESPESA,                          // tipo
       orcamento.getCategoriaId()                      // categoriaId
   );
   ```

3. Busca transacoes: `transacaoRepository.listarComFiltros(filtros, Pageable.unpaged())`
   - Se retorno for `Page<Transacao>`: chame `.getContent()` para obter lista
   - Se retorno for `List<Transacao>`: use diretamente

4. Soma despesas (BigDecimal):
   ```java
   BigDecimal totalGasto = transacoes.stream()
       .map(t -> t.getValor().valor())
       .reduce(BigDecimal.ZERO, BigDecimal::add);
   ```

5. Se `orcamento.getValorLimite().valor().compareTo(BigDecimal.ZERO) == 0`:
   retornar `Resultado` com percentual = BigDecimal.ZERO e status = EXCEDIDO

6. Calcula percentual:
   ```java
   BigDecimal percentual = totalGasto
       .multiply(BigDecimal.valueOf(100))
       .divide(orcamento.getValorLimite().valor(), 2, RoundingMode.HALF_UP);
   ```

7. Determina `StatusProgresso`:
   - `percentual.compareTo(BigDecimal.valueOf(100)) > 0` → EXCEDIDO
   - `percentual.compareTo(BigDecimal.valueOf(100)) == 0` → ATINGIDO
   - `percentual.compareTo(BigDecimal.valueOf(80)) >= 0` → ATENCAO
   - caso contrario → ABAIXO

8. Retorna:
   ```java
   new Resultado(
       orcamento.getId(),
       orcamento.getCategoriaId(),
       orcamento.getMesAno(),
       orcamento.getValorLimite(),
       new Money(totalGasto, orcamento.getValorLimite().moeda()),
       percentual,
       status
   )
   ```

---

### Arquivo 10: `orcamento/infrastructure/persistence/OrcamentoEntity.java` (IMPLEMENTAR sobre stub do /feature)

**Antes de implementar:** leia `ContaEntity.java` para ver o padrao exato de
`@Embedded + @AttributeOverride` (dois `@AttributeOverride` separados, nao wrapper).

```java
@Entity
@Table(name = "orcamento")
public class OrcamentoEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "categoria_id", columnDefinition = "uuid", nullable = false)
    private UUID categoriaId;

    @NotNull
    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "valor_limite_valor", nullable = false, precision = 19, scale = 2))
    @AttributeOverride(name = "moeda", column = @Column(name = "valor_limite_moeda", nullable = false, length = 3))
    private MoneyEmbeddable valorLimite;

    @NotNull
    @Column(name = "mes_ano", nullable = false)
    private LocalDate mesAno;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @NotNull
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    // construtor protected JPA (sem argumentos)
    // construtor com todos os campos
    // getters para todos os campos
}
```

---

### Arquivo 11: `orcamento/infrastructure/persistence/OrcamentoJpaRepository.java` (IMPLEMENTAR sobre stub)

`extends JpaRepository<OrcamentoEntity, UUID>` — sem `@Query` customizado para 5.1.

---

### Arquivo 12: `orcamento/infrastructure/persistence/OrcamentoMapper.java` (IMPLEMENTAR sobre stub)

**Antes de implementar:** leia `ContaMapper.java` ou `TransacaoMapper.java` para ver o padrao.

MapStruct: converte `OrcamentoEntity <-> Orcamento`. Seguir o padrao de conversao de
`MoneyEmbeddable` para `Money` e vice-versa (verificar como os outros mappers fazem).

---

### Arquivo 13: `orcamento/infrastructure/persistence/OrcamentoRepositoryImpl.java` (IMPLEMENTAR sobre stub)

Implementa `OrcamentoRepository`. Injeta `OrcamentoJpaRepository` + `OrcamentoMapper`.

- `salvar(Orcamento o)`: `mapper.toEntity(o)` → `jpaRepository.save(entity)` → `mapper.toOrcamento(entity)`
- `atualizar(Orcamento o)`: identico ao salvar (JPA `save` faz upsert por id)
- `buscarPorId(UUID id)`: `jpaRepository.findById(id).map(mapper::toOrcamento)`
- `listar()`: `jpaRepository.findAll().stream().map(mapper::toOrcamento).toList()`

---

### Arquivo 14: Migration SQL (gerado pelo `/migrate`)

Versao esperada: **V5** (atual maximo e V4 — verificar antes com glob).

Conteudo esperado (o migration-writer lera a Entity e gerara isto):
```sql
CREATE TABLE orcamento (
    id UUID PRIMARY KEY,
    categoria_id UUID NOT NULL,
    valor_limite_valor NUMERIC(19,2) NOT NULL,
    valor_limite_moeda VARCHAR(3) NOT NULL,
    mes_ano DATE NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_orcamento_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id)
);
```

---

### Arquivo 15: `orcamento/interfaces/dto/CriarOrcamentoRequest.java` (IMPLEMENTAR sobre stub)

```java
record CriarOrcamentoRequest(
    @NotNull UUID categoriaId,
    @NotNull BigDecimal valorLimiteValor,
    @NotNull @Size(min = 3, max = 3) String valorLimiteMoeda,
    @NotNull LocalDate mesAno
) {}
```

---

### Arquivo 16: `orcamento/interfaces/dto/OrcamentoResponse.java` (IMPLEMENTAR sobre stub)

**Antes de implementar:** leia `SaldoResponse.java` para ver o padrao de `ValorMonetario`
(record aninhado com `BigDecimal valor` e `String moeda`).

```java
record OrcamentoResponse(
    UUID id,
    UUID categoriaId,
    ValorMonetario valorLimite,
    LocalDate mesAno,
    boolean ativo,
    Instant criadoEm,
    Instant atualizadoEm
) {
    record ValorMonetario(BigDecimal valor, String moeda) {}
}
```

---

### Arquivo 17: `orcamento/interfaces/dto/ProgressoResponse.java` (NOVO — NAO gerado pelo /feature)

```java
record ProgressoResponse(
    UUID orcamentoId,
    UUID categoriaId,
    LocalDate mesAno,
    OrcamentoResponse.ValorMonetario valorLimite,
    OrcamentoResponse.ValorMonetario totalGasto,
    BigDecimal percentualUtilizado,
    String status
) {}
```

Ou definir `ValorMonetario` diretamente aqui, se o executor preferir nao referenciar
o record de outro arquivo. O importante e que o JSON tenha os campos acima.

---

### Arquivo 18: `orcamento/interfaces/OrcamentoController.java` (IMPLEMENTAR sobre stub)

5 endpoints. **Antes de implementar:** verifique se ha `GlobalExceptionHandler` no projeto
(busque por `@RestControllerAdvice` ou `@ExceptionHandler`) para saber como mapear
`OrcamentoNaoEncontradoException` para 404.

| Metodo | Path | Retorno |
|--------|------|---------|
| POST | /api/orcamentos | 201 + OrcamentoResponse |
| GET | /api/orcamentos | 200 + List<OrcamentoResponse> |
| GET | /api/orcamentos/{id} | 200 + OrcamentoResponse |
| DELETE | /api/orcamentos/{id} | 204 (sem corpo) |
| GET | /api/orcamentos/{id}/progresso | 200 + ProgressoResponse |

DELETE chama `DesativarOrcamentoUseCase` (soft delete — seta `ativo = false`).

Conversao de `Orcamento` para `OrcamentoResponse`: inline no controller (sem mapper separado
para DTOs — verificar padrao dos outros controllers).

Conversao de `CalcularProgressoDoOrcamentoUseCase.Resultado` para `ProgressoResponse`: inline.

---

### Arquivo 19: `docs/progresso.md` (EDITAR)

Leia o arquivo antes de editar. Aplique as 3 mudancas abaixo.

**Mudanca 1 -- linha "Ultima atualizacao":**
```
**Última atualização:** 2026-05-12 (Sub-etapa 5.1 -- Bounded context orcamento)
```

**Mudanca 2 -- adicionar 5.1 em "Sub-etapas concluidas"** (crie secao "Camada 4" se nao
existir, logo antes da secao "Camada 3" ou onde couber na estrutura do arquivo):
```
- **5.1 -- Bounded context `orcamento`** (2026-05-12): primeiro feature Tier 2 completo.
  Fluxo exercitado: /feature -> domain -> /migrate -> application + infra + interfaces -> /ship.
  CRUD (criar, listar, buscar, desativar) + endpoint de progresso por categoria/mes.
  Cross-bounded-context: CalcularProgressoDoOrcamentoUseCase injeta TransacaoRepository.
  StatusProgresso enum (ABAIXO/ATENCAO/ATINGIDO/EXCEDIDO) inaugurado no domain. 5 endpoints,
  4 commits, ~22 arquivos. Camada 4 inaugurada. PR #73.
```

**Mudanca 3 -- adicionar em "Historico de mudancas":**
```
- **2026-05-12** -- Sub-etapa 5.1 concluida: bounded context `orcamento`. Camada 4
  inaugurada. Primeiro feature Tier 2 completo. PR #73.
```

---

### Arquivo 20: `docs/decisoes-claude-code.md` (EDITAR)

Leia o arquivo antes de editar. Adicione nova secao antes de `## Historico de mudancas`
(linha em branco antes e depois de cada `##`):

```
## Sub-etapa 5.1 -- Bounded context `orcamento` (Camada 4 inaugurada)

### Camada 4: primeiro feature Tier 2

Fluxo Tier 2 exercitado pela primeira vez: /feature -> domain -> /migrate -> application
+ infra + interfaces -> /ship. Bounded context `orcamento` entrega CRUD + progresso
por categoria/mes.

### Decisao: cross-bounded-context no use case de progresso

`CalcularProgressoDoOrcamentoUseCase` injeta `TransacaoRepository` diretamente,
seguindo o padrao estabelecido por `CalcularSaldoDaContaUseCase` (Camada 2). Sem
wrapper ou porta intermediaria entre bounded contexts.

### Decisao: StatusProgresso enum no domain

`StatusProgresso` (ABAIXO, ATENCAO, ATINGIDO, EXCEDIDO) criado manualmente em
`orcamento/domain/` -- nao gerado pelo /feature (geradores criam apenas o minimo
estrutural, nao enums de dominio especificos).
```

Adicionar ao `## Historico de mudancas`:
```
- **2026-05-12** -- Sub-etapa 5.1 concluida: bounded context `orcamento`. Camada 4
  inaugurada. Primeiro feature Tier 2 completo. PR #73.
```

---

## Estado esperado ao iniciar

```powershell
git branch --show-current   # deve retornar: main
git status                  # deve retornar: nothing to commit, working tree clean
git log --oneline -3        # deve mostrar PR #72 (4.26 split) no topo
```

Se qualquer condicao falhar: pare e reporte.

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
git checkout -b feat/etapa-5-1-orcamento
git branch --show-current   # deve retornar: feat/etapa-5-1-orcamento
```

### Tarefa 3 -- Gerar skeleton via /feature

```
/feature orcamento
```

Pos-condicao (verificar os 11 stubs):
```powershell
Test-Path "src/main/java/com/laboratorio/financas/orcamento/domain/Orcamento.java"
Test-Path "src/main/java/com/laboratorio/financas/orcamento/domain/OrcamentoRepository.java"
Test-Path "src/main/java/com/laboratorio/financas/orcamento/domain/OrcamentoNaoEncontradoException.java"
Test-Path "src/main/java/com/laboratorio/financas/orcamento/application/CriarOrcamentoUseCase.java"
Test-Path "src/main/java/com/laboratorio/financas/orcamento/infrastructure/persistence/OrcamentoEntity.java"
Test-Path "src/main/java/com/laboratorio/financas/orcamento/infrastructure/persistence/OrcamentoJpaRepository.java"
Test-Path "src/main/java/com/laboratorio/financas/orcamento/infrastructure/persistence/OrcamentoRepositoryImpl.java"
Test-Path "src/main/java/com/laboratorio/financas/orcamento/infrastructure/persistence/OrcamentoMapper.java"
Test-Path "src/main/java/com/laboratorio/financas/orcamento/interfaces/OrcamentoController.java"
Test-Path "src/main/java/com/laboratorio/financas/orcamento/interfaces/dto/CriarOrcamentoRequest.java"
Test-Path "src/main/java/com/laboratorio/financas/orcamento/interfaces/dto/OrcamentoResponse.java"
# todos devem retornar True
```

### Tarefa 4 -- Commit 1: skeleton

```powershell
git add "src/main/java/com/laboratorio/financas/orcamento/"
git status
```

Commit:
```
feat(orcamento): cria skeleton via /feature
```

### Tarefa 5 -- Implementar domain

Substitua os stubs gerados pelo /feature com implementacoes reais:

1. `Orcamento.java` -- conforme prescricao (campos, validacoes, desativar(), getters)
2. `OrcamentoRepository.java` -- interface com 4 metodos
3. `OrcamentoNaoEncontradoException.java` -- RuntimeException com mensagem

Crie manualmente (nao gerado pelo /feature):

4. `StatusProgresso.java` -- enum com ABAIXO, ATENCAO, ATINGIDO, EXCEDIDO

Pos-condicao:
```powershell
Select-String "withDayOfMonth" "src/main/java/com/laboratorio/financas/orcamento/domain/Orcamento.java"
# deve ter match (truncagem do dia do mes)

Test-Path "src/main/java/com/laboratorio/financas/orcamento/domain/StatusProgresso.java"
# deve retornar: True
```

### Tarefa 6 -- Implementar OrcamentoEntity.java

Leia `src/main/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaEntity.java`
antes de implementar. Siga o padrao exato de dois `@AttributeOverride` separados para o
campo `MoneyEmbeddable`. Campo `valorLimite` mapeia para colunas `valor_limite_valor` e
`valor_limite_moeda`.

Pos-condicao:
```powershell
Select-String "MoneyEmbeddable" "src/main/java/com/laboratorio/financas/orcamento/infrastructure/persistence/OrcamentoEntity.java"
# deve ter match
Select-String "valor_limite_valor" "src/main/java/com/laboratorio/financas/orcamento/infrastructure/persistence/OrcamentoEntity.java"
# deve ter match
```

### Tarefa 7 -- Gerar migration e unit tests via /migrate

Antes de invocar: verifique a versao mais alta de migration existente:
```powershell
Get-ChildItem "src/main/resources/db/migration/" | Sort-Object Name
# deve listar V1-V4 -- proximo sera V5
```

```
/migrate orcamento
```

Pos-condicao:
```powershell
Test-Path "src/main/resources/db/migration/V5__cria_tabela_orcamento.sql"
# deve retornar: True

Test-Path "src/test/java/com/laboratorio/financas/orcamento/domain/OrcamentoTest.java"
# deve retornar: True
```

Se migration-writer gerar versao diferente de V5: pare e reporte.

### Tarefa 8 -- Commit 2: domain + entity + migration + unit tests

```powershell
git add "src/main/java/com/laboratorio/financas/orcamento/domain/" `
        "src/main/java/com/laboratorio/financas/orcamento/infrastructure/persistence/OrcamentoEntity.java" `
        "src/main/resources/db/migration/V5__cria_tabela_orcamento.sql" `
        "src/test/java/com/laboratorio/financas/orcamento/domain/OrcamentoTest.java"
git status
```

Commit:
```
feat(orcamento): implementa domain e Entity; adiciona migration e unit tests via /migrate
```

### Tarefa 9 -- Implementar application layer (5 use cases)

Leia `TransacaoRepository.java` antes de implementar `CalcularProgressoDoOrcamentoUseCase`
para verificar o tipo de retorno de `listarComFiltros`.

Leia `FiltrosTransacao.java` para verificar a ordem dos campos no construtor record.

Implemente (substituindo stub onde aplicavel):

1. `CriarOrcamentoUseCase.java` -- substitui stub
2. `ListarOrcamentosUseCase.java` -- NOVO
3. `BuscarOrcamentoPorIdUseCase.java` -- NOVO
4. `DesativarOrcamentoUseCase.java` -- NOVO
5. `CalcularProgressoDoOrcamentoUseCase.java` -- NOVO

Pos-condicao:
```powershell
(Get-ChildItem "src/main/java/com/laboratorio/financas/orcamento/application/").Count
# deve retornar 5
```

### Tarefa 10 -- Commit 3: application layer

```powershell
git add "src/main/java/com/laboratorio/financas/orcamento/application/"
git status
```

Commit:
```
feat(orcamento): implementa application layer (5 use cases)
```

### Tarefa 11 -- Implementar infrastructure (mapper + repository impl)

Leia `ContaMapper.java` ou `TransacaoMapper.java` antes de implementar `OrcamentoMapper`.

Implemente:

1. `OrcamentoMapper.java` -- MapStruct, converte `OrcamentoEntity <-> Orcamento`
2. `OrcamentoRepositoryImpl.java` -- implementa `OrcamentoRepository`

### Tarefa 12 -- Implementar interfaces (DTOs + controller)

Leia `SaldoResponse.java` antes de implementar DTOs para ver o padrao de `ValorMonetario`.

Verifique se existe `GlobalExceptionHandler` (`@RestControllerAdvice`) para saber o padrao
de mapeamento de excecao para 404.

Implemente:

1. `CriarOrcamentoRequest.java` -- substitui stub
2. `OrcamentoResponse.java` -- substitui stub; inclui inner record `ValorMonetario`
3. `ProgressoResponse.java` -- NOVO, criar em `interfaces/dto/`
4. `OrcamentoController.java` -- 5 endpoints conforme tabela

### Tarefa 13 -- Gerar integration tests do repository

```
/write-test src/main/java/com/laboratorio/financas/orcamento/infrastructure/persistence/OrcamentoRepositoryImpl.java
```

Aguarde o relatorio do test-writer. Pos-condicao:
```powershell
Test-Path "src/test/java/com/laboratorio/financas/orcamento/infrastructure/persistence/OrcamentoRepositoryImplTest.java"
# deve retornar: True
```

### Tarefa 14 -- Gerar E2E tests do controller

```
/write-test src/main/java/com/laboratorio/financas/orcamento/interfaces/OrcamentoController.java
```

Aguarde o relatorio do test-writer. Pos-condicao:
```powershell
Test-Path "src/test/java/com/laboratorio/financas/orcamento/interfaces/OrcamentoControllerTest.java"
# deve retornar: True
```

### Tarefa 15 -- Validacao completa

```powershell
./mvnw verify
# deve compilar + passar todos os testes (284 existentes + novos do orcamento)
# BUILD SUCCESS obrigatorio antes de prosseguir
```

Se testes falharem: corrija antes de continuar. NAO commite com testes falhando.

### Tarefa 16 -- Atualizar docs

Aplique as mudancas prescritas nos Arquivos 19 e 20 (`progresso.md`, `decisoes-claude-code.md`).
Leia cada arquivo antes de editar.

Pos-condicao:
```powershell
Select-String "5.1" "docs/progresso.md" | Measure-Object | Select-Object -Expand Count
# deve retornar > 0

Select-String "orcamento" "docs/decisoes-claude-code.md" | Measure-Object | Select-Object -Expand Count
# deve retornar > 0
```

### Tarefa 17 -- Commit 4: infrastructure + interfaces + tests + docs

```powershell
git add `
  "src/main/java/com/laboratorio/financas/orcamento/infrastructure/" `
  "src/main/java/com/laboratorio/financas/orcamento/interfaces/" `
  "src/test/java/com/laboratorio/financas/orcamento/infrastructure/" `
  "src/test/java/com/laboratorio/financas/orcamento/interfaces/" `
  "docs/progresso.md" `
  "docs/decisoes-claude-code.md" `
  "CLAUDE.md"
git status
```

Commit:
```
feat(orcamento): implementa infrastructure, interfaces e tests; registra Camada 4 inaugurada
```

### Tarefa 18 -- Validacao pre-ship

```powershell
git log --oneline feat/etapa-5-1-orcamento ^main
# deve mostrar exatamente 4 commits

git diff main --name-only | Measure-Object -Line
# deve mostrar ~22-25 arquivos

git status
# deve retornar: nothing to commit, working tree clean

Select-String "V5" "docs/progresso.md"
# NAO precisa ter match aqui -- mas os criterios abaixo devem passar

Select-String "5.1" "docs/progresso.md"
# deve ter match

Select-String "orcamento" "docs/decisoes-claude-code.md"
# deve ter match

Test-Path "src/main/resources/db/migration/V5__cria_tabela_orcamento.sql"
# deve retornar: True
```

### Tarefa 19 -- Entregar via /ship

```
/ship
```

---

## Restricoes e freios

- NAO modificar bounded contexts existentes (conta, categoria, transacao) alem de
  injetar `TransacaoRepository` no use case de progresso (sem modificar a interface).
- NAO criar migration para conta/categoria/transacao.
- NAO modificar `AbstractIntegrationTest.java` -- E2E tests extendem via subclasse
  com `@AutoConfigureMockMvc` (padrao 4.25).
- NAO modificar `Money.java`, `MoneyEmbeddable.java` ou qualquer shared domain.
- NAO alterar `FiltrosTransacao.java` -- apenas usar.
- NAO modificar `TransacaoRepository.java` ou qualquer interface de outro bounded context.
- `CLAUDE.md` ja foi editado pelo orquestrador (secao "Modelo executor" adicionada) -- incluir no commit 4, NAO reeditar o conteudo.
- Se hook bloquear commit: leia a mensagem, corrija sem `--no-verify`.
- Se `./mvnw verify` falhar: corrija antes de commitar.
- Se migration-writer gerar versao diferente de V5: pare e reporte.
- Decisao silenciosa proibida: em divergencia entre prescricao e ambiente real, pare e reporte.

---

## Estrutura de commits

```
feat(orcamento): cria skeleton via /feature
feat(orcamento): implementa domain e Entity; adiciona migration e unit tests via /migrate
feat(orcamento): implementa application layer (5 use cases)
feat(orcamento): implementa infrastructure, interfaces e tests; registra Camada 4 inaugurada
```

4 commits. Escopo `orcamento` em todos (nome do bounded context).

---

## Estado esperado ao terminar

- PR #73 aberto.
- 4 commits na branch `feat/etapa-5-1-orcamento`.
- ~22-25 arquivos novos/modificados vs main.
- `./mvnw verify` verde (284+ testes passando).
- `V5__cria_tabela_orcamento.sql` criada.
- `OrcamentoTest.java` criado (unit tests via /migrate).
- `OrcamentoRepositoryImplTest.java` criado (integration tests via /write-test).
- `OrcamentoControllerTest.java` criado (E2E tests via /write-test).
- `docs/progresso.md` com 5.1 registrada e Camada 4 inaugurada.
- `docs/decisoes-claude-code.md` com secao Sub-etapa 5.1.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO commitar o arquivo de prompt na branch (operador faz manualmente).
- NAO rodar `/ship` mais de uma vez.
