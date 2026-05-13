package com.laboratorio.financas.meta.application;

import com.laboratorio.financas.meta.domain.Meta;
import com.laboratorio.financas.meta.domain.MetaNaoEncontradaException;
import com.laboratorio.financas.meta.domain.MetaRepository;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RegistrarDepositoEmMetaUseCase {

    private final MetaRepository metaRepository;

    public RegistrarDepositoEmMetaUseCase(MetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

    public record Comando(UUID metaId, BigDecimal depositoValor, String depositoMoeda) { }

    @Transactional
    public Meta executar(Comando comando) {
        Meta meta = metaRepository.buscarPorId(comando.metaId())
                .orElseThrow(() -> new MetaNaoEncontradaException(comando.metaId()));
        Money deposito = new Money(comando.depositoValor(), Currency.getInstance(comando.depositoMoeda()));
        meta.registrarDeposito(deposito);
        return metaRepository.atualizar(meta);
    }
}
