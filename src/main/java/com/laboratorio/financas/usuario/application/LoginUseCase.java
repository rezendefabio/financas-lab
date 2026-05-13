package com.laboratorio.financas.usuario.application;

import com.laboratorio.financas.usuario.domain.CredenciaisInvalidasException;
import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LoginUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public LoginUseCase(UsuarioRepository usuarioRepository,
                        PasswordEncoder passwordEncoder,
                        TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public record Resultado(String token, String tipo, long expiresIn) { }

    @Transactional(readOnly = true)
    public Resultado executar(String email, String senha) {
        Usuario usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(CredenciaisInvalidasException::new);
        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }
        String token = tokenService.gerarToken(email);
        return new Resultado(token, "Bearer", tokenService.getExpirationSeconds());
    }
}
