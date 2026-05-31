package com.laboratorio.financas.meta.application;

import com.laboratorio.financas.meta.domain.Meta;
import com.laboratorio.financas.meta.domain.MetaRepository;
import com.laboratorio.financas.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarMetaUseCase {

    private final MetaRepository metaRepository;

    public CriarMetaUseCase(MetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

    public record Comando(UUID userId, String nome, BigDecimal valorAlvoValor,
                          String valorAlvoMoeda, LocalDate prazo) { }

    @Transactional
    public Meta executar(Comando comando) {
        Money valorAlvo = new Money(comando.valorAlvoValor(), Currency.getInstance(comando.valorAlvoMoeda()));
        Meta meta = new Meta(comando.userId(), comando.nome(), valorAlvo, comando.prazo());
        return metaRepository.salvar(meta);
    }
}
