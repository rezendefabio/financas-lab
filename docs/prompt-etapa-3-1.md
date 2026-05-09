# Prompt — Etapa 3.1: Value Object `Money` em `shared/domain`

## Contexto

A Etapa 2.9 foi concluída e fechada via PR #28 (fix do `.env` em `setup.ps1` e `dev.ps1`). Camada 1 está ✅ Concluída com débito técnico resolvido. `main` está em `80e7914`, working tree limpo.

Esta é a **primeira etapa da Camada 2** (Arquitetura otimizada para agentes). Implementa o value object `Money` em `shared/domain` — peça compartilhada que será usada por todos os bounded contexts que lidam com valores monetários (`conta`, `transacao`, `categoria` quando ela armazenar limites).

Money é o pedaço mais simples de domínio puro do projeto: zero Spring, zero JPA, zero MapStruct, zero dependência cruzada. Por isso é o ponto de partida ideal da Camada 2 — valida o padrão "domínio não conhece framework" no caso mais simples antes de aplicar em algo grande (bounded context completo de `conta` na 3.2).

Esta etapa também **ativa o threshold JaCoCo de 90% em `domain/`** (descomentando a regra que ficou pendente desde a Etapa 2.4 aguardando primeira classe no pacote).

A regra dura #6 da arquitetura (`decisoes.md`):

> **Money desde o dia 1.** Toda quantia monetária é `Money`, nunca `BigDecimal` cru, nunca `double`, nunca `float`.

Esta etapa cumpre essa regra estabelecendo o tipo. A aplicação em entidades vem nas etapas seguintes da Camada 2.

## Escopo decidido (calibrado com operador antes da redação)

- **Money mínimo, não completo.** Para o MVP single-user em BRL sem investimento, o mínimo basta. Métodos: construtor validador, `somar`, `subtrair`, `multiplicar(BigDecimal)`, `negar`, `ehZero`, `ehNegativo`, `ehPositivo`. Comparação por valor (record já entrega). Sem `dividir`, sem `percentual`, sem formatação localizada, sem conversão entre moedas, sem `Comparable`. Expansão depois é trivial (record imutável, métodos novos aditivos).
- **Escala 2 fixa + `RoundingMode.HALF_EVEN`** (banker's rounding, padrão contábil). Normalização aplicada no construtor — qualquer `BigDecimal` que entre com escala diferente é convertido. Isso evita o bug clássico onde `new Money(BigDecimal("10.005"))` se comporta diferente dependendo de quem operou primeiro.
- **Moeda inicial: BRL apenas.** O construtor aceita `Currency` (do JDK), validando que não é null. Sem restrição artificial a uma moeda só — passar `Currency.getInstance("USD")` deve funcionar. Mas o MVP usa BRL e os testes refletem isso. Multi-moeda real (com taxa de câmbio) é decisão futura quando o escopo do produto pedir.
- **Operações entre `Money` exigem mesma moeda.** `Money(10, BRL).somar(Money(5, USD))` lança `IllegalArgumentException`. Mensagem clara mencionando as duas moedas.
- **Imutabilidade real, não só sintática.** Como `BigDecimal` é imutável e `Currency` é singleton do JDK, o record `Money` é genuinamente imutável.
- **Construtor compacto do record para validação.** Padrão idiomático Java 21:
  ```java
  public record Money(BigDecimal valor, Currency moeda) {
      public Money {
          // validações + normalização
      }
  }
  ```
- **Exceção tipada do projeto, não `RuntimeException` genérica.** `decisoes.md` regra: "Exceções tipadas por contexto. Sem `RuntimeException` genérica." Para validações de Money, usar `IllegalArgumentException` (do JDK) é aceitável para casos de input inválido — é a exceção idiomática Java para "argumento inválido". Não criar exceção customizada `MoneyException` nesta etapa — overengineering. Se aparecer regra de negócio que justifique, entra depois.
- **Localização do código:** `src/main/java/com/laboratorio/financas/shared/domain/Money.java`. Pacote `shared/domain` é o que `decisoes.md` prescreve para VOs compartilhados.
- **Localização dos testes:** `src/test/java/com/laboratorio/financas/shared/domain/MoneyTest.java`. Sufixo `Test` (singular) — padrão do projeto registrado em `decisoes.md`.
- **JaCoCo: ativar threshold de 90% em `domain/`.** Hoje a regra está comentada no `pom.xml` aguardando primeira classe no pacote. Esta etapa é a primeira classe — descomentar.
- **Cobertura esperada de Money:** 100% de instrução. Record com construtor compacto + 7 métodos pequenos é trivial cobrir totalmente. Threshold de 90% dá margem mas a meta real é 100%.
- **Sem `@Entity`, sem MapStruct, sem persistência.** Money é puro. Persistência (mapeamento JPA `BigDecimal` ↔ coluna `numeric(19,2)`) entra na Etapa 3.2 quando aparecer a primeira entidade que usa Money.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra commit do squash da Etapa 2.9 com referência a PR #28 (commit `80e7914`)
- `docs/prompt-etapa-3-1.md` presente como untracked (este próprio arquivo, anexado pelo operador antes de iniciar). Se não aparecer em `git status`, parar e reportar.
- Working tree sem outras mudanças
- Pacote `com.laboratorio.financas.shared.domain` **não existe ainda**. Esta etapa cria.
- `src/main/java/com/laboratorio/financas/shared/` pode existir parcialmente (de etapas anteriores que tocaram em `shared/infrastructure/`, ex: `SecurityConfig`). Confirmar.

Validar com:

```bash
git status
git log --oneline -1
ls -la src/main/java/com/laboratorio/financas/shared/ 2>/dev/null
ls docs/prompt-etapa-3-1.md
```

Se algum dos dois primeiros itens divergir, parar e reportar.

## Tarefas

### Tarefa 1 — Validar pré-requisitos

```bash
git status
git log --oneline -3
ls docs/prompt-etapa-3-1.md
ls -la src/main/java/com/laboratorio/financas/shared/ 2>/dev/null || echo "shared/ ainda nao existe (esperado parcialmente)"
ls -la src/main/java/com/laboratorio/financas/shared/domain/ 2>/dev/null && echo "ATENCAO: shared/domain ja existe" || echo "OK: shared/domain ainda nao existe"
```

Esperado:
- Working tree limpo, exceto `docs/prompt-etapa-3-1.md` untracked
- Último commit em main: `80e7914 fix: etapa 2.9 — setup.ps1 e dev.ps1 criam .env automaticamente quando ausente (#28)`
- `shared/` pode ter `infrastructure/web/SecurityConfig.java` da Etapa 2.3 — OK
- `shared/domain/` **não deve existir** — se existir, parar e reportar

Se qualquer item divergir, parar e reportar.

### Tarefa 2 — Criar branch de trabalho

```bash
git checkout -b feat/money-vo
```

### Tarefa 3 — Criar `Money.java`

Criar `src/main/java/com/laboratorio/financas/shared/domain/Money.java`.

**Especificação completa do tipo:**

```java
package com.laboratorio.financas.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal valor, Currency moeda) {

    private static final int ESCALA = 2;
    private static final RoundingMode MODO_ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    public Money {
        Objects.requireNonNull(valor, "valor nao pode ser nulo");
        Objects.requireNonNull(moeda, "moeda nao pode ser nula");
        valor = valor.setScale(ESCALA, MODO_ARREDONDAMENTO);
    }

    public Money somar(Money outro) {
        validarMesmaMoeda(outro);
        return new Money(this.valor.add(outro.valor), this.moeda);
    }

    public Money subtrair(Money outro) {
        validarMesmaMoeda(outro);
        return new Money(this.valor.subtract(outro.valor), this.moeda);
    }

    public Money multiplicar(BigDecimal fator) {
        Objects.requireNonNull(fator, "fator nao pode ser nulo");
        return new Money(this.valor.multiply(fator), this.moeda);
    }

    public Money negar() {
        return new Money(this.valor.negate(), this.moeda);
    }

    public boolean ehZero() {
        return this.valor.signum() == 0;
    }

    public boolean ehNegativo() {
        return this.valor.signum() < 0;
    }

    public boolean ehPositivo() {
        return this.valor.signum() > 0;
    }

    private void validarMesmaMoeda(Money outro) {
        Objects.requireNonNull(outro, "outro Money nao pode ser nulo");
        if (!this.moeda.equals(outro.moeda)) {
            throw new IllegalArgumentException(
                "Moedas diferentes: " + this.moeda.getCurrencyCode()
                + " e " + outro.moeda.getCurrencyCode()
            );
        }
    }
}
```

**Notas de implementação:**

- **Sem acentos no código** (mensagens de exceção, comentários). Padrão do projeto. "nao pode ser nulo", não "não pode ser nulo".
- **Sem Lombok.** Record já dá tudo que Lombok daria (getters, equals, hashCode, toString) e sem anotação. `decisoes.md` recomenda `record` para VOs.
- **Sem Javadoc.** Convenção do projeto: Javadoc não obrigatório nesta fase (regra do `decisoes.md` na seção "Análise estática"). Código autoexplicativo basta.
- **Encoding UTF-8 sem BOM** (validar com `xxd` se houver dúvida).
- **Não adicionar métodos além dos especificados.** Se durante a implementação surgir tentação de adicionar `dividir`, `formatar`, `compareTo`, etc., parar e reportar — não tomar decisão silenciosa.

### Tarefa 4 — Criar `MoneyTest.java`

Criar `src/test/java/com/laboratorio/financas/shared/domain/MoneyTest.java`.

**Cobertura mínima exigida:**

Construtor:
1. Constrói com valor e moeda válidos
2. Lança `NullPointerException` quando valor é null
3. Lança `NullPointerException` quando moeda é null
4. Normaliza escala 0 para 2 (`new Money(new BigDecimal("10"), BRL)` → valor `10.00`)
5. Normaliza escala 4 para 2 com HALF_EVEN (`new Money(new BigDecimal("10.005"), BRL)` → valor `10.00`, banker's rounding arredonda 5 pra par mais próximo)
6. Normaliza escala 4 para 2 com HALF_EVEN — caso impar (`new Money(new BigDecimal("10.015"), BRL)` → valor `10.02`, banker's rounding arredonda 5 pra par mais próximo)
7. Aceita valor negativo (`new Money(new BigDecimal("-50.00"), BRL)`)
8. Aceita valor zero

`somar`:
9. Soma dois valores positivos da mesma moeda
10. Soma valor positivo com negativo (resultado positivo)
11. Soma valor positivo com negativo (resultado negativo)
12. Soma com zero retorna o mesmo valor
13. Lança `IllegalArgumentException` quando moedas diferentes (BRL + USD)
14. Lança `NullPointerException` quando outro é null

`subtrair`:
15. Subtrai dois valores positivos
16. Subtrai resultando em negativo
17. Subtrai zero retorna o mesmo valor
18. Lança `IllegalArgumentException` quando moedas diferentes

`multiplicar(BigDecimal)`:
19. Multiplica por inteiro positivo
20. Multiplica por fração (ex: `0.10` para calcular 10%)
21. Multiplica por zero retorna zero
22. Multiplica por negativo inverte sinal
23. Multiplicação que gera escala maior é normalizada (ex: `Money(10.00).multiplicar(0.333)` → `3.33`, não `3.330`)
24. Lança `NullPointerException` quando fator é null

`negar`:
25. Nega positivo retorna negativo
26. Nega negativo retorna positivo
27. Nega zero retorna zero (sinal pode variar mas `ehZero` continua true)

`ehZero`, `ehNegativo`, `ehPositivo`:
28. `ehZero` true para `0.00`
29. `ehZero` false para `0.01` e `-0.01`
30. `ehNegativo` true para `-1.00`, false para `0.00` e `1.00`
31. `ehPositivo` true para `1.00`, false para `0.00` e `-1.00`

Igualdade (record):
32. Dois `Money` com mesmo valor e moeda são iguais (`.equals` true, mesmo `hashCode`)
33. Dois `Money` com valores diferentes não são iguais
34. Dois `Money` com moedas diferentes não são iguais
35. `Money(10, BRL)` é igual a `Money(10.00, BRL)` (normalização de escala faz `BigDecimal("10")` virar `BigDecimal("10.00")`, e `BigDecimal.equals` é por valor escalado — então pós-normalização, ambos têm escala 2 e são iguais)

**Estrutura dos testes:**

- JUnit 5 + AssertJ. Sem Mockito (não há dependência a mockar).
- Nomes de método: `metodoTestado_cenarioDoTeste_resultadoEsperado` (regra `decisoes.md`).
- Comentários `// Given`, `// When`, `// Then` em testes não-triviais (AAA explícito, regra `decisoes.md`).
- Constante `private static final Currency BRL = Currency.getInstance("BRL");` no topo da classe pra reduzir verbosidade.
- Constante `private static final Currency USD = Currency.getInstance("USD");` para os testes de moeda diferente.
- Usar `BigDecimal` literais com string (`new BigDecimal("10.00")`), nunca `new BigDecimal(10.00)` (double impreciso).
- Testes de `NullPointerException` e `IllegalArgumentException`: usar `assertThatThrownBy(...).isInstanceOf(...).hasMessageContaining(...)` do AssertJ.

**Esboço de exemplo (apenas pra referência de estilo, não copiar literalmente):**

```java
@Test
void somar_doisValoresPositivosMesmaMoeda_retornaSoma() {
    // Given
    Money a = new Money(new BigDecimal("10.00"), BRL);
    Money b = new Money(new BigDecimal("5.50"), BRL);

    // When
    Money resultado = a.somar(b);

    // Then
    assertThat(resultado.valor()).isEqualByComparingTo("15.50");
    assertThat(resultado.moeda()).isEqualTo(BRL);
}

@Test
void somar_moedasDiferentes_lancaIllegalArgumentException() {
    // Given
    Money brl = new Money(new BigDecimal("10.00"), BRL);
    Money usd = new Money(new BigDecimal("10.00"), USD);

    // When / Then
    assertThatThrownBy(() -> brl.somar(usd))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("BRL")
        .hasMessageContaining("USD");
}
```

**Total esperado: ~35 testes.** Pode haver mais se durante a escrita aparecer caso óbvio que valha a pena cobrir. Não inflar artificialmente. Não cortar dos exigidos.

### Tarefa 5 — Ativar threshold JaCoCo de 90% em `domain/`

Editar `pom.xml`. Localizar a configuração do `jacoco-maven-plugin`, especificamente a `execution` de `id` igual a `check` (ou similar — a que define as `<rule>`s de cobertura).

A regra para `domain/` foi deixada **comentada** na Etapa 2.4 aguardando primeira classe no pacote. Esta etapa é a primeira — descomentar.

A regra esperada é algo como:

```xml
<rule>
    <element>PACKAGE</element>
    <includes>
        <include>**.domain.*</include>
    </includes>
    <limits>
        <limit>
            <counter>INSTRUCTION</counter>
            <value>COVEREDRATIO</value>
            <minimum>0.90</minimum>
        </limit>
    </limits>
</rule>
```

**Importante:** a regra exata pode estar comentada com sintaxe ligeiramente diferente. Antes de descomentar, **ler o bloco comentado existente** e descomentar exatamente o que está lá — não reescrever. Se o bloco comentado divergir significativamente do esperado, parar e reportar.

**Não tocar** nas regras de `application/`, `interfaces/` (que continuam comentadas aguardando suas primeiras classes — Etapa 3.2 e 3.3), nem nas regras ativas de BUNDLE 75% e `infrastructure/` 60%.

### Tarefa 6 — Validar localmente

```bash
# Compilação:
.\mvnw.cmd compile

# Testes (apenas Money, rápido):
.\mvnw.cmd test -Dtest=MoneyTest

# Gate completo (mesmo que CI roda):
.\mvnw.cmd verify
```

**Esperado:**
- Compilação sem warnings novos (warning de `mapstruct.defaultComponentModel` continua existindo — não é desta etapa)
- 35+ testes de Money passando
- Checkstyle: 0 violações
- SpotBugs: 0 issues
- JaCoCo: cobertura de Money em 100% de instrução, regra `domain` 90% atendida, regras BUNDLE 75% e `infrastructure` 60% continuam atendidas
- BUILD SUCCESS

**Se algum teste falhar**, parar e reportar antes de prosseguir. Não tentar adivinhar correção em série.

**Se Checkstyle reclamar de algum estilo (linha > 140 chars, indentação errada, etc.)**, corrigir e re-rodar. Se não conseguir resolver com ajuste trivial, parar e reportar — não desativar regra do Checkstyle.

**Se JaCoCo `check` falhar dizendo que `domain` está abaixo de 90%**, investigar: provavelmente algum branch do construtor compacto não está coberto. Adicionar teste, não relaxar threshold.

### Tarefa 7 — Atualizar `docs/decisoes.md`

**7a.** Localizar a seção "Padrões aplicados" da arquitetura (regra dura #6 já menciona Money). Adicionar nota curta após a regra dura #6 ou em parágrafo adjacente:

```markdown
**Implementação de `Money`** (a partir da Etapa 3.1): record imutável em `shared/domain` com `BigDecimal valor` (escala 2 fixa, `RoundingMode.HALF_EVEN`) e `Currency moeda`. Operações: `somar`, `subtrair`, `multiplicar(BigDecimal)`, `negar`, `ehZero`, `ehNegativo`, `ehPositivo`. Operações entre `Money` exigem mesma moeda — moedas diferentes lançam `IllegalArgumentException`. Métodos adiados (porta aberta): `dividir`, `percentual`, formatação localizada, `Comparable`, conversão entre moedas.
```

A redação acima é sugestão — ajustar para fluir com o texto vizinho da seção. Não duplicar conteúdo já presente.

**7b.** Localizar a seção "Cobertura mínima por camada (JaCoCo)" — especificamente o "Status atual de aplicação dos thresholds". Atualizar:

- **Antes:** `domain` 90% listado como ⏸️ "Aguardando classes (ativados na Camada 2)".
- **Depois:** `domain` 90% movido para ✅ "Ativos". Manter `application` 80% e `interfaces` 70% como aguardando.
- Atualizar o número da etapa de referência: "(Etapa 2.4)" → "(Etapas 2.4 e 3.1)".

**7c.** Adicionar entrada no **"Histórico de mudanças"** no fim do arquivo, no topo da lista:

```markdown
- **2026-05-09** — Etapa 3.1 concluída: value object `Money` implementado em `shared/domain` (record imutável, escala 2, HALF_EVEN). Threshold JaCoCo `domain` 90% ativado (descomentado no pom.xml). Mergeado via PR #XX.
```

(O `#XX` será substituído pelo número real do PR na fase de pós-criação.)

### Tarefa 8 — Atualizar `docs/progresso.md`

**8a.** Atualizar campo "Última atualização" no topo: `2026-05-09 (Etapa 3.1 — Money)`.

**8b.** Atualizar status da Camada 2 de ⏸️ "Aguardando" para 🟢 "Em andamento".

**8c.** Na seção da Camada 2, marcar o critério `Value object Money implementado e testado` como `[x]`.

**8d.** Adicionar nova seção **"Lições da Etapa 3.1"** logo antes de **"Lições da Etapa 2.9"** (ordem decrescente):

```markdown
## Lições da Etapa 3.1

### Candidatos a hook (automatizar em etapas futuras)

(Preencher com lições reais — ou `(Nenhum novo nesta etapa.)` se nada surgir.)

### Lições de ambiente

(Preencher com lições reais — ou `(Nenhum novo nesta etapa.)` se nada surgir.)
```

**Regra dura:** só registrar lições **realmente observadas**. Não inventar.

**8e.** Adicionar entrada no **"Histórico de mudanças deste documento"** no topo da lista:

```markdown
- **2026-05-09** — Etapa 3.1 concluída: `Money` implementado em `shared/domain`, threshold JaCoCo `domain` 90% ativado. Camada 2 marcada como 🟢 Em andamento. Mergeado via PR #XX.
```

### Tarefa 9 — Versionar este próprio prompt

Confirmar que `docs/prompt-etapa-3-1.md` está em disco como untracked e incluir no commit de docs (Commit 3).

### Tarefa 10 — Validação final antes de commitar

```bash
# Encoding sem BOM no Money.java e MoneyTest.java:
xxd src/main/java/com/laboratorio/financas/shared/domain/Money.java | head -1
xxd src/test/java/com/laboratorio/financas/shared/domain/MoneyTest.java | head -1
# (Esperado: nenhum começa com EF BB BF)

# Build local final:
.\mvnw.cmd verify

# Working tree esperado:
git status
```

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Arquivos permitidos:
   - `src/main/java/com/laboratorio/financas/shared/domain/Money.java` (criação)
   - `src/test/java/com/laboratorio/financas/shared/domain/MoneyTest.java` (criação)
   - `pom.xml` (apenas descomentar regra JaCoCo de `domain`)
   - `docs/decisoes.md`
   - `docs/progresso.md`
   - `docs/prompt-etapa-3-1.md` (este arquivo, versionar)

2. **Não tocar em `application.yml`, `docker-compose.yml`, `scripts/*.ps1`, `frontend/...`, `.github/...`, `Dockerfile`.** Esta etapa é Java domain puro + docs + JaCoCo.

3. **Não criar `@Entity` JPA.** Money é VO de domínio puro. Persistência entra na 3.2.

4. **Não criar `@Component`, `@Service`, `@Repository`, `@Configuration`.** Domain puro, zero Spring.

5. **Não usar Lombok.** Record entrega tudo que Money precisa.

6. **Não adicionar métodos além dos especificados.** Sem `dividir`, `percentual`, `formatar`, `compareTo`, `static factory methods` (`Money.of(...)`, `Money.zero(...)`). Se durante a implementação aparecer pressão pra incluir, parar e reportar.

7. **Não criar exceção customizada `MoneyException`** ou similar. `IllegalArgumentException` e `NullPointerException` do JDK bastam pra esta etapa. Exceções customizadas entram quando regra de negócio justificar (provavelmente em `conta` ou `transacao`).

8. **Não relaxar threshold de Checkstyle nem desabilitar regras.** Se Checkstyle reclamar, ajustar o código pra conformar. Único caso de adicionar supressão: regra que falsa-positivamente flagra padrão idiomático Java 21 (record, etc) — nesse caso, parar e reportar antes de suprimir.

9. **Não relaxar threshold JaCoCo.** Money deve atingir 100% de instrução. Se algum branch ficar descoberto, adicionar teste — não baixar threshold.

10. **Não tocar nas regras JaCoCo de `application/` e `interfaces/`.** Continuam comentadas aguardando suas etapas (3.2 e seguintes).

11. **Sem acentos no código Java** (mensagens de exceção, comentários). Padrão do projeto.

12. **Encoding UTF-8 sem BOM** nos `.java` criados. Validar com `xxd` se houver dúvida.

13. **`bash_tool` é bash, não PowerShell.** Para invocar Maven Wrapper, usar `.\mvnw.cmd` via `cmd.exe` se necessário, ou `./mvnw` se o ambiente do agente reconhecer (geralmente reconhece em Linux/WSL; em Windows nativo no terminal do agente, `.\mvnw.cmd`). Lição registrada desde 2.5.

14. **Lições da Etapa 3.1 só registram observações reais.** Se Tarefa 8d ficar com `(Nenhum novo nesta etapa.)`, tudo bem. Não inventar.

15. **Não antecipar Etapa 3.2 (`conta`).** Sem rascunhar próximas etapas. Sem criar pacote `conta/` vazio "preparando o terreno". Sem importar Money em lugar nenhum além dos próprios testes. A 3.2 abre em discussão separada.

16. **Não tomar decisão silenciosa em zona limítrofe.** Padrão recorrente registrado na retrospectiva da Camada 1. Se aparecer dúvida fora do escopo prescrito, parar e reportar — mesmo que solução pareça óbvia.

## Estrutura de commits

Branch: `feat/money-vo`

Commits atômicos, em ordem:

**Commit 1** — `feat(shared): adiciona value object Money em shared/domain`
- `src/main/java/com/laboratorio/financas/shared/domain/Money.java`

**Commit 2** — `test(shared): cobertura completa de Money (35 testes)`
- `src/test/java/com/laboratorio/financas/shared/domain/MoneyTest.java`

**Commit 3** — `build: ativa threshold JaCoCo de 90% em domain`
- `pom.xml` (apenas descomentar regra `domain`)

**Commit 4** — `docs: registra etapa 3.1 (Money) em decisoes e progresso`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-3-1.md`

## Validação antes de abrir PR

```bash
# Build completo (espelho do CI):
.\mvnw.cmd verify

# Working tree limpo após os 4 commits:
git status
git log --oneline -5
```

Esperado:
- BUILD SUCCESS
- 35+ testes Money passando
- JaCoCo `domain` 90%+ atendido (Money deve estar 100%)
- JaCoCo BUNDLE 75% atendido
- JaCoCo `infrastructure` 60% atendido
- Working tree limpo
- 4 commits na branch `feat/money-vo`

## PR

Título: `feat: etapa 3.1 — value object Money em shared/domain`

Body sugerido (ajustar com observações reais):

```markdown
## Summary

Implementa a Etapa 3.1 do roadmap (primeira etapa da Camada 2): value object `Money` em `shared/domain`. Record imutável com `BigDecimal valor` (escala 2 fixa, `RoundingMode.HALF_EVEN`) e `Currency moeda`. Sete operações essenciais. Cobertura 100%. Threshold JaCoCo `domain` 90% ativado.

### Mudanças

- `src/main/java/.../shared/domain/Money.java`: record com construtor compacto (validação + normalização de escala) e operações `somar`, `subtrair`, `multiplicar(BigDecimal)`, `negar`, `ehZero`, `ehNegativo`, `ehPositivo`.
- `src/test/java/.../shared/domain/MoneyTest.java`: <N> testes cobrindo construtor (incluindo banker's rounding em `10.005` → `10.00` e `10.015` → `10.02`), operações, validação de moeda, igualdade de record.
- `pom.xml`: regra JaCoCo de `**.domain.*` 90% descomentada.
- `docs/decisoes.md`: nota sobre implementação de Money + status JaCoCo atualizado + entrada no histórico.
- `docs/progresso.md`: critério "Money implementado" marcado, Camada 2 em andamento, lições registradas.

### Decisões de escopo

- **Money mínimo, não completo.** Sem `dividir`, sem `percentual`, sem formatação, sem `Comparable`, sem conversão entre moedas. Expansão futura é aditiva.
- **Escala 2 fixa + HALF_EVEN** (banker's rounding, padrão contábil). Normalização no construtor evita bugs de input com escala variável.
- **Operações entre `Money` exigem mesma moeda.** `IllegalArgumentException` em moedas diferentes, mensagem clara mencionando ambas.
- **Sem exceção customizada.** `IllegalArgumentException` e `NullPointerException` do JDK são idiomáticas pra validação de input em VO.

### Validação

- `mvnw verify` local: PASSOU
- Cobertura de Money: 100% (verificável em `target/site/jacoco/index.html`)
- JaCoCo `domain` 90%: atendido
- Checkstyle: 0 violações
- SpotBugs: 0 issues

### Próximo passo

Etapa 3.2 (bounded context `conta` — domain) — fora do escopo deste PR. Abre em discussão separada.
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

- Branch `feat/money-vo` empurrada para origin com 5 commits (4 + 1 de update do número do PR)
- PR aberto, CI verde, **não mergeado**
- `main` local ainda aponta pro squash da 2.9 (operador faz merge depois)
- Working tree limpo
- `Money.java` em `shared/domain` com 7 operações + construtor validador
- `MoneyTest.java` com 35+ testes, todos passando
- Threshold JaCoCo `domain` 90% ativo no `pom.xml`
- `decisoes.md` e `progresso.md` atualizados, com PR número real registrado
- Prompt versionado em `docs/prompt-etapa-3-1.md`

Reportar ao operador o estado final com `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup` e parar.

## O que NÃO fazer ao terminar

- Não mergear o PR. Aguardar autorização explícita do operador.
- Não criar prompt da próxima etapa.
- Não rascunhar Etapa 3.2 (`conta`).
- Não criar pacote `conta/` ou outros bounded contexts vazios "preparando o terreno".
- Não importar Money em qualquer lugar fora do próprio teste.
- Não tocar em `frontend/`, `scripts/`, `docker-compose.yml`, `application.yml`.
- Não sugerir "próximo passo" espontaneamente.
- Não atualizar `progresso.md` marcando algo da Etapa 3.2 — esta etapa é exclusivamente Money.
