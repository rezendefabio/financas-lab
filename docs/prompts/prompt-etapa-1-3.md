# Prompt — Etapa 1.3 da Camada 1

Você está trabalhando no projeto `financas-lab`. Estamos na **Etapa 1.3 da Camada 1** — configurar Docker Compose com PostgreSQL e Redis.

## Antes de qualquer ação, leia em ordem:

1. `docs/visao.md` — propósito do projeto
2. `docs/decisoes.md` — stack, padrões e regras duras (especialmente seção "Stack" e "Convenções operacionais")
3. `docs/adrs.md` — ADR-003 (PostgreSQL em dev e prod) e ADR-005 (Redis para JWT blacklist)
4. `docs/progresso.md` — estado atual
5. `docs/roadmap-camada-1.md` — Etapa 1.3 detalhada

Após ler, apresente um resumo de 5-7 linhas do que entendeu da tarefa antes de propor qualquer arquivo.

## Tarefa

Criar a infraestrutura de banco e cache para desenvolvimento local. Um único PR (não dividir Postgres e Redis em PRs separados).

### Branch

Criar a branch `feat/docker-compose` a partir de `main` atualizada.

### Arquivos a criar

#### 1. `docker-compose.yml` na raiz do repo

Requisitos não-negociáveis:

- **Não declarar `version:`** — campo obsoleto desde Compose v2; declarar gera warning.
- 2 serviços: `postgres` e `redis`.
- Imagens fixadas em versão major.minor (não `latest`):
  - `postgres:16-alpine`
  - `redis:7-alpine`
- Ambos com `restart: unless-stopped`.
- Ambos com healthcheck funcional:
  - Postgres: `pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}`, intervalo 10s, timeout 5s, retries 5, start_period 10s.
  - Redis: `redis-cli -a ${REDIS_PASSWORD} ping`, intervalo 10s, timeout 5s, retries 5, start_period 10s.
- Volumes nomeados (não bind mounts) para persistência:
  - `postgres_data` montado em `/var/lib/postgresql/data`
  - `redis_data` montado em `/data`
- Portas expostas em `localhost` apenas, não em `0.0.0.0`:
  - Postgres: `127.0.0.1:5432:5432`
  - Redis: `127.0.0.1:6379:6379`
- Variáveis lidas via `${VAR_NAME}` referenciando arquivo `.env`:
  - `POSTGRES_USER`
  - `POSTGRES_PASSWORD`
  - `POSTGRES_DB`
  - `REDIS_PASSWORD` (Redis com `--requirepass ${REDIS_PASSWORD}` no `command`)
- Network: deixar default (Compose cria automaticamente).
- Container names explícitos: `financas-lab-postgres` e `financas-lab-redis`.

Não incluir:

- pgAdmin, RedisInsight ou outras ferramentas de admin (não pedido, expansão de escopo).
- Variáveis de aplicação Java (entram quando Spring Boot for inicializado na 1.4).
- Configurações de logging customizado.
- Dockerfile ou imagens de aplicação (Spring Boot rodará no host, não em container).

#### 2. `.env.example` na raiz do repo

Arquivo versionado documentando as variáveis necessárias com valores placeholders seguros (não credenciais reais):

```
# PostgreSQL - usado pelo docker-compose.yml e (futuro) Spring Boot
POSTGRES_USER=financas
POSTGRES_PASSWORD=changeme_local_only
POSTGRES_DB=financas_dev

# Redis - usado pelo docker-compose.yml e (futuro) Spring Boot
REDIS_PASSWORD=changeme_local_only
```

Comentário no topo explicando que é template e que cada dev deve copiar para `.env` local.

#### 3. `.env` na raiz do repo (NÃO versionado)

Cópia do `.env.example` com mesmos valores placeholder (dev local, sem dados sensíveis).

**Confirmar antes de criar que `.gitignore` já contém `.env`.** Se não contiver, parar e reportar — não atualizar `.gitignore` sem aviso.

### Validações obrigatórias antes de commitar

Execute, na ordem:

1. `docker compose config` — valida sintaxe; deve retornar configuração resolvida sem erro nem warning.
2. `docker compose up -d` — sobe os 2 serviços em background.
3. Aguardar 30 segundos. Em seguida `docker compose ps` — ambos serviços devem aparecer com STATUS contendo `(healthy)`.
4. `docker compose exec postgres pg_isready -U financas -d financas_dev` — deve retornar `accepting connections`.
5. `docker compose exec redis redis-cli -a changeme_local_only ping` — deve retornar `PONG`. Se gerar warning sobre uso de `-a` em linha de comando, é esperado e aceitável.
6. `docker compose down` — derruba sem erro.
7. `docker compose down -v` — limpa volume (validar que não tem nada hardcoded em volume).
8. `git status` — deve mostrar apenas `docker-compose.yml` e `.env.example` como arquivos novos. `.env` **não** deve aparecer.

Se qualquer validação falhar, **pare, reporte o erro completo e não tente consertar sem instrução.** Validação é gate — falha exige retorno ao usuário.

### Commit e PR

Após todas as validações passarem:

1. Mostre `git status` e peça confirmação.
2. Após confirmação:
   - `git add docker-compose.yml .env.example`
   - `git commit -m "feat: docker compose com postgres 16 e redis 7"`
3. Push para a branch.
4. Abrir PR via `gh` CLI:
   - Title: `feat: docker compose com postgres 16 e redis 7`
   - Body com 3 seções:
     - **Summary** — 3 bullets do que foi adicionado
     - **Validações executadas** — lista dos 8 checks com status de cada (✅/❌)
     - **Notas para revisão** — alertar que `.env.example` precisa ser copiado para `.env` local em primeira instalação

## Restrições importantes

- **Não criar pasta `src/`.** Spring Boot ainda não foi inicializado.
- **Não criar `pom.xml`.** Etapa 1.4.
- **Não criar scripts `.ps1`.** Etapa 2.6.
- **Não modificar nenhum arquivo em `docs/`** nesta etapa.
- **Não inventar serviços adicionais** ao docker-compose.
- **Não usar `version:`** no docker-compose.yml.
- **Não usar imagens `latest`.**
- **Não criar `Dockerfile`.**
- **Pergunte antes de cada commit.**
- **Não force push.**

## Observações de ambiente

- Sistema: Windows nativo, PowerShell, Docker Desktop.
- Disponível: Docker 29.0.1, Compose v2.40.3.
- Branch protection ativa em `main` — push direto bloqueado, exige PR.
- `gh` CLI instalado e autenticado.
- Working tree no início: clean (com `.claude/` untracked, esperado).
