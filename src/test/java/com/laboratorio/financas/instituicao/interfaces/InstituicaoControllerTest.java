package com.laboratorio.financas.instituicao.interfaces;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laboratorio.financas.instituicao.domain.Instituicao;
import com.laboratorio.financas.instituicao.infrastructure.persistence.InstituicaoJpaRepository;
import com.laboratorio.financas.instituicao.infrastructure.persistence.InstituicaoRepositoryImpl;
import com.laboratorio.financas.instituicao.domain.TipoInstituicao;
import com.laboratorio.financas.shared.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class InstituicaoControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstituicaoRepositoryImpl repository;

    @Autowired
    private InstituicaoJpaRepository jpaRepository;

    @BeforeEach
    void limparAntes() {
        jpaRepository.deleteAll();
    }

    @AfterEach
    void limpar() {
        jpaRepository.deleteAll();
    }

    @Test
    void getInstituicoesRetornaListaDeAtivasSemAutenticacao() throws Exception {
        // Given
        repository.save(new Instituicao("Nubank", "260", TipoInstituicao.BANCO_DIGITAL, null, true));
        repository.save(new Instituicao("Inter", "077", TipoInstituicao.BANCO_DIGITAL, null, true));

        // When / Then
        mockMvc.perform(get("/api/instituicoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getInstituicoesNaoIncluiInativas() throws Exception {
        // Given -- 1 ativa + 1 inativa
        repository.save(new Instituicao("Nubank", "260", TipoInstituicao.BANCO_DIGITAL, null, true));
        repository.save(new Instituicao("Banco Inativo", null, TipoInstituicao.OUTRO, null, false));

        // When / Then
        mockMvc.perform(get("/api/instituicoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome", equalTo("Nubank")));
    }

    @Test
    void getInstituicoesRetornaListaVaziaQuandoNenhumaAtiva() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/instituicoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getInstituicoesRetornaCamposEsperados() throws Exception {
        // Given
        repository.save(new Instituicao("Nubank", "260", TipoInstituicao.BANCO_DIGITAL, null, true));

        // When / Then
        mockMvc.perform(get("/api/instituicoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].nome", equalTo("Nubank")))
                .andExpect(jsonPath("$[0].codigoBanco", equalTo("260")))
                .andExpect(jsonPath("$[0].tipo", equalTo("BANCO_DIGITAL")))
                .andExpect(jsonPath("$[0].ativa", equalTo(true)))
                .andExpect(jsonPath("$[0].criadoEm", notNullValue()));
    }

    @Test
    void getInstituicaoPorIdExistenteRetorna200() throws Exception {
        // Given
        Instituicao salva = repository.save(
                new Instituicao("Bradesco", "237", TipoInstituicao.BANCO_TRADICIONAL, null, true));

        // When / Then
        mockMvc.perform(get("/api/instituicoes/" + salva.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(salva.getId().toString())))
                .andExpect(jsonPath("$.nome", equalTo("Bradesco")))
                .andExpect(jsonPath("$.tipo", equalTo("BANCO_TRADICIONAL")));
    }

    @Test
    void getInstituicaoPorIdInexistenteRetorna404() throws Exception {
        // When / Then
        UUID idInexistente = UUID.randomUUID();
        mockMvc.perform(get("/api/instituicoes/" + idInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)));
    }

    @Test
    void getInstituicaoPorIdNaoRequerAutenticacao() throws Exception {
        // Given
        Instituicao salva = repository.save(
                new Instituicao("Santander", "033", TipoInstituicao.BANCO_TRADICIONAL, null, true));

        // When / Then -- sem nenhum header de auth
        mockMvc.perform(get("/api/instituicoes/" + salva.getId()))
                .andExpect(status().isOk());
    }
}
