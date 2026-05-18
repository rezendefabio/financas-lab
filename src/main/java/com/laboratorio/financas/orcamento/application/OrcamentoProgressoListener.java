package com.laboratorio.financas.orcamento.application;

import com.laboratorio.financas.orcamento.domain.OrcamentoRepository;
import com.laboratorio.financas.transacao.domain.TransacaoCriadaEvent;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Escuta {@link TransacaoCriadaEvent} para detectar proativamente quando um
 * orcamento e excedido. Reage de forma desacoplada e assincrona: falha aqui
 * nao afeta a transacao que originou o evento.
 */
@Component
public class OrcamentoProgressoListener {

    private static final Logger LOG = LoggerFactory.getLogger(OrcamentoProgressoListener.class);

    private final CalcularProgressoDoOrcamentoUseCase calcularProgresso;
    private final OrcamentoRepository orcamentoRepository;

    public OrcamentoProgressoListener(CalcularProgressoDoOrcamentoUseCase calcularProgresso,
                                      OrcamentoRepository orcamentoRepository) {
        this.calcularProgresso = calcularProgresso;
        this.orcamentoRepository = orcamentoRepository;
    }

    @Async
    @EventListener
    public void onTransacaoCriada(TransacaoCriadaEvent event) {
        if (!"DESPESA".equals(event.tipo()) || event.categoriaId() == null) {
            return;
        }

        LocalDate mesAno = event.data().withDayOfMonth(1);

        orcamentoRepository.listar().stream()
                .filter(o -> o.isAtivo()
                        && o.getCategoriaId().equals(event.categoriaId())
                        && o.getMesAno().equals(mesAno))
                .forEach(orcamento -> {
                    try {
                        var resultado = calcularProgresso.executar(orcamento.getId());
                        if (resultado.percentualUtilizado()
                                .compareTo(java.math.BigDecimal.valueOf(80)) >= 0) {
                            LOG.warn("Orcamento {} para categoria {} atingiu {}% do limite ({})",
                                    orcamento.getId(),
                                    orcamento.getCategoriaId(),
                                    resultado.percentualUtilizado()
                                            .setScale(1, java.math.RoundingMode.HALF_UP),
                                    resultado.status());
                        }
                    } catch (Exception e) {
                        LOG.error("Erro ao calcular progresso do orcamento {} apos TransacaoCriada",
                                orcamento.getId(), e);
                    }
                });
    }
}
