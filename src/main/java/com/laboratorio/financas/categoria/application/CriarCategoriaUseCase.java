package com.laboratorio.financas.categoria.application;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.CategoriaNaoEncontradaException;
import com.laboratorio.financas.categoria.domain.CategoriaRepository;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CriarCategoriaUseCase {

    private final CategoriaRepository repository;

    public CriarCategoriaUseCase(CategoriaRepository repository) {
        this.repository = repository;
    }

    public record Comando(
            String nome,
            TipoCategoria tipo,
            UUID categoriaPaiId,
            UUID userId,
            boolean system
    ) {
        // Construtor de compatibilidade retroativa sem userId/system
        public Comando(String nome, TipoCategoria tipo, UUID categoriaPaiId) {
            this(nome, tipo, categoriaPaiId, null, false);
        }
    }

    @Transactional
    public Categoria executar(Comando comando) {
        if (comando.categoriaPaiId() != null) {
            Categoria pai = repository.buscarPorId(comando.categoriaPaiId())
                    .orElseThrow(() -> new CategoriaNaoEncontradaException(comando.categoriaPaiId()));
            if (pai.getCategoriaPaiId() != null) {
                throw new IllegalArgumentException("Nao e permitido criar subcategoria de subcategoria");
            }
        }
        Categoria nova = new Categoria(
                UUID.randomUUID(),
                comando.nome(),
                comando.tipo(),
                comando.categoriaPaiId(),
                comando.userId(),
                comando.system(),
                java.time.Instant.now(),
                null
        );
        return repository.salvar(nova);
    }
}
