package com.laboratorio.financas.meta.interfaces;

import com.laboratorio.financas.meta.application.BuscarMetaPorIdUseCase;
import com.laboratorio.financas.meta.application.CancelarMetaUseCase;
import com.laboratorio.financas.meta.application.CriarMetaUseCase;
import com.laboratorio.financas.meta.application.ListarMetasUseCase;
import com.laboratorio.financas.meta.application.RegistrarDepositoEmMetaUseCase;
import com.laboratorio.financas.meta.domain.Meta;
import com.laboratorio.financas.meta.interfaces.dto.CriarMetaRequest;
import com.laboratorio.financas.meta.interfaces.dto.MetaResponse;
import com.laboratorio.financas.meta.interfaces.dto.RegistrarDepositoRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metas")
public class MetaController {

    private final CriarMetaUseCase criarMetaUseCase;
    private final ListarMetasUseCase listarMetasUseCase;
    private final BuscarMetaPorIdUseCase buscarMetaPorIdUseCase;
    private final CancelarMetaUseCase cancelarMetaUseCase;
    private final RegistrarDepositoEmMetaUseCase registrarDepositoUseCase;

    public MetaController(
            CriarMetaUseCase criarMetaUseCase,
            ListarMetasUseCase listarMetasUseCase,
            BuscarMetaPorIdUseCase buscarMetaPorIdUseCase,
            CancelarMetaUseCase cancelarMetaUseCase,
            RegistrarDepositoEmMetaUseCase registrarDepositoUseCase
    ) {
        this.criarMetaUseCase = criarMetaUseCase;
        this.listarMetasUseCase = listarMetasUseCase;
        this.buscarMetaPorIdUseCase = buscarMetaPorIdUseCase;
        this.cancelarMetaUseCase = cancelarMetaUseCase;
        this.registrarDepositoUseCase = registrarDepositoUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetaResponse criar(@RequestBody @Valid CriarMetaRequest request) {
        CriarMetaUseCase.Comando comando = new CriarMetaUseCase.Comando(
                request.nome(),
                request.valorAlvoValor(),
                request.valorAlvoMoeda(),
                request.prazo()
        );
        Meta meta = criarMetaUseCase.executar(comando);
        return MetaResponse.fromDomain(meta);
    }

    @GetMapping
    public List<MetaResponse> listar() {
        return listarMetasUseCase.executar().stream()
                .map(MetaResponse::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    public MetaResponse buscar(@PathVariable UUID id) {
        return MetaResponse.fromDomain(buscarMetaPorIdUseCase.executar(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable UUID id) {
        cancelarMetaUseCase.executar(id);
    }

    @PostMapping("/{id}/depositos")
    public MetaResponse registrarDeposito(@PathVariable UUID id, @RequestBody @Valid RegistrarDepositoRequest request) {
        RegistrarDepositoEmMetaUseCase.Comando comando = new RegistrarDepositoEmMetaUseCase.Comando(
                id,
                request.valor(),
                request.moeda()
        );
        Meta meta = registrarDepositoUseCase.executar(comando);
        return MetaResponse.fromDomain(meta);
    }
}
