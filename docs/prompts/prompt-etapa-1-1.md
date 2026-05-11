# Prompt — Etapa 1.1 da Camada 1

Você está trabalhando no projeto `financas-lab`, um SaaS de finanças pessoais que serve como laboratório para validar uma fábrica de desenvolvimento AI-native.

## Antes de qualquer ação, leia obrigatoriamente, em ordem:

1. `docs/visao.md` — propósito do projeto e escopo do MVP
2. `docs/decisoes.md` — stack, padrões e regras duras
3. `docs/adrs.md` — contexto histórico das decisões
4. `docs/progresso.md` — estado atual e próximas camadas
5. `docs/roadmap-camada-1.md` — plano detalhado da Camada 1
6. `docs/brief-etapa-1-1.md` — brief que originou esta tarefa (contexto adicional)

Estamos exatamente na **Etapa 1.1 da Camada 1**. Nenhuma outra etapa será executada nesta sessão. Não escreva código de feature, não crie estrutura Spring Boot, não configure Docker Compose — tudo isso é etapa posterior do roadmap.

## Tarefa

Execute os 6 itens abaixo, na ordem exata. Pergunte antes de cada commit. Mostre o conteúdo de cada arquivo proposto antes de criar.

### 1. Criar `.gitattributes` na raiz do repo

Conteúdo desejado:

- Default: `text=auto eol=lf` para todos os arquivos.
- `.bat` e `.cmd`: `text eol=crlf` (Windows exige).
- `.ps1`: `text eol=crlf` (PowerShell preferentemente CRLF).
- `.sh`: `text eol=lf`.
- Arquivos binários explícitos como binary: `*.png`, `*.jpg`, `*.jpeg`, `*.gif`, `*.ico`, `*.pdf`, `*.zip`, `*.jar`, `*.war`.
- Arquivos `*.java`, `*.xml`, `*.yml`, `*.yaml`, `*.json`, `*.md`, `*.sql`, `*.properties`: `text eol=lf` explícito.

### 2. Criar `.gitignore` na raiz do repo

Conteúdo desejado, cobrindo:

- **Java/Maven:** `target/`, `*.jar`, `*.war`, `*.class`, `hs_err_pid*`, `.mvn/wrapper/maven-wrapper.jar` (mas mantém os outros arquivos do `.mvn/wrapper`).
- **IntelliJ IDEA:** `.idea/`, `*.iml`, `*.iws`, `*.ipr`, `out/`.
- **VS Code:** `.vscode/` exceto `.vscode/settings.json`, `.vscode/tasks.json`, `.vscode/launch.json`, `.vscode/extensions.json` (essas devem ser versionadas se existirem).
- **Eclipse:** `.classpath`, `.project`, `.settings/`, `.factorypath`, `bin/`.
- **Node.js / Next.js (frontend futuro):** `node_modules/`, `.next/`, `out/`, `dist/`, `.turbo/`, `*.tsbuildinfo`.
- **Variáveis de ambiente:** `.env`, `.env.local`, `.env.*.local`. **Nunca** ignorar `.env.example`.
- **Logs:** `*.log`, `logs/`, `npm-debug.log*`, `yarn-debug.log*`.
- **Docker (volumes locais):** `data/`, `.docker-data/`.
- **Testcontainers (cache local):** `.testcontainers/`.
- **Sistema operacional:** `.DS_Store`, `Thumbs.db`, `desktop.ini`.

### 3. Criar `README.md` na raiz do repo

Curto (10-15 linhas no máximo). Estrutura:

- Título: `# financas-lab`
- 1 parágrafo dizendo o que é (cite que é projeto-laboratório para validar fábrica AI-native + SaaS de finanças pessoais como caso de uso).
- Seção "Documentação" listando os arquivos em `docs/` com 1 linha de descrição cada.
- Seção "Status" dizendo "Camada 1 (Infraestrutura de confiança) — em andamento. Ver `docs/progresso.md`."
- **NÃO** incluir badges, comandos de instalação, screenshots, "como contribuir". Nada disso ainda.

### 4. Criar `CLAUDE.md` na raiz do repo

**Mínimo, ~15 linhas.** Função: orientar instâncias futuras de Claude Code que abrirem este projeto.

Estrutura sugerida:

```
# financas-lab

Projeto-laboratório para validar fábrica AI-native. Backend Java/Spring Boot, frontend Next.js PWA, Postgres.

## Antes de qualquer ação, leia em ordem:
- docs/visao.md — propósito e escopo
- docs/decisoes.md — stack, padrões e regras duras
- docs/adrs.md — contexto histórico das decisões
- docs/progresso.md — estado atual da fábrica e próximas camadas
- docs/roadmap-camada-1.md — plano detalhado se estivermos na Camada 1

## Camada atual

Camada 1 — Infraestrutura de confiança. NÃO escrever código de feature. Apenas configurações, esqueletos e infraestrutura de validação. Features só começam na Camada 2.

## Regras duras

- Toda mudança via PR. Sem push direto em main.
- Conventional Commits obrigatórios.
- Antes de propor PR, rode .\scripts\check.ps1 (a ser criado nas etapas seguintes do roadmap).
```

### 5. Verificar que os 7 documentos estão em `docs/`

- `docs/visao.md`
- `docs/adrs.md`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/roadmap-camada-1.md`
- `docs/brief-etapa-1-1.md`
- `docs/prompt-etapa-1-1.md` (este arquivo)

Se algum estiver em outro lugar, mova para `docs/`. Se algum estiver faltando, **pare e reporte** — não invente conteúdo.

### 6. Commit inicial

Antes de commitar, mostre o `git status` e me peça confirmação. Se eu confirmar:

- `git add .gitattributes` (primeiro, sozinho — para line endings serem aplicados ao resto)
- `git commit -m "chore: define line endings via gitattributes"`
- `git add .`
- `git commit -m "chore: initial repo structure with docs and CLAUDE.md"`
- `git push origin main`

Se push der erro de branch protection, me avise — não force.

## Restrições importantes

- **Não criar pasta `src/`.** Spring Boot inicializa isso na etapa 1.4.
- **Não criar `pom.xml`.** Idem.
- **Não criar `docker-compose.yml`.** Etapa 1.3.
- **Não criar scripts `.ps1`.** Etapa 2.6.
- **Não inventar conteúdo para os documentos `.md` em `docs/`.** Se faltarem, reporte.
- **Não modificar `settings.json` do Claude Code** ou nenhum arquivo fora deste repo.
- **Pergunte antes de cada commit.**
- **Use modo plan quando achar que vai criar mais de 1 arquivo numa tacada.**

Comece lendo os documentos em `docs/` na ordem indicada e me apresente um resumo de 5-7 linhas do que entendeu da tarefa antes de propor o primeiro arquivo.
