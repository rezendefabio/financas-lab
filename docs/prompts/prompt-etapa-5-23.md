# Prompt -- Sub-etapa 5.23: Fix categorias duplicadas no formulario de transacao

## Contexto

Bug visual: o select de categoria em `/transacoes/novo` exibe entradas duplicadas
(ex: "Mercado" aparece 3 vezes). Causa: o banco pode conter categorias com nomes
identicos criadas em sessoes distintas (sem constraint UNIQUE em nome+tipo).

A correcao e no frontend: deduplicar por `id` + exibir aviso quando
houver duplicatas de nome para alertar o usuario sem esconder dados.

Leia antes de comecar:
- `frontend/src/app/(dashboard)/transacoes/novo/page.tsx` (arquivo a corrigir)
- `frontend/src/features/categorias/types/categoria.ts` (tipo Categoria)

---

## Mudanca 1 -- Deduplicar categorias no select

No componente `NovaTransacaoPage`, antes do return, adicionar filtragem
das categorias para remover duplicatas de nome dentro do mesmo tipo:

```tsx
// Deduplicar por nome dentro do mesmo tipo (IDs distintos, nomes iguais = dado duplicado no banco)
const categoriasDoTipo = (categorias ?? [])
  .filter(c => c.tipo === tipoAtual)
  .filter((c, idx, arr) => arr.findIndex(x => x.nome === c.nome) === idx)

const temDuplicatas = (categorias ?? [])
  .filter(c => c.tipo === tipoAtual).length > categoriasDoTipo.length
```

Substituir o bloco de categoria no JSX por:

```tsx
{!isTransferencia && categoriasDoTipo.length > 0 && (
  <FormItem>
    <FormLabel>Categoria (opcional)</FormLabel>
    {temDuplicatas && (
      <p className="text-xs text-amber-600">
        Categorias com nomes duplicados detectadas. Exibindo uma de cada.
      </p>
    )}
    <Controller
      control={form.control}
      name="categoriaId"
      render={({ field }) => (
        <Select value={field.value ?? ''} onValueChange={(v) => field.onChange(v || undefined)}>
          <SelectTrigger className="w-full">
            <SelectValue placeholder="Sem categoria" />
          </SelectTrigger>
          <SelectContent>
            {categoriasDoTipo.map((c) => (
              <SelectItem key={c.id} value={c.id}>{c.nome}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      )}
    />
  </FormItem>
)}
```

---

## Mudanca 2 -- Simplificar condicao redundante

Na mesma pagina, o filtro original tinha `!isTransferencia` duplicado
(na condicao externa E no filtro interno). Ja corrigido pela Mudanca 1
que usa `categoriasDoTipo` (sem essa redundancia).

Confirmar que nao ha mais `c.tipo === tipoAtual` no JSX fora do calculo de
`categoriasDoTipo` (eliminar codigo morto se houver).

---

## Fluxo de execucao

```
1. git checkout -b fix/etapa-5-23-categorias-duplicadas-transacao

2. Ler os arquivos listados acima

3. Aplicar Mudancas 1 e 2

4. .\scripts\check-front.ps1 -- verde antes de continuar

5. commit: fix(transacoes): remove duplicatas de categoria no select de nova transacao

6. Atualizar docs/progresso.md (registra sub-etapa 5.23)

7. commit: docs(progresso): registra sub-etapa 5.23
   (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-23.md)

8. /ship -> PR; corrigir apontamentos autonomamente
```

## Estrutura de commits

```
fix(transacoes): remove duplicatas de categoria no select de nova transacao
docs(progresso): registra sub-etapa 5.23
```

## Restricoes

- NAO alterar backend nem migration -- sem constraint UNIQUE por ora.
- NAO esconder categorias sem aviso -- o `temDuplicatas` alerta o usuario.
- Deduplicar por nome dentro do tipo, nao por id (ids sao unicos por definicao).
- Apenas `frontend/src/app/(dashboard)/transacoes/novo/page.tsx` deve ser modificado.
