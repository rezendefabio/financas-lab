package com.laboratorio.financas.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal valor, Currency moeda) {

    private static final int ESCALA = 2;
    private static final RoundingMode MODO_ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    public Money {
        Objects.requireNonNull(valor, "valor nao pode ser nulo");
        Objects.requireNonNull(moeda, "moeda nao pode ser nula");
        valor = valor.setScale(ESCALA, MODO_ARREDONDAMENTO);
    }

    public Money somar(Money outro) {
        validarMesmaMoeda(outro);
        return new Money(this.valor.add(outro.valor), this.moeda);
    }

    public Money subtrair(Money outro) {
        validarMesmaMoeda(outro);
        return new Money(this.valor.subtract(outro.valor), this.moeda);
    }

    public Money multiplicar(BigDecimal fator) {
        Objects.requireNonNull(fator, "fator nao pode ser nulo");
        return new Money(this.valor.multiply(fator), this.moeda);
    }

    public Money negar() {
        return new Money(this.valor.negate(), this.moeda);
    }

    public boolean ehZero() {
        return this.valor.signum() == 0;
    }

    public boolean ehNegativo() {
        return this.valor.signum() < 0;
    }

    public boolean ehPositivo() {
        return this.valor.signum() > 0;
    }

    private void validarMesmaMoeda(Money outro) {
        Objects.requireNonNull(outro, "outro Money nao pode ser nulo");
        if (!this.moeda.equals(outro.moeda)) {
            throw new IllegalArgumentException(
                "Moedas diferentes: " + this.moeda.getCurrencyCode()
                + " e " + outro.moeda.getCurrencyCode()
            );
        }
    }
}
