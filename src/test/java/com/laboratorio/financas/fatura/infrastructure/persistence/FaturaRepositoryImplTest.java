package com.laboratorio.financas.fatura.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.fatura.domain.Fatura;
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

class FaturaRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private FaturaRepositoryImpl repository;

    @Autowired
    private FaturaJpaRepository jpaRepository;

    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    private UUID userId;
    private UUID outroUserId;
    private final UUID contaId = UUID.randomUUID();
    private final LocalDate vencimento = LocalDate.of(2026, 6, 10);

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
    void salvarPersisteERetornaInstanciaEquivalente() {
        Money valor = new Money(new BigDecimal("1500.00"), Currency.getInstance("BRL"));
        Fatura nova = new Fatura(userId, contaId, "Cartao Maio", vencimento, null, valor);

        Fatura salva = repository.salvar(nova);

        assertThat(salva.getId()).isEqualTo(nova.getId());
        assertThat(salva.getUserId()).isEqualTo(userId);
        assertThat(salva.getContaId()).isEqualTo(contaId);
        assertThat(salva.getNome()).isEqualTo("Cartao Maio");
        assertThat(salva.getValorTotal().valor()).isEqualByComparingTo("1500.00");
        assertThat(salva.isPaga()).isFalse();
    }

    @Test
    void salvarComValorTotalNuloPersisteNulo() {
        Fatura nova = new Fatura(userId, contaId, "Cartao", vencimento, null, null);

        repository.salvar(nova);
        Optional<Fatura> resultado = repository.buscarPorId(nova.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getValorTotal()).isNull();
    }

    @Test
    void buscarPorIdRetornaFaturaQuandoExiste() {
        Fatura nova = new Fatura(userId, contaId, "Cartao", vencimento, null, null);
        repository.salvar(nova);

        Optional<Fatura> resultado = repository.buscarPorId(nova.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("Cartao");
    }

    @Test
    void buscarPorIdRetornaVazioQuandoNaoExiste() {
        Optional<Fatura> resultado = repository.buscarPorId(UUID.randomUUID());
        assertThat(resultado).isEmpty();
    }

    @Test
    void listarTodosRetornaFaturasDeQualquerUsuario() {
        repository.salvar(new Fatura(userId, contaId, "Maio", vencimento, null, null));
        repository.salvar(new Fatura(userId, contaId, "Junho", vencimento, null, null));
        repository.salvar(new Fatura(outroUserId, contaId, "Maio", vencimento, null, null));

        List<Fatura> resultado = repository.listarTodos();

        assertThat(resultado).hasSize(3);
        assertThat(resultado).anyMatch(f -> f.getUserId().equals(userId));
        assertThat(resultado).anyMatch(f -> f.getUserId().equals(outroUserId));
    }

    @Test
    void atualizarPersisteAlteracoes() {
        Fatura nova = new Fatura(userId, contaId, "Cartao", vencimento, null, null);
        repository.salvar(nova);

        nova.atualizar("Cartao Atualizado", vencimento, LocalDate.of(2026, 6, 3),
                new Money(new BigDecimal("999.00"), Currency.getInstance("BRL")));
        repository.atualizar(nova);

        Optional<Fatura> resultado = repository.buscarPorId(nova.getId());
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("Cartao Atualizado");
        assertThat(resultado.get().getDataFechamento()).isEqualTo(LocalDate.of(2026, 6, 3));
        assertThat(resultado.get().getValorTotal().valor()).isEqualByComparingTo("999.00");
    }

    @Test
    void deletarRemoveFatura() {
        Fatura nova = new Fatura(userId, contaId, "Cartao", vencimento, null, null);
        repository.salvar(nova);

        repository.deletar(nova.getId());

        assertThat(repository.buscarPorId(nova.getId())).isEmpty();
    }
}
