---
name: setup-infra
description: Configura infraestrutura do projeto: CI/CD (GitHub Actions), Docker Compose, banco de dados, hooks de qualidade. Recebe descricao do projeto como argumento. Gera arquivos de infra.
disable-model-invocation: true
argument-hint: "<descricao-do-projeto>"
---

Voce deve configurar a infraestrutura do projeto descrito no argumento. Execute todos os passos em ordem.

## Input

Argumento recebido: descricao do projeto (ex: "sistema de gestao financeira pessoal com contas, categorias e lancamentos").

## Passo 0 -- Ler contexto existente

Leia `docs/architecture.md` se existir -- as decisoes de arquitetura influenciam escolhas de infra:

```powershell
Test-Path docs/architecture.md
```

Se existir: leia o arquivo e extraia linguagem, framework e banco de dados escolhidos.

Verifique tambem se `docker-compose.yml` e `.github/workflows/ci.yml` ja existem:

```powershell
Test-Path docker-compose.yml
Test-Path .github/workflows/ci.yml
```

## Passo 1 -- Propor infraestrutura

Com base no dominio e nas decisoes de arquitetura, produza uma proposta de infra cobrindo:

1. **CI/CD (GitHub Actions)**: jobs propostos
   - `lint`: verificacao de estilo/formatacao
   - `test`: testes unitarios e de integracao
   - `build`: compilacao/empacotamento
   - `deploy` (opcional): se o projeto tiver deploy automatico

2. **Docker Compose**: servicos propostos
   - Banco de dados (ex: postgres:16, mysql:8, mongodb:7)
   - Cache (ex: redis:7) -- apenas se arquitetura indica necessidade
   - Outros servicos de suporte

3. **Hooks de qualidade**: validacoes recomendadas para pre-commit
   - Conventional Commits (sempre recomendado)
   - Encoding UTF-8 (sempre recomendado)
   - Lint de Markdown (se projeto tem documentacao)
   - Verificacao de tamanho de arquivos de docs (se projeto tem documentacao extensiva)
   - Hook de stack especifico (ex: @Entity sem migration para Java/Spring)

4. **Variaveis de ambiente necessarias**: lista com nome, descricao e se e obrigatoria

## Passo 2 -- Exibir proposta ao operador

Exibir ao operador a proposta formatada:

```
/setup-infra: proposta para "<descricao>"

CI/CD (GitHub Actions):
  Jobs: <lista de jobs>

Docker Compose:
  Servicos: <lista de servicos com versoes>

Hooks de qualidade recomendados:
  - <hook 1>: <descricao>
  - <hook 2>: <descricao>
  ...

Variaveis de ambiente:
  <VAR_NAME>  <descricao>  [obrigatoria/opcional]
  ...
```

Usar AskUserQuestion com a pergunta "Aprovar esta infraestrutura e gerar os arquivos?"
Opcoes: "Sim, aprovar" e "Nao, cancelar".

Se cancelar: exibir "Infra cancelada. Nenhum arquivo gerado." e terminar.

## Passo 3 -- Gerar arquivos aprovados

### 3.1 -- Criar diretorio .github/workflows/ se necessario

```powershell
if (-not (Test-Path .github/workflows)) {
    New-Item -ItemType Directory -Path .github/workflows -Force
}
```

### 3.2 -- Gerar .github/workflows/ci.yml

Gerar o arquivo com os jobs aprovados. Exemplo para Java + Maven:

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Lint (Checkstyle)
        run: ./mvnw checkstyle:check

  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: testdb
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v4
      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run tests
        run: ./mvnw verify

  build:
    runs-on: ubuntu-latest
    needs: [lint, test]
    steps:
      - uses: actions/checkout@v4
      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Build
        run: ./mvnw package -DskipTests
```

Adaptar o conteudo conforme linguagem e framework escolhidos na arquitetura.

### 3.3 -- Gerar docker-compose.yml (se nao existir)

Se `docker-compose.yml` ja existir: pular este passo (nao sobrescrever).

Se nao existir: gerar com os servicos aprovados. Exemplo para PostgreSQL:

```yaml
services:
  postgres:
    image: postgres:16
    container_name: <nome-do-projeto>-postgres
    environment:
      POSTGRES_DB: <nome-do-projeto>
      POSTGRES_USER: <nome-do-projeto>
      POSTGRES_PASSWORD: <nome-do-projeto>
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

Adaptar conforme banco de dados escolhido.

### 3.4 -- Verificar arquivos gerados

```powershell
Test-Path .github/workflows/ci.yml
```

Se nao existir: reportar erro.

## Passo 4 -- Relatorio de hooks recomendados

Gerar relatorio textual dos hooks recomendados (nao implementa os hooks -- apenas documenta):

```
Hooks de qualidade recomendados para <descricao>:

[IMPLEMENTAR via sub-etapa dedicada]

  1. conventional-commits (commit-msg, modo fail)
     Valida mensagem de commit contra Conventional Commits.

  2. encoding-utf8 (pre-commit, modo fail)
     Rejeita arquivos com encoding diferente de UTF-8.

  3. markdown-blank-lines (pre-commit, modo fail)
     Exige linha em branco antes e depois de headers Markdown.

  ... (adaptar conforme proposta aprovada)

Referencia: docs/hooks-pendentes.md para backlog de hooks.
```

## Passo 5 -- Relatorio final

Exibir:

```
/setup-infra concluido.

Arquivos gerados:
  .github/workflows/ci.yml   -- GitHub Actions com jobs: <jobs>
  docker-compose.yml         -- <status: gerado | ja existia, mantido>

Hooks documentados: <N> hooks recomendados (nao implementados -- ver relatorio acima)

Variaveis de ambiente necessarias:
  <lista>

Proximo passo: revisar arquivos gerados, commitar e invocar /ship
```
