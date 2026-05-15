package com.laboratorio.financas.tag.application;

import com.laboratorio.financas.tag.domain.Tag;
import com.laboratorio.financas.tag.domain.TagRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListarTagsUseCase {

    private final TagRepository tagRepository;

    public ListarTagsUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<Tag> executar(UUID userId) {
        return tagRepository.findByUserId(userId);
    }
}
