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

    private static final Money VALOR = new Money(new BigDecimal("100.00"), Currency.getInstance("BRL"));
    private static final LocalDate DATA = LocalDate.of(2026, 1, 15);

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

    @Test
    void salvarEBuscarPorId() {
        Emprestimo e = new Emprestimo(userId, "X", "Joao",
                TipoEmprestimo.CONCEDIDO, VALOR, DATA, false);
        repository.salvar(e);

        Optional<Emprestimo> resultado = repository.buscarPorId(e.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getDescricao()).isEqualTo("X");
        assertThat(resultado.get().getNomeTerceiro()).isEqualTo("Joao");
        assertThat(resultado.get().getTipo()).isEqualTo(TipoEmprestimo.CONCEDIDO);
    }

    @Test
    void listarPorUserIdRetornaApenasDoUsuario() {
        repository.salvar(new Emprestimo(userId, "A", null,
                TipoEmprestimo.CONCEDIDO, VALOR, DATA, false));
        repository.salvar(new Emprestimo(userId, "B", null,
                TipoEmprestimo.RECEBIDO, VALOR, DATA, true));

        UUID outroUserId = criarUsuarioPersistido();
        repository.salvar(new Emprestimo(outroUserId, "Outro", null,
                TipoEmprestimo.CONCEDIDO, VALOR, DATA, false));

        List<Emprestimo> lista = repository.listarPorUserId(userId);

        assertThat(lista).hasSize(2);
        assertThat(lista).allMatch(e -> e.getUserId().equals(userId));
    }

    @Test
    void deletarRemoveEntidade() {
        Emprestimo e = new Emprestimo(userId, "Remover", null,
                TipoEmprestimo.CONCEDIDO, VALOR, DATA, false);
        repository.salvar(e);

        repository.deletar(e.getId());

        assertThat(repository.buscarPorId(e.getId())).isEmpty();
    }
}
