package com.laboratorio.financas.lancamentorecorrente.interfaces;

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
@RequestMapping("/api/lancamentos-recorrentes")
public class LancamentoRecorrenteController {

    private final CriarLancamentoRecorrenteUseCase criarUseCase;
    private final ListarLancamentosRecorrentesUseCase listarUseCase;
    private final BuscarLancamentoRecorrentePorIdUseCase buscarUseCase;
    private final DesativarLancamentoRecorrenteUseCase desativarUseCase;
    private final ExecutarLancamentoRecorrenteUseCase executarUseCase;

    public LancamentoRecorrenteController(
            CriarLancamentoRecorrenteUseCase criarUseCase,
            ListarLancamentosRecorrentesUseCase listarUseCase,
            BuscarLancamentoRecorrentePorIdUseCase buscarUseCase,
            DesativarLancamentoRecorrenteUseCase desativarUseCase,
            ExecutarLancamentoRecorrenteUseCase executarUseCase
    ) {
        this.criarUseCase = criarUseCase;
        this.listarUseCase = listarUseCase;
        this.buscarUseCase = buscarUseCase;
        this.desativarUseCase = desativarUseCase;
        this.executarUseCase = executarUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoRecorrenteResponse criar(@RequestBody @Valid CriarLancamentoRecorrenteRequest request) {
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
        return LancamentoRecorrenteResponse.fromDomain(lancamento);
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
    public void desativar(@PathVariable UUID id) {
        desativarUseCase.executar(id);
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
}
