package com.laboratorio.financas.orcamento.interfaces;

import com.laboratorio.financas.orcamento.application.BuscarOrcamentoPorIdUseCase;
import com.laboratorio.financas.orcamento.application.CalcularProgressoDoOrcamentoUseCase;
import com.laboratorio.financas.orcamento.application.CriarOrcamentoUseCase;
import com.laboratorio.financas.orcamento.application.DesativarOrcamentoUseCase;
import com.laboratorio.financas.orcamento.application.ListarOrcamentosUseCase;
import com.laboratorio.financas.orcamento.domain.Orcamento;
import com.laboratorio.financas.orcamento.interfaces.dto.CriarOrcamentoRequest;
import com.laboratorio.financas.orcamento.interfaces.dto.OrcamentoResponse;
import com.laboratorio.financas.orcamento.interfaces.dto.ProgressoResponse;
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
@RequestMapping("/api/orcamentos")
public class OrcamentoController {

    private final CriarOrcamentoUseCase criarOrcamentoUseCase;
    private final ListarOrcamentosUseCase listarOrcamentosUseCase;
    private final BuscarOrcamentoPorIdUseCase buscarOrcamentoPorIdUseCase;
    private final DesativarOrcamentoUseCase desativarOrcamentoUseCase;
    private final CalcularProgressoDoOrcamentoUseCase calcularProgressoUseCase;

    public OrcamentoController(
            CriarOrcamentoUseCase criarOrcamentoUseCase,
            ListarOrcamentosUseCase listarOrcamentosUseCase,
            BuscarOrcamentoPorIdUseCase buscarOrcamentoPorIdUseCase,
            DesativarOrcamentoUseCase desativarOrcamentoUseCase,
            CalcularProgressoDoOrcamentoUseCase calcularProgressoUseCase
    ) {
        this.criarOrcamentoUseCase = criarOrcamentoUseCase;
        this.listarOrcamentosUseCase = listarOrcamentosUseCase;
        this.buscarOrcamentoPorIdUseCase = buscarOrcamentoPorIdUseCase;
        this.desativarOrcamentoUseCase = desativarOrcamentoUseCase;
        this.calcularProgressoUseCase = calcularProgressoUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResponse criar(@Valid @RequestBody CriarOrcamentoRequest request) {
        CriarOrcamentoUseCase.Comando comando = new CriarOrcamentoUseCase.Comando(
                request.categoriaId(),
                request.valorLimiteValor(),
                request.valorLimiteMoeda(),
                request.mesAno()
        );
        Orcamento criado = criarOrcamentoUseCase.executar(comando);
        return toResponse(criado);
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
    public void desativar(@PathVariable UUID id) {
        desativarOrcamentoUseCase.executar(id);
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

    private OrcamentoResponse toResponse(Orcamento orcamento) {
        return new OrcamentoResponse(
                orcamento.getId(),
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
