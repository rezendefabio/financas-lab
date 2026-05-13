package com.laboratorio.financas.orcamento.interfaces;

import com.laboratorio.financas.orcamento.application.CriarOrcamentoUseCase;
import com.laboratorio.financas.orcamento.interfaces.dto.CriarOrcamentoRequest;
import com.laboratorio.financas.orcamento.interfaces.dto.OrcamentoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// TODO: ajustar /api/orcamentos para o plural correto no OrcamentoController.java
@RestController
@RequestMapping("/api/orcamentos")
public class OrcamentoController {

    private final CriarOrcamentoUseCase criarOrcamentoUseCase;

    public OrcamentoController(CriarOrcamentoUseCase criarOrcamentoUseCase) {
        this.criarOrcamentoUseCase = criarOrcamentoUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResponse criar(@RequestBody @Valid CriarOrcamentoRequest request) {
        // TODO: implementar
        throw new UnsupportedOperationException("Nao implementado");
    }
}
