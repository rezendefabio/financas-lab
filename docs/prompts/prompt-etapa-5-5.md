# Prompt -- Sub-etapa 5.5: Bounded context `relatorio`

## Contexto

Primeiro bounded context **pure-query** da Camada 4. Nao tem entidade de dominio, nao
tem migration, nao tem repositorio proprio. Apenas use cases de agregacao que consultam
repositorios de outros BCs e um controller REST com endpoints GET.

Padrao novo: bounded context sem camada domain/, sem camada infrastructure/, sem /feature
e sem /migrate. So application/ e interfaces/.

Sub-etapa 5.5 (Camada 4, quinto bounded context).

---

## Design do bounded context `relatorio`

### Application layer (2 use cases)

#### 1. `GastosPorCategoriaUseCase`

Injeta: `TransacaoRepository` + `CategoriaRepository`

**Parametros (inner record `Consulta`):**
```java
record Consulta(LocalDate dataInicio, LocalDate dataFim, UUID contaId)
// contaId nullable -- null significa todas as contas
```

**Logica:**
1. Monta `FiltrosTransacao(contaId, dataInicio, dataFim, TipoTransacao.DESPESA, null)`
2. Chama `transacaoRepository.listarComFiltros(filtros, Pageable.unpaged()).getContent()`
3. Agrupa transacoes por `categoriaId` usando streams
   - Transacoes sem categoria (categoriaId null) agrupadas sob chave `null`
4. Para cada grupo, soma os valores usando `Money.somar()` (ver Money.java -- tem metodo somar())
5. Para cada categoriaId nao-nulo, busca nome via `categoriaRepository.buscarPorId()`
   - Se nao encontrado: usa `"Categoria desconhecida"`
   - Se categoriaId null: usa `"Sem categoria"`
6. Ordena itens por totalGasto decrescente
7. Calcula totalGeral somando todos os itens

**Resultado (inner record `Resultado`):**
```java
record Resultado(
    LocalDate dataInicio,
    LocalDate dataFim,
    Money totalGeral,
    List<ItemGastoPorCategoria> itensPorCategoria
)

record ItemGastoPorCategoria(
    UUID categoriaId,       // null se sem categoria
    String nomeCategoria,   // "Sem categoria" se null
    Money totalGasto
)
```

Se nenhuma transacao encontrada: retornar Resultado com totalGeral=Money(ZERO, BRL)
e lista vazia. Usar `new Money(BigDecimal.ZERO, Currency.getInstance("BRL"))` para zeros.
Para agregacao: usar a moeda da primeira transacao do grupo como referencia.

**Anotacao:** `@Transactional(readOnly = true)` no metodo executar.

---

#### 2. `EvolucaoSaldoUseCase`

Injeta: `TransacaoRepository`

**Parametros (inner record `Consulta`):**
```java
record Consulta(LocalDate dataInicio, LocalDate dataFim, UUID contaId)
// contaId nullable
```

**Logica:**
1. Monta `FiltrosTransacao(contaId, dataInicio, dataFim, null, null)`
   (tipo null = todas as transacoes, sem filtro de tipo)
2. Chama `transacaoRepository.listarComFiltros(filtros, Pageable.unpaged()).getContent()`
3. Agrupa transacoes por `YearMonth.from(transacao.getData())`
4. Itera sobre todos os meses no intervalo [YearMonth.from(dataInicio) .. YearMonth.from(dataFim)]
   -- usar loop `while (!mesAtual.isAfter(mesFim))`
5. Para cada mes:
   - Filtra transacoes do mes no mapa
   - Soma RECEITA e DESPESA separadamente (ignorar TRANSFERENCIA)
   - Calcula saldoLiquido = totalReceitas.subtrair(totalDespesas) (ver Money.subtrair())
   - Zeros: `new Money(BigDecimal.ZERO, Currency.getInstance("BRL"))`
6. Ordena por mes ascendente
7. Calcula totais gerais (soma de todos os meses)

**Resultado (inner record `Resultado`):**
```java
record Resultado(
    LocalDate dataInicio,
    LocalDate dataFim,
    Money totalReceitas,
    Money totalDespesas,
    Money saldoLiquido,
    List<ItemEvolucaoMes> evolucaoPorMes
)

record ItemEvolucaoMes(
    LocalDate mes,          // primeiro dia do mes (YearMonth.atDay(1))
    Money totalReceitas,
    Money totalDespesas,
    Money saldoLiquido
)
```

**Anotacao:** `@Transactional(readOnly = true)` no metodo executar.

---

### Interface layer

#### `RelatorioController.java`

```java
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    @GetMapping("/gastos-por-categoria")
    public GastosPorCategoriaResponse gastosPorCategoria(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) UUID contaId) { ... }

    @GetMapping("/evolucao-saldo")
    public EvolucaoSaldoResponse evolucaoSaldo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) UUID contaId) { ... }
}
```

Ambos retornam 200. Sem @ResponseStatus necessario (200 e default para @GetMapping).

#### DTOs

**`GastosPorCategoriaResponse.java`:**
```java
public record GastosPorCategoriaResponse(
    LocalDate dataInicio,
    LocalDate dataFim,
    ValorMonetario totalGeral,
    List<ItemGastoResponse> itensPorCategoria
) {
    public record ItemGastoResponse(
        UUID categoriaId,
        String nomeCategoria,
        ValorMonetario totalGasto
    ) { }

    public static GastosPorCategoriaResponse fromResultado(
            GastosPorCategoriaUseCase.Resultado r) { ... }
}
```

**`EvolucaoSaldoResponse.java`:**
```java
public record EvolucaoSaldoResponse(
    LocalDate dataInicio,
    LocalDate dataFim,
    ValorMonetario totalReceitas,
    ValorMonetario totalDespesas,
    ValorMonetario saldoLiquido,
    List<ItemEvolucaoMesResponse> evolucaoPorMes
) {
    public record ItemEvolucaoMesResponse(
        LocalDate mes,
        ValorMonetario totalReceitas,
        ValorMonetario totalDespesas,
        ValorMonetario saldoLiquido
    ) { }

    public static EvolucaoSaldoResponse fromResultado(
            EvolucaoSaldoUseCase.Resultado r) { ... }
}
```

`ValorMonetario` -- mesmo record usado em OrcamentoResponse, MetaResponse, etc.:
```java
record ValorMonetario(BigDecimal valor, String moeda)
```

Verificar se ja existe um `ValorMonetario.java` compartilhado em algum lugar antes de
criar novo -- pode estar em `shared/` ou replicado em cada BC. Se replicado, criar
em `relatorio/interfaces/dto/` mesmo (manter padrao existente).

---

## Estrutura de arquivos

```
relatorio/
  application/
    GastosPorCategoriaUseCase.java
    EvolucaoSaldoUseCase.java
  interfaces/
    RelatorioController.java
    dto/
      GastosPorCategoriaResponse.java
      EvolucaoSaldoResponse.java
```

**Sem domain/** -- nao ha entidade de dominio propria.
**Sem infrastructure/** -- nao ha repositorio proprio, sem entity, sem migration.

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-5-relatorio

2. NAO invocar /feature (nao ha entity stub a criar -- padrao diferente)
   NAO invocar /migrate (nao ha tabela nova)

3. Criar diretamente os 2 use cases em relatorio/application/

4. commit: feat(relatorio): implementa application layer (2 use cases de agregacao)

5. Criar RelatorioController + 2 DTOs em relatorio/interfaces/

6. Por convencao implicita (CLAUDE.md):
   - /write-test GastosPorCategoriaUseCase.java (Mockito)
   - /write-test EvolucaoSaldoUseCase.java (Mockito)
   - /write-test RelatorioController.java (MockMvc E2E)
   Leia a skill /write-test, entenda o padrao e execute manualmente.

7. ./mvnw verify -- BUILD SUCCESS obrigatorio

8. Atualiza docs/progresso.md (registra 5.5 e padrao novo: BC pure-query)

9. commit: feat(relatorio): implementa interface layer e testes; registra sub-etapa 5.5
   (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-5.md)

10. /ship → PR aberto com reviews automaticos; corrigir apontamentos autonomamente
    se necessario (convencao do CLAUDE.md / Passo 5.1 do /ship)
```

---

## Estrutura de commits (5.5)

```
feat(relatorio): implementa application layer (2 use cases de agregacao)
feat(relatorio): implementa interface layer e testes; registra sub-etapa 5.5
```

2 commits (sem skeleton, sem migration).

---

## Arquivos de referencia (ler antes de implementar)

- `transacao/domain/TransacaoRepository.java` -- interface a injetar
- `transacao/domain/FiltrosTransacao.java` -- record de filtros (tipo nullable)
- `transacao/domain/TipoTransacao.java` -- enum RECEITA, DESPESA, TRANSFERENCIA
- `categoria/domain/CategoriaRepository.java` -- buscarPorId para resolver nomes
- `shared/domain/Money.java` -- metodos somar() e subtrair() ja existem
- `orcamento/application/CalcularProgressoDoOrcamentoUseCase.java` -- padrao de
  injecao cross-BC + uso de Pageable.unpaged() + agrupamento de transacoes

Verificar se `ValorMonetario` ja existe como tipo compartilhado:
```powershell
Get-ChildItem -Path src -Recurse -Filter "ValorMonetario.java" | Select-Object FullName
```
Se existir: importar. Se nao: criar em relatorio/interfaces/dto/.

---

## Restricoes

- NAO modificar TransacaoRepository, CategoriaRepository ou qualquer BC existente.
- NAO criar migration.
- NAO invocar /feature ou /migrate -- este BC nao tem entity.
- Se /write-test nao souber lidar com use case sem entity proprio: implementar o
  teste manualmente seguindo o padrao de CalcularProgressoDoOrcamentoUseCaseTest.
- Se hook bloquear commit: ler a mensagem, corrigir sem --no-verify.

---

## Padrao novo a documentar em progresso.md

**BC pure-query (primeira ocorrencia):** bounded context `relatorio` nao tem entidade
de dominio, nao tem migration, nao tem repositorio proprio. So application/ e interfaces/.
Injeta repositorios de outros BCs diretamente nos use cases. Padrao para features de
agregacao/leitura que nao precisam de persistencia propria.

---

## Estado esperado ao terminar

- PR aberto com 2 commits acima de main.
- ./mvnw verify BUILD SUCCESS com todos os testes existentes + novos.
- Endpoints GET /api/relatorios/gastos-por-categoria e /api/relatorios/evolucao-saldo
  respondendo 200.
- docs/progresso.md com 5.5 e padrao BC pure-query documentados.
- docs/prompts/prompt-etapa-5-5.md commitado no ultimo commit.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO rodar /ship mais de uma vez.
- NAO criar domain/ ou infrastructure/ -- este BC nao tem essas camadas.
