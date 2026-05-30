package com.laboratorio.financas.lembrete.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.PrioridadeLembrete;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioEntity;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LembreteRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private LembreteRepositoryImpl repository;

    @Autowired
    private LembreteJpaRepository jpaRepository;

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
    void salvarEBuscarPorId() {
        Lembrete lembrete = new Lembrete(userId, "Pagar conta", "Boleto",
                LocalDate.of(2026, 6, 15), PrioridadeLembrete.ALTA, false);
        repository.salvar(lembrete);

        Optional<Lembrete> resultado = repository.buscarPorId(lembrete.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getTitulo()).isEqualTo("Pagar conta");
        assertThat(resultado.get().getDescricao()).isEqualTo("Boleto");
        assertThat(resultado.get().getDataLembrete()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(resultado.get().getPrioridade()).isEqualTo(PrioridadeLembrete.ALTA);
        assertThat(resultado.get().isConcluido()).isFalse();
    }

    @Test
    void salvarAceitaDescricaoNula() {
        Lembrete lembrete = new Lembrete(userId, "Sem desc", null,
                LocalDate.now(), PrioridadeLembrete.BAIXA, false);
        repository.salvar(lembrete);

        Optional<Lembrete> resultado = repository.buscarPorId(lembrete.getId());
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getDescricao()).isNull();
    }

    @Test
    void listarPorUserIdRetornaApenasDoUsuarioOrdenadosPorData() {
        repository.salvar(new Lembrete(userId, "B", null,
                LocalDate.of(2026, 7, 1), PrioridadeLembrete.MEDIA, false));
        repository.salvar(new Lembrete(userId, "A", null,
                LocalDate.of(2026, 6, 1), PrioridadeLembrete.MEDIA, false));
        UUID outro = criarUsuarioPersistido();
        repository.salvar(new Lembrete(outro, "Outro", null,
                LocalDate.now(), PrioridadeLembrete.MEDIA, false));

        List<Lembrete> lista = repository.listarPorUserId(userId);

        assertThat(lista).hasSize(2);
        assertThat(lista).allMatch(l -> l.getUserId().equals(userId));
        assertThat(lista.get(0).getTitulo()).isEqualTo("A");
        assertThat(lista.get(1).getTitulo()).isEqualTo("B");
    }

    @Test
    void atualizarPersisteAlteracoes() {
        Lembrete lembrete = new Lembrete(userId, "Antigo", "x",
                LocalDate.of(2026, 1, 1), PrioridadeLembrete.BAIXA, false);
        repository.salvar(lembrete);

        lembrete.atualizar("Novo", "novo x",
                LocalDate.of(2026, 12, 31), PrioridadeLembrete.ALTA, true);
        repository.atualizar(lembrete);

        Optional<Lembrete> resultado = repository.buscarPorId(lembrete.getId());
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getTitulo()).isEqualTo("Novo");
        assertThat(resultado.get().getPrioridade()).isEqualTo(PrioridadeLembrete.ALTA);
        assertThat(resultado.get().isConcluido()).isTrue();
        assertThat(resultado.get().getAtualizadoEm()).isNotNull();
    }

    @Test
    void deletarRemoveEntidade() {
        Lembrete lembrete = new Lembrete(userId, "Remover", null,
                LocalDate.now(), PrioridadeLembrete.BAIXA, false);
        repository.salvar(lembrete);

        repository.deletar(lembrete.getId());

        assertThat(repository.buscarPorId(lembrete.getId())).isEmpty();
    }
}
