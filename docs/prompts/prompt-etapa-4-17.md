# Prompt — Etapa 4.17: Primeiro subagent gerador (`test-writer` Sonnet) + skill `/write-test`

## Contexto

Camada 3 com 6 hooks (4.1-4.7, com 4.4 refinado pela 4.14) + CLAUDE.md + blueprint + 2 subagents revisores (`pr-reviewer` Haiku + `architect-reviewer` Sonnet) + 2 skills orquestradoras (`/review-pr` + `/review-arch`) + ADR-012 (revisao 4.11) apos a 4.16. Padrao skill+subagent validado em 2 casos revisores (PRs #55, #45, #35). `decisoes.md` e `decisoes-claude-code.md` saudaveis pos-split 4.16. Hook 4.4 silencioso em todos os docs. Zero debitos pendentes do projeto.

Esta sub-etapa entrega o **terceiro par skill+subagent** do projeto. **Mas com diferenca qualitativa fundamental:** primeiro subagent **gerador** do projeto (vs `pr-reviewer` e `architect-reviewer`, que sao revisores read-only).

**Implicacoes de "subagent gerador":**

- **Tools incluem `Write`.** Subagent cria arquivos no projeto, nao apenas le. Read-only quebra na 4.17.
- **Output e codigo que precisa compilar.** Criterio de smoke muda de "tem 3 secoes? cita ADRs?" para "arquivo gerado compila? testes passam?".
- **Falha tem custo pratico.** Se output sai errado, e arquivo `.java` no projeto que nao compila — ou pior, compila e testa coisa errada. Risco operacional maior que dos revisores.
- **Modelo importa mais.** Sonnet, alinhado com `architect-reviewer`. Haiku descartado para geracao de codigo idiomatico Java + Clean Architecture.

**Arquitetura escolhida: incremental, escopo focado.**

- 4.17 entrega `test-writer` cobrindo **apenas unit tests** (subset da ADR-007).
- 4.18+ amplia para integration / E2E **se o uso justificar**. Sub-etapa de refactor do mesmo subagent (categoria 4.14), nao novos subagents especialistas.
- Decisao consciente: "infraestrutura segue necessidade". Estrutura emerge do uso, nao da previsao.

Caracteristicas:

1. **Primeiro subagent gerador.** Replica padrao ADR-012 (skill orquestradora -> subagent dedicado) mas no eixo gerador. Estabelece **sub-padrao operacional novo**: subagents revisores sao read-only com output textual; subagents geradores tem `Write` e validam output via `Bash` antes de reportar.

2. **Categoria operacional nova: "primeira aplicacao de padrao em eixo novo".** Distinta de:
   - "Primeira aplicacao" (4.11): primeira implementacao do padrao recem-decidido (skill+subagent revisor).
   - "Replicacao de padrao consolidado" (4.12): segunda aplicacao do padrao validado.
   - **Esta categoria:** primeira aplicacao do padrao em um **eixo qualitativamente novo** (gerador em vez de revisor). Padrao base (skill orquestradora + `context: fork` + `agent: <nome>`) preservado; especifico de geracao (tools com `Write`, validacao via `Bash`, template arquivo+relatorio) inaugura sub-padrao.

3. **Subagent gera, valida, reporta — sem loop autonomo de auto-correcao.** Apos `Write`, subagent roda `./mvnw test -Dtest=ClasseTest` via `Bash`. Se compilou e passou: declara sucesso. Se nao: reporta erro literalmente, devolve decisao ao operador. Padrao "subagent reporta, operador decide" coerente com revisores (`pr-reviewer`, `architect-reviewer`).

4. **System prompt enumera regras duras + instrui consultar codigo existente como referencia de estilo.** Regras duras evitam erros catastroficos (`@Entity` no teste, JUnit 4 em vez de 5, Spring em unit). Referencia ao `ContaTest.java` existente evita drift estilistico do tom do projeto.

5. **CLAUDE.md NAO atualizado.** Convencao "subagents e skills" ja registrada na 4.11. 4.17 aplica convencao existente, nao cria nova.

Quando esta etapa terminar:

- `.claude/agents/test-writer.md`: terceiro subagent do projeto, modelo Sonnet, tools `Read, Grep, Glob, Bash, Write`, escopo unit tests.
- `.claude/skills/write-test/SKILL.md`: terceira skill orquestradora, recebe path de classe alvo como argumento.
- `docs/decisoes-claude-code.md`: subsecao 4.17 antes de "Claude Code hooks nativos".
- `docs/progresso.md`: sub-etapa 4.17 + criterios Camada 3 ajustados + licoes + historico.
- `docs/prompts/prompt-etapa-4-17.md`: versionado.

## Padroes que estreiam nesta etapa

1. **Subagent gerador como categoria distinta de subagent revisor.** Estabelece dimensoes que diferenciam:
   - **Tools:** geradores tem `Write` (e potencialmente `Edit` no futuro); revisores sao read-only.
   - **Output:** geradores entregam arquivo + relatorio; revisores entregam apenas relatorio em 3 secoes.
   - **Validacao:** geradores validam output via `Bash` antes de reportar; revisores nao tem o que validar (output e prosa).
   - **Smoke:** geradores exigem "output compila/passa nos testes"; revisores exigem "output tem 3 secoes + ancoragem em ADR".

   Registrar como sub-padrao da ADR-012 (sem criar ADR novo — e refinamento taxonomico, nao decisao estrutural).

2. **"Subagent reporta, operador decide" extendido para geradores.** Subagent gera codigo, valida internamente, **reporta resultado sem tentar auto-corrigir em loop**. Se output nao compila, reporta literalmente — operador decide se acata, refaz, ou abandona. Padrao "operador soberano" preservado.

3. **Arquitetura incremental em subagent gerador.** 4.17 cobre apenas unit tests (subset ADR-007). 4.18+ amplia o mesmo subagent **se uso justificar**. Sub-padrao operacional: subagents geradores **comecam com escopo focado e crescem por refactor (categoria 4.14)**, nao por proliferacao de especialistas. Decisao influenciada pelos principios "infraestrutura segue necessidade" e "estrutura emerge do uso".

## Escopo decidido (calibrado com operador antes da redacao via D1-D6)

### Arquivos modificados

| Arquivo | Tipo de mudanca |
|---|---|
| `.claude/agents/test-writer.md` | Novo (terceiro subagent, primeiro gerador) |
| `.claude/skills/write-test/SKILL.md` | Novo (terceira skill orquestradora) |
| `docs/decisoes-claude-code.md` | Subsecao 4.17 antes de "Claude Code hooks nativos" |
| `docs/progresso.md` | Sub-etapa 4.17 + criterios Camada 3 ajustados + licoes + historico |
| `docs/prompts/prompt-etapa-4-17.md` | Versionado (novo) |

**Nao tocados:** `.claude/agents/pr-reviewer.md`, `.claude/agents/architect-reviewer.md`, `.claude/skills/review-pr/SKILL.md`, `.claude/skills/review-arch/SKILL.md`, `.claude/hooks/`, `.githooks/`, `pom.xml`, `src/`, `frontend/`, `CLAUDE.md` (D5 da 4.12: nao atualizar), `docs/decisoes.md`, `docs/adrs.md`, `docs/visao.md`, `docs/blueprint-fabrica-ai-native.md`, `docs/progresso-historico.md`, `docs/hooks-pendentes.md`, `.gitignore`, `.gitattributes`.

### Conteudo de `.claude/agents/test-writer.md` (novo arquivo)

Encoding UTF-8 sem BOM. Sem acentos. Modelado pela estrutura do `architect-reviewer.md` (~175 linhas) com adaptacao para escopo gerador. Estimativa: ~200-220 linhas.

```markdown
---
name: test-writer
description: Gera unit tests para classes de dominio (POJOs em `*/domain/`). JUnit 5 + AssertJ, sem Spring, sem mock pesado. Recebe path da classe alvo como argumento. Valida output rodando `./mvnw test` antes de reportar.
tools: Read, Grep, Glob, Bash, Write
model: sonnet
---

Voce e o `test-writer` do projeto **financas-lab** — fabrica AI-native do operador Fabio. Atua como gerador de **unit tests** para classes de dominio. Primeiro subagent gerador do projeto.

## Identidade

Gerador de codigo Java idiomatico. Foco em unit tests de dominio puro. Le a classe alvo + classes vizinhas relevantes + ContaTest.java como referencia de estilo. Gera arquivo de teste, valida via `./mvnw test`, reporta resultado. **Nao tenta auto-corrigir em loop** — se nao compila ou nao passa, reporta erro literal e devolve decisao ao operador.

Tom: tecnico, direto, sem rodeios. Em portugues brasileiro coloquial profissional para o relatorio; codigo em ingles seguindo convencoes Java.

## O que voce GERA

**Apenas unit tests para classes em `*/domain/`.** Escopo focado deliberadamente (decisao operacional 4.17): comecar pequeno, ampliar via refactor em sub-etapas futuras se justificar.

Regras duras de unit test (ADR-007, decisoes-claude-code.md):

1. **JUnit 5** (`org.junit.jupiter.api.Test`, `@DisplayName`, `@Nested`, `@ParameterizedTest` quando fizer sentido). NUNCA JUnit 4.
2. **AssertJ** (`org.assertj.core.api.Assertions.assertThat`). NUNCA Hamcrest, NUNCA `assertEquals` puro do JUnit.
3. **Zero Spring.** Sem `@SpringBootTest`, sem `@Autowired`, sem `@MockBean`. Unit test e dominio puro.
4. **Zero mock pesado de DB.** Sem `@DataJpaTest`, sem Testcontainers. Unit test nao toca persistencia.
5. **Sufixo `Test`** (singular). `ContaTest.java`, nao `ContaTests.java`.
6. **Pacote espelho.** Se a classe alvo esta em `src/main/java/com/laboratorio/financas/conta/domain/Conta.java`, o teste fica em `src/test/java/com/laboratorio/financas/conta/domain/ContaTest.java`.
7. **NAO usar classe base abstract.** Unit tests nao herdam de `AbstractIntegrationTest` (essa e para integration). Cada classe de unit test e standalone.
8. **Mock manual quando precisar de dependencia.** Se a classe alvo depende de interface (ex: repository), criar mock manual inline (anonymous class ou simple stub). NUNCA usar Mockito para unit test puro de dominio (excecao: se classe alvo realmente exigir mock complexo, justificar no relatorio).

## O que voce NAO GERA

- **Integration tests.** Escopo de sub-etapa futura (4.18+ se justificar).
- **E2E tests.** Escopo de sub-etapa futura (4.19+ se justificar).
- **Test fixtures, factories, builders compartilhados.** Pode criar classe `<Classe>TestFixtures` se for inevitavel para o teste alvo, mas evite — preferir construcao inline.
- **Modificacoes na classe alvo.** Voce nao edita `src/main/java/.../Classe.java`. Apenas gera o teste. Se a classe alvo tem problema que impede teste (campo private sem getter necessario), reporta no relatorio.
- **Documentacao alem do teste.** Sem javadoc detalhado nos testes (a menos que o projeto use — verificar via Grep). Comentarios apenas onde clareza exige.

## Quando invocado

1. **Receba o path da classe alvo via argumento.** Padrao: `src/main/java/com/laboratorio/financas/<contexto>/domain/<Classe>.java`.

2. **Leia a classe alvo completamente:**

   ```bash
   cat <path-da-classe-alvo>
   ```

3. **Leia classes referenciadas:**
   - Imports que apontam para outras classes do projeto (`com.laboratorio.financas.*`).
   - `Money` em `shared/domain/` (provavelmente usado).
   - Enums no mesmo bounded context.

4. **Leia `ContaTest.java` como referencia de estilo:**

   ```bash
   cat src/test/java/com/laboratorio/financas/conta/domain/ContaTest.java
   ```

   Use como **gabarito estilistico** (tom dos `@DisplayName`, organizacao com `@Nested`, padroes de assertion). Nao copie estrutura cega — adapte ao que a classe alvo precisa.

5. **Inferir cobertura necessaria:**
   - **Construtor:** todos os ramos de validacao + caso feliz.
   - **Metodos publicos:** caso feliz + edge cases obvios.
   - **Equals/hashCode/toString:** apenas se a classe sobrescreveu de maneira nao-trivial (verificar via leitura). Se for boilerplate gerado por IDE, pular.
   - **Getters/setters triviais:** pular. Cobertura tonta.

6. **Gerar arquivo de teste** em `src/test/java/<espelho-da-classe-alvo>`.

7. **Validar via Bash:**

   ```bash
   ./mvnw test -Dtest=<NomeDoTest>
   ```

   Capturar saida. Se compilou e todos os testes passaram: sucesso. Se nao: capturar erro literal para reportar.

8. **Reportar via template prescrito** (ver abaixo).

## Template de output

\`\`\`markdown
# Test-writer para <Classe>

## Arquivo gerado

`src/test/java/.../<contexto>/domain/<Classe>Test.java` (<N> linhas).

## Cobertura

**Construtor:**
- <bullet do que cobriu>
- <bullet>

**Metodos:**
- `<metodo>`: <o que cobriu, edge cases>
- `<metodo>`: <idem>

**Edge cases:**
- <bullet>

**Nao testado deliberadamente:**
- <ex: getter trivial de campo X — boilerplate>
- <ex: equals/hashCode — gerado por IDE, sem logica custom>

## Validacao

- **Compilacao:** ✅ ou ❌
- **Execucao:** N/M testes passaram
- **Comando:** `./mvnw test -Dtest=<NomeDoTest>`

(Se ❌ ou falha de teste, incluir saida literal do erro abaixo da tabela.)

## Decisoes de design

- <ex: usei `assertThat().isInstanceOf()` em vez de try-catch porque AssertJ e convencao do projeto (referencia: ContaTest.java)>
- <ex: agrupei testes do construtor em `@Nested class Construtor` seguindo padrao de ContaTest>

## Limitacoes conhecidas

- <ex: cobertura de equals/hashCode pulada — classe usa boilerplate gerado>
- <ex: nao testei comportamento concorrente — fora do escopo de unit test>
- <ex: classe alvo tem campo private sem getter — testei comportamento indireto via metodo publico>
\`\`\`

## Exemplos

### Exemplo 1: classe simples (caso happy)

Cenario: classe `Conta` em `conta/domain/Conta.java` com construtor `Conta(String nome, Money saldoInicial)`, validacoes (nome nao vazio, saldoInicial nao negativo), metodos `creditar(Money)` e `debitar(Money)` retornando nova `Conta`.

Output esperado: gera `ContaTest.java` com testes do construtor (caso happy, nome vazio, nome null, saldo negativo), `creditar` (incrementa saldo, retorna nova instancia, imutabilidade), `debitar` (decrementa saldo, falha se ultrapassar zero se for regra, retorna nova instancia). Roda `./mvnw test -Dtest=ContaTest`, todos passam. Relatorio reporta sucesso + decisoes de design + sem limitacoes relevantes.

### Exemplo 2: validacao falhando

Cenario: classe `Transacao` em `transacao/domain/Transacao.java` com validacoes cruzadas no construtor (TRANSFERENCIA exige contaDestinoId, proibe categoriaId; outros tipos proibem contaDestinoId). Subagent gera teste, mas comete erro: testa cenario que conflita com regra do dominio que nao tinha visto. `./mvnw test` falha em 2/15 testes.

Output esperado: arquivo gerado (com bugs), relatorio com **Validacao: 13/15 passaram**, saida literal dos 2 testes que falharam, **Limitacoes conhecidas** explicando que subagent gerou testes assumindo regra X mas regra real era Y — operador decide se ajusta o teste, ajusta a classe alvo, ou refaz. **Subagent NAO tenta auto-corrigir.**

## Tom

- Direto. Sem "talvez", "considere", "seria bom" excessivos.
- Pragmatico. Decisoes do projeto sao respeitadas — nao questione ADR-007, aplique-o.
- Transparente sobre limitacoes. Se a classe alvo tem complexidade que exige decisao do operador, reporta — nao adivinha.
- Codigo em ingles seguindo convencoes Java (`should_return_X_when_Y` ou `whenY_thenX` conforme estilo do projeto — inferir via `ContaTest.java`).
- Relatorio em portugues brasileiro coloquial profissional.

## O que NAO fazer

- **NAO modifique a classe alvo.** Voce gera teste, nao edita codigo de producao. Se a classe alvo tem bug ou design problematico, reporta — nao corrige.
- **NAO tente auto-corrigir em loop.** Apos `./mvnw test`, se falhou: reporte. Nao re-escreva e re-teste tentando consertar. Operador decide.
- **NAO use Spring, Testcontainers, ou qualquer infra de persistencia.** Unit test e dominio puro.
- **NAO gere integration tests ou E2E tests.** Escopo de sub-etapas futuras.
- **NAO crie classes auxiliares (fixtures, builders) sem necessidade.** Preferir construcao inline. Excecao justificada no relatorio.
- **NAO ignore o `ContaTest.java` como referencia de estilo.** Le antes de gerar. Drift estilistico e problema operacional.
- **NAO sugira ampliar escopo** (integration, E2E). Foco no que esta na 4.17.
- **NAO referencie sub-etapa futura como argumento.**
- **NAO use Mockito em unit test puro de dominio.** Mock manual inline. Excecao deve ser justificada no relatorio.
```

### Conteudo de `.claude/skills/write-test/SKILL.md` (novo arquivo)

Encoding UTF-8 sem BOM. Sem acentos. Espelho da `review-arch/SKILL.md` com adaptacao de nome/agent/argumento/descricao.

```markdown
---
name: write-test
description: Gera unit tests para classe de dominio (POJO em */domain/) via subagent test-writer (Sonnet). Recebe path da classe alvo como argumento. Output: arquivo de teste + relatorio.
disable-model-invocation: true
context: fork
agent: test-writer
argument-hint: [path-da-classe-alvo]
allowed-tools: Bash(./mvnw *) Bash(cat *) Bash(ls *)
---

Gere unit tests para a classe em `$ARGUMENTS` seguindo todas as instrucoes do seu system prompt.

Leia a classe alvo, classes referenciadas, e `ContaTest.java` como referencia de estilo. Gere o arquivo de teste no pacote espelho. Valide via `./mvnw test -Dtest=<NomeDoTest>`. Reporte resultado no template prescrito.

Se output nao compilar ou testes falharem: reporte o erro literalmente, sem tentar auto-corrigir.
```

### Conteudo da subsecao em `docs/decisoes-claude-code.md`

Inserir **antes** da subsecao `### Claude Code hooks nativos`, **apos** "Split do `decisoes.md` por tema (Sub-etapa 4.16)":

```markdown
### Primeiro subagent gerador: test-writer + skill /write-test (Sub-etapa 4.17)

Terceiro par skill+subagent do projeto, **primeiro gerador** (vs `pr-reviewer` e `architect-reviewer` que sao revisores read-only). Aplica padrao ADR-012 em **eixo qualitativamente novo**.

**Subagent (`.claude/agents/test-writer.md`):**

- **Modelo: Sonnet.** Consistente com `architect-reviewer` (4.12). Haiku descartado para geracao de codigo idiomatico Java + Clean Architecture.
- **Tools:** `Read, Grep, Glob, Bash, Write`. Primeiro subagent do projeto com `Write` — necessario para gerar arquivo de teste. `Edit` deliberadamente fora (geracao cria arquivo novo; ampliacao de teste existente e caso de uso diferente, fora desta sub-etapa).
- **Escopo: apenas unit tests para classes em `*/domain/`.** Subset focado da ADR-007 (3 niveis). Integration e E2E ficam para sub-etapas futuras (4.18+) **se uso justificar** — sub-etapa de refactor do mesmo subagent (categoria 4.14), nao novos subagents especialistas. Decisao operacional: "infraestrutura segue necessidade".
- **Regras duras enumeradas:** JUnit 5, AssertJ, sem Spring, sem mock de DB, sufixo Test, pacote espelho, sem classe base abstract (que e para integration), mock manual inline (Mockito so com justificativa).
- **Referencia de estilo:** subagent le `ContaTest.java` antes de gerar — evita drift estilistico.
- **Validacao via Bash:** apos `Write`, subagent roda `./mvnw test -Dtest=<NomeDoTest>` para validar.
- **Sem auto-correcao em loop:** se nao compila ou testes falham, subagent **reporta erro literal e devolve decisao ao operador**. Padrao "subagent reporta, operador decide" coerente com revisores.
- **Template de output: arquivo + relatorio em 5 secoes** (Arquivo gerado, Cobertura, Validacao, Decisoes de design, Limitacoes conhecidas). Diferente dos revisores (3 secoes de texto) — geradores entregam artefato + meta-informacao.
- **Exemplos few-shot:** 2 — caso happy (Conta simples) + caso validacao falhando (subagent gerou bugs, reporta sem tentar consertar).

**Skill orquestradora (`.claude/skills/write-test/SKILL.md`):**

- Espelho da `review-arch/SKILL.md` com adaptacao de nome/agent/descricao.
- `disable-model-invocation: true` + `context: fork` + `agent: test-writer`. Mecanismo nativo do Claude Code (ADR-012 revisao 4.11).
- Argumento explicito: path da classe alvo (`/write-test src/main/.../Transacao.java`). Padrao consolidado dos revisores (`/review-pr <numero>`, `/review-arch <numero>`).
- `allowed-tools` declara comandos Bash usados (`./mvnw *`, `cat *`, `ls *`).

**Sub-padrao operacional novo: subagent gerador vs revisor.**

Categoria distinta de subagent dentro do padrao ADR-012:

| Dimensao | Subagent revisor (4.9, 4.12) | Subagent gerador (4.17) |
|---|---|---|
| Tools | `Read, Grep, Glob, Bash` (read-only) | `Read, Grep, Glob, Bash, Write` |
| Output | Relatorio em 3 secoes (texto) | Arquivo + relatorio em 5 secoes |
| Validacao | Sem validacao explicita (output e prosa) | `Bash` rodando comando do projeto antes de reportar |
| Smoke | "Tem 3 secoes? Cita ADRs?" | "Output compila? Testes passam?" |
| Risco | Baixo (read-only) | Medio (cria arquivos no projeto) |

**Sem ADR novo** — refinamento taxonomico da ADR-012, nao decisao estrutural. Sub-padrao registrado nesta subsecao + nas licoes 4.17.

**Categoria operacional nova: "primeira aplicacao de padrao em eixo novo".** Distinta de:

- **"Primeira aplicacao"** (4.11): primeira implementacao do padrao recem-decidido.
- **"Replicacao de padrao consolidado"** (4.12): segunda aplicacao do padrao validado em caso equivalente.
- **Esta categoria:** primeira aplicacao do padrao em **eixo qualitativamente novo** (gerador em vez de revisor). Padrao base (skill orquestradora + `context: fork` + `agent: <nome>`) preservado; especifico de geracao (tools com `Write`, validacao via `Bash`, template arquivo+relatorio) inaugura sub-padrao.

**Arquitetura incremental escolhida deliberadamente.** Alternativas avaliadas:

- **Arquitetura A — 3 subagents especialistas** (`test-writer-unit`, `test-writer-integration`, `test-writer-e2e`): rejeitada por decidir estrutura antes do uso real.
- **Arquitetura B — 1 subagent generalista cobrindo 3 niveis ja**: rejeitada por system prompt longo demais (300+ linhas estimadas) e risco de confundir niveis no output.
- **Arquitetura C (escolhida) — 1 subagent que cresce por refactor**: comeca focado em unit, amplia em sub-etapas seguintes se uso justificar. Coerente com principios "infraestrutura segue necessidade" e "estrutura emerge do uso".

**CLAUDE.md NAO atualizado nesta sub-etapa.** Subsecao "Subagents e skills" da 4.11 ja registra o padrao generico. 4.17 aplica convencao existente em eixo novo — distincao revisor/gerador fica documentada aqui em `decisoes-claude-code.md`, nao em CLAUDE.md (CLAUDE.md cobre convencoes do projeto, nao taxonomia interna de subagents).

**Smoke test pos-merge** (responsabilidade do operador apos autorizar merge):

1. Sessao nova do Claude Code.
2. Cobaia primaria: `Conta.java` (etapa 3.2). Domain puro, simples. Comando: `/write-test src/main/java/com/laboratorio/financas/conta/domain/Conta.java`.
3. **Criterios de sucesso:**
   - Skill dispara fork no agent `test-writer` (Sonnet) — sem execucao direta pelo Claude principal.
   - Arquivo `ContaTest.java` gerado em `src/test/java/.../conta/domain/`.
   - `./mvnw test -Dtest=ContaTest` compila e passa (todos os testes).
   - Output respeita convencoes: JUnit 5, AssertJ, sem Spring, sufixo `Test`.
   - Cobertura razoavel (nao precisa de paridade com `ContaTest` manual existente — referencia de estilo, nao gabarito a copiar).
4. **Se smoke primario OK:** cobaia secundaria `Transacao.java` (etapa 3.6, mais complexa). Se ambos OK, padrao validado.
5. **Se smoke primario falhar:** abrir 4.17.1 (refinamento pos-smoke empirico, categoria 4.9.1).

**Atencao especial:** o smoke da 4.17 e qualitativamente diferente dos smokes anteriores. Anteriores validavam "output e texto bem-formatado". 4.17 valida "output e codigo que compila e passa nos testes". Falha aqui e visivelmente quantificavel (`./mvnw test` reporta).
```

### Conteudo das edicoes em `docs/progresso.md`

**Edicao 1 — Sub-etapa 4.17 no topo de "Sub-etapas concluidas" da Camada 3** (acima da 4.16):

```markdown
- **4.17 — Primeiro subagent gerador (`test-writer` Sonnet) + skill `/write-test`** (2026-05-12): terceiro par skill+subagent do projeto, **primeiro gerador** (vs revisores `pr-reviewer` Haiku e `architect-reviewer` Sonnet). Aplica padrao ADR-012 em eixo qualitativamente novo. Subagent em `.claude/agents/test-writer.md` (flat) com modelo Sonnet e tools `Read, Grep, Glob, Bash, Write` (primeiro subagent com `Write`). Escopo focado: **apenas unit tests para classes em `*/domain/`** (subset ADR-007). Integration e E2E ficam para 4.18+ se uso justificar — sub-etapa de refactor (categoria 4.14), nao novos subagents especialistas. Arquitetura C escolhida deliberadamente (1 subagent que cresce por refactor) vs Arquitetura A (3 especialistas) ou B (1 generalista ja com 3 niveis). Regras duras enumeradas no system prompt (JUnit 5, AssertJ, sem Spring, sufixo Test, pacote espelho, mock manual inline). `ContaTest.java` como referencia de estilo. Validacao via `./mvnw test -Dtest=<NomeDoTest>` antes de reportar — **sem loop autonomo de auto-correcao**. Template de output: arquivo + relatorio em 5 secoes (Arquivo gerado, Cobertura, Validacao, Decisoes de design, Limitacoes). Skill `.claude/skills/write-test/SKILL.md` espelho da `review-arch/SKILL.md` com adaptacao. 2 exemplos few-shot (caso happy Conta + caso validacao falhando). Categoria nova: **"primeira aplicacao de padrao em eixo novo"**. Sub-padrao operacional novo (revisor vs gerador) registrado em `decisoes-claude-code.md`. CLAUDE.md NAO atualizado (regra 4.6 — convencao "subagents e skills" ja registrada na 4.11). PR #XX.
```

**Edicao 2 — Criterios da Camada 3 ajustados** (substituir bloco atual; principais mudancas: `test-writer` e `/write-test` marcados `[x]`):

```markdown
### Criterios de "pronto" (preliminar, ajustados pela 4.10, 4.11, 4.12, 4.13, 4.16 e 4.17)

- [x] `CLAUDE.md` do projeto escrito (target <=15KB) -- concluido 4.6, atualizado 4.11, 4.13 e 4.16
- [x] Padrao skill orquestradora -> subagent decidido -- ADR-012 (4.10, revisado 4.11)
- [x] Subagent `pr-reviewer` (revisao critica antes do PR) -- concluido 4.9 + refinado 4.9.1
- [x] Skill `/review-pr` orquestrando `pr-reviewer` (par com ADR-012) -- concluido 4.11
- [x] Smoke pos-merge da 4.11 validando padrao skill+subagent ponta-a-ponta -- validado em PR #55 + PR #45
- [x] Subagent `architect-reviewer` (Sonnet, valida decisoes contra ADR-004/005/006/007) -- concluido 4.12
- [x] Skill `/review-arch` orquestrando `architect-reviewer` (par com ADR-012) -- concluido 4.12
- [x] Smoke pos-merge da 4.12 validando segundo par skill+subagent -- validado em PR #35
- [x] Subagent `test-writer` + skill `/write-test` (par ADR-012, primeiro gerador) -- concluido 4.17 (escopo: unit tests; integration/E2E em 4.18+ se justificar)
- [ ] Smoke pos-merge da 4.17 validando primeiro par skill+subagent gerador
- [ ] Subagent (opcional) `migration-writer` + skill `/write-migration` (par ADR-012)
- [ ] Skill `/feature <nome>` (cria estrutura de bounded context)
- [ ] Skill `/ship` (lint + test + build + push + PR)
- [ ] Skill `/migrate` (gera migration + atualiza schema + escreve teste)
- [ ] Skill `/audit` (varre modulos buscando padrao especifico)
- [x] Hook pre-commit funcionando -- concluido 4.1-4.7, refinado 4.14
- [ ] Hook post-edit rodando testes do arquivo mexido
- [ ] Decisao sobre plugin `code-review` oficial: manter, desativar ou reaproveitar? (Re-classificado 4.15: nao e debito do projeto, e decisao pessoal do operador sobre setup Claude Code.)
```

**Edicao 3 — Bloco "Licoes da Sub-etapa 4.17"** acima de "Licoes da Sub-etapa 4.16":

```markdown
## Licoes da Sub-etapa 4.17

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — entrega subagent + skill, nao hook.)

### Licoes de ambiente

1. **Subagent gerador como categoria distinta de subagent revisor.** Sub-padrao operacional novo formalizado. Dimensoes que diferenciam: tools (geradores tem `Write`), output (artefato + relatorio vs prosa), validacao (geradores validam via `Bash`), smoke (compila/passa vs tem 3 secoes), risco (medio vs baixo). Registrado como refinamento taxonomico da ADR-012 — **sem ADR novo**, porque a decisao estrutural (skill orquestradora -> subagent dedicado) permanece; apenas a aplicacao varia.

2. **Categoria operacional nova: "primeira aplicacao de padrao em eixo novo".** Distinta de "primeira aplicacao" (4.11, primeira implementacao do padrao recem-decidido) e "replicacao de padrao consolidado" (4.12, segunda aplicacao em caso equivalente). Esta categoria cobre quando o padrao base e preservado mas o eixo de aplicacao muda qualitativamente. Padrao replicavel para futuros casos: aplicar padrao validado em eixo novo (ex: hooks em linguagem diferente, skills em fluxo diferente) sem reabrir decisao estrutural do padrao base.

3. **Arquitetura C (subagent focado que cresce por refactor) escolhida sobre A (3 especialistas) e B (1 generalista).** Princípio "infraestrutura segue necessidade" aplicado a desenho de subagent. Vantagem: 4.18 sabera se vale ampliar e como, em vez de decidir no escuro. Risco: system prompt pode crescer demais e justificar split em sub-etapa futura — aceitavel, e refactor consciente quando dor aparecer. Padrao operacional: **subagents iniciam focados e crescem por refactor, nao por proliferacao**.

4. **"Subagent reporta, operador decide" extendido para geradores.** Subagent gera, valida via `./mvnw test`, **reporta resultado sem tentar auto-corrigir em loop**. Se nao compilou ou falhou: reporta erro literal, devolve decisao. Padrao "operador soberano" preservado mesmo em subagent que mexe em arquivos do projeto. Razao: loop autonomo de auto-correcao tem risco de recursao (tenta arrumar, piora, tenta arrumar, piora). Primeiro contato com subagent gerador nao deveria abrir essa porta.

5. **Smoke qualitativamente diferente dos anteriores.** Smokes 4.11 e 4.12 (revisores) validavam "output e texto bem-formatado em 3 secoes ancoradas em ADRs". Smoke 4.17 (gerador) valida "output compila e passa nos testes". Falha aqui e visivelmente quantificavel via `./mvnw test`. Implicacao: smokes de subagents geradores tem **criterio binario forte** (compila/nao compila; passa/nao passa) — sem zona cinza interpretativa que os revisores tem. **Recomendacao operacional registrada:** smokes de subagents geradores futuros devem ser desenhados com criterio binario verificavel via comando do projeto.

6. **Primeiro subagent do projeto com `Write` — decisao consciente.** Geradores precisam escrever; revisores nao. Padrao operacional: **tools dos subagents seguem o que o subagent faz, nao um conjunto fixo**. Subagent revisor com `Write` seria desperdicio e risco. Subagent gerador sem `Write` seria inutil. Aplicavel a futuros subagents (ex: `migration-writer` se vier, tambem precisa `Write`; subagent `security-reviewer` se vier, segue read-only do `pr-reviewer`).
```

**Edicao 4 — Linha no historico** acima da entrada da 4.16:

```markdown
- **2026-05-12** — Sub-etapa 4.17 concluida: primeiro subagent gerador (`test-writer` Sonnet, tools com `Write`) + skill `/write-test`. Escopo focado em unit tests para `*/domain/` (subset ADR-007). Arquitetura C escolhida (1 subagent que cresce por refactor vs 3 especialistas ou 1 generalista). Sub-padrao operacional novo: gerador vs revisor (tools, output, validacao, smoke). Validacao via `./mvnw test` antes de reportar — sem loop autonomo de auto-correcao. Categoria nova: "primeira aplicacao de padrao em eixo novo". 6 licoes novas. CLAUDE.md NAO atualizado. PR #XX.
```

### Versionar este proprio prompt

`docs/prompts/prompt-etapa-4-17.md` entra como novo arquivo no Commit 4.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra squash da 4.16 (`3742a43` ou hash atual).
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompts/prompt-etapa-4-17.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `.claude/agents/pr-reviewer.md` existe (Haiku, ~140 linhas) — nao sera modificado.
- `.claude/agents/architect-reviewer.md` existe (Sonnet, ~175 linhas) — nao sera modificado.
- `.claude/skills/review-pr/SKILL.md` existe — nao sera modificado.
- `.claude/skills/review-arch/SKILL.md` existe — nao sera modificado.
- `.claude/agents/test-writer.md` NAO existe — sera criado na Tarefa 5.
- `.claude/skills/write-test/` NAO existe — sera criado na Tarefa 6.
- `src/main/java/com/laboratorio/financas/conta/domain/Conta.java` existe (referencia para smoke pos-merge — nao tocado nesta sub-etapa).
- `src/test/java/com/laboratorio/financas/conta/domain/ContaTest.java` existe (referencia de estilo — nao tocado).

Validar com:

```powershell
git status
git log --oneline -3
Test-Path .claude\agents\test-writer.md
Test-Path .claude\skills\write-test
Test-Path .claude\agents\pr-reviewer.md
Test-Path .claude\agents\architect-reviewer.md
Test-Path docs\prompts\prompt-etapa-4-17.md
git config core.hooksPath
```

**Pre-condicoes ADR-011:**

- `Test-Path docs\prompts\prompt-etapa-4-17.md` retorna `True`.
- `Test-Path .claude\agents\test-writer.md` retorna `False` (sera criado).
- `Test-Path .claude\skills\write-test` retorna `False` (sera criado).
- `Test-Path .claude\agents\pr-reviewer.md` retorna `True` (inalterado).
- `Test-Path .claude\agents\architect-reviewer.md` retorna `True` (inalterado).
- Working tree limpo exceto o prompt.

Se algum item divergir, **parar e reportar**.

## Tarefas

### Tarefa 1 — Validar pre-requisitos

Rodar comandos da secao "Estado esperado ao iniciar". Se divergir, parar e reportar.

### Tarefa 2 — Sincronizar Environment.CurrentDirectory (ADR-011)

```powershell
cd C:\projetos\financas-lab
[System.Environment]::CurrentDirectory = (Get-Location).Path
[System.Environment]::CurrentDirectory
```

Esperado: `C:\projetos\financas-lab`.

### Tarefa 3 — Criar branch

```bash
git checkout -b feat/etapa-4-17-test-writer
```

Prefixo `feat/` — adiciona componente funcional novo (terceiro subagent, primeiro gerador).

### Tarefa 4 — Antes de editar, ler arquivos vivos

```bash
cat docs/adrs.md
cat docs/decisoes-claude-code.md
cat docs/progresso.md
cat .claude/agents/architect-reviewer.md
cat .claude/skills/review-arch/SKILL.md
```

**Confirmar:**

- `adrs.md` tem ADR-007 (testes em tres niveis). Subagent `test-writer` cita ADR-007 explicitamente.
- `decisoes-claude-code.md` tem subsecao "Split do `decisoes.md` por tema (Sub-etapa 4.16)" antes de "Claude Code hooks nativos". Nova subsecao 4.17 entra **entre** essas duas.
- `progresso.md` tem "Sub-etapas concluidas" da Camada 3 em ordem descrescente comecando em 4.16. Sub-etapa 4.17 entra **acima** da 4.16.
- `progresso.md` tem "Criterios de 'pronto'" — bloco sera **substituido** pela versao ajustada 4.10+4.11+4.12+4.13+4.16+4.17.
- `progresso.md` tem "Licoes da Sub-etapa 4.16" — "Licoes da Sub-etapa 4.17" entra **acima**.
- `progresso.md` tem entrada de historico da 4.16 — linha da 4.17 entra **acima**.
- `.claude/agents/architect-reviewer.md` permanece com frontmatter `model: sonnet`. **Nao sera modificado.**
- `.claude/skills/review-arch/SKILL.md` permanece intacto. **Nao sera modificado.**

Se alguma divergencia, **parar e reportar**.

### Tarefa 5 — Criar `.claude/agents/test-writer.md`

Copiar bloco "Conteudo de `.claude/agents/test-writer.md`" do escopo. Encoding UTF-8 sem BOM. Sem acentos (excecao: em-dash U+2014 permitido em prosa Markdown do system prompt, conforme padrao consolidado em `pr-reviewer.md` e `architect-reviewer.md`).

**Pre-condicao ADR-011 apos criar:**

```powershell
Test-Path .claude\agents\test-writer.md   # True

$bytes = [System.IO.File]::ReadAllBytes(".claude/agents/test-writer.md")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191 (sem BOM)

$content = [System.IO.File]::ReadAllText(".claude/agents/test-writer.md", [System.Text.UTF8Encoding]::new($false))

# Frontmatter
$markers = @('name: test-writer', 'model: sonnet', 'tools: Read, Grep, Glob, Bash, Write')
foreach ($marker in $markers) {
    if ($content -match $marker) {
        Write-Host "Frontmatter OK: $marker"
    } else {
        Write-Host "Frontmatter AUSENTE: $marker"
    }
}

# Regras duras enumeradas no system prompt
$regras = @('JUnit 5', 'AssertJ', 'Zero Spring', 'sufixo `Test`', 'mock manual')
foreach ($regra in $regras) {
    if ($content -match [regex]::Escape($regra)) {
        Write-Host "Regra dura OK: $regra"
    } else {
        Write-Host "Regra dura AUSENTE: $regra"
    }
}

# ContaTest.java como referencia de estilo
if ($content -match 'ContaTest\.java') {
    Write-Host "Referencia estilistica OK"
} else {
    Write-Host "Referencia estilistica AUSENTE"
}

# Validacao via mvnw
if ($content -match './mvnw test') {
    Write-Host "Validacao via mvnw OK"
} else {
    Write-Host "Validacao via mvnw AUSENTE"
}

# Sem auto-correcao em loop
if ($content -match 'NAO tente auto-corrigir em loop') {
    Write-Host "Restricao auto-correcao OK"
} else {
    Write-Host "Restricao auto-correcao AUSENTE"
}

# 2 exemplos few-shot
$exemplos = ([regex]::Matches($content, '### Exemplo \d')).Count
Write-Host "Exemplos encontrados: $exemplos (esperado: 2)"

# Linhas totais (esperado: ~200-220)
$linhas = (Get-Content .claude\agents\test-writer.md).Count
Write-Host "Linhas totais: $linhas (esperado: 200-220)"
```

Se algum valor divergir, parar e reportar.

### Tarefa 6 — Criar `.claude/skills/write-test/SKILL.md`

Criar diretorio e arquivo:

```powershell
New-Item -ItemType Directory -Path .claude\skills\write-test -Force
```

Copiar bloco "Conteudo de `.claude/skills/write-test/SKILL.md`" do escopo. Encoding UTF-8 sem BOM. Sem acentos.

**Pre-condicao ADR-011 apos criar:**

```powershell
Test-Path .claude\skills\write-test\SKILL.md   # True

$bytes = [System.IO.File]::ReadAllBytes(".claude/skills/write-test/SKILL.md")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191 (sem BOM)

$content = [System.IO.File]::ReadAllText(".claude/skills/write-test/SKILL.md", [System.Text.UTF8Encoding]::new($false))

# Frontmatter completo
$markers = @('name: write-test', 'disable-model-invocation: true', 'context: fork', 'agent: test-writer', 'argument-hint: \[path-da-classe-alvo\]', 'allowed-tools:')
foreach ($marker in $markers) {
    if ($content -match $marker) {
        Write-Host "Frontmatter OK: $marker"
    } else {
        Write-Host "Frontmatter AUSENTE: $marker"
    }
}

# $ARGUMENTS presente no corpo
if ($content -match '\$ARGUMENTS') {
    Write-Host "Argumento OK"
} else {
    Write-Host "Argumento AUSENTE"
}

# Linhas totais (esperado: ~15-20)
$linhas = (Get-Content .claude\skills\write-test\SKILL.md).Count
Write-Host "Linhas totais: $linhas (esperado: 15-20)"
```

Se algum valor divergir, parar e reportar.

### Tarefa 7 — Atualizar `docs/decisoes-claude-code.md` (subsecao 4.17)

Copiar bloco "Conteudo da subsecao em decisoes-claude-code.md" do escopo. Inserir **antes** da linha `### Claude Code hooks nativos`, **apos** a subsecao "Split do `decisoes.md` por tema (Sub-etapa 4.16)".

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/decisoes-claude-code.md", [System.Text.UTF8Encoding]::new($false))

if ($content -match '### Primeiro subagent gerador') {
    Write-Host "Subsecao 4.17 OK"
} else {
    Write-Host "Subsecao 4.17 AUSENTE"
}

# Ordem: 4.16 antes da 4.17 antes de hooks nativos
$pos416 = $content.IndexOf('Split do')
$pos417 = $content.IndexOf('Primeiro subagent gerador')
$posNativos = $content.IndexOf('Claude Code hooks nativos')
if ($pos416 -lt $pos417 -and $pos417 -lt $posNativos) {
    Write-Host "Ordem OK"
} else {
    Write-Host "Ordem TROCADA"
}

# Linhas totais (esperado: ~520-560, crescimento do 466 atual)
$linhas = (Get-Content docs\decisoes-claude-code.md).Count
Write-Host "Linhas totais: $linhas"
```

**Atencao:** hook 4.4 NAO deve alertar (decisoes-claude-code.md em ~520-560 linhas, abaixo de 800).

### Tarefa 8 — Atualizar `docs/progresso.md` (4 edicoes)

Aplicar **edicoes 1-4** descritas no escopo:

1. Sub-etapa 4.17 ao topo de "Sub-etapas concluidas" (acima da 4.16).
2. Substituir bloco "Criterios de 'pronto'" da Camada 3 pela versao ajustada.
3. "Licoes da Sub-etapa 4.17" acima de "Licoes da Sub-etapa 4.16".
4. Linha de historico acima da entrada da 4.16.

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/progresso.md", [System.Text.UTF8Encoding]::new($false))

# Sub-etapa 4.17 presente
if ($content -match '4\.17.{1,10}Primeiro subagent gerador') {
    Write-Host "Sub-etapa 4.17 OK"
} else {
    Write-Host "Sub-etapa 4.17 AUSENTE"
}

# Licoes da 4.17
if ($content -match '## Li.{1,3}es da Sub-etapa 4\.17') {
    Write-Host "Licoes 4.17 OK"
} else {
    Write-Host "Licoes 4.17 AUSENTE"
}

# Criterio test-writer concluido
if ($content -match '\[x\] Subagent `test-writer`') {
    Write-Host "Criterio test-writer concluido OK"
} else {
    Write-Host "Criterio test-writer concluido AUSENTE"
}

# Criterio smoke 4.17 pendente
if ($content -match '\[ \] Smoke pos-merge da 4\.17') {
    Write-Host "Criterio smoke 4.17 pendente OK"
} else {
    Write-Host "Criterio smoke 4.17 pendente AUSENTE"
}

# Ordem cronologica
$pos417 = $content.IndexOf('**4.17')
$pos416 = $content.IndexOf('**4.16')
if ($pos417 -gt 0 -and $pos417 -lt $pos416) {
    Write-Host "Ordem cronologica OK"
} else {
    Write-Host "Ordem cronologica TROCADA"
}

# Linhas totais
$linhas = (Get-Content docs\progresso.md).Count
Write-Host "Linhas totais: $linhas"
```

**Atencao:** hook 4.4 NAO deve alertar.

### Tarefa 9 — Versionar este proprio prompt

```bash
git status
```

Confirmar que `docs/prompts/prompt-etapa-4-17.md` aparece como **untracked**. Sera incluido no Commit 4.

### Tarefa 10 — Commits (4 commits)

**Commit 1** — Subagent:

```bash
git add .claude/agents/test-writer.md
git status   # apenas test-writer.md staged
git commit -m "feat(claude): primeiro subagent gerador test-writer (Sonnet, unit tests apenas)"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 2** — Skill:

```bash
git add .claude/skills/write-test/SKILL.md
git status   # 1 arquivo staged
git commit -m "feat(claude): skill orquestradora write-test (par com test-writer)"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 3** — Docs:

```bash
git add docs/decisoes-claude-code.md docs/progresso.md
git status   # 2 arquivos staged
git commit -m "docs: sub-etapa 4.17 -- primeiro subagent gerador (test-writer) + sub-padrao gerador vs revisor"
```

**Pre-condicao ADR-011:** 2 arquivos staged; `$LASTEXITCODE = 0`.

**Atencao:** hook 4.4 NAO deve alertar.

**Commit 4** — Versionar prompt:

```bash
git add docs/prompts/prompt-etapa-4-17.md
git status   # 1 arquivo staged
git commit -m "docs: versiona prompt-etapa-4-17.md em docs/prompts/"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Validacao implicita dos hooks ativos:**

- Conventional Commits (4.1): mensagens validas.
- Encoding UTF-8 (4.2): valida bytes em todos os arquivos editados/criados.
- Markdown blank lines (4.3): valida headers em `.md` editados.
- Tamanho de docs (4.4): NAO deve alertar.
- Maven release (4.5), @Entity (4.7): nao se aplicam.

Se algum hook bloquear, parar e reportar.

### Tarefa 11 — Validacao final antes de push

```bash
git status
git log --oneline -5
git config core.hooksPath
```

```powershell
Test-Path .claude\agents\test-writer.md          # True
Test-Path .claude\skills\write-test\SKILL.md     # True
Test-Path .claude\agents\pr-reviewer.md          # True (inalterado)
Test-Path .claude\agents\architect-reviewer.md   # True (inalterado)
Test-Path .claude\skills\review-pr\SKILL.md      # True (inalterado)
Test-Path .claude\skills\review-arch\SKILL.md    # True (inalterado)
(Get-Content .claude\agents\test-writer.md).Count
(Get-Content .claude\skills\write-test\SKILL.md).Count
```

Esperado:

- Working tree limpo.
- 4 commits novos.
- `core.hooksPath` = `.githooks`.
- Componentes da 4.11/4.12 inalterados.

## Restricoes e freios

1. **NAO modificar `.claude/agents/pr-reviewer.md` ou `.claude/agents/architect-reviewer.md`.** Subagents existentes permanecem intactos.

2. **NAO modificar `.claude/skills/review-pr/SKILL.md` ou `.claude/skills/review-arch/SKILL.md`.** Skills existentes permanecem intactas.

3. **NAO criar outros subagents, skills, hooks** nesta sub-etapa. Apenas o par `test-writer` + `/write-test`.

4. **NAO atualizar `CLAUDE.md`.** Subsecao "Subagents e skills" da 4.11 ja registra o padrao generico. 4.17 aplica convencao existente em eixo novo — distincao revisor/gerador fica em `decisoes-claude-code.md`, nao em CLAUDE.md.

5. **NAO atualizar `docs/adrs.md`.** ADR-012 ja contempla padrao skill+subagent. 4.17 e refinamento taxonomico (sub-padrao gerador), nao ADR novo.

6. **NAO atualizar `docs/hooks-pendentes.md`** nesta sub-etapa. Sem novo debito gerado pela 4.17.

7. **NAO atualizar blueprint, `.gitignore`, `.gitattributes`, `docs/decisoes.md`, `docs/visao.md`, `docs/progresso-historico.md`.**

8. **Modelo do subagent: Sonnet, explicito no frontmatter.** `model: sonnet`. Nao `inherit`, nao Haiku, nao Opus.

9. **Tools do subagent: `Read, Grep, Glob, Bash, Write`.** Primeiro subagent do projeto com `Write`. Sem `Edit`, sem `Task`, sem `WebSearch`.

10. **Escopo do subagent: APENAS unit tests para classes em `*/domain/`.** Integration e E2E sao escopo de sub-etapas futuras (4.18+). NAO ampliar nesta sub-etapa.

11. **Template de output: 5 secoes** (Arquivo gerado, Cobertura, Validacao, Decisoes de design, Limitacoes conhecidas). Sem secao nova. Distinto dos revisores (3 secoes) — geradores entregam artefato + meta-informacao.

12. **`disable-model-invocation: true` na skill — obrigatorio.** Padrao consolidado pela 4.11.

13. **`context: fork` + `agent: test-writer` no frontmatter da skill — obrigatorio.**

14. **NAO executar a skill** (`/write-test`) nesta sub-etapa. Smoke pos-merge e responsabilidade do operador.

15. **NAO criar arquivo de teste real** (ex: `ContaTest.java` novo) nesta sub-etapa. Entregamos subagent + skill; smoke pos-merge e quem exercita.

16. **NAO modificar `src/`, `frontend/`, `pom.xml`, ou qualquer arquivo de codigo.** Sub-etapa entrega ferramenta de geracao, nao codigo gerado.

17. **Encoding UTF-8 sem BOM** em todos os arquivos editados/criados.

18. **Apenas ASCII no frontmatter, em strings de hooks `.ps1`, e em mensagens de commit.** Sem acentos, sem em-dash U+2014.

19. **Sem acentos** no body do subagent e da skill (alinhado com `pr-reviewer.md` e `architect-reviewer.md`).

20. **Ordem cronologica descrescente** em todos os blocos de historico de `progresso.md`.

21. **Sem cenarios destrutivos tradicionais.** Sub-etapa cria componentes novos; smoke pos-merge valida funcionamento.

22. **Hook 4.4 NAO deve alertar** em nenhum commit. Se alertar, investigar.

23. **Nao sugerir proxima sub-etapa** espontaneamente alem dos candidatos ja citados (smoke pos-merge + eventual 4.18 ampliando o subagent para integration tests).

24. **Nao tomar decisao silenciosa em zona limitrofe.** Reportar divergencias.

25. **Nao usar `pwsh`.** PowerShell 5.1.

26. **Nao usar `git reset --hard`.**

27. **Nao usar `git commit --no-verify`.**

## Estrutura de commits

Branch: `feat/etapa-4-17-test-writer`

**Commit 1** — `feat(claude): primeiro subagent gerador test-writer (Sonnet, unit tests apenas)`

- `.claude/agents/test-writer.md` (novo)

**Commit 2** — `feat(claude): skill orquestradora write-test (par com test-writer)`

- `.claude/skills/write-test/SKILL.md` (novo)

**Commit 3** — `docs: sub-etapa 4.17 -- primeiro subagent gerador (test-writer) + sub-padrao gerador vs revisor`

- `docs/decisoes-claude-code.md` (subsecao 4.17)
- `docs/progresso.md` (sub-etapa 4.17 + criterios + licoes + historico)

**Commit 4** — `docs: versiona prompt-etapa-4-17.md em docs/prompts/`

- `docs/prompts/prompt-etapa-4-17.md` (novo)

## Validacao antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -5
git config core.hooksPath
```

```powershell
Test-Path .claude\agents\test-writer.md
Test-Path .claude\skills\write-test\SKILL.md
Test-Path .claude\agents\pr-reviewer.md
Test-Path .claude\agents\architect-reviewer.md
```

Esperado:

- `check.ps1` passa.
- Working tree limpo.
- 4 commits novos.
- Componentes novos existem; componentes da 4.11/4.12 inalterados.

## PR

Titulo: `feat: sub-etapa 4.17 -- primeiro subagent gerador (test-writer Sonnet) + skill /write-test`

Body sugerido:

````markdown
## Summary

Terceiro par skill+subagent do projeto, **primeiro gerador**. Aplica padrao ADR-012 em eixo qualitativamente novo (gerador vs revisor). Entrega `test-writer` (Sonnet, tools com `Write`) + `/write-test` (skill orquestradora com argumento explicito).

### Por que esta sub-etapa existe

Camada 3 tem 2 subagents revisores validados em smoke (`pr-reviewer`, `architect-reviewer`). Categoria 4 da Camada 4 (modelo operacional, ainda a entrar) precisa de subagent **gerador** para escalar producao de testes. `test-writer` e o primeiro do projeto.

### Sub-padrao operacional novo: gerador vs revisor

Categoria distinta dentro do padrao ADR-012:

| Dimensao | Revisor (4.9, 4.12) | Gerador (4.17) |
|---|---|---|
| Tools | Read-only | `Write` incluido |
| Output | Relatorio em 3 secoes | Arquivo + relatorio em 5 secoes |
| Validacao | Sem validacao explicita | `Bash` rodando comando do projeto |
| Smoke | Tem 3 secoes? Cita ADRs? | Compila? Testes passam? |
| Risco | Baixo | Medio |

Sem ADR novo — refinamento taxonomico da ADR-012, decisao estrutural permanece. Sub-padrao registrado em `decisoes-claude-code.md` + nas licoes 4.17.

### Arquitetura escolhida: C (incremental)

Alternativas avaliadas:

- **A — 3 subagents especialistas** (`test-writer-unit`, `test-writer-integration`, `test-writer-e2e`): rejeitada por decidir estrutura antes do uso real.
- **B — 1 generalista cobrindo 3 niveis ja**: rejeitada por system prompt longo demais (300+ linhas estimadas) e risco de confundir niveis no output.
- **C (escolhida) — 1 subagent que cresce por refactor**: comeca focado em unit, amplia em sub-etapas seguintes (4.18+) se uso justificar. Coerente com "infraestrutura segue necessidade" e "estrutura emerge do uso".

### Escopo da 4.17

**APENAS unit tests para classes em `*/domain/`** (subset ADR-007).

Regras duras enumeradas no system prompt: JUnit 5, AssertJ, sem Spring, sem mock de DB, sufixo `Test`, pacote espelho, sem classe base abstract, mock manual inline (Mockito so com justificativa).

`ContaTest.java` como referencia estilistica — subagent le antes de gerar.

### Validacao + reporte (sem auto-correcao em loop)

Apos `Write`, subagent roda `./mvnw test -Dtest=<NomeDoTest>` via `Bash`:

- Compilou e passou: declara sucesso.
- Nao compilou ou falhou: **reporta erro literal e devolve decisao ao operador**.

**Sem loop autonomo de auto-correcao.** Padrao "operador soberano" preservado mesmo em subagent que escreve arquivos.

### Mudancas

- `.claude/agents/test-writer.md`: novo (~200-220 linhas). Frontmatter `model: sonnet` + `tools: Read, Grep, Glob, Bash, Write`. System prompt enumera regras duras + instrucao de consultar `ContaTest.java` + template de output em 5 secoes + 2 exemplos few-shot (caso happy + caso validacao falhando).
- `.claude/skills/write-test/SKILL.md`: novo (~15-20 linhas). Espelho da `review-arch/SKILL.md` com adaptacao de nome/agent/argumento. `argument-hint: [path-da-classe-alvo]`.
- `docs/decisoes-claude-code.md`: subsecao "Primeiro subagent gerador: test-writer + skill /write-test (Sub-etapa 4.17)" antes de "Claude Code hooks nativos".
- `docs/progresso.md`: sub-etapa 4.17 + criterios Camada 3 ajustados + 6 licoes + historico.
- `docs/prompts/prompt-etapa-4-17.md`: prompt versionado.

### Smoke test pos-merge sugerido (responsabilidade do operador)

1. Sessao nova do Claude Code.
2. **Cobaia primaria:** `Conta.java` (etapa 3.2). Comando: `/write-test src/main/java/com/laboratorio/financas/conta/domain/Conta.java`.
3. **Criterios:**
   - Skill dispara fork no `test-writer` (Sonnet).
   - Arquivo `ContaTest.java` gerado.
   - `./mvnw test -Dtest=ContaTest` compila e passa.
   - Output respeita convencoes (JUnit 5, AssertJ, sem Spring).
4. **Se primario OK:** cobaia secundaria `Transacao.java` (etapa 3.6, mais complexa).
5. **Se primario falhar:** abrir 4.17.1 (refinamento pos-smoke empirico).

### Categoria operacional nova

**"Primeira aplicacao de padrao em eixo novo".** Distinta de:

- "Primeira aplicacao" (4.11): primeira implementacao do padrao recem-decidido.
- "Replicacao de padrao consolidado" (4.12): segunda aplicacao em caso equivalente.
- **Esta:** padrao base preservado, eixo qualitativamente novo.

### CLAUDE.md NAO atualizado

Regra 4.6: convencao "subagents e skills" ja registrada na 4.11. Distincao revisor/gerador fica em `decisoes-claude-code.md`, nao em CLAUDE.md (CLAUDE.md cobre convencoes do projeto, nao taxonomia interna de subagents).

### Proximo passo

Decisao fora deste PR. Candidatos naturais:

- **Smoke pos-merge da 4.17** (responsabilidade do operador apos merge).
- **4.18** se uso justificar — ampliar `test-writer` para integration tests via refactor do mesmo subagent (categoria 4.14).
- **4.18 alternativo** — Skill sem subagent `/feature <nome>` (eixo novo: skill geradora pura).
- **4.17.1** (refactor pos-smoke) se smoke revelar suboptimo no output ou na invocacao.
````

## Pos-criacao do PR

1. Abrir PR via `gh pr create`.
2. Capturar o numero.
3. Editar `docs/decisoes-claude-code.md` e `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorizacao explicita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `feat/etapa-4-17-test-writer` empurrada com 5 commits (4 + 1 update do PR).
- PR aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.16.
- Working tree limpo.
- `.claude/agents/test-writer.md` existe (~200-220 linhas, frontmatter `model: sonnet`, tools com `Write`).
- `.claude/skills/write-test/SKILL.md` existe (~15-20 linhas).
- `.claude/agents/pr-reviewer.md`, `.claude/agents/architect-reviewer.md`, `.claude/skills/review-pr/SKILL.md`, `.claude/skills/review-arch/SKILL.md` inalterados.
- Reportar: `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup`, contagem de linhas dos componentes novos.

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao executar a skill `/write-test` (smoke e pos-merge).
- Nao criar arquivo de teste `ContaTest.java` adicional manualmente.
- Nao modificar `src/main/java/`, `frontend/`, `pom.xml`.
- Nao criar prompt da 4.18 ou 4.17.1.
- Nao criar outros subagents, skills, hooks, MCPs.
- Nao modificar componentes da 4.11/4.12 (`pr-reviewer.md`, `architect-reviewer.md`, `review-pr/SKILL.md`, `review-arch/SKILL.md`).
- Nao mexer em `~/.claude/` global.
- Nao atualizar `CLAUDE.md`, blueprint, `.gitignore`, `.gitattributes`, `hooks-pendentes.md`, `adrs.md`, `decisoes.md`, `visao.md`, `progresso-historico.md`.
- Nao sugerir proximo passo espontaneamente alem dos candidatos ja citados no PR body.
- Nao usar `pwsh`.
- Nao usar `git reset --hard`.
- Nao usar `git commit --no-verify`.
