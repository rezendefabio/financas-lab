# Blueprint: Construindo uma Fábrica AI-Native

> Documento de referência consolidando a estratégia para sair do uso convencional de Claude Code e construir um modelo de desenvolvimento com agentes autônomos, projetado desde o dia zero para produtividade exponencial.

---

## Sumário

1. [Reframing fundamental](#reframing-fundamental)
2. [Multi-agentes em Claude Code: o que é real](#multi-agentes-em-claude-code-o-que-é-real)
3. [O gap real: modelo de validação](#o-gap-real-modelo-de-validação)
4. [Features que viabilizam autonomia](#features-que-viabilizam-autonomia)
5. [Os três tiers de autonomia](#os-três-tiers-de-autonomia)
6. [O blueprint: 4 camadas em ordem de construção](#o-blueprint-4-camadas-em-ordem-de-construção)
7. [Camada 5 — Runtime de agentes (quando a necessidade chegar)](#camada-5--runtime-de-agentes-quando-a-necessidade-chegar)
8. [Camada 6 — Gestão de orçamento e estratégia híbrida Max + API](#camada-6--gestão-de-orçamento-e-estratégia-híbrida-max--api)
9. [Stack opinionada de partida](#stack-opinionada-de-partida)
10. [Armadilhas comuns](#armadilhas-comuns)
11. [Próximos passos práticos](#próximos-passos-práticos)

---

## Reframing fundamental

**Você não está construindo um software. Você está construindo uma fábrica que produz software.**

O produto final é o software; o investimento é a fábrica. Cada decisão de arquitetura, stack e workflow é otimizada não para "fazer isso uma vez bem feito", mas para "fazer 1.000 mudanças incrementais sem precisar ver cada uma".

A diferença operacional é brutal: na primeira semana você sente que está perdendo tempo porque escreveu 0 linhas de feature. Na quarta semana, o ROI vira exponencial.

A regra que organiza tudo:

> **A autonomia que você consegue dar é limitada pela qualidade da infraestrutura de validação.**

Não importa quantos agentes você configurou; se o CI não pega regressão, você vai voltar a revisar cada commit por segurança.

---

## Multi-agentes em Claude Code: o que é real

### Os três níveis que viram sopa no YouTube

1. **Subagents do Claude Code** — markdown em `.claude/agents/`. Roda dentro de uma sessão do CLI. É o que você usa hoje sem código.
2. **Claude Agent SDK** — biblioteca para construir aplicações com agentes programaticamente. Quando alguém mostra "100 agentes em paralelo", geralmente é isso ou um SaaS construído por cima.
3. **Frameworks externos** (CrewAI, LangGraph, AutoGen) — outra categoria, nem sempre superior.

### Subagents nativos (já rodando, transparentes)

Claude Code tem subagents built-in que rodam invisivelmente:

- **Explore** — agente rápido, read-only, otimizado para buscar e analisar codebase. Mantém resultados de exploração fora do contexto principal.
- **Plan** — usado quando você está em plan mode e o Claude precisa pesquisar o código.
- **general-purpose** — para tarefas multi-step que precisam tanto explorar quanto modificar.

Tem orquestração acontecendo, mas é invisível por design.

### Custom subagents (`.claude/agents/`)

Estrutura básica:

```markdown
---
name: code-reviewer
description: Use proactively after writing code to review for bugs, style and security issues
tools: Read, Grep, Bash
model: sonnet
---

Você é um revisor de código sênior especializado em [domínio].
Quando invocado:
1. Leia os arquivos modificados
2. Verifique padrões de segurança, lógica e estilo
3. Retorne issues estruturadas em formato markdown
```

**O ponto crítico é o campo `description`**: o Claude principal decide quando delegar baseado nele. Description vaga = subagent nunca chamado.

### O contraponto importante

Custom subagents podem ser brittle. Eles fazem **gatekeeping de contexto**: se você cria um `PythonTests` subagent, escondeu todo o contexto de testes do agente principal, que agora não consegue raciocinar holisticamente sobre uma mudança.

Quando subagent vale a pena:
- Tarefas que floodariam o contexto principal com logs/outputs descartáveis
- Workflows que você repete dezenas de vezes com instruções idênticas
- Quando você precisa restringir tools (ex: revisor que só lê, não escreve)

Para o resto, um `CLAUDE.md` bem feito + skills + hooks entrega 80% do ganho.

**Limite prático: 3 a 5 subagents totais.** Mais que isso e sua produtividade pessoal cai.

---

## O gap real: modelo de validação

O que separa o setup do Boris Cherny (criador do Claude Code, faz 150 PRs/dia do celular) do desenvolvedor médio **não é configuração**. É a estrutura de confiança que ele construiu **antes** de soltar os agentes.

| Modelo convencional | Modelo Boris |
|---|---|
| 1 thread, sequencial | N threads paralelas |
| Validação manual em cada commit | Validação em outputs/PRs agregados |
| Plan mode → executa → revisa cada passo | Define objetivo macro → loops e agentes operam → valida resultado |
| Confiança vem dos seus olhos sobre o código | Confiança vem de testes, CI, hooks, agentes que validam outros agentes |

**A grande mudança de mentalidade**: não é "como monto um exército de agentes" — é "qual fronteira de risco aceito para cada tipo de trabalho, e que infraestrutura de validação preciso construir antes de soltar a mão".

---

## Features que viabilizam autonomia

Tudo já disponível no Claude Code atual:

### `/loop [intervalo]`

Skill bundled que cria cron jobs disparando prompts automaticamente. Você passa intervalo e descrição; o Claude converte em expressão cron e agenda. Roda em background enquanto você trabalha em outra coisa.

Casos de uso:
- Babysitter de PRs (auto-rebase, fix CI)
- Healer de flaky tests
- Coletor de feedback (Twitter, issues)
- Polling de deployment status

**Limitação**: roda enquanto a sessão está aberta. Para persistência, use routines.

### Routines

Versão server-side. Roda mesmo com laptop fechado. Define em `.claude/commands/`, dispara via slash command, hook ou agendamento.

Armadilhas conhecidas:
- Cada invocação não tem memória da anterior — escreva `.last-run` em disco se precisar.
- Cadeias profundas (A → B → C → D) acumulam contexto rápido. Limite prático: 3 hops.
- Se uma routine precisa de bash não auto-aprovado, ela trava silenciosamente. Pré-aprove tools.

### `/batch`

Para mudanças paralelas em larga escala — 5 a 30 worktrees simultâneos. Renomear API em 30 arquivos? Não é sessão sequencial; são 30 sessões paralelas.

### `/simplify`

Code review com 3 agentes paralelos.

### Claude Agent SDK

Para refactors massivos, em vez de chat interativo, escreva script `bash` chamando `claude -p "..."` em paralelo. Mais escalável que tentar fazer o agente principal orquestrar dezenas de subagents.

### MCP entre agentes

Comunicação não precisa de protocolo proprietário. GitHub Issues, Linear comments, Slack via MCP. Um agente abre issue, outro pega, comenta resultado. Você lê o histórico humano-readable depois.

---

## Os três tiers de autonomia

### Tier 1 — Autonomia total (loops 24/7, sem revisão de commit)

Tarefas que **não tocam código de produto**:
- Monitor de CI / healer de flaky tests
- Atualizador de dependências
- Pull/rebase automático de branches
- Sumarizador diário de mudanças no codebase
- Watcher de issues abertas
- Coletor/clusterizador de feedback

Implementação: `/loop` ou routines. Risco zero, ganho real de produtividade.

### Tier 2 — Autonomia supervisionada (gate humano no PR, não no commit)

Tarefas em projetos próprios ou sandboxes de cliente:
- Implementação de feature pequena seguindo spec
- Refactors mecânicos
- Escrita de testes para aumentar cobertura
- Atualização de documentação

Pipeline típico: `/feature → architect-reviewer → implementer → test-writer → pr-reviewer → você`

Você revisa o PR final, não cada commit dentro dele.

**Pré-requisito não-negociável**: hooks que rodem testes/lint antes do PR ser apresentado.

### Tier 3 — Controle manual

Mantém plan mode + revisão linha a linha:
- Decisões arquiteturais novas
- Mudanças de schema sensíveis
- Primeiro contato com domínio que você ainda está aprendendo
- Código de cliente em produção sem suíte de testes confiável

Sem culpa. É a postura certa.

---

## O blueprint: 4 camadas em ordem de construção

> A maioria erra começando pelas camadas 3 e 4 (configurar agentes) sem ter as camadas 1 e 2. Resultado: agentes bonitos, codebase frágil, autonomia gera mais retrabalho que produtividade.

### Camada 1 — Infraestrutura de confiança

**Construir na primeira semana, antes de qualquer feature.** Esta é a única camada que não pode ser construída depois.

**Testes em três níveis viáveis desde o dia zero.** Não exaustivo, confiável.
- Unit: lógica não-trivial
- Integration: contratos entre módulos
- E2E: 1-2 cobrindo fluxo principal

Regra: se o CI passar, você confia que está funcional. Se não confia, o CI não é bom o suficiente — o agente não é o problema.

**CI como gate único da verdade.** Lint, format, type-check, testes, build. Tudo passa ou nada merge. O agente roda contra esse mesmo CI antes de propor PR.

**Pre-commit hooks locais espelhando o CI.** Para o agente não gastar 5 ciclos de "push → CI vermelho → fix → push" quando bastava um `make check` local.

**Conventional commits + ADRs.**
- Conventional commits: agentes geram commits melhores com padrão.
- ADRs em `docs/adr/`: forma mais barata de evitar que o agente reconsidere a mesma decisão 50 vezes.

**Feature flags + ambientes separados.** Mudanças autônomas podem ser deployadas sem ativadas. Reversibilidade é o que te deixa dormir.

### Camada 2 — Arquitetura otimizada para agentes

**Stack on-distribution.** Boring tech onde der. Você quer máxima previsibilidade, não originalidade.

**Schema-first em todos os contratos.**
- OpenAPI para HTTP
- Pydantic para Python interno
- Zod para TypeScript
- JSON Schema para fronteiras de processo

Por que importa: agentes funcionam dramaticamente melhor com contratos explícitos do que com convenções implícitas. "O endpoint retorna um user" é ambíguo; um schema é compilável.

**Modularidade com boundaries explícitas.** Cada módulo é uma "unidade de trabalho" delegável. Se uma feature toca 8 módulos, você está pedindo problema. Se toca 1-2, é trivial delegar.

Estrutura padrão sugerida:
```
src/{domain}/
  ├── api.py        # endpoints
  ├── service.py    # lógica de negócio
  ├── repository.py # acesso a dados
  ├── schemas.py    # contratos
  └── tests/
```

**Erros explícitos.** Result types ou exceções tipadas. Não retornar None ambíguo. Quanto mais explícito o caminho de erro, mais o agente acerta tratamento.

**Documentação executável.** README com `make setup`, `make dev`, `make test`, `make ship`. Comandos atômicos que o agente invoca sem decifrar prosa.

### Camada 3 — Configuração do Claude Code

**CLAUDE.md curto e específico** (target: ~10-15KB max).

Vai no CLAUDE.md:
- Como rodar/testar/deployar
- Convenções de código que não dá pra inferir do código
- Armadilhas conhecidas
- Onde mora o quê
- O que NÃO fazer

NÃO vai no CLAUDE.md:
- Tutoriais
- Explicação de tecnologias
- Histórico do projeto

**3 a 5 subagents focados** (não trinta):
- `architect-reviewer` — valida decisões contra ADRs
- `test-writer` — gera testes seguindo padrões do projeto
- `migration-runner` — workflow repetitivo de schema changes
- `pr-reviewer` — revisão crítica antes do PR chegar a você

**Skills para os 5-10 workflows que você repete:**
- `/ship` — lint + test + build + pr
- `/migrate` — gera migration + atualiza schemas + escreve testes
- `/feature <nome>` — cria estrutura padrão de uma feature
- `/debug-ci` — lê logs do CI e propõe fix
- `/audit` — passa em todos os módulos olhando uma coisa específica

**Hooks como gates automáticos:**
- Pre-commit: lint + format + type-check
- Post-edit: testes do arquivo modificado
- Pre-pr: suíte completa

**MCPs apenas dos que você usa de verdade.** Cada MCP carregado consome tokens. GitHub, Postgres do dev, Linear/Notion do projeto. Não conecte 15 só porque dá.

### Camada 4 — Modelo operacional

Aqui você ativa a fábrica de fato:

1. Ative os três tiers de autonomia conforme o tipo de tarefa
2. Configure 3-5 loops/routines no Tier 1 desde a primeira semana
3. Use `/batch` ou SDK em bash para paralelismo em larga escala
4. Use MCPs para comunicação entre agentes via canais humanos (Issues, Slack)

---

## Camada 5 — Runtime de agentes (quando a necessidade chegar)

> Esta camada é opcional no início e crítica depois. Ative quando os primeiros loops começarem a doer rodando local — tipicamente semana 5-6 do plano, quando você já tem 2-3 routines valendo a pena rodar continuamente.

### O problema que ela resolve

As Camadas 1-4 funcionam rodando local. Mas conforme a fábrica matura, problemas aparecem:

- Você fecha o laptop, loops param
- Notebook a mil ventiladores, dividindo CPU com seu trabalho ativo
- Reiniciou pra atualizar algo, perdeu estado
- Agente autônomo executando comandos no mesmo ambiente onde estão suas credenciais pessoais
- Quer paralelizar 30 worktrees mas a máquina engasga

Runtime dedicado em VPS resolve tudo isso e destrava o nível "Boris Cherny" — gerenciar agentes do celular, de qualquer lugar, sem depender da máquina local.

### Princípio: separar dev de runtime

O erro comum é tentar mover tudo pra remoto. Não faça isso.

**Local continua sendo:**
- Seu ambiente de desenvolvimento ativo
- Claude Code interativo (edição, plan mode, code review)
- Commits manuais quando o tier de autonomia exigir

**VPS vira:**
- Runtime de loops e routines persistentes
- Execução de `/batch` paralelos pesados
- Agentes do Tier 1 rodando 24/7
- Pipeline de Tier 2 quando você não está na frente do laptop

Latência de SSH atrapalha edição interativa; agentes assíncronos não ligam.

### Arquitetura recomendada

```
[Laptop / celular]
  ├─ Claude Code interativo (dev ativo)
  └─ SSH / painel pra gerenciar a VPS

[VPS — agente runtime]
  ├─ Docker / containers isolados por projeto
  ├─ Cron + routines persistentes
  ├─ Worktrees git pra batch parallel
  ├─ Credenciais scoped por projeto (deploy keys, não chaves pessoais)
  └─ Logs centralizados (Logfire / Grafana Cloud)

[Cloud APIs]
  └─ Anthropic API com rate limits e budget alerts
```

**Princípio crítico:** a VPS não tem acesso às suas credenciais pessoais. Cada projeto tem credenciais próprias. Se um agente alucinar e tentar algo destrutivo, blast radius limitado àquele projeto.

### Especificações práticas

Para começar, infraestrutura barata resolve:

- **Hetzner Cloud CX22** (~€4-5/mês) — 2 vCPU, 4GB RAM. Suficiente pra começar.
- **Hetzner CCX13** (~€13/mês) — 2 vCPU dedicados, 8GB RAM. Quando paralelizar pra valer.
- **DigitalOcean Droplet $12/mês** — alternativa, interface mais polida.

Para freelancer no Brasil, Hetzner sai melhor custo-benefício. Latência Alemanha-Brasil (~200ms) é irrelevante para agentes assíncronos.

Stack na VPS:
- Ubuntu 24.04 LTS (boring, on-distribution)
- Docker + Docker Compose
- Tailscale (acesso seguro sem expor portas públicas)
- 1Password CLI ou Doppler (gerenciamento de secrets)
- Logfire ou Grafana Cloud free tier (observabilidade)

### Segurança não-negociável

**Nunca dê acesso a repositórios de cliente sem isolamento por container.** Cliente não pediu pra ter código processado por agente em VPS sua. Mantenha código de cliente local até combinar explicitamente, ou use ambiente do próprio cliente.

**Budget alerts da Anthropic API obrigatórios antes de qualquer routine ir pra produção.** Agente em loop com bug pode queimar muito dinheiro rápido. Configure hard limits.

**Containers por projeto, não compartilhados.** Cada projeto = container próprio = credenciais próprias = blast radius isolado.

**Tailscale ou WireGuard, nunca SSH exposto na porta 22 pública.** VPS com porta 22 aberta vira alvo de bot scan em minutos.

### O que VPS não resolve

- Qualidade de CI/testes ruim — VPS executa agentes ruins com mais eficiência, é tudo
- Decisões arquiteturais sobre seu código
- Necessidade de revisar PRs no Tier 2

Infraestrutura amplifica o que existe. Não conserta o que não existe.

### Quando ativar (e quando não)

**Ative quando:**
- Você tem 2+ loops/routines com valor comprovado rodando local
- Já doeu deixar laptop ligado à noite ou perder estado por reboot
- Começou a paralelizar trabalho pesado e máquina local engasga
- Quer gerenciar agentes do celular sem depender de SSH no notebook

**Não ative quando:**
- Ainda está na Camada 1 ou 2
- Não tem nenhuma routine madura rodando
- Está procurando "produtividade" via infraestrutura em vez de via fundamentos

A regra: **infraestrutura segue necessidade, não antecipa ela.** Os primeiros loops podem rodar local em tmux. Quando começar a doer, aí VPS é o próximo passo natural.

---

## Camada 6 — Gestão de orçamento e estratégia híbrida Max + API

> Esta camada define o modelo financeiro da fábrica. Assinaturas Max são otimizadas para uso humano interativo; automação pesada quer API. Tentar fazer tudo no Max trava você no momento errado; tentar fazer tudo na API queima dinheiro sem necessidade. A estratégia é híbrida.

### O princípio que organiza tudo

> **Max para interativo. API para automação.**

Plano Max é dimensionado para uma pessoa codando em frente ao computador. Quando você liga 5 subagents em paralelo, ou roda um batch noturno em 30 worktrees, você está usando uma assinatura humana para uma carga de máquina. Vai travar.

Quem vai longe nesse modelo separa explicitamente:

- **Trabalho com você na frente** → Max 5x (Claude Code interativo)
- **Trabalho rodando sem você** → API com budget controlado

### Por que Max 5x é o ponto certo de partida

Não é por economia. É por **iteração rápida com baixa fricção**:

- Sem precisar pensar em custo a cada prompt
- Permite explorar workflows sem medo de queimar dinheiro
- Sessão de 5h cobre dia de trabalho normal
- Inclui acesso a Claude.ai pra discussão (como esse chat)
- Mensalidade fixa = previsibilidade financeira

A função do Max nos primeiros meses é **descobrir o que vale automatizar**. Você precisa rodar workflows manualmente várias vezes pra entender padrões antes de codificar em routine. Max paga essa curva de aprendizado sem te punir por experimentar.

### Limites realistas do Max 5x para multi-agent

Os números importantes:

- ~225 mensagens por janela de 5h
- Limite compartilhado entre claude.ai e Claude Code
- Limite semanal adicional acima do janela 5h
- Multi-agent consome 3-7x mais tokens que sessão simples
- Opus 4.7 consome ~1,7x mais que Sonnet, e tem tratamento mais restrito no limite semanal
- Refactor com 5 subagents paralelos pode esgotar janela em ~75 minutos

**Tradução prática:**

| Cenário | Max 5x aguenta? |
|---|---|
| Dev interativo normal + 1-2 loops leves | Confortável |
| Pipeline Tier 2 com 3-5 subagents sequenciais | Funciona com gestão |
| Batch noturno em 20+ worktrees | Trava em 1-2h |
| Dezenas de agentes paralelos 24/7 | Inviável |

### Quando migrar para API direta

Sinais claros de que parte da operação precisa sair do Max:

- Você está esperando reset 2-3x por semana
- Tem routine que você não consegue ativar porque queima a janela
- Quer paralelizar um refactor que justifica `/batch` com 10+ worktrees
- Precisa rodar agente noturno sem competir com seu uso diurno
- A automação é repetitiva e previsível o suficiente pra calcular custo por execução

Quando algum desses bate, esse workload específico move pra API. Não você inteiro — só aquela carga.

### Arquitetura híbrida final

```
[Trabalho interativo - Max 5x]
  ├─ Claude Code no laptop
  ├─ Plan mode + subagents sequenciais leves
  ├─ /loop pra monitoramento simples (CI watcher, daily summary)
  └─ Discussões em claude.ai

[Automação pesada - API direta com budget]
  ├─ Routines server-side em VPS
  ├─ /batch parallel scripting (chamadas claude -p em paralelo)
  ├─ Refactors massivos
  ├─ Agentes noturnos
  └─ Pipelines de Tier 2 quando você não está na frente

[Produção (futuro) - API com budget separado]
  └─ Features de IA dentro do produto que você está construindo
```

Três contas/budgets separados, três finalidades distintas, três níveis de controle de custo.

### Estratégias para esticar o Max 5x

Aplicar todas, não escolher:

**Sonnet por padrão, Opus por exceção.** Configure no CLAUDE.md. Sonnet resolve a maioria das tarefas com 60% do custo. Opus só pra arquitetura complexa ou raciocínio multi-step.

**Subagents com modelo barato por padrão.** `architect-reviewer`, `pr-reviewer`, `test-writer` quase sempre podem rodar Haiku ou Sonnet. Especifique no frontmatter:
```yaml
---
name: pr-reviewer
model: haiku
---
```

**`/clear` entre tarefas não relacionadas.** Contexto velho desperdiça tokens em toda mensagem subsequente. Se mudou de feature, limpa.

**Plan mode antes de implementar.** Plano ruim = retrabalho = tokens dobrados. Plan mode é o investimento de tokens que mais paga.

**MCPs apenas dos que você usa.** Cada MCP carrega definições no contexto. Auditoria mensal: tira o que não usou no mês.

**Spec prompts.** Pedidos vagos ("melhore esse código") fazem o agente varrer arquivos demais. Pedidos específicos com paths e I/O esperado economizam tokens dramaticamente.

**`/usage` como hábito.** Cheque consumo antes de iniciar batch pesado. Identifica padrões de gasto.

**Subagent só quando faz sentido.** Não pra parecer sofisticado. Spawn apenas quando o parent não precisa do raciocínio do trabalho delegado, quando há paralelismo real, ou quando o output do subagent é volumoso e descartável após sumarização.

### Configuração da conta API (preparação)

Mesmo antes de precisar, configure agora:

1. Criar conta em https://console.anthropic.com
2. Adicionar cartão
3. **Budget alert hard** (ex: $50/mês inicial). Hard limit, não warning.
4. Gerar API key separada por projeto (não reutilize chave única)
5. Documentar no `.env.example` mas nunca commitar `.env`
6. Para VPS: secrets via 1Password CLI ou Doppler, nunca hardcode

Tempo total: 15 minutos. Quando precisar de overflow, está pronto. Sem isso, você vai estar travado num momento crítico configurando billing.

### Modelo de custo realista

Pra calibrar expectativa, ordem de grandeza típica:

- **Conta Max 5x:** $100/mês fixo
- **Overflow API moderado** (pipelines Tier 2 ocasionais via API): $20-50/mês
- **Automação pesada** (batches, refactors massivos via API): $50-150/mês
- **Total mensal típico de um operador maduro:** $150-250/mês

Compare com o ROI: se a fábrica te poupa 10h/mês de trabalho que você cobraria $50-100/h, paga sozinha. Em deployments enterprise, o custo médio é cerca de $13 por developer por dia ativo e $150-250 por developer por mês. Sua escala vai ser similar.

### Quando subir para Max 20x

A regra: **só depois que Max 5x for demonstrávelmente insuficiente para uso interativo**. Não para resolver problema de automação — esse problema é da API.

Se você sente que Max 5x te trava 2-3 vezes por semana **codando interativamente** (não rodando batches), o salto pra Max 20x faz sentido. Custo-benefício favorece o 20x: 10x o preço do Pro pra 20x o uso, contra 5x o preço pra 5x o uso. Per-message é metade do preço.

Mas a maioria dos operadores resolve sem subir pra 20x: mantém Max 5x para interativo + API para automação. Mais barato, mais escalável, melhor separação de responsabilidades.

### Roadmap financeiro sugerido

**Mês 1-2 — Apenas Max 5x.** Focado em construir Camadas 1-3. Nenhuma automação pesada ainda.

**Mês 3-4 — Max 5x + conta API configurada (budget $20-50).** Primeiras routines Tier 1 podem começar a usar API se justificarem. Loops simples seguem no Max.

**Mês 5-6 — Max 5x + API ativa ($50-150).** VPS rodando, batches via API, pipeline Tier 2 ocasionalmente roteado pra API quando paralelizando.

**Mês 7+ — Reavaliar.** Pode ser que precise Max 20x. Pode ser que API supra tudo o que Max não cobre. Decisão baseada em padrões reais de uso, não projeção.

---

## Stack opinionada de partida

> Não é a stack "perfeita". É a stack on-distribution onde o Claude tem máxima fluência. Otimização para agente, não pra status no Twitter.

**Backend Python/IA:**
- `uv` (gerenciador) + Python 3.12
- FastAPI + Pydantic v2
- SQLAlchemy 2 + Alembic
- Postgres + Redis
- pytest + ruff + mypy

**Frontend (se precisar):**
- Next.js + TypeScript
- Tailwind
- Zod

**Infraestrutura:**
- Deploy: Fly.io ou Railway no início, AWS quando justificar
- CI: GitHub Actions
- Observabilidade: Logfire ou Sentry
- Feature flags: PostHog ou Unleash self-hosted

---

## Armadilhas comuns

**Configurar muitos subagents.** Limite ~5. Mais que isso, gatekeeping de contexto e produtividade pessoal cai.

**CLAUDE.md virando enciclopédia.** Mantém curto, focado em "como esse projeto funciona", não em "como Python funciona".

**Pular Camada 1.** Tentar autonomia sem CI/testes confiáveis = volta a babá de commit em uma semana.

**Stack exótica por preferência pessoal.** Se você ama Elixir mas o agente erra muito, a fábrica fica lenta. Otimize pelo agente, não pela sua preferência.

**Loops sem `.last-run`.** Routines que re-processam as mesmas coisas todo dia.

**Cadeias profundas de subagents.** Limite ~3 hops. Mais que isso, contexto satura.

**Tools não pré-aprovadas em routines.** Trava silenciosamente em execução noturna.

**Versionar routines mid-run.** `.claude/commands/` é live; agente em execução pega mudança no meio. Stamp data ou versione (`deploy-check-v2.md`).

**Acreditar que setup do Boris escala 1:1 para freelancer multi-cliente.** Ele trabalha em uma codebase que conhece há 1+ ano, com testes maduros. Para projetos variados, gradue autonomia por contexto.

---

## Próximos passos práticos

### Não começar por projeto de cliente

Cliente paga por entrega, não por experimentação. Pegue um SaaS pessoal — qualquer ideia rascunhada e adiada — como **laboratório de fábrica**.

### Construir as 4 camadas conscientemente

Em 4-6 semanas você tem um modelo replicável que pode oferecer aos clientes como diferencial:
> "Monto a infraestrutura completa de desenvolvimento AI-native em X dias."

### Pulo de posicionamento profissional

De: "freelancer Java/Python"
Para: "engenheiro que entrega sistemas projetados para desenvolvimento autônomo desde o dia 1"

Esse perfil é raro hoje e vai ser muito demandado em 12-24 meses, quando empresas perceberem que adotar agentes em codebases legadas é doloroso e quiserem contratar quem sabe começar do jeito certo.

### Sequência de aprendizado sugerida

**Semana 1-2 — Tier 1 em projeto pessoal**
Pegar 3 tarefas que você repete manualmente (atualizar deps, gerar changelog, checar CI) e mover para `/loop`. Sem ambição maior — ganhar intuição de como agentes em background se comportam, qual o custo, onde falham silenciosamente.

**Semana 3-4 — Camada 1 do projeto laboratório**
Iniciar SaaS pessoal aplicando a Camada 1 inteira. Testes, CI, hooks, ADRs, feature flags. Sem features ainda. Documente o setup como playbook.

**Semana 5-6 — Camadas 2 e 3**
Estrutura modular, schema-first, primeiros 3 subagents, primeiras 5 skills. Começar a implementar features dentro do framework.

**Semana 7-8 — Camada 4 (Tier 2)**
Pipeline `feature → architect → impl → test → pr-review` rodando. Você revisa só PRs. Experimentar o desconforto de não revisar cada linha e ver onde isso te queima.

**Semana 9+ — SDK + parallel scripting + Runtime dedicado**
Só depois de ter critério dos passos anteriores, escalar para automações pesadas via SDK. É também o momento de ativar a Camada 5 (VPS), quando os loops locais já doerem o suficiente para justificar a infraestrutura.

---

## Princípios para lembrar

1. Você está construindo a fábrica, não só o produto.
2. Autonomia é proporcional à infraestrutura de validação.
3. Stack on-distribution > stack que você acha bonita.
4. CLAUDE.md curto > CLAUDE.md enciclopédia.
5. 3-5 subagents > 30 subagents.
6. Tier de autonomia se gradua por contexto, não por orgulho.
7. Hooks e CI substituem seus olhos. Construa-os primeiro.
8. O modelo do Boris é replicável, mas não copiável 1:1. Adapte ao seu contexto multi-projeto.

---

## Referências e fontes

- [Anthropic's Boris Cherny: Why Coding Is Solved, and What Comes Next](https://www.youtube.com/watch?v=SlGRN8jh2RI)
- [Claude Code Subagents Docs](https://code.claude.com/docs/en/sub-agents)
- [Claude Agent SDK — Subagents](https://platform.claude.com/docs/en/agent-sdk/subagents)
- [How I Use Every Claude Code Feature — Shrivu Shankar](https://blog.sshh.io/p/how-i-use-every-claude-code-feature)
- [Best Practices for Claude Code Subagents — PubNub](https://www.pubnub.com/blog/best-practices-for-claude-code-sub-agents/)
- [Claude Code Routines: What the Official Docs Do Not Tell You](https://dev.to/whoffagents/claude-code-routines-what-the-official-docs-do-not-tell-you-4peh)
