package com.laboratorio.financas.usuario.domain;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository {

    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorId(UUID id);

    Usuario atualizar(Usuario usuario);

    boolean existePorEmail(String email);
}
