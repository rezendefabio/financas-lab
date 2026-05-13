package com.laboratorio.financas.categoria.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoriaRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private CategoriaRepositoryImpl repository;

    @Autowired
    private CategoriaJpaRepository jpaRepository;

    @org.junit.jupiter.api.BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    @Test
    void salvarPersisteERetornaInstanciaEquivalente() {
        // Given
        Categoria nova = new Categoria("Salario", TipoCategoria.RECEITA);

        // When
        Categoria salva = repository.salvar(nova);

        // Then
        assertThat(salva.getId()).isEqualTo(nova.getId());
        assertThat(salva.getNome()).isEqualTo("Salario");
        assertThat(salva.getTipo()).isEqualTo(TipoCategoria.RECEITA);
    }

    @Test
    void buscarPorIdRetornaCategoriaQuandoExiste() {
        // Given
        Categoria nova = new Categoria("Aluguel", TipoCategoria.DESPESA);
        repository.salvar(nova);

        // When
        Optional<Categoria> resultado = repository.buscarPorId(nova.getId());

        // Then
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(nova.getId());
        assertThat(resultado.get().getNome()).isEqualTo("Aluguel");
    }

    @Test
    void buscarPorIdRetornaVazioQuandoNaoExiste() {
        // When
        Optional<Categoria> resultado = repository.buscarPorId(UUID.randomUUID());

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    void listarTodasRetornaTodasAsCategorias() {
        // Given
        repository.salvar(new Categoria("Salario", TipoCategoria.RECEITA));
        repository.salvar(new Categoria("Freelance", TipoCategoria.RECEITA));
        repository.salvar(new Categoria("Aluguel", TipoCategoria.DESPESA));

        // When
        List<Categoria> todas = repository.listarTodas();

        // Then
        assertThat(todas).hasSize(3);
    }

    @Test
    void listarPorTipoFiltraCorretamente() {
        // Given — 2 RECEITA + 1 DESPESA
        repository.salvar(new Categoria("Salario", TipoCategoria.RECEITA));
        repository.salvar(new Categoria("Freelance", TipoCategoria.RECEITA));
        repository.salvar(new Categoria("Aluguel", TipoCategoria.DESPESA));

        // When
        List<Categoria> receitas = repository.listarPorTipo(TipoCategoria.RECEITA);

        // Then
        assertThat(receitas).hasSize(2);
        assertThat(receitas).allMatch(c -> c.getTipo() == TipoCategoria.RECEITA);
    }

    @Test
    void deletarRemoveDosBanco() {
        // Given
        Categoria nova = new Categoria("Mercado", TipoCategoria.DESPESA);
        repository.salvar(nova);

        // When
        repository.deletar(nova.getId());

        // Then
        Optional<Categoria> resultado = repository.buscarPorId(nova.getId());
        assertThat(resultado).isEmpty();
    }

    @Test
    void deletarIdInexistenteNaoLancaExcecao() {
        // Spring Data deleteById ignora silenciosamente id inexistente
        repository.deletar(UUID.randomUUID());
        // se chegar aqui sem excecao, o comportamento esta correto
    }

    @Test
    void listarRaizRetornaApenasCategoriasComCategoriaPaiIdNulo() {
        // Given — 2 raiz + 1 filho
        Categoria raiz1 = repository.salvar(new Categoria("Alimentacao", TipoCategoria.DESPESA));
        Categoria raiz2 = repository.salvar(new Categoria("Transporte", TipoCategoria.DESPESA));
        repository.salvar(new Categoria("Mercado", TipoCategoria.DESPESA, raiz1.getId()));

        // When
        List<Categoria> raizes = repository.listarRaiz();

        // Then
        assertThat(raizes).hasSize(2);
        assertThat(raizes).extracting(Categoria::getId)
                .containsExactlyInAnyOrder(raiz1.getId(), raiz2.getId());
        assertThat(raizes).allMatch(c -> c.getCategoriaPaiId() == null);
    }

    @Test
    void listarFilhosDeRetornaFilhosDoParEspecificado() {
        // Given — 1 pai com 2 filhos + 1 outro pai sem filhos
        Categoria pai = repository.salvar(new Categoria("Alimentacao", TipoCategoria.DESPESA));
        Categoria outroPai = repository.salvar(new Categoria("Transporte", TipoCategoria.DESPESA));
        Categoria filho1 = repository.salvar(new Categoria("Mercado", TipoCategoria.DESPESA, pai.getId()));
        Categoria filho2 = repository.salvar(new Categoria("Restaurante", TipoCategoria.DESPESA, pai.getId()));

        // When
        List<Categoria> filhos = repository.listarFilhosDe(pai.getId());
        List<Categoria> filhosOutroPai = repository.listarFilhosDe(outroPai.getId());

        // Then
        assertThat(filhos).hasSize(2);
        assertThat(filhos).extracting(Categoria::getId)
                .containsExactlyInAnyOrder(filho1.getId(), filho2.getId());
        assertThat(filhosOutroPai).isEmpty();
    }

    @Test
    void listarFilhosDeRetornaVazioParaPaiSemFilhos() {
        Categoria raiz = repository.salvar(new Categoria("Salario", TipoCategoria.RECEITA));

        List<Categoria> filhos = repository.listarFilhosDe(raiz.getId());

        assertThat(filhos).isEmpty();
    }
}
