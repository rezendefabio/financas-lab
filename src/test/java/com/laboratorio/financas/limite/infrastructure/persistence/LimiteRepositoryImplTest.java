package com.laboratorio.financas.limite.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.limite.domain.Limite;
import com.laboratorio.financas.limite.domain.TipoLimite;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.shared.domain.Money;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioEntity;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LimiteRepositoryImplTest extends AbstractIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Autowired
    private LimiteRepositoryImpl repository;

    @Autowired
    private LimiteJpaRepository jpaRepository;

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

    private Money valor(String v) {
        return new Money(new BigDecimal(v), BRL);
    }

    @Test
    void salvarEBuscarPorId() {
        Limite limite = new Limite(userId, "Mensal", TipoLimite.MENSAL, valor("500.00"));
        repository.salvar(limite);

        Optional<Limite> resultado = repository.buscarPorId(limite.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("Mensal");
        assertThat(resultado.get().getTipo()).isEqualTo(TipoLimite.MENSAL);
        assertThat(resultado.get().getValor().valor()).isEqualByComparingTo("500.00");
        assertThat(resultado.get().getValor().moeda()).isEqualTo(BRL);
    }

    @Test
    void listarTodosRetornaLimitesDeQualquerUsuario() {
        repository.salvar(new Limite(userId, "A", TipoLimite.DIARIO, valor("10.00")));
        repository.salvar(new Limite(userId, "B", TipoLimite.SEMANAL, valor("20.00")));
        UUID outroUser = criarUsuarioPersistido();
        repository.salvar(new Limite(outroUser, "Outro", TipoLimite.ANUAL, valor("30.00")));

        List<Limite> lista = repository.listarTodos();

        assertThat(lista).hasSize(3);
        assertThat(lista).anyMatch(l -> l.getUserId().equals(userId));
        assertThat(lista).anyMatch(l -> l.getUserId().equals(outroUser));
    }

    @Test
    void atualizarPersisteAlteracoes() {
        Limite limite = new Limite(userId, "Antigo", TipoLimite.DIARIO, valor("50.00"));
        repository.salvar(limite);

        limite.atualizar("Novo", TipoLimite.ANUAL, valor("99.99"));
        repository.atualizar(limite);

        Optional<Limite> resultado = repository.buscarPorId(limite.getId());
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("Novo");
        assertThat(resultado.get().getTipo()).isEqualTo(TipoLimite.ANUAL);
        assertThat(resultado.get().getValor().valor()).isEqualByComparingTo("99.99");
    }

    @Test
    void deletarRemoveEntidade() {
        Limite limite = new Limite(userId, "Remover", TipoLimite.MENSAL, valor("10.00"));
        repository.salvar(limite);

        repository.deletar(limite.getId());

        assertThat(repository.buscarPorId(limite.getId())).isEmpty();
    }
}
