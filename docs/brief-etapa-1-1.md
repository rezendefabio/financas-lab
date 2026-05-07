# Brief de Execução — Etapa 1.1 da Camada 1

> Este documento é o **prompt** que você vai passar para o Claude Code dentro do diretório `C:\projetos\financas-lab` para executar a primeira etapa concreta da fábrica.
>
> **Como usar:**
> 1. Coloque os 6 documentos finais (`visao.md`, `adrs.md`, `decisoes.md`, `progresso.md`, `roadmap-camada-1.md`, este `brief-etapa-1-1.md`) dentro da pasta `C:\projetos\financas-lab\docs\` localmente (ainda sem commit, vamos commitar via Claude Code).
> 2. Abra terminal em `C:\projetos\financas-lab`.
> 3. Rode `claude` para iniciar Claude Code.
> 4. Cole o prompt da seção [Prompt a passar](#prompt-a-passar) abaixo.
> 5. Acompanhe a execução. Use modo plan se quiser revisar antes de cada criação de arquivo.
>
> **Esta é uma execução em Tier 3 (controle manual).** Você revisa cada arquivo criado antes de aceitar. Apesar de ser tarefa de baixo risco, é a primeira delegação — vale calibrar confiança vendo o que ele faz.

---

## Por que delegar isto

Esta etapa cria 4 arquivos de configuração de repositório (`.gitignore`, `.gitattributes`, `README.md`, `CLAUDE.md`), reorganiza documentos para `docs/`, e faz o commit inicial. **Risco baixíssimo, valor alto:**

- Te força a usar Claude Code para criar artefatos do projeto desde o turno zero.
- Você vê concretamente como o agente interpreta os documentos fundadores que produzimos.
- Permite calibrar confiança: o que ele acerta de primeira, o que precisa de correção, o que ignorou.
- Configura corretamente line endings antes de qualquer outro arquivo entrar — evita refactor de CRLF/LF depois.

---

## Pré-requisitos antes de começar

Confirme que:

- [ ] Repo `financas-lab` existe no GitHub privado
- [ ] Clonado em `C:\projetos\financas-lab`
- [ ] Os 6 documentos `.md` (visão, ADRs, decisões, progresso, roadmap, este brief) estão em `C:\projetos\financas-lab\docs\`
- [ ] `git status` dentro do repo mostra apenas os 6 documentos `.md` em `docs/` como untracked
- [ ] Claude Code reiniciado após alteração do `settings.json` no turno anterior
- [ ] Terminal aberto em `C:\projetos\financas-lab`

---

## Prompt a passar

Copie o bloco abaixo (entre os `---`) e cole no Claude Code:

---

Você está trabalhando no projeto `financas-lab`, um SaaS de finanças pessoais que serve como laboratório para validar uma fábrica de desenvolvimento AI-native.

**Antes de qualquer ação, leia obrigatoriamente, em ordem:**

1. `docs/visao.md` — propósito do projeto e escopo do MVP
2. `docs/decisoes.md` — stack, padrões e regras duras
3. `docs/adrs.md` — contexto histórico das decisões
4. `docs/progresso.md` — estado atual e próximas camadas
5. `docs/roadmap-camada-1.md` — plano detalhado da Camada 1 (que vamos começar)
6. `docs/brief-etapa-1-1.md` — este brief, com a tarefa específica

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
- **Testcontainers (cache local):** `~/.testcontainers.properties` é per-user, não vai no repo, mas adicione `.testcontainers/` por garantia.
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

Estrutura:

```markdown
# financas-lab

Projeto-laboratório para validar fábrica AI-native. Backend Java/Spring Boot, frontend Next.js PWA, Postgres.

## Antes de qualquer ação, leia em ordem:
- docs/visao.md — propósito e escopo
- docs/decisoes.md — stack, padrões e regras duras
- docs/adrs.md — contexto histórico das decisões
- docs/progresso.md — estado atual da fábrica e próximas camadas
- docs/roadmap-camada-1.md — plano detalhado se estivermos na Camada 1

## Camada atual

Camada 1 — Infraestrutura de confiança. **NÃO escrever código de feature.** Apenas configurações, esqueletos e infraestrutura de validação. Features só começam na Camada 2.

## Regras duras

- Toda mudança via PR. Sem push direto em main.
- Conventional Commits obrigatórios.
- Antes de propor PR, rode `.\scripts\check.ps1` (a ser criado nas etapas seguintes do roadmap).
```

### 5. Verificar que os 6 documentos estão em `docs/`

- `docs/visao.md`
- `docs/adrs.md`
- `docs/decisoes.md`
- `docs/progresso.md`
- `docs/roadmap-camada-1.md`
- `docs/brief-etapa-1-1.md` (este arquivo)

Se algum estiver em outro lugar, mova para `docs/`. Se algum estiver faltando, **pare e reporte** — não invente conteúdo.

### 6. Commit inicial

Antes de commitar, mostre o `git status` e me peça confirmação. Se eu confirmar:

- `git add .gitattributes` (primeiro, sozinho — para line endings serem aplicados ao resto)
- `git commit -m "chore: define line endings via gitattributes"`
- `git add .`
- `git commit -m "chore: initial repo structure with docs and CLAUDE.md"`
- `git push origin main`

Se push der erro de branch protection (esperado se a etapa 1.2 já foi feita), me avise — não force.

---

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

---

## O que esperar do Claude Code

Após colar o prompt acima, fluxo esperado:

1. Claude Code lê os 6 documentos em sequência (você verá várias ações `Read` no log).
2. Apresenta resumo do que entendeu.
3. Propõe o conteúdo do `.gitattributes` e pergunta se pode criar.
4. Você revisa e aprova (ou ajusta).
5. Repete para `.gitignore`, `README.md`, `CLAUDE.md`.
6. Verifica que docs estão no lugar.
7. Mostra `git status`, pede confirmação.
8. Faz os commits e push.

**Tempo esperado:** 15-30 minutos com revisão atenta sua. Se passar de 1 hora, algo travou — para e me chama.

---

## Calibração: o que observar nesta primeira delegação

Esta é a primeira tarefa real delegada para Claude Code dentro da fábrica. Use ela para calibrar:

**Bom sinal:**
- Ele leu os documentos antes de propor algo
- O `.gitignore` cobre Java + Node + IDE como pedido
- O `CLAUDE.md` está dentro do tamanho proposto (~15 linhas)
- Pediu confirmação antes de cada commit
- Não inventou conteúdo nem expandiu escopo

**Sinal de calibração necessária:**
- Ignorou um dos arquivos da lista de leitura
- Adicionou seções "úteis" não pedidas no README ou CLAUDE.md (ex: badges, instalação, screenshots)
- Tentou criar `pom.xml` ou `docker-compose.yml` "já que estava configurando o projeto"
- Fez commit sem pedir confirmação
- Inventou conteúdo

**Se acontecer sinal de calibração:**
- Para a sessão
- Anota o que aconteceu em uma seção "Lições da Camada 1" no `progresso.md`
- Refina o prompt para a próxima etapa
- Eventualmente vira hook de validação na Camada 3

A fábrica se constrói tanto pelo que funciona quanto pelo que precisa ser corrigido. Não é falha — é dado.

---

## Após esta etapa

Quando a etapa 1.1 estiver concluída e merged em `main`:

1. Marque os checkboxes da etapa 1.1 em `docs/progresso.md` (pode ser feito em PR separado pelo próprio Claude Code).
2. Volte à conversa com o Claude (este chat) e me avise:
   - "Etapa 1.1 concluída"
   - Quaisquer observações sobre o comportamento do Claude Code (acertos, ajustes feitos, surpresas)
3. Te entrego o brief da etapa 1.2 (branch protection) ou seguimos direto para a 1.3 (Docker Compose), conforme calibração observada.

A partir da etapa 1.3, briefs vão ficando mais densos e tarefas mais não-triviais. A 1.1 é proposital simples — é teste de calibração.
