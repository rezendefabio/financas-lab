package com.laboratorio.financas.anexo.application;

import com.laboratorio.financas.anexo.domain.Anexo;
import com.laboratorio.financas.anexo.domain.AnexoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarAnexoUseCase {

    private final AnexoRepository repository;

    public CriarAnexoUseCase(AnexoRepository repository) {
        this.repository = repository;
    }

    public record Comando(String nome) { }

    @Transactional
    public Anexo executar(Comando comando) {
        Anexo novo = new Anexo(comando.nome());
        return repository.salvar(novo);
    }
}
