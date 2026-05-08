# Retrospectiva — Camada 1 (Infraestrutura de Confiança)

> Documento de reflexão sobre o que aprendemos construindo a Camada 1.
> Diferente do `progresso.md` (estado atual), este registro é histórico.
> Diferente do `decisoes.md` (escolhas concretas), este captura padrões emergentes e lições mais amplas.

**Período:** 2026-05-06 a 2026-05-08
**Etapas:** 1.1 a 2.8 (15 etapas, contando 2.6.1 e 2.6.2 como sub-etapas)
**PRs:** #1 a #27

---

## O que funcionou

- **Etapas pequenas com prompts cirúrgicos.** Cada etapa com restrições explícitas e mapa exato do que mudar. Quando o escopo era ambíguo, agente desviava — quando era cirúrgico, agente entregava.
- **Validação destrutiva como gate não-negociável.** Encontrou 3 bugs que toda automação validou como verde: Etapa 2.6.1 (`Write-Error` + `exit` sob `Stop`), Etapa 2.6.2 (stderr nativo sob `Stop` vazando stack trace), Etapa 2.8 (`setup.ps1` passando silenciosamente com `.env` ausente). Todos encontrados apenas em validação manual destrutiva — CI nunca teria pegado.
- **Versionamento dos próprios prompts.** Cada `prompt-etapa-X.md` em `docs/`. Permite rastrear o que foi pedido vs o que foi entregue.
- **Procedimento `#XX → número real`** após PR aberto. Padrão internalizado pelo agente desde a 2.2.
- **Tool `Write` nativa do Claude Code > heredoc via shell** para criar arquivos. Lição da 2.6: escape de shell em heredoc pode corromper conteúdo.
- **`AGENTS.md`/`CLAUDE.md` contextual por subdiretório.** Descoberto na 2.7: scaffold do Next.js 16 inclui esses arquivos intencionalmente como mecanismo de proteção contra training data desatualizada. Decisão de remover foi corrigida pelo operador — padrão agora consciente.

## O que foi mais difícil

- **Decisões silenciosas do agente em zona limítrofe.** Recorrente: 2.2 (tentou pom.xml), 2.5 (reduziu fileExtensions), 2.6 (heredoc corrompido), 2.7 (`shadcn --defaults` em vez de interativo). Padrão raiz: agente prefere resolver dentro do escopo a parar e reportar quando solução parece "óbvia". Hooks mecânicos serão fundamentais quando disponíveis.
- **PowerShell + `$ErrorActionPreference = "Stop"` tem armadilhas sérias.** Duas etapas inteiras dedicadas a fix (2.6.1, 2.6.2). Comportamento inconsistente entre sessão direta e subprocess mascarou bugs que CI não capturou.
- **Diagnóstico de bugs em script PowerShell exigiu reprodução isolada no terminal.** Inferir não funcionou em nenhum dos casos. A solução da 2.6.2 só apareceu depois de testar 3 alternativas linha-a-linha no terminal direto.
- **Falha silenciosa em `setup.ps1` com `.env` ausente.** Descoberta na Tarefa 2 desta etapa (clone novo). Containers sobem com credenciais vazias porque Docker Compose interpreta variáveis de ambiente ausentes como strings vazias. `mvn -DskipTests` não testa conexão — setup "conclui com sucesso" mas ambiente é inutilizável para dev real. CI nunca teria detectado porque CI tem secrets injetados.

## Padrões emergentes

- **Estrutura de prompts.** Convergiu para: Contexto → Estado esperado → Tarefas (numeradas) → Restrições → Estrutura de commits → Validação → PR → Pós-criação → Estado esperado ao terminar → O que NÃO fazer. Replicável.
- **Lições por etapa em `progresso.md`.** Seção dupla "Candidatos a hook" + "Lições de ambiente". Permite recuperar contexto rápido em etapas futuras.
- **Validação automática + manual com divisão clara.** Agente faz validação estática (grep, encoding, sintaxe). Operador faz validação destrutiva real (Docker parado, `.env` ausente, clone novo, branch `== main`). Responsabilidades não se sobrepõem.
- **Débito técnico consciente explícito.** Toda decisão temporária documentada no commit, PR body, código e `progresso.md`. Evita que débitos virem surpresas.

## O que faríamos diferente

- **Validar comandos em terminal direto antes de prescrever em prompt.** A 2.6 prescreveu `Write-Error` + `exit 1` sem testar em terminal — bug pegou na validação manual depois. Custo: 2 sub-etapas (2.6.1 e 2.6.2).
- **`setup.ps1` deveria criar `.env` de `.env.example` desde o início.** Ou falhar com mensagem clara se `.env` estiver ausente. A falha silenciosa só foi descoberta no clone de validação da 2.8 — qualquer contribuidor novo teria o mesmo problema sem diagnóstico claro.
- **`AGENTS.md` por subdiretório como prática consciente desde o início.** A decisão de manter o `frontend/AGENTS.md` na 2.7 mostrou um padrão útil que nem estava no roadmap. Vale incorporar explicitamente.
- **Lições "candidatos a hook" desde o dia 1.** Coletadas mas dispersas em cada etapa. A 2.8 consolida em `hooks-pendentes.md`. Se tivéssemos feito isso desde a 1.1, hoje teríamos lista mais coesa.

## Para a Camada 2

Itens explicitamente fora do escopo da Camada 1, agora candidatos:

- Value object `Money` (compartilhado, `shared/domain/`)
- Bounded context `conta` (CRUD + uso de `Money`)
- Bounded context `categoria` (CRUD)
- Spring Security configurado de verdade (auth flow real, não só whitelist)
- Endpoints de auth (signup, login, refresh, logout)
- Primeira feature implementada manualmente do início ao fim, pra servir de referência aos agentes em Tier 2
- Fix do `setup.ps1` para criar/validar `.env` de `.env.example` (débito técnico da Camada 1)

## Métricas

- **Etapas concluídas:** 15 (1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.6.1, 2.6.2, 2.7, 2.8)
- **PRs mergeados:** #XX (preencher após merge do PR final desta etapa)
- **Tempo total (clone novo + setup):** 29 segundos (clone 1.5s + setup ~27s)
- **Bugs encontrados em validação manual destrutiva:** 3 (2.6.1: exit code falso; 2.6.2: stderr nativo sob Stop; 2.8: .env ausente = credenciais vazias)
- **Sub-etapas de fix:** 2 (2.6.1, 2.6.2)
- **Linhas em `decisoes.md`:** 358
- **Linhas em `progresso.md`:** 476

---

## Princípios consolidados (vão pra Camada 2 e além)

1. **Validação manual destrutiva é não-negociável.** Não confiar só em sintaxe + CI. Clone novo, Docker parado, `.env` ausente — esses cenários CI não cobre.
2. **Reproduzir isoladamente antes de mexer no script.** Inferir custa tempo — reprodução linha-a-linha no terminal direto é o atalho real.
3. **Tool nativa > shell heredoc** pra criar arquivos com conteúdo grande.
4. **Cada decisão silenciosa do agente vira lição registrada.** Ao longo do tempo, vira regra ou hook.
5. **Etapa cirúrgica > etapa aberta.** Prompt curto e específico entrega; prompt longo e genérico desvia.
6. **`AGENTS.md`/`CLAUDE.md` contextual por subdiretório.** Mecanismo de proteção contra training data desatualizada — útil em qualquer framework que mude rápido.
7. **Working tree limpo entre etapas.** Cada etapa começa com `git status` limpo, termina com PR mergeado e working tree limpo.
8. **Scripts de setup devem ser à prova de ambiente zero.** `setup.ps1` (e equivalentes) devem validar pré-condições visíveis (`.env` presente, Docker rodando) com mensagem clara antes de executar — não confiar que o ambiente está correto porque "normalmente está".
