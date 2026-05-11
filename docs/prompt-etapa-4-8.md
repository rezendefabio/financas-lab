# Prompt — Etapa 4.8: Subir `blueprint-fabrica-ai-native.md` ao repositório (doc-only)

## Contexto

Camada 3 com 6 hooks ativos + CLAUDE.md estrutural + 2 sub-etapas doc-only de registro pos-smoke (4.2.1, 4.7.1) apos a 4.7.1.

**Esta sub-etapa entrega ato singelo:** copiar `blueprint-fabrica-ai-native.md` da pasta `C:\Users\rezen\Downloads\` para `docs/`. Documento ja e referenciado em `CLAUDE.md` (secao "Onde buscar mais") e em `docs/progresso.md` (Camada 1), mas **nunca foi commitado ao repo**. Link em CLAUDE.md aponta para arquivo inexistente.

Caracteristicas:

1. **Sub-etapa doc-only.** Sem mudanca de codigo, sem hook novo. Apenas adicao de arquivo de referencia conceitual.

2. **Arquivo pre-existente em sistema local.** Operador confirmou: `C:\Users\rezen\Downloads\blueprint-fabrica-ai-native.md`, 644 linhas, UTF-8 sem BOM, abaixo do limite warn 800.

3. **Documento de referencia conceitual fundadora.** Define vocabulario do projeto (Tier 1/2/3, routines, `/batch`, "modelo Boris Cherny"). Sem ele, conceitos referenciados em outros docs ficam orfaos.

Quando esta etapa terminar:

- `docs/blueprint-fabrica-ai-native.md` presente no repo.
- Link em `CLAUDE.md` (secao "Onde buscar mais") passa a apontar para arquivo real.
- `docs/progresso.md` registra a sub-etapa 4.8.

## Padroes que estreiam nesta etapa

Nenhum padrao novo. Sub-etapa doc-only minimalista. Aplica padroes ja consolidados:

- Branch prefixo `docs/`.
- Commit unico (sub-etapa enxuta).
- Sem validacao destrutiva tradicional.
- Smoke test pos-merge: leitura visual + verificacao de que CLAUDE.md aponta para arquivo agora presente.

## Escopo decidido

### Operacao

Copiar arquivo de `C:\Users\rezen\Downloads\blueprint-fabrica-ai-native.md` para `C:\projetos\financas-lab\docs\blueprint-fabrica-ai-native.md`. Conteudo idêntico.

**Origem confirmada pelo operador:**

- Path: `C:\Users\rezen\Downloads\blueprint-fabrica-ai-native.md`
- Tamanho: 644 linhas
- Encoding: UTF-8 sem BOM (primeiros 3 bytes: 35, 32, 66 = `# B`)
- Validado em sessao anterior antes da redacao deste prompt.

### Arquivos criados e modificados

```
docs/blueprint-fabrica-ai-native.md   ← novo (copia de Downloads)
docs/progresso.md                      ← edicao (sub-etapa + historico)
docs/prompt-etapa-4-8.md               ← novo (este proprio prompt)
```

**Nao tocar:**

- Hooks, entrypoints, orquestrador, scripts.
- `CLAUDE.md` raiz. O link **ja existe** apontando para `docs/blueprint-fabrica-ai-native.md`. Subir o arquivo torna o link funcional sem precisar editar `CLAUDE.md`. Regra de manutencao da 4.6 se aplica: sub-etapa nao muda stack/ambiente/convencoes/restricoes.
- `docs/decisoes.md`. Esta sub-etapa nao introduz regra ou padrao novo — apenas torna arquivo referenciado disponivel.
- `docs/hooks-pendentes.md`. Sem hook novo nem debito.
- ADRs.
- `pom.xml`, `src/`, `frontend/`, migrations.
- `.gitignore`, `.gitattributes`.

### Atualizacao de `docs/progresso.md`

**A.** Atualizar "Ultima atualizacao": `2026-MM-DD (Sub-etapa 4.8 — Sub blueprint-fabrica-ai-native.md ao repo)`.

**B.** Em "Sub-etapas concluidas" (ordem cronologica, apos 4.7.1):

```markdown
- **4.8 — Sub `blueprint-fabrica-ai-native.md` ao repo (doc-only)** (2026-MM-DD): sub-etapa minimalista. `CLAUDE.md` ja referenciava `docs/blueprint-fabrica-ai-native.md` em "Onde buscar mais" desde a 4.6, mas o arquivo nunca havia sido commitado — link apontava para arquivo inexistente. Operador tinha copia em `C:\Users\rezen\Downloads\` (644 linhas, UTF-8 sem BOM). Sub-etapa copia o arquivo para `docs/`. Documento de referencia conceitual fundadora — define vocabulario do projeto (Tier 1/2/3, routines, `/batch`, "modelo Boris Cherny"). Sem mudanca de codigo, sem hook, sem outras edicoes. PR #XX.
```

**C.** Adicionar secao "Licoes da Sub-etapa 4.8":

```markdown
## Licoes da Sub-etapa 4.8

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum nesta sub-etapa — doc-only minimalista.)

### Licoes de ambiente

1. **Referenciado em docs mas ausente no repo.** `CLAUDE.md` referenciava `docs/blueprint-fabrica-ai-native.md` em "Onde buscar mais" desde a 4.6. `docs/progresso.md` tambem referenciava em criterios da Camada 1. Arquivo nunca foi commitado — link quebrado por meses. Padrao a vigiar: quando CLAUDE.md ou docs adicionam link para arquivo em `docs/`, confirmar com `Test-Path` que o arquivo existe no repo, nao apenas no sistema do operador.
2. **Sub-etapa de "cumprir promessa ja feita".** 4.8 nao adiciona nada novo — entrega ato fundador que ja estava implicito em outros docs. Categoria meta-operacional: revisao periodica de links em `CLAUDE.md` e `docs/` pode revelar arquivos referenciados mas ausentes. Vale executar de novo quando sub-etapas futuras adicionarem novas referencias.
```

**D.** Historico geral:

```markdown
- **2026-MM-DD** — Sub-etapa 4.8 concluida (doc-only): `blueprint-fabrica-ai-native.md` adicionado ao repo. Link em `CLAUDE.md` (secao "Onde buscar mais") passa a apontar para arquivo real. Documento de referencia conceitual fundadora — define Tier 1/2/3, routines, `/batch`, "modelo Boris Cherny". Mergeado via PR #XX.
```

### Versionar este proprio prompt

`docs/prompt-etapa-4-8.md`.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra `4f44c93` (squash da 4.7.1) ou superior.
- `git config core.hooksPath` retorna `.githooks`.
- `docs/prompt-etapa-4-8.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- **`docs/blueprint-fabrica-ai-native.md` NAO existe** no repo.
- **`C:\Users\rezen\Downloads\blueprint-fabrica-ai-native.md`** existe (origem da copia).

Validar com:

```powershell
git status
git log --oneline -3
Test-Path docs\prompt-etapa-4-8.md
Test-Path docs\blueprint-fabrica-ai-native.md
Test-Path C:\Users\rezen\Downloads\blueprint-fabrica-ai-native.md
git config core.hooksPath
```

**Pre-condicoes ADR-011:**

- `Test-Path docs\prompt-etapa-4-8.md` retorna `True`.
- `Test-Path docs\blueprint-fabrica-ai-native.md` retorna **`False`** (nao existe ainda).
- `Test-Path C:\Users\rezen\Downloads\blueprint-fabrica-ai-native.md` retorna `True` (origem confirmada).

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
git checkout -b docs/etapa-4-8-sub-blueprint
```

Prefixo `docs/` — sub-etapa doc-only (analogo a 4.2.1, 4.6, 4.7.1).

### Tarefa 4 — Antes de copiar, ler arquivo fonte e progresso vivo

```powershell
# Validar encoding e tamanho da fonte
$bytes = [System.IO.File]::ReadAllBytes("C:\Users\rezen\Downloads\blueprint-fabrica-ai-native.md")
Write-Host "Primeiros 3 bytes (esperado: 35, 32, 66 = sem BOM): $($bytes[0..2] -join ', ')"
$linhas = [System.IO.File]::ReadAllLines("C:\Users\rezen\Downloads\blueprint-fabrica-ai-native.md").Count
Write-Host "Total de linhas (esperado: ~644): $linhas"
```

```bash
cat docs/progresso.md
```

**Confirmar:**

- Fonte tem 3 primeiros bytes `35, 32, 66` (sem BOM). Se aparecer `239, 187, 191`, **parar e reportar** — hook de encoding (4.2) vai rejeitar.
- Fonte tem ~644 linhas. Se passar de 800, alerta do hook docs-size (4.4) vai disparar — comportamento esperado, commit prossegue mas registrar.
- `progresso.md` tem "Sub-etapas concluidas" em ordem ate 4.7.1.

Se alguma divergencia critica (encoding com BOM), parar e reportar.

### Tarefa 5 — Copiar arquivo

```powershell
Copy-Item "C:\Users\rezen\Downloads\blueprint-fabrica-ai-native.md" "docs\blueprint-fabrica-ai-native.md"
```

**Pre-condicao ADR-011 — validar copia:**

```powershell
Test-Path docs\blueprint-fabrica-ai-native.md
# Esperado: True

$bytesDestino = [System.IO.File]::ReadAllBytes("docs\blueprint-fabrica-ai-native.md")
Write-Host "Primeiros 3 bytes do destino: $($bytesDestino[0..2] -join ', ')"
# Esperado: 35, 32, 66 (mesmos da fonte, sem BOM)

$linhasDestino = [System.IO.File]::ReadAllLines("docs\blueprint-fabrica-ai-native.md").Count
Write-Host "Linhas do destino: $linhasDestino"
# Esperado: 644 (identico a fonte)
```

Se algum valor divergir, parar e reportar.

### Tarefa 6 — Editar `docs/progresso.md`

Operacoes A, B, C, D conforme escopo. **Ordem cronologica:** 4.0 -> 4.0.1 -> 4.1 -> 4.2 -> 4.2.1 -> 4.3 -> 4.4 -> 4.5 -> 4.6 -> 4.7 -> 4.7.1 -> 4.8.

### Tarefa 7 — Versionar este proprio prompt

`git add docs/prompt-etapa-4-8.md`.

### Tarefa 8 — Commit unico (sub-etapa doc-only)

Por ser doc-only e minimalista, **um unico commit** consolida tudo:

```bash
git add docs/blueprint-fabrica-ai-native.md docs/progresso.md docs/prompt-etapa-4-8.md
git status   # 3 arquivos staged
git commit -m "docs: sub blueprint-fabrica-ai-native.md ao repo (referenciado mas nunca commitado)"
```

**Pre-condicao ADR-011:** 3 arquivos staged; `$LASTEXITCODE = 0`.

**Validacao implicita dos hooks ativos:**

- Encoding UTF-8 (4.2) valida `blueprint-fabrica-ai-native.md` e `progresso.md`. Sem BOM em ambos = aceito.
- Markdown blank lines (4.3) valida headers nivel 2-6. Blueprint e progresso devem passar (estao formatados).
- Tamanho de docs (4.4) pode alertar:
  - `blueprint-fabrica-ai-native.md`: 644 linhas, abaixo de 800 — sem alerta.
  - `progresso.md`: pode cruzar 800 com as adicoes da 4.8 — alerta esperado, modo warn aceita.
- Maven release (4.5) e entity-migration (4.7) nao agem (sem `.java` nem `pom.xml`).

Se hook bloquear (encoding ou blank lines), investigar antes de seguir. Provavel: blank lines em `blueprint-fabrica-ai-native.md` se algum header nivel 2-6 nao tem linha em branco antes/depois.

**Se hook de blank lines bloquear:** parar e reportar. **Nao** editar `blueprint-fabrica-ai-native.md` sem confirmar com operador — o conteudo e fonte de referencia, ajustar formato pode ser decisao consciente do operador (cosmico) ou descoberta de problema real (texto malformado).

### Tarefa 9 — Validacao final antes de push

```bash
git status
git log --oneline -3
git config core.hooksPath
Test-Path docs\blueprint-fabrica-ai-native.md
```

Esperado:
- Working tree limpo.
- 1 commit novo da 4.8.
- `core.hooksPath` retorna `.githooks`.
- `docs/blueprint-fabrica-ai-native.md` existe.

## Restricoes e freios

1. **Sub-etapa doc-only minimalista.** Nao criar `.ps1`, `.java`. Nao criar hooks, subagents, skills.

2. **Nao editar o conteudo do `blueprint-fabrica-ai-native.md`.** Copia integral da fonte. Se algum hook bloquear por formato, parar e reportar — nao corrigir sem confirmar com operador.

3. **Nao tocar em `CLAUDE.md`.** Link ja existe em "Onde buscar mais" desde a 4.6. Subir o arquivo torna o link funcional sem precisar editar `CLAUDE.md`. Regra de manutencao da 4.6 se aplica: sub-etapa nao muda stack/ambiente/convencoes/restricoes.

4. **Nao tocar em `docs/decisoes.md`, `docs/hooks-pendentes.md`, ADRs.** Sub-etapa nao adiciona regra, padrao ou debito.

5. **Nao tocar em `pom.xml`, scripts, `src/`, `frontend/`, migrations, `.gitignore`, `.gitattributes`.**

6. **Apenas conteudo prescrito.** Se durante Tarefa 4 (validar fonte) descobrir divergencia (BOM, encoding errado, conteudo diferente do esperado), parar e reportar.

7. **Nao introduzir formatacao adicional** ao blueprint. Copia integral.

8. **Tom conversacional direto** apenas nas adicoes ao `progresso.md`, consistente com restante.

9. **Sem acentos** nas adicoes ao `progresso.md` (consistencia com restante do projeto, embora `.md` aceite acentos).

10. **Ordem cronologica em `progresso.md`:** 4.0 -> 4.0.1 -> 4.1 -> 4.2 -> 4.2.1 -> 4.3 -> 4.4 -> 4.5 -> 4.6 -> 4.7 -> 4.7.1 -> 4.8.

11. **Sem cenarios destrutivos tradicionais.** Sub-etapa doc-only minimalista.

12. **Nao sugerir proxima sub-etapa** espontaneamente.

13. **Nao tomar decisao silenciosa em zona limitrofe.** Divergencias na Tarefa 4 ou Tarefa 5 -> parar e reportar.

14. **Nao usar `pwsh`.** PowerShell 5.1.

15. **Nao usar `git reset --hard`** nesta sub-etapa.

## Estrutura de commits

Branch: `docs/etapa-4-8-sub-blueprint`

**Commit unico** — `docs: sub blueprint-fabrica-ai-native.md ao repo (referenciado mas nunca commitado)`
- `docs/blueprint-fabrica-ai-native.md` (novo)
- `docs/progresso.md` (edicao)
- `docs/prompt-etapa-4-8.md` (novo)

## Validacao antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -3
git config core.hooksPath
```

Esperado:
- `check.ps1` passa.
- Working tree limpo.
- 1 commit novo.

## PR

Titulo: `docs: sub-etapa 4.8 -- sub blueprint-fabrica-ai-native.md ao repo (doc-only)`

Body sugerido:

```markdown
## Summary

Sub-etapa **doc-only minimalista**. Adiciona `docs/blueprint-fabrica-ai-native.md` ao repo. Documento ja era referenciado em `CLAUDE.md` (secao "Onde buscar mais") desde a 4.6 e em `docs/progresso.md` (criterios da Camada 1), mas **nunca havia sido commitado**. Link em `CLAUDE.md` apontava para arquivo inexistente.

### Por que esta sub-etapa existe

Descoberto durante calibracao de proxima direcao pos-4.7.1. Para entender Camada 4 (modelo operacional), conceitos do blueprint sao necessarios: Tier 1/2/3, routines, `/batch`, "modelo Boris Cherny". Esses termos aparecem em outros docs sem definicao operacional — o blueprint e o glossario. Operador tinha copia em `C:\Users\rezen\Downloads\blueprint-fabrica-ai-native.md` (644 linhas, UTF-8 sem BOM).

### Conteudo

Documento de referencia conceitual fundadora. 644 linhas. Cobre:

- Reframing fundamental ("voce nao esta construindo software, voce esta construindo uma fabrica").
- Multi-agentes em Claude Code (subagents, SDK, frameworks).
- Os tres tiers de autonomia (Tier 1 / 2 / 3).
- O blueprint das 4 camadas em ordem de construcao.
- Camadas 5-6 (runtime VPS, estrategia Max + API).
- Stack opinionada de partida.
- Armadilhas comuns.

### Mudancas

- `docs/blueprint-fabrica-ai-native.md` (novo): 644 linhas. Copia integral de `C:\Users\rezen\Downloads\`. UTF-8 sem BOM.
- `docs/progresso.md`: sub-etapa 4.8 em "Sub-etapas concluidas". Licoes da 4.8 (2 itens — referenciado mas ausente; sub-etapa de "cumprir promessa ja feita"). Entrada no historico.
- `docs/prompt-etapa-4-8.md`: prompt versionado.

### CLAUDE.md NAO atualizado

Sub-etapa nao muda stack/ambiente/convencoes/restricoes — apenas torna link em `CLAUDE.md` funcional. Regra de manutencao da 4.6 se aplica. Link em "Onde buscar mais" ja existe; apos merge, passa a apontar para arquivo presente.

### `docs/decisoes.md` NAO atualizado

Sub-etapa nao introduz regra ou padrao novo. Apenas adiciona arquivo de referencia que ja estava implicitamente prometido.

### Sem validacao destrutiva tradicional

Sub-etapa doc-only minimalista. Smoke test pos-merge: leitura visual do arquivo em `main` + verificar em sessao nova do Claude Code que o link em `CLAUDE.md` agora funciona.

### Categoria de licao

Lições da 4.8 registram duas observacoes meta-operacionais:

1. Arquivos referenciados em `CLAUDE.md` e `docs/` podem nao estar commitados ao repo. Vale revisao periodica de links.
2. Sub-etapa de "cumprir promessa ja feita" e categoria valida — entrega ato fundador implicitado em outros docs.

### Validacao pos-merge sugerida

```powershell
git checkout main
git pull
Test-Path docs/blueprint-fabrica-ai-native.md   # True
```

Em sessao nova do Claude Code (apos `/clear` ou janela nova):

- Perguntar: "O que e Tier 2 no contexto deste projeto?" -> esperado: agente le `docs/blueprint-fabrica-ai-native.md` (link em `CLAUDE.md`) e responde com definicao do blueprint.

### Proximo passo

Apos merge desta sub-etapa, sub-etapa 4.9 calibra primeiro subagent (`pr-reviewer` defendido), agora com blueprint disponivel para referencia.
```

## Pos-criacao do PR

1. Abrir PR via `gh pr create`.
2. Capturar o numero.
3. Editar `docs/progresso.md` substituindo `PR #XX` por `PR #<numero-real>` e `2026-MM-DD` pela data real.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorizacao explicita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `docs/etapa-4-8-sub-blueprint` empurrada com 2 commits (1 + 1 update do PR).
- PR aberto, CI verde, **nao mergeado**.
- `main` ainda no squash da 4.7.1.
- Working tree limpo.
- `docs/blueprint-fabrica-ai-native.md` presente no repo.
- Reportar com `git log --oneline -3`, `git status`, `gh pr view --json number,state,statusCheckRollup`, e contagem de linhas do `blueprint-fabrica-ai-native.md`.

## O que NAO fazer ao terminar

- Nao mergear o PR.
- Nao criar prompt da proxima sub-etapa.
- Nao criar hooks, subagents, skills, scripts.
- Nao editar conteudo do `blueprint-fabrica-ai-native.md`.
- Nao tocar em `CLAUDE.md`, `decisoes.md`, `hooks-pendentes.md`, ADRs.
- Nao sugerir proximo passo espontaneamente.
- Nao usar `pwsh`.
- Nao usar `git reset --hard`.
