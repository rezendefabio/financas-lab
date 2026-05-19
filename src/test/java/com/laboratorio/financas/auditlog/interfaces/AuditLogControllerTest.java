package com.laboratorio.financas.auditlog.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditLog;
import com.laboratorio.financas.auditlog.infrastructure.persistence.AuditLogJpaRepository;
import com.laboratorio.financas.auditlog.infrastructure.persistence.AuditLogRepositoryImpl;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@AutoConfigureMockMvc
class AuditLogControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private AuditLogJpaRepository auditLogJpaRepository;

    @Autowired
    private AuditLogRepositoryImpl auditLogRepository;

    @AfterEach
    void limpar() {
        auditLogJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/audit-log com entityType+entityId retorna o historico da entidade")
    void listarPorEntidadeRetorna200ComHistorico() throws Exception {
        UUID entityId = UUID.randomUUID();
        auditLogRepository.salvar(new AuditLog(
                "conta", entityId, AuditAction.CREATE, "user@exemplo.com", "FIN-CTA-001", null, "{}"));
        auditLogRepository.salvar(new AuditLog(
                "conta", entityId, AuditAction.UPDATE, "user@exemplo.com", "FIN-CTA-001", "{}", "{}"));
        auditLogRepository.salvar(new AuditLog(
                "conta", UUID.randomUUID(), AuditAction.CREATE, null, null, null, "{}"));

        mockMvc.perform(comAuth(get("/api/audit-log")
                        .param("entityType", "conta")
                        .param("entityId", entityId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].entityType").value("conta"))
                .andExpect(jsonPath("$.content[0].action").exists());
    }

    @Test
    @DisplayName("GET /api/audit-log sem entityId retorna a trilha geral filtrada")
    void listarGeralComFiltroDeActionRetorna200() throws Exception {
        auditLogRepository.salvar(new AuditLog(
                "conta", UUID.randomUUID(), AuditAction.CREATE, null, null, null, "{}"));
        auditLogRepository.salvar(new AuditLog(
                "categoria", UUID.randomUUID(), AuditAction.DELETE, null, null, "{}", null));

        mockMvc.perform(comAuth(get("/api/audit-log")
                        .param("action", "CREATE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].action").value("CREATE"));
    }

    @Test
    @DisplayName("GET /api/audit-log retorna 400 quando entityId vem sem entityType")
    void listarRetorna400QuandoEntityIdSemEntityType() throws Exception {
        mockMvc.perform(comAuth(get("/api/audit-log")
                        .param("entityId", UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/audit-log retorna 401 sem autenticacao")
    void listarRetorna401SemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/audit-log"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/audit-log retorna pagina vazia quando nao ha historico")
    void listarRetornaPaginaVaziaQuandoSemHistorico() throws Exception {
        mockMvc.perform(comAuth(get("/api/audit-log")
                        .param("entityType", "conta")
                        .param("entityId", UUID.randomUUID().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }
}
