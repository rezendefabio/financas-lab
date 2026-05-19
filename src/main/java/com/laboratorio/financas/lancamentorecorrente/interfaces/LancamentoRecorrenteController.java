package com.laboratorio.financas.lancamentorecorrente.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.lancamentorecorrente.application.BuscarLancamentoRecorrentePorIdUseCase;
import com.laboratorio.financas.lancamentorecorrente.application.CriarLancamentoRecorrenteUseCase;
import com.laboratorio.financas.lancamentorecorrente.application.DesativarLancamentoRecorrenteUseCase;
import com.laboratorio.financas.lancamentorecorrente.application.ExecutarLancamentoRecorrenteUseCase;
import com.laboratorio.financas.lancamentorecorrente.application.ListarLancamentosRecorrentesUseCase;
import com.laboratorio.financas.lancamentorecorrente.domain.LancamentoRecorrente;
import com.laboratorio.financas.lancamentorecorrente.interfaces.dto.CriarLancamentoRecorrenteRequest;
import com.laboratorio.financas.lancamentorecorrente.interfaces.dto.ExecucaoResponse;
import com.laboratorio.financas.lancamentorecorrente.interfaces.dto.LancamentoRecorrenteResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/lancamentos-recorrentes")
public class LancamentoRecorrenteController {

    private static final Logger LOG = LoggerFactory.getLogger(LancamentoRecorrenteController.class);
    private static final String ENTITY_TYPE = "lancamentorecorrente";

    private final CriarLancamentoRecorrenteUseCase criarUseCase;
    private final ListarLancamentosRecorrentesUseCase listarUseCase;
    private final BuscarLancamentoRecorrentePorIdUseCase buscarUseCase;
    private final DesativarLancamentoRecorrenteUseCase desativarUseCase;
    private final ExecutarLancamentoRecorrenteUseCase executarUseCase;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public LancamentoRecorrenteController(
            CriarLancamentoRecorrenteUseCase criarUseCase,
            ListarLancamentosRecorrentesUseCase listarUseCase,
            BuscarLancamentoRecorrentePorIdUseCase buscarUseCase,
            DesativarLancamentoRecorrenteUseCase desativarUseCase,
            ExecutarLancamentoRecorrenteUseCase executarUseCase,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper
    ) {
        this.criarUseCase = criarUseCase;
        this.listarUseCase = listarUseCase;
        this.buscarUseCase = buscarUseCase;
        this.desativarUseCase = desativarUseCase;
        this.executarUseCase = executarUseCase;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoRecorrenteResponse criar(
            @RequestBody @Valid CriarLancamentoRecorrenteRequest request,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        CriarLancamentoRecorrenteUseCase.Comando comando = new CriarLancamentoRecorrenteUseCase.Comando(
                request.descricao(),
                request.tipo(),
                request.valorValor(),
                request.valorMoeda(),
                request.contaId(),
                request.categoriaId(),
                request.periodicidade(),
                request.proximaOcorrencia()
        );
        LancamentoRecorrente lancamento = criarUseCase.executar(comando);
        LancamentoRecorrenteResponse response = LancamentoRecorrenteResponse.fromDomain(lancamento);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, lancamento.getId(), AuditAction.CREATE,
                userEmail(), screenCode, null, toJson(response)));
        return response;
    }

    @GetMapping
    public List<LancamentoRecorrenteResponse> listar() {
        return listarUseCase.executar().stream()
                .map(LancamentoRecorrenteResponse::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    public LancamentoRecorrenteResponse buscar(@PathVariable UUID id) {
        return LancamentoRecorrenteResponse.fromDomain(buscarUseCase.executar(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        LancamentoRecorrente antes = buscarUseCase.executar(id);
        String before = toJson(LancamentoRecorrenteResponse.fromDomain(antes));
        desativarUseCase.executar(id);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.DELETE,
                userEmail(), screenCode, before, null));
    }

    @PostMapping("/{id}/execucoes")
    @ResponseStatus(HttpStatus.CREATED)
    public ExecucaoResponse executar(@PathVariable UUID id) {
        ExecutarLancamentoRecorrenteUseCase.Resultado resultado = executarUseCase.executar(id);
        return new ExecucaoResponse(
                resultado.transacaoId(),
                resultado.lancamentoRecorrenteId(),
                resultado.dataExecutada(),
                resultado.novaProximaOcorrencia()
        );
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
