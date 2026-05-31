package com.laboratorio.financas.meta.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.meta.application.BuscarMetaPorIdUseCase;
import com.laboratorio.financas.meta.application.CancelarMetaUseCase;
import com.laboratorio.financas.meta.application.CriarMetaUseCase;
import com.laboratorio.financas.meta.application.ListarMetasUseCase;
import com.laboratorio.financas.meta.application.RegistrarDepositoEmMetaUseCase;
import com.laboratorio.financas.meta.domain.Meta;
import com.laboratorio.financas.meta.interfaces.dto.CriarMetaRequest;
import com.laboratorio.financas.meta.interfaces.dto.MetaResponse;
import com.laboratorio.financas.meta.interfaces.dto.RegistrarDepositoRequest;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metas")
public class MetaController {

    private static final Logger LOG = LoggerFactory.getLogger(MetaController.class);
    private static final String ENTITY_TYPE = "meta";

    private final CriarMetaUseCase criarMetaUseCase;
    private final ListarMetasUseCase listarMetasUseCase;
    private final BuscarMetaPorIdUseCase buscarMetaPorIdUseCase;
    private final CancelarMetaUseCase cancelarMetaUseCase;
    private final RegistrarDepositoEmMetaUseCase registrarDepositoUseCase;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;
    private final UsuarioRepository usuarioRepository;

    public MetaController(
            CriarMetaUseCase criarMetaUseCase,
            ListarMetasUseCase listarMetasUseCase,
            BuscarMetaPorIdUseCase buscarMetaPorIdUseCase,
            CancelarMetaUseCase cancelarMetaUseCase,
            RegistrarDepositoEmMetaUseCase registrarDepositoUseCase,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper,
            UsuarioRepository usuarioRepository
    ) {
        this.criarMetaUseCase = criarMetaUseCase;
        this.listarMetasUseCase = listarMetasUseCase;
        this.buscarMetaPorIdUseCase = buscarMetaPorIdUseCase;
        this.cancelarMetaUseCase = cancelarMetaUseCase;
        this.registrarDepositoUseCase = registrarDepositoUseCase;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetaResponse criar(
            @RequestBody @Valid CriarMetaRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        CriarMetaUseCase.Comando comando = new CriarMetaUseCase.Comando(
                resolverUserId(authentication),
                request.nome(),
                request.valorAlvoValor(),
                request.valorAlvoMoeda(),
                request.prazo()
        );
        Meta meta = criarMetaUseCase.executar(comando);
        MetaResponse response = MetaResponse.fromDomain(meta);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, meta.getId(), AuditAction.CREATE,
                userEmail(), screenCode, null, toJson(response)));
        return response;
    }

    @GetMapping
    public List<MetaResponse> listar() {
        return listarMetasUseCase.executar().stream()
                .map(MetaResponse::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    public MetaResponse buscar(@PathVariable UUID id) {
        return MetaResponse.fromDomain(buscarMetaPorIdUseCase.executar(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        Meta antes = buscarMetaPorIdUseCase.executar(id);
        String before = toJson(MetaResponse.fromDomain(antes));
        cancelarMetaUseCase.executar(id);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.DELETE,
                userEmail(), screenCode, before, null));
    }

    @PostMapping("/{id}/depositos")
    public MetaResponse registrarDeposito(
            @PathVariable UUID id,
            @RequestBody @Valid RegistrarDepositoRequest request,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        Meta antes = buscarMetaPorIdUseCase.executar(id);
        String before = toJson(MetaResponse.fromDomain(antes));
        RegistrarDepositoEmMetaUseCase.Comando comando = new RegistrarDepositoEmMetaUseCase.Comando(
                id,
                request.valor(),
                request.moeda()
        );
        Meta meta = registrarDepositoUseCase.executar(comando);
        MetaResponse response = MetaResponse.fromDomain(meta);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.UPDATE,
                userEmail(), screenCode, before, toJson(response)));
        return response;
    }

    private UUID resolverUserId(Authentication authentication) {
        String email = authentication.getName();
        return usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuario autenticado nao encontrado: " + email))
                .getId();
    }

    private String userEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : null;
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
