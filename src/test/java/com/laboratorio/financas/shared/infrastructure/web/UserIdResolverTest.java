package com.laboratorio.financas.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;

class UserIdResolverTest {

    private UsuarioRepository usuarioRepository;
    private UserIdResolver userIdResolver;

    @BeforeEach
    void setUp() {
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        userIdResolver = new UserIdResolver(usuarioRepository);
    }

    @Test
    void resolveRetornaIdDoUsuarioAutenticado() {
        String email = "fulano@exemplo.com";
        UUID id = UUID.randomUUID();
        Authentication authentication = Mockito.mock(Authentication.class);
        Usuario usuario = Mockito.mock(Usuario.class);
        when(authentication.getName()).thenReturn(email);
        when(usuarioRepository.buscarPorEmail(email)).thenReturn(Optional.of(usuario));
        when(usuario.getId()).thenReturn(id);

        UUID resultado = userIdResolver.resolve(authentication);

        assertThat(resultado).isEqualTo(id);
    }

    @Test
    void resolveLancaQuandoUsuarioNaoEncontrado() {
        String email = "inexistente@exemplo.com";
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);
        when(usuarioRepository.buscarPorEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userIdResolver.resolve(authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(email);
    }
}
