---
name: ship
description: Orquestra entrega de PR: verifica estado, roda check.ps1 (gate completo), push, cria PR com titulo do ultimo commit e lista de commits da branch. Sem argumento necessario.
disable-model-invocation: true
---

Voce deve entregar a branch atual como PR no GitHub. Execute todos os passos em ordem.
Pare e reporte ao operador se qualquer verificacao ou comando falhar.

## Passo 0 -- Verificacoes de seguranca (ADR-011)

Execute cada verificacao via Bash (PowerShell). Se qualquer uma falhar: reporte
exatamente qual falhou e termine sem fazer push ou PR.

**V1 -- branch nao e main:**
```powershell
$branch = git branch --show-current
if ($branch -eq "main") {
    Write-Host "ERRO: voce esta em main. /ship opera apenas em branches de feature."
    exit 1
}
Write-Host "Branch: $branch"
```

**V2 -- working tree limpa:**
```powershell
$status = git status --porcelain
if ($status) {
    Write-Host "ERRO: working tree nao esta limpa. Commite ou descarte as mudancas antes de /ship."
    Write-Host $status
    exit 1
}
Write-Host "Working tree: limpa"
```

**V3 -- ha commits acima de main:**
```powershell
$ahead = git rev-list --count main..HEAD
if ($ahead -eq 0) {
    Write-Host "ERRO: nenhum commit acima de main. Nao ha nada para shipar."
    exit 1
}
Write-Host "Commits acima de main: $ahead"
```

**V4 -- gh CLI disponivel e autenticado:**
```powershell
gh auth status 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: gh CLI nao autenticado. Rode 'gh auth login' antes de /ship."
    exit 1
}
Write-Host "gh CLI: autenticado"
```

## Passo 1 -- Gate completo

```powershell
.\scripts\check.ps1
```

Se exit code != 0: escreva "ERRO: gate falhou (check.ps1 exit $LASTEXITCODE). Corrija
antes de shipar." e termine. Nao faca push.

ATENCAO: check.ps1 exige Docker Desktop rodando (Testcontainers). Se Docker nao
estiver ativo, o gate falhara na verificacao de Docker antes de qualquer teste.

## Passo 1.1 -- Gate frontend (condicional)

Verifique se ha arquivos em `frontend/` entre os commits da branch:

```powershell
$frontendChanged = git diff --name-only main..HEAD | Where-Object { $_ -like "frontend/*" }
```

Se `$frontendChanged` for nao-nulo e nao-vazio: execute o gate frontend:

```powershell
# Garantir que estamos na raiz do repositorio (executor pode ter feito cd em subdir)
Set-Location (git rev-parse --show-toplevel)
.\scripts\check-front.ps1
```

Se exit code != 0: escreva "ERRO: gate frontend falhou (check-front.ps1 exit
$LASTEXITCODE). Corrija antes de shipar." e termine. Nao faca push.

Se `$frontendChanged` for nulo ou vazio: pule este passo (sem arquivos frontend
na branch, gate nao se aplica).

## Passo 2 -- Push

```powershell
$branch = git branch --show-current
git push -u origin $branch
```

Se exit code != 0: escreva "ERRO: push falhou." com o output do git e termine.
Nao crie PR.

## Passo 3 -- Extrair informacoes para o PR

```powershell
$titulo = git log -1 --format="%s"
$commits = git log main..HEAD --oneline
$branch = git branch --show-current
```

## Passo 4 -- Criar PR

Monte o body do PR com a lista de commits e o aviso padrao.
Execute:

```powershell
gh pr create `
  --title $titulo `
  --base main `
  --body "## Commits nesta branch

$commits

---
_Gerado via /ship. Adicione detalhes de decisao e smoke se necessario._"
```

Se exit code != 0: escreva "ERRO: gh pr create falhou." com o output e termine.

## Passo 5 -- Reviews automaticas

Extraia o numero do PR da URL retornada pelo gh pr create (ultimo segmento da URL).

**DETECCAO DE CONTEXTO (executar antes de qualquer review):**

Verifique se voce esta num worktree isolado (sub-agente de /batch ou /plan):

```bash
if [ -f .git ]; then
  echo "WORKTREE: Agent tool indisponivel -- pular reviews automaticos"
else
  echo "REPO_PRINCIPAL: Agent tool disponivel -- executar reviews"
fi
```

Se `.git` for um ARQUIVO (worktree): **pule os Reviews 1, 2 e 3 inteiramente.**
No relatorio final (Passo 6), substitua a secao Reviews por:

```
Reviews:
  PENDENTE -- executor rodou em worktree (Agent tool indisponivel).
  Rodar manualmente antes do merge:
    /review-pr <numero>
    /review-front <numero>   (se houver mudancas em frontend/)
```

Se `.git` for um DIRETORIO (repo principal): prosseguir normalmente com os reviews abaixo.

Verifique se ha arquivos em `frontend/` entre os commits da branch:

```powershell
$frontendChanged = git diff --name-only main..HEAD | Where-Object { $_ -like "frontend/*" }
```

Emita TODOS os reviews aplicaveis em UMA UNICA resposta (acao atomica -- nao sequencial):

- **Sempre:** Agent call para `pr-reviewer` com prompt `Revise o PR #<numero> do repositorio financas-lab antes do merge.`
- **Sempre:** Agent call para `architect-reviewer` com prompt `Revise as decisoes arquiteturais do PR #<numero> do repositorio financas-lab contra os ADRs do projeto.`
- **Se `$frontendChanged` nao-vazio:** Agent call para `front-reviewer` com prompt `Revise as mudancas de frontend do PR #<numero> do repositorio financas-lab.`

Aguarde TODOS os reviews terminarem antes de processar os resultados.
Se qualquer review reportar bloqueador: inclua no relatorio final e sinalize ao operador.
Nao cancele o /ship -- o PR ja foi aberto.

## Passo 5.1 -- Correcao autonoma de apontamentos

### Regra 1 — O que corrigir (filtro obrigatorio)

Corrija APENAS apontamentos classificados como **Bloqueador** (`## Bloqueadores`)
nos outputs dos reviews. Nunca corrija apontamentos de `## Sugestoes` nem de
`## Elogios`.

Exemplos de IDs de bloqueadores: B1, B2, B3, ..., B13 (front-reviewer),
violacao de ADR (architect-reviewer), logica incorreta (pr-reviewer).

Se todos os apontamentos forem Sugestoes ou Elogios: pule este passo inteiramente
e va direto ao Passo 6.

### Regra 2 — Apontamento subjetivo vs. objetivo

- **Objetivo** (corrija autonomamente): falta de anotacao, import ausente,
  variavel nao usada, violacao clara de convencao ADR estabelecida,
  `text-right` ausente em celula monetaria, `asChild` em base-nova, fetch direto
  fora de services/.
- **Subjetivo** (reporte e aguarde): redesign de interface, mudanca de estrategia,
  decisao que afeta outros bounded contexts.

### Regra 3 — Protocolo de edicao (obrigatorio, sem excecao)

Para cada bloqueador objetivo a corrigir, executar nesta ordem exata:

**Passo A — Ler o arquivo completo antes de qualquer edicao:**
```
Read(<path-do-arquivo-apontado>)
```
Nao editar sem antes ter lido o arquivo na integra. Sem excecao.

**Passo B — Aplicar o fix minimo em UMA unica operacao Edit:**
- Uma chamada de Edit por apontamento.
- Se o fix exigir mais de uma edicao no mesmo arquivo: fazer tudo numa
  unica chamada Edit com o bloco completo de mudanca.
- Se apos a edicao perceber que ficou errado: reportar como BLOQUEADOR
  para o operador. NAO tentar editar de novo em loop.

**Passo C — Validacao pontual (nao o gate completo):**
- Fix em arquivo `.tsx` ou `.ts` frontend:
  ```powershell
  cd <worktree>/frontend && npm run test:run -- <path/do/arquivo.test.tsx>
  ```
  Se nao houver arquivo de teste para o arquivo corrigido: pular validacao.
- Fix em arquivo `.java`:
  ```bash
  ./mvnw test -Dtest=<ClasseTest> -q
  ```
  Se nao houver teste correspondente: pular validacao.
- Fix em arquivo `.ps1`, `.sql`, `.yml` ou doc: sem validacao automatica.

**Passo D — Proibicoes absolutas:**
- NUNCA criar arquivos de teste temporarios ou probe (`__probe.*`, `*_temp.*`,
  qualquer arquivo que sera deletado em seguida).
- NUNCA rodar `check.ps1` ou `check-front.ps1` por fix individual.
- NUNCA rodar `npm run test:run` sem filtro de arquivo especifico.
- NUNCA editar o mesmo arquivo mais de uma vez por apontamento.

### Regra 4 — Gate final unico

Apos aplicar TODOS os fixes (nao apos cada um individualmente):

```powershell
.\scripts\check.ps1
```

Se houver mudancas em `frontend/`:
```powershell
powershell -NoProfile -Command "Set-Location (git rev-parse --show-toplevel); .\scripts\check-front.ps1"
```

Se qualquer gate falhar: reportar o erro ao operador. NAO tentar corrigir
automaticamente apos falha de gate — esse ciclo e irrecuperavel autonomamente.

### Regra 5 — Commit e push

Apos gate final passar:

```powershell
git add -A
git commit -m "fix(<scope>): <lista dos apontamentos corrigidos separados por virgula>"
git push
```

Editar o body do PR adicionando secao `## Correcoes autonomas`:
```powershell
gh pr edit <numero> --body-file <arquivo-com-body-atualizado>
```

Listar cada apontamento corrigido com: ID, arquivo, descricao da correcao.

Se nenhum bloqueador objetivo existir: pule este passo e va direto ao Passo 6.

## Passo 6 -- Relatorio final

Produza:

```
/ship concluido.

Branch:   <branch>
Gate:     check.ps1 passou
Push:     OK
PR:       <URL retornada pelo gh pr create>
Titulo:   <titulo>
Commits:  <numero> acima de main

Reviews:
  pr-reviewer:        <OK / BLOQUEADOR: <motivo>>
  architect-reviewer: <OK / BLOQUEADOR: <motivo>>
  front-reviewer:     <OK / BLOQUEADOR: <motivo> / N/A (sem frontend)>
```
