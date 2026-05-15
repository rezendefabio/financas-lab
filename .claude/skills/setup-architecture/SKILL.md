---
name: setup-architecture
description: Define a arquitetura do projeto: linguagem, framework, estilo (DDD/Clean/Hexagonal/CRUD), banco de dados, estrategia de testes. Recebe descricao do projeto como argumento. Gera docs/architecture.md.
disable-model-invocation: true
argument-hint: "<descricao-do-projeto>"
---

Voce deve definir a arquitetura do projeto descrito no argumento. Execute todos os passos em ordem.

## Input

Argumento recebido: descricao do projeto (ex: "sistema de gestao financeira pessoal com contas, categorias e lancamentos").

## Passo 0 -- Ler contexto existente

Verifique se `docs/architecture.md` ja existe:

```powershell
Test-Path docs/architecture.md
```

Se existir: leia o conteudo e considere as decisoes ja tomadas antes de propor alteracoes.

Verifique tambem se `CLAUDE.md` existe e leia-o para entender convencoes do projeto.

## Passo 1 -- Analisar dominio e propor arquitetura

Com base na descricao do projeto, analise o dominio e produza uma proposta de arquitetura cobrindo:

1. **Linguagem + versao**: escolha com justificativa (ex: Java 21 LTS -- ecossistema maduro, suporte a virtual threads)
2. **Framework principal**: escolha com justificativa (ex: Spring Boot 3.x -- autoconfiguracao, ecossistema)
3. **Estilo arquitetural**: uma das opcoes abaixo com justificativa:
   - **DDD com Clean Architecture**: ideal para dominios ricos com regras de negocio complexas
   - **Hexagonal (Ports & Adapters)**: ideal para isolamento de portas de entrada/saida
   - **CRUD simples**: ideal para CRUD direto sem regras de negocio complexas
   - **Microservicos**: ideal para times grandes e dominios independentes
4. **Banco de dados**: escolha com justificativa (ex: PostgreSQL -- ACID, maturidade, JSON support)
5. **Estrategia de testes**: niveis e ferramentas (ex: JUnit 5 + Mockito para unit; Testcontainers para integration; MockMvc para E2E)
6. **Estrutura de pacotes recomendada**: listagem dos pacotes principais

## Passo 2 -- Exibir proposta ao operador

Exibir ao operador a proposta formatada:

```
/setup-architecture: proposta para "<descricao>"

Linguagem:          <linguagem + versao>
                    Motivo: <justificativa>

Framework:          <framework>
                    Motivo: <justificativa>

Estilo:             <estilo arquitetural>
                    Motivo: <justificativa>

Banco de dados:     <banco>
                    Motivo: <justificativa>

Testes:
  Unit:             <ferramenta>
  Integration:      <ferramenta>
  E2E:              <ferramenta>

Estrutura de pacotes:
  <lista de pacotes>
```

Usar AskUserQuestion com a pergunta "Aprovar esta arquitetura e gerar docs/architecture.md?"
Opcoes: "Sim, aprovar" e "Nao, cancelar".

Se cancelar: exibir "Arquitetura cancelada. Nenhum arquivo gerado." e terminar.

## Passo 3 -- Gerar docs/architecture.md

Verificar se o diretorio `docs/` existe:

```powershell
Test-Path docs
```

Se nao existir: criar o diretorio com `New-Item -ItemType Directory -Path docs`.

Gerar o arquivo `docs/architecture.md` com o conteudo aprovado:

```markdown
# Architecture Decision — <descricao-do-projeto>

> Gerado por /setup-architecture. Revisado e aprovado pelo operador.

## Linguagem e Runtime

**<linguagem + versao>**

<justificativa>

## Framework Principal

**<framework>**

<justificativa>

## Estilo Arquitetural

**<estilo>**

<justificativa>

### Estrutura de Pacotes

```
<estrutura>
```

## Banco de Dados

**<banco>**

<justificativa>

## Estrategia de Testes

| Nivel       | Ferramenta   | Escopo                              |
|-------------|--------------|-------------------------------------|
| Unit        | <ferramenta> | <escopo>                            |
| Integration | <ferramenta> | <escopo>                            |
| E2E         | <ferramenta> | <escopo>                            |

## Decisoes Complementares

<outras decisoes relevantes para o dominio especifico>
```

Apos gravar o arquivo, verificar existencia:

```powershell
Test-Path docs/architecture.md
```

Se nao existir: reportar erro e terminar.

## Passo 4 -- Relatorio

Exibir:

```
/setup-architecture concluido.

Arquivo gerado: docs/architecture.md
Decisoes:
  Linguagem:   <linguagem>
  Framework:   <framework>
  Estilo:      <estilo>
  Banco:       <banco>

Proximo passo: /setup-design "<descricao>" ou /setup-infra "<descricao>"
```
