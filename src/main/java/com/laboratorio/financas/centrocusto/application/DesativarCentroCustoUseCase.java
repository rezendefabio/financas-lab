package com.laboratorio.financas.centrocusto.application;

import com.laboratorio.financas.centrocusto.domain.CentroCusto;
import com.laboratorio.financas.centrocusto.domain.CentroCustoNaoEncontradoException;
import com.laboratorio.financas.centrocusto.domain.CentroCustoRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DesativarCentroCustoUseCase {

    private final CentroCustoRepository repository;

    public DesativarCentroCustoUseCase(CentroCustoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id) {
        CentroCusto centroCusto = repository.findById(id)
                .orElseThrow(() -> new CentroCustoNaoEncontradoException(id));
        CentroCusto desativado = centroCusto.desativar();
        repository.save(desativado);
    }
}
