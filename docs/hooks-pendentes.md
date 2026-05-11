# Hooks Pendentes — Candidatos a Automatizar

> Lista consolidada de validações/regras que viraram lição em alguma etapa e foram registradas como "candidatos a hook".
> Input direto para a Camada 3 (Configuração do Claude Code), quando hooks formais entrarem.
> Atualizado conforme novas lições aparecem.

**Última atualização:** 2026-05-11 (Sub-etapa 4.7.1 — registro de licoes da 4.7)

---

## Como ler este documento

Cada item lista:
- **De onde veio:** etapa que registrou a lição
- **O que faz:** comportamento desejado do hook
- **Como detectar (esboço):** sintaxe shell ou critério de validação

Itens **não estão implementados** — são pendência pra Camada 3.

---

## Escopo de aplicabilidade (triagem da Sub-etapa 4.0)

Conforme ADR-009, cada hook nasce na pasta `.claude/hooks/<escopo>/` correspondente ao seu escopo de aplicabilidade. Esta seção mapeia cada item já registrado neste documento ao seu escopo.

| Item (resumo) | Escopo | Pasta destino |
|---|---|---|
| `mvnw spring-boot:run` sem profile | java-spring | `.claude/hooks/java-spring/` |
| Linhas em branco em Markdown | universal | `.claude/hooks/universal/` |
| Encoding UTF-8 em arquivos de texto | universal | `.claude/hooks/universal/` |
| Conventional Commits | universal | `.claude/hooks/universal/` |
| Tamanho de docs em `docs/` | universal | `.claude/hooks/universal/` |
| `<release>` no maven-compiler-plugin | java-spring | `.claude/hooks/java-spring/` |
| Ordem Lombok antes de MapStruct | java-spring | `.claude/hooks/java-spring/` |
| Versão de plugin Maven validada via Maven Central | java-spring | `.claude/hooks/java-spring/` |
| `@Entity` JPA exige migration Flyway | java-spring | `.claude/hooks/java-spring/` |
| Classe base de teste sem `abstract` | java-spring | `.claude/hooks/java-spring/` |
| `baseline-on-migrate: true` apenas em test/dev | java-spring | `.claude/hooks/java-spring/` |
| Sufixo de classe de teste (`Test` singular) | java-spring | `.claude/hooks/java-spring/` |
| `mvnw` com bit de execução no git index | java-spring | `.claude/hooks/java-spring/` |
| Scripts Windows não usam ferramentas Unix | windows | `.claude/hooks/windows/` |
| Toda configuração de branch protection passou por teste destrutivo | universal (processo) | (não é hook automatizável — mantém em documentação de processo) |
| `Write-Error` + `exit N` em `.ps1` | windows | `.claude/hooks/windows/` |
| Comando nativo + `$LASTEXITCODE` sem suspensão de `Stop` | windows | `.claude/hooks/windows/` |
| Encoding UTF-8 sem BOM em `.ps1` | windows | `.claude/hooks/windows/` |
| `shadcn init --defaults` deixa `button.tsx` | next | `.claude/hooks/next/` |
| `AGENTS.md`/`CLAUDE.md` em subdiretórios scaffold | next | `.claude/hooks/next/` |
| Agente NÃO sugere "próxima etapa" espontaneamente | universal (Claude Code hook nativo) | (sub-etapa própria após 4.2, não git hook) |
| Agente NÃO toma decisões silenciosas em zona limítrofe | universal (Claude Code hook nativo) | (sub-etapa própria após 4.2, não git hook) |
| Validação destrutiva genuína exige código com lógica condicional real | universal (princípio) | (não é hook — princípio para retrospectivas/CLAUDE.md) |

### Convenções da tabela

- **universal:** roda em qualquer projeto, independente de stack ou SO.
- **java-spring:** roda em projetos com stack Java/Maven/Spring. Tipicamente faz `grep` em `pom.xml`, `*.java`, `application*.yml`.
- **windows:** roda em ambiente Windows + PowerShell. Tipicamente valida arquivos `.ps1` ou configurações Windows-specific.
- **next:** roda em projetos com Next.js. Tipicamente valida `frontend/` ou `package.json`.
- **local:** roda só neste projeto. Reservado para regras com forte vínculo a `financas-lab/` (nenhuma hoje).

### Itens fora do mecanismo de git hooks

Três tipos de item aparecem na lista acima mas **não vão para `.claude/hooks/`**:

1. **Claude Code hooks nativos** (comportamento do agente): vivem em `.claude/settings.json` via `PreToolUse`, `Stop`, `UserPromptSubmit`. Tratados em sub-etapa própria após 4.2.
2. **Princípios para retrospectiva/CLAUDE.md**: regras que dependem de bom senso humano ou de contexto que automação não captura. Documentação, não código.
3. **Processo manual de validação**: ex. "testar branch protection destrutivamente antes de concluir". Lembrete em documentação, não hook.

### Débito de configuração (não-hook) preservado

A seção "Débitos de configuração" deste documento (`application-prod.yml` ausente) **não é hook**. Continua válida e separada — débito de schema/config a resolver junto com a etapa de deploy.

---

## Hooks de setup / ambiente

- **Validar que scripts que invocam `mvnw spring-boot:run` passam `-Dspring-boot.run.profiles=<profile>` explicitamente.** (Etapa 3.3.1) Sem a flag, Spring usa profile `default` que não tem datasource → `Failed to configure a DataSource`. Hook leve: `grep -nE "mvnw\s+spring-boot:run" scripts/*.ps1 | grep -v "spring-boot.run.profiles"` deve retornar zero linhas.

## Hooks Markdown / docs

(Todos os itens deste grupo foram implementados ate 4.4 — encoding, blank lines, tamanho.)

## Hooks Maven / Java

- **Ordem "Lombok antes de MapStruct"** em `<annotationProcessorPaths>`. (Etapa 1.4) Inverter quebra build.
- **Versão de plugin Maven validada via Maven Central** antes de ser fixada. (Etapa 1.4) Não usar memória do agente — versões podem estar desatualizadas.
- **Modificacao de `@Entity` JPA existente exige migration Flyway no mesmo PR.** (Etapa 2.1, caso edge -- modo conservador na 4.7 cobre apenas Entity nova/status A; modificacao/status M produz falso positivo alto e ficou como debito explicito). Avaliar implementacao sofisticada (parser de diff `git diff --cached -U0`) se aparecer dor real.
- **Classe base de teste sem `abstract` em pacote de shared test.** (Etapa 2.1) Validar que classes base de teste em `src/test/java/.../shared/` têm modificador `abstract` — sem ele, JUnit tenta instanciar e duplica execuções.
- **`baseline-on-migrate: true` apenas em profiles de teste/dev.** (Etapa 2.1) Nunca em `application.yml` defaults ou `application-prod.yml`.
- **Sufixo de classe de teste segue padrão do projeto.** (Etapa 2.2) Sufixo `Test` (singular) para classes novas — `IT` não é usado neste projeto (Failsafe não configurado).

## Hooks GitHub Actions / CI

- **`mvnw` com bit de execução no git index.** (Etapa 1.5) `git update-index --chmod=+x mvnw` — sem isso, CI Linux falha com `Permission denied`. Esboço: validar `git ls-files --stage mvnw | grep -c "^100755"`.
- **Comandos em scripts/instruções para Windows não usam ferramentas Unix.** (Etapa 1.5) `tail`, `head`, `grep`, `sed`, `awk` não existem no PowerShell. Equivalentes: `Select-Object`, `Select-String`.
- **Toda configuração de branch protection passou por teste destrutivo.** (Etapa 1.5) PR proposital com CI falhando, confirmar bloqueio do merge — antes de declarar concluída.

## Hooks PowerShell

- **`Write-Error` seguido de `exit N` em arquivos `.ps1`.** (Etapa 2.6.1) Sob `$ErrorActionPreference = "Stop"`, `Write-Error` lança exceção terminating e nunca atinge `exit N` — exit code propaga errado em sessão dot-source. Padrão correto: `Write-Host -ForegroundColor Red` + `exit N`.
- **Comando nativo seguido de `if ($LASTEXITCODE -ne 0)` sem suspensão local de `$ErrorActionPreference`.** (Etapa 2.6.2) Indica risco do bug que a 2.6.2 corrigiu — stderr nativo vazando sob `Stop`. Esboço: `grep -B1 -A2 "if (\$LASTEXITCODE" scripts/*.ps1` revisado caso a caso.
- **Encoding UTF-8 sem BOM em `.ps1`.** (Etapa 2.6) BOM quebra algumas validações e pode afetar `javac` em arquivos vizinhos. Hook: primeiros bytes do arquivo ≠ `EF BB BF`.

## Hooks Frontend / Next.js

- **`npx shadcn@latest init --defaults` instala componente `button.tsx` automaticamente.** (Etapa 2.7) Em etapas que proíbem componentes, detectar e remover `src/components/ui/*.tsx` gerado pelo init antes do commit.
- **`AGENTS.md` ou `CLAUDE.md` em subdiretórios gerados por scaffold.** (Etapa 2.7) Quando framework gera, decidir conscientemente manter/remover. Conteúdo específico (avisos sobre training data desatualizada) tende a valer; conteúdo genérico tende a remover.

## Débitos de configuração

- **Containers Docker com `container_name:` fixo no `docker-compose.yml`.** (Descoberto no smoke test pós-merge da Sub-etapa 4.0, registrado na 4.0.1.) `financas-lab-postgres` e `financas-lab-redis` têm nome global no Docker daemon. Tentar subir um segundo clone em paralelo dispara conflito (`Error response from daemon: Conflict. The container name "/financas-lab-postgres" is already in use...`). Sem impacto em fluxo normal (1 clone por vez). Workaround manual: `docker rm -f financas-lab-postgres financas-lab-redis` antes de rodar `setup.ps1` no segundo clone. Resolver quando paralelismo de clones virar necessidade real (debugging em branch isolada com containers separados, smoke test sistematizado pós-merge, ou ambiente CI local rodando em paralelo). Fix: remover `container_name:` deixando Docker Compose gerar nomes prefixados pelo diretório do projeto. Custo estimado: 1-2h incluindo ajustes em qualquer script que referencie container por nome e revalidação destrutiva.

- **`application-prod.yml` não existe.** (Descoberto na 3.3.1) `decisoes.md` prescreve "Profiles sempre explícitos: dev, test, prod", mas o arquivo de prod nunca foi criado. Não bloqueante hoje (sem deploy prod), mas precisa ser criado quando deploy prod entrar no escopo. Resolver junto com a etapa de deploy.

## Debitos meta-operacionais

Itens descobertos na Sub-etapa 4.10 (auditoria meta-operacional). Nao sao hooks tradicionais — sao investigacoes/mitigacoes do ambiente Claude Code que afetam credibilidade de smoke tests e determinismo de invocacao. Categoria "descoberta a aprofundar".

- **Memoria global do Claude Code em `~/.claude/projects/<hash>/memory/`.** (Descoberto na 4.10.) 21 arquivos `.md` de feedback/sub-etapas, auto-memory ON, escreve sem confirmacao. Vetor de contaminacao cross-projeto opaco. Tres mitigacoes possiveis a avaliar: (1) desligar auto-memory globalmente em configuracao do Claude Code; (2) auditar conteudo dos 21 .md existentes para identificar o que veio do `financas-lab` vs outros projetos; (3) versionar politica de retencao no repo (ex: `docs/politica-memoria-claude.md`). Sem impacto em fluxo normal — afeta credibilidade de smoke tests onde determinismo de invocacao importa. Resolver antes do smoke da primeira skill orquestradora (4.11) se a credibilidade do smoke for criterio.

- **Investigar built-in agents do Claude Code.** (Descoberto na 4.10.) Cinco built-ins identificados nominalmente: `Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup`. Competem com subagents do projeto no espaco de delegacao do Claude principal. Investigar: (1) o que cada um faz; (2) quando dispara; (3) se pode ser desabilitado ou se compete sempre; (4) interacao com skills orquestradoras (ADR-012). Documentacao oficial: https://docs.claude.com/en/docs/claude-code/sub-agents (verificar antes de assumir comportamento). Resolver antes ou junto da 4.11 — afeta interpretacao do smoke do par skill+subagent.

- **Auditar plugins globais instalados.** (Descoberto na 4.10.) `code-review` e `frontend-design` identificados na Camada 0; smoke da 4.9.1 confirmou que `code-review` desabilitado localmente ainda afeta comportamento. Investigar: (1) listar plugins instalados globalmente (`claude plugin list` ou equivalente); (2) decidir manter/desabilitar/desinstalar caso a caso; (3) versionar decisao no repo. Decisao sobre plugin `code-review` oficial (criterio aberto da Camada 3) e parcialmente bloqueada por esta investigacao.

## Hooks implementados

Itens originalmente listados em "Hooks Markdown / docs" ou outras secoes, agora implementados e ativos no projeto. Mantidos aqui como historico de progresso da Camada 3.

- **Conventional Commits** (Sub-etapa 4.1, PR #40). Implementado em `.claude/hooks/universal/conventional-commits.ps1`, invocado via `.githooks/commit-msg` no evento `commit-msg`. Tipos: feat, fix, chore, docs, test, refactor, style, perf, build, ci. Scope opcional, breaking change via `!`, descricao minima 10 chars. Excecoes automaticas: merge e revert commits. Override consciente: `git commit --no-verify` documentado em `decisoes.md`. Entrypoint usa `powershell` (Windows PowerShell 5.1).
- **Encoding UTF-8** (Sub-etapa 4.2, PR #41). Implementado em `.claude/hooks/universal/encoding-utf8.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Whitelist por extensao e nome exato. Regra adicional: `.ps1` rejeita BOM (licao 2.6); outros tipos aceitam BOM. Binarios e tipos fora da whitelist sao ignorados.
- **Blank lines em Markdown** (Sub-etapa 4.3, PR #43). Implementado em `.claude/hooks/universal/markdown-blank-lines.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Valida headers de nivel 2-6 em arquivos `.md` (qualquer pasta). Nivel 1 ignorado; fronteira do arquivo e linha em branco implicita; blocos de codigo sao ignorados.
- **Maven release explicito** (Sub-etapa 4.5, PR #45). Implementado em `.claude/hooks/java-spring/maven-release.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Hook age apenas se `pom.xml` esta no diff staged. Valida presenca de pelo menos uma tag `<release>` no conteudo do `pom.xml` (qualquer valor interno aceito). Modo fail. Primeira ocupacao de `.claude/hooks/java-spring/`.
- **@Entity nova sem migration Flyway (modo conservador)** (Sub-etapa 4.7, PR #47). Implementado em `.claude/hooks/java-spring/entity-migration.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Hook age apenas se ha `.java` novo (status A) sob `src/main/java/` contendo `@Entity`. Exige pelo menos um arquivo `src/main/resources/db/migration/V<n>__*.sql` novo no mesmo commit. Valida presenca, nao conteudo. Modo fail. Modificacao de Entity existente (status M) **nao dispara** o hook -- caso edge registrado como debito em "Pendentes".
- **Tamanho de docs em `docs/` (modo warn)** (Sub-etapa 4.4, PR #44). Implementado em `.claude/hooks/universal/docs-size.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Limite: 800 linhas totais. Alerta visual em amarelo, **nao bloqueia commit**. Apenas `.md` em `docs/` — outros `.md` ignorados. Modo `warn` registrado como padrao para regras subjetivas em `decisoes.md`.

## Notas de cuidado para validacao destrutiva

Itens que nao sao hooks automatizaveis mas precisam ser observados em scripts e prompts futuros. Formalizados em ADR-011.

- **`[System.IO.File]::WriteAllText` com path relativo em PowerShell** grava em `[System.Environment]::CurrentDirectory`, nao em `$PWD`. Quando sessao fez `cd` para entrar no repo, esses caminhos divergem. Sincronizar previamente com `[System.Environment]::CurrentDirectory = (Get-Location).Path` ou usar path absoluto. Sem isso, arquivo vai parar em diretorio invisivel ao `git`. (Descoberto em smoke test pos-merge da 4.2, registrado na 4.2.1.)
- **`git commit` retornando `nothing to commit, working tree clean`** em validacao destrutiva e sinal de falso positivo, nao de "cenario nao se aplica". Indica que `git add` falhou silenciosamente — arquivo nao foi staged. Rodar `git status` antes de cada `git commit` em cenarios destrutivos e padrao consolidado.
- **`Test-Path` apos `WriteAllText`** e padrao obrigatorio para validacao destrutiva conforme ADR-011. Se retornar `False`, parar e investigar — nao prosseguir com `git add`/`git commit`.
- **Hook entity-migration (4.7) tem regex fragil para Java single-line.** Regex atual `(?m)^\s*@Entity\b` em `.claude/hooks/java-spring/entity-migration.ps1` exige `@Entity` no inicio de linha (apos whitespace de indentacao). Nao detecta Java single-line com `@Entity` no meio da linha (ex: `package x; import y.Entity; @Entity public class Foo {}`). Caso edge improvavel em producao (IDEs formatam, devs quebram linha apos `package`/`import`). **Mitigacao quando tocar no hook por outro motivo:** trocar regex por `@Entity\b` (sem ancora de linha). Word boundary `\b` ainda evita match em `@EntityListeners`, `@EntityGraph`, etc. Descoberto no smoke test pos-merge da 4.7 (cenario B usou Java single-line sintetico).

## Hooks de processo

- **Agente NÃO sugere "próxima etapa" espontaneamente após abrir PR.** (Etapa 1.4) Cada etapa termina com PR mergeado + `progresso.md` atualizado + sync local. Próxima etapa começa em discussão separada.
- **Agente NÃO toma decisões silenciosas em zona limítrofe.** (Etapas 2.4, 2.5, 2.6, 2.7) Decisão fora do escopo prescrito = parar e reportar, mesmo que solução pareça óbvia. Padrão consistente em 5+ etapas — hooks mecânicos vão substituir vigilância humana.
- **Validação destrutiva genuína exige código com lógica condicional real.** (Etapa 2.4) Configuração Spring/JPA atinge 100% JaCoCo com qualquer teste de contexto — validação plena de cobertura só na Camada 2 em diante, quando houver código com branches.
