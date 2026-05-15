package com.laboratorio.financas.tag.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.tag.domain.Tag;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TagRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private TagRepositoryImpl repository;

    @Autowired
    private TagJpaRepository jpaRepository;

    private static final UUID USER_ID_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID USER_ID_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    @Test
    void savePersisteERetornaTag() {
        Tag tag = new Tag(USER_ID_A, "Essencial", "#FF0000");

        Tag salva = repository.save(tag);

        assertThat(salva.getId()).isEqualTo(tag.getId());
        assertThat(salva.getUserId()).isEqualTo(USER_ID_A);
        assertThat(salva.getNome()).isEqualTo("Essencial");
        assertThat(salva.getCor()).isEqualTo("#FF0000");
    }

    @Test
    void findByIdRetornaTagQuandoExiste() {
        Tag tag = new Tag(USER_ID_A, "Lazer", null);
        repository.save(tag);

        Optional<Tag> resultado = repository.findById(tag.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(tag.getId());
    }

    @Test
    void findByIdRetornaVazioQuandoNaoExiste() {
        Optional<Tag> resultado = repository.findById(UUID.randomUUID());

        assertThat(resultado).isEmpty();
    }

    @Test
    void findByUserIdRetornaApenasTagsDoUsuario() {
        repository.save(new Tag(USER_ID_A, "Tag A1", null));
        repository.save(new Tag(USER_ID_A, "Tag A2", null));
        repository.save(new Tag(USER_ID_B, "Tag B1", null));

        List<Tag> tagsA = repository.findByUserId(USER_ID_A);

        assertThat(tagsA).hasSize(2);
        assertThat(tagsA).allMatch(t -> t.getUserId().equals(USER_ID_A));
    }

    @Test
    void findByUserIdRetornaListaVaziaParaUsuarioSemTags() {
        List<Tag> tags = repository.findByUserId(UUID.randomUUID());

        assertThat(tags).isEmpty();
    }

    @Test
    void findByIdAndUserIdRetornaTagQuandoExisteParaUsuario() {
        Tag tag = new Tag(USER_ID_A, "Viagem", "#0000FF");
        repository.save(tag);

        Optional<Tag> resultado = repository.findByIdAndUserId(tag.getId(), USER_ID_A);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(tag.getId());
    }

    @Test
    void findByIdAndUserIdRetornaVazioParaOutroUsuario() {
        Tag tag = new Tag(USER_ID_A, "Viagem", null);
        repository.save(tag);

        Optional<Tag> resultado = repository.findByIdAndUserId(tag.getId(), USER_ID_B);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deleteByIdRemoveTag() {
        Tag tag = new Tag(USER_ID_A, "Remover", null);
        repository.save(tag);

        repository.deleteById(tag.getId());

        assertThat(repository.findById(tag.getId())).isEmpty();
    }

    @Test
    void saveAtualizaTagExistente() {
        Tag tag = new Tag(USER_ID_A, "Antiga", "#000000");
        repository.save(tag);

        Tag atualizada = new Tag(tag.getId(), USER_ID_A, "Nova", "#FFFFFF", tag.getCriadoEm());
        Tag resultado = repository.save(atualizada);

        assertThat(resultado.getNome()).isEqualTo("Nova");
        assertThat(resultado.getCor()).isEqualTo("#FFFFFF");
        assertThat(jpaRepository.count()).isEqualTo(1);
    }
}
