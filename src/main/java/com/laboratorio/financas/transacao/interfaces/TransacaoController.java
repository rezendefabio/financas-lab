package com.laboratorio.financas.transacao.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditEvent;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.transacao.application.BuscarTransacaoPorIdUseCase;
import com.laboratorio.financas.transacao.application.CriarTransacaoUseCase;
import com.laboratorio.financas.transacao.application.DeletarTransacaoUseCase;
import com.laboratorio.financas.transacao.application.EditarTransacaoUseCase;
import com.laboratorio.financas.transacao.application.ListarTransacoesUseCase;
import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.StatusTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.interfaces.dto.TransacaoRequest;
import com.laboratorio.financas.transacao.interfaces.dto.TransacaoResponse;
import com.laboratorio.financas.usuario.domain.Usuario;
import com.laboratorio.financas.usuario.domain.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transacoes")
@Validated
public class TransacaoController {

    private static final int SIZE_MAX = 100;
    private static final Logger LOG = LoggerFactory.getLogger(TransacaoController.class);
    private static final String ENTITY_TYPE = "transacao";

    /**
     * Whitelist de campos ordenaveis. Chave = nome aceito na query string;
     * valor = caminho da propriedade na entidade JPA. `valor` e um @Embedded
     * MoneyEmbeddable, entao a ordenacao usa o caminho `valor.valor`.
     */
    private static final Map<String, String> CAMPOS_ORDENAVEIS = Map.of(
            "data", "data",
            "valor", "valor.valor",
            "descricao", "descricao",
            "tipo", "tipo",
            "status", "status"
    );

    private static final Sort SORT_PADRAO = Sort.by(Sort.Direction.DESC, "data");

    private final CriarTransacaoUseCase criarTransacaoUseCase;
    private final ListarTransacoesUseCase listarTransacoesUseCase;
    private final BuscarTransacaoPorIdUseCase buscarTransacaoPorIdUseCase;
    private final EditarTransacaoUseCase editarTransacaoUseCase;
    private final DeletarTransacaoUseCase deletarTransacaoUseCase;
    private final UsuarioRepository usuarioRepository;
    private final AuditPublisher auditPublisher;
    private final ObjectMapper objectMapper;

    public TransacaoController(
            CriarTransacaoUseCase criarTransacaoUseCase,
            ListarTransacoesUseCase listarTransacoesUseCase,
            BuscarTransacaoPorIdUseCase buscarTransacaoPorIdUseCase,
            EditarTransacaoUseCase editarTransacaoUseCase,
            DeletarTransacaoUseCase deletarTransacaoUseCase,
            UsuarioRepository usuarioRepository,
            AuditPublisher auditPublisher,
            ObjectMapper objectMapper
    ) {
        this.criarTransacaoUseCase = criarTransacaoUseCase;
        this.listarTransacoesUseCase = listarTransacoesUseCase;
        this.buscarTransacaoPorIdUseCase = buscarTransacaoPorIdUseCase;
        this.editarTransacaoUseCase = editarTransacaoUseCase;
        this.deletarTransacaoUseCase = deletarTransacaoUseCase;
        this.usuarioRepository = usuarioRepository;
        this.auditPublisher = auditPublisher;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<TransacaoResponse> criar(
            @Valid @RequestBody TransacaoRequest request,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        UUID userId = resolverUserId();
        CriarTransacaoUseCase.Comando comando = toComando(request, userId);
        Transacao criada = criarTransacaoUseCase.executar(comando);
        TransacaoResponse response = TransacaoResponse.fromDomain(criada);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, criada.getId(), AuditAction.CREATE,
                userEmail(), screenCode, null, toJson(response)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public Page<TransacaoResponse> listar(
            @RequestParam(name = "contaId", required = false) UUID contaId,
            @RequestParam(name = "dataInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(name = "dataFim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(name = "tipo", required = false) TipoTransacao tipo,
            @RequestParam(name = "categoriaId", required = false) UUID categoriaId,
            @RequestParam(name = "status", required = false) StatusTransacao status,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(SIZE_MAX) int size
    ) {
        UUID userId = resolverUserId();
        FiltrosTransacao filtros =
                new FiltrosTransacao(contaId, dataInicio, dataFim, tipo, categoriaId, userId, status);
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<Transacao> resultado = listarTransacoesUseCase.executar(filtros, pageable);
        return resultado.map(TransacaoResponse::fromDomain);
    }

    /**
     * Converte o parametro {@code sort} no formato {@code campo:direcao} em um
     * {@link Sort}. Campo ausente/vazio cai no default ({@code data:desc}).
     * Campo fora da whitelist ou direcao invalida lancam
     * {@link IllegalArgumentException} -> HTTP 400 (via GlobalExceptionHandler).
     */
    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return SORT_PADRAO;
        }
        String[] partes = sort.split(":", 2);
        String campo = partes[0].trim();
        String propriedade = CAMPOS_ORDENAVEIS.get(campo);
        if (propriedade == null) {
            throw new IllegalArgumentException(
                    "Campo de ordenacao invalido: '" + campo
                            + "'. Campos permitidos: " + CAMPOS_ORDENAVEIS.keySet());
        }
        Sort.Direction direcao = Sort.Direction.DESC;
        if (partes.length == 2 && !partes[1].isBlank()) {
            String dir = partes[1].trim().toLowerCase();
            if (dir.equals("asc")) {
                direcao = Sort.Direction.ASC;
            } else if (dir.equals("desc")) {
                direcao = Sort.Direction.DESC;
            } else {
                throw new IllegalArgumentException(
                        "Direcao de ordenacao invalida: '" + partes[1].trim()
                                + "'. Use 'asc' ou 'desc'.");
            }
        }
        return Sort.by(direcao, propriedade);
    }

    @GetMapping("/{id}")
    public TransacaoResponse buscar(@PathVariable UUID id) {
        Transacao transacao = buscarTransacaoPorIdUseCase.executar(id);
        return TransacaoResponse.fromDomain(transacao);
    }

    @PutMapping("/{id}")
    public TransacaoResponse editar(
            @PathVariable UUID id,
            @Valid @RequestBody TransacaoRequest request,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode
    ) {
        UUID userId = resolverUserId();
        Transacao antes = buscarTransacaoPorIdUseCase.executar(id);
        String before = toJson(TransacaoResponse.fromDomain(antes));
        CriarTransacaoUseCase.Comando comando = toComando(request, userId);
        Transacao atualizada = editarTransacaoUseCase.executar(id, comando);
        TransacaoResponse response = TransacaoResponse.fromDomain(atualizada);
        auditPublisher.publish(new AuditEvent(
                ENTITY_TYPE, id, AuditAction.UPDATE,
                userEmail(), screenCode, before, toJson(response)));
        return response;
    }

    /**
     * Soft delete: marca deleted_at sem remover do banco.
     * Transacao deletada fica invisivel para listagens e buscas padrao.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Screen-Code", required = false) String screenCode) {
        Transacao antes = buscarTransacaoPorIdUseCase.executar(id);
        String before = toJson(TransacaoResponse.fromDomain(antes));
        deletarTransacaoUseCase.executar(id);
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

    private CriarTransacaoUseCase.Comando toComando(TransacaoRequest request, UUID userId) {
        List<UUID> tagIds = (request.tagIds() != null) ? request.tagIds() : List.of();
        return new CriarTransacaoUseCase.Comando(
                request.tipo(),
                request.valor(),
                request.moeda(),
                request.data(),
                request.descricao(),
                request.contaId(),
                request.contaDestinoId(),
                request.categoriaId(),
                userId,
                request.status(),
                request.payeeId(),
                tagIds
        );
    }
}
