package com.laboratorio.financas.anotacao.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.anotacao.domain.Anotacao;
import com.laboratorio.financas.anotacao.domain.PrioridadeAnotacao;
import com.laboratorio.financas.anotacao.domain.TipoAnotacao;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnotacaoRepositoryImplTest extends AbstractIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Autowired
    private AnotacaoRepositoryImpl repository;

    @Autowired
    private AnotacaoJpaRepository jpaRepository;

    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

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
    void salvarPersisteAnotacaoERetornaInstanciaEquivalente() {
        // Given
        UUID usuarioId = criarUsuarioPersistido();
        Anotacao nova = new Anotacao(
                usuarioId,
                "Pagar fatura",
                "Fatura do cartao",
                TipoAnotacao.LEMBRETE,
                PrioridadeAnotacao.ALTA,
                null,
                null
        );

        // When
        Anotacao salva = repository.salvar(nova);

        // Then
        assertThat(salva.getId()).isEqualTo(nova.getId());
        assertThat(salva.getTitulo()).isEqualTo("Pagar fatura");
        assertThat(salva.getConteudo()).isEqualTo("Fatura do cartao");
        assertThat(salva.getTipo()).isEqualTo(TipoAnotacao.LEMBRETE);
        assertThat(salva.getPrioridade()).isEqualTo(PrioridadeAnotacao.ALTA);
        assertThat(salva.getValor()).isNull();
    }

    @Test
    void salvarPersisteAnotacaoComValorMonetario() {
        // Given
        UUID usuarioId = criarUsuarioPersistido();
        Money valor = new Money(new BigDecimal("500.00"), BRL);
        Anotacao nova = new Anotacao(
                usuarioId,
                "Planejamento",
                null,
                TipoAnotacao.PLANEJAMENTO,
                PrioridadeAnotacao.MEDIA,
                valor,
                LocalDate.of(2026, 6, 1)
        );

        // When
        repository.salvar(nova);
        Optional<Anotacao> recuperada = repository.buscarPorId(nova.getId());

        // Then
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getValor()).isNotNull();
        assertThat(recuperada.get().getValor().valor()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(recuperada.get().getDataReferencia()).isEqualTo(LocalDate.of(2026, 6, 1));
    }

    @Test
    void buscarPorIdRetornaAnotacaoQuandoExiste() {
        // Given
        UUID usuarioId = criarUsuarioPersistido();
        Anotacao nova = new Anotacao(
                usuarioId, "Titulo", null, TipoAnotacao.OBSERVACAO, PrioridadeAnotacao.BAIXA, null, null
        );
        repository.salvar(nova);

        // When
        Optional<Anotacao> resultado = repository.buscarPorId(nova.getId());

        // Then
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(nova.getId());
        assertThat(resultado.get().getTitulo()).isEqualTo("Titulo");
    }

    @Test
    void buscarPorIdRetornaVazioQuandoNaoExiste() {
        // When
        Optional<Anotacao> resultado = repository.buscarPorId(UUID.randomUUID());

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    void listarPorUsuarioRetornaApenasAnotacoesDoUsuario() {
        // Given
        UUID usuarioId = criarUsuarioPersistido();
        UUID outroUsuarioId = criarUsuarioPersistido();

        Anotacao a1 = new Anotacao(
                usuarioId, "A1", null, TipoAnotacao.LEMBRETE, PrioridadeAnotacao.MEDIA, null, null
        );
        Anotacao a2 = new Anotacao(
                usuarioId, "A2", null, TipoAnotacao.ALERTA, PrioridadeAnotacao.ALTA, null, null
        );
        Anotacao outroUsuario = new Anotacao(
                outroUsuarioId, "Outro", null, TipoAnotacao.OBSERVACAO, PrioridadeAnotacao.BAIXA, null, null
        );
        repository.salvar(a1);
        repository.salvar(a2);
        repository.salvar(outroUsuario);

        // When
        List<Anotacao> resultado = repository.listarPorUsuario(usuarioId);

        // Then
        assertThat(resultado).hasSize(2);
        assertThat(resultado)
                .extracting(Anotacao::getTitulo)
                .containsExactlyInAnyOrder("A1", "A2");
    }

    @Test
    void listarPorUsuarioRetornaListaVaziaQuandoNaoHaAnotacoes() {
        // Given
        UUID usuarioId = criarUsuarioPersistido();

        // When
        List<Anotacao> resultado = repository.listarPorUsuario(usuarioId);

        // Then
        assertThat(resultado).isEmpty();
    }

    @Test
    void deletarRemoveAnotacaoDoRepositorio() {
        // Given
        UUID usuarioId = criarUsuarioPersistido();
        Anotacao nova = new Anotacao(
                usuarioId, "Para deletar", null, TipoAnotacao.LEMBRETE, PrioridadeAnotacao.BAIXA, null, null
        );
        repository.salvar(nova);

        // When
        repository.deletar(nova.getId());

        // Then
        Optional<Anotacao> resultado = repository.buscarPorId(nova.getId());
        assertThat(resultado).isEmpty();
    }
}
