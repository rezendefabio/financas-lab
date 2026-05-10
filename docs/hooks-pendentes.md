# Hooks Pendentes — Candidatos a Automatizar

> Lista consolidada de validações/regras que viraram lição em alguma etapa e foram registradas como "candidatos a hook".
> Input direto para a Camada 3 (Configuração do Claude Code), quando hooks formais entrarem.
> Atualizado conforme novas lições aparecem.

**Última atualização:** 2026-05-10 (Sub-etapa 4.0.1 — fix posição core.hooksPath + débito container_name)

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

- **Linhas em branco em Markdown.** (Etapa 1.1) Validar que arquivos `.md` modificados têm linhas em branco antes e depois de headers (`##`, `###`). Sem isso, alguns renderers não reconhecem o header.
- **Encoding UTF-8 em arquivos de texto.** (Etapa 1.1) Validar que arquivos criados estão em UTF-8 (sem BOM em scripts `.ps1`; com ou sem BOM em outros).
- **Conventional Commits.** (Etapa 1.1) Validar mensagem de commit (`feat:`, `fix:`, `chore:`, `docs:`, etc).
- **Tamanho de docs em `docs/`.** (Etapa 1.1) Alertar se algum `.md` em `docs/` ultrapassa limite (anti-enciclopédia).

## Hooks Maven / Java

- **`<release>` em vez de `<source>` + `<target>`** no maven-compiler-plugin. (Etapa 1.4) Validar uso idiomático Java 9+.
- **Ordem "Lombok antes de MapStruct"** em `<annotationProcessorPaths>`. (Etapa 1.4) Inverter quebra build.
- **Versão de plugin Maven validada via Maven Central** antes de ser fixada. (Etapa 1.4) Não usar memória do agente — versões podem estar desatualizadas.
- **Modificação de `@Entity` JPA exige migration Flyway no mesmo PR.** (Etapa 2.1) Hook detecta diff em `@Entity` sem novo arquivo `Vn__*.sql`.
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

## Hooks de processo

- **Agente NÃO sugere "próxima etapa" espontaneamente após abrir PR.** (Etapa 1.4) Cada etapa termina com PR mergeado + `progresso.md` atualizado + sync local. Próxima etapa começa em discussão separada.
- **Agente NÃO toma decisões silenciosas em zona limítrofe.** (Etapas 2.4, 2.5, 2.6, 2.7) Decisão fora do escopo prescrito = parar e reportar, mesmo que solução pareça óbvia. Padrão consistente em 5+ etapas — hooks mecânicos vão substituir vigilância humana.
- **Validação destrutiva genuína exige código com lógica condicional real.** (Etapa 2.4) Configuração Spring/JPA atinge 100% JaCoCo com qualquer teste de contexto — validação plena de cobertura só na Camada 2 em diante, quando houver código com branches.
