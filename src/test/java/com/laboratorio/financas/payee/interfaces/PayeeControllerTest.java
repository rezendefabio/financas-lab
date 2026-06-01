package com.laboratorio.financas.payee.interfaces;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.payee.infrastructure.persistence.PayeeEntity;
import com.laboratorio.financas.payee.infrastructure.persistence.PayeeJpaRepository;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class PayeeControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PayeeJpaRepository jpaRepository;

    @MockitoSpyBean
    private AuditPublisher auditPublisher;

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    private Map<String, Object> requestValido() {
        return Map.of("nome", "Supermercado");
    }

    private String criarPayeeRetornaId(String nome) throws Exception {
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/payees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", nome)))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void postPayeeValidoRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/payees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome", equalTo("Supermercado")))
                .andExpect(jsonPath("$.userId", notNullValue()));
    }

    @Test
    void postPayeeNomeBlankRetorna400() throws Exception {
        Map<String, Object> body = Map.of("nome", "   ");
        mockMvc.perform(comAuth(post("/api/payees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postPayeeNomeAcimaMaximoRetorna400() throws Exception {
        Map<String, Object> body = Map.of("nome", "a".repeat(101));
        mockMvc.perform(comAuth(post("/api/payees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPayeesRetornaListaDoUsuarioAutenticado() throws Exception {
        criarPayeeRetornaId("Supermercado");
        criarPayeeRetornaId("Farmacia");

        mockMvc.perform(comAuth(get("/api/payees")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getPayeesRetornaListaVaziaQuandoNaoHaPayees() throws Exception {
        mockMvc.perform(comAuth(get("/api/payees")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void putPayeeValidoRetorna200ComPayeeAtualizado() throws Exception {
        String id = criarPayeeRetornaId("Supermercado");
        Map<String, Object> update = Map.of("nome", "Mercado Extra");

        mockMvc.perform(comAuth(put("/api/payees/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Mercado Extra")));
    }

    @Test
    void putPayeeInexistenteRetorna404() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        Map<String, Object> update = Map.of("nome", "Novo Nome");

        mockMvc.perform(comAuth(put("/api/payees/" + idInexistente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePayeeExistenteRetorna204() throws Exception {
        String id = criarPayeeRetornaId("Farmacia");

        mockMvc.perform(comAuth(delete("/api/payees/" + id)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePayeeInexistenteRetorna404() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        mockMvc.perform(comAuth(delete("/api/payees/" + idInexistente)))
                .andExpect(status().isNotFound());
    }

    @Test
    void postPayeeSemAuthRetorna401() throws Exception {
        mockMvc.perform(post("/api/payees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putPayeeDeOutroUsuarioRetorna200EAuditaUpdate() throws Exception {
        UUID outroUserId = registrarOutroUsuario("outro-payee-put@test.com");
        Instant now = Instant.now();
        PayeeEntity payeeDeOutro = new PayeeEntity(
                UUID.randomUUID(), outroUserId, "Payee de outro", null, now, now);
        jpaRepository.save(payeeDeOutro);

        Map<String, Object> update = Map.of("nome", "Editado por outro");
        mockMvc.perform(comAuth(put("/api/payees/" + payeeDeOutro.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Editado por outro")));

        verify(auditPublisher, timeout(2000)).publish(argThat(event ->
                event.action() == AuditAction.UPDATE
                        && event.entityId().equals(payeeDeOutro.getId())
                        && event.userEmail() != null));
    }

    @Test
    void deletePayeeDeOutroUsuarioRetorna204EAuditaDelete() throws Exception {
        UUID outroUserId = registrarOutroUsuario("outro-payee-delete@test.com");
        Instant now = Instant.now();
        PayeeEntity payeeDeOutro = new PayeeEntity(
                UUID.randomUUID(), outroUserId, "Payee de outro", null, now, now);
        jpaRepository.save(payeeDeOutro);

        mockMvc.perform(comAuth(delete("/api/payees/" + payeeDeOutro.getId())))
                .andExpect(status().isNoContent());

        verify(auditPublisher, timeout(2000)).publish(argThat(event ->
                event.action() == AuditAction.DELETE
                        && event.entityId().equals(payeeDeOutro.getId())
                        && event.userEmail() != null));
    }

    @Test
    void cicloCompletoPostGetPutDeleteGet404() throws Exception {
        String id = criarPayeeRetornaId("Supermercado");

        mockMvc.perform(comAuth(get("/api/payees")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome", equalTo("Supermercado")));

        mockMvc.perform(comAuth(put("/api/payees/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Mercado Extra")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Mercado Extra")));

        mockMvc.perform(comAuth(delete("/api/payees/" + id)))
                .andExpect(status().isNoContent());

        mockMvc.perform(comAuth(delete("/api/payees/" + id)))
                .andExpect(status().isNotFound());
    }
}
