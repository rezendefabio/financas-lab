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

Apos receber os resultados dos dois reviews, avalie cada apontamento:

- **Apontamento objetivo** (falta de anotacao, variavel nao usada, import ausente,
  violacao de convencao estabelecida nos ADRs): corrija diretamente no codigo sem
  perguntar ao operador.
- **Apontamento subjetivo ou arquitetural com tradeoffs** (redesign de interface,
  mudanca de estrategia, decisao que afeta outros bounded contexts): reporte ao operador
  e aguarde instrucao.

Para cada apontamento objetivo corrigido:
1. Edite o arquivo com o fix minimo necessario.
2. Rode `.\scripts\check.ps1` para confirmar BUILD SUCCESS apos a correcao.
3. Commite com mensagem `fix(<scope>): <descricao curta do apontamento corrigido>`.
4. Faca push da branch.
5. Edite o body do PR (via `gh pr edit <numero> --body "..."`) adicionando secao
   `## Cenarios destrutivos validados` com justificativa de cada correcao.

Se nenhum apontamento objetivo existir: pule este passo e va direto ao Passo 6.

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
