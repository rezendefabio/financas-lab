package com.laboratorio.financas.lembrete.application;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.LembreteRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ListarLembretesUseCase {

    private final LembreteRepository repository;

    public ListarLembretesUseCase(LembreteRepository repository) {
        this.repository = repository;
    }

    public List<Lembrete> executar(UUID userId) {
        return repository.listarPorUserId(userId);
    }
}
