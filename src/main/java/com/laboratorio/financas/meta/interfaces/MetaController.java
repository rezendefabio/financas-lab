package com.laboratorio.financas.meta.interfaces;

import com.laboratorio.financas.meta.application.CriarMetaUseCase;
import com.laboratorio.financas.meta.interfaces.dto.CriarMetaRequest;
import com.laboratorio.financas.meta.interfaces.dto.MetaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metas")
public class MetaController {

    private final CriarMetaUseCase criarMetaUseCase;

    public MetaController(CriarMetaUseCase criarMetaUseCase) {
        this.criarMetaUseCase = criarMetaUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetaResponse criar(@RequestBody @Valid CriarMetaRequest request) {
        // TODO: implementar
        throw new UnsupportedOperationException("Nao implementado");
    }
}
