package com.laboratorio.financas.transacao.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransacaoRepository {

    Transacao salvar(Transacao transacao);

    Optional<Transacao> buscarPorId(UUID id);

    void deletar(UUID id);

    Page<Transacao> listarComFiltros(FiltrosTransacao filtros, Pageable pageable);

    TotaisTransacaoPorConta calcularTotaisPorConta(UUID contaId);
}
