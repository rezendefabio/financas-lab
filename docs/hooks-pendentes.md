# Hooks Pendentes — Candidatos a Automatizar

> Lista consolidada de validações/regras que viraram lição em alguma etapa e foram registradas como "candidatos a hook".
> Input direto para a Camada 3 (Configuração do Claude Code), quando hooks formais entrarem.
> Atualizado conforme novas lições aparecem.

**Última atualização:** 2026-05-15 (Sub-etapa 5.51 -- hooks java-spring: baseline-on-migrate e ordem Lombok/MapStruct)

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

- **Versão de plugin Maven validada via Maven Central** antes de ser fixada. (Etapa 1.4) Não usar memória do agente — versões podem estar desatualizadas.
- ~~**Modificacao de `@Entity` JPA existente exige migration Flyway no mesmo PR.**~~ Implementado na 5.53 como hook warn -- ver secao "Hooks implementados".
- **Classe base de teste sem `abstract` em pacote de shared test.** (Etapa 2.1) Validar que classes base de teste em `src/test/java/.../shared/` têm modificador `abstract` — sem ele, JUnit tenta instanciar e duplica execuções.
- **Sufixo de classe de teste segue padrão do projeto.** (Etapa 2.2) Sufixo `Test` (singular) para classes novas — `IT` não é usado neste projeto (Failsafe não configurado).

## Hooks GitHub Actions / CI

- **`mvnw` com bit de execução no git index.** (Etapa 1.5) `git update-index --chmod=+x mvnw` — sem isso, CI Linux falha com `Permission denied`. Esboço: validar `git ls-files --stage mvnw | grep -c "^100755"`.
- **Comandos em scripts/instruções para Windows não usam ferramentas Unix.** (Etapa 1.5) `tail`, `head`, `grep`, `sed`, `awk` não existem no PowerShell. Equivalentes: `Select-Object`, `Select-String`.
- **Toda configuração de branch protection passou por teste destrutivo.** (Etapa 1.5) PR proposital com CI falhando, confirmar bloqueio do merge — antes de declarar concluída.

## Hooks PowerShell

- **Comando nativo seguido de `if ($LASTEXITCODE -ne 0)` sem suspensão local de `$ErrorActionPreference`.** (Etapa 2.6.2) Indica risco do bug que a 2.6.2 corrigiu — stderr nativo vazando sob `Stop`. Esboço: `grep -B1 -A2 "if (\$LASTEXITCODE" scripts/*.ps1` revisado caso a caso.
- **Encoding UTF-8 sem BOM em `.ps1`.** (Etapa 2.6) BOM quebra algumas validações e pode afetar `javac` em arquivos vizinhos. Nota: ja coberto por `.claude/hooks/universal/encoding-utf8.ps1` (Sub-etapa 4.2) que rejeita BOM em `.ps1`.

## Hooks Frontend / Next.js

(Todos os itens deste grupo foram implementados na 5.56 -- shadcn-artifacts.ps1.)

## Débitos de configuração

- **Containers Docker com `container_name:` fixo no `docker-compose.yml`.** (Descoberto no smoke test pós-merge da Sub-etapa 4.0, registrado na 4.0.1.) `financas-lab-postgres` e `financas-lab-redis` têm nome global no Docker daemon. Tentar subir um segundo clone em paralelo dispara conflito (`Error response from daemon: Conflict. The container name "/financas-lab-postgres" is already in use...`). Sem impacto em fluxo normal (1 clone por vez). Workaround manual: `docker rm -f financas-lab-postgres financas-lab-redis` antes de rodar `setup.ps1` no segundo clone. Resolver quando paralelismo de clones virar necessidade real (debugging em branch isolada com containers separados, smoke test sistematizado pós-merge, ou ambiente CI local rodando em paralelo). Fix: remover `container_name:` deixando Docker Compose gerar nomes prefixados pelo diretório do projeto. Custo estimado: 1-2h incluindo ajustes em qualquer script que referencie container por nome e revalidação destrutiva.

- **`application-prod.yml` não existe.** (Descoberto na 3.3.1) `decisoes.md` prescreve "Profiles sempre explícitos: dev, test, prod", mas o arquivo de prod nunca foi criado. Não bloqueante hoje (sem deploy prod), mas precisa ser criado quando deploy prod entrar no escopo. Resolver junto com a etapa de deploy.

## Debitos meta-operacionais

Itens descobertos na Sub-etapa 4.10 (auditoria meta-operacional). Nao sao hooks tradicionais — sao investigacoes/mitigacoes do ambiente Claude Code que afetam credibilidade de smoke tests e determinismo de invocacao. Categoria "descoberta a aprofundar".

- **Memoria global auto-ON sem confirmacao.** (Identificado 4.10, re-classificado 4.15 apos auditoria empirica.) `~/.claude/projects/<hash>/memory/` armazena memoria derivada sem confirmacao explicita do operador. **Magnitude real apos auditoria: ~85 KB total** (somando todos os projetos do operador) -- fracao minima dos ~427 MB que `~/.claude/projects/` ocupa. A 4.10 confundiu memoria (~85 KB de resumos) com transcripts (~427 MB de logs brutos). Questao de principio ("auto-ON sem confirmacao") permanece valida; impacto pratico minimo. Acao: se aparecer dor concreta (ex: vazamento percebido de informacao entre projetos, ou comportamento inesperado vinculado a memoria), revisitar e abrir sub-etapa de mitigacao. Sem necessidade de mitigacao imediata.

- **Built-in agents do Claude Code potencialmente competindo com subagents custom.** (Identificado 4.10, re-classificado 4.15.) Built-ins documentados (`Explore`, `Plan`, `general-purpose`, `claude-code-guide`, `statusline-setup`) podem teoricamente competir com subagents custom (`pr-reviewer` Haiku, `architect-reviewer` Sonnet). **Sob observacao, sem dor pratica relatada.** Operador nao observou em sessoes recentes (maio/2026) delegacao para built-ins nem interferencia com subagents custom. Debito teorico, nao pratico. Acao: se aparecer caso onde Claude principal delega para built-in em vez de subagent custom esperado, registrar contexto especifico e abrir sub-etapa de mitigacao.

- **Transcripts em `~/.claude/projects/<hash>/<conversa-hash>/` sem rotina de expiracao.** (Achado 4.15 via auditoria empirica.) ~427 MB acumulados em 17 projetos do operador. Maior conversa unica: 90 MB. Sem mecanismo automatico de expiracao por idade ou limpeza. **Fora do escopo do projeto `financas-lab`** -- gestao de storage do Claude Code e decisao pessoal sobre setup, nao do projeto. Registrado para visibilidade. Acao: nenhuma do lado do projeto. Operador pode considerar limpeza periodica de transcripts antigos no proprio fluxo (fora desta sub-etapa).

## Agentes e skills

- **test-writer (extensao frontend)** (Sub-etapa 5.13). Detecta path `frontend/` e gera Vitest + Testing Library no lugar de JUnit. Categorias: componente (`src/app/**/*.tsx`, `src/components/**/*.tsx`), hook (`src/hooks/**/*.ts`), service/utility (`src/services/**/*.ts`, `src/lib/**/*.ts`). Arquivo de teste colocado no mesmo diretorio do alvo. Validacao: `npm run test:run` em `frontend/` via Push-Location/Pop-Location. Recusa silenciosa se arquivo de teste ja existir. Agente: `.claude/agents/test-writer.md` (extensao aditiva -- logica Java intacta).

- **front-reviewer** + **/review-front** (Sub-etapa 5.12). Caminhos: `.claude/agents/front-reviewer.md`, `.claude/skills/review-front/SKILL.md`. Revisor de codigo frontend especializado nas convencoes do projeto. 5 bloqueadores (B1 fetch fora de services/, B2 asChild em base-nova, B3 URL hardcoded de ambiente, B4 `any` em tipos de API, B5 credencial literal), 4 sugestoes (S1 console.log em producao, S2 hook/componente sem teste, S3 acesso a token fora de auth.ts/auth-provider, S4 props sem tipo explicito), 3 elogios (E1 render prop correto, E2 apiFetch em vez de fetch, E3 ApiError para tipagem). Condicional no /ship (Passo 5, Review 3): so invocado se ha arquivos `frontend/` na branch.

## Scripts de gate

Scripts de validacao que rodam como gate de qualidade (nao sao git hooks, mas sao invocados pelo /ship antes do push).

- **check-front.ps1** (Sub-etapa 5.11). Caminho: `scripts/check-front.ps1`. Gate de qualidade do frontend: executa `npm run lint`, `npm run test:run` e `npm run build` em `frontend/`, nessa ordem (fail-fast: mais rapido primeiro). Qualquer etapa com exit != 0 interrompe e impede o push. Modo **fail** (nenhuma etapa pode ser warn -- lint, testes e build sao objetivos). Invocado pelo /ship no Passo 1.1 de forma condicional: so roda se houver arquivos `frontend/` na branch (PRs puramente de backend nao sao penalizados).

## Hooks implementados

Itens originalmente listados em "Hooks Markdown / docs" ou outras secoes, agora implementados e ativos no projeto. Mantidos aqui como historico de progresso da Camada 3.

- **Conventional Commits** (Sub-etapa 4.1, PR #40). Implementado em `.claude/hooks/universal/conventional-commits.ps1`, invocado via `.githooks/commit-msg` no evento `commit-msg`. Tipos: feat, fix, chore, docs, test, refactor, style, perf, build, ci. Scope opcional, breaking change via `!`, descricao minima 10 chars. Excecoes automaticas: merge e revert commits. Override consciente: `git commit --no-verify` documentado em `decisoes.md`. Entrypoint usa `powershell` (Windows PowerShell 5.1).
- **Encoding UTF-8** (Sub-etapa 4.2, PR #41). Implementado em `.claude/hooks/universal/encoding-utf8.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Whitelist por extensao e nome exato. Regra adicional: `.ps1` rejeita BOM (licao 2.6); outros tipos aceitam BOM. Binarios e tipos fora da whitelist sao ignorados.
- **Blank lines em Markdown** (Sub-etapa 4.3, PR #43). Implementado em `.claude/hooks/universal/markdown-blank-lines.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Valida headers de nivel 2-6 em arquivos `.md` (qualquer pasta). Nivel 1 ignorado; fronteira do arquivo e linha em branco implicita; blocos de codigo sao ignorados.
- **Maven release explicito** (Sub-etapa 4.5, PR #45). Implementado em `.claude/hooks/java-spring/maven-release.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Hook age apenas se `pom.xml` esta no diff staged. Valida presenca de pelo menos uma tag `<release>` no conteudo do `pom.xml` (qualquer valor interno aceito). Modo fail. Primeira ocupacao de `.claude/hooks/java-spring/`.
- **@Entity nova sem migration Flyway (modo conservador)** (Sub-etapa 4.7, PR #47). Implementado em `.claude/hooks/java-spring/entity-migration.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Hook age apenas se ha `.java` novo (status A) sob `src/main/java/` contendo `@Entity`. Exige pelo menos um arquivo `src/main/resources/db/migration/V<n>__*.sql` novo no mesmo commit. Valida presenca, nao conteudo. Modo fail. Modificacao de Entity existente (status M) **nao dispara** o hook -- caso edge registrado como debito em "Pendentes".
- **Tamanho de docs em `docs/` (modo warn)** (Sub-etapa 4.4, PR #44; refinado pela 4.14 em PR #58). Implementado em `.claude/hooks/universal/docs-size.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Limite: 800 linhas totais. Alerta visual em amarelo, **nao bloqueia commit**. Apenas `.md` em `docs/` — outros `.md` ignorados. **A partir da 4.14:** `docs/prompts/` excluido da verificacao (prompts versionados sao registros historicos por natureza; tamanho nao e criterio de qualidade). Modo `warn` registrado como padrao para regras subjetivas em `decisoes.md`.
- **Secret Scanning** (Sub-etapa 5.10). Implementado em `.claude/hooks/universal/secret-scanning.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Monitora extensoes `.java`, `.ts`, `.tsx`, `.js`, `.jsx`, `.properties`, `.yml`, `.yaml`, `.json`. Aplica 6 padroes (P1 chave PEM privada, P2 AWS Access Key ID, P3 GitHub token, P4 OpenAI/Anthropic API key, P5 password literal, P6 secret/apiKey literal). Exclusoes: `src/test/` (senhas de teste esperadas), arquivos `*.example` e `*-example.*`. P5 e P6 ignoram valores que comecam com `$` ou `{` (placeholders Spring/env). Modo **fail** (secret scanning nunca pode ser warn).

- **baseline-on-migrate apenas em test/dev** (Sub-etapa 5.51). Implementado em `.claude/hooks/java-spring/baseline-on-migrate.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Age apenas em `application*.yml` staged em `src/main/resources/`. Permite `baseline-on-migrate: true` somente em `application-test.yml` e `application-dev.yml`. Modo fail. Licao 2.1: em producao, `baseline-on-migrate: true` faz o Flyway marcar todas as migrations existentes como ja executadas, resultando em tabelas faltando no schema real.

- **Ordem Lombok antes de MapStruct em annotationProcessorPaths** (Sub-etapa 5.51). Implementado em `.claude/hooks/java-spring/lombok-mapstruct-order.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Age apenas quando `pom.xml` esta staged. Extrai o bloco `<annotationProcessorPaths>` e compara posicoes de `lombok` e `mapstruct-processor` via `.IndexOf()`. Bloqueia se `mapstruct-processor` aparece antes de `lombok`. Modo fail. Licao 1.4: MapStruct precisa dos metodos gerados pelo Lombok (getters, setters, builders) no momento do processamento -- ordem invertida quebra o build de forma nao-obvia.

- **@Entity modificada com campo novo avisa sobre migration Flyway (modo warn)** (Sub-etapa 5.53). Implementado em `.claude/hooks/java-spring/entity-migration-modified.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Complementa o hook 4.7 (que cobre Entity nova/status A). Usa `git diff --cached -U0` para inspecionar linhas adicionadas em arquivos `.java` modificados (status M) que contenham `@Entity`. Dispara se alguma linha adicionada corresponde a campo novo: `private\s+\w`, `@Column`, `@Id` ou `@Embedded`. Modo **warn** (exit 0 sempre): falsos positivos possiveis em refactors de campos existentes; desenvolvedor decide se precisa de migration. Exibe AVISO amarelo listando arquivos suspeitos e orienta criacao de `ALTER TABLE ADD COLUMN` ou ignora se for refactor puro.

- **Artefatos de scaffold Next.js/shadcn (modo warn)** (Sub-etapa 5.56). Implementado em `.claude/hooks/next/shadcn-artifacts.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Aviso 1: detecta `frontend/src/components/ui/button.tsx` staged -- `npx shadcn@latest init --defaults` instala esse componente automaticamente; modo warn sugere remocao consciente. Aviso 2: detecta `AGENTS.md` ou `CLAUDE.md` com path comecando por `frontend/` -- frameworks de scaffold geram esses arquivos; modo warn sugere revisao e decisao consciente. Ambos os avisos em modo **warn** (exit 0): decisao de manter/remover e humana. CLAUDE.md na raiz e arquivos frontend comuns nao ativam o hook.

- **Hook post-edit unit tests** (Sub-etapa 4.22, PR #68). Hook nativo Claude Code
  (`PostToolUse`) em `.claude/hooks/post-edit/run-tests.ps1`, referenciado por
  `.claude/settings.json` (gitignored, gerado pelo `setup.ps1`). Dispara apos `Edit`
  ou `Write` em `*/domain/*.java` dentro de `src/main/java/`. Roda `mvnw test
  -Dtest=<Classe>Test` se arquivo de teste existir; silencioso caso contrario.
  Timeout 60s. Non-blocking (PostToolUse nao bloqueia por design). Escopo futuro:
  integration tests para `*RepositoryImpl.java` se performance permitir.

- **Write-Error seguido de exit em .ps1** (Sub-etapa 5.55). Implementado em `.claude/hooks/windows/write-error-exit.ps1`, invocado via `.githooks/pre-commit` (orquestrador) no evento `pre-commit`. Filtra arquivos `.ps1` staged (qualquer path). Para cada `.ps1`, percorre linhas buscando `Write-Error`; se encontrado, verifica janela de 5 linhas seguintes para `exit`. Se padrao detectado, exibe aviso em amarelo com explicacao do problema e substituicao recomendada (`Write-Host -ForegroundColor Red + exit N`), mas NAO bloqueia (exit 0). Modo **warn** (heuristica -- analise de fluxo completa requerida para certeza; aviso serve para revisao humana). Primeira ocupacao de `.claude/hooks/windows/`.

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
