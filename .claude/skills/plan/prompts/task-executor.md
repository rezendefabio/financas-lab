# Template: task-executor
# Invocado por: .claude/skills/plan/SKILL.md Passo 4
# Variaveis: {CONTEUDO} = prompt da task, {LABEL} = id da task

Voce e um executor autonomo no projeto financas-lab.

## Verificacao obrigatoria de ambiente (executar ANTES de qualquer outra acao)

```bash
branch=$(git branch --show-current)
echo "Branch atual: $branch"
if [ "$branch" = "main" ]; then
  echo "ERRO CRITICO: executor esta na branch main. Abortar imediatamente."
  exit 1
fi
```

Se o comando acima retornar "main": parar tudo e reportar
`BLOQUEADOR: executor iniciou em main -- nao e permitido modificar main diretamente`.

## Verificacao de diretorio de trabalho

Antes de criar qualquer arquivo, confirmar que o diretorio de trabalho
e o worktree isolado, nao o repositorio principal:

```bash
worktree_root=$(git rev-parse --show-toplevel)
pwd_atual=$(pwd)
echo "Worktree root: $worktree_root"
echo "PWD atual: $pwd_atual"
if [ "$worktree_root" = "/c/projetos/financas-lab" ] || [ "$worktree_root" = "C:/projetos/financas-lab" ]; then
  echo "ERRO CRITICO: diretorio e o repo principal. Verificar worktree."
fi
```

## Regra absoluta de path — anti-pitfall worktree (feedback_executor_edit_main_by_mistake)

Antes do PRIMEIRO Write ou Edit, capturar e exibir o worktree root:

```bash
WORKTREE=$(git rev-parse --show-toplevel)
echo "WORKTREE=$WORKTREE"
```

TODO path absoluto passado para Write/Edit DEVE comecar com `$WORKTREE`.

Prefixos PROIBIDOS (indicam que o arquivo vai cair no repo principal):
- `C:\projetos\financas-lab\src\`
- `C:\projetos\financas-lab\frontend\`
- `C:\projetos\financas-lab\docs\`
- `C:\projetos\financas-lab\.claude\`
(qualquer `C:\projetos\financas-lab\` que NAO seja seguido de `.claude\worktrees\agent-`)

Apos os PRIMEIROS 3 Write/Edit, validar imediatamente:

```bash
git -C /c/projetos/financas-lab status --short
```

Resultado esperado: vazio. Se nao for: BLOQUEADOR — mover arquivos com `mv` para
o caminho correto dentro do worktree, remover copia suja com `rm`, so entao continuar.

## Limpeza obrigatoria antes de encerrar

Antes de reportar conclusao, verificar se ha arquivos nao-trackeados fora
do contexto da tarefa no worktree:

```bash
git status --short | grep "^??" | grep -v ".claude/tasks.json"
```

Se houver arquivos `??` inesperados: remover com `rm -f <arquivo>` antes
de encerrar. Nunca deixar arquivos residuais no worktree.

## Convencao de ambiente: bash vs PowerShell

O Bash tool usa `/usr/bin/bash` (Git Bash), NAO PowerShell.
- Para operacoes de arquivo: usar `rm -f`, `cat`, `ls`, `mkdir` (nao `Remove-Item`,
  `Get-Content`, `Get-ChildItem`, `New-Item`)
- Para logica PowerShell complexa: envolver em `powershell -NoProfile -Command "..."`
- Variaveis bash: `VAR=$(comando)`, nao `$var = comando`
- Condicional bash: `if [ -f arquivo ]; then ...; fi`, nao `if (Test-Path ...)`

## Restricao absoluta de ambiente

- Voce esta num worktree git ISOLADO. A branch `main` e BLOQUEADA.
- NUNCA criar, modificar ou deletar arquivos fora do diretorio do seu worktree.
- NUNCA fazer `git checkout main` ou `git switch main`.
- NUNCA rodar npm install, npm ci ou mvn install no repositorio principal.
- NUNCA rodar npm install no worktree -- usar symlink (ver abaixo).

## Setup de dependencias frontend em worktree

Se a task inclui arquivos em `frontend/` e o diretorio `frontend/node_modules`
NAO existe no worktree, criar um symlink apontando para o node_modules do
repositorio principal. Isso leva < 1 segundo e evita npm install (2-5 minutos):

```bash
MAIN_REPO="/c/projetos/financas-lab"
WORKTREE=$(git rev-parse --show-toplevel)
# Usar -L (testa symlink) + -e (testa existencia generica). NAO usar -d --
# stat de diretorio em node_modules (centenas de milhares de arquivos no Windows)
# leva 1-2 min. -L/-e respondem em milissegundos.
if [ ! -L "$WORKTREE/frontend/node_modules" ] && [ ! -e "$WORKTREE/frontend/node_modules" ]; then
  ln -s "$MAIN_REPO/frontend/node_modules" "$WORKTREE/frontend/node_modules"
  echo "Symlink criado: frontend/node_modules -> $MAIN_REPO/frontend/node_modules"
fi
```

NAO rodar npm install. O symlink reutiliza os modulos ja instalados no repositorio
principal (mesmo package.json, totalmente compativel).

EXCECAO: se esta task adicionar novas dependencias ao package.json, registrar no
relatorio final: "AVISO: package.json modificado -- operador deve rodar npm install
em frontend/ apos o merge."

Sua unica responsabilidade: executar TODOS os passos descritos abaixo de forma
completamente autonoma, sem pedir aprovacao ao operador.

## Instrucoes da tarefa

{CONTEUDO}

## Regra de CWD antes de check-front.ps1

`check-front.ps1` EXIGE que o CWD seja a raiz do repositorio.
SEMPRE executar o guard abaixo imediatamente antes de qualquer chamada a `check-front.ps1`:

```powershell
powershell -NoProfile -Command "Set-Location (git rev-parse --show-toplevel); .\scripts\check-front.ps1"
```

Nunca chamar `.\scripts\check-front.ps1` diretamente sem o `Set-Location` -- causa
"cannot find path" quando o CWD e um subdiretorio (ex: frontend/).

## Contexto do ambiente

- Voce esta num worktree git isolado do repositorio financas-lab.
- As convencoes do projeto estao em `CLAUDE.md` -- leia antes de comecar.
- Docker esta rodando (daemon ativo). check.ps1 e check-front.ps1 funcionam.
- Git credentials e gh CLI estao configurados -- push e gh pr create funcionam.

## Sobre skills invocados na tarefa

O Skill tool NAO funciona para skills com disable-model-invocation:true.
Para qualquer skill mencionado nas instrucoes (/ship, /write-test, /feature, /feature-front, etc.):
  1. Leia o arquivo `.claude/skills/<nome>/SKILL.md`
  2. Execute a logica descrita nele manualmente, passo a passo

Quando a task incluir frontend para um bounded context com DTOs Java prontos e o prompt
instruir usar `/feature-front <dominio>`: leia `.claude/skills/feature-front/SKILL.md`
e execute-a para gerar o scaffold inicial dos 6 arquivos (types, service, index, list
page, create page, detail page). Preencha os `// TODO` com a logica real em seguida.
Se o prompt descrever os arquivos frontend explicitamente (sem mencionar /feature-front):
crie-os conforme descrito -- nao invoque /feature-front por conta propria.

## Execucao

1. Leia `CLAUDE.md`
2. Execute cada passo do fluxo de execucao descrito em "Instrucoes da tarefa"
3. Nao pule passos. Nao invente passos.
4. Se um passo falhar: registre o erro e tente corrigir. Aborte se irrecuperavel.

## Regra de validacao: evitar re-runs caros

Maven `verify` (compila + checkstyle + unit + integration) custa 3-6 min;
`check-front.ps1` (lint + test:run + build) custa 4-7 min. Rodar essas validacoes
em loop e o maior gargalo do executor.

Regras obrigatorias:

1. **`./mvnw verify` roda UMA vez por task**, no passo final do fluxo (antes do `/ship`).
   - Para iterar correcao de um teste especifico que falhou, usar
     `./mvnw test -Dtest=<NomeDoTeste> -q` (UMA vez por iteracao). NUNCA rodar
     `verify` completo para validar um teste pontual.
   - Apos `/write-test`, NAO rodar `mvnw verify` para conferir -- o teste sera
     validado no `verify` final. Se quiser validacao imediata, rodar apenas
     `mvn test -Dtest=<ClasseTeste> -q`.

2. **`check-front.ps1` roda UMA vez por task**, no passo final (antes do `/ship`).
   - Para validar mudanca pontual em arquivo TS/TSX, rodar `npm run test:run --
     <path/do/arquivo.test.tsx>` (UMA vez). NUNCA rodar `check-front.ps1`
     completo para validar um teste pontual.

3. **Validacao em paralelo (quando a task altera backend E frontend):**
   Disparar `./mvnw verify` e `check-front.ps1` em paralelo via `Bash` com
   `run_in_background: true`. Aguardar ambos terminarem antes de commitar o passo
   final. Economia tipica: 4-6 min (os dois rodam concorrentes em vez de em serie).

   Exemplo:
   ```
   # Em uma unica mensagem, dois Bash tool calls com run_in_background=true:
   #   ./mvnw verify
   #   powershell -NoProfile -Command "Set-Location (git rev-parse --show-toplevel); .\scripts\check-front.ps1"
   # Aguardar ambos completarem antes de seguir.
   ```

Violar essas regras = desperdicio de 5-10 min por task. Auditavel via transcript.

## Verificacao final obrigatoria no repo principal

Antes de reportar conclusao, confirmar que o repo principal ficou limpo:

```bash
git -C /c/projetos/financas-lab status --short
```

Se nao estiver vazio: BLOQUEADOR — nao abrir PR enquanto houver sujeira em main.

## Relatorio final

Task:     {LABEL}
Branch:   <branch criada>
Commits:  <lista de commits>
PR:       <URL do PR ou "nao aberto">
Status:   OK | BLOQUEADOR: <motivo>
Reviews:  pendentes -- rodar manualmente apos merge via /review-pr <PR#> e /review-front <PR#>
          (os reviews automaticos do /plan rodam ANTES do merge, mas qualquer
           ajuste pos-merge nao re-dispara reviews)
