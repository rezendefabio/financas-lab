package com.laboratorio.financas.tag.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.shared.infrastructure.web.UserIdResolver;
import com.laboratorio.financas.tag.application.AtualizarTagUseCase;
import com.laboratorio.financas.tag.application.CriarTagUseCase;
import com.laboratorio.financas.tag.application.DeletarTagUseCase;
import com.laboratorio.financas.tag.application.ListarTagsUseCase;
import com.laboratorio.financas.tag.domain.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private static final Logger LOG = LoggerFactory.getLogger(TagController.class);
    private static final String ENTITY_TYPE = "tag";

    private final CriarTagUseCase criarTagUseCase;
    private final ListarTagsUseCase listarTagsUseCase;
    private final AtualizarTagUseCase atualizarTagUseCase;
    private final DeletarTagUseCase deletarTagUseCase;
    private final UserIdResolver userIdResolver;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public TagController(
            CriarTagUseCase criarTagUseCase,
            ListarTagsUseCase listarTagsUseCase,
            AtualizarTagUseCase atualizarTagUseCase,
            DeletarTagUseCase deletarTagUseCase,
            UserIdResolver userIdResolver,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper
    ) {
        this.criarTagUseCase = criarTagUseCase;
        this.listarTagsUseCase = listarTagsUseCase;
        this.atualizarTagUseCase = atualizarTagUseCase;
        this.deletarTagUseCase = deletarTagUseCase;
        this.userIdResolver = userIdResolver;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<TagResponse> listar() {
        List<Tag> tags = listarTagsUseCase.executar();
        return tags.stream().map(TagResponse::fromDomain).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse criar(
            @Valid @RequestBody TagRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = userIdResolver.resolve(authentication);
        CriarTagUseCase.Comando comando = new CriarTagUseCase.Comando(userId, request.nome(), request.cor());
        Tag criada = criarTagUseCase.executar(comando);
        TagResponse response = TagResponse.fromDomain(criada);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, criada.getId(), AuditAction.CREATE,
                userEmail(authentication), screenCode, null, toJson(response)));
        return response;
    }

    @PutMapping("/{id}")
    public TagResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody TagRequest request,
            Authentication authentication
    ) {
        UUID userId = userIdResolver.resolve(authentication);
        AtualizarTagUseCase.Comando comando = new AtualizarTagUseCase.Comando(id, userId, request.nome(), request.cor());
        Tag atualizada = atualizarTagUseCase.executar(comando);
        return TagResponse.fromDomain(atualizada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = userIdResolver.resolve(authentication);
        deletarTagUseCase.executar(id, userId);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.DELETE,
                userEmail(authentication), screenCode, null, null));
    }

    private String userEmail(Authentication authentication) {
        return (authentication != null) ? authentication.getName() : null;
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            LOG.warn("Falha ao serializar payload de audit log para {}", ENTITY_TYPE, ex);
            return null;
        }
    }
}
