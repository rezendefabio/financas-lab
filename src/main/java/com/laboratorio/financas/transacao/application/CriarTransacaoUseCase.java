package com.laboratorio.financas.transacao.application;

import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.StatusTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoComReferenciaInvalidaException;
import com.laboratorio.financas.transacao.domain.TransacaoCriadaEvent;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarTransacaoUseCase {

    private final TransacaoRepository transacaoRepository;
    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CriarTransacaoUseCase(
            TransacaoRepository transacaoRepository,
            ContaRepository contaRepository,
            CategoriaRepository categoriaRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.transacaoRepository = transacaoRepository;
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
        this.eventPublisher = eventPublisher;
    }

    public record Comando(
            TipoTransacao tipo,
            BigDecimal valor,
            String moeda,
            LocalDate data,
            String descricao,
            UUID contaId,
            UUID contaDestinoId,
            UUID categoriaId,
            UUID userId,
            StatusTransacao status,
            UUID payeeId,
            List<UUID> tagIds
    ) {
        /** Canonical compact constructor -- armazena copia defensiva de tagIds. */
        public Comando {
            tagIds = List.copyOf(tagIds != null ? tagIds : List.of());
        }

        /** Construtor retrocompativel para RECEITA/DESPESA sem campos novos. */
        public Comando(
                TipoTransacao tipo,
                BigDecimal valor,
                String moeda,
                LocalDate data,
                String descricao,
                UUID contaId,
                UUID contaDestinoId,
                UUID categoriaId
        ) {
            this(tipo, valor, moeda, data, descricao, contaId, contaDestinoId, categoriaId,
                    null, StatusTransacao.CLEARED, null, List.of());
        }
    }

    @Transactional
    public Transacao executar(Comando comando) {
        validarReferencias(comando);

        Money valor = new Money(comando.valor(), Currency.getInstance(comando.moeda()));
        StatusTransacao status = (comando.status() != null) ? comando.status() : StatusTransacao.CLEARED;

        if (comando.tipo() == TipoTransacao.TRANSFERENCIA) {
            // Modelo Fase 1: TRANSFERENCIA gera par de transacoes
            if (comando.contaDestinoId() == null) {
                throw new IllegalArgumentException("TRANSFERENCIA exige contaDestinoId");
            }
            Transacao.TransferenciaPar par = Transacao.criarParTransferencia(
                    comando.userId(),
                    valor,
                    comando.contaId(),
                    comando.contaDestinoId(),
                    comando.data(),
                    comando.descricao(),
                    comando.categoriaId()
            );
            Transacao despesa = transacaoRepository.salvar(par.despesa());
            Transacao receita = transacaoRepository.salvar(par.receita());
            publicarEvento(despesa);
            publicarEvento(receita);
            // Retorna a despesa (transacao da conta origem) como resposta primaria
            return despesa;
        }

        Transacao nova = new Transacao(
                comando.tipo(),
                valor,
                comando.data(),
                comando.descricao(),
                comando.contaId(),
                comando.categoriaId(),
                comando.userId(),
                status,
                comando.payeeId(),
                comando.tagIds()
        );

        Transacao salva = transacaoRepository.salvar(nova);
        publicarEvento(salva);
        return salva;
    }

    /** Publica {@link TransacaoCriadaEvent} para a transacao recem-persistida. */
    private void publicarEvento(Transacao transacao) {
        eventPublisher.publishEvent(new TransacaoCriadaEvent(
                transacao.getId(),
                transacao.getCategoriaId(),
                transacao.getContaId(),
                transacao.getUserId(),
                transacao.getValor().valor(),
                transacao.getData(),
                transacao.getTipo().name()
        ));
    }

    private void validarReferencias(Comando comando) {
        if (contaRepository.buscarPorId(comando.contaId()).isEmpty()) {
            throw new TransacaoComReferenciaInvalidaException("conta", comando.contaId());
        }
        if (comando.contaDestinoId() != null
                && contaRepository.buscarPorId(comando.contaDestinoId()).isEmpty()) {
            throw new TransacaoComReferenciaInvalidaException("contaDestino", comando.contaDestinoId());
        }
        if (comando.categoriaId() != null
                && categoriaRepository.buscarPorId(comando.categoriaId()).isEmpty()) {
            throw new TransacaoComReferenciaInvalidaException("categoria", comando.categoriaId());
        }
    }
}
