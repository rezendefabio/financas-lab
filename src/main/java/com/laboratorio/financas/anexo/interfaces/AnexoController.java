package com.laboratorio.financas.anexo.interfaces;

import com.laboratorio.financas.anexo.application.CriarAnexoUseCase;
import com.laboratorio.financas.anexo.interfaces.dto.CriarAnexoRequest;
import com.laboratorio.financas.anexo.interfaces.dto.AnexoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/anexos")
public class AnexoController {

    private final CriarAnexoUseCase criarAnexoUseCase;

    public AnexoController(CriarAnexoUseCase criarAnexoUseCase) {
        this.criarAnexoUseCase = criarAnexoUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnexoResponse criar(@RequestBody @Valid CriarAnexoRequest request) {
        // TODO: implementar
        throw new UnsupportedOperationException("Nao implementado");
    }
}
