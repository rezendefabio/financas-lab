package com.laboratorio.financas.centrocusto.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.centrocusto.application.AtualizarCentroCustoUseCase;
import com.laboratorio.financas.centrocusto.application.BuscarCentroCustoPorIdUseCase;
import com.laboratorio.financas.centrocusto.application.CriarCentroCustoUseCase;
import com.laboratorio.financas.centrocusto.application.DesativarCentroCustoUseCase;
import com.laboratorio.financas.centrocusto.application.ListarCentrosCustoUseCase;
import com.laboratorio.financas.centrocusto.application.dto.AtualizarCentroCustoComando;
import com.laboratorio.financas.centrocusto.application.dto.CriarCentroCustoComando;
import com.laboratorio.financas.centrocusto.domain.CentroCusto;
import com.laboratorio.financas.centrocusto.interfaces.dto.AtualizarCentroCustoRequest;
import com.laboratorio.financas.centrocusto.interfaces.dto.CentroCustoResponse;
import com.laboratorio.financas.centrocusto.interfaces.dto.CriarCentroCustoRequest;
import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
@RequestMapping("/api/centros-custo")
public class CentroCustoController {

    private static final Logger LOG = LoggerFactory.getLogger(CentroCustoController.class);
    private static final String ENTITY_TYPE = "centro_custo";

    private final CriarCentroCustoUseCase criarCentroCustoUseCase;
    private final ListarCentrosCustoUseCase listarCentrosCustoUseCase;
    private final BuscarCentroCustoPorIdUseCase buscarCentroCustoPorIdUseCase;
    private final AtualizarCentroCustoUseCase atualizarCentroCustoUseCase;
    private final DesativarCentroCustoUseCase desativarCentroCustoUseCase;
    private final UsuarioRepository usuarioRepository;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public CentroCustoController(
            CriarCentroCustoUseCase criarCentroCustoUseCase,
            ListarCentrosCustoUseCase listarCentrosCustoUseCase,
            BuscarCentroCustoPorIdUseCase buscarCentroCustoPorIdUseCase,
            AtualizarCentroCustoUseCase atualizarCentroCustoUseCase,
            DesativarCentroCustoUseCase desativarCentroCustoUseCase,
            UsuarioRepository usuarioRepository,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper
    ) {
        this.criarCentroCustoUseCase = criarCentroCustoUseCase;
        this.listarCentrosCustoUseCase = listarCentrosCustoUseCase;
        this.buscarCentroCustoPorIdUseCase = buscarCentroCustoPorIdUseCase;
        this.atualizarCentroCustoUseCase = atualizarCentroCustoUseCase;
        this.desativarCentroCustoUseCase = desativarCentroCustoUseCase;
        this.usuarioRepository = usuarioRepository;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<CentroCustoResponse> listar() {
        UUID userId = resolverUserId();
        return listarCentrosCustoUseCase.executar(userId).stream()
                .map(CentroCustoResponse::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    public CentroCustoResponse buscar(@PathVariable UUID id) {
        UUID userId = resolverUserId();
        CentroCusto centroCusto = buscarCentroCustoPorIdUseCase.executar(id, userId);
        return CentroCustoResponse.fromDomain(centroCusto);
    }

    @PostMapping
    public ResponseEntity<CentroCustoResponse> criar(
            @Valid @RequestBody CriarCentroCustoRequest request,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = resolverUserId();
        CriarCentroCustoComando comando = new CriarCentroCustoComando(
                userId,
                request.nome(),
                request.descricao()
        );
        CentroCusto criado = criarCentroCustoUseCase.executar(comando);
        CentroCustoResponse response = CentroCustoResponse.fromDomain(criado);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, criado.getId(), AuditAction.CREATE,
                userEmail(), screenCode, null, toJson(response)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public CentroCustoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarCentroCustoRequest request,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode
    ) {
        UUID userId = resolverUserId();
        CentroCusto antes = buscarCentroCustoPorIdUseCase.executar(id, userId);
        String before = toJson(CentroCustoResponse.fromDomain(antes));
        AtualizarCentroCustoComando comando = new AtualizarCentroCustoComando(
                id,
                userId,
                request.nome(),
                request.descricao()
        );
        CentroCusto atualizado = atualizarCentroCustoUseCase.executar(comando);
        CentroCustoResponse response = CentroCustoResponse.fromDomain(atualizado);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.UPDATE,
                userEmail(), screenCode, before, toJson(response)));
        return response;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = resolverUserId();
        CentroCusto antes = buscarCentroCustoPorIdUseCase.executar(id, userId);
        String before = toJson(CentroCustoResponse.fromDomain(antes));
        desativarCentroCustoUseCase.executar(id, userId);
        CentroCusto depois = buscarCentroCustoPorIdUseCase.executar(id, userId);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.UPDATE,
                userEmail(), screenCode, before, toJson(CentroCustoResponse.fromDomain(depois))));
    }

    private UUID resolverUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (String) auth.getPrincipal();
        Usuario usuario = usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado nao encontrado: " + email));
        return usuario.getId();
    }

    private String userEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
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
