package com.laboratorio.financas.limite.application;

import com.laboratorio.financas.limite.domain.Limite;
import com.laboratorio.financas.limite.domain.LimiteNaoEncontradoException;
import com.laboratorio.financas.limite.domain.LimiteRepository;
import com.laboratorio.financas.limite.domain.TipoLimite;
import com.laboratorio.financas.shared.domain.Money;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AtualizarLimiteUseCase {

    private final LimiteRepository repository;

    public AtualizarLimiteUseCase(LimiteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Limite executar(Comando comando) {
        Limite limite = repository.buscarPorId(comando.id())
                .orElseThrow(() -> new LimiteNaoEncontradoException(comando.id()));
        if (!limite.getUserId().equals(comando.userId())) {
            throw new LimiteNaoEncontradoException(comando.id());
        }
        limite.atualizar(comando.nome(), comando.tipo(), comando.valor());
        return repository.atualizar(limite);
    }

    public record Comando(UUID id, UUID userId, String nome, TipoLimite tipo, Money valor) { }
}
