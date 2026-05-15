package com.laboratorio.financas.usuario.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@AutoConfigureMockMvc
class UsuarioRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private UsuarioJpaRepository jpaRepository;

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    @Test
    void salvarUsuarioValidoPersistERetorna() {
        Usuario usuario = new Usuario("test@email.com", "hash_bcrypt");

        Usuario salvo = repository.salvar(usuario);

        assertThat(salvo.getId()).isEqualTo(usuario.getId());
        assertThat(salvo.getEmail()).isEqualTo("test@email.com");
        assertThat(salvo.getSenhaHash()).isEqualTo("hash_bcrypt");
        assertThat(salvo.isAtivo()).isTrue();
        assertThat(salvo.getName()).isNull();
        assertThat(salvo.getUpdatedAt()).isNotNull();
    }

    @Test
    void salvarUsuarioComNamePersistERetornaName() {
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.now();
        Instant updatedAt = Instant.now();
        Usuario usuario = new Usuario(id, "named@email.com", "hash_bcrypt", true, criadoEm, "Fabio", updatedAt);

        Usuario salvo = repository.salvar(usuario);

        assertThat(salvo.getName()).isEqualTo("Fabio");
        assertThat(salvo.getUpdatedAt()).isNotNull();
    }

    @Test
    void buscarPorEmailUsuarioExisteRetornaOptionalPresente() {
        repository.salvar(new Usuario("find@email.com", "hash"));

        Optional<Usuario> encontrado = repository.buscarPorEmail("find@email.com");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getEmail()).isEqualTo("find@email.com");
    }

    @Test
    void buscarPorEmailUsuarioNaoExisteRetornaEmpty() {
        Optional<Usuario> encontrado = repository.buscarPorEmail("nao@existe.com");

        assertThat(encontrado).isEmpty();
    }

    @Test
    void existePorEmailEmailCadastradoRetornaTrue() {
        repository.salvar(new Usuario("existe@email.com", "hash"));

        assertThat(repository.existePorEmail("existe@email.com")).isTrue();
    }

    @Test
    void existePorEmailEmailNaoCadastradoRetornaFalse() {
        assertThat(repository.existePorEmail("nao@email.com")).isFalse();
    }
}
