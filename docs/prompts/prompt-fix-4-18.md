# Prompt — Correcao do teste `calcularTotaisRetornaZerosParaContaSemTransacoes` (fix antes do merge da 4.18)

## Contexto

PR #63 (branch `refactor/etapa-4-18-test-writer-integration`) entregou a sub-etapa 4.18 (ampliacao do `test-writer` para integration tests + revisao da 4.17.1). Smoke pos-entrega foi executado com `/write-test src/main/.../TransacaoJpaRepository.java` e gerou 11 `@Test` novos em `TransacaoRepositoryImplTest.java` (acrescentados via `Edit`, preservando os 10 testes existentes).

**Resultado do smoke:** 20/21 testes passaram. 1 teste falhou:

```
TransacaoRepositoryImplTest.calcularTotaisRetornaZerosParaContaSemTransacoes:287
expected: null
 but was: TotaisTransacaoPorConta[totalReceitas=0, totalDespesas=0, totalTransferenciasEnviadas=0, totalTransferenciasRecebidas=0]
```

**Causa diagnosticada pelo proprio subagent no relatorio do smoke:**

A query JPQL em `TransacaoJpaRepository.calcularTotaisPorConta`:

```jpql
SELECT new com.laboratorio.financas.transacao.domain.TotaisTransacaoPorConta(
    COALESCE(SUM(CASE WHEN ... END), 0),
    ...
)
FROM TransacaoEntity t
WHERE t.contaId = :contaId OR t.contaDestinoId = :contaId
```

E uma query de agregacao **sem `GROUP BY`** com **constructor expression** (`new ...`) e `COALESCE`. Comportamento do Hibernate nesse caso: **sempre retorna uma linha** (com zeros pelo `COALESCE`), nao `null`, mesmo quando `WHERE` nao casa nenhuma linha.

O subagent gerou teste assumindo que retornaria `null` para conta sem transacoes — premissa errada. Assertion correta e `isNotNull()` + verificacao dos zeros.

**Decisao do operador:** corrigir o teste (Opcao (a) do relatorio do subagent), manter o cenario porque o comportamento "conta sem transacoes retorna zeros, nao null" e invariante valido de documentar.

Esta correcao **NAO e sub-etapa nova** (nao e 4.18.1, nao e refinamento pos-smoke empirico). E **correcao de codigo de teste com premissa errada** — vai como commit `fix(test):` no proprio branch da 4.18, antes do merge. Sub-etapa 4.18 mantida intacta; smoke validado em todos os criterios estruturais; falha foi semantica (premissa errada sobre JPQL), nao estrutural.

## Escopo decidido

### Arquivos modificados

| Arquivo | Tipo de mudanca |
|---|---|
| `src/test/java/com/laboratorio/financas/transacao/infrastructure/persistence/TransacaoRepositoryImplTest.java` | Substituir 1 assertion + acrescentar verificacoes de zeros |

**Nao tocados:** todos os demais arquivos. Esta e correcao minima, escopo cirurgico.

### Mudanca prescrita

Localizar o `@Test` `calcularTotaisRetornaZerosParaContaSemTransacoes` em `TransacaoRepositoryImplTest.java` (linha aproximada 287). Identificar a assertion atual:

```java
assertThat(totais).isNull();
```

**Substituir** por:

```java
assertThat(totais).isNotNull();
assertThat(totais.totalReceitas()).isEqualByComparingTo(BigDecimal.ZERO);
assertThat(totais.totalDespesas()).isEqualByComparingTo(BigDecimal.ZERO);
assertThat(totais.totalTransferenciasEnviadas()).isEqualByComparingTo(BigDecimal.ZERO);
assertThat(totais.totalTransferenciasRecebidas()).isEqualByComparingTo(BigDecimal.ZERO);
```

**Atencao a confirmar antes de editar:**

- O record `TotaisTransacaoPorConta` (em `transacao/domain/`) tem accessors gerados pelo Java: `totalReceitas()`, `totalDespesas()`, `totalTransferenciasEnviadas()`, `totalTransferenciasRecebidas()`. **Confirmar nomes exatos via `cat` do record antes de gerar o codigo final** — se algum campo tiver nome diferente (improvavel mas possivel), ajustar.
- `BigDecimal` provavelmente ja esta importado no arquivo (vimos no `git diff` do smoke). **Confirmar via Grep `import.*BigDecimal`** — se nao estiver, acrescentar import.
- Usar `isEqualByComparingTo(BigDecimal.ZERO)` em vez de `isEqualTo(BigDecimal.ZERO)`. Razao: `BigDecimal.equals` considera escala (`0` != `0.00`); `isEqualByComparingTo` compara apenas valor numerico, padrao consolidado em testes do projeto que envolvem `Money`/`BigDecimal`.

### Nao modificar

- Outros testes no arquivo (10 originais + 10 novos da 4.18). Apenas o `calcularTotaisRetornaZerosParaContaSemTransacoes` muda.
- `TransacaoJpaRepository.java` (codigo de producao). A query esta correta; o teste e que tinha premissa errada.
- `src/main/`, `pom.xml`, `frontend/`, `.claude/`, `docs/`.

## Estado esperado ao iniciar

- Branch atual: `refactor/etapa-4-18-test-writer-integration` (do PR #63).
- `git status`: working tree com `TransacaoRepositoryImplTest.java` ja modificado pelo subagent durante o smoke (acrescimo dos 11 `@Test`), **commitado ou nao** — confirmar via `git status`.
- `git log --oneline -5` mostra os commits da 4.18 + commit do subagent durante o smoke (se houve commit) ou nao (se o subagent so editou sem commitar).
- `./mvnw test -Dtest=TransacaoRepositoryImplTest` resulta em `20/21 testes passaram, 1 falhou` (estado atual confirmado pelo operador).

**Validar com:**

```powershell
git branch --show-current
git status
git log --oneline -5
```

**Pre-condicoes:**

- Branch atual e `refactor/etapa-4-18-test-writer-integration`.
- Se `git status` mostrar `TransacaoRepositoryImplTest.java` como **modificado mas nao commitado** (situacao apos smoke do subagent), os 11 `@Test` ainda estao no working tree e vao entrar no commit junto com o fix. Esse cenario e aceitavel.
- Se `git status` mostrar working tree limpo, significa que o subagent ja commitou o acrescimo durante o smoke — o commit do fix entra como novo commit isolado.

Se branch divergir do esperado, **parar e reportar**.

## Tarefas

### Tarefa 1 — Validar pre-requisitos

```powershell
git branch --show-current
git status
git log --oneline -5
```

Confirmar branch correto. Anotar estado do working tree (limpo ou com `TransacaoRepositoryImplTest.java` modificado pendente).

### Tarefa 2 — Sincronizar Environment.CurrentDirectory (ADR-011)

```powershell
cd C:\projetos\financas-lab
[System.Environment]::CurrentDirectory = (Get-Location).Path
[System.Environment]::CurrentDirectory
```

Esperado: `C:\projetos\financas-lab`.

### Tarefa 3 — Confirmar nomes dos accessors do record `TotaisTransacaoPorConta`

```bash
cat src/main/java/com/laboratorio/financas/transacao/domain/TotaisTransacaoPorConta.java
```

Esperado: record Java com 4 campos (`totalReceitas`, `totalDespesas`, `totalTransferenciasEnviadas`, `totalTransferenciasRecebidas`), tipo `BigDecimal`. Accessors gerados: `totalReceitas()`, etc.

**Se os nomes divergirem do esperado, parar e reportar.** Nao prosseguir com nomes errados.

### Tarefa 4 — Localizar a assertion errada no arquivo de teste

```powershell
Select-String -Path src\test\java\com\laboratorio\financas\transacao\infrastructure\persistence\TransacaoRepositoryImplTest.java -Pattern 'calcularTotaisRetornaZerosParaContaSemTransacoes' -Context 3,20
```

Esperado: localizar o `@Test` com esse nome, exibir as ~20 linhas seguintes (corpo do teste), confirmar que **a assertion atual e `assertThat(totais).isNull();`** (provavelmente na linha 287 ou proxima, conforme erro reportado pelo Surefire).

**Anotar:**

- Linha exata onde a assertion `isNull()` aparece.
- Indentacao usada (4 espacos, conforme padrao do projeto).
- Se ha outras assertions no mesmo teste antes/depois da linha do `isNull()` (em geral nao, mas confirmar para evitar destruir verificacao adjacente).

### Tarefa 5 — Confirmar import de `BigDecimal`

```powershell
Select-String -Path src\test\java\com\laboratorio\financas\transacao\infrastructure\persistence\TransacaoRepositoryImplTest.java -Pattern 'import java.math.BigDecimal'
```

Esperado: 1 match (import ja presente — vimos no `git diff` que `import java.math.BigDecimal;` ja estava no arquivo antes do smoke).

**Se nao houver match, acrescentar `import java.math.BigDecimal;`** na secao de imports do arquivo (em ordem alfabetica entre os demais imports `java.*`).

### Tarefa 6 — Aplicar a correcao via Edit

Usar a tool `Edit` para substituir a assertion errada pelas 5 assertions corretas. Atencao:

- **Old string:** linha exata da assertion atual, incluindo indentacao (provavelmente `        assertThat(totais).isNull();` — 8 espacos de indentacao).
- **New string:** 5 linhas (`isNotNull()` + 4 `isEqualByComparingTo(BigDecimal.ZERO)`), mesma indentacao.

Conteudo exato da substituicao:

**Old string (a confirmar via Tarefa 4):**

```java
        assertThat(totais).isNull();
```

**New string:**

```java
        assertThat(totais).isNotNull();
        assertThat(totais.totalReceitas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(totais.totalDespesas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(totais.totalTransferenciasEnviadas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(totais.totalTransferenciasRecebidas()).isEqualByComparingTo(BigDecimal.ZERO);
```

**Se os nomes dos accessors confirmados na Tarefa 3 divergirem dos esperados** (ex: campo se chama `receitas` em vez de `totalReceitas`), ajustar o New string conforme.

### Tarefa 7 — Validar via `./mvnw test`

```bash
./mvnw test -Dtest=TransacaoRepositoryImplTest
```

**Criterios de sucesso:**

- Compilacao OK (sem erro de simbolo nao encontrado, sem erro de tipo).
- Execucao: **21/21 testes passaram** (10 originais + 10 novos do smoke + 1 corrigido).
- Sem `BUILD FAILURE`.

**Se compilacao falhar:** investigar (provavelmente nome de accessor errado ou import faltando). Corrigir e re-rodar.

**Se algum teste alem do corrigido falhar:** parar e reportar (algo alem da correcao prescrita foi tocado).

**Se 21/21 passar:** prosseguir para Tarefa 8.

### Tarefa 8 — Commit

```bash
git status
```

Confirmar que apenas `TransacaoRepositoryImplTest.java` esta modificado/staged (e nada mais).

**Se o working tree contiver tambem outros arquivos modificados** (ex: o subagent durante o smoke deixou modificacoes pendentes em outros arquivos), parar e reportar — esse commit deve isolar APENAS a correcao do teste.

```bash
git add src/test/java/com/laboratorio/financas/transacao/infrastructure/persistence/TransacaoRepositoryImplTest.java
git status   # apenas esse arquivo staged
git commit -m "fix(test): corrige assertion de calcularTotaisRetornaZerosParaContaSemTransacoes"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Validacao implicita dos hooks ativos:**

- Conventional Commits (4.1): `fix(test):` e prefixo valido.
- Encoding UTF-8 (4.2): valida bytes (sem alteracao de encoding esperada).
- Markdown blank lines (4.3): nao se aplica (arquivo `.java`).
- Tamanho de docs (4.4): nao se aplica (arquivo `.java`).
- Maven release (4.5), @Entity (4.7): nao se aplica.

Se algum hook bloquear, parar e reportar.

### Tarefa 9 — Push

```bash
git push
```

Se o branch ja foi pushed antes (smoke do subagent pode ter feito push), `git push` empurra apenas o novo commit.

### Tarefa 10 — Aguardar CI verde

```bash
gh pr view 63 --json number,state,statusCheckRollup
```

Aguardar o CI rodar com o novo commit. Esperado: status `SUCCESS` em todos os checks (build, qualquer outro).

**Se CI quebrar:** investigar logs do Actions, reportar ao operador.

**Se CI passar:** prosseguir para Tarefa 11.

### Tarefa 11 — Reportar e aguardar autorizacao do operador

Reportar:

- `git log --oneline -7` mostrando os commits da 4.18 + o fix.
- `gh pr view 63 --json number,state,statusCheckRollup`.
- Confirmacao de 21/21 testes passando localmente.

**NAO mergear o PR.** Aguardar autorizacao explicita do operador.

## Restricoes e freios

1. **NAO modificar nenhum outro arquivo alem de `TransacaoRepositoryImplTest.java`.** Nada em `src/main/`, `.claude/`, `docs/`, `pom.xml`, `frontend/`.

2. **NAO modificar outros testes no proprio arquivo.** Apenas o `calcularTotaisRetornaZerosParaContaSemTransacoes` muda. Os outros 20 testes (10 originais + 10 novos do smoke) ficam intactos.

3. **NAO mudar o nome do teste** (`calcularTotaisRetornaZerosParaContaSemTransacoes`). Esse nome continua descrevendo o cenario — agora a assertion alinha com a expectativa correta ("retorna zeros para conta sem transacoes" exatamente o que estamos verificando).

4. **NAO modificar o codigo de producao** `TransacaoJpaRepository.java`. A query JPQL esta correta; era o teste que tinha premissa errada.

5. **NAO criar arquivos novos.** Apenas modificar o existente.

6. **NAO usar `git commit --no-verify`.** Hooks devem validar.

7. **NAO usar `git reset --hard`.** Se algo der errado, reportar e aguardar instrucao — nao apagar trabalho.

8. **NAO usar `pwsh`.** PowerShell 5.1.

9. **NAO mergear o PR.** Aguardar autorizacao explicita do operador apos CI verde.

10. **NAO criar prompt da 4.19 ou outros.** Esta sub-etapa entrega apenas o fix.

11. **NAO modificar `.claude/agents/test-writer.md`, `.claude/skills/`, hooks, ADRs, blueprint, CLAUDE.md, progresso.md, decisoes.md, decisoes-claude-code.md, hooks-pendentes.md.** O fix nao requer registro documental — `fix(test):` em commit isolado e suficiente. A 4.18 ja documentou tudo o que precisava sobre o smoke e o gap. A premissa errada do subagent **nao e bug do system prompt** que justifique 4.18.1 — e premissa semantica que so apareceria com execucao real, e foi correta corrigir pelo operador (Opcao (a) que o proprio subagent ofereceu).

12. **NAO renumerar testes, NAO reorganizar imports do arquivo.** Mudanca cirurgica: apenas as 5 linhas da nova assertion. Se faltar import de `BigDecimal`, acrescentar em ordem alfabetica (excecao prescrita pela Tarefa 5).

13. **Encoding UTF-8 sem BOM preservado** no arquivo modificado.

14. **Indentacao padrao do projeto** (4 espacos, sem tabs). Confirmar via inspecao das linhas adjacentes.

15. **Nao tomar decisao silenciosa em zona limitrofe.** Reportar divergencias (ex: nome de accessor diferente do esperado, mais de uma ocorrencia de `isNull()` no metodo alvo).

## Validacao antes de push

```bash
./mvnw test -Dtest=TransacaoRepositoryImplTest
.\scripts\check.ps1
git status
git log --oneline -7
```

Esperado:

- `mvnw test` retorna 21/21.
- `check.ps1` passa (se existir no projeto; alternativamente, hooks rodam no commit).
- Working tree limpo apos commit.
- 1 commit novo no branch (alem dos commits da 4.18).

## Estado esperado ao terminar

- Branch `refactor/etapa-4-18-test-writer-integration` empurrada com **6 commits** (4 da 4.18 + 1 update do PR + 1 fix do teste).
- PR #63 aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.17.1.
- Working tree limpo.
- `TransacaoRepositoryImplTest.java` com 21 `@Test`, todos passando.
- Reportar: `git log --oneline -7`, `gh pr view 63 --json number,state,statusCheckRollup`, output do `./mvnw test -Dtest=TransacaoRepositoryImplTest`.

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao executar `/write-test` nem qualquer outra skill.
- Nao modificar `src/main/java/`, `frontend/`, `pom.xml`.
- Nao criar prompt da 4.19 ou de outras sub-etapas.
- Nao criar/editar subagents, skills, hooks, MCPs.
- Nao mexer em `~/.claude/` global.
- Nao atualizar nenhum doc em `docs/`.
- Nao sugerir proxima sub-etapa.
- Nao usar `pwsh`.
- Nao usar `git reset --hard`.
- Nao usar `git commit --no-verify`.
