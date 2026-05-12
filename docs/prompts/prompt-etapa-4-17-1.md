# Prompt — Etapa 4.17.1: Refinamento pos-smoke do `test-writer` (comportamento "arquivo ja existe")

## Contexto

Camada 3 com 6 hooks + CLAUDE.md + 3 subagents (`pr-reviewer` Haiku, `architect-reviewer` Sonnet, `test-writer` Sonnet) + 3 skills orquestradoras (`/review-pr`, `/review-arch`, `/write-test`) + ADR-012 (revisao 4.11) + `progresso-historico.md` (4.13) + `decisoes-claude-code.md` (4.16) apos a 4.17. Padrao skill+subagent validado em 2 casos revisores (PRs #55, #45, #35); par gerador (4.17) com smoke parcial — vide abaixo.

A 4.17 entregou `test-writer` (primeiro subagent gerador do projeto, modelo Sonnet, tools com `Write`). Smoke pos-merge foi conduzido em `Conta.java` (etapa 3.2), mas revelou borda nao-coberta pelo system prompt: **o arquivo de teste alvo (`ContaTest.java`) ja existia no projeto, com 276 linhas e 28/28 testes passando**.

O subagent **improvisou** comportamento nao-prescrito: em vez de gerar arquivo novo (sobrescrevendo o existente) ou parar e reportar, conduziu **auditoria minuciosa** da cobertura existente — analise organizada por escopo (construtor "novo", construtor de reconstrucao, `desativar()`, `equals`/`hashCode`, `toString`), com identificacao de cobertura tautologica omitida deliberadamente. Output de alta qualidade tecnica, mas comportamento improvisado (nao prescrito no system prompt). Sinal forte para refinamento explicito.

**Resultado do smoke da 4.17 — validacao parcial:**

- ✅ Skill disparou fork no agent `test-writer` (mecanismo nativo `context: fork` funcionou).
- ✅ Output respeitou template de 5 secoes (Arquivo gerado, Cobertura, Validacao, Decisoes de design, Limitacoes).
- ✅ Validacao via `./mvnw test -Dtest=ContaTest` foi executada (28/28 passaram).
- ✅ Subagent NAO sobrescreveu trabalho existente (improvisou bem — escolha menos destrutiva entre as disponiveis).
- ❌ Geracao propriamente dita NAO foi exercitada (cobaia tinha teste pre-existente).

Inventario das classes de domain do projeto via PowerShell revelou que **todas as classes com comportamento real ja tem teste manual cuidadoso**:

- `Conta.java`, `Money.java`, `Transacao.java`, `Categoria.java` — todas com `*Test.java` correspondente em `src/test/`.
- 11 classes sem teste, mas: 3 sao repositorios (interfaces puras, sem comportamento), 4 sao exceptions (boilerplate), 3 sao enums sem metodos, 2 sao records sem logica. Nenhuma e cobaia legitima.

Smoke real de **geracao propriamente dita aguarda primeiro uso real em contexto da Camada 4** — quando feature nova trouxer classe de domain sem teste ainda.

Caracteristicas:

1. **Sub-etapa de refinamento pos-smoke empirico.** Categoria 4.9.1 — segunda aplicacao no projeto. Smoke revelou borda nao-prescrita no system prompt; sub-etapa adiciona prescricao explicita para o caso "arquivo ja existe".

2. **Mudanca cirurgica no `test-writer.md`.** Adiciona bloco "Antes de gerar: verifique se arquivo ja existe" no fluxo, mais exemplo few-shot 3 ilustrando o caso. Tudo o mais (regras duras, validacao via mvnw, restricao auto-correcao, template de 5 secoes) permanece intacto.

3. **Marca smoke 4.17 como "validacao parcial" honestamente.** Sem trapacear o registro. `progresso.md` mantem `[ ] Smoke pos-merge da 4.17` mas adiciona nota explicativa. Smoke completo aguarda primeiro uso real.

4. **Sem validacao destrutiva tradicional.** Sub-etapa modifica system prompt de subagent — comportamento empirico nao validavel em smoke desta propria sub-etapa. Validacao real vem no proximo `/write-test`.

5. **CLAUDE.md NAO atualizado.** Refinamento de comportamento de subagent nao muda convencao do projeto.

Quando esta etapa terminar:

- `.claude/agents/test-writer.md`: instrucao "arquivo ja existe" adicionada ao fluxo + exemplo few-shot 3.
- `docs/decisoes-claude-code.md`: subsecao 4.17.1 antes de "Claude Code hooks nativos".
- `docs/progresso.md`: sub-etapa 4.17.1 + nota sobre smoke 4.17 parcial + licoes + historico.
- `docs/prompts/prompt-etapa-4-17-1.md`: versionado.

## Padroes que estreiam nesta etapa

1. **Segunda aplicacao da categoria "refinamento pos-smoke empirico".** Primeira foi a 4.9.1 (refinamento do `pr-reviewer` pos-smoke). Padrao consolidado por dupla aplicacao: quando smoke revela borda nao-coberta pelo system prompt, sub-etapa cirurgica X.Y.1 adiciona prescricao explicita sem mudar o resto. Categoria distinta de "ajuste de hook por contexto novo" (4.14 — hook cumpre regra, contexto novo invalida) e "errata de auditoria meta-operacional" (4.15 — auditoria com premissa errada).

2. **Smoke parcial honestamente registrado.** Em vez de marcar `[x] Smoke 4.17` (mentira parcial) ou abandonar o registro (perda de info), padrao novo: **manter como `[ ]` com nota explicativa**. Registra o que sabemos (componentes funcionam parcialmente) e o que aguardamos (geracao real). Aplicavel a futuros smokes que tropecem em contexto que invalida validacao completa.

3. **Subagent improvisou bem — mas improvisacao precisa virar prescricao.** O smoke validou que Sonnet toma decisoes sensatas em borda nao-coberta (escolheu auditar em vez de sobrescrever). Mas confiar em improvisacao recorrente e risco — proxima invocacao pode improvisar diferente (pior). Padrao operacional: **improvisacao bem-sucedida em smoke vira prescricao em refinamento subsequente**.

## Escopo decidido (calibrado com operador antes da redacao via D1-D3)

### Arquivos modificados

| Arquivo | Tipo de mudanca |
|---|---|
| `.claude/agents/test-writer.md` | Adicionar instrucao "arquivo ja existe" + exemplo few-shot 3 |
| `docs/decisoes-claude-code.md` | Subsecao 4.17.1 antes de "Claude Code hooks nativos" |
| `docs/progresso.md` | Sub-etapa 4.17.1 + nota sobre smoke 4.17 parcial + licoes + historico |
| `docs/prompts/prompt-etapa-4-17-1.md` | Versionado (novo) |

**Nao tocados:** `.claude/agents/pr-reviewer.md`, `.claude/agents/architect-reviewer.md`, `.claude/skills/`, `.claude/hooks/`, `.githooks/`, `pom.xml`, `src/`, `frontend/`, `CLAUDE.md`, `docs/decisoes.md`, `docs/adrs.md`, `docs/visao.md`, `docs/blueprint-fabrica-ai-native.md`, `docs/progresso-historico.md`, `docs/hooks-pendentes.md`, `.gitignore`, `.gitattributes`.

### Mudancas em `.claude/agents/test-writer.md`

**Mudanca 1 — Adicionar passo "0" no fluxo "Quando invocado".**

Identificar a secao `## Quando invocado` (provavelmente perto do meio do arquivo). Inserir, **antes do passo 1 atual** ("Receba o path da classe alvo via argumento."), o seguinte:

```markdown
1. **Antes de gerar, verifique se o arquivo de teste alvo ja existe.**

   O arquivo de teste vive em `src/test/java/<espelho-do-path-da-classe-alvo>` com sufixo `Test`. Verifique:

   ```bash
   ls src/test/java/com/laboratorio/financas/<contexto>/domain/<Classe>Test.java
   ```

   **Se o arquivo existe:**
   - NAO sobrescreva.
   - Rode `./mvnw test -Dtest=<NomeDoTest>` para confirmar que o teste existente passa.
   - Reporte usando o template de output em 5 secoes, com a Secao "Arquivo gerado" indicando `**Nenhum.** Arquivo X ja existia.`
   - Na Secao "Cobertura", resuma o que o arquivo existente cobre em **maximo 3 linhas, sem bullets**. Apenas indicacao geral (ex: "Construtor com validacoes, metodos publicos, equals/hashCode, toString.").
   - Na Secao "Decisao" (substitui "Decisoes de design" neste caso), liste 2 opcoes ao operador: `(a) remover arquivo existente e re-invocar /write-test, ou (b) aceitar arquivo existente`.
   - **NAO faca analise minuciosa da cobertura existente.** Analise profunda e responsabilidade de comando `/review-test` separado (nao existe ainda; pode ser entregue em sub-etapa futura se aparecer dor real).
   - Encerre apos reportar — nao siga para os passos abaixo.

   **Se o arquivo NAO existe:** prossiga para os passos abaixo.
```

E **renumerar os passos seguintes** ("Receba o path da classe alvo" passa a ser 2, e assim por diante).

**Mudanca 2 — Adicionar exemplo few-shot 3.**

Identificar a secao `## Exemplos`. Apos o exemplo 2 ("Exemplo 2: validacao falhando"), adicionar:

```markdown
### Exemplo 3: arquivo de teste ja existe (caso decidido pela 4.17.1)

Cenario: operador invoca `/write-test src/main/java/com/laboratorio/financas/conta/domain/Conta.java`. Subagent verifica `src/test/java/com/laboratorio/financas/conta/domain/ContaTest.java` — arquivo existe (276 linhas, 28/28 testes passando).

Output esperado:

\`\`\`markdown
# Test-writer para Conta

## Arquivo gerado

**Nenhum.** Arquivo `src/test/java/com/laboratorio/financas/conta/domain/ContaTest.java` ja existia.

## Cobertura

Construtor com validacoes, metodos publicos, equals/hashCode, toString. Cobertura existente parece abrangente.

## Validacao

- **Compilacao do existente:** ✅
- **Execucao do existente:** 28/28 testes passaram
- **Comando:** `./mvnw test -Dtest=ContaTest`

## Decisao

Nao sobrescrevi. Operador decide:
(a) remover arquivo existente e re-invocar `/write-test` para gerar novo,
(b) aceitar arquivo existente.

## Limitacoes conhecidas

_Nenhuma_ (sub-etapa entrega so checagem de existencia + validacao do existente; analise minuciosa fora do escopo).
\`\`\`
```

**Mudanca 3 — Atualizar restricoes "O que NAO fazer".**

Identificar a secao `## O que NAO fazer`. Apos a restricao "NAO use Mockito em unit test puro de dominio" (ou ultima restricao da lista), adicionar:

```markdown
- **NAO faca analise minuciosa de cobertura quando arquivo de teste ja existe.** Resumo em ate 3 linhas, sem bullets. Analise profunda da cobertura e responsabilidade de comando separado (`/review-test` se entregue no futuro), nao do `test-writer`.
- **NAO sobrescreva arquivo de teste pre-existente.** Padrao decidido pela 4.17.1 apos smoke parcial da 4.17: sobrescrita destrutiva e perigosa (perde teste manual cuidadoso). Subagent para, reporta presenca + status, devolve decisao ao operador.
```

### Conteudo da subsecao em `docs/decisoes-claude-code.md`

Inserir **antes** da subsecao `### Claude Code hooks nativos`, **apos** "Primeiro subagent gerador: test-writer + skill /write-test (Sub-etapa 4.17)":

```markdown
### Refinamento pos-smoke do test-writer: comportamento "arquivo ja existe" (Sub-etapa 4.17.1)

Sub-etapa de **refinamento pos-smoke empirico** — segunda aplicacao da categoria 4.9.1. Smoke da 4.17 (conduzido em `Conta.java`) revelou borda nao-coberta pelo system prompt do `test-writer`: o arquivo de teste alvo (`ContaTest.java`) ja existia no projeto, com cobertura cuidadosa manual.

**Comportamento improvisado no smoke da 4.17:**

Subagent percebeu que `ContaTest.java` ja existia, decidiu nao sobrescrever, e conduziu **auditoria minuciosa** da cobertura existente — analise organizada por escopo (construtor "novo", construtor de reconstrucao, metodos, equals/hashCode, toString), com identificacao de cobertura tautologica omitida deliberadamente. Output tecnicamente de alta qualidade, **mas comportamento improvisado** (nao prescrito no system prompt).

**Por que improvisacao precisa virar prescricao:**

Smoke validou que Sonnet toma decisoes sensatas em borda nao-coberta (escolheu auditar em vez de sobrescrever destrutivamente — opcao menos destrutiva entre as disponiveis). Mas confiar em improvisacao recorrente e risco — proxima invocacao pode improvisar diferente (pior — ex: sobrescrever sem confirmar). Padrao operacional: **improvisacao bem-sucedida em smoke vira prescricao em refinamento subsequente**.

**Prescricao adicionada ao system prompt:**

Passo "0" inserido no fluxo (renumera demais para 2+):

> Antes de gerar, verifique se o arquivo de teste alvo ja existe. Se existir: NAO sobrescreva. Rode `./mvnw test -Dtest=<NomeDoTest>` para confirmar que o existente passa. Reporte usando template padrao (Arquivo gerado: "Nenhum"). Cobertura: resumo em ate 3 linhas, sem bullets. Decisao: 2 opcoes ao operador — (a) remover arquivo e re-invocar, ou (b) aceitar existente. NAO faca analise minuciosa de cobertura.

**Razao da restricao "max 3 linhas, sem bullets" no resumo:**

Analise minuciosa e responsabilidade de comando separado (`/review-test` se entregue no futuro), nao do `test-writer`. Geradores entregam artefato + meta-informacao curta; revisores entregam analise estruturada. Manter limite de escopo entre eles.

**Exemplo few-shot 3 adicionado** ilustrando "arquivo ja existe":

- Cenario: invocacao em `Conta.java`, `ContaTest.java` existe com 28/28 testes passando.
- Output: 5 secoes do template, Secao "Arquivo gerado" indica "Nenhum", Secao "Cobertura" em 1 linha curta, Secao "Decisao" lista as 2 opcoes.

**2 restricoes novas em "O que NAO fazer":**

- NAO faca analise minuciosa de cobertura quando arquivo de teste ja existe (resumo em ate 3 linhas, sem bullets).
- NAO sobrescreva arquivo de teste pre-existente.

**Smoke da 4.17 mantido como "validacao parcial" honestamente.** `progresso.md` mantem `[ ] Smoke pos-merge da 4.17` com nota explicativa: subagent invocado via fork OK, template OK, validacao via mvnw OK, mas geracao propriamente dita nao exercitada. Smoke completo aguarda primeiro uso real em contexto da Camada 4 (quando feature nova trouxer classe de domain sem teste ainda).

**Padrao operacional novo: smoke parcial registrado honestamente.** Em vez de marcar `[x]` (mentira parcial) ou abandonar (perda de info), padrao "manter como `[ ]` com nota explicativa" formalizado. Aplicavel a futuros smokes que tropecem em contexto que invalida validacao completa — inventario empirico das 11 classes de domain sem teste no projeto (todas eram boilerplate: interfaces, exceptions, enums, records sem logica) confirmou que cobaia legitima exigiria classe nova com comportamento real, que so virá na Camada 4.

**CLAUDE.md NAO atualizado nesta sub-etapa.** Refinamento de comportamento de subagent nao muda convencao do projeto. Regra 4.6 preservada.

**Categoria operacional consolidada por dupla aplicacao: "refinamento pos-smoke empirico".** Primeira foi a 4.9.1 (refinamento do `pr-reviewer` pos-smoke). Distinta de:

- **"Patch tecnico"** (4.0.1): corrige bug do entregue.
- **"Ajuste de hook por contexto novo"** (4.14): hook cumpre regra, contexto invalida.
- **"Errata de auditoria meta-operacional"** (4.15): auditoria com premissa errada.
- **Esta categoria:** smoke empirico revela borda nao-coberta pelo system prompt; sub-etapa cirurgica adiciona prescricao explicita sem mudar o resto do componente. Padrao replicavel para qualquer subagent ou skill futuro cujo smoke revele borda similar.
```

### Conteudo das edicoes em `docs/progresso.md`

**Edicao 1 — Sub-etapa 4.17.1 no topo de "Sub-etapas concluidas" da Camada 3** (acima da 4.17):

```markdown
- **4.17.1 — Refinamento pos-smoke do `test-writer`: comportamento "arquivo ja existe"** (2026-05-12): segunda aplicacao da categoria "refinamento pos-smoke empirico" (primeira foi a 4.9.1). Smoke da 4.17 (conduzido em `Conta.java`) revelou que o arquivo de teste alvo (`ContaTest.java`) ja existia com cobertura manual cuidadosa; subagent improvisou auditoria minuciosa (output de alta qualidade tecnica, mas comportamento nao-prescrito). Inventario das 11 classes de domain sem teste no projeto confirmou que **nenhuma e cobaia legitima** (interfaces de repositorio, exceptions, enums, records sem logica). Sub-etapa adiciona prescricao explicita: passo "0" no fluxo verificando se arquivo existe; se existe, subagent para, valida via mvnw, reporta com cobertura resumida em max 3 linhas sem bullets, lista 2 opcoes ao operador. Exemplo few-shot 3 ilustra. 2 restricoes novas em "O que NAO fazer". Smoke da 4.17 mantido como **validacao parcial** honestamente (criterio nao trapaceado): componentes funcionam parcialmente, geracao real aguarda primeiro uso na Camada 4. CLAUDE.md NAO atualizado. PR #XX.
```

**Edicao 2 — Atualizar criterios da Camada 3.**

Substituir bloco atual de "Criterios de 'pronto'" (ajustado pela 4.17). Mudanca principal: linha `[ ] Smoke pos-merge da 4.17` ganha nota explicativa sobre validacao parcial.

```markdown
### Criterios de "pronto" (preliminar, ajustados pela 4.10, 4.11, 4.12, 4.13, 4.16, 4.17 e 4.17.1)

- [x] `CLAUDE.md` do projeto escrito (target <=15KB) -- concluido 4.6, atualizado 4.11, 4.13 e 4.16
- [x] Padrao skill orquestradora -> subagent decidido -- ADR-012 (4.10, revisado 4.11)
- [x] Subagent `pr-reviewer` (revisao critica antes do PR) -- concluido 4.9 + refinado 4.9.1
- [x] Skill `/review-pr` orquestrando `pr-reviewer` (par com ADR-012) -- concluido 4.11
- [x] Smoke pos-merge da 4.11 validando padrao skill+subagent ponta-a-ponta -- validado em PR #55 + PR #45
- [x] Subagent `architect-reviewer` (Sonnet, valida decisoes contra ADR-004/005/006/007) -- concluido 4.12
- [x] Skill `/review-arch` orquestrando `architect-reviewer` (par com ADR-012) -- concluido 4.12
- [x] Smoke pos-merge da 4.12 validando segundo par skill+subagent -- validado em PR #35
- [x] Subagent `test-writer` + skill `/write-test` (par ADR-012, primeiro gerador) -- concluido 4.17 (escopo: unit tests; integration/E2E em 4.18+ se justificar). Refinado pela 4.17.1 com prescricao "arquivo ja existe".
- [ ] Smoke pos-merge da 4.17 validando primeiro par skill+subagent gerador -- **validacao parcial em 2026-05-12** (subagent invocado via fork OK, template OK, validacao via mvnw OK; geracao propriamente dita NAO exercitada — cobaia `Conta` tinha teste pre-existente; subagent corretamente decidiu nao sobrescrever, comportamento prescrito pela 4.17.1). Smoke completo aguarda primeiro uso real em contexto da Camada 4.
- [ ] Subagent (opcional) `migration-writer` + skill `/write-migration` (par ADR-012)
- [ ] Skill `/feature <nome>` (cria estrutura de bounded context)
- [ ] Skill `/ship` (lint + test + build + push + PR)
- [ ] Skill `/migrate` (gera migration + atualiza schema + escreve teste)
- [ ] Skill `/audit` (varre modulos buscando padrao especifico)
- [x] Hook pre-commit funcionando -- concluido 4.1-4.7, refinado 4.14
- [ ] Hook post-edit rodando testes do arquivo mexido
- [ ] Decisao sobre plugin `code-review` oficial: manter, desativar ou reaproveitar? (Re-classificado 4.15: nao e debito do projeto, e decisao pessoal do operador sobre setup Claude Code.)
```

**Edicao 3 — Bloco "Licoes da Sub-etapa 4.17.1"** acima de "Licoes da Sub-etapa 4.17":

```markdown
## Licoes da Sub-etapa 4.17.1

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — refinamento de subagent.)

### Licoes de ambiente

1. **Categoria "refinamento pos-smoke empirico" consolidada por dupla aplicacao.** Primeira foi 4.9.1 (refinamento do `pr-reviewer` pos-smoke). Agora 4.17.1 (refinamento do `test-writer` pos-smoke). Padrao operacional: smoke revela borda nao-prescrita; sub-etapa cirurgica X.Y.1 adiciona prescricao explicita sem mudar o resto do componente. Categoria distinta de patch tecnico (4.0.1), ajuste de hook por contexto novo (4.14), errata de auditoria meta-operacional (4.15). **Recomendacao operacional firmada por dupla aplicacao:** quando smoke revela comportamento improvisado bem-sucedido, formalizar a improvisacao em refinamento subsequente — confiar em improvisacao recorrente e risco.

2. **Smoke parcial registrado honestamente: padrao novo.** Em vez de marcar `[x]` (mentira parcial) ou abandonar (perda de info), padrao formalizado: **manter como `[ ]` com nota explicativa** indicando o que foi validado e o que aguarda. Aplicavel a futuros smokes que tropecem em contexto que invalida validacao completa. **Disciplina de registro fiel** e valor por si — agentes em sessoes futuras leem o estado real, nao uma narrativa aspiracional.

3. **Inventario empirico revelou que projeto nao tinha cobaia legitima.** As 11 classes de domain sem teste no `financas-lab` eram boilerplate (interfaces de repositorio, exceptions, enums sem metodos, records sem logica) — nenhuma com comportamento real testavel. Forcar smoke em uma delas teria gerado teste tautologico (validando JVM ou constantes). **Licao operacional consolidada:** antes de calibrar smoke de subagent gerador, **auditar se projeto tem cobaia natural** (classe com comportamento real sem teste manual). Se nao tiver, smoke completo aguarda primeiro uso real — nao forcar cobaia artificial.

4. **Calibracao da 4.17 tinha lacuna que so apareceu no smoke.** Eu (assistente) deveria ter perguntado, antes de calibrar, "existe classe de domain com comportamento real sem teste manual?". Nao perguntei. Lacuna estava em D3 (smoke pos-merge). Padrao operacional registrado: **na calibracao de subagent gerador, incluir explicitamente "existe cobaia natural?" como pergunta antes de prescrever smoke**. Replicavel a futuros subagents geradores (ex: `migration-writer` exigira "existe migration que falta gerar?").

5. **Output da auditoria foi de alta qualidade — Sonnet entrega bem em analise estruturada, independente da funcao formal do subagent.** O `test-writer` improvisou comportamento de revisor e produziu analise minuciosa comparavel ao que `architect-reviewer` produziria. Sinal de que Sonnet tem competencia transversal entre funcoes. Mas **isso reforca a necessidade de prescrever escopo claramente** — sem restricao explicita, subagent pode "ajudar demais" e invadir territorio de outro componente (revisor). Restricao "max 3 linhas, sem bullets" no resumo da 4.17.1 e exatamente para evitar essa invasao.

6. **Anomalia conhecida do `context: fork` apareceu de novo: duplicacao de bullets.** Output do smoke 4.17 teve "equals e hashCode:" repetido com bullet. Mesma anomalia observada em smokes 4.11 e 4.12. Confirmado em **3 smokes diferentes com 3 subagents diferentes** — caracteristica sistematica do mecanismo de invocacao via fork, nao especifica de subagent ou modelo. Aceito como caracteristica conhecida; nao bloqueante. Tentativa de mitigar via instrucao prescritiva mais forte tem baixa probabilidade de sucesso — Claude principal (que emite cabecalho antes do fork) nao e controlado pelo system prompt do subagent.
```

**Edicao 4 — Linha no historico** acima da entrada da 4.17:

```markdown
- **2026-05-12** — Sub-etapa 4.17.1 concluida (refinamento pos-smoke empirico): refinamento do `test-writer.md` com prescricao explicita para "arquivo ja existe". Smoke da 4.17 revelou que cobaia (`Conta.java`) tinha teste pre-existente — subagent improvisou auditoria minuciosa, bem mas nao-prescrita. Sub-etapa adiciona passo "0" no fluxo (verifica existencia antes de gerar; se existe, para, reporta com resumo em max 3 linhas, lista 2 opcoes ao operador). Exemplo few-shot 3 ilustra. 2 restricoes novas. Inventario empirico confirmou que projeto nao tem cobaia legitima (11 classes sem teste sao boilerplate). Smoke 4.17 mantido como **validacao parcial** honestamente — smoke completo aguarda primeiro uso real na Camada 4. Categoria "refinamento pos-smoke empirico" consolidada por dupla aplicacao (apos 4.9.1). 6 licoes novas. PR #XX.
```

### Versionar este proprio prompt

`docs/prompts/prompt-etapa-4-17-1.md` entra como novo arquivo no Commit 4.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra squash da 4.17.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompts/prompt-etapa-4-17-1.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `.claude/agents/test-writer.md` existe (~158 linhas pos-4.17). Sera modificado nesta sub-etapa.
- `docs/decisoes-claude-code.md` tem subsecao "Primeiro subagent gerador: test-writer + skill /write-test (Sub-etapa 4.17)" antes de "Claude Code hooks nativos".

Validar com:

```powershell
git status
git log --oneline -3
git config core.hooksPath
Test-Path .claude\agents\test-writer.md
Test-Path docs\prompts\prompt-etapa-4-17-1.md
(Get-Content .claude\agents\test-writer.md).Count
```

**Pre-condicoes ADR-011:**

- `Test-Path .claude\agents\test-writer.md` retorna `True`.
- `Test-Path docs\prompts\prompt-etapa-4-17-1.md` retorna `True`.
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
git checkout -b refactor/etapa-4-17-1-arquivo-ja-existe
```

Prefixo `refactor/` — analogo a 4.14 (refactor de componente existente). Categoria "refinamento pos-smoke empirico" usa prefixo `refactor/` quando modifica componente entregue.

### Tarefa 4 — Antes de editar, ler arquivos vivos

```bash
cat .claude/agents/test-writer.md
cat docs/decisoes-claude-code.md
cat docs/progresso.md
```

**Confirmar e anotar:**

- `test-writer.md`: anotar **linha exata onde comeca a secao `## Quando invocado`**. Novo passo "0" entra antes do passo 1 atual.
- `test-writer.md`: anotar **linha exata onde comeca a secao `## Exemplos`** e **onde termina o Exemplo 2**. Exemplo 3 entra logo apos.
- `test-writer.md`: anotar **linha exata onde comeca a secao `## O que NAO fazer`** e **onde termina (ultimo bullet da lista)**. 2 restricoes novas entram apos o ultimo bullet existente.
- `decisoes-claude-code.md`: tem subsecao "Primeiro subagent gerador: test-writer + skill /write-test (Sub-etapa 4.17)" antes de "Claude Code hooks nativos". Nova subsecao 4.17.1 entra **entre** essas duas.
- `progresso.md`: tem "Sub-etapas concluidas" da Camada 3 em ordem descrescente comecando em 4.17. Sub-etapa 4.17.1 entra **acima** da 4.17.
- `progresso.md`: tem "Criterios de 'pronto'" — bloco sera **substituido** pela versao ajustada 4.10+4.11+4.12+4.13+4.16+4.17+4.17.1.
- `progresso.md`: tem "Licoes da Sub-etapa 4.17" — "Licoes da Sub-etapa 4.17.1" entra **acima**.
- `progresso.md`: tem entrada de historico da 4.17 — linha da 4.17.1 entra **acima**.

Se alguma divergencia, **parar e reportar**.

### Tarefa 5 — Modificar `.claude/agents/test-writer.md` (3 mudancas)

Aplicar **Mudancas 1, 2, 3** descritas no escopo:

1. Adicionar passo "0" no inicio do fluxo `## Quando invocado` (renumerar demais).
2. Adicionar Exemplo 3 apos o Exemplo 2 na secao `## Exemplos`.
3. Adicionar 2 restricoes novas no final da secao `## O que NAO fazer`.

**Restricoes da edicao:**

- Encoding UTF-8 sem BOM preservado.
- Sem acentos no body (alinhado com `pr-reviewer.md` e `architect-reviewer.md`). Em-dash U+2014 permitido em prosa Markdown (padrao consolidado).
- Renumeracao do fluxo "Quando invocado": passos 1-8 originais viram 2-9.

**Pre-condicao ADR-011 apos editar:**

```powershell
Test-Path .claude\agents\test-writer.md   # True

$bytes = [System.IO.File]::ReadAllBytes(".claude/agents/test-writer.md")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191 (sem BOM)

$content = [System.IO.File]::ReadAllText(".claude/agents/test-writer.md", [System.Text.UTF8Encoding]::new($false))

# Passo "0" adicionado
if ($content -match 'Antes de gerar, verifique se o arquivo de teste alvo ja existe') {
    Write-Host "Passo 0 OK"
} else {
    Write-Host "Passo 0 AUSENTE"
}

# Exemplo 3 adicionado
if ($content -match '### Exemplo 3:.{1,30}arquivo de teste ja existe') {
    Write-Host "Exemplo 3 OK"
} else {
    Write-Host "Exemplo 3 AUSENTE"
}

# 3 exemplos few-shot totais
$exemplos = ([regex]::Matches($content, '### Exemplo \d')).Count
Write-Host "Exemplos encontrados: $exemplos (esperado: 3)"

# Restricao "NAO faca analise minuciosa"
if ($content -match 'NAO faca analise minuciosa de cobertura') {
    Write-Host "Restricao analise minuciosa OK"
} else {
    Write-Host "Restricao analise minuciosa AUSENTE"
}

# Restricao "NAO sobrescreva arquivo pre-existente"
if ($content -match 'NAO sobrescreva arquivo de teste pre-existente') {
    Write-Host "Restricao sobrescrita OK"
} else {
    Write-Host "Restricao sobrescrita AUSENTE"
}

# Regras duras originais ainda presentes (amostra)
$regras = @('JUnit 5', 'AssertJ', 'Zero Spring', 'sufixo.{1,3}Test')
foreach ($r in $regras) {
    if ($content -match $r) {
        Write-Host "Regra original preservada OK: $r"
    } else {
        Write-Host "ERRO: regra original removida: $r"
    }
}

# Restricao auto-correcao preservada
if ($content -match 'NAO tente auto-corrigir em loop') {
    Write-Host "Restricao auto-correcao preservada OK"
} else {
    Write-Host "ERRO: restricao auto-correcao removida"
}

# Linhas totais (esperado: ~190-210, crescimento de ~32-52 sobre 158)
$linhas = (Get-Content .claude\agents\test-writer.md).Count
Write-Host "Linhas totais: $linhas (esperado: 190-210)"
```

Se algum valor divergir, parar e reportar.

### Tarefa 6 — Atualizar `docs/decisoes-claude-code.md` (subsecao 4.17.1)

Copiar bloco "Conteudo da subsecao em decisoes-claude-code.md" do escopo. Inserir **antes** da linha `### Claude Code hooks nativos`, **apos** "Primeiro subagent gerador: test-writer + skill /write-test (Sub-etapa 4.17)".

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/decisoes-claude-code.md", [System.Text.UTF8Encoding]::new($false))

if ($content -match '### Refinamento pos-smoke do test-writer') {
    Write-Host "Subsecao 4.17.1 OK"
} else {
    Write-Host "Subsecao 4.17.1 AUSENTE"
}

# Ordem: 4.17 antes da 4.17.1 antes de hooks nativos
$pos417 = $content.IndexOf('Primeiro subagent gerador')
$pos417_1 = $content.IndexOf('Refinamento pos-smoke do test-writer')
$posNativos = $content.IndexOf('Claude Code hooks nativos')
if ($pos417 -lt $pos417_1 -and $pos417_1 -lt $posNativos) {
    Write-Host "Ordem OK"
} else {
    Write-Host "Ordem TROCADA"
}

# Hook 4.4 NAO deve alertar (decisoes-claude-code.md em ~570-600 linhas, abaixo de 800)
$linhas = (Get-Content docs\decisoes-claude-code.md).Count
Write-Host "Linhas totais: $linhas"
```

### Tarefa 7 — Atualizar `docs/progresso.md` (4 edicoes)

Aplicar **edicoes 1-4** descritas no escopo:

1. Sub-etapa 4.17.1 ao topo de "Sub-etapas concluidas" (acima da 4.17).
2. Substituir bloco "Criterios de 'pronto'" da Camada 3 pela versao ajustada (smoke 4.17 com nota explicativa).
3. "Licoes da Sub-etapa 4.17.1" acima de "Licoes da Sub-etapa 4.17".
4. Linha de historico acima da entrada da 4.17.

**Pre-condicao ADR-011 apos editar:**

```powershell
$content = [System.IO.File]::ReadAllText("docs/progresso.md", [System.Text.UTF8Encoding]::new($false))

# Sub-etapa 4.17.1 presente
if ($content -match '4\.17\.1.{1,15}Refinamento') {
    Write-Host "Sub-etapa 4.17.1 OK"
} else {
    Write-Host "Sub-etapa 4.17.1 AUSENTE"
}

# Licoes da 4.17.1
if ($content -match '## Li.{1,3}es da Sub-etapa 4\.17\.1') {
    Write-Host "Licoes 4.17.1 OK"
} else {
    Write-Host "Licoes 4.17.1 AUSENTE"
}

# Nota sobre smoke 4.17 parcial
if ($content -match 'Smoke pos-merge da 4\.17.{1,200}validacao parcial') {
    Write-Host "Nota smoke parcial OK"
} else {
    Write-Host "Nota smoke parcial AUSENTE"
}

# Ordem cronologica
$pos417_1 = $content.IndexOf('**4.17.1')
$pos417 = $content.IndexOf('**4.17')
if ($pos417_1 -gt 0 -and $pos417_1 -lt $pos417) {
    Write-Host "Ordem cronologica OK"
} else {
    Write-Host "Ordem cronologica TROCADA"
}

# Hook 4.4 NAO deve alertar
$linhas = (Get-Content docs\progresso.md).Count
Write-Host "Linhas totais: $linhas"
```

### Tarefa 8 — Commits (4 commits)

**Commit 1** — Refinamento do subagent:

```bash
git add .claude/agents/test-writer.md
git status   # apenas test-writer.md staged
git commit -m "refactor(claude): test-writer com prescricao explicita para arquivo ja existe (sub-etapa 4.17.1)"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 2** — Decisoes:

```bash
git add docs/decisoes-claude-code.md
git status   # apenas decisoes-claude-code.md staged
git commit -m "docs: subsecao 4.17.1 -- refinamento pos-smoke empirico do test-writer"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 3** — Progresso:

```bash
git add docs/progresso.md
git status   # apenas progresso.md staged
git commit -m "docs: sub-etapa 4.17.1 -- registra refinamento + smoke 4.17 como validacao parcial"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Atencao:** hook 4.4 NAO deve alertar.

**Commit 4** — Versionar prompt:

```bash
git add docs/prompts/prompt-etapa-4-17-1.md
git status   # apenas prompt-etapa-4-17-1.md staged
git commit -m "docs: versiona prompt-etapa-4-17-1.md em docs/prompts/"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Atencao:** hook 4.4 NAO deve alertar (prompt em `docs/prompts/`, isento desde 4.14).

**Validacao implicita dos hooks ativos:**

- Conventional Commits (4.1): mensagens validas.
- Encoding UTF-8 (4.2): valida bytes em todos os arquivos editados/criados.
- Markdown blank lines (4.3): valida headers em `.md` editados.
- Tamanho de docs (4.4): NAO deve alertar.

Se algum hook bloquear, parar e reportar.

### Tarefa 9 — Validacao final antes de push

```bash
git status
git log --oneline -5
git config core.hooksPath
```

```powershell
Test-Path .claude\agents\test-writer.md          # True
(Get-Content .claude\agents\test-writer.md).Count
Test-Path .claude\agents\pr-reviewer.md          # True (inalterado)
Test-Path .claude\agents\architect-reviewer.md   # True (inalterado)
Test-Path .claude\skills\write-test\SKILL.md     # True (inalterado)
```

Esperado:

- Working tree limpo.
- 4 commits novos.
- `core.hooksPath` = `.githooks`.
- Componentes da 4.11/4.12/4.17 (skill `/write-test`) inalterados.

## Restricoes e freios

1. **NAO modificar `.claude/agents/pr-reviewer.md` ou `.claude/agents/architect-reviewer.md`.** Subagents revisores permanecem intactos.

2. **NAO modificar `.claude/skills/write-test/SKILL.md`.** Skill `/write-test` permanece intacta — refinamento e do system prompt do subagent, nao da skill.

3. **NAO modificar `.claude/skills/review-pr/SKILL.md`, `.claude/skills/review-arch/SKILL.md`, `.claude/hooks/`, `.githooks/`.**

4. **NAO criar subagents, skills, hooks novos.** Sub-etapa de refinamento.

5. **NAO atualizar `CLAUDE.md`.** Refinamento de comportamento de subagent nao muda convencao do projeto.

6. **NAO atualizar `docs/adrs.md`.** Sem ADR novo. Refinamento taxonomico fica em `decisoes-claude-code.md`.

7. **NAO atualizar `docs/decisoes.md` (fundacional).** Refinamento e de componente operacional da Camada 3 — vai em `decisoes-claude-code.md`.

8. **NAO atualizar `docs/visao.md`, `docs/blueprint-fabrica-ai-native.md`, `docs/progresso-historico.md`, `docs/hooks-pendentes.md`.**

9. **NAO marcar `[x] Smoke pos-merge da 4.17`.** Smoke fica como **validacao parcial** com nota explicativa. Padrao "smoke parcial registrado honestamente" prescrito.

10. **NAO modificar regras duras do `test-writer.md`** (JUnit 5, AssertJ, Zero Spring, sufixo Test, mock manual, etc.). Restricoes originais preservadas.

11. **NAO modificar restricao "NAO tente auto-corrigir em loop".** Padrao operador-soberano preservado.

12. **NAO modificar Exemplos 1 e 2.** Exemplo 1 (`Conta` caso happy) preservado mesmo sendo cenario hoje impossivel — pedagogico, nao checklist. Exemplo 2 (validacao falhando) preservado.

13. **NAO criar arquivos de teste reais** (ex: `ContaTest.java` novo) nesta sub-etapa. Refinamento entrega prescricao; smoke real aguarda Camada 4.

14. **NAO modificar `src/`, `frontend/`, `pom.xml`.**

15. **Encoding UTF-8 sem BOM** em todos os arquivos editados.

16. **Apenas ASCII em mensagens de commit.** Sem acentos, sem em-dash U+2014.

17. **Sem acentos no body do `test-writer.md`** (alinhado com convencao de subagents). Em-dash U+2014 permitido em prosa Markdown.

18. **Ordem cronologica descrescente** em "Sub-etapas concluidas", "Licoes", "Historico" em `progresso.md`.

19. **Sem cenarios destrutivos tradicionais.** Sub-etapa modifica system prompt — validacao via pre-condicoes ADR-011 em cada Tarefa. Validacao empirica real vem no proximo uso de `/write-test`.

20. **Hook 4.4 NAO deve alertar em nenhum commit.** Se alertar, investigar.

21. **Nao sugerir proxima sub-etapa** espontaneamente alem dos candidatos ja citados (4.18 ampliando para integration, `/feature`, eventual smoke real na Camada 4).

22. **Nao tomar decisao silenciosa em zona limitrofe.** Reportar divergencias.

23. **Nao usar `pwsh`.** PowerShell 5.1.

24. **Nao usar `git reset --hard`.**

25. **Nao usar `git commit --no-verify`.**

## Estrutura de commits

Branch: `refactor/etapa-4-17-1-arquivo-ja-existe`

**Commit 1** — `refactor(claude): test-writer com prescricao explicita para arquivo ja existe (sub-etapa 4.17.1)`

- `.claude/agents/test-writer.md` (passo "0" + exemplo 3 + 2 restricoes novas)

**Commit 2** — `docs: subsecao 4.17.1 -- refinamento pos-smoke empirico do test-writer`

- `docs/decisoes-claude-code.md` (subsecao 4.17.1)

**Commit 3** — `docs: sub-etapa 4.17.1 -- registra refinamento + smoke 4.17 como validacao parcial`

- `docs/progresso.md` (sub-etapa 4.17.1 + criterios ajustados + licoes + historico)

**Commit 4** — `docs: versiona prompt-etapa-4-17-1.md em docs/prompts/`

- `docs/prompts/prompt-etapa-4-17-1.md` (novo)

## Validacao antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -5
git config core.hooksPath
```

```powershell
Test-Path .claude\agents\test-writer.md
(Get-Content .claude\agents\test-writer.md).Count
Test-Path .claude\agents\pr-reviewer.md
Test-Path .claude\agents\architect-reviewer.md
Test-Path .claude\skills\write-test\SKILL.md
```

Esperado:

- `check.ps1` passa.
- Working tree limpo.
- 4 commits novos.
- `test-writer.md` em ~190-210 linhas (crescimento de ~32-52 sobre 158).
- Componentes nao tocados permanecem inalterados.

## PR

Titulo: `refactor: sub-etapa 4.17.1 -- test-writer com prescricao explicita para arquivo ja existe`

Body sugerido:

````markdown
## Summary

Sub-etapa de **refinamento pos-smoke empirico** (categoria 4.9.1 — segunda aplicacao). Smoke da 4.17 (conduzido em `Conta.java`) revelou borda nao-prescrita no system prompt do `test-writer`: o arquivo de teste alvo (`ContaTest.java`) ja existia com cobertura manual cuidadosa. Subagent improvisou auditoria minuciosa — output de alta qualidade tecnica, mas comportamento nao-prescrito. Sub-etapa formaliza prescricao explicita.

### Por que esta sub-etapa existe

Confiar em improvisacao recorrente e risco. Proxima invocacao pode improvisar diferente (pior — ex: sobrescrever sem confirmar). Padrao operacional: **improvisacao bem-sucedida em smoke vira prescricao em refinamento subsequente**.

### Inventario empirico revelou que projeto nao tinha cobaia legitima

11 classes de domain sem teste no `financas-lab` analisadas:

- 3 repositorios (interfaces puras, sem comportamento).
- 4 exceptions (boilerplate).
- 3 enums sem metodos.
- 2 records sem logica.

**Nenhuma com comportamento real testavel.** Forcar smoke em uma delas teria gerado teste tautologico (validando JVM ou constantes). Cobaia legitima exige classe nova com comportamento real, que so virá na Camada 4.

### Smoke da 4.17 mantido como validacao parcial

Padrao novo: **manter como `[ ]` com nota explicativa**. Componentes funcionam parcialmente (fork OK, template OK, validacao mvnw OK), mas geracao propriamente dita nao exercitada. Honesto — sem trapacear o registro.

### Prescricao adicionada

Passo "0" no fluxo do `test-writer`:

> Antes de gerar, verifique se o arquivo de teste alvo ja existe. Se existir: NAO sobrescreva. Rode `./mvnw test -Dtest=<NomeDoTest>` para confirmar que o existente passa. Reporte usando template padrao (Arquivo gerado: "Nenhum"). Cobertura: resumo em ate 3 linhas, sem bullets. Decisao: 2 opcoes ao operador.

Exemplo few-shot 3 ilustrando ("arquivo ja existe").

2 restricoes novas em "O que NAO fazer":

- NAO faca analise minuciosa de cobertura quando arquivo ja existe (max 3 linhas).
- NAO sobrescreva arquivo de teste pre-existente.

### Razao da restricao "max 3 linhas, sem bullets"

Analise minuciosa de cobertura e responsabilidade de comando separado (`/review-test` se entregue no futuro), nao do `test-writer`. Geradores entregam artefato + meta-informacao curta; revisores entregam analise estruturada. Manter limite de escopo entre eles.

### Mudancas

- `.claude/agents/test-writer.md`: passo "0" no fluxo + exemplo 3 + 2 restricoes novas. ~32-52 linhas adicionadas. Tudo o mais preservado.
- `docs/decisoes-claude-code.md`: subsecao "Refinamento pos-smoke do test-writer: comportamento 'arquivo ja existe' (Sub-etapa 4.17.1)" antes de "Claude Code hooks nativos".
- `docs/progresso.md`: sub-etapa 4.17.1 + criterios ajustados (smoke 4.17 com nota de validacao parcial) + 6 licoes + historico.
- `docs/prompts/prompt-etapa-4-17-1.md`: prompt versionado.

### Categoria consolidada por dupla aplicacao

"Refinamento pos-smoke empirico" — primeira foi 4.9.1, segunda e 4.17.1. Padrao formalizado: smoke revela borda nao-prescrita; sub-etapa cirurgica X.Y.1 adiciona prescricao explicita sem mudar o resto do componente.

### CLAUDE.md NAO atualizado

Refinamento de comportamento de subagent nao muda convencao do projeto.

### Proximo passo

Decisao fora deste PR. Candidatos naturais:

- **Smoke real do `test-writer`** acontecera no primeiro uso da Camada 4 (quando feature nova trouxer classe de domain sem teste ainda).
- **4.18** se uso justificar — ampliar `test-writer` para integration tests via refactor (categoria 4.14). **Atencao:** mesma armadilha de cobaia pode aparecer — todo `*Repository.java` provavelmente ja tem `*RepositoryIT.java` manual.
- **4.18 alternativo** — Skill sem subagent `/feature <nome>` (eixo novo: skill geradora pura).
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

- Branch `refactor/etapa-4-17-1-arquivo-ja-existe` empurrada com 5 commits (4 + 1 update do PR).
- PR aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.17.
- Working tree limpo.
- `.claude/agents/test-writer.md` em ~190-210 linhas.
- `.claude/agents/pr-reviewer.md`, `.claude/agents/architect-reviewer.md`, `.claude/skills/write-test/SKILL.md`, `.claude/skills/review-pr/SKILL.md`, `.claude/skills/review-arch/SKILL.md` inalterados.
- Reportar: `git log --oneline -5`, `git status`, `gh pr view --json number,state,statusCheckRollup`, contagem de linhas do `test-writer.md`.

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao executar a skill `/write-test` (smoke real aguarda Camada 4).
- Nao criar arquivos de teste em `src/test/`.
- Nao modificar `src/main/java/`, `frontend/`, `pom.xml`.
- Nao criar prompt da 4.18 ou outros.
- Nao criar outros subagents, skills, hooks, MCPs.
- Nao modificar `.claude/skills/`, outros subagents, hooks.
- Nao mexer em `~/.claude/` global.
- Nao atualizar `CLAUDE.md`, blueprint, `.gitignore`, `.gitattributes`, `hooks-pendentes.md`, `adrs.md`, `decisoes.md`, `visao.md`, `progresso-historico.md`.
- Nao marcar `[x] Smoke pos-merge da 4.17` (smoke fica como validacao parcial).
- Nao sugerir proximo passo espontaneamente alem dos candidatos ja citados no PR body.
- Nao usar `pwsh`.
- Nao usar `git reset --hard`.
- Nao usar `git commit --no-verify`.
