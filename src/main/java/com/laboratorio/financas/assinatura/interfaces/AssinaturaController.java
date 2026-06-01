package com.laboratorio.financas.assinatura.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.assinatura.application.AtualizarAssinaturaUseCase;
import com.laboratorio.financas.assinatura.application.BuscarAssinaturaPorIdUseCase;
import com.laboratorio.financas.assinatura.application.CriarAssinaturaUseCase;
import com.laboratorio.financas.assinatura.application.DeletarAssinaturaUseCase;
import com.laboratorio.financas.assinatura.application.ListarAssinaturasUseCase;
import com.laboratorio.financas.assinatura.interfaces.dto.AssinaturaResponse;
import com.laboratorio.financas.assinatura.interfaces.dto.AtualizarAssinaturaRequest;
import com.laboratorio.financas.assinatura.interfaces.dto.CriarAssinaturaRequest;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.shared.infrastructure.web.UserIdResolver;
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
@RequestMapping("/api/assinaturas")
public class AssinaturaController {

    private static final Logger LOG = LoggerFactory.getLogger(AssinaturaController.class);
    private static final String ENTITY_TYPE = "assinatura";

    private final CriarAssinaturaUseCase criarUseCase;
    private final ListarAssinaturasUseCase listarUseCase;
    private final BuscarAssinaturaPorIdUseCase buscarPorIdUseCase;
    private final AtualizarAssinaturaUseCase atualizarUseCase;
    private final DeletarAssinaturaUseCase deletarUseCase;
    private final UserIdResolver userIdResolver;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public AssinaturaController(
            CriarAssinaturaUseCase criarUseCase,
            ListarAssinaturasUseCase listarUseCase,
            BuscarAssinaturaPorIdUseCase buscarPorIdUseCase,
            AtualizarAssinaturaUseCase atualizarUseCase,
            DeletarAssinaturaUseCase deletarUseCase,
            UserIdResolver userIdResolver,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper) {
        this.criarUseCase = criarUseCase;
        this.listarUseCase = listarUseCase;
        this.buscarPorIdUseCase = buscarPorIdUseCase;
        this.atualizarUseCase = atualizarUseCase;
        this.deletarUseCase = deletarUseCase;
        this.userIdResolver = userIdResolver;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<AssinaturaResponse> listar() {
        return listarUseCase.executar().stream()
                .map(AssinaturaResponse::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    public AssinaturaResponse buscar(
            @PathVariable UUID id,
            Authentication authentication) {
        userIdResolver.resolve(authentication);
        return AssinaturaResponse.fromDomain(buscarPorIdUseCase.executar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssinaturaResponse criar(
            @Valid @RequestBody CriarAssinaturaRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = userIdResolver.resolve(authentication);
        Money valorMensal = new Money(request.valorMensal(), Currency.getInstance(request.moeda()));
        CriarAssinaturaUseCase.Comando comando = new CriarAssinaturaUseCase.Comando(
                userId, request.nome(), request.tipo(), valorMensal, request.dataRenovacao());
        AssinaturaResponse response = AssinaturaResponse.fromDomain(criarUseCase.executar(comando));
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, response.id(), AuditAction.CREATE,
                userEmail(authentication), screenCode, null, toJson(response)));
        return response;
    }

    @PutMapping("/{id}")
    public AssinaturaResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarAssinaturaRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        userIdResolver.resolve(authentication);
        Money valorMensal = new Money(request.valorMensal(), Currency.getInstance(request.moeda()));
        AtualizarAssinaturaUseCase.Comando comando = new AtualizarAssinaturaUseCase.Comando(
                id, request.nome(), request.tipo(), valorMensal,
                request.dataRenovacao(), request.ativa());
        AssinaturaResponse response = AssinaturaResponse.fromDomain(atualizarUseCase.executar(comando));
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
        userIdResolver.resolve(authentication);
        deletarUseCase.executar(id);
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
