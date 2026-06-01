package com.laboratorio.financas.assinatura.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.assinatura.domain.Assinatura;
import com.laboratorio.financas.assinatura.domain.TipoAssinatura;
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

class AssinaturaRepositoryImplTest extends AbstractIntegrationTest {

    private static final Money VALOR = new Money(new BigDecimal("29.90"), Currency.getInstance("BRL"));
    private static final LocalDate RENOVACAO = LocalDate.of(2026, 6, 15);

    @Autowired
    private AssinaturaRepositoryImpl repository;
    @Autowired
    private AssinaturaJpaRepository jpaRepository;
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

    @Test
    void salvarEBuscarPorIdRetornaEntidadePersistida() {
        Assinatura entidade = new Assinatura(userId, "Netflix", TipoAssinatura.STREAMING, VALOR, RENOVACAO);
        repository.salvar(entidade);

        Optional<Assinatura> resultado = repository.buscarPorId(entidade.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("Netflix");
        assertThat(resultado.get().getTipo()).isEqualTo(TipoAssinatura.STREAMING);
        assertThat(resultado.get().getValorMensal().valor()).isEqualByComparingTo("29.90");
        assertThat(resultado.get().getDataRenovacao()).isEqualTo(RENOVACAO);
        assertThat(resultado.get().isAtiva()).isTrue();
    }

    @Test
    void listarTodosRetornaAssinaturasDeQualquerUsuario() {
        repository.salvar(new Assinatura(userId, "Netflix", TipoAssinatura.STREAMING, VALOR, RENOVACAO));
        repository.salvar(new Assinatura(userId, "Spotify", TipoAssinatura.STREAMING, VALOR, RENOVACAO));
        UUID outroUserId = criarUsuarioPersistido();
        repository.salvar(new Assinatura(outroUserId, "Outro", TipoAssinatura.OUTROS, VALOR, RENOVACAO));

        List<Assinatura> resultado = repository.listarTodos();

        assertThat(resultado).hasSize(3);
        assertThat(resultado).anyMatch(a -> a.getUserId().equals(userId));
        assertThat(resultado).anyMatch(a -> a.getUserId().equals(outroUserId));
    }

    @Test
    void atualizarPersisteAlteracoes() {
        Assinatura entidade = new Assinatura(userId, "Netflix", TipoAssinatura.STREAMING, VALOR, RENOVACAO);
        repository.salvar(entidade);

        entidade.atualizar("Spotify", TipoAssinatura.OUTROS,
                new Money(new BigDecimal("49.90"), Currency.getInstance("BRL")),
                LocalDate.of(2026, 7, 1), false);
        repository.atualizar(entidade);

        Optional<Assinatura> resultado = repository.buscarPorId(entidade.getId());
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("Spotify");
        assertThat(resultado.get().isAtiva()).isFalse();
    }

    @Test
    void deletarRemoveEntidade() {
        Assinatura entidade = new Assinatura(userId, "Para deletar", TipoAssinatura.SOFTWARE, VALOR, RENOVACAO);
        repository.salvar(entidade);

        repository.deletar(entidade.getId());

        assertThat(repository.buscarPorId(entidade.getId())).isEmpty();
    }
}
