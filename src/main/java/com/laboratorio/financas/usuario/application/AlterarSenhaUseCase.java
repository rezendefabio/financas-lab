package com.laboratorio.financas.usuario.application;

import com.laboratorio.financas.usuario.domain.SenhaInvalidaException;
import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AlterarSenhaUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AlterarSenhaUseCase(UsuarioRepository usuarioRepository,
                               PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record Comando(String email, String senhaAtual, String novaSenha) { }

    @Transactional
    public void executar(Comando comando) {
        Usuario atual = usuarioRepository.buscarPorEmail(comando.email())
                .orElseThrow(() -> new IllegalStateException("Usuario nao encontrado"));
        if (!passwordEncoder.matches(comando.senhaAtual(), atual.getSenhaHash())) {
            throw new SenhaInvalidaException("Senha atual incorreta");
        }
        String novoHash = passwordEncoder.encode(comando.novaSenha());
        Usuario atualizado = new Usuario(
                atual.getId(),
                atual.getEmail(),
                novoHash,
                atual.isAtivo(),
                atual.getCriadoEm(),
                atual.getName(),
                Instant.now()
        );
        usuarioRepository.atualizar(atualizado);
    }
}
