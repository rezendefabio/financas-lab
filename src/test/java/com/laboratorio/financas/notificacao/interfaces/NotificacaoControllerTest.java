package com.laboratorio.financas.notificacao.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laboratorio.financas.notificacao.infrastructure.persistence.NotificacaoJpaRepository;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotificacaoControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private NotificacaoJpaRepository jpaRepository;

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    @Test
    void getRetornaArrayVazioQuandoNaoHaOrcamentoNemMeta() throws Exception {
        mockMvc.perform(comAuth(get("/api/notificacoes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void patchDescartarInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(patch("/api/notificacoes/" + UUID.randomUUID() + "/descartar")))
                .andExpect(status().isNotFound());
    }

    @Test
    void semAuthRetorna401() throws Exception {
        mockMvc.perform(get("/api/notificacoes")).andExpect(status().isUnauthorized());
    }
}
