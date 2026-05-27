package com.laboratorio.financas.usuario.interfaces;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laboratorio.financas.shared.AbstractAuthenticatedIntegrationTest;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class PerfilControllerTest extends AbstractAuthenticatedIntegrationTest {

    @Autowired
    private UsuarioJpaRepository jpaRepository;

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    @Test
    void getPerfilSemAuthRetorna401() throws Exception {
        mockMvc.perform(get("/api/perfil"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPerfilAutenticadoRetorna200ComEmail() throws Exception {
        mockMvc.perform(comAuth(get("/api/perfil")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", equalTo("executor@test.com")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.criadoEm", notNullValue()));
    }

    @Test
    void putPerfilComNameValidoRetorna200ENomeAtualizado() throws Exception {
        String body = "{\"name\":\"Fabio Rezende\"}";
        mockMvc.perform(comAuth(put("/api/perfil"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", equalTo("Fabio Rezende")))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));
    }

    @Test
    void putPerfilComNameMuitoLongoRetorna400() throws Exception {
        String nameLongo = "A".repeat(101);
        String body = "{\"name\":\"" + nameLongo + "\"}";
        mockMvc.perform(comAuth(put("/api/perfil"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putSenhaComSenhaAtualIncorretaRetorna422() throws Exception {
        String body = "{\"senhaAtual\":\"errada123\",\"novaSenha\":\"novaSenha123\"}";
        mockMvc.perform(comAuth(put("/api/perfil/senha"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void putSenhaComSenhaAtualCorretaRetorna204() throws Exception {
        String body = "{\"senhaAtual\":\"senha12345678\",\"novaSenha\":\"novaSenha123\"}";
        mockMvc.perform(comAuth(put("/api/perfil/senha"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void putSenhaComNovaSenhaCurtaRetorna400() throws Exception {
        String body = "{\"senhaAtual\":\"senha12345678\",\"novaSenha\":\"curta\"}";
        mockMvc.perform(comAuth(put("/api/perfil/senha"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }
}
