package com.laboratorio.financas.usuario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BuscarPerfilUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private BuscarPerfilUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BuscarPerfilUseCase(usuarioRepository);
    }

    @Test
    void executarUsuarioExistenteRetornaUsuario() {
        Usuario usuario = new Usuario(
                UUID.randomUUID(), "user@email.com", "hash",
                true, Instant.now(), "Fabio", Instant.now());
        when(usuarioRepository.buscarPorEmail("user@email.com")).thenReturn(Optional.of(usuario));

        Usuario resultado = useCase.executar("user@email.com");

        assertThat(resultado.getEmail()).isEqualTo("user@email.com");
        assertThat(resultado.getName()).isEqualTo("Fabio");
    }

    @Test
    void executarUsuarioNaoEncontradoLancaIllegalStateException() {
        when(usuarioRepository.buscarPorEmail("naoexiste@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar("naoexiste@email.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("naoexiste@email.com");
    }
}
