# Prompt — Etapa 3.3.1: Fix do `dev.ps1` para ativar profile `dev`

## Contexto

A Etapa 3.3 foi concluída e fechada via PR #31. `main` está em `5fa9f8e`. Working tree limpo.

Durante validação destrutiva manual pós-merge da 3.3 (subir aplicação local com `dev.ps1` apontando pro Postgres real), apareceu falha de inicialização do Spring:

```
APPLICATION FAILED TO START
Failed to configure a DataSource: 'url' attribute is not specified
No active profile set, falling back to 1 default profile: "default"
```

**Causa raiz:** `scripts/dev.ps1` invoca `.\mvnw spring-boot:run` sem ativar profile. Spring cai no profile `default`, que herda apenas `application.yml`. O `application.yml` (defaults) **não tem** `spring.datasource.url` — datasource é definido em `application-dev.yml` (que existe e está correto). Resultado: sem URL, sem driver, falha.

`decisoes.md` já prescreve a regra:

> **Profiles** sempre explícitos: `dev`, `test`, `prod`. Nada de "sem profile".

Mas a regra estava em prosa, não aplicada no script. Bug clássico de "código vivo > prosa" — a configuração existe (`application-dev.yml` completo e correto), o problema é que o invocador não ativa.

Esta etapa é fix cirúrgico do `dev.ps1` + uma verificação defensiva sobre profile em testes de integração + registro de débito relacionado descoberto durante diagnóstico.

## Escopo decidido (calibrado com operador antes da redação)

### Inclui

1. **Adicionar `-Dspring-boot.run.profiles=dev` ao `mvnw spring-boot:run` no `dev.ps1`.** Fix mínimo, uma linha.
2. **Verificação (sem alteração) de que `AbstractIntegrationTest` ativa profile `test`.** Se não ativar, o testes de integração estão funcionando "por coincidência" (datasource vem via `@DynamicPropertySource` do Testcontainers, mas qualquer config futura dependente de profile quebraria silenciosamente). Se ativar, segue normal. Decisão sobre alterar fica condicionada ao que o agente encontrar — **se o `@ActiveProfiles("test")` não estiver presente, parar e reportar antes de adicionar**, porque é mudança que vai além do fix do `dev.ps1`.
3. **Atualizar `decisoes.md`** registrando regra mais forte sobre ativação de profile em scripts.
4. **Registrar em `hooks-pendentes.md`** dois itens descobertos durante diagnóstico:
   - Hook futuro: validar que scripts que invocam `mvnw spring-boot:run` passam `-Dspring-boot.run.profiles`.
   - Débito: `application-prod.yml` não existe no projeto, contrariando a prescrição "Profiles sempre explícitos: dev, test, prod" do `decisoes.md`.
5. **Registrar lição em `progresso.md`** sobre o padrão "validação destrutiva real descobrindo bug que CI não pega" — terceira ocorrência consecutiva (2.6.1, 2.6.2, 2.8 também descobriram nesse padrão).

### Não inclui

- Criar `application-prod.yml`. Sem deploy prod ainda — criar agora seria especulação. Fica como débito explícito em `hooks-pendentes.md` para resolver quando deploy prod entrar no escopo.
- Alterar `setup.ps1`. Compilação (`mvnw clean install -DskipTests`) não precisa de profile. Sem bug aqui.
- Alterar `test.ps1` ou `test-integration.ps1`. Spring Boot Test ativa profile via `@ActiveProfiles` ou config Spring, não via flag de Maven. **Mas verificar `AbstractIntegrationTest` está no escopo desta etapa** (Tarefa 4).
- Alterar `application.yml` para definir `spring.profiles.default: dev`. Abordagem alternativa que mascara o problema em vez de resolver — agente seguiria sem perceber que esqueceu profile, e qualquer cenário sem default explícito (CI, deploy futuro) quebraria. Manter regra "profile sempre explícito" é mais saudável.
- Alterar `application-dev.yml`. Está correto.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- `git log --oneline -1` mostra `5fa9f8e feat: etapa 3.3 — bounded context conta (infraestrutura) (#31)`
- `docs/prompt-etapa-3-3-1.md` presente como untracked (este próprio arquivo)
- Working tree limpo
- `scripts/dev.ps1` existe e tem `.\mvnw spring-boot:run` sem flag de profile

Validar com:

```bash
git status
git log --oneline -1
ls docs/prompt-etapa-3-3-1.md
grep -n "spring-boot:run" scripts/dev.ps1
```

Esperado: linha do `mvnw spring-boot:run` aparecer no `dev.ps1` sem `-Dspring-boot.run.profiles`.

## Tarefas

### Tarefa 1 — Validar pré-requisitos

```bash
git status
git log --oneline -3
ls docs/prompt-etapa-3-3-1.md
grep -n "spring-boot:run" scripts/dev.ps1
ls src/main/resources/application*.yml
```

Esperado:
- Working tree limpo, exceto `docs/prompt-etapa-3-3-1.md` untracked
- `dev.ps1` invoca `mvnw spring-boot:run` (sem flag de profile)
- `application.yml`, `application-dev.yml`, `application-test.yml` presentes
- `application-prod.yml` **ausente** (confirmará débito a registrar)

Se algum item divergir, parar e reportar.

### Tarefa 2 — Criar branch de trabalho

```bash
git checkout -b fix/dev-script-profile
```

### Tarefa 3 — Ajustar `scripts/dev.ps1`

Localizar a última linha não-vazia do script:

```powershell
.\mvnw spring-boot:run
```

Substituir por:

```powershell
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

**Notas críticas:**

- **Aspas em volta da flag.** Sem aspas, PowerShell pode interpretar `-D` como parâmetro próprio em algumas versões. Aspas garantem passagem literal pra `mvnw`. Padrão consolidado em outros pontos do projeto.
- **Manter encoding UTF-8 sem BOM.** Validar com `xxd scripts/dev.ps1 | head -1` após edição.
- **Não alterar nenhuma outra linha do script.** O bloco de criação automática do `.env` (Etapa 2.9) e a checagem de Docker (Etapa 2.6.2) ficam intactos.

### Tarefa 4 — Verificar `AbstractIntegrationTest`

```bash
ls src/test/java/com/laboratorio/financas/shared/
cat src/test/java/com/laboratorio/financas/shared/AbstractIntegrationTest.java 2>/dev/null
```

(Caminho exato pode ser ligeiramente diferente — buscar com `find`/`grep` se necessário.)

**Inspecionar:**

- A classe tem `@ActiveProfiles("test")`?

**Se SIM:** ok, reportar e seguir. Não alterar.

**Se NÃO:** **parar e reportar ao operador.** Adicionar `@ActiveProfiles("test")` é mudança que vai além do fix do `dev.ps1` e merece decisão consciente:
- Por que estava funcionando até agora? (Hipótese: `@DynamicPropertySource` injeta tudo que precisa, e nenhuma config até hoje dependeu de profile.)
- Adicionar quebra algum teste atual? (Improvável mas possível.)
- Vale fazer aqui ou em etapa separada?

Não tomar decisão silenciosa. Reportar e aguardar.

### Tarefa 5 — Validar fix localmente

**Validação automática (sintaxe):**

```bash
xxd scripts/dev.ps1 | head -1
powershell.exe -NoProfile -Command "[void][System.Management.Automation.Language.Parser]::ParseFile('scripts\dev.ps1', [ref]\$null, [ref]\$null); 'OK'"
```

**Validação destrutiva real (do operador, não do agente):**

A validação real do fix exige Docker rodando + executar `dev.ps1` + verificar que app sobe + ver banner Spring + GET `/api/healthcheck` retorna 200. Operador faz isso pós-merge. Documentar no PR body que validação destrutiva é responsabilidade do operador.

**O agente não tenta rodar `dev.ps1`** porque (a) bloqueia terminal, (b) ambiente do agente é Linux sem Docker em geral, (c) padrão do projeto: validação destrutiva manual é do operador.

### Tarefa 6 — Atualizar `docs/decisoes.md`

**6a.** Localizar a seção "Spring específico" no `decisoes.md`. A regra atual:

> **Profiles** sempre explícitos: `dev`, `test`, `prod`. Nada de "sem profile".

Manter essa regra. **Adicionar logo abaixo** uma regra mais forte sobre aplicação em scripts:

```markdown
- **Scripts que invocam `mvnw spring-boot:run` sempre passam `-Dspring-boot.run.profiles=<profile>` explicitamente.** Sem isso, Spring cai no profile `default` (que tem apenas `application.yml`, sem datasource), levando a `Failed to configure a DataSource`. Bug consolidado na Etapa 3.3.1, descoberto em validação destrutiva manual da 3.3. Hook futuro vai validar que `dev.ps1` (e equivalentes) passam a flag.
```

**6b.** Adicionar entrada no histórico:

```markdown
- **2026-05-09** — Etapa 3.3.1 concluída: fix do `dev.ps1` para ativar profile `dev` via `-Dspring-boot.run.profiles=dev`. Bug descoberto em validação destrutiva manual pós-merge da 3.3. Mergeado via PR #XX.
```

### Tarefa 7 — Atualizar `docs/hooks-pendentes.md`

Localizar seção "Hooks de setup / ambiente" (criada na 2.8 / atualizada na 2.9).

**7a.** Adicionar item:

```markdown
- **Validar que scripts que invocam `mvnw spring-boot:run` passam `-Dspring-boot.run.profiles=<profile>` explicitamente.** (Etapa 3.3.1) Sem a flag, Spring usa profile `default` que não tem datasource → `Failed to configure a DataSource`. Hook leve: `grep -nE "mvnw\s+spring-boot:run" scripts/*.ps1 | grep -v "spring-boot.run.profiles"` deve retornar zero linhas.
```

**7b.** Criar nova seção (se não existir) "Débitos de configuração" — ou adicionar em seção apropriada:

```markdown
## Débitos de configuração

- **`application-prod.yml` não existe.** (Descoberto na 3.3.1) `decisoes.md` prescreve "Profiles sempre explícitos: dev, test, prod", mas o arquivo de prod nunca foi criado. Não bloqueante hoje (sem deploy prod), mas precisa ser criado quando deploy prod entrar no escopo. Resolver junto com a etapa de deploy.
```

Se já houver seção "Débitos de configuração" ou similar, adicionar lá em vez de criar nova.

### Tarefa 8 — Atualizar `docs/progresso.md`

**8a.** Atualizar campo "Última atualização": `2026-05-09 (Etapa 3.3.1 — fix profile dev)`.

**8b.** Adicionar nova seção **"Lições da Etapa 3.3.1"** logo antes de **"Lições da Etapa 3.3"** (ordem decrescente):

```markdown
## Lições da Etapa 3.3.1

### Candidatos a hook (automatizar em etapas futuras)

(Preencher com lições reais — ou `(Nenhum novo nesta etapa.)` se nada surgir.)

### Lições de ambiente

(Preencher com lições reais — ou `(Nenhum novo nesta etapa.)` se nada surgir.)
```

**Lição esperada (mas registrar só se realmente observada durante a execução):**

> **Validação destrutiva manual em ambiente real é instrumento de qualidade de primeira linha.** Bug do `dev.ps1` não ativando profile `dev` passou pelo CI verde de toda a Camada 2 porque CI usa profile `test` via `@DynamicPropertySource`. Só apareceu quando o operador tentou subir aplicação local de fato pós-merge da 3.3 — exatamente o tipo de cenário que CI não cobre. Quarta ocorrência consecutiva do mesmo padrão (2.6.1, 2.6.2, 2.8, 3.3.1).

Adaptar redação. Categorizar em "Lições de ambiente" ou criar categoria específica se fizer sentido.

**8c.** Adicionar entrada no histórico:

```markdown
- **2026-05-09** — Etapa 3.3.1 concluída: fix do `dev.ps1` para ativar profile `dev` (`-Dspring-boot.run.profiles=dev`). Bug descoberto em validação destrutiva manual pós-merge da 3.3. Débito de `application-prod.yml` ausente registrado em `hooks-pendentes.md`. Mergeado via PR #XX.
```

### Tarefa 9 — Versionar este próprio prompt

Confirmar que `docs/prompt-etapa-3-3-1.md` está em disco como untracked e incluir no commit de docs.

### Tarefa 10 — Validação final antes de commitar

```bash
xxd scripts/dev.ps1 | head -1
powershell.exe -NoProfile -Command "[void][System.Management.Automation.Language.Parser]::ParseFile('scripts\dev.ps1', [ref]\$null, [ref]\$null); 'OK'"
git status
git diff scripts/dev.ps1
```

Confirmar:
- Encoding sem BOM
- Sintaxe PowerShell válida
- Diff do `dev.ps1` mostra apenas a linha do `mvnw spring-boot:run` modificada
- Outros arquivos modificados são apenas docs

## Restrições e freios

1. **Não tocar em arquivos fora do escopo.** Permitidos:
   - `scripts/dev.ps1` (edição cirúrgica)
   - `docs/decisoes.md`
   - `docs/progresso.md`
   - `docs/hooks-pendentes.md`
   - `docs/prompt-etapa-3-3-1.md` (este arquivo, versionar)

2. **Não criar `application-prod.yml`.** Débito registrado em `hooks-pendentes.md`, resolvido em etapa futura quando deploy prod entrar.

3. **Não tocar em `application.yml` adicionando `spring.profiles.default: dev`.** Mascararia o problema. Manter regra "profile sempre explícito".

4. **Não tocar em `application-dev.yml`.** Está correto.

5. **Não tocar em `setup.ps1`, `test.ps1`, `test-integration.ps1`, `check.ps1`, `ship.ps1`.** Bug é exclusivo do `dev.ps1`.

6. **Não tocar em `pom.xml`, `docker-compose.yml`, código Java.**

7. **Sobre `AbstractIntegrationTest`**: apenas inspecionar. Se estiver sem `@ActiveProfiles("test")`, **parar e reportar**, não tomar decisão silenciosa.

8. **Não criar testes automatizados em PowerShell.** Validação destrutiva é do operador, pós-merge.

9. **Encoding UTF-8 sem BOM** no `dev.ps1` modificado. Validar com `xxd`.

10. **Manter `Write-Host -ForegroundColor`, não `Write-Error`.** Padrão consolidado.

11. **Manter `$ErrorActionPreference = "Stop"`** e o bloco de suspensão local em torno de `docker info`. Não tocar.

12. **Lições da Etapa 3.3.1 só registram observações reais.** Não inventar.

13. **Não antecipar Etapa 3.4.** Sem rascunhar próximas etapas.

14. **Não tomar decisão silenciosa em zona limítrofe.** Padrão consolidado. Se aparecer dúvida, parar e reportar.

## Estrutura de commits

Branch: `fix/dev-script-profile`

Commits atômicos, em ordem:

**Commit 1** — `fix(scripts): ativa profile dev em dev.ps1 via -Dspring-boot.run.profiles`
- `scripts/dev.ps1`

**Commit 2** — `docs: registra etapa 3.3.1 (fix profile dev) em decisoes, hooks-pendentes e progresso`
- `docs/decisoes.md`
- `docs/hooks-pendentes.md`
- `docs/progresso.md`
- `docs/prompt-etapa-3-3-1.md`

## Validação antes de abrir PR

```bash
xxd scripts/dev.ps1 | head -1
powershell.exe -NoProfile -Command "[void][System.Management.Automation.Language.Parser]::ParseFile('scripts\dev.ps1', [ref]\$null, [ref]\$null); 'OK'"
git status
git log --oneline -3
```

## PR

Título: `fix: etapa 3.3.1 — dev.ps1 ativa profile dev via -Dspring-boot.run.profiles`

Body sugerido:

```markdown
## Summary

Implementa a Etapa 3.3.1 do roadmap: fix cirúrgico do `dev.ps1` que invocava `mvnw spring-boot:run` sem ativar profile, fazendo Spring cair no `default` que não tem datasource. Bug descoberto em validação destrutiva manual pós-merge da 3.3 (PR #31).

### Causa raiz

`dev.ps1` chamava `.\mvnw spring-boot:run` sem flag. Spring caía em profile `default`, que herda apenas `application.yml` (sem datasource). `application-dev.yml` (correto) nunca era ativado. Resultado: `Failed to configure a DataSource: 'url' attribute is not specified`.

CI nunca pegou porque CI roda profile `test` via `@DynamicPropertySource` do Testcontainers — caminho independente do problema.

### Mudanças

- `scripts/dev.ps1`: linha final `.\mvnw spring-boot:run` substituída por `.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"`.
- `docs/decisoes.md`: regra explícita sobre ativação de profile em scripts adicionada na seção "Spring específico" + entrada no histórico.
- `docs/hooks-pendentes.md`: hook futuro de validação registrado + débito de `application-prod.yml` ausente registrado em "Débitos de configuração".
- `docs/progresso.md`: lições da etapa registradas + entrada no histórico.

### Decisões de escopo

- **Não criar `application-prod.yml` agora.** Sem deploy prod, criar seria especulação. Débito registrado em `hooks-pendentes.md` para resolver junto com etapa de deploy.
- **Não definir `spring.profiles.default: dev` em `application.yml`.** Mascararia o bug em vez de resolver. Manter regra "profile sempre explícito".
- **`AbstractIntegrationTest` verificado:** <descrever decisão tomada na Tarefa 4 — "tem `@ActiveProfiles("test")`, ok" ou "não tem, escalado e seguindo conforme orientação do operador">.

### Validação

- Encoding UTF-8 sem BOM em `dev.ps1`: confirmado via `xxd`.
- Sintaxe PowerShell válida: confirmado via `[Parser]::ParseFile`.
- **Validação destrutiva real (operador, pós-merge):**
  - `docker compose down -v && .\scripts\setup.ps1` → containers up + build OK
  - `.\scripts\dev.ps1` → Spring sobe sem erro, banner aparece, profile `dev` ativo
  - `curl http://localhost:8080/api/healthcheck` → 200 com `{"status": "ok", ...}`

### Próximo passo

Etapa 3.4 (application + interfaces de `conta` — use cases, DTOs, `@RestController`) — fora do escopo deste PR.
```

## Pós-criação do PR

1. Abrir o PR via `gh pr create`.
2. Capturar o número.
3. Editar `docs/decisoes.md`, `docs/progresso.md` substituindo `Mergeado via PR #XX` por `Mergeado via PR #<numero-real>`.
4. Commit: `docs: atualiza numero do PR no historico`
5. Push.
6. Esperar CI verde.
7. **Aguardar autorização explícita do operador antes do merge.**

## Estado esperado ao terminar

- Branch `fix/dev-script-profile` empurrada com 3 commits (2 + 1 update do PR)
- PR aberto, CI verde, **não mergeado**
- `main` ainda em `5fa9f8e`
- Working tree limpo
- `dev.ps1` modificado, encoding sem BOM, sintaxe válida
- `decisoes.md`, `progresso.md`, `hooks-pendentes.md` atualizados, com PR número real registrado
- Prompt versionado em `docs/prompt-etapa-3-3-1.md`

Reportar com `git log --oneline -3`, `git status`, `gh pr view --json number,state,statusCheckRollup` e parar.

## O que NÃO fazer ao terminar

- Não mergear o PR. Aguardar autorização explícita.
- Não criar prompt da próxima etapa.
- Não rascunhar Etapa 3.4.
- Não criar `application-prod.yml`.
- Não tocar em outros scripts além do `dev.ps1`.
- Não tocar em código Java, `pom.xml`, `application*.yml`, `docker-compose.yml`.
- Não sugerir "próximo passo" espontaneamente.
