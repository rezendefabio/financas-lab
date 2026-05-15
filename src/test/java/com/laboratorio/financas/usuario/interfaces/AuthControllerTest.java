package com.laboratorio.financas.usuario.interfaces;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laboratorio.financas.shared.AbstractIntegrationTest;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AuthControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioJpaRepository jpaRepository;

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    @Test
    void registrarDadosValidosRetorna201ComUsuario() throws Exception {
        String body = """
                {"email":"novo@email.com","senha":"senha12345678"}
                """;
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.email", equalTo("novo@email.com")))
                .andExpect(jsonPath("$.name").value(nullValue()))
                .andExpect(jsonPath("$.criadoEm", notNullValue()));
    }

    @Test
    void registrarComNameRetorna201ComNameNoResponse() throws Exception {
        String body = """
                {"email":"named@email.com","senha":"senha12345678","name":"Fabio"}
                """;
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", equalTo("Fabio")));
    }

    @Test
    void registrarNameMuitoLongoRetorna400() throws Exception {
        String nameLongo = "A".repeat(101);
        String body = "{\"email\":\"long@email.com\",\"senha\":\"senha12345678\",\"name\":\"" + nameLongo + "\"}";
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarEmailDuplicadoRetorna409() throws Exception {
        String body = """
                {"email":"dup@email.com","senha":"senha12345678"}
                """;
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void registrarEmailInvalidoRetorna400() throws Exception {
        String body = """
                {"email":"nao-e-email","senha":"senha12345678"}
                """;
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarSenhaCurtaRetorna400() throws Exception {
        String body = """
                {"email":"user@email.com","senha":"curta"}
                """;
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginCredenciaisValidasRetornaToken() throws Exception {
        String body = """
                {"email":"login@email.com","senha":"senha12345678"}
                """;
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tipo", equalTo("Bearer")))
                .andExpect(jsonPath("$.expiresIn", equalTo(900)));
    }

    @Test
    void loginSenhaErradaRetorna401() throws Exception {
        String registrar = """
                {"email":"wrong@email.com","senha":"senha12345678"}
                """;
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrar));

        String loginErrado = """
                {"email":"wrong@email.com","senha":"senhaerrada123"}
                """;
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginErrado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginEmailNaoExisteRetorna401() throws Exception {
        String body = """
                {"email":"naoexiste@email.com","senha":"senha12345678"}
                """;
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointProtegidoSemTokenRetorna401() throws Exception {
        mockMvc.perform(post("/api/contas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
