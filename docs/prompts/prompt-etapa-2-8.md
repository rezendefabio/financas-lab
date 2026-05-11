# Prompt — Etapa 2.8: Wrap-up Camada 1

## Contexto

A Etapa 2.7 foi concluída e fechada via PR #26. `main` está em `1b09e37`, working tree limpo. Frontend Next.js validado manualmente (`npm run dev` sobe, página default carrega).

Esta é a **última etapa da Camada 1**. Diferente das anteriores, **não cria código de produção**. Consolida o que foi feito, audita critérios de conclusão, registra retrospectiva e deixa a Camada 2 com fundação clara pra começar.

Trabalho desta etapa:

1. Auditar critérios de "Camada 1 concluída" definidos em `roadmap-camada-1.md` e marcar status real de cada um.
2. Validação destrutiva real: clonar o repo em pasta nova, rodar `scripts/setup.ps1`, cronometrar — confirma que critério "<10 min em máquina nova" passa.
3. Criar `docs/retrospectiva-camada-1.md` consolidando lições, padrões emergentes e o que faríamos diferente.
4. Criar `docs/hooks-pendentes.md` consolidando candidatos a hook das lições de cada etapa — vira input direto pra Camada 3.
5. Atualizar `progresso.md`: marcar Camada 1 como ✅ Concluída, ajustar pendências de Camada 0 que ficaram com `[ ]`, marcar critérios reais.
6. Atualizar `README.md` refletindo Camada 1 concluída + link pra retrospectiva.
7. Atualizar `decisoes.md` com entrada de fim de Camada 1.
8. Versionar este próprio prompt.

**Não cria código.** Não toca em `pom.xml`, `frontend/`, `scripts/`, `src/`. Apenas documentação.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra `1b09e37 feat: etapa 2.7 — inicializa frontend Next.js 16 em frontend/ (#26)`
- `docs/prompt-etapa-2-8.md` presente como untracked
- Working tree sem outras mudanças
- Docker Desktop pode estar rodando (Tarefa 2 valida `setup.ps1` — precisa de Docker)

Validar com `git status` e `git log --oneline -1` antes de começar.

## Tarefas

### Tarefa 1 — Auditar critérios de "Camada 1 concluída"

Localizar a seção **"Definição de 'Camada 1 concluída'"** em `docs/roadmap-camada-1.md` (linhas finais do arquivo). Os 6 critérios:

1. Você consegue clonar o repo numa máquina nova, rodar `.\scripts\setup.ps1` e ter tudo funcionando em menos de 10 minutos
2. CI verde no `main` há pelo menos 3 commits consecutivos
3. Cobertura JaCoCo nos thresholds
4. Pelo menos 1 PR foi rejeitado pelo CI por motivo legítimo (aprendizado)
5. Você confia que se CI está verde, código está mergeable sem segunda revisão linha a linha
6. `progresso.md` atualizado

Para cada critério, **levantar evidência concreta** (não inferir). Comandos sugeridos pra evidência:

**Critério 2 — CI verde nos últimos 3 commits:**

```bash
gh pr list --state merged --limit 5 --json number,title,headRefName,mergedAt | python3 -c "import sys, json; data = json.load(sys.stdin); [print(f\"PR #{p['number']}: {p['title'][:60]}\") for p in data]"
```

Confirmar manualmente que os PRs recentes (23, 24, 25, 26 pelo menos) tiveram check verde.

**Critério 3 — Cobertura JaCoCo:**

```bash
cd C:/projetos/financas-lab
./mvnw clean verify
# Ler target/site/jacoco/index.html ou aceitar que verify verde implica thresholds OK
```

**Critério 4 — PR rejeitado por CI legítimo:**

Recuperar do histórico (`progresso.md`, etapas 1.5 a 2.6.x) onde isso aconteceu. Lições da 1.2 mencionam PR proposital sendo bloqueado por branch protection. Lições da 2.4 e 2.5 mencionam validação destrutiva de gates.

**Critérios 1, 5, 6 — qualitativos:**

- Critério 1: validado pela Tarefa 2 abaixo (clone real)
- Critério 5: o operador responde com base em experiência. Não é métrica automática.
- Critério 6: validado pela Tarefa 5 (atualização de `progresso.md`)

**Reportar resultado da auditoria** antes de prosseguir. Se algum critério **não passar**, parar e discutir com operador antes de declarar Camada 1 concluída.

### Tarefa 2 — Validação destrutiva: clone novo + setup em <10 min

**Objetivo:** confirmar Critério 1 com evidência concreta. Não é simulação.

Procedimento:

```bash
# Cria pasta temporária fora do projeto atual
TEMP_DIR="/tmp/financas-lab-clone-test"
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"
cd "$TEMP_DIR"

# Cronometra clone
echo "=== START: $(date +%T) ==="
SECONDS=0

# Clone
git clone https://github.com/rezendefabio/financas-lab.git .
echo "=== Clone done: ${SECONDS}s ==="

# Setup (no PowerShell, via subprocess do agente, OU reportar pro operador rodar)
# Importante: bash_tool é bash, não PowerShell. setup.ps1 só roda em PowerShell.
# Agente NÃO tenta rodar .ps1 — reporta tempo de clone e instrui operador a rodar setup manualmente.

echo "=== Clone time: ${SECONDS}s ==="
echo ""
echo "Operador: rodar manualmente no PowerShell em $TEMP_DIR:"
echo "  cd $TEMP_DIR"
echo "  .\\scripts\\setup.ps1"
echo "  # cronometrar tempo total até 'Setup concluido com sucesso.'"
echo ""
echo "Reportar tempo total (clone + setup) ao agente."
```

Após o operador rodar `setup.ps1` manualmente e reportar o tempo, agente recebe valor e registra:

- Tempo total clone + setup: `<X> minutos`
- Bateu critério "<10 min"? sim/não

**Se demorar mais que 10 min**, registrar como débito técnico — não bloqueia conclusão da Camada 1, mas vira candidato a otimização (cache de Maven, imagens Docker pré-puxadas, etc).

Limpar a pasta temporária ao fim:

```bash
rm -rf /tmp/financas-lab-clone-test
```

### Tarefa 3 — Criar `docs/retrospectiva-camada-1.md`

Documento histórico/reflexivo. Estrutura sugerida:

```markdown
# Retrospectiva — Camada 1 (Infraestrutura de Confiança)

> Documento de reflexão sobre o que aprendemos construindo a Camada 1.
> Diferente do `progresso.md` (estado atual), este registro é histórico.
> Diferente do `decisoes.md` (escolhas concretas), este captura padrões emergentes e lições mais amplas.

**Período:** 2026-05-06 a 2026-05-08
**Etapas:** 1.1 a 2.8 (15 etapas, contando 2.6.1 e 2.6.2 como sub-etapas)
**PRs:** #1 a #27 (a número exato será conhecido após esta etapa abrir o PR final)

---

## O que funcionou

(Lista honesta. Itens prováveis — agente confirma e expande baseado nas lições reais registradas em `progresso.md`:)

- **Etapas pequenas com prompts cirúrgicos.** Cada etapa de 100-500 linhas de prompt, com restrições explícitas e mapa exato do que mudar. Quando o escopo era ambíguo, agente desviava — quando era cirúrgico, agente entregava.
- **Validação destrutiva como gate não-negociável.** Encontrou bugs que toda automação validou como verde (2.6.1, 2.6.2). Validação manual destrutiva é instrumento de qualidade de primeira linha.
- **Versionamento dos próprios prompts.** Cada `prompt-etapa-X.md` em `docs/`. Permite rastrear o que foi pedido vs o que foi entregue.
- **Procedimento `#XX → número real`** após PR aberto. Padrão internalizado pelo agente desde a 2.2.
- **Tool `Write` nativa do Claude Code > heredoc via shell** para criar arquivos. Lição da 2.6.

## O que foi mais difícil

- **Decisões silenciosas do agente em zona limítrofe.** Recorrente em todas as etapas: 2.2 (tentou pom.xml), 2.5 (reduziu fileExtensions), 2.6 (heredoc corrompido), 2.7 (`shadcn --defaults` em vez de interativo). Padrão raiz: agente prefere resolver dentro do escopo a parar e reportar quando solução parece "óbvia". Hooks mecânicos serão fundamentais quando os tivermos.
- **PowerShell tem armadilhas com `$ErrorActionPreference = "Stop"`.** Duas etapas inteiras dedicadas a fix (2.6.1 e 2.6.2). `Write-Error` + `exit` sob `Stop` engole exit code; comando nativo + redirecionamento de stderr sob `Stop` vaza stack trace.
- **Diagnóstico de bugs em script PowerShell exigiu reprodução isolada no terminal.** Inferir não funcionou. A solução da 2.6.2 só apareceu depois de testar 3 alternativas linha-a-linha no terminal direto.

## Padrões emergentes

- **Estrutura de prompts.** Convergiu para: Contexto → Estado esperado → Tarefas (numeradas) → Restrições → Estrutura de commits → Validação → PR → Pós-criação → Estado esperado ao terminar → O que NÃO fazer. Replicável.
- **Lições por etapa em `progresso.md`.** Seção dupla "Candidatos a hook" + "Lições de ambiente". Permite recuperar contexto rápido em etapas futuras.
- **Validação automática + manual.** Agente faz validação estática (grep, encoding, sintaxe). Operador faz validação destrutiva real (Docker parado, working tree sujo, branch == main, clone novo). Divisão clara de responsabilidades.

## O que faríamos diferente

- **Validar comandos em terminal direto antes de prescrever em prompt.** A 2.6 prescreveu `Write-Error` + `exit 1` sem testar em terminal — bug pegou na validação manual depois. Custo: 2 sub-etapas (2.6.1 e 2.6.2) corrigindo padrão que estava errado desde o início.
- **`AGENTS.md` por subdiretório como mecanismo de proteção contra training data desatualizada.** A decisão de manter o `frontend/AGENTS.md` na 2.7 mostrou um padrão útil que nem estava no roadmap. Vale incorporar como prática consciente — não esperar que cada framework gere o aviso pra gente.
- **Lições "candidatos a hook" desde o dia 1.** Coletadas mas dispersas em cada etapa. A 2.8 consolida em `hooks-pendentes.md`. Se tivéssemos feito isso desde a 1.1, hoje teríamos lista mais coesa.

## Para a Camada 2

Itens explicitamente fora do escopo da Camada 1, agora candidatos:

- Value object `Money` (compartilhado, `shared/domain/`)
- Bounded context `conta` (CRUD + uso de `Money`)
- Bounded context `categoria` (CRUD)
- Spring Security configurado de verdade (auth flow real, não só whitelist)
- Endpoints de auth (signup, login, refresh, logout)
- Primeira feature implementada manualmente do início ao fim, pra servir de referência aos agentes em Tier 2

A Camada 2 é onde o operador ainda escreve a maior parte do código manualmente, mas com fundação sólida pra delegar partes em Tier 2.

## Métricas

- **Etapas concluídas:** 15 (1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.6.1, 2.6.2, 2.7, 2.8)
- **PRs mergeados:** <preencher após o PR final desta etapa>
- **Bugs encontrados em validação manual destrutiva:** 2 (2.6.1, 2.6.2)
- **Sub-etapas de fix:** 2 (2.6.1, 2.6.2)
- **Linhas em `decisoes.md`:** <wc -l>
- **Linhas em `progresso.md`:** <wc -l>

(Agente preenche números reais usando `wc -l` antes do commit.)

---

## Princípios consolidados (vão pra Camada 2 e além)

1. **Validação manual destrutiva é não-negociável.** Não confiar só em sintaxe + CI.
2. **Reproduzir isoladamente antes de mexer no script.** Inferir custa tempo.
3. **Tool nativa > shell heredoc** pra criar arquivos com conteúdo grande.
4. **Cada decisão silenciosa do agente vira lição registrada.** Ao longo do tempo, vira regra ou hook.
5. **Etapa cirúrgica > etapa aberta.** Prompt curto e específico entrega; prompt longo e genérico desvia.
6. **`AGENTS.md`/`CLAUDE.md` contextual por subdiretório.** Mecanismo de proteção contra training data desatualizada — útil em qualquer framework que mude rápido.
7. **Working tree limpo entre etapas.** Cada etapa começa com `git status` limpo, termina com PR mergeado e working tree limpo. Disciplina de processo.
```

Adaptar conteúdo conforme lições reais registradas em `progresso.md` — esta é estrutura sugerida, não texto fixo. Lições reais variam.

### Tarefa 4 — Criar `docs/hooks-pendentes.md`

Consolidar todos os "Candidatos a hook" das lições de cada etapa em arquivo único. Estrutura:

```markdown
# Hooks Pendentes — Candidatos a Automatizar

> Lista consolidada de validações/regras que viraram lição em alguma etapa e foram registradas como "candidatos a hook".
> Input direto para a Camada 3 (Configuração do Claude Code), quando hooks formais entrarem.
> Atualizado conforme novas lições aparecem.

**Última atualização:** 2026-05-08 (consolidado durante Etapa 2.8)

---

## Como ler este documento

Cada item lista:
- **De onde veio:** etapa que registrou a lição
- **O que faz:** comportamento desejado do hook
- **Como detectar (esboço):** sintaxe shell ou critério de validação

Itens **não estão implementados** — são pendência pra Camada 3.

---

## Hooks Markdown / docs

(Coletados das lições da Etapa 1.1 e seguintes:)

- **Linhas em branco em Markdown.** Validar que arquivos `.md` modificados têm linhas em branco antes e depois de headers (`##`, `###`).
- **Encoding UTF-8 em arquivos de texto.** Validar que arquivos criados estão em UTF-8 (sem BOM em scripts `.ps1`; com ou sem BOM em outros — política a definir).
- **Conventional Commits.** Validar mensagem de commit (`feat:`, `fix:`, `chore:`, etc).
- **Tamanho de docs em `docs/`.** Alertar se algum `.md` em `docs/` ultrapassa limite (anti-enciclopédia).

## Hooks Maven / Java

(Coletados das lições 1.4 a 2.5:)

- **`<release>` em vez de `<source>` + `<target>`** no maven-compiler-plugin (validar uso idiomático Java 9+).
- **Ordem "Lombok antes de MapStruct"** em `<annotationProcessorPaths>`.
- **Versão de plugin Maven validada via Maven Central** antes de ser fixada (não memória do agente).
- **Modificação de `@Entity` JPA exige migration Flyway no mesmo PR.** Hook detecta diff em `@Entity` sem novo arquivo `Vn__*.sql`.
- **Sufixo `_test` em arquivos `.java` de teste segue padrão do projeto** (singular `Test`, não `IT` quando Failsafe não está configurado).

## Hooks GitHub Actions / CI

(Coletados das lições 1.5 e 2.7:)

- **`mvnw` com bit de execução no git index** (`git update-index --chmod=+x mvnw`) — sem isso, CI Linux falha com Permission denied.
- **Comandos em scripts/instruções para Windows não usam ferramentas Unix** (`tail`, `head`, `grep`, `sed`, `awk`). Equivalentes PowerShell: `Select-Object`, `Select-String`.
- **Toda configuração de branch protection ou required check passou por teste destrutivo** (PR proposital com CI falhando, confirmar bloqueio do merge) antes de declarada concluída.

## Hooks PowerShell

(Coletados das lições 2.6.1 e 2.6.2:)

- **`Write-Error` seguido de `exit N` em arquivos `.ps1`.** Sob `$ErrorActionPreference = "Stop"`, lança terminating exception e nunca atinge `exit N` — exit code propaga errado em sessão dot-source. Padrão correto: `Write-Host -ForegroundColor Red` + `exit N`.
- **Comando nativo (`docker`, `git`, `mvn`) seguido de `if ($LASTEXITCODE -ne 0)` em `.ps1`** sem suspensão local de `$ErrorActionPreference`. Indica risco do bug que a 2.6.2 corrigiu (stderr nativo vazando sob `Stop`).
- **Encoding UTF-8 sem BOM em `.ps1`** (BOM quebra `javac` em arquivos vizinhos e algumas validações). Hook: `xxd scripts/*.ps1 | head -1` confirmar primeiros bytes ≠ `EF BB BF`.

## Hooks Frontend / Next.js

(Coletados das lições 2.7:)

- **`AGENTS.md` ou `CLAUDE.md` em subdiretórios** — quando framework gera, decidir conscientemente manter/remover. Conteúdo específico (avisos sobre training data desatualizada) tende a valer; conteúdo genérico tende a remover.
- **shadcn/ui init com flags `--defaults`** — flag aceita tudo silenciosamente, mascarando defaults novos. Preferir interativo quando shadcn versionar comportamento.

## Hooks de processo

(Coletados das lições 2.4, 2.5, 2.6, 2.6.1, 2.7:)

- **Agente NÃO sugere "próxima etapa" espontaneamente após abrir PR.** Cada etapa termina com PR mergeado + `progresso.md` atualizado + sync local.
- **Agente NÃO toma decisões silenciosas em zona limítrofe.** Decisão fora do escopo prescrito = parar e reportar, mesmo que solução pareça óbvia. Padrão consistente em 5+ etapas — hooks mecânicos vão substituir vigilância humana.
- **Validação destrutiva genuína exige código não-trivial.** Configuração Spring/JPA atinge 100% de cobertura JaCoCo com qualquer teste — validação plena precisa de código com lógica condicional real (Camada 2 em diante).
```

Adaptar conforme lições reais. Esta estrutura é sugestão.

### Tarefa 5 — Atualizar `docs/progresso.md`

**5a.** Atualizar campo "Última atualização" no topo: `2026-05-08 (Etapa 2.8 — Camada 1 concluída)`.

**5b.** No bloco "Status geral", marcar:

```markdown
| **1** | Infraestrutura de confiança | ✅ Concluída |
```

**5c.** Auditar Camada 0 — itens marcados como `[ ]` que estão de fato concluídos:

- "Estrutura de pastas inicial criada" (linha presente desde a 1.1) — confirmar visualmente o que existe e marcar `[x]` se aplicável. Estruturas de pastas feitas: `docs/`, `scripts/`, `config/checkstyle/`, `config/spotbugs/`, `src/main/java/...`, `src/test/java/...`, `frontend/`. Aceitar como concluído.

- Outros itens da seção "Camada 0" que ficaram pendentes — auditar e ajustar.

**5d.** Na seção "Camada 1", marcar todos os critérios de "pronto" como `[x]` se de fato concluídos. Pelo menos:

- "Scripts PowerShell criados: ..." → `[x]` (já marcado na 2.6)
- "Projeto Next.js inicializado" → `[x]` (já marcado na 2.7)
- "Estrutura de pastas inicial criada" → `[x]` (auditar)
- Outros itens — auditar individualmente

**5e.** Adicionar nova subseção logo após a tabela de critérios da Camada 1:

```markdown
### Auditoria final (Etapa 2.8)

Critérios de "Camada 1 concluída" definidos em `roadmap-camada-1.md`:

| Critério | Status | Evidência |
|---|---|---|
| Clone novo + `setup.ps1` em <10 min | <preencher> | <tempo real medido na Tarefa 2 do prompt 2.8> |
| CI verde no `main` em ≥3 commits consecutivos | <preencher> | PRs #<X>, #<Y>, #<Z> mergeados com check verde |
| Cobertura JaCoCo nos thresholds | ✅ | `mvnw verify` na Tarefa 1 confirmou. BUNDLE 75%, infrastructure 60% |
| ≥1 PR rejeitado pelo CI por motivo legítimo | ✅ | Etapa 1.2: PR #12 bloqueado por branch protection (teste destrutivo). Etapas 2.4 e 2.5: validações destrutivas confirmaram CI falhando em violações reais |
| Operador confia que CI verde = código mergeable sem segunda revisão | <preencher> | <auto-avaliação do operador> |
| `progresso.md` atualizado | ✅ | Esta auditoria + outras seções refletem estado atual |

**Resultado:** Camada 1 <CONCLUÍDA / PENDENTE — preencher conforme auditoria>.

Ver `docs/retrospectiva-camada-1.md` para reflexão histórica e `docs/hooks-pendentes.md` para lista consolidada de candidatos a hook (input para Camada 3).
```

**5f.** Adicionar nova seção **"Lições da Etapa 2.8"** logo antes de **"Lições da Etapa 2.7"** (mantendo ordem decrescente). Conteúdo: lições reais observadas durante a execução da 2.8 (ex: divergência entre critérios prescritos e realidade, lacunas de docs descobertas durante auditoria).

Padrão pra preencher quando nada digno emergir:

```markdown
## Lições da Etapa 2.8

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

(Nenhuma nova nesta etapa.)
```

**5g.** Adicionar entrada no **"Histórico de mudanças deste documento"** no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.8 concluída: wrap-up Camada 1. Auditoria de critérios, retrospectiva criada (`docs/retrospectiva-camada-1.md`), hooks pendentes consolidados (`docs/hooks-pendentes.md`). Camada 1 marcada como ✅ Concluída. Mergeado via PR #XX.
```

### Tarefa 6 — Atualizar `README.md`

Localizar seção "Status" no `README.md`. Substituir:

```markdown
## Status

Camada 1 (Infraestrutura de confiança) — em andamento. Ver `docs/progresso.md`.
```

por:

```markdown
## Status

**Camada 1 (Infraestrutura de confiança) — ✅ Concluída** (2026-05-08).

Próxima: Camada 2 (Arquitetura otimizada para agentes).

Documentos relevantes:
- `docs/progresso.md` — estado atual da fábrica
- `docs/retrospectiva-camada-1.md` — reflexão sobre o que aprendemos
- `docs/hooks-pendentes.md` — candidatos a hook para Camada 3
```

### Tarefa 7 — Atualizar `decisoes.md`

Adicionar linha no **"Histórico de mudanças"** no fim do arquivo, no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.8 concluída: wrap-up Camada 1. Sem novas decisões técnicas. Documentos retrospectiva e hooks-pendentes criados. Camada 1 marcada como ✅ concluída.
```

### Tarefa 8 — Versionar este próprio prompt

Confirmar que `docs/prompt-etapa-2-8.md` está em disco como untracked e incluir no commit de docs.

## Restrições e freios

1. **Não tocar em código.** Esta etapa é puramente documentação. Arquivos permitidos:
   - `docs/progresso.md`
   - `docs/decisoes.md`
   - `docs/retrospectiva-camada-1.md` (novo)
   - `docs/hooks-pendentes.md` (novo)
   - `docs/prompt-etapa-2-8.md` (este arquivo)
   - `README.md`

2. **Não detalhar Camada 2.** Mencionar lista de itens previstos como "fora do escopo da Camada 1, candidatos para Camada 2" — sem priorização, sem estimativas, sem decisões de stack/abordagem. Detalhamento da Camada 2 é outra discussão.

3. **Não criar arquivo `roadmap-camada-2.md` ou similar.** Roadmap da Camada 2 fica para etapa específica de início da Camada 2.

4. **Não inventar lições nem métricas.** Se a Tarefa 1 (auditoria) revelar critério não atendido, registrar honestamente e parar. Se Tarefa 2 (clone novo) demorar mais que 10 min, registrar com tempo real.

5. **Não tocar em arquivos de outras etapas.** Em particular, não editar prompts antigos (`prompt-etapa-X.md`) — eles são histórico imutável.

6. **`bash_tool` é bash.** Não rodar `.ps1` direto. Tarefa 2 explicitamente delega execução de `setup.ps1` ao operador.

7. **Encoding UTF-8 sem BOM** em arquivos novos criados (retrospectiva, hooks-pendentes). Validar com `xxd` se necessário.

8. **Tamanho dos novos docs:** retrospectiva e hooks-pendentes são docs de **referência**. Nem precisam ser curtos como CLAUDE.md, mas evitar enciclopédia. Alvo: cada um <300 linhas.

9. **Lições da 2.8 só registram observações reais** (regra desde 2.5 — vale aqui também).

10. **Após Tarefa 2 reportar tempo + Tarefa 1 reportar auditoria, parar e aguardar confirmação do operador antes de declarar Camada 1 concluída.** Se algum critério não passou, conversar antes de marcar status.

## Estrutura de commits

Branch: `chore/wrap-up-camada-1`

Commits atômicos, em ordem:

**Commit 1** — `docs: cria retrospectiva e hooks-pendentes da Camada 1`
- `docs/retrospectiva-camada-1.md`
- `docs/hooks-pendentes.md`

**Commit 2** — `docs: marca Camada 1 como concluida em progresso, README e decisoes`
- `docs/progresso.md`
- `docs/decisoes.md`
- `README.md`
- `docs/prompt-etapa-2-8.md`

## Validação antes de abrir PR

```bash
# Working tree esperado:
git status

# Commits visiveis:
git log --oneline -5

# Confirma que arquivos novos estão lá:
ls docs/retrospectiva-camada-1.md docs/hooks-pendentes.md docs/prompt-etapa-2-8.md

# Confirma que README e decisoes refletem Camada 1 concluida:
grep -A3 "Camada 1" README.md
grep "2026-05-08" docs/decisoes.md | head -5
```

## PR

Título: `chore: etapa 2.8 — wrap-up Camada 1 (concluída)`

Body sugerido:

```markdown
## Summary

Encerra a Camada 1 da fábrica AI-native. Auditoria dos critérios de conclusão, retrospectiva, consolidação de hooks pendentes para Camada 3.

### Mudanças

- `docs/retrospectiva-camada-1.md` (novo): reflexão sobre o que funcionou, o que foi difícil, padrões emergentes, princípios consolidados.
- `docs/hooks-pendentes.md` (novo): lista consolidada de candidatos a hook coletados em todas as etapas. Input direto para Camada 3.
- `docs/progresso.md`: Camada 1 marcada como ✅ concluída. Auditoria final dos 6 critérios. Pendências de Camada 0 reconciliadas.
- `docs/decisoes.md`: linha no histórico.
- `README.md`: seção "Status" atualizada para refletir Camada 1 concluída + links para retrospectiva e hooks pendentes.

### Auditoria de critérios

(Preencher com resultado real da Tarefa 1 e 2 do prompt.)

| Critério | Status |
|---|---|
| Clone novo + setup em <10 min | <preencher> |
| CI verde em ≥3 commits | ✅ |
| Cobertura JaCoCo nos thresholds | ✅ |
| ≥1 PR rejeitado por CI legítimo | ✅ |
| Confiança em CI = mergeable | <preencher> |
| `progresso.md` atualizado | ✅ |

### Próxima

Camada 2 (Arquitetura otimizada para agentes). Início em etapa separada — prompt da 2.9 (ou início da Camada 2) será discutido após este merge.
```

## Pós-criação do PR

1. Abrir o PR via `gh pr create`.
2. Capturar o número.
3. Editar `docs/progresso.md` substituindo `Mergeado via PR #XX` por `Mergeado via PR #<numero-real>`.
4. Editar `docs/retrospectiva-camada-1.md` se algum lugar tiver `#XX` ou contagem de PRs (ex: "PRs #1 a #27") — atualizar com número real.
5. Commit: `docs: atualiza numero do PR no historico`
6. Push.
7. Esperar CI verde.
8. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- `main` local sincronizada com `origin/main`, incluindo squash commit da 2.8
- `git status` limpo
- `docs/retrospectiva-camada-1.md` e `docs/hooks-pendentes.md` criados
- `docs/progresso.md` reflete Camada 1 concluída
- `README.md` reflete Camada 1 concluída
- Branch `chore/wrap-up-camada-1` deletada local e remotamente

Reportar ao operador o estado final com `git log --oneline -3`, `git status`, e a tabela de auditoria preenchida. Parar.

## O que NÃO fazer ao terminar

- Não criar prompt da Camada 2 ou de início da próxima etapa
- Não tocar em código (`pom.xml`, `frontend/`, `scripts/`, `src/`)
- Não detalhar Camada 2
- Não inventar lições / métricas
- Não sugerir "próximo passo" espontaneamente — a Camada 2 começa em discussão separada com operador
