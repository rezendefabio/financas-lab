package com.laboratorio.financas.auditlog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditLog;
import com.laboratorio.financas.auditlog.domain.AuditLogRepository;
import com.laboratorio.financas.auditlog.domain.FiltrosAuditLog;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class ListarAuditLogUseCaseTest {

    private AuditLogRepository auditLogRepository;
    private ListarAuditLogUseCase useCase;

    @BeforeEach
    void setUp() {
        auditLogRepository = Mockito.mock(AuditLogRepository.class);
        useCase = new ListarAuditLogUseCase(auditLogRepository);
    }

    @Test
    void executarDelegaAoRepositorioComFiltrosEPaginacao() {
        FiltrosAuditLog filtros = new FiltrosAuditLog(
                "conta", null, AuditAction.CREATE, "user@exemplo.com", null, null);
        AuditLog log = new AuditLog("conta", UUID.randomUUID(), AuditAction.CREATE, null, null, null, "{}");
        when(auditLogRepository.listar(Mockito.eq(filtros), Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        Page<AuditLog> resultado = useCase.executar(filtros, 1, 50);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(auditLogRepository).listar(Mockito.eq(filtros), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
        assertThat(resultado.getContent()).containsExactly(log);
    }

    @Test
    void executarComFiltrosTodosNulosDelegaSemFiltrar() {
        FiltrosAuditLog filtros = new FiltrosAuditLog(null, null, null, null, null, null);
        when(auditLogRepository.listar(Mockito.eq(filtros), Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<AuditLog> resultado = useCase.executar(filtros, 0, 20);

        assertThat(resultado.getContent()).isEmpty();
        Mockito.verify(auditLogRepository).listar(Mockito.eq(filtros), Mockito.any(Pageable.class));
    }
}
