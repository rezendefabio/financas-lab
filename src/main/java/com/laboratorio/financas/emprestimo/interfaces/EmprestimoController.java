package com.laboratorio.financas.emprestimo.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.emprestimo.application.AtualizarEmprestimoUseCase;
import com.laboratorio.financas.emprestimo.application.CriarEmprestimoUseCase;
import com.laboratorio.financas.emprestimo.application.DeletarEmprestimoUseCase;
import com.laboratorio.financas.emprestimo.application.ListarEmprestimosUseCase;
import com.laboratorio.financas.emprestimo.interfaces.dto.AtualizarEmprestimoRequest;
import com.laboratorio.financas.emprestimo.interfaces.dto.CriarEmprestimoRequest;
import com.laboratorio.financas.emprestimo.interfaces.dto.EmprestimoResponse;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import jakarta.validation.Valid;
import java.util.Currency;
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
@RequestMapping("/api/emprestimos")
public class EmprestimoController {

    private static final Logger LOG = LoggerFactory.getLogger(EmprestimoController.class);
    private static final String ENTITY_TYPE = "emprestimo";

    private final CriarEmprestimoUseCase criarUseCase;
    private final ListarEmprestimosUseCase listarUseCase;
    private final AtualizarEmprestimoUseCase atualizarUseCase;
    private final DeletarEmprestimoUseCase deletarUseCase;
    private final UsuarioRepository usuarioRepository;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public EmprestimoController(CriarEmprestimoUseCase criarUseCase,
                                ListarEmprestimosUseCase listarUseCase,
                                AtualizarEmprestimoUseCase atualizarUseCase,
                                DeletarEmprestimoUseCase deletarUseCase,
                                UsuarioRepository usuarioRepository,
                                AuditPublisher auditPublisher,
                                ObjectMapper objectMapper) {
        this.criarUseCase = criarUseCase;
        this.listarUseCase = listarUseCase;
        this.atualizarUseCase = atualizarUseCase;
        this.deletarUseCase = deletarUseCase;
        this.usuarioRepository = usuarioRepository;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<EmprestimoResponse> listar(Authentication authentication) {
        UUID userId = resolverUserId(authentication);
        return listarUseCase.executar(userId).stream()
                .map(EmprestimoResponse::fromDomain)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmprestimoResponse criar(
            @Valid @RequestBody CriarEmprestimoRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = resolverUserId(authentication);
        Money valor = new Money(request.valor(), Currency.getInstance(request.moeda()));
        CriarEmprestimoUseCase.Comando comando = new CriarEmprestimoUseCase.Comando(
                userId, request.descricao(), request.nomeTerceiro(),
                request.tipo(), valor, request.dataEmprestimo());
        EmprestimoResponse response = EmprestimoResponse.fromDomain(criarUseCase.executar(comando));
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, response.id(), AuditAction.CREATE,
                userEmail(authentication), screenCode, null, toJson(response)));
        return response;
    }

    @PutMapping("/{id}")
    public EmprestimoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarEmprestimoRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = resolverUserId(authentication);
        Money valor = new Money(request.valor(), Currency.getInstance(request.moeda()));
        AtualizarEmprestimoUseCase.Comando comando = new AtualizarEmprestimoUseCase.Comando(
                id, request.descricao(), request.nomeTerceiro(),
                request.tipo(), valor, request.dataEmprestimo(), request.quitado());
        EmprestimoResponse response = EmprestimoResponse.fromDomain(atualizarUseCase.executar(comando));
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.UPDATE,
                userEmail(authentication), screenCode, null, toJson(response)));
        return response;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        resolverUserId(authentication);
        deletarUseCase.executar(id);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.DELETE,
                userEmail(authentication), screenCode, null, null));
    }

    private UUID resolverUserId(Authentication authentication) {
        String email = authentication.getName();
        return usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Usuario autenticado nao encontrado: " + email))
                .getId();
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
