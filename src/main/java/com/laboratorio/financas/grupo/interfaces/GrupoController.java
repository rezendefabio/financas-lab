package com.laboratorio.financas.grupo.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.grupo.application.AtualizarGrupoUseCase;
import com.laboratorio.financas.grupo.application.CriarGrupoUseCase;
import com.laboratorio.financas.grupo.application.DeletarGrupoUseCase;
import com.laboratorio.financas.grupo.application.ListarGruposUseCase;
import com.laboratorio.financas.grupo.domain.Grupo;
import com.laboratorio.financas.shared.infrastructure.web.UserIdResolver;
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
@RequestMapping("/api/grupos")
public class GrupoController {

    private static final Logger LOG = LoggerFactory.getLogger(GrupoController.class);
    private static final String ENTITY_TYPE = "grupo";

    private final CriarGrupoUseCase criarGrupoUseCase;
    private final ListarGruposUseCase listarGruposUseCase;
    private final AtualizarGrupoUseCase atualizarGrupoUseCase;
    private final DeletarGrupoUseCase deletarGrupoUseCase;
    private final UserIdResolver userIdResolver;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public GrupoController(
            CriarGrupoUseCase criarGrupoUseCase,
            ListarGruposUseCase listarGruposUseCase,
            AtualizarGrupoUseCase atualizarGrupoUseCase,
            DeletarGrupoUseCase deletarGrupoUseCase,
            UserIdResolver userIdResolver,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper
    ) {
        this.criarGrupoUseCase = criarGrupoUseCase;
        this.listarGruposUseCase = listarGruposUseCase;
        this.atualizarGrupoUseCase = atualizarGrupoUseCase;
        this.deletarGrupoUseCase = deletarGrupoUseCase;
        this.userIdResolver = userIdResolver;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<GrupoResponse> listar(Authentication authentication) {
        UUID userId = userIdResolver.resolve(authentication);
        List<Grupo> grupos = listarGruposUseCase.executar(userId);
        return grupos.stream().map(GrupoResponse::fromDomain).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GrupoResponse criar(
            @Valid @RequestBody GrupoRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = userIdResolver.resolve(authentication);
        CriarGrupoUseCase.Comando comando =
                new CriarGrupoUseCase.Comando(userId, request.nome(), request.descricao());
        Grupo criado = criarGrupoUseCase.executar(comando);
        GrupoResponse response = GrupoResponse.fromDomain(criado);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, criado.getId(), AuditAction.CREATE,
                userEmail(authentication), screenCode, null, toJson(response)));
        return response;
    }

    @PutMapping("/{id}")
    public GrupoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody GrupoRequest request,
            Authentication authentication
    ) {
        UUID userId = userIdResolver.resolve(authentication);
        AtualizarGrupoUseCase.Comando comando =
                new AtualizarGrupoUseCase.Comando(id, userId, request.nome(), request.descricao());
        Grupo atualizado = atualizarGrupoUseCase.executar(comando);
        return GrupoResponse.fromDomain(atualizado);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = userIdResolver.resolve(authentication);
        deletarGrupoUseCase.executar(id, userId);
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
