package com.laboratorio.financas.usuario.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void construtorCriacao_emailNormalizado_setaCamposCorretos() {
        Usuario usuario = new Usuario("  Test@Email.COM  ", "hash123");

        assertThat(usuario.getId()).isNotNull();
        assertThat(usuario.getEmail()).isEqualTo("test@email.com");
        assertThat(usuario.getSenhaHash()).isEqualTo("hash123");
        assertThat(usuario.isAtivo()).isTrue();
        assertThat(usuario.getCriadoEm()).isNotNull();
    }

    @Test
    void construtorCriacao_emailNulo_lancaException() {
        assertThatThrownBy(() -> new Usuario(null, "hash"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("email nao pode ser nulo");
    }

    @Test
    void construtorCriacao_emailVazio_lancaException() {
        assertThatThrownBy(() -> new Usuario("   ", "hash"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email nao pode ser vazio");
    }

    @Test
    void construtorCriacao_senhaHashNula_lancaException() {
        assertThatThrownBy(() -> new Usuario("user@email.com", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("senhaHash nao pode ser nulo");
    }

    @Test
    void construtorReconstrucao_todosOsCampos_setaCorretamente() {
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.now();

        Usuario usuario = new Usuario(id, "user@test.com", "bcrypt_hash", false, criadoEm);

        assertThat(usuario.getId()).isEqualTo(id);
        assertThat(usuario.getEmail()).isEqualTo("user@test.com");
        assertThat(usuario.getSenhaHash()).isEqualTo("bcrypt_hash");
        assertThat(usuario.isAtivo()).isFalse();
        assertThat(usuario.getCriadoEm()).isEqualTo(criadoEm);
    }

    @Test
    void construtorReconstrucao_idNulo_lancaException() {
        assertThatThrownBy(() -> new Usuario(null, "user@test.com", "hash", true, Instant.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("id nao pode ser nulo");
    }

    @Test
    void construtorReconstrucao_criadoEmNulo_lancaException() {
        assertThatThrownBy(() -> new Usuario(UUID.randomUUID(), "user@test.com", "hash", true, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("criadoEm nao pode ser nulo");
    }

    @Test
    void equals_mesmoid_retornaTrue() {
        UUID id = UUID.randomUUID();
        Usuario u1 = new Usuario(id, "a@a.com", "hash1", true, Instant.now());
        Usuario u2 = new Usuario(id, "b@b.com", "hash2", false, Instant.now());

        assertThat(u1).isEqualTo(u2);
    }

    @Test
    void equals_idsdiferentes_retornaFalse() {
        Usuario u1 = new Usuario("a@a.com", "hash1");
        Usuario u2 = new Usuario("b@b.com", "hash2");

        assertThat(u1).isNotEqualTo(u2);
    }

    @Test
    void toString_contem_id_email_ativo() {
        Usuario usuario = new Usuario("x@x.com", "hash");

        assertThat(usuario.toString())
                .contains("Usuario{id=")
                .contains("email=x@x.com")
                .contains("ativo=true");
    }
}
