package com.laboratorio.financas.carteira.interfaces;

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
import com.laboratorio.financas.carteira.domain.TipoCarteira;
import com.laboratorio.financas.carteira.infrastructure.persistence.CarteiraEntity;
import com.laboratorio.financas.carteira.infrastructure.persistence.CarteiraJpaRepository;
import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import java.time.Instant;
import java.util.HashMap;
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
class CarteiraControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CarteiraJpaRepository jpaRepository;

    @MockitoSpyBean
    private AuditPublisher auditPublisher;

    private static final String CONTA_ID = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    private Map<String, Object> requestValido() {
        Map<String, Object> body = new HashMap<>();
        body.put("contaId", CONTA_ID);
        body.put("nome", "Tesouro");
        body.put("tipo", "RENDA_FIXA");
        return body;
    }

    private String criarCarteiraRetornaId(String nome) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contaId", CONTA_ID);
        body.put("nome", nome);
        body.put("tipo", "RENDA_FIXA");
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void postCarteiraValidaRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome", equalTo("Tesouro")))
                .andExpect(jsonPath("$.tipo", equalTo("RENDA_FIXA")))
                .andExpect(jsonPath("$.contaId", equalTo(CONTA_ID)))
                .andExpect(jsonPath("$.ativo", equalTo(true)));
    }

    @Test
    void postCarteiraNomeBlankRetorna400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contaId", CONTA_ID);
        body.put("nome", "   ");
        body.put("tipo", "RENDA_FIXA");
        mockMvc.perform(comAuth(post("/api/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome", notNullValue()));
    }

    @Test
    void postCarteiraNomeAcimaMaximoRetorna400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("contaId", CONTA_ID);
        body.put("nome", "a".repeat(101));
        body.put("tipo", "RENDA_FIXA");
        mockMvc.perform(comAuth(post("/api/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome", notNullValue()));
    }

    @Test
    void postCarteiraSemContaIdRetorna400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("nome", "Tesouro");
        body.put("tipo", "RENDA_FIXA");
        mockMvc.perform(comAuth(post("/api/carteiras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCarteirasRetornaListaDoUsuario() throws Exception {
        criarCarteiraRetornaId("Carteira A");
        criarCarteiraRetornaId("Carteira B");

        mockMvc.perform(comAuth(get("/api/carteiras")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getCarteirasRetornaListaVaziaQuandoNaoHaCarteiras() throws Exception {
        mockMvc.perform(comAuth(get("/api/carteiras")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getCarteiraPorIdRetorna200() throws Exception {
        String id = criarCarteiraRetornaId("Tesouro");

        mockMvc.perform(comAuth(get("/api/carteiras/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(id)))
                .andExpect(jsonPath("$.nome", equalTo("Tesouro")));
    }

    @Test
    void getCarteiraInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(get("/api/carteiras/" + UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)));
    }

    @Test
    void putCarteiraExistenteRetorna200() throws Exception {
        String id = criarCarteiraRetornaId("Antiga");

        Map<String, Object> update = new HashMap<>();
        update.put("nome", "Nova");
        update.put("tipo", "CRIPTOMOEDA");
        mockMvc.perform(comAuth(put("/api/carteiras/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Nova")))
                .andExpect(jsonPath("$.tipo", equalTo("CRIPTOMOEDA")));
    }

    @Test
    void putCarteiraInexistenteRetorna404() throws Exception {
        Map<String, Object> update = new HashMap<>();
        update.put("nome", "Nova");
        update.put("tipo", "OUTROS");
        mockMvc.perform(comAuth(put("/api/carteiras/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)));
    }

    @Test
    void deleteCarteiraExistenteRetorna204() throws Exception {
        String id = criarCarteiraRetornaId("Remover");

        mockMvc.perform(comAuth(delete("/api/carteiras/" + id)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCarteiraInexistenteRetorna404() throws Exception {
        mockMvc.perform(comAuth(delete("/api/carteiras/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void cicloCompletoPostGetPutDelete() throws Exception {
        String id = criarCarteiraRetornaId("Ciclo");

        mockMvc.perform(comAuth(get("/api/carteiras")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome", equalTo("Ciclo")));

        Map<String, Object> update = new HashMap<>();
        update.put("nome", "Ciclo Atualizado");
        update.put("tipo", "RENDA_VARIAVEL");
        mockMvc.perform(comAuth(put("/api/carteiras/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Ciclo Atualizado")));

        mockMvc.perform(comAuth(delete("/api/carteiras/" + id)))
                .andExpect(status().isNoContent());

        mockMvc.perform(comAuth(get("/api/carteiras")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void semAuthRetorna401() throws Exception {
        mockMvc.perform(get("/api/carteiras"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putCarteiraDeOutroUsuarioRetorna200() throws Exception {
        UUID outroUserId = registrarOutroUsuario("outro-carteira-put@test.com");
        Instant now = Instant.now();
        CarteiraEntity carteiraDeOutro = new CarteiraEntity(
                UUID.randomUUID(), outroUserId, UUID.fromString(CONTA_ID),
                "Carteira de outro", TipoCarteira.RENDA_FIXA, true, now, now);
        jpaRepository.save(carteiraDeOutro);

        Map<String, Object> update = new HashMap<>();
        update.put("nome", "Editada por outro");
        update.put("tipo", "CRIPTOMOEDA");
        mockMvc.perform(comAuth(put("/api/carteiras/" + carteiraDeOutro.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Editada por outro")));
    }

    @Test
    void deleteCarteiraDeOutroUsuarioRetorna204EAuditaDelete() throws Exception {
        UUID outroUserId = registrarOutroUsuario("outro-carteira-delete@test.com");
        Instant now = Instant.now();
        CarteiraEntity carteiraDeOutro = new CarteiraEntity(
                UUID.randomUUID(), outroUserId, UUID.fromString(CONTA_ID),
                "Carteira de outro", TipoCarteira.RENDA_FIXA, true, now, now);
        jpaRepository.save(carteiraDeOutro);

        mockMvc.perform(comAuth(delete("/api/carteiras/" + carteiraDeOutro.getId())))
                .andExpect(status().isNoContent());

        verify(auditPublisher, timeout(2000)).publish(argThat(event ->
                event.action() == AuditAction.DELETE
                        && event.entityId().equals(carteiraDeOutro.getId())
                        && event.userEmail() != null));
    }
}
