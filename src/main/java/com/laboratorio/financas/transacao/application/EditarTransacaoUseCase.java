package com.laboratorio.financas.transacao.application;

import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.conta.domain.ContaRepository;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoComReferenciaInvalidaException;
import com.laboratorio.financas.transacao.domain.TransacaoNaoEncontradaException;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EditarTransacaoUseCase {

    private final TransacaoRepository transacaoRepository;
    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;

    public EditarTransacaoUseCase(
            TransacaoRepository transacaoRepository,
            ContaRepository contaRepository,
            CategoriaRepository categoriaRepository
    ) {
        this.transacaoRepository = transacaoRepository;
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public Transacao executar(UUID id, CriarTransacaoUseCase.Comando comando) {
        Transacao existente = transacaoRepository.buscarPorId(id)
                .orElseThrow(() -> new TransacaoNaoEncontradaException(id));

        validarReferencias(comando);

        Money valor = new Money(comando.valor(), Currency.getInstance(comando.moeda()));
        Transacao atualizada = new Transacao(
                existente.getId(),
                comando.tipo(),
                valor,
                comando.data(),
                comando.descricao(),
                comando.contaId(),
                comando.categoriaId(),
                existente.getCriadoEm(),
                Instant.now(),
                (comando.userId() != null) ? comando.userId() : existente.getUserId(),
                (comando.status() != null) ? comando.status() : existente.getStatus(),
                existente.getDeletedAt(),
                (comando.payeeId() != null) ? comando.payeeId() : existente.getPayeeId(),
                existente.getTransferGroupId(),
                existente.getTransferPairId(),
                (comando.tagIds() != null) ? comando.tagIds() : existente.getTagIds()
        );

        return transacaoRepository.salvar(atualizada);
    }

    private void validarReferencias(CriarTransacaoUseCase.Comando comando) {
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
