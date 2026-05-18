package com.laboratorio.financas.incidente.interfaces;

import com.laboratorio.financas.incidente.application.BuscarIncidenteUseCase;
import com.laboratorio.financas.incidente.application.RegistrarErroUseCase;
import com.laboratorio.financas.incidente.domain.ErroRegistrado;
import com.laboratorio.financas.incidente.interfaces.dto.IncidenteCodigoResponse;
import com.laboratorio.financas.incidente.interfaces.dto.IncidenteResponse;
import com.laboratorio.financas.incidente.interfaces.dto.RegistrarIncidenteRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidentes")
public class IncidenteController {

    private final BuscarIncidenteUseCase buscarIncidenteUseCase;
    private final RegistrarErroUseCase registrarErroUseCase;

    public IncidenteController(BuscarIncidenteUseCase buscarIncidenteUseCase,
                               RegistrarErroUseCase registrarErroUseCase) {
        this.buscarIncidenteUseCase = buscarIncidenteUseCase;
        this.registrarErroUseCase = registrarErroUseCase;
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
