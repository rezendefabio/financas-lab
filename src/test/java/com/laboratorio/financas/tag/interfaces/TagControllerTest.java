package com.laboratorio.financas.tag.interfaces;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.auditlog.domain.AuditAction;
import com.laboratorio.financas.auditlog.infrastructure.AuditPublisher;
import com.laboratorio.financas.tag.infrastructure.persistence.TagEntity;
import com.laboratorio.financas.tag.infrastructure.persistence.TagJpaRepository;
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
class TagControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TagJpaRepository jpaRepository;

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
        return Map.of("nome", "Essencial", "cor", "#FF0000");
    }

    private String criarTagRetornaId(String nome) throws Exception {
        Map<String, Object> body = Map.of("nome", nome);
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void postTagValidaRetorna201() throws Exception {
        mockMvc.perform(comAuth(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome", equalTo("Essencial")))
                .andExpect(jsonPath("$.cor", equalTo("#FF0000")));
    }

    @Test
    void postTagSemCorRetorna201ComCorNula() throws Exception {
        Map<String, Object> body = Map.of("nome", "Sem Cor");
        mockMvc.perform(comAuth(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome", equalTo("Sem Cor")));
    }

    @Test
    void postTagNomeBlankRetorna400() throws Exception {
        Map<String, Object> body = Map.of("nome", "   ");
        mockMvc.perform(comAuth(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome", notNullValue()));
    }

    @Test
    void postTagNomeAcimaMaximoRetorna400() throws Exception {
        Map<String, Object> body = Map.of("nome", "a".repeat(51));
        mockMvc.perform(comAuth(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome", notNullValue()));
    }

    @Test
    void postTagDuplicadaRetorna400() throws Exception {
        mockMvc.perform(comAuth(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nome", "Duplicada")))));

        mockMvc.perform(comAuth(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Duplicada")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTagsRetornaListaDoUsuario() throws Exception {
        mockMvc.perform(comAuth(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nome", "Tag A")))));
        mockMvc.perform(comAuth(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("nome", "Tag B")))));

        mockMvc.perform(comAuth(get("/api/tags")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getTagsRetornaListaVaziaQuandoNaoHaTags() throws Exception {
        mockMvc.perform(comAuth(get("/api/tags")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void putTagExistenteRetorna200() throws Exception {
        String id = criarTagRetornaId("Antiga");

        Map<String, Object> update = Map.of("nome", "Nova", "cor", "#00FF00");
        mockMvc.perform(comAuth(put("/api/tags/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Nova")))
                .andExpect(jsonPath("$.cor", equalTo("#00FF00")));
    }

    @Test
    void putTagInexistenteRetorna404() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        Map<String, Object> update = Map.of("nome", "Nova");
        mockMvc.perform(comAuth(put("/api/tags/" + idInexistente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)));
    }

    @Test
    void deleteTagExistenteRetorna204() throws Exception {
        String id = criarTagRetornaId("Remover");

        mockMvc.perform(comAuth(delete("/api/tags/" + id)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTagInexistenteRetorna404() throws Exception {
        UUID idInexistente = UUID.randomUUID();
        mockMvc.perform(comAuth(delete("/api/tags/" + idInexistente)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cicloCompletoPostGetPutDelete() throws Exception {
        MvcResult resultado = mockMvc.perform(comAuth(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Ciclo", "cor", "#111111")))))
                .andExpect(status().isCreated())
                .andReturn();
        String idStr = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(comAuth(get("/api/tags")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome", equalTo("Ciclo")));

        mockMvc.perform(comAuth(put("/api/tags/" + idStr)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Ciclo Atualizado")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Ciclo Atualizado")));

        mockMvc.perform(comAuth(delete("/api/tags/" + idStr)))
                .andExpect(status().isNoContent());

        mockMvc.perform(comAuth(get("/api/tags")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void semAuthRetorna401() throws Exception {
        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putTagDeOutroUsuarioRetorna200() throws Exception {
        UUID outroUserId = registrarOutroUsuario("outro-tag-put@test.com");
        TagEntity tagDeOutro = new TagEntity(
                UUID.randomUUID(), outroUserId, "Tag de outro", "#000000", Instant.now());
        jpaRepository.save(tagDeOutro);

        Map<String, Object> update = Map.of("nome", "Editada por outro", "cor", "#00FF00");
        mockMvc.perform(comAuth(put("/api/tags/" + tagDeOutro.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", equalTo("Editada por outro")));
    }

    @Test
    void deleteTagDeOutroUsuarioRetorna204() throws Exception {
        UUID outroUserId = registrarOutroUsuario("outro-tag-delete@test.com");
        TagEntity tagDeOutro = new TagEntity(
                UUID.randomUUID(), outroUserId, "Tag de outro", null, Instant.now());
        jpaRepository.save(tagDeOutro);

        mockMvc.perform(comAuth(delete("/api/tags/" + tagDeOutro.getId())))
                .andExpect(status().isNoContent());

        verify(auditPublisher, timeout(2000)).publish(argThat(event ->
                event.action() == AuditAction.DELETE
                        && event.entityId().equals(tagDeOutro.getId())
                        && event.userEmail() != null));
    }
}
