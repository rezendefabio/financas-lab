# Prompt — Etapa 3.8: Saldo derivado da Conta (fecha Camada 2)

## Contexto

A Etapa 3.7 foi concluída e fechada via PR #36. Bounded context `transacao` está vivo ponta a ponta com 5 endpoints, paginação, 5 filtros opcionais combináveis e PUT funcional. Smoke test manual confirmou POST → GET → PUT → GET → DELETE → GET 404 funcionando contra Postgres real.

Esta etapa **fecha a Camada 2**: implementa o cálculo de saldo derivado da `Conta`, que é a primeira funcionalidade que **cruza dois bounded contexts** — `Conta` lê de `Transacao` para calcular seu saldo atual.

Quando esta etapa terminar, o produto começa a fazer sentido como SaaS de finanças: usuário registra receitas/despesas/transferências e vê o saldo atualizado da conta.

## Padrões que estreiam nesta etapa

1. **Primeiro use case que cruza dois bounded contexts** — `Conta` lê do mundo de `Transacao` via porta (TransacaoRepository)
2. **Primeira query agregada** — `SUM(CASE WHEN ... THEN ... ELSE 0 END)` em JPQL
3. **Primeiro `COALESCE(SUM(...), 0)` em agregação** — garante zero em conta sem transações (padrão consistente com COALESCE da 3.7)
4. **Primeiro DTO de resposta com dados derivados/calculados** — `SaldoResponse` com breakdown completo
5. **Primeiro tipo `SELECT new ...` em JPQL** — query agregada retornando record type-safe

## Escopo decidido (calibrado com operador antes da redação)

### Endpoint

```
GET /api/contas/{id}/saldo  →  200 ou 404
```

### Resposta (`SaldoResponse`)

```json
{
  "contaId": "1d7d3f71-4f08-486a-ac97-21e870e64b39",
  "saldoInicial": { "valor": 1000.00, "moeda": "BRL" },
  "totalReceitas": { "valor": 800.00, "moeda": "BRL" },
  "totalDespesas": { "valor": 200.00, "moeda": "BRL" },
  "totalTransferenciasEnviadas": { "valor": 100.00, "moeda": "BRL" },
  "totalTransferenciasRecebidas": { "valor": 0.00, "moeda": "BRL" },
  "saldoAtual": { "valor": 1500.00, "moeda": "BRL" },
  "calculadoEm": "2026-05-10T01:00:00.000Z"
}
```

**Fórmula:**
```
saldoAtual = saldoInicial
           + totalReceitas
           - totalDespesas
           - totalTransferenciasEnviadas
           + totalTransferenciasRecebidas
```

**Notas pontuais:**
- Cada valor é um objeto `{ valor, moeda }` — mesmo padrão usado em `ContaResponse`
- `calculadoEm` é `Instant.now()` no momento do cálculo
- Conta inativa (soft-deleted) retorna saldo normalmente — quem decide se mostra é o frontend
- Sem cache. Calcula on-demand toda vez

### Decisões arquiteturais (calibradas com o operador)

- **Saldo é conceito da Conta**, não da Transacao. Use case fica em `conta/application/`
- **TransacaoRepository (interface domain) ganha método `calcularTotaisPorConta(UUID)`** — porta no domínio de Transacao, mas chamada por Conta. Padrão consolidado da Camada 1: `Conta` define o que precisa do mundo de `Transacao` via porta
- **Implementação JPQL única** com `SUM(CASE WHEN...)` retornando 4 totais em uma viagem ao banco
- **Record DTO em `transacao/domain/`**: `TotaisTransacaoPorConta` com 4 `BigDecimal` (sem `Money` aqui — moeda vem da Conta no caller)
- **Endpoint vai no `ContaController` existente** (sub-recurso de Conta, URL `/api/contas/{id}/saldo`)
- **Sem cache.** On-demand sempre
- **`COALESCE(SUM(...), 0)`** na query para conta sem transações retornar zero, não null

### Localização dos arquivos

```
src/main/java/com/laboratorio/financas/
├── conta/
│   ├── application/
│   │   └── CalcularSaldoDaContaUseCase.java                ← novo
│   └── interfaces/
│       ├── ContaController.java                            ← edição (+1 endpoint)
│       └── dto/
│           └── SaldoResponse.java                          ← novo
└── transacao/
    ├── domain/
    │   ├── TotaisTransacaoPorConta.java                    ← novo (record)
    │   └── TransacaoRepository.java                        ← edição (+1 método)
    └── infrastructure/persistence/
        ├── TransacaoJpaRepository.java                     ← edição (+1 query JPQL)
        └── TransacaoRepositoryImpl.java                    ← edição (+impl)
```

### Domain — `TotaisTransacaoPorConta`

```java
package com.laboratorio.financas.transacao.domain;

import java.math.BigDecimal;

/**
 * Resultado da agregacao de transacoes por conta.
 *
 * Cada componente representa a soma absoluta de transacoes de um tipo
 * em que a conta participa. Calculo do saldo final fica a cargo do
 * caller (CalcularSaldoDaContaUseCase em conta/application/).
 *
 * Valores nunca sao null — agregacao vazia retorna zero (COALESCE no SQL).
 */
public record TotaisTransacaoPorConta(
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal totalTransferenciasEnviadas,
        BigDecimal totalTransferenciasRecebidas
) { }
```

**Notas:**
- Sem `Money` aqui. `Money` exige currency, e a query SQL não traz currency code (vem da `Conta`). O `Money` é construído no use case quando soma com `saldoInicial`.
- Tudo em `BigDecimal`. Escala 2 vem do banco (`numeric(19, 2)`).
- Validação no construtor? **Não.** É record do banco — confiamos que SQL não retorna lixo.

### Repository (interface domain) — `TransacaoRepository`

Adicionar método:

```java
TotaisTransacaoPorConta calcularTotaisPorConta(UUID contaId);
```

Imports a adicionar: nada novo (já tem `UUID`).

### `TransacaoJpaRepository` — query JPQL agregada

```java
@Query("""
        SELECT new com.laboratorio.financas.transacao.domain.TotaisTransacaoPorConta(
            COALESCE(SUM(CASE WHEN t.tipo = com.laboratorio.financas.transacao.domain.TipoTransacao.RECEITA AND t.contaId = :contaId THEN t.valor.valor ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.tipo = com.laboratorio.financas.transacao.domain.TipoTransacao.DESPESA AND t.contaId = :contaId THEN t.valor.valor ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.tipo = com.laboratorio.financas.transacao.domain.TipoTransacao.TRANSFERENCIA AND t.contaId = :contaId THEN t.valor.valor ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN t.tipo = com.laboratorio.financas.transacao.domain.TipoTransacao.TRANSFERENCIA AND t.contaDestinoId = :contaId THEN t.valor.valor ELSE 0 END), 0)
        )
        FROM TransacaoEntity t
        WHERE t.contaId = :contaId OR t.contaDestinoId = :contaId
        """)
TotaisTransacaoPorConta calcularTotaisPorConta(@Param("contaId") UUID contaId);
```

**Notas críticas:**

1. **`SELECT new <FQCN>(...)` exige path completo da classe** no JPQL — `com.laboratorio.financas.transacao.domain.TotaisTransacaoPorConta`. Hibernate não respeita imports do arquivo Java.
2. **Enum no JPQL exige path completo também** — `com.laboratorio.financas.transacao.domain.TipoTransacao.RECEITA`. Sem isso, JPQL não resolve.
3. **`t.valor.valor`** é o caminho até o BigDecimal dentro do MoneyEmbeddable embutido (`MoneyEmbeddable.valor`). Confirmar nome do campo em `MoneyEmbeddable.java` antes de assumir — se for `getValor()` getter, o caminho continua sendo `t.valor.valor` (JPQL navega via property name, não getter).
4. **`WHERE t.contaId = :contaId OR t.contaDestinoId = :contaId`** filtra todas as transações em que a conta participa — origem ou destino. Necessário pra capturar transferências recebidas. Sem isso, agregação `transferencias_recebidas` ficaria sempre zero porque a row da transferência sai da query antes do CASE.
5. **`COALESCE(SUM(...), 0)`** garante zero em vez de null quando não há transação que case com o CASE. Sem isso, conta nova retornaria 4 nulls e o construtor do record receberia null em todos os campos.
6. **`AND t.contaId = :contaId`** dentro do `CASE` é redundante com o `WHERE`? Não — o `WHERE` admite ambos (origem ou destino), mas dentro do `CASE` precisamos ser específicos pra não contar a despesa da conta como "despesa da conta destino" indevidamente. Os 3 primeiros CASEs explicitam `contaId = :contaId` (transação onde a conta é origem), o 4º explicita `contaDestinoId = :contaId` (transação onde é destino).

**Possível pegadinha de Hibernate:** parsing de `CASE WHEN com.path.Enum.VALOR THEN coluna ELSE literal` em algumas versões do Hibernate é frágil. Se quebrar, alternativa idiomática:

```java
// Aceitavel se o SELECT new com path completo de enum nao funcionar:
SELECT new ...(
    COALESCE(SUM(CASE WHEN t.tipo = ?1 AND t.contaId = ?2 THEN t.valor.valor ELSE 0 END), 0),
    ...
)
```

E passar enum como parâmetro. **Hard line:** se a query primária quebrar, parar e reportar — não cair em workaround silencioso. Padrão consolidado.

### `TransacaoRepositoryImpl` — implementar `calcularTotaisPorConta`

```java
@Override
public TotaisTransacaoPorConta calcularTotaisPorConta(UUID contaId) {
    return jpaRepository.calcularTotaisPorConta(contaId);
}
```

Sem mapeamento — o JpaRepository já retorna o tipo do domain via `SELECT new`.

### Use case — `CalcularSaldoDaContaUseCase`

```java
package com.laboratorio.financas.conta.application;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaNaoEncontradaException;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.TotaisTransacaoPorConta;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CalcularSaldoDaContaUseCase {

    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;

    public CalcularSaldoDaContaUseCase(
            ContaRepository contaRepository,
            TransacaoRepository transacaoRepository
    ) {
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
    }

    public record Resultado(
            UUID contaId,
            Money saldoInicial,
            Money totalReceitas,
            Money totalDespesas,
            Money totalTransferenciasEnviadas,
            Money totalTransferenciasRecebidas,
            Money saldoAtual,
            Instant calculadoEm
    ) { }

    @Transactional(readOnly = true)
    public Resultado executar(UUID contaId) {
        Conta conta = contaRepository.buscarPorId(contaId)
                .orElseThrow(() -> new ContaNaoEncontradaException(contaId));

        TotaisTransacaoPorConta totais = transacaoRepository.calcularTotaisPorConta(contaId);

        Currency moeda = conta.getSaldoInicial().moeda();
        Money saldoInicial = conta.getSaldoInicial();
        Money totalReceitas = new Money(totais.totalReceitas(), moeda);
        Money totalDespesas = new Money(totais.totalDespesas(), moeda);
        Money totalTransferenciasEnviadas = new Money(totais.totalTransferenciasEnviadas(), moeda);
        Money totalTransferenciasRecebidas = new Money(totais.totalTransferenciasRecebidas(), moeda);

        BigDecimal saldoAtualValor = saldoInicial.valor()
                .add(totais.totalReceitas())
                .subtract(totais.totalDespesas())
                .subtract(totais.totalTransferenciasEnviadas())
                .add(totais.totalTransferenciasRecebidas());
        Money saldoAtual = new Money(saldoAtualValor, moeda);

        return new Resultado(
                contaId,
                saldoInicial,
                totalReceitas,
                totalDespesas,
                totalTransferenciasEnviadas,
                totalTransferenciasRecebidas,
                saldoAtual,
                Instant.now()
        );
    }
}
```

**Notas críticas:**

1. **Validação de existência da Conta primeiro.** Se conta não existe, lança `ContaNaoEncontradaException` (já existe — handler global mapeia pra 404). Não faz sentido calcular saldo de conta inexistente.
2. **Moeda vem da Conta** (`conta.getSaldoInicial().moeda()`). Todas as transações da Conta devem estar na mesma moeda (regra implícita do MVP single-currency BRL). Se em algum momento permitirmos multi-moeda, esta linha quebra — e essa é exatamente a sinalização desejada.
3. **`@Transactional(readOnly = true)`** — duas leituras (Conta + agregação Transacao) em transação read-only. Padrão.
4. **`calculadoEm = Instant.now()`** dentro do use case, não no controller. Lógica de negócio fica fora da camada de interfaces.

### DTO — `SaldoResponse`

```java
package com.laboratorio.financas.conta.interfaces.dto;

import com.laboratorio.financas.conta.application.CalcularSaldoDaContaUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaldoResponse(
        UUID contaId,
        ValorMonetario saldoInicial,
        ValorMonetario totalReceitas,
        ValorMonetario totalDespesas,
        ValorMonetario totalTransferenciasEnviadas,
        ValorMonetario totalTransferenciasRecebidas,
        ValorMonetario saldoAtual,
        Instant calculadoEm
) {

    public record ValorMonetario(BigDecimal valor, String moeda) { }

    public static SaldoResponse fromResultado(CalcularSaldoDaContaUseCase.Resultado r) {
        return new SaldoResponse(
                r.contaId(),
                toValorMonetario(r.saldoInicial().valor(), r.saldoInicial().moeda().getCurrencyCode()),
                toValorMonetario(r.totalReceitas().valor(), r.totalReceitas().moeda().getCurrencyCode()),
                toValorMonetario(r.totalDespesas().valor(), r.totalDespesas().moeda().getCurrencyCode()),
                toValorMonetario(r.totalTransferenciasEnviadas().valor(), r.totalTransferenciasEnviadas().moeda().getCurrencyCode()),
                toValorMonetario(r.totalTransferenciasRecebidas().valor(), r.totalTransferenciasRecebidas().moeda().getCurrencyCode()),
                toValorMonetario(r.saldoAtual().valor(), r.saldoAtual().moeda().getCurrencyCode()),
                r.calculadoEm()
        );
    }

    private static ValorMonetario toValorMonetario(BigDecimal valor, String moeda) {
        return new ValorMonetario(valor, moeda);
    }
}
```

**Notas:**
- Record aninhado `ValorMonetario` mantém shape `{valor, moeda}` consistente com `ContaResponse`.
- `fromResultado` é static factory — padrão consolidado.
- Sem importar `Money` no DTO (separação de camadas — DTOs não conhecem domain types diretamente).

### Controller — endpoint novo em `ContaController`

Adicionar (sem tocar nos endpoints existentes):

```java
@GetMapping("/{id}/saldo")
public SaldoResponse calcularSaldo(@PathVariable UUID id) {
    CalcularSaldoDaContaUseCase.Resultado resultado = calcularSaldoDaContaUseCase.executar(id);
    return SaldoResponse.fromResultado(resultado);
}
```

Adicionar dependência no construtor:
- Campo `private final CalcularSaldoDaContaUseCase calcularSaldoDaContaUseCase;`
- Parâmetro no construtor + atribuição

Imports adicionais:
- `com.laboratorio.financas.conta.application.CalcularSaldoDaContaUseCase`
- `com.laboratorio.financas.conta.interfaces.dto.SaldoResponse`

### Não tocar em `GlobalExceptionHandler` ou `SecurityConfig`

`ContaNaoEncontradaException` já está mapeado (handler da 3.4). Whitelist `/api/contas/**` já cobre `/api/contas/{id}/saldo`. Sem alterações.

### Testes

**`CalcularSaldoDaContaUseCaseTest`** — Mockito programático, ~10 testes:

1. **Conta existe + sem transações** → totais zero, saldoAtual = saldoInicial
2. **Conta existe + só receita 500** → saldoAtual = saldoInicial + 500
3. **Conta existe + só despesa 200** → saldoAtual = saldoInicial - 200
4. **Conta existe + transferência enviada 100** → saldoAtual = saldoInicial - 100
5. **Conta existe + transferência recebida 100** → saldoAtual = saldoInicial + 100
6. **Conta com cenário misto** (receita 800, despesa 200, transferência enviada 100, transferência recebida 50) → saldoAtual = saldoInicial + 800 - 200 - 100 + 50
7. **Conta inexistente** → `ContaNaoEncontradaException`
8. **`calculadoEm` está em janela razoável** — testa que é dentro de últimos 5 segundos
9. **Moeda preservada** — Conta em BRL → todos os Moneys em BRL
10. **Mock do `transacaoRepository.calcularTotaisPorConta` é chamado uma vez** com o contaId correto

**`ContaControllerTest`** — adicionar testes ao arquivo existente, ~5 novos testes:

1. **GET /api/contas/{id}/saldo** com conta sem transações → 200, saldoAtual = saldoInicial, todos os totais zero
2. **GET /api/contas/{id}/saldo** com conta + 1 receita persistida → 200, totalReceitas = valor, saldoAtual reflete
3. **GET /api/contas/{id}/saldo** com cenário completo (receita + despesa + transferência saída + transferência entrada) → 200, saldoAtual = fórmula completa
4. **GET /api/contas/{id}/saldo** com conta inexistente → 404 com ProblemDetail
5. **Conta inativa retorna saldo normalmente** → cria conta, desativa via `desativar()`, persiste, GET saldo → 200 com saldo correto

**Cleanup `@AfterEach`** mantém ordem `transacao → categoria → conta` para respeitar FKs (já estabelecido na 3.7).

**Atenção:** os testes 2-4 do controller exigem criar `Categoria` e `Conta`s reais persistidas + `Transacao`s reais persistidas via repositórios. Reusar helpers já estabelecidos no `TransacaoControllerTest` (copiá-los, não compartilhar — bounded contexts diferentes).

### JaCoCo

Todos os thresholds já ativos. Sem alteração no `pom.xml`. Cobertura esperada de domain (record `TotaisTransacaoPorConta`) ≥90%, application (use case) ≥80%, infrastructure (query) ≥60%, interfaces (controller + DTO) ≥70%.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra commit do squash da Etapa 3.7 com referência a PR #36
- `docs/prompt-etapa-3-8.md` presente como untracked
- Working tree limpo
- `conta/application/` existente com 4 use cases, **sem** `CalcularSaldoDaContaUseCase`
- `transacao/domain/` existente, **sem** `TotaisTransacaoPorConta`
- `TransacaoRepository` (interface) com 4 métodos atuais

Validar com:

```bash
git status
git log --oneline -3
ls docs/prompt-etapa-3-8.md
ls -la src/main/java/com/laboratorio/financas/conta/application/
ls -la src/main/java/com/laboratorio/financas/transacao/domain/
```

## Tarefas

### Tarefa 1 — Validar pré-requisitos

```bash
git status
git log --oneline -3
ls docs/prompt-etapa-3-8.md
ls -la src/main/java/com/laboratorio/financas/conta/application/
ls -la src/main/java/com/laboratorio/financas/conta/interfaces/dto/
ls -la src/main/java/com/laboratorio/financas/transacao/domain/
ls -la src/main/java/com/laboratorio/financas/transacao/infrastructure/persistence/
```

Esperado:
- `conta/application/` com 4 use cases (Criar, Listar, BuscarPorId, Desativar) — **sem** CalcularSaldo
- `conta/interfaces/dto/` com ContaRequest e ContaResponse — **sem** SaldoResponse
- `transacao/domain/` com Transacao, TipoTransacao, TransacaoRepository, FiltrosTransacao, e 2 exceções — **sem** TotaisTransacaoPorConta

Se algum item divergir, parar e reportar.

### Tarefa 2 — Criar branch de trabalho

```bash
git checkout -b feat/saldo-derivado-conta
```

### Tarefa 3 — Antes de escrever, ler código vivo

```bash
cat src/main/java/com/laboratorio/financas/conta/domain/Conta.java
cat src/main/java/com/laboratorio/financas/conta/domain/ContaRepository.java
cat src/main/java/com/laboratorio/financas/conta/domain/ContaNaoEncontradaException.java
cat src/main/java/com/laboratorio/financas/conta/application/BuscarContaPorIdUseCase.java
cat src/main/java/com/laboratorio/financas/conta/interfaces/ContaController.java
cat src/main/java/com/laboratorio/financas/conta/interfaces/dto/ContaResponse.java
cat src/main/java/com/laboratorio/financas/transacao/domain/TransacaoRepository.java
cat src/main/java/com/laboratorio/financas/transacao/infrastructure/persistence/TransacaoJpaRepository.java
cat src/main/java/com/laboratorio/financas/transacao/infrastructure/persistence/TransacaoRepositoryImpl.java
cat src/main/java/com/laboratorio/financas/shared/domain/Money.java
cat src/main/java/com/laboratorio/financas/shared/infrastructure/persistence/MoneyEmbeddable.java
```

Replicar fielmente os padrões. **Código vivo > esboço do prompt** quando divergirem.

### Tarefa 4 — Criar `TotaisTransacaoPorConta` no domain

`transacao/domain/TotaisTransacaoPorConta.java` conforme especificado.

### Tarefa 5 — Atualizar `TransacaoRepository` (interface domain)

Adicionar método `calcularTotaisPorConta(UUID contaId): TotaisTransacaoPorConta`.

### Tarefa 6 — Atualizar `TransacaoJpaRepository`

Adicionar método `calcularTotaisPorConta` com query JPQL agregada conforme especificado.

**Atenção crítica:**
- `SELECT new com.laboratorio.financas.transacao.domain.TotaisTransacaoPorConta(...)` — path completo da classe
- Enums com path completo: `com.laboratorio.financas.transacao.domain.TipoTransacao.RECEITA` (etc)
- `t.valor.valor` — caminho até BigDecimal dentro do MoneyEmbeddable. Confirmar nome exato do campo em `MoneyEmbeddable.java` antes de assumir
- `COALESCE(SUM(...), 0)` em todos os 4 SUMs
- `WHERE t.contaId = :contaId OR t.contaDestinoId = :contaId` para capturar transferências recebidas

**Se a query quebrar com erro de parsing JPQL ou Hibernate:** parar e reportar — não silenciar com workaround.

### Tarefa 7 — Atualizar `TransacaoRepositoryImpl`

Implementar `calcularTotaisPorConta` delegando direto ao JpaRepository (sem mapper — JpaRepository já retorna tipo do domain via `SELECT new`).

### Tarefa 8 — Criar `CalcularSaldoDaContaUseCase`

`conta/application/CalcularSaldoDaContaUseCase.java` conforme especificado. Construir `Money`s a partir do `BigDecimal` da agregação + currency da Conta.

### Tarefa 9 — Criar `SaldoResponse`

`conta/interfaces/dto/SaldoResponse.java` conforme especificado. Record com record aninhado `ValorMonetario` e static factory `fromResultado`.

### Tarefa 10 — Atualizar `ContaController`

Adicionar:
- Campo `private final CalcularSaldoDaContaUseCase calcularSaldoDaContaUseCase;`
- Parâmetro no construtor + atribuição
- Endpoint `GET /api/contas/{id}/saldo` retornando `SaldoResponse`

Não tocar nos endpoints existentes.

### Tarefa 11 — Criar testes do use case

`test/.../conta/application/CalcularSaldoDaContaUseCaseTest.java` com ~10 testes Mockito programáticos conforme spec.

### Tarefa 12 — Adicionar testes ao `ContaControllerTest`

Adicionar ~5 novos testes ao arquivo existente. **Não criar arquivo novo** — extender o existente.

Setup helpers para criar Categoria, Contas e Transacoes persistidas seguem o padrão estabelecido no `TransacaoControllerTest`. Copiá-los (não compartilhar entre arquivos de teste — manter independência).

### Tarefa 13 — Validar localmente

```bash
.\mvnw.cmd compile
.\mvnw.cmd test -Dtest='CalcularSaldoDaContaUseCaseTest'
.\mvnw.cmd clean verify
```

**Atenção: `clean verify`, não só `verify`.** Etapa 3.7 mostrou que cache de build pode dar falso positivo. Forçar build limpo é não-negociável agora.

**Esperado:**
- BUILD SUCCESS
- ~15 novos testes (~280 total)
- Checkstyle 0 violações, SpotBugs 0 issues
- JaCoCo todos os thresholds atendidos
- Sem warnings novos no build

**Possíveis pontos de atrito (parar e reportar):**

1. **JPQL com `SELECT new` falha por parsing**: caminho fully-qualified incorreto, enum sem path completo, ou nome de campo errado em `t.valor.valor`. Confirmar com `cat MoneyEmbeddable.java` antes.
2. **Erro "could not determine data type of parameter"**: improvável aqui (não usamos `IS NULL`), mas se aparecer, parar — pode exigir adaptação similar ao COALESCE da 3.7.
3. **Cobertura JaCoCo do record `TotaisTransacaoPorConta`**: records sem lógica não geram instruções pra cobrir além do canonical constructor. Pode passar com 0% reportado pelo JaCoCo. Se threshold quebrar especificamente nesse arquivo, **não** adicionar exclusão silenciosa — parar e reportar; talvez precise ajustar pacote no threshold ou adicionar teste de instanciação trivial.
4. **`@Transactional(readOnly = true)` cruzando bounded contexts**: precisa confirmar que `ContaRepository` e `TransacaoRepository` são chamados dentro da mesma transação Spring. Sem `@Transactional` cada repositório abre sua própria. Read-only é seguro porque ambas são leituras.
5. **Indentação Checkstyle**: 16 espaços nas continuações. Especial atenção ao `CalcularSaldoDaContaUseCase` que tem chamadas longas de `BigDecimal.add().subtract()`.

### Tarefa 14 — Atualizar `docs/decisoes.md`

**14a.** Adicionar nota em "Padrões aplicados":

```markdown
- **Cruzamento entre bounded contexts via porta no domain** (a partir da Etapa 3.8): quando um bounded context precisa ler estado de outro (ex: `Conta` calcular saldo a partir de `Transacao`), o consumidor define o método na interface de repositório do produtor (ex: `TransacaoRepository.calcularTotaisPorConta`). Implementação fica em `transacao/infrastructure/`. Sem use case do bounded context A chamando use case do B — apenas via portas (interfaces de domínio). Mantém baixo acoplamento entre contextos.
- **Agregação SQL com `COALESCE(SUM(CASE WHEN...), 0)`** (a partir da Etapa 3.8): para totais condicionais por tipo, usar `SUM(CASE WHEN tipo = X THEN valor ELSE 0 END)` envelopado em `COALESCE(..., 0)` para garantir zero em conjunto vazio. JPQL `SELECT new com.path.Record(...)` com tipo path completo + enums fully-qualified.
- **`mvnw clean verify` antes de declarar etapa pronta** (consolidado na Etapa 3.8 a partir de incidente da 3.7): build local sem `clean` pode dar falso positivo por cache de compilação. CI sempre roda do zero — local deve replicar isso na validação final.
```

**14b.** Adicionar entrada no histórico:

```markdown
- **2026-05-10** — Etapa 3.8 concluída: saldo derivado da Conta. `CalcularSaldoDaContaUseCase` em `conta/application/` cruza bounded context lendo de `TransacaoRepository.calcularTotaisPorConta` (porta no domain de Transacao). Query JPQL agregada com `SUM(CASE WHEN ...)` retornando record `TotaisTransacaoPorConta` via `SELECT new`. Endpoint `GET /api/contas/{id}/saldo` retorna `SaldoResponse` com breakdown completo (saldoInicial, 4 totais, saldoAtual, calculadoEm). Camada 2 fechada. Mergeado via PR #XX.
```

### Tarefa 15 — Atualizar `docs/progresso.md`

**15a.** Atualizar "Última atualização": `2026-05-10 (Etapa 3.8 — saldo derivado, Camada 2 fechada)`.

**15b.** Marcar Camada 2 como ✅ Concluída na tabela de status geral.

**15c.** Adicionar seção "Lições da Etapa 3.7" com candidatos a hook + lições reais (recapitulando o que aprendemos: COALESCE para tipos sensíveis em filtros opcionais, `mvnw clean verify` obrigatório, decisão silenciosa escondendo problema técnico real).

**15d.** Adicionar seção "Lições da Etapa 3.8" — só observações reais.

**15e.** Adicionar entrada no histórico:

```markdown
- **2026-05-10** — Etapa 3.8 concluída: saldo derivado da Conta. Endpoint GET /api/contas/{id}/saldo, primeiro cruzamento entre bounded contexts via porta no domain, primeira query agregada JPQL. Camada 2 fechada. Mergeado via PR #XX.
```

### Tarefa 16 — Versionar este próprio prompt

`docs/prompt-etapa-3-8.md` no commit de docs.

### Tarefa 17 — Validação final antes de commitar

```bash
find src/main/java/com/laboratorio/financas/conta/application -name "CalcularSaldo*.java" -exec sh -c 'echo "$1:"; xxd "$1" | head -1' _ {} \;
find src/main/java/com/laboratorio/financas/conta/interfaces/dto -name "SaldoResponse.java" -exec sh -c 'echo "$1:"; xxd "$1" | head -1' _ {} \;
find src/main/java/com/laboratorio/financas/transacao/domain -name "TotaisTransacaoPorConta.java" -exec sh -c 'echo "$1:"; xxd "$1" | head -1' _ {} \;

.\mvnw.cmd clean verify

git status
```

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Permitidos:
   - Arquivos novos: `TotaisTransacaoPorConta.java`, `CalcularSaldoDaContaUseCase.java`, `SaldoResponse.java`, e seus respectivos testes
   - Edição em `TransacaoRepository.java` (+1 método)
   - Edição em `TransacaoJpaRepository.java` (+1 query JPQL)
   - Edição em `TransacaoRepositoryImpl.java` (+1 impl)
   - Edição em `ContaController.java` (+1 endpoint, +1 dependência)
   - Edição em `ContaControllerTest.java` (+5 testes)
   - `docs/decisoes.md`, `docs/progresso.md`, `docs/prompt-etapa-3-8.md`

2. **Não tocar em `Transacao.java`, `TipoTransacao.java`, `TransacaoEntity.java`, `TransacaoMapper.java`, `Conta.java`, `ContaEntity.java`, `ContaMapper.java`, `Money.java`, `MoneyEmbeddable.java`, `ContaRepository.java`, `TransacaoController.java`, `GlobalExceptionHandler.java`, `SecurityConfig.java`.**

3. **Não criar nova migration.** Esta etapa não muda schema.

4. **Não tocar em `pom.xml`, `application*.yml`, `docker-compose.yml`, `scripts/*.ps1`, `frontend/`.**

5. **Não criar mais use cases além do prescrito.** Sem `CalcularSaldoComCacheUseCase`, sem `CalcularSaldoConsolidadoDeTodasAsContasUseCase`. Esta etapa entrega 1 use case.

6. **Não criar mais endpoints além do prescrito.** Sem GET /api/contas/saldo (todas), sem POST /api/contas/{id}/saldo/recalcular. Esta etapa entrega 1 endpoint.

7. **Não criar `SaldoConsolidadoUseCase`, `RelatorioMensalUseCase`, ou qualquer feature que combine `Conta` + `Transacao` além de saldo. Esta etapa entrega saldo de conta única, ponto.**

8. **Não adicionar cache** (`@Cacheable`, Caffeine, Redis para saldo). Conta única + cálculo on-demand. Cache vira decisão se virar gargalo (improvável em MVP).

9. **Não criar `SaldoCalculadoEvent` ou outro padrão de evento.** Sem domain events nesta etapa.

10. **Não tocar em `GlobalExceptionHandler`.** `ContaNaoEncontradaException` já está mapeado da 3.4. Reusa.

11. **Não tocar em `SecurityConfig`.** Whitelist `/api/contas/**` já cobre `/api/contas/{id}/saldo`. Sem entradas adicionais.

12. **Não usar Lombok.**

13. **Não relaxar Checkstyle, SpotBugs, JaCoCo.** Se algum threshold quebrar especificamente em `TotaisTransacaoPorConta` por record sem lógica, parar e reportar — não silenciar com exclusão.

14. **Sem acentos no código Java.**

15. **Encoding UTF-8 sem BOM.**

16. **Naming de teste em camelCase puro, sem underscore.** Padrão consolidado.

17. **Indentação Checkstyle**: 16 espaços nas continuações. Especial atenção ao `CalcularSaldoDaContaUseCase` que tem cadeia longa de `BigDecimal.add().subtract()`.

18. **Antes de escrever cada classe, ler a contraparte ou referência** (Tarefa 3). Código vivo > prompt.

19. **Validação destrutiva manual** é responsabilidade do operador, pós-merge. Documentar no PR body os cenários a validar.

20. **`mvnw clean verify` antes de declarar pronto.** `clean` é obrigatório nesta etapa. Lição da 3.7.

21. **Não tomar decisão silenciosa em zona limítrofe.** Sexta recorrência registrada na 3.7. Padrão consolidado: parar e reportar quando solução prescrita falhar, **especialmente** em problemas técnicos reais (não só estilísticos).

22. **Lições da Etapa 3.8 só registram observações reais.**

23. **Não antecipar features de Camada 3.** Sem hooks, sem subagents, sem skills.

## Estrutura de commits

Branch: `feat/saldo-derivado-conta`

**Commit 1** — `feat(transacao): adiciona TotaisTransacaoPorConta no dominio`
- 1 arquivo novo (`transacao/domain/TotaisTransacaoPorConta.java`)

**Commit 2** — `feat(transacao): adiciona calcularTotaisPorConta no Repository (porta + impl + JPQL)`
- Edição em `transacao/domain/TransacaoRepository.java`
- Edição em `transacao/infrastructure/persistence/TransacaoJpaRepository.java`
- Edição em `transacao/infrastructure/persistence/TransacaoRepositoryImpl.java`

**Commit 3** — `feat(conta): adiciona CalcularSaldoDaContaUseCase cruzando bounded contexts via porta`
- 1 arquivo novo (`conta/application/CalcularSaldoDaContaUseCase.java`)

**Commit 4** — `test(conta): cobertura unitaria do CalcularSaldoDaContaUseCase`
- 1 arquivo novo (`test/.../conta/application/CalcularSaldoDaContaUseCaseTest.java`)

**Commit 5** — `feat(conta): adiciona SaldoResponse e endpoint GET /api/contas/{id}/saldo`
- 1 arquivo novo (`conta/interfaces/dto/SaldoResponse.java`)
- Edição em `conta/interfaces/ContaController.java`

**Commit 6** — `test(conta): cobertura e2e do endpoint de saldo (5 cenarios)`
- Edição em `test/.../conta/interfaces/ContaControllerTest.java`

**Commit 7** — `docs: registra etapa 3.8 (saldo derivado, Camada 2 fechada) em decisoes e progresso`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-3-8.md`

## Validação antes de abrir PR

```bash
.\mvnw.cmd clean verify
git status
git log --oneline -8
```

Esperado: BUILD SUCCESS, working tree limpo, 7 commits.

## PR

Título: `feat: etapa 3.8 — saldo derivado da Conta (Camada 2 fechada)`

Body sugerido:

```markdown
## Summary

Implementa a Etapa 3.8 do roadmap (etapa final da Camada 2): cálculo de saldo derivado da `Conta`. Primeira funcionalidade do projeto que cruza dois bounded contexts — `Conta` lê de `Transacao` via porta no domain.

Quando esta etapa for mergeada, **a Camada 2 estará fechada** com 3 bounded contexts vivos ponta a ponta (`conta`, `categoria`, `transacao`) e o produto começa a fazer sentido como SaaS de finanças (registrar receitas/despesas/transferências e ver saldo).

### Padrões que estreiam aqui

1. **Cruzamento entre bounded contexts via porta no domain** — `CalcularSaldoDaContaUseCase` (em `conta/application/`) injeta `TransacaoRepository` (interface domain de Transacao). Sem use case A chamando use case B; apenas portas.
2. **Primeira query agregada JPQL** — `SELECT new TotaisTransacaoPorConta(SUM(CASE WHEN ... THEN ... ELSE 0 END), ...)` retornando record type-safe.
3. **`COALESCE(SUM(...), 0)` para conjunto vazio** — conta sem transações retorna zero, não null.
4. **Primeiro DTO de resposta com dados derivados** — `SaldoResponse` com breakdown completo (saldoInicial, totalReceitas, totalDespesas, totalTransferenciasEnviadas, totalTransferenciasRecebidas, saldoAtual, calculadoEm).

### Endpoint

```
GET /api/contas/{id}/saldo  →  200 ou 404
```

### Mudanças

- `transacao/domain/TotaisTransacaoPorConta.java`: record com 4 BigDecimals (sem `Money` — currency vem da Conta no caller).
- `transacao/domain/TransacaoRepository.calcularTotaisPorConta(UUID)`: porta no domain de Transacao.
- `transacao/infrastructure/persistence/TransacaoJpaRepository.calcularTotaisPorConta`: query JPQL agregada com 4 SUMs condicionais.
- `transacao/infrastructure/persistence/TransacaoRepositoryImpl.calcularTotaisPorConta`: delega ao JpaRepository.
- `conta/application/CalcularSaldoDaContaUseCase`: orquestra leitura da Conta + agregação Transacao, monta `Resultado` com 7 Moneys + Instant.
- `conta/interfaces/dto/SaldoResponse`: record com record aninhado `ValorMonetario`, static factory `fromResultado`.
- `conta/interfaces/ContaController`: +1 endpoint `GET /{id}/saldo`, +1 dependência.
- ~10 testes unitários (use case) + ~5 e2e (controller). Total ~280 testes.

### Decisões de escopo

- **Saldo é da Conta, não da Transacao.** Use case e DTO de response em `conta/`.
- **TransacaoRepository (interface domain) ganha método agregador.** Padrão de porta consolidado: cada bounded context define o que precisa de outro via interface.
- **Sem cache.** On-demand toda vez. Cacheia se virar gargalo (improvável em MVP).
- **Conta inativa retorna saldo normalmente.** Histórico financeiro continua relevante após "fechar" conta. Frontend decide se mostra.
- **Moeda vem da Conta.** Todas as transações da Conta na mesma moeda (regra implícita single-currency BRL do MVP). Multi-moeda romperia esta linha — sinalização desejada.

### Validação

- `mvnw clean verify` local: PASSOU
- ~15 testes novos passando, incluindo cenário misto com receitas + despesas + transferências entrada/saída
- Checkstyle: 0 violações, SpotBugs: 0 issues
- JaCoCo: todos os thresholds atendidos

### Validação destrutiva pós-merge sugerida

Smoke test ponta a ponta:
1. Criar Conta A (saldo inicial 1000)
2. Criar Conta B (saldo inicial 0)
3. Criar Categoria
4. Criar receita 500 em A
5. Criar despesa 200 em A
6. Criar transferência 100 de A para B
7. GET /api/contas/{A}/saldo → esperado: 1000 + 500 - 200 - 100 = 1200
8. GET /api/contas/{B}/saldo → esperado: 0 + 100 = 100
9. GET /api/contas/00000000-0000-0000-0000-000000000000/saldo → 404 com ProblemDetail

### Próximo passo

**Camada 2 fechada.** Próximo passo é Camada 3 (configuração do Claude Code: hooks mecânicos, subagents focados, skills de workflow), Camada 4 (modelo operacional Tier 2), ou Auth real (eliminar whitelists temporárias). Decisão fora deste PR.
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

- Branch `feat/saldo-derivado-conta` empurrada com 8 commits (7 + 1 update do PR)
- PR aberto, CI verde, **não mergeado**
- `main` ainda no squash da 3.7
- Working tree limpo
- Camada 2 pronta pra ser fechada após merge + smoke test
- Reportar com `git log --oneline -8`, `git status`, `gh pr view --json number,state,statusCheckRollup` e parar.

## O que NÃO fazer ao terminar

- Não mergear o PR.
- Não criar prompt da próxima etapa (Camada 3 ou Auth).
- Não rascunhar features de Camada 3 (hooks, subagents, skills).
- Não tocar em `frontend/`, `scripts/`, `docker-compose.yml`, `pom.xml`, `application*.yml`, migrations, `GlobalExceptionHandler`, `SecurityConfig`.
- Não criar mais use cases ou endpoints além dos prescritos.
- Não adicionar cache.
- Não sugerir "próximo passo" espontaneamente.
