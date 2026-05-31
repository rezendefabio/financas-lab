package com.laboratorio.financas.assinatura.application;

import com.laboratorio.financas.assinatura.domain.Assinatura;
import com.laboratorio.financas.assinatura.domain.AssinaturaNaoEncontradaException;
import com.laboratorio.financas.assinatura.domain.AssinaturaRepository;
import com.laboratorio.financas.assinatura.domain.TipoAssinatura;
import com.laboratorio.financas.shared.domain.Money;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AtualizarAssinaturaUseCase {

    private final AssinaturaRepository repository;

    public AtualizarAssinaturaUseCase(AssinaturaRepository repository) {
        this.repository = repository;
    }

    public record Comando(UUID id, String nome, TipoAssinatura tipo,
                          Money valorMensal, LocalDate dataRenovacao, boolean ativa) { }

    @Transactional
    public Assinatura executar(Comando comando) {
        Assinatura entidade = repository.buscarPorId(comando.id())
                .orElseThrow(() -> new AssinaturaNaoEncontradaException(comando.id()));
        entidade.atualizar(
                comando.nome(),
                comando.tipo(),
                comando.valorMensal(),
                comando.dataRenovacao(),
                comando.ativa());
        return repository.atualizar(entidade);
    }
}
