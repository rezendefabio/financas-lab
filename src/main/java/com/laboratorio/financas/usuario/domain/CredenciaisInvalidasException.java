package com.laboratorio.financas.usuario.domain;

public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Credenciais invalidas");
    }
}
