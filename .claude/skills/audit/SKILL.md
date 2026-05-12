---
name: audit
description: Varre src/main/java/ buscando um padrao e reporta todos os matches com contexto (arquivo, linha, trecho). Recebe padrao de busca como argumento (string ou regex). Exemplos: /audit TODO, /audit @Deprecated, /audit UnsupportedOperationException.
disable-model-invocation: true
argument-hint: [padrao-de-busca]
---

Voce deve varrer `src/main/java/` buscando o padrao `$ARGUMENTS` e reportar todos
os matches com contexto estruturado.

## Passo 0 -- Validacao

Se `$ARGUMENTS` estiver vazio ou nao informado: escreva a mensagem abaixo e termine.

```
ERRO: /audit requer um padrao de busca.

Exemplos:
  /audit TODO
  /audit @Deprecated
  /audit UnsupportedOperationException
  /audit "@Entity"
  /audit "throw new RuntimeException"
```

## Passo 1 -- Buscar

Use a ferramenta Grep com os parametros:
- pattern: `$ARGUMENTS`
- path: `src/main/java/`
- output_mode: `content`
- `-n`: true (mostrar numeros de linha)

## Passo 2 -- Formatar e reportar

**Se nenhum match encontrado:**
```
/audit "$ARGUMENTS"

Nenhum match encontrado em src/main/java/.
```

**Se matches encontrados:** agrupe por arquivo. Para cada arquivo liste as linhas
com match no formato `  L<numero>: <conteudo da linha>`. Ao final, totalize.

Formato de saida:

```
/audit "<padrao>"

<caminho/do/Arquivo1.java> (<n> match(es))
  L<numero>: <conteudo>
  L<numero>: <conteudo>

<caminho/do/Arquivo2.java> (<n> match(es))
  L<numero>: <conteudo>

Total: <total> match(es) em <arquivos> arquivo(s)
```
