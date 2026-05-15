package com.laboratorio.financas.usuario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.usuario.domain.EmailJaExisteException;
import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegistrarUsuarioUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrarUsuarioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegistrarUsuarioUseCase(usuarioRepository, passwordEncoder);
    }

    @Test
    void executarEmailNovoCriaCodificaESalva() {
        when(usuarioRepository.existePorEmail("user@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("bcrypt_hash");
        when(usuarioRepository.salvar(any(Usuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = useCase.executar(
                new RegistrarUsuarioUseCase.Comando("user@email.com", "senha123", null));

        assertThat(resultado.getEmail()).isEqualTo("user@email.com");
        assertThat(resultado.getSenhaHash()).isEqualTo("bcrypt_hash");
        assertThat(resultado.isAtivo()).isTrue();
        assertThat(resultado.getName()).isNull();
        verify(usuarioRepository).salvar(any(Usuario.class));
    }

    @Test
    void executarComNameSetaNameNaUsuario() {
        when(usuarioRepository.existePorEmail("user@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("bcrypt_hash");
        when(usuarioRepository.salvar(any(Usuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = useCase.executar(
                new RegistrarUsuarioUseCase.Comando("user@email.com", "senha123", "Fabio"));

        assertThat(resultado.getName()).isEqualTo("Fabio");
        verify(usuarioRepository).salvar(any(Usuario.class));
    }

    @Test
    void executarEmailJaExisteLancaEmailJaExisteException() {
        when(usuarioRepository.existePorEmail("dup@email.com")).thenReturn(true);

        assertThatThrownBy(() ->
                useCase.executar(new RegistrarUsuarioUseCase.Comando("dup@email.com", "senha123", null)))
                .isInstanceOf(EmailJaExisteException.class)
                .hasMessageContaining("dup@email.com");

        verify(usuarioRepository, never()).salvar(any());
        verify(passwordEncoder, never()).encode(anyString());
    }
}
