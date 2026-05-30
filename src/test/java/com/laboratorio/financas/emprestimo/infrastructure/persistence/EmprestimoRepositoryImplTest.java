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
        UsuarioEntity entity = new UsuarioEntity(
                id, "teste+" + id + "@test.com", "hash_bcrypt", true,
                Instant.now(), null, Instant.now());
        usuarioJpaRepository.save(entity);
        return id;
    }

    private Emprestimo novoEmprestimo(String descricao) {
        return new Emprestimo(userId, descricao, "Joao", TipoEmprestimo.CONCEDIDO,
                new Money(new BigDecimal("100.00"), BRL),
                LocalDate.of(2026, 1, 15), false);
    }

    @Test
    void salvarEBuscarPorId() {
        Emprestimo e = novoEmprestimo("Teste");
        repository.salvar(e);

        Optional<Emprestimo> resultado = repository.buscarPorId(e.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getDescricao()).isEqualTo("Teste");
        assertThat(resultado.get().getValor().valor()).isEqualByComparingTo("100.00");
        assertThat(resultado.get().getTipo()).isEqualTo(TipoEmprestimo.CONCEDIDO);
    }

    @Test
    void listarPorUserIdRetornaApenasDoUsuario() {
        repository.salvar(novoEmprestimo("A"));
        repository.salvar(novoEmprestimo("B"));

        UUID outroUserId = criarUsuarioPersistido();
        repository.salvar(new Emprestimo(outroUserId, "Outro", null, TipoEmprestimo.RECEBIDO,
                new Money(new BigDecimal("50.00"), BRL), LocalDate.of(2026, 1, 1), false));

        List<Emprestimo> lista = repository.listarPorUserId(userId);

        assertThat(lista).hasSize(2);
        assertThat(lista).allMatch(e -> e.getUserId().equals(userId));
    }

    @Test
    void atualizarPersisteMudancas() {
        Emprestimo e = novoEmprestimo("Antiga");
        repository.salvar(e);
        e.atualizar("Nova", "Maria", TipoEmprestimo.RECEBIDO,
                new Money(new BigDecimal("999.99"), BRL),
                LocalDate.of(2026, 6, 1), true);
        repository.atualizar(e);

        Emprestimo recarregado = repository.buscarPorId(e.getId()).orElseThrow();
        assertThat(recarregado.getDescricao()).isEqualTo("Nova");
        assertThat(recarregado.isQuitado()).isTrue();
        assertThat(recarregado.getTipo()).isEqualTo(TipoEmprestimo.RECEBIDO);
    }

    @Test
    void deletarRemoveEntidade() {
        Emprestimo e = novoEmprestimo("Remover");
        repository.salvar(e);

        repository.deletar(e.getId());

        assertThat(repository.buscarPorId(e.getId())).isEmpty();
    }
}
