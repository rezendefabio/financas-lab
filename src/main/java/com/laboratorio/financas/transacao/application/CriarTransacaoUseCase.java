package com.laboratorio.financas.transacao.application;

import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoComReferenciaInvalidaException;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarTransacaoUseCase {

    private final TransacaoRepository transacaoRepository;
    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;

    public CriarTransacaoUseCase(
            TransacaoRepository transacaoRepository,
            ContaRepository contaRepository,
            CategoriaRepository categoriaRepository
    ) {
        this.transacaoRepository = transacaoRepository;
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public record Comando(
            TipoTransacao tipo,
            BigDecimal valor,
            String moeda,
            LocalDate data,
            String descricao,
            UUID contaId,
            UUID contaDestinoId,
            UUID categoriaId
    ) { }

    @Transactional
    public Transacao executar(Comando comando) {
        validarReferencias(comando);

        Money valor = new Money(comando.valor(), Currency.getInstance(comando.moeda()));
        Transacao nova = new Transacao(
                comando.tipo(),
                valor,
                comando.data(),
                comando.descricao(),
                comando.contaId(),
                comando.contaDestinoId(),
                comando.categoriaId()
        );

        return transacaoRepository.salvar(nova);
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
