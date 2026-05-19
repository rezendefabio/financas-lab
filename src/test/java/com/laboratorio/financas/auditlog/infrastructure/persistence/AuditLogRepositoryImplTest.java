package com.laboratorio.financas.auditlog.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditLog;
import com.laboratorio.financas.auditlog.domain.FiltrosAuditLog;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

class AuditLogRepositoryImplTest extends AbstractIntegrationTest {

    @Autowired
    private AuditLogRepositoryImpl repository;

    @Autowired
    private AuditLogJpaRepository jpaRepository;

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    @Test
    void salvarPersisteAuditLogERetornaInstanciaEquivalente() {
        AuditLog log = new AuditLog(
                "conta", UUID.randomUUID(), AuditAction.CREATE,
                "user@exemplo.com", "FIN-CTA-001", null, "{\"nome\":\"Carteira\"}");

        AuditLog salvo = repository.salvar(log);

        assertThat(salvo.getId()).isEqualTo(log.getId());
        assertThat(salvo.getEntityType()).isEqualTo("conta");
        assertThat(salvo.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(salvo.getUserEmail()).isEqualTo("user@exemplo.com");
        assertThat(salvo.getScreenCode()).isEqualTo("FIN-CTA-001");
        assertThat(salvo.getBefore()).isNull();
        assertThat(salvo.getAfter()).isEqualTo("{\"nome\":\"Carteira\"}");
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    @Test
    void salvarAceitaUserEmailEScreenCodeNulos() {
        AuditLog log = new AuditLog(
                "categoria", UUID.randomUUID(), AuditAction.DELETE, null, null, "{}", null);

        AuditLog salvo = repository.salvar(log);

        assertThat(salvo.getUserEmail()).isNull();
        assertThat(salvo.getScreenCode()).isNull();
        assertThat(salvo.getAfter()).isNull();
    }

    @Test
    void listarPorEntidadeRetornaApenasOsEventosDaEntidadeOrdenadosPorMaisRecente() {
        UUID entityId = UUID.randomUUID();
        UUID outraEntidade = UUID.randomUUID();
        Instant base = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        repository.salvar(new AuditLog(
                UUID.randomUUID(), "conta", entityId, AuditAction.CREATE,
                null, null, null, "{}", base.minus(2, ChronoUnit.HOURS)));
        repository.salvar(new AuditLog(
                UUID.randomUUID(), "conta", entityId, AuditAction.UPDATE,
                null, null, "{}", "{}", base.minus(1, ChronoUnit.HOURS)));
        repository.salvar(new AuditLog(
                UUID.randomUUID(), "conta", outraEntidade, AuditAction.CREATE,
                null, null, null, "{}", base));

        Page<AuditLog> pagina = repository.listarPorEntidade("conta", entityId, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent().get(0).getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(pagina.getContent().get(1).getAction()).isEqualTo(AuditAction.CREATE);
    }

    @Test
    void listarComFiltrosTodosNulosRetornaTodosOsEventos() {
        repository.salvar(new AuditLog("conta", UUID.randomUUID(), AuditAction.CREATE, null, null, null, "{}"));
        repository.salvar(new AuditLog("categoria", UUID.randomUUID(), AuditAction.DELETE, null, null, "{}", null));

        FiltrosAuditLog filtros = new FiltrosAuditLog(null, null, null, null, null, null);
        Page<AuditLog> pagina = repository.listar(filtros, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
    }

    @Test
    void listarComFiltroDeEntityTypeEActionRestringeOResultado() {
        repository.salvar(new AuditLog("conta", UUID.randomUUID(), AuditAction.CREATE, null, null, null, "{}"));
        repository.salvar(new AuditLog("conta", UUID.randomUUID(), AuditAction.DELETE, null, null, "{}", null));
        repository.salvar(new AuditLog("categoria", UUID.randomUUID(), AuditAction.CREATE, null, null, null, "{}"));

        FiltrosAuditLog filtros = new FiltrosAuditLog("conta", null, AuditAction.CREATE, null, null, null);
        Page<AuditLog> pagina = repository.listar(filtros, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getEntityType()).isEqualTo("conta");
        assertThat(pagina.getContent().get(0).getAction()).isEqualTo(AuditAction.CREATE);
    }

    @Test
    void listarComFiltroDeUserEmailRestringeOResultado() {
        repository.salvar(new AuditLog(
                "conta", UUID.randomUUID(), AuditAction.CREATE, "alice@exemplo.com", null, null, "{}"));
        repository.salvar(new AuditLog(
                "conta", UUID.randomUUID(), AuditAction.CREATE, "bob@exemplo.com", null, null, "{}"));

        FiltrosAuditLog filtros = new FiltrosAuditLog(null, null, null, "alice@exemplo.com", null, null);
        Page<AuditLog> pagina = repository.listar(filtros, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getUserEmail()).isEqualTo("alice@exemplo.com");
    }

    @Test
    void listarComFiltroDeIntervaloDeDataRestringeOResultado() {
        Instant base = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        repository.salvar(new AuditLog(
                UUID.randomUUID(), "conta", UUID.randomUUID(), AuditAction.CREATE,
                null, null, null, "{}", base.minus(10, ChronoUnit.DAYS)));
        repository.salvar(new AuditLog(
                UUID.randomUUID(), "conta", UUID.randomUUID(), AuditAction.CREATE,
                null, null, null, "{}", base.minus(1, ChronoUnit.DAYS)));

        FiltrosAuditLog filtros = new FiltrosAuditLog(
                null, null, null, null, base.minus(3, ChronoUnit.DAYS), base);
        Page<AuditLog> pagina = repository.listar(filtros, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listarRetornaPaginaVaziaQuandoNaoHaCorrespondencia() {
        FiltrosAuditLog filtros = new FiltrosAuditLog("inexistente", null, null, null, null, null);
        Page<AuditLog> pagina = repository.listar(filtros, PageRequest.of(0, 10));

        assertThat(pagina.getContent()).isEmpty();
        assertThat(pagina.getTotalElements()).isZero();
    }
}
