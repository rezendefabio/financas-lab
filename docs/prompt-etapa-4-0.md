# Prompt — Etapa 4.0: Infraestrutura organizacional da Camada 3 (abre Camada 3)

## Contexto

A Camada 2 foi fechada em 2026-05-10 (PR #37 — saldo derivado da Conta). Três bounded contexts vivos ponta a ponta (`conta`, `categoria`, `transacao`), saldo cruzando bounded contexts via porta, ~280 testes, JaCoCo nos thresholds, Checkstyle e SpotBugs como gates obrigatórios.

Esta etapa **abre a Camada 3** (Configuração do Claude Code). Não implementa nenhum hook, subagent ou skill funcional. Estabelece a **infraestrutura organizacional** sobre a qual as próximas sub-etapas (4.1+) vão alimentar conteúdo:

1. Estrutura de pastas em `.claude/` separada por escopo de aplicabilidade (`universal/`, `java-spring/`, `windows/`, `next/`, `local/`).
2. Convenção de entrypoints de git hooks em `.githooks/` invocados via `core.hooksPath`.
3. `setup.ps1` aprende a configurar `core.hooksPath` automaticamente após clone.
4. Dois novos ADRs registram o layout (ADR-009) e o débito de portabilidade aceito conscientemente (ADR-010).
5. Triagem dos itens existentes em `hooks-pendentes.md`, marcando cada um com seu escopo de aplicabilidade — vira o mapa que as próximas sub-etapas usam para saber onde cada hook nasce.

Quando esta etapa terminar, o repo terá a "casa" pronta para receber hooks/agents/skills nas sub-etapas seguintes, mas nenhuma "mobília" funcional ainda.

## Padrões que estreiam nesta etapa

1. **Separação por escopo de aplicabilidade** — primeiro artefato do projeto a se organizar pensando em reuso futuro entre projetos (`universal/`) vs específicos de stack (`java-spring/`, `windows/`, `next/`) vs locais.
2. **Mecanismo `core.hooksPath`** — primeira configuração de git que muda comportamento padrão de hooks.
3. **Débito técnico de portabilidade registrado formalmente** — primeiro ADR explicitamente assumindo decisão consciente de custo futuro.
4. **Etapa sem código Java/Spring** — primeira etapa da Camada 2/3 que não toca em `src/`, `pom.xml`, ou migrations. Documentação, scripts e estrutura de pastas.

## Escopo decidido (calibrado com operador antes da redação)

### Estrutura criada

```
.claude/
├── hooks/
│   ├── universal/        (Conventional Commits, encoding UTF-8, Markdown, tamanho docs)
│   ├── java-spring/      (Maven, @Entity sem migration, sufixo Test, etc)
│   ├── windows/          (PowerShell: Write-Error+exit, Stop+stderr, BOM em .ps1)
│   ├── next/             (frontend hooks quando entrarem)
│   └── local/            (regras só deste projeto)
├── agents/
│   ├── universal/
│   ├── java-spring/
│   └── local/
└── skills/
    ├── universal/
    └── local/

.githooks/
└── README.md             (explica o padrão; entrypoints reais nascem em 4.1+)
```

**Notas:**

- Todas as pastas vazias têm `.gitkeep` para serem versionadas.
- `.githooks/` nasce **sem entrypoints funcionais** (`pre-commit`, `commit-msg`, etc.). Git é tolerante: `core.hooksPath` apontando para diretório sem hook específico simplesmente pula o hook. Entrypoints reais entram em 4.1 quando primeiro hook funcional nasce.
- `README.md` em `.githooks/` documenta o padrão de wrappers (arquivo sem extensão chama `.ps1` correspondente) para que próximas sub-etapas tenham referência.

### Decisões arquiteturais

- **`.claude/` é a casa da fábrica no projeto.** Organização interna por escopo de aplicabilidade documentada em ADR-009.
- **`.githooks/` é só entrypoint.** Lógica real fica em `.claude/hooks/*/`. Wrappers chamam scripts dentro de `.claude/hooks/`.
- **Promoção entre pastas exige evidência.** Item nasce em `local/`; promove para `universal/`, `java-spring/`, etc., apenas quando segundo contexto (ou critério explícito) provar aplicabilidade. Documentado em ADR-009.
- **PowerShell e Windows-specific aceitos como débito consciente** registrado em ADR-010. Critério de revisão: entrada da Camada 5 (VPS Linux) **ou** nascimento de 2ª fábrica em outro SO.
- **Claude Code hooks nativos (`PreToolUse`, `Stop`, etc.) ficam fora desta etapa.** São mecanismo diferente (`.claude/settings.json`), tratados em sub-etapa própria depois da 4.2.

### Localização dos arquivos modificados/criados

```
.claude/                                         ← novo (estrutura inteira)
.githooks/README.md                              ← novo
scripts/setup.ps1                                ← edição (+1 bloco para core.hooksPath)
docs/adrs.md                                     ← edição (+2 ADRs: 009 e 010)
docs/hooks-pendentes.md                          ← edição (triagem completa)
docs/decisoes.md                                 ← edição (histórico + nota Camada 3 iniciada)
docs/progresso.md                                ← edição (Camada 3 movida para 🟢 Em andamento + sub-etapa 4.0 + lições)
docs/prompt-etapa-4-0.md                         ← novo (este próprio prompt)
```

### Estrutura de pastas detalhada — comandos exatos

```bash
mkdir -p .claude/hooks/universal
mkdir -p .claude/hooks/java-spring
mkdir -p .claude/hooks/windows
mkdir -p .claude/hooks/next
mkdir -p .claude/hooks/local
mkdir -p .claude/agents/universal
mkdir -p .claude/agents/java-spring
mkdir -p .claude/agents/local
mkdir -p .claude/skills/universal
mkdir -p .claude/skills/local
mkdir -p .githooks
```

Cada pasta vazia recebe um `.gitkeep` vazio:

```bash
touch .claude/hooks/universal/.gitkeep
touch .claude/hooks/java-spring/.gitkeep
touch .claude/hooks/windows/.gitkeep
touch .claude/hooks/next/.gitkeep
touch .claude/hooks/local/.gitkeep
touch .claude/agents/universal/.gitkeep
touch .claude/agents/java-spring/.gitkeep
touch .claude/agents/local/.gitkeep
touch .claude/skills/universal/.gitkeep
touch .claude/skills/local/.gitkeep
```

### `.githooks/README.md`

```markdown
# .githooks — Entrypoints de git hooks

Esta pasta é apontada por `git config core.hooksPath` (configurado por `scripts/setup.ps1`).

**O que vai aqui:** entrypoints sem extensão (`pre-commit`, `commit-msg`, `pre-push`) que o git invoca diretamente. Cada entrypoint é um wrapper bash mínimo que chama o script `.ps1` correspondente.

**O que NÃO vai aqui:** lógica de validação. Lógica fica em `.claude/hooks/<escopo>/*.ps1` e é invocada pelos entrypoints.

## Padrão de wrapper (referência para etapas 4.1+)

Arquivo sem extensão (ex: `pre-commit`):

\```bash
#!/usr/bin/env bash
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
pwsh -NoProfile -ExecutionPolicy Bypass -File "$SCRIPT_DIR/pre-commit.ps1" "$@"
\```

Arquivo `.ps1` companheiro (ex: `pre-commit.ps1`):

\```powershell
$ErrorActionPreference = "Stop"
# Invoca scripts em .claude/hooks/<escopo>/ conforme aplicável.
# Exemplo: . "$PSScriptRoot/../.claude/hooks/universal/check-utf8.ps1"
\```

## Estado atual

Nenhum entrypoint funcional ainda. Sub-etapa 4.0 (esta) estabelece apenas a infraestrutura. Entrypoints reais nascem em 4.1+.

## Não tocar manualmente

`setup.ps1` configura `core.hooksPath` automaticamente. Em caso de clone novo ou reset, rodar `.\scripts\setup.ps1`.
```

### `scripts/setup.ps1` — edição

Adicionar bloco **antes** da finalização do script (antes da mensagem "Setup concluído"):

```powershell
# Configura core.hooksPath para apontar para .githooks/
Write-Host "Configurando core.hooksPath..." -ForegroundColor Cyan
git config core.hooksPath .githooks
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERRO] Falha ao configurar core.hooksPath." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] core.hooksPath configurado para .githooks/" -ForegroundColor Green
```

**Notas críticas:**

1. **Não usar `Write-Error` + `exit`.** Padrão consolidado da 2.6.1: `Write-Host -ForegroundColor Red` + `exit 1`.
2. **Verificar `$LASTEXITCODE`** após `git config` (comando nativo). Não precisa suspender `$ErrorActionPreference` aqui porque `git config` não escreve stderr em uso normal — mas se aparecer erro em validação, aplicar padrão da 2.6.2 (suspender `Stop` localmente).
3. **Idempotente.** Rodar `setup.ps1` duas vezes não causa erro — `git config` sobrescreve sem reclamar.
4. **Ler o arquivo inteiro antes de editar.** Localizar exatamente onde colocar o bloco (provavelmente antes do `Write-Host "Setup concluido"` final). Não duplicar nem desencaixar lógica existente.

### ADR-009 — Conteúdo a apendar em `docs/adrs.md`

Inserir após o ADR-008, antes do rodapé (se houver):

```markdown
## ADR-009 — Layout `.claude/` e mecanismo de hooks no Windows

**Status:** Aceito
**Data:** 2026-05-10

### Contexto

Abertura da Camada 3 (Configuração do Claude Code). O `hooks-pendentes.md` lista ~20 itens com escopos heterogêneos: alguns universais (Conventional Commits, encoding UTF-8), alguns específicos de stack (Maven `<release>`, sufixo `Test`), alguns específicos de SO (PowerShell `Write-Error` + `exit`), alguns específicos de framework (`shadcn init` deixando `button.tsx`). Tratar todos como iguais cria três problemas: dificulta separar o que serve a outros projetos (laboratório é também investimento em fábrica replicável), inflaciona o lookup do agente em cada validação, e mistura preocupações conceitualmente distintas. Ao mesmo tempo, criar repositório separado para a "fábrica" antes da primeira replicação real é abstração prematura (N=1 não ensina abstração).

### Decisão

**Casa única no `financas-lab/`, com separação interna por escopo de aplicabilidade.**

Estrutura:

\```
.claude/
├── hooks/
│   ├── universal/        (qualquer projeto)
│   ├── java-spring/      (preset stack Java/Maven/Spring)
│   ├── windows/          (Windows + PowerShell)
│   ├── next/             (Next.js)
│   └── local/            (só financas-lab)
├── agents/
│   ├── universal/
│   ├── java-spring/
│   └── local/
└── skills/
    ├── universal/
    └── local/
\```

**Mecanismo de git hooks no Windows:**

- `git config core.hooksPath .githooks` (configurado automaticamente por `scripts/setup.ps1`).
- Entrypoints em `.githooks/` são arquivos sem extensão (`pre-commit`, `commit-msg`, `pre-push`) — wrappers bash mínimos que invocam `pwsh -File .githooks/<nome>.ps1`. Git no Windows (Git Bash) interpreta o shebang `#!/usr/bin/env bash`.
- Lógica real fica em `.claude/hooks/<escopo>/*.ps1`, invocada pelos `.ps1` companheiros em `.githooks/`.

**Regra de promoção entre escopos:**

- Item nasce na pasta mais específica que comporta seu uso real (default: `local/`).
- Promove para escopo mais amplo (`java-spring/` → `universal/`) apenas após **evidência explícita** de aplicabilidade no escopo maior, registrada em commit ou ADR. Evidência mínima: segundo projeto/contexto provando reuso, ou justificativa técnica documentada.
- Promoção é decisão consciente, não automática. Não há ferramenta que "detecta" universalidade.

### Alternativas consideradas

- **Tudo plano em `.claude/hooks/` sem separação por escopo** — rejeitada porque não captura a heterogeneidade já presente em `hooks-pendentes.md` e dificulta reuso futuro. Adicionar disciplina retroativamente custa mais que nascer com ela.
- **Repositório separado `fabrica-ai-native/` consumido por cópia via `new-fabrica.ps1`** — considerada e rejeitada nesta fase. N=1 (este projeto único) não fornece evidência suficiente para validar a fronteira universal vs. stack vs. local. Decisão reversível: quando 2ª fábrica nascer ou itens em `universal/`/`java-spring/` estabilizarem, extrair para repo separado vira refactor barato.
- **Husky (Node.js) ou pre-commit framework (Python) em vez de PowerShell + `core.hooksPath`** — rejeitada para preservar coerência com a Camada 1 (scripts já são PowerShell, decisão da Camada 0 foi Windows nativo). Adicionar dependência nova ao ciclo de hooks é fricção desnecessária no atual estágio.
- **Claude Code hooks nativos (`PreToolUse`, `Stop`) como mecanismo único** — rejeitada como solução completa porque cobre apenas comportamento do agente, não validação de código pré-commit. Os dois mecanismos são complementares; Claude Code hooks entram em sub-etapa própria após 4.2.

### Consequências

**Aceitas:**

- Cinco pastas vazias com `.gitkeep` no nascimento — ruído visual inicial. Justificado pela disciplina que estabelece.
- Decisão sobre escopo de cada hook fica em humanos no momento da criação — não há tooling que valida automaticamente. Em troca, força reflexão explícita.
- Entrypoints em `.githooks/` exigirem wrapper bash + companheiro `.ps1` é dois arquivos por hook do git — verboso, mas idiomático no Windows + Git Bash.

**Ganhos:**

- Quando 2ª fábrica nascer, copia `hooks/universal/` + `agents/universal/` + `skills/universal/` e ignora o resto. Custo de replicação proporcional ao que de fato é replicável.
- Lookup do agente reduz: ao trabalhar em código Java, hooks em `next/` ou `windows/` são irrelevantes e nem precisam ser carregados se a invocação do agente filtrar por escopo.
- Mistura de preocupações fica visualmente clara — separação física é o gate.
- Reversibilidade preservada: estrutura interna pode virar repo separado depois sem refactor doloroso, porque a fronteira já está desenhada.
```

### ADR-010 — Conteúdo a apendar em `docs/adrs.md`

Inserir logo após o ADR-009:

```markdown
## ADR-010 — Débito de portabilidade: PowerShell e Windows-specific aceitos conscientemente

**Status:** Aceito
**Data:** 2026-05-10

### Contexto

Camada 0 decidiu Windows nativo + PowerShell + Docker Desktop como ambiente. A Camada 1 produziu 6 scripts `.ps1` em `scripts/` e dedicou 2 sub-etapas inteiras (2.6.1, 2.6.2) a bugs específicos do PowerShell. A Camada 3 vai produzir hooks e wrappers que também serão PowerShell-specific. Já existe risco previsto (Camada 5) de migração para VPS Linux para rodar routines de agente, momento em que parte dessa infraestrutura terá de ser reescrita em bash. A pergunta é: pagar custo de cross-platform agora (postura defensiva) ou aceitar custo futuro maior em troca de velocidade presente?

### Decisão

**Aceitar conscientemente o débito de portabilidade.** Manter scripts e hooks PowerShell-specific. Não introduzir abstração cross-platform (Node, Python como wrapper, Docker para tudo) preventivamente.

### Critério explícito de revisão

Esta decisão é revisitada quando **qualquer um** dos eventos abaixo ocorrer:

1. **Camada 5 entra em escopo** — abertura formal da decisão de subir VPS Linux para routines persistentes ou batch paralelo pesado.
2. **2ª fábrica nasce em outro SO** — quando segundo projeto adotar este modelo de fábrica em ambiente não-Windows.
3. **Dor concreta acumulada** — soma de tempo perdido em workarounds de PowerShell (medida em horas registradas em `progresso.md`) cruzar limiar subjetivo de "vale reescrever". Sem número fixo aqui; é gatilho de bom senso revisitado a cada retrospectiva de camada.

Quando qualquer um disparar, abrir ADR novo (superseder este) com nova decisão.

### Custo estimado da migração futura

- Reescrita de 6 scripts em `scripts/*.ps1` para `scripts/*.sh` — 4-8h.
- Reescrita de hooks em `.claude/hooks/windows/` e wrappers em `.githooks/` (quantidade depende do que a Camada 3 produzir) — 4-12h.
- Revalidação destrutiva manual de todos os fluxos no SO destino — 4-8h.
- **Total estimado: 1-3 dias de trabalho** num momento futuro escolhido conscientemente.

Estimativa intencionalmente otimista — assume que a lógica é estável e só o veículo muda. Se descobrir que padrões PowerShell-específicos vazaram para o desenho (não só sintaxe), o custo dobra.

### Mitigação enquanto débito vigora

- Cada hook em `.claude/hooks/windows/` traz comentário no topo identificando explicitamente que é Windows-only. Reduz surpresa futura.
- Lógica de hook fica enxuta nos `.ps1`; complexidade real fica em código que poderia ser portado (validação via `grep` padrão, leitura de bytes, etc.). PowerShell vira invólucro fino, não corpo da lógica.
- Estrutura `.claude/hooks/<escopo>/` já separa Windows-specific do resto. Migrar significa reescrever uma pasta, não auditar todo o projeto.

### Alternativas consideradas

- **Cross-platform desde já (Node/Python como veículo de hooks)** — rejeitada por custo presente certo em troca de ganho futuro hipotético. Postura inconsistente com decisão da Camada 0 (Windows nativo) e com princípio "stack on-distribution > stack que você acha bonita".
- **Tudo em Docker, hooks rodam em container** — rejeitada por inverter o eixo do problema (carrega complexidade massiva no presente para resolver portabilidade que pode nunca ser exercida).
- **Nada decidido formalmente, lidar quando bater** — rejeitada porque débito não registrado é débito que assombra como ansiedade difusa. Formalizar em ADR transforma "preocupação" em "tarefa futura com gatilho claro".

### Consequências

**Aceitas:**

- Quando a Camada 5 ou 2ª fábrica chegarem, 1-3 dias de trabalho de migração — custo conhecido em momento previsto.
- Contribuidores externos (improvável neste estágio, mas possível) precisariam de Windows + PowerShell para rodar localmente. Cobertura limitada.

**Ganhos:**

- Zero custo presente. Velocidade da Camada 3 não é comprometida por abstração defensiva.
- Coerência com decisões anteriores do projeto (Camada 0 e 1).
- Débito conhecido > ansiedade difusa. Decisão arquivada com critério de revisão; sai do plano mental ativo.
```

### `docs/hooks-pendentes.md` — triagem completa

Adicionar nova seção **após o cabeçalho explicativo** (após o `---` que segue "Itens **não estão implementados**..."), antes da seção "Hooks de setup / ambiente":

```markdown
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
```

### `docs/decisoes.md` — edição

**14a.** Adicionar nota em seção apropriada (provavelmente "Convenções de organização" ou criar seção "Camada 3 — Configuração do Claude Code"). Se a seção não existir, criar:

```markdown
## Camada 3 — Configuração do Claude Code

**Início:** 2026-05-10 (Sub-etapa 4.0).

### Layout de `.claude/`

`.claude/` é a casa da fábrica no projeto. Organizada por escopo de aplicabilidade conforme ADR-009: `universal/`, `java-spring/`, `windows/`, `next/`, `local/` dentro de `hooks/`, `agents/` e `skills/`. Promoção entre escopos exige evidência explícita (segundo contexto + decisão consciente).

### Mecanismo de git hooks no Windows

`git config core.hooksPath .githooks` configurado automaticamente por `scripts/setup.ps1`. Entrypoints em `.githooks/` são wrappers bash sem extensão chamando companheiros `.ps1`. Lógica real fica em `.claude/hooks/<escopo>/`.

### Débito de portabilidade

ADR-010 registra aceitação consciente: hooks e scripts PowerShell-specific. Migração avaliada ao entrar Camada 5 ou nascer 2ª fábrica em outro SO. Custo estimado: 1-3 dias.

### Claude Code hooks nativos

Mecanismo `PreToolUse`/`Stop`/`UserPromptSubmit` em `.claude/settings.json` é tratado em sub-etapa própria após 4.2. Diferente de git hooks: atua sobre comportamento do agente, não validação de código.
```

**14b.** Adicionar entrada no histórico:

```markdown
- **2026-05-10** — Sub-etapa 4.0 concluída: abertura da Camada 3 com infraestrutura organizacional. Criada estrutura `.claude/{hooks,agents,skills}/{universal,java-spring,windows,next,local}` com pastas vazias (`.gitkeep`). `.githooks/` criado com README explicativo (sem entrypoints funcionais ainda — esperam 4.1+). `setup.ps1` configura `core.hooksPath=.githooks` automaticamente. ADR-009 (layout `.claude/` e mecanismo de hooks no Windows) e ADR-010 (débito de portabilidade aceito conscientemente) registrados. Triagem completa do `hooks-pendentes.md` mapeando cada item ao escopo de aplicabilidade. Sem hooks funcionais, sem subagents, sem skills. Mergeado via PR #XX.
```

### `docs/progresso.md` — edição

**15a.** Atualizar "Última atualização": `2026-05-10 (Sub-etapa 4.0 — infraestrutura organizacional da Camada 3)`.

**15b.** Mudar status da Camada 3 na tabela geral de `⏸️ Aguardando` para `🟢 Em andamento`.

**15c.** Na seção "Camada 3 — Configuração do Claude Code", adicionar subseção "Sub-etapas concluídas" com:

```markdown
### Sub-etapas concluídas

- **4.0 — Infraestrutura organizacional** (2026-05-10): estrutura `.claude/` separada por escopo, `.githooks/` com `core.hooksPath` configurado por `setup.ps1`, ADR-009 e ADR-010, triagem do `hooks-pendentes.md`. Sem hooks/agents/skills funcionais. PR #XX.
```

**15d.** Adicionar seção "Lições da Sub-etapa 4.0" — só observações reais (placeholder se não houver):

```markdown
## Lições da Sub-etapa 4.0

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa — etapa de infraestrutura, sem geração de código que produzisse lição.)

### Lições de ambiente

(A preencher se houver durante execução — etapa de pasta e documentação, baixa probabilidade.)
```

**15e.** Adicionar entrada no histórico:

```markdown
- **2026-05-10** — Sub-etapa 4.0 concluída: abertura da Camada 3 com infraestrutura organizacional. Estrutura `.claude/` separada por escopo, `setup.ps1` configura `core.hooksPath`, ADR-009 e ADR-010 registrados, triagem do `hooks-pendentes.md`. Mergeado via PR #XX.
```

### Versionar este próprio prompt

`docs/prompt-etapa-4-0.md` no commit de docs.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra commit do squash da Etapa 3.8 (Camada 2 fechada, PR #37).
- `docs/prompt-etapa-4-0.md` presente como **untracked** (operador colocou antes de iniciar).
- Working tree limpo (exceto o prompt).
- **Não existe** pasta `.claude/` na raiz.
- **Não existe** pasta `.githooks/` na raiz.
- `git config core.hooksPath` retorna vazio ou erro (não configurado).

Validar com:

```bash
git status
git log --oneline -3
ls docs/prompt-etapa-4-0.md
ls -la .claude/ 2>&1 | head -3
ls -la .githooks/ 2>&1 | head -3
git config core.hooksPath
```

Esperado: working tree limpo (exceto prompt untracked), squash da 3.8 visível, `.claude/` e `.githooks/` ausentes, `core.hooksPath` vazio.

Se qualquer item divergir, **parar e reportar**.

## Tarefas

### Tarefa 1 — Validar pré-requisitos

Rodar comandos da seção "Estado esperado ao iniciar". Se algum item divergir, parar e reportar.

### Tarefa 2 — Criar branch de trabalho

```bash
git checkout -b chore/etapa-4-0-infra-claude
```

Nome `chore/` em vez de `feat/` — esta etapa não adiciona feature ao produto, apenas infraestrutura de processo.

### Tarefa 3 — Antes de escrever, ler arquivos vivos

```bash
cat scripts/setup.ps1
cat docs/adrs.md
cat docs/hooks-pendentes.md
cat docs/decisoes.md
cat docs/progresso.md
```

**Replicar fielmente os padrões observados.** Código vivo > esboço do prompt quando divergirem.

Atenção especial em `setup.ps1`: localizar onde colocar o bloco novo (próximo ao final, antes da mensagem de sucesso). Atenção em `adrs.md`: localizar o último ADR (ADR-008) e o final do arquivo para apendar ADR-009 e ADR-010 sem quebrar formatação.

### Tarefa 4 — Criar estrutura de pastas em `.claude/` e `.githooks/`

Executar os `mkdir -p` e `touch .gitkeep` da seção "Estrutura de pastas detalhada — comandos exatos".

Verificar com:

```bash
find .claude -type f -o -type d | sort
find .githooks -type f -o -type d | sort
```

Esperado: 13 subdiretórios sob `.claude/` (3 categorias × seus respectivos escopos: 5+3+2) com 10 `.gitkeep`s nas folhas; `.githooks/` vazio nesta tarefa (README na próxima).

### Tarefa 5 — Criar `.githooks/README.md`

Conteúdo conforme seção "`.githooks/README.md`" do escopo decidido. Encoding UTF-8 sem BOM.

### Tarefa 6 — Editar `scripts/setup.ps1`

Adicionar o bloco `core.hooksPath` na posição correta (antes da mensagem final de sucesso). Não mexer no resto.

**Validação destrutiva mínima local antes de commitar:**

```bash
# Limpar config existente (se houver)
git config --unset core.hooksPath 2>&1 | head -1
git config core.hooksPath

# Rodar setup.ps1
pwsh -File scripts/setup.ps1

# Verificar config
git config core.hooksPath
```

Esperado:
- Primeira `git config core.hooksPath` retorna vazio.
- `setup.ps1` roda até o fim sem erro (alguns passos podem demorar — docker compose, etc).
- Última `git config core.hooksPath` retorna `.githooks`.

**Se `setup.ps1` falhar por causa não relacionada ao bloco novo** (ex: Docker não rodando), validar isoladamente o bloco:

```bash
git config --unset core.hooksPath
git config core.hooksPath .githooks
git config core.hooksPath
```

Reportar no PR body.

### Tarefa 7 — Apendar ADR-009 e ADR-010 em `docs/adrs.md`

Conteúdo conforme seções "ADR-009" e "ADR-010" do escopo decidido. Posicionar após ADR-008. Encoding UTF-8.

**Atenção a Markdown:**
- Linha em branco antes e depois de cada `##` e `###` (lição da Etapa 1.1).
- Blocos de código com 3 crases corretas (cuidado com escapes do próprio prompt — no arquivo final, sem `\`).

### Tarefa 8 — Apendar triagem em `docs/hooks-pendentes.md`

Conteúdo conforme seção "`docs/hooks-pendentes.md` — triagem completa". Inserir após o cabeçalho explicativo, antes da seção "Hooks de setup / ambiente". Não remover ou modificar conteúdo existente do `hooks-pendentes.md` — apenas inserir a nova seção.

Atualizar a linha **"Última atualização:"** no topo do arquivo para:

```
**Última atualização:** 2026-05-10 (Sub-etapa 4.0 — triagem de escopo de aplicabilidade)
```

### Tarefa 9 — Editar `docs/decisoes.md`

Adicionar seção "Camada 3 — Configuração do Claude Code" conforme item 14a. Posicionar logo antes da seção "Princípios herdados do blueprint" (ou em local equivalente que faça sentido na estrutura existente).

Adicionar entrada no histórico no final do arquivo (seção "Histórico de mudanças") conforme item 14b.

### Tarefa 10 — Editar `docs/progresso.md`

Executar 15a, 15b, 15c, 15d, 15e conforme escopo decidido.

Verificação visual: a tabela "Status geral" no topo deve mostrar Camada 3 em `🟢 Em andamento`.

### Tarefa 11 — Versionar este próprio prompt

Mover/copiar `docs/prompt-etapa-4-0.md` para tracked (já está em `docs/`, basta `git add`).

### Tarefa 12 — Validar localmente

```bash
git status
find .claude -type f | sort
find .githooks -type f | sort
git config core.hooksPath
```

Esperado:
- 10 `.gitkeep`s sob `.claude/` + 1 `README.md` em `.githooks/`.
- `git config core.hooksPath` retorna `.githooks`.
- `git status` mostra (antes dos commits prescritos em "Estrutura de commits"):
  - **Novos:** 10 `.gitkeep`s sob `.claude/`, `.githooks/README.md`, `docs/prompt-etapa-4-0.md`.
  - **Modificados:** `scripts/setup.ps1`, `docs/adrs.md`, `docs/hooks-pendentes.md`, `docs/decisoes.md`, `docs/progresso.md`.

**Não rodar `mvnw clean verify` aqui** — esta etapa não toca em código Java. O CI vai rodar; se algo no projeto Java quebrar por causa desta etapa, é sinal de erro grave (não deveria acontecer).

**Possíveis pontos de atrito (parar e reportar):**

1. **`pwsh -File scripts/setup.ps1` não existir no ambiente do agente.** Se o agente roda em ambiente onde `pwsh` (PowerShell 7+) não está disponível mas só `powershell.exe` (Windows PowerShell 5.1), adaptar comando. Reportar se versão indisponível.
2. **`git config core.hooksPath` retornar algo diferente de `.githooks`** após rodar `setup.ps1`. Indica que o bloco novo não rodou ou rodou mas valor não persistiu. Parar e reportar.
3. **`.gitkeep` ignorado por `.gitignore`** — se o `.gitignore` do projeto ignora arquivos começando com `.`, os `.gitkeep`s não vão aparecer em `git status`. Verificar com `git check-ignore .claude/hooks/universal/.gitkeep`. Se ignorado, parar e reportar — talvez ajustar `.gitignore` ou usar arquivo `placeholder` com nome diferente.
4. **Tamanho do `adrs.md` ultrapassar limite após adicionar 2 ADRs**: o hook futuro de tamanho ainda não está ativo, mas se `adrs.md` ficar muito grande (>1500 linhas), reportar como nota para revisão.

### Tarefa 13 — Validação adicional do `setup.ps1`

Reproduzir um "clone novo" sem clonar de fato — apenas resetar a config e validar idempotência:

```bash
git config --unset core.hooksPath
git config core.hooksPath
pwsh -File scripts/setup.ps1
git config core.hooksPath
pwsh -File scripts/setup.ps1
git config core.hooksPath
```

Esperado:
- Após `--unset`: retorna vazio.
- Após 1ª execução: retorna `.githooks`.
- Após 2ª execução (idempotência): retorna `.githooks`, sem erro.

Se `setup.ps1` falhar por motivo não-relacionado (Docker, etc.), executar apenas o bloco do `core.hooksPath` isoladamente e reportar.

## Restrições e freios

1. **Não criar nenhum hook funcional.** Pastas `.claude/hooks/*/` permanecem vazias (só `.gitkeep`). Nenhum `.ps1` de validação. Nenhum entrypoint funcional em `.githooks/`. Essa parte é Sub-etapa 4.1+.

2. **Não criar nenhum subagent.** Pastas `.claude/agents/*/` permanecem vazias.

3. **Não criar nenhuma skill.** Pastas `.claude/skills/*/` permanecem vazias.

4. **Não criar `CLAUDE.md` na raiz do projeto.** Essa é Sub-etapa 4.3.

5. **Não criar template de CLAUDE.md ou script `new-fabrica.ps1`.** Hipótese de repo separado para fábrica foi rejeitada na discussão de calibração — não há fábrica separada nesta etapa.

6. **Não tocar em `src/`, `pom.xml`, `application*.yml`, `frontend/`, `docker-compose.yml`, migrations.** Esta etapa não toca em código Java/Spring/Next.

7. **Não tocar em scripts `.ps1` além de `setup.ps1`.** `dev.ps1`, `test.ps1`, `check.ps1`, `ship.ps1`, `test-integration.ps1` permanecem inalterados.

8. **Não tocar no `.gitignore`** a menos que `.gitkeep` esteja sendo ignorado (Tarefa 12, ponto de atrito 3). Nesse caso, parar e reportar antes de modificar.

9. **Não tocar em `.gitattributes`.**

10. **Não criar novos arquivos `.md` em `docs/`** além do próprio prompt. Não dividir `adrs.md` em múltiplos arquivos — apendar em arquivo único conforme padrão existente.

11. **Não relaxar nenhuma decisão registrada em ADRs anteriores.** Esta etapa apenas adiciona ADRs (009 e 010) — não revisa ADR-001 a ADR-008.

12. **Encoding UTF-8 sem BOM** em todos os arquivos criados. `.ps1` especialmente sensível (lição da 2.6).

13. **Linhas em branco antes e depois de headers Markdown** (lição da Etapa 1.1).

14. **Sem acentos em código** (mensagens do `setup.ps1` etc).

15. **Não usar `Write-Error` + `exit` em `setup.ps1`.** Padrão consolidado: `Write-Host -ForegroundColor Red` + `exit 1` (lição da 2.6.1).

16. **Não suspender `$ErrorActionPreference` em torno do `git config` a menos que validação destrutiva mostre necessidade.** `git config` em uso normal não dispara o padrão da 2.6.2.

17. **Não inicializar Git Bash, WSL ou qualquer ambiente além do PowerShell nativo do Windows.**

18. **Antes de escrever cada arquivo, ler a contraparte ou referência** (Tarefa 3). Código vivo > prompt.

19. **Validação destrutiva manual completa (clone novo, Docker parado, etc.)** é responsabilidade do operador, pós-merge. Documentar no PR body os cenários a validar.

20. **Não tomar decisão silenciosa em zona limítrofe.** Sétima recorrência prevista — esta etapa tem zonas limítrofes (formato exato do `.gitignore`, comportamento do `.gitkeep` em `git status`, nome do header da nova seção em `decisoes.md`). Se algo divergir do prescrito, **parar e reportar**.

21. **Não antecipar Sub-etapas 4.1+.** Sem rascunhar hook funcional, sem rascunhar subagent, sem rascunhar skill — nem mesmo em comentário.

22. **Não sugerir próxima etapa espontaneamente.** Esta etapa termina com PR aberto, CI verde, **aguardando autorização explícita** para merge.

23. **Lições da Sub-etapa 4.0 só registram observações reais.** Não inventar lição se não houver.

## Estrutura de commits

Branch: `chore/etapa-4-0-infra-claude`

**Commit 1** — `chore(claude): cria estrutura de pastas .claude/ e .githooks/`
- Arquivos novos: 10 `.gitkeep`s sob `.claude/` + `.githooks/README.md`.

**Commit 2** — `chore(scripts): setup.ps1 configura core.hooksPath`
- Edição em `scripts/setup.ps1` (+1 bloco).

**Commit 3** — `docs: ADR-009 (layout .claude) e ADR-010 (debito de portabilidade)`
- Edição em `docs/adrs.md` (apenda 2 ADRs).

**Commit 4** — `docs: triagem de escopo de aplicabilidade dos hooks pendentes`
- Edição em `docs/hooks-pendentes.md` (nova seção + atualiza data).

**Commit 5** — `docs: registra sub-etapa 4.0 (abertura Camada 3) em decisoes e progresso`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-4-0.md`

## Validação antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -6
git config core.hooksPath
```

Esperado:
- `check.ps1` passa (suite do projeto Java continua verde — esta etapa não impacta código).
- Working tree limpo.
- 5 commits novos.
- `core.hooksPath` retorna `.githooks`.

Se `check.ps1` quebrar com algo não relacionado a esta etapa (ex: Docker parado), reportar no PR body e seguir com PR mesmo assim — CI vai validar isoladamente.

## PR

Título: `chore: sub-etapa 4.0 — infraestrutura organizacional da Camada 3`

Body sugerido:

```markdown
## Summary

Abre a **Camada 3 (Configuração do Claude Code)** estabelecendo a infraestrutura organizacional sobre a qual as próximas sub-etapas vão alimentar hooks, subagents e skills funcionais.

**Esta etapa não cria nenhum hook, subagent ou skill funcional.** Cria apenas a casa: estrutura de pastas, mecanismo de invocação, ADRs registrando o desenho e o débito assumido, triagem mapeando cada hook pendente ao seu escopo.

### O que entra

- **Estrutura `.claude/`** com separação por escopo de aplicabilidade:
  - `hooks/`, `agents/`, `skills/` × `universal/`, `java-spring/`, `windows/`, `next/`, `local/` (combinações que fazem sentido).
  - 10 `.gitkeep`s preservam as pastas vazias no git.
- **`.githooks/README.md`** documentando o padrão de entrypoint (wrappers bash sem extensão chamando `.ps1` companheiros). Sem entrypoints funcionais ainda.
- **`setup.ps1`** ganha bloco que configura `git config core.hooksPath .githooks` automaticamente. Idempotente.
- **ADR-009** — layout `.claude/` + mecanismo de hooks no Windows (`core.hooksPath` + wrappers PowerShell). Define regra de promoção entre escopos.
- **ADR-010** — débito de portabilidade aceito conscientemente. PowerShell + Windows-specific. Critério de revisão: entrada da Camada 5 OU 2ª fábrica em outro SO. Custo estimado de migração: 1-3 dias.
- **`hooks-pendentes.md`** ganha seção "Escopo de aplicabilidade" mapeando cada item ao seu destino futuro em `.claude/hooks/<escopo>/`.

### O que não entra

- Hooks funcionais (4.1+).
- CLAUDE.md do projeto (4.3).
- Subagents (4.4 a 4.6).
- Skills (4.7+).
- Claude Code hooks nativos (`PreToolUse`, `Stop`) — sub-etapa própria após 4.2.
- Repositório separado de fábrica — hipótese rejeitada na calibração (N=1 não ensina abstração; reversível depois).

### Por que esta etapa primeiro

A Camada 1 produziu o padrão consolidado: **agente toma decisão silenciosa em zona limítrofe** apesar de instruções explícitas em prosa. Cinco ocorrências registradas (Etapas 2.4, 2.5, 2.6, 2.7, 3.6). Hooks mecânicos são o próximo nível de validação — substituem vigilância humana por gate automático. Estabelecer a infraestrutura antes dos hooks funcionais permite que as próximas sub-etapas sejam cirúrgicas (1 hook por sub-etapa) sem misturar decisões de plataforma com decisões de validação.

### Validação

- `scripts/setup.ps1` rodado duas vezes em sequência (validação de idempotência): `core.hooksPath` permanece `.githooks`.
- Working tree limpo após 5 commits.
- `check.ps1` passa (suite Java intocada).

### Validação destrutiva pós-merge sugerida

1. Clone novo do repo em diretório temporário.
2. Rodar `.\scripts\setup.ps1`.
3. Verificar `git config core.hooksPath` retorna `.githooks`.
4. Verificar `.claude/` presente com estrutura correta (`find .claude -type d | sort`).
5. Tentar commit qualquer — git deve aceitar (não há hook funcional, então não há rejeição).

### Próximo passo

Sub-etapa 4.1 (primeira leva de hooks funcionais — lote trivial mecânico: Conventional Commits, encoding UTF-8, linhas em branco em Markdown). Decisão fora deste PR.
```

## Pós-criação do PR

1. Abrir PR via `gh pr create`.
2. Capturar o número.
3. Editar `docs/decisoes.md` e `docs/progresso.md` substituindo `Mergeado via PR #XX` por `Mergeado via PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `chore/etapa-4-0-infra-claude` empurrada com 6 commits (5 + 1 update do PR).
- PR aberto, CI verde, **não mergeado**.
- `main` ainda no squash da 3.8.
- Working tree limpo.
- Reportar com `git log --oneline -6`, `git status`, `gh pr view --json number,state,statusCheckRollup` e parar.

## O que NÃO fazer ao terminar

- Não mergear o PR.
- Não criar prompt da Sub-etapa 4.1.
- Não rascunhar conteúdo de hooks funcionais, subagents ou skills.
- Não criar CLAUDE.md.
- Não tocar em `src/`, `pom.xml`, `application*.yml`, `frontend/`, `docker-compose.yml`, migrations, scripts além de `setup.ps1`.
- Não inflar `.githooks/` além do `README.md`.
- Não preencher `.gitkeep`s com conteúdo.
- Não sugerir "próximo passo" espontaneamente.
- Não relaxar ADRs anteriores.
- Não criar repo separado para fábrica.
