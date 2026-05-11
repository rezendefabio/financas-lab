# Prompt — Fechamento da Etapa 2.1

## Contexto

A Etapa 2.1 (Testcontainers funcional) foi mergeada via PR #17 em `main` (commit `11b3f65`). Falta o fechamento formal da etapa: ignorar o `.claude/` (settings locais pessoais que não devem ser versionados) e atualizar `docs/progresso.md` marcando 2.1 como concluída e registrando lições.

Este prompt cobre essas duas mudanças num único PR de fechamento.

## Estado esperado ao iniciar

- Branch atual: `main`, sincronizada com `origin/main`
- Working tree limpo exceto por `.claude/` untracked
- `git log --oneline -1` mostra `11b3f65 feat: testcontainers para testes de integracao com postgres real (#17)`

Validar com `git status` antes de começar. Se estado divergir, parar e reportar.

## Tarefas

### Tarefa 1 — Adicionar `.claude/` ao `.gitignore`

Editar `.gitignore` adicionando uma seção nova **antes** da seção "Sistema operacional" (que deve permanecer como última seção), seguindo o padrão visual existente (cabeçalho com `─── Nome ───`, regra simples).

Conteúdo a adicionar:

```
# ─── Claude Code (settings locais pessoais) ─────────────────────────────────
.claude/
```

Justificativa (não vai no commit, só pra contexto): `.claude/settings.local.json` contém permissões pessoais da máquina do desenvolvedor (tools pré-aprovadas individualmente). Por convenção da Anthropic, `settings.local.json` é local e não deve ser versionado. Quando houver `settings.json` (sem `.local`), `agents/`, `commands/` ou `hooks/`, esses devem ser versionados — adicionar exceções (`!.claude/settings.json` etc.) em etapa futura quando começarem a existir.

Após editar, validar:

```powershell
Get-Content .gitignore -Encoding UTF8
git status
```

Esperado: `.claude/` não aparece mais como untracked em `git status`.

### Tarefa 2 — Atualizar `docs/progresso.md`

Mudanças exatas a aplicar:

**2a.** Atualizar campo "Última atualização" no topo do documento para a data corrente.

**2b.** Na seção "Status geral", tabela de camadas, **não há mudança** — Camada 1 segue 🔵 Próxima até a etapa 2.8 fechar a camada inteira. Confirmar que a tabela continua igual.

**2c.** Na seção "Camada 1 — Infraestrutura de confiança", subseção "Critérios de 'pronto'", marcar como `[x]`:

- `Testcontainers configurado e funcional`

(Os demais critérios da Camada 1 seguem como estão — alguns já marcados de etapas anteriores, outros pendentes.)

**2d.** Adicionar nova seção "Lições da Etapa 2.1" **logo antes** da seção "Lições da Etapa 1.5" (mantendo ordem decrescente por etapa, que é o padrão observado no documento).

Conteúdo exato da nova seção:

```markdown
## Lições da Etapa 2.1

### Candidatos a hook (automatizar em etapas futuras)

1. Validar que classes base de teste em `src/test/java/.../shared/` (ou pacote equivalente) que servem de superclasse para outras classes de teste tenham modificador `abstract`. Sem `abstract`, JUnit tenta instanciar a classe base e roda o lifecycle dela além das filhas, gerando execução duplicada e potencial inicialização de container fora de hora.
2. Validar que `application-test.yml` (e qualquer profile de teste) não declare URL JDBC hardcoded quando o projeto usa Testcontainers via `@DynamicPropertySource`. Hardcoding anula o ponto da injeção dinâmica e faz o teste rodar contra banco que não é o do container.
3. Validar que `baseline-on-migrate: true` apareça **apenas** em profiles de teste (e potencialmente `dev`), nunca em `application.yml` (defaults) ou `application-prod.yml`. Em produção, baseline silencioso de schema desconhecido é fonte clássica de inconsistência.

### Lições de ambiente

1. Cold-start de CI com Testcontainers em runner do GitHub Actions (`ubuntu-latest`) foi de 33s no PR #17, bem abaixo dos 80-120s previstos no plano da etapa. O cache de camada do `postgres:16-alpine` no runner é mais agressivo que o estimado. Calibrar previsões futuras para 30-90s, com 120s como teto pessimista — não como expectativa.
2. `spring.jpa.hibernate.ddl-auto: validate` com schema vazio (sem migrations ainda) só funciona porque também não existem classes `@Entity` no projeto. Quando as primeiras entidades JPA aparecerem na Camada 2, validar contra schema vazio vai quebrar a inicialização do Spring. Exige migration `V1` não-vazia ou migrations escritas em paralelo às entidades — não depois. Já cobre a Etapa 2.2 (primeira migration Flyway).
```

**2e.** Atualizar a seção "Histórico de mudanças deste documento" no final do documento adicionando uma nova linha **no topo** da lista (mais recente primeiro):

```
- **2026-05-08** — Etapa 2.1 concluída: Testcontainers configurado, AbstractIntegrationTest criado, FinancasApplicationTests passa contra Postgres real via container, débito técnico da Etapa 1.5 (exclusão do FinancasApplicationTests no CI) resolvido. Mergeado via PR #17.
```

(Ajustar a data se diferente da data real de execução deste prompt.)

### Tarefa 3 — Versionar este próprio prompt

Mover/copiar este arquivo para `docs/prompt-etapa-2-1-fechamento.md` (se ainda não está lá) para que entre no commit junto com as outras mudanças, seguindo o padrão dos prompts anteriores.

## Restrições e freios

1. **Não tocar em nenhum arquivo fora de:** `.gitignore`, `docs/progresso.md`, `docs/prompt-etapa-2-1-fechamento.md`. Se identificar necessidade de outra mudança, parar e reportar — não executar por conta própria.

2. **Não atualizar `decisoes.md`.** A Etapa 2.1 não introduziu decisão estrutural nova; apenas implementou o que já estava em ADR-007. Tocar `decisoes.md` aqui é fora do escopo.

3. **Não inventar lições não observadas.** As 3 candidatas a hook e as 2 lições de ambiente acima foram explicitamente confirmadas como observadas pelo operador. Não adicionar mais. Não embelezar. Não inflacionar.

4. **Não antecipar Etapa 2.2.** Após PR mergeado e progresso atualizado, parar. Não sugerir próximo passo, não criar prompt da 2.2, não tocar em nada relacionado a Flyway. Fim de etapa = parada explícita.

5. **Validar conteúdo bruto antes de commitar.** Após editar cada arquivo, rodar `Get-Content <arquivo> -Encoding UTF8` (PowerShell sem `-Encoding UTF8` lê UTF-8 errado) e revisar. Não confiar em preview de tool.

## Estrutura de commits

Branch: `chore/fechamento-etapa-2-1`

Commits atômicos, em ordem:

**Commit 1** — `chore: ignora .claude/ (settings locais do Claude Code)`
- `.gitignore`

**Commit 2** — `docs: marca etapa 2.1 concluida e registra licoes`
- `docs/progresso.md`
- `docs/prompt-etapa-2-1-fechamento.md`

## Validação antes de abrir PR

```powershell
git status                                    # working tree limpo
git log --oneline -3                          # 2 commits novos visíveis
Get-Content .gitignore -Encoding UTF8 | Select-String "claude"   # regra presente
Get-Content docs/progresso.md -Encoding UTF8 | Select-String "Etapa 2.1"   # seção nova presente
```

## PR

Título: `chore: fechamento da etapa 2.1 (gitignore + progresso)`

Body (sugestão, ajustar se fizer sentido):

```markdown
## Summary

Fechamento administrativo da Etapa 2.1 após merge do PR #17 (Testcontainers funcional).

- Adiciona `.claude/` ao `.gitignore` — evita versionar `settings.local.json` (permissões pessoais da máquina do dev). Convenção: `settings.local.json` é local; `settings.json`, `agents/`, `commands/`, `hooks/` (quando existirem) serão versionados via exceções explícitas.
- Atualiza `docs/progresso.md`: marca critério "Testcontainers configurado e funcional" como concluído, adiciona seção "Lições da Etapa 2.1" (3 candidatos a hook + 2 lições de ambiente, todos observados durante a etapa), atualiza histórico de mudanças.
- Versiona o próprio prompt de fechamento em `docs/prompt-etapa-2-1-fechamento.md` seguindo o padrão dos prompts anteriores.

## Validação

- `git status` limpo após mudanças
- CI deve passar trivialmente (mudanças apenas em arquivos não-Java/não-config-de-build)

## Próximo passo

Etapa 2.2 (primeira migration Flyway) — não escopo deste PR.
```

Após PR aberto e CI verde: mergear via squash, deletar branch, atualizar `main` local. Sem antecipar 2.2.

## Estado esperado ao terminar

- `main` local sincronizada com `origin/main`, incluindo squash commit do fechamento da 2.1
- `.claude/` não aparece em `git status`
- `docs/progresso.md` reflete 2.1 concluída
- Branch `chore/fechamento-etapa-2-1` deletada local e remotamente
- Working tree limpo

Reportar ao operador o estado final com `git log --oneline -3` e `git status`. Parar.