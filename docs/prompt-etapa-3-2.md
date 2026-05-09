# Prompt — Etapa 3.2: Bounded Context `conta` — Domain Puro

## Contexto

A Etapa 3.1 foi concluída e fechada via PR #29. Value object `Money` está implementado em `shared/domain` com 100% de cobertura. Threshold JaCoCo de 90% em `domain/` está ativo. Camada 2 está 🟢 Em andamento.

Esta etapa abre o **primeiro bounded context de domínio do projeto**: `conta`. Implementa **apenas a camada de domínio** — entidade `Conta`, enum `TipoConta`, exceções de domínio se necessárias, e testes. Sem JPA, sem MapStruct, sem repository, sem controller, sem migration. Esses entram nas Etapas 3.3 e 3.4.

A divisão em três sub-etapas (3.2 domain / 3.3 infra+mapper / 3.4 application+interfaces) foi calibrada com o operador antes da redação. Razão: bounded context completo num único PR introduziria simultaneamente 7 padrões novos no projeto (primeira `@Entity`, primeira migration de domínio, primeiro MapStruct, primeiro repository pattern completo, primeiro use case, primeiro DTO de domínio, primeiro `@RestController` de domínio). Etapas pequenas com precedentes isoláveis valem mais para a fábrica que uma etapa monstra.

## Escopo decidido (calibrado com operador antes da redação)

### Modelagem de `Conta` no MVP single-user

- **`id`**: `UUID` gerado pela aplicação no construtor (não pelo banco, não em runtime de persistência). Facilita testes e evita round-trip ao banco para descobrir o id após criação.
- **`nome`**: `String`, obrigatório, validado: não-nulo, não-blank após trim, mínimo 1 char não-blank, máximo 100 chars.
- **`tipo`**: enum `TipoConta` com valores: `CORRENTE`, `POUPANCA`, `DINHEIRO`, `CARTAO_CREDITO`.
- **`saldoInicial`**: `Money`, obrigatório. Aceita zero, positivo e negativo (cartão de crédito pode iniciar com saldo negativo representando dívida; conta corrente em cheque especial idem).
- **`ativa`**: `boolean`. `true` na criação. Soft delete: método `desativar()` retorna nova instância com `ativa=false`. Não há `reativar()` no MVP — porta aberta se necessário depois.
- **`criadoEm`** / **`atualizadoEm`**: `Instant`. Auditoria mínima. `criadoEm` definido na construção e nunca muda. `atualizadoEm` é igual a `criadoEm` na criação e atualizado em mutações (apenas `desativar()` nesta etapa).
- **Sem `dono`/`usuarioId` no MVP.** Single-user. Adiar até multi-user entrar no escopo.
- **Sem `saldoAtual` nem método de cálculo.** Saldo atual é derivado de `saldoInicial + soma(transacoes)` e só faz sentido quando `transacao` existir (Etapa 3.5+). Nesta etapa, `Conta` **não tem campo nem método de saldo atual**. ADR-004 prevê isso explicitamente: "agregado raiz com invariantes — entrará em `Conta` quando saldo passar a ser derivado e exigir consistência forte". Decisão registrada no PR body.

### Decisões de design

- **`Conta` é classe imutável, não record.** Razão: tem 7 campos que precisam de validação coordenada no construtor + dois campos que mudam (`ativa`, `atualizadoEm`) em `desativar()`, retornando nova instância. Record com tantos campos vira poluição visual e o construtor compacto fica grande. Class imutável com `final` em todos os campos + getters explícitos é mais legível. Lombok `@Getter` é permitido por `decisoes.md` em entidades, mas não vamos usar nesta etapa para manter o domain 100% explícito (Lombok entra em `infrastructure/` quando ContaEntity aparecer na 3.3).
- **Construtores: dois.**
  1. Construtor "novo" (factory pra cliente externo): `Conta(String nome, TipoConta tipo, Money saldoInicial)`. Gera `id` (`UUID.randomUUID()`), define `ativa = true`, define `criadoEm = atualizadoEm = Instant.now()`.
  2. Construtor "reconstrução" (para o repository hidratar): `Conta(UUID id, String nome, TipoConta tipo, Money saldoInicial, boolean ativa, Instant criadoEm, Instant atualizadoEm)`. Recebe todos os campos. Usado pelo MapStruct na 3.3.
- **Sem `Clock` injetável nesta etapa.** `Instant.now()` direto. Simplifica testes (verificar que `criadoEm` está em janela razoável usando `isCloseTo` do AssertJ ou comparação com `Instant.now()` antes/depois). Se virar dor real em testes, refatora pra `Clock` numa etapa futura.
- **Validações de invariante**: `IllegalArgumentException` para input inválido (nulo, blank, fora do tamanho), consistente com Money. Sem exceção customizada `NomeContaInvalidoException` — overengineering pra esta etapa.
- **Igualdade por id.** Diferente de Money (record, igualdade estrutural), `Conta` é entidade — igualdade é por `id`, não pelos demais campos. Implementar `equals` e `hashCode` baseados apenas em `id`. **Esta é a regra clássica do DDD para entidades.**
- **`toString()` enxuto.** Implementar `toString()` que mostra `id`, `nome`, `tipo`, `ativa`. Não incluir `saldoInicial`, `criadoEm`, `atualizadoEm` (poluição em logs).
- **Localização**:
  - `src/main/java/com/laboratorio/financas/conta/domain/Conta.java`
  - `src/main/java/com/laboratorio/financas/conta/domain/TipoConta.java`
  - `src/test/java/com/laboratorio/financas/conta/domain/ContaTest.java`
  - `src/test/java/com/laboratorio/financas/conta/domain/TipoContaTest.java` (apenas se houver lógica no enum — atualmente é enum simples, talvez não justifique teste próprio; decidir durante implementação)

### JaCoCo

- **Threshold de `domain` 90% já está ativo** (Etapa 3.1). Esta etapa adiciona classes ao mesmo pacote, não muda configuração.
- **Não ativar threshold de `application/` nem `interfaces/`.** Continuam comentados aguardando 3.4.
- **Cobertura esperada de `Conta` e `TipoConta`: 100% de instrução.** Como na 3.1, threshold dá margem mas a meta real é 100%.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra commit do squash da Etapa 3.1 com referência a PR #29
- `docs/prompt-etapa-3-2.md` presente como untracked (este próprio arquivo, anexado pelo operador antes de iniciar). Se não aparecer em `git status`, parar e reportar.
- Working tree sem outras mudanças
- Pacote `com.laboratorio.financas.conta` **não existe ainda**. Esta etapa cria.
- Pacote `com.laboratorio.financas.shared.domain` existe com `Money.java`.

Validar com:

```bash
git status
git log --oneline -1
ls docs/prompt-etapa-3-2.md
ls -la src/main/java/com/laboratorio/financas/conta/ 2>/dev/null && echo "ATENCAO: conta/ ja existe" || echo "OK: conta/ ainda nao existe"
ls src/main/java/com/laboratorio/financas/shared/domain/Money.java
```

Esperado:
- Working tree limpo, exceto `docs/prompt-etapa-3-2.md` untracked
- Último commit em main referencia Etapa 3.1 / PR #29
- `conta/` **não existe**
- `Money.java` existe (vamos importar)

Se algum item divergir, parar e reportar.

## Tarefas

### Tarefa 1 — Validar pré-requisitos

```bash
git status
git log --oneline -3
ls docs/prompt-etapa-3-2.md
ls -la src/main/java/com/laboratorio/financas/conta/ 2>/dev/null && echo "ATENCAO" || echo "OK: conta/ ainda nao existe"
ls src/main/java/com/laboratorio/financas/shared/domain/Money.java
```

Se qualquer item divergir, parar e reportar.

### Tarefa 2 — Criar branch de trabalho

```bash
git checkout -b feat/conta-domain
```

### Tarefa 3 — Criar `TipoConta.java`

Criar `src/main/java/com/laboratorio/financas/conta/domain/TipoConta.java`.

Enum simples, 4 valores, sem comportamento adicional:

```java
package com.laboratorio.financas.conta.domain;

public enum TipoConta {
    CORRENTE,
    POUPANCA,
    DINHEIRO,
    CARTAO_CREDITO
}
```

**Não adicionar:**
- Atributos no enum (label, descrição, etc) — overengineering. Se um dia I18n precisar, vira propriedade externa.
- Método `getDescricao()` ou `from(String)` — não há caso de uso ainda.
- Javadoc.

### Tarefa 4 — Criar `Conta.java`

Criar `src/main/java/com/laboratorio/financas/conta/domain/Conta.java`.

**Especificação completa:**

```java
package com.laboratorio.financas.conta.domain;

import com.laboratorio.financas.shared.domain.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Conta {

    private static final int NOME_MAX_LENGTH = 100;

    private final UUID id;
    private final String nome;
    private final TipoConta tipo;
    private final Money saldoInicial;
    private final boolean ativa;
    private final Instant criadoEm;
    private final Instant atualizadoEm;

    /**
     * Construtor para criar nova Conta. Gera id, define ativa=true, criadoEm=atualizadoEm=now.
     */
    public Conta(String nome, TipoConta tipo, Money saldoInicial) {
        this(
            UUID.randomUUID(),
            nome,
            tipo,
            saldoInicial,
            true,
            Instant.now(),
            null  // atualizadoEm sera ajustado abaixo
        );
    }

    /**
     * Construtor de reconstrucao. Usado pelo repository para hidratar instancia persistida.
     * Todos os campos sao recebidos e validados.
     */
    public Conta(
        UUID id,
        String nome,
        TipoConta tipo,
        Money saldoInicial,
        boolean ativa,
        Instant criadoEm,
        Instant atualizadoEm
    ) {
        Objects.requireNonNull(id, "id nao pode ser nulo");
        Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
        Objects.requireNonNull(saldoInicial, "saldoInicial nao pode ser nulo");
        Objects.requireNonNull(criadoEm, "criadoEm nao pode ser nulo");
        validarNome(nome);

        this.id = id;
        this.nome = nome.trim();
        this.tipo = tipo;
        this.saldoInicial = saldoInicial;
        this.ativa = ativa;
        this.criadoEm = criadoEm;
        this.atualizadoEm = (atualizadoEm != null) ? atualizadoEm : criadoEm;
    }

    private static void validarNome(String nome) {
        Objects.requireNonNull(nome, "nome nao pode ser nulo");
        String trimmed = nome.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("nome nao pode ser vazio");
        }
        if (trimmed.length() > NOME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                "nome nao pode ter mais de " + NOME_MAX_LENGTH + " caracteres"
            );
        }
    }

    public Conta desativar() {
        if (!this.ativa) {
            return this;
        }
        return new Conta(
            this.id,
            this.nome,
            this.tipo,
            this.saldoInicial,
            false,
            this.criadoEm,
            Instant.now()
        );
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public TipoConta getTipo() { return tipo; }
    public Money getSaldoInicial() { return saldoInicial; }
    public boolean isAtiva() { return ativa; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Conta other)) return false;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Conta{id=" + id + ", nome='" + nome + "', tipo=" + tipo + ", ativa=" + ativa + "}";
    }
}
```

**Notas de implementação:**

- **Sem acentos no código** (comentários, mensagens de exceção). "nao", "reconstrucao".
- **Sem Lombok.** Domain explícito, conforme decisão.
- **Javadoc curto** apenas nos dois construtores explicando o uso. Demais membros sem Javadoc (regra do projeto).
- **Pattern matching no `equals`** (`o instanceof Conta other`) — Java 21, idiomático, sem cast manual.
- **Validação de nome aplicada nos dois construtores** via método estático `validarNome`. O construtor "novo" delega ao construtor "reconstrução", então a validação roda em todo caminho de criação.
- **`atualizadoEm` no construtor "novo"**: passa `null` ao construtor de reconstrução, que ajusta para `criadoEm`. Truque para evitar duplicar a chamada `Instant.now()` (que retornaria valores ligeiramente diferentes em invocações sucessivas). **Atenção:** o construtor de reconstrução **aceita `null`** em `atualizadoEm` mas **não** nos demais. Isso é intencional pra o construtor "novo" funcionar — comentar no código.
- **Encoding UTF-8 sem BOM.** Validar com `xxd` se houver dúvida.

**Casos limítrofes a NÃO criar:**
- Método `Conta.criar(...)` ou `Conta.reconstruir(...)` (factory methods estáticos) — construtores diretos bastam.
- Builder pattern — overengineering.
- `compareTo` ou `Comparable<Conta>` — sem caso de uso.
- Método `renomear(String novoNome)` — sem caso de uso na 3.2 (vai entrar quando UC `RenomearContaUseCase` aparecer, se aparecer).
- Atributo "categoria" ou "icone" ou "cor" — fora do escopo do MVP.

### Tarefa 5 — Criar `ContaTest.java`

Criar `src/test/java/com/laboratorio/financas/conta/domain/ContaTest.java`.

**Cenários obrigatórios:**

Construtor "novo" (`Conta(nome, tipo, saldoInicial)`):
1. Constrói com argumentos válidos: id é gerado (não-nulo), `ativa` é true, `criadoEm` está em janela razoável, `atualizadoEm` é igual a `criadoEm`
2. Constrói com nome com espaços nas pontas — nome é trimmed
3. Lança `NullPointerException` quando nome é null
4. Lança `IllegalArgumentException` quando nome é string vazia
5. Lança `IllegalArgumentException` quando nome é só espaços em branco
6. Lança `IllegalArgumentException` quando nome tem 101+ chars
7. Aceita nome com exatamente 100 chars
8. Aceita nome com 1 char
9. Lança `NullPointerException` quando tipo é null
10. Lança `NullPointerException` quando saldoInicial é null
11. Aceita saldoInicial positivo, zero e negativo
12. Dois `Conta` criados em sequência têm ids diferentes (sanity check de UUID.randomUUID)

Construtor de reconstrução (todos os campos):
13. Reconstrói com todos os campos válidos preservando os valores
14. Lança `NullPointerException` quando id é null
15. Lança `NullPointerException` quando criadoEm é null
16. Aceita `atualizadoEm` null — defaulta para `criadoEm`
17. Aceita `atualizadoEm` diferente de `criadoEm`
18. Reconstrução com `ativa=false` preserva o estado

`desativar()`:
19. Conta ativa → desativada: nova instância, mesmo id, `ativa=false`, `atualizadoEm` mais recente que `criadoEm`
20. Conta já desativada → retorna a mesma instância (referência igual, não cria nova)
21. Demais campos preservados após desativar (nome, tipo, saldoInicial, criadoEm)

Igualdade e hashCode (entidade — por id):
22. Duas `Conta` com mesmo id mas demais campos diferentes são iguais (`equals` true)
23. Duas `Conta` com ids diferentes não são iguais
24. `equals` retorna false comparando com null
25. `equals` retorna false comparando com objeto de outro tipo
26. `hashCode` é consistente com `equals` (mesmo id → mesmo hashCode)

`toString()`:
27. `toString` contém id, nome e tipo
28. `toString` não contém saldoInicial nem timestamps

**Total esperado: ~28 testes.** Pode haver mais se cenário óbvio aparecer durante a escrita. Não inflar artificialmente. Não cortar dos exigidos.

**Estrutura dos testes:**

- JUnit 5 + AssertJ. Sem Mockito.
- **Naming camelCase puro, sem underscore.** Regra do projeto, alinhada com `MoneyTest`. Exemplos:
  - `construtorNovoComArgumentosValidosCriaContaAtiva`
  - `construtorNovoComNomeNuloLancaNullPointerException`
  - `desativarContaAtivaRetornaNovaInstanciaInativa`
  - `desativarContaJaInativaRetornaMesmaInstancia`
- Comentários `// Given`, `// When`, `// Then` em testes não-triviais.
- Constantes no topo da classe pra reduzir verbosidade:
  ```java
  private static final Currency BRL = Currency.getInstance("BRL");
  private static final Money SALDO_ZERO = new Money(BigDecimal.ZERO, BRL);
  private static final Money SALDO_100 = new Money(new BigDecimal("100.00"), BRL);
  ```
- Para janela de tempo em `criadoEm`, usar AssertJ `isBetween(antes, depois)` ou `isCloseTo(now, withinSeconds(2))`. Tolerância: 5 segundos é seguro pra CI carregado.
- Para teste de "retorna mesma instância" em `desativar()` de conta inativa, usar `assertThat(resultado).isSameAs(contaJaInativa)` (compara referência).

### Tarefa 6 — Validar localmente

```bash
.\mvnw.cmd compile
.\mvnw.cmd test -Dtest=ContaTest
.\mvnw.cmd verify
```

**Esperado:**
- Compilação sem warnings novos (warning de `mapstruct.defaultComponentModel` continua — entra na 3.3)
- 28+ testes de Conta passando, mais os 40 de Money e os 2 de Healthcheck e o de Flyway (~71 testes total)
- Checkstyle: 0 violações
- SpotBugs: 0 issues
- JaCoCo: cobertura de `Conta` e `TipoConta` em 100% de instrução, regra `domain` 90% atendida (Money + Conta + TipoConta no pacote)
- BUILD SUCCESS

**Se algum teste falhar**, parar e reportar antes de prosseguir. Não tentar adivinhar correção em série.

**Se aparecer divergência entre `decisoes.md` e Checkstyle/testes existentes** (como aconteceu na 3.1 com naming), parar e reportar — não tomar decisão silenciosa, alinhar com o operador.

### Tarefa 7 — Atualizar `docs/decisoes.md`

**7a.** Localizar a seção "Padrões aplicados" da arquitetura. Adicionar nota curta sobre o padrão de igualdade em entidades (que estreia aqui):

```markdown
**Igualdade em entidades de domínio**: por `id`, não por valor. `equals` e `hashCode` implementados manualmente baseados apenas em `id`. Diferente de value objects (Money, etc) que usam record com igualdade estrutural. Estabelecido na Etapa 3.2 com `Conta`.
```

Inserir em local que faça sentido com o texto vizinho. Não duplicar.

**7b.** Adicionar entrada no **"Histórico de mudanças"** no fim do arquivo, no topo da lista:

```markdown
- **2026-05-09** — Etapa 3.2 concluída: bounded context `conta` — domain puro. Entidade `Conta` (class imutável com igualdade por id), enum `TipoConta`, validações de invariante via `IllegalArgumentException`. Saldo atual deliberadamente fora desta etapa (entrará quando `transacao` aparecer). Sem JPA, sem MapStruct, sem persistência — esses ficam para 3.3. Mergeado via PR #XX.
```

### Tarefa 8 — Atualizar `docs/progresso.md`

**8a.** Atualizar campo "Última atualização" no topo: `2026-05-09 (Etapa 3.2 — Conta domain)`.

**8b.** Na seção da Camada 2, marcar critério `Bounded context conta com domínio puro + use cases + repositório` como parcialmente atendido — adicionar nota inline tipo:
- `[~] Bounded context conta com domínio puro + use cases + repositório` (domain puro pronto na 3.2; infra/use cases/controllers nas 3.3 e 3.4)

Se o operador tiver formato preferido para "parcialmente atendido", o agente pode usar `[ ]` mantido com comentário, ou `[~]`, ou anotação explícita. Default: `[~]` com nota.

**8c.** Adicionar nova seção **"Lições da Etapa 3.2"** logo antes de **"Lições da Etapa 3.1"** (ordem decrescente):

```markdown
## Lições da Etapa 3.2

### Candidatos a hook (automatizar em etapas futuras)

(Preencher com lições reais — ou `(Nenhum novo nesta etapa.)` se nada surgir.)

### Lições de ambiente

(Preencher com lições reais — ou `(Nenhum novo nesta etapa.)` se nada surgir.)
```

**Regra dura:** só registrar lições **realmente observadas**. Não inventar.

**8d.** Adicionar entrada no **"Histórico de mudanças deste documento"** no topo da lista:

```markdown
- **2026-05-09** — Etapa 3.2 concluída: domain puro de `conta` (entidade `Conta`, enum `TipoConta`, ~28 testes). Mergeado via PR #XX.
```

### Tarefa 9 — Versionar este próprio prompt

Confirmar que `docs/prompt-etapa-3-2.md` está em disco como untracked e incluir no commit de docs (Commit 3).

### Tarefa 10 — Validação final antes de commitar

```bash
xxd src/main/java/com/laboratorio/financas/conta/domain/Conta.java | head -1
xxd src/main/java/com/laboratorio/financas/conta/domain/TipoConta.java | head -1
xxd src/test/java/com/laboratorio/financas/conta/domain/ContaTest.java | head -1
# (Esperado: nenhum começa com EF BB BF)

.\mvnw.cmd verify

git status
```

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Arquivos permitidos:
   - `src/main/java/com/laboratorio/financas/conta/domain/Conta.java` (criação)
   - `src/main/java/com/laboratorio/financas/conta/domain/TipoConta.java` (criação)
   - `src/test/java/com/laboratorio/financas/conta/domain/ContaTest.java` (criação)
   - `docs/decisoes.md`
   - `docs/progresso.md`
   - `docs/prompt-etapa-3-2.md` (este arquivo, versionar)

2. **Não tocar em `pom.xml`.** Threshold JaCoCo de `domain/` já está ativo. Não há mudança de build.

3. **Não criar `ContaEntity` (JPA), `ContaRepository`, `ContaMapper` (MapStruct), `ContaController`, `ContaService`, use case, DTO `*Request`/`*Response`.** Tudo entra nas Etapas 3.3 e 3.4.

4. **Não criar migration Flyway.** `V2__cria_tabela_conta.sql` entra na 3.3.

5. **Não criar `categoria/` ou `transacao/` ou qualquer outro bounded context.** Esta etapa é exclusivamente domain de `conta`.

6. **Não usar Lombok no domain.** Class explícita com getters manuais. Lombok entra em `infrastructure/` na 3.3.

7. **Não usar Spring no domain.** Zero `@Component`, `@Service`, `@Configuration`, `@Autowired`. Domain é puro.

8. **Não criar exceção customizada `NomeContaInvalidoException` ou similar.** `IllegalArgumentException` e `NullPointerException` do JDK bastam, consistente com Money. Exceções customizadas entram quando regra de negócio multipla justificar.

9. **Não criar factory method estático (`Conta.criar(...)`, `Conta.reconstruir(...)`).** Construtores diretos bastam.

10. **Não criar Builder pattern.** Construtores com 7 parâmetros são aceitáveis nesta escala.

11. **Não criar `compareTo`/`Comparable<Conta>`.** Sem caso de uso.

12. **Não criar método `renomear`, `alterarTipo`, `reativar`.** Sem caso de uso na 3.2.

13. **Não introduzir `Clock` injetável.** `Instant.now()` direto.

14. **Não relaxar threshold de Checkstyle nem desabilitar regras.** Se Checkstyle reclamar, ajustar o código pra conformar. **Único caso aceitável de relaxar:** se Checkstyle reclamar de algo que reflita divergência entre `decisoes.md` e a config (como na 3.1 com underscore), parar e reportar — não decidir silenciosamente.

15. **Não relaxar threshold JaCoCo.** `Conta` e `TipoConta` devem atingir 100%. Se algum branch ficar descoberto, adicionar teste.

16. **Sem acentos no código Java** (mensagens, comentários). Padrão do projeto.

17. **Encoding UTF-8 sem BOM** nos `.java` criados.

18. **Naming de método de teste em camelCase puro, sem underscore.** Regra confirmada na 3.1, alinhada com Checkstyle.

19. **Lições da Etapa 3.2 só registram observações reais.** Não inventar.

20. **Não antecipar Etapa 3.3.** Sem rascunhar próximas etapas. Sem criar `infrastructure/` vazia "preparando o terreno". Sem importar `Conta` em outro pacote além do próprio teste. A 3.3 abre em discussão separada.

21. **Não tomar decisão silenciosa em zona limítrofe.** Se aparecer dúvida fora do escopo prescrito, parar e reportar — mesmo que solução pareça óbvia. Padrão consolidado desde a Camada 1, reforçado na 3.1 com naming de testes.

## Estrutura de commits

Branch: `feat/conta-domain`

Commits atômicos, em ordem:

**Commit 1** — `feat(conta): adiciona enum TipoConta`
- `src/main/java/com/laboratorio/financas/conta/domain/TipoConta.java`

**Commit 2** — `feat(conta): adiciona entidade de dominio Conta com igualdade por id`
- `src/main/java/com/laboratorio/financas/conta/domain/Conta.java`

**Commit 3** — `test(conta): cobertura completa de Conta (~28 testes)`
- `src/test/java/com/laboratorio/financas/conta/domain/ContaTest.java`

**Commit 4** — `docs: registra etapa 3.2 (conta domain) em decisoes e progresso`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-3-2.md`

## Validação antes de abrir PR

```bash
.\mvnw.cmd verify

git status
git log --oneline -6
```

Esperado:
- BUILD SUCCESS
- ~71 testes passando (40 Money + ~28 Conta + Healthcheck + Flyway)
- JaCoCo `domain` 90% atendido (Conta e TipoConta em 100%)
- JaCoCo BUNDLE 75% atendido
- JaCoCo `infrastructure` 60% atendido
- Working tree limpo
- 4 commits na branch `feat/conta-domain`

## PR

Título: `feat: etapa 3.2 — bounded context conta (domain puro)`

Body sugerido (ajustar com observações reais):

```markdown
## Summary

Implementa a Etapa 3.2 do roadmap (segunda etapa da Camada 2): primeiro bounded context de domínio do projeto. Apenas a camada `domain/` — entidade `Conta` (class imutável com igualdade por id), enum `TipoConta`, validações de invariante. Sem JPA, sem MapStruct, sem persistência (Etapa 3.3).

### Mudanças

- `src/main/java/.../conta/domain/Conta.java`: class imutável final com 7 campos (id, nome, tipo, saldoInicial, ativa, criadoEm, atualizadoEm). Dois construtores (novo + reconstrução). Método `desativar()` retorna nova instância. `equals`/`hashCode` por id. `toString` enxuto.
- `src/main/java/.../conta/domain/TipoConta.java`: enum simples com 4 valores (CORRENTE, POUPANCA, DINHEIRO, CARTAO_CREDITO).
- `src/test/java/.../conta/domain/ContaTest.java`: <N> testes cobrindo construtores, validações, `desativar()`, igualdade, toString.
- `docs/decisoes.md`: nota sobre padrão "igualdade em entidades por id" (estreia nesta etapa) + entrada no histórico.
- `docs/progresso.md`: critério parcial marcado, lições registradas, histórico atualizado.

### Decisões de escopo

- **Saldo atual deliberadamente fora desta etapa.** Saldo é derivado (`saldoInicial + soma(transacoes)`) e só faz sentido quando `transacao` existir (Etapa 3.5+). ADR-004 já previa: agregado raiz com invariantes "entrará em `Conta` quando saldo passar a ser derivado e exigir consistência forte".
- **Class imutável, não record.** 7 campos com validação coordenada + 2 mutáveis via `desativar()` justificam class final sobre record.
- **Sem dono/usuarioId.** MVP single-user. Multi-user é decisão futura.
- **Soft delete via `desativar()`.** Não há `reativar()` no MVP — porta aberta.
- **Igualdade por id** estabelece o padrão para todas as entidades futuras (Categoria, Transacao). Diferente de Money/VOs que usam igualdade estrutural via record.
- **`UUID.randomUUID()` direto, sem `Clock` injetável.** Simples e suficiente para esta escala.
- **`IllegalArgumentException`/`NullPointerException` do JDK** para validações, consistente com Money. Sem exceções customizadas.

### Validação

- `mvnw verify` local: PASSOU
- Cobertura de `Conta` e `TipoConta`: 100%
- JaCoCo `domain` 90%: atendido
- Checkstyle: 0 violações
- SpotBugs: 0 issues

### Próximo passo

Etapa 3.3 (infra de `conta` — JPA + MapStruct + repository + migration Flyway) — fora do escopo deste PR. Abre em discussão separada.
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

- Branch `feat/conta-domain` empurrada para origin com 5 commits (4 + 1 de update do número do PR)
- PR aberto, CI verde, **não mergeado**
- `main` local ainda aponta pro squash da 3.1 (operador faz merge depois)
- Working tree limpo
- `Conta.java`, `TipoConta.java` em `conta/domain/`
- `ContaTest.java` com ~28 testes passando
- `decisoes.md` e `progresso.md` atualizados, com PR número real registrado
- Prompt versionado em `docs/prompt-etapa-3-2.md`

Reportar ao operador o estado final com `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup` e parar.

## O que NÃO fazer ao terminar

- Não mergear o PR. Aguardar autorização explícita do operador.
- Não criar prompt da próxima etapa.
- Não rascunhar Etapa 3.3.
- Não criar `infrastructure/`, `application/`, `interfaces/` em `conta/` "preparando o terreno".
- Não criar `categoria/` ou `transacao/`.
- Não tocar em `frontend/`, `scripts/`, `docker-compose.yml`, `application.yml`, `pom.xml`.
- Não sugerir "próximo passo" espontaneamente.
- Não atualizar `progresso.md` marcando algo da Etapa 3.3 — esta etapa é exclusivamente domain de `conta`.
