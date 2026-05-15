package com.laboratorio.financas.tag.application;

import com.laboratorio.financas.tag.domain.Tag;
import com.laboratorio.financas.tag.domain.TagRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarTagUseCase {

    private final TagRepository tagRepository;

    public CriarTagUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public record Comando(UUID userId, String nome, String cor) { }

    @Transactional
    public Tag executar(Comando comando) {
        tagRepository.buscarPorUserId(comando.userId()).stream()
                .filter(t -> t.getNome().equalsIgnoreCase(comando.nome().trim()))
                .findFirst()
                .ifPresent(t -> {
                    throw new IllegalArgumentException(
                        "Usuario ja possui tag com o nome: " + comando.nome()
                    );
                });
        Tag tag = new Tag(comando.userId(), comando.nome(), comando.cor());
        return tagRepository.salvar(tag);
    }
}
