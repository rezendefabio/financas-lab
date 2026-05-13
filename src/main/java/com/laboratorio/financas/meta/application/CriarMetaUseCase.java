package com.laboratorio.financas.meta.application;

import com.laboratorio.financas.meta.domain.Meta;
import com.laboratorio.financas.meta.domain.MetaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarMetaUseCase {

    private final MetaRepository metaRepository;

    public CriarMetaUseCase(MetaRepository metaRepository) {
        this.metaRepository = metaRepository;
    }

    public record Comando(String nome) { }

    @Transactional
    public Meta executar(Comando comando) {
        Meta nova = new Meta(comando.nome());
        return metaRepository.salvar(nova);
    }
}
