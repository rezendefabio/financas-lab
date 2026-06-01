package com.laboratorio.financas.payee.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.payee.domain.Payee;
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

class PayeeRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private PayeeRepositoryImpl repository;

    @Autowired
    private PayeeJpaRepository jpaRepository;

    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    private UUID userId;
    private UUID outroUserId;

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
        userId = criarUsuarioPersistido();
        outroUserId = criarUsuarioPersistido();
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
    void savePersisteERetornaInstanciaEquivalente() {
        Payee novo = new Payee(userId, "Supermercado", null);

        Payee salvo = repository.save(novo);

        assertThat(salvo.getId()).isEqualTo(novo.getId());
        assertThat(salvo.getUserId()).isEqualTo(userId);
        assertThat(salvo.getNome()).isEqualTo("Supermercado");
        assertThat(salvo.getCategoriaPadraoId()).isNull();
    }

    @Test
    void findByIdRetornaPayeeQuandoExiste() {
        Payee novo = new Payee(userId, "Farmacia", null);
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
    void listarTodosRetornaPayeesDeQualquerUsuario() {
        repository.save(new Payee(userId, "Supermercado", null));
        repository.save(new Payee(userId, "Farmacia", null));
        repository.save(new Payee(outroUserId, "Padaria", null));

        List<Payee> resultado = repository.listarTodos();

        assertThat(resultado).hasSize(3);
        assertThat(resultado).anyMatch(p -> p.getUserId().equals(userId));
        assertThat(resultado).anyMatch(p -> p.getUserId().equals(outroUserId));
    }

    @Test
    void listarTodosRetornaListaVaziaQuandoNaoHaPayees() {
        List<Payee> resultado = repository.listarTodos();
        assertThat(resultado).isEmpty();
    }

    @Test
    void findByIdAndUserIdRetornaPayeeQuandoExisteEPertenceAoUsuario() {
        Payee novo = new Payee(userId, "Academia", null);
        repository.save(novo);

        Optional<Payee> resultado = repository.findByIdAndUserId(novo.getId(), userId);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(novo.getId());
    }

    @Test
    void findByIdAndUserIdRetornaVazioQuandoUserIdDiferente() {
        Payee novo = new Payee(userId, "Academia", null);
        repository.save(novo);

        Optional<Payee> resultado = repository.findByIdAndUserId(novo.getId(), outroUserId);

        assertThat(resultado).isEmpty();
    }

    @Test
    void findByIdAndUserIdRetornaVazioQuandoNaoExiste() {
        Optional<Payee> resultado = repository.findByIdAndUserId(UUID.randomUUID(), userId);
        assertThat(resultado).isEmpty();
    }

    @Test
    void deleteByIdRemoveDosBanco() {
        Payee novo = new Payee(userId, "Restaurante", null);
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
        Payee novo = new Payee(userId, "Mercado", categoriaId);

        Payee salvo = repository.save(novo);

        assertThat(salvo.getCategoriaPadraoId()).isEqualTo(categoriaId);
    }
}
