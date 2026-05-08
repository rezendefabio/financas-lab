# Prompt — Etapa 2.6: Scripts PowerShell

## Contexto

A Etapa 2.5 (Checkstyle + SpotBugs) foi concluída e fechada via PR #22. `main` está em `d342c0d`, working tree limpo.

Esta etapa cria a camada de **comandos atômicos** do projeto — scripts PowerShell em `scripts/` que encapsulam comandos repetitivos (sobe ambiente, roda testes, valida tudo, faz push). É o princípio "documentação executável" da fábrica: comando, não prosa.

Objetivo do roadmap: comandos atômicos do projeto.

Escopo decidido nesta etapa (calibrado com operador antes da redação):

- **6 scripts**: `setup.ps1`, `dev.ps1`, `test.ps1`, `test-integration.ps1`, `check.ps1`, `ship.ps1`. Lista formalizada em `decisoes.md` desde a Etapa 0.
- **Diferenciação real entre `test.ps1` / `test-integration.ps1` / `check.ps1`:**
  - `test.ps1` → `mvnw test` (apenas testes, sem JaCoCo check / Checkstyle / SpotBugs)
  - `test-integration.ps1` → `mvnw verify -DskipCheckstyle=true -Dspotbugs.skip=true` (testes + JaCoCo, mas pula análise estática)
  - `check.ps1` → `mvnw verify` completo (testes + JaCoCo + Checkstyle + SpotBugs)
- **`ship.ps1` postura conservadora:** roda `check.ps1` + `git push`, **não** cria PR automaticamente — sugere o comando `gh pr create` em string pro operador rodar manualmente.
- **Verificações de pré-requisito enxutas:** `dev.ps1`, `test-integration.ps1`, `check.ps1`, `ship.ps1` checam Docker rodando antes de chamar Maven; `ship.ps1` checa working tree limpo. `setup.ps1` e `test.ps1` sem checagem (Surefire/Maven já reportam).
- **Encoding obrigatório: UTF-8 sem BOM** em todos os `.ps1`. Usar `[System.IO.File]::WriteAllText(<path>, <conteudo>, (New-Object System.Text.UTF8Encoding $false))` ou método equivalente. `Out-File -Encoding UTF8` adiciona BOM e é proibido.
- **Validação destrutiva fica principalmente do lado do operador** (executando manualmente em PowerShell). Agente faz validação básica via `powershell.exe -ExecutionPolicy Bypass -File ...` no bash_tool pra confirmar que o script chega a executar.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra `d342c0d feat: etapa 2.5 — Checkstyle e SpotBugs como gates de mvnw verify (#22)`
- `docs/prompt-etapa-2-6.md` presente como untracked (este próprio arquivo, anexado pelo operador antes de iniciar). Se não aparecer em `git status`, parar e reportar — significa que o arquivo não foi anexado ou está com nome diferente.
- Working tree sem outras mudanças além do prompt untracked acima
- Docker Desktop rodando (necessário pra alguns scripts terem o que validar)

Validar com `git status` e `git log --oneline -1` antes de começar. Se estado divergir, parar e reportar.

## Tarefas

### Tarefa 1 — Criar diretório `scripts/`

```bash
mkdir -p scripts
```

### Tarefa 2 — `scripts/setup.ps1`

**Função:** prepara o ambiente do zero. Sobe Docker Compose, roda `mvnw clean install -DskipTests` (compila + baixa dependências sem rodar testes — esses ficam pra `test-integration.ps1`).

**Conteúdo sugerido:**

```powershell
# scripts/setup.ps1
# Prepara o ambiente local: sobe servicos via Docker Compose,
# baixa dependencias Maven e compila o projeto.
# Ideal para primeira execucao em maquina nova ou reset completo.

$ErrorActionPreference = "Stop"

Write-Host "==> Subindo servicos Docker Compose..." -ForegroundColor Cyan
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Error "Falha ao subir Docker Compose. Docker Desktop esta rodando?"
    exit 1
}

Write-Host ""
Write-Host "==> Baixando dependencias e compilando (sem testes)..." -ForegroundColor Cyan
.\mvnw clean install -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Error "Falha no mvnw clean install."
    exit 1
}

Write-Host ""
Write-Host "Setup concluido com sucesso." -ForegroundColor Green
Write-Host "Proximos passos sugeridos:"
Write-Host "  scripts\dev.ps1                  # subir aplicacao em modo dev"
Write-Host "  scripts\test-integration.ps1     # rodar testes de integracao"
Write-Host "  scripts\check.ps1                # rodar gate completo (CI local)"
```

### Tarefa 3 — `scripts/dev.ps1`

**Função:** sobe ambiente de desenvolvimento ativo. Verifica Docker, sobe compose, roda `mvnw spring-boot:run`.

```powershell
# scripts/dev.ps1
# Sobe ambiente de desenvolvimento: Docker Compose + Spring Boot em foreground.
# Bloqueia o terminal. Ctrl+C para parar a aplicacao.

$ErrorActionPreference = "Stop"

# Verifica Docker rodando antes de tentar subir compose.
docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Error "Docker Desktop nao esta rodando. Inicie o Docker e tente novamente."
    exit 1
}

Write-Host "==> Garantindo que servicos Docker estao up..." -ForegroundColor Cyan
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Error "Falha ao subir Docker Compose."
    exit 1
}

Write-Host ""
Write-Host "==> Iniciando Spring Boot (Ctrl+C para parar)..." -ForegroundColor Cyan
.\mvnw spring-boot:run
```

### Tarefa 4 — `scripts/test.ps1`

**Função:** ciclo rápido de testes. `mvnw test` apenas — sem JaCoCo check, sem Checkstyle, sem SpotBugs.

```powershell
# scripts/test.ps1
# Ciclo rapido: roda os testes via Surefire.
# NAO executa JaCoCo check, Checkstyle nem SpotBugs.
# Para o gate completo (igual ao CI), use scripts\check.ps1.

$ErrorActionPreference = "Stop"

Write-Host "==> Rodando testes (mvnw test)..." -ForegroundColor Cyan
.\mvnw test
$exit = $LASTEXITCODE

Write-Host ""
if ($exit -eq 0) {
    Write-Host "Testes passaram." -ForegroundColor Green
} else {
    Write-Host "Testes falharam (exit $exit)." -ForegroundColor Red
}
exit $exit
```

### Tarefa 5 — `scripts/test-integration.ps1`

**Função:** roda testes + JaCoCo, mas pula Checkstyle e SpotBugs. Útil pra debugar testes sem aguardar análise estática.

```powershell
# scripts/test-integration.ps1
# Roda testes de integracao (mvnw verify) com JaCoCo,
# mas pula Checkstyle e SpotBugs para iteracao mais rapida.
# Para o gate completo, use scripts\check.ps1.

$ErrorActionPreference = "Stop"

# Verifica Docker rodando (Testcontainers precisa).
docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Error "Docker Desktop nao esta rodando. Testcontainers precisa do Docker."
    exit 1
}

Write-Host "==> Rodando testes de integracao (mvnw verify, sem analise estatica)..." -ForegroundColor Cyan
.\mvnw verify "-Dcheckstyle.skip=true" "-Dspotbugs.skip=true"
$exit = $LASTEXITCODE

Write-Host ""
if ($exit -eq 0) {
    Write-Host "Testes de integracao passaram." -ForegroundColor Green
} else {
    Write-Host "Testes de integracao falharam (exit $exit)." -ForegroundColor Red
}
exit $exit
```

**Pesquisar nomes exatos das propriedades de skip antes de fixar.** Os defaults do maven-checkstyle-plugin e do spotbugs-maven-plugin podem variar. Sugestões: `checkstyle.skip` e `spotbugs.skip`. **Se essas propriedades não existirem na versão usada**, parar e reportar antes de prosseguir — não inventar.

### Tarefa 6 — `scripts/check.ps1`

**Função:** gate completo. Espelha o que o CI roda. `mvnw verify` sem skip.

```powershell
# scripts/check.ps1
# Gate completo: espelha o que o GitHub Actions CI roda em pull_request.
# Inclui testes + JaCoCo check + Checkstyle + SpotBugs.
# Se este script passa local, CI deve passar tambem.

$ErrorActionPreference = "Stop"

# Verifica Docker rodando (Testcontainers precisa).
docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Error "Docker Desktop nao esta rodando. Testcontainers precisa do Docker."
    exit 1
}

Write-Host "==> Rodando gate completo (mvnw verify)..." -ForegroundColor Cyan
.\mvnw clean verify
$exit = $LASTEXITCODE

Write-Host ""
if ($exit -eq 0) {
    Write-Host "Gate completo passou. Equivalente ao CI verde." -ForegroundColor Green
} else {
    Write-Host "Gate completo falhou (exit $exit). Veja o output acima." -ForegroundColor Red
}
exit $exit
```

### Tarefa 7 — `scripts/ship.ps1`

**Função:** valida e empurra. Roda `check.ps1`. Se passou e working tree limpo, faz `git push`. Sugere comando pra criar PR (não executa).

```powershell
# scripts/ship.ps1
# Valida tudo localmente e empurra a branch atual para origin.
# NAO cria PR automaticamente — sugere o comando para o operador rodar.

$ErrorActionPreference = "Stop"

# 1. Working tree limpo? (commits feitos)
$status = git status --porcelain
if ($status) {
    Write-Error "Working tree nao esta limpo. Commit ou descarte mudancas antes de ship."
    Write-Host ""
    Write-Host "Mudancas pendentes:"
    Write-Host $status
    exit 1
}

# 2. Branch atual e remote tracking.
$branch = git rev-parse --abbrev-ref HEAD
if ($branch -eq "main" -or $branch -eq "master") {
    Write-Error "Voce esta em '$branch'. Ship deve rodar a partir de uma feature branch."
    exit 1
}

Write-Host "==> Branch: $branch" -ForegroundColor Cyan

# 3. Roda check.ps1 (gate completo).
Write-Host "==> Rodando gate completo antes do push..." -ForegroundColor Cyan
& "$PSScriptRoot\check.ps1"
if ($LASTEXITCODE -ne 0) {
    Write-Error "check.ps1 falhou. Push cancelado."
    exit 1
}

# 4. Push.
Write-Host ""
Write-Host "==> Empurrando '$branch' para origin..." -ForegroundColor Cyan
git push -u origin $branch
if ($LASTEXITCODE -ne 0) {
    Write-Error "git push falhou."
    exit 1
}

# 5. Sugere comando para criar PR.
Write-Host ""
Write-Host "Push concluido." -ForegroundColor Green
Write-Host ""
Write-Host "Para abrir o PR, execute:" -ForegroundColor Cyan
Write-Host ""
Write-Host "  gh pr create --title `"<titulo do PR>`" --body `"<descricao>`""
Write-Host ""
Write-Host "Ou interativamente:"
Write-Host ""
Write-Host "  gh pr create"
```

### Tarefa 8 — Atualizar README.md

Localizar o `README.md` na raiz do projeto e adicionar (sem remover conteúdo existente) uma nova seção "Comandos do projeto", após a seção introdutória. Estrutura sugerida:

```markdown
## Comandos do projeto

Os scripts em `scripts/` encapsulam os comandos atômicos do projeto.

| Comando | Função |
|---|---|
| `scripts\setup.ps1` | Sobe Docker Compose + baixa deps + compila (sem testes). Use ao clonar o repo ou após reset. |
| `scripts\dev.ps1` | Sobe Docker Compose + roda Spring Boot em modo dev. Bloqueia o terminal. |
| `scripts\test.ps1` | Ciclo rápido: `mvnw test`. Sem JaCoCo check, sem análise estática. |
| `scripts\test-integration.ps1` | Testes + JaCoCo, sem Checkstyle/SpotBugs. Útil pra debugar testes. |
| `scripts\check.ps1` | Gate completo. Equivalente ao CI. |
| `scripts\ship.ps1` | Roda `check.ps1` + `git push`. Não cria PR automaticamente. |

### Pré-requisito Windows

PowerShell por default não permite executar scripts não-assinados. Configurar uma vez:

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

### Pré-requisitos do ambiente

- Java 21 (Temurin recomendado)
- Maven (já incluso via Maven Wrapper — `.\mvnw`)
- Docker Desktop rodando
- (opcional) GitHub CLI (`gh`) para fluxo de PRs
```

### Tarefa 9 — Atualizar `decisoes.md`

**9a.** Localizar a seção **"Comandos atômicos do projeto (alvo)"** que existe desde a Etapa 0. Renomear o título removendo "(alvo)" — agora estão implementados:

```markdown
## Comandos atômicos do projeto
```

**9b.** Substituir a tabela existente pela versão final (com a diferenciação `test.ps1` / `test-integration.ps1` / `check.ps1`):

```markdown
| Comando | Função |
|---|---|
| `.\scripts\setup.ps1` | Sobe Postgres + Redis no Docker, instala deps, compila |
| `.\scripts\dev.ps1` | Sobe backend em modo dev (`mvnw spring-boot:run`) |
| `.\scripts\test.ps1` | Ciclo rápido: apenas `mvnw test` (sem análise estática) |
| `.\scripts\test-integration.ps1` | Testes + JaCoCo, pula Checkstyle/SpotBugs |
| `.\scripts\check.ps1` | Gate completo (`mvnw verify`). Equivalente ao CI. |
| `.\scripts\ship.ps1` | `check.ps1` + push. Sugere comando pra abrir PR. |
```

**9c.** Após a tabela, adicionar nota curta sobre encoding e pré-requisitos:

```markdown
**Encoding dos scripts:** UTF-8 sem BOM, obrigatoriamente. PowerShell `Out-File -Encoding UTF8` adiciona BOM e é proibido em arquivos `.ps1`. Usar `[System.IO.File]::WriteAllText(<path>, <conteudo>, (New-Object System.Text.UTF8Encoding $false))` ou método equivalente sem BOM.

**Pré-requisito Windows:** `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` (uma vez por usuário). Documentado no README.
```

**9d.** Adicionar linha no **"Histórico de mudanças"** no fim do arquivo, no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.6 concluída: 6 scripts PowerShell em `scripts/` implementados (`setup`, `dev`, `test`, `test-integration`, `check`, `ship`). Diferenciação real entre `test.ps1` (rápido), `test-integration.ps1` (testes + JaCoCo) e `check.ps1` (gate completo, espelho do CI). Encoding UTF-8 sem BOM formalizado.
```

### Tarefa 10 — Atualizar `progresso.md`

**10a.** Atualizar campo "Última atualização" no topo: `2026-05-08 (Etapa 2.6)`.

**10b.** Marcar como `[x]` na seção da Camada 1 o critério `Scripts PowerShell criados: setup.ps1, dev.ps1, test.ps1, check.ps1, ship.ps1`. Nota: a lista do critério está incompleta (faltava `test-integration.ps1` na declaração original); manter o critério como está mas marcar `[x]` — se quiser ajustar a redação do critério, fica decisão fora do escopo desta etapa.

**10c.** Adicionar nova seção **"Lições da Etapa 2.6"** logo antes de **"Lições da Etapa 2.5"** (mantendo ordem decrescente). Conteúdo: candidatos a hook e lições de ambiente que **realmente forem observados durante a execução**. Padrão pra preencher quando nada digno emergir:

```markdown
## Lições da Etapa 2.6

### Candidatos a hook (automatizar em etapas futuras)

(Nenhum novo nesta etapa.)

### Lições de ambiente

(Nenhuma nova nesta etapa.)
```

**Regra dura:** só registrar lições **realmente observadas**. Se o agente bater em pegadinha de encoding (BOM detectado depois da criação), problema com `$LASTEXITCODE` em PowerShell, comportamento inesperado de `docker info` em retorno de erro, etc. — registrar honestamente.

**10d.** Adicionar entrada no **"Histórico de mudanças deste documento"** no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.6 concluída: 6 scripts PowerShell criados em `scripts/`. README atualizado com tabela de comandos + pré-requisito ExecutionPolicy. Mergeado via PR #XX.
```

(`#XX` é placeholder. Substituir pelo número real do PR num commit adicional **depois** que o PR for aberto.)

### Tarefa 11 — Versionar este próprio prompt

Confirmar que `docs/prompt-etapa-2-6.md` está em disco como untracked e incluir no commit de docs.

### Tarefa 12 — Validação básica

Antes de commitar, confirmar:

1. **Encoding sem BOM:** rodar (em bash do agente)
   ```bash
   file scripts/*.ps1
   ```
   Output esperado: cada arquivo identificado como ASCII text ou UTF-8 (sem menção a BOM). Se aparecer "UTF-8 (with BOM)" em algum, recriar o arquivo correto antes de commitar.

2. **Sintaxe PowerShell:** rodar uma checagem mínima de cada script via `powershell.exe -Command` em bash:
   ```bash
   powershell.exe -ExecutionPolicy Bypass -Command "Get-Content scripts/setup.ps1 | Out-Null; if (\$LASTEXITCODE -ne 0) { exit 1 }"
   ```
   Repetir pra cada um dos 6. Se algum reportar erro de parse, corrigir.

3. **Não executar scripts no agente.** Os scripts mexem com Docker Desktop, fazem push pro GitHub, etc. **Validação destrutiva e comportamental fica do lado do operador**, executando manualmente em PowerShell. O agente só verifica encoding e sintaxe.

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Arquivos permitidos:
   - `scripts/setup.ps1` (novo)
   - `scripts/dev.ps1` (novo)
   - `scripts/test.ps1` (novo)
   - `scripts/test-integration.ps1` (novo)
   - `scripts/check.ps1` (novo)
   - `scripts/ship.ps1` (novo)
   - `README.md`
   - `docs/decisoes.md`
   - `docs/progresso.md`
   - `docs/prompt-etapa-2-6.md` (este arquivo)

2. **"Necessidade técnica direta" não é exceção válida à Restrição 1.** Lição da 2.2 + 2.4 + 2.5: tentar resolver gap silenciosamente (mexer em `pom.xml`, `application.yml`, `ci.yml`, scripts existentes do projeto) é o padrão exato que essa restrição existe pra bloquear.

3. **Não tocar em `pom.xml` nem em `ci.yml`.** Os scripts apenas chamam `mvnw` com flags que já são suportadas pelos plugins existentes. Se alguma flag não existir (ex: `-Dcheckstyle.skip` não funciona), parar e reportar — **não** ajustar a configuração do plugin.

4. **Não tocar em `.claude/settings.local.json`.** Pré-aprovação de scripts é decisão pessoal do operador, fora do escopo desta etapa.

5. **Encoding UTF-8 SEM BOM em todos os `.ps1`.** Ferramenta de criação que adiciona BOM (`Out-File -Encoding UTF8` em PowerShell, alguns editores) é proibida. Usar APIs explícitas que não adicionam BOM. Validar com `file scripts/*.ps1` antes de commitar.

6. **Não executar os scripts no agente.** O bash_tool do agente é bash (não PowerShell), e os scripts esperam ser invocados em PowerShell de verdade. Tentar executar `.ps1` via bash_tool pode dar resultados enganosos (script não roda, ou roda parcialmente, ou erra de forma diferente). Validação comportamental fica com o operador.

7. **Não criar `application-dev.yml`, `application-prod.yml`, ou qualquer outro arquivo.** Fora do escopo.

8. **Não antecipar Etapa 2.7 (Next.js).** Não criar `frontend/`. Não rodar `create-next-app`. Não tocar em qualquer coisa relacionada ao frontend.

9. **Não criar wrapper bash para os scripts.** Tentação: criar `scripts/setup.sh` que invoca `setup.ps1`. Recusar — esta etapa é especificamente PowerShell, e wrappers cross-shell adicionam complexidade que não foi pedida.

10. **Pesquisar antes de chutar nomes de propriedades de skip.** `-Dcheckstyle.skip` e `-Dspotbugs.skip` são razoáveis mas precisam ser confirmados contra documentação dos plugins. Se a versão usada não suporta, **parar e reportar** antes de inventar workaround.

11. **`bash_tool` do Claude Code é bash, não PowerShell.** Comandos de validação do agente devem ser sintaxe POSIX/bash. Pra invocar PowerShell de dentro do bash, usar `powershell.exe -Command ...` ou `powershell.exe -File ...`. Lição registrada na 2.5.

## Estrutura de commits

Branch: `feat/scripts-powershell`

Commits atômicos, em ordem:

**Commit 1** — `feat: cria scripts PowerShell de setup, dev e teste`
- `scripts/setup.ps1`
- `scripts/dev.ps1`
- `scripts/test.ps1`
- `scripts/test-integration.ps1`

**Commit 2** — `feat: cria scripts PowerShell de gate completo e ship`
- `scripts/check.ps1`
- `scripts/ship.ps1`

**Commit 3** — `docs: atualiza README com tabela de comandos do projeto`
- `README.md`

**Commit 4** — `docs: registra etapa 2.6 (scripts PowerShell) em decisoes e progresso`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-2-6.md`

## Validação antes de abrir PR

```bash
# Encoding limpo (sem BOM):
file scripts/*.ps1

# Sintaxe parseavel em todos:
for f in scripts/*.ps1; do
    powershell.exe -ExecutionPolicy Bypass -Command "Get-Command -Syntax (Resolve-Path $f) > \$null; exit \$LASTEXITCODE" 2>&1 | head -3
done

# Working tree limpo:
git status

# Commits visiveis:
git log --oneline -6
```

## PR

Título: `feat: etapa 2.6 — scripts PowerShell para comandos atômicos do projeto`

Body sugerido (ajustar com observações reais da execução):

```markdown
## Summary

Implementa a Etapa 2.6 do roadmap: comandos atômicos do projeto encapsulados em scripts PowerShell em `scripts/`.

### Mudanças

- `scripts/setup.ps1`: Docker Compose up + `mvnw clean install -DskipTests`. Use ao clonar o repo ou após reset.
- `scripts/dev.ps1`: verifica Docker + `docker compose up -d` + `mvnw spring-boot:run`. Bloqueia o terminal.
- `scripts/test.ps1`: ciclo rápido — `mvnw test`. Sem JaCoCo check / Checkstyle / SpotBugs.
- `scripts/test-integration.ps1`: testes + JaCoCo, pula Checkstyle e SpotBugs (`-Dcheckstyle.skip=true -Dspotbugs.skip=true`).
- `scripts/check.ps1`: gate completo — `mvnw verify`. Espelha o CI.
- `scripts/ship.ps1`: working tree limpo + branch ≠ main + `check.ps1` verde → `git push`. Sugere comando `gh pr create` mas não executa.
- `README.md`: nova seção "Comandos do projeto" com tabela e pré-requisitos.
- `decisoes.md`: seção "Comandos atômicos" atualizada (de "alvo" pra implementado), encoding sem BOM formalizado.
- `progresso.md`: marca critério, registra lições.

### Decisões de escopo

- **Diferenciação real entre `test.ps1` / `test-integration.ps1` / `check.ps1`** — três níveis de gate, do mais rápido ao mais completo.
- **`ship.ps1` não cria PR automaticamente** — sugere o comando, operador decide. Sem surpresa.
- **Encoding UTF-8 sem BOM obrigatório** em todos os `.ps1`.

### Validação

- Encoding confirmado via `file scripts/*.ps1`: todos UTF-8 sem BOM.
- Sintaxe confirmada via parser do PowerShell em cada script.
- **Validação comportamental destrutiva fica com o operador** — agente não roda `.ps1` no bash_tool.

### Próximo passo

Etapa 2.7 (inicializar Next.js) — fora do escopo deste PR.
```

## Pós-criação do PR

Antes de mergear, fazer commit adicional **na mesma branch** corrigindo o `#XX` do `progresso.md`:

1. Abrir o PR via `gh pr create`.
2. Capturar o número retornado.
3. Editar `docs/progresso.md` substituindo `Mergeado via PR #XX` por `Mergeado via PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico do progresso.md`
5. Push.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- `main` local sincronizada com `origin/main`, incluindo squash commit da 2.6
- `git status` limpo
- 6 scripts em `scripts/` versionados, encoding UTF-8 sem BOM
- README atualizado
- `progresso.md` reflete 2.6 concluída, número real do PR no histórico
- Branch `feat/scripts-powershell` deletada local e remotamente

Reportar ao operador o estado final com `git log --oneline -3` e `git status`. Parar.

## O que NÃO fazer ao terminar

- Não criar prompt da 2.7
- Não inicializar Next.js, não rodar `create-next-app`, não criar diretório `frontend/`
- Não pré-aprovar `Bash(./scripts/*.ps1)` em `.claude/settings.local.json` — ignora e sai
- Não criar wrappers bash dos scripts
- Não executar os scripts no bash_tool — validação comportamental é do operador
- Não sugerir "próximo passo" espontaneamente. Fim de etapa = parada explícita.
