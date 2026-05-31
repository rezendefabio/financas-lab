package com.laboratorio.financas.assinatura.application;

import com.laboratorio.financas.assinatura.domain.Assinatura;
import com.laboratorio.financas.assinatura.domain.AssinaturaRepository;
import com.laboratorio.financas.assinatura.domain.TipoAssinatura;
import com.laboratorio.financas.shared.domain.Money;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarAssinaturaUseCase {

    private final AssinaturaRepository repository;

    public CriarAssinaturaUseCase(AssinaturaRepository repository) {
        this.repository = repository;
    }

    public record Comando(UUID userId, String nome, TipoAssinatura tipo,
                          Money valorMensal, LocalDate dataRenovacao) { }

    @Transactional
    public Assinatura executar(Comando comando) {
        Assinatura entidade = new Assinatura(
                comando.userId(),
                comando.nome(),
                comando.tipo(),
                comando.valorMensal(),
                comando.dataRenovacao());
        return repository.salvar(entidade);
    }
}
