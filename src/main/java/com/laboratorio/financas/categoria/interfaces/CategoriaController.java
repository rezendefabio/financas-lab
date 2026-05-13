package com.laboratorio.financas.categoria.interfaces;

import com.laboratorio.financas.categoria.application.BuscarCategoriaPorIdUseCase;
import com.laboratorio.financas.categoria.application.CriarCategoriaUseCase;
import com.laboratorio.financas.categoria.application.DeletarCategoriaUseCase;
import com.laboratorio.financas.categoria.application.ListarCategoriasUseCase;
import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import com.laboratorio.financas.categoria.interfaces.dto.CategoriaResponse;
import com.laboratorio.financas.categoria.interfaces.dto.CriarCategoriaRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CriarCategoriaUseCase criarCategoriaUseCase;
    private final ListarCategoriasUseCase listarCategoriasUseCase;
    private final BuscarCategoriaPorIdUseCase buscarCategoriaPorIdUseCase;
    private final DeletarCategoriaUseCase deletarCategoriaUseCase;

    public CategoriaController(
            CriarCategoriaUseCase criarCategoriaUseCase,
            ListarCategoriasUseCase listarCategoriasUseCase,
            BuscarCategoriaPorIdUseCase buscarCategoriaPorIdUseCase,
            DeletarCategoriaUseCase deletarCategoriaUseCase
    ) {
        this.criarCategoriaUseCase = criarCategoriaUseCase;
        this.listarCategoriasUseCase = listarCategoriasUseCase;
        this.buscarCategoriaPorIdUseCase = buscarCategoriaPorIdUseCase;
        this.deletarCategoriaUseCase = deletarCategoriaUseCase;
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CriarCategoriaRequest request) {
        CriarCategoriaUseCase.Comando comando = new CriarCategoriaUseCase.Comando(
                request.nome(),
                request.tipo(),
                request.categoriaPaiId()
        );
        Categoria criada = criarCategoriaUseCase.executar(comando);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoriaResponse.fromDomain(criada));
    }

    @GetMapping
    public List<CategoriaResponse> listar(
            @RequestParam(name = "tipo", required = false) TipoCategoria tipo
    ) {
        List<Categoria> categorias = listarCategoriasUseCase.executar(tipo);
        return categorias.stream().map(CategoriaResponse::fromDomain).toList();
    }

    @GetMapping("/{id}")
    public CategoriaResponse buscar(@PathVariable UUID id) {
        Categoria categoria = buscarCategoriaPorIdUseCase.executar(id);
        return CategoriaResponse.fromDomain(categoria);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id) {
        deletarCategoriaUseCase.executar(id);
    }
}
