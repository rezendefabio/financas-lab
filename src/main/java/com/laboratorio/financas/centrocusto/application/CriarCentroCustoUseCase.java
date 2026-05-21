package com.laboratorio.financas.centrocusto.application;

import com.laboratorio.financas.centrocusto.application.dto.CriarCentroCustoComando;
import com.laboratorio.financas.centrocusto.domain.CentroCusto;
import com.laboratorio.financas.centrocusto.domain.CentroCustoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarCentroCustoUseCase {

    private final CentroCustoRepository repository;

    public CriarCentroCustoUseCase(CentroCustoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CentroCusto executar(CriarCentroCustoComando comando) {
        CentroCusto novo = new CentroCusto(comando.userId(), comando.nome(), comando.descricao());
        return repository.save(novo);
    }
}
