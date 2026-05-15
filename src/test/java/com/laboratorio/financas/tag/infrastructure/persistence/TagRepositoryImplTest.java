package com.laboratorio.financas.tag.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.tag.domain.Tag;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioEntity;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioJpaRepository;
import java.time.Instant;
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

    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    private UUID userIdA;
    private UUID userIdB;

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
        userIdA = criarUsuarioPersistido();
        userIdB = criarUsuarioPersistido();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
    }

    private UUID criarUsuarioPersistido() {
        UUID id = UUID.randomUUID();
        UsuarioEntity entity = new UsuarioEntity(
                id,
                "teste+" + id + "@test.com",
                "hash_bcrypt",
                true,
                Instant.now(),
                null,
                Instant.now()
        );
        usuarioJpaRepository.save(entity);
        return id;
    }

    @Test
    void savePersisteERetornaTag() {
        Tag tag = new Tag(userIdA, "Essencial", "#FF0000");

        Tag salva = repository.salvar(tag);

        assertThat(salva.getId()).isEqualTo(tag.getId());
        assertThat(salva.getUserId()).isEqualTo(userIdA);
        assertThat(salva.getNome()).isEqualTo("Essencial");
        assertThat(salva.getCor()).isEqualTo("#FF0000");
    }

    @Test
    void findByIdRetornaTagQuandoExiste() {
        Tag tag = new Tag(userIdA, "Lazer", null);
        repository.salvar(tag);

        Optional<Tag> resultado = repository.buscarPorId(tag.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(tag.getId());
    }

    @Test
    void findByIdRetornaVazioQuandoNaoExiste() {
        Optional<Tag> resultado = repository.buscarPorId(UUID.randomUUID());

        assertThat(resultado).isEmpty();
    }

    @Test
    void findByUserIdRetornaApenasTagsDoUsuario() {
        repository.salvar(new Tag(userIdA, "Tag A1", null));
        repository.salvar(new Tag(userIdA, "Tag A2", null));
        repository.salvar(new Tag(userIdB, "Tag B1", null));

        List<Tag> tagsA = repository.buscarPorUserId(userIdA);

        assertThat(tagsA).hasSize(2);
        assertThat(tagsA).allMatch(t -> t.getUserId().equals(userIdA));
    }

    @Test
    void findByUserIdRetornaListaVaziaParaUsuarioSemTags() {
        List<Tag> tags = repository.buscarPorUserId(UUID.randomUUID());

        assertThat(tags).isEmpty();
    }

    @Test
    void findByIdAndUserIdRetornaTagQuandoExisteParaUsuario() {
        Tag tag = new Tag(userIdA, "Viagem", "#0000FF");
        repository.salvar(tag);

        Optional<Tag> resultado = repository.buscarPorIdEUserId(tag.getId(), userIdA);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(tag.getId());
    }

    @Test
    void findByIdAndUserIdRetornaVazioParaOutroUsuario() {
        Tag tag = new Tag(userIdA, "Viagem", null);
        repository.salvar(tag);

        Optional<Tag> resultado = repository.buscarPorIdEUserId(tag.getId(), userIdB);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deleteByIdRemoveTag() {
        Tag tag = new Tag(userIdA, "Remover", null);
        repository.salvar(tag);

        repository.deletar(tag.getId());

        assertThat(repository.buscarPorId(tag.getId())).isEmpty();
    }

    @Test
    void saveAtualizaTagExistente() {
        Tag tag = new Tag(userIdA, "Antiga", "#000000");
        repository.salvar(tag);

        Tag atualizada = new Tag(tag.getId(), userIdA, "Nova", "#FFFFFF", tag.getCriadoEm());
        Tag resultado = repository.salvar(atualizada);

        assertThat(resultado.getNome()).isEqualTo("Nova");
        assertThat(resultado.getCor()).isEqualTo("#FFFFFF");
        assertThat(jpaRepository.count()).isEqualTo(1);
    }
}
