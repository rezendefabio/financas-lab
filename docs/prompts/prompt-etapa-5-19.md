# Prompt -- Sub-etapa 5.19: Transacoes frontend (listagem e criacao)

## Contexto

Implementar as paginas de Transacoes no frontend. O bounded context `transacao`
ja existe no backend e o `transacoesService` ja esta em
`src/features/transacoes/services/transacoes.service.ts`. Esta sub-etapa
adiciona as paginas Next.js e corrige a interface do servico para espelhar
o DTO Java corretamente.

Camada 4. Apenas frontend. Sem mudancas no backend.

---

## ATENCAO: bug no servico existente (regra B6)

Leia `src/main/java/.../transacao/interfaces/dto/TransacaoRequest.java` e
`frontend/src/features/transacoes/services/transacoes.service.ts`.

O Java usa campos `valor` (BigDecimal) e `moeda` (String) na raiz do record.
O servico TypeScript tem `valorValor` e `valorMoeda` -- divergencia de B6.

Corrigir antes de implementar as paginas:

```typescript
interface CriarTransacaoRequest {
  tipo: 'RECEITA' | 'DESPESA' | 'TRANSFERENCIA'
  valor: number         // era valorValor
  moeda: string         // era valorMoeda
  data: string          // ISO date: "2026-05-13"
  descricao: string
  contaId: string
  contaDestinoId?: string
  categoriaId?: string
}
```

Atualizar a chamada `criar` no servico para usar `valor` e `moeda`.

---

## Referencia para o padrao de codigo

Antes de comecar, leia:

- `frontend/src/app/(dashboard)/contas/page.tsx` (padrao de listagem)
- `frontend/src/app/(dashboard)/contas/novo/page.tsx` (padrao de formulario com select)
- `frontend/src/features/transacoes/services/transacoes.service.ts` (servico a corrigir)
- `frontend/src/features/transacoes/types/transacao.ts` (tipos existentes)
- `frontend/src/features/contas/services/contas.service.ts` (para carregar contas no form)
- `frontend/src/features/categorias/services/categorias.service.ts` (para carregar categorias no form)
- `frontend/src/shared/lib/formatters.ts` (adicionar funcoes aqui)
- `src/main/java/.../transacao/interfaces/dto/TransacaoRequest.java` (DTO Java -- fonte da verdade)
- `src/main/java/.../transacao/domain/TipoTransacao.java` (enum: RECEITA, DESPESA, TRANSFERENCIA)

---

## Mudanca 1 -- Adicionar formatadores em `src/shared/lib/formatters.ts`

Leia o arquivo antes de editar. Adicionar ao final:

```typescript
export function formatTipoTransacao(tipo: string): string {
  const labels: Record<string, string> = {
    RECEITA: 'Receita',
    DESPESA: 'Despesa',
    TRANSFERENCIA: 'Transferencia',
  }
  return labels[tipo] ?? tipo
}

export function formatDate(dataIso: string): string {
  // Append noon UTC to avoid day-off-by-one from timezone conversion
  return new Date(dataIso + 'T12:00:00').toLocaleDateString('pt-BR')
}
```

Atualizar `frontend/src/shared/lib/formatters.test.ts` adicionando:

```typescript
it('formatTipoTransacao retorna label em portugues', () => {
  expect(formatTipoTransacao('RECEITA')).toBe('Receita')
  expect(formatTipoTransacao('DESPESA')).toBe('Despesa')
  expect(formatTipoTransacao('TRANSFERENCIA')).toBe('Transferencia')
  expect(formatTipoTransacao('DESCONHECIDO')).toBe('DESCONHECIDO')
})

it('formatDate formata data ISO no padrao pt-BR', () => {
  expect(formatDate('2026-05-13')).toBe('13/05/2026')
})
```

Nota: se `formatters.ts` ja tiver `formatTipoCategoria` (adicionado pela sub-etapa 5.18),
mantenha -- apenas adicione as funcoes novas ao final.

---

## Mudanca 2 -- Pagina de listagem (`src/app/(dashboard)/transacoes/page.tsx`)

```tsx
'use client'
import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { transacoesService } from '@/features/transacoes/services/transacoes.service'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { Skeleton } from '@/shared/components/ui/skeleton'
import { formatBRL, formatTipoTransacao, formatDate } from '@/shared/lib/formatters'
import type { Transacao } from '@/features/transacoes/types/transacao'

function badgeVariant(tipo: string) {
  if (tipo === 'RECEITA') return 'default' as const
  if (tipo === 'DESPESA') return 'destructive' as const
  return 'secondary' as const
}

function TransacaoCard({ transacao }: { transacao: Transacao }) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base truncate max-w-[60%]">
            {transacao.descricao}
          </CardTitle>
          <Badge variant={badgeVariant(transacao.tipo)}>
            {formatTipoTransacao(transacao.tipo)}
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-xl font-bold tabular-nums">
          {formatBRL(transacao.valor.valor)}
        </p>
        <p className="text-xs text-muted-foreground mt-1">{formatDate(transacao.data)}</p>
      </CardContent>
    </Card>
  )
}

export default function TransacoesPage() {
  const router = useRouter()
  const { data, isLoading, isError } = useQuery({
    queryKey: ['transacoes'],
    queryFn: () => transacoesService.listar({ size: 20 }),
  })

  const transacoes = data?.content ?? []

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold tracking-tight">Transacoes</h1>
        <Button onClick={() => router.push('/transacoes/novo')}>Nova Transacao</Button>
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardHeader>
                <Skeleton className="h-5 w-40" />
              </CardHeader>
              <CardContent className="space-y-2">
                <Skeleton className="h-6 w-28" />
                <Skeleton className="h-4 w-20" />
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {isError && (
        <p className="text-sm text-destructive">Erro ao carregar transacoes.</p>
      )}

      {!isLoading && !isError && transacoes.length === 0 && (
        <div className="flex flex-col items-center gap-4 py-12 text-center">
          <p className="text-muted-foreground">Nenhuma transacao cadastrada.</p>
          <Button onClick={() => router.push('/transacoes/novo')}>Registrar primeira transacao</Button>
        </div>
      )}

      {transacoes.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {transacoes.map((t) => (
            <TransacaoCard key={t.id} transacao={t} />
          ))}
        </div>
      )}

      {data && data.totalElements > 20 && (
        <p className="text-xs text-muted-foreground text-center">
          Exibindo 20 de {data.totalElements} transacoes.
        </p>
      )}
    </div>
  )
}
```

---

## Mudanca 3 -- Formulario de criacao (`src/app/(dashboard)/transacoes/novo/page.tsx`)

Schema Zod espelhando `TransacaoRequest.java`:

| Campo Java | Anotacao | Zod |
|-----------|----------|-----|
| `tipo` | `@NotNull TipoTransacao` | `z.enum(['RECEITA','DESPESA','TRANSFERENCIA'])` |
| `valor` | `@NotNull BigDecimal` | `z.coerce.number().positive('Valor deve ser positivo')` |
| `moeda` | `@NotNull @Size(min=3,max=3)` | `z.string().length(3).default('BRL')` |
| `data` | `@NotNull LocalDate` | `z.string().min(1, 'Data obrigatoria')` |
| `descricao` | `@NotBlank @Size(max=200)` | `z.string().min(1, 'Descricao obrigatoria').max(200)` |
| `contaId` | `@NotNull UUID` | `z.string().uuid('Conta obrigatoria')` |
| `contaDestinoId` | nullable UUID | `z.string().uuid().optional()` |
| `categoriaId` | nullable UUID | `z.string().uuid().optional()` |

```tsx
'use client'
import { useForm, Controller, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import { transacoesService } from '@/features/transacoes/services/transacoes.service'
import { contasService } from '@/features/contas/services/contas.service'
import { categoriasService } from '@/features/categorias/services/categorias.service'
import { Card, CardContent } from '@/shared/components/ui/card'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/shared/components/ui/form'
import { Input } from '@/shared/components/ui/input'
import { Button } from '@/shared/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'

const schema = z.object({
  tipo: z.enum(['RECEITA', 'DESPESA', 'TRANSFERENCIA']),
  valor: z.coerce.number().positive('Valor deve ser positivo'),
  moeda: z.string().length(3).default('BRL'),
  data: z.string().min(1, 'Data obrigatoria'),
  descricao: z.string().min(1, 'Descricao obrigatoria').max(200),
  contaId: z.string().uuid('Selecione uma conta'),
  contaDestinoId: z.string().uuid().optional(),
  categoriaId: z.string().uuid().optional(),
})

type FormValues = z.infer<typeof schema>

const TIPOS = [
  { value: 'RECEITA', label: 'Receita' },
  { value: 'DESPESA', label: 'Despesa' },
  { value: 'TRANSFERENCIA', label: 'Transferencia' },
] as const

export default function NovaTransacaoPage() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState<string | null>(null)

  const { data: contas } = useQuery({
    queryKey: ['contas'],
    queryFn: contasService.listar,
  })

  const { data: categorias } = useQuery({
    queryKey: ['categorias'],
    queryFn: categoriasService.listar,
  })

  const today = new Date().toISOString().slice(0, 10)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema) as Resolver<FormValues>,
    defaultValues: {
      tipo: 'DESPESA',
      valor: 0,
      moeda: 'BRL',
      data: today,
      descricao: '',
      contaId: '',
      contaDestinoId: undefined,
      categoriaId: undefined,
    },
  })

  const tipoAtual = form.watch('tipo')
  const isTransferencia = tipoAtual === 'TRANSFERENCIA'

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      transacoesService.criar({
        tipo: values.tipo,
        valor: values.valor,
        moeda: values.moeda,
        data: values.data,
        descricao: values.descricao,
        contaId: values.contaId,
        ...(values.contaDestinoId ? { contaDestinoId: values.contaDestinoId } : {}),
        ...(values.categoriaId ? { categoriaId: values.categoriaId } : {}),
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['transacoes'] })
      router.push('/transacoes')
    },
    onError: () => {
      setApiError('Erro ao registrar transacao.')
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Voltar" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold tracking-tight">Nova Transacao</h1>
      </div>

      <div className="max-w-xl">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <Form {...form}>
              <form
                onSubmit={form.handleSubmit((v) => { setApiError(null); mutation.mutate(v) })}
                className="space-y-4"
              >
                {/* Tipo */}
                <FormItem>
                  <FormLabel>Tipo</FormLabel>
                  <Controller
                    control={form.control}
                    name="tipo"
                    render={({ field }) => (
                      <Select value={field.value} onValueChange={field.onChange}>
                        <SelectTrigger className="w-full">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {TIPOS.map((t) => (
                            <SelectItem key={t.value} value={t.value}>{t.label}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                  />
                </FormItem>

                {/* Valor */}
                <FormField
                  control={form.control}
                  name="valor"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Valor (R$)</FormLabel>
                      <FormControl>
                        <Input type="number" step="0.01" min="0.01" className="w-full max-w-xs" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {/* Data */}
                <FormField
                  control={form.control}
                  name="data"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Data</FormLabel>
                      <FormControl>
                        <Input type="date" className="w-full max-w-xs" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {/* Descricao */}
                <FormField
                  control={form.control}
                  name="descricao"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Descricao</FormLabel>
                      <FormControl>
                        <Input className="w-full" placeholder="Ex: Supermercado" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {/* Conta origem */}
                <FormItem>
                  <FormLabel>Conta {isTransferencia ? 'de origem' : ''}</FormLabel>
                  <Controller
                    control={form.control}
                    name="contaId"
                    render={({ field }) => (
                      <Select value={field.value} onValueChange={field.onChange}>
                        <SelectTrigger className="w-full">
                          <SelectValue placeholder="Selecione a conta" />
                        </SelectTrigger>
                        <SelectContent>
                          {(contas ?? []).filter(c => c.ativa).map((c) => (
                            <SelectItem key={c.id} value={c.id}>{c.nome}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                  />
                  {form.formState.errors.contaId && (
                    <p className="text-sm text-destructive">{form.formState.errors.contaId.message}</p>
                  )}
                </FormItem>

                {/* Conta destino (so para transferencia) */}
                {isTransferencia && (
                  <FormItem>
                    <FormLabel>Conta de destino</FormLabel>
                    <Controller
                      control={form.control}
                      name="contaDestinoId"
                      render={({ field }) => (
                        <Select value={field.value ?? ''} onValueChange={(v) => field.onChange(v || undefined)}>
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="Selecione a conta de destino" />
                          </SelectTrigger>
                          <SelectContent>
                            {(contas ?? []).filter(c => c.ativa).map((c) => (
                              <SelectItem key={c.id} value={c.id}>{c.nome}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      )}
                    />
                  </FormItem>
                )}

                {/* Categoria (opcional, oculto para transferencia) */}
                {!isTransferencia && categorias && categorias.length > 0 && (
                  <FormItem>
                    <FormLabel>Categoria (opcional)</FormLabel>
                    <Controller
                      control={form.control}
                      name="categoriaId"
                      render={({ field }) => (
                        <Select value={field.value ?? ''} onValueChange={(v) => field.onChange(v || undefined)}>
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="Sem categoria" />
                          </SelectTrigger>
                          <SelectContent>
                            {categorias
                              .filter(c => !isTransferencia && c.tipo === tipoAtual)
                              .map((c) => (
                                <SelectItem key={c.id} value={c.id}>{c.nome}</SelectItem>
                              ))}
                          </SelectContent>
                        </Select>
                      )}
                    />
                  </FormItem>
                )}

                <input type="hidden" {...form.register('moeda')} />

                {apiError && (
                  <p className="text-sm text-destructive">{apiError}</p>
                )}

                <div className="flex gap-3 pt-2">
                  <Button type="submit" disabled={mutation.isPending}>
                    {mutation.isPending ? 'Salvando...' : 'Salvar'}
                  </Button>
                  <Button type="button" variant="outline" onClick={() => router.push('/transacoes')}>
                    Cancelar
                  </Button>
                </div>
              </form>
            </Form>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
```

---

## Validacao

```powershell
.\scripts\check-front.ps1
```

Lint + testes + build devem passar.

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-19-transacoes-frontend

2. Ler arquivos de referencia listados acima

3. Corrigir CriarTransacaoRequest no servico (valorValor -> valor, valorMoeda -> moeda)

4. Adicionar formatTipoTransacao e formatDate em formatters.ts
   (se o arquivo ja tiver formatTipoCategoria de 5.18, apenas adicionar ao final)

5. Atualizar formatters.test.ts com casos de formatTipoTransacao e formatDate

6. Criar frontend/src/app/(dashboard)/transacoes/page.tsx

7. Criar frontend/src/app/(dashboard)/transacoes/novo/page.tsx

8. .\scripts\check-front.ps1 -- verde antes de continuar

9. commit: feat(transacoes): implementa paginas de listagem e criacao

10. Atualizar docs/progresso.md (registra sub-etapa 5.19)
    ATENCAO: se houver conflito de merge com progresso.md (sub-etapa 5.18 mesclada
    primeiro), resolver mantendo ambas as entradas.

11. commit: docs(progresso): registra sub-etapa 5.19
    (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-19.md)

12. /ship -> PR; corrigir apontamentos autonomamente
```

---

## Estrutura de commits (5.19)

```
feat(transacoes): implementa paginas de listagem e criacao
docs(progresso): registra sub-etapa 5.19
```

---

## Restricoes

- Corrigir o servico existente (B6) antes de criar as paginas.
- NAO adicionar pagina de detalhe (fora do escopo).
- NAO adicionar paginacao com controles -- exibir primeira pagina com aviso simples
  se houver mais de 20 registros.
- Categoria no formulario: filtrar pelo tipo da transacao (RECEITA mostra categorias
  RECEITA, DESPESA mostra categorias DESPESA).
- Campo contaDestinoId visivel APENAS quando tipo === 'TRANSFERENCIA'.
- Campo categoriaId oculto quando tipo === 'TRANSFERENCIA'.
- Schema Zod deve espelhar exatamente `TransacaoRequest.java` (regra B6).
- Usar `render` prop em vez de `asChild` (base-nova nao tem @radix-ui).

---

## Estado esperado ao terminar

- PR aberto com 2 commits acima de main.
- Servico corrigido: `valor` e `moeda` (nao `valorValor`/`valorMoeda`).
- `/transacoes` lista cards com badge por tipo (verde/vermelho/cinza).
- `/transacoes/novo` formulario com tipo, valor, data, descricao, conta, (categoria e
  conta destino condicionais).
- `.\scripts\check-front.ps1` verde.
