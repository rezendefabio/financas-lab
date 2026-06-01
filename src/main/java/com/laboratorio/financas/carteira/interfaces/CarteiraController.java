package com.laboratorio.financas.carteira.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.carteira.application.AtualizarCarteiraUseCase;
import com.laboratorio.financas.carteira.application.CriarCarteiraUseCase;
import com.laboratorio.financas.carteira.application.DeletarCarteiraUseCase;
import com.laboratorio.financas.carteira.application.ListarCarteirasUseCase;
import com.laboratorio.financas.carteira.domain.Carteira;
import com.laboratorio.financas.carteira.domain.CarteiraNaoEncontradaException;
import com.laboratorio.financas.carteira.domain.CarteiraRepository;
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
@RequestMapping("/api/carteiras")
public class CarteiraController {

    private static final Logger LOG = LoggerFactory.getLogger(CarteiraController.class);
    private static final String ENTITY_TYPE = "carteira";

    private final CriarCarteiraUseCase criarCarteiraUseCase;
    private final ListarCarteirasUseCase listarCarteirasUseCase;
    private final AtualizarCarteiraUseCase atualizarCarteiraUseCase;
    private final DeletarCarteiraUseCase deletarCarteiraUseCase;
    private final CarteiraRepository carteiraRepository;
    private final UserIdResolver userIdResolver;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public CarteiraController(
            CriarCarteiraUseCase criarCarteiraUseCase,
            ListarCarteirasUseCase listarCarteirasUseCase,
            AtualizarCarteiraUseCase atualizarCarteiraUseCase,
            DeletarCarteiraUseCase deletarCarteiraUseCase,
            CarteiraRepository carteiraRepository,
            UserIdResolver userIdResolver,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper
    ) {
        this.criarCarteiraUseCase = criarCarteiraUseCase;
        this.listarCarteirasUseCase = listarCarteirasUseCase;
        this.atualizarCarteiraUseCase = atualizarCarteiraUseCase;
        this.deletarCarteiraUseCase = deletarCarteiraUseCase;
        this.carteiraRepository = carteiraRepository;
        this.userIdResolver = userIdResolver;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<CarteiraResponse> listar() {
        return listarCarteirasUseCase.executar().stream()
                .map(CarteiraResponse::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    public CarteiraResponse buscar(@PathVariable UUID id) {
        Carteira carteira = carteiraRepository.buscarPorId(id)
                .orElseThrow(() -> new CarteiraNaoEncontradaException(id));
        return CarteiraResponse.fromDomain(carteira);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarteiraResponse criar(
            @Valid @RequestBody CriarCarteiraRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = userIdResolver.resolve(authentication);
        CriarCarteiraUseCase.Comando comando = new CriarCarteiraUseCase.Comando(
                userId, request.contaId(), request.nome(), request.tipo());
        Carteira criada = criarCarteiraUseCase.executar(comando);
        CarteiraResponse response = CarteiraResponse.fromDomain(criada);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, criada.getId(), AuditAction.CREATE,
                userEmail(authentication), screenCode, null, toJson(response)));
        return response;
    }

    @PutMapping("/{id}")
    public CarteiraResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarCarteiraRequest request,
            Authentication authentication
    ) {
        UUID userId = userIdResolver.resolve(authentication);
        buscarDoUsuario(id, userId);
        AtualizarCarteiraUseCase.Comando comando = new AtualizarCarteiraUseCase.Comando(
                id, request.nome(), request.tipo());
        Carteira atualizada = atualizarCarteiraUseCase.executar(comando);
        return CarteiraResponse.fromDomain(atualizada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = userIdResolver.resolve(authentication);
        buscarDoUsuario(id, userId);
        deletarCarteiraUseCase.executar(id);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.DELETE,
                userEmail(authentication), screenCode, null, null));
    }

    private Carteira buscarDoUsuario(UUID id, UUID userId) {
        return carteiraRepository.buscarPorId(id)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new CarteiraNaoEncontradaException(id));
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
