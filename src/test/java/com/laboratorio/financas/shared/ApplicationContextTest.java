package com.laboratorio.financas.shared;

import org.junit.jupiter.api.Test;

class ApplicationContextTest extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // Verifica que o contexto Spring sobe sem erros.
        // Falha aqui indica bean nao encontrado, datasource invalido,
        // erro de migration ou qualquer problema de startup.
    }
}
