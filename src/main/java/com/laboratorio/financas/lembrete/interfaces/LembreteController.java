package com.laboratorio.financas.lembrete.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.lembrete.application.AtualizarLembreteUseCase;
import com.laboratorio.financas.lembrete.application.BuscarLembreteUseCase;
import com.laboratorio.financas.lembrete.application.CriarLembreteUseCase;
import com.laboratorio.financas.lembrete.application.DeletarLembreteUseCase;
import com.laboratorio.financas.lembrete.application.ListarLembretesUseCase;
import com.laboratorio.financas.lembrete.interfaces.dto.AtualizarLembreteRequest;
import com.laboratorio.financas.lembrete.interfaces.dto.CriarLembreteRequest;
import com.laboratorio.financas.lembrete.interfaces.dto.LembreteResponse;
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
@RequestMapping("/api/lembretes")
public class LembreteController {

    private static final Logger LOG = LoggerFactory.getLogger(LembreteController.class);
    private static final String ENTITY_TYPE = "lembrete";

    private final CriarLembreteUseCase criarUseCase;
    private final ListarLembretesUseCase listarUseCase;
    private final BuscarLembreteUseCase buscarUseCase;
    private final AtualizarLembreteUseCase atualizarUseCase;
    private final DeletarLembreteUseCase deletarUseCase;
    private final UserIdResolver userIdResolver;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public LembreteController(CriarLembreteUseCase criarUseCase,
                              ListarLembretesUseCase listarUseCase,
                              BuscarLembreteUseCase buscarUseCase,
                              AtualizarLembreteUseCase atualizarUseCase,
                              DeletarLembreteUseCase deletarUseCase,
                              UserIdResolver userIdResolver,
                              AuditPublisher auditPublisher,
                              ObjectMapper objectMapper) {
        this.criarUseCase = criarUseCase;
        this.listarUseCase = listarUseCase;
        this.buscarUseCase = buscarUseCase;
        this.atualizarUseCase = atualizarUseCase;
        this.deletarUseCase = deletarUseCase;
        this.userIdResolver = userIdResolver;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<LembreteResponse> listar(Authentication authentication) {
        UUID userId = userIdResolver.resolve(authentication);
        return listarUseCase.executar(userId).stream()
                .map(LembreteResponse::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    public LembreteResponse buscarPorId(@PathVariable UUID id,
                                        Authentication authentication) {
        userIdResolver.resolve(authentication);
        return LembreteResponse.fromDomain(buscarUseCase.executar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LembreteResponse criar(
            @Valid @RequestBody CriarLembreteRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = userIdResolver.resolve(authentication);
        CriarLembreteUseCase.Comando comando = new CriarLembreteUseCase.Comando(
                userId,
                request.titulo(),
                request.descricao(),
                request.dataLembrete(),
                request.prioridade()
        );
        LembreteResponse response = LembreteResponse.fromDomain(criarUseCase.executar(comando));
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, response.id(), AuditAction.CREATE,
                userEmail(authentication), screenCode, null, toJson(response)));
        return response;
    }

    @PutMapping("/{id}")
    public LembreteResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarLembreteRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        userIdResolver.resolve(authentication);
        String before = toJson(LembreteResponse.fromDomain(buscarUseCase.executar(id)));
        AtualizarLembreteUseCase.Comando comando = new AtualizarLembreteUseCase.Comando(
                id,
                request.titulo(),
                request.descricao(),
                request.dataLembrete(),
                request.prioridade(),
                Boolean.TRUE.equals(request.concluido())
        );
        LembreteResponse response = LembreteResponse.fromDomain(atualizarUseCase.executar(comando));
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.UPDATE,
                userEmail(authentication), screenCode, before, toJson(response)));
        return response;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        userIdResolver.resolve(authentication);
        String before = toJson(LembreteResponse.fromDomain(buscarUseCase.executar(id)));
        deletarUseCase.executar(id);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.DELETE,
                userEmail(authentication), screenCode, before, null));
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
