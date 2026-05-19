package com.laboratorio.financas.auditlog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditLogTest {

    private static final String ENTITY_TYPE = "conta";
    private static final UUID ENTITY_ID = UUID.randomUUID();

    // --- Construtor "novo" ---

    @Test
    void construtorNovoComArgumentosValidosGeraIdECriadoEm() {
        Instant antes = Instant.now();

        AuditLog log = new AuditLog(
                ENTITY_TYPE, ENTITY_ID, AuditAction.CREATE,
                "user@exemplo.com", "FIN-CTA-001", null, "{\"nome\":\"Carteira\"}");

        Instant depois = Instant.now();
        assertThat(log.getId()).isNotNull();
        assertThat(log.getEntityType()).isEqualTo(ENTITY_TYPE);
        assertThat(log.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(log.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(log.getUserEmail()).isEqualTo("user@exemplo.com");
        assertThat(log.getScreenCode()).isEqualTo("FIN-CTA-001");
        assertThat(log.getBefore()).isNull();
        assertThat(log.getAfter()).isEqualTo("{\"nome\":\"Carteira\"}");
        assertThat(log.getCriadoEm()).isBetween(antes, depois);
    }

    @Test
    void construtorNovoComDoisLogsGeraIdsDiferentes() {
        AuditLog a = new AuditLog(ENTITY_TYPE, ENTITY_ID, AuditAction.CREATE, null, null, null, null);
        AuditLog b = new AuditLog(ENTITY_TYPE, ENTITY_ID, AuditAction.CREATE, null, null, null, null);
        assertThat(a.getId()).isNotEqualTo(b.getId());
    }

    @Test
    void construtorNovoAceitaUserEmailEScreenCodeNulos() {
        AuditLog log = new AuditLog(ENTITY_TYPE, ENTITY_ID, AuditAction.DELETE, null, null, "{}", null);
        assertThat(log.getUserEmail()).isNull();
        assertThat(log.getScreenCode()).isNull();
        assertThat(log.getAfter()).isNull();
    }

    // --- Construtor "reconstituicao" ---

    @Test
    void construtorReconstituicaoPreservaTodosOsCampos() {
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.parse("2026-05-18T10:15:30Z");

        AuditLog log = new AuditLog(
                id, ENTITY_TYPE, ENTITY_ID, AuditAction.UPDATE,
                "user@exemplo.com", "FIN-CTA-001", "{\"a\":1}", "{\"a\":2}", criadoEm);

        assertThat(log.getId()).isEqualTo(id);
        assertThat(log.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(log.getBefore()).isEqualTo("{\"a\":1}");
        assertThat(log.getAfter()).isEqualTo("{\"a\":2}");
    }

    // --- Validacoes ---

    @Test
    void construtorComEntityTypeNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AuditLog(null, ENTITY_ID, AuditAction.CREATE, null, null, null, null))
                .withMessageContaining("entityType");
    }

    @Test
    void construtorComEntityTypeVazioLancaIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AuditLog("   ", ENTITY_ID, AuditAction.CREATE, null, null, null, null))
                .withMessageContaining("entityType");
    }

    @Test
    void construtorComEntityIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AuditLog(ENTITY_TYPE, null, AuditAction.CREATE, null, null, null, null))
                .withMessageContaining("entityId");
    }

    @Test
    void construtorComActionNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AuditLog(ENTITY_TYPE, ENTITY_ID, null, null, null, null, null))
                .withMessageContaining("action");
    }

    @Test
    void construtorReconstituicaoComIdNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AuditLog(
                        null, ENTITY_TYPE, ENTITY_ID, AuditAction.CREATE,
                        null, null, null, null, Instant.now()))
                .withMessageContaining("id");
    }

    @Test
    void construtorReconstituicaoComCriadoEmNuloLancaNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AuditLog(
                        UUID.randomUUID(), ENTITY_TYPE, ENTITY_ID, AuditAction.CREATE,
                        null, null, null, null, null))
                .withMessageContaining("criadoEm");
    }

    // --- Identidade ---

    @Test
    void equalsComMesmoIdRetornaTrue() {
        UUID id = UUID.randomUUID();
        Instant criadoEm = Instant.now();
        AuditLog a = new AuditLog(id, ENTITY_TYPE, ENTITY_ID, AuditAction.CREATE, null, null, null, null, criadoEm);
        AuditLog b = new AuditLog(id, "categoria", UUID.randomUUID(), AuditAction.DELETE, null, null, null, null,
                criadoEm);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void equalsComIdsDiferentesRetornaFalse() {
        AuditLog a = new AuditLog(ENTITY_TYPE, ENTITY_ID, AuditAction.CREATE, null, null, null, null);
        AuditLog b = new AuditLog(ENTITY_TYPE, ENTITY_ID, AuditAction.CREATE, null, null, null, null);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsComObjetoDeOutroTipoRetornaFalse() {
        AuditLog a = new AuditLog(ENTITY_TYPE, ENTITY_ID, AuditAction.CREATE, null, null, null, null);
        assertThat(a).isNotEqualTo("nao e um AuditLog");
    }

    @Test
    void toStringContemEntityTypeEAction() {
        AuditLog log = new AuditLog(ENTITY_TYPE, ENTITY_ID, AuditAction.UPDATE, null, null, null, null);
        assertThat(log.toString()).contains("conta", "UPDATE");
    }
}
