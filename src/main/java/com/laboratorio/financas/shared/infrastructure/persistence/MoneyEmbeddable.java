package com.laboratorio.financas.shared.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Embeddable
public class MoneyEmbeddable {

    @NotNull
    @Column(name = "valor", nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @NotNull
    @Column(name = "moeda", nullable = false, length = 3)
    private String moeda;

    protected MoneyEmbeddable() {
        // Construtor protected exigido pelo JPA. Nao usar em codigo de aplicacao.
    }

    public MoneyEmbeddable(BigDecimal valor, String moeda) {
        this.valor = valor;
        this.moeda = moeda;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getMoeda() {
        return moeda;
    }
}
