package com.laboratorio.financas.auditlog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.domain.AuditLog;
import com.laboratorio.financas.auditlog.domain.AuditLogRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class ListarAuditLogPorEntidadeUseCaseTest {

    private static final String ENTITY_TYPE = "conta";
    private static final UUID ENTITY_ID = UUID.randomUUID();

    private AuditLogRepository auditLogRepository;
    private ListarAuditLogPorEntidadeUseCase useCase;

    @BeforeEach
    void setUp() {
        auditLogRepository = Mockito.mock(AuditLogRepository.class);
        useCase = new ListarAuditLogPorEntidadeUseCase(auditLogRepository);
    }

    @Test
    void executarDelegaAoRepositorioComPaginacaoCorreta() {
        AuditLog log = new AuditLog(ENTITY_TYPE, ENTITY_ID, AuditAction.CREATE, null, null, null, "{}");
        Page<AuditLog> pagina = new PageImpl<>(List.of(log));
        when(auditLogRepository.listarPorEntidade(
                Mockito.eq(ENTITY_TYPE), Mockito.eq(ENTITY_ID), Mockito.any(Pageable.class)))
                .thenReturn(pagina);

        Page<AuditLog> resultado = useCase.executar(ENTITY_TYPE, ENTITY_ID, 2, 25);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(auditLogRepository).listarPorEntidade(
                Mockito.eq(ENTITY_TYPE), Mockito.eq(ENTITY_ID), captor.capture());
        Pageable usado = captor.getValue();
        assertThat(usado.getPageNumber()).isEqualTo(2);
        assertThat(usado.getPageSize()).isEqualTo(25);
        assertThat(resultado.getContent()).containsExactly(log);
    }

    @Test
    void executarRetornaPaginaVaziaQuandoNaoHaHistorico() {
        when(auditLogRepository.listarPorEntidade(
                Mockito.anyString(), Mockito.any(UUID.class), Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        Page<AuditLog> resultado = useCase.executar(ENTITY_TYPE, ENTITY_ID, 0, 20);

        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isZero();
    }
}
