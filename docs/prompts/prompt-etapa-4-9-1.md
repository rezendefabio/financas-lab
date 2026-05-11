# Prompt — Etapa 4.9.1: Refinamento do `pr-reviewer` pos-smoke (template + few-shot)

## Contexto

Camada 3 com 6 hooks + CLAUDE.md + blueprint + 1 subagent (`pr-reviewer` Haiku) apos a 4.9. Smoke test pos-merge da 4.9 **passou no essencial** — subagent foi invocado proativamente, executou, produziu output util. Mas o output **divergiu do template prescrito**:

- Template prescrito: 3 secoes (Bloqueadores, Sugestoes, Elogios).
- Output real: 4 secoes (Visao Geral, Analise das Mudancas, Itens Especificos, Conclusao).

Subagent improvisou estrutura propria. Nao e bug — e comportamento divergente do que o prompt do subagent pediu.

**Esta sub-etapa entrega refinamento do `pr-reviewer.md`** baseado em evidencia empirica do smoke. Categoria nova: **"refinamento de componente baseado em smoke empirico"** — diferente de "registro pos-smoke falho" (4.2.1, 4.7.1), pois smoke funcionou; so revelou comportamento subotimo do componente.

Caracteristicas:

1. **Sub-etapa de refactor, nao doc-only.** Refina componente funcional (`pr-reviewer.md`). Branch prefixo `refactor/`.

2. **Sem mudanca de modelo.** Haiku permanece. Smoke confirmou que o problema era do prompt do subagent (descricao abstrata, tom sugestivo), nao da capacidade do modelo.

3. **Sem mudanca em hooks, ADRs, CLAUDE.md, outros componentes.** Foco unico no `pr-reviewer.md`.

4. **Smoke test pos-merge da 4.9.1** valida criterio refinado: output usa **exatamente** Bloqueadores → Sugestoes → Elogios, sem secoes extras.

Quando esta etapa terminar:

- `.claude/agents/pr-reviewer.md` atualizado: template prescritivo + 2 exemplos few-shot.
- `docs/decisoes.md` formaliza refinamento + categoria "refinamento de componente baseado em smoke empirico".
- `docs/progresso.md` registra 4.9.1 + 3 licoes novas.

## Padroes que estreiam nesta etapa

1. **Categoria nova: "refinamento de componente baseado em smoke empirico".** Diferente de:
   - "Registro pos-smoke falho" (4.2.1, 4.7.1): smoke falhou, documenta licao.
   - "Patch tecnico" (4.0.1): corrige bug.
   - "Refactor pos-smoke": smoke passou mas revelou subotimo; refina componente.

2. **Few-shot prompting como padrao em subagents.** Subagents Haiku aderem melhor a estrutura via exemplos concretos vs descricao abstrata. Padrao a aplicar em `architect-reviewer`, `test-writer` futuros.

3. **Tom prescritivo em instrucoes para subagents Haiku.** "DEVE usar exatamente" > "ver template abaixo". Direto > sugestivo.

## Escopo decidido

### Mudancas no `.claude/agents/pr-reviewer.md`

**Mudanca 1 — Endurecer linguagem da secao "Template de output".**

Texto **atual** (apos `## Template de output`):

```
\`\`\`markdown
# Revisao do PR #<numero>
...
\`\`\`

Se uma secao vazia, escreva `_Nenhum_` em italico.
```

Substituir por:

```
**Voce DEVE usar exatamente as 3 secoes abaixo, nesta ordem, sem acrescentar outras.** Nao use "Visao Geral", "Analise", "Conclusao", "Resumo", "Recomendacao", "Itens Especificos" ou qualquer outra secao. Apenas Bloqueadores, Sugestoes, Elogios.

Se nada se encaixa numa secao, escreva `_Nenhum_` em italico. Nao omita a secao. Nao mude o titulo.

\`\`\`markdown
# Revisao do PR #<numero>

## Bloqueadores

(Issues que devem ser resolvidas antes do merge. Vazio = nada bloqueia.)

- **<titulo curto>** (arquivo `<path>` linha N): <descricao>. Sugestao: <fix>.

## Sugestoes

(Melhorias opcionais. Operador decide acatar ou ignorar.)

- **<titulo curto>**: <descricao>. Por que: <razao>.

## Elogios

(O que esta bem feito.)

- <coisa boa>.
\`\`\`
```

**Mudanca 2 — Adicionar secao "Exemplos" ao final do subagent**, antes de "Tom" (que ja existe). Dois exemplos:

Exemplo 1 — PR doc-only com nada a apontar:

```markdown
## Exemplos

### Exemplo 1: PR doc-only sem problemas

Cenario: PR adiciona 1 entrada em `docs/progresso.md` registrando sub-etapa concluida. Sem mudanca de codigo, sem outras edicoes.

Output esperado:

\`\`\`markdown
# Revisao do PR #42

## Bloqueadores

_Nenhum_

## Sugestoes

_Nenhum_

## Elogios

- Entrada em `progresso.md` segue padrao das anteriores (ordem cronologica descrescente, formato consistente).
- Sem efeitos colaterais — nao toca em hooks, ADRs, CLAUDE.md.
\`\`\`

### Exemplo 2: PR de hook com sugestao real

Cenario: PR adiciona hook `.claude/hooks/universal/trailing-whitespace.ps1` que detecta espacos em branco no final de linhas em `.md`.

Output esperado:

\`\`\`markdown
# Revisao do PR #57

## Bloqueadores

- **Hook nao filtra arquivos por extensao** (arquivo `.claude/hooks/universal/trailing-whitespace.ps1` linha 18): hook age sobre todos arquivos staged, incluindo `.png`, `.pdf` (binarios). Em arquivo binario, regex de whitespace pode dar match falso ou erro. Sugestao: adicionar filtro `Where-Object { $_ -match '\.(md|ps1|java|sql)$' }` antes do loop principal.

## Sugestoes

- **Mensagem de erro generica**: hook diz "trailing whitespace found in line N" — util, mas nao mostra a linha. Por que: dev precisa abrir o arquivo manualmente pra ver. Sugestao: incluir o conteudo da linha truncada (primeiros 60 chars) na mensagem.
- **Falta cenario destrutivo no PR body** para arquivo binario passar pelo filtro. ADR-011 pede cenario que confirma que o hook nao age em `.png`.

## Elogios

- Regex `[ \t]+$` esta correta — cobre espaco e tab.
- Encoding UTF-8 sem BOM aplicado conforme padrao do projeto.
- `progresso.md` foi atualizado registrando o hook em "Hooks implementados".
\`\`\`
```

**Resumo do efeito:** subagent vai de ~101 linhas para ~140 linhas. Cresce ~40%. Aceitavel — exemplos sao o investimento que paga em aderencia.

### Arquivos modificados

```
.claude/agents/pr-reviewer.md   <- refactor (template prescritivo + 2 exemplos)
docs/decisoes.md                 <- edicao (subsecao 4.9.1 + categoria nova)
docs/progresso.md                <- edicao (sub-etapa + 3 licoes)
docs/prompt-etapa-4-9-1.md       <- novo (este proprio prompt)
```

**Nao tocar:**

- Outros subagents (nao existem ainda alem do pr-reviewer).
- Hooks, entrypoints, scripts.
- `CLAUDE.md` raiz (regra 4.6: refinamento de subagent nao muda stack/ambiente/convencoes/restricoes).
- `docs/hooks-pendentes.md` (sem hook, sem debito).
- ADRs, `pom.xml`, `src/`, `frontend/`, migrations.
- `docs/blueprint-fabrica-ai-native.md`.
- `.gitignore`, `.gitattributes`.

### Atualizacao de `docs/decisoes.md`

Adicionar subsecao **apos** "Primeiro subagent: pr-reviewer (Sub-etapa 4.9)" e **antes** de "Claude Code hooks nativos":

```markdown
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
```

Adicionar entrada no historico (final do arquivo):

```markdown
- **2026-MM-DD** — Sub-etapa 4.9.1 concluida (refactor): refinamento do `pr-reviewer` pos-smoke. Template de output endurecido (tom prescritivo "DEVE usar exatamente") + 2 exemplos few-shot adicionados. Haiku mantido. Categoria nova "refinamento de componente baseado em smoke empirico" — diferente de "registro pos-smoke falho" (smoke da 4.9 passou; subagent funciona, so improvisou estrutura). Padrao operacional: smoke positivo pode gerar sub-etapa de refinamento. Mergeado via PR #XX.
```

### Atualizacao de `docs/progresso.md`

**A.** Atualizar "Ultima atualizacao": `2026-MM-DD (Sub-etapa 4.9.1 — Refinamento do pr-reviewer pos-smoke)`.

**B.** Em "Sub-etapas concluidas" (ordem cronologica descrescente, **acima** da 4.9):

```markdown
- **4.9.1 — Refinamento do `pr-reviewer` pos-smoke** (2026-MM-DD): sub-etapa de refactor (branch `refactor/`). Smoke test pos-merge da 4.9 confirmou que subagent funciona — invocado proativamente, le PR, produz output util. Mas output divergiu do template prescrito: 4 secoes (Visao Geral, Analise, Itens Especificos, Conclusao) em vez das 3 (Bloqueadores, Sugestoes, Elogios). Sub-etapa **refina o `.claude/agents/pr-reviewer.md`** em 2 mudancas: (1) template prescritivo ("Voce DEVE usar exatamente as 3 secoes", com lista explicita de secoes proibidas); (2) 2 exemplos few-shot (PR doc-only sem problemas + PR de hook com sugestao real). Haiku mantido — smoke confirmou que problema era do prompt do subagent, nao da capacidade do modelo. Categoria nova **"refinamento de componente baseado em smoke empirico"** — distinta de "registro pos-smoke falho" (smoke da 4.9 passou; refinamento melhora aderencia ao prescrito). PR #XX.
```

**C.** Adicionar secao "Licoes da Sub-etapa 4.9.1":

```markdown
## Licoes da Sub-etapa 4.9.1

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — refactor de subagent.)

### Licoes de ambiente

1. **Categoria nova: "refinamento de componente baseado em smoke empirico".** Diferente de "registro pos-smoke falho" (4.2.1, 4.7.1): aqui o smoke **passou** — componente funciona — mas revelou comportamento subotimo (output divergindo do prescrito). Sub-etapa de refactor refina o componente, nao apenas documenta licao. Padrao operacional para componentes complexos (subagents, skills futuras, MCPs): smoke pos-merge pode ser positivo E gerar sub-etapa de refinamento subsequente. Diferente de hooks, onde smoke ou bloqueia (falha) ou aceita (funciona); subagents tem espectro mais amplo de "funciona mas pode melhorar".

2. **Few-shot prompting > descricao abstrata para modelos menores (Haiku).** Subagent original da 4.9 descrevia template via "ver template abaixo" com codigo Markdown. Subagent improvisou estrutura propria. Refinamento da 4.9.1 adiciona 2 exemplos completos de output — Haiku tende a aderir melhor a estrutura via exemplos concretos vs descricao abstrata. Padrao a aplicar em `architect-reviewer`, `test-writer` e outros subagents Haiku futuros: incluir 1-3 exemplos completos, nao apenas template abstrato.

3. **Tom prescritivo > tom sugestivo em instrucoes para subagents Haiku.** "Voce DEVE usar exatamente as 3 secoes, nesta ordem, sem acrescentar outras" tem aderencia maior que "ver template abaixo". Pode parecer rude na leitura humana, mas modelos menores precisam de instrucoes diretas. Lista explicita do que **nao** fazer ("Nao use 'Visao Geral', 'Analise'...") complementa a prescricao positiva. Padrao a aplicar em outros subagents Haiku.
```

**D.** Historico geral:

```markdown
- **2026-MM-DD** — Sub-etapa 4.9.1 concluida (refactor): refinamento do `pr-reviewer` pos-smoke. Template prescritivo + 2 exemplos few-shot. Haiku mantido. Categoria nova "refinamento de componente baseado em smoke empirico" — smoke da 4.9 passou (componente funciona) mas revelou divergencia do template prescrito; refactor refina aderencia. Mergeado via PR #XX.
```

### Versionar este proprio prompt

`docs/prompt-etapa-4-9-1.md`.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra `07158e3` (squash da 4.9) ou superior.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-9-1.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `.claude/agents/pr-reviewer.md` existe (criado na 4.9) com ~101 linhas, frontmatter `model: haiku`.

Validar com:

```powershell
git status
git log --oneline -3
Test-Path docs\prompt-etapa-4-9-1.md
Test-Path .claude\agents\pr-reviewer.md
(Get-Content .claude\agents\pr-reviewer.md).Count
Get-Content .claude\agents\pr-reviewer.md | Select-String "^model:"
git config core.hooksPath
```

**Pre-condicoes ADR-011:**

- `Test-Path docs\prompt-etapa-4-9-1.md` retorna `True`.
- `Test-Path .claude\agents\pr-reviewer.md` retorna `True`.
- `(Get-Content .claude\agents\pr-reviewer.md).Count` retorna ~101.
- Frontmatter contem `model: haiku`.

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
git checkout -b refactor/etapa-4-9-1-template-pr-reviewer
```

Prefixo `refactor/` — refina componente existente sem adicionar funcionalidade nem corrigir bug.

### Tarefa 4 — Antes de editar, ler arquivos vivos

```bash
cat .claude/agents/pr-reviewer.md
cat docs/decisoes.md
cat docs/progresso.md
```

**Confirmar:**

- `pr-reviewer.md` tem secao `## Template de output` com texto atual e `## Tom` apos. Nova secao `## Exemplos` entra **entre** essas duas.
- `decisoes.md` tem subsecao "Primeiro subagent: pr-reviewer (Sub-etapa 4.9)" antes de "Claude Code hooks nativos". A nova "Refinamento do pr-reviewer pos-smoke (Sub-etapa 4.9.1)" entra **entre** essas duas.
- `progresso.md` tem "Sub-etapas concluidas" em ordem descrescente ate 4.9.

Se alguma divergencia, **parar e reportar**.

### Tarefa 5 — Refatorar `.claude/agents/pr-reviewer.md`

Aplicar 2 mudancas conforme escopo:

1. Endurecer linguagem da secao "Template de output" (substituir texto introdutorio).
2. Adicionar secao "Exemplos" com 2 exemplos completos.

**Pre-condicao ADR-011:** apos editar:

```powershell
Test-Path .claude\agents\pr-reviewer.md   # True

$bytes = [System.IO.File]::ReadAllBytes(".claude/agents/pr-reviewer.md")
Write-Host "Primeiros 3 bytes: $($bytes[0..2] -join ', ')"
# Esperado: NAO 239, 187, 191 (sem BOM)

$content = [System.IO.File]::ReadAllText(".claude/agents/pr-reviewer.md", [System.Text.UTF8Encoding]::new($false))

# Validar que frontmatter permanece intacto
if ($content -match '(?s)^---\s*\nname: pr-reviewer.*?model: haiku\s*\n---') {
    Write-Host "Frontmatter intacto OK"
} else {
    Write-Host "Frontmatter ALTERADO — investigar"
}

# Validar tom prescritivo presente
if ($content -match 'DEVE usar exatamente') {
    Write-Host "Tom prescritivo OK"
} else {
    Write-Host "Tom prescritivo AUSENTE"
}

# Validar 2 exemplos presentes
$exemplos = ([regex]::Matches($content, '### Exemplo \d')).Count
Write-Host "Exemplos encontrados: $exemplos (esperado: 2)"

# Linhas totais
$linhas = (Get-Content .claude\agents\pr-reviewer.md).Count
Write-Host "Linhas totais: $linhas (esperado: ~140)"
```

Se algum valor divergir, parar e reportar.

### Tarefa 6 — Editar `docs/decisoes.md` e `docs/progresso.md`

Conforme escopo. Verificar **ordem descrescente** em `progresso.md` (4.9.1 acima de 4.9).

### Tarefa 7 — Versionar este proprio prompt

`git add docs/prompt-etapa-4-9-1.md`.

### Tarefa 8 — Commits (2 commits)

**Commit 1** — Refactor do subagent:

```bash
git add .claude/agents/pr-reviewer.md
git status   # apenas pr-reviewer.md staged
git commit -m "refactor(claude): endurece template do pr-reviewer e adiciona 2 exemplos few-shot"
```

**Pre-condicao ADR-011:** 1 arquivo staged; `$LASTEXITCODE = 0`.

**Commit 2** — Docs:

```bash
git add docs/decisoes.md docs/progresso.md docs/prompt-etapa-4-9-1.md
git status   # 3 arquivos staged
git commit -m "docs: registra refinamento pos-smoke do pr-reviewer + categoria refactor pos-smoke"
```

**Pre-condicao ADR-011:** 3 arquivos staged; `$LASTEXITCODE = 0`.

**Validacao implicita dos hooks ativos:**
- Encoding UTF-8 (4.2) valida bytes.
- Markdown blank lines (4.3) valida headers nivel 2-6 nos `.md`.
- Tamanho de docs (4.4) pode alertar se `progresso.md` cruzar 800 linhas — comportamento esperado.

Se hook bloquear, parar e reportar.

### Tarefa 9 — Validacao final antes de push

```bash
git status
git log --oneline -3
git config core.hooksPath
(Get-Content .claude\agents\pr-reviewer.md).Count
```

Esperado:
- Working tree limpo.
- 2 commits novos.
- `pr-reviewer.md` com ~140 linhas.

## Restricoes e freios

1. **Apenas refinar o `pr-reviewer.md`.** Nao criar outros subagents, hooks, skills.

2. **NAO mudar modelo do subagent.** `model: haiku` permanece. Smoke confirmou que problema era do prompt, nao do modelo.

3. **NAO mudar frontmatter** (name, description, tools, model). Mudar so corpo do subagent.

4. **NAO remover secoes existentes** do subagent (Identidade, O que verifica, etc.). Apenas refinar "Template de output" + adicionar "Exemplos".

5. **NAO tocar em hooks, entrypoints, scripts, `pom.xml`, `src/`, `frontend/`, migrations, `CLAUDE.md`, ADRs, `.gitignore`, `.gitattributes`, blueprint, hooks-pendentes.md.**

6. **CLAUDE.md NAO atualizado** (regra 4.6: refactor de subagent nao muda stack/ambiente/convencoes/restricoes).

7. **Exemplos few-shot devem ser CONCRETOS, nao abstratos.** Cenario claro + output completo. Nao usar `<placeholder>` em vez do output real.

8. **Encoding UTF-8 sem BOM** no `pr-reviewer.md`.

9. **Apenas ASCII no frontmatter.** Corpo pode ter acentos consistentes (mas idealmente sem — alinhado com restante do projeto).

10. **Ordem cronologica em `progresso.md`:** descrescente. 4.9.1 acima de 4.9.

11. **Sem cenarios destrutivos tradicionais.** Smoke test pos-merge funcional.

12. **Nao sugerir proxima sub-etapa** espontaneamente.

13. **Nao tomar decisao silenciosa em zona limitrofe.** Reportar divergencias.

14. **Nao usar `pwsh`.** PowerShell 5.1.

15. **Nao usar `git reset --hard`.**

## Estrutura de commits

Branch: `refactor/etapa-4-9-1-template-pr-reviewer`

**Commit 1** — `refactor(claude): endurece template do pr-reviewer e adiciona 2 exemplos few-shot`
- `.claude/agents/pr-reviewer.md` (refactor)

**Commit 2** — `docs: registra refinamento pos-smoke do pr-reviewer + categoria refactor pos-smoke`
- `docs/decisoes.md` (edicao)
- `docs/progresso.md` (edicao)
- `docs/prompt-etapa-4-9-1.md` (novo)

## Validacao antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -3
git config core.hooksPath
(Get-Content .claude\agents\pr-reviewer.md).Count
```

Esperado:
- `check.ps1` passa.
- Working tree limpo.
- 2 commits novos.
- `pr-reviewer.md` ~140 linhas.

## PR

Titulo: `refactor: sub-etapa 4.9.1 -- refinamento do pr-reviewer pos-smoke (template + few-shot)`

Body sugerido:

```markdown
## Summary

Refina o `pr-reviewer` (criado na 4.9) baseado em evidencia empirica do smoke test pos-merge. Categoria nova: **"refinamento de componente baseado em smoke empirico"**.

### Por que esta sub-etapa existe

Smoke test pos-merge da 4.9 confirmou:
- Subagent existe em `.claude/agents/pr-reviewer.md`. ✓
- E invocado proativamente pelo Claude principal. ✓
- Le PR via `gh pr view`/`gh pr diff`. ✓
- Produz output util. ✓

**Mas** output divergiu do template prescrito:
- Prescrito: 3 secoes (Bloqueadores, Sugestoes, Elogios).
- Real: 4 secoes (Visao Geral, Analise das Mudancas, Itens Especificos, Conclusao).

Subagent improvisou estrutura propria. Nao e bug — e comportamento desalinhado do que o prompt do subagent pediu. Causa provavel: template prescrito de forma abstrata ("ver template abaixo"), sem exemplos concretos nem tom prescritivo.

### Mudancas no `.claude/agents/pr-reviewer.md`

**1. Template de output prescritivo.**

Antes: "ver template abaixo" + bloco markdown.

Depois: "**Voce DEVE usar exatamente as 3 secoes abaixo, nesta ordem, sem acrescentar outras.** Nao use 'Visao Geral', 'Analise', 'Conclusao'..." + bloco markdown.

Lista explicita do que **nao** fazer complementa a prescricao positiva.

**2. Secao "Exemplos" com 2 exemplos few-shot.**

Exemplo 1: PR doc-only sem problemas → output com `_Nenhum_` em Bloqueadores e Sugestoes, 2 elogios curtos.

Exemplo 2: PR de hook com sugestao real → output com 1 bloqueador (filtro de extensao ausente), 2 sugestoes (mensagem de erro generica, cenario destrutivo faltando), 3 elogios.

Cenarios concretos, outputs completos. Haiku adere melhor a estrutura via exemplos concretos vs descricao abstrata.

### Sem mudanca de modelo

Haiku permanece. Smoke confirmou que problema era do prompt do subagent (descricao abstrata, tom sugestivo), nao da capacidade do modelo. Subir para Sonnet agora seria decisao sem evidencia. Esperar mais PRs reais antes de avaliar.

### Categoria nova: "refinamento de componente baseado em smoke empirico"

Diferente de:
- **"Registro pos-smoke falho"** (4.2.1, 4.7.1): smoke falhou; sub-etapa doc-only registra licao + decisao consciente (corrigir vs aceitar debito).
- **"Patch tecnico"** (4.0.1): corrige bug do que foi entregue.
- **Esta categoria:** smoke **passou** (componente funciona) E ainda gerou sub-etapa de refinamento (forma do output, aderencia ao prescrito).

Padrao operacional para componentes complexos (subagents, skills futuras, MCPs): smoke pode ser positivo E gerar refinamento subsequente. Diferente de hooks (onde smoke bloqueia ou aceita).

### Mudancas

- `.claude/agents/pr-reviewer.md`: template prescritivo + secao "Exemplos" com 2 exemplos completos. De ~101 para ~140 linhas.
- `docs/decisoes.md`: subsecao "Refinamento do pr-reviewer pos-smoke (Sub-etapa 4.9.1)" formaliza refinamento + categoria nova. Entrada no historico.
- `docs/progresso.md`: sub-etapa 4.9.1 + 3 licoes (categoria refactor pos-smoke, few-shot > abstrato, tom prescritivo > sugestivo). Entrada no historico.
- `docs/prompt-etapa-4-9-1.md`: prompt versionado.

### CLAUDE.md NAO atualizado

Regra 4.6: refactor de subagent nao muda stack/ambiente/convencoes/restricoes.

### `docs/hooks-pendentes.md` NAO atualizado

Sub-etapa nao adiciona hook nem debito.

### Sem validacao destrutiva tradicional

Subagent nao e hook. Smoke test pos-merge funcional:

\`\`\`powershell
git checkout main
git pull
(Get-Content .claude/agents/pr-reviewer.md).Count   # ~140
\`\`\`

**Smoke funcional** (em sessao nova do Claude Code, apos `/clear` ou janela nova):

1. Abrir PR de teste (mudanca trivial em branch nova).
2. Pedir: "Revisa este PR".
3. **Criterio de sucesso refinado:** output usa **exatamente** Bloqueadores → Sugestoes → Elogios, sem secoes extras (Visao Geral, Analise, Conclusao).

**Possiveis falhas:**
- Subagent ainda improvisa estrutura → tom prescritivo nao foi forte o suficiente. Sub-etapa 4.9.2 endureceria mais (ex: regra explicita no inicio do system prompt).
- Subagent segue template mas e ainda mais superficial → sinaliza limite de Haiku. Sub-etapa 4.9.2 subiria pra Sonnet.

### Proximo passo

Decisao fora deste PR.
```

## Pos-criacao do PR

1. Abrir PR via `gh pr create`.
2. Capturar o numero.
3. Editar `docs/decisoes.md` e `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>` e `2026-MM-DD` pela data real.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorizacao explicita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `refactor/etapa-4-9-1-template-pr-reviewer` empurrada com 3 commits (2 + 1 update do PR).
- PR aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.9 (`07158e3`).
- Working tree limpo.
- `.claude/agents/pr-reviewer.md` ~140 linhas.
- Reportar: `git log --oneline -3`, `git status`, `gh pr view --json number,state,statusCheckRollup`, `(Get-Content .claude\agents\pr-reviewer.md).Count`.

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao criar prompt da proxima sub-etapa.
- Nao criar outros subagents, hooks, skills, MCPs, scripts.
- Nao executar o subagent (smoke fica pos-merge).
- Nao mudar modelo do subagent.
- Nao tocar em `CLAUDE.md`, `hooks-pendentes.md`, ADRs, blueprint, `.gitignore`, `.gitattributes`.
- Nao sugerir proximo passo espontaneamente.
- Nao usar `pwsh`.
- Nao usar `git reset --hard`.
