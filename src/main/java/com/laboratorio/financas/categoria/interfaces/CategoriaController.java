package com.laboratorio.financas.categoria.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.categoria.application.BuscarCategoriaPorIdUseCase;
import com.laboratorio.financas.categoria.application.CriarCategoriaUseCase;
import com.laboratorio.financas.categoria.application.DeletarCategoriaUseCase;
import com.laboratorio.financas.categoria.application.ListarCategoriasUseCase;
import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import com.laboratorio.financas.categoria.interfaces.dto.CategoriaResponse;
import com.laboratorio.financas.categoria.interfaces.dto.CriarCategoriaRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private static final Logger LOG = LoggerFactory.getLogger(CategoriaController.class);
    private static final String ENTITY_TYPE = "categoria";

    private final CriarCategoriaUseCase criarCategoriaUseCase;
    private final ListarCategoriasUseCase listarCategoriasUseCase;
    private final BuscarCategoriaPorIdUseCase buscarCategoriaPorIdUseCase;
    private final DeletarCategoriaUseCase deletarCategoriaUseCase;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public CategoriaController(
            CriarCategoriaUseCase criarCategoriaUseCase,
            ListarCategoriasUseCase listarCategoriasUseCase,
            BuscarCategoriaPorIdUseCase buscarCategoriaPorIdUseCase,
            DeletarCategoriaUseCase deletarCategoriaUseCase,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper
    ) {
        this.criarCategoriaUseCase = criarCategoriaUseCase;
        this.listarCategoriasUseCase = listarCategoriasUseCase;
        this.buscarCategoriaPorIdUseCase = buscarCategoriaPorIdUseCase;
        this.deletarCategoriaUseCase = deletarCategoriaUseCase;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(
            @Valid @RequestBody CriarCategoriaRequest request,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando(
                request.nome(),
                request.tipo(),
                request.categoriaPaiId(),
                request.userId(),
                request.system()
        );
        Categoria criada = criarCategoriaUseCase.executar(comando);
        CategoriaResponse response = CategoriaResponse.fromDomain(criada);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, criada.getId(), AuditAction.CREATE,
                userEmail(), screenCode, null, toJson(response)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<CategoriaResponse> listar(
            @RequestParam(name = "tipo", required = false) TipoCategoria tipo
    ) {
        List<Categoria> categorias = listarCategoriasUseCase.executar(tipo);
        return categorias.stream().map(CategoriaResponse::fromDomain).toList();
    }

    @GetMapping("/{id}")
    public CategoriaResponse buscar(@PathVariable UUID id) {
        Categoria categoria = buscarCategoriaPorIdUseCase.executar(id);
        return CategoriaResponse.fromDomain(categoria);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        Categoria antes = buscarCategoriaPorIdUseCase.executar(id);
        String before = toJson(CategoriaResponse.fromDomain(antes));
        deletarCategoriaUseCase.executar(id);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.DELETE,
                userEmail(), screenCode, before, null));
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
