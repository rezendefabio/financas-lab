package com.laboratorio.financas.transacao.application;

import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.domain.TransacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarTransacoesUseCase {

    private final TransacaoRepository repository;

    public ListarTransacoesUseCase(TransacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<Transacao> executar(FiltrosTransacao filtros, Pageable pageable) {
        return repository.listarComFiltros(filtros, pageable);
    }
}
