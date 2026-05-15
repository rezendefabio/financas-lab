package com.laboratorio.financas.usuario.application;

import com.laboratorio.financas.usuario.domain.EmailJaExisteException;
import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RegistrarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrarUsuarioUseCase(UsuarioRepository usuarioRepository,
                                   PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record Comando(String email, String senha, String name) { }

    @Transactional
    public Usuario executar(Comando comando) {
        if (usuarioRepository.existePorEmail(comando.email())) {
            throw new EmailJaExisteException(comando.email());
        }
        String hash = passwordEncoder.encode(comando.senha());
        Usuario usuario = new Usuario(comando.email(), hash);
        if (comando.name() != null) {
            usuario = new Usuario(
                    usuario.getId(),
                    usuario.getEmail(),
                    usuario.getSenhaHash(),
                    usuario.isAtivo(),
                    usuario.getCriadoEm(),
                    comando.name(),
                    usuario.getUpdatedAt()
            );
        }
        return usuarioRepository.salvar(usuario);
    }
}
