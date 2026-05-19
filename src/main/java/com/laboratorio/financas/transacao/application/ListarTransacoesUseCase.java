package com.laboratorio.financas.transacao.application;

import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.OrdenacaoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarTransacoesUseCase {

    private final TransacaoRepository repository;

    public ListarTransacoesUseCase(TransacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<Transacao> executar(
            FiltrosTransacao filtros,
            int page,
            int size,
            OrdenacaoTransacao ordenacao,
            Sort.Direction direcao) {
        return repository.listarComFiltrosOrdenado(filtros, page, size, ordenacao, direcao);
    }
}
