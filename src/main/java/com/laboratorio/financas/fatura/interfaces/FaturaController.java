package com.laboratorio.financas.fatura.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.fatura.application.AtualizarFaturaUseCase;
import com.laboratorio.financas.fatura.application.BuscarFaturaPorIdUseCase;
import com.laboratorio.financas.fatura.application.CriarFaturaUseCase;
import com.laboratorio.financas.fatura.application.DeletarFaturaUseCase;
import com.laboratorio.financas.fatura.application.ListarFaturasUseCase;
import com.laboratorio.financas.fatura.domain.Fatura;
import com.laboratorio.financas.fatura.interfaces.dto.AtualizarFaturaRequest;
import com.laboratorio.financas.fatura.interfaces.dto.CriarFaturaRequest;
import com.laboratorio.financas.fatura.interfaces.dto.FaturaResponse;
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
@RequestMapping("/api/faturas")
public class FaturaController {

    private static final Logger LOG = LoggerFactory.getLogger(FaturaController.class);
    private static final String ENTITY_TYPE = "fatura";

    private final CriarFaturaUseCase criarFaturaUseCase;
    private final ListarFaturasUseCase listarFaturasUseCase;
    private final BuscarFaturaPorIdUseCase buscarFaturaPorIdUseCase;
    private final AtualizarFaturaUseCase atualizarFaturaUseCase;
    private final DeletarFaturaUseCase deletarFaturaUseCase;
    private final UserIdResolver userIdResolver;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public FaturaController(
            CriarFaturaUseCase criarFaturaUseCase,
            ListarFaturasUseCase listarFaturasUseCase,
            BuscarFaturaPorIdUseCase buscarFaturaPorIdUseCase,
            AtualizarFaturaUseCase atualizarFaturaUseCase,
            DeletarFaturaUseCase deletarFaturaUseCase,
            UserIdResolver userIdResolver,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper
    ) {
        this.criarFaturaUseCase = criarFaturaUseCase;
        this.listarFaturasUseCase = listarFaturasUseCase;
        this.buscarFaturaPorIdUseCase = buscarFaturaPorIdUseCase;
        this.atualizarFaturaUseCase = atualizarFaturaUseCase;
        this.deletarFaturaUseCase = deletarFaturaUseCase;
        this.userIdResolver = userIdResolver;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<FaturaResponse> listar() {
        return listarFaturasUseCase.executar().stream()
                .map(FaturaResponse::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    public FaturaResponse buscar(@PathVariable UUID id) {
        Fatura fatura = buscarFaturaPorIdUseCase.executar(id);
        return FaturaResponse.fromDomain(fatura);
    }

    @PostMapping
    public ResponseEntity<FaturaResponse> criar(
            @Valid @RequestBody CriarFaturaRequest request,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = userIdResolver.resolve();
        CriarFaturaUseCase.Comando comando = new CriarFaturaUseCase.Comando(
                userId,
                request.contaId(),
                request.nome(),
                request.dataVencimento(),
                request.dataFechamento(),
                request.valorTotalValor(),
                request.valorTotalMoeda()
        );
        Fatura criada = criarFaturaUseCase.executar(comando);
        FaturaResponse response = FaturaResponse.fromDomain(criada);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, criada.getId(), AuditAction.CREATE,
                userEmail(), screenCode, null, toJson(response)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public FaturaResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarFaturaRequest request,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode
    ) {
        Fatura antes = buscarFaturaPorIdUseCase.executar(id);
        String before = toJson(FaturaResponse.fromDomain(antes));
        AtualizarFaturaUseCase.Comando comando = new AtualizarFaturaUseCase.Comando(
                id,
                request.nome(),
                request.dataVencimento(),
                request.dataFechamento(),
                request.valorTotalValor(),
                request.valorTotalMoeda()
        );
        Fatura atualizada = atualizarFaturaUseCase.executar(comando);
        FaturaResponse response = FaturaResponse.fromDomain(atualizada);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.UPDATE,
                userEmail(), screenCode, before, toJson(response)));
        return response;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        Fatura antes = buscarFaturaPorIdUseCase.executar(id);
        String before = toJson(FaturaResponse.fromDomain(antes));
        deletarFaturaUseCase.executar(id);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.DELETE,
                userEmail(), screenCode, before, null));
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
