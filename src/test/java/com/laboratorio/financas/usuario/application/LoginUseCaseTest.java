package com.laboratorio.financas.usuario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.usuario.domain.CredenciaisInvalidasException;
import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import com.laboratorio.financas.usuario.infrastructure.security.JwtService;
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
    private JwtService jwtService;

    private LoginUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoginUseCase(usuarioRepository, passwordEncoder, jwtService);
    }

    @Test
    void executar_credenciaisValidas_retornaToken() {
        Usuario usuario = new Usuario("user@email.com", "bcrypt_hash");
        when(usuarioRepository.buscarPorEmail("user@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "bcrypt_hash")).thenReturn(true);
        when(jwtService.gerarToken("user@email.com")).thenReturn("jwt_token");
        when(jwtService.getExpirationSeconds()).thenReturn(86400L);

        LoginUseCase.Resultado resultado = useCase.executar("user@email.com", "senha123");

        assertThat(resultado.token()).isEqualTo("jwt_token");
        assertThat(resultado.tipo()).isEqualTo("Bearer");
        assertThat(resultado.expiresIn()).isEqualTo(86400L);
    }

    @Test
    void executar_emailNaoEncontrado_lancaCredenciaisInvalidasException() {
        when(usuarioRepository.buscarPorEmail("nao@existe.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar("nao@existe.com", "qualquersenha"))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Credenciais invalidas");
    }

    @Test
    void executar_senhaErrada_lancaCredenciaisInvalidasException() {
        Usuario usuario = new Usuario("user@email.com", "bcrypt_hash");
        when(usuarioRepository.buscarPorEmail("user@email.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "bcrypt_hash")).thenReturn(false);

        assertThatThrownBy(() -> useCase.executar("user@email.com", "errada"))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Credenciais invalidas");
    }
}
