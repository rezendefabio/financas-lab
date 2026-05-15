package com.laboratorio.financas.instituicao.interfaces;

import com.laboratorio.financas.instituicao.application.BuscarInstituicaoPorIdUseCase;
import com.laboratorio.financas.instituicao.application.ListarInstituicoesUseCase;
import com.laboratorio.financas.instituicao.domain.Instituicao;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instituicoes")
public class InstituicaoController {

    private final ListarInstituicoesUseCase listarInstituicoesUseCase;
    private final BuscarInstituicaoPorIdUseCase buscarInstituicaoPorIdUseCase;

    public InstituicaoController(
            ListarInstituicoesUseCase listarInstituicoesUseCase,
            BuscarInstituicaoPorIdUseCase buscarInstituicaoPorIdUseCase
    ) {
        this.listarInstituicoesUseCase = listarInstituicoesUseCase;
        this.buscarInstituicaoPorIdUseCase = buscarInstituicaoPorIdUseCase;
    }

    @GetMapping
    public List<InstituicaoResponse> listar() {
        List<Instituicao> instituicoes = listarInstituicoesUseCase.executar();
        return instituicoes.stream().map(InstituicaoResponse::fromDomain).toList();
    }

    @GetMapping("/{id}")
    public InstituicaoResponse buscar(@PathVariable UUID id) {
        Instituicao instituicao = buscarInstituicaoPorIdUseCase.executar(id);
        return InstituicaoResponse.fromDomain(instituicao);
    }
}
