package com.laboratorio.financas.emprestimo.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.emprestimo.domain.Emprestimo;
import com.laboratorio.financas.emprestimo.domain.TipoEmprestimo;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioEntity;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EmprestimoRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private EmprestimoRepositoryImpl repository;
    @Autowired
    private EmprestimoJpaRepository jpaRepository;
    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
        userId = criarUsuarioPersistido();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
        usuarioJpaRepository.deleteAll();
    }

    private UUID criarUsuarioPersistido() {
        UUID id = UUID.randomUUID();
        usuarioJpaRepository.save(new UsuarioEntity(
                id, "teste+" + id + "@test.com", "hash", true,
                Instant.now(), null, Instant.now()));
        return id;
    }

    private Emprestimo make(UUID owner, String descricao) {
        Money valor = new Money(new BigDecimal("100.00"), Currency.getInstance("BRL"));
        return new Emprestimo(owner, descricao, "Joao", TipoEmprestimo.CONCEDIDO,
                valor, LocalDate.of(2026, 1, 15), false);
    }

    @Test
    void salvarEBuscarPorIdRetornaEntidadePersistida() {
        Emprestimo e = make(userId, "Teste");
        repository.salvar(e);

        Optional<Emprestimo> resultado = repository.buscarPorId(e.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getDescricao()).isEqualTo("Teste");
        assertThat(resultado.get().getValor().valor()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void listarPorUserIdRetornaSomenteDoUsuario() {
        repository.salvar(make(userId, "A"));
        repository.salvar(make(userId, "B"));
        UUID outroUserId = criarUsuarioPersistido();
        repository.salvar(make(outroUserId, "Outro"));

        List<Emprestimo> resultado = repository.listarPorUserId(userId);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(n -> n.getUserId().equals(userId));
    }

    @Test
    void deletarRemoveEntidade() {
        Emprestimo e = make(userId, "Para deletar");
        repository.salvar(e);

        repository.deletar(e.getId());

        assertThat(repository.buscarPorId(e.getId())).isEmpty();
    }
}
