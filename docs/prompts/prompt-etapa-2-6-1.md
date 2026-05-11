# Prompt — Etapa 2.6.1: Fix de exit code em scripts PowerShell

## Contexto

A Etapa 2.6 foi concluída e fechada via PR #23. `main` está em `b57395b`.

Durante validação manual feita pelo operador no PowerShell, descobriu-se que **`$LASTEXITCODE` não reflete o exit do script quando rodado por dot-source/chamada direta** em casos onde `Write-Error` é usado. Diagnóstico confirmado:

- Subprocess (`powershell.exe -File scripts/ship.ps1`) retorna exit 1 corretamente quando há erro.
- Sessão atual (`.\scripts\ship.ps1` direto no terminal) deixa `$LASTEXITCODE = 0` e `$? = True` mesmo após o erro ser logado em vermelho.

Causa: combinação de `$ErrorActionPreference = "Stop"` no topo do script com `Write-Error` no caminho de erro. `Write-Error` sob `Stop` lança exceção terminating, encerra o script imediatamente, e nunca chega ao `exit 1` na linha seguinte. PowerShell em modo dot-source não atualiza `$LASTEXITCODE` nesse caso.

**Correção:** trocar `Write-Error "msg"` por `Write-Host "msg" -ForegroundColor Red` antes de cada `exit 1` nos 5 scripts afetados. `Write-Host` não dispara `Stop`, o `exit 1` na linha seguinte executa, e o exit code propaga corretamente em qualquer modo de invocação. Bonus: a mensagem fica limpa, sem dump de stack trace do PowerShell.

Esta etapa é uma **correção pequena, escopo cirúrgico**. Diferente das etapas anteriores, não vai mexer em arquitetura, configuração ou estrutura. Apenas substituições mecânicas + documentação.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra `b57395b feat: etapa 2.6 — scripts PowerShell para comandos atômicos do projeto (#23)`
- `docs/prompt-etapa-2-6-1.md` presente como untracked (este próprio arquivo, anexado pelo operador antes de iniciar). Se não aparecer em `git status`, parar e reportar.
- Working tree sem outras mudanças

Validar com `git status` e `git log --oneline -1` antes de começar.

## Tarefas

### Tarefa 1 — Substituições mecânicas

Trocar **todas** as ocorrências de `Write-Error "<mensagem>"` por `Write-Host "<mensagem>" -ForegroundColor Red` nos 5 scripts abaixo. Manter a mensagem **idêntica**. Manter o `exit N` que vem na linha seguinte **inalterado**.

Mapa exato esperado:

**`scripts/setup.ps1` — 2 substituições:**
- `Write-Error "Falha ao subir Docker Compose. Docker Desktop esta rodando?"` → `Write-Host "Falha ao subir Docker Compose. Docker Desktop esta rodando?" -ForegroundColor Red`
- `Write-Error "Falha no mvnw clean install."` → `Write-Host "Falha no mvnw clean install." -ForegroundColor Red`

**`scripts/dev.ps1` — 2 substituições:**
- `Write-Error "Docker Desktop nao esta rodando. Inicie o Docker e tente novamente."` → versão `Write-Host ... -ForegroundColor Red`
- `Write-Error "Falha ao subir Docker Compose."` → idem

**`scripts/test-integration.ps1` — 1 substituição:**
- `Write-Error "Docker Desktop nao esta rodando. Testcontainers precisa do Docker."` → versão `Write-Host ... -ForegroundColor Red`

**`scripts/check.ps1` — 1 substituição:**
- `Write-Error "Docker Desktop nao esta rodando. Testcontainers precisa do Docker."` → versão `Write-Host ... -ForegroundColor Red`

**`scripts/ship.ps1` — 4 substituições:**
- `Write-Error "Working tree nao esta limpo. Commit ou descarte mudancas antes de ship."` → versão `Write-Host ... -ForegroundColor Red`
- `Write-Error "Voce esta em '$branch'. Ship deve rodar a partir de uma feature branch."` → versão `Write-Host ... -ForegroundColor Red` (atenção: a mensagem usa interpolação de variável, manter `$branch` literal na string)
- `Write-Error "check.ps1 falhou. Push cancelado."` → idem
- `Write-Error "git push falhou."` → idem

**Total: 10 substituições em 5 arquivos.**

**Não alterar:**
- Mensagens (texto entre aspas)
- Linhas de `exit N`
- `$ErrorActionPreference = "Stop"` no topo (mantém pra outros tipos de erro fora de fluxo de validação manual)
- Mensagens com `Write-Error` que não sejam seguidas de `exit N` — não vejo nenhuma, mas se aparecer, **não tocar e reportar**

### Tarefa 2 — Validação destrutiva

Validar que ao menos 2 dos scripts agora propagam `$LASTEXITCODE = 1` corretamente quando rodados em sessão direta. Esta validação **não vai pro commit** — é confirmação de que o fix funcionou.

**Atenção:** o `bash_tool` do agente é bash, não PowerShell. Pra rodar `.ps1` e capturar `$LASTEXITCODE` da sessão, precisa invocar via `powershell.exe -Command`. Rodar `.ps1` direto via subprocess (`powershell.exe -File`) **mascara** o bug — porque subprocess sempre traduz exceção terminating em exit 1. A validação tem que ser via dot-source/invocação direta dentro de um único `powershell.exe -Command`.

Exemplo de validação correta para `ship.ps1` (working tree sujo):

```bash
# Cria estado destrutivo via bash
echo "teste destrutivo" > teste-ship.txt

# Roda script via powershell -Command (sessão única) e captura $LASTEXITCODE
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "
    .\scripts\ship.ps1
    Write-Host \"LASTEXITCODE = \$LASTEXITCODE\"
    exit \$LASTEXITCODE
"
echo "Bash exit: $?"

# Limpa
rm teste-ship.txt
```

Esperado:
- Output do script com mensagem em vermelho (sem stack trace do PowerShell)
- `LASTEXITCODE = 1`
- `Bash exit: 1`

Repetir o mesmo procedimento para `check.ps1` ou `dev.ps1` com Docker simuladamente parado — mas como o agente não pode parar o Docker do operador, **basta validar `ship.ps1` (working tree sujo) e `ship.ps1` (branch == main)**, que não dependem de Docker. Esses dois cobrem o padrão da correção em 4 dos 5 arquivos (todos exceto `setup.ps1`).

### Tarefa 3 — Atualizar `decisoes.md`

**3a.** Após a seção "Convenções operacionais" e antes de "Política de débito técnico consciente", adicionar nova subseção:

```markdown
### Scripts PowerShell

- **`Write-Host -ForegroundColor Red` em vez de `Write-Error` antes de `exit N`.** Sob `$ErrorActionPreference = "Stop"`, `Write-Error` lança exceção terminating, encerra o script antes do `exit N`, e em sessão dot-source o `$LASTEXITCODE` não é atualizado (fica 0 falsamente). Padrão correto:
  ```powershell
  if ($alguma_condicao_de_erro) {
      Write-Host "mensagem clara do erro" -ForegroundColor Red
      exit 1
  }
  ```
- **`Write-Error` é apropriado** apenas em scripts/módulos onde quem invoca vai capturar o erro como exceção (try/catch, pipeline com `-ErrorAction`). Em scripts user-facing chamados diretamente no terminal, prefira `Write-Host` colorido.
- **Manter `$ErrorActionPreference = "Stop"`** no topo dos scripts. A regra acima é apenas para o fluxo "validação detectou problema, sair com código de erro". Para erros inesperados de comandos nativos (ex: Maven crashar), `Stop` continua sendo o comportamento desejado.
```

**3b.** Adicionar linha no **"Histórico de mudanças"** no fim do arquivo, no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.6.1 concluída: fix de bug encontrado em validação manual da 2.6 — `Write-Error` + `exit 1` sob `Stop` não propagava `$LASTEXITCODE = 1` em sessão direta. Substituído por `Write-Host -ForegroundColor Red` + `exit 1` nos 5 scripts. Regra formalizada na seção "Scripts PowerShell".
```

### Tarefa 4 — Atualizar `progresso.md`

**4a.** Atualizar campo "Última atualização" no topo: `2026-05-08 (Etapa 2.6.1)`.

**4b.** Adicionar nova seção **"Lições da Etapa 2.6.1"** logo antes de **"Lições da Etapa 2.6"** (mantendo ordem decrescente):

```markdown
## Lições da Etapa 2.6.1

### Candidatos a hook (automatizar em etapas futuras)

1. Detectar `Write-Error` seguido de `exit N` em arquivos `.ps1` — combinação que indica bug do mesmo padrão que esta etapa corrigiu. Hook leve: `grep -B0 -A1 "Write-Error" scripts/*.ps1 | grep -A1 "exit"`.

### Lições de ambiente

1. **Validação manual destrutiva pega bugs que validação automática mascara.** A 2.6 passou sintaxe (parser), encoding (sem BOM) e CI (porque CI invoca via subprocess, que traduz exceção terminating em exit 1 corretamente). Mas em uso interativo no PowerShell o `$LASTEXITCODE` ficava 0 falsamente. Conclusão: validação manual no fluxo real do operador descobriu bug que toda automação validou como verde. Validar destrutivamente é não-negociável.
2. **`$ErrorActionPreference = "Stop"` + `Write-Error` + `exit N` é armadilha clássica em PowerShell.** O `Stop` faz `Write-Error` virar exceção terminating, abortando o script antes do `exit N`. Em sessão direta, `$LASTEXITCODE` permanece com o valor do último comando externo que rodou (geralmente 0). Em subprocess (`powershell.exe -File`), o exit traduz pra 1 corretamente. Comportamento inconsistente. Padrão correto registrado em `decisoes.md`.
3. **Subprocess test mascara bug de exit code em PowerShell.** Para validar exit code real de scripts `.ps1`, usar `powershell.exe -Command` rodando o script + captura de `$LASTEXITCODE` na **mesma sessão**. Subprocess via `-File` reporta exit code do processo (que sempre é 1 quando há exceção), não do comportamento da sessão.
```

**4c.** Adicionar entrada no **"Histórico de mudanças deste documento"** no topo da lista:

```markdown
- **2026-05-08** — Etapa 2.6.1 concluída: fix de exit code em scripts `.ps1`. `Write-Error` + `exit 1` substituído por `Write-Host -ForegroundColor Red` + `exit 1` nos 5 scripts afetados. Regra formalizada em `decisoes.md`. Lições registradas. Mergeado via PR #XX.
```

### Tarefa 5 — Versionar este próprio prompt

Confirmar que `docs/prompt-etapa-2-6-1.md` está em disco como untracked e incluir no commit de docs.

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Arquivos permitidos:
   - `scripts/setup.ps1` (substituições)
   - `scripts/dev.ps1` (substituições)
   - `scripts/test-integration.ps1` (substituições)
   - `scripts/check.ps1` (substituições)
   - `scripts/ship.ps1` (substituições)
   - `docs/decisoes.md`
   - `docs/progresso.md`
   - `docs/prompt-etapa-2-6-1.md` (este arquivo)

2. **Não tocar em `test.ps1`.** Esse script já usa o padrão correto (`$exit = $LASTEXITCODE; exit $exit`) e não tem `Write-Error`. Confirmar via `grep "Write-Error" scripts/test.ps1` que não retorna nada — se retornar, parar e reportar.

3. **Não tocar em `pom.xml`, `ci.yml`, `README.md`, `application.yml`, ou qualquer outro arquivo.** Esta etapa é cirúrgica. Tentação previsível: "vou aproveitar e melhorar X" — recusar.

4. **Não criar wrappers, helpers, funções compartilhadas.** Cada script mantém sua estrutura atual. Apenas trocar `Write-Error` por `Write-Host -ForegroundColor Red`.

5. **Não mudar lógica de exit, ordem de comandos, mensagens de erro.** Substituição é apenas do método de impressão. Tudo o mais permanece literal.

6. **Não tocar em `$ErrorActionPreference = "Stop"`.** Mantém. A regra adicionada em `decisoes.md` documenta por quê.

7. **Encoding obrigatório UTF-8 sem BOM.** Validar após edição com `file scripts/*.ps1` ou `xxd scripts/<nome>.ps1 | head -1` confirmando primeiros bytes (`23 20` ou similar, não `EF BB BF`). Se a tool de edição usada introduzir BOM, recriar o arquivo correto.

8. **Validação destrutiva via `powershell.exe -Command` com sessão única.** Não validar via subprocess `-File` (mascara o bug que esta etapa corrige).

9. **`bash_tool` do Claude Code é bash, não PowerShell.** Comandos de validação do agente devem ser sintaxe POSIX/bash. Pra invocar PowerShell, usar `powershell.exe -Command ...`.

10. **Não antecipar Etapa 2.7 (Next.js).** Não criar `frontend/`. Não rodar `create-next-app`.

## Estrutura de commits

Branch: `fix/scripts-exit-code`

Commits atômicos, em ordem:

**Commit 1** — `fix: usa Write-Host vermelho em vez de Write-Error em scripts .ps1`
- `scripts/setup.ps1`
- `scripts/dev.ps1`
- `scripts/test-integration.ps1`
- `scripts/check.ps1`
- `scripts/ship.ps1`

**Commit 2** — `docs: registra etapa 2.6.1 (fix exit code) em decisoes e progresso`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-2-6-1.md`

## Validação antes de abrir PR

```bash
# Confirmar que NENHUM Write-Error sobrou nos 5 scripts:
grep -n "Write-Error" scripts/setup.ps1 scripts/dev.ps1 scripts/test-integration.ps1 scripts/check.ps1 scripts/ship.ps1
# Esperado: nenhuma linha retornada

# Confirmar que test.ps1 continua sem Write-Error:
grep -n "Write-Error" scripts/test.ps1
# Esperado: nenhuma linha retornada

# Encoding limpo:
file scripts/*.ps1

# Working tree esperado:
git status
git log --oneline -3
```

Validação destrutiva (Tarefa 2):

```bash
# Validação 1: ship.ps1 com working tree sujo, dot-source captura LASTEXITCODE = 1
echo "teste" > teste-ship.txt
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "
    .\scripts\ship.ps1 2>&1 | Out-Null
    exit \$LASTEXITCODE
"
echo "Bash exit: $?"
rm teste-ship.txt
# Esperado: Bash exit: 1
```

```bash
# Validação 2: ship.ps1 em main, dot-source captura LASTEXITCODE = 1
# (precisa estar na branch fix/scripts-exit-code, então essa validação passa
#  se rodada antes do checkout. Se já estiver na branch, pular esta validação
#  e reportar.)
```

**Se uma das validações destrutivas mascarar o bug** (LASTEXITCODE = 0 mesmo com erro), parar e reportar — o fix não funcionou.

## PR

Título: `fix: etapa 2.6.1 — exit code correto em scripts PowerShell`

Body sugerido:

```markdown
## Summary

Fix de bug encontrado em validação manual da Etapa 2.6: `Write-Error "msg"; exit 1` sob `$ErrorActionPreference = "Stop"` não propagava `$LASTEXITCODE = 1` em sessão dot-source. `Write-Error` sob `Stop` lança exceção terminating, encerra o script antes do `exit 1`, e em sessão direta o `$LASTEXITCODE` permanece com valor anterior (geralmente 0).

### Mudanças

- 10 substituições mecânicas em 5 scripts: `Write-Error "msg"` → `Write-Host "msg" -ForegroundColor Red`. `exit N` na linha seguinte mantido.
- `setup.ps1` (2), `dev.ps1` (2), `test-integration.ps1` (1), `check.ps1` (1), `ship.ps1` (4).
- `test.ps1` não tocado (já usa padrão correto).
- `decisoes.md`: nova subseção "Scripts PowerShell" formalizando a regra.
- `progresso.md`: lições da etapa, incluindo o padrão a evitar.

### Validação destrutiva

Antes do fix:
- `.\scripts\ship.ps1` com working tree sujo → `$LASTEXITCODE = 0` ❌
- Subprocess `powershell.exe -File ship.ps1` → exit 1 ✓ (mascarava o bug)

Depois do fix:
- `.\scripts\ship.ps1` com working tree sujo → `$LASTEXITCODE = 1` ✓
- Mensagem em vermelho, sem stack trace do PowerShell

### Por que isso importa

Bug em gate de proteção. Se confiar no exit code de `ship.ps1` em hook ou script chamador (via dot-source), o gate não funciona. Subprocess sempre traduzia corretamente, então CI nunca pegou. Diagnóstico veio de validação manual do operador no PowerShell, exatamente o tipo de validação que automação mascara.

### Próximo passo

Etapa 2.7 (inicializar Next.js) — fora do escopo deste PR.
```

## Pós-criação do PR

1. Abrir o PR via `gh pr create`.
2. Capturar o número.
3. Editar `docs/progresso.md` substituindo `Mergeado via PR #XX` por `Mergeado via PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico do progresso.md`
5. Push.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- `main` local sincronizada com `origin/main`, incluindo squash commit da 2.6.1
- `git status` limpo
- 5 scripts em `scripts/` com `Write-Host -ForegroundColor Red` em vez de `Write-Error`
- `decisoes.md` com seção "Scripts PowerShell"
- `progresso.md` reflete 2.6.1 concluída, número real do PR no histórico
- Branch `fix/scripts-exit-code` deletada local e remotamente

Reportar ao operador o estado final com `git log --oneline -3` e `git status`. Parar.

## O que NÃO fazer ao terminar

- Não criar prompt da 2.7
- Não criar `frontend/`, não rodar `create-next-app`
- Não tocar em outros arquivos "enquanto está aqui"
- Não sugerir "próximo passo" espontaneamente
