package com.laboratorio.financas.lembrete.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.lembrete.domain.Lembrete;
import com.laboratorio.financas.lembrete.domain.Prioridade;
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
        Lembrete l = new Lembrete(userId, "Pagar", "Conta", LocalDate.of(2026, 6, 1),
                Prioridade.MEDIA);
        repository.salvar(l);

        Optional<Lembrete> resultado = repository.buscarPorId(l.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getTitulo()).isEqualTo("Pagar");
        assertThat(resultado.get().getPrioridade()).isEqualTo(Prioridade.MEDIA);
    }

    @Test
    void listarPorUserIdRetornaApenasDoUsuario() {
        UUID outroUserId = criarUsuarioPersistido();
        repository.salvar(new Lembrete(userId, "A", null, LocalDate.now(), Prioridade.BAIXA));
        repository.salvar(new Lembrete(userId, "B", null, LocalDate.now(), Prioridade.ALTA));
        repository.salvar(new Lembrete(outroUserId, "X", null, LocalDate.now(), Prioridade.BAIXA));

        List<Lembrete> lista = repository.listarPorUserId(userId);

        assertThat(lista).hasSize(2);
        assertThat(lista).allMatch(e -> e.getUserId().equals(userId));
    }

    @Test
    void atualizarPersisteMudancas() {
        Lembrete l = new Lembrete(userId, "Antigo", null, LocalDate.of(2026, 6, 1),
                Prioridade.BAIXA);
        repository.salvar(l);

        l.atualizar("Novo", "Desc", LocalDate.of(2026, 7, 1), Prioridade.ALTA, true);
        repository.atualizar(l);

        Lembrete recarregado = repository.buscarPorId(l.getId()).orElseThrow();
        assertThat(recarregado.getTitulo()).isEqualTo("Novo");
        assertThat(recarregado.isConcluido()).isTrue();
        assertThat(recarregado.getPrioridade()).isEqualTo(Prioridade.ALTA);
    }

    @Test
    void deletarRemoveEntidade() {
        Lembrete l = new Lembrete(userId, "Remover", null, LocalDate.now(), Prioridade.BAIXA);
        repository.salvar(l);

        repository.deletar(l.getId());

        assertThat(repository.buscarPorId(l.getId())).isEmpty();
    }
}
