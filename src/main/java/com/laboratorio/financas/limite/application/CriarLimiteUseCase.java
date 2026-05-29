package com.laboratorio.financas.limite.application;

import com.laboratorio.financas.limite.domain.Limite;
import com.laboratorio.financas.limite.domain.LimiteRepository;
import com.laboratorio.financas.limite.domain.TipoLimite;
import com.laboratorio.financas.shared.domain.Money;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarLimiteUseCase {

    private final LimiteRepository repository;

    public CriarLimiteUseCase(LimiteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Limite executar(Comando comando) {
        Limite limite = new Limite(
                comando.userId(),
                comando.nome(),
                comando.tipo(),
                comando.valor());
        return repository.salvar(limite);
    }

    public record Comando(UUID userId, String nome, TipoLimite tipo, Money valor) { }
}
