package com.laboratorio.financas.orcamento.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.orcamento.application.BuscarOrcamentoPorIdUseCase;
import com.laboratorio.financas.orcamento.application.CalcularProgressoDoOrcamentoUseCase;
import com.laboratorio.financas.orcamento.application.CriarOrcamentoUseCase;
import com.laboratorio.financas.orcamento.application.DesativarOrcamentoUseCase;
import com.laboratorio.financas.orcamento.application.ListarOrcamentosUseCase;
import com.laboratorio.financas.orcamento.domain.Orcamento;
import com.laboratorio.financas.orcamento.interfaces.dto.CriarOrcamentoRequest;
import com.laboratorio.financas.orcamento.interfaces.dto.OrcamentoResponse;
import com.laboratorio.financas.orcamento.interfaces.dto.ProgressoResponse;
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
@RequestMapping("/api/orcamentos")
public class OrcamentoController {

    private static final Logger LOG = LoggerFactory.getLogger(OrcamentoController.class);
    private static final String ENTITY_TYPE = "orcamento";

    private final CriarOrcamentoUseCase criarOrcamentoUseCase;
    private final ListarOrcamentosUseCase listarOrcamentosUseCase;
    private final BuscarOrcamentoPorIdUseCase buscarOrcamentoPorIdUseCase;
    private final DesativarOrcamentoUseCase desativarOrcamentoUseCase;
    private final CalcularProgressoDoOrcamentoUseCase calcularProgressoUseCase;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;
    private final UsuarioRepository usuarioRepository;

    public OrcamentoController(
            CriarOrcamentoUseCase criarOrcamentoUseCase,
            ListarOrcamentosUseCase listarOrcamentosUseCase,
            BuscarOrcamentoPorIdUseCase buscarOrcamentoPorIdUseCase,
            DesativarOrcamentoUseCase desativarOrcamentoUseCase,
            CalcularProgressoDoOrcamentoUseCase calcularProgressoUseCase,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper,
            UsuarioRepository usuarioRepository
    ) {
        this.criarOrcamentoUseCase = criarOrcamentoUseCase;
        this.listarOrcamentosUseCase = listarOrcamentosUseCase;
        this.buscarOrcamentoPorIdUseCase = buscarOrcamentoPorIdUseCase;
        this.desativarOrcamentoUseCase = desativarOrcamentoUseCase;
        this.calcularProgressoUseCase = calcularProgressoUseCase;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResponse criar(
            @Valid @RequestBody CriarOrcamentoRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        CriarOrcamentoUseCase.Comando comando = new CriarOrcamentoUseCase.Comando(
                resolverUserId(authentication),
                request.categoriaId(),
                request.valorLimiteValor(),
                request.valorLimiteMoeda(),
                request.mesAno()
        );
        Orcamento criado = criarOrcamentoUseCase.executar(comando);
        OrcamentoResponse response = toResponse(criado);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, criado.getId(), AuditAction.CREATE,
                userEmail(), screenCode, null, toJson(response)));
        return response;
    }

    @GetMapping
    public List<OrcamentoResponse> listar() {
        return listarOrcamentosUseCase.executar().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public OrcamentoResponse buscar(@PathVariable UUID id) {
        return toResponse(buscarOrcamentoPorIdUseCase.executar(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        Orcamento antes = buscarOrcamentoPorIdUseCase.executar(id);
        String before = toJson(toResponse(antes));
        desativarOrcamentoUseCase.executar(id);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.DELETE,
                userEmail(), screenCode, before, null));
    }

    @GetMapping("/{id}/progresso")
    public ProgressoResponse progresso(@PathVariable UUID id) {
        CalcularProgressoDoOrcamentoUseCase.Resultado resultado = calcularProgressoUseCase.executar(id);
        return new ProgressoResponse(
                resultado.orcamentoId(),
                resultado.categoriaId(),
                resultado.mesAno(),
                new OrcamentoResponse.ValorMonetario(
                        resultado.valorLimite().valor(),
                        resultado.valorLimite().moeda().getCurrencyCode()
                ),
                new OrcamentoResponse.ValorMonetario(
                        resultado.totalGasto().valor(),
                        resultado.totalGasto().moeda().getCurrencyCode()
                ),
                resultado.percentualUtilizado(),
                resultado.status().name()
        );
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

    private OrcamentoResponse toResponse(Orcamento orcamento) {
        return new OrcamentoResponse(
                orcamento.getId(),
                orcamento.getUserId(),
                orcamento.getCategoriaId(),
                new OrcamentoResponse.ValorMonetario(
                        orcamento.getValorLimite().valor(),
                        orcamento.getValorLimite().moeda().getCurrencyCode()
                ),
                orcamento.getMesAno(),
                orcamento.isAtivo(),
                orcamento.getCriadoEm(),
                orcamento.getAtualizadoEm()
        );
    }
}
