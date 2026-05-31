package com.laboratorio.financas.notificacao.application;

import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.meta.domain.Meta;
import com.laboratorio.financas.meta.domain.MetaRepository;
import com.laboratorio.financas.meta.domain.StatusMeta;
import com.laboratorio.financas.notificacao.domain.Notificacao;
import com.laboratorio.financas.notificacao.domain.NotificacaoRepository;
import com.laboratorio.financas.notificacao.domain.TipoNotificacao;
import com.laboratorio.financas.orcamento.application.CalcularProgressoDoOrcamentoUseCase;
import com.laboratorio.financas.orcamento.domain.Orcamento;
import com.laboratorio.financas.orcamento.domain.OrcamentoRepository;
import com.laboratorio.financas.orcamento.domain.StatusProgresso;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconcilia as notificacoes persistidas de um usuario com o estado atual de
 * orcamentos e metas (cross-context -- secao 10.4 de crud-patterns).
 *
 * <p>A logica de geracao espelha o antigo hook frontend {@code useNotificacoes}:
 * orcamento ATENCAO/EXCEDIDO e meta EM_ANDAMENTO vencendo/vencida. Mas agora a
 * lista vira registros persistidos para que o descarte sobreviva entre logins.
 *
 * <p>Algoritmo (idempotente):
 * <ul>
 *   <li>computa o conjunto atual de notificacoes;</li>
 *   <li>upsert por chave natural (cria nova; atualiza texto da existente
 *       preservando o flag {@code descartada});</li>
 *   <li>deleta as persistidas cuja condicao se resolveu (nao estao mais no
 *       conjunto atual) -- se recorrerem depois, viram notificacao nova,
 *       nao-descartada.</li>
 * </ul>
 *
 * <p>Nota: orcamento e meta nao sao multi-tenant (nao tem userId). A reconciliacao
 * le todos os ativos/em-andamento e materializa para o usuario autenticado. O
 * {@code userId} aqui e de quem ve/descarta -- correto para uma base unica com
 * multiplos operadores. Quando orcamento/meta ganharem userId, filtrar aqui.
 */
@Component
public class ReconciliarNotificacoesUseCase {

    private static final int DIAS_VENCENDO = 7;

    private final NotificacaoRepository notificacaoRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final CalcularProgressoDoOrcamentoUseCase calcularProgresso;
    private final MetaRepository metaRepository;
    private final CategoriaRepository categoriaRepository;

    public ReconciliarNotificacoesUseCase(
            NotificacaoRepository notificacaoRepository,
            OrcamentoRepository orcamentoRepository,
            CalcularProgressoDoOrcamentoUseCase calcularProgresso,
            MetaRepository metaRepository,
            CategoriaRepository categoriaRepository) {
        this.notificacaoRepository = notificacaoRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.calcularProgresso = calcularProgresso;
        this.metaRepository = metaRepository;
        this.categoriaRepository = categoriaRepository;
    }

    /** Chave natural de uma notificacao (alem do userId). */
    private record Chave(TipoNotificacao tipo, UUID referenciaId) { }

    /** Notificacao computada do estado atual (ainda nao persistida). */
    private record Calculada(TipoNotificacao tipo, UUID referenciaId, String titulo, String descricao) {
        Chave chave() {
            return new Chave(tipo, referenciaId);
        }
    }

    @Transactional
    public void executar(UUID userId) {
        List<Calculada> atuais = computarAtuais();
        Set<Chave> chavesAtuais = new HashSet<>();
        for (Calculada c : atuais) {
            chavesAtuais.add(c.chave());
        }

        // Upsert: cria nova ou atualiza texto da existente (preserva descartada).
        for (Calculada c : atuais) {
            Optional<Notificacao> existente =
                    notificacaoRepository.buscarPorChaveNatural(userId, c.tipo(), c.referenciaId());
            if (existente.isPresent()) {
                Notificacao n = existente.get();
                n.atualizarTexto(c.titulo(), c.descricao());
                notificacaoRepository.atualizar(n);
            } else {
                notificacaoRepository.salvar(new Notificacao(
                        userId, c.tipo(), c.referenciaId(), c.titulo(), c.descricao()));
            }
        }

        // Condicao resolvida: persistida que nao esta mais no conjunto atual -> deletar.
        for (Notificacao p : notificacaoRepository.listarPorUserId(userId)) {
            if (!chavesAtuais.contains(new Chave(p.getTipo(), p.getReferenciaId()))) {
                notificacaoRepository.deletar(p.getId());
            }
        }
    }

    private List<Calculada> computarAtuais() {
        List<Calculada> lista = new ArrayList<>();
        adicionarDeOrcamentos(lista);
        adicionarDeMetas(lista);
        return lista;
    }

    private void adicionarDeOrcamentos(List<Calculada> lista) {
        for (Orcamento orcamento : orcamentoRepository.listar()) {
            if (!orcamento.isAtivo()) {
                continue;
            }
            CalcularProgressoDoOrcamentoUseCase.Resultado progresso =
                    calcularProgresso.executar(orcamento.getId());
            StatusProgresso status = progresso.status();
            if (status != StatusProgresso.EXCEDIDO && status != StatusProgresso.ATENCAO) {
                continue;
            }
            String nome = categoriaRepository.buscarPorId(orcamento.getCategoriaId())
                    .map(c -> c.getNome())
                    .orElse("Orcamento");
            int percentual = progresso.percentualUtilizado()
                    .setScale(0, RoundingMode.HALF_UP).intValueExact();
            String descricao = nome + ": " + percentual + "% utilizado";
            if (status == StatusProgresso.EXCEDIDO) {
                lista.add(new Calculada(TipoNotificacao.ORCAMENTO_EXCEDIDO,
                        orcamento.getId(), "Orcamento excedido", descricao));
            } else {
                lista.add(new Calculada(TipoNotificacao.ORCAMENTO_ATENCAO,
                        orcamento.getId(), "Orcamento em atencao", descricao));
            }
        }
    }

    private void adicionarDeMetas(List<Calculada> lista) {
        LocalDate hoje = LocalDate.now();
        for (Meta meta : metaRepository.listar()) {
            if (meta.getStatus() != StatusMeta.EM_ANDAMENTO || meta.getPrazo() == null) {
                continue;
            }
            long dias = ChronoUnit.DAYS.between(hoje, meta.getPrazo());
            if (dias < 0) {
                lista.add(new Calculada(TipoNotificacao.META_VENCIDA,
                        meta.getId(), "Meta vencida", meta.getNome() + ": prazo encerrado"));
            } else if (dias <= DIAS_VENCENDO) {
                lista.add(new Calculada(TipoNotificacao.META_VENCENDO,
                        meta.getId(), "Meta vencendo em breve",
                        meta.getNome() + ": " + sufixoPrazo(dias)));
            }
        }
    }

    private static String sufixoPrazo(long dias) {
        if (dias == 0) {
            return "vence hoje";
        }
        if (dias == 1) {
            return "vence em 1 dia";
        }
        return "vence em " + dias + " dias";
    }
}
