package com.laboratorio.financas.payee.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.payee.application.AtualizarPayeeUseCase;
import com.laboratorio.financas.payee.application.CriarPayeeUseCase;
import com.laboratorio.financas.payee.application.DeletarPayeeUseCase;
import com.laboratorio.financas.payee.application.ListarPayeesUseCase;
import com.laboratorio.financas.payee.application.dto.AtualizarPayeeComando;
import com.laboratorio.financas.payee.application.dto.CriarPayeeComando;
import com.laboratorio.financas.payee.domain.Payee;
import com.laboratorio.financas.shared.infrastructure.web.UserIdResolver;
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
@RequestMapping("/api/payees")
public class PayeeController {

    private static final Logger LOG = LoggerFactory.getLogger(PayeeController.class);
    private static final String ENTITY_TYPE = "payee";

    private final CriarPayeeUseCase criarPayeeUseCase;
    private final ListarPayeesUseCase listarPayeesUseCase;
    private final AtualizarPayeeUseCase atualizarPayeeUseCase;
    private final DeletarPayeeUseCase deletarPayeeUseCase;
    private final UserIdResolver userIdResolver;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public PayeeController(
            CriarPayeeUseCase criarPayeeUseCase,
            ListarPayeesUseCase listarPayeesUseCase,
            AtualizarPayeeUseCase atualizarPayeeUseCase,
            DeletarPayeeUseCase deletarPayeeUseCase,
            UserIdResolver userIdResolver,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper
    ) {
        this.criarPayeeUseCase = criarPayeeUseCase;
        this.listarPayeesUseCase = listarPayeesUseCase;
        this.atualizarPayeeUseCase = atualizarPayeeUseCase;
        this.deletarPayeeUseCase = deletarPayeeUseCase;
        this.userIdResolver = userIdResolver;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<PayeeResponse> listar() {
        List<Payee> payees = listarPayeesUseCase.executar();
        return payees.stream().map(PayeeResponse::fromDomain).toList();
    }

    @PostMapping
    public ResponseEntity<PayeeResponse> criar(
            @Valid @RequestBody PayeeRequest request,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = userIdResolver.resolve();
        CriarPayeeComando comando = new CriarPayeeComando(userId, request.nome(), request.categoriaPadraoId());
        Payee criado = criarPayeeUseCase.executar(comando);
        PayeeResponse response = PayeeResponse.fromDomain(criado);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, criado.getId(), AuditAction.CREATE,
                userEmail(), screenCode, null, toJson(response)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public PayeeResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PayeeRequest request,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode
    ) {
        AtualizarPayeeComando comando = new AtualizarPayeeComando(
                id,
                request.nome(),
                request.categoriaPadraoId()
        );
        Payee atualizado = atualizarPayeeUseCase.executar(comando);
        PayeeResponse response = PayeeResponse.fromDomain(atualizado);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.UPDATE,
                userEmail(), screenCode, null, toJson(response)));
        return response;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        deletarPayeeUseCase.executar(id);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.DELETE,
                userEmail(), screenCode, null, null));
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
