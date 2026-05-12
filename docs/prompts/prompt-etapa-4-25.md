# Prompt -- Sub-etapa 4.25: Ampliacao do test-writer para E2E tests de controllers

## Contexto

Sub-etapa de ampliacao do subagent `test-writer` (inaugurado na 4.17, ampliado para
integration tests na 4.18). A ADR-007 prescreve tres niveis: unit, integration, E2E.
Os dois primeiros foram entregues. A 4.25 completa o terceiro nivel.

Cobaia de smoke: `ContaController.java` -- 5 endpoints (`POST /api/contas`, `GET /api/contas`,
`GET /api/contas/{id}`, `DELETE /api/contas/{id}`, `GET /api/contas/{id}/saldo`).

Esta e a ultima sub-etapa FUNCIONAL da Camada 3. Apos merge, falta apenas a 4.26
(split de docs) antes de Camada 3 fechar formalmente e Camada 4 abrir.

Lembrete de workflow: passar este prompt como TEXTO ao executor. Commitar o `.md` manualmente
apos o PR estar aberto.

---

## Escopo decidido

### Arquivo 1: `.claude/agents/test-writer.md` (EDITAR -- 6 mudancas cirurgicas)

Leia o arquivo completo antes de editar. Aplique as 6 mudancas via Edit.
Nao altere nenhum trecho alem dos prescritos.

**Mudanca 1 -- Identidade: adicionar E2E level e remover linha "fora do escopo atual"**

Substitua:
```
**Niveis de teste cobertos:**

- **Unit tests** para classes em `*/domain/` (dominio puro, JUnit 5 + AssertJ, sem Spring).
- **Integration tests** para `*RepositoryImpl` em `*/infrastructure/persistence/` (Testcontainers via `AbstractIntegrationTest`).

E2E tests (controllers em `*/interfaces/`) estao **fora do escopo atual** — sera adicionado em sub-etapa futura se uso justificar.
```

Por:
```
**Niveis de teste cobertos:**

- **Unit tests** para classes em `*/domain/` (dominio puro, JUnit 5 + AssertJ, sem Spring).
- **Integration tests** para `*RepositoryImpl` em `*/infrastructure/persistence/` (Testcontainers via `AbstractIntegrationTest`).
- **E2E tests** para controllers em `*/interfaces/` (`@AutoConfigureMockMvc` + `MockMvc`, extends `AbstractIntegrationTest`).
```

**Mudanca 2 -- Tabela de detecao de nivel: linha Controller**

Substitua:
```
| `src/main/java/.../<contexto>/interfaces/<Classe>Controller.java` | E2E (fora do escopo) | Reporta "fora do escopo conhecido — E2E nao implementado nesta versao" e termina |
```

Por:
```
| `src/main/java/.../<contexto>/interfaces/<Classe>Controller.java` | E2E | Gera/edita `src/test/.../<contexto>/interfaces/<Classe>ControllerTest.java` |
```

**Mudanca 3 -- Adicionar secao "Regras duras de E2E test" antes de "Redirecionamento JpaRepository"**

Localize a linha exata `### Redirecionamento JpaRepository -> Impl` e insira o bloco
abaixo imediatamente antes dela (com linha em branco de separacao).

Conteudo a inserir:
```
### Regras duras de E2E test (path `*/interfaces/*Controller.java`)

Aplica-se quando o path da classe alvo casa com `*/interfaces/` e termina em `Controller.java`.

1. **JUnit 5 + AssertJ** (mesmas regras 1 e 2 do unit). NUNCA JUnit 4 ou Hamcrest.
2. **Extends `AbstractIntegrationTest`** + **`@AutoConfigureMockMvc`** na classe de teste.
   `AbstractIntegrationTest` ja provisiona `@SpringBootTest` + Testcontainers Postgres.
   `@AutoConfigureMockMvc` habilita MockMvc por injecao. **Nunca modificar a classe base.**
3. **`@Autowired MockMvc mockMvc`** para requisicoes HTTP contra o contexto Spring completo.
4. **`@Autowired ObjectMapper objectMapper`** para serializar/deserializar bodies JSON.
5. **`@Autowired <Contexto>JpaRepository`** para cleanup. Setup de dados via endpoint
   POST quando disponivel (testa stack completa); cleanup via `jpaRepository.deleteAll()`.
6. **`@AfterEach void limpar()`** -- `jpaRepository.deleteAll()` entre testes. Sem estado
   compartilhado entre testes.
7. **Sem mock de servico/use-case.** E2E passa pela stack completa:
   Controller -> UseCase -> Repository -> Banco real (Testcontainers).
8. **Sufixo `Test`** (singular). `ContaControllerTest.java`, nao `ContaControllerIT.java`.
9. **Pacote espelho.** `src/test/java/.../conta/interfaces/ContaControllerTest.java`.
10. **Static imports** de `MockMvcRequestBuilders` e `MockMvcResultMatchers`.

**Referencia de estilo (exemplo inline -- ContaControllerTest):**

```java
package com.laboratorio.financas.conta.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.conta.infrastructure.persistence.ContaJpaRepository;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class ContaControllerTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ContaJpaRepository contaJpaRepository;

    @AfterEach
    void limpar() {
        contaJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/contas retorna 201 com conta criada")
    void criar_retorna201_comContaCriada() throws Exception {
        String body = """
                {"nome":"Conta Corrente","tipo":"CORRENTE","saldoInicialValor":1000.00,"saldoInicialMoeda":"BRL"}
                """;

        mockMvc.perform(post("/api/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Conta Corrente"))
                .andExpect(jsonPath("$.ativa").value(true))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/contas retorna 200 com lista")
    void listar_retorna200_comLista() throws Exception {
        String body = """
                {"nome":"Conta Poupanca","tipo":"POUPANCA","saldoInicialValor":500.00,"saldoInicialMoeda":"BRL"}
                """;
        mockMvc.perform(post("/api/contas").contentType(MediaType.APPLICATION_JSON).content(body));

        mockMvc.perform(get("/api/contas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].nome").value("Conta Poupanca"));
    }

    @Test
    @DisplayName("GET /api/contas/{id} retorna 200 quando conta existe")
    void buscar_retorna200_quandoContaExiste() throws Exception {
        String body = """
                {"nome":"Conta Dinheiro","tipo":"DINHEIRO","saldoInicialValor":0,"saldoInicialMoeda":"BRL"}
                """;
        String response = mockMvc.perform(post("/api/contas")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/contas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    @DisplayName("DELETE /api/contas/{id} retorna 204")
    void desativar_retorna204() throws Exception {
        String body = """
                {"nome":"Conta a Desativar","tipo":"CORRENTE","saldoInicialValor":0,"saldoInicialMoeda":"BRL"}
                """;
        String response = mockMvc.perform(post("/api/contas")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(delete("/api/contas/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/contas/{id}/saldo retorna 200")
    void calcularSaldo_retorna200() throws Exception {
        String body = """
                {"nome":"Conta Saldo","tipo":"CORRENTE","saldoInicialValor":2000.00,"saldoInicialMoeda":"BRL"}
                """;
        String response = mockMvc.perform(post("/api/contas")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/contas/{id}/saldo", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoAtual.valor").isNotEmpty())
                .andExpect(jsonPath("$.contaId").value(id));
    }
}
```

```

**Mudanca 4 -- Remover itens obsoletos de "O que voce NAO GERA"**

Substitua (inclui linha seguinte para contexto unico):
```
- **Integration tests.** Escopo de sub-etapa futura (4.18+ se justificar).
- **E2E tests.** Escopo de sub-etapa futura (4.19+ se justificar).
- **Test fixtures, factories, builders compartilhados.**
```

Por:
```
- **Test fixtures, factories, builders compartilhados.**
```

**Mudanca 5 -- "Quando invocado" step 1a: atualizar condicional de Controller**

Substitua:
```
   - Se path e `*Controller.java` em `*/interfaces/`, reporte "fora do escopo conhecido — E2E nao implementado nesta versao" e termine.
```

Por:
```
   - Se path e `*Controller.java` em `*/interfaces/`, proceda com E2E test (ver secao "Regras duras de E2E test").
```

**Mudanca 6 -- "O que NAO fazer": remover restricoes obsoletas anti-E2E**

Substitua (inclui linha seguinte para contexto unico):
```
- **NAO use Spring, Testcontainers, ou qualquer infra de persistencia.** Unit test e dominio puro.
- **NAO gere integration tests ou E2E tests.** Escopo de sub-etapas futuras.
- **NAO crie classes auxiliares (fixtures, builders) sem necessidade.**
```

Por:
```
- **NAO crie classes auxiliares (fixtures, builders) sem necessidade.**
```

Substitua tambem (inclui linha seguinte para contexto unico):
```
- **NAO sugira ampliar escopo** (integration, E2E). Foco no que esta na 4.17.
- **NAO referencie sub-etapa futura como argumento.**
```

Por:
```
- **NAO referencie sub-etapa futura como argumento.**
```

Pos-condicao:
```powershell
Select-String "fora do escopo atual" ".claude/agents/test-writer.md"   # NAO deve ter match
Select-String "AutoConfigureMockMvc" ".claude/agents/test-writer.md"   # deve ter match
Select-String "E2E tests" ".claude/agents/test-writer.md"              # deve ter match
Select-String "NAO gere integration tests" ".claude/agents/test-writer.md"  # NAO deve ter match
```

---

### Arquivo 2: `docs/progresso.md` (EDITAR)

Leia o arquivo antes de editar. Aplique as 5 mudancas abaixo via Edit.

**Mudanca 1 -- linha "Ultima atualizacao":**

Substitua o valor atual por:
```
**Última atualização:** 2026-05-12 (Sub-etapa 4.25 -- Ampliacao do test-writer para E2E tests de controllers)
```

**Mudanca 2 -- criterio de pronto do E2E:**

Substitua:
```
- [ ] Ampliacao do `test-writer` para E2E tests (sub-etapa futura se uso justificar)
```

Por:
```
- [x] Ampliacao do `test-writer` para E2E tests (controllers em `*/interfaces/`) -- concluido 4.25
```

**Mudanca 3 -- adicionar 4.25 em "Sub-etapas concluidas"** (logo antes da entrada 4.24):

```
- **4.25 -- Ampliacao do `test-writer` para E2E tests de controllers** (2026-05-12):
  terceira ampliacao do subagent test-writer (apos unit em 4.17, integration em 4.18).
  Completa a cobertura ADR-007: tres niveis agora suportados. Seis edicoes cirurgicas no
  `test-writer.md`: nivel E2E adicionado a identidade e tabela de detecao; secao "Regras
  duras de E2E test" com exemplo inline (ContaControllerTest -- 5 endpoints); restricoes
  obsoletas removidas de "O que voce NAO GERA" e "O que NAO fazer". Mecanismo:
  `@AutoConfigureMockMvc` + `MockMvc` + `AbstractIntegrationTest` + `ContaJpaRepository`
  para cleanup. Setup via POST endpoint; cleanup via `deleteAll()`. Smoke:
  `/write-test ContaController.java` -> `ContaControllerTest.java` gerado e todos os
  testes passam (Docker ativo). Ultima sub-etapa funcional da Camada 3 (4.26 -- split de
  docs -- sera a ultima antes de fechar Camada 3 e abrir Camada 4). PR #71.
```

**Mudanca 4 -- adicionar em "Historico de mudancas":**

```
- **2026-05-12** -- Sub-etapa 4.25 concluida: test-writer ampliado para E2E tests de
  controllers. Tres niveis ADR-007 cobertos. Ultima sub-etapa funcional da Camada 3.
  PR #71.
```

---

### Arquivo 3: `docs/decisoes-claude-code.md` (EDITAR)

Leia o arquivo antes de editar. Adicione subsecao antes do "Historico de mudancas"
(linha em branco antes e depois de cada `##`):

```
## Sub-etapa 4.25 -- Ampliacao do test-writer para E2E tests

### Terceiro nivel de teste do test-writer

A 4.25 completa a cobertura ADR-007 no test-writer:

| Nivel | Sub-etapa | Mecanismo |
|-------|-----------|-----------|
| Unit | 4.17 | JUnit 5 + AssertJ, sem Spring, sem banco |
| Integration | 4.18 | `AbstractIntegrationTest` + Testcontainers Postgres |
| E2E | 4.25 | `AbstractIntegrationTest` + `@AutoConfigureMockMvc` + MockMvc |

### Decisao: AbstractIntegrationTest + @AutoConfigureMockMvc na subclasse

`AbstractIntegrationTest` ja preve `@SpringBootTest` (webEnvironment=MOCK por default)
+ Testcontainers Postgres. E2E test adiciona `@AutoConfigureMockMvc` apenas na subclasse
-- habilita `MockMvc` por injecao sem modificar a classe base. Alternativa rejeitada:
`@SpringBootTest(webEnvironment=RANDOM_PORT)` + `TestRestTemplate` -- nao adotado porque
o projeto ja usa MockMvc e porta real nao agrega valor aqui.

### Decisao: setup via POST, cleanup via JpaRepository

Setup de dados nos E2E tests: via endpoint POST quando disponivel (exercita stack completa
de criacao, reusa logica de negocio real). Cleanup via `JpaRepository.deleteAll()` no
`@AfterEach` (rapido, determinista, sem dependencia do endpoint DELETE que pode alterar
estado mas nao destruir registro).
```

Adicione ao "Historico de mudancas":

```
- **2026-05-12** -- Sub-etapa 4.25 concluida: test-writer ampliado para E2E tests de
  controllers (terceiro nivel ADR-007). `@AutoConfigureMockMvc` + MockMvc.
  Ultima sub-etapa funcional da Camada 3. PR #71.
```

---

## Estado esperado ao iniciar

```powershell
git branch --show-current   # deve retornar: feat/etapa-4-25-test-writer-e2e
git status                  # deve retornar: nothing to commit, working tree clean
git log --oneline -3
```

**Docker ativo (obrigatorio para smoke):**
```powershell
docker ps
# deve mostrar container financas-lab-postgres ativo
```

Se Docker nao estiver ativo: `docker compose up -d postgres` antes do smoke.

---

## Tarefas

### Tarefa 1 -- Verificar estado inicial

```powershell
git branch --show-current   # deve retornar: feat/etapa-4-25-test-writer-e2e
git status
git log --oneline -3
docker ps
```

### Tarefa 2 -- Editar `.claude/agents/test-writer.md`

Leia o arquivo completo. Aplique as 6 mudancas cirurgicas. Cada Edit usa old_string/new_string
exatos conforme prescrito.

Pos-condicao:
```powershell
Select-String "AutoConfigureMockMvc" ".claude/agents/test-writer.md"        # deve ter match
Select-String "fora do escopo atual" ".claude/agents/test-writer.md"        # NAO deve ter match
Select-String "NAO gere integration tests" ".claude/agents/test-writer.md"  # NAO deve ter match
```

### Tarefa 3 -- Primeiro commit

```powershell
git add ".claude/agents/test-writer.md"
git status   # deve mostrar exatamente 1 arquivo staged
```

Commit (scope `claude` sem ponto):
```
feat(claude): amplia test-writer para E2E tests de controllers via MockMvc
```

### Tarefa 4 -- Smoke: `/write-test ContaController.java`

Execute a skill:
```
/write-test src/main/java/com/laboratorio/financas/conta/interfaces/ContaController.java
```

Criterios de sucesso:
- [ ] Nivel E2E detectado a partir do path `*/interfaces/*Controller.java`
- [ ] `ContaControllerTest.java` gerado em `src/test/java/.../conta/interfaces/`
- [ ] Extends `AbstractIntegrationTest` com `@AutoConfigureMockMvc`
- [ ] 5 endpoints cobertos (POST 201, GET lista, GET por id, DELETE 204, GET saldo)
- [ ] Todos os testes passam via `./mvnw test -Dtest=ContaControllerTest`

Se qualquer criterio falhar: reporte erro literal. Nao tente auto-corrigir.

**CRITICO: `ContaControllerTest.java` e artefato de smoke -- NAO commitar na branch.**
Apos smoke bem-sucedido, deletar o arquivo gerado:
```powershell
Remove-Item "src\test\java\com\laboratorio\financas\conta\interfaces\ContaControllerTest.java"
git status   # deve mostrar: nothing to commit, working tree clean
```

### Tarefa 5 -- Atualizar docs

Aplique as mudancas prescritas nos Arquivos 2 e 3 (`progresso.md`, `decisoes-claude-code.md`).
Leia cada arquivo antes de editar.

Pos-condicao:
```powershell
Select-String "4.25" "docs/progresso.md" | Measure-Object | Select-Object -Expand Count
# deve retornar > 0

Select-String "4.25" "docs/progresso.md"
# deve ter match (nova entrada)

Select-String "4.25" "docs/decisoes-claude-code.md" | Measure-Object | Select-Object -Expand Count
# deve retornar > 0
```

### Tarefa 6 -- Segundo e terceiro commits

```powershell
git add "docs/progresso.md"
git status
```

Commit:
```
docs(progresso): registra sub-etapa 4.25 (ultima sub-etapa funcional da Camada 3)
```

```powershell
git add "docs/decisoes-claude-code.md"
git status
```

Commit:
```
docs(decisoes): registra ampliacao do test-writer para E2E (terceiro nivel ADR-007)
```

### Tarefa 7 -- Validacao pre-ship

```powershell
git log --oneline feat/etapa-4-25-test-writer-e2e ^main
# deve mostrar exatamente 3 commits

git diff main --name-only
# deve mostrar exatamente:
#   .claude/agents/test-writer.md
#   docs/decisoes-claude-code.md
#   docs/progresso.md

git status
# deve retornar: nothing to commit, working tree clean

Select-String "AutoConfigureMockMvc" ".claude/agents/test-writer.md"   # deve ter match
Select-String "fora do escopo atual" ".claude/agents/test-writer.md"   # NAO deve ter match
Select-String "4.25" "docs/progresso.md"                               # deve ter match
```

### Tarefa 8 -- Entregar via `/ship`

```
/ship
```

---

## Restricoes e freios

- NAO commitar `ContaControllerTest.java` na branch -- e artefato de smoke.
- NAO modificar `AbstractIntegrationTest.java` -- `@AutoConfigureMockMvc` vai na SUBCLASSE.
- NAO usar `@SpringBootTest(webEnvironment=RANDOM_PORT)` -- usar `@AutoConfigureMockMvc` na subclasse.
- NAO alterar testes unit ou integration existentes.
- NAO tocar em arquivos Java de producao.
- NAO modificar `decisoes.md` (arquivo diferente de `decisoes-claude-code.md`).
- Se hook bloquear commit: leia a mensagem, corrija sem `--no-verify`.

---

## Estrutura de commits

```
feat(claude): amplia test-writer para E2E tests de controllers via MockMvc
docs(progresso): registra sub-etapa 4.25 (ultima sub-etapa funcional da Camada 3)
docs(decisoes): registra ampliacao do test-writer para E2E (terceiro nivel ADR-007)
```

---

## Estado esperado ao terminar

- PR #71 aberto.
- `.claude/agents/test-writer.md` com E2E rules completas (6 mudancas aplicadas).
- `docs/progresso.md` com 4.25 registrada (Camada 3 permanece "Em andamento" -- fecha na 4.26).
- `docs/decisoes-claude-code.md` com entrada da 4.25.
- `ContaControllerTest.java` deletado (nao na branch).
- ADR-007 completamente coberto pelos tres niveis do test-writer.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO commitar o arquivo de prompt na branch (operador faz manualmente).
- NAO rodar `/ship` mais de uma vez.
