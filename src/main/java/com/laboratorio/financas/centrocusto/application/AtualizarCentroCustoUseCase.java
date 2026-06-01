package com.laboratorio.financas.centrocusto.application;

import com.laboratorio.financas.centrocusto.application.dto.AtualizarCentroCustoComando;
import com.laboratorio.financas.centrocusto.domain.CentroCusto;
import com.laboratorio.financas.centrocusto.domain.CentroCustoNaoEncontradoException;
import com.laboratorio.financas.centrocusto.domain.CentroCustoRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AtualizarCentroCustoUseCase {

    private final CentroCustoRepository repository;

    public AtualizarCentroCustoUseCase(CentroCustoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CentroCusto executar(AtualizarCentroCustoComando comando) {
        CentroCusto existente = repository.findById(comando.id())
                .orElseThrow(() -> new CentroCustoNaoEncontradoException(comando.id()));

        CentroCusto atualizado = new CentroCusto(
                existente.getId(),
                existente.getUserId(),
                comando.nome() != null ? comando.nome() : existente.getNome(),
                comando.descricao() != null ? comando.descricao() : existente.getDescricao(),
                existente.isAtivo(),
                existente.getCriadoEm(),
                Instant.now()
        );
        return repository.save(atualizado);
    }
}
