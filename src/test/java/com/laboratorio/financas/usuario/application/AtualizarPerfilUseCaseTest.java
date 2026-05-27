package com.laboratorio.financas.usuario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
class AtualizarPerfilUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private AtualizarPerfilUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AtualizarPerfilUseCase(usuarioRepository);
    }

    @Test
    void executarAtualizaNomeESalva() {
        Usuario atual = new Usuario(
                UUID.randomUUID(), "user@email.com", "hash",
                true, Instant.now(), null, null);
        when(usuarioRepository.buscarPorEmail("user@email.com")).thenReturn(Optional.of(atual));
        when(usuarioRepository.atualizar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = useCase.executar(
                new AtualizarPerfilUseCase.Comando("user@email.com", "  Fabio  "));

        assertThat(resultado.getName()).isEqualTo("Fabio");
        assertThat(resultado.getUpdatedAt()).isNotNull();
        assertThat(resultado.getEmail()).isEqualTo("user@email.com");
    }

    @Test
    void executarNomeNuloMantemNomeNulo() {
        Usuario atual = new Usuario(
                UUID.randomUUID(), "user@email.com", "hash",
                true, Instant.now(), "Antigo", Instant.now());
        when(usuarioRepository.buscarPorEmail("user@email.com")).thenReturn(Optional.of(atual));
        when(usuarioRepository.atualizar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = useCase.executar(
                new AtualizarPerfilUseCase.Comando("user@email.com", null));

        assertThat(resultado.getName()).isNull();
    }

    @Test
    void executarUsuarioNaoEncontradoLancaIllegalStateException() {
        when(usuarioRepository.buscarPorEmail("nope@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(
                new AtualizarPerfilUseCase.Comando("nope@email.com", "Nome")))
                .isInstanceOf(IllegalStateException.class);
    }
}
