# financas-lab

Projeto-laboratório para validar fábrica AI-native. Backend Java/Spring Boot, frontend Next.js PWA, Postgres.

## Antes de qualquer ação, leia em ordem:
- docs/visao.md — propósito e escopo
- docs/decisoes.md — stack, padrões e regras duras
- docs/adrs.md — contexto histórico das decisões
- docs/progresso.md — estado atual da fábrica e próximas camadas
- docs/roadmap-camada-1.md — plano detalhado se estivermos na Camada 1

## Camada atual

Camada 1 — Infraestrutura de confiança. **NÃO escrever código de feature.** Apenas configurações, esqueletos e infraestrutura de validação. Features só começam na Camada 2.

## Regras duras

- Toda mudança via PR. Sem push direto em main.
- Conventional Commits obrigatórios.
- Antes de propor PR, rode `.\scripts\check.ps1` (a ser criado nas etapas seguintes do roadmap).
