# Prompt -- Sub-etapa 5.4: Bounded context `lancamento-recorrente`

## Contexto

Terceiro bounded context Tier 2 da Camada 4. Exercita um padrao novo: **escrita cross-BC**
-- `ExecutarLancamentoRecorrenteUseCase` cria uma `Transacao` (BC externo) a partir de um
`LancamentoRecorrente` (BC proprio). Ate agora o cruzamento de BCs era apenas leitura
(CalcularProgressoDoOrcamentoUseCase le TransacaoRepository). Aqui e escrita.

Nota de escopo: `visao.md` lista recorrencias como fora do MVP de produto. A entrada
na 5.4 e justificada pelo proposito primario da fabrica (exercitar padrao novo), nao
pelo produto.

Numeracao: 5.4 (Camada 4, quarto bounded context / quarta sub-etapa de feature).

---

## Design do bounded context `lancamento-recorrente`

### Domain: `LancamentoRecorrente.java`

**Campos:**
- `id`: UUID (gerado no construtor)
- `descricao`: String (max 200 chars)
- `tipo`: TipoTransacao (importado de `transacao.domain` -- padrao estabelecido pelo
  CalcularProgressoDoOrcamentoUseCase que importa FiltrosTransacao de transacao.domain;
  aceita apenas RECEITA ou DESPESA -- TRANSFERENCIA rejeitada no construtor)
- `valor`: Money
- `contaId`: UUID
- `categoriaId`: UUID (nullable)
- `periodicidade`: Periodicidade (enum criado em `lancamento-recorrente/domain/`)
- `proximaOcorrencia`: LocalDate
- `ativo`: boolean
- `criadoEm`: Instant (gerado no construtor)
- `atualizadoEm`: Instant

**Validacoes no construtor:**
- `descricao` not null/blank, max 200 chars
- `tipo` not null; se `tipo == TipoTransacao.TRANSFERENCIA`, lancar
  `IllegalArgumentException("TRANSFERENCIA nao e suportada em lancamentos recorrentes")`
- `valor` not null, `valor.valor()` > 0
- `contaId` not null
- `periodicidade` not null
- `proximaOcorrencia` not null

**Metodos:**
- `avancarProximaOcorrencia()`: `this.proximaOcorrencia = periodicidade.calcularProxima(this.proximaOcorrencia)`;
  `this.atualizadoEm = Instant.now()`. Mutavel (mesmo padrao de Meta.registrarDeposito).
- `desativar()`: `this.ativo = false`; `this.atualizadoEm = Instant.now()`.
- Getters para todos os campos.

### Enum: `Periodicidade.java` (em `lancamento-recorrente/domain/`)

```java
public enum Periodicidade {
    SEMANAL {
        @Override
        public LocalDate calcularProxima(LocalDate atual) { return atual.plusWeeks(1); }
    },
    QUINZENAL {
        @Override
        public LocalDate calcularProxima(LocalDate atual) { return atual.plusWeeks(2); }
    },
    MENSAL {
        @Override
        public LocalDate calcularProxima(LocalDate atual) { return atual.plusMonths(1); }
    },
    BIMESTRAL {
        @Override
        public LocalDate calcularProxima(LocalDate atual) { return atual.plusMonths(2); }
    },
    TRIMESTRAL {
        @Override
        public LocalDate calcularProxima(LocalDate atual) { return atual.plusMonths(3); }
    },
    SEMESTRAL {
        @Override
        public LocalDate calcularProxima(LocalDate atual) { return atual.plusMonths(6); }
    },
    ANUAL {
        @Override
        public LocalDate calcularProxima(LocalDate atual) { return atual.plusYears(1); }
    };

    public abstract LocalDate calcularProxima(LocalDate atual);
}
```

### Repository interface: `LancamentoRecorrenteRepository.java`

```java
LancamentoRecorrente salvar(LancamentoRecorrente lancamento);
Optional<LancamentoRecorrente> buscarPorId(UUID id);
List<LancamentoRecorrente> listar();
LancamentoRecorrente atualizar(LancamentoRecorrente lancamento);
```

### Exception: `LancamentoRecorrenteNaoEncontradoException.java`

`RuntimeException` com mensagem `"Lancamento recorrente nao encontrado: " + id`.

---

## Application layer (5 use cases)

**1. `CriarLancamentoRecorrenteUseCase`**
- `Comando`: descricao (String), tipo (TipoTransacao), valorValor (BigDecimal),
  valorMoeda (String), contaId (UUID), categoriaId (UUID nullable),
  periodicidade (Periodicidade), proximaOcorrencia (LocalDate)
- Cria `LancamentoRecorrente` com `new Money(valorValor, Currency.getInstance(valorMoeda))`
- Salva via `LancamentoRecorrenteRepository.salvar()`
- Retorna o lancamento salvo

**2. `ListarLancamentosRecorrentesUseCase`**
- Sem parametros
- Retorna `List<LancamentoRecorrente>`

**3. `BuscarLancamentoRecorrentePorIdUseCase`**
- Aceita UUID
- Retorna `LancamentoRecorrente` ou lanca `LancamentoRecorrenteNaoEncontradoException`

**4. `DesativarLancamentoRecorrenteUseCase`**
- Aceita UUID
- Busca → chama `lancamento.desativar()` → `LancamentoRecorrenteRepository.atualizar()`

**5. `ExecutarLancamentoRecorrenteUseCase`** (use case principal -- padrao novo)

Injeta: `LancamentoRecorrenteRepository` + `TransacaoRepository`
(mesmo padrao de CalcularProgressoDoOrcamentoUseCase que injeta TransacaoRepository).

Logica:
1. Busca lancamento por id (lanca `LancamentoRecorrenteNaoEncontradoException` se ausente)
2. Valida `lancamento.isAtivo()` -- se false, lanca
   `IllegalStateException("Lancamento recorrente inativo nao pode ser executado")`
3. Cria `Transacao` usando o construtor de nova transacao:
   ```java
   new Transacao(
       lancamento.getTipo(),
       lancamento.getValor(),
       lancamento.getProximaOcorrencia(),  // data da transacao = proxima ocorrencia atual
       lancamento.getDescricao(),
       lancamento.getContaId(),
       null,                               // contaDestinoId -- sempre null (nao TRANSFERENCIA)
       lancamento.getCategoriaId()
   )
   ```
   Leia `Transacao.java` antes de implementar para confirmar assinatura do construtor.
4. Salva a transacao via `transacaoRepository.salvar(transacao)`
5. Chama `lancamento.avancarProximaOcorrencia()` para avançar a data
6. Atualiza via `lancamentoRecorrenteRepository.atualizar(lancamento)`
7. Retorna `record Resultado(UUID transacaoId, UUID lancamentoRecorrenteId,
   LocalDate dataExecutada, LocalDate novaProximaOcorrencia)`
   onde `dataExecutada` e a proxima ocorrencia *antes* de avancar,
   e `novaProximaOcorrencia` e a proxima ocorrencia *apos* avancar.

---

## Infrastructure

### `LancamentoRecorrenteEntity.java`

Leia `ContaEntity.java` para ver padrao de `@Embedded + @AttributeOverrides` para Money.
Leia `MetaEntity.java` para confirmar padrao de `@Enumerated(EnumType.STRING)`.

Colunas mapeadas:
- `id UUID` -- `@Id`
- `descricao VARCHAR(200)` -- `@Column(nullable = false, length = 200)`
- `tipo VARCHAR(20)` -- `@Enumerated(EnumType.STRING)`, `@Column(nullable = false, length = 20)`
- `valor` -- `@Embedded` com `@AttributeOverrides`:
  - `valor` → `valor_valor`
  - `moeda` → `valor_moeda`
- `conta_id UUID` -- `@Column(name = "conta_id", nullable = false)`
- `categoria_id UUID` -- `@Column(name = "categoria_id")` (nullable)
- `periodicidade VARCHAR(20)` -- `@Enumerated(EnumType.STRING)`, `@Column(nullable = false, length = 20)`
- `proxima_ocorrencia DATE` -- `@Column(name = "proxima_ocorrencia", nullable = false)`
- `ativo BOOLEAN` -- `@Column(nullable = false)`
- `criado_em TIMESTAMPTZ` -- `@Column(name = "criado_em", nullable = false)`
- `atualizado_em TIMESTAMPTZ` -- `@Column(name = "atualizado_em", nullable = false)`

### `LancamentoRecorrenteJpaRepository.java`

Extends `JpaRepository<LancamentoRecorrenteEntity, UUID>`. Sem `@Query` customizado.

### `LancamentoRecorrenteMapper.java`

MapStruct. Seguir padrao de `OrcamentoMapper` ou `MetaMapper`.

### `LancamentoRecorrenteRepositoryImpl.java`

Injeta `LancamentoRecorrenteJpaRepository` + `LancamentoRecorrenteMapper`.
Implementa `LancamentoRecorrenteRepository`.

### Migration SQL (V7)

```sql
CREATE TABLE lancamento_recorrente (
    id UUID PRIMARY KEY,
    descricao VARCHAR(200) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    valor_valor NUMERIC(19,2) NOT NULL,
    valor_moeda VARCHAR(3) NOT NULL,
    conta_id UUID NOT NULL,
    categoria_id UUID,
    periodicidade VARCHAR(20) NOT NULL,
    proxima_ocorrencia DATE NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_lancamento_recorrente_conta FOREIGN KEY (conta_id) REFERENCES conta(id),
    CONSTRAINT fk_lancamento_recorrente_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id)
);
```

---

## Interface layer

### Endpoints em `LancamentoRecorrenteController.java`

- `POST /api/lancamentos-recorrentes` → 201 + `LancamentoRecorrenteResponse`
- `GET /api/lancamentos-recorrentes` → 200 + `List<LancamentoRecorrenteResponse>`
- `GET /api/lancamentos-recorrentes/{id}` → 200 + `LancamentoRecorrenteResponse`
- `DELETE /api/lancamentos-recorrentes/{id}` → 204 (desativa, nao deleta)
- `POST /api/lancamentos-recorrentes/{id}/execucoes` → 201 + `ExecucaoResponse`
  (sub-recurso -- mesmo padrao de `/depositos` em meta)

### DTOs

**`CriarLancamentoRecorrenteRequest`:**
```java
public record CriarLancamentoRecorrenteRequest(
    @NotBlank @Size(max = 200) String descricao,
    @NotNull TipoTransacao tipo,
    @NotNull BigDecimal valorValor,
    @NotNull @Size(min = 3, max = 3) String valorMoeda,
    @NotNull UUID contaId,
    UUID categoriaId,
    @NotNull Periodicidade periodicidade,
    @NotNull LocalDate proximaOcorrencia
) { }
```

**`LancamentoRecorrenteResponse`:** id, descricao, tipo, `ValorMonetario valor`,
contaId, categoriaId, periodicidade, proximaOcorrencia, ativo, criadoEm, atualizadoEm.

Onde `ValorMonetario record(BigDecimal valor, String moeda)` -- mesmo padrao de
`OrcamentoResponse` e `MetaResponse`. Implementar `fromDomain()` estatico.

**`ExecucaoResponse`:**
```java
public record ExecucaoResponse(
    UUID transacaoId,
    UUID lancamentoRecorrenteId,
    LocalDate dataExecutada,
    LocalDate novaProximaOcorrencia
) { }
```

### Controller -- endpoint de execucao

```java
@PostMapping("/{id}/execucoes")
@ResponseStatus(HttpStatus.CREATED)
public ExecucaoResponse executar(@PathVariable UUID id) {
    ExecutarLancamentoRecorrenteUseCase.Resultado resultado =
        executarUseCase.executar(id);
    return new ExecucaoResponse(
        resultado.transacaoId(),
        resultado.lancamentoRecorrenteId(),
        resultado.dataExecutada(),
        resultado.novaProximaOcorrencia()
    );
}
```

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-4-lancamento-recorrente
2. /feature lancamento-recorrente   → leia a skill, crie 11 arquivos stub manualmente
3. commit: feat(lancamento-recorrente): cria skeleton via /feature
4. Implementa LancamentoRecorrente.java + Periodicidade.java +
   LancamentoRecorrenteRepository.java + LancamentoRecorrenteNaoEncontradoException.java
5. Implementa LancamentoRecorrenteEntity.java (leia ContaEntity e MetaEntity como referencia)
6. /migrate lancamento-recorrente   → leia a skill, execute manualmente:
   - migration-writer agent gera V7__cria_tabela_lancamento_recorrente.sql
   - unit test LancamentoRecorrenteTest.java
7. commit: feat(lancamento-recorrente): implementa domain e Entity; adiciona migration e unit tests via /migrate
8. Implementa 5 use cases (leia Transacao.java antes de ExecutarLancamentoRecorrenteUseCase)
9. commit: feat(lancamento-recorrente): implementa application layer (5 use cases)
10. Implementa LancamentoRecorrenteRepositoryImpl + LancamentoRecorrenteMapper
11. Implementa LancamentoRecorrenteController + 3 DTOs
12. Por convencao implicita (CLAUDE.md): invocar /write-test para cada *UseCase.java,
    *RepositoryImpl.java e *Controller.java
13. ./mvnw verify -- BUILD SUCCESS obrigatorio
14. Atualiza docs/progresso.md (registra 5.4 e padrao novo cross-BC write)
15. commit: feat(lancamento-recorrente): implementa infrastructure, interfaces e tests; registra sub-etapa 5.4
    (inclui docs/prompts/prompt-etapa-5-4.md)
16. /ship → PR aberto com reviews automaticos
```

---

## Estrutura de commits (5.4)

```
feat(lancamento-recorrente): cria skeleton via /feature
feat(lancamento-recorrente): implementa domain e Entity; adiciona migration e unit tests via /migrate
feat(lancamento-recorrente): implementa application layer (5 use cases)
feat(lancamento-recorrente): implementa infrastructure, interfaces e tests; registra sub-etapa 5.4
```

---

## Arquivos a criar

**Domain:**
- `src/main/java/.../lancamento-recorrente/domain/LancamentoRecorrente.java`
- `src/main/java/.../lancamento-recorrente/domain/Periodicidade.java`
- `src/main/java/.../lancamento-recorrente/domain/LancamentoRecorrenteRepository.java`
- `src/main/java/.../lancamento-recorrente/domain/LancamentoRecorrenteNaoEncontradoException.java`

**Application:**
- `src/main/java/.../lancamento-recorrente/application/CriarLancamentoRecorrenteUseCase.java`
- `src/main/java/.../lancamento-recorrente/application/ListarLancamentosRecorrentesUseCase.java`
- `src/main/java/.../lancamento-recorrente/application/BuscarLancamentoRecorrentePorIdUseCase.java`
- `src/main/java/.../lancamento-recorrente/application/DesativarLancamentoRecorrenteUseCase.java`
- `src/main/java/.../lancamento-recorrente/application/ExecutarLancamentoRecorrenteUseCase.java`

**Infrastructure:**
- `src/main/java/.../lancamento-recorrente/infrastructure/persistence/LancamentoRecorrenteEntity.java`
- `src/main/java/.../lancamento-recorrente/infrastructure/persistence/LancamentoRecorrenteJpaRepository.java`
- `src/main/java/.../lancamento-recorrente/infrastructure/persistence/LancamentoRecorrenteMapper.java`
- `src/main/java/.../lancamento-recorrente/infrastructure/persistence/LancamentoRecorrenteRepositoryImpl.java`

**Interface:**
- `src/main/java/.../lancamento-recorrente/interfaces/LancamentoRecorrenteController.java`
- `src/main/java/.../lancamento-recorrente/interfaces/dto/CriarLancamentoRecorrenteRequest.java`
- `src/main/java/.../lancamento-recorrente/interfaces/dto/LancamentoRecorrenteResponse.java`
- `src/main/java/.../lancamento-recorrente/interfaces/dto/ExecucaoResponse.java`

**Gerado por agents:**
- `src/main/resources/db/migration/V7__cria_tabela_lancamento_recorrente.sql`
- `src/test/java/.../lancamento-recorrente/domain/LancamentoRecorrenteTest.java`
- Testes de application, repository e controller (por convencao implicita do CLAUDE.md)

**Docs (ultimo commit):**
- `docs/progresso.md` (registra 5.4 e padrao cross-BC write)
- `docs/prompts/prompt-etapa-5-4.md` (este arquivo)

---

## Arquivos de referencia (ler antes de implementar)

- `transacao/domain/Transacao.java` -- assinatura do construtor de nova transacao
- `transacao/domain/TransacaoRepository.java` -- interface a injetar em ExecutarUseCase
- `conta/infrastructure/persistence/ContaEntity.java` -- padrao @Embedded Money
- `meta/infrastructure/persistence/MetaEntity.java` -- padrao @Enumerated + dois @Embedded
- `orcamento/application/CalcularProgressoDoOrcamentoUseCase.java` -- padrao de injecao cross-BC
- `meta/interfaces/dto/MetaResponse.java` -- padrao fromDomain() + ValorMonetario

---

## Restricoes

- NAO modificar bounded contexts existentes (conta, categoria, transacao, orcamento, meta).
- NAO criar migration para tabelas existentes.
- NAO suportar TRANSFERENCIA em LancamentoRecorrente (rejeitar no construtor).
- NAO usar Skill tool para /feature, /migrate, /write-test, /ship -- todos tem
  disable-model-invocation: true; ler a skill e executar manualmente.
- Se hook bloquear commit: ler a mensagem, corrigir sem --no-verify.

---

## Padrao novo a documentar em progresso.md

**Cross-BC write (primeira ocorrencia):** `ExecutarLancamentoRecorrenteUseCase` injeta
`TransacaoRepository` e chama `transacaoRepository.salvar()` para criar uma `Transacao`
de outro bounded context. Ate 5.3, cruzamento de BCs era apenas leitura. Padrao estabelecido
e documentado na 5.4.

---

## Estado esperado ao terminar

- PR aberto com 4 commits acima de main.
- `./mvnw verify` BUILD SUCCESS com todos os testes existentes + novos.
- V7 migration criada.
- Testes em todos os 4 niveis (domain/application/infrastructure/interfaces).
- `docs/progresso.md` com 5.4 registrada e padrao cross-BC write documentado.
- `docs/prompts/prompt-etapa-5-4.md` commitado no ultimo commit.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO rodar /ship mais de uma vez.
- NAO commitar o arquivo de prompt separadamente -- va junto com os docs no ultimo commit.
