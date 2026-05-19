package com.laboratorio.financas.anotacao.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.anotacao.application.AtualizarAnotacaoUseCase;
import com.laboratorio.financas.anotacao.application.BuscarAnotacaoPorIdUseCase;
import com.laboratorio.financas.anotacao.application.CriarAnotacaoUseCase;
import com.laboratorio.financas.anotacao.application.DeletarAnotacaoUseCase;
import com.laboratorio.financas.anotacao.application.ListarAnotacoesUseCase;
import com.laboratorio.financas.anotacao.domain.Anotacao;
import com.laboratorio.financas.anotacao.interfaces.dto.AnotacaoResponse;
import com.laboratorio.financas.anotacao.interfaces.dto.AtualizarAnotacaoRequest;
import com.laboratorio.financas.anotacao.interfaces.dto.CriarAnotacaoRequest;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Currency;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/anotacoes")
public class AnotacaoController {

    private static final Logger LOG = LoggerFactory.getLogger(AnotacaoController.class);
    private static final String ENTITY_TYPE = "anotacao";

    private final CriarAnotacaoUseCase criarUseCase;
    private final ListarAnotacoesUseCase listarUseCase;
    private final BuscarAnotacaoPorIdUseCase buscarUseCase;
    private final AtualizarAnotacaoUseCase atualizarUseCase;
    private final DeletarAnotacaoUseCase deletarUseCase;
    private final UsuarioRepository usuarioRepository;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public AnotacaoController(
            CriarAnotacaoUseCase criarUseCase,
            ListarAnotacoesUseCase listarUseCase,
            BuscarAnotacaoPorIdUseCase buscarUseCase,
            AtualizarAnotacaoUseCase atualizarUseCase,
            DeletarAnotacaoUseCase deletarUseCase,
            UsuarioRepository usuarioRepository,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper
    ) {
        this.criarUseCase = criarUseCase;
        this.listarUseCase = listarUseCase;
        this.buscarUseCase = buscarUseCase;
        this.atualizarUseCase = atualizarUseCase;
        this.deletarUseCase = deletarUseCase;
        this.usuarioRepository = usuarioRepository;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnotacaoResponse criar(
            @Valid @RequestBody CriarAnotacaoRequest request,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID usuarioId = resolverUserId();
        Money valor = buildMoney(request.valorMontante(), request.valorMoeda());
        Anotacao anotacao = criarUseCase.executar(
                usuarioId,
                request.titulo(),
                request.conteudo(),
                request.tipo(),
                request.prioridade(),
                valor,
                request.dataReferencia()
        );
        AnotacaoResponse response = AnotacaoResponse.fromDomain(anotacao);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, anotacao.getId(), AuditAction.CREATE,
                userEmail(), screenCode, null, toJson(response)));
        return response;
    }

    @GetMapping
    public List<AnotacaoResponse> listar() {
        UUID usuarioId = resolverUserId();
        return listarUseCase.executar(usuarioId).stream()
                .map(AnotacaoResponse::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    public AnotacaoResponse buscar(@PathVariable UUID id) {
        return AnotacaoResponse.fromDomain(buscarUseCase.executar(id));
    }

    @PutMapping("/{id}")
    public AnotacaoResponse atualizar(@PathVariable UUID id,
                                       @Valid @RequestBody AtualizarAnotacaoRequest request,
                                       @RequestHeader(value = "X-Screen-Code", required = false)
                                       String screenCode) {
        Anotacao antes = buscarUseCase.executar(id);
        String before = toJson(AnotacaoResponse.fromDomain(antes));
        Money valor = buildMoney(request.valorMontante(), request.valorMoeda());
        Anotacao anotacao = atualizarUseCase.executar(
                id,
                request.titulo(),
                request.conteudo(),
                request.tipo(),
                request.prioridade(),
                valor,
                request.dataReferencia()
        );
        AnotacaoResponse response = AnotacaoResponse.fromDomain(anotacao);
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
        Anotacao antes = buscarUseCase.executar(id);
        String before = toJson(AnotacaoResponse.fromDomain(antes));
        deletarUseCase.executar(id);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.DELETE,
                userEmail(), screenCode, before, null));
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

    private Money buildMoney(BigDecimal montante, String moeda) {
        if (montante == null) {
            return null;
        }
        String moedaStr = (moeda != null && !moeda.isBlank()) ? moeda : "BRL";
        return new Money(montante, Currency.getInstance(moedaStr));
    }
}
