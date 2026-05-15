package com.laboratorio.financas.transacao.interfaces;

import com.laboratorio.financas.transacao.application.BuscarTransacaoPorIdUseCase;
import com.laboratorio.financas.transacao.application.CriarTransacaoUseCase;
import com.laboratorio.financas.transacao.application.DeletarTransacaoUseCase;
import com.laboratorio.financas.transacao.application.EditarTransacaoUseCase;
import com.laboratorio.financas.transacao.application.ListarTransacoesUseCase;
import com.laboratorio.financas.transacao.domain.FiltrosTransacao;
import com.laboratorio.financas.transacao.domain.TipoTransacao;
import com.laboratorio.financas.transacao.domain.Transacao;
import com.laboratorio.financas.transacao.interfaces.dto.TransacaoRequest;
import com.laboratorio.financas.transacao.interfaces.dto.TransacaoResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transacoes")
@Validated
public class TransacaoController {

    private static final int SIZE_MAX = 100;

    private final CriarTransacaoUseCase criarTransacaoUseCase;
    private final ListarTransacoesUseCase listarTransacoesUseCase;
    private final BuscarTransacaoPorIdUseCase buscarTransacaoPorIdUseCase;
    private final EditarTransacaoUseCase editarTransacaoUseCase;
    private final DeletarTransacaoUseCase deletarTransacaoUseCase;

    public TransacaoController(
            CriarTransacaoUseCase criarTransacaoUseCase,
            ListarTransacoesUseCase listarTransacoesUseCase,
            BuscarTransacaoPorIdUseCase buscarTransacaoPorIdUseCase,
            EditarTransacaoUseCase editarTransacaoUseCase,
            DeletarTransacaoUseCase deletarTransacaoUseCase
    ) {
        this.criarTransacaoUseCase = criarTransacaoUseCase;
        this.listarTransacoesUseCase = listarTransacoesUseCase;
        this.buscarTransacaoPorIdUseCase = buscarTransacaoPorIdUseCase;
        this.editarTransacaoUseCase = editarTransacaoUseCase;
        this.deletarTransacaoUseCase = deletarTransacaoUseCase;
    }

    @PostMapping
    public ResponseEntity<TransacaoResponse> criar(@Valid @RequestBody TransacaoRequest request) {
        // userId extraido do JWT/SecurityContext -- Fase 1: nao ha lookup de usuario por email,
        // user_id fica null ate que a integracao usuario<->transacao seja implementada
        CriarTransacaoUseCase.Comando comando = toComando(request, null);
        Transacao criada = criarTransacaoUseCase.executar(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransacaoResponse.fromDomain(criada));
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
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(SIZE_MAX) int size
    ) {
        FiltrosTransacao filtros = new FiltrosTransacao(contaId, dataInicio, dataFim, tipo, categoriaId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "data"));
        Page<Transacao> resultado = listarTransacoesUseCase.executar(filtros, pageable);
        return resultado.map(TransacaoResponse::fromDomain);
    }

    @GetMapping("/{id}")
    public TransacaoResponse buscar(@PathVariable UUID id) {
        Transacao transacao = buscarTransacaoPorIdUseCase.executar(id);
        return TransacaoResponse.fromDomain(transacao);
    }

    @PutMapping("/{id}")
    public TransacaoResponse editar(
            @PathVariable UUID id,
            @Valid @RequestBody TransacaoRequest request
    ) {
        CriarTransacaoUseCase.Comando comando = toComando(request, null);
        Transacao atualizada = editarTransacaoUseCase.executar(id, comando);
        return TransacaoResponse.fromDomain(atualizada);
    }

    /**
     * Soft delete: marca deleted_at sem remover do banco.
     * Transacao deletada fica invisivel para listagens e buscas padrao.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id) {
        deletarTransacaoUseCase.executar(id);
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
