# Prompt -- Sub-etapa 5.2: test-writer ampliado para `*/application/*UseCase.java`

## Contexto

Sub-etapa de ampliacao do test-writer (padrao inaugurado em 4.18 para integration tests,
repetido aqui para application use cases). O bounded context `orcamento` entregou
`CalcularProgressoDoOrcamentoUseCase` sem cobertura de unit test (lacuna identificada
pelo pr-reviewer do PR #73). Todos os outros bounded contexts (conta, categoria, transacao)
tem cobertura completa de application/ -- o padrao ja existe em
`CalcularSaldoDaContaUseCaseTest.java` com Mockito.mock() + zero Spring.

Esta sub-etapa amplia o agente e entrega o teste faltante como smoke funcional.

Lembrete de workflow: o executor le este arquivo diretamente do disco e o commita
junto com os docs no ultimo commit da branch (padrao a partir da 5.2).

---

## Escopo decidido

### Arquivo 1: `.claude/agents/test-writer.md` (EDITAR -- 6 mudancas cirurgicas)

Leia o arquivo completo antes de editar. Use Edit para cada mudanca.

---

**Mudanca 1 -- frontmatter `description`:**

Substitua:
```
description: Gera tests para classes do projeto. Unit tests para classes em `*/domain/` (JUnit 5 + AssertJ, sem Spring). Integration tests para `*RepositoryImpl` em `*/infrastructure/persistence/` (Testcontainers via AbstractIntegrationTest). Quando invocado em `*JpaRepository.java`, redireciona para o `*Impl` correspondente. Recebe path da classe alvo como argumento. Valida output rodando `./mvnw test` antes de reportar.
```

Por:
```
description: Gera tests para classes do projeto. Unit tests para classes em `*/domain/` (JUnit 5 + AssertJ, sem Spring). Unit tests com Mockito para `*/application/*UseCase.java`. Integration tests para `*RepositoryImpl` em `*/infrastructure/persistence/` (Testcontainers via AbstractIntegrationTest). E2E tests para `*Controller` em `*/interfaces/`. Quando invocado em `*JpaRepository.java`, redireciona para o `*Impl` correspondente. Recebe path da classe alvo como argumento. Valida output rodando `./mvnw test` antes de reportar.
```

---

**Mudanca 2 -- secao "Identidade", lista de niveis cobertos:**

Substitua:
```
**Niveis de teste cobertos:**

- **Unit tests** para classes em `*/domain/` (dominio puro, JUnit 5 + AssertJ, sem Spring).
- **Integration tests** para `*RepositoryImpl` em `*/infrastructure/persistence/` (Testcontainers via `AbstractIntegrationTest`).
- **E2E tests** para `*Controller` em `*/interfaces/` (`@AutoConfigureMockMvc` + `MockMvc`, stack completa com Testcontainers).
```

Por:
```
**Niveis de teste cobertos:**

- **Unit tests** para classes em `*/domain/` (dominio puro, JUnit 5 + AssertJ, sem Spring).
- **Unit tests com Mockito** para `*/application/*UseCase.java` (use case com dependencias mockadas, JUnit 5 + AssertJ + Mockito, sem Spring).
- **Integration tests** para `*RepositoryImpl` em `*/infrastructure/persistence/` (Testcontainers via `AbstractIntegrationTest`).
- **E2E tests** para `*Controller` em `*/interfaces/` (`@AutoConfigureMockMvc` + `MockMvc`, stack completa com Testcontainers).
```

---

**Mudanca 3 -- tabela "O que voce GERA":**

Substitua a linha:
```
| Outros paths | Fora do escopo | Reporta "path nao mapeado para nivel de teste conhecido" e termina |
```

Por estas duas linhas:
```
| `src/main/java/.../<contexto>/application/<Classe>UseCase.java` | Unit com Mockito | Gera/edita `src/test/.../<contexto>/application/<Classe>UseCaseTest.java` |
| Outros paths | Fora do escopo | Reporta "path nao mapeado para nivel de teste conhecido" e termina |
```

---

**Mudanca 4 -- adicionar nova secao apos "Regras duras de UNIT test (path `*/domain/*.java`)":**

Insira o bloco abaixo logo apos a linha:
```
**Referencia de estilo:** le `src/test/java/com/laboratorio/financas/conta/domain/ContaTest.java` antes de gerar.
```

Conteudo a inserir:

```markdown

### Regras duras de UNIT test para APPLICATION use cases (path `*/application/*UseCase.java`)

Aplica-se quando o path da classe alvo casa com `*/application/` e termina em `UseCase.java`.

1. **JUnit 5 + AssertJ** (mesmas regras 1 e 2 do domain). NUNCA JUnit 4 ou Hamcrest.
2. **Mockito** (`Mockito.mock(<Interface>.class)`) para dependencias (repositories, ports).
   Instanciar o use case diretamente via construtor no `@BeforeEach setUp()`. NUNCA `@MockBean`.
3. **Zero Spring.** Sem `@SpringBootTest`, `@Autowired`, `@MockBean`. Use case puro, instanciado manualmente.
4. **`@BeforeEach void setUp()`** com mocks criados e use case instanciado:
   ```java
   @BeforeEach
   void setUp() {
       minhaRepository = Mockito.mock(MinhaRepository.class);
       outraRepository = Mockito.mock(OutraRepository.class);
       useCase = new MinhaUseCase(minhaRepository, outraRepository);
   }
   ```
5. **`when().thenReturn()`** para configurar comportamento dos mocks.
   **`verify()`** para confirmar chamadas com argumentos corretos quando relevante.
6. **Sufixo `Test`** (singular). `CalcularProgressoDoOrcamentoUseCaseTest.java`.
7. **Pacote espelho.** Se a classe alvo esta em `.../orcamento/application/CalcularProgressoDoOrcamentoUseCase.java`,
   o teste fica em `.../orcamento/application/CalcularProgressoDoOrcamentoUseCaseTest.java`.
8. **Cobertura foco:** logica de negocio do use case (faixas de resultado, casos de erro,
   comportamento com diferentes entradas). Nao testar infraestrutura.
9. **Helpers privados** para criar fixtures inline (seguir padrao de `contaComSaldo()` e `totais()`
   do `CalcularSaldoDaContaUseCaseTest.java`).

**Referencia de estilo:** le `src/test/java/com/laboratorio/financas/conta/application/CalcularSaldoDaContaUseCaseTest.java`
antes de gerar. Use como gabarito: estrutura `@BeforeEach setUp()`, uso de `Mockito.mock()`,
padrao `when().thenReturn()`, helpers privados para fixtures.
```

---

**Mudanca 5 -- step "1a" em "Quando invocado" (deteccao de nivel):**

Substitua:
```
   - Se path nao casa nenhum nivel conhecido, reporte "path nao mapeado para nivel de teste conhecido" no template padrao e termine. Nao improvise.
   - Se path e `*JpaRepository.java`, redirecione para o `*RepositoryImpl.java` correspondente (substitua nome no caminho). Confirme via `ls` que o Impl existe.
   - Se path e `*Controller.java` em `*/interfaces/`, siga o fluxo de E2E test (Regras duras de E2E, secao acima).
```

Por:
```
   - Se path nao casa nenhum nivel conhecido, reporte "path nao mapeado para nivel de teste conhecido" no template padrao e termine. Nao improvise.
   - Se path e `*JpaRepository.java`, redirecione para o `*RepositoryImpl.java` correspondente (substitua nome no caminho). Confirme via `ls` que o Impl existe.
   - Se path e `*UseCase.java` em `*/application/`, siga o fluxo de unit test com Mockito para application use cases (Regras duras de APPLICATION, secao acima).
   - Se path e `*Controller.java` em `*/interfaces/`, siga o fluxo de E2E test (Regras duras de E2E, secao acima).
```

---

**Mudanca 6 -- step 5 em "Quando invocado" (referencia de estilo):**

Substitua:
```
5. **Leia `ContaTest.java` como referencia de estilo:**

   ```bash
   cat src/test/java/com/laboratorio/financas/conta/domain/ContaTest.java
   ```

   Use como **gabarito estilistico** (tom dos `@DisplayName`, organizacao com `@Nested`, padroes de assertion). Nao copie estrutura cega — adapte ao que a classe alvo precisa.
```

Por:
```
5. **Leia o arquivo de referencia de estilo do nivel detectado:**

   - **Domain:** `src/test/java/com/laboratorio/financas/conta/domain/ContaTest.java`
   - **Application (UseCase):** `src/test/java/com/laboratorio/financas/conta/application/CalcularSaldoDaContaUseCaseTest.java`
   - **Integration (RepositoryImpl):** `src/test/java/com/laboratorio/financas/conta/infrastructure/persistence/ContaRepositoryImplTest.java`
   - **E2E (Controller):** usar inline example da secao "Regras duras de E2E test"

   Use como **gabarito estilistico**. Nao copie estrutura cega — adapte ao que a classe alvo precisa.
```

---

Tambem atualize as duas restricoes relacionadas em "O que NAO fazer":

Substitua:
```
- **NAO sugira ampliar escopo** alem dos tres niveis cobertos (unit, integration, E2E).
```
Por:
```
- **NAO sugira ampliar escopo** alem dos quatro niveis cobertos (unit domain, unit application com Mockito, integration, E2E).
```

Substitua:
```
- **NAO use Mockito em unit test puro de dominio.** Mock manual inline. Excecao deve ser justificada no relatorio.
```
Por:
```
- **NAO use Mockito em unit test de `*/domain/`.** Mock manual inline para domain. Mockito e permitido e esperado em `*/application/*UseCase.java`. Excecao em domain deve ser justificada.
```

---

Pos-condicao:
```powershell
Select-String "application" ".claude/agents/test-writer.md" | Measure-Object | Select-Object -Expand Count
# deve retornar > 5 (multiplas referencias ao novo nivel)

Select-String "CalcularSaldoDaContaUseCaseTest" ".claude/agents/test-writer.md"
# deve ter match (nova referencia de estilo)

Select-String "Mockito.mock" ".claude/agents/test-writer.md"
# deve ter match (regras novas)
```

---

### Arquivo 2: smoke -- `/write-test` em `CalcularProgressoDoOrcamentoUseCase.java`

Apos commitar a mudanca no agente, invoque:

```
/write-test src/main/java/com/laboratorio/financas/orcamento/application/CalcularProgressoDoOrcamentoUseCase.java
```

O test-writer agora deve detectar `*/application/*UseCase.java` e gerar unit tests com Mockito.

Cenarios esperados no arquivo gerado (`CalcularProgressoDoOrcamentoUseCaseTest.java`):
- Orcamento nao encontrado → lanca `OrcamentoNaoEncontradoException`
- Zero despesas → status ABAIXO, percentual 0%
- Despesa < 80% do limite → status ABAIXO
- Despesa exatamente 80% do limite → status ATENCAO (fronteira critica)
- Despesa entre 80% e 100% → status ATENCAO
- Despesa exatamente 100% do limite → status ATINGIDO (fronteira critica)
- Despesa > 100% do limite → status EXCEDIDO
- `listarComFiltros` chamado com filtros corretos (categoriaId, tipo=DESPESA, datas do mes)

Pos-condicao:
```powershell
Test-Path "src/test/java/com/laboratorio/financas/orcamento/application/CalcularProgressoDoOrcamentoUseCaseTest.java"
# deve retornar: True
```

Valide com `./mvnw test -Dtest=CalcularProgressoDoOrcamentoUseCaseTest`.

---

### Arquivo 3: `docs/progresso.md` (EDITAR)

Leia o arquivo antes de editar. Aplique as 3 mudancas abaixo.

**Mudanca 1 -- linha "Ultima atualizacao":**
```
**Última atualização:** 2026-05-12 (Sub-etapa 5.2 -- test-writer ampliado para application use cases)
```

**Mudanca 2 -- adicionar 5.2 em "Sub-etapas concluidas" da Camada 4 (logo antes de 5.1):**
```
- **5.2 -- test-writer ampliado para `*/application/*UseCase.java`** (2026-05-12): segunda
  ampliacao do test-writer (apos 4.18 para integration). Deteccao de nivel via path
  `*/application/*UseCase.java`. Regras: JUnit 5 + AssertJ + Mockito.mock(), zero Spring,
  @BeforeEach setUp(), helpers privados. Referencia de estilo: CalcularSaldoDaContaUseCaseTest.
  Smoke: CalcularProgressoDoOrcamentoUseCaseTest gerado (cobre 7+ cenarios incluindo fronteiras
  de StatusProgresso). Lacuna do PR #73 resolvida. PR #75.
```

**Mudanca 3 -- adicionar em "Historico de mudancas":**
```
- **2026-05-12** -- Sub-etapa 5.2 concluida: test-writer ampliado para application use cases.
  CalcularProgressoDoOrcamentoUseCaseTest gerado. PR #75.
```

---

### Arquivo 4: `docs/decisoes-claude-code.md` (EDITAR)

Leia o arquivo antes de editar. Adicione nova secao antes de `## Historico de mudancas`:

```
## Sub-etapa 5.2 -- test-writer ampliado para application use cases

### Padrao de unit test para application use cases

`*/application/*UseCase.java` usa Mockito (nao mock manual inline) porque use cases
tem dependencias de interface (repositories) que exigem stub de comportamento especifico.
Diferente de `*/domain/` onde mocks manuais bastam, use cases precisam de `when().thenReturn()`
para simular cenarios de repositorio (buscar/nao encontrar, retornar lista vazia, etc.).

Referencia de estilo: `CalcularSaldoDaContaUseCaseTest.java` (Camada 2, ja existia).

### Decisao: smoke = entrega real

O smoke da 5.2 e tambem o artefato de valor: `CalcularProgressoDoOrcamentoUseCaseTest.java`
resolve a lacuna identificada pelo pr-reviewer do PR #73. Commitado na branch, nao descartado.
```

Adicionar ao `## Historico de mudancas`:
```
- **2026-05-12** -- Sub-etapa 5.2 concluida: test-writer ampliado (4o nivel: application
  use cases com Mockito). Lacuna do PR #73 resolvida. PR #75.
```

---

## Estado esperado ao iniciar

```powershell
git branch --show-current   # deve retornar: main
git status                  # deve retornar: nothing to commit, working tree clean
git log --oneline -3        # deve mostrar PR #73 (orcamento) no topo (ou PR #74 do prompt)
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
git checkout -b feat/etapa-5-2-test-writer-application
git branch --show-current
```

### Tarefa 3 -- Editar test-writer.md (6 mudancas)

Leia `.claude/agents/test-writer.md` completo antes de editar.
Aplique as 6 mudancas prescritas (+ as 2 restricoes atualizadas) usando Edit.
Nao altere nenhum trecho alem dos prescritos.

Pos-condicao:
```powershell
Select-String "CalcularSaldoDaContaUseCaseTest" ".claude/agents/test-writer.md"
# deve ter match

Select-String "Mockito.mock" ".claude/agents/test-writer.md"
# deve ter match

Select-String "application" ".claude/agents/test-writer.md" | Measure-Object | Select-Object -Expand Count
# deve retornar > 5
```

### Tarefa 4 -- Commit 1: agente ampliado

```powershell
git add ".claude/agents/test-writer.md"
git status
```

Commit:
```
feat(claude): amplia test-writer para cobrir application use cases com Mockito
```

### Tarefa 5 -- Smoke: gerar testes via /write-test

```
/write-test src/main/java/com/laboratorio/financas/orcamento/application/CalcularProgressoDoOrcamentoUseCase.java
```

Aguarde o relatorio do test-writer.

Pos-condicao:
```powershell
Test-Path "src/test/java/com/laboratorio/financas/orcamento/application/CalcularProgressoDoOrcamentoUseCaseTest.java"
# deve retornar: True
```

Se `./mvnw test -Dtest=CalcularProgressoDoOrcamentoUseCaseTest` falhar: reporte erro literal.
NAO tente corrigir o arquivo de teste manualmente -- reportar ao operador.

### Tarefa 6 -- Commit 2: arquivo de teste gerado

```powershell
git add "src/test/java/com/laboratorio/financas/orcamento/application/CalcularProgressoDoOrcamentoUseCaseTest.java"
git status
```

Commit:
```
test(orcamento): adiciona unit tests para CalcularProgressoDoOrcamentoUseCase via /write-test
```

### Tarefa 7 -- Atualizar docs

Aplique as mudancas prescritas nos Arquivos 3 e 4 (`progresso.md`, `decisoes-claude-code.md`).
Leia cada arquivo antes de editar.

### Tarefa 8 -- Commit 3: docs + prompt

```powershell
git add "docs/progresso.md" "docs/decisoes-claude-code.md" "docs/prompts/prompt-etapa-5-2.md"
git status
```

Commit:
```
docs(decisoes): registra padrao de unit test para application use cases inaugurado na 5.2
```

### Tarefa 9 -- Validacao pre-ship

```powershell
git log --oneline feat/etapa-5-2-test-writer-application ^main
# deve mostrar exatamente 3 commits

git diff main --name-only
# deve mostrar exatamente:
#   .claude/agents/test-writer.md
#   docs/decisoes-claude-code.md
#   docs/progresso.md
#   docs/prompts/prompt-etapa-5-2.md
#   src/test/java/com/laboratorio/financas/orcamento/application/CalcularProgressoDoOrcamentoUseCaseTest.java

git status
# deve retornar: nothing to commit, working tree clean

./mvnw test -Dtest=CalcularProgressoDoOrcamentoUseCaseTest
# deve passar (BUILD SUCCESS)
```

### Tarefa 10 -- Entregar via /ship

```
/ship
```

---

## Restricoes e freios

- NAO modificar arquivos Java de producao.
- NAO modificar outros agentes (migration-writer, pr-reviewer, architect-reviewer).
- NAO modificar CLAUDE.md nesta sub-etapa.
- Se o smoke falhar (test-writer gerar teste que nao compila): reporte erro literal. NAO tente corrigir manualmente.
- Se hook bloquear commit: leia a mensagem, corrija sem `--no-verify`.
- Decisao silenciosa proibida: em divergencia entre prescricao e ambiente real, pare e reporte.

---

## Estrutura de commits

```
feat(claude): amplia test-writer para cobrir application use cases com Mockito
test(orcamento): adiciona unit tests para CalcularProgressoDoOrcamentoUseCase via /write-test
docs(decisoes): registra padrao de unit test para application use cases inaugurado na 5.2
```

3 commits. Escopo `claude` para o agente, `orcamento` para o teste, `decisoes` para os docs.

---

## Estado esperado ao terminar

- PR #75 aberto.
- 3 commits na branch `feat/etapa-5-2-test-writer-application`.
- `CalcularProgressoDoOrcamentoUseCaseTest.java` existente com 7+ testes passando.
- `test-writer.md` com 4 niveis documentados (domain, application, integration, E2E).
- `docs/progresso.md` e `docs/decisoes-claude-code.md` atualizados.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO rodar `/ship` mais de uma vez.
