# Prompt -- Sub-etapa 5.8: Gaps pré-frontend

## Contexto

Tres gaps identificados por auditoria antes do frontend:
1. **Hierarquia pai/filho em Categoria** -- domain change + migration ALTER TABLE
2. **Seed de categorias iniciais** -- migration INSERT
3. **Saldo total de todas as contas** -- novo use case + endpoint

Nenhum novo bounded context. Todos os tres sao extensoes de codigo existente.

Sub-etapa 5.8 (Camada 4).

---

## Gap 1 -- Hierarquia pai/filho em Categoria

### Mudancas no domain: `Categoria.java`

Leia o arquivo atual antes de editar. Adicionar campo `categoriaPaiId` (UUID nullable).

**Construtor de criacao** -- novo parametro opcional:
```java
public Categoria(String nome, TipoCategoria tipo, UUID categoriaPaiId) {
    this(UUID.randomUUID(), nome, tipo, categoriaPaiId, Instant.now(), null);
}
```

**Construtor de reconstrucao** -- novo parametro:
```java
public Categoria(UUID id, String nome, TipoCategoria tipo, UUID categoriaPaiId,
                 Instant criadoEm, Instant atualizadoEm)
```

Getter: `public UUID getCategoriaPaiId() { return categoriaPaiId; }`

Sem validacao de hierarquia no domain (nao tem acesso ao repositorio) -- validar
na use case.

### Mudancas no repository: `CategoriaRepository.java`

Adicionar dois novos metodos:
```java
List<Categoria> listarRaiz();               // categoriaPaiId is null
List<Categoria> listarFilhosDe(UUID categoriaPaiId);
```

### Mudancas na infrastructure

**`CategoriaEntity.java`** -- adicionar campo:
```java
@Column(name = "categoria_pai_id")          // nullable, sem NOT NULL
private UUID categoriaPaiId;
```
Atualizar construtor para incluir `categoriaPaiId`. Adicionar getter.

**`CategoriaJpaRepository.java`** -- adicionar query methods:
```java
List<CategoriaEntity> findByCategoriaPaiIdIsNull();
List<CategoriaEntity> findByCategoriaPaiId(UUID categoriaPaiId);
```

**`CategoriaMapper.java`** -- usa implementacao manual (default methods).
Atualizar `toEntity()` e `toDomain()` para incluir `categoriaPaiId`.

**`CategoriaRepositoryImpl.java`** -- implementar os dois novos metodos delegando
para JpaRepository. Leia o arquivo atual antes de editar.

### Mudancas na application: `CriarCategoriaUseCase.java`

Atualizar `Comando` para incluir `categoriaPaiId`:
```java
public record Comando(String nome, TipoCategoria tipo, UUID categoriaPaiId) {}
```

Adicionar validacao de hierarquia no metodo `executar()`:
```java
if (comando.categoriaPaiId() != null) {
    Categoria pai = repository.buscarPorId(comando.categoriaPaiId())
        .orElseThrow(() -> new CategoriaNaoEncontradaException(comando.categoriaPaiId()));
    if (pai.getCategoriaPaiId() != null) {
        throw new IllegalArgumentException(
            "Nao e permitido criar subcategoria de subcategoria");
    }
}
Categoria nova = new Categoria(comando.nome(), comando.tipo(), comando.categoriaPaiId());
```

Verificar se `CategoriaNaoEncontradaException` existe -- se sim, usar; se nao, criar.

### Mudancas na interface

**`CriarCategoriaRequest.java`** -- adicionar campo opcional:
```java
UUID categoriaPaiId     // sem @NotNull -- opcional
```

**`CategoriaResponse.java`** -- adicionar campo:
```java
UUID categoriaPaiId
```
Atualizar `fromDomain()` para incluir `categoria.getCategoriaPaiId()`.

**`CategoriaController.java`** -- atualizar mapeamento do Comando em `criar()`:
```java
new CriarCategoriaUseCase.Comando(request.nome(), request.tipo(), request.categoriaPaiId())
```

### Migration V9

```sql
ALTER TABLE categoria
    ADD COLUMN categoria_pai_id UUID,
    ADD CONSTRAINT fk_categoria_pai
        FOREIGN KEY (categoria_pai_id) REFERENCES categoria(id);
```

---

## Gap 2 -- Seed de categorias iniciais

### Migration V10

```sql
INSERT INTO categoria (id, nome, tipo, categoria_pai_id, criado_em, atualizado_em) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'Alimentacao',  'DESPESA', NULL, NOW(), NOW()),
    ('c0000000-0000-0000-0000-000000000002', 'Transporte',   'DESPESA', NULL, NOW(), NOW()),
    ('c0000000-0000-0000-0000-000000000003', 'Moradia',      'DESPESA', NULL, NOW(), NOW()),
    ('c0000000-0000-0000-0000-000000000004', 'Saude',        'DESPESA', NULL, NOW(), NOW()),
    ('c0000000-0000-0000-0000-000000000005', 'Educacao',     'DESPESA', NULL, NOW(), NOW()),
    ('c0000000-0000-0000-0000-000000000006', 'Lazer',        'DESPESA', NULL, NOW(), NOW()),
    ('c0000000-0000-0000-0000-000000000007', 'Vestuario',    'DESPESA', NULL, NOW(), NOW()),
    ('c0000000-0000-0000-0000-000000000008', 'Assinaturas',  'DESPESA', NULL, NOW(), NOW()),
    ('c0000000-0000-0000-0000-000000000009', 'Salario',      'RECEITA', NULL, NOW(), NOW()),
    ('c0000000-0000-0000-0000-000000000010', 'Freelance',    'RECEITA', NULL, NOW(), NOW()),
    ('c0000000-0000-0000-0000-000000000011', 'Renda Extra',  'RECEITA', NULL, NOW(), NOW());
```

Sem codigo Java -- apenas migration.

---

## Gap 3 -- Saldo total de todas as contas ativas

### Novo use case: `conta/application/CalcularSaldoTotalUseCase.java`

Injeta: `ContaRepository` + `CalcularSaldoDaContaUseCase`

```java
public record Resultado(Money saldoTotal, int totalContas) {}

@Transactional(readOnly = true)
public Resultado executar() {
    List<Conta> contasAtivas = contaRepository.listarAtivas();
    Money total = new Money(BigDecimal.ZERO, Currency.getInstance("BRL"));
    for (Conta conta : contasAtivas) {
        CalcularSaldoDaContaUseCase.Resultado saldo =
            calcularSaldoDaContaUseCase.executar(conta.getId());
        total = total.somar(saldo.saldoAtual());
    }
    return new Resultado(total, contasAtivas.size());
}
```

Nota: `Money.somar()` exige mesma moeda. Para MVP (moeda unica BRL) e aceitavel.

### Novo DTO: `conta/interfaces/dto/SaldoTotalResponse.java`

```java
public record SaldoTotalResponse(BigDecimal valor, String moeda, int totalContas) {
    public static SaldoTotalResponse fromResultado(CalcularSaldoTotalUseCase.Resultado r) {
        return new SaldoTotalResponse(
            r.saldoTotal().valor(),
            r.saldoTotal().moeda().getCurrencyCode(),
            r.totalContas()
        );
    }
}
```

### Novo endpoint em `ContaController.java`

Adicionar ANTES de `@GetMapping("/{id}")` para evitar conflito de rota:
```java
@GetMapping("/saldo-total")
@Transactional(readOnly = true)
public SaldoTotalResponse calcularSaldoTotal() {
    return SaldoTotalResponse.fromResultado(calcularSaldoTotalUseCase.executar());
}
```

Injetar `CalcularSaldoTotalUseCase` no construtor do controller.

---

## Testes

**Por convencao implicita (CLAUDE.md):**

Arquivos com mudancas que precisam de teste atualizado ou novo:
- `CategoriaTest.java` -- atualizar: construtores mudaram (adicionar categoriaPaiId)
- `CriarCategoriaUseCaseTest.java` -- atualizar: Comando tem novo campo
- `CategoriaControllerTest.java` -- atualizar: request/response tem novo campo
- `CalcularSaldoTotalUseCaseTest.java` -- NOVO (Mockito)
- `ContaControllerTest.java` -- atualizar: adicionar teste para GET /saldo-total

Para testes existentes: leia cada arquivo antes de editar, faça mudancas cirurgicas
apenas nos pontos afetados pela mudanca (nao reescrever testes que continuam validos).

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-8-gaps-pre-frontend

2. Leia todos os arquivos de referencia antes de implementar

3. Implementa Gap 1 (hierarquia):
   - Edita Categoria.java (domain)
   - Edita CategoriaRepository.java
   - Edita CategoriaEntity.java
   - Edita CategoriaJpaRepository.java (query methods)
   - Edita CategoriaMapper.java
   - Edita CategoriaRepositoryImpl.java
   - Edita CriarCategoriaUseCase.java
   - Edita CriarCategoriaRequest.java
   - Edita CategoriaResponse.java
   - Edita CategoriaController.java
   - Cria migration V9 (ALTER TABLE)
   - Atualiza CategoriaTest.java e CriarCategoriaUseCaseTest.java e CategoriaControllerTest.java

4. commit: feat(categoria): adiciona hierarquia pai-filho

5. Cria migration V10 (seed)
6. commit: feat(categoria): seed de categorias iniciais

7. Implementa Gap 3 (saldo total):
   - Cria CalcularSaldoTotalUseCase.java
   - Cria SaldoTotalResponse.java
   - Edita ContaController.java (novo endpoint + injecao)
   - Cria CalcularSaldoTotalUseCaseTest.java (Mockito)
   - Atualiza ContaControllerTest.java (novo endpoint)

8. commit: feat(conta): adiciona endpoint de saldo total

9. ./mvnw verify -- BUILD SUCCESS obrigatorio

10. Atualiza docs/progresso.md (registra 5.8)

11. commit: feat(backend): testes e documentacao; registra sub-etapa 5.8
    (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-8.md)

12. /ship → PR com reviews; corrigir apontamentos autonomamente
```

---

## Estrutura de commits (5.8)

```
feat(categoria): adiciona hierarquia pai-filho
feat(categoria): seed de categorias iniciais
feat(conta): adiciona endpoint de saldo total
feat(backend): testes e documentacao; registra sub-etapa 5.8
```

---

## Arquivos de referencia (ler antes de implementar)

- `categoria/domain/Categoria.java` -- construtor atual a atualizar
- `categoria/infrastructure/persistence/CategoriaEntity.java` -- estrutura atual
- `categoria/infrastructure/persistence/CategoriaMapper.java` -- mapper manual
- `categoria/application/CriarCategoriaUseCase.java` -- logica atual
- `categoria/interfaces/dto/CriarCategoriaRequest.java` -- request atual
- `categoria/interfaces/dto/CategoriaResponse.java` -- response atual
- `conta/application/CalcularSaldoDaContaUseCase.java` -- retorna Resultado.saldoAtual()
- `conta/domain/ContaRepository.java` -- ja tem listarAtivas()
- `conta/interfaces/ContaController.java` -- onde adicionar novo endpoint

---

## Restricoes

- NAO modificar outros bounded contexts (transacao, orcamento, meta, etc.)
- NAO alterar migraciones existentes (V1 a V8)
- So 1 nivel de hierarquia: subcategoria nao pode ter filhos (validar na use case)
- Seed usa UUIDs fixos (c0000000-...) para ser determinístico
- Se hook bloquear commit: ler a mensagem, corrigir sem --no-verify
- Testes existentes: editar cirurgicamente, nao reescrever

---

## Estado esperado ao terminar

- PR aberto com 4 commits acima de main.
- ./mvnw verify BUILD SUCCESS com todos os testes existentes + novos.
- GET /api/contas/saldo-total respondendo com soma das contas ativas.
- POST /api/categorias aceitando categoriaPaiId opcional.
- 11 categorias seed disponiveis apos migrations.
- docs/progresso.md com 5.8 registrada.
- docs/prompts/prompt-etapa-5-8.md commitado no ultimo commit.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO rodar /ship mais de uma vez.
- NAO criar nova tabela -- apenas ALTER TABLE na tabela categoria.
