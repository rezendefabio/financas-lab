package com.laboratorio.financas.tag.application;

import com.laboratorio.financas.tag.domain.TagNaoEncontradaException;
import com.laboratorio.financas.tag.domain.TagRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeletarTagUseCase {

    private final TagRepository tagRepository;

    public DeletarTagUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional
    public void executar(UUID id) {
        tagRepository.buscarPorId(id)
                .orElseThrow(() -> new TagNaoEncontradaException(id));
        tagRepository.deletar(id);
    }
}
