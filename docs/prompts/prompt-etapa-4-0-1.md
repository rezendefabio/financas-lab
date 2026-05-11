# Prompt — Etapa 4.0.1: Fix de posição do bloco core.hooksPath em setup.ps1

## Contexto

A Sub-etapa 4.0 foi mergeada (PR #38) abrindo a Camada 3. Smoke test destrutivo pós-merge em clone novo (`C:\tempo\financas-lab-smoke\`) revelou um bug operacional no `setup.ps1`: o bloco que configura `git config core.hooksPath .githooks` foi posicionado **depois** de `docker compose up -d` e `mvnw clean install`. Quando o Docker falha (cenário real do smoke test: conflito de nomes de containers entre clones paralelos), o script aborta em `exit 1` antes de chegar no bloco do git config. Resultado: clone novo em ambiente com Docker comprometido fica com a estrutura `.claude/` presente mas o mecanismo de hooks **inerte** — `core.hooksPath` nunca é setado, e os hooks da Sub-etapa 4.1+ serão fisicamente presentes e logicamente ignorados pelo git.

Categoria do bug: **prescrição de prompt insuficientemente específica**, não decisão silenciosa do agente. O prompt da 4.0 prescreveu "antes da finalização do script (antes da mensagem de sucesso)"; agente seguiu literalmente. Em uso normal (Docker rodando), funciona. Em uso degradado, falha silenciosa.

Esta sub-etapa **corrige o bug**, **registra a lição com categoria precisa**, e **registra como débito conhecido** um segundo achado do smoke test (`docker-compose.yml` com `container_name:` fixo causando conflito entre clones paralelos — débito de configuração, não bloqueante hoje).

Quando esta etapa terminar, o `setup.ps1` configurará `core.hooksPath` de forma robusta independentemente do estado do Docker, e o débito do `container_name:` estará formalmente registrado para revisão futura.

## Padrões que estreiam nesta etapa

1. **Sub-etapa de fix descoberta exclusivamente em validação destrutiva manual** — quarta ocorrência (2.6.1, 2.6.2, 2.8, agora 4.0.1). Reforça a função do smoke test como instrumento de qualidade que CI não substitui.
2. **Validação destrutiva no roteiro de execução do agente** — primeira sub-etapa em que o prompt prescreve cenário de falha (Docker parado) como condição de "pronto", não só como sugestão pós-merge ao operador.
3. **Categorização precisa de raiz da lição** — primeira lição classificada como "prescrição de prompt insuficientemente específica" em vez de "decisão silenciosa do agente". Categorias diferentes, soluções diferentes.

## Escopo decidido (calibrado com operador antes da redação)

### O fix técnico

Mover o bloco `core.hooksPath` para entre a **validação do `.env`** (que termina na linha 18 atual) e o **bloco do Docker** (que começa na linha 20 atual).

**Posição alvo no arquivo final:**

```
linha  6:  $ErrorActionPreference = "Stop"
linhas  8-18: validação de .env (inalterada)
linhas NOVAS: bloco core.hooksPath (movido para cá)
linhas SEGUINTES: docker compose up -d (inalterado, apenas deslocado)
... resto do arquivo (mvnw, mensagem final) inalterado
```

**Justificativa narrativa:** validações estáticas (existência de `.env`) → configuração local de git (não depende de Docker, Maven, internet) → operações dependentes de ambiente externo (Docker, Maven). Cada camada do script é mais frágil que a anterior; configurações locais devem vir antes de operações remotas.

**Remover o bloco antigo** (linhas 35-42 atuais) por inteiro. Não deixar duplicação.

### Conteúdo exato do bloco a mover

O bloco mantém comportamento idêntico — apenas muda de posição:

```powershell
Write-Host "==> Configurando core.hooksPath..." -ForegroundColor Cyan
git config core.hooksPath .githooks
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERRO] Falha ao configurar core.hooksPath." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] core.hooksPath configurado para .githooks/" -ForegroundColor Green
Write-Host ""
```

**Nota crítica:** terminar o bloco com `Write-Host ""` (linha em branco) para preservar legibilidade visual antes do bloco do Docker que começa com `Write-Host "==> Subindo servicos Docker Compose..."`.

### Resultado esperado no `setup.ps1` (visão completa após o fix)

```powershell
# scripts/setup.ps1
# Prepara o ambiente local: sobe servicos via Docker Compose,
# baixa dependencias Maven e compila o projeto.
# Ideal para primeira execucao em maquina nova ou reset completo.

$ErrorActionPreference = "Stop"

if (-not (Test-Path .env)) {
    if (Test-Path .env.example) {
        Copy-Item .env.example .env
        Write-Host "AVISO: .env nao encontrado. Criado a partir de .env.example." -ForegroundColor Yellow
        Write-Host "Revise as credenciais em .env antes de usar em ambiente compartilhado." -ForegroundColor Yellow
    } else {
        Write-Host "ERRO: .env nao encontrado e .env.example tambem nao existe." -ForegroundColor Red
        Write-Host "Repositorio parece corrompido. Verifique o clone." -ForegroundColor Red
        exit 1
    }
}

Write-Host "==> Configurando core.hooksPath..." -ForegroundColor Cyan
git config core.hooksPath .githooks
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERRO] Falha ao configurar core.hooksPath." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] core.hooksPath configurado para .githooks/" -ForegroundColor Green
Write-Host ""

Write-Host "==> Subindo servicos Docker Compose..." -ForegroundColor Cyan
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "Falha ao subir Docker Compose. Docker Desktop esta rodando?" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "==> Baixando dependencias e compilando (sem testes)..." -ForegroundColor Cyan
.\mvnw clean install -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Falha no mvnw clean install." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Setup concluido com sucesso." -ForegroundColor Green
Write-Host "Proximos passos sugeridos:"
Write-Host "  scripts\dev.ps1                  # subir aplicacao em modo dev"
Write-Host "  scripts\test-integration.ps1     # rodar testes de integracao"
Write-Host "  scripts\check.ps1                # rodar gate completo (CI local)"
```

**O que mudou:** bloco do `core.hooksPath` saiu das linhas 35-42 e entrou entre o `.env` e o Docker. Tudo o mais inalterado.

### Atualização da lição no `progresso.md`

Editar a seção "Lições da Sub-etapa 4.0" — manter a "sétima recorrência" sobre `.gitignore` (continua válida, foi decisão silenciosa legítima), **adicionar** nova entrada na subseção "Lições de ambiente" (ou criar se não existir) com a categoria "prescrição insuficientemente específica":

```markdown
### Lições de ambiente (descobertas em smoke test destrutivo pós-merge)

**Bug operacional: bloco `core.hooksPath` em posição inadequada no `setup.ps1`.** Prompt da 4.0 prescreveu "antes da finalização do script (antes da mensagem de sucesso)". Agente seguiu literalmente — colocou após `docker compose up -d` e `mvnw clean install`. Em uso normal (Docker rodando), funciona. Quando Docker falha (cenário real do smoke test: conflito de container_name entre clones paralelos), script aborta em `exit 1` antes do bloco e `core.hooksPath` nunca é configurado. Resultado: clone novo em ambiente com Docker quebrado fica com estrutura `.claude/` presente mas mecanismo de hooks da 4.1+ inerte — falha silenciosa exata que a retrospectiva da Camada 1 documentou como cara de descobrir tarde.

**Categoria da lição: prescrição de prompt insuficientemente específica, não decisão silenciosa do agente.** Instrução vaga ("antes da finalização") gera execução tecnicamente correta mas operacionalmente frágil. Reforça princípio já conhecido: prompts cirúrgicos exigem especificidade absoluta sobre **invariantes** (o que NÃO pode falhar para o bloco rodar), não só sobre **fronteiras** (onde colocar). Resolvido na Sub-etapa 4.0.1.

**Segundo achado do smoke test: `docker-compose.yml` com `container_name:` fixo.** `financas-lab-postgres` e `financas-lab-redis` têm nome global; dois clones em paralelo disparam conflito do Docker daemon. Sem impacto em fluxo normal (1 clone por vez). Registrado como débito em `hooks-pendentes.md`, seção "Débitos de configuração". Resolver quando paralelismo de clones virar necessidade real (debugging em branch isolada, smoke test sistematizado pós-merge). Custo estimado: 1-2h.

**Reforço do princípio: smoke test destrutivo continua sendo instrumento de qualidade de primeira linha, mesmo em sub-etapas "só de infraestrutura".** A 4.0 parecia o tipo de etapa sem código novo, sem feature, só pasta e config — aparentemente dispensável de validação destrutiva rigorosa. Errado. Bug não estava no código, estava na **ordem de instruções** do `setup.ps1` — invisível em revisão de diff, visível em validação destrutiva real com cenário de falha. Quinta ocorrência consecutiva (2.6.1, 2.6.2, 2.8, 3.3.1, 4.0) em que smoke test destrutivo pegou bug que CI não pegaria.
```

### Atualização do `hooks-pendentes.md`

Adicionar na seção "Débitos de configuração" (logo após a entrada de `application-prod.yml` ausente):

```markdown
- **Containers Docker com `container_name:` fixo no `docker-compose.yml`.** (Descoberto no smoke test pós-merge da Sub-etapa 4.0, registrado na 4.0.1.) `financas-lab-postgres` e `financas-lab-redis` têm nome global no Docker daemon. Tentar subir um segundo clone em paralelo dispara conflito (`Error response from daemon: Conflict. The container name "/financas-lab-postgres" is already in use...`). Sem impacto em fluxo normal (1 clone por vez). Workaround manual: `docker rm -f financas-lab-postgres financas-lab-redis` antes de rodar `setup.ps1` no segundo clone. Resolver quando paralelismo de clones virar necessidade real (debugging em branch isolada com containers separados, smoke test sistematizado pós-merge, ou ambiente CI local rodando em paralelo). Fix: remover `container_name:` deixando Docker Compose gerar nomes prefixados pelo diretório do projeto. Custo estimado: 1-2h incluindo ajustes em qualquer script que referencie container por nome e revalidação destrutiva.
```

Atualizar a linha **"Última atualização:"** no topo do arquivo:

```
**Última atualização:** 2026-05-10 (Sub-etapa 4.0.1 — fix posição core.hooksPath + débito container_name)
```

### Atualização do `decisoes.md`

Adicionar entrada no histórico no final do arquivo:

```markdown
- **2026-05-10** — Sub-etapa 4.0.1 concluída: fix de posição do bloco `core.hooksPath` em `setup.ps1`. Bloco movido de depois de `docker compose up -d` + `mvnw clean install` para entre validação de `.env` e `docker compose up -d`. Bug descoberto em smoke test destrutivo pós-merge da 4.0 (clone novo com Docker em conflito de nomes). Lição registrada com categoria "prescrição de prompt insuficientemente específica". Débito do `container_name:` fixo no `docker-compose.yml` registrado em `hooks-pendentes.md`. Mergeado via PR #XX.
```

### Atualização do `progresso.md` — entrada no histórico

```markdown
- **2026-05-10** — Sub-etapa 4.0.1 concluída: fix do `setup.ps1`. Bloco `core.hooksPath` movido para posição que sobrevive a falha de Docker. Lição categorizada como "prescrição insuficientemente específica" (não decisão silenciosa). Débito Docker `container_name:` registrado. Mergeado via PR #XX.
```

### Versionar este próprio prompt

`docs/prompt-etapa-4-0-1.md` no commit de docs.

### Localização dos arquivos modificados/criados

```
scripts/setup.ps1                                ← edição (mover bloco)
docs/progresso.md                                ← edição (lições + histórico)
docs/hooks-pendentes.md                          ← edição (débito + data)
docs/decisoes.md                                 ← edição (histórico)
docs/prompt-etapa-4-0-1.md                       ← novo (este próprio prompt)
```

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`.
- `git log --oneline -1` mostra commit do squash da Sub-etapa 4.0 (PR #38).
- `docs/prompt-etapa-4-0-1.md` presente como **untracked**.
- Working tree limpo (exceto o prompt).
- `git config core.hooksPath` retorna `.githooks` (configurado pela 4.0 no projeto principal).
- `scripts/setup.ps1` contém o bloco `core.hooksPath` nas linhas 35-42 (posição que será corrigida).

Validar com:

```bash
git status
git log --oneline -3
ls docs/prompt-etapa-4-0-1.md
git config core.hooksPath
grep -n "core.hooksPath" scripts/setup.ps1
```

Esperado:
- Working tree limpo (exceto prompt).
- Squash da 4.0 visível.
- `core.hooksPath` retorna `.githooks`.
- `grep` mostra ocorrência única do termo `core.hooksPath` em `scripts/setup.ps1`, em linha próxima a 35-42.

Se algum item divergir, **parar e reportar**.

## Tarefas

### Tarefa 1 — Validar pré-requisitos

Rodar comandos da seção "Estado esperado ao iniciar". Se algum item divergir, parar e reportar.

### Tarefa 2 — Criar branch de trabalho

```bash
git checkout -b fix/etapa-4-0-1-setup-hookspath-position
```

Prefixo `fix/` em vez de `chore/` — esta etapa corrige um bug operacional real, não apenas reorganiza.

### Tarefa 3 — Antes de editar, ler arquivos vivos

```bash
cat scripts/setup.ps1
cat docs/progresso.md
cat docs/hooks-pendentes.md
cat docs/decisoes.md
```

**Confirmar a estrutura atual do `setup.ps1`** corresponde à descrita no contexto (bloco `core.hooksPath` nas linhas 35-42, após `docker compose` e `mvnw`).

Se a estrutura divergir, **parar e reportar** — o fix prescrito assume essa estrutura exata.

### Tarefa 4 — Editar `scripts/setup.ps1` (mover o bloco)

Operação em duas etapas, **na mesma edição**:

1. **Remover** o bloco antigo (linhas 35-42 aproximadamente, incluindo a linha em branco que precede o bloco):

```powershell
Write-Host ""
Write-Host "==> Configurando core.hooksPath..." -ForegroundColor Cyan
git config core.hooksPath .githooks
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERRO] Falha ao configurar core.hooksPath." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] core.hooksPath configurado para .githooks/" -ForegroundColor Green
```

2. **Inserir** o bloco em nova posição, entre o `}` final do `if (-not (Test-Path .env))` (linha 18) e o `Write-Host "==> Subindo servicos Docker Compose..."` (linha 20):

```powershell
Write-Host "==> Configurando core.hooksPath..." -ForegroundColor Cyan
git config core.hooksPath .githooks
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERRO] Falha ao configurar core.hooksPath." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] core.hooksPath configurado para .githooks/" -ForegroundColor Green
Write-Host ""
```

**Nota crítica:** o `Write-Host ""` agora vai ao final do bloco (não no início), separando-o visualmente do bloco do Docker que vem em seguida. Sem isso, fica mais difícil ler o output.

**Confirmar o estado final do arquivo** corresponde ao mostrado em "Resultado esperado no `setup.ps1` (visão completa após o fix)" do escopo decidido. Especificamente:

- Linha 6: `$ErrorActionPreference = "Stop"`.
- Linhas 8-18: bloco `.env` inalterado.
- Linhas seguintes (~20-28): bloco `core.hooksPath` movido para cá.
- Linhas após o bloco movido: `docker compose up -d`, depois `mvnw clean install`, depois mensagem final.
- **Não pode haver duplicação do bloco** `core.hooksPath`.
- **Não pode haver vestígio** do bloco antigo em sua posição original.

Validar com:

```bash
grep -nc "core.hooksPath" scripts/setup.ps1
grep -n "core.hooksPath" scripts/setup.ps1
```

Esperado:
- `grep -c` retorna ocorrências totais (espera-se 2-3 dependendo de comentário; importante é que NÃO seja duplicado).
- `grep -n` mostra a posição numérica das ocorrências — devem estar **antes** da linha `docker compose up -d`.

Se alguma divergência, **parar e reportar**.

### Tarefa 5 — Validação destrutiva local do fix (Docker rodando)

```powershell
git config --unset core.hooksPath
git config core.hooksPath
pwsh -File scripts/setup.ps1
git config core.hooksPath
```

Esperado:
- Após `--unset`: retorna vazio.
- `setup.ps1` roda até o final sem erro (Docker rodando).
- `git config core.hooksPath` retorna `.githooks`.

### Tarefa 6 — Validação destrutiva **com Docker propositalmente quebrado** (cenário do bug)

Este é o cenário que reproduz o bug original. **É o teste que prova que o fix funciona.**

**Cenário a reproduzir:** Docker rodando mas com conflito de nome de container (igual ao smoke test pós-merge da 4.0).

```powershell
# Garantir que pelo menos um container do projeto principal está criado
# (rodar dev.ps1 do projeto principal antes desta etapa, ou criar manualmente:)
docker run -d --name financas-lab-postgres postgres:16 2>&1 | head -1
docker run -d --name financas-lab-redis redis:7 2>&1 | head -1

# Resetar core.hooksPath
git config --unset core.hooksPath
git config core.hooksPath

# Rodar setup.ps1 — DEVE configurar core.hooksPath ANTES de tentar Docker
pwsh -File scripts/setup.ps1

# Verificar resultado
$exitCode = $LASTEXITCODE
git config core.hooksPath
Write-Host "Exit code do setup.ps1: $exitCode"
```

**Esperado (este é o ponto da etapa):**
- `setup.ps1` **vai falhar** no bloco do Docker (conflito de container_name) com `exit 1`.
- **Antes** de falhar, deve ter rodado o bloco do `core.hooksPath`.
- `git config core.hooksPath` retorna `.githooks` **mesmo após a falha do Docker**.
- Exit code do `setup.ps1`: 1 (correto — Docker falhou).

**Se `core.hooksPath` retornar vazio após `setup.ps1` falhar no Docker, o fix não funcionou.** Parar e reportar.

**Limpeza após o teste:**

```powershell
docker rm -f financas-lab-postgres financas-lab-redis 2>&1 | head -2
```

**Notas críticas:**

1. **Não rodar este teste em ambiente compartilhado com produção.** É puramente local.
2. **Reproduzir o cenário exato do bug original.** O smoke test pós-merge da 4.0 mostrou o erro "container name '/financas-lab-postgres' is already in use". Este teste reproduz isso intencionalmente.
3. **Se Docker Desktop não estiver rodando**, o teste muda ligeiramente: `setup.ps1` ainda falhará no `docker compose up -d`, mas com mensagem "Cannot connect to Docker daemon" em vez de conflito de nome. O resultado esperado (bloco `core.hooksPath` rodou antes) deve ser o mesmo. Reportar qual cenário foi usado.

### Tarefa 7 — Editar `docs/progresso.md`

**7a.** Atualizar "Última atualização": `2026-05-10 (Sub-etapa 4.0.1 — fix posição core.hooksPath)`.

**7b.** Localizar a seção "Lições da Sub-etapa 4.0". **Não remover** a entrada existente sobre `.gitignore` (sétima recorrência — continua válida). **Adicionar** o conteúdo descrito em "Atualização da lição no `progresso.md`" do escopo decidido. Se subseção "Lições de ambiente" já existir vazia (do prompt original da 4.0 que previu o placeholder), preencher; se não existir, criar.

**7c.** Adicionar subseção "Sub-etapa 4.0.1 — Fix de posição do bloco core.hooksPath" sob "Camada 3 — Configuração do Claude Code", logo após a entrada da 4.0:

```markdown
- **4.0.1 — Fix de posição do bloco core.hooksPath** (2026-05-10): `setup.ps1` reorganizado para configurar `core.hooksPath` ANTES de operações que podem falhar (Docker, Maven). Bug descoberto em smoke test destrutivo pós-merge da 4.0. Validação destrutiva com Docker propositalmente quebrado confirma fix. Débito Docker `container_name:` registrado em `hooks-pendentes.md`. PR #XX.
```

**7d.** Adicionar entrada no histórico geral (final do arquivo).

### Tarefa 8 — Editar `docs/hooks-pendentes.md`

Adicionar entrada na seção "Débitos de configuração" conforme escopo. Atualizar "Última atualização" no topo.

### Tarefa 9 — Editar `docs/decisoes.md`

Adicionar entrada no histórico conforme escopo. Esta etapa **não** adiciona nova decisão estrutural — apenas corrige uma posição no `setup.ps1` e registra dois aprendizados. Não criar nova seção; apenas adicionar linha no histórico.

### Tarefa 10 — Versionar este próprio prompt

`git add docs/prompt-etapa-4-0-1.md`.

### Tarefa 11 — Validação final antes de commitar

```bash
git status
grep -n "core.hooksPath" scripts/setup.ps1
.\scripts\check.ps1
```

Esperado:
- `git status` mostra 5 arquivos modificados/novos:
  - `scripts/setup.ps1` (modificado)
  - `docs/progresso.md` (modificado)
  - `docs/hooks-pendentes.md` (modificado)
  - `docs/decisoes.md` (modificado)
  - `docs/prompt-etapa-4-0-1.md` (novo)
- `grep` confirma bloco do `core.hooksPath` em posição anterior ao bloco Docker.
- `check.ps1` passa (suite Java intocada por esta etapa).

Se `check.ps1` falhar por motivo não relacionado (Docker parado pra reproduzir o cenário da Tarefa 6 e não religado, etc.), reportar e prosseguir — CI vai validar isoladamente.

## Restrições e freios

1. **Não tocar em nenhum outro script `.ps1`.** Apenas `setup.ps1`. `dev.ps1`, `test.ps1`, `check.ps1`, `ship.ps1`, `test-integration.ps1` permanecem inalterados.

2. **Não tocar em `src/`, `pom.xml`, `application*.yml`, `frontend/`, `docker-compose.yml`, migrations.**

3. **Não corrigir o débito do `container_name:` no `docker-compose.yml`.** Esta etapa apenas **registra** esse débito. Resolução é decisão futura.

4. **Não criar hooks funcionais, subagents ou skills.** Continua sendo escopo da 4.1+.

5. **Não criar CLAUDE.md.** Sub-etapa 4.3.

6. **Não tocar em `.claude/`, `.githooks/`, ou `.gitignore`.** Estrutura criada na 4.0 está correta e foi validada destrutivamente.

7. **Não tocar em ADRs (`docs/adrs.md`).** ADRs anteriores permanecem. Esta etapa não adiciona ADR novo — não é decisão estrutural, é fix operacional.

8. **Não modificar o conteúdo do bloco `core.hooksPath`** — apenas movê-lo. Mesmas mensagens, mesmo comportamento, mesma lógica de `$LASTEXITCODE`.

9. **Encoding UTF-8 sem BOM** em todos os arquivos.

10. **Linhas em branco antes e depois de headers Markdown.**

11. **Sem acentos no código** (mensagens do `setup.ps1`).

12. **Não usar `Write-Error` + `exit`.** Padrão consolidado: `Write-Host -ForegroundColor Red` + `exit 1`.

13. **Não inflar o escopo da lição.** O texto prescrito em "Atualização da lição no `progresso.md`" é o conteúdo a registrar. Não adicionar análise extra, não filosofar sobre teoria de prompts.

14. **Não reclassificar a "sétima recorrência" do `.gitignore`.** Aquela continua sendo decisão silenciosa legítima. Este bug do `setup.ps1` é categoria diferente — "prescrição insuficientemente específica". Manter as duas separadas no `progresso.md`.

15. **Não pular a Tarefa 6.** Validação destrutiva com Docker quebrado é o gate de "pronto" desta etapa. Sem ela, não há prova de que o fix funciona.

16. **Não tomar decisão silenciosa em zona limítrofe.** Se a Tarefa 6 produzir comportamento diferente do esperado (ex: `core.hooksPath` retorna `.githooks` mas vem de um cache do git em vez do bloco novo), parar e reportar — não silenciar com workaround.

17. **Não sugerir próxima etapa espontaneamente.** Esta etapa termina com PR aberto, CI verde, **aguardando autorização explícita** para merge.

18. **Antes de escrever cada edição, ler o arquivo vivo** (Tarefa 3).

19. **Lições da Sub-etapa 4.0.1 — não criar seção própria.** Esta etapa registra lição **da 4.0** (o bug foi originado lá), não da 4.0.1. Não criar "Lições da Sub-etapa 4.0.1" no `progresso.md`. Adicionar à seção existente da 4.0.

## Estrutura de commits

Branch: `fix/etapa-4-0-1-setup-hookspath-position`

**Commit 1** — `fix(setup): move bloco core.hooksPath para antes de docker compose`
- 1 arquivo: `scripts/setup.ps1`.
- Body do commit: explica brevemente que o bloco em posição anterior não rodava quando Docker falhava; movido para entre validação de `.env` e `docker compose up -d`.

**Commit 2** — `docs: registra licao categorizada e debito Docker da Sub-etapa 4.0`
- 3 arquivos: `docs/progresso.md`, `docs/hooks-pendentes.md`, `docs/decisoes.md`.

**Commit 3** — `docs: versiona prompt da etapa 4.0.1`
- 1 arquivo: `docs/prompt-etapa-4-0-1.md`.

## Validação antes de abrir PR

```bash
.\scripts\check.ps1
git status
git log --oneline -4
git config core.hooksPath
grep -n "core.hooksPath" scripts/setup.ps1
```

Esperado:
- `check.ps1` passa.
- Working tree limpo.
- 3 commits novos.
- `core.hooksPath` retorna `.githooks`.
- `grep` mostra bloco em posição anterior ao `docker compose`.

## PR

Título: `fix: sub-etapa 4.0.1 — corrige posição do bloco core.hooksPath em setup.ps1`

Body sugerido:

```markdown
## Summary

Fix de bug operacional descoberto em smoke test destrutivo pós-merge da Sub-etapa 4.0 (PR #38). O bloco que configura `git config core.hooksPath .githooks` estava posicionado **depois** de `docker compose up -d` e `mvnw clean install` no `setup.ps1`. Quando Docker falha (cenário real: conflito de `container_name:` entre clones paralelos), script aborta antes do bloco, e `core.hooksPath` nunca é configurado — clone novo com Docker comprometido fica com estrutura `.claude/` presente mas mecanismo de hooks da 4.1+ inerte (falha silenciosa).

Fix: mover o bloco para **entre** a validação de `.env` e `docker compose up -d`. Bloco depende apenas do diretório ser repo git — operação local que sobrevive a falhas de ambiente externo (Docker, Maven, internet).

### Categoria do bug

**Prescrição de prompt insuficientemente específica**, não decisão silenciosa do agente. O prompt da Sub-etapa 4.0 prescreveu "antes da finalização do script (antes da mensagem de sucesso)"; agente seguiu literalmente. Em uso normal (Docker rodando), funciona. Em uso degradado, falha silenciosa.

Lição registrada em `docs/progresso.md` sob a Sub-etapa 4.0 (não a 4.0.1) — o bug se originou na 4.0; a 4.0.1 apenas resolve.

### Segundo achado do smoke test

`docker-compose.yml` tem `container_name: financas-lab-postgres` e `financas-lab-redis` fixos. Tentar subir dois clones em paralelo dispara conflito de nome no Docker daemon. Sem impacto em fluxo normal (1 clone por vez). Registrado como débito em `hooks-pendentes.md`. Workaround manual: `docker rm -f <containers>` antes do segundo `setup.ps1`. Fix futuro estimado em 1-2h.

### Mudanças

- `scripts/setup.ps1`: bloco `core.hooksPath` movido de linhas 35-42 para entre linhas 18 e 20 (após `.env`, antes do Docker). Bloco em si inalterado.
- `docs/progresso.md`: lição categorizada como "prescrição insuficientemente específica" registrada na seção da Sub-etapa 4.0. Sub-etapa 4.0.1 adicionada à lista de "Sub-etapas concluídas" da Camada 3.
- `docs/hooks-pendentes.md`: débito Docker `container_name:` registrado.
- `docs/decisoes.md`: entrada no histórico.

### Validação

**Validação local 1: Docker rodando, fluxo normal.**

\```powershell
git config --unset core.hooksPath
pwsh -File scripts/setup.ps1
git config core.hooksPath  # → .githooks
\```

`setup.ps1` roda completo, configura `core.hooksPath`, conclui com sucesso.

**Validação local 2: Docker propositalmente em conflito (reproduz o bug original).**

\```powershell
docker run -d --name financas-lab-postgres postgres:16
docker run -d --name financas-lab-redis redis:7
git config --unset core.hooksPath
pwsh -File scripts/setup.ps1  # vai falhar no docker compose
git config core.hooksPath  # → .githooks (configurado ANTES da falha do Docker)
\```

`setup.ps1` falha no `docker compose up -d` (esperado, é o cenário do bug). **Antes** de falhar, configura `core.hooksPath`. Antes do fix, esta segunda validação retornaria vazio — provando o bug. Depois do fix, retorna `.githooks` — provando a correção.

### Validação destrutiva pós-merge sugerida

1. Limpar containers em conflito no Docker daemon (se houver):
   \```powershell
   docker rm -f financas-lab-postgres financas-lab-redis
   \```
2. Clone novo do repo em diretório temporário.
3. Rodar `.\scripts\setup.ps1` com Docker em estado normal — confirmar sucesso completo.
4. Limpar containers de novo, criar conflito manual (rodar containers do mesmo nome em background), rodar `setup.ps1` em segundo clone — confirmar que `core.hooksPath` é configurado mesmo com falha do Docker.

### Próximo passo

Sub-etapa 4.1 (primeira leva de hooks funcionais — Conventional Commits, encoding UTF-8, linhas em branco em Markdown). Decisão fora deste PR.
```

## Pós-criação do PR

1. Abrir PR via `gh pr create`.
2. Capturar o número.
3. Editar `docs/progresso.md`, `docs/hooks-pendentes.md`, `docs/decisoes.md` substituindo `PR #XX` por `PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico`.
5. Push.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `fix/etapa-4-0-1-setup-hookspath-position` empurrada com 4 commits (3 + 1 update do PR).
- PR aberto, CI verde, **não mergeado**.
- `main` ainda no squash da 4.0.
- Working tree limpo.
- `git config core.hooksPath` no projeto retorna `.githooks` (configurado pela 4.0; não muda nesta etapa).
- Reportar com `git log --oneline -4`, `git status`, `gh pr view --json number,state,statusCheckRollup` e parar.

## O que NÃO fazer ao terminar

- Não mergear o PR.
- Não criar prompt da Sub-etapa 4.1.
- Não corrigir o `container_name:` no `docker-compose.yml` — apenas registrar como débito.
- Não tocar em `.claude/`, `.githooks/`, `.gitignore`.
- Não tocar em `src/`, `pom.xml`, `application*.yml`, `frontend/`, migrations, ADRs.
- Não alterar o conteúdo (mensagens, lógica) do bloco `core.hooksPath` — apenas mover.
- Não criar "Lições da Sub-etapa 4.0.1" no `progresso.md` — adicionar à seção existente da 4.0.
- Não criar ADR novo.
- Não sugerir "próximo passo" espontaneamente.
- Não relaxar ADRs anteriores.
- Não criar repo separado de fábrica.
