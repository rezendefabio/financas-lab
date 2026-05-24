package com.laboratorio.financas.anexo.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.anexo.application.FazerUploadAnexoUseCase;
import com.laboratorio.financas.anexo.application.ListarAnexosPorEntidadeUseCase;
import com.laboratorio.financas.anexo.application.ObterUrlDownloadAnexoUseCase;
import com.laboratorio.financas.anexo.application.RemoverAnexoUseCase;
import com.laboratorio.financas.anexo.domain.Anexo;
import com.laboratorio.financas.anexo.domain.AnexoNaoEncontradoException;
import com.laboratorio.financas.anexo.domain.AnexoRepository;
import com.laboratorio.financas.anexo.interfaces.dto.AnexoResponse;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/anexos")
public class AnexoController {

    private static final Logger LOG = LoggerFactory.getLogger(AnexoController.class);
    private static final String ENTITY_TYPE = "anexo";

    private final FazerUploadAnexoUseCase fazerUploadAnexoUseCase;
    private final ObterUrlDownloadAnexoUseCase obterUrlDownloadAnexoUseCase;
    private final RemoverAnexoUseCase removerAnexoUseCase;
    private final ListarAnexosPorEntidadeUseCase listarAnexosPorEntidadeUseCase;
    private final AnexoRepository anexoRepository;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public AnexoController(
            FazerUploadAnexoUseCase fazerUploadAnexoUseCase,
            ObterUrlDownloadAnexoUseCase obterUrlDownloadAnexoUseCase,
            RemoverAnexoUseCase removerAnexoUseCase,
            ListarAnexosPorEntidadeUseCase listarAnexosPorEntidadeUseCase,
            AnexoRepository anexoRepository,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper
    ) {
        this.fazerUploadAnexoUseCase = fazerUploadAnexoUseCase;
        this.obterUrlDownloadAnexoUseCase = obterUrlDownloadAnexoUseCase;
        this.removerAnexoUseCase = removerAnexoUseCase;
        this.listarAnexosPorEntidadeUseCase = listarAnexosPorEntidadeUseCase;
        this.anexoRepository = anexoRepository;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnexoResponse upload(
            @RequestParam MultipartFile arquivo,
            @RequestParam String entidadeTipo,
            @RequestParam UUID entidadeId,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode
    ) throws IOException {
        FazerUploadAnexoUseCase.Comando comando = new FazerUploadAnexoUseCase.Comando(
                arquivo.getOriginalFilename(),
                arquivo.getContentType(),
                arquivo.getSize(),
                entidadeTipo,
                entidadeId,
                arquivo.getInputStream()
        );
        Anexo criado = fazerUploadAnexoUseCase.executar(comando);
        AnexoResponse response = AnexoResponse.de(criado);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, criado.getId(), AuditAction.CREATE,
                userEmail(), screenCode, null, toJson(response)));
        return response;
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Void> download(@PathVariable UUID id) {
        String url = obterUrlDownloadAnexoUseCase.executar(id);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

    @GetMapping
    public List<AnexoResponse> listar(
            @RequestParam String entidadeTipo,
            @RequestParam UUID entidadeId
    ) {
        return listarAnexosPorEntidadeUseCase.executar(entidadeTipo, entidadeId).stream()
                .map(AnexoResponse::de)
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        Anexo antes = anexoRepository.buscarPorId(id)
                .orElseThrow(() -> new AnexoNaoEncontradoException(id));
        String before = toJson(AnexoResponse.de(antes));
        removerAnexoUseCase.executar(id);
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
