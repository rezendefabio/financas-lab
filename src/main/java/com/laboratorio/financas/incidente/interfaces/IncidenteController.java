package com.laboratorio.financas.incidente.interfaces;

import com.laboratorio.financas.incidente.application.BuscarIncidenteUseCase;
import com.laboratorio.financas.incidente.application.ListarIncidentesUseCase;
import com.laboratorio.financas.incidente.application.RegistrarErroUseCase;
import com.laboratorio.financas.incidente.domain.ErroRegistrado;
import com.laboratorio.financas.incidente.domain.FiltrosIncidente;
import com.laboratorio.financas.incidente.interfaces.dto.IncidenteCodigoResponse;
import com.laboratorio.financas.incidente.interfaces.dto.IncidenteResponse;
import com.laboratorio.financas.incidente.interfaces.dto.RegistrarIncidenteRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidentes")
public class IncidenteController {

    private final BuscarIncidenteUseCase buscarIncidenteUseCase;
    private final ListarIncidentesUseCase listarIncidentesUseCase;
    private final RegistrarErroUseCase registrarErroUseCase;

    public IncidenteController(BuscarIncidenteUseCase buscarIncidenteUseCase,
                               ListarIncidentesUseCase listarIncidentesUseCase,
                               RegistrarErroUseCase registrarErroUseCase) {
        this.buscarIncidenteUseCase = buscarIncidenteUseCase;
        this.listarIncidentesUseCase = listarIncidentesUseCase;
        this.registrarErroUseCase = registrarErroUseCase;
    }

    @GetMapping
    public List<IncidenteResponse> listar(
            @RequestParam(required = false) Instant criadoApartirDe,
            @RequestParam(required = false) Instant criadoAte,
            @RequestParam(required = false) String classeErro,
            @RequestParam(required = false) String operacao) {
        FiltrosIncidente filtros = new FiltrosIncidente(criadoApartirDe, criadoAte, classeErro, operacao);
        return listarIncidentesUseCase.executar(filtros)
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/{codigo}")
    public IncidenteResponse buscar(@PathVariable String codigo) {
        ErroRegistrado erro = buscarIncidenteUseCase.executar(codigo);
        return toResponse(erro);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidenteCodigoResponse registrar(@RequestBody RegistrarIncidenteRequest request) {
        String codigo = registrarErroUseCase.executar(new RegistrarErroUseCase.Comando(
                request.operacao(),
                request.classeErro(),
                request.mensagem(),
                request.stackTrace()
        ));
        return new IncidenteCodigoResponse(codigo);
    }

    private IncidenteResponse toResponse(ErroRegistrado erro) {
        return new IncidenteResponse(
                erro.getId(),
                erro.getCodigo(),
                erro.getOperacao(),
                erro.getClasseErro(),
                erro.getMensagem(),
                erro.getStackTrace(),
                erro.getCriadoEm()
        );
    }
}
