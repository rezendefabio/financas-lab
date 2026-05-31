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

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final LocalDate DATA = LocalDate.of(2026, 5, 30);

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

    private Emprestimo emprestimo(UUID owner, String descricao) {
        return new Emprestimo(owner, descricao, "Joao", TipoEmprestimo.CONCEDIDO,
                new Money(new BigDecimal("150.00"), BRL), DATA);
    }

    @Test
    void salvarEBuscarPorIdRetornaEntidadePersistida() {
        Emprestimo entidade = emprestimo(userId, "Teste");
        repository.salvar(entidade);

        Optional<Emprestimo> resultado = repository.buscarPorId(entidade.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getDescricao()).isEqualTo("Teste");
        assertThat(resultado.get().getTipo()).isEqualTo(TipoEmprestimo.CONCEDIDO);
        assertThat(resultado.get().getValor().valor()).isEqualByComparingTo("150.00");
        assertThat(resultado.get().getValor().moeda()).isEqualTo(BRL);
        assertThat(resultado.get().isQuitado()).isFalse();
    }

    @Test
    void listarPorUserIdRetornaSomenteDoUsuario() {
        repository.salvar(emprestimo(userId, "A"));
        repository.salvar(emprestimo(userId, "B"));
        UUID outroUserId = criarUsuarioPersistido();
        repository.salvar(emprestimo(outroUserId, "Outro"));

        List<Emprestimo> resultado = repository.listarPorUserId(userId);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(e -> e.getUserId().equals(userId));
    }

    @Test
    void atualizarPersisteNovosValores() {
        Emprestimo entidade = emprestimo(userId, "Antigo");
        repository.salvar(entidade);

        entidade.atualizar("Novo", "Maria", TipoEmprestimo.RECEBIDO,
                new Money(new BigDecimal("300.00"), BRL), DATA.plusDays(2), true);
        repository.atualizar(entidade);

        Optional<Emprestimo> resultado = repository.buscarPorId(entidade.getId());
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getDescricao()).isEqualTo("Novo");
        assertThat(resultado.get().getTipo()).isEqualTo(TipoEmprestimo.RECEBIDO);
        assertThat(resultado.get().isQuitado()).isTrue();
    }

    @Test
    void deletarRemoveEntidade() {
        Emprestimo entidade = emprestimo(userId, "Para deletar");
        repository.salvar(entidade);

        repository.deletar(entidade.getId());

        assertThat(repository.buscarPorId(entidade.getId())).isEmpty();
    }
}
