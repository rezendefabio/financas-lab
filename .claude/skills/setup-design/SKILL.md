---
name: setup-design
description: Inicializa design system do projeto. Recebe dominio como argumento e flag --figma <url> opcional. Spawna planejador que propoe design system completo, aguarda aprovacao do operador, gera docs/design-system.md e componentes wrapper.
disable-model-invocation: true
argument-hint: "<dominio> [--figma <url>]"
---

Voce deve inicializar o design system do projeto. Execute todos os passos em ordem.

## Input

Argumento recebido: `<dominio> [--figma <url>]`

Exemplos:
- `/setup-design "gestao financeira pessoal"`
- `/setup-design "software hospitalar" --figma https://figma.com/file/abc123`

## Passo 0 -- Parsear argumentos

Extraia do argumento recebido:

1. **DOMINIO**: tudo antes de `--figma`, com trim. Se `--figma` nao presente, o argumento todo.
2. **FIGMA_URL**: valor apos `--figma`, com trim. Se ausente: `"N/A"`.

Validacao: se DOMINIO for vazio apos trim, reporte erro e termine:
```
ERRO: dominio nao pode ser vazio.
Uso: /setup-design "<dominio>" [--figma <url>]
Exemplo: /setup-design "gestao financeira pessoal"
```

## Passo 1 -- Spawnar sub-agente planejador

Use o Agent tool com:

- `subagent_type`: `design-planner`
- `prompt`: (montar conforme template abaixo, substituindo DOMINIO e FIGMA_URL)

```
Produza uma proposta completa de design system para o projeto financas-lab.

DOMINIO: {DOMINIO}
FIGMA_URL: {FIGMA_URL}

Instrucoes:
1. Leia docs/design-system.md existente como referencia de formato
2. Leia CLAUDE.md para entender stack (shadcn/ui base-nova, @base-ui/react, Next.js)
3. Liste os componentes wrapper existentes em frontend/src/shared/components/ (excluindo ui/)
4. Produza proposta completa cobrindo: paleta, tipografia, componentes, mapeamentos, page templates, bloqueadores
5. Retorne o conteudo no formato prescrito (dois blocos separados por ---SEPARADOR---)
```

Aguarde o resultado completo do sub-agente antes de continuar.

## Passo 2 -- Exibir proposta e aguardar aprovacao

Exiba ao operador o output completo do sub-agente planejador.

Em seguida, use AskUserQuestion com:

- Pergunta: `"Deseja aprovar esta proposta e gerar docs/design-system.md e os componentes wrapper listados?"`
- Opcoes: `["Sim, gerar", "Nao, cancelar"]`

Se operador escolher "Nao, cancelar":
```
/setup-design cancelado pelo operador.
Nenhum arquivo foi gerado.
```
Encerrar sem gerar nada.

## Passo 3 -- Parsear output do planejador

O output do planejador contem dois blocos separados por `---SEPARADOR---`.

- **Bloco 1**: conteudo do `docs/design-system.md`
- **Bloco 2**: lista de componentes wrapper a criar

Extraia cada bloco. Se o separador nao for encontrado, trate o output inteiro como Bloco 1
e considere que nenhum componente novo precisa ser criado.

## Passo 4 -- Gerar `docs/design-system.md`

Use a ferramenta Write para escrever o Bloco 1 em `docs/design-system.md`.

Verificacao pos-escrita:
```powershell
Test-Path "docs/design-system.md"
```

Se Test-Path retornar False: reporte erro e termine sem continuar.

## Passo 5 -- Gerar componentes wrapper

Parse o Bloco 2 para identificar a lista de componentes novos a criar.

Para cada componente listado como "a criar" (nao os existentes MoneyInput, StatCard, StatusBadge):

1. Verifique se ja existe em `frontend/src/shared/components/`:
   ```powershell
   Test-Path "frontend/src/shared/components/{NomeDoComponente}.tsx"
   ```
   Se existir: pule (nao sobrescrever).

2. Se nao existir: crie o arquivo `.tsx` com implementacao baseada no design system aprovado.
   - Usar `'use client'` se o componente usa hooks ou eventos
   - Seguir o padrao dos componentes existentes (MoneyInput, StatCard)
   - Exportar como named export E default export

3. Crie o arquivo de teste Vitest correspondente:
   `frontend/src/shared/components/{NomeDoComponente}.test.tsx`

   Padrao do teste:
   ```tsx
   import { render, screen } from '@testing-library/react'
   import { describe, it, expect } from 'vitest'
   import { NomeDoComponente } from './{NomeDoComponente}'

   describe('{NomeDoComponente}', () => {
     it('renderiza sem erros', () => {
       render(<NomeDoComponente {...propsMinimas} />)
       // assert especifico ao componente
     })
   })
   ```

Se o Bloco 2 indicar "Nenhum componente wrapper novo necessario": pule este passo.

## Passo 6 -- Relatorio final

```
/setup-design concluido.

Dominio:     {DOMINIO}
Figma:       {FIGMA_URL}

Arquivos gerados:
  docs/design-system.md
  [lista de componentes wrapper criados, um por linha, ou "Nenhum componente novo criado"]

Proximos passos:
  1. Revisar docs/design-system.md e ajustar paleta se necessario
  2. Commitar: feat(claude): /setup-design -- design system {DOMINIO}
  3. Invocar /ship para abrir PR
```

**Importante:** a skill NAO commita nem abre PR automaticamente. O operador commita manualmente
apos revisar os arquivos gerados.
