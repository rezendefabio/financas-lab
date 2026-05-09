package com.laboratorio.financas.conta.application;

import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.domain.ContaNaoEncontradaException;
import com.laboratorio.financas.conta.domain.ContaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DesativarContaUseCase {

    private final ContaRepository repository;

    public DesativarContaUseCase(ContaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID id) {
        Conta conta = repository.buscarPorId(id)
                .orElseThrow(() -> new ContaNaoEncontradaException(id));
        Conta desativada = conta.desativar();
        repository.salvar(desativada);
    }
}
