package com.laboratorio.financas.lembrete.application;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.LembreteRepository;
import com.laboratorio.financas.lembrete.domain.PrioridadeLembrete;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarLembreteUseCase {

    private final LembreteRepository repository;

    public CriarLembreteUseCase(LembreteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Lembrete executar(Comando comando) {
        Lembrete lembrete = new Lembrete(
                comando.userId(),
                comando.titulo(),
                comando.descricao(),
                comando.dataLembrete(),
                comando.prioridade(),
                comando.concluido());
        return repository.salvar(lembrete);
    }

    public record Comando(
            UUID userId,
            String titulo,
            String descricao,
            LocalDate dataLembrete,
            PrioridadeLembrete prioridade,
            boolean concluido) { }
}
