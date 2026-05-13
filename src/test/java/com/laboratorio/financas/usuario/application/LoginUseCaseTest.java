package com.laboratorio.financas.usuario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.usuario.domain.CredenciaisInvalidasException;
import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private LoginUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoginUseCase(usuarioRepository, passwordEncoder, tokenService);
    }

    @Test
    void executarCredenciaisValidasRetornaToken() {
        Usuario usuario = new Usuario("user@email.com", "bcrypt_hash");
        when(usuarioRepository.buscarPorEmail("user@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "bcrypt_hash")).thenReturn(true);
        when(tokenService.gerarToken("user@email.com")).thenReturn("jwt_token");
        when(tokenService.getExpirationSeconds()).thenReturn(900L);

        LoginUseCase.Resultado resultado = useCase.executar("user@email.com", "senha123");

        assertThat(resultado.token()).isEqualTo("jwt_token");
        assertThat(resultado.tipo()).isEqualTo("Bearer");
        assertThat(resultado.expiresIn()).isEqualTo(900L);
    }

    @Test
    void executarEmailNaoEncontradoLancaCredenciaisInvalidasException() {
        when(usuarioRepository.buscarPorEmail("nao@existe.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar("nao@existe.com", "qualquersenha"))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Credenciais invalidas");
    }

    @Test
    void executarSenhaErradaLancaCredenciaisInvalidasException() {
        Usuario usuario = new Usuario("user@email.com", "bcrypt_hash");
        when(usuarioRepository.buscarPorEmail("user@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "bcrypt_hash")).thenReturn(false);

        assertThatThrownBy(() -> useCase.executar("user@email.com", "errada"))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Credenciais invalidas");
    }
}
