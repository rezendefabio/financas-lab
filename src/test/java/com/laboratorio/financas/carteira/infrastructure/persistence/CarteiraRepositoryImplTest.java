package com.laboratorio.financas.carteira.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.carteira.domain.Carteira;
import com.laboratorio.financas.carteira.domain.TipoCarteira;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioEntity;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CarteiraRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private CarteiraRepositoryImpl repository;

    @Autowired
    private CarteiraJpaRepository jpaRepository;

    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    private UUID userIdA;
    private UUID userIdB;
    private UUID contaId;

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
        userIdA = criarUsuarioPersistido();
        userIdB = criarUsuarioPersistido();
        contaId = UUID.randomUUID();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
    }

    private UUID criarUsuarioPersistido() {
        UUID id = UUID.randomUUID();
        UsuarioEntity entity = new UsuarioEntity(
                id,
                "teste+" + id + "@test.com",
                "hash_bcrypt",
                true,
                Instant.now(),
                null,
                Instant.now()
        );
        usuarioJpaRepository.save(entity);
        return id;
    }

    @Test
    void salvarPersisteERetornaCarteira() {
        Carteira carteira = new Carteira(userIdA, contaId, "Tesouro", TipoCarteira.RENDA_FIXA);

        Carteira salva = repository.salvar(carteira);

        assertThat(salva.getId()).isEqualTo(carteira.getId());
        assertThat(salva.getUserId()).isEqualTo(userIdA);
        assertThat(salva.getContaId()).isEqualTo(contaId);
        assertThat(salva.getNome()).isEqualTo("Tesouro");
        assertThat(salva.getTipo()).isEqualTo(TipoCarteira.RENDA_FIXA);
        assertThat(salva.isAtivo()).isTrue();
    }

    @Test
    void buscarPorIdRetornaCarteiraQuandoExiste() {
        Carteira carteira = new Carteira(userIdA, contaId, "Acoes", TipoCarteira.RENDA_VARIAVEL);
        repository.salvar(carteira);

        Optional<Carteira> resultado = repository.buscarPorId(carteira.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(carteira.getId());
        assertThat(resultado.get().getTipo()).isEqualTo(TipoCarteira.RENDA_VARIAVEL);
    }

    @Test
    void buscarPorIdRetornaVazioQuandoNaoExiste() {
        Optional<Carteira> resultado = repository.buscarPorId(UUID.randomUUID());

        assertThat(resultado).isEmpty();
    }

    @Test
    void listarTodosRetornaCarteirasDeQualquerUsuario() {
        repository.salvar(new Carteira(userIdA, contaId, "Carteira A1", TipoCarteira.RENDA_FIXA));
        repository.salvar(new Carteira(userIdA, contaId, "Carteira A2", TipoCarteira.CRIPTOMOEDA));
        repository.salvar(new Carteira(userIdB, contaId, "Carteira B1", TipoCarteira.OUTROS));

        List<Carteira> todas = repository.listarTodos();

        assertThat(todas).hasSize(3);
        assertThat(todas).anyMatch(c -> c.getUserId().equals(userIdA));
        assertThat(todas).anyMatch(c -> c.getUserId().equals(userIdB));
    }

    @Test
    void listarTodosRetornaListaVaziaQuandoNaoHaCarteiras() {
        List<Carteira> carteiras = repository.listarTodos();

        assertThat(carteiras).isEmpty();
    }

    @Test
    void atualizarPersisteAlteracoes() {
        Carteira carteira = new Carteira(userIdA, contaId, "Antiga", TipoCarteira.RENDA_FIXA);
        repository.salvar(carteira);

        carteira.atualizar("Nova", TipoCarteira.CRIPTOMOEDA);
        Carteira resultado = repository.atualizar(carteira);

        assertThat(resultado.getNome()).isEqualTo("Nova");
        assertThat(resultado.getTipo()).isEqualTo(TipoCarteira.CRIPTOMOEDA);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    @Test
    void deletarRemoveCarteira() {
        Carteira carteira = new Carteira(userIdA, contaId, "Remover", TipoCarteira.OUTROS);
        repository.salvar(carteira);

        repository.deletar(carteira.getId());

        assertThat(repository.buscarPorId(carteira.getId())).isEmpty();
    }
}
