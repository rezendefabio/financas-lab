package com.laboratorio.financas.payee.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.payee.domain.Payee;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PayeeRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private PayeeRepositoryImpl repository;

    @Autowired
    private PayeeJpaRepository jpaRepository;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OUTRO_USER_ID = UUID.randomUUID();

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    @Test
    void savePersisteERetornaInstanciaEquivalente() {
        Payee novo = new Payee(USER_ID, "Supermercado", null);

        Payee salvo = repository.save(novo);

        assertThat(salvo.getId()).isEqualTo(novo.getId());
        assertThat(salvo.getUserId()).isEqualTo(USER_ID);
        assertThat(salvo.getNome()).isEqualTo("Supermercado");
        assertThat(salvo.getCategoriaPadraoId()).isNull();
    }

    @Test
    void findByIdRetornaPayeeQuandoExiste() {
        Payee novo = new Payee(USER_ID, "Farmacia", null);
        repository.save(novo);

        Optional<Payee> resultado = repository.findById(novo.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(novo.getId());
        assertThat(resultado.get().getNome()).isEqualTo("Farmacia");
    }

    @Test
    void findByIdRetornaVazioQuandoNaoExiste() {
        Optional<Payee> resultado = repository.findById(UUID.randomUUID());
        assertThat(resultado).isEmpty();
    }

    @Test
    void findByUserIdRetornaApenasPayeesDoUsuario() {
        repository.save(new Payee(USER_ID, "Supermercado", null));
        repository.save(new Payee(USER_ID, "Farmacia", null));
        repository.save(new Payee(OUTRO_USER_ID, "Padaria", null));

        List<Payee> resultado = repository.findByUserId(USER_ID);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(p -> p.getUserId().equals(USER_ID));
    }

    @Test
    void findByUserIdRetornaListaVaziaQuandoNaoHaPayees() {
        List<Payee> resultado = repository.findByUserId(USER_ID);
        assertThat(resultado).isEmpty();
    }

    @Test
    void findByIdAndUserIdRetornaPayeeQuandoExisteEPertenceAoUsuario() {
        Payee novo = new Payee(USER_ID, "Academia", null);
        repository.save(novo);

        Optional<Payee> resultado = repository.findByIdAndUserId(novo.getId(), USER_ID);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(novo.getId());
    }

    @Test
    void findByIdAndUserIdRetornaVazioQuandoUserIdDiferente() {
        Payee novo = new Payee(USER_ID, "Academia", null);
        repository.save(novo);

        Optional<Payee> resultado = repository.findByIdAndUserId(novo.getId(), OUTRO_USER_ID);

        assertThat(resultado).isEmpty();
    }

    @Test
    void findByIdAndUserIdRetornaVazioQuandoNaoExiste() {
        Optional<Payee> resultado = repository.findByIdAndUserId(UUID.randomUUID(), USER_ID);
        assertThat(resultado).isEmpty();
    }

    @Test
    void deleteByIdRemoveDosBanco() {
        Payee novo = new Payee(USER_ID, "Restaurante", null);
        repository.save(novo);

        repository.deleteById(novo.getId());

        Optional<Payee> resultado = repository.findById(novo.getId());
        assertThat(resultado).isEmpty();
    }

    @Test
    void deleteByIdIdInexistenteNaoLancaExcecao() {
        repository.deleteById(UUID.randomUUID());
        // se chegar aqui sem excecao, o comportamento esta correto
    }

    @Test
    void saveComCategoriaPadraoIdPreservaCampo() {
        UUID categoriaId = UUID.randomUUID();
        Payee novo = new Payee(USER_ID, "Mercado", categoriaId);

        Payee salvo = repository.save(novo);

        assertThat(salvo.getCategoriaPadraoId()).isEqualTo(categoriaId);
    }
}
