package com.laboratorio.financas.payee.application;

import com.laboratorio.financas.payee.application.dto.CriarPayeeComando;
import com.laboratorio.financas.payee.domain.Payee;
import com.laboratorio.financas.payee.domain.PayeeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarPayeeUseCase {

    private final PayeeRepository repository;

    public CriarPayeeUseCase(PayeeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Payee executar(CriarPayeeComando comando) {
        Payee payee = new Payee(comando.userId(), comando.nome(), comando.categoriaPadraoId());
        return repository.save(payee);
    }
}
