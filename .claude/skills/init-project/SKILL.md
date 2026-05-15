---
name: init-project
description: Macro-skill que inicializa projeto novo em 3 sub-etapas sequenciais: /setup-architecture, /setup-design e /setup-infra. Exibe plano completo ao operador antes de executar. Recebe descricao do projeto como argumento.
disable-model-invocation: true
argument-hint: "<descricao-do-projeto> [--figma <url>]"
---

Voce deve inicializar um projeto novo em 3 sub-etapas sequenciais. Execute todos os passos em ordem.
Nao use a Skill tool para invocar sub-etapas -- leia os arquivos SKILL.md e execute a logica manualmente.

## Input

Argumento recebido: descricao do projeto com flags opcionais.
Exemplos:
- `"sistema de gestao de tarefas"`
- `"e-commerce de livros usados --figma https://figma.com/file/abc123"`

## Passo 0 -- Parsear argumentos

Extrair da string de argumento recebida:

1. **descricao**: tudo antes de `--figma` (se presente), com trim. Se nao houver `--figma`, a string inteira.
2. **figmaUrl**: valor apos `--figma` (se presente), com trim. Null se ausente.

Validar:
- `descricao` nao pode ser vazia nem apenas espacos.
- Se vazia: exibir "ERRO: descricao do projeto e obrigatoria. Uso: /init-project <descricao> [--figma <url>]" e terminar.

## Passo 1 -- Exibir plano completo e aguardar aprovacao

Exibir ao operador:

```
/init-project: inicializando projeto "<descricao>"

Plano de execucao (3 sub-etapas sequenciais):

  [1] /setup-architecture "<descricao>"
      Define linguagem, framework e arquitetura do projeto.
      Gera: docs/architecture.md
      Aguarda aprovacao do operador antes de prosseguir.

  [2] /setup-design "<descricao>"[--figma <url>]
      Propoe paleta de cores, tipografia, componentes e mapeamentos de tipo-de-dado.
      Gera: docs/design-system.md + componentes wrapper em frontend/src/shared/components/
      Aguarda aprovacao do operador antes de prosseguir.

  [3] /setup-infra "<descricao>"
      Define CI/CD, Docker, banco de dados e hooks de qualidade.
      Gera: .github/workflows/ci.yml, docker-compose.yml
      Aguarda aprovacao do operador antes de prosseguir.

Cada sub-etapa exibe sua proposta e aguarda aprovacao individual.
O operador pode cancelar em qualquer ponto.
```

Usar AskUserQuestion com a pergunta "Iniciar /setup-architecture agora?"
Opcoes: "Sim, iniciar" e "Nao, cancelar".

Se cancelar: exibir "Execucao cancelada. Nenhum arquivo gerado." e terminar.

## Passo 2 -- Executar /setup-architecture

Ler o arquivo `.claude/skills/setup-architecture/SKILL.md`.

Executar a logica descrita nele manualmente, passo a passo, passando `descricao` como argumento.

A propria sub-etapa fara AskUserQuestion para aprovacao interna. Se o operador cancelar dentro da sub-etapa:
exibir "Sub-etapa 1 cancelada. /init-project encerrado." e terminar.

Ao concluir: confirmar que `docs/architecture.md` foi gerado:

```powershell
Test-Path docs/architecture.md
```

Se nao existir e sub-etapa nao foi cancelada: reportar como erro critico e terminar.

## Passo 3 -- Executar /setup-design

Ler o arquivo `.claude/skills/setup-design/SKILL.md`.

Executar a logica descrita nele manualmente, passando:
- `descricao` como dominio do projeto
- `figmaUrl` como referencia visual (se presente)

A propria sub-etapa fara AskUserQuestion para aprovacao interna. Se o operador cancelar dentro da sub-etapa:
exibir "Sub-etapa 2 cancelada. /init-project encerrado." e terminar.

Se o arquivo `.claude/skills/setup-design/SKILL.md` nao existir: reportar como aviso e pular esta
sub-etapa, continuando para o Passo 4. Registrar no relatorio final: "/setup-design: PULADO (arquivo nao encontrado)".

## Passo 4 -- Executar /setup-infra

Ler o arquivo `.claude/skills/setup-infra/SKILL.md`.

Executar a logica descrita nele manualmente, passando `descricao` como argumento.

A propria sub-etapa fara AskUserQuestion para aprovacao interna. Se o operador cancelar dentro da sub-etapa:
exibir "Sub-etapa 3 cancelada. /init-project parcialmente concluido (1 e 2 OK)." e terminar.

Ao concluir: confirmar que `.github/workflows/ci.yml` foi gerado:

```powershell
Test-Path .github/workflows/ci.yml
```

## Passo 5 -- Relatorio final

Exibir:

```
/init-project concluido.

Projeto:     <descricao>

Sub-etapas executadas:
  [1] /setup-architecture: OK -- docs/architecture.md gerado
  [2] /setup-design:       OK -- docs/design-system.md gerado
  [3] /setup-infra:        OK -- .github/workflows/ci.yml, docker-compose.yml gerados

Proximos passos:
  1. Revisar cada arquivo gerado em docs/
  2. Commitar: feat(init): inicializa projeto <descricao>
  3. Invocar /ship para abrir PR
```

Adaptar o status de cada sub-etapa conforme resultado real (OK, PULADO, CANCELADO).
