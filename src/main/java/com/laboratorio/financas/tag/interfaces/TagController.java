package com.laboratorio.financas.tag.interfaces;

import com.laboratorio.financas.tag.application.AtualizarTagUseCase;
import com.laboratorio.financas.tag.application.CriarTagUseCase;
import com.laboratorio.financas.tag.application.DeletarTagUseCase;
import com.laboratorio.financas.tag.application.ListarTagsUseCase;
import com.laboratorio.financas.tag.domain.Tag;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final CriarTagUseCase criarTagUseCase;
    private final ListarTagsUseCase listarTagsUseCase;
    private final AtualizarTagUseCase atualizarTagUseCase;
    private final DeletarTagUseCase deletarTagUseCase;
    private final UsuarioRepository usuarioRepository;

    public TagController(
            CriarTagUseCase criarTagUseCase,
            ListarTagsUseCase listarTagsUseCase,
            AtualizarTagUseCase atualizarTagUseCase,
            DeletarTagUseCase deletarTagUseCase,
            UsuarioRepository usuarioRepository
    ) {
        this.criarTagUseCase = criarTagUseCase;
        this.listarTagsUseCase = listarTagsUseCase;
        this.atualizarTagUseCase = atualizarTagUseCase;
        this.deletarTagUseCase = deletarTagUseCase;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<TagResponse> listar(Authentication authentication) {
        UUID userId = resolverUserId(authentication);
        List<Tag> tags = listarTagsUseCase.executar(userId);
        return tags.stream().map(TagResponse::fromDomain).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse criar(@Valid @RequestBody TagRequest request, Authentication authentication) {
        UUID userId = resolverUserId(authentication);
        CriarTagUseCase.Comando comando = new CriarTagUseCase.Comando(userId, request.nome(), request.cor());
        Tag criada = criarTagUseCase.executar(comando);
        return TagResponse.fromDomain(criada);
    }

    @PutMapping("/{id}")
    public TagResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody TagRequest request,
            Authentication authentication
    ) {
        UUID userId = resolverUserId(authentication);
        AtualizarTagUseCase.Comando comando = new AtualizarTagUseCase.Comando(id, userId, request.nome(), request.cor());
        Tag atualizada = atualizarTagUseCase.executar(comando);
        return TagResponse.fromDomain(atualizada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id, Authentication authentication) {
        UUID userId = resolverUserId(authentication);
        deletarTagUseCase.executar(id, userId);
    }

    private UUID resolverUserId(Authentication authentication) {
        String email = authentication.getName();
        return usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado nao encontrado: " + email))
                .getId();
    }
}
