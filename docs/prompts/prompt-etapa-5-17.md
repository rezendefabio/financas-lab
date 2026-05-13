# Prompt -- Sub-etapa 5.17: Redesign visual Fintech-Clean (polimento)

## Contexto

Polimento visual das paginas existentes. Motivacoes identificadas na revisao manual:

1. **Bug de fonte**: `globals.css` tem `--font-sans: var(--font-sans)` -- auto-referencia circular
   que nao resolve. Geist nao esta sendo aplicado. Titulos ficam em fonte serif do sistema.
2. **Login**: card flutuando em tela branca vazia -- sem identidade visual.
3. **Sidebar**: sem indicador de item ativo -- impossivel saber qual pagina esta selecionada.
4. **Dashboard**: so um card pequeno numa tela enorme -- parece inacabado.
5. **Contas**: cards sem hierarquia visual entre ativa/inativa.
6. **Botao Desativar**: rosa claro parece incompleto.

Nenhuma mudanca de logica, rotas ou dados. So camada visual.

---

## Mudanca 1 -- Fix de fonte (`src/app/globals.css`)

Leia o arquivo antes de editar.

Na secao `@theme inline`, corrigir a linha:

```css
/* ERRADO (auto-referencia, nao resolve) */
--font-sans: var(--font-sans);

/* CORRETO */
--font-sans: var(--font-geist-sans);
```

Essa e a causa dos titulos em fonte serif. Apos esta correcao, Geist Sans
sera aplicado em todo o app automaticamente via `html { @apply font-sans; }`.

---

## Mudanca 2 -- Login split-layout (`src/app/(auth)/login/page.tsx`)

Leia o arquivo completo antes de editar. Substituir o `return` do componente:

```tsx
return (
  <div className="min-h-screen flex">
    {/* Painel esquerdo -- identidade visual (oculto em mobile) */}
    <div className="hidden lg:flex lg:w-1/2 bg-primary flex-col items-center justify-center p-12">
      <div className="space-y-4 max-w-xs text-center text-primary-foreground">
        <div className="text-5xl font-bold tracking-tight">FL</div>
        <h1 className="text-2xl font-semibold">Financas Lab</h1>
        <p className="text-primary-foreground/70 text-sm leading-relaxed">
          Gestao financeira pessoal. Simples, rapido e seguro.
        </p>
      </div>
    </div>

    {/* Painel direito -- formulario */}
    <div className="flex w-full lg:w-1/2 flex-col items-center justify-center p-8 bg-background">
      <div className="w-full max-w-sm space-y-6">
        {/* Header mobile (visivel apenas em telas pequenas) */}
        <div className="lg:hidden text-center space-y-1">
          <h1 className="text-2xl font-bold">Financas Lab</h1>
        </div>

        <div className="space-y-1">
          <h2 className="text-xl font-semibold tracking-tight">Bem-vindo de volta</h2>
          <p className="text-sm text-muted-foreground">Entre com sua conta para continuar</p>
        </div>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Email</FormLabel>
                  <FormControl>
                    <Input type="email" placeholder="seu@email.com" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="senha"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Senha</FormLabel>
                  <FormControl>
                    <Input type="password" placeholder="••••••••" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            {error && (
              <p className="text-sm text-destructive">{error}</p>
            )}
            <Button
              type="submit"
              className="w-full"
              disabled={form.formState.isSubmitting}
            >
              {form.formState.isSubmitting ? 'Entrando...' : 'Entrar'}
            </Button>
          </form>
        </Form>
      </div>
    </div>
  </div>
)
```

Manter toda a logica de estado, hooks e imports existentes -- alterar apenas o JSX do return.

---

## Mudanca 3 -- Sidebar com item ativo (`src/app/(dashboard)/layout.tsx`)

Leia o arquivo completo antes de editar.

Adicionar `usePathname` para detectar rota ativa:

```tsx
import { usePathname } from 'next/navigation'

// dentro do componente:
const pathname = usePathname()
```

Atualizar o render dos itens de menu para passar `isActive`:

```tsx
{navItems.map((item) => {
  const isActive = item.href === '/'
    ? pathname === '/'
    : pathname.startsWith(item.href)

  return (
    <SidebarMenuItem key={item.href}>
      <SidebarMenuButton
        render={<Link href={item.href} />}
        isActive={isActive}
      >
        <item.icon className="h-4 w-4" />
        <span>{item.label}</span>
      </SidebarMenuButton>
    </SidebarMenuItem>
  )
})}
```

Se `SidebarMenuButton` nao aceitar prop `isActive` (verificar pelo erro de TypeScript):
alternativa e adicionar `aria-current={isActive ? 'page' : undefined}` e estilizar via CSS:

```css
/* adicionar em globals.css no @layer base */
[data-sidebar="menu-button"][aria-current="page"] {
  @apply bg-sidebar-primary text-sidebar-primary-foreground;
}
```

Leia `.claude/shared/components/ui/sidebar.tsx` para confirmar se `isActive` e suportado
antes de decidir a abordagem.

---

## Mudanca 4 -- Dashboard mais impactante (`src/app/(dashboard)/page.tsx`)

Leia o arquivo completo antes de editar. Substituir o conteudo apos o `<h1>`:

```tsx
<div className="space-y-6">
  <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>

  {/* Card principal -- saldo total */}
  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
    <Card className="sm:col-span-2 lg:col-span-1 border-l-4 border-l-primary">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground flex items-center gap-2">
          <TrendingUp className="h-4 w-4 text-primary" />
          Saldo Total
        </CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading && <div className="h-10 animate-pulse rounded bg-muted" />}
        {isError && <p className="text-sm text-destructive">Erro ao carregar saldo</p>}
        {data && (
          <>
            <p className="text-3xl font-bold tabular-nums text-foreground">
              {formatBRL(data.valor)}
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              {data.totalContas} {data.totalContas === 1 ? 'conta ativa' : 'contas ativas'}
            </p>
          </>
        )}
      </CardContent>
    </Card>

    {/* Cards placeholder para proximas features */}
    <Card className="border-dashed">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          Transacoes este mes
        </CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-2xl font-bold text-muted-foreground/40">—</p>
        <p className="text-xs text-muted-foreground mt-1">em breve</p>
      </CardContent>
    </Card>

    <Card className="border-dashed">
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          Orcamento do mes
        </CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-2xl font-bold text-muted-foreground/40">—</p>
        <p className="text-xs text-muted-foreground mt-1">em breve</p>
      </CardContent>
    </Card>
  </div>
</div>
```

Adicionar import do `TrendingUp` do lucide-react.

---

## Mudanca 5 -- Cards de contas com borda lateral (`src/app/(dashboard)/contas/page.tsx`)

Leia o arquivo completo antes de editar.

No componente `ContaCard`, alterar o `className` do `Card` para usar borda lateral colorida:

```tsx
<Card
  className={cn(
    "cursor-pointer transition-colors hover:bg-muted/50 border-l-4",
    conta.ativa ? "border-l-primary" : "border-l-border"
  )}
  onClick={onClick}
>
```

Adicionar import do `cn` de `@/shared/lib/utils` se nao existir.

No `CardContent`, tornar o saldo mais destacado:

```tsx
<CardContent>
  <p className="text-sm text-muted-foreground">{formatTipoConta(conta.tipo)}</p>
  <p className="text-xl font-bold tabular-nums mt-2">
    {formatBRL(conta.saldoInicialValor)}
  </p>
  <p className="text-xs text-muted-foreground">saldo inicial</p>
</CardContent>
```

Manter o `Badge` no header (Ativa/Inativa) -- a borda lateral e o badge se complementam.

---

## Mudanca 6 -- Botao Desativar conta (`src/app/(dashboard)/contas/[id]/page.tsx`)

Leia o arquivo completo antes de editar.

Substituir o botao de desativacao (estado inicial, antes da confirmacao):

```tsx
{!confirmando ? (
  <Button
    variant="outline"
    className="border-destructive text-destructive hover:bg-destructive hover:text-destructive-foreground"
    onClick={() => setConfirmando(true)}
  >
    Desativar conta
  </Button>
) : (
  // manter o estado de confirmacao existente sem alteracao
)}
```

---

## Mudanca 7 -- Raio de borda mais discreto (`src/app/globals.css`)

Na secao `:root`, alterar o radius base:

```css
/* ANTES */
--radius: 0.625rem;

/* DEPOIS */
--radius: 0.5rem;
```

Isso reduz o arredondamento global de botoes e cards, dando visual mais profissional
e menos "bubbly".

---

## Validacao

```powershell
.\scripts\check-front.ps1
```

Lint + testes + build devem passar. Os testes existentes nao testam estilos CSS,
entao nao devem quebrar com mudancas visuais.

Se `isActive` no `SidebarMenuButton` causar erro de TypeScript:
usar a abordagem alternativa com `aria-current` + CSS descrita na Mudanca 3.

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-17-redesign-polimento

2. Ler antes de implementar:
   - frontend/src/app/globals.css
   - frontend/src/app/(auth)/login/page.tsx
   - frontend/src/app/(dashboard)/layout.tsx
   - frontend/src/app/(dashboard)/page.tsx
   - frontend/src/app/(dashboard)/contas/page.tsx
   - frontend/src/app/(dashboard)/contas/[id]/page.tsx
   - frontend/src/shared/components/ui/sidebar.tsx (para verificar isActive)

3. Aplicar Mudanca 1 (fix fonte -- mais impactante, fazer primeiro)

4. Aplicar Mudanca 2 (login split)

5. Aplicar Mudanca 3 (sidebar ativo)

6. Aplicar Mudancas 4, 5, 6, 7 (dashboard, contas, botao, radius)

7. .\scripts\check-front.ps1 -- verde antes de commitar

8. commit: feat(frontend): redesign visual fintech-clean (split login, sidebar ativo, dashboard)

9. Atualizar docs/progresso.md

10. commit: docs(progresso): registra sub-etapa 5.17
    (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-17.md)

11. /ship -> PR; corrigir apontamentos autonomamente
```

---

## Estrutura de commits (5.17)

```
feat(frontend): redesign visual fintech-clean (split login, sidebar ativo, dashboard)
docs(progresso): registra sub-etapa 5.17
```

---

## Restricoes

- NAO alterar logica de negocio, rotas, queries ou formularios.
- NAO modificar testes existentes -- mudancas visuais nao afetam testes de comportamento.
- NAO adicionar dependencias novas -- usar apenas Tailwind, shadcn, lucide-react existentes.
- Se `isActive` nao for suportado pelo SidebarMenuButton: usar aria-current + CSS
  (nao inventar prop que nao existe -- verificar sidebar.tsx antes).
- Os cards placeholder do dashboard devem ter `border-dashed` para indicar visualmente
  que sao incompletos/futuros.

---

## Estado esperado ao terminar

- PR aberto com 2 commits acima de main.
- Geist Sans aplicado em toda a tipografia (titulos sem serif).
- Login com painel esquerdo emerald + formulario no direito.
- Item ativo na sidebar com destaque visual (emerald ou zinc-800).
- Dashboard com 3 cards (saldo total destacado + 2 placeholders).
- Cards de contas com borda lateral colorida (verde = ativa, zinc = inativa).
- Botao "Desativar conta" com outline vermelho.
- `.\scripts\check-front.ps1` verde.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO rodar /ship mais de uma vez.
- NAO testar visualmente -- validacao e via check-front.ps1. Operador testa no browser.
