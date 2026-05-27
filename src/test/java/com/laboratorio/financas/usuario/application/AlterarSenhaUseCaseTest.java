package com.laboratorio.financas.usuario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.usuario.domain.SenhaInvalidaException;
import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AlterarSenhaUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AlterarSenhaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AlterarSenhaUseCase(usuarioRepository, passwordEncoder);
    }

    @Test
    void executarSenhaAtualCorretaAtualizaHash() {
        Usuario atual = new Usuario(
                UUID.randomUUID(), "user@email.com", "hash-antigo",
                true, Instant.now(), "Fabio", Instant.now());
        when(usuarioRepository.buscarPorEmail("user@email.com")).thenReturn(Optional.of(atual));
        when(passwordEncoder.matches("senhaAtual", "hash-antigo")).thenReturn(true);
        when(passwordEncoder.encode("novaSenha")).thenReturn("hash-novo");

        useCase.executar(new AlterarSenhaUseCase.Comando("user@email.com", "senhaAtual", "novaSenha"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).atualizar(captor.capture());
        assertThat(captor.getValue().getSenhaHash()).isEqualTo("hash-novo");
        assertThat(captor.getValue().getName()).isEqualTo("Fabio");
    }

    @Test
    void executarSenhaAtualIncorretaLancaSenhaInvalidaException() {
        Usuario atual = new Usuario(
                UUID.randomUUID(), "user@email.com", "hash-antigo",
                true, Instant.now(), null, null);
        when(usuarioRepository.buscarPorEmail("user@email.com")).thenReturn(Optional.of(atual));
        when(passwordEncoder.matches("errada", "hash-antigo")).thenReturn(false);

        assertThatThrownBy(() -> useCase.executar(
                new AlterarSenhaUseCase.Comando("user@email.com", "errada", "novaSenha")))
                .isInstanceOf(SenhaInvalidaException.class)
                .hasMessageContaining("Senha atual incorreta");

        verify(usuarioRepository, never()).atualizar(any());
    }

    @Test
    void executarUsuarioNaoEncontradoLancaIllegalStateException() {
        when(usuarioRepository.buscarPorEmail("nope@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(
                new AlterarSenhaUseCase.Comando("nope@email.com", "a", "b")))
                .isInstanceOf(IllegalStateException.class);
    }
}
