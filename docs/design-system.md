# Design System — financas-lab

Guia de referencia para construcao de interfaces no projeto financas-lab.
Tema: shadcn/ui com estilo `base-nova` (usa `@base-ui/react`, nao `@radix-ui`).
Leia este arquivo antes de implementar qualquer tela nova.

---

## 1. Paleta de Cores

Cores semanticas definidas em `frontend/src/app/globals.css` via variaveis CSS (OKLCH).

### Cores principais

| Token | Valor aproximado | Uso |
|---|---|---|
| `primary` | emerald-600 (`oklch(0.596 0.145 163.225)`) | Acoes principais, CTAs, botoes de confirmacao, links ativos, barra de sidebar |
| `primary-foreground` | white | Texto sobre fundo primary |
| `destructive` | rose-500 (`oklch(0.577 0.245 27.325)`) | Erros, acoes irreversiveis, alertas criticos, mensagens de validacao |
| `destructive-foreground` | white | Texto sobre fundo destructive |
| `muted` | zinc-100 | Fundos de secoes secundarias, footer de cards |
| `muted-foreground` | zinc-500 | Textos secundarios, labels, placeholders, descricoes |
| `accent` | emerald-50 | Destaques suaves, hover states em itens de lista/select |
| `accent-foreground` | emerald-700 | Texto sobre fundo accent |
| `border` / `input` | zinc-200 | Bordas de campos e divisores |
| `ring` | emerald-600 | Outline de foco em campos e botoes |

### Sidebar

| Token | Valor | Uso |
|---|---|---|
| `sidebar` | zinc-900 | Fundo da navegacao lateral |
| `sidebar-foreground` | zinc-50 | Texto da sidebar |
| `sidebar-primary` | emerald-600 | Item ativo na sidebar |
| `sidebar-accent` | zinc-800 | Hover de itens da sidebar |

### Cores semanticas de contexto (classes utilitarias Tailwind)

Nao existem tokens CSS dedicados para warning e info -- usar classes Tailwind diretamente.

| Contexto | Classe texto | Classe fundo | Uso |
|---|---|---|---|
| Success / Receita | `text-primary` | `bg-accent` | Valores positivos, receitas, metas atingidas |
| Warning / Atencao | `text-amber-600` | `bg-amber-50` | Orcamento em atencao, prazo proximo |
| Info | `text-blue-600` | `bg-blue-50` | Informacoes neutras, dicas |
| Danger / Despesa | `text-destructive` | `bg-destructive/10` | Valores negativos, despesas, excedido |

### Graficos

Cinco tokens de chart disponíveis (`chart-1` a `chart-5`), todos em escala de verde emerald:
`chart-1` (emerald-600) → `chart-2` → `chart-3` (emerald-700) → `chart-4` (emerald-300) → `chart-5` (emerald-200 suave).

---

## 2. Tipografia

Fontes: `--font-geist-sans` (corpo) e `--font-geist-mono` (codigo). Sem fontes de terceiros.

### Escala tipografica

| Elemento | Classes Tailwind | Onde usar |
|---|---|---|
| Titulo de pagina | `text-2xl font-semibold tracking-tight` | `<h1>` no topo de cada rota |
| Titulo de card / secao | `text-base font-semibold` | `<CardTitle>`, headers de secao |
| Label de campo | `text-sm font-medium` | `<FormLabel>`, `<Label>` |
| Texto secundario / label de dado | `text-sm text-muted-foreground` | Descricoes, metadados, `<CardDescription>` |
| Valor monetario em destaque | `text-xl font-bold tabular-nums` | Saldo principal, KPI de StatCard |
| Valor monetario normal | `font-medium tabular-nums` | Valores em tabelas e listas |
| Percentual / metrica pequena | `text-sm tabular-nums` | Percentual de utilizacao, contadores |
| Mensagem de erro | `text-sm font-medium text-destructive` | `<FormMessage>` |
| Caption / rodape de tabela | `text-sm text-muted-foreground` | `<TableCaption>` |

Nota: `tabular-nums` e obrigatorio em qualquer valor numerico para evitar tremulacao ao atualizar.

---

## 3. Componentes Disponiveis

Componentes shadcn instalados em `frontend/src/shared/components/ui/`.

### Badge

**Arquivo:** `badge.tsx`

**Quando usar:** etiquetas inline, status de entidades, categorias, tipos.

**Variantes:**

| Variante | Aparencia | Uso tipico |
|---|---|---|
| `default` | fundo primary (emerald), texto branco | item ativo, status positivo |
| `secondary` | fundo zinc-100, texto escuro | categoria, tipo neutro |
| `destructive` | fundo rose/10, texto rose | erro, status critico |
| `outline` | borda visivel, sem fundo | tag opcional, filtro |
| `ghost` | sem borda, sem fundo (hover suave) | contador discreto |
| `link` | texto primary com underline | link inline |

### Button

**Arquivo:** `button.tsx`

**Quando usar:** toda acao do usuario (submit, cancelar, deletar, navegar).

**Variantes:**

| Variante | Uso tipico |
|---|---|
| `default` | acao principal (salvar, confirmar) |
| `outline` | acao secundaria (cancelar, voltar) |
| `secondary` | acao terciaria, filtros |
| `ghost` | acoes em linha (editar, detalhes) |
| `destructive` | acao irreversivel (excluir, cancelar pedido) |
| `link` | navegacao inline |

**Tamanhos:** `xs`, `sm`, `default`, `lg`, `icon`, `icon-xs`, `icon-sm`, `icon-lg`.

### Card

**Arquivo:** `card.tsx`

**Quando usar:** container de conteudo agrupado (entidades na grade, resumos, formularios).

**Subcomponentes:** `CardHeader`, `CardTitle`, `CardDescription`, `CardAction`, `CardContent`, `CardFooter`.

**Tamanhos:** `default` (padding 16px) e `sm` (padding 12px).

Padrao tipico:

```tsx
<Card>
  <CardHeader>
    <CardTitle>Titulo</CardTitle>
    <CardDescription>Descricao opcional</CardDescription>
    <CardAction><Button size="sm">Acao</Button></CardAction>
  </CardHeader>
  <CardContent>conteudo</CardContent>
  <CardFooter>rodape</CardFooter>
</Card>
```

### Dialog

**Arquivo:** `dialog.tsx`

**Quando usar:** formularios de criacao/edicao, confirmacoes de acao destrutiva.

**Subcomponentes:** `DialogTrigger`, `DialogContent`, `DialogHeader`, `DialogTitle`, `DialogDescription`, `DialogFooter`, `DialogClose`.

Nota: usa `@base-ui/react/dialog`. O botao de fechar e renderizado via `render` prop, nao `asChild`.

### DropdownMenu

**Arquivo:** `dropdown-menu.tsx`

**Quando usar:** menu de acoes por linha (editar, excluir, detalhes) acionado por botao de tres pontos.

### Form

**Arquivo:** `form.tsx`

**Quando usar:** todo formulario com validacao (react-hook-form + Zod).

**Subcomponentes:** `Form` (= FormProvider), `FormField`, `FormItem`, `FormLabel`, `FormControl`, `FormDescription`, `FormMessage`.

Padrao obrigatorio para cada campo:

```tsx
<FormField
  control={form.control}
  name="nomeDoCampo"
  render={({ field }) => (
    <FormItem>
      <FormLabel>Label</FormLabel>
      <FormControl>
        <Input {...field} />
      </FormControl>
      <FormMessage />
    </FormItem>
  )}
/>
```

### Input

**Arquivo:** `input.tsx`

**Quando usar:** campos de texto, numero, data, email.

Tipos principais usados no projeto: `text`, `number`, `date`, `month`, `email`, `hidden`.

### Label

**Arquivo:** `label.tsx`

**Quando usar:** labels standalone fora de formulario react-hook-form. Dentro de formulario, usar `FormLabel`.

### Progress

**Arquivo:** `progress.tsx`

**Quando usar:** percentual de utilizacao de orcamento, progresso de meta.

Props: `value` (numero atual), `max` (padrao 100). A barra usa `bg-primary` (emerald).

Padrao com texto:

```tsx
<div className="space-y-1">
  <Progress value={percentual} max={100} />
  <p className="text-sm tabular-nums text-muted-foreground">{percentual.toFixed(0)}%</p>
</div>
```

### Select

**Arquivo:** `select.tsx`

**Quando usar:** campos de enum fixo e FKs carregadas de endpoint.

**Subcomponentes:** `Select` (Root), `SelectTrigger`, `SelectValue`, `SelectContent`, `SelectGroup`, `SelectLabel`, `SelectItem`, `SelectSeparator`.

Tamanhos do trigger: `default` (h-8) e `sm` (h-7).

### Separator

**Arquivo:** `separator.tsx`

**Quando usar:** divisor visual entre secoes de um card ou formulario.

### Sheet

**Arquivo:** `sheet.tsx`

**Quando usar:** painel lateral deslizante para filtros ou formularios secundarios sem sair da pagina.

### Skeleton

**Arquivo:** `skeleton.tsx`

**Quando usar:** placeholder de carregamento enquanto dados sao buscados (loading state).

### Sonner (Toast)

**Arquivo:** `sonner.tsx`

**Quando usar:** notificacoes de feedback apos acoes (salvo com sucesso, erro ao excluir).

Usar `toast.success()`, `toast.error()`, `toast.info()` do pacote `sonner`.

### Table

**Arquivo:** `table.tsx`

**Quando usar:** listagens com 3+ colunas, acoes por linha.

**Subcomponentes:** `Table`, `TableHeader`, `TableBody`, `TableFooter`, `TableHead`, `TableRow`, `TableCell`, `TableCaption`.

### Tooltip

**Arquivo:** `tooltip.tsx`

**Quando usar:** texto explicativo em icones de acao (botoes sem label visivel).

---

### Componentes wrapper planejados (serao criados em task separada)

Os tres componentes abaixo ainda nao existem no codigo, mas serao criados em
`frontend/src/shared/components/`. Esta secao documenta como serao usados.

#### MoneyInput

**Caminho futuro:** `frontend/src/shared/components/MoneyInput.tsx`

**Quando usar:** todo campo `BigDecimal` com significado monetario em formularios.

**Comportamento:** aceita apenas digitos, formata enquanto digita (mascara BRL), valor interno em `number`.

**Nao usar** `<Input type="number">` direto para valores monetarios -- usar `MoneyInput`.

Uso esperado:

```tsx
<FormField
  control={form.control}
  name="saldoInicial"
  render={({ field }) => (
    <FormItem>
      <FormLabel>Saldo Inicial</FormLabel>
      <FormControl>
        <MoneyInput {...field} />
      </FormControl>
      <FormMessage />
    </FormItem>
  )}
/>
```

#### StatusBadge

**Caminho futuro:** `frontend/src/shared/components/StatusBadge.tsx`

**Quando usar:** exibicao de enum com cor semantica (status de orcamento, status de meta).

**Comportamento:** recebe o valor raw do enum e um mapa de configuracao (label + variante de Badge).

Uso esperado:

```tsx
{/* Status do orcamento: ABAIXO / ATENCAO / ATINGIDO / EXCEDIDO */}
<StatusBadge status={orcamento.status} />
```

#### StatCard

**Caminho futuro:** `frontend/src/shared/components/StatCard.tsx`

**Quando usar:** metricas e KPIs no dashboard ou cabecalho de paginas de resumo.

**Props esperadas:** `titulo` (string), `valor` (string formatado), `variacao` (opcional, numero com sinal).

Uso esperado:

```tsx
<StatCard
  titulo="Saldo Total"
  valor={formatBRL(saldoTotal)}
  variacao={variacaoMensal}
/>
```

---

## 4. Mapeamento Tipo de Dado → Componente

### Inputs (formularios)

| Tipo / Situacao | Componente | Exemplo de campo |
|---|---|---|
| `BigDecimal` monetario | `MoneyInput` | `saldoInicial`, `valorLimite`, `valorAlvo` |
| `String` nome / descricao | `<Input type="text">` com `maxLength` | nome da conta, descricao da categoria |
| `LocalDate` data simples | `<Input type="date">` | prazo da meta |
| `LocalDate` mes/ano | `<Input type="month">` (concatenar `-01` ao enviar) | `mesAno` do orcamento |
| Enum fixo | `<Select>` com mapa de labels | `TipoConta`, `TipoCategoria` |
| `UUID` FK (`categoriaId`, `contaId`) | `<Select>` carregado do endpoint correspondente | `categoriaId` no orcamento |
| `boolean` | `<Switch>` ou radio Sim/Nao | ativo/inativo |
| `String` moeda (`"BRL"`) | `<input type="hidden">` -- nunca expor ao usuario | `valorLimiteMoeda` |
| `@Email` | `<Input type="email">` | email do usuario |

### Exibicao (listagens e detalhes)

| Tipo / Situacao | Componente | Exemplo de campo |
|---|---|---|
| `BigDecimal` monetario | `formatBRL(value)` em texto read-only | saldo, limite, valor alvo |
| Status / enum com cor | `StatusBadge` | status do orcamento, status da meta |
| `boolean` ativo/inativo | `<Badge variant="default">` / `<Badge variant="secondary">` | conta ativa/inativa |
| Percentual (0-100+) | `<Progress>` + texto `XX%` | `percentualUtilizado` |
| Metrica / KPI | `StatCard` | saldo total, metas em andamento |
| `LocalDate` | `formatDate(value)` | prazo, data de lancamento |
| `Instant` / `LocalDateTime` | `formatDate(value)` read-only, nunca editavel | `criadoEm`, `atualizadoEm` |
| Objeto aninhado `ValorMonetario` | Acessar `.valor` antes de formatar | `formatBRL(orcamento.valorLimite.valor)` |
| Enum raw | label do mapa de formatacao | `formatTipoConta(conta.tipo)` |

---

## 5. Page Templates

Templates de estrutura JSX para os quatro layouts recorrentes do projeto.
Cada template e comentado com a responsabilidade de cada bloco.

### Template: Lista (tabular)

**Usar quando:** entidade tem 3+ atributos e precisa de acao por linha (orcamentos, metas, transacoes).

```tsx
// page.tsx
export default function EntidadesPage() {
  return (
    // Container principal da pagina
    <div className="flex flex-col gap-6 p-6">

      {/* Cabecalho: titulo + botao de nova entidade */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Entidades</h1>
        <Button>Nova Entidade</Button>
      </div>

      {/* Tabela principal */}
      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Nome</TableHead>
                <TableHead>Valor</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Acoes</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {entidades.map((item) => (
                <TableRow key={item.id}>
                  <TableCell className="font-medium">{item.nome}</TableCell>
                  <TableCell className="tabular-nums">{formatBRL(item.valor)}</TableCell>
                  <TableCell><StatusBadge status={item.status} /></TableCell>
                  <TableCell className="text-right">
                    {/* Menu de acoes por linha */}
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon-sm">...</Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent>
                        <DropdownMenuItem>Editar</DropdownMenuItem>
                        <DropdownMenuItem className="text-destructive">Excluir</DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

    </div>
  )
}
```

### Template: Grade de Cards

**Usar quando:** entidade tem identidade visual propria e poucos atributos (contas bancarias, categorias).

```tsx
// page.tsx
export default function EntidadesPage() {
  return (
    <div className="flex flex-col gap-6 p-6">

      {/* Cabecalho */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Contas</h1>
        <Button>Nova Conta</Button>
      </div>

      {/* Grade responsiva de cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {entidades.map((item) => (
          <Card key={item.id}>
            <CardHeader>
              <CardTitle>{item.nome}</CardTitle>
              <CardDescription>{formatTipoConta(item.tipo)}</CardDescription>
              {/* Acoes no canto superior direito */}
              <CardAction>
                <Button variant="ghost" size="icon-sm">...</Button>
              </CardAction>
            </CardHeader>
            <CardContent>
              {/* Valor de destaque */}
              <p className="text-xl font-bold tabular-nums">
                {formatBRL(item.saldo)}
              </p>
              <p className="text-sm text-muted-foreground">Saldo atual</p>
            </CardContent>
            <CardFooter>
              {/* Status ou metadado secundario */}
              <Badge variant={item.ativa ? "default" : "secondary"}>
                {item.ativa ? "Ativa" : "Inativa"}
              </Badge>
            </CardFooter>
          </Card>
        ))}
      </div>

    </div>
  )
}
```

### Template: Formulario

**Usar quando:** criacao ou edicao de entidade com validacao.

```tsx
// components/EntidadeForm.tsx
"use client"
export function EntidadeForm() {
  const form = useForm<EntidadeFormValues>({
    resolver: zodResolver(entidadeSchema),
    defaultValues: { nome: "", valor: 0 },
  })

  return (
    // Form e o FormProvider do react-hook-form
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">

        {/* Campo de texto */}
        <FormField
          control={form.control}
          name="nome"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Nome</FormLabel>
              <FormControl>
                <Input placeholder="Nome da entidade" maxLength={100} {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Campo monetario */}
        <FormField
          control={form.control}
          name="valor"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Valor</FormLabel>
              <FormControl>
                <MoneyInput {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Campo enum via Select */}
        <FormField
          control={form.control}
          name="tipo"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Tipo</FormLabel>
              <FormControl>
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="Selecione..." />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="OPCAO_A">Opcao A</SelectItem>
                    <SelectItem value="OPCAO_B">Opcao B</SelectItem>
                  </SelectContent>
                </Select>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Botoes de acao */}
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="outline" onClick={onCancel}>
            Cancelar
          </Button>
          <Button type="submit" disabled={form.formState.isSubmitting}>
            Salvar
          </Button>
        </div>

      </form>
    </Form>
  )
}
```

### Template: Detalhe

**Usar quando:** visualizacao de entidade individual com acoes (desativar, cancelar, excluir).

```tsx
// page.tsx
export default function EntidadeDetailPage() {
  return (
    <div className="flex flex-col gap-6 p-6">

      {/* Cabecalho com titulo e acoes */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{entidade.nome}</h1>
          <p className="text-sm text-muted-foreground">Criado em {formatDate(entidade.criadoEm)}</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={onEdit}>Editar</Button>
          <Button variant="destructive" onClick={onDelete}>Excluir</Button>
        </div>
      </div>

      {/* Card de dados */}
      <Card>
        <CardHeader>
          <CardTitle>Informacoes</CardTitle>
        </CardHeader>
        <CardContent>
          {/* Grade de campo: label + valor */}
          <dl className="grid grid-cols-2 gap-x-4 gap-y-3 sm:grid-cols-3">
            <div>
              <dt className="text-sm text-muted-foreground">Tipo</dt>
              <dd className="text-sm font-medium">{formatTipoConta(entidade.tipo)}</dd>
            </div>
            <div>
              <dt className="text-sm text-muted-foreground">Saldo</dt>
              <dd className="text-sm font-medium tabular-nums">{formatBRL(entidade.saldo)}</dd>
            </div>
            <div>
              <dt className="text-sm text-muted-foreground">Status</dt>
              <dd><Badge variant={entidade.ativa ? "default" : "secondary"}>
                {entidade.ativa ? "Ativa" : "Inativa"}
              </Badge></dd>
            </div>
          </dl>
        </CardContent>
      </Card>

    </div>
  )
}
```

### Template: Dashboard

**Usar quando:** visao geral com metricas e resumos de multiplas entidades.

```tsx
// page.tsx
export default function DashboardPage() {
  return (
    <div className="flex flex-col gap-6 p-6">

      {/* Titulo da pagina */}
      <h1 className="text-2xl font-semibold tracking-tight">Visao Geral</h1>

      {/* Grade de KPIs */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard titulo="Saldo Total" valor={formatBRL(saldoTotal)} />
        <StatCard titulo="Receitas do Mes" valor={formatBRL(receitasMes)} variacao={+5.2} />
        <StatCard titulo="Despesas do Mes" valor={formatBRL(despesasMes)} variacao={-2.1} />
        <StatCard titulo="Metas em Andamento" valor={String(metasAtivas)} />
      </div>

      {/* Cards de resumo por dominio */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">

        {/* Resumo de orcamentos */}
        <Card>
          <CardHeader>
            <CardTitle>Orcamentos do Mes</CardTitle>
            <CardAction>
              <Button variant="ghost" size="sm">Ver todos</Button>
            </CardAction>
          </CardHeader>
          <CardContent className="space-y-3">
            {orcamentos.map((o) => (
              <div key={o.id} className="space-y-1">
                <div className="flex justify-between text-sm">
                  <span className="font-medium">{o.categoria.nome}</span>
                  <span className="tabular-nums text-muted-foreground">
                    {formatBRL(o.totalGasto.valor)} / {formatBRL(o.valorLimite.valor)}
                  </span>
                </div>
                <Progress value={o.percentualUtilizado} max={100} />
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Ultimos lancamentos */}
        <Card>
          <CardHeader>
            <CardTitle>Ultimos Lancamentos</CardTitle>
            <CardAction>
              <Button variant="ghost" size="sm">Ver todos</Button>
            </CardAction>
          </CardHeader>
          <CardContent className="p-0">
            <Table>
              <TableBody>
                {lancamentos.map((l) => (
                  <TableRow key={l.id}>
                    <TableCell className="font-medium">{l.descricao}</TableCell>
                    <TableCell className="text-right tabular-nums">
                      <span className={l.tipo === "RECEITA" ? "text-primary" : "text-destructive"}>
                        {formatBRL(l.valor)}
                      </span>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

      </div>

    </div>
  )
}
```
