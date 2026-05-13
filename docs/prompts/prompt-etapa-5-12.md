# Prompt -- Sub-etapa 5.12: agente front-reviewer e skill /review-front

## Contexto

Agente de review especializado em codigo frontend (Next.js/TypeScript/shadcn).
Motivacao: na 5.9, bugs de CORS e origin hardcoded passaram pelo pr-reviewer e
architect-reviewer sem ser barrados -- esses agentes nao conhecem as convencoes
especificas do frontend deste projeto. O front-reviewer conhece.

Camada 4. Tres entregas: agente `.claude/agents/front-reviewer.md`,
skill `.claude/skills/review-front/SKILL.md`, e integracao condicional no /ship.

---

## O que implementar

### Novo agente: `.claude/agents/front-reviewer.md`

Seguir a estrutura exata de `pr-reviewer.md` (leia antes de criar):
- Frontmatter com `name`, `description`, `model: haiku`, tools: `Read, Grep, Glob, Bash`
- Output em 3 secoes obrigatorias: **Bloqueadores** | **Sugestoes** | **Elogios**
- Tom direto, pragmatico, sem duplicar o que pr-reviewer ou hooks ja cobrem

**Descricao do agente (para campo `description`):**
```
Revisa PRs com mudancas em frontend/: fetch fora de services/, asChild em base-nova,
valores hardcoded de ambiente, any em tipos de API, ausencia de teste para hook/componente novo.
```

**Identidade e escopo:**

Voce e um revisor senior de codigo frontend especializado nas convencoes deste projeto.
Seu foco e codigo em `frontend/`. Nao revise codigo Java, hooks git, ou docs -- isso e
escopo do pr-reviewer e architect-reviewer.

Quando invocado, voce deve:
1. Ler o diff do PR via `gh pr diff <numero>`
2. Para cada arquivo `frontend/` modificado, aplicar as regras abaixo
3. Produzir output estruturado nas 3 secoes

**Regras -- Bloqueadores (impede merge):**

| ID | Regra | Como detectar |
|----|-------|---------------|
| B1 | `fetch(` fora de `src/services/` | grep por `fetch(` em arquivos frontend fora de `src/services/` e `*.test.*` |
| B2 | `asChild` em componente shadcn | grep por `asChild` -- base-nova usa `render` prop, nao `asChild` |
| B3 | URL de ambiente hardcoded | grep por `http://localhost`, `http://192.168`, ou string com porta hardcoded fora de `.env*` |
| B4 | `any` como tipo de resposta de API | grep por `: any` ou `as any` em arquivos em `src/services/` ou `src/types/` |
| B5 | Token/credencial em codigo | grep por `Bearer `, `password`, `secret` com valor literal (ja coberto pelo hook, mas relatar se passar) |

**Regras -- Sugestoes (nao impede merge):**

| ID | Regra |
|----|-------|
| S1 | `console.log` em componente ou hook de producao (remover antes de producao) |
| S2 | Hook ou componente novo sem teste correspondente na PR |
| S3 | Acesso a token fora de `src/lib/auth.ts` ou `src/providers/auth-provider.tsx` |
| S4 | Props de componente sem tipo explícito (interface ou type alias) |

**Regras -- Elogios (reforcar bom padrao):**

| ID | Regra |
|----|-------|
| E1 | Uso correto de `render` prop em componente base-nova/shadcn |
| E2 | `apiFetch` sendo usado em vez de `fetch` diretamente |
| E3 | `ApiError` sendo usado para tipagem de erros de API |

**Formato de output (obrigatorio, igual ao pr-reviewer):**

```
## Bloqueadores

- [B1] src/app/(dashboard)/page.tsx:42 — `fetch('/api/contas')` direto fora de services/. Mover para `src/services/contas.ts`.

(ou "Nenhum." se nao houver)

## Sugestoes

- [S2] src/hooks/useContas.ts — hook novo sem teste. Adicionar `src/hooks/useContas.test.ts`.

(ou "Nenhuma." se nao houver)

## Elogios

- [E2] src/services/transacoes.ts — usa `apiFetch` corretamente em todos os endpoints.

(ou "Nenhum." se nao houver)
```

**Pragmatismo:**
- Se o PR nao tem arquivos `frontend/`, responder: "Nenhum arquivo frontend neste PR. Review nao aplicavel."
- Nao reportar violacoes em arquivos de teste (`*.test.*`, `*.spec.*`) para B1 (fetch em testes e aceitavel)
- Nao duplicar apontamentos que o pr-reviewer ja teria feito (imports faltando, convencao de nome, etc.)

---

### Nova skill: `.claude/skills/review-front/SKILL.md`

Seguir a estrutura da skill `/ship` (leia antes de criar). Esta skill e invocavel
pelo operador com `/review-front <numero-PR>`.

**Frontmatter:**
```yaml
---
name: review-front
description: Revisa mudancas de frontend em um PR usando o agente front-reviewer. Uso: /review-front <numero-PR>.
disable-model-invocation: true
---
```

**Conteudo da skill:**

```markdown
Voce deve revisar as mudancas de frontend do PR informado como argumento.

## Passo 1 -- Extrair numero do PR

O argumento passado ao invocar esta skill e o numero do PR. Se nenhum argumento
foi passado, leia o PR aberto da branch atual via:
```powershell
gh pr view --json number --jq '.number'
```

## Passo 2 -- Invocar front-reviewer

Invoque o agente `front-reviewer` via Agent tool com o prompt:

"Revise as mudancas de frontend do PR #<numero> do repositorio financas-lab.
Aplique todas as regras do seu escopo e produza output nas 3 secoes obrigatorias."

## Passo 3 -- Relatorio

Retorne o output completo do agente front-reviewer sem modificacao.
```

---

### Atualizar `.claude/skills/ship/SKILL.md`

Leia o arquivo completo antes de editar.

No **Passo 5** (Reviews automaticas), adicionar invocacao condicional do
`front-reviewer` apos o `architect-reviewer`:

```markdown
**Review 3 -- front-reviewer (condicional):**

Verifique se ha arquivos em `frontend/` entre os commits da branch:

```powershell
$frontendChanged = git diff --name-only main..HEAD | Where-Object { $_ -like "frontend/*" }
```

Se `$frontendChanged` for nao-nulo e nao-vazio:
- subagent_type: `front-reviewer`
- prompt: `Revise as mudancas de frontend do PR #<numero> do repositorio financas-lab.`

Aguarde o resultado. Se o front-reviewer reportar bloqueador: inclua no relatorio
final e sinalize ao operador.

Se `$frontendChanged` for nulo ou vazio: pule este review (sem arquivos frontend
na branch).
```

No **Passo 6** (Relatorio final), adicionar linha:
```
  front-reviewer:     <OK / BLOQUEADOR: <motivo> / N/A (sem frontend)>
```

**Cuidado com a formatacao:** respeitar linha em branco antes e depois de headers
(hook markdown-blank-lines ativo).

---

### Atualizar `docs/hooks-pendentes.md`

Adicionar entrada na secao de agentes/skills (criar se nao existir):
- Nome: **front-reviewer** + **/review-front**
- Sub-etapa: 5.12
- Caminhos: `.claude/agents/front-reviewer.md`, `.claude/skills/review-front/SKILL.md`
- Comportamento: 5 bloqueadores (B1-B5), 4 sugestoes (S1-S4), 3 elogios (E1-E3)
- Condicional no /ship: so invocado se ha arquivos `frontend/` na branch

Leia o arquivo antes de editar.

---

## Validacao destrutiva (smoke test)

Nao ha cenarios destrutivos de hook aqui (nao e um hook git). Smoke test da skill:

**Cenario 1 -- /review-front em PR existente:**
Invocar a skill diretamente lendo o SKILL.md e executando contra o PR #87 (ultimo
PR mergeado, que tem arquivos frontend):

```powershell
# Simular invocacao: invocar front-reviewer como Agent com prompt:
# "Revise as mudancas de frontend do PR #87 do repositorio financas-lab."
# Verificar que o output tem as 3 secoes: Bloqueadores, Sugestoes, Elogios
# Verificar que nao reporta arquivos Java como escopo do review
```

Documentar no body do PR: output do smoke (sumario do que o agente reportou para #87).

**Cenario 2 -- /ship em branch frontend detecta Review 3:**
Verificar na SKILL.md que o bloco do Passo 5 Review 3 esta presente e sintaticamente
correto (pode ser verificacao visual do arquivo editado).

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-12-front-reviewer

2. Ler antes de implementar:
   - .claude/agents/pr-reviewer.md          (modelo do agente)
   - .claude/agents/architect-reviewer.md   (segundo modelo)
   - .claude/skills/ship/SKILL.md           (onde integrar Review 3)
   - docs/hooks-pendentes.md               (formato de documentacao)

3. Criar .claude/agents/front-reviewer.md

4. Criar .claude/skills/review-front/SKILL.md
   (criar pasta .claude/skills/review-front/ se nao existir)

5. Editar .claude/skills/ship/SKILL.md (Passo 5 + Passo 6)

6. Editar docs/hooks-pendentes.md

7. Executar smoke (Cenario 1: front-reviewer contra PR #87)

8. commit: feat(claude): adiciona front-reviewer e skill /review-front

9. Atualizar docs/progresso.md (registra 5.12)

10. commit: docs(progresso): registra sub-etapa 5.12
    (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-12.md)

11. /ship -> PR com reviews; corrigir apontamentos autonomamente
```

---

## Estrutura de commits (5.12)

```
feat(claude): adiciona front-reviewer e skill /review-front
docs(progresso): registra sub-etapa 5.12
```

---

## Restricoes

- NAO modificar pr-reviewer ou architect-reviewer.
- NAO fazer front-reviewer revisar codigo Java ou hooks git -- escopo e so `frontend/`.
- A skill /review-front deve ter `disable-model-invocation: true` (padrao ADR-012
  para skills orquestradoras deterministas).
- Se o executor criar a pasta `.claude/skills/review-front/`: verificar com
  `Test-Path` antes de criar o arquivo SKILL.md.
- Hook markdown-blank-lines ativo: linha em branco antes e depois de todo `##`.

---

## Estado esperado ao terminar

- PR aberto com 2 commits acima de main.
- `.claude/agents/front-reviewer.md` existente com 5 bloqueadores, 4 sugestoes, 3 elogios.
- `.claude/skills/review-front/SKILL.md` existente e invocavel.
- Passo 5 do /ship com Review 3 condicional para frontend.
- Smoke do Cenario 1 documentado no body do PR (o que front-reviewer reportou para PR #87).
- docs/progresso.md com 5.12 registrada.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO rodar /ship mais de uma vez.
- NAO incluir regras de backend (Java, Spring, JPA) no front-reviewer.
