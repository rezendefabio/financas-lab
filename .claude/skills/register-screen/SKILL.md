---
name: register-screen
description: Registra uma nova tela no Screen Registry do frontend (frontend/src/shared/shell/screens.registry.ts). Valida o formato do code (MOD-ENT-NNN), unicidade e profundidade do menuPath, e injeta a entrada ScreenDefinition no manifesto. Argumentos: code title path menuPath icon (ver argument-hint).
disable-model-invocation: true
argument-hint: [code] [title] [path] [menuPath separado por > ] [icon]
---

Voce deve registrar uma nova tela no Screen Registry declarativo do frontend
do projeto financas-lab. O registry e a fonte de verdade do menu hierarquico e
do Command Palette (ADR-014, fase UI-1; `docs/frontend-master-spec.md` secao 4.1
e 4.3).

Execute todos os passos em ordem. Pare e reporte ao operador se qualquer
validacao falhar -- nao adivinhe valores.

## Contrato real do registry

Arquivo alvo: `frontend/src/shared/shell/screens.registry.ts`.

A skill manipula o array `screens: ScreenDefinition[]` exportado nesse arquivo.
O tipo `ScreenDefinition` (definido no proprio arquivo) e:

```ts
interface ScreenDefinition {
  code: string         // MOD-ENT-NNN
  title: string
  path: string
  menuPath: string[]   // hierarquia, max 3 niveis
  icon: string         // nome do icone lucide-react
  permissions: string[] // vazio nesta fase (RBAC fora do escopo)
}
```

O arquivo tambem exporta as constantes `SCREEN_CODE_REGEX` e `MAX_MENU_DEPTH`
e os helpers `getAllScreens()`, `findScreenByCode(code)` e
`findScreenByPath(path)`. Esta skill usa o regex e o limite de profundidade
abaixo, que devem coincidir com essas constantes.

## Definicoes dos argumentos

`$ARGUMENTS` traz, em ordem: `code`, `title`, `path`, `menuPath`, `icon`.
O `menuPath` e informado como segmentos separados por ` > ` (ex:
`Cadastros > Financeiro > Contas`) e deve ser convertido para um array de
strings.

Se algum argumento estiver ausente: escreva o erro abaixo e termine.

```
ERRO: /register-screen requer 5 argumentos.

Uso:
  /register-screen <code> <title> <path> "<menuPath com ' > '>" <icon>

Exemplo:
  /register-screen FIN-INV-001 Investimentos /investimentos "Cadastros > Financeiro > Investimentos" wallet
```

## Passo 0 -- Validacoes (ADR-011)

**Validacao 1 -- formato do code:**
Verifique que `code` casa com o regex `^[A-Z]{3}-[A-Z]{3}-\d{3}$` (formato
MOD-ENT-NNN da spec secao 4.3 / constante `SCREEN_CODE_REGEX`). Se nao casar:
escreva `ERRO: code invalido. Formato esperado MOD-ENT-NNN (ex: FIN-CTA-001).`
e termine. Code invalido e bloqueador.

**Validacao 2 -- unicidade do code:**
Leia `frontend/src/shared/shell/screens.registry.ts` com a ferramenta Read.
Verifique se ja existe alguma entrada no array `screens` com o mesmo `code`.
Se existir: escreva
`ERRO (BLOQUEADOR): code <code> ja registrado no screens.registry.ts. Escolha outro sequencial.`
e termine. Code duplicado e bloqueador -- a skill para e nao injeta nada.

**Validacao 3 -- profundidade do menuPath:**
Converta `menuPath` para array. Se o array tiver mais de `MAX_MENU_DEPTH` (3)
segmentos: escreva
`ERRO: menuPath tem N niveis; o maximo permitido e 3 (spec secao 4.1).`
e termine.

Se o array tiver 0 segmentos: escreva
`ERRO: menuPath nao pode ser vazio -- a tela precisa de ao menos um grupo/folha.`
e termine.

**Validacao 4 -- path unico (aviso):**
Verifique se ja existe entrada com o mesmo `path`. Se existir, isso nao e
bloqueador, mas avise: `AVISO: ja existe uma tela com path <path>.` e pergunte
ao operador se deseja continuar.

## Passo 1 -- Validar o icone

O campo `icon` deve ser um nome valido de icone do `lucide-react`, na
convencao kebab-case usada pelo registry (ex: `credit-card`, `bar-chart-3`,
`alert-triangle`). Verifique que o icone existe no mapa
`frontend/src/shared/shell/icon-map.ts`:

- Se o nome ja estiver em `iconMap`: ok, prossiga.
- Se NAO estiver: o icone novo precisa ser adicionado ao `iconMap` antes que o
  menu consiga renderiza-lo. Oriente o operador a adicionar o import do icone
  lucide-react correspondente e a entrada no `iconMap`, OU escolha um icone ja
  mapeado. Nao injete a tela com um icone fora do mapa sem registrar isso no
  relatorio final como pendencia.

## Passo 2 -- Injetar a entrada no registry

Use a ferramenta Edit para inserir uma nova entrada `ScreenDefinition` no array
`screens` de `frontend/src/shared/shell/screens.registry.ts`, ANTES do
fechamento `]` do array. Preserve a ordem das entradas existentes (a nova vai
ao final do array) e a formatacao (indentacao de 2 espacos, mesma estrutura das
entradas existentes, virgula final).

Formato da entrada a inserir:

```ts
  {
    code: '<code>',
    title: '<title>',
    path: '<path>',
    menuPath: [<segmentos como strings entre aspas, separados por virgula>],
    icon: '<icon>',
    permissions: [],
  },
```

`permissions` e sempre `[]` nesta fase -- RBAC esta fora do escopo (ADR-014
decisao 9).

## Passo 3 -- Verificacao pos-injecao

1. Releia o arquivo e confirme que a nova entrada esta presente, bem formada e
   que o array continua sintaticamente valido (virgulas, colchetes).
2. Confirme que o TypeScript continua compilando:

   ```bash
   cd frontend && npx tsc --noEmit --project tsconfig.json
   ```

   Se houver erro de TypeScript causado pela entrada nova: corrija a entrada.
   Erros pre-existentes em outros arquivos: ignore.

## Passo 4 -- Relatorio final

Produza o relatorio:

```
/register-screen concluido.

Tela registrada no screens.registry.ts:
  code:     <code>
  title:    <title>
  path:     <path>
  menuPath: <menuPath unido por ' / '>
  icon:     <icon>

Proximos passos:
  1. Garantir que a rota <path> existe em frontend/src/app/(dashboard)/
  2. Se o icone for novo, conferir que foi adicionado ao icon-map.ts
  3. npm run build (verificar sem erros)
```

Se algum passo falhou, NAO emita o relatorio de sucesso -- reporte o erro
encontrado e o estado do registry.
