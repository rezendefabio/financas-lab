# Prompt — Etapa 4.12: Segundo subagent `architect-reviewer` + skill `/review-arch` (replicacao do padrao ADR-012)

## Contexto

Camada 3 com 6 hooks + CLAUDE.md + blueprint + 1 subagent (`pr-reviewer` Haiku) + 1 skill orquestradora (`/review-pr`) + ADR-012 (com revisao 4.11) apos a 4.11. Padrao skill+subagent validado empiricamente:

- Smoke da 4.11 em PR #55 e PR #45: output saiu nas 3 secoes prescritas (Bloqueadores, Sugestoes, Elogios), tom alinhado com system prompt do subagent, achados ancorados em ADRs/sub-etapas reais do projeto.
- Anomalia observada: duplicacao de cabecalhos em algumas execucoes. Cosmetico, nao bloqueante.
- Pre-trabalho de analise pelo Claude principal aparece no transcript antes do output formatado do subagent. Aceito como caracteristica conhecida (sem prejuizo ao resultado final).

Esta sub-etapa entrega o **segundo subagent + segunda skill orquestradora** do projeto. Categoria nova: **"replicacao de padrao consolidado"** — distinta de "primeira aplicacao" (4.11, que descobriu mecanismo correto via doc oficial) e "descoberta" (4.10, que decidiu o padrao). Replicacao reusa forma validada.

`architect-reviewer` complementa `pr-reviewer`:

- **`pr-reviewer`** (existente): valida logica/cobertura/docs/padroes do PR especifico. Verifica "coerencia com ADRs ativos" no escopo do PR, mas superficialmente.
- **`architect-reviewer`** (novo): valida decisoes estruturais contra um **subset arquitetural duro** de ADRs — ADR-004 (Clean Architecture), ADR-005 (JWT auth), ADR-006 (Flyway migrations), ADR-007 (testes em tres niveis). Le codigo real (nao so diff) para raciocinar sobre camadas, dependencias, abstracoes. Modelo **Sonnet** (raciocinio mais profundo que Haiku permite).

Caracteristicas:

1. **Replicacao pura do padrao 4.11.** Subagent flat em `.claude/agents/architect-reviewer.md`; skill flat em `.claude/skills/review-arch/SKILL.md`; skill com `context: fork` + `agent: architect-reviewer` + `disable-model-invocation: true`; body curto. Sem refinamento de padrao — apenas aplicacao.

2. **Modelo Sonnet.** Diferente do `pr-reviewer` (Haiku). Raciocinio arquitetural exige capacidade maior. Invocacao pontual (apenas em PRs com mudanca estrutural relevante) compensa custo.

3. **Escopo focado em 4 ADRs.** ADR-001, ADR-002, ADR-003, ADR-008, ADR-009, ADR-010, ADR-011, ADR-012 sao convencoes operacionais ou stack — cobertas pelo `pr-reviewer` no escopo do PR. Duplicar entre subagents e desperdicio.

4. **Variante A (revisor de PR com foco arquitetural).** Skill: `/review-arch <pr-number>`. Argumento explicito (mesmo padrao da 4.11). Auditoria de codebase inteiro (sem argumento) descartada — escopo aberto, smoke nao-controlado, fica como sub-etapa separada se aparecer dor real.

5. **CLAUDE.md NAO atualizado.** Subsecao "Subagents e skills" da 4.11 ja registra o padrao generico. CLAUDE.md so muda quando convencao muda — 4.12 aplica convencao existente.

Quando esta etapa terminar:

- `.claude/agents/architect-reviewer.md`: segundo subagent do projeto, modelo Sonnet.
- `.claude/skills/review-arch/SKILL.md`: segunda skill orquestradora do projeto.
- `docs/decisoes.md`: subsecao 4.12 antes de "Claude Code hooks nativos".
- `docs/progresso.md`: sub-etapa 4.12 + criterios Camada 3 ajustados + licoes + historico.
- `docs/prompt-etapa-4-12.md`: versionado.

## Padroes que estreiam nesta etapa

1. **Categoria nova: "replicacao de padrao consolidado".** Distinta de:
   - "Descoberta" (4.10): identifica problema estrutural, decide padrao.
   - "Primeira aplicacao" (4.11): primeira implementacao do padrao, frequentemente revela imperfeicoes na prescricao original (ex: errata ADR-012).
   - **Replicacao:** segunda aplicacao do padrao validado. Sem refinamento — confirmacao de que padrao funciona em segundo caso. Util para fixar padrao antes de aplicacoes em escala (terceiro, quarto subagent).

2. **Subagent revisor com modelo Sonnet** (primeiro do projeto — `pr-reviewer` e Haiku). Raciocinio arquitetural exige capacidade maior. Padrao a aplicar: **Haiku para revisao de superficie (logica, padroes, encoding); Sonnet para revisao estrutural (dependencias, camadas, abstracoes)**. Modelo declarado explicitamente no frontmatter conforme licao 4.9.

3. **Bifurcacao explicita entre revisores.** Dois subagents revisores convivem: `pr-reviewer` cobre o micro (este PR), `architect-reviewer` cobre o estrutural (decisoes fundamentais). Operador invoca o adequado para o tipo de mudanca. Padrao "delegacao por especialidade" replicavel em revisores futuros (`security-reviewer`, `performance-reviewer` etc.).

## Escopo decidido (calibrado com operador antes da redacao via D1-D6)

### Arquivos modificados

| Arquivo | Tipo de mudanca |
|---|---|
| `.claude/agents/architect-reviewer.md` | Novo (segundo subagent) |
| `.claude/skills/review-arch/SKILL.md` | Novo (segunda skill orquestradora) |
| `docs/decisoes.md` | Subsecao 4.12 antes de "Claude Code hooks nativos" |
| `docs/progresso.md` | Sub-etapa 4.12 + criterios Camada 3 ajustados + licoes + historico |
| `docs/prompt-etapa-4-12.md` | Versionado (novo) |

**Nao tocados:** `.claude/agents/pr-reviewer.md`, `.claude/skills/review-pr/SKILL.md`, `.claude/hooks/`, `.githooks/`, `pom.xml`, `src/`, `frontend/`, `CLAUDE.md`, `blueprint-fabrica-ai-native.md`, `docs/adrs.md`, `docs/hooks-pendentes.md`, `.gitignore`, `.gitattributes`.

### Conteudo de `.claude/agents/architect-reviewer.md` (novo arquivo)

Encoding UTF-8 sem BOM. Sem acentos. Modelado pela estrutura do `pr-reviewer.md` (140 linhas) com adaptacao de escopo.

```markdown
---
name: architect-reviewer
description: Revisa decisoes estruturais de um PR contra os ADRs arquiteturais duros do projeto (Clean Architecture, JWT, Flyway, testes). Complementa pr-reviewer com olhar de camada/dependencia/abstracao.
tools: Read, Grep, Glob, Bash
model: sonnet
---

Voce e o `architect-reviewer` do projeto **financas-lab** — fabrica AI-native do operador Fabio. Atua como revisor arquitetural de PRs com mudanca estrutural relevante, complementando o `pr-reviewer` (que cobre o micro do PR) com analise de decisoes estruturais.

## Identidade

Revisor arquitetural senior. Foco em camadas, dependencias e abstracoes. Le codigo real (nao apenas diff) quando preciso para raciocinar sobre estrutura. Pragmatico — nao implica em estilo, implica em violacao de ADR ou em drift de camada. Tom direto, sem rodeios. Em portugues brasileiro coloquial profissional.

## O que voce VERIFICA

Subset arquitetural de ADRs — 4 ADRs duros que governam a estrutura do projeto:

1. **ADR-004 (Clean Architecture enxuta).** Camadas `domain` / `application` / `infrastructure` / `interfaces` respeitadas no bounded context modificado? Regras duras:
   - **Entidade JPA nunca e entidade de dominio.** `domain/` sem `@Entity`, `@Table`, `@Id`, `@Column`, `@JoinColumn`, `@OneToMany`, etc. Nem dependencias de `jakarta.persistence.*` ou `org.springframework.*`.
   - **Dependencias apontam para dentro:** `interfaces` -> `application` -> `domain`; `infrastructure` -> `domain`. `domain` so conhece a si mesmo e `shared/domain`.
   - **Use case = 1 classe por caso de uso** no padrao `<Verbo><Substantivo>UseCase` em `application/`.
   - **Repository pattern:** interface no `domain`, implementacao em `infrastructure`.
   - **Value Object `Money`** em `shared/domain` — operacoes retornam novo `Money`, comparacao por valor.
   - **DTOs separados:** `*Request`/`*Response` em `interfaces`, `*Command`/`*Query` em `application` (quando complexo), `*Entity` em `infrastructure`.

2. **ADR-005 (JWT stateless).** Access token 15 minutos via header `Authorization: Bearer`; refresh token 7 dias via cookie httpOnly+secure+sameSite=strict. PR que toca auth respeita esses parametros? Refresh token rotativo (uso descarta o antigo)?

3. **ADR-006 (Flyway com SQL puro).** Migrations em `src/main/resources/db/migration/` no padrao `V<N>__<descricao>.sql`. Sem `flyway.repair` em pipeline normal. Sem rollback automatico. `baseline-on-migrate: true` apenas em profiles de teste/dev (licao 2.1).

4. **ADR-007 (testes em tres niveis).** Unit (dominio puro, JUnit 5 + AssertJ, sem Spring); Integration (use case + repositorio real via Testcontainers Postgres); E2E (MockMvc + Spring Boot Test). Sufixo `Test` (singular). Classe base de teste em `src/test/java/.../shared/` com `abstract`. Niveis nao se misturam (unit nao chama Spring; integration nao usa mocks de DB).

## O que voce NAO verifica

Delegado ao `pr-reviewer`:

- Conventional Commits substancia (4.1 valida sintaxe).
- Mensagens de erro, edge cases, logica de codigo no escopo do PR.
- Cobertura de testes (alinhamento generico — voce verifica se os **tres niveis** estao respeitados; cobertura especifica e do `pr-reviewer`).
- Documentacao alinhada com mudanca (CLAUDE.md, decisoes.md, hooks-pendentes.md, progresso.md).
- Encoding UTF-8, blank lines de Markdown, tamanho de docs.

Delegado aos hooks:

- Encoding UTF-8 (4.2), Markdown blank lines (4.3), tamanho de docs (4.4), Maven release (4.5), @Entity sem migration nova/status A (4.7).

Outros ADRs (fora do subset arquitetural):

- ADR-001 (stack backend), ADR-002 (frontend), ADR-003 (PostgreSQL), ADR-008 (modelo financeiro), ADR-009 (layout `.claude/`), ADR-010 (portabilidade), ADR-011 (validacao destrutiva), ADR-012 (skill orquestradora): cobertos pelo `pr-reviewer` no escopo do PR.

Se hook ou `pr-reviewer` ja cobre, **NAO repita**. Se hook falhou, isso aparece no CI — nao e seu papel.

## Quando invocado

1. **Leia PR completo:**

   ```bash
   gh pr view <numero>
   gh pr diff <numero>
   ```

2. **Identifique escopo arquitetural:**
   - Mudancas em `src/main/java/com/laboratorio/financas/<contexto>/domain/`: foco em ADR-004 regras duras (zero anotacao Spring/JPA, dependencias para dentro).
   - Mudancas em `src/main/java/.../application/`: foco em ADR-004 (use case como classe, depende de interfaces do dominio).
   - Mudancas em `src/main/java/.../infrastructure/`: foco em ADR-004 (mapper na borda, repository concreto) + ADR-006 (migration acompanha @Entity nova).
   - Mudancas em `src/main/java/.../interfaces/`: foco em ADR-004 (DTO separado) + ADR-005 (auth se aplicavel).
   - Mudancas em `src/main/resources/db/migration/`: foco em ADR-006 (V<N>__*.sql sequencial, SQL puro).
   - Mudancas em `src/test/`: foco em ADR-007 (nivel apropriado, sufixo Test, classe base abstract).
   - Mudancas em config de auth (SecurityConfig, JwtFilter, etc.): foco em ADR-005.
   - Mudancas em `pom.xml`, `application.yml`, `application-*.yml`: foco em ADR-006 profiles + ADR-007 Testcontainers.

3. **Cruze com codigo real quando necessario:**
   - `Read` arquivos modificados para entender contexto.
   - `Grep` ou `Glob` para verificar se padrao se repete em outros contextos (drift detectado em multiplos lugares e sinal mais forte).
   - `Read` `docs/adrs.md` quando duvida sobre intencao original do ADR.

4. **Produza output estruturado** em 3 secoes (ver template abaixo).

## Template de output

**Voce DEVE usar exatamente as 3 secoes abaixo, nesta ordem, sem acrescentar outras.** Nao use "Visao Geral", "Analise", "Conclusao", "Resumo", "Recomendacao", "Itens Especificos" ou qualquer outra secao. Apenas Bloqueadores, Sugestoes, Elogios.

Emita cada cabecalho de secao exatamente uma vez. Nao repita "Revisao do PR #N" nem cabecalhos de secao em nenhuma circunstancia.

Se nada se encaixa numa secao, escreva `_Nenhum_` em italico. Nao omita a secao. Nao mude o titulo.

\`\`\`markdown
# Revisao arquitetural do PR #<numero>

## Bloqueadores

(Violacoes claras de ADR-004/005/006/007 que devem ser resolvidas antes do merge. Vazio = sem violacao estrutural.)

- **<titulo curto>** (arquivo `<path>` linha N, viola ADR-<numero>): <descricao da violacao>. Sugestao: <fix>.

## Sugestoes

(Melhorias arquiteturais opcionais. Drift leve, nomeacao inconsistente, oportunidade de extracao para `shared/domain`, etc. Operador decide acatar ou ignorar.)

- **<titulo curto>**: <descricao>. Por que: <razao arquitetural>.

## Elogios

(Decisoes estruturais bem-feitas. Camadas respeitadas, abstracao adequada, padroes consolidados aplicados.)

- <coisa boa>.
\`\`\`

## Exemplos

### Exemplo 1: PR que respeita os 4 ADRs (caso happy)

Cenario: PR adiciona bounded context `categoria` ponta-a-ponta. Estrutura: `categoria/domain/Categoria.java` (POJO sem anotacao), `categoria/application/CriarCategoriaUseCase.java` (classe, depende de `CategoriaRepository` interface do dominio), `categoria/infrastructure/CategoriaEntity.java` (@Entity), `categoria/infrastructure/CategoriaMapper.java` (MapStruct), `categoria/interfaces/CategoriaController.java` (REST). Migration `V3__categoria.sql`. Testes: `CategoriaTest` (unit), `CategoriaRepositoryIT` (integration via Testcontainers), `CategoriaControllerTest` (E2E MockMvc).

Output esperado:

\`\`\`markdown
# Revisao arquitetural do PR #34

## Bloqueadores

_Nenhum_

## Sugestoes

_Nenhum_

## Elogios

- Bounded context `categoria` segue ADR-004 enxutamente. `domain/Categoria.java` POJO puro, zero `jakarta.persistence`. Mapper na borda da infra converte `CategoriaEntity` <-> `Categoria` corretamente.
- ADR-006 cumprido: `V3__categoria.sql` em SQL puro, sequencial apos V2.
- ADR-007 com tres niveis bem-separados: unit sem Spring, integration via Testcontainers, E2E via MockMvc. Sufixo `Test` consistente.
\`\`\`

### Exemplo 2: PR que viola ADR-004 (caso problema arquitetural real)

Cenario: PR adiciona use case `RelatorioMensalUseCase` que recebe `TransacaoEntity` (entidade JPA) como input e retorna `Map<String, BigDecimal>`. Use case em `application/` usa `EntityManager` direto via `@PersistenceContext`. Sem repositorio. Teste em `src/test/.../application/RelatorioMensalUseCaseTest` usa `@SpringBootTest` + `@Autowired EntityManager`.

Output esperado:

\`\`\`markdown
# Revisao arquitetural do PR #67

## Bloqueadores

- **Entidade JPA vazando para `application`** (arquivo `src/main/java/.../relatorio/application/RelatorioMensalUseCase.java` linha 18, viola ADR-004): use case recebe `TransacaoEntity` diretamente como parametro. Regra dura do ADR-004: entidade JPA nunca atravessa para `application`/`domain`. Sugestao: criar `Transacao` no `domain/` (POJO), `TransacaoMapper` em `infrastructure/`, use case recebe `Transacao` apos conversao.
- **`@PersistenceContext` em `application`** (mesmo arquivo linha 22, viola ADR-004): camada `application` depende diretamente de `jakarta.persistence.EntityManager`. Dependencia aponta para fora (deveria apontar para `domain`). Sugestao: criar `RelatorioRepository` como interface em `domain/`, implementacao em `infrastructure/` que usa `EntityManager` ou JPA query.
- **Teste de use case usando `@SpringBootTest`** (arquivo `src/test/java/.../relatorio/application/RelatorioMensalUseCaseTest.java` linha 12, viola ADR-007): use case e camada `application` — teste deve ser unit (sem Spring, mock manual de repositorio). `@SpringBootTest` aqui mistura niveis. Sugestao: mover para `RelatorioMensalUseCaseIntegrationTest` se realmente precisar de Spring, ou reescrever sem Spring usando mock manual de `RelatorioRepository`.

## Sugestoes

- **Retorno `Map<String, BigDecimal>`** (arquivo `src/main/java/.../relatorio/application/RelatorioMensalUseCase.java` linha 12): tipo bruto perde semantica. Por que: relatorio mensal tem estrutura conhecida (mes, ano, totais por categoria). Sugestao: criar `RelatorioMensalQuery` no `application` e `RelatorioMensalResult` (record) com campos tipados. Alinhamento com padrao DTO separado de ADR-004.

## Elogios

- Use case nomeado `RelatorioMensalUseCase` no padrao `<Verbo><Substantivo>UseCase` consistente com ADR-004 padrao consolidado.
- Migration `V5__relatorio_view.sql` em SQL puro segue ADR-006.
\`\`\`

## Tom

- Direto. Sem "talvez", "considere", "seria bom" excessivos.
- Pragmatico. Decisoes do operador (override consciente, escopo reduzido vs licao original) sao respeitadas — nao reabra debates ja registrados em `decisoes.md` ou nos proprios ADRs.
- Sem julgamentos morais. Foco em consequencia tecnica e em violacao de regra dura.
- Quando o ADR for explicito ("regra dura, nao-negociavel"), use **Bloqueador**. Quando for padrao recomendado mas com porta aberta, use **Sugestao**.

## O que NAO fazer

- **Nao escreva** arquivos no projeto. Voce e read-only.
- **Nao poste** comentario no PR via `gh pr review`. Operador (humano) decide se cola seu output como comentario.
- **Nao verifique** o que `pr-reviewer` ou hooks cobrem (lista acima).
- **Nao repita** revisoes ja feitas em PRs anteriores.
- **Nao sugira** mudancas alem do escopo do PR.
- **Nao referencie** sub-etapa futura como argumento.
- **Nao critique** ADRs em si — voce valida contra eles, nao discute o merito deles. Se um ADR esta desatualizado, isso e sub-etapa propria (errata de ADR), nao seu papel sinalizar.
- **Nao revise PR sem mudanca estrutural relevante.** Se o PR e doc-only ou toca so configuracao trivial, output e tipicamente `_Nenhum_` nas 3 secoes. Nao force achados artificiais.
```

### Conteudo de `.claude/skills/review-arch/SKILL.md` (novo arquivo)

Encoding UTF-8 sem BOM. Sem acentos. Espelho exato da `review-pr/SKILL.md` com adaptacao de nome/agent/descricao.

```markdown
---
name: review-arch
description: Revisa decisoes arquiteturais de um PR via subagent architect-reviewer (Sonnet). Use em PRs com mudanca estrutural em domain/application/infrastructure/interfaces.
disable-model-invocation: true
context: fork
agent: architect-reviewer
argument-hint: [pr-number]
allowed-tools: Bash(gh pr view *) Bash(gh pr diff *)
---

Revise o PR #$ARGUMENTS seguindo todas as instrucoes do seu system prompt.

Use `gh pr view $ARGUMENTS` e `gh pr diff $ARGUMENTS` para ler o PR.

Produza output usando exatamente as 3 secoes prescritas no seu system prompt (Bloqueadores, Sugestoes, Elogios), sem acrescentar outras. Se uma secao nao tem itens, escreva `_Nenhum_` em italico.
```

### Conteudo da subsecao em `docs/decisoes.md`

Inserir **antes** da subsecao `### Claude Code hooks nativos` (mesma posicao usada pela 4.10 e 4.11):

```markdown
### Segundo subagent: architect-reviewer + skill /review-arch (Sub-etapa 4.12)

Segunda aplicacao do padrao decidido em ADR-012 (e refinado em 4.11). Marco estrutural — Camada 3 do blueprint pede 3-5 subagents focados, este e o segundo.

**Subagent (`.claude/agents/architect-reviewer.md`):**

- **Modelo: Sonnet.** Primeiro subagent do projeto com Sonnet — `pr-reviewer` (4.9) e Haiku. Padrao registrado: **Haiku para revisao de superficie (logica, padroes, encoding); Sonnet para revisao estrutural (dependencias, camadas, abstracoes)**.
- **Tools restritas:** `Read, Grep, Glob, Bash` (read-only). Igual `pr-reviewer`.
- **Escopo: subset arquitetural duro.** Valida contra ADR-004 (Clean Arch), ADR-005 (JWT), ADR-006 (Flyway), ADR-007 (testes). Demais ADRs (stack, layout, portabilidade, validacao destrutiva, etc.) sao cobertos pelo `pr-reviewer` no escopo do PR. Evita duplicacao entre subagents.
- **Template de output:** mesmas 3 secoes do `pr-reviewer` (Bloqueadores, Sugestoes, Elogios). Consistencia operacional entre subagents-revisores.
- **Exemplos few-shot:** 2 — caso happy (PR que respeita os 4 ADRs) + caso problema (PR que viola ADR-004 em multiplos pontos). Padrao consolidado pela 4.9.1 aplicado.
- **Tom prescritivo.** "DEVE usar exatamente as 3 secoes". Padrao consolidado pela 4.9.1 aplicado.

**Skill orquestradora (`.claude/skills/review-arch/SKILL.md`):**

- Espelho da `review-pr/SKILL.md` com adaptacao de nome/agent/descricao.
- `disable-model-invocation: true` + `context: fork` + `agent: architect-reviewer`. Mecanismo nativo do Claude Code (ADR-012 revisao 4.11).
- Body curto — 5 linhas apos frontmatter. System prompt do subagent ja contem toda a logica de revisao arquitetural.

**Categoria nova: "replicacao de padrao consolidado".** Distinta de:

- **"Descoberta"** (4.10): identifica problema estrutural, decide padrao via ADR.
- **"Primeira aplicacao"** (4.11): primeira implementacao do padrao, frequentemente revela imperfeicoes na prescricao original (errata ADR-012).
- **"Replicacao":** segunda aplicacao do padrao validado. Sem refinamento — confirmacao de que padrao funciona em segundo caso. Util para fixar padrao antes de aplicacoes em escala.

**Bifurcacao explicita entre revisores.** Dois subagents revisores convivem:

- `pr-reviewer` (Haiku) cobre o **micro** — este PR especifico, logica/cobertura/docs/padroes. Invocado via `/review-pr <numero>`.
- `architect-reviewer` (Sonnet) cobre o **estrutural** — decisoes fundamentais contra 4 ADRs duros. Invocado via `/review-arch <numero>`.

Operador escolhe o adequado conforme tipo de mudanca. Padrao "delegacao por especialidade" — replicavel em revisores futuros (`security-reviewer`, `performance-reviewer` etc.) sem retrabalho de forma.

**Variante A escolhida (revisor de PR com foco arquitetural).** Auditoria de codebase inteiro (`/review-arch` sem argumento) descartada — escopo aberto, smoke nao-controlado, output longo. Fica como sub-etapa separada se aparecer dor real ("preciso varrer projeto contra ADR-X").

**CLAUDE.md NAO atualizado nesta sub-etapa.** Subsecao "Subagents e skills" da 4.11 ja registra o padrao generico. CLAUDE.md so muda quando convencao muda — 4.12 aplica convencao existente.

**Smoke test pos-merge** (responsabilidade do operador apos autorizar merge):

1. Sessao nova do Claude Code.
2. Escolher PR com mudanca estrutural relevante (PRs antigos da Camada 2 que tocaram `src/main/java/.../domain/` ou `infrastructure/`, ex: PR #30 da etapa 3.2 conta-domain, PR #33 da 3.4 conta-ponta-a-ponta).
3. Invocar `/review-arch <numero>`.
4. **Criterios de sucesso:**
   - Skill dispara fork no agent `architect-reviewer` (Sonnet) — sem execucao direta pelo Claude principal.
   - Output usa exatamente as 3 secoes (Bloqueadores, Sugestoes, Elogios).
   - Achados, se houver, ancoram em ADR-004/005/006/007 nominalmente.
   - Sem violacao trivial detectada nos PRs ja mergeados (esses PRs foram revisados manualmente; `architect-reviewer` deve confirmar conformidade).
5. **Comparativo com smoke da 4.11:** se tom/estrutura forem consistentes entre `pr-reviewer` e `architect-reviewer`, padrao skill+subagent fica estabilizado por replicacao. Se houver inconsistencia, abre 4.12.1 (refinamento).
```

### Conteudo das edicoes em `docs/progresso.md`

**Edicao 1 — Sub-etapa 4.12 no topo de "Sub-etapas concluidas" da Camada 3** (acima da 4.11):

```markdown
- **4.12 — Segundo subagent `architect-reviewer` + skill `/review-arch`** (2026-05-11): segunda aplicacao do padrao ADR-012 validado pela 4.11. **Replicacao pura** — sem refinamento de padrao. Subagent em `.claude/agents/architect-reviewer.md` (flat) com modelo **Sonnet** (primeiro Sonnet do projeto — `pr-reviewer` e Haiku). Tools restritas read-only (`Read, Grep, Glob, Bash`). Escopo focado em subset arquitetural duro: ADR-004 (Clean Arch), ADR-005 (JWT), ADR-006 (Flyway), ADR-007 (testes). Demais ADRs cobertos pelo `pr-reviewer` — evita duplicacao. Skill `.claude/skills/review-arch/SKILL.md` espelho da `review-pr` com adaptacao de nome/agent. Template de output identico ao `pr-reviewer` (3 secoes). 2 exemplos few-shot: caso happy (PR que respeita os 4 ADRs) + caso problema (PR violando ADR-004 em multiplos pontos). Categoria nova: **"replicacao de padrao consolidado"**. Bifurcacao explicita entre revisores: `pr-reviewer` cobre o micro, `architect-reviewer` cobre o estrutural — operador escolhe via slash command. CLAUDE.md NAO atualizado (regra 4.6 — convencao ja registrada na 4.11). PR #XX.
```

**Edicao 2 — Ajustar criterios de "pronto" da Camada 3** (substituir bloco atual ajustado pela 4.10/4.11):

```markdown
### Criterios de "pronto" (preliminar, ajustados pela 4.10, 4.11 e 4.12)

- [x] `CLAUDE.md` do projeto escrito (target <=15KB) — concluido 4.6, atualizado 4.11
- [x] Padrao skill orquestradora -> subagent decidido — ADR-012 (4.10, revisado 4.11)
- [x] Subagent `pr-reviewer` (revisao critica antes do PR) — concluido 4.9 + refinado 4.9.1
- [x] Skill `/review-pr` orquestrando `pr-reviewer` (par com ADR-012) — concluido 4.11
- [x] Smoke pos-merge da 4.11 validando padrao skill+subagent ponta-a-ponta — validado em PR #55 + PR #45
- [x] Subagent `architect-reviewer` (Sonnet, valida decisoes contra ADR-004/005/006/007) — concluido 4.12
- [x] Skill `/review-arch` orquestrando `architect-reviewer` (par com ADR-012) — concluido 4.12
- [ ] Smoke pos-merge da 4.12 validando segundo par skill+subagent
- [ ] Subagent `test-writer` + skill `/write-test` (par ADR-012)
- [ ] Subagent (opcional) `migration-writer` + skill `/write-migration` (par ADR-012)
- [ ] Skill `/feature <nome>` (cria estrutura de bounded context)
- [ ] Skill `/ship` (lint + test + build + push + PR)
- [ ] Skill `/migrate` (gera migration + atualiza schema + escreve teste)
- [ ] Skill `/audit` (varre modulos buscando padrao especifico)
- [x] Hook pre-commit funcionando — concluido 4.1-4.7
- [ ] Hook post-edit rodando testes do arquivo mexido
- [ ] Decisao sobre plugin `code-review` oficial: manter, desativar ou reaproveitar?
```

**Edicao 3 — Bloco "Licoes da Sub-etapa 4.12"** acima de "Licoes da Sub-etapa 4.11":

```markdown
## Licoes da Sub-etapa 4.12

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — entrega subagent + skill, nao hook.)

### Licoes de ambiente

1. **Categoria nova: "replicacao de padrao consolidado".** Distinta de "descoberta" (4.10 — identifica problema, decide via ADR) e "primeira aplicacao" (4.11 — primeira implementacao, frequentemente refina prescricao). Replicacao reusa forma validada sem refinamento. Padrao operacional: a **terceira aplicacao do mesmo padrao** (ex: 4.13 com `test-writer`) provavelmente revela uma categoria de licao diferente — "consolidacao em escala" — quando padrao passa de 1 caso para N casos sem dor. Replicacao confirma; consolidacao em escala fixa.

2. **Modelo Sonnet para subagent que raciocina sobre estrutura.** `pr-reviewer` (Haiku) verifica logica/padroes superficiais; `architect-reviewer` (Sonnet) verifica camadas/dependencias/abstracoes. Diferenca de capacidade real — Haiku tende a ser superficial em raciocinio arquitetural (Clean Arch exige seguir dependencias entre arquivos, raciocinar sobre direcao de import). Padrao registrado: **Haiku para revisao de superficie, Sonnet para revisao estrutural**. Aplicavel a `security-reviewer`, `performance-reviewer` futuros conforme natureza da revisao.

3. **Bifurcacao explicita entre subagents revisores evita duplicacao.** `pr-reviewer` cobre "coerencia com ADRs ativos" no escopo do PR (verificacao de superficie); `architect-reviewer` cobre "subset arquitetural duro" com analise estrutural. Sem sobreposicao — cada subagent tem escopo mutuamente exclusivo. Operador escolhe via slash command. Padrao "delegacao por especialidade" replicavel em revisores futuros sem retrabalho de forma.

4. **Variante A (revisor de PR) vs Variante B (auditor de codebase) — escolha consciente.** Para architect-reviewer, Variante A escolhida — argumento explicito (numero do PR), escopo controlado, smoke determinavel. Variante B (audita codebase inteiro sem argumento) descartada — output longo, escopo aberto, smoke nao-controlado. Padrao operacional: subagents-revisores comecam como Variante A; auditores de codebase entram como subagents separados (ou skills sem subagent) se aparecer dor real.
```

**Edicao 4 — Linha no historico** acima da entrada da 4.11:

```markdown
- **2026-05-11** — Sub-etapa 4.12 concluida: segundo subagent `architect-reviewer` (Sonnet, valida ADR-004/005/006/007) + skill `/review-arch`. Replicacao pura do padrao 4.11 — sem refinamento. Categoria nova: "replicacao de padrao consolidado". Padrao Haiku/Sonnet registrado (Haiku para superficie, Sonnet para estrutura). Bifurcacao explicita entre `pr-reviewer` (micro) e `architect-reviewer` (estrutural). CLAUDE.md NAO atualizado (regra 4.6 — convencao ja na 4.11). 4 licoes novas. PR #XX.
```

### Versionar este proprio prompt

`docs/prompt-etapa-4-12.md` entra como novo arquivo no Commit 4.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra squash da 4.11.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-12.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `.claude/agents/pr-reviewer.md` existe (~140 linhas, `model: haiku`) — nao sera modificado.
- `.claude/skills/review-pr/SKILL.md` existe (~15 linhas) — nao sera modificado.
- `.claude/agents/architect-reviewer.md` NAO existe — sera criado na Tarefa 5.
- `.claude/skills/review-arch/` NAO existe — sera criado na Tarefa 6.

Validar com:

```powershell
git status
git log --oneline -3
Test-Path docs\prompt-etapa-4-12.md
Test-Path .claude\agents\pr-reviewer.md
Test-Path .claude\skills\review-pr\SKILL.md
Test-Path .claude\agents\architect-reviewer.md
Test-Path .claude\skills\review-arch
git config core.hooksPath
```

**Pre-condicoes ADR-011:**

- `Test-Path docs\prompt-etapa-4-12.md` retorna `True`.
- `Test-Path .claude\agents\pr-reviewer.md` retorna `True`.
- `Test-Path .claude\skills\review-pr\SKILL.md` retorna `True`.
- `Test-Path .claude\agents\architect-reviewer.md` retorna `False` (sera criado).
- `Test-Path .claude\skills\review-arch` retorna `False` (sera criado).
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
git checkout -b feat/etapa-4-12-architect-reviewer
```

Prefixo `feat/` — adiciona componente funcional (segundo subagent + segunda skill).

### Tarefa 4 — Antes de editar, ler arquivos vivos

```bash
cat docs/adrs.md
cat docs/decisoes.md
cat docs/progresso.md
cat .claude/agents/pr-reviewer.md
cat .claude/skills/review-pr/SKILL.md
```

**Confirmar:**

- `adrs.md` tem ADR-004, ADR-005, ADR-006, ADR-007 com secoes Decisao/Alternativas/Consequencias completas. ADR-012 com Revisao 4.11 anexada.
- `decisoes.md` tem subsecao "Primeira skill orquestradora: /review-pr (Sub-etapa 4.11)" antes de "Claude Code hooks nativos". Nova subsecao 4.12 entra **entre** essas duas.
- `progresso.md` tem "Sub-etapas concluidas" da Camada 3 em ordem descrescente comecando em 4.11. Sub-etapa 4.12 entra **acima** da 4.11.
- `progresso.md` tem "Criterios de 'pronto' (preliminar, ajustados pela 4.10 e 4.11)" — bloco sera **substituido** pela versao ajustada 4.10+4.11+4.12.
- `progresso.md` tem "Licoes da Sub-etapa 4.11" — "Licoes da Sub-etapa 4.12" entra **acima**.
- `progresso.md` tem entrada de historico da 4.11 — linha da 4.12 entra **acima**.
- `.claude/agents/pr-reviewer.md` permanece com frontmatter `model: haiku`. **Nao sera modificado.**
- `.claude/skills/review-pr/SKILL.md` permanece intacto. **Nao sera modificado.**

Se alguma divergencia, **parar e reportar**.

### Tarefa 5 — Criar `.claude/agents/architect-reviewer.md`

Copiar bloco "Conteudo de `.claude/agents/architect-reviewer.md`" do escopo. Encoding UTF-8 sem BOM. Sem acentos.

**Pre-condicao ADR-011 apos criar:**

```powershell
Test-Path .claude\agents\architect-reviewer.md   # True

$bytes = [System.IO.File]::ReadAllBytes(".claude/agents/architect-reviewer.md")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191 (sem BOM)

$content = [System.IO.File]::ReadAllText(".claude/agents/architect-reviewer.md", [System.Text.UTF8Encoding]::new($false))

# Frontmatter
$markers = @('name: architect-reviewer', 'model: sonnet', 'tools: Read, Grep, Glob, Bash')
foreach ($marker in $markers) {
    if ($content -match $marker) {
        Write-Host "Frontmatter OK: $marker"
    } else {
        Write-Host "Frontmatter AUSENTE: $marker"
    }
}

# Subset de ADRs presentes no system prompt
$adrs = @('ADR-004', 'ADR-005', 'ADR-006', 'ADR-007')
foreach ($adr in $adrs) {
    if ($content -match $adr) {
        Write-Host "ADR ref OK: $adr"
    } else {
        Write-Host "ADR ref AUSENTE: $adr"
    }
}

# Template prescritivo presente
if ($content -match 'DEVE usar exatamente as 3 secoes') {
    Write-Host "Tom prescritivo OK"
} else {
    Write-Host "Tom prescritivo AUSENTE"
}

# 2 exemplos few-shot
$exemplos = ([regex]::Matches($content, '### Exemplo \d')).Count
Write-Host "Exemplos encontrados: $exemplos (esperado: 2)"

# Linhas totais (esperado: ~150-170)
$linhas = (Get-Content .claude\agents\architect-reviewer.md).Count
Write-Host "Linhas totais: $linhas (esperado: 150-170)"
```

Se algum valor divergir, parar e reportar.

### Tarefa 6 — Criar `.claude/skills/review-arch/SKILL.md`

Criar diretorio e arquivo:

```powershell
New-Item -ItemType Directory -Path .claude\skills\review-arch -Force
```

Copiar bloco "Conteudo de `.claude/skills/review-arch/SKILL.md`" do escopo. Encoding UTF-8 sem BOM. Sem acentos.

**Pre-condicao ADR-011 apos criar:**

```powershell
Test-Path .claude\skills\review-arch\SKILL.md   # True

$bytes = [System.IO.File]::ReadAllBytes(".claude/skills/review-arch/SKILL.md")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191 (sem BOM)

$content = [System.IO.File]::ReadAllText(".claude/skills/review-arch/SKILL.md", [System.Text.UTF8Encoding]::new($false))

# Frontmatter completo
$markers = @('name: review-arch', 'disable-model-invocation: true', 'context: fork', 'agent: architect-reviewer', 'argument-hint: \[pr-number\]', 'allowed-tools:')
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

# Linhas totais (esperado: ~13-15)
$linhas = (Get-Content .claude\skills\review-arch\SKILL.md).Count
Write-Host "Linhas totais: $linhas (esperado: 13-15)"
```

Se algum valor divergir, parar e reportar.

### Tarefa 7 — Atualizar `docs/decisoes.md` (subsecao 4.12)

Copiar bloco "Conteudo da subsecao em decisoes.md" do escopo. Inserir **antes** da linha `### Claude Code hooks nativos`, **apos** a subsecao "Primeira skill orquestradora: /review-pr (Sub-etapa 4.11)".

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/decisoes.md", [System.Text.UTF8Encoding]::new($false))

if ($content -match '### Segundo subagent') {
    Write-Host "Subsecao 4.12 OK"
} else {
    Write-Host "Subsecao 4.12 AUSENTE"
}

# Ordem: 4.11 antes da 4.12 antes de hooks nativos
$pos411 = $content.IndexOf('Primeira skill orquestradora')
$pos412 = $content.IndexOf('Segundo subagent')
$posNativos = $content.IndexOf('Claude Code hooks nativos')
if ($pos411 -lt $pos412 -and $pos412 -lt $posNativos) {
    Write-Host "Ordem OK"
} else {
    Write-Host "Ordem TROCADA -- investigar"
}
```

### Tarefa 8 — Atualizar `docs/progresso.md` (4 edicoes)

Aplicar **edicoes 1-4** descritas no escopo, na ordem:

1. Adicionar sub-etapa 4.12 ao topo de "Sub-etapas concluidas" (acima da 4.11).
2. Substituir bloco "Criterios de 'pronto'" da Camada 3 pela versao ajustada 4.10+4.11+4.12.
3. Adicionar "Licoes da Sub-etapa 4.12" acima de "Licoes da Sub-etapa 4.11".
4. Adicionar linha de historico acima da entrada da 4.11.

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/progresso.md", [System.Text.UTF8Encoding]::new($false))

# Sub-etapa 4.12 presente
if ($content -match '4\.12.{1,5}Segundo subagent') {
    Write-Host "Sub-etapa 4.12 OK"
} else {
    Write-Host "Sub-etapa 4.12 AUSENTE"
}

# Licoes da 4.12 presentes
if ($content -match '## Licoes da Sub-etapa 4\.12') {
    Write-Host "Licoes 4.12 OK"
} else {
    Write-Host "Licoes 4.12 AUSENTE"
}

# Criterios atualizados (architect-reviewer concluido + smoke pendente)
if ($content -match 'Subagent `architect-reviewer`.{1,80}concluido 4\.12') {
    Write-Host "Criterio architect-reviewer concluido OK"
} else {
    Write-Host "Criterio architect-reviewer concluido AUSENTE"
}

if ($content -match 'Smoke pos-merge da 4\.12') {
    Write-Host "Criterio smoke pendente OK"
} else {
    Write-Host "Criterio smoke pendente AUSENTE"
}

# Ordem cronologica descrescente (4.12 acima de 4.11)
$pos412 = $content.IndexOf('**4.12')
$pos411 = $content.IndexOf('**4.11')
if ($pos412 -gt 0 -and $pos412 -lt $pos411) {
    Write-Host "Ordem cronologica OK"
} else {
    Write-Host "Ordem cronologica TROCADA"
}

# Linhas totais
$linhas = (Get-Content docs\progresso.md).Count
Write-Host "Linhas totais: $linhas"
```

**Atencao:** Hook 4.4 vai alertar — `progresso.md` cresce com cada sub-etapa. Modo warn, **nao bloqueia commit**. Considere split (criar `progresso-historico.md` arquivando licoes antigas) como debito em sub-etapa futura.

### Tarefa 9 — Versionar este proprio prompt

```bash
git status
```

Confirmar que `docs/prompt-etapa-4-12.md` aparece como **untracked**. Sera incluido no Commit 4.

### Tarefa 10 — Commits (4 commits)

**Commit 1** — Subagent:

```bash
git add .claude/agents/architect-reviewer.md
git status   # apenas 1 arquivo staged
git commit -m "feat(claude): segundo subagent architect-reviewer (Sonnet, valida ADR-004/005/006/007)"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 2** — Skill:

```bash
git add .claude/skills/review-arch/SKILL.md
git status   # 1 arquivo staged
git commit -m "feat(claude): segunda skill orquestradora review-arch (par com architect-reviewer)"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 3** — Docs:

```bash
git add docs/decisoes.md docs/progresso.md
git status   # 2 arquivos staged
git commit -m "docs: sub-etapa 4.12 -- registro de replicacao de padrao consolidado"
```

**Pre-condicao ADR-011:** 2 arquivos staged; `$LASTEXITCODE = 0`.

**Atencao:** hook 4.4 pode alertar para `progresso.md` cruzar limite. Nao bloqueia.

**Commit 4** — Versionar prompt:

```bash
git add docs/prompt-etapa-4-12.md
git status   # 1 arquivo staged
git commit -m "docs: versiona prompt-etapa-4-12.md"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Validacao implicita dos hooks ativos:**

- Conventional Commits (4.1): mensagens validas (`feat(claude):`, `docs:`).
- Encoding UTF-8 (4.2): valida bytes em todos os arquivos editados/criados.
- Markdown blank lines (4.3): valida headers em `.md` editados.
- Tamanho de docs (4.4): pode alertar em `progresso.md`.
- Maven release (4.5), @Entity (4.7): nao se aplicam.

Se algum hook bloquear, parar e reportar.

### Tarefa 11 — Validacao final antes de push

```bash
git status
git log --oneline -5
git config core.hooksPath
```

```powershell
Test-Path .claude\agents\architect-reviewer.md   # True
Test-Path .claude\skills\review-arch\SKILL.md    # True
Test-Path .claude\agents\pr-reviewer.md          # True (inalterado)
Test-Path .claude\skills\review-pr\SKILL.md      # True (inalterado)
(Get-Content .claude\agents\architect-reviewer.md).Count   # ~150-170
(Get-Content .claude\skills\review-arch\SKILL.md).Count    # ~13-15
```

Esperado:

- Working tree limpo.
- 4 commits novos.
- `core.hooksPath` = `.githooks`.
- Componentes da 4.11 inalterados.

## Restricoes e freios

1. **NAO modificar `.claude/agents/pr-reviewer.md`.** Subagent existente permanece intacto.

2. **NAO modificar `.claude/skills/review-pr/SKILL.md`.** Skill existente permanece intacta.

3. **NAO criar outros subagents, skills, hooks** nesta sub-etapa. Apenas o par `architect-reviewer` + `/review-arch`.

4. **NAO atualizar `CLAUDE.md`.** Subsecao "Subagents e skills" da 4.11 ja registra o padrao generico. 4.12 aplica convencao existente, nao muda.

5. **NAO atualizar `docs/adrs.md`.** ADR-012 ja contempla padrao skill+subagent (com revisao 4.11). 4.12 nao cria ADR novo.

6. **NAO atualizar `docs/hooks-pendentes.md`** nesta sub-etapa. Sem novo debito gerado pela 4.12.

7. **NAO atualizar blueprint, `.gitignore`, `.gitattributes`.**

8. **Modelo do subagent: Sonnet, explicito no frontmatter.** `model: sonnet`. Nao `inherit`, nao Haiku, nao Opus.

9. **Tools do subagent: `Read, Grep, Glob, Bash` (read-only).** Igual `pr-reviewer`. Sem Write, Edit, Task, WebSearch, ou qualquer tool de escrita/efeito colateral.

10. **Escopo do subagent: subset arquitetural duro.** APENAS ADR-004, ADR-005, ADR-006, ADR-007. Demais ADRs (001, 002, 003, 008, 009, 010, 011, 012) NAO sao verificados pelo `architect-reviewer` — sao cobertos pelo `pr-reviewer` ou estao fora do escopo de revisao arquitetural.

11. **Template de output: 3 secoes identicas ao `pr-reviewer`** (Bloqueadores, Sugestoes, Elogios). Sem secao nova. Consistencia operacional entre subagents-revisores.

12. **Skill espelho da `review-pr/SKILL.md`** com adaptacao APENAS de:
    - `name: review-arch`
    - `description` (foco arquitetural)
    - `agent: architect-reviewer`

13. **Body da skill: ~5 linhas apos frontmatter.** System prompt do subagent contem toda a logica de revisao — duplicar e gerar divergencia.

14. **`disable-model-invocation: true` na skill — obrigatorio.** Sem isso, skill pode ser invocada automaticamente, reintroduzindo nao-determinismo (lecao consolidada na 4.11).

15. **`context: fork` + `agent: architect-reviewer` no frontmatter da skill — obrigatorio.** Mecanismo nativo do Claude Code (ADR-012 revisao 4.11). Sem isso, replica problema da 4.9.1.

16. **NAO executar a skill** (`/review-arch`) nesta sub-etapa. Smoke pos-merge e responsabilidade do operador.

17. **Encoding UTF-8 sem BOM** em todos os arquivos editados/criados.

18. **Apenas ASCII no frontmatter e em mensagens de commit.** Sem acentos, sem em-dash U+2014.

19. **Sem acentos** no body do subagent e da skill (alinhado com restante do projeto).

20. **Ordem cronologica descrescente** em todos os blocos de historico de `progresso.md`.

21. **Sem cenarios destrutivos tradicionais.** Sub-etapa cria componentes novos; smoke pos-merge valida funcionamento. Pre-condicoes ADR-011 em cada Tarefa.

22. **Hook 4.4 vai alertar** em `progresso.md`. Comportamento esperado. **Nao bloqueia commit.**

23. **Nao sugerir proxima sub-etapa** espontaneamente alem do candidato natural ja citado no PR body (smoke pos-merge + eventual 4.13 com `test-writer`).

24. **Nao tomar decisao silenciosa em zona limitrofe.** Reportar divergencias.

25. **Nao usar `pwsh`.** PowerShell 5.1.

26. **Nao usar `git reset --hard`.**

27. **Nao usar `git commit --no-verify`.**

## Estrutura de commits

Branch: `feat/etapa-4-12-architect-reviewer`

**Commit 1** — `feat(claude): segundo subagent architect-reviewer (Sonnet, valida ADR-004/005/006/007)`

- `.claude/agents/architect-reviewer.md` (novo)

**Commit 2** — `feat(claude): segunda skill orquestradora review-arch (par com architect-reviewer)`

- `.claude/skills/review-arch/SKILL.md` (novo)

**Commit 3** — `docs: sub-etapa 4.12 -- registro de replicacao de padrao consolidado`

- `docs/decisoes.md` (subsecao 4.12)
- `docs/progresso.md` (sub-etapa 4.12 + criterios + licoes + historico)

**Commit 4** — `docs: versiona prompt-etapa-4-12.md`

- `docs/prompt-etapa-4-12.md` (novo)

## Validacao antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -5
git config core.hooksPath
```

```powershell
Test-Path .claude\agents\architect-reviewer.md
Test-Path .claude\skills\review-arch\SKILL.md
Test-Path .claude\agents\pr-reviewer.md
Test-Path .claude\skills\review-pr\SKILL.md
```

Esperado:

- `check.ps1` passa.
- Working tree limpo.
- 4 commits novos.
- Componentes novos existem; componentes da 4.11 inalterados.

## PR

Titulo: `feat: sub-etapa 4.12 -- segundo subagent architect-reviewer (Sonnet) + skill /review-arch`

Body sugerido:

````markdown
## Summary

Segunda aplicacao do padrao ADR-012 validado pela 4.11. **Replicacao pura** — sem refinamento do padrao. Entrega o par `architect-reviewer` (subagent Sonnet) + `/review-arch` (skill orquestradora).

### Por que esta sub-etapa existe

Padrao skill+subagent foi decidido na 4.10 (ADR-012), refinado via errata na 4.11, e validado empiricamente em PR #55 e PR #45. A 4.12 e a **segunda aplicacao** — confirma que o padrao funciona em segundo caso. Categoria nova: **"replicacao de padrao consolidado"**.

### `architect-reviewer` (subagent Sonnet)

Diferenciacao de escopo em relacao ao `pr-reviewer` (Haiku, 4.9):

- **`pr-reviewer`** cobre o **micro** do PR — logica, cobertura, docs, padroes, "coerencia com ADRs ativos" em verificacao de superficie.
- **`architect-reviewer`** cobre o **estrutural** — decisoes contra subset arquitetural duro: ADR-004 (Clean Architecture), ADR-005 (JWT), ADR-006 (Flyway), ADR-007 (testes).

Sem sobreposicao. Operador escolhe o adequado conforme tipo de mudanca:

- PR de feature trivial / refactor pequeno / docs / fix de bug → `/review-pr <numero>` (Haiku, barato, rapido).
- PR com mudanca em `domain/`, `application/`, `infrastructure/`, `interfaces/`, migrations, config de auth, testes → `/review-arch <numero>` (Sonnet, raciocinio estrutural).
- Mudanca arquitetural relevante em PR grande → ambos.

### Categoria nova: "replicacao de padrao consolidado"

Distinta de:

- **"Descoberta"** (4.10): identifica problema, decide padrao via ADR.
- **"Primeira aplicacao"** (4.11): primeira implementacao, frequentemente revela imperfeicoes na prescricao (errata ADR-012).
- **"Replicacao":** segunda aplicacao do padrao validado. Sem refinamento — confirmacao em segundo caso. Util para fixar padrao antes de aplicacoes em escala.

### Padrao Haiku/Sonnet registrado

- **Haiku** para revisao de superficie (logica, padroes, encoding) — `pr-reviewer`.
- **Sonnet** para revisao estrutural (dependencias, camadas, abstracoes) — `architect-reviewer`.

Aplicavel a revisores futuros conforme natureza da revisao (`security-reviewer`, `performance-reviewer`).

### Variante A escolhida

Skill `/review-arch <pr-number>` recebe argumento explicito. Variante B (auditor de codebase sem argumento) descartada — escopo aberto, smoke nao-controlado, output longo. Fica como sub-etapa separada se aparecer dor real ("preciso varrer projeto contra ADR-X").

### Mudancas

- `.claude/agents/architect-reviewer.md`: novo (~150-170 linhas). Frontmatter `model: sonnet` + `tools: Read, Grep, Glob, Bash` (read-only). System prompt com identidade, escopo (ADR-004/005/006/007), o que NAO verifica (delegado ao `pr-reviewer` e hooks), quando invocado, template prescritivo (3 secoes), 2 exemplos few-shot (caso happy + caso violacao de ADR-004), tom, restricoes.
- `.claude/skills/review-arch/SKILL.md`: novo (~13-15 linhas). Espelho da `review-pr/SKILL.md` com adaptacao de `name`, `description`, `agent`.
- `docs/decisoes.md`: subsecao "Segundo subagent: architect-reviewer + skill /review-arch (Sub-etapa 4.12)" antes de "Claude Code hooks nativos".
- `docs/progresso.md`: sub-etapa 4.12 + criterios Camada 3 ajustados + 4 licoes + historico.
- `docs/prompt-etapa-4-12.md`: prompt versionado.

### Smoke test pos-merge (responsabilidade do operador)

1. Sessao nova do Claude Code.
2. Escolher PR antigo com mudanca estrutural relevante. Sugestoes: PR #30 (3.2 conta domain), PR #33 (3.4 conta ponta-a-ponta), PR #35 (3.6 transacao domain+infra), PR #36 (3.7 transacao ponta-a-ponta).
3. Invocar `/review-arch <numero>`.
4. **Criterios de sucesso:**
   - Skill dispara fork no agent `architect-reviewer` (Sonnet) — sem execucao direta pelo Claude principal.
   - Output usa exatamente as 3 secoes (Bloqueadores, Sugestoes, Elogios).
   - Achados, se houver, ancoram em ADR-004/005/006/007 nominalmente.
   - Em PRs ja mergeados (revisados manualmente), esperado: minimas violacoes ou nenhuma.
5. **Comparativo com smoke da 4.11:** se tom/estrutura forem consistentes entre `/review-pr` e `/review-arch`, padrao skill+subagent fica estabilizado por replicacao.

### CLAUDE.md NAO atualizado

Regra 4.6: CLAUDE.md sincronizado com sub-etapa causadora da convencao. Subsecao "Subagents e skills" foi adicionada na 4.11 com convencao generica. 4.12 aplica convencao existente — nao cria nem altera convencao.

### Hook 4.4 alerta esperado

`progresso.md` continua crescendo. Modo warn, nao bloqueia. Considerar split (`progresso-historico.md` com licoes antigas) como debito.

### Proximo passo

Decisao fora deste PR. Candidatos naturais:

- **Smoke pos-merge da 4.12** (responsabilidade do operador apos merge).
- **4.13** — `test-writer` + skill `/write-test` (terceiro par skill+subagent; territorio novo: subagent que **gera codigo**, nao apenas revisa).
- **4.12.1** (refactor pos-smoke) se smoke revelar suboptimo no output ou na invocacao.
````

## Pos-criacao do PR

1. Abrir PR via `gh pr create`.
2. Capturar o numero.
3. Editar `docs/decisoes.md` e `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorizacao explicita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `feat/etapa-4-12-architect-reviewer` empurrada com 5 commits (4 + 1 update do PR).
- PR aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.11.
- Working tree limpo.
- `.claude/agents/architect-reviewer.md` existe (~150-170 linhas, frontmatter `model: sonnet`).
- `.claude/skills/review-arch/SKILL.md` existe (~13-15 linhas).
- `.claude/agents/pr-reviewer.md` e `.claude/skills/review-pr/SKILL.md` inalterados.
- Reportar: `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup`, contagem de linhas dos componentes novos.

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao executar as skills `/review-arch` ou `/review-pr` (smoke e pos-merge).
- Nao criar prompt da 4.13 ou 4.12.1.
- Nao criar outros subagents, skills, hooks, MCPs.
- Nao modificar componentes da 4.11 (`pr-reviewer.md`, `review-pr/SKILL.md`).
- Nao mexer em `~/.claude/` global.
- Nao atualizar `CLAUDE.md`, blueprint, `.gitignore`, `.gitattributes`, `hooks-pendentes.md`, `adrs.md`.
- Nao sugerir proximo passo espontaneamente alem dos candidatos ja citados no PR body.
- Nao usar `pwsh`.
- Nao usar `git reset --hard`.
- Nao usar `git commit --no-verify`.
