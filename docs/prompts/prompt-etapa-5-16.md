# Prompt -- Sub-etapa 5.16: Tema Fintech-Clean + validacao espelhada + B6

## Contexto

Tres entregas nesta sub-etapa:

1. **Tema visual** -- paleta "Fintech-Clean": dark sidebar zinc-900, fundo zinc-50,
   acento emerald, numeros financeiros com tabular-nums.
2. **Grid e alinhamento** -- sistema de espacamento e proporcao de campos consistente
   em todas as paginas existentes.
3. **Regra de validacao espelhada** -- formularios frontend devem replicar exatamente
   as anotacoes de validacao dos DTOs Java. Formalizado como B6 no front-reviewer
   e como convencao no CLAUDE.md.

Camada 4. Sem novos bounded contexts. Apenas frontend.

---

## Parte 1 -- Tema Fintech-Clean

### 1.1 Utilitario de formatacao: `src/shared/lib/formatters.ts`

Criar arquivo novo (nao existe ainda):

```typescript
export function formatBRL(valor: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(valor)
}

export function formatTipoConta(tipo: string): string {
  const labels: Record<string, string> = {
    CORRENTE: 'Conta Corrente',
    POUPANCA: 'Poupanca',
    DINHEIRO: 'Dinheiro',
    CARTAO_CREDITO: 'Cartao de Credito',
  }
  return labels[tipo] ?? tipo
}
```

Substituir toda ocorrencia inline de `Intl.NumberFormat` e labels de tipo nas
paginas existentes por essas funcoes. Leia cada pagina antes de editar.

### 1.2 Tema: `src/app/globals.css`

Leia o arquivo completo antes de editar. O projeto usa OKLCH.

Substituir as variaveis de cor do `:root` (light mode) pelas abaixo.
Manter estrutura existente (border-radius, font, etc.) -- alterar apenas
os valores de cor:

```css
:root {
  --background: oklch(0.985 0.001 286.375);       /* zinc-50 */
  --foreground: oklch(0.141 0.005 285.823);        /* zinc-900 */
  --card: oklch(1 0 0);                            /* white */
  --card-foreground: oklch(0.141 0.005 285.823);
  --popover: oklch(1 0 0);
  --popover-foreground: oklch(0.141 0.005 285.823);
  --primary: oklch(0.596 0.145 163.225);           /* emerald-600 */
  --primary-foreground: oklch(1 0 0);
  --secondary: oklch(0.967 0.001 286.375);         /* zinc-100 */
  --secondary-foreground: oklch(0.141 0.005 285.823);
  --muted: oklch(0.967 0.001 286.375);
  --muted-foreground: oklch(0.552 0.016 285.938);  /* zinc-500 */
  --accent: oklch(0.951 0.021 163.225);            /* emerald-50 */
  --accent-foreground: oklch(0.444 0.119 163.225); /* emerald-700 */
  --destructive: oklch(0.577 0.245 27.325);        /* rose-500 */
  --destructive-foreground: oklch(1 0 0);
  --border: oklch(0.922 0.003 286.375);            /* zinc-200 */
  --input: oklch(0.922 0.003 286.375);
  --ring: oklch(0.596 0.145 163.225);              /* emerald-600 */
  --chart-1: oklch(0.596 0.145 163.225);
  --chart-2: oklch(0.527 0.154 150.069);
  --chart-3: oklch(0.444 0.119 163.225);
  --chart-4: oklch(0.792 0.122 163.225);
  --chart-5: oklch(0.870 0.072 163.225);
  --sidebar: oklch(0.21 0.006 285.885);            /* zinc-900 */
  --sidebar-foreground: oklch(0.985 0.001 286.375);/* zinc-50 */
  --sidebar-primary: oklch(0.596 0.145 163.225);   /* emerald-600 */
  --sidebar-primary-foreground: oklch(1 0 0);
  --sidebar-accent: oklch(0.274 0.006 286.033);    /* zinc-800 */
  --sidebar-accent-foreground: oklch(0.985 0.001 286.375);
  --sidebar-border: oklch(0.274 0.006 286.033);
  --sidebar-ring: oklch(0.596 0.145 163.225);
}
```

Manter o bloco `.dark` existente sem alteracoes (dark mode nao e escopo desta sub-etapa).

### 1.3 Numeros financeiros

Em todos os componentes que exibem valores monetarios, adicionar a classe
Tailwind `tabular-nums` no elemento que contem o numero:

```tsx
<span className="tabular-nums font-medium">{formatBRL(valor)}</span>
```

---

## Parte 2 -- Grid e alinhamento consistente

### 2.1 Sistema de layout para paginas de listagem

Todas as paginas de listagem devem seguir esta estrutura:

```tsx
<div className="space-y-6">
  {/* Header */}
  <div className="flex items-center justify-between">
    <h1 className="text-2xl font-semibold tracking-tight">Titulo</h1>
    <Button>Acao principal</Button>
  </div>

  {/* Conteudo */}
  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
    {/* cards */}
  </div>
</div>
```

### 2.2 Sistema de layout para paginas de formulario

Formularios devem estar contidos em largura maxima e centralizados:

```tsx
<div className="space-y-6">
  {/* Header */}
  <div className="flex items-center gap-4">
    <Button variant="ghost" size="icon" onClick={() => router.back()}>
      <ArrowLeft className="h-4 w-4" />
    </Button>
    <h1 className="text-2xl font-semibold tracking-tight">Titulo</h1>
  </div>

  {/* Formulario contido */}
  <div className="max-w-xl">
    <Card>
      <CardContent className="pt-6 space-y-4">
        {/* campos */}
      </CardContent>
    </Card>
  </div>
</div>
```

### 2.3 Proporcao de campos por tipo de dado

Aplicar nas paginas de formulario existentes:

| Campo | Largura | Justificativa |
|-------|---------|--------------|
| nome (texto livre, max 100) | `w-full` | Texto longo, usa largura total |
| tipo (select enum) | `w-full` | Select padrao, largura total |
| valor monetario | `w-full max-w-xs` | Numero com poucos digitos |
| moeda (3 chars, readonly) | `w-20` | Campo fixo, 3 caracteres |
| UUID / ID (readonly) | `w-full font-mono text-sm` | Monoespaco para IDs |

### 2.4 Pagina de detalhe de conta

A pagina `contas/[id]/page.tsx` deve exibir as informacoes em grid 2 colunas:

```tsx
<dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
  <div>
    <dt className="text-sm text-muted-foreground">Nome</dt>
    <dd className="font-medium">{conta.nome}</dd>
  </div>
  <div>
    <dt className="text-sm text-muted-foreground">Tipo</dt>
    <dd className="font-medium">{formatTipoConta(conta.tipo)}</dd>
  </div>
  <div>
    <dt className="text-sm text-muted-foreground">Saldo Atual</dt>
    <dd className="tabular-nums font-semibold text-lg">
      {saldo ? formatBRL(saldo.valor) : '—'}
    </dd>
  </div>
  <div>
    <dt className="text-sm text-muted-foreground">Saldo Inicial</dt>
    <dd className="tabular-nums">{formatBRL(conta.saldoInicialValor)}</dd>
  </div>
</dl>
```

---

## Parte 3 -- Validacao espelhada e regra B6

### 3.1 Verificar e corrigir schema Zod de `contas/novo`

Leia `src/main/java/com/laboratorio/financas/conta/interfaces/dto/CriarContaRequest.java`
e compare com o schema Zod atual em `contas/novo/page.tsx`.

Mapeamento obrigatorio:

| Campo Java | Anotacao Java | Schema Zod obrigatorio |
|------------|--------------|------------------------|
| `nome` | `@NotBlank @Size(max=100)` | `z.string().min(1, 'Nome obrigatorio').max(100)` |
| `tipo` | `@NotNull TipoConta` | `z.enum(['CORRENTE','POUPANCA','DINHEIRO','CARTAO_CREDITO'])` |
| `saldoInicialValor` | `@NotNull BigDecimal` | `z.coerce.number().min(0)` |
| `saldoInicialMoeda` | `@NotNull @Size(min=3,max=3)` | `z.string().length(3).default('BRL')` |

Se o schema atual divergir: corrigir. Se ja estiver correto: documentar no PR
que foi verificado.

### 3.2 Criar utilitario de teste em `src/shared/lib/formatters.test.ts`

Apos criar `formatters.ts`, invocar `/write-test` para gerar o teste:
- `formatBRL(1234.56)` retorna `'R$ 1.234,56'` (formato pt-BR)
- `formatTipoConta('CORRENTE')` retorna `'Conta Corrente'`
- `formatTipoConta('DESCONHECIDO')` retorna `'DESCONHECIDO'` (fallback)

### 3.3 Atualizar `.claude/agents/front-reviewer.md` -- adicionar B6

Leia o arquivo completo antes de editar. Adicionar linha na tabela de Bloqueadores:

```markdown
| B6 | Schema Zod divergente do DTO Java | Para formularios: comparar z.string().max() com @Size(max=), z.enum() com enum Java, z.string().min(1) com @NotBlank. Divergencia e bloqueador. |
```

### 3.4 Atualizar `CLAUDE.md` -- convencao de validacao espelhada

Leia o arquivo antes de editar. Na secao `## Frontend`, adicionar apos a linha
de `/write-test`:

```markdown
- Validacao: ao criar formulario frontend, ler o `*Request.java` correspondente
  e espelhar cada anotacao Java no schema Zod: `@NotBlank` → `.min(1)`,
  `@Size(max=N)` → `.max(N)`, `@Size(min=M,max=N)` → `.length` ou `.min().max()`,
  `@Min(N)` → `.min(N)`. Divergencia entre Zod e Java e bloqueador (B6).
```

---

## Validacao (check-front.ps1)

Antes do commit:
```powershell
.\scripts\check-front.ps1
```

Deve passar lint + todos os testes (46+ existentes + teste de formatters) + build.

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-16-tema-fintech-clean

2. Ler antes de implementar:
   - frontend/src/app/globals.css
   - frontend/src/app/(dashboard)/layout.tsx
   - frontend/src/app/(dashboard)/page.tsx
   - frontend/src/app/(dashboard)/contas/page.tsx
   - frontend/src/app/(dashboard)/contas/novo/page.tsx
   - frontend/src/app/(dashboard)/contas/[id]/page.tsx
   - src/main/java/.../conta/interfaces/dto/CriarContaRequest.java
   - .claude/agents/front-reviewer.md
   - CLAUDE.md secao ## Frontend

3. Criar src/shared/lib/formatters.ts

4. Invocar /write-test frontend/src/shared/lib/formatters.ts

5. Atualizar globals.css (variaveis de cor)

6. Atualizar layout.tsx (dark sidebar -- verificar se ja tem, ajustar se precisar)

7. Atualizar dashboard page.tsx (formatBRL, tabular-nums, grid)

8. Atualizar contas/page.tsx (formatBRL, formatTipoConta, grid, header)

9. Atualizar contas/novo/page.tsx (schema Zod, proporcao campos, layout formulario)

10. Atualizar contas/[id]/page.tsx (dl/dt/dd grid, formatBRL, tabular-nums, layout)

11. .\scripts\check-front.ps1 -- verde antes de continuar

12. commit: feat(frontend): aplica tema fintech-clean, grid e formatadores

13. Editar .claude/agents/front-reviewer.md (adicionar B6)

14. Editar CLAUDE.md (convencao de validacao espelhada)

15. commit: feat(claude): adiciona B6 ao front-reviewer e convencao de validacao

16. Atualizar docs/progresso.md (registra 5.16)

17. commit: docs(progresso): registra sub-etapa 5.16
    (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-16.md)

18. /ship -> PR com reviews; corrigir apontamentos autonomamente
```

---

## Estrutura de commits (5.16)

```
feat(frontend): aplica tema fintech-clean, grid e formatadores
feat(claude): adiciona B6 ao front-reviewer e convencao de validacao
docs(progresso): registra sub-etapa 5.16
```

---

## Restricoes

- NAO alterar o bloco `.dark` do globals.css (dark mode fora do escopo).
- NAO redesenhar fluxos de navegacao -- apenas visual e layout.
- NAO adicionar dependencias novas -- usar apenas o que ja existe (Tailwind, shadcn, lucide).
- NAO mover arquivos -- refactor de estrutura foi feito na 5.15.
- Sidebar ja usa o tema `--sidebar-*` do globals.css via shadcn Sidebar component --
  atualizar apenas as variaveis CSS, nao o componente diretamente.
- Se `check-front.ps1` falhar apos mudancas no globals.css: verificar se alguma
  variavel CSS foi removida acidentalmente (o build do Next.js detecta CSS invalido).
- Testes existentes de paginas nao devem quebrar com mudancas visuais -- se quebrarem,
  investigar o motivo antes de corrigir (pode indicar teste fraco ou mudanca de DOM).

---

## Estado esperado ao terminar

- PR aberto com 3 commits acima de main.
- Sidebar com fundo zinc-900 (dark).
- Fundo das paginas zinc-50 (off-white suave).
- Botoes, badges e elementos de acao na cor emerald.
- Valores monetarios com `tabular-nums` e `formatBRL` em todas as paginas.
- Formulario de nova conta com campos proporcionais e schema Zod espelhando Java.
- `.\scripts\check-front.ps1` verde.
- B6 documentado no front-reviewer.
- Convencao de validacao espelhada no CLAUDE.md.

---

## O que NAO fazer ao terminar

- NAO fazer merge sem instrucao do operador.
- NAO rodar /ship mais de uma vez.
- NAO alterar logica de negocio ou queries -- apenas camada visual.
