package com.laboratorio.financas.lembrete.interfaces.rest;

import com.laboratorio.financas.lembrete.application.AtualizarLembreteUseCase;
import com.laboratorio.financas.lembrete.application.CriarLembreteUseCase;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LembreteMapper {

    public CriarLembreteUseCase.Comando toCriarComando(UUID userId, LembreteRequest request) {
        return new CriarLembreteUseCase.Comando(
                userId,
                request.titulo(),
                request.descricao(),
                request.dataLembrete(),
                request.prioridade(),
                Boolean.TRUE.equals(request.concluido())
        );
    }

    public AtualizarLembreteUseCase.Comando toAtualizarComando(UUID id, UUID userId,
                                                                LembreteRequest request) {
        return new AtualizarLembreteUseCase.Comando(
                id,
                userId,
                request.titulo(),
                request.descricao(),
                request.dataLembrete(),
                request.prioridade(),
                Boolean.TRUE.equals(request.concluido())
        );
    }
}
