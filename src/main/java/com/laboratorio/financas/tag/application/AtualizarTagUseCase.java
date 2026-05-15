package com.laboratorio.financas.tag.application;

import com.laboratorio.financas.tag.domain.Tag;
import com.laboratorio.financas.tag.domain.TagNaoEncontradaException;
import com.laboratorio.financas.tag.domain.TagRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AtualizarTagUseCase {

    private final TagRepository tagRepository;

    public AtualizarTagUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public record Comando(UUID id, UUID userId, String nome, String cor) { }

    @Transactional
    public Tag executar(Comando comando) {
        Tag existente = tagRepository.findByIdAndUserId(comando.id(), comando.userId())
                .orElseThrow(() -> new TagNaoEncontradaException(comando.id()));

        String novoNome = (comando.nome() != null) ? comando.nome() : existente.getNome();
        String novaCor = (comando.cor() != null) ? comando.cor() : existente.getCor();

        Tag atualizada = new Tag(existente.getId(), existente.getUserId(), novoNome, novaCor, existente.getCriadoEm());
        return tagRepository.save(atualizada);
    }
}
