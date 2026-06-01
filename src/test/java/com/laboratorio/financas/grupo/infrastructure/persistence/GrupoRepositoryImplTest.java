package com.laboratorio.financas.grupo.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.grupo.domain.Grupo;
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

class GrupoRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private GrupoRepositoryImpl repository;

    @Autowired
    private GrupoJpaRepository jpaRepository;

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
    void salvarPersisteERetornaGrupo() {
        Grupo grupo = new Grupo(userIdA, "Viagem Europa", "Gastos da viagem");

        Grupo salvo = repository.salvar(grupo);

        assertThat(salvo.getId()).isEqualTo(grupo.getId());
        assertThat(salvo.getUserId()).isEqualTo(userIdA);
        assertThat(salvo.getNome()).isEqualTo("Viagem Europa");
        assertThat(salvo.getDescricao()).isEqualTo("Gastos da viagem");
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test
    void buscarPorIdRetornaGrupoQuandoExiste() {
        Grupo grupo = new Grupo(userIdA, "Casa Nova", null);
        repository.salvar(grupo);

        Optional<Grupo> resultado = repository.buscarPorId(grupo.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(grupo.getId());
    }

    @Test
    void buscarPorIdRetornaVazioQuandoNaoExiste() {
        Optional<Grupo> resultado = repository.buscarPorId(UUID.randomUUID());

        assertThat(resultado).isEmpty();
    }

    @Test
    void listarTodosRetornaGruposDeQualquerUsuario() {
        repository.salvar(new Grupo(userIdA, "Grupo A1", null));
        repository.salvar(new Grupo(userIdA, "Grupo A2", null));
        repository.salvar(new Grupo(userIdB, "Grupo B1", null));

        List<Grupo> todos = repository.listarTodos();

        assertThat(todos).hasSize(3);
        assertThat(todos).anyMatch(g -> g.getUserId().equals(userIdA));
        assertThat(todos).anyMatch(g -> g.getUserId().equals(userIdB));
    }

    @Test
    void listarTodosRetornaListaVaziaQuandoNaoHaGrupos() {
        List<Grupo> grupos = repository.listarTodos();

        assertThat(grupos).isEmpty();
    }

    @Test
    void buscarPorIdRetornaGrupoIndependenteDoAutor() {
        Grupo grupo = new Grupo(userIdA, "Reforma", "Detalhes");
        repository.salvar(grupo);

        Optional<Grupo> resultado = repository.buscarPorId(grupo.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(grupo.getId());
        assertThat(resultado.get().getUserId()).isEqualTo(userIdA);
    }

    @Test
    void deletarRemoveGrupo() {
        Grupo grupo = new Grupo(userIdA, "Remover", null);
        repository.salvar(grupo);

        repository.deletar(grupo.getId());

        assertThat(repository.buscarPorId(grupo.getId())).isEmpty();
    }

    @Test
    void salvarAtualizaGrupoExistente() {
        Grupo grupo = new Grupo(userIdA, "Antigo", "Desc antiga");
        repository.salvar(grupo);

        Grupo atualizado = grupo.atualizar("Novo", "Desc nova");
        Grupo resultado = repository.salvar(atualizado);

        assertThat(resultado.getNome()).isEqualTo("Novo");
        assertThat(resultado.getDescricao()).isEqualTo("Desc nova");
        assertThat(jpaRepository.count()).isEqualTo(1);
    }
}
