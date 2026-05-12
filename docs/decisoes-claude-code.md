# Decisoes — Camada 3 (Configuracao do Claude Code)

> Documento dedicado a decisoes operacionais da Camada 3 do projeto: hooks, subagents, skills, padroes de validacao destrutiva, convencoes operacionais de Claude Code.
> Origem: separado de `docs/decisoes.md` na Sub-etapa 4.16 quando o arquivo original cruzou 800 linhas (trigger do hook 4.4 modo warn).
> Para decisoes fundacionais do projeto (Stack, Arquitetura, Convencoes de codigo, Politica de debito, Comandos atomicos, Frontend, Modelo financeiro, Principios do blueprint), ver `decisoes.md`.

**Data de criacao:** 2026-05-11 (Sub-etapa 4.16)

---

## Camada 3 — Configuração do Claude Code

**Início:** 2026-05-10 (Sub-etapa 4.0).

### Layout de `.claude/`

`.claude/` é a casa da fábrica no projeto. Organizada por escopo de aplicabilidade conforme ADR-009: `universal/`, `java-spring/`, `windows/`, `next/`, `local/` dentro de `hooks/`, `agents/` e `skills/`. Promoção entre escopos exige evidência explícita (segundo contexto + decisão consciente).

### Mecanismo de git hooks no Windows

`git config core.hooksPath .githooks` configurado automaticamente por `scripts/setup.ps1`. Entrypoints em `.githooks/` são wrappers bash sem extensão chamando companheiros `.ps1`. Lógica real fica em `.claude/hooks/<escopo>/`.

### Débito de portabilidade

ADR-010 registra aceitação consciente: hooks e scripts PowerShell-specific. Migração avaliada ao entrar Camada 5 ou nascer 2ª fábrica em outro SO. Custo estimado: 1-3 dias.

### Conventional Commits (Sub-etapa 4.1)

**Tipos permitidos:** feat, fix, chore, docs, test, refactor, style, perf, build, ci.

**Formato:** `<tipo>[(scope)][!]: <descricao>` com pelo menos 10 caracteres na descricao.

**Scope:** opcional. Lowercase + digitos + hifen entre parenteses. Convencao do projeto usa nome do modulo (`feat(transacao):`, `chore(scripts):`).

**Breaking change:** indicado por `!` apos scope (`feat!:` ou `feat(api)!:`).

**Excecoes automaticas:** mensagens iniciadas por `Merge ` ou `Revert ` (geradas pelo git) passam sem validacao.

**Override consciente:** `git commit --no-verify` e escape valido em emergencias (bug critico em producao, hotfix que justifica pular validacao). Cada invocacao deve ser registrada no PR body com motivo. Sem policia automatica — disciplina por norma.

**Hook implementado em:** `.claude/hooks/universal/conventional-commits.ps1`, invocado por `.githooks/commit-msg` (entrypoint bash) -> `.githooks/commit-msg.ps1` (companheiro PowerShell).

### Encoding UTF-8 (Sub-etapa 4.2)

**Regra:** arquivos de texto staged devem ser UTF-8 valido. Arquivos `.ps1` adicionalmente NAO podem ter BOM (licao da Etapa 2.6).

**Whitelist por extensao:** `.md`, `.java`, `.yml`, `.yaml`, `.xml`, `.properties`, `.ps1`, `.sql`, `.ts`, `.tsx`, `.js`, `.jsx`, `.json`, `.css`, `.html`.

**Whitelist por nome exato:** `.gitignore`, `.gitattributes`, `.editorconfig`, `.env.example`. Arquivos sem extensao dentro de `.githooks/` (entrypoints bash) tambem incluidos.

**Fora da whitelist:** binarios (`.png`, `.jpg`, `.pdf`) e tipos nao listados (`.toml`, etc) passam silenciosamente. Adicionar item a whitelist quando primeiro caso real surgir — decisao consciente, nao automatica.

**Filtro do diff:** `git diff --cached --name-only --diff-filter=ACM` (Added, Copied, Modified). Deleted e Renamed-rename-only sao ignorados (sem conteudo a validar).

**Sem deteccao por conteudo** (`file --mime` ou similar) — coerente com ADR-009 ("sem dependencias externas, PowerShell puro").

**Hook implementado em:** `.claude/hooks/universal/encoding-utf8.ps1`, invocado por `.githooks/pre-commit` -> `.githooks/pre-commit.ps1` (orquestrador 1:N).

### Padrao orquestrador 1:N para `pre-commit` (Sub-etapa 4.2)

O entrypoint companheiro `.githooks/pre-commit.ps1` e desenhado como **orquestrador**, diferente do `commit-msg.ps1` da 4.1 que e delegador 1:1. Razao: varias validacoes distintas (encoding, blank lines em Markdown, tamanho de docs, etc) precisam rodar antes de cada commit. Array `$hooks` no orquestrador e o ponto de extensao — sub-etapas seguintes da Camada 3 acrescentam linhas a esse array.

**Execucao em sequencia, nao paralela.** Cada hook le seu proprio `git diff --cached`. Se multiplos hooks falharem, todos reportam suas mensagens (sem early-exit no primeiro fail) — operador ve todas as violacoes de uma vez.

**Sem contrato compartilhado entre hooks** alem de: "exit 0 = ok, exit != 0 = bloqueia".

### Padroes de validacao destrutiva (Sub-etapa 4.2.1)

Ratificado em ADR-011. Toda validacao destrutiva — na branch da etapa ou em smoke test pos-merge — segue tres regras:

1. **Pre-condicao explicita apos cada criacao de arquivo:** `Test-Path` (ou equivalente). Se `False`, parar.
2. **`git status` antes de `git commit`** para confirmar arquivo staged. Se `nothing to commit`, parar.
3. **Verificacao de exit code apos comando que deveria falhar.** Esperar codigo `!= 0`; se vier `0`, cenario nao reproduziu erro esperado.

**Para PowerShell + `[System.IO.File]::WriteAllText` com path relativo:** sincronizar previamente:

```powershell
[System.Environment]::CurrentDirectory = (Get-Location).Path
```

Ou usar path absoluto (`"$PWD\arquivo"`). Sem isso, arquivo e gravado em diretorio que pode divergir do `$PWD` (gotcha confirmado em smoke test pos-merge da 4.2).

**Reportar pre-condicoes verificadas no PR body** — nao basta listar "cenarios validados". Listar tambem o valor observado em cada pre-condicao. Falsos positivos silenciosos sao detectados apenas por verificacao explicita.

**Aplica retroativamente:** sub-etapas 4.3+ devem incluir esses gates no roteiro de validacao destrutiva. Sub-etapas 4.0 a 4.2 ja mergeadas nao sao revistas — confirmacao empirica posterior (smoke test pos-merge da 4.2 corrigido) validou que o codigo dessas sub-etapas esta correto.

### Blank lines em Markdown (Sub-etapa 4.3)

**Regra:** arquivos `.md` staged devem ter linha em branco antes E depois de cada header de nivel 2-6 (`##` ate `######`). Headers de nivel 1 (`#`) sao ignorados (tipicamente titulo do documento).

**Escopo:** apenas `.md` (nao `.markdown`, nao `.mdx`). Qualquer pasta (nao restrito a `docs/`).

**Fronteira do arquivo e linha em branco implicita:** header na primeira linha nao precisa de linha em branco antes. Header na ultima linha nao precisa de linha em branco depois.

**Headers dentro de blocos de codigo (`` ``` ``)** sao ignorados — sao exemplos, nao headers reais. Blocos indentados com 4 espacos nao sao cobertos (limitacao consciente; raro em pratica moderna).

**Hook implementado em:** `.claude/hooks/universal/markdown-blank-lines.ps1`, segundo hook a viver dentro do orquestrador `pre-commit` (4.2). Apenas uma linha adicionada ao array `$hooks` em `.githooks/pre-commit.ps1` — sem refatorar arquitetura.

### Tamanho de docs em modo warn (Sub-etapa 4.4)

**Regra:** arquivos `.md` em `docs/` (qualquer nivel de profundidade) com mais de 800 linhas totais geram alerta visual no terminal durante o commit. **Commit prossegue normalmente — alerta nao bloqueia.**

**Escopo:** apenas `docs/*.md`. Outros `.md` (README raiz, `.github/`, `frontend/`, etc.) sao ignorados.

**Metrica:** linhas totais via `[System.IO.File]::ReadAllLines($path).Count`. Inclui linhas em branco — simples, alinhado com como o operador ve o arquivo.

**Limite:** 800 linhas. Folga sobre o `progresso.md` atual (~680) e `decisoes.md` (~470), com espaco para crescimento natural ao longo das Camadas 3 a 6.

**Padrao novo estabelecido — modo `warn` para regras subjetivas:**

Tamanho de doc nao tem "valor errado". 600 linhas pode ser certo para um doc denso; 1500 pode ser certo para um indice completo. Bloquear forcaria split apressado em momentos inoportunos. Por isso, hooks de **regras subjetivas** seguem padrao `warn`: alertam no terminal mas saem com exit code 0, deixando ao operador a decisao de agir.

Hooks de **regras objetivas** continuam em modo `fail` (Conventional Commits, encoding UTF-8, blank lines em Markdown). Modo do hook e parte do design, registrada em `decisoes.md` quando o hook nasce.

**Override:** nao aplicavel — hook nao bloqueia. `--no-verify` continua valido se necessario para outros hooks da pipeline.

**Hook implementado em:** `.claude/hooks/universal/docs-size.ps1`, terceiro hook no orquestrador `pre-commit` (1:N da 4.2). Apenas uma linha adicionada ao array `$hooks` em `.githooks/pre-commit.ps1`.

**Fecha o lote universal de Markdown** — apos 4.4, proximas universais (se houver) entram por demanda, nao pelo plano. Proximas sub-etapas focam em hooks de stack (`java-spring/`), CLAUDE.md, subagents ou skills.

### Maven release explicito (Sub-etapa 4.5)

**Regra:** se `pom.xml` esta no diff staged, deve conter pelo menos uma ocorrencia da tag `<release>` com qualquer conteudo interno. Caso contrario, commit bloqueado.

**Por que:** licao 1.4 — sem `<release>` explicito, Maven usa default que pode divergir entre dev local e CI, resultando em build inconsistente. Lab atual ja tem `<release>${java.version}</release>` configurado; hook arma regra para prevenir regressao.

**Valor da tag e livre:** `<release>21</release>`, `<release>17</release>`, `<release>${java.version}</release>` — todos passam. Hook valida presenca, nao valor. Versao Java e decisao de projeto, nao decisao de hook.

**Padrao novo estabelecido — hooks especificos de stack:**

Esta e a primeira sub-etapa a ocupar `.claude/hooks/java-spring/`. Universais (`universal/`) e especificos de stack (`java-spring/`, `next/`, `windows/`, `local/`) coexistem no orquestrador `pre-commit` sem distincao sintatica. O array `$hooks` em `.githooks/pre-commit.ps1` lista todos os hooks na ordem de registro, agnostico a escopo.

A diferenca e apenas o **criterio de aplicabilidade dentro do hook:** cada hook le `git diff --cached --name-only` e decide se vale agir. Hooks universais agem sempre (ou filtram por extensao generica como `.md`). Hooks de stack filtram por arquivos especificos da stack (`pom.xml`, `*.java`, `package.json`, etc.).

**Decisao consciente (D2 calibrada com operador):** filtro de aplicabilidade fica dentro do hook, nao no orquestrador. Razao: consistencia com 4.2-4.4. Custo de invocar hook que sai imediato com `exit 0` (quando nao se aplica) e negligivel. Centralizar filtro no orquestrador seria otimizacao prematura — so faria sentido com 20+ hooks ou com hooks pesados (parser de arquivo grande, etc).

**Hook implementado em:** `.claude/hooks/java-spring/maven-release.ps1`, quarto hook no orquestrador `pre-commit`.

### CLAUDE.md do projeto (Sub-etapa 4.6)

`CLAUDE.md` na raiz do repo carrega contexto inicial em toda sessao do Claude Code automaticamente.

**Conteudo:** identidade do projeto, stack, ambiente operacional, mecanismo de hooks (modos `fail`/`warn`), convencoes e padroes, onde buscar mais em `docs/`, lista do que nao fazer.

**Conteudo volatil NAO entra:**

- Estado atual (Camada/Sub-etapa) -- link para `docs/progresso.md`.
- Lista de hooks ativos com regras -- link para `docs/hooks-pendentes.md` (secao "Hooks implementados").
- Lista de arquivos `docs/prompt-etapa-*.md` -- proliferam, agente busca quando precisa.

CLAUDE.md menciona o **mecanismo de hooks** e os **modos `warn`/`fail`** porque sao estruturais. Lista especifica do que esta ativo fica fora.

**Alvo de tamanho:** ate 200 linhas. ~6-8KB. Limite duro: 250 linhas. Razao: CLAUDE.md entra em toda mensagem da sessao, nao so na primeira. Documento curto e denso > documento longo e completo.

**Regra de atualizacao:**

CLAUDE.md e editado **dentro da sub-etapa** que muda algo estrutural -- nao em sub-etapa propria de "atualizacao". Estrutural = stack, ambiente, convencoes, restricoes.

Sub-etapas que apenas adicionam hook **nao editam CLAUDE.md**. Hook entra na lista de `docs/hooks-pendentes.md` (que ja e linkado). Sub-etapas que avancam Camada **nao editam CLAUDE.md**. Estado vive em `docs/progresso.md` (que ja e linkado).

Esta regra entra nas Restricoes/freios dos prompts futuros: "verificar se a sub-etapa muda stack/ambiente/convencoes/restricoes. Se sim, atualizar CLAUDE.md no escopo da sub-etapa. Se nao, nao tocar em CLAUDE.md".

### @Entity sem migration Flyway (Sub-etapa 4.7)

**Regra:** se `.java` novo (status `A` no diff) sob `src/main/java/` contem `@Entity`, deve haver pelo menos um arquivo `.sql` novo em `src/main/resources/db/migration/V<n>__*.sql` no mesmo commit. Caso contrario, commit bloqueado.

**Por que:** licao 2.1 -- nova `@Entity` sem migration cria divergencia entre codigo e schema. Hibernate pode tentar criar tabela em runtime, mas isso e proibido em producao (`ddl-auto=validate`). Build local passa, prod quebra ao subir.

**Escopo reduzido conscientemente -- caso edge fora desta sub-etapa:**

Texto original da licao 2.1 em `docs/hooks-pendentes.md` menciona: "Modificacao de `@Entity` JPA exige migration Flyway no mesmo PR". Esta sub-etapa implementa apenas o caso de Entity **nova** (status `A`), nao modificacao (status `M`).

Razao: status `M` produziria falso positivo alto. Refatoracao cosmetica de Entity existente (rename de variavel Java sem `@Column(name=...)`, adicao de comentario, mudanca de formatacao) nao requer migration -- mas hook nao distingue isso sem parser de Java. Forcar criacao de migration vazia destruiria confianca no hook rapidamente.

Caso "modificacao de Entity existente requer migration" fica como **debito conscientemente aceito**, registrado em `docs/hooks-pendentes.md` na lista "Pendentes". Hoje, modificacao depende de disciplina do dev + revisao de PR. Se aparecer dor real (CI quebrar por esquecimento), sub-etapa futura calibra implementacao mais sofisticada (talvez parsing de diff `git diff --cached -U0 <arquivo>` para detectar adicao de `@Column`).

**Valor da migration e livre:** hook valida **presenca**, nao conteudo. Multiplas Entities + 1 migration consolidada = aceito (cobertura e responsabilidade do dev). Analogo ao Maven `<release>` (4.5) que valida presenca da tag, nao valor.

**Hook implementado em:** `.claude/hooks/java-spring/entity-migration.ps1`, quinto hook no orquestrador `pre-commit`, segundo em java-spring.

### Smoke test pos-merge usa input idiomatico (Sub-etapa 4.7.1)

**Regra:** smoke test pos-merge de hooks deve replicar **input idiomatico** -- codigo formatado como humanos com IDE escreveriam, nao sintetico.

**Por que:** descoberto no smoke test pos-merge da 4.7. Cenario B usou Java single-line (`package x; import y.Entity; @Entity public class Foo {}`) que nao casou com a regex do hook entity-migration (`(?m)^\s*@Entity\b` exige `@Entity` em inicio de linha). Resultado: smoke aparentou falha do hook quando o problema era input sintetico.

**Diagnostico:** hook funciona em producao real (Java idiomatico tem `@Entity` em linha propria). Smoke test sintetico mascarou falso negativo. Investigacao identificou bug no smoke, nao no hook.

**Regra concreta para smoke tests futuros:**

- **Java:** anotacoes em linha propria; `package` em uma linha; `import` cada um em linha propria; classe em linha propria; campos em linhas separadas.
- **JSON:** formatado (indented), nao compactado.
- **YAML / XML / Markdown:** formato padrao com quebras de linha.

**Excecoes:** se o hook explicitamente trabalha com formato compactado (minifier, linter de minificacao), smoke usa o formato esperado. Hoje nao ha hooks assim no projeto.

**Categoria de licao consolidada:** "smoke test deve replicar input idiomatico, nao sintetico". Adicionada como regra geral em smoke tests pos-merge de hooks daqui pra frente.

### Primeiro subagent: `pr-reviewer` (Sub-etapa 4.9)

**Componente:** `.claude/agents/pr-reviewer.md`. Modelo: **Haiku** (blueprint indica modelo barato para revisores). Tools restritas: `Read, Grep, Glob, Bash` (read-only). Invocacao **proativa via `description`** — Claude principal decide quando delegar baseado em descricao explicita de uso apos abertura de PR.

**O que faz:** complementa os hooks pre-commit automaticos do projeto. Revisa **o que hooks nao pegam**: decisoes de design vs ADRs, coerencia com sub-etapas anteriores e `decisoes.md`, logica do codigo (edge cases, mensagens de erro), cobertura de testes, documentacao alinhada com mudanca, padroes do projeto (Conventional Commits substancia, nao sintaxe).

**O que NAO faz** (delegado aos hooks 4.1-4.7): Conventional Commits sintaxe, encoding UTF-8, Markdown blank lines, tamanho de docs, Maven `<release>`, `@Entity` sem migration. Subagent que duplica hook e desperdicio.

**Output:** Markdown estruturado em 3 secoes: **Bloqueadores**, **Sugestoes**, **Elogios**. Operador (humano) ve no chat e decide se cola no PR como comentario. Subagent **nao posta no PR** via `gh pr review` (limite consciente — adiciona risco de spam, deferido para sub-etapa futura quando confianca no subagent estiver consolidada).

**Layout flat em `.claude/agents/`:** A estrutura criada na 4.0 em `.claude/agents/` tem 3 subpastas (`universal/`, `java-spring/`, `local/`), não 5 como em `.claude/hooks/` (que tem `java-spring`, `local`, `next`, `universal`, `windows`). A simetria entre `.claude/{hooks,agents,skills}/` é parcial; cada estrutura tem subpastas distintas refletindo decisões da 4.0. As subpastas de `.claude/agents/` foram prescritas por intenção de organização por escopo, mas Claude Code não reconhece subagents em subpastas — convenção é flat (`.claude/agents/*.md`). Pasta `.claude/agents/universal/` continua existindo (decisao da 4.0 nao revertida), mas **subagents vao em `.claude/agents/` direto**. Categoria: "convencao Claude Code descoberta apos prescricao do projeto" — registrada como licao na 4.9.

**Quando invocado:** description proativa pos-abertura de PR. Para PRs **puramente doc-only**, revisao breve. Para PRs com codigo, revisao completa nas 6 categorias acima.

### Refinamento do `pr-reviewer` pos-smoke (Sub-etapa 4.9.1)

**Componente:** `.claude/agents/pr-reviewer.md` (criado na 4.9, refinado aqui).

**O que mudou:** template de output explicitamente prescritivo + 2 exemplos few-shot.

**Por que:** smoke test pos-merge da 4.9 confirmou que subagent existe, e invocado proativamente, e produz output util. Mas output divergiu do template prescrito: usou 4 secoes (Visao Geral, Analise, Itens Especificos, Conclusao) em vez das 3 prescritas (Bloqueadores, Sugestoes, Elogios). Subagent improvisou estrutura propria — comportamento valido mas indesejado para consistencia entre revisoes.

**Como:**

1. **Tom prescritivo.** Substituido "ver template abaixo" por "Voce DEVE usar exatamente as 3 secoes abaixo, nesta ordem, sem acrescentar outras". Lista explicita de secoes proibidas (Visao Geral, Analise, Conclusao, etc.).
2. **Few-shot prompting.** 2 exemplos completos de output (PR doc-only sem problemas + PR de hook com sugestao real). Modelos menores como Haiku aderem melhor a estrutura via exemplos concretos vs descricao abstrata.

**Sem mudanca de modelo.** Haiku permanece. Smoke confirmou que o problema era do prompt do subagent (descricao abstrata, tom sugestivo), nao da capacidade do modelo. Subir para Sonnet agora seria decisao sem evidencia.

**Categoria nova: "refinamento de componente baseado em smoke empirico".** Diferente de:

- "Registro pos-smoke falho" (4.2.1, 4.7.1): smoke falhou, sub-etapa doc-only documenta licao + decisao consciente (corrigir vs aceitar debito).
- "Patch tecnico" (4.0.1): corrige bug do que foi entregue.
- **Esta categoria:** smoke passou, mas revelou comportamento subotimo do componente; sub-etapa de refactor refina o componente. Padrao operacional: smoke pode ser positivo (componente funciona) E ainda gerar sub-etapa de refinamento (forma do output, aderencia ao prescrito).

**Smoke test pos-merge da 4.9.1:** mesmo formato da 4.9 — abrir PR de teste, invocar revisao, conferir se output agora usa **exatamente** Bloqueadores → Sugestoes → Elogios, sem secoes extras.

### Auditoria meta-operacional (Sub-etapa 4.10)

> **Errata (Sub-etapa 4.15):** auditoria empirica posterior revelou que dois dos tres debitos registrados aqui estavam baseados em premissas equivocadas. "Plugins globais" sao plugins oficiais cacheados (nao instalacoes manuais -- debito removido do backlog). "Memoria global" foi confundida com "transcripts" (memoria real ~85 KB; transcripts ~427 MB). "Built-ins competindo" e teorico (sem dor pratica observada). Ver subsecao "Errata de auditoria meta-operacional (Sub-etapa 4.15)" abaixo para detalhes. Registro original desta subsecao preservado integralmente -- auditoria registra o que sabiamos no momento.

Sub-etapa doc-only que inaugura categoria "auditoria meta-operacional" — diferente de "registro pos-smoke falho" (4.2.1, 4.7.1) e de "refinamento pos-smoke empirico" (4.9.1), pois afeta varios componentes / estrategia de camada, nao 1 componente.

**Quatro descobertas registradas:**

1. **Memoria global do Claude Code em `~/.claude/projects/<hash>/memory/`.** 21 arquivos `.md` de feedback/sub-etapas, auto-memory ON, escreve sem confirmacao. Vetor de contaminacao cross-projeto opaco. Mitigacao detalhada (desligar auto-memory? auditar conteudo? versionar politica de retencao?) registrada como debito em `hooks-pendentes.md`.

2. **Plugins instalados globalmente afetam comportamento sem aparecer no repo.** `code-review`, `frontend-design` (identificados na Camada 0) alteram heuristicas do Claude principal mesmo desabilitados localmente. Smoke da 4.9.1 confirmou comportamento alterado com plugin desabilitado.

3. **Built-in agents do Claude Code competem com subagents do projeto.** Cinco built-ins identificados nominalmente: `Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup`. Investigacao do que cada um faz e quando dispara fica como debito.

4. **Heuristica de delegacao proativa via `description` nao e deterministica.** Smoke da 4.9.1 mostrou que description bem-formada e tom prescritivo (4.9.1 endureceu o subagent) nao garantem invocacao via Task tool. Claude principal optou por execucao direta em PR trivial. Premissa do blueprint (linha 76) precisa de reinterpretacao: description ajuda mas nao garante.

**Decisao estrutural tomada:** Caminho B — **subagents do projeto sao invocados via skill orquestradora dedicada.** Formalizado em ADR-012.

**Alternativas avaliadas e rejeitadas:**

- **Caminho A** (description imperativa "ALWAYS delegate"): palpite sem evidencia, continua dependendo de heuristica caixa-preta.
- **Caminho C** (re-pensar Camada 3 sem subagents): descarta `pr-reviewer` baseado em N=1.

**Implicacoes para a Camada 3:**

- Criterios de "pronto" ajustados em `progresso.md`: cada subagent vem com skill orquestradora correspondente.
- `pr-reviewer` (4.9 + 4.9.1) **permanece valido**. Smoke confirmou que o componente funciona quando invocado.
- Sub-etapa 4.11 implementa primeira skill orquestradora (`/review-pr` invocando `pr-reviewer`). Smoke da 4.11 valida o padrao ADR-012 ponta-a-ponta.
- Subagents futuros (`architect-reviewer`, `test-writer`, `migration-writer`) nascem com skill correspondente — nunca isolados.

**Evidencia empirica:** PR #53 (smoke da 4.9.1) e a evidencia que gerou esta auditoria. PR fechado sem merge — smoke e descoberta, nao codigo de producao. URL preservada no historico para referencia.

**CLAUDE.md NAO atualizado nesta sub-etapa.** Regra 4.6: CLAUDE.md sincronizado com sub-etapa causadora. A 4.10 decide a convencao; a 4.11 implementa a primeira skill e atualiza CLAUDE.md com a convencao em uso.

### Primeira skill orquestradora: /review-pr (Sub-etapa 4.11)

Primeira implementacao do padrao decidido em ADR-012 (Sub-etapa 4.10). Marco estrutural — Camada 3 do blueprint pede 5-10 skills, este e o primeiro.

**Componente:** `.claude/skills/review-pr/SKILL.md`. Frontmatter declara:

- `name: review-pr`, `description` clara.
- `disable-model-invocation: true` — apenas operador invoca via `/review-pr <numero>`.
- `context: fork` + `agent: pr-reviewer` — mecanismo nativo do Claude Code dispara contexto forkado no subagent `pr-reviewer`. **Sem instrucao textual** "use Task tool...". Determinismo arquitetural via frontmatter.
- `argument-hint: [pr-number]` — autocomplete sugere argumento.
- `allowed-tools: Bash(gh pr view *) Bash(gh pr diff *)` — pre-aprovacao para evitar prompt de permissao no smoke.

**Conteudo curto.** System prompt do subagent (body de `pr-reviewer.md`) ja contem toda a logica de revisao (identidade, verificacoes, template, exemplos). A skill apenas entrega a tarefa concreta (`Revise o PR #$ARGUMENTS...`) + reforco do template (3 secoes). Evita duplicacao de fonte.

**Errata ADR-012 acompanha esta sub-etapa.** Investigacao da doc oficial do Claude Code (https://code.claude.com/docs/en/skills) revelou que o mecanismo prescrito originalmente no ADR-012 ("skill contem instrucao textual: 'Use a Task tool...'") reproduzia o mesmo nao-determinismo que o ADR buscava eliminar. Mecanismo nativo `context: fork` substitui — decisao estrutural preservada, mecanismo literal refinado. Categoria nova: **"errata de ADR baseada em descoberta de documentacao oficial"**.

**Cleanup de pastas orfas.** `.claude/skills/local/` e `.claude/skills/universal/` foram criadas pela 4.0 com expectativa de organizacao por escopo. Pelo padrao oficial, skills sao flat em `.claude/skills/<nome>/SKILL.md` — pastas intermediarias nao geram slash commands. Removidas via `git rm -r` na 4.11 para evitar confusao em sub-etapas futuras. **Estruturas `.claude/{hooks,agents,skills}` continuam assimetricas** (hooks=5 subpastas, agents=flat, skills=flat) — reflete decisoes especificas de cada mecanismo.

**Smoke test pos-merge** valida o padrao ponta-a-ponta:

1. Sessao nova do Claude Code.
2. Abrir PR de teste trivial em branch nova.
3. Invocar `/review-pr <numero>`.
4. **Criterios de sucesso:**
   - Skill dispara fork no agent `pr-reviewer` (Haiku) — sem execucao direta pelo Claude principal.
   - Output usa exatamente as 3 secoes prescritas (Bloqueadores, Sugestoes, Elogios).
   - Sem secoes extras (Visao Geral, Analise, Conclusao, etc.).
5. **Risco residual reconhecido:** debitos meta-operacionais da 4.10 (memoria global em `~/.claude/projects/.../memory/`, plugins globais, built-in agents) ainda nao mitigados. Smoke positivo nao prova determinismo absoluto, mas evidencia funcional do par skill+subagent suficiente para validar padrao.

**CLAUDE.md atualizado nesta sub-etapa** (regra 4.6 — 4.11 e a sub-etapa causadora da convencao "subagents+skills" entrar em uso). Subsecao "Subagents e skills" adicionada em "Convencoes e padroes" com 4 linhas resumindo o padrao do ADR-012 revisado.

**Criterios de "pronto" da Camada 3 ajustados.** Padrao "skill orquestradora -> subagent validado com smoke" depende do smoke pos-merge da 4.11 — fica em pendente ate o smoke passar.

### Manutencao de docs por crescimento (Sub-etapa 4.13)

Sub-etapa de manutencao que inaugura categoria "manutencao de docs por crescimento". Tratamento de dor real — `progresso.md` cresceu para ~891 linhas, hook 4.4 (modo warn) alertando em cada sub-etapa que toca o arquivo.

**Split do `progresso.md`:** criterio "cortar por Camada concluida". Camadas 0 (Discovery), 1 (Infraestrutura), 2 (Arquitetura) — todas ✅ — movem para novo `docs/progresso-historico.md`. Camada 3 em andamento + Camadas 4-6 planejadas + Licoes 4.X + historico de mudancas permanecem no `progresso.md` vivo.

**Por que cortar por Camada e nao por sub-etapa atomica:** mais coerente conceitualmente (Camada concluida = historico estavel), criterio simples de comunicar e replicar, evita splits artificiais. Alternativas avaliadas: cortar por tempo (ex: 30 dias) e cortar por bloco de licao individual — ambas rejeitadas por arbitrariedade ou fragmentacao.

**Tabela "Status geral" do `progresso.md` vivo** ganha link para cada Camada concluida no historico arquivado. Operadores e agentes em sessoes futuras chegam ao detalhe historico via 1 clique.

**Reorganizacao `docs/prompts/`:** `docs/` tinha 6 docs principais + ~13+ prompts versionados (`prompt-etapa-*.md`). Prompts dominavam visualmente o diretorio. Movidos para nova subpasta `docs/prompts/` via `git mv` (preserva historico via rename detection). `docs/` fica com hierarquia clara: 6 docs principais + 1 subpasta. Padrao para prompts futuros: `docs/prompts/prompt-etapa-X-Y.md`.

**CLAUDE.md atualizado** (regra 4.6 — 4.13 e a sub-etapa causadora das convencoes "split por crescimento" + "prompts em `docs/prompts/`"). Duas linhas em "Onde buscar mais": uma para `progresso-historico.md`, outra ajustando a referencia de prompts para apontar para `docs/prompts/`.

**Debito registrado em `hooks-pendentes.md`:** "Aplicar split analogo em `decisoes.md` quando cruzar 800 linhas. Padrao consolidado pela 4.13: split por secao funcional (subsecoes de sub-etapas antigas viram `decisoes-historico.md`). Tratar quando dor real aparecer — `decisoes.md` em ~600 linhas hoje, ainda navegavel."

**Smoke 4.12 marcado como concluido nesta sub-etapa.** Relato do operador (PR #35 revisado via `/review-arch`): output usou as 3 secoes prescritas, ancoragem nominal em ADR-004/006/007, Sonnet articulou raciocinio arquitetural com profundidade qualitativamente diferente do Haiku. Padrao skill+subagent validado em 2 casos.

**Categoria operacional "manutencao de docs por crescimento":** padrao replicavel para `decisoes.md`, `adrs.md`, outros docs futuros. Distinta de outras categorias operacionais ja consolidadas (auditoria meta-operacional, errata de ADR, replicacao de padrao, refinamento pos-smoke). Trata dor especifica de tamanho/legibilidade, nao de comportamento ou decisao estrutural.

### Hook 4.4 exclui `docs/prompts/` (Sub-etapa 4.14)

Sub-etapa de refactor de hook existente — primeira do projeto. Categoria nova: **"ajuste de hook por contexto novo"**. Hook `docs-size.ps1` (4.4, modo warn) modificado para ignorar `.md` em `docs/prompts/`.

**Por que:** a 4.13 (manutencao de docs por crescimento) criou `docs/prompts/` movendo 43 prompts versionados para la via `git mv`. Hook 4.4 passou a alertar em prompts antigos longos (4.11 ~870 linhas, 4.12 953, 4.13 1018) — falso positivo. Prompts versionados sao **registros historicos por natureza**; tamanho nao e criterio de qualidade.

**Mudanca cirurgica:** filtro de path do hook ganha exclusao `docs/prompts/`. Comportamento original (alerta para `.md` em `docs/` >800 linhas) preservado para `docs/` raiz e qualquer subpasta futura que nao seja `docs/prompts/`. Modo `warn` mantido, limite 800 mantido, extensao `.md` mantida.

**Outros hooks que tocam `.md` permanecem inalterados:**

- **Hook 4.2** (encoding UTF-8): continua atuando em `docs/prompts/`. Encoding e convencao de qualidade que vale para qualquer `.md` no repo.
- **Hook 4.3** (Markdown blank lines): continua atuando em `docs/prompts/`. Mesma justificativa.

Hook 4.4 isenta `docs/prompts/` **porque tamanho e fenomeno emergente** de sub-etapas complexas, nao desvio de qualidade.

**Categoria operacional nova: "ajuste de hook por contexto novo".** Hook cumpre regra original; sub-etapa posterior (aqui 4.13) cria contexto onde regra original gera falso positivo. Refactor ajusta hook ao contexto sem reverter intencao. Distinta de:

- **"Patch tecnico"** (4.0.1): corrige bug do entregue.
- **"Refinamento pos-smoke empirico"** (4.9.1): componente funcional mas output divergente.
- **"Errata de ADR baseada em descoberta de documentacao oficial"** (4.11): decisao estrutural preservada, mecanismo literal refinado.

Aplicavel a futuros casos onde subpasta criada por sub-etapa posterior introduz contexto que invalida regra de hook existente.

**Validacao destrutiva sob ADR-011:** 6 cenarios cobrindo comportamento original preservado + comportamento novo introduzido. Detalhes no PR body da 4.14.

**Debito da 4.13 resolvido:** item "modificar hook 4.4 para excluir docs/prompts/" removido de `hooks-pendentes.md`. Padrao operacional: debito originario de sub-etapa anterior resolvido em sub-etapa subsequente — cadeia "X cria contexto -> X+N resolve debito de contexto". PR #58.

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

### Errata de auditoria meta-operacional (Sub-etapa 4.15)

Sub-etapa de errata documental. Auditoria empirica de `~/.claude/` em maio/2026 revelou que **dois dos tres debitos meta-operacionais registrados na 4.10 estavam baseados em premissas equivocadas**. Esta sub-etapa corrige o registro sem alterar o conteudo original da 4.10 (preservacao historica).

**Verificacao empirica conduzida** via PowerShell em `~/.claude/projects/` e `~/.claude/plugins/`:

- `~/.claude/projects/` tem 17 diretorios de projeto totalizando ~427 MB em 853 arquivos.
- A pasta `memory/` (memoria derivada) aparece apenas em UM projeto e ocupa ~85 KB total.
- Os ~427 MB restantes sao transcripts de conversa -- logs brutos das sessoes, distribuidos diretamente sob cada hash de projeto.
- Plugins `code-review` e `frontend-design` ficam em `~/.claude/plugins/cache/claude-plugins-official/` -- plugins oficiais cacheados pelo marketplace `claude-plugins-official`, nao instalacoes manuais.

**Re-classificacao dos 3 debitos da 4.10:**

1. **"Memoria global auto-ON sem confirmacao"** -> **debito real, mas magnitude muito menor que o suposto.** Memoria derivada total: ~85 KB. A 4.10 confundiu memoria (resumos curtos) com transcripts (logs brutos de conversa). Questao de principio ("auto-ON sem confirmacao") permanece valida; impacto pratico minimo. Acao: reescrito em `hooks-pendentes.md` com dimensionamento real.

2. **"Plugins globais nao-versionados"** -> **nao e debito do projeto.** `code-review` e `frontend-design` sao plugins oficiais distribuidos pelo Claude Code, cacheados pelo marketplace `claude-plugins-official`. Equivalentes a built-ins. Decisao sobre eles e decisao sobre setup pessoal do Claude Code, nao sobre `financas-lab`. Acao: **removido do backlog do projeto** em `hooks-pendentes.md`.

3. **"Built-in agents competindo com subagents custom"** -> **sob observacao, sem dor pratica relatada.** Operador nao observou em sessoes recentes (maio/2026) delegacao para built-ins (`Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup`) nem interferencia com `pr-reviewer` ou `architect-reviewer`. Debito teorico, nao pratico. Acao: reescrito em `hooks-pendentes.md` como "sob observacao".

**Achado novo (nao registrado pela 4.10):**

4. **Transcripts em `~/.claude/projects/<hash>/<conversa-hash>/` ocupam ~427 MB em 17 projetos.** Sem rotina de expiracao automatica. Maior conversa unica: 90 MB. **Fenomeno fora do escopo do projeto** -- gestao de storage do Claude Code e decisao pessoal sobre setup, nao do `financas-lab`. Acao: registrado em `hooks-pendentes.md` para visibilidade, sem prescricao de acao.

**Categoria operacional nova: "errata de auditoria meta-operacional".** Distinta de:

- **"Auditoria meta-operacional"** (4.10): identifica debitos baseado em observacao inicial.
- **"Errata de ADR baseada em descoberta de documentacao oficial"** (4.11): corrige decisao estrutural via doc oficial.
- **Esta categoria:** corrige auditoria anterior baseada em **verificacao empirica posterior**. Aplicavel a casos onde auditoria inicial categoriza fenomeno mal, dimensiona errado, ou inclui itens fora do escopo real.

**Padrao "auditar antes de mitigar" demonstrado em segunda aplicacao.** A 4.13 dimensionou `progresso.md` antes de cortar (descobriu 891 linhas reais e cortou por Camada concluida). A 4.15 auditou `~/.claude/` antes de mitigar (descobriu que dois debitos da 4.10 estavam baseados em premissa errada). Em ambos os casos, auditoria empirica ajustou o escopo da sub-etapa. **Recomendacao operacional consolidada:** antes de mitigar territorio opaco, auditar empiricamente. Custo da auditoria e baixo (3-5 comandos de inspecao); risco de pular auditoria e alto (mitigacao baseada em premissa errada).

**Nota de errata adicionada a subsecao da 4.10** em `decisoes.md` apontando para esta subsecao. Padrao identico a ADR-012 que recebeu errata via 4.11. Registro historico da 4.10 preservado integralmente.

**Nada modificado em `~/.claude/`.** Decisoes sobre desligar auto-memory, limpar transcripts, ou desinstalar plugins ficam **fora do escopo** -- sao decisoes pessoais do operador sobre seu setup do Claude Code, nao do projeto `financas-lab`.

### Split do `decisoes.md` por tema (Sub-etapa 4.16)

Segunda aplicacao da categoria "manutencao de docs por crescimento" (consolidada pela 4.13). **Criterio de corte diferente:** enquanto a 4.13 cortou `progresso.md` por idade (Camadas concluidas viraram historico em `progresso-historico.md`), a 4.16 corta `decisoes.md` por **tema**.

**Por que tematico e nao cronologico:** o `decisoes.md` tem dois tipos qualitativamente distintos de conteudo:

- **Decisoes fundacionais** (Stack, Arquitetura, Convencoes de codigo, Convencoes operacionais, Politica de debito tecnico, Comandos atomicos, Frontend, Modelo financeiro, Principios do blueprint): convencoes do projeto consultadas em qualquer sub-etapa para reabrir contexto. Permanecem em `decisoes.md`.
- **Decisoes operacionais de Camada 3** (Layout de `.claude/`, mecanismo de git hooks, hooks 4.1-4.7, subagents 4.9-4.12, skills 4.11-4.12, padroes 4.13-4.15): convencoes especificas de Claude Code. Movidas para `decisoes-claude-code.md`.

**Ambos os tipos sao vivos.** Decisao de Conventional Commits (4.1) continua valendo em todo commit; padrao de validacao destrutiva (4.2.1) e regra dura aplicada em toda sub-etapa. Nao havia "historico" a arquivar — apenas dois conjuntos de decisoes vivas com escopos distintos. Padrao de split temporal da 4.13 nao se aplicava.

**Operacao facilitada por demarcacao H2 ja existente:** `decisoes.md` ja tinha cabecalho `## Camada 3 — Configuracao do Claude Code` delimitando exatamente a regiao a mover. Split foi extracao da H2 inteira (com todas suas subsecoes ### filhas), incluindo "Claude Code hooks nativos" que faz parte do mesmo escopo conceitual.

**`decisoes-claude-code.md` criado** (~460-490 linhas pos-split). `decisoes.md` enxuto (~370-400 linhas). Hook 4.4 para de alertar em ambos. PR #60.

**CLAUDE.md atualizado** (regra 4.6 — 4.16 e sub-etapa causadora da convencao "decisoes separadas por tema" entrar em uso). Quarta atualizacao de CLAUDE.md no projeto (apos 4.6, 4.11, 4.13).

**Debito da 4.13 resolvido:** item "Aplicar split analogo em `decisoes.md` quando cruzar 800 linhas" removido de `hooks-pendentes.md` -> "Debitos meta-operacionais". Cadeia "debito originario X -> resolvido em X+N" replicada (4.14 ja havia resolvido outro debito originario da 4.13, sobre hook 4.4 excluir `docs/prompts/`).

**Auto-referencia consistente:** esta subsecao 4.16 esta no proprio `decisoes-claude-code.md` (arquivo splittado), nao no `decisoes.md` vivo. Padrao "sub-etapa que altera estrutura registra a alteracao dentro da nova estrutura".

**Padrao operacional consolidado: criterio de split varia conforme natureza do documento.**

- `progresso.md` (cronologico) -> split por idade (Camadas concluidas viram historico).
- `decisoes.md` (tematico) -> split por tema (decisoes operacionais separadas das fundacionais).
- `adrs.md` (se crescer no futuro) -> provavelmente split tematico por dominio de decisao.
- Outros docs futuros -> avaliar natureza antes de splittar.

**Recomendacao operacional registrada nas licoes 4.16:** antes de splittar documento que cruza limite de tamanho, identificar **criterio natural de corte conforme o que o documento e**. Replicar padrao cego (sempre por idade, ou sempre por tema) pode gerar split artificial.

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

### Refinamento pos-smoke do test-writer: comportamento "arquivo ja existe" (Sub-etapa 4.17.1)

> **Revisao (Sub-etapa 4.18):** O passo "0" prescrito aqui foi reformulado pela 4.18 para incluir excecao "metodo alvo nao coberto" — quando arquivo de teste existe MAS algum metodo publico da classe alvo nao tem `@Test` correspondente, subagent ACRESCENTA via `Edit` (nao sobrescreve, nao Write). Comportamento "ja coberto" da 4.17.1 preservado para caso onde todos os metodos ja tem teste. Ver subsecao "Ampliacao do test-writer para integration tests (Sub-etapa 4.18)" abaixo para detalhes. Registro original desta subsecao preservado integralmente.

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

### Ampliacao do test-writer para integration tests (Sub-etapa 4.18)

Sub-etapa de **ampliacao de subagent por escopo prescrito** (refactor categoria 4.14). A ADR-007 prescreve tres niveis de teste (unit, integration, E2E); a 4.17 entregou apenas unit; a 4.18 completa integration. E2E fica para sub-etapa futura.

**Tambem revisa o passo "0" da 4.17.1** ("arquivo ja existe" ganha excecao "metodo alvo nao coberto" -> acrescenta via `Edit`). Padrao identico ao da errata da 4.10 pela 4.15: nota de revisao adicionada a subsecao 4.17.1 apontando para esta subsecao 4.18; registro original da 4.17.1 preservado integralmente.

**Gap arquitetural concreto descoberto na auditoria empirica:**

Antes de calibrar a 4.18, foi conduzida auditoria via PowerShell aplicando a licao da 4.17.1 ("auditar antes de calibrar"). Resultado revelou que:

- 3 `*JpaRepository.java` do projeto tem queries customizadas (derived queries + `@Query` JPQL).
- A query `calcularTotaisPorConta` no `TransacaoJpaRepository` e particularmente complexa: JPQL com `CASE WHEN` aninhado, `COALESCE`, constructor expression para record do domain.
- **Nenhuma das 4 queries customizadas tem teste integration especifico.** Os `*RepositoryImplTest` cobrem `salvar`/`buscar`/`deletar`/constraints, nao as queries. Use cases que consomem as queries mockam o repository.

Diferente da 4.17 (sem cobaia legitima -> smoke parcial honesto), a 4.18 tem **cobaia obvia** (`calcularTotaisPorConta`) e **gap real** a fechar. Smoke binario, criterio determinavel.

**Modificacoes no `test-writer.md`:**

1. **Tools:** ganham `Edit` (alem do `Write` ja existente). Necessario para acrescentar `@Test` a arquivo existente sem sobrescrever.

2. **`description`:** atualizada para refletir os dois niveis cobertos + redirecionamento.

3. **`## Identidade`:** ampliada para reconhecer unit e integration. E2E declarado explicitamente fora do escopo.

4. **`## O que voce GERA`:** substituida integralmente. Agora prescreve:
   - **Detecao de nivel por path** (tabela: domain -> unit, infrastructure/persistence/Impl -> integration, JpaRepository -> redireciona, Controller -> E2E fora do escopo, outros -> fora do escopo conhecido).
   - **Regras duras de unit** (preservadas da 4.17).
   - **Regras duras de integration:** JUnit 5 + AssertJ; extends `AbstractIntegrationTest`; @Autowired do `*RepositoryImpl` + `*JpaRepository`; @AfterEach `limpar()`; sem mock; sufixo `Test`; pacote espelho. Referencia de estilo: `ContaRepositoryImplTest.java` ou `TransacaoRepositoryImplTest.java`.
   - **Redirecionamento JpaRepository -> Impl:** convencao do projeto e que testes de queries customizadas vivem no `*RepositoryImplTest.java`. Subagent precisa fazer a traducao implicitamente e reportar nas "Decisoes de design".

5. **Passo "0" do fluxo `## Quando invocado`:** reformulado. Detecta nivel + verifica existencia + decide acao:
   - Path nao mapeado -> reporta "fora do escopo" e termina (sem improvisar).
   - Arquivo de teste nao existe -> proceda com fluxo de criacao (passos 2+).
   - Arquivo existe + todos os metodos cobertos -> reporta "ja coberto" (comportamento 4.17.1).
   - Arquivo existe + metodo nao coberto -> ACRESCENTE via `Edit`, sem mexer nos testes ja presentes (excecao nova 4.18).
   - NAO sobrescreva (destruir trabalho manual e proibido).

6. **Exemplo few-shot 4** adicionado: integration test acrescentado a `TransacaoRepositoryImplTest.java` existente para cobrir `calcularTotaisPorConta`. Output mostra estrutura do relatorio para o caso "metodo nao coberto -> acrescenta via Edit".

7. **Restricoes em "O que NAO fazer":**
   - Restricao "NAO sobrescreva arquivo pre-existente" (4.17.1) refinada com a excecao da 4.18.
   - Restricao nova: NAO improvise nivel quando path nao casa regra mapeada.

**Smoke pos-merge prescrito** (responsabilidade do operador apos autorizar merge):

```
/write-test src/main/java/com/laboratorio/financas/transacao/infrastructure/persistence/TransacaoJpaRepository.java
```

**Criterios de sucesso:**

1. Subagent detecta path = `*JpaRepository.java` e **redireciona para o `TransacaoRepositoryImpl.java`** correspondente. Reporta o redirecionamento em "Decisoes de design".
2. Subagent verifica que `TransacaoRepositoryImplTest.java` existe e passa (11/11 testes atuais).
3. Subagent identifica que `calcularTotaisPorConta` e `findComFiltros` nao tem `@Test` correspondente no arquivo existente.
4. Subagent **acrescenta `@Test` para `calcularTotaisPorConta`** ao arquivo existente via `Edit`. Nao sobrescreve; nao mexe nos 11 testes ja presentes.
5. `./mvnw test -Dtest=TransacaoRepositoryImplTest` compila e passa (todos os testes — antigos + novos).
6. Teste gerado e integration real (extends `AbstractIntegrationTest`, Testcontainers, sem mock).
7. Cobertura razoavel da query: cenarios com dados reais (conta com receitas/despesas, conta sem transacoes, conta destino de transferencia, etc.).

**Se algum criterio falhar:** abrir 4.18.1 (refinamento pos-smoke empirico, categoria 4.9.1/4.17.1).

**Padrao operacional adotado:** "implementa e roda, ajusta se precisar" (formalizado pelo operador na sessao de calibracao). Smoke obrigatorio no fim da sub-etapa; ajuste minimo se aparecer borda; proximo item sem buscar perfeicao preventiva.

**Revisao da 4.17.1** registrada via nota na subsecao 4.17.1 (logo apos o titulo, antes do corpo):

> O passo "0" prescrito aqui foi reformulado pela 4.18 para incluir excecao "metodo alvo nao coberto" — quando arquivo de teste existe MAS algum metodo publico da classe alvo nao tem `@Test` correspondente, subagent ACRESCENTA via `Edit` (nao sobrescreve, nao Write). Comportamento "ja coberto" da 4.17.1 preservado para caso onde todos os metodos ja tem teste. Registro original da 4.17.1 preservado integralmente.

**Categoria operacional: combina duas.** "Ajuste de subagent por contexto novo" (analogo a 4.14 — escopo prescrito pela ADR-007 cumprido nominalmente parcial, completa agora) **+** revisao da prescricao 4.17.1.

**CLAUDE.md NAO atualizado.** Ampliacao de comportamento de subagent nao muda convencao do projeto. Convencao "subagents e skills" (4.11) preservada.

### Claude Code hooks nativos

Mecanismo `PreToolUse`/`Stop`/`UserPromptSubmit` em `.claude/settings.json` é tratado em sub-etapa própria após 4.2. Diferente de git hooks: atua sobre comportamento do agente, não validação de código.

## Sub-etapa 4.19 -- Skill `/feature` sem subagent

### Decisao: skill direta vs skill-com-fork

ADR-012 (4.10/4.11) prescreveu o padrao `skill orquestradora -> subagent dedicado`
para tarefas que exigem fork de contexto. A 4.19 introduz variante nova: **skill
direta sem subagent**. Claude Code principal le o `SKILL.md` e executa as instrucoes
com seus proprios tools (Write + Bash), sem fork.

Criterio de escolha entre os dois padroes:

| Padrao | Quando usar |
|---|---|
| Skill com fork (ADR-012) | Tarefa exige isolamento de contexto, modelo diferente do principal, ou tools mais restritas |
| Skill direta (4.19) | Tarefa e procedural (sequencia de Write + Bash), sem necessidade de isolamento, sem logica de negocio emergente |

`/feature` e claramente procedural: valida argumento, cria diretorios, escreve 11
arquivos de template fixo, reporta resultado. Nao ha raciocinio de dominio que
justifique isolamento em subagent.

### Frontmatter da skill direta

```yaml
name: feature
disable-model-invocation: true
argument-hint: [nome-do-bounded-context]
```

Ausencia de `context: fork` e `agent:` e intencional e distingue a skill direta das
skills-com-fork existentes. Sem `allowed-tools` restrito -- Claude Code usa seus tools
normais (Write, Bash) conforme prescrito pelo corpo do SKILL.md.

### Categoria operacional

"Primeira aplicacao de padrao em eixo novo" -- mesma da 4.17 (primeiro subagent
gerador). A 4.19 estreia o eixo "skill sem subagent", distinto de:

- skills-com-fork (review-pr 4.11, review-arch 4.12, write-test 4.17)
- subagents revisores (pr-reviewer 4.9, architect-reviewer 4.12)
- subagents geradores (test-writer 4.17)

### Smoke parcial honesto

Cobaia natural nao disponivel (operador nao tem bounded context novo para produzir
agora). Smoke verifica: (1) skill executa sem erro; (2) 11 arquivos criados com
conteudo correto; (3) `mvn compile` passa. `mvn verify` e responsabilidade do
desenvolvedor apos criar migration Flyway para a Entity gerada (hook 4.7 bloqueia
commit sem migration). Padrao registrado pela segunda vez (primeira: 4.17).

### Aviso para uso real

O skeleton gerado inclui `@Entity` em `NOMEEntity.java`. Hook 4.7 bloqueia commit de
arquivo Java novo com `@Entity` sem migration Flyway correspondente. Desenvolvedor
deve criar `V<n>__create_ARG_table.sql` antes de commitar o bounded context.

## Sub-etapa 4.20 -- Skill `/ship` sem subagent

Segunda aplicacao do padrao inaugurado na 4.19 (skill direta sem `context: fork`).
Nenhuma decisao estrutural nova -- replicacao pura. Detalhes em [[project-scope-claude-dot]].

Criterio de escolha confirmado: skill direta e adequada quando a tarefa e
puramente procedural (sequencia de comandos shell com logica de fluxo simples).
`/ship` confirma o padrao: 5 passos bem definidos, sem raciocinio de dominio.

Nota de smoke: `/ship` testou a si propria ao criar o PR #66. Primeiro smoke
verdadeiramente automatico e completo do projeto (sem cobaia artificial e sem
parcialidade -- gate real, push real, PR real).

## Historico de mudancas deste documento

- **2026-05-12** -- Sub-etapa 4.20 concluida: skill `/ship` direta (sem subagent).
  Replicacao pura do padrao 4.19. Smoke natural completo (skill criou o proprio PR).
  PR #66.
- **2026-05-12** -- Sub-etapa 4.19 concluida: skill `/feature <nome>` direta (sem
  subagent). Padrao novo: skill procedural usa Write + Bash sem fork. Criterio de
  escolha entre skill-com-fork e skill-direta registrado. Categoria "primeira aplicacao
  de padrao em eixo novo". Smoke parcial honesto (segunda aplicacao do padrao). PR #65.

