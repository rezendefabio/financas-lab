package com.laboratorio.financas.notificacao.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.notificacao.domain.Notificacao;
import com.laboratorio.financas.notificacao.domain.TipoNotificacao;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioEntity;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotificacaoRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private NotificacaoRepositoryImpl repository;
    @Autowired
    private NotificacaoJpaRepository jpaRepository;
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
    void salvarEBuscarPorChaveNatural() {
        UUID refId = UUID.randomUUID();
        repository.salvar(new Notificacao(userId, TipoNotificacao.ORCAMENTO_EXCEDIDO, refId,
                "Orcamento excedido", "Alimentacao: 120% utilizado"));

        Optional<Notificacao> achada = repository.buscarPorChaveNatural(
                userId, TipoNotificacao.ORCAMENTO_EXCEDIDO, refId);

        assertThat(achada).isPresent();
        assertThat(achada.get().getDescricao()).isEqualTo("Alimentacao: 120% utilizado");
    }

    @Test
    void buscarPorChaveNaturalRetornaVazioParaTipoDiferente() {
        UUID refId = UUID.randomUUID();
        repository.salvar(new Notificacao(userId, TipoNotificacao.ORCAMENTO_EXCEDIDO, refId, "T", "D"));

        Optional<Notificacao> achada = repository.buscarPorChaveNatural(
                userId, TipoNotificacao.ORCAMENTO_ATENCAO, refId);

        assertThat(achada).isEmpty();
    }

    @Test
    void listarPorUserIdRetornaApenasDoUsuario() {
        repository.salvar(new Notificacao(userId, TipoNotificacao.META_VENCIDA, UUID.randomUUID(), "A", "D"));
        UUID outroUserId = criarUsuarioPersistido();
        repository.salvar(new Notificacao(outroUserId, TipoNotificacao.META_VENCIDA, UUID.randomUUID(), "B", "D"));

        assertThat(repository.listarPorUserId(userId)).hasSize(1);
    }

    @Test
    void deletarRemoveNotificacao() {
        Notificacao n = new Notificacao(userId, TipoNotificacao.META_VENCENDO, UUID.randomUUID(), "T", "D");
        repository.salvar(n);

        repository.deletar(n.getId());

        assertThat(repository.buscarPorId(n.getId())).isEmpty();
    }
}
