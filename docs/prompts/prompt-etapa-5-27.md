# Prompt -- Sub-etapa 5.27: Orcamento frontend (listagem, criacao, detalhe + progresso)

## Contexto

Bounded context `orcamento` tem backend completo (5 endpoints) mas nenhuma tela frontend.
Esta sub-etapa implementa as 3 paginas: listagem, criacao e detalhe com progresso.

Seguir o catalogo de mapeamento de campos (resumido inline abaixo) ao implementar
formularios e listagens -- esta sub-etapa roda em paralelo com a 5.26 que cria o
catalogo formal, entao as regras estao copiadas aqui.

---

## Regras de mapeamento (inline -- obrigatorio seguir)

- `BigDecimal` monetario → `<Input type="number" step="0.01" min="0">` no form; `formatBRL(value)` na exibicao
- `LocalDate` mesAno (representa mes/ano) → `<Input type="month">` no form (retorna `YYYY-MM`); ao enviar, concatenar `-01` para `YYYY-MM-01`; exibir como `MM/YYYY`
- `boolean ativo` → Badge colorido na listagem; nunca editavel pelo usuario
- `UUID categoriaId` (FK) → `<Select>` carregado de `GET /api/categorias`; exibir nome da categoria
- `ValorMonetario { valor, moeda }` (objeto aninhado) → acessar como `item.valorLimite.valor` (NAO achatar); exibir com `formatBRL(item.valorLimite.valor)`
- `percentualUtilizado` (BigDecimal) → componente `<Progress>` + texto `XX%`
- `status` (String: ABAIXO/ATENCAO/ATINGIDO/EXCEDIDO) → Badge com cor semantica:
  - ABAIXO → verde (default)
  - ATENCAO → amarelo (warning/secondary)
  - ATINGIDO → laranja (outline)
  - EXCEDIDO → vermelho (destructive)
- `criadoEm` / `atualizadoEm` (Instant) → exibir read-only com `formatDate()`; nunca editavel
- Listagem de orcamentos → tabela (nao cards). Cards sao para entidades com identidade propria.

---

## API do backend

**Base:** `GET/POST /api/orcamentos`, `GET/DELETE /api/orcamentos/{id}`, `GET /api/orcamentos/{id}/progresso`

**CriarOrcamentoRequest:**
```
categoriaId:       UUID    @NotNull
valorLimiteValor:  BigDecimal @NotNull
valorLimiteMoeda:  String  @NotNull @Size(min=3,max=3)  -- sempre enviar "BRL", oculto do usuario
mesAno:            LocalDate @NotNull                   -- enviar como "YYYY-MM-01"
```

**OrcamentoResponse:**
```
id:           UUID
categoriaId:  UUID
valorLimite:  { valor: number, moeda: string }   -- objeto aninhado, nao plano
mesAno:       string  (ISO date "YYYY-MM-01")
ativo:        boolean
criadoEm:     string  (ISO instant)
atualizadoEm: string  (ISO instant)
```

**ProgressoResponse:**
```
orcamentoId:          UUID
categoriaId:          UUID
mesAno:               string
valorLimite:          { valor: number, moeda: string }
totalGasto:           { valor: number, moeda: string }
percentualUtilizado:  number
status:               string  ("ABAIXO" | "ATENCAO" | "ATINGIDO" | "EXCEDIDO")
```

---

## Arquivos a ler antes de comecar

- `CLAUDE.md` (convencoes frontend, feature-first ADR-013)
- `frontend/src/features/categorias/` (referencia de estrutura feature-first)
- `frontend/src/features/contas/` (referencia de estrutura feature-first)
- `frontend/src/shared/lib/formatters.ts` (formatadores disponiveis)
- `frontend/src/app/(dashboard)/contas/page.tsx` (referencia de tabela)
- `frontend/src/app/(dashboard)/transacoes/novo/page.tsx` (referencia de formulario com Select de categoria)

---

## Estrutura a criar

```
frontend/src/features/orcamentos/
  types/orcamento.ts          -- interfaces Orcamento, Progresso, CriarOrcamentoPayload
  services/orcamento-service.ts -- listar(), buscar(id), criar(payload), desativar(id), progresso(id)
  index.ts                    -- re-exports
frontend/src/app/(dashboard)/orcamentos/
  page.tsx                    -- listagem
  novo/page.tsx               -- criacao
  [id]/page.tsx               -- detalhe + progresso
```

---

## Pagina 1 -- `/orcamentos` (listagem)

Layout: tabela com colunas:
- **Categoria** (nome -- buscar pelo categoriaId via lista de categorias carregada junto)
- **Mes/Ano** (formatar mesAno como "MM/YYYY": ex. "05/2026")
- **Limite** (`formatBRL(orcamento.valorLimite.valor)`)
- **Status** (badge: ativo=verde/default, inativo=cinza)
- **Acoes** (link "Ver" → `/orcamentos/{id}`)

Header com botao "+ Novo Orcamento" → `/orcamentos/novo`.

Se lista vazia: mensagem "Nenhum orcamento cadastrado."

---

## Pagina 2 -- `/orcamentos/novo` (criacao)

Schema Zod (espelhar anotacoes Java):
```typescript
z.object({
  categoriaId: z.string().uuid({ message: 'Selecione uma categoria' }),
  valorLimiteValor: z.coerce.number({ invalid_type_error: 'Informe o valor' }).positive('Valor deve ser positivo'),
  mesAno: z.string().min(1, 'Selecione o mes/ano'),
})
```

Campos do formulario:
1. **Categoria** -- `<Select>` carregado de `GET /api/categorias`, agrupado por tipo (DESPESA primeiro, depois RECEITA)
2. **Valor Limite** -- `<Input type="number" step="0.01" min="0">` com prefixo "R$"
3. **Mes/Ano** -- `<Input type="month">` (retorna "YYYY-MM")

Ao submeter: montar payload com:
```typescript
{
  categoriaId: values.categoriaId,
  valorLimiteValor: values.valorLimiteValor,
  valorLimiteMoeda: 'BRL',   // sempre fixo
  mesAno: values.mesAno + '-01',  // "YYYY-MM" → "YYYY-MM-01"
}
```

Apos sucesso: redirecionar para `/orcamentos`.

---

## Pagina 3 -- `/orcamentos/[id]` (detalhe + progresso)

Duas secoes na pagina:

**Secao 1 -- Dados do orcamento:**
- Categoria: nome (buscar da lista de categorias)
- Mes/Ano: "MM/YYYY"
- Limite: `formatBRL(orcamento.valorLimite.valor)`
- Status: badge ativo/inativo
- Criado em: `formatDate(orcamento.criadoEm)`

**Secao 2 -- Progresso do mes:**
Carregar de `GET /api/orcamentos/{id}/progresso`. Exibir:
- Gasto: `formatBRL(progresso.totalGasto.valor)`
- Limite: `formatBRL(progresso.valorLimite.valor)`
- `<Progress value={Math.min(progresso.percentualUtilizado, 100)} />`
- Percentual: `progresso.percentualUtilizado.toFixed(1) + '%'`
- Status badge (cor semantica conforme regras acima)

**Botao "Desativar"** (apenas se `orcamento.ativo == true`):
- Chama `DELETE /api/orcamentos/{id}`, redireciona para `/orcamentos` apos sucesso.

**Link "Voltar"** → `/orcamentos`.

---

## Navegacao

Adicionar link "Orcamentos" no sidebar (`frontend/src/app/(dashboard)/layout.tsx` ou
componente de sidebar -- ler o arquivo para entender onde adicionar).

---

## Fluxo de execucao

```
1. git checkout -b feat/etapa-5-27-orcamentos-frontend

2. Ler os arquivos listados acima

3. Criar frontend/src/features/orcamentos/ (types, services, index)

4. Criar as 3 paginas em frontend/src/app/(dashboard)/orcamentos/

5. Adicionar link no sidebar

6. Invocar /write-test para cada arquivo criado em features/orcamentos/:
   - /write-test frontend/src/features/orcamentos/services/orcamento-service.ts
   - /write-test frontend/src/features/orcamentos/types/orcamento.ts  (se tiver logica)
   Para pages, gerar testes colocados em __tests__/ ou *.test.tsx ao lado.

7. .\scripts\check-front.ps1 -- verde antes de continuar

8. commit: feat(orcamentos): implementa paginas de listagem, criacao e detalhe com progresso

9. Atualizar docs/progresso.md (registra sub-etapa 5.27)

10. commit: docs(progresso): registra sub-etapa 5.27
    (inclui docs/progresso.md + docs/prompts/prompt-etapa-5-27.md)

11. /ship -> PR; corrigir apontamentos autonomamente
```

## Estrutura de commits

```
feat(orcamentos): implementa paginas de listagem, criacao e detalhe com progresso
docs(progresso): registra sub-etapa 5.27
```

## Restricoes

- NAO alterar backend.
- Layout de listagem: TABELA, nao cards.
- `valorLimite` e `totalGasto` sao objetos aninhados -- acessar `.valor` e `.moeda` diretamente.
- Campo `valorLimiteMoeda` NUNCA exposto ao usuario -- fixo como "BRL" no payload.
- Campo `mesAno` usa `<Input type="month">` + concatenacao de "-01" ao enviar.
- Seguir estrutura feature-first (ADR-013): todo codigo de dominio em `src/features/orcamentos/`.
- Fetch apenas via `api-client.ts` (regra B1 do front-reviewer).
