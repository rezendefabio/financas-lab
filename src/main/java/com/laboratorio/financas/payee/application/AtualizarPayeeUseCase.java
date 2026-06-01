package com.laboratorio.financas.payee.application;

import com.laboratorio.financas.payee.application.dto.AtualizarPayeeComando;
import com.laboratorio.financas.payee.domain.Payee;
import com.laboratorio.financas.payee.domain.PayeeNaoEncontradoException;
import com.laboratorio.financas.payee.domain.PayeeRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AtualizarPayeeUseCase {

    private final PayeeRepository repository;

    public AtualizarPayeeUseCase(PayeeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Payee executar(AtualizarPayeeComando comando) {
        Payee existente = repository.findById(comando.id())
                .orElseThrow(() -> new PayeeNaoEncontradoException(comando.id()));

        Payee atualizado = new Payee(
                existente.getId(),
                existente.getUserId(),
                comando.nome() != null ? comando.nome() : existente.getNome(),
                comando.categoriaPadraoId() != null ? comando.categoriaPadraoId() : existente.getCategoriaPadraoId(),
                existente.getCriadoEm(),
                Instant.now()
        );
        return repository.save(atualizado);
    }
}
