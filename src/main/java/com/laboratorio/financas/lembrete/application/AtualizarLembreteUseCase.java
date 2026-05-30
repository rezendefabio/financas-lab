package com.laboratorio.financas.lembrete.application;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.LembreteNaoEncontradoException;
import com.laboratorio.financas.lembrete.domain.LembreteRepository;
import com.laboratorio.financas.lembrete.domain.Prioridade;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AtualizarLembreteUseCase {

    private final LembreteRepository repository;

    public AtualizarLembreteUseCase(LembreteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Lembrete executar(Comando comando) {
        Lembrete lembrete = repository.buscarPorId(comando.id())
                .orElseThrow(() -> new LembreteNaoEncontradoException(comando.id()));
        lembrete.atualizar(
                comando.titulo(),
                comando.descricao(),
                comando.dataLembrete(),
                comando.prioridade(),
                comando.concluido()
        );
        return repository.atualizar(lembrete);
    }

    public record Comando(
            UUID id,
            String titulo,
            String descricao,
            LocalDate dataLembrete,
            Prioridade prioridade,
            boolean concluido
    ) {
    }
}
