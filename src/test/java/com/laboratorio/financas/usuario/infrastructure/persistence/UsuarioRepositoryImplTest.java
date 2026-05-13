package com.laboratorio.financas.usuario.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import java.util.Optional;
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
    void salvar_usuarioValido_persisteERetorna() {
        Usuario usuario = new Usuario("test@email.com", "hash_bcrypt");

        Usuario salvo = repository.salvar(usuario);

        assertThat(salvo.getId()).isEqualTo(usuario.getId());
        assertThat(salvo.getEmail()).isEqualTo("test@email.com");
        assertThat(salvo.getSenhaHash()).isEqualTo("hash_bcrypt");
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test
    void buscarPorEmail_usuarioExiste_retornaOptionalPresente() {
        repository.salvar(new Usuario("find@email.com", "hash"));

        Optional<Usuario> encontrado = repository.buscarPorEmail("find@email.com");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getEmail()).isEqualTo("find@email.com");
    }

    @Test
    void buscarPorEmail_usuarioNaoExiste_retornaEmpty() {
        Optional<Usuario> encontrado = repository.buscarPorEmail("nao@existe.com");

        assertThat(encontrado).isEmpty();
    }

    @Test
    void existePorEmail_emailCadastrado_retornaTrue() {
        repository.salvar(new Usuario("existe@email.com", "hash"));

        assertThat(repository.existePorEmail("existe@email.com")).isTrue();
    }

    @Test
    void existePorEmail_emailNaoCadastrado_retornaFalse() {
        assertThat(repository.existePorEmail("nao@email.com")).isFalse();
    }
}
