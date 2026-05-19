package com.laboratorio.financas.conta.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.conta.application.BuscarContaPorIdUseCase;
import com.laboratorio.financas.conta.application.CalcularSaldoDaContaUseCase;
import com.laboratorio.financas.conta.application.CalcularSaldoTotalUseCase;
import com.laboratorio.financas.conta.application.CriarContaUseCase;
import com.laboratorio.financas.conta.application.DesativarContaUseCase;
import com.laboratorio.financas.conta.application.ExcluirContaUseCase;
import com.laboratorio.financas.conta.application.ListarContasUseCase;
import com.laboratorio.financas.conta.domain.Conta;
import com.laboratorio.financas.conta.interfaces.dto.ContaResponse;
import com.laboratorio.financas.conta.interfaces.dto.CriarContaRequest;
import com.laboratorio.financas.conta.interfaces.dto.SaldoResponse;
import com.laboratorio.financas.conta.interfaces.dto.SaldoTotalResponse;
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
@RequestMapping("/api/contas")
public class ContaController {

    private static final Logger LOG = LoggerFactory.getLogger(ContaController.class);
    private static final String ENTITY_TYPE = "conta";

    private final CriarContaUseCase criarContaUseCase;
    private final ListarContasUseCase listarContasUseCase;
    private final BuscarContaPorIdUseCase buscarContaPorIdUseCase;
    private final DesativarContaUseCase desativarContaUseCase;
    private final ExcluirContaUseCase excluirContaUseCase;
    private final CalcularSaldoDaContaUseCase calcularSaldoDaContaUseCase;
    private final CalcularSaldoTotalUseCase calcularSaldoTotalUseCase;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public ContaController(
            CriarContaUseCase criarContaUseCase,
            ListarContasUseCase listarContasUseCase,
            BuscarContaPorIdUseCase buscarContaPorIdUseCase,
            DesativarContaUseCase desativarContaUseCase,
            ExcluirContaUseCase excluirContaUseCase,
            CalcularSaldoDaContaUseCase calcularSaldoDaContaUseCase,
            CalcularSaldoTotalUseCase calcularSaldoTotalUseCase,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper
    ) {
        this.criarContaUseCase = criarContaUseCase;
        this.listarContasUseCase = listarContasUseCase;
        this.buscarContaPorIdUseCase = buscarContaPorIdUseCase;
        this.desativarContaUseCase = desativarContaUseCase;
        this.excluirContaUseCase = excluirContaUseCase;
        this.calcularSaldoDaContaUseCase = calcularSaldoDaContaUseCase;
        this.calcularSaldoTotalUseCase = calcularSaldoTotalUseCase;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<ContaResponse> criar(
            @Valid @RequestBody CriarContaRequest request,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        CriarContaUseCase.Comando comando = new CriarContaUseCase.Comando(
                request.nome(),
                request.tipo(),
                request.saldoInicialValor(),
                request.saldoInicialMoeda(),
                request.userId(),
                request.limiteCreditoValor(),
                request.limiteCreditoMoeda(),
                request.diaFechamento(),
                request.diaVencimento()
        );
        Conta criada = criarContaUseCase.executar(comando);
        ContaResponse response = ContaResponse.fromDomain(criada);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, criada.getId(), AuditAction.CREATE,
                userEmail(), screenCode, null, toJson(response)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ContaResponse> listar(@RequestParam(name = "ativa", required = false) Boolean ativa) {
        boolean apenasAtivas = Boolean.TRUE.equals(ativa);
        List<Conta> contas = listarContasUseCase.executar(apenasAtivas);
        return contas.stream().map(ContaResponse::fromDomain).toList();
    }

    @GetMapping("/saldo-total")
    public SaldoTotalResponse calcularSaldoTotal() {
        return SaldoTotalResponse.fromResultado(calcularSaldoTotalUseCase.executar());
    }

    @GetMapping("/{id}")
    public ContaResponse buscar(@PathVariable UUID id) {
        Conta conta = buscarContaPorIdUseCase.executar(id);
        return ContaResponse.fromDomain(conta);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        Conta antes = buscarContaPorIdUseCase.executar(id);
        String before = toJson(ContaResponse.fromDomain(antes));
        desativarContaUseCase.executar(id);
        Conta depois = buscarContaPorIdUseCase.executar(id);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.UPDATE,
                userEmail(), screenCode, before, toJson(ContaResponse.fromDomain(depois))));
    }

    @DeleteMapping("/{id}/excluir")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        Conta antes = buscarContaPorIdUseCase.executar(id);
        String before = toJson(ContaResponse.fromDomain(antes));
        excluirContaUseCase.executar(id);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.DELETE,
                userEmail(), screenCode, before, null));
    }

    @GetMapping("/{id}/saldo")
    public SaldoResponse calcularSaldo(@PathVariable UUID id) {
        CalcularSaldoDaContaUseCase.Resultado resultado = calcularSaldoDaContaUseCase.executar(id);
        return SaldoResponse.fromResultado(resultado);
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
