package com.laboratorio.financas.auditlog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditLog;
import com.laboratorio.financas.auditlog.domain.AuditLogRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class RegistrarAuditLogUseCaseTest {

    private static final String ENTITY_TYPE = "conta";
    private static final UUID ENTITY_ID = UUID.randomUUID();

    private AuditLogRepository auditLogRepository;
    private RegistrarAuditLogUseCase useCase;

    @BeforeEach
    void setUp() {
        auditLogRepository = Mockito.mock(AuditLogRepository.class);
        useCase = new RegistrarAuditLogUseCase(auditLogRepository);
    }

    @Test
    void executarCriaAuditLogComOsCamposInformadosEDelegaAoRepositorio() {
        when(auditLogRepository.salvar(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLog resultado = useCase.executar(
                ENTITY_TYPE, ENTITY_ID, AuditAction.UPDATE,
                "user@exemplo.com", "FIN-CTA-001", "{\"a\":1}", "{\"a\":2}");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        Mockito.verify(auditLogRepository).salvar(captor.capture());
        AuditLog salvo = captor.getValue();
        assertThat(salvo.getEntityType()).isEqualTo(ENTITY_TYPE);
        assertThat(salvo.getEntityId()).isEqualTo(ENTITY_ID);
        assertThat(salvo.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(salvo.getUserEmail()).isEqualTo("user@exemplo.com");
        assertThat(salvo.getScreenCode()).isEqualTo("FIN-CTA-001");
        assertThat(salvo.getBefore()).isEqualTo("{\"a\":1}");
        assertThat(salvo.getAfter()).isEqualTo("{\"a\":2}");
        assertThat(resultado).isSameAs(salvo);
    }

    @Test
    void executarComUserEmailEScreenCodeNulosRegistraEventoDeSistema() {
        when(auditLogRepository.salvar(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLog resultado = useCase.executar(
                ENTITY_TYPE, ENTITY_ID, AuditAction.CREATE, null, null, null, "{}");

        assertThat(resultado.getUserEmail()).isNull();
        assertThat(resultado.getScreenCode()).isNull();
        assertThat(resultado.getBefore()).isNull();
        Mockito.verify(auditLogRepository).salvar(any(AuditLog.class));
    }
}
