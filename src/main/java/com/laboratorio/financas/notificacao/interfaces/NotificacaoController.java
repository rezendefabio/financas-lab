package com.laboratorio.financas.notificacao.interfaces;

import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.notificacao.application.DescartarNotificacaoUseCase;
import com.laboratorio.financas.notificacao.application.ListarNotificacoesUseCase;
import com.laboratorio.financas.notificacao.interfaces.dto.NotificacaoResponse;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private static final String ENTITY_TYPE = "notificacao";

    private final ListarNotificacoesUseCase listarUseCase;
    private final DescartarNotificacaoUseCase descartarUseCase;
    private final UsuarioRepository usuarioRepository;
    private final AuditPublisher auditPublisher;

    public NotificacaoController(
            ListarNotificacoesUseCase listarUseCase,
            DescartarNotificacaoUseCase descartarUseCase,
            UsuarioRepository usuarioRepository,
            AuditPublisher auditPublisher) {
        this.listarUseCase = listarUseCase;
        this.descartarUseCase = descartarUseCase;
        this.usuarioRepository = usuarioRepository;
        this.auditPublisher = auditPublisher;
    }

    @GetMapping
    public List<NotificacaoResponse> listar(Authentication authentication) {
        UUID userId = resolverUserId(authentication);
        return listarUseCase.executar(userId).stream()
                .map(NotificacaoResponse::fromDomain)
                .toList();
    }

    @PatchMapping("/{id}/descartar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void descartar(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        resolverUserId(authentication);
        descartarUseCase.executar(id);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.UPDATE,
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
}
