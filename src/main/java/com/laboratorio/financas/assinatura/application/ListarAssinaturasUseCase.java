package com.laboratorio.financas.assinatura.application;

import com.laboratorio.financas.assinatura.domain.Assinatura;
import com.laboratorio.financas.assinatura.domain.AssinaturaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarAssinaturasUseCase {

    private final AssinaturaRepository repository;

    public ListarAssinaturasUseCase(AssinaturaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Assinatura> executar(UUID userId) {
        return repository.listarPorUserId(userId);
    }
}
