package com.laboratorio.financas.categoria.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.categoria.domain.Categoria;
import com.laboratorio.financas.categoria.domain.TipoCategoria;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoriaRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private CategoriaRepositoryImpl repository;

    @Autowired
    private CategoriaJpaRepository jpaRepository;

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    private Categoria categoriaSimples(String nome, TipoCategoria tipo) {
        return new Categoria(nome, tipo);
    }

    private Categoria categoriaSystem(String nome, TipoCategoria tipo) {
        return new Categoria(UUID.randomUUID(), nome, tipo, null, null, true, Instant.now(), null);
    }

    private Categoria categoriaDeUsuario(String nome, TipoCategoria tipo, UUID userId) {
        return new Categoria(UUID.randomUUID(), nome, tipo, null, userId, false, Instant.now(), null);
    }

    @Test
    void salvarPersisteERetornaInstanciaEquivalente() {
        // Given
        Categoria nova = categoriaSimples("Salario", TipoCategoria.RECEITA);

        // When
        Categoria salva = repository.salvar(nova);

        // Then
        assertThat(salva.getId()).isEqualTo(nova.getId());
        assertThat(salva.getNome()).isEqualTo("Salario");
        assertThat(salva.getTipo()).isEqualTo(TipoCategoria.RECEITA);
        assertThat(salva.isSystem()).isFalse();
        assertThat(salva.getUserId()).isNull();
    }

    @Test
    void salvarCategoriaSystemPreservaCampos() {
        // Given
        Categoria system = categoriaSystem("Transferencia entre contas", TipoCategoria.NEUTRAL);

        // When
        Categoria salva = repository.salvar(system);

        // Then
        assertThat(salva.isSystem()).isTrue();
        assertThat(salva.getUserId()).isNull();
        assertThat(salva.getTipo()).isEqualTo(TipoCategoria.NEUTRAL);
    }

    @Test
    void salvarCategoriaDeUsuarioPreservaUserId() {
        // Given
        UUID userId = UUID.randomUUID();
        Categoria categoria = categoriaDeUsuario("Mercado", TipoCategoria.DESPESA, userId);

        // When
        Categoria salva = repository.salvar(categoria);

        // Then
        assertThat(salva.getUserId()).isEqualTo(userId);
        assertThat(salva.isSystem()).isFalse();
    }

    @Test
    void buscarPorIdRetornaCategoriaQuandoExiste() {
        // Given
        Categoria nova = categoriaSimples("Aluguel", TipoCategoria.DESPESA);
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
        repository.salvar(categoriaSimples("Salario", TipoCategoria.RECEITA));
        repository.salvar(categoriaSimples("Freelance", TipoCategoria.RECEITA));
        repository.salvar(categoriaSimples("Aluguel", TipoCategoria.DESPESA));

        // When
        List<Categoria> todas = repository.listarTodas();

        // Then
        assertThat(todas).hasSize(3);
    }

    @Test
    void listarPorTipoFiltraCorretamente() {
        // Given -- 2 RECEITA + 1 DESPESA
        repository.salvar(categoriaSimples("Salario", TipoCategoria.RECEITA));
        repository.salvar(categoriaSimples("Freelance", TipoCategoria.RECEITA));
        repository.salvar(categoriaSimples("Aluguel", TipoCategoria.DESPESA));

        // When
        List<Categoria> receitas = repository.listarPorTipo(TipoCategoria.RECEITA);

        // Then
        assertThat(receitas).hasSize(2);
        assertThat(receitas).allMatch(c -> c.getTipo() == TipoCategoria.RECEITA);
    }

    @Test
    void deletarRemoveDosBanco() {
        // Given
        Categoria nova = categoriaSimples("Mercado", TipoCategoria.DESPESA);
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
        // Given -- 2 raiz + 1 filho
        Categoria raiz1 = repository.salvar(categoriaSimples("Alimentacao", TipoCategoria.DESPESA));
        Categoria raiz2 = repository.salvar(categoriaSimples("Transporte", TipoCategoria.DESPESA));
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
        // Given -- 1 pai com 2 filhos + 1 outro pai sem filhos
        Categoria pai = repository.salvar(categoriaSimples("Alimentacao", TipoCategoria.DESPESA));
        Categoria outroPai = repository.salvar(categoriaSimples("Transporte", TipoCategoria.DESPESA));
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
        Categoria raiz = repository.salvar(categoriaSimples("Salario", TipoCategoria.RECEITA));

        List<Categoria> filhos = repository.listarFilhosDe(raiz.getId());

        assertThat(filhos).isEmpty();
    }

    @Test
    void listarVisiveisParaRetornaCategoriasSystemEDoUsuario() {
        // Given -- 1 system + 1 do usuario + 1 de outro usuario
        UUID userId = UUID.randomUUID();
        UUID outroUserId = UUID.randomUUID();
        Categoria system = repository.salvar(categoriaSystem("Transferencia", TipoCategoria.NEUTRAL));
        Categoria doUsuario = repository.salvar(categoriaDeUsuario("Salario", TipoCategoria.RECEITA, userId));
        repository.salvar(categoriaDeUsuario("Aluguel", TipoCategoria.DESPESA, outroUserId));

        // When
        List<Categoria> visiveis = repository.listarVisiveisPara(userId);

        // Then
        assertThat(visiveis).hasSize(2);
        assertThat(visiveis).extracting(Categoria::getId)
                .containsExactlyInAnyOrder(system.getId(), doUsuario.getId());
    }

    @Test
    void listarVisiveisParaRetornaApenasSystemQuandoUsuarioSemCategorias() {
        // Given -- apenas system
        UUID userId = UUID.randomUUID();
        Categoria system = repository.salvar(categoriaSystem("Transferencia", TipoCategoria.NEUTRAL));

        // When
        List<Categoria> visiveis = repository.listarVisiveisPara(userId);

        // Then
        assertThat(visiveis).hasSize(1);
        assertThat(visiveis.get(0).getId()).isEqualTo(system.getId());
        assertThat(visiveis.get(0).isSystem()).isTrue();
    }

    @Test
    void listarVisiveisParaRetornaVazioQuandoNaoHaNada() {
        // Given -- nenhuma categoria
        UUID userId = UUID.randomUUID();

        // When
        List<Categoria> visiveis = repository.listarVisiveisPara(userId);

        // Then
        assertThat(visiveis).isEmpty();
    }
}
