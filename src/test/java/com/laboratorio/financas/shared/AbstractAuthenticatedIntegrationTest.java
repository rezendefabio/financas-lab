package com.laboratorio.financas.shared;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laboratorio.financas.usuario.infrastructure.persistence.UsuarioJpaRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
public abstract class AbstractAuthenticatedIntegrationTest extends AbstractIntegrationTest {

    private static final String TEST_EMAIL = "executor@test.com";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private UsuarioJpaRepository usuarioJpaRepository;

    protected String token;
    protected UUID authenticatedUserId;

    @BeforeEach
    void autenticar() throws Exception {
        String body = "{\"email\":\"" + TEST_EMAIL + "\",\"senha\":\"senha12345678\"}";
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andReturn();

        token = mapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();

        authenticatedUserId = usuarioJpaRepository.findByEmail(TEST_EMAIL)
                .map(u -> u.getId())
                .orElse(null);
    }

    protected MockHttpServletRequestBuilder comAuth(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token);
    }
}
