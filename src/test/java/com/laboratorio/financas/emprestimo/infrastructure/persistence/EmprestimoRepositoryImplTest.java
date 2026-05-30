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

    private Emprestimo novoEmprestimo(String descricao) {
        return new Emprestimo(userId, descricao, "Joao",
                TipoEmprestimo.CONCEDIDO,
                new Money(new BigDecimal("100.00"), BRL),
                LocalDate.of(2026, 5, 30));
    }

    @Test
    void salvarEBuscarPorId() {
        Emprestimo e = novoEmprestimo("Teste");
        repository.salvar(e);

        Optional<Emprestimo> resultado = repository.buscarPorId(e.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getDescricao()).isEqualTo("Teste");
        assertThat(resultado.get().getValor().valor()).isEqualByComparingTo("100.00");
        assertThat(resultado.get().getValor().moeda()).isEqualTo(BRL);
    }

    @Test
    void listarPorUserIdRetornaApenasDoUsuario() {
        repository.salvar(novoEmprestimo("A"));
        repository.salvar(novoEmprestimo("B"));

        UUID outroUser = criarUsuarioPersistido();
        repository.salvar(new Emprestimo(outroUser, "Outro", null,
                TipoEmprestimo.RECEBIDO,
                new Money(new BigDecimal("5.00"), BRL),
                LocalDate.now()));

        List<Emprestimo> lista = repository.listarPorUserId(userId);

        assertThat(lista).hasSize(2);
        assertThat(lista).allMatch(e -> e.getUserId().equals(userId));
    }

    @Test
    void atualizarPersisteAlteracoes() {
        Emprestimo e = novoEmprestimo("Antigo");
        repository.salvar(e);

        e.atualizar("Novo", "Maria", TipoEmprestimo.RECEBIDO,
                new Money(new BigDecimal("250.00"), BRL),
                LocalDate.of(2026, 6, 1), true);
        repository.atualizar(e);

        Emprestimo recarregado = repository.buscarPorId(e.getId()).orElseThrow();
        assertThat(recarregado.getDescricao()).isEqualTo("Novo");
        assertThat(recarregado.getTipo()).isEqualTo(TipoEmprestimo.RECEBIDO);
        assertThat(recarregado.isQuitado()).isTrue();
        assertThat(recarregado.getValor().valor()).isEqualByComparingTo("250.00");
    }

    @Test
    void deletarRemoveEmprestimo() {
        Emprestimo e = novoEmprestimo("Remover");
        repository.salvar(e);

        repository.deletar(e.getId());

        assertThat(repository.buscarPorId(e.getId())).isEmpty();
    }
}
